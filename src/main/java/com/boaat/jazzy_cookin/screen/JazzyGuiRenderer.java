package com.boaat.jazzy_cookin.screen;

import com.boaat.jazzy_cookin.kitchen.StationUiProfile;
import com.boaat.jazzy_cookin.kitchen.StationUiProfile.LayoutFamily;
import com.boaat.jazzy_cookin.kitchen.StationUiProfile.MetricWidgetSpec;
import com.boaat.jazzy_cookin.kitchen.StorageType;
import com.boaat.jazzy_cookin.screen.layout.ActionWidgetSpec;
import com.boaat.jazzy_cookin.screen.layout.LayoutRegion;

import net.minecraft.client.gui.GuiGraphics;

final class JazzyGuiRenderer {
    static final int TITLE_TEXT = 0xF9F8F3;
    static final int TEXT = 0x263241;
    static final int TEXT_MUTED = 0x667284;
    static final int TEXT_SOFT = 0x8C96A4;
    static final int READY_TEXT = 0x2F7D61;
    static final int BLOCKED_TEXT = 0xB24E5D;
    static final int ACCENT = 0x4C95A0;
    static final int ACCENT_WARM = 0xD89A4B;

    private static final int FRAME_OUTER = 0xFF222B38;
    private static final int FRAME_INNER = 0xFF4A5667;
    private static final int SURFACE = 0xFFF1EDE4;
    private static final int HEADER_TEXT = 0xFFF9F8F3;
    private static final int SLOT_EDGE = 0xFF6D7582;
    private static final int SLOT_EDGE_SOFT = 0xFFECE6DB;
    private static final int SLOT_BASE = 0xFFBBB2A6;
    private static final int SLOT_INNER = 0xFF333B48;
    private static final int SLOT_DISABLED = 0x887A7470;
    private static final int BUTTON_TEXT = 0xFFF8F7F2;

    private JazzyGuiRenderer() {
    }

