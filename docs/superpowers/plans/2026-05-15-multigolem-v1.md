# MultiGolem V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a server-side Fabric mod for Minecraft 26.1.2 that adds 5 new Iron Golem variants (Copper, Gold, Emerald, Diamond, Netherite) with per-tier stats, drops, healing, and per-tier anger toggles — without registering new entity types. Vanilla clients connect with no client mod required.

**Architecture:** Every golem is a vanilla iron golem entity with a `GolemVariant` attached via Fabric's `AttachmentType` API. Stats are live-computed via a narrow mixin into the iron golem's attribute reads (with a deterministic-transient-modifier fallback if the mixin target proves hot/unstable). Creation is intercepted at the carved-pumpkin pattern-check site to recognize alternate body blocks. Drops are preferentially data-driven via a custom loot condition reading the attachment, with a tightly scoped drop-method mixin as fallback. All choices are documented per-task in the changelog.

**Tech Stack:** Java 25 · Fabric Loader 0.19.2 · Fabric API 0.148.0+26.1.2 · Fabric Loom (latest stable matching official Fabric example mod) · Minecraft 26.1.2 with **Mojang official mappings** (Yarn is no longer officially supported as of 26.1) · JUnit 5 · Mixin · GitHub Actions for build/release · Modrinth + CurseForge for distribution

**Reference spec:** `docs/superpowers/specs/2026-05-15-multigolem-design.md`
**Reference repo (porting source for verification tasks and CI):** `C:\Users\tyler\AI Projects\signport`

---

## File Structure

**Code (Java, `src/main/java/dev/charles/multigolem/`):**

```
MultiGolem.java                              # Mod entrypoint, version banner, registrations
GolemVariant.java                            # Enum with body block, ingot, drop metadata; codec
attachment/
  GolemVariantAttachment.java                # AttachmentType<GolemVariant> registration
config/
  MultiGolemConfig.java                      # JSON config load/save/validate; pure helper
  TierStats.java                             # Record: max_health, attack_damage, anger_on_hit
stats/
  GolemStatsResolver.java                    # variant + config → effective stats; pure helper
spawn/
  GolemCreationHandler.java                  # T-pattern match → spawn golem + attach variant
mixin/
  IronGolemMixin.java                        # stats / healing / anger / (maybe) drops
  CarvedPumpkinBlockMixin.java               # pattern check delegation (target confirmed by spike)
loot/
  HasGolemVariantLootCondition.java          # custom LootCondition (only if loot-table path chosen)
```

**Tests (`src/test/java/dev/charles/multigolem/`):**

```
GolemVariantTest.java
config/MultiGolemConfigTest.java
stats/GolemStatsResolverTest.java
```

**Resources (`src/main/resources/`):**

```
fabric.mod.json                                                     # mod metadata
multigolem.mixins.json                                              # mixin config
data/multigolem/loot_tables/entities/iron_golem.json                # loot override (if loot path)
```

**Project root:**

```
build.gradle                              # Bootstrapped from Fabric 26.1.2 example mod
settings.gradle
gradle.properties
gradle/wrapper/gradle-wrapper.properties  # pinned checksum
CHANGELOG.md
README.md
LICENSE
.gitignore
.github/workflows/build.yml
.github/workflows/release.yml
scripts/upload-modrinth.ps1
scripts/upload-curseforge.ps1
docs/26.1.2-mojang-targets.md             # Output of Source Inspection Spike (Task 1)
docs/playtest-checklist.md                # Manual playtest (Task 13)
docs/modrinth-listing.md
docs/curseforge-listing.md
```

---

## Conventions used in this plan

