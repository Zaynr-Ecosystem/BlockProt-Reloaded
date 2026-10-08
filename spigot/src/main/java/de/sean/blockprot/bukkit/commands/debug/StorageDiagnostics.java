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

package de.sean.blockprot.bukkit.commands.debug;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class StorageDiagnostics {

    private StorageDiagnostics() {
    }

    public static void run(@NotNull DiagnosticsContext ctx) {
        DiagnosticsContext.runDomain("4/10", "STORAGE, DATABASE & CACHING");
        checkHybridDatabase(ctx);
        checkProfileService(ctx);
        checkSkinsRestorer(ctx);
        checkAuditLogger(ctx);
        checkOnlinePlayers(ctx);
    }

    private static void checkHybridDatabase(@NotNull DiagnosticsContext ctx) {
        try {
            var db = BlockProt.getHybridDatabase();
            if (db != null) {
                BlockProtLogger.pass("HybridDatabase: active, backend=" + (BlockProt.getDefaultConfig().isMysqlEnabled() ? "MySQL" : "SQLite"));
                ctx.pass();
            } else {
                BlockProtLogger.fail("HybridDatabase", "Database instance is null");
                ctx.fail();
            }
        } catch (Exception e) {
            BlockProtLogger.fail("HybridDatabase", e.getMessage());
            ctx.fail();
        }
    }

    private static void checkProfileService(@NotNull DiagnosticsContext ctx) {
        try {
            var service = BlockProt.getProfileService();
            if (service == null) {
                BlockProtLogger.fail("ProfileService", "ProfileService instance is null");
                ctx.fail();
                return;
            }
            if (ctx.player() != null) {
                var profile = service.findByUuid(ctx.player().getUniqueId());
                BlockProtLogger.pass("ProfileService: active, found=" + (profile != null ? profile.getName() : "null (no exception)"));
            } else {
                BlockProtLogger.pass("ProfileService: active (headless verification)");
            }
            ctx.pass();
        } catch (Exception e) {
            BlockProtLogger.fail("ProfileService", e.getMessage());
            ctx.fail();
        }
    }

    private static void checkSkinsRestorer(@NotNull DiagnosticsContext ctx) {
        var plugin = Bukkit.getPluginManager().getPlugin("SkinsRestorer");
        if (plugin == null) {
            BlockProtLogger.pass("SkinsRestorer: not installed");
        } else if (!plugin.isEnabled()) {
            BlockProtLogger.pass("SkinsRestorer: installed but disabled");
        } else {
            String ver;
            try {
                ver = plugin.getPluginMeta().getVersion();
            } catch (NoSuchMethodError e) {
                @SuppressWarnings("deprecation")
                String fallback = plugin.getDescription().getVersion();
                ver = fallback;
            }
            BlockProtLogger.pass("SkinsRestorer: active v" + ver);
        }
        ctx.pass();
    }

    private static void checkAuditLogger(@NotNull DiagnosticsContext ctx) {
        var audit = BlockProt.getAuditLogger();
        BlockProtLogger.pass("AuditLogger: " + (audit == null ? "disabled (config)" : "active"));
        ctx.pass();
    }

    private static void checkOnlinePlayers(@NotNull DiagnosticsContext ctx) {
        var players = Bukkit.getOnlinePlayers();
        StringBuilder sb = new StringBuilder();
        for (Player pl : players) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(pl.getName()).append(" (").append(pl.getUniqueId()).append(")");
        }
        BlockProtLogger.pass("Online players (" + players.size() + "): " + sb);
        ctx.pass();
    }
}
