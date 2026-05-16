# MultiGolem — Design

**Date:** 2026-05-15
**Authors:** Tyler & Charles
**Status:** Draft, awaiting review (human + Codex)

## 1. Summary

MultiGolem is a Fabric mod for Minecraft 26.1.2 that adds five new Iron Golem variants — **Copper**, **Gold**, **Emerald**, **Diamond**, **Netherite** — alongside the vanilla Iron Golem. Each variant has its own creation recipe, stat profile, drops, healing material, and (from V2 onward) special ability. The mod is designed to be **server-side functional**: vanilla clients can connect to a server running MultiGolem and play normally; they will see every variant as a regular Iron Golem, but stats, drops, and creation will all behave correctly. A client component will be added in V2 purely for visual variant textures.

## 2. Goals & Non-goals

**Goals**

- Add 5 new golem tiers that scale in power from weakest (Copper) to strongest (Netherite).
- Iron Golem behavior is unchanged from vanilla in every way.
- Server-only deployable in V1 (vanilla clients work without the mod).
- Configurable stats, healing, and per-tier anger behavior.
- Clean uninstall: no world corruption; reversible on reinstall.
- Father/son friendly: ship small, ship often, each new build is a visible step forward for Charles.

**Non-goals (for now)**

- New entity types. Every golem in this mod is a vanilla `IronGolemEntity` with a variant tag attached. No entity registration, no spawn eggs.
- New blocks, items, or recipes outside the golem creation patterns. Tiers reuse vanilla blocks and ingots (copper block + copper ingot, etc.).
- A custom GUI / config screen. Config is a JSON file edited by hand for V1.
- Compatibility shims for other mods that retexture or modify Iron Golems. We will document the mixin targets but not actively integrate.

## 3. Scope (release phases)

| Phase | Contents |
|---|---|
| **V1 (MVP)** | All 6 tiers as Iron Golem + variant tag; T-shape creation recipes; stat scaling; material-specific drops; healing with matching ingot; basic config (`allow_golem_healing`, per-tier `max_health`/`attack_damage`/`anger_on_hit`). No client texture — all variants visually appear as iron golems. |
| **V2** | Client source set for variant textures. All five special abilities: copper-lightning-heal, gold-speed, emerald-villager-heal, diamond-lightning-strike (passive aura + on-attack swing with shared 30–60s cooldown), netherite-fireproof + ignite-on-hit. |
| **V3** | Village natural-spawn variant weighting. Iron golems spawned by the village mechanic roll a variant via configurable weights. Default weights and a Charles preset (see §8). |

Each phase ships independently. V1 is the immediate target.

## 4. Architecture

**One Fabric mod.** Toolchain: Java 25, Fabric Loom (1.16-SNAPSHOT per the official Fabric 26.1.2 example mod, or 1.16.1 stable — whichever the example mod currently pins), Minecraft 26.1.2, Fabric API 0.148.0+26.1.2.

**Mappings note.** As of Minecraft 26.1, the game is no longer obfuscated and Yarn is no longer officially supported — we use **Mojang official mappings** for class and method names. All Java class names in this design (`IronGolem`, `CarvedPumpkinBlock`, etc.) refer to the Mojang official names in 26.1.2, not Yarn-era names. Exact symbol names (e.g., is it `IronGolem` or `IronGolemEntity`? which method does the pumpkin pattern check live in?) will be confirmed during the **Source Inspection Spike** that is the first task in the V1 implementation plan.

**Build template note.** We will **not** blindly copy signport's `build.gradle` — signport predates 26.1 and may use pre-26.1 Loom/remap conventions (e.g., `modImplementation`, `remapJar` configuration) that are no longer the recommended path. Instead, V1 starts from the **official Fabric 26.1.2 example mod** (`net.fabricmc.fabric-loom` plugin, plain `implementation`/`compileOnly`, standard `jar` task) and selectively ports useful verification tasks from signport *after modernizing them*. Useful signport tasks to port: `checkChangelog`, `checkWrapperChecksum`, `checkUnitTestCompanions`. Signport-specific source-hygiene checks (port-sign mixins, BlueMap, etc.) are not relevant and will be dropped.

