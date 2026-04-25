package com.boaat.jazzy_cookin.kitchen.sim.schema;

import java.util.List;

import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;
import com.mojang.serialization.Codec;
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
        DishMatterTargets targets,
        DishScoreWeights weights
) {
    public static final Codec<DishSchemaDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("key").forGetter(DishSchemaDefinition::key),
            Codec.INT.fieldOf("preview_id").forGetter(DishSchemaDefinition::previewId),
            ResourceLocation.CODEC.fieldOf("result").forGetter(DishSchemaDefinition::result),
            DishCategory.CODEC.fieldOf("category").forGetter(DishSchemaDefinition::category),
            Codec.BOOL.optionalFieldOf("meal", false).forGetter(DishSchemaDefinition::meal),
            Codec.FLOAT.optionalFieldOf("preview_threshold", 0.58F).forGetter(DishSchemaDefinition::previewThreshold),
            Codec.FLOAT.optionalFieldOf("finalize_threshold", 0.62F).forGetter(DishSchemaDefinition::finalizeThreshold),
            Codec.FLOAT.optionalFieldOf("desirability", 0.80F).forGetter(DishSchemaDefinition::desirability),
            DishRoleRequirement.CODEC.listOf().optionalFieldOf("required_roles", List.of()).forGetter(DishSchemaDefinition::requiredRoles),
            DishRoleRequirement.CODEC.listOf().optionalFieldOf("optional_roles", List.of()).forGetter(DishSchemaDefinition::optionalRoles),
            DishCodecs.FOOD_TRAIT.listOf().optionalFieldOf("forbidden_traits", List.of()).forGetter(DishSchemaDefinition::forbiddenTraits),
            DishTechnique.CODEC.listOf().optionalFieldOf("required_techniques", List.of()).forGetter(DishSchemaDefinition::requiredTechniques),
            DishMatterTargets.CODEC.optionalFieldOf("targets", DishMatterTargets.EMPTY).forGetter(DishSchemaDefinition::targets),
            DishScoreWeights.CODEC.optionalFieldOf("weights", DishScoreWeights.DEFAULT).forGetter(DishSchemaDefinition::weights)
    ).apply(instance, DishSchemaDefinition::new));
}
