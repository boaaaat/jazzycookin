package com.boaat.jazzy_cookin.kitchen.sim.domain;

import java.util.List;
import java.util.Optional;

import com.boaat.jazzy_cookin.block.entity.KitchenStationBlockEntity;
import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.item.KitchenToolItem;
import com.boaat.jazzy_cookin.kitchen.DishEvaluation;
import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.kitchen.KitchenOutcomeBand;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.boaat.jazzy_cookin.recipebook.RecipeBookProgress;
import com.boaat.jazzy_cookin.recipebook.network.RecipeBookNetworking;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishRecognitionResult;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishSchema;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;
import com.boaat.jazzy_cookin.recipe.KitchenEnvironmentRequirements;
import com.boaat.jazzy_cookin.recipe.KitchenInputRequirement;
import com.boaat.jazzy_cookin.recipe.KitchenPlateInput;
import com.boaat.jazzy_cookin.recipe.KitchenPlateRecipe;
import com.boaat.jazzy_cookin.recipe.KitchenProcessInput;
import com.boaat.jazzy_cookin.recipe.KitchenProcessOutput;
import com.boaat.jazzy_cookin.recipe.KitchenProcessRecipe;
import com.boaat.jazzy_cookin.recipe.KitchenRecipeMatchPlan;
import com.boaat.jazzy_cookin.registry.JazzyRecipes;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

final class RecipeSimulationSupport {
    private static final int PLATE_DURATION_TICKS = 24;

    private RecipeSimulationSupport() {
    }

    static Optional<KitchenProcessRecipe> currentProcessRecipe(StationSimulationAccess access) {
        if (access.simulationLevel() == null) {
            return Optional.empty();
        }
        return JazzyRecipes.findProcessRecipeCandidate(
                access.simulationLevel(),
                access.simulationStationType(),
                inputStacks(access),
                access.simulationItem(access.toolSlot())
        );
    }

    static Optional<KitchenPlateRecipe> currentPlateRecipe(StationSimulationAccess access) {
        if (access.simulationLevel() == null) {
            return Optional.empty();
        }
        return JazzyRecipes.findPlateRecipe(access.simulationLevel(), inputStacks(access));
    }

    static int environmentStatus(StationSimulationAccess access) {
        if (access.simulationStationType() == com.boaat.jazzy_cookin.kitchen.StationType.PLATING_STATION) {
            return currentPlateRecipe(access).isPresent() ? 1 : 2;
        }
        Optional<KitchenProcessRecipe> recipe = currentProcessRecipe(access);
        if (recipe.isEmpty()) {
            return 2;
        }
        return environmentAllows(access, recipe.get()) ? 1 : 0;
    }

    static boolean startProcess(StationSimulationAccess access) {
        Optional<KitchenProcessRecipe> recipe = currentProcessRecipe(access);
        if (recipe.isEmpty() || access.simulationActive() || !environmentAllows(access, recipe.get()) || !canAcceptOutputs(access, recipe.get())) {
            return false;
        }
        access.simulationSetProgress(0, effectiveDuration(access, recipe.get()), true);
        access.simulationMarkChanged();
        return true;
    }

    static boolean startPlate(StationSimulationAccess access) {
        Optional<KitchenPlateRecipe> recipe = currentPlateRecipe(access);
        if (recipe.isEmpty() || access.simulationActive() || !canAcceptOutputs(access, recipe.get())) {
            return false;
        }
        access.simulationSetProgress(0, PLATE_DURATION_TICKS, true);
        access.simulationMarkChanged();
        return true;
    }

    static void serverTickProcess(StationSimulationAccess access) {
        if (!access.simulationActive()) {
            return;
        }
        Optional<KitchenProcessRecipe> recipe = currentProcessRecipe(access);
        if (recipe.isEmpty() || !environmentAllows(access, recipe.get()) || !canAcceptOutputs(access, recipe.get())) {
            stop(access);
            return;
        }

        int nextProgress = access.simulationProgress() + 1;
        if (nextProgress >= access.simulationMaxProgress()) {
            finishProcess(access, recipe.get());
            return;
        }

        access.simulationSetProgress(nextProgress, access.simulationMaxProgress(), true);
        access.simulationMarkChanged();
    }

