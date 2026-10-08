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

package de.sean.blockprot.bukkit.nbt;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.admin.AdminAction;
import de.sean.blockprot.bukkit.admin.AdminTierManager;
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.events.BlockProtLockEvent;
import de.sean.blockprot.bukkit.events.BlockProtUnlockEvent;
import de.sean.blockprot.bukkit.listeners.HopperEventListener;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import de.sean.blockprot.bukkit.storage.HybridDatabase;
import de.sean.blockprot.bukkit.storage.ProtectedBlockCache;
import de.sean.blockprot.bukkit.util.AsyncGuard;
import de.sean.blockprot.bukkit.util.BlockUtil;
import de.sean.blockprot.nbt.FriendModifyAction;
import de.sean.blockprot.nbt.LockReturnValue;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTBlock;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTTileEntity;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A block handler to get values and settings from a single lockable
 * block.
 *
 * @since 0.2.3
 */
public final class BlockNBTHandler extends FriendSupportingHandler<NBTCompound> {
    static final String OWNER_ATTRIBUTE = "splugin_owner";

    static final String LOCK_ATTRIBUTE = "blockprot_friends";

    static final String REDSTONE_ATTRIBUTE = "blockprot_redstone";

    static final String NAME_ATTRIBUTE = "blockprot_name";
    static final String LOCKED_AT_ATTRIBUTE = "blockprot_locked_at";
    static final String LINKED_ITEM_FRAME_ATTRIBUTE = "blockprot_linked_item_frame";

    /**
     * The backing block this handler handles.
     *
     * @since 0.2.3
     */
    @NotNull
    public final Block block;

    private final boolean isTileEntity;

    /**
     * Gets a live {@link BlockState} for NBT modification. {@code Block.getState(boolean)}
     * is a Paper-only overload; on Spigot it falls back to the no-arg {@code getState()}.
     */
    @NotNull
    private static BlockState getLiveState(@NotNull final Block block) {
        try {
            return block.getState(true);
        } catch (Throwable ignored) {
            return block.getState();
        }
    }

    /**
     * Create a new handler for given {@code block}.
     *
     * @param block The block we want to use and get the
     *              NBT container for.
     * @throws RuntimeException if {@code block} is not a lockable block
     *                          or lockable tile entity.
     * @since 0.2.3
     */
    @SuppressWarnings("deprecation")
    public BlockNBTHandler(@NotNull final Block block) throws RuntimeException {
        super(LOCK_ATTRIBUTE);
        AsyncGuard.assertBlockAccess(block, "BlockNBTHandler.<init>");
        this.block = block;

        if (BlockProt.getDefaultConfig().isLockableBlock(this.block.getType(), this.block.getWorld())) {
            isTileEntity = false;
            container = new NBTBlock(block).getData();
        } else if (BlockProt.getDefaultConfig().isLockableTileEntity(this.block.getType(), this.block.getWorld())) {
            isTileEntity = true;
            container = new NBTTileEntity(getLiveState(block)).getPersistentDataContainer();
        } else {
            throw new RuntimeException("Given block " + block.getType() + " is not a lockable block/tile entity");
        }
    }

    /**
     * Overridden to avoid N intermediate flushes to the tile entity when
     * setting the full friend list. Performs all mutations in-memory and
     * calls {@link #onFriendsMutated()} exactly once at the end.
     */
    @Override
    public void setFriends(@NotNull List<FriendHandler> friends) {
        container.removeKey(LOCK_ATTRIBUTE);
        for (FriendHandler f : friends) {
            container.getOrCreateCompound(LOCK_ATTRIBUTE)
                     .addCompound(f.getName())
                     .mergeCompound(f.container);
        }
        onFriendsMutated();
    }

    @Override
    protected void onFriendsMutated() {
        if (isTileEntity) {
            final String friendsKey = LOCK_ATTRIBUTE;
            NBT.modify(getLiveState(block), nbt -> {
                if (container.hasTag(friendsKey)) {
                    nbt.getOrCreateCompound(friendsKey).mergeCompound(
                        container.getOrCreateCompound(friendsKey));
                } else {
                    nbt.removeKey(friendsKey);
                }
            });
        }
    }

