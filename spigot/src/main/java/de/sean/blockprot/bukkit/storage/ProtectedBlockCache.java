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

package de.sean.blockprot.bukkit.storage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * In-memory set of protected block locations, backed by the block NBT.
 *
 * <p>Keyed by the same packed long used in {@link de.sean.blockprot.bukkit.listeners.HopperEventListener}.
 * It provides an O(1) early-exit in high-frequency event handlers (e.g. {@code InventoryMoveItemEvent}).
 * A location missing from the set is verified against the block NBT and the result is remembered,
 * so locks that survived a restart are protected without depending on the startup population.</p>
 *
 * <p>Thread-safe: all mutations use Caffeine caches and may be called from
 * region/main threads while reads may happen from any thread.</p>
 */
public final class ProtectedBlockCache {

    private static final Cache<Long, UUID> PROTECTED = Caffeine.newBuilder()
        .maximumSize(100_000)
        .build();

    private static final Cache<Long, Boolean> VERIFIED_UNPROTECTED = Caffeine.newBuilder()
        .maximumSize(8192)
        .expireAfterWrite(1, TimeUnit.SECONDS)
        .build();

    private ProtectedBlockCache() {}

    public static void mark(@NotNull Block block) {
        long key = key(block);
        PROTECTED.put(key, block.getWorld().getUID());
        VERIFIED_UNPROTECTED.invalidate(key);
    }

    public static void unmark(@NotNull Block block) {
        long key = key(block);
        PROTECTED.invalidate(key);
        VERIFIED_UNPROTECTED.invalidate(key);
    }

    public static boolean isProtected(@NotNull Block block) {
        long key = key(block);
        if (PROTECTED.getIfPresent(key) != null) return true;
        if (VERIFIED_UNPROTECTED.getIfPresent(key) != null) return false;
        boolean nbtProtected;
        try {
            nbtProtected = BlockProt.getDefaultConfig().isLockable(block.getType(), block.getWorld())
                && new BlockNBTHandler(block).isProtected();
        } catch (RuntimeException e) {
            VERIFIED_UNPROTECTED.put(key, Boolean.TRUE);
            return false;
        }
        if (nbtProtected) PROTECTED.put(key, block.getWorld().getUID());
        else VERIFIED_UNPROTECTED.put(key, Boolean.TRUE);
        return nbtProtected;
    }

    public static void clearWorld(@NotNull UUID worldUid) {
        PROTECTED.asMap().values().removeIf(uid -> uid.equals(worldUid));
    }

    public static void clear() {
        PROTECTED.invalidateAll();
        VERIFIED_UNPROTECTED.invalidateAll();
    }

    public static int size() {
        return PROTECTED.asMap().size();
    }

    private static long key(@NotNull Block block) {
        UUID uid = block.getWorld().getUID();
        long xyz = ((long) block.getX() & 0x3FFFFFFL)
                 | (((long) block.getZ() & 0x3FFFFFFL) << 26)
                 | (((long) (block.getY() + 2048) & 0xFFFL) << 52);
        long k = xyz ^ (uid.getMostSignificantBits() * 0x9e3779b97f4a7c15L);
        k ^= uid.getLeastSignificantBits() * 0x6c62272e07bb0142L;
        k ^= k >>> 33;
        k *= 0xff51afd7ed558ccdL;
        k ^= k >>> 33;
        k *= 0xc4ceb9fe1a85ec53L;
        k ^= k >>> 33;
        return k;
    }
}