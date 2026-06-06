# MultiGolem Mod Menu Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the committed Mod Menu polish design: a MultiGolem hub screen with a compact MSK-backed status square, read-only server customizations on a separate MultiGolem channel, and an offline static Golempedia.

**Architecture:** Keep three data lanes separate. `dev.charles.multigolem.status` remains the version/build-only MSK integration; new `dev.charles.multigolem.customizations` classes own server-specific gameplay snapshots; new `dev.charles.multigolem.golempedia` classes own static build/catalog-derived reference data. Client UI under `src/client/java/dev/charles/multigolem/client/modmenu` renders native Minecraft screens and delegates all data shaping to those model/state classes.

**Tech Stack:** Java 21, Fabric API networking/client events, Minecraft native `Screen`/`Button`/text rendering APIs, Mod Menu compile-only API, JUnit 5 source/model tests, Gradle via `.\gradlew.bat`.

---

## Scope Boundaries

Implement only the Mod Menu polish described by `docs/superpowers/specs/2026-06-06-multigolem-modmenu-polish-design.md` at commit `aa5af8d`.

Do:
- Keep Mod Menu optional, compile-only, and client-side.
- Preserve vanilla-client compatibility by capability-gating new clientbound packets.
- Keep MSK status focused on version/build state only.
- Use a separate MultiGolem-owned `multigolem:server_customizations` payload for server customization snapshots.
- Make Golempedia static and available without a server connection.

Do not:
- Add visual-disabling options.
- Add troubleshooting pages.
- Add extra external link surfaces beyond `Website` and `Issues`.
- Add diagnostic report copy behavior.
- Add gameplay gates, config editing, release/tag/deploy/publish work, or runtime Mod Menu dependency.

## File Structure

### Existing files to modify

- `src/client/java/dev/charles/multigolem/client/modmenu/MultiGolemModMenu.java`
  - Continue returning the Mod Menu config screen factory, but point at the hub screen if `MultiGolemStatusScreen` is renamed or delegated.
- `src/client/java/dev/charles/multigolem/client/modmenu/MultiGolemStatusScreen.java`
  - Convert into `MultiGolemHubScreen` behavior or keep as a compatibility-named hub. Remove the centered permanent status row and render a top-right status square only.
- `src/client/java/dev/charles/multigolem/client/MultiGolemClient.java`
  - Register the new server customizations client receiver/state lifecycle.
- `src/main/java/dev/charles/multigolem/MultiGolem.java`
  - Register the server customizations payload and join sender.
- `src/test/java/dev/charles/multigolem/status/MultiGolemModMenuSourceTest.java`
  - Update source-structure assertions from the old status-only screen to the hub/status-square behavior.
- `src/test/java/dev/charles/multigolem/status/MultiGolemStatusIntegrationSourceTest.java`
  - Keep MSK status assertions and add a guard that no server customization fields enter status payload/source.

### New files to create

- `src/main/java/dev/charles/multigolem/customizations/ServerCustomizationsPayload.java`
  - Clientbound custom payload record for `multigolem:server_customizations`.
- `src/main/java/dev/charles/multigolem/customizations/ServerCustomizationsSnapshot.java`
  - Immutable semantic server snapshot used by payloads and UI summaries.
- `src/main/java/dev/charles/multigolem/customizations/VariantCustomizationSummary.java`
  - Per-variant meaningful server differences from bundled defaults.
- `src/main/java/dev/charles/multigolem/customizations/ServerCustomizationsSummary.java`
  - Text/model summary groups for the read-only screen.
- `src/main/java/dev/charles/multigolem/customizations/ServerCustomizationsSummarizer.java`
  - Compares a current `MultiGolemConfig` to `MultiGolemConfig.defaults()` and produces concise player-facing groups.
- `src/main/java/dev/charles/multigolem/customizations/ServerCustomizationsNetworking.java`
  - Registers the payload and capability-gated server join send.
- `src/client/java/dev/charles/multigolem/client/customizations/ServerCustomizationsClient.java`
  - Registers the client receiver, disconnect clear, screen-close clear hook integration, and tick timeout.
- `src/client/java/dev/charles/multigolem/client/customizations/ServerCustomizationsClientState.java`
  - Testable client state holder for unavailable, pending, available, timeout, disconnect, and close transitions.
- `src/client/java/dev/charles/multigolem/client/modmenu/MultiGolemStatusWidget.java`
  - Small top-right square renderer and tooltip builder backed only by `MultiGolemStatus.display()`.
- `src/client/java/dev/charles/multigolem/client/modmenu/ServerCustomizationsScreen.java`
  - Read-only screen for available summaries or `No server customizations are available.`.
- `src/main/java/dev/charles/multigolem/golempedia/GolempediaEntry.java`
  - Static view model for one non-iron golem variant.
- `src/main/java/dev/charles/multigolem/golempedia/GolempediaCatalog.java`
  - Builds entries from `GolemVariantCatalog`, `MultiGolemConfig.defaults()`, and hard-coded player-facing ability/caveat copy.
- `src/client/java/dev/charles/multigolem/client/modmenu/GolempediaScreen.java`
  - Variant list and detail pane using `GolempediaCatalog.entries()`.
- `src/test/java/dev/charles/multigolem/customizations/ServerCustomizationsClientStateTest.java`
  - Unit tests for unavailable/pending/timeout/disconnect/screen-close state transitions.
- `src/test/java/dev/charles/multigolem/customizations/ServerCustomizationsPayloadTest.java`
  - Payload identity, channel independence from `MultiGolemStatus.PAYLOAD_PATH`, size/codec, and snapshot round-trip tests.
- `src/test/java/dev/charles/multigolem/config/ServerCustomizationsSummarizerTest.java`
  - Default and non-default summary tests.
- `src/test/java/dev/charles/multigolem/customizations/ServerCustomizationsNetworkingSourceTest.java`
  - Source guard tests for capability-gated server send and client disconnect clear.
- `src/test/java/dev/charles/multigolem/golempedia/GolempediaCatalogTest.java`
  - Catalog coverage and required content fields for every non-iron variant.
- `src/test/java/dev/charles/multigolem/golempedia/GolempediaSourceTest.java`
  - Source guard that UI uses Golempedia static models and not live server customizations.

## Verification Commands

Run these from `C:\Users\tyler\AI Projects\MultiGolem\.worktrees\modstatus-on-current-main`.

- Root guard: `git rev-parse --show-toplevel`
  - Expected: `C:/Users/tyler/AI Projects/MultiGolem/.worktrees/modstatus-on-current-main`
- Status guard: `git status --short --branch`
  - Expected before implementation: clean except the plan commit; during implementation, only task-scoped files.
- Targeted Mod Menu source tests: `.\gradlew.bat --quiet test --tests dev.charles.multigolem.status.MultiGolemModMenuSourceTest`
- Targeted customizations tests: `.\gradlew.bat --quiet test --tests dev.charles.multigolem.customizations.* --tests dev.charles.multigolem.config.ServerCustomizationsSummarizerTest`
- Targeted Golempedia tests: `.\gradlew.bat --quiet test --tests dev.charles.multigolem.golempedia.*`
- Status boundary tests: `.\gradlew.bat --quiet test --tests dev.charles.multigolem.status.MultiGolemStatusIntegrationSourceTest --tests dev.charles.multigolem.status.MultiGolemStatusPayloadTest`
- Final build gate: `.\gradlew.bat --quiet build`
- Optional artifact proof after final build: `jar tf build\libs\multigolem-0.4.0+mc26.1.2.jar`
  - Expected to include new `dev/charles/multigolem/customizations/*` and `dev/charles/multigolem/golempedia/*` classes.
  - Expected not to include `com/terraformersmc/modmenu` classes.

