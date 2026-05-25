# MultiGolem Zombie Golem - Design

**Date:** 2026-05-25
**Authors:** Tyler & Charles
**Status:** Draft, ready for implementation planning in a new session
**Predecessors:**

- V1 design: `docs/superpowers/specs/2026-05-15-multigolem-design.md`
- V2 design: `docs/superpowers/specs/2026-05-16-multigolem-v2-design.md`
- V3 design: `docs/superpowers/specs/2026-05-17-multigolem-v3-design.md`
- V3.1 permissions design: `docs/superpowers/specs/2026-05-17-multigolem-permissions-design.md`
- V4 spawn egg implementation: current `main`

## 1. Summary

Zombie Golems add a hostile corruption variant to MultiGolem.

Zombie Golems are built from a Mossy Cobblestone T-pattern plus a carved pumpkin, can appear in zombie-villager village areas, use the existing marked vanilla iron golem spawn egg path, and are immediately hostile. They attack players, villagers, wandering traders, vanilla Iron Golems, and non-zombie MultiGolem variants. They ignore zombies and zombie villagers. Their player hits apply configurable sickness effects. Their civilian hits convert villagers and wandering traders into zombie villagers instead of dealing normal damage.

The implementation should stay inside MultiGolem's existing architecture: a vanilla `IronGolem` entity with a `GolemVariantAttachment`, variant-specific targeting/interaction behavior, config-driven stats and abilities, permissions, textures, drops, healing, and marked spawn eggs. This is not a new entity-type design.

Implementation is intentionally deferred to a new session.

## 2. Goals And Non-Goals

**Goals**

- Add `zombie` as a new MultiGolem variant.
- Let players build Zombie Golems with the standard iron-golem-style T-pattern using Mossy Cobblestone body blocks.
- Make every Zombie Golem immediately hostile, including player-built and spawn-egg-spawned Zombie Golems.
- Heal Zombie Golems with Rotten Flesh.
- Apply configurable sickness effects to players hit by Zombie Golems.
- Let Zombie Golems attack villagers and wandering traders, converting them into zombie villagers.
- Let Zombie Golems fight vanilla Iron Golems and all non-zombie MultiGolem variants.
- Make Zombie Golems ignore zombies and zombie villagers.
- Add zombie-villager-driven village spawn maintenance with default max 2 Zombie Golems per qualifying village area.
- Add a marked vanilla iron golem spawn egg for Zombie Golems, consistent with V4.
- Add permissive-by-default `multigolem.create.zombie` and `multigolem.heal.zombie` permission nodes.
- Keep all new gameplay defaults configurable in `config/multigolem.json`.

**Non-goals**

- No custom Zombie Golem entity type.
- No custom Rotten Flesh Block or other new body block.
- No taming, pacifying, ownership, loyalty, or creator exception.
- No sunlight burning. Zombie Golems do not catch fire in daylight.
- No llama corruption. Wandering trader llamas are ignored by this feature.
- No strict structure-file-only abandoned-village detection unless a source spike proves it is clean and worth using.
- No regular-zombie-only spawning. Regular zombies can contribute a bonus only when zombie villagers are already present.
- No broader Skeleton Golem, undead faction, or new mob-family design in this spec.

## 3. Variant Identity

Zombie Golems are a new `GolemVariant.ZOMBIE`.

| Property | Default |
|---|---|
| Variant id | `zombie` |
| Display name | `Zombie` |
| Body block | `minecraft:mossy_cobblestone` |
| Heal item | `minecraft:rotten_flesh` |
| Drop item | `minecraft:rotten_flesh` |
| Max health | `100` |
| Attack damage | `15.0` |
| Player-created? | Same source semantics as other variants, but behavior remains hostile either way |

The defaults intentionally match vanilla Iron Golem durability and damage, then add danger through sickness and hostile behavior.

Mossy Cobblestone is selected because it is survival-accessible, craftable from Cobblestone plus Vine or Cobblestone plus Moss Block, visually fits abandoned villages, and avoids adding a custom block.

