# V2 Review Bug Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix four review findings (one P1 crash, two P2 correctness bugs, one P3 miss) identified in the MultiGolem V2 codebase.

**Architecture:** All changes are confined to existing production files — no new source files needed. Two test files get new test methods added. Changes are independent and can be applied in any order; a final build verifies everything.

**Tech Stack:** Java 25, Fabric mod (Minecraft 26.1.2), Gson for config, JUnit 5 for tests, Gradle wrapper (`.\gradlew.bat`).

---

## File Map

| Role | File |
|------|------|
| Modify (P1 fix) | `src/main/java/dev/charles/multigolem/config/MultiGolemConfig.java` |
| Modify (P2a fix) | `src/main/java/dev/charles/multigolem/ability/DiamondAbility.java` |
| Modify (P2a fix) | `src/main/java/dev/charles/multigolem/mixin/IronGolemAttackMixin.java` |
| Modify (P2b fix) | `src/main/java/dev/charles/multigolem/attachment/GolemAbilityState.java` |
| Modify (P2b fix) | `src/main/java/dev/charles/multigolem/ability/DiamondAbility.java` |
| Modify (P3 fix) | `src/main/java/dev/charles/multigolem/ability/TargetFilter.java` |
| Test (P1) | `src/test/java/dev/charles/multigolem/config/MultiGolemConfigV2Test.java` |
| Test (P3) | `src/test/java/dev/charles/multigolem/ability/TargetFilterTest.java` |

---

### Task 1: Fix P1 — non-numeric cooldown string crashes canonicalizeTierInPlace

