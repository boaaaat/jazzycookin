package com.boaat.jazzy_cookin.kitchen.sim.recognition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;
import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;

public final class DishSchema {
    @FunctionalInterface
    private interface RecognitionScorer {
        float score(FoodMatterData matter);
    }

    private record Schema(
            String key,
            int previewId,
            Supplier<? extends Item> resultItem,
            float previewThreshold,
            float finalizeThreshold,
            float desirability,
            RecognitionScorer scorer
    ) {
        private DishRecognitionResult preview(FoodMatterData matter) {
            float score = this.scorer.score(matter);
            if (score < this.previewThreshold) {
                return null;
            }
            return new DishRecognitionResult(this.key, this.previewId, this.resultItem, score, this.desirability);
        }

        private DishRecognitionResult finalizeMatch(FoodMatterData matter) {
            float score = this.scorer.score(matter);
            if (score < this.finalizeThreshold) {
                return null;
            }
            return new DishRecognitionResult(this.key, this.previewId, this.resultItem, score, this.desirability);
        }

        private DishRecognitionResult descriptor() {
            return new DishRecognitionResult(this.key, this.previewId, this.resultItem, 1.0F, this.desirability);
        }
    }

    private static final Schema SOFT_SCRAMBLED_EGGS = schema(
            "soft_scrambled_eggs",
            1,
            JazzyItems.SOFT_SCRAMBLED_EGGS,
            0.62F,
            0.65F,
            0.90F,
            matter -> average(
                    requiredTraits(matter, FoodTrait.EGG),
                    optionalTraits(matter, FoodTrait.DAIRY, FoodTrait.ALLIUM, FoodTrait.HERB, FoodTrait.SALT, FoodTrait.PEPPER),
                    atMost(matter.stirCount(), 1, 1),
                    band(matter.proteinSet(), 0.45F, 0.65F, 0.16F),
                    band(matter.water(), 0.45F, 0.75F, 0.18F),
                    atMost(matter.browning(), 0.18F, 0.12F),
                    atMost(matter.charLevel(), 0.08F, 0.08F),
                    band(matter.fragmentation(), 0.55F, 0.92F, 0.20F),
                    band(matter.cohesiveness(), 0.10F, 0.60F, 0.24F)
            )
    );

    private static final Schema SCRAMBLED_EGGS = schema(
            "scrambled_eggs",
            2,
            JazzyItems.SCRAMBLED_EGGS,
            0.62F,
            0.65F,
            0.93F,
            matter -> average(
                    requiredTraits(matter, FoodTrait.EGG),
                    optionalTraits(matter, FoodTrait.DAIRY, FoodTrait.ALLIUM, FoodTrait.HERB, FoodTrait.SALT, FoodTrait.PEPPER),
                    atLeast(matter.stirCount(), 1),
                    band(matter.proteinSet(), 0.60F, 0.82F, 0.18F),
                    band(matter.water(), 0.25F, 0.55F, 0.20F),
                    atMost(matter.browning(), 0.30F, 0.14F),
                    atMost(matter.charLevel(), 0.12F, 0.10F),
                    band(matter.fragmentation(), 0.45F, 0.88F, 0.20F),
                    band(matter.cohesiveness(), 0.12F, 0.56F, 0.22F)
            )
    );

    private static final Schema OMELET = schema(
            "omelet",
            3,
            JazzyItems.OMELET,
            0.62F,
            0.65F,
            0.96F,
            matter -> requiredTraits(matter, FoodTrait.EGG)
                    * atLeast(matter.flipCount(), 1)
                    * average(
                    optionalTraits(matter, FoodTrait.DAIRY, FoodTrait.ALLIUM, FoodTrait.HERB, FoodTrait.SALT, FoodTrait.PEPPER),
                    band(matter.proteinSet(), 0.58F, 0.80F, 0.16F),
                    band(matter.water(), 0.30F, 0.60F, 0.18F),
                    band(matter.browning(), 0.02F, 0.22F, 0.10F),
                    atMost(matter.charLevel(), 0.10F, 0.08F),
                    atMost(matter.fragmentation(), 0.35F, 0.18F),
                    band(matter.cohesiveness(), 0.45F, 0.95F, 0.20F)
            )
    );

