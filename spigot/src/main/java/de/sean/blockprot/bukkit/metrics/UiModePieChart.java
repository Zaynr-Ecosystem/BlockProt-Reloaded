/*
 * Copyright (C) 2026 Zar (VictorGugug)
 * This file is part of BlockProt Reloaded <https://github.com/VictorGugug/BlockProt-Reloaded>.
 *
 * BlockProt Reloaded is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BlockProt Reloaded is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlockProt Reloaded.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.sean.blockprot.bukkit.metrics;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import org.bstats.charts.SimplePie;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;

public final class UiModePieChart extends SimplePie {

    public UiModePieChart() {
        super("ui_mode", new UiModePieChartData());
    }

    public static @NotNull String classify(boolean useDialogs, boolean useMenus) {
        if (useDialogs && useMenus) return "Menus + Dialogs";
        if (useDialogs) return "Dialogs";
        if (useMenus) return "Menus";
        return "Commands";
    }

    public static class UiModePieChartData implements Callable<String> {
        @Override
        public String call() {
            DefaultConfig cfg = BlockProt.getDefaultConfig();
            return classify(cfg.isDialogsEnabled(), cfg.isUseMenusEnabled());
        }
    }
}
