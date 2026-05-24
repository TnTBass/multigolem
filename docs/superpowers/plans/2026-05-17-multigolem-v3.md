# MultiGolem V3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship V3 village natural-spawn weighting so villager-called iron golems can become configured MultiGolem variants while all non-village spawn paths stay unchanged.

**Architecture:** V3 keeps every spawned defender as vanilla `IronGolem` and sets the existing `GolemVariantAttachment` only after vanilla successfully creates a villager-called golem. Pure Java config, weight, and resolver helpers decide whether to leave Iron or apply a non-iron variant; the mixin is a thin bridge from the spike-confirmed villager spawn call to `VillageGolemSpawnHandler`.

**Tech Stack:** Java 25 · Fabric Loader 0.19.2 · Fabric API 0.148.0+26.1.2 · Fabric Loom · Minecraft 26.1.2 with Mojang official mappings · JUnit 5 · Mixin · optional Mixin Extras only if Task 1 chooses `@WrapOperation`

**Reference spec:** `docs/superpowers/specs/2026-05-17-multigolem-v3-design.md`
**Plan style reference:** `docs/superpowers/plans/2026-05-16-multigolem-v2.md`
**API source-of-truth doc:** `docs/26.1.2-mojang-targets.md`
**Lessons learned:** `docs/LESSONS-LEARNED.md`

---

## File Structure (locks decomposition decisions)

**New Java files (`src/main/java/dev/charles/multigolem/`):**

```
spawn/
  VillageSpawnWeights.java             # Pure helper: enabled flag, canonical weights, all-zero disablement, deterministic roll API
  VillageGolemSpawnResolver.java       # Pure-ish helper: config -> roll result, keeps mixin away from config internals
  VillageGolemSpawnHandler.java        # Runtime bridge: apply rolled variant to newly spawned IronGolem and set full health
mixin/
  VillagerMixin.java                   # Or spike-confirmed equivalent class name; thin hook into villager help-call path
```

If Task 1 chooses a Mixin Extras `@WrapOperation`, add:

```
build.gradle                           # Add Mixin Extras dependency
```

Do not add Mixin Extras unless Task 1 proves the chosen strategy requires it.

**Modified Java files:**

```
config/MultiGolemConfig.java           # Add top-level village_spawning parsing, canonicalization, lossless merge
stats/GolemStatsResolver.java          # Add V3 accessors only if resolver callers need them
MultiGolem.java                        # Optional: no direct wiring unless runtime helper needs init/log touch
src/main/resources/multigolem.mixins.json # Add the spike-confirmed V3 mixin
```

**Tests (`src/test/java/dev/charles/multigolem/`):**

```
spawn/VillageSpawnWeightsTest.java
spawn/VillageGolemSpawnResolverTest.java
config/MultiGolemConfigV3Test.java
```

**Docs:**

```
docs/26.1.2-mojang-targets.md          # Task 1 spike evidence and chosen capture strategy
docs/playtest-checklist.md             # V3 manual rows
docs/playtest.html                     # Same V3 rows in browser checklist
CHANGELOG.md                           # Unreleased player/admin-facing note
```

---

## Conventions

- **Scope stays narrow:** only villager-called golems roll V3 weights. Do not include player-built golems, mob spawners, spawn eggs, commands, existing golems, V4 spawn eggs, or V5 copper golem variants.
- **Spike first:** Task 1 blocks all code work. If it cannot prove a narrow villager help-call hook and a safe spawned-golem capture strategy, stop for design review.
- **TDD where it applies:** `VillageSpawnWeights`, `VillageGolemSpawnResolver`, and `MultiGolemConfig` V3 behavior get failing tests before implementation.
- **Config style:** extend the existing lossless `JsonObject` merge/canonicalize pattern in `MultiGolemConfig`. Preserve unknown user fields.
- **Attachment semantics:** Iron means no special attachment is required. Non-iron village outcomes call `GolemVariantAttachment.set(golem, variant)` and then set current health to the configured max.
- **Natural defender semantics:** never call `setPlayerCreated(true)` in V3. Village variants remain natural defenders.
- **Changelog voice:** write for players and server admins, not implementation details.
- **Commits:** each task ends with a focused commit. Push only if the active execution workflow asks for pushes.

