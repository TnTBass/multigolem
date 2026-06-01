# MultiGolem Phase 3 Surface State Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement optional surface state for Copper Iron Golems while preserving Phase 1+2 behavior for every existing Iron-family variant.

**Architecture:** Extend the existing identity layer from `family + variant` to `family + variant + optional surfaceState`, with strict validation that only Iron-family Copper may carry surface state in this slice. Creation, spawn egg, spawner, and rendering paths carry the full identity; drops, healing, stats, targeting, permissions, village spawning, and catalog sets deliberately continue to use family/variant semantics only.

**Tech Stack:** Java 25, Fabric API attachments, Minecraft 26.1.2 Mojang mappings, Mixin, JUnit 6, Gradle/Loom.

---

## Pre-Edit Gates

- [ ] Run `git rev-parse --show-toplevel`.
  - Expected: `C:/Users/tyler/AI Projects/MultiGolem/.worktrees/phase-3-surface-state`.
- [ ] Run `git status --short --branch`.
  - Expected: branch `codex/phase-3-surface-state`; only planned files are dirty.
- [ ] Re-read `docs/superpowers/specs/2026-05-29-multigolem-phase-3-surface-state-design.md`.
  - Expected: implementation remains the reviewed first slice: surface state for Copper Iron Golems, reserved `COPPER_GOLEM`, no Redstone, no Lapis, no runtime vanilla Copper Golem behavior, no public release work.

## File Map

- Create `src/main/java/dev/charles/multigolem/identity/GolemWeatheringStage.java`
  - Stable lower-case ids, codec, stream codec, and oxidation ordering for `UNAFFECTED`, `EXPOSED`, `WEATHERED`, and `OXIDIZED`.
- Create `src/main/java/dev/charles/multigolem/identity/GolemSurfaceState.java`
  - Value type for `GolemWeatheringStage weatheringStage` plus `boolean waxed`, with `DEFAULT` and validation helpers.
- Modify `src/main/java/dev/charles/multigolem/identity/GolemFamily.java`
  - Add reserved `COPPER_GOLEM("copper_golem")` for parsing and future identity shape only; keep existing `fromId(String)` coverage.
- Modify `src/main/java/dev/charles/multigolem/identity/GolemIdentity.java`
  - Add `Optional<GolemSurfaceState> surfaceState`, helper constructors, codecs, stream serialization, and first-slice validity rules.
- Modify `src/main/java/dev/charles/multigolem/identity/GolemIdentityMigration.java`
  - Preserve lazy migration, accept missing surface as empty, reject unsupported surface/family data through old-variant fallback.
- Modify `src/main/java/dev/charles/multigolem/attachment/GolemIdentityAttachment.java`
  - Continue to delegate read/write rules through `GolemIdentityMigration`.
- Modify `src/main/java/dev/charles/multigolem/GolemVariant.java`
  - Expose Copper-family block normalization seams used by surface derivation without changing existing variant sets; keep existing `fromId(String)` marker-reader coverage.
- Modify `src/main/java/dev/charles/multigolem/catalog/GolemVariantCatalog.java`
  - Keep current catalog sets unchanged; add any Copper surface helper only if it avoids duplicating block logic.
- Create `src/main/java/dev/charles/multigolem/spawn/CopperSurfaceResolver.java`
  - Pure resolver that maps Copper-family `BlockState` values and mixed T-pattern body blocks to `GolemSurfaceState`.
- Modify `src/main/java/dev/charles/multigolem/spawn/GolemCreationHandler.java`
  - For Copper T-patterns only, derive surface state from the four body blocks and write `GolemIdentity.ofIronVariant(GolemVariant.COPPER, surfaceState)`.
- Modify `src/main/java/dev/charles/multigolem/spawn/SpawnEggStacks.java`
  - Read/write identity-shaped surface markers while keeping old variant-only markers compatible.
- Modify `src/main/java/dev/charles/multigolem/spawn/SpawnerVariantMarker.java`
  - Mirror spawn egg identity marker rules and propagate surface through preview and actual spawn.
- Modify `src/main/java/dev/charles/multigolem/spawn/SpawnEggVariantSpawner.java`
  - Apply full identity from marked eggs.
- Modify `src/main/java/dev/charles/multigolem/mixin/BaseSpawnerMixin.java`
  - Preserve existing preview and actual spawn hooks while applying full identity.
- Modify `src/client/java/dev/charles/multigolem/client/render/GolemRenderStateExtension.java`
  - Carry `GolemIdentity` instead of only `GolemVariant`.
- Modify `src/client/java/dev/charles/multigolem/client/mixin/IronGolemRenderStateExtensionMixin.java`
  - Store default Iron identity and full identity setter/getter.
- Modify `src/client/java/dev/charles/multigolem/client/mixin/IronGolemRenderStateMixin.java`
  - Capture `GolemIdentityAttachment.get(entity)` into render state.
- Modify `src/client/java/dev/charles/multigolem/client/mixin/IronGolemRendererMixin.java`
  - Select non-Iron textures by identity.
- Modify `src/client/java/dev/charles/multigolem/client/render/GolemTextureSelector.java`
  - Select Copper surface textures by `family + variant + optional surfaceState`, with current Copper texture fallback.
- Add or modify tests:
  - `src/test/java/dev/charles/multigolem/identity/GolemWeatheringStageTest.java`
  - `src/test/java/dev/charles/multigolem/identity/GolemSurfaceStateTest.java`
  - `src/test/java/dev/charles/multigolem/identity/GolemIdentityTest.java`
  - `src/test/java/dev/charles/multigolem/attachment/GolemIdentityAttachmentTest.java`
  - `src/test/java/dev/charles/multigolem/spawn/CopperSurfaceResolverTest.java`
  - `src/test/java/dev/charles/multigolem/spawn/SpawnEggStacksTest.java`
  - `src/test/java/dev/charles/multigolem/spawn/SpawnerVariantMarkerTest.java`
  - `src/test/java/dev/charles/multigolem/client/render/GolemTextureSelectorTest.java`
  - Existing catalog, creation, village, loot, healing, stats, targeting, permission, and mixin tests listed below.

---

### Task 1: Surface Value Types And Identity Validation

