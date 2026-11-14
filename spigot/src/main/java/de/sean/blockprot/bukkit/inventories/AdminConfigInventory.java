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
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

/**
 * Top-level category selector of the inventory-based admin config editor.
 * Each category screen lives in its own {@code AdminConfig*} inventory
 * class, which shares the item builders and pastel color palette defined
 * here.
 */
public final class AdminConfigInventory extends BlockProtInventory {

    static final TextColor SOFT_GRAY     = TextColor.color(0xAAAAAA);
    static final TextColor PASTEL_MINT   = TextColor.color(0x98FF98);
    static final TextColor PASTEL_CORAL  = TextColor.color(0xFF6B6B);
    static final TextColor PASTEL_GOLD   = TextColor.color(0xF0E6A0);
    static final TextColor SOFT_BLUE     = TextColor.color(0x7EC8E3);
    static final TextColor PASTEL_PURPLE = TextColor.color(0xB39DDB);

    private static final int SLOT_LANGUAGE       = 9;
    private static final int SLOT_WORLDS         = 10;
    private static final int SLOT_PLAYERS        = 11;
    private static final int SLOT_BLOCKS         = 12;
    private static final int SLOT_ENTITY         = 13;
    private static final int SLOT_EXPIRY         = 14;
    private static final int SLOT_RAID           = 15;
    private static final int SLOT_NOTIFICATIONS  = 16;
    private static final int SLOT_MAINTENANCE    = 17;
    private static final int SLOT_RELOAD         = 22;
    private static final int SLOT_BACK           = 49;

    private static final int[] SEPARATOR_SLOTS = {
        0,1,2,3,4,5,6,7,8,
        18,19,20,21, 23,24,25,26,
        27,28,29,30,31,32,33,34,35,
        36,37,38,39,40,41,42,43,44,
        45,46,47,48, 50,51,52,53
    };

    public AdminConfigInventory() { super(false); }