---

## Task 1: Blocking V3 source spike and target-note update

No mod code is written until this task commits. This task must choose one capture strategy from the V3 spec §13 candidates and update `docs/26.1.2-mojang-targets.md` with evidence. If the only viable hook is broad enough to catch mob spawners, spawn eggs, commands, or unrelated mod-created golems, stop for design review.

**Files:**
- Modify: `docs/26.1.2-mojang-targets.md`

- [ ] **Step 1.1: Ensure decompiled sources are present**

Run:

```powershell
.\gradlew.bat --quiet genSources
```

Expected: exit 0. Common sources exist under `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-common-*/26.1.2/*-sources.jar`.

- [ ] **Step 1.2: Locate villager-called golem logic**

Find candidate source paths:

```powershell
Get-ChildItem -Path .gradle -Recurse -Filter "*sources.jar" |
  Select-String -Pattern "minecraft-common"
```

Then inspect the source jar for villager and golem references:

```powershell
jar tf .gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-common-a26c9a9f3c\26.1.2\minecraft-common-a26c9a9f3c-26.1.2-sources.jar |
  Select-String -Pattern "Villager|Golem|Behavior"
```

Extract and inspect `net/minecraft/world/entity/npc/Villager.java` first. Document whether villager golem spawning is still a named method on `Villager.class` itself. Specifically verify whether `spawnGolemIfNeeded` exists, its full signature, and whether it directly calls `SpawnUtil.trySpawnMob`.

If the logic moved into a behavior/task/helper/lambda, document the actual class, method, and why it is still inside the villager help-call path.

- [ ] **Step 1.3: Verify spawn reason and `SpawnUtil.trySpawnMob` return type**

Inspect the call site and `net/minecraft/util/SpawnUtil.java`.

Document:
- exact spawn reason used for villager-called golems. `EntitySpawnReason.MOB_SUMMONED` is only a pre-spike assumption.
- exact `SpawnUtil.trySpawnMob` signature and descriptor.
- exact return type, such as `Optional<T>`, nullable `T`, or another shape.
- whether the returned value can be captured at the call site without broad entity events.

- [ ] **Step 1.4: Evaluate and choose one spawned-golem capture strategy**

Evaluate all three candidates from the spec and document the chosen strategy:

- Candidate A: `@Local` capture in a `TAIL` inject on the named villager method.
- Candidate B: `@WrapOperation` or `@Redirect` on `SpawnUtil.trySpawnMob`, including whether Mixin Extras is required.
- Candidate C: thread-local flag plus entity event fallback, only with explicit review.

Required decision rules:
- Prefer a strategy that receives the actual created `IronGolem` return value from `SpawnUtil.trySpawnMob`.
- Keep the hook inside the villager help-call path.
- Reject any strategy that catches mob spawners, spawn eggs, commands, or unrelated mod-created golems.
- If Candidate C is the only viable approach, stop and ask for explicit review before implementation.

- [ ] **Step 1.5: Verify natural defender status**

Inspect the villager spawn path for any `setPlayerCreated` call. Document that V3 should not set `playerCreated`, and that the vanilla villager path leaves village defenders natural.

- [ ] **Step 1.6: Verify V2 loot composition**

Inspect `src/main/java/dev/charles/multigolem/loot/HasGolemVariantLootCondition.java` and `src/main/java/dev/charles/multigolem/MultiGolem.java`.

Document that V2 variant loot reads `LootContextParams.THIS_ENTITY` at death time and then calls `GolemVariantAttachment.get(e)`. This proves V3 spawn-time attachment composes with V2 drops.

