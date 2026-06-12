package dev.charles.multigolem.golempedia;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GolempediaSourceTest {
    @Test
    void golempediaScreenUsesStaticCatalogAndExpectedSections() throws IOException {
        String screen = Files.readString(Path.of("src/commonClient/java/dev/charles/multigolem/client/modmenu/GolempediaScreen.java"));
        String wrappedText = Files.readString(Path.of("src/commonClient/java/dev/charles/multigolem/client/modmenu/ModMenuWrappedText.java"));
        String catalog = Files.readString(Path.of("src/common/java/dev/charles/multigolem/golempedia/GolempediaCatalog.java"));

        assertTrue(screen.contains("GolempediaCatalog.entries()"));
        assertTrue(screen.contains("selectedIndex"));
        assertTrue(screen.contains("scrollOffset"));
        assertTrue(screen.contains("maxVisibleEntries()"));
        assertTrue(screen.contains("detailWidth(left)"));
        assertTrue(screen.contains("contentBottom()"));
        assertTrue(screen.contains("mouseScrolled("));
        assertTrue(screen.contains("Math.max(0, scrollOffset"));
        assertTrue(screen.contains("selectedIndex = index;"));
        assertTrue(screen.contains("scrollOffset = 0;"));
        assertTrue(screen.contains("ModMenuWrappedText.renderBounded("));
        assertTrue(wrappedText.contains("static int render("));
        assertTrue(wrappedText.contains("static int renderBounded("));
        assertTrue(screen.contains("statsFor(entry)"));
        assertTrue(screen.contains("villageSpawnFor(entry)"));
        assertTrue(screen.contains("Creation"));
        assertTrue(screen.contains("Healing"));
        assertTrue(screen.contains("Drops"));
        assertTrue(screen.contains("Stats"));
        assertTrue(screen.contains("Spawn Egg"));
        assertTrue(screen.contains("Village Spawns"));
        assertTrue(screen.contains("Ability"));
        assertTrue(screen.contains("Caveats"));
        assertTrue(screen.contains("ServerCustomizationsClient.state()"));
        assertFalse(screen.contains("MultiGolemStatus"));
        assertFalse(screen.contains("guiGraphics.text(font, Component.literal(body), left + 8, y + 10"));
        assertTrue(catalog.contains("caveatsFor(GolemVariant variant)"));
    }
}
