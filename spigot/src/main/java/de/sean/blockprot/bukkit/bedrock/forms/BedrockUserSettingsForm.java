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

package de.sean.blockprot.bukkit.bedrock.forms;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.bedrock.BedrockBridge;
import de.sean.blockprot.bukkit.listeners.BlockEventListener;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.jetbrains.annotations.NotNull;

import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;

/**
 * Native Bedrock player settings form with instant toggle buttons and thread-safe persistence.
 */
public final class BedrockUserSettingsForm {

    private BedrockUserSettingsForm() {}

    public static void show(@NotNull Player player) {
        if (!org.bukkit.Bukkit.isPrimaryThread() && (!BlockProt.hasFoliaLib() || !BlockProt.getFoliaLib().getScheduler().isOwnedByCurrentRegion(player))) {
            BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                if (player.isOnline()) show(player);
            });
            return;
        }
        PlayerSettingsHandler handler = new PlayerSettingsHandler(player);

        String title = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_SETTINGS));
        String choose = stripColor(Translator.get(TranslationKey.DIALOGS__SETTINGS__HEADER));

        boolean lockOnPlace = handler.getLockOnPlace();
        boolean notifications = handler.getNotificationsEnabled();
        boolean colorblind = handler.getColorblindMode();
        boolean bedrockForms = handler.getPreferBedrockForms();

        String lockLabel = (lockOnPlace ? "[ON] " : "[OFF] ") + stripColor(Translator.get(TranslationKey.INVENTORIES__LOCK_ON_PLACE));
        String notifLabel = (notifications ? "[ON] " : "[OFF] ") + stripColor(Translator.get(TranslationKey.INVENTORIES__USER_SETTINGS_NOTIFICATIONS));
        String cbLabel = (colorblind ? "[ON] " : "[OFF] ") + stripColor(Translator.get(TranslationKey.INVENTORIES__USER_SETTINGS_COLORBLIND));
        String formsLabel = (bedrockForms ? "[ON] " : "[OFF] ") + stripColor(Translator.get(TranslationKey.DIALOGS__SETTINGS__PREFER_BEDROCK_FORMS));
        String back = stripColor(Translator.get(TranslationKey.DIALOGS__BACK));

        String content = choose;

        SimpleForm.Builder builder = SimpleForm.builder()
            .title(title)
            .content(content)
            .button(lockLabel, FormImage.Type.PATH, lockOnPlace ? "textures/ui/check" : "textures/ui/cancel")
            .button(notifLabel, FormImage.Type.PATH, notifications ? "textures/ui/check" : "textures/ui/cancel")
            .button(cbLabel, FormImage.Type.PATH, colorblind ? "textures/ui/check" : "textures/ui/cancel")
            .button(formsLabel, FormImage.Type.PATH, bedrockForms ? "textures/ui/check" : "textures/ui/cancel")
            .button(back, FormImage.Type.PATH, "textures/ui/back_button_default")
            .validResultHandler(response -> {
                int clicked = response.clickedButtonId();
                BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                    if (!player.isOnline()) return;
                    PlayerSettingsHandler h = new PlayerSettingsHandler(player);
                    switch (clicked) {
                        case 0 -> {
                            boolean next = !h.getLockOnPlace();
                            h.setLockOnPlace(next);
                            BlockEventListener.invalidateSettings(player.getUniqueId());
                            show(player);
                        }
                        case 1 -> {
                            boolean next = !h.getNotificationsEnabled();
                            h.setNotificationsEnabled(next);
                            show(player);
                        }
                        case 2 -> {
                            boolean next = !h.getColorblindMode();
                            h.setColorblindMode(next);
                            show(player);
                        }
                        case 3 -> {
                            boolean next = !h.getPreferBedrockForms();
                            h.setPreferBedrockForms(next);
                            if (!next) {
                                player.openInventory(new de.sean.blockprot.bukkit.inventories.UserSettingsInventory().fill(player));
                            } else {
                                show(player);
                            }
                        }
                        default -> BedrockUserMenuForm.show(player);
                    }
                });
            });

        BedrockBridge.sendForm(player, builder.build());
    }
}
