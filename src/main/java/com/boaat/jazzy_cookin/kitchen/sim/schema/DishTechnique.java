package com.boaat.jazzy_cookin.kitchen.sim.schema;

import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

public enum DishTechnique implements StringRepresentable {
    PREPPED("prepped"),
    CUT("cut"),
    MIXED("mixed"),
    DIP_OR_COAT("dip_or_coat"),
    SIMMERED("simmered"),
    PAN_FRIED("pan_fried"),
    BAKED("baked"),
    RESTED("rested"),
    PLATED("plated");

    public static final Codec<DishTechnique> CODEC = StringRepresentable.fromEnum(DishTechnique::values);

    private final String serializedName;

    DishTechnique(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

    public float score(FoodMatterData matter, IngredientState state) {
        if (matter == null || state == null) {
            return 0.0F;
        }
        return switch (this) {
            case PREPPED -> matter.processDepth() > 0 ? 1.0F : 0.35F;
            case CUT -> switch (state) {
                case SLICED, CHOPPED, DICED, MINCED, ROUGH_CUT, SLICED_APPLE, CHOPPED_TOMATO,
                        CHOPPED_HERB, CHOPPED_CABBAGE, DICED_ONION, SLICED_BREAD -> 1.0F;
                default -> matter.fragmentation() >= 0.24F ? 0.72F : 0.25F;
            };
            case MIXED -> Math.max(matter.whiskWork() >= 0.18F ? 0.85F : 0.0F, matter.cohesiveness() >= 0.36F ? 0.72F : 0.28F);
            case DIP_OR_COAT -> state == IngredientState.BATTERED || state == IngredientState.BATTERED_PROTEIN
                    || (matter.cohesiveness() >= 0.30F && matter.water() >= 0.18F && matter.processDepth() >= 1) ? 1.0F : 0.22F;
            case SIMMERED -> switch (state) {
                case SIMMERED, SOUP_BASE, STRAINED_SOUP, SIMMERED_FILLING, BOILED -> 1.0F;
                default -> matter.water() >= 0.48F && matter.processDepth() >= 1 ? 0.72F : 0.20F;
            };
            case PAN_FRIED -> switch (state) {
                case PAN_FRIED, DEEP_FRIED, FRIED_PROTEIN, GLAZED -> 1.0F;
                default -> matter.timeInPan() > 0 || matter.browning() >= 0.08F || matter.proteinSet() >= 0.45F ? 0.78F : 0.18F;
            };
            case BAKED -> switch (state) {
                case BAKED, BAKED_BREAD, BAKED_PIE, ROASTED, COOLED_PIE, RESTED_PIE, RESTED_BREAD -> 1.0F;
                default -> matter.coreTempC() >= 80.0F && matter.cohesiveness() >= 0.42F ? 0.70F : 0.18F;
            };
            case RESTED -> switch (state) {
                case RESTED, RESTED_BREAD, RESTED_PIE, COOLED, COOLED_PIE -> 1.0F;
                default -> matter.processDepth() >= 2 ? 0.62F : 0.18F;
            };
            case PLATED -> matter.finalizedServing() || state.isPlatedState() ? 1.0F : 0.20F;
        };
    }
}
