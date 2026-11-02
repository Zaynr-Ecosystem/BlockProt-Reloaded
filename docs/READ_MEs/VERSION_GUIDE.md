# Which BlockProt do I download?

BlockProt is published as three editions. Each edition covers a range of Minecraft versions and has its own download page. Pick the one that matches your server version.

## Minecraft version to edition

| Minecraft version | Edition | Java | Download |
| --- | --- | --- | --- |
| 1.21.7 and newer (including 26.x) | **BlockProt Reloaded** | 21 or newer (25 for Minecraft 26.x) | [Modrinth](https://modrinth.com/plugin/blockprot-reloaded), [CurseForge](https://www.curseforge.com/minecraft/bukkit-plugins/blockprot-reloaded), [Hangar](https://hangar.papermc.io/VictorGugug/BlockProt-Reloaded) |
| 1.20.5 to 1.21.6 | **BlockProt Reloaded Legacy** | 21 or newer | [Modrinth](https://modrinth.com/plugin/blockprot-reloaded-legacy) |
| 1.18.2 to 1.20.4 | **BlockProt Legacy** | 17 or newer | [Modrinth](https://modrinth.com/plugin/blockprot-legacy) |
| Older than 1.18.2 | No supported edition | | Older upstream BlockProt releases may run there; they are not maintained here |

## Anonymous usage metrics (bStats)

Each edition reports anonymous metrics to its own bStats plugin page:

- **BlockProt Reloaded** (1.21.7+): [bStats #31548](https://bstats.org/plugin/bukkit/BlockProt%20Reloaded/31548)
- **BlockProt Reloaded Legacy** (1.20.5 - 1.21.6): [bStats #34385](https://bstats.org/plugin/bukkit/BlockProt%20Reloaded%20Legacy/34385)
- **BlockProt Legacy** (1.18.2 - 1.20.4): [bStats #34386](https://bstats.org/plugin/bukkit/BlockProt%20Legacy/34386)

BlockProt Reloaded 1.3.8 (Minecraft 1.21.1 to 1.21.6) and 1.3.7 (Minecraft 1.20.5 to 1.21) stay available on [GitHub Releases](https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/releases). The legacy editions replace them.

Spigot and CraftBukkit are not supported by any edition. Use Paper, Purpur or Folia.

## Find your versions

- **Minecraft version**: run `/version` in game or in the console, or read the first lines of the server log. Use the number after `MC:`, for example `1.21.6`.
- **Java version**: run `java -version` in the same environment that starts the server.

## What each edition contains

| Feature | BlockProt Reloaded | BlockProt Reloaded Legacy | BlockProt Legacy |
| --- | :---: | :---: | :---: |
| Chest inventory menus | yes | yes | yes |
| Native Paper Dialogs (`use_dialogs`) | yes | no | no |
| Bedrock forms (Geyser and Floodgate) | yes | yes | yes |
| MiniMessage translations | yes | yes | yes |
| Public developer API | yes | yes | yes |
| Console diagnostics (`/bp debug`) | yes | yes | yes |
| Paper and Purpur | yes | yes | yes |
| Folia (Minecraft 1.19.4 and newer) | yes | yes | yes |
| New features | yes | no | no |

Native dialogs need the Paper 1.21.7 API, so they only exist in BlockProt Reloaded. In the legacy editions `use_dialogs` has no effect and every menu opens as a chest inventory. The setting is kept in `config.yml` so a later move to BlockProt Reloaded activates it.

## Moving to another edition

Upgrading Minecraft usually means changing edition:

1. Stop the server.
2. Remove the old BlockProt JAR from `plugins/` and put the JAR of the edition for your new Minecraft version in its place.
3. Keep the `plugins/BlockProtReloaded/` data folder. Every edition uses the same folder, permissions and PlaceholderAPI identifier.
4. Start the server.

At startup the plugin records which edition wrote the data in `plugins/BlockProtReloaded/.edition`. When the edition or the Minecraft version changed since the last start, it makes a backup first, in `plugins/BlockProtReloaded/backups/`, named with both editions, for example `2026-10-01_12-00_bpr-legacy-1.3.9.1_to_bpr-1.4.0.zip`, and logs the path to the console.

Rules that keep the data safe:

- Only BlockProt Reloaded adds new keys to the data files. The legacy editions ignore keys they do not know and never rewrite or reset them.
- Moving up (BlockProt Legacy to BlockProt Reloaded Legacy to BlockProt Reloaded) is supported.
- Moving down is not supported. The plugin detects it, creates a backup and warns that settings written by the newer edition may be ignored.
- Two BlockProt JARs in `plugins/` fail with a plugin name conflict, because all editions share the plugin name `BlockProtReloaded`. Remove the JAR you do not need.

When the server runs a Minecraft version below the range of the installed edition, the plugin disables itself and the console tells you to read this guide and names the edition to download. This works on any Minecraft version from 1.18.2 up, because the plugin declares the oldest possible `api-version`. If the console shows `UnsupportedClassVersionError` instead, your Java is older than the edition requires: check the Java column of the table above. On a Minecraft version above the range of a legacy edition, the plugin keeps running and warns once that a newer edition exists.

## Support policy

The legacy editions receive bug fixes, security fixes, and compatibility with new Minecraft releases inside their range. They receive no new features. New features land in BlockProt Reloaded only.

Fixes are applied to BlockProt Reloaded first and carried to the legacy editions afterwards, so a legacy build can trail the newest BlockProt Reloaded version by a short time.

## Where to report issues

Open an issue on [GitHub](https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/issues) and choose the edition in the form. Read [CONTRIBUTING.md](../../CONTRIBUTING.md) and [SCOPE.md](../../SCOPE.md) first.
