package com.boaat.jazzy_cookin.kitchen.sim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record StationPhysicsState(float panTemperatureC) {
    public static final Codec<StationPhysicsState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("pan_temperature_c").forGetter(StationPhysicsState::panTemperatureC)
    ).apply(instance, StationPhysicsState::new));

    public static StationPhysicsState idle() {
        return new StationPhysicsState(22.0F);
    }
}
