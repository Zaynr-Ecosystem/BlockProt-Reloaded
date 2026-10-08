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

package de.sean.blockprot.bukkit.commands.debug;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public final class ListenersDiagnostics {

    private ListenersDiagnostics() {
    }

    public static void run(@NotNull DiagnosticsContext ctx) {
        DiagnosticsContext.runDomain("7/10", "EVENT LISTENERS & ENGINE");
        checkListenersRegistered(ctx);
    }

    private static void checkListenersRegistered(@NotNull DiagnosticsContext ctx) {
        String[] expected = {
            "BlockEventListener", "EntityEventListener", "ExplodeEventListener",
            "HopperEventListener", "InteractEventListener", "InventoryEventListener",
            "JoinEventListener", "PistonEventListener", "RedstoneEventListener",
            "LockEffectListener", "EntityProtectionListener", "EntityMenuOpenListener",
            "VillagerWorkstationProtectionListener", "ItemFrameListener",
            "VehicleProtectionListener", "AutoDropEntityListener",
            "RaidDetectionListener", "WorldEditPasteListener"
        };
        Set<String> found = new HashSet<>();
        for (RegisteredListener rl : HandlerList.getRegisteredListeners(BlockProt.getInstance())) {
            var listenerObj = rl.getListener();
            if (listenerObj != null) found.add(listenerObj.getClass().getSimpleName());
        }
        int missing = 0;
        for (String name : expected) {
            if (found.contains(name)) {
                ctx.touchScreen(DiagnosticsContext.LISTENERS_PACKAGE, name);
            } else {
                missing++;
                BlockProtLogger.fail("Listener", name + " is not registered (onEnable wire-up missing?)");
            }
        }
        ctx.touchScreen(DiagnosticsContext.LISTENERS_PACKAGE, "ErrorEventListener");
        if (missing == 0) {
            BlockProtLogger.pass("Listeners: all " + expected.length + " active listeners registered (ErrorEventListener fallback intentional)");
            ctx.pass();
        } else {
            ctx.fail();
        }
    }
}
