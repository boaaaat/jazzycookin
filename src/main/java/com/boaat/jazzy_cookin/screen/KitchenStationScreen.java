package com.boaat.jazzy_cookin.screen;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.StationUiProfile;
import com.boaat.jazzy_cookin.kitchen.StationUiProfile.KitchenScreenLayout;
import com.boaat.jazzy_cookin.kitchen.StationUiProfile.MetricWidgetSpec;
import com.boaat.jazzy_cookin.kitchen.sim.domain.SimulationDomainType;
import com.boaat.jazzy_cookin.block.entity.KitchenStationBlockEntity;
import com.boaat.jazzy_cookin.menu.KitchenStationMenu;
import com.boaat.jazzy_cookin.recipebook.client.RecipeBookClientState;
import com.boaat.jazzy_cookin.screen.layout.ActionWidgetSpec;
import com.boaat.jazzy_cookin.screen.layout.LayoutRegion;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class KitchenStationScreen extends AbstractContainerScreen<KitchenStationMenu> {
    private final StationUiProfile baseProfile;
    private StationUiProfile profile;
    private KitchenScreenLayout layout;
    private Button startButton;
    private Button lowHeatButton;
    private Button mediumHeatButton;
    private Button highHeatButton;
    private Button lowerControlButton;
    private Button raiseControlButton;
    private Button secondaryActionButton;
    private Button tertiaryActionButton;
    private Button recipeBookButton;
    private EditBox ovenTemperatureBox;
    private EditBox microwaveDurationBox;
    private int pendingOvenTemperature = -1;
    private int pendingMicrowaveDuration = -1;
    private Button heldActionButton;
    private int heldActionButtonId = -1;
    private double lastMouseX;
    private double lastMouseY;

    private record SimMetric(Component shortLabel, Component tooltipLabel, String valueText, float ratio, int color) {
    }

    public KitchenStationScreen(KitchenStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.baseProfile = menu.uiProfile();
        this.profile = this.baseProfile;
        this.layout = this.profile.layout();
        this.imageWidth = this.profile.width();
        this.imageHeight = this.profile.height();
        this.inventoryLabelY = this.profile.inventoryLabelY();
    }

    @Override
    protected void init() {
        this.profile = this.baseProfile.resolve(this.width, this.height);
        this.layout = this.profile.layout();
        this.imageWidth = this.profile.width();
        this.imageHeight = this.profile.height();
        this.inventoryLabelY = this.profile.inventoryLabelY();
        super.init();
        this.leftPos = Math.max(0, this.leftPos);
        this.topPos = Math.max(0, this.topPos);
        this.repositionSlots();

        LayoutRegion primary = this.layout.primaryAction().bounds();
        this.startButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.jazzycookin.start"), button -> this.sendButton(this.primaryActionButtonId()))
                .bounds(this.leftPos + primary.x(), this.topPos + primary.y(), primary.width(), primary.height())
                .build());
        this.startButton.setAlpha(0.0F);
        LayoutRegion secondary = this.layout.secondaryAction().bounds();
        this.secondaryActionButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.jazzycookin.stir"), button -> this.sendButton(7))
                .bounds(this.leftPos + secondary.x(), this.topPos + secondary.y(), secondary.width(), secondary.height())
                .build());
        this.secondaryActionButton.setAlpha(0.0F);
        LayoutRegion tertiary = this.layout.tertiaryAction().bounds();
        this.tertiaryActionButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.jazzycookin.fold_flip"), button -> this.sendButton(8))
                .bounds(this.leftPos + tertiary.x(), this.topPos + tertiary.y(), tertiary.width(), tertiary.height())
                .build());
        this.tertiaryActionButton.setAlpha(0.0F);

        int recipeButtonWidth = Math.max(66, this.font.width(Component.translatable("screen.jazzycookin.recipe_book_short")) + 14);
        this.recipeBookButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.jazzycookin.recipe_book_short"),
                button -> RecipeBookClientState.openRecipeBook()).bounds(this.leftPos + this.imageWidth - recipeButtonWidth - 10, this.topPos + 8, recipeButtonWidth, 18).build());
        if (this.menu.stationType() == StationType.OVEN) {
            LayoutRegion field = this.layout.ovenFieldRegion();
            this.ovenTemperatureBox = new EditBox(this.font, this.leftPos + field.x(), this.topPos + field.y(), field.width(), 18,
                    Component.translatable("screen.jazzycookin.temperature_short"));
            this.ovenTemperatureBox.setMaxLength(3);
            this.ovenTemperatureBox.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
            this.ovenTemperatureBox.setBordered(false);
            this.ovenTemperatureBox.setTextColor(JazzyGuiRenderer.TEXT);
            this.ovenTemperatureBox.setTextColorUneditable(JazzyGuiRenderer.TEXT);
            this.ovenTemperatureBox.setValue(Integer.toString(this.menu.ovenTemperature()));
            this.addRenderableWidget(this.ovenTemperatureBox);
        } else if (this.menu.stationType() == StationType.MICROWAVE) {
            LayoutRegion field = this.layout.ovenFieldRegion();
            this.microwaveDurationBox = new EditBox(this.font, this.leftPos + field.x(), this.topPos + field.y(), field.width(), 18,
                    Component.literal("Time"));
            this.microwaveDurationBox.setMaxLength(3);
            this.microwaveDurationBox.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
            this.microwaveDurationBox.setBordered(false);
            this.microwaveDurationBox.setTextColor(JazzyGuiRenderer.TEXT);
            this.microwaveDurationBox.setTextColorUneditable(JazzyGuiRenderer.TEXT);
            this.microwaveDurationBox.setValue(Integer.toString(this.menu.microwaveDurationSeconds()));
            this.addRenderableWidget(this.microwaveDurationBox);
        } else if (this.menu.stationType().supportsHeat()) {
            LayoutRegion low = this.layout.lowHeatAction().bounds();
            LayoutRegion medium = this.layout.mediumHeatAction().bounds();
            LayoutRegion high = this.layout.highHeatAction().bounds();
            this.lowHeatButton = this.addRenderableWidget(Button.builder(Component.literal("L"), button -> this.sendButton(1))
                    .bounds(this.leftPos + low.x(), this.topPos + low.y(), low.width(), low.height())
                    .build());
            this.lowHeatButton.setAlpha(0.0F);
            this.mediumHeatButton = this.addRenderableWidget(Button.builder(Component.literal("M"), button -> this.sendButton(2))
                    .bounds(this.leftPos + medium.x(), this.topPos + medium.y(), medium.width(), medium.height())
                    .build());
            this.mediumHeatButton.setAlpha(0.0F);
            this.highHeatButton = this.addRenderableWidget(Button.builder(Component.literal("H"), button -> this.sendButton(3))
                    .bounds(this.leftPos + high.x(), this.topPos + high.y(), high.width(), high.height())
                    .build());
            this.highHeatButton.setAlpha(0.0F);
        }

        if (this.menu.stationType().supportsStationControl()) {
            LayoutRegion lower = this.layout.lowerControlAction().bounds();
            LayoutRegion raise = this.layout.raiseControlAction().bounds();
            this.lowerControlButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.sendButton(4))
                    .bounds(this.leftPos + lower.x(), this.topPos + lower.y(), lower.width(), lower.height())
                    .build());
            this.lowerControlButton.setAlpha(0.0F);
            this.raiseControlButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.sendButton(5))
                    .bounds(this.leftPos + raise.x(), this.topPos + raise.y(), raise.width(), raise.height())
                    .build());
            this.raiseControlButton.setAlpha(0.0F);
        }

        this.updateButtonStates();
    }

    private void repositionSlots() {
        for (int inputIndex = 0; inputIndex < this.menu.activeInputCount(); inputIndex++) {
            StationUiProfile.Point point = this.profile.inputPositions()[inputIndex];
            Slot slot = this.menu.getSlot(inputIndex);
            SlotPositioning.setPosition(slot, point.x(), point.y());
        }

        Slot toolSlot = this.menu.getSlot(this.menu.toolMenuSlotIndex());
        SlotPositioning.setPosition(toolSlot, this.profile.toolPosition().x(), this.profile.toolPosition().y());

        Slot outputSlot = this.menu.getSlot(this.menu.outputMenuSlotIndex());
        SlotPositioning.setPosition(outputSlot, this.profile.outputPosition().x(), this.profile.outputPosition().y());

        Slot byproductSlot = this.menu.getSlot(this.menu.byproductMenuSlotIndex());
        SlotPositioning.setPosition(byproductSlot, this.profile.byproductPosition().x(), this.profile.byproductPosition().y());

        int playerInventoryBase = this.menu.visibleStationSlotCount();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = playerInventoryBase + col + row * 9;
                Slot slot = this.menu.getSlot(slotIndex);
                SlotPositioning.setPosition(slot, this.profile.playerInventoryStartX() + col * 18, this.profile.playerInventoryStartY() + row * 18);
            }
        }

        int hotbarBase = playerInventoryBase + 27;
        for (int col = 0; col < 9; col++) {
            Slot slot = this.menu.getSlot(hotbarBase + col);
            SlotPositioning.setPosition(slot, this.profile.playerInventoryStartX() + col * 18, this.profile.hotbarY());
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.updateButtonStates();
        this.syncOvenTemperatureField();
        this.syncMicrowaveDurationField();
        this.tickHeldAction();
    }

    private void updateButtonStates() {
        if (this.startButton != null) {
            this.startButton.setMessage(this.primaryActionLabel());
            this.startButton.active = this.primaryActionActive();
        }
        if (this.secondaryActionButton != null) {
            boolean panSimulation = this.isPanSimulation();
            this.secondaryActionButton.visible = panSimulation;
            this.secondaryActionButton.active = panSimulation && this.menu.simulationBatchPresent();
        }
        if (this.tertiaryActionButton != null) {
            boolean panSimulation = this.isPanSimulation();
            this.tertiaryActionButton.visible = panSimulation;
            this.tertiaryActionButton.active = panSimulation && this.menu.simulationBatchPresent();
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

    private void tickHeldAction() {
        if (this.heldActionButtonId < 0 || this.minecraft == null || this.minecraft.getWindow() == null) {
            return;
        }
        if (GLFW.glfwGetMouseButton(this.minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            this.clearHeldAction();
            return;
        }
        if (this.heldActionButton == null
                || !this.heldActionButton.visible
                || !this.heldActionButton.active
                || !this.heldActionButton.isMouseOver(this.lastMouseX, this.lastMouseY)) {
            this.clearHeldAction();
            return;
        }
        this.sendButton(this.heldActionButtonId);
    }

    private void beginHeldAction(Button button, int buttonId) {
        if (!this.menu.simulationMode()) {
            return;
        }
        if (buttonId == 6 && this.isHoldablePrimaryAction()) {
            this.heldActionButton = button;
            this.heldActionButtonId = buttonId;
            return;
        }
        if ((buttonId == 7 || buttonId == 8) && this.isPanSimulation() && this.menu.simulationBatchPresent()) {
            this.heldActionButton = button;
            this.heldActionButtonId = buttonId;
        }
    }

    private boolean isHoldablePrimaryAction() {
        return this.menu.simulationMode()
                && switch (this.menu.currentMethod()) {
                    case WHISK, MIX, KNEAD, BATTER, PROCESS, BLEND, JUICE, FREEZE_DRY -> true;
                    default -> false;
                };
    }

    private void clearHeldAction() {
        this.heldActionButton = null;
        this.heldActionButtonId = -1;
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

    private void syncMicrowaveDurationField() {
        if (this.microwaveDurationBox == null || this.microwaveDurationBox.isFocused()) {
            return;
        }

        int syncedDuration = this.menu.microwaveDurationSeconds();
        if (this.pendingMicrowaveDuration == syncedDuration) {
            this.pendingMicrowaveDuration = -1;
        }

        int displayDuration = this.pendingMicrowaveDuration > 0 ? this.pendingMicrowaveDuration : syncedDuration;
        String displayText = Integer.toString(displayDuration);
        if (!this.microwaveDurationBox.getValue().equals(displayText)) {
            this.microwaveDurationBox.setValue(displayText);
        }
    }

    private void commitMicrowaveDuration() {
        if (this.microwaveDurationBox == null) {
            return;
        }
        String value = this.microwaveDurationBox.getValue();
        if (value.isBlank()) {
            this.syncMicrowaveDurationField();
            return;
        }
        int parsedSeconds;
        try {
            parsedSeconds = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            this.syncMicrowaveDurationField();
            return;
        }
        this.applyMicrowaveDuration(parsedSeconds);
    }

    private void applyMicrowaveDuration(int seconds) {
        int normalizedDuration = KitchenStationBlockEntity.normalizeMicrowaveDuration(seconds);
        this.pendingMicrowaveDuration = normalizedDuration;
        if (this.microwaveDurationBox != null) {
            this.microwaveDurationBox.setValue(Integer.toString(normalizedDuration));
        }
        this.sendButton(2000 + normalizedDuration);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        StationUiProfile.Theme theme = this.profile.theme();

        JazzyGuiRenderer.drawStationShell(guiGraphics, left, top, this.imageWidth, this.imageHeight, theme);
        JazzyGuiRenderer.drawWorkspaceBackdrop(guiGraphics, left, top, this.layout.workspaceRegion(), this.layout.toolRegion(), theme, this.layout.family());
        JazzyGuiRenderer.drawPreviewBackdrop(guiGraphics, left, top, this.layout.previewRegion(), this.layout.outputRegion(), this.layout.byproductRegion(), theme);
        JazzyGuiRenderer.drawMetricCluster(guiGraphics, left, top, this.layout.metricClusterRegion(), theme);
        JazzyGuiRenderer.drawControlStrip(guiGraphics, left, top, this.layout.controlStripRegion(), theme);
        JazzyGuiRenderer.drawInventoryShelf(guiGraphics, left, top, this.layout.inventoryShelfRegion(), theme);

        Component methodLabel = this.menu.currentMethod().displayName();
        LayoutRegion chipBounds = this.headerChipBounds(methodLabel);
        if (chipBounds != null) {
            JazzyGuiRenderer.drawChip(
                    guiGraphics,
                    left + chipBounds.x(),
                    top + chipBounds.y(),
                    chipBounds.width(),
                    chipBounds.height(),
                    this.menu.currentMethod().isCookMethod(),
                    theme
            );
        }

        if (!this.controlDisplayLabel().getString().isEmpty()) {
            JazzyGuiRenderer.drawChip(
                    guiGraphics,
                    left + this.layout.controlChipRegion().x(),
                    top + this.layout.controlChipRegion().y(),
                    this.layout.controlChipRegion().width(),
                    this.layout.controlChipRegion().height(),
                    this.menu.stationType().supportsHeat(),
                    theme
            );
        }

        for (int slotIndex = 0; slotIndex < this.menu.slots.size(); slotIndex++) {
            Slot slot = this.menu.getSlot(slotIndex);
            if (slotIndex == this.menu.toolMenuSlotIndex() && !this.menu.stationType().usesTools()) {
                JazzyGuiRenderer.drawDisabledSlot(guiGraphics, left + slot.x, top + slot.y);
            } else {
                JazzyGuiRenderer.drawSlot(guiGraphics, left + slot.x, top + slot.y);
            }
        }

        if (this.menu.simulationMode()) {
            this.renderSimulationMetrics(guiGraphics, left, top);
        } else {
            this.renderLegacyMetricPanel(guiGraphics, left, top);
        }

        this.renderActionButtons(guiGraphics, left, top, mouseX, mouseY);
    }

    private void renderSimulationMetrics(GuiGraphics guiGraphics, int left, int top) {
        List<SimMetric> metrics = this.simulationMetrics();
        if (metrics.isEmpty()) {
            return;
        }
        for (int i = 0; i < metrics.size(); i++) {
            SimMetric metric = metrics.get(i);
            MetricWidgetSpec spec = this.layout.metricWidget(i, metrics.size());
            JazzyGuiRenderer.drawMetricWidget(guiGraphics, left, top, spec, this.profile.theme(), metric.ratio(), metric.color());
            this.drawTrimmedLabel(guiGraphics, metric.shortLabel(), left + spec.labelBounds().x(), top + spec.labelBounds().y(),
                    spec.labelBounds().width(), JazzyGuiRenderer.TEXT_MUTED);
            this.drawRightAlignedLabel(guiGraphics, Component.literal(metric.valueText()), left + spec.valueBounds().right(),
                    top + spec.valueBounds().y(), JazzyGuiRenderer.TEXT, false);
        }
    }

    private void renderLegacyMetricPanel(GuiGraphics guiGraphics, int left, int top) {
        List<SimMetric> legacyMetrics = List.of(
                new SimMetric(Component.translatable("screen.jazzycookin.metric.work"), Component.empty(), this.primaryStatusText().getString(),
                        this.menu.maxProgress() > 0 ? this.menu.progress() / (float) this.menu.maxProgress() : 0.0F, JazzyGuiRenderer.ACCENT),
                new SimMetric(Component.translatable("screen.jazzycookin.metric.ready"), Component.empty(), this.secondaryStatusText().getString(),
                        this.menu.stationType() == StationType.OVEN ? this.menu.preheatProgress() / 100.0F : (this.menu.environmentStatus() == 1 ? 1.0F : 0.0F),
                        this.menu.stationType() == StationType.OVEN ? JazzyGuiRenderer.ACCENT_WARM : this.secondaryStatusColor())
        );
        for (int i = 0; i < legacyMetrics.size(); i++) {
            SimMetric metric = legacyMetrics.get(i);
            MetricWidgetSpec spec = this.layout.metricWidget(i, legacyMetrics.size());
            JazzyGuiRenderer.drawMetricWidget(guiGraphics, left, top, spec, this.profile.theme(), metric.ratio(), metric.color());
            this.drawTrimmedLabel(guiGraphics, metric.shortLabel(), left + spec.labelBounds().x(), top + spec.labelBounds().y(),
                    spec.labelBounds().width(), JazzyGuiRenderer.TEXT_MUTED);
            this.drawRightAlignedLabel(guiGraphics, Component.literal(metric.valueText()), left + spec.valueBounds().right(),
                    top + spec.valueBounds().y(), this.secondaryStatusColor(), false);
        }
    }

    private void renderActionButtons(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        this.renderActionButton(guiGraphics, left, top, mouseX, mouseY, this.startButton, this.layout.primaryAction(), this.primaryActionLabel(), this.startButton.active);

        if (this.secondaryActionButton != null && this.secondaryActionButton.visible) {
            this.renderActionButton(guiGraphics, left, top, mouseX, mouseY, this.secondaryActionButton, this.layout.secondaryAction(),
                    Component.translatable("screen.jazzycookin.stir"), this.secondaryActionButton.active);
        }
        if (this.tertiaryActionButton != null && this.tertiaryActionButton.visible) {
            this.renderActionButton(guiGraphics, left, top, mouseX, mouseY, this.tertiaryActionButton, this.layout.tertiaryAction(),
                    Component.translatable("screen.jazzycookin.fold_flip"), this.tertiaryActionButton.active);
        }

        if (this.lowHeatButton != null) {
            this.renderActionButton(guiGraphics, left, top, mouseX, mouseY, this.lowHeatButton, this.layout.lowHeatAction(), Component.literal("L"),
                    this.lowHeatButton.active || this.menu.heatLevel() == HeatLevel.LOW);
            this.renderActionButton(guiGraphics, left, top, mouseX, mouseY, this.mediumHeatButton, this.layout.mediumHeatAction(), Component.literal("M"),
                    this.mediumHeatButton.active || this.menu.heatLevel() == HeatLevel.MEDIUM);
            this.renderActionButton(guiGraphics, left, top, mouseX, mouseY, this.highHeatButton, this.layout.highHeatAction(), Component.literal("H"),
                    this.highHeatButton.active || this.menu.heatLevel() == HeatLevel.HIGH);
        }
        if (this.lowerControlButton != null) {
            this.renderActionButton(guiGraphics, left, top, mouseX, mouseY, this.lowerControlButton, this.layout.lowerControlAction(), Component.literal("<"),
                    this.lowerControlButton.active);
            this.renderActionButton(guiGraphics, left, top, mouseX, mouseY, this.raiseControlButton, this.layout.raiseControlAction(), Component.literal(">"),
                    this.raiseControlButton.active);
        }
    }

    private void renderActionButton(
            GuiGraphics guiGraphics,
            int left,
            int top,
            int mouseX,
            int mouseY,
            Button button,
            ActionWidgetSpec spec,
            Component label,
            boolean active
    ) {
        if (button == null || spec == null || !button.visible) {
            return;
        }
        boolean hovered = button.isMouseOver(mouseX, mouseY);
        boolean pressed = this.heldActionButton == button;
        JazzyGuiRenderer.drawActionPlate(guiGraphics, left, top, spec, this.profile.theme(), active, hovered, pressed);
        this.drawCenteredTrimmedLabel(guiGraphics, label, left + spec.captionBounds().centerX(), top + spec.captionBounds().y(),
                spec.captionBounds().width() - 8, active ? 0xFFF8F7F2 : JazzyGuiRenderer.TEXT_SOFT, false);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component methodLabel = this.menu.currentMethod().displayName();
        LayoutRegion chipBounds = this.headerChipBounds(methodLabel);

        guiGraphics.drawString(this.font, this.title, this.layout.titleRegion().x(), this.layout.titleRegion().y(), JazzyGuiRenderer.TITLE_TEXT, false);
        if (chipBounds != null) {
            this.drawCenteredTrimmedLabel(guiGraphics, methodLabel, chipBounds.centerX(), chipBounds.y() + 4, chipBounds.width() - 10, JazzyGuiRenderer.TEXT, false);
        }

        guiGraphics.drawString(this.font, this.workspaceLabel(), this.layout.workspaceRegion().x() + 8, this.layout.workspaceRegion().y() + 8, JazzyGuiRenderer.TEXT_MUTED, false);
        if (this.menu.stationType().usesTools()) {
            this.drawCenteredLabel(guiGraphics, Component.translatable("screen.jazzycookin.tool_short"),
                    this.layout.toolRegion().centerX(), this.layout.toolRegion().bottom() + 4, JazzyGuiRenderer.TEXT_SOFT, false);
        }
        this.drawCenteredLabel(guiGraphics, Component.translatable("screen.jazzycookin.preview_short"),
                this.layout.previewRegion().centerX(), this.layout.previewRegion().y() + 8, JazzyGuiRenderer.TEXT_MUTED, false);
        this.drawCenteredLabel(guiGraphics, this.menu.simulationMode() ? this.simulationDomainLabel() : Component.translatable("screen.jazzycookin.status_short"),
                this.layout.metricClusterRegion().centerX(), this.layout.metricClusterRegion().y() + 8, JazzyGuiRenderer.TEXT_MUTED, false);

        this.drawCenteredLabel(guiGraphics, Component.translatable("screen.jazzycookin.output"),
                this.layout.outputRegion().centerX(), this.layout.outputRegion().y() - 10, JazzyGuiRenderer.TEXT_SOFT, false);
        this.drawCenteredLabel(guiGraphics, Component.translatable("screen.jazzycookin.byproduct_short"),
                this.layout.byproductRegion().centerX(), this.layout.byproductRegion().y() - 10, JazzyGuiRenderer.TEXT_SOFT, false);

        this.drawTrimmedLabel(guiGraphics, this.previewHeadline(), this.layout.previewRegion().x() + 10, this.layout.previewRegion().y() + 26,
                this.layout.previewRegion().width() - 56, this.previewHeadlineColor());
        this.drawTrimmedLabel(guiGraphics, this.previewSubline(), this.layout.previewRegion().x() + 10, this.layout.previewRegion().y() + 40,
                this.layout.previewRegion().width() - 56, JazzyGuiRenderer.TEXT_SOFT);

        if (this.menu.stationType() == StationType.OVEN) {
            LayoutRegion field = this.layout.ovenFieldRegion();
            guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.temperature_short"),
                    this.layout.controlStripRegion().x() + 8, this.layout.controlStripRegion().y() + 8, JazzyGuiRenderer.TEXT_MUTED, false);
            guiGraphics.drawString(this.font, Component.literal("F"), field.right() + 4, field.y() + 5, JazzyGuiRenderer.TEXT_MUTED, false);
        } else if (this.menu.stationType() == StationType.MICROWAVE) {
            LayoutRegion field = this.layout.ovenFieldRegion();
            guiGraphics.drawString(this.font, Component.literal("Time"),
                    this.layout.controlStripRegion().x() + 8, this.layout.controlStripRegion().y() + 8, JazzyGuiRenderer.TEXT_MUTED, false);
            guiGraphics.drawString(this.font, Component.literal("s"), field.right() + 4, field.y() + 5, JazzyGuiRenderer.TEXT_MUTED, false);
        }

        Component helper = this.controlHelperText();
        if (!helper.getString().isEmpty()) {
            int helperX;
            if (this.raiseControlButton != null && this.raiseControlButton.visible) {
                helperX = this.layout.raiseControlAction().bounds().right() + 4;
            } else {
                helperX = this.layout.helperTextRegion().x();
            }
            int helperY = this.layout.helperTextRegion().y();
            int helperMaxRight;
            if (this.tertiaryActionButton != null && this.tertiaryActionButton.visible) {
                helperMaxRight = this.layout.tertiaryAction().bounds().x() - 4;
            } else if (this.secondaryActionButton != null && this.secondaryActionButton.visible) {
                helperMaxRight = this.layout.secondaryAction().bounds().x() - 4;
            } else {
                helperMaxRight = this.layout.primaryAction().bounds().x() - 4;
            }
            int helperWidth = Math.max(0, helperMaxRight - helperX);
            this.drawTrimmedLabel(guiGraphics, helper, helperX, helperY, helperWidth, JazzyGuiRenderer.TEXT_MUTED);
        }

        Component controlLabel = this.controlDisplayLabel();
        if (!controlLabel.getString().isEmpty() && this.layout.controlChipRegion() != null) {
            this.drawCenteredLabel(guiGraphics, controlLabel, this.layout.controlChipRegion().centerX(), this.layout.controlChipRegion().y() + 3,
                    JazzyGuiRenderer.TEXT_MUTED, false);
        }

        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.layout.inventoryLabelRegion().x(), this.layout.inventoryLabelRegion().y(),
                JazzyGuiRenderer.TEXT, false);
    }

    private List<SimMetric> simulationMetrics() {
        if (!this.menu.simulationMode()) {
            return List.of();
        }
        return switch (this.menu.activeDomain()) {
            case PAN -> List.of(
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.pan"), Component.translatable("screen.jazzycookin.metric.pan_temp"), this.menu.simPanTempF() + "F", this.simPanTempRatio(), JazzyGuiRenderer.ACCENT_WARM),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.set"), Component.translatable("screen.jazzycookin.metric.doneness"), this.menu.simDoneness() + "%", this.menu.simDoneness() / 100.0F, JazzyGuiRenderer.ACCENT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.wet"), Component.translatable("screen.jazzycookin.metric.moisture"), this.menu.simMoisture() + "%", this.menu.simMoisture() / 100.0F, JazzyGuiRenderer.READY_TEXT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.brn"), Component.translatable("screen.jazzycookin.metric.browning"), this.menu.simBrowning() + "%", this.menu.simBrowning() / 100.0F, JazzyGuiRenderer.ACCENT_WARM)
            );
            case MIX -> this.menu.currentMethod() == KitchenMethod.WHISK
                    ? List.of(
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.air"), Component.translatable("screen.jazzycookin.metric.aeration"), this.menu.simAeration() + "%", this.menu.simAeration() / 100.0F, JazzyGuiRenderer.ACCENT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.frag"), Component.translatable("screen.jazzycookin.metric.fragmentation"), this.menu.simFragmentation() + "%", this.menu.simFragmentation() / 100.0F, JazzyGuiRenderer.ACCENT_WARM)
            )
                    : List.of(
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.body"), Component.translatable("screen.jazzycookin.metric.body_full"), this.menu.simDoneness() + "%", this.menu.simDoneness() / 100.0F, JazzyGuiRenderer.ACCENT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.wet"), Component.translatable("screen.jazzycookin.metric.moisture"), this.menu.simMoisture() + "%", this.menu.simMoisture() / 100.0F, JazzyGuiRenderer.READY_TEXT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.air"), Component.translatable("screen.jazzycookin.metric.aeration"), this.menu.simAeration() + "%", this.menu.simAeration() / 100.0F, JazzyGuiRenderer.ACCENT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.text"), Component.translatable("screen.jazzycookin.metric.texture"), this.menu.simFragmentation() + "%", this.menu.simFragmentation() / 100.0F, JazzyGuiRenderer.ACCENT_WARM)
            );
            case PROCESS -> List.of(
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.body"), Component.translatable("screen.jazzycookin.metric.body_full"), this.menu.simDoneness() + "%", this.menu.simDoneness() / 100.0F, JazzyGuiRenderer.ACCENT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.wet"), Component.translatable("screen.jazzycookin.metric.moisture"), this.menu.simMoisture() + "%", this.menu.simMoisture() / 100.0F, JazzyGuiRenderer.READY_TEXT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.text"), Component.translatable("screen.jazzycookin.metric.texture"), this.menu.simFragmentation() + "%", this.menu.simFragmentation() / 100.0F, JazzyGuiRenderer.ACCENT_WARM),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.air"), Component.translatable("screen.jazzycookin.metric.aeration"), this.menu.simAeration() + "%", this.menu.simAeration() / 100.0F, JazzyGuiRenderer.ACCENT)
            );
            case BLEND -> List.of(
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.body"), Component.translatable("screen.jazzycookin.metric.body_full"), this.menu.simDoneness() + "%", this.menu.simDoneness() / 100.0F, JazzyGuiRenderer.ACCENT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.wet"), Component.translatable("screen.jazzycookin.metric.moisture"), this.menu.simMoisture() + "%", this.menu.simMoisture() / 100.0F, JazzyGuiRenderer.READY_TEXT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.air"), Component.translatable("screen.jazzycookin.metric.aeration"), this.menu.simAeration() + "%", this.menu.simAeration() / 100.0F, JazzyGuiRenderer.ACCENT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.text"), Component.translatable("screen.jazzycookin.metric.texture"), this.menu.simFragmentation() + "%", this.menu.simFragmentation() / 100.0F, JazzyGuiRenderer.ACCENT_WARM)
            );
            case JUICE -> List.of(
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.body"), Component.translatable("screen.jazzycookin.metric.body_full"), this.menu.simDoneness() + "%", this.menu.simDoneness() / 100.0F, JazzyGuiRenderer.ACCENT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.juice"), Component.translatable("screen.jazzycookin.metric.moisture"), this.menu.simMoisture() + "%", this.menu.simMoisture() / 100.0F, JazzyGuiRenderer.READY_TEXT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.foam"), Component.translatable("screen.jazzycookin.metric.aeration"), this.menu.simAeration() + "%", this.menu.simAeration() / 100.0F, JazzyGuiRenderer.ACCENT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.pulp"), Component.translatable("screen.jazzycookin.metric.fragmentation"), this.menu.simFragmentation() + "%", this.menu.simFragmentation() / 100.0F, JazzyGuiRenderer.ACCENT_WARM)
            );
            case DRY -> List.of(
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.body"), Component.translatable("screen.jazzycookin.metric.body_full"), this.menu.simDoneness() + "%", this.menu.simDoneness() / 100.0F, JazzyGuiRenderer.ACCENT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.dry"), Component.translatable("screen.jazzycookin.metric.dryness"), (100 - this.menu.simMoisture()) + "%", (100 - this.menu.simMoisture()) / 100.0F, JazzyGuiRenderer.ACCENT_WARM),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.frag"), Component.translatable("screen.jazzycookin.metric.fragmentation"), this.menu.simFragmentation() + "%", this.menu.simFragmentation() / 100.0F, JazzyGuiRenderer.ACCENT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.air"), Component.translatable("screen.jazzycookin.metric.aeration"), this.menu.simAeration() + "%", this.menu.simAeration() / 100.0F, JazzyGuiRenderer.READY_TEXT)
            );
            case PREP -> List.of(
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.work"), Component.translatable("screen.jazzycookin.metric.progress_full"), this.menu.simDoneness() + "%", this.menu.simDoneness() / 100.0F, JazzyGuiRenderer.ACCENT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.ready"), Component.translatable("screen.jazzycookin.metric.readiness"), this.menu.simMoisture() + "%", this.menu.simMoisture() / 100.0F, this.menu.environmentStatus() == 1 ? JazzyGuiRenderer.READY_TEXT : JazzyGuiRenderer.BLOCKED_TEXT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.ctrl"), Component.translatable("screen.jazzycookin.metric.control"), this.menu.simBrowning() + "%", this.menu.simBrowning() / 100.0F, JazzyGuiRenderer.ACCENT_WARM)
            );
            case POT -> List.of(
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.work"), Component.translatable("screen.jazzycookin.metric.progress_full"), this.menu.simDoneness() + "%", this.menu.simDoneness() / 100.0F, JazzyGuiRenderer.ACCENT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.temp"), Component.translatable("screen.jazzycookin.metric.station_temp"), this.menu.simPanTempF() + "F", this.simPanTempRatio(), JazzyGuiRenderer.ACCENT_WARM),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.ready"), Component.translatable("screen.jazzycookin.metric.readiness"), this.menu.simMoisture() + "%", this.menu.simMoisture() / 100.0F, this.menu.environmentStatus() == 1 ? JazzyGuiRenderer.READY_TEXT : JazzyGuiRenderer.BLOCKED_TEXT)
            );
            case OVEN -> List.of(
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.work"), Component.translatable("screen.jazzycookin.metric.progress_full"), this.menu.simDoneness() + "%", this.menu.simDoneness() / 100.0F, JazzyGuiRenderer.ACCENT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.temp"), Component.translatable("screen.jazzycookin.metric.station_temp"), this.menu.simPanTempF() + "F", Math.max(0.0F, Math.min(1.0F, (this.menu.simPanTempF() - 72.0F) / 420.0F)), JazzyGuiRenderer.ACCENT_WARM),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.heat"), Component.translatable("screen.jazzycookin.metric.preheat_full"), this.menu.simMoisture() + "%", this.menu.simMoisture() / 100.0F, JazzyGuiRenderer.READY_TEXT)
            );
            case PRESERVE -> List.of(
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.work"), Component.translatable("screen.jazzycookin.metric.progress_full"), this.menu.simDoneness() + "%", this.menu.simDoneness() / 100.0F, JazzyGuiRenderer.ACCENT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.ready"), Component.translatable("screen.jazzycookin.metric.readiness"), this.menu.simMoisture() + "%", this.menu.simMoisture() / 100.0F, this.menu.environmentStatus() == 1 ? JazzyGuiRenderer.READY_TEXT : JazzyGuiRenderer.BLOCKED_TEXT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.ctrl"), Component.translatable("screen.jazzycookin.metric.control"), this.menu.simBrowning() + "%", this.menu.simBrowning() / 100.0F, JazzyGuiRenderer.ACCENT_WARM)
            );
            case REST -> List.of(
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.rest"), Component.translatable("screen.jazzycookin.metric.progress_full"), this.menu.simDoneness() + "%", this.menu.simDoneness() / 100.0F, JazzyGuiRenderer.ACCENT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.ready"), Component.translatable("screen.jazzycookin.metric.readiness"), this.menu.simMoisture() + "%", this.menu.simMoisture() / 100.0F, this.menu.environmentStatus() == 1 ? JazzyGuiRenderer.READY_TEXT : JazzyGuiRenderer.BLOCKED_TEXT)
            );
            case PLATE -> List.of(
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.plate"), Component.translatable("screen.jazzycookin.metric.progress_full"), this.menu.simDoneness() + "%", this.menu.simDoneness() / 100.0F, JazzyGuiRenderer.ACCENT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.ready"), Component.translatable("screen.jazzycookin.metric.readiness"), this.menu.simMoisture() + "%", this.menu.simMoisture() / 100.0F, JazzyGuiRenderer.READY_TEXT)
            );
            default -> List.of(
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.body"), Component.translatable("screen.jazzycookin.metric.body_full"), this.menu.simDoneness() + "%", this.menu.simDoneness() / 100.0F, JazzyGuiRenderer.ACCENT),
                    new SimMetric(Component.translatable("screen.jazzycookin.metric.wet"), Component.translatable("screen.jazzycookin.metric.moisture"), this.menu.simMoisture() + "%", this.menu.simMoisture() / 100.0F, JazzyGuiRenderer.READY_TEXT)
            );
        };
    }

    private Component simulationDomainLabel() {
        return switch (this.menu.activeDomain()) {
            case PAN -> Component.translatable("screen.jazzycookin.domain.pan");
            case MIX -> Component.translatable("screen.jazzycookin.domain.mix");
            case PROCESS -> Component.translatable("screen.jazzycookin.domain.process");
            case BLEND -> Component.translatable("screen.jazzycookin.domain.blend");
            case JUICE -> Component.translatable("screen.jazzycookin.domain.juice");
            case DRY -> Component.translatable("screen.jazzycookin.domain.dry");
            case PREP -> Component.translatable("screen.jazzycookin.domain.prep");
            case POT -> Component.translatable("screen.jazzycookin.domain.pot");
            case OVEN -> Component.translatable("screen.jazzycookin.domain.oven");
            case PRESERVE -> Component.translatable("screen.jazzycookin.domain.preserve");
            case REST -> Component.translatable("screen.jazzycookin.domain.rest");
            case PLATE -> Component.translatable("screen.jazzycookin.domain.plate");
            default -> Component.translatable("screen.jazzycookin.domain.simulation");
        };
    }

    private Component simulationPreviewLine() {
        if (!this.menu.simulationMode()) {
            return Component.empty();
        }
        if (this.menu.simRecognizerId() > 0) {
            return Component.translatable("screen.jazzycookin.sim_preview", this.menu.simulationPreviewName());
        }
        return this.simulationHint();
    }

    private Component simulationHint() {
        return switch (this.menu.activeDomain()) {
            case PAN -> this.menu.simulationBatchPresent()
                    ? Component.translatable("screen.jazzycookin.sim_hint.pan_active")
                    : Component.translatable("screen.jazzycookin.sim_hint.pan_start");
            case MIX -> this.menu.currentMethod() == KitchenMethod.WHISK
                    ? Component.translatable("screen.jazzycookin.sim_hint.whisk")
                    : Component.translatable("screen.jazzycookin.sim_hint.mix");
            case PROCESS -> Component.translatable("screen.jazzycookin.sim_hint.process");
            case BLEND -> Component.translatable("screen.jazzycookin.sim_hint.blend");
            case JUICE -> Component.translatable("screen.jazzycookin.sim_hint.juice");
            case DRY -> Component.translatable("screen.jazzycookin.sim_hint.dry");
            case PREP -> Component.translatable("screen.jazzycookin.sim_hint.prep");
            case POT -> Component.translatable("screen.jazzycookin.sim_hint.pot");
            case OVEN -> Component.translatable("screen.jazzycookin.sim_hint.oven");
            case PRESERVE -> Component.translatable("screen.jazzycookin.sim_hint.preserve");
            case REST -> Component.translatable("screen.jazzycookin.sim_hint.rest");
            case PLATE -> Component.translatable("screen.jazzycookin.sim_hint.plate");
            default -> Component.translatable("screen.jazzycookin.sim_hint.generic");
        };
    }

    private Component simulationStatusTooltip() {
        StringBuilder builder = new StringBuilder(this.simulationDomainLabel().getString());
        if (this.menu.simRecognizerId() > 0) {
            builder.append(" | ").append(Component.translatable("screen.jazzycookin.sim_preview", this.menu.simulationPreviewName()).getString());
        }
        for (SimMetric metric : this.simulationMetrics()) {
            builder.append(" | ").append(metric.tooltipLabel().getString()).append(": ").append(metric.valueText());
        }
        if (this.isPanSimulation()) {
            builder.append(" | ").append(Component.translatable("screen.jazzycookin.metric.char_full").getString()).append(": ").append(this.menu.simChar()).append("%");
        }
        return Component.literal(builder.toString());
    }

    private Component primaryActionTooltip() {
        if (!this.menu.simulationMode()) {
            return Component.empty();
        }
        return switch (this.menu.currentMethod()) {
            case WHISK -> Component.translatable("screen.jazzycookin.action_hint.hold_whisk");
            case MIX -> Component.translatable("screen.jazzycookin.action_hint.hold_mix");
            case KNEAD -> Component.translatable("screen.jazzycookin.action_hint.hold_knead");
            case BATTER -> Component.translatable("screen.jazzycookin.action_hint.hold_batter");
            case PROCESS -> Component.translatable("screen.jazzycookin.action_hint.hold_process");
            case BLEND -> Component.translatable("screen.jazzycookin.action_hint.hold_blend");
            case JUICE -> Component.translatable("screen.jazzycookin.action_hint.hold_juice");
            case FREEZE_DRY -> Component.translatable("screen.jazzycookin.action_hint.hold_dry");
            case PAN_FRY -> this.menu.simulationBatchPresent()
                    ? Component.translatable("screen.jazzycookin.action_hint.tap_remove")
                    : Component.translatable("screen.jazzycookin.action_hint.tap_pour");
            case CUT -> Component.translatable("screen.jazzycookin.action_hint.tap_cut");
            case GRIND -> Component.translatable("screen.jazzycookin.action_hint.tap_grind");
            case STRAIN -> Component.translatable("screen.jazzycookin.action_hint.tap_strain");
            case BOIL -> Component.translatable("screen.jazzycookin.action_hint.tap_boil");
            case SIMMER -> Component.translatable("screen.jazzycookin.action_hint.tap_simmer");
            case BAKE -> Component.translatable("screen.jazzycookin.action_hint.tap_bake");
            case ROAST -> Component.translatable("screen.jazzycookin.action_hint.tap_roast");
            case BROIL -> Component.translatable("screen.jazzycookin.action_hint.tap_broil");
            case STEAM -> Component.translatable("screen.jazzycookin.action_hint.tap_steam");
            case SMOKE -> Component.translatable("screen.jazzycookin.action_hint.tap_smoke");
            case FERMENT -> Component.translatable("screen.jazzycookin.action_hint.tap_ferment");
            case CAN -> Component.translatable("screen.jazzycookin.action_hint.tap_can");
            case DRY -> Component.translatable("screen.jazzycookin.action_hint.tap_dry");
            case MICROWAVE -> Component.translatable("screen.jazzycookin.action_hint.tap_microwave");
            case COOL -> Component.translatable("screen.jazzycookin.action_hint.tap_cool");
            case REST -> Component.translatable("screen.jazzycookin.action_hint.tap_rest");
            case SLICE -> Component.translatable("screen.jazzycookin.action_hint.tap_slice");
            case PLATE -> Component.translatable("screen.jazzycookin.action_hint.tap_plate");
            default -> Component.empty();
        };
    }

    private Component secondaryActionTooltip() {
        return Component.translatable("screen.jazzycookin.action_hint.hold_stir");
    }

    private Component tertiaryActionTooltip() {
        return Component.translatable("screen.jazzycookin.action_hint.hold_fold");
    }

    private Component controlDisplayLabel() {
        if (this.menu.stationType() == StationType.OVEN || this.menu.stationType() == StationType.MICROWAVE) {
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
        if (this.menu.stationType() == StationType.MICROWAVE) {
            return this.menu.maxProgress() > 0
                    ? Component.translatable("screen.jazzycookin.working")
                    : Component.literal(this.menu.microwaveDurationSeconds() + "s");
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

    private void drawCenteredTrimmedLabel(GuiGraphics guiGraphics, Component label, int centerX, int y, int maxWidth, int color, boolean shadow) {
        String text = label.getString();
        if (text.isEmpty()) {
            return;
        }
        String trimmed = this.font.plainSubstrByWidth(text, Math.max(0, maxWidth));
        guiGraphics.drawString(this.font, trimmed, centerX - this.font.width(trimmed) / 2, y, color, shadow);
    }

    private void drawTrimmedLabel(GuiGraphics guiGraphics, Component label, int x, int y, int maxWidth, int color) {
        String text = label.getString();
        if (text.isEmpty()) {
            return;
        }
        String trimmed = this.font.plainSubstrByWidth(text, Math.max(0, maxWidth));
        guiGraphics.drawString(this.font, trimmed, x, y, color, false);
    }

    private void drawRightAlignedLabel(GuiGraphics guiGraphics, Component label, int rightX, int y, int color, boolean shadow) {
        if (label.getString().isEmpty()) {
            return;
        }
        guiGraphics.drawString(this.font, label, rightX - this.font.width(label), y, color, shadow);
    }

    private LayoutRegion headerChipBounds(Component label) {
        LayoutRegion chipRegion = this.layout.headerChipRegion();
        int maxRight = this.recipeBookButton == null ? chipRegion.right() : this.recipeBookButton.getX() - this.leftPos - 8;
        int minLeft = Math.max(chipRegion.x(), this.layout.titleRegion().right() + 10);
        int availableWidth = maxRight - minLeft;
        if (availableWidth < 48) {
            return null;
        }
        int desiredWidth = Math.min(availableWidth, Math.max(56, this.font.width(label) + 16));
        return new LayoutRegion(maxRight - desiredWidth, chipRegion.y(), desiredWidth, chipRegion.height());
    }

    private Component previewHeadline() {
        if (this.menu.simulationMode()) {
            if (this.menu.simRecognizerId() > 0) {
                return this.menu.simulationPreviewName();
            }
            return this.simulationDomainLabel();
        }
        return this.primaryStatusText();
    }

    private Component previewSubline() {
        if (this.menu.simulationMode()) {
            return this.simulationHint();
        }
        return this.secondaryStatusText().getString().isEmpty() ? Component.translatable("screen.jazzycookin.waiting_short") : this.secondaryStatusText();
    }

    private int previewHeadlineColor() {
        if (this.menu.simulationMode()) {
            return this.menu.simRecognizerId() > 0 ? JazzyGuiRenderer.READY_TEXT : JazzyGuiRenderer.TEXT;
        }
        return this.secondaryStatusColor();
    }

    private Component controlHelperText() {
        if (this.menu.stationType() == StationType.OVEN) {
            return Component.translatable("screen.jazzycookin.helper_oven_scroll");
        }
        if (this.menu.stationType() == StationType.MICROWAVE) {
            return Component.literal("Type or scroll in 10s steps.");
        }
        if (this.isPanSimulation()) {
            return this.menu.simulationBatchPresent()
                    ? Component.translatable("screen.jazzycookin.helper_pan_actions")
                    : this.primaryActionTooltip();
        }
        return this.menu.simulationMode() ? this.primaryActionTooltip() : Component.translatable("screen.jazzycookin.helper_action_ready");
    }

    private Component primaryActionLabel() {
        if (!this.menu.simulationMode()) {
            return Component.translatable("screen.jazzycookin.start");
        }
        return switch (this.menu.currentMethod()) {
            case WHISK -> Component.translatable("screen.jazzycookin.whisk");
            case PAN_FRY -> this.menu.simulationBatchPresent()
                    ? Component.translatable("screen.jazzycookin.remove")
                    : Component.translatable("screen.jazzycookin.pour");
            case CUT -> Component.literal("Cut");
            case GRIND -> Component.literal("Grind");
            case STRAIN -> Component.literal("Strain");
            case MIX -> Component.literal("Mix");
            case KNEAD -> Component.literal("Knead");
            case BATTER -> Component.literal("Mix");
            case BOIL -> Component.literal("Boil");
            case SIMMER -> Component.literal("Simmer");
            case BAKE -> Component.literal("Bake");
            case ROAST -> Component.literal("Roast");
            case BROIL -> Component.literal("Broil");
            case STEAM -> Component.literal("Steam");
            case SMOKE -> Component.literal("Smoke");
            case FERMENT -> Component.literal("Ferment");
            case CAN -> Component.literal("Can");
            case DRY -> Component.literal("Dry");
            case MICROWAVE -> Component.literal("Heat");
            case COOL -> Component.literal("Cool");
            case REST -> Component.literal("Rest");
            case SLICE -> Component.literal("Slice");
            case PLATE -> Component.literal("Plate");
            case PROCESS -> Component.literal("Pulse");
            case BLEND -> Component.literal("Blend");
            case JUICE -> Component.literal("Juice");
            case FREEZE_DRY -> Component.literal("Dry");
            default -> Component.translatable("screen.jazzycookin.start");
        };
    }

    private int primaryActionButtonId() {
        return this.menu.simulationMode() ? 6 : 0;
    }

    private boolean primaryActionActive() {
        if (!this.menu.simulationMode()) {
            return this.menu.environmentStatus() != 0;
        }
        if (this.isPanSimulation()) {
            return this.menu.simulationBatchPresent() || this.hasInputItems();
        }
        if (this.menu.simulationWorking()) {
            return false;
        }
        if (this.menu.currentMethod() == KitchenMethod.WHISK && this.menu.getSlot(this.menu.outputMenuSlotIndex()).hasItem()) {
            return true;
        }
        return this.hasInputItems() && this.menu.environmentStatus() != 0;
    }

    private boolean hasInputItems() {
        for (int slot = 0; slot < this.menu.activeInputCount(); slot++) {
            if (this.menu.getSlot(slot).hasItem()) {
                return true;
            }
        }
        return false;
    }

    private boolean isPanSimulation() {
        return this.menu.simulationMode() && this.menu.currentMethod() == KitchenMethod.PAN_FRY;
    }

    private float simPanTempRatio() {
        return Math.max(0.0F, Math.min(1.0F, (this.menu.simPanTempF() - 72.0F) / 340.0F));
    }

    private Component workspaceLabel() {
        return switch (this.menu.stationType()) {
            case PREP_TABLE -> Component.literal("Mise en Place");
            case MIXING_BOWL -> Component.literal("Bowl");
            case PLATING_STATION -> Component.literal("Service");
            case STOVE -> Component.literal("Burners");
            case OVEN -> Component.literal("Tray");
            case FOOD_PROCESSOR -> Component.literal("Hopper");
            case BLENDER -> Component.literal("Pitcher");
            case JUICER -> Component.literal("Feed");
            case FREEZE_DRYER -> Component.literal("Trays");
            case CANNING_STATION -> Component.literal("Jar Rack");
            case FERMENTATION_CROCK -> Component.literal("Crock");
            case STEAMER -> Component.literal("Basket");
            case SMOKER -> Component.literal("Smoking Rack");
            case DRYING_RACK -> Component.literal("Rack");
            case COOLING_RACK -> Component.literal("Cooling");
            case RESTING_BOARD -> Component.literal("Rest");
            case SPICE_GRINDER -> Component.literal("Mill");
            case STRAINER -> Component.literal("Mesh");
            case MICROWAVE -> Component.literal("Bay");
        };
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        this.renderSimulationTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderSimulationTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!this.menu.simulationMode()) {
            return;
        }
        if (this.startButton != null && this.startButton.visible && this.startButton.isMouseOver(mouseX, mouseY)) {
            Component tooltip = this.primaryActionTooltip();
            if (!tooltip.getString().isEmpty()) {
                guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
            }
            return;
        }
        if (this.secondaryActionButton != null && this.secondaryActionButton.visible && this.secondaryActionButton.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, this.secondaryActionTooltip(), mouseX, mouseY);
            return;
        }
        if (this.tertiaryActionButton != null && this.tertiaryActionButton.visible && this.tertiaryActionButton.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, this.tertiaryActionTooltip(), mouseX, mouseY);
            return;
        }
        if (this.isHovering(this.layout.metricClusterRegion().x(), this.layout.metricClusterRegion().y(),
                this.layout.metricClusterRegion().width(), this.layout.metricClusterRegion().height(), mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, this.simulationStatusTooltip(), mouseX, mouseY);
            return;
        }
        if (this.isHovering(this.layout.previewRegion().x(), this.layout.previewRegion().y(),
                this.layout.previewRegion().width(), this.layout.previewRegion().height(), mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, this.simulationPreviewLine(), mouseX, mouseY);
            return;
        }
        if (this.isHovering(this.layout.controlStripRegion().x(), this.layout.controlStripRegion().y(),
                this.layout.controlStripRegion().width(), this.layout.controlStripRegion().height(), mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, this.simulationHint(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        boolean wasFocused = this.ovenTemperatureBox != null && this.ovenTemperatureBox.isFocused();
        boolean microwaveWasFocused = this.microwaveDurationBox != null && this.microwaveDurationBox.isFocused();
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (handled && this.startButton != null && this.startButton.visible && this.startButton.active && this.startButton.isMouseOver(mouseX, mouseY)) {
                this.beginHeldAction(this.startButton, this.primaryActionButtonId());
            } else if (handled && this.secondaryActionButton != null && this.secondaryActionButton.visible && this.secondaryActionButton.active
                    && this.secondaryActionButton.isMouseOver(mouseX, mouseY)) {
                this.beginHeldAction(this.secondaryActionButton, 7);
            } else if (handled && this.tertiaryActionButton != null && this.tertiaryActionButton.visible && this.tertiaryActionButton.active
                    && this.tertiaryActionButton.isMouseOver(mouseX, mouseY)) {
                this.beginHeldAction(this.tertiaryActionButton, 8);
            } else {
                this.clearHeldAction();
            }
        }
        if (wasFocused && this.ovenTemperatureBox != null && !this.ovenTemperatureBox.isFocused()) {
            this.commitOvenTemperature();
        }
        if (microwaveWasFocused && this.microwaveDurationBox != null && !this.microwaveDurationBox.isFocused()) {
            this.commitMicrowaveDuration();
        }
        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.clearHeldAction();
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
        if (this.microwaveDurationBox != null
                && scrollY != 0.0D
                && (this.microwaveDurationBox.isMouseOver(mouseX, mouseY) || this.microwaveDurationBox.isFocused())) {
            int baseDuration = this.pendingMicrowaveDuration > 0 ? this.pendingMicrowaveDuration : this.menu.microwaveDurationSeconds();
            int nextDuration = baseDuration + (scrollY > 0.0D ? 10 : -10);
            this.applyMicrowaveDuration(nextDuration);
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
        if (this.microwaveDurationBox != null && this.microwaveDurationBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                this.commitMicrowaveDuration();
                this.microwaveDurationBox.setFocused(false);
                return true;
            }
            if (this.microwaveDurationBox.keyPressed(keyCode, scanCode, modifiers)) {
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
        if (this.microwaveDurationBox != null && this.microwaveDurationBox.isFocused() && this.microwaveDurationBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void removed() {
        this.clearHeldAction();
        if (this.ovenTemperatureBox != null && this.ovenTemperatureBox.isFocused()) {
            this.commitOvenTemperature();
        }
        if (this.microwaveDurationBox != null && this.microwaveDurationBox.isFocused()) {
            this.commitMicrowaveDuration();
        }
        super.removed();
    }
}
