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

import de.sean.blockprot.nbt.LockReturnValue;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Helper to quickly obtain translations from a config by an enum key.
 *
 * <h3>Color and formatting support</h3>
 * <p>Translation values support two formats:
 * <ul>
 *   <li>Legacy {@code &} color codes (e.g. {@code &6}, {@code &a}, {@code &l})</li>
 *   <li>Adventure <a href="https://docs.advntr.dev/minimessage/format.html">MiniMessage</a>
 *       (e.g. {@code <gold>}, {@code <bold>}, {@code <gradient:red:blue>})</li>
 * </ul>
 *
 * <p>Detection uses a whitelist of known MiniMessage tag names. Placeholder tokens
 * like {@code {player}} or {@code <player>} that appear in usage strings are NOT
 * MiniMessage tags and are never passed to the MiniMessage parser.
 *
 * <p>If the raw string already contains section-symbol ({@code §}) color codes it
 * is returned as-is: no further processing is applied to avoid double-escaping or
 * passing pre-formatted strings to MiniMessage.
 *
 * <h3>BPR self-repair</h3>
 * <p>When a key is missing from the active language file but exists in the bundled
 * English file, the English value is used silently.</p>
 *
 * @since 0.1.10
 */
public final class Translator {

    public static final LinkedHashSet<String> DEFAULT_TRANSLATION_FILES = new LinkedHashSet<>(Arrays.asList(
        "translations_ar.yml", "translations_cs.yml", "translations_de.yml",
        "translations_en.yml", "translations_es.yml", "translations_fi.yml",
        "translations_fr.yml", "translations_he.yml", "translations_hu.yml",
        "translations_id.yml", "translations_it.yml", "translations_ja.yml",
        "translations_ko.yml", "translations_nl.yml", "translations_pl.yml",
        "translations_pt-br.yml", "translations_ro.yml", "translations_ru.yml",
        "translations_sk.yml", "translations_sv.yml", "translations_th.yml",
        "translations_tr.yml", "translations_uk.yml", "translations_zh-CN.yml",
        "translations_zh-TW.yml"
    ));

    @NotNull
    public static final Locale defaultLocale = Locale.UK;

    @NotNull
    private static final HashMap<TranslationKey, TranslationValue> values = new HashMap<>();

    @NotNull
    private static final HashMap<String, String> searchAliases = new HashMap<>();

    @NotNull
    private static Locale locale = defaultLocale;

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
        .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    /**
     * Whitelist of known MiniMessage tag names (lowercase).
     * Tokens not in this set are treated as plain text / placeholders, never passed to MM parser.
     */
    private static final Set<String> MM_TAGS = Set.of(
        // Color names
        "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
        "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
        "yellow", "white",
        // Formatting
        "bold", "italic", "underlined", "strikethrough", "obfuscated", "reset",
        // Special
        "gradient", "rainbow", "hover", "click", "insertion", "font",
        "color", "colour", "transition", "shadow_color",
        // Decorations
        "newline", "lang", "selector", "score", "nbt", "key"
    );

    /**
     * Regex to extract the tag name from a {@code <tagname>} or {@code </tagname>} token.
     * Group 1 is the name, stripped of leading slash and trailing content after colon/space.
     */
    private static final Pattern TAG_NAME_PATTERN = Pattern.compile("</?([a-zA-Z_][a-zA-Z0-9_]*)");

    private static final Pattern HEX_TAG_PATTERN = Pattern.compile("</?#[0-9a-fA-F]{6}>");

    private Translator() {}

    public static void loadFromConfigs(@NotNull final YamlConfiguration defaultConfig,
                                       @NotNull final YamlConfiguration config) {
        String localeStr = config.getString("locale");
        Translator.locale = (localeStr == null || localeStr.isBlank())
            ? Locale.ROOT
            : Locale.forLanguageTag(localeStr.replace('_', '-'));

        final String langFile = BlockProt.getDefaultConfigOrNull() != null
            ? BlockProt.getDefaultConfigOrNull().getLanguageFile()
            : "translations_en.yml";
        TranslationKey[] translations = TranslationKey.values();

        List<String> missingInBoth   = new ArrayList<>();
        List<String> missingInActive = new ArrayList<>();

        for (TranslationKey translation : translations) {
            String translationKey = translation.toString();

            if (!defaultConfig.contains(translationKey, true)
                    && !config.contains(translationKey, true)) {
                String fallback = BlockProt.getDefaultConfigOrNull() != null
                    ? BlockProt.getDefaultConfigOrNull().getTranslationFallbackString()
                    : null;
                values.put(translation, (fallback == null || fallback.isBlank())
                    ? new TranslationValue(translationKey)
                    : new TranslationValue(fallback));
                missingInBoth.add(translationKey);
                continue;
            }

            Object defaultValue = defaultConfig.get(translationKey);
            TranslationValue translatedValue =
                (defaultValue instanceof String)
                    ? new TranslationValue((String) defaultValue)
                    : TranslationValue.UNKNOWN_TRANSLATION_VALUE;

            Object activeValue = config.get(translationKey);
            if (activeValue instanceof String str && !str.isBlank()) {
                translatedValue.setTranslatedValue(str);
            } else {
                missingInActive.add(translationKey);
            }

            values.put(translation, translatedValue);
        }

        if (!missingInBoth.isEmpty()) {
            BlockProtLogger.log("Translator",
                "Keys missing in EN + " + langFile + " (" + missingInBoth.size() + "): "
                + String.join(", ", missingInBoth));
        }
        if (!missingInActive.isEmpty()) {
            BlockProtLogger.log("Translator",
                "English fallback keys used in " + langFile
                + " (" + missingInActive.size() + "): "
                + String.join(", ", missingInActive));
        }

        searchAliases.clear();
        loadAliases(defaultConfig);
        loadAliases(config);
    }

