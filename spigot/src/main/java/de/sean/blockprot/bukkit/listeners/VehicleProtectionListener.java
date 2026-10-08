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
import de.sean.blockprot.bukkit.audit.AuditLogger;
import de.sean.blockprot.bukkit.dialogs.BlockLockDialog;
import de.sean.blockprot.bukkit.inventories.BlockLockInventory;
import de.sean.blockprot.bukkit.inventories.InventoryState;
import de.sean.blockprot.bukkit.nbt.EntityNBTHandler;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles protection for chest boats and storage / hopper minecarts.
 *
 * <p>Only entities whose material is listed in {@code lockable_entities} in blocks.yml
 * are eligible for protection. If the list is empty, no vehicles are protected.</p>
 *
 * <p>Sneaking + right-click with empty hand opens the BlockProt protection menu.
 * Once protected the inventory is inaccessible to non-owners. Destruction of
 * the vehicle by non-owners is also blocked.</p>
 *
 * <p>Hopper-pipeline extraction is blocked unless the owner has disabled that
 * protection via the in-menu toggle.</p>
 *
 * <p>Auto-lock on place: if the player's "lock on place" setting is enabled
 * and they are not sneaking, the entity is automatically locked to them the
 * moment they place it (via {@link EntityPlaceEvent}).</p>
 */
public final class VehicleProtectionListener implements Listener {

    /**
     * ChestBoat moved to {@code org.bukkit.entity.boat.ChestBoat} in 1.21.
     * Resolved once at class-load time via reflection.
     */
    private static final Class<?> CHEST_BOAT_CLASS = resolveChestBoatClass();

