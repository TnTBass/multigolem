# Redstone Golem Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement V7 Redstone Golems as lower-strength emergency-control defenders that overcharge below 25% health, gain attack/resistance without speed, and release a Slowness X death overload pulse on Fabric and NeoForge.

**Architecture:** Keep Redstone inside the existing vanilla `IronGolem` plus MultiGolem identity architecture. Common code owns variant identity, config, persisted ability state, Redstone ability math, target filtering, docs-facing data, and tests; Fabric and NeoForge adapters only wire loader-specific tick/damage/death hooks.

**Tech Stack:** Java 25, Gradle, Fabric API, NeoForge, Sponge Mixin, JUnit 6, Mockito, common MultiGolem source sets, loader-specific adapter source sets, Revue review gates.

---

## Scope Boundaries

- Redstone Golem only.
- No Lapis, Skeleton, Copper Golem variants, release publishing, Minecraft-Docker, marketplace work, or broad refactors.
- Do not add movement speed to Redstone. Gold remains the only speed-modified current variant.
- Stop for design review if parity requires different Fabric and NeoForge gameplay behavior.

## File Structure

Source spike:
- Modify: `docs/26.2-mojang-targets.md` if present; otherwise create or append to the current target-notes file identified by the spike.

Variant, catalog, config, and state:
- Modify: `src/common/java/dev/charles/multigolem/GolemVariant.java`
- Modify: `src/common/java/dev/charles/multigolem/catalog/GolemVariantCatalog.java`
- Modify: `src/common/java/dev/charles/multigolem/config/TierStats.java`
- Modify: `src/common/java/dev/charles/multigolem/config/MultiGolemConfig.java`
- Modify: `src/common/java/dev/charles/multigolem/spawn/VillageSpawnWeights.java`
- Modify: `src/common/java/dev/charles/multigolem/attachment/GolemAbilityState.java`

Ability behavior:
- Create: `src/common/java/dev/charles/multigolem/ability/RedstoneAbility.java`
- Modify: `src/common/java/dev/charles/multigolem/attribute/VariantAttributes.java`
- Modify: `src/common/java/dev/charles/multigolem/mixin/IronGolemAttackMixin.java` if the spike chooses the existing attack path for the temporary attack bonus.
- Create or modify the narrow common mixin selected by the spike for death pulse if no existing hook covers it.

Loader adapters:
- Modify: `src/fabric/java/dev/charles/multigolem/fabric/ability/FabricAbilityEvents.java`
- Modify: `src/neoforge/java/dev/charles/multigolem/neoforge/ability/NeoForgeAbilityEvents.java`
- Modify loader mixin resource files only if the spike selects a new common mixin.

Player-facing and data surfaces:
- Modify: `src/common/resources/assets/multigolem/lang/en_us.json`
- Create: `src/common/resources/assets/multigolem/models/item/redstone_golem_spawn_egg.json`
- Create: `src/common/resources/assets/multigolem/textures/entity/redstone_golem.png`
- Create: `src/common/resources/assets/multigolem/textures/item/redstone_golem_spawn_egg.png`
- Modify texture generation scripts if generated assets are the current source of truth.
- Modify Golempedia/status/customization files discovered by `rg -n "Golempedia|ServerCustomizations|VariantCustomization|tier\\(" src/common src/commonClient`.

Tests:
- Modify: `src/test/java/dev/charles/multigolem/GolemVariantTest.java`
- Modify: `src/test/java/dev/charles/multigolem/catalog/GolemVariantCatalogTest.java`
- Modify: `src/test/java/dev/charles/multigolem/config/MultiGolemConfigTest.java`
- Modify: `src/test/java/dev/charles/multigolem/config/MultiGolemConfigV3Test.java`
- Modify: `src/test/java/dev/charles/multigolem/spawn/VillageSpawnWeightsTest.java`
- Modify: `src/test/java/dev/charles/multigolem/permissions/MultiGolemPermissionsTest.java`
- Modify: `src/test/java/dev/charles/multigolem/attachment/GolemAbilityStateTest.java`
- Create: `src/test/java/dev/charles/multigolem/ability/RedstoneAbilityTest.java`
- Modify: `src/test/java/dev/charles/multigolem/attribute/VariantAttributesTest.java`
- Modify loader source tests under `src/test/java/dev/charles/multigolem/ability`, `src/test/java/dev/charles/multigolem/neoforge`, and equivalent Fabric source-test locations.

