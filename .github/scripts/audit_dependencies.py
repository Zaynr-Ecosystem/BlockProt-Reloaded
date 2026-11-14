#!/usr/bin/env python3
import argparse
import concurrent.futures
import json
import os
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

if sys.stdout.encoding != "utf-8":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
GITHUB_TOKEN = os.environ.get("GITHUB_TOKEN", "")


def parse_gradle_properties(repo_root):
    path = os.path.join(repo_root, "gradle.properties")
    props = {}
    supported_mc = []
    if not os.path.exists(path):
        return props, supported_mc

    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line_s = line.strip()
            if "=" in line_s and not line_s.startswith("#"):
                k, v = line_s.split("=", 1)
                props[k.strip()] = v.strip()
            elif "Supported on blockProtVersion" in line_s:
                parts = line_s.split(":", 1)
                if len(parts) > 1:
                    supported_mc = [x.strip() for x in parts[1].split(",") if x.strip()]

    return props, supported_mc


def get_plugin_version_info(props, supported_mc):
    base_version = props.get("blockProtVersion", "Unknown")
    suffix = props.get("versionSuffix", "").strip()
    full_version = f"{base_version}-{suffix}" if suffix else base_version
    latest_mc = supported_mc[-1] if supported_mc else "Unknown"
    mc_range = f"{supported_mc[0]} - {latest_mc}" if len(supported_mc) > 1 else latest_mc
    java_target = props.get("targetJavaVersion", "21")
    supported_platforms = [p.strip() for p in props.get("supportedPlatforms", "Paper, Purpur, Folia").split(",") if p.strip()]
    legacy_platforms = [p.strip() for p in props.get("legacyPlatforms", "Spigot").split(",") if p.strip()]
    unsupported_platforms = [p.strip() for p in props.get("unsupportedPlatforms", "Forge, Fabric, NeoForge, Bedrock Dedicated Server, BungeeCord, Velocity").split(",") if p.strip()]
    unsupported_versions = props.get("unsupportedVersions", "<=1.20.4, >=26.3")
    return {
        "version": full_version,
        "base_version": base_version,
        "suffix": suffix,
        "latest_mc": latest_mc,
        "supported_mc": supported_mc,
        "mc_range": mc_range,
        "java_target": java_target,
        "supported_platforms": supported_platforms,
        "legacy_platforms": legacy_platforms,
        "unsupported_platforms": unsupported_platforms,
        "unsupported_versions": unsupported_versions,
    }


def fetch_maven_latest(url):
    headers = {"User-Agent": "BlockProt-CI-Checker (Mozilla/5.0)"}
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=12) as resp:
        root = ET.fromstring(resp.read())
        release = root.findtext("./versioning/release")
        if release:
            return release.strip()
        latest = root.findtext("./versioning/latest")
        if latest:
            return latest.strip()
        versions = [v.text.strip() for v in root.findall(".//version") if v.text]
        if versions:
            return versions[-1]
    return "Unknown"


def fetch_github_release_or_tag(repo):
    headers = {"User-Agent": "BlockProt-CI-Checker"}
    if GITHUB_TOKEN:
        headers["Authorization"] = f"Bearer {GITHUB_TOKEN}"

    url_release = f"https://api.github.com/repos/{repo}/releases/latest"
    try:
        req = urllib.request.Request(url_release, headers=headers)
        with urllib.request.urlopen(req, timeout=12) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            tag = data.get("tag_name", "")
            if tag:
                return tag.lstrip("v").strip()
    except Exception:
        pass

    url_tags = f"https://api.github.com/repos/{repo}/tags"
    req_tags = urllib.request.Request(url_tags, headers=headers)
    with urllib.request.urlopen(req_tags, timeout=12) as resp:
        data = json.loads(resp.read().decode("utf-8"))
        if data and isinstance(data, list) and len(data) > 0:
            tag = data[0].get("name", "")
            return tag.lstrip("v").strip()

    return "Unknown"