## 4. Recipe And Creation

Zombie Golems use the existing MultiGolem T-pattern creation language:

- 1 Mossy Cobblestone base block.
- 1 Mossy Cobblestone center block.
- 2 Mossy Cobblestone arm blocks.
- 1 carved pumpkin placed on top.

On successful creation:

1. The Mossy Cobblestone blocks and pumpkin are consumed like other player-built MultiGolem variants.
2. The spawned entity is a vanilla `IronGolem` with `GolemVariantAttachment` set to `ZOMBIE`.
3. The normal creation permission gate checks `multigolem.create.zombie`.
4. If permission is denied, no Zombie Golem spawns and the blocks are not consumed, matching the current creation-gate contract.
5. If permission is allowed, the Zombie Golem spawns and is immediately hostile.

There is no "friendly because the player built it" mode.

## 5. Core Behavior

### 5.1 Hostility

Zombie Golems are hostile mobs in behavior, even though they are implemented as an `IronGolem` variant.

They target:

- Survival/adventure players, subject to vanilla creative/spectator targeting rules.
- Villagers.
- Wandering traders.
- Vanilla Iron Golems.
- Copper, Gold, Emerald, Diamond, Netherite, and any other non-zombie MultiGolem variants.

They ignore or treat as allies:

- Zombies.
- Zombie villagers.
- Zombie Golems.

They do not need to target trader llamas. If vanilla AI or a future hook would make llamas a target, this feature should avoid adding that behavior.

### 5.2 Defender Counterplay

Vanilla Iron Golems and non-zombie MultiGolem variants should treat Zombie Golems as hostile and attack them.

This is a symmetric faction rule: living village defenders and Zombie Golems fight each other.

### 5.3 No Sunlight Burning

Zombie Golems do not burn in sunlight.

The feature goal is an abandoned-village threat, not a mob that either dies at sunrise or creates fire-related village damage. Do not add sunlight ignition, shelter-seeking, or daylight fire mitigation as part of this scope.

### 5.4 B-Lite Undead Flavor

Zombie Golems are "B-lite" undead-flavored Iron Golem variants:

- They are hostile.
- They use Rotten Flesh healing.
- They spread zombie villagers through civilian conversion.
- They apply sickness on hit.
- They may use zombie-ish texture and sounds where practical.

They do not need full undead entity mechanics such as a new mob category, Smite-specific behavior, healing/harming potion inversion, sunlight burn, or zombie reinforcement rules.

## 6. Player Hit Sickness

When a Zombie Golem successfully hits a player, it applies a configurable sickness package.

Default effects:

| Effect | Enabled | Duration | Amplifier |
|---|---:|---:|---:|
| Hunger | `true` | `12` seconds | `0` |
| Nausea | `true` | `4` seconds | `0` |
| Poison | `true` | `4` seconds | `0` |

Poison is intentionally short. The Iron-Golem-strength hit is the main damage; poison is flavor pressure.

Effect application should happen only on successful hits. It should not apply on misses, cancelled attacks, invulnerable targets, or non-player targets.

## 7. Civilian Conversion

### 7.1 Villagers

When a Zombie Golem successfully hits a normal villager:

1. The attack does not deal normal damage to the villager.
2. The villager converts into a zombie villager at the same position.
3. Conversion chance defaults to `100%`.
4. Preserve as much vanilla identity as practical, such as profession, villager type/biome, custom name, baby/adult state, persistence, and relevant NBT.

The conversion is the consequence. Do not also apply the normal Iron Golem attack damage to the villager.

### 7.2 Wandering Traders

When a Zombie Golem successfully hits a wandering trader:

1. The attack does not deal normal damage to the trader.
2. The trader converts into a zombie villager at the same position.
3. Conversion chance defaults to `100%`.
4. Preserve custom name and persistence where practical.

There is no vanilla zombie wandering trader, so zombie villager is the chosen result.

### 7.3 Trader Llamas

