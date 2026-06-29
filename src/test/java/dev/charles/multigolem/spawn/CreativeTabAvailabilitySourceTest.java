package dev.charles.multigolem.spawn;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreativeTabAvailabilitySourceTest {
    @Test
    void fabricCreativeTabRemainsStaticWithoutServerAvailabilityConfig() throws IOException {
        assertStaticCreativeTabSource("src/fabric/java/dev/charles/multigolem/fabric/event/FabricMultiGolemEvents.java");
    }

    @Test
    void neoforgeCreativeTabRemainsStaticWithoutServerAvailabilityConfig() throws IOException {
        assertStaticCreativeTabSource("src/neoforge/java/dev/charles/multigolem/neoforge/event/NeoForgeMultiGolemEvents.java");
    }

    private static void assertStaticCreativeTabSource(String path) throws IOException {
        String source = Files.readString(Path.of(path));

        assertTrue(source.contains("MultiGolem.creativeSpawnEggVariants()"));
        assertFalse(source.contains("golemAvailability()"));
        assertFalse(source.contains("MultiGolem.config()"));
    }
}
