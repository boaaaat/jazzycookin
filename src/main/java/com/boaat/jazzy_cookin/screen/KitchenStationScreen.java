package com.boaat.jazzy_cookin.screen;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.StationUiProfile;
import com.boaat.jazzy_cookin.kitchen.sim.domain.SimulationDomainType;
import com.boaat.jazzy_cookin.menu.KitchenStationMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class KitchenStationScreen extends AbstractContainerScreen<KitchenStationMenu> {
    private static final int STATUS_BAR_WIDTH = 22;

    private final StationUiProfile profile;
    private Button startButton;
    private Button lowHeatButton;
    private Button mediumHeatButton;
    private Button highHeatButton;
    private Button lowerControlButton;
    private Button raiseControlButton;
    private Button secondaryActionButton;
    private Button tertiaryActionButton;
    private EditBox ovenTemperatureBox;
    private int pendingOvenTemperature = -1;
    private Button heldActionButton;
    private int heldActionButtonId = -1;
    private double lastMouseX;
    private double lastMouseY;

    private record SimMetric(Component shortLabel, Component tooltipLabel, String valueText, float ratio, int color) {
    }

    public KitchenStationScreen(KitchenStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.profile = menu.uiProfile();
        this.imageWidth = this.profile.width();
        this.imageHeight = this.profile.height();
        this.inventoryLabelY = this.profile.inventoryLabelY();
    }

    @Override
    protected void init() {
        super.init();

        StationUiProfile.Point primary = this.profile.primaryButtonPosition();
        this.startButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.jazzycookin.start"), button -> this.sendButton(this.primaryActionButtonId()))
                .bounds(this.leftPos + primary.x(), this.topPos + primary.y(), 42, 18)
                .build());
        StationUiProfile.Point secondary = this.profile.secondaryButtonPosition();
        this.secondaryActionButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.jazzycookin.stir"), button -> this.sendButton(7))
                .bounds(this.leftPos + secondary.x(), this.topPos + secondary.y(), 42, 18)
                .build());
        StationUiProfile.Point tertiary = this.profile.tertiaryButtonPosition();
        this.tertiaryActionButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.jazzycookin.fold_flip"), button -> this.sendButton(8))
                .bounds(this.leftPos + tertiary.x(), this.topPos + tertiary.y(), 42, 18)
                .build());

        if (this.menu.stationType() == StationType.OVEN) {
            StationUiProfile.Rect field = this.profile.ovenTemperatureFieldRect();
            this.ovenTemperatureBox = new EditBox(this.font, this.leftPos + field.x(), this.topPos + field.y(), field.width(), 18,
                    Component.translatable("screen.jazzycookin.temperature_short"));
            this.ovenTemperatureBox.setMaxLength(3);
            this.ovenTemperatureBox.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
            this.ovenTemperatureBox.setTextColor(JazzyGuiRenderer.TEXT);
            this.ovenTemperatureBox.setTextColorUneditable(JazzyGuiRenderer.TEXT);
            this.ovenTemperatureBox.setValue(Integer.toString(this.menu.ovenTemperature()));
            this.addRenderableWidget(this.ovenTemperatureBox);
        } else if (this.menu.stationType().supportsHeat()) {
            StationUiProfile.Point low = this.profile.lowHeatButtonPosition();
            StationUiProfile.Point medium = this.profile.mediumHeatButtonPosition();
            StationUiProfile.Point high = this.profile.highHeatButtonPosition();
            this.lowHeatButton = this.addRenderableWidget(Button.builder(Component.literal("L"), button -> this.sendButton(1))
                    .bounds(this.leftPos + low.x(), this.topPos + low.y(), 18, 18)
                    .build());
            this.mediumHeatButton = this.addRenderableWidget(Button.builder(Component.literal("M"), button -> this.sendButton(2))
                    .bounds(this.leftPos + medium.x(), this.topPos + medium.y(), 18, 18)
                    .build());
            this.highHeatButton = this.addRenderableWidget(Button.builder(Component.literal("H"), button -> this.sendButton(3))
                    .bounds(this.leftPos + high.x(), this.topPos + high.y(), 18, 18)
                    .build());
        }

        if (this.menu.stationType().supportsStationControl()) {
            StationUiProfile.Point lower = this.profile.lowerControlButtonPosition();
            StationUiProfile.Point raise = this.profile.raiseControlButtonPosition();
            this.lowerControlButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.sendButton(4))
                    .bounds(this.leftPos + lower.x(), this.topPos + lower.y(), 18, 18)
                    .build());
            this.raiseControlButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.sendButton(5))
                    .bounds(this.leftPos + raise.x(), this.topPos + raise.y(), 18, 18)
                    .build());
        }

        this.updateButtonStates();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.updateButtonStates();
        this.syncOvenTemperatureField();
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

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        StationUiProfile.Theme theme = this.profile.theme();
        StationUiProfile.Rect deck = this.profile.topDeckCard();
        StationUiProfile.Rect work = this.profile.workspaceCard();
        StationUiProfile.Rect status = this.profile.statusCard();
        StationUiProfile.Rect result = this.profile.resultCard();
        StationUiProfile.Rect action = this.profile.actionCard();
        StationUiProfile.Rect inventory = this.profile.inventoryCard();

        JazzyGuiRenderer.drawWindow(guiGraphics, left, top, this.imageWidth, this.imageHeight, theme);
        JazzyGuiRenderer.drawCard(guiGraphics, left + deck.x(), top + deck.y(), deck.width(), deck.height(), theme);
        JazzyGuiRenderer.drawCard(guiGraphics, left + work.x(), top + work.y(), work.width(), work.height(), theme);
        JazzyGuiRenderer.drawCard(guiGraphics, left + status.x(), top + status.y(), status.width(), status.height(), theme);
        JazzyGuiRenderer.drawCard(guiGraphics, left + result.x(), top + result.y(), result.width(), result.height(), theme);
        JazzyGuiRenderer.drawCard(guiGraphics, left + action.x(), top + action.y(), action.width(), action.height(), theme);
        JazzyGuiRenderer.drawCard(guiGraphics, left + inventory.x(), top + inventory.y(), inventory.width(), inventory.height(), theme);

        Component methodLabel = this.menu.currentMethod().displayName();
        int methodChipWidth = Math.max(46, this.font.width(methodLabel) + 16);
        JazzyGuiRenderer.drawChip(
                guiGraphics,
                left + this.imageWidth - methodChipWidth - 14,
                top + 8,
                methodChipWidth,
                14,
                this.menu.currentMethod().isCookMethod(),
                theme
        );

        if (!this.controlDisplayLabel().getString().isEmpty()) {
            StationUiProfile.Rect chip = this.profile.controlChipRect();
            JazzyGuiRenderer.drawChip(
                    guiGraphics,
                    left + chip.x(),
                    top + chip.y(),
                    chip.width(),
                    chip.height(),
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
            this.renderSimulationBars(guiGraphics, left, top, status);
        } else {
            float progress = this.menu.maxProgress() > 0 ? this.menu.progress() / (float) this.menu.maxProgress() : 0.0F;
            int barWidth = Math.min(STATUS_BAR_WIDTH, Math.max(18, status.width() - 24));
            int barX = left + status.right() - barWidth - 8;
            JazzyGuiRenderer.drawProgressBar(guiGraphics, barX, top + status.y() + 18, barWidth, progress, JazzyGuiRenderer.ACCENT);
            if (this.menu.stationType() == StationType.OVEN) {
                JazzyGuiRenderer.drawProgressBar(guiGraphics, barX, top + status.y() + 36, barWidth, this.menu.preheatProgress() / 100.0F,
                        JazzyGuiRenderer.ACCENT_WARM);
            }
        }
    }

    private void renderSimulationBars(GuiGraphics guiGraphics, int left, int top, StationUiProfile.Rect status) {
        List<SimMetric> metrics = this.simulationMetrics();
        if (metrics.isEmpty()) {
            return;
        }
        int baseY = status.y() + (metrics.size() > 3 ? 16 : 22);
        int gap = metrics.size() > 3 ? 9 : 16;
        int barX = left + status.right() - STATUS_BAR_WIDTH - 8;
        for (int i = 0; i < metrics.size(); i++) {
            SimMetric metric = metrics.get(i);
            int y = top + baseY + i * gap;
            JazzyGuiRenderer.drawProgressBar(guiGraphics, barX, y, STATUS_BAR_WIDTH, metric.ratio(), metric.color());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        StationUiProfile.Rect work = this.profile.workspaceCard();
        StationUiProfile.Rect status = this.profile.statusCard();
        StationUiProfile.Rect result = this.profile.resultCard();
        StationUiProfile.Rect action = this.profile.actionCard();
        Component methodLabel = this.menu.currentMethod().displayName();
        int methodChipWidth = Math.max(46, this.font.width(methodLabel) + 16);
        int methodChipX = this.imageWidth - methodChipWidth - 14;

        guiGraphics.drawString(this.font, this.title, 14, 10, JazzyGuiRenderer.TITLE_TEXT, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.profile.inventoryCard().x() + 6, this.inventoryLabelY, JazzyGuiRenderer.TEXT, false);
        this.drawCenteredLabel(guiGraphics, methodLabel, methodChipX + methodChipWidth / 2, 11, JazzyGuiRenderer.TEXT, false);

        guiGraphics.drawString(this.font, this.workspaceLabel(), work.x(), work.y() - 11, JazzyGuiRenderer.TEXT_MUTED, false);
        if (this.menu.stationType().usesTools()) {
            Component toolLabel = Component.translatable("screen.jazzycookin.tool_short");
            guiGraphics.drawString(this.font, toolLabel, work.right() - this.font.width(toolLabel) - 4, work.y() - 11, JazzyGuiRenderer.TEXT_MUTED, false);
        }
        guiGraphics.drawString(
                this.font,
                this.menu.simulationMode() ? Component.translatable("screen.jazzycookin.feedback_short") : Component.translatable("screen.jazzycookin.status_short"),
                status.x(),
                status.y() - 11,
                JazzyGuiRenderer.TEXT_MUTED,
                false
        );
        guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.result_short"), result.x(), result.y() - 11, JazzyGuiRenderer.TEXT_MUTED, false);

        if (this.menu.simulationMode()) {
            this.drawCenteredLabel(guiGraphics, this.simulationDomainLabel(), status.centerX(), status.y() + 6, JazzyGuiRenderer.TEXT, false);
            this.renderSimulationMetricLabels(guiGraphics, status);
            StationUiProfile.Rect summary = this.profile.actionTextArea(this.isPanSimulation(), this.isPanSimulation());
            if (summary.width() >= 42) {
                this.drawTrimmedLabel(
                        guiGraphics,
                        this.simulationPreviewLine(),
                        summary.x(),
                        summary.y(),
                        summary.width(),
                        this.menu.simRecognizerId() > 0 ? JazzyGuiRenderer.READY_TEXT : JazzyGuiRenderer.TEXT_MUTED
                );
            }
        } else {
            guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.progress_short"), status.x() + 4, status.y() + 14, JazzyGuiRenderer.TEXT_SOFT, false);
            if (this.menu.stationType() == StationType.OVEN) {
                guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.preheat_short"), status.x() + 4, status.y() + 32, JazzyGuiRenderer.TEXT_SOFT, false);
            }
            this.drawCenteredLabel(guiGraphics, this.primaryStatusText(), status.centerX(), status.y() + 6, JazzyGuiRenderer.TEXT, false);
            this.drawCenteredLabel(guiGraphics, this.secondaryStatusText(), status.centerX(), status.bottom() - 13, this.secondaryStatusColor(), false);
        }

        this.drawCenteredLabel(guiGraphics, Component.translatable("screen.jazzycookin.output"), result.centerX(), result.y() + 6, JazzyGuiRenderer.TEXT_SOFT, false);
        this.drawCenteredLabel(guiGraphics, Component.translatable("screen.jazzycookin.byproduct_short"), result.centerX(), result.y() + 28, JazzyGuiRenderer.TEXT_SOFT, false);

        if (this.menu.stationType() == StationType.OVEN) {
            StationUiProfile.Rect field = this.profile.ovenTemperatureFieldRect();
            guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.temperature_short"), action.x() + 6, action.y() + 8, JazzyGuiRenderer.TEXT_MUTED, false);
            guiGraphics.drawString(this.font, Component.literal("F"), field.right() + 4, action.y() + 8, JazzyGuiRenderer.TEXT_MUTED, false);
        }

        Component controlLabel = this.controlDisplayLabel();
        if (!controlLabel.getString().isEmpty()) {
            StationUiProfile.Rect chip = this.profile.controlChipRect();
            this.drawCenteredLabel(guiGraphics, controlLabel, chip.centerX(), chip.y() + 3, JazzyGuiRenderer.TEXT_MUTED, false);
        }
    }

    private void renderSimulationMetricLabels(GuiGraphics guiGraphics, StationUiProfile.Rect status) {
        List<SimMetric> metrics = this.simulationMetrics();
        if (metrics.isEmpty()) {
            return;
        }
        int baseY = status.y() + (metrics.size() > 3 ? 16 : 22);
        int gap = metrics.size() > 3 ? 9 : 16;
        for (int i = 0; i < metrics.size(); i++) {
            int y = baseY + i * gap;
            guiGraphics.drawString(this.font, metrics.get(i).shortLabel(), status.x() + 6, y, JazzyGuiRenderer.TEXT_SOFT, false);
        }
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

    private void drawTrimmedLabel(GuiGraphics guiGraphics, Component label, int x, int y, int maxWidth, int color) {
        String text = label.getString();
        if (text.isEmpty()) {
            return;
        }
        String trimmed = this.font.plainSubstrByWidth(text, Math.max(0, maxWidth));
        guiGraphics.drawString(this.font, trimmed, x, y, color, false);
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
        StationUiProfile.Rect status = this.profile.statusCard();
        if (this.isHovering(status.x(), status.y(), status.width(), status.height(), mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, this.simulationStatusTooltip(), mouseX, mouseY);
            return;
        }
        StationUiProfile.Rect action = this.profile.actionCard();
        if (this.isHovering(action.x(), action.y(), action.width(), action.height(), mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, this.simulationHint(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        boolean wasFocused = this.ovenTemperatureBox != null && this.ovenTemperatureBox.isFocused();
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
        this.clearHeldAction();
        if (this.ovenTemperatureBox != null && this.ovenTemperatureBox.isFocused()) {
            this.commitOvenTemperature();
        }
        super.removed();
    }
}
