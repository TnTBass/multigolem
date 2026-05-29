# MultiGolem Identity Catalog Phase 1+2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Phase 1 shared variant catalog and Phase 2 identity attachment model without changing current Iron-family gameplay.

**Architecture:** Keep `GolemVariant` as the current Iron-family gameplay enum, add catalog metadata as the single source for variant surfaces, then layer `GolemIdentity` over the existing variant attachment through lazy dual-read and explicit dual-write. Existing variant APIs remain as compatibility shims while touched production paths move to catalog or identity helpers.

**Tech Stack:** Java 25, Fabric API attachments, Minecraft 26.1.2 mappings, JUnit 6, Gradle/Loom.

---

## Pre-Edit Gates

- [ ] Run `git rev-parse --show-toplevel`.
  - Expected: `C:/Users/tyler/AI Projects/MultiGolem/.worktrees/identity-catalog-phase-1-2`.
- [ ] Run `git status --short --branch`.
  - Expected: branch `codex/identity-catalog-phase-1-2`; only planned files are dirty.
- [ ] Run `.\gradlew.bat --quiet test`.
  - Baseline already passed in this worktree on 2026-05-28 with only existing deprecation/JVM warnings.

## File Map

- Create `src/main/java/dev/charles/multigolem/catalog/GolemVariantCatalog.java`
  - Owns one catalog entry per `GolemVariant`, explicit surface flags, loot ranges, permission suffixes, spawn egg asset paths, and entity texture paths.
- Create `src/main/java/dev/charles/multigolem/catalog/GolemVariantSpec.java`
  - Immutable catalog row for a current Iron-family variant.
- Create `src/main/java/dev/charles/multigolem/identity/GolemFamily.java`
  - Stable saved family ids; Phase 2 contains only `IRON_GOLEM("iron_golem")`.
- Create `src/main/java/dev/charles/multigolem/identity/GolemIdentity.java`
  - Runtime value type with default Iron identity and validation helpers.
- Create `src/main/java/dev/charles/multigolem/attachment/GolemIdentityAttachment.java`
  - Persistent/synced identity attachment with safe raw read, passive read, explicit set/clear, and old-variant fallback.
- Modify `src/main/java/dev/charles/multigolem/GolemVariant.java`
  - Delegate common metadata and set helpers to the catalog; keep codecs and old API.
- Modify `src/main/java/dev/charles/multigolem/attachment/GolemVariantAttachment.java`
  - Keep `get(entity)` as a compatibility shim over identity; add raw old-attachment helpers used by identity migration.
- Modify `src/main/java/dev/charles/multigolem/MultiGolem.java`
  - Register identity attachment; derive creative eggs and loot from catalog helpers.
- Modify `src/main/java/dev/charles/multigolem/spawn/SpawnEggStacks.java`
  - Add identity-shaped marker read/write while accepting old variant-only markers.
- Modify `src/main/java/dev/charles/multigolem/spawn/SpawnerVariantMarker.java`
  - Same identity-shaped marker read/write contract as spawn eggs.
- Modify identity writers:
  - `src/main/java/dev/charles/multigolem/spawn/GolemCreationHandler.java`
  - `src/main/java/dev/charles/multigolem/spawn/SpawnEggVariantSpawner.java`
  - `src/main/java/dev/charles/multigolem/mixin/BaseSpawnerMixin.java`
  - `src/main/java/dev/charles/multigolem/spawn/VillageGolemSpawnHandler.java`
  - `src/main/java/dev/charles/multigolem/spawn/ZombieVillageSpawnHandler.java`
- Modify passive readers only as touched:
  - `src/main/java/dev/charles/multigolem/attribute/VariantAttributes.java`
  - `src/main/java/dev/charles/multigolem/loot/HasGolemVariantLootCondition.java`
  - `src/main/java/dev/charles/multigolem/permissions/MultiGolemPermissions.java`
  - `src/main/java/dev/charles/multigolem/ability/ZombieGolemFaction.java`
  - selected mixins that already read `GolemVariantAttachment.get(...)`.
