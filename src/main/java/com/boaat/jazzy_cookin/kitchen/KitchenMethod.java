package com.boaat.jazzy_cookin.kitchen;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum KitchenMethod implements StringRepresentable {
    NONE("none"),
    CUT("cut"),
    GRIND("grind"),
    STRAIN("strain"),
    MIX("mix"),
    WHISK("whisk"),
    KNEAD("knead"),
    BATTER("batter"),
    MARINATE("marinate"),
    BOIL("boil"),
    SIMMER("simmer"),
    PAN_FRY("pan_fry"),
    DEEP_FRY("deep_fry"),
    BAKE("bake"),
    ROAST("roast"),
    BROIL("broil"),
    STEAM("steam"),
    SMOKE("smoke"),
    FERMENT("ferment"),
    CAN("can"),
    DRY("dry"),
    COOL("cool"),
    REST("rest"),
    SLICE("slice"),
    PLATE("plate");

    private static final Map<String, KitchenMethod> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(KitchenMethod::getSerializedName, Function.identity()));

    public static final Codec<KitchenMethod> CODEC = Codec.STRING.xmap(KitchenMethod::byName, KitchenMethod::getSerializedName);
    public static final StreamCodec<ByteBuf, KitchenMethod> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            KitchenMethod::byName,
            KitchenMethod::getSerializedName
    );

    private final String serializedName;

    KitchenMethod(String serializedName) {
        this.serializedName = serializedName;
    }

    public static KitchenMethod byName(String name) {
        return BY_NAME.getOrDefault(name, NONE);
    }

    public static KitchenMethod infer(
            StationType station,
            HeatLevel preferredHeat,
            ToolProfile preferredTool,
            IngredientState outputState,
            ProcessMode mode,
            boolean requiresNearbyWater,
            boolean requiresPreheat
    ) {
        if (outputState.isPlatedState()) {
            return PLATE;
        }

        return switch (station) {
            case SPICE_GRINDER -> GRIND;
            case STRAINER -> STRAIN;
            case MIXING_BOWL -> inferBowlMethod(outputState, preferredTool);
            case CANNING_STATION -> CAN;
            case DRYING_RACK -> DRY;
            case SMOKER -> SMOKE;
            case FERMENTATION_CROCK -> FERMENT;
            case STEAMER -> STEAM;
            case COOLING_RACK -> COOL;
            case RESTING_BOARD -> inferBoardMethod(outputState);
            case OVEN -> inferOvenMethod(outputState, preferredHeat, requiresPreheat);
            case STOVE -> inferStoveMethod(outputState, preferredHeat, preferredTool, mode, requiresNearbyWater);
            case PREP_TABLE -> inferPrepMethod(outputState);
            case PLATING_STATION -> PLATE;
        };
    }

    private static KitchenMethod inferPrepMethod(IngredientState outputState) {
        return switch (outputState) {
            case PEELED_APPLE, SLICED_APPLE, CHOPPED_TOMATO, CHOPPED_HERB, CHOPPED_CABBAGE, DICED_ONION, CLEANED_FISH, ROAST_CUT -> CUT;
            case GROUND_SPICE, GROUND_HERB -> GRIND;
            case SLICED_BREAD, SLICED_PIE -> SLICE;
            default -> CUT;
        };
    }

    private static KitchenMethod inferBowlMethod(IngredientState outputState, ToolProfile preferredTool) {
        if (preferredTool == ToolProfile.WHISK || outputState == IngredientState.WHISKED || outputState == IngredientState.UNDERWHISKED
                || outputState == IngredientState.OVERWHISKED || outputState == IngredientState.SEPARATED) {
            return WHISK;
        }
        if (preferredTool == ToolProfile.ROLLING_PIN || outputState == IngredientState.DOUGH || outputState == IngredientState.BREAD_DOUGH
                || outputState == IngredientState.DUMPLING_DOUGH || outputState == IngredientState.ROUGH_DOUGH
                || outputState == IngredientState.DEVELOPED_DOUGH || outputState == IngredientState.OVERWORKED_DOUGH
                || outputState == IngredientState.SMOOTH_DOUGH || outputState == IngredientState.ELASTIC_DOUGH) {
            return KNEAD;
        }
        if (outputState == IngredientState.BATTER || outputState == IngredientState.BATTERED || outputState == IngredientState.BATTERED_PROTEIN) {
            return BATTER;
        }
        if (outputState == IngredientState.MARINADE || outputState == IngredientState.MARINATED || outputState == IngredientState.MARINATED_PROTEIN) {
            return MARINATE;
        }
        return MIX;
    }

    private static KitchenMethod inferBoardMethod(IngredientState outputState) {
        return switch (outputState) {
            case RESTED_BREAD, RESTED_PIE, RESTED -> REST;
            case SLICED_BREAD, SLICED_PIE, SLICED -> SLICE;
            default -> REST;
        };
    }

    private static KitchenMethod inferOvenMethod(IngredientState outputState, HeatLevel preferredHeat, boolean requiresPreheat) {
        if (outputState == IngredientState.BROILED_PROTEIN || outputState == IngredientState.BROILED) {
            return BROIL;
        }
        if (outputState == IngredientState.ROASTED_PROTEIN || outputState == IngredientState.ROAST_VEGETABLES || outputState == IngredientState.ROASTED) {
            return ROAST;
        }
        if (requiresPreheat || preferredHeat != HeatLevel.LOW) {
            return BAKE;
        }
        return ROAST;
    }

    private static KitchenMethod inferStoveMethod(
            IngredientState outputState,
            HeatLevel preferredHeat,
            ToolProfile preferredTool,
            ProcessMode mode,
            boolean requiresNearbyWater
    ) {
        if (preferredTool == ToolProfile.SKILLET || outputState == IngredientState.PAN_FRIED) {
            return PAN_FRY;
        }
        if (outputState == IngredientState.FRIED_PROTEIN || outputState == IngredientState.DEEP_FRIED
                || outputState == IngredientState.USED_OIL || outputState == IngredientState.DIRTY_OIL || outputState == IngredientState.BURNT_OIL) {
            return DEEP_FRY;
        }
        if (requiresNearbyWater) {
            return STEAM;
        }
        if (mode == ProcessMode.PASSIVE && preferredHeat == HeatLevel.LOW) {
            return SIMMER;
        }
        if (outputState == IngredientState.SIMMERED_FILLING || outputState == IngredientState.SOUP_BASE || outputState == IngredientState.STRAINED_SOUP
                || outputState == IngredientState.SIMMERED) {
            return SIMMER;
        }
        if (preferredHeat == HeatLevel.HIGH && preferredTool == ToolProfile.POT) {
            return BOIL;
        }
        return SIMMER;
    }

    public boolean isCookMethod() {
        return switch (this) {
            case BOIL, SIMMER, PAN_FRY, DEEP_FRY, BAKE, ROAST, BROIL, STEAM, SMOKE, FERMENT, CAN, DRY -> true;
            default -> false;
        };
    }

    public boolean isFinishMethod() {
        return this == COOL || this == REST || this == SLICE || this == PLATE;
    }

    public Component displayName() {
        return Component.translatable("method.jazzycookin." + this.serializedName);
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
