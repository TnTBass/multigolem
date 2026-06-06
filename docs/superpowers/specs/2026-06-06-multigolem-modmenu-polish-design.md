# MultiGolem Mod Menu Polish Design

## Context

MultiGolem already has optional Mod Menu support in the `codex/modstatus-on-current-main` worktree. The current screen is a compact status row backed by the embedded ModStatusKit-style status model. That row reports only version/status information, which should remain its job.

This design expands the Mod Menu surface into a small player-facing hub without turning Mod Menu into a required dependency, a gameplay gate, or a server configuration editor.

## Goals

- Keep Mod Menu optional, compile-only, and client-side.
- Preserve the "server-side functional; vanilla clients welcome" promise.
- Move the status indicator into a compact top-right square/dot similar to CarryBabyAnimals.
- Keep version/status data in the existing ModStatusKit status channel only.
- Add a server-reported customizations screen on a separate MultiGolem-owned channel.
- Add a static, build-bundled Golempedia based on the variants and behavior included in the mod.
- Avoid client visual toggles, troubleshooting pages, extra link surfaces, and diagnostic report copying in this slice.

## Non-Goals

- No client option to disable MultiGolem visuals.
- No expansion of the ModStatusKit payload to carry gameplay rules or config.
- No config editor for server behavior.
- No additional website, issue, changelog, or external link buttons beyond the existing Mod Menu surfaces.
- No diagnostic report copy button in this slice.
- No release, tag, deploy, or publish work.

## User Experience

The Mod Menu config button opens a MultiGolem hub screen.

The top-right corner contains a small status square. It uses the existing `MultiGolemStatus.display()` data and remains focused on connection/version state. Hovering the square shows the current rich status tooltip with display name, status label, client version/build, server version/build, and status help text.

The main actions are:

- `Website`
- `Issues`
- `Server Customizations`
- `Golempedia`
- `Done`

`Website` and `Issues` keep the current Mod Menu metadata role. `Server Customizations` opens the server-specific screen. `Golempedia` opens the static reference screen. `Done` returns to the parent Mod Menu screen.

## Status Indicator

The status indicator is the existing MSK-backed compatibility affordance, moved from a centered row into a compact top-right square.

Tone mapping stays aligned with the current status model:

- Green: client and server public versions match.
- Teal: public versions match but build identifiers differ.
- Orange or red: client and server public versions differ, based on existing mismatch severity.
- Gray: disconnected, unknown, or server not detected.

The indicator must not display server gameplay settings. It may show text only in a hover tooltip or other compact native hint, not as a permanent central row.

## Server Customizations

`Server Customizations` is the server-specific gameplay/config screen. It is intentionally separate from the status indicator and must be backed by a MultiGolem-owned custom payload, not the ModStatusKit status payload.

Empty states:

- If the client is not connected to a server or world, show `No server customizations are available.`
- If the client is connected to a server without MultiGolem customizations support, show `No server customizations are available.`
- If a future payload is still pending, the screen may briefly show a native loading line. That loading state must clear on disconnect or screen close and must fall back to `No server customizations are available.` if no payload arrives before the implementation-defined timeout.

When server data is available, the screen reports what the connected server has customized. It is read-only and should be framed as "what this server is doing," not "configure MultiGolem."

First server snapshot fields should cover:

- Global healing enabled or disabled.
- Village spawn weighting enabled or disabled, plus non-default variant weights.
- Zombie village spawning enabled or disabled, plus notable non-default limits.
- Permissions-controlled behavior, if the server can report it reliably.
- Per-variant customizations that differ from bundled defaults and are meaningful to players.

The screen should avoid dumping every raw config value. It should summarize meaningful differences from the bundled defaults and group them by topic or variant.

## Server Customizations Channel

Server customizations use a new channel separate from MSK, for example `multigolem:server_customizations`.

The payload should be optional and capability-gated. Servers only send it to clients that advertise support. Vanilla clients and clients without the new receiver continue to work unchanged.

The payload is authoritative for the current server session. The client should clear cached customizations on disconnect and treat missing data as unavailable.

The payload should model semantic facts rather than raw JSON. For example:

- `healingEnabled`
- `villageSpawnsEnabled`
- `villageSpawnWeights`
- `zombieVillageSpawningEnabled`
- `permissionsMode`
- `variantOverrides`

Exact packet schema belongs in the implementation plan. This design only requires that the channel stay independent from the version/status channel and that unavailable data renders as unavailable.

