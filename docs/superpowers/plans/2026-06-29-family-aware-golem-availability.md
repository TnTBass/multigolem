# Family-Aware Golem Availability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a server-authoritative `golem_availability` config that can disable whole golem families or individual family/variant identities without deleting existing golems.

**Architecture:** Keep availability as loader-neutral common code keyed by `GolemIdentity` or `(GolemFamily, GolemVariant)`. Parse and canonicalize the new config beside existing `MultiGolemConfig` data, expose one availability API, and thread it into catalog, spawn, village, zombie-village, and customization surfaces without flattening family/variant identity. Loader code should only wire events or lifecycle hooks and delegate to common behavior.

**Tech Stack:** Java 25, Minecraft 26.2 mappings used by this repo, Gson config parsing, JUnit tests through `.\gradlew.bat`, common/Fabric/NeoForge source roots.

**Planning Boundary:** This document is the only artifact to change in the planning session that created it. All Java, JSON, and PowerShell blocks below are instructions for a later implementation session; do not apply them until a separate implementation session is explicitly started.

---

## Startup And Boundaries

- Work from repository root `C:\Users\tyler\AI Projects\MultiGolem`.
- Before editing in any worktree, run `git rev-parse --show-toplevel` and confirm it resolves to the assigned worktree root.
- Read scoped `AGENTS.md` files before editing source roots:
  - `src/common/AGENTS.md`: common owns loader-neutral behavior and must not import loader APIs.
  - `src/commonClient/AGENTS.md`: common client owns loader-neutral screens/state only.
  - `src/fabric/AGENTS.md`: Fabric owns Fabric event/config/networking glue and delegates behavior to common.
  - `src/neoforge/AGENTS.md`: NeoForge owns NeoForge glue and delegates behavior to common.
- Do not delete, despawn, migrate, or mutate existing world golems as part of availability. This feature blocks only future creation or future listed surfaces.
- Treat availability as stronger than permissions: check availability first, then permissions only for identities that remain available.

## File Structure

- Create `src/common/java/dev/charles/multigolem/config/GolemAvailability.java`
  - Immutable family/variant availability model.
  - Public checks for `GolemIdentity`, `GolemFamily`, and `(GolemFamily, GolemVariant)`.
  - Unknown family/variant preservation helpers for canonicalization.
- Modify `src/common/java/dev/charles/multigolem/config/MultiGolemConfig.java`
  - Add `golemAvailability` field/accessor/defaults.
  - Merge, canonicalize, parse, and write `golem_availability`.
  - Preserve unknown keys when practical and warn while ignoring them at runtime.
- Modify `src/common/java/dev/charles/multigolem/catalog/GolemVariantCatalog.java`
  - Keep `GolemVariantSpec.family()` as the source family; add identity-aware lookup/filter helpers.
  - Do not introduce a flat variant-only availability namespace.
- Modify `src/common/java/dev/charles/multigolem/catalog/GolemVariantSpec.java`
  - Add a convenience `identity()` method returning `new GolemIdentity(family, variant)`.
- Modify `src/common/java/dev/charles/multigolem/spawn/VillageSpawnWeights.java`
  - Add availability-aware roll helpers that filter by `GolemAvailability`.
  - Preserve existing variant-only roll API for callers/tests that are not availability-aware yet.
- Modify `src/common/java/dev/charles/multigolem/spawn/VillageGolemSpawnResolver.java`
  - Filter village rolls by config availability.
- Modify `src/common/java/dev/charles/multigolem/spawn/VillageGolemSpawnHandler.java`
  - Apply village identities, not just variants, while still spawning vanilla/default Iron when the roll is empty.
- Modify `src/common/java/dev/charles/multigolem/spawn/ZombieVillageSpawnHandler.java`
  - Refuse to spawn new zombie golems when `iron_golem/zombie` is unavailable.
- Modify `src/common/java/dev/charles/multigolem/spawn/SpawnEggVariantSpawner.java`
  - Reject disabled marked spawn egg identities before permission checks.
- Modify `src/common/java/dev/charles/multigolem/mixin/SpawnEggItemMixin.java`
  - Reject disabled marked spawn eggs on spawner marking before permission checks.
- Modify `src/common/java/dev/charles/multigolem/mixin/BaseSpawnerMixin.java`
  - Refuse spawner-driven spawning for disabled marked identities before adding the entity.
