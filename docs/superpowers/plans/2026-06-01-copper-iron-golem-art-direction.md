# Copper Iron Golem Art Direction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rework generated Copper Iron Golem surface textures so weathering variants share one stable layout and differ mainly by copper-stage palettes, with only subtle wax cues.

**Architecture:** Keep the existing generated-texture pipeline in `scripts/generate-textures.py`. Replace stage-specific patina and wax drawing with deterministic palette remapping plus a small shared wax-highlight pass. Strengthen `scripts/test-generate-textures.py` to enforce stable layout, palette distinction, and bounded wax deltas.

**Tech Stack:** Python 3, Pillow, existing Gradle `genTextures` and `build` tasks, Revue implementation-review.

---

### File Structure

- Modify `scripts/generate-textures.py`: add copper surface palette remapping helpers and replace stage-specific overlay geometry for Copper Iron Golem surface variants.
- Modify `scripts/test-generate-textures.py`: add tests that prove weathered textures preserve the same layout and that waxed textures only add small bounded highlights.
- Modify generated PNGs under `src/main/resources/assets/multigolem/textures/entity/iron_golem/`: regenerate the eight Copper Iron Golem surface textures.
- Do not modify gameplay, renderer, codec, spawn egg, spawner, or migration files.

### Task 1: Add Stable Layout and Bounded Wax Tests

**Files:**
- Modify: `scripts/test-generate-textures.py`

- [ ] **Step 1: Add helper functions for texture comparison**

Add these helpers near the existing image-count helpers:

```python
def alpha_mask(path: Path) -> bytes:
    image = Image.open(path).convert("RGBA")
    return bytes(a for _, _, _, a in image.getdata())


def changed_opaque_pixels(first: Path, second: Path) -> int:
    a = Image.open(first).convert("RGBA")
    b = Image.open(second).convert("RGBA")
    count = 0
    for pa, pb in zip(a.getdata(), b.getdata(), strict=True):
        if pa[3] and pb[3] and pa != pb:
            count += 1
    return count


def color_distance_pixels(first: Path, second: Path, minimum_delta: int) -> int:
    a = Image.open(first).convert("RGBA")
    b = Image.open(second).convert("RGBA")
    count = 0
    for pa, pb in zip(a.getdata(), b.getdata(), strict=True):
        if not pa[3] or not pb[3]:
            continue
        delta = abs(pa[0] - pb[0]) + abs(pa[1] - pb[1]) + abs(pa[2] - pb[2])
        if delta >= minimum_delta:
            count += 1
    return count
```

- [ ] **Step 2: Add a failing stable-layout test**

Add this test to `GenerateTexturesTest`:

```python
    def test_copper_surface_weathering_preserves_layout(self):
        generator = load_generator()
        temp_root = REPO / "build" / "texture-test"
        temp_root.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=temp_root) as tmp:
            generator.OUT_DIR = Path(tmp)
            generator.SPAWN_EGG_OUT_DIR = Path(tmp) / "item"

            self.assertEqual(generator.main(), 0)

            surface_dir = Path(tmp) / "iron_golem"
            fresh = surface_dir / "copper_golem.png"
            exposed = surface_dir / "copper_golem_exposed.png"
            weathered = surface_dir / "copper_golem_weathered.png"
            oxidized = surface_dir / "copper_golem_oxidized.png"

            self.assertEqual(alpha_mask(fresh), alpha_mask(exposed))
            self.assertEqual(alpha_mask(fresh), alpha_mask(weathered))
            self.assertEqual(alpha_mask(fresh), alpha_mask(oxidized))

            self.assertGreaterEqual(color_distance_pixels(fresh, exposed, 45), 900)
            self.assertGreaterEqual(color_distance_pixels(exposed, weathered, 45), 900)
            self.assertGreaterEqual(color_distance_pixels(weathered, oxidized, 45), 900)
```

- [ ] **Step 3: Add a failing bounded-wax test**

Add this test to `GenerateTexturesTest`:

```python
    def test_copper_surface_wax_cue_is_subtle_and_stable(self):
        generator = load_generator()
        temp_root = REPO / "build" / "texture-test"
        temp_root.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=temp_root) as tmp:
            generator.OUT_DIR = Path(tmp)
            generator.SPAWN_EGG_OUT_DIR = Path(tmp) / "item"

            self.assertEqual(generator.main(), 0)

            pairs = (
                ("copper_golem.png", "copper_golem_waxed.png"),
                ("copper_golem_exposed.png", "copper_golem_waxed_exposed.png"),
                ("copper_golem_weathered.png", "copper_golem_waxed_weathered.png"),
                ("copper_golem_oxidized.png", "copper_golem_waxed_oxidized.png"),
            )
            surface_dir = Path(tmp) / "iron_golem"
            for unwaxed_name, waxed_name in pairs:
                with self.subTest(waxed=waxed_name):
                    unwaxed = surface_dir / unwaxed_name
                    waxed = surface_dir / waxed_name
                    self.assertEqual(alpha_mask(unwaxed), alpha_mask(waxed))
                    changed = changed_opaque_pixels(unwaxed, waxed)
                    self.assertGreaterEqual(changed, 12)
                    self.assertLessEqual(changed, 80)
```

- [ ] **Step 4: Run the new tests and confirm they fail before implementation**

Run:

```powershell
python scripts\test-generate-textures.py -k copper_surface
```

Expected: at least one new test fails because current weathered/waxed variants add broad overlays and X-like wax geometry.

### Task 2: Replace Copper Surface Overlay With Palette Remapping

**Files:**
- Modify: `scripts/generate-textures.py`

- [ ] **Step 1: Add copper palettes**

Add these constants near `COPPER_SURFACES`:

```python
COPPER_STAGE_PALETTES = {
    "fresh": {
        "base": (190, 96, 58),
        "highlight": (226, 135, 96),
        "shadow": (112, 51, 30),
    },
    "exposed": {
        "base": (176, 111, 95),
        "highlight": (212, 150, 132),
        "shadow": (102, 70, 58),
    },
    "weathered": {
        "base": (91, 155, 130),
        "highlight": (134, 190, 164),
        "shadow": (55, 96, 83),
    },
    "oxidized": {
        "base": (74, 174, 153),
        "highlight": (127, 213, 188),
        "shadow": (43, 105, 96),
    },
}
```

- [ ] **Step 2: Add a luminance-based copper remap helper**

Add this function before `apply_copper_surface_details`:

```python
def remap_copper_surface_palette(img: Image.Image, stage: str) -> Image.Image:
    palette = COPPER_STAGE_PALETTES[stage]
    img = img.convert("RGBA")
    pixels = img.load()
    base = palette["base"]
    highlight = palette["highlight"]
    shadow = palette["shadow"]
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            luminance = (r * 0.299 + g * 0.587 + b * 0.114) / 255.0
            if luminance >= 0.62:
                target = highlight
                amount = 0.72
            elif luminance <= 0.34:
                target = shadow
                amount = 0.76
            else:
                target = base
                amount = 0.70
            pixels[x, y] = (
                int(r * (1.0 - amount) + target[0] * amount),
                int(g * (1.0 - amount) + target[1] * amount),
                int(b * (1.0 - amount) + target[2] * amount),
                a,
            )
    return img
```

- [ ] **Step 3: Replace weathering-specific overlay geometry**

Change `apply_copper_surface_details` so it maps stage by `patina` and calls `remap_copper_surface_palette`. Remove the stage-specific `draw_rects` and `draw_lines` blocks that draw patina patches. Keep only the subtle wax cue:

```python
def apply_copper_surface_details(img: Image.Image, patina: float, waxed: bool) -> Image.Image:
    if patina >= 0.70:
        stage = "oxidized"
    elif patina >= 0.50:
        stage = "weathered"
    elif patina >= 0.30:
        stage = "exposed"
    else:
        stage = "fresh"

    img = remap_copper_surface_palette(img, stage)

    if not waxed:
        return img

    overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    wax = (255, 228, 143, 150)
    draw_rects(draw, [
        (16, 54, 1, 1), (24, 58, 1, 1), (69, 35, 1, 1),
        (45, 13, 1, 1), (68, 13, 1, 1), (50, 56, 1, 1),
        (36, 13, 1, 1), (63, 56, 1, 1), (82, 35, 1, 1),
        (62, 13, 1, 1), (85, 13, 1, 1), (82, 78, 1, 2),
        (78, 78, 1, 2),
    ], wax)
    return Image.alpha_composite(img.convert("RGBA"), overlay)
```

- [ ] **Step 4: Run focused tests and adjust only thresholds if the new palette is clearly distinct**

Run:

```powershell
python scripts\test-generate-textures.py -k copper_surface
```

Expected: all copper-surface tests pass. If only color-distance thresholds are slightly off, inspect counts with a short Python measurement and adjust thresholds to preserve meaningful distinction.

### Task 3: Regenerate Texture Assets

**Files:**
- Modify generated PNGs under `src/main/resources/assets/multigolem/textures/entity/iron_golem/`

- [ ] **Step 1: Regenerate textures**

Run:

```powershell
python scripts\generate-textures.py
```

Expected: the eight files under `src/main/resources/assets/multigolem/textures/entity/iron_golem/` are rewritten. Non-copper variant textures should not need semantic changes.

- [ ] **Step 2: Inspect generated diff scope**

Run:

```powershell
git status --short
git diff --stat
```

Expected: changes are limited to `scripts/generate-textures.py`, `scripts/test-generate-textures.py`, and generated copper surface PNGs.

### Task 4: Verify and Commit Implementation

**Files:**
- Modified files from Tasks 1-3.

- [ ] **Step 1: Run generator tests**

Run:

```powershell
python scripts\test-generate-textures.py
```

Expected: `OK`.

- [ ] **Step 2: Run full build**

Run:

```powershell
.\gradlew.bat build
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit implementation**

Run:

```powershell
git add scripts/generate-textures.py scripts/test-generate-textures.py src/main/resources/assets/multigolem/textures/entity/iron_golem
git commit -m "fix: stabilize copper golem texture variants"
```

Expected: commit created with only the intended files.

### Task 5: Revue Implementation Review and Actioning

**Files:**
- Review committed implementation from Task 4.

- [ ] **Step 1: Run Revue doctor**

Run:

```powershell
revue-worker doctor --cwd "C:\Users\tyler\AI Projects\MultiGolem\.worktrees\phase-3-surface-state" --trusted-roots "C:\Users\tyler\AI Projects\MultiGolem\.worktrees\phase-3-surface-state"
```

Expected: `doctor: ok` and `claude_cli: ok`.

- [ ] **Step 2: Create implementation review**

Create a Revue `implementation-review` using:

- `base_sha`: the commit before Task 4.
- `files`: `scripts/generate-textures.py`, `scripts/test-generate-textures.py`, and the generated copper surface PNGs.
- `requirements`: the acceptance criteria from `docs/superpowers/specs/2026-06-01-copper-iron-golem-art-direction-design.md`.
- `known_gaps`: manual in-game art taste still requires playtesting.

- [ ] **Step 3: Run Revue packets to completion**

Run targeted `revue-worker once --review-id <review_id>` for the parent, then run queued packet work until coverage shows all intended files reviewed.

- [ ] **Step 4: Evaluate and action findings**

For each finding:

- Verify it against code and generated assets.
- Fix valid findings with TDD or deterministic asset checks.
- Mark invalid findings `false_positive` with a technical reason.
- Mark fixed findings `fixed` with `addressed_in_commit:<sha>`.

- [ ] **Step 5: Re-run verification after actioning**

Run:

```powershell
python scripts\test-generate-textures.py
.\gradlew.bat build
```

Expected: generator tests pass and build succeeds.

- [ ] **Step 6: Commit Revue actioning if code changed**

Run:

```powershell
git add <changed-files>
git commit -m "fix: action copper art review"
```

Expected: either no changes needed, or a focused actioning commit exists.

