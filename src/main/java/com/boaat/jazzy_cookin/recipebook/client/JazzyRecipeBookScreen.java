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

import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.boaat.jazzy_cookin.recipebook.JazzyRecipeBookPlanner;
import com.boaat.jazzy_cookin.recipebook.JazzyRecipeBookSelection;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

public class JazzyRecipeBookScreen extends Screen {
    private enum CatalogFilter {
        ALL(null, "screen.jazzycookin.recipe_book.filter.all"),
        DISHES(JazzyRecipeBookPlanner.CatalogCategory.DISHES, "screen.jazzycookin.recipe_book.filter.dishes"),
        BASE_INGREDIENTS(JazzyRecipeBookPlanner.CatalogCategory.BASE_INGREDIENTS, "screen.jazzycookin.recipe_book.filter.base_ingredients"),
        FARMING(JazzyRecipeBookPlanner.CatalogCategory.FARMING, "screen.jazzycookin.recipe_book.filter.farming"),
        KITCHEN(JazzyRecipeBookPlanner.CatalogCategory.KITCHEN, "screen.jazzycookin.recipe_book.filter.kitchen");

        private final JazzyRecipeBookPlanner.CatalogCategory category;
        private final String translationKey;

        CatalogFilter(JazzyRecipeBookPlanner.CatalogCategory category, String translationKey) {
            this.category = category;
            this.translationKey = translationKey;
        }

        private boolean matches(JazzyRecipeBookPlanner.CatalogEntry entry) {
            return this.category == null || entry.category() == this.category;
        }

        private Component label() {
            return Component.translatable(this.translationKey);
        }

