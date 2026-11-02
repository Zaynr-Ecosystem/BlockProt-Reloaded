<div align="center">

<img src="https://raw.githubusercontent.com/Zaynr-Ecosystem/BlockProt-Reloaded/main/images/RELEASE%20TITLES/BlockProtReloaded.png" alt="BlockProt Reloaded" />

---
[![CI](https://img.shields.io/github/actions/workflow/status/Zaynr-Ecosystem/BlockProt-Reloaded/blockprot.yml?branch=main&style=flat-square&label=CI)](https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/actions/workflows/blockprot.yml)
[![Release](https://img.shields.io/github/v/release/Zaynr-Ecosystem/BlockProt-Reloaded?style=flat-square&color=brightgreen&label=Release)](https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/releases)
[![Modrinth](https://img.shields.io/modrinth/dt/sY9BsI1V?style=flat-square&color=00AF5C&logo=modrinth&label=Modrinth)](https://modrinth.com/plugin/blockprot-reloaded-legacy)
[![bStats](https://img.shields.io/bstats/servers/34385?style=flat-square&label=bStats)](https://bstats.org/plugin/bukkit/BlockProt%20Reloaded%20Legacy/34385)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square)](LICENSE)
[![GitLocalize](https://gitlocalize.com/repo/10833/whole_project/badge.svg)](https://gitlocalize.com/repo/10833)
[![Java](https://img.shields.io/badge/Java-21+-orange?style=flat-square)](https://openjdk.org/projects/jdk/21/)
[![Paper](https://img.shields.io/badge/Paper-1.20.5%20--%201.21.6-white?style=flat-square)](https://papermc.io/)

Java 21, Paper 1.20.5 through 1.21.6, Folia support, Bedrock Forms, Admin Tiers, MySQL index, Access Audit, Entity Protection, Villager Workstation Protection, Auto-Backup, Ownership Transfer, Item Frame and Vehicle Protection.

</div>

> **BlockProt Reloaded Legacy** is the edition for Minecraft 1.20.5 to 1.21.6. For Minecraft 1.21.7 and newer download BlockProt Reloaded. Looking for another Minecraft version? See the [version guide](docs/READ_MEs/VERSION_GUIDE.md).

> **Maintenance notice:** the maintainer is taking a break to focus on other projects. Issues and pull requests are welcome for any change and are reviewed as time allows. Read [CONTRIBUTING.md](CONTRIBUTING.md) first.

Block protection plugin for Paper, Purpur, and Folia servers. Players protect chests, furnaces, barrels, doors, trapdoors, item frames, vehicles, and workstations through chest menus, without typing commands. This fork keeps the original NBT storage and adds the features described below.

## Philosophy

BlockProt Reloaded exists because the best ideas come from the community that uses the plugin. If you have a feature request, a bug report, or want to discuss improvements, open an issue on GitHub or join the [Discord](https://discord.gg/RRcjuMr9Jd). If you are planning to contribute code or documentation, read [CONTRIBUTING.md](CONTRIBUTING.md), [SCOPE.md](SCOPE.md), and [AGENTS.md](AGENTS.md) before submitting an issue or pull request. Fragmentation into multiple forks helps nobody; every improvement belongs in one place, shared with everyone.

## Translating

Translations are managed through **[GitLocalize](https://gitlocalize.com/repo/10833)**.

[![GitLocalize](https://gitlocalize.com/repo/10833/whole_project/badge.svg)](https://gitlocalize.com/repo/10833)

The English file `translations_en.yml` is the primary reference. Missing keys fall back to English automatically without server errors.

Translations use [MiniMessage](https://docs.advntr.dev/minimessage/format.html) formatting (`<gold>`, `<bold>`, `<#a3c6eb>`, `<gradient:red:blue>`). Legacy `&` and `§` color codes are still accepted, so older translation files keep working. Write `\<player>` when a literal `<` must be shown.

### Languages on GitLocalize

These 25 languages are bundled in `lang/` and actively accepting contributions:

ar, cs, de, en, es, fi, fr, he, hu, id, it, ja, ko, nl, pl, pt-br, ro, ru, sk, sv, th, tr, uk, zh-CN, zh-TW.

Both legacy color codes (`&a`, `&6`) and Adventure MiniMessage format (`<gold>`, `<gradient:...>`) are fully supported.

## Screenshots

### Block Lock Menu
![Block lock](https://raw.githubusercontent.com/Zaynr-Ecosystem/BlockProt-Reloaded/main/images/main_menu.png)

The main interface for locking blocks. Sneak and right-click any lockable block to open it. The two-row inventory displays functional controls on the top row and utility options on the bottom row.

### Friend Settings
![Friend settings](https://raw.githubusercontent.com/Zaynr-Ecosystem/BlockProt-Reloaded/main/images/friend_settings.png)

Add or remove friends from your protected blocks. Every friend can open the block and move items in and out; the level (Basic, Operator, Full Manager, or Custom) decides who may also open its menu, change settings, or manage friends.

### Player Settings
![Player settings](https://raw.githubusercontent.com/Zaynr-Ecosystem/BlockProt-Reloaded/main/images/user_settings.png)

Configure personal preferences such as lock-on-place behavior, notification sounds, and action bar hint toggles.

### Redstone Settings
![Redstone settings](https://raw.githubusercontent.com/Zaynr-Ecosystem/BlockProt-Reloaded/main/images/redstone_settings.png)

Control redstone, piston, and hopper interaction for each of your protected blocks independently.

### Block Info
![Block info](https://raw.githubusercontent.com/Zaynr-Ecosystem/BlockProt-Reloaded/main/images/block_info.png)

View the owner, friend list, protection status, and metadata for any protected block.

### Access Log
![Access log](https://raw.githubusercontent.com/Zaynr-Ecosystem/BlockProt-Reloaded/main/images/access_log.png)

Summary of all access attempts to your protected blocks. The audit log records when friends opened or interacted with your blocks.

### Access Log Detail
![Inside log](https://raw.githubusercontent.com/Zaynr-Ecosystem/BlockProt-Reloaded/main/images/inside_log.png)

Each access record shows a timestamp, player name, and action type: opened, item taken, item placed, or access denied.

### Admin Player Block-List
![Admin view](https://raw.githubusercontent.com/Zaynr-Ecosystem/BlockProt-Reloaded/main/images/admin_view.png)

Admin inspection tool to view all blocks owned by any player, including offline players. Click any entry to teleport. Shows real block icons and lock timestamps.

## Installing

Download the latest `BlockProtReloadedLegacy` JAR from [Releases](https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/releases) (tags start with `bprl-`) and place it in your `plugins/` directory. Requires **Java 21+** and **Paper, Purpur, or Folia 1.20.5 - 1.21.6**. The Modrinth, CurseForge and Hangar pages come with the first release.

### Build from Source

```bash
git clone https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded.git
cd BlockProt-Reloaded
./gradlew :blockprot-spigot:shadowJar
```

```powershell
# Windows
.\gradlew.bat :blockprot-spigot:shadowJar
```

Output is placed at `spigot/build/libs/BlockProtReloaded-VERSION.jar`.

The version suffix is controlled by `versionSuffix` in `gradle.properties`:

| Value | Output |
|---|---|
| *(blank)* | `BlockProtReloadedLegacy-1.4.0.1.jar` - stable release |
| `BEDev` | `BlockProtReloadedLegacy-1.4.0.1-BEDev.jar` - experimental pre-release |
| `hotfix` | `BlockProtReloadedLegacy-1.4.0.1-hotfix.jar` - targeted bugfix release |

## File Layout

```
plugins/BlockProtReloaded/
├── config.yml                    Main configuration
├── blocks.yml                    Lockable block definitions and family expressions
├── worlds.yml                    Per-world overrides (optional, enabled by per_worlds_config)
├── admins.yml                    Admin tiers hierarchy and staff permissions (optional)
├── integrations.yml              Centralized third-party plugin integration settings
├── blockprot_usercache.sqlite    Player UUID and name resolution cache
├── mysql/
│   ├── mysql.yml                 MySQL / MariaDB storage index configuration
│   └── blockprot_audit.sqlite    SQLite access audit log
├── lang/
│   └── translations_*.yml        25 bundled language files
├── logs/
│   ├── blockprot-current.log     Active session log
│   └── blockprot-YYYY-MM-DD--YYYY-MM-DD.log   Rotated archives
└── backups/
    └── YYYY-MM-DD_HH-MM_vVERSION.zip   Created on version upgrade
```

`blocks.yml` is generated automatically on first start. If an existing `config.yml` contains legacy block lists, those values are automatically migrated to `blocks.yml`. The player UUID cache is stored safely inside the plugin folder.

## User Interfaces

BlockProt Reloaded provides complete interface parity across three display engines:

1. **Chest Inventory GUIs:** Classic inventory screens with click protection and pagination.
2. **Native Paper Dialogs:** not available in this edition. Every menu opens as a chest inventory. The `use_dialogs` setting is inert here and kept so a later move to BlockProt Reloaded activates it.
3. **Bedrock Forms (Cumulus API):** Native touch-friendly modal forms for players connecting via Geyser and Floodgate.

### Block Lock Menu

Opened by sneaking and right-clicking any lockable block.

**Top row - functional buttons:**

| Slot | Item | Function |
|---|---|---|
| 0 | Block icon | Lock or unlock toggle |
| 1 | Redstone | Redstone, piston, and hopper settings *(storage and traversal blocks)* |
| 2 | Player Head | Manage friends and permissions |
| 3 | Name Tag | Set a custom block display name |
| 4 | Ender Pearl | Transfer block ownership |
| 5 | Emerald | Locate linked villager *(workstation blocks only)* |

The redstone button is automatically hidden for display-only blocks such as signs, lecterns, and workstations. Button positions remain fixed so muscle memory is preserved across block types.

**Bottom row - utility buttons:**

| Slot | Item | Condition |
|---|---|---|
| 9 | Spyglass | Owner or staff, block has an inventory |
| 13 | Clock | Owner or staff, audit logger enabled |
| 14 | Knowledge Book | Manager role, clipboard contains copied settings |
| 15 | Paper | Manager role |
| 16 | Compass | Manager role or admin |
| 17 | Barrier | Always displayed (close menu) |

Copying settings sends an action bar confirmation. Pasting replaces the friend list rather than appending, preventing stale permission accumulation.

## Commands

Command visibility is controlled by `use_menus` in `config.yml`. When `use_menus: false`, all subcommands are available via chat. When `use_menus: true`, players only get `/bp user` and `/bp admin`; the console keeps every subcommand.

**Menu Commands:**

| Command | Permission | Description |
|---|---|---|
| `/bp user` | `blockprot.user` | Open personal user menu |
| `/bp admin` | `blockprot.user.admin` | Open administrative control menu |

**Player Commands:**

| Command | Permission | Description |
|---|---|---|
| `/bp help` | `blockprot.user` | Display available player commands |
| `/bp about` | `blockprot.user` | Display version and environment information |
| `/bp settings` | `blockprot.user` | Open personal preferences interface |
| `/bp friends` | `blockprot.user` | Manage global default friends list |
| `/bp friends addall <player>` | `blockprot.user` | Add a friend to every block you currently own |
| `/bp stats` | `blockprot.user` | Open block statistics and location list |
| `/bp transferall <player>` | `blockprot.user` | Transfer all owned blocks to another player (a single block is transferred from its lock menu) |
| `/bp disablehints` | `blockprot.user` | Toggle protection action bar hint messages |

**Admin Commands:**

With `admin_tiers.enabled: false` (the default) the Permission column applies, where "Admin" means op or `blockprot.user.admin`. With `admin_tiers.enabled: true` the Tier column applies instead: that tier or a higher one grants the command, ops count as Owner, and `blockprot.user.admin` alone reaches T2 commands only.

| Command | Permission | Tier | Description |
|---|---|---|---|
| `/bp info <player>` | Admin | T1 | Inspect all blocks owned by a player (online or offline); prints the list when run from the console |
| `/bp unlock <player>` | Admin | T2 | Inspect or unlock protections owned by a player; lists their locked blocks when run from the console |
| `/bp lockables` | Admin | T2 | Interactive browser for all known materials with toggleable active status |
| `/bp protdel` | Admin | T3 | Bulk delete protections in a world with undo support; from the console use `/bp protdel <world> confirm` and `/bp protdel undo` |
| `/bp debug <subcommand>` | Admin or `blockprot.debug` | T3 | Run the full diagnostic suite and class coverage scans; also runs from the console at world spawn |
| `/bp reload` | Admin | Owner | Create safety backup and reload configs, blocks, and translations |
| `/bp update` | Admin | Owner | Check GitHub releases for updates with channel detection |
| `/bp integrations` | Admin | Owner | Inspect status of all third-party integrations |
| `/bp recommended <blocks\|config\|all>` | op or `blockprot.user.admin.owner` | Owner | Apply recommended configuration profiles; `undo` reverts them |
| `/bp tiers [setrole] <player> <role>` | Admin or `blockprot.user.admin.owner` | Owner | Manage staff admin tiers (t1, t2, t3, owner, custom, none) and admins.yml |

The console can run every command.

## Permissions

| Permission | Default | Description |
|---|---|---|
| `blockprot.user` | true | Standard player features: lock, friends, settings, stats, transfer |
| `blockprot.user.admin` | op | Full administrator privileges and GUI inspection |
| `blockprot.user.admin.t1` | false | Staff Tier 1 (Moderator): player block lists, read-only block info and audit logs, teleport from admin lists |
| `blockprot.user.admin.t2` | false | Staff Tier 2 (Helper): unlock, break protected blocks, container bypass, lockables GUI |
| `blockprot.user.admin.t3` | false | Staff Tier 3 (Admin): T2 plus world protection deletion, config editor, and debug |
| `blockprot.user.admin.owner` | op | Staff Tier 4 (Owner): full control, reload, update, staff role assignments |
| `blockprot.user.admin.custom` | false | Custom staff role evaluating granular action flags from admins.yml |
| `blockprot.lockmax` | false | Exemption from the `player_max_locked_block_count` limit |
| `blockprot.locklimit.<N>` | false | Assigns a custom maximum locked block limit to the player |
| `blockprot.blocks.tp` | op | Allows teleportation to blocks from statistics and admin block lists |
| `blockprot.debug` | op | Access to `/bp debug` system diagnostics |

The tier nodes only take effect with `admin_tiers.enabled: true`. Every button of the `/bp admin` hub checks the tier of its action, so a staff member only uses what their tier allows.

## Key Features

### Modern Minecraft Compatibility
Compatible with Java 21. Supports Minecraft versions from 1.20.5 through 1.21.6. On a newer Minecraft version the plugin keeps running and warns once that another edition exists; on an older one it disables itself and points to the version guide.

### Native Paper Dialogs Parity
Not available in this edition. Every menu opens as a chest inventory.

### Four-Tier Staff Hierarchy (`admins.yml`)
Configurable four-tier staff management system (`admin_tiers.enabled: true`):
- **Tier 1 (Moderator):** Player block lists (`/bp info`), a read-only lock menu on other players' blocks with block info and the audit log, and teleporting from admin block lists.
- **Tier 2 (Helper):** Container unlock, container bypass, breaking protected blocks, and the lockables browser.
- **Tier 3 (Admin):** World-wide protection deletion, the configuration editor, world expiry, and diagnostics.
- **Tier 4 (Owner):** System reloads, updates, integrations, recommended profiles, and staff assignments.
- Custom roles can also be declared with granular action flags.

### Block Family Expressions
`blocks.yml` supports compact, expressive family expressions:
```yaml
lockable_tile_entities:
  - "[*]"                               # all tile entities
  - "[*-CHEST]"                         # only chest variants
  - "[* -*SIGN]"                        # all tile entities except signs
  - "[*-FURNACE *-CHEST]"              # furnaces and chests

lockable_shulker_boxes:
  - "[*-SHULKERS -WHITE_SHULKER_BOX]"   # all shulkers except white

lockable_entities:
  - "[*-CHEST_BOATS]"                  # all chest boat variants
  - "[*-CHEST_MINECARTS *-HOPPER_MINECARTS]"
```
Full documentation: [`docs/MODERN SYNTAX AND LEGACY/BLOCK_FAMILY_SYNTAX.md`](docs/MODERN%20SYNTAX%20AND%20LEGACY/BLOCK_FAMILY_SYNTAX.md).

### Item Frame and Vehicle Protection
Item frames, glowing item frames, chest boats, storage minecarts, and hopper minecarts are protected using the standard sneak-right-click interaction. Frames mounted directly onto locked containers link to the underlying container automatically, sharing ownership and friends without requiring duplicate protection.

### Villager Workstation Protection
Villagers whose job-site points to a protected workstation inherit its protection. Non-owners cannot trade with or damage the linked villager, and cannot break blocks in the configured radius around the workstation. An Emerald button in the workstation menu initiates a particle indicator pointing to the linked villager.

### High-Performance Hopper Protection
Hoppers are checked using a thread-safe Caffeine cache and `ProtectedBlockCache`, resolving server tick lag without needing to completely disable hopper extraction.

### Hybrid MySQL / SQLite Access Audit
Stores block protections in PersistentDataContainer / NBT on the block itself. An optional MySQL / MariaDB index mirrors block locations and trust lists so large servers can search them without scanning region files; NBT stays the source of truth. An asynchronous SQLite audit log (`blockprot_audit.sqlite`) records open, item take, item place, and access denied events.

### Per-World Configuration (`worlds.yml`)
Enable `per_worlds_config: true` to configure independent block whitelists, protection rules, and feature toggles per world.

### Inactivity Cleanup and World Expiry
Configurable automated cleanup (`inactivity_cleanup_days`) that unregisters protections belonging to players who have been offline for extended periods.

## Integrations

| Plugin | Notes |
|---|---|
| Towny | Respects town, plot, and nation boundaries; cleans unclaims; ruined town bypass |
| WorldGuard | Evaluates region flags and respects the `allow-blockprot` custom region flag |
| Lands | Honors claim permissions; wilderness protection restrictions |
| ClaimChunk | Restricts locking and container access to chunk owners |
| GriefPrevention | Honors claim ownership; prevents locking outside or inside foreign claims |
| Residence | Restricts locking and container interaction to residence owners |
| PlaceholderAPI | Exposes global and per-player block counts and default friend placeholders |
| SkinsRestorer | Resolves authentic player head textures on offline-mode servers asynchronously |
| WorldEdit / FAWE | Optional paste auto-lock for newly pasted structures |
| Floodgate / Geyser | Resolves Bedrock names and skins; native Bedrock touch forms (Cumulus) |
| ViaVersion | Client protocol version detection in the `/bp lockables` interface |
| Folia | Asynchronous chunk handling and region scheduler compatibility |

## Developer API

Other plugins can query and react to BlockProt protections. The API is published through [JitPack](https://jitpack.io/#VictorGugug/BlockProt-Reloaded); use a release tag (for example `1.3.8`) as the version.

Gradle (Kotlin DSL):

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.VictorGugug.BlockProt-Reloaded:blockprot-spigot:1.3.8") { isTransitive = false }
    compileOnly("com.github.VictorGugug.BlockProt-Reloaded:common:1.3.8")
}
```

Maven:

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.VictorGugug.BlockProt-Reloaded</groupId>
    <artifactId>blockprot-spigot</artifactId>
    <version>1.3.8</version>
    <scope>provided</scope>
</dependency>
```

Add `softdepend: [BlockProtReloaded]` (or `depend`) to your `plugin.yml`, then:

```java
BlockProtAPI api = BlockProtAPI.getInstance();
if (api != null && api.isProtected(block)) {
    UUID owner = api.getOwner(block);
    boolean allowed = api.canAccess(block, player.getUniqueId());
    List<UUID> friends = api.getFriends(block);
}
```

| Method | Description |
|---|---|
| `isProtected(Block)` / `getOwner(Block)` | Whether a block is locked and who owns it |
| `canAccess(Block, UUID)` | Whether a player may use the block (owner, friend, public, or unprotected) |
| `getFriends(Block)` / `isPublic(Block)` | Players the block is shared with, and whether it is public |
| `isLockable(Block)` | Whether the block type can be locked in its world |
| `isEntityProtected(Entity)` / `getEntityOwner(Entity)` | Entity protection state and owner |
| `lockBlock(Block, Player)` / `unlockBlock(Block, Player)` | Lock or unlock through the regular events |
| `registerIntegration(PluginIntegration)` | Add your own protection rules (claims, regions) |

Events in `de.sean.blockprot.bukkit.events`: `BlockProtLockEvent`, `BlockProtUnlockEvent`, `BlockAccessEvent`, `BlockAccessMenuEvent`, and `BlockLockOnPlaceEvent`. All five events are cancellable.

Call block and entity methods on the thread that owns them: the main thread on Paper, or the owning region thread on Folia.

## Configuration Overview

```yaml
# [Language]
language_file: translations_en.yml
fallback_string: "Unknown translation"
replace_translations: true

# [Worlds]
excluded_worlds: []
per_worlds_config: false           # enables per-world configuration via worlds.yml

# [Players and friends]
bedrock_username_prefixes:
  - "."
  - "*"
  - "_"
lock_on_place_by_default: true
public_is_friend_by_default: false
player_max_locked_block_count: -1  # -1 = unlimited
lock_hint_cooldown_in_seconds: 10
friend_search_similarity: 0.5
disable_friend_functionality: false

# [Blocks and locking behavior]
modern_family_blocks: false        # auto-convert flat lists to compact family expressions
redstone_disallowed_by_default: false
simplified_hopper_logic: false
protect_locked_blocks_from_explosions: true
block_protected_block_piston_movement: true
clear_protection_on_shulker_break: false
allow_break_protected_blocks: false
respect_spawn_protection: true
block_lock_effects: true
block_lock_sounds: true
use_menus: false                   # route commands into /bp user and /bp admin
use_dialogs: false                 # native Paper Dialog modals (not available in this edition, no effect)
timed_access_max_duration_days: 90
action_bar:
  duration_seconds: 6              # duration in seconds alerts persist

# [Entity protection]
entity_protection:
  enabled: false
  auto_protect_on_tame: true
  menu_item: STICK
  villager_locate_seconds: 6

villager_workstation_protection:
  enabled: true
  radius: 2
  vertical_radius: 1

# [Expiry]
world_expiry:
  enabled: false
  check_interval_minutes: 10
  worlds: {}

# [Raid detection]
raid_detection:
  enabled: false

# [Notifications]
notify_op_of_updates: false
owner_notifications:
  enabled: true
  notify_on_open: true
  notify_on_take: true
  notify_on_place: true

# [Maintenance]
inactivity_cleanup_days: -1        # -1 = disabled
auto_reload_configs: true          # automatic file watcher
auto_reload_delay_seconds: 2
enable_session_log: true
enable_backups: true
admin_tiers:
  enabled: false                   # 4-tier admin hierarchy and admins.yml
```

## Documentation and Release History

- **Release Notes:** Detailed version-by-version changelogs, migration guides, and upgrade notes are maintained in [`docs/RELEASE_NOTES/`](docs/RELEASE_NOTES/).
- **Block Configuration Reference:** Comprehensive guide on family expressions, sub-families, and material references is located in [`docs/MODERN SYNTAX AND LEGACY/`](docs/MODERN%20SYNTAX%20AND%20LEGACY/).

## Contact and Support

Maintained by **Zar**. [Open an issue](https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/issues) for bugs or feature suggestions.

## License

Licensed under the **GNU General Public License v3**. See [`LICENSE`](LICENSE) for details.

<sub>Based on <a href="https://github.com/spnda/BlockProt">BlockProt</a> by spnda. Original copyright notices preserved as required by GPL v3.</sub>