Docs:
- Modify: `README.md`
- Modify: `docs/playtest-checklist.md`
- Modify: `docs/playtest.html`
- Modify: `docs/modrinth-listing.md`
- Modify: `docs/curseforge-listing.md`
- Modify: `INTERNAL_CHANGELOG.md`
- Modify: `CHANGELOG.md` only after Tyler approves public release-note wording.

---

### Task 1: Source Spike And Stop-Condition Check

**Files:**
- Modify: current target-notes doc selected by `rg --files docs | rg "mojang-targets|target"`

- [ ] **Step 1.1: Verify root and scope**

Run:

```powershell
git rev-parse --show-toplevel
git status --short --branch
```

Expected: root is `C:/Users/tyler/AI Projects/MultiGolem`; worktree contains only intentional Redstone planning or implementation files.

- [ ] **Step 1.2: Locate hooks and surfaces**

Run:

```powershell
rg -n "doHurtTarget|die\\(|hurt\\(|actuallyHurt|LivingDeath|LivingIncomingDamage|ServerTickEvents|LevelTickEvent|MobEffectInstance|MOVEMENT_SPEED|GolemAbilityState|ServerCustomizations|Golempedia|SpawnEggStacks|VillageSpawnWeights" src docs
```

Expected: output identifies creation, healing, attack, damage threshold, death, tick, loader adapter, ability-state, customization/status, Golempedia, spawn egg, and village-weight surfaces.

- [ ] **Step 1.3: Record spike evidence**

Append a `## Redstone Golem 26.2 Spike` section with these headings:

```markdown
## Redstone Golem 26.2 Spike

### Redstone Block T-pattern creation
### Redstone Dust healing
### Threshold detection after damage
### Death overload hook
### Attack bonus implementation path
### Resistance refresh path
### Fabric adapter registration
### NeoForge adapter registration
### Ability-state codec migration
### Status, customizations, Golempedia, and playtest surfaces
### Stop conditions
```

Each heading must name the inspected class or file, chosen hook, rejected hook if any, and whether implementation may proceed. If there is no current 26.2 target-notes doc, create `docs/26.2-mojang-targets.md`.

- [ ] **Step 1.4: Verify spike markers**

Run:

```powershell
rg -n "Redstone Golem 26.2 Spike|Threshold detection|Death overload|Fabric adapter|NeoForge adapter|Ability-state codec|Stop conditions" docs
```

Expected: all headings are present in the selected target-notes doc.

- [ ] **Step 1.5: Commit spike**

Run:

```powershell
git add docs
git commit -m "docs: spike redstone golem hooks"
```

Expected: commit succeeds. If any stop condition is hit, commit only the spike evidence and stop before gameplay implementation.

### Task 2: Variant Identity, Catalog, Permissions, And Village Weights

**Files:**
- Modify: `src/common/java/dev/charles/multigolem/GolemVariant.java`
- Modify: `src/common/java/dev/charles/multigolem/catalog/GolemVariantCatalog.java`
- Modify: `src/common/java/dev/charles/multigolem/spawn/VillageSpawnWeights.java`
- Modify: `src/test/java/dev/charles/multigolem/GolemVariantTest.java`
- Modify: `src/test/java/dev/charles/multigolem/catalog/GolemVariantCatalogTest.java`
- Modify: `src/test/java/dev/charles/multigolem/spawn/VillageSpawnWeightsTest.java`
- Modify: `src/test/java/dev/charles/multigolem/permissions/MultiGolemPermissionsTest.java`

- [ ] **Step 2.1: Write failing tests**

Add tests asserting:

```java
assertEquals(GolemVariant.REDSTONE, GolemVariant.fromBodyBlock(Blocks.REDSTONE_BLOCK).orElseThrow());
assertTrue(GolemVariant.REDSTONE.matchesBodyBlock(Blocks.REDSTONE_BLOCK.defaultBlockState()));
assertEquals(GolemVariant.REDSTONE, GolemVariant.fromIngot(Items.REDSTONE).orElseThrow());
assertEquals(Items.REDSTONE, GolemVariant.REDSTONE.dropItem());
assertEquals("redstone", GolemVariant.REDSTONE.id());
assertEquals("Redstone", GolemVariant.REDSTONE.displayName());
assertEquals("multigolem.create.redstone", MultiGolemPermissions.createNode(GolemVariant.REDSTONE));
assertEquals("multigolem.heal.redstone", MultiGolemPermissions.healNode(GolemVariant.REDSTONE));
```

