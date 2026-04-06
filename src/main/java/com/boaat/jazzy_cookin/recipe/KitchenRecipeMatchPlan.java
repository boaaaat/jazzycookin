package com.boaat.jazzy_cookin.recipe;

public record KitchenRecipeMatchPlan(
        int[] matchedSlots,
        int supportiveExtras,
        float score
) {
    public int slotForRequirement(int requirementIndex) {
        return requirementIndex >= 0 && requirementIndex < this.matchedSlots.length ? this.matchedSlots[requirementIndex] : -1;
    }
}
