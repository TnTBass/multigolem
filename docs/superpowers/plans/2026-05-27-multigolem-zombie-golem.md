# Zombie Golem Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a hostile Zombie Golem variant that is built from Mossy Cobblestone, heals from Rotten Flesh, converts villagers and wandering traders, fights village defenders, and can be maintained in zombie-villager village areas without starting Redstone, Lapis, Skeleton, or other unrelated golem work.

**Architecture:** Keep Zombie Golems inside the existing vanilla `IronGolem` plus `GolemVariantAttachment` architecture. Add the `ZOMBIE` variant, config fields, permission nodes, targeting/faction helpers, attack-side sickness/conversion helpers, zombie-village desired-count maintenance, and V4 marked spawn egg/spawner reuse in small TDD slices with source-spike gates before risky hooks.

**Tech Stack:** Java 25, Fabric 26.1.2, Fabric Loom, Fabric Permissions API, Sponge Mixin, JUnit 6, Mockito, Python planning/release gates, generated PNG assets from `scripts/generate-textures.py`.

---

## Scope Boundaries

- Zombie Golem only.
- No Redstone Golem, Lapis Golem, Skeleton Golem, undead-family framework, new entity type, custom block, taming, pacifying, sunlight burning, trader llama conversion, or release publishing.
- Stop for design review if source evidence says a custom entity type is required, generated zombie villages can exist without live zombie villagers, village-area maintenance needs broad invasive hooks, or vanilla Iron Golem counterattack support cannot stay cleanly inside the variant-only architecture.
- `anger_on_hit` remains in `tiers.zombie` for schema consistency but is ignored for `ZOMBIE`; Zombie Golems are unconditionally hostile.

## File Structure

Source-spike notes:
- Modify: `docs/26.1.2-mojang-targets.md` - append Zombie Golem spike evidence for current 26.1.2 source targets.

Variant identity, config, permissions:
- Modify: `src/main/java/dev/charles/multigolem/GolemVariant.java` - add `ZOMBIE` with Mossy Cobblestone, Rotten Flesh healing, and Rotten Flesh drops.
- Modify: `src/main/java/dev/charles/multigolem/config/TierStats.java` - add Zombie-specific optional fields.
- Modify: `src/main/java/dev/charles/multigolem/config/MultiGolemConfig.java` - add `tiers.zombie`, `zombie_village_spawning`, parsing, validation, and JSON writeback.
- Create: `src/main/java/dev/charles/multigolem/spawn/ZombieVillageSpawningConfig.java` - immutable top-level config for Zombie Golem desired-count maintenance.
- Modify: `src/main/java/dev/charles/multigolem/permissions/MultiGolemPermissions.java` - no special-case code expected, but tests must prove `multigolem.create.zombie` and `multigolem.heal.zombie` are permissive by default.

Creation and healing:
- Modify: `src/main/java/dev/charles/multigolem/spawn/GolemCreationHandler.java` - include Mossy Cobblestone T-pattern through `GolemVariant.ZOMBIE`.
- Modify: `src/main/java/dev/charles/multigolem/mixin/IronGolemMixin.java` - support Rotten Flesh healing amount from config and block vanilla iron-ingot fallback for Zombie Golems.

Combat, targeting, conversion:
- Create: `src/main/java/dev/charles/multigolem/ability/ZombieGolemFaction.java` - central faction predicate for targets and allies.
- Create: `src/main/java/dev/charles/multigolem/ability/ZombieGolemEffects.java` - player sickness effect application.
- Create: `src/main/java/dev/charles/multigolem/ability/ZombieGolemConversion.java` - villager and wandering-trader conversion to zombie villagers with data preservation.
- Modify: `src/main/java/dev/charles/multigolem/mixin/GolemTargetingMixin.java` - enforce Zombie Golem ignore rules and non-zombie golem defender targeting vetoes.
- Create: `src/main/java/dev/charles/multigolem/mixin/IronGolemRegisterGoalsMixin.java` if the targeting spike proves a goal injection is required for proactive Zombie Golem targeting.
- Modify: `src/main/java/dev/charles/multigolem/mixin/IronGolemAttackMixin.java` - dispatch Zombie player sickness and civilian conversion; failed conversion rolls deal no normal damage.
- Modify: `src/main/java/dev/charles/multigolem/mixin/LivingEntityMixin.java` - ensure `anger_on_hit` suppression never pacifies Zombie Golems.
- Modify: `src/main/resources/multigolem.mixins.json` - register any new mixins from the spike.

Zombie-village maintenance:
- Create: `src/main/java/dev/charles/multigolem/spawn/ZombieVillageSpawnResolver.java` - pure desired-count helper.
- Create: `src/main/java/dev/charles/multigolem/spawn/ZombieVillageSpawnHandler.java` - runtime scan, current live Zombie Golem count check before every spawn attempt, and safe spawn application.
- Modify: `src/main/java/dev/charles/multigolem/mixin/VillagerMixin.java` or create a narrower mixin selected by the source spike - call maintenance from the proven village-area hook.

Spawn eggs, spawners, assets:
- Modify: `src/main/java/dev/charles/multigolem/spawn/SpawnEggStacks.java` - allow `ZOMBIE` marked `minecraft:iron_golem_spawn_egg`.
- Modify: `src/main/java/dev/charles/multigolem/spawn/SpawnerVariantMarker.java` - prove existing marker logic accepts `ZOMBIE`; change only if tests fail.
- Modify: `src/main/resources/assets/multigolem/lang/en_us.json` - add Zombie Golem egg display text if needed by existing item model flow.
- Create: `src/main/resources/assets/multigolem/models/item/zombie_golem_spawn_egg.json` if V4 assets require per-variant model files.
- Create: `src/main/resources/assets/multigolem/textures/entity/zombie_golem.png` and `src/main/resources/assets/multigolem/textures/item/zombie_golem_spawn_egg.png` through the generated asset pipeline if generated assets are needed.

Tests:
- Modify: `src/test/java/dev/charles/multigolem/GolemVariantTest.java`
- Modify: `src/test/java/dev/charles/multigolem/config/MultiGolemConfigTest.java`
- Modify: `src/test/java/dev/charles/multigolem/config/MultiGolemConfigV2Test.java`
- Create: `src/test/java/dev/charles/multigolem/config/ZombieVillageSpawningConfigTest.java`
- Modify: `src/test/java/dev/charles/multigolem/permissions/MultiGolemPermissionsTest.java`
- Create: `src/test/java/dev/charles/multigolem/ability/ZombieGolemFactionTest.java`
- Create: `src/test/java/dev/charles/multigolem/ability/ZombieGolemEffectsTest.java`
- Create: `src/test/java/dev/charles/multigolem/ability/ZombieGolemConversionTest.java`
- Modify: `src/test/java/dev/charles/multigolem/mixin/IronGolemAttackMixinTest.java`
- Create: `src/test/java/dev/charles/multigolem/mixin/LivingEntityMixinTest.java`
- Create: `src/test/java/dev/charles/multigolem/spawn/ZombieVillageSpawnResolverTest.java`
- Create: `src/test/java/dev/charles/multigolem/spawn/ZombieVillageSpawnHandlerTest.java`
- Modify: `src/test/java/dev/charles/multigolem/spawn/SpawnEggStacksTest.java`
- Modify: `src/test/java/dev/charles/multigolem/spawn/SpawnerVariantMarkerTest.java`

