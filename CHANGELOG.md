# Changelog

## Unreleased

- Fix Modrinth project description sync (wrap content in 4-backtick fenced block to match the script's regex)
- Fix CurseForge upload slug in release workflow (`modern-multigolem` → `multigolem`)
- V2 design doc drafted at `docs/superpowers/specs/2026-05-16-multigolem-v2-design.md` (Codex-reviewed; awaiting human review and implementation plan)

## 0.1.0+mc26.1.2 — 2026-05-15

- Initial V1: 6 golem variants (Copper, Iron, Gold, Emerald, Diamond, Netherite) as attachments on vanilla iron golems
- T-pattern creation with each tier's material block
- Per-tier stat scaling (HP and attack damage)
- Healing with matching ingot (configurable via `allow_golem_healing`)
- Per-tier `anger_on_hit` toggle
- Material-specific drops on death
- Server-side functional; vanilla clients work without the mod
- Build/release CI (GitHub Actions) and Modrinth/CurseForge upload scripts
- README and playtest checklist for the V1 release
