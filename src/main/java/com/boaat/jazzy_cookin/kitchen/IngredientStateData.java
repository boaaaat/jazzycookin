package com.boaat.jazzy_cookin.kitchen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record IngredientStateData(
        IngredientState state,
        long createdTick,
        float quality,
        float recipeAccuracy,
        float flavor,
        float texture,
        float structure,
        float moisture,
        float purity,
        float aeration,
        int processDepth,
        int nourishment,
        int enjoyment
) {
    public static final Codec<IngredientStateData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IngredientState.CODEC.fieldOf("state").forGetter(IngredientStateData::state),
            Codec.LONG.fieldOf("created_tick").forGetter(IngredientStateData::createdTick),
            Codec.FLOAT.fieldOf("quality").forGetter(IngredientStateData::quality),
            Codec.FLOAT.optionalFieldOf("recipe_accuracy", 0.72F).forGetter(IngredientStateData::recipeAccuracy),
            Codec.FLOAT.fieldOf("flavor").forGetter(IngredientStateData::flavor),
            Codec.FLOAT.fieldOf("texture").forGetter(IngredientStateData::texture),
            Codec.FLOAT.fieldOf("structure").forGetter(IngredientStateData::structure),
            Codec.FLOAT.fieldOf("moisture").forGetter(IngredientStateData::moisture),
            Codec.FLOAT.fieldOf("purity").forGetter(IngredientStateData::purity),
            Codec.FLOAT.fieldOf("aeration").forGetter(IngredientStateData::aeration),
            Codec.INT.fieldOf("process_depth").forGetter(IngredientStateData::processDepth),
            Codec.INT.fieldOf("nourishment").forGetter(IngredientStateData::nourishment),
            Codec.INT.fieldOf("enjoyment").forGetter(IngredientStateData::enjoyment)
    ).apply(instance, IngredientStateData::new));

    public IngredientStateData withState(IngredientState newState) {
        return new IngredientStateData(
                newState,
                this.createdTick,
                this.quality,
                this.recipeAccuracy,
                this.flavor,
                this.texture,
                this.structure,
                this.moisture,
                this.purity,
                this.aeration,
                this.processDepth,
                this.nourishment,
                this.enjoyment
        );
    }

    public IngredientStateData withCreatedTick(long newCreatedTick) {
        return new IngredientStateData(
                this.state,
                newCreatedTick,
                this.quality,
                this.recipeAccuracy,
                this.flavor,
                this.texture,
                this.structure,
                this.moisture,
                this.purity,
                this.aeration,
                this.processDepth,
                this.nourishment,
                this.enjoyment
        );
    }

    public IngredientStateData withMetrics(
            IngredientState newState,
            long newCreatedTick,
            float newQuality,
            float newRecipeAccuracy,
            float newFlavor,
            float newTexture,
            float newStructure,
            float newMoisture,
            float newPurity,
            float newAeration,
            int newProcessDepth,
            int newNourishment,
            int newEnjoyment
    ) {
        return new IngredientStateData(
                newState,
                newCreatedTick,
                newQuality,
                newRecipeAccuracy,
                newFlavor,
                newTexture,
                newStructure,
                newMoisture,
                newPurity,
                newAeration,
                newProcessDepth,
                newNourishment,
                newEnjoyment
        );
    }

    public IngredientStateData withMetrics(
            IngredientState newState,
            long newCreatedTick,
            float newQuality,
            float newFlavor,
            float newTexture,
            float newStructure,
            float newMoisture,
            float newPurity,
            float newAeration,
            int newProcessDepth,
            int newNourishment,
            int newEnjoyment
    ) {
        return this.withMetrics(
                newState,
                newCreatedTick,
                newQuality,
                this.recipeAccuracy,
                newFlavor,
                newTexture,
                newStructure,
                newMoisture,
                newPurity,
                newAeration,
                newProcessDepth,
                newNourishment,
                newEnjoyment
        );
    }
}
