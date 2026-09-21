#!/usr/bin/env python3
"""Tests for release changelog extraction."""
from __future__ import annotations

import importlib.util
import re
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
    def test_compatibility_notes_are_specific_to_each_loader(self):
        extractor = load_extractor()
        combined = "- Updated compatibility to Minecraft 26.3 for Fabric and NeoForge."
        unrelated = "- Fixed connections between Fabric and NeoForge clients."
        changelog = write_changelog(f"## 0.8.2+mc26.3 — 2026-09-21\n\n{combined}\n{unrelated}\n")
        self.assertEqual(extractor.extract_section(changelog, "0.8.2+mc26.3"),
                         f"{combined}\n{unrelated}")
        for loader, name in (("fabric", "Fabric"), ("neoforge", "NeoForge")):
            with self.subTest(loader=loader):
                self.assertEqual(
                    extractor.extract_section(changelog, "0.8.2+mc26.3", loader),
                    f"- Updated compatibility to Minecraft 26.3 for {name}.\n{unrelated}",
                )

    def test_release_workflow_routes_notes_to_the_matching_upload(self):
        workflow = (REPO / ".github/workflows/release.yml").read_text(encoding="utf-8")
        uploads = re.findall(
            r'\./scripts/upload-(modrinth|curseforge)\.ps1\s+`\s*'
            r'-Slug "multigolem"\s+`\s*-Loader "(fabric|neoforge)"'
            r'.*?-ChangelogPath "([^"]+)"', workflow, re.DOTALL,
        )
        self.assertEqual(len(uploads), 4)
        self.assertEqual({(publisher, loader) for publisher, loader, _ in uploads},
                         {(publisher, loader) for publisher in ("modrinth", "curseforge")
                          for loader in ("fabric", "neoforge")})
        for publisher, loader, notes_path in uploads:
            with self.subTest(publisher=publisher, loader=loader):
                self.assertEqual(notes_path, f"release-notes-{loader}.md")
                self.assertIn(f'--loader {loader} > {notes_path}', workflow)
        self.assertIn('notes_args=(--notes-file release-notes.md)', workflow)

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
