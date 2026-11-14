#!/usr/bin/env python3
import argparse
import os
import re
import sys
from pathlib import Path

try:
    import yaml
except ImportError:
    yaml = None

REPO_ROOT = Path(__file__).resolve().parents[2]
TIER1_LANGUAGES = {"en", "es", "de", "fr"}


def extract_enum_keys(key_file):
    if not key_file.is_file():
        return []
    content = key_file.read_text(encoding="utf-8")
    keys = []
    in_enum = False
    for line in content.splitlines():
        line = line.strip()
        if "public enum TranslationKey" in line:
            in_enum = True
            continue
        if not in_enum:
            continue
        if line.startswith("//") or line.startswith("/*") or line.startswith("*"):
            continue
        m = re.match(r"^([A-Z0-9_]+)\s*[,;]", line)
        if m:
            enum_name = m.group(1)
            dotted = enum_name.replace("__", ".").lower()
            keys.append(dotted)
        if ";" in line:
            break
    return keys


def flatten_dict(d, prefix=""):
    items = {}
    if not isinstance(d, dict):
        return items
    for k, v in d.items():
        new_key = f"{prefix}.{k}" if prefix else str(k)
        if isinstance(v, dict):
            items.update(flatten_dict(v, new_key))
        else:
            items[new_key] = v
    return items


def is_invalid_value(val):
    if val is None:
        return True
    s = str(val).strip()
    return s == "" or s.lower() == "null"


def extract_placeholders(text):
    if not isinstance(text, str):
        return set()
    return set(re.findall(r"\{[a-zA-Z0-9_]+\}", text))


def tier1_languages_for(repo_path):
    edition_file = Path(repo_path) / ".github" / "EDITION"
    edition_id = edition_file.read_text(encoding="utf-8").strip() if edition_file.is_file() else "bpr"
    if edition_id in ("bpr-legacy", "bp-legacy"):
        return {"en", "es"}
    return set(TIER1_LANGUAGES)


def audit_translations(repo_root=REPO_ROOT):
    if yaml is None:
        raise RuntimeError("PyYAML is required to audit translations.")

    repo_path = Path(repo_root)
    tier1_langs = tier1_languages_for(repo_path)
    key_file = repo_path / "spigot" / "src" / "main" / "java" / "de" / "sean" / "blockprot" / "bukkit" / "TranslationKey.java"
    lang_dir = repo_path / "spigot" / "src" / "main" / "resources" / "lang"
    en_file = lang_dir / "translations_en.yml"

    enum_keys = set(extract_enum_keys(key_file))

    with open(en_file, "r", encoding="utf-8") as f:
        en_raw = yaml.safe_load(f) or {}
    en_flat = flatten_dict(en_raw)

    canonical_keys = enum_keys.union(en_flat.keys())

    en_placeholders = {k: extract_placeholders(v) for k, v in en_flat.items() if isinstance(v, str)}

    results = []
    has_tier1_failure = False

    lang_files = sorted(lang_dir.glob("translations_*.yml"))
    for lf in lang_files:
        lang_code = lf.stem.replace("translations_", "")
        is_tier1 = lang_code in tier1_langs

        with open(lf, "r", encoding="utf-8") as f:
            try:
                raw = yaml.safe_load(f) or {}
            except Exception as e:
                results.append({
                    "lang": lang_code,
                    "file": lf.name,
                    "tier1": is_tier1,
                    "error": f"YAML parse error: {e}",
                    "total": len(canonical_keys),
                    "valid": 0,
                    "null_empty": 0,
                    "missing": len(canonical_keys),
                    "placeholder_mismatches": [],
                    "status": "FAIL",
                })
                if is_tier1:
                    has_tier1_failure = True
                continue

        flat = flatten_dict(raw)

        missing_keys = []
        null_empty_keys = []
        placeholder_mismatches = []
        valid_count = 0

        for key in canonical_keys:
            if key not in flat:
                missing_keys.append(key)
            else:
                val = flat[key]
                if is_invalid_value(val):
                    null_empty_keys.append(key)
                else:
                    valid_count += 1
                    if key in en_placeholders and isinstance(val, str):
                        expected = en_placeholders[key]
                        actual = extract_placeholders(val)
                        if key == "inventories.transfer.title":
                            if not actual.issubset(expected):
                                placeholder_mismatches.append((key, expected, actual))
                        elif expected != actual:
                            placeholder_mismatches.append((key, expected, actual))

        status = "PASS"
        if is_tier1:
            if missing_keys or null_empty_keys or placeholder_mismatches:
                status = "FAIL"
                has_tier1_failure = True
        else:
            if missing_keys or null_empty_keys or placeholder_mismatches:
                status = "INCOMPLETE"

        results.append({
            "lang": lang_code,
            "file": lf.name,
            "tier1": is_tier1,
            "error": None,
            "total": len(canonical_keys),
            "valid": valid_count,
            "null_empty": len(null_empty_keys),
            "null_empty_keys": null_empty_keys,
            "missing": len(missing_keys),
            "missing_keys": missing_keys,
            "placeholder_mismatches": placeholder_mismatches,
            "status": status,
        })

    return results, canonical_keys, has_tier1_failure