    private static final Schema BROWNED_OMELET = schema(
            "browned_omelet",
            4,
            JazzyItems.BROWNED_OMELET,
            0.62F,
            0.65F,
            0.87F,
            matter -> requiredTraits(matter, FoodTrait.EGG)
                    * atLeast(matter.flipCount(), 1)
                    * average(
                    optionalTraits(matter, FoodTrait.DAIRY, FoodTrait.ALLIUM, FoodTrait.HERB, FoodTrait.SALT, FoodTrait.PEPPER),
                    band(matter.proteinSet(), 0.58F, 0.84F, 0.18F),
                    band(matter.water(), 0.24F, 0.56F, 0.18F),
                    band(matter.browning(), 0.22F, 0.45F, 0.14F),
                    atMost(matter.charLevel(), 0.16F, 0.10F),
                    atMost(matter.fragmentation(), 0.35F, 0.18F),
                    band(matter.cohesiveness(), 0.45F, 0.95F, 0.20F)
            )
    );

    private static final Schema BURNT_EGGS = schema(
            "burnt_eggs",
            5,
            JazzyItems.BURNT_EGGS,
            0.50F,
            0.55F,
            0.18F,
            matter -> average(
                    requiredTraits(matter, FoodTrait.EGG),
                    maxScore(
                            atLeast(matter.charLevel(), 0.25F, 0.18F),
                            atLeast(matter.browning(), 0.65F, 0.24F)
                    ),
                    atLeast(matter.proteinSet(), 0.35F, 0.30F)
            )
    );

    private static final Schema PIE_DOUGH = schema(
            "pie_dough",
            6,
            JazzyItems.PIE_DOUGH,
            0.60F,
            0.62F,
            0.84F,
            matter -> average(
                    stateFit(matter, IngredientState.DOUGH),
                    requiredTraits(matter, FoodTrait.FLOUR),
                    anyTrait(matter, FoodTrait.FAT, FoodTrait.OIL),
                    noTraits(matter, FoodTrait.LEAVENER),
                    band(matter.water(), 0.10F, 0.45F, 0.20F),
                    band(matter.fragmentation(), 0.04F, 0.18F, 0.12F),
                    band(matter.cohesiveness(), 0.50F, 0.85F, 0.18F),
                    atMost(matter.aeration(), 0.18F, 0.12F)
            )
    );

    private static final Schema FOCACCIA_DOUGH = schema(
            "focaccia_dough",
            7,
            JazzyItems.FOCACCIA_DOUGH,
            0.60F,
            0.62F,
            0.86F,
            matter -> average(
                    stateFit(matter, IngredientState.BREAD_DOUGH),
                    requiredTraits(matter, FoodTrait.FLOUR, FoodTrait.LEAVENER),
                    anyTrait(matter, FoodTrait.FAT, FoodTrait.OIL),
                    optionalTraits(matter, FoodTrait.WHEAT, FoodTrait.FERMENTED),
                    band(matter.water(), 0.16F, 0.50F, 0.18F),
                    band(matter.fragmentation(), 0.04F, 0.16F, 0.12F),
                    band(matter.cohesiveness(), 0.55F, 0.88F, 0.18F),
                    band(matter.aeration(), 0.02F, 0.18F, 0.10F)
            )
    );