**Files:**
- Create: `src/main/java/dev/charles/multigolem/identity/GolemWeatheringStage.java`
- Create: `src/main/java/dev/charles/multigolem/identity/GolemSurfaceState.java`
- Modify: `src/main/java/dev/charles/multigolem/identity/GolemFamily.java`
- Modify: `src/main/java/dev/charles/multigolem/identity/GolemIdentity.java`
- Test: `src/test/java/dev/charles/multigolem/identity/GolemWeatheringStageTest.java`
- Test: `src/test/java/dev/charles/multigolem/identity/GolemSurfaceStateTest.java`
- Test: `src/test/java/dev/charles/multigolem/identity/GolemIdentityTest.java`

- [ ] **Step 1.1: Write failing pure identity tests**

Add tests equivalent to:

```java
@Test
void weatheringStagesHaveStableIdsAndOrder() {
    assertEquals("unaffected", GolemWeatheringStage.UNAFFECTED.id());
    assertEquals("exposed", GolemWeatheringStage.EXPOSED.id());
    assertEquals("weathered", GolemWeatheringStage.WEATHERED.id());
    assertEquals("oxidized", GolemWeatheringStage.OXIDIZED.id());
    assertTrue(GolemWeatheringStage.OXIDIZED.isAfter(GolemWeatheringStage.WEATHERED));
}

@Test
void surfaceStateDefaultIsUnwaxedUnaffected() {
    assertEquals(new GolemSurfaceState(GolemWeatheringStage.UNAFFECTED, false), GolemSurfaceState.DEFAULT);
    assertTrue(GolemSurfaceState.DEFAULT.isDefault());
    assertFalse(new GolemSurfaceState(GolemWeatheringStage.UNAFFECTED, true).isDefault());
}

@Test
void copperIronIdentityMayCarrySurfaceButOtherIronVariantsMayNot() {
    GolemSurfaceState surface = new GolemSurfaceState(GolemWeatheringStage.OXIDIZED, true);
    assertTrue(GolemIdentity.ofIronVariant(GolemVariant.COPPER, surface).isValidForPhase3());
    assertFalse(new GolemIdentity(GolemFamily.IRON_GOLEM, GolemVariant.GOLD, Optional.of(surface)).isValidForPhase3());
}

@Test
void copperGolemFamilyIsReservedButNotValidForFirstSliceRuntime() {
    assertEquals("copper_golem", GolemFamily.COPPER_GOLEM.id());
    assertFalse(new GolemIdentity(GolemFamily.COPPER_GOLEM, GolemVariant.COPPER, Optional.empty()).isValidForPhase3());
}

@Test
void existingHelpersKeepSurfaceEmpty() {
    assertEquals(Optional.empty(), GolemIdentity.defaultIron().surfaceState());
    assertEquals(Optional.empty(), GolemIdentity.ofIronVariant(GolemVariant.DIAMOND).surfaceState());
}

@Test
void phaseTwoValidityStillMeansNoSurfaceState() {
    GolemIdentity surfaceCopper = GolemIdentity.ofIronVariant(GolemVariant.COPPER,
        new GolemSurfaceState(GolemWeatheringStage.EXPOSED, false));
    assertFalse(surfaceCopper.isValidForPhase2());
    assertTrue(surfaceCopper.isValidForPhase3());
}

@Test
void streamCodecRoundTripsAbsentSurfaceAsAbsent() {
    GolemIdentity decoded = roundTrip(GolemIdentity.STREAM_CODEC, GolemIdentity.ofIronVariant(GolemVariant.DIAMOND));
    assertEquals(Optional.empty(), decoded.surfaceState());
}
```

- [ ] **Step 1.2: Verify RED**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.identity.GolemWeatheringStageTest --tests dev.charles.multigolem.identity.GolemSurfaceStateTest --tests dev.charles.multigolem.identity.GolemIdentityTest
```

Expected: compile failure because `GolemWeatheringStage`, `GolemSurfaceState`, `COPPER_GOLEM`, and the surface-aware `GolemIdentity` shape do not exist yet.

- [ ] **Step 1.3: Implement surface types**

Create `GolemWeatheringStage`:

```java
public enum GolemWeatheringStage {
    UNAFFECTED("unaffected"),
    EXPOSED("exposed"),
    WEATHERED("weathered"),
    OXIDIZED("oxidized");

    public static final Codec<GolemWeatheringStage> CODEC = Codec.STRING.flatXmap(
        id -> fromId(id).map(DataResult::success)
            .orElseGet(() -> DataResult.error(() -> "Unknown GolemWeatheringStage id: " + id)),
        stage -> DataResult.success(stage.id)
    );
    public static final StreamCodec<ByteBuf, GolemWeatheringStage> STREAM_CODEC =
        ByteBufCodecs.STRING_UTF8.map(s -> fromId(s).orElse(UNAFFECTED), GolemWeatheringStage::id);

    private final String id;

    GolemWeatheringStage(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean isAfter(GolemWeatheringStage other) {
        return ordinal() > other.ordinal();
    }

    public static Optional<GolemWeatheringStage> fromId(String id) {
        return Arrays.stream(values()).filter(stage -> stage.id.equals(id)).findFirst();
    }
}
```

Create `GolemSurfaceState`:

```java
public record GolemSurfaceState(GolemWeatheringStage weatheringStage, boolean waxed) {
    public static final GolemSurfaceState DEFAULT =
        new GolemSurfaceState(GolemWeatheringStage.UNAFFECTED, false);