- Modify `src/common/java/dev/charles/multigolem/customizations/ServerCustomizationsSnapshot.java`
  - Carry availability state or disabled identity summaries through the server payload.
- Modify `src/common/java/dev/charles/multigolem/customizations/ServerCustomizationsSummarizer.java`
  - Omit or mark disabled identities consistently when the server config is known.
- Modify `src/common/java/dev/charles/multigolem/golempedia/GolempediaCatalog.java`
  - Add an availability-filtered view for server-known surfaces. Keep static/client-prejoin catalog behavior honest.
- Modify `src/fabric/java/dev/charles/multigolem/fabric/event/FabricMultiGolemEvents.java`
  - Keep creative-tab insertion as static unless a server-known client state is available at registration time; do not promise server-only config can hide pre-join entries.
- Modify NeoForge event/creative-tab code discovered by `rg -n "CreativeModeTab|SpawnEggStacks.create|creativeSpawnEggVariants" src/neoforge src/common src/fabric`
  - Mirror Fabric behavior without duplicating common availability rules.
- Modify/add tests under:
  - `src/test/java/dev/charles/multigolem/config/`
  - `src/test/java/dev/charles/multigolem/catalog/`
  - `src/test/java/dev/charles/multigolem/spawn/`
  - `src/test/java/dev/charles/multigolem/customizations/`
  - `src/test/java/dev/charles/multigolem/neoforge/` and Fabric source-boundary tests only if loader glue changes.

---

### Task 1: Config Model, Defaults, Parsing, And Canonicalization

**Files:**
- Create: `src/common/java/dev/charles/multigolem/config/GolemAvailability.java`
- Modify: `src/common/java/dev/charles/multigolem/config/MultiGolemConfig.java`
- Test: `src/test/java/dev/charles/multigolem/config/MultiGolemConfigTest.java`

- [ ] **Step 1.1: Write failing default and override tests**

Add tests to `MultiGolemConfigTest`:

```java
@Test
void golemAvailabilityDefaultsEnableKnownCatalogIdentities() {
    MultiGolemConfig cfg = MultiGolemConfig.defaults();

    assertTrue(cfg.golemAvailability().isAvailable(GolemIdentity.ofIronVariant(GolemVariant.COPPER)));
    assertTrue(cfg.golemAvailability().isAvailable(GolemIdentity.ofIronVariant(GolemVariant.DIAMOND)));
    assertTrue(cfg.golemAvailability().isAvailable(GolemIdentity.ofIronVariant(GolemVariant.ZOMBIE)));
}

@Test
void golemAvailabilityDisabledFamilyOverridesEnabledVariants(@TempDir Path tmp) throws IOException {
    Path file = tmp.resolve("multigolem.json");
    Files.writeString(file, """
        {
          "golem_availability": {
            "iron_golem": {
              "enabled": false,
              "variants": {
                "copper": true,
                "diamond": true
              }
            }
          }
        }
        """);

    MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);

    assertFalse(cfg.golemAvailability().isAvailable(GolemIdentity.ofIronVariant(GolemVariant.COPPER)));
    assertFalse(cfg.golemAvailability().isAvailable(GolemIdentity.ofIronVariant(GolemVariant.DIAMOND)));
}

@Test
void golemAvailabilityDisabledVariantOnlyBlocksThatFamilyVariant(@TempDir Path tmp) throws IOException {
    Path file = tmp.resolve("multigolem.json");
    Files.writeString(file, """
        {
          "golem_availability": {
            "iron_golem": {
              "enabled": true,
              "variants": {
                "diamond": false
              }
            }
          }
        }
        """);

    MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);

    assertTrue(cfg.golemAvailability().isAvailable(GolemIdentity.ofIronVariant(GolemVariant.COPPER)));
    assertFalse(cfg.golemAvailability().isAvailable(GolemIdentity.ofIronVariant(GolemVariant.DIAMOND)));
    assertTrue(cfg.golemAvailability().isAvailable(GolemIdentity.ofIronVariant(GolemVariant.NETHERITE)));
}

@Test
void golemAvailabilityCanonicalizationPreservesUnknownKeys(@TempDir Path tmp) throws IOException {
    Path file = tmp.resolve("multigolem.json");
    Files.writeString(file, """
        {
          "golem_availability": {
            "iron_golem": {
              "enabled": true,
              "variants": {
                "diamond": false,
                "obsidian": false
              }
            },
            "future_golem": {
              "enabled": false,
              "variants": {
                "brass": false
              }
            }
          }
        }
        """);

    MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
    String rewritten = Files.readString(file);

    assertFalse(cfg.golemAvailability().isAvailable(GolemIdentity.ofIronVariant(GolemVariant.DIAMOND)));
    assertTrue(rewritten.contains("\"future_golem\""));
    assertTrue(rewritten.contains("\"obsidian\""));
    assertTrue(rewritten.contains("\"brass\""));
}
```

