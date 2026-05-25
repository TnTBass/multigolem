from __future__ import annotations

from pathlib import Path
from typing import NamedTuple


ROOT = Path(__file__).resolve().parents[1]


REQUIRED_HANDOFF_MARKERS = [
    "## V4 Revue Design-Intent Review Follow-Ups",
    "Creative tab source-set caution",
    "Exact `custom_data` visual matching guardrail",
    "Permission-denied interaction behavior",
    "Egg-spawned golem ownership",
    "Spawner thread-local cleanup and save ordering",
]

REQUIRED_PLAN_MARKERS = [
    "dedicated",
    "vanilla client",
    "sole writer",
    "custom_data",
    "arm-swing",
    "setPlayerCreated(true)",
    "finally",
    "same server thread",
]

REQUIRED_REVIEW_FIX_MARKERS = [
    "multigolem$getNextSpawnData",
    "context.getPlayer() == null",
    "ComponentContents",
    "one entity per tick",
    "Vanilla client shows",
]

REQUIRED_LESSONS_MARKERS = [
    '"scope_basis": "explicit-files"',
    "Do not parallelize dependent file moves",
    "absolute paths for new files in assigned worktrees",
]


class CheckResult(NamedTuple):
    errors: list[str]
    plan_candidates: list[Path]


def fail(message: str) -> None:
    raise SystemExit(f"V4 planning handoff check failed: {message}")


def collect(root: Path = ROOT) -> CheckResult:
    root = root.resolve()
    errors: list[str] = []
    mojang_targets = root / "docs" / "26.1.2-mojang-targets.md"
    gitignore_path = root / ".gitignore"
    lessons_path = root / "docs" / "LESSONS-LEARNED.md"
    plan_dir = root / "docs" / "superpowers" / "plans"
    plan_candidates = sorted(plan_dir.glob("*v4*spawn*egg*.md"))

    if not gitignore_path.exists():
        errors.append(".gitignore does not exist")
    else:
        gitignore = gitignore_path.read_text(encoding="utf-8")
        if ".agent-review/" not in gitignore.splitlines():
            errors.append(".agent-review/ must stay ignored so Revue local ledger state is not committed")

    if not plan_candidates:
        return CheckResult(errors=errors, plan_candidates=plan_candidates)

    if not mojang_targets.exists():
        errors.append(f"{mojang_targets.relative_to(root)} does not exist")
        handoff = ""
    else:
        handoff = mojang_targets.read_text(encoding="utf-8")
    missing = [marker for marker in REQUIRED_HANDOFF_MARKERS if marker not in handoff]
    if missing:
        errors.append(f"{mojang_targets.relative_to(root)} is missing required V4 Revue markers: {', '.join(missing)}")

    if not lessons_path.exists():
        errors.append(f"{lessons_path.relative_to(root)} does not exist")
        lessons = ""
    else:
        lessons = lessons_path.read_text(encoding="utf-8")
    missing_lessons = [marker for marker in REQUIRED_LESSONS_MARKERS if marker not in lessons]
    if missing_lessons:
        errors.append(f"{lessons_path.relative_to(root)} is missing V4 process lessons: {', '.join(missing_lessons)}")

    parent_checkout = parent_checkout_for_worktree(root)
    if parent_checkout is not None:
        parent_plan_dir = parent_checkout / "docs" / "superpowers" / "plans"
        duplicates = [
            parent_plan_dir / plan.name
            for plan in plan_candidates
            if parent_has_conflicting_plan(plan, parent_plan_dir / plan.name)
        ]
        if duplicates:
            names = ", ".join(str(path.relative_to(parent_checkout)) for path in duplicates)
            errors.append(f"V4 spawn egg plan also exists in parent checkout: {names}")

    plan_text = "\n".join(path.read_text(encoding="utf-8") for path in plan_candidates)
    required_plan_markers = [*REQUIRED_PLAN_MARKERS, *REQUIRED_REVIEW_FIX_MARKERS]
    missing_plan = [marker for marker in required_plan_markers if marker not in plan_text]
    if missing_plan:
        names = ", ".join(path.name for path in plan_candidates)
        errors.append(f"{names} missing required V4 planning guardrails: {', '.join(missing_plan)}")

    return CheckResult(errors=errors, plan_candidates=plan_candidates)


def check(root: Path = ROOT) -> list[str]:
    return collect(root).errors


def parent_checkout_for_worktree(root: Path) -> Path | None:
    parts = list(root.resolve().parts)
    if ".worktrees" not in parts:
        return None
    index = parts.index(".worktrees")
    if index == 0:
        return None
    return Path(*parts[:index])


def parent_has_conflicting_plan(worktree_plan: Path, parent_plan: Path) -> bool:
    if not parent_plan.exists():
        return False
    return parent_plan.read_text(encoding="utf-8") != worktree_plan.read_text(encoding="utf-8")


def main() -> None:
    result = collect(ROOT)
    errors = result.errors
    if errors:
        fail("; ".join(errors))

    if not result.plan_candidates:
        print("V4 handoff check passed; no V4 spawn egg plan exists yet, so plan marker checks were skipped.")
    else:
        print("V4 handoff check passed.")


if __name__ == "__main__":
    main()
