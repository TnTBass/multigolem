# V2 Config Migration (Spec §6.2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the V1-style parse-and-rewrite `MultiGolemConfig.loadOrCreate` with the lossless V2 migration described in spec §6.2 (merge → validate/canonicalize → compare → atomic write), preserving unknown fields and never corrupting an existing on-disk file.

**Architecture:** Operate on a single mutable `JsonObject` instance through the whole pipeline. Read the raw on-disk JSON; recursively merge defaults without overwriting existing keys; canonicalize known fields in place (clamp numerics, drop non-finite, uppercase `diamond_target_mode`, drop unknown enum members); hand the merged object to existing `parse()` for the in-memory model; compare a serialized form of the merged object byte-for-byte against the original file content; if different, write via tmp-file + `Files.move` ATOMIC_MOVE. Malformed JSON loads defaults and is left on disk untouched.

**Tech Stack:** Java 25, Gson 2 (already in classpath), JUnit 5, Fabric/Yarn build via `./gradlew.bat` on Windows.

**Scope note:** This plan supersedes the implementation skeleton in `docs/superpowers/plans/2026-05-16-multigolem-v2.md` Task 3. That task acknowledged the canonicalize step was left "for the implementer"; this plan enumerates every per-field rule and adds the malformed-JSON-not-overwritten and tmp-file-not-leaked tests that were missing.

**Repo facts that constrain the work:**
- Source: `src/main/java/dev/charles/multigolem/config/MultiGolemConfig.java` (current V1-style `loadOrCreate` is the file under modification).
- Tests: `src/test/java/dev/charles/multigolem/config/MultiGolemConfigV2Test.java` (already imports `MinecraftBootstrap`, `@TempDir`, etc. — append to it).
- Per-field validation rules live in spec `docs/superpowers/specs/2026-05-16-multigolem-v2-design.md` §6.1; the matching numeric clamp ranges and enum sets are already encoded in `MultiGolemConfig.parse*` / `TierStats` constants (`MIN_HEALTH`, `MAX_HEALTH`, `MIN_DAMAGE`, `MAX_DAMAGE`, `RECOGNIZED_IGNORED_TARGET_TYPES`, `RECOGNIZED_DIAMOND_TARGET_MODES`). Reuse those — do not introduce new constants.
- Windows build pattern (per global CLAUDE.md): run `./gradlew.bat` writing to a temp file, then `Select-String` the file. Never pipe `Select-String` inline.

---

## File Map

- **Modify** `src/main/java/dev/charles/multigolem/config/MultiGolemConfig.java`
  - Rewrite `loadOrCreate(Path)` per spec §6.2.
  - Add private helpers: `mergeDefaultsRecursive(JsonObject, JsonObject)`, `canonicalizeAndValidateInPlace(JsonObject)`, `canonicalizeTierInPlace(GolemVariant, JsonObject)`, `atomicWrite(Path, JsonObject)`.
  - Replace the existing `writeQuietly(Path, MultiGolemConfig)` callsite in the no-file branch with `atomicWrite(path, toJson(defaults))`. Delete `writeQuietly` if it has no other callers.
  - Add imports: `java.nio.file.AtomicMoveNotSupportedException`, `java.nio.file.StandardCopyOption`, `java.util.UUID`, `java.util.Map.Entry` (or just `java.util.Map`).
  - Keep `parse(JsonObject)` and all existing `parse*`/`read*`/`clamp*` helpers unchanged — `parse` is now called on the post-canonicalize object, so it is effectively a no-op on canonical input.
- **Modify** `src/test/java/dev/charles/multigolem/config/MultiGolemConfigV2Test.java`
  - Append the migration tests defined in Task 1 below.
  - Existing tests must remain green.

No new files. No production-side public-API changes.

---

## Task 1: Write failing migration tests

**Files:**
- Test: `src/test/java/dev/charles/multigolem/config/MultiGolemConfigV2Test.java`

These tests encode every spec §6.2 guarantee. Append them verbatim. Do not relax any assertion later — if an assertion fails after implementation, the bug is in the implementation, not the test.

