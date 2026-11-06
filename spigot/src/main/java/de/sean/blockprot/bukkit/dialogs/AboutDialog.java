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
import de.sean.blockprot.bukkit.nbt.EntityNBTHandler;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_CORAL;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_GOLD;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_MINT;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_PURPLE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_BLUE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_GRAY;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class AboutDialog {

    private AboutDialog() {}

    public static void show(@NotNull Player player) {
        show(player, DialogOrigin.USER_MENU);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        String version = BlockProt.getPluginVersion();
        String serverVersion = Bukkit.getVersion();
        String bukkitVersion = Bukkit.getBukkitVersion();
        String javaVersion = System.getProperty("java.version");

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__TITLE)),
            PASTEL_MINT, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();

        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__DESCRIPTION)), SOFT_GRAY))
            .build()));
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__MAINTAINER)),
            TextColor.color(0x888888))));
        body.add(DialogBodyEntry.text(Component.empty()));

        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__VERSION_LABEL)), SOFT_GRAY))
            .append(Component.text(version, PASTEL_GOLD, TextDecoration.BOLD))
            .build()));
        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__SERVER_LABEL)), SOFT_GRAY))
            .append(Component.text(serverVersion, TextColor.color(0x888888)))
            .build()));
        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__API_LABEL)), SOFT_GRAY))
            .append(Component.text(bukkitVersion, TextColor.color(0x888888)))
            .build()));
        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__JAVA_LABEL)), SOFT_GRAY))
            .append(Component.text(javaVersion, TextColor.color(0x888888)))
            .build()));

        boolean dbActive = BlockProt.getHybridDatabase() != null && BlockProt.getHybridDatabase().isEnabled();
        String dbMode = dbActive
            ? stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__DB_MYSQL))
            : stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__DB_SQLITE));
        int cacheCount = de.sean.blockprot.bukkit.storage.ProtectedBlockCache.size();
        List<String> activeInts = new ArrayList<>();
        List<de.sean.blockprot.bukkit.integrations.PluginIntegration> allInts = BlockProt.getInstance().getIntegrations();
        for (var pi : allInts) {
            if (pi.isEnabled()) activeInts.add(pi.name);
        }
        String intText = activeInts.isEmpty()
            ? Translator.get(TranslationKey.DIALOGS__ABOUT__INTEGRATIONS_DISABLED).replace("{count}", String.valueOf(allInts.size()))
            : Translator.get(TranslationKey.DIALOGS__ABOUT__INTEGRATIONS_ACTIVE).replace("{enabled}", String.valueOf(activeInts.size())).replace("{total}", String.valueOf(allInts.size())).replace("{names}", String.join(", ", activeInts));
        intText = stripColor(intText);

        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__DB_ENGINE_LABEL)), SOFT_GRAY))
            .append(Component.text(dbMode, PASTEL_MINT))
            .build()));
        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__CACHE_LABEL)), SOFT_GRAY))
            .append(Component.text(cacheCount, PASTEL_GOLD))
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__CACHE_SUFFIX)), PASTEL_GOLD))
            .build()));
        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__INTEGRATIONS_LABEL)), SOFT_GRAY))
            .append(Component.text(intText, activeInts.isEmpty() ? PASTEL_CORAL : PASTEL_MINT))
            .build()));

        body.add(DialogBodyEntry.text(Component.empty()));

        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__REPORT_ISSUES)), SOFT_GRAY))
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__ISSUES_URL)),
                SOFT_BLUE, TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl("https://github.com/VictorGugug/BlockProt-Reloaded/issues"))
                .hoverEvent(HoverEvent.showText(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_TO_OPEN)), SOFT_GRAY))))
            .build()));

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton backBtn = DialogNavigation.backButton(
            exitOrigin,
            exitOrigin == DialogOrigin.USER_MENU ? p -> UserMenuDialog.show(p)
                : exitOrigin == DialogOrigin.ADMIN_MENU ? p -> AdminMenuDialog.show(p)
                : null,
            exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__RETURN_PREVIOUS : null
        );

        bridge.showMultiAction(player, title, body, List.of(backBtn), null, 1);
    }

    }
