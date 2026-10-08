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

package de.sean.blockprot.bukkit.config;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.VersionCompat;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class DefaultConfig extends BlockProtConfig {

    public static final List<String> BLOCKS_YML_KEY_ORDER = BlocksConfigStorage.BLOCKS_YML_KEY_ORDER;

    private final ArrayList<Material> lockableTileEntities = new ArrayList<>();
    private final ArrayList<Material> shulkerBoxes = new ArrayList<>();
    private final ArrayList<Material> lockableBlocks = new ArrayList<>();
    private final ArrayList<Material> lockableDoors = new ArrayList<>();
    private final ArrayList<Material> lockableEntities = new ArrayList<>();

    private final Set<Material> lockableTileEntitiesSet = EnumSet.noneOf(Material.class);
    private final Set<Material> shulkerBoxesSet = EnumSet.noneOf(Material.class);
    private final Set<Material> lockableBlocksSet = EnumSet.noneOf(Material.class);
    private final Set<Material> lockableDoorsSet = EnumSet.noneOf(Material.class);
    private final Set<Material> lockableEntitiesSet = EnumSet.noneOf(Material.class);
    private final Set<Material> allLockableSet = EnumSet.noneOf(Material.class);

    private final ArrayList<Material> inactiveTileEntities = new ArrayList<>();
    private final ArrayList<Material> inactiveShulkerBoxes = new ArrayList<>();
    private final ArrayList<Material> inactiveBlocks = new ArrayList<>();
    private final ArrayList<Material> inactiveDoors = new ArrayList<>();
    private final ArrayList<Material> inactiveEntities = new ArrayList<>();

    private final ArrayList<InventoryType> lockableInventories = new ArrayList<>(Arrays.asList(
        InventoryType.CHEST, InventoryType.FURNACE, InventoryType.SMOKER, InventoryType.BLAST_FURNACE,
        InventoryType.HOPPER, InventoryType.BARREL, InventoryType.BREWING, InventoryType.SHULKER_BOX,
        InventoryType.ANVIL, InventoryType.DISPENSER, InventoryType.DROPPER, InventoryType.LECTERN,
        InventoryType.GRINDSTONE, InventoryType.STONECUTTER, InventoryType.LOOM,
        InventoryType.CARTOGRAPHY, InventoryType.SMITHING
    ));

    private final HashSet<Material> knownGoodTileEntities = new HashSet<>(Arrays.asList(
        Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST,
        Material.FURNACE, Material.SMOKER, Material.BLAST_FURNACE,
        Material.HOPPER, Material.BARREL, Material.BREWING_STAND, Material.DISPENSER, Material.DROPPER,
        Material.LECTERN, Material.BEEHIVE, Material.BEE_NEST,
        Material.JUKEBOX, Material.BEACON,
        Material.OAK_SIGN, Material.OAK_WALL_SIGN,
        Material.SPRUCE_SIGN, Material.SPRUCE_WALL_SIGN,
        Material.BIRCH_SIGN, Material.BIRCH_WALL_SIGN,
        Material.JUNGLE_SIGN, Material.JUNGLE_WALL_SIGN,
        Material.ACACIA_SIGN, Material.ACACIA_WALL_SIGN,
        Material.DARK_OAK_SIGN, Material.DARK_OAK_WALL_SIGN,
        Material.CRIMSON_SIGN, Material.CRIMSON_WALL_SIGN,
        Material.WARPED_SIGN, Material.WARPED_WALL_SIGN
    ));

    private final List<String> excludedWorlds;
    private final File dataFolder;
    private YamlConfiguration blocksConfig = null;
    private YamlConfiguration mysqlConfig  = null;
    private volatile Set<Material> cachedAutoDropBlocks = null;

    public DefaultConfig(@NotNull final FileConfiguration config) {
        this(config, null);
    }

    public DefaultConfig(@NotNull final FileConfiguration config, final File dataFolder) {
        super(config);
        this.dataFolder = dataFolder;
        this.excludedWorlds = config.getStringList("excluded_worlds");
        this.removeBlockDefaults();

        if (config.contains("recommended_config_applied")) {
            config.set("recommended_config_applied", null);
            if (BlockProt.getInstance() != null) {
                BlockProt.getInstance().saveConfig();
            }
        }

        if (dataFolder != null) {
            String blocksFilePath = config.getString("blocks_file", "blocks.yml");
            File blocksFile = new File(dataFolder, blocksFilePath);
            try {
                if (blocksFile.exists()) {
                    this.blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);
                    BlocksConfigStorage.patchBlocksFileIfNeeded(blocksFile, this.blocksConfig);
                } else {
                    File parent = blocksFile.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();
                    BlocksConfigStorage.createBlocksFileWithHeader(blocksFile);
                    this.blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);
                    BlockProtLogger.log("blocks", "Created empty blocks.yml at " + blocksFile.getPath());
                }
            } catch (Exception ex) {
                BlockProtLogger.warn("blocks file handling failed: " + ex.getMessage());
                this.blocksConfig = null;
            }
        }
        this.loadBlocksFromConfig();

        if (dataFolder != null) {
            File mysqlFile = new File(dataFolder, "mysql/mysql.yml");
            if (mysqlFile.exists()) {
                this.mysqlConfig = YamlConfiguration.loadConfiguration(mysqlFile);
            }
        }
    }

    private void addMaterialIfExists(Collection<Material> set, String... names) {
        for (String name : names) {
            Material m = Material.matchMaterial(name);
            if (m != null) set.add(m);
        }
    }

    private void syncLockableSets() {
        lockableTileEntitiesSet.clear();
        lockableTileEntitiesSet.addAll(lockableTileEntities);

        shulkerBoxesSet.clear();
        shulkerBoxesSet.addAll(shulkerBoxes);

        lockableBlocksSet.clear();
        lockableBlocksSet.addAll(lockableBlocks);

        lockableDoorsSet.clear();
        lockableDoorsSet.addAll(lockableDoors);

        lockableEntitiesSet.clear();
        lockableEntitiesSet.addAll(lockableEntities);

        allLockableSet.clear();
        allLockableSet.addAll(lockableTileEntitiesSet);
        allLockableSet.addAll(shulkerBoxesSet);
        allLockableSet.addAll(lockableBlocksSet);
        allLockableSet.addAll(lockableDoorsSet);
    }

    private void loadBlockListFromConfig(
            @NotNull String key, @NotNull final ArrayList<Material> activeList,
            @NotNull final ArrayList<Material> inactiveList,
            @NotNull final BlockFamilyParser.Family family,
            @NotNull final java.util.function.Function<Material, Boolean> validateCallback) {
        activeList.clear();
        inactiveList.clear();

        Object rawValue = null;
        if (this.blocksConfig != null && this.blocksConfig.contains(key))
            rawValue = this.blocksConfig.get(key);
        if (rawValue == null) rawValue = config.get(key);
        if (rawValue == null) return;

        Set<Material> resolved = new LinkedHashSet<>();

        if (rawValue instanceof List<?> list) {
            for (Object o : list) {
                if (!(o instanceof String s)) continue;
                String trimmed = s.trim();
                if (BlockFamilyParser.isFamilyExpression(trimmed)) {
                    resolved.addAll(BlockFamilyParser.parseFamilyExpression(trimmed, family));
                } else {
                    Material m = Material.matchMaterial(trimmed);
                    if (m != null) BlockFamilyParser.addMaterialWithPlacementVariants(m, resolved);
                }
            }
        } else if (rawValue instanceof String s) {
            String trimmed = s.trim();
            if (BlockFamilyParser.isFamilyExpression(trimmed)) {
                resolved.addAll(BlockFamilyParser.parseFamilyExpression(trimmed, family));
            } else {
                Material m = Material.matchMaterial(trimmed);
                if (m != null) BlockFamilyParser.addMaterialWithPlacementVariants(m, resolved);
            }
        }

        for (Material m : resolved) {
            if (validateCallback.apply(m)) activeList.add(m);
        }

        Set<Material> allFamily = BlockFamilyParser.getFamilyMembers(family);
        Set<Material> activeSet = new HashSet<>(resolved);
        for (Material m : allFamily) {
            if (!activeSet.contains(m) && validateCallback.apply(m)) {
                inactiveList.add(m);
            }
        }
    }

    private void loadBlocksFromConfig() {
        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_19_R1))
            addMaterialIfExists(knownGoodTileEntities, "MANGROVE_SIGN", "MANGROVE_WALL_SIGN");
        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_20_R1)) {
            addMaterialIfExists(knownGoodTileEntities, "CHISELED_BOOKSHELF", "DECORATED_POT");
            addMaterialIfExists(knownGoodTileEntities,
                "OAK_WALL_HANGING_SIGN", "OAK_HANGING_SIGN",
                "SPRUCE_WALL_HANGING_SIGN", "SPRUCE_HANGING_SIGN",
                "BIRCH_WALL_HANGING_SIGN", "BIRCH_HANGING_SIGN",
                "JUNGLE_WALL_HANGING_SIGN", "JUNGLE_HANGING_SIGN",
                "ACACIA_WALL_HANGING_SIGN", "ACACIA_HANGING_SIGN",
                "DARK_OAK_WALL_HANGING_SIGN", "DARK_OAK_HANGING_SIGN",
                "CRIMSON_WALL_HANGING_SIGN", "CRIMSON_HANGING_SIGN",
                "WARPED_WALL_HANGING_SIGN", "WARPED_HANGING_SIGN",
                "MANGROVE_HANGING_SIGN", "MANGROVE_WALL_HANGING_SIGN",
                "CHERRY_SIGN", "CHERRY_WALL_SIGN", "CHERRY_HANGING_SIGN", "CHERRY_WALL_HANGING_SIGN",
                "BAMBOO_SIGN", "BAMBOO_WALL_SIGN", "BAMBOO_HANGING_SIGN", "BAMBOO_WALL_HANGING_SIGN");
        }
        addMaterialIfExists(knownGoodTileEntities, "CRAFTER");
        addMaterialIfExists(knownGoodTileEntities,
            "COPPER_CHEST", "EXPOSED_COPPER_CHEST", "WEATHERED_COPPER_CHEST", "OXIDIZED_COPPER_CHEST",
            "WAXED_COPPER_CHEST", "WAXED_EXPOSED_COPPER_CHEST", "WAXED_WEATHERED_COPPER_CHEST", "WAXED_OXIDIZED_COPPER_CHEST",
            "COPPER_TRAPPED_CHEST", "EXPOSED_COPPER_TRAPPED_CHEST", "WEATHERED_COPPER_TRAPPED_CHEST", "OXIDIZED_COPPER_TRAPPED_CHEST",
            "WAXED_COPPER_TRAPPED_CHEST", "WAXED_EXPOSED_COPPER_TRAPPED_CHEST", "WAXED_WEATHERED_COPPER_TRAPPED_CHEST", "WAXED_OXIDIZED_COPPER_TRAPPED_CHEST",
            "OAK_SHELF", "SPRUCE_SHELF", "BIRCH_SHELF", "JUNGLE_SHELF", "ACACIA_SHELF",
            "DARK_OAK_SHELF", "MANGROVE_SHELF", "CHERRY_SHELF", "PALE_OAK_SHELF", "POPLAR_SHELF",
            "BAMBOO_SHELF", "CRIMSON_SHELF", "WARPED_SHELF");

        java.util.function.Function<Material, Boolean> tileEntityValidator = m -> {
            String name = m.name();
            if (knownGoodTileEntities.contains(m)) return true;
            if (name.contains("COPPER_CHEST") || name.contains("COPPER_TRAPPED_CHEST")) return true;
            if (name.endsWith("_SHELF")) return true;
            if (name.endsWith("_SIGN") || name.endsWith("_WALL_SIGN")
                || name.endsWith("_HANGING_SIGN") || name.endsWith("_WALL_HANGING_SIGN")) return true;
            if (name.equals("DECORATED_POT") || name.equals("CHISELED_BOOKSHELF") || name.equals("CRAFTER")) return true;
            if (name.equals("JUKEBOX")) return true;
            return false;
        };

        loadBlockListFromConfig("lockable_tile_entities", lockableTileEntities, inactiveTileEntities,
            BlockFamilyParser.Family.TILE_ENTITIES, tileEntityValidator);
        loadBlockListFromConfig("lockable_shulker_boxes", shulkerBoxes, inactiveShulkerBoxes,
            BlockFamilyParser.Family.SHULKER_BOXES, m -> m.toString().contains("SHULKER_BOX"));
        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_16_R3)) {
            java.util.function.Function<Material, Boolean> blockValidator = m -> {
                if (knownGoodTileEntities.contains(m)) return false;
                String name = m.name();
                if (name.equals("DRAGON_EGG")) return true;
                if (name.equals("COMPOSTER") || name.equals("BELL") || name.equals("NOTE_BLOCK")) return true;
                if (name.equals("GRINDSTONE") || name.equals("STONECUTTER") || name.equals("LOOM")) return true;
                if (name.equals("CARTOGRAPHY_TABLE") || name.equals("SMITHING_TABLE") || name.equals("ENCHANTING_TABLE") || name.equals("FLETCHING_TABLE")) return true;
                if (name.contains("CAULDRON")) return true;
                if (name.contains("ANVIL")) return true;
                if (name.contains("FENCE_GATE")) return true;
                if (name.contains("TRAPDOOR")) return true;
                if (name.endsWith("_BED")) return true;
                return false;
            };
            loadBlockListFromConfig("lockable_blocks", lockableBlocks, inactiveBlocks,
                BlockFamilyParser.Family.BLOCKS, blockValidator);
            loadBlockListFromConfig("lockable_doors", lockableDoors, inactiveDoors,
                BlockFamilyParser.Family.DOORS, m -> m.toString().contains("DOOR"));
            lockableBlocks.addAll(lockableDoors);
        }

        loadBlockListFromConfig("lockable_entities", lockableEntities, inactiveEntities,
            BlockFamilyParser.Family.ENTITIES, m -> {
                String name = m.name();
                return name.contains("CHEST_BOAT") || name.contains("CHEST_RAFT") || name.equals("CHEST_MINECART") || name.equals("HOPPER_MINECART")
                    || name.equals("ITEM_FRAME") || name.equals("GLOW_ITEM_FRAME");
            });

        syncLockableSets();
        refreshAutoDropCache();
    }

    private void refreshAutoDropCache() {
        List<?> raw = null;
        if (blocksConfig != null && blocksConfig.contains("auto_drop_to_inventory.blocks"))
            raw = blocksConfig.getList("auto_drop_to_inventory.blocks");
        if (raw == null) raw = config.getList("auto_drop_to_inventory.blocks");
        if (raw == null) {
            this.cachedAutoDropBlocks = Collections.emptySet();
            return;
        }

        Set<Material> result = new LinkedHashSet<>();
        for (Object o : raw) {
            if (!(o instanceof String s)) continue;
            String trimmed = s.trim();
            if (BlockFamilyParser.isFamilyExpression(trimmed)) {
                for (BlockFamilyParser.Family f : BlockFamilyParser.Family.values()) {
                    result.addAll(BlockFamilyParser.parseFamilyExpressionSilent(trimmed, f));
                }
            } else {
                Material m = Material.matchMaterial(trimmed);
                if (m != null) BlockFamilyParser.addMaterialWithPlacementVariants(m, result);
            }
        }
        this.cachedAutoDropBlocks = Collections.unmodifiableSet(result);
    }

    @Nullable public String getLanguageFile() { return config.getString("language_file"); }

    public boolean shouldReplaceTranslations() {
        return !config.contains("replace_translations") || config.getBoolean("replace_translations");
    }

    public boolean shouldNotifyOpOfUpdates() {
        return config.contains("notify_op_of_updates") && config.getBoolean("notify_op_of_updates");
    }

    public boolean disallowRedstoneOnPlace() {
        return config.contains("redstone_disallowed_by_default") && config.getBoolean("redstone_disallowed_by_default");
    }

    public boolean isWorldExcluded(World world) {
        if (isWorldsConfigEnabled()) {
            WorldsConfig wc = BlockProt.getWorldsConfig();
            if (wc != null && wc.isWorldDisabled(world)) return true;
        }
        if (listContainsIgnoreCase(excludedWorlds, world.getName())) return true;
        try {
            String keyVal = world.getKey().value();
            if (listContainsIgnoreCase(excludedWorlds, keyVal)) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    public boolean isWorldsConfigEnabled() { return isPerWorldsConfigEnabled(); }
    public boolean isPerWorldsConfigEnabled() {
        if (config.contains("per_worlds_config")) return config.getBoolean("per_worlds_config", false);
        return config.getBoolean("worlds_config_enabled", false);
    }

    public boolean isAutoReloadEnabled() {
        return config.getBoolean("auto_reload_configs", true);
    }

    public int getAutoReloadDelaySeconds() {
        int val = config.getInt("auto_reload_delay_seconds", 0);
        if (val < 0) return 0;
        if (val > 5) return 5;
        return val;
    }

    public boolean isAdminTiersEnabled() {
        return config.getBoolean("admin_tiers.enabled", false);
    }

    public int getActionBarDurationSeconds() {
        return Math.max(1, config.getInt("action_bar.duration_seconds", 6));
    }

    public long getActionBarDurationTicks() {
        return (long) getActionBarDurationSeconds() * 20L;
    }

    public boolean isWorldExcluded(InventoryHolder holder) {
        try {
            if (holder instanceof DoubleChest) {
                @Nullable World world = ((DoubleChest) holder).getWorld();
                if (world == null) return true;
                return isWorldExcluded(world);
            }
            return isWorldExcluded(((BlockInventoryHolder) holder).getBlock().getWorld());
        } catch (ClassCastException e) { return true; }
    }

    public boolean lockOnPlaceByDefault() {
        return !config.contains("lock_on_place_by_default") || config.getBoolean("lock_on_place_by_default");
    }

    public boolean publicIsFriendByDefault() {
        return config.contains("public_is_friend_by_default") && config.getBoolean("public_is_friend_by_default");
    }

    @Nullable public String getTranslationFallbackString() {
        return !config.contains("fallback_string") ? "" : config.getString("fallback_string");
    }

    @Nullable public Integer getMaxLockedBlockCount() {
        if (!config.contains("player_max_locked_block_count")) return null;
        int val = config.getInt("player_max_locked_block_count");
        return val > 0 ? val : null;
    }

    public void removeBlockDefaults() {
        Configuration defaults = config.getDefaults();
        if (defaults != null) {
            defaults.set("lockable_tile_entities", null);
            defaults.set("lockable_shulker_boxes", null);
            defaults.set("lockable_blocks", null);
            defaults.set("lockable_doors", null);
            config.setDefaults(defaults);
        }
    }

    public long getLockHintCooldown() {
        return config.contains("lock_hint_cooldown_in_seconds") ? config.getLong("lock_hint_cooldown_in_seconds") : 10;
    }

    public boolean areExtraCommandsEnabled() {
        return !isUseMenusEnabled();
    }

    public boolean isUseMenusEnabled() {
        return config.getBoolean("use_menus", false);
    }

    public boolean isDialogsEnabled() {
        return config.getBoolean("use_dialogs", false);
    }

    public boolean shouldUseDialogs() {
        return isDialogsEnabled() && VersionCompat.hasDialogApi();
    }

    public boolean shouldUseDialogs(@NotNull Player player) {
        if (!shouldUseDialogs()) return false;
        return new de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler(player).getPreferDialogs();
    }

    public boolean shouldEnableAllOptionalFeatures() { return false; }
    public long getTimedAccessMaxDurationSeconds() {
        int days = config.getInt("timed_access_max_duration_days", 90);
        if (days <= 0) return Long.MAX_VALUE;
        return (long) days * 24 * 60 * 60;
    }
    public boolean isMysqlEnabled() { return mysqlConfig != null && mysqlConfig.contains("mysql.enabled") && mysqlConfig.getBoolean("mysql.enabled"); }

    @NotNull public String getMysqlJdbcUrl() {
        ConfigurationSection src = mysqlConfig != null ? mysqlConfig : config;
        String configured = src.getString("mysql.jdbc_url", "");
        if (configured != null && !configured.isBlank()) return configured;
        return "jdbc:mysql://" + src.getString("mysql.host","127.0.0.1") + ":"
            + src.getInt("mysql.port", 3306) + "/" + src.getString("mysql.database","blockprot")
            + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8";
    }

    @NotNull public String getMysqlUsername() {
        ConfigurationSection src = mysqlConfig != null ? mysqlConfig : config;
        return src.getString("mysql.username","blockprot");
    }
    @NotNull public String getMysqlPassword() {
        ConfigurationSection src = mysqlConfig != null ? mysqlConfig : config;
        return src.getString("mysql.password","");
    }
    public int getMysqlPoolSize() {
        ConfigurationSection src = mysqlConfig != null ? mysqlConfig : config;
        return Math.max(1, src.getInt("mysql.pool.maximum_pool_size", 10));
    }
    public int getMysqlMinimumIdle() {
        ConfigurationSection src = mysqlConfig != null ? mysqlConfig : config;
        return Math.max(0, src.getInt("mysql.pool.minimum_idle", 2));
    }
    public long getMysqlConnectionTimeoutMillis() {
        ConfigurationSection src = mysqlConfig != null ? mysqlConfig : config;
        return Math.max(1000L, src.getLong("mysql.pool.connection_timeout_ms", 10000L));
    }

    public boolean shouldProtectLockedBlocksFromExplosions() {
        return !config.contains("protect_locked_blocks_from_explosions") || config.getBoolean("protect_locked_blocks_from_explosions");
    }
    public boolean shouldBlockProtectedBlockPistonMovement() {
        return !config.contains("block_protected_block_piston_movement") || config.getBoolean("block_protected_block_piston_movement");
    }

    @NotNull public List<String> getBedrockUsernamePrefixes() {
        return config.contains("bedrock_username_prefixes") ? config.getStringList("bedrock_username_prefixes") : List.of(".", "*", "_");
    }

    public double getFriendSearchSimilarityPercentage() {
        return config.contains("friend_search_similarity") ? config.getDouble("friend_search_similarity") : 0.5;
    }

    public boolean isFriendFunctionalityDisabled() {
        return config.contains("disable_friend_functionality") && config.getBoolean("disable_friend_functionality");
    }

    public boolean isPistonProtectionEnabled() { return config.getBoolean("piston_protection", true); }
    public boolean shouldClearProtectionOnShulkerBreak()  { return config.getBoolean("clear_protection_on_shulker_break", false); }

    public boolean isSimplifiedHopperLogic() { return config.getBoolean("simplified_hopper_logic", false); }
    public boolean shouldAllowBreakProtectedBlocks()       { return config.getBoolean("allow_break_protected_blocks", false); }
    public boolean shouldRespectSpawnProtection()          { return !config.contains("respect_spawn_protection") || config.getBoolean("respect_spawn_protection"); }
    public boolean isLockEffectEnabled()                   { return !config.contains("block_lock_effects") || config.getBoolean("block_lock_effects"); }
    public boolean isLockSoundEnabled()                    { return !config.contains("block_lock_sounds") || config.getBoolean("block_lock_sounds"); }
    public boolean isWorldExpiryEnabled()               { return config.getBoolean("world_expiry.enabled", false); }
    public int getWorldExpiryCheckInterval()            { return config.getInt("world_expiry.check_interval_minutes", 10); }
    public java.util.Map<String, String> getWorldExpiryDurations() {
        ConfigurationSection section = config.getConfigurationSection("world_expiry.worlds");
        if (section == null) return java.util.Map.of();
        java.util.Map<String, String> result = new java.util.LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            result.put(key, section.getString(key, "0"));
        }
        return result;
    }
    public void setWorldExpiryDuration(@NotNull String worldName, @NotNull String duration) {
        config.set("world_expiry.worlds." + worldName, duration);
        BlockProt.getInstance().saveConfig();
    }

    public @NotNull FileConfiguration getBukkitConfig() {
        return config;
    }

    public void setAndSave(@NotNull String key, @NotNull Object value) {
        BlockProt plugin = BlockProt.getInstance();
        Object oldValue = config.get(key);
        if (plugin.getFileWatcher() != null) {
            plugin.getFileWatcher().suppressPath("config.yml");
        }
        config.set(key, value);
        plugin.saveConfig();
        if (!java.util.Objects.equals(oldValue, value)) {
            BlockProtLogger.log("config", key + " changed from " + oldValue + " to " + value);
        }
        if ("use_menus".equals(key)) {
            org.bukkit.Bukkit.getOnlinePlayers().forEach(Player::updateCommands);
        }
        if (plugin.getFileWatcher() != null) {
            plugin.getFileWatcher().requestProgrammaticReload();
        }
    }

    public void setLockOnPlaceByDefault(boolean value) { setAndSave("lock_on_place_by_default", value); }
    public void setPublicIsFriendByDefault(boolean value) { setAndSave("public_is_friend_by_default", value); }
    public void setSimplifiedHopperLogic(boolean value) { setAndSave("simplified_hopper_logic", value); }
    public void setProtectFromExplosions(boolean value) { setAndSave("protect_locked_blocks_from_explosions", value); }
    public void setBlockPistonMovement(boolean value) { setAndSave("block_protected_block_piston_movement", value); }
    public void setClearProtectionOnShulkerBreak(boolean value) { setAndSave("clear_protection_on_shulker_break", value); }
    public void setAllowBreakProtectedBlocks(boolean value) { setAndSave("allow_break_protected_blocks", value); }
    public void setRespectSpawnProtection(boolean value) { setAndSave("respect_spawn_protection", value); }
    public void setLockEffects(boolean value) { setAndSave("block_lock_effects", value); }
    public void setLockSounds(boolean value) { setAndSave("block_lock_sounds", value); }
    public void setWorldExpiryEnabled(boolean value) { setAndSave("world_expiry.enabled", value); }
    public void setEntityProtectionEnabled(boolean value) { setAndSave("entity_protection.enabled", value); }
    public void setNotifyOpOfUpdates(boolean value) { setAndSave("notify_op_of_updates", value); }
    public void setSessionLogEnabled(boolean value) { setAndSave("enable_session_log", value); }
    public void setBackupsEnabled(boolean value) { setAndSave("enable_backups", value); }
    public void setAutoReloadConfigs(boolean value) { setAndSave("auto_reload_configs", value); }

    public void setAutoReloadDelaySeconds(int value) {
        int clamped = Math.max(0, Math.min(5, value));
        setAndSave("auto_reload_delay_seconds", clamped);
    }

    public void setRedstoneDisallowedByDefault(boolean value) { setAndSave("redstone_disallowed_by_default", value); }
    public void setLanguageFile(@NotNull String value) { setAndSave("language_file", value); }
    public void setPlayerMaxLockedBlockCount(int value) { setAndSave("player_max_locked_block_count", value); }
    public void setLockHintCooldown(int value) { setAndSave("lock_hint_cooldown_in_seconds", value); }
    public void setFriendSearchSimilarity(double value) { setAndSave("friend_search_similarity", value); }

    public boolean isOwnerNotificationsEnabled() { return config.getBoolean("owner_notifications.enabled", true); }
    public boolean isNotifyOnOpen()               { return isOwnerNotificationsEnabled() && config.getBoolean("owner_notifications.notify_on_open", true); }
    public boolean isNotifyOnTake()               { return isOwnerNotificationsEnabled() && config.getBoolean("owner_notifications.notify_on_take", true); }
    public boolean isNotifyOnPlace()              { return isOwnerNotificationsEnabled() && config.getBoolean("owner_notifications.notify_on_place", true); }

    public boolean isSessionLogEnabled()                { return config.getBoolean("enable_session_log", true); }
    public boolean isBackupsEnabled()                   { return config.getBoolean("enable_backups", true); }

    @Nullable
    public YamlConfiguration getBlocksConfig() {
        return blocksConfig;
    }

    public boolean isAutoDropToInventoryEnabled() {
        if (blocksConfig != null) return blocksConfig.getBoolean("auto_drop_to_inventory.enabled", true);
        return config.getBoolean("auto_drop_to_inventory.enabled", true);
    }

    @NotNull
    public Set<Material> getAutoDropToInventoryBlocks() {
        Set<Material> cached = this.cachedAutoDropBlocks;
        return cached != null ? cached : Collections.emptySet();
    }

    public boolean isAutoDropToInventory(@NotNull Material type) {
        return isAutoDropToInventoryEnabled() && getAutoDropToInventoryBlocks().contains(type);
    }

    public boolean isAutoDropToInventoryEnabled(@NotNull World world) {
        WorldsConfig wc = BlockProt.getWorldsConfig();
        if (isWorldsConfigEnabled() && wc != null && wc.hasWorldConfig(world))
            return wc.isAutoDropToInventoryEnabled(world);
        return isAutoDropToInventoryEnabled();
    }

    public String getConsolePrefixColor() { return "§x§8§0§4§0§0§0"; }
    public String getConsoleInfoColor()   { return "§x§D§2§B§4§8§C"; }
    public String getBlocksFilePath()      { return config.getString("blocks_file", "blocks.yml"); }

    public boolean isEntityProtectionEnabled() {
        return getEntityProtectionBoolean("enabled", false);
    }

    public boolean isEntityProtectionAutoProtectOnTame() {
        return getEntityProtectionBoolean("auto_protect_on_tame", true);
    }

    @NotNull
    public Material getEntityProtectionMenuItem() {
        String raw = getEntityProtectionString("menu_item", "STICK");
        Material m = Material.matchMaterial(raw == null ? "STICK" : raw);
        return m == null ? Material.STICK : m;
    }

    public List<String> getProtectableEntities() {
        List<String> raw = null;
        if (blocksConfig != null && blocksConfig.contains("protectable_entities")) {
            raw = blocksConfig.getStringList("protectable_entities");
        } else if (config.contains("entity_protection.protectable_entities")) {
            raw = config.getStringList("entity_protection.protectable_entities");
        }
        if (raw == null || raw.isEmpty()) return List.of();
        List<String> filtered = new ArrayList<>();
        for (String s : raw) {
            if (s != null && !BlocksConfigStorage.isPlaceholderEntry(s)) {
                filtered.add(s.trim());
            }
        }
        return filtered;
    }

    public int getVillagerLocateSeconds() {
        return Math.max(1, Math.min(10, config.getInt("entity_protection.villager_locate_seconds",
            config.getInt("pet_protection.villager_locate_seconds", 6))));
    }

    public boolean isVillagerWorkstationProtectionEnabled() {
        return config.getBoolean("villager_workstation_protection.enabled", true);
    }

    public int getVillagerWorkstationProtectionRadius() {
        return Math.max(0, Math.min(8, config.getInt("villager_workstation_protection.radius", 2)));
    }

    public int getVillagerWorkstationProtectionVerticalRadius() {
        return Math.max(0, Math.min(4, config.getInt("villager_workstation_protection.vertical_radius", 1)));
    }

    private boolean getEntityProtectionBoolean(@NotNull String leaf, boolean def) {
        String modern = "entity_protection." + leaf;
        if (config.contains(modern)) return config.getBoolean(modern, def);
        return config.getBoolean("pet_protection." + leaf, def);
    }

    @Nullable
    private String getEntityProtectionString(@NotNull String leaf, @NotNull String def) {
        String modern = "entity_protection." + leaf;
        if (config.contains(modern)) return config.getString(modern, def);
        return config.getString("pet_protection." + leaf, def);
    }

    @NotNull
    public String getEntityProtectionDeniedMessage() {
        return Translator.get(TranslationKey.MESSAGES__ENTITY_DENIED);
    }

    public boolean isLockable(Material type) { return allLockableSet.contains(type); }

    public boolean isLockable(@NotNull Material type, @NotNull World world) {
        WorldsConfig wc = BlockProt.getWorldsConfig();
        if (isWorldsConfigEnabled() && wc != null && wc.hasWorldConfig(world)) return wc.isLockable(world, type);
        return isLockable(type);
    }

    public boolean isLockableShulkerBox(@NotNull Material type, @NotNull World world) {
        WorldsConfig wc = BlockProt.getWorldsConfig();
        if (isWorldsConfigEnabled() && wc != null && wc.hasWorldConfig(world)) return wc.isLockableShulkerBox(world, type);
        return isLockableShulkerBox(type);
    }

    public boolean isLockableBlock(@NotNull Material type, @NotNull World world) {
        WorldsConfig wc = BlockProt.getWorldsConfig();
        if (isWorldsConfigEnabled() && wc != null && wc.hasWorldConfig(world)) return wc.isLockableBlock(world, type);
        return isLockableBlock(type);
    }

    public boolean isLockableTileEntity(@NotNull Material type, @NotNull World world) {
        WorldsConfig wc = BlockProt.getWorldsConfig();
        if (isWorldsConfigEnabled() && wc != null && wc.hasWorldConfig(world)) return wc.isLockableTileEntity(world, type) || wc.isLockableShulkerBox(world, type);
        return isLockableTileEntity(type);
    }

    public boolean isLockableDoor(@NotNull Material type, @NotNull World world) {
        WorldsConfig wc = BlockProt.getWorldsConfig();
        if (isWorldsConfigEnabled() && wc != null && wc.hasWorldConfig(world)) return wc.isLockableDoor(world, type);
        return isLockableDoor(type);
    }

    public boolean isLockableBlock(Material type)       { return lockableBlocksSet.contains(type); }
    public boolean isLockableTileEntity(Material type)  { return lockableTileEntitiesSet.contains(type) || shulkerBoxesSet.contains(type); }
    public boolean isLockableDoor(Material type)        { return lockableDoorsSet.contains(type); }
    public boolean isLockableShulkerBox(Material type)  { return shulkerBoxesSet.contains(type); }
    public boolean isLockableInventory(InventoryType t) { return lockableInventories.contains(t); }

    public void addTestLockable(@NotNull Material material) {
        if (!lockableTileEntities.contains(material)) {
            lockableTileEntities.add(material);
            syncLockableSets();
        }
    }

    public void removeTestLockable(@NotNull Material material) {
        lockableTileEntities.remove(material);
        syncLockableSets();
    }

    public boolean isLockableEntity(@NotNull Material type) {
        return lockableEntitiesSet.contains(type);
    }

    public boolean isLockableEntity(@NotNull Material type, @NotNull World world) {
        WorldsConfig wc = BlockProt.getWorldsConfig();
        if (isWorldsConfigEnabled() && wc != null && wc.hasWorldConfig(world))
            return wc.isLockableEntity(world, type);
        return isLockableEntity(type);
    }

    public boolean isInactive(Material type) {
        return inactiveTileEntities.contains(type) || inactiveShulkerBoxes.contains(type)
            || inactiveBlocks.contains(type) || inactiveDoors.contains(type) || inactiveEntities.contains(type);
    }

    @NotNull public List<Material> getInactiveTileEntities() { return Collections.unmodifiableList(inactiveTileEntities); }
    @NotNull public List<Material> getInactiveShulkerBoxes() { return Collections.unmodifiableList(inactiveShulkerBoxes); }
    @NotNull public List<Material> getInactiveBlocks()       { return Collections.unmodifiableList(inactiveBlocks); }
    @NotNull public List<Material> getInactiveDoors()        { return Collections.unmodifiableList(inactiveDoors); }
    @NotNull public List<Material> getInactiveEntities()     { return Collections.unmodifiableList(inactiveEntities); }

    public boolean isModernFamilyBlocks() { return config.getBoolean("modern_family_blocks", false); }

    private boolean isKeyInExpressionFormat(@NotNull String configKey) {
        return BlocksConfigModifier.isKeyInExpressionFormat(blocksConfig, isModernFamilyBlocks(), configKey);
    }

    @Nullable
    private File getBlocksFile() {
        if (dataFolder == null) return null;
        String blocksFilePath = config.getString("blocks_file", "blocks.yml");
        return new File(dataFolder, blocksFilePath);
    }

    public static boolean isPlaceholderEntry(@Nullable Object o) {
        return BlocksConfigStorage.isPlaceholderEntry(o);
    }

    public static void sanitizeBlocksListsForSave(@NotNull YamlConfiguration cfg, boolean modernFormat) {
        BlocksConfigStorage.sanitizeBlocksListsForSave(cfg, modernFormat);
    }

    public static void sanitizeListsForSave(@NotNull YamlConfiguration cfg, @NotNull String prefix, boolean modernFormat) {
        BlocksConfigStorage.sanitizeListsForSave(cfg, prefix, modernFormat);
    }

    public static YamlConfiguration reorderBlocksKeys(@NotNull YamlConfiguration source) {
        return BlocksConfigStorage.reorderBlocksKeys(source);
    }

    private void saveBlocksConfig() {
        File file = getBlocksFile();
        if (file == null || blocksConfig == null) return;
        try {
            BlockProt plugin = BlockProt.getInstance();
            if (plugin != null && plugin.getFileWatcher() != null) {
                plugin.getFileWatcher().suppressNext();
            }
            BlocksConfigStorage.sanitizeBlocksListsForSave(blocksConfig, isModernFamilyBlocks());
            blocksConfig = BlocksConfigStorage.reorderBlocksKeys(blocksConfig);
            blocksConfig.save(file);
            BlocksConfigStorage.prependBlocksHeader(file);
        } catch (IOException e) {
            BlockProtLogger.warn("Failed to save blocks.yml: " + e.getMessage());
        }
        BlockProt plugin = BlockProt.getInstance();
        if (plugin != null && plugin.getFileWatcher() != null) {
            plugin.getFileWatcher().requestProgrammaticReload();
        }
    }

    public static void prependBlocksHeader(@NotNull File file) throws IOException {
        BlocksConfigStorage.prependBlocksHeader(file);
    }

    public static void cleanNullPlaceholderLines(@NotNull File file) throws IOException {
        BlocksConfigStorage.cleanNullPlaceholderLines(file);
    }

    private void reloadBlocksAfterToggle() {
        loadBlocksFromConfig();
        BlocksConfigModifier.checkAutoDisableModernFamilyBlocks(blocksConfig, isModernFamilyBlocks(), dataFolder);
    }

    public boolean hasConfiguredBlocks() {
        return BlocksConfigModifier.hasConfiguredBlocks(blocksConfig);
    }

    @Nullable
    public static String configKeyForMaterial(@NotNull Material material) {
        return BlocksConfigModifier.configKeyForMaterial(material);
    }

    @Nullable
    public static String configKeyForFamily(@NotNull BlockFamilyParser.Family family) {
        return BlocksConfigModifier.configKeyForFamily(family);
    }

    public synchronized void convertBlocksFileFormat(boolean toModern) {
        if (blocksConfig == null) return;
        BlocksConfigModifier.writeFamilyKeyFormat(blocksConfig, "lockable_tile_entities", BlockFamilyParser.Family.TILE_ENTITIES, lockableTileEntities, toModern);
        BlocksConfigModifier.writeFamilyKeyFormat(blocksConfig, "lockable_shulker_boxes", BlockFamilyParser.Family.SHULKER_BOXES, shulkerBoxes, toModern);
        List<Material> blocksOnly = lockableBlocks.stream().filter(m -> !lockableDoors.contains(m)).toList();
        BlocksConfigModifier.writeFamilyKeyFormat(blocksConfig, "lockable_blocks", BlockFamilyParser.Family.BLOCKS, blocksOnly, toModern);
        BlocksConfigModifier.writeFamilyKeyFormat(blocksConfig, "lockable_doors", BlockFamilyParser.Family.DOORS, lockableDoors, toModern);
        BlocksConfigModifier.writeFamilyKeyFormat(blocksConfig, "lockable_entities", BlockFamilyParser.Family.ENTITIES, lockableEntities, toModern);
        saveBlocksConfig();
        reloadBlocksAfterToggle();
    }

    public synchronized boolean toggleLockable(@NotNull Material material, @NotNull Player who) {
        String configKey = configKeyForMaterial(material);
        if (configKey == null) {
            BlockProtLogger.warn("toggleLockable: " + material.name()
                + " does not belong to any BlockFamilyParser family; ignoring click from "
                + who.getName() + ".");
            return isLockable(material);
        }
        if (blocksConfig == null) return isLockable(material);

        String name = material.name();
        boolean currentlyActive = isLockable(material) || isLockableEntity(material);
        BlockFamilyParser.Family family = BlocksConfigModifier.familyForMaterial(material);

        if (family != null) {
            Object raw = blocksConfig.get(configKey);
            Set<Material> active = BlockFamilyParser.parse(raw, family);
            if (currentlyActive) {
                active.remove(material);
            } else {
                active.add(material);
            }
            if (isKeyInExpressionFormat(configKey)) {
                String expr = BlockFamilyParser.toFamilyExpression(active, family);
                if (expr != null) {
                    blocksConfig.set(configKey, List.of(expr));
                } else {
                    blocksConfig.set(configKey, active.stream().map(Material::name).sorted().toList());
                }
            } else {
                blocksConfig.set(configKey, active.stream().map(Material::name).sorted().toList());
            }
        }

        saveBlocksConfig();
        reloadBlocksAfterToggle();

        String actionText = currentlyActive
            ? Translator.get(TranslationKey.DISABLED)
            : Translator.get(TranslationKey.ENABLED);
        BlockProtLogger.log("lockables-toggle",
            actionText + " " + name + " in " + configKey
                + " (by " + who.getName() + ")");
        if (who.isOnline()) {
            ComponentMessages.sendLegacyActionBar(who, Translator.get(TranslationKey.MESSAGES__LOCKABLES__TOGGLE_FEEDBACK)
                .replace("{action}", actionText)
                .replace("{name}", name));
        }

        return !currentlyActive;
    }

    public synchronized void setLockable(@NotNull Material material, boolean lockable, @NotNull Player who) {
        boolean currentlyActive = isLockable(material) || isLockableEntity(material);
        if (currentlyActive == lockable) return;
        toggleLockable(material, who);
    }

    public synchronized void batchSetLockable(@NotNull List<Material> materials, boolean lockable, @NotNull Player who) {
        if (blocksConfig == null) return;
        Map<String, Set<Material>> activeByKey = new LinkedHashMap<>();

        for (String key : BlocksConfigStorage.LOCKABLE_LIST_KEYS) {
            BlockFamilyParser.Family family = BlocksConfigModifier.familyForConfigKey(key);
            if (family == null) continue;
            Object raw = blocksConfig.get(key);
            if (raw == null) {
                activeByKey.put(key, new LinkedHashSet<>());
                continue;
            }
            activeByKey.put(key, new LinkedHashSet<>(BlockFamilyParser.parse(raw, family)));
        }

        int changed = 0;
        for (Material mat : materials) {
            String configKey = configKeyForMaterial(mat);
            if (configKey == null) continue;
            BlockFamilyParser.Family family = BlocksConfigModifier.familyForMaterial(mat);
            if (family == null) continue;
            Set<Material> active = activeByKey.get(configKey);
            if (active == null) continue;
            boolean currentlyActive = active.contains(mat);
            if (currentlyActive == lockable) continue;
            if (lockable) {
                active.add(mat);
            } else {
                active.remove(mat);
            }
            changed++;
        }

        if (changed == 0) return;

        for (Map.Entry<String, Set<Material>> entry : activeByKey.entrySet()) {
            String configKey = entry.getKey();
            Set<Material> activeSet = entry.getValue();
            if (isKeyInExpressionFormat(configKey)) {
                BlockFamilyParser.Family family = BlocksConfigModifier.familyForConfigKey(configKey);
                String expr = family != null ? BlockFamilyParser.toFamilyExpression(activeSet, family) : null;
                if (expr != null) {
                    blocksConfig.set(configKey, List.of(expr));
                } else {
                    blocksConfig.set(configKey, activeSet.stream().map(Material::name).sorted().toList());
                }
            } else {
                blocksConfig.set(configKey, activeSet.stream().map(Material::name).sorted().toList());
            }
        }

        saveBlocksConfig();
        reloadBlocksAfterToggle();

        String actionText = lockable
            ? Translator.get(TranslationKey.ENABLED)
            : Translator.get(TranslationKey.DISABLED);
        BlockProtLogger.log("lockables-batch",
            actionText + " " + changed + " materials in batch (by " + who.getName() + ")");
        if (who.isOnline()) {
            ComponentMessages.sendLegacyActionBar(who, Translator.get(TranslationKey.MESSAGES__LOCKABLES__TOGGLE_FEEDBACK)
                .replace("{action}", actionText)
                .replace("{name}", String.valueOf(changed) + " blocks"));
        }
    }

    public synchronized void toggleFamily(
            @NotNull BlockFamilyParser.Family family,
            @NotNull String configKey,
            @NotNull String expression,
            @NotNull Player who) {
        if (blocksConfig == null) return;

        Object raw = blocksConfig.get(configKey);
        Set<Material> current = BlockFamilyParser.parse(raw, family);
        Set<Material> target = BlockFamilyParser.parse(expression, family);
        boolean allActive = target.stream().allMatch(current::contains);

        if (allActive) {
            current.removeAll(target);
        } else {
            current.addAll(target);
        }

        if (isKeyInExpressionFormat(configKey)) {
            String expr = BlockFamilyParser.toFamilyExpression(current, family);
            if (expr != null) {
                blocksConfig.set(configKey, List.of(expr));
            } else {
                blocksConfig.set(configKey, current.stream().map(Material::name).sorted().toList());
            }
        } else {
            blocksConfig.set(configKey, current.stream().map(Material::name).sorted().toList());
        }
        saveBlocksConfig();
        reloadBlocksAfterToggle();

        String actionText = allActive
            ? Translator.get(TranslationKey.DISABLED)
            : Translator.get(TranslationKey.ENABLED);
        BlockProtLogger.log("lockables-toggle",
            actionText + " " + family.name() + " via " + expression + " in " + configKey
                + " (by " + who.getName() + ")");
        if (who.isOnline()) {
            ComponentMessages.sendLegacyActionBar(who, Translator.get(TranslationKey.MESSAGES__LOCKABLES__TOGGLE_FAMILY_FEEDBACK)
                .replace("{action}", actionText)
                .replace("{family}", family.name()));
        }
    }

    public synchronized boolean toggleAutoDropMaterial(@NotNull Material material, @NotNull Player who) {
        if (blocksConfig == null) return isAutoDropToInventory(material);

        Set<Material> current = new LinkedHashSet<>(getAutoDropToInventoryBlocks());
        boolean currentlyActive = current.contains(material);
        String name = material.name();

        if (currentlyActive) {
            current.remove(material);
        } else {
            current.add(material);
        }

        writeAutoDropBlocks(current);

        String actionText = currentlyActive
            ? Translator.get(TranslationKey.DISABLED)
            : Translator.get(TranslationKey.ENABLED);
        BlockProtLogger.log("autodrop-toggle",
            actionText + " " + name + " in auto_drop_to_inventory"
                + " (by " + who.getName() + ")");
        if (who.isOnline()) {
            ComponentMessages.sendLegacyActionBar(who, Translator.get(TranslationKey.MESSAGES__AUTO_DROP__TOGGLE_FEEDBACK)
                .replace("{action}", actionText)
                .replace("{name}", name));
        }

        return !currentlyActive;
    }

    public synchronized boolean toggleAutoDropFamily(@NotNull BlockFamilyParser.Family family, @NotNull Player who) {
        if (blocksConfig == null) return false;

        Set<Material> current = new LinkedHashSet<>(getAutoDropToInventoryBlocks());
        Set<Material> members = BlockFamilyParser.getFamilyMembers(family);
        boolean allActive = members.stream().allMatch(current::contains);

        if (allActive) {
            current.removeAll(members);
        } else {
            current.addAll(members);
        }

        writeAutoDropBlocks(current);

        String actionText = allActive
            ? Translator.get(TranslationKey.DISABLED)
            : Translator.get(TranslationKey.ENABLED);
        BlockProtLogger.log("autodrop-toggle",
            actionText + " " + family.name() + " in auto_drop_to_inventory"
                + " (by " + who.getName() + ")");
        if (who.isOnline()) {
            ComponentMessages.sendLegacyActionBar(who, Translator.get(TranslationKey.MESSAGES__AUTO_DROP__TOGGLE_FAMILY_FEEDBACK)
                .replace("{action}", actionText)
                .replace("{family}", family.name()));
        }

        return !allActive;
    }

    private synchronized void writeAutoDropBlocks(@NotNull Set<Material> active) {
        if (blocksConfig == null) return;

        List<String> list;
        if (isModernFamilyBlocks()) {
            list = new ArrayList<>();

            boolean allActive = true;
            for (BlockFamilyParser.Family f : BlockFamilyParser.Family.values()) {
                if (!active.containsAll(BlockFamilyParser.getFamilyMembers(f))) {
                    allActive = false;
                    break;
                }
            }
            if (allActive) {
                list.add("[*]");
            } else {
                Set<Material> covered = new java.util.LinkedHashSet<>();
                for (BlockFamilyParser.SubFamily sf : BlockFamilyParser.SubFamily.values()) {
                    Set<Material> sfMembers = BlockFamilyParser.getSubFamilyMembers(sf);
                    if (sfMembers.isEmpty()) continue;
                    if (active.containsAll(sfMembers)) {
                        list.add("[*-" + sf.tag + "]");
                        covered.addAll(sfMembers);
                    }
                }
                for (Material m : active.stream().sorted().toList()) {
                    if (!covered.contains(m)) list.add(m.name());
                }
            }
        } else {
            list = active.stream().map(Material::name).sorted().toList();
        }

        blocksConfig.set("auto_drop_to_inventory.blocks", list);
        saveBlocksConfig();
        reloadBlocksAfterToggle();
    }

    @NotNull
    public static Material representativeMaterialForFamily(@NotNull BlockFamilyParser.Family family) {
        return BlocksConfigModifier.representativeMaterialForFamily(family);
    }
}
