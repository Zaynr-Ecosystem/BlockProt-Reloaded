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

import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public final class BlocksConfigModifier {

    private BlocksConfigModifier() {}

    @Nullable
    public static String configKeyForMaterial(@NotNull Material material) {
        for (BlockFamilyParser.Family family : BlockFamilyParser.Family.values()) {
            if (BlockFamilyParser.getFamilyMembers(family).contains(material)) {
                return configKeyForFamily(family);
            }
        }
        return null;
    }

    @Nullable
    public static String configKeyForFamily(@NotNull BlockFamilyParser.Family family) {
        return switch (family) {
            case TILE_ENTITIES -> "lockable_tile_entities";
            case SHULKER_BOXES -> "lockable_shulker_boxes";
            case BLOCKS -> "lockable_blocks";
            case DOORS -> "lockable_doors";
            case ENTITIES -> "lockable_entities";
        };
    }

    @Nullable
    public static BlockFamilyParser.Family familyForMaterial(@NotNull Material material) {
        for (BlockFamilyParser.Family family : BlockFamilyParser.Family.values()) {
            if (BlockFamilyParser.getFamilyMembers(family).contains(material)) {
                return family;
            }
        }
        return null;
    }

    @Nullable
    public static BlockFamilyParser.Family familyForConfigKey(@NotNull String configKey) {
        return switch (configKey) {
            case "lockable_tile_entities" -> BlockFamilyParser.Family.TILE_ENTITIES;
            case "lockable_shulker_boxes" -> BlockFamilyParser.Family.SHULKER_BOXES;
            case "lockable_blocks" -> BlockFamilyParser.Family.BLOCKS;
            case "lockable_doors" -> BlockFamilyParser.Family.DOORS;
            case "lockable_entities" -> BlockFamilyParser.Family.ENTITIES;
            default -> null;
        };
    }

    @NotNull
    public static Material representativeMaterialForFamily(@NotNull BlockFamilyParser.Family family) {
        return switch (family) {
            case TILE_ENTITIES -> Material.CHEST;
            case SHULKER_BOXES -> Material.SHULKER_BOX;
            case BLOCKS -> Material.ANVIL;
            case DOORS -> Material.OAK_DOOR;
            case ENTITIES -> Material.ITEM_FRAME;
        };
    }

    public static boolean isKeyInExpressionFormat(
            @Nullable YamlConfiguration blocksConfig,
            boolean modernFamilyBlocks,
            @NotNull String configKey) {
        if (blocksConfig == null) return modernFamilyBlocks;
        Object raw = blocksConfig.get(configKey);
        if (raw instanceof String s) {
            String trimmed = s.trim();
            if (!BlocksConfigStorage.isPlaceholderEntry(trimmed)) {
                return BlockFamilyParser.isFamilyExpression(trimmed);
            }
        }
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (!(o instanceof String s)) continue;
                String trimmed = s.trim();
                if (BlocksConfigStorage.isPlaceholderEntry(trimmed)) continue;
                return BlockFamilyParser.isFamilyExpression(trimmed);
            }
        }
        return modernFamilyBlocks;
    }

    public static boolean hasConfiguredBlocks(@Nullable YamlConfiguration blocksConfig) {
        if (blocksConfig == null) return false;
        for (String key : BlocksConfigStorage.LOCKABLE_LIST_KEYS) {
            List<?> list = blocksConfig.getList(key);
            if (list == null) continue;
            for (Object o : list) {
                if (!BlocksConfigStorage.isPlaceholderEntry(o)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void checkAutoDisableModernFamilyBlocks(
            @Nullable YamlConfiguration blocksConfig,
            boolean isModernFamily,
            @Nullable File dataFolder) {
        if (!isModernFamily || blocksConfig == null || dataFolder == null) return;
        boolean allEmpty = true;
        outer:
        for (String key : BlocksConfigStorage.LOCKABLE_LIST_KEYS) {
            List<?> list = blocksConfig.getList(key);
            if (list == null) continue;
            for (Object o : list) {
                if (o instanceof String s && s.isBlank()) continue;
                if (o == null) continue;
                allEmpty = false;
                break outer;
            }
        }
        if (allEmpty) {
            File configFile = new File(dataFolder, "config.yml");
            if (configFile.exists()) {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
                cfg.set("modern_family_blocks", false);
                try {
                    cfg.save(configFile);
                } catch (java.io.IOException e) {
                    BlockProtLogger.warn("Failed to save config.yml: " + e.getMessage());
                }
                BlockProtLogger.log("blocks-auto", "All lockable lists empty, disabled modern_family_blocks.");
            }
        }
    }

    public static void writeFamilyKeyFormat(
            @NotNull YamlConfiguration blocksConfig,
            @NotNull String configKey,
            @NotNull BlockFamilyParser.Family family,
            @NotNull List<Material> active,
            boolean toModern) {
        if (!blocksConfig.contains(configKey) && active.isEmpty()) return;
        if (toModern) {
            String expr = BlockFamilyParser.toFamilyExpression(active, family);
            blocksConfig.set(configKey, expr != null
                ? List.of(expr)
                : active.stream().map(Material::name).sorted().toList());
        } else {
            blocksConfig.set(configKey, active.stream().map(Material::name).sorted().toList());
        }
    }
}
