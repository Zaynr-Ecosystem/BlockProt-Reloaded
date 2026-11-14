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

package de.sean.blockprot.bukkit.bedrock;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.Form;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Bridge between BlockProt and Floodgate / Geyser Bedrock forms.
 */
public final class BedrockBridge {

    private BedrockBridge() {}

    public static boolean isFloodgatePresent() {
        return Bukkit.getPluginManager().isPluginEnabled("floodgate")
            || Bukkit.getPluginManager().isPluginEnabled("Floodgate");
    }

    public static boolean isGeyserPresent() {
        return Bukkit.getPluginManager().isPluginEnabled("Geyser-Spigot")
            || Bukkit.getPluginManager().isPluginEnabled("geyser")
            || Bukkit.getPluginManager().isPluginEnabled("Geyser");
    }

    @NotNull
    public static String sanitizeHandshakeHostname(@Nullable String rawHostname) {
        if (rawHostname == null) return "";
        int nulIdx = rawHostname.indexOf('\0');
        String clean = nulIdx >= 0 ? rawHostname.substring(0, nulIdx) : rawHostname;
        int queryIdx = clean.indexOf('?');
        return queryIdx >= 0 ? clean.substring(0, queryIdx) : clean;
    }

    @Nullable
    public static String extractHandshakeQueryParam(@Nullable String rawHostname, @NotNull String paramName) {
        if (rawHostname == null || paramName.isEmpty()) return null;
        int nulIdx = rawHostname.indexOf('\0');
        String clean = nulIdx >= 0 ? rawHostname.substring(0, nulIdx) : rawHostname;
        int queryIdx = clean.indexOf('?');
        if (queryIdx < 0 || queryIdx + 1 >= clean.length()) return null;
        String query = clean.substring(queryIdx + 1);
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            String k = eq >= 0 ? pair.substring(0, eq) : pair;
            if (k.equalsIgnoreCase(paramName)) {
                return eq >= 0 ? pair.substring(eq + 1) : "";
            }
        }
        return null;
    }

    public static boolean isBedrockPlayer(@Nullable Player player) {
        if (player == null) return false;
        return isBedrockPlayer(player.getUniqueId());
    }

    private static final java.util.Set<String> autoDetectedPrefixes = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static void registerAutoDetectedPrefix(@NotNull String prefix) {
        if (!prefix.isEmpty()) {
            autoDetectedPrefixes.add(prefix);
        }
    }

    public static boolean isBedrockPlayer(@Nullable UUID uuid) {
        if (uuid == null) return false;
        if (isFloodgatePresent()) {
            try {
                if (FloodgateApi.getInstance().isFloodgatePlayer(uuid)) return true;
            } catch (Throwable ignored) {}
        }
        if (isGeyserPresent()) {
            try {
                Class<?> geyserApiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
                Object api = geyserApiClass.getMethod("api").invoke(null);
                if (api != null) {
                    Object isBedrock = geyserApiClass.getMethod("isBedrockPlayer", UUID.class).invoke(api, uuid);
                    if (Boolean.TRUE.equals(isBedrock)) return true;
                }
            } catch (Throwable ignored) {}
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            for (String prefix : BlockProt.getDefaultConfig().getBedrockUsernamePrefixes()) {
                if (prefix != null && !prefix.isEmpty() && player.getName().startsWith(prefix)) {
                    return true;
                }
            }
            for (String prefix : autoDetectedPrefixes) {
                if (prefix != null && !prefix.isEmpty() && player.getName().startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean shouldUseBedrockForms(@NotNull Player player) {
        if (!isBedrockPlayer(player)) return false;
        return new PlayerSettingsHandler(player).getPreferBedrockForms();
    }

    public static boolean sendForm(@NotNull Player player, @NotNull Form form) {
        return sendForm(player.getUniqueId(), form);
    }

    public static boolean sendForm(@NotNull UUID uuid, @NotNull Form form) {
        if (isFloodgatePresent()) {
            try {
                if (FloodgateApi.getInstance().isFloodgatePlayer(uuid)) {
                    return FloodgateApi.getInstance().sendForm(uuid, form);
                }
            } catch (Throwable t) {
                BlockProtLogger.warn(
                    Translator.get(TranslationKey.CONSOLE__BEDROCK_FORM_SEND_FAILED)
                        .replace("{platform}", "Floodgate")
                        .replace("{error}", String.valueOf(t.getMessage())));
            }
        }
        if (isGeyserPresent()) {
            try {
                Class<?> geyserApiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
                Object api = geyserApiClass.getMethod("api").invoke(null);
                if (api != null) {
                    Method sendFormMethod = geyserApiClass.getMethod("sendForm", UUID.class, Form.class);
                    Object result = sendFormMethod.invoke(api, uuid, form);
                    if (result instanceof Boolean b) return b;
                    return true;
                }
            } catch (Throwable t) {
                BlockProtLogger.warn(
                    Translator.get(TranslationKey.CONSOLE__BEDROCK_FORM_SEND_FAILED)
                        .replace("{platform}", "Geyser")
                        .replace("{error}", String.valueOf(t.getMessage())));
            }
        }
        return false;
    }
}