    public static final Codec<GolemSurfaceState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        GolemWeatheringStage.CODEC.fieldOf("weathering_stage").forGetter(GolemSurfaceState::weatheringStage),
        Codec.BOOL.fieldOf("waxed").forGetter(GolemSurfaceState::waxed)
    ).apply(instance, GolemSurfaceState::new));

    public static final StreamCodec<ByteBuf, GolemSurfaceState> STREAM_CODEC = StreamCodec.composite(
        GolemWeatheringStage.STREAM_CODEC, GolemSurfaceState::weatheringStage,
        ByteBufCodecs.BOOL, GolemSurfaceState::waxed,
        GolemSurfaceState::new
    );

    public boolean isDefault() {
        return DEFAULT.equals(this);
    }
}
```

- [ ] **Step 1.4: Extend identity and family**

Change `GolemFamily` to include:

```java
IRON_GOLEM("iron_golem"),
COPPER_GOLEM("copper_golem");
```

Change `GolemIdentity` to:

```java
public record GolemIdentity(
    GolemFamily family,
    GolemVariant variant,
    Optional<GolemSurfaceState> surfaceState
) {
    private static final GolemIdentity DEFAULT_IRON =
        new GolemIdentity(GolemFamily.IRON_GOLEM, GolemVariant.IRON, Optional.empty());

    public static GolemIdentity defaultIron() {
        return DEFAULT_IRON;
    }

    public static GolemIdentity ofIronVariant(GolemVariant variant) {
        return new GolemIdentity(GolemFamily.IRON_GOLEM, variant, Optional.empty());
    }

    public static GolemIdentity ofIronVariant(GolemVariant variant, GolemSurfaceState surfaceState) {
        return new GolemIdentity(GolemFamily.IRON_GOLEM, variant, Optional.of(surfaceState));
    }

    public boolean isDefaultIron() {
        return DEFAULT_IRON.equals(this);
    }

    public boolean isValidForPhase2() {
        return family == GolemFamily.IRON_GOLEM && variant != null && surfaceState.isEmpty();
    }

    public boolean isValidForPhase3() {
        if (family != GolemFamily.IRON_GOLEM || variant == null) return false;
        return surfaceState.isEmpty() || variant == GolemVariant.COPPER;
    }
}
```

Update `CODEC` to use `GolemSurfaceState.CODEC.optionalFieldOf("surface_state", Optional.empty())`. Update `STREAM_CODEC` so absent surface state round-trips as absent. Prefer a composite codec with an explicit surface-present boolean:

```java
public static final StreamCodec<ByteBuf, GolemIdentity> STREAM_CODEC = StreamCodec.composite(
    GolemFamily.STREAM_CODEC, GolemIdentity::family,
    GolemVariant.STREAM_CODEC, GolemIdentity::variant,
    ByteBufCodecs.BOOL, identity -> identity.surfaceState().isPresent(),
    GolemSurfaceState.STREAM_CODEC, identity -> identity.surfaceState().orElse(GolemSurfaceState.DEFAULT),
    (family, variant, hasSurface, surface) ->
        new GolemIdentity(family, variant, hasSurface ? Optional.of(surface) : Optional.empty())
);
```

Do not encode missing surface as an empty weathering-stage id, because that would decode through the stage fallback as `UNAFFECTED` instead of preserving `Optional.empty()`.

- [ ] **Step 1.5: Verify GREEN**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.identity.GolemWeatheringStageTest --tests dev.charles.multigolem.identity.GolemSurfaceStateTest --tests dev.charles.multigolem.identity.GolemIdentityTest
```

Expected: pass.

---

### Task 2: Lazy Migration And Attachment Compatibility

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/identity/GolemIdentityMigration.java`
- Modify: `src/main/java/dev/charles/multigolem/attachment/GolemIdentityAttachment.java`
- Test: `src/test/java/dev/charles/multigolem/attachment/GolemIdentityAttachmentTest.java`

- [ ] **Step 2.1: Write failing migration compatibility tests**

Add tests equivalent to:

```java
@Test
void oldOnlyVariantResolvesWithEmptySurfaceState() {
    MapBackedIdentityStorage storage = oldOnlyStorage(GolemVariant.DIAMOND);
    assertEquals(GolemIdentity.ofIronVariant(GolemVariant.DIAMOND), GolemIdentityMigration.resolve(storage));
    assertEquals(Optional.of(GolemVariant.DIAMOND), storage.rawVariant());
}

@Test
void phaseTwoIdentityWithMissingSurfaceStillResolves() {
    MapBackedIdentityStorage storage = new MapBackedIdentityStorage();
    storage.setRawIdentity(new GolemIdentity(GolemFamily.IRON_GOLEM, GolemVariant.ZOMBIE, Optional.empty()));
    assertEquals(GolemIdentity.ofIronVariant(GolemVariant.ZOMBIE), GolemIdentityMigration.resolve(storage));
}

@Test
void validCopperSurfaceIdentityWinsOverConflictingOldVariant() {
    GolemIdentity copper = GolemIdentity.ofIronVariant(GolemVariant.COPPER,
        new GolemSurfaceState(GolemWeatheringStage.WEATHERED, true));
    MapBackedIdentityStorage storage = oldAndNewStorage(GolemVariant.GOLD, copper);
    assertEquals(copper, GolemIdentityMigration.resolve(storage));
    assertEquals(Optional.of(GolemVariant.GOLD), storage.rawVariant());
}

@Test
void invalidNonCopperSurfaceFallsBackToOldVariantWithoutMutating() {
    GolemSurfaceState surface = new GolemSurfaceState(GolemWeatheringStage.EXPOSED, false);
    MapBackedIdentityStorage storage = oldAndNewStorage(
        GolemVariant.EMERALD,
        new GolemIdentity(GolemFamily.IRON_GOLEM, GolemVariant.GOLD, Optional.of(surface))
    );
    assertEquals(GolemIdentity.ofIronVariant(GolemVariant.EMERALD), GolemIdentityMigration.resolve(storage));
    assertEquals(Optional.of(GolemVariant.EMERALD), storage.rawVariant());
}

@Test
void reservedCopperGolemFamilyFallsBackToOldVariantWithoutBeingTreatedAsRuntimeValid() {
    MapBackedIdentityStorage storage = oldAndNewStorage(
        GolemVariant.NETHERITE,
        new GolemIdentity(GolemFamily.COPPER_GOLEM, GolemVariant.COPPER, Optional.empty())
    );
    assertEquals(GolemIdentity.ofIronVariant(GolemVariant.NETHERITE), GolemIdentityMigration.resolve(storage));
}

@Test
void writingDefaultIronStillClearsBothAttachments() {
    MapBackedIdentityStorage storage = oldAndNewStorage(GolemVariant.DIAMOND,
        GolemIdentity.ofIronVariant(GolemVariant.COPPER, GolemSurfaceState.DEFAULT));
    GolemIdentityMigration.write(storage, GolemIdentity.defaultIron());
    assertTrue(storage.rawIdentity().isEmpty());
    assertTrue(storage.rawVariant().isEmpty());
}

