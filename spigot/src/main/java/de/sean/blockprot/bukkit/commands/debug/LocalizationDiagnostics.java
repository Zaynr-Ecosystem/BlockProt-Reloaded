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

package de.sean.blockprot.bukkit.commands.debug;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.config.LangConfig;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class LocalizationDiagnostics {

    private LocalizationDiagnostics() {
    }

    public static void run(@NotNull DiagnosticsContext ctx) {
        DiagnosticsContext.runDomain("3/10", "LOCALIZATION & TRANSLATIONS");
        checkTranslations(ctx);
        checkLanguages(ctx);
    }

    private static void checkTranslations(@NotNull DiagnosticsContext ctx) {
        int blank = 0;
        int errors = 0;
        for (TranslationKey key : TranslationKey.values()) {
            try {
                String v = Translator.get(key);
                if (v == null || v.isBlank()) {
                    blank++;
                }
            } catch (Exception e) {
                errors++;
                BlockProtLogger.fail("Translation key " + key.name(), e.getMessage());
            }
        }
        if (errors == 0) {
            BlockProtLogger.pass("Translations: " + TranslationKey.values().length + " keys OK, " + blank + " blank");
            ctx.pass();
        } else {
            BlockProtLogger.fail("Translations", errors + " key(s) threw exceptions");
            ctx.fail();
        }
    }

    private static void checkLanguages(@NotNull DiagnosticsContext ctx) {
        try {
            BlockProt plugin = BlockProt.getInstance();
            String active = BlockProt.getDefaultConfig().getLanguageFile();
            String[] allLangs = Translator.DEFAULT_TRANSLATION_FILES.toArray(new String[0]);

            int totalKeys = TranslationKey.values().length;
            int langOk = 0;
            int langFail = 0;
            int skipped = 0;

            BlockProtLogger.subGroup("Language files:");

            for (String fileName : allLangs) {
                if (!LangConfig.isLanguageEnabled(fileName)) {
                    skipped++;
                    BlockProtLogger.skipSub(fileName, "disabled in config");
                    continue;
                }
                boolean isActive = fileName.equals(active);
                YamlConfiguration langFile = null;
                File diskFile = new File(plugin.getDataFolder(), "lang/" + fileName);
                try {
                    if (diskFile.exists()) {
                        langFile = YamlConfiguration.loadConfiguration(diskFile);
                    } else {
                        InputStream jarStream = plugin.getResource("lang/" + fileName);
                        if (jarStream == null) {
                            BlockProtLogger.failSub(fileName, "not found on disk or in jar");
                            langFail++;
                            continue;
                        }
                        langFile = YamlConfiguration.loadConfiguration(
                            new BufferedReader(new InputStreamReader(jarStream, StandardCharsets.UTF_8)));
                    }
                } catch (Exception e) {
                    BlockProtLogger.failSub(fileName, "load error: " + e.getMessage());
                    langFail++;
                    continue;
                }

                int presentKeys = 0;
                List<String> missingKeys = new ArrayList<>();
                for (TranslationKey key : TranslationKey.values()) {
                    String k = key.toString();
                    if (langFile.isConfigurationSection(k)) continue;
                    Object value = langFile.get(k);
                    if (value instanceof String && !((String) value).isEmpty()) {
                        presentKeys++;
                    } else {
                        missingKeys.add(key.name());
                    }
                }
                int pct = totalKeys == 0 ? 0 : (int) Math.round(100.0 * presentKeys / totalKeys);
                String status = isActive ? "ACTIVE" : "inactive";
                String detail = fileName + " [" + status + "]: " + pct + "% (" + presentKeys + "/" + totalKeys + " keys)";
                if (pct < 100) {
                    int totalMissing = missingKeys.size();
                    int showCount = Math.min(totalMissing, 5);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < showCount; i++) {
                        if (i > 0) sb.append(" ");
                        sb.append(missingKeys.get(i));
                    }
                    if (totalMissing > showCount) sb.append(" ...");
                    detail += " missing=" + totalMissing + " ex: " + sb;
                }
                BlockProtLogger.passSub(detail);
                langOk++;
            }

            if (langFail == 0) {
                ctx.pass();
            } else {
                ctx.fail();
            }
        } catch (Exception e) {
            BlockProtLogger.fail("Language files", e.getMessage());
            ctx.fail();
        }
    }
}