Docs and release-facing notes:
- Modify: `README.md`
- Modify: `docs/playtest-checklist.md`
- Modify: `docs/modrinth-listing.md`
- Modify: `docs/curseforge-listing.md`
- Modify: `CHANGELOG.md`
- Modify: `INTERNAL_CHANGELOG.md` if the active release flow already uses it for planning notes.
- Do not tag, publish, or release.

---

### Task 1: Source Spike - Creation, Healing, Attack, Targeting, Counterattack, Conversion, Village Maintenance, Templates, Spawn Egg, Spawner

**Files:**
- Modify: `docs/26.1.2-mojang-targets.md`

- [ ] **Step 1.1: Verify worktree before spike**

Run:

```powershell
git rev-parse --show-toplevel
git status --short --branch
```

Expected: root is `C:/Users/tyler/AI Projects/MultiGolem`; status shows the assigned worktree only.

- [ ] **Step 1.2: Inspect current Minecraft and mod source targets**

Run:

```powershell
rg -n "trySpawnGolem|mobInteract|doHurtTarget|setTarget|registerGoals|spawnGolemIfNeeded|ZombieVillager|WanderingTrader|SpawnEggItem|BaseSpawner|SpawnData|GolemSpawnOrigin|SpawnOrigin" .gradle src docs
```

Expected: output identifies current source locations for player-built creation, Rotten Flesh healing interception, player hit sickness, villager/wandering-trader conversion, targeting/faction hooks, vanilla Iron Golem counterattack invasiveness, village-area maintenance, generated zombie-village zombie-villager template evidence, V4 marked spawn egg, spawner marker reuse, and whether `GolemSpawnOrigin`/`SpawnOrigin` still exists.

- [ ] **Step 1.3: Append spike evidence**

Add a `## Zombie Golem 26.1.2 Spike` section to `docs/26.1.2-mojang-targets.md` with these exact headings:

```markdown
## Zombie Golem 26.1.2 Spike

### Mossy Cobblestone T-pattern creation

### Rotten Flesh healing

### Player sickness attack hook

### Villager and wandering-trader conversion data preservation

### Targeting and faction hooks

### Vanilla Iron Golem counterattack invasiveness

### Village-area maintenance

### Generated zombie-village zombie-villager template evidence

### V4 marked spawn egg and spawner reuse

### Spawn origin marker

### Stop conditions
```

Each heading must include the inspected class, method signature, chosen hook, rejected hooks, and whether execution may proceed. The village template section must explicitly say whether generated zombie-village templates produce live zombie villagers in-world. The spawn origin marker section must explicitly say whether `GolemSpawnOrigin.VILLAGE` exists and is still the correct origin marker for maintenance-spawned Zombie Golems. If generated zombie-village live zombie-villager evidence cannot be proven, stop and request design review.

- [ ] **Step 1.4: Verify spike coverage**

Run:

```powershell
rg -n "Zombie Golem 26.1.2 Spike|village-area maintenance|generated zombie-village|zombie-villager template|targeting|faction|vanilla Iron Golem counterattack|too invasive|conversion|Rotten Flesh healing|spawn egg|spawner" docs/26.1.2-mojang-targets.md
```

Expected: every required spike marker appears under the new Zombie Golem section.

- [ ] **Step 1.5: Commit spike**

Run:

```powershell
git add docs/26.1.2-mojang-targets.md
git commit -m "docs: spike zombie golem source hooks"
```

Expected: commit succeeds. If any stop condition is hit, commit only the spike evidence and stop before gameplay implementation.

---

### Task 2: Variant Identity And Permissions

**Files:**
- Modify: `src/test/java/dev/charles/multigolem/GolemVariantTest.java`
- Modify: `src/test/java/dev/charles/multigolem/permissions/MultiGolemPermissionsTest.java`
- Modify: `src/main/java/dev/charles/multigolem/GolemVariant.java`

- [ ] **Step 2.1: Add failing variant tests**

Add tests:

```java
@Test
void zombieUsesMossyCobblestoneAndRottenFlesh() {
    assertEquals(GolemVariant.ZOMBIE, GolemVariant.fromBodyBlock(Blocks.MOSSY_COBBLESTONE).orElseThrow());
    assertTrue(GolemVariant.ZOMBIE.matchesBodyBlock(Blocks.MOSSY_COBBLESTONE.defaultBlockState()));
    assertEquals(GolemVariant.ZOMBIE, GolemVariant.fromIngot(Items.ROTTEN_FLESH).orElseThrow());
    assertEquals(Items.ROTTEN_FLESH, GolemVariant.ZOMBIE.dropItem());
    assertEquals("zombie", GolemVariant.ZOMBIE.id());
    assertEquals("Zombie", GolemVariant.ZOMBIE.displayName());
}
```

Add permission node assertions:

```java
@Test
void zombiePermissionNodesArePermissiveByDefault() {
    assertTrue(MultiGolemPermissions.canCreate(GolemVariant.ZOMBIE, (node, defaultValue) -> defaultValue));
    assertTrue(MultiGolemPermissions.canHeal(GolemVariant.ZOMBIE, (node, defaultValue) -> defaultValue));
    assertEquals("multigolem.create.zombie", MultiGolemPermissions.createNode(GolemVariant.ZOMBIE));
    assertEquals("multigolem.heal.zombie", MultiGolemPermissions.healNode(GolemVariant.ZOMBIE));
}
```

- [ ] **Step 2.2: Run tests to verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.GolemVariantTest --tests dev.charles.multigolem.permissions.MultiGolemPermissionsTest
```

Expected: fail because `GolemVariant.ZOMBIE` does not exist.

- [ ] **Step 2.3: Add `ZOMBIE` enum value**

In `GolemVariant.java`, add:

```java
ZOMBIE("zombie", "Zombie", Blocks.MOSSY_COBBLESTONE, Items.ROTTEN_FLESH, Items.ROTTEN_FLESH)
```

Keep `IRON` as a vanilla-owned variant and keep the enum maps generated from `values()`.

- [ ] **Step 2.4: Run tests to verify pass**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.GolemVariantTest --tests dev.charles.multigolem.permissions.MultiGolemPermissionsTest
```

Expected: pass.

- [ ] **Step 2.5: Commit variant identity**

Run:

```powershell
git add src/main/java/dev/charles/multigolem/GolemVariant.java src/test/java/dev/charles/multigolem/GolemVariantTest.java src/test/java/dev/charles/multigolem/permissions/MultiGolemPermissionsTest.java
git commit -m "feat: add zombie golem variant identity"
```

Expected: commit succeeds.

---

