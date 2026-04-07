package com.boaat.jazzy_cookin.recipebook;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.KitchenOutcomeBand;
import com.boaat.jazzy_cookin.kitchen.KitchenSourceProfile;
import com.boaat.jazzy_cookin.kitchen.ProcessMode;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.boaat.jazzy_cookin.recipe.KitchenInputRequirement;
import com.boaat.jazzy_cookin.recipe.KitchenProcessOutcome;
import com.boaat.jazzy_cookin.recipe.KitchenProcessOutput;
import com.boaat.jazzy_cookin.recipe.KitchenPlateRecipe;
import com.boaat.jazzy_cookin.recipe.KitchenProcessRecipe;
import com.boaat.jazzy_cookin.registry.JazzyRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class JazzyRecipeBookPlanner {
    public enum StepKind {
        CRAFT,
        SOURCE,
        PROCESS,
        PLATE
    }

    public record OutputKey(ResourceLocation itemId, IngredientState state) {
        public String stableId() {
            return this.itemId + "@" + this.state.getSerializedName();
        }
    }

    public record Requirement(
            List<ItemStack> choices,
            IngredientState requiredState,
            int count,
            @Nullable OutputKey dependencyKey
    ) {
        public boolean hasDependency() {
            return this.dependencyKey != null;
        }
    }

    public record StepOption(
            String optionId,
            StepKind kind,
            String chainKey,
            OutputKey outputKey,
            List<Requirement> requirements,
            List<OutputKey> prerequisites,
            ItemStack outputStack,
            @Nullable StationType station,
            @Nullable KitchenMethod method,
            @Nullable ToolProfile preferredTool,
            List<ToolProfile> allowedTools,
            boolean toolRequired,
            int durationTicks,
            HeatLevel preferredHeat,
            HeatLevel minimumHeat,
            HeatLevel maximumHeat,
            boolean requiresPreheat,
            boolean nearbyWater,
            boolean sheltered,
            ProcessMode mode,
            List<String> notes
    ) {
        public List<Requirement> recursiveRequirements() {
            return this.requirements.stream().filter(Requirement::hasDependency).toList();
        }
    }

    public record PlanStep(
            String id,
            String chainKey,
            StepKind kind,
            OutputKey outputKey,
            ItemStack outputStack,
            List<StepOption> options,
            List<String> dependencyStepIds
    ) {
        public boolean expandable() {
            return !this.dependencyStepIds.isEmpty();
        }
    }

    public record Plan(
            ResourceLocation itemId,
            IngredientState state,
            String chainKey,
            ItemStack targetStack,
            String rootStepId,
            List<PlanStep> steps
    ) {
        public Optional<PlanStep> step(String stepId) {
            return this.steps.stream().filter(step -> step.id().equals(stepId)).findFirst();
        }

        public int indexOfStep(String stepId) {
            for (int index = 0; index < this.steps.size(); index++) {
                if (this.steps.get(index).id().equals(stepId)) {
                    return index;
                }
            }
            return -1;
        }

        public int currentStepIndex(Set<String> completedStepIds) {
            for (int index = 0; index < this.steps.size(); index++) {
                if (!completedStepIds.contains(this.steps.get(index).id())) {
                    return index;
                }
            }
            return this.steps.isEmpty() ? -1 : this.steps.size() - 1;
        }

        public @Nullable PlanStep currentStep(Set<String> completedStepIds) {
            if (this.steps.isEmpty()) {
                return null;
            }
            for (PlanStep step : this.steps) {
                if (!completedStepIds.contains(step.id())) {
                    return step;
                }
            }
            return this.steps.get(this.steps.size() - 1);
        }

        public int focusedStepIndex(Set<String> completedStepIds, @Nullable String preferredStepId) {
            PlanStep step = this.focusedStep(completedStepIds, preferredStepId);
            return step == null ? -1 : Math.max(0, this.indexOfStep(step.id()));
        }

        public @Nullable PlanStep focusedStep(Set<String> completedStepIds, @Nullable String preferredStepId) {
            if (this.steps.isEmpty()) {
                return null;
            }
            if (preferredStepId != null && !preferredStepId.isBlank()) {
                int startIndex = this.indexOfStep(preferredStepId);
                if (startIndex >= 0) {
                    for (int index = startIndex; index < this.steps.size(); index++) {
                        PlanStep step = this.steps.get(index);
                        if (!completedStepIds.contains(step.id())) {
                            return step;
                        }
                    }
                }
            }
            return this.currentStep(completedStepIds);
        }

        public boolean isComplete(Set<String> completedStepIds) {
            return !this.steps.isEmpty() && this.steps.stream().allMatch(step -> completedStepIds.contains(step.id()));
        }
    }

    public record CatalogEntry(ResourceLocation itemId, Item item, String displayName, List<IngredientState> producibleStates) {
    }

    private record OptionGroup(OutputKey outputKey, String chainKey, List<StepOption> options) {
        private String stepId() {
            return JazzyRecipeBookPlanner.stepId(this.chainKey, this.outputKey);
        }

        private PlanStep toStep(List<String> dependencyStepIds) {
            StepKind mergedKind = this.options.stream()
                    .map(StepOption::kind)
                    .findFirst()
                    .orElse(StepKind.CRAFT);
            ItemStack outputStack = this.options.stream()
                    .map(StepOption::outputStack)
                    .findFirst()
                    .map(ItemStack::copy)
                    .orElse(ItemStack.EMPTY);
            return new PlanStep(this.stepId(), this.chainKey, mergedKind, this.outputKey, outputStack, List.copyOf(this.options), List.copyOf(dependencyStepIds));
        }
    }

    private record StepOptionBuilder(
            String optionId,
            StepKind kind,
            String chainKey,
            OutputKey outputKey,
            ItemStack outputStack,
            List<Requirement> requirements,
            List<OutputKey> prerequisites,
            @Nullable StationType station,
            @Nullable KitchenMethod method,
            @Nullable ToolProfile preferredTool,
            List<ToolProfile> allowedTools,
            boolean toolRequired,
            int durationTicks,
            HeatLevel preferredHeat,
            HeatLevel minimumHeat,
            HeatLevel maximumHeat,
            boolean requiresPreheat,
            boolean nearbyWater,
            boolean sheltered,
            ProcessMode mode,
            List<String> notes
    ) {
        private StepOption build() {
            return new StepOption(
                    this.optionId,
                    this.kind,
                    Objects.requireNonNullElse(this.chainKey, ""),
                    this.outputKey,
                    List.copyOf(this.requirements),
                    List.copyOf(this.prerequisites),
                    this.outputStack.copy(),
                    this.station,
                    this.method,
                    this.preferredTool,
                    List.copyOf(this.allowedTools),
                    this.toolRequired,
                    this.durationTicks,
                    this.preferredHeat,
                    this.minimumHeat,
                    this.maximumHeat,
                    this.requiresPreheat,
                    this.nearbyWater,
                    this.sheltered,
                    this.mode,
                    List.copyOf(this.notes)
            );
        }
    }

    private static final Comparator<IngredientState> STATE_ORDER = Comparator
            .comparingInt(JazzyRecipeBookPlanner::stateRank)
            .thenComparing(IngredientState::getSerializedName);

    private final Map<OutputKey, Map<String, OptionGroup>> groupsByOutput;
    private final Map<ResourceLocation, CatalogEntry> catalogByItem;
    private final List<CatalogEntry> catalog;

    private JazzyRecipeBookPlanner(
            Map<OutputKey, Map<String, OptionGroup>> groupsByOutput,
            Map<ResourceLocation, CatalogEntry> catalogByItem,
            List<CatalogEntry> catalog
    ) {
        this.groupsByOutput = groupsByOutput;
        this.catalogByItem = catalogByItem;
        this.catalog = catalog;
    }

    public static JazzyRecipeBookPlanner create(Level level) {
        return create(level.getRecipeManager(), level.registryAccess());
    }

    public static JazzyRecipeBookPlanner create(RecipeManager recipeManager, HolderLookup.Provider registries) {
        List<StepOptionBuilder> builders = new ArrayList<>();

        for (RecipeHolder<KitchenProcessRecipe> holder : recipeManager.getAllRecipesFor(JazzyRecipes.KITCHEN_PROCESS_TYPE.get())) {
            KitchenProcessRecipe recipe = holder.value();
            builders.add(processBuilder(holder.id().toString(), recipe, recipe.output(), KitchenOutcomeBand.IDEAL));
            for (KitchenProcessOutcome outcome : recipe.outcomes()) {
                builders.add(processBuilder(
                        holder.id() + "#" + outcome.band().getSerializedName(),
                        recipe,
                        outcome.output(),
                        outcome.band()
                ));
            }
        }

        for (RecipeHolder<KitchenPlateRecipe> holder : recipeManager.getAllRecipesFor(JazzyRecipes.KITCHEN_PLATE_TYPE.get())) {
            KitchenPlateRecipe recipe = holder.value();
            builders.add(new StepOptionBuilder(
                    holder.id().toString(),
                    StepKind.PLATE,
                    recipe.recipeBook().normalizedChainKey(),
                    RecipeBookDisplayUtil.outputKey(recipe.output().result(), recipe.output().state()),
                    RecipeBookDisplayUtil.displayOutput(recipe.output()),
                    requirementsFrom(recipe.inputs()),
                    List.of(),
                    StationType.PLATING_STATION,
                    KitchenMethod.PLATE,
                    null,
                    List.of(),
                    false,
                    24,
                    HeatLevel.OFF,
                    HeatLevel.OFF,
                    HeatLevel.OFF,
                    false,
                    false,
                    false,
                    ProcessMode.ACTIVE,
                    List.of("Plate the prepared dish with the serving pieces shown.")
            ));
        }

        for (RecipeHolder<CraftingRecipe> holder : recipeManager.getAllRecipesFor(RecipeType.CRAFTING)) {
            CraftingRecipe recipe = holder.value();
            ItemStack result = recipe.getResultItem(registries);
            if (result.isEmpty() || !RecipeBookDisplayUtil.isModItem(result.getItem())) {
                continue;
            }

            IngredientState state = RecipeBookDisplayUtil.defaultStateForItem(result.getItem());
            builders.add(new StepOptionBuilder(
                    holder.id().toString(),
                    StepKind.CRAFT,
                    "",
                    RecipeBookDisplayUtil.outputKey(result, state),
                    RecipeBookDisplayUtil.displayStack(result, state, result.getCount()),
                    craftingRequirements(recipe.getIngredients()),
                    List.of(),
                    null,
                    null,
                    null,
                    List.of(),
                    false,
                    0,
                    HeatLevel.OFF,
                    HeatLevel.OFF,
                    HeatLevel.OFF,
                    false,
                    false,
                    false,
                    ProcessMode.ACTIVE,
                    List.of("Craft this item with a crafting table recipe.")
            ));
        }

        addSourceGuide(builders, SourceGuideRegistry.appleSaplingGuide(), "apple_sapling");
        for (KitchenSourceProfile profile : KitchenSourceProfile.values()) {
            addSourceGuide(builders, SourceGuideRegistry.guideFor(profile), profile.getSerializedName());
        }

        Map<OutputKey, Map<String, List<StepOption>>> groupedOptions = new HashMap<>();
        for (StepOptionBuilder builder : builders) {
            StepOption option = builder.build();
            groupedOptions
                    .computeIfAbsent(option.outputKey(), key -> new HashMap<>())
                    .computeIfAbsent(option.chainKey(), key -> new ArrayList<>())
                    .add(option);
        }

        Map<OutputKey, Map<String, OptionGroup>> groupsByOutput = new HashMap<>();
        groupedOptions.forEach((outputKey, byChain) -> {
            Map<String, OptionGroup> groups = new HashMap<>();
            byChain.forEach((chainKey, options) -> groups.put(chainKey, new OptionGroup(
                    outputKey,
                    chainKey,
                    options.stream().sorted(Comparator.comparing(StepOption::optionId)).toList()
            )));
            groupsByOutput.put(outputKey, groups);
        });

        Map<ResourceLocation, Set<IngredientState>> statesByItem = new HashMap<>();
        groupsByOutput.keySet().forEach(output -> statesByItem.computeIfAbsent(output.itemId(), key -> new LinkedHashSet<>()).add(output.state()));

        List<CatalogEntry> catalog = BuiltInRegistries.ITEM.stream()
                .filter(RecipeBookDisplayUtil::isModItem)
                .map(item -> {
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
                    Set<IngredientState> states = statesByItem.computeIfAbsent(itemId, key -> new LinkedHashSet<>());
                    if (states.isEmpty()) {
                        states.add(RecipeBookDisplayUtil.defaultStateForItem(item));
                    }
                    return new CatalogEntry(itemId, item, new ItemStack(item).getHoverName().getString(), states.stream().sorted(STATE_ORDER).toList());
                })
                .sorted(Comparator.comparing(CatalogEntry::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        Map<ResourceLocation, CatalogEntry> catalogByItem = catalog.stream()
                .collect(Collectors.toMap(CatalogEntry::itemId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        return new JazzyRecipeBookPlanner(groupsByOutput, catalogByItem, catalog);
    }

    public List<CatalogEntry> catalog() {
        return this.catalog;
    }

    public Optional<CatalogEntry> catalogEntry(ResourceLocation itemId) {
        return Optional.ofNullable(this.catalogByItem.get(itemId));
    }

    public List<IngredientState> producibleStates(ResourceLocation itemId) {
        return this.catalogEntry(itemId).map(CatalogEntry::producibleStates).orElse(List.of());
    }

    public List<String> chainKeysFor(OutputKey outputKey) {
        Map<String, OptionGroup> groups = this.groupsByOutput.get(outputKey);
        if (groups == null) {
            return List.of();
        }
        return groups.keySet().stream().sorted(Comparator.comparing(key -> key.isBlank() ? "~" : key)).toList();
    }

    public List<Plan> plansFor(ResourceLocation itemId, IngredientState state) {
        return this.plansFor(new OutputKey(itemId, state), null);
    }

    public Optional<Plan> planFor(JazzyRecipeBookSelection selection) {
        return this.plansFor(new OutputKey(selection.itemId(), selection.state()), selection.normalizedChainKey()).stream().findFirst();
    }

    public boolean hasPlan(OutputKey outputKey, @Nullable String preferredChainKey) {
        return this.resolveGroup(outputKey, preferredChainKey) != null;
    }

    public static String stepId(String chainKey, OutputKey outputKey) {
        String branch = chainKey == null || chainKey.isBlank() ? "shared" : chainKey;
        return branch + "|" + outputKey.stableId();
    }

    private List<Plan> plansFor(OutputKey target, @Nullable String preferredChainKey) {
        Map<String, OptionGroup> groups = this.groupsByOutput.get(target);
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }

        if (preferredChainKey != null && !preferredChainKey.isBlank()) {
            OptionGroup resolved = this.resolveGroup(target, preferredChainKey);
            return resolved == null ? List.of() : List.of(this.buildPlan(resolved));
        }

        if (groups.size() == 1) {
            return List.of(this.buildPlan(groups.values().iterator().next()));
        }

        return groups.values().stream()
                .sorted(Comparator.comparing(group -> group.chainKey().isBlank() ? "~" : group.chainKey()))
                .map(this::buildPlan)
                .toList();
    }

    private Plan buildPlan(OptionGroup rootGroup) {
        LinkedHashMap<String, PlanStep> orderedSteps = new LinkedHashMap<>();
        Set<String> visiting = new HashSet<>();
        this.collectSteps(rootGroup.outputKey(), rootGroup.chainKey(), orderedSteps, visiting);
        LinkedHashMap<String, PlanStep> enrichedSteps = new LinkedHashMap<>();
        for (PlanStep step : orderedSteps.values()) {
            OptionGroup group = this.resolveGroup(step.outputKey(), step.chainKey());
            List<String> dependencyStepIds = group == null ? List.of() : this.dependencyStepIds(group);
            enrichedSteps.put(step.id(), new PlanStep(
                    step.id(),
                    step.chainKey(),
                    step.kind(),
                    step.outputKey(),
                    step.outputStack().copy(),
                    step.options(),
                    dependencyStepIds
            ));
        }
        return new Plan(
                rootGroup.outputKey().itemId(),
                rootGroup.outputKey().state(),
                rootGroup.chainKey(),
                rootGroup.toStep(List.of()).outputStack().copy(),
                rootGroup.stepId(),
                List.copyOf(enrichedSteps.values())
        );
    }

    private void collectSteps(OutputKey outputKey, @Nullable String preferredChainKey, LinkedHashMap<String, PlanStep> orderedSteps, Set<String> visiting) {
        OptionGroup group = this.resolveGroup(outputKey, preferredChainKey);
        if (group == null) {
            return;
        }

        String visitKey = group.stepId();
        if (!visiting.add(visitKey)) {
            return;
        }

        StepOption canonicalOption = group.options().get(0);
        for (OutputKey prerequisite : canonicalOption.prerequisites()) {
            this.collectSteps(prerequisite, preferredChainKey, orderedSteps, visiting);
        }
        for (Requirement requirement : canonicalOption.recursiveRequirements()) {
            this.collectSteps(requirement.dependencyKey(), preferredChainKey, orderedSteps, visiting);
        }

        orderedSteps.putIfAbsent(group.stepId(), group.toStep(List.of()));
        visiting.remove(visitKey);
    }

    private List<String> dependencyStepIds(OptionGroup group) {
        LinkedHashSet<String> dependencyIds = new LinkedHashSet<>();
        StepOption canonicalOption = group.options().get(0);
        for (OutputKey prerequisite : canonicalOption.prerequisites()) {
            OptionGroup dependency = this.resolveGroup(prerequisite, group.chainKey());
            if (dependency != null) {
                dependencyIds.add(dependency.stepId());
            }
        }
        for (Requirement requirement : canonicalOption.recursiveRequirements()) {
            OptionGroup dependency = this.resolveGroup(requirement.dependencyKey(), group.chainKey());
            if (dependency != null) {
                dependencyIds.add(dependency.stepId());
            }
        }
        return List.copyOf(dependencyIds);
    }

    private @Nullable OptionGroup resolveGroup(OutputKey outputKey, @Nullable String preferredChainKey) {
        Map<String, OptionGroup> groups = this.groupsByOutput.get(outputKey);
        if (groups == null || groups.isEmpty()) {
            return null;
        }
        if (preferredChainKey != null && !preferredChainKey.isBlank() && groups.containsKey(preferredChainKey)) {
            return groups.get(preferredChainKey);
        }
        if (groups.size() == 1) {
            return groups.values().iterator().next();
        }
        if (groups.containsKey("")) {
            return groups.get("");
        }
        return null;
    }

    private static StepOptionBuilder processBuilder(
            String optionId,
            KitchenProcessRecipe recipe,
            KitchenProcessOutput output,
            KitchenOutcomeBand band
    ) {
        return new StepOptionBuilder(
                optionId,
                StepKind.PROCESS,
                recipe.recipeBook().normalizedChainKey(),
                RecipeBookDisplayUtil.outputKey(output.result(), output.state()),
                RecipeBookDisplayUtil.displayOutput(output),
                requirementsFrom(recipe.inputs()),
                List.of(),
                recipe.station(),
                recipe.method(),
                recipe.preferredTool().orElse(null),
                List.copyOf(recipe.allowedTools()),
                recipe.toolRequired(),
                recipe.effectiveDuration(),
                recipe.preferredHeat(),
                recipe.minimumHeat(),
                recipe.maximumHeat(),
                recipe.requiresPreheat(),
                recipe.requiresNearbyWater(),
                recipe.environmentRequirements().sheltered(),
                recipe.mode(),
                processNotes(recipe, band)
        );
    }

    private static List<String> processNotes(KitchenProcessRecipe recipe, KitchenOutcomeBand band) {
        List<String> notes = new ArrayList<>();
        if (band == KitchenOutcomeBand.UNDER) {
            notes.add("Aim for the underworked or undercooked result for this state.");
        } else if (band == KitchenOutcomeBand.OVER) {
            notes.add("Aim for the overworked or overcooked result for this state.");
        }
        if (!recipe.allowedToolsOrPreferred().isEmpty()) {
            notes.add("Use the listed tool profile for this step.");
        }
        if (recipe.usesHeat()) {
            notes.add("Run this step at the listed heat range.");
        }
        if (recipe.requiresPreheat()) {
            notes.add("Preheat the station before starting.");
        }
        if (recipe.requiresNearbyWater()) {
            notes.add("This station needs nearby water.");
        }
        if (recipe.environmentRequirements().sheltered()) {
            notes.add("Keep this step sheltered from open sky.");
        }
        return notes;
    }

    private static void addSourceGuide(List<StepOptionBuilder> builders, SourceGuideRegistry.SourceGuide guide, String chainKey) {
        ItemStack sourceStack = new ItemStack(guide.sourceItem().get());
        OutputKey sourceDependency = RecipeBookDisplayUtil.outputKey(sourceStack.getItem(), RecipeBookDisplayUtil.defaultStateForItem(sourceStack.getItem()));
        List<OutputKey> prerequisites = List.of(sourceDependency);

        for (SourceGuideRegistry.HarvestOutput output : guide.outputs()) {
            builders.add(new StepOptionBuilder(
                    "source:" + guide.key() + ":" + output.ingredientId().id(),
                    StepKind.SOURCE,
                    chainKey,
                    new OutputKey(output.itemId(), output.state()),
                    output.displayStack(),
                    List.of(),
                    prerequisites,
                    null,
                    null,
                    null,
                    List.of(),
                    false,
                    0,
                    HeatLevel.OFF,
                    HeatLevel.OFF,
                    HeatLevel.OFF,
                    false,
                    false,
                    false,
                    ProcessMode.ACTIVE,
                    sourceNotes(guide)
            ));
        }
    }

    private static List<String> sourceNotes(SourceGuideRegistry.SourceGuide guide) {
        List<String> notes = new ArrayList<>();
        notes.add(guide.summary());
        notes.addAll(guide.notes());
        if (guide.randomResult()) {
            notes.add("This source can yield different outputs, so harvest results are not guaranteed.");
        }
        return notes;
    }

    private static List<Requirement> requirementsFrom(List<KitchenInputRequirement> inputs) {
        return inputs.stream()
                .map(requirement -> requirementFromIngredient(requirement.ingredient(), requirement.requiredState(), requirement.count()))
                .toList();
    }

    private static List<Requirement> craftingRequirements(Collection<Ingredient> ingredients) {
        List<Requirement> expanded = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            if (!ingredient.isEmpty()) {
                expanded.add(requirementFromIngredient(ingredient, IngredientState.PANTRY_READY, 1));
            }
        }

        Map<String, List<Requirement>> grouped = expanded.stream().collect(Collectors.groupingBy(
                JazzyRecipeBookPlanner::requirementKey,
                LinkedHashMap::new,
                Collectors.toList()
        ));

        List<Requirement> collapsed = new ArrayList<>();
        grouped.forEach((key, group) -> {
            Requirement first = group.get(0);
            collapsed.add(new Requirement(first.choices(), first.requiredState(), group.size(), first.dependencyKey()));
        });
        return collapsed;
    }

    private static String requirementKey(Requirement requirement) {
        String choices = requirement.choices().stream()
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                .sorted()
                .collect(Collectors.joining(","));
        return requirement.requiredState().getSerializedName() + "|" + choices + "|" + requirement.hasDependency();
    }

    private static Requirement requirementFromIngredient(Ingredient ingredient, IngredientState requiredState, int count) {
        List<ItemStack> choices = Arrays.stream(ingredient.getItems())
                .map(stack -> RecipeBookDisplayUtil.displayStack(stack, requiredState, count))
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();

        OutputKey dependencyKey = null;
        if (choices.size() == 1 && RecipeBookDisplayUtil.isModItem(choices.get(0).getItem())) {
            dependencyKey = RecipeBookDisplayUtil.outputKey(choices.get(0), requiredState);
        }
        return new Requirement(choices, requiredState, count, dependencyKey);
    }

    private static int stateRank(IngredientState state) {
        if (state.isPlatedState()) {
            return 0;
        }
        if (state == IngredientState.RESTED || state == IngredientState.RESTED_BREAD || state == IngredientState.RESTED_PIE) {
            return 1;
        }
        if (state == IngredientState.COOLED || state == IngredientState.COOLED_PIE) {
            return 2;
        }
        if (isCookedState(state)) {
            return 3;
        }
        if (isPreparedState(state)) {
            return 4;
        }
        return 5;
    }

    private static boolean isCookedState(IngredientState state) {
        return switch (state) {
            case BOILED, SIMMERED, PAN_FRIED, DEEP_FRIED, BAKED, ROASTED, STEAMED, SMOKED, FERMENTED, GLAZED, FREEZE_DRIED, BAKED_BREAD,
                    BAKED_PIE, FRIED_PROTEIN, ROASTED_PROTEIN, BROILED_PROTEIN, SMOKED_PROTEIN, STEAMED_DUMPLINGS, STRAINED_SOUP,
                    SIMMERED_FILLING, HOT_PRESERVE, PORTIONED_MEAL -> true;
            default -> false;
        };
    }

    private static boolean isPreparedState(IngredientState state) {
        return switch (state) {
            case WASHED, WHOLE, PEELED, SLICED, CHOPPED, DICED, MINCED, ROUGH_CUT, STRAINED, CRUSHED, COARSE_PASTE, SMOOTH_PASTE, OVERWORKED_PASTE,
                    COARSE_POWDER, FINE_POWDER, MIXED, SMOOTH_MIXTURE, LUMPY_MIXTURE, UNDERWHISKED, WHISKED, OVERWHISKED, SEPARATED, SHAGGY_DOUGH,
                    ROUGH_DOUGH, DEVELOPING_DOUGH, DEVELOPED_DOUGH, SMOOTH_DOUGH, ELASTIC_DOUGH, OVERWORKED_DOUGH, BATTERED, STUFFED, MARINATED,
                    SHAPED_BASE, RAW_ASSEMBLED_PIE, RAW_ASSEMBLED_PIZZA, DOUGH, BREAD_DOUGH, SOUP_BASE, DUMPLING_FILLING, DUMPLING_DOUGH,
                    RAW_DUMPLINGS, STICKED_PRODUCT, ASSEMBLED_SANDWICH, MARINATED_PROTEIN, BATTER, BATTERED_PROTEIN, CLEANED_FISH, ROAST_CUT,
                    CHUNKY, SMOOTH, CREAMY, PASTE, MUSH, FRESH_JUICE, PULP -> true;
            default -> false;
        };
    }
}