    private static final Schema GARLIC_BUTTER = schema(
            "garlic_butter",
            8,
            JazzyItems.GARLIC_BUTTER,
            0.60F,
            0.62F,
            0.82F,
            matter -> average(
                    stateFit(matter, IngredientState.SMOOTH_PASTE),
                    requiredTraits(matter, FoodTrait.ALLIUM),
                    anyTrait(matter, FoodTrait.FAT, FoodTrait.OIL, FoodTrait.DAIRY),
                    optionalTraits(matter, FoodTrait.HERB),
                    atMost(matter.water(), 0.30F, 0.18F),
                    atLeast(matter.fat(), 0.45F, 0.20F),
                    band(matter.fragmentation(), 0.08F, 0.34F, 0.16F),
                    band(matter.cohesiveness(), 0.18F, 0.62F, 0.18F)
            )
    );

    private static final Schema CHEESE_SAUCE = schema(
            "cheese_sauce",
            9,
            JazzyItems.CHEESE_SAUCE,
            0.60F,
            0.62F,
            0.86F,
            matter -> average(
                    stateFit(matter, IngredientState.CREAMY),
                    requiredTraits(matter, FoodTrait.DAIRY),
                    anyTrait(matter, FoodTrait.PROTEIN, FoodTrait.FAT),
                    atLeast(matter.cheeseLoad(), 0.10F, 0.10F),
                    band(matter.water(), 0.30F, 0.85F, 0.18F),
                    band(matter.fat(), 0.12F, 0.72F, 0.18F),
                    band(matter.fragmentation(), 0.06F, 0.30F, 0.14F),
                    band(matter.cohesiveness(), 0.28F, 0.66F, 0.16F)
            )
    );

    private static final Schema NUT_BUTTER = schema(
            "nut_butter",
            10,
            JazzyItems.NUT_BUTTER,
            0.60F,
            0.62F,
            0.88F,
            matter -> average(
                    stateFit(matter, IngredientState.PASTE),
                    requiredTraits(matter, FoodTrait.NUT),
                    noTraits(matter, FoodTrait.BREAD, FoodTrait.LEGUME),
                    atMost(matter.water(), 0.35F, 0.20F),
                    atLeast(matter.fat(), 0.22F, 0.16F),
                    band(matter.fragmentation(), 0.20F, 0.45F, 0.14F),
                    band(matter.cohesiveness(), 0.18F, 0.50F, 0.18F)
            )
    );

    private static final Schema PACKED_BREADCRUMBS = schema(
            "packed_breadcrumbs",
            11,
            JazzyItems.PACKED_BREADCRUMBS,
            0.60F,
            0.62F,
            0.74F,
            matter -> average(
                    stateFit(matter, IngredientState.COARSE_POWDER),
                    requiredTraits(matter, FoodTrait.BREAD),
                    atMost(matter.water(), 0.28F, 0.18F),
                    band(matter.fragmentation(), 0.28F, 0.60F, 0.16F),
                    band(matter.cohesiveness(), 0.05F, 0.35F, 0.14F)
            )
    );

    private static final Schema HUMMUS_PREP = schema(
            "hummus_prep",
            12,
            JazzyItems.HUMMUS_PREP,
            0.60F,
            0.62F,
            0.90F,
            matter -> average(
                    stateFit(matter, IngredientState.SMOOTH_PASTE),
                    requiredTraits(matter, FoodTrait.LEGUME),
                    anyTrait(matter, FoodTrait.ACIDIC, FoodTrait.FAT, FoodTrait.OIL),
                    optionalTraits(matter, FoodTrait.ALLIUM),
                    band(matter.water(), 0.20F, 0.70F, 0.20F),
                    atLeast(matter.seasoningLoad(), 0.05F, 0.08F),
                    band(matter.fragmentation(), 0.20F, 0.50F, 0.16F),
                    band(matter.cohesiveness(), 0.18F, 0.56F, 0.18F)
            )
    );

