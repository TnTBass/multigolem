#!/usr/bin/env python3
"""Tests for the release-note style gate."""
from __future__ import annotations

import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path


REPO = Path(__file__).resolve().parent.parent
SCRIPT = REPO / "scripts" / "check-changelog-style.py"


def load_checker():
    spec = importlib.util.spec_from_file_location("check_changelog_style", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def write_changelog(text: str) -> Path:
    temp_root = REPO / "build" / "changelog-style-test"
    temp_root.mkdir(parents=True, exist_ok=True)
    handle = tempfile.NamedTemporaryFile("w", suffix=".md", dir=temp_root, delete=False)
    with handle:
        handle.write(text)
    return Path(handle.name)


class ChangelogStyleTest(unittest.TestCase):
    def test_player_focused_unreleased_notes_pass(self):
        checker = load_checker()
        changelog = write_changelog("""# Changelog

## Unreleased

- Fixed a server watchdog crash when Diamond golems searched for targets.
- Added clearer Netherite golem fire effects.

## 0.2.0+mc26.1.2

- Added golem abilities.
""")

        self.assertEqual(checker.main(["--changelog", str(changelog), "--section", "Unreleased"]), 0)

    def test_development_log_phrasing_fails_with_guidance(self):
        checker = load_checker()
        changelog = write_changelog("""# Changelog

## Unreleased

- Task 21: Client renderer selects per-variant texture via IronGolemRendererMixin.
- V2 design doc drafted at `docs/superpowers/specs/example.md`.
""")

        result = checker.check_changelog(changelog, "Unreleased")

        self.assertFalse(result.ok)
        self.assertIn("players/server admins", result.message)
        self.assertIn("Task 21", result.message)
        self.assertIn("Mixin", result.message)
        self.assertIn("drafted at", result.message)

    def test_tag_build_checks_matching_version_section(self):
        checker = load_checker()
        changelog = write_changelog("""# Changelog

## Unreleased

- Task 99: still allowed here when checking a release tag section.

## 0.2.0+mc26.1.2 - 2026-05-16

- Added client-side variant textures.
""")

        result = checker.check_changelog(changelog, "0.2.0+mc26.1.2")

        self.assertTrue(result.ok, result.message)

    def test_missing_target_section_fails(self):
        checker = load_checker()
        changelog = write_changelog("""# Changelog

## Unreleased

- Added golem abilities.
""")

        result = checker.check_changelog(changelog, "9.9.9+mc26.1.2")

        self.assertFalse(result.ok)
        self.assertIn("Could not find changelog section", result.message)


if __name__ == "__main__":
    unittest.main()
