# MultiGolem Identity And Variant Catalog Refactor - Design

**Date:** 2026-05-27
**Authors:** Tyler & Charles
**Status:** Draft, ready for Revue design-spec review
**Predecessors:**

- V1 design: `docs/superpowers/specs/2026-05-15-multigolem-design.md`
- V2 design: `docs/superpowers/specs/2026-05-16-multigolem-v2-design.md`
- V3 design: `docs/superpowers/specs/2026-05-17-multigolem-v3-design.md`
- V3.1 permissions design: `docs/superpowers/specs/2026-05-17-multigolem-permissions-design.md`
- V4 spawn egg implementation: current `main`
- Zombie Golem implementation worktree: `codex/zombie-golem-implementation`

## 1. Summary

MultiGolem needs a shared identity and catalog foundation before more variants are added.

The recent Zombie Golem playtest exposed repeat failure modes: the variant existed, but one registration list forgot the spawn egg, another hard-coded list forgot drops, and targeting needed shared faction logic to avoid killing converted zombie villagers. Redstone, Lapis, tarnished Copper Iron Golems, and future vanilla Copper Golem variants will make those failure modes more likely unless variant data is centralized.

This refactor combines Phase 1 and Phase 2 into one behavior-preserving implementation wave:

- Phase 1: centralize current Iron Golem variant surfaces into shared catalog/set helpers.
- Phase 2: introduce a runtime `GolemIdentity` model with lazy migration from the existing `GolemVariantAttachment`.

The refactor must not change current gameplay, public defaults, or server-owner upgrade steps. Existing worlds should load without manual migration. Existing saved variant attachments remain valid. New identity data is introduced through dual-read and compatibility dual-write, not through a destructive one-way conversion.

Phase 3 is intentionally documented but deferred. It will use the identity model for surface state, tarnished Copper Iron Golems, and eventual vanilla Copper Golem-family support.

## 2. Goals And Non-Goals

**Goals**

- Prevent future variants from needing hand-edited lists in spawn eggs, loot, permissions, assets, docs companion tests, and registration code.
- Keep all current Iron Golem variants behavior-equivalent after the refactor.
- Introduce `GolemFamily`, `GolemIdentity`, and `GolemIdentityAttachment` as the new runtime identity language.
- Preserve existing saved `GolemVariantAttachment` data as a compatibility fallback.
- Lazy-migrate by reading old-only data, returning equivalent identity, and writing new identity data when code next applies or updates identity.
- Dual-write old `GolemVariantAttachment` for Iron-family variants during the compatibility window.
- Make the next Redstone/Lapis implementation and later Copper Golem-family work less likely to miss required surfaces.
- Add tests that fail when a new variant is added without required catalog coverage.

**Non-goals**

- No gameplay balance changes.
- No Redstone, Lapis, tarnished Copper, or vanilla Copper Golem-family implementation in this refactor.
- No removal of `GolemVariantAttachment`.
- No hard migration that rewrites all existing entities on world load.
- No server-owner manual migration step.
- No custom entity type split for current Iron Golem variants.
- No full config-system redesign beyond small testable seams needed for catalog coverage.
- No Phase 3 surface-state persistence yet.

## 3. Current Pain Points

Current code treats `GolemVariant` as both gameplay identity and the complete registration source. That works while every variant is a vanilla `IronGolem`, but it does not scale cleanly.

The strongest repeat bugs and risks are:

- Spawn egg creative-tab registration was a hard-coded non-Iron list.
- Iron Golem loot pools were a separate hard-coded non-Iron list.
- Tests often repeat variant names instead of deriving from required variant sets.
- `TierStats` is a wide nullable record whose constructor is easy to misuse.
- Ability dispatch is concentrated in mixins and will become harder to scan as more variants add hit, tick, targeting, or interaction behavior.
- The word "Copper" is already overloaded: MultiGolem has a Copper-tier Iron Golem, while vanilla has a Copper Golem with weathering states.

The refactor should reduce missed-list bugs now and make the naming model ready for the Copper-family split later.

## 4. Phase 1: Shared Variant Catalog

Phase 1 creates shared variant surfaces without changing saved data.

### 4.1 Variant Sets

Add explicit helpers for the sets that different systems need:

- `GolemVariant.nonIronVariants()`
- `GolemVariant.spawnEggVariants()`
- `GolemVariant.lootVariants()`
- `GolemVariant.multiGolemPlayerBuildableVariants()`
- `VillageSpawnWeights.rollOrder()` remains the explicit village-spawn order because it is a gameplay distribution, not simply "all variants."

