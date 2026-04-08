package com.boaat.jazzy_cookin.recipebook.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.recipebook.JazzyRecipeBookPlanner;
import com.boaat.jazzy_cookin.recipebook.JazzyRecipeBookSelection;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class JazzyRecipeBookScreen extends Screen {
    private record VisibleStep(JazzyRecipeBookPlanner.PlanStep step, int depth, boolean expanded) {
        public boolean expandable() {
            return this.step.expandable();
        }
    }

    private static final int DESIGN_PANEL_WIDTH = 374;
    private static final int DESIGN_PANEL_HEIGHT = 222;
    private static final int HEADER_HEIGHT = 20;
    private static final int DESIGN_LEFT_WIDTH = 144;
    private static final int ITEM_ROW_HEIGHT = 18;
    private static final int SEARCH_TOP = 38;
    private static final int ITEM_LIST_TOP = 60;
    private static final int STEP_LIST_TOP = 78;
    private static final int STEP_ROW_HEIGHT = 24;
    private static final int STEP_INDENT = 12;
    private static final int STEP_EXPAND_SIZE = 10;

    private EditBox searchBox;
    private Button pinButton;
    private Button stateBackButton;
    private Button stateForwardButton;
    private Button chainBackButton;
    private Button chainForwardButton;
    private int panelLeft;
    private int panelTop;
    private int panelW;
    private int panelH;
    private int leftW;
    private int itemListBottom;
    private int stepListBottom;
    private final List<JazzyRecipeBookPlanner.CatalogEntry> filteredEntries = new ArrayList<>();
    private final Map<String, Integer> catalogNameCounts = new HashMap<>();
    private final Map<String, Boolean> expandedSteps = new HashMap<>();
    private int itemScroll;
    private int stepScroll;
    private ResourceLocation selectedItemId;
    private IngredientState selectedState;
    private String selectedChainKey = "";
    private String selectedStepId = "";
    private String displayedPlanKey = "";

    public JazzyRecipeBookScreen() {
        super(Component.translatable("screen.jazzycookin.recipe_book.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.panelW = Math.min(DESIGN_PANEL_WIDTH, this.width - 8);
        this.panelH = Math.min(DESIGN_PANEL_HEIGHT, this.height - 8);
        this.leftW = this.panelW * DESIGN_LEFT_WIDTH / DESIGN_PANEL_WIDTH;
        this.panelLeft = Math.max(4, (this.width - this.panelW) / 2);
        this.panelTop = Math.max(4, (this.height - this.panelH) / 2);
        this.itemListBottom = Math.max(ITEM_LIST_TOP + ITEM_ROW_HEIGHT, this.panelH - 14);
        this.stepListBottom = Math.max(STEP_LIST_TOP + STEP_ROW_HEIGHT, this.panelH - 38);

        this.searchBox = this.addRenderableWidget(new EditBox(
                this.font,
                this.panelLeft + 10,
                this.panelTop + SEARCH_TOP,
                this.leftW - 20,
                16,
                Component.translatable("screen.jazzycookin.recipe_book.search")
        ));
        this.searchBox.setResponder(value -> {
            this.itemScroll = 0;
            this.refreshEntries();
            this.refreshSelection();
        });

        this.stateBackButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.cycleState(-1))
                .bounds(this.panelLeft + this.leftW + 18, this.panelTop + 34, 18, 18)
                .build());
        this.stateForwardButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.cycleState(1))
                .bounds(this.panelLeft + this.panelW - 28, this.panelTop + 34, 18, 18)
                .build());
        this.chainBackButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.cycleChain(-1))
                .bounds(this.panelLeft + this.leftW + 18, this.panelTop + 56, 18, 18)
                .build());
        this.chainForwardButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.cycleChain(1))
                .bounds(this.panelLeft + this.panelW - 28, this.panelTop + 56, 18, 18)
                .build());
        this.pinButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> this.togglePin())
                .bounds(this.panelLeft + this.leftW + 18, this.panelTop + this.panelH - 30, this.panelW - this.leftW - 28, 20)
                .build());

        this.refreshEntries();
        this.restoreSelection();
        this.refreshSelection();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isPinnedSelection()) {
            String focusedStepId = RecipeBookClientState.focusedStepId();
            if (focusedStepId != null && !focusedStepId.equals(this.selectedStepId)) {
                this.selectedStepId = focusedStepId;
                this.currentPlan().ifPresent(plan -> {
                    this.ensureExpandedPath(plan, focusedStepId);
                    this.ensureSelectedStepVisible();
                });
            }
        }
        this.refreshButtons();
    }

    private void refreshEntries() {
        this.filteredEntries.clear();
        this.catalogNameCounts.clear();
        JazzyRecipeBookPlanner planner = RecipeBookClientState.planner();
        if (planner == null) {
            return;
        }

        for (JazzyRecipeBookPlanner.CatalogEntry entry : planner.catalog()) {
            this.catalogNameCounts.merge(entry.displayName().toLowerCase(Locale.ROOT), 1, Integer::sum);
        }

        String query = this.searchQuery();
        for (JazzyRecipeBookPlanner.CatalogEntry entry : planner.catalog()) {
            if (query.isBlank() || this.catalogMatches(entry, query)) {
                this.filteredEntries.add(entry);
            }
        }
        if (this.filteredEntries.isEmpty()) {
            this.selectedItemId = null;
            this.selectedState = null;
            this.selectedChainKey = "";
            this.selectedStepId = "";
            this.displayedPlanKey = "";
            return;
        }

        if (this.selectedItemId == null || this.filteredEntries.stream().noneMatch(entry -> entry.itemId().equals(this.selectedItemId))) {
            JazzyRecipeBookPlanner.CatalogEntry first = this.filteredEntries.get(0);
            this.selectedItemId = first.itemId();
            this.selectedState = first.producibleStates().get(0);
            this.selectedChainKey = "";
        }
    }

    private String searchQuery() {
        return this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
    }

    private boolean catalogMatches(JazzyRecipeBookPlanner.CatalogEntry entry, String query) {
        String normalizedPath = entry.itemId().getPath().replace('_', ' ').toLowerCase(Locale.ROOT);
        String normalizedDisplay = entry.displayName().toLowerCase(Locale.ROOT);
        return normalizedDisplay.contains(query) || normalizedPath.contains(query);
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
            this.selectedStepId = "";
            this.expandedSteps.clear();
            this.displayedPlanKey = "";
            return;
        }
        if (this.selectedState == null || !states.contains(this.selectedState)) {
            this.selectedState = states.get(0);
        }

        List<JazzyRecipeBookPlanner.Plan> plans = planner.plansFor(this.selectedItemId, this.selectedState);
        if (plans.isEmpty()) {
            this.selectedChainKey = "";
            this.selectedStepId = "";
            this.expandedSteps.clear();
            this.displayedPlanKey = "";
        } else if (plans.stream().noneMatch(plan -> Objects.equals(plan.chainKey(), this.selectedChainKey))) {
            this.selectedChainKey = plans.get(0).chainKey();
        }

        RecipeBookClientState.rememberSelection(new JazzyRecipeBookSelection(this.selectedItemId, this.selectedState, this.selectedChainKey));

        Optional<JazzyRecipeBookPlanner.Plan> optionalPlan = this.currentPlan();
        String planKey = this.planKey();
        if (optionalPlan.isEmpty()) {
            this.displayedPlanKey = "";
            this.selectedStepId = "";
            this.expandedSteps.clear();
            this.stepScroll = 0;
            return;
        }

        JazzyRecipeBookPlanner.Plan plan = optionalPlan.get();
        if (!planKey.equals(this.displayedPlanKey)) {
            this.displayedPlanKey = planKey;
            this.expandedSteps.clear();
            this.stepScroll = 0;
        }
        if (this.selectedStepId.isBlank() || plan.step(this.selectedStepId).isEmpty()) {
            this.selectedStepId = this.defaultSelectedStepId(plan);
        }
        this.ensureExpandedPath(plan, this.selectedStepId);
        this.ensureSelectedStepVisible();
    }

    private void refreshButtons() {
        if (this.pinButton == null || this.stateBackButton == null || this.stateForwardButton == null || this.chainBackButton == null || this.chainForwardButton == null) {
            return;
        }

        JazzyRecipeBookPlanner planner = RecipeBookClientState.planner();
        if (planner == null || this.selectedItemId == null || this.selectedState == null) {
            this.stateBackButton.visible = false;
            this.stateForwardButton.visible = false;
            this.chainBackButton.visible = false;
            this.chainForwardButton.visible = false;
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
        this.selectedStepId = "";
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
        this.selectedStepId = "";
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
            RecipeBookClientState.pinSelection(selection, this.selectedStepId);
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

    private String planKey() {
        if (this.selectedItemId == null || this.selectedState == null) {
            return "";
        }
        return this.selectedItemId + "|" + this.selectedState.getSerializedName() + "|" + this.selectedChainKey;
    }

    private Set<String> currentCompletedStepIds() {
        return this.isPinnedSelection() ? RecipeBookClientState.completedStepIds() : Set.of();
    }

    private String defaultSelectedStepId(JazzyRecipeBookPlanner.Plan plan) {
        String preferredStepId = this.isPinnedSelection()
                ? Objects.requireNonNullElse(RecipeBookClientState.focusedStepId(), "")
                : this.selectedStepId;
        JazzyRecipeBookPlanner.PlanStep step = plan.focusedStep(this.currentCompletedStepIds(), preferredStepId);
        if (step != null) {
            return step.id();
        }
        return plan.rootStepId();
    }

    private List<VisibleStep> currentVisibleSteps() {
        Optional<JazzyRecipeBookPlanner.Plan> optionalPlan = this.currentPlan();
        if (optionalPlan.isEmpty()) {
            return List.of();
        }

        JazzyRecipeBookPlanner.Plan plan = optionalPlan.get();
        List<VisibleStep> visibleSteps = new ArrayList<>();
        this.appendVisibleStep(plan, plan.rootStepId(), 0, visibleSteps, new HashSet<>());
        return visibleSteps;
    }

    private void appendVisibleStep(
            JazzyRecipeBookPlanner.Plan plan,
            String stepId,
            int depth,
            List<VisibleStep> visibleSteps,
            Set<String> path
    ) {
        if (!path.add(stepId)) {
            return;
        }
        Optional<JazzyRecipeBookPlanner.PlanStep> optionalStep = plan.step(stepId);
        if (optionalStep.isEmpty()) {
            path.remove(stepId);
            return;
        }

        JazzyRecipeBookPlanner.PlanStep step = optionalStep.get();
        boolean expanded = this.expandedSteps.getOrDefault(step.id(), step.id().equals(plan.rootStepId()));
        visibleSteps.add(new VisibleStep(step, depth, expanded));
        if (expanded) {
            for (String dependencyStepId : step.dependencyStepIds()) {
                this.appendVisibleStep(plan, dependencyStepId, depth + 1, visibleSteps, path);
            }
        }
        path.remove(stepId);
    }

    private void ensureExpandedPath(JazzyRecipeBookPlanner.Plan plan, String stepId) {
        if (stepId == null || stepId.isBlank()) {
            this.expandedSteps.put(plan.rootStepId(), true);
            return;
        }
        this.expandedSteps.put(plan.rootStepId(), true);
        this.expandPathToStep(plan, plan.rootStepId(), stepId);
    }

    private boolean expandPathToStep(JazzyRecipeBookPlanner.Plan plan, String currentStepId, String targetStepId) {
        if (currentStepId.equals(targetStepId)) {
            return true;
        }
        Optional<JazzyRecipeBookPlanner.PlanStep> optionalStep = plan.step(currentStepId);
        if (optionalStep.isEmpty()) {
            return false;
        }
        for (String dependencyStepId : optionalStep.get().dependencyStepIds()) {
            if (this.expandPathToStep(plan, dependencyStepId, targetStepId)) {
                this.expandedSteps.put(currentStepId, true);
                return true;
            }
        }
        return false;
    }

    private boolean isDescendantStep(JazzyRecipeBookPlanner.Plan plan, String ancestorStepId, String targetStepId) {
        if (targetStepId == null || targetStepId.isBlank()) {
            return false;
        }
        if (ancestorStepId.equals(targetStepId)) {
            return true;
        }
        Optional<JazzyRecipeBookPlanner.PlanStep> optionalStep = plan.step(ancestorStepId);
        if (optionalStep.isEmpty()) {
            return false;
        }
        for (String dependencyStepId : optionalStep.get().dependencyStepIds()) {
            if (this.isDescendantStep(plan, dependencyStepId, targetStepId)) {
                return true;
            }
        }
        return false;
    }

    private void ensureSelectedStepVisible() {
        if (this.selectedStepId.isBlank()) {
            return;
        }
        List<VisibleStep> visibleSteps = this.currentVisibleSteps();
        int rowIndex = -1;
        for (int index = 0; index < visibleSteps.size(); index++) {
            if (visibleSteps.get(index).step().id().equals(this.selectedStepId)) {
                rowIndex = index;
                break;
            }
        }
        if (rowIndex < 0) {
            return;
        }

        int visibleRows = this.visibleStepRows();
        if (rowIndex < this.stepScroll) {
            this.stepScroll = rowIndex;
        } else if (rowIndex >= this.stepScroll + visibleRows) {
            this.stepScroll = Math.max(0, rowIndex - visibleRows + 1);
        }
    }

    private int visibleStepRows() {
        return Math.max(1, (this.stepListBottom - STEP_LIST_TOP) / STEP_ROW_HEIGHT);
    }

    private void selectStep(String stepId) {
        this.selectedStepId = stepId;
        this.currentPlan().ifPresent(plan -> {
            this.ensureExpandedPath(plan, stepId);
            this.ensureSelectedStepVisible();
        });

        if (this.isPinnedSelection() && this.selectedItemId != null && this.selectedState != null) {
            RecipeBookClientState.pinSelection(new JazzyRecipeBookSelection(this.selectedItemId, this.selectedState, this.selectedChainKey), stepId);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.clickedItem(mouseX, mouseY)) {
            return true;
        }
        if (this.clickedStep(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isMouseOverItemList(mouseX, mouseY)) {
            int visibleRows = (this.itemListBottom - ITEM_LIST_TOP) / ITEM_ROW_HEIGHT;
            int maxScroll = Math.max(0, this.filteredEntries.size() - visibleRows);
            this.itemScroll = Math.max(0, Math.min(maxScroll, this.itemScroll + (scrollY > 0 ? -1 : 1)));
            return true;
        }
        if (this.isMouseOverStepList(mouseX, mouseY)) {
            int maxScroll = Math.max(0, this.currentVisibleSteps().size() - this.visibleStepRows());
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
        this.selectedStepId = "";
        this.refreshSelection();
        return true;
    }

    private boolean clickedStep(double mouseX, double mouseY) {
        if (!this.isMouseOverStepList(mouseX, mouseY)) {
            return false;
        }
        List<VisibleStep> visibleSteps = this.currentVisibleSteps();
        int row = this.clickedStepRow(mouseY);
        int index = this.stepScroll + row;
        if (index < 0 || index >= visibleSteps.size()) {
            return false;
        }

        VisibleStep visibleStep = visibleSteps.get(index);
        int cardTop = this.panelTop + STEP_LIST_TOP + 6 + row * STEP_ROW_HEIGHT;
        int expandLeft = this.expandButtonLeft(visibleStep);
        if (visibleStep.expandable()
                && mouseX >= expandLeft
                && mouseX < expandLeft + STEP_EXPAND_SIZE
                && mouseY >= cardTop + 6
                && mouseY < cardTop + 6 + STEP_EXPAND_SIZE) {
            boolean expanded = !visibleStep.expanded();
            this.expandedSteps.put(visibleStep.step().id(), expanded);
            if (!expanded && this.currentPlan().filter(plan -> this.isDescendantStep(plan, visibleStep.step().id(), this.selectedStepId)).isPresent()) {
                this.selectStep(visibleStep.step().id());
            }
            return true;
        }

        this.selectStep(visibleStep.step().id());
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
        guiGraphics.fill(0, 0, this.width, this.height, 0x80060810);
        guiGraphics.fill(this.panelLeft, this.panelTop, this.panelLeft + this.panelW, this.panelTop + this.panelH, 0xFF101318);
        guiGraphics.fill(this.panelLeft + 1, this.panelTop + 1, this.panelLeft + this.panelW - 1, this.panelTop + this.panelH - 1, 0xFF181C22);
        guiGraphics.fill(this.panelLeft + 1, this.panelTop + 1, this.panelLeft + this.panelW - 1, this.panelTop + HEADER_HEIGHT, 0xFF1A3040);
        guiGraphics.fill(this.panelLeft + 1, this.panelTop + HEADER_HEIGHT, this.panelLeft + this.panelW - 1, this.panelTop + HEADER_HEIGHT + 2, 0xFF5AAAB1);
        guiGraphics.fill(this.panelLeft + this.leftW, this.panelTop + HEADER_HEIGHT + 2, this.panelLeft + this.leftW + 1, this.panelTop + this.panelH - 1, 0xFF2A3442);
        guiGraphics.drawString(this.font, this.title, this.panelLeft + 10, this.panelTop + 6, 0xFFF0F2F5, false);

        this.renderItemList(guiGraphics, mouseX, mouseY);
        this.renderDetails(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
    }

    @Override
    public void renderTransparentBackground(GuiGraphics guiGraphics) {
    }

    private void renderItemList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.recipe_book.catalog"), this.panelLeft + 10, this.panelTop + 26, 0xFF8C95A6, false);
        if (this.filteredEntries.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.recipe_book.no_matches"), this.panelLeft + 10, this.panelTop + ITEM_LIST_TOP + 8, 0xFF606878, false);
            return;
        }

        int visibleRows = (this.itemListBottom - ITEM_LIST_TOP) / ITEM_ROW_HEIGHT;
        for (int row = 0; row < visibleRows; row++) {
            int index = this.itemScroll + row;
            if (index >= this.filteredEntries.size()) {
                break;
            }
            JazzyRecipeBookPlanner.CatalogEntry entry = this.filteredEntries.get(index);
            int y = this.panelTop + ITEM_LIST_TOP + row * ITEM_ROW_HEIGHT;
            boolean selected = entry.itemId().equals(this.selectedItemId);
            int bg = selected ? 0xFF242E3A : 0x44181C22;
            if (this.isMouseOverItemList(mouseX, mouseY) && this.clickedItemRow(mouseY) == row) {
                bg = 0xFF2A3442;
            }
            guiGraphics.fill(this.panelLeft + 8, y, this.panelLeft + this.leftW - 8, y + ITEM_ROW_HEIGHT - 2, bg);
            ItemStack icon = new ItemStack(entry.item());
            guiGraphics.renderItem(icon, this.panelLeft + 12, y + 1);
            guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(this.catalogLabel(entry), this.leftW - 34), this.panelLeft + 32, y + 5, 0xFFDCE0E8, false);
        }
    }

    private String catalogLabel(JazzyRecipeBookPlanner.CatalogEntry entry) {
        String displayName = entry.displayName();
        if (this.catalogNameCounts.getOrDefault(displayName.toLowerCase(Locale.ROOT), 0) <= 1) {
            return displayName;
        }
        return displayName + " [" + entry.itemId().getPath().replace('_', ' ') + "]";
    }

    private void renderDetails(GuiGraphics guiGraphics) {
        int x = this.panelLeft + this.leftW + 10;
        int y = this.panelTop + 26;
        if (this.selectedItemId == null) {
            guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.recipe_book.empty"), x, y, 0xFF606878, false);
            return;
        }

        ItemStack stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(this.selectedItemId));
        guiGraphics.renderItem(stack, x, y);
        guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(stack.getHoverName().getString(), this.panelW - this.leftW - 46), x + 20, y + 4, 0xFFF0F2F5, false);

        if (this.selectedState != null) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.jazzycookin.recipe_book.state", Component.translatable("state.jazzycookin." + this.selectedState.getSerializedName())),
                    x + 22,
                    this.panelTop + 40,
                    0xFF8C95A6,
                    false
            );
        }

        if (!this.availablePlans().isEmpty()) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.jazzycookin.recipe_book.path", RecipeBookClientState.chainLabel(this.selectedChainKey)),
                    x + 22,
                    this.panelTop + 62,
                    0xFF8C95A6,
                    false
            );
        }

        guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.recipe_book.steps"), x, this.panelTop + STEP_LIST_TOP - 12, 0xFF8C95A6, false);
        guiGraphics.fill(x, this.panelTop + STEP_LIST_TOP, this.panelLeft + this.panelW - 10, this.panelTop + this.stepListBottom, 0x44101418);

        Optional<JazzyRecipeBookPlanner.Plan> optionalPlan = this.currentPlan();
        if (optionalPlan.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.recipe_book.no_path"), x + 6, this.panelTop + STEP_LIST_TOP + 8, 0xFF606878, false);
            return;
        }

        JazzyRecipeBookPlanner.Plan plan = optionalPlan.get();
        Set<String> completed = this.currentCompletedStepIds();
        List<VisibleStep> visibleSteps = this.currentVisibleSteps();
        int visibleRows = this.visibleStepRows();
        int rowY = this.panelTop + STEP_LIST_TOP + 6;
        for (int row = 0; row < visibleRows; row++) {
            int index = this.stepScroll + row;
            if (index >= visibleSteps.size()) {
                break;
            }

            VisibleStep visibleStep = visibleSteps.get(index);
            JazzyRecipeBookPlanner.PlanStep step = visibleStep.step();
            boolean complete = completed.contains(step.id());
            boolean selected = step.id().equals(this.selectedStepId);
            int cardTop = rowY + row * STEP_ROW_HEIGHT;
            guiGraphics.fill(this.stepCardLeft(), cardTop, this.stepCardRight(), cardTop + STEP_ROW_HEIGHT - 2, this.stepBackgroundColor(complete, selected));

            for (int depth = 0; depth < visibleStep.depth(); depth++) {
                int guideX = this.stepCardLeft() + 30 + depth * STEP_INDENT;
                guiGraphics.fill(guideX, cardTop + 3, guideX + 1, cardTop + STEP_ROW_HEIGHT - 5, 0x442A3442);
            }

            guiGraphics.drawString(this.font, Component.literal((plan.indexOfStep(step.id()) + 1) + "."), this.stepCardLeft() + 8, cardTop + 6,
                    complete ? 0xFF4ADE80 : 0xFFF0B429, false);

            if (visibleStep.expandable()) {
                int expandLeft = this.expandButtonLeft(visibleStep);
                guiGraphics.fill(expandLeft, cardTop + 6, expandLeft + STEP_EXPAND_SIZE, cardTop + 6 + STEP_EXPAND_SIZE, 0xFF2A3442);
                guiGraphics.drawString(this.font, Component.literal(visibleStep.expanded() ? "-" : "+"), expandLeft + 3, cardTop + 7, 0xFFDCE0E8, false);
            }

            int iconX = this.stepIconLeft(visibleStep);
            guiGraphics.renderItem(step.outputStack(), iconX, cardTop + 4);
            int textX = iconX + 20;
            int textWidth = this.stepCardRight() - textX - 6;
            guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(step.outputStack().getHoverName().getString(), textWidth), textX, cardTop + 3, 0xFFDCE0E8, false);
            guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(this.stepDetail(step, visibleStep), textWidth), textX, cardTop + 13, 0xFF8C95A6, false);
        }
    }

    private int stepCardLeft() {
        return this.panelLeft + this.leftW + 14;
    }

    private int stepCardRight() {
        return this.panelLeft + this.panelW - 14;
    }

    private int expandButtonLeft(VisibleStep visibleStep) {
        return this.stepCardLeft() + 26 + visibleStep.depth() * STEP_INDENT;
    }

    private int stepIconLeft(VisibleStep visibleStep) {
        int baseLeft = this.stepCardLeft() + 40 + visibleStep.depth() * STEP_INDENT;
        return visibleStep.expandable() ? baseLeft : baseLeft - 10;
    }

    private int stepBackgroundColor(boolean complete, boolean selected) {
        if (complete && selected) {
            return 0xCC1A3A2C;
        }
        if (selected) {
            return 0xCC1C2C3C;
        }
        if (complete) {
            return 0xAA163028;
        }
        return 0xAA141A22;
    }

    private String stepDetail(JazzyRecipeBookPlanner.PlanStep step, VisibleStep visibleStep) {
        if (step.expandable() && !visibleStep.expanded()) {
            return Component.translatable("screen.jazzycookin.recipe_book.expand_hint").getString();
        }
        if (!step.options().isEmpty()) {
            JazzyRecipeBookPlanner.StepOption option = step.options().get(0);
            if (option.station() != null) {
                return option.station().displayName().getString();
            }
            if (!option.requirements().isEmpty()) {
                JazzyRecipeBookPlanner.Requirement requirement = option.requirements().get(0);
                ItemStack choice = requirement.choices().isEmpty() ? ItemStack.EMPTY : requirement.choices().get(0);
                return choice.isEmpty()
                        ? Component.translatable("screen.jazzycookin.recipe_book.materials").getString()
                        : choice.getHoverName().getString();
            }
            if (!option.notes().isEmpty()) {
                return option.notes().get(0);
            }
        }
        return Component.translatable("screen.jazzycookin.recipe_book.step").getString();
    }

    private boolean isMouseOverItemList(double mouseX, double mouseY) {
        return mouseX >= this.panelLeft + 8 && mouseX < this.panelLeft + this.leftW - 8
                && mouseY >= this.panelTop + ITEM_LIST_TOP && mouseY < this.panelTop + this.itemListBottom;
    }

    private boolean isMouseOverStepList(double mouseX, double mouseY) {
        int x = this.panelLeft + this.leftW + 10;
        return mouseX >= x && mouseX < this.panelLeft + this.panelW - 10
                && mouseY >= this.panelTop + STEP_LIST_TOP && mouseY < this.panelTop + this.stepListBottom;
    }

    private int clickedItemRow(double mouseY) {
        return ((int) mouseY - (this.panelTop + ITEM_LIST_TOP)) / ITEM_ROW_HEIGHT;
    }

    private int clickedStepRow(double mouseY) {
        return ((int) mouseY - (this.panelTop + STEP_LIST_TOP + 6)) / STEP_ROW_HEIGHT;
    }
}
