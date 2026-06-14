package dev.charles.multigolem.releasecheck;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiloaderArtifactNamingSourceTest {
    @Test
    void loaderArtifactsUseModLoaderVersionMinecraftVersionOrder() throws IOException {
        String fabricBuild = Files.readString(Path.of("fabric/build.gradle"));
        String neoforgeBuild = Files.readString(Path.of("neoforge/build.gradle"));

        assertTrue(fabricBuild.contains("base.archivesName = \"multigolem-fabric-${project.version}\""));
        assertTrue(neoforgeBuild.contains("base.archivesName = \"multigolem-neoforge-${project.version}\""));

        assertFalse(fabricBuild.contains("base.archivesName = \"multigolem-${project.version}-fabric\""));
        assertFalse(neoforgeBuild.contains("base.archivesName = \"multigolem-${project.version}-neoforge\""));
    }
}
