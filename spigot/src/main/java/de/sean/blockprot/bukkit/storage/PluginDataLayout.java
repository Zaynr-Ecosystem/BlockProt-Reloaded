/*
 * Copyright (C) 2026 Zar (VictorGugug)
 * This file is part of BlockProt Reloaded <https://github.com/VictorGugug/BlockProt-Reloaded>.
 *
 * BlockProt Reloaded is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BlockProt Reloaded is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlockProt Reloaded.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.sean.blockprot.bukkit.storage;

import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.Edition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Locale;
import java.util.Properties;

/**
 * Centralizes internal storage paths under {@code <dataFolder>/data/} and
 * transparently migrates legacy root databases and dot-marker files.
 */
public final class PluginDataLayout {

    public static final String DATA_DIR_NAME = "data";
    public static final String USERCACHE_FILE_NAME = "usercache.sqlite";
    public static final String AUDIT_DB_FILE_NAME = "audit.sqlite";
    public static final String STATE_FILE_NAME = "state.yml";

    public static final String LEGACY_USERCACHE_NAME = "blockprot_usercache.sqlite";
    public static final String LEGACY_AUDIT_DIR_NAME = "mysql";
    public static final String LEGACY_AUDIT_DB_NAME = "blockprot_audit.sqlite";
    public static final String LEGACY_EDITION_FILE = ".edition";
    public static final String LEGACY_FIRST_START_FILE = ".first_start_done";
    public static final String LEGACY_RECOMMENDED_FILE = ".recommended_state.yml";

    private static final String[] SQLITE_SUFFIXES = {"", "-wal", "-shm"};

    private PluginDataLayout() {}

    public static @NotNull File dataDir(@NotNull File pluginDataFolder) {
        return new File(pluginDataFolder, DATA_DIR_NAME);
    }

    public static @NotNull File userCacheFile(@NotNull File pluginDataFolder) {
        return new File(dataDir(pluginDataFolder), USERCACHE_FILE_NAME);
    }

    public static @NotNull File auditDbFile(@NotNull File pluginDataFolder) {
        return new File(dataDir(pluginDataFolder), AUDIT_DB_FILE_NAME);
    }

    public static @NotNull File stateFile(@NotNull File pluginDataFolder) {
        return new File(dataDir(pluginDataFolder), STATE_FILE_NAME);
    }

    public static boolean isManagedInternalFile(@NotNull File dataDir, @NotNull File candidate) {
        if (!candidate.getParentFile().equals(dataDir)) return false;
        String lower = candidate.getName().toLowerCase(Locale.ROOT);
        for (String suffix : SQLITE_SUFFIXES) {
            if (lower.equals(USERCACHE_FILE_NAME + suffix)) return true;
        }
        return lower.equals(STATE_FILE_NAME);
    }

    public static boolean hasAuditDatabase(@NotNull File pluginDataFolder) {
        if (auditDbFile(pluginDataFolder).exists()) return true;
        if (new File(new File(pluginDataFolder, LEGACY_AUDIT_DIR_NAME), LEGACY_AUDIT_DB_NAME).exists()) return true;
        return new File(pluginDataFolder, LEGACY_AUDIT_DB_NAME).exists();
    }

    /**
     * Migrates legacy SQLite files and dot-marker files into {@code data/}.
     *
     * @return {@code true} if an older database or state file was moved.
     */
    public static boolean migrateAll(@NotNull File pluginDataFolder, @Nullable File serverRoot) {
        boolean migrated = false;
        if (migrateUserCache(pluginDataFolder, serverRoot)) migrated = true;
        if (migrateAuditDb(pluginDataFolder)) migrated = true;
        if (migrateMarkers(pluginDataFolder)) migrated = true;
        return migrated;
    }

    public static boolean migrateUserCache(@NotNull File pluginDataFolder, @Nullable File serverRoot) {
        File target = userCacheFile(pluginDataFolder);
        boolean moved = false;

        File pluginRootLegacy = new File(pluginDataFolder, LEGACY_USERCACHE_NAME);
        if (moveSqliteWithCompanions(pluginRootLegacy, target)) {
            moved = true;
        }

        if (serverRoot != null) {
            File serverRootLegacy = new File(serverRoot, LEGACY_USERCACHE_NAME);
            if (moveSqliteWithCompanions(serverRootLegacy, target)) {
                moved = true;
            }
        }
        return moved;
    }