- Add or modify tests:
  - `src/test/java/dev/charles/multigolem/catalog/GolemVariantCatalogTest.java`
  - `src/test/java/dev/charles/multigolem/identity/GolemIdentityTest.java`
  - `src/test/java/dev/charles/multigolem/attachment/GolemIdentityAttachmentTest.java`
  - `src/test/java/dev/charles/multigolem/spawn/SpawnEggStacksTest.java`
  - `src/test/java/dev/charles/multigolem/spawn/SpawnerVariantMarkerTest.java`
  - `src/test/java/dev/charles/multigolem/MultiGolemRegistrationTest.java`
  - `src/test/java/dev/charles/multigolem/permissions/MultiGolemPermissionsTest.java`
  - `src/test/java/dev/charles/multigolem/attribute/VariantAttributesTest.java`
- Modify `CHANGELOG.md` and/or `INTERNAL_CHANGELOG.md` only for scoped beta/refactor clarity required by build gates.

---

### Task 1: Catalog Metadata And Variant Sets

**Files:**
- Create: `src/main/java/dev/charles/multigolem/catalog/GolemVariantSpec.java`
- Create: `src/main/java/dev/charles/multigolem/catalog/GolemVariantCatalog.java`
- Create: `src/test/java/dev/charles/multigolem/catalog/GolemVariantCatalogTest.java`
- Modify: `src/main/java/dev/charles/multigolem/GolemVariant.java`
- Modify: `src/test/java/dev/charles/multigolem/GolemVariantTest.java`

- [ ] **Step 1.1: Write failing catalog completeness tests**

Add `GolemVariantCatalogTest` with assertions equivalent to:

```java
@Test
void everyVariantHasExactlyOneCatalogEntry() {
    assertEquals(EnumSet.allOf(GolemVariant.class), GolemVariantCatalog.variants());
}

@Test
void everyCatalogEntryDeclaresRequiredSurfaceIntent() {
    for (GolemVariantSpec spec : GolemVariantCatalog.entries()) {
        assertNotNull(spec.family(), spec.variant().id());
        assertNotNull(spec.bodyBlockMatcherSupplier(), spec.variant().id());
        assertNotNull(spec.healItem(), spec.variant().id());
        assertNotNull(spec.dropItem(), spec.variant().id());
        assertNotNull(spec.permissionSuffix(), spec.variant().id());
        assertTrue(spec.lootMin() <= spec.lootMax(), spec.variant().id());
        if (spec.spawnEggEnabled()) {
            assertFalse(spec.spawnEggModelPath().contains("/"), "spawnEggModelPath must be a bare filename");
            assertFalse(spec.spawnEggTexturePath().contains("/"), "spawnEggTexturePath must be a bare filename");
            assertTrue(spec.spawnEggModelPath().endsWith("_golem_spawn_egg.json"));
            assertTrue(spec.spawnEggTexturePath().endsWith("_golem_spawn_egg.png"));
        }
        if (spec.renderable()) {
            assertFalse(spec.entityTexturePath().contains("/"), "entityTexturePath must be a bare filename");
            assertTrue(spec.entityTexturePath().endsWith("_golem.png"));
        }
    }
}

@Test
void catalogDerivedSetsMatchCurrentIntent() {
    assertEquals(List.of(COPPER, GOLD, EMERALD, DIAMOND, NETHERITE, ZOMBIE), GolemVariant.spawnEggVariants());
    assertEquals(List.of(COPPER, GOLD, EMERALD, DIAMOND, NETHERITE, ZOMBIE), GolemVariant.lootVariants());
    assertEquals(List.of(COPPER, GOLD, EMERALD, DIAMOND, NETHERITE, ZOMBIE), GolemVariant.multiGolemPlayerBuildableVariants());
    assertEquals(List.of(COPPER, GOLD, EMERALD, DIAMOND, NETHERITE, ZOMBIE), GolemVariant.nonIronVariants());
}
```