Trader llamas are out of scope. Zombie Golems should not gain special llama targeting or conversion behavior.

## 8. Healing

Zombie Golems heal from Rotten Flesh.

Default behavior:

- `allow_golem_healing` still globally controls all golem healing.
- Matching item is Rotten Flesh.
- Heal amount defaults to `25.0`.
- Healing requires `multigolem.heal.zombie`, permissive by default.
- Full-health Zombie Golems no-op before permission checks, matching the current healing UX.
- Wrong healing items should not fall through to vanilla iron-ingot healing for Zombie Golems.
- Healing never pacifies, tames, or changes targeting behavior.

This should follow the existing V3.1 healing permission semantics for full-health no-op, mismatch handling, denial, item consumption, and repair feedback.

## 9. Zombie Village Spawn Maintenance

### 9.1 Intent

The gameplay intent is that naturally generated abandoned/zombie villages reliably have Zombie Golems.

The concrete implementation should use a zombie-villager-driven village maintenance rule because MultiGolem already works with villager/golem village behavior and because regular zombies alone should not create Zombie Golems.

This means generated abandoned villages are the primary target, but the practical rule may also apply to overrun village areas that contain zombie villagers. If strict "naturally generated abandoned villages only" filtering is required, implementation must first prove a clean structure signal exists and then return for design review before adding that restriction. Without that proof, the design prefers the zombie-villager village-area rule below.

### 9.2 Eligibility

A village area is eligible when it has at least one zombie villager in the relevant village scan area.

Regular zombies alone never make an area eligible.

Regular zombies may add bonus pressure only when at least one zombie villager is present.

### 9.3 Desired Count Defaults

Default config:

```json
"zombie_village_spawning": {
  "enabled": true,
  "min_zombie_villagers": 1,
  "zombie_villagers_per_golem": 5,
  "regular_zombie_bonus_enabled": true,
  "regular_zombie_bonus_threshold": 3,
  "max_zombie_golems_per_village": 2
}
```

Default desired-count behavior:

- 0 zombie villagers: desired Zombie Golems = 0.
- 1-4 zombie villagers: desired Zombie Golems = 1.
- 5 or more zombie villagers: desired Zombie Golems = 2, capped by `max_zombie_golems_per_village`.
- If regular zombie bonus is enabled and 3 or more regular zombies are present with at least 1 zombie villager, desired Zombie Golems can increase by 1, still capped by `max_zombie_golems_per_village`.
- Existing Zombie Golems count against the desired total.

This is not a raw spawn rate. It is a local desired-count maintenance rule that should avoid runaway spawning.

### 9.4 Spawn Timing And Placement

Implementation planning must source-spike the safest hook. The design preference is:

- Reuse vanilla village/golem spawning position logic where possible.
- Avoid spawning inside blocks, too close to players, or outside the village area.
- Avoid one-tick mass spawning; maintenance may happen gradually.
- Respect vanilla mob-spawning constraints where reasonable, but do not require villagers to be alive or panicking.

If no safe village-area hook exists, pause during implementation planning and revisit this section rather than broadening the hook silently.

## 10. Spawn Eggs And Spawners

Zombie Golems get a marked vanilla iron golem spawn egg like the V4 variants.

Expected behavior:

- The item remains a vanilla `minecraft:iron_golem_spawn_egg` with MultiGolem custom data marking `multigolem.variant = zombie`.
- Modded clients may show the Zombie Golem egg with variant-specific item model/texture if the existing V4 asset path supports it.
- Vanilla clients remain compatible and see a vanilla iron golem spawn egg.
- Using the marked egg checks `multigolem.create.zombie`.
- Successful egg spawn creates a Zombie Golem and it is immediately hostile.
- Marked Zombie Golem eggs can configure spawners like the existing marked variant eggs.
- Spawner-spawned Zombie Golems are immediately hostile.

Unmarked vanilla iron golem spawn eggs and unmarked vanilla spawners remain unchanged.

## 11. Permissions

Add two nodes:

- `multigolem.create.zombie`
- `multigolem.heal.zombie`

