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

package de.sean.blockprot.bukkit.storage;

import de.sean.blockprot.bukkit.Edition;
import de.sean.blockprot.bukkit.tasks.BackupTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginDataLayoutTest {

    @Test
    void migratesUserCacheFromPluginRootWithWalAndShm(@TempDir File dir) throws Exception {
        File oldDb = new File(dir, "blockprot_usercache.sqlite");
        File oldWal = new File(dir, "blockprot_usercache.sqlite-wal");
        File oldShm = new File(dir, "blockprot_usercache.sqlite-shm");
        Files.writeString(oldDb.toPath(), "cache-bytes");
        Files.writeString(oldWal.toPath(), "wal-bytes");
        Files.writeString(oldShm.toPath(), "shm-bytes");

        boolean moved = PluginDataLayout.migrateUserCache(dir, null);

        assertTrue(moved);
        assertFalse(oldDb.exists());
        assertFalse(oldWal.exists());
        assertFalse(oldShm.exists());
        File newDb = PluginDataLayout.userCacheFile(dir);
        assertEquals("cache-bytes", Files.readString(newDb.toPath()));
        assertEquals("wal-bytes", Files.readString(new File(newDb.getPath() + "-wal").toPath()));
        assertEquals("shm-bytes", Files.readString(new File(newDb.getPath() + "-shm").toPath()));
    }

    @Test
    void migratesUserCacheFromServerRootWhenMissingInPluginFolder(@TempDir File root) throws Exception {
        File serverRoot = new File(root, "server");
        File dataFolder = new File(root, "plugins/BlockProtReloaded");
        serverRoot.mkdirs();
        dataFolder.mkdirs();
        File oldDb = new File(serverRoot, "blockprot_usercache.sqlite");
        Files.writeString(oldDb.toPath(), "server-cache");

        boolean moved = PluginDataLayout.migrateUserCache(dataFolder, serverRoot);

        assertTrue(moved);
        assertFalse(oldDb.exists());
        assertEquals("server-cache", Files.readString(PluginDataLayout.userCacheFile(dataFolder).toPath()));
    }

    @Test
    void migratesAuditDbAndRemovesOnlyEmptyMysqlDirectory(@TempDir File root) throws Exception {
        File emptyCase = new File(root, "empty-case");
        File mysqlDir = new File(emptyCase, "mysql");
        mysqlDir.mkdirs();
        Files.writeString(new File(mysqlDir, "blockprot_audit.sqlite").toPath(), "audit-1");
        Files.writeString(new File(mysqlDir, "blockprot_audit.sqlite-wal").toPath(), "audit-wal");

        assertTrue(PluginDataLayout.migrateAuditDb(emptyCase));
        assertFalse(mysqlDir.exists());
        assertEquals("audit-1", Files.readString(PluginDataLayout.auditDbFile(emptyCase).toPath()));
        assertEquals("audit-wal", Files.readString(new File(PluginDataLayout.auditDbFile(emptyCase).getPath() + "-wal").toPath()));

        File configCase = new File(root, "with-mysql-yml");
        File mysqlDirWithConfig = new File(configCase, "mysql");
        mysqlDirWithConfig.mkdirs();
        Files.writeString(new File(mysqlDirWithConfig, "mysql.yml").toPath(), "mysql:\n  enabled: true\n");
        Files.writeString(new File(mysqlDirWithConfig, "blockprot_audit.sqlite").toPath(), "audit-2");

        assertTrue(PluginDataLayout.migrateAuditDb(configCase));
        assertTrue(mysqlDirWithConfig.isDirectory());
        assertTrue(new File(mysqlDirWithConfig, "mysql.yml").isFile());
        assertEquals("audit-2", Files.readString(PluginDataLayout.auditDbFile(configCase).toPath()));
    }

    @Test
    void consolidatesLegacyDotFilesIntoUnifiedStateYml(@TempDir File dir) throws Exception {
        Files.writeString(new File(dir, ".edition").toPath(),
            "edition=bpr\nversion=1.4.3\nminecraft=26.3\nwritten=2026-11-14T00:00:00Z\n");
        Files.writeString(new File(dir, ".first_start_done").toPath(), "");
        Files.writeString(new File(dir, ".recommended_state.yml").toPath(), "blocks_applied: true\n");

        PluginDataLayout.migrateAll(dir, null);

        assertFalse(new File(dir, ".edition").exists());
        assertFalse(new File(dir, ".first_start_done").exists());
        assertFalse(new File(dir, ".recommended_state.yml").exists());
        assertTrue(new File(PluginDataLayout.dataDir(dir), "state.yml").isFile());

        assertTrue(PluginDataLayout.isFirstStartDone(dir));
        assertTrue(PluginDataLayout.isRecommendedApplied(dir, "blocks_applied"));
        Properties marker = PluginDataLayout.readEditionMarker(dir);
        assertNotNull(marker);
        assertEquals("bpr", marker.getProperty("edition"));
        assertEquals("1.4.3", marker.getProperty("version"));
        assertEquals("26.3", marker.getProperty("minecraft"));
    }

    @Test
    void backupTaskIgnoresManagedDataFilesOnFreshInstallButDetectsAuditDb(@TempDir File dir) throws Exception {
        File dataDir = PluginDataLayout.dataDir(dir);
        dataDir.mkdirs();
        Files.writeString(PluginDataLayout.userCacheFile(dir).toPath(), "fresh-cache");
        PluginDataLayout.writeEditionMarker(dir, Edition.BPR, "1.4.3", "26.3");
        PluginDataLayout.markFirstStartDone(dir);

        assertFalse(BackupTask.hasPriorData(dir));

        Files.writeString(PluginDataLayout.auditDbFile(dir).toPath(), "prior-audit");
        assertTrue(BackupTask.hasPriorData(dir));
    }

    @Test
    void storesAndReadsLastSeenChangelogVersionInStateYml(@TempDir File dir) {
        assertEquals(null, PluginDataLayout.getLastSeenChangelogVersion(dir));
        PluginDataLayout.markChangelogSeen(dir, "1.4.4");
        assertEquals("1.4.4", PluginDataLayout.getLastSeenChangelogVersion(dir));
    }
}
