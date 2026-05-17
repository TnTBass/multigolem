# MultiGolem — Modrinth Listing

## Project Description

````markdown
MultiGolem adds five new Iron Golem variants — Copper, Gold, Emerald, Diamond, and Netherite — alongside the vanilla Iron Golem. Each variant has its own stats, drops, healing material, and a special ability.

- **Server-side functional.** Vanilla clients can connect to a server running MultiGolem with no mod installed.
- **Build them like an iron golem.** T-pattern of body blocks + a carved pumpkin. Swap the body block for copper, gold, emerald, diamond, or netherite blocks. Waxed and oxidized copper blocks work for the Copper variant.
- **Each tier scales.** Copper is the weakest; Netherite is strong enough to kill a Warden in ~6 hits.
- **Heal with matching ingots.** Copper golem? Copper ingot. Diamond golem? Diamond. Configurable.
- **Custom textures.** Each variant has a unique skin on modded clients; vanilla clients see iron golems as usual.

## Special Abilities

- **Copper — lightning heal:** lightning strikes heal the golem instead of damaging it.
- **Gold — speed + vanity:** +75% movement speed, sprint-dust particles while walking, sunlight-shine particles when stationary outdoors during the day.
- **Emerald — villager aura:** heals passively while any villager or wandering trader is within 8 blocks.
- **Diamond — lightning strike:** passive LOS zap of nearby hostiles (30–60s cooldown) + on-attack lightning. Self-immune to lightning damage. Creepers excluded from targeting by default.
- **Netherite — fire immunity + ignite:** fully immune to fire and lava damage; ignites any mob it hits for 5 seconds (skips other netherite golems).

## Configuration

Edit `config/multigolem.json`, created on first server start. Server admins can tune:

- Global ingot healing with `allow_golem_healing`
- Per-tier `max_health`, `attack_damage`, and `anger_on_hit`
- Per-tier `ignored_target_types` such as `CREEPERS`, `ENDERMEN`, `PLAYERS`, and `ALL_BOSSES`
- Copper lightning healing, Gold movement/particles, Emerald healing aura, Diamond lightning targeting/cooldowns, and Netherite fire immunity/ignite duration

Existing V1 config files migrate automatically to the V2 schema, and unknown fields are preserved where possible.

A father-and-son project. Built with Charles.
````
