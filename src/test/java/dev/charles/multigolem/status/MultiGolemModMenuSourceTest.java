package dev.charles.multigolem.status;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MultiGolemModMenuSourceTest {
    @Test
    void modMenuIsOptionalAndClientOnly() throws IOException {
        String build = Files.readString(Path.of("build.gradle"));
        String metadata = Files.readString(Path.of("src/main/resources/fabric.mod.json"));

        assertTrue(build.contains("compileOnly \"com.terraformersmc:modmenu:${project.modmenu_version}\""));
        int dependsStart = metadata.indexOf("\"depends\"");
        int dependsEnd = metadata.indexOf("\n  }", dependsStart);
        int entrypointsStart = metadata.indexOf("\"entrypoints\"");
        assertTrue(entrypointsStart >= 0, "entrypoints key missing");
        String dependsBlock = dependsStart >= 0 && dependsEnd > dependsStart
            ? metadata.substring(dependsStart, dependsEnd)
            : "";
        String entrypointsBlock = metadata.substring(entrypointsStart, dependsStart);
        assertFalse(dependsBlock.contains("\"modmenu\""), "ModMenu must not be a runtime dependency");
        assertTrue(entrypointsBlock.contains("\"modmenu\""));
        assertTrue(metadata.contains("dev.charles.multigolem.client.modmenu.MultiGolemModMenu"));
    }

    @Test
    void modMenuScreenRendersCompactStatusDotOnly() throws IOException {
        String api = Files.readString(Path.of("src/client/java/dev/charles/multigolem/client/modmenu/MultiGolemModMenu.java"));
        String screen = Files.readString(Path.of("src/client/java/dev/charles/multigolem/client/modmenu/MultiGolemStatusScreen.java"));

        assertTrue(api.contains("@Environment(EnvType.CLIENT)"));
        assertTrue(api.contains("implements ModMenuApi"));
        assertTrue(api.contains("MultiGolemStatusScreen::new"));
        assertTrue(screen.contains("MultiGolemStatus.display()"));
        assertTrue(screen.contains("display.statusLabel()"));
        assertTrue(screen.contains("display.helpText()"));
        assertTrue(screen.contains("indicatorDot()"));
        assertFalse(screen.contains("indicatorFor(StatusTone"));
        assertTrue(screen.contains("centeredText(font, Component.literal(display.helpText())"), "hover help should be centered instead of mouse-anchored");
        assertFalse(screen.contains("setTooltipForNextFrame"));
        assertTrue(screen.contains("DOT_SIZE"), "status UI should be a compact dot, not a config-style button stack");
        assertTrue(screen.contains("Button.builder(Component.literal(\"Cancel\")"), "screen should have a clear CarryBabyAnimals-style cancel affordance");
        assertTrue(screen.contains("button -> onClose()"), "cancel button should return to the parent screen");
        assertFalse(screen.contains("\"Client: \""));
        assertFalse(screen.contains("\"Server: \""));
        assertFalse(screen.contains("\"Updates\""));
        assertFalse(screen.contains("\"Done\""));
    }
}