def format_markdown_report(results, canonical_count):
    lines = [
        "# Translation & i18n Audit Report",
        "",
        f"Total canonical translation keys: **{canonical_count}**",
        "",
        "| Language | Tier | Valid Keys | Null / Empty | Missing | Placeholders | Status |",
        "| :--- | :---: | :---: | :---: | :---: | :---: | :---: |",
    ]

    for r in results:
        tier_label = "Tier-1" if r["tier1"] else "Secondary"
        ph_label = "OK" if not r["placeholder_mismatches"] else f"[x] {len(r['placeholder_mismatches'])} issue(s)"
        status_badge = f"**{r['status']}**" if r["status"] == "PASS" else f"**`{r['status']}`**"
        lines.append(
            f"| `{r['lang']}` ({r['file']}) | {tier_label} | {r['valid']}/{r['total']} | {r['null_empty']} | {r['missing']} | {ph_label} | {status_badge} |"
        )

    tier1_fails = [r for r in results if r["tier1"] and r["status"] != "PASS"]
    if tier1_fails:
        lines.append("")
        lines.append("## Tier-1 Language Deficiencies")
        for f in tier1_fails:
            lines.append(f"### `{f['file']}` (Tier-1)")
            if f["null_empty_keys"]:
                lines.append(f"- **Null or empty keys ({len(f['null_empty_keys'])}):** {', '.join(f['null_empty_keys'][:10])}")
            if f["missing_keys"]:
                lines.append(f"- **Missing keys ({len(f['missing_keys'])}):** {', '.join(f['missing_keys'][:10])}")
            if f["placeholder_mismatches"]:
                lines.append(f"- **Placeholder mismatches ({len(f['placeholder_mismatches'])}):**")
                for k, exp, act in f["placeholder_mismatches"][:5]:
                    lines.append(f"  - `{k}`: expected `{sorted(exp)}`, got `{sorted(act)}`")

    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description="Audit BlockProt Reloaded translation parity and completeness.")
    parser.add_argument("--repo-root", default=str(REPO_ROOT), help="Repository root path.")
    parser.add_argument("--report-path", default="", help="Optional path to output markdown report.")
    parser.add_argument("--summary", action="store_true", help="Print markdown report to stdout.")
    args = parser.parse_args()

    results, canonical_count, failed = audit_translations(args.repo_root)
    report = format_markdown_report(results, len(canonical_count))

    if args.report_path:
        out_file = Path(args.report_path)
        out_file.parent.mkdir(parents=True, exist_ok=True)
        out_file.write_text(report, encoding="utf-8")

    gh_summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if gh_summary:
        with open(gh_summary, "a", encoding="utf-8") as f:
            f.write(report + "\n")

    if args.summary or not args.report_path:
        print(report)

    if failed:
        sys.exit(1)
    sys.exit(0)


if __name__ == "__main__":
    main()
