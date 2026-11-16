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

public final class LockablesProfileBarChart extends SimpleBarChart {

    public LockablesProfileBarChart() {
        super("lockable_categories", new LockablesProfileBarChartData());
    }

    public static @NotNull Map<String, Integer> collect(@NotNull DefaultConfig cfg) {
        return collect(
            cfg.hasLockableTileEntities(),
            cfg.hasLockableShulkerBoxes(),
            cfg.hasLockableBlocksOnly(),
            cfg.hasLockableDoors(),
            cfg.hasLockableEntities(),
            !cfg.getProtectableEntities().isEmpty(),
            !cfg.getAutoDropToInventoryBlocks().isEmpty()
        );
    }

    public static @NotNull Map<String, Integer> collect(boolean tileEntities,
                                                        boolean shulkers,
                                                        boolean blocks,
                                                        boolean doors,
                                                        boolean entities,
                                                        boolean protectableEntities,
                                                        boolean autoDropBlocks) {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("Tile Entities", tileEntities ? 1 : 0);
        map.put("Shulker Boxes", shulkers ? 1 : 0);
        map.put("Blocks", blocks ? 1 : 0);
        map.put("Doors", doors ? 1 : 0);
        map.put("Lockable Entities", entities ? 1 : 0);
        map.put("Protectable Entities", protectableEntities ? 1 : 0);
        map.put("Auto-Drop Blocks", autoDropBlocks ? 1 : 0);
        return map;
    }

    public static class LockablesProfileBarChartData implements Callable<Map<String, Integer>> {
        @Override
        public Map<String, Integer> call() {
            return collect(BlockProt.getDefaultConfig());
        }
    }
}
