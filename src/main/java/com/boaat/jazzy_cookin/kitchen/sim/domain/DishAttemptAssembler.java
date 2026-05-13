package com.boaat.jazzy_cookin.kitchen.sim.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfiles;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishAttemptData;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishIngredientRequirement;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishRole;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaDefinition;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishStepRequirement;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishTechnique;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

final class DishAttemptAssembler {
    private DishAttemptAssembler() {
    }

    static DishAttemptData build(DishSchemaDefinition schema, StationSimulationAccessView access, float stationQuality) {
        if (schema == null) {
            return DishAttemptData.EMPTY;
        }
        Set<String> completed = new LinkedHashSet<>();
        Set<String> equipmentEvents = new LinkedHashSet<>();
        for (ItemStack stack : access.inputStacks()) {
            DishAttemptData inherited = KitchenStackUtil.dishAttempt(stack);
            completed.addAll(inherited.completedSteps());
            equipmentEvents.addAll(inherited.equipmentEvents());
        }

        DishStepRequirement currentStep = currentStep(schema, access.stationType(), access.toolProfile(), completed);
        if (currentStep != null) {
            completed.add(currentStep.id());
            equipmentEvents.add(equipmentEvent(currentStep.id(), access.stationType(), access.toolProfile()));
        }

        IngredientResult ingredients = scoreIngredients(schema, access.inputStacks());
        boolean wrongTechnique = currentStep == null && schema.steps().stream().anyMatch(step -> !completed.contains(step.id()));
        return new DishAttemptData(
                schema.key(),
                List.copyOf(completed),
                ingredients.score(),
                ingredients.missingCore(),
                ingredients.unmeasured(),
                wrongTechnique,
                Mth.clamp(1.0F - stationQuality, 0.0F, 1.0F),
                currentStep != null ? currentStep.id() : "",
                access.stationType().getSerializedName(),
                access.toolProfile().getSerializedName(),
                List.copyOf(equipmentEvents)
        ).normalized();
    }

    private static String equipmentEvent(String stepId, StationType stationType, ToolProfile toolProfile) {
        return stepId + "|" + stationType.getSerializedName() + "|" + toolProfile.getSerializedName();
    }

    private static DishStepRequirement currentStep(
            DishSchemaDefinition schema,
            StationType stationType,
            ToolProfile toolProfile,
            Set<String> completed
    ) {
        for (DishStepRequirement step : schema.steps()) {
            if (completed.contains(step.id())) {
                continue;
            }
            if (step.station() != stationType) {
                continue;
            }
            if (!toolMatches(step, toolProfile)) {
                continue;
            }
            if (!completed.containsAll(step.prerequisites())) {
                continue;
            }
            return step;
        }
        return schema.steps().stream()
                .filter(step -> !completed.contains(step.id()))
                .filter(step -> step.station() == stationType && techniqueMatchesStation(step.technique(), stationType))
                .filter(step -> toolMatches(step, toolProfile))
                .filter(step -> completed.containsAll(step.prerequisites()))
                .findFirst()
                .orElse(null);
    }

    private static boolean toolMatches(DishStepRequirement step, ToolProfile toolProfile) {
        if (!step.tools().isEmpty()) {
            return step.tools().contains(toolProfile);
        }
        return step.tool().isEmpty() || step.tool().get() == toolProfile;
    }

    private static boolean techniqueMatchesStation(DishTechnique technique, StationType stationType) {
        return switch (technique) {
            case MIXED, DIP_OR_COAT -> stationType == StationType.MIXING_BOWL || stationType == StationType.BLENDER;
            case PAN_FRIED -> stationType == StationType.STOVE;
            case PLATED -> stationType == StationType.PLATING_STATION;
            case PREPPED, CUT -> stationType == StationType.PREP_TABLE;
            case BAKED -> stationType == StationType.OVEN;
            case SIMMERED -> stationType == StationType.STOVE;
            case RESTED -> stationType == StationType.RESTING_BOARD || stationType == StationType.COOLING_RACK;
        };
    }

