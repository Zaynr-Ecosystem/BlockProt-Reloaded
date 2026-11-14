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
import java.util.Set;
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
 * Dialog counterpart of {@code AutoDropInventory}: lists the families eligible
 * for auto_drop_to_inventory and drills into {@link AutoDropFamilyDialog} per family.
 */
public final class AutoDropDialog {

    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_MINT = TextColor.color(0x98FF98);
    private static final TextColor PASTEL_CORAL = TextColor.color(0xFF6B6B);
    private static final TextColor PASTEL_GOLD = TextColor.color(0xF0E6A0);
    private static final TextColor PASTEL_ORANGE = TextColor.color(0xDFB98E);
    private static final TextColor SOFT_BLUE = TextColor.color(0x7EC8E3);

    private static final List<BlockFamilyParser.Family> FAMILIES = List.of(
        BlockFamilyParser.Family.TILE_ENTITIES,
        BlockFamilyParser.Family.SHULKER_BOXES,
        BlockFamilyParser.Family.BLOCKS,
        BlockFamilyParser.Family.DOORS,
        BlockFamilyParser.Family.ENTITIES
    );

    private AutoDropDialog() {}

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        show(player, backOrigin, null, "", 0);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin,
                            @Nullable DialogButton.DialogClickHandler parentBack) {
        show(player, backOrigin, parentBack, "", 0);
    }

    public static void showSearchPrompt(@NotNull Player player, @NotNull DialogOrigin backOrigin,
                                        @Nullable DialogButton.DialogClickHandler parentBack) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__SEARCH)),
            PASTEL_GOLD, TextDecoration.BOLD
        );
        List<DialogBodyEntry> body = List.of(
            DialogBodyEntry.text(Component.text(
                stripColor(Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__SEARCH_TITLE)).replace("{query}", "..."),
                SOFT_GRAY))
        );
        DialogTextField field = DialogTextField.of(
            "search_query",
            Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__SEARCH))),
            "",
            stripColor(Translator.get(TranslationKey.ICON__SEARCH))
                + stripColor(Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__SEARCH))
        );
        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            p -> show(p, backOrigin, parentBack)
        );
        bridge.showValueInput(player, title, body, field, query -> {
            show(player, backOrigin, parentBack, query != null ? query.trim() : "", 0);
        }, backBtn);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin,
                            @Nullable DialogButton.DialogClickHandler parentBack,
                            @NotNull String searchQuery, int page) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        DefaultConfig cfg = BlockProt.getDefaultConfig();
        Set<Material> autoDropBlocks = cfg.getAutoDropToInventoryBlocks();
        boolean colorblind = new de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler(player).getColorblindMode();

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__TITLE)),
            PASTEL_GOLD, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        List<DialogButton> buttons = new ArrayList<>();

        if (!searchQuery.isBlank()) {
            final int perPage = 6;
            List<Material> matches = BlockFamilyParser.searchMaterials(searchQuery);
            int totalPages = Math.max(1, (int) Math.ceil(matches.size() / (double) perPage));
            int safePage = Math.max(0, Math.min(page, totalPages - 1));
            int from = safePage * perPage;
            int to = Math.min(from + perPage, matches.size());
            List<Material> pageMats = matches.subList(from, to);

            body.add(DialogBodyEntry.text(Component.text(
                stripColor(Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__SEARCH_TITLE)).replace("{query}", searchQuery)
                + " | " + stripColor(Translator.get(TranslationKey.DIALOGS__PAGE))
                    .replace("{current}", String.valueOf(safePage + 1))
                    .replace("{total}", String.valueOf(totalPages)),
                SOFT_GRAY)));

            for (Material mat : pageMats) {
                boolean active = autoDropBlocks.contains(mat);
                TextColor c = active ? PASTEL_MINT : PASTEL_CORAL;
                String displayName = LockableCategoryDialog.formatMaterialName(mat.name());
                String icon = BpDialogStyles.indicatorIcon(active, colorblind);

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
                        show(p, backOrigin, parentBack, searchQuery, safePage);
                    }
                ));
            }

            BpDialogStyles.padToGrid(buttons, 6);

            // Fixed Bottom Navigation Row (3 columns): [ Prev ] [ Clear / Volver ] [ Next ]
            DialogButton prevBtn = safePage > 0
                ? new DialogButton("prev",
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), SOFT_GRAY),
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV_HINT)), TextColor.color(0x888888)),
                    p -> show(p, backOrigin, parentBack, searchQuery, safePage - 1))
                : new DialogButton("prev_disabled",
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), TextColor.color(0x555555)),
                    Component.text(""),
                    p -> {});

            DialogButton clearBtn = new DialogButton("clear_search",
                Component.text(stripColor(Translator.get(TranslationKey.ICON__UNDO))
                    + stripColor(Translator.get(TranslationKey.DIALOGS__CLOSE)), SOFT_GRAY),
                Component.text(""),
                p -> show(p, backOrigin, parentBack, "", 0)
            );

            DialogButton nextBtn = safePage + 1 < totalPages
                ? new DialogButton("next",
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), SOFT_GRAY),
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT_HINT)), TextColor.color(0x888888)),
                    p -> show(p, backOrigin, parentBack, searchQuery, safePage + 1))
                : new DialogButton("next_disabled",
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), TextColor.color(0x555555)),
                    Component.text(""),
                    p -> {});

            buttons.add(prevBtn);
            buttons.add(clearBtn);
            buttons.add(nextBtn);
        } else {
            body.add(DialogBodyEntry.text(Component.text(
                stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__AUTO_DROP_LORE)), SOFT_GRAY)));

            for (BlockFamilyParser.Family family : FAMILIES) {
                Set<Material> members = BlockFamilyParser.getFamilyMembers(family);
                long active = members.stream().filter(autoDropBlocks::contains).count();
                long total = members.size();
                boolean noneActive = active == 0;
                TextColor c = BpDialogStyles.stateColor(active, total);
                String label = friendlyName(family.name());
                String icon = BpDialogStyles.indicatorIcon(active, total, colorblind);

                buttons.add(new DialogButton("family_" + family.name(),
                    Component.text()
                        .append(Component.text(icon, c))
                        .append(Component.text(label, NamedTextColor.WHITE))
                        .append(Component.text(" (" + active + "/" + total + ")", TextColor.color(0x888888)))
                        .build(),
                    Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__LEFT_CLICK_HINT)), TextColor.color(0x888888)),
                    p -> AutoDropFamilyDialog.show(p, backOrigin, family, parentBack)
                ));
            }

            BpDialogStyles.padToGrid(buttons, 6);

            // Fixed Bottom Navigation Row (3 columns): [ Prev ] [ Search Blocks ] [ Next ]
            DialogButton prevBtn = new DialogButton("prev_disabled",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), TextColor.color(0x555555)),
                Component.text(""),
                p -> {});

            DialogButton searchBtn = new DialogButton("search_prompt",
                Component.text(stripColor(Translator.get(TranslationKey.ICON__SEARCH))
                    + stripColor(Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__SEARCH)), PASTEL_GOLD),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_TO_OPEN)), TextColor.color(0x888888)),
                p -> showSearchPrompt(p, backOrigin, parentBack)
            );

            DialogButton nextBtn = new DialogButton("next_disabled",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), TextColor.color(0x555555)),
                Component.text(""),
                p -> {});

            buttons.add(prevBtn);
            buttons.add(searchBtn);
            buttons.add(nextBtn);
        }

        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            parentBack != null
                ? parentBack
                : backOrigin == DialogOrigin.ADMIN_MENU ? p -> AdminMenuDialog.show(p) : null
        );

        bridge.showMultiAction(player, title, body, buttons, backBtn, 3);
    }

    @NotNull
    private static String friendlyName(@NotNull String name) {
        String[] words = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orxA-F]", "");
    }
}
