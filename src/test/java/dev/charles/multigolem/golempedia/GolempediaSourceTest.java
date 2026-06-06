package dev.charles.multigolem.golempedia;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GolempediaSourceTest {
    @Test
    void golempediaScreenUsesStaticCatalogAndExpectedSections() throws IOException {
        String screen = Files.readString(Path.of("src/client/java/dev/charles/multigolem/client/modmenu/GolempediaScreen.java"));
        String catalog = Files.readString(Path.of("src/main/java/dev/charles/multigolem/golempedia/GolempediaCatalog.java"));

        assertTrue(screen.contains("GolempediaCatalog.entries()"));
        assertTrue(screen.contains("selectedIndex"));
        assertTrue(screen.contains("maxVisibleEntries()"));
        assertTrue(screen.contains("Creation"));
        assertTrue(screen.contains("Healing"));
        assertTrue(screen.contains("Drops"));
        assertTrue(screen.contains("Spawn Egg"));
        assertTrue(screen.contains("Village Spawns"));
        assertTrue(screen.contains("Ability"));
        assertTrue(screen.contains("Caveats"));
        assertFalse(screen.contains("ServerCustomizations"));
        assertFalse(screen.contains("MultiGolemStatus"));
        assertTrue(catalog.contains("caveatsFor(GolemVariant variant)"));
    }
}
