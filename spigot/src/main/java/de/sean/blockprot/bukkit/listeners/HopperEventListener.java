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

package de.sean.blockprot.bukkit.listeners;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.storage.ProtectedBlockCache;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Caches hopper access checks per block to avoid repeated NBT reads.
 */
public class HopperEventListener implements Listener {

    /**
     * Caffeine cache keyed by packed world+block coords. TTL-based eviction replaces
     * the previous manual ConcurrentHashMap + periodic purge task.
     */
    private record CacheEntry(boolean isProtected, String owner, boolean hopperProtection) {}

    private static final Cache<Long, CacheEntry> cache = Caffeine.newBuilder()
        .maximumSize(2048)
        .expireAfterWrite(1, TimeUnit.SECONDS)
        .build();

    /**
     * Encodes world UID + block coords into a single long cache key.
     *
     * <p>Previous implementation used XOR with fixed shifts which produced hash
     * collisions for blocks whose (x, z) coordinates are symmetric (e.g. (1,2) and
     * (2,1) produced the same key). This version uses a Cantor/FNV-inspired mix that
     * distributes all three axes and the world UID independently.
     *
     * <p>Formula: mix the 64-bit world UID with packed (x, y, z) using multiply-xor
     * steps borrowed from MurmurHash3's finalizer. Safe for all valid Minecraft
     * coordinate ranges (x/z ±30 000 000, y ±2048).
     */
    private static long cacheKey(@NotNull Block block) {
        UUID uid = block.getWorld().getUID();
        long xyz = ((long) block.getX() & 0x3FFFFFFL)
                 | (((long) block.getZ() & 0x3FFFFFFL) << 26)
                 | (((long) (block.getY() + 2048) & 0xFFFL) << 52);
        long key = xyz ^ (uid.getMostSignificantBits() * 0x9e3779b97f4a7c15L);
        key ^= uid.getLeastSignificantBits() * 0x6c62272e07bb0142L;
        key ^= key >>> 33;
        key *= 0xff51afd7ed558ccdL;
        key ^= key >>> 33;
        key *= 0xc4ceb9fe1a85ec53L;
        key ^= key >>> 33;
        return key;
    }

    /**
     * Returns a cache entry for the given block, reading NBT only on cache miss or expiry.
     * Must be called from the server main thread (NBT access is not thread-safe).
     */
    @Nullable
    private CacheEntry getEntry(@Nullable Block block) {
        if (block == null) return null;
        long key = cacheKey(block);
        CacheEntry entry = cache.getIfPresent(key);
        if (entry != null) return entry;

        try {
            BlockNBTHandler handler = new BlockNBTHandler(block);
            entry = new CacheEntry(
                handler.isProtected(),
                handler.getOwner(),
                handler.getRedstoneHandler().getHopperProtection()
            );
            cache.put(key, entry);
            return entry;
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static void invalidate(@NotNull Block block) {
        cache.invalidate(cacheKey(block));
    }

    @EventHandler
    public void onItemMove(InventoryMoveItemEvent event) {
        InventoryHolder sourceHolder = event.getSource().getHolder();
        InventoryHolder destHolder = event.getDestination().getHolder();
        if (sourceHolder == null && destHolder == null) return;
        if (BlockProt.getDefaultConfig().isSimplifiedHopperLogic()) return;

        // Check source container (preventing unauthorized extraction / siphoning)
        if (sourceHolder != null && !BlockProt.getDefaultConfig().isWorldExcluded(sourceHolder)) {
            Block source = getBlock(sourceHolder);
            if (source != null && ProtectedBlockCache.isProtected(source)
                    && BlockProt.getDefaultConfig().isLockable(source.getType(), source.getWorld())) {
                CacheEntry sourceEntry = getEntry(source);
                if (sourceEntry != null && sourceEntry.isProtected() && sourceEntry.hopperProtection()) {
                    if (!isMatchingOwner(destHolder, sourceEntry.owner())) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        // Check destination container (preventing unauthorized insertion into protected blocks)
        if (destHolder != null && !BlockProt.getDefaultConfig().isWorldExcluded(destHolder)) {
            Block dest = getBlock(destHolder);
            if (dest != null && ProtectedBlockCache.isProtected(dest)
                    && BlockProt.getDefaultConfig().isLockable(dest.getType(), dest.getWorld())) {
                CacheEntry destEntry = getEntry(dest);
                if (destEntry != null && destEntry.isProtected() && destEntry.hopperProtection()) {
                    if (!isMatchingOwner(sourceHolder, destEntry.owner())) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    private boolean isMatchingOwner(@Nullable InventoryHolder holder, @NotNull String ownerUuid) {
        if (holder == null) return false;
        Block block = getBlock(holder);
        if (block != null && BlockProt.getDefaultConfig().isLockable(block.getType(), block.getWorld())) {
            CacheEntry entry = getEntry(block);
            return entry != null && entry.isProtected() && entry.owner().equals(ownerUuid);
        }
        return false;
    }

    @Nullable
    private Block getBlock(InventoryHolder holder) {
        if (holder instanceof Container) return ((Container) holder).getBlock();
        else if (holder instanceof DoubleChest doubleChest) {
            if (doubleChest.getWorld() == null) return null;
            return doubleChest.getWorld().getBlockAt(doubleChest.getLocation());
        } else return null;
    }
}