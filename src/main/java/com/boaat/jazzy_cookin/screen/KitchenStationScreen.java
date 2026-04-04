package com.boaat.jazzy_cookin.screen;

import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.menu.KitchenStationMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class KitchenStationScreen extends AbstractContainerScreen<KitchenStationMenu> {
    public KitchenStationScreen(KitchenStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 176;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(Component.translatable("screen.jazzycookin.start"), button -> this.sendButton(0))
                .bounds(this.leftPos + 98, this.topPos + 20, 60, 20)
                .build());

        if (this.menu.stationType().supportsHeat()) {
            this.addRenderableWidget(Button.builder(Component.translatable("heat.jazzycookin.low"), button -> this.sendButton(1))
                    .bounds(this.leftPos + 8, this.topPos + 70, 44, 20)
                    .build());
            this.addRenderableWidget(Button.builder(Component.translatable("heat.jazzycookin.medium"), button -> this.sendButton(2))
                    .bounds(this.leftPos + 56, this.topPos + 70, 56, 20)
                    .build());
            this.addRenderableWidget(Button.builder(Component.translatable("heat.jazzycookin.high"), button -> this.sendButton(3))
                    .bounds(this.leftPos + 116, this.topPos + 70, 44, 20)
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

        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFF201A14);
        guiGraphics.fill(left + 4, top + 4, left + this.imageWidth - 4, top + this.imageHeight - 4, 0xFF3A2F26);
        guiGraphics.fill(left + 18, top + 18, left + 86, top + 74, 0xFF1A1410);
        guiGraphics.fill(left + 114, top + 30, left + 146, top + 62, 0xFF1A1410);

        int maxProgress = this.menu.maxProgress();
        if (maxProgress > 0) {
            int progressWidth = Math.max(0, Math.min(54, Math.round((this.menu.progress() / (float) maxProgress) * 54.0F)));
            guiGraphics.fill(left + 100, top + 46, left + 100 + progressWidth, top + 52, 0xFFD99B3B);
        }

        if (this.menu.stationType() == StationType.OVEN) {
            int preheatWidth = Math.max(0, Math.min(54, Math.round(this.menu.preheatProgress() * 0.54F)));
            guiGraphics.fill(left + 100, top + 58, left + 100 + preheatWidth, top + 64, 0xFFD15A32);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 0xF2E7D5, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xF2E7D5, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.current_heat", Component.translatable("heat.jazzycookin." + this.menu.heatLevel().getSerializedName())), 96, 24, 0xE3CFAE, false);
        if (this.menu.stationType() == StationType.OVEN) {
            guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.preheat", this.menu.preheatProgress()), 96, 58, 0xE3CFAE, false);
        } else {
            guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.progress", this.menu.progress(), this.menu.maxProgress()), 96, 58, 0xE3CFAE, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
