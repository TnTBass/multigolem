# MultiGolem Lapis Golem - Design

**Date:** 2026-06-26
**Authors:** Tyler & Charles
**Status:** Approved design, implementation deferred to a new session
**Predecessors:**

- Current roadmap: `README.md`
- Prior combined Redstone/Lapis sketch: `docs/superpowers/specs/2026-05-25-multigolem-redstone-lapis-design.md`
- Current 26.2 target notes: `docs/26.2-mojang-targets.md`
- Redstone implementation plan: `docs/superpowers/plans/2026-06-24-multigolem-redstone-golem.md`

## 1. Summary

V8 adds the Lapis Golem as a fragile anti-magic support defender. The current `README.md` roadmap is the source of truth for this design; the older combined Redstone/Lapis spec is background only.

Lapis is not a brawler and does not get charm, Resistance, or another offensive ability. Its value is a 15-block ward that protects allied village defenders and civilians from magic-tagged damage and configured magic-style harmful effects.

The Lapis Golem remains a vanilla `IronGolem` entity with MultiGolem variant data, config-backed defaults, spawn egg and spawner support, village weighting, matching-material healing, matching drops, and client texture selection.

## 2. Goals And Non-Goals

### Goals

- Add Lapis as a first-class `GolemVariant`.
- Preserve the existing no-new-entity architecture.
- Make Lapis a weak combat unit whose value comes from anti-magic support.
- Give Lapis half of vanilla Iron Golem health and attack by default.
- Protect nearby allied entities from magic-tagged damage and configured magic-style harmful effects.
- Include Lapis in player-built creation, healing, drops, spawn eggs, spawners, permissions, village spawn weights, public docs, Golempedia/status/customization surfaces, and playtest checklists.
- Make ward behavior configurable, including player protection and the protected effect id list.

### Non-Goals

- No charm or mind-control mechanic.
- No extra offensive ability.
- No Resistance buff or general damage resistance aura.
- No protection from mundane damage such as melee, arrows, explosions, fire, lava, fall damage, drowning, or suffocation.
- No new blocks, items, entity types, custom geometry, or bespoke animation system.
- No broad config refactor beyond fields needed for this variant.
- No release publishing in the design phase.

## 3. Variant Identity

| Field | Default |
|---|---|
| Variant id | `lapis` |
| Display name | `Lapis` |
| Body block | `minecraft:lapis_block` |
| Healing item | `minecraft:lapis_lazuli` |
| Primary drop | `minecraft:lapis_lazuli` |
| Max health | `50` |
| Attack damage | `7.5` |
| Role | Fragile anti-magic support |

Lapis defaults to half the vanilla Iron Golem's health and half its attack damage. It can still defend itself with normal golem melee, but it is intentionally weak enough that other defenders should protect it.

Lapis should ignore creepers by default through the existing `ignored_target_types` config pattern.

## 4. Ability: Lapis Ward

### 4.1 Behavior

The Lapis ward is active when the Lapis Golem is:

- Alive.
- Awake/active according to normal Minecraft entity state.
- Enabled by config.

The ward is not combat-gated and does not require a current target.

While active, the ward protects eligible entities within 15 blocks. Protection should feel continuous to players: if an eligible entity is inside the ward, hostile magic does not stick. Internally, implementation may use periodic refresh/cleanup plus damage/effect hooks.

### 4.2 Protected Entities

Protected by default:

- The Lapis Golem itself.
- Villagers.
- Wandering traders.
- Iron Golems.
- Friendly, non-zombie MultiGolems.

Players are not protected by default. Server owners may enable player protection with config.

Zombie Golems and hostile entities are not protected by the ward.

### 4.3 Magic Damage Protection

The ward blocks damage sources that Minecraft identifies or tags as magic.

The implementation should use the current 26.2 damage-source/tag path instead of maintaining a hand-expanded supernatural list. Adjacent supernatural damage, such as dragon breath or wither-skull damage, is protected only if it reaches the same magic damage-source/tag classification.

Mundane damage remains effective, including melee, projectiles, explosions, fire, lava, fall damage, drowning, suffocation, and similar non-magic sources.

### 4.4 Magic-Style Effect Protection

The ward removes or prevents a configurable list of magic-style harmful effects. Defaults:

- `minecraft:poison`
- `minecraft:wither`
- `minecraft:weakness`
- `minecraft:slowness`
- `minecraft:blindness`
- `minecraft:nausea`
- `minecraft:levitation`
- `minecraft:darkness`
- `minecraft:mining_fatigue`

This list is deliberately thematic rather than "all harmful effects." Server owners can add or remove effect ids through config.

Unknown or invalid effect ids should not crash runtime behavior. Preserve unknown values through config write-back if the existing config system supports that cleanly; ignore invalid ids at runtime.

### 4.5 Visual Readability

