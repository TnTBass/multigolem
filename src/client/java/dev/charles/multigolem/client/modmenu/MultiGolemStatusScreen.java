package dev.charles.multigolem.client.modmenu;

import dev.charles.multigolem.internal.modstatus.ModStatusDisplay;
import dev.charles.multigolem.internal.modstatus.StatusTone;
import dev.charles.multigolem.status.MultiGolemStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class MultiGolemStatusScreen extends Screen {
    private static final int DOT_SIZE = 8;
    private static final int GAP = 6;
    private final Screen parent;

    public MultiGolemStatusScreen(Screen parent) {
        super(Component.literal("MultiGolem"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(200, width - 32);
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
            .bounds((width - buttonWidth) / 2, height - 28, buttonWidth, 20)
            .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, tickDelta);

        ModStatusDisplay display = MultiGolemStatus.display();
        MutableComponent label = statusLabel(display);
        MutableComponent dot = Component.literal(indicatorDot())
            .withStyle(formattingFor(display.tone()));
        int dotWidth = font.width(dot);
        int labelWidth = font.width(label);
        int totalWidth = dotWidth + GAP + labelWidth;
        int left = (width - totalWidth) / 2;
        int top = Math.max(32, height / 2 - 5);
        int textLeft = left + dotWidth + GAP;

        guiGraphics.text(font, dot, left, top, 0xFFFFFFFF);
        guiGraphics.text(font, label, textLeft, top, 0xFFFFFFFF);

        if (isHoveringStatus(left, top, totalWidth, mouseX, mouseY)) {
            guiGraphics.centeredText(font, Component.literal(display.helpText()), width / 2, top - 24, 0xFFFFFFFF);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private static MutableComponent statusLabel(ModStatusDisplay display) {
        return Component.literal(display.statusLabel())
            .withStyle(formattingFor(display.tone()));
    }

    private static String indicatorDot() {
        return "\u25CF";
    }

    private static ChatFormatting formattingFor(StatusTone tone) {
        return switch (tone) {
            case GREEN -> ChatFormatting.GREEN;
            case ORANGE -> ChatFormatting.GOLD;
            case GRAY -> ChatFormatting.GRAY;
        };
    }

    private static boolean isHoveringStatus(int left, int top, int width, int mouseX, int mouseY) {
        return mouseX >= left
            && mouseX <= left + width
            && mouseY >= top - 2
            && mouseY <= top + DOT_SIZE + 2;
    }
}
