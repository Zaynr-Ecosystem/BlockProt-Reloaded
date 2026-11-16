#!/usr/bin/env python3
import argparse
import json
import re
import sys
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_PLUGIN_ID = 31548
DEFAULT_OUTPUT = REPO_ROOT / "build" / "reports" / "bstats-report.md"


def fetch_json(url, timeout=15):
    req = urllib.request.Request(url, headers={"User-Agent": "BlockProt-Reloaded-TelemetryAudit"})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.load(resp)


def extract_registered_charts(repo_root):
    blockprot = repo_root / "spigot" / "src" / "main" / "java" / "de" / "sean" / "blockprot" / "bukkit" / "BlockProt.java"
    metrics_dir = repo_root / "spigot" / "src" / "main" / "java" / "de" / "sean" / "blockprot" / "bukkit" / "metrics"
    content = blockprot.read_text(encoding="utf-8")
    classes = re.findall(r"metrics\.addCustomChart\(new\s+([A-Za-z0-9_]+)\(\)\)", content)
    charts = []
    for cls in classes:
        java_file = metrics_dir / f"{cls}.java"
        chart_id = None
        if java_file.is_file():
            m = re.search(r'super\(\s*"([^"]+)"', java_file.read_text(encoding="utf-8"))
            if m:
                chart_id = m.group(1)
        charts.append({"class": cls, "chart_id": chart_id, "file_exists": java_file.is_file()})
    return charts


def extract_structural_metrics_classes(repo_root):
    diag = repo_root / "spigot" / "src" / "main" / "java" / "de" / "sean" / "blockprot" / "bukkit" / "commands" / "debug" / "UtilityDiagnostics.java"
    content = diag.read_text(encoding="utf-8")
    return set(re.findall(r'"de\.sean\.blockprot\.bukkit\.metrics\.([A-Za-z0-9_]+)"', content))


def extract_feature_usage_keys(repo_root):
    f = repo_root / "spigot" / "src" / "main" / "java" / "de" / "sean" / "blockprot" / "bukkit" / "metrics" / "FeatureUsageBarChart.java"
    return re.findall(r'map\.put\(\s*"([^"]+)"', f.read_text(encoding="utf-8"))


def extract_config_boolean_paths(repo_root):
    cfg = repo_root / "spigot" / "src" / "main" / "resources" / "config.yml"
    paths = []
    stack = []
    for raw in cfg.read_text(encoding="utf-8").splitlines():
        stripped = raw.split("#", 1)[0].rstrip()
        if not stripped:
            continue
        indent = len(raw) - len(raw.lstrip(" "))
        m = re.match(r"^([a-zA-Z0-9_]+):\s*(.*)$", stripped.strip())
        if not m:
            continue
        key, val = m.group(1), m.group(2).strip()
        while stack and stack[-1][0] >= indent:
            stack.pop()
        stack.append((indent, key))
        if val in ("true", "false"):
            paths.append(".".join(k for _, k in stack))
    return paths


def audit_code_coverage(repo_root):
    registered = extract_registered_charts(repo_root)
    structural = extract_structural_metrics_classes(repo_root)
    missing_structural = [c["class"] for c in registered if c["class"] not in structural]
    missing_chart_ids = [c["class"] for c in registered if not c["chart_id"]]
    feature_keys = extract_feature_usage_keys(repo_root)
    config_booleans = extract_config_boolean_paths(repo_root)
    return {
        "registered_charts": registered,
        "missing_structural": missing_structural,
        "missing_chart_ids": missing_chart_ids,
        "feature_usage_keys": feature_keys,
        "config_booleans": config_booleans,
    }


def fetch_live_snapshot(plugin_id=DEFAULT_PLUGIN_ID):
    info = fetch_json(f"https://bstats.org/api/v1/plugins/{plugin_id}")
    charts = info.get("charts", {})
    snapshot = {
        "plugin_id": plugin_id,
        "name": info.get("name", ""),
        "fetched_at_utc": datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC"),
        "charts": {},
    }
    for cid, meta in charts.items():
        data = fetch_json(f"https://bstats.org/api/v1/plugins/{plugin_id}/charts/{cid}/data")
        snapshot["charts"][cid] = {
            "type": meta.get("type"),
            "title": meta.get("title"),
            "data": data,
        }
    return snapshot


def summarize_pie(data):
    if not isinstance(data, list):
        return []
    total = sum(item.get("y", 0) for item in data if isinstance(item, dict))
    rows = sorted((item for item in data if isinstance(item, dict)), key=lambda x: x.get("y", 0), reverse=True)
    return [(r.get("name", "?"), r.get("y", 0), (r.get("y", 0) * 100.0 / total) if total else 0.0) for r in rows]


def summarize_bar(data):
    if not isinstance(data, list):
        return []
    rows = []
    for item in data:
        if not isinstance(item, dict):
            continue
        vals = item.get("data") or [0]
        rows.append((item.get("name", "?"), vals[0] if vals else 0))
    return sorted(rows, key=lambda x: (-x[1], x[0]))


