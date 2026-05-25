# MultiGolem — Lessons Learned

Captured across V1 (v0.1.0) and V2 (v0.2.0 + v0.2.1) implementation. Read this before starting V3, V4, V5, or any future phase. Every item below cost real time the first time we learned it — read once, save a day later.

---

## Process lessons

### Spike first, every phase

V1's source-inspection spike caught:

- `IronGolem` moved package from `net.minecraft.world.entity.animal.IronGolem` to `net.minecraft.world.entity.animal.golem.IronGolem` in 26.1
- `ResourceLocation` was renamed to `Identifier` in 26.1
- Vanilla iron golem `ATTACK_DAMAGE` is `15.0`, not `14.0` as the spec assumed

V2's spike caught:

- **The vanilla Copper Golem already exists in MC 26.1.2** — scope-affecting discovery that drove the "coexistence" design (single block = vanilla copper golem; T-shape = our copper variant)
- Renderer pipeline changed: `getTextureLocation(state)` takes a render-state object, not the entity
- `JAVA_25` mixin compatibility level is available (don't fall back to `JAVA_21` unnecessarily)
- `Mob#setTarget` is the right unified veto point for targeting (catches both proactive `NearestAttackableTargetGoal` and reactive `HurtByTargetGoal` paths)

Without these spikes, we'd have written code against assumed APIs and crashed at startup. The spike doc (`docs/26.1.2-mojang-targets.md`) is the canonical API reference for this project. **Re-run the spike at the start of every phase and append findings to that doc.**

Bake spike findings into every subsequent implementation prompt. Don't make subagents re-derive Mojang names — give them the answers up front.

### Codex review in three rounds catches different categories of issues

| Round | Reviewing | Caught (sampling) |
|---|---|---|
| 1 | Architecture sketch | ALLOW_DAMAGE event vs LightningBolt mixin choice, attachment-strategy alternatives, lava-walking deferral |
| 2 | Written spec | Lossless config migration (preserving unknown fields), renderer mixin failure mode, diamond cooldown precondition gaps |
| 3 | Precision pass on revised spec | Dying-target / cooldown-preservation contradiction, atomic-move fallback edge case, spike specifics |

Three rounds is the right cadence for a sizable phase. One round would have shipped real bugs. For tiny phases (e.g., a single bug fix), one round is fine — but never skip review entirely.

### Subagent reliability is patchy on Fabric/Windows work

Across V1 + V2 implementation:

- About 50% of subagent dispatches completed cleanly (file work + commit + push + structured report)
- The other 50% stalled before the commit step — controller closed them out inline

Patterns that helped:

- **Pre-baked APIs in the prompt** (FQNs, method signatures, descriptors). Subagents that had to investigate APIs themselves stalled.
- **Tight scope per task.** Tasks above ~20 tool calls had a much higher stall rate.
- **Mandatory structured report at the end of the prompt.** Helped with the truncation-mid-report problem.
- **Long-running gradle invocations are the worst.** First-time `./gradlew build` (10+ min) and `./gradlew genSources` (3–10 min) caused most stalls. Run those inline from the controller; dispatch subagents only after caches are warm.

**Inline execution is often faster than fighting subagents on complex tasks.** If a task has investigation, novel API surface, or a long-running command, just do it directly.

### Reviews catch behavior edges better than syntax

V3.1 permissions compiled and built cleanly, but review still caught the real bugs:

- Full-health golems should no-op before permission checks. A denial message on an action that cannot actually heal is noisy and changes vanilla-feeling behavior.
- Recognized but mismatched ingots must not fall through to vanilla `IronGolem#mobInteract`. Because every MultiGolem variant is still an `IronGolem`, letting an iron ingot fall through on a damaged non-iron variant can heal and consume the item through vanilla, bypassing the tier permission entirely.
- `ThreadLocal` context helpers should save and restore prior state, not only remove in `finally`. Nesting is not expected in the vanilla placement path, but modded/mixin surfaces are safer when reentrant calls restore the outer context.

Builds catch descriptors and compilation. Focused review catches "this compiles but changes player behavior."

### Honest scope checks > running out of context

V1 spanned multiple sessions deliberately. V2 paused at Task 2 with a clean handoff doc rather than failing mid-task. Each pushed commit is a resumable checkpoint.

Pattern: when context is more than ~40% consumed and a substantial task is ahead, stop at a clean boundary and hand off rather than start the task. Hand-off prompts are cheap; mid-task crashes are expensive.

### Kid-friendly language improved the design itself

When Charles couldn't follow "list of FQN strings vs named modes" we landed on `ALL_HOSTILE_MOBS` / `CREEPERS_ONLY`-style enum naming. That ended up being genuinely better config UX for everyone, not just kids.

Forcing plain-language explanations exposed jargon that would have hurt server admins too. **When a design decision is hard to explain to an 8-year-old, the design is probably wrong, not the explanation.**

### Player-facing CHANGELOG voice

The audience is players and server admins, not developers. See the `0.2.1+mc26.1.2` entry as the template:

```
- Fixed Gold, Emerald, Diamond, and Netherite golems reverting to 100 HP
  after chunk unloads or server restarts.
- Refined variant textures so Gold looks brighter and more golden,
  Emerald looks greener, Diamond no longer looks dirty, and Netherite
  has darker material variation with lava cracks visible from
  multiple sides.
```

Not:

```
- Fixed VariantAttributes.apply not being called on ServerEntityEvents.ENTITY_LOAD
- Adjusted generate-textures.py HSL shift parameters per tier
```

The implementation detail goes in the commit message; the CHANGELOG entry is for the people who run the mod.

---

## Technical lessons

### Mixin `@Inject` does NOT walk the class hierarchy

This is documented in `docs/26.1.2-mojang-targets.md` under "Inherited methods — mixin gotcha" because it cost a debug session in V1. Read that section before writing any new mixin.

Rule: target the **declaring** class, not the convenient subclass. Filter at runtime with `instanceof`:

```java
@Mixin(LivingEntity.class)
public abstract class GolemAngerMixin {
    @Inject(method = "setLastHurtByMob", at = @At("HEAD"), cancellable = true)
    private void multigolem$filterAnger(LivingEntity attacker, CallbackInfo ci) {
        if (!(((Object) this) instanceof IronGolem self)) return;
        // ...
    }
}
```

The error you get when you do it wrong is `Critical injection failure: ... could not find any targets matching '<method>' in net/minecraft/world/entity/animal/golem/IronGolem`. Don't trust the "No refMap loaded" line in the log — the real cause is almost always this.

### Never trust assumed vanilla values

V1's spec had iron golem `ATTACK_DAMAGE` as 14.0. The spike caught it — vanilla is 15.0. We would have silently changed vanilla iron golem behavior without noticing.

**Always re-verify vanilla values from the decompiled source, even when documenting "unchanged from vanilla."**

### Layered vanilla AI is the rule, not the exception

Vanilla iron golem's `NearestAttackableTargetGoal` (proactive) already excludes creepers via a lambda. But `HurtByTargetGoal` (reactive) doesn't — it retaliates against anything that hit it, creepers included.

That's why `ignored_target_types` needed the unified `Mob#setTarget` veto rather than a goal-level filter. When considering a behavior change, **enumerate all the goals/event paths the change could need to intercept**, not just the obvious one.

### Required mixins fail the whole client; embrace fail-fast

V2 round-2 review pushed back on "graceful fallback to vanilla textures when renderer mixin fails to apply." Implementing optional mixins (`required: false` + per-injection `require = 0`) is messy and the silent-failure mode is worse than a clear startup error.

If the renderer mixin doesn't apply, the client should refuse to start with a clear Mixin error. Catch regressions in CI with `runClient` rather than letting players silently see plain iron golems.

### Config layer is harder than features — copy V2's

The V2 config layer took three Codex review rounds to nail down. Edge cases that matter:

- Non-finite doubles (NaN, +Infinity, -Infinity) — replace with defaults + WARN, don't propagate
- Explicit `JsonNull` handling (e.g., `copper_lightning_heal_amount: null` meaning "full heal") — call `isJsonNull()` before `getAsDouble()`
- Case-insensitive enum parsing with canonical uppercase write-back (so `"all_hostile_mobs"` becomes `"ALL_HOSTILE_MOBS"` on disk)
- Lossless `JsonObject` merge that preserves user-added unknown fields (e.g., `"_user_note": "..."` or third-party mod settings)
- Atomic write via tmp file + `ATOMIC_MOVE`, with a documented fallback (we chose "leave file untouched, retry next startup" rather than non-atomic copy)

For V3+, copy `MultiGolemConfig` and `TierStats` patterns rather than rebuilding the validation layer. The shape is known and tested.

### Existing-in-vanilla discoveries can scope-change a phase

V2's spike turned up that vanilla Copper Golem exists in 26.1.2 — a feature the V1 spec hadn't accounted for. We had to add a coexistence design (different recipe shapes for vanilla vs our variant).

Before writing the design for any phase, **grep for the feature in vanilla source first.** Saves a brainstorming round.

### Permission gates must distinguish denial from no-op

V3.1 added permissive-by-default permission gates for player-built creation and ingot healing. The subtle part was not node strings; it was preserving vanilla/no-op behavior around the denied action:

- Check permissions only when the action would actually do something.
- Full-health golem + matching ingot should return before permission checks: no denial, no `FAIL`, no consume, no sound.
- Unrecognized items should fall through normally.
- Recognized but mismatched healing ingots should cancel with `InteractionResult.PASS`, not `FAIL`, to prevent vanilla iron healing while avoiding a denial message for the wrong item.
- Actual permission denial should use `InteractionResult.FAIL` before heal, sound, or item consumption.

Rule: for permission work, first classify the interaction as "not our action," "our action but no-op," or "our action and would mutate state." Only the last category should ask the permissions provider.

### Document intentional ungated paths next to the branch

Some ungated paths are not omissions. In V3.1:

- Iron T-pattern creation is vanilla-owned and must not get a `multigolem.create.iron` gate.
- Missing player context during creation is ungated by design. Dispenser, piston, command-block, or unsupported placement paths should keep current behavior instead of guessing a responsible player.
- Village natural spawns, commands, spawn eggs, mob spawners, existing golems, drops, stats, abilities, targeting, and anger behavior stay outside V3.1 permission scope.

Put short comments beside the branch that enforces the exception. Future contributors are less likely to "fix" an intentional hole if the reason is in the code.

### Modern 26.1 Loom uses plain `implementation`

The V3.1 permissions plan initially said `modImplementation include("me.lucko:fabric-permissions-api:0.7.0")`, but this repo's 26.1 Loom setup uses plain `implementation` for Fabric Loader and Fabric API. Gradle rejected `modImplementation`; the working shape is:

```groovy
implementation include("me.lucko:fabric-permissions-api:0.7.0")
```

This still nests the permissions API in the jar. When adding future Fabric-side dependencies, match the repo's existing dependency style first, then verify with `compileJava`, `build`, and a startup smoke if mixins/runtime loading are involved.

### Revue review bridge replaced agent-review-bridge

The local review bridge is now Revue:

- Skill: `revue-bridge`
- Review gate wrapper: `superpowers-review-gates`
- Worker CLI: `revue-worker`
- MCP server/tools: `revue-ledger`
- Claude reviewer adapter name: `claude-code`

Do not create new review requests with the old reviewer name `claude`; Revue's worker will not claim them. If that happens, cancel the request as an audited attempt and create a new request with `reviewer="claude-code"`.

Before spending Claude budget, run:

```powershell
revue-worker doctor --cwd "<repo-or-worktree>" --trusted-roots "<trusted-root>"
```

Then show the operator surface before `revue-worker once`, including the dashboard link plus status/tail/cancel commands. If Revue packetizes a review, run the child packet review ID explicitly; the parent may stop at `packetized` with a pending packet. After actioning findings, update the Revue finding statuses so `revue-worker status` shows `0 unresolved`.

### Revue local ledger state must stay local

Revue writes request, result, prompt, transcript, dashboard, and lock files under `.agent-review/`. That directory is local process state, not project source. Keep `.agent-review/` ignored in `.gitignore`; never commit review ledger contents.

For V4 spawn egg planning, run the explicit guard before handing the plan to implementation:

```powershell
python scripts/check-v4-planning-handoff.py
```

The guard verifies `.agent-review/` is ignored, the V4 Revue follow-up notes exist in `docs/26.1.2-mojang-targets.md`, and, once a V4 spawn egg plan exists, that it includes the Revue planning guardrails.

### Revue findings should become plan constraints, not vague memory

The V4 design-intent review found several issues that were not implementation bugs yet, but would become bugs if the plan omitted them. Record these as explicit plan requirements:

- Verify creative tab registration against integrated client, modded dedicated server, and vanilla client behavior. Do not assume client-only registration is enough.
- If item visuals select on exact `minecraft:custom_data`, the marked spawn egg stack factory must be the sole writer to that component.
- Permission-denied spawn egg use may still arm-swing because vanilla `spawnMob` returns `InteractionResult.SUCCESS`; use overlay denial as feedback and do not add brittle extra hooks just to suppress the swing.
- Marked spawn eggs must not call `setPlayerCreated(true)`; only T-pattern player-built golems get that ownership flag.
- Any spawner thread-local must be cleared with a guaranteed cleanup path.
- Spawner `setEntityId` and the marker write happen synchronously in one `useOn` call; document that there is no async save window between those injects.
- `BaseSpawner.setEntityId` mutates the existing `SpawnData.entity` compound. If a marked egg configures a spawner and a later unmarked vanilla iron golem egg reconfigures the same spawner, clear the `multigolem` marker or the stale variant will leak into future spawns.

### Zombie Golem planning guardrails should be mechanical

The Zombie Golem design intentionally stretches MultiGolem in places where a vague implementation plan would drift:

- Call zombie-village spawning a **desired-count maintenance rule**, not a spawn rate. The default is max 2 Zombie Golems per qualifying village area, with existing Zombie Golems counted against the cap.
- Zombie villagers are the eligibility anchor. One zombie villager is enough; regular zombies alone must never qualify an area. Regular zombies can only add bonus pressure when zombie villagers are already present.
- Generated abandoned villages are the player-facing target, but strict generated-structure filtering is not assumed. If the implementation wants "naturally generated abandoned villages only," it must first prove a clean source signal and return for design review before adding that restriction.
- Keep Zombie Golems inside the existing `IronGolem` + `GolemVariantAttachment` architecture unless a source spike proves that impossible.
- Set faction rules before coding targeting hooks: Zombie Golems attack players, villagers, wandering traders, vanilla Iron Golems, and non-zombie MultiGolems; they ignore zombies and zombie villagers; non-zombie golems fight Zombie Golems.
- Civilian conversion is the signature mechanic. Villagers and wandering traders become zombie villagers with 100% default chance, and the conversion hit should not also apply normal Iron Golem damage.
- The next plan must spike village-area maintenance, targeting/faction hooks, villager/trader conversion data preservation, Rotten Flesh healing, and V4 marked spawn egg/spawner behavior before implementation.

Mechanical gate:

```powershell
python scripts/check-zombie-golem-planning-handoff.py
```

The gate verifies the Zombie Golem spec and lessons exist. Once a Zombie Golem plan exists, it also checks that the plan carries the source-spike constraints and approved behavior markers forward.

### V4 planning guardrails should be mechanical

The V4 spawn egg planning session produced several process failures that should be caught by scripts, not remembered:

- Use absolute paths for new files in assigned worktrees, or immediately verify both the assigned worktree and parent checkout with `git status --short`. A relative patch can land in the parent checkout even after `git rev-parse --show-toplevel` was verified in the shell.
- Do not parallelize dependent file moves, especially copy-then-delete cleanup. Run those steps sequentially so the delete cannot win before the copy reads the source.
- If Revue MCP `review_create` times out but `revue-worker doctor` is healthy, check `revue-worker status` before retrying so duplicate requests are not created. If a temporary helper is needed, keep it under ignored `.agent-review/tmp/`, call the canonical `review_create`, and show `operator-links` before `revue-worker once`.
- Revue `review_unit` uses `"scope_basis": "explicit-files"` plus `recommended`, `user_choice`, and `reason`. Do not use the older ad hoc `type/files/notes` shape.
- For V4 spawn egg planning, `scripts/check-v4-planning-handoff.py` is the mechanical gate. It must keep checking the plan-only review fixes: exact `custom_data` selector verification, null-player spawner denial, existing `BaseSpawner.nextSpawnData` mutation, one entity per tick for the spawner thread-local, and vanilla-client icon fallback. The authoritative marker list lives in the script.

---

## Release plumbing lessons

### `sed signport → multigolem` missed `modern-signport`

The CurseForge upload script had a default slug `modern-signport`; sed turned it into `modern-multigolem`. Worked (the actual upload uses the project ID env var) but logged the wrong URL. **After any cross-project file-by-file port, audit each file by eye for residual references, not just grep.**

### Modrinth description sync requires 4-backtick fenced markdown

The first V1 release logged a warning because `docs/modrinth-listing.md` didn't wrap the description in 4-backtick fences. Took a real release to discover. The script's regex is:

```
'## Project Description\n+````markdown\n([\s\S]*?)````'
```

Now documented in `docs/modrinth-listing.md` shape. New listings copy that pattern.

### First CurseForge upload pends moderator approval

The release workflow succeeds (HTTP 200, file ID issued) but the file isn't publicly visible until a human approves the project. Don't panic-redeploy when the file doesn't appear on the project page within minutes — wait a few hours.

### Public docs drift every phase

V3.1 updated README and marketplace docs for permissions, but review caught stale public copy that still said village natural-spawn weighting was "arriving in V3" even after V3 had shipped.

Phase-close docs checks should include:

- README roadmap: completed phases marked done and the "next" marker advanced.
- Modrinth and CurseForge "Known Limitations": remove shipped limitations or replace them with current ones.
- CHANGELOG: user/admin-facing behavior, not implementation steps.
- Playtest checklist: new behavior plus negative-scope rows.

If a file is touched for a new phase, scan the surrounding public copy for stale phase language. Grep helps, but read the nearby section by eye.

### Vanilla asset templates need a provenance sidecar

`build-inputs/textures/iron_golem.template.png` is a verbatim copy of Mojang's vanilla iron golem texture. It's committed as a build input only; the generated per-tier textures are the deliverables that ship in the mod jar. The sidecar `LICENSE-AND-PROVENANCE.md` records:

- Source jar reference
- SHA-256 hash of the template
- Mojang EULA note (do not redistribute the template itself)

When you extract a new vanilla asset for any V3+ work, do the same.

### `addPermanentModifier`, not `addTransientModifier`, for stat overrides

Transient attribute modifiers are **not written to entity NBT**. When a chunk unloads and reloads, transient modifiers on `MAX_HEALTH` and `ATTACK_DAMAGE` silently disappear. Minecraft then clamps current health to the (now-vanilla) max, permanently damaging every golem whose configured HP exceeds the vanilla iron golem base of 100.

Copper was immune to this bug because its configured max health (60) is *below* vanilla base (100) — the clamp never fired.

Rule: any `AttributeModifier` that must survive entity save/load (which is all of them for stat overrides) must use `addPermanentModifier`. Only use `addTransientModifier` for effects that should intentionally reset on reload (e.g., a temporary potion effect you're managing manually).

### World-wide AABB entity scans hang the server

`world.getEntitiesOfClass(IronGolem.class, new AABB(-30_000_000, -64, -30_000_000, 30_000_000, 320, 30_000_000))` looks harmless but causes the entity section storage's `LongAVLTreeSet.subSet` to enumerate every section key across the entire world coordinate range. On a world with many loaded chunks this takes tens of seconds, triggering the server watchdog.

The stack trace signature to recognize it:

```
at it.unimi.dsi.fastutil.longs.LongAVLTreeSet.subSet(...)
at net.minecraft.world.level.entity.EntitySectionStorage.forEachAccessibleNonEmptySection(...)
at net.minecraft.world.level.entity.EntitySectionStorage.getEntities(...)
at dev.charles.multigolem.ability.SomeAbility.onTick(...)
```

The correct replacement for "give me all loaded entities of type X" with no spatial filter:

```java
world.getEntities(EntityTypeTest.forClass(IronGolem.class), e -> true)
```

`ServerLevel.getEntities(EntityTypeTest, Predicate)` is a no-AABB overload that walks only the loaded entity list. Note: `Level.getEntities()` (no args) is `protected abstract` and inaccessible outside the class hierarchy — use the `ServerLevel` overload.

### Asymmetric bug behavior is the fastest path to root cause

When a bug affects *some* variants but not others, the exempted variant is telling you exactly what's different. Don't start with the broken cases — start by asking "what is unique about the working case?"

In the HP-loss bug: copper was fine, all others were damaged. Copper is the only variant with configured HP below vanilla base. That one observation immediately ruled out every damage-source hypothesis and pointed directly to attribute modifier loss + health clamping — without reading a single line of ability code.

General pattern: if N things are broken and 1 is not, the working one is your control. Read its properties and find the one dimension where it differs from the broken set. The root cause lives on that dimension.

---

## What I'd do differently next time

- **Run all spikes inline, not via a subagent.** Investigation work is one of the worst fits for subagent dispatch.
- **Skip per-task two-stage review for tasks under ~50 lines.** Implementer's structured report is sufficient for trivial tasks; full review is for tasks where design judgment matters (mixins, novel APIs, anything touching the renderer or save/load).
- **Charles writes texture art prompts directly.** He's old enough and the art direction is more his than mine. Set up the format, then step out.
- **Bootstrap one prompt template per task type.** "Mixin task" / "ALLOW_DAMAGE listener task" / "Config-layer task" / "Tick-handler task" — each has known shape. Subagent success rate would be higher with templated prompts.

---

## Related docs

- `docs/26.1.2-mojang-targets.md` — canonical Mojang/Fabric API reference for this project. Re-spike and append findings each phase.
- `docs/superpowers/specs/2026-05-15-multigolem-design.md` — V1 design (and V3 scope sketch in §3, §6.1.1)
- `docs/superpowers/specs/2026-05-16-multigolem-v2-design.md` — V2 design (and the canonical example of three-round Codex review structure)
- `docs/superpowers/plans/2026-05-16-multigolem-v2.md` — V2 plan (canonical example of TDD-task plan style)
- `docs/playtest-checklist.md` — manual regression checklist; extend each phase
- `README.md` — public player/server-admin overview, gameplay docs, and config reference

## Where to pick up

For agentic workers (Codex, Claude, etc.) resuming work on this project:

Read this file first. Every item here cost real time the first time we learned it. Covers process patterns, technical gotchas, config-layer edge cases, and release plumbing details.

- **V3 starting point:** `docs/superpowers/specs/2026-05-15-multigolem-design.md` §3 (V3 scope) and §6.1.1 (config weights including the Charles preset). Follow the same brainstorm -> spec -> Codex review -> plan -> execute flow used by V1 and V2.
- **V4 starting point:** use the V4 spawn egg spike sections in `docs/26.1.2-mojang-targets.md`, especially the Revue follow-up section, then write the formal implementation plan before coding. The vanilla `SpawnEggItem` lives at `net.minecraft.world.item.SpawnEggItem`; the current V4 branch already source-spiked the normal egg path, item-model path, and spawner path.
- **V5 starting point:** no spec yet. Start with a fresh brainstorming session. The vanilla `CopperGolem` entity is at `net.minecraft.world.entity.animal.golem.CopperGolem` and its spawn flow is documented in `docs/26.1.2-mojang-targets.md` under the "Copper Golem already exists in vanilla 26.1.2" finding.
- **Zombie Golem starting point:** `docs/superpowers/specs/2026-05-25-multigolem-zombie-golem-design.md`, then run `python scripts/check-zombie-golem-planning-handoff.py` while drafting the implementation plan. Do not implement until the plan has spiked village-area maintenance, targeting/faction hooks, civilian conversion, Rotten Flesh healing, and V4 spawn egg/spawner reuse.

V3, V4, V5 are independent of each other after V3's natural-spawn ships. They can be released in any order, or interleaved with other work. The recommended order above (V3 -> V4 -> V5) honors the original V1 roadmap promise first.