    @NotNull
    public String getOwner() {
        if (!container.hasTag(OWNER_ATTRIBUTE)) return "";
        else return container.getString(OWNER_ATTRIBUTE);
    }

    /**
     * Set the current owner of this block.
     *
     * @param owner The new owner for this block. Should
     *              be a valid UUID.
     * @since 0.2.3
     */
    public void setOwner(@NotNull final String owner) {
        if (isTileEntity) {
            NBT.modify(getLiveState(block), nbt -> {
                nbt.setString(OWNER_ATTRIBUTE, owner);
            });
        }
        container.setString(OWNER_ATTRIBUTE, owner);
    }

    /**
     * Gets the redstone settings handler for this block. Will remap
     * any legacy redstone settings to the new system.
     *
     * @return The redstone settings handler.
     * @since 0.4.13
     */
    public @NotNull RedstoneSettingsHandler getRedstoneHandler() {
        return new RedstoneSettingsHandler(
            container.getOrCreateCompound(REDSTONE_ATTRIBUTE));
    }

    /**
     * Whether or not this block is protected. This is evaluated by checking
     * if an owner exists and if any friends have been added to the block.
     *
     * @return True, if this block is not protected and there is no owner.
     * @since 0.2.3
     */
    public boolean isNotProtected() {
        return getOwner().isEmpty() && getFriends().isEmpty();
    }

    /**
     * @return True, if this block is protected.
     * @see #isNotProtected()
     * @since 0.2.3
     */
    public boolean isProtected() {
        return !isNotProtected();
    }

    /**
     * Checks whether given {@code player} is the owner of this block.
     *
     * @param player A String representing a players UUID.
     * @return Whether {@code player} is the owner of this block.
     * @since 0.2.3
     */
    public boolean isOwner(@NotNull final String player) {
        return getOwner().equals(player);
    }

    /**
     * Checks whether given {@code player} is the owner of this block.
     * @param player The player's UUID.
     * @since 1.1.7
     */
    public boolean isOwner(@NotNull final UUID player) {
        return getOwner().equals(player.toString());
    }

    public @NotNull String getName() {
        if (!container.hasTag(NAME_ATTRIBUTE))
            return block.getType().toString();
        return container.getString(NAME_ATTRIBUTE);
    }

    public void setName(@NotNull String name) {
        if (isTileEntity) {
            NBT.modify(getLiveState(block), nbt -> {
                nbt.setString(NAME_ATTRIBUTE, name);
            });
        }
        container.setString(NAME_ATTRIBUTE, name);
    }

    /** Returns the epoch-millis when this block was first locked, or -1 if not recorded. */
    public long getLockedAt() {
        if (!container.hasTag(LOCKED_AT_ATTRIBUTE)) return -1L;
        return container.getLong(LOCKED_AT_ATTRIBUTE);
    }

    // Item frame link (inverse of EntityNBTHandler#getLinkedBlock)

    /**
     * Returns the UUID string of the item frame linked to this block (e.g. an
     * item frame mounted on this chest), or an empty string if none is linked.
     * A chest with a linked frame is treated as a single protection unit: the
     * frame has no separate lock/friends UI, it shares this block's.
     */
    @NotNull
    public String getLinkedItemFrameUuid() {
        if (!container.hasTag(LINKED_ITEM_FRAME_ATTRIBUTE)) return "";
        return container.getString(LINKED_ITEM_FRAME_ATTRIBUTE);
    }

    public void setLinkedItemFrameUuid(@NotNull String uuid) {
        if (isTileEntity) {
            NBT.modify(getLiveState(block), nbt -> {
                nbt.setString(LINKED_ITEM_FRAME_ATTRIBUTE, uuid);
            });
        }
        container.setString(LINKED_ITEM_FRAME_ATTRIBUTE, uuid);
    }

    public void clearLinkedItemFrameUuid() {
        if (isTileEntity) {
            NBT.modify(getLiveState(block), nbt -> {
                nbt.removeKey(LINKED_ITEM_FRAME_ATTRIBUTE);
            });
        }
        container.removeKey(LINKED_ITEM_FRAME_ATTRIBUTE);
    }

