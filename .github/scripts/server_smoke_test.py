#!/usr/bin/env python3
import argparse
import json
import re
import shutil
import subprocess
import sys
import time
import urllib.request
from pathlib import Path

FILL_API = "https://fill.papermc.io/v3/projects"
PURPUR_API = "https://api.purpurmc.org/v2/purpur"
SUMMARY_RE = re.compile(r"Console diagnostics finished: (\d+) passed, (\d+) failed \((\d+) total\)")
PLAYERS_SKIPPED = 2
ANSI_RE = re.compile(r"\x1b\[[0-9;]*m")


def fetch_json(url):
    request = urllib.request.Request(url, headers={"User-Agent": "BlockProt-Reloaded-CI"})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


def declared_versions(repo_root):
    for line in (repo_root / "gradle.properties").read_text(encoding="utf-8").splitlines():
        if line.strip().startswith("# Supported on blockProtVersion"):
            return [v.strip() for v in line.split(":", 1)[1].split(",") if v.strip()]
    return []


def purpur_versions():
    return fetch_json(PURPUR_API)["versions"]


def resolve_purpur(requested):
    if requested not in purpur_versions():
        raise SystemExit(f"No purpur build found for {requested}")
    build = fetch_json(f"{PURPUR_API}/{requested}/latest")["build"]
    return requested, build, f"{PURPUR_API}/{requested}/{build}/download"


def resolve_build(project, requested, repo_root):
    if project == "purpur":
        return resolve_purpur(requested)
    available = fetch_json(f"{FILL_API}/{project}")["versions"]
    flat = [v for group in available.values() for v in group]
    candidates = [requested] if requested else list(reversed(declared_versions(repo_root)))
    for version in candidates:
        if version in flat:
            builds = fetch_json(f"{FILL_API}/{project}/versions/{version}/builds")
            if builds:
                return version, builds[0]["id"], builds[0]["downloads"]["server:default"]["url"]
    raise SystemExit(f"No {project} build found for {requested or 'any declared version'}")