    public static boolean migrateAuditDb(@NotNull File pluginDataFolder) {
        File target = auditDbFile(pluginDataFolder);
        boolean moved = false;

        File legacyDir = new File(pluginDataFolder, LEGACY_AUDIT_DIR_NAME);
        File legacyDirDb = new File(legacyDir, LEGACY_AUDIT_DB_NAME);
        if (moveSqliteWithCompanions(legacyDirDb, target)) {
            moved = true;
        }

        File rootLegacyDb = new File(pluginDataFolder, LEGACY_AUDIT_DB_NAME);
        if (moveSqliteWithCompanions(rootLegacyDb, target)) {
            moved = true;
        }

        if (legacyDir.isDirectory()) {
            String[] remaining = legacyDir.list();
            if (remaining != null && remaining.length == 0) {
                legacyDir.delete();
            }
        }
        return moved;
    }

    public static boolean migrateMarkers(@NotNull File pluginDataFolder) {
        File firstStartMarker = new File(pluginDataFolder, LEGACY_FIRST_START_FILE);
        File recommendedMarker = new File(pluginDataFolder, LEGACY_RECOMMENDED_FILE);
        File editionMarker = new File(pluginDataFolder, LEGACY_EDITION_FILE);

        boolean hasFirstStart = firstStartMarker.isFile();
        boolean hasRecommended = recommendedMarker.isFile();
        boolean hasEdition = editionMarker.isFile();
        if (!hasFirstStart && !hasRecommended && !hasEdition) {
            return false;
        }

        YamlConfiguration state = loadState(pluginDataFolder);
        boolean dirty = false;

        if (hasFirstStart) {
            if (!state.getBoolean("first_start_done", false)) {
                state.set("first_start_done", true);
                dirty = true;
            }
        }

        if (hasRecommended) {
            YamlConfiguration oldRec = YamlConfiguration.loadConfiguration(recommendedMarker);
            for (String key : oldRec.getKeys(false)) {
                String targetKey = "recommended." + key;
                if (!state.contains(targetKey)) {
                    state.set(targetKey, oldRec.get(key));
                    dirty = true;
                }
            }
        }

        if (hasEdition && !state.contains("edition.id")) {
            Properties props = readLegacyEditionProperties(editionMarker);
            if (props != null && props.containsKey("edition")) {
                state.set("edition.id", props.getProperty("edition"));
                state.set("edition.version", props.getProperty("version", ""));
                state.set("edition.minecraft", props.getProperty("minecraft", ""));
                if (props.containsKey("written")) {
                    state.set("edition.written", props.getProperty("written"));
                }
                dirty = true;
            }
        }

        boolean saved = !dirty || saveState(pluginDataFolder, state);
        if (saved) {
            if (hasFirstStart) firstStartMarker.delete();
            if (hasRecommended) recommendedMarker.delete();
            if (hasEdition) editionMarker.delete();
        }
        return dirty && saved;
    }

    public static boolean isFirstStartDone(@NotNull File pluginDataFolder) {
        if (new File(pluginDataFolder, LEGACY_FIRST_START_FILE).exists()) {
            return true;
        }
        YamlConfiguration state = loadState(pluginDataFolder);
        return state.getBoolean("first_start_done", false);
    }

    public static void markFirstStartDone(@NotNull File pluginDataFolder) {
        YamlConfiguration state = loadState(pluginDataFolder);
        state.set("first_start_done", true);
        saveState(pluginDataFolder, state);
        File legacy = new File(pluginDataFolder, LEGACY_FIRST_START_FILE);
        if (legacy.exists()) {
            legacy.delete();
        }
    }

    public static boolean isRecommendedApplied(@NotNull File pluginDataFolder, @NotNull String key) {
        YamlConfiguration state = loadState(pluginDataFolder);
        if (state.contains("recommended." + key)) {
            return state.getBoolean("recommended." + key, false);
        }
        File legacy = new File(pluginDataFolder, LEGACY_RECOMMENDED_FILE);
        if (!legacy.isFile()) return false;
        return YamlConfiguration.loadConfiguration(legacy).getBoolean(key, false);
    }

    public static void setRecommendedApplied(@NotNull File pluginDataFolder, @NotNull String key, boolean value) {
        migrateMarkers(pluginDataFolder);
        YamlConfiguration state = loadState(pluginDataFolder);
        state.set("recommended." + key, value);
        saveState(pluginDataFolder, state);
    }

