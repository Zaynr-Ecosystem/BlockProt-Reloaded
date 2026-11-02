# BlockProt Reloaded: Player Guide

This guide explains how to use BlockProt Reloaded as a player: how to lock your
blocks, manage friends, and tune your personal settings. Server configuration is
covered in `ADMIN_GUIDE.md`.

## 1. How locking works

BlockProt Reloaded protects blocks such as chests, furnaces, barrels, shulkers,
doors, anvils, item frames, and more. Each protected block has one **owner** and an
optional list of **friends**.

- Only the owner and their friends can open, take from, or place into a protected
  block.
- Everyone else is blocked with a "You don't have permission" message.
- Protection survives chunk unloads, server restarts, and explosions.

### How to lock a block

1. Sneak (crouch) and **right-click the block with an empty hand**.
2. Click the **Lock** button (the block's own item, first slot of the menu).
3. Optionally add friends or make the block public (section 3).

> Only blocks that the server administrator marked as lockable can be locked. If
> sneak + right-click does nothing, the block type is not on the server's lockable
> list. Players cannot add blocks themselves; ask the administrator to enable it
> (they use `/bp lockables`).

### Auto-lock on place

If the server has auto-lock enabled (the default), any lockable block you place is
locked automatically and you see **"Block protected!"** in the action bar. Sneaking
while placing skips auto-lock. You can turn auto-lock off for yourself in
`/bp settings` (section 4).

### The locking hint

The first time you right-click a block you have access to (before you have opened
any BlockProt menu), you see a chat message: *"You can protect your blocks by
crouching and right-clicking your chests, furnaces, and more!"* The message is
clickable and turns hints off. Once you have opened any BlockProt menu or clicked
that link, the hint does not appear again. You can re-enable it anytime in
`/bp settings` (Hints toggle) and turn it off with `/bp disablehints`.

### Auto drop to inventory

The server administrator can mark certain blocks and entities so their drops go
straight to your inventory instead of the ground when you break them. This is a
server setting; you do not configure it.

- For containers (chests, furnaces, barrels, dispensers, and so on) the block and
  its contents arrive in your inventory together.
- A double chest delivers only the half you broke. The other half stays where it
  was, so nothing is duplicated or lost.
- Doors and beds still drop a single item, the same as vanilla, and no half is
  left floating behind.
- Item frames, glow item frames, storage minecarts, hopper minecarts, and chest
  boats deliver the item plus whatever they carried. If a projectile (an arrow or
  trident) knocks an item frame off, the drops go to whoever shot it.
- Blocks protected by another player are skipped when the break would be blocked,
  so a non-owner does not receive the contents.

## 2. The lock menu

Sneak + right-clicking a lockable block you own (or manage) opens its menu:

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
| Paper | **Copy** this block's configuration to your clipboard. |
| Knowledge book | **Paste** a copied configuration onto this block. |
| Compass | **Block info**: owner, name, friends, redstone/hopper settings, linked item frame. |
| Barrier | Back / close. |

Item frames, chest boats, storage minecarts, and hopper minecarts can be protected
the same way. Tamed animals can be protected too if the server enabled entity
protection: hold the configured item (default: a stick) and right-click the
animal. Villagers are protected through their workstation when the server has
villager workstation protection enabled.

## 3. Pets, entities, and villagers (if the server has entity protection on)

This is a separate system from block protection, and the server administrator has
to turn it on. Ask them if you are not sure.

### Protecting a pet or entity

1. Tame an animal normally (wolf, cat, horse, parrot, and so on), or find an entity
   the server administrator configured as protectable (such as an Allay, Armadillo,
   or Iron Golem).
2. Hold the **menu item** (a stick by default) and right-click the animal or entity.
3. If protection is on and the animal auto-protects on tame, it is already yours;
   otherwise use the Entity Settings menu that opens to turn protection on.

### The Entity Settings menu

| Toggle | What it blocks for everyone except you and your friends |
|---|---|
| Entity protection | Master switch: turns protection for this animal on/off. |
| Block damage | Other players (and their arrows/tridents) cannot hurt it. |
| Block interaction | Other players cannot right-click, feed, or rename it. |
| Block leash | Other players cannot leash or unleash it. |
| Block pickup (parrot) | Other players cannot pick it up onto their shoulder. |

The same menu has its own **Friends** list, separate from your block friends: add
a friend here to let them interact with this specific pet.

If your pet dies while protected, you get a chat message naming it (its custom
name if you gave it one).

### Villagers

Villagers are protected through their **workstation** rather than directly: once a
villager is protected, the block it works at (lectern, cartography table, and so
on) is automatically protected too, so nobody can steal your villager's job site.
Use the Emerald button in a workstation's lock menu to see a particle trail to the
linked villager.

## 4. Friends and public blocks

### Friends

A friend is a player you explicitly add to a block. Manage friends from the lock
menu (player head button), from `/bp friends`, or from `/bp user` to Friends.

When you search for a name to add, type it in chat and press Enter (or into the
text field when using dialog mode). The search works whether the player is online
or has simply played on the server before; a player who is online right now is
always found. Search queries automatically normalize accents and diacritics,
matching player names reliably regardless of keyboard accents.

| Command | What it does |
|---|---|
| `/bp friends` | Opens your default-friends menu. |
| `/bp friends addall <player>` | Adds a friend to **all** your protected blocks. |
| `/bp transferall <player>` | Transfers ownership of **all** your protected blocks to another player. |
| `/bp logs [here|<page>]` | Opens the paginated access audit log for your protected blocks or the targeted block. |

### Friend permission levels

Starting in 1.3.6, you can grant four different permission levels when adding or
editing a friend on a block. Every level can open the block and take or deposit
items; the level decides what else the friend may do:

- **Basic:** Use the block only. The lock menu does not open for them.
- **Operator:** Also open the lock menu, change block settings and the block
  name, view the audit log, and inspect the contents.
- **Full Manager:** Everything Operator can do, plus managing friends. They cannot
  transfer or delete the protection.
- **Custom:** Configure four independent permissions (Open Container, Deposit Items, Withdraw Items, and Redstone Access) via dialogs, chest inventory menus, or Bedrock forms.

### Public blocks

The lock menu's friends screen also lists **The Public**. Making a block public
lets anyone open it, which is useful for shops, public farms, and shared builds.

## 5. Personal settings (`/bp settings`)

| Toggle | Meaning |
|---|---|
| Lock on place | Auto-lock every block you place (see section 1). |
| Hints | Show/hide the locking hint messages. |
| Prefer dialogs | Use native dialog windows instead of inventory menus (see below). |
| Prefer Bedrock forms | Use native touch forms for Bedrock Edition players (Geyser/Floodgate). |
| Colorblind mode | High-contrast visual indicators in menus and status messages. |
| Notifications | Owner notifications on/off for you. |
| Friends | Opens the default-friends menu. |

### Dialogs, Bedrock forms, and inventories

The server can offer multiple presentation styles:
- **Inventory menus:** classic chest GUIs available on any server software.
- **Dialog windows:** Minecraft's native dialog system (Paper 1.21.7+). If enabled
  on the server, you can toggle "Prefer dialogs" in `/bp settings`.
- **Bedrock forms:** touch-friendly forms for Bedrock players connecting through
  Geyser and Floodgate, toggleable via "Prefer Bedrock forms".

### Colorblind accessibility

When you turn on "Colorblind mode" in `/bp settings`:
- Status indicators throughout menus, dialogs, and messages switch to high-contrast
  symbols instead of relying purely on green and red colors.
- Allowed or enabled options display with a checkmark (`✔`), while blocked or
  disabled options display with a cross (`✖`).
- Active and inactive settings also use clear bullet indicators (`●` active,
  `○` inactive).
- This ensures full clarity for players with color vision deficiencies.

### Bedrock player skins

When playing through Bedrock Edition (via Geyser and Floodgate), player heads in
menus, friend lists, and statistics resolve your authentic Bedrock skin
automatically rather than falling back to default Steve or Alex heads.

## 6. Statistics, transfers, and info

- `/bp stats` shows your protected-block statistics and your block list. If the
  server grants the `blockprot.blocks.tp` permission, left-clicking a location
  entry teleports you to that block.
- `/bp about` shows plugin and fork information.
- `/bp disablehints` turns the hint messages off directly.

### Raid alerts

If the server administrator turned on raid detection, you get an action-bar
warning (remaining visible for 6 seconds) plus a chat message (with a clickable
teleport link if you have the `blockprot.blocks.tp` permission) whenever an
explosion hits one of your locked blocks, whether or not it was destroyed.
Rapid repeated explosions are debounced so you receive one coherent alert
instead of notification spam. If you were offline when it happened, the alert
is delivered in chat the next time you join. This is a notification-only
feature; it does not stop explosions or change whether your blocks survive them,
that part is controlled by the administrator's explosion-protection setting.

## 7. Limits and what players cannot do

- The server can cap how many blocks each player may lock
  (`player_max_locked_block_count`). If you hit the cap, you see a warning message
  both when auto-locking on place and when locking manually. The administrator can
  raise the cap or exempt you with the `blockprot.lockmax` or
  `blockprot.locklimit.<N>` permission.
- Players **cannot** add blocks to the lockable list, edit `blocks.yml` or
  `config.yml`, or use the admin commands (`/bp lockables`, `/bp admin`,
  `/bp reload`, and so on). Those are operator-only.

## 8. Troubleshooting

| Problem | Solution |
|---|---|
| Sneak + right-click does nothing | The block type is not lockable on this server. Ask the administrator to add it via `/bp lockables`. |
| "You don't have permission" on a block | You are not the owner or a friend of that block. Check the block info (Compass) for the owner name and ask them to add you. |
| Can't open my block after a restart | Broken blocks lose their protection by design. Shulker boxes keep protection on the item itself. |
| I want to stop seeing hints | Run `/bp disablehints`, or toggle Hints in `/bp settings`. |
| I want dialogs/inventories | Toggle "Prefer dialogs" in `/bp settings` (only visible when dialogs are enabled on the server). |
| Right-clicking my pet with a stick does nothing | Entity protection is off on this server, or the server uses a different menu item. Ask the administrator. |
| Friend search finds nobody | Double-check the spelling; the search needs a reasonably close match. If the name is only a rough match, the server lists up to 26 close candidates (or six per page in dialog mode) so you can pick the right one. If the target is definitely online and no candidate appears, ask the administrator to update the plugin (older versions missed online players in pet/entity search specifically). |