def format_markdown_report(coverage, snapshot=None, live_error=None):
    lines = ["# bStats Telemetry Audit"]
    lines.append("")
    lines.append("## Code Telemetry Coverage")
    lines.append(f"- Registered custom charts in `BlockProt.java`: {len(coverage['registered_charts'])}")
    chart_list = ", ".join(f"`{c['chart_id'] or c['class']}`" for c in coverage["registered_charts"])
    lines.append(f"- Chart IDs: {chart_list}")
    lines.append(f"- Tracked keys in `FeatureUsageBarChart`: {len(coverage['feature_usage_keys'])}")
    lines.append(f"- Boolean toggles in `config.yml`: {len(coverage['config_booleans'])}")
    if coverage["missing_structural"]:
        lines.append(f"- Missing in `UtilityDiagnostics.checkStructuralClasses`: {', '.join(coverage['missing_structural'])}")
    else:
        lines.append("- Structural diagnostics coverage: complete")
    if coverage["missing_chart_ids"]:
        lines.append(f"- Missing chart IDs: {', '.join(coverage['missing_chart_ids'])}")

    if live_error:
        lines.append("")
        lines.append("## Live bStats Snapshot")
        lines.append(f"- Unavailable: {live_error}")
        return "\n".join(lines) + "\n"

    if snapshot is not None:
        lines.append("")
        lines.append(f"## Live bStats Snapshot (Plugin {snapshot['plugin_id']}, {snapshot['fetched_at_utc']})")
        charts = snapshot["charts"]
        for key, label in (("servers", "Servers"), ("players", "Players")):
            series = charts.get(key, {}).get("data") or []
            if series:
                latest = series[-1][1]
                lo = min(p[1] for p in series)
                hi = max(p[1] for p in series)
                lines.append(f"- {label}: {latest} (24h range {lo}-{hi})")

        for cid, heading in (
            ("pluginVersion", "Plugin Versions"),
            ("minecraftVersion", "Minecraft Versions"),
            ("serverSoftware", "Server Software"),
            ("language", "Configured Language"),
            ("ui_mode", "UI Mode"),
            ("recommended_setup", "Recommended Setup"),
            ("dialog_api_status", "Dialog API Status"),
            ("protected_blocks_scale", "Protected Blocks Scale"),
            ("build_channel", "Build Channel"),
            ("bedrock_support", "Bedrock Support"),
            ("storage_backend", "Storage Backend"),
        ):
            c = charts.get(cid)
            if not c:
                continue
            items = summarize_pie(c["data"])
            if items:
                formatted = ", ".join(f"{name}: {val} ({pct:.1f}%)" for name, val, pct in items)
                lines.append(f"- {heading}: {formatted}")

        for cid, heading in (
            ("integrations", "Integrations"),
            ("feature_usage", "Feature Usage"),
            ("lockable_categories", "Lockable Categories"),
        ):
            c = charts.get(cid)
            if not c:
                continue
            items = summarize_bar(c["data"])
            if items:
                formatted = ", ".join(f"{name}: {val}" for name, val in items)
                lines.append(f"- {heading}: {formatted}")

        pending = [
            c["chart_id"] for c in coverage["registered_charts"]
            if c["chart_id"] and c["chart_id"] not in charts
        ]
        if pending:
            lines.append(f"- Charts in code not yet visible on bStats API: {', '.join(f'`{p}`' for p in pending)}")

    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser(description="Audit BlockProt Reloaded bStats telemetry and code coverage.")
    parser.add_argument("--plugin-id", type=int, default=DEFAULT_PLUGIN_ID)
    parser.add_argument("--offline", action="store_true", help="Skip live bStats API fetch and run code audit only.")
    parser.add_argument("--strict-coverage", action="store_true", help="Exit non-zero if any registered chart is missing from UtilityDiagnostics.")
    parser.add_argument("--output", default=str(DEFAULT_OUTPUT), help="Markdown report path.")
    parser.add_argument("--json-output", default="", help="Optional raw JSON snapshot output path.")
    args = parser.parse_args()

    coverage = audit_code_coverage(REPO_ROOT)
    snapshot = None
    live_error = None
    if not args.offline:
        try:
            snapshot = fetch_live_snapshot(args.plugin_id)
        except Exception as exc:
            live_error = str(exc)

    report = format_markdown_report(coverage, snapshot=snapshot, live_error=live_error)
    out_path = Path(args.output)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(report, encoding="utf-8")

    if args.json_output:
        json_path = Path(args.json_output)
        json_path.parent.mkdir(parents=True, exist_ok=True)
        payload = {"coverage": coverage, "snapshot": snapshot, "live_error": live_error}
        json_path.write_text(json.dumps(payload, indent=2), encoding="utf-8")

    print(report)
    if args.strict_coverage and (coverage["missing_structural"] or coverage["missing_chart_ids"]):
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