## Rollback Notes

- The server customizations lane is additive. If it causes trouble, revert `dev.charles.multigolem.customizations`, `dev.charles.multigolem.client.customizations`, the `MultiGolem`/`MultiGolemClient` registration calls, and the `Server Customizations` button while leaving the MSK status lane intact.
- The Golempedia lane is additive and offline. If it causes trouble, revert `dev.charles.multigolem.golempedia`, `GolempediaScreen`, and the hub button.
- The status widget refactor should preserve existing `MultiGolemStatus.display()` and status payload behavior. If the hub refactor regresses, restore the previous `MultiGolemStatusScreen` body and reapply only the top-right widget after tests isolate the issue.
- Do not use rollback to remove unrelated user changes. Check `git diff --name-only` before reverting any task.

---

### Task 1: Tighten Mod Menu Source Tests For The Hub And Status Widget

**Files:**
- Modify: `src/test/java/dev/charles/multigolem/status/MultiGolemModMenuSourceTest.java`
- Modify: `src/test/java/dev/charles/multigolem/status/MultiGolemStatusIntegrationSourceTest.java`

- [ ] **Step 1: Write failing source assertions for the hub buttons, top-right status square, and optional Mod Menu boundary**

Replace `modMenuScreenRendersRichCarryBabyAnimalsStyleStatusDetails` with two focused tests:

```java
@Test
void modMenuHubShowsRequiredActionsAndTopRightStatusSquare() throws IOException {
    String api = Files.readString(Path.of("src/client/java/dev/charles/multigolem/client/modmenu/MultiGolemModMenu.java"));
    String screen = Files.readString(Path.of("src/client/java/dev/charles/multigolem/client/modmenu/MultiGolemStatusScreen.java"));
    String widget = Files.readString(Path.of("src/client/java/dev/charles/multigolem/client/modmenu/MultiGolemStatusWidget.java"));

    assertTrue(api.contains("@Environment(EnvType.CLIENT)"));
    assertTrue(api.contains("implements ModMenuApi"));
    assertTrue(api.contains("MultiGolemStatusScreen::new"));
    assertTrue(screen.contains("Button.builder(Component.literal(\"Website\")"));
    assertTrue(screen.contains("Button.builder(Component.literal(\"Issues\")"));
    assertTrue(screen.contains("Button.builder(Component.literal(\"Server Customizations\")"));
    assertTrue(screen.contains("Button.builder(Component.literal(\"Golempedia\")"));
    assertTrue(screen.contains("Button.builder(Component.literal(\"Done\")"));
    assertFalse(screen.contains("Button.builder(Component.literal(\"Cancel\")"));
    assertTrue(screen.contains("MultiGolemStatusWidget.renderTopRight("));
    assertFalse(screen.contains("renderStatusRow(guiGraphics"), "hub must not keep a centered permanent status row");
    assertFalse(screen.contains("display.statusLabel()"), "status label belongs in tooltip only");
    assertTrue(widget.contains("MultiGolemStatus.display()"));
    assertTrue(widget.contains("right - STATUS_SQUARE_SIZE"));
    assertTrue(widget.contains("top"));
    assertTrue(widget.contains("tooltipText(ModStatusDisplay display)"));
    assertTrue(widget.contains("\"Status: \" + display.statusLabel()"));
    assertTrue(widget.contains("\"Client: \" + versionWithBuild(display.clientVersion(), display.clientBuild())"));
    assertTrue(widget.contains("\"Server: \" + versionWithBuild(display.serverVersion(), display.serverBuild())"));
    assertTrue(widget.contains("addHelpTextLines(text, display.helpText())"));
    assertTrue(widget.contains("case TEAL -> 0xFF55FFFF"));
    assertTrue(widget.contains("case TEAL -> ChatFormatting.AQUA"));
}
```

Add a status boundary test:

```java
@Test
void statusPayloadStaysVersionStatusOnlyAndDoesNotCarryServerCustomizations() throws IOException {
    String statusPayload = Files.readString(Path.of("src/main/java/dev/charles/multigolem/status/MultiGolemStatusPayload.java"));
    String status = Files.readString(Path.of("src/main/java/dev/charles/multigolem/status/MultiGolemStatus.java"));
    String statusNetworking = Files.readString(Path.of("src/main/java/dev/charles/multigolem/status/MultiGolemStatusNetworking.java"));

    assertTrue(statusPayload.contains("encodedVersion"));
    assertTrue(statusPayload.contains("ModStatusVersionPayload"));
    assertFalse(statusPayload.contains("healingEnabled"));
    assertFalse(statusPayload.contains("villageSpawnWeights"));
    assertFalse(statusPayload.contains("variantOverrides"));
    assertFalse(status.contains("server_customizations"));
    assertFalse(statusNetworking.contains("ServerCustomizationsPayload"));
}
```

- [ ] **Step 2: Run the failing Mod Menu/status boundary tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.status.MultiGolemModMenuSourceTest --tests dev.charles.multigolem.status.MultiGolemStatusIntegrationSourceTest
```

Expected: FAIL because `MultiGolemStatusWidget.java` does not exist, the screen still has `Cancel`, and status is still rendered as a centered row.

- [ ] **Step 3: Do not change production code in this task**

This task only locks the behavior with failing tests. The next tasks make the tests pass.

### Task 2: Extract The MSK Status Indicator Into A Top-Right Widget

**Files:**
- Create: `src/client/java/dev/charles/multigolem/client/modmenu/MultiGolemStatusWidget.java`
- Modify: `src/client/java/dev/charles/multigolem/client/modmenu/MultiGolemStatusScreen.java`
- Test: `src/test/java/dev/charles/multigolem/status/MultiGolemModMenuSourceTest.java`

- [ ] **Step 1: Create the widget by moving status tooltip and square helpers out of `MultiGolemStatusScreen`**

Create `MultiGolemStatusWidget` with this shape:

```java
package dev.charles.multigolem.client.modmenu;

import dev.charles.multigolem.internal.modstatus.ModStatusDisplay;
import dev.charles.multigolem.internal.modstatus.StatusTone;
import dev.charles.multigolem.status.MultiGolemStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

final class MultiGolemStatusWidget {
    private static final int STATUS_SQUARE_SIZE = 8;
    private static final int STATUS_SQUARE_BORDER_COLOR = 0xFF222222;
    private static final int TOP_MARGIN = 8;
    private static final int RIGHT_MARGIN = 8;

    private MultiGolemStatusWidget() {
    }

    static void renderTopRight(GuiGraphicsExtractor guiGraphics, Font font, int right, int top, int mouseX, int mouseY) {
        ModStatusDisplay display = MultiGolemStatus.display();
        int left = right - RIGHT_MARGIN - STATUS_SQUARE_SIZE;
        int squareTop = top + TOP_MARGIN;

        renderStatusSquare(guiGraphics, display.tone(), left, squareTop);
        if (isHoveringStatus(left, squareTop, mouseX, mouseY)) {
            guiGraphics.setComponentTooltipForNextFrame(font, tooltipLines(display), mouseX, mouseY);
        }
    }

    private static void renderStatusSquare(GuiGraphicsExtractor guiGraphics, StatusTone tone, int left, int top) {
        guiGraphics.fill(left, top, left + STATUS_SQUARE_SIZE, top + STATUS_SQUARE_SIZE, STATUS_SQUARE_BORDER_COLOR);
        guiGraphics.fill(left + 1, top + 1, left + STATUS_SQUARE_SIZE - 1, top + STATUS_SQUARE_SIZE - 1, toneColor(tone));
    }