- [ ] **Step 1.1: Append the 7 migration tests**

Append the following methods to the existing `MultiGolemConfigV2Test` class (before its closing brace). No new imports needed beyond what's already there.

```java
    @Test
    void migration_preservesUnknownTopLevelFields(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "_user_note": "lowered copper hp",
              "_third_party_mod_settings": { "foo": 42 },
              "allow_golem_healing": true,
              "tiers": {}
            }
            """);
        MultiGolemConfig.loadOrCreate(file);
        String after = Files.readString(file);
        assertTrue(after.contains("_user_note"), "unknown top-level field must be preserved");
        assertTrue(after.contains("_third_party_mod_settings"), "unknown top-level object must be preserved");
        assertTrue(after.contains("\"foo\""), "nested unknown fields must be preserved");
    }

    @Test
    void migration_preservesUnknownTierFields(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "tiers": {
                "copper": {
                  "max_health": 60,
                  "attack_damage": 8.5,
                  "anger_on_hit": true,
                  "_my_experimental_field": "preserve_me"
                }
              }
            }
            """);
        MultiGolemConfig.loadOrCreate(file);
        String after = Files.readString(file);
        assertTrue(after.contains("_my_experimental_field"), "unknown per-tier field must be preserved");
        assertTrue(after.contains("preserve_me"));
    }

    @Test
    void migration_fillsV2Defaults_preservesV1Values(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        // V1-shaped config: no V2 fields, copper has a user-edited max_health
        Files.writeString(file, """
            {
              "allow_golem_healing": true,
              "tiers": {
                "copper": { "max_health": 75, "attack_damage": 10.0, "anger_on_hit": true }
              }
            }
            """);
        MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
        assertEquals(75, cfg.tier(GolemVariant.COPPER).maxHealth(), "V1 value preserved in memory");
        assertTrue(cfg.tier(GolemVariant.COPPER).copperLightningImmune(), "V2 default filled in memory");
        String after = Files.readString(file);
        assertTrue(after.contains("\"copper_lightning_immune\""), "V2 field added on disk");
        assertTrue(after.contains("\"max_health\": 75"), "V1 value preserved on disk");
    }

    @Test
    void migration_canonicalizesDiamondTargetMode(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "tiers": { "diamond": { "diamond_target_mode": "all_hostile_mobs_and_players" } }
            }
            """);
        MultiGolemConfig.loadOrCreate(file);
        String after = Files.readString(file);
        assertTrue(after.contains("\"ALL_HOSTILE_MOBS_AND_PLAYERS\""), "canonical uppercase rewritten on disk");
        assertFalse(after.contains("all_hostile_mobs_and_players"), "lowercase removed");
    }

    @Test
    void migration_doesNotRewriteIdenticalFile(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        MultiGolemConfig.loadOrCreate(file);                 // first load writes defaults
        long mtime1 = Files.getLastModifiedTime(file).toMillis();
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        MultiGolemConfig.loadOrCreate(file);                 // second load should be a no-op write
        long mtime2 = Files.getLastModifiedTime(file).toMillis();
        assertEquals(mtime1, mtime2, "identical config should not be rewritten");
    }

    @Test
    void migration_malformedJson_leavesFileUntouched(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        String garbage = "{ this is not valid json";
        Files.writeString(file, garbage);
        MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
        // In-memory defaults are used
        assertEquals(100, cfg.tier(GolemVariant.IRON).maxHealth());
        // On-disk file is unchanged byte-for-byte
        assertEquals(garbage, Files.readString(file), "malformed file must NOT be overwritten");
    }

    @Test
    void migration_doesNotLeaveTempFile(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        MultiGolemConfig.loadOrCreate(file);                 // triggers write
        try (var stream = Files.list(tmp)) {
            long tmpCount = stream.filter(p -> p.getFileName().toString().contains(".tmp.")).count();
            assertEquals(0, tmpCount, "no .tmp.* file should be left behind after successful write");
        }
    }
```

