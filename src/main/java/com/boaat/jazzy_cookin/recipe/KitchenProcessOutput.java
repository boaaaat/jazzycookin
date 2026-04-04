package com.boaat.jazzy_cookin.recipe;

import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record KitchenProcessOutput(
        ItemStack result,
        IngredientState state,
        float qualityBonus,
        float flavorDelta,
        float textureDelta,
        float structureDelta,
        float moistureDelta,
        float purityDelta,
        float aerationDelta,
        int nourishment,
        int enjoyment
) {
    public static final Codec<KitchenProcessOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("result").forGetter(KitchenProcessOutput::result),
            IngredientState.CODEC.fieldOf("state").forGetter(KitchenProcessOutput::state),
            Codec.FLOAT.optionalFieldOf("quality_bonus", 0.0F).forGetter(KitchenProcessOutput::qualityBonus),
            Codec.FLOAT.optionalFieldOf("flavor_delta", 0.0F).forGetter(KitchenProcessOutput::flavorDelta),
            Codec.FLOAT.optionalFieldOf("texture_delta", 0.0F).forGetter(KitchenProcessOutput::textureDelta),
            Codec.FLOAT.optionalFieldOf("structure_delta", 0.0F).forGetter(KitchenProcessOutput::structureDelta),
            Codec.FLOAT.optionalFieldOf("moisture_delta", 0.0F).forGetter(KitchenProcessOutput::moistureDelta),
            Codec.FLOAT.optionalFieldOf("purity_delta", 0.0F).forGetter(KitchenProcessOutput::purityDelta),
            Codec.FLOAT.optionalFieldOf("aeration_delta", 0.0F).forGetter(KitchenProcessOutput::aerationDelta),
            Codec.INT.optionalFieldOf("nourishment", 0).forGetter(KitchenProcessOutput::nourishment),
            Codec.INT.optionalFieldOf("enjoyment", 0).forGetter(KitchenProcessOutput::enjoyment)
    ).apply(instance, KitchenProcessOutput::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, KitchenProcessOutput> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, KitchenProcessOutput::result,
            IngredientState.STREAM_CODEC, KitchenProcessOutput::state,
            ByteBufCodecs.FLOAT, KitchenProcessOutput::qualityBonus,
            ByteBufCodecs.FLOAT, KitchenProcessOutput::flavorDelta,
            ByteBufCodecs.FLOAT, KitchenProcessOutput::textureDelta,
            ByteBufCodecs.FLOAT, KitchenProcessOutput::structureDelta,
            ByteBufCodecs.FLOAT, KitchenProcessOutput::moistureDelta,
            ByteBufCodecs.FLOAT, KitchenProcessOutput::purityDelta,
            ByteBufCodecs.FLOAT, KitchenProcessOutput::aerationDelta,
            ByteBufCodecs.VAR_INT, KitchenProcessOutput::nourishment,
            ByteBufCodecs.VAR_INT, KitchenProcessOutput::enjoyment,
            KitchenProcessOutput::new
    );
}