    private static int toneColor(StatusTone tone) {
        return switch (tone) {
            case GREEN -> 0xFF55FF55;
            case TEAL -> 0xFF55FFFF;
            case ORANGE -> 0xFFFFAA00;
            case RED -> 0xFFFF5555;
            case GRAY -> 0xFFAAAAAA;
        };
    }

    private static ChatFormatting formattingFor(StatusTone tone) {
        return switch (tone) {
            case GREEN -> ChatFormatting.GREEN;
            case TEAL -> ChatFormatting.AQUA;
            case ORANGE -> ChatFormatting.GOLD;
            case RED -> ChatFormatting.RED;
            case GRAY -> ChatFormatting.GRAY;
        };
    }

    private static List<Component> tooltipLines(ModStatusDisplay display) {
        return tooltipText(display).stream()
            .<Component>map(text -> Component.literal(text).withStyle(formattingFor(display.tone())))
            .toList();
    }

    static List<String> tooltipText(ModStatusDisplay display) {
        List<String> text = new ArrayList<>();
        text.add(display.displayName());
        text.add("Status: " + display.statusLabel());
        text.add("Client: " + versionWithBuild(display.clientVersion(), display.clientBuild()));
        text.add("Server: " + versionWithBuild(display.serverVersion(), display.serverBuild()));
        addHelpTextLines(text, display.helpText());
        return text;
    }

    private static void addHelpTextLines(List<String> text, String helpText) {
        if (helpText == null || helpText.isBlank()) {
            return;
        }

        for (String sentence : helpText.split("(?<=\\.)\\s+")) {
            String trimmed = sentence.trim();
            if (!trimmed.isEmpty()) {
                text.add(trimmed);
            }
        }
    }

    private static String versionWithBuild(String version, String build) {
        if (version == null || version.isBlank()) {
            return "Unknown";
        }
        return build == null || build.isBlank() || "dev".equalsIgnoreCase(build) ? version : version + "+" + build;
    }

    private static boolean isHoveringStatus(int left, int top, int mouseX, int mouseY) {
        return mouseX >= left
            && mouseX <= left + STATUS_SQUARE_SIZE
            && mouseY >= top
            && mouseY <= top + STATUS_SQUARE_SIZE;
    }
}
```

- [ ] **Step 2: Call the widget from the hub screen render path**

Inside `MultiGolemStatusScreen.extractRenderState`, keep the `super` call and replace the centered row calculation with:

```java
MultiGolemStatusWidget.renderTopRight(guiGraphics, font, width, 0, mouseX, mouseY);
```

Remove the moved status row helper methods and unused imports from `MultiGolemStatusScreen`.

- [ ] **Step 3: Run targeted source tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.status.MultiGolemModMenuSourceTest
```

Expected: FAIL until Task 3 adds hub buttons and removes `Cancel`; then PASS.

### Task 3: Build The Mod Menu Hub Screen

**Files:**
- Modify: `src/client/java/dev/charles/multigolem/client/modmenu/MultiGolemStatusScreen.java`
- Create: `src/client/java/dev/charles/multigolem/client/modmenu/ServerCustomizationsScreen.java`
- Create: `src/client/java/dev/charles/multigolem/client/modmenu/GolempediaScreen.java`
- Test: `src/test/java/dev/charles/multigolem/status/MultiGolemModMenuSourceTest.java`

- [ ] **Step 1: Update the hub init method**

Use a simple vertical stack of five native buttons:

```java
@Override
protected void init() {
    int buttonWidth = Math.min(220, width - 32);
    int left = (width - buttonWidth) / 2;
    int top = Math.max(36, height / 2 - 58);
    int step = 24;

    addRenderableWidget(Button.builder(Component.literal("Website"), button ->
        Util.getPlatform().openUri("https://modrinth.com/mod/multigolem")
    ).bounds(left, top, buttonWidth, 20).build());

    addRenderableWidget(Button.builder(Component.literal("Issues"), button ->
        Util.getPlatform().openUri("https://github.com/TnTBass/MultiGolem/issues")
    ).bounds(left, top + step, buttonWidth, 20).build());

    addRenderableWidget(Button.builder(Component.literal("Server Customizations"), button ->
        Minecraft.getInstance().setScreen(new ServerCustomizationsScreen(this))
    ).bounds(left, top + step * 2, buttonWidth, 20).build());

    addRenderableWidget(Button.builder(Component.literal("Golempedia"), button ->
        Minecraft.getInstance().setScreen(new GolempediaScreen(this))
    ).bounds(left, top + step * 3, buttonWidth, 20).build());

    addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
        .bounds(left, top + step * 4, buttonWidth, 20)
        .build());
}
```

Add imports:

```java
import net.minecraft.Util;
```

The exact website URL should match current release metadata if the project already declares a project homepage elsewhere. If a different canonical website is in `fabric.mod.json`, use that value instead of adding a new link surface.

- [ ] **Step 2: Add placeholder screens so hub navigation compiles**

Create `ServerCustomizationsScreen`:

```java
package dev.charles.multigolem.client.modmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ServerCustomizationsScreen extends Screen {
    private static final Component TITLE = Component.literal("Server Customizations");
    private static final Component UNAVAILABLE = Component.literal("No server customizations are available.");
    private final Screen parent;

    public ServerCustomizationsScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(200, width - 32);
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
            .bounds((width - buttonWidth) / 2, height - 28, buttonWidth, 20)
            .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, tickDelta);
        guiGraphics.centeredText(font, TITLE, width / 2, 20, 0xFFFFFFFF);
        guiGraphics.centeredText(font, UNAVAILABLE, width / 2, height / 2 - 5, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
```

Create `GolempediaScreen`:

```java
package dev.charles.multigolem.client.modmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GolempediaScreen extends Screen {
    private static final Component TITLE = Component.literal("Golempedia");
    private final Screen parent;

    public GolempediaScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(200, width - 32);
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
            .bounds((width - buttonWidth) / 2, height - 28, buttonWidth, 20)
            .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, tickDelta);
        guiGraphics.centeredText(font, TITLE, width / 2, 20, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
```

- [ ] **Step 3: Run hub source tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.status.MultiGolemModMenuSourceTest
```

Expected: PASS for hub/status-square source structure. If the URL assertions are added, update them only to match existing metadata, not to invent extra link surfaces.

### Task 4: Add Server Customizations Client State Tests First

**Files:**
- Create: `src/test/java/dev/charles/multigolem/customizations/ServerCustomizationsClientStateTest.java`
- Create later: `src/client/java/dev/charles/multigolem/client/customizations/ServerCustomizationsClientState.java`

- [ ] **Step 1: Write state transition tests**

Create:

```java
package dev.charles.multigolem.customizations;