- [ ] **Step 1.2: Verify RED**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.catalog.GolemVariantCatalogTest
```

Expected: compile failure because the catalog package and new helper methods do not exist.

- [ ] **Step 1.3: Implement catalog rows**

Create `GolemVariantSpec` as a record with fields:

```java
public record GolemVariantSpec(
    GolemVariant variant,
    GolemFamily family,
    Supplier<Predicate<BlockState>> bodyBlockMatcherSupplier,
    Item healItem,
    Item dropItem,
    int lootMin,
    int lootMax,
    boolean spawnEggEnabled,
    boolean lootEnabled,
    boolean playerBuildable,
    boolean healEnabled,
    boolean permissionEnabled,
    boolean renderable,
    String permissionSuffix,
    String entityTexturePath,
    String spawnEggModelPath,
    String spawnEggTexturePath
) {}
```

Create `GolemVariantCatalog` with an `EnumMap<GolemVariant, GolemVariantSpec>` containing all seven current variants. Use `spawnEggEnabled=false`, `lootEnabled=false`, and `playerBuildable=false` for `IRON`; keep `healEnabled=true`, `permissionEnabled=true`, and `renderable=false` for `IRON`.

Catalog rows must store bare asset filenames only:

```java
entityTexturePath = "diamond_golem.png";
spawnEggModelPath = "diamond_golem_spawn_egg.json";
spawnEggTexturePath = "diamond_golem_spawn_egg.png";
```

Do not store subdirectory-prefixed values such as `item/diamond_golem_spawn_egg.json`; tests join these names to the known resource directories.

The `bodyBlockMatcherSupplier` must return a lazy predicate. Do not capture or construct concrete `BlockState` objects while building static catalog rows. If a row needs Copper-family matching, the supplier should return a lambda that evaluates the passed runtime `BlockState`.

Expose:

```java
public static Collection<GolemVariantSpec> entries();
public static EnumSet<GolemVariant> variants();
public static GolemVariantSpec require(GolemVariant variant);
public static Optional<GolemVariantSpec> forBodyBlock(BlockState state);
public static Optional<GolemVariantSpec> forHealItem(Item item);
public static List<GolemVariant> variantsWhere(Predicate<GolemVariantSpec> predicate);
```

Move Copper body-block matching into the catalog matcher by reusing the existing `GolemVariant.matchesBodyBlock` logic until Task 1 refactors it fully.

- [ ] **Step 1.4: Delegate `GolemVariant` helpers to catalog**

Keep existing enum constructor fields for compatibility, but change:

```java
public static List<GolemVariant> nonIronVariants() {
    return GolemVariantCatalog.variantsWhere(spec -> spec.variant() != IRON);
}

public static List<GolemVariant> spawnEggVariants() {
    return GolemVariantCatalog.variantsWhere(GolemVariantSpec::spawnEggEnabled);
}

public static List<GolemVariant> lootVariants() {
    return GolemVariantCatalog.variantsWhere(GolemVariantSpec::lootEnabled);
}

public static List<GolemVariant> multiGolemPlayerBuildableVariants() {
    return GolemVariantCatalog.variantsWhere(GolemVariantSpec::playerBuildable);
}
```

Also update `fromBodyBlock(Block)` and `fromIngot(Item)` to use the catalog.

- [ ] **Step 1.5: Verify GREEN**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.catalog.GolemVariantCatalogTest --tests dev.charles.multigolem.GolemVariantTest
```

Expected: pass.

---

### Task 2: Catalog-Driven Registration, Assets, Permissions, And Loot

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/MultiGolem.java`
- Modify: `src/main/java/dev/charles/multigolem/spawn/GolemCreationHandler.java`
- Modify: `src/main/java/dev/charles/multigolem/permissions/MultiGolemPermissions.java`
- Modify: `src/test/java/dev/charles/multigolem/MultiGolemRegistrationTest.java`
- Modify: `src/test/java/dev/charles/multigolem/spawn/SpawnEggItemDefinitionTest.java`
- Modify: `src/test/java/dev/charles/multigolem/permissions/MultiGolemPermissionsTest.java`
- Modify: `src/test/java/dev/charles/multigolem/attribute/VariantAttributesTest.java`

- [ ] **Step 2.1: Write failing registration and asset tests**

Update tests so they derive expectations from catalog helpers:

```java
assertIterableEquals(GolemVariant.spawnEggVariants(), MultiGolem.creativeSpawnEggVariants());
for (GolemVariant variant : GolemVariant.lootVariants()) {
    MultiGolem.VariantLootDrop drop = MultiGolem.lootDropFor(variant);
    assertTrue(drop.min() > 0, variant.id());
    assertTrue(drop.max() >= drop.min(), variant.id());
}
```

In `SpawnEggItemDefinitionTest`, replace `GolemVariant.nonIronVariants()` with `GolemVariant.spawnEggVariants()` and assert each catalog path exists:

```java
GolemVariantSpec spec = GolemVariantCatalog.require(variant);
assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/multigolem/models/item").resolve(spec.spawnEggModelPath())));
assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/multigolem/textures/item").resolve(spec.spawnEggTexturePath())));
assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/multigolem/textures/entity").resolve(spec.entityTexturePath())));
```

In `MultiGolemPermissionsTest`, loop over `GolemVariant.multiGolemPlayerBuildableVariants()` for create nodes and `GolemVariantCatalog.variantsWhere(GolemVariantSpec::healEnabled)` for heal nodes.

- [ ] **Step 2.2: Verify RED**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.MultiGolemRegistrationTest --tests dev.charles.multigolem.spawn.SpawnEggItemDefinitionTest --tests dev.charles.multigolem.permissions.MultiGolemPermissionsTest
```

