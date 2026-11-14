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

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.FriendSupportingHandler;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.nbt.FriendModifyAction;
import de.sean.blockprot.nbt.LockReturnValue;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.sean.blockprot.bukkit.util.SkinCache;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import de.sean.blockprot.bukkit.BukkitCompat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A base inventory holder for each of the plugins inventories.
 *
 * @since 0.2.2
 */
@SuppressWarnings("deprecation")
public abstract class BlockProtInventory implements InventoryHolder {
    protected Inventory inventory;

    public BlockProtInventory(boolean createInventory) {
        if (createInventory)
            inventory = createInventory();
    }

    @NotNull
    @Override
    public final Inventory getInventory() {
        return inventory;
    }

    abstract int getSize();

    @Nullable
    abstract String getTranslatedInventoryName();

    @Nullable
    public final String getDefaultInventoryName() {
        final var inventoryName = getTranslatedInventoryName();
        if (inventoryName == null)
            return null;
        return inventoryName.isEmpty() ? this.getClass().getSimpleName() : inventoryName;
    }

    public abstract void onClick(@NotNull final InventoryClickEvent event, @NotNull final InventoryState state);

    public abstract void onClose(@NotNull final InventoryCloseEvent event, @NotNull final InventoryState state);

    @NotNull
    protected final Inventory createInventory() {
        var name = getDefaultInventoryName();
        if (name != null) {
            return ComponentMessages.createInventory(this, getSize(),
                net.kyori.adventure.text.Component.text(stripColors(name)));
        } else {
            return Bukkit.createInventory(this, getSize());
        }
    }

    private static String stripColors(@NotNull String text) {
        return text.replaceAll("[§&][0-9a-fk-orx]", "");
    }

    @NotNull
    protected final Inventory createInventory(@NotNull String title) {
        return ComponentMessages.createInventory(this, getSize(),
            net.kyori.adventure.text.Component.text(stripColors(title)));
    }

    protected final void modifyFriendsForAction(
            @NotNull final Player player,
            @NotNull final UUID friend,
            @NotNull final FriendModifyAction action
    ) {
        applyChanges(
            player,
            (handler) -> handler.modifyFriends(
                player.getUniqueId().toString(),
                friend.toString(),
                action
            ),
            (handler) -> {
                switch (action) {
                    case ADD_FRIEND -> handler.addFriend(friend.toString());
                    case REMOVE_FRIEND -> handler.removeFriend(friend.toString());
                }
            }
        );
    }

    protected void applyChanges(
        @NotNull final Player player,
        @Nullable final Function<BlockNBTHandler, LockReturnValue> onBlockChanges,
        @Nullable final Consumer<PlayerSettingsHandler> onSettingsChanges) {
        InventoryState state = InventoryState.get(player.getUniqueId());
        switch (state.friendSearchState) {
            case FRIEND_SEARCH -> {
                if (onBlockChanges == null) return;
                assert state.getBlock() != null;
                BlockNBTHandler nbtHandler = getNbtHandlerOrNull(state.getBlock());
                if (nbtHandler == null) return;
                LockReturnValue ret = onBlockChanges.apply(nbtHandler);
                if (ret.success)
                    nbtHandler.applyToOtherContainer();
            }
            case DEFAULT_FRIEND_SEARCH -> {
                if (onSettingsChanges == null) return;
                PlayerSettingsHandler settingsHandler = new PlayerSettingsHandler(player);
                onSettingsChanges.accept(settingsHandler);
            }
        }
    }

