#!/usr/bin/env python3
"""Smoke tests for generated golem texture material identity."""
from __future__ import annotations

import importlib.util
import hashlib
import tempfile
import unittest
from pathlib import Path

from PIL import Image


REPO = Path(__file__).resolve().parent.parent
SCRIPT = REPO / "scripts" / "generate-textures.py"
GENERATED_TEXTURES = (
    "copper_golem.png",
    "gold_golem.png",
    "emerald_golem.png",
    "diamond_golem.png",
    "netherite_golem.png",
)
GENERATED_SPAWN_EGG_TEXTURES = (
    "copper_golem_spawn_egg.png",
    "gold_golem_spawn_egg.png",
    "emerald_golem_spawn_egg.png",
    "diamond_golem_spawn_egg.png",
    "netherite_golem_spawn_egg.png",
)


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


def count_pixels_in_regions(path: Path, regions, predicate) -> int:
    image = Image.open(path).convert("RGBA")
    count = 0
    for x, y, w, h in regions:
        for py in range(y, y + h):
            for px in range(x, x + w):
                r, g, b, a = image.getpixel((px, py))
                if a and predicate(r, g, b):
                    count += 1
    return count


def longest_vertical_run(path: Path, predicate) -> int:
    image = Image.open(path).convert("RGBA")
    longest = 0
    for x in range(image.width):
        current = 0
        for y in range(image.height):
            r, g, b, a = image.getpixel((x, y))
            if a and predicate(r, g, b):
                current += 1
                longest = max(longest, current)
            else:
                current = 0
    return longest


def count_columns_with_vertical_run(path: Path, predicate, minimum_run: int) -> int:
    image = Image.open(path).convert("RGBA")
    columns = 0
    for x in range(image.width):
        current = 0
        column_matches = False
        for y in range(image.height):
            r, g, b, a = image.getpixel((x, y))
            if a and predicate(r, g, b):
                current += 1
                if current >= minimum_run:
                    column_matches = True
            else:
                current = 0
        if column_matches:
            columns += 1
    return columns


def hash_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