@Test
void writingCopperSurfaceDualWritesOldCopperVariantOnly() {
    MapBackedIdentityStorage storage = new MapBackedIdentityStorage();
    GolemIdentity identity = GolemIdentity.ofIronVariant(GolemVariant.COPPER,
        new GolemSurfaceState(GolemWeatheringStage.OXIDIZED, false));
    GolemIdentityMigration.write(storage, identity);
    assertEquals(Optional.of(identity), storage.rawIdentity());
    assertEquals(Optional.of(GolemVariant.COPPER), storage.rawVariant());
}
```

- [ ] **Step 2.2: Verify RED**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.attachment.GolemIdentityAttachmentTest
```

Expected: compile failures or assertion failures until migration validates Phase 3 identity shape.

- [ ] **Step 2.3: Implement migration rules**

Keep `GolemIdentityAttachment` as a thin adapter. Change `GolemIdentityMigration.resolve(...)` to accept only `identity.isValidForPhase3()` and otherwise fall back to old variant:

```java
public static GolemIdentity resolve(GolemIdentityStorage storage) {
    return storage.rawIdentity()
        .filter(GolemIdentity::isValidForPhase3)
        .orElseGet(() -> storage.rawVariant()
            .map(GolemIdentity::ofIronVariant)
            .orElse(GolemIdentity.defaultIron()));
}
```

Change write validation to reject unsupported Copper Golem-family and non-Copper surface data:

```java
public static void write(GolemIdentityStorage storage, GolemIdentity identity) {
    if (identity == null || !identity.isValidForPhase3() || identity.isDefaultIron()) {
        storage.clearRawIdentity();
        storage.clearRawVariant();
        return;
    }
    storage.setRawIdentity(identity);
    storage.setRawVariant(identity.variant());
}
```

Do not add passive normalization or automatic deletion during reads.

- [ ] **Step 2.4: Verify GREEN**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.attachment.GolemIdentityAttachmentTest --tests dev.charles.multigolem.identity.GolemIdentityTest
```

Expected: pass.

---

### Task 3: Copper Body Block Surface Resolution

**Files:**
- Create: `src/main/java/dev/charles/multigolem/spawn/CopperSurfaceResolver.java`
- Modify: `src/main/java/dev/charles/multigolem/GolemVariant.java`
- Modify: `src/main/java/dev/charles/multigolem/spawn/GolemCreationHandler.java`
- Test: `src/test/java/dev/charles/multigolem/spawn/CopperSurfaceResolverTest.java`

- [ ] **Step 3.1: Write failing resolver tests**

Add pure tests equivalent to:

```java
@ParameterizedTest
@MethodSource("singleCopperBlocks")
void singleCopperFamilyBlockMapsToExpectedSurface(Block block, GolemWeatheringStage stage, boolean waxed) {
    assertEquals(Optional.of(new GolemSurfaceState(stage, waxed)),
        CopperSurfaceResolver.surfaceFor(block.defaultBlockState()));
}

@Test
void mixedWeatheringUsesMostOxidizedStage() {
    List<BlockState> body = List.of(
        Blocks.COPPER_BLOCK.defaultBlockState(),
        Blocks.EXPOSED_COPPER.defaultBlockState(),
        Blocks.WEATHERED_COPPER.defaultBlockState(),
        Blocks.OXIDIZED_COPPER.defaultBlockState()
    );
    assertEquals(new GolemSurfaceState(GolemWeatheringStage.OXIDIZED, false),
        CopperSurfaceResolver.resolveBody(body).orElseThrow());
}

@Test
void mixedWaxedAndUnwaxedIsUnwaxedUnlessEveryBlockIsWaxed() {
    assertEquals(new GolemSurfaceState(GolemWeatheringStage.WEATHERED, false),
        CopperSurfaceResolver.resolveBody(List.of(
            Blocks.WAXED_WEATHERED_COPPER.defaultBlockState(),
            Blocks.WEATHERED_COPPER.defaultBlockState(),
            Blocks.WAXED_WEATHERED_COPPER.defaultBlockState(),
            Blocks.WEATHERED_COPPER.defaultBlockState()
        )).orElseThrow());

    assertEquals(new GolemSurfaceState(GolemWeatheringStage.WEATHERED, true),
        CopperSurfaceResolver.resolveBody(List.of(
            Blocks.WAXED_WEATHERED_COPPER.defaultBlockState(),
            Blocks.WAXED_WEATHERED_COPPER.defaultBlockState(),
            Blocks.WAXED_WEATHERED_COPPER.defaultBlockState(),
            Blocks.WAXED_WEATHERED_COPPER.defaultBlockState()
        )).orElseThrow());
}

@Test
void nonCopperBodyBlockHasNoSurface() {
    assertTrue(CopperSurfaceResolver.surfaceFor(Blocks.GOLD_BLOCK.defaultBlockState()).isEmpty());
}
```

Include full copper block cases for `COPPER_BLOCK`, `EXPOSED_COPPER`, `WEATHERED_COPPER`, `OXIDIZED_COPPER`, `WAXED_COPPER_BLOCK`, `WAXED_EXPOSED_COPPER`, `WAXED_WEATHERED_COPPER`, and `WAXED_OXIDIZED_COPPER`.

- [ ] **Step 3.2: Verify RED**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.CopperSurfaceResolverTest
```

Expected: compile failure because `CopperSurfaceResolver` does not exist.

- [ ] **Step 3.3: Implement block normalization and resolver**

Expose a package-usable Copper-family helper in `GolemVariant` or place all Copper logic in `CopperSurfaceResolver`. Implement:

```java
public static Optional<GolemSurfaceState> surfaceFor(BlockState state) {
    Block block = state.getBlock();
    boolean waxed = HoneycombItem.WAX_OFF_BY_BLOCK.get().containsKey(block);
    Block unwaxed = waxed ? HoneycombItem.WAX_OFF_BY_BLOCK.get().get(block) : block;
    if (!(unwaxed instanceof WeatheringCopper) && !state.is(BlockTags.COPPER)) {
        return Optional.empty();
    }
    return weatheringStage(unwaxed)
        .map(stage -> new GolemSurfaceState(stage, waxed));
}

public static Optional<GolemSurfaceState> resolveBody(List<BlockState> bodyStates) {
    if (bodyStates.size() != 4) throw new IllegalArgumentException("Copper Iron Golem body must have four blocks");
    GolemWeatheringStage stage = GolemWeatheringStage.UNAFFECTED;
    boolean allWaxed = true;
    for (BlockState state : bodyStates) {
        GolemSurfaceState surface = surfaceFor(state).orElse(null);
        if (surface == null) return Optional.empty();
        if (surface.weatheringStage().isAfter(stage)) stage = surface.weatheringStage();
        allWaxed &= surface.waxed();
    }
    return Optional.of(new GolemSurfaceState(stage, allWaxed));
}
```