import dev.charles.multigolem.client.customizations.ServerCustomizationsClientState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerCustomizationsClientStateTest {
    @Test
    void disconnectedStartsUnavailable() {
        ServerCustomizationsClientState state = new ServerCustomizationsClientState(100);

        assertEquals(ServerCustomizationsClientState.ViewState.UNAVAILABLE, state.viewState());
        assertTrue(state.summary().isEmpty());
    }

    @Test
    void pendingFallsBackToUnavailableAfterTimeout() {
        ServerCustomizationsClientState state = new ServerCustomizationsClientState(3);

        state.onJoin();
        assertEquals(ServerCustomizationsClientState.ViewState.PENDING, state.viewState());

        state.tick();
        state.tick();
        assertEquals(ServerCustomizationsClientState.ViewState.PENDING, state.viewState());

        state.tick();
        assertEquals(ServerCustomizationsClientState.ViewState.UNAVAILABLE, state.viewState());
        assertTrue(state.summary().isEmpty());
    }

    @Test
    void pendingResolvesUnavailableOnDisconnect() {
        ServerCustomizationsClientState state = new ServerCustomizationsClientState(100);

        state.onJoin();
        state.onDisconnect();

        assertEquals(ServerCustomizationsClientState.ViewState.UNAVAILABLE, state.viewState());
        assertTrue(state.summary().isEmpty());
    }

    @Test
    void pendingResolvesUnavailableOnScreenClose() {
        ServerCustomizationsClientState state = new ServerCustomizationsClientState(100);

        state.onJoin();
        state.onScreenClose();

        assertEquals(ServerCustomizationsClientState.ViewState.UNAVAILABLE, state.viewState());
        assertTrue(state.summary().isEmpty());
    }

    @Test
    void receivedSummaryBecomesAvailableAndDisconnectClearsIt() {
        ServerCustomizationsClientState state = new ServerCustomizationsClientState(100);
        ServerCustomizationsSummary summary = new ServerCustomizationsSummary(
            java.util.List.of("Global healing: enabled"),
            java.util.List.of(),
            java.util.List.of(),
            java.util.List.of()
        );

        state.onJoin();
        state.onServerSummary(summary);

        assertEquals(ServerCustomizationsClientState.ViewState.AVAILABLE, state.viewState());
        assertEquals(summary, state.summary().orElseThrow());

        state.onDisconnect();
        assertEquals(ServerCustomizationsClientState.ViewState.UNAVAILABLE, state.viewState());
        assertTrue(state.summary().isEmpty());
    }
}
```

This explicitly covers the Revue-fixed spec point: pending/loading state resolves to unavailable on timeout, disconnect, and screen close.

- [ ] **Step 2: Run the failing state tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.customizations.ServerCustomizationsClientStateTest
```

Expected: FAIL because the state and summary classes do not exist.

### Task 5: Implement Server Customizations Client State And Summary Models

**Files:**
- Create: `src/client/java/dev/charles/multigolem/client/customizations/ServerCustomizationsClientState.java`
- Create: `src/main/java/dev/charles/multigolem/customizations/ServerCustomizationsSummary.java`
- Test: `src/test/java/dev/charles/multigolem/customizations/ServerCustomizationsClientStateTest.java`

- [ ] **Step 1: Add summary record**

```java
package dev.charles.multigolem.customizations;

import java.util.List;
import java.util.Objects;

public record ServerCustomizationsSummary(
    List<String> globalLines,
    List<String> villageLines,
    List<String> zombieVillageLines,
    List<String> variantLines
) {
    public ServerCustomizationsSummary {
        Objects.requireNonNull(globalLines, "globalLines");
        Objects.requireNonNull(villageLines, "villageLines");
        Objects.requireNonNull(zombieVillageLines, "zombieVillageLines");
        Objects.requireNonNull(variantLines, "variantLines");
        globalLines = List.copyOf(globalLines);
        villageLines = List.copyOf(villageLines);
        zombieVillageLines = List.copyOf(zombieVillageLines);
        variantLines = List.copyOf(variantLines);
    }

    public boolean isEmpty() {
        return globalLines.isEmpty()
            && villageLines.isEmpty()
            && zombieVillageLines.isEmpty()
            && variantLines.isEmpty();
    }
}
```

- [ ] **Step 2: Add state holder**

```java
package dev.charles.multigolem.client.customizations;

import dev.charles.multigolem.customizations.ServerCustomizationsSummary;

import java.util.Optional;

public final class ServerCustomizationsClientState {
    public enum ViewState {
        UNAVAILABLE,
        PENDING,
        AVAILABLE
    }

    private final int timeoutTicks;
    private ViewState viewState = ViewState.UNAVAILABLE;
    private int pendingTicks;
    private ServerCustomizationsSummary summary;

    public ServerCustomizationsClientState(int timeoutTicks) {
        if (timeoutTicks < 1) {
            throw new IllegalArgumentException("timeoutTicks must be positive");
        }
        this.timeoutTicks = timeoutTicks;
    }

    public void onJoin() {
        viewState = ViewState.PENDING;
        pendingTicks = 0;
        summary = null;
    }

    public void onServerSummary(ServerCustomizationsSummary summary) {
        this.summary = summary;
        this.viewState = ViewState.AVAILABLE;
        this.pendingTicks = 0;
    }

    public void tick() {
        if (viewState != ViewState.PENDING) {
            return;
        }
        pendingTicks++;
        if (pendingTicks >= timeoutTicks) {
            clear();
        }
    }

    public void onDisconnect() {
        clear();
    }

    public void onScreenClose() {
        if (viewState == ViewState.PENDING) {
            clear();
        }
    }

    public ViewState viewState() {
        return viewState;
    }

    public Optional<ServerCustomizationsSummary> summary() {
        return Optional.ofNullable(summary);
    }

    private void clear() {
        viewState = ViewState.UNAVAILABLE;
        pendingTicks = 0;
        summary = null;
    }
}
```

- [ ] **Step 3: Run state tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.customizations.ServerCustomizationsClientStateTest
```

Expected: PASS.

### Task 6: Define Server Customizations Snapshot And Payload Tests First

**Files:**
- Create: `src/test/java/dev/charles/multigolem/customizations/ServerCustomizationsPayloadTest.java`
- Create later: `src/main/java/dev/charles/multigolem/customizations/ServerCustomizationsPayload.java`
- Create later: `src/main/java/dev/charles/multigolem/customizations/ServerCustomizationsSnapshot.java`
- Create later: `src/main/java/dev/charles/multigolem/customizations/VariantCustomizationSummary.java`

- [ ] **Step 1: Write payload/channel tests**

Create:

```java
package dev.charles.multigolem.customizations;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.status.MultiGolemStatus;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServerCustomizationsPayloadTest {
    @Test
    void customizationsChannelIsSeparateFromStatusChannel() {
        assertEquals(Identifier.fromNamespaceAndPath("multigolem", "server_customizations"), ServerCustomizationsPayload.ID);
        assertEquals("mod_status/server_version", MultiGolemStatus.PAYLOAD_PATH);
        assertNotEquals(MultiGolemStatus.PAYLOAD_PATH, ServerCustomizationsPayload.ID.getPath());
        assertNotEquals("multigolem:" + MultiGolemStatus.PAYLOAD_PATH, ServerCustomizationsPayload.ID.toString());
    }

    @Test
    void payloadPreservesSemanticSnapshot() {
        EnumMap<GolemVariant, Integer> weights = new EnumMap<>(GolemVariant.class);
        weights.put(GolemVariant.IRON, 19);
        weights.put(GolemVariant.COPPER, 22);
        ServerCustomizationsSnapshot snapshot = new ServerCustomizationsSnapshot(
            true,
            true,
            weights,
            false,
            "permission checks use LuckPerms when present",
            List.of(new VariantCustomizationSummary(GolemVariant.COPPER, List.of("Village spawn weight is 22 instead of 19")))
        );

        ServerCustomizationsPayload payload = new ServerCustomizationsPayload(snapshot);

        assertEquals(snapshot, payload.snapshot());
        assertEquals(ServerCustomizationsPayload.TYPE, payload.type());
        assertEquals(22, payload.snapshot().villageSpawnWeights().get(GolemVariant.COPPER));
        assertEquals(19, payload.snapshot().villageSpawnWeights().get(GolemVariant.IRON));
    }

    @Test
    void snapshotDefensivelyCopiesCollections() {
        EnumMap<GolemVariant, Integer> weights = new EnumMap<>(GolemVariant.class);
        weights.put(GolemVariant.COPPER, 3);
        ServerCustomizationsSnapshot snapshot = new ServerCustomizationsSnapshot(
            true,
            true,
            weights,
            true,
            "default permissions",
            List.of()
        );

        weights.put(GolemVariant.COPPER, 99);

        assertEquals(3, snapshot.villageSpawnWeights().get(GolemVariant.COPPER));
        assertThrows(UnsupportedOperationException.class,
            () -> snapshot.variantOverrides().add(new VariantCustomizationSummary(GolemVariant.GOLD, List.of("changed"))));
    }
}
```

- [ ] **Step 2: Run failing payload tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.customizations.ServerCustomizationsPayloadTest
```

