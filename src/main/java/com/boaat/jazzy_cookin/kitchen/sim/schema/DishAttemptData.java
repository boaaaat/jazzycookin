package com.boaat.jazzy_cookin.kitchen.sim.schema;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Mth;

public record DishAttemptData(
        String schemaKey,
        List<String> completedSteps,
        float ingredientScore,
        boolean missingCoreIngredient,
        boolean unmeasuredIngredient,
        boolean wrongTechnique,
        float qualityPenalty,
        String equipmentStep,
        String station,
        String tool,
        List<String> equipmentEvents
) {
    public static final DishAttemptData EMPTY = new DishAttemptData("", List.of(), 0.0F, false, false, false, 0.0F, "", "", "", List.of());

    public static final Codec<DishAttemptData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("schema_key", "").forGetter(DishAttemptData::schemaKey),
            Codec.STRING.listOf().optionalFieldOf("completed_steps", List.of()).forGetter(DishAttemptData::completedSteps),
            Codec.FLOAT.optionalFieldOf("ingredient_score", 0.0F).forGetter(DishAttemptData::ingredientScore),
            Codec.BOOL.optionalFieldOf("missing_core_ingredient", false).forGetter(DishAttemptData::missingCoreIngredient),
            Codec.BOOL.optionalFieldOf("unmeasured_ingredient", false).forGetter(DishAttemptData::unmeasuredIngredient),
            Codec.BOOL.optionalFieldOf("wrong_technique", false).forGetter(DishAttemptData::wrongTechnique),
            Codec.FLOAT.optionalFieldOf("quality_penalty", 0.0F).forGetter(DishAttemptData::qualityPenalty),
            Codec.STRING.optionalFieldOf("equipment_step", "").forGetter(DishAttemptData::equipmentStep),
            Codec.STRING.optionalFieldOf("station", "").forGetter(DishAttemptData::station),
            Codec.STRING.optionalFieldOf("tool", "").forGetter(DishAttemptData::tool),
            Codec.STRING.listOf().optionalFieldOf("equipment_events", List.of()).forGetter(DishAttemptData::equipmentEvents)
    ).apply(instance, DishAttemptData::new));

    public DishAttemptData(
            String schemaKey,
            List<String> completedSteps,
            float ingredientScore,
            boolean missingCoreIngredient,
            boolean unmeasuredIngredient,
            boolean wrongTechnique,
            float qualityPenalty
    ) {
        this(schemaKey, completedSteps, ingredientScore, missingCoreIngredient, unmeasuredIngredient, wrongTechnique, qualityPenalty, "", "", "", List.of());
    }

    public DishAttemptData(
            String schemaKey,
            List<String> completedSteps,
            float ingredientScore,
            boolean missingCoreIngredient,
            boolean unmeasuredIngredient,
            boolean wrongTechnique,
            float qualityPenalty,
            String equipmentStep,
            String station,
            String tool
    ) {
        this(schemaKey, completedSteps, ingredientScore, missingCoreIngredient, unmeasuredIngredient, wrongTechnique, qualityPenalty, equipmentStep, station, tool, List.of());
    }

    public DishAttemptData normalized() {
        return new DishAttemptData(
                this.schemaKey == null ? "" : this.schemaKey,
                this.completedSteps == null ? List.of() : List.copyOf(this.completedSteps),
                Mth.clamp(this.ingredientScore, 0.0F, 1.0F),
                this.missingCoreIngredient,
                this.unmeasuredIngredient,
                this.wrongTechnique,
                Mth.clamp(this.qualityPenalty, 0.0F, 1.0F),
                this.equipmentStep == null ? "" : this.equipmentStep,
                this.station == null ? "" : this.station,
                this.tool == null ? "" : this.tool,
                this.equipmentEvents == null ? List.of() : List.copyOf(this.equipmentEvents)
        );
    }

    public boolean hasStep(String stepId) {
        return stepId != null && this.completedSteps.contains(stepId);
    }
}