    public static @Nullable String getLastSeenChangelogVersion(@NotNull File pluginDataFolder) {
        YamlConfiguration state = loadState(pluginDataFolder);
        String value = state.getString("changelog.last_seen_version");
        return (value == null || value.isBlank()) ? null : value;
    }

    public static void markChangelogSeen(@NotNull File pluginDataFolder, @NotNull String version) {
        YamlConfiguration state = loadState(pluginDataFolder);
        state.set("changelog.last_seen_version", version);
        saveState(pluginDataFolder, state);
    }

    public static @Nullable Properties readEditionMarker(@NotNull File pluginDataFolder) {
        YamlConfiguration state = loadState(pluginDataFolder);
        ConfigurationSection section = state.getConfigurationSection("edition");
        if (section != null && section.contains("id")) {
            Properties props = new Properties();
            props.setProperty("edition", section.getString("id", ""));
            props.setProperty("version", section.getString("version", ""));
            props.setProperty("minecraft", section.getString("minecraft", ""));
            String written = section.getString("written");
            if (written != null) {
                props.setProperty("written", written);
            }
            return props;
        }
        return readLegacyEditionProperties(new File(pluginDataFolder, LEGACY_EDITION_FILE));
    }

    public static void writeEditionMarker(@NotNull File pluginDataFolder, @NotNull Edition edition,
                                          @NotNull String version, @NotNull String minecraft) {
        YamlConfiguration state = loadState(pluginDataFolder);
        state.set("edition.id", edition.id());
        state.set("edition.version", version);
        state.set("edition.minecraft", minecraft);
        state.set("edition.written", Instant.now().toString());
        if (saveState(pluginDataFolder, state)) {
            File legacy = new File(pluginDataFolder, LEGACY_EDITION_FILE);
            if (legacy.exists()) {
                legacy.delete();
            }
        }
    }

    private static @Nullable Properties readLegacyEditionProperties(@NotNull File file) {
        if (!file.isFile()) return null;
        try (FileInputStream in = new FileInputStream(file)) {
            Properties properties = new Properties();
            properties.load(in);
            return properties;
        } catch (IOException | IllegalArgumentException e) {
            BlockProtLogger.log("backup", "Edition marker unreadable: " + e.getMessage());
            return null;
        }
    }

    private static @NotNull YamlConfiguration loadState(@NotNull File pluginDataFolder) {
        File file = stateFile(pluginDataFolder);
        if (!file.isFile()) {
            return new YamlConfiguration();
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private static boolean saveState(@NotNull File pluginDataFolder, @NotNull YamlConfiguration state) {
        File dir = dataDir(pluginDataFolder);
        if (!dir.isDirectory() && !dir.mkdirs()) {
            BlockProtLogger.warn("Failed to create data directory: " + dir.getAbsolutePath());
            return false;
        }
        try {
            state.save(stateFile(pluginDataFolder));
            return true;
        } catch (IOException e) {
            BlockProtLogger.warn("Failed to save data/state.yml: " + e.getMessage());
            return false;
        }
    }

    private static boolean moveSqliteWithCompanions(@NotNull File sourceDb, @NotNull File targetDb) {
        if (!sourceDb.exists()) return false;

        File targetDir = targetDb.getParentFile();
        if (targetDir != null && !targetDir.isDirectory() && !targetDir.mkdirs()) {
            return false;
        }

        if (targetDb.exists()) {
            for (String suffix : SQLITE_SUFFIXES) {
                File oldPart = new File(sourceDb.getParentFile(), sourceDb.getName() + suffix);
                if (oldPart.exists()) {
                    oldPart.delete();
                }
            }
            return false;
        }

        try {
            Files.move(sourceDb.toPath(), targetDb.toPath(), StandardCopyOption.REPLACE_EXISTING);
            for (int i = 1; i < SQLITE_SUFFIXES.length; i++) {
                String suffix = SQLITE_SUFFIXES[i];
                File oldSide = new File(sourceDb.getParentFile(), sourceDb.getName() + suffix);
                if (oldSide.exists()) {
                    File newSide = new File(targetDb.getParentFile(), targetDb.getName() + suffix);
                    Files.move(oldSide.toPath(), newSide.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return true;
        } catch (IOException e) {
            BlockProtLogger.warn("Failed to migrate SQLite file " + sourceDb.getName() + ": " + e.getMessage());
            return false;
        }
    }
}