    static void serverTickPlate(StationSimulationAccess access) {
        if (!access.simulationActive()) {
            return;
        }
        Optional<KitchenPlateRecipe> recipe = currentPlateRecipe(access);
        if (recipe.isEmpty() || !canAcceptOutputs(access, recipe.get())) {
            stop(access);
            return;
        }

        int nextProgress = access.simulationProgress() + 1;
        if (nextProgress >= access.simulationMaxProgress()) {
            finishPlate(access, recipe.get());
            return;
        }

        access.simulationSetProgress(nextProgress, access.simulationMaxProgress(), true);
        access.simulationMarkChanged();
    }

    static ItemStack previewProcessStack(StationSimulationAccess access, KitchenProcessRecipe recipe) {
        Level level = access.simulationLevel();
        if (level == null) {
            return ItemStack.EMPTY;
        }

        KitchenRecipeMatchPlan matchPlan = recipe.candidatePlan(
                access.simulationStationType(),
                inputStacks(access),
                access.simulationItem(access.toolSlot()),
                level
        ).orElse(null);
        if (matchPlan == null) {
            return ItemStack.EMPTY;
        }

        List<ItemStack> consumedInputs = matchedInputs(access, matchPlan, recipe.inputs());
        KitchenProcessOutput resolvedOutput = recipe.outputForBand(currentOutcomeBand(access, recipe));
        IngredientStateData outputData = DishEvaluation.evaluateProcess(
                level,
                recipe,
                resolvedOutput,
                consumedInputs,
                access.simulationItem(access.toolSlot()),
                access.simulationHeatLevel(),
                access.simulationPreheatProgress() >= 100,
                matchPlan.score()
        );
        return createOutputStack(resolvedOutput.result(), outputData, level.getGameTime());
    }

    static ItemStack previewPlateStack(StationSimulationAccess access, KitchenPlateRecipe recipe) {
        Level level = access.simulationLevel();
        if (level == null) {
            return ItemStack.EMPTY;
        }

        KitchenRecipeMatchPlan matchPlan = recipe.matchPlan(new KitchenPlateInput(inputStacks(access)), level).orElse(null);
        if (matchPlan == null) {
            return ItemStack.EMPTY;
        }

        List<ItemStack> consumedInputs = matchedInputs(access, matchPlan, recipe.inputs());
        IngredientStateData outputData = DishEvaluation.evaluatePlate(level, recipe, consumedInputs, matchPlan.score());
        return createOutputStack(recipe.output().result(), outputData, level.getGameTime());
    }

    static int previewRecognizerId(ItemStack stack, long gameTime) {
        if (stack.isEmpty()) {
            return 0;
        }
        FoodMatterData matter = com.boaat.jazzy_cookin.kitchen.KitchenStackUtil.getOrCreateFoodMatter(stack, gameTime);
        DishRecognitionResult recognition = DishSchema.preview(matter);
        return recognition != null ? recognition.previewId() : 0;
    }

    static int approximateStationTempF(StationSimulationAccess access) {
        if (access.simulationStationType() == com.boaat.jazzy_cookin.kitchen.StationType.OVEN) {
            return switch (access.simulationHeatLevel()) {
                case LOW -> 250;
                case MEDIUM -> HeatLevel.DEFAULT_OVEN_TEMPERATURE;
                case HIGH -> 450;
                default -> 72;
            };
        }
        return switch (access.simulationHeatLevel()) {
            case LOW -> 220;
            case MEDIUM -> 320;
            case HIGH -> 410;
            default -> 72;
        };
    }

    private static void finishProcess(StationSimulationAccess access, KitchenProcessRecipe recipe) {
        Level level = access.simulationLevel();
        if (level == null) {
            stop(access);
            return;
        }

        KitchenRecipeMatchPlan matchPlan = recipe.candidatePlan(
                access.simulationStationType(),
                inputStacks(access),
                access.simulationItem(access.toolSlot()),
                level
        ).orElse(null);
        if (matchPlan == null) {
            stop(access);
            return;
        }

        List<ItemStack> consumedInputs = matchedInputs(access, matchPlan, recipe.inputs());

        KitchenProcessOutput resolvedOutput = recipe.outputForBand(currentOutcomeBand(access, recipe));
        IngredientStateData outputData = DishEvaluation.evaluateProcess(
                level,
                recipe,
                resolvedOutput,
                consumedInputs,
                access.simulationItem(access.toolSlot()),
                access.simulationHeatLevel(),
                access.simulationPreheatProgress() >= 100,
                matchPlan.score()
        );

        for (int i = 0; i < recipe.inputs().size(); i++) {
            int matchedSlot = matchPlan.slotForRequirement(i);
            if (matchedSlot >= 0) {
                access.simulationRemoveItem(matchedSlot, recipe.inputs().get(i).count());
            }
        }

        ItemStack outputStack = createOutputStack(resolvedOutput.result(), outputData, level.getGameTime());
        access.simulationMergeIntoSlot(access.outputSlot(), outputStack);
        if (!resolvedOutput.byproduct().isEmpty()) {
            access.simulationMergeIntoSlot(access.byproductSlot(), createOutputStack(resolvedOutput.byproduct(), null, level.getGameTime()));
        }
        damageTool(access, recipe);
        if (guidePlayer(access) instanceof ServerPlayer player
                && RecipeBookProgress.recordKitchenOutput(player, outputStack, recipe.recipeBook().normalizedChainKey())) {
            RecipeBookNetworking.sync(player);
        }
        stop(access);
        access.simulationMarkChanged();
    }

