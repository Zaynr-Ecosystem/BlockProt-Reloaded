/*
 * Copyright (C) 2021 - 2026 spnda
 * Modifications Copyright (C) 2025 - 2026 Zaynr (Zar)
 * This file is part of BlockProt Reloaded <https://github.com/VictorGugug/BlockProt-Reloaded>.
 * Based on BlockProt <https://github.com/spnda/BlockProt>.
 *
 * BlockProt is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BlockProt is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlockProt.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.sean.blockprot.bukkit.audit;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SQLite-based access audit log. Records who attempted to access which block and when.
 *
 * Improvements over original:
 *  - WAL journal mode: concurrent readers never block the async writer.
 *  - Shared cache + connection pool (3 connections) instead of a single shared Connection,
 *    which caused "database is locked" under concurrent async writes.
 *  - Writes are dispatched via Bukkit's async scheduler (same thread pool used by the rest
 *    of the plugin) instead of the global ForkJoinPool, so they obey the server's
 *    thread-pool limits and show up correctly in Paper's async task profiler.
 *  - Auto-commit is left enabled per connection; each INSERT is its own fast WAL transaction.
 *  - pruneIfNeeded uses a COUNT(*) cache to avoid a full table scan on every write.
 */
public final class AuditLogger {

    public enum Action {
        ACCESS_DENIED,   // A player without permission attempted to open a protected block.
        ACCESS_GRANTED,  // A player with permission accessed a block (generic, unused by default).
        OPENED,          // A player opened (accessed the inventory of) a protected block.
        ITEM_TAKEN,      // A player took an item from a protected block.
        ITEM_PLACED,     // A player placed an item into a protected block.
        RAID_EXPLOSION,  // An explosion affected or attempted to affect a protected block.
        ADMIN_UNLOCK     // An admin remotely removed the protection from a block (kept for DB compatibility).
    }

    public record AuditEntry(
        long id,
        String playerUuid,
        String playerName,
        String world,
        int x, int y, int z,
        Action action,
        long timestamp
    ) {}

    private static final String DB_NAME    = "blockprot_audit.sqlite";
    private static final int    MAX_ENTRIES = 50_000;
    private static final int    PRUNE_BATCH = 5_000;
    /** Only run a COUNT(*) check every N writes to avoid a full-table scan on each insert. */
    private static final int    PRUNE_CHECK_INTERVAL = 500;

    private final String jdbcUrl;
    /** Lightweight pool: three independent JDBC connections. */
    private final Connection[] pool = new Connection[3];
    private int poolIdx = 0;

    private volatile boolean closed = false;
    private int writesSincePrune = 0;

    public AuditLogger(@NotNull File dataFolder) throws SQLException {
        File mysqlDir = new File(dataFolder, "mysql");
        if (!mysqlDir.exists()) mysqlDir.mkdirs();

        // Migrate the old database from the root folder to mysql/ if it still exists there.
        File oldDb = new File(dataFolder, DB_NAME);
        File newDb = new File(mysqlDir, DB_NAME);
        if (oldDb.exists() && !newDb.exists()) {
            oldDb.renameTo(newDb);
        } else if (oldDb.exists()) {
            oldDb.delete();
        }

        jdbcUrl = "jdbc:sqlite:" + newDb.getAbsolutePath();

        for (int i = 0; i < pool.length; i++) {
            pool[i] = openConnection();
        }
        initSchema(pool[0]);
    }

