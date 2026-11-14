/*
 * Copyright (C) 2026 Zar (VictorGugug)
 * This file is part of BlockProt Reloaded <https://github.com/VictorGugug/BlockProt-Reloaded>.
 *
 * BlockProt Reloaded is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BlockProt Reloaded is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlockProt Reloaded.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.sean.blockprot.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class ReleaseNotesFormatter {

    private static final Pattern HTML_COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[([^\\]]+)]\\(([^)]+)\\)");

    private ReleaseNotesFormatter() {
    }

    public static @NotNull List<String> formatSummaryLines(@Nullable String rawMarkdown,
                                                             int maxLines,
                                                             int maxLineLength) {
        if (rawMarkdown == null || maxLines <= 0 || maxLineLength <= 3) {
            return Collections.emptyList();
        }
        String normalized = rawMarkdown.replace("\r\n", "\n").replace('\r', '\n');
        normalized = HTML_COMMENT.matcher(normalized).replaceAll("");
        normalized = HTML_TAG.matcher(normalized).replaceAll("");
        normalized = MARKDOWN_LINK.matcher(normalized).replaceAll("$1");

        String[] rawLines = normalized.split("\n");
        int startIndex = findHighlightsIndex(rawLines);
        List<String> result = new ArrayList<>();
        for (int i = startIndex; i < rawLines.length; i++) {
            String cleaned = cleanLine(rawLines[i]);
            if (cleaned.isEmpty()) continue;
            result.add(truncate(cleaned, maxLineLength));
            if (result.size() >= maxLines) break;
        }
        return result;
    }

    private static int findHighlightsIndex(@NotNull String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String lower = lines[i].trim().toLowerCase(Locale.ROOT);
            if ("## highlights".equals(lower) || "### highlights".equals(lower)) {
                return i;
            }
        }
        return 0;
    }

    private static @NotNull String cleanLine(@NotNull String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || "---".equals(trimmed) || "***".equals(trimmed) || "___".equals(trimmed)) {
            return "";
        }
        while (trimmed.startsWith("#")) {
            trimmed = trimmed.substring(1).trim();
        }
        boolean isBullet = false;
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            isBullet = true;
            trimmed = trimmed.substring(2).trim();
        }
        trimmed = trimmed.replace("**", "").replace("__", "").replace("`", "").trim();
        if (trimmed.isEmpty()) return "";
        return isBullet ? "- " + trimmed : trimmed;
    }

    private static @NotNull String truncate(@NotNull String line, int maxLength) {
        if (line.length() <= maxLength) return line;
        int limit = maxLength - 3;
        int lastSpace = line.lastIndexOf(' ', limit);
        int cut = lastSpace > limit / 2 ? lastSpace : limit;
        return line.substring(0, cut).trim() + "...";
    }
}
