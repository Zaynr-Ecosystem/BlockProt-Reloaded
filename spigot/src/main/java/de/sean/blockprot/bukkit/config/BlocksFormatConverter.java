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

package de.sean.blockprot.bukkit.config;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class BlocksFormatConverter {

    private static final String AUTO_DROP_BLOCKS_KEY_LOCAL = "auto_drop_to_inventory.blocks";

    private BlocksFormatConverter() {
    }

    public static void mergeMissingBlocksKeys(@NotNull BlockProt plugin) {
        String blocksPath = plugin.getConfig().getString("blocks_file", "blocks.yml");
        File diskFile = new File(plugin.getDataFolder(), blocksPath);
        if (!diskFile.exists()) return;
        InputStream jarStream = plugin.getResource("blocks.yml");
        if (jarStream == null) return;

        YamlConfiguration jarConfig = YamlConfiguration.loadConfiguration(
            new BufferedReader(new InputStreamReader(jarStream, StandardCharsets.UTF_8)));

        YamlConfiguration diskConfig = new YamlConfiguration();
        try {
            diskConfig.load(diskFile);
        } catch (IOException | InvalidConfigurationException ex) {
            boolean modern = plugin.getConfig().getBoolean("modern_family_blocks", false);
            BlocksYamlRepair.handleCorruptBlocksYaml(plugin, diskFile, jarConfig, ex, plugin.getFileWatcher(), modern);
            return;
        }
        processBlocksMerge(plugin, diskFile, jarConfig, diskConfig);
    }

    public static void processBlocksMerge(
        @NotNull BlockProt plugin,
        @NotNull File diskFile,
        @NotNull YamlConfiguration jarConfig,
        @NotNull YamlConfiguration diskConfig
    ) {
        int added = 0;

        for (String key : jarConfig.getKeys(false)) {
            if (!diskConfig.contains(key)) {
                diskConfig.set(key, jarConfig.get(key));
                added++;
                BlockProtLogger.log("blocks-merge", "blocks.yml: added missing section " + key);
            }
        }

        boolean cleaned = false;
        String[] listKeys = {
            "lockable_tile_entities", "lockable_shulker_boxes",
            "lockable_blocks", "lockable_doors", "lockable_entities",
            "protectable_entities",
            "auto_drop_to_inventory.blocks"
        };
        for (String key : listKeys) {
            if (!diskConfig.contains(key)) continue;
            List<?> raw = diskConfig.getList(key);
            if (raw == null || raw.isEmpty()) continue;

            boolean hasRealEntry = false;
            for (Object o : raw) {
                if (DefaultConfig.isPlaceholderEntry(o)) continue;
                hasRealEntry = true;
                break;
            }
            if (!hasRealEntry) continue;

            List<Object> filtered = new ArrayList<>();
            for (Object o : raw) {
                if (DefaultConfig.isPlaceholderEntry(o)) continue;
                filtered.add(o);
            }
            if (filtered.size() != raw.size()) {
                diskConfig.set(key, filtered);
                cleaned = true;
                BlockProtLogger.log("blocks-clean", "Removed placeholder entries from " + key);
            }
        }

        if (added == 0 && !cleaned) return;
        try {
            if (plugin.getFileWatcher() != null) plugin.getFileWatcher().suppressNext();
            boolean modern = plugin.getConfig().getBoolean("modern_family_blocks", false);
            DefaultConfig.sanitizeBlocksListsForSave(diskConfig, modern);
            diskConfig = DefaultConfig.reorderBlocksKeys(diskConfig);
            diskConfig.save(diskFile);
            DefaultConfig.prependBlocksHeader(diskFile);
            if (added > 0) {
                BlockProtLogger.log("blocks-merge", "blocks.yml: merged " + added + " new section(s) from JAR.");
            }
        } catch (IOException e) {
            BlockProtLogger.warn("Failed to save blocks.yml after merge: " + e.getMessage());
        }
    }

    public static void convertBlocksFormatIfNeeded(@NotNull BlockProt plugin) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        boolean modern = false;
        if (configFile.exists()) {
            modern = YamlConfiguration.loadConfiguration(configFile).getBoolean("modern_family_blocks", false);
        }

        String blocksPath = plugin.getConfig().getString("blocks_file", "blocks.yml");
        File blocksFile = new File(plugin.getDataFolder(), blocksPath);
        if (blocksFile.exists()) {
            YamlConfiguration blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);
            boolean blocksDirty = convertConfigurationListsIfNeeded(blocksConfig, "", modern);

            if (blocksConfig.contains(AUTO_DROP_BLOCKS_KEY_LOCAL)) {
                Object raw = blocksConfig.get(AUTO_DROP_BLOCKS_KEY_LOCAL);
                if (raw instanceof List<?> list) {
                    boolean autoDropNeedsConversion = false;
                    if (!modern) {
                        for (Object o : list) {
                            if (o instanceof String s && BlockFamilyParser.isFamilyExpression(s.trim())) {
                                autoDropNeedsConversion = true;
                                break;
                            }
                        }
                    }

                    if (autoDropNeedsConversion) {
                        Set<Material> autoDropMaterials = new LinkedHashSet<>();
                        for (Object o : list) {
                            if (DefaultConfig.isPlaceholderEntry(o)) continue;
                            if (o instanceof String s) {
                                String trimmed = s.trim();
                                if (BlockFamilyParser.isFamilyExpression(trimmed)) {
                                    for (BlockFamilyParser.Family f : BlockFamilyParser.Family.values()) {
                                        autoDropMaterials.addAll(BlockFamilyParser.parseFamilyExpressionSilent(trimmed, f));
                                    }
                                } else {
                                    Material m = Material.matchMaterial(trimmed);
                                    if (m != null) autoDropMaterials.add(m);
                                }
                            }
                        }
                        blocksConfig.set(AUTO_DROP_BLOCKS_KEY_LOCAL,
                            autoDropMaterials.stream().map(Material::name).sorted().toList());
                        blocksDirty = true;
                    }
                }
            }

            if (blocksDirty) {
                try {
                    if (plugin.getFileWatcher() != null) plugin.getFileWatcher().suppressNext();
                    DefaultConfig.sanitizeListsForSave(blocksConfig, "", modern);
                    blocksConfig = DefaultConfig.reorderBlocksKeys(blocksConfig);
                    blocksConfig.save(blocksFile);
                    DefaultConfig.prependBlocksHeader(blocksFile);
                    BlockProtLogger.log("blocks-convert",
                        "blocks.yml: converted format to " + (modern ? "family expressions" : "flat names") + ".");
                } catch (IOException e) {
                    BlockProtLogger.warn("Failed to save blocks.yml after format conversion: " + e.getMessage());
                }
            }
        }

        File worldsFile = new File(plugin.getDataFolder(), "worlds.yml");
        if (worldsFile.exists()) {
            YamlConfiguration worldsConfig = YamlConfiguration.loadConfiguration(worldsFile);
            ConfigurationSection worldsSection = worldsConfig.getConfigurationSection("worlds");
            if (worldsSection != null) {
                boolean worldsDirty = false;
                for (String worldName : worldsSection.getKeys(false)) {
                    String prefix = "worlds." + worldName + ".";
                    if (convertConfigurationListsIfNeeded(worldsConfig, prefix, modern)) {
                        DefaultConfig.sanitizeListsForSave(worldsConfig, prefix, modern);
                        worldsDirty = true;
                    }
                }
                if (worldsDirty) {
                    try {
                        if (plugin.getFileWatcher() != null) plugin.getFileWatcher().suppressNext();
                        worldsConfig.save(worldsFile);
                        DefaultConfig.cleanNullPlaceholderLines(worldsFile);
                        BlockProtLogger.log("worlds-convert",
                            "worlds.yml: converted format to " + (modern ? "family expressions" : "flat names") + ".");
                    } catch (IOException e) {
                        BlockProtLogger.warn("Failed to save worlds.yml after format conversion: " + e.getMessage());
                    }
                }
            }
        }
    }

    private static boolean convertConfigurationListsIfNeeded(@NotNull YamlConfiguration cfg, @NotNull String prefix, boolean modern) {
        Map<String, BlockFamilyParser.Family> keyFamilies = new LinkedHashMap<>();
        keyFamilies.put("lockable_tile_entities", BlockFamilyParser.Family.TILE_ENTITIES);
        keyFamilies.put("lockable_shulker_boxes", BlockFamilyParser.Family.SHULKER_BOXES);
        keyFamilies.put("lockable_blocks", BlockFamilyParser.Family.BLOCKS);
        keyFamilies.put("lockable_doors", BlockFamilyParser.Family.DOORS);
        keyFamilies.put("lockable_entities", BlockFamilyParser.Family.ENTITIES);

        boolean needsConversion = false;
        for (String key : keyFamilies.keySet()) {
            String fullKey = prefix + key;
            if (!cfg.contains(fullKey)) continue;
            Object raw = cfg.get(fullKey);
            if (!(raw instanceof List<?> list)) continue;

            boolean hasRealEntry = false;
            for (Object o : list) {
                if (DefaultConfig.isPlaceholderEntry(o)) continue;
                hasRealEntry = true;
                if (o instanceof String s) {
                    boolean isExpr = BlockFamilyParser.isFamilyExpression(s.trim());
                    if (isExpr != modern) {
                        needsConversion = true;
                        break;
                    }
                }
            }
            if (needsConversion) break;

            if (!hasRealEntry) {
                boolean alreadyNormalized = modern
                    ? (list.size() == 1 && "[]".equals(String.valueOf(list.get(0)).trim()))
                    : (list.size() == 2 && list.get(0) == null && list.get(1) == null);
                if (!alreadyNormalized) {
                    needsConversion = true;
                    break;
                }
            }
        }

        if (!needsConversion) return false;

        for (Map.Entry<String, BlockFamilyParser.Family> entry : keyFamilies.entrySet()) {
            String key = entry.getKey();
            String fullKey = prefix + key;
            BlockFamilyParser.Family family = entry.getValue();
            if (!cfg.contains(fullKey)) continue;

            Set<Material> materials = BlockFamilyParser.parse(cfg.get(fullKey), family);
            if (modern) {
                String expr = BlockFamilyParser.toFamilyExpression(materials, family);
                if (expr != null) {
                    cfg.set(fullKey, List.of(expr));
                } else {
                    cfg.set(fullKey, materials.stream().map(Material::name).sorted().toList());
                }
            } else {
                cfg.set(fullKey, materials.stream().map(Material::name).sorted().toList());
            }
        }
        return true;
    }
}