Update catalog/village tests so roll order is:

```java
List.of(IRON, COPPER, REDSTONE, GOLD, EMERALD, DIAMOND, NETHERITE)
```

and default Redstone village weight is `19`.

- [ ] **Step 2.2: Run tests to verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.GolemVariantTest --tests dev.charles.multigolem.catalog.GolemVariantCatalogTest --tests dev.charles.multigolem.spawn.VillageSpawnWeightsTest --tests dev.charles.multigolem.permissions.MultiGolemPermissionsTest
```

Expected: fail because `GolemVariant.REDSTONE` does not exist.

- [ ] **Step 2.3: Implement identity and catalog**

Add enum value:

```java
REDSTONE("redstone", "Redstone", Blocks.REDSTONE_BLOCK, Items.REDSTONE, Items.REDSTONE)
```

Add `REDSTONE` to catalog specs with loot `3..5`, spawn egg enabled, loot enabled, player-buildable, permission-enabled, and renderable. Add `REDSTONE` to `VillageSpawnWeights.ROLL_ORDER` after Copper and before Gold, and default weight `19`.

- [ ] **Step 2.4: Run tests to verify pass**

Run the command from Step 2.2 again.

Expected: pass.

- [ ] **Step 2.5: Commit identity**

Run:

```powershell
git add src/common/java/dev/charles/multigolem/GolemVariant.java src/common/java/dev/charles/multigolem/catalog/GolemVariantCatalog.java src/common/java/dev/charles/multigolem/spawn/VillageSpawnWeights.java src/test/java/dev/charles/multigolem/GolemVariantTest.java src/test/java/dev/charles/multigolem/catalog/GolemVariantCatalogTest.java src/test/java/dev/charles/multigolem/spawn/VillageSpawnWeightsTest.java src/test/java/dev/charles/multigolem/permissions/MultiGolemPermissionsTest.java
git commit -m "feat: add redstone golem identity"
```

Expected: commit succeeds.

### Task 3: Config Defaults And Validation

**Files:**
- Modify: `src/common/java/dev/charles/multigolem/config/TierStats.java`
- Modify: `src/common/java/dev/charles/multigolem/config/MultiGolemConfig.java`
- Modify: `src/test/java/dev/charles/multigolem/config/MultiGolemConfigTest.java`
- Modify: `src/test/java/dev/charles/multigolem/config/MultiGolemConfigV3Test.java`

- [ ] **Step 3.1: Write failing config tests**

Assert defaults:

```java
TierStats redstone = MultiGolemConfig.defaults().tier(GolemVariant.REDSTONE);
assertEquals(90, redstone.maxHealth());
assertEquals(13.0, redstone.attackDamage(), 0.0001);
assertEquals(List.of("CREEPERS"), redstone.ignoredTargetTypes());
assertTrue(redstone.redstoneOverchargeEnabled());
assertEquals(0.25, redstone.redstoneOverchargeHealthThresholdPercent(), 0.0001);
assertEquals(12.0, redstone.redstoneOverchargeDurationSeconds(), 0.0001);
assertEquals(45.0, redstone.redstoneOverchargeCooldownSeconds(), 0.0001);
assertEquals(1.5, redstone.redstoneOverchargeAttackMultiplier(), 0.0001);
assertEquals(1, redstone.redstoneOverchargeResistanceAmplifier());
assertEquals(3.0, redstone.redstoneOverchargeResistanceRefreshSeconds(), 0.0001);
assertTrue(redstone.redstoneDeathPulseEnabled());
assertEquals(8, redstone.redstoneDeathPulseRadius());
assertEquals(6.0, redstone.redstoneDeathPulseSlownessSeconds(), 0.0001);
assertEquals(9, redstone.redstoneDeathPulseSlownessAmplifier());
assertTrue(redstone.redstoneParticlesEnabled());
assertTrue(redstone.redstoneDeathPulseParticlesEnabled());
```

Add malformed config tests proving threshold, duration, cooldown, multiplier, amplifier, and radius clamp to the design ranges.

- [ ] **Step 3.2: Run tests to verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.config.MultiGolemConfigTest --tests dev.charles.multigolem.config.MultiGolemConfigV3Test
```

