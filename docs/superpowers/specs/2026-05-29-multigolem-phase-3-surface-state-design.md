# MultiGolem Phase 3 Surface State Design

**Date:** 2026-05-29
**Authors:** Tyler & Charles
**Status:** Draft, ready for Revue design-spec review
**Baseline:** `46eca52 refactor: add identity catalog baseline`
**Branch:** `codex/phase-3-surface-state`

## 1. Summary

Phase 3 extends the Phase 1+2 identity model with an optional surface state. The first implementation slice should support tarnished and waxed Copper Iron Golems while preserving all current Iron-family behavior.

Surface state is not a new variant. A weathered Copper Iron Golem is still:

```text
family = iron_golem
variant = copper
surface = oxidized or waxed_oxidized
```

This keeps `GolemVariant.COPPER` as the gameplay variant and lets later vanilla Copper Golem-family support use the same surface model without overloading the Iron-family variant enum.

Phase 3 should add `GolemFamily.COPPER_GOLEM` as a reserved family id and design-compatible type surface, but the first implementation slice must not change vanilla Copper Golem behavior, create new Copper Golem-family variants, add Redstone or Lapis, publish a public release, upload marketplace files, or tag a release.

## 2. Goals And Non-Goals

**Goals**

- Add a durable Java type for weathering and waxed state.
- Extend `GolemIdentity` with optional surface state while keeping old saved identities valid.
- Resolve Copper Iron Golem T-pattern body blocks into the correct initial surface state.
- Let spawn eggs, spawners, village spawning, rendering, drops, healing, stats, targeting, and permissions carry or ignore surface state deliberately.
- Preserve current behavior for Iron, Gold, Emerald, Diamond, Netherite, and Zombie.
- Reserve a clean `GolemFamily.COPPER_GOLEM` direction for future vanilla Copper Golem-family work.
- Define tests that prove Phase 1+2 compatibility and prevent surface-state regressions.

**Non-goals**

- No runtime Phase 3 implementation in this design pass.
- No Redstone or Lapis design or implementation.
- No custom entity type split for Iron-family variants.
- No weathering tick simulation for Copper Iron Golems in the first slice.
- No behavior changes to vanilla single-block Copper Golems.
- No public release prep, Modrinth upload, CurseForge upload, or release tag.

## 3. Java Model

### 3.1 Weathering Stage

Add a new enum:

```java
public enum GolemWeatheringStage {
    UNAFFECTED("unaffected"),
    EXPOSED("exposed"),
    WEATHERED("weathered"),
    OXIDIZED("oxidized")
}
```

The ids are lower-case saved ids. They match vanilla copper weathering language and are stable for NBT, item marker, and stream serialization.

### 3.2 Surface State

Add a new value type:

```java
public record GolemSurfaceState(
    GolemWeatheringStage weatheringStage,
    boolean waxed
) {
    public static final GolemSurfaceState DEFAULT =
        new GolemSurfaceState(GolemWeatheringStage.UNAFFECTED, false);

    public boolean isDefault() {
        return DEFAULT.equals(this);
    }
}
```

The first slice supports all four weathering stages and both wax states:

- `unaffected`
- `exposed`
- `weathered`
- `oxidized`
- `unaffected + waxed`
- `exposed + waxed`
- `weathered + waxed`
- `oxidized + waxed`

Waxed state is represented only by the `boolean waxed`; it is not encoded into the enum and not modeled as separate variants.

### 3.3 Golem Identity

Extend `GolemIdentity` to:

```java
public record GolemIdentity(
    GolemFamily family,
    GolemVariant variant,
    Optional<GolemSurfaceState> surfaceState
) {}
```

Surface state is optional rather than represented by a default value on every identity. `Optional.empty()` means the identity has no meaningful surface state. This keeps existing Iron-family variants small and prevents non-Copper golems from silently acquiring a meaningless default surface.

Use helper constructors to keep current call sites readable:

```java
public static GolemIdentity defaultIron();
public static GolemIdentity ofIronVariant(GolemVariant variant);
public static GolemIdentity ofIronVariant(GolemVariant variant, GolemSurfaceState surfaceState);
```

The compatibility meaning of `defaultIron()` remains unchanged:

```text
family = iron_golem
variant = iron
surfaceState = empty
```

### 3.4 Family Validity

Extend `GolemFamily` with:

```java
COPPER_GOLEM("copper_golem")
```

The first Phase 3 implementation slice must parse and reject unsupported Copper-family identities safely, falling back through the lazy migration path, but it must not instantiate, mutate, render, or behavior-patch vanilla Copper Golems unless a later approved plan asks for that.

Validity rules:

- `IRON_GOLEM + IRON` through `IRON_GOLEM + ZOMBIE` are valid.
- `IRON_GOLEM + COPPER + surfaceState` is valid.
- `IRON_GOLEM + non-COPPER + surfaceState` is invalid unless a future design explicitly grants that variant a surface.
- `COPPER_GOLEM + any current GolemVariant` is reserved and not valid for first-slice runtime behavior.
- Unknown family, unknown variant, invalid family/variant pairing, or invalid surface data falls back through the existing lazy migration path.

