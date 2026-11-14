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
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class AutoDropFamilyInventory extends BlockProtInventory {

    private static final TextColor PASTEL_CORAL = TextColor.color(0xFF6B6B);
    private static final TextColor PASTEL_MINT  = TextColor.color(0x98FF98);
    private static final TextColor PASTEL_AQUA  = TextColor.color(0xA8E2E2);

    private static final int CONTENT_SLOTS = 45;
    private static final int SLOT_PREV = 48;
    private static final int SLOT_NEXT = 50;
    private static final int SLOT_BACK = 53;

    private record Entry(@Nullable Material material, boolean active, @Nullable String sectionLabel) {
        static Entry separator(String label) { return new Entry(null, false, label); }
        static Entry block(Material m, boolean active) { return new Entry(m, active, null); }
    }

    private BlockFamilyParser.Family currentFamily;
    private List<Entry> pagedList = List.of();
    private int cachedPage = 0;

    public AutoDropFamilyInventory() { super(false); }

    @Override int getSize() { return InventoryConstants.sextupletLine; }

    @Override
    String getTranslatedInventoryName() { return Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__FAMILY_TITLE); }

    @NotNull
    public Inventory fill(@NotNull Player player, @NotNull BlockFamilyParser.Family family, int page,
                          @NotNull InventoryState parentState) {
        this.currentFamily = family;
        DefaultConfig cfg = BlockProt.getDefaultConfig();
        Set<Material> autoDropBlocks = cfg.getAutoDropToInventoryBlocks();

        pagedList = buildEntries(family, autoDropBlocks);
        int totalPages = Math.max(1, (int) Math.ceil(pagedList.size() / (double) CONTENT_SLOTS));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        cachedPage = safePage;
        int start = safePage * CONTENT_SLOTS;
        int end = Math.min(start + CONTENT_SLOTS, pagedList.size());

        String title = Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__FAMILY_TITLE)
            .replace("{family}", friendlyName(family.name()));
        inventory = createInventory(title);

        for (int i = start; i < end; i++) {
            Entry e = pagedList.get(i);
            int slot = i - start;
            if (e.material() != null) {
                inventory.setItem(slot, blockItem(e.material(), e.active()));
            } else {
                inventory.setItem(slot, separatorItem(e.sectionLabel() != null ? e.sectionLabel() : ""));
            }
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
            player.openInventory(fill(player, currentFamily, cachedPage - 1, state));
            return;
        }

        int totalPages = Math.max(1, (int) Math.ceil(pagedList.size() / (double) CONTENT_SLOTS));
        if (slot == SLOT_NEXT && cachedPage < totalPages - 1) {
            player.openInventory(fill(player, currentFamily, cachedPage + 1, state));
            return;
        }

        if (slot < CONTENT_SLOTS) {
            int absIdx = cachedPage * CONTENT_SLOTS + slot;
            if (absIdx < pagedList.size()) {
                Entry e = pagedList.get(absIdx);
                if (e.material() != null) {
                    BlockProt.getDefaultConfig().toggleAutoDropMaterial(e.material(), player);
                    player.openInventory(fill(player, currentFamily, cachedPage, state));
                }
            }
        }
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    @NotNull
    private static List<Entry> buildEntries(@NotNull BlockFamilyParser.Family family,
                                            @NotNull Set<Material> autoDropBlocks) {
        Map<BlockFamilyParser.SubFamily, List<Material>> grouped = new LinkedHashMap<>();
        List<Material> ungrouped = new ArrayList<>();

        for (Material m : BlockFamilyParser.getFamilyMembers(family)) {
            BlockFamilyParser.SubFamily sf = BlockFamilyParser.subFamilyOf(m);
            if (sf != null && sf.ownerFamily == family) {
                grouped.computeIfAbsent(sf, k -> new ArrayList<>()).add(m);
            } else {
                ungrouped.add(m);
            }
        }

        for (List<Material> list : grouped.values()) {
            list.sort(Comparator.comparing(Material::name));
        }
        ungrouped.sort(Comparator.comparing(Material::name));

        List<Entry> result = new ArrayList<>();
        for (Map.Entry<BlockFamilyParser.SubFamily, List<Material>> g : grouped.entrySet()) {
            if (g.getValue().isEmpty()) continue;
            result.add(Entry.separator(friendlyName(g.getKey().tag)));
            for (Material m : g.getValue()) {
                result.add(Entry.block(m, autoDropBlocks.contains(m)));
            }
        }
        if (!ungrouped.isEmpty()) {
            result.add(Entry.separator(Translator.get(TranslationKey.INVENTORIES__LOCKABLES__CATEGORY__MISC)));
            for (Material m : ungrouped) {
                result.add(Entry.block(m, autoDropBlocks.contains(m)));
            }
        }
        if (!result.isEmpty() && result.get(0).material() == null) result.remove(0);
        return result;
    }

    @NotNull
    private static ItemStack separatorItem(@NotNull String label) {
        ItemStack sep = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = sep.getItemMeta();
        if (meta != null) {
            ComponentMessages.displayName(meta, Component.text(label).color(PASTEL_AQUA));
            ComponentMessages.lore(meta, List.of());
            sep.setItemMeta(meta);
        }
        return sep;
    }

    @NotNull
    private static ItemStack blockItem(@NotNull Material mat, boolean active) {
        Material display = resolveDisplayMaterial(mat);
        ItemStack stack;
        try {
            stack = new ItemStack(display, 1);
            if (stack.getType() == Material.AIR) stack = new ItemStack(Material.PAPER, 1);
        } catch (Exception e) {
            stack = new ItemStack(Material.PAPER, 1);
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            NamedTextColor nameColor = active ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY;
            String suffix = active ? "" : Translator.get(TranslationKey.INVENTORIES__LOCKABLES__OFF_SUFFIX);
            ComponentMessages.displayName(meta, Component.text(friendlyName(mat.name()) + suffix).color(nameColor));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(mat.name()).color(NamedTextColor.DARK_GRAY));
            String status = active
                ? Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__STATUS_ACTIVE)
                : Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__STATUS_INACTIVE);
            lore.add(Component.text(status).color(active ? PASTEL_MINT : PASTEL_CORAL));
            lore.add(Component.text(Translator.get(TranslationKey.INVENTORIES__LOCKABLES__LEFT_CLICK_HINT)).color(PASTEL_MINT));
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