These helpers must be derived from catalog metadata, not from renamed hard-coded enum lists. A helper such as `spawnEggVariants()` should filter catalog entries where `spawnEggEnabled == true`; `lootVariants()` should filter entries with mod-owned loot metadata; `multiGolemPlayerBuildableVariants()` should filter entries with mod-owned creation metadata. The catalog is the one place where a new variant declares those surfaces.

Each helper must have a focused test that explains why the set includes or excludes Iron and Zombie.

Default expectations:

- Spawn eggs include every non-Iron Iron-family variant.
- Loot variants include every non-Iron Iron-family variant.
- MultiGolem player-buildable variants include every custom Iron-family variant whose creation is handled by this mod's pattern code. Vanilla Iron remains vanilla-owned and should not be treated as a marked MultiGolem creation result.
- Village roll order remains opt-in and weighted by the existing village spawning config.

### 4.2 Variant Specs

Create a small catalog type for common per-variant metadata. The first version can wrap existing `GolemVariant` data rather than replacing the enum.

The catalog should expose:

- id
- display name
- family, initially always `IRON_GOLEM`
- body block or body-block predicate
- heal item
- drop item
- loot count range
- spawn egg enabled
- entity texture path
- spawn egg model path
- spawn egg texture path
- permission node suffix

The catalog should not own complex ability behavior in Phase 1. Ability-specific code can still live in existing ability classes and mixins.

### 4.3 Registration From Catalog

These systems should derive from catalog/set helpers:

- creative spawn egg registration
- conditional Iron Golem loot pools
- spawn egg stack tests
- asset existence tests
- permission node coverage tests
- future generated docs/playtest companion checks where practical

Direct `List.of(COPPER, GOLD, ...)` usage should be removed from production code unless the list represents a gameplay-specific order.

Every `GolemVariant` enum value must map to exactly one catalog entry, or to an explicit documented exclusion with a reason. Phase 1 should prefer "catalog completeness first": adding a new enum constant without catalog coverage is a test failure before any downstream helper can silently omit it.

### 4.4 Tests

Add coverage tests that fail loudly when a new variant is added but not wired.

Required checks:

- Every `GolemVariant` enum value has exactly one catalog entry, or a documented exclusion.
- Every catalog entry declares its spawn egg, loot, player-buildable, heal, permission, and render-surface intent explicitly.
- Set helpers are derived from catalog metadata and match the expected filtered catalog entries.
- Every `spawnEggVariants()` entry can create a marked spawn egg.
- Every `spawnEggVariants()` entry has an item model and item texture.
- Every renderable non-Iron variant has an entity texture.
- Every `lootVariants()` entry has a loot mapping and non-empty count range.
- Iron cannot be created as a marked spawn egg.
- Iron continues to use vanilla Iron Golem loot only.
- Permission node tests loop over all custom create/heal variants instead of naming each variant manually.

## 5. Phase 2: Golem Identity Model

Phase 2 introduces the identity model that Phase 3 will extend.

### 5.1 GolemFamily

Add a new enum:

```java
public enum GolemFamily {
    IRON_GOLEM("iron_golem");
}
```

Only `IRON_GOLEM` is implemented in Phase 2. `COPPER_GOLEM` is reserved for Phase 3.

The family id must be stable and lower-case because it will become part of saved identity data and marked spawn egg data.

### 5.2 GolemIdentity

Add a value type:

```java
public record GolemIdentity(
    GolemFamily family,
    GolemVariant variant
) {}
```

Phase 2 does not add surface state yet. Current tarnish/weathering ideas should be documented for Phase 3, but the runtime record stays minimal until code needs it.

The identity default is:

```java
new GolemIdentity(GolemFamily.IRON_GOLEM, GolemVariant.IRON)
```

### 5.3 GolemIdentityAttachment

Add a new attachment for identity. It should support:

- read new identity data when present
- fall back to old `GolemVariantAttachment` when identity is absent
- fall back to Iron identity when both are absent
- set identity in one central method
- clear identity only when setting the default Iron identity, matching the current "missing means Iron" uninstall-friendly pattern

For Phase 2, only Iron-family identities are valid. If a saved identity has an unknown family, unknown variant, or a family/variant mismatch, code should log a warning and fall back safely to Iron identity.

### 5.4 Compatibility With GolemVariantAttachment

The existing attachment remains a compatibility surface.

Read behavior:

