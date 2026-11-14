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

package de.sean.blockprot.bukkit.commands;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtConsole;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.storage.PluginDataLayout;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Applies the recommended starting configuration in two independent halves.
 *
 * <p>{@code /bp recommended blocks} only writes {@code blocks.yml} (lockable
 * lists and auto-drop) and remembers that with the {@code recommended_blocks_applied}
 * key stored inside {@code blocks.yml} itself. {@code /bp recommended config} only
 * writes {@code config.yml} (the recommended settings that are not enabled by
 * default: {@code modern_family_blocks}, {@code use_menus}, {@code use_dialogs})
 * and remembers that with {@code recommended_config_applied} in {@code config.yml}.
 * {@code /bp recommended all} applies both halves in sequence.
 *
 * <p>Each half can only be applied once; passing {@code force} as the third argument
 * bypasses that guard so the half can be re-applied.
 */
public class RecommendedCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!canUseCommand(sender)) {
            sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_CONSOLE_ONLY));
            return true;
        }

        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String first = args[1].toLowerCase(Locale.ROOT);

        if (first.equals("undo")) {
            if (args.length != 3) {
                sendUsage(sender);
                return true;
            }
            String undoTarget = args[2].toLowerCase(Locale.ROOT);
            if (!undoTarget.equals("blocks") && !undoTarget.equals("config") && !undoTarget.equals("all")) {
                sendUsage(sender);
                return true;
            }
            if (undoTarget.equals("blocks")) return undoBlocks(sender, true);
            if (undoTarget.equals("config")) return undoConfig(sender, true);
            return undoAll(sender);
        }

        String target = first;
        if (!target.equals("blocks") && !target.equals("config") && !target.equals("all")) {
            sendUsage(sender);
            return true;
        }

        boolean force = args.length >= 3 && args[2].equalsIgnoreCase("force");
        if (args.length > 3 || (args.length == 3 && !force)) {
            sendUsage(sender);
            return true;
        }

        if (target.equals("blocks")) {
            return applyBlocks(sender, force, true);
        }
        if (target.equals("config")) {
            return applyConfig(sender, force, true);
        }
        return applyAll(sender, force);
    }

    private void sendOutput(@NotNull CommandSender sender, @NotNull String message) {
        ComponentMessages.sendLegacy(sender, message);
        if (!(sender instanceof ConsoleCommandSender)) {
            BlockProtConsole.info(message);
        }
        BlockProtLogger.log("recommended", message);
    }

    private void sendUsage(@NotNull CommandSender sender) {
        sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_USAGE_HEADER));
        sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_USAGE_APPLY));
        sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_USAGE_UNDO));
    }

    private boolean applyAll(@NotNull CommandSender sender, boolean force) {
        sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_HEADER));
        boolean b = applyBlocks(sender, force, false);
        boolean c = applyConfig(sender, force, false);
        if (b || c) {
            sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_RELOAD));
        }
        return true;
    }

    private boolean applyBlocks(@NotNull CommandSender sender, boolean force, boolean standalone) {
        BlockProt plugin = BlockProt.getInstance();
        File blocksFile = new File(plugin.getDataFolder(), "blocks.yml");

        if (!blocksFile.exists()) {
            if (standalone) {
                sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_HEADER));
            }
            sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_BLOCKS_MISSING));
            return false;
        }

        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(blocksFile);
            boolean alreadyApplied = isStateApplied("blocks_applied") || cfg.getBoolean("recommended_blocks_applied", false);

            if (alreadyApplied && !force) {
                if (standalone) {
                    sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_HEADER));
                }
                String message = Translator.get(TranslationKey.CONSOLE__RECOMMENDED_ALREADY_APPLIED)
                    .replace("{target}", "blocks");
                sendOutput(sender, message);
                return false;
            }

            if (standalone) {
                sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_HEADER));
            }

            cfg.set("lockable_tile_entities", List.of("[*-CHEST *-FURNACE *-TRANSPORT *-MISC *-SHELF *-SIGN]"));
            cfg.set("lockable_shulker_boxes", List.of("[*]"));
            cfg.set("lockable_blocks", List.of("[*-ANVIL *-CAULDRON *-WORKSTATION *-TRAPDOOR *-FENCE_GATE *-BED]"));
            cfg.set("lockable_doors", List.of("[*]"));
            cfg.set("lockable_entities", List.of("[*-ITEM_FRAMES]"));
            cfg.set("auto_drop_to_inventory.enabled", true);
            Set<org.bukkit.Material> allShulkers = BlockFamilyParser.getFamilyMembers(BlockFamilyParser.Family.SHULKER_BOXES);
            if (!allShulkers.isEmpty()) {
                cfg.set("auto_drop_to_inventory.blocks", List.of("[*-SHULKERS]"));
            } else {
                cfg.set("auto_drop_to_inventory.blocks", List.of());
            }
            cfg.set("recommended_blocks_applied", null);
            setInternalState("blocks_applied", true);

            if (plugin.getFileWatcher() != null) {
                plugin.getFileWatcher().suppressPath("blocks.yml");
            }
            DefaultConfig.sanitizeBlocksListsForSave(cfg, false);
            cfg = DefaultConfig.reorderBlocksKeys(cfg);
            cfg.save(blocksFile);
            DefaultConfig.prependBlocksHeader(blocksFile);

            sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_BLOCKS_DONE));
            if (standalone) {
                sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_RELOAD));
            }

            if (plugin.getFileWatcher() != null) {
                plugin.getFileWatcher().requestProgrammaticReload();
            }
            return true;
        } catch (IOException e) {
            if (standalone) {
                sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_HEADER));
            }
            sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_FAILED)
                .replace("{file}", "blocks.yml")
                .replace("{error}", e.getMessage()));
            return false;
        }
    }

    private boolean applyConfig(@NotNull CommandSender sender, boolean force, boolean standalone) {
        DefaultConfig defaultConfig = BlockProt.getDefaultConfig();
        boolean alreadyApplied = isStateApplied("config_applied") || defaultConfig.getBukkitConfig().getBoolean("recommended_config_applied", false);

        if (alreadyApplied && !force) {
            if (standalone) {
                sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_HEADER));
            }
            String message = Translator.get(TranslationKey.CONSOLE__RECOMMENDED_ALREADY_APPLIED)
                .replace("{target}", "config");
            sendOutput(sender, message);
            return false;
        }

        if (standalone) {
            sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_HEADER));
        }

        defaultConfig.setAndSave("modern_family_blocks", true);
        defaultConfig.setAndSave("use_menus", true);
        defaultConfig.setAndSave("use_dialogs", true);
        defaultConfig.getBukkitConfig().set("recommended_config_applied", null);
        BlockProt.getInstance().saveConfig();
        setInternalState("config_applied", true);

        sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_CONFIG_DONE));
        if (standalone) {
            sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_RELOAD));
        }

        return true;
    }

    private boolean undoAll(@NotNull CommandSender sender) {
        sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_UNDO_HEADER));
        boolean b = undoBlocks(sender, false);
        boolean c = undoConfig(sender, false);
        if (b || c) {
            sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_RELOAD));
        }
        return true;
    }

    private boolean undoBlocks(@NotNull CommandSender sender, boolean standalone) {
        BlockProt plugin = BlockProt.getInstance();
        File blocksFile = new File(plugin.getDataFolder(), "blocks.yml");

        if (!blocksFile.exists()) {
            if (standalone) {
                sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_UNDO_HEADER));
            }
            sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_BLOCKS_MISSING));
            return false;
        }

        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(blocksFile);
            boolean applied = isStateApplied("blocks_applied") || cfg.getBoolean("recommended_blocks_applied", false);

            if (!applied) {
                if (standalone) {
                    sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_UNDO_HEADER));
                }
                String message = Translator.get(TranslationKey.CONSOLE__RECOMMENDED_UNDO_NOTHING)
                    .replace("{target}", "blocks");
                sendOutput(sender, message);
                return false;
            }

            if (standalone) {
                sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_UNDO_HEADER));
            }

            cfg.set("lockable_tile_entities", List.of());
            cfg.set("lockable_shulker_boxes", List.of());
            cfg.set("lockable_blocks", List.of());
            cfg.set("lockable_doors", List.of());
            cfg.set("lockable_entities", List.of());
            cfg.set("auto_drop_to_inventory.enabled", true);
            cfg.set("auto_drop_to_inventory.blocks", List.of());
            cfg.set("recommended_blocks_applied", null);
            setInternalState("blocks_applied", false);

            if (plugin.getFileWatcher() != null) {
                plugin.getFileWatcher().suppressPath("blocks.yml");
            }
            DefaultConfig.sanitizeBlocksListsForSave(cfg, false);
            cfg = DefaultConfig.reorderBlocksKeys(cfg);
            cfg.save(blocksFile);
            DefaultConfig.prependBlocksHeader(blocksFile);

            String message = Translator.get(TranslationKey.CONSOLE__RECOMMENDED_UNDO_DONE)
                .replace("{target}", "blocks");
            sendOutput(sender, message);
            if (standalone) {
                sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_RELOAD));
            }

            if (plugin.getFileWatcher() != null) {
                plugin.getFileWatcher().requestProgrammaticReload();
            }
            return true;
        } catch (IOException e) {
            if (standalone) {
                sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_UNDO_HEADER));
            }
            sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_FAILED)
                .replace("{file}", "blocks.yml")
                .replace("{error}", e.getMessage()));
            return false;
        }
    }

    private boolean undoConfig(@NotNull CommandSender sender, boolean standalone) {
        DefaultConfig defaultConfig = BlockProt.getDefaultConfig();
        boolean applied = isStateApplied("config_applied") || defaultConfig.getBukkitConfig().getBoolean("recommended_config_applied", false);

        if (!applied) {
            if (standalone) {
                sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_UNDO_HEADER));
            }
            String message = Translator.get(TranslationKey.CONSOLE__RECOMMENDED_UNDO_NOTHING)
                .replace("{target}", "config");
            sendOutput(sender, message);
            return false;
        }

        if (standalone) {
            sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_UNDO_HEADER));
        }

        defaultConfig.setAndSave("modern_family_blocks", false);
        defaultConfig.setAndSave("use_menus", false);
        defaultConfig.setAndSave("use_dialogs", false);
        defaultConfig.getBukkitConfig().set("recommended_config_applied", null);
        BlockProt.getInstance().saveConfig();
        setInternalState("config_applied", false);

        String message = Translator.get(TranslationKey.CONSOLE__RECOMMENDED_UNDO_DONE)
            .replace("{target}", "config");
        sendOutput(sender, message);
        if (standalone) {
            sendOutput(sender, Translator.get(TranslationKey.CONSOLE__RECOMMENDED_RELOAD));
        }

        return true;
    }

    private static boolean isStateApplied(String key) {
        return PluginDataLayout.isRecommendedApplied(BlockProt.getInstance().getDataFolder(), key);
    }

    private static void setInternalState(String key, boolean value) {
        PluginDataLayout.setRecommendedApplied(BlockProt.getInstance().getDataFolder(), key, value);
    }

    @Override
    public boolean canUseCommand(@NotNull CommandSender sender) {
        return de.sean.blockprot.bukkit.admin.AdminTierManager.hasPermission(sender, de.sean.blockprot.bukkit.admin.AdminAction.RECOMMENDED);
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 2) {
            return List.of("blocks", "config", "all", "undo");
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("undo")) {
            return List.of("blocks", "config", "all");
        }
        if (args.length == 3 && (args[1].equalsIgnoreCase("blocks") || args[1].equalsIgnoreCase("config") || args[1].equalsIgnoreCase("all"))) {
            return List.of("force");
        }
        return Collections.emptyList();
    }
}