    public boolean hasLinkedItemFrame() {
        return !getLinkedItemFrameUuid().isEmpty();
    }

    /**
     * Checks whether given {@code player} can access this block. If possible, it's
     * always recommended to use {@link #canAccess(FriendHandler)}.
     *
     * @see #canAccess(FriendHandler)
     * @param player The player to check for.
     * @return True, if {@code player} can access this block.
     * @since 0.2.3
     */
    public boolean canAccess(@NotNull final String player) {
        Optional<FriendHandler> friend = getFriend(player);
        if (isNotProtected() || getOwner().equals(player)) return true;
        if (friend.isEmpty()) return false;
        FriendHandler fh = friend.get();
        if (fh.getLevel() != FriendHandler.LEVEL_CUSTOM) return true;
        if (block != null && BlockProt.getDefaultConfig().isLockableDoor(block.getType(), block.getWorld())) {
            return fh.canOpenContainer() || fh.canUseRedstone();
        }
        return fh.canOpenContainer();
    }

    /**
     * Checks whether given {@code friend} can access this block. This does not
     * guarantee that the {@code friend} is also allowed to manage the block
     * or take items from it.
     *
     * @param friend The {@link FriendHandler} to evaluate.
     * @return {@code true} if the block is unprotected or {@code friend} has read permission.
     */
    public boolean canAccess(@NotNull final FriendHandler friend) {
        if (isNotProtected()) return true;
        if (friend.getLevel() != FriendHandler.LEVEL_CUSTOM) return true;
        if (block != null && BlockProt.getDefaultConfig().isLockableDoor(block.getType(), block.getWorld())) {
            return friend.canOpenContainer() || friend.canUseRedstone();
        }
        return friend.canOpenContainer();
    }

    /**
     * Returns true if the given string is NOT a valid non-negative or negative decimal number.
     *
     * <p>Fixed: the original loop used {@code for(i = x ? 0 : -1; ++i < len)} which skipped
     * index 0 entirely when the first char was NOT '-', causing single-digit permission values
     * like "5" to be misclassified as non-numeric.
     *
     * @param string The string to check.
     * @return {@code true} if {@code string} is not numeric.
     */
    public boolean isNotNumeric(String string) {
        if (string == null || string.isEmpty()) return true;
        // Allow an optional leading '-' for negative values.
        int start = string.charAt(0) == '-' ? 1 : 0;
        // A bare "-" with nothing after it is not numeric.
        if (start >= string.length()) return true;
        for (int i = start; i < string.length(); i++) {
            final char c = string.charAt(i);
            if (!Character.isDigit(c) && c != '.') return true;
        }
        return false;
    }

    /**
     * Locks this block for given {@code player} as the owner.
     *
     * @param player The player to set as an owner.
     * @return A {@link LockReturnValue} whether the block was successfully locked,
     * else there might have been issues with permissions.
     * @since 0.4.6
     */
    @NotNull
    public LockReturnValue lockBlock(@NotNull final Player player) {
        return lockBlock(player, BlockProtLockEvent.Cause.MANUAL);
    }