- [ ] **Step 1.7: Append V3 spike findings**

Append a section to `docs/26.1.2-mojang-targets.md`:

```markdown
## V3 Spike Findings (2026-05-17)

Output of V3 Task 1. Used by V3 villager-called spawn weighting.

### Villager help-call spawn target

- **Chosen target:** record exact FQN plus method name.
- **Named on Villager.class:** record yes or no, with source evidence.
- **Method signature:** record the full source signature.
- **Descriptor:** record the JVM descriptor used by Mixin.
- **Spawn reason:** record the exact `EntitySpawnReason` observed at the villager call site.
- **SpawnUtil.trySpawnMob return type:** record the exact generic or nullable return shape from source.

### Spawned-golem capture strategy

- **Chosen strategy:** record Candidate A, Candidate B, or Candidate C.
- **Why chosen:** explain why it is the narrowest reliable villager help-call hook.
- **Rejected alternatives:** explain why each non-selected candidate was rejected.
- **Mixin Extras required:** record yes or no. If yes, add the exact `build.gradle` dependency needed by later tasks.

### Scope safety

- Hook remains inside villager help-call path: yes/no.
- Mob spawners excluded: yes/no.
- Spawn eggs excluded: yes/no.
- Commands excluded: yes/no.
- Existing golems unchanged: yes/no.
- `playerCreated` remains false: yes/no.

### V2 loot composition

- V2 loot condition reads `LootContextParams.THIS_ENTITY` at death time.
- V3 spawn-time attachment therefore controls variant drops for village-spawned non-iron golems.
```

- [ ] **Step 1.8: Commit**

Run:

```powershell
git add docs/26.1.2-mojang-targets.md
git commit -m "docs: V3 source spike for village golem spawns"
```

Expected: commit succeeds. Do not proceed to Task 2 unless the spike selected Candidate A or B, or Candidate C has explicit human approval.

---

## Task 2: Add `VillageSpawnWeights` pure helper (TDD)

**Files:**
- Create: `src/test/java/dev/charles/multigolem/spawn/VillageSpawnWeightsTest.java`
- Create: `src/main/java/dev/charles/multigolem/spawn/VillageSpawnWeights.java`

- [ ] **Step 2.1: Write failing tests**

Create `VillageSpawnWeightsTest` with these tests:

