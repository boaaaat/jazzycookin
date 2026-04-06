package com.boaat.jazzy_cookin.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record KitchenRecipeGuideData(
        KitchenRecipeGuideKind kind,
        String guideKey,
        String tutorialKey,
        int tutorialStep,
        String recognizerKey,
        boolean allowFlexibleInputs,
        boolean allowSupportiveExtras,
        float minimumScore
) {
    public static final KitchenRecipeGuideData DEFAULT = new KitchenRecipeGuideData(
            KitchenRecipeGuideKind.KNOWN_DISH,
            "",
            "",
            0,
            "",
            true,
            true,
            0.72F
    );

    public static final Codec<KitchenRecipeGuideData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            KitchenRecipeGuideKind.CODEC.optionalFieldOf("kind", KitchenRecipeGuideKind.KNOWN_DISH).forGetter(KitchenRecipeGuideData::kind),
            Codec.STRING.optionalFieldOf("guide_key", "").forGetter(KitchenRecipeGuideData::guideKey),
            Codec.STRING.optionalFieldOf("tutorial_key", "").forGetter(KitchenRecipeGuideData::tutorialKey),
            Codec.INT.optionalFieldOf("tutorial_step", 0).forGetter(KitchenRecipeGuideData::tutorialStep),
            Codec.STRING.optionalFieldOf("recognizer_key", "").forGetter(KitchenRecipeGuideData::recognizerKey),
            Codec.BOOL.optionalFieldOf("allow_flexible_inputs", true).forGetter(KitchenRecipeGuideData::allowFlexibleInputs),
            Codec.BOOL.optionalFieldOf("allow_supportive_extras", true).forGetter(KitchenRecipeGuideData::allowSupportiveExtras),
            Codec.FLOAT.optionalFieldOf("minimum_score", 0.72F).forGetter(KitchenRecipeGuideData::minimumScore)
    ).apply(instance, KitchenRecipeGuideData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, KitchenRecipeGuideData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public KitchenRecipeGuideData decode(RegistryFriendlyByteBuf buffer) {
            return new KitchenRecipeGuideData(
                    KitchenRecipeGuideKind.STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.FLOAT.decode(buffer)
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, KitchenRecipeGuideData value) {
            KitchenRecipeGuideKind.STREAM_CODEC.encode(buffer, value.kind());
            ByteBufCodecs.STRING_UTF8.encode(buffer, value.guideKey());
            ByteBufCodecs.STRING_UTF8.encode(buffer, value.tutorialKey());
            ByteBufCodecs.VAR_INT.encode(buffer, value.tutorialStep());
            ByteBufCodecs.STRING_UTF8.encode(buffer, value.recognizerKey());
            ByteBufCodecs.BOOL.encode(buffer, value.allowFlexibleInputs());
            ByteBufCodecs.BOOL.encode(buffer, value.allowSupportiveExtras());
            ByteBufCodecs.FLOAT.encode(buffer, value.minimumScore());
        }
    };

    public String guideKeyOrDefault(String fallback) {
        return this.guideKey.isBlank() ? fallback : this.guideKey;
    }

    public String recognizerKeyOrDefault(String fallback) {
        return this.recognizerKey.isBlank() ? fallback : this.recognizerKey;
    }
}
