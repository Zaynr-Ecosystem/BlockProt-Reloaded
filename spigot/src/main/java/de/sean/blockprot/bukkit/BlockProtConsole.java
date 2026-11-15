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

package de.sean.blockprot.bukkit;

import de.sean.blockprot.bukkit.util.ComponentMessages;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class BlockProtConsole {

    /**
     * Manual identity prefix used on every line sent through {@link Bukkit#getConsoleSender()}.
     * Emitting through the console sender (instead of {@link Logger}) bypasses the server's
     * log4j {@code TerminalConsoleAppender}, whose Minecraft-color-to-ANSI translation only
     * fires when a real terminal is detected. Wrappers that pipe stdout without a terminal
     * (e.g. GSM on Windows) never get that translation and print the raw section-sign codes
     * or mojibake instead. Bukkit's console sender does its own terminal-aware color
     * downsampling and works consistently across CMD, GSM, Linux PTY/Bash, and hosting panels.
     * This prefix replaces the {@code [HH:mm:ss INFO]: [BlockProt Reloaded]} line the
     * standard logger would have added.
     */
    public static final String PASTEL_MINT    = hex("8FE3B0");
    public static final String PASTEL_CORAL   = hex("F0A0A0");
    public static final String PASTEL_GOLD    = hex("D2B48C");
    public static final String SOFT_BLUE      = hex("A0C4E8");
    public static final String PASTEL_PURPLE  = hex("C8A0E0");
    public static final String PASTEL_COFFEE  = hex("D8B994");
    public static final String PASTEL_ORANGE  = hex("FFB703");
    public static final String BRIGHT_RED     = hex("FF3B3B");
    public static final String BOLD_RED       = BRIGHT_RED + "§l";
    public static final String PASTEL_GRAY    = "§7";
    public static final String CONNECTOR_GRAY = "§8";

    private static final String BANNER_CYAN = PASTEL_MINT;
    private static final String ACCENT_MINT = PASTEL_MINT;
    private static final String LABEL_GRAY = "§7";
    private static final String INACTIVE_GRAY = "§8";

    private static String prefix() {
        return "§8[§r" + Edition.current().displayName() + "§8] §r";
    }

    private static final String WARN_TAG = "§e[WARN]§r ";
    private static final String ERROR_TAG = "§c[ERROR]§r ";

    /** Converts a 6-digit hex string to the extended section-sign hex format Adventure's legacy serializer accepts. */
    private static String hex(@NotNull String rgb) {
        StringBuilder sb = new StringBuilder("§x");
        for (char c : rgb.toCharArray()) {
            sb.append('§').append(c);
        }
        return sb.toString();
    }

    @Nullable
    private static List<StartupLine> startupBuffer = null;

    @Nullable
    private static List<String> guideBuffer = null;

    @Nullable
    private static Logger pluginLogger = null;

    private record StartupLine(String message, boolean isWarning) {}

    private BlockProtConsole() {}

    public static void beginStartup(@NotNull Logger logger) {
        pluginLogger = logger;
        startupBuffer = new ArrayList<>();
        guideBuffer = new ArrayList<>();
    }

    /**
     * Buffers a first-start guide line so it prints directly under the banner,
     * before the boot checklist. Only relevant during {@link #beginStartup}.
     */
    public static void guide(@NotNull String message) {
        if (guideBuffer != null) {
            guideBuffer.add(message);
        } else if (pluginLogger != null) {
            raw(prefix() + message);
        }
    }

    public static void printStartupBanner(@NotNull String version) {
        List<StartupLine> lines = startupBuffer != null ? startupBuffer : new ArrayList<>();
        List<String> guideLines = guideBuffer != null ? guideBuffer : new ArrayList<>();
        startupBuffer = null;
        guideBuffer = null;
        if (pluginLogger == null) return;

        raw("");
        raw(PASTEL_COFFEE + "  ██████╗ ██████╗  ██████╗ ");
        raw(PASTEL_COFFEE + "  ██╔══██╗██╔══██╗██╔══██╗");
        raw(PASTEL_COFFEE + "  ██████╔╝██████╔╝██████╔╝");
        raw(PASTEL_COFFEE + "  ██╔══██╗██╔═══╝ ██╔══██╗");
        raw(PASTEL_COFFEE + "  ██████╔╝██║     ██║  ██║");
        raw(PASTEL_COFFEE + "  ╚═════╝ ╚═╝     ╚═╝  ╚═╝");
        raw("§r        " + PASTEL_MINT + Edition.current().displayName());
        raw("§r            " + PASTEL_GOLD + "v" + version);
        raw("§r  " + PASTEL_GOLD + Edition.current().aboutLine(version));

        for (String guideLine : guideLines) {
            raw(guideLine);
        }

        if (lines.isEmpty()) {
            raw(prefix() + "  No startup messages.");
            raw("");
            return;
        }

        boolean previousWasWarning = false;
        boolean separatorPrinted = false;
        for (StartupLine line : lines) {
            if (line.isWarning() && !previousWasWarning && !separatorPrinted) {
                raw("");
                separatorPrinted = true;
            }
            String tag = line.isWarning() ? WARN_TAG : "";
            raw(prefix() + tag + line.message());
            previousWasWarning = line.isWarning();
        }
        raw("");
    }

    public static void printUnsupportedEditionBanner(
            @NotNull Edition edition,
            @NotNull String pluginVersion,
            int major, int minor, int patch,
            @NotNull String mcVersion,
            @NotNull String summary,
            @NotNull Logger logger) {
        raw("");
        for (Edition.BannerLine line : edition.unsupportedBannerLines(pluginVersion, major, minor, patch, mcVersion)) {
            String color = switch (line.style()) {
                case BORDER, TITLE, HIGHLIGHT, URL -> BOLD_RED;
                case INFO, STATUS -> BRIGHT_RED;
            };
            raw(color + "  " + line.text());
        }
        raw("");
        logger.severe(summary);
    }

    /**
     * Sends a line straight through {@link Bukkit#getConsoleSender()} with no
     * additional prefixing, used for pre-formatted lines (the banner art, and
     * lines that already carry {@link #prefix()} themselves).
     */
    private static void raw(@NotNull String message) {
        ComponentMessages.sendLegacy(Bukkit.getConsoleSender(), message);
        BlockProtLogger.log(message);
    }

    public static String padRight(@NotNull String s, int n) {
        if (s.length() >= n) return s;
        return s + " ".repeat(n - s.length());
    }

    private static void bootLine(@NotNull String label, @NotNull String status, @NotNull String statusColor, boolean isLast) {
        String connector = isLast ? "└─" : "├─";
        String paddedLabel = padRight(label, 16);
        info("  " + CONNECTOR_GRAY + connector + " " + LABEL_GRAY + paddedLabel + "  " + statusColor + status);
    }

    public static void bootStatus(@NotNull String label, boolean active, @NotNull String status, boolean isLast) {
        String connector = isLast ? "└─" : "├─";
        String dot = active ? PASTEL_MINT + "● " : PASTEL_ORANGE + "● ";
        String statusColor = active ? PASTEL_MINT : PASTEL_ORANGE;
        String paddedLabel = padRight(label, 16);
        info("  " + CONNECTOR_GRAY + connector + " " + LABEL_GRAY + paddedLabel + "  " + dot + statusColor + status);
    }

    public static void bootStatus(@NotNull String label, boolean active, @NotNull String status) {
        bootStatus(label, active, status, false);
    }

    public static void boot(@NotNull String label, @NotNull String status) {
        bootStatus(label, true, status, false);
    }

    public static void bootLast(@NotNull String label, @NotNull String status) {
        bootLine(label, status, ACCENT_MINT, true);
    }

    /** Same as {@link #boot(String, String)} but for informational, non-active statuses (e.g. an integration that is not installed). */
    public static void bootMuted(@NotNull String label, @NotNull String status) {
        bootStatus(label, false, status, false);
    }

    /**
     * Prints one boot checklist line with an explicit status color (used by
     * the update check, whose green/red/orange states do not map to the
     * active/inactive pair of {@link #bootStatus}).
     */
    public static void bootColored(@NotNull String label, @NotNull String status, @NotNull String statusColor) {
        String paddedLabel = padRight(label, 16);
        info("  " + CONNECTOR_GRAY + "├─" + " " + LABEL_GRAY + paddedLabel + "  " + statusColor + "● " + statusColor + status);
    }

    /** Prints an indented continuation line under a boot checklist line. */
    public static void bootIndented(@NotNull String message, @NotNull String color) {
        info("  " + CONNECTOR_GRAY + "│   " + color + message);
    }

    public static void info(@NotNull String message) {
        emit(message.startsWith("  ") ? message : "  " + message);
    }

    public static void success(@NotNull String message) {
        emit(message.startsWith("  ") ? message : "  " + message);
    }

    public static void warn(@NotNull String message) {
        String prefixed = message.startsWith("  ") ? message : "  " + message;
        if (startupBuffer != null) {
            startupBuffer.add(new StartupLine(prefixed, true));
        } else {
            raw(prefix() + WARN_TAG + prefixed);
        }
    }

    public static void error(@NotNull String message) {
        String prefixed = message.startsWith("  ") ? message : "  " + message;
        if (startupBuffer != null) {
            startupBuffer.add(new StartupLine(prefixed, true));
        } else {
            raw(prefix() + ERROR_TAG + prefixed);
        }
    }

    public static void integrationEnabled(@NotNull String integrationName) {
        emit("  " + integrationName + " hooked");
    }

    private static void emit(@NotNull String message) {
        if (startupBuffer != null) {
            startupBuffer.add(new StartupLine(message, false));
        } else {
            raw(prefix() + message);
        }
    }
}