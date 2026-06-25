# MultiGolem Redstone Golem - Design

**Date:** 2026-06-24
**Authors:** Tyler & Charles
**Status:** Draft design and implementation-planning input; implementation intentionally not started; Revue review not run in this session
**Predecessors:**

- V5 Zombie Golem design: `docs/superpowers/specs/2026-05-25-multigolem-zombie-golem-design.md`
- Prior combined Redstone/Lapis sketch: `docs/superpowers/specs/2026-05-25-multigolem-redstone-lapis-design.md`
- Multiloader/NeoForge design: `docs/superpowers/specs/2026-06-11-multigolem-multiloader-neoforge-design.md`

## 1. Summary

Redstone Golems are V7's lower-strength emergency-control defenders. They are built from Redstone Blocks, heal with Redstone Dust, remain in the existing vanilla `IronGolem` plus MultiGolem identity architecture, and are available on both Fabric and NeoForge.

The Redstone role is not raw damage. A Redstone Golem is weaker than Gold in normal combat, but becomes temporarily harder to kill when badly damaged. Below 25% health it overcharges, gaining attack and resistance without gaining movement speed. On death it releases an overload pulse that applies Slowness X to nearby hostile targets, buying a village a last few seconds rather than turning the golem into an explosive damage dealer.

Implementation is deferred until Tyler explicitly approves moving from design into implementation.

## 2. Goals And Non-Goals

**Goals**

- Add `redstone` as a first-class `GolemVariant`.
- Build Redstone Golems from a standard Iron-Golem-style Redstone Block T-pattern plus carved pumpkin.
- Heal Redstone Golems with Redstone Dust.
- Make Redstone lower-strength than Gold during normal combat.
- Trigger overcharge at or below 25% health.
- While overcharged, grant attack and resistance bonuses but no speed bonus.
- Persist enough overcharge state to survive save/load without retriggering particles or cooldown incorrectly.
- Release a Slowness X overload pulse on death.
- Add Redstone marked spawn egg, spawner marker support, permissions, village spawn weighting, status/customization payload exposure, Golempedia, docs, and manual verification rows.
- Preserve Fabric and NeoForge parity from the first implementation slice.

**Non-goals**

- No Redstone Golem implementation in this design session.
- No V8 Lapis Golem or unrelated golem work.
- No new entity type, block, item, model system, or redstone power emission.
- No speed boost, pathfinding boost, jump boost, teleport, or player command behavior.
- No explosion damage or block destruction from the death pulse.
- No friendly-fire Slowness against villagers, wandering traders, Iron Golems, non-zombie MultiGolems, or the owner/creator concept. MultiGolem currently does not have ownership; this design does not add it.

## 3. Variant Identity

| Property | Default |
|---|---|
| Variant id | `redstone` |
| Display name | `Redstone` |
| Body block | `minecraft:redstone_block` |
| Heal item | `minecraft:redstone` |
| Drop item | `minecraft:redstone` |
| Max health | `90` |
| Attack damage | `13.0` |
| Movement speed | Vanilla Iron Golem speed; no Redstone modifier |
| Knockback resistance | Vanilla Iron Golem baseline unless overcharged |

Redstone's base stats put it below Iron and clearly below Gold. The Redstone value is emergency control: it has weaker normal pressure, then becomes a temporary defensive problem when nearly dead.

Drop counts should follow the existing catalog pattern for common materials: `3` to `5` Redstone Dust plus normal poppy behavior if the current loot path includes poppies.

## 4. Construction, Healing, Permissions, And Spawns

Redstone uses the existing MultiGolem creation language:

1. Player places a carved pumpkin on a Redstone Block T-pattern.
2. MultiGolem recognizes `minecraft:redstone_block` as `GolemVariant.REDSTONE`.
3. Creation checks `multigolem.create.redstone`.
4. On success, a vanilla `IronGolem` receives Redstone identity, configured stats, and full fresh-spawn health.
5. On denial, no entity spawns and blocks remain intact.

Healing uses Redstone Dust and checks `multigolem.heal.redstone`. Global `allow_golem_healing` still applies. Full-health Redstone Golems no-op before permission checks, matching current healing UX. Wrong healing items must not fall through to vanilla iron-ingot healing.

Marked spawn eggs and marked spawners follow the existing V4/V5 pattern: the item remains a marked `minecraft:iron_golem_spawn_egg`, uses the same creation permission as other marked eggs, and spawned Redstone Golems are not player-created for village-origin or ownership semantics.

## 5. Overcharge Ability

### 5.1 Trigger

Overcharge triggers when a Redstone Golem's health is at or below 25% of its configured max health after damage is applied.

Default trigger fields:

| Field | Default | Meaning |
|---|---:|---|
| `redstone_overcharge_enabled` | `true` | Enables the overcharge ability. |
| `redstone_overcharge_health_threshold_percent` | `0.25` | Fraction of max health at or below which overcharge starts. |
| `redstone_overcharge_duration_seconds` | `12.0` | Active overcharge duration. |
| `redstone_overcharge_cooldown_seconds` | `45.0` | Cooldown after overcharge ends. |

