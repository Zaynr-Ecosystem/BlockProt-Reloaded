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
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Protects villagers that are linked to a protected workstation.
 */
public final class VillagerWorkstationProtectionListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVillagerDamage(@NotNull EntityDamageByEntityEvent event) {
        if (!BlockProt.getDefaultConfig().isVillagerWorkstationProtectionEnabled()) return;
        if (!(event.getEntity() instanceof Villager villager)) return;
        Player attacker = resolvePlayer(event.getDamager());
        if (attacker == null || de.sean.blockprot.bukkit.admin.AdminTierManager.hasPermission(attacker, de.sean.blockprot.bukkit.admin.AdminAction.CONTAINER_BYPASS)) return;

        Block workstation = getProtectedLinkedWorkstation(villager);
        if (workstation == null) return;
        if (canAccess(attacker, workstation)) return;

        event.setCancelled(true);
        de.sean.blockprot.bukkit.util.TemporaryActionBar.show(attacker, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION), BlockProt.getDefaultConfig().getActionBarDurationTicks());
        BlockProtLogger.log("entity-protection", "ACCESS_DENIED villager damage: villager="
            + villager.getUniqueId() + " workstation=" + locString(workstation.getLocation())
            + " player=" + attacker.getName());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVillagerInteract(@NotNull PlayerInteractEntityEvent event) {
        if (!BlockProt.getDefaultConfig().isVillagerWorkstationProtectionEnabled()) return;
        if (!(event.getRightClicked() instanceof Villager villager)) return;
        Player player = event.getPlayer();
        if (de.sean.blockprot.bukkit.admin.AdminTierManager.hasPermission(player, de.sean.blockprot.bukkit.admin.AdminAction.CONTAINER_BYPASS)) return;

        Block workstation = getProtectedLinkedWorkstation(villager);
        if (workstation == null) return;
        if (canAccess(player, workstation)) return;

        event.setCancelled(true);
        de.sean.blockprot.bukkit.util.TemporaryActionBar.show(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION), BlockProt.getDefaultConfig().getActionBarDurationTicks());
        BlockProtLogger.log("entity-protection", "ACCESS_DENIED villager interact: villager="
            + villager.getUniqueId() + " workstation=" + locString(workstation.getLocation())
            + " player=" + player.getName());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreakNearWorkstation(@NotNull BlockBreakEvent event) {
        if (!BlockProt.getDefaultConfig().isVillagerWorkstationProtectionEnabled()) return;
        Player player = event.getPlayer();
        if (de.sean.blockprot.bukkit.admin.AdminTierManager.hasPermission(player, de.sean.blockprot.bukkit.admin.AdminAction.BREAK)) return;

        Block workstation = findNearbyProtectedWorkstation(event.getBlock());
        if (workstation == null) return;

        if (!canAccess(player, workstation)) {
            event.setCancelled(true);
            de.sean.blockprot.bukkit.util.TemporaryActionBar.show(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION), BlockProt.getDefaultConfig().getActionBarDurationTicks());
            BlockProtLogger.log("entity-protection", "ACCESS_DENIED block break near workstation: block="
                + event.getBlock().getType().name() + " location=" + locString(event.getBlock().getLocation())
                + " workstation=" + locString(workstation.getLocation()) + " player=" + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockInteractNearWorkstation(@NotNull PlayerInteractEvent event) {
        if (!BlockProt.getDefaultConfig().isVillagerWorkstationProtectionEnabled()) return;
        if (event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        if (de.sean.blockprot.bukkit.admin.AdminTierManager.hasPermission(player, de.sean.blockprot.bukkit.admin.AdminAction.CONTAINER_BYPASS)) return;

        Block workstation = findNearbyProtectedWorkstation(event.getClickedBlock());
        if (workstation == null) return;

        if (workstation.equals(event.getClickedBlock())) return;

        if (!canAccess(player, workstation)) {
            event.setCancelled(true);
            de.sean.blockprot.bukkit.util.TemporaryActionBar.show(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION), BlockProt.getDefaultConfig().getActionBarDurationTicks());
            BlockProtLogger.log("entity-protection", "ACCESS_DENIED block interact near workstation: block="
                + event.getClickedBlock().getType().name() + " location=" + locString(event.getClickedBlock().getLocation())
                + " workstation=" + locString(workstation.getLocation()) + " player=" + player.getName());
        }
    }

    @Nullable
    private static Block findNearbyProtectedWorkstation(@NotNull Block block) {
        int radius   = BlockProt.getDefaultConfig().getVillagerWorkstationProtectionRadius();
        int vRadius  = BlockProt.getDefaultConfig().getVillagerWorkstationProtectionVerticalRadius();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int bx = block.getX() + dx;
                int bz = block.getZ() + dz;
                if (!block.getWorld().isChunkLoaded(bx >> 4, bz >> 4)) continue;
                for (int dy = -vRadius; dy <= vRadius; dy++) {
                    Block b = block.getRelative(dx, dy, dz);
                    if (BlockProt.hasFoliaLib()
                        && !BlockProt.getFoliaLib().getScheduler().isOwnedByCurrentRegion(b.getLocation())) {
                        continue;
                    }
                    if (isWorkstation(b.getType())) {
                        try {
                            BlockNBTHandler handler = new BlockNBTHandler(b);
                            if (handler.isProtected()) {
                                return b;
                            }
                        } catch (RuntimeException ignored) {}
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    public static Villager findLinkedVillager(@NotNull Block workstation) {
        for (Entity entity : workstation.getWorld().getNearbyEntities(workstation.getLocation().add(0.5, 0.5, 0.5), 16, 8, 16)) {
            if (!(entity instanceof Villager villager)) continue;
            Location jobSite = getJobSite(villager);
            if (jobSite != null && sameBlock(jobSite, workstation.getLocation())) return villager;
        }
        return null;
    }

    @Nullable
    private static Block getProtectedLinkedWorkstation(@NotNull Villager villager) {
        Location jobSite = getJobSite(villager);
        if (jobSite == null || jobSite.getWorld() == null) return null;
        if (!jobSite.getWorld().isChunkLoaded(jobSite.getBlockX() >> 4, jobSite.getBlockZ() >> 4)) return null;
        if (BlockProt.hasFoliaLib() && !BlockProt.getFoliaLib().getScheduler().isOwnedByCurrentRegion(jobSite)) {
            return null;
        }
        Block block = jobSite.getBlock();
        if (!isWorkstation(block.getType())) return null;
        if (!isWithinProtectedArea(villager.getLocation(), block.getLocation())) return null;
        try {
            BlockNBTHandler handler = new BlockNBTHandler(block);
            return handler.isProtected() ? block : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static Location getJobSite(@NotNull Villager villager) {
        try {
            Class<?> memoryKey = Class.forName("org.bukkit.entity.memory.MemoryKey");
            Object jobSiteKey = memoryKey.getField("JOB_SITE").get(null);
            Method getMemory = villager.getClass().getMethod("getMemory", memoryKey);
            Object value = getMemory.invoke(villager, jobSiteKey);
            return value instanceof Location loc ? loc : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static boolean canAccess(@NotNull Player player, @NotNull Block workstation) {
        try {
            BlockNBTHandler handler = new BlockNBTHandler(workstation);
            return handler.canAccess(player.getUniqueId().toString());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isWithinProtectedArea(@NotNull Location villager, @NotNull Location workstation) {
        if (villager.getWorld() == null || workstation.getWorld() == null
                || !villager.getWorld().equals(workstation.getWorld())) return false;
        int radius  = BlockProt.getDefaultConfig().getVillagerWorkstationProtectionRadius();
        int vRadius = BlockProt.getDefaultConfig().getVillagerWorkstationProtectionVerticalRadius();
        return Math.abs(villager.getBlockX() - workstation.getBlockX()) <= radius
            && Math.abs(villager.getBlockZ() - workstation.getBlockZ()) <= radius
            && Math.abs(villager.getBlockY() - workstation.getBlockY()) <= vRadius;
    }

    private static boolean isWorkstation(@NotNull Material material) {
        String name = material.name();
        return name.equals("GRINDSTONE") || name.equals("STONECUTTER") || name.equals("LOOM")
            || name.equals("CARTOGRAPHY_TABLE") || name.equals("SMITHING_TABLE")
            || name.equals("ENCHANTING_TABLE") || name.equals("FLETCHING_TABLE")
            || name.equals("LECTERN") || name.equals("COMPOSTER") || name.equals("BREWING_STAND")
            || name.equals("BLAST_FURNACE") || name.equals("SMOKER") || name.equals("BARREL")
            || name.equals("CAULDRON");
    }

    private static boolean sameBlock(@NotNull Location a, @NotNull Location b) {
        return a.getWorld() != null && b.getWorld() != null && a.getWorld().equals(b.getWorld())
            && a.getBlockX() == b.getBlockX()
            && a.getBlockY() == b.getBlockY()
            && a.getBlockZ() == b.getBlockZ();
    }

    @Nullable
    private Player resolvePlayer(@NotNull Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof org.bukkit.entity.Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player p) return p;
        }
        return null;
    }

    @NotNull
    private static String locString(@NotNull Location loc) {
        String world = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";
        return world + " [" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + "]";
    }
}