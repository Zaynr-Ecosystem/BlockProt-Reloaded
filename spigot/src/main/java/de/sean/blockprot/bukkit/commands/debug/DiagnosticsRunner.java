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

import de.sean.blockprot.bukkit.*;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class DiagnosticsRunner {

    private DiagnosticsRunner() {
    }

    public static void runInteractive(@NotNull Player player) {
        AtomicInteger passed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        Set<String> coveredClasses = new HashSet<>();
        BlockProtLogger.startDebugReport();

        logEnvironmentInfo(player);

        ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__DEBUG__RUNNING_DIAGNOSTICS));
        ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__DEBUG__RESULTS_GO_TO_LOG));

        DiagnosticsContext ctx = new DiagnosticsContext(
            player, player.getLocation(), player.getUniqueId(), passed, failed, coveredClasses
        );

        EnvironmentDiagnostics.run(ctx);
        ConfigurationDiagnostics.run(ctx);
        LocalizationDiagnostics.run(ctx);
        StorageDiagnostics.run(ctx);

        BlockProt.getFoliaLib().getScheduler().runAtEntity(player, tickTask -> {
            DefaultConfig cfg = BlockProt.getDefaultConfig();
            boolean tempChestAdded = false;
            if (!cfg.isLockableTileEntity(Material.CHEST, player.getWorld()) && !cfg.isLockableBlock(Material.CHEST, player.getWorld())) {
                cfg.addTestLockable(Material.CHEST);
                tempChestAdded = true;
            }

            try {
                PersistenceDiagnostics.run(ctx);
                CommandsAndPermissionsDiagnostics.run(ctx);
                ListenersDiagnostics.run(ctx);
                UserInterfaceDiagnostics.run(ctx);
                UtilityDiagnostics.run(ctx);
                PlayerSimulationDiagnostics.run(ctx);

                int p2 = passed.get();
                int f2 = failed.get();
                int total = p2 + f2;
                BlockProtLogger.separator();
                BlockProtLogger.log("SUMMARY: " + p2 + " passed, " + f2 + " failed / " + total + " total");

                boolean ok = f2 == 0;
                ComponentMessages.sendLegacyActionBar(player, ok
                    ? Translator.get(TranslationKey.MESSAGES__DEBUG__CHECKS_PASSED_ACTIONBAR)
                        .replace("{passed}", String.valueOf(p2))
                    : Translator.get(TranslationKey.MESSAGES__DEBUG__CHECKS_FAILED_ACTIONBAR)
                        .replace("{failed}", String.valueOf(f2))
                        .replace("{total}", String.valueOf(total)));
                ComponentMessages.sendLegacy(player, ok
                    ? Translator.get(TranslationKey.MESSAGES__DEBUG__CHECKS_PASSED_CHAT)
                        .replace("{passed}", String.valueOf(p2))
                    : Translator.get(TranslationKey.MESSAGES__DEBUG__CHECKS_FAILED_CHAT)
                        .replace("{failed}", String.valueOf(f2)));

                File reportFile = BlockProtLogger.getDebugReportFile();
                BlockProtLogger.endDebugReport();
                if (reportFile != null) {
                    ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__DEBUG__LOG_PATH)
                        .replace("{path}", reportFile.getPath()));
                }
            } finally {
                if (tempChestAdded) {
                    cfg.removeTestLockable(Material.CHEST);
                }
            }
        });
    }

    public static void runHeadless(@NotNull CommandSender sender) {
        AtomicInteger passed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        Set<String> coveredClasses = new HashSet<>();
        BlockProtLogger.startDebugReport();

        logEnvironmentInfo(sender);

        ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__DEBUG__HEADLESS_START));

        World world = Bukkit.getWorlds().get(0);
        Location spawn = world.getSpawnLocation();
        Location origin = new Location(world, spawn.getBlockX(), world.getMaxHeight() - 2, spawn.getBlockZ());
        UUID actor = UUID.nameUUIDFromBytes("BlockProtDebugConsole".getBytes(StandardCharsets.UTF_8));
        DiagnosticsContext ctx = new DiagnosticsContext(null, origin, actor, passed, failed, coveredClasses);

        EnvironmentDiagnostics.run(ctx);
        ConfigurationDiagnostics.run(ctx);
        LocalizationDiagnostics.run(ctx);
        StorageDiagnostics.run(ctx);

        BlockProt.getFoliaLib().getScheduler().runAtLocation(origin, task -> {
            DefaultConfig cfg = BlockProt.getDefaultConfig();
            boolean tempChestAdded = false;
            if (!cfg.isLockableTileEntity(Material.CHEST, world) && !cfg.isLockableBlock(Material.CHEST, world)) {
                cfg.addTestLockable(Material.CHEST);
                tempChestAdded = true;
            }
            try {
                PersistenceDiagnostics.run(ctx);
                CommandsAndPermissionsDiagnostics.run(ctx);
                ListenersDiagnostics.run(ctx);
                UserInterfaceDiagnostics.run(ctx);
                UtilityDiagnostics.run(ctx);
                PlayerSimulationDiagnostics.run(ctx);

                int p2 = passed.get();
                int f2 = failed.get();
                int total = p2 + f2;
                BlockProtLogger.separator();
                BlockProtLogger.log("HEADLESS SUMMARY: " + p2 + " passed, " + f2 + " failed / " + total + " total");
                ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__DEBUG__HEADLESS_FINISHED)
                    .replace("{passed}", String.valueOf(p2))
                    .replace("{failed}", String.valueOf(f2))
                    .replace("{total}", String.valueOf(total)));
                File reportFile = BlockProtLogger.getDebugReportFile();
                BlockProtLogger.endDebugReport();
                if (reportFile != null) {
                    ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__DEBUG__LOG_PATH)
                        .replace("{path}", reportFile.getPath()));
                }
            } finally {
                if (tempChestAdded) {
                    cfg.removeTestLockable(Material.CHEST);
                }
            }
        });
    }

    private static void logEnvironmentInfo(@NotNull Player player) {
        BlockProtLogger.log("Session: " + java.time.LocalDateTime.now()
            + " | Plugin: " + Edition.current().displayName() + " " + BlockProt.getPluginVersion()
            + " | Player: " + player.getName() + " (" + player.getUniqueId() + ")");
        BlockProtLogger.log("Edition: " + Edition.current().displayName() + " " + BlockProt.getPluginVersion());
        BlockProtLogger.log("Server: " + Bukkit.getVersion()
            + " | API: " + Bukkit.getBukkitVersion()
            + " | Java: " + System.getProperty("java.version"));
        BlockProtLogger.log("Compat: " + VersionCompat.getDiagnosticString()
            + " | BukkitCompat: " + BukkitCompat.getDiagnosticString()
            + " | FoliaLib: [isFolia=" + BlockProt.getFoliaLib().isFolia()
            + ", isPaper=" + BlockProt.getFoliaLib().isPaper()
            + ", isSpigot=" + BlockProt.getFoliaLib().isSpigot() + "]");
    }

    private static void logEnvironmentInfo(@NotNull CommandSender sender) {
        BlockProtLogger.log("Session: " + java.time.LocalDateTime.now()
            + " | Plugin: " + Edition.current().displayName() + " " + BlockProt.getPluginVersion()
            + " | Sender: " + sender.getName() + " (Console/Headless)");
        BlockProtLogger.log("Server: " + Bukkit.getVersion()
            + " | API: " + Bukkit.getBukkitVersion()
            + " | Java: " + System.getProperty("java.version"));
        BlockProtLogger.log("Compat: " + VersionCompat.getDiagnosticString()
            + " | BukkitCompat: " + BukkitCompat.getDiagnosticString()
            + " | FoliaLib: [isFolia=" + BlockProt.getFoliaLib().isFolia()
            + ", isPaper=" + BlockProt.getFoliaLib().isPaper()
            + ", isSpigot=" + BlockProt.getFoliaLib().isSpigot() + "]");
    }
}
