# Changelog

## Unreleased

## 0.6.1+mc26.2 — 2026-06-19

- Updated the Fabric and NeoForge builds for Minecraft 26.2.

## 0.6.0+mc26.1.2 — 2026-06-14

- Added NeoForge support for Minecraft 26.1.2, including golem creation, variant stats, abilities, permissions, networking, marked spawn eggs, spawners, rendering, Mod Menu configuration, server customizations, and Golempedia parity with Fabric.
- Changed release artifacts to use loader-first jar names such as `multigolem-fabric-0.6.0+mc26.1.2.jar` and `multigolem-neoforge-0.6.0+mc26.1.2.jar`.
- Kept loader metadata version display focused on the MultiGolem version, while build metadata remains available for client/server mismatch diagnostics.

## 0.5.1+mc26.1.2 — 2026-06-07

- Added a ModMenu status indicator, powered by the open-source [ModStatusKit GitHub project](https://github.com/TnTBass/ModStatusKit), so players and server owners can quickly confirm they are running the same MultiGolem version, with client and server version/build details, build-only mismatch highlighting, and warning-level mismatch status that calls out possible differences without treating them as a connection break.
- Added a ModMenu hub with quick access to server customizations and Golempedia when MultiGolem is installed on the client.
- Added a server customizations screen that shows the connected server's active MultiGolem settings, including global healing, variant stats, ability settings, and village-spawn behavior. When disconnected, the client falls back to the mod's default values.
- Added Golempedia, a client-side reference for each golem variant with creation blocks, healing items, drops, stats, spawn egg availability, village-spawn notes, and ability summaries.

## 0.5.0+mc26.1.2 — 2026-06-02

- Improved Copper Iron Golems so they keep the copper state they were built from and render matching fresh, exposed, weathered, oxidized, and waxed textures, with an updated default copper look.
- Improved Emerald Golem textures so the body, arms, legs, and spawn egg read brighter and more clearly as emerald instead of oxidized copper.
- Refactored golem variant code to make future golem types easier to add and to prepare for vanilla Copper Golem variants.

## 0.4.0+mc26.1.2 — 2026-05-28

- Added hostile Zombie Golems built from Mossy Cobblestone. They heal with Rotten Flesh, apply Hunger/Nausea/Poison to players, convert villagers and wandering traders into zombie villagers, fight village defenders, and stay allied with zombies, zombie villagers, and other Zombie Golems.
- Added Zombie Golem spawn support through marked spawn eggs, marked spawners, and zombie-village maintenance spawning.
- Added permissive-by-default `multigolem.create.zombie` and `multigolem.heal.zombie` permissions.
- Added separate `netherite_village_ignite_seconds` config so server admins can opt Netherite village defenders into ignite-on-hit separately from player-built and other non-village Netherite golems.

## 0.3.0+mc26.1.2 — 2026-05-24

- Disabled golem healing no longer plays the normal heal feedback when players use matching materials on damaged golems.
- Villages can now naturally spawn MultiGolem variants as defenders.
- Added server config for village spawn weights. Iron, Copper, Gold, and Emerald are common by default, Diamond is rare, and Netherite has a default weight of `0` so villages do not naturally spawn fire-starting Netherite defenders unless a server owner opts in.
- Server owners can now control who may create or heal each MultiGolem tier using Fabric Permissions API-compatible permission providers.
- Permissions are permissive by default, so existing servers keep their current creation and healing behavior unless a permissions plugin denies a node.
- Permission checks apply to player-built MultiGolem creation, ingot-based healing, marked spawn egg use, and marked spawn egg spawner configuration.
- Added Copper, Gold, Emerald, Diamond, and Netherite Golem Spawn Egg variants as marked vanilla iron golem spawn eggs.
- The MultiGolem Copper Golem Spawn Egg uses a different icon from the vanilla Copper Golem Spawn Egg, but both spawn eggs have the same name.
- Marked MultiGolem spawn eggs can configure monster spawners to spawn the matching MultiGolem variant.
- Added a MultiGolem icon and project links for ModMenu and mod list screens.

## 0.2.2+mc26.1.2 — 2026-05-17

- Fixed waxed and oxidized copper blocks not creating MultiGolem Copper golems in the iron-golem T-pattern.
- Fixed Diamond golems getting stuck with an overlong lightning cooldown after firing once, so they can recharge again within the configured cooldown window.
- Fixed Netherite golems showing the burning state even though fire and lava damage was blocked.
- Increased the default Gold golem movement speed multiplier from `1.25` to `1.75`.

## 0.2.1+mc26.1.2 — 2026-05-17

- Fixed Gold, Emerald, Diamond, and Netherite golems losing their extra health after chunk unloads or server restarts. High-tier golems now keep their configured max HP instead of dropping back to 100 HP.
- Fixed a server watchdog crash that could happen in busy worlds when Gold, Emerald, or Diamond golems checked nearby entities.
- Improved variant textures so Gold reads as brighter metal, Emerald is greener, Diamond looks clean instead of grimy, and Netherite has darker material shading with lava cracks visible from multiple sides.

## 0.2.0+mc26.1.2 — 2026-05-16

- Added client-side variant textures so Copper, Gold, Emerald, Diamond, and Netherite golems are visually distinct in-game.
- Added material abilities:
  - Gold golems move faster and show light particle effects.
  - Emerald golems heal nearby villagers.
  - Copper golems absorb lightning and heal from it.
  - Diamond golems can call lightning in combat and resist lightning damage.
  - Netherite golems resist fire and lava damage and can ignite enemies they hit.
- Added client sync for golem variants so multiplayer clients can render the correct texture.
- Added the first V2 config migration pieces, including persistent ability state and target-filter support for iron golem targeting.
- Fixed release automation issues for Modrinth project description updates and CurseForge uploads.

## 0.1.0+mc26.1.2 — 2026-05-15

- Initial release for Minecraft 26.1.2.
- Added six iron golem variants: Copper, Iron, Gold, Emerald, Diamond, and Netherite.
- Added T-pattern creation using each variant's material block.
- Added per-variant health and attack scaling.
- Added healing with matching ingots, configurable with `allow_golem_healing`.
- Added configurable `anger_on_hit` behavior per variant.
- Added material-specific drops when golems die.
- Vanilla clients can still join servers running the mod.
