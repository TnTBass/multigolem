#!/usr/bin/env python3
"""Generate per-tier golem textures from the vanilla iron_golem.png template."""
from __future__ import annotations
import sys
from pathlib import Path
from PIL import Image
import colorsys

REPO = Path(__file__).resolve().parent.parent
TEMPLATE = REPO / "build-inputs" / "textures" / "iron_golem.template.png"
OUT_DIR = REPO / "src" / "main" / "resources" / "assets" / "multigolem" / "textures" / "entity"

TIERS = {
    "copper":    {"hue_shift": -10, "saturation": 1.25, "lightness": 0.95},
    "gold":      {"hue_shift": 35,  "saturation": 1.55, "lightness": 1.10},
    "emerald":   {"hue_shift": 110, "saturation": 1.45, "lightness": 0.85},
    "diamond":   {"hue_shift": 165, "saturation": 0.75, "lightness": 1.20},
    "netherite": {"hue_shift": 280, "saturation": 0.40, "lightness": 0.40},
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

def main() -> int:
    if not TEMPLATE.exists():
        print(f"ERROR: template not found at {TEMPLATE}", file=sys.stderr)
        return 1
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    template = Image.open(TEMPLATE)
    for tier, params in TIERS.items():
        img = shift_hue(template.copy(), params["hue_shift"], params["saturation"], params["lightness"])
        out = OUT_DIR / f"{tier}_golem.png"
        img.save(out, "PNG")
        print(f"wrote {out.relative_to(REPO)}")
    return 0

if __name__ == "__main__":
    sys.exit(main())
