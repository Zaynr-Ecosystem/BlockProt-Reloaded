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
import de.sean.blockprot.bukkit.admin.AdminTier;
import de.sean.blockprot.bukkit.admin.AdminTierManager;
import de.sean.blockprot.bukkit.dialogs.AdminTiersDialog;
import de.sean.blockprot.bukkit.dialogs.DialogOrigin;
import de.sean.blockprot.bukkit.inventories.AdminTiersInventory;
import de.sean.blockprot.bukkit.inventories.InventoryState;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Manages administrative tiers and roles via /bp tiers.
 */
public final class TiersCommand implements CommandExecutor {

    @Override
    public boolean canUseCommand(@NotNull CommandSender sender) {
        return BlockProt.getDefaultConfig().isAdminTiersEnabled()
            && (sender.isOp() || AdminTierManager.hasPermission(sender, AdminAction.SETROLE)
                || sender.hasPermission(Permissions.ADMIN_OWNER.key()));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!canUseCommand(sender)) {
            ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
            return true;
        }

        if (!BlockProt.getDefaultConfig().isAdminTiersEnabled()) {
            ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__ADMIN_TIERS_DISABLED));
            return true;
        }

        if (args.length <= 1 || (args.length == 2 && (args[1].equalsIgnoreCase("gui") || args[1].equalsIgnoreCase("menu")))) {
            if (sender instanceof Player player) {
                if (BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
                    AdminTiersDialog.show(player, DialogOrigin.NONE);
                } else {
                    InventoryState state = InventoryState.builder()
                        .origin(InventoryState.MenuOrigin.NONE)
                        .build();
                    InventoryState.set(player.getUniqueId(), state);
                    player.openInventory(new AdminTiersInventory().fill(player, 0));
                }
                return true;
            }
            ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__ADMIN_SETROLE_USAGE));
            return true;
        }

        String targetName = null;
        String roleName = null;

        if (args.length >= 4 && args[1].equalsIgnoreCase("setrole")) {
            targetName = args[2];
            roleName = args[3];
        } else if (args.length == 3 && !args[1].equalsIgnoreCase("setrole")) {
            targetName = args[1];
            roleName = args[2];
        } else if (args.length == 2 && !args[1].equalsIgnoreCase("setrole")) {
            targetName = args[1];
        } else {
            ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__ADMIN_SETROLE_USAGE));
            return true;
        }

        Player target = Bukkit.getPlayer(targetName);
        UUID targetUuid = target != null ? target.getUniqueId() : null;
        if (targetUuid == null) {
            try {
                targetUuid = UUID.fromString(targetName);
            } catch (IllegalArgumentException ignored) {}
        }
        boolean isOp = false;
        if (targetUuid == null) {
            @SuppressWarnings("deprecation")
            OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
            if (op.hasPlayedBefore() || op.isOnline()) {
                targetUuid = op.getUniqueId();
                isOp = op.isOp();
            }
        } else if (target != null) {
            isOp = target.isOp();
        } else {
            isOp = Bukkit.getOfflinePlayer(targetUuid).isOp();
        }

        if (targetUuid == null) {
            ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__ADMIN_INFO_PLAYER_NOT_FOUND)
                .replace("{player}", targetName));
            return true;
        }

        if (roleName == null) {
            AdminTier configuredTier = AdminTierManager.getRole(targetUuid);
            ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__ADMIN_TIERS_INFO)
                .replace("{player}", targetName)
                .replace("{role}", Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__CURRENT_ROLE)
                    .replace("{role}", configuredTier.getIdentifier()))
                .replace("{op}", Translator.get(isOp ? TranslationKey.MESSAGES__ADMIN_TIERS_OP_YES
                    : TranslationKey.MESSAGES__ADMIN_TIERS_OP_NO)));
            return true;
        }

        AdminTier tier = AdminTier.fromString(roleName);
        AdminTierManager.setPlayerRole(targetUuid, tier, null);
        ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__ADMIN_SETROLE_SUCCESS)
            .replace("{player}", targetName)
            .replace("{role}", tier.getIdentifier()));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!canUseCommand(sender)) return Collections.emptyList();

        if (args.length == 2) {
            return List.of("setrole", "gui");
        }
        if (args.length == 3) {
            if (args[1].equalsIgnoreCase("setrole")) return null;
            return List.of("owner", "t3", "t2", "t1", "custom", "user", "none");
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("setrole")) {
            return List.of("owner", "t3", "t2", "t1", "custom", "user", "none");
        }
        return Collections.emptyList();
    }
}