Imports needed:

```java
import dev.charles.multigolem.identity.GolemIdentity;
```

- [ ] **Step 1.2: Run config tests and verify they fail for missing API**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.config.MultiGolemConfigTest
```

Expected: FAIL with compile errors for `golemAvailability()` or `GolemAvailability` missing.

- [ ] **Step 1.3: Implement `GolemAvailability`**

Create `GolemAvailability` with this public shape:

```java
public final class GolemAvailability {
    public static GolemAvailability defaults();
    public static FamilyAvailability familyDefault(GolemFamily family);
    public boolean isFamilyAvailable(GolemFamily family);
    public boolean isAvailable(GolemIdentity identity);
    public boolean isAvailable(GolemFamily family, GolemVariant variant);
    public Map<GolemFamily, FamilyAvailability> knownFamilies();

    public record FamilyAvailability(
        boolean enabled,
        EnumMap<GolemVariant, Boolean> variants
    ) {
        public boolean isAvailable(GolemVariant variant) {
            return enabled && variants.getOrDefault(variant, true);
        }
    }
}
```

Implementation requirements:

- `defaults()` enables every known `GolemFamily` and every cataloged variant for that family.
- `isAvailable(GolemIdentity)` returns `false` for `null`, null family, null variant, or family/variant pairs not present in `GolemVariantCatalog`.
- Missing known family and variant keys from config parse as enabled.
- Disabled family always wins over variant entries.

- [ ] **Step 1.4: Wire `MultiGolemConfig` field and parser**

Update `MultiGolemConfig`:

```java
private final GolemAvailability golemAvailability;

public GolemAvailability golemAvailability() {
    return golemAvailability;
}
```

Update constructors, `defaults()`, `forTesting(...)`, `equals`, and `hashCode` to include `golemAvailability`. Prefer adding an overloaded `forTesting(...)` that accepts availability while keeping the current helper as a default-enabled convenience for existing tests.

Add JSON output:

```java
root.add("golem_availability", golemAvailabilityToJson(cfg.golemAvailability));
```

Add parse/canonicalize helpers:

```java
private static GolemAvailability parseGolemAvailability(JsonObject root) { ... }
private static void canonicalizeGolemAvailabilityInPlace(JsonObject root) { ... }
private static JsonObject golemAvailabilityToJson(GolemAvailability availability) { ... }
```

Canonical JSON shape:

```json
"golem_availability": {
  "iron_golem": {
    "enabled": true,
    "variants": {
      "copper": true,
      "iron": true,
      "redstone": true,
      "gold": true,
      "emerald": true,
      "diamond": true,
      "netherite": true,
      "zombie": true
    }
  },
  "copper_golem": {
    "enabled": true,
    "variants": {}
  }
}
```

Warn and ignore at runtime:

```java
MultiGolem.LOG.warn("unknown golem_availability family key '{}'; preserved but ignored", key);
MultiGolem.LOG.warn("unknown golem_availability.{}.variants key '{}'; preserved but ignored", familyKey, variantKey);
MultiGolem.LOG.warn("golem_availability.{}.enabled is not a boolean; using true", familyKey);
MultiGolem.LOG.warn("golem_availability.{}.variants.{} is not a boolean; using true", familyKey, variantKey);
```

- [ ] **Step 1.5: Run config tests and commit**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.config.MultiGolemConfigTest
```

Expected: PASS.

Commit:

```powershell
git add src/common/java/dev/charles/multigolem/config/GolemAvailability.java src/common/java/dev/charles/multigolem/config/MultiGolemConfig.java src/test/java/dev/charles/multigolem/config/MultiGolemConfigTest.java
git commit -m "feat: add golem availability config"
```

---

### Task 2: Identity-Aware Catalog And Village Availability Filtering

