# MultiGolem

## Features

- Adds eight Iron Golem variants: Copper, Redstone, Gold, Lapis, Emerald, Diamond, Netherite, and Zombie.
- Server-side functional: vanilla clients can connect to a server running MultiGolem with no mod installed.
- Build variants with the vanilla iron-golem T-shape using copper, redstone, gold, lapis, emerald, diamond, netherite, or mossy cobblestone body blocks. Copper Iron Golems preserve fresh, exposed, weathered, oxidized, and waxed copper states with matching textures.
- Marked vanilla iron golem spawn eggs for Copper, Redstone, Gold, Lapis, Emerald, Diamond, Netherite, and Zombie variants.
- Stats scale by role; Copper is the weakest, Redstone is a lower-strength emergency-control defender, Lapis is fragile anti-magic support, and Netherite is the strongest.
- Heal each variant with its matching material.
- Custom textures per variant on modded clients; vanilla clients still see regular iron golems.
- Optional ModMenu screens show client/server MultiGolem version status, active server customization values, and a Golempedia reference for golem creation, stats, drops, village spawns, and abilities.

## Special Abilities

- **Copper:** lightning strikes heal instead of damage.
- **Redstone:** overcharges at or below 25% health for attack and resistance without speed, then releases a Slowness X overload pulse on death.
- **Gold:** +75% movement speed, sprint-dust particles while moving, and sunlight-shine particles while idle outdoors.
- **Lapis:** protects nearby allied village entities from magic damage and configured harmful magical effects. Player protection is disabled by default and can be enabled by server config.
- **Emerald:** heals passively while villagers or wandering traders are nearby.
- **Diamond:** passive LOS lightning zap of nearby hostiles, on-attack lightning, and lightning immunity. Creepers are excluded by default.
- **Netherite:** immune to fire and lava damage, and ignites hit mobs for 5 seconds.
- **Zombie:** hostile corrupted golem that heals from Rotten Flesh, applies Hunger/Nausea/Poison to players, converts villagers and wandering traders into zombie villagers, fights village defenders, and stays allied with zombies and converted zombie villagers.

Cross-tier `ignored_target_types` lets server admins stop tiers from targeting categories such as creepers, players, endermen, or bosses. Copper, Redstone, Gold, Lapis, Emerald, Diamond, and Netherite ignore creepers by default to prevent collateral block damage.

## Creation Recipes

Build a T-shape (1 base, 1 center, 2 arms) out of one of:

- Copper Block, including fresh, exposed, weathered, oxidized, and waxed variants → Copper Iron Golem
- Redstone Block → Redstone Golem
- Gold Block → Gold Golem
- Lapis Block → Lapis Golem
- Emerald Block → Emerald Golem
- Diamond Block → Diamond Golem
- Netherite Block → Netherite Golem
- Mossy Cobblestone → Zombie Golem

Place a carved pumpkin on top. Iron golem recipe is unchanged from vanilla. The vanilla single-copper-block Copper Golem recipe remains vanilla-owned.

## Configuration

Edit `config/multigolem.json`, created on first server start. Server admins can tune:

- Global ingot healing with `allow_golem_healing`
- Per-tier `max_health`, `attack_damage`, and `anger_on_hit`
- Per-tier `ignored_target_types` values: `CREEPERS`, `ENDERMEN`, `PLAYERS`, `ALL_BOSSES`
- Ability settings for Copper lightning healing, Redstone overcharge/death pulse, Gold movement/particles, Lapis ward protection, Emerald healing aura, Diamond lightning targeting/cooldowns, Netherite fire immunity/ignite duration, and Zombie sickness/conversion/Rotten Flesh healing
- Zombie-villager village maintenance with `zombie_village_spawning`

Server owners can optionally control who may create, heal, use marked spawn eggs for, or configure spawners for each MultiGolem tier with LuckPerms-compatible permission nodes. The same creation permission nodes cover marked spawn egg use and marked spawn egg spawner configuration. Permissions are permissive by default, so existing servers keep their current behavior unless a permissions plugin denies a node.

Existing V1 config files migrate automatically to the V2 schema, and unknown fields are preserved where possible.

## Modpacks

MultiGolem is allowed in modpacks.

## Client ModMenu Tools

Players with ModMenu installed can open MultiGolem's client-side hub to check whether their client and server MultiGolem versions match. Server owners and players can also inspect the connected server's active customization values, including variant health, attack damage, healing, ability settings, and village-spawn behavior. When not connected to a server, Golempedia and customization screens show MultiGolem's default values.

## Requirements

- Minecraft 26.2 or later compatible 26.x runtime
- Fabric build: Fabric Loader 0.19.2+ and Fabric API 0.152.2+26.2
- NeoForge build: NeoForge 26.2.0.6-beta or later compatible 26.2 runtime

## Spawn Eggs

MultiGolem adds Copper, Redstone, Gold, Lapis, Emerald, Diamond, Netherite, and Zombie Golem Spawn Egg stacks as marked vanilla iron golem spawn eggs. Unmarked vanilla iron golem spawn eggs remain vanilla-owned.
