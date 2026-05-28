# MultiGolem Redstone And Lapis Golems - Design

**Date:** 2026-05-25
**Authors:** Tyler & Charles
**Status:** Approved design, implementation deferred to a new session
**Predecessors:**

- V1 design: `docs/superpowers/specs/2026-05-15-multigolem-design.md`
- V2 design: `docs/superpowers/specs/2026-05-16-multigolem-v2-design.md`
- V3 design: `docs/superpowers/specs/2026-05-17-multigolem-v3-design.md`
- Mojang/Fabric target notes: `docs/26.1.2-mojang-targets.md`

## 1. Summary

This expansion adds two new MultiGolem variants:

- **Redstone Golem:** a mid-tier control defender built from redstone blocks.
- **Lapis Golem:** a mid-tier enchantment defender built from lapis blocks.

The current ladder remains anchored by Copper as the weakest combat tier and Netherite as the strongest endgame tier. Redstone fits between Iron and Gold. Lapis fits between Gold and Emerald.

The abilities are deliberately readable in play:

- Redstone controls the battlefield with slowing pulses and weakening hits.
- Lapis protects allies and charms hostile mobs into fighting other enemies.

Both variants remain vanilla `IronGolem` entities with MultiGolem attachments, server-side functional behavior, config-backed defaults, spawn egg support, village weighting support, matching-material healing, matching drops, and texture selection on modded clients.

## 2. Goals And Non-Goals

**Goals**

- Add Redstone and Lapis as first-class `GolemVariant` values.
- Preserve the existing no-new-entity architecture.
- Keep the stat ladder understandable and balanced around the existing tiers.
- Give Redstone and Lapis distinct roles that do not replace Gold, Emerald, Diamond, or Netherite.
- Make all new ability parameters configurable in `config/multigolem.json`.
- Include both variants in player-built creation, spawn eggs, permissions, drops, healing, village spawn weights, public docs, and playtest checklists.
- Keep Lapis charm readable and bounded so it feels magical without permanently rewriting mob behavior.
- Explicitly allow Lapis charm to affect creepers by default.

**Non-goals**

- No new blocks, items, or entity types.
- No real redstone power emission from Redstone golems.
- No permanent mob faction conversion.
- No player-controllable charmed mobs.
- No charm effect on bosses by default.
- No custom geometry or new animation system; continue using the texture-selection client path.
- No broad refactor of the config system beyond the fields needed for these variants.
- No implementation in this design session.

## 3. Tier Fit

### 3.1 Default Stats

| Tier | Max HP | Avg Atk Damage | Role |
|---|---:|---:|---|
| Copper | 60 | 8.5 | Weak utility |
| Iron | 100 | 15.0 | Vanilla baseline |
| Redstone | 115 | 19.0 | Tactical control |
| Gold | 130 | 22.5 | Fast striker |
| Lapis | 170 | 30.0 | Enchantment support |
| Emerald | 200 | 40.0 | Village sustain |
| Diamond | 350 | 62.5 | Rare offensive power |
| Netherite | 600 | 85.0 | Endgame brawler |

These defaults make Redstone slightly better than Iron in raw combat but worse than Gold. Lapis is stronger than Gold, but still below Emerald's durability and damage.

### 3.2 Creation, Healing, And Drops

| Variant | Body block | Healing item | Primary drop |
|---|---|---|---|
| Redstone | `minecraft:redstone_block` | `minecraft:redstone` | `minecraft:redstone` |
| Lapis | `minecraft:lapis_block` | `minecraft:lapis_lazuli` | `minecraft:lapis_lazuli` |

Both use the same T-pattern and carved pumpkin flow as existing MultiGolem variants.

The drop count should follow the existing material-drop pattern unless implementation discovers a specific economy reason to tune it lower. Redstone and Lapis are cheaper than Diamond and Netherite, so their value comes from utility rather than rare drops.

## 4. Redstone Golem

### 4.1 Identity

Redstone is the control golem. It does not win by being stronger than Gold. It wins by slowing groups and making individual enemies hit less hard.

Plain-language version: Redstone says "stay back."

### 4.2 Ability: Redstone Pulse

While a Redstone golem is in combat, it emits a pulse at a configured interval. Nearby hostile mobs receive `Slowness I` for a short duration.

Default behavior:

- Pulse only while the golem has or recently had a valid combat target.
- The recent-combat window defaults to 5 seconds after the last valid combat target.
- Affect hostile mobs within 6 blocks.
- Apply `Slowness I` for 3 seconds.
- Pulse every 5 seconds.
- Respect Redstone's `ignored_target_types` for which mobs the ability affects.
- Use red dust particles in a small ring so players can read the event.