- [ ] **Step 1.2: Run the new tests; verify each fails**

Two-step Windows build pattern (per global CLAUDE.md):

```powershell
./gradlew.bat --quiet test --tests "dev.charles.multigolem.config.MultiGolemConfigV2Test" > test_out.txt 2>&1
Select-String -Path test_out.txt -Pattern 'FAILED|migration_' | Select-Object -First 80
```

Expected: each of the 7 new `migration_*` tests is listed as FAILED. Existing V2 tests still pass.

If any of the 7 unexpectedly *passes*, the test is wrong (e.g., assertion too weak) — fix the test first, do not move on.

- [ ] **Step 1.3: Commit failing tests**

```powershell
git add src/test/java/dev/charles/multigolem/config/MultiGolemConfigV2Test.java
git commit -m "test: failing tests for V1->V2 lossless config migration (spec §6.2)"
```

---

## Task 2: Implement the recursive default-merge

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/config/MultiGolemConfig.java`

- [ ] **Step 2.1: Add the `mergeDefaultsRecursive` helper**

Add this private static method to `MultiGolemConfig` (anywhere among the other private helpers). It mutates `onDisk` in place.

```java
    private static void mergeDefaultsRecursive(JsonObject onDisk, JsonObject defaults) {
        for (Map.Entry<String, JsonElement> e : defaults.entrySet()) {
            String key = e.getKey();
            JsonElement defVal = e.getValue();
            if (!onDisk.has(key)) {
                onDisk.add(key, defVal.deepCopy());
            } else if (defVal.isJsonObject() && onDisk.get(key).isJsonObject()) {
                mergeDefaultsRecursive(onDisk.getAsJsonObject(key), defVal.getAsJsonObject());
            }
            // else: on-disk value (including JsonNull, primitives, arrays, type-mismatched values)
            //       is kept verbatim. Canonicalization runs afterwards and fixes malformed cases.
        }
        // Unknown on-disk keys (NOT in defaults) are untouched -> preserved.
    }
```

Add import if missing: `import java.util.Map;`

- [ ] **Step 2.2: Build to confirm compile**

```powershell
./gradlew.bat --quiet compileJava > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'error:|FAILED' | Select-Object -First 40
```

Expected: no errors. (Helper is unused at this stage — that is fine; Java does not warn on unused private methods.)

No commit yet — helpers will be wired up in Task 5.

---

## Task 3: Implement in-place canonicalization

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/config/MultiGolemConfig.java`

Goal: walk the merged object and rewrite each known field so its on-disk value matches what `parse*` would return in memory. After this runs, calling `parse(root)` becomes a no-op on canonical input — which is exactly why the byte-compare in §6.2 step 5 is meaningful.

Per spec §6.1 the per-field rules are:

| Field | Rule |
|---|---|
| `allow_golem_healing` | boolean, default `true` on type mismatch |
| `max_health` | int, clamp `[MIN_HEALTH, MAX_HEALTH]`, default on non-number |
| `attack_damage` | double, clamp `[MIN_DAMAGE, MAX_DAMAGE]`, non-finite → default |
| `anger_on_hit` | boolean, default on type mismatch |
| `ignored_target_types` | array of string; drop entries not in `RECOGNIZED_IGNORED_TARGET_TYPES`; non-array → default |
| `copper_lightning_immune` | boolean (copper only) |
| `copper_lightning_heal_amount` | nullable double, clamp `[0, 2048]`, non-finite → default, `JsonNull` preserved as full heal |
| `gold_speed_multiplier` | double, clamp `[0.1, 10.0]`, non-finite → default |
| `gold_sprint_particles_enabled`, `gold_sunlight_shine_enabled` | boolean |
| `emerald_aura_range` | int, clamp `[1, 64]` |
| `emerald_heal_interval_seconds` | double, `max(0.5, raw)`, non-finite → default |
| `emerald_heal_per_tick` | double, clamp `[0, 2048]`, non-finite → default |
| `emerald_count_wandering_traders` | boolean |
| `diamond_target_mode` | string, case-insensitive parse, uppercase rewrite, unknown → default `"ALL_HOSTILE_MOBS"` |
| `diamond_cooldown_min_seconds`, `diamond_cooldown_max_seconds` | int, clamp `[0, 3600]`; if `min > max`, swap (canonicalize on disk too) |
| `diamond_aura_range` | int, clamp `[1, 64]` |
| `diamond_lightning_proof` | boolean |
| `netherite_fire_immune` | boolean |
| `netherite_ignite_seconds` | int, clamp `[0, 300]` |

