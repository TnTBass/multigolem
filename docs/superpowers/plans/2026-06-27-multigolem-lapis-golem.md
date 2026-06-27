# Lapis Golem Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement V8 Lapis Golems as fragile anti-magic support defenders with configurable ward protection for allied village entities on Fabric and NeoForge.

**Architecture:** Keep Lapis inside the existing vanilla `IronGolem` plus MultiGolem identity architecture. Common code owns variant identity, config defaults, ward math, entity eligibility, damage/effect predicates, docs-facing data, and tests; Fabric and NeoForge adapters only wire loader-specific tick, damage, and effect-application hooks.

**Tech Stack:** Java 25, Gradle, Minecraft 26.2 Mojang mappings, Fabric API `0.152.2+26.2`, NeoForge `26.2.0.6-beta`, Sponge Mixin, JUnit 6, Mockito, common MultiGolem source sets, loader-specific adapter source sets, Revue review gates.

---

## Scope Boundaries

- Lapis Golem only.
- Use `docs/superpowers/specs/2026-06-26-multigolem-lapis-golem-design.md` and the current `README.md` roadmap as source of truth.
- Do not revive the older combined Redstone/Lapis spec except for historical comparison if a contradiction must be explained.
- Do not add charm, mind control, Resistance, speed, ordinary damage resistance, custom geometry, a new entity type, new blocks, or new items.
- Do not start release publishing, Modrinth/CurseForge upload, Minecraft-Docker deploy, or live server testing unless Tyler explicitly opens that phase.
- Stop for design review if Fabric and NeoForge cannot provide equivalent Lapis ward behavior.

## File Structure

Source spike:
- Modify: `docs/26.2-mojang-targets.md`

Variant, catalog, config, stats, and village weights:
- Modify: `src/common/java/dev/charles/multigolem/GolemVariant.java`
- Modify: `src/common/java/dev/charles/multigolem/catalog/GolemVariantCatalog.java`
- Modify: `src/common/java/dev/charles/multigolem/config/TierStats.java`
- Modify: `src/common/java/dev/charles/multigolem/config/MultiGolemConfig.java`
- Modify: `src/common/java/dev/charles/multigolem/stats/GolemStatsResolver.java`
- Modify: `src/common/java/dev/charles/multigolem/spawn/VillageSpawnWeights.java`
- Modify: `src/common/java/dev/charles/multigolem/attribute/VariantAttributes.java`

Ward behavior:
- Create: `src/common/java/dev/charles/multigolem/ability/LapisAbility.java`
- Modify: `src/fabric/java/dev/charles/multigolem/fabric/ability/FabricAbilityEvents.java`
- Modify: `src/neoforge/java/dev/charles/multigolem/neoforge/ability/NeoForgeAbilityEvents.java`
- Modify loader mixin resource files only if the source spike selects a common mixin fallback for effect application.

Player-facing and data surfaces:
- Modify: `src/common/resources/assets/minecraft/items/iron_golem_spawn_egg.json`
- Create: `src/common/resources/assets/multigolem/models/item/lapis_golem_spawn_egg.json`
- Create: `src/common/resources/assets/multigolem/textures/entity/lapis_golem.png`
- Create: `src/common/resources/assets/multigolem/textures/item/lapis_golem_spawn_egg.png`
- Modify: `src/common/resources/assets/multigolem/lang/en_us.json`
- Modify Golempedia/status/customization files found by:

```powershell
rg -n "Golempedia|ServerCustomizations|VariantCustomization|MultiGolemStatus|VillageSpawnWeights.rollOrder|GolemVariantCatalog" src/common src/commonClient src/fabric src/neoforge src/test
```

Tests:
- Modify: `src/test/java/dev/charles/multigolem/GolemVariantTest.java`
- Modify: `src/test/java/dev/charles/multigolem/catalog/GolemVariantCatalogTest.java`
- Modify: `src/test/java/dev/charles/multigolem/config/MultiGolemConfigTest.java`
- Modify: `src/test/java/dev/charles/multigolem/config/MultiGolemConfigV3Test.java`
- Modify: `src/test/java/dev/charles/multigolem/stats/GolemStatsResolverTest.java`
- Modify: `src/test/java/dev/charles/multigolem/spawn/VillageSpawnWeightsTest.java`
- Modify: `src/test/java/dev/charles/multigolem/permissions/MultiGolemPermissionsTest.java`
- Create: `src/test/java/dev/charles/multigolem/ability/LapisAbilityTest.java`
- Modify: `src/test/java/dev/charles/multigolem/ability/FabricAbilityEventsSourceTest.java`
- Modify: `src/test/java/dev/charles/multigolem/neoforge/NeoForgeEventsSourceTest.java`
- Modify spawn egg, Golempedia, customization, status, and generated asset tests discovered by the source searches above.

Docs:
- Modify: `README.md`
- Modify: `docs/modrinth-listing.md`
- Modify: `docs/curseforge-listing.md`
- Modify: `docs/playtest-checklist.md`
- Modify: `docs/playtest.html`
- Modify: `CHANGELOG.md`
- Modify: `INTERNAL_CHANGELOG.md` if the active release flow still uses it for implementation notes.

---

