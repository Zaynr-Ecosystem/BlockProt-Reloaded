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
import de.sean.blockprot.bukkit.util.ComponentMessages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Displays the live BlockProt command list using the active language file.
 */
public final class HelpCommand implements CommandExecutor {

    @Override
    public boolean canUseCommand(@NotNull CommandSender sender) {
        return !(sender instanceof Player) || sender.hasPermission(Permissions.USER.key());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player && !canUseCommand(sender)) {
            player.sendMessage(Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
            return true;
        }
        send(sender, Translator.get(TranslationKey.HELP__HEADER));

        final boolean useMenus = !BlockProt.getDefaultConfig().areExtraCommandsEnabled();
        final boolean isOp = sender.isOp();
        final boolean isAdmin = AdminTierManager.hasAnyAdminPermission(sender) || sender.hasPermission(Permissions.USER_ADMIN.key());
        final boolean hasDebug = sender.hasPermission(Permissions.DEBUG.key()) || AdminTierManager.hasPermission(sender, AdminAction.DEBUG);
        final boolean canUpdate = isOp || AdminTierManager.hasPermission(sender, AdminAction.UPDATE);
        final boolean canReload = isOp || AdminTierManager.hasPermission(sender, AdminAction.RELOAD);
        final boolean canLockables = isOp || AdminTierManager.hasPermission(sender, AdminAction.LOCKABLES);
        final boolean canRecommended = isOp || AdminTierManager.hasPermission(sender, AdminAction.RECOMMENDED);
        final boolean canProtDel = isOp || AdminTierManager.hasPermission(sender, AdminAction.PROTDEL);
        final boolean canIntegrations = isOp || AdminTierManager.hasPermission(sender, AdminAction.INTEGRATIONS);

        if (useMenus) {
            send(sender, Translator.get(TranslationKey.HELP__GUI_MODE_TITLE));
            send(sender, Translator.get(TranslationKey.HELP__GUI_USER));
            if (isAdmin) {
                send(sender, Translator.get(TranslationKey.HELP__GUI_ADMIN));
            }
            if (canUpdate) {
                send(sender, Translator.get(TranslationKey.HELP__UPDATE));
            }
        } else {
            send(sender, Translator.get(TranslationKey.HELP__BLOCK_PROTECTION_TITLE));
            send(sender, Translator.get(TranslationKey.HELP__BLOCK_PROTECTION_SETTINGS));

            if (!BlockProt.getDefaultConfig().isFriendFunctionalityDisabled()) {
                send(sender, Translator.get(TranslationKey.HELP__FRIENDS_TITLE));
                send(sender, Translator.get(TranslationKey.HELP__FRIENDS_GUI));
                send(sender, Translator.get(TranslationKey.HELP__FRIENDS_ADDALL));
                send(sender, Translator.get(TranslationKey.HELP__TRANSFER));
            }

            send(sender, Translator.get(TranslationKey.HELP__OTHER_TITLE));
            send(sender, Translator.get(TranslationKey.HELP__SETTINGS));
            send(sender, Translator.get(TranslationKey.HELP__STATS));
            send(sender, Translator.get(TranslationKey.HELP__ABOUT));
            send(sender, Translator.get(TranslationKey.HELP__DISABLE_HINTS));

            if (isAdmin) {
                send(sender, Translator.get(TranslationKey.HELP__ADMIN_INFO));
                send(sender, Translator.get(TranslationKey.HELP__UNLOCK));
                if (BlockProt.getDefaultConfig().isSessionLogEnabled()) {
                    send(sender, Translator.get(TranslationKey.HELP__LOGS));
                }
            }
            if (canIntegrations) {
                send(sender, Translator.get(TranslationKey.HELP__INTEGRATIONS));
            }
            if (canUpdate) {
                send(sender, Translator.get(TranslationKey.HELP__UPDATE));
            }
            if (canReload) {
                send(sender, Translator.get(TranslationKey.HELP__RELOAD));
            }
            if (canLockables) {
                send(sender, Translator.get(TranslationKey.HELP__LOCKABLES));
            }
            if (canRecommended) {
                send(sender, Translator.get(TranslationKey.HELP__RECOMMENDED));
            }
            if (canProtDel) {
                send(sender, Translator.get(TranslationKey.HELP__WORLD_PROT_DEL));
            }
            if (hasDebug) {
                send(sender, Translator.get(TranslationKey.HELP__DEBUG));
            }
        }

        send(sender, Translator.get(TranslationKey.HELP__FOOTER));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }

    private void send(@NotNull CommandSender sender, @NotNull String text) {
        if (text.isBlank()) return;
        ComponentMessages.sendLegacy(sender, text);
    }
}