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
import de.sean.blockprot.bukkit.dialogs.DialogOrigin;
import de.sean.blockprot.bukkit.dialogs.UpdateDialog;
import de.sean.blockprot.bukkit.tasks.UpdateChecker;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Checks for plugin updates and notifies the sender.
 */
public class UpdateCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!canUseCommand(sender)) return false;

        if (sender instanceof Player player && BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
            UpdateDialog.show(player, DialogOrigin.NONE);
            return true;
        }

        List<Player> recipients = (sender instanceof Player p)
            ? java.util.Collections.singletonList(p)
            : null;
        String pluginVersion = BlockProt.getPluginVersion();
        UpdateChecker checker = new UpdateChecker(pluginVersion, recipients, true);
        BlockProt.getFoliaLib().getScheduler().runAsync(asyncTask -> {
            checker.run();
            if (sender instanceof Player p) {
                BlockProt.getFoliaLib().getScheduler().runAtEntity(p, syncTask -> {
                    if (p.isOnline()) {
                        UpdateChecker.sendLatestOrInstalledNotesChat(p, pluginVersion, 5);
                    }
                });
            }
        });
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return null;
    }

    @Override
    public boolean canUseCommand(@NotNull CommandSender sender) {
        return de.sean.blockprot.bukkit.admin.AdminTierManager.hasPermission(sender, de.sean.blockprot.bukkit.admin.AdminAction.UPDATE);
    }
}