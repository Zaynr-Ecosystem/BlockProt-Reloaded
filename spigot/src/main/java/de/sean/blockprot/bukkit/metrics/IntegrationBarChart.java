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

package de.sean.blockprot.bukkit.metrics;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.config.IntegrationConfig;
import org.bstats.charts.SimpleBarChart;
import org.bukkit.plugin.PluginManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public final class IntegrationBarChart extends SimpleBarChart {
    public IntegrationBarChart() {
        super("integrations", new IntegrationBarChartData());
    }

    public static class IntegrationBarChartData implements Callable<Map<String, Integer>> {
        @Override
        public Map<String, Integer> call() {
            HashMap<String, Integer> map = new HashMap<>();
            BlockProt instance = BlockProt.getInstance();
            for (var integration : instance.getIntegrations()) {
                map.put(integration.name, integration.isEnabled() ? 1 : 0);
            }
            PluginManager pm = instance.getServer().getPluginManager();
            boolean worldEditPresent = pm.isPluginEnabled("WorldEdit") || pm.isPluginEnabled("FastAsyncWorldEdit");
            boolean worldEditActive = worldEditPresent && IntegrationConfig.getBoolean("worldedit.enabled", true);
            map.put("worldedit", worldEditActive ? 1 : 0);
            return map;
        }
    }
}