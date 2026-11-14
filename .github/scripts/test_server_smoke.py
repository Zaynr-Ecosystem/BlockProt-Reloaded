import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import server_smoke_test as smoke  # noqa: E402

PAPER_MARKER = "[08:20:05 ERROR]: --- DO NOT REPORT THIS TO PAPER - THIS IS NOT A BUG OR A CRASH  - git-Paper-388 (MC: 1.18.2) ---"
PURPUR_MARKER_START = "[18:35:12 ERROR]: --- DO NOT REPORT THIS TO PURPUR - THIS IS NOT A BUG OR A CRASH  - git-Purpur-1632 (MC: 1.18.2) ---"
PURPUR_MARKER_END = "[18:35:13 ERROR]: --- DO NOT REPORT THIS TO PURPUR - THIS IS NOT A BUG OR A CRASH ---"


class PluginErrors(unittest.TestCase):
    def test_watchdog_thread_dumps_are_not_plugin_errors(self):
        text = "\n".join([
            "[08:20:04 INFO]: [BlockProt] Running console diagnostics",
            PAPER_MARKER,
            "[08:20:05 ERROR]: The server has not responded for 10 seconds! Creating thread dump",
            "[08:20:05 ERROR]: \t\tBlockProtLegacy-1.3.9.1-BEDev.jar//de.sean.blockprot.bukkit.commands.DebugCommand.checkNbt(DebugCommand.java:737)",
            PAPER_MARKER,
            "[08:20:06 INFO]: [BlockProt] Console diagnostics finished: 65 passed, 0 failed (65 total).",
        ])
        self.assertEqual([], smoke.plugin_error_lines(text))

    def test_purpur_watchdog_thread_dumps_are_not_plugin_errors(self):
        text = "\n".join([
            "[18:35:02 INFO]: [BlockProt] Running console diagnostics at world spawn (no players online)...",
            PURPUR_MARKER_START,
            "[18:35:12 ERROR]: The server has not responded for 10 seconds! Creating thread dump",
            "[18:35:13 ERROR]: \t\tBlockProtLegacy-1.4.0.1-BEDev.jar//de.sean.blockprot.bukkit.commands.DebugCommand.checkNbt(DebugCommand.java:737)",
            PURPUR_MARKER_END,
            "[18:35:14 INFO]: [BlockProt] Console diagnostics finished: 65 passed, 0 failed (65 total).",
        ])
        self.assertEqual([], smoke.plugin_error_lines(text))

    def test_real_plugin_errors_are_reported(self):
        text = "\n".join([
            "[08:20:04 ERROR]: [BlockProt Reloaded] Something broke",
            "java.lang.IllegalStateException: at de.sean.blockprot.bukkit.Foo",
        ])
        self.assertEqual(2, len(smoke.plugin_error_lines(text)))

    def test_errors_after_a_dump_are_still_reported(self):
        text = "\n".join([PAPER_MARKER, "[x ERROR]: BlockProt frame", PAPER_MARKER, "[x ERROR]: [BlockProt] real failure"])
        self.assertEqual(["[x ERROR]: [BlockProt] real failure"], smoke.plugin_error_lines(text))


if __name__ == "__main__":
    unittest.main()
