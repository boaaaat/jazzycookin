package com.boaat.jazzy_cookin.kitchen.sim.schema;

import java.util.List;
import java.util.Optional;

import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record DishStepRequirement(
        String id,
        StationType station,
        DishTechnique technique,
        Optional<ToolProfile> tool,
        List<String> prerequisites,
        float progressTarget,
        Optional<IngredientState> outputState
) {
    public static final Codec<DishStepRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(DishStepRequirement::id),
            StationType.CODEC.fieldOf("station").forGetter(DishStepRequirement::station),
            DishTechnique.CODEC.fieldOf("technique").forGetter(DishStepRequirement::technique),
            ToolProfile.CODEC.optionalFieldOf("tool").forGetter(DishStepRequirement::tool),
            Codec.STRING.listOf().optionalFieldOf("prerequisites", List.of()).forGetter(DishStepRequirement::prerequisites),
            Codec.FLOAT.optionalFieldOf("progress_target", 1.0F).forGetter(DishStepRequirement::progressTarget),
            IngredientState.CODEC.optionalFieldOf("output_state").forGetter(DishStepRequirement::outputState)
    ).apply(instance, DishStepRequirement::new));
}
