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

package de.sean.blockprot.bukkit.bedrock.forms;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.bedrock.BedrockBridge;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.cumulus.util.FormImage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;

/**
 * Native Bedrock admin configuration forms with instant toggle feedback and verified icons.
 */
public final class BedrockAdminConfigForm {

    private BedrockAdminConfigForm() {}

    public static void showCategories(@NotNull Player player) {
        String title = stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__CONFIG));
        String choose = stripColor(Translator.get(TranslationKey.DIALOGS__CHOOSE_OPTION));

        String catPlayers = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_PLAYERS));
        String catBlocks = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_BLOCKS));
        String catEntity = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_ENTITY));
        String catExpiry = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_EXPIRY));
        String catNotifications = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_NOTIFICATIONS));
        String catMaintenance = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_MAINTENANCE));
        String back = stripColor(Translator.get(TranslationKey.DIALOGS__BACK));

        SimpleForm.Builder builder = SimpleForm.builder()
            .title(title)
            .content(choose)
            .button(catPlayers, FormImage.Type.PATH, "textures/ui/FriendsIcon")
            .button(catBlocks, FormImage.Type.PATH, "textures/items/iron_ingot")
            .button(catEntity, FormImage.Type.PATH, "textures/items/name_tag")
            .button(catExpiry, FormImage.Type.PATH, "textures/items/clock_item")
            .button(catNotifications, FormImage.Type.PATH, "textures/ui/World")
            .button(catMaintenance, FormImage.Type.PATH, "textures/ui/settings_glyph_color_2x")
            .button(back, FormImage.Type.PATH, "textures/ui/back_button_default")
            .validResultHandler(response -> {
                int clicked = response.clickedButtonId();
                BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                    if (!player.isOnline()) return;
                    switch (clicked) {
                        case 0 -> showPlayersCategory(player);
                        case 1 -> showBlocksCategory(player);
                        case 2 -> showEntityCategory(player);
                        case 3 -> showExpiryCategory(player);
                        case 4 -> showNotificationsCategory(player);
                        case 5 -> showMaintenanceCategory(player);
                        default -> BedrockAdminMenuForm.show(player);
                    }
                });
            });

        BedrockBridge.sendForm(player, builder.build());
    }

    private static void showPlayersCategory(@NotNull Player player) {
        DefaultConfig cfg = BlockProt.getDefaultConfig();
        String title = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_PLAYERS));

        boolean lockOnPlace = cfg.lockOnPlaceByDefault();
        boolean publicFriend = cfg.publicIsFriendByDefault();
        boolean disableFriends = cfg.isFriendFunctionalityDisabled();
        int maxBlocks = cfg.getMaxLockedBlockCount() != null ? cfg.getMaxLockedBlockCount() : -1;

        String lockLabel = (lockOnPlace ? "[ON] " : "[OFF] ") + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__LOCK_ON_PLACE));
        String publicLabel = (publicFriend ? "[ON] " : "[OFF] ") + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__PUBLIC_IS_FRIEND));
        String friendsLabel = (disableFriends ? "[ON] " : "[OFF] ") + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__DISABLE_FRIENDS));
        String maxBlocksLabel = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__MAX_BLOCKS)) + ": " + maxBlocks;
        String back = stripColor(Translator.get(TranslationKey.DIALOGS__BACK));

        SimpleForm.Builder builder = SimpleForm.builder()
            .title(title)
            .content(title);

        List<Runnable> actions = new ArrayList<>();

        builder.button(lockLabel, FormImage.Type.PATH, lockOnPlace ? "textures/ui/check" : "textures/ui/cancel");
        actions.add(() -> {
            cfg.setLockOnPlaceByDefault(!lockOnPlace);
            showPlayersCategory(player);
        });

        builder.button(publicLabel, FormImage.Type.PATH, publicFriend ? "textures/ui/check" : "textures/ui/cancel");
        actions.add(() -> {
            cfg.setPublicIsFriendByDefault(!publicFriend);
            showPlayersCategory(player);
        });

        builder.button(friendsLabel, FormImage.Type.PATH, disableFriends ? "textures/ui/check" : "textures/ui/cancel");
        actions.add(() -> {
            cfg.setAndSave("disable_friend_functionality", !disableFriends);
            showPlayersCategory(player);
        });

        builder.button(maxBlocksLabel, FormImage.Type.PATH, "textures/ui/pencil_edit_icon");
        actions.add(() -> openIntPrompt(player, stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__MAX_BLOCKS)), maxBlocks, val -> {
            cfg.setPlayerMaxLockedBlockCount(val);
            showPlayersCategory(player);
        }));

        builder.button(back, FormImage.Type.PATH, "textures/ui/back_button_default");
        actions.add(() -> showCategories(player));

        builder.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            if (clicked >= 0 && clicked < actions.size()) {
                BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                    if (!player.isOnline()) return;
                    actions.get(clicked).run();
                });
            }
        });

        BedrockBridge.sendForm(player, builder.build());
    }

    private static void showBlocksCategory(@NotNull Player player) {
        DefaultConfig cfg = BlockProt.getDefaultConfig();
        String title = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_BLOCKS));

        boolean explosions = cfg.shouldProtectLockedBlocksFromExplosions();
        boolean pistons = cfg.shouldBlockProtectedBlockPistonMovement();
        boolean spawn = cfg.shouldRespectSpawnProtection();
        boolean effects = cfg.isLockEffectEnabled();
        boolean sounds = cfg.isLockSoundEnabled();

        String expLabel = (explosions ? "[ON] " : "[OFF] ") + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__PROTECT_EXPLOSIONS));
        String pisLabel = (pistons ? "[ON] " : "[OFF] ") + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__PISTON_MOVEMENT));
        String spawnLabel = (spawn ? "[ON] " : "[OFF] ") + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__SPAWN_PROTECTION));
        String effLabel = (effects ? "[ON] " : "[OFF] ") + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__LOCK_EFFECTS));
        String sndLabel = (sounds ? "[ON] " : "[OFF] ") + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__LOCK_SOUNDS));
        String back = stripColor(Translator.get(TranslationKey.DIALOGS__BACK));

        SimpleForm.Builder builder = SimpleForm.builder()
            .title(title)
            .content(title);

        List<Runnable> actions = new ArrayList<>();

        builder.button(expLabel, FormImage.Type.PATH, explosions ? "textures/ui/check" : "textures/ui/cancel");
        actions.add(() -> {
            cfg.setProtectFromExplosions(!explosions);
            showBlocksCategory(player);
        });

        builder.button(pisLabel, FormImage.Type.PATH, pistons ? "textures/ui/check" : "textures/ui/cancel");
        actions.add(() -> {
            cfg.setBlockPistonMovement(!pistons);
            showBlocksCategory(player);
        });

        builder.button(spawnLabel, FormImage.Type.PATH, spawn ? "textures/ui/check" : "textures/ui/cancel");
        actions.add(() -> {
            cfg.setRespectSpawnProtection(!spawn);
            showBlocksCategory(player);
        });

        builder.button(effLabel, FormImage.Type.PATH, effects ? "textures/ui/check" : "textures/ui/cancel");
        actions.add(() -> {
            cfg.setLockEffects(!effects);
            showBlocksCategory(player);
        });

        builder.button(sndLabel, FormImage.Type.PATH, sounds ? "textures/ui/check" : "textures/ui/cancel");
        actions.add(() -> {
            cfg.setLockSounds(!sounds);
            showBlocksCategory(player);
        });

        builder.button(back, FormImage.Type.PATH, "textures/ui/back_button_default");
        actions.add(() -> showCategories(player));

        builder.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            if (clicked >= 0 && clicked < actions.size()) {
                BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                    if (!player.isOnline()) return;
                    actions.get(clicked).run();
                });
            }
        });

        BedrockBridge.sendForm(player, builder.build());
    }

    private static void showEntityCategory(@NotNull Player player) {
        DefaultConfig cfg = BlockProt.getDefaultConfig();
        String title = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_ENTITY));

        boolean entity = cfg.isEntityProtectionEnabled();
        boolean autoTame = cfg.isEntityProtectionAutoProtectOnTame();
        boolean villager = cfg.isVillagerWorkstationProtectionEnabled();
        int locateSeconds = cfg.getVillagerLocateSeconds();

        String entLabel = (entity ? "[ON] " : "[OFF] ") + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__PROTECTION_ENABLED));
        String tameLabel = (autoTame ? "[ON] " : "[OFF] ") + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__AUTO_PROTECT_TAME));
        String vilLabel = (villager ? "[ON] " : "[OFF] ") + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__WORKSTATION_ENABLED));
        String secLabel = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__VILLAGER_LOCATE_SECONDS)) + ": " + locateSeconds + "s";
        String back = stripColor(Translator.get(TranslationKey.DIALOGS__BACK));

        SimpleForm.Builder builder = SimpleForm.builder()
            .title(title)
            .content(title);

        List<Runnable> actions = new ArrayList<>();

        builder.button(entLabel, FormImage.Type.PATH, entity ? "textures/ui/check" : "textures/ui/cancel");
        actions.add(() -> {
            cfg.setEntityProtectionEnabled(!entity);
            showEntityCategory(player);
        });

        builder.button(tameLabel, FormImage.Type.PATH, autoTame ? "textures/ui/check" : "textures/ui/cancel");
        actions.add(() -> {
            cfg.setAndSave("entity_protection.auto_protect_on_tame", !autoTame);
            showEntityCategory(player);
        });

        builder.button(vilLabel, FormImage.Type.PATH, villager ? "textures/ui/check" : "textures/ui/cancel");
        actions.add(() -> {
            cfg.setAndSave("villager_workstation_protection.enabled", !villager);
            showEntityCategory(player);
        });

        builder.button(secLabel, FormImage.Type.PATH, "textures/ui/pencil_edit_icon");
        actions.add(() -> openIntPrompt(player, stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__VILLAGER_LOCATE_SECONDS)), locateSeconds, val -> {
            cfg.setAndSave("entity_protection.villager_locate_seconds", Math.max(1, Math.min(10, val)));
            showEntityCategory(player);
        }));

        builder.button(back, FormImage.Type.PATH, "textures/ui/back_button_default");
        actions.add(() -> showCategories(player));

        builder.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            if (clicked >= 0 && clicked < actions.size()) {
                BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                    if (!player.isOnline()) return;
                    actions.get(clicked).run();
                });
            }
        });

        BedrockBridge.sendForm(player, builder.build());
    }

    private static void showExpiryCategory(@NotNull Player player) {
        DefaultConfig cfg = BlockProt.getDefaultConfig();
        String title = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_EXPIRY));

        boolean expiry = cfg.isWorldExpiryEnabled();
        int interval = cfg.getWorldExpiryCheckInterval();

        String expLabel = (expiry ? "[ON] " : "[OFF] ") + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__EXPIRY__ENABLED));
        String intLabel = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__EXPIRY__CHECK_INTERVAL)) + ": " + interval + "m";
        String back = stripColor(Translator.get(TranslationKey.DIALOGS__BACK));

        SimpleForm.Builder builder = SimpleForm.builder()
            .title(title)
            .content(title);

        List<Runnable> actions = new ArrayList<>();

        builder.button(expLabel, FormImage.Type.PATH, expiry ? "textures/ui/check" : "textures/ui/cancel");
        actions.add(() -> {
            cfg.setWorldExpiryEnabled(!expiry);
            showExpiryCategory(player);
        });

        builder.button(intLabel, FormImage.Type.PATH, "textures/ui/pencil_edit_icon");
        actions.add(() -> openIntPrompt(player, stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__EXPIRY__CHECK_INTERVAL)), interval, val -> {
            cfg.setAndSave("world_expiry.check_interval_minutes", Math.max(1, val));
            showExpiryCategory(player);
        }));

        builder.button(back, FormImage.Type.PATH, "textures/ui/back_button_default");
        actions.add(() -> showCategories(player));

        builder.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            if (clicked >= 0 && clicked < actions.size()) {
                BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                    if (!player.isOnline()) return;
                    actions.get(clicked).run();
                });
            }
        });

        BedrockBridge.sendForm(player, builder.build());
    }

    private static void showNotificationsCategory(@NotNull Player player) {
        DefaultConfig cfg = BlockProt.getDefaultConfig();
        String title = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_NOTIFICATIONS));

        boolean ops = cfg.shouldNotifyOpOfUpdates();
        boolean owner = cfg.isOwnerNotificationsEnabled();

        String opsLabel = (ops ? "[ON] " : "[OFF] ") + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__NOTIFY_OPS));
        String ownLabel = (owner ? "[ON] " : "[OFF] ") + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__OWNER_ENABLED));
        String back = stripColor(Translator.get(TranslationKey.DIALOGS__BACK));

        SimpleForm.Builder builder = SimpleForm.builder()
            .title(title)
            .content(title);

        List<Runnable> actions = new ArrayList<>();

        builder.button(opsLabel, FormImage.Type.PATH, ops ? "textures/ui/check" : "textures/ui/cancel");
        actions.add(() -> {
            cfg.setNotifyOpOfUpdates(!ops);
            showNotificationsCategory(player);
        });

        builder.button(ownLabel, FormImage.Type.PATH, owner ? "textures/ui/check" : "textures/ui/cancel");
        actions.add(() -> {
            cfg.setAndSave("owner_notifications.enabled", !owner);
            showNotificationsCategory(player);
        });

        builder.button(back, FormImage.Type.PATH, "textures/ui/back_button_default");
        actions.add(() -> showCategories(player));

        builder.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            if (clicked >= 0 && clicked < actions.size()) {
                BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                    if (!player.isOnline()) return;
                    actions.get(clicked).run();
                });
            }
        });

        BedrockBridge.sendForm(player, builder.build());
    }

    private static void showMaintenanceCategory(@NotNull Player player) {
        DefaultConfig cfg = BlockProt.getDefaultConfig();
        String title = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_MAINTENANCE));

        boolean reload = cfg.isAutoReloadEnabled();
        boolean log = cfg.isSessionLogEnabled();
        boolean backup = cfg.isBackupsEnabled();

        String relLabel = (reload ? "[ON] " : "[OFF] ") + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__AUTO_RELOAD));
        String logLabel = (log ? "[ON] " : "[OFF] ") + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__SESSION_LOG));
        String bakLabel = (backup ? "[ON] " : "[OFF] ") + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__BACKUPS));
        String back = stripColor(Translator.get(TranslationKey.DIALOGS__BACK));

        SimpleForm.Builder builder = SimpleForm.builder()
            .title(title)
            .content(title);

        List<Runnable> actions = new ArrayList<>();

        builder.button(relLabel, FormImage.Type.PATH, reload ? "textures/ui/check" : "textures/ui/cancel");
        actions.add(() -> {
            cfg.setAutoReloadConfigs(!reload);
            showMaintenanceCategory(player);
        });

        builder.button(logLabel, FormImage.Type.PATH, log ? "textures/ui/check" : "textures/ui/cancel");
        actions.add(() -> {
            cfg.setSessionLogEnabled(!log);
            showMaintenanceCategory(player);
        });

        builder.button(bakLabel, FormImage.Type.PATH, backup ? "textures/ui/check" : "textures/ui/cancel");
        actions.add(() -> {
            cfg.setBackupsEnabled(!backup);
            showMaintenanceCategory(player);
        });

        builder.button(back, FormImage.Type.PATH, "textures/ui/back_button_default");
        actions.add(() -> showCategories(player));

        builder.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            if (clicked >= 0 && clicked < actions.size()) {
                BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                    if (!player.isOnline()) return;
                    actions.get(clicked).run();
                });
            }
        });

        BedrockBridge.sendForm(player, builder.build());
    }

    private static void openIntPrompt(@NotNull Player player, @NotNull String title, int current, @NotNull java.util.function.IntConsumer onSave) {
        CustomForm.Builder builder = CustomForm.builder()
            .title(title)
            .input(title, "", String.valueOf(current))
            .validResultHandler(response -> {
                CustomFormResponse res = response;
                try {
                    int val = Integer.parseInt(res.asInput(0).trim());
                    BlockProt.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                        if (player.isOnline()) onSave.accept(val);
                    });
                } catch (NumberFormatException ignored) {}
            });

        BedrockBridge.sendForm(player, builder.build());
    }
}
