# Identity Catalog Playtest Bugfixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the three Phase 1+2 playtest regressions without expanding into Phase 3 or changing gameplay balance.

**Architecture:** Keep the Phase 1 catalog and Phase 2 identity model as compatibility layers over vanilla `IronGolem`. Add small identity/catalog-aware helpers only where they directly cover the failed paths: heal interaction prediction, Diamond special-effect target eligibility, and spawner preview identity propagation.

**Tech Stack:** Java 25, Fabric API attachments, Minecraft 26.1.2 mappings, Mixin, JUnit 6, Gradle/Loom.

---

## Investigation Summary

- Worktree root verified before edits: `C:/Users/tyler/AI Projects/MultiGolem/.worktrees/identity-catalog-phase-1-2`.
- Branch: `codex/identity-catalog-phase-1-2`.
- Parent checkout status checked separately: clean `main...origin/main`.
- Bug 1 likely root cause: `IronGolemMixin` returns immediately on the client and returns `PASS` for a mismatched known heal item, so vanilla `IronGolem` interaction prediction can show misleading repair feedback for custom variants even when the server does not apply or persist a real custom heal.
- Bug 2 root cause: Diamond special-effect predicates use `TargetFilter.DiamondTargetPredicate`, which classifies vanilla hostile mobs and players but not a Zombie Golem entity. Targeting works through `ZombieGolemFaction`, but lightning eligibility still sees the target as a vanilla `IronGolem`, not a hostile Zombie Golem.
- Bug 3 root cause: `BaseSpawnerMixin` applies identity only during server spawn output. The cage preview display entity is built from spawner display/spawn data, so it never receives the custom identity before client render-state texture selection runs.
- Revue plan review `rev_1c642a13b25145b68fc1a1a8ae5b8e86` findings actioned before coding:
  - Accepted `finding-1`: make vanilla/default Iron pass-through the first heal decision rule.
  - Accepted `finding-2`: pin the spawner preview hook to `BaseSpawner.getOrCreateDisplayEntity(Level, BlockPos)` and guard it to the client display path.
  - Rejected `finding-3`: `TargetFilter.DiamondTargetPredicate.of(String)` already exists in `TargetFilter.java`.
  - Rejected `finding-4`: `IronGolemAttackMixinTest.java` already exists as a tracked test; it was not listed in dirty status because this bug-fix plan had not modified it yet.

## Files

- Modify: `src/main/java/dev/charles/multigolem/mixin/IronGolemMixin.java`
  - Centralize heal interaction decision so client prediction and server persistence agree for custom variants.
- Modify: `src/main/java/dev/charles/multigolem/ability/TargetFilter.java`
  - Add a testable Diamond target helper that can classify a target entity together with an optional golem identity.
- Modify: `src/main/java/dev/charles/multigolem/mixin/IronGolemAttackMixin.java`
  - Use the identity-aware Diamond helper before spawning lightning on melee attack.
- Modify: `src/main/java/dev/charles/multigolem/ability/DiamondAbility.java`
  - Use the same identity-aware Diamond helper for aura lightning scans.
- Modify: `src/main/java/dev/charles/multigolem/spawn/SpawnerVariantMarker.java`
  - Add a small helper that applies a marker identity to an already-created display entity when it is an `IronGolem`.
- Modify: `src/main/java/dev/charles/multigolem/mixin/BaseSpawnerMixin.java`
  - Apply marker identity to the spawner preview/display entity on the client/display path, while preserving the existing server spawn path.
- Test: `src/test/java/dev/charles/multigolem/mixin/IronGolemMixinTest.java`
- Test: `src/test/java/dev/charles/multigolem/ability/TargetFilterTest.java`
- Test: `src/test/java/dev/charles/multigolem/spawn/SpawnerVariantMarkerTest.java`
- Optional doc/changelog touch only if the fix changes beta-test notes: `CHANGELOG.md`

---

### Task 1: Heal Interaction Prediction And Persistence

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/mixin/IronGolemMixin.java`
- Test: `src/test/java/dev/charles/multigolem/mixin/IronGolemMixinTest.java`

- [ ] **Step 1.1: Write failing heal decision tests**

Add pure tests that describe the decision table without constructing live players:

```java
@Test
void customGolemRejectsKnownWrongHealItemInsteadOfPassingToVanilla() {
    assertEquals(InteractionResult.FAIL,
        IronGolemMixin.healInteractionResultForTest(
            true, GolemVariant.DIAMOND, GolemVariant.IRON, false, true));
}