Map vanilla full copper blocks explicitly in `weatheringStage(Block block)` to avoid relying on unstable implementation details:

```java
if (block == Blocks.COPPER_BLOCK) return Optional.of(GolemWeatheringStage.UNAFFECTED);
if (block == Blocks.EXPOSED_COPPER) return Optional.of(GolemWeatheringStage.EXPOSED);
if (block == Blocks.WEATHERED_COPPER) return Optional.of(GolemWeatheringStage.WEATHERED);
if (block == Blocks.OXIDIZED_COPPER) return Optional.of(GolemWeatheringStage.OXIDIZED);
return Optional.empty();
```

- [ ] **Step 3.4: Wire creation to collect the four body blocks**

Add a helper in `GolemCreationHandler`:

```java
private static List<BlockState> bodyStates(BlockPattern.BlockPatternMatch match) {
    return List.of(
        match.getBlock(0, 1, 0).getState(),
        match.getBlock(1, 1, 0).getState(),
        match.getBlock(2, 1, 0).getState(),
        match.getBlock(1, 0, 0).getState()
    );
}

private static GolemIdentity identityForMatch(GolemVariant variant, BlockPattern.BlockPatternMatch match) {
    if (variant != GolemVariant.COPPER) {
        return GolemIdentity.ofIronVariant(variant);
    }
    return CopperSurfaceResolver.resolveBody(bodyStates(match))
        .map(surface -> GolemIdentity.ofIronVariant(GolemVariant.COPPER, surface))
        .orElseGet(() -> GolemIdentity.ofIronVariant(GolemVariant.COPPER));
}
```

Use `GolemIdentityAttachment.set(golem, identityForMatch(variant, match))`.

- [ ] **Step 3.5: Verify GREEN**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.CopperSurfaceResolverTest
```

Expected: pass.

---

### Task 4: Creation, Permissions, And Catalog Regression Tests

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/spawn/GolemCreationHandler.java`
- Modify tests around creation seams if existing helper tests need identity capture.
- Test: `src/test/java/dev/charles/multigolem/catalog/GolemVariantCatalogTest.java`
- Test: `src/test/java/dev/charles/multigolem/permissions/MultiGolemPermissionsTest.java`
- Test: `src/test/java/dev/charles/multigolem/spawn/VillageGolemSpawnHandlerTest.java`
- Test: `src/test/java/dev/charles/multigolem/spawn/VillageSpawnWeightsTest.java`

- [ ] **Step 4.1: Write failing source-level or seam tests for creation identity**

If live `BlockPatternMatch` construction is impractical, add package-private seams from Task 3 and test them directly:

```java
@Test
void copperCreationIdentityIncludesResolvedSurface() {
    GolemIdentity identity = GolemCreationHandler.identityForBodyStatesForTest(
        GolemVariant.COPPER,
        List.of(
            Blocks.WAXED_OXIDIZED_COPPER.defaultBlockState(),
            Blocks.WAXED_OXIDIZED_COPPER.defaultBlockState(),
            Blocks.WAXED_OXIDIZED_COPPER.defaultBlockState(),
            Blocks.WAXED_OXIDIZED_COPPER.defaultBlockState()
        )
    );

    assertEquals(GolemFamily.IRON_GOLEM, identity.family());
    assertEquals(GolemVariant.COPPER, identity.variant());
    assertEquals(Optional.of(new GolemSurfaceState(GolemWeatheringStage.OXIDIZED, true)), identity.surfaceState());
}

@Test
void nonCopperCreationIdentityHasEmptySurface() {
    assertEquals(Optional.empty(),
        GolemCreationHandler.identityForBodyStatesForTest(
            GolemVariant.DIAMOND,
            List.of(Blocks.DIAMOND_BLOCK.defaultBlockState(), Blocks.DIAMOND_BLOCK.defaultBlockState(),
                Blocks.DIAMOND_BLOCK.defaultBlockState(), Blocks.DIAMOND_BLOCK.defaultBlockState())
        ).surfaceState());
}
```

- [ ] **Step 4.2: Add regression tests for unchanged variant sets and permissions**

Keep current set expectations unchanged:

```java
assertEquals(List.of(COPPER, GOLD, EMERALD, DIAMOND, NETHERITE, ZOMBIE), GolemVariant.spawnEggVariants());
assertEquals(List.of(COPPER, GOLD, EMERALD, DIAMOND, NETHERITE, ZOMBIE), GolemVariant.lootVariants());
assertEquals(List.of(COPPER, GOLD, EMERALD, DIAMOND, NETHERITE, ZOMBIE), GolemVariant.multiGolemPlayerBuildableVariants());
assertFalse(Arrays.stream(GolemVariant.values()).anyMatch(v -> v.id().equals("redstone") || v.id().equals("lapis")));
```

Assert Copper surface state does not add a new permission node:

```java
assertEquals("multigolem.create.copper", MultiGolemPermissions.createNode(GolemVariant.COPPER));
assertEquals("multigolem.heal.copper", MultiGolemPermissions.healNode(GolemVariant.COPPER));
```

Assert village rolls remain surface-empty:

```java
assertEquals(GolemIdentity.ofIronVariant(GolemVariant.COPPER),
    VillageGolemSpawnHandler.identityForVillageRollForTest(GolemVariant.COPPER));
```

