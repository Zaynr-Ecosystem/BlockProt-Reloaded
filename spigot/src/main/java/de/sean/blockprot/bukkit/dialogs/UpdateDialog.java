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
import de.sean.blockprot.bukkit.tasks.UpdateChecker;
import de.sean.blockprot.bukkit.tasks.UpdateChecker.GitHubRelease;
import de.sean.blockprot.util.SemanticVersion;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_CORAL;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_GOLD;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_MINT;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_BLUE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_GRAY;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class UpdateDialog {

    private UpdateDialog() {}

    public static void show(@NotNull Player player) {
        show(player, DialogOrigin.ADMIN_MENU);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__UPDATE__TITLE)),
            SOFT_BLUE, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.empty()));

        String pluginVersion = BlockProt.getPluginVersion();
        body.add(DialogBodyEntry.text(
            Component.text()
                .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__UPDATE__CURRENT)), SOFT_GRAY))
                .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__UPDATE__VERSION_PREFIX)) + pluginVersion, NamedTextColor.WHITE, TextDecoration.BOLD))
                .build()
        ));

        SemanticVersion cached = UpdateChecker.latestVersion;
        GitHubRelease cachedRelease = UpdateChecker.latestRelease;
        SemanticVersion currentVersion = new SemanticVersion(pluginVersion);

        if (cached != null) {
            String releaseUrl = cachedRelease != null
                    && cachedRelease.getHtmlUrl() != null
                    && !cachedRelease.getHtmlUrl().isBlank()
                ? cachedRelease.getHtmlUrl()
                : "https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/releases/latest";

            body.add(DialogBodyEntry.text(Component.empty()));
            body.add(DialogBodyEntry.text(
                Component.text()
                    .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__UPDATE__LATEST)), SOFT_GRAY))
                    .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__UPDATE__VERSION_PREFIX)) + cached.toString(), NamedTextColor.WHITE, TextDecoration.BOLD))
                    .build()
            ));

            if (cached.compareTo(currentVersion) > 0) {
                body.add(DialogBodyEntry.text(
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__UPDATE__OUTDATED)), PASTEL_CORAL, TextDecoration.BOLD)
                ));
            } else if (cached.compareTo(currentVersion) < 0) {
                body.add(DialogBodyEntry.text(
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__UPDATE__AHEAD)), PASTEL_GOLD, TextDecoration.BOLD)
                ));
            } else {
                body.add(DialogBodyEntry.text(
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__UPDATE__UP_TO_DATE)), PASTEL_MINT, TextDecoration.BOLD)
                ));
            }

            body.add(DialogBodyEntry.text(Component.empty()));
            body.add(DialogBodyEntry.text(kindLabel(cached)));

            if (cached.compareTo(currentVersion) > 0) {
                body.add(DialogBodyEntry.text(
                    Component.text()
                        .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__UPDATE__DOWNLOAD)), SOFT_GRAY))
                        .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__UPDATE__VERSION_PREFIX)) + cached.toString(), SOFT_BLUE, TextDecoration.UNDERLINED)
                            .clickEvent(ClickEvent.openUrl(releaseUrl))
                            .hoverEvent(HoverEvent.showText(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_TO_OPEN)), SOFT_GRAY))))
                        .append(Component.text(" | " + releaseUrl, NamedTextColor.WHITE, TextDecoration.UNDERLINED)
                            .clickEvent(ClickEvent.openUrl(releaseUrl))
                            .hoverEvent(HoverEvent.showText(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_TO_OPEN)), SOFT_GRAY))))
                        .build()
                ));
            }
        } else {
            body.add(DialogBodyEntry.text(Component.empty()));
            body.add(DialogBodyEntry.text(
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__UPDATE__CHECK_HINT)), TextColor.color(0x888888))
            ));
        }

        List<DialogButton> buttons = new ArrayList<>();
        buttons.add(new DialogButton("check",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__UPDATE__CHECK_NOW)), NamedTextColor.WHITE),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__UPDATE__DOWNLOAD)), TextColor.color(0x888888)),
            p -> {
                UpdateChecker checker = new UpdateChecker(pluginVersion, true, () ->
                    BlockProt.getFoliaLib().getScheduler().runAtEntity(p, syncTask -> {
                        if (p.isOnline()) UpdateDialog.show(p, backOrigin);
                    }));
                BlockProt.getFoliaLib().getScheduler().runAsync(asyncTask -> checker.run());
            }
        ));

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            originHint(exitOrigin),
            originBack(player, exitOrigin)
        );

        bridge.showMultiAction(player, title, body, buttons, backBtn, 2);
    }

    private static Component kindLabel(@NotNull SemanticVersion version) {
        String base = version.baseVersion();
        if (version.isHotfix()) {
            return Component.text(
                stripColor(Translator.get(TranslationKey.DIALOGS__UPDATE__KIND_HOTFIX))
                    .replace("{version}", version.toString())
                    .replace("{base_version}", base),
                PASTEL_CORAL, TextDecoration.BOLD);
        }
        if (version.isPreRelease()) {
            return Component.text(
                stripColor(Translator.get(TranslationKey.DIALOGS__UPDATE__KIND_DEV))
                    .replace("{version}", version.toString()),
                PASTEL_GOLD, TextDecoration.BOLD);
        }
        return Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__UPDATE__KIND_STABLE))
                .replace("{version}", version.toString()),
            PASTEL_MINT, TextDecoration.BOLD);
    }

    private static Component originHint(DialogOrigin origin) {
        switch (origin) {
            case ADMIN_MENU: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_ADMIN_MENU)), TextColor.color(0x888888));
            default: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888));
        }
    }

    private static DialogButton.DialogClickHandler originBack(Player player, DialogOrigin origin) {
        switch (origin) {
            case ADMIN_MENU: return p -> AdminMenuDialog.show(p);
            default: return null;
        }
    }
}
