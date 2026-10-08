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
import de.sean.blockprot.bukkit.tasks.ConfigFileWatcher;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BlocksYamlRepair {

    private BlocksYamlRepair() {
    }

    public static void handleCorruptBlocksYaml(
        @NotNull Plugin plugin,
        @NotNull File diskFile,
        @NotNull YamlConfiguration jarConfig,
        @NotNull Exception ex,
        @Nullable ConfigFileWatcher fileWatcher,
        boolean modern
    ) {
        int errorLine = getErrorLine(ex.getMessage());
        String lineInfo = errorLine > 0 ? " at line " + errorLine : "";
        plugin.getLogger().severe("CRITICAL: blocks.yml is corrupted" + lineInfo + ".");
        plugin.getLogger().severe("Cause: " + ex.getMessage());
        plugin.getLogger().severe("Attempting intelligent repair...");
        BlockProtLogger.log("yaml-repair", "CRITICAL: blocks.yml corrupted" + lineInfo + ". Cause: " + ex.getMessage());

        YamlConfiguration recovered = attemptYamlRecovery(diskFile, jarConfig, plugin);
        if (recovered == null) {
            plugin.getLogger().severe("Repair FAILED: unable to recover any data from blocks.yml.");
            plugin.getLogger().severe("Manually restore blocks.yml from backup. Report this bug at: https://github.com/VictorGugug/BlockProt-Reloaded/issues");
            BlockProtLogger.log("yaml-repair", "Repair FAILED. No data could be recovered. Report as bug: https://github.com/VictorGugug/BlockProt-Reloaded/issues");
            return;
        }

        for (String key : jarConfig.getKeys(false)) {
            if (!recovered.contains(key)) recovered.set(key, jarConfig.get(key));
        }

        try {
            if (fileWatcher != null) fileWatcher.suppressNext();
            DefaultConfig.sanitizeBlocksListsForSave(recovered, modern);
            recovered = DefaultConfig.reorderBlocksKeys(recovered);
            recovered.save(diskFile);
            DefaultConfig.prependBlocksHeader(diskFile);
            plugin.getLogger().warning("blocks.yml repaired" + lineInfo + ". Corrupted file saved as blocks.yml.corrupted.*. Verify blocks.yml content.");
            BlockProtLogger.log("yaml-repair", "Repair SUCCESS" + lineInfo + ". Corrupted file backed up as blocks.yml.corrupted.*");
        } catch (IOException e) {
            plugin.getLogger().severe("Repair FAILED: could not write repaired blocks.yml: " + e.getMessage());
            plugin.getLogger().severe("Manually fix blocks.yml. Report this bug at: https://github.com/VictorGugug/BlockProt-Reloaded/issues");
            BlockProtLogger.log("yaml-repair", "Repair FAILED: write error: " + e.getMessage() + ". Report as bug.");
        }
    }

    public static int getErrorLine(@Nullable String errorMsg) {
        if (errorMsg == null) return -1;
        Pattern p = Pattern.compile("line (\\d+)");
        Matcher m = p.matcher(errorMsg);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    @Nullable
    public static YamlConfiguration attemptYamlRecovery(
        @NotNull File corruptedFile,
        @NotNull YamlConfiguration jarTemplate,
        @NotNull Plugin plugin
    ) {
        backupCorruptedFile(corruptedFile, plugin);

        YamlConfiguration recovered = new YamlConfiguration();
        recovered.set("lockable_tile_entities", List.of());
        recovered.set("lockable_shulker_boxes", List.of());
        recovered.set("lockable_blocks", List.of());
        recovered.set("lockable_doors", List.of());
        recovered.set("lockable_entities", List.of());
        recovered.set("auto_drop_to_inventory.enabled", true);
        recovered.set("auto_drop_to_inventory.blocks", List.of());

        String[] listKeys = {
            "lockable_tile_entities", "lockable_shulker_boxes",
            "lockable_blocks", "lockable_doors", "lockable_entities",
            "auto_drop_to_inventory.blocks"
        };

        try {
            List<String> lines = Files.readAllLines(corruptedFile.toPath(), StandardCharsets.UTF_8);
            String currentKey = null;
            List<String> recoveredValues = new ArrayList<>();

            for (String line : lines) {
                String trimmed = line.trim();

                for (String key : listKeys) {
                    if (trimmed.equals(key + ":") || trimmed.startsWith(key + ":")) {
                        currentKey = key;
                        if (!trimmed.endsWith(":") || trimmed.length() > key.length() + 1) {
                            String inlineValue = trimmed.substring(trimmed.indexOf(':') + 1).trim();
                            if (!inlineValue.isEmpty() && !inlineValue.equals("[]")) {
                                recoveredValues.add(inlineValue);
                            }
                        }
                        break;
                    }
                }

                if (currentKey != null && (trimmed.startsWith("- ") || (!trimmed.isEmpty() && !trimmed.contains(":")))) {
                    String value = trimmed.startsWith("- ") ? trimmed.substring(2).trim() : trimmed;
                    value = value.replaceAll("^\"|\"$", "");
                    value = value.replaceAll("^'|'$", "");
                    if (!value.isEmpty() && !value.equals("[]") && !value.equals("-")) {
                        recoveredValues.add(value);
                    }
                }

                boolean isNewKey = false;
                for (String key : jarTemplate.getKeys(false)) {
                    if (trimmed.equals(key + ":")) {
                        isNewKey = true;
                        break;
                    }
                }
                if (isNewKey || (trimmed.contains(":") && !trimmed.startsWith("-") && !trimmed.startsWith("#"))) {
                    if (!recoveredValues.isEmpty() && currentKey != null) {
                        recovered.set(currentKey, new ArrayList<>(recoveredValues));
                        BlockProtLogger.log("blocks-recover", "Recovered " + recoveredValues.size() + " entries for: " + currentKey);
                        recoveredValues.clear();
                    }
                    currentKey = null;
                }
            }

            if (!recoveredValues.isEmpty() && currentKey != null) {
                recovered.set(currentKey, new ArrayList<>(recoveredValues));
                BlockProtLogger.log("blocks-recover", "Recovered " + recoveredValues.size() + " entries for: " + currentKey);
            }

            return recovered;
        } catch (IOException e) {
            BlockProtLogger.warn("Failed to read corrupted blocks.yml for recovery: " + e.getMessage());
            return null;
        }
    }

    public static void backupCorruptedFile(@NotNull File originalFile, @NotNull Plugin plugin) {
        try {
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date());
            File backup = new File(originalFile.getParentFile(), originalFile.getName() + ".corrupted." + timestamp);
            Files.copy(originalFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().warning("Corrupted blocks.yml backed up as: " + backup.getName());
        } catch (IOException e) {
            BlockProtLogger.warn("Failed to backup corrupted blocks.yml: " + e.getMessage());
        }
    }
}
