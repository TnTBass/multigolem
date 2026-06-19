package dev.charles.multigolem.client.modmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MultiGolemStatusScreen extends Screen {
    private static final int BOTTOM_BUTTON_MARGIN = 32;

    private final Screen parent;

    public MultiGolemStatusScreen(Screen parent) {
        super(Component.literal("MultiGolem"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(220, width - 32);
        int left = (width - buttonWidth) / 2;
        int actionStep = 24;
        int primaryTop = Math.max(44, height / 2 - 28);

        addRenderableWidget(Button.builder(Component.literal("Server Customizations"), button ->
            Minecraft.getInstance().setScreenAndShow(new ServerCustomizationsScreen(this))
        ).bounds(left, primaryTop, buttonWidth, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Golempedia"), button ->
            Minecraft.getInstance().setScreenAndShow(new GolempediaScreen(this))
        ).bounds(left, primaryTop + actionStep, buttonWidth, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
            .bounds(left, height - BOTTOM_BUTTON_MARGIN, buttonWidth, 20)
            .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, tickDelta);
        MultiGolemStatusWidget.renderTopRight(guiGraphics, font, width, 0, mouseX, mouseY);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(parent);
    }
}