- [ ] **Step 4.3: Verify RED**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.GolemCreationHandlerTest --tests dev.charles.multigolem.catalog.GolemVariantCatalogTest --tests dev.charles.multigolem.permissions.MultiGolemPermissionsTest --tests dev.charles.multigolem.spawn.VillageGolemSpawnHandlerTest --tests dev.charles.multigolem.spawn.VillageSpawnWeightsTest
```

Expected: compile failure for new test seams or assertion failure until creation and village helpers exist.

- [ ] **Step 4.4: Implement minimal seams and preserve behavior**

Add package-private seam methods only where tests need pure verification:

```java
static GolemIdentity identityForBodyStatesForTest(GolemVariant variant, List<BlockState> bodyStates) {
    if (variant != GolemVariant.COPPER) return GolemIdentity.ofIronVariant(variant);
    return CopperSurfaceResolver.resolveBody(bodyStates)
        .map(surface -> GolemIdentity.ofIronVariant(GolemVariant.COPPER, surface))
        .orElseGet(() -> GolemIdentity.ofIronVariant(GolemVariant.COPPER));
}
```

For village spawning, keep `GolemIdentity.ofIronVariant(variant)` and do not derive surface state from config, biome, block, or random input.

- [ ] **Step 4.5: Verify GREEN**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.GolemCreationHandlerTest --tests dev.charles.multigolem.catalog.GolemVariantCatalogTest --tests dev.charles.multigolem.permissions.MultiGolemPermissionsTest --tests dev.charles.multigolem.spawn.VillageGolemSpawnHandlerTest --tests dev.charles.multigolem.spawn.VillageSpawnWeightsTest
```

Expected: pass.

---

### Task 5: Spawn Egg And Spawner Surface Markers

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/spawn/SpawnEggStacks.java`
- Modify: `src/main/java/dev/charles/multigolem/spawn/SpawnerVariantMarker.java`
- Modify: `src/main/java/dev/charles/multigolem/spawn/SpawnEggVariantSpawner.java`
- Modify: `src/main/java/dev/charles/multigolem/mixin/BaseSpawnerMixin.java`
- Test: `src/test/java/dev/charles/multigolem/spawn/SpawnEggStacksTest.java`
- Test: `src/test/java/dev/charles/multigolem/spawn/SpawnerVariantMarkerTest.java`

- [ ] **Step 5.1: Write failing marker tests**

Add tests equivalent to:

```java
@Test
void spawnEggSurfaceMarkerRoundTripsFullIdentity() {
    GolemIdentity identity = GolemIdentity.ofIronVariant(GolemVariant.COPPER,
        new GolemSurfaceState(GolemWeatheringStage.WEATHERED, true));
    ItemStack stack = SpawnEggStacks.create(identity);
    assertEquals(identity, SpawnEggStacks.identityFrom(stack).orElseThrow());
    assertTrue(SpawnEggStacks.customDataSnbt(stack).contains("surface"));
    assertTrue(SpawnEggStacks.customDataSnbt(stack).contains("weathering_stage"));
}

@Test
void oldVariantOnlyCopperMarkerReadsAsSurfaceEmptyCopper() {
    ItemStack stack = markedEggWith("{multigolem:{variant:\"copper\"}}");
    assertEquals(GolemIdentity.ofIronVariant(GolemVariant.COPPER), SpawnEggStacks.identityFrom(stack).orElseThrow());
}

@Test
void invalidSurfaceMarkerReadsEmptyInsteadOfWrongGolem() {
    ItemStack stack = markedEggWith("{multigolem:{family:\"iron_golem\",variant:\"gold\",surface:{weathering_stage:\"oxidized\",waxed:true}}}");
    assertTrue(SpawnEggStacks.identityFrom(stack).isEmpty());
}

@Test
void spawnerSurfaceMarkerRoundTripsFullIdentity() {
    CompoundTag tag = new CompoundTag();
    GolemIdentity identity = GolemIdentity.ofIronVariant(GolemVariant.COPPER,
        new GolemSurfaceState(GolemWeatheringStage.EXPOSED, false));
    SpawnerVariantMarker.writeIdentity(tag, identity);
    assertEquals(identity, SpawnerVariantMarker.readIdentity(tag).orElseThrow());
    assertEquals(identity, SpawnerVariantMarker.previewIdentity(tag).orElseThrow());
}
```

- [ ] **Step 5.2: Verify RED**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.SpawnEggStacksTest --tests dev.charles.multigolem.spawn.SpawnerVariantMarkerTest
```

Expected: compile failure for `SpawnEggStacks.create(GolemIdentity)` or assertion failure because markers do not write/read `surface`.

- [ ] **Step 5.3: Implement identity-shaped surface marker read/write**

Add constants:

```java
private static final String SURFACE_KEY = "surface";
private static final String WEATHERING_STAGE_KEY = "weathering_stage";
private static final String WAXED_KEY = "waxed";
```

Add `SpawnEggStacks.create(GolemIdentity identity)` and keep `create(GolemVariant variant)` as a wrapper:

```java
public static ItemStack create(GolemIdentity identity) {
    if (identity.variant() == GolemVariant.IRON || !identity.isValidForPhase3()) {
        throw new IllegalArgumentException("Unsupported marked spawn egg identity: " + identity);
    }
    ItemStack stack = new ItemStack(Items.IRON_GOLEM_SPAWN_EGG);
    stack.set(DataComponents.ITEM_NAME, Component.literal(identity.variant().displayName() + " Golem Spawn Egg"));
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(markerTag(identity)));
    return stack;
}
```

Write `surface` only when `identity.surfaceState().isPresent()`:

```java
CompoundTag surface = new CompoundTag();
surface.putString(WEATHERING_STAGE_KEY, state.weatheringStage().id());
surface.putBoolean(WAXED_KEY, state.waxed());
multigolem.put(SURFACE_KEY, surface);
```

Read markers through a shared helper used by both spawn eggs and spawners:

```java
private static Optional<GolemIdentity> identityFromMarker(CompoundTag multigolem) {
    Optional<GolemFamily> family = GolemFamily.fromId(multigolem.getStringOr(FAMILY_KEY, GolemFamily.IRON_GOLEM.id()));
    Optional<GolemVariant> variant = GolemVariant.fromId(multigolem.getStringOr(VARIANT_KEY, ""));
    if (family.isEmpty() || variant.isEmpty()) return Optional.empty();
    Optional<GolemSurfaceState> surface = surfaceFrom(multigolem.getCompound(SURFACE_KEY));
    GolemIdentity identity = new GolemIdentity(family.get(), variant.get(), surface);
    if (!identity.isValidForPhase3() || identity.isDefaultIron()) return Optional.empty();
    return Optional.of(identity);
}
```

Malformed `surface` must make the marker read empty. Missing `surface` must read as `Optional.empty()`.

- [ ] **Step 5.4: Apply full identity in spawn paths**

Ensure `SpawnEggVariantSpawner` and both `BaseSpawnerMixin` paths call:

