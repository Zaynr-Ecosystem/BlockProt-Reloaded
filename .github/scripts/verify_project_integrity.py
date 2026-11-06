#!/usr/bin/env python3
import argparse
import os
import re
import sys
from pathlib import Path

try:
    import yaml
except ImportError:
    print("Warning: PyYAML not installed. Attempting basic verification.")
    yaml = None

REPO_ROOT = Path(__file__).resolve().parents[2]


def find_files(dir_path, extension):
    if not dir_path.exists():
        return []
    return [p for p in dir_path.rglob(f"*.{extension}") if p.is_file()]


def check_yaml_syntax(repo_root):
    results = []
    if yaml is None:
        return True, ["PyYAML not available, skipped deep YAML syntax checking"]

    yaml_files = []
    resources_dir = repo_root / "spigot" / "src" / "main" / "resources"
    if resources_dir.exists():
        yaml_files.extend(resources_dir.rglob("*.yml"))

    failed = []
    passed_count = 0
    for yf in yaml_files:
        rel = yf.relative_to(repo_root)
        try:
            with open(yf, "r", encoding="utf-8") as f:
                yaml.safe_load(f)
            passed_count += 1
        except Exception as e:
            failed.append(f"{rel}: {e}")

    if failed:
        return False, [f"YAML syntax failure in {len(failed)} file(s):"] + [f"  - {err}" for err in failed]
    return True, [f"All {passed_count} YAML resource files parsed successfully"]


def check_translation_parity(repo_root):
    key_file = repo_root / "spigot" / "src" / "main" / "java" / "de" / "sean" / "blockprot" / "bukkit" / "TranslationKey.java"
    if not key_file.exists():
        return False, ["TranslationKey.java not found"]

    key_content = key_file.read_text(encoding="utf-8")
    keys = []
    in_enum = False
    for line in key_content.splitlines():
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
            keys.append((enum_name, dotted))
        if ";" in line:
            break

    if not keys:
        return False, ["No translation keys extracted from TranslationKey.java"]

    if yaml is None:
        return True, [f"{len(keys)} keys declared in TranslationKey.java (YAML loader unavailable for parity check)"]

    def flatten(d, prefix=""):
        items = {}
        if not isinstance(d, dict):
            return items
        for k, v in d.items():
            new_key = f"{prefix}.{k}" if prefix else str(k)
            if isinstance(v, dict):
                items.update(flatten(v, new_key))
            else:
                items[new_key] = v
        return items

    tier1_langs = [
        ("en", "translations_en.yml", True),
        ("es", "translations_es.yml", True),
        ("de", "translations_de.yml", True),
        ("fr", "translations_fr.yml", True),
    ]

    def is_invalid_value(val):
        if val is None:
            return True
        s = str(val).strip()
        return s == "" or s.lower() == "null"

    details = [f"Total declared translation keys: {len(keys)}"]
    ok = True
    for lang_code, filename, strict in tier1_langs:
        lang_file = repo_root / "spigot" / "src" / "main" / "resources" / "lang" / filename
        if not lang_file.exists():
            if strict:
                return False, [f"Missing reference language file: {filename}"]
            continue
        with open(lang_file, "r", encoding="utf-8") as f:
            lang_dict = flatten(yaml.safe_load(f) or {})
        missing = [dotted for _, dotted in keys if dotted not in lang_dict or is_invalid_value(lang_dict[dotted])]
        details.append(f"{filename} coverage: {len(keys) - len(missing)}/{len(keys)}")
        if missing:
            if strict:
                ok = False
                details.append(f"Deficiencies in {filename} ({len(missing)}): {', '.join(missing[:5])}")
            else:
                details.append(f"Notice: Deficiencies in {filename} ({len(missing)}): {', '.join(missing[:5])}")

    return ok, details


def check_gpl_license_headers(repo_root):
    java_files = find_files(repo_root / "spigot" / "src", "java") + find_files(repo_root / "common" / "src", "java")
    if not java_files:
        return False, ["No Java source files found"]

    missing = []
    for jf in java_files:
        rel = jf.relative_to(repo_root)
        try:
            with open(jf, "r", encoding="utf-8") as f:
                head = "".join([f.readline() for _ in range(25)])
            if "GNU General Public License" not in head or "Copyright (C)" not in head:
                missing.append(str(rel))
        except Exception as e:
            missing.append(f"{rel} (read error: {e})")

    if missing:
        return False, [f"Missing GPL v3 license header in {len(missing)} file(s):"] + [f"  - {m}" for m in missing]
    return True, [f"All {len(java_files)} Java files have required GPL v3 license headers"]


def check_no_placeholders(repo_root):
    java_files = find_files(repo_root / "spigot" / "src", "java") + find_files(repo_root / "common" / "src", "java")
    placeholder_pattern = re.compile(r"//\s*(TODO|FIXME|XXX)\b")

    found = []
    for jf in java_files:
        rel = jf.relative_to(repo_root)
        try:
            with open(jf, "r", encoding="utf-8") as f:
                for line_num, line in enumerate(f, 1):
                    if placeholder_pattern.search(line):
                        found.append(f"{rel}:{line_num}: {line.strip()}")
        except Exception as e:
            found.append(f"{rel} (read error: {e})")

    if found:
        return False, [f"Placeholder markers found in {len(found)} location(s):"] + [f"  - {loc}" for loc in found]
    return True, [f"No placeholder markers (TODO/FIXME/XXX) found across {len(java_files)} Java files"]