```java
package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class VillageSpawnWeightsTest {

    @Test
    void defaults_matchV3Spec() {
        VillageSpawnWeights weights = VillageSpawnWeights.defaults();
        assertTrue(weights.enabled());
        assertEquals(19, weights.weight(GolemVariant.IRON));
        assertEquals(19, weights.weight(GolemVariant.COPPER));
        assertEquals(19, weights.weight(GolemVariant.GOLD));
        assertEquals(19, weights.weight(GolemVariant.EMERALD));
        assertEquals(5, weights.weight(GolemVariant.DIAMOND));
        assertEquals(2, weights.weight(GolemVariant.NETHERITE));
        assertEquals(83, weights.totalWeight());
    }

    @Test
    void deterministicRollCanReachEachVariant() {
        VillageSpawnWeights weights = VillageSpawnWeights.defaults();
        assertEquals(Optional.of(GolemVariant.IRON), weights.roll(bound -> 0));
        assertEquals(Optional.of(GolemVariant.COPPER), weights.roll(bound -> 19));
        assertEquals(Optional.of(GolemVariant.GOLD), weights.roll(bound -> 38));
        assertEquals(Optional.of(GolemVariant.EMERALD), weights.roll(bound -> 57));
        assertEquals(Optional.of(GolemVariant.DIAMOND), weights.roll(bound -> 76));
        assertEquals(Optional.of(GolemVariant.NETHERITE), weights.roll(bound -> 81));
    }

    @Test
    void eachSuccessfulCallRollsIndependently() {
        VillageSpawnWeights weights = VillageSpawnWeights.defaults();
        AtomicInteger calls = new AtomicInteger();
        assertEquals(Optional.of(GolemVariant.IRON), weights.roll(bound -> calls.getAndIncrement() == 0 ? 0 : 81));
        assertEquals(Optional.of(GolemVariant.NETHERITE), weights.roll(bound -> calls.getAndIncrement() == 0 ? 0 : 81));
        assertEquals(2, calls.get());
    }

    @Test
    void enabledFalseLeavesIronWithoutRolling() {
        VillageSpawnWeights weights = VillageSpawnWeights.defaults().withEnabled(false);
        assertEquals(Optional.empty(), weights.roll(bound -> fail("disabled weights must not call random")));
    }

    @Test
    void explicitAllZeroRecognizedWeightsDisableRolling() {
        EnumMap<GolemVariant, Integer> map = new EnumMap<>(GolemVariant.class);
        for (GolemVariant variant : GolemVariant.values()) {
            map.put(variant, 0);
        }
        VillageSpawnWeights weights = new VillageSpawnWeights(true, map);
        assertTrue(weights.isAllZero());
        assertEquals(Optional.empty(), weights.roll(bound -> fail("all-zero weights must not call random")));
    }

    @Test
    void negativeWeightsClampToZero() {
        EnumMap<GolemVariant, Integer> map = new EnumMap<>(GolemVariant.class);
        map.put(GolemVariant.IRON, -10);
        map.put(GolemVariant.COPPER, 1);
        map.put(GolemVariant.GOLD, 0);
        map.put(GolemVariant.EMERALD, 0);
        map.put(GolemVariant.DIAMOND, 0);
        map.put(GolemVariant.NETHERITE, 0);
        VillageSpawnWeights weights = new VillageSpawnWeights(true, map);
        assertEquals(0, weights.weight(GolemVariant.IRON));
        assertEquals(Optional.of(GolemVariant.COPPER), weights.roll(bound -> 0));
    }
}
```

- [ ] **Step 2.2: Run tests and verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.VillageSpawnWeightsTest
```

Expected: FAIL because `VillageSpawnWeights` does not exist.

- [ ] **Step 2.3: Implement the helper**

Create `VillageSpawnWeights`:

```java
package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntUnaryOperator;

public record VillageSpawnWeights(boolean enabled, EnumMap<GolemVariant, Integer> weights) {

    public VillageSpawnWeights {
        weights = sanitize(weights);
    }

    public static VillageSpawnWeights defaults() {
        EnumMap<GolemVariant, Integer> defaults = new EnumMap<>(GolemVariant.class);
        defaults.put(GolemVariant.IRON, 19);
        defaults.put(GolemVariant.COPPER, 19);
        defaults.put(GolemVariant.GOLD, 19);
        defaults.put(GolemVariant.EMERALD, 19);
        defaults.put(GolemVariant.DIAMOND, 5);
        defaults.put(GolemVariant.NETHERITE, 2);
        return new VillageSpawnWeights(true, defaults);
    }

    public VillageSpawnWeights withEnabled(boolean enabled) {
        return new VillageSpawnWeights(enabled, weights);
    }

    public int weight(GolemVariant variant) {
        return weights.getOrDefault(variant, 0);
    }

    public int totalWeight() {
        int total = 0;
        for (GolemVariant variant : GolemVariant.values()) total += weight(variant);
        return total;
    }

    public boolean isAllZero() {
        return totalWeight() == 0;
    }

    public Optional<GolemVariant> roll(IntUnaryOperator nextIntBounded) {
        if (!enabled || isAllZero()) return Optional.empty();
        int ticket = nextIntBounded.applyAsInt(totalWeight());
        int cursor = 0;
        for (GolemVariant variant : GolemVariant.values()) {
            cursor += weight(variant);
            if (ticket < cursor) return Optional.of(variant);
        }
        return Optional.empty();
    }

