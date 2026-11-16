#!/usr/bin/env python3
import argparse
import json
import os
import re
import subprocess
import sys
import urllib.request
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
SCRIPTS = REPO_ROOT / ".github" / "scripts"
sys.path.insert(0, str(SCRIPTS))
import audit_bstats  # noqa: E402
import edition as editions  # noqa: E402

STABLE_VERSION = re.compile(r"^\d+(\.\d+){1,2}$")


def fetch_json(url):
    request = urllib.request.Request(url, headers={"User-Agent": "BlockProt-Reloaded-CI"})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


def version_key(version):
    return tuple(int(part) for part in version.split("."))


def declared_latest():
    for line in (REPO_ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines():
        if line.strip().startswith("# Supported on blockProtVersion"):
            versions = [v.strip() for v in line.split(":", 1)[1].split(",") if v.strip()]
            return max(versions, key=version_key)
    raise SystemExit("Supported version list not found in gradle.properties")


def upstream_versions(project):
    if project == "purpur":
        return fetch_json("https://api.purpurmc.org/v2/purpur")["versions"]
    groups = fetch_json(f"https://fill.papermc.io/v3/projects/{project}")["versions"]
    return [v for group in groups.values() for v in group]


def newer_versions(project, latest):
    stable = [v for v in upstream_versions(project) if STABLE_VERSION.match(v)]
    return sorted({v for v in stable if version_key(v) > version_key(latest)}, key=version_key)


def java_for(leg):
    home = os.environ.get(f"JAVA_HOME_{leg['java']}_X64")
    return str(Path(home) / "bin" / "java") if home else "java"


def smoke_test(jar, project, version, workdir, java="java"):
    result = subprocess.run(
        [sys.executable, str(SCRIPTS / "server_smoke_test.py"), "--jar", jar, "--project", project,
         "--version", version, "--workdir", workdir, "--java", java],
        capture_output=True, text=True)
    lines = [line for line in result.stdout.splitlines() if line.strip()]
    return result.returncode == 0, lines[-1] if lines else "no output"


def dependency_findings(json_path):
    subprocess.run([sys.executable, str(SCRIPTS / "audit_dependencies.py"), "--json-output", json_path,
                    "--no-summary"], capture_output=True, text=True)
    data = json.loads(Path(json_path).read_text(encoding="utf-8"))
    return [d for d in data.get("dependency_results", []) if not d.get("up_to_date")]


def main_edition_sections(args):
    latest = declared_latest()
    sections = []
    upstream_rows = []
    smoke_rows = []
    for project in ("paper", "folia", "purpur"):
        for version in newer_versions(project, latest):
            upstream_rows.append(f"| {project.capitalize()} | {version} |")
            if project != "purpur" and args.jar:
                ok, detail = smoke_test(args.jar, project, version, f"build/weekly-{project}-{version}")
                smoke_rows.append(f"| {project.capitalize()} {version} | {'PASS' if ok else 'FAIL'} | {detail} |")
    if upstream_rows:
        sections.append("## New server versions\n\nNewer than the latest declared version "
                        f"`{latest}` in `gradle.properties`.\n\n| Software | Version |\n| :--- | :--- |\n"
                        + "\n".join(upstream_rows))
    if smoke_rows:
        sections.append("## Real server diagnostics on new versions\n\n| Server | Result | Detail |\n"
                        "| :--- | :---: | :--- |\n" + "\n".join(smoke_rows))
    outdated = dependency_findings(args.audit_json)
    if outdated:
        rows = [f"| {d['name']} | {d['category']} | `{d['current']}` | `{d['latest']}` |" for d in outdated]
        sections.append("## Outdated dependencies\n\n| Dependency | Category | Current | Latest |\n"
                        "| :--- | :--- | :--- | :--- |\n" + "\n".join(rows))
    telemetry = bstats_section()
    if telemetry:
        sections.append(telemetry)
    return sections


def bstats_section():
    coverage = audit_bstats.audit_code_coverage(REPO_ROOT)
    snapshot = None
    live_error = None
    try:
        snapshot = audit_bstats.fetch_live_snapshot()
    except Exception as exc:
        live_error = str(exc)
    return audit_bstats.format_markdown_report(coverage, snapshot=snapshot, live_error=live_error).strip()


def legacy_edition_sections(args, config):
    rows = []
    if args.jar:
        for leg in config["smoke"]:
            ok, detail = smoke_test(args.jar, leg["project"], leg["version"],
                                    f"build/weekly-{leg['project']}-{leg['version']}", java_for(leg))
            if not ok:
                rows.append(f"| {leg['project'].capitalize()} {leg['version']} | FAIL | {detail} |")
    if not rows:
        return []
    return ["## Real server diagnostics on the supported range\n\n| Server | Result | Detail |\n"
            "| :--- | :---: | :--- |\n" + "\n".join(rows)]


def build_report(args):
    config = editions.load_edition(args.edition)
    if args.edition == "bpr":
        return main_edition_sections(args)
    return legacy_edition_sections(args, config)


def report_title(edition_id):
    return "# Weekly maintenance report" if edition_id == "bpr" else f"# Weekly maintenance report [{edition_id}]"


def main():
    parser = argparse.ArgumentParser(description="Weekly upstream, compatibility and dependency check.")
    parser.add_argument("--edition", default="bpr", help="Edition id from .github/editions.json")
    parser.add_argument("--jar", default="", help="Plugin JAR used for real server tests")
    parser.add_argument("--audit-json", default="build/reports/weekly-dependency-audit.json")
    parser.add_argument("--output", default="build/reports/weekly-maintenance.md")
    args = parser.parse_args()
    Path(args.output).parent.mkdir(parents=True, exist_ok=True)
    Path(args.audit_json).parent.mkdir(parents=True, exist_ok=True)
    sections = build_report(args)
    body = (report_title(args.edition) + "\n\n" + "\n\n".join(sections) + "\n") if sections else ""
    Path(args.output).write_text(body, encoding="utf-8")
    print(body if body else "No findings.")


if __name__ == "__main__":
    main()
