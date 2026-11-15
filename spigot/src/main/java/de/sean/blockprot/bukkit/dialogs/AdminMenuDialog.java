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
import de.sean.blockprot.bukkit.admin.AdminAction;
import de.sean.blockprot.bukkit.admin.AdminTierManager;
import de.sean.blockprot.bukkit.config.ReloadCoordinator;
import de.sean.blockprot.bukkit.config.ReloadReport;
import de.sean.blockprot.bukkit.tasks.UpdateChecker;
import de.sean.blockprot.util.SemanticVersion;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_CORAL;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_GOLD;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_MINT;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_PURPLE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_BLUE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_GRAY;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class AdminMenuDialog {

    private AdminMenuDialog() {}

    public static void show(@NotNull Player player) {
        show(player, DialogOrigin.NONE);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__TITLE)),
            PASTEL_CORAL, TextDecoration.BOLD
        );

        String lockables = stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__LOCKABLES));
        String reload = stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__RELOAD));
        String update = stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__UPDATE));
        String integrations = stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__INTEGRATIONS));
        String config = stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__CONFIG));
        String tiers = stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__TIERS));
        String stats = stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__STATS));
        String debug = stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__DEBUG));
        String info = stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__INFO));
        String protdel = stripColor(Translator.get(TranslationKey.INVENTORIES__WORLD_PROT_DEL__TITLE));
        String worldExpiry = stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__WORLD_EXPIRY));
        String unlock = stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__UNLOCK));

        SemanticVersion pendingUpdate = UpdateChecker.getAvailableUpdate(BlockProt.getPluginVersion());

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_MENU__HEADER)), SOFT_GRAY)));
        if (pendingUpdate != null) {
            LegacyComponentSerializer legacy = LegacyComponentSerializer.builder()
                .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
            body.add(DialogBodyEntry.text(legacy.deserialize(
                Translator.get(TranslationKey.MESSAGES__UPDATE__OUTDATED)
                    .replace("{version}", pendingUpdate.toString()))));
        }
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CHOOSE)), TextColor.color(0x888888))));

        DialogButton lockablesBtn = new DialogButton("lockables",
            Component.text(stripColor(Translator.get(TranslationKey.ICON__LOCKABLES)) + lockables, NamedTextColor.WHITE),
            tooltip(stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__LOCKABLES)), PASTEL_MINT),
            p -> { if (!AdminTierManager.deny(p, AdminAction.LOCKABLES)) LockablesDialog.show(p, DialogOrigin.ADMIN_MENU); }
        );

        DialogButton configBtn = new DialogButton("config",
            Component.text(stripColor(Translator.get(TranslationKey.ICON__CONFIG)) + config, NamedTextColor.WHITE),
            tooltip(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_MENU__CONFIG_TOOLTIP)), PASTEL_GOLD),
            p -> { if (!AdminTierManager.deny(p, AdminAction.CONFIG)) AdminConfigDialog.show(p, DialogOrigin.ADMIN_MENU); }
        );

        DialogButton tiersBtn = new DialogButton("tiers",
            Component.text(stripColor(Translator.get(TranslationKey.ICON__TIERS)) + tiers, NamedTextColor.WHITE),
            tooltip(stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__TIERS_LORE)), PASTEL_GOLD),
            p -> { if (!AdminTierManager.deny(p, AdminAction.SETROLE)) AdminTiersDialog.show(p, DialogOrigin.ADMIN_MENU); }
        );

        DialogButton worldExpiryBtn = new DialogButton("world_expiry",
            Component.text(stripColor(Translator.get(TranslationKey.ICON__WORLD_EXPIRY)) + worldExpiry, NamedTextColor.WHITE),
            tooltip(stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__WORLD_EXPIRY_LORE)), SOFT_BLUE),
            p -> { if (!AdminTierManager.deny(p, AdminAction.CONFIG)) WorldExpiryDialog.show(p, DialogOrigin.ADMIN_MENU); }
        );

        DialogButton protdelBtn = new DialogButton("protdel",
            Component.text(stripColor(Translator.get(TranslationKey.ICON__DISABLE_ALL)) + protdel, NamedTextColor.WHITE),
            tooltip(protdel, PASTEL_CORAL),
            p -> { if (!AdminTierManager.deny(p, AdminAction.PROTDEL)) ProtdelDialog.show(p, null, DialogOrigin.ADMIN_MENU); }
        );

        DialogButton unlockBtn = new DialogButton("unlock",
            Component.text(stripColor(Translator.get(TranslationKey.ICON__UNLOCK)) + unlock, NamedTextColor.WHITE),
            tooltip(stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__UNLOCK_LORE)), PASTEL_CORAL),
            p -> { if (!AdminTierManager.deny(p, AdminAction.UNLOCK)) AdminConfigValueDialog.openText(
                p,
                "unlock_player",
                stripColor(Translator.get(TranslationKey.MESSAGES__BP_UNLOCK_USAGE)),
                "",
                targetName -> {
                    if (targetName == null || targetName.isBlank()) {
                        return stripColor(Translator.get(TranslationKey.MESSAGES__BP_UNLOCK_USAGE));
                    }
                    return null;
                },
                targetName -> UnlockDialog.show(p, DialogOrigin.ADMIN_MENU, targetName.trim(), 0),
                () -> show(p, backOrigin)
            ); }
        );

        DialogButton reloadBtn = new DialogButton("reload",
            Component.text(stripColor(Translator.get(TranslationKey.ICON__RELOAD)) + reload, NamedTextColor.WHITE),
            tooltip(stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__RELOAD)), PASTEL_MINT),
            p -> {
                if (AdminTierManager.deny(p, AdminAction.RELOAD)) return;
                ReloadReport report = ReloadCoordinator.commitCommand();
                p.sendMessage(Component.text(
                    report.isSuccess()
                        ? Translator.get(TranslationKey.MESSAGES__ADMIN_RELOAD_DONE)
                        : Translator.get(TranslationKey.MESSAGES__ADMIN_RELOAD_FAILED)
                            .replace("{error}", String.valueOf(report.getErrorMessage())),
                    report.isSuccess() ? PASTEL_MINT : PASTEL_CORAL));
            }
        );

        Component updateLabel = pendingUpdate != null
            ? Component.text(stripColor(Translator.get(TranslationKey.ICON__SEARCH)) + update + " (v" + pendingUpdate + ")",
                PASTEL_GOLD, TextDecoration.BOLD)
            : Component.text(stripColor(Translator.get(TranslationKey.ICON__SEARCH)) + update, NamedTextColor.WHITE);
        Component updateTooltip = pendingUpdate != null
            ? tooltip(Translator.get(TranslationKey.DIALOGS__UPDATE__OUTDATED) + " (v" + pendingUpdate + ")", PASTEL_GOLD)
            : tooltip(stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__UPDATE)), SOFT_BLUE);
        DialogButton updateBtn = new DialogButton("update",
            updateLabel,
            updateTooltip,
            p -> { if (!AdminTierManager.deny(p, AdminAction.UPDATE)) UpdateDialog.show(p, DialogOrigin.ADMIN_MENU); }
        );

        DialogButton integrationsBtn = new DialogButton("integrations",
            Component.text(stripColor(Translator.get(TranslationKey.ICON__INTEGRATIONS)) + integrations, NamedTextColor.WHITE),
            tooltip(stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__INTEGRATIONS)), SOFT_BLUE),
            p -> { if (!AdminTierManager.deny(p, AdminAction.INTEGRATIONS)) IntegrationsDialog.show(p, DialogOrigin.ADMIN_MENU); }
        );

        DialogButton statsBtn = new DialogButton("stats",
            Component.text(stripColor(Translator.get(TranslationKey.ICON__STATS)) + stats, NamedTextColor.WHITE),
            tooltip(stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__STATS)), PASTEL_PURPLE),
            p -> { if (!AdminTierManager.deny(p, AdminAction.INFO)) StatsDialog.show(p, DialogOrigin.ADMIN_MENU); }
        );

        DialogButton debugBtn = new DialogButton("debug",
            Component.text(stripColor(Translator.get(TranslationKey.ICON__DEBUG)) + debug, NamedTextColor.WHITE),
            tooltip(stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__DEBUG)), PASTEL_CORAL),
            p -> {
                if (AdminTierManager.deny(p, AdminAction.DEBUG)) return;
                BlockProt.getInstance().getLogger().info("/bp debug run from dialog");
                p.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                    Translator.get(TranslationKey.MESSAGES__DEBUG__RUNNING_DIAGNOSTICS)));
                de.sean.blockprot.bukkit.commands.DebugCommand.run(p);
            }
        );

        DialogButton infoBtn = new DialogButton("info",
            Component.text(stripColor(Translator.get(TranslationKey.ICON__INFO)) + info, NamedTextColor.WHITE),
            tooltip(stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__INFO)), SOFT_BLUE),
            p -> { if (!AdminTierManager.deny(p, AdminAction.INFO)) InfoDialog.show(p, DialogOrigin.ADMIN_MENU); }
        );

        DialogButton aboutBtn = new DialogButton("about",
            Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__ABOUT)), NamedTextColor.WHITE),
            tooltip(stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__ABOUT)), PASTEL_GOLD),
            p -> AboutDialog.show(p, DialogOrigin.ADMIN_MENU)
        );

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton exitBtn = new DialogButton("exit",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            originHint(exitOrigin),
            originBack(player, exitOrigin)
        );

        List<DialogButton> actions = new ArrayList<>();
        actions.add(lockablesBtn);
        actions.add(configBtn);
        if (BlockProt.getDefaultConfig().isAdminTiersEnabled()) {
            actions.add(tiersBtn);
        }
        if (BlockProt.getDefaultConfig().isWorldExpiryEnabled()) {
            actions.add(worldExpiryBtn);
        }
        actions.add(protdelBtn);
        actions.add(unlockBtn);
        actions.add(integrationsBtn);
        actions.add(statsBtn);
        actions.add(infoBtn);
        actions.add(reloadBtn);
        actions.add(updateBtn);
        actions.add(debugBtn);
        actions.add(aboutBtn);
        bridge.showMultiAction(player, title, body, actions, exitBtn, 2);
    }

    private static Component tooltip(String description, TextColor accent) {
        return Component.join(
            JoinConfiguration.newlines(),
            Component.text(description, accent),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_TO_OPEN)), TextColor.color(0x888888))
        );
    }

    static Component originHint(DialogOrigin origin) {
        switch (origin) {
            case ADMIN_MENU: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_ADMIN_MENU)), TextColor.color(0x888888));
            case USER_MENU: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_USER_MENU)), TextColor.color(0x888888));
            default: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888));
        }
    }

    static DialogButton.DialogClickHandler originBack(Player player, DialogOrigin origin) {
        switch (origin) {
            case ADMIN_MENU: return p -> AdminMenuDialog.show(p);
            case USER_MENU: return p -> UserMenuDialog.show(p);
            default: return null;
        }
    }
}
