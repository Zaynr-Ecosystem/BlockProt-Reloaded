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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricsChartsTest {

    @Test
    void classifiesUiModesAndRecommendedSetupAccurately() {
        assertEquals("Menus + Dialogs", UiModePieChart.classify(true, true));
        assertEquals("Dialogs", UiModePieChart.classify(true, false));
        assertEquals("Menus", UiModePieChart.classify(false, true));
        assertEquals("Commands", UiModePieChart.classify(false, false));

        assertEquals("Blocks + Config", RecommendedUsagePieChart.classify(true, true, true));
        assertEquals("Blocks Only", RecommendedUsagePieChart.classify(true, false, true));
        assertEquals("Config Only", RecommendedUsagePieChart.classify(false, true, false));
        assertEquals("Manual Setup", RecommendedUsagePieChart.classify(false, false, true));
        assertEquals("Unconfigured", RecommendedUsagePieChart.classify(false, false, false));
    }
}
