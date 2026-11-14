![LOCKABLE BLOCKS REFERENCE](https://raw.githubusercontent.com/Zaynr-Ecosystem/BlockProt-Reloaded/main/images/RELEASE%20TITLES/docs/LOCKABLE%20BLOCKS%20REFERENCE.png)

Complete list of every block and entity that can be locked, organized by family and sub-family.
Covers all Minecraft versions from 1.21.7 up to 26.3.

For the same catalog organized by family: see `LOCKABLE_BLOCKS_BY_FAMILY.md`.

For the family expression syntax used in modern mode: see `BLOCK_FAMILY_SYNTAX.md`.
For the in-game block browser and how to use it: see the `/bp lockables` section below.
For a step-by-step setup walkthrough aimed at new servers: see `../READ_MEs/ADMIN_GUIDE.md`. For the player-facing usage guide: see `../READ_MEs/PLAYER_GUIDE.md`.


## The /bp lockables command
`/bp lockables` opens a paged GUI that shows every block the family system knows about,
regardless of whether it is currently enabled or disabled on the server.

**Who can use it:** any player with the `blockprot.user.admin` permission, or any server operator.
Available in both menu mode and CLI mode - it is always accessible regardless of `use_menus`.

**What it shows:**

- Blocks with a normal icon and green **Status: ACTIVE** are currently lockable. Players can protect them.
- Blocks shown in gray with a red **Status: INACTIVE** are known to the system but disabled in `blocks.yml`.
  They are listed so admins can see exactly what is available and add them back if needed.

**Clicking a block in the GUI:**

- **Left-click** a block -> toggles its live lockable status in `blocks.yml` and refreshes the GUI (same for `[Select All]` / `[Enable All]` category headers)
- **Right-click** a block -> sends a clickable message to your chat; clicking it copies `MATERIAL_NAME` to your clipboard, ready to paste into `blocks.yml`

The info book in slot 46 shows the server version, your client version (if ViaVersion is installed),
and a count of how many blocks are currently active vs inactive.


## Families overview

| Config key                | Family tag      | Sub-families                                                                    |
|---------------------------|-----------------|---------------------------------------------------------------------------------|
| `lockable_tile_entities`  | `TILE_ENTITIES` | `CHEST`, `FURNACE`, `SHELF`, `TRANSPORT`, `MISC`, `SIGN`                        |
| `lockable_shulker_boxes`  | `SHULKER_BOXES` | `SHULKERS`                                                                      |
| `lockable_blocks`         | `BLOCKS`        | `ANVIL`, `CAULDRON`, `FENCE_GATE`, `TRAPDOOR`, `WORKSTATION`, `BED`            |
| `lockable_doors`          | `DOORS`         | `DOORS`                                                                         |
| `lockable_entities`       | `ENTITIES`      | `CHEST_BOATS`, `CHEST_MINECARTS`, `HOPPER_MINECARTS`, `ITEM_FRAMES`            |

`lockable_entities` uses entity NBT (persistent data container), not block NBT. Family expressions are fully supported.
Item frames (`*-ITEM_FRAMES`) are **enabled by default**. Other entity sub-families are disabled: add them to enable.


## lockable_tile_entities

Blocks backed by a block entity. Protection blocks GUI access and hopper interaction.

### Sub-family: CHEST

Token: `*-CHEST`

| Material | MC version | Notes |
|---|---|---|
| `CHEST` | 1.0 | Double-chest: both halves share ownership. |
| `TRAPPED_CHEST` | 1.5 | Emits a redstone signal when opened. Same double-chest rule. |
| `ENDER_CHEST` | 1.0 | Per-player inventory. Locking prevents other players from opening it at that location. |
| `COPPER_CHEST` | 26.1 | Copper Age addition. |
| `EXPOSED_COPPER_CHEST` | 26.1 | |
| `WEATHERED_COPPER_CHEST` | 26.1 | |
| `OXIDIZED_COPPER_CHEST` | 26.1 | |
| `WAXED_COPPER_CHEST` | 26.1 | Does not oxidize further. |
| `WAXED_EXPOSED_COPPER_CHEST` | 26.1 | |
| `WAXED_WEATHERED_COPPER_CHEST` | 26.1 | |
| `WAXED_OXIDIZED_COPPER_CHEST` | 26.1 | |
| `COPPER_TRAPPED_CHEST` | 26.1 | Trapped variant. |
| `EXPOSED_COPPER_TRAPPED_CHEST` | 26.1 | |
| `WEATHERED_COPPER_TRAPPED_CHEST` | 26.1 | |
| `OXIDIZED_COPPER_TRAPPED_CHEST` | 26.1 | |
| `WAXED_COPPER_TRAPPED_CHEST` | 26.1 | |
| `WAXED_EXPOSED_COPPER_TRAPPED_CHEST` | 26.1 | |
| `WAXED_WEATHERED_COPPER_TRAPPED_CHEST` | 26.1 | |
| `WAXED_OXIDIZED_COPPER_TRAPPED_CHEST` | 26.1 | |

### Sub-family: FURNACE

Token: `*-FURNACE`

| Material | MC version | Notes |
|---|---|---|
| `FURNACE` | 1.0 | Blocks retrieval of output and fuel. |
| `SMOKER` | 1.14 | Food only, 2x smelting speed. |
| `BLAST_FURNACE` | 1.14 | Ores and metals only, 2x smelting speed. |

### Sub-family: SHELF

Token: `*-SHELF`

Displays up to three item stacks on its front face. Added in 26.1 (The Copper Age).

| Material | MC version |
|---|---|
| `OAK_SHELF` | 26.1 |
| `SPRUCE_SHELF` | 26.1 |
| `BIRCH_SHELF` | 26.1 |
| `JUNGLE_SHELF` | 26.1 |
| `ACACIA_SHELF` | 26.1 |
| `DARK_OAK_SHELF` | 26.1 |
| `MANGROVE_SHELF` | 26.1 |
| `CHERRY_SHELF` | 26.1 |
| `PALE_OAK_SHELF` | 26.1 |
| `POPLAR_SHELF` | 26.3 |
| `BAMBOO_SHELF` | 26.1 |
| `CRIMSON_SHELF` | 26.1 |
| `WARPED_SHELF` | 26.1 |

### Sub-family: TRANSPORT

Token: `*-TRANSPORT`

| Material | MC version | Notes |
|---|---|---|
| `HOPPER` | 1.5 | Protects against item extraction and redirection. |
| `DISPENSER` | 1.0 | Blocks non-owners from triggering it or accessing its inventory. |
| `DROPPER` | 1.5 | Same as dispenser. |

### Sub-family: MISC

Token: `*-MISC`

Remaining tile entities with no dedicated sub-family.

| Material | MC version | Notes |
|---|---|---|
| `BARREL` | 1.14 | Single-block storage equivalent to a chest. |
| `BREWING_STAND` | 1.0 | Blocks access to potions and ingredients. |
| `DECORATED_POT` | 1.20 | Stores one stack. Accessible via hopper. |
| `CHISELED_BOOKSHELF` | 1.20 | Stores up to six books. Redstone-readable slot index. |
| `CRAFTER` | 1.21 | Automated 3x3 crafting block. |
| `LECTERN` | 1.14 | Blocks other players from taking or replacing the displayed book. |
| `BEEHIVE` | 1.15 | Blocks honey and honeycomb harvesting. |
| `BEE_NEST` | 1.15 | Natural variant of the beehive. Same protection behaviour. |
| `JUKEBOX` | 1.21 | Disc storage added as a block entity in 1.21. Hopper-accessible. |

### Sub-family: SIGN

Token: `*-SIGN`

All sign tile entities: floor signs, wall signs, hanging signs, and wall-mounted hanging signs.
Locking prevents other players from editing the text. Not included in the default `blocks.yml` -
add `*-SIGN` explicitly to enable.

Wood types covered: oak, spruce, birch, jungle, acacia, dark_oak, mangrove, cherry, bamboo, crimson, warped, pale_oak, poplar.

| Pattern | MC version |
|---|---|
| `*_SIGN` | 1.0+ - floor-placed sign |
| `*_WALL_SIGN` | 1.0+ - wall-mounted sign |
| `*_HANGING_SIGN` | 1.20 - hanging sign |
| `*_WALL_HANGING_SIGN` | 1.20 - wall-mounted hanging sign |


## lockable_shulker_boxes

Shulker boxes carry their NBT when broken. BlockProt preserves the lock data in the dropped item
so the box is still protected when placed again elsewhere.

### Sub-family: SHULKERS

Token: `*-SHULKERS` (also matches `[*]` since this is the only sub-family)

| Material | MC version |
|---|---|
| `SHULKER_BOX` | 1.11 |
| `WHITE_SHULKER_BOX` | 1.12 |
| `ORANGE_SHULKER_BOX` | 1.12 |
| `MAGENTA_SHULKER_BOX` | 1.12 |
| `LIGHT_BLUE_SHULKER_BOX` | 1.12 |
| `YELLOW_SHULKER_BOX` | 1.12 |
| `LIME_SHULKER_BOX` | 1.12 |
| `PINK_SHULKER_BOX` | 1.12 |
| `GRAY_SHULKER_BOX` | 1.12 |
| `LIGHT_GRAY_SHULKER_BOX` | 1.12 |
| `CYAN_SHULKER_BOX` | 1.12 |
| `PURPLE_SHULKER_BOX` | 1.12 |
| `BLUE_SHULKER_BOX` | 1.12 |
| `BROWN_SHULKER_BOX` | 1.12 |
| `GREEN_SHULKER_BOX` | 1.12 |
| `RED_SHULKER_BOX` | 1.12 |
| `BLACK_SHULKER_BOX` | 1.12 |


## lockable_blocks

Interactive blocks that are not tile entities. Protection blocks right-click interaction.

### Sub-family: ANVIL

Token: `*-ANVIL`

| Material | MC version | Notes |
|---|---|---|
| `ANVIL` | 1.4 | Blocks the GUI (item naming, repairing, enchanting). |
| `CHIPPED_ANVIL` | 1.4 | Damaged variant. |
| `DAMAGED_ANVIL` | 1.4 | Heavily damaged variant. |

### Sub-family: CAULDRON

Token: `*-CAULDRON`

| Material | MC version | Notes |
|---|---|---|
| `CAULDRON` | 1.0 | Empty cauldron. Blocks filling and draining. |
| `WATER_CAULDRON` | 1.17 | Contains water (1-3 levels). |
| `LAVA_CAULDRON` | 1.17 | Contains lava. |
| `POWDER_SNOW_CAULDRON` | 1.17 | Contains powder snow. |

### Sub-family: WORKSTATION

Token: `*-WORKSTATION`

Blocks non-owners from opening the GUI. A villager linked to a protected workstation is also
protected - see the [Entity protection](#entity-protection) section below.

| Material | MC version |
|---|---|
| `GRINDSTONE` | 1.14 |
| `STONECUTTER` | 1.14 |
| `LOOM` | 1.14 |
| `CARTOGRAPHY_TABLE` | 1.14 |
| `SMITHING_TABLE` | 1.14 |
| `ENCHANTING_TABLE` | 1.0 |
| `FLETCHING_TABLE` | 1.14 |

### Sub-family: FENCE_GATE

Token: `*-FENCE_GATE`

Blocks non-owners from opening or closing the gate.

| Material | MC version |
|---|---|
| `OAK_FENCE_GATE` | 1.0 |
| `SPRUCE_FENCE_GATE` | 1.8 |
| `BIRCH_FENCE_GATE` | 1.8 |
| `JUNGLE_FENCE_GATE` | 1.8 |
| `ACACIA_FENCE_GATE` | 1.8 |
| `DARK_OAK_FENCE_GATE` | 1.8 |
| `MANGROVE_FENCE_GATE` | 1.19 |
| `CHERRY_FENCE_GATE` | 1.20 |
| `PALE_OAK_FENCE_GATE` | 1.21.4 |
| `POPLAR_FENCE_GATE` | 26.3 |
| `BAMBOO_FENCE_GATE` | 1.20 |
| `CRIMSON_FENCE_GATE` | 1.16 |
| `WARPED_FENCE_GATE` | 1.16 |

### Sub-family: TRAPDOOR

Token: `*-TRAPDOOR`

Blocks non-owners from opening or closing the trapdoor.

| Material | MC version |
|---|---|
| `OAK_TRAPDOOR` | 1.0 |
| `SPRUCE_TRAPDOOR` | 1.8 |
| `BIRCH_TRAPDOOR` | 1.8 |
| `JUNGLE_TRAPDOOR` | 1.8 |
| `ACACIA_TRAPDOOR` | 1.8 |
| `DARK_OAK_TRAPDOOR` | 1.8 |
| `MANGROVE_TRAPDOOR` | 1.19 |
| `CHERRY_TRAPDOOR` | 1.20 |
| `PALE_OAK_TRAPDOOR` | 1.21.4 |
| `POPLAR_TRAPDOOR` | 26.3 |
| `BAMBOO_TRAPDOOR` | 1.20 |
| `CRIMSON_TRAPDOOR` | 1.16 |
| `WARPED_TRAPDOOR` | 1.16 |
| `IRON_TRAPDOOR` | 1.8 |
| `COPPER_TRAPDOOR` | 1.21 |
| `EXPOSED_COPPER_TRAPDOOR` | 1.21 |
| `WEATHERED_COPPER_TRAPDOOR` | 1.21 |
| `OXIDIZED_COPPER_TRAPDOOR` | 1.21 |
| `WAXED_COPPER_TRAPDOOR` | 1.21 |
| `WAXED_EXPOSED_COPPER_TRAPDOOR` | 1.21 |
| `WAXED_WEATHERED_COPPER_TRAPDOOR` | 1.21 |
| `WAXED_OXIDIZED_COPPER_TRAPDOOR` | 1.21 |

### Sub-family: BED

Token: `*-BED`

A bed occupies two blocks (foot and head half). Locking either half locks both: the plugin
mirrors owner/friends/redstone data to the paired half automatically, the same way it does
for double chests and two-block doors. Breaking, sneak-right-click menu access, and the
lock-hint message all work identically to any other `BLOCKS` family member.

| Material | MC version |
|---|---|
| `WHITE_BED` | 1.0 |
| `ORANGE_BED` | 1.0 |
| `MAGENTA_BED` | 1.0 |
| `LIGHT_BLUE_BED` | 1.0 |
| `YELLOW_BED` | 1.0 |
| `LIME_BED` | 1.0 |
| `PINK_BED` | 1.0 |
| `GRAY_BED` | 1.0 |
| `LIGHT_GRAY_BED` | 1.0 |
| `CYAN_BED` | 1.0 |
| `PURPLE_BED` | 1.0 |
| `BLUE_BED` | 1.0 |
| `BROWN_BED` | 1.0 |
| `GREEN_BED` | 1.0 |
| `RED_BED` | 1.0 |
| `BLACK_BED` | 1.0 |
| `STRAW_BED` | 26.3 |

### Ungrouped

Blocks with no dedicated sub-family token. Target individually with `MATERIAL_NAME`.

| Material | MC version | Notes |
|---|---|---|
| `DRAGON_EGG` | 1.0 | Right-clicking teleports it. Protection blocks other players from touching it. |
| `COMPOSTER` | 1.14 | Blocks item deposits and bone meal retrieval. |
| `BELL` | 1.14 | Blocks ringing by non-owners. |
| `NOTE_BLOCK` | 1.0 | Blocks pitch changes, useful for musical builds. |


## lockable_doors

Two-block-tall door blocks. Protection blocks non-owners from opening or closing the door.

### Sub-family: DOORS

Token: `*-DOORS` (also matches `[*]`)

| Material | MC version |
|---|---|
| `OAK_DOOR` | 1.0 |
| `SPRUCE_DOOR` | 1.8 |
| `BIRCH_DOOR` | 1.8 |
| `JUNGLE_DOOR` | 1.8 |
| `ACACIA_DOOR` | 1.8 |
| `DARK_OAK_DOOR` | 1.8 |
| `MANGROVE_DOOR` | 1.19 |
| `CHERRY_DOOR` | 1.20 |
| `PALE_OAK_DOOR` | 1.21.4 |
| `POPLAR_DOOR` | 26.3 |
| `BAMBOO_DOOR` | 1.20 |
| `CRIMSON_DOOR` | 1.16 |
| `WARPED_DOOR` | 1.16 |
| `IRON_DOOR` | 1.0 |
| `COPPER_DOOR` | 1.21 |
| `EXPOSED_COPPER_DOOR` | 1.21 |
| `WEATHERED_COPPER_DOOR` | 1.21 |
| `OXIDIZED_COPPER_DOOR` | 1.21 |
| `WAXED_COPPER_DOOR` | 1.21 |
| `WAXED_EXPOSED_COPPER_DOOR` | 1.21 |
| `WAXED_WEATHERED_COPPER_DOOR` | 1.21 |
| `WAXED_OXIDIZED_COPPER_DOOR` | 1.21 |


## lockable_entities

Storage entities and item frames. Protection is stored in the entity's persistent data container
(PDC) via NBT-API and survives chunk reloads and server restarts. It does NOT survive the entity
being killed (the entity ceases to exist; protection data goes with it).

**Default state:** empty (`[]`) on a fresh install, same as every other lockable list. Run `/bp recommended blocks` for a sensible default that enables `ITEM_FRAME` and `GLOW_ITEM_FRAME`, or configure manually via `/bp lockables` or `blocks.yml`.
Other entity types are disabled until an admin adds them to `blocks.yml`. This is intentional:
entity protection has a higher performance profile than block protection because entities are not
indexed by chunk the same way.

To enable all entity types:
```yaml
lockable_entities:
  - "[*]"
```

### Sub-family: CHEST_BOATS

Token: `*-CHEST_BOATS`

Protects the inventory from player access and hopper-pipeline extraction.
`ChestBoat` is resolved at runtime via reflection to support both 1.20.x
(`org.bukkit.entity.ChestBoat`) and 1.21+ (`org.bukkit.entity.boat.ChestBoat`).

| Material | MC version |
|---|---|
| `CHEST_BOAT` | 1.19 - generic (oak) |
| `OAK_CHEST_BOAT` | 1.19 |
| `SPRUCE_CHEST_BOAT` | 1.19 |
| `BIRCH_CHEST_BOAT` | 1.19 |
| `JUNGLE_CHEST_BOAT` | 1.19 |
| `ACACIA_CHEST_BOAT` | 1.19 |
| `DARK_OAK_CHEST_BOAT` | 1.19 |
| `MANGROVE_CHEST_BOAT` | 1.19 |
| `CHERRY_CHEST_BOAT` | 1.20 |
| `PALE_OAK_CHEST_BOAT` | 1.21.4 |
| `POPLAR_CHEST_BOAT` | 26.3 |
| `BAMBOO_CHEST_RAFT` | 1.20 |

### Sub-family: CHEST_MINECARTS

Token: `*-CHEST_MINECARTS`

Protects the inventory from player access and hopper-pipeline extraction.

| Material | MC version |
|---|---|
| `CHEST_MINECART` | 1.0 |

### Sub-family: HOPPER_MINECARTS

Token: `*-HOPPER_MINECARTS`

Protects the inventory from player access and disables item-collection by the minecart.

| Material | MC version |
|---|---|
| `HOPPER_MINECART` | 1.5 |

### Sub-family: ITEM_FRAMES

Token: `*-ITEM_FRAMES`

Item frames and glowing item frames use entity PDC for protection. They are part of the
`ENTITIES` family and are shown in `/bp lockables`. They are **enabled by default** in
`blocks.yml`. Remove them from the list or use the sub-family token `*-ITEM_FRAMES` to manage them.

| Material | MC version | Notes |
|---|---|---|
| `ITEM_FRAME` | 1.4 | Standard item frame. |
| `GLOW_ITEM_FRAME` | 1.17 | Glowing variant, always illuminated. |

**How to protect an item frame:**

1. Sneak and right-click the frame with an empty hand.
2. The BlockProt protection menu opens (owner assignment, friends).
3. Once protected: non-owners cannot rotate or swap the displayed item, and cannot break the frame.

**Admin override:** players with `blockprot.user.admin` always bypass frame protection.

Example `blocks.yml` to enable all entity types including frames:
```yaml
lockable_entities:
  - "[*]"
```

Enable only storage vehicles and keep frames disabled:
```yaml
lockable_entities:
  - "[*-CHEST_BOATS *-CHEST_MINECARTS *-HOPPER_MINECARTS]"
```


## Entity protection

Entity protection covers tamed animals, protectable entities, and villagers linked to protected workstations.
Configured under `entity_protection` in `config.yml` (legacy key: `pet_protection`) and `protectable_entities` in `blocks.yml`.

```yaml
# config.yml
entity_protection:
  enabled: false              # master switch
  auto_protect_on_tame: true  # auto-protect newly tamed animals
  villager_locate_seconds: 6  # duration (1-10 s) of the particle locate effect

# blocks.yml
protectable_entities:
  - VILLAGER
  -
```

`protectable_entities` in `blocks.yml` configures entity types that can be protected via the entity protection menu item. Currently, only `VILLAGER` is compatible and supported. In modern syntax mode, empty lists are represented as `- '[]'`.

### Tamed animals

When `entity_protection.enabled: true`, owners can open the entity protection menu
by right-clicking their tamed pet with the configured menu item (default: stick).

Protection flags per pet:
- `no_damage` - blocks damage from non-owners (player attacks and projectiles)
- `no_interact` - blocks right-click (feeding, naming, etc.)
- `no_leash` - blocks leashing/unleashing by non-owners
- `no_pickup` - blocks parrot-on-shoulder pickup by non-owners

Death notification: if the owner is online when a protected pet dies, they receive a chat message.

### Villagers linked to workstations

When a villager's job site (stored in its memory) points to a protected workstation block, the
villager inherits protection from the block owner. No explicit configuration is needed - it is
automatic whenever the workstation block is locked.

Protected actions:
- Damage by non-owners (attacks and projectiles)
- Right-click interaction (trading GUI) by non-owners
- Breaking blocks within a 2x1x2 area around the workstation (horizontal 2, vertical 1)

**Locate villager button:**

In the BlockProt lock menu for a protected workstation, an Emerald button appears. Clicking it
closes the menu and starts a particle effect on the linked villager for `villager_locate_seconds`
(default 6, max 10). The particles are only visible to the player who clicked the button.
If no villager is linked to the workstation, nothing happens.


## auto_drop_to_inventory

Controls which blocks deliver their contents directly to the breaker's inventory instead of
dropping items on the ground. Spans all families - a single expression resolves against the
full lockable universe.

In legacy mode, list material names explicitly. In modern mode, use family expressions.

Legacy default (shulkers only):
```yaml
auto_drop_to_inventory:
  enabled: true
  blocks:
    - SHULKER_BOX
    - WHITE_SHULKER_BOX
    - ORANGE_SHULKER_BOX
    # ... (all 17 variants)
```

Modern equivalent:
```yaml
auto_drop_to_inventory:
  enabled: true
  blocks:
    - "[*-SHULKERS]"
```

Other useful modern expressions:
```yaml
    - "[*]"                               # every lockable block
    - "[*-SHULKERS *-CHEST]"             # shulkers + all chest variants
    - "[*-SHULKERS -WHITE_SHULKER_BOX]"  # shulkers except white
```
