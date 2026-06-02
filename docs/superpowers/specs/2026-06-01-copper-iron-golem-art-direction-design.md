# Copper Iron Golem Art Direction Design

## Goal

Make Copper Iron Golem weathering variants read as the same golem aging through copper states, not as separate golem designs.

## Reference

Minecraft 26.1.2 vanilla copper golem assets use one stable indexed body layout for the copper, exposed, weathered, and oxidized textures. The stage files differ by palette, while the pixel layout stays consistent. The vanilla living copper golem does not use separate waxed body textures with large wax symbols.

## Design

Copper Iron Golem texture generation should use one stable base layout for every copper surface state. Weathering should be expressed primarily through palette remapping:

- Fresh: warm copper.
- Exposed: muted rose-brown copper.
- Weathered: green-blue patina with some aged copper warmth preserved.
- Oxidized: strong teal patina.

The generator must remove weathering-specific line patterns and broad stage-specific patch overlays. Exposed, weathered, and oxidized states should retain the same marks and silhouette as the fresh Copper Iron Golem.

Waxed variants should keep the same stable layout. They may add a subtle, consistent wax cue: small honey-gold highlights on the same few metal-edge pixels across all waxed states. They must not add X marks, diagonal bands, or any separate symbolic pattern.

## Non-Goals

- Do not change copper surface-state capture, codecs, spawners, spawn eggs, renderer routing, or migration.
- Do not change vanilla Copper Golem behavior.
- Do not add new gameplay signals for wax beyond texture presentation.
- Do not ship or release this work.

## Acceptance Criteria

- Generated fresh, exposed, weathered, and oxidized Copper Iron Golem textures share the same alpha/layout footprint.
- Weathering variants are visually distinct by color palette, not by new pattern geometry.
- Waxed variants differ from their unwaxed counterpart only by a small bounded wax-highlight set.
- Waxed variants do not contain the previous large X-shaped wax marks.
- Existing texture generation remains deterministic.
- `python scripts/test-generate-textures.py` passes.
- `.\gradlew.bat build` passes.

