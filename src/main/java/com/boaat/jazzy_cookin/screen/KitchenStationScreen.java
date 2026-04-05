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
        this.imageWidth = 196;
        this.imageHeight = 184;
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
        guiGraphics.fill(left + 12, top + 18, left + 92, top + 74, 0xFF1A1410);
        guiGraphics.fill(left + 120, top + 22, left + 172, top + 66, 0xFF1A1410);

        int maxProgress = this.menu.maxProgress();
        if (maxProgress > 0) {
            int progressWidth = Math.max(0, Math.min(54, Math.round((this.menu.progress() / (float) maxProgress) * 54.0F)));
            guiGraphics.fill(left + 104, top + 74, left + 104 + progressWidth, top + 80, 0xFFD99B3B);
        }

        if (this.menu.stationType() == StationType.OVEN) {
            int preheatWidth = Math.max(0, Math.min(54, Math.round(this.menu.preheatProgress() * 0.54F)));
            guiGraphics.fill(left + 104, top + 86, left + 104 + preheatWidth, top + 92, 0xFFD15A32);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 0xF2E7D5, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xF2E7D5, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.method", this.menu.currentMethod().displayName()), 100, 8, 0xE3CFAE, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.current_heat", Component.translatable("heat.jazzycookin." + this.menu.heatLevel().getSerializedName())), 100, 20, 0xE3CFAE, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.output"), 118, 40, 0xD6C4A7, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.byproduct"), 112, 62, 0xD6C4A7, false);
        if (this.menu.stationType() == StationType.OVEN) {
            guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.preheat", this.menu.preheatProgress()), 100, 82, 0xE3CFAE, false);
        } else {
            guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.progress", this.menu.progress(), this.menu.maxProgress()), 100, 82, 0xE3CFAE, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
