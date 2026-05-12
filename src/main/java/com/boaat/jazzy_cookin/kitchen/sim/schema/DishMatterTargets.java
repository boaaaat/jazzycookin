package com.boaat.jazzy_cookin.kitchen.sim.schema;

import java.util.List;
import java.util.Optional;

import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
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
        Optional<FloatRange> cohesiveness,
        Optional<FloatRange> whiskWork,
        Optional<FloatRange> stirCount,
        Optional<FloatRange> flipCount,
        Optional<FloatRange> timeInPan,
        Optional<FloatRange> processDepth,
        Optional<FloatRange> surfaceTempC,
        Optional<FloatRange> coreTempC
) {
    public static final DishMatterTargets EMPTY = new DishMatterTargets(
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
    );

    private record CompositionTargets(
            Optional<FloatRange> water,
            Optional<FloatRange> fat,
            Optional<FloatRange> protein
    ) {
        private static final MapCodec<CompositionTargets> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                FloatRange.CODEC.optionalFieldOf("water").forGetter(CompositionTargets::water),
                FloatRange.CODEC.optionalFieldOf("fat").forGetter(CompositionTargets::fat),
                FloatRange.CODEC.optionalFieldOf("protein").forGetter(CompositionTargets::protein)
        ).apply(instance, CompositionTargets::new));
    }

    private record FlavorTargets(
            Optional<FloatRange> seasoning,
            Optional<FloatRange> cheese,
            Optional<FloatRange> onion,
            Optional<FloatRange> herb,
            Optional<FloatRange> pepper
    ) {
        private static final MapCodec<FlavorTargets> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                FloatRange.CODEC.optionalFieldOf("seasoning").forGetter(FlavorTargets::seasoning),
                FloatRange.CODEC.optionalFieldOf("cheese").forGetter(FlavorTargets::cheese),
                FloatRange.CODEC.optionalFieldOf("onion").forGetter(FlavorTargets::onion),
                FloatRange.CODEC.optionalFieldOf("herb").forGetter(FlavorTargets::herb),
                FloatRange.CODEC.optionalFieldOf("pepper").forGetter(FlavorTargets::pepper)
        ).apply(instance, FlavorTargets::new));
    }

    private record CookTextureTargets(
            Optional<FloatRange> proteinSet,
            Optional<FloatRange> browning,
            Optional<FloatRange> charLevel,
            Optional<FloatRange> aeration,
            Optional<FloatRange> fragmentation,
            Optional<FloatRange> cohesiveness
    ) {
        private static final MapCodec<CookTextureTargets> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                FloatRange.CODEC.optionalFieldOf("protein_set").forGetter(CookTextureTargets::proteinSet),
                FloatRange.CODEC.optionalFieldOf("browning").forGetter(CookTextureTargets::browning),
                FloatRange.CODEC.optionalFieldOf("char_level").forGetter(CookTextureTargets::charLevel),
                FloatRange.CODEC.optionalFieldOf("aeration").forGetter(CookTextureTargets::aeration),
                FloatRange.CODEC.optionalFieldOf("fragmentation").forGetter(CookTextureTargets::fragmentation),
                FloatRange.CODEC.optionalFieldOf("cohesiveness").forGetter(CookTextureTargets::cohesiveness)
        ).apply(instance, CookTextureTargets::new));
    }

    private record ProcessTargets(
            Optional<FloatRange> whiskWork,
            Optional<FloatRange> stirCount,
            Optional<FloatRange> flipCount,
            Optional<FloatRange> timeInPan,
            Optional<FloatRange> processDepth
    ) {
        private static final MapCodec<ProcessTargets> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                FloatRange.CODEC.optionalFieldOf("whisk_work").forGetter(ProcessTargets::whiskWork),
                FloatRange.CODEC.optionalFieldOf("stir_count").forGetter(ProcessTargets::stirCount),
                FloatRange.CODEC.optionalFieldOf("flip_count").forGetter(ProcessTargets::flipCount),
                FloatRange.CODEC.optionalFieldOf("time_in_pan").forGetter(ProcessTargets::timeInPan),
                FloatRange.CODEC.optionalFieldOf("process_depth").forGetter(ProcessTargets::processDepth)
        ).apply(instance, ProcessTargets::new));
    }

    private record ThermalTargets(
            Optional<FloatRange> surfaceTempC,
            Optional<FloatRange> coreTempC
    ) {
        private static final MapCodec<ThermalTargets> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                FloatRange.CODEC.optionalFieldOf("surface_temp_c").forGetter(ThermalTargets::surfaceTempC),
                FloatRange.CODEC.optionalFieldOf("core_temp_c").forGetter(ThermalTargets::coreTempC)
        ).apply(instance, ThermalTargets::new));
    }

    public static final Codec<DishMatterTargets> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CompositionTargets.CODEC.forGetter(DishMatterTargets::compositionTargets),
            FlavorTargets.CODEC.forGetter(DishMatterTargets::flavorTargets),
            CookTextureTargets.CODEC.forGetter(DishMatterTargets::cookTextureTargets),
            ProcessTargets.CODEC.forGetter(DishMatterTargets::processTargets),
            ThermalTargets.CODEC.forGetter(DishMatterTargets::thermalTargets)
    ).apply(instance, (composition, flavor, cookTexture, process, thermal) -> new DishMatterTargets(
            composition.water(),
            composition.fat(),
            composition.protein(),
            flavor.seasoning(),
            flavor.cheese(),
            flavor.onion(),
            flavor.herb(),
            flavor.pepper(),
            cookTexture.proteinSet(),
            cookTexture.browning(),
            cookTexture.charLevel(),
            cookTexture.aeration(),
            cookTexture.fragmentation(),
            cookTexture.cohesiveness(),
            process.whiskWork(),
            process.stirCount(),
            process.flipCount(),
            process.timeInPan(),
            process.processDepth(),
            thermal.surfaceTempC(),
            thermal.coreTempC()
    )));

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

    public float processScore(FoodMatterData matter) {
        return average(List.of(
                score(this.whiskWork, matter.whiskWork()),
                score(this.stirCount, matter.stirCount()),
                score(this.flipCount, matter.flipCount()),
                score(this.timeInPan, matter.timeInPan()),
                score(this.processDepth, matter.processDepth())
        ));
    }

    public float thermalScore(FoodMatterData matter) {
        return average(List.of(
                score(this.surfaceTempC, matter.surfaceTempC()),
                score(this.coreTempC, matter.coreTempC())
        ));
    }

    public boolean hasProcessTargets() {
        return this.whiskWork.isPresent()
                || this.stirCount.isPresent()
                || this.flipCount.isPresent()
                || this.timeInPan.isPresent()
                || this.processDepth.isPresent();
    }

    public boolean hasThermalTargets() {
        return this.surfaceTempC.isPresent() || this.coreTempC.isPresent();
    }

    private CompositionTargets compositionTargets() {
        return new CompositionTargets(this.water, this.fat, this.protein);
    }

    private FlavorTargets flavorTargets() {
        return new FlavorTargets(this.seasoning, this.cheese, this.onion, this.herb, this.pepper);
    }

    private CookTextureTargets cookTextureTargets() {
        return new CookTextureTargets(this.proteinSet, this.browning, this.charLevel, this.aeration, this.fragmentation, this.cohesiveness);
    }

    private ProcessTargets processTargets() {
        return new ProcessTargets(this.whiskWork, this.stirCount, this.flipCount, this.timeInPan, this.processDepth);
    }

    private ThermalTargets thermalTargets() {
        return new ThermalTargets(this.surfaceTempC, this.coreTempC);
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
