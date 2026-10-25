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
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.tasks.VillagerLocateTask;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import de.sean.blockprot.nbt.LockReturnValue;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.cumulus.util.FormImage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;

/**
 * Native Bedrock lock menu form with verified textures.
 */
public final class BedrockBlockLockForm {

    private BedrockBlockLockForm() {}

    public static void show(@NotNull Player player, @NotNull Block block, @NotNull BlockNBTHandler handler) {
        if (!Bukkit.isPrimaryThread() && (!BlockProt.hasFoliaLib() || !BlockProt.getFoliaLib().getScheduler().isOwnedByCurrentRegion(block))) {
            BlockProt.getFoliaLib().getScheduler().runAtLocation(block.getLocation(), task -> {
                if (player.isOnline()) show(player, block, new BlockNBTHandler(block));
            });
            return;
        }
        boolean isNotProtected = handler.isNotProtected();
        boolean isOwner = handler.isOwner(player.getUniqueId());
        String blockName = block.getType().name();
        String customName = handler.getName();

        String title = (customName != null && !customName.isEmpty()) ? customName : blockName;
        StringBuilder content = new StringBuilder();
        content.append(blockName).append("\n");
        String ownerName = getOwnerName(handler);
        content.append(stripColor(Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__OWNER_LABEL)))
            .append(": ").append(!isNotProtected ? (ownerName != null ? ownerName : handler.getOwner()) : "-")
            .append("\n");

        SimpleForm.Builder builder = SimpleForm.builder()
            .title(title)
            .content(content.toString());

        List<Runnable> actions = new ArrayList<>();

        if (isNotProtected) {
            String lockBtn = stripColor(Translator.get(TranslationKey.INVENTORIES__LOCK));
            builder.button(lockBtn, FormImage.Type.PATH, "textures/ui/check");
            actions.add(() -> {
                LockReturnValue ret = handler.lockBlock(player);
                if (!ret.success && ret.reason != null) {
                    ComponentMessages.sendLegacy(player, Translator.get(ret.reason));
                }
                show(player, block, handler);
            });
        } else if (isOwner || de.sean.blockprot.bukkit.admin.AdminTierManager.hasPermission(player, de.sean.blockprot.bukkit.admin.AdminAction.UNLOCK)) {
            String unlockBtn = stripColor(Translator.get(TranslationKey.INVENTORIES__UNLOCK));
            builder.button(unlockBtn, FormImage.Type.PATH, "textures/ui/cancel");
            actions.add(() -> {
                handler.clear();
                handler.applyToOtherContainer();
                ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__UNLOCKED));
                show(player, block, handler);
            });

            String friendsBtn = stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__MANAGE));
            builder.button(friendsBtn, FormImage.Type.PATH, "textures/ui/FriendsIcon");
            actions.add(() -> BedrockFriendManageForm.show(player, handler, () -> show(player, block, handler)));

            String nameBtn = stripColor(Translator.get(TranslationKey.INVENTORIES__SET_BLOCK_NAME));
            builder.button(nameBtn, FormImage.Type.PATH, "textures/ui/pencil_edit_icon");
            actions.add(() -> showNamePrompt(player, block, handler));

            String transferBtn = stripColor(Translator.get(TranslationKey.INVENTORIES__TRANSFER__BUTTON));
            builder.button(transferBtn, FormImage.Type.PATH, "textures/items/name_tag");
            actions.add(() -> BedrockTransferForm.showTransferBlockPrompt(player, block));

            String settingsBtn = stripColor(Translator.get(TranslationKey.INVENTORIES__BLOCK_SETTINGS__TITLE));
            builder.button(settingsBtn, FormImage.Type.PATH, "textures/ui/settings_glyph_color_2x");
            actions.add(() -> BedrockBlockSettingsForm.show(player, block, handler));

            if (isWorkstation(block.getType())) {
                String locateVillager = stripColor(Translator.get(TranslationKey.INVENTORIES__LOCATE_VILLAGER));
                builder.button(locateVillager, FormImage.Type.PATH, "textures/ui/World");
                actions.add(() -> {
                    int seconds = BlockProt.getDefaultConfig().getVillagerLocateSeconds();
                    VillagerLocateTask.startIfLinked(player, block, seconds);
                });
            }
        }

        String close = stripColor(Translator.get(TranslationKey.DIALOGS__CLOSE));
        builder.button(close, FormImage.Type.PATH, "textures/ui/cancel");
        actions.add(() -> {});

        builder.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            if (clicked >= 0 && clicked < actions.size()) {
                BlockProt.getFoliaLib().getScheduler().runAtLocation(block.getLocation(), task -> {
                    if (!player.isOnline()) return;
                    actions.get(clicked).run();
                });
            }
        });

        BedrockBridge.sendForm(player, builder.build());
    }

    private static void showNamePrompt(@NotNull Player player, @NotNull Block block, @NotNull BlockNBTHandler handler) {
        String title = stripColor(Translator.get(TranslationKey.INVENTORIES__SET_BLOCK_NAME));
        String currentName = handler.getName();

        CustomForm.Builder builder = CustomForm.builder()
            .title(title)
            .input(stripColor(Translator.get(TranslationKey.DIALOGS__CHOOSE_OPTION)), "", currentName != null ? currentName : "")
            .validResultHandler(response -> {
                CustomFormResponse res = response;
                String newName = res.asInput(0);
                BlockProt.getFoliaLib().getScheduler().runAtLocation(block.getLocation(), task -> {
                    if (!player.isOnline()) return;
                    BlockNBTHandler h = new BlockNBTHandler(block);
                    h.setName(newName != null ? newName.trim() : "");
                    show(player, block, h);
                });
            });

        BedrockBridge.sendForm(player, builder.build());
    }

    @Nullable
    private static String getOwnerName(@NotNull BlockNBTHandler handler) {
        String uuidStr = handler.getOwner();
        if (uuidStr == null || uuidStr.isEmpty()) return null;
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(uuidStr)).getName();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isWorkstation(Material material) {
        String name = material.name();
        return name.equals("GRINDSTONE") || name.equals("STONECUTTER") || name.equals("LOOM")
            || name.equals("CARTOGRAPHY_TABLE") || name.equals("SMITHING_TABLE")
            || name.equals("ENCHANTING_TABLE") || name.equals("FLETCHING_TABLE")
            || name.equals("LECTERN") || name.equals("COMPOSTER") || name.equals("BREWING_STAND")
            || name.equals("BLAST_FURNACE") || name.equals("SMOKER") || name.equals("BARREL")
            || name.equals("CAULDRON");
    }
}