Expected: fail until production code exposes catalog-driven registration and path helpers.

- [ ] **Step 2.3: Replace production non-gameplay lists**

Change:

```java
static List<GolemVariant> creativeSpawnEggVariants() {
    return GolemVariant.spawnEggVariants();
}
```

Change loot registration to loop `GolemVariant.lootVariants()` and make `lootDropFor` read `GolemVariantCatalog.require(variant)` instead of a switch. Keep `IRON` throwing `IllegalArgumentException`.

Change `GolemCreationHandler.trySpawnVariant` to loop `GolemVariant.multiGolemPlayerBuildableVariants()` instead of `GolemVariant.values()` plus `IRON` filtering.

Keep `VillageSpawnWeights.rollOrder()` unchanged because it is gameplay distribution.

- [ ] **Step 2.4: Verify GREEN**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.MultiGolemRegistrationTest --tests dev.charles.multigolem.spawn.SpawnEggItemDefinitionTest --tests dev.charles.multigolem.permissions.MultiGolemPermissionsTest --tests dev.charles.multigolem.attribute.VariantAttributesTest
```

Expected: pass.

---

### Task 3: Identity Value Types And Attachment Lazy Migration

**Files:**
- Create: `src/main/java/dev/charles/multigolem/identity/GolemFamily.java`
- Create: `src/main/java/dev/charles/multigolem/identity/GolemIdentity.java`
- Create: `src/main/java/dev/charles/multigolem/identity/GolemIdentityStorage.java`
- Create: `src/main/java/dev/charles/multigolem/attachment/GolemIdentityAttachment.java`
- Create: `src/test/java/dev/charles/multigolem/identity/GolemIdentityTest.java`
- Create: `src/test/java/dev/charles/multigolem/attachment/GolemIdentityAttachmentTest.java`
- Modify: `src/main/java/dev/charles/multigolem/attachment/GolemVariantAttachment.java`
- Modify: `src/main/java/dev/charles/multigolem/MultiGolem.java`

- [ ] **Step 3.1: Write failing identity and migration tests**

Add pure tests for:

```java
assertEquals("iron_golem", GolemFamily.IRON_GOLEM.id());
assertEquals(new GolemIdentity(GolemFamily.IRON_GOLEM, GolemVariant.IRON), GolemIdentity.defaultIron());
assertTrue(GolemIdentity.ofIronVariant(GolemVariant.DIAMOND).isValidForPhase2());
```

Add attachment tests with mocked `Entity` where possible, or a small fake wrapper if direct Fabric attachment calls require bootstrap:

Before writing entity-facing tests, add a pure storage seam so lazy-migration logic is testable without mocking Fabric-injected attachment methods:

```java
public interface GolemIdentityStorage {
    Optional<GolemIdentity> rawIdentity();
    void setRawIdentity(GolemIdentity identity);
    void clearRawIdentity();
    Optional<GolemVariant> rawVariant();
    void setRawVariant(GolemVariant variant);
    void clearRawVariant();
}
```

Add a map-backed test double in `GolemIdentityAttachmentTest`:

```java
private static final class MapBackedIdentityStorage implements GolemIdentityStorage {
    private GolemIdentity identity;
    private GolemVariant variant;

