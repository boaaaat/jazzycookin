package com.boaat.jazzy_cookin.screen;

import com.boaat.jazzy_cookin.kitchen.StationUiProfile;
import com.boaat.jazzy_cookin.kitchen.StationUiProfile.LayoutFamily;
import com.boaat.jazzy_cookin.kitchen.StationUiProfile.MetricWidgetSpec;
import com.boaat.jazzy_cookin.kitchen.StorageType;
import com.boaat.jazzy_cookin.screen.layout.ActionWidgetSpec;
import com.boaat.jazzy_cookin.screen.layout.LayoutRegion;

import net.minecraft.client.gui.GuiGraphics;

final class JazzyGuiRenderer {
    static final int TITLE_TEXT = 0xF0F2F5;
    static final int TEXT = 0xE7ECF4;
    static final int TEXT_MUTED = 0xAFB8C8;
    static final int TEXT_SOFT = 0x8B96A8;
    static final int READY_TEXT = 0x4ADE80;
    static final int BLOCKED_TEXT = 0xF87171;
    static final int ACCENT = 0x5CC8D0;
    static final int ACCENT_WARM = 0xF0B429;

    private static final int FRAME_OUTER = 0xFF101318;
    private static final int SLOT_EDGE = 0xFF353C4A;
    private static final int SLOT_BASE = 0xFF262C36;
    private static final int SLOT_INNER = 0xFF14181E;
    private static final int SLOT_DISABLED = 0x88101418;

    private JazzyGuiRenderer() {
    }