Expected: fail because Redstone fields do not exist.

- [ ] **Step 3.3: Add config model**

Extend `TierStats` after Zombie fields with Redstone fields:

```java
Boolean redstoneOverchargeEnabled,
Double redstoneOverchargeHealthThresholdPercent,
Double redstoneOverchargeDurationSeconds,
Double redstoneOverchargeCooldownSeconds,
Double redstoneOverchargeAttackMultiplier,
Integer redstoneOverchargeResistanceAmplifier,
Double redstoneOverchargeResistanceRefreshSeconds,
Boolean redstoneDeathPulseEnabled,
Integer redstoneDeathPulseRadius,
Double redstoneDeathPulseSlownessSeconds,
Integer redstoneDeathPulseSlownessAmplifier,
Boolean redstoneParticlesEnabled,
Boolean redstoneDeathPulseParticlesEnabled
```

Add `tiers.redstone` defaults and JSON read/write/canonicalization with the validation ranges from the spec.

- [ ] **Step 3.4: Run tests to verify pass**

Run the command from Step 3.2 again.

Expected: pass.

- [ ] **Step 3.5: Commit config**

Run:

```powershell
git add src/common/java/dev/charles/multigolem/config/TierStats.java src/common/java/dev/charles/multigolem/config/MultiGolemConfig.java src/test/java/dev/charles/multigolem/config/MultiGolemConfigTest.java src/test/java/dev/charles/multigolem/config/MultiGolemConfigV3Test.java
git commit -m "feat: configure redstone golem defaults"
```

Expected: commit succeeds.

### Task 4: Persisted Redstone Ability State

**Files:**
- Modify: `src/common/java/dev/charles/multigolem/attachment/GolemAbilityState.java`
- Modify: `src/test/java/dev/charles/multigolem/attachment/GolemAbilityStateTest.java`

- [ ] **Step 4.1: Write failing state tests**

Add tests:

```java
GolemAbilityState state = GolemAbilityState.fresh()
    .withRedstoneOvercharge(1200L)
    .withRedstoneCooldown(2400L)
    .withRedstoneWasBelowThreshold(true);
assertEquals(1200L, state.redstoneOverchargeActiveUntilGameTime());
assertEquals(2400L, state.redstoneOverchargeCooldownUntilGameTime());
assertTrue(state.redstoneWasBelowThreshold());
```

Add a codec test parsing old JSON with only Diamond fields and asserting Redstone fields default to inactive and `false`.

- [ ] **Step 4.2: Run tests to verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.attachment.GolemAbilityStateTest
```

Expected: fail because Redstone state accessors do not exist.

- [ ] **Step 4.3: Extend state record**

Add fields and codec optional defaults:

```java
long redstoneOverchargeActiveUntilGameTime,
long redstoneOverchargeCooldownUntilGameTime,
boolean redstoneWasBelowThreshold
```

Add helper methods `redstoneOverchargeActive(long now)`, `redstoneCooldownReady(long now)`, `withRedstoneOvercharge(long until)`, `withRedstoneCooldown(long until)`, and `withRedstoneWasBelowThreshold(boolean below)`.

- [ ] **Step 4.4: Run tests to verify pass**

Run Step 4.2 again.

Expected: pass.

- [ ] **Step 4.5: Commit state**

Run:

```powershell
git add src/common/java/dev/charles/multigolem/attachment/GolemAbilityState.java src/test/java/dev/charles/multigolem/attachment/GolemAbilityStateTest.java
git commit -m "feat: persist redstone ability state"
```

Expected: commit succeeds.

### Task 5: Redstone Ability Behavior

**Files:**
- Create: `src/common/java/dev/charles/multigolem/ability/RedstoneAbility.java`
- Create: `src/test/java/dev/charles/multigolem/ability/RedstoneAbilityTest.java`
- Modify: `src/common/java/dev/charles/multigolem/attribute/VariantAttributes.java`
- Modify: `src/test/java/dev/charles/multigolem/attribute/VariantAttributesTest.java`

- [ ] **Step 5.1: Write failing ability tests**

Cover pure behavior:

```java
assertTrue(RedstoneAbility.shouldTriggerOvercharge(25.0F, 100.0F, 0.25, false, true));
assertFalse(RedstoneAbility.shouldTriggerOvercharge(25.1F, 100.0F, 0.25, false, true));
assertFalse(RedstoneAbility.shouldTriggerOvercharge(10.0F, 100.0F, 0.25, true, true));
assertEquals(19.5, RedstoneAbility.overchargedAttackDamage(13.0, 1.5), 0.0001);
assertEquals(1, RedstoneAbility.resistanceEffectAmplifier(MultiGolemConfig.defaults().tier(GolemVariant.REDSTONE)));
assertEquals(9, RedstoneAbility.deathPulseSlownessAmplifier(MultiGolemConfig.defaults().tier(GolemVariant.REDSTONE)));
```

Add target-filter tests that a death pulse affects a Zombie Golem and hostile mob but skips villagers, wandering traders, players, Iron Golems, non-zombie MultiGolems, Redstone Golems, and creepers by default.

Add a `VariantAttributesTest` assertion that Redstone never uses `SPEED_MODIFIER_ID` and only Gold receives a movement-speed delta.

- [ ] **Step 5.2: Run tests to verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.ability.RedstoneAbilityTest --tests dev.charles.multigolem.attribute.VariantAttributesTest
```

