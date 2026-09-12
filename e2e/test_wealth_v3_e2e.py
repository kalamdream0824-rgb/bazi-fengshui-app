import importlib.util
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("run_wealth_v3_e2e.py")
SPEC = importlib.util.spec_from_file_location("run_wealth_v3_e2e", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class WealthV36VisibleCopyTest(unittest.TestCase):
    def test_repeated_annual_sentence_is_rendered_once_with_all_applicable_years(self):
        content = {
            "years": [
                {"year": 2026, "income": [{"text": "进账条件相同。"}],
                 "retention": {"text": "先留下钱。"}, "risk": None,
                 "observations": [], "actions": []},
                {"year": 2027, "income": [{"text": "进账条件相同。"}],
                 "retention": {"text": "先留下钱。"}, "risk": None,
                 "observations": [], "actions": []},
                {"year": 2028, "income": [{"text": "进账条件不同。"}],
                 "retention": {"text": "先留下钱。"}, "risk": None,
                 "observations": [], "actions": []},
            ]
        }
        group = getattr(MODULE, "group_annual_copy", lambda _content: {})(content)

        self.assertEqual(
            [
                {"firstYear": 2026, "text": "进账条件相同。", "years": [2026, 2027]},
                {"firstYear": 2028, "text": "进账条件不同。", "years": [2028]},
            ],
            group.get("income"),
        )
        self.assertEqual(
            [{"firstYear": 2026, "text": "先留下钱。", "years": [2026, 2027, 2028]}],
            group.get("retention"),
        )

    def test_current_saved_report_versions_are_explicit(self):
        self.assertEqual("wealth-narrative-v4", getattr(MODULE, "EXPECTED_CONTENT_VERSION", None))
        self.assertEqual("wealth-plain-v3.18", getattr(MODULE, "EXPECTED_COPY_VERSION", None))


if __name__ == "__main__":
    unittest.main()
