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

import de.sean.blockprot.bukkit.audit.AuditLogger;
import de.sean.blockprot.bukkit.commands.BlockProtCommand;
import de.sean.blockprot.bukkit.config.BlocksFormatConverter;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.config.IntegrationConfig;
import de.sean.blockprot.bukkit.config.LangConfig;
import de.sean.blockprot.bukkit.config.WorldsConfig;
import de.sean.blockprot.bukkit.integrations.*;
import de.sean.blockprot.bukkit.lifecycle.LegacyMigrationManager;
import de.sean.blockprot.bukkit.listeners.*;
import de.sean.blockprot.bukkit.metrics.IntegrationBarChart;
import de.sean.blockprot.bukkit.metrics.LanguagePieChart;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.storage.HybridDatabase;
import de.sean.blockprot.bukkit.tasks.ConfigFileWatcher;
import de.sean.blockprot.bukkit.tasks.BackupTask;
import de.sean.blockprot.bukkit.tasks.InactivityCleanupTask;
import de.sean.blockprot.bukkit.tasks.WorldExpiryTask;
import de.sean.blockprot.bukkit.tasks.UpdateChecker;
import de.sean.blockprot.util.SemanticVersion;
import com.tcoded.folialib.FoliaLib;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.enginehub.squirrelid.cache.ProfileCache;
import org.enginehub.squirrelid.cache.SQLiteCache;
import org.enginehub.squirrelid.resolver.ProfileService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class BlockProt extends JavaPlugin {

    public static final int pluginId = 31548; // BlockProt Reloaded on bStats
    public static final String defaultLanguageFile = "translations_en.yml";

    @Nullable private static BlockProt instance;
    @Nullable private static DefaultConfig defaultConfig = null;

    private final ArrayList<PluginIntegration> integrations = new ArrayList<>();

    @Nullable private static SQLiteCache playerProfileCache = null;
    @Nullable private static ProfileService playerProfileService = null;
    @Nullable private static WorldsConfig worldsConfig = null;
    @Nullable private static AuditLogger auditLogger = null;
    @Nullable private static HybridDatabase hybridDatabase = null;
    @Nullable private ConfigFileWatcher fileWatcher = null;

    private static final LegacyMigrationManager legacyMigrationManager = new LegacyMigrationManager();

    @Nullable private static String pluginVersion = null;
    @Nullable private static List<String> pluginAuthors = null;

    @Nullable private static FoliaLib foliaLib = null;

    private Metrics metrics;

    public static boolean isFirstStartThisSession() {
        return legacyMigrationManager.isFirstStartThisSession();
    }

    public static LegacyMigrationManager getMigrationManager() {
        return legacyMigrationManager;
    }

    public static boolean hasFoliaLib() {
        return foliaLib != null;
    }

    @NotNull
    public static FoliaLib getFoliaLib() {
        assert foliaLib != null;
        return foliaLib;
    }

    @NotNull
    public static BlockProt getInstance() {
        assert instance != null;
        return instance;
    }

    @Nullable
    public static BlockProt getInstanceOrNull() {
        return instance;
    }

    @NotNull
    public static DefaultConfig getDefaultConfig() throws AssertionError {
        assert defaultConfig != null : "default config should not be null.";
        return defaultConfig;
    }

    @Nullable
    public ConfigFileWatcher getFileWatcher() {
        return fileWatcher;
    }

    public List<PluginIntegration> getIntegrations() {
        return Collections.unmodifiableList(integrations);
    }

    private boolean integrationsLoaded = false;
    private boolean integrationsEnabled = false;

    @NotNull public static String getPluginVersion() { return pluginVersion != null ? pluginVersion : ""; }
    @NotNull public static List<String> getPluginAuthors() { return pluginAuthors != null ? pluginAuthors : List.of(); }

    @NotNull public static ProfileCache    getProfileCache()   { assert playerProfileCache   != null; return playerProfileCache; }
    @NotNull public static ProfileService  getProfileService() { assert playerProfileService != null; return playerProfileService; }
    @Nullable public static WorldsConfig   getWorldsConfig()   { return worldsConfig; }
    @Nullable public static AuditLogger    getAuditLogger()    { return auditLogger; }
    @Nullable public static HybridDatabase getHybridDatabase() { return hybridDatabase; }

    @Override
    @SuppressWarnings("deprecation")
    public void onLoad() {
        instance = this;
        pluginVersion = this.getDescription().getVersion();
        pluginAuthors = this.getDescription().getAuthors();
        if (isUnsupportedMinecraft()) return;
        try {
            File cacheFile = new File(this.getDataFolder(), "blockprot_usercache.sqlite");
            if (!cacheFile.getParentFile().exists()) cacheFile.getParentFile().mkdirs();
            playerProfileCache   = new SQLiteCache(cacheFile);
            playerProfileService = new CachedProfileService(playerProfileCache);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open SQLite connection to usercache database", e);
        }
        try { registerIntegration(new TownyIntegration());          } catch (NoClassDefFoundError ignored) {}
        try { registerIntegration(new PlaceholderAPIIntegration()); } catch (NoClassDefFoundError ignored) {}
        try { registerIntegration(new ViaVersionIntegration());     } catch (NoClassDefFoundError ignored) {}
        try { registerIntegration(new WorldGuardIntegration());     } catch (NoClassDefFoundError ignored) {}
        try { registerIntegration(new LandsPluginIntegration());    } catch (NoClassDefFoundError ignored) {}
        try { registerIntegration(new ClaimChunkIntegration());    } catch (NoClassDefFoundError ignored) {}
        try { registerIntegration(new ResidenceIntegration());     } catch (NoClassDefFoundError ignored) {}
        try { registerIntegration(new GriefPreventionIntegration()); } catch (NoClassDefFoundError ignored) {}
        try { registerIntegration(new GeyserIntegration());         } catch (NoClassDefFoundError ignored) {}
        try { registerIntegration(new FloodgateIntegration());      } catch (NoClassDefFoundError ignored) {}
        for (PluginIntegration integration : integrations) {
            try { integration.load(); } catch (NoClassDefFoundError ignored) {}
        }
        integrationsLoaded = true;
    }

    private static boolean isUnsupportedMinecraft() {
        Edition edition = Edition.current();
        return !edition.isNewerThanRange(VersionCompat.MAJOR, VersionCompat.MINOR, VersionCompat.PATCH)
            && !edition.supports(VersionCompat.MAJOR, VersionCompat.MINOR, VersionCompat.PATCH);
    }

    private static @NotNull String unsupportedMinecraftMessage(@NotNull Edition edition) {
        return Edition.recommendedFor(VersionCompat.MAJOR, VersionCompat.MINOR, VersionCompat.PATCH)
            .map(recommended -> editionMessage(TranslationKey.CONSOLE__MINECRAFT_UNSUPPORTED, edition, recommended))
            .orElseGet(() -> Translator.get(TranslationKey.CONSOLE__MINECRAFT_UNSUPPORTED_NONE)
                .replace("{mc}", VersionCompat.getVersionString())
                .replace("{guide}", Edition.guideUrl()));
    }

    private static @NotNull String editionMessage(@NotNull TranslationKey key, @NotNull Edition edition,
                                                  @NotNull Edition recommended) {
        return Translator.get(key)
            .replace("{edition}", edition.displayName())
            .replace("{version}", getPluginVersion())
            .replace("{mc}", VersionCompat.getVersionString())
            .replace("{recommended}", recommended.displayName())
            .replace("{range}", recommended.rangeLabel())
            .replace("{edition_range}", edition.rangeLabel())
            .replace("{url}", recommended.downloadUrl())
            .replace("{guide}", Edition.guideUrl());
    }

    @Override
    public void onEnable() {
        final Edition edition = Edition.current();
        boolean newerThanRange = edition.isNewerThanRange(VersionCompat.MAJOR, VersionCompat.MINOR, VersionCompat.PATCH);
        boolean unsupportedVersion = isUnsupportedMinecraft();
        if (isRunningCraftBukkit() || unsupportedVersion) {
            this.saveDefaultConfig();
            this.reloadConfig();
            defaultConfig = new DefaultConfig(this.getConfig(), this.getDataFolder());
            Translator.resetTranslations();
            try {
                InputStream s = this.getResource("lang/" + defaultLanguageFile);
                if (s != null) {
                    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(
                        new java.io.BufferedReader(new java.io.InputStreamReader(s, StandardCharsets.UTF_8)));
                    Translator.loadFromConfigs(cfg, cfg);
                }
            } catch (Exception ignored) {}
            final var message = unsupportedVersion
                ? unsupportedMinecraftMessage(edition)
                : Translator.get(TranslationKey.CONSOLE__CRAFTBUKKIT_UNSUPPORTED);
            getLogger().severe(message);
            getServer().getPluginManager().registerEvents(new ErrorEventListener(message), this);
            return;
        }

        legacyMigrationManager.migrateFromLegacyFolders(this);
        legacyMigrationManager.migrateLegacySqliteFile(this);

        BlockProtConsole.beginStartup(this.getLogger());

        foliaLib = new FoliaLib(this);
        foliaLib.getScheduler().runAsync(task -> new UpdateChecker(BlockProt.getPluginVersion(), () -> {}).run());
        MinecraftVersion.disableUpdateCheck();
        this.saveDefaultConfig();
        legacyMigrationManager.migrateOldLockableListsFromConfigYml(this);
        saveResourceSilent("blocks.yml", false);
        this.reloadConfigAndTranslations();
        legacyMigrationManager.flushMigrationLog(this);

        if (newerThanRange) {
            BlockProtConsole.warn(editionMessage(TranslationKey.CONSOLE__MINECRAFT_NEWER_AVAILABLE, edition,
                Edition.recommendedFor(VersionCompat.MAJOR, VersionCompat.MINOR, VersionCompat.PATCH).orElse(Edition.BPR)));
        }

        boolean sessionLogEnabled = defaultConfig.isSessionLogEnabled();
        BlockProtLogger.init(this.getDataFolder(), sessionLogEnabled);
        String version = BlockProt.getPluginVersion();

        BlockProtLogger.log("Startup: BlockProt v" + version);
        BlockProtLogger.log("Server: " + Bukkit.getVersion());
        BlockProtLogger.log("Runtime: " + VersionCompat.getDiagnosticString());
        if (VersionCompat.is26Family()) {
            BlockProtLogger.log("Version scheme: " + VersionCompat.MAJOR + ".x year-based detected.");
        }

        announceBuildChannel(version);

        BlockProtConsole.bootStatus(
            Translator.get(TranslationKey.CONSOLE__BOOT_CONFIGURATION),
            true,
            Translator.get(TranslationKey.CONSOLE__BOOT_LOADED));

        StatHandler.enable();

        boolean experimentalBuild = false;
        try { experimentalBuild = new SemanticVersion(version).isPreRelease(); } catch (Exception ignored) {}
        boolean editionChanged = BackupTask.currentTransition(this.getDataFolder()).changed();
        if ((legacyMigrationManager.isMigrationPerformed() || experimentalBuild || editionChanged) && defaultConfig.isBackupsEnabled()) {
            new BackupTask(this.getDataFolder()).run();
        }
        BackupTask.recordCurrentEdition(this.getDataFolder());
        saveResourceSilent("worlds.yml", false);
        if (defaultConfig.isPerWorldsConfigEnabled()) {
            File worldsFile = new File(this.getDataFolder(), "worlds.yml");
            YamlConfiguration worldsDisk = WorldsConfig.scanAndPopulate(worldsFile, this.getConfig(), this.getLogger());
            worldsConfig = new WorldsConfig(worldsDisk);
        }

        hybridDatabase = new HybridDatabase(this);
        hybridDatabase.start(defaultConfig);
        de.sean.blockprot.bukkit.admin.AdminTierManager.load();

        try {
            auditLogger = new AuditLogger(this.getDataFolder());
        } catch (Exception e) {
            BlockProtConsole.warn(Translator.get(TranslationKey.CONSOLE__AUDIT_LOGGER_FAILED)
                .replace("{error}", e.getMessage()));
        }

        fileWatcher = new ConfigFileWatcher(this);
        if (defaultConfig.isAutoReloadEnabled()) {
            fileWatcher.start();
        }

        int inactivityDays = this.getConfig().getInt("inactivity_cleanup_days", -1);
        if (inactivityDays > 0) {
            foliaLib.getScheduler().runAsync(task -> new InactivityCleanupTask(inactivityDays).run());
        }

        if (defaultConfig.isWorldExpiryEnabled()) {
            int interval = Math.max(1, defaultConfig.getWorldExpiryCheckInterval());
            foliaLib.getScheduler().runTimer(new WorldExpiryTask(), 0L, interval * 60L * 20L);
        }

        metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new IntegrationBarChart());
        metrics.addCustomChart(new LanguagePieChart());

        final PluginManager pm = getServer().getPluginManager();
        registerEvent(pm, new BlockEventListener(this));
        registerEvent(pm, new EntityEventListener());
        registerEvent(pm, new ExplodeEventListener());
        registerEvent(pm, new HopperEventListener());
        registerEvent(pm, new InteractEventListener());
        registerEvent(pm, new InventoryEventListener());
        registerEvent(pm, new JoinEventListener());
        registerEvent(pm, new PistonEventListener());
        registerEvent(pm, new RedstoneEventListener());
        registerEvent(pm, new LockEffectListener());
        registerEvent(pm, new EntityProtectionListener());
        registerEvent(pm, new EntityMenuOpenListener());
        registerEvent(pm, new VillagerWorkstationProtectionListener());
        registerEvent(pm, new ItemFrameListener());
        registerEvent(pm, new VehicleProtectionListener());
        registerEvent(pm, new AutoDropEntityListener());
        registerEvent(pm, new RaidDetectionListener());
        registerEvent(pm, new WorldEditPasteListener(this));

        BlockProtConsole.bootStatus(
            Translator.get(TranslationKey.CONSOLE__BOOT_LISTENERS),
            true,
            Translator.get(TranslationKey.CONSOLE__BOOT_REGISTERED));

        org.bukkit.command.PluginCommand bpCmd = this.getCommand("blockprot");
        if (bpCmd != null) {
            BlockProtCommand bpc = new BlockProtCommand();
            bpCmd.setExecutor(bpc);
            bpCmd.setTabCompleter(bpc);
        } else {
            BlockProtLogger.error("Failed to register /blockprot command: getCommand(\"blockprot\") returned null. Check plugin.yml command declaration!", null);
        }

        BlockProtConsole.bootStatus(
            Translator.get(TranslationKey.CONSOLE__BOOT_COMMANDS),
            true,
            Translator.get(TranslationKey.CONSOLE__BOOT_REGISTERED));

        boolean hasDialogs = VersionCompat.hasDialogApi();
        BlockProtConsole.bootStatus(
            Translator.get(TranslationKey.CONSOLE__BOOT_DIALOG_API),
            hasDialogs,
            hasDialogs ? Translator.get(TranslationKey.CONSOLE__BOOT_AVAILABLE)
                       : Translator.get(TranslationKey.CONSOLE__BOOT_NOT_AVAILABLE));

        boolean dbActive = hybridDatabase != null && hybridDatabase.isEnabled();
        String dbType = dbActive ? "MySQL" : "SQLite";
        BlockProtConsole.bootStatus("Database", true, dbType + " Ready");

        List<String> enabledIntegrations = new ArrayList<>();
        List<String> disabledIntegrations = new ArrayList<>();
        for (PluginIntegration integration : integrations) {
            try {
                integration.enable();
                if (integration.isEnabled()) {
                    enabledIntegrations.add(integration.name);
                } else {
                    disabledIntegrations.add(integration.name);
                }
            } catch (NoClassDefFoundError ignored) {
                disabledIntegrations.add(integration.name);
            }
        }
        integrationsEnabled = true;
        new BlockProtAPI(this);

        int activeIntegrations = enabledIntegrations.size();
        int totalIntegrations = integrations.size();
        boolean hasIntegrations = activeIntegrations > 0;
        String intSummary = hasIntegrations
            ? "Active (" + activeIntegrations + "/" + totalIntegrations + ": " + String.join(", ", enabledIntegrations) + ")"
            : "Disabled (0/" + totalIntegrations + ")";
        BlockProtConsole.bootStatus("Integrations", hasIntegrations, intSummary);

        boolean auditActive = auditLogger != null;
        BlockProtConsole.bootStatus("Audit Logger", auditActive, auditActive ? "Active" : "Disabled");

        printBootUpdateStatus(version);

        BlockProtConsole.bootStatus(
            Translator.get(TranslationKey.CONSOLE__BOOT_STARTUP_TIME),
            true,
            java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() + " ms",
            true);

        VersionValidator.validateStartup();

        if (legacyMigrationManager.isFirstStart(this) && !defaultConfig.hasConfiguredBlocks()) {
            String guideHeader = Translator.get(TranslationKey.CONSOLE__FIRST_START__HEADER);
            String guideTitle  = Translator.get(TranslationKey.CONSOLE__FIRST_START__TITLE)
                .replace("BlockProt Reloaded", Edition.current().displayName());
            String guideStep1  = Translator.get(TranslationKey.CONSOLE__FIRST_START__STEP1)
                .replace("{command}", "/bp lockables");
            String guideStep2  = Translator.get(TranslationKey.CONSOLE__FIRST_START__STEP2)
                .replace("{file}", "blocks.yml");
            String guideStep3  = Translator.get(TranslationKey.CONSOLE__FIRST_START__STEP3)
                .replace("{url}", "https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded");
            String guideStep4  = Translator.get(TranslationKey.CONSOLE__FIRST_START__STEP4)
                .replace("{command}", "/bp recommended");
            String guideFooter = Translator.get(TranslationKey.CONSOLE__FIRST_START__FOOTER);

            BlockProtLogger.log("startup", guideTitle);
            BlockProtLogger.log("startup", guideStep1);
            BlockProtLogger.log("startup", guideStep2);
            BlockProtLogger.log("startup", guideStep3);
            BlockProtLogger.log("startup", guideStep4);

            BlockProtConsole.guide(guideHeader);
            BlockProtConsole.guide(guideTitle);
            BlockProtConsole.guide(guideStep1);
            BlockProtConsole.guide(guideStep2);
            BlockProtConsole.guide(guideStep3);
            BlockProtConsole.guide(guideStep4);
            BlockProtConsole.guide(guideFooter);
            legacyMigrationManager.markFirstStartDone(this);
        }

        BlockProtConsole.printStartupBanner(version);

        super.onEnable();
    }

    private void announceBuildChannel(@NotNull String version) {
        try {
            SemanticVersion sv = new SemanticVersion(version);
            String msg;
            if (sv.isHotfix()) {
                msg = Translator.get(TranslationKey.CONSOLE__BOOT_BUILD_HOTFIX)
                    .replace("{version}", version)
                    .replace("{base}", sv.baseVersion());
            } else if (sv.isPreRelease()) {
                msg = Translator.get(TranslationKey.CONSOLE__BOOT_BUILD_EXPERIMENTAL)
                    .replace("{version}", version);
            } else {
                msg = Translator.get(TranslationKey.CONSOLE__BOOT_BUILD_STABLE)
                    .replace("{version}", version);
            }
            getLogger().info(msg);
        } catch (Exception ignored) {}
    }

    private void printBootUpdateStatus(@NotNull String version) {
        String label = Translator.get(TranslationKey.CONSOLE__BOOT_UPDATE);
        long deadline = System.currentTimeMillis() + 3000;
        while (UpdateChecker.latestVersion == null && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (UpdateChecker.latestVersion == null) {
            new UpdateChecker(version, () -> {}).run();
        }
        SemanticVersion latest = UpdateChecker.latestVersion;
        if (latest == null) {
            BlockProtConsole.bootColored(
                label,
                Translator.get(TranslationKey.CONSOLE__BOOT_UPDATE_NOT_CHECKED),
                BlockProtConsole.PASTEL_GRAY);
            return;
        }
        SemanticVersion current = new SemanticVersion(version);
        int cmp = latest.compareTo(current);
        if (cmp > 0) {
            String releaseUrl = UpdateChecker.latestRelease != null
                && UpdateChecker.latestRelease.getHtmlUrl() != null
                && !UpdateChecker.latestRelease.getHtmlUrl().isBlank()
                ? UpdateChecker.latestRelease.getHtmlUrl()
                : "https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/releases/latest";
            BlockProtConsole.bootColored(
                label,
                Translator.get(TranslationKey.CONSOLE__BOOT_UPDATE_OUTDATED)
                    .replace("{version}", latest.toString()),
                BlockProtConsole.PASTEL_CORAL);
            BlockProtConsole.bootIndented(
                Translator.get(TranslationKey.CONSOLE__BOOT_UPDATE_DOWNLOAD)
                    .replace("{url}", releaseUrl),
                BlockProtConsole.SOFT_BLUE);
        } else if (cmp < 0) {
            BlockProtConsole.bootColored(
                label,
                Translator.get(TranslationKey.CONSOLE__BOOT_UPDATE_AHEAD)
                    .replace("{version}", latest.toString()),
                BlockProtConsole.PASTEL_GOLD);
        } else {
            BlockProtConsole.bootColored(
                label,
                Translator.get(TranslationKey.CONSOLE__BOOT_UPDATE_UP_TO_DATE)
                    .replace("{version}", latest.toString()),
                BlockProtConsole.PASTEL_MINT);
        }
    }

    @Override
    public void onDisable() {
        if (isUnsupportedMinecraft()) {
            super.onDisable();
            return;
        }
        if (!isRunningCraftBukkit()) {
            BlockProtConsole.info(Translator.get(TranslationKey.CONSOLE__SAVING_STATISTICS));
            StatHandler.disable();
            getServer().getOnlinePlayers().forEach(HumanEntity::closeInventory);
        }

        if (fileWatcher    != null) fileWatcher.stop();
        if (auditLogger    != null) auditLogger.close();
        if (hybridDatabase != null) hybridDatabase.close();
        de.sean.blockprot.bukkit.admin.AdminTierManager.save();
        BlockProtLogger.close();
        super.onDisable();
    }

    public void reloadConfigAndTranslations() {
        if (fileWatcher != null) fileWatcher.suppressNext();
        legacyMigrationManager.cleanLegacyConfigKeys(this);
        legacyMigrationManager.mergeMissingConfigKeys(this);
        BlocksFormatConverter.mergeMissingBlocksKeys(this);
        BlocksFormatConverter.convertBlocksFormatIfNeeded(this);
        saveResourceSilent("mysql/mysql.yml", false);
        saveResourceSilent("worlds.yml", false);
        saveResourceSilent("lang/lang.yml", false);
        LangConfig.reload();
        this.reloadConfig();
        defaultConfig = new DefaultConfig(this.getConfig(), this.getDataFolder());
        de.sean.blockprot.bukkit.admin.AdminTierManager.load();

        Translator.resetTranslations();

        final String langFolder = "lang/";
        final String activeLang = defaultConfig.getLanguageFile() == null
            ? defaultLanguageFile : defaultConfig.getLanguageFile();

        for (String resource : Translator.DEFAULT_TRANSLATION_FILES) {
            if (!LangConfig.isLanguageEnabled(resource) && !resource.equals(activeLang)) {
                File diskFile = new File(this.getDataFolder(), langFolder + resource);
                if (diskFile.exists() && diskFile.delete()) {
                    BlockProtLogger.log("lang-cleanup", "Deleted disabled language file: " + resource);
                }
                continue;
            }
            File diskFile = new File(this.getDataFolder(), langFolder + resource);
            if (!diskFile.exists()) {
                this.saveResource(langFolder + resource, false);
            } else {
                mergeMissingLangKeys(langFolder, resource, diskFile);
            }
        }

        String fallbackLang = LangConfig.getFallbackLanguage();
        YamlConfiguration fallbackLanguageConfig;
        try (InputStream fallbackLanguageStream = this.getResource(langFolder + fallbackLang)) {
            if (fallbackLanguageStream == null) {
                fallbackLang = defaultLanguageFile;
                try (InputStream retry = this.getResource(langFolder + fallbackLang)) {
                    fallbackLanguageConfig = retry != null ? YamlConfiguration.loadConfiguration(
                        new BufferedReader(new InputStreamReader(retry, StandardCharsets.UTF_8))) : new YamlConfiguration();
                }
            } else {
                fallbackLanguageConfig = YamlConfiguration.loadConfiguration(
                    new BufferedReader(new InputStreamReader(fallbackLanguageStream, StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            fallbackLanguageConfig = new YamlConfiguration();
            getLogger().warning("Failed to load fallback language: " + e.getMessage());
        }

        YamlConfiguration wantedConfig = saveAndLoadConfigFile(
            langFolder, activeLang, BlockProt.defaultConfig.shouldReplaceTranslations());
        Translator.loadFromConfigs(fallbackLanguageConfig, wantedConfig);

        updateLangCompletionPercentages(langFolder);

        if (defaultConfig.isPerWorldsConfigEnabled()) {
            File worldsFile = new File(this.getDataFolder(), "worlds.yml");
            YamlConfiguration worldsDisk = WorldsConfig.scanAndPopulate(worldsFile, this.getConfig(), this.getLogger());
            worldsConfig = new WorldsConfig(worldsDisk);
        } else {
            worldsConfig = null;
        }

        IntegrationConfig.reload();
        for (PluginIntegration integration : integrations) {
            integration.reload();
        }

        if (fileWatcher != null) {
            boolean shouldRun = defaultConfig.isAutoReloadEnabled();
            boolean isRunning = fileWatcher.isRunning();
            if (shouldRun && !isRunning) {
                fileWatcher.start();
            } else if (!shouldRun && isRunning) {
                fileWatcher.stop();
                BlockProtConsole.info(Translator.get(TranslationKey.CONSOLE__AUTO_RELOAD_DISABLED));
            }
        }

        if (defaultConfig.isDialogsEnabled() && !VersionCompat.hasDialogApi()) {
            String dialogsUnavailable = Translator.get(TranslationKey.CONSOLE__DIALOGS_UNAVAILABLE);
            BlockProtConsole.warn(dialogsUnavailable);
            BlockProtLogger.warn(dialogsUnavailable);
            defaultConfig.setAndSave("use_dialogs", false);
            BlockProtLogger.log("dialogs", "Auto-disabled use_dialogs in config.yml (Paper dialog API unavailable).");
        }
    }

    private void registerEvent(@NotNull PluginManager pm, Listener listener) {
        pm.registerEvents(listener, this);
    }

    void registerIntegration(@NotNull PluginIntegration integration) {
        this.integrations.add(integration);
        BlockProtLogger.log("integration", "Registered: " + integration.name);
        try {
            if (integrationsLoaded) {
                integration.load();
            }
        } catch (NoClassDefFoundError ignored) {}
        try {
            if (integrationsEnabled) {
                integration.enable();
            }
        } catch (NoClassDefFoundError ignored) {}
    }

    @Nullable
    public Plugin getPlugin(String pluginName) {
        return this.getServer().getPluginManager().getPlugin(pluginName);
    }

    @NotNull
    public YamlConfiguration saveAndLoadConfigFile(String folder, String name, boolean replace) {
        final String path = folder + (folder.endsWith("/") ? "" : "/") + name;
        File file = new File(this.getDataFolder(), path);
        if (!file.exists()) {
            try {
                this.saveResource(path, replace);
            } catch (IllegalArgumentException e) {
                if (!folder.startsWith("integrations")) {
                    getLogger().warning(Translator.get(TranslationKey.CONSOLE__CONFIG_LANGUAGE_MISSING)
                        .replace("{file}", name));
                }
                return new YamlConfiguration();
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private void mergeMissingLangKeys(@NotNull String langFolder, @NotNull String resource, @NotNull File diskFile) {
        try (InputStream jarStream = this.getResource(langFolder + resource)) {
            if (jarStream == null) return;

            YamlConfiguration jarConfig  = YamlConfiguration.loadConfiguration(
                new BufferedReader(new InputStreamReader(jarStream, StandardCharsets.UTF_8)));
            YamlConfiguration diskConfig = YamlConfiguration.loadConfiguration(diskFile);

            int added = 0;
            for (String key : jarConfig.getKeys(true)) {
                if (jarConfig.isConfigurationSection(key)) continue;
                if (!diskConfig.contains(key)) {
                    diskConfig.set(key, jarConfig.get(key));
                    added++;
                    BlockProtLogger.log("lang-merge", resource + ": added missing key: " + key);
                }
            }

            if (added > 0) {
                try {
                    diskConfig.save(diskFile);
                    BlockProtLogger.log("lang-merge", resource + ": added " + added + " missing key(s).");
                } catch (IOException e) {
                    BlockProtConsole.warn(Translator.get(TranslationKey.CONSOLE__LANG_KEYS_SAVE_FAILED)
                        .replace("{file}", resource).replace("{error}", e.getMessage()));
                }
            }
        } catch (IOException e) {
            BlockProtLogger.warn("Failed to merge lang keys for " + resource + ": " + e.getMessage());
        }
    }

    private void updateLangCompletionPercentages(@NotNull String langFolder) {
        String activeLang = defaultConfig.getLanguageFile() != null
            ? defaultConfig.getLanguageFile() : defaultLanguageFile;
        File langYml = new File(this.getDataFolder(), "lang/lang.yml");
        if (!langYml.exists()) return;
        try {
            Map<String, Integer> percentages = new HashMap<>();
            for (String resource : Translator.DEFAULT_TRANSLATION_FILES) {
                if (!LangConfig.isLanguageEnabled(resource) && !resource.equals(activeLang)) continue;
                YamlConfiguration langFile = null;
                File diskFile = new File(this.getDataFolder(), langFolder + resource);
                if (diskFile.exists()) {
                    langFile = YamlConfiguration.loadConfiguration(diskFile);
                } else {
                    try (InputStream jarStream = this.getResource(langFolder + resource)) {
                        if (jarStream != null) {
                            langFile = YamlConfiguration.loadConfiguration(
                                new BufferedReader(new InputStreamReader(jarStream, StandardCharsets.UTF_8)));
                        }
                    }
                }
                if (langFile == null) continue;
                percentages.put(resource, Translator.computeCompletionPercentage(langFile));
            }

            YamlConfiguration existing = YamlConfiguration.loadConfiguration(langYml);
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(langYml), StandardCharsets.UTF_8))) {
                String line;
                boolean inLanguages = false;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.strip();
                    if (trimmed.equals("languages:") || trimmed.startsWith("languages:")) {
                        inLanguages = true;
                        String langIndent = line.substring(0, line.length() - line.stripLeading().length());
                        String indent = langIndent + "  ";
                        lines.add(line);
                        for (String fileName : Translator.DEFAULT_TRANSLATION_FILES) {
                            String code = LangConfig.fileNameToCode(fileName);
                            boolean enabled = existing.getBoolean("languages." + code, false);
                            int pct = percentages.getOrDefault(fileName, 0);
                            lines.add(indent + code + ": " + enabled);
                            lines.add(indent + "# K: " + pct + "%");
                        }
                    } else if (inLanguages) {
                        if (trimmed.isEmpty()) continue;
                        if (!trimmed.startsWith("#") && line.stripLeading().length() == line.length()
                            && line.length() > 0) {
                            inLanguages = false;
                            lines.add(line);
                        }
                    } else {
                        lines.add(line);
                    }
                }
            }
            StringBuilder newContent = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                newContent.append(lines.get(i));
                if (i < lines.size() - 1) newContent.append(System.lineSeparator());
            }

            String currentContent;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(langYml), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String l;
                boolean first = true;
                while ((l = reader.readLine()) != null) {
                    if (!first) sb.append(System.lineSeparator());
                    sb.append(l);
                    first = false;
                }
                currentContent = sb.toString();
            }

            if (currentContent.equals(newContent.toString())) {
                return;
            }

            if (fileWatcher != null) fileWatcher.suppressPath("lang/lang.yml");
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(langYml), StandardCharsets.UTF_8)) {
                writer.write(newContent.toString());
            }
        } catch (IOException e) {
            getLogger().warning("Failed to update lang.yml completion percentages: " + e.getMessage());
        }
    }

    private void saveResourceSilent(@NotNull String name, boolean replace) {
        File dest = new File(this.getDataFolder(), name);
        if (!replace && dest.exists()) return;
        try {
            this.saveResource(name, replace);
        } catch (Exception e) {
            BlockProtLogger.log("resource", "Could not save resource " + name + ": " + e.getMessage());
        }
    }

    private boolean isRunningCraftBukkit() {
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}