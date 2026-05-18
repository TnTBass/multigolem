# MultiGolem V1 Playtest Checklist

Run a Fabric server with this mod installed. Open a client and connect (modded or vanilla). Work through each row. A failed row blocks release.

## Creation

- [ ] Build T-pattern of copper blocks + carved pumpkin → copper golem spawns, 60 HP.
- [ ] Same with gold blocks → gold golem, 130 HP.
- [ ] Emerald → emerald golem, 200 HP.
- [ ] Diamond → diamond golem, 350 HP.
- [ ] Netherite → netherite golem, 600 HP.
- [ ] Iron blocks (vanilla) → iron golem, 100 HP. Vanilla behavior unchanged.
- [ ] Single copper block + pumpkin → vanilla copper golem (chest-bearer). MultiGolem does not interfere.
- [ ] Pumpkin on a single block (not in any recipe shape) → nothing happens (vanilla behavior).

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

## Vanilla client compatibility

- [ ] Run the modded server. Connect with a vanilla MC 26.1.2 client.
- [ ] Verify the vanilla client doesn't get kicked at login.
- [ ] Have a modded player create a copper-variant golem on the same world; verify the vanilla client sees it as an iron golem (appearance) but reacts correctly to its stats (e.g., dies in copper-golem hit counts, drops copper ingots when killed).

---

# V2 — v0.2.0

## Textures (modded client)

- [ ] Copper golem displays with copper-tinted skin.
- [ ] Repeat for gold, emerald, diamond, netherite.
- [ ] Iron golem displays as vanilla (no change).

## Textures (vanilla client)

- [ ] Vanilla client connects to V2 server; sees all variants as iron golems.
- [ ] Modded player and vanilla player share the same world; modded sees colors, vanilla sees iron — no desync.

## Copper lightning heal

- [ ] Stand near copper golem in thunderstorm OR strike with channeling trident — golem takes no damage AND heals.
- [ ] Set `copper_lightning_heal_amount: 0` — lightning still doesn't damage, but no heal either.
- [ ] Set `copper_lightning_immune: false` — lightning damages normally.

## Gold speed + vanity

- [ ] Gold golem moves visibly faster than iron.
- [ ] Sprint-dust particles emit when gold golem walks.
- [ ] Sunlight-shine particle appears when gold golem is stationary outdoors during day.
- [ ] No shine particle indoors or at night.

## Emerald villager aura

- [ ] Damage emerald golem; stand near a villager; verify heal every 2s.
- [ ] Move villager out of 8-block range; heal stops.
- [ ] Wandering trader nearby with `emerald_count_wandering_traders: true` — heal proceeds.
- [ ] Zombie villager nearby — no heal triggered.
- [ ] Particle emits with each heal tick.

## Diamond lightning

- [ ] Approach diamond golem with creeper at 12 blocks → no zap (creeper in `ignored_target_types`).
- [ ] Approach with skeleton at 12 blocks → zap; verify cooldown timing (30–60s before next).
- [ ] Hit something with diamond golem when cooldown ready → swing summons lightning.
- [ ] Block line-of-sight with a wall — passive zap doesn't fire through wall.
- [ ] Lightning bolt hits diamond golem — no damage (self-immunity).
- [ ] **Bystander immunity (default targeting):** spawn two diamond golems with a hostile mob (e.g., skeleton) near both, within passive aura range. Diamond A zaps the hostile. Verify (a) bolt hits hostile, not bystander diamond B; (b) incidental AoE lightning doesn't damage diamond B (self-immunity); (c) diamond B's cooldown is not consumed or reset; (d) no chain reaction.
- [ ] **Direct-hit immunity (forced):** use `/summon lightning_bolt ~ ~ ~` directly on a diamond golem. Verify (a) no damage; (b) cooldown unchanged.
- [ ] **On-attack with dying target (strict):** hit a mob whose HP < diamond golem's per-swing damage; vanilla swing kills it; verify on-attack lightning does NOT fire and cooldown is NOT consumed.
- [ ] **On-attack with filtered target:** have diamond golem swing at a creeper (in `ignored_target_types`); verify cooldown is NOT consumed.
- [ ] Set `diamond_target_mode: BOSSES_ONLY`; restart; verify normal hostiles aren't zapped.

## Netherite fire/lava + ignite

- [ ] Netherite golem stands in lava — no damage.
- [ ] Hit by ghast fireball — no damage.
- [ ] Magma block — no damage.
- [ ] Hit a mob — mob catches fire for 5 seconds.
- [ ] Hit a vanilla fire-immune mob (blaze) — no fire effect, no error.
- [ ] **Hit another netherite-variant golem** — no fire effect on target (netherite ignite skips netherite-variant targets).
- *Lava-walking is not in V2 — no row for this release.*