    protected int findItemIndex(@NotNull final ItemStack item) {
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = contents[i];
            if (stack == null) continue;
            if (stack.equals(item)) return i;
        }
        return -1;
    }

    public void setBackButton() {
        setBackButton(inventory.getSize() - 1);
    }

    public void setBackButton(int index) {
        setItemStack(index, Material.BLACK_STAINED_GLASS_PANE, TranslationKey.INVENTORIES__BACK);
    }

    public void setItemStack(int index, Material material, TranslationKey key) {
        setItemStack(index, material, Translator.get(key));
    }

    public void setItemStack(int index, Material material, TranslationKey key, List<String> lore) {
        setItemStack(index, material, Translator.get(key), lore);
    }

    public void setItemStack(int index, Material material, String text) {
        setItemStack(index, material, text, Collections.emptyList());
    }

    public void setItemStack(int index, Material material, String text, List<String> lore) {
        final ItemStack stack = new ItemStack(material, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(material);
        }

        assert meta != null;
        // Use plain Component so Minecraft applies its default white colour: no colour injection.
        ComponentMessages.displayName(meta, Component.text(stripColors(text)));
        if (!lore.isEmpty()) {
            ComponentMessages.lore(meta, lore.stream()
                .map(s -> LegacyComponentSerializer.legacySection().deserialize(s))
                .toList());
        }

        stack.setItemMeta(meta);
        inventory.setItem(index, stack);
    }

    public void setEnchantedItemStack(int index, Material material, TranslationKey key, boolean value) {
        ItemStack stack = new ItemStack(material, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) Bukkit.getItemFactory().getItemMeta(material);
        if (meta != null) {
            ComponentMessages.displayName(meta, Component.text(stripColors(Translator.get(key))));
            stack.setItemMeta(meta);
        }
        toggleEnchants(stack, value);
        inventory.setItem(index, stack);
    }

    public void setPlayerSkull(int index, @Nullable final PlayerProfile profile) {
        if (!Bukkit.isPrimaryThread()) {
            // Inventory mutations must happen on the main/region thread. Defer.
            BlockProt.getFoliaLib().getScheduler().runNextTick(tickTask -> setPlayerSkull(index, profile));
            return;
        }
        final var stack = new ItemStack(Material.PLAYER_HEAD, 1);
        var meta = (SkullMeta) stack.getItemMeta();
        if (meta == null)
            meta = (SkullMeta) Bukkit.getItemFactory().getItemMeta(Material.PLAYER_HEAD);

        try {
            assert meta != null;
            if (profile != null) meta.setOwnerProfile(profile);
            if (profile != null && profile.getName() != null)
                ComponentMessages.displayName(meta, Component.text(profile.getName()));
        } catch (Throwable ignored) {}
        stack.setItemMeta(meta);
        inventory.setItem(index, stack);
    }

