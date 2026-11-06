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
import de.sean.blockprot.bukkit.commands.TransferCommand;
import de.sean.blockprot.bukkit.inventories.TextInput;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_CORAL;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_GOLD;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_MINT;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_PURPLE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_BLUE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_GRAY;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class UserMenuDialog {

    private UserMenuDialog() {}

    public static void show(@NotNull Player player) {
        show(player, DialogOrigin.NONE);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__TITLE)),
            PASTEL_GOLD, TextDecoration.BOLD
        );

        String settings = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__SETTINGS));
        String friends = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__FRIENDS));
        String placements = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__PLACEMENTS));
        String about = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__ABOUT));
        String transfer = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__TRANSFER));

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__USER_MENU__HEADER)), SOFT_GRAY)));
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__CHOOSE_OPTION)), TextColor.color(0x888888))));

        DialogButton settingsBtn = new DialogButton("settings",
            Component.text(stripColor(Translator.get(TranslationKey.ICON__SETTINGS)) + settings, NamedTextColor.WHITE),
            tooltip(stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__SETTINGS)), PASTEL_MINT),
            p -> {
                new PlayerSettingsHandler(p).setHasPlayerInteractedWithMenu(true);
                UserSettingsDialog.show(p);
            }
        );

        DialogButton friendsBtn = new DialogButton("friends",
            Component.text(stripColor(Translator.get(TranslationKey.ICON__FRIENDS)) + friends, NamedTextColor.WHITE),
            tooltip(stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__FRIENDS)), SOFT_BLUE),
            p -> FriendManageDialog.show(p)
        );

        DialogButton statsBtn = new DialogButton("stats",
            Component.text(stripColor(Translator.get(TranslationKey.ICON__STATS)) + placements, NamedTextColor.WHITE),
            tooltip(stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__PLACEMENTS)), PASTEL_PURPLE),
            p -> StatsDialog.showUserStats(p, DialogOrigin.USER_MENU)
        );

        DialogButton transferBtn = new DialogButton("transfer",
            Component.text(transfer, NamedTextColor.WHITE),
            tooltip(stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__TRANSFER_LORE)), SOFT_BLUE),
            p -> {
                p.closeInventory();
                bridge.closeDialog(p);
                Consumer<String> handleName = name -> {
                    if (name == null || name.isBlank()) return;
                    TransferCommand.transferAll(p, name);
                };
                TextInput.open(p, BlockProt.getInstance(), handleName);
            }
        );

        DialogButton aboutBtn = new DialogButton("about",
            Component.text(stripColor(Translator.get(TranslationKey.ICON__ABOUT)) + about, NamedTextColor.WHITE),
            tooltip(stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__ABOUT)), PASTEL_GOLD),
            p -> AboutDialog.show(p)
        );

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton exitBtn = new DialogButton("exit",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            originHint(exitOrigin),
            originBack(player, exitOrigin)
        );

        List<DialogButton> actions = new ArrayList<>();
        if (!BlockProt.getDefaultConfig().isFriendFunctionalityDisabled()) {
            actions.add(friendsBtn);
        }
        actions.add(settingsBtn);
        actions.add(statsBtn);
        actions.add(transferBtn);
        actions.add(aboutBtn);
        bridge.showMultiAction(player, title, body, actions, exitBtn, 2);
    }

    private static Component tooltip(String description, TextColor accent) {
        return Component.join(
            JoinConfiguration.newlines(),
            Component.text(description, accent),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_TO_OPEN)), TextColor.color(0x888888))
        );
    }

    private static Component originHint(DialogOrigin origin) {
        switch (origin) {
            case ADMIN_MENU: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_ADMIN_MENU)), TextColor.color(0x888888));
            default: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888));
        }
    }

    static DialogButton.DialogClickHandler originBack(Player player, DialogOrigin origin) {
        switch (origin) {
            case ADMIN_MENU: return p -> AdminMenuDialog.show(p);
            default: return null;
        }
    }
}
