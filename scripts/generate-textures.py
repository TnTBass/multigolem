#!/usr/bin/env python3
"""Generate per-tier golem textures from the vanilla iron_golem.png template."""
from __future__ import annotations
import sys
from pathlib import Path
from PIL import Image, ImageDraw
import colorsys

REPO = Path(__file__).resolve().parent.parent
TEMPLATE = REPO / "build-inputs" / "textures" / "iron_golem.template.png"
SPAWN_EGG_TEMPLATE = REPO / "build-inputs" / "textures" / "spawn_egg" / "iron_golem_spawn_egg.template.png"
OUT_DIR = REPO / "src" / "main" / "resources" / "assets" / "multigolem" / "textures" / "entity"
SPAWN_EGG_OUT_DIR = REPO / "src" / "main" / "resources" / "assets" / "multigolem" / "textures" / "item"

TIERS = {
    "copper":    {"hue_shift": -10, "saturation": 1.25, "lightness": 0.95},
    "gold":      {"hue_shift": 38,  "saturation": 1.70, "lightness": 1.14},
    "emerald":   {"hue_shift": 110, "saturation": 1.45, "lightness": 0.85},
    "diamond":   {"hue_shift": 165, "saturation": 1.00, "lightness": 1.12},
    "netherite": {"hue_shift": 280, "saturation": 0.34, "lightness": 0.36},
    "zombie":    {"hue_shift": 92,  "saturation": 0.72, "lightness": 0.62},
}

def shift_hue(img: Image.Image, hue_deg: float, sat_mul: float, lum_mul: float) -> Image.Image:
    img = img.convert("RGBA")
    pixels = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            h, l, s = colorsys.rgb_to_hls(r / 255.0, g / 255.0, b / 255.0)
            h = (h + hue_deg / 360.0) % 1.0
            s = max(0.0, min(1.0, s * sat_mul))
            l = max(0.0, min(1.0, l * lum_mul))
            nr, ng, nb = colorsys.hls_to_rgb(h, l, s)
            pixels[x, y] = (int(nr * 255), int(ng * 255), int(nb * 255), a)
    return img

def blend_material(img: Image.Image, color: tuple[int, int, int], amount: float) -> Image.Image:
    img = img.convert("RGBA")
    pixels = img.load()
    tr, tg, tb = color
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            pixels[x, y] = (
                int(r * (1.0 - amount) + tr * amount),
                int(g * (1.0 - amount) + tg * amount),
                int(b * (1.0 - amount) + tb * amount),
                a,
            )
    return img

def draw_rects(draw: ImageDraw.ImageDraw, rects: list[tuple[int, int, int, int]], fill: tuple[int, int, int, int]) -> None:
    for x, y, w, h in rects:
        draw.rectangle((x, y, x + w - 1, y + h - 1), fill=fill)

def draw_lines(draw: ImageDraw.ImageDraw, lines: list[list[tuple[int, int]]], fill: tuple[int, int, int, int], width: int = 1) -> None:
    for line in lines:
        draw.line(line, fill=fill, width=width)

