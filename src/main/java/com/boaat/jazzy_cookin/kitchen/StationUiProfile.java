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
    private static final int MID_WIDTH = 328;
    private static final int MID_HEIGHT = 258;
    private static final int COMPACT_WIDTH = 312;
    private static final int COMPACT_HEIGHT = 252;

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
                    LARGE_HEIGHT,
                    grid(34, 58, 3, 2, 24, 22),
                    new Point(130, 76),
                    new Point(282, 62),
                    new Point(282, 102)
            );
            case OVEN -> chamberProfile(
                    stationType,
                    Theme.OVEN,
                    LARGE_WIDTH,
                    LARGE_HEIGHT,
                    grid(34, 58, 3, 2, 26, 24),
                    new Point(132, 76),
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
        int inventoryX = centeredInventoryStart(width);
        int inventoryStartY = height - 88;
        int hotbarY = height - 30;
        LayoutRegion inventoryShelf = new LayoutRegion(14, inventoryStartY - 18, width - 28, 98);
        LayoutRegion controlStrip = new LayoutRegion(18, inventoryShelf.y() - 34, width - 36, 26);
        LayoutRegion titleRegion = new LayoutRegion(14, 10, 190, 14);
        LayoutRegion chipRegion = new LayoutRegion(width - 106, 8, 92, 18);
        LayoutRegion inventoryLabelRegion = new LayoutRegion(inventoryX, inventoryStartY - 13, 96, 10);
        LayoutRegion helperRegion = new LayoutRegion(controlStrip.x() + 90, controlStrip.y() + 8, Math.max(40, controlStrip.width() - 200), 10);

        ActionWidgetSpec primaryAction = button(controlStrip.right() - 64, controlStrip.y() + 4, 54, 18);
        ActionWidgetSpec secondaryAction = button(controlStrip.right() - 126, controlStrip.y() + 4, 54, 18);
        ActionWidgetSpec tertiaryAction = button(controlStrip.right() - 188, controlStrip.y() + 4, 54, 18);
        ActionWidgetSpec lowHeatAction = button(controlStrip.x() + 12, controlStrip.y() + 4, 18, 18);
        ActionWidgetSpec mediumHeatAction = button(controlStrip.x() + 34, controlStrip.y() + 4, 18, 18);
        ActionWidgetSpec highHeatAction = button(controlStrip.x() + 56, controlStrip.y() + 4, 18, 18);
        ActionWidgetSpec lowerControlAction = button(controlStrip.x() + 12, controlStrip.y() + 4, 18, 18);
        ActionWidgetSpec raiseControlAction = button(controlStrip.x() + 84, controlStrip.y() + 4, 18, 18);
        LayoutRegion controlChip = new LayoutRegion(controlStrip.x() + 34, controlStrip.y() + 6, 48, 14);
        LayoutRegion ovenField = new LayoutRegion(controlStrip.x() + 32, controlStrip.y() + 4, 44, 18);

        KitchenScreenLayout layout = new KitchenScreenLayout(
                family,
                workspaceRegion,
                toolRegion,
                previewRegion,
                outputRegion,
                byproductRegion,
                metricRegion,
                controlStrip,
                inventoryShelf,
                titleRegion,
                chipRegion,
                helperRegion,
                inventoryLabelRegion,
                primaryAction,
                secondaryAction,
                tertiaryAction,
                stationType.supportsHeat() && stationType != StationType.OVEN ? lowHeatAction : null,
                stationType.supportsHeat() && stationType != StationType.OVEN ? mediumHeatAction : null,
                stationType.supportsHeat() && stationType != StationType.OVEN ? highHeatAction : null,
                stationType.supportsStationControl() ? lowerControlAction : null,
                stationType.supportsStationControl() ? raiseControlAction : null,
                stationType == StationType.OVEN ? null : (stationType.supportsHeat() || stationType.supportsStationControl() ? controlChip : null),
                stationType == StationType.OVEN ? ovenField : null
        );

        return new StationUiProfile(
                stationType,
                StationCapacityProfile.forStation(stationType),
                theme,
                width,
                height,
                inputs,
                tool,
                output,
                byproduct,
                inventoryX,
                inventoryStartY,
                hotbarY,
                layout
        );
    }

    private static ActionWidgetSpec button(int x, int y, int width, int height) {
        LayoutRegion bounds = new LayoutRegion(x, y, width, height);
        LayoutRegion label = new LayoutRegion(x, y + 4, width, 8);
        return new ActionWidgetSpec(bounds, label);
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
            LayoutRegion helperTextRegion,
            LayoutRegion inventoryLabelRegion,
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
            int gap = 6;
            int widgetHeight = Math.max(16, (this.metricClusterRegion.height() - Math.max(0, count - 1) * gap) / Math.max(1, count));
            int y = this.metricClusterRegion.y() + index * (widgetHeight + gap);
            LayoutRegion widget = new LayoutRegion(this.metricClusterRegion.x(), y, this.metricClusterRegion.width(), widgetHeight);
            LayoutRegion label = new LayoutRegion(widget.x() + 8, widget.y() + 4, Math.max(30, widget.width() - 56), 8);
            LayoutRegion meter = new LayoutRegion(widget.x() + 8, widget.bottom() - 7, Math.max(42, widget.width() - 48), 4);
            LayoutRegion value = new LayoutRegion(widget.right() - 34, widget.y() + 4, 28, 8);
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
                    this.headerChipRegion
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
