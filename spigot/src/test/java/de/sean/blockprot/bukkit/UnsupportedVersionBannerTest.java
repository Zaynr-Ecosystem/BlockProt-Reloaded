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

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnsupportedVersionBannerTest {

    @BeforeEach
    void loadTranslations() {
        Translator.resetTranslations();
        InputStream stream = getClass().getResourceAsStream("/lang/translations_en.yml");
        assertNotNull(stream);
        YamlConfiguration en = YamlConfiguration.loadConfiguration(
            new InputStreamReader(stream, StandardCharsets.UTF_8));
        Translator.loadFromConfigs(en, en);
    }

    @AfterEach
    void resetTranslations() {
        Translator.resetTranslations();
    }

    @Test
    void bprOn1214RecommendsBprLegacyWithDownloadLink() {
        List<Edition.BannerLine> lines = Edition.BPR.unsupportedBannerLines("1.4.5", 1, 21, 4, "1.21.4");
        String joined = join(lines);
        assertFalse(Edition.BPR.supports(1, 21, 4));
        assertTrue(joined.contains("BlockProt Reloaded"));
        assertTrue(joined.contains("1.21.4"));
        assertTrue(joined.contains("BlockProt Reloaded Legacy"));
        assertTrue(joined.contains("1.20.5 - 1.21.6"));
        assertTrue(joined.contains("https://modrinth.com/plugin/blockprot-reloaded-legacy"));
        assertTrue(joined.contains("VERSION_GUIDE.md"));
        assertFalse(joined.contains("{"));

        String summary = Edition.BPR.unsupportedSummary("1.4.5", 1, 21, 4, "1.21.4");
        assertTrue(summary.contains("BlockProt Reloaded Legacy"));
        assertTrue(summary.contains("https://modrinth.com/plugin/blockprot-reloaded-legacy"));
    }

    @Test
    void bprOn1201RecommendsBpLegacyWithDownloadLink() {
        String joined = join(Edition.BPR.unsupportedBannerLines("1.4.5", 1, 20, 1, "1.20.1"));
        assertTrue(joined.contains("BlockProt Legacy"));
        assertTrue(joined.contains("1.18.2 - 1.20.4"));
        assertTrue(joined.contains("https://modrinth.com/plugin/blockprot-legacy"));
        assertFalse(joined.contains("{"));
    }

    @Test
    void bprOn1171ShowsNoEditionSupportedAndGuideLink() {
        List<Edition.BannerLine> lines = Edition.BPR.unsupportedBannerLines("1.4.5", 1, 17, 1, "1.17.1");
        String joined = join(lines);
        assertTrue(joined.contains("No BlockProt edition supports Minecraft 1.17.1"));
        assertTrue(joined.contains("VERSION_GUIDE.md"));
        assertFalse(joined.contains("https://modrinth.com/plugin/"));
        assertFalse(joined.contains("{"));

        String summary = Edition.BPR.unsupportedSummary("1.4.5", 1, 17, 1, "1.17.1");
        assertTrue(summary.contains("No BlockProt edition supports Minecraft 1.17.1"));
    }

    @Test
    void bprLegacyOutOfRangeBothDirectionsRecommendsCorrectEditions() {
        assertFalse(Edition.BPR_LEGACY.supports(1, 20, 4));
        String older = join(Edition.BPR_LEGACY.unsupportedBannerLines("1.4.0.2", 1, 20, 4, "1.20.4"));
        assertTrue(older.contains("BlockProt Legacy"));
        assertTrue(older.contains("https://modrinth.com/plugin/blockprot-legacy"));
        assertFalse(older.contains("{"));

        assertFalse(Edition.BPR_LEGACY.supports(26, 3, 0));
        assertTrue(Edition.BPR_LEGACY.isNewerThanRange(26, 3, 0));
        String newer = join(Edition.BPR_LEGACY.unsupportedBannerLines("1.4.0.2", 26, 3, 0, "26.3"));
        assertTrue(newer.contains("BlockProt Reloaded"));
        assertTrue(newer.contains("1.21.7+"));
        assertTrue(newer.contains("https://modrinth.com/plugin/blockprot-reloaded"));
        assertFalse(newer.contains("{"));

        String newerSummary = Edition.BPR_LEGACY.unsupportedSummary("1.4.0.2", 26, 3, 0, "26.3");
        assertTrue(newerSummary.contains("BlockProt Reloaded"));
        assertTrue(newerSummary.contains("https://modrinth.com/plugin/blockprot-reloaded"));
    }

    @Test
    void bpLegacyOutOfRangeAboveRecommendsBprLegacyOrBpr() {
        assertFalse(Edition.BP_LEGACY.supports(1, 21, 4));
        assertTrue(Edition.BP_LEGACY.isNewerThanRange(1, 21, 4));
        String mid = join(Edition.BP_LEGACY.unsupportedBannerLines("1.4.0.2", 1, 21, 4, "1.21.4"));
        assertTrue(mid.contains("BlockProt Reloaded Legacy"));
        assertTrue(mid.contains("https://modrinth.com/plugin/blockprot-reloaded-legacy"));
        assertFalse(mid.contains("{"));

        assertFalse(Edition.BP_LEGACY.supports(26, 3, 0));
        String latest = join(Edition.BP_LEGACY.unsupportedBannerLines("1.4.0.2", 26, 3, 0, "26.3"));
        assertTrue(latest.contains("BlockProt Reloaded"));
        assertTrue(latest.contains("https://modrinth.com/plugin/blockprot-reloaded"));
        assertFalse(latest.contains("{"));
    }

    private static String join(List<Edition.BannerLine> lines) {
        return lines.stream().map(Edition.BannerLine::text).collect(Collectors.joining("\n"));
    }
}