### Task 1: Source Spike And Stop-Condition Check

**Files:**
- Modify: `docs/26.2-mojang-targets.md`

- [ ] **Step 1.1: Verify root and clean scope**

Run:

```powershell
git rev-parse --show-toplevel
git status --short --branch
```

Expected: root is `C:/Users/tyler/AI Projects/MultiGolem`; worktree contains only intentional Lapis planning or implementation files.

- [ ] **Step 1.2: Locate Lapis implementation surfaces**

Run:

```powershell
rg -n "GolemVariant|GolemVariantCatalog|TierStats|MultiGolemConfig|GolemStatsResolver|VillageSpawnWeights|VariantAttributes|SpawnEggStacks|SpawnerVariantMarker|ServerTickEvents|ALLOW_DAMAGE|ServerMobEffectEvents|LivingIncomingDamageEvent|MobEffectEvent|Golempedia|ServerCustomizations|playtest" src docs README.md CHANGELOG.md INTERNAL_CHANGELOG.md
```

Expected: output identifies common variant/config/stats/village/spawn-egg/surface files plus Fabric and NeoForge ability event adapters.

- [ ] **Step 1.3: Verify 26.2 damage classification API**

Run:

```powershell
javap -classpath "$env:USERPROFILE\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged-deobf\26.2\minecraft-merged-deobf-26.2.jar" net.minecraft.tags.DamageTypeTags
javap -classpath "$env:USERPROFILE\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged-deobf\26.2\minecraft-merged-deobf-26.2.jar" net.minecraft.world.damagesource.DamageTypes
```

Expected evidence from the 2026-06-27 planning read:
- `DamageSource#is(ResourceKey<DamageType>)` is already used in common abilities through `source.is(DamageTypes.LIGHTNING_BOLT)`.
- `DamageTypeTags` has no `IS_MAGIC` constant in 26.2.
- `DamageTypes` has `MAGIC`, `INDIRECT_MAGIC`, `WITHER`, `DRAGON_BREATH`, and `WITHER_SKULL`.

Chosen initial predicate:

```java
source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)
```

Stop and update the design/plan before implementation if the spike proves there is a better 26.2 magic tag or if dragon breath / wither skull are classified differently than the design expects.

- [ ] **Step 1.4: Verify 26.2 effect-application hooks**

Run:

```powershell
javap -classpath "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1\net.fabricmc.fabric-api\fabric-entity-events-v1\5.0.5+06488ac19e\5cbecb675b91d0ebfe2ef3f20339e1a2c76c435\fabric-entity-events-v1-5.0.5+06488ac19e.jar;$env:USERPROFILE\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged-deobf\26.2\minecraft-merged-deobf-26.2.jar" net.fabricmc.fabric.api.entity.event.v1.effect.ServerMobEffectEvents
javap -classpath "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1\net.fabricmc.fabric-api\fabric-entity-events-v1\5.0.5+06488ac19e\5cbecb675b91d0ebfe2ef3f20339e1a2c76c435\fabric-entity-events-v1-5.0.5+06488ac19e.jar;$env:USERPROFILE\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged-deobf\26.2\minecraft-merged-deobf-26.2.jar" "net.fabricmc.fabric.api.entity.event.v1.effect.ServerMobEffectEvents$AllowAdd"
jar tf "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1\net.neoforged\neoforge\26.2.0.6-beta\e020675a2198e9287eca1bd76f9fe80c83c009ad\neoforge-26.2.0.6-beta-sources.jar" | Select-String -Pattern "MobEffectEvent|LivingIncomingDamageEvent"
```

Expected evidence from the 2026-06-27 planning read:
- Fabric has `ServerMobEffectEvents.ALLOW_ADD`.
- Fabric `AllowAdd` signature is `boolean allowAdd(MobEffectInstance, LivingEntity, EffectEventContext)`.
- NeoForge has `net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable`.
- NeoForge `MobEffectEvent.Applicable` exposes `getEffectInstance()`, `setResult(Result)`, `getResult()`, `getApplicationResult()`, and `getEffectSource()`.

Chosen initial hook path:
- Fabric: register `ServerMobEffectEvents.ALLOW_ADD.register(LapisAbility::allowEffectApplication)`.
- NeoForge: add a listener for `MobEffectEvent.Applicable`; call `event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY)` when Lapis blocks the effect. Verify the exact enum name in source before editing because it is source-level API.
- Fallback: if either loader path fails compile, add one narrow common mixin around `LivingEntity#addEffect(MobEffectInstance, Entity)` and list it in both loader mixin JSON resources.

- [ ] **Step 1.5: Record spike evidence**

Append a `## Lapis Golem 26.2 Spike` section to `docs/26.2-mojang-targets.md` with these headings:

```markdown
## Lapis Golem 26.2 Spike

### Lapis Block T-pattern creation
### Lapis Lazuli healing
### Damage classification
### Fabric damage hook
### NeoForge damage hook
### Fabric effect hook
### NeoForge effect hook
### Ward tick cadence
### Status, customizations, Golempedia, docs, and playtest surfaces
### Stop conditions
```

Each heading must name inspected files/classes, chosen hook, rejected hook if any, exact 26.2 API names, and whether implementation may proceed.

