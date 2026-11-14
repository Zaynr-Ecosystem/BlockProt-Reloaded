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
import de.sean.blockprot.bukkit.tasks.WorldProtectionEraser;
import de.sean.blockprot.bukkit.tasks.WorldProtectionEraser.UndoBatch;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ProtdelDialog {

    private static final TextColor PASTEL_CORAL = TextColor.color(0xFF6B6B);
    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_MINT = TextColor.color(0x98FF98);
    private static final TextColor PASTEL_GOLD = TextColor.color(0xF0E6A0);
    private static final TextColor SOFT_BLUE = TextColor.color(0x7EC8E3);

    private ProtdelDialog() {}

    public static void show(@NotNull Player player, @Nullable String worldName) {
        show(player, worldName, DialogOrigin.ADMIN_MENU);
    }

    public static void show(@NotNull Player player, @Nullable String worldName, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        if (worldName != null) {
            showConfirm(player, worldName, bridge, backOrigin);
            return;
        }

        showWorldSelector(player, bridge, backOrigin);
    }

    private static void showWorldSelector(@NotNull Player player, @NotNull DialogBridge bridge,
                                           @NotNull DialogOrigin backOrigin) {
        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__WORLD_PROT_DEL__TITLE)),
            PASTEL_CORAL, TextDecoration.BOLD
        );

        List<World> worlds = Bukkit.getWorlds();
        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__HEADER)), SOFT_GRAY)));
        body.add(DialogBodyEntry.text(Component.empty()));

        List<DialogButton> buttons = new ArrayList<>();
        for (World world : worlds) {
            String wName = world.getName();
            String hint = stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__WORLD_HINT))
                .replace("{world}", wName);
            buttons.add(new DialogButton("world_" + wName,
                Component.text(wName, NamedTextColor.WHITE),
                Component.text(hint, TextColor.color(0x888888)),
                p -> showConfirm(p, wName, bridge, backOrigin)));
        }

        List<DialogButton> undoNav = new ArrayList<>();
        List<UndoBatch> batches = WorldProtectionEraser.getUndoHistory(player.getUniqueId());
        if (!batches.isEmpty()) {
            undoNav.add(new DialogButton("undo_all",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__UNDO_LABEL))
                    .replace("{count}", String.valueOf(batches.size())), PASTEL_GOLD),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__RESTORE_HINT))
                    .replace("{count}", String.valueOf(
                        batches.stream().mapToInt(b -> b.snapshots().size()).sum())),
                    TextColor.color(0x888888)),
                p -> showUndoSelector(p, bridge, backOrigin)));
        }

        buttons.addAll(undoNav);

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton exitBtn = DialogNavigation.backButton(
            exitOrigin,
            exitOrigin == DialogOrigin.ADMIN_MENU ? p -> AdminMenuDialog.show(p)
                : exitOrigin == DialogOrigin.USER_MENU ? p -> UserMenuDialog.show(p)
                : null,
            exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__RETURN_PREVIOUS : null
        );

        bridge.showMultiAction(player, title, body, buttons, exitBtn, 2);
    }

    private static void showUndoSelector(@NotNull Player player, @NotNull DialogBridge bridge,
                                          @NotNull DialogOrigin backOrigin) {
        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__UNDO_TITLE)),
            PASTEL_GOLD, TextDecoration.BOLD);

        List<UndoBatch> batches = WorldProtectionEraser.getUndoHistory(player.getUniqueId());
        List<DialogBodyEntry> body = new ArrayList<>();
        if (batches.isEmpty()) {
            body.add(DialogBodyEntry.text(Component.text(
                stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__NO_HISTORY)),
                TextColor.color(0x888888))));
        }

        List<DialogButton> buttons = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < batches.size(); i++) {
            UndoBatch ub = batches.get(i);
            String dateStr = sdf.format(new Date(ub.timestamp()));
            int count = ub.snapshots().size();
            int idx = i;
            buttons.add(new DialogButton("undo_" + i,
                Component.text(stripColor(Translator.get(TranslationKey.ICON__UNDO)) + dateStr + " (" + count + stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__BLOCKS_SUFFIX)) + ")", NamedTextColor.WHITE),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__RESTORE_HINT))
                    .replace("{count}", String.valueOf(count)), TextColor.color(0x888888)),
                p -> {
                    executeUndo(player, ub);
                    showUndoSelector(player, bridge, backOrigin);
                }
            ));
        }

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton exitBtn = DialogNavigation.backButton(
            exitOrigin,
            exitOrigin == DialogOrigin.NONE ? null : p -> showWorldSelector(p, bridge, backOrigin),
            exitOrigin == DialogOrigin.NONE ? null : TranslationKey.DIALOGS__RETURN_PREVIOUS
        );

        bridge.showMultiAction(player, title, body, buttons, exitBtn, 1);
    }

    private static void showConfirm(@NotNull Player player, @NotNull String worldName,
                                     @NotNull DialogBridge bridge, @NotNull DialogOrigin backOrigin) {
        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__WORLD_PROT_DEL__CONFIRM_TITLE)),
            PASTEL_CORAL, TextDecoration.BOLD
        );

        Component worldComp = Component.text(worldName, PASTEL_GOLD);

        DialogButton yesBtn = new DialogButton("confirm",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__CONFIRM)),
                PASTEL_CORAL, TextDecoration.BOLD),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__WORLD_HINT))
                .replace("{world}", worldName), PASTEL_CORAL),
            p -> executeDelete(player, worldName, bridge, backOrigin)
        );

        DialogButton noBtn = new DialogButton("cancel",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__CANCEL)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)),
                TextColor.color(0x888888)),
            p -> show(p, null, backOrigin)
        );

        bridge.showConfirmation(player, title,
            List.of(
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__HEADER))
                    + " - " + worldName + "?", NamedTextColor.WHITE),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__CANT_UNDO)), PASTEL_CORAL)),
            yesBtn, noBtn);
    }

    private static void executeDelete(@NotNull Player player, @NotNull String worldName,
                                       @NotNull DialogBridge bridge, @NotNull DialogOrigin backOrigin) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            ComponentMessages.send(player, Component.text(
                stripColor(Translator.get(TranslationKey.MESSAGES__WORLD_PROT_DEL_WORLD_NOT_FOUND))
                    .replace("{world}", worldName), PASTEL_CORAL));
            show(player, null, backOrigin);
            return;
        }

        ComponentMessages.send(player, Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__DELETING))
                .replace("{world}", worldName), SOFT_GRAY));

        WorldProtectionEraser.erase(world, player.getUniqueId(), count -> {
            String msg = count == 0
                ? stripColor(Translator.get(TranslationKey.MESSAGES__WORLD_PROT_DEL_NONE))
                    .replace("{world}", worldName)
                : stripColor(Translator.get(TranslationKey.MESSAGES__WORLD_PROT_DEL_DONE))
                    .replace("{world}", worldName)
                    .replace("{count}", String.valueOf(count));
            ComponentMessages.send(player, Component.text(msg, count == 0 ? SOFT_GRAY : PASTEL_MINT));
            if (player.isOnline()) {
                BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> show(player, null, backOrigin));
            }
        });
    }

    private static void executeUndo(@NotNull Player player, @NotNull UndoBatch batch) {
        WorldProtectionEraser.undo(player.getUniqueId(), batch, restored -> ComponentMessages.send(player,
            Component.text(stripColor(Translator.get(TranslationKey.MESSAGES__WORLD_PROT_DEL_UNDO_DONE))
                .replace("{count}", String.valueOf(restored)), PASTEL_MINT)));
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orxA-F]", "");
    }
}
