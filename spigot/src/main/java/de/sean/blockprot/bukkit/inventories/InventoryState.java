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

package de.sean.blockprot.bukkit.inventories;

import de.sean.blockprot.bukkit.events.BlockAccessMenuEvent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Storage for the current state and location of each player's
 * interactions with this plugin's menus.
 *
 * @since 0.1.9
 */
public final class InventoryState {
    private static final ConcurrentHashMap<String, InventoryState> players = new ConcurrentHashMap<>();

    @NotNull
    public final ArrayList<UUID> friendResultCache = new ArrayList<>();

    @Nullable
    private final UUID blockWorldUid;
    private final int blockX;
    private final int blockY;
    private final int blockZ;

    @Nullable
    private UUID entityProtectionId = null;

    @Nullable
    public UUID entityUUID = null;

    @NotNull
    public FriendSearchState friendSearchState = FriendSearchState.FRIEND_SEARCH;

    public int currentPageIndex = 0;

    @Nullable
    public UUID currentFriend = null;

    @NotNull
    public Set<BlockAccessMenuEvent.MenuPermission> menuPermissions = new HashSet<>();

    public boolean remoteLockPendingConfirm = false;

    /**
     * Set right before an inventory is intentionally closed to prompt a chat
     * input (see {@code TextInput#open}), so the close listener does not
     * mistake the resulting {@code InventoryCloseEvent} for a player-initiated
     * ESC/E/X close and reopen the parent menu underneath the chat prompt.
     * Consumed (reset to false) by the listener the first time it is checked.
     */
    public boolean suppressCloseReopen = false;

    @NotNull
    public MenuOrigin origin = MenuOrigin.NONE;

    /**
     * Stack of menus the player navigated through. Each menu pushes its own
     * {@link MenuOrigin} before opening a submenu; the back button pops it so
     * navigation always returns to the actual parent window, regardless of how
     * deep the player went. Empty when the menu chain is a single level (the
     * {@link #origin} field alone describes the parent).
     */
    @NotNull
    public final ArrayDeque<MenuOrigin> originStack = new ArrayDeque<>();

    /** Pushes a menu onto the navigation stack. */
    public void pushOrigin(@NotNull MenuOrigin origin) {
        originStack.push(origin);
    }

    /** Pops the last pushed menu; {@link MenuOrigin#NONE} when the stack is empty. */
    @NotNull
    public MenuOrigin popOrigin() {
        return originStack.isEmpty() ? MenuOrigin.NONE : originStack.pop();
    }

    public InventoryState(@Nullable Block block) {
        if (block != null && block.getWorld() != null) {
            this.blockWorldUid = block.getWorld().getUID();
            this.blockX = block.getX();
            this.blockY = block.getY();
            this.blockZ = block.getZ();
        } else {
            this.blockWorldUid = null;
            this.blockX = 0;
            this.blockY = 0;
            this.blockZ = 0;
        }
    }

    public static void clear() {
        players.clear();
    }

    public static void set(String player, InventoryState state) {
        players.put(player, state);
    }

    public static void set(UUID player, InventoryState state) {
        players.put(player.toString(), state);
    }

    public static InventoryState get(String player) {
        return players.get(player);
    }

    public static InventoryState get(UUID player) {
        return players.get(player.toString());
    }

    @NotNull
    public static InventoryState getOrCreate(@NotNull UUID player) {
        InventoryState existing = players.get(player.toString());
        if (existing != null) return existing;
        InventoryState fresh = new InventoryState(null);
        players.put(player.toString(), fresh);
        return fresh;
    }

    public static void remove(String player) {
        players.remove(player);
    }

    public static void remove(UUID player) {
        players.remove(player.toString());
    }

    @Nullable
    public Block getBlock() {
        if (this.blockWorldUid == null) return null;
        World world = Bukkit.getWorld(this.blockWorldUid);
        return world != null ? world.getBlockAt(this.blockX, this.blockY, this.blockZ) : null;
    }

    @Nullable
    public UUID getEntityProtectionId() {
        return entityProtectionId;
    }

    public void setEntityProtectionId(@Nullable UUID id) {
        this.entityProtectionId = id;
    }

    /**
     * The current search state of the friend menu.
     *
     * @since 0.1.9
     */
    public enum FriendSearchState {
        /** This search is currently for a single block. @since 0.1.9 */
        FRIEND_SEARCH,
        /** This search is currently for the default friends of a player. @since 0.1.9 */
        DEFAULT_FRIEND_SEARCH,
    }

    /** Identifies which menu opened the current one, driving back-button behaviour. */
    public enum MenuOrigin {
        NONE,
        BLOCK_LOCK,
        USER_MENU,
        ADMIN_MENU,
        FRIEND_MANAGE,
        STATISTICS,
        USER_SETTINGS,
        LOCKABLES,
        AUTO_DROP,
        WORLD_LOCKABLE_SELECTION,
        PLAYER_LIST,
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Block block = null;
        private MenuOrigin origin = MenuOrigin.NONE;
        private FriendSearchState friendSearchState = FriendSearchState.FRIEND_SEARCH;

        private Builder() {}

        public Builder block(@org.jetbrains.annotations.Nullable Block block) { this.block = block; return this; }
        public Builder origin(@NotNull MenuOrigin origin) { this.origin = origin; return this; }
        public Builder friendSearchState(@NotNull FriendSearchState state) { this.friendSearchState = state; return this; }

        @NotNull
        public InventoryState build() {
            InventoryState s = new InventoryState(block);
            s.origin = origin;
            s.friendSearchState = friendSearchState;
            return s;
        }
    }
}