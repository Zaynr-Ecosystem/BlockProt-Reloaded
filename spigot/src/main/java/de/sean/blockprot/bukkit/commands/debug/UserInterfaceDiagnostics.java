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

package de.sean.blockprot.bukkit.commands.debug;

import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.dialogs.*;
import de.sean.blockprot.bukkit.inventories.*;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.EntityNBTHandler;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class UserInterfaceDiagnostics {

    private UserInterfaceDiagnostics() {
    }

    public static void run(@NotNull DiagnosticsContext ctx) {
        DiagnosticsContext.runDomain("8/10", "USER INTERFACE & SCREENS");
        if (ctx.player() != null) {
            checkInventoryCreation(ctx);
            checkAllInventories(ctx);
            checkAllDialogs(ctx);
        } else {
            BlockProtLogger.skipSub("Inventories and dialogs", "require an online player to render");
        }
    }

    private static void checkInventoryCreation(@NotNull DiagnosticsContext ctx) {
        Player player = ctx.player();
        if (player == null) return;
        try {
            Inventory inv = ComponentMessages.createInventory(player, 9,
                net.kyori.adventure.text.Component.text(Translator.get(TranslationKey.MESSAGES__DEBUG__INVENTORY_TITLE)));
            BlockProtLogger.pass("Inventory creation: ComponentMessages.createInventory OK (size=" + inv.getSize() + ")");
            ctx.pass();
        } catch (Throwable e) {
            BlockProtLogger.fail("createInventory", e.getMessage());
            ctx.fail();
        }
    }

    private static void checkAllInventories(@NotNull DiagnosticsContext ctx) {
        Player player = ctx.player();
        if (player == null) return;

        InventoryState base = new InventoryState(null);
        base.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
        base.origin = InventoryState.MenuOrigin.NONE;
        InventoryState.set(player.getUniqueId(), base);

        BlockProtLogger.subGroup("Inventories:");

        ctx.inv("UserMenuInventory", () -> new UserMenuInventory().fill(player));
        ctx.inv("AdminMenuInventory", () -> new AdminMenuInventory().fill(player));
        ctx.inv("UserSettingsInventory", () -> new UserSettingsInventory().fill(player));
        ctx.inv("StatisticsInventory", () -> new StatisticsInventory().fill(player));
        ctx.inv("FriendManageInventory (default)", () -> new FriendManageInventory().fill(player));

        ctx.inv("StatisticListInventory", () -> {
            PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
            StatHandler.getStatistic(stat, player);
            InventoryState ns = new InventoryState(null);
            ns.currentPageIndex = 0;
            InventoryState.set(player.getUniqueId(), ns);
            @SuppressWarnings("unchecked")
            var castedStat = (de.sean.blockprot.bukkit.nbt.stats.BukkitListStatistic<
                de.sean.blockprot.nbt.stats.ListStatisticItem<?, Material>, ?>)
                (de.sean.blockprot.bukkit.nbt.stats.BukkitListStatistic<?, ?>) stat;
            return new StatisticListInventory().fill(player, castedStat);
        });

        ctx.inv("AdminBlockListInventory", () -> {
            PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
            StatHandler.getStatistic(stat, player);
            InventoryState ns = new InventoryState(null);
            ns.origin = InventoryState.MenuOrigin.ADMIN_MENU;
            InventoryState.set(player.getUniqueId(), ns);
            return new AdminBlockListInventory().fill(player, player.getName(), stat);
        });

        ctx.inv("RedstoneSettingsInventory", () -> {
            var loc = player.getLocation().clone();
            var world = player.getWorld();
            var orig = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                new BlockNBTHandler(block).setOwner(player.getUniqueId().toString());
                InventoryState rs = new InventoryState(block);
                rs.friendSearchState = InventoryState.FriendSearchState.FRIEND_SEARCH;
                InventoryState.set(player.getUniqueId(), rs);
                return new RedstoneSettingsInventory().fill(player, rs);
            } finally {
                world.setType(loc, orig);
            }
        });

        ctx.inv("BlockLockInventory", () -> {
            var loc = player.getLocation().clone();
            var world = player.getWorld();
            var orig = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                var h = new BlockNBTHandler(block);
                h.setOwner(player.getUniqueId().toString());
                InventoryState bl = new InventoryState(block);
                bl.friendSearchState = InventoryState.FriendSearchState.FRIEND_SEARCH;
                InventoryState.set(player.getUniqueId(), bl);
                return new BlockLockInventory().fill(player, Material.CHEST, h);
            } finally {
                world.setType(loc, orig);
            }
        });

        ctx.inv("BlockInfoInventory", () -> {
            var loc = player.getLocation().clone();
            var world = player.getWorld();
            var orig = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                var h = new BlockNBTHandler(block);
                h.setOwner(player.getUniqueId().toString());
                InventoryState bi = new InventoryState(block);
                bi.currentPageIndex = 0;
                InventoryState.set(player.getUniqueId(), bi);
                return new BlockInfoInventory().fill(player, h);
            } finally {
                world.setType(loc, orig);
            }
        });

        ctx.inv("BlockInspectContentsInventory", () -> {
            var loc = player.getLocation().clone();
            var world = player.getWorld();
            var orig = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                new BlockNBTHandler(block).setOwner(player.getUniqueId().toString());
                InventoryState bic = new InventoryState(block);
                InventoryState.set(player.getUniqueId(), bic);
                return new BlockInspectContentsInventory(player).fill();
            } finally {
                world.setType(loc, orig);
            }
        });

        ctx.inv("AuditInventory (no entries)", () -> {
            var loc = player.getLocation().clone();
            var world = player.getWorld();
            var orig = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                InventoryState ai = new InventoryState(block);
                ai.currentPageIndex = 0;
                InventoryState.set(player.getUniqueId(), ai);
                return new AuditInventory().fill(player);
            } finally {
                world.setType(loc, orig);
            }
        });

        BlockProtLogger.skipSub("EntitySettingsInventory", "requires live Entity, not testable without one");
        ctx.touchScreen(DiagnosticsContext.INVENTORY_PACKAGE, "EntitySettingsInventory");
        ctx.pass();

        ctx.inv("FriendSearchHistoryInventory", () -> {
            InventoryState fh = new InventoryState(null);
            fh.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            InventoryState.set(player.getUniqueId(), fh);
            return new FriendSearchHistoryInventory().fill(player);
        });

        ctx.inv("AutoDropInventory", () -> new AutoDropInventory().fill(player));
        ctx.inv("AutoDropFamilyInventory", () -> {
            InventoryState ad = new InventoryState(null);
            ad.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            InventoryState.set(player.getUniqueId(), ad);
            return new AutoDropFamilyInventory().fill(player, BlockFamilyParser.Family.BLOCKS, 0, ad);
        });
        ctx.inv("AutoDropSearchInventory", () -> {
            InventoryState ad = new InventoryState(null);
            ad.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            InventoryState.set(player.getUniqueId(), ad);
            return new AutoDropSearchInventory().fill(player, "chest", 0);
        });
        ctx.inv("LockablesInventory", () -> {
            InventoryState lk = new InventoryState(null);
            InventoryState.set(player.getUniqueId(), lk);
            return new LockablesInventory().fill(player, 0);
        });
        ctx.inv("WorldExpiryInventory", () -> {
            InventoryState we = new InventoryState(null);
            we.currentPageIndex = 0;
            InventoryState.set(player.getUniqueId(), we);
            return new WorldExpiryInventory().fill(player, 0);
        });
        ctx.inv("WorldLockableSelectionInventory", () -> new WorldLockableSelectionInventory().fill(player));
        ctx.inv("WorldLockableDetailInventory", () -> {
            org.bukkit.World w = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            if (w == null) return null;
            return new WorldLockableDetailInventory(w).fill(player);
        });
        ctx.inv("BpUnlockInventory", () -> {
            PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
            StatHandler.getStatistic(stat, player);
            return new BpUnlockInventory().fill(player, player.getName(), stat);
        });
        ctx.inv("FriendCandidateSelectionInventory", () -> new FriendCandidateSelectionInventory(
            List.of(), match -> {}, () -> null).fill(player));
        ctx.inv("FriendDetailInventory", () -> new FriendDetailInventory().fill(player));
        ctx.inv("FriendSearchResultInventory", () -> new FriendSearchResultInventory().fill(player, player.getName()));
        ctx.inv("TransferSearchInventory", () -> new TransferSearchInventory().fill(player, player.getName()));

        BlockProtLogger.skipSub("PlayerListInventory", "open() opens a live GUI, no fill() to build");
        ctx.touchScreen(DiagnosticsContext.INVENTORY_PACKAGE, "PlayerListInventory");
        ctx.pass();

        ctx.inv("EntityInfoInventory", () -> {
            var loc = player.getLocation().clone();
            var world = player.getWorld();
            var ent = world.spawn(loc, ArmorStand.class, stand -> {
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setSilent(true);
            });
            try {
                var h = new EntityNBTHandler(ent);
                h.setOwner(player.getUniqueId().toString());
                return new EntityInfoInventory().fill(player, ent, h);
            } finally {
                ent.remove();
            }
        });
        ctx.inv("EntityBlockSettingsInventory", () -> {
            var loc = player.getLocation().clone();
            var world = player.getWorld();
            var ent = world.spawn(loc, ArmorStand.class, stand -> {
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setSilent(true);
            });
            try {
                var h = new EntityNBTHandler(ent);
                h.setOwner(player.getUniqueId().toString());
                return new EntityBlockSettingsInventory().fill(player, ent, h);
            } finally {
                ent.remove();
            }
        });
        ctx.inv("EntityFriendManageInventory", () -> {
            var loc = player.getLocation().clone();
            var world = player.getWorld();
            var ent = world.spawn(loc, ArmorStand.class, stand -> {
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setSilent(true);
            });
            try {
                var h = new EntityNBTHandler(ent);
                h.setOwner(player.getUniqueId().toString());
                return new EntityFriendManageInventory().fill(player, ent, h);
            } finally {
                ent.remove();
            }
        });
        ctx.inv("EntityFriendSearchResultInventory", () -> {
            var loc = player.getLocation().clone();
            var world = player.getWorld();
            var ent = world.spawn(loc, ArmorStand.class, stand -> {
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setSilent(true);
            });
            try {
                var h = new EntityNBTHandler(ent);
                h.setOwner(player.getUniqueId().toString());
                return new EntityFriendSearchResultInventory().fill(player, ent, h, player.getName());
            } finally {
                ent.remove();
            }
        });
        BlockProtLogger.skipSub("EntityInspectContentsInventory", "requires a container entity, not testable with an ArmorStand");
        ctx.touchScreen(DiagnosticsContext.INVENTORY_PACKAGE, "EntityInspectContentsInventory");
        ctx.pass();
        ctx.inv("WorldProtDeleteInventory", () -> new WorldProtDeleteInventory().fill(player, null));
        ctx.inv("WorldProtDeleteConfirmInventory", () -> {
            org.bukkit.World w = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            if (w == null) return null;
            return new WorldProtDeleteConfirmInventory().fill(player, w.getName());
        });

        ctx.inv("AdminConfigInventory", () -> new AdminConfigInventory().fill(player));
        ctx.inv("AdminConfigLanguageInventory", () -> new AdminConfigLanguageInventory().fill(player));
        ctx.inv("AdminConfigWorldsInventory", () -> new AdminConfigWorldsInventory().fill(player));
        ctx.inv("AdminConfigPlayersInventory", () -> new AdminConfigPlayersInventory().fill(player));
        ctx.inv("AdminConfigBlocksInventory", () -> new AdminConfigBlocksInventory().fill(player));
        ctx.inv("AdminConfigEntityInventory", () -> new AdminConfigEntityInventory().fill(player));
        ctx.inv("AdminConfigExpiryInventory", () -> new AdminConfigExpiryInventory().fill(player));
        ctx.inv("AdminConfigRaidInventory", () -> new AdminConfigRaidInventory().fill(player));
        ctx.inv("AdminConfigNotificationsInventory", () -> new AdminConfigNotificationsInventory().fill(player));
        ctx.inv("AdminConfigMaintenanceInventory", () -> new AdminConfigMaintenanceInventory().fill(player));
        ctx.inv("AdminTiersInventory", () -> new AdminTiersInventory().fill(player, 0));
        ctx.inv("AdminTierSelectInventory", () -> new AdminTierSelectInventory(player.getName(), player.getUniqueId()).fill(player));
        ctx.inv("AdminCustomFlagsInventory", () -> new AdminCustomFlagsInventory(player.getName(), player.getUniqueId()).fill(player));

        BlockProtLogger.skipSub("FriendSearchInventory", "chat-input gateway, no fill() to build");
        ctx.touchScreen(DiagnosticsContext.INVENTORY_PACKAGE, "FriendSearchInventory");
        ctx.pass();

        InventoryState.set(player.getUniqueId(), base);

        BlockProtLogger.subGroup("Inventory title translations (14 titles):");
        TranslationKey[] titleKeys = {
            TranslationKey.INVENTORIES__BLOCK_LOCK,
            TranslationKey.INVENTORIES__BLOCK_INFO__TITLE,
            TranslationKey.INVENTORIES__USER_SETTINGS,
            TranslationKey.INVENTORIES__USER_MENU__TITLE,
            TranslationKey.INVENTORIES__ADMIN_MENU__TITLE,
            TranslationKey.INVENTORIES__ADMIN_BLOCK_LIST__TITLE,
            TranslationKey.INVENTORIES__FRIENDS__MANAGE,
            TranslationKey.INVENTORIES__FRIENDS__EDIT,
            TranslationKey.INVENTORIES__TIMED__TITLE,
            TranslationKey.INVENTORIES__REDSTONE__SETTINGS,
            TranslationKey.INVENTORIES__STATISTICS__STATISTICS,
            TranslationKey.INVENTORIES__AUDIT__TITLE,
            TranslationKey.INVENTORIES__TRANSFER__TITLE,
            TranslationKey.INVENTORIES__ENTITY_SETTINGS__SETTINGS,
        };
        for (TranslationKey k : titleKeys) {
            String v = Translator.get(k);
            if (v == null || v.isBlank()) {
                BlockProtLogger.failSub(k.name(), "blank/missing");
                ctx.fail();
            } else {
                BlockProtLogger.passSub(k.name() + " = \"" + v + "\"");
                ctx.pass();
            }
        }
    }

    private static void checkAllDialogs(@NotNull DiagnosticsContext ctx) {
        Player player = ctx.player();
        if (player == null) return;

        DialogBridgeFactory.setTestBridge(new NoopDialogBridge());
        try {
            BlockProtLogger.subGroup("Dialogs:");
            ctx.dlg("AboutDialog", () -> AboutDialog.show(player));
            ctx.dlg("AdminMenuDialog", () -> AdminMenuDialog.show(player));
            ctx.dlg("AdminConfigDialog", () -> AdminConfigDialog.show(player));
            ctx.dlg("AdminConfigLanguageDialog", () -> AdminConfigLanguageDialog.show(player, DialogOrigin.ADMIN_MENU));
            ctx.dlg("AdminConfigWorldsDialog", () -> AdminConfigWorldsDialog.show(player, DialogOrigin.ADMIN_MENU));
            ctx.dlg("AdminConfigPlayersDialog", () -> AdminConfigPlayersDialog.show(player, DialogOrigin.ADMIN_MENU));
            ctx.dlg("AdminConfigBlocksDialog", () -> AdminConfigBlocksDialog.show(player, DialogOrigin.ADMIN_MENU));
            ctx.dlg("AdminConfigBlocksLockingDialog", () -> AdminConfigBlocksLockingDialog.show(player, DialogOrigin.ADMIN_MENU));
            ctx.dlg("AdminConfigBlocksBehaviorDialog", () -> AdminConfigBlocksBehaviorDialog.show(player, DialogOrigin.ADMIN_MENU));
            ctx.dlg("AdminConfigBlocksEffectsDialog", () -> AdminConfigBlocksEffectsDialog.show(player, DialogOrigin.ADMIN_MENU));
            ctx.dlg("AdminConfigEntityDialog", () -> AdminConfigEntityDialog.show(player, DialogOrigin.ADMIN_MENU));
            ctx.dlg("AdminConfigExpiryDialog", () -> AdminConfigExpiryDialog.show(player, DialogOrigin.ADMIN_MENU));
            ctx.dlg("AdminConfigRaidDialog", () -> AdminConfigRaidDialog.show(player, DialogOrigin.ADMIN_MENU));
            ctx.dlg("AdminConfigNotificationsDialog", () -> AdminConfigNotificationsDialog.show(player, DialogOrigin.ADMIN_MENU));
            ctx.dlg("AdminConfigMaintenanceDialog", () -> AdminConfigMaintenanceDialog.show(player, DialogOrigin.ADMIN_MENU));
            ctx.dlg("AuditDialog", () -> {
                var loc = player.getLocation().clone();
                var world = player.getWorld();
                var orig = world.getBlockAt(loc).getType();
                world.setType(loc, Material.CHEST);
                try {
                    var block = world.getBlockAt(loc);
                    new BlockNBTHandler(block).setOwner(player.getUniqueId().toString());
                    AuditDialog.show(player, block);
                } finally {
                    world.setType(loc, orig);
                }
            });
            ctx.dlg("AutoDropDialog", () -> AutoDropDialog.show(player, DialogOrigin.ADMIN_MENU));
            ctx.dlg("AutoDropFamilyDialog", () -> AutoDropFamilyDialog.show(player, DialogOrigin.ADMIN_MENU, BlockFamilyParser.Family.BLOCKS));
            ctx.dlg("AutoDropSearchDialog", () -> AutoDropSearchDialog.show(player, DialogOrigin.ADMIN_MENU, null, "chest", 0));
            ctx.dlg("BlockInfoDialog", () -> {
                var loc = player.getLocation().clone();
                var world = player.getWorld();
                var orig = world.getBlockAt(loc).getType();
                world.setType(loc, Material.CHEST);
                try {
                    var block = world.getBlockAt(loc);
                    var h = new BlockNBTHandler(block);
                    h.setOwner(player.getUniqueId().toString());
                    BlockInfoDialog.show(player, block, h);
                } finally {
                    world.setType(loc, orig);
                }
            });
            ctx.dlg("BlockLockDialog", () -> {
                var loc = player.getLocation().clone();
                var world = player.getWorld();
                var orig = world.getBlockAt(loc).getType();
                world.setType(loc, Material.CHEST);
                try {
                    var block = world.getBlockAt(loc);
                    var h = new BlockNBTHandler(block);
                    h.setOwner(player.getUniqueId().toString());
                    BlockLockDialog.show(player, block, h);
                } finally {
                    world.setType(loc, orig);
                }
            });
            ctx.dlg("BlockSettingsDialog", () -> {
                var loc = player.getLocation().clone();
                var world = player.getWorld();
                var orig = world.getBlockAt(loc).getType();
                world.setType(loc, Material.CHEST);
                try {
                    var block = world.getBlockAt(loc);
                    var h = new BlockNBTHandler(block);
                    h.setOwner(player.getUniqueId().toString());
                    BlockSettingsDialog.show(player, block, h);
                } finally {
                    world.setType(loc, orig);
                }
            });
            ctx.dlg("DebugDialog", () -> DebugDialog.show(player));
            ctx.dlg("EntityInfoDialog", () -> {
                var loc = player.getLocation().clone();
                var world = player.getWorld();
                var ent = world.spawn(loc, ArmorStand.class, stand -> {
                    stand.setGravity(false);
                    stand.setVisible(false);
                    stand.setSilent(true);
                });
                try {
                    var h = new EntityNBTHandler(ent);
                    h.setOwner(player.getUniqueId().toString());
                    EntityInfoDialog.show(player, ent, h);
                } finally {
                    ent.remove();
                }
            });
            ctx.dlg("EntityBlockSettingsDialog", () -> {
                var loc = player.getLocation().clone();
                var world = player.getWorld();
                var ent = world.spawn(loc, ArmorStand.class, stand -> {
                    stand.setGravity(false);
                    stand.setVisible(false);
                    stand.setSilent(true);
                });
                try {
                    var h = new EntityNBTHandler(ent);
                    h.setOwner(player.getUniqueId().toString());
                    EntityBlockSettingsDialog.show(player, ent, h);
                } finally {
                    ent.remove();
                }
            });
            ctx.dlg("EntitySettingsDialog", () -> {
                var ent = player.getWorld().spawn(player.getLocation().clone(), Wolf.class, wolf -> {
                    wolf.setAI(false);
                    wolf.setSilent(true);
                });
                try {
                    EntitySettingsDialog.show(player, ent);
                } finally {
                    ent.remove();
                }
            });
            ctx.dlg("EntityFriendManageDialog", () -> {
                var loc = player.getLocation().clone();
                var world = player.getWorld();
                var ent = world.spawn(loc, ArmorStand.class, stand -> {
                    stand.setGravity(false);
                    stand.setVisible(false);
                    stand.setSilent(true);
                });
                try {
                    var h = new EntityNBTHandler(ent);
                    h.setOwner(player.getUniqueId().toString());
                    EntityFriendManageDialog.show(player, ent, h);
                } finally {
                    ent.remove();
                }
            });
            ctx.dlg("FriendManageDialog", () -> FriendManageDialog.show(player));
            ctx.dlg("FriendCandidateSelectionDialog", () -> FriendCandidateSelectionDialog.show(
                player, List.of(), match -> {}, () -> {}));
            ctx.dlg("InfoDialog", () -> InfoDialog.show(player, DialogOrigin.ADMIN_MENU));
            ctx.dlg("IntegrationsDialog", () -> IntegrationsDialog.show(player));
            ctx.dlg("LockablesDialog", () -> LockablesDialog.show(player, DialogOrigin.ADMIN_MENU));
            ctx.dlg("LockableCategoryDialog", () -> LockableCategoryDialog.show(
                player, DialogOrigin.ADMIN_MENU, "lockable_blocks", List.of(Material.CHEST)));
            ctx.dlg("ProtdelDialog", () -> ProtdelDialog.show(player, null));
            ctx.dlg("StatsDialog", () -> StatsDialog.show(player, DialogOrigin.ADMIN_MENU));
            ctx.dlg("UnlockDialog", () -> UnlockDialog.show(player, player.getName()));
            ctx.dlg("UpdateDialog", () -> UpdateDialog.show(player));
            ctx.dlg("UserMenuDialog", () -> UserMenuDialog.show(player));
            ctx.dlg("UserSettingsDialog", () -> UserSettingsDialog.show(player));
            ctx.dlg("WorldLockableSelectionDialog", () -> WorldLockableSelectionDialog.show(player, DialogOrigin.ADMIN_MENU));
            ctx.dlg("WorldLockableDetailDialog", () -> {
                org.bukkit.World w = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
                if (w == null) return;
                WorldLockableDetailDialog.show(player, DialogOrigin.ADMIN_MENU, w);
            });
            ctx.dlg("AdminTiersDialog", () -> AdminTiersDialog.show(player, DialogOrigin.ADMIN_MENU));
            ctx.dlg("AdminTierSelectDialog", () -> AdminTierSelectDialog.show(player, player.getName(), player.getUniqueId(), DialogOrigin.ADMIN_MENU));
            ctx.dlg("AdminCustomFlagsDialog", () -> AdminCustomFlagsDialog.show(player, player.getName(), player.getUniqueId(), DialogOrigin.ADMIN_MENU));
            ctx.dlg("FriendDetailDialog", () -> {
                var loc = player.getLocation().clone();
                var world = player.getWorld();
                var orig = world.getBlockAt(loc).getType();
                world.setType(loc, Material.CHEST);
                try {
                    var block = world.getBlockAt(loc);
                    var h = new BlockNBTHandler(block);
                    h.setOwner(player.getUniqueId().toString());
                    h.addFriend(player.getUniqueId().toString());
                    FriendDetailDialog.showForBlock(player, block, h, player.getUniqueId().toString(), 0);
                } finally {
                    world.setType(loc, orig);
                }
            });
            ctx.dlg("WorldExpiryDialog", () -> WorldExpiryDialog.show(player, DialogOrigin.ADMIN_MENU));
        } finally {
            DialogBridgeFactory.setTestBridge(null);
        }
    }
}
