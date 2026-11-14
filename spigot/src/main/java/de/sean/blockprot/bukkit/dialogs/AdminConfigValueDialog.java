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

package de.sean.blockprot.bukkit.dialogs;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Edits a single config.yml value (int, double, or raw string) from inside a
 * dialog-based admin settings screen ({@link AdminConfigDialog} and future
 * admin/settings screens), using a native {@link DialogTextField}.
 *
 * <p>Unlike the previous mechanism in {@code AdminConfigDialog} (which called
 * {@code bridge.closeDialog(player)} and opened a separate sign/anvil/chat
 * prompt), this shows the input inside the same dialog that is already open
 * and reads it back on confirm via {@link DialogBridge#showValueInput}. No
 * dialog is closed or re-shown before the value is captured, so the client
 * cursor-recenter behavior that occurs when a dialog is reopened does not
 * apply here.
 *
 * <p>When the Dialog API is unavailable (non-Paper, or below 1.21.7), callers
 * should use {@link #openFallback} directly, which reuses the existing
 * sign/anvil/chat prompt chain instead of this class.
 */
public final class AdminConfigValueDialog {

    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_MINT = TextColor.color(0x98FF98);
    private static final TextColor PASTEL_CORAL = TextColor.color(0xFF6B6B);
    private static final TextColor PASTEL_GOLD = TextColor.color(0xF0E6A0);
    private static final TextColor SOFT_BLUE = TextColor.color(0x7EC8E3);
    private static final TextColor PASTEL_PURPLE = TextColor.color(0xB39DDB);

    private AdminConfigValueDialog() {}

    /**
     * Shows a dialog-native text field pre-filled with currentValue, and calls
     * onValid with the parsed int once the player clicks confirm. Invalid
     * input re-shows the same value dialog with an error line instead of
     * silently discarding the edit. The back button returns to reopen via
     * onCancel, which the caller supplies to redraw the screen the field was
     * opened from.
     */
    public static void openInt(
        @NotNull Player player,
        @NotNull String configKey,
        @NotNull String hintLabel,
        int currentValue,
        @NotNull Consumer<Integer> onValid,
        @NotNull Runnable onCancel
    ) {
        openInt(player, configKey, hintLabel, currentValue, onValid, onCancel, null);
    }

    /**
     * Shows a dialog-native text field for a free-text or validated string
     * config value. validator is called with the trimmed input on confirm;
     * return null to accept the value as-is, or an error message to reject it
     * and re-show the field with that message. Pass a validator that always
     * returns null for genuinely free-form text (e.g. fallback_string).
     */
    public static void openText(
        @NotNull Player player,
        @NotNull String configKey,
        @NotNull String hintLabel,
        @NotNull String currentValue,
        @NotNull java.util.function.Function<String, String> validator,
        @NotNull Consumer<String> onValid,
        @NotNull Runnable onCancel
    ) {
        openText(player, configKey, hintLabel, currentValue, validator, onValid, onCancel, null);
    }

    private static void openText(
        @NotNull Player player,
        @NotNull String configKey,
        @NotNull String hintLabel,
        @NotNull String currentValue,
        @NotNull java.util.function.Function<String, String> validator,
        @NotNull Consumer<String> onValid,
        @NotNull Runnable onCancel,
        @Nullable String errorLine
    ) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) {
            openFallback(player, configKey, currentValue, raw -> {
                String err = validator.apply(raw.trim());
                if (err == null) onValid.accept(raw.trim());
                else player.sendMessage(err);
            });
            return;
        }
        show(player, bridge, configKey, hintLabel, currentValue, onCancel, errorLine, raw -> {
            String trimmed = raw.trim();
            String err = validator.apply(trimmed);
            if (err == null) {
                onValid.accept(trimmed);
            } else {
                openText(player, configKey, hintLabel, currentValue, validator, onValid, onCancel, err);
            }
        });
    }

    private static void openInt(
        @NotNull Player player,
        @NotNull String configKey,
        @NotNull String hintLabel,
        int currentValue,
        @NotNull Consumer<Integer> onValid,
        @NotNull Runnable onCancel,
        @Nullable String errorLine
    ) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) {
            openFallbackInt(player, configKey, currentValue, onValid);
            return;
        }
        show(player, bridge, configKey, hintLabel, String.valueOf(currentValue), onCancel, errorLine, raw -> {
            try {
                onValid.accept(Integer.parseInt(raw.trim()));
            } catch (NumberFormatException e) {
                String err = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__INVALID_NUMBER))
                    .replace("{input}", raw);
                openInt(player, configKey, hintLabel, currentValue, onValid, onCancel, err);
            }
        });
    }

    /** Same contract as {@link #openInt}, for double-valued config keys. */
    public static void openDouble(
        @NotNull Player player,
        @NotNull String configKey,
        @NotNull String hintLabel,
        double currentValue,
        @NotNull Consumer<Double> onValid,
        @NotNull Runnable onCancel
    ) {
        openDouble(player, configKey, hintLabel, currentValue, onValid, onCancel, null);
    }

    private static void openDouble(
        @NotNull Player player,
        @NotNull String configKey,
        @NotNull String hintLabel,
        double currentValue,
        @NotNull Consumer<Double> onValid,
        @NotNull Runnable onCancel,
        @Nullable String errorLine
    ) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) {
            openFallbackDouble(player, configKey, currentValue, onValid);
            return;
        }
        show(player, bridge, configKey, hintLabel, String.valueOf(currentValue), onCancel, errorLine, raw -> {
            try {
                onValid.accept(Double.parseDouble(raw.trim()));
            } catch (NumberFormatException e) {
                String err = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__INVALID_NUMBER))
                    .replace("{input}", raw);
                openDouble(player, configKey, hintLabel, currentValue, onValid, onCancel, err);
            }
        });
    }

    private static void show(
        @NotNull Player player,
        @NotNull DialogBridge bridge,
        @NotNull String configKey,
        @NotNull String hintLabel,
        @NotNull String currentValue,
        @NotNull Runnable onCancel,
        @Nullable String errorLine,
        @NotNull Consumer<String> onRawSubmit
    ) {
        Component title = Component.text(hintLabel, PASTEL_GOLD, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new java.util.ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(hintLabel, SOFT_GRAY)));
        body.add(DialogBodyEntry.text(Component.join(JoinConfiguration.newlines(),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__VALUE_CURRENT)) + currentValue, SOFT_GRAY))));
        if (errorLine != null) {
            body.add(DialogBodyEntry.text(Component.text(errorLine, PASTEL_CORAL)));
        }

        DialogTextField field = DialogTextField.of(
            sanitizeInputKey(configKey),
            Component.text(hintLabel, NamedTextColor.WHITE),
            currentValue,
            stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CONFIRM_VALUE))
        );

        DialogButton back = new DialogButton("cancel",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__VALUE_CLICK_EDIT)), TextColor.color(0x888888)),
            p -> onCancel.run());

        bridge.showValueInput(player, title, body, field, onRawSubmit, back);
    }

    // -- Non-Paper / pre-1.21.7 fallback, reusing the existing prompt chain --

    private static void openFallbackInt(@NotNull Player player, @NotNull String configKey,
                                         int currentValue, @NotNull Consumer<Integer> onValid) {
        openFallback(player, configKey, String.valueOf(currentValue), raw -> {
            try {
                onValid.accept(Integer.parseInt(raw.trim()));
            } catch (NumberFormatException e) {
                player.sendMessage(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__INVALID_NUMBER))
                    .replace("{input}", raw));
            }
        });
    }

    private static void openFallbackDouble(@NotNull Player player, @NotNull String configKey,
                                            double currentValue, @NotNull Consumer<Double> onValid) {
        openFallback(player, configKey, String.valueOf(currentValue), raw -> {
            try {
                onValid.accept(Double.parseDouble(raw.trim()));
            } catch (NumberFormatException e) {
                player.sendMessage(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__INVALID_NUMBER))
                    .replace("{input}", raw));
            }
        });
    }

    /**
     * Opens the chat-input prompt directly, for servers without the Dialog
     * API. Kept as a public entry point so non-dialog admin screens (or a
     * future inventory-based settings menu) can reuse it without duplicating
     * the {@link de.sean.blockprot.bukkit.inventories.TextInput} call.
     */
    public static void openFallback(@NotNull Player player, @NotNull String configKey,
                                     @NotNull String currentValue, @NotNull Consumer<String> onRawSubmit) {
        String prompt = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTER_NEW_VALUE))
            .replace("{key}", configKey).replace("{value}", currentValue);
        de.sean.blockprot.bukkit.inventories.TextInput.open(player, BlockProt.getInstance(), prompt, input -> {
            if (input == null || input.isBlank()) return;
            onRawSubmit.accept(input.trim());
        });
    }

    private static String stripColor(String s) {
        return s.replaceAll("[\u00A7&][0-9a-fk-orxA-F]", "");
    }

    /** Paper's dialog text input key must not contain dots; config keys are dotted paths. */
    private static String sanitizeInputKey(String configKey) {
        return configKey.replaceAll("[^a-zA-Z0-9_]", "_");
    }
}

