#!/usr/bin/env python3
"""Smoke tests for generated golem texture material identity."""
from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path

from PIL import Image


REPO = Path(__file__).resolve().parent.parent
SCRIPT = REPO / "scripts" / "generate-textures.py"


def load_generator():
    spec = importlib.util.spec_from_file_location("generate_textures", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


def count_pixels(path: Path, predicate) -> int:
    image = Image.open(path).convert("RGBA")
    data = image.tobytes()
    count = 0
    for i in range(0, len(data), 4):
        r, g, b, a = data[i:i + 4]
        if a and predicate(r, g, b):
            count += 1
    return count


class GenerateTexturesTest(unittest.TestCase):
    def test_material_details_are_visible_in_generated_outputs(self):
        generator = load_generator()
        temp_root = REPO / "build" / "texture-test"
        temp_root.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=temp_root) as tmp:
            generator.OUT_DIR = Path(tmp)

            self.assertEqual(generator.main(), 0)

            netherite = Path(tmp) / "netherite_golem.png"
            diamond = Path(tmp) / "diamond_golem.png"
            gold = Path(tmp) / "gold_golem.png"
            copper = Path(tmp) / "copper_golem.png"
            emerald = Path(tmp) / "emerald_golem.png"

            copper_pixels = count_pixels(
                copper,
                lambda r, g, b: r >= 155 and 70 <= g <= 135 and b <= 95,
            )
            ember_pixels = count_pixels(
                netherite,
                lambda r, g, b: r >= 210 and 45 <= g <= 145 and b <= 55,
            )
            emerald_green_pixels = count_pixels(
                emerald,
                lambda r, g, b: r <= 95 and g >= 155 and 75 <= b <= 140,
            )
            blue_green_pixels = count_pixels(
                diamond,
                lambda r, g, b: 55 <= r <= 170 and g >= 165 and b >= 155,
            )
            olive_grime_pixels = count_pixels(
                diamond,
                lambda r, g, b: 95 <= r <= 165 and 105 <= g <= 165 and b <= 80,
            )
            gold_highlight_pixels = count_pixels(
                gold,
                lambda r, g, b: r >= 240 and g >= 205 and 60 <= b <= 150,
            )

            self.assertGreaterEqual(copper_pixels, 350)
            self.assertGreaterEqual(ember_pixels, 18)
            self.assertGreaterEqual(emerald_green_pixels, 60)
            self.assertGreaterEqual(blue_green_pixels, 250)
            self.assertGreaterEqual(olive_grime_pixels, 18)
            self.assertGreaterEqual(gold_highlight_pixels, 90)


if __name__ == "__main__":
    unittest.main()
