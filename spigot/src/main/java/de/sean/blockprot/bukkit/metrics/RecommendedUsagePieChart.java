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
import de.sean.blockprot.bukkit.storage.PluginDataLayout;
import org.bstats.charts.SimplePie;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.Callable;

public final class RecommendedUsagePieChart extends SimplePie {

    public RecommendedUsagePieChart() {
        super("recommended_setup", new RecommendedUsagePieChartData());
    }

    public static @NotNull String classify(boolean blocksApplied,
                                           boolean configApplied,
                                           boolean hasConfiguredBlocks) {
        if (blocksApplied && configApplied) return "Blocks + Config";
        if (blocksApplied) return "Blocks Only";
        if (configApplied) return "Config Only";
        return hasConfiguredBlocks ? "Manual Setup" : "Unconfigured";
    }

    public static class RecommendedUsagePieChartData implements Callable<String> {
        @Override
        public String call() {
            File dataFolder = BlockProt.getInstance().getDataFolder();
            DefaultConfig cfg = BlockProt.getDefaultConfig();
            boolean blocksApplied = PluginDataLayout.isRecommendedApplied(dataFolder, "blocks_applied");
            boolean configApplied = PluginDataLayout.isRecommendedApplied(dataFolder, "config_applied");
            return classify(blocksApplied, configApplied, cfg.hasConfiguredBlocks());
        }
    }
}