- [ ] **Step 1.6: Commit spike**

Run:

```powershell
git add docs/26.2-mojang-targets.md
git commit -m "docs: spike lapis golem hooks"
```

Expected: commit succeeds. If any stop condition is hit, commit only spike evidence and stop before gameplay implementation.

### Task 2: Variant Identity, Catalog, Permissions, And Village Weights

**Files:**
- Modify: `src/common/java/dev/charles/multigolem/GolemVariant.java`
- Modify: `src/common/java/dev/charles/multigolem/catalog/GolemVariantCatalog.java`
- Modify: `src/common/java/dev/charles/multigolem/spawn/VillageSpawnWeights.java`
- Modify: `src/test/java/dev/charles/multigolem/GolemVariantTest.java`
- Modify: `src/test/java/dev/charles/multigolem/catalog/GolemVariantCatalogTest.java`
- Modify: `src/test/java/dev/charles/multigolem/spawn/VillageSpawnWeightsTest.java`
- Modify: `src/test/java/dev/charles/multigolem/permissions/MultiGolemPermissionsTest.java`

- [ ] **Step 2.1: Write failing identity and order tests**

Add assertions equivalent to:

```java
assertEquals(GolemVariant.LAPIS, GolemVariant.fromBodyBlock(Blocks.LAPIS_BLOCK).orElseThrow());
assertTrue(GolemVariant.LAPIS.matchesBodyBlock(Blocks.LAPIS_BLOCK.defaultBlockState()));
assertEquals(GolemVariant.LAPIS, GolemVariant.fromIngot(Items.LAPIS_LAZULI).orElseThrow());
assertEquals(Items.LAPIS_LAZULI, GolemVariant.LAPIS.dropItem());
assertEquals("lapis", GolemVariant.LAPIS.id());
assertEquals("Lapis", GolemVariant.LAPIS.displayName());
assertEquals("multigolem.create.lapis", MultiGolemPermissions.createNode(GolemVariant.LAPIS));
assertEquals("multigolem.heal.lapis", MultiGolemPermissions.healNode(GolemVariant.LAPIS));
assertEquals(List.of(IRON, COPPER, REDSTONE, GOLD, LAPIS, EMERALD, DIAMOND, NETHERITE), VillageSpawnWeights.rollOrder());
assertEquals(5, VillageSpawnWeights.defaults().weight(GolemVariant.LAPIS));
```

- [ ] **Step 2.2: Run tests to verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.GolemVariantTest --tests dev.charles.multigolem.catalog.GolemVariantCatalogTest --tests dev.charles.multigolem.spawn.VillageSpawnWeightsTest --tests dev.charles.multigolem.permissions.MultiGolemPermissionsTest
```

Expected: fail because `GolemVariant.LAPIS` does not exist.

- [ ] **Step 2.3: Add Lapis variant and catalog entry**

Add `LAPIS` between `GOLD` and `EMERALD` in `GolemVariant`:

```java
LAPIS("lapis", "Lapis", Blocks.LAPIS_BLOCK, Items.LAPIS_LAZULI, Items.LAPIS_LAZULI),
```

Add `GolemVariant.LAPIS` to `GolemVariantCatalog.buildSpecs()` with the same spawn egg, loot, player-buildable, permission, and renderable flags as Redstone/Gold/Emerald. Add `case LAPIS -> Items.LAPIS_LAZULI;` to `dropItemFor`.

Update `VillageSpawnWeights.ROLL_ORDER`:

```java
List.of(IRON, COPPER, REDSTONE, GOLD, LAPIS, EMERALD, DIAMOND, NETHERITE)
```

Update defaults so Lapis weight is `5`, Emerald remains present after Lapis, Diamond remains rare, and Netherite remains `0`.

- [ ] **Step 2.4: Run focused tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.GolemVariantTest --tests dev.charles.multigolem.catalog.GolemVariantCatalogTest --tests dev.charles.multigolem.spawn.VillageSpawnWeightsTest --tests dev.charles.multigolem.permissions.MultiGolemPermissionsTest
```

Expected: pass.

- [ ] **Step 2.5: Commit identity slice**

Run:

```powershell
git add src/common/java/dev/charles/multigolem/GolemVariant.java src/common/java/dev/charles/multigolem/catalog/GolemVariantCatalog.java src/common/java/dev/charles/multigolem/spawn/VillageSpawnWeights.java src/test/java/dev/charles/multigolem src/test/java/dev/charles/multigolem/catalog src/test/java/dev/charles/multigolem/spawn src/test/java/dev/charles/multigolem/permissions
git commit -m "feat: add lapis golem identity"
```

Expected: commit succeeds with only Lapis identity, catalog, permission-node, and village-weight changes.

### Task 3: Config Defaults, Validation, Stats Resolver, And Attributes

