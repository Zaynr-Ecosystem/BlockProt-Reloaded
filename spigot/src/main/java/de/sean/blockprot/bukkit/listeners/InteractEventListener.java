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

import de.sean.blockprot.bukkit.*;
import de.sean.blockprot.bukkit.dialogs.BlockLockDialog;
import de.sean.blockprot.bukkit.events.BlockAccessEvent;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.type.Lectern;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Handles player block interactions: access control, lock-on-place bypass,
 * lectern write restrictions, and lock hint messages.
 */
public class InteractEventListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void playerInteract(PlayerInteractEvent event) {
        // The InsaneShops plugin uses this weird FakeEvent event that inherits from PlayerInteractEvent.
        // We don't want to trigger on that interact event, so here's this check.
        if (event.getClass().getName().equals("Lme.TechsCode.InsaneShops.utilities.FakeEvent;"))
            return;

        if (event.getClickedBlock() == null) return;
        if (event.isCancelled() || event.useInteractedBlock() == org.bukkit.event.Event.Result.DENY) return;
        if (BlockProt.getDefaultConfig().isWorldExcluded(event.getClickedBlock().getWorld())) return;
        if (!BlockProt.getDefaultConfig().isLockable(event.getClickedBlock().getType(),
            event.getClickedBlock().getWorld())) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            Player offHandPlayer = event.getPlayer();
            var offHandItem = event.getItem();
            if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND
                    && offHandPlayer.isSneaking()
                    && offHandPlayer.getInventory().getItemInMainHand().getType().isAir()
                    && (offHandItem == null || !offHandItem.getType().isBlock())) {
                event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            }
            return;
        }

        // Dragon Egg teleports on ANY click (left or right) and bypasses cancel in some Paper versions.
        // Force-cancel both PHYSICAL and RIGHT_CLICK_BLOCK to prevent the teleport.
        if (event.getClickedBlock().getType() == Material.DRAGON_EGG) {
            BlockNBTHandler eggHandler = new BlockNBTHandler(event.getClickedBlock());
            if (eggHandler.isProtected()) {
                Player eggPlayer = event.getPlayer();
                if (!eggHandler.canAccess(eggPlayer.getUniqueId().toString())
                        && !de.sean.blockprot.bukkit.admin.AdminTierManager.hasPermission(eggPlayer, de.sean.blockprot.bukkit.admin.AdminAction.CONTAINER_BYPASS)) {
                    event.setCancelled(true);
                    event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
                    event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
                    sendMessage(eggPlayer, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                    return;
                }
            }
        }

        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            BlockAccessEvent accessEvent = new BlockAccessEvent(event.getClickedBlock(), player);
            Bukkit.getPluginManager().callEvent(accessEvent);
            if (accessEvent.isCancelled()) {
                event.setCancelled(true);
                sendMessage(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
            } else if (!accessEvent.shouldBypassProtections()) {
                BlockNBTHandler handler = new BlockNBTHandler(event.getClickedBlock());
                if (handler.isProtected() && !handler.isOwner(player.getUniqueId())) {
                    String ownerUuidStr = handler.getOwner();
                    String ownerName = null;
                    if (!ownerUuidStr.isEmpty()) {
                        try {
                            ownerName = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(ownerUuidStr)).getName();
                        } catch (IllegalArgumentException ignored) {}
                    }
                    if (ownerName != null && !ownerName.isEmpty()) {
                        de.sean.blockprot.bukkit.util.TemporaryActionBar.show(player, Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__OWNER_LABEL) + " " + ownerName, BlockProt.getDefaultConfig().getActionBarDurationTicks());
                    }
                }
                if (!(handler.canAccess(player.getUniqueId().toString()) || de.sean.blockprot.bukkit.admin.AdminTierManager.hasPermission(player, de.sean.blockprot.bukkit.admin.AdminAction.CONTAINER_BYPASS))) {
                    event.setCancelled(true);
                    sendMessage(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                    de.sean.blockprot.bukkit.audit.AuditLogger audit = BlockProt.getAuditLogger();
                    if (audit != null) {
                        audit.log(player.getUniqueId(), player.getName(), event.getClickedBlock().getLocation(),
                            de.sean.blockprot.bukkit.audit.AuditLogger.Action.ACCESS_DENIED);
                    }
                } else {
                    // Player has access: ensure the event is NOT cancelled regardless of what
                    // lower-priority listeners (vanilla Paper included) may have set.
                    event.setCancelled(false);
                    if (event.getClickedBlock().getType() == Material.LECTERN && !handler.isOwner(player.getUniqueId())) {
                        // Lectern: book placement uses InteractEvent; canAccess already checked.
                        // Take-book case handled by PlayerTakeLecternBookEvent.
                        final var lectern = (Lectern)event.getClickedBlock().getBlockData();
                        if (!lectern.hasBook()) {
                            final var friend = handler.getFriend(player.getUniqueId().toString());
                            if (friend.isEmpty() || !friend.get().canWrite()) {
                                event.setCancelled(true);
                                sendMessage(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                            }
                        }
                    } else if (!(new PlayerSettingsHandler(player).hasPlayerInteractedWithMenu())) {
                        Long timestamp = LockHintMessageCooldown.getTimestamp(player);
                        if (timestamp == null || timestamp < System.currentTimeMillis() - (BlockProt.getDefaultConfig().getLockHintCooldown() * 1000)) {
                            String message = Translator.get(TranslationKey.MESSAGES__LOCK_HINT);
                            if (!message.isEmpty()) {
                                LockHintMessageCooldown.setTimestamp(player);
                                var tooltip = Translator.get(TranslationKey.MESSAGES__HINT_HOVER_TEXT);
                                sendEventsMessage(player, message, true,
                                    "/blockprot disablehints", tooltip.isEmpty() ? null : tooltip);
                            }
                        }
                    }
                }
            } else {
                    // bypassProtections was set by an integration
                event.setCancelled(false);
            }
        } else {
            if (event.hasItem()) return;
            // Skip if the off-hand holds a placeable block: the player is placing, not menu-opening.
            var offHandItem = player.getInventory().getItemInOffHand();
            if (!offHandItem.getType().isAir() && offHandItem.getType().isBlock()) return;
            event.setCancelled(true);

            if (!player.hasPermission(Permissions.USER.key())) {
                sendMessage(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                return;
            }

            BlockNBTHandler blockHandler = new BlockNBTHandler(event.getClickedBlock());
            if (blockHandler.isProtected() && !blockHandler.isOwner(player.getUniqueId())
                    && !de.sean.blockprot.bukkit.admin.AdminTierManager.hasPermission(player, de.sean.blockprot.bukkit.admin.AdminAction.UNLOCK)
                    && !de.sean.blockprot.bukkit.admin.AdminTierManager.hasPermission(player, de.sean.blockprot.bukkit.admin.AdminAction.INFO)) {
                var friendOpt = blockHandler.getFriend(player.getUniqueId().toString());
                if (friendOpt.isEmpty() || friendOpt.get().doesRepresentPublic() || !friendOpt.get().canOpenMenu()) {
                    sendMessage(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                    return;
                }
            }

            if (de.sean.blockprot.bukkit.bedrock.BedrockBridge.shouldUseBedrockForms(player)) {
                new PlayerSettingsHandler(player).setHasPlayerInteractedWithMenu(true);
                de.sean.blockprot.bukkit.bedrock.forms.BedrockBlockLockForm.show(player, event.getClickedBlock(), new de.sean.blockprot.bukkit.nbt.BlockNBTHandler(event.getClickedBlock()));
            } else if (BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
                BlockLockDialog.showBlock(player, event.getClickedBlock());
            } else {
                BlockProtAPI api = BlockProtAPI.getInstance();
                if (api == null) return;
                Inventory inv = api.getLockInventoryForBlock(event.getClickedBlock(), player);
                if (inv == null) {
                    sendMessage(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                } else {
                    new PlayerSettingsHandler(player).setHasPlayerInteractedWithMenu(true);
                    player.openInventory(inv);
                }
            }
        }
    }

    private void sendMessage(@NotNull Player player, @NotNull String text) {
        de.sean.blockprot.bukkit.util.TemporaryActionBar.show(player, text);
    }

    private void sendMessage(@NotNull Player player, @NotNull String text, boolean asChat) {
        var comp = LegacyComponentSerializer.legacySection().deserialize(text);
        if (asChat) ComponentMessages.send(player, comp);
        else de.sean.blockprot.bukkit.util.TemporaryActionBar.show(player, comp);
    }

    private void sendEventsMessage(@NotNull Player player, @NotNull String text, boolean asChat, @Nullable String command, @Nullable String tooltip) {
        var comp = Component.text(text);
        if (command != null) comp = comp.clickEvent(ClickEvent.runCommand(command));
        if (tooltip != null) comp = comp.hoverEvent(HoverEvent.showText(Component.text(tooltip)));
        if (asChat) ComponentMessages.send(player, comp);
        else de.sean.blockprot.bukkit.util.TemporaryActionBar.show(player, comp);
    }

    private static class LockHintMessageCooldown {
        // WeakHashMap lets the GC collect entries when the Player object is no longer
        // strongly referenced (i.e. after the player disconnects), preventing memory leaks.
        private static final java.util.WeakHashMap<Player, Long> timestamps = new java.util.WeakHashMap<>();

        public static void setTimestamp(final @NotNull Player player) {
            timestamps.put(player, System.currentTimeMillis());
        }

        @Nullable
        public static Long getTimestamp(final @NotNull Player player) {
            return timestamps.get(player);
        }
    }
}