The pulse must not power redstone components. The ability is combat control, not world automation.

### 4.3 Ability: Static Strike

On successful melee hit, a Redstone golem applies `Weakness I` to the target for 4 seconds.

Default behavior:

- Trigger only after vanilla confirms the hit succeeded.
- Do not trigger if the target is already dead or removed.
- Respect `ignored_target_types`.
- Use a short red spark visual on the target.

### 4.4 Redstone Config Defaults

| Field | Default | Meaning |
|---|---:|---|
| `redstone_pulse_enabled` | `true` | Enables the combat pulse. |
| `redstone_pulse_range` | `6` | Block radius for affected hostile mobs. |
| `redstone_pulse_interval_seconds` | `5.0` | Seconds between pulses. |
| `redstone_pulse_combat_linger_seconds` | `5.0` | Seconds after the last valid combat target during which pulses may continue. |
| `redstone_pulse_slowness_seconds` | `3.0` | Slowness duration applied by pulse. |
| `redstone_static_strike_enabled` | `true` | Enables on-hit weakness. |
| `redstone_static_strike_weakness_seconds` | `4.0` | Weakness duration applied on hit. |
| `redstone_particles_enabled` | `true` | Enables pulse and hit visuals. |

Suggested validation:

- Range: integer, clamp to `1..64`.
- Intervals and durations: finite doubles, minimum `0.5` for intervals, minimum `0.0` for durations.
- Booleans: follow the existing malformed-field fallback behavior.

## 5. Lapis Golem

### 5.1 Identity

Lapis is the enchantment golem. It protects allies and briefly turns enemies against each other.

Plain-language version: Lapis says "stand together" and "no, you work for us now."

### 5.2 Ability: Lapis Ward

At a configured interval, a Lapis golem grants nearby allied entities a short `Resistance I` effect.

Default affected allies:

- Villagers.
- Wandering traders.
- Iron golems and MultiGolem variants.

Players are not affected by default. This keeps Lapis focused on village defense rather than becoming a player buff station. Server owners can opt players in with config.

Default behavior:

- Affect allies within 8 blocks.
- Apply `Resistance I` for 4 seconds.
- Pulse every 6 seconds.
- Use blue enchantment particles around protected allies.
- Do not heal. Emerald keeps the healing identity.

### 5.3 Ability: Lapis Charm

On successful melee hit, a Lapis golem charms a hostile mob for a short duration.

While charmed, the mob:

- Cannot target players.
- Cannot target villagers or wandering traders.
- Cannot target iron golems or MultiGolem variants.
- Prefers nearby hostile mobs as targets.
- Becomes effectively passive if no valid hostile target is nearby.
- Shows blue enchantment particles and `Glowing` so players can identify it.

Charm is temporary. It does not permanently change the mob's type, team, brain, owner, NBT identity, or saved faction state.

### 5.4 Creeper Decision

Creepers are charmable by default.

This is intentional and approved. A charmed creeper may target and explode near enemy mobs. That creates a funny, risky, readable Lapis moment. It also means Lapis must not ignore creepers by default, unlike most current non-iron tiers.

Collateral damage is part of the trade-off. The design should document this in public copy and playtest rows rather than hiding it behind a surprising config edge.

A Lapis golem can be inside the blast radius when it charms a creeper at melee range. That self-damage risk is intentional unless implementation testing shows it makes the ability unusable. The implementation should not make Lapis explosion-proof by default; if playtesting shows the golem dies too reliably to its own charmed creepers, tune charm targeting or retreat behavior in a follow-up design change.

### 5.5 Boss And Edge-Case Rules

Bosses are immune to charm by default.

Charm should be best-effort for normal hostile mobs. Some mobs have special AI, ownership, projectile, explosion, teleport, or anger behavior. The implementation should exclude or no-op on problematic mobs rather than adding fragile special-case control for every Minecraft AI path.

Default exclusions:

- Boss mobs through the same boss classification style used by `ignored_target_types`.
- Non-living entities.
- Non-hostile mobs.
- Other golems.
- Players, villagers, and wandering traders.

Endermen and other complex mobs are allowed unless implementation evidence shows they cannot be safely supported. If a mob cannot be supported cleanly, the implementation should document the exclusion and add a playtest note.

