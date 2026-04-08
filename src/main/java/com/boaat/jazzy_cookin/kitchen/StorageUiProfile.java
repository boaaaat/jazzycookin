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
        int width = BASE_WIDTH;
        int height = BASE_HEIGHT;
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
        int tabSize = Math.max(18, Math.round(TAB_SIZE * this.layoutScale()));
        int gapX = Math.max(4, Math.round(TAB_GAP_X * this.layoutScale()));
        int gapY = Math.max(2, Math.round(TAB_GAP_Y * this.layoutScale()));
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
        float scale = resolveScale(screenWidth, screenHeight, this.width, this.height);
        if (Math.abs(scale - 1.0F) < 0.01F) {
            return this;
        }
        return new StorageUiProfile(
                this.storageType,
                Math.round(this.width * scale),
                Math.round(this.height * scale),
                Math.round(this.storageStartX * scale),
                Math.round(this.storageStartY * scale),
                Math.round(this.playerInventoryStartX * scale),
                Math.round(this.playerInventoryStartY * scale),
                Math.round(this.hotbarY * scale),
                scale(this.storageRegion, scale),
                scale(this.supportRegion, scale),
                scale(this.tabRailRegion, scale),
                scale(this.pageRailRegion, scale),
                scale(this.inventoryShelfRegion, scale),
                scale(this.titleRegion, scale),
                scale(this.headerChipRegion, scale),
                scale(this.inventoryLabelRegion, scale)
        );
    }

    private float layoutScale() {
        return this.width / (float) BASE_WIDTH;
    }

    private static LayoutRegion scale(LayoutRegion region, float scale) {
        if (region == null) {
            return null;
        }
        return new LayoutRegion(
                Math.round(region.x() * scale),
                Math.round(region.y() * scale),
                Math.round(region.width() * scale),
                Math.round(region.height() * scale)
        );
    }

    private static float resolveScale(int screenWidth, int screenHeight, int baseWidth, int baseHeight) {
        float widthScale = Math.max(0.84F, (screenWidth - 24.0F) / Math.max(1.0F, baseWidth));
        float heightScale = Math.max(0.84F, (screenHeight - 24.0F) / Math.max(1.0F, baseHeight));
        float aspect = screenHeight <= 0 ? 1.0F : screenWidth / (float) screenHeight;
        float aspectModifier = aspect > 2.0F ? 1.06F : aspect < 1.18F ? 0.95F : 1.0F;
        return Math.max(0.84F, Math.min(1.55F, Math.min(widthScale, heightScale) * aspectModifier));
    }
}