1. If new identity exists and is valid, return it.
2. Else if old variant exists, return `IRON_GOLEM + oldVariant`.
3. Else return default Iron identity.

Write behavior:

1. New identity-aware code writes `GolemIdentityAttachment`.
2. If identity family is `IRON_GOLEM`, also write the old `GolemVariantAttachment`.
3. If identity is default Iron, clear both identity and old variant attachment where the current code would clear old variant data.

This is lazy migration. Existing worlds require no action from server owners. Old-only golems continue to work. New identity data appears as entities are touched by creation, spawn egg, spawner, village spawn, healing, stats application, or other identity-setting code.

### 5.5 Attachment Call Sites

Phase 2 should start moving reads through identity-aware helpers, but does not need to rewrite every call site at once.

Priority call sites:

- creation handler
- spawn egg application
- spawner marker application
- village spawn application
- loot condition
- stats application
- texture selection
- permissions
- healing
- targeting/faction helpers

For compatibility, `GolemVariantAttachment.get(entity)` must continue to exist in Phase 2 and must derive from identity if identity is present. Existing call sites can then be migrated incrementally without splitting the behavior of identity-aware and legacy readers.

## 6. Runtime Data Flow

### 6.1 Player-Built Creation

Creation resolves a body block to a catalog entry or identity:

1. inspect T-pattern body block
2. resolve to `GolemIdentity(IRON_GOLEM, variant)`
3. check create permission from identity/catalog
4. spawn vanilla `IronGolem`
5. set identity through `GolemIdentityAttachment.set(...)`
6. dual-write old `GolemVariantAttachment` for Iron-family compatibility
7. apply stats

Current behavior must remain unchanged for Copper, Iron, Gold, Emerald, Diamond, Netherite, and Zombie.

### 6.2 Spawn Eggs And Spawners

Phase 2 keeps existing marked Iron Golem spawn eggs compatible.

Current marker:

```snbt
{multigolem:{variant:"diamond"}}
```

Phase 2 may continue writing the current marker for Iron-family spawn eggs, but the reader contract is identity-shaped:

```snbt
{multigolem:{family:"iron_golem", variant:"diamond"}}
```

Phase 2 reader behavior:

- If only `variant` is present, treat it as `family:"iron_golem"` for backward compatibility.
- If `family:"iron_golem"` and a valid Iron-family `variant` are present, accept the identity.
- If `family` is present but is not recognized by Phase 2, reject the marker and do not apply a variant.
- If `family` is recognized but the variant is unknown or invalid for that family, reject the marker and do not apply a variant.
- Rejected markers should log a warning or debug-level diagnostic according to the existing item/spawn marker style, but must not crash world load or item use.

### 6.3 Loot

Loot registration should use catalog entries:

- Iron remains vanilla-owned and has no added conditional pool.
- Non-Iron Iron-family variants add conditional pools based on identity/variant.
- Zombie drops Rotten Flesh by catalog loot data.

The loot condition can continue to accept `GolemVariant` in Phase 2, but internally it should be identity-aware or use a compatibility helper so Phase 3 can add family-specific conditions.

### 6.4 Rendering

Current texture selection remains `IronGolem` renderer texture replacement.

Phase 2 should move path construction to catalog metadata:

```text
textures/entity/<family>/<variant>.png
```

or keep the current flat path while adding a catalog helper that can later route to family folders. The important part is that render paths come from one place.

Phase 3 will decide the final family/state texture layout.

## 7. Lazy Migration Details

Lazy migration must be non-destructive.

Required cases:

| Saved data | Result |
|---|---|
| no identity, no old variant | `IRON_GOLEM + IRON` |
| no identity, old `diamond` variant | `IRON_GOLEM + DIAMOND` |
| new `IRON_GOLEM + ZOMBIE`, old absent | `IRON_GOLEM + ZOMBIE` |
| new `IRON_GOLEM + GOLD`, old `DIAMOND` | prefer new `IRON_GOLEM + GOLD` and leave old data untouched until next set |
| invalid new identity, old `EMERALD` | warn and fall back to `IRON_GOLEM + EMERALD` |
| invalid new identity, no old variant | warn and fall back to `IRON_GOLEM + IRON` |

No code should delete old variant data merely because a new identity was read. Deletion happens only through explicit identity-setting semantics.

An explicit identity set means code intentionally calls the central `GolemIdentityAttachment.set(...)` or equivalent mutation method with a resolved identity. Passive reads, healing checks, stats lookups, texture selection, permission checks, loot checks, and targeting/faction checks are not explicit sets and must not clear old data. Creation, spawn egg application, spawner marker application, village spawn application, and any future administrative identity-changing action are explicit sets.

