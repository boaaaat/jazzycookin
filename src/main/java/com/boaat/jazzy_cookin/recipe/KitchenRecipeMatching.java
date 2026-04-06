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
            FoodTrait.FAT,
            FoodTrait.OIL,
            FoodTrait.CONDIMENT,
            FoodTrait.SAUCE,
            FoodTrait.PRESERVE,
            FoodTrait.FERMENTED,
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
            return new KitchenRecipeMatchPlan(new int[0], 0, 1.0F);
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
            if (requirements.get(requirementIndex).matchScore(inputStacks.get(requirementIndex), gameTime) <= 0.0F) {
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

        float score = Mth.clamp(1.0F - supportiveExtras * 0.08F, 0.0F, 1.0F);
        return score >= minimumScore(guide, inputStacks, requirements, supportiveExtras)
                ? new KitchenRecipeMatchPlan(matchedSlots, supportiveExtras, score)
                : null;
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
            int supportiveExtras = countSupportiveExtras(inputStacks, requirements, searchState.currentMatches, gameTime);
            if (supportiveExtras < 0) {
                return;
            }
            if (!guide.allowSupportiveExtras() && supportiveExtras > 0) {
                return;
            }

            float score = Mth.clamp(1.0F - supportiveExtras * 0.08F, 0.0F, 1.0F);
            if (score < minimumScore(guide, inputStacks, requirements, supportiveExtras)) {
                return;
            }

            if (searchState.bestPlan == null || score > searchState.bestPlan.score()) {
                searchState.bestPlan = new KitchenRecipeMatchPlan(Arrays.copyOf(searchState.currentMatches, searchState.currentMatches.length), supportiveExtras, score);
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
        for (KitchenInputRequirement requirement : requirements) {
            if (requirement.ingredientMatches(stack) || requirement.matches(stack, gameTime)) {
                return true;
            }
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

        float expandedLayoutFloor = Mth.clamp(1.0F - extraCapacity * 0.08F, 0.48F, 1.0F);
        return Math.min(guide.minimumScore(), expandedLayoutFloor);
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