Lapis charm also respects Lapis's `ignored_target_types` in addition to the explicit exclusions above. Lapis must not include `CREEPERS` in `ignored_target_types` by default because creeper charm is an approved feature.

### 5.6 Lapis Config Defaults

| Field | Default | Meaning |
|---|---:|---|
| `lapis_ward_enabled` | `true` | Enables the ally resistance aura. |
| `lapis_ward_range` | `8` | Block radius for warded allies. |
| `lapis_ward_interval_seconds` | `6.0` | Seconds between ward pulses. |
| `lapis_ward_resistance_seconds` | `4.0` | Resistance duration applied to allies. |
| `lapis_ward_affects_players` | `false` | Allows players to receive ward when enabled. |
| `lapis_charm_enabled` | `true` | Enables on-hit charm. |
| `lapis_charm_duration_seconds` | `8.0` | Charm duration. |
| `lapis_charm_cooldown_seconds` | `6.0` | Minimum time between charm applications per Lapis golem. |
| `lapis_charm_range` | `10` | Search radius for enemy targets a charmed mob can be redirected toward. |
| `lapis_charm_affects_creepers` | `true` | Allows creepers to be charmed. |
| `lapis_charm_affects_bosses` | `false` | Allows bosses to be charmed when explicitly enabled. |
| `lapis_charm_max_active_targets` | `1` | Maximum simultaneous charmed mobs per Lapis golem. |
| `lapis_charm_glowing_enabled` | `true` | Applies Glowing while charmed. |
| `lapis_particles_enabled` | `true` | Enables ward and charm visuals. |

If `lapis_charm_max_active_targets` is already reached, a later Lapis hit does not refresh the existing charm, does not charm a new target, and does not consume the cooldown. This keeps active-target limits predictable and testable.

Suggested validation:

- Ranges: integer, clamp to `1..64`.
- Intervals and durations: finite doubles, minimum `0.5` for intervals and cooldowns, minimum `0.0` for effect durations.
- `lapis_charm_max_active_targets`: integer, clamp to `1..16`.
- Booleans: follow the existing malformed-field fallback behavior.

## 6. Village Spawning

Redstone and Lapis should be included in village natural-spawn weighting.

Recommended default weights:

| Variant | Weight |
|---|---:|
| Iron | 19 |
| Copper | 19 |
| Redstone | 19 |
| Gold | 19 |
| Lapis | 12 |
| Emerald | 19 |
| Diamond | 5 |
| Netherite | 0 |

Rationale:

- Iron, Copper, Redstone, Gold, and Emerald are common village defenders.
- Lapis is uncommon because charm is more disruptive and more memorable.
- Diamond remains rare.
- Netherite remains opt-in only because fire-starting village defenders are dangerous.

The roll order should be updated deliberately. A natural order for player-facing config and docs is:

`iron`, `copper`, `redstone`, `gold`, `lapis`, `emerald`, `diamond`, `netherite`

Existing configs must migrate by filling missing `redstone` and `lapis` weights with defaults while preserving unknown fields.

Existing tier configs must also migrate by filling missing Redstone and Lapis tier objects and all missing Redstone/Lapis ability fields from defaults. Unknown user fields must remain preserved through the existing raw `JsonObject` merge/canonicalize/write-back flow.

## 7. Permissions

Add matching create/heal permission nodes:

- `multigolem.create.redstone`
- `multigolem.create.lapis`
- `multigolem.heal.redstone`
- `multigolem.heal.lapis`

Permissions remain permissive by default, consistent with the existing Fabric Permissions API behavior.

Spawn egg behavior should follow the V4 pattern: marked vanilla iron golem spawn eggs for Redstone and Lapis variants should require the same creation permission checks as the existing variant spawn eggs.

## 8. Client And Visuals

The client path remains texture-selection based.

Required visual assets:

- Redstone golem texture.
- Lapis golem texture.
- Redstone spawn egg stack display.
- Lapis spawn egg stack display.

Asset ownership defaults to Tyler and Charles for final art direction. If final art is not ready during implementation, use clearly material-colored placeholder textures that preserve the vanilla iron golem silhouette and are good enough for playtesting. Placeholders must be documented in the implementation summary and must not block server-side functionality.

Required particle cues:

- Redstone pulse: red dust ring around the golem.
- Redstone hit: short red spark on the target.
- Lapis ward: blue enchantment particles around protected allies.
- Lapis charm: blue enchantment particles on the charmed mob and optional `Glowing`.

No GeckoLib, custom model, or bespoke animation controller is needed.

