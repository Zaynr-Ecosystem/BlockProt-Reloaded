/*
 * Copyright (C) 2021 - 2026 spnda
 * Modifications Copyright (C) 2025 - 2026 Zaynr (Zar)
 * This file is part of BlockProt Reloaded <https://github.com/VictorGugug/BlockProt-Reloaded>.
 * Based on BlockProt <https://github.com/spnda/BlockProt>.
 *
 * BlockProt Reloaded is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BlockProt Reloaded is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlockProt Reloaded. If not, see <https://www.gnu.org/licenses/>.
 */

package de.sean.blockprot.bukkit.inventories;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public final class AutoDropInventory extends BlockProtInventory {

    private static final TextColor PASTEL_CORAL  = TextColor.color(0xFF6B6B);
    private static final TextColor PASTEL_MINT   = TextColor.color(0x98FF98);
    private static final TextColor PASTEL_GOLD   = TextColor.color(0xF0E6A0);
    private static final TextColor PASTEL_YELLOW = TextColor.color(0xF0E6A0);

    private record FamilyEntry(BlockFamilyParser.Family family, Material icon, int slot) {}

    private static final int SLOT_SEARCH = 4;
    private static final int SLOT_BACK = 26;

    private static final List<FamilyEntry> FAMILIES = List.of(
        new FamilyEntry(BlockFamilyParser.Family.TILE_ENTITIES, Material.CHEST, 10),
        new FamilyEntry(BlockFamilyParser.Family.SHULKER_BOXES, Material.SHULKER_BOX, 12),
        new FamilyEntry(BlockFamilyParser.Family.BLOCKS, Material.ANVIL, 14),
        new FamilyEntry(BlockFamilyParser.Family.DOORS, Material.OAK_DOOR, 16),
        new FamilyEntry(BlockFamilyParser.Family.ENTITIES, Material.ITEM_FRAME, 18)
    );

    public AutoDropInventory() { super(false); }

    @Override int getSize() { return InventoryConstants.tripleLine; }

    @Override
    String getTranslatedInventoryName() { return Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__TITLE); }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        inventory = createInventory();
        DefaultConfig cfg = BlockProt.getDefaultConfig();
        Set<Material> autoDropBlocks = cfg.getAutoDropToInventoryBlocks();

        for (FamilyEntry fe : FAMILIES) {
            Set<Material> members = BlockFamilyParser.getFamilyMembers(fe.family());
            long active = members.stream().filter(autoDropBlocks::contains).count();
            long total = members.size();
            String label = friendlyName(fe.family().name());
            boolean allActive = active == total;
            boolean noneActive = active == 0;

            ItemStack stack = new ItemStack(fe.icon());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                TextColor nameColor = allActive ? PASTEL_MINT : (noneActive ? PASTEL_CORAL : PASTEL_GOLD);
                ComponentMessages.displayName(meta, Component.text(label).color(nameColor));

                TextColor statusColor = allActive ? PASTEL_MINT : (noneActive ? PASTEL_CORAL : PASTEL_GOLD);
                String statusText = allActive
                    ? Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__STATUS_ACTIVE)
                    : (noneActive
                        ? Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__STATUS_INACTIVE)
                        : active + "/" + total);

                ComponentMessages.lore(meta, List.of(
                    Component.text(statusText).color(statusColor),
                    Component.text(Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__LEFT_CLICK_HINT)).color(PASTEL_MINT),
                    Component.text(Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__RIGHT_CLICK_HINT)).color(PASTEL_YELLOW)
                ));
                stack.setItemMeta(meta);
            }
            inventory.setItem(fe.slot(), stack);
        }

        ItemStack searchStack = new ItemStack(Material.COMPASS);
        ItemMeta searchMeta = searchStack.getItemMeta();
        if (searchMeta != null) {
            ComponentMessages.displayName(searchMeta,
                Component.text(Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__SEARCH)).color(PASTEL_YELLOW));
            ComponentMessages.lore(searchMeta, List.of(
                Component.text(Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__SEARCH_LORE)).color(PASTEL_MINT)
            ));
            searchStack.setItemMeta(searchMeta);
        }
        inventory.setItem(SLOT_SEARCH, searchStack);

        InventoryState state = InventoryState.get(player.getUniqueId());
        boolean hasParent = state != null && (state.origin != InventoryState.MenuOrigin.NONE || !state.originStack.isEmpty());
        if (hasParent) {
            setBackButton(SLOT_BACK);
        } else {
            setItemStack(SLOT_BACK, Material.BARRIER, TranslationKey.INVENTORIES__ADMIN_MENU__CLOSE);
        }
        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= getSize()) return;

        if (slot == SLOT_BACK) {
            boolean hasParent = state.origin != InventoryState.MenuOrigin.NONE || !state.originStack.isEmpty();
            if (hasParent) {
                goBack(player, state);
            } else {
                closeAndOpen(player, null);
            }
            return;
        }

        if (slot == SLOT_SEARCH) {
            AutoDropSearchInventory.startSearchFromInventory(player);
            return;
        }

        for (FamilyEntry fe : FAMILIES) {
            if (slot == fe.slot()) {
                if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
                    BlockProt.getDefaultConfig().toggleAutoDropFamily(fe.family(), player);
                    player.openInventory(fill(player));
                } else {
                    state.originStack.push(InventoryState.MenuOrigin.AUTO_DROP);
                    player.openInventory(new AutoDropFamilyInventory().fill(player, fe.family(), 0, state));
                }
                return;
            }
        }
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    @NotNull
    private static String friendlyName(@NotNull String name) {
        String[] words = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}