- [ ] **Step 3.1: Add `canonicalizeAndValidateInPlace` and `canonicalizeTierInPlace`**

Add these private static methods. They operate on the merged `JsonObject` (so every known field is guaranteed present — defaults were merged in Task 2). They never read unknown keys.

```java
    private static void canonicalizeAndValidateInPlace(JsonObject root) {
        // Top-level: allow_golem_healing must be boolean; if not, replace with default true.
        rewriteBooleanInPlace(root, "allow_golem_healing", true);

        JsonObject tiers = (root.has("tiers") && root.get("tiers").isJsonObject())
            ? root.getAsJsonObject("tiers") : null;
        if (tiers == null) return;

        MultiGolemConfig def = defaults();
        for (GolemVariant v : GolemVariant.values()) {
            if (!tiers.has(v.id()) || !tiers.get(v.id()).isJsonObject()) continue;
            canonicalizeTierInPlace(v, tiers.getAsJsonObject(v.id()), def.tier(v));
        }
    }

    private static void canonicalizeTierInPlace(GolemVariant variant, JsonObject t, TierStats def) {
        // V1 fields
        rewriteClampedIntInPlace(t, "max_health", TierStats.MIN_HEALTH, TierStats.MAX_HEALTH, def.maxHealth());
        rewriteClampedDoubleInPlace(t, "attack_damage", TierStats.MIN_DAMAGE, TierStats.MAX_DAMAGE, def.attackDamage());
        rewriteBooleanInPlace(t, "anger_on_hit", def.angerOnHit());
        rewriteIgnoredTargetTypesInPlace(t, def.ignoredTargetTypes());

        // Copper
        if (def.copperLightningImmune() != null) {
            rewriteBooleanInPlace(t, "copper_lightning_immune", def.copperLightningImmune());
            rewriteCopperHealAmountInPlace(t, def.copperLightningHealAmount());
        }
        // Gold
        if (def.goldSpeedMultiplier() != null) {
            rewriteClampedDoubleInPlace(t, "gold_speed_multiplier", 0.1, 10.0, def.goldSpeedMultiplier());
            rewriteBooleanInPlace(t, "gold_sprint_particles_enabled", def.goldSprintParticlesEnabled());
            rewriteBooleanInPlace(t, "gold_sunlight_shine_enabled", def.goldSunlightShineEnabled());
        }
        // Emerald
        if (def.emeraldAuraRange() != null) {
            rewriteClampedIntInPlace(t, "emerald_aura_range", 1, 64, def.emeraldAuraRange());
            rewriteMinDoubleInPlace(t, "emerald_heal_interval_seconds", 0.5, def.emeraldHealIntervalSeconds());
            rewriteClampedDoubleInPlace(t, "emerald_heal_per_tick", 0.0, 2048.0, def.emeraldHealPerTick());
            rewriteBooleanInPlace(t, "emerald_count_wandering_traders", def.emeraldCountWanderingTraders());
        }
        // Diamond
        if (def.diamondTargetMode() != null) {
            rewriteDiamondTargetModeInPlace(t, def.diamondTargetMode());
            rewriteClampedIntInPlace(t, "diamond_cooldown_min_seconds", 0, 3600, def.diamondCooldownMinSeconds());
            rewriteClampedIntInPlace(t, "diamond_cooldown_max_seconds", 0, 3600, def.diamondCooldownMaxSeconds());
            int min = t.get("diamond_cooldown_min_seconds").getAsInt();
            int max = t.get("diamond_cooldown_max_seconds").getAsInt();
            if (min > max) {
                MultiGolem.LOG.warn("diamond cooldown min ({}) > max ({}); swapping", min, max);
                t.addProperty("diamond_cooldown_min_seconds", max);
                t.addProperty("diamond_cooldown_max_seconds", min);
            }
            rewriteClampedIntInPlace(t, "diamond_aura_range", 1, 64, def.diamondAuraRange());
            rewriteBooleanInPlace(t, "diamond_lightning_proof", def.diamondLightningProof());
        }
        // Netherite
        if (def.netheriteFireImmune() != null) {
            rewriteBooleanInPlace(t, "netherite_fire_immune", def.netheriteFireImmune());
            rewriteClampedIntInPlace(t, "netherite_ignite_seconds", 0, 300, def.netheriteIgniteSeconds());
        }
    }

    private static void rewriteBooleanInPlace(JsonObject obj, String key, boolean fallback) {
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isBoolean()) {
            obj.addProperty(key, fallback);
        }
    }

    private static void rewriteClampedIntInPlace(JsonObject obj, String key, int min, int max, int fallback) {
        JsonElement el = obj.get(key);
        int value;
        if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
            try {
                value = el.getAsInt();
            } catch (Exception e) {
                value = fallback;
            }
        } else {
            value = fallback;
        }
        int clamped = Math.max(min, Math.min(max, value));
        // Rewrite always: ensures the on-disk value is numerically clamped even if input was JsonNull/string/object.
        obj.addProperty(key, clamped);
    }

    private static void rewriteClampedDoubleInPlace(JsonObject obj, String key, double min, double max, double fallback) {
        JsonElement el = obj.get(key);
        double value;
        if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
            double raw = el.getAsDouble();
            value = Double.isFinite(raw) ? raw : fallback;
        } else {
            value = fallback;
        }
        obj.addProperty(key, Math.max(min, Math.min(max, value)));
    }

    private static void rewriteMinDoubleInPlace(JsonObject obj, String key, double min, double fallback) {
        JsonElement el = obj.get(key);
        double value;
        if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
            double raw = el.getAsDouble();
            value = Double.isFinite(raw) ? raw : fallback;
        } else {
            value = fallback;
        }
        obj.addProperty(key, Math.max(min, value));
    }

    private static void rewriteCopperHealAmountInPlace(JsonObject t, Double fallback) {
        JsonElement el = t.get("copper_lightning_heal_amount");
        if (el instanceof JsonNull) {
            // JsonNull is canonical for "full heal" — preserve as-is.
            return;
        }
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
            if (fallback == null) t.add("copper_lightning_heal_amount", JsonNull.INSTANCE);
            else t.addProperty("copper_lightning_heal_amount", fallback);
            return;
        }
        double raw = el.getAsDouble();
        if (!Double.isFinite(raw)) {
            if (fallback == null) t.add("copper_lightning_heal_amount", JsonNull.INSTANCE);
            else t.addProperty("copper_lightning_heal_amount", fallback);
            return;
        }
        t.addProperty("copper_lightning_heal_amount", Math.max(0.0, Math.min(2048.0, raw)));
    }

    private static void rewriteIgnoredTargetTypesInPlace(JsonObject t, List<String> fallback) {
        JsonElement el = t.get("ignored_target_types");
        if (el == null || !el.isJsonArray()) {
            JsonArray arr = new JsonArray();
            for (String s : fallback) arr.add(s);
            t.add("ignored_target_types", arr);
            return;
        }
        JsonArray src = el.getAsJsonArray();
        JsonArray cleaned = new JsonArray();
        for (JsonElement e : src) {
            if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) continue;
            String s = e.getAsString();
            if (RECOGNIZED_IGNORED_TARGET_TYPES.contains(s)) cleaned.add(s);
            else MultiGolem.LOG.warn("unknown ignored_target_types value '{}'; dropped", s);
        }
        t.add("ignored_target_types", cleaned);
    }

    private static void rewriteDiamondTargetModeInPlace(JsonObject t, String fallback) {
        JsonElement el = t.get("diamond_target_mode");
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
            t.addProperty("diamond_target_mode", fallback);
            return;
        }
        String raw = el.getAsString();
        String canonical = raw == null ? null : raw.toUpperCase(Locale.ROOT);
        if (canonical == null || !RECOGNIZED_DIAMOND_TARGET_MODES.contains(canonical)) {
            MultiGolem.LOG.warn("unknown diamond_target_mode '{}'; using {}", raw, fallback);
            t.addProperty("diamond_target_mode", fallback);
        } else {
            t.addProperty("diamond_target_mode", canonical);
        }
    }
```