The trigger is edge-driven: while already overcharged, additional damage does not refresh the duration. While on cooldown, dropping below the threshold again does not start a new overcharge. After cooldown expires, a later health transition back to or below threshold can trigger again.

Healing above the threshold does not cancel an active overcharge. Death clears persisted active state as part of normal entity removal.

### 5.2 Buffs

While overcharged, a Redstone Golem gets:

| Field | Default | Meaning |
|---|---:|---|
| `redstone_overcharge_attack_multiplier` | `1.50` | Multiplies configured attack damage while active. |
| `redstone_overcharge_resistance_amplifier` | `1` | Resistance II because Minecraft amplifiers are zero-based. |
| `redstone_overcharge_resistance_refresh_seconds` | `3.0` | Resistance effect duration applied/refreshed during active state. |

No Redstone field may modify movement speed. `VariantAttributes` should continue to reserve movement speed changes for Gold only. Redstone overcharge should use attack and resistance paths, not the Gold speed modifier.

The resistance effect is preferred over a permanent armor/knockback modifier because it is readable, visible to players, and easy to clear on expiry. Attack bonus may be implemented as a temporary attribute modifier or as a centralized attack-damage multiplier, but it must be deterministic, removed on expiry, and not stack with itself.

### 5.3 Persistence

Extend `GolemAbilityState` or an equivalent common ability-state record with Redstone fields:

- `redstone_overcharge_active_until_game_time`
- `redstone_overcharge_cooldown_until_game_time`
- `redstone_was_below_threshold`

Persisting these fields prevents save/load from retriggering overcharge every time a low-health Redstone Golem is loaded. If an old save lacks the fields, defaults are inactive and not below threshold.

### 5.4 Visual And Audio Cues

Default cues:

- Start: redstone dust ring and a comparator/click-like sound if a suitable vanilla sound exists.
- Active tick: light red dust particles around the torso at a restrained interval.
- End: short fizz/click cue.

If sound selection is not clean during implementation, keep particles and document sound deferral in the implementation summary.

## 6. Death Overload Pulse

When a Redstone Golem dies, it releases one overload pulse.

Default fields:

| Field | Default | Meaning |
|---|---:|---|
| `redstone_death_pulse_enabled` | `true` | Enables the death pulse. |
| `redstone_death_pulse_radius` | `8` | Block radius. |
| `redstone_death_pulse_slowness_seconds` | `6.0` | Slowness duration. |
| `redstone_death_pulse_slowness_amplifier` | `9` | Slowness X because amplifiers are zero-based. |
| `redstone_death_pulse_particles_enabled` | `true` | Enables death-pulse particles independently from overcharge particles. |

Target filtering:

- Affect hostile mobs that the Redstone Golem could reasonably fight.
- Do not affect villagers, wandering traders, vanilla Iron Golems, non-zombie MultiGolems, Redstone Golems, players, creative/spectator players, zombies allied with Zombie Golems unless they are already valid hostile targets through current targeting rules, or non-living entities.
- Respect Redstone's `ignored_target_types`; default Redstone ignores creepers to match the current non-iron defender policy.
- Zombie Golems count as hostile and should be affected.

The pulse applies only Slowness. It does not deal damage, ignite, knock back, break blocks, power redstone, or create explosions.

## 7. Village, Raid, Targeting, And Existing Systems

Redstone Golems are village defenders like Copper, Gold, Emerald, Diamond, and Netherite.

- Villagers and raids should treat Redstone as an Iron-family defender through existing variant identity.
- Redstone Golems use the current normal defender targeting rules, including `ignored_target_types`.
- Zombie Golems and Redstone Golems should fight each other through the current Zombie faction rules once Redstone is part of non-zombie variant sets.
- Redstone Golems are not hostile to players by default.
- Permissions affect player-built creation, marked egg use, marked spawner configuration, and Redstone Dust healing only.
- Natural village spawns are not permission-gated.
- Existing golems do not change variant after upgrade.

Village spawn defaults should add Redstone as a common defender:

| Variant | Weight |
|---|---:|
| Iron | 19 |
| Copper | 19 |
| Redstone | 19 |
| Gold | 19 |
| Emerald | 19 |
| Diamond | 5 |
| Netherite | 0 |

The player-facing order should become `iron`, `copper`, `redstone`, `gold`, `emerald`, `diamond`, `netherite`, `zombie` where the surface includes all variants. Zombie village maintenance remains separate from normal village defender weights.

## 8. Fabric And NeoForge Parity

Common code owns Redstone identity, config, state, filtering, overcharge math, death pulse math, and ability behavior. Loader adapters own event wiring.

Expected parity points:

