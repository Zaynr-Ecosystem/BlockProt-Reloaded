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

package de.sean.blockprot.bukkit.inventories;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.bukkit.listeners.BlockEventListener;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Single-row inventory with user settings.
 *
 * Layout (9 slots, 0-8):
 *   0 = Lock on place   (BARRIER)
 *   1 = Hints toggle    (KNOWLEDGE_BOOK)
 *   3 = Notifications   (BELL)
 *   4 = Friends skull   (PLAYER_HEAD)
 *   8 = Back
 */
public class UserSettingsInventory extends BlockProtInventory {

    public UserSettingsInventory() { super(true); }

    private static final int SLOT_LOCK_ON_PLACE   = 0;
    private static final int SLOT_HINTS           = 1;
    private static final int SLOT_PREFER_DIALOGS  = 2;
    private static final int SLOT_NOTIFICATIONS   = 3;
    private static final int SLOT_COLORBLIND      = 4;
    private static final int SLOT_FRIENDS         = 5;

    @Override
    int getSize() { return InventoryConstants.lineLength; }

    @NotNull
    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__USER_SETTINGS);
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null) return;

        switch (item.getType()) {
            case BARRIER -> {
                PlayerSettingsHandler h = new PlayerSettingsHandler(player);
                boolean next = !h.getLockOnPlace();
                h.setLockOnPlace(next);
                BlockEventListener.invalidateSettings(player.getUniqueId());
                setEnchantedOptionItemStack(
                    SLOT_LOCK_ON_PLACE,
                    Material.BARRIER,
                    TranslationKey.INVENTORIES__LOCK_ON_PLACE,
                    next
                );
                player.updateInventory();
            }
            case KNOWLEDGE_BOOK -> {
                PlayerSettingsHandler h = new PlayerSettingsHandler(player);
                boolean hintsCurrentlyEnabled = !h.hasPlayerInteractedWithMenu();
                h.setHasPlayerInteractedWithMenu(hintsCurrentlyEnabled);
                fill(player);
                player.updateInventory();
            }
            case PAPER -> {
                if (de.sean.blockprot.bukkit.bedrock.BedrockBridge.isBedrockPlayer(player)) {
                    PlayerSettingsHandler h = new PlayerSettingsHandler(player);
                    boolean nextState = !h.getPreferBedrockForms();
                    h.setPreferBedrockForms(nextState);
                    if (nextState) {
                        switchToExternalUi(player, state,
                            () -> de.sean.blockprot.bukkit.bedrock.forms.BedrockUserSettingsForm.show(player));
                    } else {
                        fill(player);
                        player.updateInventory();
                    }
                } else if (BlockProt.getDefaultConfig().isDialogsEnabled()) {
                    PlayerSettingsHandler h = new PlayerSettingsHandler(player);
                    boolean nextState = !h.getPreferDialogs();
                    h.setPreferDialogs(nextState);
                    if (nextState) {
                        de.sean.blockprot.bukkit.dialogs.DialogOrigin dOrigin =
                            hasUserMenuOrigin(state)
                                ? de.sean.blockprot.bukkit.dialogs.DialogOrigin.USER_MENU
                                : de.sean.blockprot.bukkit.dialogs.DialogOrigin.NONE;
                        switchToExternalUi(player, state,
                            () -> de.sean.blockprot.bukkit.dialogs.UserSettingsDialog.show(player, dOrigin));
                    } else {
                        fill(player);
                        player.updateInventory();
                    }
                }
            }
            case BELL -> {
                PlayerSettingsHandler h = new PlayerSettingsHandler(player);
                boolean next = !h.getNotificationsEnabled();
                h.setNotificationsEnabled(next);
                setEnchantedOptionItemStack(
                    SLOT_NOTIFICATIONS,
                    Material.BELL,
                    TranslationKey.INVENTORIES__USER_SETTINGS_NOTIFICATIONS,
                    next
                );
                player.updateInventory();
            }
            case SPYGLASS -> {
                PlayerSettingsHandler h = new PlayerSettingsHandler(player);
                boolean next = !h.getColorblindMode();
                h.setColorblindMode(next);
                setEnchantedOptionItemStack(
                    SLOT_COLORBLIND,
                    Material.SPYGLASS,
                    TranslationKey.INVENTORIES__USER_SETTINGS_COLORBLIND,
                    next
                );
                player.updateInventory();
            }
            case PLAYER_HEAD -> {
                state.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
                state.origin = InventoryState.MenuOrigin.USER_SETTINGS;
                state.originStack.push(InventoryState.MenuOrigin.USER_SETTINGS);
                closeAndOpen(player, new FriendManageInventory().fill(player));
            }
            case BLACK_STAINED_GLASS_PANE -> goBack(player, state);
            default -> closeAndOpen(player, null);
        }
        event.setCancelled(true);
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    private static boolean hasUserMenuOrigin(@NotNull InventoryState state) {
        return state.origin == InventoryState.MenuOrigin.USER_MENU
            || state.originStack.contains(InventoryState.MenuOrigin.USER_MENU);
    }

    public Inventory fill(Player player) {
        PlayerSettingsHandler settings = new PlayerSettingsHandler(player);

        setEnchantedOptionItemStack(
            SLOT_LOCK_ON_PLACE,
            Material.BARRIER,
            TranslationKey.INVENTORIES__LOCK_ON_PLACE,
            settings.getLockOnPlace()
        );

        boolean hintsEnabled = !settings.hasPlayerInteractedWithMenu();
        setEnchantedOptionItemStack(
            SLOT_HINTS,
            Material.KNOWLEDGE_BOOK,
            TranslationKey.INVENTORIES__USER_MENU__HINTS,
            hintsEnabled
        );

        if (de.sean.blockprot.bukkit.bedrock.BedrockBridge.isBedrockPlayer(player)) {
            setEnchantedOptionItemStack(
                SLOT_PREFER_DIALOGS,
                Material.PAPER,
                TranslationKey.DIALOGS__SETTINGS__PREFER_BEDROCK_FORMS,
                settings.getPreferBedrockForms()
            );
        } else if (BlockProt.getDefaultConfig().isDialogsEnabled()) {
            setEnchantedOptionItemStack(
                SLOT_PREFER_DIALOGS,
                Material.PAPER,
                TranslationKey.DIALOGS__SETTINGS__PREFER_DIALOGS,
                settings.getPreferDialogs()
            );
        }

        setEnchantedOptionItemStack(
            SLOT_NOTIFICATIONS,
            Material.BELL,
            TranslationKey.INVENTORIES__USER_SETTINGS_NOTIFICATIONS,
            settings.getNotificationsEnabled()
        );

        setEnchantedOptionItemStack(
            SLOT_COLORBLIND,
            Material.SPYGLASS,
            TranslationKey.INVENTORIES__USER_SETTINGS_COLORBLIND,
            settings.getColorblindMode()
        );

        if (!BlockProt.getDefaultConfig().isFriendFunctionalityDisabled()) {
            setPlayerSkullAsync(SLOT_FRIENDS, player, player.getUniqueId(), player.getName(),
                Translator.get(TranslationKey.INVENTORIES__FRIENDS__MANAGE));
        }

        InventoryState st = InventoryState.get(player.getUniqueId());
        if (st != null && (!st.originStack.isEmpty()
                || (st.origin != InventoryState.MenuOrigin.NONE
                    && st.origin != InventoryState.MenuOrigin.USER_SETTINGS))) {
            setBackButton();
        } else {
            setItemStack(getSize() - 1, Material.BLACK_STAINED_GLASS_PANE,
                TranslationKey.INVENTORIES__ADMIN_MENU__CLOSE);
        }
        return inventory;
    }
}