**Root cause:** `canonicalizeInt` guards against non-number primitives and returns early without modifying the field. If the on-disk JSON has `"diamond_cooldown_min_seconds": "oops"`, `canonicalizeInt` leaves the string in place. The swap block on lines 202-211 then checks only `isJsonPrimitive()` (true for a string!) and calls `getAsInt()`, which throws `NumberFormatException` at server startup.

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/config/MultiGolemConfig.java:202-211`
- Test: `src/test/java/dev/charles/multigolem/config/MultiGolemConfigV2Test.java`

- [ ] **Step 1: Add the failing test**

Add this method to `MultiGolemConfigV2Test`:

```java
@Test
void v2_stringCooldownValues_doNotCrash(@TempDir Path tmp) throws IOException {
    Path file = tmp.resolve("multigolem.json");
    Files.writeString(file, """
        {
          "tiers": {
            "diamond": {
              "diamond_cooldown_min_seconds": "oops",
              "diamond_cooldown_max_seconds": "also_bad"
            }
          }
        }
        """);
    // Must not throw; falls back to defaults for non-numeric fields
    MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
    assertEquals(30, cfg.tier(GolemVariant.DIAMOND).diamondCooldownMinSeconds());
    assertEquals(60, cfg.tier(GolemVariant.DIAMOND).diamondCooldownMaxSeconds());
}
```

- [ ] **Step 2: Run the test to confirm it fails**

```powershell
.\gradlew.bat test --tests "dev.charles.multigolem.config.MultiGolemConfigV2Test.v2_stringCooldownValues_doNotCrash" --rerun > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'PASSED|FAILED|error:|NumberFormatException' | Select-Object -First 30
```

Expected: FAILED with `NumberFormatException` or similar.

- [ ] **Step 3: Fix the swap block in MultiGolemConfig.java**

In `canonicalizeTierInPlace`, find the swap block (around line 202) and add `isNumber()` guards:

Change:
```java
if (t.has("diamond_cooldown_min_seconds") && t.has("diamond_cooldown_max_seconds")
        && t.get("diamond_cooldown_min_seconds").isJsonPrimitive()
        && t.get("diamond_cooldown_max_seconds").isJsonPrimitive()) {
    int mn = t.get("diamond_cooldown_min_seconds").getAsInt();
    int mx = t.get("diamond_cooldown_max_seconds").getAsInt();
```

To:
```java
if (t.has("diamond_cooldown_min_seconds") && t.has("diamond_cooldown_max_seconds")
        && t.get("diamond_cooldown_min_seconds").isJsonPrimitive()
        && t.get("diamond_cooldown_min_seconds").getAsJsonPrimitive().isNumber()
        && t.get("diamond_cooldown_max_seconds").isJsonPrimitive()
        && t.get("diamond_cooldown_max_seconds").getAsJsonPrimitive().isNumber()) {
    int mn = t.get("diamond_cooldown_min_seconds").getAsInt();
    int mx = t.get("diamond_cooldown_max_seconds").getAsInt();
```

- [ ] **Step 4: Run the test to confirm it passes**

```powershell
.\gradlew.bat test --tests "dev.charles.multigolem.config.MultiGolemConfigV2Test.v2_stringCooldownValues_doNotCrash" --rerun > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'PASSED|FAILED|error:' | Select-Object -First 20
```

Expected: PASSED.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/charles/multigolem/config/MultiGolemConfig.java \
        src/test/java/dev/charles/multigolem/config/MultiGolemConfigV2Test.java
git commit -m "fix: guard cooldown swap block against non-numeric string primitives (P1)"
```

---

### Task 2: Fix P2a — Math.abs(Long.MIN_VALUE) in cooldown RNG

**Root cause:** Both `DiamondAbility.java` and `IronGolemAttackMixin.java` compute a random cooldown offset via `Math.abs(random.nextLong()) % span`. `Math.abs(Long.MIN_VALUE) == Long.MIN_VALUE` (still negative due to two's complement overflow), so the modulo result can rarely be negative, placing `nextAt` earlier than intended and effectively making the golem fire again immediately.

**Fix:** Replace with `Math.floorMod(random.nextLong(), span)`, which guarantees a non-negative result when `span > 0`.

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/ability/DiamondAbility.java:94`
- Modify: `src/main/java/dev/charles/multigolem/mixin/IronGolemAttackMixin.java:62`

- [ ] **Step 1: Fix DiamondAbility.java**

Line ~94 in `tickDiamond`:

Change:
```java
long nextAt = now + min + (Math.abs(world.getRandom().nextLong()) % span);
```
To:
```java
long nextAt = now + min + Math.floorMod(world.getRandom().nextLong(), span);
```

- [ ] **Step 2: Fix IronGolemAttackMixin.java**

Line ~62 in `dispatchVariantEffect`:

Change:
```java
long nextAt = level.getGameTime() + min + (Math.abs(level.getRandom().nextLong()) % span);
```
To:
```java
long nextAt = level.getGameTime() + min + Math.floorMod(level.getRandom().nextLong(), span);
```

- [ ] **Step 3: Build to verify no compilation errors**

```powershell
.\gradlew.bat --quiet build > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'error:|FAILED|cannot find symbol' | Select-Object -First 20
```

Expected: no error output.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/dev/charles/multigolem/ability/DiamondAbility.java \
        src/main/java/dev/charles/multigolem/mixin/IronGolemAttackMixin.java
git commit -m "fix: use Math.floorMod to avoid Math.abs(Long.MIN_VALUE) RNG edge case (P2)"
```

---

### Task 3: Fix P2b — Diamond passive aura failed-scan backoff

**Root cause:** `DiamondAbility.tickDiamond` runs every server tick for all diamond golems. When no valid targets exist (wrong biome, all LOS-blocked, all excluded), it returns without spending the cooldown, so the same expensive entity scan + LOS raycasts repeat on the next tick and every tick thereafter until a valid target appears.

**Fix:** Add a `nextDiamondScanGameTime` field to `GolemAbilityState`. When a passive scan finds no candidates, set a 2-second (40-tick) backoff. The on-attack path in `IronGolemAttackMixin` is unaffected because it doesn't go through the passive scan gate.

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/attachment/GolemAbilityState.java`
- Modify: `src/main/java/dev/charles/multigolem/ability/DiamondAbility.java`

- [ ] **Step 1: Add scan backoff field to GolemAbilityState**

Replace the entire record:

```java
package dev.charles.multigolem.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record GolemAbilityState(long nextDiamondAbilityGameTime, long nextDiamondScanGameTime) {

    public static final Codec<GolemAbilityState> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.LONG.optionalFieldOf("next_diamond_ability_game_time", 0L)
            .forGetter(GolemAbilityState::nextDiamondAbilityGameTime),
        Codec.LONG.optionalFieldOf("next_diamond_scan_game_time", 0L)
            .forGetter(GolemAbilityState::nextDiamondScanGameTime)
    ).apply(i, GolemAbilityState::new));

    public static GolemAbilityState fresh() {
        return new GolemAbilityState(0L, 0L);
    }

    public GolemAbilityState withDiamondCooldown(long nextGameTime) {
        return new GolemAbilityState(nextGameTime, nextDiamondScanGameTime);
    }

    public GolemAbilityState withDiamondScanBackoff(long nextScan) {
        return new GolemAbilityState(nextDiamondAbilityGameTime, nextScan);
    }

    public boolean diamondCooldownReady(long currentGameTime) {
        return currentGameTime >= nextDiamondAbilityGameTime;
    }

    public boolean diamondScanReady(long currentGameTime) {
        return currentGameTime >= nextDiamondScanGameTime;
    }
}
```

- [ ] **Step 2: Gate the passive scan on diamondScanReady and set backoff on miss**

In `DiamondAbility.tickDiamond`, after the early-return `if (!ability.diamondCooldownReady(now)) return;` block and before the candidate search, add the scan-ready gate. Then set a backoff when no candidates are found.

The updated `tickDiamond` method (only the relevant section changes; full method shown for clarity):

```java
private static void tickDiamond(ServerLevel world, IronGolem golem, dev.charles.multigolem.config.TierStats stats) {
    GolemAbilityState ability = GolemAbilityStateAttachment.get(golem);
    long now = world.getGameTime();

    // Cooldown-ready visual: emit END_ROD every second while primed
    if (ability.diamondCooldownReady(now) && now % 20 == 0) {
        world.sendParticles(ParticleTypes.END_ROD,
            golem.getX(), golem.getEyeY() + 0.4, golem.getZ(),
            1, 0.05, 0.05, 0.05, 0.0);
    }

    if (!ability.diamondCooldownReady(now)) return;
    if (!ability.diamondScanReady(now)) return;

    int range = stats.diamondAuraRange();
    var modePredicate = TargetFilter.DiamondTargetPredicate.of(stats.diamondTargetMode());
    var excludeFilter = TargetFilter.fromIgnoredList(stats.ignoredTargetTypes());

    AABB box = golem.getBoundingBox().inflate(range);
    List<Entity> candidates = world.getEntities(golem, box, e ->
        e.isAlive() && !e.isRemoved()
            && modePredicate.matches(e)
            && !excludeFilter.isExcluded(e)
            && hasLineOfSight(world, golem, e));
    if (candidates.isEmpty()) {
        GolemAbilityStateAttachment.set(golem, ability.withDiamondScanBackoff(now + 40L));
        return;
    }

    Entity target = candidates.stream()
        .min(Comparator.comparingDouble(e -> e.distanceToSqr(golem)))
        .orElseThrow();

    var bolt = EntityType.LIGHTNING_BOLT.create(world, EntitySpawnReason.TRIGGERED);
    if (bolt != null) {
        bolt.setPos(target.getX(), target.getY(), target.getZ());
        world.addFreshEntity(bolt);
    }

    long min = Math.max(0, stats.diamondCooldownMinSeconds()) * 20L;
    long max = Math.max(min, stats.diamondCooldownMaxSeconds()) * 20L;
    long span = Math.max(1, max - min + 1);
    long nextAt = now + min + Math.floorMod(world.getRandom().nextLong(), span);
    GolemAbilityStateAttachment.set(golem, ability.withDiamondCooldown(nextAt));
}
```

- [ ] **Step 3: Build to verify no compilation errors**

```powershell
.\gradlew.bat --quiet build > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'error:|FAILED|cannot find symbol' | Select-Object -First 20
```

Expected: no error output.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/dev/charles/multigolem/attachment/GolemAbilityState.java \
        src/main/java/dev/charles/multigolem/ability/DiamondAbility.java
git commit -m "fix: add 2-second scan backoff on failed passive diamond aura scan (P2)"
```

---

### Task 4: Fix P3 — EnderDragonPart not treated as boss

**Root cause:** `TargetFilter.isBossClass` recognizes `EnderDragon`, `WitherBoss`, and `Warden`, but not `EnderDragonPart`. The Ender Dragon spawns several `EnderDragonPart` entities as its hitboxes; `BOSSES_ONLY` mode won't target them and `ALL_BOSSES` exclusion won't exclude them.

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/ability/TargetFilter.java`
- Test: `src/test/java/dev/charles/multigolem/ability/TargetFilterTest.java`

- [ ] **Step 1: Add the failing test**

Add this method to `TargetFilterTest`. `EnderDragonPart` is in `net.minecraft.world.entity.boss.enderdragon`:

```java
@Test
void ignored_allBosses_excludesEnderDragonPart() {
    TargetFilter f = TargetFilter.fromIgnoredList(List.of("ALL_BOSSES"));
    assertTrue(f.isExcludedClass(net.minecraft.world.entity.boss.enderdragon.EnderDragonPart.class));
}

@Test
void diamondMode_bossesOnly_matchesEnderDragonPart() {
    DiamondTargetPredicate p = DiamondTargetPredicate.of("BOSSES_ONLY");
    assertTrue(p.matchesClass(net.minecraft.world.entity.boss.enderdragon.EnderDragonPart.class));
}
```

- [ ] **Step 2: Run the tests to confirm they fail**

```powershell
.\gradlew.bat test --tests "dev.charles.multigolem.ability.TargetFilterTest.ignored_allBosses_excludesEnderDragonPart" --tests "dev.charles.multigolem.ability.TargetFilterTest.diamondMode_bossesOnly_matchesEnderDragonPart" --rerun > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'PASSED|FAILED|error:' | Select-Object -First 20
```

Expected: both FAILED.

- [ ] **Step 3: Add EnderDragonPart to isBossClass in TargetFilter.java**

Add the import (alongside the existing `EnderDragon` import):
```java
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
```

Change `isBossClass`:
```java
private static boolean isBossClass(Class<?> type) {
    return WitherBoss.class.isAssignableFrom(type)
        || EnderDragon.class.isAssignableFrom(type)
        || EnderDragonPart.class.isAssignableFrom(type)
        || Warden.class.isAssignableFrom(type);
}
```

- [ ] **Step 4: Run the tests to confirm they pass**

```powershell
.\gradlew.bat test --tests "dev.charles.multigolem.ability.TargetFilterTest.ignored_allBosses_excludesEnderDragonPart" --tests "dev.charles.multigolem.ability.TargetFilterTest.diamondMode_bossesOnly_matchesEnderDragonPart" --rerun > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'PASSED|FAILED|error:' | Select-Object -First 20
```

Expected: both PASSED.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/charles/multigolem/ability/TargetFilter.java \
        src/test/java/dev/charles/multigolem/ability/TargetFilterTest.java
git commit -m "fix: treat EnderDragonPart as a boss in TargetFilter (P3)"
```

---

### Task 5: Full build verification

- [ ] **Step 1: Run full build**

```powershell
.\gradlew.bat --quiet build > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'error:|FAILED|cannot find symbol|incompatible types|mixin apply failed' | Select-Object -First 80
```

Expected: no output (clean build).

- [ ] **Step 2: Confirm test count**

```powershell
Select-String -Path build_out.txt -Pattern 'tests were run|BUILD SUCCESSFUL' | Select-Object -First 5
```
