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

package de.sean.blockprot.bukkit.tasks;

import de.sean.blockprot.bukkit.Edition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupTaskEditionMarkerTest {

    @TempDir
    File dir;

    private static Properties marker(String edition, String version, String minecraft) {
        Properties properties = new Properties();
        if (edition != null) properties.setProperty("edition", edition);
        if (version != null) properties.setProperty("version", version);
        if (minecraft != null) properties.setProperty("minecraft", minecraft);
        return properties;
    }

    @Test
    void freshInstallWritesMarkerAndIsNotAMigration() {
        assertNull(BackupTask.readMarker(dir));
        assertFalse(BackupTask.decide(null, Edition.BPR, "1.3.9", "1.21.8").changed());
        BackupTask.writeMarker(dir, Edition.BPR, "1.3.9", "1.21.8");
        Properties written = BackupTask.readMarker(dir);
        assertEquals("bpr", written.getProperty("edition"));
        assertEquals("1.3.9", written.getProperty("version"));
        assertEquals("1.21.8", written.getProperty("minecraft"));
        assertTrue(written.containsKey("written"));
    }

    @Test
    void sameEditionSameVersionIsNotAMigration() {
        BackupTask.Transition t = BackupTask.decide(marker("bpr", "1.3.9", "1.21.8"), Edition.BPR, "1.3.9", "1.21.8");
        assertFalse(t.migration());
        assertFalse(t.downgrade());
    }

    @Test
    void patchLevelChangesAreNotMigrations() {
        BackupTask.Transition t = BackupTask.decide(marker("bpr", "1.3.9", "1.21.7"), Edition.BPR, "1.3.10", "1.21.8");
        assertFalse(t.migration());
    }

    @Test
    void editionChangeIsAMigration() {
        BackupTask.Transition t = BackupTask.decide(marker("bpr-legacy", "1.3.9.1", "1.21.6"), Edition.BPR, "1.3.9", "1.21.7");
        assertTrue(t.migration());
        assertFalse(t.downgrade());
        assertEquals("bpr-legacy", t.fromEdition());
        assertEquals("1.3.9.1", t.fromVersion());
    }

    @Test
    void lowerRankIsADowngrade() {
        BackupTask.Transition t = BackupTask.decide(marker("bpr", "1.3.9", "1.21.8"), Edition.BPR_LEGACY, "1.3.9.1", "1.21.6");
        assertTrue(t.migration());
        assertTrue(t.downgrade());
    }

    @Test
    void olderVersionOfTheSameEditionIsADowngrade() {
        BackupTask.Transition t = BackupTask.decide(marker("bpr", "1.3.9", "1.21.8"), Edition.BPR, "1.3.8", "1.21.8");
        assertTrue(t.downgrade());
        assertTrue(t.changed());
    }

    @Test
    void patchUpgradeAndDataLayoutMigrationTriggerStartupBackup() {
        BackupTask.Transition patch = BackupTask.decide(marker("bpr", "1.4.4", "26.3"), Edition.BPR, "1.4.5", "26.3");
        assertFalse(patch.changed());
        assertTrue(BackupTask.isVersionChanged(patch, "1.4.5"));
        assertTrue(BackupTask.isStartupUpgradeOrMigration(false, false, patch, "1.4.5"));

        BackupTask.Transition same = BackupTask.decide(marker("bpr", "1.4.5", "26.3"), Edition.BPR, "1.4.5", "26.3");
        assertFalse(BackupTask.isVersionChanged(same, "1.4.5"));
        assertFalse(BackupTask.isStartupUpgradeOrMigration(false, false, same, "1.4.5"));
        assertTrue(BackupTask.isStartupUpgradeOrMigration(false, true, same, "1.4.5"));

        BackupTask.Transition fresh = BackupTask.decide(null, Edition.BPR, "1.4.5", "26.3");
        assertFalse(BackupTask.isVersionChanged(fresh, "1.4.5"));
        assertFalse(BackupTask.isStartupUpgradeOrMigration(false, false, fresh, "1.4.5"));
    }

    @Test
    void forStartupUpgradeCreatesZipWithoutAuditDbWhenMarkerPresent() throws Exception {
        Files.writeString(new File(dir, "config.yml").toPath(), "language_file: translations_en.yml\n");
        BackupTask.writeMarker(dir, Edition.BPR, "1.4.4", "26.3");

        BackupTask.forStartupUpgrade(dir).run();

        File[] zips = new File(dir, "backups").listFiles((d, name) -> name.endsWith(".zip"));
        assertTrue(zips != null && zips.length == 1);
    }

    @Test
    void unreadableMarkerIsIgnored() throws Exception {
        Files.writeString(new File(dir, ".edition").toPath(), "no edition key here\n");
        Properties read = BackupTask.readMarker(dir);
        assertFalse(BackupTask.decide(read, Edition.BPR, "1.3.9", "1.21.8").changed());
        assertFalse(BackupTask.decide(marker("unknown-id", "1.0.0", "1.8"), Edition.BPR, "1.3.9", "1.21.8").changed());
    }
}
