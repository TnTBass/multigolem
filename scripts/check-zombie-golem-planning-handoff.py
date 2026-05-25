from __future__ import annotations

from pathlib import Path
from typing import NamedTuple


ROOT = Path(__file__).resolve().parents[1]


SPEC_PATH = Path("docs/superpowers/specs/2026-05-25-multigolem-zombie-golem-design.md")
LESSONS_PATH = Path("docs/LESSONS-LEARNED.md")
PLAN_GLOB = "*zombie*golem*.md"


REQUIRED_SPEC_MARKERS = [
    "Mossy Cobblestone",
    "Rotten Flesh",
    "multigolem.create.zombie",
    "multigolem.heal.zombie",
    "zombie_village_spawning",
    "max_zombie_golems_per_village",
    "regular zombies alone",
    "GolemVariantAttachment",
    "Hunger",
    "Nausea",
    "Poison",
    "wandering traders",
    "spawn egg",
]

REQUIRED_LESSONS_MARKERS = [
    "desired-count maintenance rule",
    "Zombie villagers are the eligibility anchor",
    "regular zombies alone must never qualify",
    "strict generated-structure filtering is not assumed",
    "IronGolem` + `GolemVariantAttachment",
    "Civilian conversion is the signature mechanic",
    "python scripts/check-zombie-golem-planning-handoff.py",
]

REQUIRED_PLAN_MARKERS = [
    "Mossy Cobblestone",
    "Rotten Flesh",
    "multigolem.create.zombie",
    "multigolem.heal.zombie",
    "zombie_village_spawning",
    "max_zombie_golems_per_village",
    "desired-count",
    "regular zombies alone",
    "zombie villagers",
    "players",
    "villagers",
    "wandering traders",
    "Iron Golems",
    "non-zombie MultiGolems",
    "Hunger",
    "Nausea",
    "Poison",
    "GolemVariantAttachment",
    "marked",
    "spawn egg",
    "spawner",
]

REQUIRED_PLAN_SPIKE_MARKERS = [
    "village-area maintenance",
    "targeting",
    "faction",
    "conversion",
    "Rotten Flesh healing",
    "spawn egg",
    "spawner",
]


class CheckResult(NamedTuple):
    errors: list[str]
    plan_candidates: list[Path]


def fail(message: str) -> None:
    raise SystemExit(f"Zombie Golem planning handoff check failed: {message}")


def collect(root: Path = ROOT) -> CheckResult:
    root = root.resolve()
    errors: list[str] = []
    spec_path = root / SPEC_PATH
    lessons_path = root / LESSONS_PATH
    plan_dir = root / "docs" / "superpowers" / "plans"
    plan_candidates = sorted(plan_dir.glob(PLAN_GLOB)) if plan_dir.exists() else []

    spec = read_or_error(root, spec_path, errors)
    missing_spec = [marker for marker in REQUIRED_SPEC_MARKERS if marker not in spec]
    if missing_spec:
        errors.append(f"{SPEC_PATH} is missing required Zombie Golem design markers: {', '.join(missing_spec)}")

    lessons = read_or_error(root, lessons_path, errors)
    missing_lessons = [marker for marker in REQUIRED_LESSONS_MARKERS if marker not in lessons]
    if missing_lessons:
        errors.append(f"{LESSONS_PATH} is missing Zombie Golem planning lessons: {', '.join(missing_lessons)}")

    if plan_candidates:
        plan_text = "\n".join(path.read_text(encoding="utf-8") for path in plan_candidates)
        missing_plan = [marker for marker in REQUIRED_PLAN_MARKERS if marker not in plan_text]
        if missing_plan:
            names = ", ".join(path.name for path in plan_candidates)
            errors.append(f"{names} missing required Zombie Golem planning guardrails: {', '.join(missing_plan)}")

        missing_spikes = [marker for marker in REQUIRED_PLAN_SPIKE_MARKERS if marker not in plan_text]
        if missing_spikes:
            names = ", ".join(path.name for path in plan_candidates)
            errors.append(f"{names} missing required Zombie Golem source-spike guardrails: {', '.join(missing_spikes)}")

    return CheckResult(errors=errors, plan_candidates=plan_candidates)


def read_or_error(root: Path, path: Path, errors: list[str]) -> str:
    if not path.exists():
        errors.append(f"{path.relative_to(root)} does not exist")
        return ""
    return path.read_text(encoding="utf-8")


def check(root: Path = ROOT) -> list[str]:
    return collect(root).errors


def main() -> None:
    result = collect(ROOT)
    if result.errors:
        fail("; ".join(result.errors))

    if not result.plan_candidates:
        print("Zombie Golem handoff check passed; no Zombie Golem plan exists yet, so plan marker checks were skipped.")
    else:
        print("Zombie Golem handoff check passed.")


if __name__ == "__main__":
    main()
