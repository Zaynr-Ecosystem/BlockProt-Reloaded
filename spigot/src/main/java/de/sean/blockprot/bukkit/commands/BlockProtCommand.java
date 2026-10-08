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

package de.sean.blockprot.bukkit.commands;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.admin.AdminTierManager;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Main dispatcher for /blockprot (alias /bp).
 *
 * <p>Visibility rules apply to player senders only; console always has access to
 * every CLI and admin subcommand regardless of {@code use_menus}, since the GUI
 * subcommands ({@code user}, {@code admin}) require a player and have no console
 * equivalent.
 * <ul>
 *   <li><b>use_menus=true</b>, player sender: tab-complete shows only {@code user}
 *       and {@code admin}. Any other subcommand is blocked with a usage hint.</li>
 *   <li><b>use_menus=false</b>, player sender: tab-complete shows all CLI commands
 *       (help, settings, friends, stats, etc.). {@code user} and {@code admin} are
 *       hidden and blocked.</li>
 *   <li>Console sender: always sees and can run all CLI and admin subcommands.
 *       {@code user}/{@code admin} are blocked with a player-only message.</li>
 * </ul>
 */
public final class BlockProtCommand implements TabExecutor {

    private static final Map<String, CommandExecutor> GUI_COMMANDS   = new LinkedHashMap<>();
    private static final Map<String, CommandExecutor> CLI_COMMANDS   = new LinkedHashMap<>();
    private static final Map<String, CommandExecutor> ADMIN_COMMANDS = new LinkedHashMap<>();
    private static final Map<String, CommandExecutor> ALL_COMMANDS   = new LinkedHashMap<>();

    static {
        gui("user",         new UserMenuCommand());
        gui("admin",        new AdminMenuCommand());

        cli("help",         new HelpCommand());
        cli("settings",     new SettingsCommand());
        cli("friends",      new FriendsAddAllCommand());
        cli("stats",        new StatisticsCommand());
        cli("transferall",  new TransferCommand());
        cli("about",        new AboutCommand());
        cli("disablehints", new HintsCommand());
        cli("info",         new InfoCommand());
        cli("reload",       new ReloadCommand());
        cli("update",       new UpdateCommand());
        cli("integrations", new IntegrationsCommand());
        cli("debug",        new DebugCommand());
        cli("unlock",       new AdminUnlockCommand());
        cli("protdel",      new WorldProtDeleteCommand());
        cli("logs",         new LogsCommand());

        admin("lockables",   new LockablesCommand());
        admin("recommended", new RecommendedCommand());
        admin("tiers",       new TiersCommand());
    }

    private static void gui(String name, CommandExecutor exec) {
        GUI_COMMANDS.put(name, exec);
        ALL_COMMANDS.put(name, exec);
    }

    private static void cli(String name, CommandExecutor exec) {
        CLI_COMMANDS.put(name, exec);
        ALL_COMMANDS.put(name, exec);
    }

    private static void admin(String name, CommandExecutor exec) {
        ADMIN_COMMANDS.put(name, exec);
        ALL_COMMANDS.put(name, exec);
    }

    @NotNull
    public static Map<String, CommandExecutor> getActiveCommands(@NotNull CommandSender sender) {
        boolean menusEnabled = !BlockProt.getDefaultConfig().areExtraCommandsEnabled();
        boolean isAdmin = AdminTierManager.hasAnyAdminPermission(sender);

        if (menusEnabled) {
            if (sender instanceof Player) {
                Map<String, CommandExecutor> cmds = new LinkedHashMap<>();
                cmds.put("user", GUI_COMMANDS.get("user"));
                if (isAdmin) {
                    cmds.put("admin", GUI_COMMANDS.get("admin"));
                }
                return Collections.unmodifiableMap(cmds);
            }
            Map<String, CommandExecutor> consoleCmds = new LinkedHashMap<>();
            consoleCmds.putAll(CLI_COMMANDS);
            consoleCmds.putAll(ADMIN_COMMANDS);
            return Collections.unmodifiableMap(consoleCmds);
        }

        Map<String, CommandExecutor> cli = new LinkedHashMap<>();
        cli.putAll(CLI_COMMANDS);
        if (isAdmin || !(sender instanceof Player)) {
            cli.putAll(ADMIN_COMMANDS);
        }
        return Collections.unmodifiableMap(cli);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        try {
            boolean menusEnabled = !BlockProt.getDefaultConfig().areExtraCommandsEnabled();
            boolean playerMenuMode = menusEnabled && sender instanceof Player;

            if (args.length == 0) {
                if (playerMenuMode) {
                    CommandExecutor exec = GUI_COMMANDS.get("user");
                    return exec != null && exec.onCommand(sender, command, label, args);
                }
                CommandExecutor help = CLI_COMMANDS.get("help");
                return help != null && help.onCommand(sender, command, label, args);
            }

            String sub = args[0].toLowerCase(Locale.ROOT);

            if (!menusEnabled && GUI_COMMANDS.containsKey(sub)) {
                ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__CMD_USAGE_CLI_GUI_ONLY));
                return true;
            }

            Map<String, CommandExecutor> active = getActiveCommands(sender);
            CommandExecutor exec = active.get(sub);

            if (exec != null) {
                if (exec.canUseCommand(sender)) {
                    return exec.onCommand(sender, command, label, args);
                }
                ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                return true;
            }

            if (playerMenuMode) {
                ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__CMD_USAGE_MENUS));
            } else {
                ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__CMD_USAGE_CLI));
            }
            return true;
        } catch (Throwable t) {
            String fullCmd = "/" + label + (args.length > 0 ? " " + String.join(" ", args) : "");
            String senderInfo = (sender instanceof Player p)
                ? p.getName() + " (" + p.getUniqueId() + ")"
                : sender.getName() + " (Console)";
            BlockProtLogger.error("Exception occurred while executing command '" + fullCmd + "' by " + senderInfo + ": " + t.getMessage(), t);
            ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__ERROR_PREFIX)
                + t.getClass().getSimpleName() + ": " + (t.getMessage() != null ? t.getMessage() : "Unknown error"));
            return true;
        }
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                               @NotNull String alias, @NotNull String[] args) {
        Map<String, CommandExecutor> active = getActiveCommands(sender);

        if (args.length > 1) {
            CommandExecutor exec = active.get(args[0].toLowerCase(Locale.ROOT));
            if (exec != null && exec.canUseCommand(sender)) {
                List<String> sub = exec.onTabComplete(sender, command, alias, args);
                return sub != null ? sub : Collections.emptyList();
            }
            return Collections.emptyList();
        }

        String partial = args.length == 1 ? args[0].toLowerCase(Locale.ROOT) : "";
        List<String> result = new ArrayList<>();
        for (var entry : active.entrySet()) {
            if (entry.getKey().startsWith(partial) && entry.getValue().canUseCommand(sender))
                result.add(entry.getKey());
        }
        return result;
    }
}