def parse_gradle_build_files(repo_root):
    """Dynamically extract declared dependency versions from build.gradle.kts files."""
    versions = {}
    spigot_path = os.path.join(repo_root, "spigot", "build.gradle.kts")
    if os.path.exists(spigot_path):
        with open(spigot_path, "r", encoding="utf-8") as f:
            content = f.read()
            m = re.search(r'id\("com\.gradleup\.shadow"\)\s*version\s*"([^"]+)"', content)
            if m:
                versions["shadow"] = m.group(1).strip()
            m = re.search(r'id\("xyz\.jpenilla\.run-paper"\)\s*version\s*"([^"]+)"', content)
            if m:
                versions["run_paper"] = m.group(1).strip()
            m = re.search(r'platform\("org\.junit:junit-bom:([^"]+)"\)', content)
            if m:
                versions["junit_bom"] = m.group(1).strip()
            m = re.search(r'mockbukkit-v1\.21:([^"]+)', content)
            if m:
                versions["mockbukkit"] = m.group(1).strip()
            m = re.search(r'mysql-connector-j:([^"]+)', content)
            if m:
                versions["mysql"] = m.group(1).strip()
            m = re.search(r'Residence:([^"]+)', content)
            if m:
                versions["residence"] = m.group(1).strip()
            m = re.search(r'GriefPrevention:GriefPrevention:([^"]+)', content)
            if m:
                versions["griefprevention"] = m.group(1).strip()
            m = re.search(r'LandsAPI:([^"]+)', content)
            if m:
                versions["lands"] = m.group(1).strip()
            m = re.search(r'claimchunk:claimchunk:([^"]+)', content)
            if m:
                versions["claimchunk"] = m.group(1).strip()
            m = re.search(r'floodgate:api:([^"]+)', content)
            if m:
                versions["floodgate"] = m.group(1).strip()
            m = re.search(r'cumulus:cumulus:([^"]+)', content)
            if m:
                versions["cumulus"] = m.group(1).strip()
            m = re.search(r'caffeine:caffeine:([^"]+)', content)
            if m:
                versions["caffeine"] = m.group(1).strip()
            m = re.search(r'commons-lang3:([^"]+)', content)
            if m:
                versions["commons_lang"] = m.group(1).strip()
            m = re.search(r'bstats-bukkit:([^"]+)', content)
            if m:
                versions["bstats"] = m.group(1).strip()
            m = re.search(r'FoliaLib:([^\s"]+)', content)
            if m:
                versions["folialib"] = m.group(1).strip()
            m = re.search(r'squirrelid:([^"]+)', content)
            if m:
                versions["squirrelid"] = m.group(1).strip()
            m = re.search(r'HikariCP:([^"]+)', content)
            if m:
                versions["hikari"] = m.group(1).strip()
            m = re.search(r'adventure-api:([^"]+)', content)
            if m:
                versions["adventure"] = m.group(1).strip()

    common_path = os.path.join(repo_root, "common", "build.gradle.kts")
    if os.path.exists(common_path):
        with open(common_path, "r", encoding="utf-8") as f:
            content = f.read()
            m = re.search(r'org\.jetbrains:annotations:([^"]+)', content)
            if m:
                versions["annotations"] = m.group(1).strip()

    return versions


def is_version_match(current, latest):
    c = current.strip().lstrip("v").lower()
    l = latest.strip().lstrip("v").lower()

    if c == l:
        return True

    if c.startswith(l) or l.startswith(c):
        return True

    try:
        def to_ints(v_str):
            clean = re.sub(r"[-+].*$", "", v_str)
            return [int(x) for x in re.findall(r"\d+", clean)]
        c_parts = to_ints(c)
        l_parts = to_ints(l)
        if c_parts and l_parts:
            if c_parts >= l_parts or (len(c_parts) >= 3 and len(l_parts) >= 3 and c_parts[:3] == l_parts[:3]):
                return True
    except Exception:
        pass

    return False


