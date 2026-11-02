# BlockProt Reloaded: Administrator Guide

This guide covers installing, configuring, and operating BlockProt Reloaded on a
Minecraft server. Player-facing usage is covered in `PLAYER_GUIDE.md`.

**Related documentation:** `LOCKABLE_BLOCKS_REFERENCE.md` (every lockable material),
`BLOCK_FAMILY_SYNTAX.md` (family expression syntax), `LEGACY_BLOCKS_SYNTAX.md`
(flat-name syntax), the release notes in `docs/RELEASE_NOTES/`.

## 1. What BlockProt Reloaded does

BlockProt Reloaded protects the blocks and entities your players care about:
chests, furnaces, barrels, shulkers, doors, anvils, item frames, storage minecarts,
tamed animals, and more. Each protected block stores an owner and an optional list
of friends.

- Only the **owner** (and players the owner added as **friends**) can open, take
  from, or place into a protected block.
- **Everyone else** is blocked with a "You don't have permission" message, a sound,
  and particle effects (all configurable). The denial is recorded in the audit log.
- Protection survives chunk unloads, server restarts, and explosions. The owner
  data is stored on the block itself (NBT/PDC); the optional MySQL index only
  mirrors it (section 11).

## 2. Installation and first start

1. Put the JAR into your server's `plugins/` folder and start the server.
2. On the very first start, the plugin prints a **First Start Guide** in the console
   and sends it in-game to every operator who joins:
   1. Use `/bp lockables` in-game to configure lockable blocks.
   2. Or edit `blocks.yml` manually in the plugin folder.
   3. See the docs (this repository).
   4. Run `/bp recommended` in the console for a recommended configuration
      (section 5).
3. When a player right-clicks a protected block they have access to for the first
   time (before they have opened any BlockProt menu), they see a chat hint:
   *"You can protect your blocks by crouching and right-clicking your chests,
   furnaces, and more!"* The message is clickable and runs `/blockprot
   disablehints` to turn hints off (see `PLAYER_GUIDE.md`). Once a player opens
   any BlockProt menu or clicks that link, the hint never appears again for that
   player; until then, repeats are throttled by `lock_hint_cooldown_in_seconds`
   (default 10).

> **Fresh installs start with every lockable list empty.** Nothing is protected
> until you add blocks to the lockable lists. Upgrades from an older install keep
> their existing `blocks.yml`.

> **Players cannot add blocks themselves.** `/bp lockables` is restricted to
> operators and players with the `blockprot.user.admin` permission. Only an
> administrator decides which blocks are lockable.

## 3. Data folder: `plugins/BlockProtReloaded/`

The plugin data folder is `plugins/BlockProtReloaded/` (named after the plugin).

| File | Purpose |
|---|---|
| `config.yml` | Main configuration (section 10). |
| `blocks.yml` | Which blocks are lockable (section 6). |
| `worlds.yml` | Per-world lockable lists (used with `per_worlds_config: true`). |
| `lang/lang.yml` | Language enable/disable and completion percentages. |
| `lang/translations_*.yml` | Translation files (English and Spanish are maintained). |
| `mysql/mysql.yml` | Optional MySQL index (section 11). |
| `integrations.yml` | Settings for supported third-party plugins (section 13). |
| `mysql/blockprot_audit.sqlite` | Local SQLite audit log, stored in the `mysql/` subfolder (section 14). |
| `admins.yml` | Standalone admin roles and custom action permissions without LuckPerms (section 16). |
| `logs/blockprot-*.log` | Activity log written by the plugin. |
| `backups/` | Automatic ZIP backups (section 14). |

## 4. Two interface modes: menus vs. dialogs

BlockProt Reloaded offers two presentation styles, controlled by two independent
settings in `config.yml`:

| Setting | Default | What it does |
|---|---|---|
| `use_menus` | `false` | Consolidates all menus into two hub GUIs: `/bp user` (players) and `/bp admin` (admins). When `false`, every feature keeps its dedicated subcommand (`/bp settings`, `/bp friends`, ...). |
| `use_dialogs` | `false` | Uses Minecraft's **native dialog system** instead of chest inventories, anvils, and chat input. If the server does not expose the dialog API, the plugin logs a warning and auto-disables this setting. |

### What a "dialog" is

Minecraft added a native dialog system in 1.21.6. Paper exposes a server-side API
for it since 1.21.7. Instead of opening a fake chest inventory, the plugin renders
real client-side windowed menus with buttons, toggle rows, text input fields, and
pagination. Dialogs look cleaner and avoid the limitations of inventory GUIs
(anvil prompts, chat typing, and so on). The Paper dialog API is still marked
experimental upstream, so the plugin treats it as optional and falls back to
inventories whenever it is unavailable.

### What each mode gives you

- **Legacy inventory mode** (`use_dialogs: false`, works on any server software):
  all menus are chest inventories. Block settings are edited in a standard
  inventory. Free-text prompts (block names, friend search, transfer target,
  admin config values, world-expiry duration) always use **chat input**: type
  the value in chat and press Enter. Paper listens on its native chat event;
  every other server software (Spigot, CraftBukkit, hybrid forks) listens on
  the legacy Bukkit chat event instead, so the prompt works identically
  everywhere. The plugin no longer opens an anvil or sign GUI for any of these
  prompts (the anvil inventory was found to open and close in a loop and crash
  the client on some Spigot builds; chat input has no such failure mode).
- **Dialog mode** (`use_dialogs: true`, Paper 1.21.7+ only):
  the same menus are native dialog windows with proper buttons and text inputs.
- **Menu mode** (`use_menus: true`): tab-complete only shows `user` and `admin`.
  Individual subcommands are hidden from tab-complete (still typed directly if you
  know them, except console-only ones) and everything is reachable from the two
  hubs.
- **CLI mode** (`use_menus: false`, default): every feature has its own command.

### Per-player choice of dialogs vs. inventories

Players can also choose individually whether they prefer dialogs over inventories
in `/bp settings` (the "Prefer dialogs" toggle). This per-player setting only has
an effect when dialogs are enabled globally. The stored value comes from
`PlayerSettingsHandler.getPreferDialogs()`.

### Bedrock forms (Geyser and Floodgate)

Starting in 1.3.6, BlockProt Reloaded integrates native Bedrock forms (Cumulus) for
Bedrock Edition players connecting through Geyser and Floodgate. Bedrock users
automatically receive touch-friendly modal and simple forms for locking, settings,
friend management, statistics, and admin tools, eliminating inventory GUI quirks
on touchscreens. Bedrock players can toggle between native forms and Java inventory
menus at any time in `/bp settings` ("Prefer Bedrock forms").

Additionally, BlockProt Reloaded resolves authentic Bedrock player skins on player
skulls across menus, friend lists, and statistics by querying the official Geyser
Skin API (`https://api.geysermc.org/v2/skin/{xuid}`) using Floodgate XUIDs and
Xbox gamertags, cached locally with Caffeine.