Expected: FAIL because payload/snapshot classes do not exist.

### Task 7: Implement Server Customizations Snapshot And Payload

**Files:**
- Create: `src/main/java/dev/charles/multigolem/customizations/VariantCustomizationSummary.java`
- Create: `src/main/java/dev/charles/multigolem/customizations/ServerCustomizationsSnapshot.java`
- Create: `src/main/java/dev/charles/multigolem/customizations/ServerCustomizationsPayload.java`
- Test: `src/test/java/dev/charles/multigolem/customizations/ServerCustomizationsPayloadTest.java`

- [ ] **Step 1: Add variant summary record**

```java
package dev.charles.multigolem.customizations;

import dev.charles.multigolem.GolemVariant;

import java.util.List;
import java.util.Objects;

public record VariantCustomizationSummary(GolemVariant variant, List<String> lines) {
    public VariantCustomizationSummary {
        Objects.requireNonNull(variant, "variant");
        Objects.requireNonNull(lines, "lines");
        lines = List.copyOf(lines);
    }
}
```

- [ ] **Step 2: Add server snapshot record**

```java
package dev.charles.multigolem.customizations;

import dev.charles.multigolem.GolemVariant;

import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

public record ServerCustomizationsSnapshot(
    boolean healingEnabled,
    boolean villageSpawnsEnabled,
    EnumMap<GolemVariant, Integer> villageSpawnWeights,
    boolean zombieVillageSpawningEnabled,
    String permissionsMode,
    List<VariantCustomizationSummary> variantOverrides
) {
    public ServerCustomizationsSnapshot {
        Objects.requireNonNull(villageSpawnWeights, "villageSpawnWeights");
        Objects.requireNonNull(permissionsMode, "permissionsMode");
        Objects.requireNonNull(variantOverrides, "variantOverrides");
        villageSpawnWeights = new EnumMap<>(villageSpawnWeights);
        variantOverrides = List.copyOf(variantOverrides);
    }

    @Override
    public EnumMap<GolemVariant, Integer> villageSpawnWeights() {
        return new EnumMap<>(villageSpawnWeights);
    }
}
```

- [ ] **Step 3: Add initial custom payload**

Implement `ServerCustomizationsPayload` as a `CustomPacketPayload` with:

```java
public static final Identifier ID = Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID, "server_customizations");
public static final Type<ServerCustomizationsPayload> TYPE = new Type<>(ID);
```

Use a `StreamCodec<RegistryFriendlyByteBuf, ServerCustomizationsPayload>` that writes semantic fields, not raw JSON:

```java
private static void write(RegistryFriendlyByteBuf buf, ServerCustomizationsPayload payload) {
    ServerCustomizationsSnapshot snapshot = payload.snapshot();
    buf.writeBoolean(snapshot.healingEnabled());
    buf.writeBoolean(snapshot.villageSpawnsEnabled());
    buf.writeVarInt(snapshot.villageSpawnWeights().size());
    for (Map.Entry<GolemVariant, Integer> entry : snapshot.villageSpawnWeights().entrySet()) {
        buf.writeUtf(entry.getKey().id());
        buf.writeVarInt(entry.getValue());
    }
    buf.writeBoolean(snapshot.zombieVillageSpawningEnabled());
    buf.writeUtf(snapshot.permissionsMode(), 256);
    buf.writeVarInt(snapshot.variantOverrides().size());
    for (VariantCustomizationSummary variant : snapshot.variantOverrides()) {
        buf.writeUtf(variant.variant().id());
        buf.writeVarInt(variant.lines().size());
        for (String line : variant.lines()) {
            buf.writeUtf(line, 512);
        }
    }
}
```

The `read` method should mirror the writer, call `GolemVariant.fromId`, reject unknown variant IDs with `IllegalArgumentException`, cap list counts to a small implementation limit such as 64 lines, and create a new `ServerCustomizationsSnapshot`.

- [ ] **Step 4: Run payload tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.customizations.ServerCustomizationsPayloadTest
```

Expected: PASS. If Minecraft buffer APIs require exact method name adjustments, keep the same schema and update only method calls.

### Task 8: Summarize Server Config Differences From Defaults

**Files:**
- Create: `src/test/java/dev/charles/multigolem/config/ServerCustomizationsSummarizerTest.java`
- Create: `src/main/java/dev/charles/multigolem/customizations/ServerCustomizationsSummarizer.java`
- Modify: `src/main/java/dev/charles/multigolem/customizations/ServerCustomizationsSnapshot.java`
- Modify: `src/main/java/dev/charles/multigolem/config/MultiGolemConfig.java`

- [ ] **Step 1: Write summarizer tests**

Create tests that verify default snapshots are concise and non-default snapshots report meaningful differences:

```java
package dev.charles.multigolem.config;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.customizations.ServerCustomizationsSnapshot;
import dev.charles.multigolem.customizations.ServerCustomizationsSummarizer;
import dev.charles.multigolem.customizations.ServerCustomizationsSummary;
import dev.charles.multigolem.customizations.VariantCustomizationSummary;
import dev.charles.multigolem.spawn.VillageSpawnWeights;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServerCustomizationsSummarizerTest {
    @Test
    void defaultConfigSnapshotReportsNoVariantOverrides() {
        ServerCustomizationsSnapshot snapshot = ServerCustomizationsSummarizer.snapshot(MultiGolemConfig.defaults());

        assertTrue(snapshot.healingEnabled());
        assertTrue(snapshot.villageSpawnsEnabled());
        assertTrue(snapshot.zombieVillageSpawningEnabled());
        assertTrue(snapshot.variantOverrides().isEmpty());
    }

    @Test
    void nonDefaultVillageWeightIsReported() {
        EnumMap<GolemVariant, Integer> weights = VillageSpawnWeights.defaults().weights();
        weights.put(GolemVariant.COPPER, VillageSpawnWeights.defaults().weight(GolemVariant.COPPER) + 5);
        MultiGolemConfig customized = MultiGolemConfig.forTesting(
            MultiGolemConfig.defaults().allowGolemHealing(),
            MultiGolemConfig.defaults().tiersForTesting(),
            new VillageSpawnWeights(true, weights),
            MultiGolemConfig.defaults().zombieVillageSpawning()
        );

        ServerCustomizationsSummary summary = ServerCustomizationsSummarizer.summary(customized);

        assertTrue(summary.villageLines().stream().anyMatch(line -> line.contains("Copper")));
        assertTrue(summary.villageLines().stream().anyMatch(line -> line.contains("24")));
    }

    @Test
    void snapshotSummaryOverloadSupportsClientPayloadSnapshots() {
        EnumMap<GolemVariant, Integer> weights = VillageSpawnWeights.defaults().weights();
        weights.put(GolemVariant.GOLD, 7);
        ServerCustomizationsSnapshot snapshot = new ServerCustomizationsSnapshot(
            false,
            true,
            weights,
            true,
            "permission checks use the server's configured permission provider when present",
            List.of(new VariantCustomizationSummary(GolemVariant.GOLD, List.of("Gold speed multiplier differs from defaults")))
        );

        ServerCustomizationsSummary summary = ServerCustomizationsSummarizer.summary(snapshot);

        assertTrue(summary.globalLines().stream().anyMatch(line -> line.contains("disabled")));
        assertTrue(summary.villageLines().stream().anyMatch(line -> line.contains("Gold")));
        assertTrue(summary.variantLines().stream().anyMatch(line -> line.contains("Gold speed multiplier")));
    }
}
```

- [ ] **Step 2: Run failing summarizer tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.config.ServerCustomizationsSummarizerTest
```

