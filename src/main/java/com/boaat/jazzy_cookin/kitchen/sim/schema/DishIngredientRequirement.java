package com.boaat.jazzy_cookin.kitchen.sim.schema;

import java.util.List;
import java.util.Optional;

import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.MeasureUnit;
import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

public record DishIngredientRequirement(
        Optional<ResourceLocation> item,
        DishRole role,
        List<FoodTrait> anyTraits,
        List<FoodTrait> allTraits,
        Optional<IngredientState> state,
        float idealAmount,
        float minAmount,
        float maxAmount,
        MeasureUnit unit,
        boolean core,
        boolean measuredRequired
) {
    public static final Codec<DishIngredientRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("item").forGetter(DishIngredientRequirement::item),
            DishRole.CODEC.fieldOf("role").forGetter(DishIngredientRequirement::role),
            DishCodecs.FOOD_TRAIT.listOf().optionalFieldOf("any_traits", List.of()).forGetter(DishIngredientRequirement::anyTraits),
            DishCodecs.FOOD_TRAIT.listOf().optionalFieldOf("all_traits", List.of()).forGetter(DishIngredientRequirement::allTraits),
            IngredientState.CODEC.optionalFieldOf("state").forGetter(DishIngredientRequirement::state),
            Codec.FLOAT.optionalFieldOf("ideal_amount", 1.0F).forGetter(DishIngredientRequirement::idealAmount),
            Codec.FLOAT.optionalFieldOf("min_amount", 1.0F).forGetter(DishIngredientRequirement::minAmount),
            Codec.FLOAT.optionalFieldOf("max_amount", 64.0F).forGetter(DishIngredientRequirement::maxAmount),
            MeasureUnit.CODEC.optionalFieldOf("unit", MeasureUnit.COUNT).forGetter(DishIngredientRequirement::unit),
            Codec.BOOL.optionalFieldOf("core", true).forGetter(DishIngredientRequirement::core),
            Codec.BOOL.optionalFieldOf("measured_required", false).forGetter(DishIngredientRequirement::measuredRequired)
    ).apply(instance, DishIngredientRequirement::new));

    public boolean hasMeasuredAmount() {
        return this.idealAmount > 0.0F && this.maxAmount >= this.minAmount;
    }
}
