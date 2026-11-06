# Contributor Starting Points

The maintainer is away for an indefinite time. BlockProt Reloaded 1.4.3 is released. The two legacy editions are built and tested but not published yet. Everything is tested automatically in GitHub Actions, and the repository is ready for anyone who wants to keep improving it.

The open issues below were written as starting points. Any of them can be taken by anyone: comment on the issue to say you are working on it, then follow [CONTRIBUTING.md](../../CONTRIBUTING.md). Issues and pull requests are welcome for any other change too, and are reviewed as time allows.

## Starting points

| Issue | What it is | Good for | Status |
| --- | --- | --- | --- |
| [#15](https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/issues/15) | Extend the two-player test beyond chests (doors, item frames, friends, transfers) | JavaScript, Minecraft protocol basics | Open |
| [#16](https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/issues/16) | Review about 159 swallowed exceptions and log the ones that hide real failures | Java, no behavior change | Closed / Resolved |
| [#17](https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/issues/17) | Split `DebugCommand`, `BlockProt` and `DefaultConfig` by responsibility | Java refactoring, guarded by `bp debug` and the player tests | Closed / Resolved |
| [#18](https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/issues/18) | Decide whether to support Spigot (discussion first) | Anyone running Spigot | Open |
| [#19](https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/issues/19) | Add a `language` chart to bStats | Small Java change on `main` | Closed / Resolved |
| [#20](https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/issues/20) | Update dependencies that need code changes (Adventure 5, Cumulus 2, LandsAPI 7, Run-Paper) | Gradle, Java | Closed / Resolved |
| [#21](https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/issues/21) | Run the two-player test on Minecraft 26.2 and 26.3 | Tracking, waiting on the test client | Open |

## How the repository is organized

- `main` is BlockProt Reloaded (Minecraft 1.21.7 and newer). `BlockProt-Reloaded-Legacy` covers Minecraft 1.20.5 to 1.21.6 and `BlockProt-Legacy` covers 1.18.2 to 1.20.4. Changes land in `main` first. Bug fixes are then carried to the legacy branches with `git cherry-pick`, as described in [CONTRIBUTING.md](../../CONTRIBUTING.md). Legacy branches accept no new features.
- Commits follow `BPR:<branch>(<type>): <summary>`, title only.
- [SCOPE.md](../../SCOPE.md) says what is in and out of scope. Read it before proposing a feature.

## How changes are verified

- `./gradlew build` runs the unit tests.
- `python -m unittest discover -s .github/scripts -p "test_*.py"` runs the automation script tests.
- The `Player Tests` workflow boots a real Paper, Folia and Purpur server for every supported Minecraft version (a version without a server build of its own runs on its newest hotfix, such as 26.1 on 26.1.2), runs the 65 console diagnostics and, where the test client supports the version, plays with two simulated players (an owner and an intruder). It runs on every push that touches code, once a week, and before any publication. Pull requests do not run it; they run the build, the script tests and the console diagnostics on the edition's smoke servers, so run the player test locally for changes that affect gameplay.
- To run it locally: `npm install mineflayer@4`, set `NODE_PATH` to its `node_modules`, then `python .github/scripts/server_smoke_test.py --jar <jar> --project paper --version <minecraft version> --workdir build/e2e --players .github/scripts/e2e_players.js`.

## Releasing

Version numbers, tags and publication are done through the `blockprot.yml` workflow (preview first, then publish). They are not part of a contribution: a pull request should not change `blockProtVersion` or `versionSuffix`.