### Task 3: Config Defaults, Validation, And Desired Count Helper

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/config/TierStats.java`
- Modify: `src/main/java/dev/charles/multigolem/config/MultiGolemConfig.java`
- Create: `src/main/java/dev/charles/multigolem/spawn/ZombieVillageSpawningConfig.java`
- Modify: `src/test/java/dev/charles/multigolem/config/MultiGolemConfigTest.java`
- Modify: `src/test/java/dev/charles/multigolem/config/MultiGolemConfigV2Test.java`
- Create: `src/test/java/dev/charles/multigolem/config/ZombieVillageSpawningConfigTest.java`

- [ ] **Step 3.1: Add failing config tests**

Add assertions that defaults include:

```java
TierStats zombie = MultiGolemConfig.defaults().tier(GolemVariant.ZOMBIE);
assertEquals(100, zombie.maxHealth());
assertEquals(15.0, zombie.attackDamage(), 0.0001);
assertTrue(zombie.angerOnHit(), "schema keeps anger_on_hit, runtime ignores it for ZOMBIE");
assertEquals(List.of(), zombie.ignoredTargetTypes());
assertEquals(25.0, zombie.zombieRottenFleshHealAmount(), 0.0001);
assertTrue(zombie.zombieHungerEnabled());
assertEquals(12, zombie.zombieHungerSeconds());
assertTrue(zombie.zombieNauseaEnabled());
assertEquals(4, zombie.zombieNauseaSeconds());
assertTrue(zombie.zombiePoisonEnabled());
assertEquals(4, zombie.zombiePoisonSeconds());
assertTrue(zombie.zombieConvertVillagersEnabled());
assertEquals(1.0, zombie.zombieVillagerConversionChance(), 0.0001);
assertTrue(zombie.zombieConvertWanderingTradersEnabled());
assertEquals(1.0, zombie.zombieWanderingTraderConversionChance(), 0.0001);
```

Add desired-count tests:

```java
ZombieVillageSpawningConfig cfg = ZombieVillageSpawningConfig.defaults();
assertEquals(0, cfg.desiredCount(0, 10, 0));
assertEquals(1, cfg.desiredCount(1, 0, 0));
assertEquals(1, cfg.desiredCount(4, 0, 0));
assertEquals(2, cfg.desiredCount(5, 0, 0));
assertEquals(0, cfg.desiredCount(0, 3, 0), "regular zombies alone never qualify");
assertEquals(2, cfg.desiredCount(1, 3, 0), "regular zombies can add bonus only after one zombie villager qualifies");
assertEquals(0, cfg.withMaxZombieGolemsPerVillage(0).desiredCount(10, 10, 0));
```

- [ ] **Step 3.2: Run tests to verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.config.MultiGolemConfigTest --tests dev.charles.multigolem.config.MultiGolemConfigV2Test --tests dev.charles.multigolem.config.ZombieVillageSpawningConfigTest
```

Expected: fail because Zombie config accessors and `ZombieVillageSpawningConfig` do not exist.

- [ ] **Step 3.3: Add config model**

Extend `TierStats` with Zombie fields after Netherite:

```java
Double zombieRottenFleshHealAmount,
Boolean zombieHungerEnabled,
Integer zombieHungerSeconds,
Integer zombieHungerAmplifier,
Boolean zombieNauseaEnabled,
Integer zombieNauseaSeconds,
Integer zombieNauseaAmplifier,
Boolean zombiePoisonEnabled,
Integer zombiePoisonSeconds,
Integer zombiePoisonAmplifier,
Boolean zombieConvertVillagersEnabled,
Double zombieVillagerConversionChance,
Boolean zombieConvertWanderingTradersEnabled,
Double zombieWanderingTraderConversionChance
```

Create `ZombieVillageSpawningConfig` with:

```java
public record ZombieVillageSpawningConfig(
    boolean enabled,
    int minZombieVillagers,
    int zombieVillagersPerGolem,
    boolean regularZombieBonusEnabled,
    int regularZombieBonusThreshold,
    int maxZombieGolemsPerVillage
) {
    public static ZombieVillageSpawningConfig defaults() {
        return new ZombieVillageSpawningConfig(true, 1, 5, true, 3, 2);
    }

    public int desiredCount(int zombieVillagers, int regularZombies, int currentZombieGolems) {
        if (!enabled || maxZombieGolemsPerVillage <= 0 || zombieVillagers < minZombieVillagers) return 0;
        int desired = 1 + ((zombieVillagers - minZombieVillagers) / zombieVillagersPerGolem);
        if (regularZombieBonusEnabled && regularZombies >= regularZombieBonusThreshold) desired++;
        return Math.max(0, Math.min(maxZombieGolemsPerVillage, desired));
    }

    public ZombieVillageSpawningConfig withMaxZombieGolemsPerVillage(int max) {
        return new ZombieVillageSpawningConfig(enabled, minZombieVillagers, zombieVillagersPerGolem,
            regularZombieBonusEnabled, regularZombieBonusThreshold, max);
    }
}
```

Add `zombieVillageSpawning()` to `MultiGolemConfig`, JSON defaults for `zombie_village_spawning`, validation clamps, parsing, and writeback. Clamp durations and amplifiers to non-negative values, conversion chances to `0.0` through `1.0`, heal amount to `0.0` through `2048.0`, `min_zombie_villagers`, `zombie_villagers_per_golem`, and `regular_zombie_bonus_threshold` to at least `1`, and `max_zombie_golems_per_village` to `0` or higher.

- [ ] **Step 3.4: Run config tests to verify pass**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.config.MultiGolemConfigTest --tests dev.charles.multigolem.config.MultiGolemConfigV2Test --tests dev.charles.multigolem.config.ZombieVillageSpawningConfigTest
```

Expected: pass and generated config preserves unknown fields while adding `tiers.zombie` and `zombie_village_spawning`.

- [ ] **Step 3.5: Commit config**

Run:

```powershell
git add src/main/java/dev/charles/multigolem/config/TierStats.java src/main/java/dev/charles/multigolem/config/MultiGolemConfig.java src/main/java/dev/charles/multigolem/spawn/ZombieVillageSpawningConfig.java src/test/java/dev/charles/multigolem/config/MultiGolemConfigTest.java src/test/java/dev/charles/multigolem/config/MultiGolemConfigV2Test.java src/test/java/dev/charles/multigolem/config/ZombieVillageSpawningConfigTest.java
git commit -m "feat: configure zombie golem defaults"
```

Expected: commit succeeds.

---

### Task 4: Player-Built Creation And Rotten Flesh Healing

**Files:**
- Modify: `src/test/java/dev/charles/multigolem/GolemVariantTest.java`
- Create or Modify: current creation-handler tests if present; otherwise create `src/test/java/dev/charles/multigolem/spawn/GolemCreationHandlerTest.java`
- Modify: `src/test/java/dev/charles/multigolem/mixin/IronGolemMixinTest.java` if available; otherwise extend the existing healing test file chosen by the spike.
- Modify: `src/main/java/dev/charles/multigolem/spawn/GolemCreationHandler.java`
- Modify: `src/main/java/dev/charles/multigolem/mixin/IronGolemMixin.java`

- [ ] **Step 4.1: Add failing creation and healing tests**

Add tests that prove:

```java
assertEquals(GolemVariant.ZOMBIE, GolemVariant.fromBodyBlock(Blocks.MOSSY_COBBLESTONE).orElseThrow());
assertEquals(GolemVariant.ZOMBIE, GolemVariant.fromIngot(Items.ROTTEN_FLESH).orElseThrow());
```

Add healing tests with mocked or harnessed `IronGolem` interactions. If Task 1 names a different healing test file than `IronGolemMixinTest`, use that exact file in every test command and commit step in this task:

```java
@Test
void deniedZombieRottenFleshHealingDoesNotHealOrConsume() {
    IronGolem golem = damagedGolem(GolemVariant.ZOMBIE, 50.0F);
    Player player = playerHolding(Items.ROTTEN_FLESH, 1);
    denyPermission("multigolem.heal.zombie");

    InteractionResult result = invokeHeal(golem, player, InteractionHand.MAIN_HAND);

    assertEquals(InteractionResult.FAIL, result);
    assertEquals(50.0F, golem.getHealth(), 0.0001F);
    assertEquals(1, player.getItemInHand(InteractionHand.MAIN_HAND).getCount());
}

