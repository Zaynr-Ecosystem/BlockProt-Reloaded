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
import de.sean.blockprot.bukkit.dialogs.AutoDropDialog;
import de.sean.blockprot.bukkit.dialogs.AutoDropSearchDialog;
import de.sean.blockprot.bukkit.dialogs.DialogButton;
import de.sean.blockprot.bukkit.dialogs.DialogOrigin;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Results of an auto-drop block search: paged 54-slot list of matching
 * materials in relevance order, each toggling auto_drop_to_inventory.
 * Also hosts the shared chat-prompt entry points used by both the
 * inventory and the dialog auto-drop screens.
 */
public final class AutoDropSearchInventory extends BlockProtInventory {

    private static final TextColor PASTEL_CORAL = TextColor.color(0xFF6B6B);
    private static final TextColor PASTEL_MINT  = TextColor.color(0x98FF98);

    private static final int CONTENT_SLOTS = 45;
    private static final int SLOT_PREV = 48;
    private static final int SLOT_NEXT = 50;
    private static final int SLOT_BACK = 53;

    private String currentQuery = "";
    private List<Material> pagedList = List.of();
    private int cachedPage = 0;

    public AutoDropSearchInventory() { super(false); }

    @Override int getSize() { return InventoryConstants.sextupletLine; }

    @Override
    String getTranslatedInventoryName() { return Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__SEARCH_TITLE); }

    @NotNull
    public Inventory fill(@NotNull Player player, @NotNull String query, int page) {
        this.currentQuery = query;
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        pagedList = BlockFamilyParser.searchMaterials(query);
        int totalPages = Math.max(1, (int) Math.ceil(pagedList.size() / (double) CONTENT_SLOTS));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        cachedPage = safePage;
        int start = safePage * CONTENT_SLOTS;
        int end = Math.min(start + CONTENT_SLOTS, pagedList.size());

        String title = Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__SEARCH_TITLE)
            .replace("{query}", query);
        inventory = createInventory(title);

        for (int i = start; i < end; i++) {
            Material mat = pagedList.get(i);
            inventory.setItem(i - start, blockItem(mat, cfg.getAutoDropToInventoryBlocks().contains(mat)));
        }

        if (safePage > 0) setItemStack(SLOT_PREV, Material.ARROW, Translator.get(TranslationKey.INVENTORIES__LAST_PAGE));
        if (safePage < totalPages - 1) setItemStack(SLOT_NEXT, Material.ARROW, Translator.get(TranslationKey.INVENTORIES__NEXT_PAGE));

        InventoryState state = InventoryState.get(player.getUniqueId());
        boolean hasParent = state != null && (state.origin != InventoryState.MenuOrigin.NONE || !state.originStack.isEmpty());
        if (hasParent) {
            ItemStack backStack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta backMeta = backStack.getItemMeta();
            if (backMeta != null) {
                ComponentMessages.displayName(backMeta, Component.text(Translator.get(TranslationKey.INVENTORIES__BACK)).color(PASTEL_CORAL));
                backStack.setItemMeta(backMeta);
            }
            inventory.setItem(SLOT_BACK, backStack);
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

        if (slot == SLOT_PREV && cachedPage > 0) {
            player.openInventory(fill(player, currentQuery, cachedPage - 1));
            return;
        }

        int totalPages = Math.max(1, (int) Math.ceil(pagedList.size() / (double) CONTENT_SLOTS));
        if (slot == SLOT_NEXT && cachedPage < totalPages - 1) {
            player.openInventory(fill(player, currentQuery, cachedPage + 1));
            return;
        }

        if (slot < CONTENT_SLOTS) {
            int absIdx = cachedPage * CONTENT_SLOTS + slot;
            if (absIdx < pagedList.size()) {
                BlockProt.getDefaultConfig().toggleAutoDropMaterial(pagedList.get(absIdx), player);
                player.openInventory(fill(player, currentQuery, cachedPage));
            }
        }
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    /**
     * Opens the chat prompt for a new block search, used by the inventory
     * entry point. The confirm callback reopens the originating screen when
     * there are no results and presents the result list otherwise.
     */
    public static void startSearchFromInventory(@NotNull Player player) {
        TextInput.open(player, BlockProt.getInstance(),
            Translator.get(TranslationKey.DIALOGS__AUTO_DROP__SEARCH_PROMPT),
            text -> finishSearch(player, null, null, text, false));
    }

    /**
     * Dialog entry point: closes the current dialog, prompts in chat, then
     * shows the result dialog (or a notice when nothing matched), keeping
     * {@code backOrigin} / {@code parentBack} for the return path.
     */
    public static void startSearchFromDialog(@NotNull Player player,
                                             @NotNull DialogOrigin backOrigin,
                                             @Nullable DialogButton.DialogClickHandler parentBack) {
        TextInput.open(player, BlockProt.getInstance(),
            Translator.get(TranslationKey.DIALOGS__AUTO_DROP__SEARCH_PROMPT),
            text -> finishSearch(player, backOrigin, parentBack, text, true));
    }

    private static void finishSearch(@NotNull Player player,
                                     @Nullable DialogOrigin backOrigin,
                                     @Nullable DialogButton.DialogClickHandler parentBack,
                                     @Nullable String text, boolean dialogMode) {
        if (text == null || text.isBlank()) return;
        List<Material> results = BlockFamilyParser.searchMaterials(text);
        if (results.isEmpty()) {
            String message = Translator.get(TranslationKey.MESSAGES__AUTO_DROP__SEARCH_NO_RESULTS)
                .replace("{query}", text);
            ComponentMessages.sendActionBar(player, LegacyComponentSerializer.legacySection().deserialize(message));
            if (dialogMode && backOrigin != null) {
                AutoDropDialog.show(player, backOrigin, parentBack);
            } else {
                player.openInventory(new AutoDropInventory().fill(player));
            }
            return;
        }
        if (dialogMode && backOrigin != null) {
            AutoDropSearchDialog.show(player, backOrigin, parentBack, text, 0);
        } else {
            player.openInventory(new AutoDropSearchInventory().fill(player, text, 0));
        }
    }

    @NotNull
    private static ItemStack blockItem(@NotNull Material mat, boolean active) {
        Material display = resolveDisplayMaterial(mat);
        ItemStack stack = new ItemStack(display, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            NamedTextColor nameColor = active ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY;
            ComponentMessages.displayName(meta, Component.text(friendlyName(mat.name())).color(nameColor));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(mat.name()).color(NamedTextColor.DARK_GRAY));
            lore.add(Component.text(active
                ? Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__STATUS_ACTIVE)
                : Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__STATUS_INACTIVE))
                .color(active ? PASTEL_MINT : PASTEL_CORAL));
            ComponentMessages.lore(meta, lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @NotNull
    private static Material resolveDisplayMaterial(@NotNull Material mat) {
        String name = mat.name();
        if (name.equals("WATER_CAULDRON") || name.equals("LAVA_CAULDRON")
                || name.equals("POWDER_SNOW_CAULDRON")) {
            return Material.CAULDRON;
        }
        if (name.endsWith("_WALL_SIGN") && !name.endsWith("_HANGING_SIGN")) {
            Material m = Material.matchMaterial(name.replace("_WALL_SIGN", "_SIGN"));
            return m != null ? m : Material.OAK_SIGN;
        }
        if (name.endsWith("_WALL_HANGING_SIGN")) {
            Material m = Material.matchMaterial(name.replace("_WALL_HANGING_SIGN", "_HANGING_SIGN"));
            return m != null ? m : Material.OAK_HANGING_SIGN;
        }
        return mat;
    }

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