    private static EnumMap<GolemVariant, Integer> sanitize(Map<GolemVariant, Integer> input) {
        EnumMap<GolemVariant, Integer> result = new EnumMap<>(GolemVariant.class);
        for (GolemVariant variant : GolemVariant.values()) {
            int raw = input == null ? 0 : input.getOrDefault(variant, 0);
            result.put(variant, Math.max(0, raw));
        }
        return result;
    }
}
```

- [ ] **Step 2.4: Run tests and commit**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.VillageSpawnWeightsTest
git add src/test/java/dev/charles/multigolem/spawn/VillageSpawnWeightsTest.java src/main/java/dev/charles/multigolem/spawn/VillageSpawnWeights.java
git commit -m "feat: add village spawn weight helper"
```

Expected: tests pass and commit succeeds.

---

## Task 3: Extend config with `village_spawning` (TDD)

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/config/MultiGolemConfig.java`
- Test: `src/test/java/dev/charles/multigolem/config/MultiGolemConfigV3Test.java`

- [ ] **Step 3.1: Write failing config tests**

Create `MultiGolemConfigV3Test` covering:
- defaults include `village_spawning.enabled: true` and weights `19/19/19/19/5/2`.
- V2 config migrates to include `village_spawning`.
- existing V2 fields and unknown user fields are preserved.
- `enabled: false` parses correctly.
- missing `enabled` fills `true`.
- malformed `enabled` falls back to `true` and warns.
- explicit all-zero weights parse as intentional disablement.
- missing, non-object, or malformed weights fall back to defaults and warn.
- valid weights object with missing individual keys fills those keys from defaults and warns.
- weights object containing only unknown keys falls back to defaults and warns.
- unknown variant keys are preserved on disk but ignored by `VillageSpawnWeights`.
- negative weights clamp to `0`.
- non-numeric weights fall back to that variant's default.

Use assertion style from `MultiGolemConfigV2Test`. The runtime accessor should be:

```java
VillageSpawnWeights village = cfg.villageSpawnWeights();
assertTrue(village.enabled());
assertEquals(19, village.weight(GolemVariant.IRON));
```

- [ ] **Step 3.2: Run tests and verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.config.MultiGolemConfigV3Test
```

Expected: FAIL because `villageSpawnWeights()` and V3 parsing do not exist.

- [ ] **Step 3.3: Implement config shape**

Update `MultiGolemConfig`:
- add `private final VillageSpawnWeights villageSpawnWeights`.
- add `public VillageSpawnWeights villageSpawnWeights()`.
- update constructor, `equals`, and `hashCode`.
- update `defaults()` to include `VillageSpawnWeights.defaults()`.
- update `toJson()` to write top-level `village_spawning`.
- update `canonicalizeAndValidateInPlace(...)` with a top-level V3 canonicalizer before tier canonicalization.
- update `parse(...)` to read `village_spawning`.

Validation semantics must match the V3 spec exactly:
- malformed `enabled` -> default `true`.
- missing/non-object/malformed `weights` -> V3 defaults.
- known missing variant key in otherwise valid object -> that variant default.
- only unknown keys -> defaults.
- all six recognized keys present and resolved to `0` -> intentional disablement.
- unknown keys under `weights` preserved on disk and ignored at runtime.