Expected: FAIL until summarizer/factory support exists.

- [ ] **Step 3: Add package-private test factory accessors**

In `MultiGolemConfig`, add package-private helpers near the existing accessors:

```java
Map<GolemVariant, TierStats> tiersForTesting() {
    return new EnumMap<>(tiers);
}

static MultiGolemConfig forTesting(
    boolean allowGolemHealing,
    Map<GolemVariant, TierStats> tiers,
    VillageSpawnWeights villageSpawnWeights,
    ZombieVillageSpawningConfig zombieVillageSpawning
) {
    return new MultiGolemConfig(allowGolemHealing, tiers, villageSpawnWeights, zombieVillageSpawning);
}
```

- [ ] **Step 4: Implement summarizer**

Implement:

```java
public final class ServerCustomizationsSummarizer {
    private ServerCustomizationsSummarizer() {
    }

    public static ServerCustomizationsSnapshot snapshot(MultiGolemConfig config) {
        MultiGolemConfig defaults = MultiGolemConfig.defaults();
        return new ServerCustomizationsSnapshot(
            config.allowGolemHealing(),
            config.villageSpawnWeights().enabled(),
            config.villageSpawnWeights().weights(),
            config.zombieVillageSpawning().enabled(),
            "permission checks use the server's configured permission provider when present",
            variantOverrides(config, defaults)
        );
    }

    public static ServerCustomizationsSummary summary(MultiGolemConfig config) {
        return summary(snapshot(config), MultiGolemConfig.defaults());
    }

    public static ServerCustomizationsSummary summary(ServerCustomizationsSnapshot snapshot) {
        return summary(snapshot, MultiGolemConfig.defaults());
    }
}
```

`summary(ServerCustomizationsSnapshot snapshot)` is the overload used by the client receiver after decoding `ServerCustomizationsPayload`. The private `summary(ServerCustomizationsSnapshot snapshot, MultiGolemConfig defaults)` helper should group:
- Global: healing enabled/disabled.
- Village: village spawn weighting enabled/disabled and only non-default weights.
- Zombie village: enabled/disabled and non-default limits.
- Variant: non-default player-meaningful tier values, grouped as readable lines.

Do not dump every config field. Include only differences from defaults or globally useful enabled/disabled state.

- [ ] **Step 5: Run summarizer tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.config.ServerCustomizationsSummarizerTest
```

Expected: PASS.

### Task 9: Register Server Customizations Networking Separately From MSK

**Files:**
- Create: `src/test/java/dev/charles/multigolem/customizations/ServerCustomizationsNetworkingSourceTest.java`
- Create: `src/main/java/dev/charles/multigolem/customizations/ServerCustomizationsNetworking.java`
- Create: `src/client/java/dev/charles/multigolem/client/customizations/ServerCustomizationsClient.java`
- Modify: `src/main/java/dev/charles/multigolem/MultiGolem.java`
- Modify: `src/client/java/dev/charles/multigolem/client/MultiGolemClient.java`

- [ ] **Step 1: Write source guards for capability-gated send and lifecycle clear**

Create:

```java
package dev.charles.multigolem.customizations;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ServerCustomizationsNetworkingSourceTest {
    @Test
    void serverCustomizationsNetworkingUsesOwnPayloadAndCapabilityGate() throws IOException {
        String source = Files.readString(Path.of("src/main/java/dev/charles/multigolem/customizations/ServerCustomizationsNetworking.java"));
        String main = Files.readString(Path.of("src/main/java/dev/charles/multigolem/MultiGolem.java"));

        assertTrue(source.contains("PayloadTypeRegistry.clientboundPlay().register(ServerCustomizationsPayload.TYPE"));
        assertTrue(source.contains("ServerPlayConnectionEvents.JOIN"));
        assertTrue(source.contains("ServerPlayNetworking.canSend(player, ServerCustomizationsPayload.TYPE)"));
        assertTrue(source.contains("ServerPlayNetworking.send(player, new ServerCustomizationsPayload"));
        assertTrue(source.contains("ServerCustomizationsSummarizer.snapshot"));
        assertFalse(source.contains("MultiGolemStatusPayload"));
        assertTrue(main.contains("ServerCustomizationsNetworking.registerServer();"));
    }

    @Test
    void clientCustomizationsReceiverClearsOnDisconnectAndTicksTimeout() throws IOException {
        String source = Files.readString(Path.of("src/client/java/dev/charles/multigolem/client/customizations/ServerCustomizationsClient.java"));
        String client = Files.readString(Path.of("src/client/java/dev/charles/multigolem/client/MultiGolemClient.java"));

        assertTrue(source.contains("ClientPlayNetworking.registerGlobalReceiver(ServerCustomizationsPayload.TYPE"));
        assertTrue(source.contains("ServerCustomizationsSummarizer.summary(payload.snapshot())"));
        assertTrue(source.contains("ClientPlayConnectionEvents.JOIN"));
        assertTrue(source.contains("ClientPlayConnectionEvents.DISCONNECT"));
        assertTrue(source.contains("ClientTickEvents.END_CLIENT_TICK"));
        assertTrue(source.contains("ClientLifecycleEvents.CLIENT_STOPPING"));
        assertTrue(source.contains("STATE.onDisconnect()"));
        assertTrue(source.contains("STATE.tick()"));
        assertTrue(client.contains("ServerCustomizationsClient.register();"));
    }
}
```

- [ ] **Step 2: Run failing networking source tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.customizations.ServerCustomizationsNetworkingSourceTest
```

Expected: FAIL because networking classes are not registered.

- [ ] **Step 3: Implement server networking**

Create `ServerCustomizationsNetworking` that mirrors status networking but uses the customizations payload:

```java
public final class ServerCustomizationsNetworking {
    private static boolean registered;

    public static void registerServer() {
        if (registered) {
            return;
        }
        registered = true;
        PayloadTypeRegistry.clientboundPlay().register(ServerCustomizationsPayload.TYPE, ServerCustomizationsPayload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendServerCustomizations(handler.player));
    }

    static boolean sendServerCustomizations(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, ServerCustomizationsPayload.TYPE)) {
            MultiGolem.LOG.debug("Skipping MultiGolem customizations payload; client did not advertise {}", ServerCustomizationsPayload.ID);
            return false;
        }
        ServerPlayNetworking.send(player, new ServerCustomizationsPayload(
            ServerCustomizationsSummarizer.snapshot(MultiGolem.config())
        ));
        return true;
    }
}
```

Use the actual live config accessor name from `MultiGolem`. If there is no accessor, add a package-visible `MultiGolem.config()` or equivalent read-only accessor and test it.

- [ ] **Step 4: Implement client receiver**

Create `ServerCustomizationsClient` with a singleton state:

```java
public final class ServerCustomizationsClient {
    private static final int TIMEOUT_TICKS = 100;
    private static final ServerCustomizationsClientState STATE = new ServerCustomizationsClientState(TIMEOUT_TICKS);
    private static volatile boolean registered;

    public static ServerCustomizationsClientState state() {
        return STATE;
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        ClientPlayNetworking.registerGlobalReceiver(ServerCustomizationsPayload.TYPE, (payload, context) ->
            context.client().execute(() -> STATE.onServerSummary(ServerCustomizationsSummarizer.summary(payload.snapshot())))
        );
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> STATE.onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> STATE.onDisconnect());
        ClientTickEvents.END_CLIENT_TICK.register(client -> STATE.tick());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> STATE.onDisconnect());
    }
}
```

