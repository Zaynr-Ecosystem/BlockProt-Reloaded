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
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.admin.AdminAction;
import de.sean.blockprot.bukkit.admin.AdminTierManager;
import de.sean.blockprot.bukkit.commands.debug.DiagnosticsRunner;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * /blockprot debug: diagnostics and manual test-bench.
 */
public class DebugCommand implements CommandExecutor {

    private static final String NOTCH_UUID = "069a79f4-44e9-4726-a5be-fca90e38aaf5";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!canUseCommand(sender)) return false;
        if (!(sender instanceof Player player)) {
            if (args.length < 2 || args[1].equalsIgnoreCase("run")) {
                Player target = null;
                if (args.length >= 3) {
                    target = Bukkit.getPlayer(args[2]);
                }
                if (target == null && !Bukkit.getOnlinePlayers().isEmpty()) {
                    target = Bukkit.getOnlinePlayers().iterator().next();
                }
                if (target != null) {
                    ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__DEBUG__HEADLESS_PLAYER_CONTEXT).replace("{player}", target.getName()));
                    run(target);
                    return true;
                } else {
                    runHeadless(sender);
                    return true;
                }
            }
            sender.sendMessage(Translator.get(TranslationKey.MESSAGES__ONLY_PLAYERS));
            return false;
        }

        if (args.length < 2) {
            player.sendMessage(Translator.get(TranslationKey.MESSAGES__DEBUG_USAGE));
            return false;
        }

        switch (args[1]) {
            case "placeDebugChest" -> {
                player.getWorld().setType(player.getLocation(), Material.CHEST);
                new BlockNBTHandler(player.getWorld().getBlockAt(player.getLocation())).setOwner(NOTCH_UUID);
                ab(player, Translator.get(TranslationKey.MESSAGES__DEBUG__CHEST_PLACED));
                return true;
            }
            case "placeDebugShulker" -> {
                player.getWorld().setType(player.getLocation(), Material.SHULKER_BOX);
                new BlockNBTHandler(player.getWorld().getBlockAt(player.getLocation())).setOwner(NOTCH_UUID);
                ab(player, Translator.get(TranslationKey.MESSAGES__DEBUG__SHULKER_PLACED));
                return true;
            }
            case "clearSearchHistory" -> {
                new PlayerSettingsHandler(player).clearSearchHistory();
                ab(player, Translator.get(TranslationKey.MESSAGES__DEBUG__HISTORY_CLEARED));
                return true;
            }
            case "run" -> {
                run(player);
                return true;
            }
        }
        return false;
    }

    /**
     * Shows the diagnostics hint and runs the diagnostic suite asynchronously.
     * Used by both the CLI and the admin menu.
     */
    public static void run(@NotNull Player player) {
        ab(player, Translator.get(TranslationKey.MESSAGES__DEBUG__RUNNING_DIAGNOSTICS));
        BlockProt.getFoliaLib().getScheduler().runAsync(task -> DiagnosticsRunner.runInteractive(player));
    }

    public static void runHeadless(@NotNull CommandSender sender) {
        BlockProt.getFoliaLib().getScheduler().runAsync(task -> DiagnosticsRunner.runHeadless(sender));
    }

    private static void ab(@NotNull Player p, @NotNull String msg) {
        ComponentMessages.sendLegacyActionBar(p, msg);
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (!canUseCommand(sender)) return Collections.emptyList();
        return List.of("run", "placeDebugChest", "placeDebugShulker", "clearSearchHistory");
    }

    @Override
    public boolean canUseCommand(@NotNull CommandSender sender) {
        return sender.isOp() || AdminTierManager.hasPermission(sender, AdminAction.DEBUG)
            || sender.hasPermission(Permissions.DEBUG.key());
    }
}