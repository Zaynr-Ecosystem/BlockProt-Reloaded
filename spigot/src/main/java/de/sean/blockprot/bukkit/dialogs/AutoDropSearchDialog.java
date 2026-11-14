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
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dialog counterpart of {@code AutoDropSearchInventory}: paged per-material
 * toggles for a block name search, in relevance order.
 */
public final class AutoDropSearchDialog {

    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_MINT = TextColor.color(0x98FF98);
    private static final TextColor PASTEL_CORAL = TextColor.color(0xFF6B6B);
    private static final TextColor PASTEL_GOLD = TextColor.color(0xF0E6A0);

    private static final int PER_PAGE = 9;

    private AutoDropSearchDialog() {}

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin,
                            @Nullable DialogButton.DialogClickHandler parentBack,
                            @NotNull String query, int page) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        DefaultConfig cfg = BlockProt.getDefaultConfig();
        List<Material> materials = BlockFamilyParser.searchMaterials(query);

        int totalPages = Math.max(1, (int) Math.ceil(materials.size() / (double) PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * PER_PAGE;
        int to = Math.min(from + PER_PAGE, materials.size());
        List<Material> pageMats = materials.subList(from, to);

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__SEARCH_TITLE))
                .replace("{query}", query),
            PASTEL_GOLD, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__PAGE))
                .replace("{current}", String.valueOf(safePage + 1))
                .replace("{total}", String.valueOf(totalPages)),
            TextColor.color(0x888888))));

        List<DialogButton> buttons = new ArrayList<>();
        boolean colorblind = new de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler(player).getColorblindMode();
        for (Material mat : pageMats) {
            boolean active = cfg.getAutoDropToInventoryBlocks().contains(mat);
            TextColor c = active ? PASTEL_MINT : PASTEL_CORAL;
            String displayName = LockableCategoryDialog.formatMaterialName(mat.name());
            String icon = colorblind
                ? (active ? "✔ " : "✖ ")
                : stripColor(Translator.get(active ? TranslationKey.ICON__TOGGLE_ON : TranslationKey.ICON__TOGGLE_OFF));

            buttons.add(new DialogButton("mat_" + mat.name(),
                Component.text()
                    .append(Component.text(icon, c))
                    .append(Component.text(displayName, NamedTextColor.WHITE))
                    .build(),
                Component.join(JoinConfiguration.newlines(),
                    Component.text(mat.name(), SOFT_GRAY),
                    Component.text(active
                        ? stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_DISABLE_SINGLE))
                        : stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_ENABLE_SINGLE)), c)),
                p -> {
                    cfg.toggleAutoDropMaterial(mat, p);
                    show(p, backOrigin, parentBack, query, safePage);
                }
            ));
        }

        BpDialogStyles.padToGrid(buttons, PER_PAGE);

        DialogButton prevBtn = safePage > 0
            ? new DialogButton("prev",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV_HINT)), TextColor.color(0x888888)),
                p -> show(p, backOrigin, parentBack, query, safePage - 1))
            : new DialogButton("prev_disabled",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), TextColor.color(0x555555)),
                Component.text(""),
                p -> {});

        DialogButton spacerBtn = new DialogButton("spacer_nav", Component.empty(), null, p -> {});

        DialogButton nextBtn = safePage + 1 < totalPages
            ? new DialogButton("next",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT_HINT)), TextColor.color(0x888888)),
                p -> show(p, backOrigin, parentBack, query, safePage + 1))
            : new DialogButton("next_disabled",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), TextColor.color(0x555555)),
                Component.text(""),
                p -> {});

        buttons.add(prevBtn);
        buttons.add(spacerBtn);
        buttons.add(nextBtn);

        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            p -> AutoDropDialog.show(p, backOrigin, parentBack)
        );

        bridge.showMultiAction(player, title, body, buttons, backBtn, 3);
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orxA-F]", "");
    }
}