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
import de.sean.blockprot.bukkit.dialogs.LockableCategoryDialog;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.RedstoneSettingsHandler;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.jetbrains.annotations.NotNull;

import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;

/**
 * Native Bedrock settings form for block protection flags with instant toggling and thread safety.
 */
public final class BedrockBlockSettingsForm {

    private BedrockBlockSettingsForm() {}

    public static void show(@NotNull Player player, @NotNull Block block, @NotNull BlockNBTHandler handler) {
        if (!org.bukkit.Bukkit.isPrimaryThread() && (!BlockProt.hasFoliaLib() || !BlockProt.getFoliaLib().getScheduler().isOwnedByCurrentRegion(block))) {
            BlockProt.getFoliaLib().getScheduler().runAtLocation(block.getLocation(), task -> {
                if (player.isOnline()) show(player, block, new BlockNBTHandler(block));
            });
            return;
        }
        String title = stripColor(Translator.get(TranslationKey.INVENTORIES__BLOCK_SETTINGS__TITLE));
        String rawRedstone = stripColor(Translator.get(TranslationKey.INVENTORIES__REDSTONE__REDSTONE_PROTECTION));
        String rawPiston = stripColor(Translator.get(TranslationKey.INVENTORIES__REDSTONE__PISTON_PROTECTION));
        String rawHopper = stripColor(Translator.get(TranslationKey.INVENTORIES__REDSTONE__HOPPER_PROTECTION));
        String back = stripColor(Translator.get(TranslationKey.DIALOGS__BACK));

        boolean isStorage = isStorageType(block.getType());
        RedstoneSettingsHandler rs = handler.getRedstoneHandler();

        boolean redstone = rs.getCurrentProtection();
        boolean piston = rs.getPistonProtection();
        boolean hopper = rs.getHopperProtection();

        String redstoneLabel = (redstone ? "[ON] " : "[OFF] ") + rawRedstone;
        String pistonLabel = (piston ? "[ON] " : "[OFF] ") + rawPiston;
        String hopperLabel = (hopper ? "[ON] " : "[OFF] ") + rawHopper;

        String content = LockableCategoryDialog.formatMaterialName(block.getType().name()) + "\n\n" + stripColor(Translator.get(TranslationKey.DIALOGS__CHOOSE_OPTION));

        SimpleForm.Builder builder = SimpleForm.builder()
            .title(title)
            .content(content)
            .button(redstoneLabel, FormImage.Type.PATH, redstone ? "textures/ui/check" : "textures/ui/cancel")
            .button(pistonLabel, FormImage.Type.PATH, piston ? "textures/ui/check" : "textures/ui/cancel");

        if (isStorage) {
            builder.button(hopperLabel, FormImage.Type.PATH, hopper ? "textures/ui/check" : "textures/ui/cancel");
        }

        builder.button(back, FormImage.Type.PATH, "textures/ui/back_button_default");

        builder.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BlockProt.getFoliaLib().getScheduler().runAtLocation(block.getLocation(), task -> {
                if (!player.isOnline()) return;
                BlockNBTHandler h = new BlockNBTHandler(block);
                RedstoneSettingsHandler r = h.getRedstoneHandler();
                if (clicked == 0) {
                    r.setCurrentProtection(!r.getCurrentProtection());
                    h.applyToOtherContainer();
                    show(player, block, h);
                } else if (clicked == 1) {
                    r.setPistonProtection(!r.getPistonProtection());
                    h.applyToOtherContainer();
                    show(player, block, h);
                } else if (isStorage && clicked == 2) {
                    r.setHopperProtection(!r.getHopperProtection());
                    h.applyToOtherContainer();
                    show(player, block, h);
                } else {
                    BedrockBlockLockForm.show(player, block, h);
                }
            });
        });

        BedrockBridge.sendForm(player, builder.build());
    }

    private static boolean isStorageType(Material m) {
        String n = m.name();
        return n.contains("CHEST") || n.equals("BARREL") || n.contains("SHULKER_BOX")
            || n.equals("HOPPER") || n.equals("DISPENSER") || n.equals("DROPPER")
            || n.equals("FURNACE") || n.equals("SMOKER") || n.equals("BLAST_FURNACE")
            || n.equals("BREWING_STAND") || n.equals("JUKEBOX")
            || n.equals("CHISELED_BOOKSHELF") || n.equals("DECORATED_POT") || n.equals("CRAFTER")
            || n.endsWith("_SHELF");
    }
}
