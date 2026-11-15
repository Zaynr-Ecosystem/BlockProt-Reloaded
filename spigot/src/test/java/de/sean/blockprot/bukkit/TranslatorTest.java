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

import de.sean.blockprot.bukkit.config.DefaultConfig;
import org.bukkit.configuration.file.YamlConfiguration;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class TranslatorTest {

    private static ServerMock server;
    private static File tempFolder;

    @BeforeAll
    static void setup() throws Exception {
        server = MockBukkit.mock();
        tempFolder = File.createTempFile("bp_translator_test", "");
        tempFolder.delete();
        tempFolder.mkdirs();
        tempFolder.deleteOnExit();

        var field = BlockProt.class.getDeclaredField("defaultConfig");
        field.setAccessible(true);
        field.set(null, new DefaultConfig(new YamlConfiguration(), tempFolder));

        var stream = TranslatorTest.class.getResourceAsStream("/lang/translations_en.yml");
        if (stream != null) {
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                YamlConfiguration lang = YamlConfiguration.loadConfiguration(reader);
                Translator.loadFromConfigs(lang, lang);
            }
        }
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void allTranslationKeysHaveDefaultMapping() {
        for (TranslationKey key : TranslationKey.values()) {
            String result = Translator.get(key);
            assertNotNull(result, "TranslationKey." + key + " returned null");
            assertNotEquals(key.toString(), result, "TranslationKey." + key + " returned unmapped raw key");
            if (key != TranslationKey.INVENTORIES__BLOCK_INFO__LORE_PREFIX) {
                assertFalse(result.isBlank(), "TranslationKey." + key + " returned blank");
            }
        }
    }

    @Test
    void translatorReturnsNonEmptyForCommonKeys() {
        String noPermission = Translator.get(TranslationKey.MESSAGES__NO_PERMISSION);
        assertFalse(noPermission.isBlank(), "MESSAGES__NO_PERMISSION should not be blank");
    }

    @Test
    void translatorFallsBackGracefully() throws Exception {
        YamlConfiguration english = new YamlConfiguration();
        var stream = TranslatorTest.class.getResourceAsStream("/lang/translations_en.yml");
        assertNotNull(stream, "Bundled /lang/translations_en.yml must exist");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            english.load(reader);
        }
        String expected = english.getString(TranslationKey.MESSAGES__LOCK_ON_PLACE_SUCCESS.toString());
        assertNotNull(expected);
        assertFalse(expected.isBlank());

        try {
            Translator.loadFromConfigs(english, new YamlConfiguration());
            String raw = Translator.getRaw(TranslationKey.MESSAGES__LOCK_ON_PLACE_SUCCESS);
            String rendered = Translator.get(TranslationKey.MESSAGES__LOCK_ON_PLACE_SUCCESS);
            assertEquals(expected, raw);
            assertFalse(rendered.isBlank());
            assertFalse(rendered.contains("<green>"));
            assertTrue(rendered.contains("Block protected!"));
        } finally {
            Translator.loadFromConfigs(english, english);
        }
    }

    @Test
    void notifyOpOfUpdatesDefaultsTrueAndHonorsExplicitFalse() {
        YamlConfiguration unset = new YamlConfiguration();
        assertTrue(new DefaultConfig(unset, tempFolder).shouldNotifyOpOfUpdates());

        YamlConfiguration disabled = new YamlConfiguration();
        disabled.set("notify_op_of_updates", false);
        assertFalse(new DefaultConfig(disabled, tempFolder).shouldNotifyOpOfUpdates());
    }
}
