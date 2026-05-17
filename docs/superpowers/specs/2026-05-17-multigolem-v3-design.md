# MultiGolem V3 — Design

**Date:** 2026-05-17
**Authors:** Tyler & Charles
**Status:** Draft, awaiting external Claude review
**Predecessors:**

- V1 design: `docs/superpowers/specs/2026-05-15-multigolem-design.md`
- V2 design: `docs/superpowers/specs/2026-05-16-multigolem-v2-design.md`
- Mojang/Fabric target notes: `docs/26.1.2-mojang-targets.md`

## 1. Summary

V3 adds configurable village natural-spawn weighting for MultiGolem variants.

When villagers naturally call an iron golem defender, MultiGolem rolls a configured weighted table. The result can be Iron, Copper, Gold, Emerald, Diamond, or Netherite. Non-iron results use the existing V1/V2 attachment-driven system, so village-spawned variants get the same stats, textures, drops, healing, abilities, ignored-target behavior, and `anger_on_hit` behavior as player-built variants.

V3 is server-side gameplay work only. It adds no items, blocks, textures, entity types, attachments, or client-only code.

## 2. Goals And Non-Goals

**Goals**

- Let villager-called iron golems roll into MultiGolem variants.
- Keep the normal vanilla village spawn rules and position-finding behavior.
- Make the chance table configurable in `config/multigolem.json`.
- Keep Iron as a valid weighted result.
- Treat village-spawned variants as natural village defenders, not player-built golems.
- Preserve the existing V2 behavior surface for any variant produced by V3.
- Add clear playtest rows for village spawns, mob spawners, and command-spawned iron golems.

**Non-goals**

- No retroactive conversion of existing golems.
- No changes to player-built T-pattern golem creation.
- No rolling for mob spawners, spawn eggs, `/summon`, or other command-created golems.
- No rolling for other mods that create iron golems outside the villager help-call path.
- No new client textures, items, blocks, spawn eggs, or custom entity types.
- No per-village memory, pity timer, biome table, or structure-specific weighting.

## 3. Scope

V3 affects only iron golems created by the vanilla villager help-call mechanic. In Minecraft 26.1.2 source inspection, the expected path is `Villager#spawnGolemIfNeeded(...)`, which calls:

```java
SpawnUtil.trySpawnMob(
    EntityType.IRON_GOLEM,
    EntitySpawnReason.MOB_SUMMONED,
    level,
    this.blockPosition(),
    10,
    8,
    6,
    SpawnUtil.Strategy.LEGACY_IRON_GOLEM,
    false
)
```

The implementation plan must still start with a source spike to prove the exact hook and the safest way to capture the newly spawned golem. If the spike cannot safely identify the created entity from the villager call path, implementation pauses for design review instead of widening the hook silently.

## 4. Architecture

Foundation stays the same as V1 and V2:

- Every variant is still a vanilla `IronGolem` entity.
- `GolemVariantAttachment` remains the source of truth for variant identity.
- `VariantAttributes.apply(...)` remains responsible for max health, attack damage, and Gold movement speed modifiers.
- V2 ability handlers continue to key off `GolemVariantAttachment.get(golem)`.
- The client renderer continues to read synced variant attachments for textures.

V3 should add only a small village-spawn layer:

| Component | Expected path | Job |
|---|---|---|
| `VillageSpawnWeights` | `src/main/java/dev/charles/multigolem/spawn/VillageSpawnWeights.java` | Pure helper for weighted variant rolls and disabled/all-zero behavior. |
| `VillageGolemSpawnHandler` | `src/main/java/dev/charles/multigolem/spawn/VillageGolemSpawnHandler.java` | Applies the V3 roll to a newly created villager-called `IronGolem`. |
| `VillagerMixin` or equivalent | `src/main/java/dev/charles/multigolem/mixin/...` | Narrow hook into the spike-confirmed villager spawn point. |
| Config additions | `MultiGolemConfig` and companion record/helper | Loads, validates, merges, and writes `village_spawning`. |

The mixin should stay thin. It should prove vanilla successfully spawned a golem, hand that golem to `VillageGolemSpawnHandler`, and return control to vanilla.

## 5. Config

V3 adds one new top-level config object:

```json
"village_spawning": {
  "enabled": true,
  "weights": {
    "iron": 19,
    "copper": 19,
    "gold": 19,
    "emerald": 19,
    "diamond": 5,
    "netherite": 2
  }
}
```

### 5.1 Default Weights

Charles's V3 default is:

