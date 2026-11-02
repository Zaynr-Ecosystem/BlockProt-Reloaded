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

import com.google.gson.annotations.SerializedName;
import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.Edition;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import de.sean.blockprot.util.SemanticVersion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Checks for new plugin releases on GitHub and notifies ops/admins.
 *
 * <p>Release channels are derived from the version suffix (see
 * {@link SemanticVersion}): clean releases ("1.3.4") and post-release
 * corrections ("1.3.4-hotfix", "1.3.4-fix.1") are the stable channel;
 * BEDev/snapshot builds are the experimental pre-release channel. Servers
 * running a stable-channel version only see stable-channel updates, so a
 * hotfix tag is always offered to servers on its base release, while BEDev
 * builds are never offered to stable users. Servers on a pre-release
 * version see everything.
 */
public final class UpdateChecker implements Runnable {

    /**
     * GitHub Releases API endpoint for this fork.
     * {@code /releases/latest} only returns stable releases.
     * {@code /releases} (list) is used instead so pre-releases are also detected.
     */
    private static final String GITHUB_API_URL =
        "https://api.github.com/repos/Zaynr-Ecosystem/BlockProt-Reloaded/releases";

    private static final String RELEASES_URL =
        "https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/releases";

    private static String releaseUrl() {
        return Edition.current().tagPrefix().isEmpty() ? RELEASES_URL + "/latest" : RELEASES_URL;
    }

    /**
     * Cached result of the last successful GitHub API call.
     * Package-accessible so {@link BackupTask} can read the cached value
     * without issuing a redundant HTTP request.
     */
    @Nullable
    public static volatile SemanticVersion latestVersion;

    /**
     * Full cached release that {@link #latestVersion} was parsed from,
     * carrying its tag, name and per-tag release page.
     */
    @Nullable
    public static volatile GitHubRelease latestRelease;

    @Nullable
    private final List<Player> recipients;

    @Nullable
    private final Runnable onComplete;

    @NotNull
    private final String pluginVersion;

    @NotNull
    private final SemanticVersion currentVersion;

    public UpdateChecker(@NotNull final String version) {
        this.pluginVersion = version;
        this.recipients = null;
        this.onComplete = null;
        this.currentVersion = new SemanticVersion(version);
    }

    public UpdateChecker(@NotNull final String version,
                         @Nullable final List<Player> recipients) {
        this.recipients = recipients;
        this.pluginVersion = version;
        this.onComplete = null;
        this.currentVersion = new SemanticVersion(version);
    }

    public UpdateChecker(@NotNull final String version,
                         @Nullable final Runnable onComplete) {
        this.pluginVersion = version;
        this.recipients = null;
        this.onComplete = onComplete;
        this.currentVersion = new SemanticVersion(version);
    }

    @Override
    public void run() {
        if (latestVersion != null) {
            this.sendMessage(currentVersion, latestVersion, latestRelease);
            return;
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_API_URL))
                .header("User-Agent", "BlockProt-Reloaded-UpdateChecker")
                .header("Accept", "application/vnd.github+json")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return;

            // Parse the list of releases and find the highest version
            // compatible with the current build channel (see class javadoc).
            GitHubRelease[] releases = new com.google.gson.Gson().fromJson(
                response.body(), GitHubRelease[].class);

