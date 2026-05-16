# Vanilla iron_golem.png template — provenance

**Source:** Minecraft 26.1.2 client jar, asset path
`assets/minecraft/textures/entity/iron_golem/iron_golem.png`

**Extracted:** 2026-05-16 via Loom-decompiled client jar at
`.gradle/loom-cache/minecraftMaven/.../minecraft-clientOnly-*.jar`

**SHA-256:**
37b8337e08ce66f82218b332fc6059795389f3d59836cfb3108d458190e20416

**License:** This file is a verbatim copy of Mojang's vanilla iron golem
texture. It is committed to this repo as a build input only — the
generated per-tier textures derived from it (in `src/main/resources/assets/`)
are the deliverables that ship in the mod jar. Mojang's Minecraft EULA
applies to this template file; do not redistribute outside this build
pipeline.

**Refresh procedure:** when Minecraft updates and the UV layout changes,
re-run Task 9.1 of the V2 plan to extract the fresh template, update the
SHA-256 above, and re-run `genTextures` to regenerate the per-tier textures.