@Test
void allowedZombieRottenFleshHealingUsesConfiguredAmountAndConsumesOneItem() {
    IronGolem golem = damagedGolem(GolemVariant.ZOMBIE, 50.0F);
    Player player = playerHolding(Items.ROTTEN_FLESH, 2);
    allowPermission("multigolem.heal.zombie");
    setZombieRottenFleshHealAmount(25.0);

    InteractionResult result = invokeHeal(golem, player, InteractionHand.MAIN_HAND);

    assertEquals(InteractionResult.SUCCESS, result);
    assertEquals(75.0F, golem.getHealth(), 0.0001F);
    assertEquals(1, player.getItemInHand(InteractionHand.MAIN_HAND).getCount());
}

@Test
void fullHealthZombieRottenFleshHealingNoOpsBeforePermissionCheck() {
    IronGolem golem = fullHealthGolem(GolemVariant.ZOMBIE);
    Player player = playerHolding(Items.ROTTEN_FLESH, 1);

    InteractionResult result = invokeHeal(golem, player, InteractionHand.MAIN_HAND);

    assertEquals(InteractionResult.PASS, result);
    assertPermissionNotChecked("multigolem.heal.zombie");
    assertEquals(1, player.getItemInHand(InteractionHand.MAIN_HAND).getCount());
}

@Test
void zombieGolemDoesNotHealFromIronIngotFallback() {
    IronGolem golem = damagedGolem(GolemVariant.ZOMBIE, 50.0F);
    Player player = playerHolding(Items.IRON_INGOT, 1);

    InteractionResult result = invokeHeal(golem, player, InteractionHand.MAIN_HAND);

    assertNotEquals(InteractionResult.SUCCESS, result);
    assertEquals(50.0F, golem.getHealth(), 0.0001F);
    assertEquals(1, player.getItemInHand(InteractionHand.MAIN_HAND).getCount());
}
```

- [ ] **Step 4.2: Run tests to verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.GolemVariantTest --tests "*GolemCreationHandlerTest" --tests "*IronGolemMixinTest"
```

Expected: fail for missing Zombie creation/healing behavior. Replace `*IronGolemMixinTest` with the exact healing test file name confirmed by Task 1 if the spike selects a different test file.

- [ ] **Step 4.3: Implement creation and healing**

Use the existing `GolemCreationHandler.trySpawnVariant(...)` path so Mossy Cobblestone T-pattern creation consumes blocks only after `multigolem.create.zombie` passes. In `IronGolemMixin`, replace hard-coded `self.heal(25.0F)` for non-iron variants with:

```java
float healAmount = variant == GolemVariant.ZOMBIE
    ? MultiGolem.config().tier(GolemVariant.ZOMBIE).zombieRottenFleshHealAmount().floatValue()
    : 25.0F;
self.heal(healAmount);
```

Keep full-health no-op before permission checks and wrong-item handling before vanilla fallback. Wrong healing items must not let vanilla iron-ingot healing heal Zombie Golems.

- [ ] **Step 4.4: Run tests to verify pass**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.GolemVariantTest --tests "*GolemCreationHandlerTest" --tests "*IronGolemMixinTest"
```

Expected: pass. Replace `*IronGolemMixinTest` with the exact healing test file name confirmed by Task 1 if the spike selects a different test file.

- [ ] **Step 4.5: Commit creation and healing**

Run:

```powershell
git add src/main/java/dev/charles/multigolem/spawn/GolemCreationHandler.java src/main/java/dev/charles/multigolem/mixin/IronGolemMixin.java src/test/java/dev/charles/multigolem/GolemVariantTest.java src/test/java/dev/charles/multigolem/spawn/GolemCreationHandlerTest.java src/test/java/dev/charles/multigolem/mixin/IronGolemMixinTest.java
git commit -m "feat: create and heal zombie golems"
```

Expected: commit succeeds. If the actual healing test file has a different name from the spike, stage that exact file instead.

---

### Task 5: Zombie Faction, Targeting, And Counterattack

**Files:**
- Create: `src/test/java/dev/charles/multigolem/ability/ZombieGolemFactionTest.java`
- Create: `src/main/java/dev/charles/multigolem/ability/ZombieGolemFaction.java`
- Modify: `src/main/java/dev/charles/multigolem/mixin/GolemTargetingMixin.java`
- Modify: `src/main/java/dev/charles/multigolem/mixin/LivingEntityMixin.java`
- Create: `src/main/java/dev/charles/multigolem/mixin/IronGolemRegisterGoalsMixin.java` if required by Task 1 spike.
- Modify: `src/main/resources/multigolem.mixins.json` if a new mixin is created.

- [ ] **Step 5.1: Add failing faction tests**

Add tests for this table:

```java
// Zombie Golem targets: survival/adventure players, villagers, wandering traders,
// vanilla Iron Golems, Copper, Gold, Emerald, Diamond, Netherite, and any non-zombie MultiGolems.
// Zombie Golem ignores: zombies, zombie villagers, Zombie Golems.
// Non-zombie MultiGolems and vanilla Iron Golems attack Zombie Golems.
// Trader llamas are not added as special targets.
// anger_on_hit is ignored for ZOMBIE and cannot make Zombie Golems passive.
```

Use helpers that accept `GolemVariant attackerVariant`, `Entity target`, and `TargetRole`.

Add a mixin-level anger suppression test:

```java
@Test
void zombieGolemRetaliationIsNotSuppressedByAngerOnHitFalse() {
    IronGolem zombieGolem = golemWithVariant(GolemVariant.ZOMBIE);
    Player attacker = survivalPlayer();
    setTierAngerOnHit(GolemVariant.ZOMBIE, false);

    boolean cancelled = invokeSetLastHurtByMobSuppression(zombieGolem, attacker);

    assertFalse(cancelled, "anger_on_hit is ignored for ZOMBIE; retaliation must remain enabled");
}
```

- [ ] **Step 5.2: Run tests to verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.ability.ZombieGolemFactionTest --tests dev.charles.multigolem.ability.TargetFilterTest
```

Expected: fail because `ZombieGolemFaction` and the Zombie-specific `LivingEntityMixin` anger assertion do not exist.

- [ ] **Step 5.3: Implement faction helper**

Create `ZombieGolemFaction`:

```java
public final class ZombieGolemFaction {
    private ZombieGolemFaction() {}

    public static boolean zombieGolemShouldTarget(Entity target) {
        if (target instanceof Zombie || target instanceof ZombieVillager) return false;
        if (target instanceof IronGolem golem) {
            return GolemVariantAttachment.get(golem) != GolemVariant.ZOMBIE;
        }
        return target instanceof Player
            || target instanceof Villager
            || target instanceof WanderingTrader;
    }

    public static boolean nonZombieGolemShouldTarget(Entity target) {
        return target instanceof IronGolem golem
            && GolemVariantAttachment.get(golem) == GolemVariant.ZOMBIE;
    }
}
```

Adjust imports and exact type checks according to the source spike. Do not add trader llama targeting.

- [ ] **Step 5.4: Wire targeting hooks**

Use the hook selected in Task 1. If `Mob#setTarget` is sufficient, extend `GolemTargetingMixin` so:

```java
if (variant == GolemVariant.ZOMBIE && !ZombieGolemFaction.zombieGolemShouldTarget(target)) {
    ci.cancel();
    return;
}
if (variant != GolemVariant.ZOMBIE && target instanceof IronGolem targetGolem
        && GolemVariantAttachment.get(targetGolem) == GolemVariant.ZOMBIE) {
    return;
}
```

If proactive targeting needs goal injection, add `IronGolemRegisterGoalsMixin` with only the goals proven by Task 1 and register it in `multigolem.mixins.json`.

In `LivingEntityMixin`, keep any existing `anger_on_hit=false` suppression disabled for `GolemVariant.ZOMBIE`:

