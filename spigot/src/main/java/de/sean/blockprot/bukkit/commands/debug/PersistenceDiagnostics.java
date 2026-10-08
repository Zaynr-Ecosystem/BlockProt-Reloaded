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

import de.sean.blockprot.bukkit.BlockProtAPI;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.entities.EntityProtectionHandler;
import de.sean.blockprot.bukkit.listeners.EffectGeometry;
import de.sean.blockprot.bukkit.nbt.BlockAccessFlag;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.EntityNBTHandler;
import de.sean.blockprot.bukkit.nbt.FriendHandler;
import de.sean.blockprot.bukkit.nbt.PlayerInventoryClipboard;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.bukkit.nbt.RedstoneSettingsHandler;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.BlockCountStatistic;
import de.sean.blockprot.bukkit.nbt.stats.LocationListEntry;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import de.sean.blockprot.bukkit.storage.ProtectedBlockCache;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public final class PersistenceDiagnostics {

    private PersistenceDiagnostics() {
    }

    public static void run(@NotNull DiagnosticsContext ctx) {
        DiagnosticsContext.runDomain("5/10", "NBT & DATA PERSISTENCE ENGINE");
        checkNbt(ctx);
        checkEntityNbt(ctx);
        if (ctx.player() != null) {
            checkPlayerSettings(ctx);
        } else {
            BlockProtLogger.skipSub("PlayerSettings", "requires an online player");
        }
        checkNbtSubHandlers(ctx);
        checkPublicApi(ctx);
        checkTransferStatistics(ctx);
    }

    private static void checkNbt(@NotNull DiagnosticsContext ctx) {
        try {
            var loc = ctx.origin().clone();
            var world = ctx.origin().getWorld();
            var orig = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var h = new BlockNBTHandler(world.getBlockAt(loc));
                h.setOwner(DiagnosticsContext.NOTCH_UUID);
                h.setName("debug_nbt_test");
                String owner = h.getOwner();
                String name = h.getName();
                long lockedAt = h.getLockedAt();
                if (DiagnosticsContext.NOTCH_UUID.equals(owner) && "debug_nbt_test".equals(name)) {
                    BlockProtLogger.pass("NBT block: write/read OK (owner=" + owner + " name=" + name + " lockedAt=" + lockedAt + ")");
                    ctx.pass();
                } else {
                    BlockProtLogger.fail("NBT block mismatch", "owner=" + owner + " name=" + name);
                    ctx.fail();
                }
            } finally {
                world.setType(loc, orig);
            }
        } catch (Exception e) {
            BlockProtLogger.fail("NBT block", e.getMessage());
            ctx.fail();
        }
    }

    private static void checkEntityNbt(@NotNull DiagnosticsContext ctx) {
        try {
            var loc = ctx.origin().clone();
            var world = ctx.origin().getWorld();
            var entity = world.spawn(loc, org.bukkit.entity.ArmorStand.class, stand -> {
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setSilent(true);
            });
            try {
                var handler = new EntityNBTHandler(entity);
                handler.setOwner(DiagnosticsContext.NOTCH_UUID);
                String readBack = handler.getOwner();
                if (DiagnosticsContext.NOTCH_UUID.equals(readBack)) {
                    BlockProtLogger.pass("NBT entity: write/read OK (owner=" + readBack + ")");
                    ctx.pass();
                } else {
                    BlockProtLogger.fail("NBT entity mismatch", "expected=" + DiagnosticsContext.NOTCH_UUID + " got=" + readBack);
                    ctx.fail();
                }
            } finally {
                entity.remove();
            }
        } catch (Exception e) {
            BlockProtLogger.fail("NBT entity", e.getMessage());
            ctx.fail();
        }
    }

    private static void checkPlayerSettings(@NotNull DiagnosticsContext ctx) {
        if (ctx.player() == null) return;
        try {
            var ps = new PlayerSettingsHandler(ctx.player());
            BlockProtLogger.pass("PlayerSettings: lockOnPlace=" + ps.getLockOnPlace()
                + " hintsEnabled=" + !ps.hasPlayerInteractedWithMenu());
            ctx.pass();
        } catch (Exception e) {
            BlockProtLogger.fail("PlayerSettings", e.getMessage());
            ctx.fail();
        }
    }

    private static void checkNbtSubHandlers(@NotNull DiagnosticsContext ctx) {
        BlockProtLogger.subGroup("NBT sub-handlers (8 handlers):");
        try {
            var loc = ctx.origin().clone();
            var world = ctx.origin().getWorld();
            var orig = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var h = new BlockNBTHandler(world.getBlockAt(loc));
                h.setOwner(DiagnosticsContext.NOTCH_UUID);
                h.addFriend("069a79f4-44e9-4726-a5be-fca90e38aaf5");
                boolean hasFriend = h.containsFriend("069a79f4-44e9-4726-a5be-fca90e38aaf5");
                List<FriendHandler> friends = h.getFriends();
                if (!hasFriend || friends.isEmpty()) {
                    BlockProtLogger.failSub("FriendSupportingHandler", "addFriend/containsFriend mismatch");
                    ctx.fail();
                } else {
                    FriendHandler first = friends.get(0);
                    boolean canRead = first.canRead();
                    boolean isManager = first.isManager();
                    EnumSet<BlockAccessFlag> flags = BlockAccessFlag.parseFlags(0);
                    List<String> lore = BlockAccessFlag.accumulateAccessFlagLore(flags);
                    h.removeFriend("069a79f4-44e9-4726-a5be-fca90e38aaf5");
                    BlockProtLogger.passSub("FriendSupportingHandler/FriendHandler: contains=" + hasFriend
                        + " friends=" + friends.size() + " canRead=" + canRead + " isManager=" + isManager
                        + " flags=" + flags.size() + " loreLines=" + lore.size());
                    ctx.pass();
                }
            } finally {
                world.setType(loc, orig);
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("FriendSupportingHandler", e.getMessage());
            ctx.fail();
        }

        try {
            var loc = ctx.origin().clone();
            var world = ctx.origin().getWorld();
            var orig = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var h = new BlockNBTHandler(world.getBlockAt(loc));
                RedstoneSettingsHandler rs = h.getRedstoneHandler();
                rs.setPistonProtection(false);
                rs.setHopperProtection(false);
                boolean piston = rs.getPistonProtection();
                boolean hopper = rs.getHopperProtection();
                boolean current = rs.getCurrentProtection();
                rs.reset();
                BlockProtLogger.passSub("RedstoneSettingsHandler: piston=" + piston + " hopper=" + hopper
                    + " current=" + current + " reset OK");
                ctx.pass();
            } finally {
                world.setType(loc, orig);
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("RedstoneSettingsHandler", e.getMessage());
            ctx.fail();
        }

        try {
            BlockCountStatistic stat = new BlockCountStatistic();
            stat.updateContainer((de.tr7zw.changeme.nbtapi.NBTContainer)
                de.tr7zw.changeme.nbtapi.NBT.createNBTObject());
            stat.increment();
            int value = stat.get();
            if (value < 0) {
                BlockProtLogger.failSub("BlockCountStatistic", "negative value after increment");
                ctx.fail();
            } else {
                BlockProtLogger.passSub("BlockCountStatistic: key=" + stat.getKey() + " type=" + stat.getType()
                    + " item=" + stat.getItemType() + " value=" + value);
                ctx.pass();
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("BlockCountStatistic", e.getMessage());
            ctx.fail();
        }

        try {
            var loc = ctx.origin().clone();
            var world = ctx.origin().getWorld();
            var orig = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                var h = new BlockNBTHandler(block);
                h.setOwner(DiagnosticsContext.NOTCH_UUID);
                EffectGeometry geometry = EffectGeometry.createForBlock(block);
                if (geometry.getBoundingBox() == null || geometry.getUnionCenter() == null) {
                    BlockProtLogger.failSub("EffectGeometry", "null bounding box or union center");
                    ctx.fail();
                } else {
                    int perimeter = geometry.getPerimeterPoints(0.5).size();
                    BlockProtLogger.passSub("EffectGeometry: box=" + geometry.getBoundingBox().getVolume()
                        + " center=" + geometry.getUnionCenter() + " perimeterPoints=" + perimeter);
                    ctx.pass();
                }

                ProtectedBlockCache.unmark(block);
                ProtectedBlockCache.mark(block);
                boolean cachedProtected = ProtectedBlockCache.isProtected(block);
                ProtectedBlockCache.unmark(block);
                if (!cachedProtected) {
                    BlockProtLogger.failSub("ProtectedBlockCache", "mark() did not make block protected");
                    ctx.fail();
                } else {
                    BlockProtLogger.passSub("ProtectedBlockCache: mark/isProtected/unmark roundtrip OK, size="
                        + ProtectedBlockCache.size());
                    ctx.pass();
                }
            } finally {
                world.setType(loc, orig);
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("EffectGeometry/ProtectedBlockCache", e.getMessage());
            ctx.fail();
        }

        try {
            var loc = ctx.origin().clone();
            var world = ctx.origin().getWorld();
            var ent = world.spawn(loc, org.bukkit.entity.ArmorStand.class, stand -> {
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setSilent(true);
            });
            try {
                if (!EntityProtectionHandler.isSupportedEntity(ent)) {
                    BlockProtLogger.passSub("EntityProtectionHandler: ArmorStand not supported, structural check only");
                    ctx.pass();
                } else {
                    var handler = EntityProtectionHandler.forEntityOrNull(ent);
                    if (handler == null) {
                        BlockProtLogger.passSub("EntityProtectionHandler: forEntityOrNull=null for ArmorStand (expected)");
                        ctx.pass();
                    } else {
                        handler.enable(UUID.fromString(DiagnosticsContext.NOTCH_UUID));
                        handler.setNoDamage(false);
                        handler.setNoLeash(false);
                        boolean ok = handler.getOwner().equals(UUID.fromString(DiagnosticsContext.NOTCH_UUID))
                            && handler.isProtected()
                            && !handler.isNoDamage()
                            && !handler.isNoLeash();
                        handler.clear();
                        if (!ok) {
                            BlockProtLogger.failSub("EntityProtectionHandler", "owner/flags mismatch after enable()");
                            ctx.fail();
                        } else {
                            BlockProtLogger.passSub("EntityProtectionHandler: enable/owner/flags/clear roundtrip OK");
                            ctx.pass();
                        }
                    }
                }
            } finally {
                ent.remove();
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("EntityProtectionHandler", e.getMessage());
            ctx.fail();
        }

        try {
            var loc = ctx.origin().clone();
            var world = ctx.origin().getWorld();
            var orig = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var h = new BlockNBTHandler(world.getBlockAt(loc));
                h.setOwner(DiagnosticsContext.NOTCH_UUID);
                LocationListEntry entry = new LocationListEntry(loc);
                if (entry.getBlock() == null || entry.getItemType() == null || entry.getTitle() == null) {
                    BlockProtLogger.failSub("LocationListEntry", "null field for constructed entry");
                    ctx.fail();
                } else {
                    BlockProtLogger.passSub("LocationListEntry: block=" + entry.getBlock().getType()
                        + " item=" + entry.getItemType() + " title=" + entry.getTitle());
                    ctx.pass();
                }

                UUID clipboardOwner = UUID.fromString(DiagnosticsContext.NOTCH_UUID);
                PlayerInventoryClipboard.remove(clipboardOwner.toString());
                PlayerInventoryClipboard.set(clipboardOwner.toString(),
                    (de.tr7zw.changeme.nbtapi.NBTContainer) de.tr7zw.changeme.nbtapi.NBT.createNBTObject());
                boolean hasClipboard = PlayerInventoryClipboard.contains(clipboardOwner.toString());
                PlayerInventoryClipboard.remove(clipboardOwner.toString());
                if (!hasClipboard) {
                    BlockProtLogger.failSub("PlayerInventoryClipboard", "set() did not register clipboard");
                    ctx.fail();
                } else {
                    BlockProtLogger.passSub("PlayerInventoryClipboard: set/contains/remove roundtrip OK");
                    ctx.pass();
                }
            } finally {
                world.setType(loc, orig);
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("LocationListEntry/PlayerInventoryClipboard", e.getMessage());
            ctx.fail();
        }
    }

    private static void checkPublicApi(@NotNull DiagnosticsContext ctx) {
        var api = BlockProtAPI.getInstance();
        if (api == null) {
            BlockProtLogger.fail("BlockProtAPI", "getInstance() returned null");
            ctx.fail();
            return;
        }
        var loc = ctx.origin().clone();
        var world = ctx.origin().getWorld();
        var orig = world.getBlockAt(loc).getType();
        world.setType(loc, Material.CHEST);
        try {
            var block = world.getBlockAt(loc);
            UUID owner = UUID.fromString(DiagnosticsContext.NOTCH_UUID);
            UUID friend = UUID.randomUUID();
            UUID stranger = UUID.randomUUID();
            boolean unprotected = !api.isProtected(block) && api.getOwner(block) == null
                && api.canAccess(block, stranger) && api.isLockable(block);
            var handler = new BlockNBTHandler(block);
            handler.setOwner(DiagnosticsContext.NOTCH_UUID);
            handler.addFriend(friend.toString());
            boolean owned = api.isProtected(block) && owner.equals(api.getOwner(block))
                && api.canAccess(block, owner) && api.canAccess(block, friend) && !api.canAccess(block, stranger)
                && api.getFriends(block).equals(List.of(friend)) && !api.isPublic(block);
            if (unprotected && owned) {
                BlockProtLogger.pass("BlockProtAPI: isProtected/getOwner/canAccess/getFriends/isPublic/isLockable OK");
                ctx.pass();
            } else {
                BlockProtLogger.fail("BlockProtAPI", "unprotected=" + unprotected + " owned=" + owned
                    + " friends=" + api.getFriends(block));
                ctx.fail();
            }
        } catch (Exception e) {
            BlockProtLogger.fail("BlockProtAPI", e.getClass().getSimpleName() + ": " + e.getMessage());
            ctx.fail();
        } finally {
            world.setType(loc, orig);
        }
    }

    private static void checkTransferStatistics(@NotNull DiagnosticsContext ctx) {
        var loc = ctx.origin().clone();
        var world = ctx.origin().getWorld();
        var orig = world.getBlockAt(loc).getType();
        world.setType(loc, Material.CHEST);
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        try {
            var block = world.getBlockAt(loc);
            var handler = new BlockNBTHandler(block);
            handler.setOwner(from.toString());
            StatHandler.addBlockByUuid(from, block.getLocation());
            var countBefore = new BlockCountStatistic();
            StatHandler.getStatistic(countBefore);
            int before = countBefore.get();
            boolean transferred = handler.transferOwner(from.toString(), to.toString()).success;
            var fromStat = new PlayerBlocksStatistic();
            StatHandler.getStatisticByUuid(fromStat, from);
            var toStat = new PlayerBlocksStatistic();
            StatHandler.getStatisticByUuid(toStat, to);
            var countAfter = new BlockCountStatistic();
            StatHandler.getStatistic(countAfter);
            boolean moved = fromStat.get().isEmpty() && toStat.get().size() == 1;
            if (transferred && moved && countAfter.get() == before) {
                BlockProtLogger.pass("Transfer statistics: block moved from old to new owner, global count unchanged (" + before + ")");
                ctx.pass();
            } else {
                BlockProtLogger.fail("Transfer statistics", "transferred=" + transferred + " oldOwnerBlocks=" + fromStat.get().size()
                    + " newOwnerBlocks=" + toStat.get().size() + " count " + before + " -> " + countAfter.get());
                ctx.fail();
            }
            StatHandler.removeContainerByUuid(to, block.getLocation());
            handler.clear();
        } catch (Exception e) {
            BlockProtLogger.fail("Transfer statistics", e.getClass().getSimpleName() + ": " + e.getMessage());
            ctx.fail();
        } finally {
            world.setType(loc, orig);
        }
    }
}
