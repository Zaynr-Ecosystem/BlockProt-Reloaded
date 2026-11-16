#!/usr/bin/env python3
import sys
import unittest
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPTS_DIR))
import audit_bstats  # noqa: E402


class AuditBstatsTests(unittest.TestCase):
    def test_code_coverage_discovers_all_registered_charts_and_diagnostics(self):
        coverage = audit_bstats.audit_code_coverage(audit_bstats.REPO_ROOT)
        chart_ids = {c["chart_id"] for c in coverage["registered_charts"]}
        self.assertEqual(coverage["missing_structural"], [])
        self.assertEqual(coverage["missing_chart_ids"], [])
        self.assertIn("feature_usage", chart_ids)
        self.assertIn("lockable_categories", chart_ids)
        self.assertIn("dialog_api_status", chart_ids)
        self.assertIn("protected_blocks_scale", chart_ids)
        self.assertIn("build_channel", chart_ids)
        self.assertIn("bedrock_support", chart_ids)
        self.assertIn("storage_backend", chart_ids)
        self.assertGreaterEqual(len(coverage["feature_usage_keys"]), 30)

    def test_markdown_report_handles_live_and_offline_snapshots(self):
        coverage = audit_bstats.audit_code_coverage(audit_bstats.REPO_ROOT)
        snapshot = {
            "plugin_id": 31548,
            "fetched_at_utc": "2026-11-15 18:00 UTC",
            "charts": {
                "servers": {"type": "single_linechart", "title": "Servers", "data": [[1, 100], [2, 109]]},
                "players": {"type": "single_linechart", "title": "Players", "data": [[1, 20], [2, 31]]},
                "ui_mode": {"type": "simple_pie", "title": "UI Mode", "data": [{"name": "Commands", "y": 6}, {"name": "Menus", "y": 2}]},
                "feature_usage": {"type": "simple_bar", "title": "Feature Usage", "data": [{"name": "Auto Reload", "data": [8]}]},
            },
        }
        report = audit_bstats.format_markdown_report(coverage, snapshot=snapshot)
        self.assertIn("Servers: 109 (24h range 100-109)", report)
        self.assertIn("UI Mode: Commands: 6 (75.0%), Menus: 2 (25.0%)", report)
        self.assertIn("Charts in code not yet visible on bStats API", report)


if __name__ == "__main__":
    unittest.main()