    private Connection openConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl);
        conn.setAutoCommit(true);
        try (Statement s = conn.createStatement()) {
            // WAL mode: readers don't block the writer and vice-versa.
            s.execute("PRAGMA journal_mode=WAL");
            // Relaxed sync: safe with WAL; trades a tiny crash-window for ~3x write speed.
            s.execute("PRAGMA synchronous=NORMAL");
            // Keep 4 MB of WAL in memory before flushing.
            s.execute("PRAGMA cache_size=-4096");
        }
        return conn;
    }

    private void initSchema(@NotNull Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS audit_log (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    player_name TEXT,
                    world       TEXT NOT NULL,
                    x           INTEGER NOT NULL,
                    y           INTEGER NOT NULL,
                    z           INTEGER NOT NULL,
                    action      TEXT NOT NULL,
                    timestamp   INTEGER NOT NULL
                )
                """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_location ON audit_log(world, x, y, z)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player   ON audit_log(player_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_time     ON audit_log(timestamp)");
        }
    }

    /** Returns the next connection from the round-robin pool (thread-safe). */
    private synchronized Connection acquire() {
        Connection conn = pool[poolIdx % pool.length];
        poolIdx++;
        return conn;
    }

    /**
     * Records an event asynchronously via Bukkit's scheduler.
     * Never blocks the calling (main) thread.
     */
    public void log(@NotNull UUID player, @NotNull String playerName,
                    @NotNull Location loc, @NotNull Action action) {
        if (closed) return;
        final String world = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";
        final int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();
        final long ts = System.currentTimeMillis();
        BlockProt.getFoliaLib().getScheduler().runAsync(asyncTask -> {
            if (closed) return;
            try {
                pruneIfNeeded();
                Connection conn = acquire();
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO audit_log(player_uuid,player_name,world,x,y,z,action,timestamp) VALUES(?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, player.toString());
                    ps.setString(2, playerName);
                    ps.setString(3, world);
                    ps.setInt(4, bx);
                    ps.setInt(5, by);
                    ps.setInt(6, bz);
                    ps.setString(7, action.name());
                    ps.setLong(8, ts);
                    ps.executeUpdate();
                }
                writesSincePrune++;
            } catch (SQLException e) {
                BlockProt.getInstance().getLogger().warning(
                    Translator.get(TranslationKey.CONSOLE__AUDIT_LOGGER_FAILED)
                        .replace("{error}", e.getMessage()));
            }
        });
    }

    @NotNull
    public List<AuditEntry> getRecentEntries(int limit, int offset) {
        List<AuditEntry> entries = new ArrayList<>();
        try {
            Connection conn = acquire();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT ? OFFSET ?")) {
                ps.setInt(1, limit);
                ps.setInt(2, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) entries.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            BlockProt.getInstance().getLogger().warning(
                Translator.get(TranslationKey.CONSOLE__AUDIT_LOGGER_FAILED)
                    .replace("{error}", e.getMessage()));
        }
        return entries;
    }

    public int countEntries() {
        try {
            Connection conn = acquire();
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM audit_log")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            BlockProt.getInstance().getLogger().warning(
                Translator.get(TranslationKey.CONSOLE__AUDIT_LOGGER_FAILED)
                    .replace("{error}", e.getMessage()));
        }
        return 0;
    }

    @NotNull
    public List<AuditEntry> getEntriesForBlock(@NotNull String world, int x, int y, int z, int limit) {
        return getEntriesForBlock(world, x, y, z, limit, 0);
    }

    @NotNull
    public List<AuditEntry> getEntriesForBlock(@NotNull String world, int x, int y, int z, int limit, int offset) {
        List<AuditEntry> entries = new ArrayList<>();
        try {
            Connection conn = acquire();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM audit_log WHERE world=? AND x=? AND y=? AND z=? ORDER BY timestamp DESC LIMIT ? OFFSET ?")) {
                ps.setString(1, world);
                ps.setInt(2, x);
                ps.setInt(3, y);
                ps.setInt(4, z);
                ps.setInt(5, limit);
                ps.setInt(6, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) entries.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            BlockProt.getInstance().getLogger().warning(
                Translator.get(TranslationKey.CONSOLE__AUDIT_LOGGER_FAILED)
                    .replace("{error}", e.getMessage()));
        }
        return entries;
    }

    public int countEntriesForBlock(@NotNull String world, int x, int y, int z) {
        try {
            Connection conn = acquire();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM audit_log WHERE world=? AND x=? AND y=? AND z=?")) {
                ps.setString(1, world);
                ps.setInt(2, x);
                ps.setInt(3, y);
                ps.setInt(4, z);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            BlockProt.getInstance().getLogger().warning(
                Translator.get(TranslationKey.CONSOLE__AUDIT_LOGGER_FAILED)
                    .replace("{error}", e.getMessage()));
        }
        return 0;
    }

    @NotNull
    public List<AuditEntry> getEntriesForPlayer(@NotNull UUID player, int limit) {
        return getEntriesForPlayer(player, limit, 0);
    }

    @NotNull
    public List<AuditEntry> getEntriesForPlayer(@NotNull UUID player, int limit, int offset) {
        List<AuditEntry> entries = new ArrayList<>();
        try {
            Connection conn = acquire();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM audit_log WHERE player_uuid=? ORDER BY timestamp DESC LIMIT ? OFFSET ?")) {
                ps.setString(1, player.toString());
                ps.setInt(2, limit);
                ps.setInt(3, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) entries.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            BlockProt.getInstance().getLogger().warning(
                Translator.get(TranslationKey.CONSOLE__AUDIT_LOGGER_FAILED)
                    .replace("{error}", e.getMessage()));
        }
        return entries;
    }

    public int countEntriesForPlayer(@NotNull UUID player) {
        try {
            Connection conn = acquire();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM audit_log WHERE player_uuid=?")) {
                ps.setString(1, player.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            BlockProt.getInstance().getLogger().warning(
                Translator.get(TranslationKey.CONSOLE__AUDIT_LOGGER_FAILED)
                    .replace("{error}", e.getMessage()));
        }
        return 0;
    }

    private AuditEntry mapRow(ResultSet rs) throws SQLException {
        return new AuditEntry(
            rs.getLong("id"),
            rs.getString("player_uuid"),
            rs.getString("player_name"),
            rs.getString("world"),
            rs.getInt("x"), rs.getInt("y"), rs.getInt("z"),
            Action.valueOf(rs.getString("action")),
            rs.getLong("timestamp")
        );
    }

    /**
     * Prunes old entries when the table grows too large.
     * Only runs a COUNT(*) every {@link #PRUNE_CHECK_INTERVAL} writes to avoid overhead.
     */
    private synchronized void pruneIfNeeded() throws SQLException {
        if (writesSincePrune < PRUNE_CHECK_INTERVAL) return;
        writesSincePrune = 0;
        Connection conn = acquire();
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM audit_log");
            if (rs.next() && rs.getInt(1) > MAX_ENTRIES) {
                stmt.execute(
                    "DELETE FROM audit_log WHERE id IN (SELECT id FROM audit_log ORDER BY timestamp ASC LIMIT " + PRUNE_BATCH + ")");
            }
        }
    }

    public void close() {
        closed = true;
        for (Connection conn : pool) {
            try {
                if (conn != null && !conn.isClosed()) conn.close();
            } catch (SQLException ignored) {}
        }
    }
}