    @Override public Optional<GolemIdentity> rawIdentity() { return Optional.ofNullable(identity); }
    @Override public void setRawIdentity(GolemIdentity identity) { this.identity = identity; }
    @Override public void clearRawIdentity() { this.identity = null; }
    @Override public Optional<GolemVariant> rawVariant() { return Optional.ofNullable(variant); }
    @Override public void setRawVariant(GolemVariant variant) { this.variant = variant; }
    @Override public void clearRawVariant() { this.variant = null; }
}
```

`GolemIdentityAttachment` should expose package-private pure helpers such as `resolve(GolemIdentityStorage storage)` and `write(GolemIdentityStorage storage, GolemIdentity identity)`. Public `get(Entity)` and `set(Entity, GolemIdentity)` adapt the Fabric entity attachments to this storage interface, then apply attributes when the entity is an `IronGolem`.

```java
@Test
void oldOnlyVariantResolvesToIronFamilyIdentityWithoutClearingOldData() {
    MapBackedIdentityStorage storage = oldOnlyStorage(GolemVariant.DIAMOND);
    assertEquals(GolemIdentity.ofIronVariant(GolemVariant.DIAMOND), GolemIdentityAttachment.resolve(storage));
    assertEquals(Optional.of(GolemVariant.DIAMOND), storage.rawVariant());
}

@Test
void newIdentityWinsOverConflictingOldVariant() {
    MapBackedIdentityStorage storage = oldAndNewStorage(GolemVariant.DIAMOND, GolemIdentity.ofIronVariant(GolemVariant.GOLD));
    assertEquals(GolemIdentity.ofIronVariant(GolemVariant.GOLD), GolemIdentityAttachment.resolve(storage));
}

@Test
void explicitDefaultIronSetClearsBothAttachments() {
    MapBackedIdentityStorage storage = oldAndNewStorage(GolemVariant.DIAMOND, GolemIdentity.ofIronVariant(GolemVariant.GOLD));
    GolemIdentityAttachment.write(storage, GolemIdentity.defaultIron());
    assertTrue(storage.rawIdentity().isEmpty());
    assertTrue(storage.rawVariant().isEmpty());
}
```

- [ ] **Step 3.2: Verify RED**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.identity.GolemIdentityTest --tests dev.charles.multigolem.attachment.GolemIdentityAttachmentTest
```

Expected: compile failure because identity types do not exist.

- [ ] **Step 3.3: Implement identity model and attachment**

Implement `GolemFamily` with `CODEC`, `STREAM_CODEC`, `fromId(String)`, and `IRON_GOLEM("iron_golem")`.

Implement `GolemIdentity` as:

```java
public record GolemIdentity(GolemFamily family, GolemVariant variant) {
    public static final GolemIdentity DEFAULT_IRON = new GolemIdentity(GolemFamily.IRON_GOLEM, GolemVariant.IRON);
    public static GolemIdentity defaultIron() { return DEFAULT_IRON; }
    public static GolemIdentity ofIronVariant(GolemVariant variant) { return new GolemIdentity(GolemFamily.IRON_GOLEM, variant); }
    public boolean isDefaultIron() { return equals(DEFAULT_IRON); }
    public boolean isValidForPhase2() { return family == GolemFamily.IRON_GOLEM && variant != null; }
}
```

Add codecs for a record-shaped persistent attachment:

```java
public static final Codec<GolemIdentity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
    GolemFamily.CODEC.fieldOf("family").forGetter(GolemIdentity::family),
    GolemVariant.CODEC.fieldOf("variant").forGetter(GolemIdentity::variant)
).apply(instance, GolemIdentity::new));
```

Implement `GolemIdentityAttachment`:

```java
public static GolemIdentity get(Entity entity) {
    return resolve(storageFor(entity));
}

public static Optional<GolemIdentity> getRaw(Entity entity) {
    GolemIdentity attached = entity.getAttached(TYPE);
    return attached != null && attached.isValidForPhase2() ? Optional.of(attached) : Optional.empty();
}

public static void set(Entity entity, GolemIdentity identity) {
    write(storageFor(entity), identity);
    if (entity instanceof IronGolem golem) VariantAttributes.apply(golem);
}

static GolemIdentity resolve(GolemIdentityStorage storage) {
    Optional<GolemIdentity> attached = storage.rawIdentity();
    if (attached.isPresent() && attached.get().isValidForPhase2()) return attached.get();
    return storage.rawVariant()
        .map(GolemIdentity::ofIronVariant)
        .orElse(GolemIdentity.defaultIron());
}

static void write(GolemIdentityStorage storage, GolemIdentity identity) {
    if (identity == null || !identity.isValidForPhase2() || identity.isDefaultIron()) {
        storage.clearRawIdentity();
        storage.clearRawVariant();
    } else {
        storage.setRawIdentity(identity);
        storage.setRawVariant(identity.variant());
    }
}
```

