# MultiGolem — CurseForge Listing

## Features

- Adds five Iron Golem variants: Copper, Gold, Emerald, Diamond, and Netherite.
- Server-side functional: vanilla clients can connect to a server running MultiGolem with no mod installed.
- Build variants with the vanilla iron-golem T-shape using copper, gold, emerald, diamond, or netherite body blocks. Waxed and oxidized copper blocks work for the Copper variant.
- Marked vanilla iron golem spawn eggs for Copper, Gold, Emerald, Diamond, and Netherite variants.
- Stats scale by material; Copper is the weakest and Netherite is the strongest.
- Heal each variant with its matching ingot.
- Custom textures per variant on modded clients; vanilla clients still see regular iron golems.

## Special Abilities

- **Copper:** lightning strikes heal instead of damage.
- **Gold:** +75% movement speed, sprint-dust particles while moving, and sunlight-shine particles while idle outdoors.
- **Emerald:** heals passively while villagers or wandering traders are nearby.
- **Diamond:** passive LOS lightning zap of nearby hostiles, on-attack lightning, and lightning immunity. Creepers are excluded by default.
- **Netherite:** immune to fire and lava damage, and ignites hit mobs for 5 seconds.

Cross-tier `ignored_target_types` lets server admins stop tiers from targeting categories such as creepers, players, endermen, or bosses. Copper, Gold, Emerald, Diamond, and Netherite ignore creepers by default to prevent collateral block damage.

## Creation Recipes

Build a T-shape (1 base, 1 center, 2 arms) out of one of:

- Copper Block, including waxed or oxidized variants → Copper Golem
- Gold Block → Gold Golem
- Emerald Block → Emerald Golem
- Diamond Block → Diamond Golem
- Netherite Block → Netherite Golem

Place a carved pumpkin on top. Iron golem recipe is unchanged from vanilla.

## Configuration

Edit `config/multigolem.json`, created on first server start. Server admins can tune:

- Global ingot healing with `allow_golem_healing`
- Per-tier `max_health`, `attack_damage`, and `anger_on_hit`
- Per-tier `ignored_target_types` values: `CREEPERS`, `ENDERMEN`, `PLAYERS`, `ALL_BOSSES`
- Ability settings for Copper lightning healing, Gold movement/particles, Emerald healing aura, Diamond lightning targeting/cooldowns, and Netherite fire immunity/ignite duration

Server owners can optionally control who may create, heal, use marked spawn eggs for, or configure spawners for each MultiGolem tier with LuckPerms-compatible permission nodes. The same creation permission nodes cover marked spawn egg use and marked spawn egg spawner configuration. Permissions are permissive by default, so existing servers keep their current behavior unless a permissions plugin denies a node.

Existing V1 config files migrate automatically to the V2 schema, and unknown fields are preserved where possible.

## Requirements

- Minecraft 26.1.2
- Fabric Loader 0.19.2+
- Fabric API 0.148.0+26.1.2

## Spawn Eggs

MultiGolem adds Copper, Gold, Emerald, Diamond, and Netherite Golem Spawn Egg stacks as marked vanilla iron golem spawn eggs. Unmarked vanilla iron golem spawn eggs remain vanilla-owned.