    private static final Schema CHOPPED_PRODUCE_BLEND = schema(
            "chopped_produce_blend",
            13,
            JazzyItems.CHOPPED_PRODUCE_BLEND,
            0.58F,
            0.60F,
            0.72F,
            matter -> average(
                    stateFit(matter, IngredientState.ROUGH_CUT),
                    anyTrait(matter, FoodTrait.FRUIT, FoodTrait.VEGETABLE),
                    noTraits(matter, FoodTrait.PROTEIN, FoodTrait.DAIRY, FoodTrait.BREAD),
                    band(matter.water(), 0.18F, 0.90F, 0.18F),
                    band(matter.fragmentation(), 0.20F, 0.55F, 0.16F),
                    band(matter.cohesiveness(), 0.16F, 0.50F, 0.16F)
            )
    );

    private static final Schema LEMON_JUICE = schema(
            "lemon_juice",
            14,
            JazzyItems.LEMON_JUICE,
            0.60F,
            0.62F,
            0.82F,
            matter -> average(
                    stateFit(matter, IngredientState.FRESH_JUICE),
                    requiredTraits(matter, FoodTrait.FRUIT, FoodTrait.ACIDIC),
                    noTraits(matter, FoodTrait.DAIRY, FoodTrait.PROTEIN, FoodTrait.BREAD),
                    band(matter.water(), 0.40F, 1.0F, 0.18F),
                    atMost(matter.aeration(), 0.10F, 0.08F),
                    band(matter.fragmentation(), 0.02F, 0.18F, 0.10F),
                    band(matter.cohesiveness(), 0.05F, 0.20F, 0.10F)
            )
    );

    private static final Schema MIXED_JUICE = schema(
            "mixed_juice",
            15,
            JazzyItems.MIXED_JUICE,
            0.58F,
            0.60F,
            0.78F,
            matter -> average(
                    stateFit(matter, IngredientState.FRESH_JUICE),
                    anyTrait(matter, FoodTrait.FRUIT, FoodTrait.VEGETABLE),
                    noTraits(matter, FoodTrait.DAIRY, FoodTrait.PROTEIN, FoodTrait.BREAD, FoodTrait.ACIDIC),
                    band(matter.water(), 0.40F, 1.0F, 0.18F),
                    atMost(matter.aeration(), 0.10F, 0.08F),
                    band(matter.fragmentation(), 0.02F, 0.18F, 0.10F),
                    band(matter.cohesiveness(), 0.05F, 0.24F, 0.12F)
            )
    );

    private static final Schema FRUIT_JUICE_BLEND = schema(
            "fruit_juice_blend",
            16,
            JazzyItems.FRUIT_JUICE_BLEND,
            0.58F,
            0.60F,
            0.80F,
            matter -> average(
                    stateFit(matter, IngredientState.SMOOTH),
                    anyTrait(matter, FoodTrait.FRUIT, FoodTrait.VEGETABLE),
                    noTraits(matter, FoodTrait.DAIRY),
                    band(matter.water(), 0.30F, 0.95F, 0.18F),
                    band(matter.aeration(), 0.12F, 0.36F, 0.12F),
                    band(matter.fragmentation(), 0.08F, 0.24F, 0.12F),
                    band(matter.cohesiveness(), 0.35F, 0.56F, 0.12F)
            )
    );

    private static final Schema SMOOTHIE_BLEND = schema(
            "smoothie_blend",
            17,
            JazzyItems.SMOOTHIE_BLEND,
            0.58F,
            0.60F,
            0.86F,
            matter -> average(
                    stateFit(matter, IngredientState.SMOOTH),
                    anyTrait(matter, FoodTrait.FRUIT, FoodTrait.VEGETABLE),
                    requiredTraits(matter, FoodTrait.DAIRY),
                    band(matter.water(), 0.30F, 0.95F, 0.18F),
                    band(matter.aeration(), 0.16F, 0.40F, 0.12F),
                    band(matter.fragmentation(), 0.08F, 0.22F, 0.12F),
                    band(matter.cohesiveness(), 0.38F, 0.60F, 0.12F)
            )
    );

