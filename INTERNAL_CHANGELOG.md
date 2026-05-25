# Internal Changelog

## Unreleased

- Fixed the release changelog gate so CI ignores regenerated item texture outputs from the spawn egg texture generator.
- Captured the V4.1 planning handoff for separate village-spawned Netherite ignite configuration and the rationale for tracking village origin instead of using `playerCreated`.
- Documented the V4.1 default rationale: villages are often wooden, so opt-in Netherite village defenders should not start fires unless a server owner configures village ignite seconds.
- Added a Gradle-backed V4 planning handoff gate, tests for that gate, and process notes for worktree-safe plan creation and Revue review request schema.
