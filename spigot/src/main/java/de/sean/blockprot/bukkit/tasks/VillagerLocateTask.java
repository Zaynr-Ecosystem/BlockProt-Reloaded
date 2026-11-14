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

import com.tcoded.folialib.wrapper.task.WrappedTask;
import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BukkitCompat;
import de.sean.blockprot.bukkit.listeners.VillagerWorkstationProtectionListener;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Emits particle beacons on a linked villager for a configurable duration so the
 * block owner can locate them in-world.
 *
 * <p>Runs every 10 ticks (0.5 s) and cancels itself after {@code durationSeconds}.
 * Particle type: DUST in magenta. Falls back to REDSTONE on older servers where
 * {@link BukkitCompat#PARTICLE_DUST} resolves to the legacy name.</p>
 */
public final class VillagerLocateTask implements Consumer<WrappedTask> {

    private final Player   viewer;
    private final Villager villager;
    private final Block    workstation;
    private final int      maxTicks;
    private int            elapsed = 0;

    private static final int TICKS_PER_PULSE = 10;

    public VillagerLocateTask(@NotNull Player viewer, @NotNull Villager villager,
                               @NotNull Block workstation, int durationSeconds) {
        this.viewer      = viewer;
        this.villager    = villager;
        this.workstation = workstation;
        this.maxTicks    = (durationSeconds * 20) / TICKS_PER_PULSE;
    }

    @Override
    public void accept(WrappedTask task) {
        if (!viewer.isOnline() || !villager.isValid()) {
            task.cancel();
            return;
        }

        elapsed++;
        Location particleLoc = villager.getLocation().add(0, 0.1, 0);
        BlockProt.getFoliaLib().getScheduler().runAtEntity(viewer, t -> {
            if (viewer.isOnline()) {
                spawnParticles(particleLoc);
            }
        });
        if (elapsed >= maxTicks) {
            task.cancel();
        }
    }

    private void spawnParticles(@NotNull Location loc) {
        try {
            // DUST_COLOR_TRANSITION is available on 1.17+; use a magenta -> white transition.
            Particle.DustTransition transition = new Particle.DustTransition(
                Color.fromRGB(255, 0, 200), Color.WHITE, 1.5f);
            viewer.spawnParticle(BukkitCompat.PARTICLE_DUST_COLOR_TRANSITION, loc,
                12, 0.2, 0.5, 0.2, 0, transition);
        } catch (Exception ignored) {
            // Fallback: plain DUST particle (1.13+ / older API)
            try {
                Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(255, 0, 200), 1.5f);
                viewer.spawnParticle(BukkitCompat.PARTICLE_DUST, loc, 12, 0.2, 0.5, 0.2, 0, dust);
            } catch (Exception ex) {
                // If even that fails (very old server), silently skip.
            }
        }
    }

    /**
     * Starts a locate task for the villager linked to {@code workstation}, if one exists.
     * Does nothing and returns false if no linked villager is found within the search radius.
     *
     * @param viewer    The player who will see the particles.
     * @param workstation The workstation block whose linked villager to locate.
     * @param durationSeconds How long to show particles (capped at 10 s).
     * @return true if a villager was found and the task was started, false otherwise.
     */
    public static boolean startIfLinked(@NotNull Player viewer, @NotNull Block workstation, int durationSeconds) {
        Villager villager = VillagerWorkstationProtectionListener.findLinkedVillager(workstation);
        if (villager == null) return false;

        int capped = Math.max(1, Math.min(10, durationSeconds));
        BlockProt.getFoliaLib().getScheduler().runAtEntityTimer(
            villager, new VillagerLocateTask(viewer, villager, workstation, capped), 1L, TICKS_PER_PULSE);
        return true;
    }
}