| Variant | Weight |
|---|---:|
| Iron | 19 |
| Copper | 19 |
| Gold | 19 |
| Emerald | 19 |
| Diamond | 5 |
| Netherite | 2 |

In plain terms: Iron, Copper, Gold, and Emerald are about equally likely. Diamond is rare. Netherite is rarer.

Weights are integer tickets in a hat. They do not need to total 100.

### 5.2 Validation Rules

- `enabled: false` disables village variant rolling completely.
- If all recognized variant weights are intentionally `0`, village variant rolling is disabled and the golem stays Iron.
- Missing `village_spawning` is filled from defaults during the existing lossless config merge.
- Missing, non-object, or malformed `weights` falls back to V3 defaults and logs a warning.
- Negative weights are clamped to `0` and log a warning.
- Non-numeric weights fall back to that variant's default and log a warning.
- Unknown keys under `weights` are preserved on disk where possible, ignored by the resolver, and log a warning.
- Existing V2 fields and unknown user fields must remain preserved by the raw `JsonObject` merge/canonicalize style introduced in V2.

## 6. Data Flow

1. Villagers run the vanilla golem-spawn decision.
2. Vanilla confirms enough nearby villagers want a golem.
3. Vanilla calls its normal `SpawnUtil.trySpawnMob(EntityType.IRON_GOLEM, EntitySpawnReason.MOB_SUMMONED, ...)` path.
4. If vanilla fails to spawn a golem, MultiGolem does nothing.
5. If vanilla succeeds, MultiGolem identifies the newly spawned `IronGolem`.
6. If `village_spawning.enabled` is false, MultiGolem leaves it Iron.
7. If all recognized weights are zero, MultiGolem leaves it Iron.
8. Otherwise, MultiGolem rolls `VillageSpawnWeights`.
9. If the roll picks Iron, MultiGolem leaves the golem as normal Iron.
10. If the roll picks a non-iron variant, MultiGolem calls `GolemVariantAttachment.set(golem, variant)`.
11. MultiGolem sets the golem's current health to that variant's configured max health so it starts full.
12. MultiGolem does not call `setPlayerCreated(true)`. Village variants remain natural defenders.

Each successful villager-called spawn rolls independently. There is no per-village memory and no balancing based on previous rolls.

## 7. Behavior Details

### 7.1 Full V2 Behavior

A village-spawned non-iron variant must behave the same as a player-built variant of the same tier:

- Configured max health and attack damage.
- Gold speed modifier.
- Client texture sync and rendering.
- Matching healing item.
- Variant-specific drops.
- Copper, Gold, Emerald, Diamond, and Netherite abilities.
- `ignored_target_types`.
- `anger_on_hit`.

V3 must not create a weaker "spawn-only" variant path.

### 7.2 Natural Defender Status

Village-spawned variants are natural village defenders. They stay `playerCreated=false`.

This keeps them distinct from player-built T-pattern golems, which are created through `GolemCreationHandler` and call `setPlayerCreated(true)`.

### 7.3 Existing Golems

Existing golems are unchanged. V3 only acts at the moment a new villager-called golem is created.

### 7.4 Spawners And Commands

Mob spawners and command-created iron golems do not roll V3 weights.

This is intentional. Spawners and commands are admin/mapmaker tools, and their behavior should stay predictable. If a spawner or command creates a plain `minecraft:iron_golem` with no MultiGolem attachment, MultiGolem treats it as Iron.

## 8. Source Spike Requirements

The V3 implementation plan starts with a source spike and updates `docs/26.1.2-mojang-targets.md`.

The spike must prove:

- Exact class and method for villager-called golem spawning in Minecraft 26.1.2.
- Exact signature and descriptor for the chosen mixin target.
- How the hook identifies the newly spawned `IronGolem`.
- Whether the hook can remain inside the villager help-call path.
- Whether any Fabric event can safely express this without catching mob spawners or commands.
- Spawn reason observed for villager-called golems.
- Whether `playerCreated` remains false.

If the only viable hook is broad enough to catch spawners, commands, spawn eggs, or unrelated mod-created golems, pause for review before implementing.

## 9. Error Handling

- Config load errors follow V2's pattern: warn, repair known bad V3 fields where safe, and keep the server running.
- Malformed `weights` falls back to defaults with a warning.
- All-zero weights are treated as intentional disablement, not a warning.
- A failed variant application logs an error and leaves the spawned golem as Iron rather than crashing the server.
- A mixin target failure should fail fast at startup with a clear Mixin error, matching the existing required-mixin posture.

## 10. Testing

### 10.1 Unit Tests

