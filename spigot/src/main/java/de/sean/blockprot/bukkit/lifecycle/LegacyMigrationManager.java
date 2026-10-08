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

package de.sean.blockprot.bukkit.lifecycle;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtConsole;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public final class LegacyMigrationManager {

    public static final Set<String> EXTERNAL_CONFIG_KEYS = Set.of(
        "lockable_tile_entities", "lockable_shulker_boxes", "lockable_blocks", "lockable_doors",
        "lockable_entities", "protectable_entities", "entity_protection.protectable_entities",
        "auto_drop_to_inventory",
        "mysql.enabled", "mysql.host", "mysql.port", "mysql.database",
        "mysql.username", "mysql.password", "mysql.jdbc_url",
        "mysql.pool.maximum_pool_size", "mysql.pool.minimum_idle", "mysql.pool.connection_timeout_ms",
        "console.prefix_color", "console.info_color"
    );

    @Nullable private String pendingMigrationLog = null;
    private boolean pendingMigrationSuccess = false;
    @Nullable private String pendingMigrationError = null;
    private boolean isMigrationPerformed = false;
    private boolean firstStartThisSession = false;

    public boolean isMigrationPerformed() {
        return isMigrationPerformed;
    }

    public boolean isFirstStartThisSession() {
        return firstStartThisSession;
    }

    public boolean isFirstStart(@NotNull BlockProt plugin) {
        if (firstStartThisSession) return true;
        File marker = new File(plugin.getDataFolder(), ".first_start_done");
        firstStartThisSession = !marker.exists();
        return firstStartThisSession;
    }

    public void markFirstStartDone(@NotNull BlockProt plugin) {
        try {
            new File(plugin.getDataFolder(), ".first_start_done").createNewFile();
        } catch (IOException e) {
            BlockProtLogger.log("startup", "Could not create first-start marker: " + e.getMessage());
        }
    }

    public void migrateFromLegacyFolders(@NotNull BlockProt plugin) {
        if (new File(plugin.getDataFolder(), "config.yml").exists()) return;

        final String[] legacyNames = {"BlockProt", "BlockProtPlus"};
        final File pluginsDir = plugin.getDataFolder().getParentFile();
        if (pluginsDir == null) return;

        for (String legacyName : legacyNames) {
            File legacyFolder = new File(pluginsDir, legacyName);
            if (!legacyFolder.isDirectory()) continue;
            if (new File(legacyFolder, ".migrated").exists()) continue;
            if (!new File(legacyFolder, "config.yml").exists()) continue;

            pendingMigrationLog = legacyName;
            try {
                copyDirectoryContents(legacyFolder.toPath(), plugin.getDataFolder().toPath());
                plugin.saveResource("config.yml", true);
                applyLegacyConfigValues(
                    new File(legacyFolder, "config.yml"),
                    new File(plugin.getDataFolder(), "config.yml")
                );

                File legacyLang = new File(legacyFolder, "lang");
                File newLang = new File(plugin.getDataFolder(), "lang");
                if (legacyLang.isDirectory()) {
                    File[] langFiles = legacyLang.listFiles(
                        f -> f.isFile() && f.getName().endsWith(".yml"));
                    if (langFiles != null) {
                        for (File lf : langFiles) {
                            mergeYamlUserValues(lf, new File(newLang, lf.getName()));
                        }
                    }
                }

                Files.createFile(legacyFolder.toPath().resolve(".migrated"));
                pendingMigrationSuccess = true;
                isMigrationPerformed = true;
                markFirstStartDone(plugin);
            } catch (IOException e) {
                pendingMigrationError = e.getMessage();
            }
            break;
        }
    }

    public void flushMigrationLog(@NotNull BlockProt plugin) {
        if (pendingMigrationLog == null) return;
        String source = pendingMigrationLog;
        String target = plugin.getDataFolder().getName();
        if (pendingMigrationSuccess) {
            BlockProtConsole.info(Translator.get(TranslationKey.CONSOLE__MIGRATION_START)
                .replace("{source}", source).replace("{target}", target));
            BlockProtConsole.info(Translator.get(TranslationKey.CONSOLE__MIGRATION_DONE)
                .replace("{source}", source));
        } else {
            BlockProtConsole.warn(Translator.get(TranslationKey.CONSOLE__MIGRATION_FAILED)
                .replace("{source}", source)
                .replace("{error}", pendingMigrationError != null ? pendingMigrationError : "unknown"));
        }
        pendingMigrationLog = null;
        pendingMigrationSuccess = false;
        pendingMigrationError = null;
    }

    public void migrateLegacySqliteFile(@NotNull BlockProt plugin) {
        File legacyFile = new File(Bukkit.getWorldContainer(), "blockprot_usercache.sqlite");
        if (!legacyFile.exists()) return;
        File newFile = new File(plugin.getDataFolder(), "blockprot_usercache.sqlite");
        if (newFile.exists()) {
            if (!legacyFile.delete()) {
                plugin.getLogger().warning("Could not delete legacy blockprot_usercache.sqlite from server root.");
            }
            return;
        }
        try {
            Files.move(legacyFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            isMigrationPerformed = true;
            plugin.getLogger().info("Moved blockprot_usercache.sqlite into plugin data folder.");
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to move blockprot_usercache.sqlite: " + e.getMessage());
        }
    }

    public void migrateOldLockableListsFromConfigYml(@NotNull BlockProt plugin) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) return;

        File blocksFile = new File(plugin.getDataFolder(), "blocks.yml");
        if (blocksFile.exists()) return;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);

        String[] lockableKeys = {
            "lockable_tile_entities", "lockable_shulker_boxes",
            "lockable_blocks", "lockable_doors", "lockable_entities"
        };

        boolean hasAny = false;
        for (String key : lockableKeys) {
            Object val = cfg.get(key);
            if (val instanceof List<?> list && !list.isEmpty()) {
                hasAny = true;
                break;
            }
        }
        if (!hasAny) return;

        YamlConfiguration blocks = new YamlConfiguration();
        for (String key : lockableKeys) {
            blocks.set(key, cfg.getList(key, Collections.emptyList()));
        }
        blocks.set("auto_drop_to_inventory.enabled",
            cfg.getBoolean("auto_drop_to_inventory.enabled", true));
        blocks.set("auto_drop_to_inventory.blocks",
            cfg.getList("auto_drop_to_inventory.blocks", Collections.emptyList()));

        try {
            blocks.save(blocksFile);
            BlockProtLogger.log("migrate-blocks",
                "Created blocks.yml with lockable lists migrated from config.yml.");

            boolean changed = false;
            for (String key : lockableKeys) {
                if (cfg.contains(key)) {
                    cfg.set(key, null);
                    changed = true;
                }
            }
            if (cfg.contains("auto_drop_to_inventory")) {
                cfg.set("auto_drop_to_inventory", null);
                changed = true;
            }
            if (changed) {
                cfg.save(configFile);
                BlockProtLogger.log("migrate-blocks",
                    "Cleaned up lockable lists from config.yml (now in blocks.yml).");
            }
        } catch (IOException e) {
            BlockProtLogger.warn("Failed to migrate lockable lists from config.yml: " + e.getMessage());
        }
    }

    public void cleanLegacyConfigKeys(@NotNull BlockProt plugin) {
        File diskFile = new File(plugin.getDataFolder(), "config.yml");
        if (!diskFile.exists()) return;

        YamlConfiguration userValues = YamlConfiguration.loadConfiguration(diskFile);

        InputStream jarStream = plugin.getResource("config.yml");
        if (jarStream == null) return;
        YamlConfiguration template = YamlConfiguration.loadConfiguration(
            new BufferedReader(new InputStreamReader(jarStream, StandardCharsets.UTF_8)));

        boolean dirty = false;

        if (userValues.contains("worlds_config_enabled") && !userValues.contains("per_worlds_config")) {
            userValues.set("per_worlds_config", userValues.getBoolean("worlds_config_enabled", false));
            userValues.set("worlds_config_enabled", null);
            dirty = true;
            BlockProtLogger.log("config-migrate", "Migrated 'worlds_config_enabled' -> 'per_worlds_config'");
        }
        if (userValues.contains("pet_protection") && !userValues.contains("entity_protection")) {
            userValues.set("entity_protection.enabled", userValues.getBoolean("pet_protection.enabled", false));
            userValues.set("entity_protection.auto_protect_on_tame",
                userValues.getBoolean("pet_protection.auto_protect_on_tame", true));
            userValues.set("entity_protection.menu_item",
                userValues.getString("pet_protection.menu_item", "STICK"));
            userValues.set("entity_protection.villager_locate_seconds",
                userValues.getInt("pet_protection.villager_locate_seconds", 6));
            userValues.set("pet_protection", null);
            dirty = true;
            BlockProtLogger.log("config-migrate", "Migrated 'pet_protection' -> 'entity_protection'");
        }

        int added = 0;
        for (String key : template.getKeys(true)) {
            if (template.isConfigurationSection(key)) continue;
            if (EXTERNAL_CONFIG_KEYS.contains(key)) continue;
            if (!userValues.contains(key)) {
                userValues.set(key, template.get(key));
                added++;
                dirty = true;
            }
        }

        if (!dirty) return;
        try {
            if (plugin.getFileWatcher() != null) plugin.getFileWatcher().suppressNext();
            userValues.save(diskFile);
            BlockProtLogger.log("config-clean",
                "config.yml merged: all user values preserved" +
                (added > 0 ? ", added " + added + " missing key(s) from template." : "."));
        } catch (IOException e) {
            BlockProtLogger.warn("Failed to save config.yml after merge: " + e.getMessage());
        }
    }

    public void mergeMissingConfigKeys(@NotNull BlockProt plugin) {
        File diskFile = new File(plugin.getDataFolder(), "config.yml");
        if (!diskFile.exists()) return;
        InputStream jarStream = plugin.getResource("config.yml");
        if (jarStream == null) return;

        YamlConfiguration jarConfig = YamlConfiguration.loadConfiguration(new BufferedReader(new InputStreamReader(jarStream, StandardCharsets.UTF_8)));
        YamlConfiguration diskConfig = YamlConfiguration.loadConfiguration(diskFile);
        int added = 0;
        for (String key : jarConfig.getKeys(true)) {
            if (jarConfig.isConfigurationSection(key)) continue;
            if (EXTERNAL_CONFIG_KEYS.contains(key)) continue;
            if (!diskConfig.contains(key)) {
                diskConfig.set(key, jarConfig.get(key));
                added++;
                BlockProtLogger.log("config-merge", "config.yml: added missing key: " + key);
            }
        }

        if (added == 0) return;
        try {
            if (plugin.getFileWatcher() != null) plugin.getFileWatcher().suppressNext();
            diskConfig.save(diskFile);
            BlockProtLogger.log("config-merge", "config.yml: added " + added + " missing option(s).");
        } catch (IOException e) {
            BlockProtConsole.warn(Translator.get(TranslationKey.CONSOLE__CONFIG_KEYS_SAVE_FAILED)
                .replace("{error}", e.getMessage()));
            BlockProtLogger.log("config-merge", "Failed to save config.yml after merge: " + e.getMessage());
        }
    }

    private static void applyLegacyConfigValues(@NotNull File legacySrc, @NotNull File freshDst) throws IOException {
        if (!legacySrc.exists() || !freshDst.exists()) return;
        YamlConfiguration legacyCfg = YamlConfiguration.loadConfiguration(legacySrc);
        YamlConfiguration freshCfg = YamlConfiguration.loadConfiguration(freshDst);
        boolean changed = false;
        for (String key : legacyCfg.getKeys(true)) {
            if (legacyCfg.isConfigurationSection(key)) continue;
            freshCfg.set(key, legacyCfg.get(key));
            changed = true;
        }
        if (changed) freshCfg.save(freshDst);
    }

    private static void mergeYamlUserValues(@NotNull File src, @NotNull File dst) throws IOException {
        if (!src.exists()) return;
        YamlConfiguration srcCfg = YamlConfiguration.loadConfiguration(src);
        if (!dst.exists()) {
            Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            return;
        }
        YamlConfiguration dstCfg = YamlConfiguration.loadConfiguration(dst);
        boolean changed = false;
        for (String key : srcCfg.getKeys(true)) {
            if (srcCfg.isConfigurationSection(key)) continue;
            if (!dstCfg.contains(key)) {
                dstCfg.set(key, srcCfg.get(key));
                changed = true;
            }
        }
        if (changed) dstCfg.save(dst);
    }

    private static void copyDirectoryContents(@NotNull Path src, @NotNull Path dst) throws IOException {
        Files.walkFileTree(src, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path target = dst.resolve(src.relativize(dir));
                if (!Files.exists(target)) Files.createDirectories(target);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().equals(".migrated")) return FileVisitResult.CONTINUE;
                if (src.relativize(file).toString().equals("config.yml")) return FileVisitResult.CONTINUE;
                Path target = dst.resolve(src.relativize(file));
                if (!Files.exists(target)) {
                    Files.copy(file, target, StandardCopyOption.COPY_ATTRIBUTES);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
