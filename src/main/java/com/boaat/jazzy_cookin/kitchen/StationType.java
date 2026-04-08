package com.boaat.jazzy_cookin.kitchen;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.boaat.jazzy_cookin.kitchen.sim.domain.SimulationDomainType;
import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum StationType implements StringRepresentable {
    PREP_TABLE("prep_table", true, false),
    SPICE_GRINDER("spice_grinder", true, false),
    STRAINER("strainer", true, false),
    MIXING_BOWL("mixing_bowl", true, false),
    MICROWAVE("microwave", false, true),
    FOOD_PROCESSOR("food_processor", false, false),
    BLENDER("blender", false, false),
    JUICER("juicer", false, false),
    FREEZE_DRYER("freeze_dryer", false, false),
    CANNING_STATION("canning_station", true, true),
    DRYING_RACK("drying_rack", false, false),
    SMOKER("smoker", true, true),
    FERMENTATION_CROCK("fermentation_crock", false, false),
    STEAMER("steamer", true, true),
    STOVE("stove", true, true),
    OVEN("oven", true, true),
    COOLING_RACK("cooling_rack", false, false),
    RESTING_BOARD("resting_board", false, false),
    PLATING_STATION("plating_station", false, false);

    private static final Map<String, StationType> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(StationType::getSerializedName, Function.identity()));

    public static final Codec<StationType> CODEC = Codec.STRING.xmap(StationType::byName, StationType::getSerializedName);
    public static final StreamCodec<ByteBuf, StationType> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            StationType::byName,
            StationType::getSerializedName
    );

    private final String serializedName;
    private final boolean usesTools;
    private final boolean supportsHeat;

    StationType(String serializedName, boolean usesTools, boolean supportsHeat) {
        this.serializedName = serializedName;
        this.usesTools = usesTools;
        this.supportsHeat = supportsHeat;
    }

    public static StationType byName(String name) {
        return BY_NAME.getOrDefault(name, PREP_TABLE);
    }

    public boolean supportsHeat() {
        return this.supportsHeat;
    }

    public boolean usesTools() {
        return this.usesTools;
    }

    public boolean supportsStationControl() {
        return this == PREP_TABLE
                || this == SPICE_GRINDER
                || this == MIXING_BOWL
                || this == FOOD_PROCESSOR
                || this == BLENDER;
    }

    public boolean usesFuel() {
        return this == STOVE || this == OVEN;
    }

    public SimulationDomainType simulationDomain() {
        return switch (this) {
            case PLATING_STATION -> SimulationDomainType.PLATE;
            case MIXING_BOWL -> SimulationDomainType.MIX;
            case STOVE -> SimulationDomainType.PAN;
            case FOOD_PROCESSOR -> SimulationDomainType.PROCESS;
            case BLENDER -> SimulationDomainType.BLEND;
            case JUICER -> SimulationDomainType.JUICE;
            case FREEZE_DRYER -> SimulationDomainType.DRY;
            case PREP_TABLE -> SimulationDomainType.PREP;
            case CANNING_STATION, FERMENTATION_CROCK, DRYING_RACK -> SimulationDomainType.PRESERVE;
            case OVEN, MICROWAVE, SMOKER, STEAMER -> SimulationDomainType.OVEN;
            case COOLING_RACK, RESTING_BOARD -> SimulationDomainType.REST;
            default -> SimulationDomainType.NONE;
        };
    }

    public Component controlLabel(int controlIndex) {
        int clamped = Math.max(0, Math.min(2, controlIndex));
        String band = switch (clamped) {
            case 0 -> "low";
            case 2 -> "high";
            default -> "medium";
        };
        String family = switch (this) {
            case PREP_TABLE -> "prep";
            case SPICE_GRINDER -> "grind";
            case MIXING_BOWL -> "mix";
            case FOOD_PROCESSOR -> "process";
            case BLENDER -> "blend";
            default -> "generic";
        };
        return Component.translatable("control.jazzycookin." + family + "." + band);
    }

    public Component displayName() {
        return Component.translatable("station.jazzycookin." + this.serializedName);
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
