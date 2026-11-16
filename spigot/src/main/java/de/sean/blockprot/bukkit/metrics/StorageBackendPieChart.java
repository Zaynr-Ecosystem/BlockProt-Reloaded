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
import org.bstats.charts.SimplePie;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;

public final class StorageBackendPieChart extends SimplePie {

    public StorageBackendPieChart() {
        super("storage_backend", new StorageBackendPieChartData());
    }

    public static @NotNull String classify(boolean mysqlEnabled, boolean auditActive) {
        if (mysqlEnabled && auditActive) return "MySQL + Audit";
        if (mysqlEnabled) return "MySQL";
        if (auditActive) return "SQLite + Audit";
        return "SQLite";
    }

    public static class StorageBackendPieChartData implements Callable<String> {
        @Override
        public String call() {
            boolean mysql = BlockProt.getDefaultConfig().isMysqlEnabled();
            boolean audit = BlockProt.getAuditLogger() != null;
            return classify(mysql, audit);
        }
    }
}