**Files:**
- Modify: `src/common/java/dev/charles/multigolem/catalog/GolemVariantSpec.java`
- Modify: `src/common/java/dev/charles/multigolem/catalog/GolemVariantCatalog.java`
- Modify: `src/common/java/dev/charles/multigolem/spawn/VillageSpawnWeights.java`
- Modify: `src/common/java/dev/charles/multigolem/spawn/VillageGolemSpawnResolver.java`
- Modify: `src/common/java/dev/charles/multigolem/spawn/VillageGolemSpawnHandler.java`
- Test: `src/test/java/dev/charles/multigolem/catalog/GolemVariantCatalogTest.java`
- Test: `src/test/java/dev/charles/multigolem/spawn/VillageSpawnWeightsTest.java`

- [ ] **Step 2.1: Write failing catalog identity tests**

Add to `GolemVariantCatalogTest`:

```java
@Test
void catalogEntriesExposeFamilyVariantIdentity() {
    assertEquals(
        GolemIdentity.ofIronVariant(GolemVariant.COPPER),
        GolemVariantCatalog.require(GolemVariant.COPPER).identity()
    );
    assertEquals(
        new GolemIdentity(GolemFamily.IRON_GOLEM, GolemVariant.ZOMBIE),
        GolemVariantCatalog.require(GolemVariant.ZOMBIE).identity()
    );
}

@Test
void catalogCanFilterAvailableIdentitiesWithoutFlatteningFamilies() {
    GolemAvailability availability = GolemAvailability.defaults()
        .withVariant(GolemFamily.IRON_GOLEM, GolemVariant.DIAMOND, false);

    List<GolemIdentity> identities = GolemVariantCatalog.identitiesWhereAvailable(availability);

    assertTrue(identities.contains(GolemIdentity.ofIronVariant(GolemVariant.COPPER)));
    assertFalse(identities.contains(GolemIdentity.ofIronVariant(GolemVariant.DIAMOND)));
}
```

If immutable builder names differ, keep this semantic shape and define `withVariant(...)` or a package-private test factory in Task 1.

- [ ] **Step 2.2: Write failing village filtering tests**

Add to `VillageSpawnWeightsTest`:

```java
@Test
void rollAvailableSkipsDisabledVariants() {
    EnumMap<GolemVariant, Integer> map = new EnumMap<>(GolemVariant.class);
    for (GolemVariant variant : GolemVariant.values()) {
        map.put(variant, 0);
    }
    map.put(GolemVariant.COPPER, 10);
    map.put(GolemVariant.DIAMOND, 10);
    VillageSpawnWeights weights = new VillageSpawnWeights(true, map);
    GolemAvailability availability = GolemAvailability.defaults()
        .withVariant(GolemFamily.IRON_GOLEM, GolemVariant.COPPER, false);

    assertEquals(Optional.of(GolemIdentity.ofIronVariant(GolemVariant.DIAMOND)),
        weights.rollAvailable(availability, bound -> 0));
}

@Test
void rollAvailableReturnsEmptyWhenAllPositiveWeightsAreDisabled() {
    EnumMap<GolemVariant, Integer> map = new EnumMap<>(GolemVariant.class);
    for (GolemVariant variant : GolemVariant.values()) {
        map.put(variant, 0);
    }
    map.put(GolemVariant.COPPER, 10);
    VillageSpawnWeights weights = new VillageSpawnWeights(true, map);
    GolemAvailability availability = GolemAvailability.defaults()
        .withVariant(GolemFamily.IRON_GOLEM, GolemVariant.COPPER, false);

    assertEquals(Optional.empty(), weights.rollAvailable(availability, bound -> fail("no available weight should remain")));
}
```

Imports needed:

```java
import dev.charles.multigolem.config.GolemAvailability;
import dev.charles.multigolem.identity.GolemFamily;
import dev.charles.multigolem.identity.GolemIdentity;
```

