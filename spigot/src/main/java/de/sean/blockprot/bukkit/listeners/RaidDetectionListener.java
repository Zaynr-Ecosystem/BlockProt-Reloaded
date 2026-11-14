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

package de.sean.blockprot.bukkit.listeners;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.audit.AuditLogger;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.storage.ProtectedBlockCache;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import de.sean.blockprot.bukkit.util.TemporaryActionBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects explosion attempts near protected blocks and alerts owners.
 */
public final class RaidDetectionListener implements Listener {

    private static final Map<UUID, List<String>> pendingAlerts = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastAlertTimes = new ConcurrentHashMap<>();
    private static final UUID ENVIRONMENT_UUID = new UUID(0L, 0L);
    private static final long ALERT_COOLDOWN_MS = 5000L;

    public static void clear() {
        pendingAlerts.clear();
        lastAlertTimes.clear();
    }

    @Nullable
    public static List<String> popPendingAlerts(@NotNull UUID uuid) {
        return pendingAlerts.remove(uuid);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBlockExplode(@NotNull BlockExplodeEvent event) {
        if (BlockProt.getDefaultConfig().isWorldExcluded(event.getBlock().getWorld())) return;
        checkBlocks(event.blockList(), null, event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onEntityExplode(@NotNull EntityExplodeEvent event) {
        if (BlockProt.getDefaultConfig().isWorldExcluded(event.getEntity().getWorld())) return;
        checkBlocks(event.blockList(), event.getEntity(), event.getLocation());
    }

    private void checkBlocks(@NotNull List<Block> blocks, @Nullable Entity source, @NotNull Location origin) {
        if (!BlockProt.getInstance().getConfig().getBoolean("raid_detection.enabled", false)) return;
        for (Block block : blocks) {
            if (!BlockProt.getDefaultConfig().isLockable(block.getType(), block.getWorld())) continue;
            if (!ProtectedBlockCache.isProtected(block)) continue;

            BlockNBTHandler handler;
            try {
                handler = new BlockNBTHandler(block);
            } catch (RuntimeException ignored) {
                continue;
            }

            if (!handler.isProtected()) continue;

            Location loc = block.getLocation();
            String world = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";
            int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
            String material = block.getType().name();
            String actorDisplay = resolveActor(source);

            AuditLogger audit = BlockProt.getAuditLogger();
            if (audit != null) {
                UUID actorUuid = source instanceof Player p ? p.getUniqueId() : ENVIRONMENT_UUID;
                audit.log(actorUuid, actorDisplay, loc, AuditLogger.Action.RAID_EXPLOSION);
            }
            BlockProtLogger.log("raid-detection", Translator.get(TranslationKey.CONSOLE__RAID_LOG)
                .replace("{block}", material)
                .replace("{world}", world)
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y))
                .replace("{z}", String.valueOf(z))
                .replace("{actor}", actorDisplay));

            String ownerUuid = handler.getOwner();
            if (ownerUuid == null || ownerUuid.isBlank()) continue;

            UUID ownerId;
            try {
                ownerId = UUID.fromString(ownerUuid);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            long now = System.currentTimeMillis();
            Long lastAlert = lastAlertTimes.get(ownerId);
            if (lastAlert != null && (now - lastAlert) < ALERT_COOLDOWN_MS) {
                continue;
            }
            lastAlertTimes.put(ownerId, now);

            String alertMsg = Translator.get(TranslationKey.MESSAGES__RAID_ALERT)
                .replace("{block}", material)
                .replace("{world}", world)
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y))
                .replace("{z}", String.valueOf(z))
                .replace("{actor}", actorDisplay);

            String coordsMsg = Translator.get(TranslationKey.MESSAGES__RAID_COORDS)
                .replace("{block}", material)
                .replace("{world}", world)
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y))
                .replace("{z}", String.valueOf(z))
                .replace("{actor}", actorDisplay);

            org.bukkit.OfflinePlayer offlineOwner = Bukkit.getOfflinePlayer(ownerId);
            Player onlineOwner = offlineOwner.isOnline() ? offlineOwner.getPlayer() : null;

            if (onlineOwner != null) {
                sendAlertToOnline(onlineOwner, alertMsg, coordsMsg, loc);
            } else {
                pendingAlerts.computeIfAbsent(ownerId, k -> new java.util.ArrayList<>())
                    .add(coordsMsg);
            }
        }
    }

    private void sendAlertToOnline(@NotNull Player player, @NotNull String alertMsg,
                                   @NotNull String coordsMsg, @NotNull Location loc) {
        TemporaryActionBar.show(player, alertMsg, BlockProt.getDefaultConfig().getActionBarDurationTicks());

        boolean hasTp = player.hasPermission(Permissions.BLOCKS_TP.key());
        Component chat = buildChatComponent(coordsMsg, hasTp, loc);
        ComponentMessages.send(player, chat);
    }

    @NotNull
    private Component buildChatComponent(@NotNull String coordsMsg, boolean hasTp, @NotNull Location loc) {
        Component base = LegacyComponentSerializer.legacySection().deserialize(coordsMsg);
        if (!hasTp) return base;

        String world = loc.getWorld() != null ? loc.getWorld().getName() : "world";
        String tpLabel = Translator.get(TranslationKey.MESSAGES__RAID_TP_LABEL);
        Component tpLink = LegacyComponentSerializer.legacySection().deserialize(tpLabel)
            .clickEvent(ClickEvent.runCommand(
                String.format("/execute in %s run tp @s %d %d %d",
                    world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())));
        return base.append(Component.space()).append(tpLink);
    }

    @NotNull
    private String resolveActor(@Nullable Entity source) {
        if (source == null) return Translator.get(TranslationKey.MESSAGES__RAID_ACTOR_ENVIRONMENT);
        if (source instanceof Player p) return p.getName();
        return source.getType().name();
    }
}