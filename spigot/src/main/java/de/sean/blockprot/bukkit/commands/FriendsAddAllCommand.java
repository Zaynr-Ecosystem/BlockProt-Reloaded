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

package de.sean.blockprot.bukkit.commands;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.dialogs.DialogOrigin;
import de.sean.blockprot.bukkit.dialogs.FriendManageDialog;
import de.sean.blockprot.bukkit.inventories.FriendManageInventory;
import de.sean.blockprot.bukkit.inventories.InventoryState;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import de.sean.blockprot.bukkit.util.PlayerNameResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Handles the {@code /blockprot friends} subcommand.
 */
public final class FriendsAddAllCommand implements CommandExecutor {

    private static final long SEARCH_MSG_TIMEOUT_TICKS = 60L;

    @Override
    public boolean canUseCommand(@NotNull CommandSender sender) {
        return !BlockProt.getDefaultConfig().isFriendFunctionalityDisabled()
            && sender.hasPermission(Permissions.USER.key());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__ONLY_PLAYERS)));
            return true;
        }
        if (!canUseCommand(sender)) {
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__NO_PERMISSION)));
            return true;
        }
        if (args.length == 1) {
            openFriendManageGui(player);
            return true;
        }

        if (args.length < 3 || !isAddAllAlias(args[1])) {
            sendAction(player, Translator.get(TranslationKey.MESSAGES__FRIENDS_ADDALL_USAGE));
            return true;
        }

        handleAddAllCommand(player, args[2]);
        return true;
    }

    private void openFriendManageGui(@NotNull Player player) {
        if (BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
            FriendManageDialog.show(player, DialogOrigin.NONE);
            return;
        }
        InventoryState state = new InventoryState(null);
        state.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
        state.origin = InventoryState.MenuOrigin.NONE;
        InventoryState.set(player.getUniqueId(), state);
        var inv = new FriendManageInventory().fill(player);
        if (inv != null) player.openInventory(inv);
    }

    private void handleAddAllCommand(@NotNull Player player, @NotNull String targetName) {
        if (targetName.equalsIgnoreCase(player.getName())) {
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__TRANSFER_SELF)));
            return;
        }

        sendAction(player, Translator.get(TranslationKey.MESSAGES__FRIENDS_SEARCHING)
            .replace("{player}", targetName));

        final WrappedTask[] clearTask = {null};
        clearTask[0] = BlockProt.getFoliaLib().getScheduler().runLater(() -> {
            if (ComponentMessages.isActionBarSupported()) {
                ComponentMessages.sendActionBar(player, Component.empty());
            }
        }, SEARCH_MSG_TIMEOUT_TICKS);

        BlockProt.getFoliaLib().getScheduler().runAsync(asyncTask -> {
            OfflinePlayer target = resolveOfflinePlayer(targetName);

            if (clearTask[0] != null) {
                clearTask[0].cancel();
                clearTask[0] = null;
            }

            if (target == null || target.getUniqueId() == null) {
                BlockProt.getFoliaLib().getScheduler().runAtEntity(player, tickTask -> {
                    sendAction(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                    BlockProt.getFoliaLib().getScheduler().runLater(() -> {
                        if (ComponentMessages.isActionBarSupported()) {
                            ComponentMessages.sendActionBar(player, Component.empty());
                        }
                    }, SEARCH_MSG_TIMEOUT_TICKS);
                });
                return;
            }

            final OfflinePlayer finalTarget = target;
            final String targetUuid = target.getUniqueId().toString();

            BlockProt.getFoliaLib().getScheduler().runAtEntity(player, tickTask -> {
                if (finalTarget.getUniqueId().equals(player.getUniqueId())) {
                    player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        Translator.get(TranslationKey.MESSAGES__TRANSFER_SELF)));
                    return;
                }
                if (ComponentMessages.isActionBarSupported()) {
                    ComponentMessages.sendActionBar(player, Component.empty());
                }
                int modified = applyAddAllToOwnedBlocks(player, targetUuid);

                PlayerSettingsHandler settings = new PlayerSettingsHandler(player);
                if (!settings.containsFriend(targetUuid)) {
                    settings.addFriend(targetUuid);
                }

                final int count = modified;
                player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                    Translator.get(TranslationKey.MESSAGES__FRIENDS_ADDALL_SUCCESS)
                        .replace("{player}", targetName)
                        .replace("{count}", String.valueOf(count))));
            });
        });
    }

    @Nullable
    private OfflinePlayer resolveOfflinePlayer(@NotNull String name) {
        OfflinePlayer target = PlayerNameResolver.findOfflinePlayer(name);
        if (target == null) {
            @SuppressWarnings("deprecation")
            OfflinePlayer fallback = Bukkit.getOfflinePlayer(name);
            if (fallback.hasPlayedBefore()) target = fallback;
        }
        return target;
    }

    private int applyAddAllToOwnedBlocks(@NotNull Player player, @NotNull String targetUuid) {
        final PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
        StatHandler.getStatistic(stat, player);

        int modified = 0;
        for (var entry : stat.get()) {
            var loc = entry.get();
            if (loc.getWorld() == null) continue;
            try {
                var block = loc.getWorld().getBlockAt(loc);
                if (!BlockProt.getDefaultConfig().isLockable(block.getType())) continue;
                var handler = new BlockNBTHandler(block);
                if (!handler.isOwner(player.getUniqueId())) continue;
                if (!handler.containsFriend(targetUuid)) {
                    handler.addFriend(targetUuid);
                    handler.applyToOtherContainer();
                    modified++;
                }
            } catch (Exception ignored) {}
        }
        return modified;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 2)
            return isAddAllAliasPrefix(args[1]) ? List.of("addall") : Collections.emptyList();
        if (args.length == 3 && isAddAllAlias(args[1]))
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                .toList();
        return Collections.emptyList();
    }

    private void sendAction(@NotNull Player player, @NotNull String text) {
        ComponentMessages.sendLegacyActionBar(player, text);
    }

    private boolean isAddAllAlias(@NotNull String value) {
        return value.equalsIgnoreCase("addall");
    }

    private boolean isAddAllAliasPrefix(@NotNull String value) {
        return "addall".startsWith(value.toLowerCase(java.util.Locale.ROOT));
    }
}