Expected: fail because `RedstoneAbility` and Redstone speed assertions do not exist.

- [ ] **Step 5.3: Implement pure behavior**

Create `RedstoneAbility` with pure methods for threshold checks, active/cooldown timing, attack calculation, resistance duration ticks, pulse duration ticks, and target filtering. Keep world mutation methods thin wrappers around these pure methods.

Use `TargetFilter.fromIgnoredList(stats.ignoredTargetTypes())` and current Zombie faction helpers so Redstone death pulse treats Zombie Golems as hostile and non-zombie defenders as friendly.

Evaluate vanilla sound candidates for overcharge start/end cues, starting with `SoundEvents.COMPARATOR_CLICK` and `SoundEvents.REDSTONE_TORCH_BURNOUT` if those names exist in the current mappings. Pick the cleanest available sound pair, or document sound deferral in the implementation summary if the mapping or gameplay fit is not clean.

- [ ] **Step 5.4: Guard movement speed**

Keep `VariantAttributes.apply(...)` speed logic equivalent to:

```java
double speedDelta = (variant == GolemVariant.GOLD)
    ? (MultiGolem.config().tier(GolemVariant.GOLD).goldSpeedMultiplier() - 1.0)
    : 0.0;
```

Add the test from Step 5.1 so a future Redstone speed field cannot slip in unnoticed.

- [ ] **Step 5.5: Run tests to verify pass**

Run Step 5.2 again.

Expected: pass.

- [ ] **Step 5.6: Commit ability behavior**

Run:

```powershell
git add src/common/java/dev/charles/multigolem/ability/RedstoneAbility.java src/common/java/dev/charles/multigolem/attribute/VariantAttributes.java src/test/java/dev/charles/multigolem/ability/RedstoneAbilityTest.java src/test/java/dev/charles/multigolem/attribute/VariantAttributesTest.java
git commit -m "feat: add redstone overcharge rules"
```

Expected: commit succeeds.

### Task 6: Runtime Hooks For Overcharge And Death Pulse

**Files:**
- Modify: hook files selected by Task 1
- Modify: loader mixin resources if a new common mixin is selected
- Modify: `src/fabric/java/dev/charles/multigolem/fabric/ability/FabricAbilityEvents.java`
- Modify: `src/neoforge/java/dev/charles/multigolem/neoforge/ability/NeoForgeAbilityEvents.java`
- Modify loader source tests

- [ ] **Step 6.1: Write failing source tests for parity**

Update Fabric and NeoForge source tests to assert each loader calls:

```java
RedstoneAbility.onTick(level);
```

and wires the selected damage/death hook or selected common mixin resource. For NeoForge, assert no Fabric imports. For Fabric, assert no NeoForge imports.