Both are permissive by default, consistent with the existing V3.1 policy.

Creation permission covers:

- Player-built Mossy Cobblestone T-pattern creation.
- Marked Zombie Golem spawn egg use.
- Marked Zombie Golem spawn egg spawner configuration.

Healing permission covers:

- Rotten Flesh healing on damaged Zombie Golems.

Natural zombie-village spawn maintenance is not player creation and should not require a player permission.

## 12. Config

Zombie Golem config should fit the existing top-level and per-tier model.

### 12.1 Tier Defaults

Add `tiers.zombie` with shared fields:

```json
"zombie": {
  "max_health": 100,
  "attack_damage": 15.0,
  "anger_on_hit": true,
  "ignored_target_types": [],
  "zombie_rotten_flesh_heal_amount": 25.0,
  "zombie_hunger_enabled": true,
  "zombie_hunger_seconds": 12,
  "zombie_hunger_amplifier": 0,
  "zombie_nausea_enabled": true,
  "zombie_nausea_seconds": 4,
  "zombie_nausea_amplifier": 0,
  "zombie_poison_enabled": true,
  "zombie_poison_seconds": 4,
  "zombie_poison_amplifier": 0,
  "zombie_convert_villagers_enabled": true,
  "zombie_villager_conversion_chance": 1.0,
  "zombie_convert_wandering_traders_enabled": true,
  "zombie_wandering_trader_conversion_chance": 1.0
}
```

`anger_on_hit` remains present for schema consistency, but Zombie Golems are already hostile. Implementation planning should decide whether it is meaningful for Zombie Golems or simply ignored.

### 12.2 Spawn Maintenance Defaults

Add top-level `zombie_village_spawning`:

```json
"zombie_village_spawning": {
  "enabled": true,
  "min_zombie_villagers": 1,
  "zombie_villagers_per_golem": 5,
  "regular_zombie_bonus_enabled": true,
  "regular_zombie_bonus_threshold": 3,
  "max_zombie_golems_per_village": 2
}
```

### 12.3 Validation Rules

- Preserve existing lossless merge/canonicalize behavior.
- Missing zombie tier fields merge from defaults.
- Unknown zombie fields are preserved where possible.
- Durations clamp to non-negative values.
- Effect amplifiers clamp to a safe non-negative range.
- Conversion chances clamp to `0.0` through `1.0`.
- Heal amount clamps to `0.0` through `2048.0`.
- `min_zombie_villagers` clamps to at least `1`.
- `zombie_villagers_per_golem` clamps to at least `1`.
- `regular_zombie_bonus_threshold` clamps to at least `1`.
- `max_zombie_golems_per_village` clamps to `0` or higher. `0` intentionally disables zombie-village Zombie Golem spawning while leaving craft/egg paths intact.

## 13. Data Flow

### 13.1 Player-Built Creation

1. Player places carved pumpkin atop a Mossy Cobblestone T-pattern.
2. MultiGolem recognizes the body block as `GolemVariant.ZOMBIE`.
3. MultiGolem checks `multigolem.create.zombie`.
4. If denied, creation fails with no block consumption.
5. If allowed, MultiGolem spawns a vanilla `IronGolem`.
6. MultiGolem sets `GolemVariantAttachment` to `ZOMBIE`.
7. MultiGolem applies configured stats.
8. Zombie targeting behavior makes the golem hostile immediately.

### 13.2 Player Attack Effect

1. Zombie Golem successfully hits a player.
2. MultiGolem reads `tiers.zombie` sickness config.
3. Enabled effects are applied for their configured durations and amplifiers.
4. Normal attack damage remains Iron-Golem-strength by default.

### 13.3 Civilian Conversion

1. Zombie Golem successfully hits a villager or wandering trader.
2. MultiGolem cancels or bypasses normal damage for that hit.
3. MultiGolem rolls the configured conversion chance.
4. On success, MultiGolem replaces the target with a zombie villager.
5. On failure, implementation planning must decide whether the hit does no damage or falls back to normal damage. Design preference: no normal damage, because conversion is the intended behavior.

