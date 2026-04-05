package com.boaat.jazzy_cookin.screen;

import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.menu.KitchenStationMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

public class KitchenStationScreen extends AbstractContainerScreen<KitchenStationMenu> {
    private static final int MAIN_CARD_X = 14;
    private static final int MAIN_CARD_Y = 34;
    private static final int MAIN_CARD_WIDTH = 202;
    private static final int MAIN_CARD_HEIGHT = 84;
    private static final int WORK_CARD_X = 20;
    private static final int WORK_CARD_Y = 44;
    private static final int WORK_CARD_WIDTH = 98;
    private static final int WORK_CARD_HEIGHT = 48;
    private static final int STATUS_CARD_X = 128;
    private static final int STATUS_CARD_Y = 44;
    private static final int STATUS_CARD_WIDTH = 44;
    private static final int STATUS_CARD_HEIGHT = 48;
    private static final int RESULT_CARD_X = 180;
    private static final int RESULT_CARD_Y = 44;
    private static final int RESULT_CARD_WIDTH = 30;
    private static final int RESULT_CARD_HEIGHT = 52;
    private static final int ACTION_CARD_X = 20;
    private static final int ACTION_CARD_Y = 93;
    private static final int ACTION_CARD_WIDTH = 190;
    private static final int ACTION_CARD_HEIGHT = 20;
    private static final int INVENTORY_CARD_X = 14;
    private static final int INVENTORY_CARD_Y = 124;
    private static final int INVENTORY_CARD_WIDTH = 202;
    private static final int INVENTORY_CARD_HEIGHT = 84;

    private Button startButton;
    private Button lowHeatButton;
    private Button mediumHeatButton;
    private Button highHeatButton;
    private Button lowerControlButton;
    private Button raiseControlButton;
    private EditBox ovenTemperatureBox;
    private int pendingOvenTemperature = -1;

    public KitchenStationScreen(KitchenStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 230;
        this.imageHeight = 216;
        this.inventoryLabelY = 113;
    }

    @Override
    protected void init() {
        super.init();

        this.startButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.jazzycookin.start"), button -> this.sendButton(0))
                .bounds(this.leftPos + 154, this.topPos + 94, 56, 20)
                .build());

        if (this.menu.stationType() == StationType.OVEN) {
            this.ovenTemperatureBox = new EditBox(this.font, this.leftPos + 52, this.topPos + 94, 44, 18, Component.translatable("screen.jazzycookin.temperature_short"));
            this.ovenTemperatureBox.setMaxLength(3);
            this.ovenTemperatureBox.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
            this.ovenTemperatureBox.setTextColor(JazzyGuiRenderer.TEXT);
            this.ovenTemperatureBox.setTextColorUneditable(JazzyGuiRenderer.TEXT);
            this.ovenTemperatureBox.setValue(Integer.toString(this.menu.ovenTemperature()));
            this.addRenderableWidget(this.ovenTemperatureBox);
        } else if (this.menu.stationType().supportsHeat()) {
            this.lowHeatButton = this.addRenderableWidget(Button.builder(Component.literal("L"), button -> this.sendButton(1))
                    .bounds(this.leftPos + 22, this.topPos + 94, 18, 18)
                    .build());
            this.mediumHeatButton = this.addRenderableWidget(Button.builder(Component.literal("M"), button -> this.sendButton(2))
                    .bounds(this.leftPos + 44, this.topPos + 94, 18, 18)
                    .build());
            this.highHeatButton = this.addRenderableWidget(Button.builder(Component.literal("H"), button -> this.sendButton(3))
                    .bounds(this.leftPos + 66, this.topPos + 94, 18, 18)
                    .build());
        }