Expose package-private old-only helpers in `GolemVariantAttachment` to avoid recursive `get`/`set` calls:

```java
static Optional<GolemVariant> getRawOld(Entity entity);
static void setOldOnly(Entity entity, GolemVariant variant);
static void clearOld(Entity entity);
```

Change public `GolemVariantAttachment.get(entity)` to return `GolemIdentityAttachment.get(entity).variant()`.
Change public `GolemVariantAttachment.set(entity, variant)` to call `GolemIdentityAttachment.set(entity, GolemIdentity.ofIronVariant(variant))`.

Register `GolemIdentityAttachment.touch()` before or next to `GolemVariantAttachment.touch()` in `MultiGolem.onInitialize()`.

- [ ] **Step 3.4: Verify GREEN**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.identity.GolemIdentityTest --tests dev.charles.multigolem.attachment.GolemIdentityAttachmentTest --tests dev.charles.multigolem.GolemVariantTest
```

Expected: pass.

---

### Task 4: Identity-Shaped Spawn Egg And Spawner Markers

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/spawn/SpawnEggStacks.java`
- Modify: `src/main/java/dev/charles/multigolem/spawn/SpawnerVariantMarker.java`
- Modify: `src/main/java/dev/charles/multigolem/spawn/SpawnEggVariantSpawner.java`
- Modify: `src/main/java/dev/charles/multigolem/mixin/SpawnEggItemMixin.java`
- Modify: `src/main/java/dev/charles/multigolem/mixin/BaseSpawnerMixin.java`
- Modify: `src/test/java/dev/charles/multigolem/spawn/SpawnEggStacksTest.java`
- Modify: `src/test/java/dev/charles/multigolem/spawn/SpawnerVariantMarkerTest.java`

- [ ] **Step 4.1: Write failing marker compatibility tests**

Add tests:

```java
@Test
void oldVariantOnlySpawnEggMarkerReadsAsIronFamilyIdentity() {
    ItemStack stack = markedEggWith("{multigolem:{variant:\"diamond\"}}");
    assertEquals(GolemIdentity.ofIronVariant(GolemVariant.DIAMOND), SpawnEggStacks.identityFrom(stack).orElseThrow());
}

@Test
void oldIronVariantMarkerReadsEmptyBecauseIronIsDefault() {
    ItemStack stack = markedEggWith("{multigolem:{variant:\"iron\"}}");
    assertTrue(SpawnEggStacks.identityFrom(stack).isEmpty(), "iron default marker must return empty");
}

@Test
void newFamilyVariantSpawnEggMarkerReadsIdentity() {
    ItemStack stack = markedEggWith("{multigolem:{family:\"iron_golem\",variant:\"zombie\"}}");
    assertEquals(GolemIdentity.ofIronVariant(GolemVariant.ZOMBIE), SpawnEggStacks.identityFrom(stack).orElseThrow());
}

@Test
void unknownFamilyOrUnknownVariantMarkerReadsEmpty() {
    assertTrue(SpawnEggStacks.identityFrom(markedEggWith("{multigolem:{family:\"copper_golem\",variant:\"diamond\"}}")).isEmpty());
    assertTrue(SpawnEggStacks.identityFrom(markedEggWith("{multigolem:{family:\"iron_golem\",variant:\"obsidian\"}}")).isEmpty());
}
```

Mirror the same cases for `SpawnerVariantMarker.readIdentity(CompoundTag)`.

- [ ] **Step 4.2: Verify RED**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.SpawnEggStacksTest --tests dev.charles.multigolem.spawn.SpawnerVariantMarkerTest
```

Expected: compile failure for `identityFrom`/`readIdentity` or assertion failure for new marker shapes.

- [ ] **Step 4.3: Implement identity marker read/write**

Keep writing old variant-only SNBT unless tests are adjusted deliberately, to preserve V4 resource predicates:

```java
multigolem.putString("variant", identity.variant().id());
```

Add readers:

```java
public static Optional<GolemIdentity> identityFrom(ItemStack stack) {
    CompoundTag multigolem = ...;
    String familyId = multigolem.getStringOr("family", GolemFamily.IRON_GOLEM.id());
    Optional<GolemFamily> family = GolemFamily.fromId(familyId);
    Optional<GolemVariant> variant = GolemVariant.fromId(multigolem.getStringOr("variant", ""));
    if (family.isEmpty() || variant.isEmpty()) return Optional.empty();
    GolemIdentity identity = new GolemIdentity(family.get(), variant.get());
    if (!identity.isValidForPhase2() || identity.isDefaultIron()) return Optional.empty();
    return Optional.of(identity);
}

