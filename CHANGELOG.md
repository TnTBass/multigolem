# Changelog

## Unreleased

- Fix Modrinth project description sync (wrap content in 4-backtick fenced block to match the script's regex)
- Fix CurseForge upload slug in release workflow (`modern-multigolem` → `multigolem`)
- V2 design doc drafted at `docs/superpowers/specs/2026-05-16-multigolem-v2-design.md` (Codex-reviewed; awaiting human review and implementation plan)
- V2 art direction reference image added at `docs/v2-texture-art-direction.png` (canonical visual target for the texture generator script)
- V2 spec revised per Codex round-2 review: lava-walking made hard non-goal; lossless `JsonObject` merge migration + atomic write; renderer fails fast (no silent fallback); diamond on-attack preserves cooldown on failed targets; netherite ignite skips netherite-variant targets; expanded validation rules; case-insensitive `diamond_target_mode`; spike 6 targeting candidates enumerated; implementation order reworked to fail-fast on targeting mixin
- V2 spec revised per Codex round-3 review: diamond dying-target made strict (no fire, no cooldown); diamond bystander/direct-hit playtests rewritten correctly; config migration order clarified (merge → validate → compare → write) with canonicalized rewrite; atomic-write guarantee tightened (leave-untouched fallback rather than non-atomic copy); spike 2 expanded to cover ALLOW_DAMAGE semantics; spike 6 hook preference reordered (earlier-acquisition hooks first); spike 8 S2C packet fallback details specified; §5.3 wording generalized
- V2 implementation plan drafted at `docs/superpowers/plans/2026-05-16-multigolem-v2.md` (23 tasks, spike-first, GolemTargetingMixin as fail-fast checkpoint at Task 7)
- V2 Tasks 3–7: lossless V1→V2 config migration; `GolemStatsResolver` V2 accessors; `GolemAbilityState` persistent attachment; `TargetFilter` predicates; `GolemTargetingMixin` enforcing `ignored_target_types` on iron-golem melee AI via `Mob#setTarget` veto

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
