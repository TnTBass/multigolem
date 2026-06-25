# Internal Changelog

## Unreleased

- Implemented V7 Redstone Golem as a lower-strength emergency-control defender with Redstone Block construction, Redstone Dust healing, overcharge attack/resistance without speed, a Slowness X death pulse, marked eggs/assets, docs, and Fabric/NeoForge parity.
- Added Redstone Golem design and implementation planning artifacts without starting gameplay implementation.
- Stopped publishing sources jars to Modrinth while keeping sources artifacts on GitHub Releases.
- Added NeoForge permission nodes and permission API checks for the multiloader NeoForge adapter.
- Added a NeoForge Gradle skeleton with metadata-only entrypoints and loader-suffixed NeoForge artifacts.
- Added a release-source gate for loader-suffixed Fabric and NeoForge artifacts across GitHub Release, Modrinth, and CurseForge paths.
- Moved Fabric build ownership into a loader-specific Gradle project and renamed Fabric build artifacts with a loader suffix.
- Reshaped the current Fabric source layout into common, common-client, Fabric, and Fabric-client roots while preserving the existing Fabric build.
- Added multiloader planning documents and a loader-neutral common source scan for the upcoming Fabric-preserving NeoForge port.
- Fixed the release workflow changelog extractor so version sections with `+mc...` metadata and dated headings publish explicit release notes instead of GitHub's generated fallback notes.
- Implemented the reviewed Zombie Golem design without starting Redstone/Lapis or unrelated golem work.
- Fixed the release changelog gate so CI ignores regenerated item texture outputs from the spawn egg texture generator.
- Captured the V4.1 planning handoff for separate village-spawned Netherite ignite configuration and the rationale for tracking village origin instead of using `playerCreated`.
- Documented the V4.1 default rationale: villages are often wooden, so opt-in Netherite village defenders should not start fires unless a server owner configures village ignite seconds.
- Added a Gradle-backed V4 planning handoff gate, tests for that gate, and process notes for worktree-safe plan creation and Revue review request schema.