This preserves vanilla-client compatibility because the server sends only when `ServerPlayNetworking.canSend` reports client support.

- [ ] **Step 5: Register initializers**

In `MultiGolem.onInitialize`, after status registration, add:

```java
ServerCustomizationsNetworking.registerServer();
```

In `MultiGolemClient.onInitializeClient`, after status client registration, add:

```java
ServerCustomizationsClient.register();
```

- [ ] **Step 6: Run customizations networking and status boundary tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.customizations.ServerCustomizationsNetworkingSourceTest --tests dev.charles.multigolem.status.MultiGolemStatusIntegrationSourceTest
```

Expected: PASS. If source strings need small updates for formatter/import differences, keep the asserted boundaries: own payload, capability gate, lifecycle clear, and no MSK payload reuse.

### Task 10: Wire Server Customizations UI To State

**Files:**
- Modify: `src/client/java/dev/charles/multigolem/client/modmenu/ServerCustomizationsScreen.java`
- Modify: `src/test/java/dev/charles/multigolem/status/MultiGolemModMenuSourceTest.java`

- [ ] **Step 1: Add source assertions for unavailable/loading/available rendering and close clearing**

Add to `MultiGolemModMenuSourceTest`:

```java
@Test
void serverCustomizationsScreenRendersUnavailableAndClearsPendingOnClose() throws IOException {
    String screen = Files.readString(Path.of("src/client/java/dev/charles/multigolem/client/modmenu/ServerCustomizationsScreen.java"));

    assertTrue(screen.contains("ServerCustomizationsClient.state()"));
    assertTrue(screen.contains("No server customizations are available."));
    assertTrue(screen.contains("Loading server customizations"));
    assertTrue(screen.contains("ViewState.UNAVAILABLE"));
    assertTrue(screen.contains("ViewState.PENDING"));
    assertTrue(screen.contains("ViewState.AVAILABLE"));
    assertTrue(screen.contains("onScreenClose()"));
    assertFalse(screen.contains("configure MultiGolem"));
    assertFalse(screen.contains("Button.builder(Component.literal(\"Copy"));
}
```

- [ ] **Step 2: Run the failing UI source test**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.status.MultiGolemModMenuSourceTest
```

Expected: FAIL until the screen reads client state.

- [ ] **Step 3: Render unavailable, pending, and available state**

Update `ServerCustomizationsScreen.extractRenderState` to read:

```java
ServerCustomizationsClientState state = ServerCustomizationsClient.state();
switch (state.viewState()) {
    case UNAVAILABLE -> renderCenteredLine(guiGraphics, UNAVAILABLE, height / 2 - 5, 0xFFAAAAAA);
    case PENDING -> renderCenteredLine(guiGraphics, Component.literal("Loading server customizations..."), height / 2 - 5, 0xFFAAAAAA);
    case AVAILABLE -> state.summary().ifPresentOrElse(
        summary -> renderSummary(guiGraphics, summary),
        () -> renderCenteredLine(guiGraphics, UNAVAILABLE, height / 2 - 5, 0xFFAAAAAA)
    );
}
```

Add `renderSummary` groups for Global, Village Spawns, Zombie Village Spawns, and Variants. Render only text lines from `ServerCustomizationsSummary`; do not expose raw config keys or controls.

Update `onClose`:

```java
ServerCustomizationsClient.state().onScreenClose();
Minecraft.getInstance().setScreen(parent);
```

- [ ] **Step 4: Run Mod Menu source tests and state tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.status.MultiGolemModMenuSourceTest --tests dev.charles.multigolem.customizations.ServerCustomizationsClientStateTest
```

Expected: PASS.

### Task 11: Add Golempedia Catalog Tests First

**Files:**
- Create: `src/test/java/dev/charles/multigolem/golempedia/GolempediaCatalogTest.java`
- Create later: `src/main/java/dev/charles/multigolem/golempedia/GolempediaEntry.java`
- Create later: `src/main/java/dev/charles/multigolem/golempedia/GolempediaCatalog.java`

- [ ] **Step 1: Write catalog coverage and content tests**

Create:

```java
package dev.charles.multigolem.golempedia;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.catalog.GolemVariantCatalog;
import dev.charles.multigolem.catalog.GolemVariantSpec;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GolempediaCatalogTest {
    @Test
    void includesEveryNonIronCatalogVariantRegardlessOfBuildableOrSpawnEggFlags() {
        EnumSet<GolemVariant> expected = GolemVariantCatalog.entries().stream()
            .map(GolemVariantSpec::variant)
            .filter(variant -> variant != GolemVariant.IRON)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(GolemVariant.class)));
        EnumSet<GolemVariant> actual = GolempediaCatalog.entries().stream()
            .map(GolempediaEntry::variant)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(GolemVariant.class)));

        assertEquals(expected, actual);
    }

    @Test
    void eachEntryHasRequiredPlayerFacingFields() {
        for (GolempediaEntry entry : GolempediaCatalog.entries()) {
            assertFalse(entry.displayName().isBlank(), entry.variant() + " display name");
            assertFalse(entry.creationSummary().isBlank(), entry.variant() + " creation");
            assertFalse(entry.healingItem().isBlank(), entry.variant() + " healing");
            assertFalse(entry.dropSummary().isBlank(), entry.variant() + " drops");
            assertFalse(entry.spawnEggSummary().isBlank(), entry.variant() + " spawn egg");
            assertFalse(entry.villageSpawnSummary().isBlank(), entry.variant() + " village spawn");
            assertFalse(entry.coreAbility().isBlank(), entry.variant() + " ability");
            assertFalse(entry.caveats().isEmpty(), entry.variant() + " caveats");
        }
    }

    @Test
    void golempediaDoesNotDependOnServerCustomizationsState() throws IOException {
        String source = Files.readString(Path.of("src/main/java/dev/charles/multigolem/golempedia/GolempediaCatalog.java"));

        assertTrue(source.contains("GolemVariantCatalog.entries()"));
        assertTrue(source.contains("MultiGolemConfig.defaults()"));
        assertFalse(source.contains("ServerCustomizations"));
        assertFalse(source.contains("MultiGolemStatus"));
    }
}
```

Add the missing imports:

```java
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
```

This explicitly covers the Revue-fixed spec point: every non-iron variant represented in `GolemVariantCatalog` is included regardless of buildable/spawn-egg flags.

- [ ] **Step 2: Run failing Golempedia tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.golempedia.GolempediaCatalogTest
```

Expected: FAIL because Golempedia classes do not exist.

### Task 12: Implement Static Golempedia View Models

**Files:**
- Create: `src/main/java/dev/charles/multigolem/golempedia/GolempediaEntry.java`
- Create: `src/main/java/dev/charles/multigolem/golempedia/GolempediaCatalog.java`
- Test: `src/test/java/dev/charles/multigolem/golempedia/GolempediaCatalogTest.java`

- [ ] **Step 1: Add entry record**

