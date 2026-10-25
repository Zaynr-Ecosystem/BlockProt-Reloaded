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
import de.sean.blockprot.bukkit.config.DefaultConfig;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;

/**
 * Native Bedrock form for browsing and toggling lockable blocks with instant feedback.
 */
public final class BedrockLockablesForm {

    private BedrockLockablesForm() {}

    public static void show(@NotNull Player player) {
        String title = stripColor(Translator.get(TranslationKey.INVENTORIES__LOCKABLES__TITLE));
        String choose = stripColor(Translator.get(TranslationKey.DIALOGS__CHOOSE_OPTION));

        String tile = stripColor(Translator.get(TranslationKey.INVENTORIES__LOCKABLES__CATEGORY__STORAGE));
        String shulker = stripColor(Translator.get(TranslationKey.INVENTORIES__LOCKABLES__CATEGORY__SHULKERS));
        String doors = stripColor(Translator.get(TranslationKey.INVENTORIES__LOCKABLES__CATEGORY__DOORS));
        String entities = stripColor(Translator.get(TranslationKey.INVENTORIES__LOCKABLES__CATEGORY__ENTITIES));
        String back = stripColor(Translator.get(TranslationKey.DIALOGS__BACK));

        String content = choose;

        SimpleForm.Builder builder = SimpleForm.builder()
            .title(title)
            .content(content)
            .button(tile, FormImage.Type.PATH, "textures/items/minecart_chest")
            .button(shulker, FormImage.Type.PATH, "textures/items/shulker_shell")
            .button(doors, FormImage.Type.PATH, "textures/items/door_iron")
            .button(entities, FormImage.Type.PATH, "textures/items/name_tag")
            .button(back, FormImage.Type.PATH, "textures/ui/back_button_default")
            .validResultHandler(response -> {
                int clicked = response.clickedButtonId();
                BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                    if (!player.isOnline()) return;
                    switch (clicked) {
                        case 0 -> showCategoryItems(player, BlockFamilyParser.Family.TILE_ENTITIES);
                        case 1 -> showCategoryItems(player, BlockFamilyParser.Family.SHULKER_BOXES);
                        case 2 -> showCategoryItems(player, BlockFamilyParser.Family.DOORS);
                        case 3 -> showCategoryItems(player, BlockFamilyParser.Family.ENTITIES);
                        default -> BedrockAdminMenuForm.show(player);
                    }
                });
            });

        BedrockBridge.sendForm(player, builder.build());
    }

    private static void showCategoryItems(@NotNull Player player, @NotNull BlockFamilyParser.Family family) {
        DefaultConfig cfg = BlockProt.getDefaultConfig();
        String title = family.name();
        String back = stripColor(Translator.get(TranslationKey.DIALOGS__BACK));

        Set<Material> materials = BlockFamilyParser.getFamilyMembers(family);
        List<Material> matList = new ArrayList<>(materials);

        long activeCount = matList.stream().filter(m -> cfg.isLockable(m) || cfg.isLockableEntity(m)).count();
        boolean noneActive = activeCount == 0;

        String content = stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CAT_PREFIX))
            + family.name() + " (" + activeCount + "/" + matList.size() + ")\n\n"
            + stripColor(Translator.get(TranslationKey.DIALOGS__CHOOSE_OPTION));

        SimpleForm.Builder builder = SimpleForm.builder()
            .title(title)
            .content(content);

        String toggleAllLabel = stripColor(Translator.get(noneActive ? TranslationKey.DIALOGS__CLICK_ENABLE : TranslationKey.DIALOGS__CLICK_DISABLE));
        builder.button(toggleAllLabel, FormImage.Type.PATH, noneActive ? "textures/ui/check" : "textures/ui/cancel");

        for (Material mat : matList) {
            boolean active = cfg.isLockable(mat) || cfg.isLockableEntity(mat);
            String label = (active ? "[ON] " : "[OFF] ") + formatName(mat.name());
            builder.button(label, FormImage.Type.PATH, active ? "textures/ui/check" : "textures/ui/cancel");
        }

        builder.button(back, FormImage.Type.PATH, "textures/ui/back_button_default");

        builder.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                if (!player.isOnline()) return;
                if (clicked == 0) {
                    boolean target = noneActive;
                    cfg.batchSetLockable(matList, target, player);
                    showCategoryItems(player, family);
                } else if (clicked <= matList.size()) {
                    Material selected = matList.get(clicked - 1);
                    cfg.toggleLockable(selected, player);
                    showCategoryItems(player, family);
                } else {
                    show(player);
                }
            });
        });

        BedrockBridge.sendForm(player, builder.build());
    }

    private static String formatName(String raw) {
        String[] parts = raw.toLowerCase(java.util.Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
