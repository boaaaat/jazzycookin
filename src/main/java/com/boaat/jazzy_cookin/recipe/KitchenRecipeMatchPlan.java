package com.boaat.jazzy_cookin.recipe;

public record KitchenRecipeMatchPlan(
        int[] matchedSlots,
        int supportiveExtras,
        float score,
        float requiredFit,
        float ingredientFit,
        float stateFit,
        float specificity,
        float exactMatchRatio,
        float extraPenalty
) {
    public int slotForRequirement(int requirementIndex) {
        return requirementIndex >= 0 && requirementIndex < this.matchedSlots.length ? this.matchedSlots[requirementIndex] : -1;
    }

    public boolean betterThan(KitchenRecipeMatchPlan other) {
        if (other == null) {
            return true;
        }
        int scoreCompare = compare(this.score, other.score);
        if (scoreCompare != 0) {
            return scoreCompare > 0;
        }
        int requiredCompare = compare(this.requiredFit, other.requiredFit);
        if (requiredCompare != 0) {
            return requiredCompare > 0;
        }
        int exactCompare = compare(this.exactMatchRatio, other.exactMatchRatio);
        if (exactCompare != 0) {
            return exactCompare > 0;
        }
        int specificityCompare = compare(this.specificity, other.specificity);
        if (specificityCompare != 0) {
            return specificityCompare > 0;
        }
        int ingredientCompare = compare(this.ingredientFit, other.ingredientFit);
        if (ingredientCompare != 0) {
            return ingredientCompare > 0;
        }
        int stateCompare = compare(this.stateFit, other.stateFit);
        if (stateCompare != 0) {
            return stateCompare > 0;
        }
        int penaltyCompare = compare(other.extraPenalty, this.extraPenalty);
        if (penaltyCompare != 0) {
            return penaltyCompare > 0;
        }
        return this.supportiveExtras < other.supportiveExtras;
    }

    private static int compare(float left, float right) {
        float delta = left - right;
        if (Math.abs(delta) < 0.0005F) {
            return 0;
        }
        return delta > 0.0F ? 1 : -1;
    }
}
