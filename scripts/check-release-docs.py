from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]

STALE_PHRASES = [
    "No spawn egg support yet",
    "planned for V4",
    "V4 (next)",
    "**V4** (next)",
]

REQUIRED_DOC_MARKERS = {
    "README.md": [
        "marked spawn egg use",
        "marked spawn egg spawner configuration",
        "**V4** ✅",
        "Marked vanilla iron golem spawn eggs",
    ],
    "docs/modrinth-listing.md": [
        "Marked vanilla iron golem spawn eggs",
        "marked spawn egg use",
        "marked spawn egg spawner configuration",
    ],
    "docs/curseforge-listing.md": [
        "Marked vanilla iron golem spawn eggs",
        "marked spawn egg use",
        "marked spawn egg spawner configuration",
    ],
    "docs/playtest-checklist.md": [
        "Unmarked vanilla mob spawner iron golems do not roll variants.",
        "Unmarked vanilla spawn egg iron golems do not roll variants.",
    ],
    "docs/playtest.html": [
        "Unmarked vanilla mob spawner iron golems do not roll variants.",
        "Unmarked vanilla spawn egg iron golems do not roll variants.",
    ],
}

REQUIRED_CONTACT_EXACT = {
    "issues": "https://github.com/TnTBass/multigolem/issues",
    "sources": "https://github.com/TnTBass/multigolem",
}

REQUIRED_HOMEPAGE_PREFIX = "https://modrinth.com/mod/"

REQUIRED_LANG_KEYS = {
    "modmenu.nameTranslation.multigolem",
    "modmenu.summaryTranslation.multigolem",
    "modmenu.descriptionTranslation.multigolem",
}


def check(root: Path = ROOT) -> list[str]:
    root = root.resolve()
    errors: list[str] = []

    for relative in [
        "README.md",
        "docs/modrinth-listing.md",
        "docs/curseforge-listing.md",
        "docs/playtest-checklist.md",
        "docs/playtest.html",
    ]:
        path = root / relative
        if not path.exists():
            errors.append(f"{relative} does not exist")
            continue
        text = path.read_text(encoding="utf-8")
        for phrase in STALE_PHRASES:
            if phrase in text:
                errors.append(f"{relative} still contains stale phrase: {phrase}")

    for relative, markers in REQUIRED_DOC_MARKERS.items():
        path = root / relative
        if not path.exists():
            continue
        text = path.read_text(encoding="utf-8")
        missing = [marker for marker in markers if marker not in text]
        if missing:
            errors.append(f"{relative} is missing release-doc markers: {', '.join(missing)}")

    mod_json_path = root / "src/fabric/resources/fabric.mod.json"
    if not mod_json_path.exists():
        errors.append("src/fabric/resources/fabric.mod.json does not exist")
    else:
        mod_json = json.loads(mod_json_path.read_text(encoding="utf-8"))
        icon = mod_json.get("icon")
        if icon != "assets/multigolem/icon.png":
            errors.append("fabric.mod.json must set icon to assets/multigolem/icon.png")

        contact = mod_json.get("contact", {})
        homepage = contact.get("homepage", "")
        if not homepage.startswith(REQUIRED_HOMEPAGE_PREFIX) or homepage == REQUIRED_HOMEPAGE_PREFIX:
            errors.append(f"fabric.mod.json contact.homepage must be a Modrinth mod URL under {REQUIRED_HOMEPAGE_PREFIX}")
        for key, expected in REQUIRED_CONTACT_EXACT.items():
            if contact.get(key) != expected:
                errors.append(f"fabric.mod.json contact.{key} must be {expected}")

    icon_path = root / "src/common/resources/assets/multigolem/icon.png"
    if not icon_path.exists():
        errors.append("src/common/resources/assets/multigolem/icon.png does not exist")
    elif icon_path.stat().st_size > 256 * 1024:
        errors.append("src/common/resources/assets/multigolem/icon.png must stay under 256 KiB")

    lang_path = root / "src/common/resources/assets/multigolem/lang/en_us.json"
    if not lang_path.exists():
        errors.append("src/common/resources/assets/multigolem/lang/en_us.json does not exist")
    else:
        lang = json.loads(lang_path.read_text(encoding="utf-8"))
        missing_lang = sorted(REQUIRED_LANG_KEYS - set(lang.keys()))
        if missing_lang:
            errors.append(f"assets/multigolem/lang/en_us.json is missing: {', '.join(missing_lang)}")

    return errors


def main() -> None:
    errors = check(ROOT)
    if errors:
        raise SystemExit("Release docs check failed: " + "; ".join(errors))
    print("Release docs check passed.")


if __name__ == "__main__":
    main()
