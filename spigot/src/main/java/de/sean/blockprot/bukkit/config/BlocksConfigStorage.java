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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public final class BlocksConfigStorage {

    private BlocksConfigStorage() {}

    public static final List<String> BLOCKS_YML_KEY_ORDER = List.of(
        "lockable_tile_entities", "lockable_shulker_boxes",
        "lockable_blocks", "lockable_doors", "lockable_entities",
        "protectable_entities",
        "auto_drop_to_inventory"
    );

    public static final List<String> LOCKABLE_LIST_KEYS = List.of(
        "lockable_tile_entities", "lockable_shulker_boxes",
        "lockable_blocks", "lockable_doors", "lockable_entities"
    );

    public static final String AUTO_DROP_BLOCKS_KEY = "auto_drop_to_inventory.blocks";

    public static final List<String> BLOCKS_HEADER = List.of(
        "# BlockProt Reloaded - blocks.yml",
        "# Add block or material names below each list, one per line, replacing the blank lines.",
        "# Run /bp recommended blocks for a ready-made starting selection instead of editing by hand.",
        "# Format: flat names (CHEST) or family expressions ([*-CHEST]).",
        "# See docs/MODERN SYNTAX AND LEGACY/BLOCK_FAMILY_SYNTAX.md for full syntax.",
        "# Your lockable lists are never rewritten on startup/reload; structural merges, format conversion, and repair may rewrite this file."
    );

    public static boolean isPlaceholderEntry(@Nullable Object o) {
        if (o == null) return true;
        if (o instanceof String s) {
            String t = s.trim();
            return t.isEmpty() || t.equals("[]") || t.equals("[ ]");
        }
        return false;
    }

    public static void sanitizeBlocksListsForSave(@NotNull YamlConfiguration cfg, boolean modernFormat) {
        sanitizeListsForSave(cfg, "", modernFormat);
    }

    public static void sanitizeListsForSave(@NotNull YamlConfiguration cfg, @NotNull String prefix, boolean modernFormat) {
        List<String> keys = new ArrayList<>(LOCKABLE_LIST_KEYS);
        if (prefix.isEmpty()) {
            keys.add("protectable_entities");
            keys.add(AUTO_DROP_BLOCKS_KEY);
        }
        for (String k : keys) {
            String key = prefix + k;
            if (!cfg.contains(key)) continue;
            List<?> raw = cfg.getList(key);
            if (raw == null) continue;

            List<Object> real = new ArrayList<>();
            for (Object o : raw) {
                if (!isPlaceholderEntry(o)) real.add(o);
            }

            if (real.isEmpty()) {
                cfg.set(key, modernFormat ? List.of("[]") : Arrays.asList(null, null));
            } else if (real.size() != raw.size()) {
                cfg.set(key, real);
            }
        }
    }

    public static YamlConfiguration reorderBlocksKeys(@NotNull YamlConfiguration source) {
        YamlConfiguration result = new YamlConfiguration();
        for (String key : BLOCKS_YML_KEY_ORDER) {
            if (!source.contains(key)) continue;
            Object val = source.get(key);
            if (val instanceof ConfigurationSection section) {
                for (String sk : section.getKeys(false)) {
                    result.set(key + "." + sk, section.get(sk));
                }
            } else {
                result.set(key, val);
            }
        }
        for (String key : source.getKeys(false)) {
            if (BLOCKS_YML_KEY_ORDER.contains(key)) continue;
            Object val = source.get(key);
            if (val instanceof ConfigurationSection section) {
                for (String sk : section.getKeys(false)) {
                    result.set(key + "." + sk, section.get(sk));
                }
            } else {
                result.set(key, val);
            }
        }
        return result;
    }

    public static void prependBlocksHeader(@NotNull File file) throws IOException {
        List<String> existingLines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        if (!existingLines.isEmpty() && existingLines.get(0).equals(BLOCKS_HEADER.get(0))) {
            cleanNullPlaceholderLines(file);
            return;
        }
        List<String> newLines = new ArrayList<>(BLOCKS_HEADER);
        newLines.add("");
        int skipLimit = BLOCKS_HEADER.size() + 1;
        int skipCount = 0;
        for (String line : existingLines) {
            if (line.startsWith("# BlockProt") || (line.isEmpty() && skipCount < skipLimit)) {
                skipCount++;
                continue;
            }
            newLines.add(line);
        }
        Files.write(file.toPath(), newLines, StandardCharsets.UTF_8);
        cleanNullPlaceholderLines(file);
    }

    public static void cleanNullPlaceholderLines(@NotNull File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        List<String> newLines = new ArrayList<>();
        boolean modified = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.equals("- null") || trimmed.equals("- 'null'")) {
                int idx = line.indexOf('-');
                String newLine = line.substring(0, idx + 1);
                newLines.add(newLine);
                modified = true;
            } else {
                newLines.add(line);
            }
        }
        if (modified) {
            Files.write(file.toPath(), newLines, StandardCharsets.UTF_8);
        }
    }

    public static void patchBlocksFileIfNeeded(@NotNull File blocksFile, @NotNull YamlConfiguration bc) {
        boolean dirty = false;

        if (!bc.contains("lockable_entities")) {
            bc.set("lockable_entities", Collections.emptyList());
            BlockProtLogger.log("blocks-patch", "blocks.yml: added missing key 'lockable_entities' (empty list).");
            dirty = true;
        }

        if (!bc.contains("auto_drop_to_inventory")) {
            bc.set("auto_drop_to_inventory.enabled", true);
            bc.set("auto_drop_to_inventory.blocks", Arrays.asList(null, null));
            BlockProtLogger.log("blocks-patch", "blocks.yml: added missing key 'auto_drop_to_inventory' (enabled by default).");
            dirty = true;
        }

        if (!bc.contains("protectable_entities")) {
            bc.set("protectable_entities", Arrays.asList(null, null));
            BlockProtLogger.log("blocks-patch", "blocks.yml: added missing key 'protectable_entities' (empty list).");
            dirty = true;
        }

        if (bc.contains("recommended_blocks_applied")) {
            bc.set("recommended_blocks_applied", null);
            dirty = true;
        }

        if (dirty) {
            try {
                bc.save(blocksFile);
                prependBlocksHeader(blocksFile);
            } catch (IOException e) {
                BlockProtLogger.warn("blocks.yml patch save failed: " + e.getMessage());
            }
        }
    }

    public static void createBlocksFileWithHeader(@NotNull File blocksFile) throws IOException {
        List<String> lines = new ArrayList<>(BLOCKS_HEADER);
        lines.add("");
        for (String key : LOCKABLE_LIST_KEYS) {
            lines.add(key + ":");
            lines.add("-");
            lines.add("-");
        }
        lines.add("");
        lines.add("protectable_entities:");
        lines.add("- VILLAGER");
        lines.add("-");
        lines.add("");
        lines.add("auto_drop_to_inventory:");
        lines.add("  enabled: true");
        lines.add("  blocks:");
        lines.add("  -");
        lines.add("  -");
        lines.add("");
        Files.write(blocksFile.toPath(), lines, StandardCharsets.UTF_8);
    }
}
