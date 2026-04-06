package com.boaat.jazzy_cookin.kitchen.sim;

import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Mth;

public record FoodMatterData(
        long createdTick,
        IngredientStateData summaryHint,
        long traitMask,
        float surfaceTempC,
        float coreTempC,
        float water,
        float fat,
        float protein,
        float aeration,
        float fragmentation,
        float cohesiveness,
        float proteinSet,
        float browning,
        float charLevel,
        float seasoningLoad,
        float cheeseLoad,
        float onionLoad,
        float herbLoad,
        float pepperLoad,
        float whiskWork,
        int stirCount,
        int flipCount,
        int timeInPan,
        int processDepth,
        boolean finalizedServing
) {
    private static final IngredientStateData DEFAULT_SUMMARY_HINT = new IngredientStateData(
            IngredientState.PANTRY_READY,
            0L,
            0.70F,
            0.72F,
            0.42F,
            0.40F,
            0.36F,
            0.45F,
            0.68F,
            0.10F,
            0,
            1,
            1
    );

    private record ThermalFields(float surfaceTempC, float coreTempC) {
    }

    private record CompositionFields(
            float water,
            float fat,
            float protein,
            float aeration,
            float fragmentation,
            float cohesiveness,
            float proteinSet,
            float browning,
            float charLevel,
            float seasoningLoad,
            float cheeseLoad,
            float onionLoad,
            float herbLoad,
            float pepperLoad
    ) {
    }

    private record ProcessFields(
            float whiskWork,
            int stirCount,
            int flipCount,
            int timeInPan,
            int processDepth,
            boolean finalizedServing
    ) {
    }

    private static final Codec<ThermalFields> THERMAL_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("surface_temp_c").forGetter(ThermalFields::surfaceTempC),
            Codec.FLOAT.fieldOf("core_temp_c").forGetter(ThermalFields::coreTempC)
    ).apply(instance, ThermalFields::new));

    private static final Codec<CompositionFields> COMPOSITION_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("water").forGetter(CompositionFields::water),
            Codec.FLOAT.fieldOf("fat").forGetter(CompositionFields::fat),
            Codec.FLOAT.fieldOf("protein").forGetter(CompositionFields::protein),
            Codec.FLOAT.fieldOf("aeration").forGetter(CompositionFields::aeration),
            Codec.FLOAT.fieldOf("fragmentation").forGetter(CompositionFields::fragmentation),
            Codec.FLOAT.fieldOf("cohesiveness").forGetter(CompositionFields::cohesiveness),
            Codec.FLOAT.fieldOf("protein_set").forGetter(CompositionFields::proteinSet),
            Codec.FLOAT.fieldOf("browning").forGetter(CompositionFields::browning),
            Codec.FLOAT.fieldOf("char_level").forGetter(CompositionFields::charLevel),
            Codec.FLOAT.fieldOf("seasoning_load").forGetter(CompositionFields::seasoningLoad),
            Codec.FLOAT.fieldOf("cheese_load").forGetter(CompositionFields::cheeseLoad),
            Codec.FLOAT.fieldOf("onion_load").forGetter(CompositionFields::onionLoad),
            Codec.FLOAT.fieldOf("herb_load").forGetter(CompositionFields::herbLoad),
            Codec.FLOAT.fieldOf("pepper_load").forGetter(CompositionFields::pepperLoad)
    ).apply(instance, CompositionFields::new));

    private static final Codec<ProcessFields> PROCESS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("whisk_work").forGetter(ProcessFields::whiskWork),
            Codec.INT.fieldOf("stir_count").forGetter(ProcessFields::stirCount),
            Codec.INT.fieldOf("flip_count").forGetter(ProcessFields::flipCount),
            Codec.INT.fieldOf("time_in_pan").forGetter(ProcessFields::timeInPan),
            Codec.INT.fieldOf("process_depth").forGetter(ProcessFields::processDepth),
            Codec.BOOL.fieldOf("finalized_serving").forGetter(ProcessFields::finalizedServing)
    ).apply(instance, ProcessFields::new));

    public static final Codec<FoodMatterData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("created_tick").forGetter(FoodMatterData::createdTick),
            IngredientStateData.CODEC.optionalFieldOf("summary_hint", DEFAULT_SUMMARY_HINT).forGetter(FoodMatterData::summaryHint),
            Codec.LONG.optionalFieldOf("trait_mask", 0L).forGetter(FoodMatterData::traitMask),
            THERMAL_CODEC.fieldOf("thermal").forGetter(data -> new ThermalFields(data.surfaceTempC, data.coreTempC)),
            COMPOSITION_CODEC.fieldOf("composition").forGetter(data -> new CompositionFields(
                    data.water,
                    data.fat,
                    data.protein,
                    data.aeration,
                    data.fragmentation,
                    data.cohesiveness,
                    data.proteinSet,
                    data.browning,
                    data.charLevel,
                    data.seasoningLoad,
                    data.cheeseLoad,
                    data.onionLoad,
                    data.herbLoad,
                    data.pepperLoad
            )),
            PROCESS_CODEC.fieldOf("process").forGetter(data -> new ProcessFields(
                    data.whiskWork,
                    data.stirCount,
                    data.flipCount,
                    data.timeInPan,
                    data.processDepth,
                    data.finalizedServing
            ))
    ).apply(instance, (createdTick, summaryHint, traitMask, thermal, composition, process) -> new FoodMatterData(
            createdTick,
            summaryHint,
            traitMask,
            thermal.surfaceTempC(),
            thermal.coreTempC(),
            composition.water(),
            composition.fat(),
            composition.protein(),
            composition.aeration(),
            composition.fragmentation(),
            composition.cohesiveness(),
            composition.proteinSet(),
            composition.browning(),
            composition.charLevel(),
            composition.seasoningLoad(),
            composition.cheeseLoad(),
            composition.onionLoad(),
            composition.herbLoad(),
            composition.pepperLoad(),
            process.whiskWork(),
            process.stirCount(),
            process.flipCount(),
            process.timeInPan(),
            process.processDepth(),
            process.finalizedServing()
    )));

    public static FoodMatterData fromLegacy(IngredientStateData legacy, boolean finalizedServing) {
        return new FoodMatterData(
                legacy.createdTick(),
                legacy,
                0L,
                22.0F,
                22.0F,
                legacy.moisture(),
                0.10F,
                0.18F,
                legacy.aeration(),
                legacy.processDepth() > 0 ? 0.32F : 0.08F,
                legacy.structure(),
                legacy.state().isPlatedState() ? 0.78F : 0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                legacy.processDepth() > 0 ? 0.35F : 0.0F,
                0,
                0,
                0,
                legacy.processDepth(),
                finalizedServing
        );
    }

    public FoodMatterData clamp() {
        return new FoodMatterData(
                this.createdTick,
                this.summaryHint,
                this.traitMask,
                Math.max(0.0F, this.surfaceTempC),
                Math.max(0.0F, this.coreTempC),
                Mth.clamp(this.water, 0.0F, 1.0F),
                Mth.clamp(this.fat, 0.0F, 1.0F),
                Mth.clamp(this.protein, 0.0F, 1.0F),
                Mth.clamp(this.aeration, 0.0F, 1.0F),
                Mth.clamp(this.fragmentation, 0.0F, 1.0F),
                Mth.clamp(this.cohesiveness, 0.0F, 1.0F),
                Mth.clamp(this.proteinSet, 0.0F, 1.0F),
                Mth.clamp(this.browning, 0.0F, 1.0F),
                Mth.clamp(this.charLevel, 0.0F, 1.0F),
                Mth.clamp(this.seasoningLoad, 0.0F, 1.0F),
                Mth.clamp(this.cheeseLoad, 0.0F, 1.0F),
                Mth.clamp(this.onionLoad, 0.0F, 1.0F),
                Mth.clamp(this.herbLoad, 0.0F, 1.0F),
                Mth.clamp(this.pepperLoad, 0.0F, 1.0F),
                Mth.clamp(this.whiskWork, 0.0F, 2.0F),
                Math.max(0, this.stirCount),
                Math.max(0, this.flipCount),
                Math.max(0, this.timeInPan),
                Math.max(0, this.processDepth),
                this.finalizedServing
        );
    }

    public FoodMatterData withTemps(float newSurfaceTempC, float newCoreTempC) {
        return new FoodMatterData(
                this.createdTick,
                this.summaryHint,
                this.traitMask,
                newSurfaceTempC,
                newCoreTempC,
                this.water,
                this.fat,
                this.protein,
                this.aeration,
                this.fragmentation,
                this.cohesiveness,
                this.proteinSet,
                this.browning,
                this.charLevel,
                this.seasoningLoad,
                this.cheeseLoad,
                this.onionLoad,
                this.herbLoad,
                this.pepperLoad,
                this.whiskWork,
                this.stirCount,
                this.flipCount,
                this.timeInPan,
                this.processDepth,
                this.finalizedServing
        ).clamp();
    }

    public FoodMatterData withWorkingState(
            float newWater,
            float newAeration,
            float newFragmentation,
            float newCohesiveness,
            float newProteinSet,
            float newBrowning,
            float newCharLevel,
            float newWhiskWork,
            int newStirCount,
            int newFlipCount,
            int newTimeInPan,
            int newProcessDepth,
            boolean newFinalizedServing
    ) {
        return new FoodMatterData(
                this.createdTick,
                this.summaryHint,
                this.traitMask,
                this.surfaceTempC,
                this.coreTempC,
                newWater,
                this.fat,
                this.protein,
                newAeration,
                newFragmentation,
                newCohesiveness,
                newProteinSet,
                newBrowning,
                newCharLevel,
                this.seasoningLoad,
                this.cheeseLoad,
                this.onionLoad,
                this.herbLoad,
                this.pepperLoad,
                newWhiskWork,
                newStirCount,
                newFlipCount,
                newTimeInPan,
                newProcessDepth,
                newFinalizedServing
        ).clamp();
    }

    public FoodMatterData withFlavorLoads(
            float newFat,
            float newSeasoningLoad,
            float newCheeseLoad,
            float newOnionLoad,
            float newHerbLoad,
            float newPepperLoad
    ) {
        return new FoodMatterData(
                this.createdTick,
                this.summaryHint,
                this.traitMask,
                this.surfaceTempC,
                this.coreTempC,
                this.water,
                newFat,
                this.protein,
                this.aeration,
                this.fragmentation,
                this.cohesiveness,
                this.proteinSet,
                this.browning,
                this.charLevel,
                newSeasoningLoad,
                newCheeseLoad,
                newOnionLoad,
                newHerbLoad,
                newPepperLoad,
                this.whiskWork,
                this.stirCount,
                this.flipCount,
                this.timeInPan,
                this.processDepth,
                this.finalizedServing
        ).clamp();
    }

    public FoodMatterData withCreatedTick(long newCreatedTick) {
        return new FoodMatterData(
                newCreatedTick,
                this.summaryHint.withCreatedTick(newCreatedTick),
                this.traitMask,
                this.surfaceTempC,
                this.coreTempC,
                this.water,
                this.fat,
                this.protein,
                this.aeration,
                this.fragmentation,
                this.cohesiveness,
                this.proteinSet,
                this.browning,
                this.charLevel,
                this.seasoningLoad,
                this.cheeseLoad,
                this.onionLoad,
                this.herbLoad,
                this.pepperLoad,
                this.whiskWork,
                this.stirCount,
                this.flipCount,
                this.timeInPan,
                this.processDepth,
                this.finalizedServing
        ).clamp();
    }

    public FoodMatterData withAddedTraits(long addedTraitMask) {
        return new FoodMatterData(
                this.createdTick,
                this.summaryHint,
                this.traitMask | addedTraitMask,
                this.surfaceTempC,
                this.coreTempC,
                this.water,
                this.fat,
                this.protein,
                this.aeration,
                this.fragmentation,
                this.cohesiveness,
                this.proteinSet,
                this.browning,
                this.charLevel,
                this.seasoningLoad,
                this.cheeseLoad,
                this.onionLoad,
                this.herbLoad,
                this.pepperLoad,
                this.whiskWork,
                this.stirCount,
                this.flipCount,
                this.timeInPan,
                this.processDepth,
                this.finalizedServing
        ).clamp();
    }

    public boolean hasTrait(FoodTrait trait) {
        return FoodTrait.has(this.traitMask, trait);
    }

    public boolean isWorkedButUnfinished() {
        return !this.finalizedServing && this.processDepth > 0;
    }
}