- Fabric and NeoForge both register Redstone tick handling.
- Fabric and NeoForge both route death/damage hooks needed for overcharge and overload.
- Common state attachment persists on both loaders.
- Fabric and NeoForge permission nodes use the same names and permissive defaults.
- Fabric and NeoForge marked eggs and spawners use the same common marker payload.
- Fabric and NeoForge customizations/status payloads expose the same Redstone config summary.
- Fabric and NeoForge client render paths select the same Redstone texture when the client has MultiGolem installed.

If one loader lacks a clean hook for the chosen implementation, stop and revise the plan before accepting one-loader behavior.

## 9. Config Validation

Add Redstone fields to `TierStats` using the existing nullable ability-field pattern.

Validation rules:

- Health and attack use existing shared clamps.
- Threshold clamps to `0.01..1.0`.
- Radius clamps to `1..64`.
- Durations and cooldowns clamp to `0.0..3600.0`, except refresh duration clamps to at least `0.5`.
- Attack multiplier clamps to `1.0..10.0`.
- Resistance amplifier clamps to `0..4`.
- Slowness amplifier clamps to `0..9`; default remains Slowness X.
- Booleans follow existing malformed-field fallback behavior.
- Unknown fields remain preserved where the current merge/write-back path preserves them.

## 10. Source Spike Requirements

The implementation plan must start with a source spike for:

- Current body-block creation path for adding Redstone Block.
- Current healing interception path for Redstone Dust and wrong-item fallback prevention.
- Current damage or tick hook that can detect the threshold crossing after damage.
- Current death hook that can emit exactly one overload pulse.
- Whether attack bonus is cleaner as an attribute modifier or centralized attack-damage multiplier.
- Whether resistance should be refreshed from tick handling or event handling.
- Exact Fabric event registration changes.
- Exact NeoForge event registration changes.
- Existing ability-state attachment codec migration behavior.
- Existing status/customization payload surfaces that enumerate per-tier fields.
- Existing Golempedia and playtest surfaces that enumerate variants.

If a spike shows that Redstone needs a new entity type, speed modifier, or one-loader-only behavior, stop for design review.

## 11. Testing

Unit and source tests should cover:

- `GolemVariant.REDSTONE` id, display name, body block, heal item, drop item, catalog flags, and spawn egg inclusion.
- Redstone defaults in `MultiGolemConfig`.
- Config merge/write-back fills missing Redstone tier fields into older configs.
- Validation clamps Redstone ability fields.
- Village weights include Redstone in roll order and defaults.
- Permission nodes `multigolem.create.redstone` and `multigolem.heal.redstone`.
- Ability state codec round-trips Redstone fields and old JSON defaults them safely.
- Overcharge trigger starts once at or below threshold.
- Active overcharge does not refresh from repeated damage.
- Cooldown blocks retrigger.
- Healing above threshold does not cancel active overcharge.
- Overcharge attack bonus is removed on expiry and does not stack.
- Redstone never receives a movement-speed modifier.
- Death pulse applies Slowness X to valid hostile targets.
- Death pulse skips villagers, traders, players, defenders, and ignored target types.
- Fabric and NeoForge event source tests prove Redstone tick/death/damage hooks are registered.

Manual verification rows should be added for both Fabric and NeoForge:

- Build Redstone Block T-pattern plus carved pumpkin.
- Heal damaged Redstone Golem with Redstone Dust.
- Deny create/heal permissions and verify no block/item consumption.
- Spawn Redstone from marked egg and marked spawner.
- Force Redstone below 25% health and verify overcharge starts.
- Verify overcharge increases attack/resistance and does not increase speed.
- Heal above 25% while overcharged and verify active state continues until expiry.
- Verify cooldown prevents immediate retrigger.
- Kill Redstone and verify Slowness X pulse affects nearby hostiles.
- Verify the pulse skips villagers, players, and non-zombie golems.
- Verify Zombie Golems and Redstone Golems fight each other.
- Verify Fabric and NeoForge behavior match.

## 12. Documentation And Release Surfaces

Implementation should update:

- `README.md`
- `docs/playtest-checklist.md`
- `docs/playtest.html`
- `docs/modrinth-listing.md`
- `docs/curseforge-listing.md`
- `INTERNAL_CHANGELOG.md`
- `CHANGELOG.md` only when Tyler approves public release wording

Public wording should describe Redstone as a lower-strength emergency-control defender that overcharges near death and leaves a Slowness X pulse. It should explicitly say the overcharge does not add speed.

## 13. Approved Design Decisions

- Body block is `minecraft:redstone_block`.
- Heal and drop item is `minecraft:redstone`.
- Redstone is lower-strength than Iron and Gold by default: 90 HP and 13 attack.
- Redstone ignores creepers by default like the current non-iron defenders.
- Overcharge triggers at or below 25% health.
- Overcharge lasts 12 seconds by default and has a 45 second cooldown.
- Overcharge grants attack/resistance without speed.
- Death overload pulse applies Slowness X for 6 seconds in an 8 block radius.
- Death overload pulse affects hostile targets only and does not damage, explode, ignite, or power redstone.
- Fabric and NeoForge parity is required in the first implementation.
- V8 Lapis and unrelated golem work remain out of scope.
