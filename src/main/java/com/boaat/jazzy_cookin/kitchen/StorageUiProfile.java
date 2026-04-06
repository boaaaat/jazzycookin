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
        int width = 308;
        int height = 278;
        int inventoryStartX = (width - 162) / 2;
        int inventoryStartY = 186;
        int hotbarY = 244;
        LayoutRegion storageRegion = new LayoutRegion(20, 44, 268, 68);
        LayoutRegion supportRegion = new LayoutRegion(20, 118, 268, 44);
        LayoutRegion inventoryShelf = new LayoutRegion(18, 172, 272, 96);
        LayoutRegion titleRegion = new LayoutRegion(14, 10, 140, 14);
        LayoutRegion chipRegion = new LayoutRegion(width - 98, 8, 84, 18);
        LayoutRegion inventoryLabel = new LayoutRegion(inventoryStartX, inventoryStartY - 13, 96, 10);
        if (storageType == StorageType.PANTRY) {
            return new StorageUiProfile(
                    storageType,
                    width,
                    height,
                    73,
                    61,
                    inventoryStartX,
                    inventoryStartY,
                    hotbarY,
                    storageRegion,
                    supportRegion,
                    new LayoutRegion(30, 124, 214, 32),
                    new LayoutRegion(250, 124, 28, 32),
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
                73,
                61,
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
        int[] rowCounts = new int[] { 6, 5 };
        int row = index < rowCounts[0] ? 0 : 1;
        int col = row == 0 ? index : index - rowCounts[0];
        int count = rowCounts[row];
        int rowWidth = count * TAB_SIZE + (count - 1) * TAB_GAP_X;
        int startX = this.tabRailRegion.x() + (this.tabRailRegion.width() - rowWidth) / 2;
        int y = this.tabRailRegion.y() + row * (TAB_SIZE + TAB_GAP_Y);
        return new LayoutRegion(startX + col * (TAB_SIZE + TAB_GAP_X), y, TAB_SIZE, TAB_SIZE);
    }

    public LayoutRegion pageUpBounds() {
        if (this.pageRailRegion == null) {
            throw new IllegalStateException("Paging is only available for pantry screens");
        }
        return new LayoutRegion(this.pageRailRegion.x() + 4, this.pageRailRegion.y() + 2, 20, 12);
    }

    public LayoutRegion pageDownBounds() {
        if (this.pageRailRegion == null) {
            throw new IllegalStateException("Paging is only available for pantry screens");
        }
        return new LayoutRegion(this.pageRailRegion.x() + 4, this.pageRailRegion.bottom() - 14, 20, 12);
    }
}