### 13.4 Zombie Village Maintenance

1. A village-area maintenance hook scans a bounded village area.
2. It counts zombie villagers.
3. If fewer than `min_zombie_villagers`, desired count is 0.
4. It counts regular zombies only for optional bonus pressure.
5. It counts existing Zombie Golems.
6. It computes desired count and caps it at `max_zombie_golems_per_village`.
7. If existing count is below desired count, it attempts to spawn a Zombie Golem at a safe village position.
8. The spawned entity is a vanilla `IronGolem` with `GolemVariantAttachment` set to `ZOMBIE`.

## 14. Source Spike Requirements

The implementation plan must start with source spikes for the highest-risk hooks:

- Exact current player-built creation path for adding a new Mossy Cobblestone body block.
- Exact current healing interception path for adding Rotten Flesh without letting vanilla iron-ingot healing leak through.
- Exact current attack hook for applying player sickness and civilian conversion.
- Whether villager/trader conversion can reuse vanilla zombie conversion helpers or requires explicit replacement.
- Which villager/trader data can be preserved safely during conversion.
- Exact targeting/goal hooks needed to make Zombie Golems target players, villagers, traders, and non-zombie golems.
- Exact targeting/goal hooks needed to make non-zombie golems attack Zombie Golems.
- Exact targeting/goal hooks needed to keep Zombie Golems from attacking zombies and zombie villagers.
- Whether zombie-ish sounds can be added without a custom entity type. If not clean, sounds should be deferred.
- Exact village-area hook or maintenance mechanism for zombie-villager-driven spawning.
- Whether a clean generated abandoned-village structure signal exists. If not, use the zombie-villager village-area rule.
- Exact V4 spawn egg data/model path for adding `zombie`.
- Exact spawner marker behavior for marked Zombie Golem eggs.

If any spike proves that the feature requires a custom entity type, implementation must pause for design review.

## 15. Error Handling

- Config errors should warn, clamp or fall back where safe, and keep the server running.
- A failed Zombie Golem variant application should log an error and leave the entity as a normal Iron Golem rather than crashing.
- A failed sickness effect application should log an error and leave normal attack damage intact.
- A failed civilian conversion should log an error and avoid duplicate entities.
- A failed zombie-village maintenance spawn should skip that attempt without tight retry loops.
- Required mixin target failures may fail fast at startup, consistent with existing required-mixin posture.

## 16. Testing

### 16.1 Unit Tests

- `GolemVariant` maps Mossy Cobblestone to `ZOMBIE`.
- `GolemVariant` maps Rotten Flesh to `ZOMBIE` healing.
- Config defaults include `tiers.zombie`.
- Config defaults include `zombie_village_spawning`.
- Zombie sickness config clamps invalid durations, amplifiers, and booleans.
- Zombie conversion chances clamp to `0.0` through `1.0`.
- Zombie village spawn maintenance desired-count helper covers:
  - 0 zombie villagers => 0.
  - 1 zombie villager => 1.
  - 4 zombie villagers => 1.
  - 5 zombie villagers => 2.
  - regular zombies without zombie villagers => 0.
  - regular zombie bonus with zombie villagers raises desired count but respects cap.
  - max cap 0 disables maintenance spawning.
- Permissions include `multigolem.create.zombie` and `multigolem.heal.zombie`.

### 16.2 Integration Or Minecraft-Backed Tests

Where the current test harness can support it:

- Player-built Mossy Cobblestone T-pattern creates a Zombie Golem.
- Denied `multigolem.create.zombie` prevents player-built creation.
- Rotten Flesh heals a damaged Zombie Golem.
- Denied `multigolem.heal.zombie` prevents healing without consuming Rotten Flesh.
- Wrong healing item does not heal Zombie Golems through vanilla fallback.
- Marked Zombie Golem egg spawns a Zombie Golem.
- Denied `multigolem.create.zombie` prevents marked egg spawn.
- Marked Zombie Golem egg configures spawners with `zombie` marker.
- Spawner-spawned Zombie Golems are hostile.