- [ ] **Step 3.2: Build**

```powershell
./gradlew.bat --quiet compileJava > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'error:|FAILED' | Select-Object -First 40
```

Expected: no errors.

No commit yet — wiring happens in Task 5.

---

## Task 4: Implement atomic write

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/config/MultiGolemConfig.java`

- [ ] **Step 4.1: Add `atomicWrite` helper**

```java
    private static void atomicWrite(Path target, JsonObject root) {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp." + UUID.randomUUID());
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
                MultiGolem.LOG.warn(
                    "filesystem does not support atomic move; on-disk config left as-is, "
                    + "will be re-attempted next start. In-memory config is the V2-merged version.");
            }
        } catch (IOException e) {
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            MultiGolem.LOG.warn("failed to write multigolem config at {}: {}", target, e.getMessage());
        }
    }
```

Add imports: `java.nio.file.AtomicMoveNotSupportedException`, `java.nio.file.StandardCopyOption`, `java.util.UUID`.

- [ ] **Step 4.2: Build**

```powershell
./gradlew.bat --quiet compileJava > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'error:|FAILED' | Select-Object -First 40
```

Expected: no errors.

---

## Task 5: Rewire `loadOrCreate`

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/config/MultiGolemConfig.java`

- [ ] **Step 5.1: Replace `loadOrCreate` body**

