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
import de.sean.blockprot.bukkit.commands.TransferCommand;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import de.sean.blockprot.bukkit.util.PlayerNameResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.jetbrains.annotations.NotNull;

import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;

/**
 * Native Bedrock forms for block ownership transfers.
 */
public final class BedrockTransferForm {

    private BedrockTransferForm() {}

    public static void showTransferAllPrompt(@NotNull Player player) {
        String title = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__TRANSFER));
        String prompt = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__TRANSFER_LORE));

        CustomForm.Builder builder = CustomForm.builder()
            .title(title)
            .input(prompt, "", "")
            .validResultHandler(response -> {
                CustomFormResponse res = response;
                String targetName = res.asInput(0);
                if (targetName != null && !targetName.isBlank()) {
                    BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                        if (player.isOnline()) {
                            TransferCommand.transferAll(player, targetName.trim());
                        }
                    });
                }
            });

        BedrockBridge.sendForm(player, builder.build());
    }

    public static void showTransferBlockPrompt(@NotNull Player player, @NotNull Block block) {
        String title = stripColor(Translator.get(TranslationKey.INVENTORIES__TRANSFER__BUTTON));
        String prompt = stripColor(Translator.get(TranslationKey.DIALOGS__CHOOSE_OPTION));

        CustomForm.Builder builder = CustomForm.builder()
            .title(title)
            .input(prompt, "", "")
            .validResultHandler(response -> {
                CustomFormResponse res = response;
                String targetName = res.asInput(0);
                if (targetName == null || targetName.isBlank()) return;

                transferBlock(player, block, targetName.trim());
            });

        BedrockBridge.sendForm(player, builder.build());
    }

    private static void transferBlock(@NotNull Player player, @NotNull Block block, @NotNull String targetName) {
        BlockProt.getFoliaLib().getScheduler().runAsync(task -> {
            OfflinePlayer target = PlayerNameResolver.findOfflinePlayer(targetName);
            if (target == null) {
                @SuppressWarnings("deprecation")
                OfflinePlayer fallback = Bukkit.getOfflinePlayer(targetName);
                if (fallback.hasPlayedBefore()) target = fallback;
            }

            if (target == null || target.getUniqueId() == null) {
                ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__TRANSFER_PLAYER_NOT_FOUND).replace("{player}", targetName));
                return;
            }

            if (target.getUniqueId().equals(player.getUniqueId())) {
                ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__TRANSFER_SELF_GUI));
                return;
            }

            final OfflinePlayer finalTarget = target;
            BlockProt.getFoliaLib().getScheduler().runAtLocation(block.getLocation(), locTask -> {
                if (!player.isOnline()) return;
                try {
                    BlockNBTHandler handler = new BlockNBTHandler(block);
                    if (!handler.isOwner(player.getUniqueId())) {
                        ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__TRANSFER_NOT_OWNER_GUI));
                        return;
                    }

                    var result = handler.transferOwner(player.getUniqueId().toString(), finalTarget.getUniqueId().toString());
                    if (result.success) {
                        String name = finalTarget.getName() != null ? finalTarget.getName() : targetName;
                        ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__TRANSFER_SUCCESS).replace("{player}", name));
                    } else {
                        ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__TRANSFER_FAILED));
                    }
                } catch (RuntimeException e) {
                    ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__TRANSFER_FAILED));
                }
            });
        });
    }
}