**Mod identity**

- Mod id: `multigolem`
- Java package: `dev.charles.multigolem`
- Archives base name: `multigolem`

**Source set layout**

- V1: `src/main/java` only. Server-side functional.
- V2: adds `src/client/java` via `loom.splitEnvironmentSourceSets()` for texture/render code.

**Core idea: variant-as-attachment, no new entity types**

Every golem (vanilla, created by player, or village-spawned) is a vanilla `IronGolem` entity (Mojang name; previously known as `IronGolemEntity` under Yarn). Each entity carries an optional `GolemVariant` attached via Fabric's `AttachmentType` API. The attachment persists with the entity's NBT automatically. An entity with no attachment is treated as `IRON` (so vanilla worlds and pre-mod golems work without migration).

**Variant persistence:** the attachment's codec stores the variant as a **stable lowercase string identifier** (`"copper"`, `"iron"`, `"gold"`, `"emerald"`, `"diamond"`, `"netherite"`) — never as an enum ordinal. This protects us against reordering or insertion of new variants in V2+ (e.g., adding `obsidian` between `iron` and `gold` would shift ordinals and silently corrupt saved data). Unknown identifiers on load are treated as `IRON` and logged once per session.

**Why this approach**

- Vanilla clients work — they only see iron golems on the wire.
- No new entity registration, spawn-egg plumbing, or model boilerplate.
- Village spawning (V3) is a single hook on iron golem spawn.
- Texture in V2 is a render mixin keyed on the variant tag.

**Stat application — strategy with explicit fallback**

**Primary plan (preferred):** live-computed stats via a narrow mixin. We mixin into the iron golem's max-health and attack-damage attribute reads and, when the entity carries a non-IRON variant, return the variant's configured value. Stats live entirely as code + config. Nothing extra is written to NBT. Uninstalling cleanly reverts stats on the next load (with the netherite-HP-clamp side effect documented earlier).