**Files:**
- Modify: `src/common/java/dev/charles/multigolem/config/TierStats.java`
- Modify: `src/common/java/dev/charles/multigolem/config/MultiGolemConfig.java`
- Modify: `src/common/java/dev/charles/multigolem/stats/GolemStatsResolver.java`
- Modify: `src/common/java/dev/charles/multigolem/attribute/VariantAttributes.java`
- Modify: `src/test/java/dev/charles/multigolem/config/MultiGolemConfigTest.java`
- Modify: `src/test/java/dev/charles/multigolem/config/MultiGolemConfigV3Test.java`
- Modify: `src/test/java/dev/charles/multigolem/stats/GolemStatsResolverTest.java`
- Modify: `src/test/java/dev/charles/multigolem/attribute/VariantAttributesTest.java`

- [ ] **Step 3.1: Write failing config, stats, and attribute tests**

Add tests asserting:

```java
TierStats lapis = MultiGolemConfig.defaults().tier(GolemVariant.LAPIS);
assertEquals(50, lapis.maxHealth());
assertEquals(7.5, lapis.attackDamage(), 0.0001);
assertEquals(List.of("CREEPERS"), lapis.ignoredTargetTypes());
assertTrue(lapis.lapisWardEnabled());
assertEquals(15, lapis.lapisWardRange());
assertEquals(5, lapis.lapisWardScanIntervalTicks());
assertFalse(lapis.lapisWardAffectsPlayers());
assertTrue(lapis.lapisWardMagicDamageEnabled());
assertTrue(lapis.lapisWardEffectCleanupEnabled());
assertEquals(List.of(
    "minecraft:poison",
    "minecraft:wither",
    "minecraft:weakness",
    "minecraft:slowness",
    "minecraft:blindness",
    "minecraft:nausea",
    "minecraft:levitation",
    "minecraft:darkness",
    "minecraft:mining_fatigue"
), lapis.lapisWardEffectIds());
assertTrue(lapis.lapisParticlesEnabled());
```

Add migration/write-back tests showing an older config missing `tiers.lapis` and `village_spawning.weights.lapis` is rewritten with defaults while unknown fields remain present.

Add validation tests:

```java
assertEquals(1, loaded.tier(LAPIS).lapisWardRange());
assertEquals(64, loadedHigh.tier(LAPIS).lapisWardRange());
assertEquals(1, loaded.tier(LAPIS).lapisWardScanIntervalTicks());
assertEquals(200, loadedHigh.tier(LAPIS).lapisWardScanIntervalTicks());
assertTrue(loaded.tier(LAPIS).lapisWardEffectIds().contains("unknown:kept_for_writeback"));
```

Add `VariantAttributesTest` assertions before the red run:

```java
MultiGolemConfig config = MultiGolemConfig.defaults();
assertEquals(50.0F, VariantAttributes.freshSpawnHealth(GolemVariant.LAPIS, config));
assertEquals(config.tier(GolemVariant.LAPIS).maxHealth(),
    VariantAttributes.freshSpawnHealth(GolemVariant.LAPIS, config));
```

For attack damage, add or extend the current attribute-source test so Lapis inherits the existing base-stat delta path and no special-case speed modifier:

```java
assertTrue(source.contains("stats.attackDamage() - IRON_BASE_ATTACK"));
assertFalse(source.contains("GolemVariant.LAPIS) ?"), "Lapis must not add a special attack or speed branch");
```

- [ ] **Step 3.2: Run tests to verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.config.MultiGolemConfigTest --tests dev.charles.multigolem.config.MultiGolemConfigV3Test --tests dev.charles.multigolem.stats.GolemStatsResolverTest --tests dev.charles.multigolem.attribute.VariantAttributesTest
```

Expected: fail because Lapis config fields, resolver accessors, and attribute expectations do not exist.

- [ ] **Step 3.3: Extend `TierStats` and config parser**

Add nullable Lapis fields after Redstone fields:

```java
Boolean lapisWardEnabled,
Integer lapisWardRange,
Integer lapisWardScanIntervalTicks,
Boolean lapisWardAffectsPlayers,
Boolean lapisWardMagicDamageEnabled,
Boolean lapisWardEffectCleanupEnabled,
List<String> lapisWardEffectIds,
Boolean lapisParticlesEnabled
```

Update every `TierStats` constructor bridge, `clampedHealthDamage()`, `defaults()`, `parseTier()`, `toJson()`, and `canonicalizeTierInPlace()` so only the Lapis tier receives non-null Lapis fields.

Clamp:
- `lapis_ward_range`: `1..64`
- `lapis_ward_scan_interval_ticks`: `1..200`

Do not drop invalid `lapis_ward_effect_ids` strings during config canonicalization. Runtime resolution in `LapisAbility` will ignore invalid ids.

- [ ] **Step 3.4: Add typed resolver accessors**

Add accessors to `GolemStatsResolver`:

```java
public boolean lapisWardEnabled() { return config.tier(GolemVariant.LAPIS).lapisWardEnabled(); }
public int lapisWardRange() { return config.tier(GolemVariant.LAPIS).lapisWardRange(); }
public int lapisWardScanIntervalTicks() { return config.tier(GolemVariant.LAPIS).lapisWardScanIntervalTicks(); }
public boolean lapisWardAffectsPlayers() { return config.tier(GolemVariant.LAPIS).lapisWardAffectsPlayers(); }
public boolean lapisWardMagicDamageEnabled() { return config.tier(GolemVariant.LAPIS).lapisWardMagicDamageEnabled(); }
public boolean lapisWardEffectCleanupEnabled() { return config.tier(GolemVariant.LAPIS).lapisWardEffectCleanupEnabled(); }
public List<String> lapisWardEffectIds() { return config.tier(GolemVariant.LAPIS).lapisWardEffectIds(); }
public boolean lapisParticlesEnabled() { return config.tier(GolemVariant.LAPIS).lapisParticlesEnabled(); }
```

`VariantAttributes` should require no special-case beyond default config stats. Implement only the minimal changes needed for the Step 3.1 assertions to pass; if they already pass after config defaults are added, leave `VariantAttributes.java` unchanged.

- [ ] **Step 3.5: Run focused tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.config.MultiGolemConfigTest --tests dev.charles.multigolem.config.MultiGolemConfigV3Test --tests dev.charles.multigolem.stats.GolemStatsResolverTest --tests dev.charles.multigolem.attribute.VariantAttributesTest
```