def check_architectural_boundary(repo_root):
    common_java = find_files(repo_root / "common" / "src", "java")
    if not common_java:
        return True, [":common subproject has 0 files (or empty)"]

    forbidden_pattern = re.compile(r"import\s+(org\.bukkit|io\.papermc)\.")
    violations = []
    for jf in common_java:
        rel = jf.relative_to(repo_root)
        try:
            with open(jf, "r", encoding="utf-8") as f:
                for line_num, line in enumerate(f, 1):
                    if forbidden_pattern.search(line):
                        violations.append(f"{rel}:{line_num}: {line.strip()}")
        except Exception as e:
            violations.append(f"{rel} (read error: {e})")

    if violations:
        return False, [f"Architectural boundary violation in :common ({len(violations)}):"] + [f"  - {v}" for v in violations]
    return True, [f"Pure Java boundary verified in :common across {len(common_java)} files (0 Bukkit/Paper imports)"]


def check_forbidden_characters(repo_root):
    java_files = find_files(repo_root / "spigot" / "src", "java") + find_files(repo_root / "common" / "src", "java")
    bad_chars = {
        "\u2014": "em-dash",
        "\u2013": "en-dash",
        "\u2018": "left single quote",
        "\u2019": "right single quote",
        "\u201c": "left double quote",
        "\u201d": "right double quote",
        "\u2192": "right arrow",
        "\u2190": "left arrow"
    }

    found = []
    for jf in java_files:
        rel = jf.relative_to(repo_root)
        try:
            with open(jf, "r", encoding="utf-8") as f:
                for line_num, line in enumerate(f, 1):
                    for ch, name in bad_chars.items():
                        if ch in line:
                            found.append(f"{rel}:{line_num}: contains {name} ({repr(ch)})")
        except Exception as e:
            found.append(f"{rel} (read error: {e})")

    if found:
        return False, [f"Forbidden typographic characters found in {len(found)} location(s):"] + [f"  - {f}" for f in found]
    return True, [f"No forbidden typographic characters detected across {len(java_files)} Java files"]


def check_version_metadata(repo_root):
    props_path = repo_root / "gradle.properties"
    if not props_path.exists():
        return False, ["gradle.properties missing"]

    props = {}
    with open(props_path, "r", encoding="utf-8") as f:
        for line in f:
            line_s = line.strip()
            if "=" in line_s and not line_s.startswith("#"):
                k, v = line_s.split("=", 1)
                props[k.strip()] = v.strip()

    v = props.get("blockProtVersion", "unknown")
    suffix = props.get("versionSuffix", "")
    java_ver = props.get("targetJavaVersion", "unknown")

    full_ver = v if not suffix else f"{v}-{suffix}"
    is_bedev = suffix.startswith("BEDev")
    is_release = suffix == ""

    status_tag = "RELEASE (stable)" if is_release else ("SNAPSHOT (BEDev pre-release)" if is_bedev else f"CUSTOM ({suffix})")

    details = [
        f"Version: {full_ver} [{status_tag}]",
        f"baseVersion: {v}, versionSuffix: '{suffix}', targetJava: {java_ver}"
    ]
    return True, details


def main():
    parser = argparse.ArgumentParser(description="Verify BlockProt Reloaded project integrity.")
    parser.add_argument("--repo-root", default=str(REPO_ROOT), help="Path to repository root.")
    parser.add_argument("--report-path", default="build/reports/integrity-report.md", help="Output report markdown path.")
    parser.add_argument("--summary", action="store_true", help="Print step summary directly to stdout.")
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    report_file = Path(args.report_path)
    if not report_file.is_absolute():
        report_file = repo_root / report_file

    checks = [
        ("Version Metadata", check_version_metadata),
        ("YAML Configuration Syntax", check_yaml_syntax),
        ("Translation Parity (Tier-1: EN, ES, DE, FR)", check_translation_parity),
        ("GNU GPL v3 License Headers", check_gpl_license_headers),
        ("Absence of Placeholder Markers", check_no_placeholders),
        ("Architectural Boundary (:common)", check_architectural_boundary),
        ("Clean Typography & Character Check", check_forbidden_characters),
    ]

    all_ok = True
    markdown_lines = [
        "# Project Integrity & Diagnostic Verification Report\n",
        f"**Repository Root:** `{repo_root}`  \n"
    ]

    print("=================================================================")
    print("      BlockProt Reloaded Project Integrity Verification         ")
    print("=================================================================\n")

    for name, func in checks:
        ok, details = func(repo_root)
        if not ok:
            all_ok = False
        status_str = "PASS" if ok else "FAIL"
        badge = "PASS" if ok else "FAIL"
        print(f"[{status_str}] {name}")
        for d in details:
            print(f"       {d}")
        print()

        markdown_lines.append(f"### {name} - **{badge}**\n")
        for d in details:
            markdown_lines.append(f"- {d}\n")
        markdown_lines.append("\n")

    summary_status = "PASSED" if all_ok else "FAILED"
    markdown_lines.append(f"## Summary: **{summary_status}**\n")

    report_file.parent.mkdir(parents=True, exist_ok=True)
    report_file.write_text("".join(markdown_lines), encoding="utf-8")
    print(f"Integrity report written to: {report_file}")

    if args.summary:
        print("".join(markdown_lines))

    if not all_ok:
        sys.exit(1)
    sys.exit(0)


if __name__ == "__main__":
    main()