class GenerateTexturesTest(unittest.TestCase):
    def test_texture_generation_is_deterministic(self):
        generator = load_generator()
        temp_root = REPO / "build" / "texture-test"
        temp_root.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=temp_root) as first, tempfile.TemporaryDirectory(dir=temp_root) as second:
            generator.OUT_DIR = Path(first)
            generator.SPAWN_EGG_OUT_DIR = Path(first) / "item"
            self.assertEqual(generator.main(), 0)
            first_hashes = {
                name: hash_file(Path(first) / name)
                for name in GENERATED_TEXTURES
            }
            first_spawn_egg_hashes = {
                name: hash_file(generator.SPAWN_EGG_OUT_DIR / name)
                for name in GENERATED_SPAWN_EGG_TEXTURES
            }

            generator.OUT_DIR = Path(second)
            generator.SPAWN_EGG_OUT_DIR = Path(second) / "item"
            self.assertEqual(generator.main(), 0)
            second_hashes = {
                name: hash_file(Path(second) / name)
                for name in GENERATED_TEXTURES
            }
            second_spawn_egg_hashes = {
                name: hash_file(generator.SPAWN_EGG_OUT_DIR / name)
                for name in GENERATED_SPAWN_EGG_TEXTURES
            }

            self.assertEqual(first_hashes, second_hashes)
            self.assertEqual(first_spawn_egg_hashes, second_spawn_egg_hashes)

    def test_material_details_are_visible_in_generated_outputs(self):
        generator = load_generator()
        temp_root = REPO / "build" / "texture-test"
        temp_root.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=temp_root) as tmp:
            generator.OUT_DIR = Path(tmp)
            generator.SPAWN_EGG_OUT_DIR = Path(tmp) / "item"

            self.assertEqual(generator.main(), 0)

            netherite = Path(tmp) / "netherite_golem.png"
            diamond = Path(tmp) / "diamond_golem.png"
            gold = Path(tmp) / "gold_golem.png"
            copper = Path(tmp) / "copper_golem.png"
            emerald = Path(tmp) / "emerald_golem.png"
            spawn_egg_dir = Path(tmp) / "item"
            copper_egg = spawn_egg_dir / "copper_golem_spawn_egg.png"
            gold_egg = spawn_egg_dir / "gold_golem_spawn_egg.png"
            emerald_egg = spawn_egg_dir / "emerald_golem_spawn_egg.png"
            diamond_egg = spawn_egg_dir / "diamond_golem_spawn_egg.png"
            netherite_egg = spawn_egg_dir / "netherite_golem_spawn_egg.png"

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
            gold_body_pixels = count_pixels(
                gold,
                lambda r, g, b: r >= 215 and g >= 145 and b <= 150,
            )
            gold_highlight_pixels = count_pixels(
                gold,
                lambda r, g, b: r >= 240 and g >= 205 and 60 <= b <= 150,
            )
            gold_bright_pixels = count_pixels(
                gold,
                lambda r, g, b: r >= 250 and g >= 220 and 70 <= b <= 140,
            )
            lava = lambda r, g, b: r >= 210 and 45 <= g <= 150 and b <= 65
            dim_lava = lambda r, g, b: 110 <= r <= 210 and 35 <= g <= 115 and b <= 75
            netherite_back_regions = [
                (24, 8, 8, 10), (40, 51, 18, 12), (76, 27, 4, 30),
                (76, 64, 4, 30), (53, 5, 6, 16), (76, 5, 6, 16),
            ]
            netherite_front_regions = [
                (8, 8, 8, 10), (11, 51, 18, 12), (66, 27, 4, 30),
                (66, 64, 4, 30), (42, 5, 6, 16), (65, 5, 6, 16),
            ]
            netherite_side_regions = [
                (32, 8, 8, 10), (58, 51, 18, 12), (80, 27, 4, 30),
                (80, 64, 4, 30), (59, 5, 6, 16), (82, 5, 6, 16),
            ]
            netherite_arm_regions = [
                (66, 27, 8, 30), (76, 27, 8, 30),
                (66, 64, 8, 30), (80, 64, 8, 30),
            ]
            netherite_back_lava_pixels = count_pixels_in_regions(
                netherite,
                netherite_back_regions,
                lava,
            )
            netherite_front_lava_pixels = count_pixels_in_regions(
                netherite,
                netherite_front_regions,
                lava,
            )
            netherite_side_lava_pixels = count_pixels_in_regions(
                netherite,
                netherite_side_regions,
                lava,
            )
            netherite_arm_lava_pixels = count_pixels_in_regions(
                netherite,
                netherite_arm_regions,
                lava,
            )
            netherite_eye_lava_pixels = count_pixels_in_regions(
                netherite,
                [(8, 13, 8, 3)],
                lava,
            )
            netherite_eye_dim_lava_pixels = count_pixels_in_regions(
                netherite,
                [(8, 13, 8, 3)],
                dim_lava,
            )
            netherite_lava_pixels = count_pixels(netherite, lava)
            netherite_lava_crack_columns = count_columns_with_vertical_run(
                netherite,
                lava,
                3,
            )
            netherite_dark_pixels = count_pixels(
                netherite,
                lambda r, g, b: 14 <= r <= 80 and 12 <= g <= 75 and 15 <= b <= 85,
            )
            netherite_deep_dark_pixels = count_pixels(
                netherite,
                lambda r, g, b: 8 <= r <= 45 and 8 <= g <= 42 and 10 <= b <= 50,
            )

            self.assertGreaterEqual(copper_pixels, 350)
            self.assertGreaterEqual(emerald_green_pixels, 120)
            self.assertGreaterEqual(blue_green_pixels, 250)
            self.assertLessEqual(olive_grime_pixels, 8)
            self.assertGreaterEqual(gold_body_pixels, 1200)
            self.assertGreaterEqual(gold_highlight_pixels, 90)
            self.assertGreaterEqual(gold_bright_pixels, 320)
            self.assertGreaterEqual(netherite_dark_pixels, 450)
            self.assertGreaterEqual(ember_pixels, 10)
            self.assertGreaterEqual(netherite_deep_dark_pixels, 800)
            self.assertGreaterEqual(netherite_back_lava_pixels, 12)
            self.assertGreaterEqual(netherite_front_lava_pixels, 12)
            self.assertGreaterEqual(netherite_side_lava_pixels, 12)
            self.assertGreaterEqual(netherite_arm_lava_pixels, 24)
            self.assertGreaterEqual(netherite_eye_lava_pixels, 2)
            self.assertLessEqual(netherite_eye_lava_pixels, 4)
            self.assertGreaterEqual(netherite_eye_dim_lava_pixels, 4)
            self.assertLessEqual(netherite_eye_dim_lava_pixels, 8)
            self.assertGreaterEqual(netherite_lava_crack_columns, 8)
            self.assertGreaterEqual(netherite_lava_pixels, 55)
            self.assertLessEqual(netherite_lava_pixels, 280)
            self.assertGreaterEqual(longest_vertical_run(netherite, lava), 8)

            copper_egg_pixels = count_pixels(copper_egg, lambda r, g, b: r >= 150 and 70 <= g <= 145 and b <= 110)
            patina_egg_pixels = count_pixels(copper_egg, lambda r, g, b: r <= 115 and g >= 135 and 95 <= b <= 160)
            gold_egg_pixels = count_pixels(gold_egg, lambda r, g, b: r >= 215 and g >= 145 and b <= 145)
            emerald_egg_pixels = count_pixels(emerald_egg, lambda r, g, b: r <= 100 and g >= 150 and 70 <= b <= 145)
            diamond_egg_pixels = count_pixels(diamond_egg, lambda r, g, b: 45 <= r <= 170 and g >= 160 and b >= 150)
            diamond_egg_olive_pixels = count_pixels(diamond_egg, lambda r, g, b: 95 <= r <= 165 and 105 <= g <= 165 and b <= 80)
            netherite_egg_dark_pixels = count_pixels(netherite_egg, lambda r, g, b: 8 <= r <= 70 and 8 <= g <= 65 and 10 <= b <= 80)
            netherite_egg_crack_pixels = count_pixels(netherite_egg, lava)

            self.assertGreaterEqual(copper_egg_pixels, 30)
            self.assertGreaterEqual(patina_egg_pixels, 2)
            self.assertGreaterEqual(gold_egg_pixels, 35)
            self.assertGreaterEqual(emerald_egg_pixels, 25)
            self.assertGreaterEqual(diamond_egg_pixels, 25)
            self.assertLessEqual(diamond_egg_olive_pixels, 4)
            self.assertGreaterEqual(netherite_egg_dark_pixels, 30)
            self.assertGreaterEqual(netherite_egg_crack_pixels, 2)


if __name__ == "__main__":
    unittest.main()
