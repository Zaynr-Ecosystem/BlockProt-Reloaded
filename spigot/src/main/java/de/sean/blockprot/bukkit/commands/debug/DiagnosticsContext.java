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

import de.sean.blockprot.bukkit.BlockProtLogger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

public record DiagnosticsContext(
    @Nullable Player player,
    @NotNull Location origin,
    @NotNull UUID actor,
    @NotNull AtomicInteger passed,
    @NotNull AtomicInteger failed,
    @NotNull Set<String> coveredClasses
) {
    public static final String NOTCH_UUID = "069a79f4-44e9-4726-a5be-fca90e38aaf5";
    public static final String INVENTORY_PACKAGE = "de.sean.blockprot.bukkit.inventories";
    public static final String DIALOG_PACKAGE = "de.sean.blockprot.bukkit.dialogs";
    public static final String LISTENERS_PACKAGE = "de.sean.blockprot.bukkit.listeners";
    public static final String COMMANDS_PACKAGE = "de.sean.blockprot.bukkit.commands";

    public static void runDomain(@NotNull String index, @NotNull String title) {
        BlockProtLogger.separator();
        BlockProtLogger.log("[" + index + "] " + title);
    }

    public void pass() {
        passed.incrementAndGet();
    }

    public void fail() {
        failed.incrementAndGet();
    }

    public void touch(@NotNull String fqcn) {
        coveredClasses.add(fqcn);
    }

    public void touchScreen(@NotNull String pkg, @NotNull String displayedName) {
        coveredClasses.add(pkg + "." + displayedName.split(" ")[0]);
    }

    public void dlg(@NotNull String name, @NotNull Runnable body) {
        try {
            touchScreen(DIALOG_PACKAGE, name);
            body.run();
            BlockProtLogger.passSub(name);
            pass();
        } catch (Exception e) {
            BlockProtLogger.failSub(name, e.getClass().getSimpleName() + ": " + e.getMessage());
            fail();
        }
    }

    public void inv(@NotNull String name, @NotNull Callable<Inventory> supplier) {
        try {
            touchScreen(INVENTORY_PACKAGE, name);
            Inventory result = supplier.call();
            if (result != null) {
                BlockProtLogger.passSub(name + " (size=" + result.getSize() + ")");
            } else {
                BlockProtLogger.skipSub(name, "returned null (intentional for some paths)");
            }
            pass();
        } catch (Exception e) {
            BlockProtLogger.failSub(name, e.getClass().getSimpleName() + ": " + e.getMessage());
            fail();
        }
    }

    public void simSub(@NotNull String name, @NotNull Runnable test) {
        try {
            test.run();
            BlockProtLogger.passSub(name);
            pass();
        } catch (Throwable e) {
            BlockProtLogger.failSub(name, e.getClass().getSimpleName() + ": " + e.getMessage());
            fail();
        }
    }
}