```java
GolemVariant variant = GolemVariantAttachment.get(golem);
if (variant == GolemVariant.ZOMBIE) return;
if (!MultiGolem.config().tier(variant).angerOnHit()) {
    ci.cancel();
}
```

- [ ] **Step 5.5: Run tests to verify pass**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.ability.ZombieGolemFactionTest --tests dev.charles.multigolem.ability.TargetFilterTest
.\gradlew.bat --quiet test --tests dev.charles.multigolem.mixin.LivingEntityMixinTest
.\gradlew.bat --quiet compileJava
```

Expected: pass and compile succeeds.

- [ ] **Step 5.6: Commit faction and targeting**

Run:

```powershell
git add src/main/java/dev/charles/multigolem/ability/ZombieGolemFaction.java src/main/java/dev/charles/multigolem/mixin/GolemTargetingMixin.java src/main/java/dev/charles/multigolem/mixin/LivingEntityMixin.java src/main/java/dev/charles/multigolem/mixin/IronGolemRegisterGoalsMixin.java src/main/resources/multigolem.mixins.json src/test/java/dev/charles/multigolem/ability/ZombieGolemFactionTest.java src/test/java/dev/charles/multigolem/mixin/LivingEntityMixinTest.java
git commit -m "feat: add zombie golem faction targeting"
```

Expected: commit succeeds. Omit `IronGolemRegisterGoalsMixin.java` from `git add` if the source spike did not require it.

---

### Task 6: Player Sickness And Civilian Conversion

**Files:**
- Create: `src/test/java/dev/charles/multigolem/ability/ZombieGolemEffectsTest.java`
- Create: `src/test/java/dev/charles/multigolem/ability/ZombieGolemConversionTest.java`
- Modify: `src/test/java/dev/charles/multigolem/mixin/IronGolemAttackMixinTest.java`
- Create: `src/main/java/dev/charles/multigolem/ability/ZombieGolemEffects.java`
- Create: `src/main/java/dev/charles/multigolem/ability/ZombieGolemConversion.java`
- Modify: `src/main/java/dev/charles/multigolem/mixin/IronGolemAttackMixin.java`

- [ ] **Step 6.1: Add failing sickness tests**

Add tests that verify enabled defaults apply:

```java
// Hunger for 12 seconds amplifier 0.
// Nausea for 4 seconds amplifier 0.
// Poison for 4 seconds amplifier 0.
// Disabled effects are skipped independently.
// Effects apply only after IronGolem#doHurtTarget returns true and only for players.
```

- [ ] **Step 6.2: Add failing conversion tests**

Add tests that verify:

```java
// Villager successful conversion creates ZombieVillager and applies no normal damage.
// WanderingTrader successful conversion creates ZombieVillager and applies no normal damage.
// Failed villager conversion roll deals no normal damage.
// Failed wandering-trader conversion roll deals no normal damage.
// Villager profession, villager type, custom name, baby/adult state, persistence, and relevant NBT are preserved where the source spike says safe.
// Wandering trader custom name and persistence are preserved where the source spike says safe.
// Trader llamas are ignored.
```

Add mixin integration tests for the no-normal-damage path:

```java
@Test
void villagerConversionSuccessCancelsNormalIronGolemDamage() {
    IronGolem zombieGolem = golemWithVariant(GolemVariant.ZOMBIE);
    Villager villager = villagerWithHealth(20.0F);
    forceConversionRoll(0.0);

    boolean vanillaDamageCalled = invokeZombieCivilianAttackHook(zombieGolem, villager);

    assertFalse(vanillaDamageCalled);
    assertEquals(20.0F, villager.getHealth(), 0.0001F);
    assertZombieVillagerReplacementSpawned();
}

@Test
void villagerConversionFailedRollCancelsNormalIronGolemDamage() {
    IronGolem zombieGolem = golemWithVariant(GolemVariant.ZOMBIE);
    Villager villager = villagerWithHealth(20.0F);
    forceConversionRoll(1.0);
    setZombieVillagerConversionChance(0.0);

    boolean vanillaDamageCalled = invokeZombieCivilianAttackHook(zombieGolem, villager);

    assertFalse(vanillaDamageCalled);
    assertEquals(20.0F, villager.getHealth(), 0.0001F);
    assertNoZombieVillagerReplacementSpawned();
}

@Test
void wanderingTraderConversionSuccessOrFailedRollCancelsNormalIronGolemDamage() {
    IronGolem zombieGolem = golemWithVariant(GolemVariant.ZOMBIE);
    WanderingTrader trader = wanderingTraderWithHealth(20.0F);
    setZombieWanderingTraderConversionChance(0.0);

    boolean vanillaDamageCalled = invokeZombieCivilianAttackHook(zombieGolem, trader);

    assertFalse(vanillaDamageCalled);
    assertEquals(20.0F, trader.getHealth(), 0.0001F);
}
```

- [ ] **Step 6.3: Run tests to verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.ability.ZombieGolemEffectsTest --tests dev.charles.multigolem.ability.ZombieGolemConversionTest --tests dev.charles.multigolem.mixin.IronGolemAttackMixinTest
```

Expected: fail because helpers and dispatch are not implemented.

- [ ] **Step 6.4: Implement sickness helper**

Create `ZombieGolemEffects.applyPlayerSickness(ServerLevel level, Player player, TierStats stats)`:

```java
if (Boolean.TRUE.equals(stats.zombieHungerEnabled())) {
    player.addEffect(new MobEffectInstance(MobEffects.HUNGER, stats.zombieHungerSeconds() * 20, stats.zombieHungerAmplifier()));
}
if (Boolean.TRUE.equals(stats.zombieNauseaEnabled())) {
    player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, stats.zombieNauseaSeconds() * 20, stats.zombieNauseaAmplifier()));
}
if (Boolean.TRUE.equals(stats.zombiePoisonEnabled())) {
    player.addEffect(new MobEffectInstance(MobEffects.POISON, stats.zombiePoisonSeconds() * 20, stats.zombiePoisonAmplifier()));
}
```

- [ ] **Step 6.5: Implement conversion helper**

Create `ZombieGolemConversion.tryConvert(ServerLevel level, Entity target, TierStats stats, RandomSource random)` returning an enum result:

```java
public enum ConversionResult {
    NOT_CIVILIAN,
    CONVERTED,
    FAILED_ROLL_NO_DAMAGE,
    FAILED_ERROR_NO_DAMAGE
}
```

For `Villager` and `WanderingTrader`, roll configured chance. On success, create `EntityType.ZOMBIE_VILLAGER`, copy supported data from Task 1 evidence, place it at the target position, add it to the level, and discard the original. On failed roll, leave the original alive and return `FAILED_ROLL_NO_DAMAGE`. Normal Iron Golem damage must not be applied for either success or failed roll.

- [ ] **Step 6.6: Wire attack mixin**

In the cancellable attack hook selected by Task 1, handle `GolemVariant.ZOMBIE` before vanilla damage is applied to villagers or wandering traders. Do not rely on the current TAIL effect hook to prevent normal damage; the conversion hook must run early enough to cancel or bypass vanilla Iron Golem damage for both successful conversions and failed conversion rolls.

```java
if (variant == GolemVariant.ZOMBIE) {
    TierStats stats = MultiGolem.config().tier(GolemVariant.ZOMBIE);
    ZombieGolemConversion.ConversionResult result =
        ZombieGolemConversion.tryConvert(level, target, stats, self.getRandom());
    if (result != ZombieGolemConversion.ConversionResult.NOT_CIVILIAN) {
        cir.setReturnValue(true);
        return;
    }
}
```

