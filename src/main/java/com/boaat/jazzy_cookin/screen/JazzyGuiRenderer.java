package com.boaat.jazzy_cookin.screen;

import com.boaat.jazzy_cookin.kitchen.StationUiProfile;

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
    private static final int TAB_HOVER = 0xFF9BA7B7;
    private static final int TAB_SELECTED = 0xFF5AAAB1;

    private JazzyGuiRenderer() {
    }

    static void drawWindow(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        drawWindow(guiGraphics, x, y, width, height, StationUiProfile.Theme.BOARD);
    }

    static void drawWindow(GuiGraphics guiGraphics, int x, int y, int width, int height, StationUiProfile.Theme theme) {
        Palette palette = paletteFor(theme);
        guiGraphics.fill(x, y, x + width, y + height, WINDOW_BORDER);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, WINDOW_EDGE);
        guiGraphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, palette.windowSurface());
        guiGraphics.fill(x + 4, y + 4, x + width - 4, y + 26, palette.headerBg());
        guiGraphics.fill(x + 4, y + 26, x + width - 4, y + 28, palette.headerAccent());
    }

    static void drawCard(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        drawCard(guiGraphics, x, y, width, height, StationUiProfile.Theme.BOARD);
    }

    static void drawCard(GuiGraphics guiGraphics, int x, int y, int width, int height, StationUiProfile.Theme theme) {
        Palette palette = paletteFor(theme);
        guiGraphics.fill(x, y, x + width, y + height, CARD_BORDER);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, palette.cardShade());
        guiGraphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, palette.cardSurface());
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
            guiGraphics.fill(x, y, x + filledWidth, y + 6, opaque(color));
        }
    }

    static void drawChip(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean warm) {
        drawChip(guiGraphics, x, y, width, height, warm, StationUiProfile.Theme.BOARD);
    }

    static void drawChip(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean warm, StationUiProfile.Theme theme) {
        Palette palette = paletteFor(theme);
        guiGraphics.fill(x, y, x + width, y + height, CHIP_BORDER);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, warm ? palette.chipWarm() : palette.chipCool());
    }

    static void drawIconTab(GuiGraphics guiGraphics, int x, int y, boolean hovered, boolean selected) {
        int frame = selected ? TAB_SELECTED : hovered ? TAB_HOVER : CARD_BORDER;
        guiGraphics.fill(x, y, x + 20, y + 20, frame);
        drawSlot(guiGraphics, x + 2, y + 2);
    }

    private static int opaque(int color) {
        return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
    }

    private static Palette paletteFor(StationUiProfile.Theme theme) {
        return switch (theme) {
            case BOWL -> new Palette(0xFF2F4A5F, 0xFF7BC3C9, 0xFFE4DDD4, 0xFFF9F8F4, 0xFFF2FBFD, 0xFFF6EEE3, 0xFFF0ECE4);
            case SERVICE -> new Palette(0xFF3A4356, 0xFFC9A24B, 0xFFE6DDD0, 0xFFFBF8F1, 0xFFFBF8F1, 0xFFF5EBD7, 0xFFF3F0EA);
            case RANGE -> new Palette(0xFF3C424F, 0xFFE28B42, 0xFFE2D8CC, 0xFFF8F6F2, 0xFFF5FBFC, 0xFFFBEEE2, 0xFFF0ECE4);
            case OVEN -> new Palette(0xFF513C31, 0xFFDA8752, 0xFFE7D8CA, 0xFFFBF6F0, 0xFFF9F3E9, 0xFFFBE7D8, 0xFFF3ECE3);
            case MACHINE -> new Palette(0xFF304A60, 0xFF5DB8CC, 0xFFDCE4E9, 0xFFF5F9FB, 0xFFF3FBFD, 0xFFEAF7FA, 0xFFEFF4F7);
            case GLASS -> new Palette(0xFF355264, 0xFF7DB7D0, 0xFFDCE6EA, 0xFFF5FAFC, 0xFFF1FBFD, 0xFFE8F5F9, 0xFFEFF5F7);
            case CROCK -> new Palette(0xFF5C4637, 0xFFAA8359, 0xFFE3D5C4, 0xFFF9F5EE, 0xFFF7F0E5, 0xFFF4E6D8, 0xFFF0E8DE);
            case PRESERVE -> new Palette(0xFF4D3F5C, 0xFFD06C66, 0xFFE3D7DF, 0xFFFBF7FA, 0xFFF9F2F7, 0xFFF8E8E4, 0xFFF1EBF0);
            case STEAM -> new Palette(0xFF35515A, 0xFF63B7AE, 0xFFDDE6E4, 0xFFF7FAF8, 0xFFF1FBF8, 0xFFE7F7F2, 0xFFEEF4F2);
            case COLD -> new Palette(0xFF2F4360, 0xFF7FB6E8, 0xFFD9E3F1, 0xFFF5F8FD, 0xFFF0F6FD, 0xFFE8F1FC, 0xFFEDF2F8);
            case RACK -> new Palette(0xFF4E5147, 0xFF9BB06C, 0xFFE0DDD2, 0xFFF8F7F2, 0xFFF6F9F1, 0xFFF1F2E5, 0xFFF0EEE6);
            case SMOKE -> new Palette(0xFF3F3F46, 0xFFC0895A, 0xFFE1D8CF, 0xFFF8F5F2, 0xFFF8F4EE, 0xFFF5E9DD, 0xFFEFEBE5);
            case MICROWAVE -> new Palette(0xFF2D4960, 0xFF65BDD7, 0xFFDCE6EA, 0xFFF7FAFB, 0xFFF2FAFD, 0xFFEAF6F8, 0xFFEFF4F7);
            case CITRUS -> new Palette(0xFF42604A, 0xFFF0B54B, 0xFFE5E1D0, 0xFFF9F8F2, 0xFFFCF8E7, 0xFFFBEFD9, 0xFFF1EEE4);
            case SPICE -> new Palette(0xFF5A4132, 0xFFD47E4E, 0xFFE5D6CB, 0xFFF9F5F1, 0xFFF9F3EE, 0xFFF7E8DE, 0xFFF2ECE6);
            case BOARD -> new Palette(HEADER_BG, HEADER_ACCENT, CARD_SHADE, CARD_SURFACE, CHIP_FILL, CHIP_FILL_WARM, WINDOW_SURFACE);
        };
    }

    private record Palette(int headerBg, int headerAccent, int cardShade, int cardSurface, int chipCool, int chipWarm, int windowSurface) {
    }
}
