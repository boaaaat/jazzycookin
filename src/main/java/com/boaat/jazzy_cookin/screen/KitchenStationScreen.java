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
    private ApplianceUiLibComponents.ApplianceTextField applianceOvenTemperatureField;
    private ApplianceUiLibComponents.StoveDialWidget applianceStoveDial;
    private ApplianceUiLibComponents.ApplianceStatusChip appliancePreheatChip;
    private ApplianceUiLibComponents.ApplianceStatusChip applianceCookTimeChip;
    private int pendingStoveDialLevel = -1;
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
        this.applianceOvenTemperatureField = null;
        this.applianceStoveDial = null;
        this.appliancePreheatChip = null;
        this.applianceCookTimeChip = null;
        this.pendingStoveDialLevel = -1;
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

        LayoutRegion inventory = this.applianceInventoryRegion();
        this.applianceInventoryLabel = new ApplianceUiLibComponents.ApplianceLabel((this.imageWidth - 96) / 2, inventory.y() + 10,
                96, 12, this.playerInventoryTitle, ApplianceUiLibComponents.LabelAlign.CENTER);
        this.applianceInventoryLabel.setColor(JazzyGuiRenderer.TEXT);
        this.applianceOverlayRoot.addChild(this.applianceInventoryLabel);

        LayoutRegion control = this.applianceControlRegion();
        if (this.menu.stationType() == StationType.STOVE) {
            ApplianceUiLibComponents.ApplianceLabel dialLabel = new ApplianceUiLibComponents.ApplianceLabel(control.x() + 12,
                    control.y() + 12, 30, 12, Component.translatable("screen.jazzycookin.stove_dial"),
                    ApplianceUiLibComponents.LabelAlign.LEFT);
            dialLabel.setColor(JazzyGuiRenderer.TEXT_MUTED);
            this.applianceOverlayRoot.addChild(dialLabel);

            this.applianceStoveDial = new ApplianceUiLibComponents.StoveDialWidget(control.x() + 44, control.y() + 7, 30, 20,
                    this.profile.theme(), this.menu.stoveDialLevel(), value -> {
                        this.pendingStoveDialLevel = value;
                        this.sendButton(3000 + value);
                    });
            this.applianceOverlayRoot.addChild(this.applianceStoveDial);

            int startWidth = 72;
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
            this.applianceTemperatureLabel = new ApplianceUiLibComponents.ApplianceLabel(control.x() + 10, control.y() + 12,
                    66, 12, Component.translatable("screen.jazzycookin.temperature_full"),
                    ApplianceUiLibComponents.LabelAlign.LEFT);
            this.applianceTemperatureLabel.setColor(JazzyGuiRenderer.TEXT_MUTED);
            this.applianceOverlayRoot.addChild(this.applianceTemperatureLabel);

            this.applianceOvenTemperatureField = new ApplianceUiLibComponents.ApplianceTextField(control.x() + 82, control.y() + 8,
                    42, 18, Integer.toString(this.menu.ovenTemperature()));
            this.applianceOvenTemperatureField.setMaxLength(3);
            this.applianceOvenTemperatureField.setTextPredicate(ApplianceUiLibComponents.ApplianceTextField.INTEGER_INPUT_PREDICATE);
            this.applianceOvenTemperatureField.setTextColor(JazzyGuiRenderer.TEXT);
            this.applianceOvenTemperatureField.setTextColorUneditable(JazzyGuiRenderer.TEXT);
            this.applianceOverlayRoot.addChild(this.applianceOvenTemperatureField);

            this.applianceTemperatureSuffixLabel = new ApplianceUiLibComponents.ApplianceLabel(control.x() + 128, control.y() + 12,
                    12, 12, Component.literal("F"), ApplianceUiLibComponents.LabelAlign.LEFT);
            this.applianceTemperatureSuffixLabel.setColor(JazzyGuiRenderer.TEXT_MUTED);
            this.applianceOverlayRoot.addChild(this.applianceTemperatureSuffixLabel);

            int startWidth = 68;
            int cookWidth = 86;
            int preheatWidth = 74;
            int startX = control.right() - startWidth - 8;
            int cookX = startX - cookWidth - 6;
            int preheatX = cookX - preheatWidth - 6;

            this.appliancePreheatChip = new ApplianceUiLibComponents.ApplianceStatusChip(preheatX, control.y() + 7,
                    preheatWidth, 20, this.profile.theme(), Component.empty());
            this.applianceOverlayRoot.addChild(this.appliancePreheatChip);

            this.applianceCookTimeChip = new ApplianceUiLibComponents.ApplianceStatusChip(cookX, control.y() + 7,
                    cookWidth, 20, this.profile.theme(), Component.empty());
            this.applianceOverlayRoot.addChild(this.applianceCookTimeChip);

            this.applianceStartAction = new ApplianceUiLibComponents.ApplianceButton(startX, control.y() + 7, startWidth, 20,
                    this.profile.theme(), Component.translatable("screen.jazzycookin.start"),
                    () -> this.sendButton(this.primaryActionButtonId()));
            this.applianceOverlayRoot.addChild(this.applianceStartAction);
        }
    }

    private LayoutRegion applianceWorkspaceRegion() {
        return new LayoutRegion(18, 42, 190, 92);
    }

    private LayoutRegion appliancePreviewRegion() {
        return new LayoutRegion(this.imageWidth - 120, 42, 102, 82);
    }

    private LayoutRegion applianceStatusRegion() {
        return null;
    }

    private LayoutRegion applianceControlRegion() {
        return new LayoutRegion(18, 130, this.imageWidth - 36, 34);
    }

    private LayoutRegion applianceInventoryRegion() {
        return new LayoutRegion(14, this.imageHeight - 106, this.imageWidth - 28, 96);
    }

    private LayoutRegion applianceRecipeButtonRegion() {
        int width = Math.max(84, this.font.width(Component.translatable("screen.jazzycookin.recipe_book_short")) + 20);
        return new LayoutRegion(this.imageWidth - width - 12, 8, width, 20);
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
        this.syncStoveDialWidget();
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
        if (this.applianceStartAction != null) {
            this.applianceStartAction.setMessage(this.primaryActionLabel());
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
            this.applianceStatusPrimaryLabel.setText(this.primaryStatusText());
            this.applianceStatusPrimaryLabel.setColor(this.previewHeadlineColor());
        }
        if (this.applianceStatusSecondaryLabel != null) {
            Component secondary = this.secondaryStatusText();
            this.applianceStatusSecondaryLabel.setVisible(!secondary.getString().isEmpty());
            this.applianceStatusSecondaryLabel.setText(secondary);
            this.applianceStatusSecondaryLabel.setColor(this.secondaryStatusColor());
        }
        if (this.appliancePreheatChip != null) {
            this.appliancePreheatChip.setText(this.ovenPreheatChipText());
            this.appliancePreheatChip.setTextColor(this.secondaryStatusColor());
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

    private void syncStoveDialWidget() {
        if (this.applianceStoveDial == null) {
            return;
        }
        int syncedDial = this.menu.stoveDialLevel();
        if (this.pendingStoveDialLevel == syncedDial) {
            this.pendingStoveDialLevel = -1;
        }
        int displayDial = this.pendingStoveDialLevel > 0 ? this.pendingStoveDialLevel : syncedDial;
        if (displayDial > 0 && this.applianceStoveDial.dialValue() != displayDial) {
            this.applianceStoveDial.setDialValue(displayDial);
        }
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

        if (this.menu.simulationMode()) {
            this.renderSimulationMetrics(guiGraphics, left, top);
        } else {
            this.renderLegacyMetricPanel(guiGraphics, left, top);
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
        JazzyGuiRenderer.drawInventoryShelf(guiGraphics, left, top, inventory, theme);

        guiGraphics.fill(left + preview.x() + 20, top + preview.y() + 30, left + preview.right() - 20, top + preview.y() + 31, 0x44231A13);
        guiGraphics.fill(left + preview.x() + 20, top + preview.y() + 58, left + preview.right() - 20, top + preview.y() + 59, 0x44231A13);

        if (this.menu.stationType() == StationType.STOVE) {
            guiGraphics.fill(left + workspace.x() + 14, top + workspace.y() + 18, left + workspace.right() - 14, top + workspace.bottom() - 14, 0x22130F0B);
            for (int inputIndex = 0; inputIndex < this.menu.activeInputCount(); inputIndex++) {
                Slot slot = this.menu.getSlot(inputIndex);
                guiGraphics.fill(left + slot.x - 6, top + slot.y - 6, left + slot.x + 22, top + slot.y + 22, 0x331B1510);
            }
            Slot toolSlot = this.menu.getSlot(this.menu.toolMenuSlotIndex());
            guiGraphics.fill(left + toolSlot.x - 8, top + toolSlot.y - 8, left + toolSlot.x + 24, top + toolSlot.y + 24, 0x33211610);
        } else {
            guiGraphics.fill(left + workspace.x() + 14, top + workspace.y() + 14, left + workspace.right() - 14, top + workspace.bottom() - 14, 0x22110D0A);
            for (int rail = 0; rail < 4; rail++) {
                int railY = top + workspace.y() + 24 + rail * 16;
                guiGraphics.fill(left + workspace.x() + 18, railY, left + workspace.right() - 18, railY + 2, 0x44372B20);
            }
            guiGraphics.fill(left + workspace.x() + 28, top + workspace.bottom() - 22, left + workspace.right() - 28, top + workspace.bottom() - 16, 0x33221610);
            Slot toolSlot = this.menu.getSlot(this.menu.toolMenuSlotIndex());
            guiGraphics.fill(left + toolSlot.x - 8, top + toolSlot.y - 8, left + toolSlot.x + 24, top + toolSlot.y + 24, 0x33211610);
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

    private void renderLegacyMetricPanel(GuiGraphics guiGraphics, int left, int top) {
        LayoutRegion lane = this.layout.statusLaneRegion();
        if (lane.width() <= 0 || lane.height() <= 0) {
            return;
        }

        Component primary = this.primaryStatusText();
        Component secondary = this.secondaryStatusText();
        boolean showSecondary = !secondary.getString().isEmpty()
                && !secondary.getString().equals(primary.getString())
                && lane.width() >= 72;
        boolean showBar = lane.height() >= 12;
        int inset = 4;
        int textY = top + lane.y() + (showBar ? 0 : Math.max(0, (lane.height() - 8) / 2));

        if (showSecondary) {
            int halfWidth = Math.max(18, (lane.width() - inset * 2 - 6) / 2);
            this.drawTrimmedLabel(guiGraphics, primary, left + lane.x() + inset, textY, halfWidth, this.previewHeadlineColor());
            this.drawRightAlignedLabel(guiGraphics, secondary, left + lane.right() - inset, textY, halfWidth, this.secondaryStatusColor(), false);
        } else {
            this.drawCenteredTrimmedLabel(guiGraphics, primary, left + lane.centerX(), textY, lane.width() - inset * 2, this.previewHeadlineColor(), false);
        }

        if (showBar) {
            int barX = left + lane.x() + inset;
            int barY = top + lane.bottom() - 3;
            int barWidth = Math.max(14, lane.width() - inset * 2);
            guiGraphics.fill(barX, barY, barX + barWidth, barY + 2, 0xFF1A1F27);
            int fillWidth = Math.round(barWidth * this.legacyStatusRatio());
            if (fillWidth > 0) {
                guiGraphics.fill(barX, barY, barX + fillWidth, barY + 2, this.legacyStatusFillColor());
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

    private Component ovenPreheatChipText() {
        String prefix = Component.translatable("screen.jazzycookin.preheat_short").getString();
        if (this.menu.preheatProgress() >= 100) {
            return Component.literal(prefix + " " + Component.translatable("screen.jazzycookin.ready_short").getString());
        }
        return Component.literal(prefix + " " + this.menu.preheatProgress() + "%");
    }

    private Component ovenCookTimeChipText() {
        String prefix = Component.translatable("screen.jazzycookin.cook_time").getString();
        if (this.menu.maxProgress() > 0) {
            int remaining = Math.max(0, this.menu.maxProgress() - this.menu.progress());
            return Component.literal(prefix + " " + this.formatDurationTicks(remaining) + "/" + this.formatDurationTicks(this.menu.maxProgress()));
        }
        return this.ovenCandidateRecipe()
                .map(recipe -> Component.literal(prefix + " " + this.formatDurationTicks(recipe.effectiveDuration())))
                .orElse(Component.literal(prefix + " --"));
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
        int seconds = Math.max(1, Math.round(ticks / 20.0F));
        return seconds + "s";
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
        Component helper = this.visibleHelperText();
        if (!this.hasPreviewContent() && !helper.getString().isEmpty()) {
            return helper;
        }
        if (this.menu.simulationMode()) {
            if (this.menu.simRecognizerId() > 0) {
                return this.menu.simulationPreviewName();
            }
            return this.primaryStatusText();
        }
        return this.primaryStatusText();
    }

    private Component previewSubline() {
        if (!this.hasPreviewContent()) {
            return Component.empty();
        }
        if (this.menu.simulationMode()) {
            if (this.menu.simRecognizerId() > 0) {
                return this.menu.currentMethod().displayName();
            }
            Component helper = this.visibleHelperText();
            if (!helper.getString().isEmpty()) {
                return helper;
            }
            return this.menu.currentMethod().displayName();
        }
        Component secondary = this.secondaryStatusText();
        if (!secondary.getString().isEmpty() && !secondary.getString().equals(this.primaryStatusText().getString())) {
            return secondary;
        }
        return this.visibleHelperText();
    }

    private int previewHeadlineColor() {
        Component helper = this.visibleHelperText();
        if (!helper.getString().isEmpty() && this.previewHeadline().getString().equals(helper.getString())) {
            return JazzyGuiRenderer.TEXT_SOFT;
        }
        if (this.menu.simulationMode()) {
            return this.menu.simRecognizerId() > 0 ? JazzyGuiRenderer.READY_TEXT : JazzyGuiRenderer.TEXT;
        }
        return this.secondaryStatusColor();
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
        return this.menu.simulationMode() ? this.primaryActionTooltip() : Component.translatable("screen.jazzycookin.helper_action_ready");
    }

    private boolean hasPreviewContent() {
        return this.menu.getSlot(this.menu.outputMenuSlotIndex()).hasItem()
                || this.menu.getSlot(this.menu.byproductMenuSlotIndex()).hasItem()
                || this.menu.simRecognizerId() > 0;
    }

    private float legacyStatusRatio() {
        if (this.menu.maxProgress() > 0) {
            return Math.max(0.0F, Math.min(1.0F, this.menu.progress() / (float) this.menu.maxProgress()));
        }
        if (this.menu.stationType() == StationType.OVEN) {
            return Math.max(0.0F, Math.min(1.0F, this.menu.preheatProgress() / 100.0F));
        }
        return this.menu.environmentStatus() == 1 ? 1.0F : 0.0F;
    }

    private int legacyStatusFillColor() {
        if (this.menu.maxProgress() > 0) {
            return JazzyGuiRenderer.ACCENT;
        }
        return this.secondaryStatusColor();
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
        if (!this.menu.simulationMode()) {
            return;
        }
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
