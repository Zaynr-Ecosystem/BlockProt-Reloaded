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

package de.sean.blockprot.bukkit.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders Adventure {@link Component}s on every supported server.
 *
 * <p>Paper-based servers (Paper, Folia, Purpur) expose Adventure to plugins, so
 * components are sent through the native APIs. Vanilla Spigot/Bukkit does not
 * put Adventure on the plugin classpath and lacks the component overloads, so
 * this class falls back to legacy colour-coded strings, which the server
 * converts before display.
 *
 * <p>Because Paper resolves {@code net.kyori.adventure.*} parent-first, the
 * Adventure classes bundled into the plugin jar are only ever used on servers
 * that do not ship Adventure themselves.
 */
public final class ComponentMessages {

    /** True when the server API has {@code CommandSender#sendMessage(Component)}. */
    private static final boolean COMPONENT_CHAT_SUPPORTED = hasMethod(CommandSender.class, "sendMessage");

    /** True when the server API has {@code Player#sendActionBar(Component)}. */
    private static final boolean COMPONENT_ACTION_BAR_SUPPORTED = hasMethod(Player.class, "sendActionBar");

    /** True when the server API has {@code ItemMeta#displayName()} (and the {@code lore()} counterpart). */
    private static final boolean COMPONENT_ITEM_META_SUPPORTED = hasNoArgMethod(ItemMeta.class, "displayName");

    /** Whether the server API has {@code Player#sendActionBar(Component)}. */
    public static boolean isActionBarSupported() {
        return COMPONENT_ACTION_BAR_SUPPORTED;
    }

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
        .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private ComponentMessages() {}

    /**
     * Escapes MiniMessage tag brackets in untrusted strings before template insertion.
     */
    @NotNull
    public static String escapeMiniMessage(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().escapeTags(input);
    }

    /**
     * Sends a component to a command sender natively when supported, otherwise
     * as a legacy colour-coded string.
     */
    public static void send(@NotNull CommandSender sender, @NotNull Component message) {
        if (COMPONENT_CHAT_SUPPORTED) {
            sender.sendMessage(message);
        } else {
            sender.sendMessage(LEGACY.serialize(message));
        }
    }

    /**
     * Sends a pre-formatted legacy colour-coded string, upgrading it to a
     * component on servers that support components so colours render natively,
     * and passing it through unchanged on servers without component support.
     */
    public static void sendLegacy(@NotNull CommandSender sender, @NotNull String message) {
        if (COMPONENT_CHAT_SUPPORTED) {
            sender.sendMessage(LEGACY.deserialize(message));
        } else {
            sender.sendMessage(message);
        }
    }

    /**
     * Shows a component in a player's action bar natively when supported,
     * otherwise falls back to a regular chat message.
     */
    public static void sendActionBar(@NotNull Player player, @NotNull Component message) {
        if (COMPONENT_ACTION_BAR_SUPPORTED) {
            player.sendActionBar(message);
        } else {
            player.sendMessage(LEGACY.serialize(message));
        }
    }

    /**
     * Shows a pre-formatted legacy colour-coded string in a player's action bar
     * natively when supported, otherwise as a regular chat message.
     */
    public static void sendLegacyActionBar(@NotNull Player player, @NotNull String message) {
        if (COMPONENT_ACTION_BAR_SUPPORTED) {
            player.sendActionBar(LEGACY.deserialize(message));
        } else {
            player.sendMessage(message);
        }
    }

    /** Sets an item's display name natively when supported, otherwise as a legacy colour-coded string. */
    @SuppressWarnings("deprecation")
    public static void displayName(@NotNull ItemMeta meta, @NotNull Component displayName) {
        if (COMPONENT_ITEM_META_SUPPORTED) {
            meta.displayName(displayName);
        } else {
            meta.setDisplayName(LEGACY.serialize(displayName));
        }
    }

    /** Reads an item's display name as a component. */
    @SuppressWarnings("deprecation")
    @Nullable
    public static Component displayName(@NotNull ItemMeta meta) {
        if (COMPONENT_ITEM_META_SUPPORTED) {
            return meta.displayName();
        }
        String legacy = meta.getDisplayName();
        return legacy == null ? null : LEGACY.deserialize(legacy);
    }

    /** Sets an item's lore natively when supported, otherwise as legacy colour-coded strings. */
    @SuppressWarnings("deprecation")
    public static void lore(@NotNull ItemMeta meta, @NotNull List<? extends Component> lore) {
        if (COMPONENT_ITEM_META_SUPPORTED) {
            meta.lore(new ArrayList<>(lore));
        } else {
            List<String> lines = new ArrayList<>(lore.size());
            for (Component line : lore) {
                lines.add(LEGACY.serialize(line));
            }
            meta.setLore(lines);
        }
    }

    /** Reads an item's lore as components. */
    @SuppressWarnings("deprecation") // Shut up (legacy API, replaced by components)
    @Nullable
    public static List<Component> lore(@NotNull ItemMeta meta) {
        if (COMPONENT_ITEM_META_SUPPORTED) {
            return meta.lore();
        }
        List<String> legacy = meta.getLore();
        if (legacy == null) return null;
        List<Component> lines = new ArrayList<>(legacy.size());
        for (String line : legacy) {
            lines.add(LEGACY.deserialize(line));
        }
        return lines;
    }

    /**
     * Creates an inventory with a component title natively when supported, otherwise
     * with a legacy colour-coded string title.
     *
     * <p>{@code Bukkit.createInventory(InventoryHolder, int, Component)} is a Paper-only
     * overload; Spigot/CraftBukkit only expose the {@code String}-title overload and throw
     * {@link NoSuchMethodError} on the Component one.
     */
    @NotNull
    @SuppressWarnings("deprecation")
    public static Inventory createInventory(@NotNull InventoryHolder holder, int size, @NotNull Component title) {
        try {
            return Bukkit.createInventory(holder, size, title);
        } catch (NoSuchMethodError e) {
            return Bukkit.createInventory(holder, size, LEGACY.serialize(title));
        }
    }

    private static boolean hasMethod(@NotNull Class<?> type, @NotNull String methodName) {
        try {
            Class<?> platformComponent = Class.forName(
                "net.kyori.adventure.text.Component",
                false,
                type.getClassLoader());
            return type.getMethod(methodName, platformComponent) != null;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean hasNoArgMethod(@NotNull Class<?> type, @NotNull String methodName) {
        try {
            return type.getMethod(methodName) != null;
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }
}
