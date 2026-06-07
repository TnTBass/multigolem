package dev.charles.multigolem.client.modmenu;

import dev.charles.multigolem.client.customizations.ServerCustomizationsClient;
import dev.charles.multigolem.client.customizations.ServerCustomizationsClientState;
import dev.charles.multigolem.customizations.ServerCustomizationsSummary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ServerCustomizationsScreen extends Screen {
    private static final Component UNAVAILABLE = Component.literal("No server customizations are available.");

    private final Screen parent;

    ServerCustomizationsScreen(Screen parent) {
        super(Component.literal("Server Customizations"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(220, width - 32);
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
            .bounds((width - buttonWidth) / 2, height - 28, buttonWidth, 20)
            .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, tickDelta);
        ServerCustomizationsClientState state = ServerCustomizationsClient.state();
        if (state.viewState() == ServerCustomizationsClientState.ViewState.UNAVAILABLE) {
            renderCenteredLine(guiGraphics, UNAVAILABLE, height / 2 - 5, 0xFFAAAAAA);
        } else if (state.viewState() == ServerCustomizationsClientState.ViewState.PENDING) {
            renderCenteredLine(guiGraphics, Component.literal("Loading server customizations..."), height / 2 - 5, 0xFFAAAAAA);
        } else if (state.viewState() == ServerCustomizationsClientState.ViewState.AVAILABLE) {
            state.summary().ifPresentOrElse(
                summary -> renderSummary(guiGraphics, summary),
                () -> renderCenteredLine(guiGraphics, UNAVAILABLE, height / 2 - 5, 0xFFAAAAAA)
            );
        }
    }

    @Override
    public void onClose() {
        ServerCustomizationsClient.state().onScreenClose();
        Minecraft.getInstance().setScreen(parent);
    }

    private void renderCenteredLine(GuiGraphicsExtractor guiGraphics, Component text, int y, int color) {
        guiGraphics.text(font, text, (width - font.width(text)) / 2, y, color);
    }

    private void renderSummary(GuiGraphicsExtractor guiGraphics, ServerCustomizationsSummary summary) {
        int left = Math.max(16, width / 2 - 140);
        int maxWidth = Math.max(80, width - left - 24);
        int y = 32;
        y = renderGroup(guiGraphics, "Global", summary.globalLines(), left, y, maxWidth);
        y = renderGroup(guiGraphics, "Village Spawns", summary.villageLines(), left, y, maxWidth);
        y = renderGroup(guiGraphics, "Zombie Village Spawns", summary.zombieVillageLines(), left, y, maxWidth);
        renderGroup(guiGraphics, "Variants", summary.variantLines(), left, y, maxWidth);
    }

    private int renderGroup(GuiGraphicsExtractor guiGraphics, String heading, java.util.List<String> lines, int left, int y, int maxWidth) {
        if (lines.isEmpty()) {
            return y;
        }
        if (y >= height - 42) {
            return y;
        }
        guiGraphics.text(font, Component.literal(heading), left, y, 0xFFFFFFFF);
        y += 12;
        for (String line : lines) {
            if (y >= height - 42) {
                return y;
            }
            y = ModMenuWrappedText.render(guiGraphics, font, line, left + 8, y, maxWidth - 8, 0xFFAAAAAA);
        }
        return y + 6;
    }
}
