#!/usr/bin/env python3
"""Require release notes to be written for players and server admins."""
from __future__ import annotations

import argparse
import os
import re
import sys
from dataclasses import dataclass
from pathlib import Path


DEFAULT_CHANGELOG = Path(__file__).resolve().parent.parent / "CHANGELOG.md"

DEVELOPMENT_LOG_PATTERNS = [
    (re.compile(r"\bTasks?\s+\d+(?:[–-]\d+)?:", re.IGNORECASE), "task-numbered implementation log"),
    (re.compile(r"\b[A-Za-z0-9_]+Mixin\b"), "mixin implementation detail"),
    (re.compile(r"\bSTREAM_CODEC\b"), "network implementation detail"),
    (re.compile(r"\bALLOW_DAMAGE\b"), "event-hook implementation detail"),
    (re.compile(r"\bTAIL inject\b", re.IGNORECASE), "mixin injection detail"),
    (re.compile(r"\bentrypoint\b", re.IGNORECASE), "loader implementation detail"),
    (re.compile(r"\bdrafted at\b", re.IGNORECASE), "document workflow note"),
    (re.compile(r"\bCodex-reviewed\b", re.IGNORECASE), "agent workflow note"),
    (re.compile(r"\bimplementation plan\b", re.IGNORECASE), "planning workflow note"),
    (re.compile(r"\bdocs/superpowers/"), "internal planning path"),
]


@dataclass
class CheckResult:
    ok: bool
    message: str


def target_section_from_env() -> str:
    ref_name = os.environ.get("GITHUB_REF_NAME", "")
    if ref_name.startswith("v"):
        return ref_name[1:]
    return "Unreleased"


def section_name_from_header(line: str) -> str | None:
    match = re.match(r"^##\s+(.+?)\s*$", line)
    if not match:
        return None

    name = match.group(1).strip()
    return re.split(r"\s+[—-]\s+", name, maxsplit=1)[0].strip()


def read_section(changelog: Path, section: str) -> list[tuple[int, str]] | None:
    lines = changelog.read_text(encoding="utf-8").splitlines()
    in_section = False
    section_lines: list[tuple[int, str]] = []

    for index, line in enumerate(lines, start=1):
        header = section_name_from_header(line)
        if header is not None:
            if in_section:
                break
            if header == section:
                in_section = True
            continue

        if in_section:
            section_lines.append((index, line))

    return section_lines if in_section else None


def check_changelog(changelog: Path, section: str) -> CheckResult:
    section_lines = read_section(changelog, section)
    if section_lines is None:
        return CheckResult(False, f"Could not find changelog section '{section}' in {changelog}.")

    findings: list[str] = []
    for line_number, line in section_lines:
        for pattern, reason in DEVELOPMENT_LOG_PATTERNS:
            match = pattern.search(line)
            if match:
                findings.append(f"line {line_number}: {reason}: {match.group(0)}")

    if not findings:
        return CheckResult(True, f"Changelog section '{section}' is written in release-note style.")

    details = "\n".join(f"- {finding}" for finding in findings)
    return CheckResult(
        False,
        "CHANGELOG.md release notes must be written for players/server admins, not as a development log.\n"
        "Rewrite the section to describe visible gameplay changes, admin/config impact, compatibility, and fixes.\n\n"
        f"Flagged development-log markers in section '{section}':\n{details}",
    )


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--changelog", type=Path, default=DEFAULT_CHANGELOG)
    parser.add_argument("--section", default=target_section_from_env())
    args = parser.parse_args(argv)

    result = check_changelog(args.changelog, args.section)
    print(result.message, file=sys.stderr if not result.ok else sys.stdout)
    return 0 if result.ok else 1


if __name__ == "__main__":
    sys.exit(main())