Expected: pass.

- [ ] **Step 3.6: Commit config slice**

Run:

```powershell
git add src/common/java/dev/charles/multigolem/config src/common/java/dev/charles/multigolem/stats src/common/java/dev/charles/multigolem/attribute src/test/java/dev/charles/multigolem/config src/test/java/dev/charles/multigolem/stats src/test/java/dev/charles/multigolem/attribute
git commit -m "feat: add lapis golem config defaults"
```

Expected: commit succeeds.

### Task 4: Common Lapis Ward Behavior

**Files:**
- Create: `src/common/java/dev/charles/multigolem/ability/LapisAbility.java`
- Create: `src/test/java/dev/charles/multigolem/ability/LapisAbilityTest.java`

- [ ] **Step 4.1: Write failing pure behavior tests**

Cover:
- `shouldScan(now, lastScan, interval)` defaults to every 5 ticks and respects configured interval.
- `isMagicDamage(source)` returns true for `DamageTypes.MAGIC` and `DamageTypes.INDIRECT_MAGIC`.
- `isMagicDamage(source)` returns false for melee, projectile, explosion, fire, lava, fall, drowning, suffocation, and lightning unless the 26.2 spike explicitly proves otherwise.
- `shouldProtectTargetClass` allows villagers, wandering traders, Iron Golems, friendly non-zombie MultiGolems, Lapis itself, and optionally players.
- `shouldProtectTargetClass` rejects Zombie Golems and hostile entities.
- `effectIdConfigured("minecraft:poison")` is true by default.
- `effectIdConfigured("minecraft:strength")` is false by default.
- invalid effect id strings do not throw during resolution.

Use class-level tests where a live `ServerLevel` is not required:

```java
assertTrue(LapisAbility.shouldProtectTargetClass(Villager.class, null, false));
assertTrue(LapisAbility.shouldProtectTargetClass(WanderingTrader.class, null, false));
assertTrue(LapisAbility.shouldProtectTargetClass(IronGolem.class, GolemVariant.IRON, false));
assertTrue(LapisAbility.shouldProtectTargetClass(IronGolem.class, GolemVariant.LAPIS, false));
assertFalse(LapisAbility.shouldProtectTargetClass(IronGolem.class, GolemVariant.ZOMBIE, false));
assertFalse(LapisAbility.shouldProtectTargetClass(Player.class, null, false));
assertTrue(LapisAbility.shouldProtectTargetClass(Player.class, null, true));
```

- [ ] **Step 4.2: Run tests to verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.ability.LapisAbilityTest
```

Expected: fail because `LapisAbility` does not exist.

- [ ] **Step 4.3: Implement common ability helpers and tick scan**

Create `LapisAbility` with these public methods:

```java
public static void onTick(ServerLevel world)
public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount)
public static boolean allowEffectApplication(MobEffectInstance effect, LivingEntity entity)
public static boolean shouldScan(long now, long lastScan, int intervalTicks)
public static boolean isMagicDamage(DamageSource source)
public static boolean shouldProtectTargetClass(Class<? extends LivingEntity> targetClass, GolemVariant targetVariant, boolean playersEnabled)
public static boolean isConfiguredEffect(MobEffectInstance effect, TierStats stats)
```

Implementation rules:
- `onTick` iterates loaded `IronGolem` entities and only handles `GolemVariant.LAPIS`.
- Active means alive, not removed, server ticking, and `lapis_ward_enabled=true`.
- Scan every `lapis_ward_scan_interval_ticks` per Lapis golem. If no persisted Lapis state is added, using `golem.getId()` plus `world.getGameTime()` modulo interval is acceptable for stateless cleanup cadence.
- Find protected entities in `AABB` inflated by `lapis_ward_range`.
- Remove configured effects with `target.removeEffect(holder)` when cleanup is enabled.
- Emit restrained particles only when `lapis_particles_enabled=true`.
- Wrap tick side effects in try/catch and log, matching Redstone's defensive tick pattern.

Damage/effect protection rules:
- Return `true` from `allowDamage` for non-magic damage, disabled ward, no nearby active Lapis ward, invalid target, or dead/inactive Lapis.
- Return `false` from `allowDamage` only for eligible targets protected by a nearby active Lapis ward and magic damage enabled.
- Return `false` from `allowEffectApplication` only for configured effects when cleanup is enabled and target is protected.
- Use runtime registry lookup for configured effect ids; ignore invalid ids without throwing.

- [ ] **Step 4.4: Run focused tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.ability.LapisAbilityTest
```