Replace the entire current `loadOrCreate(Path path)` method with:

```java
    public static MultiGolemConfig loadOrCreate(Path path) {
        if (!Files.exists(path)) {
            MultiGolemConfig defaults = defaults();
            atomicWrite(path, toJson(defaults));
            return defaults;
        }

        String originalContent;
        JsonObject onDisk;
        try {
            originalContent = Files.readString(path, StandardCharsets.UTF_8);
            onDisk = GSON.fromJson(originalContent, JsonObject.class);
            if (onDisk == null) {
                MultiGolem.LOG.warn("multigolem config at {} is empty; using defaults, NOT overwriting", path);
                return defaults();
            }
        } catch (JsonSyntaxException e) {
            MultiGolem.LOG.warn(
                "multigolem config at {} is malformed; using defaults, NOT overwriting: {}",
                path, e.getMessage());
            return defaults();
        } catch (IOException e) {
            MultiGolem.LOG.warn("could not read multigolem config at {}: {}", path, e.getMessage());
            return defaults();
        }

        JsonObject defaultsJson = toJson(defaults());
        mergeDefaultsRecursive(onDisk, defaultsJson);
        canonicalizeAndValidateInPlace(onDisk);

        MultiGolemConfig parsed = parse(onDisk);

        String merged = GSON.toJson(onDisk);
        if (!merged.equals(originalContent)) {
            atomicWrite(path, onDisk);
        }
        return parsed;
    }
```

- [ ] **Step 5.2: Delete `writeQuietly` if now unused**

Grep:

```powershell
Select-String -Path src -Pattern 'writeQuietly' -Recurse | Select-Object -First 20
```

If the only remaining hit is the method declaration in `MultiGolemConfig.java`, delete the `writeQuietly` method. Otherwise leave it alone.

- [ ] **Step 5.3: Build and run full test suite**

```powershell
./gradlew.bat --quiet build > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'error:|FAILED|cannot find symbol|incompatible types|mixin apply failed|migration_' | Select-Object -First 80
```

