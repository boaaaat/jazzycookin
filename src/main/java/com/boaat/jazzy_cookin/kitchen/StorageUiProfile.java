package com.boaat.jazzy_cookin.kitchen;

import com.boaat.jazzy_cookin.screen.layout.LayoutRegion;

public record StorageUiProfile(
        StorageType storageType,
        int width,
        int height,
        int storageStartX,
        int storageStartY,
        int playerInventoryStartX,
        int playerInventoryStartY,
        int hotbarY,
        LayoutRegion storageRegion,
        LayoutRegion supportRegion,
        LayoutRegion tabRailRegion,
        LayoutRegion pageRailRegion,
        LayoutRegion inventoryShelfRegion,
        LayoutRegion titleRegion,
        LayoutRegion headerChipRegion,
        LayoutRegion inventoryLabelRegion
) {
    private static final int BASE_WIDTH = 308;
    private static final int BASE_HEIGHT = 278;
    private static final int VANILLA_WIDTH = 176;
    private static final int VANILLA_HEIGHT = 222;
    private static final int MIN_WIDTH = 244;
    private static final int MIN_HEIGHT = 256;
    private static final int MAX_WIDTH = 356;
    private static final int MAX_HEIGHT = 316;
    private static final int SLOT_SIZE = 18;
    public static final int TAB_SIZE = 22;
    public static final int TAB_GAP_X = 6;
    public static final int TAB_GAP_Y = 4;

    public StorageUiProfile {
        for (LayoutRegion region : new LayoutRegion[] {
                storageRegion,
                supportRegion,
                inventoryShelfRegion,
                titleRegion,
                headerChipRegion,
                inventoryLabelRegion
        }) {
            if (region.x() < 0 || region.y() < 0 || region.right() > width || region.bottom() > height) {
                throw new IllegalArgumentException("Storage layout region out of bounds: " + region);
            }
        }
        if (tabRailRegion != null && (tabRailRegion.x() < 0 || tabRailRegion.y() < 0 || tabRailRegion.right() > width || tabRailRegion.bottom() > height)) {
            throw new IllegalArgumentException("Tab rail out of bounds");
        }
        if (pageRailRegion != null && (pageRailRegion.x() < 0 || pageRailRegion.y() < 0 || pageRailRegion.right() > width || pageRailRegion.bottom() > height)) {
            throw new IllegalArgumentException("Page rail out of bounds");
        }
    }

    public static StorageUiProfile forType(StorageType storageType) {
        return buildProfile(storageType, VANILLA_WIDTH, VANILLA_HEIGHT);
    }

    private static StorageUiProfile buildProfile(StorageType storageType, int width, int height) {
        width = VANILLA_WIDTH;
        height = VANILLA_HEIGHT;
        int inventoryStartX = 8;
        int inventoryStartY = 140;
        int hotbarY = 198;
        LayoutRegion storageRegion = new LayoutRegion(7, 17, 162, 38);
        LayoutRegion supportRegion = new LayoutRegion(8, 60, 160, 62);
        LayoutRegion inventoryShelf = new LayoutRegion(0, 126, 176, 96);
        LayoutRegion chipRegion = new LayoutRegion(98, 6, 70, 12);
        LayoutRegion titleRegion = new LayoutRegion(8, 6, 88, 10);
        LayoutRegion inventoryLabel = new LayoutRegion(8, 128, 96, 10);
        int storageStartX = 8;
        int storageStartY = 18;
        if (storageType == StorageType.PANTRY) {
            return new StorageUiProfile(
                    storageType,
                    width,
                    height,
                    storageStartX,
                    storageStartY,
                    inventoryStartX,
                    inventoryStartY,
                    hotbarY,
                    storageRegion,
                    supportRegion,
                    new LayoutRegion(10, 64, 118, 54),
                    new LayoutRegion(136, 66, 28, 50),
                    inventoryShelf,
                    titleRegion,
                    chipRegion,
                    inventoryLabel
            );
        }
        return new StorageUiProfile(
                storageType,
                width,
                height,
                storageStartX,
                storageStartY,
                inventoryStartX,
                inventoryStartY,
                hotbarY,
                storageRegion,
                supportRegion,
                null,
                null,
                inventoryShelf,
                titleRegion,
                chipRegion,
                inventoryLabel
        );
    }

    public LayoutRegion tabBounds(int index) {
        if (this.tabRailRegion == null) {
            throw new IllegalStateException("Tabs are only available for pantry screens");
        }
        int tabSize = Math.max(18, Math.round(TAB_SIZE * this.layoutScale()));
        int gapX = Math.max(4, Math.round(TAB_GAP_X * this.layoutScale()));
        int gapY = Math.max(2, Math.round(TAB_GAP_Y * this.layoutScale()));
        tabSize = Math.min(tabSize, Math.max(18, (this.tabRailRegion.height() - gapY) / 2));
        int[] rowCounts = new int[] { 6, 5 };
        int row = index < rowCounts[0] ? 0 : 1;
        int col = row == 0 ? index : index - rowCounts[0];
        int count = rowCounts[row];
        int rowWidth = count * tabSize + (count - 1) * gapX;
        int startX = this.tabRailRegion.x() + (this.tabRailRegion.width() - rowWidth) / 2;
        int y = this.tabRailRegion.y() + row * (tabSize + gapY);
        return new LayoutRegion(startX + col * (tabSize + gapX), y, tabSize, tabSize);
    }

    public LayoutRegion pageUpBounds() {
        if (this.pageRailRegion == null) {
            throw new IllegalStateException("Paging is only available for pantry screens");
        }
        int insetX = Math.max(3, Math.round(4 * this.layoutScale()));
        int insetY = Math.max(2, Math.round(2 * this.layoutScale()));
        int width = Math.max(18, Math.round(20 * this.layoutScale()));
        int height = Math.max(10, Math.round(12 * this.layoutScale()));
        return new LayoutRegion(this.pageRailRegion.x() + insetX, this.pageRailRegion.y() + insetY, width, height);
    }

    public LayoutRegion pageDownBounds() {
        if (this.pageRailRegion == null) {
            throw new IllegalStateException("Paging is only available for pantry screens");
        }
        int insetX = Math.max(3, Math.round(4 * this.layoutScale()));
        int insetBottom = Math.max(10, Math.round(14 * this.layoutScale()));
        int width = Math.max(18, Math.round(20 * this.layoutScale()));
        int height = Math.max(10, Math.round(12 * this.layoutScale()));
        return new LayoutRegion(this.pageRailRegion.x() + insetX, this.pageRailRegion.bottom() - insetBottom, width, height);
    }

    public StorageUiProfile resolve(int screenWidth, int screenHeight) {
        return buildProfile(this.storageType, VANILLA_WIDTH, VANILLA_HEIGHT);
    }

    private float layoutScale() {
        return this.width / (float) BASE_WIDTH;
    }

    private static int resolveWidth(int screenWidth, int preferredWidth) {
        int available = Math.max(MIN_WIDTH, screenWidth - 24);
        int preferred = Math.max(preferredWidth, screenWidth >= 500 ? preferredWidth + 20 : preferredWidth);
        return clamp(preferred, MIN_WIDTH, Math.min(MAX_WIDTH, available));
    }

    private static int resolveHeight(int screenHeight, int preferredHeight, boolean pantry) {
        int available = Math.max(MIN_HEIGHT, screenHeight - 24);
        int preferred = Math.max(preferredHeight, pantry ? 286 : preferredHeight);
        if (screenHeight >= 360) {
            preferred += 10;
        }
        return clamp(preferred, MIN_HEIGHT, Math.min(MAX_HEIGHT, available));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
