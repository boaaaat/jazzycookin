package com.boaat.jazzy_cookin.recipebook.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.recipebook.JazzyRecipeBookPlanner;
import com.boaat.jazzy_cookin.recipebook.JazzyRecipeBookSelection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class JazzyRecipeBookScreen extends Screen {
    private static final int PANEL_WIDTH = 374;
    private static final int PANEL_HEIGHT = 222;
    private static final int HEADER_HEIGHT = 20;
    private static final int LEFT_WIDTH = 144;
    private static final int ITEM_ROW_HEIGHT = 18;
    private static final int SEARCH_TOP = 38;
    private static final int ITEM_LIST_TOP = 60;
    private static final int ITEM_LIST_BOTTOM = 208;
    private static final int STEP_LIST_TOP = 78;
    private static final int STEP_LIST_BOTTOM = 184;

    private EditBox searchBox;
    private Button pinButton;
    private Button stateBackButton;
    private Button stateForwardButton;
    private Button chainBackButton;
    private Button chainForwardButton;
    private int panelLeft;
    private int panelTop;
    private final List<JazzyRecipeBookPlanner.CatalogEntry> filteredEntries = new ArrayList<>();
    private int itemScroll;
    private int stepScroll;
    private ResourceLocation selectedItemId;
    private IngredientState selectedState;
    private String selectedChainKey = "";

    public JazzyRecipeBookScreen() {
        super(Component.translatable("screen.jazzycookin.recipe_book.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.panelLeft = (this.width - PANEL_WIDTH) / 2;
        this.panelTop = (this.height - PANEL_HEIGHT) / 2;

        this.searchBox = this.addRenderableWidget(new EditBox(
                this.font,
                this.panelLeft + 10,
                this.panelTop + SEARCH_TOP,
                LEFT_WIDTH - 20,
                16,
                Component.translatable("screen.jazzycookin.recipe_book.search")
        ));
        this.searchBox.setResponder(value -> {
            this.itemScroll = 0;
            this.refreshEntries();
        });

        this.stateBackButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.cycleState(-1))
                .bounds(this.panelLeft + LEFT_WIDTH + 18, this.panelTop + 34, 18, 18)
                .build());
        this.stateForwardButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.cycleState(1))
                .bounds(this.panelLeft + PANEL_WIDTH - 28, this.panelTop + 34, 18, 18)
                .build());
        this.chainBackButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.cycleChain(-1))
                .bounds(this.panelLeft + LEFT_WIDTH + 18, this.panelTop + 56, 18, 18)
                .build());
        this.chainForwardButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.cycleChain(1))
                .bounds(this.panelLeft + PANEL_WIDTH - 28, this.panelTop + 56, 18, 18)
                .build());
        this.pinButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> this.togglePin())
                .bounds(this.panelLeft + LEFT_WIDTH + 18, this.panelTop + PANEL_HEIGHT - 30, PANEL_WIDTH - LEFT_WIDTH - 28, 20)
                .build());

        this.refreshEntries();
        this.restoreSelection();
        this.refreshSelection();
    }

    @Override
    public void tick() {
        super.tick();
        this.refreshButtons();
    }

    private void refreshEntries() {
        this.filteredEntries.clear();
        JazzyRecipeBookPlanner planner = RecipeBookClientState.planner();
        if (planner == null) {
            return;
        }

        String query = this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(java.util.Locale.ROOT);
        for (JazzyRecipeBookPlanner.CatalogEntry entry : planner.catalog()) {
            if (query.isBlank() || entry.displayName().toLowerCase(java.util.Locale.ROOT).contains(query)) {
                this.filteredEntries.add(entry);
            }
        }
        if (this.filteredEntries.isEmpty()) {
            this.selectedItemId = null;
            this.selectedState = null;
            this.selectedChainKey = "";
            return;
        }

        if (this.selectedItemId == null || this.filteredEntries.stream().noneMatch(entry -> entry.itemId().equals(this.selectedItemId))) {
            JazzyRecipeBookPlanner.CatalogEntry first = this.filteredEntries.get(0);
            this.selectedItemId = first.itemId();
            this.selectedState = first.producibleStates().get(0);
            this.selectedChainKey = "";
        }
    }

    private void restoreSelection() {
        Optional<JazzyRecipeBookSelection> preferred = RecipeBookClientState.lastViewedSelection();
        if (preferred.isEmpty()) {
            preferred = Optional.ofNullable(RecipeBookClientState.activeSelection());
        }
        preferred.ifPresent(selection -> {
            this.selectedItemId = selection.itemId();
            this.selectedState = selection.state();
            this.selectedChainKey = selection.normalizedChainKey();
        });
    }

    private void refreshSelection() {
        if (this.selectedItemId == null) {
            return;
        }
        JazzyRecipeBookPlanner planner = RecipeBookClientState.planner();
        if (planner == null) {
            return;
        }
        List<IngredientState> states = planner.producibleStates(this.selectedItemId);
        if (states.isEmpty()) {
            this.selectedState = IngredientState.PANTRY_READY;
            this.selectedChainKey = "";
            return;
        }
        if (this.selectedState == null || !states.contains(this.selectedState)) {
            this.selectedState = states.get(0);
        }

        List<JazzyRecipeBookPlanner.Plan> plans = planner.plansFor(this.selectedItemId, this.selectedState);
        if (plans.isEmpty()) {
            this.selectedChainKey = "";
        } else if (plans.stream().noneMatch(plan -> Objects.equals(plan.chainKey(), this.selectedChainKey))) {
            this.selectedChainKey = plans.get(0).chainKey();
        }

        RecipeBookClientState.rememberSelection(new JazzyRecipeBookSelection(this.selectedItemId, this.selectedState, this.selectedChainKey));
        this.stepScroll = 0;
    }

    private void refreshButtons() {
        JazzyRecipeBookPlanner planner = RecipeBookClientState.planner();
        if (planner == null || this.selectedItemId == null || this.selectedState == null) {
            this.pinButton.active = false;
            return;
        }

        List<IngredientState> states = planner.producibleStates(this.selectedItemId);
        this.stateBackButton.visible = this.stateForwardButton.visible = states.size() > 1;
        List<JazzyRecipeBookPlanner.Plan> plans = planner.plansFor(this.selectedItemId, this.selectedState);
        this.chainBackButton.visible = this.chainForwardButton.visible = plans.size() > 1;
        this.pinButton.active = !plans.isEmpty();
        this.pinButton.setMessage(this.isPinnedSelection()
                ? Component.translatable("screen.jazzycookin.recipe_book.unpin")
                : Component.translatable("screen.jazzycookin.recipe_book.pin"));
    }

    private void cycleState(int delta) {
        if (this.selectedItemId == null) {
            return;
        }
        JazzyRecipeBookPlanner planner = RecipeBookClientState.planner();
        if (planner == null) {
            return;
        }
        List<IngredientState> states = planner.producibleStates(this.selectedItemId);
        if (states.size() <= 1) {
            return;
        }
        int index = Math.max(0, states.indexOf(this.selectedState));
        this.selectedState = states.get(Math.floorMod(index + delta, states.size()));
        this.selectedChainKey = "";
        this.refreshSelection();
    }

    private void cycleChain(int delta) {
        List<JazzyRecipeBookPlanner.Plan> plans = this.availablePlans();
        if (plans.size() <= 1) {
            return;
        }
        int index = 0;
        for (int i = 0; i < plans.size(); i++) {
            if (Objects.equals(plans.get(i).chainKey(), this.selectedChainKey)) {
                index = i;
                break;
            }
        }
        this.selectedChainKey = plans.get(Math.floorMod(index + delta, plans.size())).chainKey();
        this.refreshSelection();
    }

    private void togglePin() {
        if (this.selectedItemId == null || this.selectedState == null) {
            return;
        }
        JazzyRecipeBookSelection selection = new JazzyRecipeBookSelection(this.selectedItemId, this.selectedState, this.selectedChainKey);
        if (this.isPinnedSelection()) {
            RecipeBookClientState.unpinSelection();
        } else {
            RecipeBookClientState.pinSelection(selection);
        }
    }

    private boolean isPinnedSelection() {
        JazzyRecipeBookSelection activeSelection = RecipeBookClientState.activeSelection();
        if (activeSelection == null || this.selectedItemId == null || this.selectedState == null) {
            return false;
        }
        return activeSelection.itemId().equals(this.selectedItemId)
                && activeSelection.state() == this.selectedState
                && activeSelection.normalizedChainKey().equals(this.selectedChainKey);
    }

    private List<JazzyRecipeBookPlanner.Plan> availablePlans() {
        if (this.selectedItemId == null || this.selectedState == null) {
            return List.of();
        }
        JazzyRecipeBookPlanner planner = RecipeBookClientState.planner();
        return planner == null ? List.of() : planner.plansFor(this.selectedItemId, this.selectedState);
    }

    private Optional<JazzyRecipeBookPlanner.Plan> currentPlan() {
        return this.availablePlans().stream().filter(plan -> Objects.equals(plan.chainKey(), this.selectedChainKey)).findFirst();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.clickedItem(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isMouseOverItemList(mouseX, mouseY)) {
            int visibleRows = (ITEM_LIST_BOTTOM - ITEM_LIST_TOP) / ITEM_ROW_HEIGHT;
            int maxScroll = Math.max(0, this.filteredEntries.size() - visibleRows);
            this.itemScroll = Math.max(0, Math.min(maxScroll, this.itemScroll + (scrollY > 0 ? -1 : 1)));
            return true;
        }
        if (this.isMouseOverStepList(mouseX, mouseY)) {
            int totalSteps = this.currentPlan().map(plan -> plan.steps().size()).orElse(0);
            int visibleRows = Math.max(1, (STEP_LIST_BOTTOM - STEP_LIST_TOP) / 22);
            int maxScroll = Math.max(0, totalSteps - visibleRows);
            this.stepScroll = Math.max(0, Math.min(maxScroll, this.stepScroll + (scrollY > 0 ? -1 : 1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean clickedItem(double mouseX, double mouseY) {
        if (!this.isMouseOverItemList(mouseX, mouseY)) {
            return false;
        }
        int relativeY = (int) mouseY - (this.panelTop + ITEM_LIST_TOP);
        int row = relativeY / ITEM_ROW_HEIGHT;
        int index = this.itemScroll + row;
        if (index < 0 || index >= this.filteredEntries.size()) {
            return false;
        }
        JazzyRecipeBookPlanner.CatalogEntry entry = this.filteredEntries.get(index);
        this.selectedItemId = entry.itemId();
        this.selectedState = entry.producibleStates().get(0);
        this.selectedChainKey = "";
        this.refreshSelection();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox != null && this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox != null && this.searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x70070A0E);
        guiGraphics.fill(this.panelLeft, this.panelTop, this.panelLeft + PANEL_WIDTH, this.panelTop + PANEL_HEIGHT, 0xF0181B20);
        guiGraphics.fill(this.panelLeft, this.panelTop, this.panelLeft + PANEL_WIDTH, this.panelTop + HEADER_HEIGHT, 0xFF7A5C32);
        guiGraphics.fill(this.panelLeft + LEFT_WIDTH, this.panelTop + HEADER_HEIGHT, this.panelLeft + LEFT_WIDTH + 1, this.panelTop + PANEL_HEIGHT, 0xFF2A313B);
        guiGraphics.drawString(this.font, this.title, this.panelLeft + 10, this.panelTop + 6, 0xFFF3E9DB, false);

        this.renderItemList(guiGraphics, mouseX, mouseY);
        this.renderDetails(guiGraphics, mouseX, mouseY);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderItemList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.recipe_book.catalog"), this.panelLeft + 10, this.panelTop + 24, 0xFFC7CDD6, false);
        int visibleRows = (ITEM_LIST_BOTTOM - ITEM_LIST_TOP) / ITEM_ROW_HEIGHT;
        for (int row = 0; row < visibleRows; row++) {
            int index = this.itemScroll + row;
            if (index >= this.filteredEntries.size()) {
                break;
            }
            JazzyRecipeBookPlanner.CatalogEntry entry = this.filteredEntries.get(index);
            int y = this.panelTop + ITEM_LIST_TOP + row * ITEM_ROW_HEIGHT;
            boolean selected = entry.itemId().equals(this.selectedItemId);
            int bg = selected ? 0xFF36404C : 0x66212730;
            if (this.isMouseOverItemList(mouseX, mouseY) && this.clickedRow(mouseY) == row) {
                bg = 0xFF3E4855;
            }
            guiGraphics.fill(this.panelLeft + 8, y, this.panelLeft + LEFT_WIDTH - 8, y + ITEM_ROW_HEIGHT - 2, bg);
            ItemStack icon = new ItemStack(entry.item());
            guiGraphics.renderItem(icon, this.panelLeft + 12, y + 1);
            guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(entry.displayName(), LEFT_WIDTH - 34), this.panelLeft + 32, y + 5, 0xFFF1F4F8, false);
        }
    }

    private void renderDetails(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = this.panelLeft + LEFT_WIDTH + 10;
        int y = this.panelTop + 24;
        if (this.selectedItemId == null) {
            guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.recipe_book.empty"), x, y, 0xFFB6BCC6, false);
            return;
        }

        ItemStack stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(this.selectedItemId));
        guiGraphics.renderItem(stack, x, y);
        guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(stack.getHoverName().getString(), PANEL_WIDTH - LEFT_WIDTH - 46), x + 20, y + 4, 0xFFF5F5F1, false);

        if (this.selectedState != null) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.jazzycookin.recipe_book.state", Component.translatable("state.jazzycookin." + this.selectedState.getSerializedName())),
                    x + 22,
                    this.panelTop + 38,
                    0xFFCFD6DE,
                    false
            );
        }

        if (!this.availablePlans().isEmpty()) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.jazzycookin.recipe_book.path", RecipeBookClientState.chainLabel(this.selectedChainKey)),
                    x + 22,
                    this.panelTop + 60,
                    0xFFCFD6DE,
                    false
            );
        }

        guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.recipe_book.steps"), x, this.panelTop + 78 - 12, 0xFFC7CDD6, false);
        guiGraphics.fill(x, this.panelTop + STEP_LIST_TOP, this.panelLeft + PANEL_WIDTH - 10, this.panelTop + STEP_LIST_BOTTOM, 0x661A2027);

        Optional<JazzyRecipeBookPlanner.Plan> optionalPlan = this.currentPlan();
        if (optionalPlan.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.recipe_book.no_path"), x + 6, this.panelTop + STEP_LIST_TOP + 8, 0xFFB4BBC4, false);
            return;
        }

        JazzyRecipeBookPlanner.Plan plan = optionalPlan.get();
        Set<String> completed = this.isPinnedSelection() ? RecipeBookClientState.completedStepIds() : Set.of();
        int rowY = this.panelTop + STEP_LIST_TOP + 6;
        int visibleRows = Math.max(1, (STEP_LIST_BOTTOM - STEP_LIST_TOP) / 22);
        for (int row = 0; row < visibleRows; row++) {
            int index = this.stepScroll + row;
            if (index >= plan.steps().size()) {
                break;
            }
            JazzyRecipeBookPlanner.PlanStep step = plan.steps().get(index);
            boolean complete = completed.contains(step.id());
            int cardTop = rowY + row * 22;
            guiGraphics.fill(x + 4, cardTop, this.panelLeft + PANEL_WIDTH - 14, cardTop + 18, complete ? 0xAA254F37 : 0xAA28303A);
            guiGraphics.drawString(this.font, Component.literal((index + 1) + "."), x + 8, cardTop + 5, complete ? 0xFF8AD79E : 0xFFE9D7B6, false);
            guiGraphics.renderItem(step.outputStack(), x + 20, cardTop + 1);
            guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(step.outputStack().getHoverName().getString(), PANEL_WIDTH - LEFT_WIDTH - 72),
                    x + 40, cardTop + 2, 0xFFF0F2F5, false);
            if (!step.options().isEmpty()) {
                String detail = this.stepDetail(step.options().get(0));
                guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(detail, PANEL_WIDTH - LEFT_WIDTH - 72), x + 40, cardTop + 10, 0xFFB6BEC8, false);
            }
        }
    }

    private String stepDetail(JazzyRecipeBookPlanner.StepOption option) {
        if (option.station() != null) {
            return option.station().displayName().getString();
        }
        if (!option.requirements().isEmpty()) {
            ItemStack firstChoice = option.requirements().get(0).choices().isEmpty() ? ItemStack.EMPTY : option.requirements().get(0).choices().get(0);
            return firstChoice.isEmpty() ? Component.translatable("screen.jazzycookin.recipe_book.materials").getString() : firstChoice.getHoverName().getString();
        }
        if (!option.notes().isEmpty()) {
            return option.notes().get(0);
        }
        return Component.translatable("screen.jazzycookin.recipe_book.step").getString();
    }

    private boolean isMouseOverItemList(double mouseX, double mouseY) {
        return mouseX >= this.panelLeft + 8 && mouseX < this.panelLeft + LEFT_WIDTH - 8
                && mouseY >= this.panelTop + ITEM_LIST_TOP && mouseY < this.panelTop + ITEM_LIST_BOTTOM;
    }

    private boolean isMouseOverStepList(double mouseX, double mouseY) {
        int x = this.panelLeft + LEFT_WIDTH + 10;
        return mouseX >= x && mouseX < this.panelLeft + PANEL_WIDTH - 10
                && mouseY >= this.panelTop + STEP_LIST_TOP && mouseY < this.panelTop + STEP_LIST_BOTTOM;
    }

    private int clickedRow(double mouseY) {
        return ((int) mouseY - (this.panelTop + ITEM_LIST_TOP)) / ITEM_ROW_HEIGHT;
    }
}
