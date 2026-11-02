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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Iterables;
import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.events.BlockLockOnPlaceEvent;
import de.sean.blockprot.bukkit.events.BlockProtLockEvent;
import de.sean.blockprot.bukkit.integrations.PluginIntegration;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.BlockCountStatistic;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import de.sean.blockprot.bukkit.storage.ProtectedBlockCache;
import de.sean.blockprot.bukkit.util.BlockUtil;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import de.sean.blockprot.nbt.LockReturnValue;
import de.sean.blockprot.bukkit.VersionCompat;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Handles block-related events: break, place, burn, physics, dispense, and signs.
 */
public class BlockEventListener implements Listener {
    private final BlockProt blockProt;

    private final NamespacedKey shulkerDataKey;

    private static final Cache<UUID, Boolean> settingsCache = Caffeine.newBuilder()
        .maximumSize(512)
        .expireAfterWrite(5, TimeUnit.SECONDS)
        .build();

    public BlockEventListener(@NotNull BlockProt blockProt) {
        this.blockProt = blockProt;
        this.shulkerDataKey = new NamespacedKey(blockProt, "shulker_data");
    }

    public static void invalidateSettings(@NotNull UUID uuid) {
        settingsCache.invalidate(uuid);
    }

    public static void invalidateAllSettings() {
        settingsCache.invalidateAll();
    }