    private static IngredientResult scoreIngredients(DishSchemaDefinition schema, List<ItemStack> inputs) {
        if (schema.ingredients().isEmpty()) {
            return new IngredientResult(1.0F, false, false);
        }
        List<DishIngredientRequirement> requirements = schema.ingredients();
        Match[] matches = bestUniqueMatches(requirements, inputs);
        float total = 0.0F;
        boolean missingCore = false;
        boolean unmeasured = false;
        for (int index = 0; index < requirements.size(); index++) {
            DishIngredientRequirement requirement = requirements.get(index);
            Match match = matches[index];
            total += match.score();
            if (requirement.core() && match.score() < 0.50F) {
                missingCore = true;
            }
            if (requirement.measuredRequired() && match.score() > 0.0F && !match.measured()) {
                unmeasured = true;
            }
        }
        return new IngredientResult(Mth.clamp(total / requirements.size(), 0.0F, 1.0F), missingCore, unmeasured);
    }

    private static Match[] bestUniqueMatches(List<DishIngredientRequirement> requirements, List<ItemStack> inputs) {
        Match[][] matrix = new Match[requirements.size()][inputs.size()];
        for (int requirementIndex = 0; requirementIndex < requirements.size(); requirementIndex++) {
            for (int inputIndex = 0; inputIndex < inputs.size(); inputIndex++) {
                matrix[requirementIndex][inputIndex] = match(requirements.get(requirementIndex), inputs.get(inputIndex));
            }
        }
        Assignment assignment = bestAssignment(requirements, matrix, 0, new boolean[inputs.size()], new Match[requirements.size()]);
        return assignment.matches();
    }

    private static Assignment bestAssignment(
            List<DishIngredientRequirement> requirements,
            Match[][] matrix,
            int requirementIndex,
            boolean[] usedInputs,
            Match[] current
    ) {
        if (requirementIndex >= requirements.size()) {
            Match[] copy = new Match[current.length];
            System.arraycopy(current, 0, copy, 0, current.length);
            return new Assignment(copy, assignmentScore(requirements, copy));
        }

        Assignment best = null;
        current[requirementIndex] = Match.NONE;
        best = bestAssignment(requirements, matrix, requirementIndex + 1, usedInputs, current);
        for (int inputIndex = 0; inputIndex < usedInputs.length; inputIndex++) {
            Match match = matrix[requirementIndex][inputIndex];
            if (usedInputs[inputIndex] || match.score() <= 0.0F) {
                continue;
            }
            usedInputs[inputIndex] = true;
            current[requirementIndex] = match;
            Assignment candidate = bestAssignment(requirements, matrix, requirementIndex + 1, usedInputs, current);
            if (candidate.score() > best.score()) {
                best = candidate;
            }
            current[requirementIndex] = Match.NONE;
            usedInputs[inputIndex] = false;
        }
        return best;
    }

    private static float assignmentScore(List<DishIngredientRequirement> requirements, Match[] matches) {
        float total = 0.0F;
        int coreMissing = 0;
        for (int index = 0; index < requirements.size(); index++) {
            Match match = matches[index];
            total += match.score();
            if (requirements.get(index).core() && match.score() < 0.50F) {
                coreMissing++;
            }
        }
        return total - coreMissing * 0.55F;
    }

    private static Match match(DishIngredientRequirement requirement, ItemStack stack) {
        if (stack.isEmpty() || !matches(requirement, stack)) {
            return Match.NONE;
        }
        float totalAmount = KitchenStackUtil.measuredAmount(stack, requirement.unit());
        boolean measured = KitchenStackUtil.isMeasured(stack);
        if (!requirement.hasMeasuredAmount()) {
            return new Match(1.0F, measured);
        }
        if (totalAmount >= requirement.minAmount() && totalAmount <= requirement.maxAmount()) {
            return new Match(1.0F, measured);
        }
        if (totalAmount < requirement.minAmount()) {
            return new Match(Mth.clamp(totalAmount / Math.max(0.001F, requirement.minAmount()), 0.0F, 1.0F), measured);
        }
        return new Match(Mth.clamp(requirement.maxAmount() / Math.max(0.001F, totalAmount), 0.0F, 1.0F), measured);
    }

    private static boolean matches(DishIngredientRequirement requirement, ItemStack stack) {
        if (requirement.state().isPresent() && KitchenStackUtil.getFoodState(stack) != requirement.state().get()) {
            return false;
        }
        if (requirement.item().isPresent()) {
            if (BuiltInRegistries.ITEM.get(requirement.item().get()) != stack.getItem()) {
                return false;
            }
            return traitFiltersMatch(requirement, stack);
        }

        boolean hasTraitFilters = !requirement.allTraits().isEmpty() || !requirement.anyTraits().isEmpty();
        if (!hasTraitFilters && !roleMatches(requirement.role(), stack)) {
            return false;
        }

        return traitFiltersMatch(requirement, stack);
    }

