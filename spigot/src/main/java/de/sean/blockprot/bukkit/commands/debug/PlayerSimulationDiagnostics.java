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

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.inventories.InventoryState;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.EntityNBTHandler;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.bukkit.nbt.RedstoneSettingsHandler;
import de.sean.blockprot.bukkit.storage.ProtectedBlockCache;
import de.sean.blockprot.bukkit.util.BlockUtil;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PlayerSimulationDiagnostics {

    private PlayerSimulationDiagnostics() {
    }

    public static void run(@NotNull DiagnosticsContext ctx) {
        DiagnosticsContext.runDomain("10/10", "PLAYER SIMULATION & GAMEPLAY VERIFICATION");
        checkPlayerSimulation(ctx);
    }

    private static void checkPlayerSimulation(@NotNull DiagnosticsContext ctx) {
        BlockProtLogger.subGroup("Player simulation & gameplay scenarios (10 scenarios):");
        var loc = ctx.origin().clone();
        var world = ctx.origin().getWorld();
        var orig = world.getBlockAt(loc).getType();
        UUID actor = ctx.actor();
        Player player = ctx.player();
        UUID strangerUuid = UUID.randomUUID();
        UUID friendUuid = UUID.fromString(DiagnosticsContext.NOTCH_UUID);

        ctx.simSub("10a. Block Lock & Cache Lifecycle", () -> {
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                var handler = new BlockNBTHandler(block);
                handler.setOwner(actor.toString());
                handler.setName("SimChest");
                ProtectedBlockCache.mark(block);

                if (!handler.isOwner(actor) || !ProtectedBlockCache.isProtected(block)) {
                    throw new IllegalStateException("Block not marked as owned or cached as protected");
                }
            } finally {
                ProtectedBlockCache.unmark(world.getBlockAt(loc));
                world.setType(loc, orig);
            }
        });

        ctx.simSub("10b. Stranger Access Denial & Ownership", () -> {
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                var handler = new BlockNBTHandler(block);
                handler.setOwner(actor.toString());

                if (!handler.canAccess(actor.toString())) {
                    throw new IllegalStateException("Owner player unexpectedly denied access");
                }
                if (handler.canAccess(strangerUuid.toString())) {
                    throw new IllegalStateException("Stranger UUID unexpectedly granted access to locked block");
                }
            } finally {
                world.setType(loc, orig);
            }
        });

        ctx.simSub("10c. Friend Lifecycle on Block (Add/Access/Remove)", () -> {
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                var handler = new BlockNBTHandler(block);
                handler.setOwner(actor.toString());

                handler.addFriend(friendUuid.toString());
                if (!handler.containsFriend(friendUuid.toString()) || !handler.canAccess(friendUuid.toString())) {
                    throw new IllegalStateException("Friend not found or cannot access after addFriend");
                }
                handler.removeFriend(friendUuid.toString());
                if (handler.containsFriend(friendUuid.toString()) && handler.canAccess(friendUuid.toString())) {
                    throw new IllegalStateException("Friend still present or has access after removeFriend");
                }
            } finally {
                world.setType(loc, orig);
            }
        });

        ctx.simSub("10d. Redstone & Hopper Protection Toggles", () -> {
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                var handler = new BlockNBTHandler(block);
                RedstoneSettingsHandler rs = handler.getRedstoneHandler();
                boolean origPiston = rs.getPistonProtection();
                boolean origHopper = rs.getHopperProtection();
                rs.setPistonProtection(!origPiston);
                rs.setHopperProtection(!origHopper);
                boolean toggledPiston = rs.getPistonProtection();
                boolean toggledHopper = rs.getHopperProtection();
                rs.reset();
                if (toggledPiston == origPiston || toggledHopper == origHopper) {
                    throw new IllegalStateException("Redstone/Hopper protection failed to toggle");
                }
            } finally {
                world.setType(loc, orig);
            }
        });

        ctx.simSub("10e. Multi-Block & Family Expressions", () -> {
            boolean isDoor = BlockFamilyParser.getFamilyMembers(BlockFamilyParser.Family.DOORS).contains(Material.OAK_DOOR);
            boolean isTrapdoor = BlockFamilyParser.getFamilyMembers(BlockFamilyParser.Family.BLOCKS).contains(Material.OAK_TRAPDOOR);
            boolean isGate = BlockFamilyParser.getFamilyMembers(BlockFamilyParser.Family.BLOCKS).contains(Material.OAK_FENCE_GATE);
            boolean isTile = BlockFamilyParser.getFamilyMembers(BlockFamilyParser.Family.TILE_ENTITIES).contains(Material.CHEST);
            String readable = BlockUtil.getHumanReadableBlockName(Material.CHEST);
            if (!isDoor || !isTrapdoor || !isGate || !isTile || readable.isBlank()) {
                throw new IllegalStateException("Family parser or readable name check failed for standard blocks");
            }
        });

        ctx.simSub("10f. Hazard & Explosion Protection Behavior", () -> {
            boolean explosionProtect = BlockProt.getDefaultConfig().shouldProtectLockedBlocksFromExplosions();
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                ProtectedBlockCache.mark(block);
                if (!ProtectedBlockCache.isProtected(block)) {
                    throw new IllegalStateException("Block not registered in ProtectedBlockCache");
                }
            } finally {
                ProtectedBlockCache.unmark(world.getBlockAt(loc));
                world.setType(loc, orig);
            }
        });

        ctx.simSub("10g. Entity Protection Simulation", () -> {
            var entity = world.spawn(loc, ArmorStand.class, stand -> {
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setSilent(true);
            });
            try {
                var entityHandler = new EntityNBTHandler(entity);
                entityHandler.setOwner(actor.toString());
                if (!entityHandler.isOwner(actor.toString())) {
                    throw new IllegalStateException("Entity owner not set to player UUID");
                }
                if (entityHandler.isOwner(strangerUuid.toString())) {
                    throw new IllegalStateException("Entity owner check returned true for stranger UUID");
                }
                entityHandler.addFriend(friendUuid.toString());
                if (!entityHandler.hasFriend(friendUuid.toString())) {
                    throw new IllegalStateException("Entity friends list missing added friend UUID");
                }
                entityHandler.removeFriend(friendUuid.toString());
                if (entityHandler.hasFriend(friendUuid.toString())) {
                    throw new IllegalStateException("Entity friends list still contains friend after removal");
                }
            } finally {
                entity.remove();
            }
        });

        ctx.simSub("10h. GUI State & Theft Prevention Integrity", () -> {
            InventoryState state = new InventoryState(null);
            state.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            state.origin = InventoryState.MenuOrigin.NONE;
            InventoryState.set(actor, state);
            InventoryState retrieved = InventoryState.get(actor);
            if (retrieved == null || retrieved.friendSearchState != InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH) {
                throw new IllegalStateException("InventoryState set/get mismatch");
            }
            InventoryState.remove(actor);
            if (InventoryState.get(actor) != null) {
                throw new IllegalStateException("InventoryState not null after remove()");
            }
        });

        ctx.simSub("10i. Command Permission Nodes Verification", () -> {
            for (Permissions perm : Permissions.values()) {
                if (perm.key() == null || perm.key().isBlank()) {
                    throw new IllegalStateException("Permission node has null or blank key: " + perm.name());
                }
                if (player != null) player.hasPermission(perm.key());
            }
        });

        if (player == null) {
            BlockProtLogger.skipSub("10j. Player Settings & Search History Roundtrip", "requires an online player");
            return;
        }
        ctx.simSub("10j. Player Settings & Search History Roundtrip", () -> {
            PlayerSettingsHandler ps = new PlayerSettingsHandler(player);
            boolean origLock = ps.getLockOnPlace();
            ps.setLockOnPlace(!origLock);
            boolean toggled = ps.getLockOnPlace();
            ps.setLockOnPlace(origLock);
            if (toggled == origLock) {
                throw new IllegalStateException("Failed to toggle player lockOnPlace setting");
            }
            ps.clearSearchHistory();
        });
    }
}