The **recommended peak experience** on Paper 1.21.7+ is `use_menus: true` combined
with `use_dialogs: true`, and `/bp recommended config` enables exactly that.

## 5. The recommended commands

`/bp recommended` (console or in-game Owner tier) applies a sensible starting configuration
in two independent halves, so you can apply just what you want:

| Command | Writes to | What it writes |
|---|---|---|
| `/bp recommended blocks` | `blocks.yml` only | Lockable lists for chests/furnaces/transport/misc/shelf/sign families, all shulkers, anvils/cauldrons/workstations/trapdoors/fence gates/beds, all doors, item frames; auto-drop set to shulkers. |
| `/bp recommended config` | `config.yml` only | `modern_family_blocks: true`, `use_menus: true`, `use_dialogs: true`. |
| `/bp recommended all` | both files | Applies both halves in sequence. |

- Each half can be applied **only once**. Re-running an already-applied half does
  nothing and tells you it was already applied. This prevents silently overwriting
  manual edits you made since.
- To re-apply a half anyway, pass `force`:
  `/bp recommended blocks force`, `/bp recommended config force`, or
  `/bp recommended all force`.
- `/bp recommended` with no argument prints the usage line.
- **Undo:** `/bp recommended undo blocks`, `/bp recommended undo config`, or
  `/bp recommended undo all` reverts a half back to its shipped state. `undo
  blocks` clears every lockable list and the auto-drop list and resets
  `auto_drop_to_inventory.enabled` to `true`. `undo config` resets
  `modern_family_blocks`, `use_menus`, and `use_dialogs` to `false`. Each undo
  only works if the matching half was previously applied; otherwise it prints a
  no-op message. Console or in-game server owner (`blockprot.user.admin.owner`),
  like the apply half.
- Both halves trigger an auto-reload (respecting `auto_reload_delay_seconds`).

> Why two halves? `blocks` only touches which blocks can be locked, while `config`
> only touches the three settings that are not enabled by default. If you only want
> the block lists, you do not have to enable menus or dialogs, and vice versa.

## 6. `blocks.yml`: which blocks are lockable

```yaml
lockable_tile_entities:   # chests, furnaces, hoppers, barrels, shelves, signs, ...
lockable_shulker_boxes:   # all shulker box variants
lockable_blocks:          # anvils, cauldrons, workstations, trapdoors, fence gates, beds
lockable_doors:           # all wood types + iron
lockable_entities:        # item frames, storage minecarts, ...
auto_drop_to_inventory:   # enabled: true, blocks: [*-SHULKERS] after /bp recommended blocks
```

- Entries can be **flat material names** (`CHEST`) or **family expressions**
  (`[*-CHEST *-FURNACE]`). Expressions only work for the lists marked as families.
- The lockable lists you configure are **never rewritten** on startup or reload.
  The file itself may still be rewritten for structural merges, format
  conversion, and repair; only `/bp lockables` GUI toggles and
  `/bp recommended blocks` change your lists.
- **Auto drop to inventory:** any block or entity listed in
  `auto_drop_to_inventory.blocks` delivers its drops straight to the breaking
  player's inventory instead of the ground. The feature is enabled by default
  (`auto_drop_to_inventory.enabled: true`); it applies to every player, operators
  and admins included, and creative mode is always excluded.
  - *Container blocks (chests, trapped chests, furnaces, blast furnaces, smokers,
    dispensers, droppers, hoppers, barrels, brewing stands, chiseled bookshelves,
    decorated pots, crafters, and every other tile entity that holds an
    inventory):* the block item and its contents go into the inventory together.
    The contents are read from the block's snapshot, so a double chest delivers
    only the clicked half (its 27 slots). The other half stays where it is, so
    nothing is duplicated or lost.
  - *Double blocks (doors, beds, and any other Bisected block):* exactly one item
    is delivered, matching vanilla, and the verified complementary half is removed
    at the same time so no half is left floating behind.
  - *Shulker boxes:* a dedicated path keeps the box, its contents, and the
    protection data together on the item that goes into the inventory. An admin
    breaking another player's shulker clears the protection regardless of
    `clear_protection_on_shulker_break` (that setting only controls what happens
    when the owner breaks their own). Unprotected shulkers without an owner fall
    back to the regular break, they are not auto-dropped.
  - *Lockable entities (item frames, glow item frames, storage/hopper minecarts,
    chest boats):* the entity item plus the framed item or carried contents go to
    the breaker's inventory. A frame or vehicle broken by a projectile (arrow or
    trident) routes the drops to the shooter.
  - *Protection mirror:* a protected block or entity owned by another player is
    exempt when the break would be blocked by protection. In that case nothing is
    delivered to the non-owner's inventory, so there is no duplication while the
    block stays intact. The owner (or an admin) still gets the auto-drop as
    normal. When `allow_break_protected_blocks: true` the break is not blocked,
    so the drops do go to whoever breaks it.
  - *Counters:* the stat and hopper caches the plugin normally updates on a
    successful break are updated on the auto-drop path too, so owner counters and
    hopper tracking stay consistent.
  - *Auto-Drop screen:* `/bp lockables` -> Auto Drop lists the families. The
    search entry (a compass in the inventory, or the search button in dialog
    mode) accepts a block name and lists matching materials, ranked by relevance,
    one click per material exactly like the family pages. Search queries support
    localized aliases from `translations_*.yml` (such as Spanish "horno",
    "barril", "tolva") and automatically normalize accents and diacritics, matching
    terms without requiring exact vanilla IDs.
