package com.boaat.jazzy_cookin.kitchen.sim.schema;

import java.util.List;

import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

public record DishSchemaDefinition(
        String key,
        int previewId,
        ResourceLocation result,
        DishCategory category,
        boolean meal,
        float previewThreshold,
        float finalizeThreshold,
        float desirability,
        List<DishRoleRequirement> requiredRoles,
        List<DishRoleRequirement> optionalRoles,
        List<FoodTrait> forbiddenTraits,
        List<DishTechnique> requiredTechniques,
        List<String> prerequisiteSchemas,
        List<ResourceLocation> servingItems,
        List<DishIngredientRequirement> ingredients,
        List<DishStepRequirement> steps,
        DishMatterTargets targets,
        DishScoreWeights weights
) {
    private record IdentityFields(
            String key,
            int previewId,
            ResourceLocation result,
            DishCategory category,
            boolean meal
    ) {
        private static final MapCodec<IdentityFields> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("key").forGetter(IdentityFields::key),
                Codec.INT.fieldOf("preview_id").forGetter(IdentityFields::previewId),
                ResourceLocation.CODEC.fieldOf("result").forGetter(IdentityFields::result),
                DishCategory.CODEC.fieldOf("category").forGetter(IdentityFields::category),
                Codec.BOOL.optionalFieldOf("meal", false).forGetter(IdentityFields::meal)
        ).apply(instance, IdentityFields::new));
    }

    private record ScoreFields(
            float previewThreshold,
            float finalizeThreshold,
            float desirability,
            DishMatterTargets targets,
            DishScoreWeights weights
    ) {
        private static final MapCodec<ScoreFields> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("preview_threshold", 0.58F).forGetter(ScoreFields::previewThreshold),
                Codec.FLOAT.optionalFieldOf("finalize_threshold", 0.62F).forGetter(ScoreFields::finalizeThreshold),
                Codec.FLOAT.optionalFieldOf("desirability", 0.80F).forGetter(ScoreFields::desirability),
                DishMatterTargets.CODEC.optionalFieldOf("targets", DishMatterTargets.EMPTY).forGetter(ScoreFields::targets),
                DishScoreWeights.CODEC.optionalFieldOf("weights", DishScoreWeights.DEFAULT).forGetter(ScoreFields::weights)
        ).apply(instance, ScoreFields::new));
    }

    private record RoleFields(
            List<DishRoleRequirement> requiredRoles,
            List<DishRoleRequirement> optionalRoles,
            List<FoodTrait> forbiddenTraits,
            List<DishTechnique> requiredTechniques
    ) {
        private static final MapCodec<RoleFields> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                DishRoleRequirement.CODEC.listOf().optionalFieldOf("required_roles", List.of()).forGetter(RoleFields::requiredRoles),
                DishRoleRequirement.CODEC.listOf().optionalFieldOf("optional_roles", List.of()).forGetter(RoleFields::optionalRoles),
                DishCodecs.FOOD_TRAIT.listOf().optionalFieldOf("forbidden_traits", List.of()).forGetter(RoleFields::forbiddenTraits),
                DishTechnique.CODEC.listOf().optionalFieldOf("required_techniques", List.of()).forGetter(RoleFields::requiredTechniques)
        ).apply(instance, RoleFields::new));
    }

    private record StepFields(
            List<String> prerequisiteSchemas,
            List<ResourceLocation> servingItems,
            List<DishIngredientRequirement> ingredients,
            List<DishStepRequirement> steps
    ) {
        private static final MapCodec<StepFields> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.listOf().optionalFieldOf("prerequisite_schemas", List.of()).forGetter(StepFields::prerequisiteSchemas),
                ResourceLocation.CODEC.listOf().optionalFieldOf("serving_items", List.of()).forGetter(StepFields::servingItems),
                DishIngredientRequirement.CODEC.listOf().optionalFieldOf("ingredients", List.of()).forGetter(StepFields::ingredients),
                DishStepRequirement.CODEC.listOf().optionalFieldOf("steps", List.of()).forGetter(StepFields::steps)
        ).apply(instance, StepFields::new));
    }

    public static final Codec<DishSchemaDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IdentityFields.CODEC.forGetter(DishSchemaDefinition::identityFields),
            ScoreFields.CODEC.forGetter(DishSchemaDefinition::scoreFields),
            RoleFields.CODEC.forGetter(DishSchemaDefinition::roleFields),
            StepFields.CODEC.forGetter(DishSchemaDefinition::stepFields)
    ).apply(instance, (identity, score, roles, steps) -> new DishSchemaDefinition(
            identity.key(),
            identity.previewId(),
            identity.result(),
            identity.category(),
            identity.meal(),
            score.previewThreshold(),
            score.finalizeThreshold(),
            score.desirability(),
            roles.requiredRoles(),
            roles.optionalRoles(),
            roles.forbiddenTraits(),
            roles.requiredTechniques(),
            steps.prerequisiteSchemas(),
            steps.servingItems(),
            steps.ingredients(),
            steps.steps(),
            score.targets(),
            score.weights()
    )));

    private IdentityFields identityFields() {
        return new IdentityFields(this.key, this.previewId, this.result, this.category, this.meal);
    }

    private ScoreFields scoreFields() {
        return new ScoreFields(this.previewThreshold, this.finalizeThreshold, this.desirability, this.targets, this.weights);
    }

    private RoleFields roleFields() {
        return new RoleFields(this.requiredRoles, this.optionalRoles, this.forbiddenTraits, this.requiredTechniques);
    }

    private StepFields stepFields() {
        return new StepFields(this.prerequisiteSchemas, this.servingItems, this.ingredients, this.steps);
    }
}
