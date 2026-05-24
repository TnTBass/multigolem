from __future__ import annotations

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MOJANG_TARGETS = ROOT / "docs" / "26.1.2-mojang-targets.md"
GITIGNORE = ROOT / ".gitignore"
PLAN_DIR = ROOT / "docs" / "superpowers" / "plans"


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


def fail(message: str) -> None:
    raise SystemExit(f"V4 planning handoff check failed: {message}")


def main() -> None:
    gitignore = GITIGNORE.read_text(encoding="utf-8")
    if ".agent-review/" not in gitignore.splitlines():
        fail(".agent-review/ must stay ignored so Revue local ledger state is not committed")

    handoff = MOJANG_TARGETS.read_text(encoding="utf-8")
    missing = [marker for marker in REQUIRED_HANDOFF_MARKERS if marker not in handoff]
    if missing:
        fail(f"{MOJANG_TARGETS.relative_to(ROOT)} is missing required V4 Revue markers: {', '.join(missing)}")

    plan_candidates = sorted(PLAN_DIR.glob("*v4*spawn*egg*.md"))
    if not plan_candidates:
        print("V4 handoff check passed; no V4 spawn egg plan exists yet, so plan marker checks were skipped.")
        return

    plan_text = "\n".join(path.read_text(encoding="utf-8") for path in plan_candidates)
    missing_plan = [marker for marker in REQUIRED_PLAN_MARKERS if marker not in plan_text]
    if missing_plan:
        names = ", ".join(path.name for path in plan_candidates)
        fail(f"{names} missing required V4 planning guardrails: {', '.join(missing_plan)}")

    print("V4 handoff check passed.")


if __name__ == "__main__":
    main()
