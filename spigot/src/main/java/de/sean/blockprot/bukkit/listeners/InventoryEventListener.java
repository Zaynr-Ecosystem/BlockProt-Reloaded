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
import de.sean.blockprot.bukkit.audit.AuditLogger;
import de.sean.blockprot.bukkit.events.BlockAccessEvent;
import de.sean.blockprot.bukkit.inventories.BlockProtInventory;
import de.sean.blockprot.bukkit.inventories.InventoryState;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.EntityNBTHandler;
import de.sean.blockprot.bukkit.nbt.FriendHandler;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class InventoryEventListener implements Listener {

    private static final Set<InventoryAction> TAKE_ACTIONS = EnumSet.of(
        InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_HALF,
        InventoryAction.PICKUP_ONE, InventoryAction.PICKUP_SOME,
        InventoryAction.DROP_ALL_SLOT, InventoryAction.DROP_ONE_SLOT,
        InventoryAction.MOVE_TO_OTHER_INVENTORY,
        InventoryAction.HOTBAR_SWAP,
        InventoryAction.COLLECT_TO_CURSOR
    );
    private static final Set<InventoryAction> PLACE_ACTIONS = EnumSet.of(
        InventoryAction.PLACE_ALL, InventoryAction.PLACE_ONE, InventoryAction.PLACE_SOME
    );
    private static final Class<?> CHEST_BOAT_CLASS = resolveChestBoatClass();

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        final Player player = (Player) event.getWhoClicked();
        final InventoryHolder topHolder = event.getInventory().getHolder();

        if (topHolder instanceof BlockProtInventory) {
            event.setCancelled(true);
            InventoryState state = InventoryState.get(player.getUniqueId());
            if (state == null) {
                state = InventoryState.builder().build();
                InventoryState.set(player.getUniqueId(), state);
            }
            final var clickedInventory = event.getClickedInventory();
            if (clickedInventory != null && clickedInventory.getHolder() instanceof BlockProtInventory bpInventory) {
                try {
                    bpInventory.onClick(event, state);
                } catch (Throwable t) {
                    BlockProtLogger.error("Failed to process inventory click in " + bpInventory.getClass().getSimpleName(), t);
                    ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__MENU_INTERNAL_ERROR));
                }
            }
            player.updateInventory();
            return;
        }

        try {
            InventoryHolder rawHolder = event.getInventory().getHolder();
            if (rawHolder == null) return;
            if (rawHolder instanceof Entity entity && isProtectedInventoryEntity(entity)) {
                handleEntityInventoryClick(event, player, entity);
                return;
            }
            if (!(rawHolder instanceof BlockInventoryHolder blockHolder)) return;
            Block block = blockHolder.getBlock();
            if (BlockProt.getDefaultConfig().isLockable(block.getType(), block.getWorld())) {
                BlockNBTHandler handler = new BlockNBTHandler(block);
                String playerUuid = player.getUniqueId().toString();

                if (handler.isProtected() && !handler.isOwner(playerUuid)) {
                    final var friend = handler.getFriend(playerUuid);
                    if (friend.isPresent()) {
                        FriendHandler friendHandler = friend.get();
                        if (!friendHandler.canOpenContainer()) {
                            event.setCancelled(true);
                            player.closeInventory();
                            return;
                        }
                        if (TAKE_ACTIONS.contains(event.getAction()) && !friendHandler.canWithdraw()) {
                            event.setCancelled(true);
                            sendMessage(player, Translator.get(TranslationKey.MESSAGES__CUSTOM_PERM_NO_WITHDRAW));
                            return;
                        }
                        if (PLACE_ACTIONS.contains(event.getAction()) && !friendHandler.canDeposit()) {
                            event.setCancelled(true);
                            sendMessage(player, Translator.get(TranslationKey.MESSAGES__CUSTOM_PERM_NO_DEPOSIT));
                            return;
                        }
                        if (!friendHandler.canWrite()) {
                            event.setCancelled(true);
                        } else if (!friendHandler.canRead()) {
                            event.setCancelled(true);
                            player.closeInventory();
                        } else {
                            if (event.getClickedInventory() != null
                                    && event.getClickedInventory().equals(event.getInventory())) {
                                notifyOwnerItemAction(handler, player, block, event.getAction(), event.getCurrentItem());
                            }
                        }
                    } else {
                        player.closeInventory();
                        event.setCancelled(true);
                    }
                } else if (handler.isProtected() && handler.isOwner(playerUuid)) {
                    // Owner: log item actions to audit
                    if (event.getClickedInventory() != null
                            && event.getClickedInventory().equals(event.getInventory())) {
                        ItemStack item = event.getCurrentItem();
                        if (item != null && !item.getType().isAir()) {
                            AuditLogger audit = BlockProt.getAuditLogger();
                            if (audit != null) {
                                AuditLogger.Action act = TAKE_ACTIONS.contains(event.getAction())
                                    ? AuditLogger.Action.ITEM_TAKEN
                                    : (PLACE_ACTIONS.contains(event.getAction()) ? AuditLogger.Action.ITEM_PLACED : null);
                                if (act != null) audit.log(player.getUniqueId(), player.getName(), block.getLocation(), act);
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            BlockProtLogger.error("Failed to handle container inventory click", t);
        }
    }

    @EventHandler
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof BlockProtInventory) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLecternClick(@NotNull PlayerTakeLecternBookEvent event) {
        final var handler = new BlockNBTHandler(event.getLectern().getBlock());
        final var uuid = event.getPlayer().getUniqueId();

        if (handler.isProtected() && !handler.isOwner(uuid)) {
            final var friend = handler.getFriend(uuid.toString());
            if (friend.isPresent()) {
                if (!friend.get().canWrite()) {
                    event.setCancelled(true);
                } else if (!friend.get().canRead()) {
                    event.setCancelled(true);
                    event.getPlayer().closeInventory();
                }
            } else {
                event.setCancelled(true);
                event.getPlayer().closeInventory();
            }
        }
    }

    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        final InventoryState state = InventoryState.get(player.getUniqueId());
        if (state == null) return;
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BlockProtInventory bpInventory) {
            try {
                bpInventory.onClose(event, state);
            } catch (Throwable t) {
                BlockProtLogger.error("Failed to process inventory close in " + bpInventory.getClass().getSimpleName(), t);
            }
            if (!state.originStack.isEmpty()) {
                // Player-initiated close (ESC / E / X) on a submenu: reopen the parent
                // menu next tick. Programmatic navigation reopens another inventory
                // before the task runs, so it never triggers this path.
                BlockProt.getFoliaLib().getScheduler().runAtEntity(player, syncTask -> {
                    Player online = Bukkit.getPlayer(player.getUniqueId());
                    if (online == null || !online.isOnline()) return;
                    if (online.getOpenInventory().getTopInventory().getHolder() instanceof BlockProtInventory) return;
                    InventoryState st = InventoryState.get(online.getUniqueId());
                    if (st == null || st.originStack.isEmpty()) return;
                    if (st.suppressCloseReopen) {
                        // Closed intentionally to prompt a chat input (TextInput#open),
                        // not a player-initiated ESC/E/X close. Do not reopen the parent.
                        st.suppressCloseReopen = false;
                        return;
                    }
                    bpInventory.goBack(online, st);
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(@NotNull InventoryOpenEvent event) {
        String playerUuid = event.getPlayer().getUniqueId().toString();
        InventoryHolder holder = event.getInventory().getHolder();

        if (event.isCancelled()) {
            if (event.getPlayer() instanceof Player player) {
                if (holder instanceof Entity entity && isProtectedInventoryEntity(entity)) {
                    EntityNBTHandler handler = new EntityNBTHandler(entity);
                    if (handler.isProtected()) {
                        AuditLogger audit = BlockProt.getAuditLogger();
                        if (audit != null) {
                            audit.log(player.getUniqueId(), player.getName(), entity.getLocation(), AuditLogger.Action.ACCESS_DENIED);
                        }
                        BlockProtLogger.log("entity-protection", "ACCESS_DENIED (already cancelled) inventory open: "
                            + entity.getType().name() + " entity=" + entity.getUniqueId() + " player=" + player.getName());
                    }
                } else if (holder instanceof Container || holder instanceof DoubleChest) {
                    Block block = holder instanceof Container container ? container.getBlock() : ((DoubleChest) holder).getLocation().getBlock();
                    if (BlockProt.getDefaultConfig().isLockable(block.getType(), block.getWorld())) {
                        BlockNBTHandler handler = new BlockNBTHandler(block);
                        if (handler.isProtected()) {
                            AuditLogger audit = BlockProt.getAuditLogger();
                            if (audit != null) {
                                audit.log(player.getUniqueId(), player.getName(), block.getLocation(), AuditLogger.Action.ACCESS_DENIED);
                            }
                            BlockProtLogger.log("audit", "ACCESS_DENIED (already cancelled) block open: "
                                + block.getType().name() + " location=" + block.getLocation() + " player=" + player.getName());
                        }
                    }
                }
            }
            return;
        }

        if (holder instanceof BlockProtInventory) {
            InventoryState state = InventoryState.get(playerUuid);
            if (state == null || state.getBlock() == null) return;
            try {
                BlockNBTHandler handler = new BlockNBTHandler(state.getBlock());
                Optional<FriendHandler> friend = handler.getFriend(playerUuid);
                if (!(handler.isNotProtected()
                        || handler.isOwner(playerUuid)
                        || (friend.isPresent() && friend.get().isManager())
                        || event.getPlayer().hasPermission(Permissions.USER_ADMIN.key()))) {
                    event.setCancelled(true);
                    sendMessage(event.getPlayer(), Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                }
            } catch (RuntimeException ignored) {}

        } else if (holder instanceof Entity entity && isProtectedInventoryEntity(entity)
                && event.getPlayer() instanceof Player player) {
            EntityNBTHandler handler = new EntityNBTHandler(entity);
            if (!handler.isProtected()) return;

            if (!(handler.canAccess(playerUuid) || player.hasPermission(Permissions.USER_ADMIN.key()))) {
                event.setCancelled(true);
                sendMessage(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                AuditLogger audit = BlockProt.getAuditLogger();
                if (audit != null) {
                    audit.log(player.getUniqueId(), player.getName(), entity.getLocation(), AuditLogger.Action.ACCESS_DENIED);
                }
                BlockProtLogger.log("entity-protection", "ACCESS_DENIED inventory open: "
                    + entity.getType().name() + " entity=" + entity.getUniqueId() + " player=" + player.getName());
            } else if (!handler.isOwner(playerUuid)) {
                // Log OPENED only for non-owner access (friends/admins): routine
                // access by the owner is not a security-relevant event.
                AuditLogger audit = BlockProt.getAuditLogger();
                if (audit != null) {
                    audit.log(player.getUniqueId(), player.getName(), entity.getLocation(), AuditLogger.Action.OPENED);
                }
                BlockProtLogger.log("entity-protection", "OPENED inventory: "
                    + entity.getType().name() + " entity=" + entity.getUniqueId() + " player=" + player.getName());
            }

        } else if ((holder instanceof Container || holder instanceof DoubleChest)
                && event.getPlayer() instanceof Player player) {
            Block block;
            if (holder instanceof Container container) {
                block = container.getBlock();
            } else {
                block = ((DoubleChest) holder).getLocation().getBlock();
            }

            if (BlockProt.getDefaultConfig().isLockable(block.getType(), block.getWorld())) {
                BlockAccessEvent accessEvent = new BlockAccessEvent(block, player);
                Bukkit.getPluginManager().callEvent(accessEvent);

                if (accessEvent.isCancelled()) {
                    event.setCancelled(true);
                    sendMessage(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                    BlockNBTHandler handler = new BlockNBTHandler(block);
                    if (handler.isProtected()) {
                        AuditLogger audit = BlockProt.getAuditLogger();
                        if (audit != null) {
                            audit.log(player.getUniqueId(), player.getName(), block.getLocation(), AuditLogger.Action.ACCESS_DENIED);
                        }
                    }
                } else {
                    BlockNBTHandler handler = new BlockNBTHandler(block);
                    if (!accessEvent.shouldBypassProtections()
                            && !(handler.canAccess(playerUuid) || player.hasPermission(Permissions.USER_ADMIN.key()))) {
                        event.setCancelled(true);
                        sendMessage(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                        AuditLogger audit = BlockProt.getAuditLogger();
                        if (audit != null && handler.isProtected()) {
                            audit.log(player.getUniqueId(), player.getName(), block.getLocation(), AuditLogger.Action.ACCESS_DENIED);
                        }
                    } else if (handler.isProtected()) {
                        // Log OPENED for both owner and friends
                        AuditLogger audit = BlockProt.getAuditLogger();
                        if (audit != null) {
                            audit.log(player.getUniqueId(), player.getName(), block.getLocation(), AuditLogger.Action.OPENED);
                        }
                        // Notify owner only when a non-owner opens the block
                        if (!handler.isOwner(playerUuid) && BlockProt.getDefaultConfig().isNotifyOnOpen()) {
                            String blockName = friendlyBlockName(block);
                            notifyOwner(handler, player,
                                Translator.get(TranslationKey.MESSAGES__NOTIFY_OPENED)
                                    .replace("{player}", player.getName())
                                    .replace("{block}", blockName));
                        }
                    }
                }
            }
        }
    }

    private static void handleEntityInventoryClick(@NotNull InventoryClickEvent event,
                                                   @NotNull Player player,
                                                   @NotNull Entity entity) {
        EntityNBTHandler handler = new EntityNBTHandler(entity);
        if (!handler.isProtected()) return;
        String playerUuid = player.getUniqueId().toString();
        if (handler.canAccess(playerUuid) || player.hasPermission(Permissions.USER_ADMIN.key())) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getInventory())) {
                AuditLogger.Action act = TAKE_ACTIONS.contains(event.getAction())
                    ? AuditLogger.Action.ITEM_TAKEN
                    : (PLACE_ACTIONS.contains(event.getAction()) ? AuditLogger.Action.ITEM_PLACED : null);
                AuditLogger audit = BlockProt.getAuditLogger();
                if (audit != null && act != null) {
                    audit.log(player.getUniqueId(), player.getName(), entity.getLocation(), act);
                }
            }
            return;
        }

        event.setCancelled(true);
        player.closeInventory();
        AuditLogger audit = BlockProt.getAuditLogger();
        if (audit != null) {
            audit.log(player.getUniqueId(), player.getName(), entity.getLocation(), AuditLogger.Action.ACCESS_DENIED);
        }
        BlockProtLogger.log("entity-protection", "ACCESS_DENIED inventory click: "
            + entity.getType().name() + " entity=" + entity.getUniqueId() + " player=" + player.getName());
    }

    private static boolean isProtectedInventoryEntity(@NotNull Entity entity) {
        return entity instanceof StorageMinecart
            || entity instanceof HopperMinecart
            || (CHEST_BOAT_CLASS != null && CHEST_BOAT_CLASS.isInstance(entity));
    }

    private static Class<?> resolveChestBoatClass() {
        try { return Class.forName("org.bukkit.entity.boat.ChestBoat"); } catch (ClassNotFoundException ignored) {}
        try { return Class.forName("org.bukkit.entity.ChestBoat"); } catch (ClassNotFoundException ignored) {}
        return null;
    }

    // Helpers

    private void sendMessage(@NotNull HumanEntity player, @NotNull String text) {
        if (!(player instanceof Player p)) return;
        ComponentMessages.sendActionBar(p, LegacyComponentSerializer.legacySection().deserialize(text));
    }

    /**
     * Sends an action-bar notification to the block owner if they are online,
     * different from the accessing player, and have notifications enabled.
     */
    private static void notifyOwner(@NotNull BlockNBTHandler handler,
                                    @NotNull Player accessor,
                                    @NotNull String message) {
        String ownerUuid = handler.getOwner();
        if (ownerUuid.isBlank()) return;
        try {
            UUID uuid = UUID.fromString(ownerUuid);
            if (uuid.equals(accessor.getUniqueId())) return;
            Player owner = Bukkit.getPlayer(uuid);
            if (owner != null && owner.isOnline()) {
                // Respect per-player notification preference
                if (!new de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler(owner).getNotificationsEnabled()) return;
                ComponentMessages.sendActionBar(owner, LegacyComponentSerializer.legacySection().deserialize(message));
            }
        } catch (IllegalArgumentException ignored) {}
    }

    /**
     * Notifies the owner about an item take/place action by a friend, if enabled.
     */
    private static void notifyOwnerItemAction(@NotNull BlockNBTHandler handler,
                                              @NotNull Player player,
                                              @NotNull Block block,
                                              @NotNull InventoryAction action,
                                              @Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        String blockName = friendlyBlockName(block);
        String itemName  = friendlyMaterialName(item.getType().name());
        int    amount    = item.getAmount();

        if (TAKE_ACTIONS.contains(action) && BlockProt.getDefaultConfig().isNotifyOnTake()) {
            notifyOwner(handler, player,
                Translator.get(TranslationKey.MESSAGES__NOTIFY_ITEM_TAKEN)
                    .replace("{player}", player.getName())
                    .replace("{amount}", String.valueOf(amount))
                    .replace("{item}", itemName)
                    .replace("{block}", blockName));
            AuditLogger audit = BlockProt.getAuditLogger();
            if (audit != null) audit.log(player.getUniqueId(), player.getName(), block.getLocation(), AuditLogger.Action.ITEM_TAKEN);
        } else if (PLACE_ACTIONS.contains(action) && BlockProt.getDefaultConfig().isNotifyOnPlace()) {
            notifyOwner(handler, player,
                Translator.get(TranslationKey.MESSAGES__NOTIFY_ITEM_PLACED)
                    .replace("{player}", player.getName())
                    .replace("{amount}", String.valueOf(amount))
                    .replace("{item}", itemName)
                    .replace("{block}", blockName));
            AuditLogger audit = BlockProt.getAuditLogger();
            if (audit != null) audit.log(player.getUniqueId(), player.getName(), block.getLocation(), AuditLogger.Action.ITEM_PLACED);
        }
    }

    /**
     * Converts a block's material name to a human-readable title-cased string.
     * Delegates to {@link #friendlyMaterialName(String)}.
     *
     * @param block The block whose type name to format.
     * @return The formatted display name, e.g. {@code "Oak Chest"}.
     */
    @NotNull
    private static String friendlyBlockName(@NotNull Block block) {
        return friendlyMaterialName(block.getType().name());
    }

    /**
     * Converts a material name string to a human-readable title-cased display name.
     * Underscores are replaced with spaces and each word is capitalised.
     * Example: {@code "OAK_CHEST"} -> {@code "Oak Chest"}.
     *
     * @param name The raw material name (e.g. from {@link org.bukkit.Material#name()}).
     * @return The formatted display name.
     */
    @NotNull
    private static String friendlyMaterialName(@NotNull String name) {
        String[] words = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}