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
import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishRoleRequirement;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaDefinition;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaScorer;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishTechnique;
import com.boaat.jazzy_cookin.recipe.KitchenInputRequirement;
import com.boaat.jazzy_cookin.recipe.KitchenProcessOutcome;
import com.boaat.jazzy_cookin.recipe.KitchenProcessOutput;
import com.boaat.jazzy_cookin.recipe.KitchenPlateRecipe;
import com.boaat.jazzy_cookin.recipe.KitchenProcessRecipe;
import com.boaat.jazzy_cookin.registry.JazzyItems;
import com.boaat.jazzy_cookin.registry.JazzyItems.IngredientId;
import com.boaat.jazzy_cookin.registry.JazzyRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        Set<OutputKey> consumedOutputKeys = new LinkedHashSet<>();

        for (RecipeHolder<KitchenProcessRecipe> holder : recipeManager.getAllRecipesFor(JazzyRecipes.KITCHEN_PROCESS_TYPE.get())) {
            KitchenProcessRecipe recipe = holder.value();
            builders.add(processBuilder(holder.id().toString(), recipe, recipe.output(), KitchenOutcomeBand.IDEAL));
            collectDependencyKeys(consumedOutputKeys, recipe.inputs());
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
            collectDependencyKeys(consumedOutputKeys, recipe.inputs());
        }

        addSchemaGuides(builders, consumedOutputKeys);

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
        groupsByOutput.keySet().stream()
                .filter(output -> !consumedOutputKeys.contains(output))
                .filter(output -> shouldCatalogOutput(groupsByOutput.get(output)))
                .forEach(output -> statesByItem.computeIfAbsent(output.itemId(), key -> new LinkedHashSet<>()).add(output.state()));

        List<CatalogEntry> catalog = statesByItem.entrySet().stream()
                .map(entry -> {
                    Item item = BuiltInRegistries.ITEM.get(entry.getKey());
                    return new CatalogEntry(
                            entry.getKey(),
                            item,
                            new ItemStack(item).getHoverName().getString(),
                            entry.getValue().stream().sorted(STATE_ORDER).toList()
                    );
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

    private static void addSchemaGuides(List<StepOptionBuilder> builders, Set<OutputKey> consumedOutputKeys) {
        for (DishSchemaDefinition schema : DishSchemaScorer.schemas()) {
            StepOptionBuilder builder = schemaGuideBuilder(schema);
            if (builder == null) {
                continue;
            }
            builders.add(builder);
            if (builder.kind == StepKind.PLATE) {
                builder.requirements.stream()
                        .map(Requirement::dependencyKey)
                        .filter(Objects::nonNull)
                        .forEach(consumedOutputKeys::add);
            }
        }
    }

    private static @Nullable StepOptionBuilder schemaGuideBuilder(DishSchemaDefinition schema) {
        Item result = BuiltInRegistries.ITEM.get(schema.result());
        if (result == Items.AIR || !RecipeBookDisplayUtil.isModItem(result)) {
            return null;
        }

        IngredientState outputState = RecipeBookDisplayUtil.defaultStateForItem(result);
        boolean platedGuide = schema.requiredTechniques().contains(DishTechnique.PLATED);
        List<Requirement> requirements = platedGuide
                ? schemaPlatingRequirements(schema)
                : schemaRoleRequirements(schema.requiredRoles());
        DishTechnique primaryTechnique = primaryTechnique(schema);
        StationType station = stationFor(primaryTechnique);
        KitchenMethod method = methodFor(primaryTechnique, schema);
        ToolProfile preferredTool = preferredToolFor(primaryTechnique, schema);
        List<ToolProfile> allowedTools = allowedToolsFor(primaryTechnique, preferredTool);

        return new StepOptionBuilder(
                "schema:" + schema.key(),
                platedGuide ? StepKind.PLATE : StepKind.PROCESS,
                schemaChainKey(schema),
                RecipeBookDisplayUtil.outputKey(result, outputState),
                RecipeBookDisplayUtil.displayStack(new ItemStack(result), outputState, 1),
                requirements,
                List.of(),
                station,
                method,
                preferredTool,
                allowedTools,
                preferredTool != null,
                durationFor(primaryTechnique),
                preferredHeatFor(primaryTechnique),
                minimumHeatFor(primaryTechnique),
                maximumHeatFor(primaryTechnique),
                primaryTechnique == DishTechnique.BAKED,
                false,
                false,
                ProcessMode.ACTIVE,
                schemaNotes(schema, primaryTechnique, platedGuide)
        );
    }

    private static String schemaChainKey(DishSchemaDefinition schema) {
        return "schema:" + schemaFamilyKey(schema.key());
    }

    private static String schemaFamilyKey(String schemaKey) {
        return switch (schemaKey) {
            case "tomato_soup_base" -> "creamy_tomato_soup";
            case "hummus_prep" -> "hummus_plate";
            case "falafel_prep" -> "falafel_plate";
            case "confetti_rice_prep" -> "confetti_rice_with_fish";
            default -> schemaKey.endsWith("_prep")
                    ? schemaKey.substring(0, schemaKey.length() - "_prep".length())
                    : schemaKey;
        };
    }

    private static DishTechnique primaryTechnique(DishSchemaDefinition schema) {
        if (schema.requiredTechniques().contains(DishTechnique.PLATED)) {
            return DishTechnique.PLATED;
        }
        if (schema.requiredTechniques().contains(DishTechnique.PAN_FRIED)) {
            return DishTechnique.PAN_FRIED;
        }
        if (schema.requiredTechniques().contains(DishTechnique.SIMMERED)) {
            return DishTechnique.SIMMERED;
        }
        if (schema.requiredTechniques().contains(DishTechnique.BAKED)) {
            return DishTechnique.BAKED;
        }
        if (schema.requiredTechniques().contains(DishTechnique.MIXED)) {
            return DishTechnique.MIXED;
        }
        if (schema.requiredTechniques().contains(DishTechnique.CUT) || schema.requiredTechniques().contains(DishTechnique.PREPPED)) {
            return DishTechnique.CUT;
        }
        if (schema.requiredTechniques().contains(DishTechnique.RESTED)) {
            return DishTechnique.RESTED;
        }
        return schema.requiredTechniques().isEmpty() ? DishTechnique.PREPPED : schema.requiredTechniques().get(0);
    }

    private static StationType stationFor(DishTechnique technique) {
        return switch (technique) {
            case PLATED -> StationType.PLATING_STATION;
            case PAN_FRIED, SIMMERED -> StationType.STOVE;
            case BAKED -> StationType.OVEN;
            case MIXED -> StationType.MIXING_BOWL;
            case CUT, PREPPED -> StationType.PREP_TABLE;
            case RESTED -> StationType.RESTING_BOARD;
        };
    }

    private static KitchenMethod methodFor(DishTechnique technique, DishSchemaDefinition schema) {
        return switch (technique) {
            case PLATED -> KitchenMethod.PLATE;
            case PAN_FRIED -> KitchenMethod.PAN_FRY;
            case SIMMERED -> KitchenMethod.SIMMER;
            case BAKED -> KitchenMethod.BAKE;
            case MIXED -> schema.requiredRoles().stream().anyMatch(role -> role.role().getSerializedName().equals("grain"))
                    ? KitchenMethod.KNEAD
                    : KitchenMethod.MIX;
            case CUT, PREPPED -> KitchenMethod.CUT;
            case RESTED -> KitchenMethod.REST;
        };
    }

    private static @Nullable ToolProfile preferredToolFor(DishTechnique technique, DishSchemaDefinition schema) {
        return switch (technique) {
            case PAN_FRIED -> ToolProfile.FRYING_SKILLET;
            case SIMMERED -> ToolProfile.SAUCEPAN;
            case BAKED -> schema.key().contains("pie") ? ToolProfile.PIE_TIN : ToolProfile.BAKING_TRAY;
            case MIXED -> schema.requiredRoles().stream().anyMatch(role -> role.role().getSerializedName().equals("grain"))
                    ? ToolProfile.ROLLING_PIN
                    : ToolProfile.WHISK;
            case CUT, PREPPED -> ToolProfile.CHEF_KNIFE;
            default -> null;
        };
    }

    private static List<ToolProfile> allowedToolsFor(DishTechnique technique, @Nullable ToolProfile preferredTool) {
        if (preferredTool == null) {
            return List.of();
        }
        return switch (technique) {
            case PAN_FRIED -> List.of(ToolProfile.PAN, ToolProfile.SKILLET, ToolProfile.FRYING_SKILLET);
            case SIMMERED -> List.of(ToolProfile.POT, ToolProfile.STOCK_POT, ToolProfile.SAUCEPAN);
            case BAKED -> preferredTool == ToolProfile.PIE_TIN
                    ? List.of(ToolProfile.PIE_TIN, ToolProfile.BAKING_TRAY)
                    : List.of(ToolProfile.BAKING_TRAY);
            case MIXED -> preferredTool == ToolProfile.ROLLING_PIN
                    ? List.of(ToolProfile.ROLLING_PIN, ToolProfile.WHISK)
                    : List.of(ToolProfile.WHISK);
            case CUT, PREPPED -> List.of(ToolProfile.KNIFE, ToolProfile.CHEF_KNIFE, ToolProfile.PARING_KNIFE);
            default -> List.of(preferredTool);
        };
    }

    private static int durationFor(DishTechnique technique) {
        return switch (technique) {
            case PLATED -> 24;
            case PAN_FRIED -> 150;
            case SIMMERED -> 220;
            case BAKED -> 260;
            case MIXED -> 60;
            case CUT, PREPPED -> 36;
            case RESTED -> 80;
        };
    }

    private static HeatLevel preferredHeatFor(DishTechnique technique) {
        return switch (technique) {
            case PAN_FRIED, SIMMERED, BAKED -> HeatLevel.MEDIUM;
            default -> HeatLevel.OFF;
        };
    }

    private static HeatLevel minimumHeatFor(DishTechnique technique) {
        return switch (technique) {
            case PAN_FRIED, SIMMERED, BAKED -> HeatLevel.LOW;
            default -> HeatLevel.OFF;
        };
    }

    private static HeatLevel maximumHeatFor(DishTechnique technique) {
        return switch (technique) {
            case PAN_FRIED, SIMMERED, BAKED -> HeatLevel.HIGH;
            default -> HeatLevel.OFF;
        };
    }

    private static List<Requirement> schemaRoleRequirements(List<DishRoleRequirement> roles) {
        List<Requirement> requirements = new ArrayList<>();
        for (DishRoleRequirement role : roles) {
            List<ItemStack> choices = exampleStacksFor(role);
            if (choices.isEmpty()) {
                continue;
            }
            IngredientState state = RecipeBookDisplayUtil.defaultStateForItem(choices.get(0).getItem());
            OutputKey dependency = choices.size() == 1 && RecipeBookDisplayUtil.isModItem(choices.get(0).getItem())
                    ? RecipeBookDisplayUtil.outputKey(choices.get(0), state)
                    : null;
            requirements.add(new Requirement(choices, state, 1, dependency));
        }
        return requirements;
    }

    private static List<Requirement> schemaPlatingRequirements(DishSchemaDefinition schema) {
        List<Requirement> requirements = new ArrayList<>();
        Requirement prepared = preparedDishRequirement(schema.key());
        if (prepared != null) {
            requirements.add(prepared);
        } else {
            requirements.addAll(schemaRoleRequirements(schema.requiredRoles()));
        }
        return requirements;
    }

    private static @Nullable Requirement preparedDishRequirement(String schemaKey) {
        Item item = preparedDishItem(schemaKey);
        if (item == Items.AIR) {
            return null;
        }
        IngredientState state = "sliceable_pie".equals(schemaKey)
                ? IngredientState.COOLED_PIE
                : RecipeBookDisplayUtil.defaultStateForItem(item);
        ItemStack stack = RecipeBookDisplayUtil.displayStack(new ItemStack(item), state, 1);
        return new Requirement(List.of(stack), state, 1, RecipeBookDisplayUtil.outputKey(item, state));
    }

    private static Item preparedDishItem(String schemaKey) {
        Item explicit = switch (schemaKey) {
            case "creamy_tomato_soup" -> JazzyItems.TOMATO_SOUP_BASE.get();
            case "sliceable_pie" -> JazzyItems.ASSEMBLED_TRAY_PIE.get();
            case "hummus_plate" -> JazzyItems.HUMMUS_PREP.get();
            case "falafel_plate" -> JazzyItems.FALAFEL_PREP.get();
            case "confetti_rice_with_fish" -> JazzyItems.CONFETTI_RICE_PREP.get();
            default -> Items.AIR;
        };
        if (explicit != Items.AIR) {
            return explicit;
        }

        String baseKey = schemaKey.endsWith("_plate")
                ? schemaKey.substring(0, schemaKey.length() - "_plate".length())
                : schemaKey;
        Item inferred = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("jazzycookin", baseKey + "_prep"));
        return inferred == Items.AIR ? Items.AIR : inferred;
    }

    private static List<ItemStack> exampleStacksFor(DishRoleRequirement role) {
        LinkedHashSet<Item> items = new LinkedHashSet<>();
        role.allTraits().forEach(trait -> addExamplesForTrait(items, trait));
        role.anyTraits().forEach(trait -> addExamplesForTrait(items, trait));
        return items.stream()
                .limit(5)
                .map(item -> RecipeBookDisplayUtil.displayStack(new ItemStack(item), RecipeBookDisplayUtil.defaultStateForItem(item), 1))
                .filter(stack -> !stack.isEmpty())
                .toList();
    }

    private static void addExamplesForTrait(LinkedHashSet<Item> items, FoodTrait trait) {
        switch (trait) {
            case EGG -> addIngredients(items, IngredientId.EGGS);
            case CHICKEN -> addIngredients(items, IngredientId.CHICKEN);
            case PROTEIN, ANIMAL_PROTEIN -> addIngredients(items, IngredientId.CHICKEN, IngredientId.FISH_FILLET, IngredientId.EGGS);
            case PLANT_PROTEIN, LEGUME -> addIngredients(items, IngredientId.LENTILS, IngredientId.CHICKPEAS, IngredientId.TOFU);
            case SOY -> addIngredients(items, IngredientId.TOFU, IngredientId.SOY_SAUCE);
            case FLOUR, WHEAT -> addIngredients(items, IngredientId.ALL_PURPOSE_FLOUR, IngredientId.BREAD_FLOUR, IngredientId.WHOLE_WHEAT_FLOUR);
            case GRAIN -> addIngredients(items, IngredientId.RICE, IngredientId.ALL_PURPOSE_FLOUR, IngredientId.BREAD);
            case RICE -> addIngredients(items, IngredientId.RICE);
            case BREAD -> addIngredients(items, IngredientId.BREAD);
            case FAT, OIL -> addIngredients(items, IngredientId.OLIVE_OIL, IngredientId.BUTTER, IngredientId.VEGETABLE_OIL);
            case DAIRY -> addIngredients(items, IngredientId.SHELF_STABLE_CREAM, IngredientId.CHEESE, IngredientId.BUTTER);
            case SALT -> addIngredients(items, IngredientId.TABLE_SALT, IngredientId.KOSHER_SALT, IngredientId.SEA_SALT);
            case SPICE -> addIngredients(items, IngredientId.CURRY_POWDER, IngredientId.BLACK_PEPPER, IngredientId.PAPRIKA);
            case PEPPER -> addIngredients(items, IngredientId.BLACK_PEPPER, IngredientId.JALAPENOS, IngredientId.RED_PEPPER);
            case HERB -> addIngredients(items, IngredientId.BASIL, IngredientId.ROSEMARY, IngredientId.PARSLEY);
            case ALLIUM, AROMATIC -> addIngredients(items, IngredientId.ONIONS, IngredientId.GARLIC, IngredientId.GINGER);
            case TOMATO -> addIngredients(items, IngredientId.TOMATOES, IngredientId.CANNED_TOMATOES, IngredientId.TOMATO_SAUCE);
            case VEGETABLE -> addIngredients(items, IngredientId.TOMATOES, IngredientId.CARROTS, IngredientId.ONIONS);
            case FRUIT -> addIngredients(items, IngredientId.APPLES, IngredientId.LEMONS);
            case SWEETENER, SYRUP -> addIngredients(items, IngredientId.WHITE_SUGAR, IngredientId.HONEY, IngredientId.MAPLE_SYRUP);
            case ACIDIC -> addIngredients(items, IngredientId.LEMONS, IngredientId.WHITE_VINEGAR, IngredientId.BALSAMIC_VINEGAR);
            case SAUCE -> addIngredients(items, IngredientId.BROTH, IngredientId.STOCK, IngredientId.TOMATO_SAUCE);
            case COCONUT -> addIngredients(items, IngredientId.COCONUT_MILK, IngredientId.COCONUT_OIL);
            case STARCH -> addIngredients(items, IngredientId.POTATOES, IngredientId.RICE, IngredientId.CORNMEAL);
            case CORN -> addIngredients(items, IngredientId.CORNMEAL, IngredientId.CORN_FLOUR);
            case NUT -> addIngredients(items, IngredientId.ALMONDS, IngredientId.WALNUTS, IngredientId.PECANS);
            case PRESERVE -> addIngredients(items, IngredientId.JAM, IngredientId.JELLY, IngredientId.SYRUP_PRESERVES);
            case CONDIMENT -> addIngredients(items, IngredientId.SOY_SAUCE, IngredientId.MUSTARD, IngredientId.KETCHUP);
            default -> {
            }
        }
    }

    private static void addIngredients(LinkedHashSet<Item> items, IngredientId... ingredientIds) {
        for (IngredientId ingredientId : ingredientIds) {
            items.add(JazzyItems.ingredient(ingredientId).get());
        }
    }

    private static List<String> schemaNotes(DishSchemaDefinition schema, DishTechnique primaryTechnique, boolean platedGuide) {
        List<String> notes = new ArrayList<>();
        if (!schema.requiredRoles().isEmpty()) {
            notes.add("Match required roles: " + roleSummary(schema.requiredRoles()) + ".");
        }
        if (!schema.optionalRoles().isEmpty()) {
            notes.add("Optional roles can improve the grade: " + roleSummary(schema.optionalRoles()) + ".");
        }
        if (!schema.requiredTechniques().isEmpty()) {
            notes.add("Technique targets: " + schema.requiredTechniques().stream()
                    .map(DishTechnique::getSerializedName)
                    .collect(Collectors.joining(", ")) + ".");
        }
        notes.add("Flexible schema guide: examples are suggestions, and matching substitutions can still score well.");
        if (primaryTechnique == DishTechnique.BAKED) {
            notes.add("Preheat the oven and manage bake time to avoid pale or burned results.");
        } else if (primaryTechnique == DishTechnique.PAN_FRIED) {
            notes.add("Manage heat, browning, and doneness; flipping or stirring helps the cooking score.");
        } else if (primaryTechnique == DishTechnique.SIMMERED) {
            notes.add("Use enough liquid, simmer steadily, and season near the end.");
        } else if (platedGuide) {
            notes.add("Plate a matching prepared component; garnish and the right container improve presentation.");
        }
        return List.copyOf(notes);
    }

    private static String roleSummary(List<DishRoleRequirement> roles) {
        return roles.stream()
                .map(role -> role.role().getSerializedName())
                .distinct()
                .collect(Collectors.joining(", "));
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

    private static void collectDependencyKeys(Set<OutputKey> consumedOutputKeys, List<KitchenInputRequirement> inputs) {
        for (KitchenInputRequirement input : inputs) {
            Arrays.stream(input.ingredient().getItems())
                    .filter(stack -> !stack.isEmpty() && RecipeBookDisplayUtil.isModItem(stack.getItem()))
                    .map(stack -> RecipeBookDisplayUtil.outputKey(stack, input.requiredState()))
                    .forEach(consumedOutputKeys::add);
        }
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

    private static boolean shouldCatalogOutput(@Nullable Map<String, OptionGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return false;
        }
        return groups.values().stream()
                .flatMap(group -> group.options().stream())
                .map(StepOption::kind)
                .anyMatch(kind -> kind == StepKind.PROCESS || kind == StepKind.PLATE);
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
