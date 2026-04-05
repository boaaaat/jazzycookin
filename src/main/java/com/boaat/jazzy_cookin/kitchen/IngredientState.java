package com.boaat.jazzy_cookin.kitchen;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum IngredientState implements StringRepresentable {
    RAW("raw"),
    WASHED("washed"),
    WHOLE("whole"),
    PEELED("peeled"),
    SLICED("sliced"),
    CHOPPED("chopped"),
    DICED("diced"),
    MINCED("minced"),
    ROUGH_CUT("rough_cut"),
    STRAINED("strained"),
    CRUSHED("crushed"),
    COARSE_PASTE("coarse_paste"),
    SMOOTH_PASTE("smooth_paste"),
    OVERWORKED_PASTE("overworked_paste"),
    COARSE_POWDER("coarse_powder"),
    FINE_POWDER("fine_powder"),
    MIXED("mixed"),
    SMOOTH_MIXTURE("smooth_mixture"),
    LUMPY_MIXTURE("lumpy_mixture"),
    UNDERWHISKED("underwhisked"),
    WHISKED("whisked"),
    OVERWHISKED("overwhisked"),
    SEPARATED("separated"),
    SHAGGY_DOUGH("shaggy_dough"),
    ROUGH_DOUGH("rough_dough"),
    DEVELOPING_DOUGH("developing_dough"),
    DEVELOPED_DOUGH("developed_dough"),
    SMOOTH_DOUGH("smooth_dough"),
    ELASTIC_DOUGH("elastic_dough"),
    OVERWORKED_DOUGH("overworked_dough"),
    BATTERED("battered"),
    MARINATED("marinated"),
    BOILED("boiled"),
    SIMMERED("simmered"),
    PAN_FRIED("pan_fried"),
    DEEP_FRIED("deep_fried"),
    BAKED("baked"),
    ROASTED("roasted"),
    STEAMED("steamed"),
    SMOKED("smoked"),
    FERMENTED("fermented"),
    COOLED("cooled"),
    RESTED("rested"),
    PLATED("plated"),
    PANTRY_READY("pantry_ready"),
    WHOLE_APPLE("whole_apple"),
    WILD_BERRIES("wild_berries"),
    PEELED_APPLE("peeled_apple"),
    SLICED_APPLE("sliced_apple"),
    WHOLE_TOMATO("whole_tomato"),
    CHOPPED_TOMATO("chopped_tomato"),
    WHOLE_HERB("whole_herb"),
    CHOPPED_HERB("chopped_herb"),
    GROUND_HERB("ground_herb"),
    DRIED_HERB("dried_herb"),
    WHOLE_WHEAT("whole_wheat"),
    WHOLE_CABBAGE("whole_cabbage"),
    CHOPPED_CABBAGE("chopped_cabbage"),
    WHOLE_ONION("whole_onion"),
    DICED_ONION("diced_onion"),
    WHOLE_EGG("whole_egg"),
    FRESH_MILK("fresh_milk"),
    RAW_FISH("raw_fish"),
    CLEANED_FISH("cleaned_fish"),
    RAW_PROTEIN("raw_protein"),
    MARINADE("marinade"),
    MARINATED_PROTEIN("marinated_protein"),
    BATTER("batter"),
    BATTERED_PROTEIN("battered_protein"),
    FRIED_PROTEIN("fried_protein"),
    ROAST_CUT("roast_cut"),
    ROASTED_PROTEIN("roasted_protein"),
    BROILED_PROTEIN("broiled_protein"),
    SMOKED_PROTEIN("smoked_protein"),
    WHOLE_SPICE("whole_spice"),
    GROUND_SPICE("ground_spice"),
    CRUST_MIX("crust_mix"),
    DOUGH("dough"),
    BREAD_DOUGH("bread_dough"),
    BAKED_BREAD("baked_bread"),
    RESTED_BREAD("rested_bread"),
    SLICED_BREAD("sliced_bread"),
    SOUP_BASE("soup_base"),
    STRAINED_SOUP("strained_soup"),
    DUMPLING_FILLING("dumpling_filling"),
    DUMPLING_DOUGH("dumpling_dough"),
    RAW_DUMPLINGS("raw_dumplings"),
    STEAMED_DUMPLINGS("steamed_dumplings"),
    ROAST_VEGETABLES("roast_vegetables"),
    CANNED_TOMATO("canned_tomato"),
    APPLE_PRESERVE("apple_preserve"),
    DRIED_FRUIT("dried_fruit"),
    FERMENTED_VEGETABLE("fermented_vegetable"),
    CULTURED_DAIRY("cultured_dairy"),
    FRESH_OIL("fresh_oil"),
    USED_OIL("used_oil"),
    DIRTY_OIL("dirty_oil"),
    BURNT_OIL("burnt_oil"),
    SIMMERED_FILLING("simmered_filling"),
    RAW_ASSEMBLED_PIE("raw_assembled_pie"),
    BAKED_PIE("baked_pie"),
    COOLED_PIE("cooled_pie"),
    RESTED_PIE("rested_pie"),
    SLICED_PIE("sliced_pie"),
    PLATED_SLICE("plated_slice"),
    PLATED_SOUP_MEAL("plated_soup_meal"),
    PLATED_DUMPLING_MEAL("plated_dumpling_meal"),
    PLATED_FRIED_MEAL("plated_fried_meal"),
    PLATED_ROAST_MEAL("plated_roast_meal"),
    SPOILED("spoiled"),
    MOLDY("moldy");

    private static final Map<String, IngredientState> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(IngredientState::getSerializedName, Function.identity()));

    public static final Codec<IngredientState> CODEC = Codec.STRING.xmap(IngredientState::byName, IngredientState::getSerializedName);
    public static final StreamCodec<ByteBuf, IngredientState> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            IngredientState::byName,
            IngredientState::getSerializedName
    );

    private final String serializedName;

    IngredientState(String serializedName) {
        this.serializedName = serializedName;
    }

    public static IngredientState byName(String name) {
        return BY_NAME.getOrDefault(name, PANTRY_READY);
    }

    public boolean isPlatedState() {
        return this == PLATED_SLICE
                || this == PLATED_SOUP_MEAL
                || this == PLATED_DUMPLING_MEAL
                || this == PLATED_FRIED_MEAL
                || this == PLATED_ROAST_MEAL
                || this == PLATED;
    }

    public boolean isSpoiledState() {
        return this == SPOILED || this == MOLDY;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
