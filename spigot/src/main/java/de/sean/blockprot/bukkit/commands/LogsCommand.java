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
import de.sean.blockprot.bukkit.audit.AuditLogger;
import de.sean.blockprot.bukkit.dialogs.AuditDialog;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.FriendHandler;
import de.sean.blockprot.bukkit.storage.ProtectedBlockCache;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class LogsCommand implements CommandExecutor {

    private static final int PER_PAGE = 7;
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM HH:mm");

    @Override
    public boolean canUseCommand(@NotNull CommandSender sender) {
        return sender.hasPermission(Permissions.USER.key());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        AuditLogger audit = BlockProt.getAuditLogger();
        if (audit == null) {
            ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__LOGS_NO_ENTRIES));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("here")) {
            if (!(sender instanceof Player player)) {
                ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__ONLY_PLAYERS));
                return true;
            }
            return handleHere(player, args);
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("player")) {
            return handlePlayer(sender, args);
        }

        return handleGeneral(sender, args);
    }

    private boolean handleHere(@NotNull Player player, @NotNull String[] args) {
        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__LOGS_NO_TARGET));
            return true;
        }

        if (!ProtectedBlockCache.isProtected(target)
                || !BlockProt.getDefaultConfig().isLockable(target.getType(), target.getWorld())) {
            ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__LOGS_TARGET_NOT_PROTECTED));
            return true;
        }

        BlockNBTHandler handler = new BlockNBTHandler(target);
        String playerUuid = player.getUniqueId().toString();
        boolean isOwner = handler.isOwner(playerUuid);
        boolean isAdmin = AdminTierManager.hasPermission(player, AdminAction.LOGS)
            || player.hasPermission(Permissions.USER_ADMIN.key());
        Optional<FriendHandler> friend = handler.getFriend(playerUuid);
        boolean canAudit = friend.isPresent() && friend.get().canViewAudit();

        if (!isOwner && !isAdmin && !canAudit) {
            ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
            return true;
        }

        int page = 1;
        if (args.length > 1) {
            try {
                page = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException ignored) {}
        }

        if (BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
            AuditDialog.show(player, target);
            return true;
        }

        AuditLogger audit = BlockProt.getAuditLogger();
        if (audit == null) return true;

        String world = target.getWorld().getName();
        int x = target.getX(), y = target.getY(), z = target.getZ();
        int total = audit.countEntriesForBlock(world, x, y, z);
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) PER_PAGE));
        int safePage = Math.min(page, totalPages);
        int offset = (safePage - 1) * PER_PAGE;
        List<AuditLogger.AuditEntry> entries = audit.getEntriesForBlock(world, x, y, z, PER_PAGE, offset);

        renderEntries(player, entries, safePage, totalPages, total, "/bp logs here");
        return true;
    }

    private boolean handlePlayer(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!AdminTierManager.hasPermission(sender, AdminAction.LOGS)
                && !sender.hasPermission(Permissions.USER_ADMIN.key())) {
            ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
            return true;
        }

        if (args.length < 2) {
            ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__LOGS_USAGE));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        UUID targetUuid = target.getUniqueId();

        int page = 1;
        if (args.length > 2) {
            try {
                page = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException ignored) {}
        }

        if (sender instanceof Player player && BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
            AuditDialog.showPlayerLogs(player, targetUuid, page - 1);
            return true;
        }

        AuditLogger audit = BlockProt.getAuditLogger();
        if (audit == null) return true;

        int total = audit.countEntriesForPlayer(targetUuid);
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) PER_PAGE));
        int safePage = Math.min(page, totalPages);
        int offset = (safePage - 1) * PER_PAGE;
        List<AuditLogger.AuditEntry> entries = audit.getEntriesForPlayer(targetUuid, PER_PAGE, offset);

        renderEntries(sender, entries, safePage, totalPages, total, "/bp logs player " + args[1]);
        return true;
    }

    private boolean handleGeneral(@NotNull CommandSender sender, @NotNull String[] args) {
        boolean isAdmin = AdminTierManager.hasPermission(sender, AdminAction.LOGS)
            || sender.hasPermission(Permissions.USER_ADMIN.key());

        if (!isAdmin) {
            if (sender instanceof Player player) {
                return handleHere(player, args);
            }
            ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
            return true;
        }

        int page = 1;
        if (args.length > 0) {
            try {
                page = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException e) {
                ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__LOGS_USAGE));
                return true;
            }
        }

        if (sender instanceof Player player && BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
            AuditDialog.showAllLogs(player, page - 1);
            return true;
        }

        AuditLogger audit = BlockProt.getAuditLogger();
        if (audit == null) return true;

        int total = audit.countEntries();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) PER_PAGE));
        int safePage = Math.min(page, totalPages);
        int offset = (safePage - 1) * PER_PAGE;
        List<AuditLogger.AuditEntry> entries = audit.getRecentEntries(PER_PAGE, offset);

        renderEntries(sender, entries, safePage, totalPages, total, "/bp logs");
        return true;
    }

    private void renderEntries(@NotNull CommandSender sender,
                               @NotNull List<AuditLogger.AuditEntry> entries,
                               int page, int totalPages, int totalCount,
                               @NotNull String baseCommand) {
        String header = Translator.get(TranslationKey.MESSAGES__LOGS_HEADER)
            .replace("{page}", String.valueOf(page))
            .replace("{total}", String.valueOf(totalPages));
        ComponentMessages.sendLegacy(sender, header);

        if (entries.isEmpty()) {
            ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__LOGS_NO_ENTRIES));
            return;
        }

        for (AuditLogger.AuditEntry entry : entries) {
            String timeStr = DATE_FMT.format(new Date(entry.timestamp()));
            TextColor actionColor = switch (entry.action()) {
                case ACCESS_DENIED, RAID_EXPLOSION -> TextColor.color(0xFF6B6B);
                case OPENED -> TextColor.color(0x74B9FF);
                case ITEM_TAKEN -> TextColor.color(0xFDCB6E);
                case ITEM_PLACED -> TextColor.color(0x00B894);
                default -> NamedTextColor.WHITE;
            };

            Component line = Component.text()
                .append(Component.text("[" + timeStr + "] ", TextColor.color(0x888888)))
                .append(Component.text(entry.playerName() != null ? entry.playerName() : "Unknown", NamedTextColor.WHITE))
                .append(Component.text(" " + entry.action().name(), actionColor))
                .append(Component.text(" @ " + entry.world() + " (" + entry.x() + ", " + entry.y() + ", " + entry.z() + ")", TextColor.color(0xAAAAAA)))
                .build();
            ComponentMessages.send(sender, line);
        }

        String pageInfo = Translator.get(TranslationKey.MESSAGES__LOGS_PAGE_INFO)
            .replace("{page}", String.valueOf(page))
            .replace("{total}", String.valueOf(totalPages))
            .replace("{count}", String.valueOf(totalCount));
        ComponentMessages.sendLegacy(sender, pageInfo);

        Component nav = Component.empty();
        if (page > 1) {
            Component prev = Component.text(Translator.get(TranslationKey.MESSAGES__LOGS_PREV))
                .clickEvent(ClickEvent.runCommand(baseCommand + " " + (page - 1)))
                .hoverEvent(HoverEvent.showText(Component.text("Page " + (page - 1))));
            nav = nav.append(prev);
        }
        if (page < totalPages) {
            if (page > 1) nav = nav.append(Component.text("  ", NamedTextColor.GRAY));
            Component next = Component.text(Translator.get(TranslationKey.MESSAGES__LOGS_NEXT))
                .clickEvent(ClickEvent.runCommand(baseCommand + " " + (page + 1)))
                .hoverEvent(HoverEvent.showText(Component.text("Page " + (page + 1))));
            nav = nav.append(next);
        }

        if (page > 1 || page < totalPages) {
            ComponentMessages.send(sender, nav);
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if ("here".startsWith(args[0].toLowerCase())) completions.add("here");
            if (AdminTierManager.hasPermission(sender, AdminAction.LOGS)
                    || sender.hasPermission(Permissions.USER_ADMIN.key())) {
                if ("player".startsWith(args[0].toLowerCase())) completions.add("player");
            }
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("player")) {
            List<String> players = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    players.add(p.getName());
                }
            }
            return players;
        }
        return Collections.emptyList();
    }
}
