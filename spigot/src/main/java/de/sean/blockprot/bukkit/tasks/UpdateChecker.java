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
import de.sean.blockprot.bukkit.BlockProtConsole;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.Edition;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import de.sean.blockprot.bukkit.util.TemporaryActionBar;
import de.sean.blockprot.util.ReleaseNotesFormatter;
import de.sean.blockprot.util.SemanticVersion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
import java.util.Collections;
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
        "https://api.github.com/repos/Zaynr-Ecosystem/BlockProt-Reloaded/releases?per_page=100";

    private static final String RELEASES_URL =
        "https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/releases";

    public static final TextColor PASTEL_CORAL = TextColor.color(0xFF6B6B);
    public static final TextColor REDDISH_GOLD = TextColor.color(0xFF8C69);

    private static String releaseUrl() {
        return Edition.current().tagPrefix().isEmpty() ? RELEASES_URL + "/latest" : RELEASES_URL;
    }

    public static @NotNull String tagReleaseUrl(@NotNull String rawVersion, @NotNull Edition edition) {
        return RELEASES_URL + "/tag/" + edition.tagPrefix() + rawVersion;
    }

    public static @Nullable SemanticVersion getAvailableUpdate(@NotNull String currentPluginVersion) {
        SemanticVersion cached = latestVersion;
        if (cached == null) return null;
        try {
            SemanticVersion current = new SemanticVersion(currentPluginVersion);
            return cached.compareTo(current) > 0 ? cached : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Cached result of the last successful GitHub API call.
     * Package-accessible so {@link BackupTask} can read the cached value
     * without issuing a redundant HTTP request.
     */
    private static final long CACHE_TTL_MILLIS = 30 * 60 * 1000L;
    private static volatile long lastCheckTimeMillis = 0L;

    @Nullable
    public static volatile SemanticVersion latestVersion;

    /**
     * Full cached release that {@link #latestVersion} was parsed from,
     * carrying its tag, name and per-tag release page.
     */
    @Nullable
    public static volatile GitHubRelease latestRelease;

    @Nullable
    public static volatile GitHubRelease installedRelease;

    @Nullable
    private final List<Player> recipients;

    @Nullable
    private final Runnable onComplete;

    @NotNull
    private final String pluginVersion;

    @NotNull
    private final SemanticVersion currentVersion;

    private final boolean forceRefresh;

    private final boolean actionBarOnly;

    public UpdateChecker(@NotNull final String version) {
        this(version, null, null, false, false);
    }

    public UpdateChecker(@NotNull final String version,
                         @Nullable final List<Player> recipients) {
        this(version, recipients, null, false, false);
    }

    public UpdateChecker(@NotNull final String version,
                         @Nullable final List<Player> recipients,
                         final boolean forceRefresh) {
        this(version, recipients, null, forceRefresh, false);
    }

    public UpdateChecker(@NotNull final String version,
                         @Nullable final Runnable onComplete) {
        this(version, null, onComplete, false, false);
    }

    public UpdateChecker(@NotNull final String version,
                         final boolean forceRefresh,
                         @Nullable final Runnable onComplete) {
        this(version, null, onComplete, forceRefresh, false);
    }

    public static @NotNull UpdateChecker forJoinActionBar(@NotNull final String version,
                                                          @NotNull final Player player) {
        return new UpdateChecker(version, Collections.singletonList(player), null, false, true);
    }

    private UpdateChecker(@NotNull final String version,
                          @Nullable final List<Player> recipients,
                          @Nullable final Runnable onComplete,
                          final boolean forceRefresh,
                          final boolean actionBarOnly) {
        this.pluginVersion = version;
        this.recipients = recipients;
        this.onComplete = onComplete;
        this.currentVersion = new SemanticVersion(version);
        this.forceRefresh = forceRefresh;
        this.actionBarOnly = actionBarOnly;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        if (!forceRefresh && latestVersion != null && (now - lastCheckTimeMillis < CACHE_TTL_MILLIS)) {
            this.sendMessage(currentVersion, latestVersion, latestRelease);
            return;
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
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

            if (response.statusCode() != 200) {
                if (latestVersion != null) {
                    this.sendMessage(currentVersion, latestVersion, latestRelease);
                } else if (onComplete != null) {
                    onComplete.run();
                }
                return;
            }

            // Parse the list of releases and find the highest version
            // compatible with the current build channel (see class javadoc).
            GitHubRelease[] releases = new com.google.gson.Gson().fromJson(
                response.body(), GitHubRelease[].class);

            List<GitHubRelease> releaseList = releases != null ? Arrays.asList(releases) : Collections.emptyList();
            GitHubRelease currentRelease = selectInstalled(releaseList, currentVersion, Edition.current());
            if (currentRelease != null) {
                UpdateChecker.installedRelease = currentRelease;
            }
            GitHubRelease bestRelease = selectBest(releaseList, currentVersion, Edition.current());
            if (bestRelease == null) {
                if (onComplete != null) onComplete.run();
                return;
            }
            SemanticVersion best = bestRelease.asSemantic(Edition.current());
            UpdateChecker.latestVersion = best;
            UpdateChecker.latestRelease = bestRelease;
            UpdateChecker.lastCheckTimeMillis = System.currentTimeMillis();
            this.sendMessage(currentVersion, best, bestRelease);

        } catch (Exception ignored) {
            if (latestVersion != null) {
                this.sendMessage(currentVersion, latestVersion, latestRelease);
            } else if (onComplete != null) {
                onComplete.run();
            }
        }
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
            if (!current.isPreRelease() && !v.isHotfix() && (rel.prerelease || v.isPreRelease())) continue;
            if (best == null || v.compareTo(best) > 0) {
                best = v;
                bestRelease = rel;
            }
        }
        return bestRelease;
    }

    static @Nullable GitHubRelease selectInstalled(@NotNull List<GitHubRelease> releases,
                                                   @NotNull SemanticVersion current,
                                                   @NotNull Edition edition) {
        for (GitHubRelease rel : releases) {
            if (rel.draft || !rel.belongsTo(edition)) continue;
            if (rel.asSemantic(edition).compareTo(current) == 0) {
                return rel;
            }
        }
        return null;
    }

    private void sendMessage(SemanticVersion currentVersion, SemanticVersion latestVersion,
                             @Nullable GitHubRelease release) {
        boolean isOutdated = latestVersion.compareTo(currentVersion) > 0;
        String releaseUrl = release != null && release.htmlUrl != null && !release.htmlUrl.isBlank()
            ? release.htmlUrl
            : releaseUrl();

        if (isOutdated) {
            TranslationKey key = consoleOutdatedKey(latestVersion);
            String msg = Translator.get(key)
                .replace("{version}", latestVersion.toString())
                .replace("{base_version}", latestVersion.baseVersion())
                + " | " + releaseUrl;
            BlockProtConsole.updateAlert(msg, latestVersion.isPreRelease());
        } else if (this.recipients == null || this.recipients.isEmpty()) {
            if (latestVersion.compareTo(currentVersion) < 0) {
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

        if (this.recipients != null && !this.recipients.isEmpty()) {
            if (this.actionBarOnly) {
                if (isOutdated) {
                    Component actionBarComp = buildActionBarUpdateComponent(latestVersion);
                    long durationTicks = BlockProt.getDefaultConfig() != null
                        ? BlockProt.getDefaultConfig().getActionBarDurationTicks()
                        : 120L;
                    for (Player player : recipients) {
                        if (!player.isOnline()) continue;
                        if (BlockProt.hasFoliaLib()) {
                            BlockProt.getFoliaLib().getScheduler().runAtEntityLater(player, task -> {
                                if (player.isOnline()) {
                                    TemporaryActionBar.show(player, actionBarComp, durationTicks);
                                }
                            }, 20L);
                        } else {
                            TemporaryActionBar.show(player, actionBarComp, durationTicks);
                        }
                    }
                }
            } else {
                LegacyComponentSerializer legacy = LegacyComponentSerializer.builder()
                    .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
                Component comp;
                if (isOutdated) {
                    String message = formatOutdatedText(latestVersion);
                    String downloadLabel = stripTrailingColon(Translator.get(TranslationKey.DIALOGS__UPDATE__DOWNLOAD));
                    comp = legacy.deserialize(message)
                        .color(outdatedAlertColor(latestVersion))
                        .append(Component.text(" [" + downloadLabel + " v" + latestVersion + "]",
                            TextColor.color(0x7EC8E3), TextDecoration.UNDERLINED))
                        .clickEvent(ClickEvent.openUrl(releaseUrl))
                        .hoverEvent(HoverEvent.showText(
                            Component.text(Translator.get(TranslationKey.MESSAGES__UPDATE__CLICK_HINT))));
                } else if (latestVersion.compareTo(currentVersion) < 0) {
                    String message = currentVersion.isHotfix()
                        ? Translator.get(TranslationKey.MESSAGES__UPDATE__AHEAD_HOTFIX)
                            .replace("{version}", currentVersion.toString())
                            .replace("{base_version}", currentVersion.baseVersion())
                        : Translator.get(TranslationKey.MESSAGES__UPDATE__AHEAD)
                            .replace("{version}", latestVersion.toString());
                    comp = legacy.deserialize(message);
                } else {
                    String raw = Translator.getRaw(TranslationKey.MESSAGES__UPDATE__UP_TO_DATE);
                    if (raw.isBlank()) {
                        raw = "<#98FF98>BlockProt Reloaded is up to date.";
                    }
                    comp = MiniMessage.miniMessage().deserialize(raw);
                }
                for (Player player : recipients) {
                    ComponentMessages.send(player, comp);
                    if (isOutdated && release != null) {
                        sendHighlightLines(player, release, 4);
                    }
                }
            }
        }

        if (onComplete != null) {
            onComplete.run();
        }
    }

    public static void sendHighlightLines(@NotNull Player player,
                                          @NotNull GitHubRelease release,
                                          int maxLines) {
        List<String> lines = ReleaseNotesFormatter.formatSummaryLines(release.body, maxLines, 90);
        for (String line : lines) {
            ComponentMessages.send(player, Component.text("  " + line, TextColor.color(0xC8D0D8)));
        }
    }

    public static void sendReleaseNotesChat(@NotNull Player player,
                                            @Nullable GitHubRelease release,
                                            @NotNull String version,
                                            @NotNull String fallbackUrl,
                                            int maxLines) {
        String url = release != null && release.htmlUrl != null && !release.htmlUrl.isBlank()
            ? release.htmlUrl
            : fallbackUrl;
        List<String> summaryLines = release != null
            ? ReleaseNotesFormatter.formatSummaryLines(release.body, maxLines, 90)
            : Collections.emptyList();
        if (!summaryLines.isEmpty()) {
            String headerRaw = Translator.getRaw(TranslationKey.MESSAGES__UPDATE__NOTES_HEADER);
            if (headerRaw.isBlank()) {
                headerRaw = "<#7EC8E3>Release notes for v{version}:";
            }
            Component header = MiniMessage.miniMessage().deserialize(headerRaw.replace("{version}", version))
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(Component.text(Translator.get(TranslationKey.MESSAGES__UPDATE__CLICK_HINT))));
            ComponentMessages.send(player, header);
            for (String line : summaryLines) {
                ComponentMessages.send(player, Component.text("  " + line, TextColor.color(0xC8D0D8)));
            }
        }

        String changelogLabel = stripTrailingColon(Translator.get(TranslationKey.DIALOGS__UPDATE__OPEN_GITHUB));
        Component link = Component.text(changelogLabel, TextColor.color(0x7EC8E3), TextDecoration.UNDERLINED)
            .clickEvent(ClickEvent.openUrl(url))
            .hoverEvent(HoverEvent.showText(Component.text(Translator.get(TranslationKey.MESSAGES__UPDATE__CLICK_HINT))));
        ComponentMessages.send(player, link);
    }

    private static String stripTrailingColon(@NotNull String raw) {
        String trimmed = raw.stripTrailing();
        if (trimmed.endsWith(":")) {
            return trimmed.substring(0, trimmed.length() - 1).stripTrailing();
        }
        return trimmed;
    }

    public static void sendLatestOrInstalledNotesChat(@NotNull Player player,
                                                      @NotNull String pluginVersion,
                                                      int maxLines) {
        SemanticVersion cached = UpdateChecker.latestVersion;
        SemanticVersion current = new SemanticVersion(pluginVersion);
        boolean outdated = cached != null && cached.compareTo(current) > 0;
        GitHubRelease target = outdated ? UpdateChecker.latestRelease : UpdateChecker.installedRelease;
        String ver = outdated ? cached.toString() : pluginVersion;
        String url = tagReleaseUrl(ver, Edition.current());
        sendReleaseNotesChat(player, target, ver, url, maxLines);
    }

    /**
     * Message template for an available update, chosen by the release channel:
     * hotfixes (important bug-fix) and experimental pre-releases (BEDev builds)
     * get their own wording, everything else uses the standard outdated message.
     */
    @Contract("_ -> !null")
    public static @NotNull TranslationKey outdatedMessageKey(@NotNull SemanticVersion version) {
        if (version.isHotfix()) return TranslationKey.MESSAGES__UPDATE__OUTDATED_HOTFIX;
        if (version.isPreRelease()) return TranslationKey.MESSAGES__UPDATE__OUTDATED_DEV;
        return TranslationKey.MESSAGES__UPDATE__OUTDATED;
    }

    public static @NotNull TextColor outdatedAlertColor(@NotNull SemanticVersion version) {
        return version.isPreRelease() ? REDDISH_GOLD : PASTEL_CORAL;
    }

    public static @NotNull String formatOutdatedText(@NotNull SemanticVersion version) {
        return Translator.get(outdatedMessageKey(version))
            .replace("{version}", version.toString())
            .replace("{base_version}", version.baseVersion());
    }

    @Contract("_ -> !null")
    static @NotNull TranslationKey actionBarOutdatedKey(@NotNull SemanticVersion version) {
        if (version.isHotfix()) return TranslationKey.MESSAGES__UPDATE__ACTIONBAR_OUTDATED_HOTFIX;
        if (version.isPreRelease()) return TranslationKey.MESSAGES__UPDATE__ACTIONBAR_OUTDATED_DEV;
        return TranslationKey.MESSAGES__UPDATE__ACTIONBAR_OUTDATED;
    }

    static @NotNull Component buildActionBarUpdateComponent(@NotNull SemanticVersion latestVersion) {
        TranslationKey key = actionBarOutdatedKey(latestVersion);
        String raw = Translator.getRaw(key);
        if (raw.isBlank() || raw.equals(key.toString())) {
            if (latestVersion.isHotfix()) {
                raw = "<#FF6B6B>Hotfix v{version} available for BlockProt Reloaded! <#7EC8E3>Use /bp update";
            } else if (latestVersion.isPreRelease()) {
                raw = "<#FF8C69>BEDev {version} available! <#7EC8E3>Use /bp update";
            } else {
                raw = "<#FF6B6B>BlockProt Reloaded v{version} available! <#7EC8E3>Use /bp update";
            }
        }
        String formatted = raw
            .replace("{version}", latestVersion.toString())
            .replace("{base_version}", latestVersion.baseVersion());
        if (formatted.indexOf('\u00A7') >= 0) {
            return LegacyComponentSerializer.legacySection().deserialize(formatted);
        }
        return MiniMessage.miniMessage().deserialize(formatted);
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

        @SerializedName("body")
        String body;

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

        @Nullable
        public String getBody() {
            return body;
        }

        @Nullable
        public String getTagName() {
            return tagName;
        }

        @Nullable
        public String getName() {
            return name;
        }
    }
}
