package com.boaat.jazzy_cookin.screen;

import net.minecraft.client.gui.GuiGraphics;

final class JazzyGuiRenderer {
    static final int TEXT = 0x404040;
    static final int TEXT_MUTED = 0x5A5A5A;
    static final int READY_TEXT = 0x2E7D32;
    static final int BLOCKED_TEXT = 0xB23A2F;

    private static final int FRAME_DARK = 0xFF2F2F2F;
    private static final int FRAME_MID = 0xFF555555;
    private static final int PANEL_LIGHT = 0xFFC6C6C6;
    private static final int PANEL_HEADER = 0xFFB6B6B6;
    private static final int PANEL_SHADE = 0xFF8B8B8B;
    private static final int SLOT_INNER = 0xFF373737;
    private static final int SLOT_DISABLED = 0x88606060;
    private static final int PROGRESS_BG = 0xFF373737;

    private JazzyGuiRenderer() {
    }

    static void drawWindow(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, FRAME_DARK);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, FRAME_MID);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL_LIGHT);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + 18, PANEL_HEADER);
        guiGraphics.fill(x + 2, y + 18, x + width - 2, y + 19, FRAME_MID);
    }

    static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, FRAME_MID);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, PANEL_SHADE);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL_LIGHT);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0x22000000);
    }

    static void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, FRAME_DARK);
        guiGraphics.fill(x - 1, y - 1, x + 16, y, 0xFFFFFFFF);
        guiGraphics.fill(x - 1, y - 1, x, y + 16, 0xFFFFFFFF);
        guiGraphics.fill(x, y, x + 16, y + 16, PANEL_SHADE);
        guiGraphics.fill(x + 1, y + 1, x + 15, y + 15, SLOT_INNER);
        guiGraphics.fill(x + 16, y - 1, x + 17, y + 17, FRAME_MID);
        guiGraphics.fill(x - 1, y + 16, x + 17, y + 17, FRAME_MID);
    }

    static void drawDisabledSlot(GuiGraphics guiGraphics, int x, int y) {
        drawSlot(guiGraphics, x, y);
        guiGraphics.fill(x + 1, y + 1, x + 15, y + 15, SLOT_DISABLED);
    }

    static void drawProgressBar(GuiGraphics guiGraphics, int x, int y, int width, float progress, int color) {
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y + 7, FRAME_DARK);
        guiGraphics.fill(x, y, x + width, y + 6, PROGRESS_BG);
        int filledWidth = Math.max(0, Math.min(width, Math.round(width * progress)));
        if (filledWidth > 0) {
            guiGraphics.fill(x, y, x + filledWidth, y + 6, color);
        }
    }
}