- **Per-world override:** with `per_worlds_config: true`, each world listed in
  `worlds.yml` takes full control of its own lists (always in family-expression
  syntax). Worlds without an entry keep using `blocks.yml`.

### The "GUI doesn't open" problem (most common question)

If a player sneaks and right-clicks a chest and **nothing happens**, the block is
not in the lockable lists. The lock menu only opens on blocks the plugin considers
lockable. Fix it in any of these ways:

1. Run `/bp lockables` (in-game, as operator) and toggle the block/material on. You
   can "select all" per category.
2. Edit `blocks.yml` manually and add the material (see the syntax docs above).
3. Run `/bp recommended blocks` in the console for the full recommended set.
4. Enable `modern_family_blocks` (or run `/bp recommended config`) so whole
   families (e.g. all chests) can be enabled with one expression.

## 7. Locking and interacting with blocks

### How players lock a block

1. Sneak (crouch) and **right-click the block with an empty hand**.
   - The block must be in the lockable lists (section 6).
   - With dialogs enabled, a native dialog opens; otherwise a chest inventory opens.
2. Click the **Lock** button (the block's own item, first slot).
3. Optionally add **friends** or make it **public** (`PLAYER_GUIDE.md`).

### The placement message

When a player places a block that is in the lockable lists, and `lock_on_place_by_default`
is enabled for that player (default) while they are not sneaking, the block is
locked automatically and the player sees **"Block protected!"** in the action bar.
The same confirmation appears when item frames and vehicles are auto-protected.

### Who can do what

| Action | Owner | Friend | Everyone else |
|---|---|---|---|
| Open / use the block | Yes | Yes | Blocked |
| Take items / place items | Yes | Yes | Blocked |
| Open the lock menu, change settings and name | Yes | Operator level and above | No |
| Manage friends | Yes | Full Manager level | No |
| Break the block | Yes (protection clears) | No | No (unless `allow_break_protected_blocks: true`) |
| See the owner | No | Yes (name shown in action bar) | No |

### What right-click does for the owner and everyone else

- **Owner / friend with access:** the block opens normally. A chat hint teaches
  the player how locking works (section 2) unless they already opened a BlockProt
  menu or disabled hints (`/bp disablehints`, or the Hints toggle in
  `/bp settings`).
- **Stranger:** the interaction is cancelled, a "You don't have permission" message
  is shown, a sound plays, and particles appear (all configurable). The denial is
  recorded in the audit log.

### The lock menu (buttons)

When a player sneaks and right-clicks a lockable block they own (or manage), they
get:

| Button | Action |
|---|---|
| Block icon (slot 0) | **Lock / Unlock** the block. |
| Redstone | **Block settings**: redstone interaction, piston movement, hopper access. |
| Player head | **Friends**: add/remove friends, make the block public. |
| Name tag | **Set a block name** (visible to everyone who has access). |
| Ender pearl | **Transfer** the block to another player. |
| Emerald | **Locate the villager** linked to this workstation (particles). |
| Spyglass | **Inspect contents** without opening (owner/admin only, storage blocks). |
| Clock | **Audit log** for this block (who did what, when). |
| Paper | **Copy** this block's configuration (owner/friends/name/...) to your clipboard. |
| Knowledge book | **Paste** a copied configuration onto this block. |
| Compass | **Block info**: owner, name, friends, redstone/hopper settings, linked item frame. |
| Barrier | Back / close. |

> Item frames, chest boats, storage minecarts, and hopper minecarts can be
> protected the same way. Interacting with a frame linked to a protected block
> opens the linked block's menu.

## 8. Entity protection: tamed animals and villagers

Entity protection is a separate system from block protection. It is **disabled by
default** (`entity_protection.enabled: false`); enable it in `config.yml` and
reload before your players can use it.

### How a player protects a pet

1. Tame an animal normally (wolf, cat, horse, parrot, and so on).
2. Hold the **menu item** (`entity_protection.menu_item`, default `STICK`) and
   right-click the tamed animal.
3. Only the entity's **owner** (the player who tamed it, matched against
   `Tameable#getOwnerUniqueId()`) or an admin (`blockprot.user.admin`) may open this
   menu; everyone else gets the "no permission" message and the interaction is
   cancelled.
4. The **Entity Settings** menu opens with four independent toggles:

| Toggle | Blocks |
|---|---|
| Entity protection (master switch for this entity) | Turns protection for this specific entity on/off. |
| Block damage | Other players (and their projectiles) cannot damage the entity. |
| Block interaction | Other players cannot right-click, feed, or otherwise interact with it. |
| Block leash | Other players cannot leash or unleash it. |
| Block pickup (parrot) | Other players cannot pick the entity up onto their shoulder (parrots). |

A protected entity also has its own **friends list** (add/remove friends, toggle
manager permission), reachable from the Entity Settings menu, mirroring the block
friends system but backed by `EntityNBTHandler` instead of block NBT.

- **Auto-protect on tame** (`entity_protection.auto_protect_on_tame`, default
  `true`): when a player tames an animal, protection is automatically enabled with
  them as the owner. This only fires if `entity_protection.enabled` is also `true`.
- **Owner death notification:** when a protected entity dies, its online owner
  gets a chat message naming the entity (its custom name if set, otherwise its
  type).
- **Protectable non-tameable entities:** starting in 1.3.6, entity protection expands
  beyond tameable mobs. In `blocks.yml`, `protectable_entities` configures entity
  types, with `VILLAGER` configured as the currently supported and compatible type.
  Players can right-click an unowned villager with the menu item (stick) to claim,
  protect, and manage it, preventing unauthorized trades and interactions from others.
- **Bypass:** the entity's owner and any player with `blockprot.user.admin` always
  bypass every protection toggle on that entity.
- **Villager workstations** (`villager_workstation_protection`, enabled by
  default independently of `entity_protection.enabled`): when a villager is
  protected, the plugin automatically protects any workstation block (lectern,
  cartography table, etc.) within `radius` (horizontal) and `vertical_radius`
  (vertical) blocks, so a protected villager cannot have its job site stolen by
  another player. `entity_protection.villager_locate_seconds` controls how long
  the "Locate Linked Villager" particle trail from the block lock menu lasts.