- [ ] **Step 3.4: Run tests and commit**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.config.MultiGolemConfigV3Test
.\gradlew.bat --quiet test --tests dev.charles.multigolem.config.MultiGolemConfigV2Test
git add src/main/java/dev/charles/multigolem/config/MultiGolemConfig.java src/test/java/dev/charles/multigolem/config/MultiGolemConfigV3Test.java
git commit -m "feat: add village spawn config"
```

Expected: V3 tests pass and V2 config tests still pass.

---

## Task 4: Add `VillageGolemSpawnResolver` (TDD)

**Files:**
- Create: `src/test/java/dev/charles/multigolem/spawn/VillageGolemSpawnResolverTest.java`
- Create: `src/main/java/dev/charles/multigolem/spawn/VillageGolemSpawnResolver.java`
- Modify: `src/main/java/dev/charles/multigolem/stats/GolemStatsResolver.java` only if callers benefit from a V3 accessor

- [ ] **Step 4.1: Write failing resolver tests**

Create tests for:
- enabled false returns "leave Iron" and does not call random.
- all-zero weights return "leave Iron" and do not call random.
- an Iron roll returns "leave Iron".
- a non-iron roll returns that variant.
- resolver uses `MultiGolemConfig.villageSpawnWeights()` rather than hardcoded defaults.

Use a return type that makes the mixin code obvious:

```java
Optional<GolemVariant> result = new VillageGolemSpawnResolver(config).rollVariant(bound -> 19);
assertEquals(Optional.of(GolemVariant.COPPER), result);
```

`Optional.empty()` means leave the spawned golem as Iron.

- [ ] **Step 4.2: Run tests and verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.VillageGolemSpawnResolverTest
```

Expected: FAIL because resolver does not exist.

- [ ] **Step 4.3: Implement resolver**

Create:

```java
package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.MultiGolemConfig;

import java.util.Optional;
import java.util.function.IntUnaryOperator;

public final class VillageGolemSpawnResolver {

    private final MultiGolemConfig config;

    public VillageGolemSpawnResolver(MultiGolemConfig config) {
        this.config = config;
    }

    public Optional<GolemVariant> rollVariant(IntUnaryOperator nextIntBounded) {
        Optional<GolemVariant> rolled = config.villageSpawnWeights().roll(nextIntBounded);
        if (rolled.isEmpty() || rolled.get() == GolemVariant.IRON) return Optional.empty();
        return rolled;
    }
}
```

- [ ] **Step 4.4: Run tests and commit**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.VillageGolemSpawnResolverTest
git add src/test/java/dev/charles/multigolem/spawn/VillageGolemSpawnResolverTest.java src/main/java/dev/charles/multigolem/spawn/VillageGolemSpawnResolver.java src/main/java/dev/charles/multigolem/stats/GolemStatsResolver.java
git commit -m "feat: add village golem spawn resolver"
```

Expected: tests pass and commit succeeds.

---

## Task 5: Add runtime `VillageGolemSpawnHandler`

**Files:**
- Create: `src/main/java/dev/charles/multigolem/spawn/VillageGolemSpawnHandler.java`

- [ ] **Step 5.1: Implement handler**

Create a runtime helper that accepts a newly spawned `IronGolem` from the spike-confirmed villager path:

```java
package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import net.minecraft.world.entity.animal.golem.IronGolem;

public final class VillageGolemSpawnHandler {

    private VillageGolemSpawnHandler() {}

    public static void applyVillageRoll(IronGolem golem) {
        if (golem == null) return;
        try {
            VillageGolemSpawnResolver resolver = new VillageGolemSpawnResolver(MultiGolem.config());
            resolver.rollVariant(bound -> golem.getRandom().nextInt(bound)).ifPresent(variant -> applyVariant(golem, variant));
        } catch (Throwable t) {
            MultiGolem.LOG.error("Failed to apply village golem variant roll to golem {}", golem.getId(), t);
        }
    }

