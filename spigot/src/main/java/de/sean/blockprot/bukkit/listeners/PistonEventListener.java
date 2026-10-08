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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

/**
 * Blocks piston extends/retracts from destroying or moving protected blocks.
 */
public class PistonEventListener implements Listener {
    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (BlockProt.getDefaultConfig().isWorldExcluded(event.getBlock().getWorld())) {
            return;
        }

        for (Block block : event.getBlocks()) {
            Block target = block.getRelative(event.getDirection());
            if (ProtectedBlockCache.isProtected(target)
                    && BlockProt.getDefaultConfig().isLockable(target.getType(), target.getWorld())) {
                BlockNBTHandler targetNbt = new BlockNBTHandler(target);
                if (targetNbt.isProtected()) {
                    event.setCancelled(true);
                    return;
                }
            }

            if (!ProtectedBlockCache.isProtected(block)) continue;

            if (BlockProt.getDefaultConfig().isLockableShulkerBox(block.getType(), block.getWorld())) {
                // Shulker boxes drop right away, so they will never be pushable if locked.
                if (new BlockNBTHandler(block).isProtected()) {
                    event.setCancelled(true);
                    return;
                }
            } else if (BlockProt.getDefaultConfig().isLockable(block.getType(), block.getWorld())) {
                BlockNBTHandler nbtHandler = new BlockNBTHandler(block);
                if (nbtHandler.isProtected()
                        && (BlockProt.getDefaultConfig().shouldBlockProtectedBlockPistonMovement()
                        || nbtHandler.getRedstoneHandler().getPistonProtection())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!event.isSticky()) return;
        if (BlockProt.getDefaultConfig().isWorldExcluded(event.getBlock().getWorld())) {
            return;
        }
        for (Block block : event.getBlocks()) {
            if (!ProtectedBlockCache.isProtected(block)) continue;

            if (BlockProt.getDefaultConfig().isLockableShulkerBox(block.getType(), block.getWorld())) {
                if (new BlockNBTHandler(block).isProtected()) {
                    event.setCancelled(true);
                    return;
                }
            } else if (BlockProt.getDefaultConfig().isLockable(block.getType(), block.getWorld())) {
                BlockNBTHandler nbtHandler = new BlockNBTHandler(block);
                if (nbtHandler.isProtected()
                        && (BlockProt.getDefaultConfig().shouldBlockProtectedBlockPistonMovement()
                        || nbtHandler.getRedstoneHandler().getPistonProtection())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}