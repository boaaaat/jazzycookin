package com.boaat.jazzy_cookin.screen;

import net.minecraft.client.gui.GuiGraphics;

final class JazzyGuiRenderer {
    static final int TITLE_TEXT = 0xF9F8F3;
    static final int TEXT = 0x2E3746;
    static final int TEXT_MUTED = 0x667284;
    static final int TEXT_SOFT = 0x8E97A5;
    static final int READY_TEXT = 0x2F7D61;
    static final int BLOCKED_TEXT = 0xB24E5D;
    static final int ACCENT = 0x4C95A0;
    static final int ACCENT_WARM = 0xD89A4B;

    private static final int WINDOW_BORDER = 0xFF2B3442;
    private static final int WINDOW_EDGE = 0xFF4D596B;
    private static final int WINDOW_SURFACE = 0xFFF3EFE7;
    private static final int HEADER_BG = 0xFF344558;
    private static final int HEADER_ACCENT = 0xFF5AAAB1;
    private static final int CARD_BORDER = 0xFFD6CCBD;
    private static final int CARD_SHADE = 0xFFE5DED2;
    private static final int CARD_SURFACE = 0xFFFBFAF6;
    private static final int SLOT_EDGE = 0xFF727A87;
    private static final int SLOT_HIGHLIGHT = 0xFFF8F6EF;
    private static final int SLOT_BASE = 0xFFBBB4A9;
    private static final int SLOT_INNER = 0xFF333B48;
    private static final int SLOT_DISABLED = 0x887A7470;
    private static final int PROGRESS_BG = 0xFFE1DDD6;
    private static final int CHIP_BORDER = 0xFFC7D0DA;
    private static final int CHIP_FILL = 0xFFF6FBFC;
    private static final int CHIP_FILL_WARM = 0xFFFAF2E7;

    private JazzyGuiRenderer() {
    }

    static void drawWindow(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, WINDOW_BORDER);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, WINDOW_EDGE);
        guiGraphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, WINDOW_SURFACE);
        guiGraphics.fill(x + 4, y + 4, x + width - 4, y + 26, HEADER_BG);
        guiGraphics.fill(x + 4, y + 26, x + width - 4, y + 28, HEADER_ACCENT);
    }

    static void drawCard(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, CARD_BORDER);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, CARD_SHADE);
        guiGraphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, CARD_SURFACE);
    }

    static void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
        guiGraphics.fill(x - 1, y - 1, x + 16, y, SLOT_HIGHLIGHT);
        guiGraphics.fill(x - 1, y - 1, x, y + 16, SLOT_HIGHLIGHT);
        guiGraphics.fill(x, y, x + 16, y + 16, SLOT_BASE);
        guiGraphics.fill(x + 1, y + 1, x + 15, y + 15, SLOT_INNER);
        guiGraphics.fill(x + 16, y - 1, x + 17, y + 17, SLOT_EDGE);
        guiGraphics.fill(x - 1, y + 16, x + 17, y + 17, SLOT_EDGE);
    }

    static void drawDisabledSlot(GuiGraphics guiGraphics, int x, int y) {
        drawSlot(guiGraphics, x, y);
        guiGraphics.fill(x + 1, y + 1, x + 15, y + 15, SLOT_DISABLED);
    }

    static void drawProgressBar(GuiGraphics guiGraphics, int x, int y, int width, float progress, int color) {
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y + 7, CARD_BORDER);
        guiGraphics.fill(x, y, x + width, y + 6, PROGRESS_BG);
        int filledWidth = Math.max(0, Math.min(width, Math.round(width * progress)));
        if (filledWidth > 0) {
            guiGraphics.fill(x, y, x + filledWidth, y + 6, color);
        }
    }

    static void drawChip(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean warm) {
        guiGraphics.fill(x, y, x + width, y + height, CHIP_BORDER);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, warm ? CHIP_FILL_WARM : CHIP_FILL);
    }
}
