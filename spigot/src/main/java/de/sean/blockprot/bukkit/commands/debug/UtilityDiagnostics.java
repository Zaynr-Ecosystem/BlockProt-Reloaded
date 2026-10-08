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

package de.sean.blockprot.bukkit.commands.debug;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.commands.BlockProtCommand;
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.config.IntegrationConfig;
import de.sean.blockprot.bukkit.config.ReloadReport;
import de.sean.blockprot.bukkit.dialogs.*;
import de.sean.blockprot.bukkit.util.*;
import de.sean.blockprot.util.SemanticVersion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.*;
import java.util.jar.JarFile;

public final class UtilityDiagnostics {

    private static final Map<String, BlockFamilyParser.Family> INTEGRITY_KEY_FAMILIES = new LinkedHashMap<>();

    static {
        INTEGRITY_KEY_FAMILIES.put("lockable_tile_entities", BlockFamilyParser.Family.TILE_ENTITIES);
        INTEGRITY_KEY_FAMILIES.put("lockable_shulker_boxes", BlockFamilyParser.Family.SHULKER_BOXES);
        INTEGRITY_KEY_FAMILIES.put("lockable_blocks", BlockFamilyParser.Family.BLOCKS);
        INTEGRITY_KEY_FAMILIES.put("lockable_doors", BlockFamilyParser.Family.DOORS);
        INTEGRITY_KEY_FAMILIES.put("lockable_entities", BlockFamilyParser.Family.ENTITIES);
    }

    private UtilityDiagnostics() {
    }

    public static void run(@NotNull DiagnosticsContext ctx) {
        DiagnosticsContext.runDomain("9/10", "UTILITY HELPERS & BENCHMARKS");
        checkMessages(ctx);
        checkBlocksYmlIntegrity(ctx);
        checkWorldsYmlIntegrity(ctx);
        checkSkinCache(ctx);
        checkUtilityHelpers(ctx);
        checkStructuralClasses(ctx);
        checkEnumeratedCoverage(ctx);
    }

    private static void checkMessages(@NotNull DiagnosticsContext ctx) {
        TranslationKey[] abKeys = {
            TranslationKey.MESSAGES__LOCK_HINT,
            TranslationKey.MESSAGES__CHAT_INPUT_PROMPT,
            TranslationKey.MESSAGES__CHAT_INPUT_CANCELLED,
            TranslationKey.MESSAGES__COPY_DONE,
            TranslationKey.MESSAGES__PASTE_DONE,
            TranslationKey.MESSAGES__TRANSFER_SELF_GUI,
            TranslationKey.MESSAGES__TRANSFER_NOT_OWNER_GUI,
            TranslationKey.MESSAGES__TRANSFER_FAILED,
        };
        TranslationKey[] chatKeys = {
            TranslationKey.MESSAGES__NO_PERMISSION,
            TranslationKey.MESSAGES__FRIEND_ADDED,
            TranslationKey.MESSAGES__FRIEND_REMOVED,
            TranslationKey.MESSAGES__UNLOCKED,
            TranslationKey.MESSAGES__TIMED_ACCESS_GRANTED,
            TranslationKey.MESSAGES__TIMED_ACCESS_NOT_OWNER,
            TranslationKey.MESSAGES__TIMED_ACCESS_OVER_MAX,
            TranslationKey.MESSAGES__TRANSFER_SUCCESS,
        };
        int ok = 0;
        int bad = 0;
        for (TranslationKey k : abKeys) {
            String v = Translator.get(k);
            if (v == null || v.isBlank()) {
                BlockProtLogger.fail("ActionBar msg blank", k.name());
                bad++;
            } else {
                ok++;
            }
        }
        for (TranslationKey k : chatKeys) {
            String v = Translator.get(k);
            if (v == null || v.isBlank()) {
                BlockProtLogger.fail("Chat msg blank", k.name());
                bad++;
            } else {
                ok++;
            }
        }
        if (bad == 0) {
            BlockProtLogger.pass("Messages: all " + (abKeys.length + chatKeys.length) + " keys present ("
                + abKeys.length + " actionbar, " + chatKeys.length + " chat)");
            ctx.pass();
        } else {
            BlockProtLogger.fail("Messages", bad + " key(s) blank or missing");
            ctx.fail();
        }
    }

