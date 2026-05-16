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