    private static final Schema PACKED_FREEZE_DRY_APPLES = schema(
            "packed_freeze_dry_apples",
            18,
            JazzyItems.PACKED_FREEZE_DRY_APPLES,
            0.58F,
            0.60F,
            0.78F,
            matter -> average(
                    stateFit(matter, IngredientState.FREEZE_DRIED),
                    requiredTraits(matter, FoodTrait.FRUIT),
                    noTraits(matter, FoodTrait.PROTEIN),
                    band(matter.water(), 0.0F, 0.18F, 0.08F),
                    band(matter.fragmentation(), 0.20F, 0.50F, 0.14F),
                    band(matter.cohesiveness(), 0.14F, 0.40F, 0.12F)
            )
    );

    private static final Schema FREEZE_DRIED_MEAL = schema(
            "freeze_dried_meal",
            19,
            JazzyItems.FREEZE_DRIED_MEAL,
            0.58F,
            0.60F,
            0.74F,
            matter -> average(
                    stateFit(matter, IngredientState.FREEZE_DRIED),
                    nourishmentAtLeast(matter, 5, 2),
                    band(matter.water(), 0.0F, 0.18F, 0.08F),
                    band(matter.fragmentation(), 0.18F, 0.45F, 0.16F),
                    band(matter.cohesiveness(), 0.18F, 0.40F, 0.12F)
            )
    );

    private static final List<Schema> ALL_SCHEMAS = List.of(
            BURNT_EGGS,
            SOFT_SCRAMBLED_EGGS,
            SCRAMBLED_EGGS,
            OMELET,
            BROWNED_OMELET,
            PIE_DOUGH,
            FOCACCIA_DOUGH,
            GARLIC_BUTTER,
            CHEESE_SAUCE,
            NUT_BUTTER,
            PACKED_BREADCRUMBS,
            HUMMUS_PREP,
            CHOPPED_PRODUCE_BLEND,
            LEMON_JUICE,
            MIXED_JUICE,
            FRUIT_JUICE_BLEND,
            SMOOTHIE_BLEND,
            PACKED_FREEZE_DRY_APPLES,
            FREEZE_DRIED_MEAL
    );
    private static final Map<Integer, Schema> BY_PREVIEW_ID = indexByPreviewId();

    private DishSchema() {
    }

    public static DishRecognitionResult preview(FoodMatterData matter) {
        if (matter == null) {
            return null;
        }
        DishRecognitionResult best = null;
        for (Schema schema : ALL_SCHEMAS) {
            DishRecognitionResult result = schema.preview(matter);
            if (isBetterMatch(best, result)) {
                best = result;
            }
        }
        return best;
    }

    public static DishRecognitionResult finalizeResult(FoodMatterData matter) {
        if (matter == null) {
            return SCRAMBLED_EGGS.descriptor();
        }
        DishRecognitionResult best = null;
        for (Schema schema : ALL_SCHEMAS) {
            DishRecognitionResult result = schema.finalizeMatch(matter);
            if (isBetterMatch(best, result)) {
                best = result;
            }
        }
        return best != null ? best : SCRAMBLED_EGGS.descriptor();
    }

    public static DishRecognitionResult descriptor(int previewId) {
        Schema schema = BY_PREVIEW_ID.get(previewId);
        return schema != null ? schema.descriptor() : null;
    }

    private static Schema schema(
            String key,
            int previewId,
            Supplier<? extends Item> resultItem,
            float previewThreshold,
            float finalizeThreshold,
            float desirability,
            RecognitionScorer scorer
    ) {
        return new Schema(key, previewId, resultItem, previewThreshold, finalizeThreshold, desirability, scorer);
    }

    private static boolean isBetterMatch(DishRecognitionResult currentBest, DishRecognitionResult candidate) {
        if (candidate == null) {
            return false;
        }
        if (currentBest == null) {
            return true;
        }
        if (candidate.score() > currentBest.score()) {
            return true;
        }
        if (candidate.score() == currentBest.score() && candidate.desirability() > currentBest.desirability()) {
            return true;
        }
        return false;
    }

