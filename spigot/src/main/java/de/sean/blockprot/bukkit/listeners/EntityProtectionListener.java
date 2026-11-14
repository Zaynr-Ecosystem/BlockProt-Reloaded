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
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.admin.AdminAction;
import de.sean.blockprot.bukkit.admin.AdminTierManager;
import de.sean.blockprot.bukkit.entities.EntityProtectionHandler;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Handles all protection logic for tamed entities (pets).
 *
 * <p>Protected events:
 * <ul>
 *   <li>Damage: from players or their projectiles (when no_damage is ON)</li>
 *   <li>Interact: right-click, feeding, naming (when no_interact is ON)</li>
 *   <li>Leash / Unleash: attach or remove leads (when no_leash is ON)</li>
 *   <li>Pickup: parrot shoulder pickup (when no_pickup is ON)</li>
 *   <li>Death message: notifies owner when a protected entity dies</li>
 *   <li>Auto-protect on tame: when global setting is enabled in config</li>
 * </ul>
 *
 * <p>Players with {@code blockprot.admin} or {@code blockprot.bypass} always bypass protection.
 *
 * <p>Renamed from {@code PetProtectionListener} as part of the pet -> entity protection
 * rename. Behaviour is unchanged.
 */
public final class EntityProtectionListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(@NotNull EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player player)) return;
        if (!BlockProt.getDefaultConfig().isEntityProtectionEnabled()) return;
        if (!BlockProt.getDefaultConfig().isEntityProtectionAutoProtectOnTame()) return;

        EntityProtectionHandler handler = EntityProtectionHandler.forEntityOrNull(event.getEntity());
        if (handler == null) return;
        handler.enable(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(@NotNull EntityDamageByEntityEvent event) {
        if (!BlockProt.getDefaultConfig().isEntityProtectionEnabled()) return;

        Entity victim = event.getEntity();
        EntityProtectionHandler handler = EntityProtectionHandler.forEntityOrNull(victim);
        if (handler == null || !handler.isProtected() || !handler.isNoDamage()) return;

        Player attacker = resolvePlayer(event.getDamager());
        if (attacker == null) {
            if (isPetAttack(event.getDamager())) {
                return;
            }
            event.setCancelled(true);
            return;
        }
        if (isBypassing(attacker, handler)) return;

        event.setCancelled(true);
        attacker.sendMessage(BlockProt.getDefaultConfig().getEntityProtectionDeniedMessage());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(@NotNull PlayerInteractEntityEvent event) {
        if (!BlockProt.getDefaultConfig().isEntityProtectionEnabled()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        EntityProtectionHandler handler = EntityProtectionHandler.forEntityOrNull(event.getRightClicked());
        if (handler == null || !handler.isProtected() || !handler.isNoInteract()) return;

        Player player = event.getPlayer();
        if (isBypassing(player, handler)) return;

        event.setCancelled(true);
        player.sendMessage(BlockProt.getDefaultConfig().getEntityProtectionDeniedMessage());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLeash(@NotNull PlayerLeashEntityEvent event) {
        if (!BlockProt.getDefaultConfig().isEntityProtectionEnabled()) return;

        EntityProtectionHandler handler = EntityProtectionHandler.forEntityOrNull(event.getEntity());
        if (handler == null || !handler.isProtected() || !handler.isNoLeash()) return;

        Player player = event.getPlayer();
        if (isBypassing(player, handler)) return;

        event.setCancelled(true);
        player.sendMessage(BlockProt.getDefaultConfig().getEntityProtectionDeniedMessage());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUnleash(@NotNull PlayerUnleashEntityEvent event) {
        if (!BlockProt.getDefaultConfig().isEntityProtectionEnabled()) return;

        EntityProtectionHandler handler = EntityProtectionHandler.forEntityOrNull(event.getEntity());
        if (handler == null || !handler.isProtected() || !handler.isNoLeash()) return;

        Player player = event.getPlayer();
        if (isBypassing(player, handler)) return;

        event.setCancelled(true);
        player.sendMessage(BlockProt.getDefaultConfig().getEntityProtectionDeniedMessage());
    }

    // Parrot-on-shoulder is triggered by a PlayerInteractEntityEvent on the parrot,
    // so the no_interact flag already covers that case. The no_pickup flag acts as
    // a standalone guard in case no_interact is OFF but the owner still wants to
    // prevent others from wearing their parrot.

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onParrotPickup(@NotNull PlayerInteractEntityEvent event) {
        if (!BlockProt.getDefaultConfig().isEntityProtectionEnabled()) return;
        if (!(event.getRightClicked() instanceof Parrot)) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        EntityProtectionHandler handler = EntityProtectionHandler.forEntityOrNull(event.getRightClicked());
        if (handler == null || !handler.isProtected() || !handler.isNoPickup()) return;

        Player player = event.getPlayer();
        if (isBypassing(player, handler)) return;

        event.setCancelled(true);
        player.sendMessage(BlockProt.getDefaultConfig().getEntityProtectionDeniedMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityProtectionDeath(@NotNull EntityDeathEvent event) {
        if (!BlockProt.getDefaultConfig().isEntityProtectionEnabled()) return;

        LivingEntity dead = event.getEntity();
        EntityProtectionHandler handler = EntityProtectionHandler.forEntityOrNull(dead);
        if (handler == null || !handler.isProtected()) return;

        UUID ownerUuid = handler.getOwner();
        if (ownerUuid == null) return;

        Player owner = dead.getServer().getPlayer(ownerUuid);
        if (owner == null || !owner.isOnline()) return;

        String entityName = dead.customName() != null
            ? net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(dead.customName())
            : dead.getType().name();
        String message = Translator.get(TranslationKey.MESSAGES__ENTITY_DEATH_NOTIFICATION).replace("{pet_name}", entityName);
        owner.sendMessage(message);
    }

    @Nullable
    private Player resolvePlayer(@NotNull Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj) {
            ProjectileSource source = proj.getShooter();
            if (source instanceof Player p) return p;
        }
        // Wolf or other pet attacking on behalf of a player: intentionally ignored;
        // pet-on-pet PvP is considered vanilla and not blocked by default.
        return null;
    }

    private boolean isPetAttack(@NotNull Entity damager) {
        if (damager instanceof Tameable tamed && tamed.isTamed()) {
            return true;
        }
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Tameable tamed && tamed.isTamed()) {
            return true;
        }
        return false;
    }

    private boolean isBypassing(@NotNull Player player, @NotNull EntityProtectionHandler handler) {
        return handler.isOwner(player.getUniqueId())
            || player.hasPermission(Permissions.USER_ADMIN.key())
            || AdminTierManager.hasPermission(player, AdminAction.CONTAINER_BYPASS);
    }
}