Expected: pass.

- [ ] **Step 4.5: Commit common ward slice**

Run:

```powershell
git add src/common/java/dev/charles/multigolem/ability/LapisAbility.java src/test/java/dev/charles/multigolem/ability/LapisAbilityTest.java
git commit -m "feat: add lapis ward behavior"
```

Expected: commit succeeds.

### Task 5: Fabric And NeoForge Hook Wiring

**Files:**
- Modify: `src/fabric/java/dev/charles/multigolem/fabric/ability/FabricAbilityEvents.java`
- Modify: `src/neoforge/java/dev/charles/multigolem/neoforge/ability/NeoForgeAbilityEvents.java`
- Modify: `src/test/java/dev/charles/multigolem/ability/FabricAbilityEventsSourceTest.java`
- Modify: `src/test/java/dev/charles/multigolem/neoforge/NeoForgeEventsSourceTest.java`
- Modify loader mixin JSON files only if Task 1 selects the common mixin fallback.

- [ ] **Step 5.1: Write failing hook tests**

Fabric source test assertions:

```java
assertTrue(source.contains("ServerTickEvents.START_LEVEL_TICK.register(LapisAbility::onTick);"));
assertTrue(source.contains("ServerLivingEntityEvents.ALLOW_DAMAGE.register(LapisAbility::allowDamage);"));
assertTrue(source.contains("ServerMobEffectEvents.ALLOW_ADD.register"));
assertFalse(source.contains("net.neoforged"), file);
```

NeoForge source test assertions:

```java
assertTrue(source.contains("LapisAbility.onTick(level);"));
assertTrue(source.contains("| !LapisAbility.allowDamage(entity, source, amount)"));
assertTrue(source.contains("MobEffectEvent.Applicable"));
assertTrue(source.contains("LapisAbility.allowEffectApplication"));
assertFalse(source.contains("net.fabricmc"), file);
```

- [ ] **Step 5.2: Run tests to verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.ability.FabricAbilityEventsSourceTest --tests dev.charles.multigolem.neoforge.NeoForgeEventsSourceTest
```

Expected: fail because Lapis hooks are not wired.

- [ ] **Step 5.3: Wire loader adapters**

Fabric:

```java
ServerTickEvents.START_LEVEL_TICK.register(LapisAbility::onTick);
ServerLivingEntityEvents.ALLOW_DAMAGE.register(LapisAbility::allowDamage);
ServerMobEffectEvents.ALLOW_ADD.register((effect, entity, context) ->
    LapisAbility.allowEffectApplication(effect, entity));
```

NeoForge:

```java
NeoForge.EVENT_BUS.addListener(NeoForgeAbilityEvents::onLivingIncomingDamage);
NeoForge.EVENT_BUS.addListener(NeoForgeAbilityEvents::onMobEffectApplicable);
```

Add:

```java
private static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    Entity entity = event.getEntity();
    DamageSource source = event.getSource();
    float amount = event.getAmount();
    boolean cancelDamage = !CopperAbility.allowDamage(entity, source, amount)
        | !NetheriteAbility.allowDamage(entity, source, amount)
        | !DiamondAbility.allowDamage(entity, source, amount)
        | !LapisAbility.allowDamage(entity, source, amount);
    if (cancelDamage) {
        event.setCanceled(true);
    }
}

private static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
    if (!LapisAbility.allowEffectApplication(event.getEffectInstance(), event.getEntity())) {
        event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }
}
```

Before committing, verify the exact NeoForge damage event and effect result names in source. The current adapter uses `LivingIncomingDamageEvent` for Copper, Netherite, and Diamond damage prevention; extend that existing handler rather than creating a second damage handler unless the spike proves the 26.2 event path changed. If the actual 26.2 effect enum constant differs from `DO_NOT_APPLY`, update both implementation and source test to the real constant.

- [ ] **Step 5.4: Run compile and hook tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.ability.FabricAbilityEventsSourceTest --tests dev.charles.multigolem.neoforge.NeoForgeEventsSourceTest
.\gradlew.bat --quiet compileJava compileFabricJava compileNeoForgeJava
```

Expected: tests and compile pass. If compile fails on Fabric or NeoForge effect hooks, return to Task 1 spike and select the common mixin fallback.

- [ ] **Step 5.5: Commit hook slice**

Run:

```powershell
git add src/fabric/java/dev/charles/multigolem/fabric/ability/FabricAbilityEvents.java src/neoforge/java/dev/charles/multigolem/neoforge/ability/NeoForgeAbilityEvents.java src/test/java/dev/charles/multigolem/ability/FabricAbilityEventsSourceTest.java src/test/java/dev/charles/multigolem/neoforge/NeoForgeEventsSourceTest src/common/resources src/fabric/resources src/neoforge/resources
git commit -m "feat: wire lapis ward hooks"
```

Expected: commit succeeds. Stage mixin resources only if they changed.

### Task 6: Spawn Eggs, Assets, Golempedia, Status, And Customizations