    private static void checkBlocksYmlIntegrity(@NotNull DiagnosticsContext ctx) {
        try {
            DefaultConfig defaultConfig = BlockProt.getDefaultConfig();
            File dataFolder = BlockProt.getInstance().getDataFolder();
            File blocksFile = new File(dataFolder, defaultConfig.getBlocksFilePath());
            if (!blocksFile.exists()) {
                BlockProtLogger.fail("blocks.yml integrity", "file not found at " + blocksFile.getPath());
                ctx.fail();
                return;
            }
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(blocksFile);

            int checked = 0;
            int unresolved = 0;
            int mismatched = 0;
            for (Map.Entry<String, BlockFamilyParser.Family> e : INTEGRITY_KEY_FAMILIES.entrySet()) {
                String key = e.getKey();
                BlockFamilyParser.Family family = e.getValue();
                if (!cfg.contains(key)) continue;
                Object raw = cfg.get(key);
                List<?> rawList = raw instanceof List<?> ? (List<?>) raw : List.of(raw);
                for (Object o : rawList) {
                    if (DefaultConfig.isPlaceholderEntry(o)) continue;
                    if (!(o instanceof String s)) continue;
                    String trimmed = s.trim();
                    checked++;
                    if (BlockFamilyParser.isFamilyExpression(trimmed)) {
                        Set<Material> resolved = BlockFamilyParser.parseFamilyExpressionSilent(trimmed, family);
                        if (resolved.isEmpty()) {
                            BlockProtLogger.fail("blocks.yml entry", key + ": '" + trimmed + "' resolved to 0 materials");
                            unresolved++;
                        }
                    } else {
                        Material m = Material.matchMaterial(trimmed);
                        if (m == null) {
                            BlockProtLogger.fail("blocks.yml entry", key + ": '" + trimmed + "' is not a valid Material");
                            unresolved++;
                            continue;
                        }
                        boolean isEntitiesKey = key.equals("lockable_entities");
                        boolean actual = isEntitiesKey ? defaultConfig.isLockableEntity(m) : defaultConfig.isLockable(m);
                        if (!actual) {
                            BlockProtLogger.fail("blocks.yml entry", key + ": '" + trimmed + "' listed but isLockable()=false");
                            mismatched++;
                        }
                    }
                }
            }

            if (unresolved == 0 && mismatched == 0) {
                BlockProtLogger.pass("blocks.yml integrity: " + checked + " entries checked OK (0 unresolved, 0 mismatched)");
                ctx.pass();
            } else {
                BlockProtLogger.fail("blocks.yml integrity", unresolved + " unresolved, " + mismatched + " mismatched");
                ctx.fail();
            }
        } catch (Exception ex) {
            BlockProtLogger.fail("blocks.yml integrity", ex.getMessage());
            ctx.fail();
        }
    }

