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
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.admin.AdminAction;
import de.sean.blockprot.bukkit.admin.AdminTier;
import de.sean.blockprot.bukkit.admin.AdminTierManager;
import de.sean.blockprot.bukkit.commands.BlockProtCommand;
import de.sean.blockprot.bukkit.integrations.PluginIntegration;
import de.sean.blockprot.bukkit.integrations.ViaVersionIntegration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CommandsAndPermissionsDiagnostics {

    private CommandsAndPermissionsDiagnostics() {
    }

    public static void run(@NotNull DiagnosticsContext ctx) {
        DiagnosticsContext.runDomain("6/10", "COMMANDS, PERMISSIONS & INTEGRATIONS");
        checkIntegrations(ctx);
        checkCommandsRegistered(ctx);
        checkAdminTiers(ctx);
    }

    private static void checkIntegrations(@NotNull DiagnosticsContext ctx) {
        try {
            List<PluginIntegration> integrations = BlockProt.getInstance().getIntegrations();
            if (integrations == null || integrations.isEmpty()) {
                BlockProtLogger.pass("Integrations: none registered");
                ctx.pass();
                return;
            }
            int active = 0;
            BlockProtLogger.subGroup("Integrations (" + integrations.size() + " registered):");
            for (PluginIntegration integration : integrations) {
                String name = integration.getClass().getSimpleName();
                boolean enabled = integration.isEnabled();
                if (enabled) active++;

                if (integration instanceof ViaVersionIntegration via) {
                    BlockProtLogger.passSub(name + ": " + via.getDetailedStatus());
                } else {
                    org.bukkit.plugin.Plugin plugin = integration.getPlugin();
                    String ver = "unknown";
                    if (plugin != null) {
                        try {
                            ver = plugin.getPluginMeta().getVersion();
                        } catch (NoSuchMethodError err) {
                            @SuppressWarnings("deprecation")
                            String fallback = plugin.getDescription().getVersion();
                            ver = fallback;
                        }
                    }
                    if (enabled) {
                        BlockProtLogger.passSub(name + ": ACTIVE v" + ver);
                    } else {
                        BlockProtLogger.passSub(name + ": INACTIVE (plugin not found or disabled)");
                    }
                }
            }
            ctx.pass();
        } catch (Exception e) {
            BlockProtLogger.fail("Integrations", e.getMessage());
            ctx.fail();
        }
    }

    private static void checkCommandsRegistered(@NotNull DiagnosticsContext ctx) {
        BlockProtLogger.subGroup("Commands & permissions:");
        org.bukkit.command.PluginCommand cmd = Bukkit.getPluginCommand("blockprot");
        if (cmd == null) {
            BlockProtLogger.failSub("blockprot command", "not registered");
            ctx.fail();
        } else if (cmd.getExecutor() == null) {
            BlockProtLogger.failSub("blockprot command", "has no executor");
            ctx.fail();
        } else {
            BlockProtLogger.passSub("blockprot registered, executor="
                + cmd.getExecutor().getClass().getSimpleName());
            ctx.touchScreen(DiagnosticsContext.COMMANDS_PACKAGE, "BlockProtCommand");
            ctx.pass();
        }

        try (var in = BlockProt.getInstance().getResource("plugin.yml")) {
            if (in == null) throw new IllegalStateException("plugin.yml resource missing");
            var cfg = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
            var permissions = cfg.getConfigurationSection("permissions");
            int missing = 0;
            for (Permissions perm : Permissions.values()) {
                String key = perm.key();
                if (permissions == null || !permissions.contains(key)) {
                    missing++;
                    BlockProtLogger.failSub("Permission node", key + " not declared in plugin.yml");
                }
            }
            if (missing == 0) {
                BlockProtLogger.passSub("Permissions: all " + Permissions.values().length + " nodes declared in plugin.yml");
                ctx.pass();
            } else {
                ctx.fail();
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("Commands", "plugin.yml read failed: " + e.getMessage());
            ctx.fail();
        }

        String[] commandClasses = {
            "UserMenuCommand", "AdminMenuCommand",
            "HelpCommand", "SettingsCommand", "FriendsAddAllCommand", "StatisticsCommand",
            "TransferCommand", "AboutCommand", "HintsCommand", "InfoCommand",
            "ReloadCommand", "UpdateCommand", "IntegrationsCommand", "DebugCommand",
            "AdminUnlockCommand", "WorldProtDeleteCommand", "LogsCommand", "LockablesCommand", "RecommendedCommand",
            "TiersCommand"
        };
        Set<String> wired = new HashSet<>();
        for (String name : commandClasses) {
            try {
                Class.forName(BlockProtCommand.class.getPackageName() + "." + name);
                ctx.touch(BlockProtCommand.class.getPackageName() + "." + name);
                wired.add(name);
            } catch (Throwable e) {
                BlockProtLogger.failSub("Command class", name + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
                ctx.fail();
            }
        }
        if (wired.size() == commandClasses.length) {
            BlockProtLogger.passSub("Command classes: " + wired.size() + "/" + commandClasses.length
                + " present and loadable");
            ctx.pass();
        } else {
            BlockProtLogger.failSub("Command classes", wired.size() + "/" + commandClasses.length
                + " present and loadable (MISSING SOME)");
            ctx.fail();
        }

        String[] integrationClasses = {
            "TownyIntegration", "PlaceholderAPIIntegration", "ViaVersionIntegration",
            "WorldGuardIntegration", "LandsPluginIntegration", "ClaimChunkIntegration",
            "ResidenceIntegration", "GriefPreventionIntegration",
            "GeyserIntegration", "FloodgateIntegration"
        };
        Set<String> regNames = new HashSet<>();
        for (PluginIntegration integration : BlockProt.getInstance().getIntegrations()) {
            regNames.add(integration.getClass().getSimpleName());
        }
        int regMissing = 0;
        for (String name : integrationClasses) {
            if (!regNames.contains(name)) {
                regMissing++;
                BlockProtLogger.failSub("Integration wiring", name + " not in BlockProt.onLoad() integration list");
            }
        }
        if (regMissing == 0) {
            BlockProtLogger.passSub("Integration wiring: all " + integrationClasses.length
                + " integration classes constructed in BlockProt.onLoad()");
            ctx.pass();
        } else {
            ctx.fail();
        }
    }

    private static void checkAdminTiers(@NotNull DiagnosticsContext ctx) {
        try {
            boolean parsingOk = AdminTier.fromString("t1") == AdminTier.T1
                && AdminTier.fromString("t2") == AdminTier.T2
                && AdminTier.fromString("t3") == AdminTier.T3
                && AdminTier.fromString("owner") == AdminTier.OWNER
                && AdminTier.fromString("custom") == AdminTier.CUSTOM
                && AdminTier.fromString("user") == AdminTier.NONE
                && AdminTier.fromString("normal") == AdminTier.NONE
                && AdminTier.fromString("none") == AdminTier.NONE
                && AdminTier.fromString("unknown_val") == AdminTier.NONE;
            if (!parsingOk) {
                BlockProtLogger.fail("AdminTier parsing", "fromString failed to resolve expected tier");
                ctx.fail();
                return;
            }

            boolean hierarchyOk = AdminTier.OWNER.includes(AdminTier.T3)
                && AdminTier.T3.includes(AdminTier.T2)
                && AdminTier.T2.includes(AdminTier.T1)
                && AdminTier.T1.includes(AdminTier.T1)
                && !AdminTier.T1.includes(AdminTier.T2)
                && !AdminTier.NONE.includes(AdminTier.T1);
            if (!hierarchyOk) {
                BlockProtLogger.fail("AdminTier hierarchy", "includes() hierarchy check failed");
                ctx.fail();
                return;
            }

            boolean actionsOk = AdminAction.INFO.getMinimumTier() == AdminTier.T1
                && AdminAction.TELEPORT.getMinimumTier() == AdminTier.T1
                && AdminAction.LOGS.getMinimumTier() == AdminTier.T1
                && AdminAction.BREAK.getMinimumTier() == AdminTier.T2
                && AdminAction.UNLOCK.getMinimumTier() == AdminTier.T2
                && AdminAction.LOCKABLES.getMinimumTier() == AdminTier.T2
                && AdminAction.CONTAINER_BYPASS.getMinimumTier() == AdminTier.T2
                && AdminAction.PROTDEL.getMinimumTier() == AdminTier.T3
                && AdminAction.CONFIG.getMinimumTier() == AdminTier.T3
                && AdminAction.DEBUG.getMinimumTier() == AdminTier.T3
                && AdminAction.RELOAD.getMinimumTier() == AdminTier.OWNER
                && AdminAction.UPDATE.getMinimumTier() == AdminTier.OWNER
                && AdminAction.INTEGRATIONS.getMinimumTier() == AdminTier.OWNER
                && AdminAction.RECOMMENDED.getMinimumTier() == AdminTier.OWNER
                && AdminAction.SETROLE.getMinimumTier() == AdminTier.OWNER;
            if (!actionsOk) {
                BlockProtLogger.fail("AdminAction mappings", "unexpected minimum tier mapping");
                ctx.fail();
                return;
            }

            boolean consolePerms = true;
            for (AdminAction action : AdminAction.values()) {
                if (!AdminTierManager.hasPermission(Bukkit.getConsoleSender(), action)) {
                    consolePerms = false;
                    break;
                }
            }
            if (!consolePerms) {
                BlockProtLogger.fail("AdminTierManager", "Console sender denied on admin action");
                ctx.fail();
                return;
            }

            UUID testUuid = UUID.randomUUID();
            AdminTierManager.setPlayerRole(testUuid, AdminTier.CUSTOM, EnumSet.of(AdminAction.RELOAD));
            AdminTierManager.save();
            AdminTierManager.load();
            AdminTier loadedRole = AdminTierManager.getRole(testUuid);
            boolean allRolesOk = AdminTierManager.getAllConfiguredRoles().containsKey(testUuid);
            boolean flagsOk = AdminTierManager.getCustomFlags(testUuid).contains(AdminAction.RELOAD);
            boolean toggledOn = AdminTierManager.toggleCustomFlag(testUuid, AdminAction.DEBUG);
            boolean toggledOff = !AdminTierManager.toggleCustomFlag(testUuid, AdminAction.DEBUG);
            if (loadedRole != AdminTier.CUSTOM || !allRolesOk || !flagsOk || !toggledOn || !toggledOff) {
                BlockProtLogger.fail("AdminTierManager", "getRole / customFlags roundtrip mismatch for test entry");
                ctx.fail();
                return;
            }
            AdminTierManager.setPlayerRole(testUuid, AdminTier.NONE, null);
            AdminTierManager.save();
            if (AdminTierManager.getRole(testUuid) != AdminTier.NONE) {
                BlockProtLogger.fail("AdminTierManager", "Role not removed after set to NONE");
                ctx.fail();
                return;
            }

            AdminTier currentTier = ctx.player() != null ? AdminTierManager.getPlayerTier(ctx.player()) : AdminTier.NONE;
            boolean anyAdmin = ctx.player() != null ? AdminTierManager.hasAnyAdminPermission(ctx.player()) : true;
            String callerDesc = ctx.player() != null
                ? "callerTier=" + currentTier.getIdentifier() + ", hasAnyAdmin=" + anyAdmin
                : "headless verification";

            BlockProtLogger.pass("Admin tiers: parsing, hierarchy, actions (15), console bypass, and storage roundtrip OK ("
                + callerDesc + ")");
            ctx.pass();
        } catch (Exception e) {
            BlockProtLogger.fail("Admin tiers", e.getMessage());
            ctx.fail();
        }
    }
}