Expected: no errors, no failures. All 7 `migration_*` tests pass; all pre-existing V1 + V2 tests still pass.

If any `migration_*` test fails, diagnose by reading the failure detail:

```powershell
Select-String -Path build_out.txt -Pattern 'migration_|expected:|FAILED' -Context 0,3 | Select-Object -First 120
```

Fix the implementation — never the test assertion. The most likely failure modes:
- `migration_doesNotRewriteIdenticalFile` fails: indicates `toJson(defaults)` and the recursive merge produce a JSON byte sequence that differs from what the no-file branch first wrote. Cause is almost always insertion-order drift; the no-file branch and the merge branch must produce byte-identical output for the same logical content. Fix: ensure the no-file branch writes via the same path (`atomicWrite(path, toJson(defaults))`) and that no canonicalization step reorders keys.
- `migration_canonicalizesDiamondTargetMode` fails: `canonicalizeTierInPlace` did not run on the merged object, or ran before the diamond entry was merged in. Verify order: merge first, canonicalize second.
- `migration_malformedJson_leavesFileUntouched` fails: the catch branch fell through to a write. Verify the JsonSyntaxException branch `return defaults();` with no write.

- [ ] **Step 5.4: Commit**

```powershell
git add src/main/java/dev/charles/multigolem/config/MultiGolemConfig.java
git commit -m "feat(config): lossless V1->V2 JSON-merge migration with atomic write

Implements spec §6.2: read raw JSON; recursively merge V2 defaults
without overwriting existing keys; canonicalize known fields in place
(clamp numerics, drop non-finite, uppercase diamond_target_mode, drop
unknown enum members, swap min/max cooldown); compare serialized bytes
to original; if changed, atomic-move tmp file over target. Malformed
JSON loads defaults and never overwrites the user's file. Preserves
unknown top-level and per-tier fields."
```

---

## Task 6: Final verification

- [ ] **Step 6.1: Confirm clean build and no leftover temp files in repo**

```powershell
./gradlew.bat --quiet build > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'error:|FAILED|BUILD SUCCESSFUL|BUILD FAILED' | Select-Object -First 20
git status
```

Expected: `BUILD SUCCESSFUL`; `git status` shows nothing other than possibly untracked `logs/` and `build_out.txt`. Remove the latter:

```powershell
Remove-Item build_out.txt -ErrorAction SilentlyContinue
Remove-Item test_out.txt -ErrorAction SilentlyContinue
```

- [ ] **Step 6.2: Sanity-spot the spec coverage**

Re-read spec §6.2 algorithm steps 1–6 and verify each maps to code:
1. Read raw → `Files.readString` + `GSON.fromJson(..., JsonObject.class)` in `loadOrCreate`.
2. Merge defaults → `mergeDefaultsRecursive`.
3. Canonicalize known fields → `canonicalizeAndValidateInPlace`.
4. Hand to runtime → `parse(onDisk)`.
5. Compare bytes → `merged.equals(originalContent)`.
6. Atomic write → `atomicWrite`.

Plus the failure-mode rules:
- Malformed JSON → `JsonSyntaxException` branch returns defaults without writing.
- `AtomicMoveNotSupportedException` → temp deleted, WARN logged, no fallback to non-atomic write.
- IOException during write → temp deleted, WARN logged.

If any of these has no clear corresponding line, fix before reporting done.

---

## Self-Review Checklist Results

- **Spec coverage:** §6.2 steps 1–6 each map to a named code construct; failure modes match §6.2 + §10 bullet "Config migration write failure". ✅
- **Placeholder scan:** No TBD, no "handle edge cases", no "similar to". ✅
- **Type consistency:** `mergeDefaultsRecursive`, `canonicalizeAndValidateInPlace`, `canonicalizeTierInPlace`, `atomicWrite` names used consistently across tasks. `RECOGNIZED_*` constants match the existing `MultiGolemConfig` declarations. `TierStats` accessor names (`copperLightningImmune()` etc.) match the record definition. ✅
