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
import de.sean.blockprot.bukkit.config.DefaultConfig;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Notifications category of the dialog-based admin config editor.
 */
public final class AdminConfigNotificationsDialog {

    private AdminConfigNotificationsDialog() {}

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        Component title = Component.text(
            AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_NOTIFICATIONS)),
            AdminConfigDialog.PASTEL_PURPLE, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_NOTIFICATIONS), AdminConfigDialog.SOFT_GRAY)));

        boolean colorblind = new de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler(player).getColorblindMode();
        List<DialogButton> buttons = new ArrayList<>();
        buttons.add(AdminConfigDialog.toggleBtn("notify_op_of_updates", "notify_op_of_updates",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__NOTIFY_OPS_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__NOTIFY_OPS),
            cfg.shouldNotifyOpOfUpdates(),
            colorblind,
            p -> { cfg.setNotifyOpOfUpdates(!cfg.shouldNotifyOpOfUpdates()); show(p, backOrigin); }));
        buttons.add(AdminConfigDialog.toggleBtn("notify_admins_on_join", "notify_admins_on_join",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__NOTIFY_ADMINS_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__NOTIFY_ADMINS),
            cfg.isNotifyAdminsOnJoinSettingEnabled(),
            colorblind,
            p -> { cfg.setNotifyAdminsOnJoin(!cfg.isNotifyAdminsOnJoinSettingEnabled()); show(p, backOrigin); }));
        buttons.add(AdminConfigDialog.toggleBtn("owner_notifications.enabled", "owner_notifications.enabled",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__OWNER_ENABLED_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__OWNER_ENABLED),
            cfg.isOwnerNotificationsEnabled(),
            colorblind,
            p -> { cfg.setAndSave("owner_notifications.enabled", !cfg.isOwnerNotificationsEnabled()); show(p, backOrigin); }));

        boolean notifyOpen = cfg.getBukkitConfig().getBoolean("owner_notifications.notify_on_open", true);
        buttons.add(AdminConfigDialog.toggleBtn("owner_notifications.notify_on_open", "owner_notifications.notify_on_open",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__OWNER_OPEN_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__OWNER_OPEN),
            notifyOpen,
            colorblind,
            p -> { cfg.setAndSave("owner_notifications.notify_on_open", !notifyOpen); show(p, backOrigin); }));

        boolean notifyTake = cfg.getBukkitConfig().getBoolean("owner_notifications.notify_on_take", true);
        buttons.add(AdminConfigDialog.toggleBtn("owner_notifications.notify_on_take", "owner_notifications.notify_on_take",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__OWNER_TAKE_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__OWNER_TAKE),
            notifyTake,
            colorblind,
            p -> { cfg.setAndSave("owner_notifications.notify_on_take", !notifyTake); show(p, backOrigin); }));

        boolean notifyPlace = cfg.getBukkitConfig().getBoolean("owner_notifications.notify_on_place", true);
        buttons.add(AdminConfigDialog.toggleBtn("owner_notifications.notify_on_place", "owner_notifications.notify_on_place",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__OWNER_PLACE_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__OWNER_PLACE),
            notifyPlace,
            colorblind,
            p -> { cfg.setAndSave("owner_notifications.notify_on_place", !notifyPlace); show(p, backOrigin); }));

        AdminConfigDialog.bridgeReturn(player, bridge, title, body, buttons, backOrigin);
    }
}