    private static void applyVariant(IronGolem golem, GolemVariant variant) {
        if (variant == GolemVariant.IRON) return;
        GolemVariantAttachment.set(golem, variant);
        golem.setHealth(golem.getMaxHealth());
    }
}
```

Do not call `setPlayerCreated(true)`.

- [ ] **Step 5.2: Build and commit**

Run:

```powershell
.\gradlew.bat --quiet build
git add src/main/java/dev/charles/multigolem/spawn/VillageGolemSpawnHandler.java
git commit -m "feat: apply village golem variant rolls"
```

Expected: build passes.

---

## Task 6: Add the spike-chosen villager mixin

The exact code in this task depends on Task 1. Keep the mixin thin: capture the newly spawned `IronGolem`, call `VillageGolemSpawnHandler.applyVillageRoll(golem)`, and do nothing else.

**Files:**
- Create: `src/main/java/dev/charles/multigolem/mixin/VillagerMixin.java` or the spike-confirmed equivalent
- Modify: `src/main/resources/multigolem.mixins.json`
- Modify: `build.gradle` only if Task 1 selected Mixin Extras

- [ ] **Step 6.1: Apply dependency decision from Task 1**

If Task 1 chose Candidate A or a plain Mixin `@Redirect`, do not touch `build.gradle`.

If Task 1 chose Candidate B with Mixin Extras `@WrapOperation`, add the exact dependency from the spike notes and document it in the commit message.

- [ ] **Step 6.2: Implement the mixin**

Use the Task 1 selected strategy:

- Candidate A: inject into the spike-confirmed villager method and capture the returned `SpawnUtil.trySpawnMob` value only if local capture is proven stable.
- Candidate B: wrap or redirect the spike-confirmed `SpawnUtil.trySpawnMob` call site, inspect its returned value, and pass only `IronGolem` results to the handler.
- Candidate C: do not implement unless Tyler explicitly approved it after Task 1.

Required invariants:
- only the villager help-call path is hooked.
- failed vanilla spawns do nothing.
- Iron rolls leave the golem unmodified.
- no broad entity-load, spawn-egg, command, or spawner hook is added.
- no `setPlayerCreated(true)` call is introduced.

- [ ] **Step 6.3: Register mixin**

Add the mixin class name to `src/main/resources/multigolem.mixins.json`.

- [ ] **Step 6.4: Build and commit**

Run:

```powershell
.\gradlew.bat --quiet build
git add build.gradle src/main/java/dev/charles/multigolem/mixin src/main/resources/multigolem.mixins.json
git commit -m "feat: hook villager-called golem spawns"
```

Expected: build passes. If build fails due to mixin target mismatch, return to Task 1 notes and fix the target rather than widening the hook.

---

## Task 7: Update playtest docs

**Files:**
- Modify: `docs/playtest-checklist.md`
- Modify: `docs/playtest.html`

- [ ] **Step 7.1: Add Markdown V3 rows**

Append `# V3 - village natural spawns` to `docs/playtest-checklist.md` with these rows:

```markdown
# V3 - village natural spawns

## Village variant rolls

- [ ] Set `village_spawning.weights` to `{"iron":0,"copper":1,"gold":0,"emerald":0,"diamond":0,"netherite":0}`. Trigger a villager-called golem spawn. Copper variant spawns.
- [ ] Repeat forced one-at-a-time weights for Gold, Emerald, Diamond, and Netherite. Each target variant can spawn from villagers.
- [ ] Set `netherite: 1` and all other recognized weights to `0`. Villagers can spawn Netherite.
- [ ] Restore default weights. Over repeated village spawns, Iron, Copper, Gold, Emerald, Diamond, and Netherite are all possible outcomes; Diamond and Netherite are rare.

## V2 behavior on village-spawned variants

- [ ] Village-spawned non-iron variant starts at configured full health.
- [ ] Textures render correctly on a modded client.
- [ ] Healing with the matching ingot works.
- [ ] Variant-specific drops use the village-spawned variant.
- [ ] Copper lightning heal works.
- [ ] Gold speed and particles work.
- [ ] Emerald villager aura works.
- [ ] Diamond lightning behavior works.
- [ ] Netherite fire immunity and ignite-on-hit work.
- [ ] `ignored_target_types` applies.
- [ ] `anger_on_hit` applies.

## Scope and config negatives

- [ ] Village-spawned variants are natural defenders, not player-created golems.
- [ ] Existing golems do not change after upgrading to V3.
- [ ] `village_spawning.enabled: false` leaves villager-called spawns as Iron.
- [ ] Fully explicit all-zero weights leave villager-called spawns as Iron.
- [ ] Mob spawner iron golems do not roll variants.
- [ ] Spawn egg iron golems do not roll variants.
- [ ] Command-spawned iron golems do not roll variants.
- [ ] Malformed `weights` falls back to defaults and logs a warning.
```

