package com.boaat.jazzy_cookin.kitchen;

import java.util.Arrays;

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
        int hotbarY
) {
    private static final int SLOT_SIZE = 18;

    public StationUiProfile {
        inputPositions = Arrays.copyOf(inputPositions, inputPositions.length);
        if (inputPositions.length != capacity.inputCount()) {
            throw new IllegalArgumentException("UI profile must expose exactly " + capacity.inputCount() + " input positions");
        }
    }

    public static StationUiProfile forStation(StationType stationType) {
        return switch (stationType) {
            case PREP_TABLE -> largeProfile(
                    stationType,
                    Theme.BOARD,
                    292,
                    232,
                    rowMajor(28, 52, 5, 18, 20, 10),
                    new Point(124, 62),
                    new Point(238, 56),
                    new Point(238, 78)
            );
            case MIXING_BOWL -> midProfile(
                    stationType,
                    Theme.BOWL,
                    272,
                    226,
                    new Point[] {
                            new Point(30, 56),
                            new Point(52, 46),
                            new Point(74, 44),
                            new Point(96, 54),
                            new Point(30, 78),
                            new Point(52, 88),
                            new Point(74, 90),
                            new Point(96, 80)
                    },
                    new Point(126, 66),
                    new Point(220, 56),
                    new Point(220, 78)
            );
            case PLATING_STATION -> largeProfile(
                    stationType,
                    Theme.SERVICE,
                    292,
                    232,
                    new Point[] {
                            new Point(28, 56),
                            new Point(50, 46),
                            new Point(72, 44),
                            new Point(94, 54),
                            new Point(28, 78),
                            new Point(50, 88),
                            new Point(72, 90),
                            new Point(94, 80)
                    },
                    new Point(124, 66),
                    new Point(238, 56),
                    new Point(238, 78)
            );
            case STOVE -> largeProfile(
                    stationType,
                    Theme.RANGE,
                    292,
                    232,
                    rowMajor(28, 54, 3, 18, 20, 6),
                    new Point(104, 64),
                    new Point(238, 56),
                    new Point(238, 78)
            );
            case OVEN -> largeProfile(
                    stationType,
                    Theme.OVEN,
                    292,
                    232,
                    rowMajor(28, 52, 3, 18, 22, 6),
                    new Point(106, 64),
                    new Point(238, 56),
                    new Point(238, 78)
            );
            case FOOD_PROCESSOR -> midProfile(
                    stationType,
                    Theme.MACHINE,
                    268,
                    226,
                    new Point[] {
                            new Point(30, 54),
                            new Point(50, 54),
                            new Point(70, 54),
                            new Point(38, 76),
                            new Point(58, 76),
                            new Point(78, 76)
                    },
                    new Point(114, 66),
                    new Point(216, 56),
                    new Point(216, 78)
            );
            case BLENDER -> midProfile(
                    stationType,
                    Theme.GLASS,
                    268,
                    226,
                    rowMajor(30, 50, 3, 22, 24, 6),
                    new Point(114, 66),
                    new Point(216, 56),
                    new Point(216, 78)
            );
            case FERMENTATION_CROCK -> midProfile(
                    stationType,
                    Theme.CROCK,
                    268,
                    226,
                    rowMajor(34, 52, 3, 20, 22, 6),
                    new Point(110, 66),
                    new Point(216, 56),
                    new Point(216, 78)
            );
            case CANNING_STATION -> midProfile(
                    stationType,
                    Theme.PRESERVE,
                    268,
                    226,
                    rowMajor(28, 52, 3, 22, 22, 6),
                    new Point(108, 66),
                    new Point(216, 56),
                    new Point(216, 78)
            );
            case STEAMER -> midProfile(
                    stationType,
                    Theme.STEAM,
                    268,
                    226,
                    rowMajor(34, 48, 3, 22, 26, 6),
                    new Point(114, 66),
                    new Point(216, 56),
                    new Point(216, 78)
            );
            case FREEZE_DRYER -> midProfile(
                    stationType,
                    Theme.COLD,
                    256,
                    224,
                    new Point[] {
                            new Point(30, 54),
                            new Point(52, 54),
                            new Point(74, 54),
                            new Point(41, 78),
                            new Point(63, 78)
                    },
                    new Point(108, 66),
                    new Point(206, 56),
                    new Point(206, 78)
            );
            case DRYING_RACK -> compactProfile(
                    stationType,
                    Theme.RACK,
                    244,
                    220,
                    new Point[] {
                            new Point(30, 56),
                            new Point(52, 56),
                            new Point(74, 56),
                            new Point(41, 80),
                            new Point(63, 80)
                    },
                    new Point(106, 68),
                    new Point(194, 56),
                    new Point(194, 78)
            );
            case SMOKER -> midProfile(
                    stationType,
                    Theme.SMOKE,
                    256,
                    224,
                    new Point[] {
                            new Point(30, 54),
                            new Point(52, 54),
                            new Point(74, 54),
                            new Point(41, 78),
                            new Point(63, 78)
                    },
                    new Point(108, 66),
                    new Point(206, 56),
                    new Point(206, 78)
            );
            case MICROWAVE -> compactProfile(
                    stationType,
                    Theme.MICROWAVE,
                    238,
                    220,
                    grid(34, 56, 2, 2, 22, 22),
                    new Point(96, 68),
                    new Point(190, 56),
                    new Point(190, 78)
            );
            case JUICER -> compactProfile(
                    stationType,
                    Theme.CITRUS,
                    238,
                    220,
                    new Point[] {
                            new Point(30, 50),
                            new Point(52, 50),
                            new Point(40, 74),
                            new Point(62, 74)
                    },
                    new Point(98, 64),
                    new Point(190, 56),
                    new Point(190, 78)
            );
            case SPICE_GRINDER -> compactProfile(
                    stationType,
                    Theme.SPICE,
                    238,
                    220,
                    grid(34, 54, 2, 2, 22, 22),
                    new Point(98, 66),
                    new Point(190, 56),
                    new Point(190, 78)
            );
            case STRAINER -> compactProfile(
                    stationType,
                    Theme.GLASS,
                    238,
                    220,
                    grid(34, 56, 2, 2, 22, 22),
                    new Point(98, 68),
                    new Point(190, 56),
                    new Point(190, 78)
            );
            case COOLING_RACK -> compactProfile(
                    stationType,
                    Theme.RACK,
                    250,
                    220,
                    row(28, 64, 4, 20),
                    new Point(110, 64),
                    new Point(202, 56),
                    new Point(202, 78)
            );
            case RESTING_BOARD -> compactProfile(
                    stationType,
                    Theme.BOARD,
                    250,
                    220,
                    row(28, 64, 4, 20),
                    new Point(110, 64),
                    new Point(202, 56),
                    new Point(202, 78)
            );
        };
    }

    public int inventoryLabelY() {
        return this.playerInventoryStartY - 15;
    }

    public Rect workspaceCard() {
        return boundsAround(this.inputPositions, this.toolPosition, 12, 12, 12, 12);
    }

    public Rect resultCard() {
        return boundsAround(new Point[] { this.outputPosition, this.byproductPosition }, null, 10, 10, 12, 12);
    }

    public Rect statusCard() {
        Rect workspace = this.workspaceCard();
        Rect result = this.resultCard();
        int x = workspace.right() + 8;
        int width = Math.max(64, result.x() - x - 8);
        return new Rect(x, workspace.y(), width, workspace.height());
    }

    public Rect topDeckCard() {
        Rect workspace = this.workspaceCard();
        Rect status = this.statusCard();
        Rect result = this.resultCard();
        int minX = Math.min(workspace.x(), Math.min(status.x(), result.x())) - 8;
        int minY = Math.min(workspace.y(), Math.min(status.y(), result.y())) - 8;
        int maxX = Math.max(workspace.right(), Math.max(status.right(), result.right())) + 8;
        int maxY = Math.max(workspace.bottom(), Math.max(status.bottom(), result.bottom())) + 8;
        return new Rect(minX, minY, maxX - minX, maxY - minY);
    }

    public Rect actionCard() {
        Rect deck = this.topDeckCard();
        return new Rect(deck.x() + 4, deck.bottom() + 8, deck.width() - 8, 24);
    }

    public Rect inventoryCard() {
        return new Rect(this.playerInventoryStartX - 14, this.playerInventoryStartY - 10, 190, 82);
    }

    public Point primaryButtonPosition() {
        Rect action = this.actionCard();
        return new Point(action.right() - 48, action.y() + 3);
    }

    public Point secondaryButtonPosition() {
        Rect action = this.actionCard();
        return new Point(action.right() - 98, action.y() + 3);
    }

    public Point tertiaryButtonPosition() {
        Rect action = this.actionCard();
        return new Point(action.right() - 148, action.y() + 3);
    }

    public Point lowHeatButtonPosition() {
        Rect action = this.actionCard();
        return new Point(action.x() + 8, action.y() + 3);
    }

    public Point mediumHeatButtonPosition() {
        Point low = this.lowHeatButtonPosition();
        return new Point(low.x() + 22, low.y());
    }

    public Point highHeatButtonPosition() {
        Point low = this.lowHeatButtonPosition();
        return new Point(low.x() + 44, low.y());
    }

    public Point lowerControlButtonPosition() {
        Rect action = this.actionCard();
        return new Point(action.x() + 8, action.y() + 3);
    }

    public Point raiseControlButtonPosition() {
        Rect action = this.actionCard();
        return new Point(action.x() + 86, action.y() + 3);
    }

    public Rect controlChipRect() {
        Point lower = this.lowerControlButtonPosition();
        return new Rect(lower.x() + 20, lower.y() + 2, 54, 14);
    }

    public Rect ovenTemperatureFieldRect() {
        Rect action = this.actionCard();
        return new Rect(action.x() + 32, action.y() + 3, 48, 18);
    }

    public Rect actionTextArea(boolean panSimulation, boolean supportsSecondaryButtons) {
        Rect action = this.actionCard();
        int startX = action.x() + 8;
        if (this.stationType == StationType.OVEN) {
            startX = action.x() + 96;
        } else if (this.stationType.supportsHeat()) {
            startX = action.x() + 76;
        } else if (this.stationType.supportsStationControl()) {
            startX = action.x() + 110;
        }
        int endX = supportsSecondaryButtons ? this.tertiaryButtonPosition().x() - 6 : this.primaryButtonPosition().x() - 6;
        if (panSimulation) {
            return new Rect(startX, action.y() + 4, 0, action.height() - 8);
        }
        return new Rect(startX, action.y() + 7, Math.max(0, endX - startX), 10);
    }

    private static StationUiProfile largeProfile(
            StationType stationType,
            Theme theme,
            int width,
            int height,
            Point[] inputs,
            Point tool,
            Point output,
            Point byproduct
    ) {
        int inventoryX = centeredInventoryStart(width);
        return new StationUiProfile(stationType, StationCapacityProfile.forStation(stationType), theme, width, height, inputs, tool, output, byproduct, inventoryX, 148, 206);
    }

    private static StationUiProfile midProfile(
            StationType stationType,
            Theme theme,
            int width,
            int height,
            Point[] inputs,
            Point tool,
            Point output,
            Point byproduct
    ) {
        int inventoryX = centeredInventoryStart(width);
        return new StationUiProfile(stationType, StationCapacityProfile.forStation(stationType), theme, width, height, inputs, tool, output, byproduct, inventoryX, 146, 204);
    }

    private static StationUiProfile compactProfile(
            StationType stationType,
            Theme theme,
            int width,
            int height,
            Point[] inputs,
            Point tool,
            Point output,
            Point byproduct
    ) {
        int inventoryX = centeredInventoryStart(width);
        return new StationUiProfile(stationType, StationCapacityProfile.forStation(stationType), theme, width, height, inputs, tool, output, byproduct, inventoryX, 142, 200);
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

    private static Point[] rowMajor(int startX, int startY, int cols, int gapX, int gapY, int count) {
        Point[] points = new Point[count];
        for (int index = 0; index < count; index++) {
            int row = index / cols;
            int col = index % cols;
            points[index] = new Point(startX + col * gapX, startY + row * gapY);
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

    private static Rect boundsAround(Point[] points, Point extra, int leftPad, int topPad, int rightPad, int bottomPad) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Point point : points) {
            minX = Math.min(minX, point.x());
            minY = Math.min(minY, point.y());
            maxX = Math.max(maxX, point.x() + SLOT_SIZE);
            maxY = Math.max(maxY, point.y() + SLOT_SIZE);
        }
        if (extra != null) {
            minX = Math.min(minX, extra.x());
            minY = Math.min(minY, extra.y());
            maxX = Math.max(maxX, extra.x() + SLOT_SIZE);
            maxY = Math.max(maxY, extra.y() + SLOT_SIZE);
        }
        return new Rect(minX - leftPad, minY - topPad, (maxX - minX) + leftPad + rightPad, (maxY - minY) + topPad + bottomPad);
    }

    public record Point(int x, int y) {
    }

    public record Rect(int x, int y, int width, int height) {
        public int right() {
            return this.x + this.width;
        }

        public int bottom() {
            return this.y + this.height;
        }

        public int centerX() {
            return this.x + this.width / 2;
        }

        public int centerY() {
            return this.y + this.height / 2;
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
