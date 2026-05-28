from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("check-zombie-golem-planning-handoff.py")
_CHECKER = None


def load_checker():
    global _CHECKER
    if _CHECKER is not None:
        return _CHECKER
    spec = importlib.util.spec_from_file_location("check_zombie_golem_planning_handoff", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    _CHECKER = module
    return _CHECKER


class CheckZombieGolemPlanningHandoffTest(unittest.TestCase):
    def test_good_repo_without_plan_passes(self):
        checker = load_checker()
        with tempfile.TemporaryDirectory() as tmp:
            root = self.write_repo(Path(tmp) / "MultiGolem", write_plan=False)

            self.assertEqual([], checker.check(root))

    def test_good_plan_passes(self):
        checker = load_checker()
        with tempfile.TemporaryDirectory() as tmp:
            root = self.write_repo(Path(tmp) / "MultiGolem", write_plan=True)

            self.assertEqual([], checker.check(root))

    def test_fails_when_spec_markers_are_missing(self):
        checker = load_checker()
        with tempfile.TemporaryDirectory() as tmp:
            root = self.write_repo(Path(tmp) / "MultiGolem", write_plan=False)
            (root / checker.SPEC_PATH).write_text("", encoding="utf-8")

            errors = checker.check(root)

            joined = "\n".join(errors)
            self.assertIn("Mossy Cobblestone", joined)
            self.assertIn("Rotten Flesh", joined)
            self.assertIn("zombie_village_spawning", joined)

    def test_fails_when_lessons_are_missing(self):
        checker = load_checker()
        with tempfile.TemporaryDirectory() as tmp:
            root = self.write_repo(Path(tmp) / "MultiGolem", write_plan=False)
            (root / checker.LESSONS_PATH).write_text("", encoding="utf-8")

            errors = checker.check(root)

            joined = "\n".join(errors)
            self.assertIn("desired-count maintenance rule", joined)
            self.assertIn("regular zombies alone must never qualify", joined)
            self.assertIn("Civilian conversion is the signature mechanic", joined)

    def test_fails_when_plan_behavior_markers_are_missing(self):
        checker = load_checker()
        with tempfile.TemporaryDirectory() as tmp:
            root = self.write_repo(Path(tmp) / "MultiGolem", write_plan=True)
            plan = root / "docs" / "superpowers" / "plans" / "2026-05-25-multigolem-zombie-golem.md"
            plan.write_text("\n".join(checker.REQUIRED_PLAN_SPIKE_MARKERS), encoding="utf-8")

            errors = checker.check(root)

            joined = "\n".join(errors)
            self.assertIn("Mossy Cobblestone", joined)
            self.assertIn("multigolem.create.zombie", joined)
            self.assertIn("non-zombie MultiGolems", joined)

    def test_fails_when_plan_spike_markers_are_missing(self):
        checker = load_checker()
        with tempfile.TemporaryDirectory() as tmp:
            root = self.write_repo(Path(tmp) / "MultiGolem", write_plan=True)
            plan = root / "docs" / "superpowers" / "plans" / "2026-05-25-multigolem-zombie-golem.md"
            plan.write_text("\n".join(checker.REQUIRED_PLAN_MARKERS), encoding="utf-8")

            errors = checker.check(root)

            joined = "\n".join(errors)
            self.assertIn("village-area maintenance", joined)
            self.assertIn("Rotten Flesh healing", joined)

    def write_repo(self, root: Path, write_plan: bool) -> Path:
        checker = load_checker()
        (root / "docs" / "superpowers" / "specs").mkdir(parents=True)
        (root / "docs" / "superpowers" / "plans").mkdir(parents=True)
        (root / "docs").mkdir(exist_ok=True)
        (root / checker.SPEC_PATH).write_text(
            "\n".join(checker.REQUIRED_SPEC_MARKERS),
            encoding="utf-8",
        )
        (root / checker.LESSONS_PATH).write_text(
            "\n".join(checker.REQUIRED_LESSONS_MARKERS),
            encoding="utf-8",
        )
        if write_plan:
            (root / "docs" / "superpowers" / "plans" / "2026-05-25-multigolem-zombie-golem.md").write_text(
                "\n".join([*checker.REQUIRED_PLAN_MARKERS, *checker.REQUIRED_PLAN_SPIKE_MARKERS]),
                encoding="utf-8",
            )
        return root


if __name__ == "__main__":
    unittest.main()