Pure Java gets failing tests first:

- `VillageSpawnWeightsTest`
  - Default weights are `19/19/19/19/5/2`.
  - Deterministic random can roll each expected variant.
  - Each successful call rolls independently.
  - All-zero recognized weights reports disabled/no result.
  - Negative weights clamp to zero.
  - Malformed/missing weights falls back to defaults through config parsing.

- `MultiGolemConfigV3Test`
  - V2 config migrates to include `village_spawning`.
  - Existing V2 fields are preserved.
  - Unknown user fields are preserved.
  - `enabled: false` parses correctly.
  - All-zero weights parse as intentional disablement.
  - Missing/non-object/malformed weights fall back to defaults and warn.
  - Unknown variant weight keys are preserved but ignored.

- Resolver/helper tests
  - The code path used by the mixin can ask whether village spawning is enabled.
  - The code path can roll a variant or report "leave Iron" without touching Minecraft runtime classes.

### 10.2 Build And Smoke

Mixin/Fabric behavior is verified by build plus focused in-game smoke:

- Build passes.
- Server starts without mixin errors.
- Villager-called golem spawn can produce each configured variant when weights are forced one-at-a-time.
- Village-spawned non-iron variant starts at configured full health.
- Village-spawned variant has its V2 behavior.
- Village-spawned variant is not player-created.

### 10.3 Manual Playtest

Extend both:

- `docs/playtest-checklist.md`
- `docs/playtest.html`

V3 rows:

- Villagers can spawn Copper, Gold, Emerald, Diamond, and Netherite when each weight is forced to `1` and all others are `0`.
- Default weights allow Iron, Copper, Gold, Emerald, Diamond, and Netherite outcomes over repeated village spawns.
- Diamond appears rarely under defaults.
- Netherite appears rarer than Diamond under defaults.
- Village-spawned variants have full V2 behavior: texture, stats, abilities, healing, drops, `ignored_target_types`, and `anger_on_hit`.
- Village-spawned variants are natural defenders, not player-created golems.
- Existing golems do not change after upgrading to V3.
- `village_spawning.enabled: false` leaves villager-called spawns as Iron.
- All-zero weights leave villager-called spawns as Iron.
- Mob spawner iron golems do not roll variants.
- Command-spawned iron golems do not roll variants.
- Malformed `weights` falls back to defaults and logs a warning.

## 11. Release Notes Shape

The V3 changelog should speak to players and server admins, not describe internal implementation tasks.

Expected player/admin-facing summary:

- Villages can now naturally spawn MultiGolem variants as defenders.
- Server owners can configure the village spawn weights.
- Iron, Copper, Gold, and Emerald are common by default; Diamond and Netherite are rare.
- Existing golems, player-built golems, spawners, and command-spawned golems are unchanged.

## 12. Review Notes

### 12.1 Decisions From Tyler And Charles

- V3 rolling happens only when villagers call for help.
- Default weights are Iron 19, Copper 19, Gold 19, Emerald 19, Diamond 5, Netherite 2.
- All-zero weights intentionally disable village variant rolling.
- Mangled/missing weights fall back to defaults with a server-log warning.
- Village-spawned variants are natural village defenders.
- Existing golems are not converted.
- Each successful villager-called spawn rolls independently.
- Iron remains a valid weighted outcome.
- Village-spawned variants get full current V2 behavior.
- Existing `anger_on_hit` still applies.
- Spawner and command-created iron golems should get explicit negative playtest rows.
- The HTML checklist at `docs/playtest.html` should be updated along with the Markdown checklist.

### 12.2 External Claude Review

V1 and V2 both benefited from external review before implementation planning. V3 should do the same before writing the implementation plan.

The review should focus on:

- Whether the villager spawn hook is too broad or too fragile.
- Whether the config edge cases are explicit enough.
- Whether all-zero weights and malformed weights are clearly distinct.
- Whether spawners, commands, spawn eggs, and other mod-created golems are safely excluded.
- Whether tests are strong enough to catch accidental broad hooks.

## 13. Open Items For Implementation Planning

- Exact mixin target and capture strategy for the newly spawned `IronGolem`.
- Whether a tiny accessor/wrapper is needed around `SpawnUtil.trySpawnMob` return value.
- Exact shape of the V3 config record/helper.
- Whether to leave Iron rolls with no attachment or explicitly attach `GolemVariant.IRON`. Preferred default: leave no attachment unless implementation simplicity argues otherwise, because no attachment already means Iron.

---

*End of design. External Claude review is the next gate before writing the V3 implementation plan.*