        if (this.menu.stationType().supportsStationControl()) {
            this.lowerControlButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.sendButton(4))
                    .bounds(this.leftPos + 22, this.topPos + 94, 18, 18)
                    .build());
            this.raiseControlButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.sendButton(5))
                    .bounds(this.leftPos + 98, this.topPos + 94, 18, 18)
                    .build());
        }

        this.updateButtonStates();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.updateButtonStates();
        this.syncOvenTemperatureField();
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

    private void syncOvenTemperatureField() {
        if (this.ovenTemperatureBox == null || this.ovenTemperatureBox.isFocused()) {
            return;
        }

        int syncedTemperature = this.menu.ovenTemperature();
        if (this.pendingOvenTemperature == syncedTemperature) {
            this.pendingOvenTemperature = -1;
        }

        int displayTemperature = this.pendingOvenTemperature > 0 ? this.pendingOvenTemperature : syncedTemperature;
        String displayText = Integer.toString(displayTemperature);
        if (!this.ovenTemperatureBox.getValue().equals(displayText)) {
            this.ovenTemperatureBox.setValue(displayText);
        }
    }

    private void commitOvenTemperature() {
        if (this.ovenTemperatureBox == null) {
            return;
        }

        String value = this.ovenTemperatureBox.getValue();
        if (value.isBlank()) {
            this.syncOvenTemperatureField();
            return;
        }

        int parsedTemperature;
        try {
            parsedTemperature = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            this.syncOvenTemperatureField();
            return;
        }

        this.applyOvenTemperature(parsedTemperature);
    }

    private void applyOvenTemperature(int temperature) {
        int normalizedTemperature = HeatLevel.normalizeOvenTemperature(temperature);
        this.pendingOvenTemperature = normalizedTemperature;
        if (this.ovenTemperatureBox != null) {
            this.ovenTemperatureBox.setValue(Integer.toString(normalizedTemperature));
        }
        this.sendButton(1000 + normalizedTemperature);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        JazzyGuiRenderer.drawWindow(guiGraphics, left, top, this.imageWidth, this.imageHeight);
        JazzyGuiRenderer.drawCard(guiGraphics, left + MAIN_CARD_X, top + MAIN_CARD_Y, MAIN_CARD_WIDTH, MAIN_CARD_HEIGHT);
        JazzyGuiRenderer.drawCard(guiGraphics, left + WORK_CARD_X, top + WORK_CARD_Y, WORK_CARD_WIDTH, WORK_CARD_HEIGHT);
        JazzyGuiRenderer.drawCard(guiGraphics, left + STATUS_CARD_X, top + STATUS_CARD_Y, STATUS_CARD_WIDTH, STATUS_CARD_HEIGHT);
        JazzyGuiRenderer.drawCard(guiGraphics, left + RESULT_CARD_X, top + RESULT_CARD_Y, RESULT_CARD_WIDTH, RESULT_CARD_HEIGHT);
        JazzyGuiRenderer.drawCard(guiGraphics, left + ACTION_CARD_X, top + ACTION_CARD_Y, ACTION_CARD_WIDTH, ACTION_CARD_HEIGHT);
        JazzyGuiRenderer.drawCard(guiGraphics, left + INVENTORY_CARD_X, top + INVENTORY_CARD_Y, INVENTORY_CARD_WIDTH, INVENTORY_CARD_HEIGHT);

        Component methodLabel = this.menu.currentMethod().displayName();
        int methodChipWidth = Math.max(46, this.font.width(methodLabel) + 16);
        JazzyGuiRenderer.drawChip(
                guiGraphics,
                left + this.imageWidth - methodChipWidth - 14,
                top + 8,
                methodChipWidth,
                14,
                this.menu.currentMethod().isCookMethod()
        );

        if (!this.controlDisplayLabel().getString().isEmpty()) {
            int controlChipWidth = this.menu.stationType().supportsStationControl() ? 52 : 50;
            int controlChipX = this.menu.stationType().supportsStationControl() ? 44 : 90;
            JazzyGuiRenderer.drawChip(
                    guiGraphics,
                    left + controlChipX,
                    top + 96,
                    controlChipWidth,
                    14,
                    this.menu.stationType().supportsHeat()
            );
        }

        for (int slotIndex = 0; slotIndex < this.menu.slots.size(); slotIndex++) {
            Slot slot = this.menu.getSlot(slotIndex);
            if (slotIndex == 4 && !this.menu.stationType().usesTools()) {
                JazzyGuiRenderer.drawDisabledSlot(guiGraphics, left + slot.x, top + slot.y);
            } else {
                JazzyGuiRenderer.drawSlot(guiGraphics, left + slot.x, top + slot.y);
            }
        }

        float progress = this.menu.maxProgress() > 0 ? this.menu.progress() / (float) this.menu.maxProgress() : 0.0F;
        JazzyGuiRenderer.drawProgressBar(guiGraphics, left + 136, top + 60, 28, progress, JazzyGuiRenderer.ACCENT);
        if (this.menu.stationType() == StationType.OVEN) {
            JazzyGuiRenderer.drawProgressBar(guiGraphics, left + 136, top + 76, 28, this.menu.preheatProgress() / 100.0F, JazzyGuiRenderer.ACCENT_WARM);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component methodLabel = this.menu.currentMethod().displayName();
        int methodChipWidth = Math.max(46, this.font.width(methodLabel) + 16);
        int methodChipX = this.imageWidth - methodChipWidth - 14;
        Component resultByproductLabel = Component.translatable("screen.jazzycookin.byproduct_short");

        guiGraphics.drawString(this.font, this.title, 14, 10, JazzyGuiRenderer.TITLE_TEXT, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 20, this.inventoryLabelY, JazzyGuiRenderer.TEXT, false);
        this.drawCenteredLabel(guiGraphics, methodLabel, methodChipX + methodChipWidth / 2, 11, JazzyGuiRenderer.TEXT, false);

        guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.inputs"), WORK_CARD_X, 35, JazzyGuiRenderer.TEXT_MUTED, false);
        if (this.menu.stationType().usesTools()) {
            Component toolLabel = Component.translatable("screen.jazzycookin.tool_short");
            guiGraphics.drawString(this.font, toolLabel, WORK_CARD_X + WORK_CARD_WIDTH - this.font.width(toolLabel), 35, JazzyGuiRenderer.TEXT_MUTED, false);
        }
        guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.status_short"), STATUS_CARD_X, 35, JazzyGuiRenderer.TEXT_MUTED, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.result_short"), RESULT_CARD_X, 35, JazzyGuiRenderer.TEXT_MUTED, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.progress_short"), STATUS_CARD_X + 4, 54, JazzyGuiRenderer.TEXT_SOFT, false);
        if (this.menu.stationType() == StationType.OVEN) {
            guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.preheat_short"), STATUS_CARD_X + 4, 70, JazzyGuiRenderer.TEXT_SOFT, false);
        }
        this.drawCenteredLabel(guiGraphics, Component.translatable("screen.jazzycookin.output"), RESULT_CARD_X + RESULT_CARD_WIDTH / 2, 46, JazzyGuiRenderer.TEXT_SOFT, false);
        this.drawCenteredLabel(guiGraphics, resultByproductLabel, RESULT_CARD_X + RESULT_CARD_WIDTH / 2, 68, JazzyGuiRenderer.TEXT_SOFT, false);

        if (this.menu.stationType() == StationType.OVEN) {
            guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.temperature_short"), 24, 99, JazzyGuiRenderer.TEXT_MUTED, false);
            guiGraphics.drawString(this.font, Component.literal("F"), 100, 99, JazzyGuiRenderer.TEXT_MUTED, false);
        }

        Component controlLabel = this.controlDisplayLabel();
        if (!controlLabel.getString().isEmpty()) {
            int controlChipWidth = this.menu.stationType().supportsStationControl() ? 52 : 50;
            int controlChipX = this.menu.stationType().supportsStationControl() ? 44 : 90;
            this.drawCenteredLabel(guiGraphics, controlLabel, controlChipX + controlChipWidth / 2, 99, JazzyGuiRenderer.TEXT_MUTED, false);
        }

        this.drawCenteredLabel(guiGraphics, this.primaryStatusText(), STATUS_CARD_X + STATUS_CARD_WIDTH / 2, 48, JazzyGuiRenderer.TEXT, false);
        this.drawCenteredLabel(guiGraphics, this.secondaryStatusText(), STATUS_CARD_X + STATUS_CARD_WIDTH / 2, 85, this.secondaryStatusColor(), false);
    }

    private Component controlDisplayLabel() {
        if (this.menu.stationType() == StationType.OVEN) {
            return Component.empty();
        }
        if (this.menu.stationType().supportsHeat()) {
            return Component.translatable("heat.jazzycookin." + this.menu.heatLevel().getSerializedName());
        }
        if (this.menu.stationType().supportsStationControl()) {
            return this.menu.stationType().controlLabel(this.menu.controlSetting());
        }
        return Component.empty();
    }

    private Component primaryStatusText() {
        if (this.menu.maxProgress() > 0) {
            int percent = Math.round((this.menu.progress() / (float) this.menu.maxProgress()) * 100.0F);
            return Component.literal(percent + "%");
        }
        if (this.menu.environmentStatus() == 0) {
            return Component.translatable("screen.jazzycookin.blocked_short");
        }
        if (this.menu.environmentStatus() == 1) {
            return Component.translatable("screen.jazzycookin.ready_short");
        }
        return Component.translatable("screen.jazzycookin.idle_short");
    }

    private Component secondaryStatusText() {
        if (this.menu.stationType() == StationType.OVEN) {
            return this.menu.preheatProgress() >= 100
                    ? Component.translatable("screen.jazzycookin.hot_short")
                    : Component.translatable("screen.jazzycookin.warm_short");
        }
        if (this.menu.maxProgress() > 0) {
            return Component.translatable("screen.jazzycookin.working");
        }
        return Component.empty();
    }

    private int secondaryStatusColor() {
        if (this.menu.stationType() == StationType.OVEN) {
            return this.menu.preheatProgress() >= 100 ? JazzyGuiRenderer.READY_TEXT : JazzyGuiRenderer.ACCENT_WARM;
        }
        if (this.menu.maxProgress() > 0) {
            return JazzyGuiRenderer.TEXT_MUTED;
        }
        if (this.menu.environmentStatus() == 0) {
            return JazzyGuiRenderer.BLOCKED_TEXT;
        }
        if (this.menu.environmentStatus() == 1) {
            return JazzyGuiRenderer.READY_TEXT;
        }
        return JazzyGuiRenderer.TEXT_SOFT;
    }

    private void drawCenteredLabel(GuiGraphics guiGraphics, Component label, int centerX, int y, int color, boolean shadow) {
        if (label.getString().isEmpty()) {
            return;
        }
        guiGraphics.drawString(this.font, label, centerX - this.font.width(label) / 2, y, color, shadow);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean wasFocused = this.ovenTemperatureBox != null && this.ovenTemperatureBox.isFocused();
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (wasFocused && this.ovenTemperatureBox != null && !this.ovenTemperatureBox.isFocused()) {
            this.commitOvenTemperature();
        }
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.ovenTemperatureBox != null
                && scrollY != 0.0D
                && (this.ovenTemperatureBox.isMouseOver(mouseX, mouseY) || this.ovenTemperatureBox.isFocused())) {
            int baseTemperature = this.pendingOvenTemperature > 0 ? this.pendingOvenTemperature : this.menu.ovenTemperature();
            int nextTemperature = baseTemperature + (scrollY > 0.0D ? HeatLevel.OVEN_TEMPERATURE_STEP : -HeatLevel.OVEN_TEMPERATURE_STEP);
            this.applyOvenTemperature(nextTemperature);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.ovenTemperatureBox != null && this.ovenTemperatureBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                this.commitOvenTemperature();
                this.ovenTemperatureBox.setFocused(false);
                return true;
            }
            if (this.ovenTemperatureBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.ovenTemperatureBox != null && this.ovenTemperatureBox.isFocused() && this.ovenTemperatureBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void removed() {
        if (this.ovenTemperatureBox != null && this.ovenTemperatureBox.isFocused()) {
            this.commitOvenTemperature();
        }
        super.removed();
    }
}
