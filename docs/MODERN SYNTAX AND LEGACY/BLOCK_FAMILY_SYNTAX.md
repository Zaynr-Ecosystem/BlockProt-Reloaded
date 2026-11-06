![BLOCK FAMILY SYNTAX](https://raw.githubusercontent.com/Zaynr-Ecosystem/BlockProt-Reloaded/main/images/RELEASE%20TITLES/docs/BLOCK%20FAMILY%20SYNTAX.png)

Family expressions are a compact syntax for `blocks.yml`, `worlds.yml`, and
`auto_drop_to_inventory`. They are **always parsed** regardless of the `modern_family_blocks`
flag in `config.yml`.

`modern_family_blocks` controls formatting when saving files via in-game interfaces (such as `/bp lockables`):
- `true`  -> saves and updates `blocks.yml` using compact family expressions.
- `false` -> saves and updates `blocks.yml` using flat material names.

**IMPORTANT:** `modern_family_blocks` does **NOT** force auto-conversion of `blocks.yml` on server startup or reload. `blocks.yml` is always read as-is, and family expressions are parsed dynamically regardless of this flag. When all lockable lists are empty, this flag automatically disables to `false`.

They replace flat material lists with token-based expressions that resolve dynamically
against the full family registry at startup and on `/bp reload`.

## Compression

When a key is saved in modern format, the plugin picks the shortest valid representation
per sub-family independently: direct inclusion (`CHEST BARREL`) if the selected members are
the minority, or exclusion (`*-CHEST -COPPER_CHEST`) if they are the majority. Selecting a
single block from a large sub-family no longer produces a long exclusion list for the whole
family; only that one sub-family's members are ever weighed against each other.

## Empty lists

An empty key is never written as a real empty YAML list (`[]`). In legacy (flat) mode it
is saved as two blank template lines (`-` / `-`), inviting the user to type material names
directly in their place. In modern mode it is saved as the single-element list
`- '[]'`, which is the only representation SnakeYAML round-trips safely for "nothing selected"
without corrupting the rest of the file's structure on the next save.


## Format: plain names or a bracket expression

Every list key (`lockable_tile_entities`, `lockable_blocks`, `lockable_entities`, etc.)
is a normal YAML list. Each entry is either a plain material name, one per line,
or a single quoted string wrapped in `[...]` (a family expression) standing for
many materials at once. The two forms can be mixed across different lines of the
same list.

```yaml
lockable_entities:
  - ITEM_FRAME                              # plain material
  - '[*-CHEST_BOATS -CHEST_MINECART]'       # family expression
```

Square brackets without commas are read as one family expression, not a list:
`lockable_entities: [ITEM_FRAME ACACIA_CHEST_BOAT]` resolves to nothing, since
neither word is a valid expression token, and no warning is logged for this case.
Use a plain list, or a flow-list with commas (`[ITEM_FRAME, ACACIA_CHEST_BOAT]`),
for one or two specific materials. Reach for an expression when you want a whole
sub-family, or a family minus a few exceptions.

## Expression tokens

All expressions are wrapped in `[...]` and contain space-separated tokens. The
result set starts empty; `*` or `*-TAG` populate it, `-*TAG` removes a sub-family
from it, and `NAME` / `-NAME` add or remove individual materials.

| Token    | Operation                                              | Example |
|----------|-----------------------------------------------------------|---------|
| `*`      | Include all members of the family                          | `[*]` -> everything |
| `*-TAG`  | Include all members of a sub-family                         | `[*-CHEST]` -> only chests |
| `-*TAG`  | Exclude a sub-family from the current base                  | `[* -*CHEST]` -> everything but chests |
| `NAME`   | Include one material (must belong to this family)           | `[CHEST BARREL]` -> only those two |
| `-NAME`  | Exclude one material (must belong to this family)           | `[*-CHEST -COPPER_CHEST]` -> chests minus copper |

`-*TAG` with no prior `*` or `*-TAG` has nothing to remove from and resolves to
an empty set; `[-*CHEST]` alone disables nothing. To drop one sub-family while
keeping the rest, include the base first: `[* -*CHEST]`.

`NAME` and `-NAME` are validated against the family of the config key; a material
from another family is rejected with a console warning and discarded, e.g.
`[* -FLETCHING_TABLE]` under `lockable_tile_entities` drops that token since
`FLETCHING_TABLE` belongs to `BLOCKS`, not `TILE_ENTITIES`.

## More examples

```yaml
lockable_tile_entities:
  - "[*]"                                  # everything
  - "[CHEST BARREL]"                       # only these two
  - "[*-CHEST *-SHELF]"                    # two sub-families
  - "[* -*SIGN]"                           # everything except the SIGN sub-family
  - "[*-FURNACE *-SHELF *-TRANSPORT *-MISC *-CHEST -COPPER_CHEST]"  # all sub-families, CHEST minus copper

lockable_blocks:
  - "[* -DRAGON_EGG]"                      # everything except one block

lockable_shulker_boxes:
  - "[*-SHULKERS -WHITE_SHULKER_BOX]"      # all shulkers minus one

lockable_entities:
  - "[*-CHEST_BOATS *-CHEST_MINECARTS *-HOPPER_MINECARTS *-ITEM_FRAMES]"

# worlds.yml - per-world example
worlds:
  survival:
    enabled: true
    auto_drop_to_inventory_enabled: true
    lockable_tile_entities:
      - "[*]"
    lockable_shulker_boxes:
      - "[*-SHULKERS -WHITE_SHULKER_BOX]"
    lockable_blocks:
      - "[* -DRAGON_EGG]"
    lockable_doors:
      - "[*]"
  creative:
    enabled: false
    lockable_tile_entities: []
    lockable_shulker_boxes: []
    lockable_blocks: []
    lockable_doors: []
```

## auto_drop_to_inventory

Resolves expressions against **all families** independently (union).
Each family processes the expression against its own members.
Sub-family tokens that do not belong to a given family are silently skipped for that family.

```yaml
auto_drop_to_inventory:
  enabled: true
  blocks:
    - "[*-SHULKERS]"                       # all shulkers auto-drop to inventory
    - "[*-SHULKERS -RED_SHULKER_BOX]"      # all shulkers except red
    - "[*]"                                # all lockable blocks across all families
    - "[*-SHULKERS *-CHEST]"              # shulkers + all chest variants

# To disable shulker auto-drop entirely:
#   Option A - set enabled: false
#   Option B - remove shulker entries from blocks list
#   Option C - do not include [*-SHULKERS] or individual shulker names
#
# Note: [-*SHULKERS] alone produces an empty set (no base inclusion = nothing to remove from).
# It will NOT disable shulkers that are listed via other entries in the same blocks list.
```

## Sub-family reference

| Key                       | Family          | Available sub-family tags                                          |
|---------------------------|-----------------|--------------------------------------------------------------------|
| `lockable_tile_entities`  | `TILE_ENTITIES` | `CHEST`, `FURNACE`, `SHELF`, `TRANSPORT`, `MISC`, `SIGN`           |
| `lockable_shulker_boxes`  | `SHULKER_BOXES` | `SHULKERS`                                                         |
| `lockable_blocks`         | `BLOCKS`        | `ANVIL`, `CAULDRON`, `FENCE_GATE`, `TRAPDOOR`, `WORKSTATION`, `BED` |
| `lockable_doors`          | `DOORS`         | `DOORS`                                                            |
| `lockable_entities`       | `ENTITIES`      | `CHEST_BOATS`, `CHEST_MINECARTS`, `HOPPER_MINECARTS`, `ITEM_FRAMES` |

For the full list of materials in each sub-family: see `LOCKABLE_BLOCKS_REFERENCE.md`.
