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
import de.sean.blockprot.bukkit.admin.AdminAction;
import de.sean.blockprot.bukkit.admin.AdminTierManager;
import de.sean.blockprot.bukkit.bedrock.BedrockBridge;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.jetbrains.annotations.NotNull;

import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;

/**
 * Native Bedrock admin menu form for {@code /bp admin} with verified textures.
 */
public final class BedrockAdminMenuForm {

    private BedrockAdminMenuForm() {}

    public static void show(@NotNull Player player) {
        String title = stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__TITLE));
        String config = stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__CONFIG));
        String lockables = stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__LOCKABLES));
        String autoDrop = stripColor(Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__TITLE));
        String reload = stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__RELOAD));
        String close = stripColor(Translator.get(TranslationKey.DIALOGS__CLOSE));

        SimpleForm.Builder builder = SimpleForm.builder()
            .title(title)
            .content(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_MENU__HEADER)))
            .button(config, FormImage.Type.PATH, "textures/ui/settings_glyph_color_2x")
            .button(lockables, FormImage.Type.PATH, "textures/items/book_normal")
            .button(autoDrop, FormImage.Type.PATH, "textures/items/hopper")
            .button(reload, FormImage.Type.PATH, "textures/ui/refresh_light")
            .button(close, FormImage.Type.PATH, "textures/ui/cancel")
            .validResultHandler(response -> {
                int clicked = response.clickedButtonId();
                BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                    if (!player.isOnline()) return;
                    AdminAction required = switch (clicked) {
                        case 0 -> AdminAction.CONFIG;
                        case 1, 2 -> AdminAction.LOCKABLES;
                        case 3 -> AdminAction.RELOAD;
                        default -> null;
                    };
                    if (required != null && AdminTierManager.deny(player, required)) return;
                    switch (clicked) {
                        case 0 -> BedrockAdminConfigForm.showCategories(player);
                        case 1 -> BedrockLockablesForm.show(player);
                        case 2 -> BedrockAutoDropForm.show(player);
                        case 3 -> {
                            new de.sean.blockprot.bukkit.tasks.BackupTask(BlockProt.getInstance().getDataFolder(), true).run();
                            de.sean.blockprot.bukkit.config.ReloadCoordinator.commitCommand();
                            de.sean.blockprot.bukkit.listeners.BlockEventListener.invalidateAllSettings();
                            ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__RELOAD_DONE));
                            show(player);
                        }
                    }
                });
            });

        BedrockBridge.sendForm(player, builder.build());
    }
}