- [ ] **Step 7.2: Add matching HTML rows**

Add a new V3 section to `docs/playtest.html` with the same rows as checkbox labels. Keep the existing progress script unchanged.

- [ ] **Step 7.3: Commit docs**

Run:

```powershell
git add docs/playtest-checklist.md docs/playtest.html
git commit -m "docs: add V3 village spawn playtest rows"
```

Expected: commit succeeds.

---

## Task 8: Update CHANGELOG under `## Unreleased`

**Files:**
- Modify: `CHANGELOG.md`

- [ ] **Step 8.1: Add player/admin-facing note**

Under `## Unreleased`, add bullets in this voice:

```markdown
- Villages can now naturally spawn MultiGolem variants as defenders.
- Added server config for village spawn weights. Iron, Copper, Gold, and Emerald are common by default, while Diamond and Netherite are rare.
- Existing golems, player-built golems, mob spawners, spawn eggs, and command-spawned iron golems are unchanged.
```

Do not mention mixins, `SpawnUtil`, attachments, or source spikes in the changelog.

- [ ] **Step 8.2: Run changelog style gate and commit**

Run:

```powershell
python scripts\check-changelog-style.py --section Unreleased
git add CHANGELOG.md
git commit -m "docs: add V3 changelog entry"
```

Expected: style gate passes and commit succeeds.

---

## Task 9: Full verification

**Files:**
- No expected source edits. Fix failures in focused follow-up commits if verification finds issues.

- [ ] **Step 9.1: Run unit tests**

Run:

```powershell
.\gradlew.bat --quiet test
```

Expected: all unit tests pass.

- [ ] **Step 9.2: Run full build/check**

Run:

```powershell
.\gradlew.bat --quiet build
```

Expected: build passes, including `checkChangelog`, `checkReleaseNotesStyle`, and `checkUnitTestCompanions`.

- [ ] **Step 9.3: Server startup smoke**

Start a Fabric 26.1.2 server with the built jar and Fabric API. Confirm:
- server starts without mixin errors.
- `config/multigolem.json` contains `village_spawning`.
- no warnings appear for a default config.

- [ ] **Step 9.4: Focused in-game smoke**

Before full playtest, force one variant at a time:
- `copper: 1`, others `0`.
- `diamond: 1`, others `0`.
- `netherite: 1`, others `0`.

For each, trigger a villager-called golem spawn and verify:
- expected variant appears.
- current health equals configured max health.
- `playerCreated` remains false if checked through debug/NBT tooling.
- V2 texture and one representative V2 behavior work.

- [ ] **Step 9.5: Negative scope smoke**

Verify:
- `/summon minecraft:iron_golem` produces plain Iron.
- spawn egg produces plain Iron.
- mob spawner produces plain Iron.
- existing golems remain unchanged.

- [ ] **Step 9.6: Commit any final fixes**

If verification required changes, commit them with a focused message. If no changes were needed, no commit is required.

---

## Self-Review Checklist

- V3 is scoped only to villager-called golems.
- Task 1 blocks all code work and verifies named villager method status, spawn reason, `SpawnUtil.trySpawnMob` return type, capture strategy, Mixin Extras requirement, player-created status, and V2 loot composition.
- Pure Java helpers and config parsing have failing tests first.
- Config semantics distinguish all-zero intentional disablement from malformed/missing weights.
- Unknown config fields are preserved.
- V2 loot reads the variant attachment at death time.
- Both playtest docs are updated.
- `CHANGELOG.md` uses player/admin-facing language under `## Unreleased`.
- No V4 spawn eggs, V5 copper golem variants, player-built golem changes, command behavior changes, spawner behavior changes, or existing-golem conversion are included.