Keep player sickness in the post-success attack-effect hook so Hunger, Nausea, and Poison apply only after successful player hits. The required behavior is unchanged: conversion hits and failed conversion rolls deal no normal damage.

- [ ] **Step 6.7: Run tests to verify pass**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.ability.ZombieGolemEffectsTest --tests dev.charles.multigolem.ability.ZombieGolemConversionTest --tests dev.charles.multigolem.mixin.IronGolemAttackMixinTest
.\gradlew.bat --quiet compileJava
```

Expected: pass and compile succeeds.

- [ ] **Step 6.8: Commit attack behavior**

Run:

```powershell
git add src/main/java/dev/charles/multigolem/ability/ZombieGolemEffects.java src/main/java/dev/charles/multigolem/ability/ZombieGolemConversion.java src/main/java/dev/charles/multigolem/mixin/IronGolemAttackMixin.java src/test/java/dev/charles/multigolem/ability/ZombieGolemEffectsTest.java src/test/java/dev/charles/multigolem/ability/ZombieGolemConversionTest.java src/test/java/dev/charles/multigolem/mixin/IronGolemAttackMixinTest.java
git commit -m "feat: add zombie golem attack effects"
```

Expected: commit succeeds.

---

### Task 7: Zombie-Village Desired-Count Maintenance

**Files:**
- Create: `src/test/java/dev/charles/multigolem/spawn/ZombieVillageSpawnResolverTest.java`
- Create: `src/test/java/dev/charles/multigolem/spawn/ZombieVillageSpawnHandlerTest.java`
- Create: `src/main/java/dev/charles/multigolem/spawn/ZombieVillageSpawnResolver.java`
- Create: `src/main/java/dev/charles/multigolem/spawn/ZombieVillageSpawnHandler.java`
- Modify: `src/main/java/dev/charles/multigolem/mixin/VillagerMixin.java` or the exact village-area hook selected by Task 1.
- Modify: `src/main/resources/multigolem.mixins.json` if a new mixin is selected.

- [ ] **Step 7.1: Add failing resolver tests**

Use `ZombieVillageSpawningConfig.defaults()` and assert:

```java
assertEquals(0, resolver.desiredCount(0, 0, 0));
assertEquals(1, resolver.desiredCount(1, 0, 0));
assertEquals(1, resolver.desiredCount(4, 0, 0));
assertEquals(2, resolver.desiredCount(5, 0, 0));
assertEquals(0, resolver.desiredCount(0, 4, 0), "regular zombies alone never qualify");
assertEquals(2, resolver.desiredCount(1, 3, 0));
assertEquals(2, resolver.desiredCount(20, 20, 0), "default max_zombie_golems_per_village is 2");
```

- [ ] **Step 7.2: Add failing handler tests**

Test the handler with fake scan results:

```java
@Test
void oneZombieVillagerQualifiesButRegularZombiesAloneDoNot() {
    assertEquals(1, handler.desiredSpawnAttempts(scan(1, 0, 0)));
    assertEquals(0, handler.desiredSpawnAttempts(scan(0, 5, 0)));
}

@Test
void existingZombieGolemsCountAgainstDesiredCount() {
    assertEquals(0, handler.desiredSpawnAttempts(scan(5, 3, 2)));
    assertEquals(1, handler.desiredSpawnAttempts(scan(5, 3, 1)));
}

@Test
void recomputesLiveZombieGolemCountBeforeEverySpawnAttempt() {
    FakeZombieVillageScan scan = new FakeZombieVillageScan()
        .withZombieVillagers(5)
        .withRegularZombies(3)
        .withLiveZombieGolemCounts(0, 1, 2);

    handler.maintain(scan);

    assertEquals(List.of("count", "spawn", "count", "spawn", "count"), scan.events());
    assertEquals(2, scan.spawnAttempts());
}

@Test
void conversionCreatedZombieVillagersCannotExceedMaxCap() {
    FakeZombieVillageScan scan = new FakeZombieVillageScan()
        .withZombieVillagers(1)
        .withRegularZombies(3)
        .withLiveZombieGolemCounts(1);

    handler.maintain(scan);

    assertEquals(0, scan.spawnAttempts());
}
```

- [ ] **Step 7.3: Run tests to verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.ZombieVillageSpawnResolverTest --tests dev.charles.multigolem.spawn.ZombieVillageSpawnHandlerTest
```

Expected: fail because resolver/handler do not exist.

- [ ] **Step 7.4: Implement resolver and handler**

Implement `ZombieVillageSpawnResolver` as a pure wrapper around `ZombieVillageSpawningConfig`. Implement `ZombieVillageSpawnHandler` so each spawn attempt does:

```java
int zombieVillagers = scan.countZombieVillagers();
int regularZombies = scan.countRegularZombies();
int currentZombieGolems = scan.countLiveZombieGolems();
int desired = resolver.desiredCount(zombieVillagers, regularZombies, currentZombieGolems);
while (currentZombieGolems < desired) {
    Optional<BlockPos> pos = scan.findSafeVillageSpawnPosition();
    if (pos.isEmpty()) break;
    Optional<IronGolem> spawned = spawnZombieGolem(level, pos.get());
    if (spawned.isEmpty()) break;
    currentZombieGolems = scan.countLiveZombieGolems();
}
```

The current live Zombie Golem count must be recomputed before every maintenance spawn attempt. Spawned golems must use vanilla `IronGolem`, `GolemVariantAttachment.set(golem, GolemVariant.ZOMBIE)`, configured stats, and `GolemSpawnOrigin.VILLAGE` only if Task 1 confirms that origin marker still exists and remains the correct model for village-area maintenance.

- [ ] **Step 7.5: Wire selected village-area hook**

Use Task 1 evidence. If the hook is `Villager#spawnGolemIfNeeded`, call `ZombieVillageSpawnHandler.maintain(...)` near the vanilla village golem maintenance path without requiring living villagers to be the eligibility source. If no safe hook exists, stop with the committed spike and request design review.

