package com.boaat.jazzy_cookin.screen;

import com.boaat.jazzy_cookin.menu.KitchenStorageMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class KitchenStorageScreen extends AbstractContainerScreen<KitchenStorageMenu> {
    public KitchenStorageScreen(KitchenStorageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 176;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        if (this.menu.isPantry()) {
            this.addRenderableWidget(Button.builder(Component.translatable("item.jazzycookin.flour"), button -> this.sendButton(0))
                    .bounds(this.leftPos + 8, this.topPos + 4, 40, 18)
                    .build());
            this.addRenderableWidget(Button.builder(Component.translatable("item.jazzycookin.cane_sugar"), button -> this.sendButton(1))
                    .bounds(this.leftPos + 50, this.topPos + 4, 54, 18)
                    .build());
            this.addRenderableWidget(Button.builder(Component.translatable("item.jazzycookin.butter"), button -> this.sendButton(2))
                    .bounds(this.leftPos + 106, this.topPos + 4, 28, 18)
                    .build());
            this.addRenderableWidget(Button.builder(Component.translatable("item.jazzycookin.baking_spice"), button -> this.sendButton(3))
                    .bounds(this.leftPos + 136, this.topPos + 4, 32, 18)
                    .build());
            this.addRenderableWidget(Button.builder(Component.translatable("item.jazzycookin.frying_oil"), button -> this.sendButton(4))
                    .bounds(this.leftPos + 8, this.topPos + 24, 40, 18)
                    .build());
            this.addRenderableWidget(Button.builder(Component.translatable("item.jazzycookin.ceramic_plate"), button -> this.sendButton(5))
                    .bounds(this.leftPos + 50, this.topPos + 24, 54, 18)
                    .build());
            this.addRenderableWidget(Button.builder(Component.translatable("item.jazzycookin.canning_jar"), button -> this.sendButton(6))
                    .bounds(this.leftPos + 106, this.topPos + 24, 28, 18)
                    .build());
            this.addRenderableWidget(Button.builder(Component.translatable("item.jazzycookin.pie_tin"), button -> this.sendButton(7))
                    .bounds(this.leftPos + 136, this.topPos + 24, 32, 18)
                    .build());
        }
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
        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFF1B1814);
        guiGraphics.fill(left + 4, top + 4, left + this.imageWidth - 4, top + this.imageHeight - 4, 0xFF332D24);
        guiGraphics.fill(left + 6, top + 22, left + this.imageWidth - 6, top + 64, 0xFF17130F);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 0xF2E7D5, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xF2E7D5, false);
        guiGraphics.drawString(
                this.font,
                Component.translatable(this.menu.isPantry() ? "screen.jazzycookin.pantry_hint" : "screen.jazzycookin.cellar_hint"),
                8,
                18,
                0xD6C4A7,
                false
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