def run_target_check(item):
    category, name, current, ftype, target_endpoint = item
    try:
        if ftype == "maven":
            latest = fetch_maven_latest(target_endpoint)
        elif ftype == "github":
            latest = fetch_github_release_or_tag(target_endpoint)
        else:
            latest = "Unknown"
    except Exception as e:
        latest = f"Error: {e}"

    up_to_date = is_version_match(current, latest)
    status_indicator = "[v]" if up_to_date else "[x]"

    return {
        "category": category,
        "name": name,
        "current": current,
        "latest": latest,
        "up_to_date": up_to_date,
        "status": status_indicator,
    }


STABLE_VERSION = re.compile(r"^\d+(\.\d+)+$")


def fetch_json(url):
    request = urllib.request.Request(url, headers={"User-Agent": "BlockProt-Reloaded-Audit"})
    with urllib.request.urlopen(request, timeout=10) as response:
        return json.loads(response.read().decode("utf-8"))


def version_key(version):
    return tuple(int(part) for part in version.split("."))


def newest_published(declared, published):
    for version in reversed(declared):
        family = [v for v in published if v == version or v.startswith(version + ".")]
        if family:
            return max(family, key=version_key)
    return None


def fill_row(project, declared):
    published = [v for group in fetch_json(f"https://fill.papermc.io/v3/projects/{project}")["versions"].values()
                 for v in group if STABLE_VERSION.match(v)]
    target = newest_published(declared, published)
    if target is None:
        return "None", "None", max(published, key=version_key)
    builds = fetch_json(f"https://fill.papermc.io/v3/projects/{project}/versions/{target}/builds")
    stable = [b for b in builds if b.get("channel") in ("STABLE", "RELEASE")]
    other = [b for b in builds if b not in stable]
    stable_str = f"{target} #{max(b['id'] for b in stable)}" if stable else f"{target}: no stable build"
    other_str = f"{target} {other[0].get('channel')} #{max(b['id'] for b in other)}" if other else "None"
    return stable_str, other_str, max(published, key=version_key)


def purpur_row(declared):
    published = [v for v in fetch_json("https://api.purpurmc.org/v2/purpur")["versions"] if STABLE_VERSION.match(v)]
    target = newest_published(declared, published)
    if target is None:
        return "None", "None", max(published, key=version_key)
    build = fetch_json(f"https://api.purpurmc.org/v2/purpur/{target}").get("builds", {}).get("latest", "unknown")
    return f"{target} #{build}", "None", max(published, key=version_key)


def audit_server_platforms(version_info):
    declared = version_info.get("supported_mc", [])
    latest_declared = declared[-1] if declared else "unknown"
    results = []
    for name, tier, fetch in (("Paper", "[v] Tested", lambda: fill_row("paper", declared)),
                              ("Folia", "[v] Tested", lambda: fill_row("folia", declared)),
                              ("Purpur", "[v] Tested", lambda: purpur_row(declared))):
        row = {"platform": name, "tier": tier, "plugin_support": f"{version_info['version']} ({version_info['mc_range']})",
               "declared_mc": latest_declared}
        try:
            stable, experimental, newest = fetch()
            newer = declared and version_key(newest) > version_key(latest_declared)
            row.update({"stable_build": stable, "experimental_build": experimental,
                        "upstream_status": f"[!] Newer upstream version {newest}" if newer else "[v] No newer version"})
        except Exception as error:
            row.update({"stable_build": "Unknown", "experimental_build": "Unknown", "upstream_status": f"Error: {error}"})
        results.append(row)
    for legacy in version_info.get("legacy_platforms", []):
        results.append({"platform": legacy, "tier": "[!] Not tested", "plugin_support": "Starts with a warning",
                        "declared_mc": "N/A", "stable_build": "N/A", "experimental_build": "N/A",
                        "upstream_status": "Not checked"})
    for unsupported in version_info.get("unsupported_platforms", []):
        results.append({"platform": unsupported, "tier": "[x] Unsupported", "plugin_support": "None",
                        "declared_mc": "N/A", "stable_build": "N/A", "experimental_build": "N/A",
                        "upstream_status": "Not checked"})
    return results