**Files:**
- Modify: `src/common/resources/assets/minecraft/items/iron_golem_spawn_egg.json`
- Create: `src/common/resources/assets/multigolem/models/item/lapis_golem_spawn_egg.json`
- Create: `src/common/resources/assets/multigolem/textures/entity/lapis_golem.png`
- Create: `src/common/resources/assets/multigolem/textures/item/lapis_golem_spawn_egg.png`
- Modify: `src/common/resources/assets/multigolem/lang/en_us.json`
- Modify Golempedia/status/customization files and tests discovered in the File Structure section.

- [ ] **Step 6.1: Write failing surface tests**

Add assertions:

```java
ItemStack egg = SpawnEggStacks.create(GolemVariant.LAPIS);
assertTrue(egg.is(Items.IRON_GOLEM_SPAWN_EGG));
assertEquals(Optional.of(GolemVariant.LAPIS), SpawnEggStacks.variantFrom(egg));
assertTrue(SpawnEggStacks.customDataSnbt(egg).contains("variant:\"lapis\""));
```

Add Golempedia/status/customization tests asserting Lapis appears with:
- Lapis Block body.
- Lapis Lazuli healing/drop.
- 50 HP and 7.5 attack.
- Fragile anti-magic support role.
- 15-block ward.
- Players opt-in.
- Magic damage and configured magic-style effects only.

- [ ] **Step 6.2: Run tests to verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.SpawnEggStacksTest --tests dev.charles.multigolem.spawn.SpawnEggItemDefinitionTest --tests dev.charles.multigolem.golempedia.* --tests dev.charles.multigolem.customizations.* --tests dev.charles.multigolem.status.*
```

Expected: fail for missing Lapis surfaces/assets.

- [ ] **Step 6.3: Add assets and summaries**

Add Lapis spawn egg model and item definition branch using the same marked vanilla iron golem spawn egg convention as existing variants. Add Lapis textures through the existing generated-asset convention if the repository uses generated PNGs; otherwise add explicit PNGs only after confirming current assets are hand-authored.

Update Golempedia, server customizations, and status summaries through common catalog/default data so Fabric and NeoForge transports remain deterministic.

- [ ] **Step 6.4: Run focused surface tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.SpawnEggStacksTest --tests dev.charles.multigolem.spawn.SpawnEggItemDefinitionTest --tests dev.charles.multigolem.golempedia.* --tests dev.charles.multigolem.customizations.* --tests dev.charles.multigolem.status.*
```

Expected: pass.

- [ ] **Step 6.5: Commit surface slice**

Run:

```powershell
git add src/common/resources src/common/java/dev/charles/multigolem/golempedia src/common/java/dev/charles/multigolem/customizations src/common/java/dev/charles/multigolem/status src/test/java/dev/charles/multigolem/spawn src/test/java/dev/charles/multigolem/golempedia src/test/java/dev/charles/multigolem/customizations src/test/java/dev/charles/multigolem/status
git commit -m "feat: expose lapis golem surfaces"
```

Expected: commit succeeds.

### Task 7: Documentation And Playtest Rows

**Files:**
- Modify: `README.md`
- Modify: `docs/modrinth-listing.md`
- Modify: `docs/curseforge-listing.md`
- Modify: `docs/playtest-checklist.md`
- Modify: `docs/playtest.html`
- Modify: `CHANGELOG.md`
- Modify: `INTERNAL_CHANGELOG.md`

- [ ] **Step 7.1: Update public docs**

Update README:
- Opening description includes Lapis.
- Recipe table adds `Lapis Block | Lapis Golem` between Gold and Emerald.
- Stats table adds `Lapis | 50 | 7.5`.
- Special abilities table says Lapis protects nearby allied village entities from magic-tagged damage and configured magic-style harmful effects; players are opt-in; ordinary damage is not blocked.
- Config tier id list includes `lapis`.
- `ignored_target_types` text includes Lapis in the default creeper-ignore list.
- Ability field table includes all `lapis_ward_*` and `lapis_particles_enabled` fields.
- Permission lists include `multigolem.create.lapis` and `multigolem.heal.lapis`.
- Roadmap marks V8 as implemented only when implementation and verification are complete.

Update listing docs with player-facing wording:

```markdown
Lapis Golems are fragile anti-magic support defenders. By default they protect nearby villagers, wandering traders, Iron Golems, and friendly MultiGolems from magic damage and configured harmful magical effects, while ordinary combat, explosions, fire, lava, and falls still work normally. Player protection is disabled by default and can be enabled by server config.
```

- [ ] **Step 7.2: Add manual playtest rows**

Add Fabric and NeoForge rows for:
- Build Lapis from Lapis Blocks and carved pumpkin.
- Heal damaged Lapis with Lapis Lazuli.
- Verify low combat stats compared with Iron.
- Verify creepers are ignored by default.
- Force village spawn weight and see Lapis in village roll.
- Verify protected villagers/golems within 15 blocks avoid magic-tagged damage.
- Verify protected villagers/golems avoid configured effects.
- Verify dragon breath and wither skull follow the exact 26.2 classification from Task 1.
- Verify players are not protected by default.
- Enable player protection and verify players are protected.
- Verify melee, projectile, explosion, fire/lava, fall, drowning, and suffocation are not blocked.
- Verify protection stops when the Lapis Golem dies, unloads, becomes inactive, or ward config is disabled.