    private static Class<?> resolveChestBoatClass() {
        try { return Class.forName("org.bukkit.entity.boat.ChestBoat"); } catch (ClassNotFoundException ignored) {}
        try { return Class.forName("org.bukkit.entity.ChestBoat");      } catch (ClassNotFoundException ignored) {}
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehiclePlace(@NotNull EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (!isProtectedVehicleType(entity)) return;

        Player player = event.getPlayer();
        if (player == null) return;
        if (BlockProt.getDefaultConfig().isWorldExcluded(player.getWorld())) return;
        if (!player.hasPermission(Permissions.USER.key())) return;

        if (!new PlayerSettingsHandler(player).getLockOnPlace()) return;
        if (player.isSneaking()) return;

        EntityNBTHandler handler = new EntityNBTHandler(entity);
        if (handler.isProtected()) return;

        handler.setOwner(player.getUniqueId().toString());
        ComponentMessages.sendActionBar(player, LegacyComponentSerializer.legacySection().deserialize(
            Translator.get(TranslationKey.MESSAGES__LOCK_ON_PLACE_SUCCESS)));
        BlockProtLogger.log("entity-protection", "AUTO-LOCKED "
            + entity.getType().name() + " entity=" + entity.getUniqueId()
            + " owner=" + player.getName());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleInteract(@NotNull PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!isProtectedVehicleType(entity)) return;

        Player player = event.getPlayer();
        if (BlockProt.getDefaultConfig().isWorldExcluded(player.getWorld())) return;

        EntityNBTHandler handler = new EntityNBTHandler(entity);

        if (player.isSneaking()) {
            var mainHand = player.getInventory().getItemInMainHand();
            if (!mainHand.getType().isAir()) return;

            event.setCancelled(true);

            if (!player.hasPermission(Permissions.USER.key())) {
                sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                return;
            }

            if (handler.isProtected() && !handler.isManager(player.getUniqueId().toString())
                    && !player.hasPermission(Permissions.USER_ADMIN.key())) {
                sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                AuditLogger audit = BlockProt.getAuditLogger();
                if (audit != null) {
                    audit.log(player.getUniqueId(), player.getName(),
                        entity.getLocation(), AuditLogger.Action.ACCESS_DENIED);
                }
                BlockProtLogger.log("entity-protection", "ACCESS_DENIED vehicle menu open: "
                    + entity.getType().name() + " entity=" + entity.getUniqueId()
                    + " player=" + player.getName());
                return;
            }

            if (BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
                BlockLockDialog.showForEntity(player, entity, handler);
            } else {
                InventoryState state = InventoryState.getOrCreate(player.getUniqueId());
                state.entityUUID = entity.getUniqueId();

                var inv = new BlockLockInventory().fillForEntity(player, entity, handler);
                if (inv == null) {
                    sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                    return;
                }
                player.openInventory(inv);
            }
        } else {
            if (!handler.isProtected()) return;
            if (handler.canAccess(player.getUniqueId().toString())
                    || player.hasPermission(Permissions.USER_ADMIN.key())) return;

            event.setCancelled(true);
            sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
            AuditLogger audit = BlockProt.getAuditLogger();
            if (audit != null) {
                audit.log(player.getUniqueId(), player.getName(),
                    entity.getLocation(), AuditLogger.Action.ACCESS_DENIED);
            }
            BlockProtLogger.log("entity-protection", "ACCESS_DENIED interact: "
                + entity.getType().name() + " entity=" + entity.getUniqueId()
                + " player=" + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHopperPullFromVehicle(@NotNull InventoryMoveItemEvent event) {
        InventoryHolder sourceHolder = event.getSource().getHolder();
        if (!(sourceHolder instanceof Entity entity)) return;
        if (!isProtectedVehicleType(entity)) return;

        if (entity.getWorld() != null
                && BlockProt.getDefaultConfig().isWorldExcluded(entity.getWorld())) return;

        EntityNBTHandler handler = new EntityNBTHandler(entity);
        if (!handler.isProtected()) return;
        if (!handler.isHopperProtectionEnabled()) return;
        if (BlockProt.getDefaultConfig().isSimplifiedHopperLogic()) return;

        event.setCancelled(true);
        BlockProtLogger.log("entity-protection", "BLOCKED hopper extraction from "
            + entity.getType().name() + " entity=" + entity.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHopperPushToVehicle(@NotNull InventoryMoveItemEvent event) {
        InventoryHolder destHolder = event.getDestination().getHolder();
        if (!(destHolder instanceof Entity entity)) return;
        if (!isProtectedVehicleType(entity)) return;

        if (entity.getWorld() != null
                && BlockProt.getDefaultConfig().isWorldExcluded(entity.getWorld())) return;

        EntityNBTHandler handler = new EntityNBTHandler(entity);
        if (!handler.isProtected()) return;
        if (!handler.isHopperProtectionEnabled()) return;
        if (BlockProt.getDefaultConfig().isSimplifiedHopperLogic()) return;

        event.setCancelled(true);
        BlockProtLogger.log("entity-protection", "BLOCKED hopper insertion into "
            + entity.getType().name() + " entity=" + entity.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleDamage(@NotNull VehicleDamageEvent event) {
        Entity vehicle = event.getVehicle();
        if (!isProtectedVehicleType(vehicle)) return;
        EntityNBTHandler handler = new EntityNBTHandler(vehicle);
        if (!handler.isProtected()) return;
        if (!canModifyVehicle(event.getAttacker(), vehicle, handler)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleDestroy(@NotNull VehicleDestroyEvent event) {
        Entity vehicle = event.getVehicle();
        if (!isProtectedVehicleType(vehicle)) return;
        EntityNBTHandler handler = new EntityNBTHandler(vehicle);
        if (!handler.isProtected()) return;
        if (!canModifyVehicle(event.getAttacker(), vehicle, handler)) {
            event.setCancelled(true);
        }
    }

    private boolean isProtectedVehicleType(@NotNull Entity entity) {
        Material mat = resolveVehicleMaterial(entity);
        if (mat == null) return false;
        return BlockProt.getDefaultConfig().isLockableEntity(mat);
    }

    @Nullable
    private Material resolveVehicleMaterial(@NotNull Entity entity) {
        if (CHEST_BOAT_CLASS != null && CHEST_BOAT_CLASS.isInstance(entity)) {
            try {
                return Material.matchMaterial(entity.getType().name());
            } catch (Exception ignored) {
                return Material.matchMaterial("CHEST_BOAT");
            }
        }
        if (entity instanceof StorageMinecart) return Material.CHEST_MINECART;
        if (entity instanceof HopperMinecart)  return Material.HOPPER_MINECART;
        return null;
    }

    private boolean canModifyVehicle(@Nullable Entity attacker,
                                     @NotNull Entity vehicle,
                                     @NotNull EntityNBTHandler handler) {
        if (attacker == null) return false;

        Player player = resolvePlayer(attacker);
        if (player == null) {
            BlockProtLogger.log("entity-protection", "BLOCKED non-player modification by "
                + attacker.getType().name() + " on " + vehicle.getType().name()
                + " entity=" + vehicle.getUniqueId());
            return false;
        }

        String playerUuid = player.getUniqueId().toString();
        if (handler.isOwner(playerUuid) || player.hasPermission(Permissions.USER_ADMIN.key())) {
            return true;
        }

        sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
        AuditLogger audit = BlockProt.getAuditLogger();
        if (audit != null) {
            audit.log(player.getUniqueId(), player.getName(),
                vehicle.getLocation(), AuditLogger.Action.ACCESS_DENIED);
        }
        BlockProtLogger.log("entity-protection", "ACCESS_DENIED break/damage by "
            + player.getName() + " on " + vehicle.getType().name()
            + " entity=" + vehicle.getUniqueId());
        return false;
    }

    @Nullable
    private Player resolvePlayer(@NotNull Entity attacker) {
        if (attacker instanceof Player p) return p;
        if (attacker instanceof org.bukkit.entity.Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player p) return p;
        }
        return null;
    }

    private void sendActionBar(@NotNull Player player, @NotNull String text) {
        de.sean.blockprot.bukkit.util.TemporaryActionBar.show(player, text);
    }
}