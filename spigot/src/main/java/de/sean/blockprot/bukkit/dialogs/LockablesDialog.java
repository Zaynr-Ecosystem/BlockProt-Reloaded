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
import de.sean.blockprot.bukkit.config.WorldsConfig;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class LockablesDialog {

    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_MINT = TextColor.color(0x98FF98);
    private static final TextColor PASTEL_CORAL = TextColor.color(0xFF6B6B);
    private static final TextColor PASTEL_GOLD = TextColor.color(0xF0E6A0);
    private static final TextColor SOFT_BLUE = TextColor.color(0x7EC8E3);
    private static final TextColor PASTEL_PURPLE = TextColor.color(0xB39DDB);
    private static final TextColor PASTEL_ORANGE = TextColor.color(0xDFB98E);

    private static final int PER_PAGE = 6;

    private LockablesDialog() {}

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        show(player, backOrigin, "", 0);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin, int page) {
        show(player, backOrigin, "", page);
    }

    public static void showSearchPrompt(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
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
            p -> show(p, backOrigin)
        );
        bridge.showValueInput(player, title, body, field, query -> {
            show(player, backOrigin, query != null ? query.trim() : "", 0);
        }, backBtn);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin,
                            @NotNull String searchQuery, int page) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;
        DefaultConfig cfg = BlockProt.getDefaultConfig();
        boolean colorblind = new de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler(player).getColorblindMode();

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__HEADER)),
            PASTEL_GOLD, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        List<DialogButton> buttons = new ArrayList<>();

        if (!searchQuery.isBlank()) {
            List<Material> matches = BlockFamilyParser.searchMaterials(searchQuery);
            int totalPages = Math.max(1, (int) Math.ceil(matches.size() / (double) PER_PAGE));
            int safePage = Math.max(0, Math.min(page, totalPages - 1));
            int from = safePage * PER_PAGE;
            int to = Math.min(from + PER_PAGE, matches.size());
            List<Material> pageMats = matches.subList(from, to);

            body.add(DialogBodyEntry.text(Component.text(
                stripColor(Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__SEARCH_TITLE)).replace("{query}", searchQuery)
                + " | " + stripColor(Translator.get(TranslationKey.DIALOGS__PAGE))
                    .replace("{current}", String.valueOf(safePage + 1))
                    .replace("{total}", String.valueOf(totalPages)),
                SOFT_GRAY)));

            for (Material mat : pageMats) {
                boolean active = cfg.isLockable(mat) || cfg.isLockableEntity(mat);
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
                        cfg.toggleLockable(mat, p);
                        show(p, backOrigin, searchQuery, safePage);
                    }
                ));
            }

            BpDialogStyles.padToGrid(buttons, 6);

            // Fixed Bottom Navigation Row (3 columns): [ Prev ] [ Clear / Volver ] [ Next ]
            DialogButton prevBtn = safePage > 0
                ? new DialogButton("prev",
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), SOFT_GRAY),
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV_HINT)), TextColor.color(0x888888)),
                    p -> show(p, backOrigin, searchQuery, safePage - 1))
                : new DialogButton("prev_disabled",
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), TextColor.color(0x555555)),
                    Component.text(""),
                    p -> {});

            DialogButton clearBtn = new DialogButton("clear_search",
                Component.text(stripColor(Translator.get(TranslationKey.ICON__UNDO))
                    + stripColor(Translator.get(TranslationKey.DIALOGS__CLOSE)), SOFT_GRAY),
                Component.text(""),
                p -> show(p, backOrigin, "", 0)
            );

            DialogButton nextBtn = safePage + 1 < totalPages
                ? new DialogButton("next",
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), SOFT_GRAY),
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT_HINT)), TextColor.color(0x888888)),
                    p -> show(p, backOrigin, searchQuery, safePage + 1))
                : new DialogButton("next_disabled",
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), TextColor.color(0x555555)),
                    Component.text(""),
                    p -> {});

            buttons.add(prevBtn);
            buttons.add(clearBtn);
            buttons.add(nextBtn);
        } else {
            List<CategoryEntry> categories = buildCategoryInfo(cfg);
            int totalPages = Math.max(1, (int) Math.ceil(categories.size() / (double) PER_PAGE));
            int safePage = Math.max(0, Math.min(page, totalPages - 1));
            int from = safePage * PER_PAGE;
            int to = Math.min(from + PER_PAGE, categories.size());
            List<CategoryEntry> pageEntries = categories.subList(from, to);

            body.add(DialogBodyEntry.text(Component.text(
                stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__HEADER))
                + " | " + stripColor(Translator.get(TranslationKey.DIALOGS__PAGE))
                    .replace("{current}", String.valueOf(safePage + 1))
                    .replace("{total}", String.valueOf(totalPages)),
                SOFT_GRAY)));

            for (CategoryEntry entry : pageEntries) {
                boolean noneActive = entry.activeCount == 0;
                TextColor c = BpDialogStyles.stateColor(entry.activeCount, entry.totalCount);
                String catLabel = translateCategory(entry.label);
                String icon = BpDialogStyles.indicatorIcon(entry.activeCount, entry.totalCount, colorblind);

                buttons.add(new DialogButton("cat_" + entry.label,
                    Component.text()
                        .append(Component.text(icon, c))
                        .append(Component.text(catLabel, NamedTextColor.WHITE))
                        .append(Component.text(" (" + entry.activeCount + "/" + entry.totalCount + ")", TextColor.color(0x888888)))
                        .build(),
                    Component.join(JoinConfiguration.newlines(),
                        Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CAT_PREFIX)), SOFT_GRAY)
                            .append(Component.text(catLabel, NamedTextColor.WHITE)),
                        Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_ENABLE))
                            + " / " + stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_DISABLE)), TextColor.color(0x888888))),
                    p -> LockableCategoryDialog.show(p, backOrigin, entry.label, entry.materials)
                ));
            }

            BpDialogStyles.padToGrid(buttons, 6);

            // Fixed Bottom Navigation Row (3 columns): [ Prev ] [ Auto-Drop ] [ Next ]
            DialogButton prevBtn = safePage > 0
                ? new DialogButton("prev",
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), SOFT_GRAY),
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV_HINT)), TextColor.color(0x888888)),
                    p -> show(p, backOrigin, "", safePage - 1))
                : new DialogButton("prev_disabled",
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), TextColor.color(0x555555)),
                    Component.text(""),
                    p -> {});

            DialogButton autoDropBtn = new DialogButton("auto_drop",
                Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__AUTO_DROP)), SOFT_BLUE),
                Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__AUTO_DROP_LORE)), TextColor.color(0x888888)),
                p -> AutoDropDialog.show(p, backOrigin, q -> LockablesDialog.show(q, backOrigin))
            );

            DialogButton nextBtn = safePage + 1 < totalPages
                ? new DialogButton("next",
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), SOFT_GRAY),
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT_HINT)), TextColor.color(0x888888)),
                    p -> show(p, backOrigin, "", safePage + 1))
                : new DialogButton("next_disabled",
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), TextColor.color(0x555555)),
                    Component.text(""),
                    p -> {});

            buttons.add(prevBtn);
            buttons.add(autoDropBtn);
            buttons.add(nextBtn);
        }

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton backBtn = DialogNavigation.backButton(
            exitOrigin,
            exitOrigin == DialogOrigin.ADMIN_MENU ? p -> AdminMenuDialog.show(p) : null,
            exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__RETURN_PREVIOUS : null
        );

        bridge.showMultiAction(player, title, body, buttons, backBtn, 3);
    }

    private static int worldsConfigCount() {
        WorldsConfig wc = BlockProt.getWorldsConfig();
        if (wc == null) return 0;
        int count = 0;
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            if (wc.hasWorldConfig(w)) count++;
        }
        return count;
    }

    static List<CategoryEntry> buildCategoryInfo(DefaultConfig cfg) {
        Map<String, List<Material>> catMap = new LinkedHashMap<>();
        catMap.put("Chests", new ArrayList<>());
        catMap.put("Shulkers", new ArrayList<>());
        catMap.put("Furnaces", new ArrayList<>());
        catMap.put("Storage", new ArrayList<>());
        catMap.put("Signs", new ArrayList<>());
        catMap.put("Doors", new ArrayList<>());
        catMap.put("Trapdoors", new ArrayList<>());
        catMap.put("Beds", new ArrayList<>());
        catMap.put("Gates", new ArrayList<>());
        catMap.put("Workstations", new ArrayList<>());
        catMap.put("Interactive", new ArrayList<>());
        catMap.put("Entities", new ArrayList<>());

        for (Material m : Material.values()) {
            if (m.isLegacy()) continue;
            String n = m.name();
            List<Material> dest = null;
            if (n.contains("SHULKER_BOX")) dest = catMap.get("Shulkers");
            else if (n.endsWith("_DOOR") && !n.contains("TRAP") && !n.contains("BOAT")) dest = catMap.get("Doors");
            else if (n.contains("TRAPDOOR")) dest = catMap.get("Trapdoors");
            else if (n.endsWith("_BED")) dest = catMap.get("Beds");
            else if (n.contains("FENCE_GATE")) dest = catMap.get("Gates");
            else if ((n.contains("FURNACE") || n.equals("BLAST_FURNACE") || n.equals("SMOKER")) && m.isBlock()) dest = catMap.get("Furnaces");
            else if ((n.contains("CHEST") || n.equals("ENDER_CHEST")) && m.isBlock() && !n.contains("BOAT")) dest = catMap.get("Chests");
            else if (n.endsWith("_SIGN") || n.endsWith("_WALL_SIGN") || n.endsWith("_HANGING_SIGN") || n.endsWith("_WALL_HANGING_SIGN")) dest = catMap.get("Signs");
            else if ((n.equals("BARREL") || n.endsWith("_SHELF") || n.equals("DECORATED_POT") || n.equals("CHISELED_BOOKSHELF")
                || n.equals("CRAFTER") || n.equals("BREWING_STAND") || n.equals("HOPPER") || n.equals("DISPENSER")
                || n.equals("DROPPER") || n.equals("BEEHIVE") || n.equals("BEE_NEST") || n.equals("JUKEBOX")
                || n.equals("LECTERN") || n.equals("BEACON")) && m.isBlock()) dest = catMap.get("Storage");
            else if ((n.equals("GRINDSTONE") || n.equals("STONECUTTER") || n.equals("LOOM") || n.equals("CARTOGRAPHY_TABLE")
                || n.equals("SMITHING_TABLE") || n.equals("ENCHANTING_TABLE") || n.equals("FLETCHING_TABLE")
                || n.contains("ANVIL")) && m.isBlock()) dest = catMap.get("Workstations");
            else if ((n.contains("BUTTON") || n.contains("LEVER") || n.contains("DAYLIGHT_DETECTOR")
                || n.contains("PRESSURE_PLATE") || n.equals("OBSERVER") || n.equals("TARGET")
                || n.equals("NOTE_BLOCK") || n.equals("BELL") || n.equals("COMPOSTER")
                || n.equals("DRAGON_EGG") || n.contains("CAULDRON")) && m.isBlock()) dest = catMap.get("Interactive");
            else if (BlockFamilyParser.getFamilyMembers(BlockFamilyParser.Family.ENTITIES).contains(m)) dest = catMap.get("Entities");
            if (dest != null && !isKnownLockableMaterial(m)) dest = null;
            if (dest != null) dest.add(m);
        }

        List<CategoryEntry> result = new ArrayList<>();
        for (Map.Entry<String, List<Material>> e : catMap.entrySet()) {
            List<Material> mats = e.getValue();
            long activeCount = mats.stream().filter(m -> cfg.isLockable(m) || cfg.isLockableEntity(m)).count();
            result.add(new CategoryEntry(e.getKey(), mats, activeCount, mats.size()));
        }
        result.removeIf(ce -> ce.totalCount == 0);
        return result;
    }

    private static boolean isKnownLockableMaterial(@NotNull Material m) {
        for (BlockFamilyParser.Family family : BlockFamilyParser.Family.values()) {
            if (BlockFamilyParser.getFamilyMembers(family).contains(m)) return true;
        }
        return false;
    }

    static String translateCategory(String label) {
        return switch (label) {
            case "Chests" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__CHESTS));
            case "Shulkers" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__SHULKERS));
            case "Furnaces" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__FURNACES));
            case "Storage" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__STORAGE));
            case "Signs" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__SIGNS));
            case "Doors" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__DOORS));
            case "Trapdoors" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__TRAPDOORS));
            case "Beds" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__BEDS));
            case "Gates" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__GATES));
            case "Workstations" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__WORKSTATIONS));
            case "Interactive" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__INTERACTIVE));
            case "Entities" -> stripColor(Translator.get(TranslationKey.DIALOGS__LOCKABLES__CATEGORIES__ENTITIES));
            default -> label;
        };
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orxA-F]", "");
    }

    record CategoryEntry(String label, List<Material> materials, long activeCount, long totalCount) {}
}
