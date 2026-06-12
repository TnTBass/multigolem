package dev.charles.multigolem.neoforge;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgeSkeletonSourceTest {
    // The Fabric test task sets workingDir to rootProject.projectDir; keep these
    // paths repo-root relative to match the existing source-text tests.
    @Test
    void neoforgeCoordinatesAreRecordedForMinecraft2612() throws IOException {
        String properties = Files.readString(Path.of("gradle.properties"));

        assertTrue(properties.contains("neoforge_version=26.1.2.75"));
        assertTrue(properties.contains("moddevgradle_version=2.0.141"));
        assertTrue(properties.contains("Minecraft 26.1.2"));
    }

    @Test
    void neoforgeMetadataDefinesSkeletonEntrypoints() throws IOException {
        String metadata = Files.readString(Path.of("src/neoforge/resources/META-INF/neoforge.mods.toml"));

        assertTrue(metadata.contains("modLoader=\"javafml\""));
        assertTrue(metadata.contains("modId=\"multigolem\""));
        assertTrue(metadata.contains("version=\"${version}\""));
        assertTrue(metadata.contains("displayName=\"MultiGolem\""));
        assertTrue(metadata.contains("logoFile=\"assets/multigolem/icon.png\""));
        assertTrue(metadata.contains("modId=\"neoforge\""));
        assertTrue(metadata.contains("versionRange=\"[26.1.2.75,)\""));
        assertTrue(metadata.contains("modId=\"minecraft\""));
        assertTrue(metadata.contains("versionRange=\"[26.1.2,26.2)\""));
    }

    @Test
    void neoforgeEntrypointInitializesConfigBeforeGameplayAdapters() throws IOException {
        String mainEntrypoint = Files.readString(Path.of("src/neoforge/java/dev/charles/multigolem/neoforge/MultiGolemNeoForge.java"));
        String clientEntrypoint = Files.readString(Path.of("src/neoforgeClient/java/dev/charles/multigolem/neoforge/client/MultiGolemNeoForgeClient.java"));

        assertTrue(mainEntrypoint.contains("@Mod(MultiGolem.MOD_ID)"));
        assertTrue(mainEntrypoint.contains("MultiGolemStatus.initializeVersion"));
        assertTrue(mainEntrypoint.contains("MultiGolem.initialize(FMLPaths.CONFIGDIR.get().resolve(MultiGolem.MOD_ID + \".json\"));"));
        assertTrue(mainEntrypoint.contains("NeoForgeMultiGolemEvents.register(modBus);"));
        assertTrue(mainEntrypoint.contains("NeoForgeAbilityEvents.register();"));
        assertFalse(mainEntrypoint.contains("registerServer"));
        assertFalse(mainEntrypoint.contains("Fabric"));

        assertTrue(clientEntrypoint.contains("Dist.CLIENT"));
        assertFalse(clientEntrypoint.contains("registerClient"));
        assertFalse(clientEntrypoint.contains("ClientPlayNetworking"));
        assertFalse(clientEntrypoint.contains("Fabric"));
    }

    @Test
    void neoforgeBuildProducesLoaderSuffixedArtifacts() throws IOException {
        String build = Files.readString(Path.of("neoforge/build.gradle"));
        String rootBuild = Files.readString(Path.of("build.gradle"));

        assertTrue(build.contains("id 'net.neoforged.moddev'"));
        assertTrue(build.contains("base.archivesName = \"multigolem-${project.version}-neoforge\""));
        assertTrue(build.contains("archiveVersion = \"\""));
        assertTrue(rootBuild.contains(":neoforge:build"));
    }
}