## 9. Architecture Notes

Implementation should follow existing patterns:

- `GolemVariant` remains the source of body block, healing item, drop item, id, and display name.
- `MultiGolemConfig.defaults()` defines the default stats and ability defaults.
- `TierStats` carries nullable ability-specific config fields.
- `GolemStatsResolver` exposes typed accessors for ability code.
- `VariantAttributes.apply(...)` handles health, attack damage, and any movement modifiers.
- On-hit ability dispatch belongs near the existing attack effect path.
- Periodic ability ticks belong in ability classes, not in large mixins.
- Targeting veto or redirection for Lapis charm should use the existing `Mob#setTarget` lesson: cover proactive and reactive targeting paths, with cheap early filters.

The Lapis charm implementation needs an attachment or state record to track:

- Which mobs are charmed.
- Charm expiry game time.
- The Lapis golem that applied the charm, if enforcing max active targets per golem.
- Original target clearing or veto state.
- Cleanup on charmed mob death, owning Lapis golem death, and chunk unload or entity removal.

Do not rely on one-time `setTarget(...)` calls alone. Charmed mobs can reacquire targets through goals and retaliation paths, so the charm must be enforced while active.

Charm cleanup must be deterministic. Normal expiry removes charm state and restores normal behavior. Death, chunk unload, entity removal, or owning Lapis golem death must also remove or invalidate charm state so stale attachments do not leak into later loads.

## 10. Data Flow

### 10.1 Redstone Pulse

1. Server tick reaches a Redstone golem.
2. Ability checks whether the golem is in or recently in combat.
3. Ability checks the configured pulse interval.
4. Ability finds nearby hostile mobs in range.
5. Ability filters out targets excluded by Redstone's `ignored_target_types`.
6. Ability applies `Slowness I`.
7. Ability emits red pulse particles.

### 10.2 Redstone Static Strike

1. Vanilla `IronGolem#doHurtTarget(...)` succeeds.
2. Variant attack-effect dispatch sees `GolemVariant.REDSTONE`.
3. Ability filters dead, removed, or excluded targets.
4. Ability applies `Weakness I`.
5. Ability emits red spark particles.

### 10.3 Lapis Ward

1. Server tick reaches a Lapis golem.
2. Ability checks the configured ward interval.
3. Ability finds nearby allied entities.
4. Ability applies `Resistance I` for the configured duration.
5. Ability emits blue enchantment particles.

### 10.4 Lapis Charm

1. Vanilla `IronGolem#doHurtTarget(...)` succeeds.
2. Variant attack-effect dispatch sees `GolemVariant.LAPIS`.
3. Ability checks cooldown and active target limits.
4. If active target limits are already reached, the hit is ignored for charm purposes and the cooldown is not consumed.
5. Ability verifies the target is a charmable hostile mob.
6. Ability attaches charm state to the target with an expiry time.
7. Ability clears or redirects the target away from protected entities.
8. Ability tries to assign a nearby hostile target.
9. While charm is active, targeting hooks prevent protected targets and prefer hostile targets.
10. While charm is active, retaliation against protected entities is blocked even if a player, villager, or golem damages the charmed mob.
11. On expiry, death, chunk unload, entity removal, or owning Lapis golem death, charm state is removed or invalidated and normal mob behavior resumes.

## 11. Testing And Playtest Coverage

### 11.1 Unit And Contract Tests

Add or update focused tests for:

- `GolemVariant` id/body/healing/drop mappings.
- Config defaults for Redstone and Lapis stats.
- Config merge/write-back fills `redstone` and `lapis` tiers into older configs.
- Config merge/write-back fills all Redstone and Lapis ability fields into older configs missing those keys.
- Config validation clamps new ability fields.
- Village weights include Redstone and Lapis and preserve intentional all-zero behavior.
- Permission node generation for create/heal.
- Spawn egg stack generation includes Redstone and Lapis.
- Redstone pulse target filtering.
- Redstone pulse stops after `redstone_pulse_combat_linger_seconds` expires with no new valid combat target.
- Lapis charm target filtering, including creepers allowed and bosses denied by default.
- Lapis charm respects `ignored_target_types` without excluding creepers by default.
- Lapis charm blocks retaliation re-targeting against players, villagers, and golems when those entities attack the charmed mob.
- Lapis charm ignores later hits without consuming cooldown when `lapis_charm_max_active_targets` is already reached.
- Charm expiry behavior for attachment/state helpers.
- Charm cleanup on charmed mob death.
- Charm cleanup or invalidation on owning Lapis golem death.
- Charm cleanup or invalidation on chunk unload or entity removal.