    @NotNull
    public LockReturnValue lockBlock(@NotNull final Player player, @NotNull final BlockProtLockEvent.Cause cause) {
        String owner = getOwner();
        final String playerUuid = player.getUniqueId().toString();

        LockReturnValue maxCheck = checkMaxBlockCount(player, owner);
        if (maxCheck != null) return maxCheck;

        if (owner.isEmpty()) {
            // Respect spawn-protection radius from server.properties
            if (BlockProt.getDefaultConfig().shouldRespectSpawnProtection()
                    && !player.isOp() && !player.hasPermission(Permissions.USER_ADMIN.key())) {
                int spawnRadius = org.bukkit.Bukkit.getServer().getSpawnRadius();
                if (spawnRadius > 0) {
                    org.bukkit.Location spawn = block.getWorld().getSpawnLocation();
                    double dx = block.getX() - spawn.getBlockX();
                    double dz = block.getZ() - spawn.getBlockZ();
                    if ((dx * dx + dz * dz) <= (double)(spawnRadius * spawnRadius)) {
                        return new LockReturnValue(false, LockReturnValue.Reason.NO_PERMISSION);
                    }
                }
            }

            BlockProtLockEvent event = new BlockProtLockEvent(block, player, cause);
            BlockProt.getInstance().getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return new LockReturnValue(false, LockReturnValue.Reason.NO_PERMISSION);
            }

            setOwner(playerUuid);
            container.setLong(LOCKED_AT_ATTRIBUTE, System.currentTimeMillis());
            this.applyToOtherContainer();
            StatHandler.addBlock(player, block.getLocation());
            HybridDatabase hybridDatabase = BlockProt.getHybridDatabase();
            if (hybridDatabase != null) {
                hybridDatabase.upsertBlockIndex(player.getUniqueId(), block.getLocation(), block.getType().name());
            }
            ProtectedBlockCache.mark(block);
            HopperEventListener.invalidate(block);
            return new LockReturnValue(true, null);
        } else if (owner.equals(playerUuid) || AdminTierManager.hasPermission(player, AdminAction.UNLOCK)) {
            return performUnlock(player);
        }

