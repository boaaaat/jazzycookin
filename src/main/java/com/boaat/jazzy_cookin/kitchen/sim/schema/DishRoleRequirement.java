package com.boaat.jazzy_cookin.kitchen.sim.schema;

import java.util.List;

import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record DishRoleRequirement(
        DishRole role,
        List<FoodTrait> anyTraits,
        List<FoodTrait> allTraits,
        float weight
) {
    public static final Codec<DishRoleRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DishRole.CODEC.fieldOf("role").forGetter(DishRoleRequirement::role),
            DishCodecs.FOOD_TRAIT.listOf().optionalFieldOf("any_traits", List.of()).forGetter(DishRoleRequirement::anyTraits),
            DishCodecs.FOOD_TRAIT.listOf().optionalFieldOf("all_traits", List.of()).forGetter(DishRoleRequirement::allTraits),
            Codec.FLOAT.optionalFieldOf("weight", 1.0F).forGetter(DishRoleRequirement::weight)
    ).apply(instance, DishRoleRequirement::new));

    public static DishRoleRequirement any(DishRole role, float weight, FoodTrait... traits) {
        return new DishRoleRequirement(role, List.of(traits), List.of(), weight);
    }

    public static DishRoleRequirement all(DishRole role, float weight, FoodTrait... traits) {
        return new DishRoleRequirement(role, List.of(), List.of(traits), weight);
    }

    public float score(FoodMatterData matter) {
        if (matter == null) {
            return 0.0F;
        }
        boolean allMatched = this.allTraits.isEmpty() || this.allTraits.stream().allMatch(matter::hasTrait);
        boolean anyMatched = this.anyTraits.isEmpty() || this.anyTraits.stream().anyMatch(matter::hasTrait);
        if (allMatched && anyMatched) {
            return 1.0F;
        }
        if (!this.allTraits.isEmpty()) {
            long matched = this.allTraits.stream().filter(matter::hasTrait).count();
            return Math.max(0.0F, matched / (float) this.allTraits.size() * 0.76F);
        }
        return 0.0F;
    }
}