## 9. Raid detection

Raid detection watches for explosions (from TNT, creepers, wither skulls, and any
other `BlockExplodeEvent` / `EntityExplodeEvent`) that hit a **lockable** block,
whether or not that specific block happens to be protected. It is **disabled by
default** (`raid_detection.enabled: false`, toggleable live in `/bp admin` -> Config
-> Raid detection). This is a detection and alerting layer only: it does not stop
or reduce explosion damage. Whether a locked block actually survives an explosion
is controlled separately by `protect_locked_blocks_from_explosions` (section 10).

Starting in 1.3.6, raid detection checks `ProtectedBlockCache` before processing
blocks, ensuring explosions across unprotected terrain produce zero NBT overhead.
Rapid explosions against a player's protected blocks are debounced within a 5-second
cooldown per owner to prevent notification spam. In addition, action-bar alerts
persist reliably for 6 seconds (configurable via `action_bar.duration_seconds`).

When `raid_detection.enabled: true` and an explosion affects a lockable block:

1. A line is written to the session log for every lockable block hit, protected or
   not (useful for spotting raid patterns even on unprotected areas).
2. If the block **is protected**, an audit entry (`RAID_EXPLOSION`) is recorded
   for it, visible from that block's Audit Log button and via `/bp info`.
3. The block's **owner** is alerted:
   - **Online:** an action-bar alert appears immediately and stays visible for
     the configured action-bar duration (default 6 seconds), followed by a chat
     message with the block type, world, and coordinates. Players with
     `blockprot.blocks.tp` also get a clickable "[Teleport]" link.
   - **Offline:** the alert is queued in memory and delivered as a chat message
     the next time the owner joins (the teleport link is included only if they
     have `blockprot.blocks.tp` at join time). Queued alerts are not persisted to
     disk; a server restart before the owner rejoins clears them.

Enable this if you want owners to be notified when griefers try to blow up their
storage; leave it off (the default) if you would rather keep explosion-related
logging to a minimum, or if you already protect blocks from explosions entirely
via `protect_locked_blocks_from_explosions: true` and do not need the alerting
layer on top.

## 10. `config.yml`: every setting explained

### Language

| Setting | Default | Meaning | In-Game Route |
|---|---|---|---|
| `language_file` | `translations_en.yml` | Active translation file. | Dialog: `/bp admin -> Config -> Language`<br>Menu: `/bp admin -> Config -> Language` (slot 11) |
| `fallback_string` | `"Unknown translation"` | Text shown for a missing translation key. | Dialog: `/bp admin -> Config -> Language`<br>Menu: `/bp admin -> Config -> Language` (slot 13) |
| `replace_translations` | `true` | Automatically fill missing keys from the English file. | Dialog: `/bp admin -> Config -> Language`<br>Menu: `/bp admin -> Config -> Language` (slot 15) |

### Worlds

| Setting | Default | Meaning | In-Game Route |
|---|---|---|---|
| `excluded_worlds` | `[]` | Worlds where the plugin is completely disabled. | Dialog: `/bp admin -> Config -> Worlds`<br>Menu: `/bp admin -> Config -> Worlds` (slot 11) |
| `per_worlds_config` | `false` | Use per-world lockable lists from `worlds.yml`. | Dialog: `/bp admin -> Config -> Worlds`<br>Menu: `/bp admin -> Config -> Worlds` (slot 13) |

### Players and friends

| Setting | Default | Meaning | In-Game Route |
|---|---|---|---|
| `bedrock_username_prefixes` | `.`, `*`, `_` | Username prefixes treated as Bedrock (Geyser/Floodgate) players. | Dialog: `/bp admin -> Config -> Players`<br>Menu: `/bp admin -> Config -> Players` (slot 22) |
| `lock_on_place_by_default` | `true` | Auto-lock blocks when a player places them (see section 7). | Dialog: `/bp admin -> Config -> Players`<br>Menu: `/bp admin -> Config -> Players` (slot 10) |
| `public_is_friend_by_default` | `false` | Treat public blocks as friends everywhere. | Dialog: `/bp admin -> Config -> Players`<br>Menu: `/bp admin -> Config -> Players` (slot 11) |
| `player_max_locked_block_count` | `-1` | Max locked blocks per player (`-1` = unlimited). Exempt players with the `blockprot.lockmax` permission (unlimited) or `blockprot.locklimit.<N>` (a specific cap). | Dialog: `/bp admin -> Config -> Players`<br>Menu: `/bp admin -> Config -> Players` (slot 12) |
| `lock_hint_cooldown_in_seconds` | `10` | Seconds between hint messages. | Dialog: `/bp admin -> Config -> Players`<br>Menu: `/bp admin -> Config -> Players` (slot 13) |
| `friend_search_similarity` | `0.5` | Friend-search fuzziness (0.0 exact, 1.0 very loose). Search always checks every currently online player (even one who has never played before) plus every player in the offline-player cache, so a player who is online right now is never missed even if their name has not been written to the offline cache yet. | Dialog: `/bp admin -> Config -> Players`<br>Menu: `/bp admin -> Config -> Players` (slot 14) |
| `disable_friend_functionality` | `false` | Disable the friends system entirely. | Dialog: `/bp admin -> Config -> Players`<br>Menu: `/bp admin -> Config -> Players` (slot 15) |

### Blocks and locking behavior

