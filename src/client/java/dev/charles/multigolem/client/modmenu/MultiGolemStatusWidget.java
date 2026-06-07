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
    private static final int MIN_TOOLTIP_ANCHOR_Y = 72;

    private MultiGolemStatusWidget() {
    }

    static void renderTopRight(GuiGraphicsExtractor guiGraphics, Font font, int right, int top, int mouseX, int mouseY) {
        ModStatusDisplay display = MultiGolemStatus.display();
        int left = right - STATUS_SQUARE_SIZE - RIGHT_MARGIN;
        int squareTop = top + TOP_MARGIN;

        renderStatusSquare(guiGraphics, display.tone(), left, squareTop);
        if (isHoveringStatus(left, squareTop, mouseX, mouseY)) {
            guiGraphics.setComponentTooltipForNextFrame(font, tooltipLines(display), mouseX, tooltipAnchorY(squareTop, mouseY));
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
        List<String> text = tooltipText(display);
        List<Component> lines = new ArrayList<>();
        for (int i = 0; i < text.size(); i++) {
            ChatFormatting style = switch (i) {
                case 1 -> formattingFor(display.tone());
                case 0, 2, 3 -> ChatFormatting.WHITE;
                default -> ChatFormatting.GRAY;
            };
            lines.add(Component.literal(text.get(i)).withStyle(style));
        }
        return lines;
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

    private static int tooltipAnchorY(int squareTop, int mouseY) {
        return Math.max(mouseY, MIN_TOOLTIP_ANCHOR_Y);
    }
}
