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

### Vanilla asset templates need a provenance sidecar

`build-inputs/textures/iron_golem.template.png` is a verbatim copy of Mojang's vanilla iron golem texture. It's committed as a build input only; the generated per-tier textures are the deliverables that ship in the mod jar. The sidecar `LICENSE-AND-PROVENANCE.md` records:

- Source jar reference
- SHA-256 hash of the template
- Mojang EULA note (do not redistribute the template itself)

When you extract a new vanilla asset for any V3+ work, do the same.

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
- `README.md` — public roadmap + "Where to pick up" pointers
