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
import de.sean.blockprot.bukkit.BlockProtAPI;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.admin.AdminAction;
import de.sean.blockprot.bukkit.admin.AdminTierManager;
import de.sean.blockprot.bukkit.commands.AboutCommand;
import de.sean.blockprot.bukkit.commands.DebugCommand;
import de.sean.blockprot.bukkit.dialogs.AdminConfigDialog;
import de.sean.blockprot.bukkit.dialogs.DialogOrigin;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import de.sean.blockprot.bukkit.tasks.UpdateChecker;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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

public class AdminMenuInventory extends BlockProtInventory {

    private static final int SLOT_LOCKABLES     = 10;
    private static final int SLOT_CONFIG        = 11;
    private static final int SLOT_WORLD_EXPIRY  = 12;
    private static final int SLOT_PROT_DEL      = 13;
    private static final int SLOT_UNLOCK        = 14;
    private static final int SLOT_INTEGRATIONS  = 15;
    private static final int SLOT_STATS         = 16;
    private static final int SLOT_TIERS         = 19;
    private static final int SLOT_INFO          = 20;
    private static final int SLOT_RELOAD        = 21;
    private static final int SLOT_UPDATE        = 22;
    private static final int SLOT_DEBUG         = 23;
    private static final int SLOT_ABOUT         = 24;
    private static final int SLOT_BACK          = 31;