## 4. Body Block To Surface Mapping

The Copper Iron Golem T-pattern keeps the existing MultiGolem T-shape and stays distinct from vanilla's single-block Copper Golem pillar.

For this phase, "copper-family blocks" means the same creation scope as current `GolemVariant.COPPER`: blocks that satisfy `BlockTags.COPPER` or are vanilla `WeatheringCopper` blocks after wax-off normalization through `HoneycombItem.WAX_OFF_BY_BLOCK`. This includes weathered and waxed full copper blocks and any other vanilla copper-tagged blocks the current Copper T-pattern already accepts; it does not expand the pattern to unrelated decorative blocks outside that tag/helper contract.

When the four body blocks in the T-pattern are all copper-family blocks, resolve `GolemVariant.COPPER` and derive the initial surface state from the matched body blocks:

| Body blocks | Initial surface |
| --- | --- |
| All unwaxed copper blocks | matching weathering stage, `waxed=false` |
| All waxed copper blocks | matching weathering stage, `waxed=true` |
| Mixed waxed and unwaxed but same weathering stage | matching weathering stage, `waxed=true` if every block is waxed, otherwise `waxed=false` |
| Mixed weathering stages | use the most oxidized stage present; `waxed=true` only if every body block is waxed |

Oxidation order matches the enum declaration: `UNAFFECTED < EXPOSED < WEATHERED < OXIDIZED`. "Most oxidized" means the highest stage in that order.

This rule gives deterministic behavior for imperfect player builds and avoids rejecting patterns that vanilla copper tags already consider part of the copper block family.

For non-Copper body materials, creation continues to resolve `GolemIdentity.ofIronVariant(variant)` with no surface state.

## 5. Persistence And Migration

Existing saved identity data from Phase 1+2 has only `family` and `variant`. It must decode as the same identity with `surfaceState = Optional.empty()`.

Read behavior:

1. If new identity exists, is valid, and either has no surface state or has a surface state allowed for that family/variant, return it.
2. If the new identity uses `COPPER_GOLEM`, treat it as a recognized but unsupported reserved family in the first slice. Do not log it as corrupt; fall through to old variant data when present or default Iron when absent.
3. Else if old variant exists, return `IRON_GOLEM + oldVariant + empty surfaceState`.
4. Else return `defaultIron()`.

Write behavior:

1. New identity-aware code writes the extended `GolemIdentityAttachment`.
2. Iron-family identities continue to dual-write the old `GolemVariantAttachment`.
3. The old attachment stores only the variant. It never stores surface state.
4. Setting default Iron through the central mutation path clears both attachments as before.

No passive read may rewrite, normalize, or delete old-only data. Creation, spawn egg application, spawner marker application, village spawning, and future admin identity-changing actions are explicit mutation paths.

Codecs should accept missing `surfaceState` as empty. Malformed surface state should invalidate only the new identity and fall back to old variant data when present.

## 6. Propagation By System

### 6.1 Player-Built Golems

Creation is the first required surface-aware mutation path.

The creation handler resolves the T-pattern into:

```text
GolemIdentity(IRON_GOLEM, COPPER, Optional.of(surfaceState))
```

only for Copper Iron Golems. It checks `multigolem.create.copper` exactly as today. No new permission node is required for weathered or waxed forms.

### 6.2 Spawn Eggs

Marked Iron Golem spawn eggs should continue to write and read current variant-only markers for non-surface variants.

Surface-bearing Copper Iron Golem eggs should use an identity-shaped marker:

```snbt
{multigolem:{family:"iron_golem",variant:"copper",surface:{weathering_stage:"weathered",waxed:true}}}
```

Compatibility rules:

- Old `{multigolem:{variant:"copper"}}` reads as Copper Iron Golem with empty surface state.
- Empty surface state renders and behaves as the current Copper Iron Golem.
- Unknown or unsupported surface data makes the marker read empty rather than crashing or creating the wrong golem.

The first implementation slice does not need to add separate creative-tab entries for every weathered and waxed Copper Iron Golem unless explicitly approved later. The smallest useful slice can keep one Copper spawn egg and use surface-state markers only when a command, test seam, or future UI creates such a stack.

### 6.3 Spawners

Spawner markers mirror spawn egg identity markers. The existing spawner preview and server-spawn paths must propagate the full identity, including surface state, to both preview entity and actual spawned entity.

If a spawner has an old variant-only marker, it remains surface-empty and behaves like today's Copper Iron Golem.

### 6.4 Village Spawning

Village spawning remains variant-weight based. A Copper roll produces `IRON_GOLEM + COPPER + empty surfaceState` unless a later gameplay plan defines biome, block, or config-based surface selection.

This preserves current village balance and avoids introducing hidden weathering randomness.

### 6.5 Rendering

Rendering should carry `GolemIdentity`, not only `GolemVariant`, into the client render state.

Texture selection keys become:

```text
family + variant + optional surfaceState
```

Recommended texture path shape:

```text
textures/entity/iron_golem/copper_golem.png
textures/entity/iron_golem/copper_golem_exposed.png
textures/entity/iron_golem/copper_golem_weathered.png
textures/entity/iron_golem/copper_golem_oxidized.png
textures/entity/iron_golem/copper_golem_waxed.png
textures/entity/iron_golem/copper_golem_waxed_exposed.png
textures/entity/iron_golem/copper_golem_waxed_weathered.png
textures/entity/iron_golem/copper_golem_waxed_oxidized.png
```

The implementation may keep existing flat paths for non-surface variants during the first slice, but Copper surface textures must be selected from a single catalog/helper so future family-specific texture folders do not require renderer rewrites.

Missing texture fallback should be the current Copper Iron Golem texture, not vanilla Iron. In the recommended folder layout, that fallback is `textures/entity/iron_golem/copper_golem.png`, the unaffected and unwaxed Copper texture. If the implementation keeps the existing flat layout for the first slice, the equivalent fallback is the current `textures/entity/copper_golem.png` asset.

### 6.6 Drops, Healing, Stats, Targeting, And Permissions

These systems use family and variant only in the first slice:

- Drops: Copper Iron Golems drop Copper ingots regardless of surface state.
- Healing: Copper Iron Golems heal with Copper ingots regardless of surface state.
- Stats and abilities: Copper stats and lightning behavior remain unchanged.
- Targeting/faction: surface state has no targeting meaning.
- Permissions: `multigolem.create.copper` and `multigolem.heal.copper` cover every Copper surface state.

Tests should prove these systems ignore surface state deliberately rather than accidentally losing identity.

## 7. Future Vanilla Copper Golem-Family Direction

`GolemFamily.COPPER_GOLEM` exists to prevent the name collision between MultiGolem's Copper Iron Golem and vanilla's Copper Golem.

Future work may model vanilla Copper Golem-family identities as:

```text
family = copper_golem
variant = copper
surface = exposed + waxed
```

That future phase needs its own design because vanilla Copper Golems have different entity class, model, animation, chest/block behavior, spawn pattern, and likely different hooks. This Phase 3 design only ensures the identity data model will not block that future.

## 8. First Implementation Slice

The smallest useful Phase 3 implementation slice is:

1. Add `GolemWeatheringStage`, `GolemSurfaceState`, and optional surface state on `GolemIdentity`.
2. Keep existing identity helpers source-compatible where possible.
3. Parse old identity data with missing surface as empty.
4. Derive Copper Iron Golem creation surface from body blocks.
5. Carry full identity through spawn egg markers, spawner markers, and render state.
6. Add Copper surface texture selection with safe fallback.
7. Prove drops, healing, permissions, stats, village spawning, and targeting preserve Phase 1+2 behavior.

Out of scope for that first slice:

- Weathering over time.
- Waxing or scraping existing golems after spawn.
- New creative entries for every Copper surface.
- Vanilla Copper Golem-family runtime support.
- New permissions per surface.
- Redstone or Lapis.
- Release publishing.

## 9. Tests

Required compatibility tests:

- Phase 1+2 identity with only `family` and `variant` decodes with empty surface state.
- Old-only `GolemVariantAttachment` resolves to identity with empty surface state and remains non-mutating.
- New identity wins over conflicting old variant without deleting old data on passive read.
- Default Iron still clears both attachments only through explicit mutation.
- Non-Copper Iron-family variants reject or sanitize non-empty surface state.
- Invalid surface state falls back to old variant data when old data exists.

Required surface tests:

- Every supported copper body block maps to the expected weathering stage and wax state.
- Mixed weathering body blocks choose the most oxidized stage.
- Mixed waxed and unwaxed body blocks resolve as unwaxed unless every block is waxed.
- Copper creation writes identity with surface state.
- Spawn egg and spawner markers round-trip identity-shaped surface data.
- Old variant-only Copper markers still read as Copper with empty surface state.
- Spawner preview and actual spawns receive the same surface identity.
- Texture selector distinguishes family, variant, weathering stage, and waxed state.
- Missing Copper surface texture falls back to current Copper texture.

Required regression tests:

- Iron, Gold, Emerald, Diamond, Netherite, and Zombie render, heal, drop, target, and check permissions as before.
- Village Copper rolls remain surface-empty.
- `GolemVariant.spawnEggVariants()`, `lootVariants()`, and `multiGolemPlayerBuildableVariants()` are unchanged.
- No Redstone or Lapis catalog rows are introduced.

Closeout verification for the design/prep pass remains:

```powershell
git rev-parse --show-toplevel
.\gradlew.bat --quiet test
.\gradlew.bat build
```

## 10. Acceptance Criteria

- The exact surface-state Java types are named and have stable saved ids.
- Surface state is optional on `GolemIdentity`.
- All four weathering stages and waxed/unwaxed forms are supported.
- Existing saved golems without surface state load unchanged.
- Copper Iron Golem body-block state has deterministic initial surface mapping.
- Rendering has a family + variant + surface texture-selection contract.
- Drops, healing, stats, targeting, and permissions deliberately ignore surface state in the first slice.
- Vanilla Copper Golem-family work has a reserved family direction but no runtime implementation.
- First-slice out-of-scope boundaries are explicit.
- Tests cover compatibility, propagation, and Phase 1+2 regressions.