def apply_material_details(tier: str, img: Image.Image) -> Image.Image:
    overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)

    if tier == "copper":
        img = blend_material(img, (190, 96, 58), 0.24)
        draw_lines(draw, [
            [(19, 52), (16, 61), (18, 70)],
            [(66, 29), (68, 41), (65, 54)],
            [(42, 8), (44, 15), (43, 22)],
        ], (52, 145, 104, 150))
        draw_rects(draw, [
            (12, 52, 17, 11), (67, 28, 5, 26), (42, 6, 6, 16),
            (65, 6, 6, 16),
        ], (204, 111, 61, 120))
        draw_rects(draw, [(23, 55, 2, 2), (69, 37, 2, 3), (45, 17, 2, 2)], (79, 178, 124, 180))

    elif tier == "gold":
        img = blend_material(img, (255, 205, 24), 0.46)
        draw_rects(draw, [
            (12, 52, 17, 6), (16, 58, 10, 4), (67, 28, 5, 24),
            (41, 5, 6, 2), (64, 5, 6, 2), (42, 11, 5, 8),
            (65, 11, 5, 8), (44, 16, 4, 5), (67, 16, 4, 5),
        ], (255, 241, 92, 230))
        draw_rects(draw, [
            (27, 52, 2, 11), (72, 28, 2, 25), (48, 5, 2, 17),
            (71, 5, 2, 17),
        ], (124, 71, 7, 130))

    elif tier == "diamond":
        img = blend_material(img, (72, 207, 218), 0.34)
        draw_lines(draw, [
            [(12, 59), (19, 52), (28, 59)],
            [(66, 31), (70, 39), (66, 49)],
            [(42, 7), (48, 14), (42, 21)],
            [(65, 7), (70, 14), (65, 21)],
        ], (235, 255, 247, 190))
        draw_lines(draw, [
            [(16, 52), (22, 63)],
            [(69, 28), (66, 43), (70, 56)],
            [(44, 6), (48, 20)],
        ], (20, 137, 151, 150))
        draw_rects(draw, [
            (67, 40, 3, 9), (13, 56, 3, 6), (42, 15, 3, 6),
            (20, 61, 3, 4), (24, 56, 2, 5), (45, 12, 2, 4),
            (69, 10, 2, 5), (66, 48, 2, 8),
        ], (50, 176, 189, 160))

    elif tier == "netherite":
        img = blend_material(img, (12, 11, 16), 0.66)
        draw_rects(draw, [
            (12, 52, 7, 4), (22, 58, 6, 5), (68, 28, 5, 8),
            (41, 6, 4, 7), (66, 14, 4, 7), (40, 51, 18, 12),
            (76, 28, 4, 8), (54, 13, 4, 7), (82, 64, 4, 8),
        ], (14, 13, 17, 125))
        lava_cracks = [
            [(14, 52), (14, 55), (16, 58)],
            [(16, 58), (15, 62)],
            [(68, 29), (68, 37), (70, 43), (68, 53)],
            [(72, 31), (71, 39), (73, 48), (72, 56)],
            [(43, 8), (43, 12)],
            [(44, 52), (44, 57), (46, 63)],
            [(51, 52), (52, 57), (54, 62)],
            [(55, 12), (55, 17)],
            [(77, 29), (78, 38), (76, 47), (79, 56)],
            [(82, 64), (83, 74), (81, 83), (84, 93)],
            [(67, 65), (69, 74), (68, 84), (71, 93)],
            [(80, 28), (82, 37), (80, 46), (83, 55)],
            [(60, 53), (66, 56), (72, 62)],
            [(66, 13), (69, 17), (68, 21)],
            [(76, 7), (78, 13), (77, 20)],
        ]
        draw_lines(draw, lava_cracks, (104, 27, 16, 155), width=2)
        draw_lines(draw, lava_cracks, (255, 91, 29, 255), width=1)
        draw_rects(draw, [
            (16, 58, 1, 1), (70, 35, 1, 1), (46, 60, 1, 1),
            (55, 17, 1, 1), (77, 34, 1, 1), (82, 69, 1, 1),
            (68, 52, 1, 2), (72, 55, 1, 2), (79, 55, 1, 2),
            (84, 92, 1, 2), (71, 92, 1, 2), (83, 54, 1, 2),
        ], (255, 91, 29, 255))
        draw_rects(draw, [
            (14, 54, 1, 1), (68, 32, 1, 1), (44, 55, 1, 1),
            (55, 15, 1, 1), (77, 32, 1, 1), (82, 67, 1, 1),
            (69, 44, 1, 1), (72, 48, 1, 1), (78, 47, 1, 1),
            (83, 75, 1, 1), (68, 84, 1, 1), (82, 37, 1, 1),
        ], (255, 154, 45, 230))
        draw_rects(draw, [
            (8, 14, 1, 1), (10, 14, 1, 1),
            (13, 14, 1, 1), (15, 14, 1, 1),
        ], (146, 58, 31, 190))
        draw_rects(draw, [(9, 14, 1, 1), (14, 14, 1, 1)], (255, 91, 29, 255))

    elif tier == "emerald":
        img = blend_material(img, (32, 176, 88), 0.38)
        draw_rects(draw, [
            (12, 52, 17, 10), (68, 34, 4, 13), (43, 11, 4, 9),
            (66, 11, 3, 8),
        ], (45, 215, 107, 215))
        draw_lines(draw, [
            [(12, 52), (18, 57), (28, 52)],
            [(66, 28), (71, 38), (66, 54)],
            [(42, 6), (48, 15), (42, 22)],
        ], (24, 99, 58, 150))

    elif tier == "zombie":
        img = blend_material(img, (68, 108, 54), 0.42)
        draw_rects(draw, [
            (12, 52, 17, 11), (66, 28, 5, 25), (42, 6, 6, 16),
            (65, 6, 6, 16), (40, 51, 18, 12), (76, 64, 4, 28),
        ], (57, 88, 43, 150))
        draw_lines(draw, [
            [(13, 52), (17, 58), (15, 64)],
            [(68, 29), (67, 39), (70, 50)],
            [(43, 7), (45, 14), (43, 21)],
            [(66, 8), (69, 15), (67, 21)],
            [(42, 52), (50, 57), (57, 62)],
        ], (101, 132, 61, 185), width=2)
        draw_rects(draw, [
            (14, 55, 3, 3), (22, 59, 4, 2), (69, 36, 2, 5),
            (44, 15, 2, 3), (67, 17, 3, 2), (51, 56, 4, 3),
        ], (138, 171, 86, 220))
        draw_rects(draw, [
            (8, 13, 2, 2), (13, 13, 2, 2),
        ], (177, 36, 28, 220))

    return Image.alpha_composite(img.convert("RGBA"), overlay)

