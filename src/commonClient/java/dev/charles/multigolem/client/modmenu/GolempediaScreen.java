package dev.charles.multigolem.client.modmenu;

import dev.charles.multigolem.client.customizations.ServerCustomizationsClient;
import dev.charles.multigolem.client.customizations.ServerCustomizationsClientState;
import dev.charles.multigolem.golempedia.GolempediaCatalog;
import dev.charles.multigolem.golempedia.GolempediaEntry;
import dev.charles.multigolem.golempedia.GolempediaVillageSpawns;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class GolempediaScreen extends Screen {
    private final List<GolempediaEntry> entries = GolempediaCatalog.entries();
    private final Screen parent;
    private int selectedIndex = 0;
    private int scrollOffset = 0;

    GolempediaScreen(Screen parent) {
        super(Component.literal("Golempedia"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(220, width - 32);
        int listWidth = Math.min(120, Math.max(80, width / 3));
        int top = 32;
        int visibleEntries = maxVisibleEntries();
        for (int i = 0; i < visibleEntries; i++) {
            int index = i;
            addRenderableWidget(Button.builder(Component.literal(entries.get(i).displayName()), button -> {
                selectedIndex = index;
                scrollOffset = 0;
            })
                .bounds(12, top + i * 22, listWidth, 20)
                .build());
        }
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
            .bounds((width - buttonWidth) / 2, height - 28, buttonWidth, 20)
            .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, tickDelta);
        if (entries.isEmpty()) {
            Component empty = Component.literal("Golempedia");
            guiGraphics.text(font, empty, (width - font.width(empty)) / 2, height / 2 - 5, 0xFFFFFFFF);
            return;
        }
        renderEntry(guiGraphics, entries.get(Math.max(0, Math.min(selectedIndex, entries.size() - 1))));
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (entries.isEmpty()) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        scrollOffset = Math.max(0, scrollOffset - (int) Math.signum(verticalAmount) * 18);
        return true;
    }

    private void renderEntry(GuiGraphicsExtractor guiGraphics, GolempediaEntry entry) {
        int left = Math.max(148, width / 3 + 16);
        int maxWidth = detailWidth(left);
        int y = 32 - scrollOffset;
        guiGraphics.text(font, Component.literal(entry.displayName()), left, y, 0xFFFFFFFF);
        y += 16;
        y = renderSection(guiGraphics, "Creation", entry.creationSummary(), left, y, maxWidth);
        y = renderSection(guiGraphics, "Healing", entry.healingItem(), left, y, maxWidth);
        y = renderSection(guiGraphics, "Drops", entry.dropSummary(), left, y, maxWidth);
        y = renderSection(guiGraphics, "Stats", String.join("\n", statsFor(entry)), left, y, maxWidth);
        y = renderSection(guiGraphics, "Spawn Egg", entry.spawnEggSummary(), left, y, maxWidth);
        y = renderSection(guiGraphics, "Village Spawns", villageSpawnFor(entry), left, y, maxWidth);
        y = renderSection(guiGraphics, "Ability", entry.coreAbility(), left, y, maxWidth);
        renderSection(guiGraphics, "Caveats", String.join(" ", entry.caveats()), left, y, maxWidth);
    }

    private int renderSection(GuiGraphicsExtractor guiGraphics, String heading, String body, int left, int y, int maxWidth) {
        if (y >= height - 42) {
            return y;
        }
        guiGraphics.text(font, Component.literal(heading), left, y, 0xFFFFFFFF);
        int nextY = ModMenuWrappedText.renderBounded(guiGraphics, font, body, left + 8, y + 10, maxWidth - 8, contentBottom(), 0xFFAAAAAA);
        return nextY + 6;
    }

    private int maxVisibleEntries() {
        return Math.min(entries.size(), Math.max(0, (height - 68) / 22));
    }

    private int detailWidth(int left) {
        return Math.max(80, width - left - 24);
    }

    private int contentBottom() {
        return height - 34;
    }

    private List<String> statsFor(GolempediaEntry entry) {
        ServerCustomizationsClientState state = ServerCustomizationsClient.state();
        if (state.viewState() == ServerCustomizationsClientState.ViewState.AVAILABLE) {
            return state.snapshot()
                .map(snapshot -> snapshot.golempediaStats().get(entry.variant()))
                .filter(lines -> lines != null && !lines.isEmpty())
                .orElse(entry.statLines());
        }
        return entry.statLines();
    }

    private String villageSpawnFor(GolempediaEntry entry) {
        ServerCustomizationsClientState state = ServerCustomizationsClient.state();
        if (state.viewState() == ServerCustomizationsClientState.ViewState.AVAILABLE) {
            return state.snapshot()
                .map(snapshot -> GolempediaVillageSpawns.summary(
                    entry.variant(),
                    snapshot.villageSpawnsEnabled(),
                    snapshot.villageSpawnWeights(),
                    snapshot.zombieVillageSpawningEnabled()
                ))
                .orElse(entry.villageSpawnSummary());
        }
        return entry.villageSpawnSummary();
    }
}
