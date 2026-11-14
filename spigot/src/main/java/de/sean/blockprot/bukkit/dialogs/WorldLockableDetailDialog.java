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
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dialog counterpart of {@code WorldLockableDetailInventory}: per-material
 * lockable toggles for a single world, stored in that world's list inside
 * {@code worlds.yml}.
 */
public final class WorldLockableDetailDialog {

    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_MINT = TextColor.color(0x98FF98);
    private static final TextColor PASTEL_CORAL = TextColor.color(0xFF6B6B);
    private static final TextColor PASTEL_GOLD = TextColor.color(0xF0E6A0);

    private static final int PER_PAGE = 9;

    private WorldLockableDetailDialog() {}

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin, @NotNull World world) {
        show(player, backOrigin, world, 0);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin,
                            @NotNull World world, int page) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        List<MaterialEntry> all = buildAllEntries(world);
        int totalPages = Math.max(1, (int) Math.ceil(all.size() / (double) PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * PER_PAGE;
        int to = Math.min(from + PER_PAGE, all.size());
        List<MaterialEntry> pageEntries = all.subList(from, to);

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__LOCKABLES__TITLE))
                + " - " + world.getName(),
            PASTEL_GOLD, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__PAGE))
                .replace("{current}", String.valueOf(safePage + 1))
                .replace("{total}", String.valueOf(totalPages)),
            TextColor.color(0x888888))));

        boolean colorblind = new de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler(player).getColorblindMode();
        List<DialogButton> buttons = new ArrayList<>();
        for (MaterialEntry entry : pageEntries) {
            boolean active = entry.active();
            TextColor c = active ? PASTEL_MINT : PASTEL_CORAL;
            String displayName = LockableCategoryDialog.formatMaterialName(entry.material().name());
            String icon = BpDialogStyles.indicatorIcon(active, colorblind);

            buttons.add(new DialogButton("mat_" + entry.material().name(),
                Component.text()
                    .append(Component.text(icon, c))
                    .append(Component.text(displayName, NamedTextColor.WHITE))
                    .build(),
                Component.join(JoinConfiguration.newlines(),
                    Component.text(entry.material().name(), SOFT_GRAY),
                    Component.text(active
                        ? stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_DISABLE_SINGLE))
                        : stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_ENABLE_SINGLE)), c)),
                p -> {
                    toggleInWorld(entry.material(), world, p);
                    show(p, backOrigin, world, safePage);
                }));
        }

        BpDialogStyles.padToGrid(buttons, PER_PAGE);

        DialogButton prevBtn = safePage > 0
            ? new DialogButton("prev",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV_HINT)), TextColor.color(0x888888)),
                p -> show(p, backOrigin, world, safePage - 1))
            : new DialogButton("prev_disabled",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), TextColor.color(0x555555)),
                Component.text(""),
                p -> {});

        DialogButton spacerBtn = new DialogButton("spacer_nav", Component.empty(), null, p -> {});

        DialogButton nextBtn = safePage + 1 < totalPages
            ? new DialogButton("next",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), SOFT_GRAY),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT_HINT)), TextColor.color(0x888888)),
                p -> show(p, backOrigin, world, safePage + 1))
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
            p -> WorldLockableSelectionDialog.show(p, backOrigin));

        bridge.showMultiAction(player, title, body, buttons, backBtn, 3);
    }

    private static void toggleInWorld(@NotNull Material material, @NotNull World world, @NotNull Player who) {
        File worldsFile = new File(BlockProt.getInstance().getDataFolder(), "worlds.yml");
        if (!worldsFile.exists()) return;

        BlockFamilyParser.Family family = familyOf(material);
        if (family == null) return;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(worldsFile);
        String key = "worlds." + world.getName();
        String listKey = key + "." + DefaultConfig.configKeyForFamily(family);

        Set<Material> active = new LinkedHashSet<>(BlockFamilyParser.parse(cfg.get(listKey), family));
        boolean currentlyActive = active.contains(material);
        if (currentlyActive) {
            active.remove(material);
        } else {
            active.add(material);
        }

        String expression = BlockFamilyParser.toFamilyExpression(active, family);
        cfg.set(listKey, List.of(expression != null ? expression : "[]"));

        if (BlockProt.getInstance().getFileWatcher() != null) {
            BlockProt.getInstance().getFileWatcher().suppressNext();
        }
        try {
            cfg.save(worldsFile);
        } catch (IOException ex) {
            BlockProtLogger.warn("Failed to save worlds.yml for " + world.getName() + ": " + ex.getMessage());
        }

        if (BlockProt.getInstance().getFileWatcher() != null) {
            BlockProt.getInstance().getFileWatcher().requestProgrammaticReload();
        }

        String actionText = currentlyActive
            ? Translator.get(TranslationKey.DISABLED)
            : Translator.get(TranslationKey.ENABLED);
        BlockProtLogger.log("world-lockables-toggle",
            actionText + " " + material.name() + " in world " + world.getName()
                + " (by " + who.getName() + ")");
        if (who.isOnline()) {
            ComponentMessages.sendActionBar(who, LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__LOCKABLES__TOGGLE_FEEDBACK)
                    .replace("{action}", actionText)
                    .replace("{name}", material.name() + " §7(" + world.getName() + ")")));
        }
    }

    @Nullable
    private static BlockFamilyParser.Family familyOf(@NotNull Material m) {
        for (BlockFamilyParser.Family family : BlockFamilyParser.Family.values()) {
            if (BlockFamilyParser.getFamilyMembers(family).contains(m)) return family;
        }
        return null;
    }

    @NotNull
    private static List<MaterialEntry> buildAllEntries(@NotNull World world) {
        DefaultConfig cfg = BlockProt.getDefaultConfig();
        List<MaterialEntry> result = new ArrayList<>();

        File worldsFile = new File(BlockProt.getInstance().getDataFolder(), "worlds.yml");
        YamlConfiguration wcfg = worldsFile.exists() ? YamlConfiguration.loadConfiguration(worldsFile) : null;

        for (BlockFamilyParser.Family family : BlockFamilyParser.Family.values()) {
            String configKey = DefaultConfig.configKeyForFamily(family);
            if (configKey == null) continue;

            String fullKey = "worlds." + world.getName() + "." + configKey;
            boolean worldHasList = wcfg != null && wcfg.contains(fullKey);
            Set<Material> worldActive = worldHasList
                ? BlockFamilyParser.parse(wcfg.get(fullKey), family)
                : Set.of();

            for (Material m : BlockFamilyParser.getFamilyMembers(family)) {
                boolean global = cfg.isLockable(m) || cfg.isLockableEntity(m);
                boolean active = worldHasList ? worldActive.contains(m) : global;
                result.add(new MaterialEntry(m, active));
            }
        }
        result.sort(Comparator.comparing(e -> e.material().name()));
        return result;
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orxA-F]", "");
    }

    private record MaterialEntry(@NotNull Material material, boolean active) {}
}