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

package de.sean.blockprot.bukkit.admin;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves and persists administrative roles, tiers, and granular permissions.
 */
public final class AdminTierManager {
    private static final Map<UUID, AdminRoleEntry> ADMIN_ENTRIES = new ConcurrentHashMap<>();

    private record AdminRoleEntry(@NotNull AdminTier tier, @NotNull Set<AdminAction> customFlags) {}

    private AdminTierManager() {}

    public static void load() {
        ADMIN_ENTRIES.clear();
        File file = getConfigFile();
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection adminsSection = config.getConfigurationSection("admins");
        if (adminsSection == null) return;

        for (String key : adminsSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String tierName = adminsSection.getString(key + ".tier", "none");
                AdminTier tier = AdminTier.fromString(tierName);
                List<String> rawFlags = adminsSection.getStringList(key + ".custom_flags");
                Set<AdminAction> flags = EnumSet.noneOf(AdminAction.class);
                for (String rawFlag : rawFlags) {
                    try {
                        flags.add(AdminAction.valueOf(rawFlag.toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException ignored) {}
                }
                ADMIN_ENTRIES.put(uuid, new AdminRoleEntry(tier, flags));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public static void save() {
        File file = getConfigFile();
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, AdminRoleEntry> entry : ADMIN_ENTRIES.entrySet()) {
            String path = "admins." + entry.getKey().toString();
            config.set(path + ".tier", entry.getValue().tier().getIdentifier());
            List<String> flagNames = new ArrayList<>();
            for (AdminAction action : entry.getValue().customFlags()) {
                flagNames.add(action.name());
            }
            config.set(path + ".custom_flags", flagNames);
        }
        try {
            config.save(file);
        } catch (IOException ignored) {}
    }

    private static File getConfigFile() {
        return new File(BlockProt.getInstance().getDataFolder(), "admins.yml");
    }

    private static boolean isNegated(@NotNull Player player, @NotNull String node) {
        return player.isPermissionSet(node) && !player.hasPermission(node);
    }

    @NotNull
    public static AdminTier getPlayerTier(@NotNull Player player) {
        if (player.hasPermission(Permissions.ADMIN_OWNER.key()) && !isNegated(player, Permissions.ADMIN_OWNER.key())) return AdminTier.OWNER;
        if (player.hasPermission(Permissions.ADMIN_T3.key()) && !isNegated(player, Permissions.ADMIN_T3.key())) return AdminTier.T3;
        if (player.hasPermission(Permissions.ADMIN_T2.key()) && !isNegated(player, Permissions.ADMIN_T2.key())) return AdminTier.T2;
        if (player.hasPermission(Permissions.ADMIN_T1.key()) && !isNegated(player, Permissions.ADMIN_T1.key())) return AdminTier.T1;
        if (player.hasPermission(Permissions.ADMIN_CUSTOM.key()) && !isNegated(player, Permissions.ADMIN_CUSTOM.key())) return AdminTier.CUSTOM;

        AdminRoleEntry entry = ADMIN_ENTRIES.get(player.getUniqueId());
        if (entry != null && entry.tier() != AdminTier.NONE) {
            return entry.tier();
        }

        if (player.isOp()
                && !isNegated(player, Permissions.ADMIN_OWNER.key())
                && !isNegated(player, Permissions.USER_ADMIN.key())) {
            return AdminTier.OWNER;
        }
        return AdminTier.NONE;
    }

    @NotNull
    public static Set<AdminAction> getCustomFlags(@NotNull Player player) {
        Set<AdminAction> flags = EnumSet.noneOf(AdminAction.class);
        AdminRoleEntry entry = ADMIN_ENTRIES.get(player.getUniqueId());
        if (entry != null) {
            flags.addAll(entry.customFlags());
        }
        return flags;
    }

    public static boolean hasPermission(@NotNull CommandSender sender, @NotNull AdminAction action) {
        if (sender instanceof ConsoleCommandSender) return true;
        if (!(sender instanceof Player player)) return sender.isOp();

        String actionNode = "blockprot.user.admin.action." + action.name().toLowerCase(Locale.ROOT);
        if (isNegated(player, actionNode)) return false;
        if (action == AdminAction.CONTAINER_BYPASS && isNegated(player, "blockprot.admin.bypass")) return false;

        if (!BlockProt.getDefaultConfig().isAdminTiersEnabled()) {
            if (action == AdminAction.RECOMMENDED) {
                if (isNegated(player, Permissions.ADMIN_OWNER.key()) || isNegated(player, Permissions.USER_ADMIN.key())) {
                    return false;
                }
                return player.isOp() || player.hasPermission(Permissions.ADMIN_OWNER.key());
            }
            if (isNegated(player, Permissions.USER_ADMIN.key())) return false;
            return player.isOp() || player.hasPermission(Permissions.USER_ADMIN.key());
        }

        AdminTier tier = getPlayerTier(player);
        if (tier == AdminTier.OWNER) return true;
        if (tier.includes(action.getMinimumTier())) return true;

        if (tier == AdminTier.CUSTOM || player.hasPermission(Permissions.ADMIN_CUSTOM.key())) {
            if (player.hasPermission(actionNode)) {
                return true;
            }
            if (getCustomFlags(player).contains(action)) {
                return true;
            }
        }

        if (!isNegated(player, Permissions.USER_ADMIN.key()) && player.hasPermission(Permissions.USER_ADMIN.key())) {
            return action.getMinimumTier().getLevel() <= AdminTier.T2.getLevel();
        }

        return false;
    }

    public static boolean deny(@NotNull Player player, @NotNull AdminAction action) {
        if (hasPermission(player, action)) return false;
        ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
        return true;
    }

    public static boolean canTeleport(@NotNull Player player) {
        return player.hasPermission(Permissions.BLOCKS_TP.key())
            || (BlockProt.getDefaultConfig().isAdminTiersEnabled() && hasPermission(player, AdminAction.TELEPORT));
    }

    public static boolean hasAnyAdminPermission(@NotNull CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) return true;
        if (!(sender instanceof Player player)) return sender.isOp();

        if (!BlockProt.getDefaultConfig().isAdminTiersEnabled()) {
            return player.isOp() || player.hasPermission(Permissions.USER_ADMIN.key());
        }

        if (getPlayerTier(player) != AdminTier.NONE) return true;
        return player.hasPermission(Permissions.USER_ADMIN.key());
    }

    public static void setPlayerRole(@NotNull UUID uuid, @NotNull AdminTier tier, @Nullable Set<AdminAction> customFlags) {
        Set<AdminAction> flags = customFlags != null ? EnumSet.copyOf(customFlags) : EnumSet.noneOf(AdminAction.class);
        if (tier == AdminTier.NONE) {
            ADMIN_ENTRIES.remove(uuid);
        } else {
            ADMIN_ENTRIES.put(uuid, new AdminRoleEntry(tier, flags));
        }
        save();

        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            PlayerSettingsHandler handler = new PlayerSettingsHandler(online);
            if (tier == AdminTier.NONE) {
                handler.clearAdminTier();
            } else {
                handler.setAdminTier(tier.getIdentifier());
                List<String> flagNames = new ArrayList<>();
                for (AdminAction action : flags) {
                    flagNames.add(action.name());
                }
                handler.setAdminCustomFlags(String.join(",", flagNames));
            }
        }
    }

    @NotNull
    public static AdminTier getRole(@NotNull UUID uuid) {
        AdminRoleEntry entry = ADMIN_ENTRIES.get(uuid);
        return entry != null ? entry.tier() : AdminTier.NONE;
    }

    @NotNull
    public static Set<AdminAction> getCustomFlags(@NotNull UUID uuid) {
        AdminRoleEntry entry = ADMIN_ENTRIES.get(uuid);
        if (entry != null && !entry.customFlags().isEmpty()) {
            return EnumSet.copyOf(entry.customFlags());
        }
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return getCustomFlags(online);
        }
        return EnumSet.noneOf(AdminAction.class);
    }

    public static boolean toggleCustomFlag(@NotNull UUID uuid, @NotNull AdminAction action) {
        Set<AdminAction> current = EnumSet.copyOf(getCustomFlags(uuid));
        boolean nowActive;
        if (current.contains(action)) {
            current.remove(action);
            nowActive = false;
        } else {
            current.add(action);
            nowActive = true;
        }
        setPlayerRole(uuid, AdminTier.CUSTOM, current);
        return nowActive;
    }

    @NotNull
    public static Map<UUID, AdminTier> getAllConfiguredRoles() {
        Map<UUID, AdminTier> result = new HashMap<>();
        for (Map.Entry<UUID, AdminRoleEntry> entry : ADMIN_ENTRIES.entrySet()) {
            result.put(entry.getKey(), entry.getValue().tier());
        }
        return Collections.unmodifiableMap(result);
    }
}
