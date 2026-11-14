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

package de.sean.blockprot.bukkit.tasks;

import de.sean.blockprot.bukkit.Edition;
import de.sean.blockprot.util.SemanticVersion;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckerSelectionTest {

    private static UpdateChecker.GitHubRelease release(String tag, boolean prerelease) {
        UpdateChecker.GitHubRelease release = new UpdateChecker.GitHubRelease();
        release.tagName = tag;
        release.prerelease = prerelease;
        return release;
    }

    private static List<UpdateChecker.GitHubRelease> all() {
        List<UpdateChecker.GitHubRelease> list = new ArrayList<>();
        list.add(release("1.3.9", false));
        list.add(release("1.4.0", false));
        list.add(release("bprl-1.3.9.1", false));
        list.add(release("bprl-1.3.9.2", false));
        list.add(release("bprl-1.3.9.2-BEDev.1", true));
        list.add(release("bpl-1.3.9.1", false));
        return list;
    }

    private static String pick(Edition edition, String running) {
        UpdateChecker.GitHubRelease best = UpdateChecker.selectBest(all(), new SemanticVersion(running), edition);
        return best == null ? null : best.tagName;
    }

    @Test
    void mainEditionIgnoresEditionTags() {
        List<UpdateChecker.GitHubRelease> releases = new ArrayList<>();
        releases.add(release("1.3.9", false));
        releases.add(release("bprl-1.3.9.1", false));
        releases.add(release("bpl-1.3.9.1", false));
        assertEquals("1.3.9", UpdateChecker.selectBest(releases, new SemanticVersion("1.3.8"), Edition.BPR).tagName);
    }

    @Test
    void mainEditionNeverPicksAnEditionTagWhenNothingElseExists() {
        List<UpdateChecker.GitHubRelease> releases = new ArrayList<>();
        releases.add(release("bprl-1.3.9.1", false));
        assertNull(UpdateChecker.selectBest(releases, new SemanticVersion("1.3.8"), Edition.BPR));
    }

    @Test
    void legacyEditionSeesOnlyItsOwnPrefixAndIgnoresMain() {
        assertEquals("bprl-1.3.9.2", pick(Edition.BPR_LEGACY, "1.3.9.1"));
        assertEquals("bpl-1.3.9.1", pick(Edition.BP_LEGACY, "1.3.9.1-BEDev.1"));
    }

    @Test
    void preReleaseServerSeesPreReleaseBuildsOfItsEdition() {
        List<UpdateChecker.GitHubRelease> releases = new ArrayList<>();
        releases.add(release("bprl-1.3.9.2-BEDev.1", true));
        assertEquals("bprl-1.3.9.2-BEDev.1",
            UpdateChecker.selectBest(releases, new SemanticVersion("1.3.9.1-BEDev.1"), Edition.BPR_LEGACY).tagName);
    }

    @Test
    void stableServerDoesNotSeePreReleaseBuilds() {
        List<UpdateChecker.GitHubRelease> releases = new ArrayList<>();
        releases.add(release("bprl-1.3.9.2-BEDev.1", true));
        assertNull(UpdateChecker.selectBest(releases, new SemanticVersion("1.3.9.1"), Edition.BPR_LEGACY));
    }

    @Test
    void fourPartVersionsWithSuffixOrderCorrectly() {
        SemanticVersion dev = new SemanticVersion("1.3.9.1-BEDev.1");
        SemanticVersion stable = new SemanticVersion("1.3.9.1");
        SemanticVersion next = new SemanticVersion("1.3.9.2");
        assertTrue(dev.compareTo(stable) < 0);
        assertTrue(stable.compareTo(next) < 0);
    }

    @Test
    void draftsAreSkipped() {
        UpdateChecker.GitHubRelease draft = release("1.3.9", false);
        draft.draft = true;
        List<UpdateChecker.GitHubRelease> releases = new ArrayList<>();
        releases.add(draft);
        assertNull(UpdateChecker.selectBest(releases, new SemanticVersion("1.3.8"), Edition.BPR));
    }

    @Test
    void hotfixTaggedAsPrereleaseOnGitHubIsOfferedToStableServer() {
        List<UpdateChecker.GitHubRelease> releases = new ArrayList<>();
        releases.add(release("1.4.2-hotfix", true));
        assertEquals("1.4.2-hotfix", UpdateChecker.selectBest(releases, new SemanticVersion("1.4.2"), Edition.BPR).tagName);
    }

    @Test
    void hotfixRunningServerSeesNewerRelease() {
        List<UpdateChecker.GitHubRelease> releases = new ArrayList<>();
        releases.add(release("1.4.2", false));
        assertEquals("1.4.2", UpdateChecker.selectBest(releases, new SemanticVersion("1.4.1-hotfix"), Edition.BPR).tagName);
    }

    @Test
    void stableServerDetectsNextStableOrHotfixIgnoringLegacyAndDevBuilds() {
        List<UpdateChecker.GitHubRelease> releases = new ArrayList<>();
        releases.add(release("bpl-1.4.0.1", false));
        releases.add(release("bprl-1.4.0.1", false));
        releases.add(release("1.4.2", false));
        releases.add(release("1.4.3", false));
        releases.add(release("1.4.4-BEDev.1", true));

        assertEquals("1.4.3", UpdateChecker.selectBest(releases, new SemanticVersion("1.4.2"), Edition.BPR).tagName);
        assertEquals("1.4.3", UpdateChecker.selectBest(releases, new SemanticVersion("1.4.3"), Edition.BPR).tagName);

        releases.add(release("1.4.3-hotfix.1", true));
        assertEquals("1.4.3-hotfix.1", UpdateChecker.selectBest(releases, new SemanticVersion("1.4.3"), Edition.BPR).tagName);

        releases.add(release("1.4.4", false));
        assertEquals("1.4.4", UpdateChecker.selectBest(releases, new SemanticVersion("1.4.3"), Edition.BPR).tagName);
    }
}
