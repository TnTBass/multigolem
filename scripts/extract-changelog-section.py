#!/usr/bin/env python3
"""Extract a versioned CHANGELOG.md section for release publishing."""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


DEFAULT_CHANGELOG = Path(__file__).resolve().parent.parent / "CHANGELOG.md"


def section_name_from_header(line: str) -> str | None:
    match = re.match(r"^##\s+(.+?)\s*$", line)
    if not match:
        return None

    name = match.group(1).strip()
    return re.split(r"\s+[—-]\s+", name, maxsplit=1)[0].strip()


def extract_section(changelog: Path, section: str, loader: str | None = None) -> str:
    lines = changelog.read_text(encoding="utf-8").splitlines()
    in_section = False
    captured: list[str] = []

    for line in lines:
        header = section_name_from_header(line)
        if header is not None:
            if in_section:
                break
            if header == section:
                in_section = True
            continue

        if in_section:
            captured.append(line)

    while captured and not captured[0].strip():
        captured.pop(0)
    while captured and not captured[-1].strip():
        captured.pop()

    notes = "\n".join(captured)
    if loader is not None:
        loader_name = {"fabric": "Fabric", "neoforge": "NeoForge"}[loader]
        # Tailor only the standard compatibility bullet; other release copy stays verbatim.
        notes = re.sub(
            r"(?m)^(- Updated compatibility to Minecraft [0-9]+(?:\.[0-9]+)* for )Fabric and NeoForge\.$",
            lambda match: f"{match.group(1)}{loader_name}.",
            notes,
        )
    return notes


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--changelog", type=Path, default=DEFAULT_CHANGELOG)
    parser.add_argument("--section", required=True)
    parser.add_argument("--loader", choices=("fabric", "neoforge"),
                        help="Tailor compatibility notes for a marketplace loader upload.")
    args = parser.parse_args(argv)

    extracted = extract_section(args.changelog, args.section, args.loader)
    if not extracted.strip():
        print(f"Could not find non-empty changelog section '{args.section}' in {args.changelog}.", file=sys.stderr)
        return 1

    print(extracted)
    return 0


if __name__ == "__main__":
    sys.exit(main())
