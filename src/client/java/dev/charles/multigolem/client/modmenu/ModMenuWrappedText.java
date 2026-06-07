package dev.charles.multigolem.client.modmenu;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

final class ModMenuWrappedText {
    private static final int LINE_HEIGHT = 10;

    private ModMenuWrappedText() {
    }

    static int render(GuiGraphicsExtractor guiGraphics, Font font, String text, int left, int top, int maxWidth, int color) {
        return renderBounded(guiGraphics, font, text, left, top, maxWidth, Integer.MAX_VALUE, color);
    }

    static int renderBounded(GuiGraphicsExtractor guiGraphics, Font font, String text, int left, int top, int maxWidth, int bottom, int color) {
        int y = top;
        for (String line : wrap(font, text, maxWidth)) {
            if (y + LINE_HEIGHT > bottom) {
                return y;
            }
            guiGraphics.text(font, Component.literal(line), left, y, color);
            y += LINE_HEIGHT;
        }
        return y;
    }

    static List<String> wrap(Font font, String text, int maxWidth) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\R")) {
            wrapParagraph(font, paragraph.trim(), maxWidth, lines);
        }
        return lines;
    }

    private static void wrapParagraph(Font font, String paragraph, int maxWidth, List<String> lines) {
        if (paragraph.isEmpty()) {
            return;
        }

        StringBuilder current = new StringBuilder();
        for (String word : paragraph.split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && font.width(candidate) > maxWidth) {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            } else {
                current.setLength(0);
                current.append(candidate);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
    }
}