```java
package dev.charles.multigolem.golempedia;

import dev.charles.multigolem.GolemVariant;

import java.util.List;
import java.util.Objects;

public record GolempediaEntry(
    GolemVariant variant,
    String displayName,
    String creationSummary,
    String healingItem,
    String dropSummary,
    String spawnEggSummary,
    String villageSpawnSummary,
    String coreAbility,
    List<String> caveats
) {
    public GolempediaEntry {
        Objects.requireNonNull(variant, "variant");
        displayName = requireText(displayName, "displayName");
        creationSummary = requireText(creationSummary, "creationSummary");
        healingItem = requireText(healingItem, "healingItem");
        dropSummary = requireText(dropSummary, "dropSummary");
        spawnEggSummary = requireText(spawnEggSummary, "spawnEggSummary");
        villageSpawnSummary = requireText(villageSpawnSummary, "villageSpawnSummary");
        coreAbility = requireText(coreAbility, "coreAbility");
        Objects.requireNonNull(caveats, "caveats");
        caveats = List.copyOf(caveats);
        if (caveats.isEmpty()) {
            throw new IllegalArgumentException("caveats must not be empty");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
```

- [ ] **Step 2: Add catalog builder**

Implement `GolempediaCatalog.entries()` by iterating:

```java
GolemVariantCatalog.entries().stream()
    .filter(spec -> spec.variant() != GolemVariant.IRON)
```

For each `GolemVariantSpec`, derive:
- `creationSummary`: body block or creation summary from variant/catalog knowledge.
- `healingItem`: `spec.healItem().getDescription().getString()`.
- `dropSummary`: `spec.lootEnabled()` plus `spec.dropItem()` and `lootMin`/`lootMax`.
- `spawnEggSummary`: enabled/disabled from `spec.spawnEggEnabled()`.
- `villageSpawnSummary`: default weight from `MultiGolemConfig.defaults().villageSpawnWeights().weight(variant)` for roll-order variants; Zombie uses `MultiGolemConfig.defaults().zombieVillageSpawning()`.
- `coreAbility`: concise per-variant ability text.
- `caveats`: concise caveats, especially server defaults may differ, Netherite fire risk, Zombie hostile/faction behavior, Diamond targeting cooldown, Emerald aura, Copper lightning behavior.

Use private switches for ability/caveat copy:

```java
private static String abilityFor(GolemVariant variant) {
    return switch (variant) {
        case COPPER -> "Copper golems are lightning-resistant and can recover from lightning interactions.";
        case GOLD -> "Gold golems move faster and can show sprint and sunlight shine behavior.";
        case EMERALD -> "Emerald golems provide a nearby healing aura.";
        case DIAMOND -> "Diamond golems can use a hostile-target aura with a cooldown.";
        case NETHERITE -> "Netherite golems are fireproof and can ignite nearby attackers.";
        case ZOMBIE -> "Zombie golems fight as an undead-flavored variant with zombie conversion effects.";
        case IRON -> throw new IllegalArgumentException("Iron is not a Golempedia entry");
    };
}

private static List<String> caveatsFor(GolemVariant variant) {
    return switch (variant) {
        case COPPER -> List.of("Server settings may change lightning healing amounts or target rules.");
        case GOLD -> List.of("Server settings may change speed, sprint particles, or sunlight shine behavior.");
        case EMERALD -> List.of("Server settings may change aura range, interval, healing amount, or which villagers count.");
        case DIAMOND -> List.of("Server settings may change target mode, aura range, cooldowns, or lightning protection.");
        case NETHERITE -> List.of("Netherite golems can be dangerous near villages when fire ignition is enabled.");
        case ZOMBIE -> List.of("Zombie village spawning and conversion effects are server-configurable.");
        case IRON -> throw new IllegalArgumentException("Iron is not a Golempedia entry");
    };
}
```

Do not read live server state, `ServerCustomizationsClientState`, or `MultiGolemStatus`.

- [ ] **Step 3: Run Golempedia catalog tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.golempedia.GolempediaCatalogTest
```

Expected: PASS.

### Task 13: Render Golempedia Static UI

**Files:**
- Create: `src/test/java/dev/charles/multigolem/golempedia/GolempediaSourceTest.java`
- Modify: `src/client/java/dev/charles/multigolem/client/modmenu/GolempediaScreen.java`

- [ ] **Step 1: Write source guard for static UI**

Create:

```java
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

        assertTrue(screen.contains("GolempediaCatalog.entries()"));
        assertTrue(screen.contains("selectedIndex"));
        assertTrue(screen.contains("Creation"));
        assertTrue(screen.contains("Healing"));
        assertTrue(screen.contains("Drops"));
        assertTrue(screen.contains("Spawn Egg"));
        assertTrue(screen.contains("Village Spawns"));
        assertTrue(screen.contains("Ability"));
        assertTrue(screen.contains("Caveats"));
        assertFalse(screen.contains("ServerCustomizations"));
        assertFalse(screen.contains("MultiGolemStatus"));
    }
}
```

- [ ] **Step 2: Run failing source guard**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.golempedia.GolempediaSourceTest
```

Expected: FAIL until the screen uses the static catalog.

- [ ] **Step 3: Implement the list/detail screen**

Update `GolempediaScreen` to:
- Store `List<GolempediaEntry> entries = GolempediaCatalog.entries();`
- Store `int selectedIndex = 0;`
- Render a left column of variant buttons.
- Render a right detail pane with section labels: Creation, Healing, Drops, Spawn Egg, Village Spawns, Ability, Caveats.
- Keep the Done button returning to the hub.

Use native text drawing only. Do not add icons, generated art, external links, or server-status references in this slice.

- [ ] **Step 4: Run Golempedia tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.golempedia.*
```

Expected: PASS.

### Task 14: Full Boundary Verification

**Files:**
- Modify only if tests expose integration errors.

- [ ] **Step 1: Run all focused tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.status.MultiGolemModMenuSourceTest --tests dev.charles.multigolem.status.MultiGolemStatusIntegrationSourceTest --tests dev.charles.multigolem.status.MultiGolemStatusPayloadTest --tests dev.charles.multigolem.customizations.* --tests dev.charles.multigolem.config.ServerCustomizationsSummarizerTest --tests dev.charles.multigolem.golempedia.*
```

Expected: PASS.

- [ ] **Step 2: Run the full build**

Run:

```powershell
.\gradlew.bat --quiet build
```

Expected: PASS.

- [ ] **Step 3: Inspect the jar for optional Mod Menu and class ownership**

Run:

```powershell
jar tf build\libs\multigolem-0.4.0+mc26.1.2.jar
```

Expected:
- Contains `dev/charles/multigolem/customizations/ServerCustomizationsPayload.class`.
- Contains `dev/charles/multigolem/golempedia/GolempediaCatalog.class`.
- Contains `dev/charles/multigolem/status/MultiGolemStatusPayload.class`.
- Does not contain `com/terraformersmc/modmenu`.
- Does not contain `cloud/explosive/modstatuskit`.

- [ ] **Step 4: Final scope check**

Run:

```powershell
git diff --name-only
```

Expected changed paths are only implementation/test files named in this plan. No release scripts, changelogs, upload scripts, tag metadata, visual-disabling options, troubleshooting pages, or diagnostic copy behavior.

## Self-Review Notes

- Spec coverage: The tasks cover hub buttons/status square, MSK-only version/status indicator, unavailable customizations states, separate customizations payload/channel, static Golempedia, optional Mod Menu, vanilla-client compatibility, and scope exclusions.
- Placeholder scan: No `TBD`, `TODO`, "implement later", or unspecific "write tests" placeholders are intentionally left. Where exact Minecraft API details may vary, the plan states the required schema/behavior and constrains any adjustment to API method names.
- Type consistency: The plan consistently names `ServerCustomizationsSnapshot`, `ServerCustomizationsSummary`, `VariantCustomizationSummary`, `ServerCustomizationsClientState`, `GolempediaEntry`, and `GolempediaCatalog`.
- Scope creep check: The plan does not add visual options, troubleshooting pages, diagnostic copy, gameplay gates, release work, or extra link surfaces beyond Website and Issues.
