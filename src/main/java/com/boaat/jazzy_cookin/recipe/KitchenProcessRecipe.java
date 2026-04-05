package com.boaat.jazzy_cookin.recipe;

import java.util.List;
import java.util.Optional;

import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.KitchenOutcomeBand;
import com.boaat.jazzy_cookin.kitchen.ProcessMode;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.boaat.jazzy_cookin.registry.JazzyRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record KitchenProcessRecipe(
        StationType station,
        List<KitchenInputRequirement> inputs,
        KitchenMethod method,
        Optional<ToolProfile> preferredTool,
        List<ToolProfile> allowedTools,
        boolean toolRequired,
        int duration,
        int passiveDurationTicks,
        HeatLevel preferredHeat,
        HeatLevel minimumHeat,
        HeatLevel maximumHeat,
        boolean requiresPreheat,
        ProcessMode mode,
        KitchenEnvironmentRequirements environmentRequirements,
        List<KitchenProcessOutcome> outcomes,
        KitchenProcessOutput output
) implements Recipe<KitchenProcessInput> {
    @Override
    public boolean matches(KitchenProcessInput input, Level level) {
        if (input.station() != this.station) {
            return false;
        }

        if (!this.matchesTool(input.tool())) {
            return false;
        }

        if (!this.matchesHeat(input.heat())) {
            return false;
        }

        if (this.requiresPreheat && !input.preheated()) {
            return false;
        }

        if (input.inputs().size() < this.inputs.size()) {
            return false;
        }

        for (int i = 0; i < this.inputs.size(); i++) {
            if (!this.inputs.get(i).matches(input.inputs().get(i), level.getGameTime())) {
                return false;
            }
        }

        for (int i = this.inputs.size(); i < input.inputs().size(); i++) {
            if (!input.inputs().get(i).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesTool(ItemStack toolStack) {
        ToolProfile actualProfile = ToolProfile.fromStack(toolStack);
        List<ToolProfile> allowed = this.allowedToolsOrPreferred();
        if (this.toolRequired && actualProfile == ToolProfile.NONE) {
            return false;
        }
        if (allowed.isEmpty()) {
            return true;
        }
        if (actualProfile == ToolProfile.NONE) {
            return !this.toolRequired;
        }
        return allowed.contains(actualProfile);
    }

    private boolean matchesHeat(HeatLevel actualHeat) {
        if (!this.usesHeat()) {
            return actualHeat == HeatLevel.OFF || actualHeat == this.preferredHeat || actualHeat == HeatLevel.LOW;
        }
        if (actualHeat == HeatLevel.OFF) {
            return false;
        }
        if (actualHeat.ordinal() < this.minimumHeat.ordinal()) {
            return false;
        }
        return this.maximumHeat == HeatLevel.OFF || actualHeat.ordinal() <= this.maximumHeat.ordinal();
    }

    @Override
    public ItemStack assemble(KitchenProcessInput input, HolderLookup.Provider registries) {
        return this.output.result().copy();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.output.result().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public RecipeSerializer<? extends Recipe<KitchenProcessInput>> getSerializer() {
        return JazzyRecipes.KITCHEN_PROCESS_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<KitchenProcessInput>> getType() {
        return JazzyRecipes.KITCHEN_PROCESS_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public boolean usesHeat() {
        return this.minimumHeat != HeatLevel.OFF || this.maximumHeat != HeatLevel.OFF || this.preferredHeat != HeatLevel.OFF;
    }

    public List<ToolProfile> allowedToolsOrPreferred() {
        if (!this.allowedTools.isEmpty()) {
            return this.allowedTools;
        }
        return this.preferredTool.map(List::of).orElse(List.of());
    }

    public boolean allowsTool(ToolProfile profile) {
        return this.allowedToolsOrPreferred().isEmpty() || this.allowedToolsOrPreferred().contains(profile);
    }

    public int effectiveDuration() {
        return this.mode == ProcessMode.PASSIVE && this.passiveDurationTicks > 0 ? this.passiveDurationTicks : this.duration;
    }

    public boolean requiresNearbyWater() {
        return this.environmentRequirements.nearbyWater();
    }

    public KitchenProcessOutput outputForBand(KitchenOutcomeBand band) {
        return this.outcomes.stream()
                .filter(outcome -> outcome.band() == band)
                .map(KitchenProcessOutcome::output)
                .findFirst()
                .orElse(this.output);
    }
}