| Setting | Default | Meaning | In-Game Route |
|---|---|---|---|
| `modern_family_blocks` | `false` | Format written by `/bp lockables`: `false` = flat names, `true` = family expressions. **Never** rewrites existing `blocks.yml` content. | Dialog: `/bp admin -> Config -> Blocks -> Behavior`<br>Menu: `/bp admin -> Config -> Blocks -> Behavior` (slot 12) |
| `redstone_disallowed_by_default` | `false` | Deny redstone interaction with locked blocks by default. | Dialog: `/bp admin -> Config -> Blocks -> Behavior`<br>Menu: `/bp admin -> Config -> Blocks -> Behavior` (slot 13) |
| `simplified_hopper_logic` | `false` | Simpler (faster) hopper logic for large servers. | Dialog: `/bp admin -> Config -> Blocks -> Behavior`<br>Menu: `/bp admin -> Config -> Blocks -> Behavior` (slot 14) |
| `protect_locked_blocks_from_explosions` | `true` | Locked blocks survive explosions. | Dialog: `/bp admin -> Config -> Blocks -> Locking`<br>Menu: `/bp admin -> Config -> Blocks -> Locking` (slot 11) |
| `block_protected_block_piston_movement` | `true` | Pistons cannot move locked blocks. | Dialog: `/bp admin -> Config -> Blocks -> Locking`<br>Menu: `/bp admin -> Config -> Blocks -> Locking` (slot 12) |
| `clear_protection_on_shulker_break` | `false` | Clear protection when a shulker box is broken. Note: when an **admin** breaks another player's shulker, protection is always cleared regardless of this setting. | Dialog: `/bp admin -> Config -> Blocks -> Locking`<br>Menu: `/bp admin -> Config -> Blocks -> Locking` (slot 13) |
| `allow_break_protected_blocks` | `false` | Anyone may break protected blocks (protection data stays on the item). | Dialog: `/bp admin -> Config -> Blocks -> Locking`<br>Menu: `/bp admin -> Config -> Blocks -> Locking` (slot 14) |
| `respect_spawn_protection` | `true` | Respect vanilla spawn protection. | Dialog: `/bp admin -> Config -> Blocks -> Locking`<br>Menu: `/bp admin -> Config -> Blocks -> Locking` (slot 15) |
| `block_lock_effects` | `true` | Particles when interacting with a locked block. | Dialog: `/bp admin -> Config -> Blocks -> Effects`<br>Menu: `/bp admin -> Config -> Blocks -> Effects` (slot 11) |
| `block_lock_sounds` | `true` | Sounds when interacting with a locked block. | Dialog: `/bp admin -> Config -> Blocks -> Effects`<br>Menu: `/bp admin -> Config -> Blocks -> Effects` (slot 12) |
| `action_bar.duration_seconds` | `6` | Duration in seconds that action-bar notifications remain visible on screen. | Dialog: `/bp admin -> Config -> Blocks -> Effects`<br>Menu: `/bp admin -> Config -> Blocks -> Effects` (slot 22) |
| `use_menus` | `false` | Consolidate menus into `/bp user` and `/bp admin`. | Dialog: `/bp admin -> Config -> Blocks -> Effects`<br>Menu: `/bp admin -> Config -> Blocks -> Effects` (slot 13) |
| `use_dialogs` | `false` | Use Paper's native dialog system (Paper 1.21.7+ only; auto-disables otherwise). | Dialog: `/bp admin -> Config -> Blocks -> Effects`<br>Menu: `/bp admin -> Config -> Blocks -> Effects` (slot 14) |
| `timed_access_max_duration_days` | `90` | Editable in the admin config dialog (`/bp admin` -> Config), but has no functional effect yet (reserved for a future timed-access feature). | Dialog: `/bp admin -> Config -> Blocks -> Effects`<br>Menu: `/bp admin -> Config -> Blocks -> Effects` (slot 15) |

### Entity protection

| Setting | Default | Meaning | In-Game Route |
|---|---|---|---|
| `entity_protection.enabled` | `false` | Enable protection for tamed entities. | Dialog: `/bp admin -> Config -> Entity`<br>Menu: `/bp admin -> Config -> Entity` (slot 10) |
| `entity_protection.auto_protect_on_tame` | `true` | Auto-protect newly tamed animals. | Dialog: `/bp admin -> Config -> Entity`<br>Menu: `/bp admin -> Config -> Entity` (slot 11) |
| `entity_protection.menu_item` | `STICK` | Item used to open the entity protection menu. | Dialog: `/bp admin -> Config -> Entity`<br>Menu: `/bp admin -> Config -> Entity` (slot 12) |
| `entity_protection.villager_locate_seconds` | `6` | Seconds to locate a villager via particles. | Dialog: `/bp admin -> Config -> Entity`<br>Menu: `/bp admin -> Config -> Entity` (slot 13) |
| `villager_workstation_protection.enabled` | `true` | Auto-protect workstations of protected villagers. | Dialog: `/bp admin -> Config -> Entity`<br>Menu: `/bp admin -> Config -> Entity` (slot 14) |
| `villager_workstation_protection.radius` | `2` | Horizontal radius. | Dialog: `/bp admin -> Config -> Entity`<br>Menu: `/bp admin -> Config -> Entity` (slot 15) |
| `villager_workstation_protection.vertical_radius` | `1` | Vertical radius. | Dialog: `/bp admin -> Config -> Entity`<br>Menu: `/bp admin -> Config -> Entity` (slot 16) |

### Admin tiers

| Setting | Default | Meaning | In-Game Route |
|---|---|---|---|
| `admin_tiers.enabled` | `false` | When true, enables the 4-tier admin hierarchy (`t1`, `t2`, `t3`, `owner`) and granular action permissions instead of the single legacy `blockprot.user.admin` permission (section 16). | Dialog: `/bp admin -> Config -> Maintenance`<br>Menu: `/bp admin -> Config -> Maintenance` (slot 22) |

### Expiry

| Setting | Default | Meaning | In-Game Route |
|---|---|---|---|
| `world_expiry.enabled` | `false` | Enable world-level expiry of inactive protections. | Dialog: `/bp admin -> Config -> Expiry`<br>Menu: `/bp admin -> Config -> Expiry` (slot 12) |
| `world_expiry.check_interval_minutes` | `10` | How often to check for expirations. | Dialog: `/bp admin -> Config -> Expiry`<br>Menu: `/bp admin -> Config -> Expiry` (slot 14) |
| `world_expiry.worlds` | `{}` | Per-world expiry times. | Dialog: `/bp admin -> Config -> Expiry -> Per-World Expiry`<br>Menu: `/bp admin -> World Expiry` (slot 12) |

> Per-block manual expiry is planned for a future release and is not available yet.

### Raid detection

