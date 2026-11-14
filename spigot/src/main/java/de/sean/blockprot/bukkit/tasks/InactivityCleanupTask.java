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

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.listeners.HopperEventListener;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Task that releases protected blocks from inactive players.
 * Only activated if {@code inactivity_cleanup_days > 0} in config.yml.
 * Runs once at server startup on an async thread.
 *
 * <p>Players are processed in batches of {@link #BATCH_SIZE} with a
 * {@link #BATCH_PAUSE_MS} pause between batches to avoid holding a
 * hard reference to all offline player objects simultaneously. On
 * large servers with thousands of historical players this prevents
 * a spike in heap usage.</p>
 */
public final class InactivityCleanupTask implements Runnable {

    private static final int  BATCH_SIZE     = 200;
    private static final long BATCH_PAUSE_MS = 50L; // ~1 server tick between batches

    /** Pre-compiled pattern for stripping legacy Minecraft color codes from console output. */
    private static final Pattern COLOR_STRIP = Pattern.compile("(?i)§[0-9A-FK-ORX]");

    private final long thresholdMs;

    public InactivityCleanupTask(int days) {
        this.thresholdMs = TimeUnit.DAYS.toMillis(days);
    }

    public static boolean isEligibleOffline(@NotNull OfflinePlayer offline, long cutoffMs) {
        if (offline.isOnline()) return false;
        long lastActivity = Math.max(offline.getLastSeen(), offline.getLastPlayed());
        return lastActivity > 0L && lastActivity <= cutoffMs;
    }

    @Override
    public void run() {
        long cutoff = System.currentTimeMillis() - thresholdMs;
        AtomicInteger total = new AtomicInteger(0);

        // getOfflinePlayers() returns all players who have ever joined.
        // We copy to a list so we can slice it into batches without keeping
        // the array alive for the full duration of the run.
        List<OfflinePlayer> all = Arrays.asList(Bukkit.getOfflinePlayers());
        int size = all.size();

        for (int batchStart = 0; batchStart < size; batchStart += BATCH_SIZE) {
            int batchEnd = Math.min(batchStart + BATCH_SIZE, size);
            List<OfflinePlayer> batch = all.subList(batchStart, batchEnd);

            List<Candidate> candidates = new ArrayList<>();
            for (OfflinePlayer offline : batch) {
                if (!isEligibleOffline(offline, cutoff)) continue;

                try {
                    var stat = new PlayerBlocksStatistic();
                    StatHandler.getStatisticByUuid(stat, offline.getUniqueId());

                    for (var entry : stat.get()) {
                        var loc = entry.get();
                        if (loc.getWorld() == null) continue;
                        candidates.add(new Candidate(loc.clone(), offline.getUniqueId()));
                    }
                } catch (Exception ignored) {}
            }

            if (!candidates.isEmpty()) {
                CompletableFuture.allOf(candidates.stream()
                    .map(candidate -> BlockProt.getFoliaLib().getScheduler().runAtLocation(
                        candidate.location(), task -> processCandidate(candidate, total)))
                    .toArray(CompletableFuture[]::new)).join();
            }

            // Yield between batches to avoid prolonged GC pressure.
            if (batchEnd < size) {
                try { Thread.sleep(BATCH_PAUSE_MS); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        final int freed = total.get();
        BlockProt.getFoliaLib().getScheduler().runNextTick(task -> {
            if (freed > 0) {
                String msg = Translator.get(TranslationKey.MESSAGES__INACTIVITY_CLEANUP_DONE)
                    .replace("{count}", String.valueOf(freed));
                // Strip color codes before printing to the console logger.
                BlockProt.getInstance().getLogger().info(COLOR_STRIP.matcher(msg).replaceAll(""));
                Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission(Permissions.USER_ADMIN.key()))
                    .forEach(p -> p.sendMessage(LegacyComponentSerializer.legacySection().deserialize(msg)));
            }
        });
    }

    private void processCandidate(@NotNull Candidate candidate, @NotNull AtomicInteger total) {
        try {
            var block = candidate.location().getWorld().getBlockAt(candidate.location());
            if (!BlockProt.getDefaultConfig().isLockable(block.getType())) return;
            var handler = new BlockNBTHandler(block);
            if (handler.isOwner(candidate.ownerUuid())) {
                handler.clear();
                handler.applyToOtherContainer();
                HopperEventListener.invalidate(block);
                StatHandler.removeContainerByUuid(candidate.ownerUuid(), candidate.location());
                total.incrementAndGet();
            }
        } catch (Exception ignored) {}
    }

    private record Candidate(Location location, UUID ownerUuid) {}
}