Setting default Iron identity through the central mutation path clears both attachments only when the caller is intentionally applying default Iron as the entity identity. It must not happen as a side effect of merely reading an old-only or invalid identity fallback.

## 8. Phase 3 Direction

Phase 3 should be designed after Phase 1+2 land and pass review.

Expected Phase 3 scope:

- Add `COPPER_GOLEM` to `GolemFamily`.
- Extend `GolemIdentity` with optional surface state.
- Model tarnish/weathering as state, not as many separate variants.
- Support Copper Iron Golems with weathered/waxed body-block state.
- Support vanilla Copper Golem-family variants.
- Add family-aware spawn egg markers.
- Add family/state-aware texture paths.
- Add family-specific adapter hooks instead of one universal mixin.

The likely Phase 3 surface model:

```java
public record GolemSurfaceState(
    WeatheringStage weatheringStage,
    boolean waxed
) {}
```

This lets these identities coexist cleanly:

```text
family=iron_golem, variant=copper, surface=oxidized+waxed
family=copper_golem, variant=vanilla_copper, surface=oxidized+waxed
```

Phase 3 should not be smuggled into Phase 1+2.

## 9. Testing And Verification

The implementation plan must use TDD for behavior changes and migration compatibility.

Required test groups:

- `GolemVariantCatalogTest`
- `GolemIdentityTest`
- `GolemIdentityAttachmentTest`
- `SpawnEggStacksTest`
- `SpawnerVariantMarkerTest`
- `HasGolemVariantLootConditionTest` or equivalent loot condition unit coverage
- `GolemTextureSelectorTest` or asset existence gate
- existing config, permission, creation, and ability tests

Required migration tests:

- old-only variant attachment resolves to Iron-family identity
- new-only identity resolves correctly
- new identity wins over conflicting old variant
- invalid new identity falls back to valid old variant
- passive reads of missing/default identity do not clear old data
- passive reads of invalid new identity do not clear old fallback data
- default Iron identity clears attachments according to existing uninstall-friendly behavior
- Iron-family identity writes both new identity and old variant compatibility data
- healing, stats, texture, permission, loot, and targeting/faction reads do not mutate identity data
- creation, spawn egg, spawner marker, and village spawn paths call the central identity mutation path

Required verification commands:

```powershell
git rev-parse --show-toplevel
python scripts/check-zombie-golem-planning-handoff.py
python scripts/test-check-zombie-golem-planning-handoff.py
.\gradlew.bat --quiet test
.\gradlew.bat build
```

The implementation plan may narrow test commands during TDD loops, but closeout must run the full build.

## 10. Rollout And Review Plan

This refactor should happen after the current Zombie Golem playtest fixes are stable and before adding Redstone/Lapis or Copper Golem-family work.

Recommended sequence:

1. Write and review this design spec.
2. Send the design spec through Revue as a `design-spec-review`.
3. Action findings.
4. Write an implementation plan for Phase 1+2 only.
5. Send the implementation plan through Revue as an `implementation-plan-review`.
6. Implement Phase 1+2 in the assigned worktree with TDD.
7. Send implementation through Revue as an `implementation-review`.
8. Only after Phase 1+2 is merged, create a Phase 3 design for tarnish/weathering and Copper Golem family support.

The parent checkout must remain untouched during worktree execution. Before any implementation edits, run:

```powershell
git rev-parse --show-toplevel
```

and confirm the root is the assigned worktree.

## 11. Acceptance Criteria

- Existing Iron-family variants behave the same after the refactor.
- Existing old `GolemVariantAttachment` data continues to load.
- New identity reads prefer new identity data but safely fall back to old variant data.
- Iron-family identity writes preserve old attachment compatibility.
- Passive reads never lazy-delete old variant data.
- Default Iron clear behavior occurs only through explicit identity mutation.
- Phase 2 spawn egg marker reading validates unexpected family and variant tags without crashing.
- `GolemVariantAttachment.get(entity)` remains available as a Phase 2 compatibility shim over identity.
- Spawn egg registration and loot registration are catalog/set-driven.
- Tests fail when a new variant lacks required spawn egg, loot, permission, or asset coverage.
- No server-owner manual migration is required.
- Phase 3 Copper Golem-family and tarnish/weathering work has a clear next-step model but is not implemented in this phase.