- [ ] **Step 2.3: Run focused tests and verify they fail**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.catalog.GolemVariantCatalogTest --tests dev.charles.multigolem.spawn.VillageSpawnWeightsTest
```

Expected: FAIL with missing `identity()`, `identitiesWhereAvailable(...)`, and `rollAvailable(...)`.

- [ ] **Step 2.4: Implement catalog identity helpers**

Add to `GolemVariantSpec`:

```java
public GolemIdentity identity() {
    return new GolemIdentity(family, variant);
}
```

Add to `GolemVariantCatalog`:

```java
public static boolean contains(GolemFamily family, GolemVariant variant) { ... }
public static Optional<GolemVariantSpec> find(GolemFamily family, GolemVariant variant) { ... }
public static List<GolemIdentity> identitiesWhereAvailable(GolemAvailability availability) { ... }
```

Implementation details:

- `contains(...)` checks family and variant together.
- `find(...)` returns a spec only when both match.
- `identitiesWhereAvailable(...)` maps available specs to `spec.identity()`.

- [ ] **Step 2.5: Implement village availability roll**

Add to `VillageSpawnWeights`:

```java
public Optional<GolemIdentity> rollAvailable(GolemAvailability availability, IntUnaryOperator nextIntBounded) {
    if (!enabled) return Optional.empty();

    int total = 0;
    for (GolemVariant variant : ROLL_ORDER) {
        GolemIdentity identity = GolemIdentity.ofIronVariant(variant);
        if (availability.isAvailable(identity)) {
            total += weight(variant);
        }
    }
    if (total <= 0) return Optional.empty();

    int ticket = nextIntBounded.applyAsInt(total);
    int cursor = 0;
    for (GolemVariant variant : ROLL_ORDER) {
        GolemIdentity identity = GolemIdentity.ofIronVariant(variant);
        if (!availability.isAvailable(identity)) continue;
        cursor += weight(variant);
        if (ticket < cursor) return Optional.of(identity);
    }
    return Optional.empty();
}
```

Update `VillageGolemSpawnResolver` to call `config.villageSpawnWeights().rollAvailable(config.golemAvailability(), random)` and return `Optional<GolemIdentity>` or keep a variant convenience wrapper only at the boundary. Update `VillageGolemSpawnHandler` so it applies `GolemIdentity` to `GolemIdentityAttachment`; skip attachment for default Iron as it does today.

- [ ] **Step 2.6: Run focused tests and commit**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.catalog.GolemVariantCatalogTest --tests dev.charles.multigolem.spawn.VillageSpawnWeightsTest
```

Expected: PASS.

Commit:

```powershell
git add src/common/java/dev/charles/multigolem/catalog src/common/java/dev/charles/multigolem/spawn src/test/java/dev/charles/multigolem/catalog src/test/java/dev/charles/multigolem/spawn
git commit -m "feat: filter village golem rolls by availability"
```

---

### Task 3: Server-Authoritative Creation Blocking Before Permissions

**Files:**
- Modify: `src/common/java/dev/charles/multigolem/spawn/SpawnEggVariantSpawner.java`
- Modify: `src/common/java/dev/charles/multigolem/mixin/SpawnEggItemMixin.java`
- Modify: `src/common/java/dev/charles/multigolem/mixin/BaseSpawnerMixin.java`
- Modify: `src/common/java/dev/charles/multigolem/spawn/ZombieVillageSpawnHandler.java`
- Modify player-construction hook files found by:
  - `rg -n "canCreate|sendCreateDenied|playerBuildable|matchesBodyBlock|trySpawnMob|IronGolem" src/common src/fabric src/neoforge`
- Test: `src/test/java/dev/charles/multigolem/spawn/SpawnEggStacksTest.java`
- Test: add focused source tests under `src/test/java/dev/charles/multigolem/spawn/` if runtime spawn tests are too heavy.

- [ ] **Step 3.1: Add a common denial helper test seam**

Create or extend a small common helper so heavy mixins call one method:

```java
public final class GolemAvailabilityGuards {
    public static boolean canCreate(MultiGolemConfig config, GolemIdentity identity) {
        return config.golemAvailability().isAvailable(identity);
    }

    public static boolean canCreate(MultiGolemConfig config, GolemFamily family, GolemVariant variant) {
        return config.golemAvailability().isAvailable(family, variant);
    }
}
```

Write tests proving:

```java
assertFalse(GolemAvailabilityGuards.canCreate(configWithDiamondDisabled, GolemIdentity.ofIronVariant(GolemVariant.DIAMOND)));
assertTrue(GolemAvailabilityGuards.canCreate(configWithDiamondDisabled, GolemIdentity.ofIronVariant(GolemVariant.COPPER)));
```