### 11.2 Manual Playtest Rows

Add rows to `docs/playtest-checklist.md` and `docs/playtest.html` for:

- Build Redstone with redstone blocks and carved pumpkin.
- Heal Redstone with redstone dust.
- Verify Redstone pulse slows nearby hostiles.
- Verify Redstone hit applies weakness.
- Verify Redstone does not power redstone components.
- Build Lapis with lapis blocks and carved pumpkin.
- Heal Lapis with lapis lazuli.
- Verify Lapis ward grants Resistance to villagers and golems.
- Verify players do not receive Lapis ward by default.
- Verify Lapis charm makes a hostile mob stop targeting villagers/players/golems.
- Verify a charmed mob does not retaliate against a player, villager, or golem that damages it while charm is active.
- Verify a charmed mob attacks another hostile when one is nearby.
- Verify a charmed mob becomes passive when no hostile target is nearby.
- Verify creepers can be charmed and may explode near enemy mobs.
- Verify Lapis golem survives or is documented as intentionally harmed when a charmed creeper explodes at close range.
- Verify bosses are not charmed by default.
- Verify village-spawn forced weights for Redstone and Lapis.
- Verify Redstone and Lapis spawn eggs apply variants and permissions.

## 12. Documentation And Release Surfaces

Implementation must update:

- `README.md`
- `docs/modrinth-listing.md`
- `docs/curseforge-listing.md`
- `docs/playtest-checklist.md`
- `docs/playtest.html`
- `CHANGELOG.md`
- Any release metadata or checks that enumerate variant names.

Public wording should be clear that:

- Redstone slows groups and weakens targets.
- Lapis wards allies and briefly charms hostile mobs.
- Lapis can charm creepers by default, which is intentional and risky.
- Netherite remains strongest; Diamond remains the rare offensive lightning tier.

## 13. Risks And Mitigations

| Risk | Mitigation |
|---|---|
| Lapis charm fights Minecraft AI. | Keep charm temporary, enforce through targeting veto while active, and exclude edge-case mobs if needed. |
| Charmed creepers damage villages. | Keep this as intentional behavior, document it, and make `lapis_charm_affects_creepers` configurable. |
| Charmed creepers damage the Lapis golem that charmed them. | Treat self-damage as an intentional risk during the first implementation; document and playtest close-range creeper explosions before adding mitigation. |
| Lapis replaces Emerald. | Lapis grants resistance and charm; it does not heal. Emerald remains the sustain tier. |
| Redstone replaces Gold. | Redstone controls mobs; Gold remains faster and higher damage. |
| New config fields make `TierStats` bulky. | Follow the existing nullable ability-field pattern for this implementation, and avoid unrelated config refactors. |
| Village defaults become too noisy. | Make Lapis uncommon by default and keep Netherite at zero. |
| Public docs drift from defaults. | Update README, marketplace docs, playtest docs, tests, and changelog in the same implementation branch. |

## 14. Implementation Handoff Prompt

Use this prompt to start the implementation session:

```text
We are implementing the approved MultiGolem Redstone/Lapis design in:
C:\Users\tyler\AI Projects\MultiGolem

Start by reading:
- AGENTS.md instructions from the thread: verify `git rev-parse --show-toplevel` before edits and keep all edits in the assigned repo/worktree.
- docs/superpowers/specs/2026-05-25-multigolem-redstone-lapis-design.md
- docs/26.1.2-mojang-targets.md
- docs/LESSONS-LEARNED.md
- Current variant/config/spawn/ability files under src/main/java/dev/charles/multigolem

Scope:
- Implement Redstone and Lapis golems exactly as specified.
- Keep the no-new-entity architecture.
- Add config, tests, docs, playtest rows, permissions, spawn eggs, village weights, textures/assets, and changelog updates required by the spec.
- If final Redstone/Lapis art is not ready, use material-colored placeholder textures and document them.
- Lapis charm must affect creepers by default.
- Bosses remain charm-immune by default.
- Redstone must not power redstone components.
- Do not create a PR unless Tyler explicitly asks.

Workflow:
- Use Superpowers TDD and implementation planning.
- Verify repo root before edits.
- Keep changes scoped to this feature.
- Run focused tests first, then full build/release checks appropriate for MultiGolem.
- If using Revue/Claude review, action findings explicitly and separate implementation readiness from release readiness.
```