public static Optional<GolemVariant> variantFrom(ItemStack stack) {
    return identityFrom(stack).map(GolemIdentity::variant);
}
```

Add equivalent `SpawnerVariantMarker.writeIdentity` and `readIdentity`; keep old `write/read` as variant compatibility wrappers.

Change spawn egg/spawner application paths to call `GolemIdentityAttachment.set(golem, identity)` once they have an identity. Preserve permission checks by using `identity.variant()` in Phase 2.

- [ ] **Step 4.4: Verify GREEN**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.SpawnEggStacksTest --tests dev.charles.multigolem.spawn.SpawnerVariantMarkerTest
```

Expected: pass.

---

### Task 5: Explicit Identity Mutation Paths

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/spawn/GolemCreationHandler.java`
- Modify: `src/main/java/dev/charles/multigolem/spawn/VillageGolemSpawnHandler.java`
- Modify: `src/main/java/dev/charles/multigolem/spawn/ZombieVillageSpawnHandler.java`
- Modify: `src/main/java/dev/charles/multigolem/spawn/SpawnEggVariantSpawner.java`
- Modify: `src/main/java/dev/charles/multigolem/mixin/BaseSpawnerMixin.java`
- Modify tests near each path as needed.

- [ ] **Step 5.1: Write failing mutation-path tests**

In existing handler tests, assert that explicit paths pass identity rather than only variant:

```java
assertEquals(GolemIdentity.ofIronVariant(GolemVariant.NETHERITE), applier.identity);
```

For pure tests where `IronGolem` is hard to instantiate, change test seams from `VillageVariantApplier.apply(golem, variant, origin)` to `VillageIdentityApplier.apply(golem, identity, origin)`.

- [ ] **Step 5.2: Verify RED**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.VillageGolemSpawnHandlerTest --tests dev.charles.multigolem.spawn.ZombieVillageSpawnHandlerTest
```

Expected: fail until handler seams use `GolemIdentity`.

- [ ] **Step 5.3: Implement central identity writes**

Use `GolemIdentity.ofIronVariant(variant)` at these explicit mutation points:

- player-built creation
- marked spawn egg spawn
- marked spawner spawn
- village spawn roll
- zombie village maintenance spawn

Each path should call `GolemIdentityAttachment.set(...)` exactly once for identity mutation and should not directly write the old attachment.

- [ ] **Step 5.4: Verify GREEN**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.VillageGolemSpawnHandlerTest --tests dev.charles.multigolem.spawn.ZombieVillageSpawnHandlerTest --tests dev.charles.multigolem.spawn.SpawnEggStacksTest --tests dev.charles.multigolem.spawn.SpawnerVariantMarkerTest
```

Expected: pass.

---

### Task 6: Passive Identity Reads Stay Non-Mutating

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/attribute/VariantAttributes.java`
- Modify: `src/main/java/dev/charles/multigolem/loot/HasGolemVariantLootCondition.java`
- Modify: `src/main/java/dev/charles/multigolem/ability/ZombieGolemFaction.java`
- Modify touched mixins only where useful for helper extraction.
- Modify/add tests for attachment passive reads and source checks.

- [ ] **Step 6.1: Write failing passive-read tests**

Add attachment tests for:

```java
@Test
void passiveVariantCompatibilityGetDoesNotClearOldOnlyData() {
    MapBackedIdentityStorage storage = oldOnlyStorage(GolemVariant.EMERALD);
    assertEquals(GolemVariant.EMERALD, GolemIdentityAttachment.resolve(storage).variant());
    assertEquals(Optional.of(GolemVariant.EMERALD), storage.rawVariant());
}

@Test
void passiveInvalidNewIdentityReadFallsBackToOldWithoutClearingEitherAttachment() {
    MapBackedIdentityStorage storage = invalidNewAndOldStorage(GolemVariant.EMERALD);
    assertEquals(GolemIdentity.ofIronVariant(GolemVariant.EMERALD), GolemIdentityAttachment.resolve(storage));
    assertEquals(Optional.of(GolemVariant.EMERALD), storage.rawVariant());
}
```