- [ ] **Step 3.2: Run guard tests and verify they fail**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.GolemAvailabilityGuardsTest
```

Expected: FAIL until the helper exists.

- [ ] **Step 3.3: Block marked spawn egg spawning before permissions**

In `SpawnEggVariantSpawner.spawnMarkedOrVanilla(...)`, after reading identity and confirming Iron Golem type, add:

```java
if (!MultiGolem.config().golemAvailability().isAvailable(identity.get())) {
    if (user instanceof ServerPlayer player) {
        MultiGolemPermissions.sendCreateDenied(player, identity.get().variant());
    }
    return null;
}
```

Keep the existing permission check after this block.

- [ ] **Step 3.4: Block spawner marking before permissions**

In `SpawnEggItemMixin.multigolem$denyMarkedSpawnerUse(...)`, check availability before extracting `variant` for permissions:

```java
GolemIdentity requested = identity.get();
if (!MultiGolem.config().golemAvailability().isAvailable(requested)) {
    MultiGolemPermissions.sendCreateDenied(player, requested.variant());
    cir.setReturnValue(InteractionResult.FAIL);
    cir.cancel();
    return;
}
```

Then run the existing permission check for available identities.

- [ ] **Step 3.5: Block spawner-driven spawning**

In `BaseSpawnerMixin.multigolem$applyVariantBeforeSpawnerAdd(...)`, when `MULTIGOLEM_SPAWNER_IDENTITY` is present and unavailable:

```java
if (identity.isPresent() && !MultiGolem.config().golemAvailability().isAvailable(identity.get())) {
    return false;
}
```

Do this before setting attachments or calling `level.tryAddFreshEntityWithPassengers(entity)`.

- [ ] **Step 3.6: Block zombie village spawning**

In `ZombieVillageSpawnHandler.maintain(...)` or before runtime spawn attempts:

```java
if (!MultiGolem.config().golemAvailability().isAvailable(GolemIdentity.ofIronVariant(GolemVariant.ZOMBIE))) {
    return;
}
```

Put the check outside the `while` loop so disabled zombie golems do not run spawn position scans.

- [ ] **Step 3.7: Block player-built golems before permissions**

Use:

```powershell
rg -n "canCreate|sendCreateDenied|playerBuildable|matchesBodyBlock|trySpawnMob|IronGolem" src/common src/fabric src/neoforge
```

Find the player construction path that identifies a body block/spec. Insert availability before any `MultiGolemPermissions.canCreate(...)` call:

```java
GolemIdentity identity = spec.identity();
if (!MultiGolem.config().golemAvailability().isAvailable(identity)) {
    MultiGolemPermissions.sendCreateDenied(player, identity.variant());
    return false;
}
```

If no player object exists at that layer, return a structured denial from common code and let the loader/event layer send the existing denial message.

- [ ] **Step 3.8: Add source-boundary tests for hook ordering**

If direct runtime tests are too expensive, add source tests asserting the guard appears before permission checks:

```java
String source = Files.readString(Path.of("src/common/java/dev/charles/multigolem/spawn/SpawnEggVariantSpawner.java"));
assertTrue(source.indexOf("golemAvailability().isAvailable") < source.indexOf("MultiGolemPermissions.canCreate"));
```

Add equivalent targeted tests for `SpawnEggItemMixin` and the player-construction hook file. Keep these tests narrow and delete them later only if real runtime tests replace them.

- [ ] **Step 3.9: Run spawn and boundary tests and commit**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.* --tests dev.charles.multigolem.permissions.* --tests dev.charles.multigolem.neoforge.NeoForgeEventsSourceTest --tests dev.charles.multigolem.ability.FabricAbilityEventsSourceTest
```

Expected: PASS.

Commit:

```powershell
git add src/common/java/dev/charles/multigolem src/test/java/dev/charles/multigolem
git commit -m "feat: block disabled golem creation paths"
```

---

### Task 4: Client-Facing Summaries, Golempedia, And Creative-Tab Honesty

