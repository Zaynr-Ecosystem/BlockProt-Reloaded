<img src="https://raw.githubusercontent.com/Zaynr-Ecosystem/BlockProt-Reloaded/main/images/RELEASE%20TITLES/BlockProtReloaded.png" alt="BlockProt Reloaded" />

---
> **BlockProt Reloaded Legacy (Minecraft 1.20.5 - 1.21.6)**. Looking for another Minecraft version? See the [version guide](https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/blob/main/docs/READ_MEs/VERSION_GUIDE.md).

BlockProt Reloaded Legacy is a maintained fork of [BlockProt](https://github.com/spnda/BlockProt) for Paper, Purpur, and Folia servers (Minecraft 1.20.5 - 1.21.6). It preserves the original NBT-based protection core while adding stability fixes, performance improvements, and new features not present upstream.


## What it does

Players sneak and right-click any lockable block or entity to open the protection GUI: no commands required. From the GUI they can lock or unlock, manage a per-block friend list and make blocks public, control redstone and hopper access, inspect the access audit trail, and copy and paste settings between blocks.

Admins get a separate toolset: a paged block browser (`/bp lockables`), per-player block lists (`/bp info`), an unlock GUI (`/bp unlock`), staff roles management (`/bp tiers`), diagnostics (`/bp debug run`), and a full audit log per protected block.


## What is protected

Only blocks and entities that exist on your Minecraft version can be locked; the lists below name the complete set across editions.

**Tile entities (storage):** all chest variants, all 17 shulker box colors, all 12 shelf variants (1.21.9+), furnace, smoker, blast furnace, hopper, dispenser, dropper, barrel, brewing stand, decorated pot, chiseled bookshelf, crafter, jukebox, lectern, beehive, bee nest.

**Interactive blocks:** doors, trapdoors, and fence gates (all wood variants plus iron and all copper oxidation stages), anvils, cauldrons, enchanting table, grindstone, stonecutter, loom, cartography table, smithing table, composter, bell, note block, dragon egg.

**Entities:** item frames, glowing item frames, chest boats (all wood variants), chest minecarts, hopper minecarts. Tamed animals (wolves, cats, parrots, horses, llamas) and non-tameable protectable entities (villagers) via the entity protection system.

All block lists are defined in `blocks.yml` and can be changed at runtime without restarting.


## Features added in this fork

### Fixed lock menu layout
Every button in the Block Lock GUI now occupies a fixed slot across all block and entity types. Previously, a missing button (for example, no Redstone button on a workstation) caused all subsequent buttons to shift left, making the menu inconsistent between block types.

### Entity protection
Tamed animals (wolves, cats, parrots, horses, llamas) and villagers can be protected. Right-click your entity while holding the configured menu item (default: Stick) to open the protection GUI. Disabled by default; enable under `entity_protection` in `config.yml`. Renamed from `pet_protection` in earlier versions: the old key is migrated automatically on upgrade, no manual edit needed.

### Villager workstation protection
A villager whose job-site memory points to a protected workstation block inherits that block's protection automatically. Non-owners cannot damage or trade with the linked villager and cannot break or interact with blocks in a configurable area around the workstation. A "Locate linked villager" Emerald button appears in the workstation's lock menu, showing a short particle effect at the villager's location visible only to the clicking player. Configurable in `config.yml`:

```yaml
villager_workstation_protection:
  enabled: true
  radius: 2           # horizontal search radius (0-8)
  vertical_radius: 1  # vertical search radius (0-4)
```

### Item frame protection with automatic block linking
Item frames and glowing item frames are protected with the same sneak-and-right-click flow. A frame placed on the face of a lockable block is automatically linked to that block: it shares the block's owner, friends, and lock state exactly, and interacting with it opens the block's lock menu directly. Frames not mounted on a lockable block keep the standalone entity-protection flow.

### Storage vehicle protection
Chest boats, storage minecarts, and hopper minecarts support the same sneak-and-right-click flow. Once protected, non-owners cannot open the inventory, and the vehicle is protected from damage and destruction. Hopper extraction can be toggled per vehicle from the in-menu Redstone button.

### Block family expression system
`blocks.yml` and `worlds.yml` support compact family expressions alongside standard flat material lists:

```yaml
lockable_tile_entities:
  - "[*]"                        # all tile entities
  - "[*-CHEST -*SIGN]"           # all chests, no signs
lockable_entities:
  - "[*-CHEST_BOATS ITEM_FRAME]" # chest boats and item frames only
```

Sub-families: `CHEST`, `FURNACE`, `SHELF`, `TRANSPORT`, `MISC`, `SIGN` (tile entities); `SHULKERS`; `ANVIL`, `CAULDRON`, `WORKSTATION`, `TRAPDOOR`, `FENCE_GATE` (blocks); `DOORS`; `CHEST_BOATS`, `CHEST_MINECARTS`, `HOPPER_MINECARTS`, `ITEM_FRAMES` (entities).

Full syntax: [`docs/MODERN SYNTAX AND LEGACY/BLOCK_FAMILY_SYNTAX.md`](https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/blob/main/docs/MODERN%20SYNTAX%20AND%20LEGACY/BLOCK_FAMILY_SYNTAX.md). Full material list: [`docs/MODERN SYNTAX AND LEGACY/LOCKABLE_BLOCKS_REFERENCE.md`](https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/blob/main/docs/MODERN%20SYNTAX%20AND%20LEGACY/LOCKABLE_BLOCKS_REFERENCE.md).

### Config and blocks auto-merge on every reload
On every startup and `/bp reload`, missing keys are added from JAR defaults and renamed keys are migrated. Existing values: including every family expression already written in `blocks.yml`: are never overwritten or reordered. In legacy flat-list mode, new material entries from a newer JAR are appended automatically so new block types become lockable without manual edits.

### Raid detection
Monitors explosions near lockable blocks. Logs the event, alerts the owner via action bar and chat if online, or queues the alert for delivery at next login. Configurable via `raid_detection.enabled`.

### StatHandler stability
`saveFile()` was rewritten to use a temp-write, validate, backup, replace sequence with a fallback for filesystems that do not support atomic cross-directory moves (fixes `Failed to swap backup NBT file` on Windows). A Caffeine cache throttles the per-player stale-entry scan to once per 60 seconds per player. A chunk-load guard prevents synchronous chunk loads in `purgeStalePbsEntries`.

### Performance
- Caffeine cache on `HopperEventListener` for O(1) hopper early-exit via `ProtectedBlockCache`.
- `purgeStalePbsEntries` skips unloaded chunks entirely to prevent main-thread stalls.
- Audit log writes run asynchronously.

### Other additions
- Multi-tier staff permissions (`admin_tiers.enabled`) and standalone `admins.yml` hierarchy.
- Native Paper Dialogs: not available in this edition. Every menu opens as a chest inventory.
- Bedrock touch forms (Cumulus API via Geyser and Floodgate).
- Ownership transfer: `/bp transferall <player>` moves ownership of all your blocks to another player.
- Per-world configuration via `worlds.yml` (`per_worlds_config: true`).
- Auto-reload via `ConfigFileWatcher` (`auto_reload_configs`, default `true`).
- Automatic backup zip on version upgrade.
- Legacy folder migration: copies data from `BlockProt` or `BlockProtPlus` folders automatically on first boot after rename.
- SQLite usercache moved into the plugin data folder; old file is migrated automatically.
- Inactivity cleanup: removes protections owned by long-inactive players (`inactivity_cleanup_days`, disabled by default).
- WorldEdit paste auto-lock (disabled by default).
- PlaceholderAPI placeholders: `%blockprot_global_block_count%`, `%blockprot_own_block_count%`, `%blockprot_default_friends%`.
- Folia compatibility via FoliaLib.
- ViaVersion, ViaBackwards, and ViaRewind detection.


## Commands

| Command | Permission | Description |
|---|---|---|
| `/bp lockables` | `blockprot.user.admin` | Paged GUI listing all blocks and entities with active/inactive status |
| `/bp tiers` | `blockprot.user.admin` or `blockprot.user.admin.owner` | Staff roles GUI and custom permissions manager |
| `/bp info <player>` | `blockprot.user.admin` | All blocks owned by a player with teleport links |
| `/bp unlock <player>` | `blockprot.user.admin` | Inspect or remove any player's protections (console: lists them) |
| `/bp reload` | `blockprot.user.admin` | Reload all config files and merge missing keys |
| `/bp debug run` | `blockprot.user.admin` or `blockprot.debug` | Run internal diagnostics (also from the console) |
| `/bp transferall <player>` | `blockprot.user` | Transfer ownership of all your protected blocks to another player |


## Permissions

| Permission | Default | Description |
|---|---|---|
| `blockprot.user` | true | All standard player features |
| `blockprot.user.admin` | op | Admin commands and GUIs |
| `blockprot.user.admin.t1` | false | Low staff tier (block lists, read-only block info and audit logs) |
| `blockprot.user.admin.t2` | false | Medium staff tier (unlock, lockables, break protected blocks) |
| `blockprot.user.admin.t3` | false | High staff tier (world protection deletion, config, debug) |
| `blockprot.user.admin.owner` | op | Full owner control and staff role assignment |
| `blockprot.lockmax` | false | Exempt from the block count limit (unlimited) |
| `blockprot.locklimit.<N>` | false | Per-player cap override (e.g. `blockprot.locklimit.500`) |
| `blockprot.blocks.tp` | op | Teleport to blocks from stats and admin GUIs |
| `blockprot.debug` | op | Access to `/bp debug` |

The staff tier nodes only apply with `admin_tiers.enabled: true`.


## Compatibility

| | |
|---|---|
| Minecraft | 1.20.5 - 1.21.6 |
| Server software | Paper, Purpur, Folia |
| Java | Java 21+ required |
| MySQL | MySQL 8+, MariaDB 10.5+ (optional) |
| Languages | 25 languages on [GitLocalize](https://gitlocalize.com/repo/10833): AR, CS, DE, EN, ES, FI, FR, HE, HU, ID, IT, JA, KO, NL, PL, PT-BR, RO, RU, SK, SV, TH, TR, UK, ZH-CN, ZH-TW |


## Integrations

Towny, WorldGuard, Lands, ClaimChunk, GriefPrevention, Residence, PlaceholderAPI, SkinsRestorer, WorldEdit/FAWE, Floodgate/Geyser, ViaVersion/ViaBackwards/ViaRewind, Folia.


## Install

Place the JAR in `plugins/` and restart. Requires Java 21+ and Paper, Purpur, or Folia 1.20.5 - 1.21.6. On upgrade, config keys are migrated automatically: no manual edits required.


## Documentation

- Release history: [`docs/RELEASE_NOTES/`](https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/tree/main/docs/RELEASE_NOTES)
- Block family syntax: [`docs/MODERN SYNTAX AND LEGACY/BLOCK_FAMILY_SYNTAX.md`](https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/blob/main/docs/MODERN%20SYNTAX%20AND%20LEGACY/BLOCK_FAMILY_SYNTAX.md)
- Full lockable materials reference: [`docs/MODERN SYNTAX AND LEGACY/LOCKABLE_BLOCKS_REFERENCE.md`](https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/blob/main/docs/MODERN%20SYNTAX%20AND%20LEGACY/LOCKABLE_BLOCKS_REFERENCE.md)
- Administrator guide: [`docs/READ_MEs/ADMIN_GUIDE.md`](https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/blob/main/docs/READ_MEs/ADMIN_GUIDE.md)
- Player guide: [`docs/READ_MEs/PLAYER_GUIDE.md`](https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/blob/main/docs/READ_MEs/PLAYER_GUIDE.md)

Repository: https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded
Issues and contributions: https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/issues
License: GNU General Public License v3 - based on [BlockProt](https://github.com/spnda/BlockProt) by spnda.
