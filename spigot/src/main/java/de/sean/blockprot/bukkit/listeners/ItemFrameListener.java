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
import de.sean.blockprot.bukkit.admin.AdminAction;
import de.sean.blockprot.bukkit.admin.AdminTierManager;
import de.sean.blockprot.bukkit.audit.AuditLogger;
import de.sean.blockprot.bukkit.events.BlockAccessMenuEvent;
import de.sean.blockprot.bukkit.dialogs.BlockLockDialog;
import de.sean.blockprot.bukkit.inventories.BlockLockInventory;
import de.sean.blockprot.bukkit.inventories.InventoryState;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.EntityNBTHandler;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles protection for Item Frames (normal and glowing).
 *
 * <p>Item frames are never an independent protection unit when they are mounted
 * on a lockable storage block (chest, barrel, shulker box, etc.): they are
 * automatically <b>linked</b> to that block on placement (see {@link #onFramePlace}).
 * A linked frame has no owner/friends of its own: interacting with it opens the
 * underlying block's BlockLock menu, and the frame's protection follows the block's
 * exactly (same owner, same friends, locks and unlocks together). This avoids the
 * double-protection UX problem of a chest + its decorative item frame being two
 * separate things a player has to manage.</p>
 *
 * <p>Item frames NOT mounted on a lockable block (on terrain, on an unlockable
 * block, or detached) keep the legacy standalone-entity protection flow: sneaking
 * and right-clicking with an empty hand opens the BlockProt entity menu directly.</p>
 *
 * <p>Frames use {@link EntityNBTHandler} which stores protection data in the
 * entity's persistent data container. Protection survives chunk reloads and
 * server restarts.</p>
 */
public final class ItemFrameListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrameInteract(@NotNull PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame frame)) return;
        if (!isFrameProtectionEnabled(frame)) return;

        Player player = event.getPlayer();
        if (BlockProt.getDefaultConfig().isWorldExcluded(player.getWorld())) return;

        EntityNBTHandler handler = new EntityNBTHandler(frame);

        Block linkedBlock = resolveLinkedBlock(handler);
        if (linkedBlock != null) {
            handleLinkedFrameInteract(event, frame, linkedBlock);
            return;
        }

        if (player.isSneaking()) {
            // Sneaking + empty hand -> open protection menu.
            var mainHand = player.getInventory().getItemInMainHand();
            if (!mainHand.getType().isAir()) return;

            event.setCancelled(true);

            if (!player.hasPermission(Permissions.USER.key())) {
                sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                return;
            }

            // Deny menu access if this frame is already protected by someone else.
            if (handler.isProtected() && !handler.isManager(player.getUniqueId().toString())
                    && !player.hasPermission(Permissions.USER_ADMIN.key())) {
                sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                AuditLogger audit = BlockProt.getAuditLogger();
                if (audit != null) {
                    audit.log(player.getUniqueId(), player.getName(), frame.getLocation(), AuditLogger.Action.ACCESS_DENIED);
                }
                BlockProtLogger.log("entity-protection", "ACCESS_DENIED frame menu open: "
                    + frame.getType().name() + " entity=" + frame.getUniqueId() + " player=" + player.getName());
                return;
            }

            if (BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
                BlockLockDialog.showForEntity(player, frame, handler);
            } else {
                InventoryState state = InventoryState.getOrCreate(player.getUniqueId());
                state.entityUUID = frame.getUniqueId();

                var inv = new BlockLockInventory().fillForEntity(player, frame, handler);
                if (inv == null) {
                    sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                    return;
                }
                player.openInventory(inv);
            }
        } else {
            // Normal right-click: block if protected and player is not owner/friend/admin.
            if (!handler.isProtected()) return;
            if (handler.canAccess(player.getUniqueId().toString())
                    || player.hasPermission(Permissions.USER_ADMIN.key())) return;
            event.setCancelled(true);
            sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
            AuditLogger audit = BlockProt.getAuditLogger();
            if (audit != null) {
                audit.log(player.getUniqueId(), player.getName(), frame.getLocation(), AuditLogger.Action.ACCESS_DENIED);
            }
            BlockProtLogger.log("entity-protection", "ACCESS_DENIED frame interact: "
                + frame.getType().name() + " entity=" + frame.getUniqueId() + " player=" + player.getName());
        }
    }

    private void handleLinkedFrameInteract(@NotNull PlayerInteractEntityEvent event,
                                            @NotNull ItemFrame frame,
                                            @NotNull Block linkedBlock) {
        Player player = event.getPlayer();
        BlockNBTHandler blockHandler;
        try {
            blockHandler = new BlockNBTHandler(linkedBlock);
        } catch (RuntimeException e) {
            return; // block no longer lockable/loaded: fall through to vanilla behaviour
        }

        if (player.isSneaking()) {
            var mainHand = player.getInventory().getItemInMainHand();
            if (!mainHand.getType().isAir()) return;

            event.setCancelled(true);
            if (!player.hasPermission(Permissions.USER.key())) {
                sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                return;
            }

            if (blockHandler.isProtected()
                    && !blockHandler.isOwner(player.getUniqueId().toString())
                    && blockHandler.getFriend(player.getUniqueId().toString()).isEmpty()
                    && !player.hasPermission(Permissions.USER_ADMIN.key())
                    && !AdminTierManager.hasPermission(player, AdminAction.UNLOCK)
                    && !AdminTierManager.hasPermission(player, AdminAction.INFO)) {
                sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                return;
            }

            BlockAccessMenuEvent menuEvent = new BlockAccessMenuEvent(linkedBlock, player);
            for (BlockAccessMenuEvent.MenuPermission perm : resolveLinkedBlockMenuPermissions(player, blockHandler)) {
                menuEvent.addPermission(perm);
            }
            org.bukkit.Bukkit.getPluginManager().callEvent(menuEvent);
            if (menuEvent.isCancelled() || menuEvent.getPermissions().isEmpty()) {
                sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                return;
            }

            if (BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
                BlockLockDialog.show(player, linkedBlock, blockHandler);
            } else {
                InventoryState state = new InventoryState(linkedBlock);
                state.menuPermissions = menuEvent.getPermissions();
                InventoryState.set(player.getUniqueId(), state);

                Inventory inv = new BlockLockInventory().fill(player, linkedBlock.getType(), blockHandler);
                if (inv == null) {
                    sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                    return;
                }
                player.openInventory(inv);
            }
        } else {
            if (!blockHandler.isProtected()) return;
            if (blockHandler.canAccess(player.getUniqueId().toString())
                    || player.hasPermission(Permissions.USER_ADMIN.key())) return;
            event.setCancelled(true);
            sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
            AuditLogger audit = BlockProt.getAuditLogger();
            if (audit != null) {
                audit.log(player.getUniqueId(), player.getName(), frame.getLocation(), AuditLogger.Action.ACCESS_DENIED);
            }
            BlockProtLogger.log("entity-protection", "ACCESS_DENIED linked-frame interact: "
                + frame.getType().name() + " entity=" + frame.getUniqueId()
                + " linkedBlock=" + linkedBlock.getType().name() + " player=" + player.getName());
        }
    }

    /**
     * Computes the menu permissions for a linked frame's underlying block, mirroring
     * {@link de.sean.blockprot.bukkit.BlockProtAPI#getLockInventoryForBlock}. This is
     * required because the linked-frame menu opens the block's {@link BlockLockInventory}
     * directly, bypassing the normal {@link BlockAccessMenuEvent} flow that would
     * otherwise compute these permissions: without it the menu would render with no
     * buttons at all, since {@link BlockLockInventory#fill} gates every action behind
     * {@code state.menuPermissions}.
     */
    @NotNull
    private Set<BlockAccessMenuEvent.MenuPermission> resolveLinkedBlockMenuPermissions(
            @NotNull Player player, @NotNull BlockNBTHandler blockHandler) {
        Set<BlockAccessMenuEvent.MenuPermission> permissions = new HashSet<>();
        String playerUuid = player.getUniqueId().toString();
        boolean isAdmin = player.hasPermission(Permissions.USER_ADMIN.key());

        if (AdminTierManager.hasPermission(player, AdminAction.UNLOCK)) {
            permissions.add(BlockAccessMenuEvent.MenuPermission.LOCK);
            permissions.add(BlockAccessMenuEvent.MenuPermission.INFO);
        } else if (!blockHandler.isNotProtected() && AdminTierManager.hasPermission(player, AdminAction.INFO)) {
            permissions.add(BlockAccessMenuEvent.MenuPermission.INFO);
        }

        if (blockHandler.isOwner(playerUuid) || isAdmin) {
            permissions.add(BlockAccessMenuEvent.MenuPermission.LOCK);
            permissions.add(BlockAccessMenuEvent.MenuPermission.INFO);
            permissions.add(BlockAccessMenuEvent.MenuPermission.MANAGER);
        } else if (blockHandler.isNotProtected()) {
            permissions.add(BlockAccessMenuEvent.MenuPermission.LOCK);
        } else {
            blockHandler.getFriend(playerUuid).ifPresent(friend -> {
                if (friend.canOpenMenu()) permissions.add(BlockAccessMenuEvent.MenuPermission.INFO);
                if (friend.isManager()) permissions.add(BlockAccessMenuEvent.MenuPermission.MANAGER);
            });
        }
        return permissions;
    }

    /**
     * Resolves the block linked to this frame, if any, validating that the link
     * is still consistent (the frame must still be attached to that exact block).
     * Returns null if there is no link, or if the link is stale (block moved/changed
     * since linking: defensive check against desync after world edits).
     */
    @Nullable
    private Block resolveLinkedBlock(@NotNull EntityNBTHandler handler) {
        String encoded = handler.getLinkedBlock();
        if (encoded.isEmpty()) return null;
        String[] parts = encoded.split(",");
        if (parts.length != 4) return null;
        try {
            org.bukkit.World world = org.bukkit.Bukkit.getWorld(parts[0]);
            if (world == null) return null;
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return world.getBlockAt(x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrameBreakByPlayer(@NotNull HangingBreakByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) return;
        if (!isFrameProtectionEnabled(frame)) return;
        if (BlockProt.getDefaultConfig().isWorldExcluded(frame.getWorld())) return;

        Player player = resolveAttacker(event.getRemover());
        EntityNBTHandler handler = new EntityNBTHandler(frame);

        Block linkedBlock = resolveLinkedBlock(handler);
        if (linkedBlock != null) {
            try {
                BlockNBTHandler blockHandler = new BlockNBTHandler(linkedBlock);
                if (!blockHandler.isProtected()) return;
                if (player == null) {
                    event.setCancelled(true);
                    return;
                }
                if (!blockHandler.canAccess(player.getUniqueId().toString())
                        && !player.hasPermission(Permissions.USER_ADMIN.key())) {
                    event.setCancelled(true);
                    sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                    BlockProtLogger.log("entity-protection", "ACCESS_DENIED linked-frame break: "
                        + frame.getType().name() + " entity=" + frame.getUniqueId() + " player=" + player.getName());
                }
            } catch (RuntimeException ignored) {}
            return;
        }

        if (!handler.isProtected()) return;
        if (player == null) {
            event.setCancelled(true);
            return;
        }

        if (!handler.canAccess(player.getUniqueId().toString())
                && !player.hasPermission(Permissions.USER_ADMIN.key())) {
            event.setCancelled(true);
            sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
            BlockProtLogger.log("entity-protection", "ACCESS_DENIED frame break: "
                + frame.getType().name() + " entity=" + frame.getUniqueId() + " player=" + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrameDamageByEntity(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) return;
        if (!isFrameProtectionEnabled(frame)) return;
        if (BlockProt.getDefaultConfig().isWorldExcluded(frame.getWorld())) return;

        Player player = resolveAttacker(event.getDamager());
        EntityNBTHandler handler = new EntityNBTHandler(frame);

        Block linkedBlock = resolveLinkedBlock(handler);
        if (linkedBlock != null) {
            try {
                BlockNBTHandler blockHandler = new BlockNBTHandler(linkedBlock);
                if (!blockHandler.isProtected()) return;
                if (player == null) {
                    event.setCancelled(true);
                    return;
                }
                if (!blockHandler.canAccess(player.getUniqueId().toString())
                        && !player.hasPermission(Permissions.USER_ADMIN.key())) {
                    event.setCancelled(true);
                    sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                    BlockProtLogger.log("entity-protection", "ACCESS_DENIED linked-frame damage: "
                        + frame.getType().name() + " entity=" + frame.getUniqueId() + " player=" + player.getName());
                }
            } catch (RuntimeException ignored) {}
            return;
        }

        if (!handler.isProtected()) return;
        if (player == null) {
            event.setCancelled(true);
            return;
        }

        if (!handler.canAccess(player.getUniqueId().toString())
                && !player.hasPermission(Permissions.USER_ADMIN.key())) {
            event.setCancelled(true);
            sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
            BlockProtLogger.log("entity-protection", "ACCESS_DENIED frame damage: "
                + frame.getType().name() + " entity=" + frame.getUniqueId() + " player=" + player.getName());
        }
    }

    @Nullable
    private static Player resolveAttacker(@Nullable Entity source) {
        if (source instanceof Player p) return p;
        if (source instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    private boolean isFrameProtectionEnabled(@NotNull ItemFrame frame) {
        Material mat = frame instanceof GlowItemFrame ? Material.GLOW_ITEM_FRAME : Material.ITEM_FRAME;
        return BlockProt.getDefaultConfig().isLockableEntity(mat, frame.getWorld());
    }

    /**
     * Handles item frame placement.
     *
     * <p>Two scenarios are covered:
     * <ol>
     *   <li><b>Mounted on a lockable block</b>: the frame is automatically linked
     *       to that block (bidirectional link via {@link EntityNBTHandler#setLinkedBlock}
     *       and {@link BlockNBTHandler#setLinkedItemFrameUuid}). From this point on the
     *       frame has no protection state of its own: it shares the block's owner,
     *       friends, lock state, and menu. This prevents griefers from placing frames
     *       on someone's chest to visually interfere with it, and avoids the player
     *       having to separately protect a decorative frame next to a chest they
     *       already protected.</li>
     *   <li><b>Not mounted on a lockable block</b>: frame remains a standalone
     *       protectable entity. If the placing player has "lock on place" enabled
     *       (and is not sneaking), the frame is immediately locked to them, exactly
     *       mirroring the behaviour for lockable blocks.</li>
     * </ol>
     *
     * <p>Linking always takes priority over scenario 2: a frame mounted on a
     * lockable block is never given its own owner.</p>
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFramePlace(@NotNull HangingPlaceEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) return;
        if (!isFrameProtectionEnabled(frame)) return;
        Player player = event.getPlayer();
        if (player == null) return;
        if (BlockProt.getDefaultConfig().isWorldExcluded(player.getWorld())) return;

        EntityNBTHandler frameHandler = new EntityNBTHandler(frame);
        if (frameHandler.isProtected() || frameHandler.isLinkedToBlock()) return; // already owned/linked: do not overwrite

        // Scenario 1: link to the attached block, if it's lockable
        BlockFace attachedFace = frame.getAttachedFace();
        if (attachedFace != null) {
        // getAttachedFace() faces the wall, so the attached block is opposite.
            Block attachedBlock = frame.getLocation().getBlock()
                .getRelative(attachedFace.getOppositeFace());
            if (BlockProt.getDefaultConfig().isLockable(
                    attachedBlock.getType(), attachedBlock.getWorld())) {
                try {
                    BlockNBTHandler blockHandler = new BlockNBTHandler(attachedBlock);
                    // Only link if the block doesn't already have a different linked frame.
                    // Defensive: one block face holds at most one frame.
                    if (!blockHandler.hasLinkedItemFrame()) {
                        frameHandler.setLinkedBlock(attachedBlock.getWorld().getName(),
                            attachedBlock.getX(), attachedBlock.getY(), attachedBlock.getZ());
                        blockHandler.setLinkedItemFrameUuid(frame.getUniqueId().toString());
                        BlockProtLogger.log("entity-protection",
                            "LINKED frame " + frame.getUniqueId()
                            + " to " + attachedBlock.getType().name()
                            + " at " + attachedBlock.getX() + "," + attachedBlock.getY() + "," + attachedBlock.getZ());
                        return; // linked: skip standalone auto-lock entirely
                    }
                } catch (RuntimeException ignored) {}
            }
        }

        // Scenario 2: auto-lock to the placing player (standalone frame only)
        if (!player.hasPermission(Permissions.USER.key())) return;
        if (!new de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler(player).getLockOnPlace()) return;
        if (player.isSneaking()) return;

        frameHandler.setOwner(player.getUniqueId().toString());
        de.sean.blockprot.bukkit.util.TemporaryActionBar.show(player,
            Translator.get(TranslationKey.MESSAGES__LOCK_ON_PLACE_SUCCESS));
        // No session log for auto-lock: would spam on every frame placement.
    }

    private boolean isProtectedFrame(@NotNull ItemFrame frame) {
        if (!isFrameProtectionEnabled(frame)) return false;
        if (BlockProt.getDefaultConfig().isWorldExcluded(frame.getWorld())) return false;
        EntityNBTHandler handler = new EntityNBTHandler(frame);
        Block linkedBlock = resolveLinkedBlock(handler);
        if (linkedBlock != null) {
            try {
                if (new BlockNBTHandler(linkedBlock).isProtected()) return true;
            } catch (RuntimeException ignored) {}
        }
        return handler.isProtected();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(@NotNull HangingBreakEvent event) {
        if (event instanceof HangingBreakByEntityEvent) return;
        if (!(event.getEntity() instanceof ItemFrame frame)) return;
        if (!isProtectedFrame(frame)) return;
        if (event.getCause() == HangingBreakEvent.RemoveCause.EXPLOSION
                && !BlockProt.getDefaultConfig().shouldProtectLockedBlocksFromExplosions()) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTeleport(@NotNull EntityTeleportEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) return;
        if (isProtectedFrame(frame)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDropItem(@NotNull EntityDropItemEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) return;
        if (isProtectedFrame(frame)) {
            event.setCancelled(true);
        }
    }

    private void sendActionBar(@NotNull Player player, @NotNull String text) {
        de.sean.blockprot.bukkit.util.TemporaryActionBar.show(player, text);
    }
}