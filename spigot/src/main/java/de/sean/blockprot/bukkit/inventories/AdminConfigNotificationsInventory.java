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
import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

/**
 * Notifications category of the inventory-based admin config editor.
 */
public final class AdminConfigNotificationsInventory extends BlockProtInventory {

    private static final int SLOT_NOTIFY_OPS = 11;
    private static final int SLOT_NOTIFY_ADMINS = 20;
    private static final int SLOT_OWNER_NOTIFICATIONS = 15;
    private static final int SLOT_NOTIFY_OPEN = 23;
    private static final int SLOT_NOTIFY_TAKE = 24;
    private static final int SLOT_NOTIFY_PLACE = 25;
    private static final int SLOT_BACK = 49;

    private static final int[] SEPARATOR_SLOTS = {
        0,1,2,3,4,5,6,7,8,
        9,10, 12,13,14, 16,17,
        18,19, 21,22, 26,
        27,28,29,30,31,32,33,34,35,
        36,37,38,39,40,41,42,43,44,
        45,46,47,48, 50,51,52,53
    };

    public AdminConfigNotificationsInventory() { super(false); }

    @Override int getSize() { return InventoryConstants.sextupletLine; }

    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_NOTIFICATIONS);
    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        inventory = createInventory();
        fillSeparators();
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        inventory.setItem(SLOT_NOTIFY_OPS, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__NOTIFY_OPS_TITLE),
            "notify_op_of_updates",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__NOTIFY_OPS),
            cfg.shouldNotifyOpOfUpdates()));
        inventory.setItem(SLOT_NOTIFY_ADMINS, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__NOTIFY_ADMINS_TITLE),
            "notify_admins_on_join",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__NOTIFY_ADMINS),
            cfg.isNotifyAdminsOnJoinSettingEnabled()));
        inventory.setItem(SLOT_OWNER_NOTIFICATIONS, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__OWNER_ENABLED_TITLE),
            "owner_notifications.enabled",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__OWNER_ENABLED),
            cfg.isOwnerNotificationsEnabled()));

        boolean notifyOpen = cfg.getBukkitConfig().getBoolean("owner_notifications.notify_on_open", true);
        inventory.setItem(SLOT_NOTIFY_OPEN, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__OWNER_OPEN_TITLE),
            "owner_notifications.notify_on_open",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__OWNER_OPEN),
            notifyOpen));

        boolean notifyTake = cfg.getBukkitConfig().getBoolean("owner_notifications.notify_on_take", true);
        inventory.setItem(SLOT_NOTIFY_TAKE, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__OWNER_TAKE_TITLE),
            "owner_notifications.notify_on_take",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__OWNER_TAKE),
            notifyTake));

        boolean notifyPlace = cfg.getBukkitConfig().getBoolean("owner_notifications.notify_on_place", true);
        inventory.setItem(SLOT_NOTIFY_PLACE, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__OWNER_PLACE_TITLE),
            "owner_notifications.notify_on_place",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__OWNER_PLACE),
            notifyPlace));

        setBackButton(SLOT_BACK);
        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= getSize()) return;

        DefaultConfig cfg = BlockProt.getDefaultConfig();
        if (slot == SLOT_BACK) {
            player.openInventory(new AdminConfigInventory().fill(player));
        } else if (slot == SLOT_NOTIFY_OPS) {
            cfg.setNotifyOpOfUpdates(!cfg.shouldNotifyOpOfUpdates());
            player.openInventory(fill(player));
        } else if (slot == SLOT_NOTIFY_ADMINS) {
            cfg.setNotifyAdminsOnJoin(!cfg.isNotifyAdminsOnJoinSettingEnabled());
            player.openInventory(fill(player));
        } else if (slot == SLOT_OWNER_NOTIFICATIONS) {
            cfg.setAndSave("owner_notifications.enabled", !cfg.isOwnerNotificationsEnabled());
            player.openInventory(fill(player));
        } else if (slot == SLOT_NOTIFY_OPEN) {
            boolean notifyOpen = cfg.getBukkitConfig().getBoolean("owner_notifications.notify_on_open", true);
            cfg.setAndSave("owner_notifications.notify_on_open", !notifyOpen);
            player.openInventory(fill(player));
        } else if (slot == SLOT_NOTIFY_TAKE) {
            boolean notifyTake = cfg.getBukkitConfig().getBoolean("owner_notifications.notify_on_take", true);
            cfg.setAndSave("owner_notifications.notify_on_take", !notifyTake);
            player.openInventory(fill(player));
        } else if (slot == SLOT_NOTIFY_PLACE) {
            boolean notifyPlace = cfg.getBukkitConfig().getBoolean("owner_notifications.notify_on_place", true);
            cfg.setAndSave("owner_notifications.notify_on_place", !notifyPlace);
            player.openInventory(fill(player));
        }
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

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
    }
}