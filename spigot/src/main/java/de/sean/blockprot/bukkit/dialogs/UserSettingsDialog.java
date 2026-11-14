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

import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.listeners.BlockEventListener;
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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class UserSettingsDialog {

    private UserSettingsDialog() {}

    public static void show(@NotNull Player player) {
        show(player, DialogOrigin.USER_MENU);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        PlayerSettingsHandler settings = new PlayerSettingsHandler(player);

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__USER_SETTINGS)),
            PASTEL_GOLD, TextDecoration.BOLD
        );

        boolean hintsEnabled = !settings.hasPlayerInteractedWithMenu();

        boolean colorblind = settings.getColorblindMode();

        String rawLock = stripColor(Translator.get(TranslationKey.INVENTORIES__LOCK_ON_PLACE));
        String rawHints = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__HINTS));
        String rawNotif = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_SETTINGS_NOTIFICATIONS));
        String rawCb = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_SETTINGS_COLORBLIND));

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__SETTINGS__HEADER)), SOFT_GRAY)));
        body.add(DialogBodyEntry.text(Component.empty()));

        String autoLockDesc = stripColor(Translator.get(TranslationKey.DIALOGS__SETTINGS__AUTO_LOCK_DESC));
        String hintsDesc = stripColor(Translator.get(TranslationKey.DIALOGS__SETTINGS__HINTS_DESC));
        String notifDesc = stripColor(Translator.get(TranslationKey.DIALOGS__SETTINGS__NOTIFICATIONS_DESC));
        String cbDesc = stripColor(Translator.get(TranslationKey.DIALOGS__SETTINGS__COLORBLIND_DESC));

        DialogButton lockBtn = new DialogButton("lock",
            toggleLabel(rawLock, settings.getLockOnPlace(), colorblind, PASTEL_MINT, PASTEL_CORAL),
            tooltip(rawLock, settings.getLockOnPlace(), autoLockDesc),
            p -> {
                PlayerSettingsHandler h = new PlayerSettingsHandler(p);
                h.setLockOnPlace(!h.getLockOnPlace());
                BlockEventListener.invalidateSettings(p.getUniqueId());
                show(p, backOrigin);
            }
        );

        DialogButton hintsBtn = new DialogButton("hints",
            toggleLabel(rawHints, hintsEnabled, colorblind, PASTEL_MINT, PASTEL_CORAL),
            tooltip(rawHints, hintsEnabled, hintsDesc),
            p -> {
                PlayerSettingsHandler h = new PlayerSettingsHandler(p);
                h.setHasPlayerInteractedWithMenu(hintsEnabled);
                show(p, backOrigin);
            }
        );

        DialogButton notifBtn = new DialogButton("notifications",
            toggleLabel(rawNotif, settings.getNotificationsEnabled(), colorblind, PASTEL_MINT, PASTEL_CORAL),
            tooltip(rawNotif, settings.getNotificationsEnabled(), notifDesc),
            p -> {
                PlayerSettingsHandler h = new PlayerSettingsHandler(p);
                h.setNotificationsEnabled(!h.getNotificationsEnabled());
                show(p, backOrigin);
            }
        );

        DialogButton cbBtn = new DialogButton("colorblind",
            toggleLabel(rawCb, colorblind, colorblind, PASTEL_MINT, PASTEL_CORAL),
            tooltip(rawCb, colorblind, cbDesc),
            p -> {
                PlayerSettingsHandler h = new PlayerSettingsHandler(p);
                h.setColorblindMode(!h.getColorblindMode());
                show(p, backOrigin);
            }
        );

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton exitBtn = new DialogButton("exit",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            originHint(exitOrigin),
            originBack(player, exitOrigin)
        );

        List<DialogButton> actions = new ArrayList<>();
        actions.add(lockBtn);
        actions.add(hintsBtn);
        actions.add(notifBtn);
        actions.add(cbBtn);

        if (de.sean.blockprot.bukkit.bedrock.BedrockBridge.isBedrockPlayer(player)) {
            String rawForms = stripColor(Translator.get(TranslationKey.DIALOGS__SETTINGS__PREFER_BEDROCK_FORMS));
            String formsDesc = stripColor(Translator.get(TranslationKey.DIALOGS__SETTINGS__PREFER_BEDROCK_FORMS_DESC));
            boolean preferForms = settings.getPreferBedrockForms();
            DialogButton formsBtn = new DialogButton("prefer_bedrock_forms",
                toggleLabel(rawForms, preferForms, colorblind, PASTEL_MINT, PASTEL_CORAL),
                tooltip(rawForms, preferForms, formsDesc),
                p -> {
                    PlayerSettingsHandler h = new PlayerSettingsHandler(p);
                    boolean nextState = !h.getPreferBedrockForms();
                    h.setPreferBedrockForms(nextState);
                    if (nextState) {
                        de.sean.blockprot.bukkit.BlockProt.getFoliaLib().getScheduler().runAtEntityLater(p, () -> {
                            de.sean.blockprot.bukkit.bedrock.forms.BedrockUserSettingsForm.show(p);
                        }, 1L);
                    } else {
                        show(p, backOrigin);
                    }
                }
            );
            actions.add(formsBtn);
        } else if (de.sean.blockprot.bukkit.BlockProt.getDefaultConfig().isDialogsEnabled()) {
            String rawDialogs = stripColor(Translator.get(TranslationKey.DIALOGS__SETTINGS__PREFER_DIALOGS));
            String dialogsDesc = stripColor(Translator.get(TranslationKey.DIALOGS__SETTINGS__PREFER_DIALOGS_DESC));
            boolean preferDialogs = settings.getPreferDialogs();
            DialogButton dialogsBtn = new DialogButton("prefer_dialogs",
                toggleLabel(rawDialogs, preferDialogs, colorblind, PASTEL_MINT, PASTEL_CORAL),
                tooltip(rawDialogs, preferDialogs, dialogsDesc),
                p -> {
                    PlayerSettingsHandler h = new PlayerSettingsHandler(p);
                    boolean nextState = !h.getPreferDialogs();
                    h.setPreferDialogs(nextState);
                    if (!nextState) {
                        prepareInventoryStateForSettings(p, backOrigin);
                        p.openInventory(new de.sean.blockprot.bukkit.inventories.UserSettingsInventory().fill(p));
                    } else {
                        show(p, backOrigin);
                    }
                }
            );
            actions.add(dialogsBtn);
        }

        bridge.showMultiAction(player, title, body, actions, exitBtn, 1);
    }

    private static void prepareInventoryStateForSettings(@NotNull Player player,
                                                         @NotNull DialogOrigin backOrigin) {
        de.sean.blockprot.bukkit.inventories.InventoryState state =
            new de.sean.blockprot.bukkit.inventories.InventoryState(null);
        state.friendSearchState = de.sean.blockprot.bukkit.inventories.InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
        if (DialogBridgeFactory.resolveOrigin(backOrigin) == DialogOrigin.USER_MENU) {
            state.origin = de.sean.blockprot.bukkit.inventories.InventoryState.MenuOrigin.USER_MENU;
            state.originStack.push(de.sean.blockprot.bukkit.inventories.InventoryState.MenuOrigin.USER_MENU);
        } else {
            state.origin = de.sean.blockprot.bukkit.inventories.InventoryState.MenuOrigin.NONE;
        }
        de.sean.blockprot.bukkit.inventories.InventoryState.set(player.getUniqueId(), state);
    }

    static Component originHint(DialogOrigin origin) {
        switch (origin) {
            case USER_MENU: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_USER_MENU)), TextColor.color(0x888888));
            default: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888));
        }
    }

    static DialogButton.DialogClickHandler originBack(Player player, DialogOrigin origin) {
        switch (origin) {
            case USER_MENU: return p -> UserMenuDialog.show(p);
            default: return null;
        }
    }

    private static Component toggleLabel(String name, boolean enabled, boolean colorblind,
                                         TextColor onColor, TextColor offColor) {
        TextColor color = enabled ? onColor : offColor;
        String icon = colorblind
            ? (enabled ? "✔ " : "✖ ")
            : stripColor(Translator.get(enabled ? TranslationKey.ICON__TOGGLE_ON : TranslationKey.ICON__TOGGLE_OFF));
        return Component.text()
            .append(Component.text(icon, color))
            .append(Component.text(name, NamedTextColor.WHITE))
            .build();
    }

    private static Component tooltip(String name, boolean enabled, String description) {
        String status = enabled
            ? stripColor(Translator.get(TranslationKey.DIALOGS__STATUS_ENABLED))
            : stripColor(Translator.get(TranslationKey.DIALOGS__STATUS_DISABLED));
        return Component.join(
            JoinConfiguration.newlines(),
            Component.text(name, SOFT_BLUE, TextDecoration.BOLD),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__STATUS_LABEL)), SOFT_GRAY)
                .append(Component.text(status, enabled ? PASTEL_MINT : PASTEL_CORAL)),
            Component.text(description, TextColor.color(0x888888))
        );
    }
}
