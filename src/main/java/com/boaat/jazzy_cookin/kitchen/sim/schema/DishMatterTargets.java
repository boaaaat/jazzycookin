package com.boaat.jazzy_cookin.kitchen.sim.schema;

import java.util.List;
import java.util.Optional;

import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record DishMatterTargets(
        Optional<FloatRange> water,
        Optional<FloatRange> fat,
        Optional<FloatRange> protein,
        Optional<FloatRange> seasoning,
        Optional<FloatRange> cheese,
        Optional<FloatRange> onion,
        Optional<FloatRange> herb,
        Optional<FloatRange> pepper,
        Optional<FloatRange> proteinSet,
        Optional<FloatRange> browning,
        Optional<FloatRange> charLevel,
        Optional<FloatRange> aeration,
        Optional<FloatRange> fragmentation,
        Optional<FloatRange> cohesiveness
) {
    public static final DishMatterTargets EMPTY = new DishMatterTargets(
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
    );

    public static final Codec<DishMatterTargets> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FloatRange.CODEC.optionalFieldOf("water").forGetter(DishMatterTargets::water),
            FloatRange.CODEC.optionalFieldOf("fat").forGetter(DishMatterTargets::fat),
            FloatRange.CODEC.optionalFieldOf("protein").forGetter(DishMatterTargets::protein),
            FloatRange.CODEC.optionalFieldOf("seasoning").forGetter(DishMatterTargets::seasoning),
            FloatRange.CODEC.optionalFieldOf("cheese").forGetter(DishMatterTargets::cheese),
            FloatRange.CODEC.optionalFieldOf("onion").forGetter(DishMatterTargets::onion),
            FloatRange.CODEC.optionalFieldOf("herb").forGetter(DishMatterTargets::herb),
            FloatRange.CODEC.optionalFieldOf("pepper").forGetter(DishMatterTargets::pepper),
            FloatRange.CODEC.optionalFieldOf("protein_set").forGetter(DishMatterTargets::proteinSet),
            FloatRange.CODEC.optionalFieldOf("browning").forGetter(DishMatterTargets::browning),
            FloatRange.CODEC.optionalFieldOf("char_level").forGetter(DishMatterTargets::charLevel),
            FloatRange.CODEC.optionalFieldOf("aeration").forGetter(DishMatterTargets::aeration),
            FloatRange.CODEC.optionalFieldOf("fragmentation").forGetter(DishMatterTargets::fragmentation),
            FloatRange.CODEC.optionalFieldOf("cohesiveness").forGetter(DishMatterTargets::cohesiveness)
    ).apply(instance, DishMatterTargets::new));

    public float compositionScore(FoodMatterData matter) {
        return average(List.of(
                score(this.water, matter.water()),
                score(this.fat, matter.fat()),
                score(this.protein, matter.protein())
        ));
    }

    public float seasoningScore(FoodMatterData matter) {
        return average(List.of(
                score(this.seasoning, matter.seasoningLoad()),
                score(this.cheese, matter.cheeseLoad()),
                score(this.onion, matter.onionLoad()),
                score(this.herb, matter.herbLoad()),
                score(this.pepper, matter.pepperLoad())
        ));
    }

    public float cookingScore(FoodMatterData matter) {
        return average(List.of(
                score(this.proteinSet, matter.proteinSet()),
                score(this.browning, matter.browning()),
                score(this.charLevel, matter.charLevel())
        ));
    }

    public float textureScore(FoodMatterData matter) {
        return average(List.of(
                score(this.aeration, matter.aeration()),
                score(this.fragmentation, matter.fragmentation()),
                score(this.cohesiveness, matter.cohesiveness())
        ));
    }

    private static Optional<Float> score(Optional<FloatRange> range, float value) {
        return range.map(target -> target.score(value));
    }

    private static float average(List<Optional<Float>> values) {
        float total = 0.0F;
        int count = 0;
        for (Optional<Float> value : values) {
            if (value.isPresent()) {
                total += value.get();
                count++;
            }
        }
        return count > 0 ? total / count : 0.62F;
    }
}
