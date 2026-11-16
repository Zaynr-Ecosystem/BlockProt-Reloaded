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

import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.BlockCountStatistic;
import org.bstats.charts.SimplePie;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;

public final class ProtectedScalePieChart extends SimplePie {

    public ProtectedScalePieChart() {
        super("protected_blocks_scale", new ProtectedScalePieChartData());
    }

    public static @NotNull String classify(int count) {
        if (count <= 0) return "0";
        if (count <= 50) return "1-50";
        if (count <= 250) return "51-250";
        if (count <= 1000) return "251-1000";
        if (count <= 5000) return "1001-5000";
        return "5001+";
    }

    public static class ProtectedScalePieChartData implements Callable<String> {
        @Override
        public String call() {
            if (!StatHandler.isLoaded()) {
                return "Unavailable";
            }
            BlockCountStatistic countStat = new BlockCountStatistic();
            StatHandler.getStatistic(countStat);
            return classify(countStat.get());
        }
    }
}