```java
GolemIdentityAttachment.set(golem, identity);
```

Do not collapse identity back to variant before writing attachments. Keep permission checks as `identity.variant()` for this slice.

- [ ] **Step 5.5: Verify GREEN**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.SpawnEggStacksTest --tests dev.charles.multigolem.spawn.SpawnerVariantMarkerTest
```

Expected: pass.

---

### Task 6: Rendering Identity And Copper Surface Textures

**Files:**
- Modify: `src/client/java/dev/charles/multigolem/client/render/GolemRenderStateExtension.java`
- Modify: `src/client/java/dev/charles/multigolem/client/mixin/IronGolemRenderStateExtensionMixin.java`
- Modify: `src/client/java/dev/charles/multigolem/client/mixin/IronGolemRenderStateMixin.java`
- Modify: `src/client/java/dev/charles/multigolem/client/mixin/IronGolemRendererMixin.java`
- Modify: `src/client/java/dev/charles/multigolem/client/render/GolemTextureSelector.java`
- Test: `src/test/java/dev/charles/multigolem/client/render/GolemTextureSelectorTest.java`

- [ ] **Step 6.1: Write failing texture selector tests**

Add tests equivalent to:

```java
@Test
void copperSurfaceTexturesUseFamilyFolderAndWeatheringSuffixes() {
    assertEquals(id("textures/entity/iron_golem/copper_golem.png"),
        GolemTextureSelector.get(GolemIdentity.ofIronVariant(GolemVariant.COPPER)));
    assertEquals(id("textures/entity/iron_golem/copper_golem_exposed.png"),
        GolemTextureSelector.get(GolemIdentity.ofIronVariant(GolemVariant.COPPER,
            new GolemSurfaceState(GolemWeatheringStage.EXPOSED, false))));
    assertEquals(id("textures/entity/iron_golem/copper_golem_waxed_oxidized.png"),
        GolemTextureSelector.get(GolemIdentity.ofIronVariant(GolemVariant.COPPER,
            new GolemSurfaceState(GolemWeatheringStage.OXIDIZED, true))));
}

@Test
void nonCopperVariantsKeepExistingFlatTexturePaths() {
    assertEquals(id("textures/entity/diamond_golem.png"),
        GolemTextureSelector.get(GolemIdentity.ofIronVariant(GolemVariant.DIAMOND)));
}

@Test
void invalidOrMissingCopperSurfaceFallsBackToCurrentCopperTexture() {
    assertEquals(id("textures/entity/iron_golem/copper_golem.png"),
        GolemTextureSelector.copperFallbackForTest());
}
```

- [ ] **Step 6.2: Verify RED**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.client.render.GolemTextureSelectorTest
```

Expected: compile failure or assertion failure because selector still accepts only `GolemVariant`.

- [ ] **Step 6.3: Store identity in render state**

Change `GolemRenderStateExtension` to:

```java
GolemIdentity multigolem$getIdentity();
void multigolem$setIdentity(GolemIdentity identity);
```

Change `IronGolemRenderStateExtensionMixin` default field to `GolemIdentity.defaultIron()`.

Change `IronGolemRenderStateMixin` to capture:

```java
GolemIdentity identity = GolemIdentityAttachment.get(entity);
((GolemRenderStateExtension) state).multigolem$setIdentity(identity);
```

- [ ] **Step 6.4: Implement identity-aware texture selector**

Keep non-Copper flat paths for this first slice. Add Copper surface paths:

```java
public static Identifier get(GolemIdentity identity) {
    if (identity.family() == GolemFamily.IRON_GOLEM && identity.variant() == GolemVariant.COPPER) {
        return copperTexture(identity.surfaceState().orElse(GolemSurfaceState.DEFAULT));
    }
    return get(identity.variant());
}

private static Identifier copperTexture(GolemSurfaceState surface) {
    String suffix = switch (surface.weatheringStage()) {
        case UNAFFECTED -> surface.waxed() ? "_waxed" : "";
        case EXPOSED -> surface.waxed() ? "_waxed_exposed" : "_exposed";
        case WEATHERED -> surface.waxed() ? "_waxed_weathered" : "_weathered";
        case OXIDIZED -> surface.waxed() ? "_waxed_oxidized" : "_oxidized";
    };
    return Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID,
        "textures/entity/iron_golem/copper_golem" + suffix + ".png");
}
```

Change renderer mixin to read identity and return texture when `identity.variant() != GolemVariant.IRON`.

- [ ] **Step 6.5: Add texture asset existence tests or fallback constraint**

If Copper surface assets do not exist yet, add an explicit test that documents missing assets must fall back to `textures/entity/copper_golem.png` or create the required texture assets in a later asset task. Do not leave a selector that points at non-existent runtime textures without a test documenting the expected fallback.

- [ ] **Step 6.6: Verify GREEN**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.client.render.GolemTextureSelectorTest
```

Expected: pass.

---

### Task 7: Deliberate Non-Surface Regression Sweep

**Files:**
- Modify tests only unless a regression is exposed:
  - `src/test/java/dev/charles/multigolem/loot/HasGolemVariantLootConditionTest.java`
  - `src/test/java/dev/charles/multigolem/mixin/IronGolemMixinTest.java`
  - `src/test/java/dev/charles/multigolem/attribute/VariantAttributesTest.java`
  - `src/test/java/dev/charles/multigolem/ability/TargetFilterTest.java`
  - `src/test/java/dev/charles/multigolem/ability/ZombieGolemFactionTest.java`
  - `src/test/java/dev/charles/multigolem/mixin/GolemTargetingMixinTest.java`
  - `src/test/java/dev/charles/multigolem/permissions/MultiGolemPermissionsTest.java`

- [ ] **Step 7.1: Add surface-ignored regression cases**

Add focused tests proving these systems deliberately ignore Copper surface state:

```java
GolemIdentity oxidizedCopper = GolemIdentity.ofIronVariant(GolemVariant.COPPER,
    new GolemSurfaceState(GolemWeatheringStage.OXIDIZED, true));
