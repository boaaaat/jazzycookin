package com.boaat.jazzy_cookin.screen;

import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.menu.KitchenStationMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class KitchenStationScreen extends AbstractContainerScreen<KitchenStationMenu> {
    private static final int MACHINE_PANEL_X = 7;
    private static final int MACHINE_PANEL_Y = 17;
    private static final int MACHINE_PANEL_WIDTH = 162;
    private static final int MACHINE_PANEL_HEIGHT = 74;
    private static final int WORK_PANEL_X = 12;
    private static final int WORK_PANEL_Y = 22;
    private static final int WORK_PANEL_WIDTH = 82;
    private static final int WORK_PANEL_HEIGHT = 50;
    private static final int STATUS_PANEL_X = 92;
    private static final int STATUS_PANEL_Y = 22;
    private static final int STATUS_PANEL_WIDTH = 30;
    private static final int STATUS_PANEL_HEIGHT = 34;
    private static final int RESULT_PANEL_X = 128;
    private static final int RESULT_PANEL_Y = 22;
    private static final int RESULT_PANEL_WIDTH = 34;
    private static final int RESULT_PANEL_HEIGHT = 54;
    private static final int INVENTORY_PANEL_X = 7;
    private static final int INVENTORY_PANEL_Y = 99;
    private static final int INVENTORY_PANEL_WIDTH = 162;
    private static final int INVENTORY_PANEL_HEIGHT = 80;
    private static final int SLOT_COUNT = 43;

    private Button startButton;
    private Button lowHeatButton;
    private Button mediumHeatButton;
    private Button highHeatButton;
    private Button lowerControlButton;
    private Button raiseControlButton;

    public KitchenStationScreen(KitchenStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = 92;
    }

    @Override
    protected void init() {
        super.init();
        int controlsY = this.topPos + 69;
        this.startButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.jazzycookin.start"), button -> this.sendButton(0))
                .bounds(this.leftPos + 120, controlsY, 48, 18)
                .build());

        if (this.menu.stationType().supportsHeat()) {
            this.lowHeatButton = this.addRenderableWidget(Button.builder(Component.literal("L"), button -> this.sendButton(1))
                    .bounds(this.leftPos + 18, controlsY, 18, 18)
                    .build());
            this.mediumHeatButton = this.addRenderableWidget(Button.builder(Component.literal("M"), button -> this.sendButton(2))
                    .bounds(this.leftPos + 40, controlsY, 18, 18)
                    .build());
            this.highHeatButton = this.addRenderableWidget(Button.builder(Component.literal("H"), button -> this.sendButton(3))
                    .bounds(this.leftPos + 62, controlsY, 18, 18)
                    .build());
        }

        if (this.menu.stationType().supportsStationControl()) {
            this.lowerControlButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.sendButton(4))
                    .bounds(this.leftPos + 18, controlsY, 18, 18)
                    .build());
            this.raiseControlButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.sendButton(5))
                    .bounds(this.leftPos + 62, controlsY, 18, 18)
                    .build());
        }

        this.updateButtonStates();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.updateButtonStates();
    }

    private void updateButtonStates() {
        if (this.startButton != null) {
            this.startButton.active = this.menu.environmentStatus() != 0;
        }

        if (this.lowHeatButton != null) {
            this.lowHeatButton.active = this.menu.heatLevel() != HeatLevel.LOW;
        }
        if (this.mediumHeatButton != null) {
            this.mediumHeatButton.active = this.menu.heatLevel() != HeatLevel.MEDIUM;
        }
        if (this.highHeatButton != null) {
            this.highHeatButton.active = this.menu.heatLevel() != HeatLevel.HIGH;
        }
        if (this.lowerControlButton != null) {
            this.lowerControlButton.active = this.menu.controlSetting() > 0;
        }
        if (this.raiseControlButton != null) {
            this.raiseControlButton.active = this.menu.controlSetting() < 2;
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

        JazzyGuiRenderer.drawWindow(guiGraphics, left, top, this.imageWidth, this.imageHeight);
        JazzyGuiRenderer.drawPanel(guiGraphics, left + MACHINE_PANEL_X, top + MACHINE_PANEL_Y, MACHINE_PANEL_WIDTH, MACHINE_PANEL_HEIGHT);
        JazzyGuiRenderer.drawPanel(guiGraphics, left + WORK_PANEL_X, top + WORK_PANEL_Y, WORK_PANEL_WIDTH, WORK_PANEL_HEIGHT);
        JazzyGuiRenderer.drawPanel(guiGraphics, left + STATUS_PANEL_X, top + STATUS_PANEL_Y, STATUS_PANEL_WIDTH, STATUS_PANEL_HEIGHT);
        JazzyGuiRenderer.drawPanel(guiGraphics, left + RESULT_PANEL_X, top + RESULT_PANEL_Y, RESULT_PANEL_WIDTH, RESULT_PANEL_HEIGHT);
        JazzyGuiRenderer.drawPanel(guiGraphics, left + INVENTORY_PANEL_X, top + INVENTORY_PANEL_Y, INVENTORY_PANEL_WIDTH, INVENTORY_PANEL_HEIGHT);

        for (int slotIndex = 0; slotIndex < SLOT_COUNT; slotIndex++) {
            Slot slot = this.menu.getSlot(slotIndex);
            if (slotIndex == 4 && !this.menu.stationType().usesTools()) {
                JazzyGuiRenderer.drawDisabledSlot(guiGraphics, left + slot.x, top + slot.y);
            } else {
                JazzyGuiRenderer.drawSlot(guiGraphics, left + slot.x, top + slot.y);
            }
        }

        float progress = this.menu.maxProgress() > 0 ? this.menu.progress() / (float) this.menu.maxProgress() : 0.0F;
        JazzyGuiRenderer.drawProgressBar(guiGraphics, left + 96, top + 36, 22, progress, 0xFF6AA84F);

        if (this.menu.stationType() == StationType.OVEN) {
            JazzyGuiRenderer.drawProgressBar(guiGraphics, left + 96, top + 46, 22, this.menu.preheatProgress() / 100.0F, 0xFFE69138);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, JazzyGuiRenderer.TEXT, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, JazzyGuiRenderer.TEXT, false);

        this.drawCenteredLabel(guiGraphics, this.menu.currentMethod().displayName(), 107, 22, JazzyGuiRenderer.TEXT);
        this.drawCenteredLabel(guiGraphics, Component.translatable("screen.jazzycookin.output"), this.menu.getSlot(5).x + 8, this.menu.getSlot(5).y - 10, JazzyGuiRenderer.TEXT_MUTED);
        this.drawCenteredLabel(
                guiGraphics,
                Component.translatable("screen.jazzycookin.byproduct"),
                this.menu.getSlot(6).x + 8,
                this.menu.getSlot(6).y - 10,
                JazzyGuiRenderer.TEXT_MUTED
        );

        if (this.menu.stationType().supportsHeat()) {
            this.drawCenteredLabel(
                    guiGraphics,
                    Component.translatable("heat.jazzycookin." + this.menu.heatLevel().getSerializedName()),
                    49,
                    57,
                    JazzyGuiRenderer.TEXT_MUTED
            );
        } else if (this.menu.stationType().supportsStationControl()) {
            this.drawCenteredLabel(guiGraphics, this.menu.stationType().controlLabel(this.menu.controlSetting()), 49, 57, JazzyGuiRenderer.TEXT_MUTED);
        }

        if (this.menu.stationType() == StationType.OVEN) {
            this.drawCenteredLabel(
                    guiGraphics,
                    Component.literal("Preheat " + this.menu.preheatProgress() + "%"),
                    107,
                    57,
                    JazzyGuiRenderer.TEXT_MUTED
            );
        } else if (this.menu.environmentStatus() != 2) {
            this.drawCenteredLabel(
                    guiGraphics,
                    Component.translatable(this.menu.environmentStatus() == 1
                            ? "screen.jazzycookin.environment_ready"
                            : "screen.jazzycookin.environment_blocked"),
                    107,
                    57,
                    this.menu.environmentStatus() == 1 ? JazzyGuiRenderer.READY_TEXT : JazzyGuiRenderer.BLOCKED_TEXT
            );
        }

        this.drawCenteredLabel(
                guiGraphics,
                Component.literal(this.menu.progress() + " / " + this.menu.maxProgress()),
                107,
                69,
                JazzyGuiRenderer.TEXT_MUTED
        );
    }

    private void drawCenteredLabel(GuiGraphics guiGraphics, Component label, int centerX, int y, int color) {
        guiGraphics.drawString(this.font, label, centerX - this.font.width(label) / 2, y, color, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