### 16.3 Playtest Rows

- Build a Mossy Cobblestone T-pattern plus carved pumpkin. Zombie Golem spawns.
- Confirm the player-built Zombie Golem immediately attacks the creator.
- Spawn a Zombie Golem with its marked egg. It is immediately hostile.
- Damage a Zombie Golem. Right-click with Rotten Flesh. It heals and consumes Rotten Flesh.
- Right-click a full-health Zombie Golem with Rotten Flesh. No permission denial, no consumption, no repair feedback.
- Right-click a damaged Zombie Golem with Iron Ingot. It does not heal through vanilla fallback.
- Let a Zombie Golem hit a survival player. Confirm Hunger, Nausea, and Poison defaults.
- Disable each sickness effect independently and confirm it no longer applies.
- Let a Zombie Golem hit a villager. The villager becomes a zombie villager and does not take normal hit damage first.
- Let a Zombie Golem hit a wandering trader. The trader becomes a zombie villager.
- Confirm trader llamas are not specially targeted or converted.
- Confirm Zombie Golems ignore zombies and zombie villagers.
- Confirm Zombie Golems fight vanilla Iron Golems.
- Confirm Copper, Gold, Emerald, Diamond, and Netherite golems fight Zombie Golems.
- Confirm regular zombies alone do not produce Zombie Golems.
- Confirm an area with 1 zombie villager can spawn/maintain 1 Zombie Golem.
- Confirm an area with enough zombie villagers can maintain up to 2 Zombie Golems by default.
- Confirm existing Zombie Golems count against the cap.
- Confirm setting `max_zombie_golems_per_village` to `0` disables maintenance spawning but not player-built or egg-spawned Zombie Golems.

## 17. Documentation And Release Notes

Implementation should update:

- `README.md`
- `docs/playtest-checklist.md`
- `docs/modrinth-listing.md`
- `docs/curseforge-listing.md`
- `CHANGELOG.md`
- `INTERNAL_CHANGELOG.md`, if used for the active release flow

Player-facing copy should describe Zombie Golems as hostile corrupted golems, not defenders.

## 18. Open Implementation Questions

These are plan/spike questions, not unresolved product decisions:

- What is the narrowest safe hook for zombie-villager village maintenance?
- Can implementation reliably identify a "village area" without persisting new per-village state?
- Can vanilla conversion helpers preserve enough villager/trader data, or is explicit replacement cleaner?
- Can zombie-ish sounds be attached cleanly to an `IronGolem` variant?
- Does `anger_on_hit` have any useful meaning for an always-hostile Zombie Golem, or should it be retained only for schema consistency?

## 19. Approved Design Decisions

- Zombie Golems are craftable.
- Crafting block is Mossy Cobblestone.
- Zombie Golems are immediately hostile, including to their creator.
- Zombie Golems hit like Iron Golems by default.
- Zombie Golem player hits apply Hunger, short Nausea, and short Poison by default.
- Zombie Golems heal from Rotten Flesh.
- Rotten Flesh healing does not tame or pacify Zombie Golems.
- Zombie Golems do not burn in sunlight.
- Zombie Golems attack villagers and wandering traders.
- Civilian hits convert villagers and wandering traders into zombie villagers with 100% default chance.
- Trader llamas are ignored.
- Zombie Golems fight vanilla Iron Golems and non-zombie MultiGolems.
- Zombie Golems ignore zombies and zombie villagers.
- At least one zombie villager makes a village area eligible for Zombie Golem maintenance spawning.
- Regular zombies alone cannot spawn Zombie Golems.
- Regular zombies can provide a bonus only when zombie villagers are present.
- Default max Zombie Golems per qualifying village area is 2.
- Add `multigolem.create.zombie` and `multigolem.heal.zombie`, permissive by default.
- Add marked Zombie Golem spawn eggs and spawner support like existing V4 variants.