- [ ] **Step 6.2: Run source tests to verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.ability.FabricAbilityEventsSourceTest --tests dev.charles.multigolem.neoforge.NeoForgeEventsSourceTest
```

Expected: fail because Redstone hooks are not registered.

- [ ] **Step 6.3: Wire tick and selected hooks**

Add `RedstoneAbility.onTick(level)` to Fabric `ServerTickEvents.START_LEVEL_TICK` registration and NeoForge `LevelTickEvent.Pre` handling. Wire threshold/death hooks exactly as selected by the spike. The runtime hook must:

```java
if (GolemVariantAttachment.get(golem) != GolemVariant.REDSTONE) return;
RedstoneAbility.updateOvercharge(level, golem);
```

Death hook must call pulse emission once per death event and must not run on client levels.

- [ ] **Step 6.4: Run focused verification**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.ability.RedstoneAbilityTest --tests dev.charles.multigolem.ability.FabricAbilityEventsSourceTest --tests dev.charles.multigolem.neoforge.NeoForgeEventsSourceTest
.\gradlew.bat --quiet checkCommonSourceSetsLoaderNeutral
```

Expected: pass.

- [ ] **Step 6.5: Commit hooks**

Run:

```powershell
git add src/fabric/java/dev/charles/multigolem/fabric/ability/FabricAbilityEvents.java src/neoforge/java/dev/charles/multigolem/neoforge/ability/NeoForgeAbilityEvents.java src/common/java/dev/charles/multigolem/mixin src/fabric/resources src/neoforge/resources src/test/java/dev/charles/multigolem/ability src/test/java/dev/charles/multigolem/neoforge
git commit -m "feat: wire redstone ability hooks"
```

Expected: commit succeeds. Stage only files changed by the selected hook path.

### Task 7: Spawn Eggs, Assets, Status, Customizations, And Golempedia

**Files:**
- Modify spawn egg, asset, lang, status/customization, and Golempedia files discovered by Task 1
- Modify matching tests

- [ ] **Step 7.1: Write failing surface tests**

Add assertions that:

```java
ItemStack egg = SpawnEggStacks.create(GolemVariant.REDSTONE);
assertTrue(egg.is(Items.IRON_GOLEM_SPAWN_EGG));
assertEquals(Optional.of(GolemVariant.REDSTONE), SpawnEggStacks.variantFrom(egg));
```

Add Golempedia/status/customization tests asserting Redstone appears with Redstone Block, Redstone Dust, 90 HP, 13 attack, overcharge below 25%, no speed bonus, and Slowness X death pulse.

