package com.boaat.jazzy_cookin.kitchen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.boaat.jazzy_cookin.screen.layout.ActionWidgetSpec;
import com.boaat.jazzy_cookin.screen.layout.LayoutRegion;

public record StationUiProfile(
        StationType stationType,
        StationCapacityProfile capacity,
        Theme theme,
        int width,
        int height,
        Point[] inputPositions,
        Point toolPosition,
        Point outputPosition,
        Point byproductPosition,
        int playerInventoryStartX,
        int playerInventoryStartY,
        int hotbarY,
        KitchenScreenLayout layout
) {
    private static final int LARGE_WIDTH = 344;
    private static final int LARGE_HEIGHT = 264;
    private static final int APPLIANCE_HEIGHT = 304;
    private static final int MID_WIDTH = 328;
    private static final int MID_HEIGHT = 258;
    private static final int COMPACT_WIDTH = 312;
    private static final int COMPACT_HEIGHT = 252;
    private static final int MIN_WIDTH = 286;
    private static final int MIN_HEIGHT = 244;
    private static final int MAX_WIDTH = 384;
    private static final int MAX_HEIGHT = 324;
    private static final int SLOT_SIZE = 18;

    public StationUiProfile {
        inputPositions = Arrays.copyOf(inputPositions, inputPositions.length);
        if (inputPositions.length != capacity.inputCount()) {
            throw new IllegalArgumentException("UI profile must expose exactly " + capacity.inputCount() + " input positions");
        }
        layout.validateWithin(width, height);
    }

    public static StationUiProfile forStation(StationType stationType) {
        return switch (stationType) {
            case PREP_TABLE -> boardProfile(
                    stationType,
                    Theme.BOARD,
                    LARGE_WIDTH,
                    LARGE_HEIGHT,
                    grid(32, 56, 5, 2, 24, 22),
                    new Point(162, 78),
                    new Point(280, 62),
                    new Point(280, 102)
            );
            case MIXING_BOWL -> mixProfile(
                    stationType,
                    Theme.BOWL,
                    MID_WIDTH,
                    MID_HEIGHT,
                    new Point[] {
                            new Point(42, 62),
                            new Point(68, 44),
                            new Point(98, 40),
                            new Point(128, 52),
                            new Point(134, 88),
                            new Point(100, 104),
                            new Point(68, 100),
                            new Point(40, 84)
                    },
                    new Point(28, 82),
                    new Point(252, 64),
                    new Point(252, 104)
            );
            case PLATING_STATION -> plateProfile(
                    stationType,
                    Theme.SERVICE,
                    LARGE_WIDTH,
                    LARGE_HEIGHT,
                    new Point[] {
                            new Point(34, 60),
                            new Point(58, 46),
                            new Point(88, 42),
                            new Point(118, 54),
                            new Point(118, 90),
                            new Point(88, 104),
                            new Point(58, 100),
                            new Point(34, 86)
                    },
                    new Point(160, 82),
                    new Point(280, 62),
                    new Point(280, 102)
            );
            case STOVE -> rangeProfile(
                    stationType,
                    Theme.RANGE,
                    LARGE_WIDTH,
                    APPLIANCE_HEIGHT,
                    new Point[] {
                            new Point(60, 62),
                            new Point(100, 46),
                            new Point(140, 62),
                            new Point(140, 102),
                            new Point(100, 118),
                            new Point(60, 102)
                    },
                    new Point(172, 82),
                    new Point(282, 62),
                    new Point(282, 102)
            );
            case OVEN -> chamberProfile(
                    stationType,
                    Theme.OVEN,
                    LARGE_WIDTH,
                    APPLIANCE_HEIGHT,
                    new Point[] {
                            new Point(58, 54),
                            new Point(58, 82),
                            new Point(58, 110)
                    },
                    new Point(152, 82),
                    new Point(282, 62),
                    new Point(282, 102)
            );
            case FOOD_PROCESSOR -> machineProfile(
                    stationType,
                    Theme.MACHINE,
                    MID_WIDTH,
                    MID_HEIGHT,
                    new Point[] {
                            new Point(38, 58),
                            new Point(62, 50),
                            new Point(86, 58),
                            new Point(44, 84),
                            new Point(68, 92),
                            new Point(92, 84)
                    },
                    new Point(132, 78),
                    new Point(252, 64),
                    new Point(252, 104)
            );
            case BLENDER -> machineProfile(
                    stationType,
                    Theme.GLASS,
                    MID_WIDTH,
                    MID_HEIGHT,
                    new Point[] {
                            new Point(40, 56),
                            new Point(64, 46),
                            new Point(88, 40),
                            new Point(40, 84),
                            new Point(64, 94),
                            new Point(88, 84)
                    },
                    new Point(132, 78),
                    new Point(252, 64),
                    new Point(252, 104)
            );
            case FERMENTATION_CROCK -> preserveProfile(
                    stationType,
                    Theme.CROCK,
                    MID_WIDTH,
                    MID_HEIGHT,
                    grid(36, 56, 3, 2, 24, 24),
                    new Point(132, 80),
                    new Point(252, 64),
                    new Point(252, 104)
            );
            case CANNING_STATION -> preserveProfile(
                    stationType,
                    Theme.PRESERVE,
                    MID_WIDTH,
                    MID_HEIGHT,
                    grid(36, 56, 3, 2, 24, 24),
                    new Point(132, 80),
                    new Point(252, 64),
                    new Point(252, 104)
            );
            case STEAMER -> chamberProfile(
                    stationType,
                    Theme.STEAM,
                    MID_WIDTH,
                    MID_HEIGHT,
                    grid(38, 54, 3, 2, 24, 28),
                    new Point(132, 78),
                    new Point(252, 64),
                    new Point(252, 104)
            );
            case FREEZE_DRYER -> preserveProfile(
                    stationType,
                    Theme.COLD,
                    MID_WIDTH,
                    MID_HEIGHT,
                    new Point[] {
                            new Point(40, 58),
                            new Point(66, 58),
                            new Point(92, 58),
                            new Point(54, 88),
                            new Point(80, 88)
                    },
                    new Point(132, 78),
                    new Point(252, 64),
                    new Point(252, 104)
            );
            case DRYING_RACK -> restProfile(
                    stationType,
                    Theme.RACK,
                    COMPACT_WIDTH,
                    COMPACT_HEIGHT,
                    new Point[] {
                            new Point(40, 60),
                            new Point(66, 60),
                            new Point(92, 60),
                            new Point(54, 90),
                            new Point(80, 90)
                    },
                    new Point(126, 80),
                    new Point(236, 64),
                    new Point(236, 104)
            );
            case SMOKER -> chamberProfile(
                    stationType,
                    Theme.SMOKE,
                    MID_WIDTH,
                    MID_HEIGHT,
                    new Point[] {
                            new Point(40, 58),
                            new Point(66, 58),
                            new Point(92, 58),
                            new Point(54, 88),
                            new Point(80, 88)
                    },
                    new Point(132, 78),
                    new Point(252, 64),
                    new Point(252, 104)
            );
            case MICROWAVE -> chamberProfile(
                    stationType,
                    Theme.MICROWAVE,
                    COMPACT_WIDTH,
                    COMPACT_HEIGHT,
                    grid(40, 62, 2, 2, 26, 24),
                    new Point(126, 82),
                    new Point(236, 64),
                    new Point(236, 104)
            );
            case JUICER -> machineProfile(
                    stationType,
                    Theme.CITRUS,
                    COMPACT_WIDTH,
                    COMPACT_HEIGHT,
                    new Point[] {
                            new Point(42, 58),
                            new Point(68, 50),
                            new Point(54, 84),
                            new Point(80, 76)
                    },
                    new Point(126, 80),
                    new Point(236, 64),
                    new Point(236, 104)
            );
            case SPICE_GRINDER -> boardProfile(
                    stationType,
                    Theme.SPICE,
                    COMPACT_WIDTH,
                    COMPACT_HEIGHT,
                    grid(42, 60, 2, 2, 28, 24),
                    new Point(126, 82),
                    new Point(236, 64),
                    new Point(236, 104)
            );
            case STRAINER -> boardProfile(
                    stationType,
                    Theme.GLASS,
                    COMPACT_WIDTH,
                    COMPACT_HEIGHT,
                    grid(42, 60, 2, 2, 28, 24),
                    new Point(126, 82),
                    new Point(236, 64),
                    new Point(236, 104)
            );
            case COOLING_RACK -> restProfile(
                    stationType,
                    Theme.RACK,
                    COMPACT_WIDTH,
                    COMPACT_HEIGHT,
                    row(38, 74, 4, 26),
                    new Point(126, 78),
                    new Point(236, 64),
                    new Point(236, 104)
            );
            case RESTING_BOARD -> restProfile(
                    stationType,
                    Theme.BOARD,
                    COMPACT_WIDTH,
                    COMPACT_HEIGHT,
                    row(38, 74, 4, 26),
                    new Point(126, 78),
                    new Point(236, 64),
                    new Point(236, 104)
            );
        };
    }

    public int inventoryLabelY() {
        return this.layout.inventoryLabelRegion().y();
    }

    public StationUiProfile resolve(int screenWidth, int screenHeight) {
        int resolvedWidth = resolveWidth(screenWidth, this.width);
        int resolvedHeight = resolveHeight(screenHeight, this.height);
        if (resolvedWidth == this.width && resolvedHeight == this.height) {
            return this;
        }
        return buildProfile(
                this.stationType,
                this.theme,
                resolvedWidth,
                resolvedHeight,
                this.inputPositions,
                this.toolPosition,
                this.outputPosition,
                this.byproductPosition,
                this.layout.family(),
                this.layout.workspaceRegion(),
                this.layout.toolRegion(),
                this.layout.previewRegion(),
                this.layout.outputRegion(),
                this.layout.byproductRegion(),
                this.layout.metricClusterRegion()
        );
    }

    private static StationUiProfile boardProfile(
            StationType stationType,
            Theme theme,
            int width,
            int height,
            Point[] inputs,
            Point tool,
            Point output,
            Point byproduct
    ) {
        int sideWidth = width >= LARGE_WIDTH ? 108 : 94;
        int sideX = width - sideWidth - 18;
        int workspaceWidth = sideX - 30;
        return buildProfile(
                stationType,
                theme,
                width,
                height,
                inputs,
                tool,
                output,
                byproduct,
                LayoutFamily.PREP,
                new LayoutRegion(18, 42, workspaceWidth, 88),
                regionAround(tool, 26, 26),
                new LayoutRegion(sideX, 42, sideWidth, 82),
                new LayoutRegion(output.x() - 10, output.y() - 10, 34, 30),
                new LayoutRegion(byproduct.x() - 10, byproduct.y() - 10, 34, 30),
                new LayoutRegion(sideX, 130, sideWidth, 36)
        );
    }

    private static StationUiProfile mixProfile(
            StationType stationType,
            Theme theme,
            int width,
            int height,
            Point[] inputs,
            Point tool,
            Point output,
            Point byproduct
    ) {
        return buildProfile(
                stationType,
                theme,
                width,
                height,
                inputs,
                tool,
                output,
                byproduct,
                LayoutFamily.MIX,
                new LayoutRegion(18, 42, 178, 100),
                regionAround(tool, 24, 24),
                new LayoutRegion(210, 42, 100, 82),
                new LayoutRegion(output.x() - 10, output.y() - 10, 34, 30),
                new LayoutRegion(byproduct.x() - 10, byproduct.y() - 10, 34, 30),
                new LayoutRegion(210, 130, 100, 42)
        );
    }

    private static StationUiProfile plateProfile(
            StationType stationType,
            Theme theme,
            int width,
            int height,
            Point[] inputs,
            Point tool,
            Point output,
            Point byproduct
    ) {
        int sideWidth = width >= LARGE_WIDTH ? 108 : 94;
        int sideX = width - sideWidth - 18;
        int workspaceWidth = sideX - 30;
        return buildProfile(
                stationType,
                theme,
                width,
                height,
                inputs,
                tool,
                output,
                byproduct,
                LayoutFamily.PLATE,
                new LayoutRegion(18, 42, workspaceWidth, 100),
                regionAround(tool, 28, 28),
                new LayoutRegion(sideX, 42, sideWidth, 82),
                new LayoutRegion(output.x() - 10, output.y() - 10, 34, 30),
                new LayoutRegion(byproduct.x() - 10, byproduct.y() - 10, 34, 30),
                new LayoutRegion(sideX, 130, sideWidth, 40)
        );
    }

    private static StationUiProfile rangeProfile(
            StationType stationType,
            Theme theme,
            int width,
            int height,
            Point[] inputs,
            Point tool,
            Point output,
            Point byproduct
    ) {
        int sideWidth = width >= LARGE_WIDTH ? 108 : 94;
        int sideX = width - sideWidth - 18;
        int workspaceWidth = sideX - 30;
        return buildProfile(
                stationType,
                theme,
                width,
                height,
                inputs,
                tool,
                output,
                byproduct,
                LayoutFamily.RANGE,
                new LayoutRegion(18, 42, workspaceWidth, 92),
                regionAround(tool, 34, 30),
                new LayoutRegion(sideX, 42, sideWidth, 82),
                new LayoutRegion(output.x() - 10, output.y() - 10, 34, 30),
                new LayoutRegion(byproduct.x() - 10, byproduct.y() - 10, 34, 30),
                new LayoutRegion(sideX, 130, sideWidth, 40)
        );
    }

    private static StationUiProfile chamberProfile(
            StationType stationType,
            Theme theme,
            int width,
            int height,
            Point[] inputs,
            Point tool,
            Point output,
            Point byproduct
    ) {
        int sideWidth = width >= LARGE_WIDTH ? 108 : 94;
        int sideX = width - sideWidth - 18;
        int workspaceWidth = sideX - 30;
        return buildProfile(
                stationType,
                theme,
                width,
                height,
                inputs,
                tool,
                output,
                byproduct,
                LayoutFamily.CHAMBER,
                new LayoutRegion(18, 42, workspaceWidth, 92),
                regionAround(tool, 34, 30),
                new LayoutRegion(sideX, 42, sideWidth, 82),
                new LayoutRegion(output.x() - 10, output.y() - 10, 34, 30),
                new LayoutRegion(byproduct.x() - 10, byproduct.y() - 10, 34, 30),
                new LayoutRegion(sideX, 130, sideWidth, 40)
        );
    }

    private static StationUiProfile machineProfile(
            StationType stationType,
            Theme theme,
            int width,
            int height,
            Point[] inputs,
            Point tool,
            Point output,
            Point byproduct
    ) {
        int sideWidth = 100;
        int sideX = width - sideWidth - 18;
        int workspaceWidth = sideX - 30;
        return buildProfile(
                stationType,
                theme,
                width,
                height,
                inputs,
                tool,
                output,
                byproduct,
                LayoutFamily.MACHINE,
                new LayoutRegion(18, 42, workspaceWidth, 98),
                regionAround(tool, 26, 26),
                new LayoutRegion(sideX, 42, sideWidth, 82),
                new LayoutRegion(output.x() - 10, output.y() - 10, 34, 30),
                new LayoutRegion(byproduct.x() - 10, byproduct.y() - 10, 34, 30),
                new LayoutRegion(sideX, 130, sideWidth, 42)
        );
    }

    private static StationUiProfile preserveProfile(
            StationType stationType,
            Theme theme,
            int width,
            int height,
            Point[] inputs,
            Point tool,
            Point output,
            Point byproduct
    ) {
        int sideWidth = 100;
        int sideX = width - sideWidth - 18;
        int workspaceWidth = sideX - 30;
        return buildProfile(
                stationType,
                theme,
                width,
                height,
                inputs,
                tool,
                output,
                byproduct,
                LayoutFamily.PRESERVE,
                new LayoutRegion(18, 42, workspaceWidth, 98),
                regionAround(tool, 26, 26),
                new LayoutRegion(sideX, 42, sideWidth, 82),
                new LayoutRegion(output.x() - 10, output.y() - 10, 34, 30),
                new LayoutRegion(byproduct.x() - 10, byproduct.y() - 10, 34, 30),
                new LayoutRegion(sideX, 130, sideWidth, 42)
        );
    }

    private static StationUiProfile restProfile(
            StationType stationType,
            Theme theme,
            int width,
            int height,
            Point[] inputs,
            Point tool,
            Point output,
            Point byproduct
    ) {
        return buildProfile(
                stationType,
                theme,
                width,
                height,
                inputs,
                tool,
                output,
                byproduct,
                LayoutFamily.REST,
                new LayoutRegion(18, 42, 176, 96),
                regionAround(tool, 24, 24),
                new LayoutRegion(width - 112, 42, 94, 82),
                new LayoutRegion(output.x() - 10, output.y() - 10, 34, 30),
                new LayoutRegion(byproduct.x() - 10, byproduct.y() - 10, 34, 30),
                new LayoutRegion(width - 112, 130, 94, 38)
        );
    }

    private static StationUiProfile buildProfile(
            StationType stationType,
            Theme theme,
            int width,
            int height,
            Point[] inputs,
            Point tool,
            Point output,
            Point byproduct,
            LayoutFamily family,
            LayoutRegion workspaceRegion,
            LayoutRegion toolRegion,
            LayoutRegion previewRegion,
            LayoutRegion outputRegion,
            LayoutRegion byproductRegion,
            LayoutRegion metricRegion
    ) {
        StationCapacityProfile resolvedCapacity = StationCapacityProfile.forStation(stationType);
        int margin = width <= 300 ? 14 : 18;
        int contentTop = 40;
        int controlHeight = stationType == StationType.OVEN ? 48 : stationType == StationType.STOVE ? 36 : 28;
        int inventoryStartY = height - 84;
        int hotbarY = height - 26;
        LayoutRegion inventoryShelf = new LayoutRegion(10, inventoryStartY - 17, width - 20, 91);
        int controlTop = inventoryShelf.y() - controlHeight - 8;
        int contentBottom = Math.max(contentTop + 58, controlTop - 8);
        int contentHeight = contentBottom - contentTop;

        int previewWidth = clamp(Math.round(width * 0.26F), 66, width >= LARGE_WIDTH ? 104 : 92);
        int metricWidth = clamp(Math.round(width * 0.18F), 48, width >= LARGE_WIDTH ? 70 : 60);
        int gap = width <= 300 ? 6 : 8;
        int workspaceWidth = width - margin * 2 - previewWidth - metricWidth - gap * 2;
        if (workspaceWidth < 116) {
            int shortage = 116 - workspaceWidth;
            int previewReduction = Math.min(shortage, Math.max(0, previewWidth - 62));
            previewWidth -= previewReduction;
            shortage -= previewReduction;
            metricWidth -= Math.min(shortage, Math.max(0, metricWidth - 44));
            workspaceWidth = width - margin * 2 - previewWidth - metricWidth - gap * 2;
        }

        LayoutRegion resolvedWorkspaceRegion = new LayoutRegion(margin, contentTop, Math.max(104, workspaceWidth), contentHeight);
        LayoutRegion resolvedMetricRegion = new LayoutRegion(resolvedWorkspaceRegion.right() + gap, contentTop, metricWidth, contentHeight);
        LayoutRegion resolvedPreviewRegion = new LayoutRegion(resolvedMetricRegion.right() + gap, contentTop,
                width - margin - (resolvedMetricRegion.right() + gap), contentHeight);

        LayoutRegion resolvedToolRegion = stationType.usesTools()
                ? new LayoutRegion(
                resolvedWorkspaceRegion.right() - 32,
                resolvedWorkspaceRegion.y() + Math.max(8, (resolvedWorkspaceRegion.height() - 26) / 2),
                26,
                26)
                : null;
        Point[] resolvedInputs = arrangeInputSlots(resolvedCapacity.inputCount(), resolvedWorkspaceRegion, resolvedToolRegion);
        Point resolvedToolPoint = resolvedToolRegion == null
                ? new Point(resolvedWorkspaceRegion.right() - 26, resolvedWorkspaceRegion.bottom() - 26)
                : slotPoint(resolvedToolRegion);
        Point[] previewSlots = previewSlotPoints(resolvedPreviewRegion);
        Point resolvedOutputPoint = previewSlots[0];
        Point resolvedByproductPoint = previewSlots[1];
        LayoutRegion resolvedOutputRegion = slotFrame(resolvedOutputPoint);
        LayoutRegion resolvedByproductRegion = slotFrame(resolvedByproductPoint);

        LayoutRegion controlStrip = new LayoutRegion(margin, controlTop, width - margin * 2, controlHeight);
        int chipWidth = clamp(width / 4, 62, 86);
        int chipX = width - chipWidth - 100;
        LayoutRegion titleRegion = new LayoutRegion(14, 10, Math.max(64, chipX - 28), 14);
        LayoutRegion chipRegion = new LayoutRegion(chipX, 8, chipWidth, 18);
        LayoutRegion previewStatusRegion = new LayoutRegion(
                resolvedPreviewRegion.x() + 6,
                resolvedPreviewRegion.y() + 7,
                Math.max(28, resolvedPreviewRegion.width() - 12),
                Math.max(10, resolvedOutputRegion.y() - resolvedPreviewRegion.y() - 12)
        );
        LayoutRegion inventoryLabelRegion = new LayoutRegion(centeredInventoryStart(width), inventoryStartY - 13, 96, 10);
        LayoutRegion helperRegion = new LayoutRegion(controlStrip.x() + 8, controlStrip.y() + 4, Math.max(32, controlStrip.width() - 16), 10);
        int primaryWidth = clamp(Math.round(controlStrip.width() * 0.24F), 54, 72);
        int auxWidth = clamp(Math.round(controlStrip.width() * 0.16F), 24, 42);
        int primaryX = controlStrip.right() - primaryWidth - 8;
        int secondaryX = stationType == StationType.STOVE ? primaryX - auxWidth - 4 : primaryX;
        int tertiaryX = stationType == StationType.STOVE ? secondaryX - auxWidth - 4 : primaryX;
        int actionClusterLeft = stationType == StationType.STOVE ? tertiaryX : primaryX;
        LayoutRegion actionCluster = new LayoutRegion(actionClusterLeft, controlStrip.y() + 3, controlStrip.right() - actionClusterLeft - 8, controlStrip.height() - 6);
        int controlTextLeft = controlStrip.x() + (stationType.supportsHeat() ? 94 : stationType.supportsStationControl() ? 58 : 8);
        LayoutRegion controlLabel = new LayoutRegion(controlTextLeft, controlStrip.y() + Math.max(5, (controlStrip.height() - 8) / 2),
                Math.max(24, actionCluster.x() - controlTextLeft - 6), 10);
        LayoutRegion statusLane = new LayoutRegion(resolvedMetricRegion.x() + 6, resolvedMetricRegion.y() + 2,
                Math.max(28, resolvedMetricRegion.width() - 12), Math.max(6, resolvedMetricRegion.height() - 4));

        int actionY = controlStrip.y() + Math.max(4, (controlStrip.height() - 18) / 2);
        ActionWidgetSpec primaryAction = button(primaryX, actionY, primaryWidth, 18);
        ActionWidgetSpec secondaryAction = button(secondaryX, actionY, auxWidth, 18);
        ActionWidgetSpec tertiaryAction = button(tertiaryX, actionY, auxWidth, 18);
        ActionWidgetSpec lowHeatAction = button(controlStrip.x() + 32, actionY, 18, 18);
        ActionWidgetSpec mediumHeatAction = button(controlStrip.x() + 54, actionY, 18, 18);
        ActionWidgetSpec highHeatAction = button(controlStrip.x() + 76, actionY, 18, 18);
        ActionWidgetSpec lowerControlAction = button(controlStrip.x() + 8, actionY, 18, 18);
        ActionWidgetSpec raiseControlAction = button(controlStrip.x() + 32, actionY, 18, 18);
        LayoutRegion controlChip = new LayoutRegion(controlLabel.x(), actionY, controlLabel.width(), 18);
        LayoutRegion ovenField = new LayoutRegion(controlStrip.x() + 42, actionY, 42, 18);

        KitchenScreenLayout layout = new KitchenScreenLayout(
                family,
                resolvedWorkspaceRegion,
                resolvedToolRegion,
                resolvedPreviewRegion,
                resolvedOutputRegion,
                resolvedByproductRegion,
                resolvedMetricRegion,
                controlStrip,
                inventoryShelf,
                titleRegion,
                chipRegion,
                previewStatusRegion,
                helperRegion,
                inventoryLabelRegion,
                controlLabel,
                actionCluster,
                statusLane,
                primaryAction,
                secondaryAction,
                tertiaryAction,
                stationType.supportsHeat() && stationType != StationType.OVEN ? lowHeatAction : null,
                stationType.supportsHeat() && stationType != StationType.OVEN ? mediumHeatAction : null,
                stationType.supportsHeat() && stationType != StationType.OVEN ? highHeatAction : null,
                stationType.supportsStationControl() ? lowerControlAction : null,
                stationType.supportsStationControl() ? raiseControlAction : null,
                stationType == StationType.OVEN ? null : (stationType.supportsHeat() || stationType.supportsStationControl() ? controlChip : null),
                stationType == StationType.OVEN || stationType == StationType.MICROWAVE ? ovenField : null
        );

        return new StationUiProfile(
                stationType,
                resolvedCapacity,
                theme,
                width,
                height,
                resolvedInputs,
                resolvedToolPoint,
                resolvedOutputPoint,
                resolvedByproductPoint,
                centeredInventoryStart(width),
                inventoryStartY,
                hotbarY,
                layout
        );
    }

    private static Point[] arrangeInputSlots(int count, LayoutRegion workspaceRegion, LayoutRegion toolRegion) {
        if (count <= 0) {
            return new Point[0];
        }

        int reservedRight = toolRegion == null ? 0 : 34;
        int areaX = workspaceRegion.x() + 8;
        int areaY = workspaceRegion.y() + 12;
        int areaWidth = Math.max(SLOT_SIZE, workspaceRegion.width() - 16 - reservedRight);
        int areaHeight = Math.max(SLOT_SIZE, workspaceRegion.height() - 20);
        int gapX = workspaceRegion.width() <= 124 ? 4 : 6;
        int gapY = workspaceRegion.height() <= 70 ? 4 : 6;
        int maxColumns = Math.max(1, (areaWidth + gapX) / (SLOT_SIZE + gapX));
        int columns = Math.min(count, maxColumns);
        if (count >= 8 && maxColumns >= 4) {
            columns = Math.min(maxColumns, 4);
        } else if (count >= 5 && maxColumns >= 3) {
            columns = Math.min(maxColumns, 3);
        }
        int rows = (count + columns - 1) / columns;
        while (rows * SLOT_SIZE + Math.max(0, rows - 1) * gapY > areaHeight && columns < maxColumns) {
            columns++;
            rows = (count + columns - 1) / columns;
        }

        Point[] points = new Point[count];
        int gridHeight = rows * SLOT_SIZE + Math.max(0, rows - 1) * gapY;
        int startY = areaY + Math.max(0, (areaHeight - gridHeight) / 2);
        for (int index = 0; index < count; index++) {
            int row = index / columns;
            int col = index % columns;
            int rowCount = Math.min(columns, count - row * columns);
            int rowWidth = rowCount * SLOT_SIZE + Math.max(0, rowCount - 1) * gapX;
            int startX = areaX + Math.max(0, (areaWidth - rowWidth) / 2);
            points[index] = new Point(startX + col * (SLOT_SIZE + gapX), startY + row * (SLOT_SIZE + gapY));
        }
        return points;
    }

    private static Point[] previewSlotPoints(LayoutRegion previewRegion) {
        int centerX = previewRegion.x() + Math.max(0, (previewRegion.width() - 16) / 2);
        if (previewRegion.height() < 82) {
            int y = previewRegion.bottom() - 26;
            int firstX = clamp(previewRegion.centerX() - 22, previewRegion.x() + 10, previewRegion.right() - 52);
            int secondX = Math.min(previewRegion.right() - 24, firstX + 28);
            return new Point[] { new Point(firstX, y), new Point(secondX, y) };
        }
        int firstY = previewRegion.bottom() - 54;
        return new Point[] { new Point(centerX, firstY), new Point(centerX, firstY + 28) };
    }

    private static ActionWidgetSpec button(int x, int y, int width, int height) {
        LayoutRegion bounds = new LayoutRegion(x, y, width, height);
        LayoutRegion label = new LayoutRegion(x, y + 4, width, 8);
        return new ActionWidgetSpec(bounds, label);
    }

    private static Point slotPoint(LayoutRegion region) {
        return new Point(region.x() + Math.max(0, (region.width() - 16) / 2), region.y() + Math.max(0, (region.height() - 16) / 2));
    }

    private static LayoutRegion slotFrame(Point point) {
        return new LayoutRegion(point.x() - 10, point.y() - 10, 34, 30);
    }

    private static int resolveWidth(int screenWidth, int preferredWidth) {
        int available = Math.max(MIN_WIDTH, screenWidth - 24);
        int preferred = Math.max(preferredWidth, screenWidth >= 520 ? preferredWidth + 24 : preferredWidth);
        return clamp(preferred, MIN_WIDTH, Math.min(MAX_WIDTH, available));
    }

    private static int resolveHeight(int screenHeight, int preferredHeight) {
        int available = Math.max(MIN_HEIGHT, screenHeight - 24);
        int preferred = Math.max(preferredHeight, screenHeight >= 380 ? preferredHeight + 12 : preferredHeight);
        return clamp(preferred, MIN_HEIGHT, Math.min(MAX_HEIGHT, available));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static LayoutRegion regionAround(Point center, int width, int height) {
        return new LayoutRegion(center.x() - width / 2 + 8, center.y() - height / 2 + 8, width, height);
    }

    private static int centeredInventoryStart(int width) {
        return (width - 162) / 2;
    }

    private static Point[] grid(int startX, int startY, int cols, int rows, int gapX, int gapY) {
        Point[] points = new Point[cols * rows];
        int index = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                points[index++] = new Point(startX + col * gapX, startY + row * gapY);
            }
        }
        return points;
    }

    private static Point[] row(int startX, int startY, int count, int gapX) {
        Point[] points = new Point[count];
        for (int index = 0; index < count; index++) {
            points[index] = new Point(startX + index * gapX, startY);
        }
        return points;
    }

    public record Point(int x, int y) {
    }

    public enum LayoutFamily {
        MIX,
        PREP,
        RANGE,
        CHAMBER,
        MACHINE,
        PRESERVE,
        REST,
        PLATE
    }

    public record MetricWidgetSpec(
            LayoutRegion bounds,
            LayoutRegion labelBounds,
            LayoutRegion meterBounds,
            LayoutRegion valueBounds
    ) {
    }

    public record KitchenScreenLayout(
            LayoutFamily family,
            LayoutRegion workspaceRegion,
            LayoutRegion toolRegion,
            LayoutRegion previewRegion,
            LayoutRegion outputRegion,
            LayoutRegion byproductRegion,
            LayoutRegion metricClusterRegion,
            LayoutRegion controlStripRegion,
            LayoutRegion inventoryShelfRegion,
            LayoutRegion titleRegion,
            LayoutRegion headerChipRegion,
            LayoutRegion previewStatusRegion,
            LayoutRegion helperTextRegion,
            LayoutRegion inventoryLabelRegion,
            LayoutRegion controlLabelRegion,
            LayoutRegion actionClusterRegion,
            LayoutRegion statusLaneRegion,
            ActionWidgetSpec primaryAction,
            ActionWidgetSpec secondaryAction,
            ActionWidgetSpec tertiaryAction,
            ActionWidgetSpec lowHeatAction,
            ActionWidgetSpec mediumHeatAction,
            ActionWidgetSpec highHeatAction,
            ActionWidgetSpec lowerControlAction,
            ActionWidgetSpec raiseControlAction,
            LayoutRegion controlChipRegion,
            LayoutRegion ovenFieldRegion
    ) {
        public MetricWidgetSpec metricWidget(int index, int count) {
            int gap = count > 2 ? 2 : 3;
            int widgetHeight = Math.max(6, (this.metricClusterRegion.height() - Math.max(0, count - 1) * gap) / Math.max(1, count));
            int y = this.metricClusterRegion.y() + index * (widgetHeight + gap);
            LayoutRegion widget = new LayoutRegion(this.metricClusterRegion.x(), y, this.metricClusterRegion.width(), widgetHeight);
            LayoutRegion label = new LayoutRegion(widget.x() + 5, widget.y() + 1, Math.max(18, widget.width() - 36), 8);
            LayoutRegion meter = new LayoutRegion(widget.x() + 5, widget.bottom() - 3, Math.max(22, widget.width() - 30), 2);
            LayoutRegion value = new LayoutRegion(widget.right() - 22, widget.y() + 1, 16, 8);
            return new MetricWidgetSpec(widget, label, meter, value);
        }

        public void validateWithin(int width, int height) {
            List<LayoutRegion> topLevel = List.of(
                    this.workspaceRegion,
                    this.previewRegion,
                    this.metricClusterRegion,
                    this.controlStripRegion,
                    this.inventoryShelfRegion,
                    this.titleRegion,
                    this.headerChipRegion,
                    this.previewStatusRegion,
                    this.controlLabelRegion,
                    this.actionClusterRegion,
                    this.statusLaneRegion
            );
            for (LayoutRegion region : topLevel) {
                this.requireBounds(region, width, height);
            }
            for (LayoutRegion nested : new LayoutRegion[] {
                    this.toolRegion,
                    this.outputRegion,
                    this.byproductRegion,
                    this.helperTextRegion,
                    this.inventoryLabelRegion,
                    this.controlChipRegion,
                    this.ovenFieldRegion
            }) {
                if (nested != null) {
                    this.requireBounds(nested, width, height);
                }
            }
            for (ActionWidgetSpec action : this.actionWidgets()) {
                this.requireBounds(action.bounds(), width, height);
                if (action.captionBounds() != null) {
                    this.requireBounds(action.captionBounds(), width, height);
                }
            }

            this.requireNoOverlap(this.workspaceRegion, this.previewRegion, "workspace", "preview");
            this.requireNoOverlap(this.workspaceRegion, this.metricClusterRegion, "workspace", "metrics");
            this.requireNoOverlap(this.previewRegion, this.metricClusterRegion, "preview", "metrics");
            this.requireNoOverlap(this.controlStripRegion, this.metricClusterRegion, "controls", "metrics");
            this.requireNoOverlap(this.controlStripRegion, this.inventoryShelfRegion, "controls", "inventory");
            this.requireNoOverlap(this.titleRegion, this.headerChipRegion, "title", "chip");

            if (this.toolRegion != null && !this.workspaceRegion.contains(this.toolRegion)) {
                throw new IllegalArgumentException("Tool region must stay within workspace region");
            }
            if (this.outputRegion != null && !this.previewRegion.contains(this.outputRegion)) {
                throw new IllegalArgumentException("Output region must stay within preview region");
            }
            if (this.byproductRegion != null && !this.previewRegion.contains(this.byproductRegion)) {
                throw new IllegalArgumentException("Byproduct region must stay within preview region");
            }
            if (!this.previewRegion.contains(this.previewStatusRegion)) {
                throw new IllegalArgumentException("Preview status region must stay within preview region");
            }
            if (!this.controlStripRegion.contains(this.controlLabelRegion)) {
                throw new IllegalArgumentException("Control label region must stay within control strip");
            }
            if (!this.controlStripRegion.contains(this.actionClusterRegion)) {
                throw new IllegalArgumentException("Action cluster region must stay within control strip");
            }
            if (!this.metricClusterRegion.contains(this.statusLaneRegion)) {
                throw new IllegalArgumentException("Status lane region must stay within metric region");
            }
        }

        private List<ActionWidgetSpec> actionWidgets() {
            ArrayList<ActionWidgetSpec> widgets = new ArrayList<>();
            for (ActionWidgetSpec action : new ActionWidgetSpec[] {
                    this.primaryAction,
                    this.secondaryAction,
                    this.tertiaryAction,
                    this.lowHeatAction,
                    this.mediumHeatAction,
                    this.highHeatAction,
                    this.lowerControlAction,
                    this.raiseControlAction
            }) {
                if (action != null) {
                    widgets.add(action);
                }
            }
            return widgets;
        }

        private void requireBounds(LayoutRegion region, int width, int height) {
            if (region.x() < 0 || region.y() < 0 || region.right() > width || region.bottom() > height) {
                throw new IllegalArgumentException("Layout region out of bounds: " + region);
            }
        }

        private void requireNoOverlap(LayoutRegion first, LayoutRegion second, String firstName, String secondName) {
            if (first != null && second != null && first.intersects(second)) {
                throw new IllegalArgumentException("Layout regions overlap: " + firstName + " and " + secondName);
            }
        }
    }

    public enum Theme {
        BOARD,
        BOWL,
        SERVICE,
        RANGE,
        OVEN,
        MACHINE,
        GLASS,
        CROCK,
        PRESERVE,
        STEAM,
        COLD,
        RACK,
        SMOKE,
        MICROWAVE,
        CITRUS,
        SPICE
    }
}
