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
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Prevents entities (endermen, falling blocks) from
 * interacting with or bypassing protection on locked blocks.
 */
public final class EntityEventListener implements Listener {
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(@NotNull final EntityChangeBlockEvent event) {
        if (BlockProt.getDefaultConfig().isWorldExcluded(event.getBlock().getWorld())) return;
        Material blockType = event.getBlock().getType();
        Entity entity = event.getEntity();
        if (blockType == Material.AIR || blockType == Material.CAVE_AIR || blockType == Material.VOID_AIR) {
            if (entity instanceof FallingBlock) {
                Block below = event.getBlock().getRelative(BlockFace.DOWN);
                if (de.sean.blockprot.bukkit.storage.ProtectedBlockCache.isProtected(below)) {
                    event.setCancelled(true);
                }
            }
            return;
        }
        org.bukkit.World world = event.getBlock().getWorld();
        if (entity instanceof FallingBlock) {
            Material mat = ((FallingBlock) entity).getBlockData().getMaterial();

            if (mat.toString().contains("ANVIL") &&
                BlockProt.getDefaultConfig().isLockableBlock(mat, world)) {
                if (BlockProt.getDefaultConfig().isLockable(blockType, world)) {
                    BlockNBTHandler handler = new BlockNBTHandler(event.getBlock());
                    if (handler.isProtected()) event.setCancelled(true);
                }
            }
        } else if (entity instanceof Enderman) {
            if (BlockProt.getDefaultConfig().isLockable(blockType, world)) {
                BlockNBTHandler handler = new BlockNBTHandler(event.getBlock());
                if (handler.isProtected()) event.setCancelled(true);
            }
        } else if (BlockProt.getDefaultConfig().isLockable(blockType, world)) {
            BlockNBTHandler handler = new BlockNBTHandler(event.getBlock());
            if (handler.isProtected()) event.setCancelled(true);
        }
    }
}