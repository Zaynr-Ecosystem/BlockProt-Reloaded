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

package de.sean.blockprot.bukkit.admin;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class AdminTierAccessTest {

    private static ServerMock server;
    private static File dataFolder;

    @BeforeAll
    static void initServer() throws Exception {
        server = MockBukkit.mock();
        dataFolder = File.createTempFile("bp_admin_tier_test", "");
        dataFolder.delete();
        dataFolder.mkdirs();
        dataFolder.deleteOnExit();
    }

    @AfterAll
    static void shutdownServer() {
        MockBukkit.unmock();
    }

    private static void useTiers(boolean enabled) throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("admin_tiers.enabled", enabled);
        var field = BlockProt.class.getDeclaredField("defaultConfig");
        field.setAccessible(true);
        field.set(null, new DefaultConfig(yaml, dataFolder));
    }

    private PlayerMock playerWith(String name, String... nodes) {
        PlayerMock player = server.addPlayer(name);
        player.setOp(false);
        var attachment = player.addAttachment(MockBukkit.createMockPlugin());
        for (String node : nodes) attachment.setPermission(node, true);
        return player;
    }

    @Test
    void withoutTiersEveryActionFollowsTheAdminNode() throws Exception {
        useTiers(false);
        PlayerMock admin = playerWith("PlainAdmin", "blockprot.user.admin");
        PlayerMock user = playerWith("PlainUser");
        for (AdminAction action : AdminAction.values()) {
            if (action == AdminAction.RECOMMENDED) continue;
            assertTrue(AdminTierManager.hasPermission(admin, action), action.name());
            assertFalse(AdminTierManager.hasPermission(user, action), action.name());
        }
        assertFalse(AdminTierManager.canTeleport(admin));
    }

    @Test
    void tierOneReachesOnlyTheInspectionActions() throws Exception {
        useTiers(true);
        PlayerMock moderator = playerWith("Moderator", "blockprot.user.admin.t1");
        assertTrue(AdminTierManager.hasPermission(moderator, AdminAction.INFO));
        assertTrue(AdminTierManager.hasPermission(moderator, AdminAction.LOGS));
        assertTrue(AdminTierManager.canTeleport(moderator));
        for (AdminAction action : new AdminAction[] {AdminAction.UNLOCK, AdminAction.CONFIG, AdminAction.PROTDEL,
                AdminAction.RELOAD, AdminAction.SETROLE, AdminAction.LOCKABLES, AdminAction.DEBUG}) {
            assertFalse(AdminTierManager.hasPermission(moderator, action), action.name());
        }
    }

    @Test
    void tierTwoUnlocksButCannotConfigure() throws Exception {
        useTiers(true);
        PlayerMock helper = playerWith("Helper", "blockprot.user.admin.t2");
        assertTrue(AdminTierManager.hasPermission(helper, AdminAction.UNLOCK));
        assertTrue(AdminTierManager.hasPermission(helper, AdminAction.INFO));
        assertFalse(AdminTierManager.hasPermission(helper, AdminAction.CONFIG));
        assertFalse(AdminTierManager.hasPermission(helper, AdminAction.SETROLE));
    }

    @Test
    void theAdminNodeAloneStopsAtTierTwoWhenTiersAreOn() throws Exception {
        useTiers(true);
        PlayerMock admin = playerWith("NodeAdmin", "blockprot.user.admin");
        assertTrue(AdminTierManager.hasPermission(admin, AdminAction.UNLOCK));
        assertFalse(AdminTierManager.hasPermission(admin, AdminAction.CONFIG));
        assertFalse(AdminTierManager.hasPermission(admin, AdminAction.RELOAD));
    }

    @Test
    void operatorsCountAsOwners() throws Exception {
        useTiers(true);
        PlayerMock op = server.addPlayer("Operator");
        op.setOp(true);
        assertTrue(AdminTierManager.hasPermission(op, AdminAction.SETROLE));
        assertTrue(AdminTierManager.hasPermission(op, AdminAction.CONFIG));
    }

    @Test
    void playersWithoutATierHaveNoAdminAction() throws Exception {
        useTiers(true);
        PlayerMock user = playerWith("Nobody");
        for (AdminAction action : AdminAction.values()) {
            assertFalse(AdminTierManager.hasPermission(user, action), action.name());
        }
        assertFalse(AdminTierManager.canTeleport(user));
    }

    @Test
    void negatedAdminNodesOverrideOpFallback() throws Exception {
        useTiers(false);
        PlayerMock op = server.addPlayer("NegatedOp");
        op.setOp(true);
        var attachment = op.addAttachment(MockBukkit.createMockPlugin());
        attachment.setPermission("blockprot.user.admin", false);
        assertFalse(AdminTierManager.hasPermission(op, AdminAction.UNLOCK));
        assertFalse(AdminTierManager.hasPermission(op, AdminAction.INFO));

        useTiers(true);
        attachment.setPermission("blockprot.user.admin.action.unlock", false);
        assertFalse(AdminTierManager.hasPermission(op, AdminAction.UNLOCK));
    }
}
