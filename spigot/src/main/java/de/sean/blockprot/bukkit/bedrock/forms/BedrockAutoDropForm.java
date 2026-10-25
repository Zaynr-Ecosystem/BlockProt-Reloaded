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
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;

/**
 * Native Bedrock form for Auto-Drop configuration.
 */
public final class BedrockAutoDropForm {

    private static final List<BlockFamilyParser.Family> FAMILIES = List.of(
        BlockFamilyParser.Family.TILE_ENTITIES,
        BlockFamilyParser.Family.SHULKER_BOXES,
        BlockFamilyParser.Family.BLOCKS,
        BlockFamilyParser.Family.DOORS,
        BlockFamilyParser.Family.ENTITIES
    );

    private BedrockAutoDropForm() {}

    public static void show(@NotNull Player player) {
        String title = stripColor(Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__TITLE));
        String choose = stripColor(Translator.get(TranslationKey.DIALOGS__CHOOSE_OPTION));
        String back = stripColor(Translator.get(TranslationKey.DIALOGS__BACK));

        SimpleForm.Builder builder = SimpleForm.builder()
            .title(title)
            .content(choose);

        for (BlockFamilyParser.Family f : FAMILIES) {
            builder.button(f.name(), FormImage.Type.PATH, "textures/items/hopper");
        }
        builder.button(back, FormImage.Type.PATH, "textures/ui/back_button_default");

        builder.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                if (!player.isOnline()) return;
                if (clicked >= 0 && clicked < FAMILIES.size()) {
                    show(player);
                } else {
                    BedrockAdminMenuForm.show(player);
                }
            });
        });

        BedrockBridge.sendForm(player, builder.build());
    }
}
