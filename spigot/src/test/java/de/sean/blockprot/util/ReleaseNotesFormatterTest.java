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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseNotesFormatterTest {

    @Test
    void stripsMarkdownAndHtmlAndLimitsLines() {
        String raw = """
            <!-- internal note -->
            ## What's Changed
            ---
            - **Storage** moved under [data folder](https://example.org) using `state.yml`
            * *Changelog viewer* with **GitHub** link support
            <details><summary>Details</summary></details>
            - Third line
            - Fourth line
            """;

        List<String> lines = ReleaseNotesFormatter.formatSummaryLines(raw, 3, 60);

        assertEquals(3, lines.size());
        assertEquals("What's Changed", lines.get(0));
        assertTrue(lines.get(1).startsWith("- Storage moved under data folder using state.yml"));
        assertTrue(lines.get(2).startsWith("- *Changelog viewer*"));
    }

    @Test
    void prioritizesHighlightsSectionOverLongIntroParagraph() {
        String raw = """
            # BlockProt Reloaded 1.4.4 Release Notes

            Welcome to BlockProt Reloaded 1.4.4. This intro paragraph should not consume the preview lines.

            ## Highlights

            - **Clean Data Layout**: Internal SQLite caches moved into data/.
            - **Changelog Viewer**: Admins can read release notes in-game or open GitHub.
            """;

        List<String> lines = ReleaseNotesFormatter.formatSummaryLines(raw, 3, 80);

        assertEquals(3, lines.size());
        assertEquals("Highlights", lines.get(0));
        assertTrue(lines.get(1).startsWith("- Clean Data Layout:"));
        assertTrue(lines.get(2).startsWith("- Changelog Viewer:"));
    }

    @Test
    void handlesNullAndTruncatesLongLinesGracefully() {
        assertTrue(ReleaseNotesFormatter.formatSummaryLines(null, 5, 50).isEmpty());

        List<String> truncated = ReleaseNotesFormatter.formatSummaryLines(
            "- Added a very long sentence describing plugin configuration telemetry and metrics cleanup",
            2,
            40
        );
        assertEquals(1, truncated.size());
        assertTrue(truncated.get(0).length() <= 40);
        assertTrue(truncated.get(0).endsWith("..."));
    }
}