    static void drawStationShell(GuiGraphics guiGraphics, int x, int y, int width, int height, StationUiProfile.Theme theme) {
        Palette palette = paletteFor(theme);
        guiGraphics.fill(x, y, x + width, y + height, FRAME_OUTER);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, FRAME_INNER);
        guiGraphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, palette.windowSurface());
        guiGraphics.fill(x + 6, y + 6, x + width - 6, y + 28, palette.headerBg());
        guiGraphics.fill(x + 6, y + 28, x + width - 6, y + 31, palette.headerAccent());
        guiGraphics.fill(x + 6, y + height - 92, x + width - 6, y + height - 90, palette.headerAccent());
    }

    static void drawStorageShell(GuiGraphics guiGraphics, int x, int y, int width, int height, StorageType storageType) {
        StationUiProfile.Theme theme = switch (storageType) {
            case PANTRY -> StationUiProfile.Theme.BOARD;
            case FRIDGE -> StationUiProfile.Theme.COLD;
            case FREEZER -> StationUiProfile.Theme.GLASS;
        };
        drawStationShell(guiGraphics, x, y, width, height, theme);
    }

    static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, StationUiProfile.Theme theme, PanelStyle style) {
        Palette palette = paletteFor(theme);
        int border = style == PanelStyle.RAISED ? palette.panelBorder() : palette.shadow();
        int frame = switch (style) {
            case WORKSPACE -> palette.workSurface();
            case PREVIEW -> palette.previewSurface();
            case METRIC -> palette.metricSurface();
            case CONTROL -> palette.controlSurface();
            case INVENTORY -> palette.shelfSurface();
            case STORAGE -> palette.workSurface();
            case RAISED -> palette.panelSurface();
        };
        guiGraphics.fill(x, y, x + width, y + height, border);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, palette.shadow());
        guiGraphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, frame);
        guiGraphics.fill(x + 3, y + 3, x + width - 3, y + 10, pale(frame, 0.08F));
    }

    static void drawWorkspaceBackdrop(
            GuiGraphics guiGraphics,
            int left,
            int top,
            LayoutRegion workspace,
            LayoutRegion toolRegion,
            StationUiProfile.Theme theme,
            LayoutFamily family
    ) {
        Palette palette = paletteFor(theme);
        drawPanel(guiGraphics, left + workspace.x(), top + workspace.y(), workspace.width(), workspace.height(), theme, PanelStyle.WORKSPACE);
        switch (family) {
            case MIX -> drawBowlBackdrop(guiGraphics, left + workspace.x(), top + workspace.y(), workspace.width(), workspace.height(), palette);
            case PREP -> drawBoardBackdrop(guiGraphics, left + workspace.x(), top + workspace.y(), workspace.width(), workspace.height(), palette);
            case RANGE -> drawRangeBackdrop(guiGraphics, left + workspace.x(), top + workspace.y(), workspace.width(), workspace.height(), palette);
            case CHAMBER -> drawChamberBackdrop(guiGraphics, left + workspace.x(), top + workspace.y(), workspace.width(), workspace.height(), palette);
            case MACHINE -> drawMachineBackdrop(guiGraphics, left + workspace.x(), top + workspace.y(), workspace.width(), workspace.height(), palette);
            case PRESERVE -> drawPreserveBackdrop(guiGraphics, left + workspace.x(), top + workspace.y(), workspace.width(), workspace.height(), palette);
            case REST -> drawRestBackdrop(guiGraphics, left + workspace.x(), top + workspace.y(), workspace.width(), workspace.height(), palette);
            case PLATE -> drawPlateBackdrop(guiGraphics, left + workspace.x(), top + workspace.y(), workspace.width(), workspace.height(), palette);
        }
        if (toolRegion != null) {
            drawPanel(guiGraphics, left + toolRegion.x(), top + toolRegion.y(), toolRegion.width(), toolRegion.height(), theme, PanelStyle.RAISED);
        }
    }

    static void drawPreviewBackdrop(
            GuiGraphics guiGraphics,
            int left,
            int top,
            LayoutRegion previewRegion,
            LayoutRegion outputRegion,
            LayoutRegion byproductRegion,
            StationUiProfile.Theme theme
    ) {
        drawPanel(guiGraphics, left + previewRegion.x(), top + previewRegion.y(), previewRegion.width(), previewRegion.height(), theme, PanelStyle.PREVIEW);
        drawPanel(guiGraphics, left + outputRegion.x(), top + outputRegion.y(), outputRegion.width(), outputRegion.height(), theme, PanelStyle.RAISED);
        drawPanel(guiGraphics, left + byproductRegion.x(), top + byproductRegion.y(), byproductRegion.width(), byproductRegion.height(), theme, PanelStyle.RAISED);
    }

    static void drawMetricCluster(GuiGraphics guiGraphics, int left, int top, LayoutRegion region, StationUiProfile.Theme theme) {
        drawPanel(guiGraphics, left + region.x(), top + region.y(), region.width(), region.height(), theme, PanelStyle.METRIC);
    }

    static void drawMetricWidget(GuiGraphics guiGraphics, int left, int top, MetricWidgetSpec spec, StationUiProfile.Theme theme, float ratio, int color) {
        Palette palette = paletteFor(theme);
        LayoutRegion bounds = spec.bounds();
        guiGraphics.fill(left + bounds.x(), top + bounds.y(), left + bounds.right(), top + bounds.bottom(), palette.metricInset());
        guiGraphics.fill(left + bounds.x() + 1, top + bounds.y() + 1, left + bounds.right() - 1, top + bounds.bottom() - 1, palette.metricSurface());
        LayoutRegion meter = spec.meterBounds();
        guiGraphics.fill(left + meter.x(), top + meter.y(), left + meter.right(), top + meter.bottom(), palette.meterTrack());
        int fill = Math.max(0, Math.min(meter.width(), Math.round(meter.width() * ratio)));
        if (fill > 0) {
            guiGraphics.fill(left + meter.x(), top + meter.y(), left + meter.x() + fill, top + meter.bottom(), opaque(color));
        }
    }

    static void drawControlStrip(GuiGraphics guiGraphics, int left, int top, LayoutRegion region, StationUiProfile.Theme theme) {
        drawPanel(guiGraphics, left + region.x(), top + region.y(), region.width(), region.height(), theme, PanelStyle.CONTROL);
    }

    static void drawInventoryShelf(GuiGraphics guiGraphics, int left, int top, LayoutRegion region, StationUiProfile.Theme theme) {
        Palette palette = paletteFor(theme);
        drawPanel(guiGraphics, left + region.x(), top + region.y(), region.width(), region.height(), theme, PanelStyle.INVENTORY);
        guiGraphics.fill(left + region.x() + 12, top + region.y() + 16, left + region.right() - 12, top + region.y() + 18, palette.shadow());
    }

    static void drawStorageSupport(GuiGraphics guiGraphics, int left, int top, LayoutRegion region, StorageType storageType) {
        drawPanel(guiGraphics, left + region.x(), top + region.y(), region.width(), region.height(), storageTheme(storageType), PanelStyle.CONTROL);
    }

    static void drawActionPlate(
            GuiGraphics guiGraphics,
            int left,
            int top,
            ActionWidgetSpec spec,
            StationUiProfile.Theme theme,
            boolean active,
            boolean hovered,
            boolean pressed
    ) {
        Palette palette = paletteFor(theme);
        LayoutRegion bounds = spec.bounds();
        int edge = !active ? palette.shadow() : pressed ? palette.headerAccent() : hovered ? pale(palette.headerAccent(), 0.12F) : palette.panelBorder();
        int fill = !active ? darken(palette.controlSurface(), 0.10F) : pressed ? darken(palette.headerBg(), 0.08F) : hovered ? pale(palette.headerBg(), 0.06F) : palette.headerBg();
        guiGraphics.fill(left + bounds.x(), top + bounds.y(), left + bounds.right(), top + bounds.bottom(), edge);
        guiGraphics.fill(left + bounds.x() + 1, top + bounds.y() + 1, left + bounds.right() - 1, top + bounds.bottom() - 1, fill);
        guiGraphics.fill(left + bounds.x() + 2, top + bounds.y() + 2, left + bounds.right() - 2, top + bounds.y() + 4, pale(fill, 0.14F));
    }

    static void drawChip(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean warm, StationUiProfile.Theme theme) {
        Palette palette = paletteFor(theme);
        guiGraphics.fill(x, y, x + width, y + height, palette.panelBorder());
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, warm ? palette.chipWarm() : palette.chipCool());
    }

    static void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
        guiGraphics.fill(x - 1, y - 1, x + 16, y, SLOT_EDGE_SOFT);
        guiGraphics.fill(x - 1, y - 1, x, y + 16, SLOT_EDGE_SOFT);
        guiGraphics.fill(x, y, x + 16, y + 16, SLOT_BASE);
        guiGraphics.fill(x + 1, y + 1, x + 15, y + 15, SLOT_INNER);
        guiGraphics.fill(x + 16, y - 1, x + 17, y + 17, SLOT_EDGE);
        guiGraphics.fill(x - 1, y + 16, x + 17, y + 17, SLOT_EDGE);
    }

    static void drawDisabledSlot(GuiGraphics guiGraphics, int x, int y) {
        drawSlot(guiGraphics, x, y);
        guiGraphics.fill(x + 1, y + 1, x + 15, y + 15, SLOT_DISABLED);
    }

    static void drawStorageTab(GuiGraphics guiGraphics, int x, int y, boolean hovered, boolean selected, StorageType storageType) {
        Palette palette = paletteFor(storageTheme(storageType));
        int frame = selected ? palette.headerAccent() : hovered ? pale(palette.headerAccent(), 0.10F) : palette.panelBorder();
        guiGraphics.fill(x, y, x + 22, y + 22, frame);
        guiGraphics.fill(x + 1, y + 1, x + 21, y + 21, palette.panelSurface());
        guiGraphics.fill(x + 2, y + 2, x + 20, y + 7, pale(palette.panelSurface(), 0.08F));
        drawSlot(guiGraphics, x + 3, y + 3);
    }

    static void drawPageButton(GuiGraphics guiGraphics, int x, int y, int width, int height, StorageType storageType, boolean active, boolean hovered) {
        drawActionPlate(
                guiGraphics,
                0,
                0,
                new ActionWidgetSpec(new LayoutRegion(x, y, width, height), null),
                storageTheme(storageType),
                active,
                hovered,
                false
        );
    }

    private static StationUiProfile.Theme storageTheme(StorageType storageType) {
        return switch (storageType) {
            case PANTRY -> StationUiProfile.Theme.BOARD;
            case FRIDGE -> StationUiProfile.Theme.COLD;
            case FREEZER -> StationUiProfile.Theme.GLASS;
        };
    }

    private static void drawBowlBackdrop(GuiGraphics guiGraphics, int x, int y, int width, int height, Palette palette) {
        drawRing(guiGraphics, x + width / 2 - 44, y + 14, 88, 68, palette.shadow(), palette.panelSurface(), palette.previewSurface());
        guiGraphics.fill(x + 18, y + height - 22, x + width - 18, y + height - 18, palette.shadow());
    }

    private static void drawBoardBackdrop(GuiGraphics guiGraphics, int x, int y, int width, int height, Palette palette) {
        guiGraphics.fill(x + 10, y + 12, x + width - 10, y + height - 12, palette.previewSurface());
        for (int stripe = 0; stripe < 5; stripe++) {
            int stripeX = x + 18 + stripe * 28;
            guiGraphics.fill(stripeX, y + 16, stripeX + 10, y + height - 16, pale(palette.workSurface(), 0.04F));
        }
        guiGraphics.fill(x + width - 32, y + 16, x + width - 26, y + height - 16, palette.headerAccent());
    }

    private static void drawRangeBackdrop(GuiGraphics guiGraphics, int x, int y, int width, int height, Palette palette) {
        guiGraphics.fill(x + 12, y + 16, x + width - 12, y + height - 16, palette.previewSurface());
        drawRing(guiGraphics, x + 80, y + 28, 38, 28, palette.shadow(), palette.metricInset(), SLOT_INNER);
        drawRing(guiGraphics, x + 126, y + 38, 44, 34, palette.shadow(), palette.metricInset(), SLOT_INNER);
    }

    private static void drawChamberBackdrop(GuiGraphics guiGraphics, int x, int y, int width, int height, Palette palette) {
        guiGraphics.fill(x + 14, y + 14, x + width - 14, y + height - 14, SLOT_INNER);
        for (int rail = 0; rail < 4; rail++) {
            int railY = y + 24 + rail * 16;
            guiGraphics.fill(x + 20, railY, x + width - 20, railY + 2, pale(palette.previewSurface(), 0.10F));
        }
        guiGraphics.fill(x + 30, y + height - 28, x + width - 30, y + height - 20, palette.previewSurface());
    }

    private static void drawMachineBackdrop(GuiGraphics guiGraphics, int x, int y, int width, int height, Palette palette) {
        guiGraphics.fill(x + width - 78, y + 12, x + width - 30, y + height - 14, SLOT_INNER);
        guiGraphics.fill(x + width - 68, y + 18, x + width - 40, y + height - 20, palette.previewSurface());
        guiGraphics.fill(x + 18, y + height - 24, x + width - 22, y + height - 18, palette.shadow());
        guiGraphics.fill(x + 36, y + 14, x + 74, y + 24, pale(palette.previewSurface(), 0.08F));
    }

    private static void drawPreserveBackdrop(GuiGraphics guiGraphics, int x, int y, int width, int height, Palette palette) {
        guiGraphics.fill(x + 14, y + height - 22, x + width - 14, y + height - 18, palette.shadow());
        for (int jar = 0; jar < 3; jar++) {
            int jarX = x + 28 + jar * 34;
            guiGraphics.fill(jarX, y + 26, jarX + 18, y + 54, palette.previewSurface());
            guiGraphics.fill(jarX + 2, y + 20, jarX + 16, y + 26, palette.headerAccent());
        }
    }

    private static void drawRestBackdrop(GuiGraphics guiGraphics, int x, int y, int width, int height, Palette palette) {
        for (int slat = 0; slat < 5; slat++) {
            int slatY = y + 18 + slat * 12;
            guiGraphics.fill(x + 16, slatY, x + width - 16, slatY + 6, pale(palette.workSurface(), 0.05F));
        }
    }

    private static void drawPlateBackdrop(GuiGraphics guiGraphics, int x, int y, int width, int height, Palette palette) {
        drawRing(guiGraphics, x + width / 2 - 42, y + 18, 84, 60, palette.shadow(), palette.previewSurface(), SURFACE);
        guiGraphics.fill(x + 24, y + height - 22, x + width - 24, y + height - 18, palette.shadow());
    }

    private static void drawRing(GuiGraphics guiGraphics, int x, int y, int width, int height, int outer, int mid, int inner) {
        guiGraphics.fill(x, y, x + width, y + height, outer);
        guiGraphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, mid);
        guiGraphics.fill(x + 10, y + 10, x + width - 10, y + height - 10, inner);
    }

    private static int opaque(int color) {
        return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
    }

    private static int pale(int color, float amount) {
        return mix(color, 0xFFFFFFFF, amount);
    }

    private static int darken(int color, float amount) {
        return mix(color, 0xFF000000, amount);
    }

    private static int mix(int color, int target, float amount) {
        float clamped = Math.max(0.0F, Math.min(1.0F, amount));
        int a = (color >>> 24) & 0xFF;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        int tr = (target >>> 16) & 0xFF;
        int tg = (target >>> 8) & 0xFF;
        int tb = target & 0xFF;
        int nr = Math.round(r + (tr - r) * clamped);
        int ng = Math.round(g + (tg - g) * clamped);
        int nb = Math.round(b + (tb - b) * clamped);
        return (a << 24) | (nr << 16) | (ng << 8) | nb;
    }

    private static Palette paletteFor(StationUiProfile.Theme theme) {
        return switch (theme) {
            case BOWL -> new Palette(0xFF314A60, 0xFF7AC2CC, 0xFFD0C4B7, 0xFFF8F4EC, 0xFFF3EFE6, 0xFFEAF5F7, 0xFFE2F1F4, 0xFFE8DED1,
                    0xFF344558, 0xFFE4DED3, 0xFFCCD8DD, 0xFFF3F9FA, 0xFFF6EEE3, 0xFFEBF4F6);
            case SERVICE -> new Palette(0xFF394357, 0xFFC9A24B, 0xFFD0C3B0, 0xFFFBF7EF, 0xFFF4EFE4, 0xFFF7F0E3, 0xFFF6EFE2, 0xFFE7DED1,
                    0xFF4E596C, 0xFFF1EADC, 0xFFD9D0C2, 0xFFFBF8F3, 0xFFF8EED7, 0xFFF1EEE8);
            case RANGE -> new Palette(0xFF3E4450, 0xFFE28B42, 0xFFD0C6B9, 0xFFF8F5F0, 0xFFF1ECE4, 0xFFE7E0D6, 0xFFF1ECE4, 0xFFE1D8CC,
                    0xFF343B46, 0xFFE8DFD4, 0xFFD0C8BD, 0xFFF7F4EE, 0xFFF7E8DD, 0xFFECE6DC);
            case OVEN -> new Palette(0xFF5A4032, 0xFFDA8752, 0xFFD7C3B6, 0xFFFBF5EE, 0xFFF5ECE4, 0xFFF4E6D9, 0xFFF5ECE4, 0xFFE6D8CC,
                    0xFF47352C, 0xFFEDE1D5, 0xFFD8C8BD, 0xFFFBF7F1, 0xFFF7E3D1, 0xFFEFE7DE);
            case MACHINE -> new Palette(0xFF304A60, 0xFF5DB8CC, 0xFFCBD7DD, 0xFFF4F9FB, 0xFFEFF5F7, 0xFFE8F4F8, 0xFFE9F5F9, 0xFFDDE7EC,
                    0xFF385464, 0xFFE2EDF1, 0xFFD2DEE3, 0xFFF6FAFB, 0xFFEAF6FA, 0xFFEFF4F7);
            case GLASS -> new Palette(0xFF355264, 0xFF7DB7D0, 0xFFC7D7DD, 0xFFF4FAFC, 0xFFEFF7F9, 0xFFE8F4F8, 0xFFE9F6F8, 0xFFDDE8EB,
                    0xFF3D5B6D, 0xFFE5EFF3, 0xFFD3E2E7, 0xFFF6FBFC, 0xFFE7F5F9, 0xFFEEF5F7);
            case CROCK -> new Palette(0xFF5C4637, 0xFFAA8359, 0xFFD4C3B4, 0xFFF7F2EB, 0xFFF0E8DE, 0xFFEEE1D2, 0xFFF2E8DC, 0xFFE2D4C5,
                    0xFF513E31, 0xFFE7DACC, 0xFFD2C1B2, 0xFFF8F4ED, 0xFFF3E5D6, 0xFFEDE4DA);
            case PRESERVE -> new Palette(0xFF4D3F5C, 0xFFD06C66, 0xFFD2C4CF, 0xFFFAF5F8, 0xFFF2EAF0, 0xFFF3E7E4, 0xFFF2EBF0, 0xFFE1D5DE,
                    0xFF4A3F56, 0xFFEAE0E8, 0xFFD8CDD6, 0xFFFBF8FA, 0xFFF6E5E2, 0xFFF0EBF0);
            case STEAM -> new Palette(0xFF35515A, 0xFF63B7AE, 0xFFCBDBD8, 0xFFF7FAF8, 0xFFEFF4F1, 0xFFE6F4F1, 0xFFEAF6F2, 0xFFDBE6E3,
                    0xFF3C575F, 0xFFE3EFEA, 0xFFD1E0DC, 0xFFF8FBF9, 0xFFE7F7F2, 0xFFEEF4F2);
            case COLD -> new Palette(0xFF2F4360, 0xFF7FB6E8, 0xFFC7D4E2, 0xFFF4F8FD, 0xFFEAF0F8, 0xFFE5EDF9, 0xFFE9F0FB, 0xFFD9E3F1,
                    0xFF36506E, 0xFFE1EAF5, 0xFFD0DDEB, 0xFFF7FAFE, 0xFFE7F0FC, 0xFFECF2F8);
            case RACK -> new Palette(0xFF4E5147, 0xFF9BB06C, 0xFFD3D0C1, 0xFFF8F7F1, 0xFFF0EFE8, 0xFFEEF0E1, 0xFFF2F3E8, 0xFFE1DDD0,
                    0xFF53574B, 0xFFE7E6DA, 0xFFD2D0C4, 0xFFF9F8F3, 0xFFF0F1E2, 0xFFEFEEE6);
            case SMOKE -> new Palette(0xFF403F46, 0xFFC0895A, 0xFFD0C7C0, 0xFFF8F5F2, 0xFFF0ECE8, 0xFFF1E7DD, 0xFFF1ECE8, 0xFFE2D9D3,
                    0xFF434046, 0xFFE7E0DA, 0xFFD2CBC6, 0xFFF8F5F3, 0xFFF3E6DB, 0xFFEDE9E4);
            case MICROWAVE -> new Palette(0xFF2D4960, 0xFF65BDD7, 0xFFC8D6DD, 0xFFF4FAFB, 0xFFEFF5F7, 0xFFE7F5F8, 0xFFE9F6F8, 0xFFDCE7EB,
                    0xFF365267, 0xFFE3EDF2, 0xFFD3DFE4, 0xFFF7FBFC, 0xFFE7F5F9, 0xFFEEF4F7);
            case CITRUS -> new Palette(0xFF42604A, 0xFFF0B54B, 0xFFD6D2BF, 0xFFF9F8F2, 0xFFF2F0E6, 0xFFFBF0D8, 0xFFF7F3E6, 0xFFE5E1D0,
                    0xFF496854, 0xFFEEE9D9, 0xFFD8D3C4, 0xFFFAF8F3, 0xFFFBEFD9, 0xFFF1EEE4);
            case SPICE -> new Palette(0xFF5A4132, 0xFFD47E4E, 0xFFD8C6BC, 0xFFF9F4F0, 0xFFF2ECE5, 0xFFF5E9DD, 0xFFF4EEE7, 0xFFE5D6CB,
                    0xFF5C4738, 0xFFEADCD2, 0xFFD5C5BB, 0xFFF9F5F1, 0xFFF7E8DE, 0xFFF2ECE6);
            case BOARD -> new Palette(0xFF344558, 0xFF5AAAB1, 0xFFD3C8BB, 0xFFF8F5EE, 0xFFF1ECE4, 0xFFF0E8DC, 0xFFF1ECE4, 0xFFE5DDD1,
                    0xFF364658, 0xFFE7E0D5, 0xFFD4CCC0, 0xFFF9F6F0, 0xFFF4ECDD, 0xFFF1EEE7);
        };
    }

    enum PanelStyle {
        WORKSPACE,
        PREVIEW,
        METRIC,
        CONTROL,
        INVENTORY,
        STORAGE,
        RAISED
    }

    private record Palette(
            int headerBg,
            int headerAccent,
            int panelBorder,
            int panelSurface,
            int workSurface,
            int previewSurface,
            int metricSurface,
            int shelfSurface,
            int controlSurface,
            int metricInset,
            int meterTrack,
            int chipCool,
            int chipWarm,
            int windowSurface
        ) {
        int shadow() {
            return JazzyGuiRenderer.darken(this.panelBorder, 0.12F);
        }
    }
}
