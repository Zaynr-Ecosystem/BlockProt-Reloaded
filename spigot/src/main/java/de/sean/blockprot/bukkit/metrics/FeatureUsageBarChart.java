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
import org.bstats.charts.SimpleBarChart;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public final class FeatureUsageBarChart extends SimpleBarChart {

    public FeatureUsageBarChart() {
        super("feature_usage", new FeatureUsageBarChartData());
    }

    public static @NotNull Map<String, Integer> collect(@NotNull DefaultConfig cfg) {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("Lock On Place", boolInt(cfg.lockOnPlaceByDefault()));
        map.put("Modern Family Blocks", boolInt(cfg.isModernFamilyBlocks()));
        map.put("Per-World Config", boolInt(cfg.isPerWorldsConfigEnabled()));
        map.put("Entity Protection", boolInt(cfg.isEntityProtectionEnabled()));
        map.put("Raid Detection", boolInt(cfg.getBukkitConfig().getBoolean("raid_detection.enabled", false)));
        map.put("World Expiry", boolInt(cfg.isWorldExpiryEnabled()));
        map.put("Admin Tiers", boolInt(cfg.isAdminTiersEnabled()));
        map.put("Friends System", boolInt(!cfg.isFriendFunctionalityDisabled()));
        map.put("Simplified Hopper", boolInt(cfg.isSimplifiedHopperLogic()));
        map.put("MySQL Index", boolInt(cfg.isMysqlEnabled()));
        map.put("Auto Update Notify", boolInt(cfg.shouldNotifyOpOfUpdates()));
        map.put("Auto Drop", boolInt(cfg.isAutoDropToInventoryEnabled()));
        map.put("Owner Notifications", boolInt(cfg.isOwnerNotificationsEnabled()));
        map.put("Auto Reload", boolInt(cfg.isAutoReloadEnabled()));
        return map;
    }

    private static int boolInt(boolean enabled) {
        return enabled ? 1 : 0;
    }

    public static class FeatureUsageBarChartData implements Callable<Map<String, Integer>> {
        @Override
        public Map<String, Integer> call() {
            return collect(BlockProt.getDefaultConfig());
        }
    }
}