**Files:**
- Modify: `src/common/java/dev/charles/multigolem/customizations/ServerCustomizationsSnapshot.java`
- Modify: `src/common/java/dev/charles/multigolem/customizations/ServerCustomizationsPayload.java`
- Modify: `src/common/java/dev/charles/multigolem/customizations/ServerCustomizationsSummarizer.java`
- Modify: `src/common/java/dev/charles/multigolem/golempedia/GolempediaCatalog.java`
- Modify: `src/commonClient/java/dev/charles/multigolem/client/modmenu/ServerCustomizationsScreen.java` only if display grouping needs explicit disabled wording.
- Modify: `src/fabric/java/dev/charles/multigolem/fabric/event/FabricMultiGolemEvents.java` only to document/static-filter server-known limitations in code.
- Test: `src/test/java/dev/charles/multigolem/config/ServerCustomizationsSummarizerTest.java`
- Test: `src/test/java/dev/charles/multigolem/customizations/ServerCustomizationsPayloadTest.java`
- Test: `src/test/java/dev/charles/multigolem/golempedia/GolempediaCatalogTest.java`

- [ ] **Step 4.1: Write failing customization summary tests**

Add to `ServerCustomizationsSummarizerTest`:

```java
@Test
void summaryMarksDisabledVariantsWhenServerAvailabilityIsKnown() {
    MultiGolemConfig cfg = MultiGolemConfig.defaults()
        .withGolemAvailability(GolemAvailability.defaults()
            .withVariant(GolemFamily.IRON_GOLEM, GolemVariant.DIAMOND, false));

    ServerCustomizationsSummary summary = ServerCustomizationsSummarizer.summary(cfg);

    assertTrue(summary.variantLines().stream().anyMatch(line -> line.contains("Diamond") && line.contains("disabled")));
    assertFalse(summary.villageLines().stream().anyMatch(line -> line.startsWith("Diamond:") && line.contains("roughly")));
}

@Test
void summaryMarksDisabledFamilyOnceWithoutListingEnabledVariantOverridesAsAvailable() {
    MultiGolemConfig cfg = MultiGolemConfig.defaults()
        .withGolemAvailability(GolemAvailability.defaults()
            .withFamily(GolemFamily.IRON_GOLEM, false)
            .withVariant(GolemFamily.IRON_GOLEM, GolemVariant.COPPER, true));

    ServerCustomizationsSummary summary = ServerCustomizationsSummarizer.summary(cfg);

    assertTrue(summary.variantLines().stream().anyMatch(line -> line.contains("Iron Golem family") && line.contains("disabled")));
    assertFalse(summary.variantLines().stream().anyMatch(line -> line.startsWith("Copper: Health:")));
}
```

- [ ] **Step 4.2: Run customization tests and verify they fail**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.config.ServerCustomizationsSummarizerTest --tests dev.charles.multigolem.customizations.ServerCustomizationsPayloadTest
```

Expected: FAIL until availability reaches snapshots/summaries.

- [ ] **Step 4.3: Extend server customization payload/snapshot**

Add availability summary data to `ServerCustomizationsSnapshot`, for example:

```java
List<String> disabledAvailabilityLines
```

or a structured list:

```java
List<DisabledGolemAvailabilitySummary>
```

Prefer structured records if payload compatibility tests can be kept deterministic. Include only server-known state. Do not imply this hides pre-join creative-tab entries.

- [ ] **Step 4.4: Filter or mark summaries**

In `ServerCustomizationsSummarizer`:

- Village lines: omit disabled identities from weight/roughly-active summaries, or mark them as `disabled by availability`.
- Variant lines: for disabled family, emit one family-level disabled line and skip per-variant stats under that family.
- Variant lines: for disabled variant under enabled family, emit `Diamond: disabled by server availability` instead of normal stats.
- Overrides: do not show disabled variants as active customization stats unless the line explicitly says disabled.

- [ ] **Step 4.5: Add availability-aware Golempedia view**

In `GolempediaCatalog`, add:

```java
public static List<GolempediaEntry> entriesAvailableToServer(MultiGolemConfig config)
```

This should filter by `config.golemAvailability()` when a server config/payload is available. Keep existing static catalog methods unchanged for client pre-join/static surfaces.

- [ ] **Step 4.6: Document creative-tab limitation in code path**

In Fabric and NeoForge creative spawn egg registration, do not use server-only config as if it can hide client pre-join entries. If a server-known client state is not available at creative tab build time, leave static behavior and rely on server rejection.

Add or update a source test asserting comments or behavior do not call `MultiGolem.config().golemAvailability()` from client/static creative-tab registration unless there is an actual server-known client state.

- [ ] **Step 4.7: Run summary/Golempedia tests and commit**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.config.ServerCustomizationsSummarizerTest --tests dev.charles.multigolem.customizations.* --tests dev.charles.multigolem.golempedia.*
```