    private static Map<Integer, Schema> indexByPreviewId() {
        Map<Integer, Schema> byPreviewId = new LinkedHashMap<>();
        for (Schema schema : ALL_SCHEMAS) {
            byPreviewId.put(schema.previewId(), schema);
        }
        return byPreviewId;
    }

    private static float requiredTraits(FoodMatterData matter, FoodTrait... traits) {
        for (FoodTrait trait : traits) {
            if (!matter.hasTrait(trait)) {
                return 0.0F;
            }
        }
        return 1.0F;
    }

    private static float optionalTraits(FoodMatterData matter, FoodTrait... traits) {
        if (traits.length == 0) {
            return 1.0F;
        }
        int matched = 0;
        for (FoodTrait trait : traits) {
            if (matter.hasTrait(trait)) {
                matched++;
            }
        }
        return matched / (float) traits.length;
    }

    private static float anyTrait(FoodMatterData matter, FoodTrait... traits) {
        for (FoodTrait trait : traits) {
            if (matter.hasTrait(trait)) {
                return 1.0F;
            }
        }
        return 0.0F;
    }

    private static float noTraits(FoodMatterData matter, FoodTrait... traits) {
        for (FoodTrait trait : traits) {
            if (matter.hasTrait(trait)) {
                return 0.0F;
            }
        }
        return 1.0F;
    }

    private static float stateFit(FoodMatterData matter, IngredientState... states) {
        if (matter.summaryHint() == null || states.length == 0) {
            return 1.0F;
        }
        IngredientState currentState = matter.summaryHint().state();
        for (IngredientState state : states) {
            if (currentState == state) {
                return 1.0F;
            }
        }
        return 0.0F;
    }

    private static float nourishmentAtLeast(FoodMatterData matter, int minimum, int softness) {
        int nourishment = matter.summaryHint() != null ? matter.summaryHint().nourishment() : 0;
        if (nourishment >= minimum) {
            return 1.0F;
        }
        if (softness <= 0) {
            return 0.0F;
        }
        return Mth.clamp(1.0F - ((minimum - nourishment) / (float) softness), 0.0F, 1.0F);
    }

    private static float atLeast(int value, int minimum) {
        return value >= minimum ? 1.0F : 0.0F;
    }

    private static float atMost(int value, int maximum, int softness) {
        if (value <= maximum) {
            return 1.0F;
        }
        if (softness <= 0) {
            return 0.0F;
        }
        return Mth.clamp(1.0F - ((value - maximum) / (float) softness), 0.0F, 1.0F);
    }

    private static float atLeast(float value, float minimum, float softness) {
        if (value >= minimum) {
            return 1.0F;
        }
        if (softness <= 0.0F) {
            return 0.0F;
        }
        return Mth.clamp(1.0F - ((minimum - value) / softness), 0.0F, 1.0F);
    }

    private static float atMost(float value, float maximum, float softness) {
        if (value <= maximum) {
            return 1.0F;
        }
        if (softness <= 0.0F) {
            return 0.0F;
        }
        return Mth.clamp(1.0F - ((value - maximum) / softness), 0.0F, 1.0F);
    }

    private static float band(float value, float minimum, float maximum, float softness) {
        if (value < minimum) {
            return atLeast(value, minimum, softness);
        }
        if (value > maximum) {
            return atMost(value, maximum, softness);
        }
        return 1.0F;
    }

    private static float average(float... values) {
        if (values.length == 0) {
            return 0.0F;
        }
        float total = 0.0F;
        for (float value : values) {
            total += Mth.clamp(value, 0.0F, 1.0F);
        }
        return total / values.length;
    }

    private static float maxScore(float... values) {
        float best = 0.0F;
        for (float value : values) {
            best = Math.max(best, Mth.clamp(value, 0.0F, 1.0F));
        }
        return best;
    }
}