- [ ] **Step 7.3: Run docs checks**

Run:

```powershell
.\gradlew.bat --quiet checkChangelog checkReleaseNotesStyle checkReleaseDocs
rg -n "Lapis|lapis|multigolem.create.lapis|multigolem.heal.lapis|lapis_ward|magic-tagged|15 blocks" README.md docs CHANGELOG.md INTERNAL_CHANGELOG.md
```

Expected: checks pass; search output shows Lapis coverage in public docs, playtest docs, and changelogs.

- [ ] **Step 7.4: Commit docs slice**

Run:

```powershell
git add README.md docs/modrinth-listing.md docs/curseforge-listing.md docs/playtest-checklist.md docs/playtest.html CHANGELOG.md
if ((git status --short -- INTERNAL_CHANGELOG.md) -ne "") { git add INTERNAL_CHANGELOG.md }
git commit -m "docs: document lapis golems"
```

Expected: commit succeeds.

### Task 8: Full Verification And Revue Implementation Review

**Files:**
- Review all files changed by Tasks 1-7.

- [ ] **Step 8.1: Run focused behavioral tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.GolemVariantTest --tests dev.charles.multigolem.catalog.GolemVariantCatalogTest --tests dev.charles.multigolem.config.MultiGolemConfigTest --tests dev.charles.multigolem.config.MultiGolemConfigV3Test --tests dev.charles.multigolem.stats.GolemStatsResolverTest --tests dev.charles.multigolem.spawn.VillageSpawnWeightsTest --tests dev.charles.multigolem.permissions.MultiGolemPermissionsTest --tests dev.charles.multigolem.ability.LapisAbilityTest --tests dev.charles.multigolem.ability.FabricAbilityEventsSourceTest --tests dev.charles.multigolem.neoforge.NeoForgeEventsSourceTest --tests dev.charles.multigolem.spawn.SpawnEggStacksTest --tests dev.charles.multigolem.spawn.SpawnEggItemDefinitionTest --tests dev.charles.multigolem.golempedia.* --tests dev.charles.multigolem.customizations.* --tests dev.charles.multigolem.status.*
```

Expected: pass.

- [ ] **Step 8.2: Run full verification gate**

Run:

```powershell
.\gradlew.bat --quiet clean check
```

Expected: pass. If output is large, redirect to a log file and inspect compact failure spans before reading full logs.

- [ ] **Step 8.3: Run placeholder and scope scans**

Run:

```powershell
rg -n "T[B]D|T[O]DO|implement later|fill in details|Similar to Task|appropriate error handling" docs/superpowers/plans/2026-06-27-multigolem-lapis-golem.md src README.md docs CHANGELOG.md INTERNAL_CHANGELOG.md
rg -n "charm|mind-control|Resistance aura|ordinary damage resistance|new entity type|deploy|publish|Modrinth upload|CurseForge upload" docs/superpowers/plans/2026-06-27-multigolem-lapis-golem.md src README.md docs
```

Expected: first command has no plan-placeholder failures. Second command only shows explicit non-goal or release-boundary wording.

- [ ] **Step 8.4: Send implementation diff through Revue**

Use `superpowers-review-gates` and `revue-bridge` with:
- `review_mode`: `implementation-review`
- `review_unit`: explicit files changed by Tasks 1-7
- `exclude`: `.agent-review/**`
- Requirements copied from `docs/superpowers/specs/2026-06-26-multigolem-lapis-golem-design.md`
- Commands run from Steps 8.1-8.3

Before worker execution, paste the Revue dashboard/status/tail/cancel operator links returned by `review_create`.

- [ ] **Step 8.5: Action Revue findings**

Use `superpowers:receiving-code-review` before edits. For each finding:
- Fix valid findings with focused tests.
- Mark false positives only with concrete repo evidence.
- Document accepted risk only if technically justified and Tyler accepts it.

Do not trust parent status alone. Inspect packetized child reviews, synthesis or aggregate state when present, `review_findings_list`, unresolved counts, and `review_closeout_evidence`.

- [ ] **Step 8.6: Final verification after Revue fixes**

Run:

```powershell
.\gradlew.bat --quiet clean check
git status --short --branch
```

Expected: full check passes and git status contains only intentional post-review changes before the final commit.

---

## Self-Review Checklist

- Variant registration/material/config: Tasks 2-3.
- Health, attack damage, healing item, drops, spawn eggs, creative grouping: Tasks 2, 3, and 6.
- Village natural spawn inclusion and final roll order: Task 2.
- Creeper ignore default: Task 3.
- 15-block anti-magic ward: Tasks 3-5.
- Configurable range, scan interval, protected categories, player opt-in, magic damage handling, and effect list: Tasks 3-5.
- Fabric and NeoForge damage/effect hook strategy with exact 26.2 API verification: Tasks 1 and 5.
- Tests and verification: every task plus Task 8.
- README/docs/changelog/playtest updates: Task 7.
- Small reviewable implementation order: Tasks 1-8 with commits per slice.