def main():
    parser = argparse.ArgumentParser(description="Audit project dependencies and server platform upstream builds.")
    parser.add_argument("--json-output", help="Path to write audit results in JSON format.")
    parser.add_argument("--output-file", help="Path to write markdown audit report.")
    parser.add_argument("--no-summary", action="store_true", help="Do not write directly to GITHUB_STEP_SUMMARY.")
    args = parser.parse_args()

    props, supported_mc = parse_gradle_properties(REPO_ROOT)
    version_info = get_plugin_version_info(props, supported_mc)
    gv = parse_gradle_build_files(REPO_ROOT)

    nbt_ver = props.get("nbtApiVersion", "2.16.1")
    towny_ver = props.get("townyVersion", "0.103.2.7")
    papi_ver = props.get("papiVersion", "2.12.3")
    wg_ver = props.get("worldGuardVersion", "7.0.17")

    audit_targets = [
        ("Plugin Integration", "Towny", towny_ver, "github", "TownyAdvanced/Towny"),
        ("Plugin Integration", "PlaceholderAPI", papi_ver, "maven", "https://repo.extendedclip.com/content/repositories/placeholderapi/me/clip/placeholderapi/maven-metadata.xml"),
        ("Plugin Integration", "WorldGuard", wg_ver, "maven", "https://maven.enginehub.org/repo/com/sk89q/worldguard/worldguard-bukkit/maven-metadata.xml"),
        ("Plugin Integration", "LandsAPI", gv.get("lands", "6.28.11"), "github", "angeschossen/LandsAPI"),
        ("Plugin Integration", "ClaimChunk", gv.get("claimchunk", "0.0.25-FIX3"), "github", "cjburkey01/ClaimChunk"),
        ("Plugin Integration", "Residence", gv.get("residence", "6.0.2.3"), "github", "Zrips/Residence"),
        ("Plugin Integration", "GriefPrevention", gv.get("griefprevention", "16.18.7"), "github", "GriefPrevention/GriefPrevention"),
        ("Plugin Integration", "Floodgate API", gv.get("floodgate", "2.2.3-SNAPSHOT"), "maven", "https://repo.opencollab.dev/main/org/geysermc/floodgate/api/maven-metadata.xml"),
        ("Plugin Integration", "Cumulus (Geyser)", gv.get("cumulus", "1.1.2"), "maven", "https://repo.opencollab.dev/main/org/geysermc/cumulus/cumulus/maven-metadata.xml"),
        ("Plugin Integration", "ViaVersion", "5.12.2", "github", "ViaVersion/ViaVersion"),

        ("Core Dependency", "item-nbt-api", nbt_ver, "maven", "https://repo.codemc.org/repository/maven-public/de/tr7zw/item-nbt-api/maven-metadata.xml"),
        ("Core Dependency", "FoliaLib", gv.get("folialib", "0.5.1"), "maven", "https://repo.tcoded.com/releases/com/tcoded/FoliaLib/maven-metadata.xml"),
        ("Core Dependency", "HikariCP", gv.get("hikari", "7.1.0"), "maven", "https://repo1.maven.org/maven2/com/zaxxer/HikariCP/maven-metadata.xml"),
        ("Core Dependency", "mysql-connector-j", gv.get("mysql", "26.7.0"), "maven", "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/maven-metadata.xml"),
        ("Core Dependency", "Caffeine", gv.get("caffeine", "3.2.4"), "maven", "https://repo1.maven.org/maven2/com/github/ben-manes/caffeine/caffeine/maven-metadata.xml"),
        ("Core Dependency", "Commons Lang3", gv.get("commons_lang", "3.17.0"), "maven", "https://repo1.maven.org/maven2/org/apache/commons/commons-lang3/maven-metadata.xml"),
        ("Core Dependency", "bStats Bukkit", gv.get("bstats", "3.2.1"), "maven", "https://repo1.maven.org/maven2/org/bstats/bstats-bukkit/maven-metadata.xml"),
        ("Core Dependency", "Adventure API", gv.get("adventure", "4.17.0"), "maven", "https://repo1.maven.org/maven2/net/kyori/adventure-api/maven-metadata.xml"),
        ("Core Dependency", "JetBrains Annotations", gv.get("annotations", "26.1.0"), "maven", "https://repo1.maven.org/maven2/org/jetbrains/annotations/maven-metadata.xml"),
        ("Core Dependency", "SquirrelID", gv.get("squirrelid", "0.3.2"), "maven", "https://maven.enginehub.org/repo/org/enginehub/squirrelid/maven-metadata.xml"),

        ("Build & Test", "JUnit BOM", gv.get("junit_bom", "6.1.3"), "maven", "https://repo1.maven.org/maven2/org/junit/junit-bom/maven-metadata.xml"),
        ("Build & Test", "MockBukkit v1.21", gv.get("mockbukkit", "4.116.3"), "maven", "https://repo1.maven.org/maven2/org/mockbukkit/mockbukkit/mockbukkit-v1.21/maven-metadata.xml"),
        ("Build & Test", "Shadow Plugin", gv.get("shadow", "9.6.1"), "maven", "https://plugins.gradle.org/m2/com/gradleup/shadow/com.gradleup.shadow.gradle.plugin/maven-metadata.xml"),
        ("Build & Test", "Run-Paper Plugin", gv.get("run_paper", "3.0.2"), "maven", "https://plugins.gradle.org/m2/xyz/jpenilla/run-paper/xyz.jpenilla.run-paper.gradle.plugin/maven-metadata.xml"),
    ]

    print("BlockProt Reloaded Dependency and Integration Version Audit")
    print(f"Plugin Version: {version_info['version']}")
    print(f"Latest Declared Minecraft Support: {version_info['latest_mc']} (Range: {version_info['mc_range']})")
    print(f"Java Baseline: Bytecode {version_info['java_target']} (JDK 25 Toolchain)")

    results = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
        future_to_item = {executor.submit(run_target_check, item): item for item in audit_targets}
        for future in concurrent.futures.as_completed(future_to_item):
            results.append(future.result())

    cat_order = {"Plugin Integration": 1, "Core Dependency": 2, "Build & Test": 3}
    results.sort(key=lambda r: (cat_order.get(r["category"], 99), r["name"].lower()))

    up_to_date_count = sum(1 for r in results if r["up_to_date"])
    outdated_count = len(results) - up_to_date_count
    total_count = len(results)

    print(f"Status   {'Dependency / Integration':<26} {'Category':<22} {'Current':<16} {'Latest':<16}")
    for r in results:
        print(f"{r['status']:<8} {r['name']:<26} {r['category']:<22} {r['current']:<16} {r['latest']:<16}")
    print(f"Total: {total_count} | Up-to-date: {up_to_date_count} [v] | Updates Available: {outdated_count} [x]")

    md_lines = []
    md_lines.append("## Dependency and Plugin Integration Audit\n")
    md_lines.append(f"> **Plugin Version:** `{version_info['version']}` &nbsp;|&nbsp; "
                    f"**Latest Supported Minecraft:** `{version_info['latest_mc']}` &nbsp;|&nbsp; "
                    f"**Range:** `{version_info['mc_range']}` &nbsp;|&nbsp; "
                    f"**Java:** `{version_info['java_target']}`\n")
    md_lines.append(f"- **Summary:** `{up_to_date_count}` up-to-date [v] &nbsp;•&nbsp; `{outdated_count}` updates available [x]\n")

    category_headings = {
        "Plugin Integration": "Plugin Integrations",
        "Core Dependency": "Core Dependencies",
        "Build & Test": "Build and Test Tooling",
    }

    current_cat = None
    for r in results:
        if r["category"] != current_cat:
            if current_cat is not None:
                md_lines.append("\n")
            current_cat = r["category"]
            heading = category_headings.get(current_cat, f"{current_cat}s")
            md_lines.append(f"### {heading}\n")
            md_lines.append("| Status | Component | Declared Version | Latest Version | Evaluation |")
            md_lines.append("| :---: | :--- | :---: | :---: | :--- |")

        evaluation = "Up to date" if r["up_to_date"] else "Update available"
        md_lines.append(f"| `{r['status']}` | **{r['name']}** | `{r['current']}` | `{r['latest']}` | {evaluation} |")

    # Audit server platform software and upstream builds
    platform_results = audit_server_platforms(version_info)
    print("\nServer Platform Software & Upstream Build Audit")
    print(f"{'Platform':<26} {'Support Tier':<16} {'Plugin Support':<24} {'Declared MC':<14} {'Stable Build':<20} {'Experimental Build':<24} {'Upstream Status'}")
    for p in platform_results:
        print(f"{p['platform']:<26} {p['tier']:<16} {p['plugin_support']:<24} {p['declared_mc']:<14} {p['stable_build']:<20} {p['experimental_build']:<24} {p['upstream_status']}")

    print("\nDeclared Compatibility Boundaries:")
    print(f"  Supported Platforms:   {', '.join(version_info.get('supported_platforms', []))}")
    if version_info.get("legacy_platforms"):
        print(f"  Legacy Platforms:      {', '.join(version_info.get('legacy_platforms', []))}")
    print(f"  Unsupported Platforms: {', '.join(version_info.get('unsupported_platforms', []))}")
    print(f"  Unsupported Versions:  {version_info.get('unsupported_versions', '')}")

    md_lines.append("\n### Server Platform Software and Upstream Builds\n")
    md_lines.append("| Platform | Support Tier | Plugin Support Range | Declared Target | Latest Stable Build | Latest Experimental Build | Upstream Status |")
    md_lines.append("| :--- | :---: | :---: | :---: | :--- | :--- | :--- |")
    for p in platform_results:
        md_lines.append(f"| **{p['platform']}** | `{p['tier']}` | `{p['plugin_support']}` | `{p['declared_mc']}` | `{p['stable_build']}` | `{p['experimental_build']}` | {p['upstream_status']} |")

    md_lines.append("\n### Declared Compatibility Boundaries\n")
    md_lines.append(f"- **Supported Server Platforms:** `{', '.join(version_info.get('supported_platforms', []))}`")
    if version_info.get("legacy_platforms"):
        md_lines.append(f"- **Legacy Server Platform:** `{', '.join(version_info.get('legacy_platforms', []))}` (not tested)")
    md_lines.append(f"- **Unsupported Server Platforms:** `{', '.join(version_info.get('unsupported_platforms', []))}`")
    md_lines.append(f"- **Unsupported Minecraft Versions:** `{version_info.get('unsupported_versions', '')}`")

    md_content = "\n".join(md_lines) + "\n"

    if args.json_output:
        json_path = Path(args.json_output)
        if not json_path.is_absolute():
            json_path = Path(REPO_ROOT) / json_path
        json_path.parent.mkdir(parents=True, exist_ok=True)
        audit_payload = {
            "version_info": version_info,
            "dependency_results": results,
            "platform_results": platform_results,
            "up_to_date_count": up_to_date_count,
            "outdated_count": outdated_count,
            "total_count": total_count,
        }
        json_path.write_text(json.dumps(audit_payload, indent=2, ensure_ascii=False), encoding="utf-8")
        print(f"Audit results exported to JSON: {json_path}")

    if args.output_file:
        out_file = Path(args.output_file)
        if not out_file.is_absolute():
            out_file = Path(REPO_ROOT) / out_file
        out_file.parent.mkdir(parents=True, exist_ok=True)
        out_file.write_text(md_content, encoding="utf-8")
        print(f"Audit markdown report written to: {out_file}")

    step_summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if step_summary_path and not args.no_summary:
        try:
            with open(step_summary_path, "a", encoding="utf-8") as f:
                f.write("\n" + md_content)
        except Exception as e:
            print(f"Warning: Could not write to GITHUB_STEP_SUMMARY: {e}", file=sys.stderr)

    return 0


if __name__ == "__main__":
    sys.exit(main())
