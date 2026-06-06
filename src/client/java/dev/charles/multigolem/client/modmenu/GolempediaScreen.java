package dev.charles.multigolem.client.modmenu;

import dev.charles.multigolem.golempedia.GolempediaCatalog;
import dev.charles.multigolem.golempedia.GolempediaEntry;
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
            addRenderableWidget(Button.builder(Component.literal(entries.get(i).displayName()), button -> selectedIndex = index)
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
        Minecraft.getInstance().setScreen(parent);
    }

    private void renderEntry(GuiGraphicsExtractor guiGraphics, GolempediaEntry entry) {
        int left = Math.max(148, width / 3 + 16);
        int y = 32;
        guiGraphics.text(font, Component.literal(entry.displayName()), left, y, 0xFFFFFFFF);
        y += 16;
        y = renderSection(guiGraphics, "Creation", entry.creationSummary(), left, y);
        y = renderSection(guiGraphics, "Healing", entry.healingItem(), left, y);
        y = renderSection(guiGraphics, "Drops", entry.dropSummary(), left, y);
        y = renderSection(guiGraphics, "Spawn Egg", entry.spawnEggSummary(), left, y);
        y = renderSection(guiGraphics, "Village Spawns", entry.villageSpawnSummary(), left, y);
        y = renderSection(guiGraphics, "Ability", entry.coreAbility(), left, y);
        renderSection(guiGraphics, "Caveats", String.join(" ", entry.caveats()), left, y);
    }

    private int renderSection(GuiGraphicsExtractor guiGraphics, String heading, String body, int left, int y) {
        guiGraphics.text(font, Component.literal(heading), left, y, 0xFFFFFFFF);
        guiGraphics.text(font, Component.literal(body), left + 8, y + 10, 0xFFAAAAAA);
        return y + 26;
    }

    private int maxVisibleEntries() {
        return Math.min(entries.size(), Math.max(0, (height - 68) / 22));
    }
}