| Setting | Default | Meaning | In-Game Route |
|---|---|---|---|
| `raid_detection.enabled` | `false` | Detect and alert on possible raid attempts against locked blocks (section 9). Off by default; turn it on once you have decided how you want raid alerts handled on your server. | Dialog: `/bp admin -> Config -> Raid`<br>Menu: `/bp admin -> Config -> Raid` (slot 13) |

### Notifications

| Setting | Default | Meaning | In-Game Route |
|---|---|---|---|
| `notify_op_of_updates` | `false` | Notify OPs when an update is available. | Dialog: `/bp admin -> Config -> Notifications`<br>Menu: `/bp admin -> Config -> Notifications` (slot 12) |
| `owner_notifications.enabled` | `true` | Allow owner notifications. | Dialog: `/bp admin -> Config -> Notifications`<br>Menu: `/bp admin -> Config -> Notifications` (slot 14) |
| `owner_notifications.notify_on_open` | `true` | Notify owner when someone opens their block. | Dialog: `/bp admin -> Config -> Notifications`<br>Menu: `/bp admin -> Config -> Notifications` (slot 20) |
| `owner_notifications.notify_on_take` | `true` | Notify owner when someone takes items. | Dialog: `/bp admin -> Config -> Notifications`<br>Menu: `/bp admin -> Config -> Notifications` (slot 22) |
| `owner_notifications.notify_on_place` | `true` | Notify owner when someone places items. | Dialog: `/bp admin -> Config -> Notifications`<br>Menu: `/bp admin -> Config -> Notifications` (slot 24) |

Update notifications are channel-aware: the plugin tells you whether the
available update is a stable release, an important bug-fix (hotfix, stating
the release it corrects), or an experimental development build (BEDev).
Experimental builds are never announced on stable servers, and stable servers
only ever see stable-channel updates. On startup, the console prints which
channel the running build is: stable, hotfix, or experimental BEDev.

### Maintenance

| Setting | Default | Meaning | In-Game Route |
|---|---|---|---|
| `inactivity_cleanup_days` | `-1` | Remove protections after this many days of inactivity (`-1` = disabled). | Dialog: `/bp admin -> Config -> Maintenance`<br>Menu: `/bp admin -> Config -> Maintenance` (slot 11) |
| `auto_reload_configs` | `true` | Automatically reload when files change externally. | Dialog: `/bp admin -> Config -> Maintenance`<br>Menu: `/bp admin -> Config -> Maintenance` (slot 12) |
| `auto_reload_delay_seconds` | `2` | Quiet period (0-5 s) after a file change before reloading; batches rapid saves. | Dialog: `/bp admin -> Config -> Maintenance`<br>Menu: `/bp admin -> Config -> Maintenance` (slot 13) |
| `enable_session_log` | `true` | Write a session header to the log at startup. | Dialog: `/bp admin -> Config -> Maintenance`<br>Menu: `/bp admin -> Config -> Maintenance` (slot 14) |
| `enable_backups` | `true` | Create automatic backups. | Dialog: `/bp admin -> Config -> Maintenance`<br>Menu: `/bp admin -> Config -> Maintenance` (slot 15) |

## 11. Storage: NBT vs. MySQL

- **Default (no setup needed):** protection data is stored in the block's own
  NBT/PDC tags. It survives restarts and chunk reloads and lives on the server
  where the block is.
- **MySQL index (optional):** mirrors block locations and player trust lists in
  MySQL / MariaDB so large servers can search and audit them without scanning
  region files. NBT stays the source of truth; the index does not replace it.
  Edit `mysql/mysql.yml`:

```yaml
mysql:
  enabled: true
  host: "127.0.0.1"
  port: 3306
  database: "blockprot"
  username: "blockprot"
  password: ""
  # jdbc_url: "jdbc:mysql://..."   # optional: overrides host/port/database
```

Then run `/bp reload`. The pool uses HikariCP (`maximum_pool_size`, `minimum_idle`,
`connection_timeout_ms`).

### Player persistent data (PDC / NBT)

Starting in 1.3.6, player preferences, friend definitions, search history, and admin settings are stored cleanly in the player's PersistentDataContainer under a single root `blockprot` compound (`BukkitValues/blockprot/...`):

- `preferences`: boolean toggles for auto-lock on place, dialog preferences, Bedrock form preferences, notification toggles, colorblind mode (`✔` allowed/active, `✖` blocked/inactive, `●` selected, `○` unselected), and menu interaction status.
- `friends`: friend UUID compounds containing individual permission levels and granular flags.
- `history`: recent friend and player search history.
- `admin`: admin tier assignment and custom permission flags for servers managing staff without a permissions plugin.

Legacy flat root tags are migrated automatically to these nested subcompounds upon login, and the old keys are safely removed from the player's root tag list so tools like NBTExplorer display a clean, well-organized hierarchy with zero data loss.

## 12. Languages

- `lang/lang.yml` lists all bundled languages; set each to `true` to enable it or
  `false` to skip parsing it. The language selected via `language_file` in
  `config.yml` is always loaded regardless.
- **English and Spanish are maintained** and always complete. The other bundled
  files are community translations that may have missing keys (the `K: 0%`
  comments show completion after a reload).
- `config.yml`'s `language_file: translations_es.yml` switches the server
  language.

## 13. Integrations (`integrations.yml`)

The plugin auto-detects supported plugins when they are present (soft-depend):
**Towny, PlaceholderAPI, WorldGuard, Lands, ClaimChunk, ViaVersion, ViaBackwards**,
plus Residence and GriefPrevention support through their APIs.

| Integration | What it does |
|---|---|
| WorldGuard | Respects regions via the `allow-blockprot` region flag (`enable_flag_functionality`). |
| Lands | `allow_protecting_containers_in_wilderness`, `require_protect_for_friends_flag`. |
| Towny | `cleanup_plots_after_unclaim`, bypass protection in ruined towns. |
| ClaimChunk | `restrict_access_to_chunk_owner`. |
| WorldEdit | Master toggle `enabled`; `paste_autolock.enabled` (default `false`), radius, max blocks per paste, delay. |
| PlaceholderAPI | Placeholder support for other plugins. |
| ViaVersion / ViaBackwards | Multi-version client support. |
| Residence | `restrict_access_to_residence_owner`. |
| GriefPrevention | `restrict_access_to_claim_owner`. |

