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

package de.sean.blockprot.bukkit.bedrock.forms;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.bedrock.BedrockBridge;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.LocationListEntry;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;

/**
 * Native Bedrock statistics form for {@code /bp stats}.
 */
public final class BedrockStatsForm {

    private BedrockStatsForm() {}

    public static void showUserStats(@NotNull Player player) {
        if (!org.bukkit.Bukkit.isPrimaryThread() && (!BlockProt.hasFoliaLib() || !BlockProt.getFoliaLib().getScheduler().isOwnedByCurrentRegion(player))) {
            BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                if (player.isOnline()) showUserStats(player);
            });
            return;
        }
        String title = stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__PLACEMENTS));

        PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
        StatHandler.getStatistic(stat, player);
        List<LocationListEntry> entries = stat.get();

        int total = entries.size();
        StringBuilder content = new StringBuilder();
        content.append(stripColor(Translator.get(TranslationKey.INVENTORIES__USER_MENU__PLACEMENTS)))
            .append(": ").append(total).append("\n\n");

        for (int i = 0; i < Math.min(entries.size(), 10); i++) {
            Location loc = entries.get(i).get();
            if (loc != null && loc.getWorld() != null) {
                content.append(loc.getBlock().getType().name())
                    .append(" (").append(loc.getBlockX()).append(", ")
                    .append(loc.getBlockY()).append(", ")
                    .append(loc.getBlockZ()).append(")\n");
            }
        }

        String back = stripColor(Translator.get(TranslationKey.DIALOGS__BACK));

        SimpleForm.Builder builder = SimpleForm.builder()
            .title(title)
            .content(content.toString())
            .button(back, FormImage.Type.PATH, "textures/ui/back_button_default")
            .validResultHandler(response -> BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                if (player.isOnline()) BedrockUserMenuForm.show(player);
            }));

        BedrockBridge.sendForm(player, builder.build());
    }
}
