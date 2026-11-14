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

package de.sean.blockprot.bukkit;

import de.sean.blockprot.bukkit.bedrock.BedrockBridge;
import de.sean.blockprot.bukkit.commands.LogsCommand;
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.dialogs.BpDialogStyles;
import de.sean.blockprot.bukkit.inventories.InventoryState;
import de.sean.blockprot.bukkit.storage.ProtectedBlockCache;
import de.sean.blockprot.bukkit.tasks.InactivityCleanupTask;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import de.sean.blockprot.bukkit.util.PlayerNameResolver;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuditAndRegressionTest {

    private static ServerMock server;

    @BeforeAll
    static void initServer() throws Exception {
        server = MockBukkit.mock();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("bedrock_username_prefixes", List.of(".", "*"));
        File tempDir = File.createTempFile("bp_audit_reg_test", "");
        tempDir.delete();
        tempDir.mkdirs();
        tempDir.deleteOnExit();

        var field = BlockProt.class.getDeclaredField("defaultConfig");
        field.setAccessible(true);
        field.set(null, new DefaultConfig(yaml, tempDir));
    }

    @AfterAll
    static void shutdownServer() {
        ProtectedBlockCache.clear();
        InventoryState.clear();
        MockBukkit.unmock();
    }

    @BeforeEach
    void resetCaches() {
        ProtectedBlockCache.clear();
        InventoryState.clear();
    }

    @Test
    void logsNormalizeArgsStripsLeadingCommandToken() {
        assertArrayEquals(new String[] {"here"}, LogsCommand.normalizeArgs(new String[] {"logs", "here"}));
        assertArrayEquals(new String[] {"player", "Steve"}, LogsCommand.normalizeArgs(new String[] {"LOGS", "player", "Steve"}));
        assertArrayEquals(new String[] {"2"}, LogsCommand.normalizeArgs(new String[] {"2"}));
        assertArrayEquals(new String[0], LogsCommand.normalizeArgs(new String[] {"logs"}));
    }

    @Test
    void playerNameResolverFindsBedrockPrefixedOnlinePlayer() {
        var bedrockPlayer = server.addPlayer(".AlexBedrock");
        assertSame(bedrockPlayer, PlayerNameResolver.findOfflinePlayer(".AlexBedrock"));
        assertSame(bedrockPlayer, PlayerNameResolver.findOfflinePlayer("AlexBedrock"));
        assertNull(PlayerNameResolver.findOfflinePlayer("MissingPlayerName999"));
    }

    @Test
    void protectedBlockCacheClearsPerWorldOnly() {
        WorldMock worldA = server.addSimpleWorld("cache_world_a");
        WorldMock worldB = server.addSimpleWorld("cache_world_b");
        Block blockA = worldA.getBlockAt(10, 64, 10);
        Block blockB = worldB.getBlockAt(10, 64, 10);
        blockA.setType(Material.CHEST);
        blockB.setType(Material.CHEST);

        ProtectedBlockCache.mark(blockA);
        ProtectedBlockCache.mark(blockB);
        assertTrue(ProtectedBlockCache.isProtected(blockA));
        assertTrue(ProtectedBlockCache.isProtected(blockB));

        ProtectedBlockCache.clearWorld(worldA.getUID());
        assertFalse(ProtectedBlockCache.isProtected(blockA));
        assertTrue(ProtectedBlockCache.isProtected(blockB));

        ProtectedBlockCache.unmark(blockB);
        assertFalse(ProtectedBlockCache.isProtected(blockB));
    }

    @Test
    void inventoryStateResolvesBlockByCoordinatesWithoutHoldingBlockReference() {
        WorldMock world = server.addSimpleWorld("inv_state_world");
        Block block = world.getBlockAt(3, 70, -5);
        block.setType(Material.CHEST);

        InventoryState state = InventoryState.builder().block(block).build();
        Block resolved = state.getBlock();
        assertNotNull(resolved);
        assertEquals(world.getUID(), resolved.getWorld().getUID());
        assertEquals(3, resolved.getX());
        assertEquals(70, resolved.getY());
        assertEquals(-5, resolved.getZ());

        InventoryState.set(java.util.UUID.randomUUID(), state);
        InventoryState.clear();
    }

    @Test
    void inactivityCleanupEligibilityHandlesOnlineAndInactiveOfflinePlayers() {
        long cutoff = 1_000_000L;
        OfflinePlayer online = offlineProxy(true, 500_000L, 500_000L);
        OfflinePlayer neverSeen = offlineProxy(false, 0L, 0L);
        OfflinePlayer activeRecently = offlineProxy(false, 1_500_000L, 500_000L);
        OfflinePlayer inactiveOffline = offlineProxy(false, 900_000L, 900_000L);

        assertFalse(InactivityCleanupTask.isEligibleOffline(online, cutoff));
        assertFalse(InactivityCleanupTask.isEligibleOffline(neverSeen, cutoff));
        assertFalse(InactivityCleanupTask.isEligibleOffline(activeRecently, cutoff));
        assertTrue(InactivityCleanupTask.isEligibleOffline(inactiveOffline, cutoff));
    }

    @Test
    void bedrockBridgeSanitizesHandshakeAndExtractsQueryParameters() {
        String raw = "play.example.org?floodgate=1&region=eu\0FML3\0";
        assertEquals("play.example.org", BedrockBridge.sanitizeHandshakeHostname(raw));
        assertEquals("1", BedrockBridge.extractHandshakeQueryParam(raw, "FLOODGATE"));
        assertEquals("eu", BedrockBridge.extractHandshakeQueryParam(raw, "region"));
        assertNull(BedrockBridge.extractHandshakeQueryParam(raw, "missing"));
        assertEquals("", BedrockBridge.sanitizeHandshakeHostname(null));
    }

    @Test
    void grateSubFamilyAndVersionCompatMatchSafely() {
        Set<Material> grates = BlockFamilyParser.getSubFamilyMembers(BlockFamilyParser.SubFamily.GRATE);
        assertTrue(grates.contains(Material.COPPER_GRATE));
        assertTrue(grates.contains(Material.WAXED_OXIDIZED_COPPER_GRATE));
        assertEquals(BlockFamilyParser.SubFamily.GRATE, BlockFamilyParser.subFamilyOf(Material.COPPER_GRATE));

        assertEquals(Material.CHEST, VersionCompat.matchMaterialIfAtLeast("CHEST", 1, 18));
        assertNull(VersionCompat.matchMaterialIfAtLeast("CHEST", 99, 0));
    }

    @Test
    void dialogStylesUseCanonicalPastelHexValues() {
        assertEquals(0x98FF98, BpDialogStyles.PASTEL_MINT.value());
        assertEquals(0xFF6B6B, BpDialogStyles.PASTEL_CORAL.value());
        assertEquals(0xF0E6A0, BpDialogStyles.PASTEL_GOLD.value());
        assertEquals(0x7EC8E3, BpDialogStyles.SOFT_BLUE.value());
        assertEquals(0xB39DDB, BpDialogStyles.PASTEL_PURPLE.value());
    }

    @Test
    void componentMessagesEscapesMiniMessageTags() {
        String escaped = ComponentMessages.escapeMiniMessage("<red>Bob</red>");
        assertNotEquals("<red>Bob</red>", escaped);
        assertEquals("\\<red>Bob\\</red>", escaped);
    }

    private static OfflinePlayer offlineProxy(boolean online, long lastSeen, long lastPlayed) {
        return (OfflinePlayer) Proxy.newProxyInstance(
            OfflinePlayer.class.getClassLoader(),
            new Class<?>[] {OfflinePlayer.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "isOnline" -> online;
                case "getLastSeen" -> lastSeen;
                case "getLastPlayed" -> lastPlayed;
                default -> null;
            }
        );
    }
}