Run `/bp integrations` to list which are active on your server.

## 14. Reloading, backups, and logs

- **`/bp reload`** (who may run it: section 15): creates a backup first, then reloads config,
  translations, and block lists in one atomic quiet-period reload. Per-key change
  diffs go to the session log; the console prints "Reload completed."
- **Auto-reload:** when `auto_reload_configs: true`, editing any config file with a
  text editor triggers a reload after the quiet period. You never need to restart.
- **Backups:** with `enable_backups: true`, the plugin writes ZIP backups to
  `plugins/BlockProtReloaded/backups/` (keeps the last 10). A backup is also
  created before every manual `/bp reload`, and automatically at startup when
  a data migration is performed or when the running build is an experimental
  (BEDev/snapshot) pre-release, whose storage code may not be battle-tested
  yet.
- **Logs:** `plugins/BlockProtReloaded/logs/blockprot-*.log` records commands,
  reloads, denials, and protection decisions, with colors stripped for easy reading.
- **Audit log:** denials and block activity are recorded in
  `plugins/BlockProtReloaded/mysql/blockprot_audit.sqlite` and can be inspected in-game with `/bp logs [here|player <name>|<page>]` or via Paper dialogs.

## 15. Command reference

### Player commands

| Command | Permission | What it does |
|---|---|---|
| `/bp` | `blockprot.user` | Opens the user menu (menu mode) or the help page (CLI mode). |
| `/bp user` | `blockprot.user` | User menu hub: settings, friends, placements, transfer, about. |
| `/bp settings` | `blockprot.user` | Personal settings (lock on place, hints, dialogs, notifications). |
| `/bp friends` | `blockprot.user` | Default-friends menu. |
| `/bp friends addall <player>` | `blockprot.user` | Add a friend to all your blocks. |
| `/bp stats` | `blockprot.user` | Your protected-block statistics and block list. |
| `/bp transferall <player>` | `blockprot.user` | Transfer all your blocks to another player. |
| `/bp about` | `blockprot.user` | Plugin and fork information. |
| `/bp disablehints` | `blockprot.user` | Disable the lock hint messages. |
| `/bp logs [here|player <name>|<page>]` | `blockprot.user` | Paginated access audit log for your blocks, the targeted block (`here`), or another player (admin). |

### Admin commands

"Admin" means OP or `blockprot.user.admin`. That is the rule with
`admin_tiers.enabled: false` (the default). With `admin_tiers.enabled: true`
the Tier column applies instead: that tier or a higher one grants the command,
OPs count as Owner, and `blockprot.user.admin` alone reaches T2 commands only.

| Command | Access | Tier | What it does |
|---|---|---|---|
| `/bp admin` | Admin | any tier; each button checks its own tier | Admin menu hub: lockables, config editor, reload, update, integrations, stats, debug, info, about, world expiry, world protection deletion. Auto-Drop lives under `/bp lockables` -> Auto Drop, not the admin hub. |
| `/bp tiers [setrole] [player] [tier]` | Admin or `blockprot.user.admin.owner` | Owner | Staff roles menu (chest inventory or dialog). Without arguments, opens the staff management GUI. With arguments, assigns an admin tier (`t1`, `t2`, `t3`, `owner`, `custom`, `none`) to a player when `admin_tiers.enabled: true`. |
| `/bp lockables` | Admin | T2 | Browse and toggle which blocks are lockable (the GUI writes `blocks.yml`). This is the only in-game way to add lockable blocks; regular players cannot use it. |
| `/bp info <player>` | Admin | T1 | Opens a player's block list. From the console, prints the owned blocks. |
| `/bp unlock <player>` | Admin | T2 | Opens a GUI to unlock/remove protections for a player. From the console, lists the player's locked blocks. |
| `/bp protdel [world]` | Admin | T3 | Delete all protections in a world (with confirmation). From the console: `/bp protdel <world> confirm`, and `/bp protdel undo` to restore the last deletion. |
| `/bp reload` | Admin | Owner | Reload configuration (always creates a backup first). |
| `/bp update` | Admin | Owner | Check for updates. |
| `/bp integrations` | Admin | Owner | List active integrations. |
| `/bp debug` | Admin or `blockprot.debug` | T3 | Developer diagnostics. From the console it runs at world spawn; player-only screens are reported as SKIP. |
| `/bp recommended blocks` / `config` / `all` | OP or `blockprot.user.admin.owner` | Owner | Apply the recommended `blocks.yml`, `config.yml`, or both (section 5). |

> In menu mode (`use_menus: true`), player-facing subcommands are hidden from
> tab-complete and reachable via `/bp user` and `/bp admin`. The console always has
> access to every command.

## 16. Permissions

### Core permissions

| Node | Default | Meaning |
|---|---|---|
| `blockprot.user` | `true` (everyone) | All standard player features. |
| `blockprot.user.admin` | `op` | Admin node: full admin features, breaking any protected block (clearing protection), bypassing entity protection and the inactivity cleanup; implies `blockprot.user`. With `admin_tiers.enabled: true` it reaches T2 actions only. |
| `blockprot.lockmax` | `false` | Exempt from the per-player block cap (unlimited locked blocks). |
| `blockprot.locklimit.<N>` | `false` | Override the per-player cap with a specific number (e.g. `blockprot.locklimit.500` = max 500). Highest granted value wins. |
| `blockprot.blocks.tp` | `op` | Teleport to a protected block from the statistics inventory. |
| `blockprot.debug` | `op` | Run `/bp debug` diagnostics. |

> `blockprot.locklimit.<N>` is a dynamic node: replace `<N>` with a number (e.g.
> `blockprot.locklimit.500`). The old `blockprot.max_blocks` node is deprecated
> and has no effect.

### Multi-tier admin hierarchy (`admin_tiers.enabled: true`)

Starting in 1.3.6, setting `admin_tiers.enabled: true` in `config.yml` activates a 4-tier role hierarchy plus custom permissions. The tier nodes default to `false`; grant them with a permissions plugin, `/bp tiers`, or `admins.yml`:

| Node | Tier name | Allowed actions |
|---|---|---|
| `blockprot.user.admin.t1` | Low (Moderator) | Inspection only: player block lists (`/bp info` and the hub), a read-only lock menu on other players' blocks with block info and the audit log, and teleporting from admin block lists and the audit log. |
| `blockprot.user.admin.t2` | Medium (Helper) | All T1 actions plus breaking/unlocking protected blocks (`/bp unlock`), bypassing container protection on open, and configuring lockables (`/bp lockables`). |
| `blockprot.user.admin.t3` | High (Admin) | All T2 actions plus mass world deletion (`/bp protdel`), the configuration editor and world expiry in `/bp admin`, and debugging (`/bp debug`). |
| `blockprot.user.admin.owner` | Owner | Full control: all T3 actions plus system reload (`/bp reload`), update checking (`/bp update`), integrations (`/bp integrations`), assigning staff roles (`/bp tiers setrole`), and running `/bp recommended` in-game. |
| `blockprot.user.admin.custom` | Custom | Evaluates granular action flags configured in player NBT or `admins.yml`. |

### Managing staff without a permissions plugin (`admins.yml`)

If your server runs without LuckPerms or another permissions plugin, you can manage admin tiers directly in-game:

- Use `/bp tiers` to open the Staff Roles management menu (chest inventory or native Paper dialog).
- Clicking a player opens their role selector, showing their current tier, server operator status, and available roles (`None`, `Low (Moderator)`, `Medium (Helper)`, `High (Admin)`, `Owner`, `Custom`).
- The `Custom` role allows fine-grained toggle of 15 administrative action permissions:
  - In Chest Inventory GUI: left-click selects the Custom role and immediately opens the permissions configuration menu (`AdminCustomFlagsInventory`); right-click opens the configuration menu directly to view or edit existing custom permissions without reassigning.
  - In Paper Dialogs: two distinct buttons are provided ("Custom" to assign and open permissions; "Config" to open permissions directly).
  - Inside the custom permissions menu, clicking any of the 15 actions toggles that flag on or off for the player, persisting immediately to player NBT and `admins.yml`.
- Alternatively, use `/bp tiers setrole <player> <tier>` (or `/bp tiers <player> <tier>`, requires OP or `owner` tier) to assign a staff member's tier directly from chat or console.
- Roles assigned this way are stored in the player's persistent NBT (`admin/tier`, `admin/custom_flags`) and mirrored to `admins.yml` in the plugin folder.
- When `admin_tiers.enabled: true`, the plugin checks LuckPerms / Bukkit permissions first, then falls back to `admins.yml` and player NBT, ensuring offline staff remain recognized.

## 17. Troubleshooting

| Problem | Cause | Solution |
|---|---|---|
| Sneak + right-click does nothing | Block is not in the lockable lists | `/bp lockables` and toggle it on, add it to `blocks.yml`, or run `/bp recommended blocks` (console). |
| The hint message never appears | The player has already opened a BlockProt menu or disabled hints (the hint only shows until then) | It can be re-enabled: `/bp settings` and toggle Hints on. |
| "You don't have permission" on your own block | You are not the owner (or a friend) | Check the block info (Compass in the lock menu) for the owner; ask them to add you as a friend. |
| Can't open a block after restart | Protection was stored in NBT and the block was replaced/removed | Broken blocks lose their protection by design. Shulkers keep protection on the item if `clear_protection_on_shulker_break: false` (default). |
| Shulker box (with its items) drops on the ground when broken | `auto_drop_to_inventory.blocks` is empty, or `auto_drop_to_inventory.enabled` is `false` | Run `/bp recommended blocks` or add `[*-SHULKERS]` to `auto_drop_to_inventory.blocks` via `/bp lockables` -> Auto Drop so the box goes straight into the breaker's inventory (its contents and protection stay on the item). |
| A chest/furnace/dispenser drops its contents on the ground when broken | The block is not in `auto_drop_to_inventory.blocks` | Add it via `/bp lockables` -> Auto Drop (or the search entry). The feature delivers the block item and its contents together; a double chest delivers only the half that was broken. |
| Door or bed leaves one half floating, or drops two items | This was a bug in versions before the auto-drop rewrite | Update to the current build. One item is delivered and the verified complementary half is removed automatically. |
| Dialogs don't appear | Server is not Paper 1.21.7+ | Dialogs auto-disable; the plugin falls back to inventories and logs `CONSOLE__DIALOGS_UNAVAILABLE`. |
| Config changes are ignored | Auto-reload off, or edited during the quiet period | Run `/bp reload`, or set `auto_reload_configs: true`. |
| WorldEdit paste is not protected | `paste_autolock` is off | Set `integrations.yml` to `worldedit.paste_autolock.enabled: true`. |
| Players exceed the block limit | `player_max_locked_block_count` | Raise the cap or grant `blockprot.locklimit.<N>` / `blockprot.lockmax`. |
| Right-clicking a tamed animal with a stick does nothing | `entity_protection.enabled` is `false` (the default) | Set it to `true` in `config.yml` (or via `/bp admin` -> Config -> Entity protection) and reload. |
| Owners are not getting raid alerts | `raid_detection.enabled` is `false` (the default) | Set it to `true` in `config.yml` (or via `/bp admin` -> Config -> Raid detection) and reload. |
| A player says friend/pet-friend search finds nobody even though the target is online | Prior to 1.3.4-hotfix, entity (pet) friend search and one dialog-based block friend search used a network/exact-name profile lookup instead of the local player list, and offline-player-cache-only search could occasionally lag a fresh join | Update to 1.3.4-hotfix or later. All friend-search paths (block, entity, and both dialog variants) now check the currently online player list first, so an online player is always found regardless of `friend_search_similarity`. |

## 18. Quick start checklist (new server)

1. Install the JAR and start the server once.
2. In the console: `/bp recommended blocks` (populate lockable lists).
3. In the console: `/bp recommended config` (enable `modern_family_blocks`,
   `use_menus`, `use_dialogs`).
4. In-game as OP: `/bp lockables` to fine-tune the lists (or "select all").
5. Check `/bp admin` to Config if you want to adjust anything from inside the game.
6. Tell your players to read `PLAYER_GUIDE.md`: sneak and right-click any chest to
   lock it.