/**
     * Sets a player skull at {@code index} and, if no skin is yet cached, asynchronously
     * fetches the real skin via Paper's {@code PlayerProfile.update()} and refreshes
     * the same slot once it arrives.
     *
     * <p>This is the preferred way to render a player head in any inventory.
     *
     * @param index  Inventory slot index.
     * @param player Player who owns this inventory; used to verify the inventory is still open.
     * @param uuid   Player UUID.
     * @param name   Player name to display on the skull.
     */
    public void setPlayerSkullAsync(int index, @NotNull Player player, @NotNull UUID uuid, @NotNull String name) {
        setPlayerSkullAsync(index, player, uuid, name, (String) null, (List<String>) null);
    }

    public void setPlayerSkullAsync(int index, @NotNull Player player, @NotNull UUID uuid, @NotNull String name, @Nullable String lore) {
        setPlayerSkullAsync(index, player, uuid, name, null, lore != null ? List.of(lore) : null);
    }

    public void setPlayerSkullAsync(int index, @NotNull Player player, @NotNull UUID uuid, @NotNull String name, @Nullable String customTitle, @Nullable String lore) {
        setPlayerSkullAsync(index, player, uuid, name, customTitle, lore != null ? List.of(lore) : null);
    }

    public void setPlayerSkullAsync(int index, @NotNull Player player, @NotNull UUID uuid, @NotNull String name, @Nullable String customTitle, @Nullable List<String> lore) {
        PlayerProfile immediate = SkinCache.getCachedOrOnlineProfile(name, uuid);
        if (immediate != null) {
            setPlayerSkull(index, immediate);
            if (customTitle != null) setDisplayName(index, customTitle);
            if (lore != null && !lore.isEmpty()) setLoreList(index, lore);
            return;
        }

        setPlayerSkull(index, createPlayerProfile(uuid, name));
        if (customTitle != null) setDisplayName(index, customTitle);
        if (lore != null && !lore.isEmpty()) setLoreList(index, lore);

        SkinCache.getOrFetchAsync(name, uuid).thenAcceptAsync(freshProfile -> {
            if (!player.isOnline()) return;
            Inventory top = player.getOpenInventory().getTopInventory();
            if (top == null || top.getHolder() != BlockProtInventory.this) return;
            setPlayerSkull(index, freshProfile);
            if (customTitle != null) setDisplayName(index, customTitle);
            if (lore != null && !lore.isEmpty()) setLoreList(index, lore);
        }, runnable -> BlockProt.getFoliaLib().getScheduler().runAtEntity(player, tickTask -> runnable.run()));
    }

    private void setDisplayName(int index, @NotNull String title) {
        ItemStack stack = inventory.getItem(index);
        if (stack == null) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        ComponentMessages.displayName(meta, LegacyComponentSerializer.legacySection().deserialize(title));
        stack.setItemMeta(meta);
    }

    private void setLoreList(int index, @NotNull List<String> lore) {
        ItemStack stack = inventory.getItem(index);
        if (stack == null) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        ComponentMessages.lore(meta, lore.stream()
            .map(s -> LegacyComponentSerializer.legacySection().deserialize(s))
            .toList());
        stack.setItemMeta(meta);
    }

    public static boolean hasSkin(@Nullable PlayerProfile profile) {
        return SkinCache.hasSkin(profile);
    }

    @Nullable
    public static PlayerProfile createPlayerProfile(@NotNull final UUID uuid, @NotNull final String name) {
        try {
            return Bukkit.createProfile(uuid, name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public void goBack(@NotNull final Player player, @NotNull final InventoryState state) {
        InventoryState.MenuOrigin parent = state.originStack.isEmpty()
            ? state.origin
            : state.originStack.pop();
        state.origin = state.originStack.isEmpty() ? InventoryState.MenuOrigin.NONE : state.originStack.peek();
        switch (parent) {
            case BLOCK_LOCK -> {
                var block = state.getBlock();
                var handler = getNbtHandlerOrNull(block);
                if (handler != null) {
                    player.openInventory(new BlockLockInventory().fill(player, block.getType(), handler));
                } else {
                    closeAndOpen(player, null);
                }
            }
            case USER_MENU -> {
                if (BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
                    switchToExternalUi(player, state, () -> de.sean.blockprot.bukkit.dialogs.UserMenuDialog.show(player));
                } else {
                    player.openInventory(new UserMenuInventory().fill(player));
                }
            }
            case ADMIN_MENU -> {
                if (BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
                    switchToExternalUi(player, state, () -> de.sean.blockprot.bukkit.dialogs.AdminMenuDialog.show(player));
                } else {
                    player.openInventory(new AdminMenuInventory().fill(player));
                }
            }
            case FRIEND_MANAGE -> {
                var inv = new FriendManageInventory().fill(player);
                closeAndOpen(player, inv);
            }
            case STATISTICS -> player.openInventory(new StatisticsInventory().fill(player));
            case USER_SETTINGS -> player.openInventory(new UserSettingsInventory().fill(player));
            case LOCKABLES -> player.openInventory(new LockablesInventory().fill(player, state.currentPageIndex));
            case AUTO_DROP -> player.openInventory(new AutoDropInventory().fill(player));
            case WORLD_LOCKABLE_SELECTION -> player.openInventory(new WorldLockableSelectionInventory().fill(player));
            case PLAYER_LIST -> new PlayerListInventory().open(player);
            default -> closeAndOpen(player, null);
        }
    }

    protected void closeAndOpen(@NotNull final HumanEntity player, @Nullable final Inventory inventory) {
        if (inventory != null) {
            player.openInventory(inventory);
        } else {
            player.closeInventory();
            InventoryState.remove(player.getUniqueId());
        }
    }

    protected static void switchToExternalUi(@NotNull Player player,
                                             @NotNull InventoryState state,
                                             @NotNull Runnable openTask) {
        // Clear navigation stack before closing so onInventoryClose does not reopen the parent chest GUI.
        state.suppressCloseReopen = true;
        state.originStack.clear();
        InventoryState.remove(player.getUniqueId());
        player.closeInventory();
        BlockProt.getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
            if (player.isOnline()) {
                openTask.run();
            }
        }, 1L);
    }

    protected Material getProperMaterial(Material material) {
        String name = material.name();
        if (name.endsWith("_WALL_SIGN")) {
            Material m = Material.matchMaterial(name.replace("_WALL_SIGN", "_SIGN"));
            return m == null ? material : m;
        }
        if (name.endsWith("_WALL_HANGING_SIGN")) {
            Material m = Material.matchMaterial(name.replace("_WALL_HANGING_SIGN", "_HANGING_SIGN"));
            return m == null ? material : m;
        }
        return material;
    }

    @Nullable
    protected BlockNBTHandler getNbtHandlerOrNull(@Nullable Block block) {
        if (block == null) return null;

        try {
            return new BlockNBTHandler(block);
        } catch (RuntimeException e) {
            return null;
        }
    }

    protected @Nullable FriendSupportingHandler<NBTCompound> getFriendSupportingHandler(@NotNull InventoryState.FriendSearchState state,
                                                                                        @Nullable Player player,
                                                                                        @Nullable Block block) {
        return switch (state) {
            case FRIEND_SEARCH -> block == null ? null : getNbtHandlerOrNull(block);
            case DEFAULT_FRIEND_SEARCH -> player == null ? null : new PlayerSettingsHandler(player);
        };
    }

    @NotNull
    @Deprecated
    protected ItemStack toggleEnchants(@NotNull ItemStack stack, final @Nullable Boolean toggle) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(stack.getType());
        }
        if (meta != null) {
            if (meta.hasEnchants() && (toggle == null || !toggle)) {
                meta.removeEnchant(BukkitCompat.GLOW_ENCHANT);
            } else if (!meta.hasEnchants() && (toggle == null || toggle)) {
                meta.addEnchant(BukkitCompat.GLOW_ENCHANT, 1, true);
            }
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    protected void updateTitle(@NotNull Player player, @NotNull String title) {
        this.inventory = this.createInventory(title);
        this.closeAndOpen(player, this.inventory);
    }

    public void setEnchantedOptionItemStack(int index, Material material, TranslationKey key, boolean value) {
        ItemStack stack = new ItemStack(material, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) Bukkit.getItemFactory().getItemMeta(material);
        if (meta != null) {
            String displayNameText = stripColors(Translator.get(key)) + ": " +
                (value ? stripColors(Translator.get(TranslationKey.ENABLED)) : stripColors(Translator.get(TranslationKey.DISABLED)));
            ComponentMessages.displayName(meta, Component.text(displayNameText));
            stack.setItemMeta(meta);
        }
        toggleOption(stack, value);
        inventory.setItem(index, stack);
    }

    public void setEnchantedOptionItemStack(int index, Material material, String text, List<String> lore, boolean value) {
        final ItemStack stack = new ItemStack(material, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(material);
        }
        if (meta != null) {
            ComponentMessages.displayName(meta, Component.text(stripColors(text)));
            if (!lore.isEmpty()) {
                ComponentMessages.lore(meta, lore.stream()
                    .map(s -> LegacyComponentSerializer.legacySection().deserialize(s))
                    .toList());
            }
            stack.setItemMeta(meta);
        }
        toggleEnchants(stack, value);
        inventory.setItem(index, stack);
    }

    @NotNull
    protected ItemStack toggleOption(@NotNull ItemStack stack, final @Nullable Boolean toggle) {
        var meta = stack.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(stack.getType());
        }
        if (meta != null) {
            // Get current display name as plain text (strip colors from legacy serialization)
            var displayNameComponent = ComponentMessages.displayName(meta);
            String name = displayNameComponent != null
                ? stripColors(LegacyComponentSerializer.legacySection().serialize(displayNameComponent))
                : "";
            final var pos = name.lastIndexOf(':');
            if (pos != -1) {
                name = name.substring(0, pos);
            }
            Enchantment glow = BukkitCompat.GLOW_ENCHANT;

            if (meta.hasEnchants() && (toggle == null || !toggle)) {
                meta.removeEnchant(glow);
                ComponentMessages.displayName(meta, Component.text(name + ": " + stripColors(Translator.get(TranslationKey.DISABLED))));
            } else if (!meta.hasEnchants() && (toggle == null || toggle)) {
                meta.addEnchant(glow, 1, true);
                ComponentMessages.displayName(meta, Component.text(name + ": " + stripColors(Translator.get(TranslationKey.ENABLED))));
            }
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}