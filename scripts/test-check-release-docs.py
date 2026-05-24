from __future__ import annotations

import importlib.util
import json
import shutil
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("check-release-docs.py")
_CHECKER = None


def load_checker():
    global _CHECKER
    if _CHECKER is not None:
        return _CHECKER
    spec = importlib.util.spec_from_file_location("check_release_docs", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    _CHECKER = module
    return _CHECKER


class CheckReleaseDocsTest(unittest.TestCase):
    def test_good_release_docs_pass(self):
        checker = load_checker()
        with tempfile.TemporaryDirectory() as tmp:
            root = self.write_good_repo(Path(tmp) / "MultiGolem")

            self.assertEqual([], checker.check(root))

    def test_fails_on_stale_spawn_egg_copy(self):
        checker = load_checker()
        with tempfile.TemporaryDirectory() as tmp:
            root = self.write_good_repo(Path(tmp) / "MultiGolem")
            (root / "docs/modrinth-listing.md").write_text(
                "## Known Limitations\n\n- No spawn egg support yet - planned for V4.\n",
                encoding="utf-8",
            )

            errors = checker.check(root)

            joined = "\n".join(errors)
            self.assertIn("No spawn egg support yet", joined)
            self.assertIn("planned for V4", joined)

    def test_fails_on_bold_v4_next_roadmap_copy(self):
        checker = load_checker()
        with tempfile.TemporaryDirectory() as tmp:
            root = self.write_good_repo(Path(tmp) / "MultiGolem")
            readme = root / "README.md"
            readme.write_text(
                "\n".join(
                    [
                        "marked spawn egg use",
                        "marked spawn egg spawner configuration",
                        "**V4** (next): Spawn eggs for the 5 Iron Golem variants.",
                        "Marked vanilla iron golem spawn eggs",
                    ]
                ),
                encoding="utf-8",
            )

            errors = checker.check(root)

            joined = "\n".join(errors)
            self.assertIn("**V4** (next)", joined)
            self.assertIn("**V4** ✅", joined)

    def test_fails_when_modmenu_icon_metadata_is_missing(self):
        checker = load_checker()
        with tempfile.TemporaryDirectory() as tmp:
            root = self.write_good_repo(Path(tmp) / "MultiGolem")
            (root / "src/main/resources/fabric.mod.json").write_text(
                json.dumps({"schemaVersion": 1, "id": "multigolem"}),
                encoding="utf-8",
            )
            (root / "src/main/resources/assets/multigolem/icon.png").unlink()

            errors = checker.check(root)

            joined = "\n".join(errors)
            self.assertIn("fabric.mod.json must set icon", joined)
            self.assertIn("fabric.mod.json contact.homepage", joined)
            self.assertIn("fabric.mod.json contact.issues", joined)
            self.assertIn("fabric.mod.json contact.sources", joined)
            self.assertIn("icon.png does not exist", joined)

    def test_fails_when_icon_exceeds_modmenu_size_limit(self):
        checker = load_checker()
        with tempfile.TemporaryDirectory() as tmp:
            root = self.write_good_repo(Path(tmp) / "MultiGolem")
            (root / "src/main/resources/assets/multigolem/icon.png").write_bytes(b"x" * (256 * 1024 + 1))

            errors = checker.check(root)

            self.assertIn("must stay under 256 KiB", "\n".join(errors))

    def test_allows_any_modrinth_mod_homepage_slug(self):
        checker = load_checker()
        with tempfile.TemporaryDirectory() as tmp:
            root = self.write_good_repo(Path(tmp) / "MultiGolem")
            mod_json_path = root / "src/main/resources/fabric.mod.json"
            mod_json = json.loads(mod_json_path.read_text(encoding="utf-8"))
            mod_json["contact"]["homepage"] = "https://modrinth.com/mod/multi-golem"
            mod_json_path.write_text(json.dumps(mod_json), encoding="utf-8")

            self.assertEqual([], checker.check(root))

    def test_changelog_gate_ignores_generated_item_texture_outputs(self):
        build_gradle = Path(__file__).resolve().parents[1] / "build.gradle"
        text = build_gradle.read_text(encoding="utf-8")

        self.assertIn(
            r"textures\/(entity|item)\/.*\.png",
            text,
        )

    def write_good_repo(self, root: Path) -> Path:
        (root / "docs").mkdir(parents=True)
        (root / "src/main/resources/assets/multigolem/lang").mkdir(parents=True)

        for relative, markers in load_checker().REQUIRED_DOC_MARKERS.items():
            path = root / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("\n".join(markers), encoding="utf-8")

        (root / "src/main/resources/fabric.mod.json").write_text(
            json.dumps(
                {
                    "schemaVersion": 1,
                    "id": "multigolem",
                    "icon": "assets/multigolem/icon.png",
                    "contact": {
                        "homepage": "https://modrinth.com/mod/multigolem",
                        "issues": "https://github.com/TnTBass/multigolem/issues",
                        "sources": "https://github.com/TnTBass/multigolem",
                    },
                }
            ),
            encoding="utf-8",
        )
        (root / "src/main/resources/assets/multigolem/lang/en_us.json").write_text(
            json.dumps(
                {
                    "modmenu.nameTranslation.multigolem": "MultiGolem",
                    "modmenu.summaryTranslation.multigolem": "Adds material golem variants.",
                    "modmenu.descriptionTranslation.multigolem": "Adds material golem variants.",
                }
            ),
            encoding="utf-8",
        )
        shutil.copyfile(
            Path(__file__).resolve(),
            root / "src/main/resources/assets/multigolem/icon.png",
        )
        return root


if __name__ == "__main__":
    unittest.main()
