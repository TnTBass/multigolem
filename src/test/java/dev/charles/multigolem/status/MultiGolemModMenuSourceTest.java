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
    void modMenuScreenRendersRichCarryBabyAnimalsStyleStatusDetails() throws IOException {
        String api = Files.readString(Path.of("src/client/java/dev/charles/multigolem/client/modmenu/MultiGolemModMenu.java"));
        String screen = Files.readString(Path.of("src/client/java/dev/charles/multigolem/client/modmenu/MultiGolemStatusScreen.java"));

        assertTrue(api.contains("@Environment(EnvType.CLIENT)"));
        assertTrue(api.contains("implements ModMenuApi"));
        assertTrue(api.contains("MultiGolemStatusScreen::new"));
        assertTrue(screen.contains("MultiGolemStatus.display()"));
        assertTrue(screen.contains("display.statusLabel()"));
        assertTrue(screen.contains("display.helpText()"));
        assertTrue(screen.contains("tooltipText(ModStatusDisplay display)"), "screen should build a rich CBA-style status tooltip");
        assertTrue(screen.contains("\"Status: \" + display.statusLabel()"));
        assertTrue(screen.contains("\"Client: \" + versionWithBuild(display.clientVersion(), display.clientBuild())"));
        assertTrue(screen.contains("\"Server: \" + versionWithBuild(display.serverVersion(), display.serverBuild())"));
        assertTrue(screen.contains("addHelpTextLines(text, display.helpText())"), "tooltip help text should wrap into sentence-sized lines");
        assertTrue(screen.contains("helpText.split(\"(?<=\\\\.)\\\\s+\")"), "sentence wrapping should split after periods");
        assertFalse(screen.contains("text.add(display.helpText())"), "tooltip should not keep long help text on one line");
        assertTrue(screen.contains("case TEAL -> 0xFF55FFFF"), "screen should map MSK teal build-mismatch tone");
        assertTrue(screen.contains("case TEAL -> ChatFormatting.AQUA"), "screen should format teal status labels");
        assertTrue(screen.contains("renderStatusRow(guiGraphics"), "screen should use an explicit reference-style status row");
        assertTrue(screen.contains("display.displayName()"));
        assertTrue(screen.contains("setComponentTooltipForNextFrame"), "hover should use the Minecraft tooltip surface");
        assertFalse(screen.contains("centeredText(font, Component.literal(display.helpText())"));
        assertTrue(screen.contains("guiGraphics.fill"), "status UI should use a filled square like Carry Baby Animals");
        assertTrue(screen.contains("STATUS_SIZE"), "status UI should use stable status-square dimensions");
        assertTrue(screen.contains("Button.builder(Component.literal(\"Cancel\")"), "screen should have a clear CarryBabyAnimals-style cancel affordance");
        assertTrue(screen.contains("button -> onClose()"), "cancel button should return to the parent screen");
        assertFalse(screen.contains("renderDetail(guiGraphics, \"Client:\""), "client version should stay in the tooltip, not as a visible row");
        assertFalse(screen.contains("renderDetail(guiGraphics, \"Server:\""), "server version should stay in the tooltip, not as a visible row");
        assertFalse(screen.contains("\"Updates:\""), "update link should not render as a visible row on this compact status screen");
        assertFalse(screen.contains("\"Done\""));
    }
}
