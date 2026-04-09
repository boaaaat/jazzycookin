package com.boaat.jazzy_cookin.recipe;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfiles;
import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

final class KitchenRecipeMatching {
    private static final Set<FoodTrait> SUPPORTIVE_EXTRA_TRAITS = EnumSet.of(
            FoodTrait.SWEETENER,
            FoodTrait.SYRUP,
            FoodTrait.SALT,
            FoodTrait.SPICE,
            FoodTrait.HERB,
            FoodTrait.ALLIUM,
            FoodTrait.AROMATIC,
            FoodTrait.FAT,
            FoodTrait.OIL,
            FoodTrait.CONDIMENT,
            FoodTrait.SAUCE,
            FoodTrait.PRESERVE,
            FoodTrait.ACIDIC,
            FoodTrait.PEPPER,
            FoodTrait.CHOCOLATE
    );

    private KitchenRecipeMatching() {
    }

    static KitchenRecipeMatchPlan findBestPlan(
            List<ItemStack> inputStacks,
            List<KitchenInputRequirement> requirements,
            KitchenRecipeGuideData guide,
            long gameTime
    ) {
        if (requirements.isEmpty()) {
            return new KitchenRecipeMatchPlan(new int[0], 0, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F);
        }
        if (inputStacks.size() < requirements.size()) {
            return null;
        }

        return guide.allowFlexibleInputs()
                ? findFlexiblePlan(inputStacks, requirements, guide, gameTime)
                : findOrderedPlan(inputStacks, requirements, guide, gameTime);
    }

    private static KitchenRecipeMatchPlan findOrderedPlan(
            List<ItemStack> inputStacks,
            List<KitchenInputRequirement> requirements,
            KitchenRecipeGuideData guide,
            long gameTime
    ) {
        int[] matchedSlots = new int[requirements.size()];
        Arrays.fill(matchedSlots, -1);
        for (int requirementIndex = 0; requirementIndex < requirements.size(); requirementIndex++) {
            float matchScore = requirements.get(requirementIndex).matchScore(inputStacks.get(requirementIndex), gameTime);
            if (matchScore <= 0.0F) {
                return null;
            }
            matchedSlots[requirementIndex] = requirementIndex;
        }

        int supportiveExtras = countSupportiveExtras(inputStacks, requirements, matchedSlots, gameTime);
        if (supportiveExtras < 0) {
            return null;
        }
        if (!guide.allowSupportiveExtras() && supportiveExtras > 0) {
            return null;
        }

        KitchenRecipeMatchPlan plan = buildPlan(inputStacks, requirements, matchedSlots, supportiveExtras, gameTime);
        return plan.score() >= minimumScore(guide, inputStacks, requirements, supportiveExtras) ? plan : null;
    }

    private static KitchenRecipeMatchPlan findFlexiblePlan(
            List<ItemStack> inputStacks,
            List<KitchenInputRequirement> requirements,
            KitchenRecipeGuideData guide,
            long gameTime
    ) {
        SearchState searchState = new SearchState(new int[requirements.size()], new boolean[inputStacks.size()], null);
        Arrays.fill(searchState.currentMatches, -1);
        search(inputStacks, requirements, guide, gameTime, 0, searchState);
        return searchState.bestPlan;
    }

    private static void search(
            List<ItemStack> inputStacks,
            List<KitchenInputRequirement> requirements,
            KitchenRecipeGuideData guide,
            long gameTime,
            int requirementIndex,
            SearchState searchState
    ) {
        if (requirementIndex >= requirements.size()) {
            for (int index = 0; index < requirements.size(); index++) {
                int matchedSlot = searchState.currentMatches[index];
                if (matchedSlot < 0 || requirements.get(index).matchScore(inputStacks.get(matchedSlot), gameTime) <= 0.0F) {
                    return;
                }
            }

            int supportiveExtras = countSupportiveExtras(inputStacks, requirements, searchState.currentMatches, gameTime);
            if (supportiveExtras < 0) {
                return;
            }
            if (!guide.allowSupportiveExtras() && supportiveExtras > 0) {
                return;
            }

            KitchenRecipeMatchPlan plan = buildPlan(
                    inputStacks,
                    requirements,
                    Arrays.copyOf(searchState.currentMatches, searchState.currentMatches.length),
                    supportiveExtras,
                    gameTime
            );
            if (plan.score() < minimumScore(guide, inputStacks, requirements, supportiveExtras)) {
                return;
            }

            if (plan.betterThan(searchState.bestPlan)) {
                searchState.bestPlan = plan;
            }
            return;
        }

        KitchenInputRequirement requirement = requirements.get(requirementIndex);
        for (int slot = 0; slot < inputStacks.size(); slot++) {
            if (searchState.usedSlots[slot] || requirement.matchScore(inputStacks.get(slot), gameTime) <= 0.0F) {
                continue;
            }

            searchState.usedSlots[slot] = true;
            searchState.currentMatches[requirementIndex] = slot;
            search(inputStacks, requirements, guide, gameTime, requirementIndex + 1, searchState);
            searchState.currentMatches[requirementIndex] = -1;
            searchState.usedSlots[slot] = false;
        }
    }