            GitHubRelease bestRelease = selectBest(Arrays.asList(releases), currentVersion, Edition.current());
            if (bestRelease == null) return;
            SemanticVersion best = bestRelease.asSemantic(Edition.current());
            UpdateChecker.latestVersion = best;
            UpdateChecker.latestRelease = bestRelease;
            this.sendMessage(currentVersion, best, bestRelease);

        } catch (Exception ignored) { }
    }

    /**
     * Picks the highest release of {@code edition} the running version should be offered: stable-channel
     * servers only see stable candidates, pre-release servers see everything of their own edition.
     */
    static @Nullable GitHubRelease selectBest(@NotNull List<GitHubRelease> releases,
                                              @NotNull SemanticVersion current,
                                              @NotNull Edition edition) {
        SemanticVersion best = null;
        GitHubRelease bestRelease = null;
        for (GitHubRelease rel : releases) {
            if (rel.draft || !rel.belongsTo(edition)) continue;
            SemanticVersion v = rel.asSemantic(edition);
            if (v.isExperimental()) continue;
            if (!current.isPreRelease() && (rel.prerelease || v.isPreRelease())) continue;
            if (best == null || v.compareTo(best) > 0) {
                best = v;
                bestRelease = rel;
            }
        }
        return bestRelease;
    }

    private void sendMessage(SemanticVersion currentVersion, SemanticVersion latestVersion,
                             @Nullable GitHubRelease release) {
        boolean isOutdated = latestVersion.compareTo(currentVersion) > 0;
        String releaseUrl = release != null && release.htmlUrl != null && !release.htmlUrl.isBlank()
            ? release.htmlUrl
            : releaseUrl();

        if (this.recipients != null && !this.recipients.isEmpty()) {
            String message;
            if (isOutdated) {
                message = Translator.get(outdatedMessageKey(latestVersion))
                    .replace("{version}", latestVersion.toString())
                    .replace("{base_version}", latestVersion.baseVersion());
            } else if (latestVersion.compareTo(currentVersion) < 0) {
                message = currentVersion.isHotfix()
                    ? Translator.get(TranslationKey.MESSAGES__UPDATE__AHEAD_HOTFIX)
                        .replace("{version}", currentVersion.toString())
                        .replace("{base_version}", currentVersion.baseVersion())
                    : Translator.get(TranslationKey.MESSAGES__UPDATE__AHEAD)
                        .replace("{version}", latestVersion.toString());
            } else {
                message = Translator.get(TranslationKey.MESSAGES__UPDATE__UP_TO_DATE);
            }
            var comp = Component.text(message);
            if (isOutdated) {
                comp = comp.color(latestVersion.isHotfix()
                        ? TextColor.color(0xF08080)
                        : TextColor.color(0xF0E6A0))
                    .append(Component.text(" | " + Translator.get(TranslationKey.DIALOGS__UPDATE__DOWNLOAD), TextColor.color(0xF0E6A0))
                        .append(Component.text(releaseUrl, NamedTextColor.WHITE, TextDecoration.UNDERLINED)))
                    .clickEvent(ClickEvent.openUrl(releaseUrl))
                    .hoverEvent(HoverEvent.showText(
                        Component.text(Translator.get(TranslationKey.MESSAGES__UPDATE__CLICK_HINT))));
            }
            for (Player player : recipients) {
                ComponentMessages.send(player, comp);
            }
            if (onComplete != null) {
                onComplete.run();
            }
        } else if (onComplete != null) {
            onComplete.run();
        } else {
            if (isOutdated) {
                TranslationKey key = consoleOutdatedKey(latestVersion);
                BlockProt.getInstance().getLogger().warning(
                    Translator.get(key)
                        .replace("{version}", latestVersion.toString())
                        .replace("{base_version}", latestVersion.baseVersion())
                    + " | " + releaseUrl);
            } else if (latestVersion.compareTo(currentVersion) < 0) {
                String aheadMessage = currentVersion.isHotfix()
                    ? Translator.get(TranslationKey.MESSAGES__UPDATE__AHEAD_HOTFIX)
                        .replace("{version}", currentVersion.toString())
                        .replace("{base_version}", currentVersion.baseVersion())
                    : Translator.get(TranslationKey.MESSAGES__UPDATE__AHEAD)
                        .replace("{version}", latestVersion.toString());
                BlockProtLogger.log("update-checker", aheadMessage);
            } else {
                BlockProtLogger.log("update-checker",
                    Translator.get(TranslationKey.CONSOLE__UPDATE__UP_TO_DATE));
            }
        }
    }

    /**
     * Message template for an available update, chosen by the release channel:
     * hotfixes (important bug-fix) and experimental pre-releases (BEDev builds)
     * get their own wording, everything else uses the standard outdated message.
     */
    @Contract("_ -> !null")
    private static @NotNull TranslationKey outdatedMessageKey(@NotNull SemanticVersion version) {
        if (version.isHotfix()) return TranslationKey.MESSAGES__UPDATE__OUTDATED_HOTFIX;
        if (version.isPreRelease()) return TranslationKey.MESSAGES__UPDATE__OUTDATED_DEV;
        return TranslationKey.MESSAGES__UPDATE__OUTDATED;
    }

    /**
     * Console counterpart of {@link #outdatedMessageKey}: the same channel
     * split so a BEDev candidate offered to a pre-release server is announced
     * as an experimental build, not as a "stable" version.
     */
    @Contract("_ -> !null")
    private static @NotNull TranslationKey consoleOutdatedKey(@NotNull SemanticVersion version) {
        if (version.isHotfix()) return TranslationKey.CONSOLE__UPDATE__AVAILABLE_HOTFIX;
        if (version.isPreRelease()) return TranslationKey.CONSOLE__UPDATE__AVAILABLE_DEV;
        return TranslationKey.CONSOLE__UPDATE__AVAILABLE;
    }

    /**
     * Represents one entry from {@code GET /repos/{owner}/{repo}/releases}.
     */
    public static final class GitHubRelease {
        @SerializedName("tag_name")
        String tagName;

        @SerializedName("html_url")
        String htmlUrl;

        @SerializedName("name")
        String name;

        @SerializedName("prerelease")
        boolean prerelease;

        @SerializedName("draft")
        boolean draft;

        boolean belongsTo(@NotNull Edition edition) {
            if (tagName == null) return false;
            return edition.tagPrefix().isEmpty()
                ? !Edition.isEditionTag(tagName)
                : tagName.startsWith(edition.tagPrefix());
        }

        @Contract("_ -> new")
        public @NotNull SemanticVersion asSemantic(@NotNull Edition edition) {
            String version = tagName != null ? tagName : "0.0.0";
            if (version.startsWith(edition.tagPrefix())) {
                version = version.substring(edition.tagPrefix().length());
            }
            if (version.startsWith("v") || version.startsWith("V")) {
                version = version.substring(1);
            }
            return new SemanticVersion(version);
        }

        @Contract(" -> new")
        public @NotNull SemanticVersion asSemantic() {
            return asSemantic(Edition.current());
        }

        @Nullable
        public String getHtmlUrl() {
            return htmlUrl;
        }
    }
}
