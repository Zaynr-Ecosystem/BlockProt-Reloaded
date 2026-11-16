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

import de.sean.blockprot.bukkit.config.DefaultConfig;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.io.File;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsChartsTest {

    private static File tempFolder;

    @BeforeAll
    static void setup() throws Exception {
        MockBukkit.mock();
        tempFolder = File.createTempFile("bp_metrics_test", "");
        tempFolder.delete();
        tempFolder.mkdirs();
        tempFolder.deleteOnExit();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

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

    @Test
    void classifiesNewDiagnosticPieChartsAccurately() {
        assertEquals("Active", DialogApiStatusPieChart.classify(true, true));
        assertEquals("Supported (Disabled)", DialogApiStatusPieChart.classify(true, false));
        assertEquals("Unsupported", DialogApiStatusPieChart.classify(false, true));
        assertEquals("Unsupported", DialogApiStatusPieChart.classify(false, false));

        assertEquals("0", ProtectedScalePieChart.classify(0));
        assertEquals("1-50", ProtectedScalePieChart.classify(50));
        assertEquals("51-250", ProtectedScalePieChart.classify(250));
        assertEquals("251-1000", ProtectedScalePieChart.classify(1000));
        assertEquals("1001-5000", ProtectedScalePieChart.classify(5000));
        assertEquals("5001+", ProtectedScalePieChart.classify(5001));

        assertEquals("Stable", BuildChannelPieChart.classify("1.4.6"));
        assertEquals("Hotfix", BuildChannelPieChart.classify("1.4.6-hotfix.1"));
        assertEquals("BEDev", BuildChannelPieChart.classify("1.4.7-BEDev.1"));
        assertEquals("Experimental", BuildChannelPieChart.classify("1.4.7-exp"));
        assertEquals("Unknown", BuildChannelPieChart.classify(""));

        assertEquals("Geyser + Floodgate", BedrockSupportPieChart.classify(true, true));
        assertEquals("Geyser Only", BedrockSupportPieChart.classify(true, false));
        assertEquals("Floodgate Only", BedrockSupportPieChart.classify(false, true));
        assertEquals("Java Only", BedrockSupportPieChart.classify(false, false));

        assertEquals("MySQL + Audit", StorageBackendPieChart.classify(true, true));
        assertEquals("MySQL", StorageBackendPieChart.classify(true, false));
        assertEquals("SQLite + Audit", StorageBackendPieChart.classify(false, true));
        assertEquals("SQLite", StorageBackendPieChart.classify(false, false));
    }

    @Test
    void collectsExpandedFeatureUsageAndLockablesProfile() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("notify_op_of_updates", true);
        yaml.set("notify_admins_on_join", false);
        yaml.set("inactivity_cleanup_days", 30);
        yaml.set("player_max_locked_block_count", 100);
        yaml.set("enable_backups", false);
        yaml.set("enable_session_log", false);
        yaml.set("public_is_friend_by_default", true);
        yaml.set("entity_protection.enabled", true);
        yaml.set("entity_protection.auto_protect_on_tame", true);

        DefaultConfig cfg = new DefaultConfig(yaml, tempFolder);
        Map<String, Integer> features = FeatureUsageBarChart.collect(cfg, true);

        assertEquals(34, features.size());
        assertEquals(1, features.get("Auto Update Notify"));
        assertEquals(0, features.get("Notify Admins On Join"));
        assertEquals(1, features.get("Inactivity Cleanup"));
        assertEquals(1, features.get("Max Locks Limit"));
        assertEquals(0, features.get("Auto Backups"));
        assertEquals(0, features.get("Session Log"));
        assertEquals(1, features.get("Public As Friend"));
        assertEquals(1, features.get("Entity Protection"));
        assertEquals(1, features.get("Entity Auto-Protect Tame"));
        assertEquals(1, features.get("WorldEdit Paste Autolock"));

        Map<String, Integer> lockables = LockablesProfileBarChart.collect(true, false, true, false, true, false, true);
        assertEquals(7, lockables.size());
        assertEquals(1, lockables.get("Tile Entities"));
        assertEquals(0, lockables.get("Shulker Boxes"));
        assertEquals(1, lockables.get("Blocks"));
        assertEquals(0, lockables.get("Doors"));
        assertEquals(1, lockables.get("Lockable Entities"));
        assertEquals(0, lockables.get("Protectable Entities"));
        assertEquals(1, lockables.get("Auto-Drop Blocks"));

        assertTrue(LockablesProfileBarChart.collect(cfg).containsKey("Tile Entities"));
    }
}