If invalid identity construction cannot be represented through codecs without corrupt NBT, use a package-private test hook that sets an invalid raw identity only in test scope.

- [ ] **Step 6.2: Verify RED**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.attachment.GolemIdentityAttachmentTest --tests dev.charles.multigolem.attribute.VariantAttributesTest --tests dev.charles.multigolem.ability.ZombieGolemFactionTest
```

Expected: fail until passive reads use non-mutating helpers and invalid-new fallback is represented.

- [ ] **Step 6.3: Extract small identity-aware read helpers**

Use helpers where they reduce duplicate logic:

```java
public static GolemVariant variantOf(Entity entity) {
    return GolemIdentityAttachment.get(entity).variant();
}

public static boolean isVariant(Entity entity, GolemVariant variant) {
    return variantOf(entity) == variant;
}
```

Keep `GolemVariantAttachment.get(entity)` as the public compatibility shim, but prefer direct identity helper calls in files already being modified for identity.

Do not call `GolemIdentityAttachment.set` from passive paths.

- [ ] **Step 6.4: Verify GREEN**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.attachment.GolemIdentityAttachmentTest --tests dev.charles.multigolem.attribute.VariantAttributesTest --tests dev.charles.multigolem.ability.ZombieGolemFactionTest --tests dev.charles.multigolem.mixin.GolemTargetingMixinTest
```

Expected: pass.

---

### Task 7: Scoped Beta Notes And Full Verification

**Files:**
- Modify: `CHANGELOG.md` or `INTERNAL_CHANGELOG.md`
- Review: `gradle.properties`

- [ ] **Step 7.1: Add scoped beta/refactor note**

Add an Unreleased bullet that says this branch contains the Phase 1+2 identity/catalog refactor and branch builds should be treated as beta validation builds. Do not bump release versions unless a release-prep task explicitly asks for it.

- [ ] **Step 7.2: Run targeted suite**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.catalog.GolemVariantCatalogTest --tests dev.charles.multigolem.identity.GolemIdentityTest --tests dev.charles.multigolem.attachment.GolemIdentityAttachmentTest --tests dev.charles.multigolem.spawn.SpawnEggStacksTest --tests dev.charles.multigolem.spawn.SpawnerVariantMarkerTest --tests dev.charles.multigolem.MultiGolemRegistrationTest --tests dev.charles.multigolem.permissions.MultiGolemPermissionsTest --tests dev.charles.multigolem.attribute.VariantAttributesTest
```

Expected: pass.

- [ ] **Step 7.3: Run required closeout commands**

Run exactly:

```powershell
git rev-parse --show-toplevel
.\gradlew.bat --quiet test
.\gradlew.bat build
```

Expected:

- root is the assigned worktree
- tests pass
- build succeeds
- beta jar exists under `build/libs/`

---

## Revue Gates

- [ ] Send this plan through Revue as `implementation-plan-review` before implementation.
- [ ] Use `superpowers:receiving-code-review` before changing the plan for any Revue findings.
- [ ] After implementation and local verification, send the final diff through Revue as `implementation-review`.
- [ ] Use `superpowers:receiving-code-review` before actioning final findings.
- [ ] Run the closeout commands again after any finding fixes.

## Self-Review

- Phase 1 coverage: catalog entries, catalog-derived set helpers, spawn eggs, loot, buildable variants, heal items, permissions, render/assets, and registration intent are covered in Tasks 1 and 2.
- Phase 2 coverage: `GolemFamily`, `GolemIdentity`, identity attachment, lazy migration, old attachment compatibility, dual-write, default clear, passive-read non-mutation, and marker validation are covered in Tasks 3 through 6.
- Explicit exclusion: `VillageSpawnWeights.rollOrder()` remains a hand-authored gameplay distribution list.
- Phase 3 exclusion: no Copper Golem family, tarnish/weathering persistence, or surface state runtime model is planned here.
- Placeholder scan: no `TBD`, `TODO`, or unspecified edge handling remains in this plan.
