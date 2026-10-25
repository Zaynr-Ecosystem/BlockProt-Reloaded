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
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.jetbrains.annotations.NotNull;

import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;

/**
 * Native Bedrock user menu form for {@code /bp user}.
 */
public final class BedrockUserMenuForm {

    private BedrockUserMenuForm() {}

    public static void show(@NotNull Player player) {
        String title = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__TITLE));
        String friends = stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__MANAGE));
        String settings = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_SETTINGS));
        String placements = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__PLACEMENTS));
        String transfer = stripColor(Translator.get(TranslationKey.INVENTORIES__TRANSFER__TITLE));
        String about = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__ABOUT));
        String close = stripColor(Translator.get(TranslationKey.DIALOGS__CLOSE));

        SimpleForm.Builder builder = SimpleForm.builder()
            .title(title)
            .content(stripColor(Translator.get(TranslationKey.DIALOGS__CHOOSE_OPTION)))
            .button(friends, FormImage.Type.PATH, "textures/ui/FriendsIcon")
            .button(settings, FormImage.Type.PATH, "textures/ui/settings_glyph_color_2x")
            .button(placements, FormImage.Type.PATH, "textures/items/book_normal")
            .button(transfer, FormImage.Type.PATH, "textures/items/name_tag")
            .button(about, FormImage.Type.PATH, "textures/items/nether_star")
            .button(close, FormImage.Type.PATH, "textures/ui/cancel")
            .validResultHandler(response -> {
                int clicked = response.clickedButtonId();
                BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                    if (!player.isOnline()) return;
                    switch (clicked) {
                        case 0 -> BedrockFriendManageForm.show(player);
                        case 1 -> BedrockUserSettingsForm.show(player);
                        case 2 -> BedrockStatsForm.showUserStats(player);
                        case 3 -> BedrockTransferForm.showTransferAllPrompt(player);
                        case 4 -> showAbout(player);
                    }
                });
            });

        BedrockBridge.sendForm(player, builder.build());
    }

    @SuppressWarnings("deprecation")
    private static void showAbout(@NotNull Player player) {
        String title = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__ABOUT));
        String version = de.sean.blockprot.bukkit.BlockProt.getInstance().getDescription().getVersion();
        String back = stripColor(Translator.get(TranslationKey.DIALOGS__BACK));
        String desc = stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__DESCRIPTION));
        String verLabel = stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__VERSION_LABEL));
        String maintainer = stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__MAINTAINER));
        String authorLabel = stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__AUTHOR_LABEL));
        String content = desc + "\n\n" + verLabel + ": " + version + "\n" + authorLabel + "\n" + maintainer;

        SimpleForm.Builder builder = SimpleForm.builder()
            .title(title)
            .content(content)
            .button(back, FormImage.Type.PATH, "textures/ui/back_button_default")
            .validResultHandler(response -> BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                if (player.isOnline()) show(player);
            }));

        BedrockBridge.sendForm(player, builder.build());
    }
}