## Golempedia

`Golempedia` is a static, build-bundled reference for the golem types included in the installed mod build. It does not depend on server connection state and does not claim to reflect a server's custom configuration.

The screen should feel like a compact Civilopedia-style reference:

- A variant list for Copper, Gold, Emerald, Diamond, Netherite, and Zombie.
- A detail pane for the selected variant.
- Concise player-facing sections with native Minecraft UI styling.

Each entry should cover:

- Body block or creation summary.
- Healing item.
- Drop item or loot note.
- Spawn egg availability.
- Village spawn note, where relevant.
- Core ability.
- Special caveats.

Content should be derived from local build knowledge where practical:

- `GolemVariantCatalog` for variant coverage, heal items, drops, spawn egg support, render support, and buildable status.
- `MultiGolemConfig.defaults()` / `TierStats` for default behavior values that players need to understand.
- Existing README wording for concise player-facing descriptions.

Golempedia may include default values, but it must not imply that default values are active on a connected server. Server-specific deviations belong in `Server Customizations`.

## Data Ownership

The design has three separate data lanes:

- Status indicator: existing ModStatusKit-style status state, version/build only.
- Server Customizations: future MultiGolem-owned server snapshot channel, session-specific and authoritative when present.
- Golempedia: bundled static/catalog/default data from the installed client build.

Keeping these lanes separate prevents the MSK payload from becoming a general feature bus and keeps player reference content available offline.

## Architecture

The implementation should split UI content from rendering:

- `MultiGolemStatusScreen` becomes the Mod Menu hub or delegates to a new hub screen.
- A small status widget/helper renders the top-right square and tooltip from `ModStatusDisplay`.
- A server customizations client state holder tracks optional server snapshot data and clears it on disconnect.
- A customizations payload and client/server networking pair send server data on join or when config data becomes available.
- Golempedia view models expose static variant facts without requiring UI code to know catalog internals.

The UI should stay native to Minecraft's screen/button/text components. Do not add a large config UI library for this slice.

## Error Handling

- Missing Mod Menu: no effect; the mod still runs normally.
- Missing server customizations payload: show `No server customizations are available.`
- Malformed or unsupported customizations payload: ignore it, log a concise debug or warning message, and keep the screen unavailable.
- Disconnect/client stop: clear server customizations and status-derived connection state as appropriate.
- Missing catalog/default data for a Golempedia variant: fail tests before release rather than silently omitting a variant.

## Testing

Focused tests should cover:

- Mod Menu remains optional and is not listed as a runtime dependency.
- The hub screen uses the top-right status square and no longer relies on a centered permanent status row.
- Status tooltip still uses `MultiGolemStatus.display()` and keeps version/build details.
- Server Customizations shows the unavailable state when disconnected or when no server data exists.
- If Server Customizations uses a pending/loading state, it transitions to unavailable when no payload arrives, on disconnect, or on screen close.
- Server customizations state clears on disconnect.
- The server customizations channel is distinct from `MultiGolemStatus.PAYLOAD_PATH`.
- The server sends customizations only after a capability check.
- Golempedia includes every non-iron variant represented in `GolemVariantCatalog`, regardless of whether that variant is currently player-buildable or spawn-egg eligible.
- Golempedia content for each variant includes creation, healing, drops, spawn egg support, ability, and caveat fields.

Verification should include targeted Java tests for the new state/model code, source-structure tests for optional Mod Menu and networking boundaries, then the normal project build/test gate selected for the implementation plan.

## Acceptance Criteria

- Mod Menu opens a MultiGolem hub with Website, Issues, Server Customizations, Golempedia, Done, and a compact top-right status square.
- Status remains version/build-focused and backed by the existing MSK status display.
- No server gameplay data is added to the MSK status payload.
- Server Customizations is read-only and says `No server customizations are available.` when disconnected or when the server lacks the new MultiGolem customizations payload.
- Server-specific customization data, when implemented, uses a separate MultiGolem-owned channel.
- Golempedia is available offline and reflects static facts from the installed build.
- The design does not add visual-disabling options, extra link pages, troubleshooting pages, or diagnostic copy behavior.

## Deferred Follow-Ups

- Diagnostic report copying for bug reports.
- Additional customizations snapshot fields after the first server channel proves useful.
- Richer Golempedia formatting or icons if the first static reference screen feels too text-heavy.
