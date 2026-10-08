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

import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.FriendHandler;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_CORAL;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_GOLD;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_MINT;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_PURPLE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_GRAY;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;

/**
 * Dialog for configuring an individual friend's permission tier and custom flags.
 */
public final class FriendDetailDialog {

    private FriendDetailDialog() {}

    public static void showForBlock(@NotNull Player player, @NotNull Block block,
                                    @NotNull BlockNBTHandler handler, @NotNull String friendUuid, int page) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        Optional<FriendHandler> opt = handler.getFriend(friendUuid);
        if (opt.isEmpty()) {
            FriendManageDialog.showForBlock(player, block, handler, page);
            return;
        }

        FriendHandler friend = opt.get();
        String friendName = getPlayerName(friendUuid);
        String displayName = friendName != null ? friendName : friendUuid;
        boolean colorblind = new PlayerSettingsHandler(player).getColorblindMode();

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__EDIT)),
            PASTEL_PURPLE, TextDecoration.BOLD
        );

        int currentLevel = friend.getLevel();
        String levelName = getLevelName(currentLevel);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(displayName, NamedTextColor.WHITE, TextDecoration.BOLD)));
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__LEVEL_LABEL)).replace("{level}", levelName),
            PASTEL_GOLD
        )));

        List<DialogButton> buttons = new ArrayList<>();

        buttons.add(new DialogButton("level_1",
            levelButtonText(stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__LEVEL_BASIC)), currentLevel == FriendHandler.LEVEL_BASIC, colorblind),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__LEVEL_BASIC_DESC)), TextColor.color(0x888888)),
            p -> {
                friend.setLevel(FriendHandler.LEVEL_BASIC);
                handler.notifyFriendsMutated();
                handler.applyToOtherContainer();
                showForBlock(p, block, handler, friendUuid, page);
            }
        ));

        buttons.add(new DialogButton("level_2",
            levelButtonText(stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__LEVEL_OPERATOR)), currentLevel == FriendHandler.LEVEL_OPERATOR, colorblind),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__LEVEL_OPERATOR_DESC)), TextColor.color(0x888888)),
            p -> {
                friend.setLevel(FriendHandler.LEVEL_OPERATOR);
                handler.notifyFriendsMutated();
                handler.applyToOtherContainer();
                showForBlock(p, block, handler, friendUuid, page);
            }
        ));

        buttons.add(new DialogButton("level_3",
            levelButtonText(stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__LEVEL_MANAGER)), currentLevel == FriendHandler.LEVEL_MANAGER, colorblind),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__LEVEL_MANAGER_DESC)), TextColor.color(0x888888)),
            p -> {
                friend.setLevel(FriendHandler.LEVEL_MANAGER);
                handler.notifyFriendsMutated();
                handler.applyToOtherContainer();
                showForBlock(p, block, handler, friendUuid, page);
            }
        ));

        buttons.add(new DialogButton("level_4",
            levelButtonText(stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__LEVEL_CUSTOM)), currentLevel == FriendHandler.LEVEL_CUSTOM, colorblind),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__LEVEL_CUSTOM_DESC)), TextColor.color(0x888888)),
            p -> {
                friend.setLevel(FriendHandler.LEVEL_CUSTOM);
                handler.notifyFriendsMutated();
                handler.applyToOtherContainer();
                showForBlock(p, block, handler, friendUuid, page);
            }
        ));

        if (currentLevel == FriendHandler.LEVEL_CUSTOM) {
            addCustomFlagButton(buttons, friend, FriendHandler.FLAG_OPEN_MENU,
                stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__FLAG_OPEN_MENU)),
                colorblind, () -> {
                    handler.notifyFriendsMutated();
                    handler.applyToOtherContainer();
                    showForBlock(player, block, handler, friendUuid, page);
                });

            addCustomFlagButton(buttons, friend, FriendHandler.FLAG_EDIT_SETTINGS,
                stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__FLAG_EDIT_SETTINGS)),
                colorblind, () -> {
                    handler.notifyFriendsMutated();
                    handler.applyToOtherContainer();
                    showForBlock(player, block, handler, friendUuid, page);
                });

            addCustomFlagButton(buttons, friend, FriendHandler.FLAG_EDIT_NAME,
                stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__FLAG_EDIT_NAME)),
                colorblind, () -> {
                    handler.notifyFriendsMutated();
                    handler.applyToOtherContainer();
                    showForBlock(player, block, handler, friendUuid, page);
                });

            addCustomFlagButton(buttons, friend, FriendHandler.FLAG_INSPECT,
                stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__FLAG_INSPECT)),
                colorblind, () -> {
                    handler.notifyFriendsMutated();
                    handler.applyToOtherContainer();
                    showForBlock(player, block, handler, friendUuid, page);
                });

            addCustomFlagButton(buttons, friend, FriendHandler.FLAG_VIEW_AUDIT,
                stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__FLAG_VIEW_AUDIT)),
                colorblind, () -> {
                    handler.notifyFriendsMutated();
                    handler.applyToOtherContainer();
                    showForBlock(player, block, handler, friendUuid, page);
                });

            addCustomFlagButton(buttons, friend, FriendHandler.FLAG_MANAGE_FRIENDS,
                stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__FLAG_MANAGE_FRIENDS)),
                colorblind, () -> {
                    handler.notifyFriendsMutated();
                    handler.applyToOtherContainer();
                    showForBlock(player, block, handler, friendUuid, page);
                });

            addCustomFlagButton(buttons, friend, FriendHandler.FLAG_OPEN_CONTAINER,
                stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__FLAG_OPEN_CONTAINER)),
                colorblind, () -> {
                    handler.notifyFriendsMutated();
                    handler.applyToOtherContainer();
                    showForBlock(player, block, handler, friendUuid, page);
                });

            addCustomFlagButton(buttons, friend, FriendHandler.FLAG_DEPOSIT,
                stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__FLAG_DEPOSIT)),
                colorblind, () -> {
                    handler.notifyFriendsMutated();
                    handler.applyToOtherContainer();
                    showForBlock(player, block, handler, friendUuid, page);
                });

            addCustomFlagButton(buttons, friend, FriendHandler.FLAG_WITHDRAW,
                stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__FLAG_WITHDRAW)),
                colorblind, () -> {
                    handler.notifyFriendsMutated();
                    handler.applyToOtherContainer();
                    showForBlock(player, block, handler, friendUuid, page);
                });

            addCustomFlagButton(buttons, friend, FriendHandler.FLAG_REDSTONE,
                stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__FLAG_REDSTONE)),
                colorblind, () -> {
                    handler.notifyFriendsMutated();
                    handler.applyToOtherContainer();
                    showForBlock(player, block, handler, friendUuid, page);
                });
        }

        buttons.add(new DialogButton("remove_friend",
            Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__REMOVE)), PASTEL_CORAL),
            Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__REMOVE)), TextColor.color(0x888888)),
            p -> {
                handler.removeFriend(friendUuid);
                handler.applyToOtherContainer();
                FriendManageDialog.showForBlock(p, block, handler, page);
            }
        ));

        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            p -> FriendManageDialog.showForBlock(p, block, handler, page)
        );

        bridge.showMultiAction(player, title, body, buttons, backBtn, 2);
    }

    private static void addCustomFlagButton(List<DialogButton> buttons, FriendHandler friend,
                                            int flag, String label, boolean colorblind, Runnable onToggle) {
        boolean active = friend.hasFlag(flag);
        TextColor color = active ? PASTEL_MINT : PASTEL_CORAL;
        String icon = BpDialogStyles.indicatorIcon(active, colorblind);

        buttons.add(new DialogButton("flag_" + flag,
            Component.text()
                .append(Component.text(icon, color))
                .append(Component.text(label, NamedTextColor.WHITE))
                .build(),
            Component.text(active
                ? stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_DISABLE_SINGLE))
                : stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_ENABLE_SINGLE)), TextColor.color(0x888888)),
            p -> {
                friend.setFlag(flag, !active);
                onToggle.run();
            }
        ));
    }

    private static Component levelButtonText(String name, boolean active, boolean colorblind) {
        TextColor color = active ? PASTEL_MINT : SOFT_GRAY;
        String icon = BpDialogStyles.indicatorIcon(active, colorblind);
        return Component.text()
            .append(Component.text(icon, color))
            .append(Component.text(name, active ? NamedTextColor.WHITE : SOFT_GRAY))
            .build();
    }

    public static String getLevelName(int level) {
        return switch (level) {
            case FriendHandler.LEVEL_BASIC -> stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__LEVEL_BASIC));
            case FriendHandler.LEVEL_OPERATOR -> stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__LEVEL_OPERATOR));
            case FriendHandler.LEVEL_MANAGER -> stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__LEVEL_MANAGER));
            case FriendHandler.LEVEL_CUSTOM -> stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__LEVEL_CUSTOM));
            default -> stripColor(Translator.get(TranslationKey.DIALOGS__FRIENDS__LEVEL_LABEL)).replace("{level}", String.valueOf(level));
        };
    }

    private static String getPlayerName(String uuidStr) {
        try {
            OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
            if (op.getName() != null) return op.getName();
        } catch (IllegalArgumentException ignored) {}
        return null;
    }
}
