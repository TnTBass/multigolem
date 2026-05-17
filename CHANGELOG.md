# Changelog

## Unreleased

- Documented roadmap items V4 (spawn eggs for Iron Golem variants) and V5 (Copper Golem variants) in README so future contributors and AI assistants can pick them up. V3 (village natural-spawn weighting) remains next.

## 0.2.1+mc26.1.2 — 2026-05-17

- Fixed Gold, Emerald, Diamond, and Netherite golems reverting to 100 HP after chunk unloads or server restarts.
- Fixed a server watchdog crash that could happen on worlds with many Gold, Emerald, or Diamond golems.
- Refined variant textures so Gold looks brighter and more golden, Emerald looks greener, Diamond no longer looks dirty, and Netherite has darker material variation with lava cracks visible from multiple sides.
- Fixed release builds so generated textures rebuild cleanly on GitHub Actions.
- Added a maintenance workflow for updating the Modrinth project listing without publishing a new mod version.
- Added changelog checks so future release notes stay focused on players and server admins, and generated texture files do not cause false release-blocking failures.

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
