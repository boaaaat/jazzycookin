package com.boaat.jazzy_cookin.screen;

import java.util.List;
import java.util.Optional;

import org.lwjgl.glfw.GLFW;

import com.boaat.jazzy_cookin.block.entity.KitchenStationBlockEntity;
import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.StationUiProfile;
import com.boaat.jazzy_cookin.kitchen.StationUiProfile.KitchenScreenLayout;
import com.boaat.jazzy_cookin.kitchen.StationUiProfile.MetricWidgetSpec;
import com.boaat.jazzy_cookin.kitchen.sim.domain.SimulationDomainType;
import com.boaat.jazzy_cookin.menu.KitchenStationMenu;
import com.boaat.jazzy_cookin.recipe.KitchenProcessRecipe;
import com.boaat.jazzy_cookin.recipebook.client.RecipeBookClientState;
import com.boaat.jazzy_cookin.registry.JazzyRecipes;
import com.boaat.jazzy_cookin.screen.layout.ActionWidgetSpec;
import com.boaat.jazzy_cookin.screen.layout.LayoutRegion;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class KitchenStationScreen extends AbstractContainerScreen<KitchenStationMenu> {
    private static final int STOVE_BURNER_COUNT = 6;
    private static final int STOVE_BURNER_SIZE = 34;

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
    private ApplianceUiLibComponents.ApplianceRoot applianceOverlayRoot;
    private ApplianceUiLibComponents.ApplianceButton applianceRecipeButton;
    private ApplianceUiLibComponents.ApplianceButton applianceStartAction;
    private ApplianceUiLibComponents.ApplianceButton applianceSecondaryAction;
    private ApplianceUiLibComponents.ApplianceButton applianceTertiaryAction;
    private ApplianceUiLibComponents.ApplianceLabel applianceTitleLabel;
    private ApplianceUiLibComponents.ApplianceLabel applianceWorkspaceLabel;
    private ApplianceUiLibComponents.ApplianceLabel applianceToolLabel;
    private ApplianceUiLibComponents.ApplianceLabel appliancePreviewHeadlineLabel;
    private ApplianceUiLibComponents.ApplianceLabel applianceStatusPrimaryLabel;
    private ApplianceUiLibComponents.ApplianceLabel applianceStatusSecondaryLabel;
    private ApplianceUiLibComponents.ApplianceLabel applianceInventoryLabel;
    private ApplianceUiLibComponents.ApplianceLabel applianceTemperatureLabel;
    private ApplianceUiLibComponents.ApplianceLabel applianceTemperatureSuffixLabel;
    private ApplianceUiLibComponents.ApplianceLabel applianceFuelLabel;
    private ApplianceUiLibComponents.ApplianceStatusChip applianceFuelChip;
    private ApplianceUiLibComponents.StoveDialWidget applianceStoveDial;
    private ApplianceUiLibComponents.ApplianceTextField applianceOvenTemperatureField;
    private ApplianceUiLibComponents.ApplianceButton appliancePreheatAction;
    private ApplianceUiLibComponents.ApplianceStatusChip applianceCookTimeChip;
    private int pendingStoveDialLevel = -1;
    private int pendingOvenTemperature = -1;
    private int pendingOvenCookTimeMinutes = -1;
    private int pendingMicrowaveDuration = -1;
    private Button heldActionButton;
    private int heldActionButtonId = -1;
    private double lastMouseX;
    private double lastMouseY;

    private record SimMetric(Component shortLabel, Component tooltipLabel, String valueText, float ratio, int color) {
    }

    private record SimulationStatusView(
            Component primary,
            Component secondary,
            float barRatio,
            int primaryColor,
            int secondaryColor,
            int barColor
    ) {
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

        this.resetApplianceWidgets();
        this.startButton = null;
        this.lowHeatButton = null;
        this.mediumHeatButton = null;
        this.highHeatButton = null;
        this.lowerControlButton = null;
        this.raiseControlButton = null;
        this.secondaryActionButton = null;
        this.tertiaryActionButton = null;
        this.recipeBookButton = null;
        this.ovenTemperatureBox = null;
        this.microwaveDurationBox = null;

        if (this.usesApplianceUiLib()) {
            this.initApplianceUiLib();
        } else {
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
        }

        this.updateButtonStates();
    }

    private boolean usesApplianceUiLib() {
        return this.menu.stationType() == StationType.STOVE || this.menu.stationType() == StationType.OVEN;
    }

    private void resetApplianceWidgets() {
        this.applianceOverlayRoot = null;
        this.applianceRecipeButton = null;
        this.applianceStartAction = null;
        this.applianceSecondaryAction = null;
        this.applianceTertiaryAction = null;
        this.applianceTitleLabel = null;
        this.applianceWorkspaceLabel = null;
        this.applianceToolLabel = null;
        this.appliancePreviewHeadlineLabel = null;
        this.applianceStatusPrimaryLabel = null;
        this.applianceStatusSecondaryLabel = null;
        this.applianceInventoryLabel = null;
        this.applianceTemperatureLabel = null;
        this.applianceTemperatureSuffixLabel = null;
        this.applianceFuelLabel = null;
        this.applianceFuelChip = null;
        this.applianceStoveDial = null;
        this.applianceOvenTemperatureField = null;
        this.appliancePreheatAction = null;
        this.applianceCookTimeChip = null;
        this.pendingStoveDialLevel = -1;
        this.pendingOvenCookTimeMinutes = -1;
    }

    private void initApplianceUiLib() {
        this.applianceOverlayRoot = new ApplianceUiLibComponents.ApplianceRoot(this.leftPos, this.topPos, this.imageWidth, this.imageHeight);

        LayoutRegion recipeRegion = this.applianceRecipeButtonRegion();
        int titleWidth = Math.max(110, recipeRegion.x() - 22);
        this.applianceTitleLabel = new ApplianceUiLibComponents.ApplianceLabel(14, 11, titleWidth, 12, this.title,
                ApplianceUiLibComponents.LabelAlign.LEFT);
        this.applianceTitleLabel.setColor(JazzyGuiRenderer.TITLE_TEXT);
        this.applianceOverlayRoot.addChild(this.applianceTitleLabel);

        this.applianceRecipeButton = new ApplianceUiLibComponents.ApplianceButton(recipeRegion.x(), recipeRegion.y(),
                recipeRegion.width(), recipeRegion.height(), this.profile.theme(),
                Component.translatable("screen.jazzycookin.recipe_book_short"), RecipeBookClientState::openRecipeBook);
        this.applianceOverlayRoot.addChild(this.applianceRecipeButton);

        LayoutRegion workspace = this.applianceWorkspaceRegion();
        this.applianceWorkspaceLabel = new ApplianceUiLibComponents.ApplianceLabel(workspace.x() + 8, workspace.y() + 8,
                workspace.width() - 16, 12, this.workspaceLabel(), ApplianceUiLibComponents.LabelAlign.LEFT);
        this.applianceWorkspaceLabel.setColor(JazzyGuiRenderer.TEXT_MUTED);
        this.applianceOverlayRoot.addChild(this.applianceWorkspaceLabel);

        int toolLabelY = this.profile.toolPosition().y() + 22;
        if (toolLabelY + 8 >= this.applianceControlRegion().y()) {
            toolLabelY = Math.max(workspace.y() + 18, this.profile.toolPosition().y() - 10);
        }
        this.applianceToolLabel = new ApplianceUiLibComponents.ApplianceLabel(this.profile.toolPosition().x() - 12, toolLabelY,
                40, 12, Component.translatable("screen.jazzycookin.tool_short"), ApplianceUiLibComponents.LabelAlign.CENTER);
        this.applianceToolLabel.setColor(JazzyGuiRenderer.TEXT_SOFT);
        this.applianceOverlayRoot.addChild(this.applianceToolLabel);

        LayoutRegion preview = this.appliancePreviewRegion();
        this.appliancePreviewHeadlineLabel = new ApplianceUiLibComponents.ApplianceLabel(preview.x() + 8, preview.y() + 10,
                preview.width() - 16, 12, Component.empty(), ApplianceUiLibComponents.LabelAlign.CENTER);
        this.appliancePreviewHeadlineLabel.setColor(JazzyGuiRenderer.TEXT_MUTED);
        this.applianceOverlayRoot.addChild(this.appliancePreviewHeadlineLabel);

        LayoutRegion status = this.applianceStatusRegion();
        if (status != null) {
            this.applianceStatusPrimaryLabel = new ApplianceUiLibComponents.ApplianceLabel(status.x() + 8, status.y() + 8,
                    status.width() - 16, 12, Component.empty(), ApplianceUiLibComponents.LabelAlign.CENTER);
            this.applianceStatusPrimaryLabel.setColor(JazzyGuiRenderer.TEXT_MUTED);
            this.applianceOverlayRoot.addChild(this.applianceStatusPrimaryLabel);
            this.applianceStatusSecondaryLabel = new ApplianceUiLibComponents.ApplianceLabel(status.x() + 8, status.y() + 18,
                    status.width() - 16, 12, Component.empty(), ApplianceUiLibComponents.LabelAlign.CENTER);
            this.applianceStatusSecondaryLabel.setColor(JazzyGuiRenderer.TEXT_SOFT);
            this.applianceOverlayRoot.addChild(this.applianceStatusSecondaryLabel);
        }

        LayoutRegion control = this.applianceControlRegion();
        if (this.menu.stationType() == StationType.STOVE) {
            this.applianceFuelLabel = new ApplianceUiLibComponents.ApplianceLabel(control.x() + 8, control.y() + 11,
                    56, 12, Component.literal("⛽"), ApplianceUiLibComponents.LabelAlign.LEFT);
            this.applianceFuelLabel.setColor(JazzyGuiRenderer.TEXT_MUTED);
            this.applianceOverlayRoot.addChild(this.applianceFuelLabel);

            LayoutRegion dial = this.applianceStoveDialRegion();
            this.applianceStoveDial = new ApplianceUiLibComponents.StoveDialWidget(
                    dial.x(),
                    dial.y(),
                    dial.width(),
                    dial.height(),
                    this.profile.theme(),
                    this.menu.stoveDialLevel(),
                    this::applyStoveDialLevel);
            this.applianceOverlayRoot.addChild(this.applianceStoveDial);

            int startWidth = 76;
            int actionWidth = 56;
            int startX = control.right() - startWidth - 8;
            this.applianceStartAction = new ApplianceUiLibComponents.ApplianceButton(startX, control.y() + 7, startWidth, 20,
                    this.profile.theme(), Component.translatable("screen.jazzycookin.start"),
                    () -> this.sendButton(this.primaryActionButtonId()));
            this.applianceOverlayRoot.addChild(this.applianceStartAction);

            this.applianceSecondaryAction = new ApplianceUiLibComponents.ApplianceButton(startX - actionWidth - 6, control.y() + 7,
                    actionWidth, 20, this.profile.theme(), Component.translatable("screen.jazzycookin.stir"),
                    () -> this.sendButton(7));
            this.applianceOverlayRoot.addChild(this.applianceSecondaryAction);

            this.applianceTertiaryAction = new ApplianceUiLibComponents.ApplianceButton(startX - (actionWidth + 6) * 2, control.y() + 7,
                    actionWidth, 20, this.profile.theme(), Component.translatable("screen.jazzycookin.fold_flip"),
                    () -> this.sendButton(8));
            this.applianceOverlayRoot.addChild(this.applianceTertiaryAction);
        } else {
            this.applianceTemperatureLabel = new ApplianceUiLibComponents.ApplianceLabel(control.x() + 10, control.y() + 6,
                    18, 12, Component.literal("🌡"),
                    ApplianceUiLibComponents.LabelAlign.LEFT);
            this.applianceTemperatureLabel.setColor(JazzyGuiRenderer.TEXT_MUTED);
            this.applianceOverlayRoot.addChild(this.applianceTemperatureLabel);

            this.applianceOvenTemperatureField = new ApplianceUiLibComponents.ApplianceTextField(control.x() + 28, control.y() + 2,
                    44, 18, Integer.toString(this.menu.ovenTemperature()));
            this.applianceOvenTemperatureField.setMaxLength(3);
            this.applianceOvenTemperatureField.setTextPredicate(ApplianceUiLibComponents.ApplianceTextField.INTEGER_INPUT_PREDICATE);
            this.applianceOvenTemperatureField.setTextColor(JazzyGuiRenderer.TEXT);
            this.applianceOvenTemperatureField.setTextColorUneditable(JazzyGuiRenderer.TEXT);
            this.applianceOverlayRoot.addChild(this.applianceOvenTemperatureField);

            this.applianceTemperatureSuffixLabel = new ApplianceUiLibComponents.ApplianceLabel(control.x() + 76, control.y() + 6,
                    12, 12, Component.literal("F"), ApplianceUiLibComponents.LabelAlign.LEFT);
            this.applianceTemperatureSuffixLabel.setColor(JazzyGuiRenderer.TEXT_MUTED);
            this.applianceOverlayRoot.addChild(this.applianceTemperatureSuffixLabel);

            this.applianceFuelChip = new ApplianceUiLibComponents.ApplianceStatusChip(control.x() + 118, control.y() + 2,
                    72, 18, this.profile.theme(), Component.empty());
            this.applianceOverlayRoot.addChild(this.applianceFuelChip);

            int actionWidth = 92;
            int startX = control.right() - actionWidth - 8;
            int cookX = control.x() + 28;

            this.appliancePreheatAction = new ApplianceUiLibComponents.ApplianceButton(startX, control.y() + 2, actionWidth, 18,
                    this.profile.theme(), Component.empty(), () -> this.sendButton(4000));
            this.applianceOverlayRoot.addChild(this.appliancePreheatAction);

            this.applianceCookTimeChip = new ApplianceUiLibComponents.ApplianceStatusChip(cookX, control.y() + 24,
                    118, 18, this.profile.theme(), Component.empty());
            this.applianceOverlayRoot.addChild(this.applianceCookTimeChip);

            this.applianceStartAction = new ApplianceUiLibComponents.ApplianceButton(startX, control.y() + 24, actionWidth, 18,
                    this.profile.theme(), Component.translatable("screen.jazzycookin.start"),
                    () -> this.sendButton(this.primaryActionButtonId()));
            this.applianceOverlayRoot.addChild(this.applianceStartAction);
        }
    }

    private LayoutRegion applianceWorkspaceRegion() {
        return this.menu.stationType() == StationType.STOVE
                ? new LayoutRegion(18, 42, 204, 108)
                : new LayoutRegion(18, 42, 192, 114);
    }

    private LayoutRegion appliancePreviewRegion() {
        return this.menu.stationType() == StationType.STOVE
                ? new LayoutRegion(this.imageWidth - 114, 42, 96, 108)
                : new LayoutRegion(this.imageWidth - 126, 42, 108, 114);
    }

    private LayoutRegion applianceStatusRegion() {
        return null;
    }

    private LayoutRegion applianceControlRegion() {
        return this.menu.stationType() == StationType.STOVE
                ? new LayoutRegion(18, 146, this.imageWidth - 36, 40)
                : new LayoutRegion(18, 140, this.imageWidth - 36, 50);
    }

    private LayoutRegion applianceInventoryRegion() {
        return new LayoutRegion(14, this.profile.playerInventoryStartY() - 2, this.imageWidth - 28, 84);
    }

    private LayoutRegion applianceRecipeButtonRegion() {
        int width = Math.max(84, this.font.width(Component.translatable("screen.jazzycookin.recipe_book_short")) + 20);
        return new LayoutRegion(this.imageWidth - width - 12, 8, width, 20);
    }

    private LayoutRegion applianceStoveDialRegion() {
        return new LayoutRegion(90, 78, 36, 24);
    }

    private LayoutRegion stoveBurnerRegion(int burnerIndex) {
        Slot slot = this.menu.getSlot(burnerIndex);
        return new LayoutRegion(slot.x + 8 - STOVE_BURNER_SIZE / 2, slot.y + 8 - STOVE_BURNER_SIZE / 2 - 2, STOVE_BURNER_SIZE, STOVE_BURNER_SIZE);
    }

    private StationUiProfile.Point applianceFuelSlotPosition() {
        LayoutRegion control = this.applianceControlRegion();
        return this.menu.stationType() == StationType.STOVE
                ? new StationUiProfile.Point(control.x() + 38, control.y() + 9)
                : new StationUiProfile.Point(control.x() + 94, control.y() + 2);
    }

    private LayoutRegion ovenCookTimeRegion() {
        LayoutRegion control = this.applianceControlRegion();
        return new LayoutRegion(control.right() - 178, control.y() + 13, 84, 20);
    }

    private int stoveDialLevel() {
        int pending = this.pendingStoveDialLevel;
        return pending >= 0 ? pending : this.menu.stoveDialLevel();
    }

    private void repositionSlots() {
        for (int inputIndex = 0; inputIndex < this.menu.activeInputCount(); inputIndex++) {
            StationUiProfile.Point point = this.profile.inputPositions()[inputIndex];
            Slot slot = this.menu.getSlot(inputIndex);
            SlotPositioning.setPosition(slot, point.x(), point.y());
        }

        Slot toolSlot = this.menu.getSlot(this.menu.toolMenuSlotIndex());
        SlotPositioning.setPosition(toolSlot, this.profile.toolPosition().x(), this.profile.toolPosition().y());

        if (this.menu.usesFuelSlot()) {
            StationUiProfile.Point fuel = this.usesApplianceUiLib() ? this.applianceFuelSlotPosition() : this.profile.toolPosition();
            Slot fuelSlot = this.menu.getSlot(this.menu.fuelMenuSlotIndex());
            SlotPositioning.setPosition(fuelSlot, fuel.x(), fuel.y());
        }

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
        this.syncStoveDial();
        this.syncOvenTemperatureField();
        this.syncOvenCookTimeSelection();
        this.syncMicrowaveDurationField();
        this.tickHeldAction();
    }

    private void updateButtonStates() {
        SimulationStatusView statusView = this.simulationStatusView();
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
        if (this.applianceStartAction != null) {
            this.applianceStartAction.setMessage(this.appliancePrimaryActionLabel());
            this.applianceStartAction.setActive(this.primaryActionActive());
        }
        if (this.applianceSecondaryAction != null) {
            boolean panSimulation = this.isPanSimulation();
            this.applianceSecondaryAction.setVisible(panSimulation);
            this.applianceSecondaryAction.setActive(panSimulation && this.menu.simulationBatchPresent());
        }
        if (this.applianceTertiaryAction != null) {
            boolean panSimulation = this.isPanSimulation();
            this.applianceTertiaryAction.setVisible(panSimulation);
            this.applianceTertiaryAction.setActive(panSimulation && this.menu.simulationBatchPresent());
        }
        if (this.appliancePreviewHeadlineLabel != null) {
            this.appliancePreviewHeadlineLabel.setText(this.previewHeadline());
            this.appliancePreviewHeadlineLabel.setColor(this.previewHeadlineColor());
        }
        if (this.applianceStatusPrimaryLabel != null) {
            this.applianceStatusPrimaryLabel.setText(statusView.primary());
            this.applianceStatusPrimaryLabel.setColor(statusView.primaryColor());
        }
        if (this.applianceStatusSecondaryLabel != null) {
            Component secondary = statusView.secondary();
            this.applianceStatusSecondaryLabel.setVisible(!secondary.getString().isEmpty());
            this.applianceStatusSecondaryLabel.setText(secondary);
            this.applianceStatusSecondaryLabel.setColor(statusView.secondaryColor());
        }
        if (this.applianceFuelLabel != null && this.menu.usesFuelSlot()) {
            this.applianceFuelLabel.setText(this.applianceFuelText());
        }
        if (this.applianceStoveDial != null) {
            this.applianceStoveDial.setDialValue(this.stoveDialLevel());
            this.applianceStoveDial.setActive(true);
        }
        if (this.applianceFuelChip != null && this.menu.usesFuelSlot()) {
            this.applianceFuelChip.setText(this.applianceFuelText());
            this.applianceFuelChip.setTextColor(this.applianceFuelColor());
        }
        if (this.appliancePreheatAction != null) {
            this.appliancePreheatAction.setMessage(this.ovenPreheatButtonText());
            this.appliancePreheatAction.setActive(true);
        }
        if (this.applianceCookTimeChip != null) {
            this.applianceCookTimeChip.setText(this.ovenCookTimeChipText());
            this.applianceCookTimeChip.setTextColor(this.menu.maxProgress() > 0 ? JazzyGuiRenderer.TEXT : JazzyGuiRenderer.TEXT_MUTED);
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
        return switch (this.menu.currentMethod()) {
            case WHISK, MIX, KNEAD, BATTER, PROCESS, BLEND, JUICE, FREEZE_DRY -> true;
            default -> false;
        };
    }

    private void clearHeldAction() {
        this.heldActionButton = null;
        this.heldActionButtonId = -1;
    }

    private void syncStoveDial() {
        if (this.menu.stationType() != StationType.STOVE) {
            return;
        }
        int syncedLevel = this.menu.stoveDialLevel();
        if (this.pendingStoveDialLevel == syncedLevel) {
            this.pendingStoveDialLevel = -1;
        }
    }

    private void applyStoveDialLevel(int level) {
        int normalizedLevel = KitchenStationBlockEntity.normalizeStoveBurnerLevel(level);
        this.pendingStoveDialLevel = normalizedLevel;
        if (this.applianceStoveDial != null) {
            this.applianceStoveDial.setDialValue(normalizedLevel);
        }
        this.sendButton(3100 + normalizedLevel);
    }

    private void syncOvenTemperatureField() {
        boolean vanillaFocused = this.ovenTemperatureBox != null && this.ovenTemperatureBox.isFocused();
        boolean uiLibFocused = this.applianceOvenTemperatureField != null && this.applianceOvenTemperatureField.isFocused();
        if (vanillaFocused || uiLibFocused) {
            return;
        }

        int syncedTemperature = this.menu.ovenTemperature();
        if (this.pendingOvenTemperature == syncedTemperature) {
            this.pendingOvenTemperature = -1;
        }

        int displayTemperature = this.pendingOvenTemperature > 0 ? this.pendingOvenTemperature : syncedTemperature;
        String displayText = Integer.toString(displayTemperature);
        if (this.ovenTemperatureBox != null && !this.ovenTemperatureBox.getValue().equals(displayText)) {
            this.ovenTemperatureBox.setValue(displayText);
        }
        if (this.applianceOvenTemperatureField != null && !this.applianceOvenTemperatureField.textValue().equals(displayText)) {
            this.applianceOvenTemperatureField.setText(displayText);
        }
    }

    private void syncOvenCookTimeSelection() {
        if (this.menu.stationType() != StationType.OVEN) {
            return;
        }
        int syncedMinutes = this.menu.ovenCookTimeMinutes();
        if (this.pendingOvenCookTimeMinutes == syncedMinutes) {
            this.pendingOvenCookTimeMinutes = -1;
        }
    }

    private void commitOvenTemperature() {
        String value;
        if (this.ovenTemperatureBox != null) {
            value = this.ovenTemperatureBox.getValue();
        } else if (this.applianceOvenTemperatureField != null) {
            value = this.applianceOvenTemperatureField.textValue();
        } else {
            return;
        }
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
        if (this.applianceOvenTemperatureField != null) {
            this.applianceOvenTemperatureField.setText(Integer.toString(normalizedTemperature));
        }
        this.sendButton(1000 + normalizedTemperature);
    }

    private int ovenCookTimeMinutes() {
        return this.pendingOvenCookTimeMinutes > 0 ? this.pendingOvenCookTimeMinutes : this.menu.ovenCookTimeMinutes();
    }

    private void applyOvenCookTimeMinutes(int minutes) {
        int normalizedMinutes = KitchenStationBlockEntity.normalizeOvenCookTimeMinutes(minutes);
        this.pendingOvenCookTimeMinutes = normalizedMinutes;
        this.sendButton(5000 + normalizedMinutes);
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

        if (this.usesApplianceUiLib()) {
            this.renderApplianceBackground(guiGraphics, left, top, theme);
            for (int slotIndex = 0; slotIndex < this.menu.slots.size(); slotIndex++) {
                Slot slot = this.menu.getSlot(slotIndex);
                if (slotIndex == this.menu.toolMenuSlotIndex() && !this.menu.stationType().usesTools()) {
                    JazzyGuiRenderer.drawDisabledSlot(guiGraphics, left + slot.x, top + slot.y);
                } else {
                    JazzyGuiRenderer.drawSlot(guiGraphics, left + slot.x, top + slot.y);
                }
            }
            return;
        }

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

        if (this.visibleSimulationMetrics().isEmpty()) {
            this.renderStatusLane(guiGraphics, left, top);
        } else {
            this.renderSimulationMetrics(guiGraphics, left, top);
        }

        this.renderActionButtons(guiGraphics, left, top, mouseX, mouseY);
    }

    private void renderApplianceBackground(GuiGraphics guiGraphics, int left, int top, StationUiProfile.Theme theme) {
        LayoutRegion workspace = this.applianceWorkspaceRegion();
        LayoutRegion preview = this.appliancePreviewRegion();
        LayoutRegion status = this.applianceStatusRegion();
        LayoutRegion control = this.applianceControlRegion();
        LayoutRegion inventory = this.applianceInventoryRegion();

        JazzyGuiRenderer.drawStationShell(guiGraphics, left, top, this.imageWidth, this.imageHeight, theme);
        JazzyGuiRenderer.drawPanel(guiGraphics, left + workspace.x(), top + workspace.y(), workspace.width(), workspace.height(), theme,
                JazzyGuiRenderer.PanelStyle.WORKSPACE);
        JazzyGuiRenderer.drawPanel(guiGraphics, left + preview.x(), top + preview.y(), preview.width(), preview.height(), theme,
                JazzyGuiRenderer.PanelStyle.PREVIEW);
        if (status != null) {
            JazzyGuiRenderer.drawPanel(guiGraphics, left + status.x(), top + status.y(), status.width(), status.height(), theme,
                    JazzyGuiRenderer.PanelStyle.METRIC);
        }
        JazzyGuiRenderer.drawPanel(guiGraphics, left + control.x(), top + control.y(), control.width(), control.height(), theme,
                JazzyGuiRenderer.PanelStyle.CONTROL);
        JazzyGuiRenderer.drawPanel(guiGraphics, left + inventory.x(), top + inventory.y(), inventory.width(), inventory.height(), theme,
                JazzyGuiRenderer.PanelStyle.INVENTORY);

        guiGraphics.fill(left + preview.x() + 20, top + preview.y() + 30, left + preview.right() - 20, top + preview.y() + 31, 0x44231A13);
        guiGraphics.fill(left + preview.x() + 20, top + preview.y() + 58, left + preview.right() - 20, top + preview.y() + 59, 0x44231A13);

        if (this.menu.stationType() == StationType.STOVE) {
            guiGraphics.fill(left + workspace.x() + 12, top + workspace.y() + 18, left + workspace.right() - 12, top + workspace.bottom() - 12, 0x22130F0B);
            guiGraphics.fill(left + workspace.x() + 16, top + workspace.y() + 24, left + workspace.right() - 16, top + workspace.bottom() - 18, 0x2619130E);
            this.renderStoveBurnerDeck(guiGraphics, left, top);
            Slot toolSlot = this.menu.getSlot(this.menu.toolMenuSlotIndex());
            guiGraphics.fill(left + toolSlot.x - 8, top + toolSlot.y - 8, left + toolSlot.x + 24, top + toolSlot.y + 24, 0x33211610);
            if (this.menu.usesFuelSlot()) {
                Slot fuelSlot = this.menu.getSlot(this.menu.fuelMenuSlotIndex());
                guiGraphics.fill(left + fuelSlot.x - 8, top + fuelSlot.y - 8, left + fuelSlot.x + 24, top + fuelSlot.y + 24, 0x33211610);
            }
        } else {
            guiGraphics.fill(left + workspace.x() + 14, top + workspace.y() + 14, left + workspace.right() - 14, top + workspace.bottom() - 14, 0x22110D0A);
            guiGraphics.fill(left + workspace.x() + 22, top + workspace.y() + 16, left + workspace.x() + 92, top + workspace.bottom() - 14, 0x2A20160E);
            for (int rail = 0; rail < 5; rail++) {
                int railY = top + workspace.y() + 20 + rail * 22;
                guiGraphics.fill(left + workspace.x() + 18, railY, left + workspace.right() - 20, railY + 2, 0x44372B20);
            }
            guiGraphics.fill(left + workspace.x() + 34, top + workspace.bottom() - 18, left + workspace.right() - 30, top + workspace.bottom() - 14, 0x33221610);
            Slot toolSlot = this.menu.getSlot(this.menu.toolMenuSlotIndex());
            guiGraphics.fill(left + toolSlot.x - 8, top + toolSlot.y - 8, left + toolSlot.x + 24, top + toolSlot.y + 24, 0x33211610);
            if (this.menu.usesFuelSlot()) {
                Slot fuelSlot = this.menu.getSlot(this.menu.fuelMenuSlotIndex());
                guiGraphics.fill(left + fuelSlot.x - 8, top + fuelSlot.y - 8, left + fuelSlot.x + 24, top + fuelSlot.y + 24, 0x33211610);
            }
        }
    }

    private void renderStoveBurnerDeck(GuiGraphics guiGraphics, int left, int top) {
        final int[] heatColors = {
                0xFF5A2A18,
                0xFF7A3418,
                0xFF9A451A,
                0xFFBC5A1E,
                0xFFE18428,
                0xFFFFC14A
        };
        int dialLevel = this.stoveDialLevel();
        LayoutRegion dial = this.applianceStoveDialRegion();
        int dialLeft = left + dial.x();
        int dialTop = top + dial.y();
        int dialRight = dialLeft + dial.width();
        int dialBottom = dialTop + dial.height();

        guiGraphics.fill(dialLeft - 10, dialTop - 10, dialRight + 10, dialBottom + 10, 0x33271D16);
        guiGraphics.fill(dialLeft - 7, dialTop - 7, dialRight + 7, dialBottom + 7, 0x44150F0B);
        guiGraphics.fill(dialLeft - 2, dialTop - 2, dialRight + 2, dialBottom + 2, 0x551A130E);

        for (int burnerIndex = 0; burnerIndex < this.menu.activeInputCount(); burnerIndex++) {
            LayoutRegion burner = this.stoveBurnerRegion(burnerIndex);
            int burnerLeft = left + burner.x();
            int burnerTop = top + burner.y();
            int burnerRight = burnerLeft + burner.width();
            int burnerBottom = burnerTop + burner.height();
            boolean occupied = this.menu.getSlot(burnerIndex).hasItem();

            guiGraphics.fill(burnerLeft, burnerTop, burnerRight, burnerBottom, 0x44271D16);
            guiGraphics.fill(burnerLeft + 2, burnerTop + 2, burnerRight - 2, burnerBottom - 2, 0xFF14110E);
            guiGraphics.fill(burnerLeft + 4, burnerTop + 4, burnerRight - 4, burnerBottom - 4, 0xFF231C16);
            guiGraphics.fill(burnerLeft + 7, burnerTop + 7, burnerRight - 7, burnerBottom - 7, 0xFF0F0C09);

            if (dialLevel > 0) {
                int glowInset = Math.max(4, 11 - dialLevel);
                int glowColor = dialLevel <= 2 ? 0x557A3418 : dialLevel <= 4 ? 0x77BC5A1E : 0x99FFC14A;
                if (occupied) {
                    glowColor = dialLevel <= 2 ? 0x887A3418 : dialLevel <= 4 ? 0xAABC5A1E : 0xCCFFC14A;
                }
                guiGraphics.fill(burnerLeft + glowInset, burnerTop + glowInset, burnerRight - glowInset, burnerBottom - glowInset, glowColor);
            }

            int meterLeft = burnerLeft + 4;
            int meterTop = burnerBottom + 3;
            int meterWidth = Math.max(18, burner.width() - 8);
            int meterHeight = 5;
            int segmentGap = 1;
            int segmentWidth = Math.max(2, (meterWidth - 5 * segmentGap) / 6);
            guiGraphics.fill(meterLeft - 1, meterTop - 1, meterLeft + meterWidth + 1, meterTop + meterHeight + 1, 0xCC120F0C);
            for (int segment = 0; segment < 6; segment++) {
                int segmentLeft = meterLeft + segment * (segmentWidth + segmentGap);
                int segmentRight = segmentLeft + segmentWidth;
                int color = segment < dialLevel ? heatColors[segment] : 0xFF2A211A;
                guiGraphics.fill(segmentLeft, meterTop, segmentRight, meterTop + meterHeight, color);
            }
        }
    }

    private void renderSimulationMetrics(GuiGraphics guiGraphics, int left, int top) {
        List<SimMetric> metrics = this.visibleSimulationMetrics();
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
                    top + spec.valueBounds().y(), spec.valueBounds().width(), JazzyGuiRenderer.TEXT, false);
        }
    }

    private void renderStatusLane(GuiGraphics guiGraphics, int left, int top) {
        LayoutRegion lane = this.layout.statusLaneRegion();
        if (lane.width() <= 0 || lane.height() <= 0) {
            return;
        }

        SimulationStatusView statusView = this.simulationStatusView();
        Component primary = statusView.primary();
        Component secondary = statusView.secondary();
        boolean showSecondary = !secondary.getString().isEmpty()
                && !secondary.getString().equals(primary.getString())
                && lane.width() >= 72;
        boolean showBar = lane.height() >= 12;
        int inset = 4;
        int textY = top + lane.y() + (showBar ? 0 : Math.max(0, (lane.height() - 8) / 2));

        if (showSecondary) {
            int halfWidth = Math.max(18, (lane.width() - inset * 2 - 6) / 2);
            this.drawTrimmedLabel(guiGraphics, primary, left + lane.x() + inset, textY, halfWidth, statusView.primaryColor());
            this.drawRightAlignedLabel(guiGraphics, secondary, left + lane.right() - inset, textY, halfWidth, statusView.secondaryColor(), false);
        } else {
            this.drawCenteredTrimmedLabel(guiGraphics, primary, left + lane.centerX(), textY, lane.width() - inset * 2, statusView.primaryColor(), false);
        }

        if (showBar) {
            int barX = left + lane.x() + inset;
            int barY = top + lane.bottom() - 3;
            int barWidth = Math.max(14, lane.width() - inset * 2);
            guiGraphics.fill(barX, barY, barX + barWidth, barY + 2, 0xFF1A1F27);
            int fillWidth = Math.round(barWidth * statusView.barRatio());
            if (fillWidth > 0) {
                guiGraphics.fill(barX, barY, barX + fillWidth, barY + 2, statusView.barColor());
            }
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
        if (this.usesApplianceUiLib()) {
            return;
        }

        Component methodLabel = this.menu.currentMethod().displayName();
        LayoutRegion chipBounds = this.headerChipBounds(methodLabel);
        LayoutRegion previewStatus = this.layout.previewStatusRegion();
        Component previewHeadline = this.previewHeadline();
        Component previewSubline = this.previewSubline();
        Component controlLabel = this.controlDisplayLabel();

        guiGraphics.drawString(this.font, this.title, this.layout.titleRegion().x(), this.layout.titleRegion().y(), JazzyGuiRenderer.TITLE_TEXT, false);
        if (chipBounds != null) {
            this.drawCenteredTrimmedLabel(guiGraphics, methodLabel, chipBounds.centerX(), chipBounds.y() + 4, chipBounds.width() - 10, JazzyGuiRenderer.TEXT, false);
        }

        this.drawTrimmedLabel(guiGraphics, this.workspaceLabel(), this.layout.workspaceRegion().x() + 8, this.layout.workspaceRegion().y() + 8,
                this.layout.workspaceRegion().width() - 16, JazzyGuiRenderer.TEXT_MUTED);
        if (this.menu.stationType().usesTools()) {
            int toolLabelY = this.layout.toolRegion().bottom() + 4;
            if (toolLabelY + 8 >= this.layout.controlStripRegion().y()) {
                toolLabelY = Math.max(this.layout.workspaceRegion().y() + 18, this.layout.toolRegion().y() - 10);
            }
            this.drawCenteredTrimmedLabel(guiGraphics, Component.translatable("screen.jazzycookin.tool_short"),
                    this.layout.toolRegion().centerX(), toolLabelY, this.layout.toolRegion().width() + 18, JazzyGuiRenderer.TEXT_SOFT, false);
        }

        boolean showPreviewSubline = previewStatus.height() >= 20
                && previewStatus.width() >= 72
                && !previewSubline.getString().isEmpty()
                && !previewSubline.getString().equals(previewHeadline.getString());
        int previewHeadlineY = showPreviewSubline
                ? previewStatus.y()
                : previewStatus.y() + Math.max(0, (previewStatus.height() - 8) / 2);
        this.drawCenteredTrimmedLabel(guiGraphics, previewHeadline, previewStatus.centerX(), previewHeadlineY,
                previewStatus.width(), this.previewHeadlineColor(), false);
        if (showPreviewSubline) {
            this.drawCenteredTrimmedLabel(guiGraphics, previewSubline, previewStatus.centerX(), previewHeadlineY + 10,
                    previewStatus.width(), JazzyGuiRenderer.TEXT_SOFT, false);
        }

        if (this.menu.stationType() == StationType.OVEN) {
            LayoutRegion field = this.layout.ovenFieldRegion();
            this.drawTrimmedLabel(guiGraphics, Component.translatable("screen.jazzycookin.temperature_short"),
                    this.layout.controlStripRegion().x() + 8, this.layout.controlStripRegion().y() + 8, 20, JazzyGuiRenderer.TEXT_MUTED);
            guiGraphics.drawString(this.font, Component.literal("F"), field.right() + 4, field.y() + 5, JazzyGuiRenderer.TEXT_MUTED, false);
        } else if (this.menu.stationType() == StationType.MICROWAVE) {
            LayoutRegion field = this.layout.ovenFieldRegion();
            this.drawTrimmedLabel(guiGraphics, Component.literal("Time"),
                    this.layout.controlStripRegion().x() + 8, this.layout.controlStripRegion().y() + 8, 20, JazzyGuiRenderer.TEXT_MUTED);
            guiGraphics.drawString(this.font, Component.literal("s"), field.right() + 4, field.y() + 5, JazzyGuiRenderer.TEXT_MUTED, false);
        }

        if (!controlLabel.getString().isEmpty() && this.layout.controlChipRegion() != null) {
            this.drawCenteredTrimmedLabel(guiGraphics, controlLabel, this.layout.controlChipRegion().centerX(), this.layout.controlChipRegion().y() + 5,
                    this.layout.controlChipRegion().width() - 8, JazzyGuiRenderer.TEXT_MUTED, false);
        }

        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.layout.inventoryLabelRegion().x(), this.layout.inventoryLabelRegion().y(),
                JazzyGuiRenderer.TEXT, false);
    }

    private List<SimMetric> simulationMetrics() {
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
        if (this.menu.simRecognizerId() > 0) {
            return Component.translatable("screen.jazzycookin.sim_preview", this.menu.simulationPreviewName());
        }
        return this.simulationHint();
    }

    private List<SimMetric> visibleSimulationMetrics() {
        List<SimMetric> metrics = this.simulationMetrics();
        int maxVisible = this.layout.metricClusterRegion().height() < 22 || this.layout.metricClusterRegion().width() < 86 ? 1 : 2;
        if (metrics.size() <= maxVisible) {
            return metrics;
        }
        return metrics.subList(0, maxVisible);
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

    private SimulationStatusView simulationStatusView() {
        if (this.menu.maxProgress() > 0) {
            int percent = Math.round((this.menu.progress() / (float) this.menu.maxProgress()) * 100.0F);
            return new SimulationStatusView(
                    Component.literal(percent + "%"),
                    this.menu.stationType() == StationType.OVEN ? Component.literal("▶ Cooking") : Component.translatable("screen.jazzycookin.working"),
                    Math.max(0.0F, Math.min(1.0F, this.menu.progress() / (float) this.menu.maxProgress())),
                    JazzyGuiRenderer.TEXT,
                    JazzyGuiRenderer.TEXT_MUTED,
                    JazzyGuiRenderer.ACCENT
            );
        }
        if (this.menu.stationType() == StationType.OVEN) {
            if (this.ovenWaitingForFuel()) {
                return new SimulationStatusView(
                        Component.translatable("screen.jazzycookin.blocked_short"),
                        Component.literal("⛽ Load Fuel"),
                        0.0F,
                        JazzyGuiRenderer.BLOCKED_TEXT,
                        JazzyGuiRenderer.BLOCKED_TEXT,
                        JazzyGuiRenderer.BLOCKED_TEXT
                );
            }
            if (this.menu.ovenPreheating()) {
                if (this.menu.preheatProgress() >= 100) {
                    return new SimulationStatusView(
                            Component.translatable("screen.jazzycookin.ready_short"),
                            Component.literal("🔥 Ready"),
                            1.0F,
                            JazzyGuiRenderer.READY_TEXT,
                            JazzyGuiRenderer.READY_TEXT,
                            JazzyGuiRenderer.ACCENT_WARM
                    );
                }
                return new SimulationStatusView(
                        Component.literal(this.menu.preheatProgress() + "%"),
                        Component.literal("🔥 Preheating"),
                        Math.max(0.0F, Math.min(1.0F, this.menu.preheatProgress() / 100.0F)),
                        JazzyGuiRenderer.ACCENT_WARM,
                        JazzyGuiRenderer.TEXT,
                        JazzyGuiRenderer.ACCENT_WARM
                );
            }
            if (this.menu.preheatProgress() > 0) {
                float ratio = Math.max(0.0F, Math.min(1.0F, this.menu.preheatProgress() / 100.0F));
                return new SimulationStatusView(
                        Component.literal(this.menu.preheatProgress() + "%"),
                        Component.literal("❄ Cooling"),
                        ratio,
                        JazzyGuiRenderer.TEXT_SOFT,
                        JazzyGuiRenderer.TEXT_SOFT,
                        JazzyGuiRenderer.TEXT_SOFT
                );
            }
        }
        if (this.menu.environmentStatus() == 0) {
            return new SimulationStatusView(
                    Component.translatable("screen.jazzycookin.blocked_short"),
                    this.menu.usesFuelSlot() && !this.menu.getSlot(this.menu.fuelMenuSlotIndex()).hasItem()
                            ? Component.literal("⛽ Fuel")
                            : Component.empty(),
                    0.0F,
                    JazzyGuiRenderer.BLOCKED_TEXT,
                    JazzyGuiRenderer.BLOCKED_TEXT,
                    JazzyGuiRenderer.BLOCKED_TEXT
            );
        }
        if (this.menu.simRecognizerId() > 0) {
            return new SimulationStatusView(
                    this.menu.simulationPreviewName(),
                    Component.translatable("screen.jazzycookin.ready_short"),
                    1.0F,
                    JazzyGuiRenderer.READY_TEXT,
                    JazzyGuiRenderer.READY_TEXT,
                    JazzyGuiRenderer.READY_TEXT
            );
        }
        if (this.menu.environmentStatus() == 1) {
            Component secondary = this.controlDisplayLabel();
            if (secondary.getString().isEmpty() && this.menu.stationType() == StationType.MICROWAVE) {
                secondary = Component.literal(this.menu.microwaveDurationSeconds() + "s");
            }
            return new SimulationStatusView(
                    Component.translatable("screen.jazzycookin.ready_short"),
                    secondary,
                    1.0F,
                    JazzyGuiRenderer.READY_TEXT,
                    JazzyGuiRenderer.TEXT_SOFT,
                    JazzyGuiRenderer.READY_TEXT
            );
        }
        return new SimulationStatusView(
                Component.translatable("screen.jazzycookin.idle_short"),
                this.visibleHelperText(),
                0.0F,
                JazzyGuiRenderer.TEXT_SOFT,
                JazzyGuiRenderer.TEXT_SOFT,
                JazzyGuiRenderer.TEXT_SOFT
        );
    }

    private Component applianceFuelText() {
        if (!this.menu.usesFuelSlot()) {
            return Component.empty();
        }
        int fuelPercent = this.menu.fuelPercent();
        if (fuelPercent > 0 || this.menu.getSlot(this.menu.fuelMenuSlotIndex()).hasItem()) {
            return Component.literal("⛽ " + fuelPercent + "%");
        }
        return Component.literal("⛽ --");
    }

    private int applianceFuelColor() {
        if (!this.menu.usesFuelSlot()) {
            return JazzyGuiRenderer.TEXT_MUTED;
        }
        if (this.menu.fuelBurnTime() > 0) {
            return JazzyGuiRenderer.ACCENT_WARM;
        }
        if (this.menu.getSlot(this.menu.fuelMenuSlotIndex()).hasItem()) {
            return JazzyGuiRenderer.TEXT;
        }
        return JazzyGuiRenderer.TEXT_MUTED;
    }

    private boolean ovenWaitingForFuel() {
        return this.menu.stationType() == StationType.OVEN
                && this.menu.ovenPreheating()
                && this.menu.fuelBurnTime() <= 0
                && !this.menu.getSlot(this.menu.fuelMenuSlotIndex()).hasItem();
    }

    private Component ovenPreheatButtonText() {
        if (!this.menu.ovenPreheating()) {
            return Component.literal("🔥 Preheat");
        }
        if (this.menu.preheatProgress() >= 100) {
            return Component.literal("🔥 Ready");
        }
        if (this.ovenWaitingForFuel()) {
            return Component.literal("⛽ Fuel");
        }
        return Component.literal("🔥 " + this.menu.preheatProgress() + "%");
    }

    private Component ovenCookTimeChipText() {
        if (this.menu.maxProgress() > 0) {
            int remaining = Math.max(0, this.menu.maxProgress() - this.menu.progress());
            return Component.literal("⏱ " + this.formatDurationTicks(remaining) + "/" + this.formatDurationTicks(this.menu.maxProgress()));
        }
        return Component.literal("⏱ " + this.ovenCookTimeMinutes() + "m");
    }

    private Optional<KitchenProcessRecipe> ovenCandidateRecipe() {
        if (this.minecraft == null || this.minecraft.level == null || this.menu.stationType() != StationType.OVEN) {
            return Optional.empty();
        }
        return JazzyRecipes.findProcessRecipeCandidate(this.minecraft.level, StationType.OVEN, this.activeInputStacks(),
                this.menu.getSlot(this.menu.toolMenuSlotIndex()).getItem());
    }

    private List<ItemStack> activeInputStacks() {
        java.util.ArrayList<ItemStack> stacks = new java.util.ArrayList<>();
        for (int slot = 0; slot < this.menu.activeInputCount(); slot++) {
            stacks.add(this.menu.getSlot(slot).getItem());
        }
        return stacks;
    }

    private String formatDurationTicks(int ticks) {
        int totalSeconds = Math.max(0, Math.round(ticks / 20.0F));
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + ":" + String.format("%02d", seconds);
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
        String trimmed = this.fitText(text, maxWidth);
        guiGraphics.drawString(this.font, trimmed, centerX - this.font.width(trimmed) / 2, y, color, shadow);
    }

    private void drawTrimmedLabel(GuiGraphics guiGraphics, Component label, int x, int y, int maxWidth, int color) {
        String text = label.getString();
        if (text.isEmpty()) {
            return;
        }
        String trimmed = this.fitText(text, maxWidth);
        guiGraphics.drawString(this.font, trimmed, x, y, color, false);
    }

    private void drawRightAlignedLabel(GuiGraphics guiGraphics, Component label, int rightX, int y, int maxWidth, int color, boolean shadow) {
        String text = label.getString();
        if (text.isEmpty()) {
            return;
        }
        String trimmed = this.fitText(text, maxWidth);
        guiGraphics.drawString(this.font, trimmed, rightX - this.font.width(trimmed), y, color, shadow);
    }

    private LayoutRegion headerChipBounds(Component label) {
        if (this.menu.currentMethod() == KitchenMethod.NONE || label.getString().isBlank()) {
            return null;
        }
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
        SimulationStatusView statusView = this.simulationStatusView();
        Component helper = this.visibleHelperText();
        if (!this.hasPreviewContent() && !helper.getString().isEmpty()) {
            return helper;
        }
        if (this.menu.simRecognizerId() > 0) {
            return this.menu.simulationPreviewName();
        }
        return statusView.primary();
    }

    private Component previewSubline() {
        SimulationStatusView statusView = this.simulationStatusView();
        if (!this.hasPreviewContent()) {
            return Component.empty();
        }
        if (this.menu.simRecognizerId() > 0) {
            return this.menu.currentMethod().displayName();
        }
        Component secondary = statusView.secondary();
        if (!secondary.getString().isEmpty() && !secondary.getString().equals(statusView.primary().getString())) {
            return secondary;
        }
        return this.visibleHelperText();
    }

    private int previewHeadlineColor() {
        SimulationStatusView statusView = this.simulationStatusView();
        Component helper = this.visibleHelperText();
        if (!helper.getString().isEmpty() && this.previewHeadline().getString().equals(helper.getString())) {
            return JazzyGuiRenderer.TEXT_SOFT;
        }
        return this.menu.simRecognizerId() > 0 ? JazzyGuiRenderer.READY_TEXT : statusView.primaryColor();
    }

    private Component visibleHelperText() {
        if (this.menu.stationType() == StationType.OVEN || this.menu.stationType() == StationType.MICROWAVE) {
            return Component.empty();
        }
        if (!this.hasInputItems() && !this.menu.simulationWorking()) {
            return Component.literal("Add items");
        }
        return Component.empty();
    }

    private Component controlHelperText() {
        if (this.isPanSimulation()) {
            return this.menu.simulationBatchPresent()
                    ? Component.translatable("screen.jazzycookin.helper_pan_actions")
                    : this.primaryActionTooltip();
        }
        return this.primaryActionTooltip();
    }

    private boolean hasPreviewContent() {
        return this.menu.getSlot(this.menu.outputMenuSlotIndex()).hasItem()
                || this.menu.getSlot(this.menu.byproductMenuSlotIndex()).hasItem()
                || this.menu.simRecognizerId() > 0;
    }

    private Component primaryActionLabel() {
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

    private Component appliancePrimaryActionLabel() {
        return Component.literal("▶ ").append(this.primaryActionLabel());
    }

    private int primaryActionButtonId() {
        return 6;
    }

    private boolean primaryActionActive() {
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
        return this.menu.currentMethod() == KitchenMethod.PAN_FRY;
    }

    private float simPanTempRatio() {
        return Math.max(0.0F, Math.min(1.0F, (this.menu.simPanTempF() - 72.0F) / 340.0F));
    }

    private String fitText(String text, int maxWidth) {
        if (text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = this.font.width(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return this.font.plainSubstrByWidth(text, maxWidth);
        }
        String trimmed = this.font.plainSubstrByWidth(text, maxWidth - ellipsisWidth);
        while (!trimmed.isEmpty() && this.font.width(trimmed + ellipsis) > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? this.font.plainSubstrByWidth(text, maxWidth) : trimmed + ellipsis;
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
        if (this.applianceOverlayRoot != null) {
            this.applianceOverlayRoot.renderTree(guiGraphics, mouseX, mouseY, partialTick);
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        this.renderSimulationTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderSimulationTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.usesApplianceUiLib()) {
            if (this.applianceStartAction != null && this.applianceStartAction.isVisible() && this.applianceStartAction.isMouseOver(mouseX, mouseY)) {
                Component tooltip = this.primaryActionTooltip();
                if (!tooltip.getString().isEmpty()) {
                    guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
                }
                return;
            }
            if (this.applianceSecondaryAction != null && this.applianceSecondaryAction.isVisible() && this.applianceSecondaryAction.isMouseOver(mouseX, mouseY)) {
                guiGraphics.renderTooltip(this.font, this.secondaryActionTooltip(), mouseX, mouseY);
                return;
            }
            if (this.applianceTertiaryAction != null && this.applianceTertiaryAction.isVisible() && this.applianceTertiaryAction.isMouseOver(mouseX, mouseY)) {
                guiGraphics.renderTooltip(this.font, this.tertiaryActionTooltip(), mouseX, mouseY);
            }
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
        if (this.usesApplianceUiLib()) {
            boolean wasFocused = this.applianceOvenTemperatureField != null && this.applianceOvenTemperatureField.isFocused();
            boolean handled = this.applianceOverlayRoot != null && this.applianceOverlayRoot.mouseClicked(mouseX, mouseY, button);
            if (!handled) {
                handled = super.mouseClicked(mouseX, mouseY, button);
            }
            if (wasFocused && this.applianceOvenTemperatureField != null
                    && !this.applianceOvenTemperatureField.isMouseOver(mouseX, mouseY)
                    && this.applianceOvenTemperatureField.isFocused()) {
                this.applianceOvenTemperatureField.setFocused(false);
            }
            if (wasFocused && this.applianceOvenTemperatureField != null && !this.applianceOvenTemperatureField.isFocused()) {
                this.commitOvenTemperature();
            }
            return handled;
        }

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
        if (this.usesApplianceUiLib()) {
            boolean handled = this.applianceOverlayRoot != null && this.applianceOverlayRoot.mouseReleased(mouseX, mouseY, button);
            return handled || super.mouseReleased(mouseX, mouseY, button);
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.clearHeldAction();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.usesApplianceUiLib()) {
            if (this.applianceOvenTemperatureField != null
                    && scrollY != 0.0D
                    && (this.applianceOvenTemperatureField.isMouseOver(mouseX, mouseY) || this.applianceOvenTemperatureField.isFocused())) {
                int baseTemperature = this.pendingOvenTemperature > 0 ? this.pendingOvenTemperature : this.menu.ovenTemperature();
                int nextTemperature = baseTemperature + (scrollY > 0.0D ? HeatLevel.OVEN_TEMPERATURE_STEP : -HeatLevel.OVEN_TEMPERATURE_STEP);
                this.applyOvenTemperature(nextTemperature);
                return true;
            }
            if (this.applianceCookTimeChip != null
                    && scrollY != 0.0D
                    && this.applianceCookTimeChip.isMouseOver(mouseX, mouseY)) {
                int stepMinutes = hasShiftDown() ? 5 : 1;
                int nextMinutes = this.ovenCookTimeMinutes() + (scrollY > 0.0D ? stepMinutes : -stepMinutes);
                this.applyOvenCookTimeMinutes(nextMinutes);
                return true;
            }
            if (this.applianceOverlayRoot != null && this.applianceOverlayRoot.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }
        }
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
        if (this.usesApplianceUiLib()) {
            if (this.applianceOvenTemperatureField != null && this.applianceOvenTemperatureField.isFocused()
                    && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
                this.commitOvenTemperature();
                this.applianceOvenTemperatureField.setFocused(false);
                return true;
            }
            if (this.applianceOverlayRoot != null && this.applianceOverlayRoot.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
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
        if (this.usesApplianceUiLib() && this.applianceOverlayRoot != null && this.applianceOverlayRoot.charTyped(codePoint, modifiers)) {
            return true;
        }
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
        if (this.applianceOvenTemperatureField != null && this.applianceOvenTemperatureField.isFocused()) {
            this.commitOvenTemperature();
        }
        if (this.ovenTemperatureBox != null && this.ovenTemperatureBox.isFocused()) {
            this.commitOvenTemperature();
        }
        if (this.microwaveDurationBox != null && this.microwaveDurationBox.isFocused()) {
            this.commitMicrowaveDuration();
        }
        super.removed();
    }
}
