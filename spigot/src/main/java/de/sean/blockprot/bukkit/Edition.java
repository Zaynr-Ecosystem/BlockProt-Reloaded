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

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

public enum Edition {
    BP_LEGACY("bp-legacy", "BlockProt Legacy", "BP Legacy", "bpl-",
        new int[] {1, 18, 2}, new int[] {1, 20, 4},
        "https://modrinth.com/plugin/blockprot-legacy", 1),
    BPR_LEGACY("bpr-legacy", "BlockProt Reloaded Legacy", "BPR Legacy", "bprl-",
        new int[] {1, 20, 5}, new int[] {1, 21, 6},
        "https://modrinth.com/plugin/blockprot-reloaded-legacy", 2),
    BPR("bpr", "BlockProt Reloaded", "BPR", "",
        new int[] {1, 21, 7}, null,
        "https://modrinth.com/plugin/blockprot-reloaded", 3);

    private static final String GUIDE_URL =
        "https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/blob/main/docs/READ_MEs/VERSION_GUIDE.md";

    private static Edition current;

    private final String id;
    private final String displayName;
    private final String shortName;
    private final String tagPrefix;
    private final int[] min;
    private final int[] max;
    private final String downloadUrl;
    private final int rank;

    Edition(String id, String displayName, String shortName, String tagPrefix,
            int[] min, int[] max, String downloadUrl, int rank) {
        this.id = id;
        this.displayName = displayName;
        this.shortName = shortName;
        this.tagPrefix = tagPrefix;
        this.min = min;
        this.max = max;
        this.downloadUrl = downloadUrl;
        this.rank = rank;
    }

    public @NotNull String id() { return id; }
    public @NotNull String displayName() { return displayName; }
    public @NotNull String shortName() { return shortName; }
    public @NotNull String tagPrefix() { return tagPrefix; }
    public @NotNull String downloadUrl() { return downloadUrl; }
    public int rank() { return rank; }

    public static @NotNull String guideUrl() {
        return GUIDE_URL;
    }

    public static synchronized @NotNull Edition current() {
        if (current == null) {
            current = fromId(readBundledId());
        }
        return current;
    }

    static @NotNull Edition fromId(String id) {
        for (Edition edition : values()) {
            if (edition.id.equals(id)) return edition;
        }
        return BPR;
    }

    private static String readBundledId() {
        try (InputStream in = Edition.class.getResourceAsStream("/edition.properties")) {
            if (in == null) return null;
            Properties properties = new Properties();
            properties.load(in);
            return properties.getProperty("id");
        } catch (IOException e) {
            return null;
        }
    }

    public boolean supports(int major, int minor, int patch) {
        return compare(major, minor, patch, min) >= 0 && !isNewerThanRange(major, minor, patch);
    }

    public boolean isNewerThanRange(int major, int minor, int patch) {
        return max != null && compare(major, minor, patch, max) > 0;
    }

    public static @NotNull Optional<Edition> recommendedFor(int major, int minor, int patch) {
        for (Edition edition : values()) {
            if (edition.supports(major, minor, patch)) return Optional.of(edition);
        }
        return Optional.empty();
    }

    public @NotNull String rangeLabel() {
        String from = version(min);
        return max == null ? from + "+" : from + " - " + version(max);
    }

    public static boolean isEditionTag(@NotNull String tag) {
        for (Edition edition : values()) {
            if (!edition.tagPrefix.isEmpty() && tag.startsWith(edition.tagPrefix)) return true;
        }
        return false;
    }

    public @NotNull String aboutLine(@NotNull String pluginVersion) {
        return Translator.get(TranslationKey.CONSOLE__ABOUT_EDITION)
            .replace("{edition}", displayName)
            .replace("{version}", pluginVersion)
            .replace("{range}", rangeLabel());
    }

    public boolean supportsDialogs() {
        return this == BPR;
    }

    private static int compare(int major, int minor, int patch, int[] bound) {
        if (major != bound[0]) return Integer.compare(major, bound[0]);
        if (minor != bound[1]) return Integer.compare(minor, bound[1]);
        return Integer.compare(patch, bound[2]);
    }

    private static String version(int[] parts) {
        return parts[0] + "." + parts[1] + "." + parts[2];
    }
}
