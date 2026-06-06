package dev.charles.multigolem.client.modmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

public final class MultiGolemStatusScreen extends Screen {
    private final Screen parent;

    public MultiGolemStatusScreen(Screen parent) {
        super(Component.literal("MultiGolem"));
        this.parent = parent;
    }

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

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, tickDelta);
        MultiGolemStatusWidget.renderTopRight(guiGraphics, font, width, 0, mouseX, mouseY);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
