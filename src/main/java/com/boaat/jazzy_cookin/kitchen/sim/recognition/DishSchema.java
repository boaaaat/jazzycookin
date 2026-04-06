package com.boaat.jazzy_cookin.kitchen.sim.recognition;

import java.util.List;
import java.util.function.Supplier;

import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;

public final class DishSchema {
    private static final DishSchema SOFT_SCRAMBLED = new DishSchema(
            "soft_scrambled_eggs",
            1,
            JazzyItems.SOFT_SCRAMBLED_EGGS,
            0.45F,
            0.65F,
            0.45F,
            0.75F,
            0.0F,
            0.18F,
            0.55F,
            1.0F,
            0
    );
    private static final DishSchema SCRAMBLED = new DishSchema(
            "scrambled_eggs",
            2,
            JazzyItems.SCRAMBLED_EGGS,
            0.60F,
            0.82F,
            0.25F,
            0.55F,
            0.0F,
            0.30F,
            0.45F,
            1.0F,
            0
    );
    private static final DishSchema OMELET = new DishSchema(
            "omelet",
            3,
            JazzyItems.OMELET,
            0.58F,
            0.80F,
            0.30F,
            0.60F,
            0.0F,
            0.22F,
            0.0F,
            0.35F,
            1
    );
    private static final DishSchema BROWNED_OMELET = new DishSchema(
            "browned_omelet",
            4,
            JazzyItems.BROWNED_OMELET,
            0.58F,
            0.80F,
            0.30F,
            0.60F,
            0.22F,
            0.45F,
            0.0F,
            0.35F,
            1
    );

    private static final List<DishSchema> EGG_DISHES = List.of(SOFT_SCRAMBLED, SCRAMBLED, OMELET, BROWNED_OMELET);

    private final String key;
    private final int previewId;
    private final Supplier<? extends Item> resultItem;
    private final float minProteinSet;
    private final float maxProteinSet;
    private final float minMoisture;
    private final float maxMoisture;
    private final float minBrowning;
    private final float maxBrowning;
    private final float minFragmentation;
    private final float maxFragmentation;
    private final int minFlipCount;

    private DishSchema(
            String key,
            int previewId,
            Supplier<? extends Item> resultItem,
            float minProteinSet,
            float maxProteinSet,
            float minMoisture,
            float maxMoisture,
            float minBrowning,
            float maxBrowning,
            float minFragmentation,
            float maxFragmentation,
            int minFlipCount
    ) {
        this.key = key;
        this.previewId = previewId;
        this.resultItem = resultItem;
        this.minProteinSet = minProteinSet;
        this.maxProteinSet = maxProteinSet;
        this.minMoisture = minMoisture;
        this.maxMoisture = maxMoisture;
        this.minBrowning = minBrowning;
        this.maxBrowning = maxBrowning;
        this.minFragmentation = minFragmentation;
        this.maxFragmentation = maxFragmentation;
        this.minFlipCount = minFlipCount;
    }

    public float score(FoodMatterData data) {
        if (data.flipCount() < this.minFlipCount) {
            return 0.0F;
        }

        float proteinScore = bandScore(data.proteinSet(), this.minProteinSet, this.maxProteinSet, 0.22F);
        float moistureScore = bandScore(data.water(), this.minMoisture, this.maxMoisture, 0.22F);
        float browningScore = bandScore(data.browning(), this.minBrowning, this.maxBrowning, 0.18F);
        float fragmentationScore = bandScore(data.fragmentation(), this.minFragmentation, this.maxFragmentation, 0.20F);
        float charPenalty = data.charLevel() * 0.65F;
        float seasoningBonus = Math.min(0.08F, data.seasoningLoad() * 0.10F + data.herbLoad() * 0.08F);

        return Mth.clamp((proteinScore + moistureScore + browningScore + fragmentationScore) * 0.25F + seasoningBonus - charPenalty, 0.0F, 1.0F);
    }

    public DishRecognitionResult result(float score) {
        return new DishRecognitionResult(this.key, this.previewId, this.resultItem, score);
    }

    private static float bandScore(float value, float min, float max, float tolerance) {
        if (value >= min && value <= max) {
            return 1.0F;
        }
        if (value < min) {
            return Mth.clamp(1.0F - (min - value) / tolerance, 0.0F, 1.0F);
        }
        return Mth.clamp(1.0F - (value - max) / tolerance, 0.0F, 1.0F);
    }

    public static DishRecognitionResult preview(FoodMatterData data) {
        if (isBurnt(data)) {
            return new DishRecognitionResult("burnt_eggs", 5, JazzyItems.BURNT_EGGS, 1.0F);
        }

        DishRecognitionResult best = null;
        for (DishSchema schema : EGG_DISHES) {
            float score = schema.score(data);
            if (best == null || score > best.score()) {
                best = schema.result(score);
            }
        }
        return best != null && best.score() >= 0.40F ? best : null;
    }

    public static DishRecognitionResult finalizeResult(FoodMatterData data) {
        if (isBurnt(data)) {
            return new DishRecognitionResult("burnt_eggs", 5, JazzyItems.BURNT_EGGS, 1.0F);
        }

        DishRecognitionResult best = null;
        for (DishSchema schema : EGG_DISHES) {
            float score = schema.score(data);
            if (best == null || score > best.score()) {
                best = schema.result(score);
            }
        }
        if (best != null && best.score() >= 0.65F) {
            return best;
        }
        return new DishRecognitionResult("scrambled_eggs", 2, JazzyItems.SCRAMBLED_EGGS, best != null ? best.score() : 0.50F);
    }

    private static boolean isBurnt(FoodMatterData data) {
        return data.charLevel() >= 0.25F || data.browning() >= 0.65F;
    }
}
