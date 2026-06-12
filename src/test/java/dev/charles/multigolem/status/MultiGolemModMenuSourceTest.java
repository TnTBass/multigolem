package dev.charles.multigolem.status;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MultiGolemModMenuSourceTest {
    @Test
    void modMenuIsOptionalAndClientOnly() throws IOException {
        String build = Files.readString(Path.of("fabric/build.gradle"));
        String metadata = Files.readString(Path.of("src/fabric/resources/fabric.mod.json"));

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
        assertTrue(metadata.contains("dev.charles.multigolem.fabric.client.modmenu.FabricMultiGolemModMenu"));
    }

    @Test
    void modMenuHubShowsRequiredActionsAndTopRightStatusSquare() throws IOException {
        String api = Files.readString(Path.of("src/fabricClient/java/dev/charles/multigolem/fabric/client/modmenu/FabricMultiGolemModMenu.java"));
        String screen = Files.readString(Path.of("src/commonClient/java/dev/charles/multigolem/client/modmenu/MultiGolemStatusScreen.java"));
        String widget = Files.readString(Path.of("src/commonClient/java/dev/charles/multigolem/client/modmenu/MultiGolemStatusWidget.java"));

        assertTrue(api.contains("@Environment(EnvType.CLIENT)"));
        assertTrue(api.contains("implements ModMenuApi"));
        assertTrue(api.contains("MultiGolemStatusScreen::new"));
        assertFalse(screen.contains("Button.builder(Component.literal(\"Website\")"));
        assertFalse(screen.contains("Button.builder(Component.literal(\"Issues\")"));
        assertFalse(screen.contains("openUri("));
        assertTrue(screen.contains("Button.builder(Component.literal(\"Server Customizations\")"));
        assertTrue(screen.contains("Button.builder(Component.literal(\"Golempedia\")"));
        assertTrue(screen.contains("Button.builder(Component.literal(\"Done\")"));
        assertTrue(screen.contains("primaryTop + actionStep"));
        assertTrue(screen.contains("height - BOTTOM_BUTTON_MARGIN"));
        assertFalse(screen.contains("Button.builder(Component.literal(\"Cancel\")"));
        assertTrue(screen.contains("MultiGolemStatusWidget.renderTopRight("));
        assertFalse(screen.contains("renderStatusRow(guiGraphics"), "hub must not keep a centered permanent status row");
        assertFalse(screen.contains("display.statusLabel()"), "status label belongs in tooltip only");
        assertTrue(widget.contains("MultiGolemStatus.display()"));
        assertTrue(widget.contains("right - STATUS_SQUARE_SIZE - RIGHT_MARGIN"));
        assertTrue(widget.contains("top + TOP_MARGIN"));
        assertTrue(widget.contains("tooltipAnchorY(squareTop, mouseY)"));
        assertTrue(widget.contains("Math.max(mouseY, MIN_TOOLTIP_ANCHOR_Y)"));
        assertTrue(widget.contains("tooltipText(ModStatusDisplay display)"));
        assertTrue(widget.contains("\"Status: \" + display.statusLabel()"));
        assertTrue(widget.contains("\"Client: \" + versionWithBuild(display.clientVersion(), display.clientBuild())"));
        assertTrue(widget.contains("\"Server: \" + versionWithBuild(display.serverVersion(), display.serverBuild())"));
        assertTrue(widget.contains("addHelpTextLines(text, display.helpText())"));
        assertTrue(widget.contains("case TEAL -> 0xFF55FFFF"));
        assertTrue(widget.contains("case TEAL -> ChatFormatting.AQUA"));
        assertFalse(screen.contains("renderDetail(guiGraphics, \"Client:\""), "client version should stay in the tooltip, not as a visible row");
        assertFalse(screen.contains("renderDetail(guiGraphics, \"Server:\""), "server version should stay in the tooltip, not as a visible row");
        assertFalse(screen.contains("\"Updates:\""), "update link should not render as a visible row on this compact status screen");
    }

    @Test
    void serverCustomizationsScreenRendersUnavailableAndClearsPendingOnClose() throws IOException {
        String screen = Files.readString(Path.of("src/commonClient/java/dev/charles/multigolem/client/modmenu/ServerCustomizationsScreen.java"));

        assertTrue(screen.contains("ServerCustomizationsClient.state()"));
        assertTrue(screen.contains("No server customizations are available."));
        assertTrue(screen.contains("Loading server customizations"));
        assertTrue(screen.contains("ViewState.UNAVAILABLE"));
        assertTrue(screen.contains("ViewState.PENDING"));
        assertTrue(screen.contains("ViewState.AVAILABLE"));
        assertTrue(screen.contains("onScreenClose()"));
        assertTrue(screen.contains("ModMenuWrappedText.render("));
        assertTrue(screen.contains("mouseScrolled("));
        assertTrue(screen.contains("scrollOffset"));
        assertFalse(screen.contains("guiGraphics.text(font, Component.literal(line), left + 8, y"));
        assertFalse(screen.contains("configure MultiGolem"));
        assertFalse(screen.contains("Button.builder(Component.literal(\"Copy"));
    }
}