@Test
void customGolemAcceptsMatchingHealItemForClientPrediction() {
    assertEquals(InteractionResult.SUCCESS,
        IronGolemMixin.healInteractionResultForTest(
            true, GolemVariant.DIAMOND, GolemVariant.DIAMOND, false, true));
}

@Test
void fullHealthCustomGolemConsumesNoItemAndDoesNotPretendHeal() {
    assertEquals(InteractionResult.PASS,
        IronGolemMixin.healInteractionResultForTest(
            true, GolemVariant.DIAMOND, GolemVariant.DIAMOND, true, true));
}
```

- [ ] **Step 1.2: Run heal tests to verify RED**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.mixin.IronGolemMixinTest
```

Expected: compile failure or assertion failure because the test seam/decision behavior does not exist yet.

- [ ] **Step 1.3: Implement minimal heal decision helper**

Extract only the interaction-result choice from `multigolem$healWithMatchingIngot(...)`.

Rules:
- Target variant is `IRON` or default/no custom identity: `PASS` immediately, preserving vanilla Iron Golem repair behavior for every held item.
- Unknown held item: `PASS`.
- Healing disabled and held item is any known heal material: `FAIL`.
- Known held item does not match a custom variant: `FAIL`, so vanilla cannot client-predict or server-handle iron repair for a custom golem.
- Matching custom variant with missing health and allowed permission: `SUCCESS`.
- Matching custom variant at full health: `PASS`.

Keep server-only state mutation in the injection method: permission denial message, `self.heal(...)`, sound, item consume, and `cir.setReturnValue(...)`.

- [ ] **Step 1.4: Run heal tests to verify GREEN**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.mixin.IronGolemMixinTest
```

Expected: heal decision tests pass.

---

### Task 2: Diamond Lightning Against Zombie Golems

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/ability/TargetFilter.java`
- Modify: `src/main/java/dev/charles/multigolem/mixin/IronGolemAttackMixin.java`
- Modify: `src/main/java/dev/charles/multigolem/ability/DiamondAbility.java`
- Test: `src/test/java/dev/charles/multigolem/ability/TargetFilterTest.java`

- [ ] **Step 2.1: Write failing identity-aware target tests**

Add tests:

```java
@Test
void diamondMode_allHostileMobs_matchesZombieGolemIdentity() {
    DiamondTargetPredicate p = DiamondTargetPredicate.of("ALL_HOSTILE_MOBS");
    assertTrue(p.matchesGolemVariant(GolemVariant.ZOMBIE));
    assertFalse(p.matchesGolemVariant(GolemVariant.DIAMOND));
}

@Test
void diamondMode_noneStillRejectsZombieGolemIdentity() {
    DiamondTargetPredicate p = DiamondTargetPredicate.of("NONE");
    assertFalse(p.matchesGolemVariant(GolemVariant.ZOMBIE));
}
```

- [ ] **Step 2.2: Run target tests to verify RED**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.ability.TargetFilterTest
```

Expected: compile failure because `matchesGolemVariant` does not exist.

- [ ] **Step 2.3: Implement identity-aware Diamond predicate**

Extend `TargetFilter.DiamondTargetPredicate` with:

```java
boolean matchesGolemVariant(GolemVariant variant);
```

For `ALL_HOSTILE_MOBS` and `ALL_HOSTILE_MOBS_AND_PLAYERS`, return `true` only for `GolemVariant.ZOMBIE`. For `BOSSES_ONLY` and `NONE`, return `false`.

In `IronGolemAttackMixin` and `DiamondAbility`, when `target instanceof IronGolem targetGolem`, read `GolemVariantAttachment.get(targetGolem)` and use `matchesGolemVariant(...)`; otherwise keep the existing `matches(entity)` behavior.

- [ ] **Step 2.4: Run target and attack tests to verify GREEN**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.ability.TargetFilterTest --tests dev.charles.multigolem.mixin.IronGolemAttackMixinTest
```

Expected: tests pass.

---