def prepare_server(workdir, jar, server_url, port):
    workdir.mkdir(parents=True, exist_ok=True)
    for stale in ("world", "world_nether", "world_the_end", "crash-reports", "session.lock"):
        stale_path = workdir / stale
        if stale_path.is_dir():
            shutil.rmtree(stale_path, ignore_errors=True)
        elif stale_path.is_file():
            stale_path.unlink(missing_ok=True)
    plugins = workdir / "plugins"
    if plugins.exists():
        shutil.rmtree(plugins)
    plugins.mkdir()
    shutil.copy(jar, plugins / jar.name)
    server_jar = workdir / "server.jar"
    request = urllib.request.Request(server_url, headers={"User-Agent": "BlockProt-Reloaded-CI"})
    with urllib.request.urlopen(request, timeout=300) as response, open(server_jar, "wb") as out:
        shutil.copyfileobj(response, out)
    (workdir / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    (workdir / "server.properties").write_text(
        f"server-port={port}\nonline-mode=false\nlevel-type=minecraft\\:flat\n"
        "generate-structures=false\nspawn-protection=0\n", encoding="utf-8")
    return server_jar


def read_log(path):
    return ANSI_RE.sub("", path.read_text(encoding="utf-8", errors="replace"))


def wait_for(path, predicate, timeout):
    deadline = time.time() + timeout
    while time.time() < deadline:
        time.sleep(2)
        text = read_log(path)
        if predicate(text):
            return text
    return read_log(path)


WATCHDOG_MARKER = "DO NOT REPORT THIS TO PAPER"


def plugin_error_lines(text):
    errors = []
    in_dump = False
    for line in text.splitlines():
        if WATCHDOG_MARKER in line:
            in_dump = not in_dump
            continue
        if in_dump:
            continue
        if ("ERROR" in line or "Exception" in line) and ("BlockProt" in line or "de.sean.blockprot" in line):
            errors.append(line)
    return errors


def run_players(args, version):
    script = Path(args.players).resolve()
    try:
        result = subprocess.run(["node", str(script), str(args.port), version], capture_output=True, text=True,
                                timeout=args.players_timeout, cwd=script.parent)
    except subprocess.TimeoutExpired:
        print("Simulated players timed out")
        return False
    print(result.stdout)
    print(result.stderr)
    if result.returncode == PLAYERS_SKIPPED:
        return None
    return result.returncode == 0


def run(args):
    players_ok = True
    repo_root = Path(__file__).resolve().parents[2]
    jar = Path(args.jar).resolve()
    workdir = Path(args.workdir).resolve()
    version, build, url = resolve_build(args.project, args.version, repo_root)
    print("=" * 64)
    print(f"  Server Smoke Test: {args.project} {version} (build {build})")
    print(f"  Port: {args.port} | Java: {args.java}")
    print("=" * 64)
    server_jar = prepare_server(workdir, jar, url, args.port)
    log_path = workdir / "console.log"
    with open(log_path, "w", encoding="utf-8") as log:
        process = subprocess.Popen([args.java, "-Xmx2G", "-jar", server_jar.name, "--nogui"], cwd=workdir,
                                   stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT, text=True)
        try:
            text = wait_for(log_path, lambda t: "Done (" in t or process.poll() is not None, args.boot_timeout)
            if "Done (" not in text:
                print("Server failed to boot within timeout:")
                print(text[-4000:])
                return 1, version, None, None
            process.stdin.write("bp debug\n")
            process.stdin.flush()
            text = wait_for(log_path, lambda t: SUMMARY_RE.search(t) is not None, args.debug_timeout)
            if args.players:
                for command in ("op BPOwner", "bp recommended blocks force", "bp reload"):
                    process.stdin.write(command + "\n")
                    process.stdin.flush()
                    time.sleep(4)
                players_ok = run_players(args, version)
            process.stdin.write("stop\n")
            process.stdin.flush()
            process.wait(timeout=120)
        finally:
            if process.poll() is None:
                process.kill()
    text = read_log(log_path)
    match = SUMMARY_RE.search(text)
    plugin_errors = plugin_error_lines(text)
    if plugin_errors:
        print("  Plugin errors detected:")
        for line in plugin_errors:
            print(f"    ! {line}")
    if match is None:
        print("  Diagnostics summary not found in console output")
        return 1, version, None, None
    passed, failed, total = (int(g) for g in match.groups())
    print(f"  Diagnostics: {passed} passed, {failed} failed ({total} total)")
    if args.players:
        p_state = "PASS" if players_ok else ("SKIP" if players_ok is None else "FAIL")
        print(f"  Simulated Players: {p_state}")
    print("=" * 64)
    return (0 if failed == 0 and not plugin_errors and players_ok is not False else 1), version, (passed, failed, total), players_ok


def append_or_update_summary(summary_file, label, version, status, detail):
    target_prefix = f"| {label} {version} |"
    new_row = f"| {label} {version} | {status} | {detail} |\n"
    summary_path = Path(summary_file)
    if not summary_path.exists():
        with open(summary_path, "w", encoding="utf-8") as out:
            out.write(new_row)
        return
    content = summary_path.read_text(encoding="utf-8")
    lines = content.splitlines(keepends=True)
    updated = False
    new_lines = []
    for line in lines:
        if line.startswith(target_prefix):
            new_lines.append(new_row)
            updated = True
        else:
            new_lines.append(line)
    if not updated:
        new_lines.append(new_row)
    summary_path.write_text("".join(new_lines), encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description="Boot a real Paper, Folia or Purpur server with the plugin and run console diagnostics.")
    parser.add_argument("--jar", required=True)
    parser.add_argument("--project", choices=["paper", "folia", "purpur"], default="paper")
    parser.add_argument("--version", default="", help="Minecraft version; defaults to the newest declared version available")
    parser.add_argument("--workdir", default="build/smoke-server")
    parser.add_argument("--port", type=int, default=25599)
    parser.add_argument("--boot-timeout", type=int, default=420)
    parser.add_argument("--debug-timeout", type=int, default=180)
    parser.add_argument("--players", default="", help="Node script that joins simulated players once diagnostics finish")
    parser.add_argument("--players-timeout", type=int, default=240)
    parser.add_argument("--summary", default="", help="Markdown file to append the result to")
    parser.add_argument("--java", default="java", help="Java executable used to run the server")
    parser.add_argument("--summary-title", default="", help="Label used in the summary row instead of the project name")
    args = parser.parse_args()
    code, version, result, players = run(args)
    if args.summary:
        status = "PASS" if code == 0 else "FAIL"
        detail = f"{result[0]} passed, {result[1]} failed ({result[2]} total)" if result else "no diagnostics summary"
        if args.players and players is None and result:
            detail += ", two-player test skipped (no client protocol data)"
        elif args.players and players:
            detail += ", two-player test passed"
        label = args.summary_title or args.project.capitalize()
        append_or_update_summary(args.summary, label, version, status, detail)
    sys.exit(code)


if __name__ == "__main__":
    main()