    private static void checkWorldsYmlIntegrity(@NotNull DiagnosticsContext ctx) {
        try {
            DefaultConfig defaultConfig = BlockProt.getDefaultConfig();
            if (!defaultConfig.isPerWorldsConfigEnabled()) {
                BlockProtLogger.pass("worlds.yml integrity: per_worlds_config disabled, skipping");
                ctx.pass();
                return;
            }
            File worldsFile = new File(BlockProt.getInstance().getDataFolder(), "worlds.yml");
            if (!worldsFile.exists()) {
                BlockProtLogger.fail("worlds.yml integrity", "file not found at " + worldsFile.getPath());
                ctx.fail();
                return;
            }
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(worldsFile);
            ConfigurationSection worldsSection = cfg.getConfigurationSection("worlds");
            if (worldsSection == null) {
                BlockProtLogger.pass("worlds.yml integrity: no 'worlds' section, nothing to check");
                ctx.pass();
                return;
            }

            int checked = 0;
            int unresolved = 0;
            int mismatched = 0;
            int worldsChecked = 0;
            for (String worldName : worldsSection.getKeys(false)) {
                ConfigurationSection ws = worldsSection.getConfigurationSection(worldName);
                if (ws == null) continue;
                boolean enabled = ws.getBoolean("enabled", false);
                org.bukkit.World world = Bukkit.getWorld(worldName);
                worldsChecked++;

                for (Map.Entry<String, BlockFamilyParser.Family> e : INTEGRITY_KEY_FAMILIES.entrySet()) {
                    String key = e.getKey();
                    BlockFamilyParser.Family family = e.getValue();
                    if (!ws.contains(key)) continue;
                    Object raw = ws.get(key);
                    List<?> rawList = raw instanceof List<?> ? (List<?>) raw : List.of(raw);
                    for (Object o : rawList) {
                        if (DefaultConfig.isPlaceholderEntry(o)) continue;
                        if (!(o instanceof String s)) continue;
                        String trimmed = s.trim();
                        checked++;
                        if (BlockFamilyParser.isFamilyExpression(trimmed)) {
                            Set<Material> resolved = BlockFamilyParser.parseFamilyExpressionSilent(trimmed, family);
                            if (resolved.isEmpty()) {
                                BlockProtLogger.fail("worlds.yml entry", worldName + "." + key + ": '" + trimmed + "' resolved to 0 materials");
                                unresolved++;
                            }
                        } else {
                            Material m = Material.matchMaterial(trimmed);
                            if (m == null) {
                                BlockProtLogger.fail("worlds.yml entry", worldName + "." + key + ": '" + trimmed + "' is not a valid Material");
                                unresolved++;
                                continue;
                            }
                            if (enabled && world != null) {
                                boolean isEntitiesKey = key.equals("lockable_entities");
                                boolean actual = isEntitiesKey
                                    ? defaultConfig.isLockableEntity(m, world)
                                    : defaultConfig.isLockable(m, world);
                                if (!actual) {
                                    BlockProtLogger.fail("worlds.yml entry", worldName + "." + key + ": '" + trimmed + "' listed but not reflected by isLockable(world)");
                                    mismatched++;
                                }
                            }
                        }
                    }
                }
            }

            if (unresolved == 0 && mismatched == 0) {
                BlockProtLogger.pass("worlds.yml integrity: " + worldsChecked + " world(s), " + checked + " entries checked OK");
                ctx.pass();
            } else {
                BlockProtLogger.fail("worlds.yml integrity", unresolved + " unresolved, " + mismatched + " mismatched");
                ctx.fail();
            }
        } catch (Exception ex) {
            BlockProtLogger.fail("worlds.yml integrity", ex.getMessage());
            ctx.fail();
        }
    }

    private static void checkSkinCache(@NotNull DiagnosticsContext ctx) {
        BlockProtLogger.subGroup("SkinCache tiers:");
        Player player = ctx.player();
        String name = player != null ? player.getName() : "Notch";
        UUID uuid = player != null ? player.getUniqueId() : UUID.fromString(DiagnosticsContext.NOTCH_UUID);
        try {
            var profile = SkinCache.getCachedOrOnlineProfile(name, uuid);
            if (profile != null) {
                BlockProtLogger.passSub("tier 1 (online profile): resolved for " + name);
                ctx.pass();
            } else {
                BlockProtLogger.passSub("tier 1 (online profile): no cached/online skin for " + name + " (null, no exception)");
                ctx.pass();
            }
        } catch (Throwable e) {
            BlockProtLogger.failSub("SkinCache tier 1", e.getClass().getSimpleName() + ": " + e.getMessage());
            ctx.fail();
        }

        var sr = Bukkit.getPluginManager().getPlugin("SkinsRestorer");
        try {
            var srProfile = SkinCache.resolveSkinsRestorer(uuid, name);
            if (sr != null && sr.isEnabled()) {
                BlockProtLogger.passSub("tier 2 (SkinsRestorer): present, resolve returned "
                    + (srProfile != null ? "a profile" : "null"));
            } else {
                BlockProtLogger.passSub("tier 2 (SkinsRestorer): absent, resolve no-opped (null)");
            }
            ctx.pass();
        } catch (Throwable e) {
            BlockProtLogger.failSub("SkinCache tier 2", e.getClass().getSimpleName() + ": " + e.getMessage());
            ctx.fail();
        }
    }

