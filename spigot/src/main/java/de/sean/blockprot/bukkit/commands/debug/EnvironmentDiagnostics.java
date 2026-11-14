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
import de.sean.blockprot.bukkit.BukkitCompat;
import de.sean.blockprot.bukkit.Edition;
import de.sean.blockprot.bukkit.VersionCompat;
import de.sean.blockprot.bukkit.storage.PluginDataLayout;
import de.sean.blockprot.bukkit.tasks.UpdateChecker;
import de.sean.blockprot.util.SemanticVersion;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public final class EnvironmentDiagnostics {

    private EnvironmentDiagnostics() {
    }

    public static void run(@NotNull DiagnosticsContext ctx) {
        DiagnosticsContext.runDomain("1/10", "ENVIRONMENT & COMPATIBILITY");
        checkConfig(ctx);
        checkBukkitCompat(ctx);
        checkFoliaLib(ctx);
        checkEdition(ctx);
        checkProfileAndAuth(ctx);
        checkUpdateChecker(ctx);
        checkMetrics(ctx);
    }

    private static void checkConfig(@NotNull DiagnosticsContext ctx) {
        try {
            var cfg = BlockProt.getDefaultConfig();
            BlockProtLogger.pass("Config: friendDisabled=" + cfg.isFriendFunctionalityDisabled()
                + " maxBlocks=" + cfg.getMaxLockedBlockCount()
                + " lockEffects=" + cfg.isLockEffectEnabled()
                + " lockSound=" + cfg.isLockSoundEnabled()
                + " entityProtection=" + cfg.isEntityProtectionEnabled()
                + " raidDetection=" + BlockProt.getInstance().getConfig().getBoolean("raid_detection.enabled", true));
            ctx.pass();
        } catch (Exception e) {
            BlockProtLogger.fail("Config", e.getMessage());
            ctx.fail();
        }
    }

    private static void checkBukkitCompat(@NotNull DiagnosticsContext ctx) {
        try {
            var dust = BukkitCompat.PARTICLE_DUST;
            var dustTransition = BukkitCompat.PARTICLE_DUST_COLOR_TRANSITION;
            var enchant = BukkitCompat.GLOW_ENCHANT;
            if (dust == null || dustTransition == null || enchant == null) {
                BlockProtLogger.fail("BukkitCompat", "One or more fields resolved to null");
                ctx.fail();
                return;
            }
            BlockProtLogger.pass("BukkitCompat: PARTICLE_DUST=" + dust.name()
                + " TRANSITION=" + dustTransition.name()
                + " GLOW=" + enchant.getKey().getKey()
                + " newParticle=" + BukkitCompat.hasNewParticleNames()
                + " newEnchant=" + BukkitCompat.hasNewEnchantmentNames());
            ctx.pass();
        } catch (Exception e) {
            BlockProtLogger.fail("BukkitCompat", e.getMessage());
            ctx.fail();
        }
    }

    private static void checkFoliaLib(@NotNull DiagnosticsContext ctx) {
        try {
            var folia = BlockProt.getFoliaLib();
            BlockProtLogger.pass("FoliaLib: active (isFolia=" + folia.isFolia()
                + " isPaper=" + folia.isPaper() + " isSpigot=" + folia.isSpigot() + ")");
            ctx.pass();
        } catch (Exception e) {
            BlockProtLogger.fail("FoliaLib", e.getMessage());
            ctx.fail();
        }
    }

    private static void checkEdition(@NotNull DiagnosticsContext ctx) {
        Edition edition = Edition.current();
        boolean notOlder = edition.supports(VersionCompat.MAJOR, VersionCompat.MINOR, VersionCompat.PATCH)
            || edition.isNewerThanRange(VersionCompat.MAJOR, VersionCompat.MINOR, VersionCompat.PATCH);
        boolean marker = PluginDataLayout.readEditionMarker(BlockProt.getInstance().getDataFolder()) != null;
        if (notOlder && marker) {
            BlockProtLogger.pass("Edition: " + edition.id() + " range=" + edition.rangeLabel() + " marker=present");
            ctx.pass();
        } else {
            BlockProtLogger.fail("Edition", "id=" + edition.id() + " minecraftInRange=" + notOlder + " marker=" + marker);
            ctx.fail();
        }
    }

    private static void checkProfileAndAuth(@NotNull DiagnosticsContext ctx) {
        try {
            boolean online = Bukkit.getOnlineMode();
            BlockProtLogger.pass("Profile & Auth: online-mode=" + online
                + " (mojangLookup=" + (online ? "enabled" : "bypassed_offline_safe") + ")");
            ctx.pass();
        } catch (Exception e) {
            BlockProtLogger.fail("Profile & Auth", e.getMessage());
            ctx.fail();
        }
    }

    private static void checkUpdateChecker(@NotNull DiagnosticsContext ctx) {
        try {
            boolean enabled = BlockProt.getInstance().getConfig().getBoolean("check_for_updates", true);
            SemanticVersion latest = UpdateChecker.latestVersion;
            SemanticVersion current = new SemanticVersion(BlockProt.getPluginVersion());
            int cmp = (latest != null) ? current.compareTo(latest) : 0;
            String state = (latest == null) ? "not_checked_yet" : (cmp > 0 ? "ahead" : (cmp == 0 ? "up_to_date" : "outdated"));
            BlockProtLogger.pass("UpdateChecker: enabled=" + enabled
                + " repo=Zaynr-Ecosystem/BlockProt-Reloaded current=" + current
                + (latest != null ? " latest=" + latest + " (" + state + ")" : " (cached=none)"));
            ctx.pass();
        } catch (Exception e) {
            BlockProtLogger.fail("UpdateChecker", e.getMessage());
            ctx.fail();
        }
    }

    private static void checkMetrics(@NotNull DiagnosticsContext ctx) {
        try {
            boolean enabled = BlockProt.getInstance().getConfig().getBoolean("metrics", true);
            BlockProtLogger.pass("Metrics (bStats): enabled=" + enabled + " pluginId=31548");
            ctx.pass();
        } catch (Exception e) {
            BlockProtLogger.fail("Metrics", e.getMessage());
            ctx.fail();
        }
    }
}