    private static void finishPlate(StationSimulationAccess access, KitchenPlateRecipe recipe) {
        Level level = access.simulationLevel();
        if (level == null) {
            stop(access);
            return;
        }

        KitchenRecipeMatchPlan matchPlan = recipe.matchPlan(new KitchenPlateInput(inputStacks(access)), level).orElse(null);
        if (matchPlan == null) {
            stop(access);
            return;
        }

        List<ItemStack> consumedInputs = matchedInputs(access, matchPlan, recipe.inputs());

        IngredientStateData outputData = DishEvaluation.evaluatePlate(level, recipe, consumedInputs, matchPlan.score());
        for (int i = 0; i < recipe.inputs().size(); i++) {
            int matchedSlot = matchPlan.slotForRequirement(i);
            if (matchedSlot >= 0) {
                access.simulationRemoveItem(matchedSlot, recipe.inputs().get(i).count());
            }
        }

        ItemStack outputStack = createOutputStack(recipe.output().result(), outputData, level.getGameTime());
        access.simulationMergeIntoSlot(access.outputSlot(), outputStack);
        if (!recipe.output().byproduct().isEmpty()) {
            access.simulationMergeIntoSlot(access.byproductSlot(), createOutputStack(recipe.output().byproduct(), null, level.getGameTime()));
        }
        if (guidePlayer(access) instanceof ServerPlayer player
                && RecipeBookProgress.recordKitchenOutput(player, outputStack, recipe.recipeBook().normalizedChainKey())) {
            RecipeBookNetworking.sync(player);
        }
        stop(access);
        access.simulationMarkChanged();
    }

    private static ItemStack createOutputStack(ItemStack template, IngredientStateData outputData, long gameTime) {
        ItemStack output = template.copy();
        if (output.getItem() instanceof KitchenIngredientItem ingredientItem) {
            return outputData != null
                    ? ingredientItem.createStack(output.getCount(), gameTime, outputData)
                    : ingredientItem.createStack(output.getCount(), gameTime);
        }
        return output;
    }

    private static void stop(StationSimulationAccess access) {
        access.simulationSetProgress(0, 0, false);
        access.simulationMarkChanged();
    }

    private static List<ItemStack> inputStacks(StationSimulationAccess access) {
        return java.util.stream.IntStream.rangeClosed(access.inputStart(), access.inputEnd())
                .mapToObj(access::simulationItem)
                .toList();
    }

    private static boolean environmentAllows(StationSimulationAccess access, KitchenProcessRecipe recipe) {
        if (!recipe.isEnvironmentReady(access.simulationHeatLevel(), access.simulationPreheatProgress() >= 100)) {
            return false;
        }
        Level level = access.simulationLevel();
        if (level == null) {
            return true;
        }

        KitchenEnvironmentRequirements requirements = recipe.environmentRequirements();
        if (requirements.nearbyWater()) {
            BlockEntity blockEntity = (BlockEntity) access;
            boolean hasWater = false;
            for (Direction direction : Direction.values()) {
                if (level.getFluidState(blockEntity.getBlockPos().relative(direction)).is(FluidTags.WATER)) {
                    hasWater = true;
                    break;
                }
            }
            if (!hasWater) {
                return false;
            }
        }

        if (requirements.sheltered()) {
            BlockEntity blockEntity = (BlockEntity) access;
            if (level.canSeeSky(blockEntity.getBlockPos().above())) {
                return false;
            }
        }
        return true;
    }

