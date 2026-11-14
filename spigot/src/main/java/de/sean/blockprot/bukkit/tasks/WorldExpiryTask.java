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

package de.sean.blockprot.bukkit.tasks;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.listeners.HopperEventListener;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.storage.ProtectedBlockCache;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WorldExpiryTask implements Runnable {

    private static final int BATCH_PER_TICK = 20;

    @Override
    public void run() {
        DefaultConfig cfg = BlockProt.getDefaultConfig();
        if (!cfg.isWorldExpiryEnabled()) return;

        Map<String, String> durations = cfg.getWorldExpiryDurations();
        if (durations.isEmpty()) return;

        for (Map.Entry<String, String> entry : durations.entrySet()) {
            String worldName = entry.getKey();
            String durationStr = entry.getValue();
            if ("0".equals(durationStr) || "-1".equals(durationStr)) continue;

            long durationMs = parseDuration(durationStr);
            if (durationMs <= 0) continue;

            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            scanWorld(world, durationMs);
        }
    }

    private void scanWorld(@NotNull World world, long durationMs) {
        long cutoff = System.currentTimeMillis() - durationMs;

        Chunk[] chunks = world.getLoadedChunks();
        Set<Material> nonTileTypes = new HashSet<>();
        for (BlockFamilyParser.Family f : BlockFamilyParser.Family.values()) {
            if (f == BlockFamilyParser.Family.ENTITIES) continue;
            nonTileTypes.addAll(BlockFamilyParser.getFamilyMembers(f));
        }

        for (Chunk chunk : chunks) {
            Location chunkAnchor = new Location(
                world,
                (chunk.getX() << 4) + 8.0,
                Math.max(world.getMinHeight(), 64),
                (chunk.getZ() << 4) + 8.0
            );
            int chunkX = chunk.getX();
            int chunkZ = chunk.getZ();
            BlockProt.getFoliaLib().getScheduler().runAtLocation(chunkAnchor, task -> {
                if (!world.isChunkLoaded(chunkX, chunkZ)) return;
                Chunk loadedChunk = world.getChunkAt(chunkX, chunkZ);
                scanChunkAndExpire(world, loadedChunk, nonTileTypes, cutoff);
            });
        }
    }

    private void scanChunkAndExpire(@NotNull World world,
                                    @NotNull Chunk chunk,
                                    @NotNull Set<Material> nonTileTypes,
                                    long cutoff) {
        List<Location> expired = new ArrayList<>();

        for (BlockState state : chunk.getTileEntities()) {
            Location loc = state.getLocation();
            if (!ProtectedBlockCache.isProtected(loc.getBlock())) continue;
            try {
                BlockNBTHandler handler = new BlockNBTHandler(loc.getBlock());
                if (handler.isProtected() && handler.getLockedAt() > 0 && handler.getLockedAt() < cutoff) {
                    expired.add(loc.clone());
                }
            } catch (RuntimeException e) {
                BlockProtLogger.log("world-expiry", "Failed scanning tile block at " + loc + ": " + e.getMessage());
            }
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block.getState() instanceof TileState) continue;
                    if (nonTileTypes.contains(block.getType())) {
                        if (!ProtectedBlockCache.isProtected(block)) continue;
                        try {
                            BlockNBTHandler handler = new BlockNBTHandler(block);
                            if (handler.isProtected() && handler.getLockedAt() > 0 && handler.getLockedAt() < cutoff) {
                                expired.add(block.getLocation().clone());
                            }
                        } catch (RuntimeException e) {
                            BlockProtLogger.log("world-expiry", "Failed scanning block at " + block.getLocation() + ": " + e.getMessage());
                        }
                    }
                }
            }
        }

        if (expired.isEmpty()) return;

        for (Location loc : expired) {
            if (loc.getWorld() == null) continue;
            Block block = loc.getBlock();
            try {
                BlockNBTHandler handler = new BlockNBTHandler(block);
                if (!handler.isProtected()) continue;
                String ownerUuid = handler.getOwner();
                handler.clear();
                handler.applyToOtherContainer();
                HopperEventListener.invalidate(block);
                ProtectedBlockCache.unmark(block);
                if (ownerUuid != null && !ownerUuid.isEmpty()) {
                    try {
                        StatHandler.removeContainerByUuid(UUID.fromString(ownerUuid), loc);
                    } catch (IllegalArgumentException ignored) {}
                }
            } catch (RuntimeException e) {
                BlockProtLogger.log("world-expiry", "Failed to clear expired block at " + loc + ": " + e.getMessage());
            }
        }

        BlockProtLogger.log("world-expiry",
            "Expired " + expired.size() + " protections in world " + world.getName());
    }

    private static long parseDuration(@NotNull String input) {
        try {
            String s = input.trim().toLowerCase();
            if (s.endsWith("d")) return Long.parseLong(s.substring(0, s.length() - 1)) * 86400000L;
            if (s.endsWith("h")) return Long.parseLong(s.substring(0, s.length() - 1)) * 3600000L;
            if (s.endsWith("m")) return Long.parseLong(s.substring(0, s.length() - 1)) * 60000L;
            if (s.endsWith("s")) return Long.parseLong(s.substring(0, s.length() - 1)) * 1000L;
        } catch (NumberFormatException ignored) {}
        return -1;
    }
}