## `ignored_target_types`

- [ ] Default: copper/gold/emerald/diamond/netherite ignore creepers in melee.
- [ ] Default: iron attacks creepers (vanilla behavior preserved).
- [ ] Set `iron.ignored_target_types: ["CREEPERS"]` → iron now ignores creepers too.
- [ ] Unknown value in list (`"DRAGONS_AND_CASTLES"`) → warning logged at startup, value dropped, other entries preserved.

## Save/load + migration

- [ ] V1 config (no V2 fields) loads into V2 server; defaults appear in file after first run.
- [ ] World with active diamond cooldown saved, server restarted — cooldown still active when world loads.
- [ ] V2 client connects to V1 server — variant textures don't appear (V1 doesn't sync the attachment), no crash, no errors logged.

## Vanilla parity preserved

- [ ] Existing V1 playtest checklist (all rows) still passes — V2 adds, doesn't break.

---

# V3 — village natural spawns

## Village variant rolls

- [ ] Set `village_spawning.weights` to `{"iron":0,"copper":1,"gold":0,"emerald":0,"diamond":0,"netherite":0}`. Trigger a villager-called golem spawn. Copper variant spawns.
- [ ] Repeat forced one-at-a-time weights for Gold, Emerald, Diamond, and Netherite. Each target variant can spawn from villagers.
- [ ] Set `netherite: 1` and all other recognized weights to `0`. Villagers can spawn Netherite.
- [ ] Restore default weights. Over repeated village spawns, Iron, Copper, Gold, Emerald, Diamond, and Netherite are all possible outcomes; Diamond and Netherite are rare.

## V2 behavior on village-spawned variants

- [ ] Village-spawned non-iron variant starts at configured full health.
- [ ] Textures render correctly on a modded client.
- [ ] Healing with the matching ingot works.
- [ ] Variant-specific drops use the village-spawned variant.
- [ ] Copper lightning heal works.
- [ ] Gold speed and particles work.
- [ ] Emerald villager aura works.
- [ ] Diamond lightning behavior works.
- [ ] Netherite fire immunity and ignite-on-hit work.
- [ ] `ignored_target_types` applies.
- [ ] `anger_on_hit` applies.

## Scope and config negatives

- [ ] Village-spawned variants are natural defenders, not player-created golems.
- [ ] Existing golems do not change after upgrading to V3.
- [ ] `village_spawning.enabled: false` leaves villager-called spawns as Iron.
- [ ] Fully explicit all-zero weights leave villager-called spawns as Iron.
- [ ] Mob spawner iron golems do not roll variants.
- [ ] Spawn egg iron golems do not roll variants.
- [ ] Command-spawned iron golems do not roll variants.
- [ ] Malformed `weights` falls back to defaults and logs a warning.

---

# V3.1 - permissions

## Creation permissions

- [ ] With no LuckPerms or permissions provider installed, player-built Diamond and Netherite MultiGolem T-patterns still spawn normally.
- [ ] Denying `multigolem.create.diamond` prevents a player-built Diamond MultiGolem T-pattern from spawning.
- [ ] Denied Diamond creation leaves all T-pattern blocks intact.
- [ ] Denied Diamond creation shows `You do not have permission to create a Diamond golem.`
- [ ] Granting `multigolem.create.diamond` allows Diamond T-pattern creation.
- [ ] `multigolem.admin.bypass` allows Diamond creation even when `multigolem.create.diamond` is denied.
- [ ] Vanilla Iron golem T-pattern creation is unchanged and has no MultiGolem creation permission node.

## Healing permissions

- [ ] With no LuckPerms or permissions provider installed, ingot-based healing still works normally.
- [ ] Denying `multigolem.heal.netherite` prevents Netherite golem healing.
- [ ] Denied Netherite healing does not consume the netherite ingot.
- [ ] Denied Netherite healing does not play vanilla repair feedback.
- [ ] Denied Netherite healing shows `You do not have permission to heal a Netherite golem.`
- [ ] Granting `multigolem.heal.netherite` allows Netherite healing.
- [ ] Denying `multigolem.heal.iron` prevents vanilla Iron golem iron-ingot healing.
- [ ] `multigolem.admin.bypass` allows healing even when the tier-specific heal node is denied.

## Scope negatives

- [ ] Village natural spawns are not permission-gated.
- [ ] Command-spawned golems are not permission-gated.
- [ ] Spawn egg golems are not permission-gated.
- [ ] Mob spawner golems are not permission-gated.
- [ ] Existing golems, drops, stats, abilities, targeting, and anger behavior are unchanged.
