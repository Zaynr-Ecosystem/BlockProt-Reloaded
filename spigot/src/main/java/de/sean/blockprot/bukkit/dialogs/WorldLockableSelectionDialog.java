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

package de.sean.blockprot.bukkit.dialogs;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.config.WorldsConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Dialog counterpart of {@code WorldLockableSelectionInventory}: lists the
 * loaded worlds and drills into the per-world lockable detail screen.
 */
public final class WorldLockableSelectionDialog {

    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_MINT = TextColor.color(0x98FF98);
    private static final TextColor PASTEL_CORAL = TextColor.color(0xFF6B6B);
    private static final TextColor PASTEL_GOLD = TextColor.color(0xF0E6A0);

    private static final int PER_PAGE = 6;

    private WorldLockableSelectionDialog() {}

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        show(player, backOrigin, 0);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin, int page) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        List<World> worlds = Bukkit.getWorlds();
        int totalPages = Math.max(1, (int) Math.ceil(worlds.size() / (double) PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * PER_PAGE;
        int to = Math.min(from + PER_PAGE, worlds.size());
        List<World> pageWorlds = worlds.subList(from, to);

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__LOCKABLES__TITLE))
                + " - " + stripColor(Translator.get(TranslationKey.WORLDS__WORLDS)),
            PASTEL_GOLD, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.WORLDS__PER_WORLD_CONFIG)), SOFT_GRAY)));
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__PAGE))
                .replace("{current}", String.valueOf(safePage + 1))
                .replace("{total}", String.valueOf(totalPages)),
            TextColor.color(0x888888))));

        List<DialogButton> buttons = new ArrayList<>();

        if (worlds.isEmpty()) {
            body.add(DialogBodyEntry.text(Component.text(
                stripColor(Translator.get(TranslationKey.WORLDS__NO_WORLDS)), SOFT_GRAY)));
        }

        WorldsConfig wc = BlockProt.getWorldsConfig();
        Map<String, Integer> counts = wc != null ? wc.getAllWorldLockedCounts() : Map.of();

        for (World world : pageWorlds) {
            String wName = world.getName();
            boolean enabled = wc == null || wc.hasWorldConfig(world) || !wc.isWorldDisabled(world);
            int count = counts.getOrDefault(wName, 0);
            TextColor c = enabled ? PASTEL_MINT : PASTEL_CORAL;

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(stripColor(Translator.get(TranslationKey.WORLDS__STATUS))
                + ": " + stripColor(Translator.get(enabled
                    ? TranslationKey.ENABLED : TranslationKey.DISABLED)), c));
            lore.add(Component.text(stripColor(Translator.get(TranslationKey.WORLDS__PROTECTED_COUNT))
                + ": " + (count >= 0 ? String.valueOf(count) : "?")
                + stripColor(Translator.get(TranslationKey.INVENTORIES__LOCKABLE_SELECTION__BLOCKS_SUFFIX)),
                TextColor.color(0x888888)));
            if (wc != null && wc.hasWorldConfig(world)) {
                lore.add(Component.text(stripColor(Translator.get(TranslationKey.WORLDS__WORLD_CONFIG_HINT)),
                    TextColor.color(0x888888)));
            }

            boolean colorblind = new de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler(player).getColorblindMode();
            String icon = BpDialogStyles.indicatorIcon(enabled, colorblind);

            buttons.add(new DialogButton("world_" + wName,
                Component.text()
                    .append(Component.text(icon, c))
                    .append(Component.text(wName, NamedTextColor.WHITE))
                    .build(),
                Component.join(JoinConfiguration.newlines(), lore),
                p -> WorldLockableDetailDialog.show(p, backOrigin, world)));
        }

        List<DialogButton> navButtons = new ArrayList<>();
        if (safePage > 0) {
            int prevPage = safePage - 1;
            navButtons.add(new DialogButton("prev",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV_HINT)), TextColor.color(0x888888)),
                p -> show(p, backOrigin, prevPage)));
        }
        if (safePage + 1 < totalPages) {
            int nextPage = safePage + 1;
            navButtons.add(new DialogButton("next",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT_HINT)), TextColor.color(0x888888)),
                p -> show(p, backOrigin, nextPage)));
        }

        BpDialogStyles.padToRowEnd(buttons, 2);
        buttons.addAll(navButtons);

        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            p -> LockablesDialog.show(p, backOrigin));

        bridge.showMultiAction(player, title, body, buttons, backBtn, 2);
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orxA-F]", "");
    }
}