def apply_spawn_egg_material_details(tier: str, img: Image.Image) -> Image.Image:
    overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)

    if tier == "copper":
        img = blend_material(img, (190, 96, 58), 0.30)
        draw_rects(draw, [(4, 4, 4, 3), (9, 9, 3, 2), (6, 12, 2, 1)], (211, 112, 61, 190))
        draw_rects(draw, [(8, 4, 2, 2), (5, 10, 2, 2), (11, 6, 1, 2)], (70, 169, 119, 230))
    elif tier == "gold":
        img = blend_material(img, (255, 205, 24), 0.50)
        draw_rects(draw, [(4, 4, 5, 3), (8, 8, 4, 3), (5, 12, 3, 1)], (255, 239, 91, 230))
        draw_rects(draw, [(11, 5, 1, 5), (7, 12, 4, 1)], (128, 74, 8, 130))
    elif tier == "emerald":
        img = blend_material(img, (32, 176, 88), 0.44)
        draw_rects(draw, [(4, 4, 5, 3), (8, 9, 4, 2), (6, 12, 2, 1)], (45, 220, 111, 225))
        draw_lines(draw, [[(4, 4), (8, 7), (12, 4)], [(5, 11), (9, 8), (12, 11)]], (22, 95, 56, 160))
    elif tier == "diamond":
        img = blend_material(img, (72, 207, 218), 0.42)
        draw_lines(draw, [[(4, 5), (7, 3), (10, 5)], [(5, 11), (8, 8), (12, 11)]], (232, 255, 247, 210))
        draw_rects(draw, [(9, 5, 2, 2), (6, 9, 2, 2), (11, 10, 1, 2)], (42, 170, 187, 190))
    elif tier == "netherite":
        img = blend_material(img, (12, 11, 16), 0.72)
        draw_rects(draw, [(4, 4, 4, 3), (9, 8, 3, 3), (5, 12, 4, 1)], (17, 15, 20, 170))
        cracks = [[(5, 4), (5, 7), (6, 9)], [(9, 5), (10, 8), (9, 12)], [(12, 4), (11, 7), (12, 10)]]
        draw_lines(draw, cracks, (106, 28, 17, 170), width=2)
        draw_lines(draw, cracks, (255, 91, 29, 255), width=1)
    elif tier == "zombie":
        img = blend_material(img, (68, 108, 54), 0.48)
        draw_rects(draw, [(4, 4, 5, 3), (9, 8, 3, 3), (5, 12, 4, 1)], (59, 92, 44, 190))
        draw_lines(draw, [[(5, 4), (6, 7), (5, 11)], [(10, 5), (9, 8), (11, 12)]], (131, 162, 84, 220))
        draw_rects(draw, [(7, 5, 1, 1), (11, 7, 1, 1)], (177, 36, 28, 230))
    return Image.alpha_composite(img.convert("RGBA"), overlay)

def generate_spawn_egg_icon(tier: str, params: dict[str, float]) -> Image.Image:
    base = Image.open(SPAWN_EGG_TEMPLATE).convert("RGBA")
    img = shift_hue(base.copy(), params["hue_shift"], params["saturation"], params["lightness"])
    return apply_spawn_egg_material_details(tier, img)

def main() -> int:
    if not TEMPLATE.exists():
        print(f"ERROR: template not found at {TEMPLATE}", file=sys.stderr)
        return 1
    if not SPAWN_EGG_TEMPLATE.exists():
        print(f"ERROR: spawn egg template not found at {SPAWN_EGG_TEMPLATE}", file=sys.stderr)
        return 1
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    SPAWN_EGG_OUT_DIR.mkdir(parents=True, exist_ok=True)
    template = Image.open(TEMPLATE)
    for tier, params in TIERS.items():
        img = shift_hue(template.copy(), params["hue_shift"], params["saturation"], params["lightness"])
        img = apply_material_details(tier, img)
        out = OUT_DIR / f"{tier}_golem.png"
        img.save(out, "PNG")
        print(f"wrote {out.relative_to(REPO)}")
        spawn_egg = generate_spawn_egg_icon(tier, params)
        spawn_egg_out = SPAWN_EGG_OUT_DIR / f"{tier}_golem_spawn_egg.png"
        spawn_egg.save(spawn_egg_out, "PNG")
        print(f"wrote {spawn_egg_out.relative_to(REPO)}")
    return 0

if __name__ == "__main__":
    sys.exit(main())
