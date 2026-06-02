#!/usr/bin/env python3
"""Tests for release changelog extraction."""
from __future__ import annotations

import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path


REPO = Path(__file__).resolve().parent.parent
SCRIPT = REPO / "scripts" / "extract-changelog-section.py"


def load_extractor():
    spec = importlib.util.spec_from_file_location("extract_changelog_section", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def write_changelog(text: str) -> Path:
    temp_root = REPO / "build" / "extract-changelog-section-test"
    temp_root.mkdir(parents=True, exist_ok=True)
    handle = tempfile.NamedTemporaryFile("w", suffix=".md", dir=temp_root, delete=False, encoding="utf-8")
    with handle:
        handle.write(text)
    return Path(handle.name)


class ExtractChangelogSectionTest(unittest.TestCase):
    def test_extracts_plus_version_with_dated_em_dash_heading(self):
        extractor = load_extractor()
        changelog = write_changelog("""# Changelog

## Unreleased

## 0.5.0+mc26.1.2 — 2026-06-02

- Improved Copper Iron Golems.
- Improved Emerald Golem textures.

## 0.4.0+mc26.1.2 — 2026-05-28

- Added Zombie Golems.
""")

        extracted = extractor.extract_section(changelog, "0.5.0+mc26.1.2")

        self.assertEqual(
            extracted,
            "- Improved Copper Iron Golems.\n- Improved Emerald Golem textures.",
        )

    def test_missing_section_returns_empty_string(self):
        extractor = load_extractor()
        changelog = write_changelog("""# Changelog

## Unreleased

- Added future work.
""")

        self.assertEqual(extractor.extract_section(changelog, "9.9.9+mc26.1.2"), "")


if __name__ == "__main__":
    unittest.main()
