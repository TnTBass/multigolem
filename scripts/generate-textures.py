#!/usr/bin/env python3
"""Generate per-tier golem textures from the vanilla iron_golem.png template."""
from __future__ import annotations
import sys
from pathlib import Path
from PIL import Image, ImageDraw
import colorsys

REPO = Path(__file__).resolve().parent.parent
TEMPLATE = REPO / "build-inputs" / "textures" / "iron_golem.template.png"
OUT_DIR = REPO / "src" / "main" / "resources" / "assets" / "multigolem" / "textures" / "entity"

TIERS = {
    "copper":    {"hue_shift": -10, "saturation": 1.25, "lightness": 0.95},
    "gold":      {"hue_shift": 38,  "saturation": 1.70, "lightness": 1.14},
    "emerald":   {"hue_shift": 110, "saturation": 1.45, "lightness": 0.85},
    "diamond":   {"hue_shift": 165, "saturation": 1.00, "lightness": 1.12},
    "netherite": {"hue_shift": 280, "saturation": 0.34, "lightness": 0.36},
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

    return Image.alpha_composite(img.convert("RGBA"), overlay)

def main() -> int:
    if not TEMPLATE.exists():
        print(f"ERROR: template not found at {TEMPLATE}", file=sys.stderr)
        return 1
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    template = Image.open(TEMPLATE)
    for tier, params in TIERS.items():
        img = shift_hue(template.copy(), params["hue_shift"], params["saturation"], params["lightness"])
        img = apply_material_details(tier, img)
        out = OUT_DIR / f"{tier}_golem.png"
        img.save(out, "PNG")
        print(f"wrote {out.relative_to(REPO)}")
    return 0

if __name__ == "__main__":
    sys.exit(main())
