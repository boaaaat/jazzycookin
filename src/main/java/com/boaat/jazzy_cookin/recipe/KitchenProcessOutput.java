package com.boaat.jazzy_cookin.recipe;

import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record KitchenProcessOutput(
        ItemStack result,
        ItemStack byproduct,
        IngredientState state,
        float qualityBonus,
        float recipeAccuracyDelta,
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
            ItemStack.CODEC.optionalFieldOf("byproduct", ItemStack.EMPTY).forGetter(KitchenProcessOutput::byproduct),
            IngredientState.CODEC.fieldOf("state").forGetter(KitchenProcessOutput::state),
            Codec.FLOAT.optionalFieldOf("quality_bonus", 0.0F).forGetter(KitchenProcessOutput::qualityBonus),
            Codec.FLOAT.optionalFieldOf("recipe_accuracy_delta", 0.0F).forGetter(KitchenProcessOutput::recipeAccuracyDelta),
            Codec.FLOAT.optionalFieldOf("flavor_delta", 0.0F).forGetter(KitchenProcessOutput::flavorDelta),
            Codec.FLOAT.optionalFieldOf("texture_delta", 0.0F).forGetter(KitchenProcessOutput::textureDelta),
            Codec.FLOAT.optionalFieldOf("structure_delta", 0.0F).forGetter(KitchenProcessOutput::structureDelta),
            Codec.FLOAT.optionalFieldOf("moisture_delta", 0.0F).forGetter(KitchenProcessOutput::moistureDelta),
            Codec.FLOAT.optionalFieldOf("purity_delta", 0.0F).forGetter(KitchenProcessOutput::purityDelta),
            Codec.FLOAT.optionalFieldOf("aeration_delta", 0.0F).forGetter(KitchenProcessOutput::aerationDelta),
            Codec.INT.optionalFieldOf("nourishment", 0).forGetter(KitchenProcessOutput::nourishment),
            Codec.INT.optionalFieldOf("enjoyment", 0).forGetter(KitchenProcessOutput::enjoyment)
    ).apply(instance, KitchenProcessOutput::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, KitchenProcessOutput> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public KitchenProcessOutput decode(RegistryFriendlyByteBuf buffer) {
            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
            ItemStack byproduct = ItemStack.STREAM_CODEC.decode(buffer);
            IngredientState state = IngredientState.STREAM_CODEC.decode(buffer);
            float qualityBonus = buffer.readFloat();
            float recipeAccuracyDelta = buffer.readFloat();
            float flavorDelta = buffer.readFloat();
            float textureDelta = buffer.readFloat();
            float structureDelta = buffer.readFloat();
            float moistureDelta = buffer.readFloat();
            float purityDelta = buffer.readFloat();
            float aerationDelta = buffer.readFloat();
            int nourishment = buffer.readVarInt();
            int enjoyment = buffer.readVarInt();
            return new KitchenProcessOutput(
                    result,
                    byproduct,
                    state,
                    qualityBonus,
                    recipeAccuracyDelta,
                    flavorDelta,
                    textureDelta,
                    structureDelta,
                    moistureDelta,
                    purityDelta,
                    aerationDelta,
                    nourishment,
                    enjoyment
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, KitchenProcessOutput value) {
            ItemStack.STREAM_CODEC.encode(buffer, value.result());
            ItemStack.STREAM_CODEC.encode(buffer, value.byproduct());
            IngredientState.STREAM_CODEC.encode(buffer, value.state());
            buffer.writeFloat(value.qualityBonus());
            buffer.writeFloat(value.recipeAccuracyDelta());
            buffer.writeFloat(value.flavorDelta());
            buffer.writeFloat(value.textureDelta());
            buffer.writeFloat(value.structureDelta());
            buffer.writeFloat(value.moistureDelta());
            buffer.writeFloat(value.purityDelta());
            buffer.writeFloat(value.aerationDelta());
            buffer.writeVarInt(value.nourishment());
            buffer.writeVarInt(value.enjoyment());
        }
    };
}
