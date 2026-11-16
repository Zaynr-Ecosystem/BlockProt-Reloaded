import os
import sys
import unittest
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parent))
import weekly_maintenance  # noqa: E402


class Title(unittest.TestCase):
    def test_main_keeps_the_original_title_so_the_open_issue_is_reused(self):
        self.assertEqual("# Weekly maintenance report", weekly_maintenance.report_title("bpr"))

    def test_legacy_editions_carry_their_id(self):
        self.assertEqual("# Weekly maintenance report [bp-legacy]", weekly_maintenance.report_title("bp-legacy"))


class JavaSelection(unittest.TestCase):
    def test_uses_the_matching_java_home_when_present(self):
        with mock.patch.dict(os.environ, {"JAVA_HOME_17_X64": "/opt/jdk17"}):
            self.assertTrue(weekly_maintenance.java_for({"java": 17}).replace("\\", "/").endswith("/opt/jdk17/bin/java"))

    def test_falls_back_to_the_default_java(self):
        env = {k: v for k, v in os.environ.items() if k != "JAVA_HOME_21_X64"}
        with mock.patch.dict(os.environ, env, clear=True):
            self.assertEqual("java", weekly_maintenance.java_for({"java": 21}))


class LegacyReport(unittest.TestCase):
    def test_only_failures_are_reported(self):
        config = {"smoke": [{"project": "paper", "version": "1.20.5", "java": 21},
                            {"project": "paper", "version": "1.21", "java": 21}]}
        args = mock.Mock(jar="x.jar")
        results = iter([(True, "65 passed"), (False, "boot failed")])
        with mock.patch.object(weekly_maintenance, "smoke_test", side_effect=lambda *a, **k: next(results)):
            sections = weekly_maintenance.legacy_edition_sections(args, config)
        self.assertEqual(1, len(sections))
        self.assertIn("Paper 1.21 | FAIL", sections[0])
        self.assertNotIn("1.20.5", sections[0])

    def test_all_passing_yields_no_report(self):
        config = {"smoke": [{"project": "paper", "version": "1.20.5", "java": 21}]}
        with mock.patch.object(weekly_maintenance, "smoke_test", return_value=(True, "ok")):
            self.assertEqual([], weekly_maintenance.legacy_edition_sections(mock.Mock(jar="x.jar"), config))


class BstatsTelemetrySection(unittest.TestCase):
    def test_bstats_section_handles_network_failure_gracefully(self):
        with mock.patch.object(weekly_maintenance.audit_bstats, "fetch_live_snapshot", side_effect=OSError("offline")):
            section = weekly_maintenance.bstats_section()
        self.assertIn("## Code Telemetry Coverage", section)
        self.assertIn("Unavailable: offline", section)


if __name__ == "__main__":
    unittest.main()