Expected: PASS.

Commit:

```powershell
git add src/common/java/dev/charles/multigolem/customizations src/common/java/dev/charles/multigolem/golempedia src/commonClient/java/dev/charles/multigolem/client/modmenu src/fabric/java/dev/charles/multigolem/fabric/event src/neoforge/java src/test/java/dev/charles/multigolem
git commit -m "feat: surface disabled golems in server customizations"
```

---

### Task 5: Final Integration, Compatibility Checks, And Review

**Files:**
- Modify only files already touched by Tasks 1-4 unless final verification reveals a scoped defect.
- Documentation update only if implementation discovers a durable user-facing behavior that belongs in existing docs.

- [ ] **Step 5.1: Run targeted full feature tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.config.* --tests dev.charles.multigolem.catalog.* --tests dev.charles.multigolem.spawn.* --tests dev.charles.multigolem.customizations.* --tests dev.charles.multigolem.golempedia.* --tests dev.charles.multigolem.permissions.*
```

Expected: PASS.

- [ ] **Step 5.2: Run loader source-boundary tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.neoforge.* --tests dev.charles.multigolem.ability.FabricAbilityEventsSourceTest --tests dev.charles.multigolem.permissions.FabricPermissionsBoundarySourceTest
```

Expected: PASS.

- [ ] **Step 5.3: Run durable build gate**

Run:

```powershell
.\gradlew.bat build
```

Expected: PASS.

- [ ] **Step 5.4: Inspect final diff**

Run:

```powershell
git diff --stat
git diff --name-only
```

Expected changed paths are limited to:

- `src/common/java/dev/charles/multigolem/config/`
- `src/common/java/dev/charles/multigolem/catalog/`
- `src/common/java/dev/charles/multigolem/spawn/`
- `src/common/java/dev/charles/multigolem/mixin/`
- `src/common/java/dev/charles/multigolem/customizations/`
- `src/common/java/dev/charles/multigolem/golempedia/`
- loader glue only where creative tabs or construction hooks require it
- corresponding focused tests

- [ ] **Step 5.5: Request bounded review**

Request review over the implementation diff and include:

- Source design: `docs/superpowers/specs/2026-06-29-family-aware-golem-availability-design.md`
- This plan: `docs/superpowers/plans/2026-06-29-family-aware-golem-availability.md`
- Base commit: the commit before Task 1
- Head commit: the final implementation commit

Ask reviewers to check:

- Disabled family overrides enabled variant entries.
- Missing known families/variants default enabled.
- Unknown family/variant keys are warned, ignored at runtime, and preserved when practical.
- Existing spawned golems are untouched.
- Availability is checked before permissions.
- Village, zombie-village, spawn egg, spawner marking, spawner-driven spawn, and player construction paths are blocked.
- Client/static creative-tab limitations are represented honestly.

- [ ] **Step 5.6: Apply review feedback with `superpowers:receiving-code-review`**

For each finding:

- Verify it against code and tests.
- Fix valid findings with focused tests.
- Mark false positives only with concrete code/test evidence.
- Document accepted risk only when technically justified.

- [ ] **Step 5.7: Final commit**

Run:

```powershell
git status --short
```

Expected: only intended implementation/test/doc files are modified.

Commit:

```powershell
git add src docs
git commit -m "feat: add family-aware golem availability"
```

---

## Self-Review Checklist

- Config model covers family enable flags and variant overrides under `golem_availability`.
- Disabled family wins over enabled variants.
- Missing known families and variants default to enabled.
- Unknown family and variant keys are warned and ignored at runtime, while canonicalization preserves them when practical.
- Availability checks use `GolemIdentity` or `(GolemFamily, GolemVariant)`, not a flat `GolemVariant` namespace.
- Catalog integration preserves `GolemVariantSpec.family()`.
- New creation paths are covered: player-built golems, marked spawn egg spawning, spawner marking, spawner-driven spawning, village rolls, zombie village spawning, and future family-specific hooks through shared guard helpers.
- Existing spawned golems are not removed or migrated.
- Server-only config does not claim to hide pre-join client creative-tab entries.
- Availability is checked before permissions.
- Tests cover defaults, family-disabled override behavior, variant-disabled behavior, village roll filtering, construction/spawn egg blocking before permissions, and customization summaries.
