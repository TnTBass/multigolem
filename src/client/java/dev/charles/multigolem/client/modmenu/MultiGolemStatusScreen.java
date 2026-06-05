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

import java.util.ArrayList;
import java.util.List;

public final class MultiGolemStatusScreen extends Screen {
    private static final int STATUS_SIZE = 8;
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
        int statusWidth = STATUS_SIZE + GAP + font.width(statusLabel(display));
        int detailWidth = Math.max(
            font.width("Client: " + versionWithBuild(display.clientVersion(), display.clientBuild())),
            font.width("Server: " + versionWithBuild(display.serverVersion(), display.serverBuild()))
        );
        int updateWidth = display.updateUrl() == null ? 0 : font.width("Updates: Modrinth");
        int totalWidth = Math.max(statusWidth, Math.max(detailWidth, updateWidth));
        int left = (width - totalWidth) / 2;
        int top = Math.max(32, height / 2 - 18);

        renderStatusRow(guiGraphics, display, left, top, statusWidth, mouseX, mouseY);
        renderDetail(guiGraphics, "Client:", versionWithBuild(display.clientVersion(), display.clientBuild()), left, top + 14);
        renderDetail(guiGraphics, "Server:", versionWithBuild(display.serverVersion(), display.serverBuild()), left, top + 26);

        if (display.updateUrl() != null) {
            guiGraphics.text(font, Component.literal("Updates: Modrinth"), left, top + 38, 0xFFAAAAAA);
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

    private void renderStatusRow(
        GuiGraphicsExtractor guiGraphics,
        ModStatusDisplay display,
        int left,
        int top,
        int rowWidth,
        int mouseX,
        int mouseY
    ) {
        MutableComponent label = statusLabel(display);
        int textLeft = left + STATUS_SIZE + GAP;

        guiGraphics.fill(left, top, left + STATUS_SIZE, top + STATUS_SIZE, 0xAA000000);
        guiGraphics.fill(left + 1, top + 1, left + STATUS_SIZE - 1, top + STATUS_SIZE - 1, toneColor(display.tone()));
        guiGraphics.text(font, label, textLeft, top, 0xFFFFFFFF);

        if (isHoveringStatus(left, top, rowWidth, mouseX, mouseY)) {
            guiGraphics.setComponentTooltipForNextFrame(font, tooltipLines(display), mouseX, mouseY);
        }
    }

    private void renderDetail(GuiGraphicsExtractor guiGraphics, String label, String value, int left, int top) {
        guiGraphics.text(font, Component.literal(label + " " + value), left, top, 0xFFDDDDDD);
    }

    private static List<Component> tooltipLines(ModStatusDisplay display) {
        return tooltipText(display).stream()
            .<Component>map(Component::literal)
            .toList();
    }

    static List<String> tooltipText(ModStatusDisplay display) {
        List<String> text = new ArrayList<>();
        text.add(display.displayName());
        text.add("Status: " + display.statusLabel());
        text.add("Client: " + versionWithBuild(display.clientVersion(), display.clientBuild()));
        text.add("Server: " + versionWithBuild(display.serverVersion(), display.serverBuild()));
        if (!display.helpText().isEmpty()) {
            text.add(display.helpText());
        }
        return text;
    }

    private static String versionWithBuild(String version, String build) {
        if (version == null || version.isBlank()) {
            return "Unknown";
        }
        return build == null || build.isBlank() || "dev".equalsIgnoreCase(build) ? version : version + "+" + build;
    }

    private static boolean isHoveringStatus(int left, int top, int width, int mouseX, int mouseY) {
        return mouseX >= left
            && mouseX <= left + width
            && mouseY >= top - 2
            && mouseY <= top + STATUS_SIZE + 2;
    }
}
