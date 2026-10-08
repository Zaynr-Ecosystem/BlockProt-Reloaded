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

package de.sean.blockprot.bukkit.dialogs;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.audit.AuditLogger;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_CORAL;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_GOLD;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_MINT;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_PURPLE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_BLUE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_GRAY;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AuditDialog {

    private static final int PER_PAGE = 8;
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM HH:mm");

    private AuditDialog() {}

    public static void show(@NotNull Player player, @NotNull Location location) {
        // No parent menu to return to: exit button closes the dialog.
        show(player, location, null, 0, null);
    }

    public static void show(@NotNull Player player, @NotNull Block block) {
        show(player, block.getLocation(), null, 0, () -> BlockLockDialog.showBlock(player, block));
    }

    public static void show(@NotNull Player player, @NotNull Location location,
                             @Nullable String filterPlayerUuid, int page,
                             @Nullable Runnable backAction) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        AuditLogger audit = BlockProt.getAuditLogger();
        if (audit == null) {
            bridge.showNotice(player,
                Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__AUDIT__TITLE)),
                    SOFT_BLUE, TextDecoration.BOLD),
                List.of(Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__AUDIT__NO_ENTRIES)),
                    SOFT_GRAY)),
                new DialogButton("exit",
                    Component.text(stripColor(Translator.get(backAction != null ? TranslationKey.DIALOGS__BACK : TranslationKey.DIALOGS__CLOSE)), SOFT_GRAY),
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
                    backAction != null ? p -> backAction.run() : null));
            return;
        }

        List<AuditLogger.AuditEntry> allEntries = audit.getEntriesForBlock(
            location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), 500);

        try {
            BlockNBTHandler ownerHandler = new BlockNBTHandler(location.getBlock());
            if (ownerHandler.isProtected() && ownerHandler.getOwner() != null) {
                String ownerUuid = ownerHandler.getOwner();
                allEntries.removeIf(e -> ownerUuid.equals(e.playerUuid()));
            }
        } catch (RuntimeException ignored) {
            // Location might be an entity (like a boat) standing on a non-lockable block like snow.
        }

        allEntries.removeIf(e -> filterPlayerUuid != null && !e.playerUuid().equals(filterPlayerUuid));

        if (allEntries.isEmpty()) {
            bridge.showNotice(player,
                Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__AUDIT__TITLE)),
                    SOFT_BLUE, TextDecoration.BOLD),
                List.of(Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__AUDIT__NO_ENTRIES)),
                    SOFT_GRAY)),
                new DialogButton("exit",
                    Component.text(stripColor(Translator.get(backAction != null ? TranslationKey.DIALOGS__BACK : TranslationKey.DIALOGS__CLOSE)), SOFT_GRAY),
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
                    backAction != null ? p -> backAction.run() : null));
            return;
        }

        int totalPages = Math.max(1, (int) Math.ceil(allEntries.size() / (double) PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * PER_PAGE;
        int to = Math.min(from + PER_PAGE, allEntries.size());
        List<AuditLogger.AuditEntry> pageEntries = allEntries.subList(from, to);

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__AUDIT__TITLE)),
            SOFT_BLUE, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__PAGE))
                .replace("{current}", String.valueOf(safePage + 1))
                .replace("{total}", String.valueOf(totalPages)),
            TextColor.color(0x888888))));

        for (AuditLogger.AuditEntry entry : pageEntries) {
            String actionStr = actionLabel(entry.action());
            String timeStr = DATE_FMT.format(new Date(entry.timestamp()));
            TextColor color = actionColor(entry.action());
            body.add(DialogBodyEntry.text(Component.text()
                .append(Component.text(timeStr, SOFT_GRAY))
                .append(Component.text(" ", SOFT_GRAY))
                .append(Component.text(actionStr, color))
                .append(Component.text(" - " + entry.playerName(), NamedTextColor.WHITE))
                .build()));
        }

        List<DialogButton> buttons = new ArrayList<>();

        if (safePage > 0) {
            int prev = safePage - 1;
            buttons.add(new DialogButton("prev",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV_HINT)), TextColor.color(0x888888)),
                p -> show(player, location, filterPlayerUuid, prev, backAction)));
        }
        if (safePage + 1 < totalPages) {
            int next = safePage + 1;
            buttons.add(new DialogButton("next",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT_HINT)), TextColor.color(0x888888)),
                p -> show(player, location, filterPlayerUuid, next, backAction)));
        }

        // backAction is only null for the standalone entry point (no parent menu to return
        // to), in which case the button just closes the dialog. Otherwise it always runs
        // backAction: this is one level of internal navigation back to whichever menu
        // opened the audit log, not an external-origin exit gated by config.
        DialogButton exitBtn = new DialogButton("exit",
            Component.text(stripColor(Translator.get(backAction != null ? TranslationKey.DIALOGS__BACK : TranslationKey.DIALOGS__CLOSE)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            backAction != null ? p -> backAction.run() : null);

        bridge.showMultiAction(player, title, body, buttons, exitBtn, 1);
    }

    public static void showAllLogs(@NotNull Player player, int page) {
        showLogs(player, null, null, null, null, null, page, null);
    }

    public static void showPlayerLogs(@NotNull Player player, @NotNull UUID targetPlayer, int page) {
        showLogs(player, null, null, null, null, targetPlayer.toString(), page, null);
    }

    public static void showLogs(@NotNull Player player,
                                @Nullable String world, @Nullable Integer x, @Nullable Integer y, @Nullable Integer z,
                                @Nullable String filterPlayerUuid, int page,
                                @Nullable Runnable backAction) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        AuditLogger audit = BlockProt.getAuditLogger();
        if (audit == null) {
            bridge.showNotice(player,
                Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__AUDIT__TITLE)),
                    SOFT_BLUE, TextDecoration.BOLD),
                List.of(Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__AUDIT__NO_ENTRIES)),
                    SOFT_GRAY)),
                new DialogButton("exit",
                    Component.text(stripColor(Translator.get(backAction != null ? TranslationKey.DIALOGS__BACK : TranslationKey.DIALOGS__CLOSE)), SOFT_GRAY),
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
                    backAction != null ? p -> backAction.run() : null));
            return;
        }

        List<AuditLogger.AuditEntry> allEntries;
        if (world != null && x != null && y != null && z != null) {
            allEntries = audit.getEntriesForBlock(world, x, y, z, 500);
        } else if (filterPlayerUuid != null) {
            try {
                allEntries = audit.getEntriesForPlayer(UUID.fromString(filterPlayerUuid), 500);
            } catch (IllegalArgumentException e) {
                allEntries = new ArrayList<>();
            }
        } else {
            allEntries = audit.getRecentEntries(500, 0);
        }

        if (allEntries.isEmpty()) {
            bridge.showNotice(player,
                Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__AUDIT__TITLE)),
                    SOFT_BLUE, TextDecoration.BOLD),
                List.of(Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__AUDIT__NO_ENTRIES)),
                    SOFT_GRAY)),
                new DialogButton("exit",
                    Component.text(stripColor(Translator.get(backAction != null ? TranslationKey.DIALOGS__BACK : TranslationKey.DIALOGS__CLOSE)), SOFT_GRAY),
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
                    backAction != null ? p -> backAction.run() : null));
            return;
        }

        int totalPages = Math.max(1, (int) Math.ceil(allEntries.size() / (double) PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * PER_PAGE;
        int to = Math.min(from + PER_PAGE, allEntries.size());
        List<AuditLogger.AuditEntry> pageEntries = allEntries.subList(from, to);

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__AUDIT__TITLE)),
            SOFT_BLUE, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__PAGE))
                .replace("{current}", String.valueOf(safePage + 1))
                .replace("{total}", String.valueOf(totalPages)),
            TextColor.color(0x888888))));

        for (AuditLogger.AuditEntry entry : pageEntries) {
            String actionStr = actionLabel(entry.action());
            String timeStr = DATE_FMT.format(new Date(entry.timestamp()));
            TextColor color = actionColor(entry.action());
            body.add(DialogBodyEntry.text(Component.text()
                .append(Component.text(timeStr, SOFT_GRAY))
                .append(Component.text(" ", SOFT_GRAY))
                .append(Component.text(actionStr, color))
                .append(Component.text(" - " + entry.playerName(), NamedTextColor.WHITE))
                .build()));
        }

        List<DialogButton> buttons = new ArrayList<>();

        if (safePage > 0) {
            int prev = safePage - 1;
            buttons.add(new DialogButton("prev",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV_HINT)), TextColor.color(0x888888)),
                p -> showLogs(player, world, x, y, z, filterPlayerUuid, prev, backAction)));
        }
        if (safePage + 1 < totalPages) {
            int next = safePage + 1;
            buttons.add(new DialogButton("next",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT_HINT)), TextColor.color(0x888888)),
                p -> showLogs(player, world, x, y, z, filterPlayerUuid, next, backAction)));
        }

        DialogButton exitBtn = new DialogButton("exit",
            Component.text(stripColor(Translator.get(backAction != null ? TranslationKey.DIALOGS__BACK : TranslationKey.DIALOGS__CLOSE)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            backAction != null ? p -> backAction.run() : null);

        bridge.showMultiAction(player, title, body, buttons, exitBtn, 1);
    }

    private static TextColor actionColor(AuditLogger.Action action) {
        return switch (action) {
            case ACCESS_DENIED, RAID_EXPLOSION -> PASTEL_CORAL;
            case OPENED -> SOFT_BLUE;
            case ITEM_TAKEN -> PASTEL_GOLD;
            case ITEM_PLACED -> PASTEL_MINT;
            default -> NamedTextColor.WHITE;
        };
    }

    private static String actionLabel(AuditLogger.Action action) {
        TranslationKey key = switch (action) {
            case ACCESS_DENIED -> TranslationKey.INVENTORIES__AUDIT__ACTION_ACCESS_DENIED;
            case OPENED -> TranslationKey.INVENTORIES__AUDIT__ACTION_OPENED;
            case ITEM_TAKEN -> TranslationKey.INVENTORIES__AUDIT__ACTION_ITEM_TAKEN;
            case ITEM_PLACED -> TranslationKey.INVENTORIES__AUDIT__ACTION_ITEM_PLACED;
            case RAID_EXPLOSION -> TranslationKey.INVENTORIES__AUDIT__ACTION_RAID_EXPLOSION;
            default -> TranslationKey.INVENTORIES__AUDIT__ACTION_UNKNOWN;
        };
        return stripColor(Translator.get(key));
    }
}
