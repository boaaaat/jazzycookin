package com.boaat.jazzy_cookin.screen;

import java.util.ArrayList;
import java.util.List;

import com.boaat.jazzy_cookin.menu.KitchenStorageMenu;
import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class KitchenStorageScreen extends AbstractContainerScreen<KitchenStorageMenu> {
    private static final int STORAGE_CARD_X = 14;
    private static final int STORAGE_CARD_Y = 34;
    private static final int STORAGE_CARD_WIDTH = 202;
    private static final int STORAGE_CARD_HEIGHT = 62;
    private static final int SHORTCUT_CARD_X = 14;
    private static final int SHORTCUT_CARD_Y = 104;
    private static final int SHORTCUT_CARD_WIDTH = 202;
    private static final int SHORTCUT_CARD_HEIGHT = 30;
    private static final int INVENTORY_CARD_X = 14;
    private static final int INVENTORY_CARD_Y = 138;
    private static final int INVENTORY_CARD_WIDTH = 202;
    private static final int INVENTORY_CARD_HEIGHT = 78;
    private static final int SLOT_COUNT = 45;

    private final List<PantryShortcut> pantryShortcuts = new ArrayList<>();

    public KitchenStorageScreen(KitchenStorageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 230;
        this.imageHeight = 222;
        this.inventoryLabelY = 127;
    }

    @Override
    protected void init() {
        super.init();
        this.pantryShortcuts.clear();
        if (this.menu.isPantry()) {
            this.addShortcut(0, 34, 110, JazzyItems.FLOUR.get().getDefaultInstance());
            this.addShortcut(1, 54, 110, JazzyItems.CANE_SUGAR.get().getDefaultInstance());
            this.addShortcut(2, 74, 110, JazzyItems.BUTTER.get().getDefaultInstance());
            this.addShortcut(3, 94, 110, JazzyItems.BAKING_SPICE.get().getDefaultInstance());
            this.addShortcut(4, 118, 110, JazzyItems.FRYING_OIL.get().getDefaultInstance());
            this.addShortcut(5, 138, 110, JazzyItems.CERAMIC_PLATE.get().getDefaultInstance());
            this.addShortcut(6, 158, 110, JazzyItems.CANNING_JAR.get().getDefaultInstance());
            this.addShortcut(7, 178, 110, JazzyItems.SALT.get().getDefaultInstance());
        }
    }

    private void addShortcut(int buttonId, int x, int y, ItemStack icon) {
        Button button = this.addRenderableWidget(Button.builder(Component.empty(), pressed -> this.sendButton(buttonId))
                .bounds(this.leftPos + x, this.topPos + y, 18, 18)
                .build());
        this.pantryShortcuts.add(new PantryShortcut(button, icon));
    }

    private void sendButton(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        JazzyGuiRenderer.drawWindow(guiGraphics, left, top, this.imageWidth, this.imageHeight);
        JazzyGuiRenderer.drawCard(guiGraphics, left + STORAGE_CARD_X, top + STORAGE_CARD_Y, STORAGE_CARD_WIDTH, STORAGE_CARD_HEIGHT);
        JazzyGuiRenderer.drawCard(guiGraphics, left + SHORTCUT_CARD_X, top + SHORTCUT_CARD_Y, SHORTCUT_CARD_WIDTH, SHORTCUT_CARD_HEIGHT);
        JazzyGuiRenderer.drawCard(guiGraphics, left + INVENTORY_CARD_X, top + INVENTORY_CARD_Y, INVENTORY_CARD_WIDTH, INVENTORY_CARD_HEIGHT);

        for (int slotIndex = 0; slotIndex < SLOT_COUNT; slotIndex++) {
            JazzyGuiRenderer.drawSlot(guiGraphics, left + this.menu.getSlot(slotIndex).x, top + this.menu.getSlot(slotIndex).y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component shelfLabel = Component.translatable("screen.jazzycookin.shelves_short");

        guiGraphics.drawString(this.font, this.title, 14, 10, JazzyGuiRenderer.TITLE_TEXT, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.storage_short"), STORAGE_CARD_X, 35, JazzyGuiRenderer.TEXT_MUTED, false);
        guiGraphics.drawString(this.font, shelfLabel, STORAGE_CARD_X + STORAGE_CARD_WIDTH - this.font.width(shelfLabel), 35, JazzyGuiRenderer.TEXT_MUTED, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 20, this.inventoryLabelY, JazzyGuiRenderer.TEXT, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.hotbar_short"), 20, 182, JazzyGuiRenderer.TEXT_SOFT, false);

        if (this.menu.isPantry()) {
            this.drawCenteredLabel(
                    guiGraphics,
                    Component.translatable("screen.jazzycookin.quick_supply"),
                    this.imageWidth / 2,
                    97,
                    JazzyGuiRenderer.TEXT_MUTED
            );
        } else {
            this.drawCenteredLabel(
                    guiGraphics,
                    Component.translatable("screen.jazzycookin.cellar_hint"),
                    this.imageWidth / 2,
                    113,
                    JazzyGuiRenderer.TEXT_MUTED
            );
        }
    }

    private void drawCenteredLabel(GuiGraphics guiGraphics, Component label, int centerX, int y, int color) {
        guiGraphics.drawString(this.font, label, centerX - this.font.width(label) / 2, y, color, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        PantryShortcut hoveredShortcut = null;
        for (PantryShortcut shortcut : this.pantryShortcuts) {
            guiGraphics.renderItem(shortcut.icon(), shortcut.button().getX() + 1, shortcut.button().getY() + 1);
            if (shortcut.button().isMouseOver(mouseX, mouseY)) {
                hoveredShortcut = shortcut;
            }
        }

        if (hoveredShortcut != null) {
            guiGraphics.renderTooltip(this.font, hoveredShortcut.icon(), mouseX, mouseY);
        } else {
            this.renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    private record PantryShortcut(Button button, ItemStack icon) {
    }
}
