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
import de.sean.blockprot.bukkit.nbt.FriendHandler;
import de.sean.blockprot.bukkit.nbt.FriendSupportingHandler;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import de.sean.blockprot.bukkit.util.PlayerLookup;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.cumulus.util.FormImage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;

/**
 * Native Bedrock friend management forms with verified textures.
 */
public final class BedrockFriendManageForm {

    private BedrockFriendManageForm() {}

    private static void runForHandler(@NotNull Player player, @NotNull FriendSupportingHandler<?> handler, @NotNull Runnable action) {
        if (handler instanceof BlockNBTHandler bnh) {
            BlockProt.getFoliaLib().getScheduler().runAtLocation(bnh.block.getLocation(), task -> {
                if (player.isOnline()) action.run();
            });
        } else {
            BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                if (player.isOnline()) action.run();
            });
        }
    }

    public static void show(@NotNull Player player) {
        BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> {
            if (player.isOnline()) {
                show(player, new PlayerSettingsHandler(player), null);
            }
        });
    }

    public static void show(@NotNull Player player, @NotNull FriendSupportingHandler<?> handler, @Nullable Runnable onBack) {
        if (handler instanceof BlockNBTHandler bnh) {
            if (!org.bukkit.Bukkit.isPrimaryThread() && (!BlockProt.hasFoliaLib() || !BlockProt.getFoliaLib().getScheduler().isOwnedByCurrentRegion(bnh.block))) {
                BlockProt.getFoliaLib().getScheduler().runAtLocation(bnh.block.getLocation(), task -> {
                    if (player.isOnline()) show(player, new BlockNBTHandler(bnh.block), onBack);
                });
                return;
            }
        } else if (!org.bukkit.Bukkit.isPrimaryThread() && (!BlockProt.hasFoliaLib() || !BlockProt.getFoliaLib().getScheduler().isOwnedByCurrentRegion(player))) {
            BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                if (player.isOnline()) show(player, handler, onBack);
            });
            return;
        }
        String title = stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__MANAGE));
        String addFriend = stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__SEARCH));
        String back = stripColor(Translator.get(TranslationKey.DIALOGS__BACK));

        List<FriendHandler> friends = handler.getFriends();

        SimpleForm.Builder builder = SimpleForm.builder()
            .title(title)
            .content(stripColor(Translator.get(TranslationKey.DIALOGS__CHOOSE_OPTION)))
            .button(addFriend, FormImage.Type.PATH, "textures/ui/magnifyingGlass");

        for (FriendHandler fh : friends) {
            String uuidStr = fh.getName();
            String name = resolvePlayerName(uuidStr);
            builder.button(name, FormImage.Type.PATH, "textures/ui/FriendsIcon");
        }

        builder.button(back, FormImage.Type.PATH, "textures/ui/back_button_default");

        builder.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            runForHandler(player, handler, () -> {
                if (clicked == 0) {
                    showAddFriendPrompt(player, handler, () -> show(player, handler, onBack));
                } else if (clicked <= friends.size()) {
                    FriendHandler selected = friends.get(clicked - 1);
                    showFriendActionPrompt(player, handler, selected, () -> show(player, handler, onBack));
                } else {
                    if (onBack != null) onBack.run();
                    else BedrockUserMenuForm.show(player);
                }
            });
        });

        BedrockBridge.sendForm(player, builder.build());
    }

    private static void showAddFriendPrompt(@NotNull Player player, @NotNull FriendSupportingHandler<?> handler, @NotNull Runnable returnBack) {
        String title = stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__SEARCH));
        String prompt = stripColor(Translator.get(TranslationKey.MESSAGES__CHAT_INPUT_PROMPT));

        CustomForm.Builder builder = CustomForm.builder()
            .title(title)
            .input(prompt, "", "")
            .validResultHandler(response -> {
                CustomFormResponse res = response;
                String input = res.asInput(0);
                if (input == null || input.isBlank()) {
                    runForHandler(player, handler, returnBack);
                    return;
                }

                BlockProt.getFoliaLib().getScheduler().runAsync(task -> {
                    double similarity = BlockProt.getDefaultConfig().getFriendSearchSimilarityPercentage();
                    var match = PlayerLookup.findBestMatch(input.trim(), similarity, player.getUniqueId());
                    if (match == null) {
                        ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__FRIEND_PLAYER_NOT_FOUND));
                        return;
                    }

                    runForHandler(player, handler, () -> {
                        handler.addFriend(match.getKey().toString());
                        if (handler instanceof BlockNBTHandler bnh) {
                            bnh.applyToOtherContainer();
                        }
                        ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__FRIEND_ADDED).replace("{player}", match.getValue()));
                        returnBack.run();
                    });
                });
            });

        BedrockBridge.sendForm(player, builder.build());
    }

    private static void showFriendActionPrompt(@NotNull Player player, @NotNull FriendSupportingHandler<?> handler, @NotNull FriendHandler friendHandler, @NotNull Runnable returnBack) {
        String uuidStr = friendHandler.getName();
        String name = resolvePlayerName(uuidStr);

        String title = name;
        int currentLevel = friendHandler.getLevel();
        String levelName = de.sean.blockprot.bukkit.dialogs.FriendDetailDialog.getLevelName(currentLevel);
        String remove = stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__REMOVE));
        String back = stripColor(Translator.get(TranslationKey.DIALOGS__BACK));

        SimpleForm.Builder builder = SimpleForm.builder()
            .title(title)
            .content(stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__LEVEL_LABEL)).replace("{level}", levelName))
            .button((currentLevel == FriendHandler.LEVEL_BASIC ? "[*] " : "") + stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__LEVEL_BASIC)), FormImage.Type.PATH, "textures/ui/icon_recipe_item")
            .button((currentLevel == FriendHandler.LEVEL_OPERATOR ? "[*] " : "") + stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__LEVEL_OPERATOR)), FormImage.Type.PATH, "textures/ui/op")
            .button((currentLevel == FriendHandler.LEVEL_MANAGER ? "[*] " : "") + stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__LEVEL_MANAGER)), FormImage.Type.PATH, "textures/ui/icon_star")
            .button((currentLevel == FriendHandler.LEVEL_CUSTOM ? "[*] " : "") + stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__LEVEL_CUSTOM)), FormImage.Type.PATH, "textures/ui/automation_glyph_color")
            .button(remove, FormImage.Type.PATH, "textures/ui/trash_default")
            .button(back, FormImage.Type.PATH, "textures/ui/back_button_default")
            .validResultHandler(response -> {
                int clicked = response.clickedButtonId();
                runForHandler(player, handler, () -> {
                    if (clicked >= 0 && clicked <= 3) {
                        int newLevel = clicked + 1;
                        friendHandler.setLevel(newLevel);
                        handler.notifyFriendsMutated();
                        if (handler instanceof BlockNBTHandler bnh) {
                            bnh.applyToOtherContainer();
                        }
                        showFriendActionPrompt(player, handler, friendHandler, returnBack);
                    } else if (clicked == 4) {
                        handler.removeFriend(uuidStr);
                        if (handler instanceof BlockNBTHandler bnh) {
                            bnh.applyToOtherContainer();
                        }
                        ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__FRIEND_REMOVED).replace("{player}", name));
                        returnBack.run();
                    } else {
                        returnBack.run();
                    }
                });
            });

        BedrockBridge.sendForm(player, builder.build());
    }

    private static String resolvePlayerName(String uuidStr) {
        try {
            UUID uuid = UUID.fromString(uuidStr);
            var offline = org.bukkit.Bukkit.getOfflinePlayer(uuid);
            if (offline.getName() != null) return offline.getName();
        } catch (IllegalArgumentException ignored) {}
        return uuidStr.length() > 8 ? uuidStr.substring(0, 8) : uuidStr;
    }
}