    private static void checkUtilityHelpers(@NotNull DiagnosticsContext ctx) {
        BlockProtLogger.subGroup("Utility helpers (14 utilities):");
        try {
            Duration parsed = DurationParser.parse("2h30m");
            if (parsed == null || parsed.toMinutes() != 150) {
                BlockProtLogger.failSub("DurationParser", "parse(2h30m) = " + parsed);
                ctx.fail();
            } else {
                BlockProtLogger.passSub("DurationParser: parse(2h30m)=" + parsed
                    + " format=" + DurationParser.format(parsed));
                ctx.pass();
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("DurationParser", e.getMessage());
            ctx.fail();
        }

        try {
            DurationLimits limits = DurationLimits.create(60, 60, 24, 28, 12, 5);
            boolean okSec = limits.validate(Duration.ofSeconds(30));
            boolean okDay = limits.validate(Duration.ofDays(2));
            boolean over = limits.validate(Duration.ofDays(3650));
            long applicable = limits.getApplicableLimit(Duration.ofDays(30));
            if (!okSec || !okDay || over || applicable <= 0) {
                BlockProtLogger.failSub("DurationLimits", "unexpected validation results");
                ctx.fail();
            } else {
                BlockProtLogger.passSub("DurationLimits: 30s=" + okSec + " 2d=" + okDay + " 10y=" + over
                    + " applicable(30d)=" + applicable + "ms");
                ctx.pass();
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("DurationLimits", e.getMessage());
            ctx.fail();
        }

        try {
            int dist = StringUtil.levenshtein("chest", "chst");
            double sim = StringUtil.similarity("chest", "chest");
            if (dist < 1 || sim <= 0) {
                BlockProtLogger.failSub("StringUtil", "unexpected distance/similarity");
                ctx.fail();
            } else {
                BlockProtLogger.passSub("StringUtil: levenshtein(chest,chst)=" + dist + " similarity=" + sim);
                ctx.pass();
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("StringUtil", e.getMessage());
            ctx.fail();
        }

        try {
            String name = BlockUtil.getHumanReadableBlockName(Material.CHEST);
            if (name == null || name.isBlank()) {
                BlockProtLogger.failSub("BlockUtil", "blank readable name for CHEST");
                ctx.fail();
            } else {
                BlockProtLogger.passSub("BlockUtil: getHumanReadableBlockName(CHEST)=" + name);
                ctx.pass();
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("BlockUtil", e.getMessage());
            ctx.fail();
        }

        try {
            AsyncGuard.assertSync("debug utility group");
            BlockProtLogger.passSub("AsyncGuard: assertSync accepted (running on main thread)");
            ctx.pass();
        } catch (Exception e) {
            BlockProtLogger.failSub("AsyncGuard", e.getMessage());
            ctx.fail();
        }

        Player player = ctx.player();
        if (player != null) {
            try {
                Set<String> candidates = PlayerNameResolver.getNameCandidates(player.getName());
                if (candidates == null || candidates.isEmpty()) {
                    BlockProtLogger.failSub("PlayerNameResolver", "no candidates for own name");
                    ctx.fail();
                } else {
                    BlockProtLogger.passSub("PlayerNameResolver: " + candidates.size() + " candidate(s) for " + player.getName());
                    ctx.pass();
                }
            } catch (Exception e) {
                BlockProtLogger.failSub("PlayerNameResolver", e.getMessage());
                ctx.fail();
            }

            try {
                TemporaryActionBar.show(player, "debug", 1L);
                TemporaryActionBar.cancel(player.getUniqueId());
                BlockProtLogger.passSub("TemporaryActionBar: show+cancel OK");
                ctx.pass();
            } catch (Exception e) {
                BlockProtLogger.failSub("TemporaryActionBar", e.getMessage());
                ctx.fail();
            }
        } else {
            BlockProtLogger.passSub("PlayerNameResolver/TemporaryActionBar: skipped (headless console)");
            ctx.pass();
        }

        try {
            String stripped = BpDialogStyles.stripColor("&a&lTest");
            if (stripped == null || stripped.contains("&")) {
                BlockProtLogger.failSub("BpDialogStyles", "stripColor left a code: '" + stripped + "'");
                ctx.fail();
            } else {
                BlockProtLogger.passSub("BpDialogStyles: stripColor(&a&lTest)=" + stripped
                    + " palette=" + BpDialogStyles.SOFT_GRAY + "," + BpDialogStyles.PASTEL_MINT + ","
                    + BpDialogStyles.PASTEL_CORAL + "," + BpDialogStyles.PASTEL_GOLD + ","
                    + BpDialogStyles.SOFT_BLUE + "," + BpDialogStyles.PASTEL_PURPLE);
                ctx.pass();
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("BpDialogStyles", e.getMessage());
            ctx.fail();
        }

        try {
            DialogButton back = DialogNavigation.backButton(DialogOrigin.NONE, null);
            DialogButton backAdmin = DialogNavigation.backButton(DialogOrigin.ADMIN_MENU, null);
            if (back == null || backAdmin == null) {
                BlockProtLogger.failSub("DialogNavigation", "backButton returned null");
                ctx.fail();
            } else {
                BlockProtLogger.passSub("DialogNavigation: backButton(NONE) id=" + back.id()
                    + " backButton(ADMIN_MENU) id=" + backAdmin.id());
                ctx.pass();
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("DialogNavigation", e.getMessage());
            ctx.fail();
        }

        if (player != null) {
            try {
                DialogState.push(player, ignored -> {});
                boolean popped = DialogState.pop(player);
                DialogState.clear(player);
                BlockProtLogger.passSub("DialogState: push/pop=" + popped + " clear OK");
                ctx.pass();
            } catch (Exception e) {
                BlockProtLogger.failSub("DialogState", e.getMessage());
                ctx.fail();
            }
        } else {
            BlockProtLogger.passSub("DialogState: skipped (headless console)");
            ctx.pass();
        }

        try {
            Map<String, Object> before = new LinkedHashMap<>();
            before.put("key", "old");
            Map<String, Object> after = new LinkedHashMap<>();
            after.put("key", "new");
            Map<String, Object> snap1 = ReloadReport.captureSnapshot(before, "test.yml");
            Map<String, Object> snap2 = ReloadReport.captureSnapshot(after, "test.yml");
            List<ReloadReport.ChangeDiff> diffs = ReloadReport.compareSnapshots(snap1, snap2);
            if (diffs == null || diffs.isEmpty()) {
                BlockProtLogger.failSub("ReloadReport", "expected a diff between old/new snapshots");
                ctx.fail();
            } else {
                BlockProtLogger.passSub("ReloadReport: capture/compare produced " + diffs.size() + " diff(s)");
                ctx.pass();
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("ReloadReport", e.getMessage());
            ctx.fail();
        }

        try {
            boolean integrationFlag = IntegrationConfig.getBoolean("debug.integration_test", true);
            BlockProtLogger.passSub("IntegrationConfig: getBoolean(default)=" + integrationFlag);
            ctx.pass();
        } catch (Exception e) {
            BlockProtLogger.failSub("IntegrationConfig", e.getMessage());
            ctx.fail();
        }

        try {
            String curVer = new SemanticVersion(BlockProt.getPluginVersion()).baseVersion();
            SemanticVersion stable = new SemanticVersion(curVer);
            SemanticVersion bedev = new SemanticVersion(curVer + "-BEDev");
            SemanticVersion bdev = new SemanticVersion(curVer + "-bdev");
            SemanticVersion snap = new SemanticVersion(curVer + "-SNAPSHOT-3");
            SemanticVersion hotfix = new SemanticVersion(curVer + "-hotfix");
            SemanticVersion fixN = new SemanticVersion(curVer + "-fix.1");
            SemanticVersion release = new SemanticVersion(curVer + "-RELEASE");
            SemanticVersion exp = new SemanticVersion(curVer + "-exp");
            boolean ranksOk = !stable.isPreRelease() && !stable.isHotfix()
                && bedev.isPreRelease() && bdev.isPreRelease() && snap.isPreRelease()
                && hotfix.isHotfix() && fixN.isHotfix()
                && !release.isPreRelease() && !release.isHotfix()
                && exp.isExperimental();
            boolean orderOk = bedev.compareTo(stable) < 0
                && stable.compareTo(hotfix) < 0
                && bedev.compareTo(bdev) == 0
                && new SemanticVersion(curVer + "-BEDev.2").compareTo(new SemanticVersion(curVer + "-BEDev.1")) > 0
                && stable.compareTo(new SemanticVersion("99.0.0")) < 0
                && stable.compareTo(stable) == 0;
            boolean baseOk = hotfix.baseVersion().equals(curVer)
                && bedev.baseVersion().equals(curVer);
            if (!ranksOk || !orderOk || !baseOk) {
                BlockProtLogger.failSub("SemanticVersion",
                    "ranks=" + ranksOk + " order=" + orderOk + " base=" + baseOk);
                ctx.fail();
            } else {
                BlockProtLogger.passSub("SemanticVersion: ranks, order, and baseVersion verified (" + curVer + ")");
                ctx.pass();
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("SemanticVersion", e.getMessage());
            ctx.fail();
        }

        DialogBridgeFactory.setTestBridge(new NoopDialogBridge());
        try {
            ctx.touchScreen(DiagnosticsContext.DIALOG_PACKAGE, "AdminConfigValueDialog");
            if (player != null) {
                AdminConfigValueDialog.openInt(player, "debug.test", "hint", 0, v -> {}, () -> {});
                AdminConfigValueDialog.openText(player, "debug.test", "hint", "value", s -> null, v -> {}, () -> {});
                BlockProtLogger.passSub("AdminConfigValueDialog: openInt/openText routed through test bridge");
            } else {
                BlockProtLogger.passSub("AdminConfigValueDialog: touched (headless)");
            }
            ctx.pass();
        } catch (Exception e) {
            BlockProtLogger.failSub("AdminConfigValueDialog", e.getMessage());
            ctx.fail();
        } finally {
            DialogBridgeFactory.setTestBridge(null);
        }
    }

    private static void checkStructuralClasses(@NotNull DiagnosticsContext ctx) {
        String[] names = {
            "de.sean.blockprot.bukkit.admin.AdminTier",
            "de.sean.blockprot.bukkit.admin.AdminAction",
            "de.sean.blockprot.bukkit.admin.AdminTierManager",
            "de.sean.blockprot.bukkit.dialogs.AdminTiersDialog",
            "de.sean.blockprot.bukkit.dialogs.AdminTierSelectDialog",
            "de.sean.blockprot.bukkit.dialogs.AdminCustomFlagsDialog",
            "de.sean.blockprot.bukkit.inventories.AdminTiersInventory",
            "de.sean.blockprot.bukkit.inventories.AdminTierSelectInventory",
            "de.sean.blockprot.bukkit.inventories.AdminCustomFlagsInventory",
            "de.sean.blockprot.bukkit.events.BlockAccessEvent",
            "de.sean.blockprot.bukkit.events.BlockAccessMenuEvent",
            "de.sean.blockprot.bukkit.events.BlockLockOnPlaceEvent",
            "de.sean.blockprot.bukkit.events.BlockProtLockEvent",
            "de.sean.blockprot.bukkit.events.BlockProtUnlockEvent",
            "de.sean.blockprot.bukkit.inventories.BlockProtInventory",
            "de.sean.blockprot.bukkit.inventories.InventoryConstants",
            "de.sean.blockprot.bukkit.inventories.ChatInput",
            "de.sean.blockprot.bukkit.inventories.LegacyChatInput",
            "de.sean.blockprot.bukkit.inventories.TextInput",
            "de.sean.blockprot.bukkit.listeners.ErrorEventListener",
            "de.sean.blockprot.bukkit.logger.PluginActivityLog",
            "de.sean.blockprot.bukkit.metrics.IntegrationBarChart",
            "de.sean.blockprot.bukkit.config.BlockProtConfig",
            "de.sean.blockprot.bukkit.config.ReloadCoordinator",
            "de.sean.blockprot.bukkit.tasks.BackupTask",
            "de.sean.blockprot.bukkit.tasks.ConfigFileWatcher",
            "de.sean.blockprot.bukkit.tasks.InactivityCleanupTask",
            "de.sean.blockprot.bukkit.tasks.StatisticFileSaveTask",
            "de.sean.blockprot.bukkit.tasks.UpdateChecker",
            "de.sean.blockprot.bukkit.tasks.VillagerLocateTask",
            "de.sean.blockprot.bukkit.tasks.WorldExpiryTask",
            "de.sean.blockprot.bukkit.BlockProtAPI",
            "de.sean.blockprot.bukkit.BlockProtConsole",
            "de.sean.blockprot.bukkit.CachedProfileService",
            "de.sean.blockprot.bukkit.TranslationValue",
            "de.sean.blockprot.bukkit.VersionValidator",
            "de.sean.blockprot.bukkit.storage.HybridDatabase",
            "de.sean.blockprot.bukkit.storage.ProtectedBlockCache",
            "de.sean.blockprot.bukkit.nbt.stats.BukkitStatistic",
            "de.sean.blockprot.bukkit.nbt.stats.FloatStatistic",
            "de.sean.blockprot.bukkit.nbt.stats.IntStatistic",
            "de.sean.blockprot.bukkit.nbt.stats.LocationListStatistic",
            "de.sean.blockprot.bukkit.nbt.stats.StringStatistic",
        };
        int loaded = 0;
        for (String name : names) {
            try {
                Class.forName(name, false, BlockProt.getInstance().getClass().getClassLoader());
                loaded++;
            } catch (Throwable e) {
                BlockProtLogger.fail("Structural class", name + ": " + e.getClass().getSimpleName()
                    + ": " + e.getMessage());
                ctx.fail();
            }
        }
        if (loaded == names.length) {
            BlockProtLogger.pass("Structural classes: " + loaded + "/" + names.length + " loadable"
                + " (events, gateways, tasks, console, metrics, misc)");
            ctx.pass();
        } else {
            BlockProtLogger.fail("Structural classes", (names.length - loaded) + " class(es) failed to load");
        }
    }

    private static void checkEnumeratedCoverage(@NotNull DiagnosticsContext ctx) {
        var codeSource = BlockProt.getInstance().getClass().getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            BlockProtLogger.pass("Class coverage: not running from a jar, enumeration skipped (dev workspace)");
            ctx.pass();
            return;
        }
        int total = 0;
        int uncovered = 0;
        String[] packages = ctx.player() != null
            ? new String[]{DiagnosticsContext.INVENTORY_PACKAGE, DiagnosticsContext.DIALOG_PACKAGE, BlockProtCommand.class.getPackageName(), DiagnosticsContext.LISTENERS_PACKAGE}
            : new String[]{BlockProtCommand.class.getPackageName(), DiagnosticsContext.LISTENERS_PACKAGE};
        try (var jar = new JarFile(new File(codeSource.getLocation().toURI()))) {
            for (String pkg : packages) {
                String dir = pkg.replace('.', '/') + "/";
                for (var entry : Collections.list(jar.entries())) {
                    String entryName = entry.getName();
                    if (!entryName.startsWith(dir) || !entryName.endsWith(".class")) continue;
                    if (entryName.indexOf('/', dir.length()) != -1) continue;
                    String simpleName = entryName.substring(dir.length(), entryName.length() - ".class".length());
                    if (simpleName.indexOf('$') != -1) continue;
                    if (!(simpleName.endsWith("Inventory") || simpleName.endsWith("Dialog")
                        || simpleName.endsWith("Command") || simpleName.endsWith("Listener"))) continue;
                    String fqcn = pkg + "." + simpleName;
                    try {
                        Class<?> c = Class.forName(fqcn, false,
                            BlockProt.getInstance().getClass().getClassLoader());
                        int mods = c.getModifiers();
                        if (Modifier.isAbstract(mods) || c.isInterface() || c.isEnum()) continue;
                    } catch (Throwable e) {
                        BlockProtLogger.fail("Class coverage", fqcn + " failed to load: "
                            + e.getClass().getSimpleName() + ": " + e.getMessage());
                        ctx.fail();
                        continue;
                    }
                    total++;
                    if (!ctx.coveredClasses().contains(fqcn)) {
                        uncovered++;
                        BlockProtLogger.fail("Class coverage", simpleName
                            + " is exercised by no debug group (new screen? add it to a group)");
                    }
                }
            }
        } catch (Exception e) {
            BlockProtLogger.fail("Class coverage",
                e.getClass().getSimpleName() + ": " + e.getMessage());
            ctx.fail();
            return;
        }
        if (uncovered == 0) {
            BlockProtLogger.pass("Class coverage: " + (total - uncovered) + "/" + total
                + " screen classes exercised (inventories, dialogs, commands, listeners)");
            ctx.pass();
        } else {
            ctx.fail();
        }
    }
}
