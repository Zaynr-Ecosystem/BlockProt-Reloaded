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
import de.sean.blockprot.bukkit.Edition;
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.admin.AdminAction;
import de.sean.blockprot.bukkit.admin.AdminTierManager;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.bukkit.tasks.UpdateChecker;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import de.sean.blockprot.bukkit.util.SkinCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Collections;
import java.util.List;

/**
 * Handles player join events: skin pre-fetch, update checks, raid alert delivery.
 */
public class JoinEventListener implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.updateCommands();

        // Pre-fetch the player's own skin so their head is ready on first GUI open.
        BlockProt.getFoliaLib().getScheduler().runAsync(asyncTask ->
            SkinCache.getOrFetch(player.getName(), player.getUniqueId()));

        if (player.isOp() || AdminTierManager.hasPermission(player, AdminAction.UPDATE)) {
            if (BlockProt.consumePendingUpgradeNotice()) {
                String currentVersion = BlockProt.getPluginVersion();
                UpdateChecker.GitHubRelease installed = UpdateChecker.installedRelease;
                String releaseUrl = installed != null && installed.getHtmlUrl() != null && !installed.getHtmlUrl().isBlank()
                    ? installed.getHtmlUrl()
                    : UpdateChecker.tagReleaseUrl(currentVersion, Edition.current());
                String raw = Translator.getRaw(TranslationKey.MESSAGES__UPDATE__UPGRADED_NOTICE);
                if (raw.isBlank()) {
                    raw = "<#98FF98>BlockProt Reloaded updated to v{version}! <#7EC8E3>Click to read release notes on GitHub.";
                }
                Component notice = MiniMessage.miniMessage().deserialize(raw.replace("{version}", currentVersion))
                    .clickEvent(ClickEvent.openUrl(releaseUrl))
                    .hoverEvent(HoverEvent.showText(Component.text(Translator.get(TranslationKey.MESSAGES__UPDATE__CLICK_HINT))));
                ComponentMessages.send(player, notice);
                if (installed != null) {
                    UpdateChecker.sendHighlightLines(player, installed, 4);
                }
            }
            if (BlockProt.getDefaultConfig().shouldNotifyAdminsOnJoin()) {
                UpdateChecker checker = UpdateChecker.forJoinActionBar(BlockProt.getPluginVersion(), player);
                BlockProt.getFoliaLib().getScheduler().runAsync(asyncTask -> checker.run());
            }
        }
        if (BlockProt.getDefaultConfig().publicIsFriendByDefault() && !player.hasPlayedBefore()) {
            new PlayerSettingsHandler(player).addEveryoneAsFriend();
        }

        // Show configuration guide on the very first server start
        if (BlockProt.isFirstStartThisSession() && player.isOp()) {
            if (!BlockProt.getDefaultConfig().hasConfiguredBlocks()) {
                String line = Translator.get(TranslationKey.MESSAGES__FIRST_RUN__HEADER)
                    .replace("{line}", " ".repeat(48));
                ComponentMessages.send(player, LegacyComponentSerializer.legacySection().deserialize(line));
                ComponentMessages.send(player, LegacyComponentSerializer.legacySection().deserialize(
                    Translator.get(TranslationKey.MESSAGES__FIRST_RUN__TITLE)));
                ComponentMessages.send(player, LegacyComponentSerializer.legacySection().deserialize(
                    Translator.get(TranslationKey.MESSAGES__FIRST_RUN__STEP1)
                        .replace("{command}", "/bp lockables")));
                ComponentMessages.send(player, LegacyComponentSerializer.legacySection().deserialize(
                    Translator.get(TranslationKey.MESSAGES__FIRST_RUN__STEP1_CLICK)
                        .replace("{command}", "/bp lockables"))
                    .clickEvent(ClickEvent.suggestCommand("/bp lockables")));
                ComponentMessages.send(player, LegacyComponentSerializer.legacySection().deserialize(
                    Translator.get(TranslationKey.MESSAGES__FIRST_RUN__STEP2)
                        .replace("{file}", "blocks.yml")));
                ComponentMessages.send(player, LegacyComponentSerializer.legacySection().deserialize(
                    Translator.get(TranslationKey.MESSAGES__FIRST_RUN__STEP3)
                        .replace("{url}", "https://github.com/VictorGugug/BlockProt-Reloaded")));
                ComponentMessages.send(player, LegacyComponentSerializer.legacySection().deserialize(
                    Translator.get(TranslationKey.MESSAGES__FIRST_RUN__STEP4)
                        .replace("{command}", "/bp recommended")));
                ComponentMessages.send(player, LegacyComponentSerializer.legacySection().deserialize(
                    Translator.get(TranslationKey.MESSAGES__FIRST_RUN__FOOTER)
                        .replace("{line}", " ".repeat(48))));
            }
        }

        // Deliver queued raid alerts to the owner on join.
        List<String> pending = RaidDetectionListener.popPendingAlerts(player.getUniqueId());
        if (pending != null && !pending.isEmpty()) {
            boolean hasTp = player.hasPermission(Permissions.BLOCKS_TP.key());
            for (String alertLine : pending) {
                Component msg = LegacyComponentSerializer.legacySection().deserialize(alertLine);
                if (hasTp) {
                    String tpLabel = Translator.get(TranslationKey.MESSAGES__RAID_TP_LABEL);
                    Component tpLink = LegacyComponentSerializer.legacySection().deserialize(tpLabel)
                        .clickEvent(ClickEvent.suggestCommand("/tp " + extractCoordsHint(alertLine)));
                    msg = msg.append(Component.space()).append(tpLink);
                }
                ComponentMessages.send(player, msg);
            }
        }
    }

    private String extractCoordsHint(String line) {
        int open  = line.indexOf('[');
        int close = line.indexOf(']', open);
        if (open < 0 || close < 0) return "";
        String coords = line.substring(open + 1, close).replace(",", "").trim();
        return coords;
    }
}