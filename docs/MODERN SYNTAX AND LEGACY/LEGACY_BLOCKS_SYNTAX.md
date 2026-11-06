![LEGACY BLOCKS SYNTAX](https://raw.githubusercontent.com/Zaynr-Ecosystem/BlockProt-Reloaded/main/images/RELEASE%20TITLES/docs/LEGACY%20BLOCKS%20SYNTAX.png)

# blocks.yml: Configuration Guide

`blocks.yml` controls which blocks, entities, and containers players can protect on your server.

> New to BlockProt Reloaded? Start with the step-by-step setup guide at `../READ_MEs/ADMIN_GUIDE.md`. For the player-facing usage guide: see `../READ_MEs/PLAYER_GUIDE.md`.

## Two valid formats

Every list key accepts **two formats**. Do not mix them on the same line.

### Flat list (plain)

One material per line, each prefixed with `-`:

```yaml
lockable_entities:
  - ITEM_FRAME
  - GLOW_ITEM_FRAME
```

**Use this when:** you only want a handful of specific materials. Simple, explicit, no surprises.

**Do NOT write as:** `lockable_entities: [ITEM_FRAME GLOW_ITEM_FRAME]`, that is a family expression, not a list, and the parser will silently produce an empty result (no warning).

### Modern / family expressions (compact)

A single quoted string wrapped in `[...]` that expands to many materials at once:

```yaml
lockable_entities:
  - "[*-ITEM_FRAMES]"
```

**Use this when:** you want entire groups (e.g. all chest variants, all trapdoors) or you want "everything except a few".

**See:** `BLOCK_FAMILY_SYNTAX.md` for the full expression syntax and token reference.

## Quick reference

All five lockable list keys ship with two blank template lines (`-` / `-`) by default on a fresh install, ready for you to type material names directly in their place. Run `/bp recommended blocks` (console only) to populate a sensible default set instead of configuring everything by hand. The table below shows what `/bp recommended blocks` writes for each key when `modern_family_blocks: false` (flat mode).

| Key | Family | `/bp recommended blocks` default (flat mode) |
|---|---|---|
| `lockable_tile_entities` | TILE_ENTITIES | All chests, furnaces, hoppers, barrels, shelves, etc. |
| `lockable_shulker_boxes` | SHULKER_BOXES | All 17 shulker box variants |
| `lockable_blocks` | BLOCKS | Anvils, cauldrons, workstations (grindstone, stonecutter, loom, cartography table, smithing table, enchanting table, fletching table), dragon egg, composter, bell, note block |
| `lockable_doors` | DOORS | All wood types + iron |
| `lockable_entities` | ENTITIES | ITEM_FRAME + GLOW_ITEM_FRAME only |
| `protectable_entities` | None | Configured with `VILLAGER` by default; currently `VILLAGER` is the only compatible and supported entity type for stick-protection |
| `auto_drop_to_inventory` | None | Shipped with two blank template lines under `blocks:` like every other list, so it starts empty but is ready to type into. Stays empty even after `/bp recommended blocks` in flat mode; only the modern-mode (`modern_family_blocks: true`) branch of `/bp recommended blocks` populates it, with `[*-SHULKERS]` |

Fence gates, trapdoors, and copper doors belong to the BLOCKS and DOORS families respectively and can be added with family expressions (e.g. `"[*-TRAPDOOR]"`, `"[*-FENCE_GATE]"`), but `/bp recommended blocks` does not include them in its flat-mode defaults.

### Flat list: per-material

```yaml
lockable_tile_entities:
  - CHEST
  - FURNACE
  - HOPPER
```

### Modern -- family expression

```yaml
lockable_tile_entities:
  - "[*-CHEST *-FURNACE]"
```

## How family expressions resolve

1. The parser identifies which family the key belongs to (e.g. `lockable_tile_entities` -> `TILE_ENTITIES`).
2. Each token inside `[...]` adds or removes materials from that family's member set.
3. The result is a list of `Material` enum values that the rest of BlockProt uses for protection checks.

If you use a token that does not belong to that family (e.g. `FLETCHING_TABLE` inside `lockable_tile_entities`), it is rejected with a console warning and discarded.

## Enabling and disabling entities

`lockable_entities` controls entity protection. It is empty by default; nothing is protected until it is configured, either manually or via `/bp recommended blocks`.

```yaml
# Enable everything (all entity sub-families)
lockable_entities:
  - "[*]"

# Only storage vehicles (boats + minecarts)
lockable_entities:
  - "[*-CHEST_BOATS *-CHEST_MINECARTS *-HOPPER_MINECARTS]"

# Only item frames (default)
lockable_entities:
  - ITEM_FRAME
  - GLOW_ITEM_FRAME

# Disable all entity protection
lockable_entities: []
```

## auto_drop_to_inventory

Shulker boxes auto-drop their contents to the breaker's inventory on break. Accepts both formats:

```yaml
auto_drop_to_inventory:
  enabled: true
  blocks:
    - "[*-SHULKERS]"
```

For full details: `BLOCK_FAMILY_SYNTAX.md` | `LOCKABLE_BLOCKS_REFERENCE.md`