- [ ] **Step 7.2: Run tests to verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.SpawnEggStacksTest --tests dev.charles.multigolem.golempedia.* --tests dev.charles.multigolem.customizations.* --tests dev.charles.multigolem.status.*
```

Expected: fail for missing Redstone surfaces.

- [ ] **Step 7.3: Implement surfaces**

Add Redstone lang/model/texture entries using existing generated-asset conventions. If texture generation owns variant textures, update the generator and generator tests instead of hand-editing generated PNGs.

Add Redstone to Golempedia and server customization summaries. Keep payload field order deterministic and mirrored across Fabric and NeoForge transports because payload records are common.

- [ ] **Step 7.4: Run tests to verify pass**

Run Step 7.2 again.

Expected: pass.

- [ ] **Step 7.5: Commit surfaces**

Run:

```powershell
git add src/common/resources src/common/java src/commonClient/java src/test/java scripts
git commit -m "feat: expose redstone golem surfaces"
```

Expected: commit succeeds. Stage only changed Redstone files.

### Task 8: Docs, Playtest Rows, And Internal Changelog

**Files:**
- Modify: `README.md`
- Modify: `docs/playtest-checklist.md`
- Modify: `docs/playtest.html`
- Modify: `docs/modrinth-listing.md`
- Modify: `docs/curseforge-listing.md`
- Modify: `INTERNAL_CHANGELOG.md`

- [ ] **Step 8.1: Update docs**

Document:

```markdown
| Redstone Block | Redstone Golem |
| Redstone | 90 | 13.0 |
| Redstone | Lower-strength emergency-control defender; overcharges below 25% health for attack/resistance without speed, then releases a Slowness X death pulse |
```

Do not update public `CHANGELOG.md` until Tyler approves release wording.

- [ ] **Step 8.2: Add manual verification rows**

Add Fabric and NeoForge rows for creation, Redstone Dust healing, create/heal denials, marked egg, marked spawner, overcharge below 25%, attack/resistance boost, no speed boost, cooldown, death pulse Slowness X, friendly filtering, Zombie Golem fight behavior, and save/load of active/cooldown state.

- [ ] **Step 8.3: Run docs checks**

Run:

```powershell
.\gradlew.bat --quiet checkReleaseDocs checkChangelog
git diff --check
```

Expected: docs gates and whitespace pass. `CHANGELOG.md` remains unchanged unless Tyler approved public wording.

- [ ] **Step 8.4: Commit docs**

Run:

```powershell
git add README.md docs/playtest-checklist.md docs/playtest.html docs/modrinth-listing.md docs/curseforge-listing.md INTERNAL_CHANGELOG.md
git commit -m "docs: document redstone golems"
```

Expected: commit succeeds.

### Task 9: Final Verification Without Release

**Files:**
- Verify all changed files.

- [ ] **Step 9.1: Run focused automated verification**

Run:

```powershell
git rev-parse --show-toplevel
.\gradlew.bat --quiet test --tests dev.charles.multigolem.GolemVariantTest --tests dev.charles.multigolem.catalog.GolemVariantCatalogTest --tests dev.charles.multigolem.config.MultiGolemConfigTest --tests dev.charles.multigolem.config.MultiGolemConfigV3Test --tests dev.charles.multigolem.spawn.VillageSpawnWeightsTest --tests dev.charles.multigolem.permissions.MultiGolemPermissionsTest --tests dev.charles.multigolem.attachment.GolemAbilityStateTest --tests dev.charles.multigolem.ability.RedstoneAbilityTest --tests dev.charles.multigolem.attribute.VariantAttributesTest --tests dev.charles.multigolem.spawn.SpawnEggStacksTest --tests dev.charles.multigolem.golempedia.* --tests dev.charles.multigolem.customizations.* --tests dev.charles.multigolem.status.* --tests dev.charles.multigolem.neoforge.NeoForgeEventsSourceTest
.\gradlew.bat --quiet checkCommonSourceSetsLoaderNeutral checkReleaseDocs checkChangelog
```

Expected: all pass.

- [ ] **Step 9.2: Run durable build gate**

Run:

```powershell
.\gradlew.bat --quiet :fabric:test :fabric:build :neoforge:test :neoforge:build check
```

Expected: all pass before implementation is called complete.

- [ ] **Step 9.3: Scan for scope drift and placeholders**

Run:

```powershell
rg -n "Lapis|Skeleton|Copper Golem variants|T[B]D|T[O]DO|implement\s+later|fill\s+in\s+details|Similar\s+to\s+Task|appropriate\s+error\s+handling" src README.md docs CHANGELOG.md INTERNAL_CHANGELOG.md
rg -n "redstone|Redstone|overcharge|Slowness X|multigolem.create.redstone|multigolem.heal.redstone" src README.md docs INTERNAL_CHANGELOG.md
```

Expected: first command shows only explicit out-of-scope references in planning docs and no placeholder failures. Second command shows Redstone coverage in code, tests, docs, and internal changelog.

- [ ] **Step 9.4: Request implementation review**

Use Revue `implementation-review` with explicit files changed and the verification commands above. Inspect child packet reviews, synthesis/replacement reviews, closeout evidence, and unresolved counts before calling implementation complete.

## Plan Self-Review

Spec coverage:
- Variant identity, Redstone Block construction, Redstone Dust healing, permissions, spawn eggs, spawners, village weights, status/customizations, Golempedia, docs, and playtest rows are covered in Tasks 2, 3, 7, and 8.
- Overcharge trigger, duration, cooldown, attack/resistance values, no speed boost, persistence, and save/load are covered in Tasks 3, 4, 5, 6, and 9.
- Slowness X death overload pulse, target filtering, visual cues, and no damage/explosion/redstone power are covered in Tasks 5, 6, 8, and 9.
- Fabric and NeoForge parity are covered in Tasks 1, 6, 7, and 9.

Placeholder scan:
- No placeholder implementation steps are intentionally left in this plan.
- Conditional work is tied to explicit source-spike decisions and stop conditions.

Type consistency:
- `GolemVariant.REDSTONE`, Redstone `TierStats` accessors, `GolemAbilityState` Redstone fields, and `RedstoneAbility` are introduced before later tasks reference them.
- Commands use the current multiloader source-set paths and keep loader APIs out of common code.