    private static boolean traitFiltersMatch(DishIngredientRequirement requirement, ItemStack stack) {
        FoodMatterData matter = KitchenStackUtil.getFoodMatter(stack);
        if (matter != null) {
            boolean all = requirement.allTraits().isEmpty() || requirement.allTraits().stream().allMatch(matter::hasTrait);
            boolean any = requirement.anyTraits().isEmpty() || requirement.anyTraits().stream().anyMatch(matter::hasTrait);
            return all && any;
        }
        boolean all = requirement.allTraits().isEmpty() || requirement.allTraits().stream().allMatch(trait -> FoodMaterialProfiles.hasTrait(stack, trait));
        boolean any = requirement.anyTraits().isEmpty() || requirement.anyTraits().stream().anyMatch(trait -> FoodMaterialProfiles.hasTrait(stack, trait));
        return all && any;
    }

    private static boolean roleMatches(DishRole role, ItemStack stack) {
        return switch (role) {
            case PROTEIN -> hasAnyTrait(stack, FoodTrait.PROTEIN, FoodTrait.ANIMAL_PROTEIN, FoodTrait.PLANT_PROTEIN, FoodTrait.EGG);
            case GRAIN -> hasAnyTrait(stack, FoodTrait.GRAIN, FoodTrait.STARCH, FoodTrait.WHEAT, FoodTrait.RICE, FoodTrait.CORN, FoodTrait.BREAD, FoodTrait.PASTA);
            case FAT -> hasAnyTrait(stack, FoodTrait.FAT, FoodTrait.OIL);
            case AROMATIC -> hasAnyTrait(stack, FoodTrait.AROMATIC, FoodTrait.ALLIUM);
            case ACID -> hasAnyTrait(stack, FoodTrait.ACIDIC);
            case SWEETENER -> hasAnyTrait(stack, FoodTrait.SWEETENER, FoodTrait.SYRUP);
            case HERB -> hasAnyTrait(stack, FoodTrait.HERB);
            case SALT -> hasAnyTrait(stack, FoodTrait.SALT);
            case SPICE -> hasAnyTrait(stack, FoodTrait.SPICE, FoodTrait.PEPPER);
            case BINDER -> hasAnyTrait(stack, FoodTrait.EGG, FoodTrait.FLOUR, FoodTrait.STARCH);
            case LIQUID -> hasAnyTrait(stack, FoodTrait.DAIRY, FoodTrait.SAUCE, FoodTrait.ACIDIC);
            case VEGETABLE -> hasAnyTrait(stack, FoodTrait.VEGETABLE, FoodTrait.LEAFY_GREEN, FoodTrait.LEGUME);
            case FRUIT -> hasAnyTrait(stack, FoodTrait.FRUIT);
            case DAIRY -> hasAnyTrait(stack, FoodTrait.DAIRY);
            case CONTAINER -> false;
            case GARNISH -> hasAnyTrait(stack, FoodTrait.HERB, FoodTrait.SPICE, FoodTrait.FRUIT, FoodTrait.VEGETABLE);
        };
    }

    private static boolean hasAnyTrait(ItemStack stack, FoodTrait... traits) {
        FoodMatterData matter = KitchenStackUtil.getFoodMatter(stack);
        for (FoodTrait trait : traits) {
            if (matter != null ? matter.hasTrait(trait) : FoodMaterialProfiles.hasTrait(stack, trait)) {
                return true;
            }
        }
        return false;
    }

    static StationSimulationAccessView view(com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess access) {
        List<ItemStack> inputs = new ArrayList<>();
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            inputs.add(access.simulationItem(slot).copy());
        }
        return new StationSimulationAccessView(
                access.simulationStationType(),
                ToolProfile.fromStack(access.simulationItem(access.toolSlot())),
                inputs
        );
    }

    record StationSimulationAccessView(StationType stationType, ToolProfile toolProfile, List<ItemStack> inputStacks) {
    }

    private record IngredientResult(float score, boolean missingCore, boolean unmeasured) {
    }

    private record Match(float score, boolean measured) {
        private static final Match NONE = new Match(0.0F, false);
    }

    private record Assignment(Match[] matches, float score) {
    }
}