    static void drawStationShell(GuiGraphics guiGraphics, int x, int y, int width, int height, StationUiProfile.Theme theme) {
        Palette palette = paletteFor(theme);
        guiGraphics.fill(x, y, x + width, y + height, FRAME_OUTER);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, palette.windowSurface());
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + 28, palette.headerBg());
        guiGraphics.fill(x + 1, y + 28, x + width - 1, y + 30, palette.headerAccent());
        guiGraphics.fill(x + 8, y + height - 92, x + width - 8, y + height - 91, palette.panelBorder());
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
        int border = style == PanelStyle.RAISED ? pale(palette.panelBorder(), 0.12F) : palette.panelBorder();
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
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, frame);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + 3, pale(frame, 0.04F));
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
        guiGraphics.fill(left + region.x() + 12, top + region.y() + 16, left + region.right() - 12, top + region.y() + 17, palette.panelBorder());
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
        int bx = left + bounds.x();
        int by = top + bounds.y();
        int bx2 = left + bounds.right();
        int by2 = top + bounds.bottom();
        if (!active) {
            guiGraphics.fill(bx, by, bx2, by2, palette.panelBorder());
            guiGraphics.fill(bx + 1, by + 1, bx2 - 1, by2 - 1, darken(palette.controlSurface(), 0.12F));
        } else if (pressed) {
            guiGraphics.fill(bx, by, bx2, by2, palette.headerAccent());
            guiGraphics.fill(bx + 1, by + 1, bx2 - 1, by2 - 1, darken(palette.headerBg(), 0.08F));
        } else if (hovered) {
            guiGraphics.fill(bx, by, bx2, by2, palette.headerAccent());
            guiGraphics.fill(bx + 1, by + 1, bx2 - 1, by2 - 1, pale(palette.headerBg(), 0.10F));
        } else {
            guiGraphics.fill(bx, by, bx2, by2, pale(palette.headerBg(), 0.18F));
            guiGraphics.fill(bx + 1, by + 1, bx2 - 1, by2 - 1, palette.headerBg());
            guiGraphics.fill(bx + 1, by + 1, bx2 - 1, by + 3, pale(palette.headerBg(), 0.10F));
        }
    }

    static void drawChip(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean warm, StationUiProfile.Theme theme) {
        Palette palette = paletteFor(theme);
        int bg = warm ? palette.chipWarm() : palette.chipCool();
        guiGraphics.fill(x, y, x + width, y + height, pale(bg, 0.18F));
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, bg);
    }

    static void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
        guiGraphics.fill(x, y, x + 16, y + 16, SLOT_BASE);
        guiGraphics.fill(x, y, x + 16, y + 1, SLOT_INNER);
        guiGraphics.fill(x, y, x + 1, y + 16, SLOT_INNER);
        guiGraphics.fill(x + 1, y + 1, x + 15, y + 15, SLOT_INNER);
    }

    static void drawDisabledSlot(GuiGraphics guiGraphics, int x, int y) {
        drawSlot(guiGraphics, x, y);
        guiGraphics.fill(x + 1, y + 1, x + 15, y + 15, SLOT_DISABLED);
    }

    static void drawStorageTab(GuiGraphics guiGraphics, int x, int y, boolean hovered, boolean selected, StorageType storageType) {
        Palette palette = paletteFor(storageTheme(storageType));
        int frame = selected ? palette.headerAccent() : hovered ? pale(palette.headerAccent(), 0.30F) : palette.panelBorder();
        guiGraphics.fill(x, y, x + 22, y + 22, frame);
        guiGraphics.fill(x + 1, y + 1, x + 21, y + 21, palette.panelSurface());
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
        drawRing(guiGraphics, x + width / 2 - 44, y + 14, 88, 68,
                darken(palette.workSurface(), 0.15F), pale(palette.workSurface(), 0.04F), darken(palette.workSurface(), 0.08F));
        guiGraphics.fill(x + 18, y + height - 22, x + width - 18, y + height - 18, darken(palette.workSurface(), 0.10F));
    }

    private static void drawBoardBackdrop(GuiGraphics guiGraphics, int x, int y, int width, int height, Palette palette) {
        guiGraphics.fill(x + 10, y + 12, x + width - 10, y + height - 12, darken(palette.workSurface(), 0.06F));
        for (int stripe = 0; stripe < 5; stripe++) {
            int stripeX = x + 18 + stripe * 28;
            guiGraphics.fill(stripeX, y + 16, stripeX + 10, y + height - 16, pale(palette.workSurface(), 0.03F));
        }
        guiGraphics.fill(x + width - 32, y + 16, x + width - 26, y + height - 16, darken(palette.headerAccent(), 0.50F));
    }

    private static void drawRangeBackdrop(GuiGraphics guiGraphics, int x, int y, int width, int height, Palette palette) {
        guiGraphics.fill(x + 12, y + 16, x + width - 12, y + height - 16, darken(palette.workSurface(), 0.06F));
        drawRing(guiGraphics, x + 80, y + 28, 38, 28,
                darken(palette.workSurface(), 0.12F), darken(palette.workSurface(), 0.20F), darken(palette.workSurface(), 0.30F));
        drawRing(guiGraphics, x + 126, y + 38, 44, 34,
                darken(palette.workSurface(), 0.12F), darken(palette.workSurface(), 0.20F), darken(palette.workSurface(), 0.30F));
    }

    private static void drawChamberBackdrop(GuiGraphics guiGraphics, int x, int y, int width, int height, Palette palette) {
        guiGraphics.fill(x + 14, y + 14, x + width - 14, y + height - 14, darken(palette.workSurface(), 0.20F));
        for (int rail = 0; rail < 4; rail++) {
            int railY = y + 24 + rail * 16;
            guiGraphics.fill(x + 20, railY, x + width - 20, railY + 2, pale(palette.workSurface(), 0.06F));
        }
        guiGraphics.fill(x + 30, y + height - 28, x + width - 30, y + height - 20, darken(palette.workSurface(), 0.08F));
    }

    private static void drawMachineBackdrop(GuiGraphics guiGraphics, int x, int y, int width, int height, Palette palette) {
        guiGraphics.fill(x + width - 78, y + 12, x + width - 30, y + height - 14, darken(palette.workSurface(), 0.20F));
        guiGraphics.fill(x + width - 68, y + 18, x + width - 40, y + height - 20, pale(palette.workSurface(), 0.03F));
        guiGraphics.fill(x + 18, y + height - 24, x + width - 22, y + height - 18, darken(palette.workSurface(), 0.10F));
        guiGraphics.fill(x + 36, y + 14, x + 74, y + 24, pale(palette.workSurface(), 0.04F));
    }

    private static void drawPreserveBackdrop(GuiGraphics guiGraphics, int x, int y, int width, int height, Palette palette) {
        guiGraphics.fill(x + 14, y + height - 22, x + width - 14, y + height - 18, darken(palette.workSurface(), 0.10F));
        for (int jar = 0; jar < 3; jar++) {
            int jarX = x + 28 + jar * 34;
            guiGraphics.fill(jarX, y + 26, jarX + 18, y + 54, pale(palette.workSurface(), 0.04F));
            guiGraphics.fill(jarX + 2, y + 20, jarX + 16, y + 26, darken(palette.headerAccent(), 0.50F));
        }
    }

    private static void drawRestBackdrop(GuiGraphics guiGraphics, int x, int y, int width, int height, Palette palette) {
        for (int slat = 0; slat < 5; slat++) {
            int slatY = y + 18 + slat * 12;
            guiGraphics.fill(x + 16, slatY, x + width - 16, slatY + 6, pale(palette.workSurface(), 0.03F));
        }
    }

    private static void drawPlateBackdrop(GuiGraphics guiGraphics, int x, int y, int width, int height, Palette palette) {
        drawRing(guiGraphics, x + width / 2 - 42, y + 18, 84, 60,
                darken(palette.workSurface(), 0.12F), pale(palette.workSurface(), 0.04F), darken(palette.workSurface(), 0.06F));
        guiGraphics.fill(x + 24, y + height - 22, x + width - 24, y + height - 18, darken(palette.workSurface(), 0.10F));
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
            case BOWL -> new Palette(0xFF1E3550, 0xFF5BC9D4, 0xFF2A3548, 0xFF212838, 0xFF1C2535, 0xFF1E2838, 0xFF1F2735, 0xFF1A2230,
                    0xFF192430, 0xFF161D28, 0xFF253040, 0xFF1A2E40, 0xFF332B18, 0xFF1A1F2A);
            case SERVICE -> new Palette(0xFF302818, 0xFFD4A84E, 0xFF38342A, 0xFF262420, 0xFF222018, 0xFF24221A, 0xFF23211C, 0xFF1E1C18,
                    0xFF1C1A15, 0xFF18160F, 0xFF2E2A20, 0xFF1E2430, 0xFF35291A, 0xFF1D1C1A);
            case RANGE -> new Palette(0xFF302015, 0xFFE28B42, 0xFF36302A, 0xFF252220, 0xFF201E18, 0xFF221E18, 0xFF22201A, 0xFF1C1A16,
                    0xFF1A1815, 0xFF161412, 0xFF2C2820, 0xFF1E2430, 0xFF352818, 0xFF1C1A18);
            case OVEN -> new Palette(0xFF35221A, 0xFFDA8752, 0xFF38302A, 0xFF282420, 0xFF222018, 0xFF24201A, 0xFF24221C, 0xFF1E1C18,
                    0xFF1C1A15, 0xFF181510, 0xFF2E2A20, 0xFF1E2430, 0xFF35281A, 0xFF1D1A18);
            case MACHINE -> new Palette(0xFF1A3050, 0xFF5DB8CC, 0xFF2A3544, 0xFF212734, 0xFF1C2432, 0xFF1E2636, 0xFF1F2534, 0xFF1A2030,
                    0xFF18222E, 0xFF161C26, 0xFF242E3C, 0xFF1A2C3E, 0xFF302A1A, 0xFF1A1E28);
            case GLASS -> new Palette(0xFF1A3048, 0xFF7DB7D0, 0xFF2A3445, 0xFF212835, 0xFF1C2432, 0xFF1E2838, 0xFF1F2635, 0xFF1A2130,
                    0xFF18222F, 0xFF161C28, 0xFF242F3E, 0xFF1A2D40, 0xFF302B1A, 0xFF1A1F28);
            case CROCK -> new Palette(0xFF30221A, 0xFFAA8359, 0xFF36302A, 0xFF262320, 0xFF222018, 0xFF241E18, 0xFF24211C, 0xFF1E1C18,
                    0xFF1C1A15, 0xFF181510, 0xFF2C2820, 0xFF1E2430, 0xFF35261A, 0xFF1D1A18);
            case PRESERVE -> new Palette(0xFF2A1E30, 0xFFD06C66, 0xFF34303A, 0xFF252230, 0xFF201E28, 0xFF221E26, 0xFF22202A, 0xFF1C1A22,
                    0xFF1A1820, 0xFF16141C, 0xFF2A2834, 0xFF1E2438, 0xFF352220, 0xFF1C1A20);
            case STEAM -> new Palette(0xFF1A3030, 0xFF63B7AE, 0xFF2A3540, 0xFF212830, 0xFF1C2428, 0xFF1E2830, 0xFF1F262E, 0xFF1A2128,
                    0xFF18222A, 0xFF161C22, 0xFF242E38, 0xFF1A2E38, 0xFF302A1A, 0xFF1A1E22);
            case COLD -> new Palette(0xFF1A2848, 0xFF7FB6E8, 0xFF2A3248, 0xFF212738, 0xFF1C2236, 0xFF1E2638, 0xFF1F2536, 0xFF1A2030,
                    0xFF181E30, 0xFF161A26, 0xFF242C3E, 0xFF1A2A42, 0xFF302A1A, 0xFF1A1E2A);
            case RACK -> new Palette(0xFF223020, 0xFF9BB06C, 0xFF303630, 0xFF232828, 0xFF1E2420, 0xFF202620, 0xFF202522, 0xFF1A201E,
                    0xFF181E1A, 0xFF161A18, 0xFF262E28, 0xFF1E2E30, 0xFF302A1A, 0xFF1A1E1C);
            case SMOKE -> new Palette(0xFF282428, 0xFFC0895A, 0xFF343038, 0xFF252328, 0xFF201E22, 0xFF222022, 0xFF222022, 0xFF1C1A1E,
                    0xFF1A181C, 0xFF161418, 0xFF2A2830, 0xFF1E2230, 0xFF352218, 0xFF1C1B1E);
            case MICROWAVE -> new Palette(0xFF1A2C48, 0xFF65BDD7, 0xFF2A3345, 0xFF212834, 0xFF1C2332, 0xFF1E2636, 0xFF1F2534, 0xFF1A2030,
                    0xFF18222E, 0xFF161C26, 0xFF242E3C, 0xFF1A2C3E, 0xFF302A1A, 0xFF1A1E28);
            case CITRUS -> new Palette(0xFF243020, 0xFFF0B54B, 0xFF303428, 0xFF24262A, 0xFF1E2218, 0xFF22241C, 0xFF22241E, 0xFF1C1E18,
                    0xFF1A1C18, 0xFF161812, 0xFF282C22, 0xFF1E2830, 0xFF352C1A, 0xFF1C1E1A);
            case SPICE -> new Palette(0xFF30201A, 0xFFD47E4E, 0xFF38302A, 0xFF262420, 0xFF222018, 0xFF24201A, 0xFF24221C, 0xFF1E1C18,
                    0xFF1C1A15, 0xFF181510, 0xFF2C2820, 0xFF1E2430, 0xFF352418, 0xFF1D1A18);
            case BOARD -> new Palette(0xFF1A3040, 0xFF5AAAB1, 0xFF2A3442, 0xFF212834, 0xFF1C2430, 0xFF1E2632, 0xFF1F2632, 0xFF1A2028,
                    0xFF18222C, 0xFF161C24, 0xFF242E3A, 0xFF1A2C3C, 0xFF30281A, 0xFF1A1F26);
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