        private CatalogFilter next() {
            CatalogFilter[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    private record VisibleStep(
            JazzyRecipeBookPlanner.PlanStep step,
            int instructionIndex,
            ItemStack icon,
            String title,
            String detail
    ) {
        public String id() {
            return this.step.id() + "#" + this.instructionIndex;
        }

        public int depth() {
            return 0;
        }

        public boolean expanded() {
            return true;
        }

        public boolean expandable() {
            return false;
        }
    }

    private static final int DESIGN_PANEL_WIDTH = 500;
    private static final int DESIGN_PANEL_HEIGHT = 286;
    private static final int HEADER_HEIGHT = 20;
    private static final int DESIGN_LEFT_WIDTH = 132;
    private static final int ITEM_ROW_HEIGHT = 18;
    private static final int SEARCH_TOP = 38;
    private static final int FILTER_TOP = 58;
    private static final int ITEM_LIST_TOP = 82;
    private static final int STEP_LIST_TOP = 78;
    private static final int STEP_ROW_HEIGHT = 22;
    private static final int STEP_INDENT = 12;
    private static final int STEP_EXPAND_SIZE = 10;

    private EditBox searchBox;
    private Button filterButton;
    private Button pinButton;
    private Button stateBackButton;
    private Button stateForwardButton;
    private Button chainBackButton;
    private Button chainForwardButton;
    private float panelScale = 1.0F;
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
    private CatalogFilter catalogFilter = CatalogFilter.ALL;
    private int itemScroll;
    private int stepScroll;
    private ResourceLocation selectedItemId;
    private IngredientState selectedState;
    private String selectedChainKey = "";
    private String selectedStepId = "";
    private String selectedInstructionId = "";
    private String displayedPlanKey = "";

    public JazzyRecipeBookScreen() {
        super(Component.translatable("screen.jazzycookin.recipe_book.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.panelScale = this.resolvePanelScale();
        this.panelW = Math.min(this.width - 8, Math.round(DESIGN_PANEL_WIDTH * this.panelScale));
        this.panelH = Math.min(this.height - 8, Math.round(DESIGN_PANEL_HEIGHT * this.panelScale));
        this.leftW = Math.max(136, Math.round(DESIGN_LEFT_WIDTH * this.panelScale));
        this.panelLeft = Math.max(4, (this.width - this.panelW) / 2);
        this.panelTop = Math.max(4, (this.height - this.panelH) / 2);
        this.itemListBottom = Math.max(this.sy(ITEM_LIST_TOP) + this.sy(ITEM_ROW_HEIGHT), this.panelH - this.sy(14));
        this.stepListBottom = Math.max(this.sy(STEP_LIST_TOP) + this.sy(STEP_ROW_HEIGHT), this.panelH - this.sy(38));

        this.searchBox = this.addRenderableWidget(new EditBox(
                this.font,
                this.panelLeft + this.sx(10),
                this.panelTop + this.sy(SEARCH_TOP),
                this.leftW - this.sx(20),
                Math.max(16, this.sy(16)),
                Component.translatable("screen.jazzycookin.recipe_book.search")
        ));
        this.searchBox.setResponder(value -> {
            this.itemScroll = 0;
            this.refreshEntries();
            this.refreshSelection();
        });

        this.filterButton = this.addRenderableWidget(Button.builder(this.filterButtonMessage(), button -> this.cycleCatalogFilter())
                .bounds(this.panelLeft + this.sx(10), this.panelTop + this.sy(FILTER_TOP), this.leftW - this.sx(20), this.sy(18))
                .build());

        this.stateBackButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.cycleState(-1))
                .bounds(this.panelLeft + this.leftW + this.sx(18), this.panelTop + this.sy(34), this.sx(18), this.sy(18))
                .build());
        this.stateForwardButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.cycleState(1))
                .bounds(this.panelLeft + this.panelW - this.sx(28), this.panelTop + this.sy(34), this.sx(18), this.sy(18))
                .build());
        this.chainBackButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.cycleChain(-1))
                .bounds(this.panelLeft + this.leftW + this.sx(18), this.panelTop + this.sy(56), this.sx(18), this.sy(18))
                .build());
        this.chainForwardButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.cycleChain(1))
                .bounds(this.panelLeft + this.panelW - this.sx(28), this.panelTop + this.sy(56), this.sx(18), this.sy(18))
                .build());
        this.pinButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> this.togglePin())
                .bounds(this.panelLeft + this.leftW + this.sx(18), this.panelTop + this.panelH - this.sy(30), this.panelW - this.leftW - this.sx(28), this.sy(20))
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
            if (this.catalogFilter.matches(entry) && (query.isBlank() || this.catalogMatches(entry, query))) {
                this.filteredEntries.add(entry);
            }
        }
        if (this.filteredEntries.isEmpty()) {
            this.selectedItemId = null;
            this.selectedState = null;
            this.selectedChainKey = "";
            this.selectedStepId = "";
            this.selectedInstructionId = "";
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

    private Component filterButtonMessage() {
        return this.catalogFilter.label();
    }

    private void cycleCatalogFilter() {
        this.catalogFilter = this.catalogFilter.next();
        if (this.filterButton != null) {
            this.filterButton.setMessage(this.filterButtonMessage());
        }
        this.itemScroll = 0;
        this.refreshEntries();
        this.refreshSelection();
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
            this.selectedInstructionId = "";
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
            this.selectedInstructionId = "";
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
            this.selectedInstructionId = "";
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
            this.selectedInstructionId = "";
        }
        this.ensureExpandedPath(plan, this.selectedStepId);
        this.ensureSelectedStepVisible();
    }

    private void refreshButtons() {
        if (this.pinButton == null || this.filterButton == null || this.stateBackButton == null || this.stateForwardButton == null || this.chainBackButton == null || this.chainForwardButton == null) {
            return;
        }
        this.filterButton.setMessage(this.filterButtonMessage());

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
        this.selectedInstructionId = "";
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
        this.selectedInstructionId = "";
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
        if (!preferredStepId.isBlank() && plan.step(preferredStepId).isPresent()) {
            return preferredStepId;
        }
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
        for (JazzyRecipeBookPlanner.PlanStep step : plan.steps()) {
            visibleSteps.addAll(this.instructionRowsFor(step));
        }
        return visibleSteps;
    }

    private List<VisibleStep> instructionRowsFor(JazzyRecipeBookPlanner.PlanStep step) {
        if (step.options().isEmpty()) {
            return List.of(new VisibleStep(step, 0, step.outputStack().copy(),
                    "Obtain " + step.outputStack().getHoverName().getString(),
                    "Make or collect this item."));
        }

        JazzyRecipeBookPlanner.StepOption option = step.options().get(0);
        List<VisibleStep> rows = new ArrayList<>();
        int row = 0;
        for (JazzyRecipeBookPlanner.Requirement requirement : option.requirements()) {
            ItemStack icon = requirement.choices().isEmpty() ? step.outputStack().copy() : requirement.choices().get(0).copy();
            rows.add(new VisibleStep(step, row++, icon,
                    requirementTitle(step, requirement),
                    requirementDetail(requirement)));
        }
        String setup = controlSummary(option);
        if (option.station() != null || !setup.isBlank()) {
            rows.add(new VisibleStep(step, row++, step.outputStack().copy(),
                    setupTitle(option),
                    setup.isBlank() ? "Prepare the station for this step." : setup));
        }
        rows.add(new VisibleStep(step, row, step.outputStack().copy(), stepActionTitle(step, option), stepActionDetail(step, option)));
        return rows;
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
        visibleSteps.add(new VisibleStep(step, visibleSteps.size(), step.outputStack().copy(),
                step.outputStack().getHoverName().getString(), Component.translatable("screen.jazzycookin.recipe_book.step").getString()));
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
        String selectedVisibleId = this.selectedVisibleStepId(visibleSteps);
        for (int index = 0; index < visibleSteps.size(); index++) {
            VisibleStep visibleStep = visibleSteps.get(index);
            if (visibleStep.id().equals(selectedVisibleId)) {
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
        return Math.max(1, (this.stepListBottom - this.sy(STEP_LIST_TOP)) / this.sy(STEP_ROW_HEIGHT));
    }

    private void selectStep(String stepId) {
        this.selectStep(stepId, "");
    }

    private void selectStep(String stepId, String instructionId) {
        this.selectedStepId = stepId;
        this.selectedInstructionId = instructionId;
        this.currentPlan().ifPresent(plan -> {
            this.ensureExpandedPath(plan, stepId);
            this.ensureSelectedStepVisible();
        });

        if (this.isPinnedSelection() && this.selectedItemId != null && this.selectedState != null) {
            RecipeBookClientState.pinSelection(new JazzyRecipeBookSelection(this.selectedItemId, this.selectedState, this.selectedChainKey), stepId);
        }
    }

    private String selectedVisibleStepId(List<VisibleStep> visibleSteps) {
        if (!this.selectedInstructionId.isBlank()) {
            return this.selectedInstructionId;
        }
        for (VisibleStep visibleStep : visibleSteps) {
            if (visibleStep.step().id().equals(this.selectedStepId)) {
                return visibleStep.id();
            }
        }
        return "";
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
            int visibleRows = (this.itemListBottom - this.sy(ITEM_LIST_TOP)) / this.sy(ITEM_ROW_HEIGHT);
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
        int relativeY = (int) mouseY - (this.panelTop + this.sy(ITEM_LIST_TOP));
        int row = relativeY / this.sy(ITEM_ROW_HEIGHT);
        int index = this.itemScroll + row;
        if (index < 0 || index >= this.filteredEntries.size()) {
            return false;
        }
        JazzyRecipeBookPlanner.CatalogEntry entry = this.filteredEntries.get(index);
        this.selectedItemId = entry.itemId();
        this.selectedState = entry.producibleStates().get(0);
        this.selectedChainKey = "";
        this.selectedStepId = "";
        this.selectedInstructionId = "";
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
        if (visibleStep.expandable()) {
            int indent = visibleStep.depth() * this.sx(STEP_INDENT);
            int numX = this.stepCardLeft() + this.sx(6) + indent;
            int numWidth = this.font.width((index + 1) + ".") + 4;
            int arrowLeft = numX + numWidth;
            int arrowRight = arrowLeft + this.sx(STEP_EXPAND_SIZE);
            int cardTop = this.panelTop + this.sy(STEP_LIST_TOP) + this.sy(4) + row * this.sy(STEP_ROW_HEIGHT);
            if (mouseX >= arrowLeft && mouseX < arrowRight && mouseY >= cardTop && mouseY < cardTop + this.sy(STEP_ROW_HEIGHT) - 2) {
                boolean expanded = !visibleStep.expanded();
                this.expandedSteps.put(visibleStep.step().id(), expanded);
                if (!expanded && this.currentPlan().filter(plan -> this.isDescendantStep(plan, visibleStep.step().id(), this.selectedStepId)).isPresent()) {
                    this.selectStep(visibleStep.step().id());
                }
                return true;
            }
        }

        this.selectStep(visibleStep.step().id(), visibleStep.id());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox != null && this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (RecipeBookClientState.previousStepKeyMatches(keyCode, scanCode)) {
            return this.focusRelativeStep(-1);
        }
        if (RecipeBookClientState.nextStepKeyMatches(keyCode, scanCode)) {
            return this.focusRelativeStep(1);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean focusRelativeStep(int delta) {
        Optional<JazzyRecipeBookPlanner.Plan> optionalPlan = this.currentPlan();
        if (optionalPlan.isEmpty()) {
            return false;
        }
        JazzyRecipeBookPlanner.Plan plan = optionalPlan.get();
        List<VisibleStep> visibleSteps = this.currentVisibleSteps();
        if (visibleSteps.isEmpty()) {
            return false;
        }
        String selectedVisibleId = this.selectedVisibleStepId(visibleSteps);
        int currentIndex = -1;
        for (int index = 0; index < visibleSteps.size(); index++) {
            if (visibleSteps.get(index).id().equals(selectedVisibleId)) {
                currentIndex = index;
                break;
            }
        }
        if (currentIndex < 0) {
            String focusedStepId = Objects.requireNonNullElse(RecipeBookClientState.focusedStepId(), "");
            for (int index = 0; index < visibleSteps.size(); index++) {
                if (visibleSteps.get(index).step().id().equals(focusedStepId)) {
                    currentIndex = index;
                    break;
                }
            }
            if (currentIndex < 0) {
                currentIndex = 0;
            }
        }
        int nextIndex = Math.max(0, Math.min(visibleSteps.size() - 1, currentIndex + delta));
        if (nextIndex == currentIndex) {
            return true;
        }
        VisibleStep next = visibleSteps.get(nextIndex);
        this.selectStep(next.step().id(), next.id());
        return true;
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
        guiGraphics.fill(this.panelLeft + 1, this.panelTop + 1, this.panelLeft + this.panelW - 1, this.panelTop + this.sy(HEADER_HEIGHT), 0xFF1A3040);
        guiGraphics.fill(this.panelLeft + 1, this.panelTop + this.sy(HEADER_HEIGHT), this.panelLeft + this.panelW - 1, this.panelTop + this.sy(HEADER_HEIGHT) + 2, 0xFF67CED7);
        guiGraphics.fill(this.panelLeft + this.leftW, this.panelTop + this.sy(HEADER_HEIGHT) + 2, this.panelLeft + this.leftW + 1, this.panelTop + this.panelH - 1, 0xFF354150);
        guiGraphics.drawString(this.font, this.title, this.panelLeft + this.sx(10), this.panelTop + this.sy(6), 0xFFF4F7FB, false);

        this.renderItemList(guiGraphics, mouseX, mouseY);
        this.renderDetails(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderStepTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
    }

    @Override
    public void renderTransparentBackground(GuiGraphics guiGraphics) {
    }

    private void renderItemList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.recipe_book.catalog"), this.panelLeft + this.sx(10), this.panelTop + this.sy(26), 0xFFAEB8C9, false);
        if (this.filteredEntries.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.recipe_book.no_matches"),
                    this.panelLeft + this.sx(10), this.panelTop + this.sy(ITEM_LIST_TOP) + this.sy(8), 0xFF9099A8, false);
            return;
        }

        int visibleRows = (this.itemListBottom - this.sy(ITEM_LIST_TOP)) / this.sy(ITEM_ROW_HEIGHT);
        for (int row = 0; row < visibleRows; row++) {
            int index = this.itemScroll + row;
            if (index >= this.filteredEntries.size()) {
                break;
            }
            JazzyRecipeBookPlanner.CatalogEntry entry = this.filteredEntries.get(index);
            int y = this.panelTop + this.sy(ITEM_LIST_TOP) + row * this.sy(ITEM_ROW_HEIGHT);
            boolean selected = entry.itemId().equals(this.selectedItemId);
            int bg = selected ? 0xFF242E3A : 0x44181C22;
            if (this.isMouseOverItemList(mouseX, mouseY) && this.clickedItemRow(mouseY) == row) {
                bg = 0xFF2A3442;
            }
            guiGraphics.fill(this.panelLeft + this.sx(8), y, this.panelLeft + this.leftW - this.sx(8), y + this.sy(ITEM_ROW_HEIGHT) - 2, bg);
            ItemStack icon = new ItemStack(entry.item());
            guiGraphics.renderItem(icon, this.panelLeft + this.sx(12), y + Math.max(1, this.sy(1)));
            guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(this.catalogLabel(entry), this.leftW - this.sx(40)),
                    this.panelLeft + this.sx(32), y + this.sy(5), 0xFFE8EDF6, false);
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
        int x = this.panelLeft + this.leftW + this.sx(10);
        int y = this.panelTop + this.sy(26);
        if (this.selectedItemId == null) {
            guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.recipe_book.empty"), x, y, 0xFF9099A8, false);
            return;
        }

        ItemStack stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(this.selectedItemId));
        guiGraphics.renderItem(stack, x, y);
        guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(stack.getHoverName().getString(), this.panelW - this.leftW - this.sx(48)),
                x + this.sx(20), y + this.sy(4), 0xFFF4F7FB, false);

        if (this.selectedState != null) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.jazzycookin.recipe_book.state", Component.translatable("state.jazzycookin." + this.selectedState.getSerializedName())),
                    x + this.sx(22),
                    this.panelTop + this.sy(40),
                    0xFFAEB8C9,
                    false
            );
        }

        if (!this.availablePlans().isEmpty()) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.jazzycookin.recipe_book.path", RecipeBookClientState.chainLabel(this.selectedChainKey)),
                    x + this.sx(22),
                    this.panelTop + this.sy(62),
                    0xFFAEB8C9,
                    false
            );
        }

        guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.recipe_book.steps"), x, this.panelTop + this.sy(STEP_LIST_TOP) - this.sy(12), 0xFFAEB8C9, false);
        guiGraphics.fill(x, this.panelTop + this.sy(STEP_LIST_TOP), this.panelLeft + this.panelW - this.sx(10), this.panelTop + this.stepListBottom, 0x44101418);

        Optional<JazzyRecipeBookPlanner.Plan> optionalPlan = this.currentPlan();
        if (optionalPlan.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.recipe_book.no_path"),
                    x + this.sx(6), this.panelTop + this.sy(STEP_LIST_TOP) + this.sy(8), 0xFF9099A8, false);
            return;
        }

        JazzyRecipeBookPlanner.Plan plan = optionalPlan.get();
        Set<String> completed = this.currentCompletedStepIds();
        List<VisibleStep> visibleSteps = this.currentVisibleSteps();
        int visibleRows = this.visibleStepRows();
        int rowY = this.panelTop + this.sy(STEP_LIST_TOP) + this.sy(4);
        for (int row = 0; row < visibleRows; row++) {
            int index = this.stepScroll + row;
            if (index >= visibleSteps.size()) {
                break;
            }

            VisibleStep visibleStep = visibleSteps.get(index);
            JazzyRecipeBookPlanner.PlanStep step = visibleStep.step();
            boolean complete = completed.contains(step.id());
            boolean selected = visibleStep.id().equals(this.selectedVisibleStepId(visibleSteps));
            int cardTop = rowY + row * this.sy(STEP_ROW_HEIGHT);
            guiGraphics.fill(this.stepCardLeft(), cardTop, this.stepCardRight(), cardTop + this.sy(STEP_ROW_HEIGHT) - 2, this.stepBackgroundColor(complete, selected));
            if (selected) {
                guiGraphics.fill(this.stepCardLeft(), cardTop, this.stepCardLeft() + 2, cardTop + this.sy(STEP_ROW_HEIGHT) - 2, complete ? 0xFF4ADE80 : 0xFF5AAAB1);
            }

            int indent = visibleStep.depth() * this.sx(STEP_INDENT);
            for (int depth = 0; depth < visibleStep.depth(); depth++) {
                int guideX = this.stepCardLeft() + this.sx(8) + depth * this.sx(STEP_INDENT);
                guiGraphics.fill(guideX, cardTop + 2, guideX + 1, cardTop + this.sy(STEP_ROW_HEIGHT) - 4, 0x33404D60);
            }

            int numX = this.stepCardLeft() + this.sx(6) + indent;
            guiGraphics.drawString(this.font, Component.literal((index + 1) + "."), numX, cardTop + this.sy(4),
                    complete ? 0xFF4ADE80 : 0xFFF0B429, false);

            int afterNum = numX + this.font.width((index + 1) + ".") + 4;
            if (visibleStep.expandable()) {
                String arrow = visibleStep.expanded() ? "\u25BC" : "\u25B6";
                guiGraphics.drawString(this.font, Component.literal(arrow), afterNum, cardTop + this.sy(4), 0xFFAEB8C9, false);
                afterNum += this.sx(STEP_EXPAND_SIZE);
            }

            int iconX = afterNum + this.sx(2);
            guiGraphics.renderItem(visibleStep.icon(), iconX, cardTop + this.sy(3));
            int textX = iconX + this.sx(18);
            int textWidth = Math.max(10, this.stepCardRight() - textX - this.sx(4));
            guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(visibleStep.title(), textWidth), textX, cardTop + this.sy(2), 0xFFE8EDF6, false);
            guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(visibleStep.detail(), textWidth), textX, cardTop + this.sy(12), 0xFFAEB8C9, false);
        }
    }

    private void renderSelectedStepInstructions(GuiGraphics guiGraphics, JazzyRecipeBookPlanner.Plan plan) {
        JazzyRecipeBookPlanner.PlanStep step = plan.step(this.selectedStepId).orElseGet(() -> plan.focusedStep(this.currentCompletedStepIds(), this.selectedStepId));
        if (step == null) {
            return;
        }

        int left = this.stepCardLeft();
        int right = this.stepCardRight();
        int top = this.panelTop + this.stepListBottom + this.sy(6);
        int bottom = this.panelTop + this.panelH - this.sy(34);
        guiGraphics.fill(left, top, right, bottom, 0xAA101418);
        guiGraphics.fill(left, top, left + 2, bottom, 0xFF67CED7);

        int stepIndex = Math.max(0, plan.indexOfStep(step.id()));
        int textX = left + this.sx(8);
        int textWidth = Math.max(20, right - textX - this.sx(8));
        guiGraphics.drawString(
                this.font,
                this.font.plainSubstrByWidth("Step " + (stepIndex + 1) + " of " + plan.steps().size() + ": " + step.outputStack().getHoverName().getString(), textWidth),
                textX,
                top + this.sy(5),
                0xFFF4F7FB,
                false
        );

        List<String> lines = this.stepInstructionLines(step);
        int lineY = top + this.sy(17);
        int maxLines = Math.max(1, (bottom - lineY - this.sy(2)) / 10);
        for (int index = 0; index < Math.min(maxLines, lines.size()); index++) {
            int color = index == lines.size() - 1 ? 0xFF8CDBB5 : 0xFFAEB8C9;
            guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(lines.get(index), textWidth), textX, lineY + index * 10, color, false);
        }
    }

    private int stepCardLeft() {
        return this.panelLeft + this.leftW + 14;
    }

    private int stepCardRight() {
        return this.panelLeft + this.panelW - 14;
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
        if (!step.options().isEmpty()) {
            JazzyRecipeBookPlanner.StepOption option = step.options().get(0);
            List<String> parts = new ArrayList<>();
            if (option.station() != null) {
                parts.add(option.station().displayName().getString());
            } else if (option.method() != null) {
                parts.add(option.method().displayName().getString());
            }
            if (!option.requirements().isEmpty()) {
                JazzyRecipeBookPlanner.Requirement requirement = option.requirements().get(0);
                ItemStack choice = requirement.choices().isEmpty() ? ItemStack.EMPTY : requirement.choices().get(0);
                if (!choice.isEmpty()) {
                    String requirementText = (requirement.count() > 1 ? requirement.count() + "x " : "") + choice.getHoverName().getString();
                    if (option.requirements().size() > 1) {
                        requirementText += " +" + (option.requirements().size() - 1);
                    }
                    parts.add(requirementText);
                }
            }
            if (option.requiresPreheat()) {
                parts.add("Preheat");
            }
            if (!option.notes().isEmpty()) {
                parts.add(option.notes().get(0));
            }
            if (!parts.isEmpty()) {
                return String.join(" | ", parts);
            }
        }
        return Component.translatable("screen.jazzycookin.recipe_book.step").getString();
    }

    private List<String> stepInstructionLines(JazzyRecipeBookPlanner.PlanStep step) {
        if (step.options().isEmpty()) {
            return List.of("Make or obtain " + step.outputStack().getHoverName().getString() + ".");
        }

        JazzyRecipeBookPlanner.StepOption option = step.options().get(0);
        List<String> lines = new ArrayList<>();
        lines.add(stepActionLine(step, option));
        String requirements = requirementSummary(option);
        if (!requirements.isBlank()) {
            lines.add("Add: " + requirements + ".");
        }
        String controls = controlSummary(option);
        if (!controls.isBlank()) {
            lines.add(controls + ".");
        }
        lines.add("Suggested perfect-score path; matching substitutions can still count.");
        return lines;
    }

    private static String requirementTitle(JazzyRecipeBookPlanner.PlanStep step, JazzyRecipeBookPlanner.Requirement requirement) {
        String item = compactRequirementLabel(requirement);
        return switch (step.kind()) {
            case PLATE -> "Add " + item;
            case PROCESS -> "Add " + item;
            case CRAFT -> "Get " + item;
            case SOURCE -> "Have " + item;
        };
    }

    private static String requirementDetail(JazzyRecipeBookPlanner.Requirement requirement) {
        if (requirement.choices().size() > 1) {
            return requirement.choices().size() + " matching choices; this is the suggested one.";
        }
        return requirement.requiredState() == IngredientState.PANTRY_READY
                ? "Use this in the next action."
                : "State: " + Component.translatable("state.jazzycookin." + requirement.requiredState().getSerializedName()).getString();
    }

    private static String setupTitle(JazzyRecipeBookPlanner.StepOption option) {
        if (option.station() != null) {
            return "Set up " + option.station().displayName().getString();
        }
        if (option.method() != null) {
            return "Set up " + option.method().displayName().getString();
        }
        return "Set controls";
    }

    private static String stepActionTitle(JazzyRecipeBookPlanner.PlanStep step, JazzyRecipeBookPlanner.StepOption option) {
        String output = step.outputStack().getHoverName().getString();
        return switch (step.kind()) {
            case CRAFT -> "Craft " + output;
            case SOURCE -> "Collect " + output;
            case PLATE -> "Plate " + output;
            case PROCESS -> {
                if (option.method() != null) {
                    yield option.method().displayName().getString() + " " + output;
                }
                yield "Make " + output;
            }
        };
    }

    private static String stepActionDetail(JazzyRecipeBookPlanner.PlanStep step, JazzyRecipeBookPlanner.StepOption option) {
        String output = step.outputStack().getHoverName().getString();
        return switch (step.kind()) {
            case CRAFT -> "Use the normal crafting recipe.";
            case SOURCE -> "Harvest the source block or collect this output.";
            case PLATE -> "Start the Plating Station to finish " + output + ".";
            case PROCESS -> "Start the station and cook toward the score targets.";
        };
    }

    private static String stepActionLine(JazzyRecipeBookPlanner.PlanStep step, JazzyRecipeBookPlanner.StepOption option) {
        String output = step.outputStack().getHoverName().getString();
        return switch (step.kind()) {
            case CRAFT -> "Craft " + output + ".";
            case SOURCE -> "Harvest or collect " + output + ".";
            case PLATE -> "Plate " + output + " at the Plating Station.";
            case PROCESS -> {
                if (option.station() != null) {
                    yield "Use the " + option.station().displayName().getString() + " to make " + output + ".";
                }
                if (option.method() != null) {
                    yield "Use " + option.method().displayName().getString() + " to make " + output + ".";
                }
                yield "Prepare " + output + ".";
            }
        };
    }

    private static String requirementSummary(JazzyRecipeBookPlanner.StepOption option) {
        if (option.requirements().isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        int shown = Math.min(3, option.requirements().size());
        for (int index = 0; index < shown; index++) {
            parts.add(requirementLabel(option.requirements().get(index)));
        }
        if (option.requirements().size() > shown) {
            parts.add("+" + (option.requirements().size() - shown) + " more");
        }
        return String.join(", ", parts);
    }

    private static String controlSummary(JazzyRecipeBookPlanner.StepOption option) {
        List<String> parts = new ArrayList<>();
        if (option.preferredTool() != null && option.preferredTool() != ToolProfile.NONE) {
            parts.add("Tool: " + toolName(option.preferredTool()));
        }
        if (option.preferredHeat() != null && !"off".equals(option.preferredHeat().getSerializedName())) {
            parts.add("Heat: " + recipeHeatLabel(option).getString());
        }
        if (option.requiresPreheat()) {
            parts.add("Preheat first");
        }
        if (option.durationTicks() > 0) {
            parts.add(option.durationTicks() + " ticks");
        }
        return String.join(" | ", parts);
    }

    private void renderStepTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Optional<VisibleStep> hoveredStep = this.hoveredVisibleStep(mouseX, mouseY);
        if (hoveredStep.isEmpty()) {
            return;
        }
        List<Component> tooltip = this.stepTooltip(hoveredStep.get());
        if (!tooltip.isEmpty()) {
            List<FormattedCharSequence> lines = tooltip.stream().map(Component::getVisualOrderText).toList();
            guiGraphics.renderTooltip(this.font, lines, mouseX, mouseY);
        }
    }

    private Optional<VisibleStep> hoveredVisibleStep(double mouseX, double mouseY) {
        if (!this.isMouseOverStepList(mouseX, mouseY)) {
            return Optional.empty();
        }
        List<VisibleStep> visibleSteps = this.currentVisibleSteps();
        int row = this.clickedStepRow(mouseY);
        int index = this.stepScroll + row;
        if (row < 0 || row >= this.visibleStepRows() || index < 0 || index >= visibleSteps.size()) {
            return Optional.empty();
        }
        return Optional.of(visibleSteps.get(index));
    }

    private List<Component> stepTooltip(VisibleStep visibleStep) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(visibleStep.title()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(visibleStep.detail()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Part of: " + visibleStep.step().outputStack().getHoverName().getString()).withStyle(ChatFormatting.DARK_GRAY));
        return tooltip;
    }

    private static String compactRequirementLabel(JazzyRecipeBookPlanner.Requirement requirement) {
        String count = requirement.count() > 1 ? requirement.count() + "x " : "";
        if (requirement.choices().isEmpty()) {
            return count + Component.translatable("screen.jazzycookin.recipe_book.step").getString();
        }
        String name = requirement.choices().get(0).getHoverName().getString();
        if (requirement.choices().size() > 1) {
            return count + name + " +" + (requirement.choices().size() - 1);
        }
        return count + name;
    }

    private static String requirementLabel(JazzyRecipeBookPlanner.Requirement requirement) {
        StringBuilder builder = new StringBuilder();
        if (requirement.count() > 1) {
            builder.append(requirement.count()).append("x ");
        }
        if (requirement.choices().isEmpty()) {
            builder.append(Component.translatable("screen.jazzycookin.recipe_book.step").getString());
            return builder.toString();
        }
        int shown = Math.min(3, requirement.choices().size());
        for (int i = 0; i < shown; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(requirement.choices().get(i).getHoverName().getString());
        }
        if (requirement.choices().size() > shown) {
            builder.append(" +").append(requirement.choices().size() - shown);
        }
        return builder.toString();
    }

    private static String toolList(List<ToolProfile> tools) {
        StringBuilder builder = new StringBuilder();
        int shown = Math.min(4, tools.size());
        for (int i = 0; i < shown; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(toolName(tools.get(i)));
        }
        if (tools.size() > shown) {
            builder.append(" +").append(tools.size() - shown);
        }
        return builder.toString();
    }

    private static String toolName(ToolProfile tool) {
        return Component.translatable("tool.jazzycookin." + tool.getSerializedName()).getString();
    }

    private static Component recipeHeatLabel(JazzyRecipeBookPlanner.StepOption option) {
        if (usesNumberedHeat(option.station())) {
            return Component.literal(switch (option.preferredHeat()) {
                case LOW -> "1-2";
                case MEDIUM -> "3-4";
                case HIGH -> "5-6";
                default -> "1";
            });
        }
        HeatLevel heatLevel = option.preferredHeat();
        return Component.translatable("heat.jazzycookin." + heatLevel.getSerializedName());
    }

    private static boolean usesNumberedHeat(StationType stationType) {
        return stationType == StationType.STOVE || stationType == StationType.SMOKER || stationType == StationType.STEAMER;
    }

    private boolean isMouseOverItemList(double mouseX, double mouseY) {
        return mouseX >= this.panelLeft + this.sx(8) && mouseX < this.panelLeft + this.leftW - this.sx(8)
                && mouseY >= this.panelTop + this.sy(ITEM_LIST_TOP) && mouseY < this.panelTop + this.itemListBottom;
    }

    private boolean isMouseOverStepList(double mouseX, double mouseY) {
        int x = this.panelLeft + this.leftW + this.sx(10);
        return mouseX >= x && mouseX < this.panelLeft + this.panelW - this.sx(10)
                && mouseY >= this.panelTop + this.sy(STEP_LIST_TOP) && mouseY < this.panelTop + this.stepListBottom;
    }

    private int clickedItemRow(double mouseY) {
        return ((int) mouseY - (this.panelTop + this.sy(ITEM_LIST_TOP))) / this.sy(ITEM_ROW_HEIGHT);
    }

    private int clickedStepRow(double mouseY) {
        return ((int) mouseY - (this.panelTop + this.sy(STEP_LIST_TOP) + this.sy(4))) / this.sy(STEP_ROW_HEIGHT);
    }

    private int sx(int designPx) {
        return Math.max(1, Math.round(designPx * this.panelScale));
    }

    private int sy(int designPx) {
        return Math.max(1, Math.round(designPx * this.panelScale));
    }

    private float resolvePanelScale() {
        float widthScale = (this.width - 24.0F) / DESIGN_PANEL_WIDTH;
        float heightScale = (this.height - 24.0F) / DESIGN_PANEL_HEIGHT;
        return Math.max(0.92F, Math.min(1.85F, Math.min(widthScale, heightScale)));
    }
}
