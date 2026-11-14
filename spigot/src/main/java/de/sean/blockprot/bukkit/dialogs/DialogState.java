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

package de.sean.blockprot.bukkit.dialogs;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Per-player dialog back stack for screens that always return to the same
 * parent dialog, mirroring the inventory system's origin stack.
 */
public final class DialogState {

    private static final Map<UUID, Deque<Consumer<Player>>> BACK_STACKS = new ConcurrentHashMap<>();

    private DialogState() {}

    public static void clear(@NotNull Player player) {
        BACK_STACKS.remove(player.getUniqueId());
    }

    public static void clearAll() {
        BACK_STACKS.clear();
    }

    public static void push(@NotNull Player player, @NotNull Consumer<Player> backAction) {
        BACK_STACKS.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>()).push(backAction);
    }

    public static boolean pop(@NotNull Player player) {
        Deque<Consumer<Player>> stack = BACK_STACKS.get(player.getUniqueId());
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Consumer<Player> backAction = stack.pop();
        if (stack.isEmpty()) {
            BACK_STACKS.remove(player.getUniqueId());
        }
        backAction.accept(player);
        return true;
    }
}