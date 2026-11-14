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
import de.sean.blockprot.bukkit.config.ReloadCoordinator;
import de.sean.blockprot.bukkit.config.ReloadReport;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Top-level category selector of the dialog-based admin config editor. Each
 * category screen lives in its own {@code AdminConfig*} dialog class, which
 * shares the button builders and pastel color palette defined here.
 */
public final class AdminConfigDialog {

    static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    static final TextColor PASTEL_MINT = TextColor.color(0x98FF98);
    static final TextColor PASTEL_CORAL = TextColor.color(0xFF6B6B);
    static final TextColor PASTEL_GOLD = TextColor.color(0xF0E6A0);
    static final TextColor SOFT_BLUE = TextColor.color(0x7EC8E3);
    static final TextColor PASTEL_PURPLE = TextColor.color(0xB39DDB);

    private AdminConfigDialog() {}

    public static void show(@NotNull Player player) {
        show(player, DialogOrigin.ADMIN_MENU);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        showCategories(player, backOrigin);
    }

    private static void showCategories(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__TITLE)),
            SOFT_BLUE, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__HINT)), TextColor.color(0x888888))));

        List<DialogButton> buttons = new ArrayList<>();
        buttons.add(catBtn(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_LANGUAGE)), p -> AdminConfigLanguageDialog.show(p, backOrigin)));
        buttons.add(catBtn(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_WORLDS)), p -> AdminConfigWorldsDialog.show(p, backOrigin)));
        buttons.add(catBtn(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_PLAYERS)), p -> AdminConfigPlayersDialog.show(p, backOrigin)));
        buttons.add(catBtn(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_BLOCKS)), p -> AdminConfigBlocksDialog.show(p, backOrigin)));
        buttons.add(catBtn(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_ENTITY)), p -> AdminConfigEntityDialog.show(p, backOrigin)));
        buttons.add(catBtn(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_EXPIRY)), p -> AdminConfigExpiryDialog.show(p, backOrigin)));
        buttons.add(catBtn(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_RAID)), p -> AdminConfigRaidDialog.show(p, backOrigin)));
        buttons.add(catBtn(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_NOTIFICATIONS)), p -> AdminConfigNotificationsDialog.show(p, backOrigin)));
        buttons.add(catBtn(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_MAINTENANCE)), p -> AdminConfigMaintenanceDialog.show(p, backOrigin)));

        String reload = stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__RELOAD));
        buttons.add(new DialogButton("reload",
            Component.text(stripColor(Translator.get(TranslationKey.ICON__RELOAD)) + reload, NamedTextColor.WHITE),
            Component.join(JoinConfiguration.newlines(),
                Component.text(reload, PASTEL_MINT),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_TO_OPEN)), TextColor.color(0x888888))),
            p -> {
                ReloadReport report = ReloadCoordinator.commitCommand();
                p.sendMessage(Component.text(
                    report.isSuccess()
                        ? Translator.get(TranslationKey.MESSAGES__ADMIN_RELOAD_DONE)
                        : Translator.get(TranslationKey.MESSAGES__ADMIN_RELOAD_FAILED)
                            .replace("{error}", report.getErrorMessage() != null ? report.getErrorMessage() : "unknown"),
                    report.isSuccess() ? PASTEL_MINT : PASTEL_CORAL));
            }));

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__RETURN_ADMIN_MENU)), TextColor.color(0x888888)),
            exitOrigin == DialogOrigin.NONE ? null : p -> AdminMenuDialog.show(p));

        bridge.showMultiAction(player, title, body, buttons, backBtn, 3);
    }

    static DialogButton catBtn(String label, DialogButton.DialogClickHandler handler) {
        return new DialogButton(label.toLowerCase().replace(' ', '_'),
            Component.text(label, NamedTextColor.WHITE),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__VALUE_CLICK_EDIT)), TextColor.color(0x888888)),
            handler);
    }

    static DialogButton valueBtn(String id, String configKey, String title, String description,
                                   String currentValue, DialogButton.DialogClickHandler clickAction) {
        return new DialogButton(id,
            Component.text()
                .append(Component.text(title, NamedTextColor.WHITE))
                .append(Component.text(": ", SOFT_GRAY))
                .append(Component.text(currentValue, PASTEL_GOLD, TextDecoration.BOLD))
                .build(),
            Component.join(JoinConfiguration.newlines(),
                Component.text(description, SOFT_GRAY),
                Component.text(configKey, TextColor.color(0x666666)),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__VALUE_CURRENT)) + currentValue, TextColor.color(0x888888)),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__VALUE_CLICK_EDIT)), TextColor.color(0x888888))),
            clickAction);
    }

    static DialogButton toggleBtn(String id, String configKey, String title, String description,
                                   boolean active, boolean colorblind, DialogButton.DialogClickHandler handler) {
        TextColor c = active ? PASTEL_MINT : PASTEL_CORAL;
        String icon = BpDialogStyles.indicatorIcon(active, colorblind);
        return new DialogButton(id,
            Component.text()
                .append(Component.text(icon, c))
                .append(Component.text(title, NamedTextColor.WHITE))
                .build(),
            Component.join(JoinConfiguration.newlines(),
                Component.text(description, SOFT_GRAY),
                Component.text(configKey, TextColor.color(0x666666)),
                Component.text(stripColor(Translator.get(active ? TranslationKey.ICON__TRUE : TranslationKey.ICON__FALSE)), c),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__VALUE_BOOL)), TextColor.color(0x888888))),
            handler);
    }

    static DialogButton toggleBtn(String id, String configKey, String title, String description,
                                   boolean active, DialogButton.DialogClickHandler handler) {
        return toggleBtn(id, configKey, title, description, active, false, handler);
    }

    static DialogButton dialogToggleBtn(String id, String configKey, String title, String description,
                                           boolean active, boolean colorblind, DialogButton.DialogClickHandler handler) {
        TextColor c = active ? PASTEL_MINT : PASTEL_CORAL;
        String marker = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__DIALOG_MARKER));
        String icon = BpDialogStyles.indicatorIcon(active, colorblind);
        return new DialogButton(id,
            Component.text()
                .append(Component.text(icon, c))
                .append(Component.text(title, NamedTextColor.WHITE))
                .build(),
            Component.join(JoinConfiguration.newlines(),
                Component.text(marker, PASTEL_GOLD),
                Component.text(description, SOFT_GRAY),
                Component.text(configKey, TextColor.color(0x666666)),
                Component.text(stripColor(Translator.get(active ? TranslationKey.ICON__TRUE : TranslationKey.ICON__FALSE)), c),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__VALUE_BOOL)), TextColor.color(0x888888))),
            handler);
    }

    static DialogButton dialogToggleBtn(String id, String configKey, String title, String description,
                                           boolean active, DialogButton.DialogClickHandler handler) {
        return dialogToggleBtn(id, configKey, title, description, active, false, handler);
    }

    /**
     * Renders a category screen with a back button that returns to the
     * top-level category selector. The back button label follows the exit
     * origin and shows the close label when the editor is a standalone entry.
     */
    static void bridgeReturn(@NotNull Player player, @NotNull DialogBridge bridge,
                               @NotNull Component title, @NotNull List<DialogBodyEntry> body,
                               @NotNull List<DialogButton> buttons, @NotNull DialogOrigin backOrigin) {
        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            exitOrigin == DialogOrigin.NONE ? 
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLOSE)), TextColor.color(0x888888)) :
                returnHint(),
            exitOrigin == DialogOrigin.NONE ? null : p -> showCategories(p, backOrigin));
        bridge.showMultiAction(player, title, body, buttons, backBtn, 2);
    }

    static Component returnHint() {
        return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__RETURN_CATEGORIES)), TextColor.color(0x888888));
    }

    static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orxA-F]", "");
    }
}