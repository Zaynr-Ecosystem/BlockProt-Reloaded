#!/usr/bin/env python3
import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import edition as editions  # noqa: E402

REPO_ROOT = Path(__file__).resolve().parents[2]
PLATFORM_README = REPO_ROOT / "docs" / "READ_MEs" / "MODR_HANG_README.md"
OUTPUT_DIR = REPO_ROOT / "docs" / "READ_MEs" / "editions"
TEMPLATE = OUTPUT_DIR / "MODR_HANG_README.bpr.md"
EDITION_FILE = REPO_ROOT / ".github" / "EDITION"
GUIDE_URL = "https://github.com/Zaynr-Ecosystem/BlockProt-Reloaded/blob/main/docs/READ_MEs/VERSION_GUIDE.md"
RENDERED = ("bpr-legacy", "bp-legacy")

MAIN_BOX = f"> **Looking for another Minecraft version?** See the [version guide]({GUIDE_URL})."
MAIN_INTRO = ("BlockProt Reloaded is a maintained fork of [BlockProt](https://github.com/spnda/BlockProt) "
              "for Paper, Purpur, and Folia servers (Minecraft 1.21.7 through 26.3).")
MAIN_PROTECTED_HEADING = "## What is protected\n\n"
MAIN_DIALOGS = "- Native Paper Dialogs (`use_dialogs: true`) with pastel color palettes."
MAIN_FOLIA = "- Folia compatibility via FoliaLib."
MAIN_MINECRAFT_ROW = "| Minecraft | 1.21.7 - 26.3 (detected numerically at runtime) |"
MAIN_SERVER_ROW = "| Server software | Paper, Purpur, Folia |"
MAIN_JAVA_ROW = "| Java | 21+ required (JDK 25 toolchain) |"
MAIN_INSTALL = "Requires Java 21+ and Paper, Purpur, or Folia 1.21.7 - 26.3."


def join_and(items):
    return " and ".join(items) if len(items) == 2 else ", ".join(items[:-1]) + ", and " + items[-1]


def join_or(items):
    return " or ".join(items) if len(items) == 2 else ", ".join(items[:-1]) + ", or " + items[-1]


def replace_once(text, old, new):
    if text.count(old) != 1:
        raise SystemExit(f"Template line not found exactly once: {old!r}")
    return text.replace(old, new)


def render(template, config):
    info = config["description"]
    servers = info["servers"]
    text = template
    text = replace_once(text, MAIN_BOX,
                        f"> **{info['title']}**. Looking for another Minecraft version? See the [version guide]({GUIDE_URL}).")
    text = replace_once(text, MAIN_INTRO,
                        f"{config['displayName']} is a maintained fork of [BlockProt](https://github.com/spnda/BlockProt) "
                        f"for {join_and(servers)} servers (Minecraft {info['range']}).")
    text = replace_once(text, MAIN_PROTECTED_HEADING,
                        MAIN_PROTECTED_HEADING + "Only blocks and entities that exist on your Minecraft version can be locked; "
                        "the lists below name the complete set across editions.\n\n")
    text = replace_once(text, MAIN_DIALOGS,
                        "- Native Paper Dialogs: not available in this edition. Every menu opens as a chest inventory.")
    if "Folia" not in servers:
        text = replace_once(text, MAIN_FOLIA, "- Folia: not declared for this edition.")
    text = replace_once(text, MAIN_MINECRAFT_ROW, f"| Minecraft | {info['range']} |")
    text = replace_once(text, MAIN_SERVER_ROW, f"| Server software | {', '.join(servers)} |")
    text = replace_once(text, MAIN_JAVA_ROW, f"| Java | {info['java']} required |")
    text = replace_once(text, MAIN_INSTALL, f"Requires {info['java']} and {join_or(servers)} {info['range']}.")
    return text


def read(path):
    return path.read_text(encoding="utf-8").replace("\r\n", "\n")


def outputs():
    template = read(TEMPLATE)
    for edition_id in RENDERED:
        config = editions.load_edition(edition_id)
        yield OUTPUT_DIR / f"MODR_HANG_README.{edition_id}.md", render(template, config)
    current = EDITION_FILE.read_text(encoding="utf-8").strip() if EDITION_FILE.is_file() else "bpr"
    yield PLATFORM_README, template if current == "bpr" else render(template, editions.load_edition(current))


def main():
    parser = argparse.ArgumentParser(description="Render the per-edition platform descriptions from the shared template.")
    parser.add_argument("--check", action="store_true", help="Fail when a generated file differs from the disk")
    args = parser.parse_args()
    stale = []
    for path, content in outputs():
        if args.check:
            if not path.is_file() or read(path) != content:
                stale.append(path.name)
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding="utf-8", newline="\n")
            print(f"wrote {path.relative_to(REPO_ROOT)}")
    if stale:
        print("out of date: " + ", ".join(stale))
        return 1
    if args.check:
        print("descriptions are up to date")
    return 0


if __name__ == "__main__":
    sys.exit(main())