    @Override int getSize() { return InventoryConstants.sextupletLine; }

    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__TITLE);
    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        inventory = createInventory();
        fillSeparators();

        inventory.setItem(SLOT_LANGUAGE, categoryItem(Material.WRITABLE_BOOK,
            TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_LANGUAGE));
        inventory.setItem(SLOT_WORLDS, categoryItem(Material.GRASS_BLOCK,
            TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_WORLDS));
        setPlayerSkullAsync(SLOT_PLAYERS, player, player.getUniqueId(), player.getName(),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_PLAYERS));
        inventory.setItem(SLOT_BLOCKS, categoryItem(Material.ANVIL,
            TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_BLOCKS));
        inventory.setItem(SLOT_ENTITY, categoryItem(Material.NAME_TAG,
            TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_ENTITY));
        inventory.setItem(SLOT_EXPIRY, categoryItem(Material.CLOCK,
            TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_EXPIRY));
        inventory.setItem(SLOT_RAID, categoryItem(Material.CROSSBOW,
            TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_RAID));
        inventory.setItem(SLOT_NOTIFICATIONS, categoryItem(Material.BELL,
            TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_NOTIFICATIONS));
        inventory.setItem(SLOT_MAINTENANCE, categoryItem(Material.DAMAGED_ANVIL,
            TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_MAINTENANCE));

        inventory.setItem(SLOT_RELOAD, item(Material.COMPARATOR,
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__RELOAD),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__RELOAD_LORE)));

        setBackButton(SLOT_BACK);
        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= getSize()) return;

        if (slot == SLOT_LANGUAGE) {
            player.openInventory(new AdminConfigLanguageInventory().fill(player));
        } else if (slot == SLOT_WORLDS) {
            player.openInventory(new AdminConfigWorldsInventory().fill(player));
        } else if (slot == SLOT_PLAYERS) {
            player.openInventory(new AdminConfigPlayersInventory().fill(player));
        } else if (slot == SLOT_BLOCKS) {
            player.openInventory(new AdminConfigBlocksInventory().fill(player));
        } else if (slot == SLOT_ENTITY) {
            player.openInventory(new AdminConfigEntityInventory().fill(player));
        } else if (slot == SLOT_EXPIRY) {
            player.openInventory(new AdminConfigExpiryInventory().fill(player));
        } else if (slot == SLOT_RAID) {
            player.openInventory(new AdminConfigRaidInventory().fill(player));
        } else if (slot == SLOT_NOTIFICATIONS) {
            player.openInventory(new AdminConfigNotificationsInventory().fill(player));
        } else if (slot == SLOT_MAINTENANCE) {
            player.openInventory(new AdminConfigMaintenanceInventory().fill(player));
        } else if (slot == SLOT_RELOAD) {
            player.closeInventory();
            if (BlockProt.getDefaultConfig().isBackupsEnabled()) {
                new de.sean.blockprot.bukkit.tasks.BackupTask(
                    BlockProt.getInstance().getDataFolder(), true).run();
            }
            BlockProt.getInstance().reloadConfigAndTranslations();
            ComponentMessages.sendLegacyActionBar(player,
                Translator.get(TranslationKey.MESSAGES__ADMIN_RELOAD_DONE));
        } else if (slot == SLOT_BACK) {
            goBack(player, state);
        }
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    // -- shared item builders --

    static ItemStack toggleItem(String title, String configKey, String description, boolean active) {
        ItemStack stack = new ItemStack(Material.REPEATER);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        String state = strip(Translator.get(active
            ? TranslationKey.ENABLED : TranslationKey.DISABLED));
        ComponentMessages.displayName(meta, Component.text()
            .append(Component.text(strip(title), NamedTextColor.WHITE))
            .append(Component.text(": ", SOFT_GRAY))
            .append(Component.text(state, active ? PASTEL_MINT : PASTEL_CORAL))
            .build());
        ComponentMessages.lore(meta, List.of(
            Component.text(strip(description), SOFT_GRAY),
            Component.text(configKey, TextColor.color(0x666666)),
            Component.text(strip(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__VALUE_BOOL)), SOFT_GRAY)));
        stack.setItemMeta(meta);
        return stack;
    }

    static ItemStack valueItem(String title, String current, String description, String configKey) {
        ItemStack stack = new ItemStack(Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        ComponentMessages.displayName(meta, Component.text()
            .append(Component.text(strip(title), NamedTextColor.WHITE))
            .append(Component.text(": ", SOFT_GRAY))
            .append(Component.text(strip(current), PASTEL_GOLD, TextDecoration.BOLD))
            .build());
        ComponentMessages.lore(meta, List.of(
            Component.text(strip(description), SOFT_GRAY),
            Component.text(configKey, TextColor.color(0x666666)),
            Component.text(strip(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__VALUE_CURRENT))
                + strip(current), TextColor.color(0x888888)),
            Component.text(strip(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__VALUE_CLICK_EDIT)),
                TextColor.color(0x888888))));
        stack.setItemMeta(meta);
        return stack;
    }

    static String strip(String s) {
        return s.replaceAll("[§&][0-9a-fk-orxA-F]", "");
    }

    private ItemStack categoryItem(Material material, TranslationKey nameKey) {
        return item(material, Translator.get(nameKey),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__VALUE_CLICK_EDIT));
    }

    private void fillSeparators() {
        ItemStack sep = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = sep.getItemMeta();
        if (meta != null) {
            ComponentMessages.displayName(meta, Component.text(""));
            sep.setItemMeta(meta);
        }
        for (int s : SEPARATOR_SLOTS) {
            inventory.setItem(s, sep);
        }
        ItemStack sectionHeader = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta hMeta = sectionHeader.getItemMeta();
        if (hMeta != null) {
            ComponentMessages.displayName(hMeta, Component.text(""));
            sectionHeader.setItemMeta(hMeta);
        }
        for (int s = 9; s < getSize(); s += 9) {
            inventory.setItem(s, sectionHeader);
        }
    }

    static ItemStack item(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        ComponentMessages.displayName(meta, Component.text(
            name.replaceAll("[§&][0-9a-fk-orx]", "")));
        List<Component> loreList = new ArrayList<>(lore.length);
        for (String s : lore) {
            loreList.add(LegacyComponentSerializer.legacySection().deserialize(s));
        }
        ComponentMessages.lore(meta, loreList);
        item.setItemMeta(meta);
        return item;
    }
}