Lapis should use blue/enchantment-style particles to make ward protection readable without creating noise every tick. Particle behavior is configurable.

Minimum visual cues:

- A subtle Lapis ward shimmer or pulse around the Lapis Golem.
- A small protection cue when an eligible entity has magic damage blocked or a configured effect removed.

No custom animation system is required.

## 5. Configuration

Lapis fields should follow the existing tier/config pattern and participate in config migration/write-back for older config files.

Suggested defaults:

| Field | Default | Meaning |
|---|---:|---|
| `lapis_ward_enabled` | `true` | Enables the Lapis ward. |
| `lapis_ward_range` | `15` | Block radius for protected allies. |
| `lapis_ward_affects_players` | `false` | Allows players to receive ward protection. |
| `lapis_ward_magic_damage_enabled` | `true` | Blocks magic-tagged damage for protected entities. |
| `lapis_ward_effect_cleanup_enabled` | `true` | Removes/prevents configured magic-style effects. |
| `lapis_ward_effect_ids` | See section 4.4 | Effect ids removed/prevented by the ward. |
| `lapis_particles_enabled` | `true` | Enables ward particles. |

Suggested validation:

- `lapis_ward_range`: clamp to `1..64`.
- `lapis_ward_effect_ids`: preserve configured strings where possible, resolve valid ids at runtime, and ignore invalid ids without crashing.
- Booleans follow existing malformed-field fallback behavior.

## 6. Village Spawning

Lapis is included in village natural spawns by default, but rare.

Recommended village spawn weight:

| Variant | Weight |
|---|---:|
| Lapis | `5` |

Rationale:

- Lapis is useful against witches and magic/status threats.
- Its weak combat stats prevent it from replacing sturdier defenders.
- Rare natural spawns create memorable support moments without making every village anti-magic by default.

The player-facing roll order should place Lapis between Gold and Emerald:

`iron`, `copper`, `redstone`, `gold`, `lapis`, `emerald`, `diamond`, `netherite`

Existing configs must migrate by filling missing Lapis village weights and tier fields from defaults while preserving unknown fields.

## 7. Permissions, Spawns, And Drops

Add matching create/heal permission nodes:

- `multigolem.create.lapis`
- `multigolem.heal.lapis`

Permissions remain permissive by default, consistent with the existing permissions behavior.

Spawn egg behavior follows existing marked vanilla Iron Golem spawn egg rules:

- Marked Lapis spawn eggs spawn Lapis Golems.
- Marked Lapis spawn eggs can configure spawners for Lapis Golems.
- Permission checks match existing variant spawn egg and spawner behavior.

Drops follow the existing material-drop pattern unless implementation evidence shows a specific economy issue.

## 8. Architecture Notes

Implementation should follow current Redstone-era patterns:

- `GolemVariant` owns id, display name, body block, healing item, and drop item.
- The variant catalog and config defaults expose Lapis consistently across runtime, docs-facing summaries, Golempedia, status, and customizations.
- `TierStats` carries nullable ability-specific config fields using the existing pattern.
- `GolemStatsResolver` exposes typed accessors for ward behavior.
- `VariantAttributes` applies Lapis health and attack defaults.
- Ward logic belongs in a focused common ability class, likely `LapisAbility`.
- Loader adapters should only wire loader-specific tick, damage, and effect hooks where needed.

Damage blocking should be based on Minecraft's current 26.2 magic damage-source/tag path. The implementation should confirm the exact 26.2 API surface before editing mixins or loader hooks.

Effect cleanup should resolve configured effect ids at runtime and ignore invalid ids safely.

## 9. Data Flow

### 9.1 Ward Tick

1. Server tick reaches a loaded Lapis Golem.
2. Ability checks that the golem is alive, awake/active, and ward-enabled.
3. Ability finds eligible allies within `lapis_ward_range`.
4. Ability removes configured magic-style effects from protected entities when effect cleanup is enabled.
5. Ability emits configured particle cues at a restrained interval.

### 9.2 Magic Damage

1. A living entity receives damage.
2. Damage hook checks whether the source is magic-tagged/identified.
3. If not magic, damage proceeds normally.
4. If magic, ability checks whether the target is protected by an active Lapis ward.
5. If protected, the magic damage is blocked.
6. If not protected, damage proceeds normally.

### 9.3 Effect Application

1. A living entity receives a mob effect.
2. Effect hook checks whether the effect id is in the configured Lapis ward effect list.
3. If not listed, the effect proceeds normally.
4. If listed, ability checks whether the target is protected by an active Lapis ward.
5. If protected and cleanup is enabled, the effect is prevented or removed.
6. If not protected, the effect proceeds normally.

## 10. Testing And Playtest Coverage

### 10.1 Unit And Contract Tests

Add or update focused tests for:

- `GolemVariant` id/body/healing/drop mappings.
- Lapis default stats: `50` health and `7.5` attack.
- Lapis default ignored target types include creepers.
- Lapis village spawn weight is present and rare.
- Config defaults include Lapis tier fields and ward fields.
- Config migration/write-back fills missing Lapis tier and ward fields into older configs.
- Config validation clamps ward range and handles invalid effect ids without crashing.
- Ward entity eligibility: villagers, wandering traders, Iron Golems, friendly MultiGolems, Lapis itself, players off by default, players on when configured.
- Magic damage blocked for protected entities in range.
- Magic damage not blocked outside range or when Lapis is dead, inactive, or disabled.
- Mundane damage is not blocked.
- Configured magic-style harmful effects are removed or prevented in range.
- Unconfigured effects are not removed.
- Fabric and NeoForge hook registration for tick, damage, and effect paths.

### 10.2 Manual Playtest Rows

Add rows to `docs/playtest-checklist.md` and `docs/playtest.html` for:

- Build a Lapis Golem with lapis blocks and a carved pumpkin.
- Heal a damaged Lapis Golem with lapis lazuli.
- Verify Lapis has low combat stats compared with Iron.
- Verify Lapis ignores creepers by default.
- Verify a village-spawned Lapis Golem can appear under forced weight.
- Verify protected villagers/golems within 15 blocks avoid configured magic-style effects.
- Verify protected villagers/golems within 15 blocks avoid magic-tagged damage.
- Verify players are not protected by default.
- Enable player protection and verify players are protected inside the ward.
- Verify melee, projectile, explosion, fire/lava, and fall damage are not blocked.
- Verify protection stops when the Lapis Golem dies, unloads, becomes inactive, or ward config is disabled.

## 11. Documentation And Release Surfaces

Implementation must update:

- `README.md`
- `docs/modrinth-listing.md`
- `docs/curseforge-listing.md`
- `docs/playtest-checklist.md`
- `docs/playtest.html`
- `CHANGELOG.md`
- `INTERNAL_CHANGELOG.md` if used for implementation notes in the active release flow.
- Any release metadata or checks that enumerate variant names.
- Golempedia, status, and customization surfaces.

Public wording should emphasize:

- Lapis is a fragile anti-magic support golem.
- Lapis has half-Iron health and attack by default.
- Lapis protects nearby allies from magic-tagged damage and configured magic-style negative effects.
- Players are opt-in for protection.
- Lapis does not block ordinary combat damage.

## 12. Risks And Mitigations

| Risk | Mitigation |
|---|---|
| Magic damage classification differs between loaders or 26.2 APIs. | Confirm the exact 26.2 damage-source/tag API before implementation and cover Fabric/NeoForge hooks with source tests. |
| Ward becomes too broad and invalidates ordinary threats. | Protect only magic-tagged damage and configured effects; keep mundane damage untouched. |
| Lapis replaces combat defenders. | Keep half-Iron health and attack, rare village spawns, and no offensive ability. |
| Player ward becomes overpowered. | Keep player protection off by default and configurable. |
| Invalid config effect ids crash runtime. | Resolve effect ids defensively and ignore invalid ids at runtime. |
| Particle effects become noisy. | Use restrained blue/enchantment cues and make particles configurable. |

## 13. Implementation Handoff Prompt

Use this prompt to start the implementation session:

```text
We are implementing the approved MultiGolem V8 Lapis Golem design in:
C:\Users\tyler\AI Projects\MultiGolem

Start by reading:
- AGENTS.md instructions from the thread: verify `git rev-parse --show-toplevel` before edits and keep all edits in the assigned repo/worktree.
- docs/superpowers/specs/2026-06-26-multigolem-lapis-golem-design.md
- docs/26.2-mojang-targets.md
- docs/LESSONS-LEARNED.md
- Current variant/config/spawn/ability files under src/common/java/dev/charles/multigolem plus Fabric and NeoForge adapter paths.

Scope:
- Implement Lapis Golem exactly as specified.
- Keep the no-new-entity architecture.
- Use the current README roadmap as the source of truth; the older combined Redstone/Lapis spec is background only.
- Lapis defaults to 50 HP and 7.5 attack.
- Lapis ward range defaults to 15 blocks.
- Players are not protected by default; make player protection configurable.
- Protect only magic-tagged/identified damage and configured magic-style harmful effect ids.
- Do not add charm, Resistance, or an offensive ability.
- Do not publish, tag, deploy, or create a PR unless Tyler explicitly asks.

Workflow:
- Use Superpowers TDD and implementation planning.
- Verify repo root before edits.
- Keep changes scoped to this feature.
- Run focused tests first, then full build/release checks appropriate for MultiGolem.
- If using Revue review, action findings explicitly and separate implementation readiness from release readiness.
```