- [ ] **Step 7.6: Run tests to verify pass**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.ZombieVillageSpawnResolverTest --tests dev.charles.multigolem.spawn.ZombieVillageSpawnHandlerTest --tests dev.charles.multigolem.spawn.VillageGolemSpawnHandlerTest
.\gradlew.bat --quiet compileJava
```

Expected: pass and compile succeeds.

- [ ] **Step 7.7: Commit maintenance**

Run:

```powershell
git add src/main/java/dev/charles/multigolem/spawn/ZombieVillageSpawnResolver.java src/main/java/dev/charles/multigolem/spawn/ZombieVillageSpawnHandler.java src/main/java/dev/charles/multigolem/mixin/VillagerMixin.java src/main/resources/multigolem.mixins.json src/test/java/dev/charles/multigolem/spawn/ZombieVillageSpawnResolverTest.java src/test/java/dev/charles/multigolem/spawn/ZombieVillageSpawnHandlerTest.java
git commit -m "feat: maintain zombie golems in zombie villages"
```

Expected: commit succeeds. Replace `VillagerMixin.java` with the exact selected hook file if Task 1 chooses another file.

---

### Task 8: Marked Spawn Egg, Spawner Reuse, And Generated Assets

**Files:**
- Modify: `src/test/java/dev/charles/multigolem/spawn/SpawnEggStacksTest.java`
- Modify: `src/test/java/dev/charles/multigolem/spawn/SpawnerVariantMarkerTest.java`
- Modify: `src/main/java/dev/charles/multigolem/spawn/SpawnEggStacks.java`
- Modify: `src/main/java/dev/charles/multigolem/spawn/SpawnerVariantMarker.java` only if tests show a `ZOMBIE` gap.
- Modify: `src/main/resources/assets/multigolem/lang/en_us.json`
- Create: `src/main/resources/assets/multigolem/models/item/zombie_golem_spawn_egg.json` if required.
- Create: `src/main/resources/assets/multigolem/textures/entity/zombie_golem.png` if generated.
- Create: `src/main/resources/assets/multigolem/textures/item/zombie_golem_spawn_egg.png` if generated.
- Modify: `scripts/generate-textures.py` and `scripts/test-generate-textures.py` if generated assets need a new palette entry.

- [ ] **Step 8.1: Add failing egg and spawner tests**

Add assertions:

```java
ItemStack egg = SpawnEggStacks.create(GolemVariant.ZOMBIE);
assertTrue(egg.is(Items.IRON_GOLEM_SPAWN_EGG));
assertEquals(Optional.of(GolemVariant.ZOMBIE), SpawnEggStacks.variantFrom(egg));
assertTrue(SpawnEggStacks.customDataSnbt(egg).contains("variant:\"zombie\""));
```

Add spawner marker assertions:

```java
@Test
void zombieSpawnerMarkerRoundTripsToSpawnedGolemAttachment() {
    CompoundTag spawnDataEntity = new CompoundTag();
    SpawnerVariantMarker.write(spawnDataEntity, GolemVariant.ZOMBIE);
    IronGolem spawned = new IronGolem(EntityType.IRON_GOLEM, level);

    SpawnerVariantMarker.applyIfPresent(spawnDataEntity, spawned);

    assertEquals(GolemVariant.ZOMBIE, GolemVariantAttachment.get(spawned));
}

@Test
void unmarkedSpawnerLeavesIronGolemUnchanged() {
    CompoundTag spawnDataEntity = new CompoundTag();
    IronGolem spawned = new IronGolem(EntityType.IRON_GOLEM, level);

    SpawnerVariantMarker.applyIfPresent(spawnDataEntity, spawned);

    assertEquals(GolemVariant.IRON, GolemVariantAttachment.get(spawned));
}
```

- [ ] **Step 8.2: Run tests to verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.SpawnEggStacksTest --tests dev.charles.multigolem.spawn.SpawnerVariantMarkerTest
```

Expected: fail only for missing Zombie model/variant handling if any V4 code excludes new enum values.

- [ ] **Step 8.3: Implement egg and spawner support**

Keep the item as `minecraft:iron_golem_spawn_egg` with `minecraft:custom_data` marker:

```snbt
{multigolem:{variant:"zombie"}}
```

Use `multigolem.create.zombie` for marked egg spawn and marked spawner configuration. Successful egg and spawner spawns must be immediately hostile because the `ZOMBIE` attachment drives targeting.

- [ ] **Step 8.4: Generate assets if needed**

Run:

```powershell
.\gradlew.bat --quiet genTextures
python scripts/test-generate-textures.py
```

Expected: pass. If generated assets are not needed because V4 item models already derive from the enum without per-variant asset changes, document that in `docs/26.1.2-mojang-targets.md` and do not add unused files.

- [ ] **Step 8.5: Run tests to verify pass**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.SpawnEggStacksTest --tests dev.charles.multigolem.spawn.SpawnerVariantMarkerTest
.\gradlew.bat --quiet compileJava
```

Expected: pass and compile succeeds.

- [ ] **Step 8.6: Commit eggs and assets**

Run:

```powershell
git add src/main/java/dev/charles/multigolem/spawn/SpawnEggStacks.java src/main/java/dev/charles/multigolem/spawn/SpawnerVariantMarker.java src/main/resources/assets/multigolem/lang/en_us.json src/main/resources/assets/multigolem/models/item/zombie_golem_spawn_egg.json src/main/resources/assets/multigolem/textures/entity/zombie_golem.png src/main/resources/assets/multigolem/textures/item/zombie_golem_spawn_egg.png scripts/generate-textures.py scripts/test-generate-textures.py src/test/java/dev/charles/multigolem/spawn/SpawnEggStacksTest.java src/test/java/dev/charles/multigolem/spawn/SpawnerVariantMarkerTest.java
git commit -m "feat: add zombie golem marked egg support"
```

Expected: commit succeeds. Stage only files that exist after the asset decision.

---

### Task 9: Docs, Playtest Checklist, Config Docs, And Changelog

**Files:**
- Modify: `README.md`
- Modify: `docs/playtest-checklist.md`
- Modify: `docs/modrinth-listing.md`
- Modify: `docs/curseforge-listing.md`
- Modify: `CHANGELOG.md`
- Modify: `INTERNAL_CHANGELOG.md` if active notes are being used.

- [ ] **Step 9.1: Add docs text**

Document Zombie Golems as hostile corrupted golems:

```markdown
| Zombie | Mossy Cobblestone | Hostile corrupted golem; heals from Rotten Flesh, spreads zombie villagers, and fights village defenders |
```

Config docs must include `tiers.zombie`, sickness fields, conversion chances, Rotten Flesh healing, and top-level `zombie_village_spawning`.

Permission docs must include:

```markdown
- `multigolem.create.zombie`
- `multigolem.heal.zombie`
```

- [ ] **Step 9.2: Add playtest checklist rows**

Add rows for Mossy Cobblestone T-pattern creation, immediate hostility, Rotten Flesh healing, wrong-item no fallback, Hunger/Nausea/Poison, villager conversion, wandering trader conversion, failed conversion no normal damage, trader llamas ignored, Zombie Golems ignoring zombies and zombie villagers, fights with vanilla Iron Golems and non-zombie MultiGolems, zombie-villager eligibility, regular zombies alone never qualifying, max 2 maintenance cap, current live count before every maintenance spawn attempt, `max_zombie_golems_per_village = 0`, marked egg spawn, marked spawner spawn, and denied `multigolem.create.zombie` / `multigolem.heal.zombie`.

- [ ] **Step 9.3: Add changelog entries**

Under `CHANGELOG.md` `## Unreleased`, add player/server-admin-facing bullets:

```markdown
- Added hostile Zombie Golems built from Mossy Cobblestone, with Rotten Flesh healing, sickness effects on player hits, villager and wandering-trader conversion, marked spawn eggs, and zombie-village maintenance spawning.
- Added permissive-by-default `multigolem.create.zombie` and `multigolem.heal.zombie` permissions.
```

If using `INTERNAL_CHANGELOG.md`, add:

```markdown
- Implemented the reviewed Zombie Golem design without starting Redstone/Lapis or unrelated golem work.
```

- [ ] **Step 9.4: Run docs checks**

Run:

```powershell
python scripts/check-zombie-golem-planning-handoff.py
python scripts/check-changelog-style.py --section Unreleased
python scripts/check-release-docs.py
.\gradlew.bat --quiet checkReleaseNotesStyle checkReleaseDocs checkChangelog
```

Expected: all pass. The planning handoff gate still passes and sees the plan markers.

- [ ] **Step 9.5: Commit docs**

Run:

```powershell
git add README.md docs/playtest-checklist.md docs/modrinth-listing.md docs/curseforge-listing.md CHANGELOG.md INTERNAL_CHANGELOG.md
git commit -m "docs: document zombie golems"
```

Expected: commit succeeds. Omit `INTERNAL_CHANGELOG.md` if unchanged.

---

### Task 10: Final Verification Without Release