    private boolean getLockOnPlace(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        Boolean cached = settingsCache.getIfPresent(uuid);
        if (cached != null) return cached;
        boolean value = new PlayerSettingsHandler(player).getLockOnPlace();
        settingsCache.put(uuid, value);
        return value;
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if (BlockProt.getDefaultConfig().isWorldExcluded(event.getBlock().getWorld())) return;
        if (!BlockProt.getDefaultConfig().isLockable(event.getBlock().getType(), event.getBlock().getWorld())) return;
        if (!ProtectedBlockCache.isProtected(event.getBlock())) return;
        BlockNBTHandler handler = new BlockNBTHandler(event.getBlock());
        if (handler.isProtected()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (BlockProt.getDefaultConfig().isWorldExcluded(event.getBlock().getWorld())) return;
        if (!BlockProt.getDefaultConfig().isLockable(event.getBlock().getType(), event.getBlock().getWorld())) return;

        if (!ProtectedBlockCache.isProtected(event.getBlock())) {
            HopperEventListener.invalidate(event.getBlock());
            return;
        }

        BlockNBTHandler handler = new BlockNBTHandler(event.getBlock());
        final Player breaker = event.getPlayer();
        final boolean isAdmin = de.sean.blockprot.bukkit.admin.AdminTierManager.hasPermission(breaker, de.sean.blockprot.bukkit.admin.AdminAction.BREAK);
        final boolean isOwner = handler.isOwner(breaker.getUniqueId().toString());

        if (!isOwner && handler.isProtected()) {
            if (!isAdmin && !BlockProt.getDefaultConfig().shouldAllowBreakProtectedBlocks()) {
                event.setCancelled(true);
                de.sean.blockprot.bukkit.audit.AuditLogger audit = BlockProt.getAuditLogger();
                if (audit != null) {
                    audit.log(breaker.getUniqueId(), breaker.getName(), event.getBlock().getLocation(), de.sean.blockprot.bukkit.audit.AuditLogger.Action.ACCESS_DENIED);
                }
            }
        }

        if (!event.isCancelled()) {
            cleanupBreak(event.getBlock(), handler);
        }
    }

    private void cleanupBreak(@NotNull Block block, @NotNull BlockNBTHandler handler) {
        String ownerUuid = handler.getOwner();
        if (!ownerUuid.isBlank()) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerUuid));
            if (owner.isOnline() && owner.getPlayer() != null) {
                StatHandler.removeContainer(owner.getPlayer(), block);
            } else {
                PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
                StatHandler.getStatisticByUuid(stat, UUID.fromString(ownerUuid));
                stat.remove(block.getLocation());
                BlockCountStatistic countStat = new BlockCountStatistic();
                StatHandler.getStatistic(countStat);
                countStat.decrement();
                StatHandler.markDirty();
            }
        }
        HopperEventListener.invalidate(block);
        handler.clear();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAutoDropBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!BlockProt.getDefaultConfig().isAutoDropToInventory(block.getType())) return;
        if (!BlockProt.getDefaultConfig().isAutoDropToInventoryEnabled(block.getWorld())) return;
        if (BlockProt.getDefaultConfig().isLockableShulkerBox(block.getType(), block.getWorld())) return;
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        // Blocked break attempts must not deliver the item before the break is cancelled.
        BlockNBTHandler handler = null;
        if (BlockProt.getDefaultConfig().isLockable(block.getType(), block.getWorld()) && ProtectedBlockCache.isProtected(block)) {
            try {
                handler = new BlockNBTHandler(block);
            } catch (RuntimeException e) {
                return;
            }
            if (handler.isProtected()
                && !handler.isOwner(player.getUniqueId().toString())
                && !de.sean.blockprot.bukkit.admin.AdminTierManager.hasPermission(player, de.sean.blockprot.bukkit.admin.AdminAction.BREAK)
                && !BlockProt.getDefaultConfig().shouldAllowBreakProtectedBlocks()) {
                return;
            }
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        List<ItemStack> items = new ArrayList<>(block.getDrops(tool));

        BlockState state = block.getState();
        if (state instanceof Container container) {
            org.bukkit.inventory.Inventory snapshot = container.getSnapshotInventory();
            if (snapshot != null) {
                for (ItemStack content : snapshot.getContents()) {
                    if (content != null && !content.getType().isAir()) {
                        items.add(content);
                    }
                }
            }
        }

        Block partner = findDoubleBlockPartner(block);
        if (items.isEmpty() && partner != null) {
            items.addAll(partner.getDrops(tool));
        }

        if (items.isEmpty()) return;

        event.setCancelled(true);
        if (handler != null) {
            cleanupBreak(block, handler);
        }
        if (partner != null) {
            partner.setType(Material.AIR, false);
        }
        block.setType(Material.AIR, true);

        for (ItemStack item : items) {
            java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            for (ItemStack overflow : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), overflow);
            }
        }
    }

    private Block findDoubleBlockPartner(@NotNull Block block) {
        org.bukkit.block.data.BlockData data = block.getBlockData();
        if (data instanceof Bed bed) {
            BlockFace facing = bed.getFacing();
            Block other = block.getRelative(bed.getPart() == Bed.Part.HEAD ? facing.getOppositeFace() : facing);
            if (other.getType() == block.getType()
                && other.getBlockData() instanceof Bed otherBed
                && otherBed.getPart() != bed.getPart()) {
                return other;
            }
        } else if (data instanceof Bisected bisected) {
            Block other = block.getRelative(bisected.getHalf() == Bisected.Half.TOP ? BlockFace.DOWN : BlockFace.UP);
            if (other.getType() == block.getType()
                && other.getBlockData() instanceof Bisected otherBisected
                && otherBisected.getHalf() != bisected.getHalf()) {
                return other;
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onShulkerAutoDropBlockBreak(BlockBreakEvent event) {
        if (BlockProt.getDefaultConfig().isWorldExcluded(event.getBlock().getWorld())) return;
        if (!BlockProt.getDefaultConfig().isLockableShulkerBox(event.getBlock().getType(), event.getBlock().getWorld())) return;

        if (!(event.getBlock().getState() instanceof org.bukkit.block.TileState)) return;

        BlockNBTHandler handler;
        try {
            handler = new BlockNBTHandler(event.getBlock());
        } catch (RuntimeException e) {
            return;
        }

        final Player shulkerBreaker = event.getPlayer();
        final boolean isShulkerAdmin = de.sean.blockprot.bukkit.admin.AdminTierManager.hasPermission(shulkerBreaker, de.sean.blockprot.bukkit.admin.AdminAction.BREAK);
        final boolean isShulkerOwner = handler.isOwner(shulkerBreaker.getUniqueId().toString());

        if ((isShulkerOwner || isShulkerAdmin) && (!event.isCancelled() && event.isDropItems() && shulkerBreaker.getGameMode() != GameMode.CREATIVE)) {
            StatHandler.removeContainer(shulkerBreaker, event.getBlock());
            HopperEventListener.invalidate(event.getBlock());
            event.setDropItems(false);
            Collection<ItemStack> itemsToDrop = event.getBlock().getDrops();
            if (itemsToDrop.isEmpty()) return;

            var item = Iterables.getFirst(itemsToDrop, null);
            if (item == null) return;

            boolean clearProtection = isShulkerAdmin && !isShulkerOwner
                ? true
                : BlockProt.getDefaultConfig().shouldClearProtectionOnShulkerBreak();
            // Respect both the global feature flag AND the per-material list.
            boolean autoDropEnabled = BlockProt.getDefaultConfig().isAutoDropToInventoryEnabled(event.getBlock().getWorld())
                && BlockProt.getDefaultConfig().isAutoDropToInventory(event.getBlock().getType());

            if (!clearProtection) {
                if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_20_R4)) {
                    final var meta = item.getItemMeta();
                    final var pdc = meta.getPersistentDataContainer();
                    pdc.set(shulkerDataKey, PersistentDataType.STRING, "Hi!");
                    item.setItemMeta(meta);

                    final var nbt = NBT.itemStackToNBT(item);
                    final var entityData = nbt.getOrCreateCompound("components").getOrCreateCompound("minecraft:block_entity_data");
                    entityData.setString("id", "minecraft:shulker_box");
                    entityData.getOrCreateCompound("PublicBukkitValues").mergeCompound(handler.getNbtCopy());
                    item = Objects.requireNonNull(NBT.itemStackFromNBT(nbt));
                } else {
                    NBT.modify(item, readWriteItemNBT -> {
                        readWriteItemNBT.getOrCreateCompound("BlockEntityTag").getOrCreateCompound("PublicBukkitValues").mergeCompound(handler.getNbtCopy());
                    });
                }
            }

            event.getBlock().setType(Material.AIR);
            event.setCancelled(true);

            if (autoDropEnabled) {
                java.util.HashMap<Integer, ItemStack> leftover = shulkerBreaker.getInventory().addItem(item);
                for (ItemStack overflow : leftover.values()) {
                    shulkerBreaker.getWorld().dropItemNaturally(shulkerBreaker.getLocation(), overflow);
                }
            } else {
                shulkerBreaker.getWorld().dropItemNaturally(event.getBlock().getLocation(), item);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (BlockProt.getDefaultConfig().isWorldExcluded(event.getBlock().getWorld())) return;
        if (!event.getPlayer().hasPermission(Permissions.USER.key())) return;
        if (!BlockProt.getDefaultConfig().isLockable(event.getBlock().getType(), event.getBlock().getWorld())) return;

        Block block = event.getBlockPlaced();
        String playerUuid = event.getPlayer().getUniqueId().toString();
        BlockNBTHandler handler = new BlockNBTHandler(block);

        if (handler.isNotProtected()) {

            if (getLockOnPlace(event.getPlayer()) && !event.getPlayer().isSneaking()) {
                BlockLockOnPlaceEvent lockOnPlaceEvent = new BlockLockOnPlaceEvent(event.getBlock(), event.getPlayer());

                Bukkit.getPluginManager().callEvent(lockOnPlaceEvent);
                if (!lockOnPlaceEvent.isCancelled()) {
                    LockReturnValue lock = handler.lockBlock(event.getPlayer(), BlockProtLockEvent.Cause.LOCK_ON_PLACE);
                    if (!lock.success) {
                        event.setCancelled(true);
                        if (lock.reason != null) {
                            ComponentMessages.sendActionBar(event.getPlayer(),
                            LegacyComponentSerializer.legacySection().deserialize(Translator.get(lock.reason)));
                        }
                        return;
                    }

                    new PlayerSettingsHandler(event.getPlayer()).getFriendsStream()
                        .filter(fh -> PluginIntegration.filterFriendByUuidForAll(UUID.fromString(fh.getName()), event.getPlayer(), block))
                        .forEach(handler::addFriend);

                    ComponentMessages.sendActionBar(event.getPlayer(),
                        LegacyComponentSerializer.legacySection().deserialize(
                            Translator.get(TranslationKey.MESSAGES__LOCK_ON_PLACE_SUCCESS)
                        )
                    );
                }

                if (BlockProt.getDefaultConfig().disallowRedstoneOnPlace()) {
                    handler.getRedstoneHandler().setAll(false);
                }
            }

            BlockProt.getFoliaLib().getScheduler().runAtLocationLater(
                block.getLocation(),
                () -> {
                    if (BlockFamilyParser.getSubFamilyMembers(BlockFamilyParser.SubFamily.CHEST).contains(block.getType())) {
                        final BlockState doubleChestState = BlockUtil.getDoubleChest(block);
                        if (doubleChestState != null) {
                            final BlockNBTHandler doubleChestHandler = new BlockNBTHandler(doubleChestState.getBlock());
                            if (doubleChestHandler.isNotProtected() || doubleChestHandler.isOwner(playerUuid)) {
                                handler.mergeHandler(doubleChestHandler);
                            } else {
                                event.getPlayer().getWorld().getBlockAt(block.getLocation()).breakNaturally();
                            }

                            StatHandler.removeContainer(event.getPlayer(), block);
                        }
                    } else {
                        BlockState freshState = VersionCompat.getBlockState(block, true);
                        if (freshState instanceof org.bukkit.block.TileState) {
                            handler.setName(BlockUtil.getHumanReadableBlockName(block.getType()));
                            handler.applyToOtherContainer();
                        }
                    }
                },
                1
            );
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPhysics(@NotNull final BlockPhysicsEvent event) {
        if (event.getChangedType().toString().contains("ANVIL") &&
            BlockProt.getDefaultConfig().isLockableBlock(event.getChangedType(), event.getBlock().getWorld())) {
            BlockNBTHandler handler = new BlockNBTHandler(event.getBlock());

            if (handler.isProtected())
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDispense(@NotNull BlockDispenseEvent event) {
        if (BlockProt.getDefaultConfig().isWorldExcluded(event.getBlock().getWorld())) return;
        org.bukkit.block.BlockFace facing = null;
        try {
            org.bukkit.block.data.Directional dir =
                (org.bukkit.block.data.Directional) event.getBlock().getBlockData();
            facing = dir.getFacing();
        } catch (ClassCastException ignored) { return; }
        if (facing == null) return;
        org.bukkit.block.Block target = event.getBlock().getRelative(facing);
        if (!BlockProt.getDefaultConfig().isLockable(target.getType(), target.getWorld())) return;
        try {
            BlockNBTHandler handler = new BlockNBTHandler(target);
            if (handler.isProtected()) event.setCancelled(true);
        } catch (RuntimeException ignored) {}
    }

    @EventHandler
    public void onPlayerQuit(@NotNull final PlayerQuitEvent event) {
        settingsCache.invalidate(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onSignChanged(@NotNull final SignChangeEvent event) {
        final Block block = event.getBlock();
        if (!BlockProt.getDefaultConfig().isLockable(block.getType(), block.getWorld())) return;
        final var handler = new BlockNBTHandler(block);
        if (!handler.isProtected()) return;

        final Player player = event.getPlayer();
        final String playerUuid = player.getUniqueId().toString();
        if (handler.isOwner(playerUuid) || player.hasPermission(Permissions.USER_ADMIN.key())) return;

        final var friend = handler.getFriend(playerUuid);
        if (friend.isPresent() && friend.get().canWrite()) return;

        event.setCancelled(true);
    }
}