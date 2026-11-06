import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import audit_translations  # noqa: E402


class TestAuditTranslations(unittest.TestCase):
    def test_flatten_dict(self):
        nested = {
            "a": {"b": "val1", "c": {"d": "val2"}},
            "e": "val3"
        }
        flat = audit_translations.flatten_dict(nested)
        self.assertEqual(flat, {
            "a.b": "val1",
            "a.c.d": "val2",
            "e": "val3"
        })

    def test_is_invalid_value(self):
        self.assertTrue(audit_translations.is_invalid_value(None))
        self.assertTrue(audit_translations.is_invalid_value(""))
        self.assertTrue(audit_translations.is_invalid_value("   "))
        self.assertTrue(audit_translations.is_invalid_value("null"))
        self.assertTrue(audit_translations.is_invalid_value("NULL"))
        self.assertTrue(audit_translations.is_invalid_value("Null"))
        self.assertFalse(audit_translations.is_invalid_value("Valid text"))
        self.assertFalse(audit_translations.is_invalid_value(0))
        self.assertFalse(audit_translations.is_invalid_value(False))

    def test_extract_placeholders(self):
        text = "Hello {player}, your block {block} at {world} was raided by {actor}!"
        ph = audit_translations.extract_placeholders(text)
        self.assertEqual(ph, {"{player}", "{block}", "{world}", "{actor}"})
        self.assertEqual(audit_translations.extract_placeholders("No placeholders"), set())
        self.assertEqual(audit_translations.extract_placeholders(None), set())

    def test_format_markdown_report(self):
        mock_results = [
            {
                "lang": "en",
                "file": "translations_en.yml",
                "tier1": True,
                "error": None,
                "total": 10,
                "valid": 10,
                "null_empty": 0,
                "null_empty_keys": [],
                "missing": 0,
                "missing_keys": [],
                "placeholder_mismatches": [],
                "status": "PASS"
            },
            {
                "lang": "de",
                "file": "translations_de.yml",
                "tier1": True,
                "error": None,
                "total": 10,
                "valid": 9,
                "null_empty": 1,
                "null_empty_keys": ["messages.test"],
                "missing": 0,
                "missing_keys": [],
                "placeholder_mismatches": [],
                "status": "FAIL"
            }
        ]
        report = audit_translations.format_markdown_report(mock_results, 10)
        self.assertIn("# Translation & i18n Audit Report", report)
        self.assertIn("Tier-1 Language Deficiencies", report)
        self.assertIn("`translations_de.yml` (Tier-1)", report)


if __name__ == "__main__":
    unittest.main()