**Files:**
- Verify all changed files.

- [ ] **Step 10.1: Run focused verification**

Run:

```powershell
python scripts/check-zombie-golem-planning-handoff.py
python scripts/test-check-zombie-golem-planning-handoff.py
.\gradlew.bat --quiet test --tests dev.charles.multigolem.GolemVariantTest --tests dev.charles.multigolem.config.MultiGolemConfigTest --tests dev.charles.multigolem.config.MultiGolemConfigV2Test --tests dev.charles.multigolem.config.ZombieVillageSpawningConfigTest --tests dev.charles.multigolem.permissions.MultiGolemPermissionsTest --tests dev.charles.multigolem.ability.ZombieGolemFactionTest --tests dev.charles.multigolem.ability.ZombieGolemEffectsTest --tests dev.charles.multigolem.ability.ZombieGolemConversionTest --tests dev.charles.multigolem.spawn.ZombieVillageSpawnResolverTest --tests dev.charles.multigolem.spawn.ZombieVillageSpawnHandlerTest --tests dev.charles.multigolem.spawn.SpawnEggStacksTest --tests dev.charles.multigolem.spawn.SpawnerVariantMarkerTest --tests dev.charles.multigolem.mixin.IronGolemAttackMixinTest
```

Expected: all pass.

- [ ] **Step 10.2: Run full verification**

Run:

```powershell
.\gradlew.bat --quiet check
.\gradlew.bat --quiet build
```

Expected: build succeeds. Release reminder may print because `CHANGELOG.md` has Unreleased entries; do not release.

- [ ] **Step 10.3: Scan for scope drift and placeholders**

Run:

```powershell
rg -n "Redstone|Lapis|Skeleton|T[B]D|T[O]DO|implement\s+later|fill\s+in\s+details|Similar\s+to\s+Task|appropriate\s+error\s+handling" docs/superpowers/plans/2026-05-27-multigolem-zombie-golem.md src README.md docs CHANGELOG.md INTERNAL_CHANGELOG.md
rg -n "zombie|Zombie|Rotten Flesh|Mossy Cobblestone|zombie_village_spawning|multigolem.create.zombie|multigolem.heal.zombie|anger_on_hit" src README.md docs CHANGELOG.md INTERNAL_CHANGELOG.md
```

Expected: first command returns no unrelated golem scope from implementation files except this plan's explicit "do not include" boundaries; no placeholder red flags appear. Second command shows all Zombie Golem surfaces in code, tests, docs, and changelog.

- [ ] **Step 10.4: Commit verification cleanup**

Run:

```powershell
git status --short --branch
git log --oneline -10
```

Expected: clean worktree on the assigned branch after the task commits; no release tag or publishing commit exists.

---

## Final Verification Commands

Run these before asking for implementation review:

```powershell
git rev-parse --show-toplevel
git status --short --branch
python scripts/check-zombie-golem-planning-handoff.py
python scripts/test-check-zombie-golem-planning-handoff.py
python scripts/check-changelog-style.py --section Unreleased
python scripts/check-release-docs.py
.\gradlew.bat --quiet check
.\gradlew.bat --quiet build
rg -n "Redstone|Lapis|Skeleton|T[B]D|T[O]DO|implement\s+later|fill\s+in\s+details|Similar\s+to\s+Task|appropriate\s+error\s+handling" docs/superpowers/plans/2026-05-27-multigolem-zombie-golem.md src README.md docs CHANGELOG.md INTERNAL_CHANGELOG.md
```

Expected:
- Root is `C:/Users/tyler/AI Projects/MultiGolem`.
- Planning handoff, planning tests, changelog style, release docs, Gradle `check`, and Gradle `build` pass.
- Scope scan shows no Redstone/Lapis/Skeleton implementation drift and no placeholder phrases.
- No release is performed.

## Manual Playtest Checklist

- [ ] Build Mossy Cobblestone T-pattern plus carved pumpkin; Zombie Golem spawns.
- [ ] Player-built Zombie Golem immediately attacks the creator.
- [ ] Marked Zombie Golem spawn egg spawns a Zombie Golem and checks `multigolem.create.zombie`.
- [ ] Marked Zombie Golem spawn egg configures a spawner; spawner-spawned Zombie Golem is immediately hostile.
- [ ] Damaged Zombie Golem heals with Rotten Flesh and checks `multigolem.heal.zombie`.
- [ ] Full-health Zombie Golem plus Rotten Flesh is a no-op before permission checks.
- [ ] Damaged Zombie Golem does not heal from Iron Ingot through vanilla fallback.
- [ ] Zombie Golem player hit applies Hunger, Nausea, and Poison defaults.
- [ ] Disabled sickness effects are skipped independently.
- [ ] Villager hit converts to zombie villager and takes no normal damage.
- [ ] Failed villager conversion roll deals no normal damage.
- [ ] Wandering trader hit converts to zombie villager and takes no normal damage.
- [ ] Trader llamas are ignored.
- [ ] Zombie Golems ignore zombies and zombie villagers.
- [ ] Zombie Golems fight vanilla Iron Golems and non-zombie MultiGolems.
- [ ] One zombie villager qualifies an area.
- [ ] Regular zombies alone never qualify.
- [ ] Default max is 2 Zombie Golems per qualifying village area.
- [ ] Current live Zombie Golem count is checked before every maintenance spawn attempt.
- [ ] `max_zombie_golems_per_village = 0` disables maintenance spawning while craft and egg paths still work.

## Self-Review

Spec coverage:
- Covered Mossy Cobblestone T-pattern creation in Tasks 1, 2, and 4.
- Covered immediate hostility, targeting/faction hooks, vanilla Iron Golem counterattack invasiveness, Zombie Golems fighting vanilla Iron Golems and non-zombie MultiGolems, and Zombie Golems ignoring zombies and zombie villagers in Tasks 1 and 5.
- Covered Iron Golem default health/damage, `anger_on_hit` ignored for `ZOMBIE`, sickness defaults, conversion chances, Rotten Flesh healing, permissions, and config validation in Tasks 3, 4, and 6.
- Covered villager and wandering-trader conversion, data preservation, failed conversion rolls deal no normal damage, and trader llamas ignored in Tasks 1 and 6.
- Covered one zombie villager qualifying, regular zombies alone never qualifying, default max 2, desired-count maintenance, and current live Zombie Golem count before every spawn attempt in Tasks 1, 3, and 7.
- Covered marked spawn egg and spawner support in Tasks 1 and 8.
- Covered docs, playtest checklist, config docs, generated assets if needed, release-facing changelog entries, and no release in Tasks 8, 9, and 10.

Placeholder scan:
- No unresolved placeholder language or vague edge-case steps are intentionally left in this plan.
- The only conditional steps are tied to explicit source-spike outcomes and include stop conditions.

Type/signature consistency:
- `GolemVariant.ZOMBIE`, `ZombieVillageSpawningConfig`, `ZombieVillageSpawnResolver`, `ZombieVillageSpawnHandler`, `ZombieGolemFaction`, `ZombieGolemEffects`, and `ZombieGolemConversion` are introduced before later tasks reference them.
- `tiers.zombie`, `zombie_village_spawning`, `multigolem.create.zombie`, `multigolem.heal.zombie`, `GolemVariantAttachment`, and `GolemSpawnOrigin.VILLAGE` are used consistently with current repo naming.

Exact command coverage:
- Every task includes focused failure and pass commands.
- Final verification includes planning gate, planning gate tests, docs checks, Gradle `check`, Gradle `build`, and scope/placeholder scans.
