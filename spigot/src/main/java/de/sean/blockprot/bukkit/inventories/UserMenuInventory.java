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
import de.sean.blockprot.bukkit.commands.AboutCommand;
import de.sean.blockprot.bukkit.commands.TransferCommand;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UserMenuInventory extends BlockProtInventory {

    private static final int SLOT_FRIENDS    = 11;
    private static final int SLOT_SETTINGS   = 12;
    private static final int SLOT_STATS      = 13;
    private static final int SLOT_TRANSFER   = 14;
    private static final int SLOT_ABOUT      = 15;
    private static final int SLOT_BACK       = 22;

    private static final int[] SEPARATOR_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8,
        9, 10, 16, 17,
        18, 19, 20, 21, 23, 24, 25, 26
    };

    public UserMenuInventory() { super(false); }

    @Override int getSize() { return InventoryConstants.tripleLine; }

    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__USER_MENU__TITLE);
    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        inventory = createInventory();
        fillSeparators();

        if (!BlockProt.getDefaultConfig().isFriendFunctionalityDisabled()) {
            setPlayerSkullAsync(SLOT_FRIENDS, player, player.getUniqueId(), player.getName(),
                Translator.get(TranslationKey.INVENTORIES__USER_MENU__FRIENDS),
                Translator.get(TranslationKey.INVENTORIES__USER_MENU__FRIENDS_LORE));
        }
        inventory.setItem(SLOT_SETTINGS, item(Material.WRITABLE_BOOK,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__SETTINGS),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__SETTINGS_LORE)));
        inventory.setItem(SLOT_STATS, item(Material.BOOK,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__PLACEMENTS),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__PLACEMENTS_LORE)));
        inventory.setItem(SLOT_TRANSFER, item(Material.HOPPER,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__TRANSFER),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__TRANSFER_LORE)));
        inventory.setItem(SLOT_ABOUT, item(Material.NETHER_STAR,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__ABOUT),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__ABOUT_LORE)));

        InventoryState state = InventoryState.get(player.getUniqueId());
        if (state != null && state.origin != InventoryState.MenuOrigin.NONE) {
            setBackButton(SLOT_BACK);
        } else {
            inventory.setItem(SLOT_BACK, item(Material.BARRIER,
                Translator.get(TranslationKey.INVENTORIES__USER_MENU__CLOSE),
                Translator.get(TranslationKey.INVENTORIES__USER_MENU__CLOSE_LORE)));
        }

        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= getSize()) return;

        if (slot == SLOT_SETTINGS) {
            InventoryState newState = new InventoryState(null);
            newState.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            newState.origin = InventoryState.MenuOrigin.USER_MENU;
            newState.originStack.push(InventoryState.MenuOrigin.USER_MENU);
            InventoryState.set(player.getUniqueId(), newState);
            player.openInventory(new UserSettingsInventory().fill(player));
            new PlayerSettingsHandler(player).setHasPlayerInteractedWithMenu(true);
        } else if (slot == SLOT_FRIENDS && !BlockProt.getDefaultConfig().isFriendFunctionalityDisabled()) {
            InventoryState newState = new InventoryState(null);
            newState.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            newState.origin = InventoryState.MenuOrigin.USER_MENU;
            newState.originStack.push(InventoryState.MenuOrigin.USER_MENU);
            InventoryState.set(player.getUniqueId(), newState);
            var inv = new FriendManageInventory().fill(player);
            if (inv != null) player.openInventory(inv);
        } else if (slot == SLOT_STATS) {
            InventoryState newState = new InventoryState(null);
            newState.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            newState.origin = InventoryState.MenuOrigin.USER_MENU;
            newState.originStack.push(InventoryState.MenuOrigin.USER_MENU);
            InventoryState.set(player.getUniqueId(), newState);
            player.openInventory(new StatisticsInventory().fill(player));
        } else if (slot == SLOT_TRANSFER) {
            player.closeInventory();
            Consumer<String> handleName = name -> {
                if (name == null || name.isBlank()) return;
                TransferCommand.transferAll(player, name);
            };
            TextInput.open(player, BlockProt.getInstance(), handleName);
        } else if (slot == SLOT_ABOUT) {
            player.closeInventory();
            AboutCommand.showAbout(player);
        } else if (slot == SLOT_BACK) {
            goBack(player, state);
        }
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    private void fillSeparators() {
        ItemStack sep = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = sep.getItemMeta();
        if (meta != null) {
            ComponentMessages.displayName(meta, net.kyori.adventure.text.Component.text(""));
            sep.setItemMeta(meta);
        }
        for (int s : SEPARATOR_SLOTS) {
            inventory.setItem(s, sep);
        }
    }

    private ItemStack item(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        ComponentMessages.displayName(meta, net.kyori.adventure.text.Component.text(
            name.replaceAll("[§&][0-9a-fk-orx]", "")));
        List<net.kyori.adventure.text.Component> l = new ArrayList<>();
        for (String s : lore) l.add(LegacyComponentSerializer.legacySection().deserialize(s));
        ComponentMessages.lore(meta, l);
        item.setItemMeta(meta);
        return item;
    }
}