    private static void loadAliases(@NotNull YamlConfiguration cfg) {
        var section = cfg.getConfigurationSection("search_aliases");
        if (section == null) return;
        for (String canonicalKey : section.getKeys(false)) {
            String rawVal = section.getString(canonicalKey);
            if (rawVal == null || rawVal.isBlank()) continue;
            String target = canonicalKey.toUpperCase(Locale.ROOT);
            for (String alias : rawVal.split(",")) {
                String trimmed = alias.trim().toUpperCase(Locale.ROOT);
                if (!trimmed.isEmpty()) {
                    searchAliases.put(trimmed, target);
                    String unaccented = java.text.Normalizer.normalize(trimmed, java.text.Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "");
                    searchAliases.put(unaccented, target);
                }
            }
        }
    }

    @NotNull
    public static String normalizeSearchToken(@NotNull String token) {
        String upper = token.trim().toUpperCase(Locale.ROOT);
        return searchAliases.getOrDefault(upper, upper);
    }

    private static boolean containsMiniMessage(@NotNull String text) {
        if (text.indexOf('\u00A7') >= 0) return false;

        if (HEX_TAG_PATTERN.matcher(text).find()) return true;
        var matcher = TAG_NAME_PATTERN.matcher(text);
        while (matcher.find()) {
            String tagName = matcher.group(1).toLowerCase(Locale.ROOT);
            if (MM_TAGS.contains(tagName)) return true;
        }
        return false;
    }

    @NotNull
    private static String process(@NotNull String raw) {
        if (raw.indexOf('\u00A7') >= 0) return raw;

        if (containsMiniMessage(raw)) {
            Component component = MINI_MESSAGE.deserialize(raw);
            return LEGACY_SERIALIZER.serialize(component);
        }

        return raw.replace('&', '\u00a7');
    }

    @NotNull
    public static String getRaw(@NotNull final TranslationKey key) {
        TranslationValue value = values.get(key);
        if (value == null) {
            String fallback = null;
            if (BlockProt.getDefaultConfigOrNull() != null) {
                fallback = BlockProt.getDefaultConfigOrNull().getTranslationFallbackString();
            }
            return (fallback == null || fallback.isBlank()) ? key.toString() : fallback;
        }
        return value.getValue();
    }

    @NotNull
    public static String get(@NotNull final TranslationKey key) {
        TranslationValue value = values.get(key);
        if (value == null) {
            String fallback = null;
            if (BlockProt.getDefaultConfigOrNull() != null) {
                fallback = BlockProt.getDefaultConfigOrNull().getTranslationFallbackString();
            }
            return process((fallback == null || fallback.isBlank()) ? key.toString() : fallback);
        }
        return process(value.getValue());
    }

    @NotNull
    public static String get(@NotNull final LockReturnValue.Reason reason) {
        return switch (reason) {
            case NO_PERMISSION -> get(TranslationKey.MESSAGES__NO_PERMISSION);
            case EXCEEDED_MAX_BLOCK_COUNT -> get(TranslationKey.MESSAGES__EXCEEDED_MAX_BLOCK_COUNT);
        };
    }

    @NotNull
    public static Locale getLocale() {
        return locale;
    }

    public static void resetTranslations() {
        values.clear();
        searchAliases.clear();
    }

    public static int computeCompletionPercentage(@NotNull YamlConfiguration langFile) {
        int presentKeys = 0;
        for (TranslationKey translation : TranslationKey.values()) {
            String key = translation.toString();
            if (langFile.isConfigurationSection(key)) continue;
            Object value = langFile.get(key);
            if (value instanceof String && !((String) value).isEmpty()) {
                presentKeys++;
            }
        }
        int totalKeys = TranslationKey.values().length;
        if (totalKeys == 0) return 0;
        return (int) Math.round(100.0 * presentKeys / totalKeys);
    }

    public static int countMissingKeys(@NotNull YamlConfiguration langFile, @NotNull YamlConfiguration reference) {
        int missing = 0;
        for (TranslationKey translation : TranslationKey.values()) {
            String key = translation.toString();
            if (reference.contains(key, true) && !reference.isConfigurationSection(key)) {
                if (!langFile.contains(key, true) || langFile.isConfigurationSection(key)) {
                    missing++;
                } else {
                    Object val = langFile.get(key);
                    if (!(val instanceof String) || ((String) val).isEmpty()) {
                        missing++;
                    }
                }
            }
        }
        return missing;
    }
}