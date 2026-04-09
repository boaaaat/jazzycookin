package com.boaat.jazzy_cookin.kitchen.sim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Mth;

public record FoodMatterData(
        long createdTick,
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
        float preservationLevel,
        float oxidation,
        float microbialLoad,
        float whiskWork,
        int stirCount,
        int flipCount,
        int timeInPan,
        int processDepth,
        boolean finalizedServing
) {
    public static final float UNSET_ENVIRONMENT = -1.0F;

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

    private record EnvironmentFields(float preservationLevel, float oxidation, float microbialLoad) {
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

    private static final EnvironmentFields DEFAULT_ENVIRONMENT = new EnvironmentFields(UNSET_ENVIRONMENT, UNSET_ENVIRONMENT, UNSET_ENVIRONMENT);

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

    private static final Codec<EnvironmentFields> ENVIRONMENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("preservation_level").forGetter(EnvironmentFields::preservationLevel),
            Codec.FLOAT.fieldOf("oxidation").forGetter(EnvironmentFields::oxidation),
            Codec.FLOAT.fieldOf("microbial_load").forGetter(EnvironmentFields::microbialLoad)
    ).apply(instance, EnvironmentFields::new));

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
            ENVIRONMENT_CODEC.optionalFieldOf("environment", DEFAULT_ENVIRONMENT).forGetter(data -> new EnvironmentFields(
                    data.preservationLevel,
                    data.oxidation,
                    data.microbialLoad
            )),
            PROCESS_CODEC.fieldOf("process").forGetter(data -> new ProcessFields(
                    data.whiskWork,
                    data.stirCount,
                    data.flipCount,
                    data.timeInPan,
                    data.processDepth,
                    data.finalizedServing
            ))
    ).apply(instance, (createdTick, traitMask, thermal, composition, environment, process) -> new FoodMatterData(
            createdTick,
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
            environment.preservationLevel(),
            environment.oxidation(),
            environment.microbialLoad(),
            process.whiskWork(),
            process.stirCount(),
            process.flipCount(),
            process.timeInPan(),
            process.processDepth(),
            process.finalizedServing()
    )));

    public static float derivePreservationLevel(
            long traitMask,
            float water,
            float protein,
            float fragmentation,
            int processDepth,
            boolean finalizedServing
    ) {
        float preservation = 0.0F;
        if (FoodTrait.has(traitMask, FoodTrait.PRESERVE)) {
            preservation = Math.max(preservation, 0.45F);
        }
        if (FoodTrait.has(traitMask, FoodTrait.FERMENTED)) {
            preservation = Math.max(preservation, 0.55F);
        }
        if (FoodTrait.has(traitMask, FoodTrait.ACIDIC)) {
            preservation = Math.max(preservation, 0.10F);
        }
        if (water <= 0.10F) {
            preservation = Math.max(preservation, 0.95F);
        } else if (water <= 0.22F) {
            preservation = Math.max(preservation, 0.68F);
        } else if (water <= 0.34F && fragmentation >= 0.18F) {
            preservation = Math.max(preservation, 0.42F);
        }
        if (protein <= 0.05F && water <= 0.22F) {
            preservation = Math.max(preservation, 0.74F);
        }
        if (processDepth >= 2 && water <= 0.35F) {
            preservation = Math.max(preservation, 0.22F);
        }
        if (finalizedServing && preservation < 0.20F) {
            preservation = 0.12F;
        }
        return Mth.clamp(preservation, 0.0F, 1.0F);
    }

    public static float deriveOxidation(
            long traitMask,
            float fat,
            float browning,
            float charLevel,
            float preservationLevel
    ) {
        float oxidation = 0.03F + fat * 0.18F + browning * 0.06F + charLevel * 0.06F - preservationLevel * 0.10F;
        if (FoodTrait.has(traitMask, FoodTrait.ACIDIC)) {
            oxidation *= 0.85F;
        }
        if (waterLowStable(preservationLevel, fat)) {
            oxidation *= 0.55F;
        }
        return Mth.clamp(oxidation, 0.0F, 1.0F);
    }

    public static float deriveMicrobialLoad(
            long traitMask,
            float water,
            float protein,
            float preservationLevel,
            boolean finalizedServing
    ) {
        float microbial = 0.02F + water * 0.14F + protein * 0.10F - preservationLevel * 0.16F;
        if (FoodTrait.has(traitMask, FoodTrait.ACIDIC)) {
            microbial -= 0.03F;
        }
        if (water <= 0.18F) {
            microbial *= 0.25F;
        } else if (water <= 0.30F) {
            microbial *= 0.55F;
        }
        if (finalizedServing) {
            microbial += 0.01F;
        }
        return Mth.clamp(microbial, 0.0F, 1.0F);
    }

    private static boolean waterLowStable(float preservationLevel, float fat) {
        return preservationLevel >= 0.65F || (preservationLevel >= 0.40F && fat <= 0.18F);
    }

    public FoodMatterData clamp() {
        float normalizedPreservation = this.preservationLevel >= 0.0F
                ? this.preservationLevel
                : derivePreservationLevel(this.traitMask, this.water, this.protein, this.fragmentation, this.processDepth, this.finalizedServing);
        float normalizedOxidation = this.oxidation >= 0.0F
                ? this.oxidation
                : deriveOxidation(this.traitMask, this.fat, this.browning, this.charLevel, normalizedPreservation);
        float normalizedMicrobialLoad = this.microbialLoad >= 0.0F
                ? this.microbialLoad
                : deriveMicrobialLoad(this.traitMask, this.water, this.protein, normalizedPreservation, this.finalizedServing);

        return new FoodMatterData(
                this.createdTick,
                this.traitMask,
                this.surfaceTempC,
                this.coreTempC,
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
                Mth.clamp(normalizedPreservation, 0.0F, 1.0F),
                Mth.clamp(normalizedOxidation, 0.0F, 1.0F),
                Mth.clamp(normalizedMicrobialLoad, 0.0F, 1.0F),
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
                this.preservationLevel,
                this.oxidation,
                this.microbialLoad,
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
                this.preservationLevel,
                this.oxidation,
                this.microbialLoad,
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
                this.preservationLevel,
                this.oxidation,
                this.microbialLoad,
                this.whiskWork,
                this.stirCount,
                this.flipCount,
                this.timeInPan,
                this.processDepth,
                this.finalizedServing
        ).clamp();
    }

    public FoodMatterData withPreservationState(float newPreservationLevel, float newOxidation, float newMicrobialLoad) {
        return new FoodMatterData(
                this.createdTick,
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
                newPreservationLevel,
                newOxidation,
                newMicrobialLoad,
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
                this.preservationLevel,
                this.oxidation,
                this.microbialLoad,
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
                this.preservationLevel,
                this.oxidation,
                this.microbialLoad,
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

    public boolean isPreservedShelfStable() {
        return this.preservationLevel >= 0.70F || (this.preservationLevel >= 0.45F && this.water <= 0.22F);
    }

    public boolean isWorkedButUnfinished() {
        return !this.finalizedServing && this.processDepth > 0;
    }
}