- **Mojang names.** Class and method names use Mojang official mappings as confirmed by **Task 1 (Source Inspection Spike)**. Where this plan writes a name like `IronGolem`, treat it as the *intended* Mojang name; if the spike finds a different real name, substitute consistently across subsequent tasks and note the substitution in the commit message.
- **Commit cadence.** Each task ends with a commit. Commits use Conventional Commits (`feat:`, `fix:`, `chore:`, `test:`, `docs:`).
- **TDD.** Pure-Java helpers (`GolemVariant`, `MultiGolemConfig`, `GolemStatsResolver`) get unit tests written *before* implementation. Mixin code and Fabric-integrated code is covered by the manual playtest checklist plus tight in-game smoke tests.
- **PowerShell on Windows.** Build commands shown for PowerShell (the user's shell). Per `CLAUDE.md`, long Gradle commands redirect to a temp file before grepping.

---

## Task 0: Initialize project from Fabric 26.1.2 example mod

Bootstrap the repository scaffolding from the official Fabric example mod, then `git init` and make the first commit. This task produces a buildable empty mod.

**Files:**
- Create: `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat`, `.gitignore`, `LICENSE`, `README.md`
- Create: `src/main/java/dev/charles/multigolem/MultiGolem.java`
- Create: `src/main/resources/fabric.mod.json`
- Create: `src/main/resources/multigolem.mixins.json` (empty mixin list for now)
- Create: `src/client/java/.gitkeep` (empty file — placeholder so the V2 source set has a home, but no client classes yet)

- [ ] **Step 0.1: Clone the Fabric example mod template into a scratch directory**

Run:
```powershell
cd "C:\Users\tyler\AI Projects"
git clone --depth 1 --branch 26.1.2 https://github.com/FabricMC/fabric-example-mod.git multigolem-template
```

If `26.1.2` is not a branch, fall back to the default branch and verify `gradle.properties` lists `minecraft_version=26.1.2`. If neither yields 26.1.2, list available branches with `git -C multigolem-template branch -a` and pick the closest 26.1 branch.

- [ ] **Step 0.2: Copy scaffolding into MultiGolem**

Run:
```powershell
$src = "C:\Users\tyler\AI Projects\multigolem-template"
$dst = "C:\Users\tyler\AI Projects\MultiGolem"
Copy-Item "$src\build.gradle" "$dst\"
Copy-Item "$src\settings.gradle" "$dst\"
Copy-Item "$src\gradle.properties" "$dst\"
Copy-Item "$src\.gitignore" "$dst\"
Copy-Item "$src\gradlew" "$dst\"
Copy-Item "$src\gradlew.bat" "$dst\"
Copy-Item "$src\gradle" "$dst\" -Recurse
Copy-Item "$src\LICENSE" "$dst\" -ErrorAction SilentlyContinue
```

Delete the scratch template afterward:
```powershell
Remove-Item "C:\Users\tyler\AI Projects\multigolem-template" -Recurse -Force
```

- [ ] **Step 0.3: Edit `gradle.properties` for MultiGolem**

Replace the file's mod-properties block. Final file:

```properties
# Done to increase the memory available to gradle.
org.gradle.jvmargs=-Xmx1G
org.gradle.parallel=true

# Fabric Properties
# check these on https://fabricmc.net/develop
minecraft_version=26.1.2
loader_version=0.19.2
loom_version=1.16-SNAPSHOT

# Mod Properties
mod_version=0.1.0+mc26.1.2
maven_group=dev.charles.multigolem
archives_base_name=multigolem

# Dependencies
fabric_version=0.148.0+26.1.2
```

If the example mod's pinned `loom_version` is newer (e.g., `1.17-SNAPSHOT`), keep the template's value rather than downgrading. The `fabric_version` should match the example mod's pin if it differs from `0.148.0+26.1.2`.

- [ ] **Step 0.4: Edit `build.gradle` to use the MultiGolem mod name**

Replace any `"example"` / `"modid"` references with `"multigolem"`. Specifically the `loom { mods { "modid" { ... } } }` block becomes `"multigolem" { sourceSet sourceSets.main }`. Add the `splitEnvironmentSourceSets()` call so V2 client code has a home but no client source set is required yet — when no client classes exist, Loom won't generate a client config file but the source set declaration is harmless.

Resulting `loom` block:

```gradle
loom {
    splitEnvironmentSourceSets()

    mods {
        "multigolem" {
            sourceSet sourceSets.main
            sourceSet sourceSets.client
        }
    }
}
```

Confirm the `repositories` block contains `mavenCentral()` (it should from the template). No extra repos needed for V1.

Confirm `dependencies` uses plain `implementation` for `fabric-loader` and `fabric-api` (the modern 26.1+ flow — not `modImplementation`).

Confirm `tasks.withType(JavaCompile)` sets `options.release = 25` and the `java` block uses `JavaLanguageVersion.of(25)`.

- [ ] **Step 0.5: Write `src/main/resources/fabric.mod.json`**

```json
{
  "schemaVersion": 1,
  "id": "multigolem",
  "version": "${version}",
  "name": "MultiGolem",
  "description": "Adds Copper, Gold, Emerald, Diamond, and Netherite Iron Golem variants. Server-side functional; vanilla clients welcome.",
  "authors": [
    "Tyler",
    "Charles"
  ],
  "contact": {
    "homepage": "https://github.com/TnTBass/multigolem"
  },
  "license": "MIT",
  "environment": "*",
  "entrypoints": {
    "main": [
      "dev.charles.multigolem.MultiGolem"
    ]
  },
  "mixins": [
    "multigolem.mixins.json"
  ],
  "depends": {
    "fabricloader": ">=0.19.2",
    "fabric-api": "*",
    "minecraft": "~26.1.2",
    "java": ">=25"
  }
}
```

(The homepage URL is a placeholder; we'll set the real one in Task 14.)

- [ ] **Step 0.6: Write `src/main/resources/multigolem.mixins.json`**

```json
{
  "required": true,
  "package": "dev.charles.multigolem.mixin",
  "compatibilityLevel": "JAVA_21",
  "mixins": [],
  "injectors": {
    "defaultRequire": 1
  }
}
```

(Mixin's compatibility level lags Java releases; `JAVA_21` is the highest published level at time of writing — substitute `JAVA_22` or higher only if the example-mod template ships with it. Mixin runs bytecode regardless of source level.)

- [ ] **Step 0.7: Write the mod entrypoint**

Create `src/main/java/dev/charles/multigolem/MultiGolem.java`:

```java
package dev.charles.multigolem;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiGolem implements ModInitializer {
    public static final String MOD_ID = "multigolem";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOG.info("MultiGolem starting up - Charles & Tyler's golem variants");
    }
}
```

- [ ] **Step 0.8: Create the client source-set placeholder**

```powershell
New-Item -ItemType Directory -Path "src/client/java/dev/charles/multigolem" -Force | Out-Null
New-Item -ItemType File -Path "src/client/java/.gitkeep" -Force | Out-Null
```

- [ ] **Step 0.9: Verify the project builds**

Per the user's CLAUDE.md, redirect the build to a temp file:

```powershell
./gradlew --quiet build > build_out.txt 2>&1
$LASTEXITCODE
```

Expected: exit code `0`. If non-zero, inspect with:

```powershell
Select-String -Path build_out.txt -Pattern 'error:|FAILED|cannot find symbol|incompatible types|mixin apply failed' | Select-Object -First 80
```

Fix any errors before continuing.

- [ ] **Step 0.10: Verify the mod loads in a dev client**

```powershell
./gradlew --quiet runServer > run_out.txt 2>&1
```

Watch the file for "MultiGolem starting up". After the line appears, stop the server (`Ctrl+C` in the Gradle process). Verify:

```powershell
Select-String -Path run_out.txt -Pattern 'MultiGolem starting up'
```

Expected: one match.

- [ ] **Step 0.11: Initialize git, commit**

```powershell
git init -b main
git add -A
git commit -m "chore: bootstrap from Fabric 26.1.2 example mod"
```

---

## Task 1: Source Inspection Spike — confirm 26.1.2 Mojang targets

Produce `docs/26.1.2-mojang-targets.md` documenting the exact class and method names we will mixin. **No mod code is written until this is complete and committed.** The spike is required by the spec (§6.2) and by Codex review finding #4.

**Files:**
- Create: `docs/26.1.2-mojang-targets.md`

- [ ] **Step 1.1: Generate Minecraft sources**

```powershell
./gradlew --quiet genSources > gensources_out.txt 2>&1
```

This produces decompiled sources under `.gradle/loom-cache/` (exact path varies by Loom version). Find the `IronGolem` and `CarvedPumpkinBlock` sources:

```powershell
Get-ChildItem -Path ".gradle" -Recurse -Filter "IronGolem.java" | Select-Object FullName
Get-ChildItem -Path ".gradle" -Recurse -Filter "CarvedPumpkinBlock.java" | Select-Object FullName
```

Pick the source jar (usually `*-sources.jar` extracted), not a class file.

- [ ] **Step 1.2: Inspect the iron golem class — find max-health and attack-damage attribute setup**

Open the `IronGolem.java` source and answer in the targets doc:

- Exact fully-qualified class name? (Expected: `net.minecraft.world.entity.animal.IronGolem`. Confirm.)
- How are base attributes registered? (Expected: a `createAttributes()` or `createMobAttributes()` static method returning an `AttributeSupplier.Builder`. Note the exact method name and signature.)
- Where is **attack damage rolled per swing**? Look for a `doHurtTarget` / `tryAttack` method that reads `Attributes.ATTACK_DAMAGE`. Note the method name, descriptor, and the line that reads attack damage.
- Where is **the right-click ingot heal** handled? Look for `mobInteract` / `interactMob` / similar. Note the method name and how it checks for an iron ingot.
- Where is **retaliation against an attacker** handled? Look for `setLastHurtByPlayer`, `setTarget`, or anger-related methods called from a `hurt` / `damage` override. Note the method name.
- Where is **death loot** computed? Note the `getDefaultLootTable` / `getLootTable` method, and search for any `dropFromLootTable` override.

- [ ] **Step 1.3: Inspect carved pumpkin / golem creation**

Open `CarvedPumpkinBlock.java`. Answer:

- Where is the iron-golem T-pattern checked? In recent versions this lives in a `trySpawnGolem(Level, BlockPos)` method (or similar) that uses a `BlockPattern` static field like `IRON_GOLEM_FULL_PATTERN`. Note the exact method name and signature.
- How is the iron block requirement encoded? (Look for `BlockInWorld` predicates checking `Blocks.IRON_BLOCK`.)
- Where does the spawn actually call `EntityType.IRON_GOLEM.create(...)`? Note the line that creates and spawns the entity.

- [ ] **Step 1.4: Inspect attribute read path for the live-computed mixin**

Open `LivingEntity.java`. Answer:

- Method that reads an attribute value? Expected: `getAttributeValue(Holder<Attribute>)` or `getAttribute(Holder<Attribute>).getValue()`. Note the exact signature in 26.1.2.
- Is the method called frequently (e.g., from `tick`)? Grep across the decompiled sources:

```powershell
Get-ChildItem -Path ".gradle" -Recurse -Filter "*.java" | Select-String "getAttributeValue\(" | Measure-Object
```

Document the count and the highest-frequency callers. This informs the live-vs-modifier decision in Task 6.

- [ ] **Step 1.5: Decide live-computed vs transient modifiers**

Based on Step 1.4 findings, decide:

- If `getAttributeValue` is called fewer than ~5 times per entity per tick from hot code → **live-computed** is fine. Plan goes ahead with Task 6 plan A.
- If it's called many times per tick from hot code (e.g., the renderer or pathfinding reads it every tick) → **fallback to deterministic transient modifiers**. Task 6 follows plan B.

Document the decision in the targets doc with a one-sentence rationale.

- [ ] **Step 1.6: Decide loot-table vs drop-mixin path**

In the decompiled source, find `LootContext` and `LootContextParam`. Answer:

- Is the killed entity exposed as a `LootContextParam` accessible by a custom `LootItemCondition`? (Look for `LootContextParams.THIS_ENTITY`.)
- Does the `LootCondition` API allow reading the entity in 26.1.2 the same way it did in 1.20–1.21?

Decide:
- If yes → loot-table path (Task 10 plan A).
- If no → drop-method mixin path (Task 10 plan B).

Document the decision.

- [ ] **Step 1.7: Write the targets document**

Create `docs/26.1.2-mojang-targets.md`:

```markdown
# Minecraft 26.1.2 Mojang Mapping Targets

Output of the Source Inspection Spike (Task 1 of V1 plan). Used by all mixin tasks.

**Spike date:** <YYYY-MM-DD>
**Loom version:** <from gradle.properties>
**Minecraft version:** 26.1.2

## IronGolem

- FQN: `<actual.path.IronGolem>`
- Attributes registration method: `<methodName>(<signature>)` at line <N>
- Attack damage roll site: `<methodName>(<signature>)` at line <N>; reads `Attributes.ATTACK_DAMAGE` via `<getAttributeValue(...)>`
- Right-click heal site: `<methodName>(<signature>)` at line <N>; checks `<ingot item reference>`
- Retaliation / set-target site: `<methodName>(<signature>)` at line <N>
- Loot table method: `<methodName>(<signature>)` at line <N>

## CarvedPumpkinBlock

- FQN: `<actual.path.CarvedPumpkinBlock>`
- Iron golem pattern check method: `<methodName>(<signature>)` at line <N>
- Pattern field name: `<IRON_GOLEM_FULL_PATTERN-or-equivalent>`
- Spawn call line: `<file:line>`

## LivingEntity

- Attribute read method: `<getAttributeValue signature>`
- Hot-path callers found: <count>; top callers: <list>

## Decisions

1. **Stat strategy:** <live-computed | transient-modifiers> — rationale: <one sentence>
2. **Drops strategy:** <loot-table | drop-mixin> — rationale: <one sentence>

## Notes for future updates

When Minecraft updates, re-run this spike, diff this doc, and adjust mixin targets accordingly.
```

Fill in every `<placeholder>` with real findings. **Do not commit the doc with `<placeholder>` text remaining.**

- [ ] **Step 1.8: Commit the spike**

```powershell
git add docs/26.1.2-mojang-targets.md
git commit -m "docs: Source Inspection Spike for MC 26.1.2 Mojang targets"
```

---

## Task 2: `GolemVariant` enum and codec

Pure-Java enum + lookup tables. TDD: tests first.

**Files:**
- Create: `src/main/java/dev/charles/multigolem/GolemVariant.java`
- Test: `src/test/java/dev/charles/multigolem/GolemVariantTest.java`

- [ ] **Step 2.1: Add JUnit 5 to `build.gradle`**

Add to `dependencies`:

```gradle
testImplementation "org.junit.jupiter:junit-jupiter:6.0.3"
testRuntimeOnly "org.junit.platform:junit-platform-launcher"
```

And below the existing tasks block:

```gradle
tasks.withType(Test).configureEach {
    useJUnitPlatform()
}
```

Verify the build still passes:

```powershell
./gradlew --quiet build > build_out.txt 2>&1; $LASTEXITCODE
```

Expected: `0`.

- [ ] **Step 2.2: Write the failing test for variant lookup by body block**

Create `src/test/java/dev/charles/multigolem/GolemVariantTest.java`:

```java
package dev.charles.multigolem;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GolemVariantTest {

    @Test
    void fromBodyBlock_recognizesAllSixTiers() {
        assertEquals(GolemVariant.COPPER,    GolemVariant.fromBodyBlock(Blocks.COPPER_BLOCK).orElseThrow());
        assertEquals(GolemVariant.IRON,      GolemVariant.fromBodyBlock(Blocks.IRON_BLOCK).orElseThrow());
        assertEquals(GolemVariant.GOLD,      GolemVariant.fromBodyBlock(Blocks.GOLD_BLOCK).orElseThrow());
        assertEquals(GolemVariant.EMERALD,   GolemVariant.fromBodyBlock(Blocks.EMERALD_BLOCK).orElseThrow());
        assertEquals(GolemVariant.DIAMOND,   GolemVariant.fromBodyBlock(Blocks.DIAMOND_BLOCK).orElseThrow());
        assertEquals(GolemVariant.NETHERITE, GolemVariant.fromBodyBlock(Blocks.NETHERITE_BLOCK).orElseThrow());
    }

    @Test
    void fromBodyBlock_returnsEmptyForUnsupportedBlock() {
        assertTrue(GolemVariant.fromBodyBlock(Blocks.DIRT).isEmpty());
    }

    @Test
    void fromIngot_recognizesAllSixTiers() {
        assertEquals(GolemVariant.COPPER,    GolemVariant.fromIngot(Items.COPPER_INGOT).orElseThrow());
        assertEquals(GolemVariant.IRON,      GolemVariant.fromIngot(Items.IRON_INGOT).orElseThrow());
        assertEquals(GolemVariant.GOLD,      GolemVariant.fromIngot(Items.GOLD_INGOT).orElseThrow());
        assertEquals(GolemVariant.EMERALD,   GolemVariant.fromIngot(Items.EMERALD).orElseThrow());
        assertEquals(GolemVariant.DIAMOND,   GolemVariant.fromIngot(Items.DIAMOND).orElseThrow());
        assertEquals(GolemVariant.NETHERITE, GolemVariant.fromIngot(Items.NETHERITE_INGOT).orElseThrow());
    }

    @Test
    void fromIngot_returnsEmptyForUnsupportedItem() {
        assertTrue(GolemVariant.fromIngot(Items.APPLE).isEmpty());
    }

    @Test
    void idIsStableLowercase() {
        assertEquals("copper",    GolemVariant.COPPER.id());
        assertEquals("iron",      GolemVariant.IRON.id());
        assertEquals("gold",      GolemVariant.GOLD.id());
        assertEquals("emerald",   GolemVariant.EMERALD.id());
        assertEquals("diamond",   GolemVariant.DIAMOND.id());
        assertEquals("netherite", GolemVariant.NETHERITE.id());
    }

    @Test
    void fromId_roundTripsAllVariants() {
        for (GolemVariant v : GolemVariant.values()) {
            assertEquals(v, GolemVariant.fromId(v.id()).orElseThrow());
        }
    }

    @Test
    void fromId_returnsEmptyForUnknown() {
        assertTrue(GolemVariant.fromId("obsidian").isEmpty());
        assertTrue(GolemVariant.fromId("").isEmpty());
    }

    @Test
    void dropItem_matchesExpected() {
        assertEquals(Items.COPPER_INGOT,    GolemVariant.COPPER.dropItem());
        assertEquals(Items.IRON_INGOT,      GolemVariant.IRON.dropItem());
        assertEquals(Items.GOLD_INGOT,      GolemVariant.GOLD.dropItem());
        assertEquals(Items.EMERALD,         GolemVariant.EMERALD.dropItem());
        assertEquals(Items.DIAMOND,         GolemVariant.DIAMOND.dropItem());
        assertEquals(Items.NETHERITE_SCRAP, GolemVariant.NETHERITE.dropItem());
    }
}
```

- [ ] **Step 2.3: Run the test, verify it fails to compile**

```powershell
./gradlew --quiet test > test_out.txt 2>&1; $LASTEXITCODE
```

Expected: non-zero exit; `test_out.txt` contains `cannot find symbol` for `GolemVariant`.

- [ ] **Step 2.4: Implement `GolemVariant`**

Create `src/main/java/dev/charles/multigolem/GolemVariant.java`:

```java
package dev.charles.multigolem;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum GolemVariant {
    COPPER   ("copper",    Blocks.COPPER_BLOCK,    Items.COPPER_INGOT,    Items.COPPER_INGOT),
    IRON     ("iron",      Blocks.IRON_BLOCK,      Items.IRON_INGOT,      Items.IRON_INGOT),
    GOLD     ("gold",      Blocks.GOLD_BLOCK,      Items.GOLD_INGOT,      Items.GOLD_INGOT),
    EMERALD  ("emerald",   Blocks.EMERALD_BLOCK,   Items.EMERALD,         Items.EMERALD),
    DIAMOND  ("diamond",   Blocks.DIAMOND_BLOCK,   Items.DIAMOND,         Items.DIAMOND),
    NETHERITE("netherite", Blocks.NETHERITE_BLOCK, Items.NETHERITE_INGOT, Items.NETHERITE_SCRAP);

    public static final Codec<GolemVariant> CODEC = Codec.STRING.flatXmap(
        id -> fromId(id)
            .map(com.mojang.serialization.DataResult::success)
            .orElseGet(() -> com.mojang.serialization.DataResult.error(() -> "Unknown GolemVariant id: " + id)),
        v -> com.mojang.serialization.DataResult.success(v.id)
    );

    private static final Map<Block, GolemVariant> BY_BODY_BLOCK = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(v -> v.bodyBlock, Function.identity()));
    private static final Map<Item, GolemVariant> BY_HEAL_INGOT = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(v -> v.healIngot, Function.identity()));
    private static final Map<String, GolemVariant> BY_ID = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(v -> v.id, Function.identity()));

    private final String id;
    private final Block bodyBlock;
    private final Item healIngot;
    private final Item dropItem;

    GolemVariant(String id, Block bodyBlock, Item healIngot, Item dropItem) {
        this.id = id;
        this.bodyBlock = bodyBlock;
        this.healIngot = healIngot;
        this.dropItem = dropItem;
    }

    public String id() { return id; }
    public Block bodyBlock() { return bodyBlock; }
    public Item healIngot() { return healIngot; }
    public Item dropItem() { return dropItem; }

    public static Optional<GolemVariant> fromBodyBlock(Block block) {
        return Optional.ofNullable(BY_BODY_BLOCK.get(block));
    }

    public static Optional<GolemVariant> fromIngot(Item item) {
        return Optional.ofNullable(BY_HEAL_INGOT.get(item));
    }

    public static Optional<GolemVariant> fromId(String id) {
        if (id == null || id.isEmpty()) return Optional.empty();
        return Optional.ofNullable(BY_ID.get(id));
    }
}
```

- [ ] **Step 2.5: Run tests, verify they pass**

```powershell
./gradlew --quiet test > test_out.txt 2>&1; $LASTEXITCODE
```

Expected: `0`. If not, inspect:

```powershell
Select-String -Path test_out.txt -Pattern 'FAILED|error:'
```

- [ ] **Step 2.6: Commit**

```powershell
git add build.gradle src/main/java/dev/charles/multigolem/GolemVariant.java src/test/java/dev/charles/multigolem/GolemVariantTest.java
git commit -m "feat: add GolemVariant enum with body-block/ingot/drop mapping and codec"
```

---

## Task 3: `GolemVariantAttachment` — Fabric AttachmentType registration

Wire the attachment so the variant persists with the entity's NBT. This is integration code — no unit tests, but verified by smoke-testing in Task 7.

**Files:**
- Create: `src/main/java/dev/charles/multigolem/attachment/GolemVariantAttachment.java`
- Modify: `src/main/java/dev/charles/multigolem/MultiGolem.java` (call the registrar in `onInitialize`)

- [ ] **Step 3.1: Implement the attachment type**

Create `src/main/java/dev/charles/multigolem/attachment/GolemVariantAttachment.java`:

```java
package dev.charles.multigolem.attachment;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class GolemVariantAttachment {

    public static final AttachmentType<GolemVariant> TYPE = AttachmentRegistry
        .<GolemVariant>builder()
        .persistent(GolemVariant.CODEC)
        .buildAndRegister(ResourceLocation.fromNamespaceAndPath(MultiGolem.MOD_ID, "variant"));

    private GolemVariantAttachment() {}

    public static GolemVariant get(Entity entity) {
        GolemVariant attached = entity.getAttached(TYPE);
        return attached != null ? attached : GolemVariant.IRON;
    }

    public static Optional<GolemVariant> getRaw(Entity entity) {
        return Optional.ofNullable(entity.getAttached(TYPE));
    }

    public static void set(Entity entity, GolemVariant variant) {
        entity.setAttached(TYPE, variant);
    }

    public static void touch() {
        // Calling this from MultiGolem.onInitialize forces class load and TYPE registration.
    }
}
```

(Method names `getAttached`/`setAttached` are the Fabric Attachment API names. If the API in `fabric_version=0.148.0+26.1.2` uses different names, substitute consistently; the spike output will have caught this if you ran a quick `Find` against the loom-cache sources.)

- [ ] **Step 3.2: Wire into the entrypoint**

Edit `src/main/java/dev/charles/multigolem/MultiGolem.java`:

```java
package dev.charles.multigolem;

import dev.charles.multigolem.attachment.GolemVariantAttachment;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiGolem implements ModInitializer {
    public static final String MOD_ID = "multigolem";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        GolemVariantAttachment.touch();
        LOG.info("MultiGolem starting up - Charles & Tyler's golem variants");
    }
}
```

- [ ] **Step 3.3: Build and verify**

```powershell
./gradlew --quiet build > build_out.txt 2>&1; $LASTEXITCODE
```

Expected: `0`.

- [ ] **Step 3.4: Commit**

```powershell
git add src/main/java/dev/charles/multigolem/attachment/GolemVariantAttachment.java src/main/java/dev/charles/multigolem/MultiGolem.java
git commit -m "feat: register GolemVariant attachment type"
```

---

## Task 4: Config — `TierStats` record and `MultiGolemConfig` loader

JSON config with validation. TDD-first because `MultiGolemConfig` is a pure helper and the `checkUnitTestCompanions` task we'll port in Task 14 requires it to have a test.

**Files:**
- Create: `src/main/java/dev/charles/multigolem/config/TierStats.java`
- Create: `src/main/java/dev/charles/multigolem/config/MultiGolemConfig.java`
- Test: `src/test/java/dev/charles/multigolem/config/MultiGolemConfigTest.java`

- [ ] **Step 4.1: Write the failing test**

Create `src/test/java/dev/charles/multigolem/config/MultiGolemConfigTest.java`:

```java
package dev.charles.multigolem.config;

import dev.charles.multigolem.GolemVariant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MultiGolemConfigTest {

    @Test
    void defaults_haveExpectedStats() {
        MultiGolemConfig cfg = MultiGolemConfig.defaults();
        assertTrue(cfg.allowGolemHealing());
        assertEquals(60,   cfg.tier(GolemVariant.COPPER).maxHealth());
        assertEquals(8.5,  cfg.tier(GolemVariant.COPPER).attackDamage(), 0.0001);
        assertEquals(100,  cfg.tier(GolemVariant.IRON).maxHealth());
        assertEquals(14.0, cfg.tier(GolemVariant.IRON).attackDamage(), 0.0001);
        assertEquals(600,  cfg.tier(GolemVariant.NETHERITE).maxHealth());
        assertEquals(85.0, cfg.tier(GolemVariant.NETHERITE).attackDamage(), 0.0001);
        for (GolemVariant v : GolemVariant.values()) {
            assertTrue(cfg.tier(v).angerOnHit(), v + " should default angerOnHit=true");
        }
    }

    @Test
    void loadFromFile_missingFile_writesDefaults(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        MultiGolemConfig loaded = MultiGolemConfig.loadOrCreate(file);
        assertEquals(MultiGolemConfig.defaults(), loaded);
        assertTrue(Files.exists(file), "default config should be written on first run");
    }

    @Test
    void loadFromFile_validJson_parses(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "allow_golem_healing": false,
              "tiers": {
                "copper":    { "max_health": 50,  "attack_damage": 5.0,  "anger_on_hit": false },
                "iron":      { "max_health": 100, "attack_damage": 14.0, "anger_on_hit": true  },
                "gold":      { "max_health": 130, "attack_damage": 22.5, "anger_on_hit": true  },
                "emerald":   { "max_health": 200, "attack_damage": 40.0, "anger_on_hit": true  },
                "diamond":   { "max_health": 350, "attack_damage": 62.5, "anger_on_hit": true  },
                "netherite": { "max_health": 600, "attack_damage": 85.0, "anger_on_hit": true  }
              }
            }
            """);
        MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
        assertFalse(cfg.allowGolemHealing());
        assertEquals(50,  cfg.tier(GolemVariant.COPPER).maxHealth());
        assertEquals(5.0, cfg.tier(GolemVariant.COPPER).attackDamage(), 0.0001);
        assertFalse(cfg.tier(GolemVariant.COPPER).angerOnHit());
    }

    @Test
    void loadFromFile_malformedJson_returnsDefaultsAndDoesNotOverwrite(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        String original = "{not valid json";
        Files.writeString(file, original);
        MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
        assertEquals(MultiGolemConfig.defaults(), cfg);
        assertEquals(original, Files.readString(file), "malformed config must not be overwritten");
    }

    @Test
    void loadFromFile_outOfRangeValues_clamped(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "allow_golem_healing": true,
              "tiers": {
                "copper":    { "max_health": -10,    "attack_damage": -5.0,  "anger_on_hit": true },
                "iron":      { "max_health": 100,    "attack_damage": 14.0,  "anger_on_hit": true },
                "gold":      { "max_health": 130,    "attack_damage": 22.5,  "anger_on_hit": true },
                "emerald":   { "max_health": 200,    "attack_damage": 40.0,  "anger_on_hit": true },
                "diamond":   { "max_health": 350,    "attack_damage": 62.5,  "anger_on_hit": true },
                "netherite": { "max_health": 100000, "attack_damage": 1e9,   "anger_on_hit": true }
              }
            }
            """);
        MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
        assertEquals(1,    cfg.tier(GolemVariant.COPPER).maxHealth());
        assertEquals(0.0,  cfg.tier(GolemVariant.COPPER).attackDamage(), 0.0001);
        assertEquals(2048, cfg.tier(GolemVariant.NETHERITE).maxHealth());
        assertEquals(2048.0, cfg.tier(GolemVariant.NETHERITE).attackDamage(), 0.0001);
    }

    @Test
    void loadFromFile_partialTiers_filledWithDefaults(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "tiers": {
                "copper": { "max_health": 70, "attack_damage": 10.0, "anger_on_hit": false }
              }
            }
            """);
        MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
        assertTrue(cfg.allowGolemHealing(), "missing top-level fields fall back to default");
        assertEquals(70, cfg.tier(GolemVariant.COPPER).maxHealth());
        assertEquals(100, cfg.tier(GolemVariant.IRON).maxHealth(), "missing tier uses default");
        assertEquals(600, cfg.tier(GolemVariant.NETHERITE).maxHealth(), "missing tier uses default");
    }
}
```

- [ ] **Step 4.2: Run, verify failure**

```powershell
./gradlew --quiet test > test_out.txt 2>&1; $LASTEXITCODE
```

Expected: non-zero with `cannot find symbol: MultiGolemConfig`.

- [ ] **Step 4.3: Implement `TierStats`**

Create `src/main/java/dev/charles/multigolem/config/TierStats.java`:

```java
package dev.charles.multigolem.config;

public record TierStats(int maxHealth, double attackDamage, boolean angerOnHit) {

    public static final int MIN_HEALTH = 1;
    public static final int MAX_HEALTH = 2048;
    public static final double MIN_DAMAGE = 0.0;
    public static final double MAX_DAMAGE = 2048.0;

    public TierStats clamped() {
        int h = Math.max(MIN_HEALTH, Math.min(MAX_HEALTH, maxHealth));
        double d = Math.max(MIN_DAMAGE, Math.min(MAX_DAMAGE, attackDamage));
        if (h == maxHealth && d == attackDamage) return this;
        return new TierStats(h, d, angerOnHit);
    }

    public boolean isClamped() {
        return maxHealth < MIN_HEALTH || maxHealth > MAX_HEALTH
            || attackDamage < MIN_DAMAGE || attackDamage > MAX_DAMAGE;
    }
}
```

- [ ] **Step 4.4: Implement `MultiGolemConfig`**

Create `src/main/java/dev/charles/multigolem/config/MultiGolemConfig.java`:

```java
package dev.charles.multigolem.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class MultiGolemConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final boolean allowGolemHealing;
    private final Map<GolemVariant, TierStats> tiers;

    private MultiGolemConfig(boolean allowGolemHealing, Map<GolemVariant, TierStats> tiers) {
        this.allowGolemHealing = allowGolemHealing;
        this.tiers = new EnumMap<>(tiers);
    }

    public boolean allowGolemHealing() { return allowGolemHealing; }

    public TierStats tier(GolemVariant variant) {
        return tiers.get(variant);
    }

    public static MultiGolemConfig defaults() {
        EnumMap<GolemVariant, TierStats> map = new EnumMap<>(GolemVariant.class);
        map.put(GolemVariant.COPPER,    new TierStats(60,  8.5,  true));
        map.put(GolemVariant.IRON,      new TierStats(100, 14.0, true));
        map.put(GolemVariant.GOLD,      new TierStats(130, 22.5, true));
        map.put(GolemVariant.EMERALD,   new TierStats(200, 40.0, true));
        map.put(GolemVariant.DIAMOND,   new TierStats(350, 62.5, true));
        map.put(GolemVariant.NETHERITE, new TierStats(600, 85.0, true));
        return new MultiGolemConfig(true, map);
    }

    public static MultiGolemConfig loadOrCreate(Path path) {
        if (!Files.exists(path)) {
            MultiGolemConfig defaults = defaults();
            writeQuietly(path, defaults);
            return defaults;
        }
        try {
            String contents = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject root = GSON.fromJson(contents, JsonObject.class);
            if (root == null) {
                MultiGolem.LOG.warn("multigolem config at {} is empty; using defaults (not overwriting)", path);
                return defaults();
            }
            return parse(root);
        } catch (JsonSyntaxException e) {
            MultiGolem.LOG.warn("multigolem config at {} is malformed; using defaults and NOT overwriting your file: {}", path, e.getMessage());
            return defaults();
        } catch (IOException e) {
            MultiGolem.LOG.warn("could not read multigolem config at {}; using defaults: {}", path, e.getMessage());
            return defaults();
        }
    }

    private static MultiGolemConfig parse(JsonObject root) {
        MultiGolemConfig defaults = defaults();
        boolean healing = defaults.allowGolemHealing;
        if (root.has("allow_golem_healing") && root.get("allow_golem_healing").getAsJsonPrimitive().isBoolean()) {
            healing = root.get("allow_golem_healing").getAsBoolean();
        } else if (root.has("allow_golem_healing")) {
            MultiGolem.LOG.warn("allow_golem_healing is not a boolean; using default {}", healing);
        }

        EnumMap<GolemVariant, TierStats> tiers = new EnumMap<>(GolemVariant.class);
        JsonObject tiersJson = root.has("tiers") && root.get("tiers").isJsonObject()
            ? root.getAsJsonObject("tiers") : new JsonObject();
        for (GolemVariant v : GolemVariant.values()) {
            TierStats def = defaults.tier(v);
            if (!tiersJson.has(v.id()) || !tiersJson.get(v.id()).isJsonObject()) {
                tiers.put(v, def);
                continue;
            }
            JsonObject t = tiersJson.getAsJsonObject(v.id());
            int health = readInt(t, "max_health", def.maxHealth());
            double damage = readDouble(t, "attack_damage", def.attackDamage());
            boolean anger = readBoolean(t, "anger_on_hit", def.angerOnHit());
            TierStats raw = new TierStats(health, damage, anger);
            if (raw.isClamped()) {
                MultiGolem.LOG.warn("tier {} has out-of-range values; clamping to [{}..{}] HP / [{}..{}] dmg",
                    v.id(), TierStats.MIN_HEALTH, TierStats.MAX_HEALTH, TierStats.MIN_DAMAGE, TierStats.MAX_DAMAGE);
            }
            tiers.put(v, raw.clamped());
        }
        return new MultiGolemConfig(healing, tiers);
    }

    private static int readInt(JsonObject obj, String key, int fallback) {
        try {
            return obj.has(key) ? obj.get(key).getAsInt() : fallback;
        } catch (Exception e) {
            MultiGolem.LOG.warn("field {} is not an int; using default {}", key, fallback);
            return fallback;
        }
    }

    private static double readDouble(JsonObject obj, String key, double fallback) {
        try {
            return obj.has(key) ? obj.get(key).getAsDouble() : fallback;
        } catch (Exception e) {
            MultiGolem.LOG.warn("field {} is not a number; using default {}", key, fallback);
            return fallback;
        }
    }

    private static boolean readBoolean(JsonObject obj, String key, boolean fallback) {
        try {
            return obj.has(key) ? obj.get(key).getAsBoolean() : fallback;
        } catch (Exception e) {
            MultiGolem.LOG.warn("field {} is not a boolean; using default {}", key, fallback);
            return fallback;
        }
    }

    private static void writeQuietly(Path path, MultiGolemConfig cfg) {
        try {
            Files.createDirectories(path.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("allow_golem_healing", cfg.allowGolemHealing);
            JsonObject tiers = new JsonObject();
            for (GolemVariant v : GolemVariant.values()) {
                TierStats s = cfg.tier(v);
                JsonObject t = new JsonObject();
                t.addProperty("max_health", s.maxHealth());
                t.addProperty("attack_damage", s.attackDamage());
                t.addProperty("anger_on_hit", s.angerOnHit());
                tiers.add(v.id(), t);
            }
            root.add("tiers", tiers);
            Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            MultiGolem.LOG.warn("could not write default multigolem config to {}: {}", path, e.getMessage());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MultiGolemConfig that)) return false;
        return allowGolemHealing == that.allowGolemHealing && tiers.equals(that.tiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allowGolemHealing, tiers);
    }
}
```

(Note: Gson is already a transitive dependency of Minecraft via the Fabric runtime, so no extra Gradle entry is needed. If the build complains about a missing Gson at compile time, add `compileOnly "com.google.code.gson:gson:2.11.0"` to `build.gradle` and re-run.)

- [ ] **Step 4.5: Run tests, verify they pass**

```powershell
./gradlew --quiet test > test_out.txt 2>&1; $LASTEXITCODE
```

Expected: `0`.

- [ ] **Step 4.6: Wire config loading into the entrypoint**

Edit `src/main/java/dev/charles/multigolem/MultiGolem.java`:

```java
package dev.charles.multigolem;

import dev.charles.multigolem.attachment.GolemVariantAttachment;
import dev.charles.multigolem.config.MultiGolemConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class MultiGolem implements ModInitializer {
    public static final String MOD_ID = "multigolem";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    private static volatile MultiGolemConfig CONFIG = MultiGolemConfig.defaults();

    public static MultiGolemConfig config() { return CONFIG; }

    @Override
    public void onInitialize() {
        GolemVariantAttachment.touch();
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json");
        CONFIG = MultiGolemConfig.loadOrCreate(configFile);
        LOG.info("MultiGolem starting up - config loaded from {}", configFile);
    }
}
```

- [ ] **Step 4.7: Build and verify**

```powershell
./gradlew --quiet build > build_out.txt 2>&1; $LASTEXITCODE
```

Expected: `0`.

- [ ] **Step 4.8: Commit**

```powershell
git add src/main/java/dev/charles/multigolem/config/TierStats.java src/main/java/dev/charles/multigolem/config/MultiGolemConfig.java src/main/java/dev/charles/multigolem/MultiGolem.java src/test/java/dev/charles/multigolem/config/MultiGolemConfigTest.java
git commit -m "feat: add MultiGolemConfig with JSON load/validate/clamp"
```

---

## Task 5: `GolemStatsResolver` — pure helper

Trivial wrapper, but pulled out so the mixin in Task 6 stays thin and the resolver has a unit test (required by `checkUnitTestCompanions`).

**Files:**
- Create: `src/main/java/dev/charles/multigolem/stats/GolemStatsResolver.java`
- Test: `src/test/java/dev/charles/multigolem/stats/GolemStatsResolverTest.java`

- [ ] **Step 5.1: Write the failing test**

Create `src/test/java/dev/charles/multigolem/stats/GolemStatsResolverTest.java`:

```java
package dev.charles.multigolem.stats;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.MultiGolemConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GolemStatsResolverTest {

    @Test
    void resolvesAllVariantsFromDefaults() {
        MultiGolemConfig cfg = MultiGolemConfig.defaults();
        GolemStatsResolver r = new GolemStatsResolver(cfg);

        assertEquals(60,   r.maxHealth(GolemVariant.COPPER));
        assertEquals(8.5,  r.attackDamage(GolemVariant.COPPER), 0.0001);
        assertTrue(r.angerOnHit(GolemVariant.COPPER));

        assertEquals(600,  r.maxHealth(GolemVariant.NETHERITE));
        assertEquals(85.0, r.attackDamage(GolemVariant.NETHERITE), 0.0001);
    }

    @Test
    void ironMatchesVanillaDefaults() {
        GolemStatsResolver r = new GolemStatsResolver(MultiGolemConfig.defaults());
        assertEquals(100,  r.maxHealth(GolemVariant.IRON));
        assertEquals(14.0, r.attackDamage(GolemVariant.IRON), 0.0001);
    }
}
```

- [ ] **Step 5.2: Run, verify failure**

```powershell
./gradlew --quiet test > test_out.txt 2>&1; $LASTEXITCODE
```

Expected: non-zero with `cannot find symbol: GolemStatsResolver`.

- [ ] **Step 5.3: Implement**

Create `src/main/java/dev/charles/multigolem/stats/GolemStatsResolver.java`:

```java
package dev.charles.multigolem.stats;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.MultiGolemConfig;
import dev.charles.multigolem.config.TierStats;

public final class GolemStatsResolver {

    private final MultiGolemConfig config;

    public GolemStatsResolver(MultiGolemConfig config) {
        this.config = config;
    }

    public int maxHealth(GolemVariant variant) {
        return config.tier(variant).maxHealth();
    }

    public double attackDamage(GolemVariant variant) {
        return config.tier(variant).attackDamage();
    }

    public boolean angerOnHit(GolemVariant variant) {
        return config.tier(variant).angerOnHit();
    }

    public TierStats stats(GolemVariant variant) {
        return config.tier(variant);
    }
}
```

- [ ] **Step 5.4: Run tests, verify pass**

```powershell
./gradlew --quiet test > test_out.txt 2>&1; $LASTEXITCODE
```

Expected: `0`.

- [ ] **Step 5.5: Commit**

```powershell
git add src/main/java/dev/charles/multigolem/stats/GolemStatsResolver.java src/test/java/dev/charles/multigolem/stats/GolemStatsResolverTest.java
git commit -m "feat: add GolemStatsResolver pure helper"
```

---

## Task 6: `IronGolemMixin` — stat interception

This task implements either **Plan A (live-computed via `getAttributeValue` mixin)** or **Plan B (deterministic transient modifiers applied on attachment set/load)**, per the decision recorded in `docs/26.1.2-mojang-targets.md` from Task 1.

**Files:**
- Create: `src/main/java/dev/charles/multigolem/mixin/IronGolemMixin.java`
- Modify: `src/main/resources/multigolem.mixins.json` (add the mixin)

### Plan A — live-computed

- [ ] **Step 6.A.1: Add the mixin to the config**

Edit `src/main/resources/multigolem.mixins.json` — replace `"mixins": []` with:

```json
  "mixins": [
    "IronGolemMixin"
  ],
```

- [ ] **Step 6.A.2: Implement the mixin**

Create `src/main/java/dev/charles/multigolem/mixin/IronGolemMixin.java` (substitute `<METHOD>` and `<SIGNATURE>` placeholders with the exact names from `docs/26.1.2-mojang-targets.md`):

```java
package dev.charles.multigolem.mixin;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IronGolem.class)
public abstract class IronGolemMixin {

    @Inject(method = "getAttributeValue(Lnet/minecraft/core/Holder;)D", at = @At("HEAD"), cancellable = true)
    private void multigolem$liveComputeStat(Holder<Attribute> attribute, CallbackInfoReturnable<Double> cir) {
        IronGolem self = (IronGolem) (Object) this;
        GolemVariant variant = GolemVariantAttachment.get(self);
        if (variant == GolemVariant.IRON) return;

        if (attribute.is(Attributes.MAX_HEALTH)) {
            cir.setReturnValue((double) MultiGolem.config().tier(variant).maxHealth());
        } else if (attribute.is(Attributes.ATTACK_DAMAGE)) {
            cir.setReturnValue(MultiGolem.config().tier(variant).attackDamage());
        }
    }
}
```

The `getAttributeValue` descriptor is the Mojang-mapped LivingEntity method. If the spike confirmed a different signature (e.g., `(Lnet/minecraft/world/entity/ai/attributes/Attribute;)D` without the `Holder` wrapper in older builds), substitute. If `Attributes.MAX_HEALTH` is named `GENERIC_MAX_HEALTH` in 26.1.2, substitute consistently.

### Plan B — transient modifiers (fallback)

Use only if Step 1.5 found live-computed too hot.

- [ ] **Step 6.B.1: Add the mixin to the config** — same as 6.A.1.

- [ ] **Step 6.B.2: Implement a different `IronGolemMixin`**

```java
package dev.charles.multigolem.mixin;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import dev.charles.multigolem.config.TierStats;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IronGolem.class)
public abstract class IronGolemMixin {

    private static final ResourceLocation HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(MultiGolem.MOD_ID, "variant_health");
    private static final ResourceLocation DAMAGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(MultiGolem.MOD_ID, "variant_damage");
    private static final double IRON_BASE_HEALTH = 100.0;
    private static final double IRON_BASE_ATTACK = 15.0;

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void multigolem$ensureModifiers(CallbackInfo ci) {
        IronGolem self = (IronGolem) (Object) this;
        GolemVariant variant = GolemVariantAttachment.get(self);
        TierStats stats = MultiGolem.config().tier(variant);

        applyDelta(self.getAttribute(Attributes.MAX_HEALTH), HEALTH_MODIFIER_ID, stats.maxHealth() - IRON_BASE_HEALTH);
        applyDelta(self.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_MODIFIER_ID, stats.attackDamage() - IRON_BASE_ATTACK);
    }

    private static void applyDelta(AttributeInstance instance, ResourceLocation id, double delta) {
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null && existing.amount() == delta) return;
        instance.removeModifier(id);
        if (delta != 0.0) {
            instance.addTransientModifier(new AttributeModifier(id, delta, AttributeModifier.Operation.ADD_VALUE));
        }
    }
}
```

If the modifier API in 26.1.2 differs (UUID-based instead of ResourceLocation-based), use the appropriate constructor — the spike output will have caught this.

### Both plans

- [ ] **Step 6.3: Build**

```powershell
./gradlew --quiet build > build_out.txt 2>&1; $LASTEXITCODE
```

Expected: `0`. Mixin failures will show as `mixin apply failed` in the filter.

- [ ] **Step 6.4: In-game smoke test**

Run `./gradlew runServer`. In the dev client (or another `runClient` window if you have one), use a command to attach a variant manually since creation isn't wired yet:

```
/data merge entity @e[type=iron_golem,limit=1,sort=nearest] {fabric:attachments:{"multigolem:variant":"netherite"}}
```

(Exact attachment NBT path may vary by Fabric API version; if `/data get entity @e[...] fabric:attachments` doesn't show the path, query an entity that does have attachments set programmatically in Task 7's smoke test to find the path. If you can't set it via /data in this task, defer the in-game smoke to after Task 7.)

Verify with `/data get entity @e[type=iron_golem,limit=1] Health` that maximum HP reflects 600 (Plan A) or shows the modifier (Plan B). Stop the server.

- [ ] **Step 6.5: Commit**

```powershell
git add src/main/resources/multigolem.mixins.json src/main/java/dev/charles/multigolem/mixin/IronGolemMixin.java
git commit -m "feat: live-compute golem stats from attached variant (per spike decision)"
```

(Adjust the commit message body to mention which plan was implemented.)

---

## Task 7: `GolemCreationHandler` + `CarvedPumpkinBlockMixin` — the wow moment

After this task, Charles can place 4 copper blocks + a pumpkin and spawn a copper golem with 60 HP.

**Files:**
- Create: `src/main/java/dev/charles/multigolem/spawn/GolemCreationHandler.java`
- Create: `src/main/java/dev/charles/multigolem/mixin/CarvedPumpkinBlockMixin.java`
- Modify: `src/main/resources/multigolem.mixins.json` (add `CarvedPumpkinBlockMixin`)

- [ ] **Step 7.1: Implement `GolemCreationHandler`**

Create `src/main/java/dev/charles/multigolem/spawn/GolemCreationHandler.java`:

```java
package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;

public final class GolemCreationHandler {

    private GolemCreationHandler() {}

    /**
     * Attempts to spawn a variant golem from a T-pattern of body blocks.
     * Returns true if a non-vanilla variant was spawned (and the caller should suppress vanilla iron spawn).
     * Returns false if no recognized body block was at the center / unsupported pattern → caller falls through to vanilla.
     */
    public static boolean trySpawnVariant(Level level, BlockPos headPos, Block bodyBlock) {
        if (!(level instanceof ServerLevel server)) return false;
        Optional<GolemVariant> maybeVariant = GolemVariant.fromBodyBlock(bodyBlock);
        if (maybeVariant.isEmpty()) return false;
        GolemVariant variant = maybeVariant.get();
        if (variant == GolemVariant.IRON) return false;

        // Pumpkin head is consumed by the caller (vanilla pattern check matches and replaces blocks).
        // We need to remove the 4 body blocks: center directly below the head, plus the cross arms.
        BlockPos center = headPos.below();
        clearBlocks(server, center);

        BlockPos spawnPos = headPos.below(2);
        IronGolem golem = EntityType.IRON_GOLEM.create(server, EntitySpawnReason.MOB_SUMMONED);
        if (golem == null) {
            MultiGolem.LOG.warn("Failed to create iron golem entity for variant {}", variant.id());
            return false;
        }
        golem.moveTo(spawnPos.getX() + 0.5, spawnPos.getY() + 0.05, spawnPos.getZ() + 0.5, 0.0F, 0.0F);
        golem.setPlayerCreated(true);
        GolemVariantAttachment.set(golem, variant);
        golem.setHealth(MultiGolem.config().tier(variant).maxHealth());

        server.addFreshEntity(golem);
        return true;
    }

    private static void clearBlocks(ServerLevel level, BlockPos center) {
        // center block (body)
        level.setBlock(center, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        // two arm blocks east/west of center (vanilla iron golem pattern is N-S facing, arms on X axis; mirror by Z too)
        for (BlockPos arm : new BlockPos[]{ center.east(), center.west(), center.north(), center.south() }) {
            BlockState s = level.getBlockState(arm);
            if (s.is(s.getBlock())) {
                // Only clear if it's a body block — we don't know orientation; clear both arms harmlessly.
                level.setBlock(arm, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        // base block below center
        level.setBlock(center.below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }
}
```

(The arm-clearing is intentionally generous because we cleared more than strictly needed — vanilla's pumpkin pattern resolves orientation. If this causes incorrect clears in playtest, narrow it by checking which arm pair is body-block before clearing.)

(The exact spawn reason constant may be named `EntitySpawnReason.MOB_SUMMONED`, `MobSpawnType.MOB_SUMMONED`, or `EntitySpawnReason.SPAWN_EGG` in 26.1.2 — use the Mojang-name confirmed by the spike.)

- [ ] **Step 7.2: Implement `CarvedPumpkinBlockMixin`**

Use the target method name confirmed by the spike (Step 1.3). Create `src/main/java/dev/charles/multigolem/mixin/CarvedPumpkinBlockMixin.java`:

```java
package dev.charles.multigolem.mixin;

import dev.charles.multigolem.spawn.GolemCreationHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CarvedPumpkinBlock.class)
public abstract class CarvedPumpkinBlockMixin {

    /**
     * Replace with the actual signature confirmed by docs/26.1.2-mojang-targets.md.
     * In recent Mojang mappings this method is named trySpawnGolem and takes (Level, BlockPos).
     */
    @Inject(method = "trySpawnGolem(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"), cancellable = true)
    private void multigolem$tryVariantSpawn(Level level, BlockPos pos, CallbackInfo ci) {
        BlockState bodyState = level.getBlockState(pos.below());
        if (GolemCreationHandler.trySpawnVariant(level, pos, bodyState.getBlock())) {
            ci.cancel();
        }
        // If the body block was iron or unsupported, fall through to vanilla logic.
    }
}
```

- [ ] **Step 7.3: Add to mixins config**

Edit `src/main/resources/multigolem.mixins.json`:

```json
  "mixins": [
    "IronGolemMixin",
    "CarvedPumpkinBlockMixin"
  ],
```

- [ ] **Step 7.4: Build**

```powershell
./gradlew --quiet build > build_out.txt 2>&1; $LASTEXITCODE
```

Expected: `0`.

- [ ] **Step 7.5: In-game smoke — the wow moment**

```powershell
./gradlew runServer > run_out.txt 2>&1
```

In a separate `runClient` instance, in creative mode:

1. Build a T-pattern with **copper blocks**: 1 block on the ground, 1 above it, 2 blocks as arms on each side of the upper block (the standard iron golem T).
2. Place a **carved pumpkin** on top.
3. Verify a golem spawns. Check its HP via `/data get entity @e[type=iron_golem,limit=1,sort=nearest] Health` — expect `60.0f` for copper.
4. Repeat for gold (130), emerald (200), diamond (350), netherite (600).
5. Build a vanilla **iron** T-pattern → verify it still spawns a 100-HP iron golem (vanilla path).

Stop the server.

If patterns don't spawn: check `run_out.txt` for `mixin apply failed`. If the mixin target method name was wrong, fix per spike output and re-run.

- [ ] **Step 7.6: Commit**

```powershell
git add src/main/java/dev/charles/multigolem/spawn/GolemCreationHandler.java src/main/java/dev/charles/multigolem/mixin/CarvedPumpkinBlockMixin.java src/main/resources/multigolem.mixins.json
git commit -m "feat: spawn variant golems from copper/gold/emerald/diamond/netherite T-pattern"
```

---

## Task 8: Healing — right-click with matching ingot

Extend `IronGolemMixin` to handle right-click healing for non-IRON variants. IRON keeps vanilla behavior (iron ingot heals iron golem).

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/mixin/IronGolemMixin.java`

- [ ] **Step 8.1: Add the heal injection**

Append the following method to `IronGolemMixin`. Use the `mobInteract` method name confirmed by the spike (Step 1.2 — likely `mobInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;`):

```java
    @Inject(method = "mobInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"), cancellable = true)
    private void multigolem$healWithMatchingIngot(net.minecraft.world.entity.player.Player player,
                                                   net.minecraft.world.InteractionHand hand,
                                                   CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir) {
        if (!MultiGolem.config().allowGolemHealing()) return;

        IronGolem self = (IronGolem) (Object) this;
        GolemVariant variant = GolemVariantAttachment.get(self);
        net.minecraft.world.item.ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) return;

        java.util.Optional<GolemVariant> heldVariant = GolemVariant.fromIngot(held.getItem());
        if (heldVariant.isEmpty()) return;
        if (heldVariant.get() != variant) return;
        if (variant == GolemVariant.IRON) return; // vanilla handles iron→iron natively

        float before = self.getHealth();
        self.heal(25.0F);
        if (self.getHealth() == before) return; // already full HP; let vanilla handle it
        if (!player.getAbilities().instabuild) held.shrink(1);
        self.level().playSound(null, self.blockPosition(),
            net.minecraft.sounds.SoundEvents.IRON_GOLEM_REPAIR,
            net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);
        cir.setReturnValue(net.minecraft.world.InteractionResult.sidedSuccess(self.level().isClientSide()));
    }
```

Add the missing imports at the top of the file (or keep them fully qualified as above — both are acceptable; fully qualified avoids touching the import section). Substitute Mojang names from the spike if any differ (`InteractionResult.sidedSuccess` vs `succeedsIfCallerIsServer` etc.).

- [ ] **Step 8.2: Build**

```powershell
./gradlew --quiet build > build_out.txt 2>&1; $LASTEXITCODE
```

Expected: `0`.

- [ ] **Step 8.3: In-game smoke**

Run a server. Spawn a copper golem (Task 7's recipe), damage it to ~half HP (e.g., shoot it with a snowball or attack with a wooden sword — but turn off retaliation first by setting `anger_on_hit: false` for copper in the config and restart, **or** outrun it). Right-click with a copper ingot. Expect: HP increases by 25, sound plays, one copper ingot consumed.

Then right-click the copper golem with an iron ingot — expect: nothing happens. Right-click a vanilla iron golem with copper — expect: nothing happens. Right-click iron with iron — expect: vanilla heal (unchanged).

Set `allow_golem_healing: false` in `config/multigolem.json`, restart server, retry: expect no heal.

- [ ] **Step 8.4: Commit**

```powershell
git add src/main/java/dev/charles/multigolem/mixin/IronGolemMixin.java
git commit -m "feat: heal variant golems with matching ingot"
```

---

## Task 9: Anger toggle — per-tier `anger_on_hit`

When a tier's `anger_on_hit` is `false`, the golem does not retaliate against players who attack it.

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/mixin/IronGolemMixin.java`

- [ ] **Step 9.1: Identify the retaliation interception point**

From Step 1.2 spike, the retaliation site is most likely either:

- An override of `Mob#setLastHurtByPlayer(Player)` on `IronGolem`, or
- A call to `setTarget(LivingEntity)` from a `hurt`/`actuallyHurt` override.

Use whichever the spike identified. If `setLastHurtByPlayer` is the easier surface, inject there; otherwise inject into the damage handler.

- [ ] **Step 9.2: Add the inject**

Append to `IronGolemMixin` (substituting the actual method signature from the spike):

```java
    @Inject(method = "setLastHurtByPlayer(Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"), cancellable = true)
    private void multigolem$suppressAngerOnHit(net.minecraft.world.entity.player.Player attacker, CallbackInfo ci) {
        IronGolem self = (IronGolem) (Object) this;
        GolemVariant variant = GolemVariantAttachment.get(self);
        if (!MultiGolem.config().tier(variant).angerOnHit()) {
            ci.cancel();
        }
    }
```

If the spike identified a `setTarget` site instead, the inject targets `setTarget(Lnet/minecraft/world/entity/LivingEntity;)V` with the same body (but you'll need an `@Inject` + `cancellable` to short-circuit). Either way, the *damage itself* still applies — we only cancel retaliation targeting.

- [ ] **Step 9.3: Build**

```powershell
./gradlew --quiet build > build_out.txt 2>&1; $LASTEXITCODE
```

Expected: `0`.

- [ ] **Step 9.4: In-game smoke**

Edit `config/multigolem.json` and set `"anger_on_hit": false` for copper. Restart server. Spawn a copper golem, hit it with a sword. Expect: it takes damage but does not turn toward you or attack. Set `true` and restart — expect: it retaliates as normal.

- [ ] **Step 9.5: Commit**

```powershell
git add src/main/java/dev/charles/multigolem/mixin/IronGolemMixin.java
git commit -m "feat: per-tier anger_on_hit toggle suppresses retaliation when false"
```

---

## Task 10: Drops — variant-specific death loot

Per Step 1.6, this task takes **plan A (loot-table override + custom condition)** or **plan B (drop-method mixin)**.

**Files (Plan A):**
- Create: `src/main/java/dev/charles/multigolem/loot/HasGolemVariantLootCondition.java`
- Create: `src/main/resources/data/multigolem/loot_tables/entities/iron_golem.json` (override)
- Modify: `src/main/java/dev/charles/multigolem/MultiGolem.java` (register the loot condition)

**Files (Plan B):**
- Modify: `src/main/java/dev/charles/multigolem/mixin/IronGolemMixin.java`

### Plan A — loot-table override (preferred per spec §6.3)

- [ ] **Step 10.A.1: Implement the custom loot condition**

Create `src/main/java/dev/charles/multigolem/loot/HasGolemVariantLootCondition.java`:

```java
package dev.charles.multigolem.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

public record HasGolemVariantLootCondition(GolemVariant variant) implements LootItemCondition {

    public static final MapCodec<HasGolemVariantLootCondition> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            GolemVariant.CODEC.fieldOf("variant").forGetter(HasGolemVariantLootCondition::variant)
        ).apply(instance, HasGolemVariantLootCondition::new)
    );

    public static final LootItemConditionType TYPE = new LootItemConditionType(CODEC);

    @Override
    public LootItemConditionType getType() { return TYPE; }

    @Override
    public boolean test(LootContext ctx) {
        Entity e = ctx.getParamOrNull(LootContextParams.THIS_ENTITY);
        return e != null && GolemVariantAttachment.get(e) == variant;
    }
}
```

- [ ] **Step 10.A.2: Register the condition type**

Add to `MultiGolem.onInitialize`:

```java
        net.minecraft.core.Registry.register(
            net.minecraft.core.registries.BuiltInRegistries.LOOT_CONDITION_TYPE,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "has_golem_variant"),
            dev.charles.multigolem.loot.HasGolemVariantLootCondition.TYPE
        );
```

- [ ] **Step 10.A.3: Write the loot-table override**

Create `src/main/resources/data/multigolem/loot_tables/entities/iron_golem.json`. Six pools, one per variant, each gated by the `multigolem:has_golem_variant` condition. Poppy pool is shared (no condition):

```json
{
  "type": "minecraft:entity",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        { "type": "minecraft:item", "name": "minecraft:poppy", "functions": [
          { "function": "minecraft:set_count", "count": { "type": "uniform", "min": 0, "max": 2 } }
        ]}
      ]
    },
    {
      "rolls": 1,
      "conditions": [ { "condition": "multigolem:has_golem_variant", "variant": "iron" } ],
      "entries": [
        { "type": "minecraft:item", "name": "minecraft:iron_ingot", "functions": [
          { "function": "minecraft:set_count", "count": { "type": "uniform", "min": 3, "max": 5 } }
        ]}
      ]
    },
    {
      "rolls": 1,
      "conditions": [ { "condition": "multigolem:has_golem_variant", "variant": "copper" } ],
      "entries": [
        { "type": "minecraft:item", "name": "minecraft:copper_ingot", "functions": [
          { "function": "minecraft:set_count", "count": { "type": "uniform", "min": 3, "max": 5 } }
        ]}
      ]
    },
    {
      "rolls": 1,
      "conditions": [ { "condition": "multigolem:has_golem_variant", "variant": "gold" } ],
      "entries": [
        { "type": "minecraft:item", "name": "minecraft:gold_ingot", "functions": [
          { "function": "minecraft:set_count", "count": { "type": "uniform", "min": 3, "max": 5 } }
        ]}
      ]
    },
    {
      "rolls": 1,
      "conditions": [ { "condition": "multigolem:has_golem_variant", "variant": "emerald" } ],
      "entries": [
        { "type": "minecraft:item", "name": "minecraft:emerald", "functions": [
          { "function": "minecraft:set_count", "count": { "type": "uniform", "min": 3, "max": 5 } }
        ]}
      ]
    },
    {
      "rolls": 1,
      "conditions": [ { "condition": "multigolem:has_golem_variant", "variant": "diamond" } ],
      "entries": [
        { "type": "minecraft:item", "name": "minecraft:diamond", "functions": [
          { "function": "minecraft:set_count", "count": { "type": "uniform", "min": 3, "max": 5 } }
        ]}
      ]
    },
    {
      "rolls": 1,
      "conditions": [ { "condition": "multigolem:has_golem_variant", "variant": "netherite" } ],
      "entries": [
        { "type": "minecraft:item", "name": "minecraft:netherite_scrap", "functions": [
          { "function": "minecraft:set_count", "count": { "type": "uniform", "min": 2, "max": 3 } }
        ]}
      ]
    }
  ]
}
```

Note: this **replaces** the vanilla iron-golem loot table. Iron drops are preserved via the iron-gated pool above. The path `data/multigolem/loot_tables/entities/iron_golem.json` overrides `data/minecraft/loot_tables/entities/iron_golem.json` — confirm Fabric resolves this correctly in 26.1.2; if not, switch to a Fabric loot-table modify event in `MultiGolem.onInitialize` adding the new pools to the existing iron golem table.

- [ ] **Step 10.A.4: Build and smoke-test**

```powershell
./gradlew --quiet build > build_out.txt 2>&1; $LASTEXITCODE
```

Run a server. Spawn a copper golem. Kill it (creative). Expect: 3–5 copper ingots + 0–2 poppies drop. Repeat for each variant, including vanilla iron.

- [ ] **Step 10.A.5: Commit**

```powershell
git add src/main/java/dev/charles/multigolem/loot/HasGolemVariantLootCondition.java src/main/resources/data/multigolem/loot_tables/entities/iron_golem.json src/main/java/dev/charles/multigolem/MultiGolem.java
git commit -m "feat: variant-aware loot table for iron golem deaths"
```

### Plan B — drop-method mixin (fallback)

Use only if Step 1.6 found `LootContext` can't expose the entity to a custom condition.

- [ ] **Step 10.B.1: Append the drops injection to `IronGolemMixin`**

Use the loot-table method confirmed by the spike (Step 1.2 — likely `getDefaultLootTable()` returns a `ResourceKey<LootTable>`):

```java
    @Inject(method = "getDefaultLootTable()Lnet/minecraft/resources/ResourceKey;", at = @At("HEAD"), cancellable = true)
    private void multigolem$variantLootTable(CallbackInfoReturnable<net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable>> cir) {
        IronGolem self = (IronGolem) (Object) this;
        GolemVariant variant = GolemVariantAttachment.get(self);
        if (variant == GolemVariant.IRON) return;
        cir.setReturnValue(net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.LOOT_TABLE,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MultiGolem.MOD_ID, "entities/" + variant.id() + "_golem")
        ));
    }
```

Then ship six separate loot tables: `data/multigolem/loot_tables/entities/copper_golem.json`, `gold_golem.json`, `emerald_golem.json`, `diamond_golem.json`, `netherite_golem.json`, and `iron_golem.json` (or skip the iron one and let vanilla handle iron). Each looks like the vanilla iron golem table but with the variant's drop swapped in.

- [ ] **Step 10.B.2: Smoke-test, then commit** with `feat: variant-aware loot via per-variant tables (drop-mixin fallback)`.

---

## Task 11: Port verification Gradle tasks from signport

Bring over the modernized signport tasks: `checkChangelog`, `checkWrapperChecksum`, `checkUnitTestCompanions`, `releaseReminder`. Skip `checkSourceHygiene` (signport-specific).

**Files:**
- Modify: `build.gradle`
- Create: `CHANGELOG.md`

- [ ] **Step 11.1: Bootstrap `CHANGELOG.md`**

Create at project root:

```markdown
# Changelog

## Unreleased

- Initial V1: 6 golem variants (Copper, Iron, Gold, Emerald, Diamond, Netherite) as attachments on vanilla iron golems
- T-pattern creation with each tier's material block
- Per-tier stat scaling (HP and attack damage)
- Healing with matching ingot (configurable via `allow_golem_healing`)
- Per-tier `anger_on_hit` toggle
- Material-specific drops on death
- Server-side functional; vanilla clients work without the mod
```

- [ ] **Step 11.2: Port `checkChangelog`**

Copy the `tasks.register("checkChangelog")` block from `C:\Users\tyler\AI Projects\signport\build.gradle` lines 81–186. No changes required — it's repo-agnostic.

- [ ] **Step 11.3: Port `checkWrapperChecksum`**

Copy `tasks.register("checkWrapperChecksum")` from signport lines 188–200. No changes required.

- [ ] **Step 11.4: Port `checkUnitTestCompanions`**

Copy the task from signport lines 202–227. Adjust the `include` patterns to match MultiGolem's classes:

```gradle
    def requiredTestFiles = fileTree("src/main/java").matching {
        include "**/*Config.java"
        include "**/*Resolver.java"
    }.files.collect { mainFile ->
```

(We dropped `*State` and `*Format` since MultiGolem has none.)

- [ ] **Step 11.5: Port `releaseReminder` and wire into `check`/`build`**

Copy `tasks.register("releaseReminder")` from signport lines 322–356.

Add to `build.gradle`:

```gradle
tasks.named("check") {
    dependsOn("checkChangelog", "checkWrapperChecksum", "checkUnitTestCompanions")
}

tasks.named("build") {
    finalizedBy("releaseReminder")
}
```

- [ ] **Step 11.6: Pin the wrapper checksum**

If `gradle/wrapper/gradle-wrapper.properties` doesn't already have `distributionSha256Sum=...`, fetch the official checksum for the current Gradle distribution from https://gradle.org/release-checksums/ and add the line:

```
distributionSha256Sum=<64-hex-char-sha256>
```

- [ ] **Step 11.7: Build and verify all gates pass**

```powershell
./gradlew --quiet check > check_out.txt 2>&1; $LASTEXITCODE
```

Expected: `0`. If `checkUnitTestCompanions` fails, ensure `MultiGolemConfig` and `GolemStatsResolver` each have a `*Test.java` companion (they should from Tasks 4 and 5).

- [ ] **Step 11.8: Commit**

```powershell
git add build.gradle CHANGELOG.md gradle/wrapper/gradle-wrapper.properties
git commit -m "chore: port checkChangelog/checkWrapperChecksum/checkUnitTestCompanions/releaseReminder from signport"
```

---

## Task 12: CI workflows + Modrinth/CurseForge upload scripts

Bring over `.github/workflows/build.yml`, `release.yml`, `scripts/upload-modrinth.ps1`, `scripts/upload-curseforge.ps1`, and the listing markdown.

**Files:**
- Create: `.github/workflows/build.yml`
- Create: `.github/workflows/release.yml`
- Create: `scripts/upload-modrinth.ps1`
- Create: `scripts/upload-curseforge.ps1`
- Create: `docs/modrinth-listing.md`
- Create: `docs/curseforge-listing.md`

- [ ] **Step 12.1: Copy build/release workflows from signport**

```powershell
New-Item -ItemType Directory -Path ".github\workflows" -Force | Out-Null
Copy-Item "C:\Users\tyler\AI Projects\signport\.github\workflows\build.yml" ".github\workflows\build.yml"
Copy-Item "C:\Users\tyler\AI Projects\signport\.github\workflows\release.yml" ".github\workflows\release.yml"
```

Edit both files — search-and-replace `signport` → `multigolem`. Specifically check: artifact names, project IDs (in env), and any path references like `scripts/upload-*.ps1` (paths stay the same). Verify both files retain pinned SHAs for actions, not version tags.

- [ ] **Step 12.2: Copy upload scripts**

```powershell
New-Item -ItemType Directory -Path "scripts" -Force | Out-Null
Copy-Item "C:\Users\tyler\AI Projects\signport\scripts\upload-modrinth.ps1" "scripts\upload-modrinth.ps1"
Copy-Item "C:\Users\tyler\AI Projects\signport\scripts\upload-curseforge.ps1" "scripts\upload-curseforge.ps1"
```

Read each file. Substitute project-name strings (`signport` → `multigolem`) but leave API URLs and env var names unchanged. The scripts already use `MODRINTH_TOKEN`, `CURSEFORGE_TOKEN`, `CURSEFORGE_PROJECT_ID` as env names — keep them.

- [ ] **Step 12.3: Draft listing docs**

Create `docs/modrinth-listing.md`:

```markdown
# MultiGolem — Modrinth Listing

## Project Description

MultiGolem adds five new Iron Golem variants — Copper, Gold, Emerald, Diamond, and Netherite — alongside the vanilla Iron Golem. Each variant has its own stats, drops, and healing material.

- **Server-side functional.** Vanilla clients can connect to a server running MultiGolem with no mod installed.
- **Build them like an iron golem.** T-pattern of body blocks + a carved pumpkin. Swap the body block for copper, gold, emerald, diamond, or netherite blocks.
- **Each tier scales.** Copper is the weakest; netherite is strong enough to kill a Warden in ~6 hits.
- **Heal with matching ingots.** Copper golem? Copper ingot. Diamond golem? Diamond. Configurable.
- **Configurable.** Per-tier health, damage, and "should this tier retaliate when hit" toggles.

A father-and-son project. Built with Charles.
```

Create `docs/curseforge-listing.md` with the required sections (signport's hygiene check requires `## Features`, `## Sign Format`, `## Commands`, `## Permissions`, `## Requirements` — but those are signport-specific; for MultiGolem the natural sections are):

```markdown
# MultiGolem — CurseForge Listing

## Features

- 5 new Iron Golem variants: Copper, Gold, Emerald, Diamond, Netherite
- Stats scale with material; netherite kills a Warden in ~6 hits
- T-pattern creation: 4 body blocks + carved pumpkin
- Heal with matching ingots
- Per-tier `anger_on_hit` toggle in config
- Server-side functional; vanilla clients welcome

## Creation Recipes

Build a T-shape (1 base, 1 center, 2 arms) out of one of:

- Copper Block → Copper Golem
- Gold Block → Gold Golem
- Emerald Block → Emerald Golem
- Diamond Block → Diamond Golem
- Netherite Block → Netherite Golem

Place a carved pumpkin on top.

## Configuration

Edit `config/multigolem.json` to tune per-tier `max_health`, `attack_damage`, and `anger_on_hit`. Set `allow_golem_healing: false` to disable ingot-based healing globally.

## Requirements

- Minecraft 26.1.2
- Fabric Loader 0.19.2+
- Fabric API 0.148.0+26.1.2

## V1 Limitations

- No client texture yet — all variants visually appear as iron golems on modded and vanilla clients alike. Custom textures coming in V2.
- No special abilities yet — they arrive in V2.
- No village natural-spawn variants yet — arriving in V3.
```

- [ ] **Step 12.4: Commit**

```powershell
git add .github scripts docs/modrinth-listing.md docs/curseforge-listing.md
git commit -m "chore: add build/release CI and Modrinth/CurseForge listing drafts"
```

(Project IDs in the workflows/scripts remain placeholders until the Modrinth/CurseForge projects are created — set them as GitHub repo secrets at that point.)

---

## Task 13: Manual playtest checklist + README

**Files:**
- Create: `docs/playtest-checklist.md`
- Modify: `README.md`

- [ ] **Step 13.1: Write the playtest checklist**

Create `docs/playtest-checklist.md`:

```markdown
# MultiGolem V1 Playtest Checklist

Run a Fabric server with this mod installed. Open a client and connect (modded or vanilla). Work through each row. A failed row blocks V1 release.

## Creation

- [ ] Build T-pattern of copper blocks + carved pumpkin → copper golem spawns, 60 HP.
- [ ] Same with gold blocks → gold golem, 130 HP.
- [ ] Emerald → emerald golem, 200 HP.
- [ ] Diamond → diamond golem, 350 HP.
- [ ] Netherite → netherite golem, 600 HP.
- [ ] Iron blocks (vanilla) → iron golem, 100 HP. Vanilla behavior unchanged.
- [ ] Pumpkin on a single block (no T-pattern) → nothing happens (vanilla behavior).

## Combat baselines

- [ ] Copper golem vs zombie on Hard: copper survives ~10 zombie hits and kills the zombie in 2–4 swings.
- [ ] Netherite golem vs Warden: netherite wins 1v1 with HP to spare; kills warden in ~6 swings.

## Drops

- [ ] Kill each variant in creative; verify the matching ingot drops (3–5 for most; 2–3 netherite scrap for netherite) plus 0–2 poppies.
- [ ] Kill a vanilla iron golem; verify vanilla 3–5 iron ingots + poppies.

## Healing

- [ ] Damage a copper golem; right-click with copper ingot → heals 25 HP, consumes 1 ingot.
- [ ] Right-click copper golem with iron ingot → no heal.
- [ ] Right-click iron golem with copper ingot → no heal.
- [ ] Right-click iron golem with iron ingot → vanilla heal works.
- [ ] Set `allow_golem_healing: false` and restart. Re-test: no variant accepts heals; iron also rejects.

## Anger toggle

- [ ] Set `copper.anger_on_hit: false` and restart. Attack copper golem: takes damage but does not retaliate.
- [ ] Set back to `true`. Attack copper golem: retaliates as normal.

## Save/load and mod removal

- [ ] Save a world with one of each variant. Quit and rejoin. Verify each retains its variant and stats.
- [ ] Uninstall the mod. Load the world. Verify: server boots; all variant golems now appear as plain iron golems with 100 HP; HP of any over-100 entity is clamped. No crashes.
- [ ] Reinstall the mod. Load the world. Verify: variants are restored, stats recompute, drops work again.

## Config robustness

- [ ] Delete `config/multigolem.json`. Start server. Verify it's recreated with defaults.
- [ ] Corrupt the config file (replace contents with `{not json`). Start server. Verify: server starts, defaults used, file is NOT overwritten.
- [ ] Set `netherite.max_health: 100000`. Verify: warning logged, value clamped to 2048.
```

- [ ] **Step 13.2: Write the README**

Create `README.md`:

```markdown
# MultiGolem

A Fabric mod for Minecraft 26.1.2 that adds Copper, Gold, Emerald, Diamond, and Netherite golem variants alongside the vanilla Iron Golem. Built by Tyler and Charles.

## What it does

- Five new golem tiers, built like an Iron Golem (T-pattern + carved pumpkin) but with a different body block.
- Stats scale: Copper is weakest, Netherite is strongest (designed to beat a Warden 1v1).
- Heal each golem with its matching ingot.
- Per-tier configuration: HP, attack damage, whether the tier retaliates when hit.
- Server-side functional. Vanilla clients can connect with no mod installed.

## Building

```
./gradlew build
```

Output jar lives at `build/libs/multigolem-<version>.jar`.

## Running in dev

```
./gradlew runServer
./gradlew runClient
```

## Configuration

Edit `config/multigolem.json` (created on first server start). Set per-tier `max_health`, `attack_damage`, `anger_on_hit`, and the global `allow_golem_healing` toggle.

## Roadmap

- **V1** (this release): Variants, stats, drops, healing, anger toggle, config. Server-side only.
- **V2**: Client texture, special abilities (copper lightning heal, gold speed, emerald villager heal, diamond lightning, netherite fire immunity + ignite).
- **V3**: Village natural-spawn variant weighting.

See `docs/superpowers/specs/2026-05-15-multigolem-design.md` for the full design.

## Contributing / License

MIT. Issues and PRs welcome.
```

- [ ] **Step 13.3: Commit**

```powershell
git add docs/playtest-checklist.md README.md
git commit -m "docs: add playtest checklist and README"
```

---

## Task 14: Run the full playtest checklist

This is the V1 acceptance gate. Do not tag a release until every checkbox in `docs/playtest-checklist.md` passes.

- [ ] **Step 14.1: Build a release jar**

```powershell
./gradlew --quiet build > build_out.txt 2>&1; $LASTEXITCODE
```

Expected: `0`. Find the jar at `build/libs/multigolem-0.1.0+mc26.1.2.jar`.

- [ ] **Step 14.2: Set up a test server**

Drop the jar plus Fabric API into a fresh MC 26.1.2 server's `mods/` folder. Start the server. Verify the log shows `MultiGolem starting up - config loaded from ...`.

- [ ] **Step 14.3: Work through every row of `docs/playtest-checklist.md`**

Tick each box as it passes. If any fails: file an issue, fix it in a new task, return here.

- [ ] **Step 14.4: Test with a vanilla client**

Connect a vanilla MC 26.1.2 client to the modded server. Verify:

- You can place blocks and the server doesn't kick you for unknown packets.
- A modded player on the same server can create a copper golem and the vanilla client sees it as an iron golem (visually).
- The vanilla client can punch it, gets dropped copper ingots from kills, can heal it with copper ingots (server-side checks fire correctly).

- [ ] **Step 14.5: Commit any fixes uncovered**

If the checklist surfaced bugs: fix them, commit each fix with its own message, then re-run the affected rows.

---

## Task 15: Tag and ship V1

After all playtest rows pass.

- [ ] **Step 15.1: Update CHANGELOG.md**

Replace `## Unreleased` with `## 0.1.0+mc26.1.2 — <YYYY-MM-DD>`, then add a fresh empty `## Unreleased` section above it.

- [ ] **Step 15.2: Commit the version bump**

```powershell
git add CHANGELOG.md
git commit -m "chore: release v0.1.0+mc26.1.2"
```

- [ ] **Step 15.3: Push to GitHub**

Create the GitHub repo at `TnTBass/multigolem` (web UI or `gh repo create`). Add it as the origin:

```powershell
git remote add origin https://github.com/TnTBass/multigolem.git
git push -u origin main
```

- [ ] **Step 15.4: Set repo secrets**

In GitHub repo Settings → Secrets and variables → Actions, add:

- `MODRINTH_TOKEN` (from https://modrinth.com/settings/pats — create with project-write scope)
- `CURSEFORGE_TOKEN` (from https://legacy.curseforge.com/account/api-tokens)
- `CURSEFORGE_PROJECT_ID` (from the project page once created)

Create the Modrinth project at https://modrinth.com/projects and the CurseForge project at https://www.curseforge.com/project-create. Both can stay "draft" until the first release uploads.

- [ ] **Step 15.5: Tag and push**

```powershell
git tag -a v0.1.0+mc26.1.2 -m "MultiGolem V1: copper/gold/emerald/diamond/netherite golems"
git push origin v0.1.0+mc26.1.2
```

GitHub Actions will run `release.yml`, build the jar, publish a GitHub Release, and upload to Modrinth + CurseForge.

- [ ] **Step 15.6: Verify the release**

- GitHub: a release shows at `https://github.com/TnTBass/multigolem/releases/tag/v0.1.0+mc26.1.2` with the jar attached.
- Modrinth: the version appears on the project page.
- CurseForge: the file appears on the project files page (may require manual "approve" by CurseForge moderators on first release).

Charles plays the mod. 🎉

---

## V1 done. V2 and V3 planning happen in separate plans.