        return new LockReturnValue(false, LockReturnValue.Reason.NO_PERMISSION);
    }

    private LockReturnValue checkMaxBlockCount(@NotNull final Player player, String owner) {
        if (!owner.isEmpty()) return null;
        Integer maxBlockCount = BlockProt.getDefaultConfig().getMaxLockedBlockCount();
        if (maxBlockCount == null) return null;

        PlayerBlocksStatistic playerBlocksStatistic = new PlayerBlocksStatistic();
        StatHandler.getStatistic(playerBlocksStatistic, player);
        if (player.hasPermission(Permissions.LOCKMAX.key())) {
            // Use streams with parseInt try-catch instead of manual ArrayList copy + linear scan.
            // On servers with 200-500 permission nodes (LuckPerms inheritance chains) this avoids
            // creating a full copy of the effective-permissions collection on every lock.
            java.util.OptionalInt limit = player.getEffectivePermissions().stream()
                .filter(p -> p.getValue()
                    && p.getPermission().toLowerCase().startsWith("blockprot.locklimit."))
                .mapToInt(p -> {
                    try {
                        return Integer.parseInt(
                            p.getPermission().substring("blockprot.locklimit.".length()));
                    } catch (NumberFormatException e) {
                        return -1;
                    }
                })
                .filter(v -> v >= 0)
                .max();

            if (limit.isPresent() && playerBlocksStatistic.get().size() >= limit.getAsInt()) {
                return new LockReturnValue(false, LockReturnValue.Reason.EXCEEDED_MAX_BLOCK_COUNT);
            }
        } else if (playerBlocksStatistic.get().size() >= maxBlockCount) {
            return new LockReturnValue(false, LockReturnValue.Reason.EXCEEDED_MAX_BLOCK_COUNT);
        }
        return null;
    }

    private LockReturnValue performUnlock(@NotNull final Player player) {
        BlockProtUnlockEvent event = new BlockProtUnlockEvent(block, player, BlockProtUnlockEvent.Cause.MANUAL);
        BlockProt.getInstance().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return new LockReturnValue(false, LockReturnValue.Reason.NO_PERMISSION);
        }

        StatHandler.removeContainer(player, block);
        HybridDatabase hybridDatabase = BlockProt.getHybridDatabase();
        if (hybridDatabase != null) {
            hybridDatabase.removeBlockIndex(block.getLocation());
        }
        this.clear();
        this.applyToOtherContainer();
        ProtectedBlockCache.unmark(block);
        HopperEventListener.invalidate(block);
        return new LockReturnValue(true, null);
    }

    /**
     * Transfers ownership of this block from {@code currentOwner} to {@code newOwner}.
     * The old owner is automatically added as a regular friend so they retain access.
     * Requires that the caller is the current owner or has admin permission.
     *
     * @param currentOwnerUuid UUID string of the current owner.
     * @param newOwnerUuid     UUID string of the player to transfer ownership to.
     * @return A {@link LockReturnValue} indicating success or the reason for failure.
     * @since 1.2.0
     */
    @NotNull
    public LockReturnValue transferOwner(@NotNull final String currentOwnerUuid, @NotNull final String newOwnerUuid) {
        if (!isOwner(currentOwnerUuid)) return new LockReturnValue(false, LockReturnValue.Reason.NO_PERMISSION);
        if (currentOwnerUuid.equals(newOwnerUuid)) return new LockReturnValue(false, LockReturnValue.Reason.NO_PERMISSION);
        // Keep old owner as a friend so they can still access the block.
        if (!containsFriend(currentOwnerUuid)) addFriend(currentOwnerUuid);
        setOwner(newOwnerUuid);
        // Remove the new owner from the friend list if present (they are now the owner).
        if (containsFriend(newOwnerUuid)) removeFriend(newOwnerUuid);
        applyToOtherContainer();
        UUID newOwner = UUID.fromString(newOwnerUuid);
        StatHandler.removeContainerByUuid(UUID.fromString(currentOwnerUuid), block.getLocation());
        StatHandler.addBlockByUuid(newOwner, block.getLocation());
        HybridDatabase hybridDatabase = BlockProt.getHybridDatabase();
        if (hybridDatabase != null) {
            hybridDatabase.upsertBlockIndex(newOwner, block.getLocation(), block.getType().name());
        }
        return new LockReturnValue(true, null);
    }

    /**
     * Modifies the friends of this block for given {@code action}.
     *
     * @param player The player requesting this command, should be the owner.
     * @param friend The friend do to {@code action} with.
     * @param action The action we should perform with {@code friend} on this block.
     * @return A {@link LockReturnValue} whether or not the friends were modified
     * successfully.
     * @since 0.4.6
     */
    @NotNull
    public LockReturnValue modifyFriends(@NotNull final String player, @NotNull final String friend, @NotNull final FriendModifyAction action) {
        // This theoretically shouldn't happen, though we will still check for it just to be sure
        if (!isOwner(player)) return new LockReturnValue(
            false, null
        );

        switch (action) {
            case ADD_FRIEND -> {
                if (containsFriend(friend)) {
                    return new LockReturnValue(false, null);
                } else {
                    addFriend(friend);
                    this.applyToOtherContainer();
                    return new LockReturnValue(true, null);
                }
            }
            case REMOVE_FRIEND -> {
                if (containsFriend(friend)) {
                    removeFriend(friend);
                    this.applyToOtherContainer();
                    return new LockReturnValue(true, null);
                } else {
                    return new LockReturnValue(false, null);
                }
            }
            default -> {
                return new LockReturnValue(false, null);
            }
        }
    }

    /**
     * @see #applyToOtherContainer(Predicate, Consumer)
     * @since 0.4.6
     */
    public void applyToOtherContainer() {
        this.applyToOtherContainer(handler -> true, handler -> {
        });
    }

    /**
     * This applies any changes to this container to a possible other
     * half. For example doors consist from two blocks, as do double
     * chests. Without this call, all methods will modify only the local,
     * current block.
     * <p>
     * This method is specifically not called on each modification of NBT,
     * as this would be a massive, unnecessary performance penalty.
     *
     * @param condition A predicate defining whether the data should be merged
     *                  over from the given {@link BlockNBTHandler}.
     * @param orElse    If {@code condition} is not true, this function can be used
     *                  as a callback when applying fails.
     * @since 0.4.10
     */
    public void applyToOtherContainer(@NotNull Predicate<BlockNBTHandler> condition, @NotNull Consumer<BlockNBTHandler> orElse) {
        if (BlockProt.getDefaultConfig().isLockableDoor(block.getType(), block.getWorld())) {
            final Block otherDoor = BlockUtil.getOtherDoorHalf(block.getState());
            if (otherDoor == null) return;
            // Guard: the adjacent door half may be in an unloaded chunk on large servers.
            // Constructing BlockNBTHandler on an unloaded chunk causes NbtApiException.
            if (!otherDoor.getChunk().isLoaded()) return;
            final BlockNBTHandler otherDoorHandler = new BlockNBTHandler(otherDoor);
            if (condition.test(otherDoorHandler)) {
                otherDoorHandler.mergeHandler(this);
            } else {
                orElse.accept(otherDoorHandler);
            }
        } else if (BlockFamilyParser.getSubFamilyMembers(BlockFamilyParser.SubFamily.CHEST).contains(this.block.getType())) {
            final BlockState doubleChestState = BlockUtil.getDoubleChest(this.block);
            if (doubleChestState != null) {
                final BlockNBTHandler doubleChestHandler = new BlockNBTHandler(doubleChestState.getBlock());
                if (condition.test(doubleChestHandler)) {
                    doubleChestHandler.mergeHandler(this);
                } else {
                    orElse.accept(doubleChestHandler);
                }
            }
        } else if (BlockFamilyParser.getSubFamilyMembers(BlockFamilyParser.SubFamily.BED).contains(this.block.getType())) {
            final Block otherBedHalf = BlockUtil.getOtherBedHalf(this.block.getState());
            if (otherBedHalf == null) return;
            // Guard: the adjacent bed half may be in an unloaded chunk on large servers.
            if (!otherBedHalf.getChunk().isLoaded()) return;
            final BlockNBTHandler otherBedHandler = new BlockNBTHandler(otherBedHalf);
            if (condition.test(otherBedHandler)) {
                otherBedHandler.mergeHandler(this);
            } else {
                orElse.accept(otherBedHandler);
            }
        }
    }

    /**
     * Clears all values from this block and resets it to the
     * defaults.
     *
     * @since 0.3.2
     */
    public void clear() {
        HybridDatabase hybridDatabase = BlockProt.getHybridDatabase();
        if (hybridDatabase != null) {
            hybridDatabase.removeBlockIndex(block.getLocation());
        }
        this.setOwner("");
        this.setFriends(Collections.emptyList());
        this.getRedstoneHandler().reset();
        unlinkItemFrameIfPresent();
    }

    /**
     * If this block has a linked item frame, clears that frame's own owner/friends
     * (it was sharing this block's protection) and removes the link from both sides.
     * Called whenever this block is unlocked, so the frame never ends up silently
     * still "protected" by a block that no longer exists as a protection unit.
     */
    private void unlinkItemFrameIfPresent() {
        String frameUuid = getLinkedItemFrameUuid();
        if (frameUuid.isEmpty()) return;
        try {
            org.bukkit.entity.Entity frame = org.bukkit.Bukkit.getEntity(java.util.UUID.fromString(frameUuid));
            if (frame != null) {
                de.sean.blockprot.bukkit.nbt.EntityNBTHandler frameHandler = new de.sean.blockprot.bukkit.nbt.EntityNBTHandler(frame);
                frameHandler.clearOwner();
                frameHandler.clearLinkedBlock();
            }
        } catch (IllegalArgumentException ignored) {}
        clearLinkedItemFrameUuid();
    }

    /**
     * Merges this handler with another {@link NBTHandler}.
     *
     * @param handler The handler to merge with. If {@code handler} is not an instance
     *                of {@link BlockNBTHandler}, this will do nothing.
     * @since 0.3.2
     */
    @Override
    public void mergeHandler(@NotNull NBTHandler<?> handler) {
        if (!(handler instanceof final BlockNBTHandler blockNBTHandler)) return;
        this.setOwner(blockNBTHandler.getOwner());
        this.setFriends(blockNBTHandler.getFriends());
        this.getRedstoneHandler().mergeHandler(blockNBTHandler.getRedstoneHandler());
        this.setName(handler.getName());
    }

    @Override
    public void pasteNbt(@NotNull NBTContainer container) {
        // We remove the owner key for security reasons: owner must never change via paste.
        container.removeKey(OWNER_ATTRIBUTE);
        // Clear friends before paste (replace, not append).
        // Matches GitHub #268.
        this.setFriends(java.util.Collections.emptyList());
        super.pasteNbt(container);
        this.applyToOtherContainer();
    }
}
