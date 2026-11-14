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

package de.sean.blockprot.bukkit.commands;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtConsole;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.Edition;
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.dialogs.AboutDialog;
import de.sean.blockprot.bukkit.dialogs.DialogOrigin;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Displays full plugin version, maintainer info, runtime state, and diagnostic info.
 */
public class AboutCommand implements CommandExecutor {
    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_MINT = TextColor.color(0x98FF98);
    private static final TextColor PASTEL_GOLD = TextColor.color(0xF0E6A0);
    private static final TextColor SOFT_BLUE = TextColor.color(0x7EC8E3);

    @Override
    public boolean canUseCommand(@NotNull CommandSender sender) {
        return !(sender instanceof Player) || sender.hasPermission(Permissions.USER.key());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player && !canUseCommand(sender)) {
            player.sendMessage(Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
            return true;
        }
        String senderName = (sender instanceof Player p) ? p.getName() : "Console";
        BlockProtLogger.log("command", senderName + " executed /bp about");

        if (sender instanceof Player player && BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
            AboutDialog.show(player, DialogOrigin.NONE);
            return true;
        }

        if (sender instanceof Player player) {
            printPlayerAbout(player);
            return true;
        }

        printConsoleAbout();
        return true;
    }

    /**
     * Prints the player-facing about summary. Used by both the CLI and the
     * inventory menus (which cannot open dialogs).
     */
    public static void showAbout(@NotNull Player player) {
        printPlayerAbout(player);
    }

    private static void printConsoleAbout() {
        String version = BlockProt.getPluginVersion();
        String issuesUrl = "https://github.com/VictorGugug/BlockProt-Reloaded/issues";
        BlockProtConsole.info(Translator.get(TranslationKey.DIALOGS__ABOUT__VERSION_LABEL) + BlockProtConsole.PASTEL_GOLD + version);
        BlockProtConsole.info(Edition.current().aboutLine(version));
        BlockProtConsole.info(Translator.get(TranslationKey.DIALOGS__ABOUT__AUTHOR_LABEL));
        BlockProtConsole.info(Translator.get(TranslationKey.DIALOGS__ABOUT__MAINTAINER_LABEL));
        BlockProtConsole.info(Translator.get(TranslationKey.DIALOGS__ABOUT__REPORT_ISSUES) + " " + BlockProtConsole.SOFT_BLUE + issuesUrl);
        BlockProtLogger.log("about | version=" + version + " | issues=" + issuesUrl);
    }

    private static void printPlayerAbout(Player player) {
        String version = BlockProt.getPluginVersion();
        String issuesUrl = "https://github.com/VictorGugug/BlockProt-Reloaded/issues";
        ComponentMessages.send(player, Component.text()
            .append(Component.text(Translator.get(TranslationKey.DIALOGS__ABOUT__VERSION_LABEL), SOFT_GRAY))
            .append(Component.text(version, PASTEL_GOLD))
            .build());
        ComponentMessages.send(player, Component.text(Edition.current().aboutLine(version), SOFT_GRAY));
        ComponentMessages.send(player, Component.text(Translator.get(TranslationKey.DIALOGS__ABOUT__AUTHOR_LABEL), PASTEL_MINT));
        ComponentMessages.send(player, Component.text(Translator.get(TranslationKey.DIALOGS__ABOUT__MAINTAINER_LABEL), PASTEL_MINT));
        ComponentMessages.send(player, Component.text()
            .append(Component.text(Translator.get(TranslationKey.DIALOGS__ABOUT__REPORT_ISSUES), SOFT_GRAY))
            .append(Component.text(" ", SOFT_GRAY))
            .append(Component.text(issuesUrl, SOFT_BLUE)
                .clickEvent(ClickEvent.openUrl(issuesUrl))
                .hoverEvent(HoverEvent.showText(Component.text(Translator.get(TranslationKey.DIALOGS__CLICK_TO_OPEN).replaceAll("[§&][0-9a-fk-orxA-F]", ""), SOFT_GRAY))))
            .build());
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return null;
    }
}