### Task 3: Spawner Preview Identity Propagation

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/spawn/SpawnerVariantMarker.java`
- Modify: `src/main/java/dev/charles/multigolem/mixin/BaseSpawnerMixin.java`
- Test: `src/test/java/dev/charles/multigolem/spawn/SpawnerVariantMarkerTest.java`

- [ ] **Step 3.1: Write failing marker application tests**

Add a pure seam test around a marker-read helper first:

```java
@Test
void markerIdentityCanBeReadForPreviewApplication() {
    CompoundTag entity = new CompoundTag();
    entity.putString("id", "minecraft:iron_golem");
    SpawnerVariantMarker.write(entity, GolemVariant.DIAMOND);

    assertEquals(GolemIdentity.ofIronVariant(GolemVariant.DIAMOND),
        SpawnerVariantMarker.previewIdentity(entity).orElseThrow());
}
```

If a mockable `IronGolem` helper seam is practical, also test that applying an empty marker returns `false` and a non-empty marker returns `true` only for Iron Golems.

- [ ] **Step 3.2: Run spawner marker tests to verify RED**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.SpawnerVariantMarkerTest
```

Expected: compile failure because `previewIdentity` does not exist.

- [ ] **Step 3.3: Implement preview marker helper and client/display mixin hook**

Add `SpawnerVariantMarker.previewIdentity(CompoundTag entityTag)` as a named wrapper over `readIdentity(...)` for the preview path.

In `BaseSpawnerMixin`, add a narrow redirect around the 26.1.2 display entity cache-miss path:

```java
@Redirect(
    method = "getOrCreateDisplayEntity(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/entity/Entity;",
    at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/entity/EntityType;loadEntityRecursive(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/EntityProcessor;)Lnet/minecraft/world/entity/Entity;"
    )
)
```

Guard the redirect with `level.isClientSide()` before applying any preview-only identity. When the display entity is created from a marked Iron Golem spawn tag:

```java
if (displayEntity instanceof IronGolem golem) {
    SpawnerVariantMarker.previewIdentity(entityTag)
        .ifPresent(identity -> GolemIdentityAttachment.set(golem, identity));
}
```

Keep the existing server `serverTick` ThreadLocal spawn hook intact because manual playtesting confirmed actual spawner output is correct.

- [ ] **Step 3.4: Run spawner tests to verify GREEN**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.SpawnerVariantMarkerTest
```

Expected: tests pass.

---

### Task 4: Targeted Regression Sweep

**Files:**
- Existing modified production and test files only.

- [ ] **Step 4.1: Run focused identity/catalog regression tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.mixin.IronGolemMixinTest --tests dev.charles.multigolem.ability.TargetFilterTest --tests dev.charles.multigolem.mixin.IronGolemAttackMixinTest --tests dev.charles.multigolem.spawn.SpawnerVariantMarkerTest --tests dev.charles.multigolem.spawn.SpawnEggStacksTest --tests dev.charles.multigolem.attachment.GolemIdentityAttachmentTest
```

Expected: all selected tests pass.

- [ ] **Step 4.2: Inspect final diff**

Run:

```powershell
git diff -- src/main/java src/test/java docs/superpowers/plans/2026-05-29-identity-catalog-playtest-bugfixes.md
```

Expected: diff is scoped to the three playtest bug fixes and the plan.

---

## Final Verification

After implementation review findings are handled:

```powershell
git rev-parse --show-toplevel
.\gradlew.bat --quiet test
.\gradlew.bat build
```

Expected root: `C:/Users/tyler/AI Projects/MultiGolem/.worktrees/identity-catalog-phase-1-2`.

Expected test/build result: both Gradle commands exit 0.

## Manual Playtest Follow-Up

- Confirmed: Iron bars no longer heal or crash custom golems.
- Confirmed: matching custom materials still heal the appropriate custom golems.
- Confirmed: iron ingots still heal vanilla Iron Golems.
- Confirmed: Diamond Golem lightning hits Zombie Golems and recharges.
- Confirmed: spawner cage previews show the configured variant and actual spawn output remains correct.
- Confirmed: villagers spawn alternate golem variants.
- Known skipped manual check: permissions behavior was not manually playtested in this pass; automated permission tests remain in the suite.