**Fallback (if the mixin target is hot or unstable):** if the only viable interception point is a base method called every tick for every entity, switch to **deterministic transient attribute modifiers**: when a variant attachment is set or loaded, apply `AttributeModifier`s for max health and attack damage with a stable per-variant UUID (`Operation.ADD_VALUE`, with values calculated to land on the configured target after vanilla's base). On attachment clear/unload we remove them by UUID. The modifiers are not persisted to NBT (we re-apply them on load from the variant tag — variant remains the source of truth). Uninstall behavior is the same as the live-computed path: variant tag becomes orphaned, modifiers aren't re-applied, stats revert.

The implementation plan's first spike will benchmark/inspect the mixin target before committing to the primary plan. The chosen path will be documented in the V1 changelog so we can revisit if other mods conflict.

On spawn (player-built or V3 village-spawned), we explicitly call setHealth() to the variant's max so the golem starts at full health.

**Drops & healing** are handled in the same mixin class by reading the variant tag.

**Anger** is handled by intercepting the retaliation path (the vanilla iron golem code that calls `setAngryAt`/`setTarget` in response to being attacked by a player) and skipping it when the tier's `anger_on_hit` is `false`.

## 5. Components (V1)

| Component | Path | Job |
|---|---|---|
| `MultiGolem` | `dev/charles/multigolem/MultiGolem.java` | Mod entrypoint. Registers the attachment type, loads config on server start, logs version banner. |
| `GolemVariant` | `dev/charles/multigolem/GolemVariant.java` | Enum of the 6 tiers. Each carries: body-block `Block`, healing ingot `Item`, drop `Item`, and (V2+) ability metadata. Provides `fromBodyBlock(Block)` and `fromIngot(Item)` lookups. |
| `GolemVariantAttachment` | `dev/charles/multigolem/GolemVariantAttachment.java` | `AttachmentType<GolemVariant>` registration with a `Codec` so it persists with entity NBT. |
| `MultiGolemConfig` | `dev/charles/multigolem/config/MultiGolemConfig.java` | Loads and saves `config/multigolem.json`. Holds `allow_golem_healing` and per-tier stat/anger overrides. Validates and clamps values on load. Has `*Config` suffix → unit-test companion required (per signport's `checkUnitTestCompanions` convention). |
| `GolemStatsResolver` | `dev/charles/multigolem/stats/GolemStatsResolver.java` | Pure helper: given a `GolemVariant` and a `MultiGolemConfig`, returns the effective `max_health`, `attack_damage`, `anger_on_hit`. Centralized so mixins stay thin. `*Resolver` suffix → unit-test companion required. |
| `GolemCreationHandler` | `dev/charles/multigolem/spawn/GolemCreationHandler.java` | Given the T-pattern's body block, maps to a `GolemVariant`, spawns an iron golem entity, attaches the variant, and sets HP to the variant's max. |
| `IronGolemMixin` | `dev/charles/multigolem/mixin/IronGolemMixin.java` | (Mojang name — confirm during spike.) Live-computes max health & attack damage from variant (or applies/removes deterministic transient modifiers, per the fallback in §4); intercepts healing/right-click to allow matching-ingot heal; intercepts retaliation for the `anger_on_hit` toggle; intercepts death drops to swap iron for the variant's ingot **if** the loot-table path proves infeasible (see §6.3). |
| `CarvedPumpkinBlockMixin` | `dev/charles/multigolem/mixin/CarvedPumpkinBlockMixin.java` | (Class name pending source spike — see §6.2.) Replaces vanilla's iron-block-only pattern check with a check that matches any of the 6 supported body blocks; on match, delegates spawn to `GolemCreationHandler`. |
| `multigolem.mixins.json` | `src/main/resources/multigolem.mixins.json` | Mixin config declaring the two mixin classes. |
| `fabric.mod.json` | `src/main/resources/fabric.mod.json` | Mod metadata. Lists entrypoints (`MultiGolem::onInitialize`), mixin file, dependencies. Credits Charles. |
| `multigolem.json` (default config) | written to `config/` on first run | See §7. |

## 6. Tier specifications

### 6.1 Stats (V1 defaults)

| Tier | HP | Avg Atk Dmg | Notes |
|---|---|---|---|
| Copper | 60 | 8.5 | Weakest. ~2–4 hits to kill a zombie; takes ~10–20 hits from a zombie depending on difficulty. |
| Iron | 100 | 14.0 | **Vanilla — never modified.** |
| Gold | 130 | 22.5 | Mid-tier. |
| Emerald | 200 | 40.0 | Tough. |
| Diamond | 350 | 62.5 | Very tough. |
| Netherite | 600 | 85.0 | Designed to kill a Warden in ~6 hits and survive a 1v1. |

Vanilla iron golem damage is rolled per swing via `attackDamage * 0.5 + random(0, attackDamage)` — so the listed value is the *upper midpoint*, not a fixed hit. We keep that exact jitter pattern by letting vanilla's roll logic apply on top of our configured value. So Netherite's listed 85 will roll roughly between **~43 and ~128 per swing**, with a typical hit around 85; Iron's 14 rolls ~7.5–21.5 (unchanged from vanilla). The §6.1 column is labeled "Avg Atk Dmg" to reflect this.

All values are overridable via config.

### 6.2 Creation recipes

Identical to the vanilla iron golem T-pattern, swapping the body block. The head is always a carved pumpkin or jack-o-lantern (vanilla behavior).

| Variant | Body block |
|---|---|
| Copper | `minecraft:copper_block` |
| Iron | `minecraft:iron_block` (vanilla) |
| Gold | `minecraft:gold_block` |
| Emerald | `minecraft:emerald_block` |
| Diamond | `minecraft:diamond_block` |
| Netherite | `minecraft:netherite_block` |

**Required first task — Source Inspection Spike.** Before writing `GolemCreationHandler` or `CarvedPumpkinBlockMixin`, the first implementation task is to decompile/inspect the Minecraft 26.1.2 source (via Loom's `genSources` output) and **prove** the exact class and method that performs the iron golem creation check today. Document findings in `docs/26.1.2-mojang-targets.md`. Specifically: which class holds the pattern check (`CarvedPumpkinBlock`? a separate `IronGolem.SPAWN_PATTERN` definition? something else?), which method we mixin into, and what signature it has. The design here doesn't change — only the concrete target name and signature do.

### 6.3 Drops

On death, replace iron ingot drops with the variant's matching item. The vanilla iron golem also drops 0–2 poppies; we preserve this for all variants (poppies are universal — they are *the* iron golem flavor item).

**Implementation choice (decided during spike #2 of the implementation plan):**

- **Preferred path: data-driven loot tables** — provide a `multigolem` override of the iron golem loot table with a custom `LootCondition` that reads the `GolemVariant` attachment and selects the appropriate `LootPool`. This is mod-compatibility-friendly: other mods that also touch the iron golem loot table can coexist via Fabric's loot-table modification API.
- **Fallback: tightly scoped mixin** on the drop method — only used if 26.1.2 loot conditions can't cleanly read entity attachments (e.g., if `LootContext` doesn't expose the entity in a way our custom condition can consume). If we fall back, the V1 changelog will note this as a known compat tradeoff.

The implementation plan's loot spike will pick one and document the call. Either way, the spec for *what* drops doesn't change:

| Variant | Primary drop |
|---|---|
| Copper | `minecraft:copper_ingot` × 3–5 |
| Iron | `minecraft:iron_ingot` × 3–5 (vanilla) |
| Gold | `minecraft:gold_ingot` × 3–5 |
| Emerald | `minecraft:emerald` × 3–5 |
| Diamond | `minecraft:diamond` × 3–5 |
| Netherite | `minecraft:netherite_scrap` × 2–3 |

Counts mirror vanilla iron golem's `3–5 iron ingots` for tiers Copper through Diamond. Netherite is 2–3 because netherite scrap is rarer and 3–5 would meaningfully break the netherite economy; this can be tuned later.

### 6.4 Healing

When `allow_golem_healing` is `true` (default), right-clicking a golem with the *matching* ingot heals it by 25 HP (same as vanilla iron golem's iron-ingot heal). Wrong ingot, no effect (passes through to vanilla interact behavior — which, for any non-iron-ingot held item, does nothing).

| Variant | Healing ingot |
|---|---|
| Copper | `minecraft:copper_ingot` |
| Iron | `minecraft:iron_ingot` |
| Gold | `minecraft:gold_ingot` |
| Emerald | `minecraft:emerald` |
| Diamond | `minecraft:diamond` |
| Netherite | `minecraft:netherite_ingot` |

### 6.5 Special abilities (V2 — not built in V1)

Listed here for design completeness; implementation is V2.

| Tier | Ability |
|---|---|
| Copper | Lightning strike on a copper golem **heals** the golem (real-world flavor: copper is a conductor). Lightning damage component is nullified for this entity. |
| Iron | None (vanilla). |
| Gold | +25% movement speed compared to iron golem. |
| Emerald | When a villager is within 8 blocks, the emerald golem regenerates 1 HP every 2 seconds (passive heal). |
| Diamond | **Combined lightning ability with a 30–60s shared cooldown.** Two trigger paths share the same cooldown: (a) **passive**: when the cooldown is ready and a hostile mob is within 12 blocks, the golem calls a lightning bolt down on it; (b) **active**: when the cooldown is ready and the golem attacks an enemy, that swing additionally calls a lightning bolt on the target. Either path consumes the cooldown. Cooldown is rolled randomly in [30s, 60s] after each use. |
| Netherite | **Fire/lava immune.** **On-hit ignite:** the golem's melee attacks set the target on fire for 5 seconds. |

## 7. Configuration

**File:** `config/multigolem.json` — written with defaults on first server start if missing.

```json
{
  "allow_golem_healing": true,
  "tiers": {
    "copper":    { "max_health":  60, "attack_damage":  8.5, "anger_on_hit": true },
    "iron":      { "max_health": 100, "attack_damage": 14.0, "anger_on_hit": true },
    "gold":      { "max_health": 130, "attack_damage": 22.5, "anger_on_hit": true },
    "emerald":   { "max_health": 200, "attack_damage": 40.0, "anger_on_hit": true },
    "diamond":   { "max_health": 350, "attack_damage": 62.5, "anger_on_hit": true },
    "netherite": { "max_health": 600, "attack_damage": 85.0, "anger_on_hit": true }
  }
}
```

**Per-tier `anger_on_hit`:** when `false`, that tier's golems will not retaliate against a player who attacks them (skips the iron golem retaliation path). Other anger sources (e.g., villager defense) are unchanged.

**Validation rules**

- Missing top-level fields → fill with in-code defaults, log INFO.
- Missing per-tier fields → fill with defaults for that tier, log INFO.
- `max_health` outside `[1, 2048]` → clamp, log WARN.
- `attack_damage` outside `[0.0, 2048.0]` → clamp, log WARN.
- Malformed JSON → load all in-code defaults, log WARN, keep playing. Do not overwrite the user's file (so they can fix typos).

Config is loaded once at server start and held in memory. No hot-reload in V1.

### 7.1 V3 additions (preview, not in V1)

```json
"village_spawning": {
  "enabled": true,
  "weights": {
    "iron": 50, "copper": 20, "gold": 10, "emerald": 14, "diamond": 5, "netherite": 1
  }
}
```

Weights are integer ratios — they don't need to sum to 100, the code will normalize. Charles's preset (commented as `_charles_preset` for documentation): all 19 except netherite at 5 — i.e., `iron: 19, copper: 19, gold: 19, emerald: 19, diamond: 19, netherite: 5`.

## 8. Data flows

### 8.1 Creating a golem (player-built)

1. Player places a carved pumpkin on the top of a T-shape made of 4 body blocks (1 center + 2 arms + 1 base) of a supported material.
2. `CarvedPumpkinBlockMixin` recognizes the pattern. Calls `GolemVariant.fromBodyBlock(body)`; if present, hands the spawn off to `GolemCreationHandler`.
3. Handler spawns a vanilla `IronGolemEntity`, attaches `GolemVariant.X` via the attachment API, sets HP to the variant's configured max, removes the 4 body blocks + pumpkin as vanilla does.
4. Subsequent calls to `getAttributeValue(MAX_HEALTH)` / `(ATTACK_DAMAGE)` return the variant's values via mixin.

### 8.2 Combat: golem attacks

1. Vanilla `IronGolemEntity#tryAttack` runs. It reads `getAttributeValue(ATTACK_DAMAGE)`.
2. Our mixin intercepts that read; if the entity is non-IRON, returns the variant's `attack_damage`.
3. Vanilla applies the ±jitter and damages the target.
4. (V2) If the variant is netherite, post-hit injection sets target on fire for 5s. If diamond and cooldown ready, also spawns lightning on the target and resets the cooldown.

### 8.3 Combat: golem is attacked

1. Vanilla retaliation logic fires (attacker becomes the golem's target).
2. Our mixin intercepts; if the variant's `anger_on_hit` is `false`, cancel the retaliation. The damage itself still applies.

### 8.4 Healing

1. Player right-clicks golem with an item.
2. Mixin checks: `allow_golem_healing && heldItem == variant.healingIngot`.
3. If yes: heal 25 HP, decrement stack by 1 (unless creative), play vanilla heal effect.
4. If no: fall through to vanilla interact (which, for non-iron-ingot held items, does nothing).

### 8.5 Death and drops

1. Vanilla iron golem death path runs.
2. Drops are determined per §6.3 — preferred via a data-driven loot table override with a custom `LootCondition` reading the variant attachment; fallback via a tightly scoped drop-method mixin. Either way, non-IRON variants drop the variant's primary item (count 3–5, or 2–3 for netherite scrap) plus vanilla's 0–2 poppies; IRON drops vanilla 3–5 iron ingots + poppies.
3. Entity is removed normally.

### 8.6 World load with pre-existing iron golems

1. Vanilla iron golem loads with no MultiGolem attachment.
2. Mixin reads attachment → absent → treats as `GolemVariant.IRON`.
3. Vanilla stats (100 HP, normal damage) apply. No change observed.

## 9. Error handling

- **Config file missing** → write defaults; log INFO.
- **Malformed JSON** → fall back to in-code defaults entirely; log WARN; do not overwrite user's file.
- **Out-of-range config values** → clamp to safe range; log WARN with field name and clamped value.
- **Unknown variant in attachment NBT** (e.g., a future tier name loaded by an older mod build) → treat as `IRON`; log WARN once per session.
- **Pre-existing entity, no attachment** → treat as `IRON`. No migration.
- **Crash safety** — none of the mixins should swallow exceptions silently; they should `throw` so Fabric's crash report captures them. Validation failures during config load do not throw.

## 10. Testing

**Unit tests** (`src/test/java`, JUnit 5 like signport):

- `GolemVariantTest` — body-block→variant lookup, ingot→variant lookup, round-trip via codec.
- `MultiGolemConfigTest` — load defaults, parse valid config, reject malformed JSON (returns defaults, doesn't overwrite), clamp out-of-range, partial config fills missing fields.
- `GolemStatsResolverTest` — for each variant and a known config, returns expected `max_health` / `attack_damage` / `anger_on_hit`. Overrides take precedence over baked defaults.

**Gradle conventions inherited from signport**

- Keep the `checkUnitTestCompanions` task; it requires that any class matching `*Config`, `*Resolver`, `*State`, `*Format` has a corresponding `*Test`. Our `MultiGolemConfig` and `GolemStatsResolver` satisfy this.
- Keep `checkWrapperChecksum`, `checkSourceHygiene`, and `checkChangelog` adapted to MultiGolem's files. Source-hygiene patterns specific to signport (port-sign mixins, BlueMap, etc.) will be removed; the wrapper checksum and changelog gates remain as-is.

**Manual playtest checklist** (lives in `docs/playtest-checklist.md`, created with V1):

- [ ] Build a copper golem from copper blocks + pumpkin; verify it spawns and starts at 60 HP.
- [ ] Repeat for gold, emerald, diamond, netherite.
- [ ] Existing pumpkin + 4 iron blocks still creates a vanilla iron golem with 100 HP.
- [ ] Copper golem vs zombie on Hard: takes ~10 hits to die; kills the zombie in ~2–4 swings.
- [ ] Netherite golem vs Warden: kills warden in ~6 hits; netherite survives with HP remaining.
- [ ] Each variant drops its matching ingot/gem (3–5; netherite scrap 2–3) plus 0–2 poppies on death.
- [ ] Healing: each variant accepts its own ingot for +25 HP; rejects other ingots.
- [ ] `allow_golem_healing: false` → no ingot heals any golem.
- [ ] `anger_on_hit: false` for copper → attacking a copper golem does not aggro it.
- [ ] Load a saved world that contains a pre-existing iron golem; verify it functions normally as IRON variant.
- [ ] Uninstall the mod → server boots; existing variant golems load as plain iron golems with 100 HP (clamped); no crashes.
- [ ] Reinstall the mod → variant golems regain their tier stats and drops.

## 11. Build & release plumbing

**Foundation: official Fabric 26.1.2 example mod.** `build.gradle`, `settings.gradle`, `gradle.properties`, and the `gradle/` wrapper directory are bootstrapped from Fabric's current example mod template (Mojang-mappings, modern Loom flow with plain `implementation`/`compileOnly` and standard `jar`, no remap-era conventions). Pin: `minecraft_version=26.1.2`, `loader_version=0.19.2`, `loom_version=1.16-SNAPSHOT` (or current stable pin matching the template), `fabric_api_version=0.148.0+26.1.2`.

**Selectively port from signport, after modernizing each task:**

- ✅ `checkChangelog` — generic, port as-is.
- ✅ `checkWrapperChecksum` — generic, port as-is.
- ✅ `checkUnitTestCompanions` — port the pattern, adjust file globs (we want tests for `*Config`, `*Resolver`).
- ✅ `releaseReminder` — generic, port as-is.
- ❌ `checkSourceHygiene` — signport's version checks port-sign mixin specifics and `.location()` API drift; **do not copy**. Write a fresh MultiGolem-specific hygiene check after V1 ships (or skip entirely until needed).
- ✅ `scripts/upload-modrinth.ps1`, `scripts/upload-curseforge.ps1` — generic upload scripts, port as-is with project IDs swapped.
- ✅ `.github/workflows/build.yml`, `release.yml` — port with project name and IDs swapped; keep the pinned-SHA action references and minimal-permissions blocks.

**Bootstrapped files for V1:**

- `CHANGELOG.md` with `## Unreleased` so the changelog gate passes from day one.
- `docs/modrinth-listing.md` and `docs/curseforge-listing.md` drafted before V1 release.
- `docs/26.1.2-mojang-targets.md` — output of the source-inspection spike. The exact class/method names mixins target. Updated whenever Minecraft updates.

These are release-time concerns; the V1 implementation plan will set them up as the final step before tagging `v0.1.0+mc26.1.2`.

## 12. Codex review — incorporated decisions

Codex reviewed an earlier draft. Findings have been folded into the spec; the resolutions are summarized here for traceability.

1. **Tooling for 26.1+** — Yarn references removed. Mojang official mappings are the source of truth. Toolchain language in §4 updated. ✅
2. **Don't blindly copy signport's Gradle** — §4 and §11 updated to bootstrap from Fabric's 26.1.2 example mod, then selectively port modernized signport tasks. List of which tasks port and which don't is in §11. ✅
3. **Mojang names in implementation** — class references updated (`IronGolem`, not `IronGolemEntity`). Exact symbol names confirmed by Source Inspection Spike (§6.2). ✅
4. **Creation mixin needs a spike first** — Source Inspection Spike added as the **first task** of the V1 implementation plan (§6.2). Output: `docs/26.1.2-mojang-targets.md`. ✅
5. **Identifier persistence, never ordinal** — Variant codec persists stable lowercase strings (§4). ✅
6. **Loot tables preferred, mixin only as fallback** — §6.3 documents the decision criteria and the V1 plan's loot spike. ✅
7. **Attribute strategy: primary + fallback** — §4 names the live-computed mixin as the primary plan and deterministic transient modifiers (variant remains source of truth) as the explicit fallback if the mixin target is hot/unstable. The spike will choose. ✅

**Remaining open questions** (for the implementation plan to resolve, not blockers for moving forward):

- Anger interception point — cleanest surface (cancel retaliation in `wasHurtBy`-equivalent vs. flag/shim). Implementation-plan-level decision after looking at the source.
- Performance baseline — if the live-computed mixin is the chosen path, add a smoke benchmark or at minimum a `// hot-path note` comment with measured timing.

---

*End of design. V1 implementation plan written next via the writing-plans skill.*