    private static int countSupportiveExtras(
            List<ItemStack> inputStacks,
            List<KitchenInputRequirement> requirements,
            int[] matchedSlots,
            long gameTime
    ) {
        boolean[] matched = new boolean[inputStacks.size()];
        for (int slot : matchedSlots) {
            if (slot >= 0 && slot < matched.length) {
                matched[slot] = true;
            }
        }

        int supportiveExtras = 0;
        for (int slot = 0; slot < inputStacks.size(); slot++) {
            if (matched[slot] || inputStacks.get(slot).isEmpty()) {
                continue;
            }
            if (!isSupportiveExtra(inputStacks.get(slot), requirements, gameTime)) {
                return -1;
            }
            supportiveExtras++;
        }
        return supportiveExtras;
    }

    private static boolean isSupportiveExtra(ItemStack stack, List<KitchenInputRequirement> requirements, long gameTime) {
        if (stack.isEmpty()) {
            return true;
        }
        return FoodMaterialProfiles.profileFor(stack)
                .map(profile -> {
                    Set<FoodTrait> traits = FoodTrait.unpack(profile.traitMask());
                    return !traits.isEmpty() && traits.stream().allMatch(SUPPORTIVE_EXTRA_TRAITS::contains);
                })
                .orElse(false);
    }

    private static float minimumScore(
            KitchenRecipeGuideData guide,
            List<ItemStack> inputStacks,
            List<KitchenInputRequirement> requirements,
            int supportiveExtras
    ) {
        if (!guide.allowSupportiveExtras() || supportiveExtras <= 0) {
            return guide.minimumScore();
        }

        long occupiedInputs = inputStacks.stream().filter(stack -> !stack.isEmpty()).count();
        int extraCapacity = Math.max(0, (int) occupiedInputs - requirements.size());
        if (extraCapacity <= 0) {
            return guide.minimumScore();
        }

        float expandedLayoutFloor = Mth.clamp(1.0F - extraCapacity * 0.05F, 0.60F, 1.0F);
        return Math.min(guide.minimumScore(), expandedLayoutFloor);
    }

    private static KitchenRecipeMatchPlan buildPlan(
            List<ItemStack> inputStacks,
            List<KitchenInputRequirement> requirements,
            int[] matchedSlots,
            int supportiveExtras,
            long gameTime
    ) {
        float requiredFit = 0.0F;
        float ingredientFit = 0.0F;
        float stateFit = 0.0F;
        float specificity = 0.0F;
        float exactMatchRatio = 0.0F;
        int exactMatches = 0;

        for (int index = 0; index < requirements.size(); index++) {
            KitchenInputRequirement requirement = requirements.get(index);
            ItemStack stack = inputStacks.get(matchedSlots[index]);
            requiredFit += requirement.matchScore(stack, gameTime);
            ingredientFit += requirement.ingredientFitScore(stack);
            stateFit += requirement.stateScore(stack, gameTime);
            specificity += requirement.specificityScore();
            if (requirement.exactItemMatch(stack)) {
                exactMatches++;
            }
        }

        int requirementCount = Math.max(1, requirements.size());
        requiredFit /= requirementCount;
        ingredientFit /= requirementCount;
        stateFit /= requirementCount;
        specificity /= requirementCount;
        exactMatchRatio = exactMatches / (float) requirementCount;
        float extraPenalty = supportiveExtras * 0.04F;
        float score = Mth.clamp(
                requiredFit * 0.44F
                        + stateFit * 0.20F
                        + ingredientFit * 0.14F
                        + specificity * 0.10F
                        + exactMatchRatio * 0.12F
                        - extraPenalty,
                0.0F,
                1.0F
        );

        return new KitchenRecipeMatchPlan(
                matchedSlots,
                supportiveExtras,
                score,
                requiredFit,
                ingredientFit,
                stateFit,
                specificity,
                exactMatchRatio,
                extraPenalty
        );
    }

    private static final class SearchState {
        private final int[] currentMatches;
        private final boolean[] usedSlots;
        private KitchenRecipeMatchPlan bestPlan;

        private SearchState(int[] currentMatches, boolean[] usedSlots, KitchenRecipeMatchPlan bestPlan) {
            this.currentMatches = currentMatches;
            this.usedSlots = usedSlots;
            this.bestPlan = bestPlan;
        }
    }
}