    private static final int[] SEPARATOR_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8,
        9, 17,
        18, 25, 26,
        27, 28, 29, 30, 32, 33, 34, 35
    };

    private static final java.util.Map<Integer, AdminAction> SLOT_ACTIONS = java.util.Map.ofEntries(
        java.util.Map.entry(SLOT_LOCKABLES, AdminAction.LOCKABLES),
        java.util.Map.entry(SLOT_CONFIG, AdminAction.CONFIG),
        java.util.Map.entry(SLOT_TIERS, AdminAction.SETROLE),
        java.util.Map.entry(SLOT_RELOAD, AdminAction.RELOAD),
        java.util.Map.entry(SLOT_UPDATE, AdminAction.UPDATE),
        java.util.Map.entry(SLOT_INTEGRATIONS, AdminAction.INTEGRATIONS),
        java.util.Map.entry(SLOT_STATS, AdminAction.INFO),
        java.util.Map.entry(SLOT_DEBUG, AdminAction.DEBUG),
        java.util.Map.entry(SLOT_WORLD_EXPIRY, AdminAction.CONFIG),
        java.util.Map.entry(SLOT_PROT_DEL, AdminAction.PROTDEL),
        java.util.Map.entry(SLOT_INFO, AdminAction.INFO),
        java.util.Map.entry(SLOT_UNLOCK, AdminAction.UNLOCK)
    );

    public AdminMenuInventory() { super(false); }

    @Override int getSize() { return InventoryConstants.quadrupleLine; }

    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__TITLE);
    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        inventory = createInventory();
        fillSeparators();

        inventory.setItem(SLOT_CONFIG, item(Material.REPEATER,
            stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__CONFIG)),
            stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__CONFIG_LORE))));
        inventory.setItem(SLOT_LOCKABLES, item(Material.CHEST,
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__LOCKABLES),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__LOCKABLES_LORE)));
        if (BlockProt.getDefaultConfig().isWorldExpiryEnabled()) {
            inventory.setItem(SLOT_WORLD_EXPIRY, item(Material.CLOCK,
                Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__WORLD_EXPIRY),
                Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__WORLD_EXPIRY_LORE)));
        }
        inventory.setItem(SLOT_PROT_DEL, item(Material.BARRIER,
            Translator.get(TranslationKey.INVENTORIES__WORLD_PROT_DEL__TITLE),
            ""));
        inventory.setItem(SLOT_UNLOCK, item(Material.IRON_BARS,
            stripColor(Translator.get(TranslationKey.INVENTORIES__BP_UNLOCK__TITLE)).replace(": {player}", ""),
            stripColor(Translator.get(TranslationKey.DIALOGS__UNLOCK__HEADER))));
        inventory.setItem(SLOT_INTEGRATIONS, item(Material.CHAIN,
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__INTEGRATIONS),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__INTEGRATIONS_LORE)));
        inventory.setItem(SLOT_STATS, item(Material.BOOK,
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__STATS),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__STATS_LORE)));

        if (BlockProt.getDefaultConfig().isAdminTiersEnabled()) {
            inventory.setItem(SLOT_TIERS, item(Material.NETHERITE_CHESTPLATE,
                Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__TIERS),
                Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__TIERS_LORE)));
        }

        setPlayerSkullAsync(SLOT_INFO, player, player.getUniqueId(), player.getName(),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__INFO),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__INFO_LORE));
        inventory.setItem(SLOT_RELOAD, item(Material.COMPARATOR,
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__RELOAD),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__RELOAD_LORE)));
        inventory.setItem(SLOT_UPDATE, item(Material.SPYGLASS,
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__UPDATE),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__UPDATE_LORE)));
        inventory.setItem(SLOT_DEBUG, item(Material.COMMAND_BLOCK,
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__DEBUG),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__DEBUG_LORE)));
        inventory.setItem(SLOT_ABOUT, item(Material.NETHER_STAR,
            stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__ABOUT)),
            stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__ABOUT_LORE))));

        InventoryState state = InventoryState.get(player.getUniqueId());
        if (state != null && state.origin != InventoryState.MenuOrigin.NONE) {
            setBackButton(SLOT_BACK);
        } else {
            inventory.setItem(SLOT_BACK, item(Material.BARRIER,
                Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__CLOSE),
                Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__CLOSE_LORE)));
        }

        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= getSize()) return;
        AdminAction required = SLOT_ACTIONS.get(slot);
        if (required != null && AdminTierManager.deny(player, required)) return;

        if (slot == SLOT_LOCKABLES) {
            if (de.sean.blockprot.bukkit.bedrock.BedrockBridge.shouldUseBedrockForms(player)) {
                player.closeInventory();
                BlockProt.getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
                    de.sean.blockprot.bukkit.bedrock.forms.BedrockLockablesForm.show(player);
                }, 1L);
            } else if (BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
                player.closeInventory();
                de.sean.blockprot.bukkit.dialogs.LockablesDialog.show(player, DialogOrigin.ADMIN_MENU);
            } else {
                InventoryState newState = InventoryState.builder()
                    .origin(InventoryState.MenuOrigin.ADMIN_MENU)
                    .build();
                newState.originStack.push(InventoryState.MenuOrigin.ADMIN_MENU);
                newState.currentPageIndex = 0;
                InventoryState.set(player.getUniqueId(), newState);
                player.openInventory(new LockablesInventory().fill(player, 0));
            }

        } else if (slot == SLOT_CONFIG) {
            if (BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
                player.closeInventory();
                AdminConfigDialog.show(player, DialogOrigin.ADMIN_MENU);
            } else {
                InventoryState newState = InventoryState.builder()
                    .origin(InventoryState.MenuOrigin.ADMIN_MENU)
                    .build();
                newState.originStack.push(InventoryState.MenuOrigin.ADMIN_MENU);
                newState.currentPageIndex = 0;
                InventoryState.set(player.getUniqueId(), newState);
                player.openInventory(new AdminConfigInventory().fill(player));
            }

        } else if (slot == SLOT_TIERS && BlockProt.getDefaultConfig().isAdminTiersEnabled()) {
            if (BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
                player.closeInventory();
                de.sean.blockprot.bukkit.dialogs.AdminTiersDialog.show(player, DialogOrigin.ADMIN_MENU);
            } else {
                InventoryState newState = InventoryState.builder()
                    .origin(InventoryState.MenuOrigin.ADMIN_MENU)
                    .build();
                newState.originStack.push(InventoryState.MenuOrigin.ADMIN_MENU);
                newState.currentPageIndex = 0;
                InventoryState.set(player.getUniqueId(), newState);
                player.openInventory(new AdminTiersInventory().fill(player, 0));
            }

        } else if (slot == SLOT_RELOAD) {
            player.closeInventory();
            if (BlockProt.getDefaultConfig().isBackupsEnabled()) {
                new de.sean.blockprot.bukkit.tasks.BackupTask(
                    BlockProt.getInstance().getDataFolder(), true).run();
            }
            BlockProt.getInstance().reloadConfigAndTranslations();
            ComponentMessages.sendLegacyActionBar(player, Translator.get(TranslationKey.MESSAGES__ADMIN_RELOAD_DONE));

        } else if (slot == SLOT_UPDATE) {
            player.closeInventory();
            ComponentMessages.sendLegacyActionBar(player, Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__UPDATE_LORE));
            List<Player> opRecipients = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.isOp() || AdminTierManager.hasPermission(online, AdminAction.UPDATE)) {
                    opRecipients.add(online);
                }
            }
            if (!opRecipients.contains(player)) {
                opRecipients.add(player);
            }
            UpdateChecker checker = new UpdateChecker(BlockProt.getPluginVersion(), opRecipients, true);
            BlockProt.getFoliaLib().getScheduler().runAsync(asyncTask -> checker.run());

        } else if (slot == SLOT_INTEGRATIONS) {
            player.closeInventory();
            var list = BlockProtAPI.getInstance().getIntegrations().stream()
                .filter(i -> i.isEnabled())
                .map(i -> i.name).toList();
            String names = list.isEmpty() ? Translator.get(TranslationKey.MESSAGES__INTEGRATIONS__NONE) : String.join(", ", list);
            String msg = Translator.get(TranslationKey.MESSAGES__ADMIN_INTEGRATIONS)
                .replace("{count}", String.valueOf(list.size()))
                .replace("{integrations}", names);
            ComponentMessages.sendLegacyActionBar(player, msg);

        } else if (slot == SLOT_STATS) {
            InventoryState newState = new InventoryState(null);
            newState.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            newState.origin = InventoryState.MenuOrigin.ADMIN_MENU;
            newState.originStack.push(InventoryState.MenuOrigin.ADMIN_MENU);
            InventoryState.set(player.getUniqueId(), newState);
            player.openInventory(new StatisticsInventory().fill(player));

        } else if (slot == SLOT_DEBUG) {
            player.closeInventory();
            DebugCommand.run(player);

        } else if (slot == SLOT_WORLD_EXPIRY && BlockProt.getDefaultConfig().isWorldExpiryEnabled()) {
            InventoryState newState = InventoryState.builder()
                .origin(InventoryState.MenuOrigin.ADMIN_MENU)
                .build();
            newState.originStack.push(InventoryState.MenuOrigin.ADMIN_MENU);
            newState.currentPageIndex = 0;
            InventoryState.set(player.getUniqueId(), newState);
            player.openInventory(new WorldExpiryInventory().fill(player, 0));

        } else if (slot == SLOT_PROT_DEL) {
            InventoryState newState = InventoryState.builder()
                .origin(InventoryState.MenuOrigin.ADMIN_MENU)
                .build();
            newState.originStack.push(InventoryState.MenuOrigin.ADMIN_MENU);
            newState.currentPageIndex = 0;
            InventoryState.set(player.getUniqueId(), newState);
            player.openInventory(new WorldProtDeleteInventory().fill(player, ""));

        } else if (slot == SLOT_ABOUT) {
            player.closeInventory();
            AboutCommand.showAbout(player);

        } else if (slot == SLOT_INFO) {
            player.closeInventory();
            Consumer<String> handleName = inputName -> {
                if (inputName == null || inputName.isBlank()) return;
                BlockProt.getFoliaLib().getScheduler().runAsync(asyncTask -> {
                    @SuppressWarnings("deprecation")
                    OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(inputName);
                    if (target == null) {
                        @SuppressWarnings("deprecation")
                        OfflinePlayer fallback = Bukkit.getOfflinePlayer(inputName);
                        if (fallback.hasPlayedBefore()) target = fallback;
                    }
                    final OfflinePlayer finalTarget = target;
                    BlockProt.getFoliaLib().getScheduler().runAtEntity(player, syncTask -> {
                        if (finalTarget == null || finalTarget.getUniqueId() == null) {
                            ComponentMessages.sendLegacyActionBar(player, Translator.get(TranslationKey.MESSAGES__ADMIN_INFO_PLAYER_NOT_FOUND)
                                .replace("{player}", inputName));
                            return;
                        }
                        String displayName = finalTarget.getName() != null ? finalTarget.getName() : inputName;
                        PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
                        StatHandler.getStatisticByUuid(stat, finalTarget.getUniqueId());
                        InventoryState ns = new InventoryState(null);
                        ns.currentPageIndex = 0;
                        ns.origin = InventoryState.MenuOrigin.ADMIN_MENU;
                        ns.originStack.push(InventoryState.MenuOrigin.ADMIN_MENU);
                        InventoryState.set(player.getUniqueId(), ns);
                        player.openInventory(new AdminBlockListInventory().fill(player, displayName, stat));
                    });
                });
            };
            TextInput.open(player, BlockProt.getInstance(),
                Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__INFO), handleName);

        } else if (slot == SLOT_UNLOCK) {
            player.closeInventory();
            Consumer<String> handleName = inputName -> {
                if (inputName == null || inputName.isBlank()) return;
                BlockProt.getFoliaLib().getScheduler().runAsync(asyncTask -> {
                    @SuppressWarnings("deprecation")
                    OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(inputName);
                    if (target == null) {
                        @SuppressWarnings("deprecation")
                        OfflinePlayer fallback = Bukkit.getOfflinePlayer(inputName);
                        if (fallback.hasPlayedBefore()) target = fallback;
                    }
                    final OfflinePlayer finalTarget = target;
                    BlockProt.getFoliaLib().getScheduler().runAtEntity(player, syncTask -> {
                        if (finalTarget == null || finalTarget.getUniqueId() == null) {
                            ComponentMessages.sendLegacyActionBar(player, Translator.get(TranslationKey.MESSAGES__ADMIN_INFO_PLAYER_NOT_FOUND)
                                .replace("{player}", inputName));
                            return;
                        }
                        String displayName = finalTarget.getName() != null ? finalTarget.getName() : inputName;
                        PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
                        StatHandler.getStatisticByUuid(stat, finalTarget.getUniqueId());
                        if (stat.get().isEmpty()) {
                            ComponentMessages.sendLegacyActionBar(player, Translator.get(TranslationKey.MESSAGES__BP_UNLOCK_NO_BLOCKS)
                                .replace("{player}", displayName));
                            return;
                        }
                        InventoryState ns = InventoryState.builder()
                            .origin(InventoryState.MenuOrigin.ADMIN_MENU)
                            .build();
                        ns.originStack.push(InventoryState.MenuOrigin.ADMIN_MENU);
                        InventoryState.set(player.getUniqueId(), ns);
                        player.openInventory(new BpUnlockInventory().fill(player, displayName, stat));
                    });
                });
            };
            TextInput.open(player, BlockProt.getInstance(),
                stripColor(Translator.get(TranslationKey.INVENTORIES__BP_UNLOCK__TITLE)).replace(": {player}", ""), handleName);

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
        List<net.kyori.adventure.text.Component> loreList = new ArrayList<>(lore.length);
        for (String s : lore) loreList.add(LegacyComponentSerializer.legacySection().deserialize(s));
        ComponentMessages.lore(meta, loreList);
        item.setItemMeta(meta);
        return item;
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orx]", "");
    }
}
