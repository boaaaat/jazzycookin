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
    private static final int STORAGE_PANEL_X = 7;
    private static final int STORAGE_PANEL_Y = 17;
    private static final int STORAGE_PANEL_WIDTH = 162;
    private static final int STORAGE_PANEL_HEIGHT = 60;
    private static final int INVENTORY_PANEL_X = 7;
    private static final int INVENTORY_PANEL_Y = 81;
    private static final int INVENTORY_PANEL_WIDTH = 162;
    private static final int INVENTORY_PANEL_HEIGHT = 80;
    private static final int SLOT_COUNT = 45;

    private final List<PantryShortcut> pantryShortcuts = new ArrayList<>();

    public KitchenStorageScreen(KitchenStorageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = 74;
    }

    @Override
    protected void init() {
        super.init();
        this.pantryShortcuts.clear();
        if (this.menu.isPantry()) {
            this.addShortcut(0, 8, 58, JazzyItems.FLOUR.get().getDefaultInstance());
            this.addShortcut(1, 28, 58, JazzyItems.CANE_SUGAR.get().getDefaultInstance());
            this.addShortcut(2, 48, 58, JazzyItems.BUTTER.get().getDefaultInstance());
            this.addShortcut(3, 68, 58, JazzyItems.BAKING_SPICE.get().getDefaultInstance());
            this.addShortcut(4, 90, 58, JazzyItems.FRYING_OIL.get().getDefaultInstance());
            this.addShortcut(5, 110, 58, JazzyItems.CERAMIC_PLATE.get().getDefaultInstance());
            this.addShortcut(6, 130, 58, JazzyItems.CANNING_JAR.get().getDefaultInstance());
            this.addShortcut(7, 150, 58, JazzyItems.SALT.get().getDefaultInstance());
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
        JazzyGuiRenderer.drawPanel(guiGraphics, left + STORAGE_PANEL_X, top + STORAGE_PANEL_Y, STORAGE_PANEL_WIDTH, STORAGE_PANEL_HEIGHT);
        JazzyGuiRenderer.drawPanel(guiGraphics, left + INVENTORY_PANEL_X, top + INVENTORY_PANEL_Y, INVENTORY_PANEL_WIDTH, INVENTORY_PANEL_HEIGHT);

        for (int slotIndex = 0; slotIndex < SLOT_COUNT; slotIndex++) {
            JazzyGuiRenderer.drawSlot(guiGraphics, left + this.menu.getSlot(slotIndex).x, top + this.menu.getSlot(slotIndex).y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, JazzyGuiRenderer.TEXT, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, JazzyGuiRenderer.TEXT, false);
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