assertEquals(GolemVariant.COPPER, oxidizedCopper.variant());
```

Use that identity or an entity/test seam carrying it to prove:

- Copper drops are still Copper ingots.
- Copper healing still uses `multigolem.heal.copper`.
- Copper stats and lightning behavior remain keyed to `GolemVariant.COPPER`.
- Copper targeting/faction rules are unchanged.
- `multigolem.create.copper` covers every Copper body-block surface.
- Iron, Gold, Emerald, Diamond, Netherite, and Zombie identities have empty surface state in these tests.
- Reserved Copper Golem-family identities are rejected before gameplay systems treat them as supported. Add a pure attachment/migration regression:

```java
@Test
void reservedCopperGolemFamilyDoesNotEnterRuntimeAsSupportedIdentity() {
    MapBackedIdentityStorage storage = new MapBackedIdentityStorage();
    GolemIdentityMigration.write(storage,
        new GolemIdentity(GolemFamily.COPPER_GOLEM, GolemVariant.COPPER, Optional.empty()));

    assertEquals(GolemIdentity.defaultIron(), GolemIdentityMigration.resolve(storage));
    assertTrue(storage.rawIdentity().isEmpty());
    assertTrue(storage.rawVariant().isEmpty());
}
```

Creation and village-spawn seams should only construct `GolemIdentity.ofIronVariant(...)`, and permission checks should continue to accept `GolemVariant` rather than arbitrary family-bearing identities in this first slice.

- [ ] **Step 7.2: Verify focused regression tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.loot.HasGolemVariantLootConditionTest --tests dev.charles.multigolem.mixin.IronGolemMixinTest --tests dev.charles.multigolem.attribute.VariantAttributesTest --tests dev.charles.multigolem.ability.TargetFilterTest --tests dev.charles.multigolem.ability.ZombieGolemFactionTest --tests dev.charles.multigolem.mixin.GolemTargetingMixinTest --tests dev.charles.multigolem.permissions.MultiGolemPermissionsTest
```

Expected: pass.

---

### Task 8: Scoped Verification And Internal Notes

**Files:**
- Modify `INTERNAL_CHANGELOG.md` only if a maintainer-only beta note is required by current repo gates.
- Do not modify release metadata unless a separate release-prep prompt explicitly asks for it.

- [ ] **Step 8.1: Check plan scope before final implementation review**

Run:

```powershell
git diff -- src/main/java src/client/java src/test/java docs/superpowers/plans/2026-05-31-multigolem-phase-3-surface-state.md INTERNAL_CHANGELOG.md CHANGELOG.md
```

Expected: runtime changes are limited to Phase 3 surface state, reserved `COPPER_GOLEM`, Copper Iron Golem surface propagation, tests, and optional internal notes. No Redstone, Lapis, Modrinth, CurseForge, tag, or public release prep changes appear.

- [ ] **Step 8.2: Run targeted Phase 3 suite**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.identity.GolemWeatheringStageTest --tests dev.charles.multigolem.identity.GolemSurfaceStateTest --tests dev.charles.multigolem.identity.GolemIdentityTest --tests dev.charles.multigolem.attachment.GolemIdentityAttachmentTest --tests dev.charles.multigolem.spawn.CopperSurfaceResolverTest --tests dev.charles.multigolem.spawn.SpawnEggStacksTest --tests dev.charles.multigolem.spawn.SpawnerVariantMarkerTest --tests dev.charles.multigolem.client.render.GolemTextureSelectorTest --tests dev.charles.multigolem.catalog.GolemVariantCatalogTest --tests dev.charles.multigolem.permissions.MultiGolemPermissionsTest --tests dev.charles.multigolem.spawn.VillageGolemSpawnHandlerTest --tests dev.charles.multigolem.spawn.VillageSpawnWeightsTest
```

Expected: pass.

- [ ] **Step 8.3: Run required closeout commands**

Run exactly:

```powershell
git rev-parse --show-toplevel
git status --short --branch
.\gradlew.bat --quiet test
.\gradlew.bat build
```

Expected:

- root is `C:/Users/tyler/AI Projects/MultiGolem/.worktrees/phase-3-surface-state`
- branch is `codex/phase-3-surface-state`
- status contains only intentional Phase 3 changes before commit
- tests pass
- build succeeds

---

## Revue Gates

- [ ] Send this plan through Revue as `implementation-plan-review` before runtime implementation starts.
- [ ] Use explicit file scope: `docs/superpowers/plans/2026-05-31-multigolem-phase-3-surface-state.md`.
- [ ] Show Revue operator surface before `revue-worker once`: dashboard link, status command, tail command, and cancel command.
- [ ] If Revue packetizes, inspect the parent, run the child packet review explicitly, and verify coverage includes the plan file.
- [ ] Use `superpowers:receiving-code-review` before editing the plan for findings.
- [ ] Mark valid Revue findings `fixed` after actioning them.
- [ ] Reject invalid findings only with explicit technical reasoning in the finding update.
- [ ] Confirm Revue shows `0 unresolved` before final verification and commit.
- [ ] After implementation is completed in a future session, send the runtime diff through Revue as `implementation-review`.

## Explicit Scope Boundaries

- [ ] Do not implement runtime Phase 3 code in the planning/review session that creates this document.
- [ ] Do not add Redstone or Lapis variants.
- [ ] Do not change vanilla single-block Copper Golem behavior.
- [ ] Do not add Copper Golem-family runtime behavior beyond reserving and safely rejecting `GolemFamily.COPPER_GOLEM`.
- [ ] Do not add weathering ticks, waxing, or scraping for existing golems.
- [ ] Do not add separate creative-tab entries for every Copper surface unless a later reviewed plan approves that UI.
- [ ] Do not publish, upload to Modrinth, upload to CurseForge, tag, or prep a public release.

## Self-Review

- Spec coverage: the plan covers surface value types, optional identity surface state, reserved Copper Golem family, Copper body-block mapping, lazy migration, spawn egg and spawner propagation, rendering, compatibility, and regressions.
- Missing tests check: compatibility, rendering/texture selection, spawn egg markers, spawner markers, player-built Copper mapping, drops, healing, stats, targeting, permissions, village spawning, catalog sets, and non-Copper unchanged behavior are all represented.
- Placeholder scan: no unfinished placeholder language or unbounded "add tests" instructions remain.
- Scope scan: no Redstone, Lapis, release, Modrinth, CurseForge, tag, or vanilla Copper Golem-family runtime work is included.
- Planning-session boundary: this document is an implementation plan only; runtime code changes start only in a later execution session after Revue plan review is resolved.