    private static int effectiveDuration(StationSimulationAccess access, KitchenProcessRecipe recipe) {
        if (recipe.mode() == com.boaat.jazzy_cookin.kitchen.ProcessMode.PASSIVE) {
            return Math.max(40, recipe.effectiveDuration());
        }
        return Math.max(20, Math.round(recipe.effectiveDuration() / toolSpeedMultiplier(access, recipe)));
    }

    private static float toolSpeedMultiplier(StationSimulationAccess access, KitchenProcessRecipe recipe) {
        List<ToolProfile> allowedTools = recipe.allowedToolsOrPreferred();
        if (allowedTools.isEmpty()) {
            return 1.0F;
        }

        if (access.simulationItem(access.toolSlot()).getItem() instanceof KitchenToolItem toolItem) {
            if (recipe.preferredTool().isPresent() && toolItem.profile() == recipe.preferredTool().get()) {
                return toolItem.speedMultiplier();
            }
            if (recipe.allowsTool(toolItem.profile())) {
                return toolItem.speedMultiplier() * 0.88F;
            }
        }

        return access.simulationItem(access.toolSlot()).isEmpty() ? 0.75F : 0.82F;
    }

    private static boolean canAcceptOutputs(StationSimulationAccess access, KitchenProcessRecipe recipe) {
        KitchenProcessOutput resolvedOutput = recipe.outputForBand(currentOutcomeBand(access, recipe));
        if (!access.simulationCanAcceptStack(access.outputSlot(), resolvedOutput.result())) {
            return false;
        }
        return resolvedOutput.byproduct().isEmpty() || access.simulationCanAcceptStack(access.byproductSlot(), resolvedOutput.byproduct());
    }

    private static boolean canAcceptOutputs(StationSimulationAccess access, KitchenPlateRecipe recipe) {
        if (!access.simulationCanAcceptStack(access.outputSlot(), recipe.output().result())) {
            return false;
        }
        return recipe.output().byproduct().isEmpty() || access.simulationCanAcceptStack(access.byproductSlot(), recipe.output().byproduct());
    }

    private static void damageTool(StationSimulationAccess access, KitchenProcessRecipe recipe) {
        if (recipe.allowedToolsOrPreferred().isEmpty()) {
            return;
        }

        ItemStack tool = access.simulationItem(access.toolSlot());
        if (!tool.isDamageableItem()) {
            return;
        }

        tool.setDamageValue(tool.getDamageValue() + 1);
        if (tool.getDamageValue() >= tool.getMaxDamage()) {
            access.simulationSetItem(access.toolSlot(), ItemStack.EMPTY);
        } else {
            access.simulationMarkChanged();
        }
    }

    private static KitchenOutcomeBand currentOutcomeBand(StationSimulationAccess access, KitchenProcessRecipe recipe) {
        if (!access.simulationStationType().supportsStationControl() || recipe.outcomes().isEmpty()) {
            return KitchenOutcomeBand.IDEAL;
        }
        return KitchenOutcomeBand.fromControlIndex(access.simulationControlSetting());
    }

    private static ItemStack matchedInputCopy(StationSimulationAccess access, KitchenRecipeMatchPlan matchPlan, List<KitchenInputRequirement> requirements, int requirementIndex) {
        if (requirementIndex < 0 || requirementIndex >= requirements.size()) {
            return ItemStack.EMPTY;
        }
        int matchedSlot = matchPlan.slotForRequirement(requirementIndex);
        return matchedSlot >= 0 ? copySized(access.simulationItem(matchedSlot), requirements.get(requirementIndex).count()) : ItemStack.EMPTY;
    }

    private static List<ItemStack> matchedInputs(
            StationSimulationAccess access,
            KitchenRecipeMatchPlan matchPlan,
            List<KitchenInputRequirement> requirements
    ) {
        return java.util.stream.IntStream.range(0, requirements.size())
                .mapToObj(index -> matchedInputCopy(access, matchPlan, requirements, index))
                .toList();
    }

    private static ItemStack copySized(ItemStack stack, int count) {
        if (count <= 0 || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    private static ServerPlayer guidePlayer(StationSimulationAccess access) {
        if (!(access instanceof KitchenStationBlockEntity station) || station.simulationLevel() == null || station.activeGuidePlayerId() == null) {
            return null;
        }
        return station.simulationLevel().getServer() != null
                ? station.simulationLevel().getServer().getPlayerList().getPlayer(station.activeGuidePlayerId())
                : null;
    }
}
