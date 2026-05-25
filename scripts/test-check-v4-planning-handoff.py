from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("check-v4-planning-handoff.py")
_CHECKER = None


def load_checker():
    global _CHECKER
    if _CHECKER is not None:
        return _CHECKER
    spec = importlib.util.spec_from_file_location("check_v4_planning_handoff", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    _CHECKER = module
    return _CHECKER


class CheckV4PlanningHandoffTest(unittest.TestCase):
    def test_good_v4_plan_passes(self):
        checker = load_checker()
        with tempfile.TemporaryDirectory() as tmp:
            root = self.write_repo(Path(tmp) / "MultiGolem" / ".worktrees" / "codex-v4-spawn-eggs")

            self.assertEqual([], checker.check(root))

    def test_fails_when_plan_was_left_in_parent_checkout(self):
        checker = load_checker()
        with tempfile.TemporaryDirectory() as tmp:
            parent = Path(tmp) / "MultiGolem"
            root = self.write_repo(parent / ".worktrees" / "codex-v4-spawn-eggs")
            parent_plan_dir = parent / "docs" / "superpowers" / "plans"
            parent_plan_dir.mkdir(parents=True)
            (parent_plan_dir / "2026-05-23-multigolem-v4-spawn-eggs.md").write_text(
                "parent checkout draft",
                encoding="utf-8",
            )

            errors = checker.check(root)

            self.assertIn("also exists in parent checkout", "\n".join(errors))

    def test_allows_same_tracked_plan_in_parent_checkout_after_v4_merge(self):
        checker = load_checker()
        with tempfile.TemporaryDirectory() as tmp:
            parent = Path(tmp) / "MultiGolem"
            root = self.write_repo(parent / ".worktrees" / "codex-v4-1")
            parent_plan_dir = parent / "docs" / "superpowers" / "plans"
            parent_plan_dir.mkdir(parents=True)
            (parent_plan_dir / "2026-05-23-multigolem-v4-spawn-eggs.md").write_text(
                self.good_plan_text(),
                encoding="utf-8",
            )

            self.assertEqual([], checker.check(root))

    def test_fails_when_review_fix_guardrails_are_missing(self):
        checker = load_checker()
        with tempfile.TemporaryDirectory() as tmp:
            root = self.write_repo(Path(tmp) / "MultiGolem" / ".worktrees" / "codex-v4-spawn-eggs")
            plan = root / "docs" / "superpowers" / "plans" / "2026-05-23-multigolem-v4-spawn-eggs.md"
            plan.write_text(
                "\n".join(checker.REQUIRED_PLAN_MARKERS),
                encoding="utf-8",
            )

            errors = checker.check(root)

            joined = "\n".join(errors)
            self.assertIn("multigolem$getNextSpawnData", joined)
            self.assertIn("context.getPlayer() == null", joined)
            self.assertIn("ComponentContents", joined)
            self.assertIn("one entity per tick", joined)
            self.assertIn("Vanilla client shows", joined)
            for marker in checker.REQUIRED_REVIEW_FIX_MARKERS:
                self.assertIn(marker, joined)

    def test_fails_when_process_lessons_are_missing(self):
        checker = load_checker()
        with tempfile.TemporaryDirectory() as tmp:
            root = self.write_repo(Path(tmp) / "MultiGolem" / ".worktrees" / "codex-v4-spawn-eggs")
            (root / "docs" / "LESSONS-LEARNED.md").write_text("", encoding="utf-8")

            errors = checker.check(root)

            joined = "\n".join(errors)
            self.assertIn('"scope_basis": "explicit-files"', joined)
            self.assertIn("Do not parallelize dependent file moves", joined)
            self.assertIn("absolute paths for new files in assigned worktrees", joined)

    def test_fails_when_handoff_markers_are_missing(self):
        checker = load_checker()
        with tempfile.TemporaryDirectory() as tmp:
            root = self.write_repo(Path(tmp) / "MultiGolem" / ".worktrees" / "codex-v4-spawn-eggs")
            (root / "docs" / "26.1.2-mojang-targets.md").write_text("", encoding="utf-8")

            errors = checker.check(root)

            joined = "\n".join(errors)
            for marker in checker.REQUIRED_HANDOFF_MARKERS:
                self.assertIn(marker, joined)

    def test_flat_checkout_with_v4_plan_skips_parent_duplicate_check(self):
        checker = load_checker()
        with tempfile.TemporaryDirectory() as tmp:
            root = self.write_repo(Path(tmp) / "MultiGolem")

            self.assertEqual([], checker.check(root))

    def test_no_v4_plan_skips_v4_specific_markers(self):
        checker = load_checker()
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp) / "MultiGolem"
            root.mkdir()
            (root / ".gitignore").write_text(".agent-review/\n", encoding="utf-8")

            self.assertEqual([], checker.check(root))

    def write_repo(self, root: Path) -> Path:
        (root / "docs" / "superpowers" / "plans").mkdir(parents=True)
        (root / ".gitignore").write_text(".agent-review/\n", encoding="utf-8")
        (root / "docs" / "LESSONS-LEARNED.md").write_text(
            "\n".join(load_checker().REQUIRED_LESSONS_MARKERS),
            encoding="utf-8",
        )
        (root / "docs" / "26.1.2-mojang-targets.md").write_text(
            "\n".join(load_checker().REQUIRED_HANDOFF_MARKERS),
            encoding="utf-8",
        )
        (root / "docs" / "superpowers" / "plans" / "2026-05-23-multigolem-v4-spawn-eggs.md").write_text(
            self.good_plan_text(),
            encoding="utf-8",
        )
        return root

    def good_plan_text(self) -> str:
        checker = load_checker()
        return "\n".join(
            [
                *checker.REQUIRED_PLAN_MARKERS,
                *checker.REQUIRED_REVIEW_FIX_MARKERS,
            ]
        )


if __name__ == "__main__":
    unittest.main()
