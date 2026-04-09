package com.boaat.jazzy_cookin.kitchen.sim.recognition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.item.KitchenMealItem;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfile;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfiles;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;
import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class DishSchema {
    private enum CatalogFamily {
        JUICE,
        BLEND,
        DOUGH,
        SAUCE,
        SOUP,
        FRIED,
        BAKED,
        GRAIN,
        PLATED,
        GENERIC
    }

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

    private static final List<Schema> MANUAL_SCHEMAS = List.of(
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
    private static final List<Schema> EGG_DISH_SCHEMAS = List.of(BURNT_EGGS, SOFT_SCRAMBLED_EGGS, SCRAMBLED_EGGS, OMELET, BROWNED_OMELET);
    private static List<Schema> allSchemas;
    private static Map<Integer, Schema> byPreviewId;

    private DishSchema() {
    }

    public static DishRecognitionResult preview(FoodMatterData matter) {
        DishRecognitionResult eggDish = previewEggDish(matter);
        if (eggDish != null) {
            return eggDish;
        }
        if (matter != null && (matter.finalizedServing() || isFinishedServingState(matter))) {
            DishRecognitionResult meal = previewMeal(matter);
            if (meal != null) {
                return meal;
            }
        }
        return preview(matter, item -> true);
    }

    public static DishRecognitionResult previewPrepared(FoodMatterData matter) {
        return preview(matter, DishSchema::isPreparedFood);
    }

    public static DishRecognitionResult previewMeal(FoodMatterData matter) {
        return preview(matter, DishSchema::isMeal);
    }

    public static DishRecognitionResult preview(FoodMatterData matter, Predicate<Item> filter) {
        if (matter == null) {
            return null;
        }
        ensureSchemas();
        DishRecognitionResult best = null;
        for (Schema schema : allSchemas) {
            if (!filter.test(schema.resultItem().get())) {
                continue;
            }
            DishRecognitionResult result = schema.preview(matter);
            if (isBetterMatch(best, result)) {
                best = result;
            }
        }
        return best;
    }

    public static DishRecognitionResult finalizeResult(FoodMatterData matter) {
        DishRecognitionResult eggDish = finalizeEggDish(matter);
        if (eggDish != null) {
            return eggDish;
        }
        if (matter != null && (matter.finalizedServing() || isFinishedServingState(matter))) {
            DishRecognitionResult meal = finalizeMeal(matter);
            if (meal != null) {
                return meal;
            }
        }
        return finalizeResult(matter, item -> true);
    }

    public static DishRecognitionResult finalizePrepared(FoodMatterData matter) {
        return finalizeResult(matter, DishSchema::isPreparedFood);
    }

    public static DishRecognitionResult finalizeMeal(FoodMatterData matter) {
        return finalizeResult(matter, DishSchema::isMeal);
    }

    public static DishRecognitionResult finalizeResult(FoodMatterData matter, Predicate<Item> filter) {
        if (matter == null) {
            return null;
        }
        ensureSchemas();
        DishRecognitionResult best = null;
        for (Schema schema : allSchemas) {
            if (!filter.test(schema.resultItem().get())) {
                continue;
            }
            DishRecognitionResult result = schema.finalizeMatch(matter);
            if (isBetterMatch(best, result)) {
                best = result;
            }
        }
        if (best != null) {
            return best;
        }
        return preview(matter, filter);
    }

    public static DishRecognitionResult descriptor(int previewId) {
        ensureSchemas();
        Schema schema = byPreviewId.get(previewId);
        return schema != null ? schema.descriptor() : null;
    }

    public static boolean hasRecognizerFor(Item item) {
        ensureSchemas();
        for (Schema schema : allSchemas) {
            if (schema.resultItem().get() == item) {
                return true;
            }
        }
        return false;
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

    private static synchronized void ensureSchemas() {
        if (allSchemas != null && byPreviewId != null) {
            return;
        }

        List<Schema> generated = buildCatalogSchemas();
        List<Schema> combined = new ArrayList<>(MANUAL_SCHEMAS);
        combined.addAll(generated);
        allSchemas = List.copyOf(combined);
        byPreviewId = indexByPreviewId(allSchemas);
    }

    private static List<Schema> buildCatalogSchemas() {
        Set<Item> manualItems = new HashSet<>();
        for (Schema schema : MANUAL_SCHEMAS) {
            manualItems.add(schema.resultItem().get());
        }

        List<Schema> generated = new ArrayList<>();
        int previewId = 100;
        for (var ingredient : JazzyItems.ingredientItems()) {
            Item item = ingredient.get();
            if (manualItems.add(item)) {
                generated.add(generatedSchema(item, previewId++, false));
            }
        }
        for (var prepared : JazzyItems.preparedItems()) {
            Item item = prepared.get();
            if (manualItems.add(item)) {
                generated.add(generatedSchema(item, previewId++, false));
            }
        }
        for (var meal : JazzyItems.mealItems()) {
            Item item = meal.get();
            if (manualItems.add(item)) {
                generated.add(generatedSchema(item, previewId++, true));
            }
        }
        return generated;
    }

    private static Schema generatedSchema(Item item, int previewId, boolean meal) {
        FoodMaterialProfile profile = FoodMaterialProfiles.profileFor(new ItemStack(item)).orElseThrow(
                () -> new IllegalStateException("Missing generated recognizer profile for " + BuiltInRegistries.ITEM.getKey(item))
        );
        IngredientState expectedState = expectedState(item);
        float previewThreshold = meal ? 0.54F : 0.50F;
        float finalizeThreshold = meal ? 0.58F : 0.54F;
        float desirability = meal ? 0.84F : 0.72F;
        return schema(
                BuiltInRegistries.ITEM.getKey(item).getPath(),
                previewId,
                () -> item,
                previewThreshold,
                finalizeThreshold,
                desirability,
                matter -> generatedScore(matter, profile, expectedState, meal, item)
        );
    }

    private static float generatedScore(
            FoodMatterData matter,
            FoodMaterialProfile profile,
            IngredientState expectedState,
            boolean meal,
            Item item
    ) {
        CatalogFamily family = generatedFamily(item, expectedState, meal);
        float stateScore = generatedStateFit(matter, expectedState, item);
        float traitCoverage = generatedTraitCoverage(matter.traitMask(), profile.traitMask());
        float compositionScore = average(
                closeness(matter.water(), profile.water()),
                closeness(matter.fat(), profile.fat()),
                closeness(matter.protein(), profile.protein()),
                closeness(matter.seasoningLoad(), profile.seasoningLoad()),
                closeness(matter.cheeseLoad(), profile.cheeseLoad()),
                closeness(matter.onionLoad(), profile.onionLoad()),
                closeness(matter.herbLoad(), profile.herbLoad()),
                closeness(matter.pepperLoad(), profile.pepperLoad())
        );
        float familyFit = generatedFamilyFit(matter, family);
        float processScore = meal
                ? average(matter.finalizedServing() ? 1.0F : 0.48F, Mth.clamp(matter.processDepth() / 4.0F, 0.0F, 1.0F))
                : average(!matter.finalizedServing() ? 1.0F : 0.62F, Mth.clamp(matter.processDepth() / 3.0F, 0.0F, 1.0F));
        float conditionScore = average(
                1.0F - matter.charLevel(),
                1.0F - Mth.clamp(matter.microbialLoad(), 0.0F, 1.0F),
                Mth.clamp(0.45F + matter.preservationLevel() * 0.30F, 0.0F, 1.0F)
        );
        float score = average(stateScore, traitCoverage, compositionScore, familyFit, processScore, conditionScore);
        if (!meal && expectedState.isPlatedState() != currentState(matter).isPlatedState()) {
            score *= 0.35F;
        }
        if (item instanceof KitchenIngredientItem && !(item instanceof KitchenMealItem) && isCookedMatter(matter) && isPantryLikeState(expectedState)) {
            score *= 0.12F;
        }
        if (item instanceof KitchenIngredientItem && !(item instanceof KitchenMealItem) && matter.finalizedServing()) {
            score *= 0.30F;
        }
        if (item instanceof KitchenMealItem && !matter.finalizedServing() && !isFinishedServingState(matter)) {
            score *= 0.88F;
        }
        return Mth.clamp(score, 0.0F, 1.0F);
    }

    private static CatalogFamily generatedFamily(Item item, IngredientState expectedState, boolean meal) {
        String id = BuiltInRegistries.ITEM.getKey(item).getPath();
        if (id.contains("juice") || expectedState == IngredientState.FRESH_JUICE) {
            return CatalogFamily.JUICE;
        }
        if (id.contains("smoothie") || expectedState == IngredientState.SMOOTH || expectedState == IngredientState.CREAMY) {
            return CatalogFamily.BLEND;
        }
        if (id.contains("dough") || id.contains("batter") || expectedState == IngredientState.DOUGH || expectedState == IngredientState.BREAD_DOUGH) {
            return CatalogFamily.DOUGH;
        }
        if (id.contains("sauce") || id.contains("butter") || id.contains("hummus") || expectedState == IngredientState.SMOOTH_PASTE
                || expectedState == IngredientState.PASTE || expectedState == IngredientState.CREAMY) {
            return CatalogFamily.SAUCE;
        }
        if (id.contains("soup") || id.contains("stew") || id.contains("curry") || id.contains("masala")
                || id.contains("tadka") || id.contains("sabzi") || id.contains("shakshuka")
                || expectedState == IngredientState.SIMMERED || expectedState == IngredientState.SIMMERED_FILLING) {
            return CatalogFamily.SOUP;
        }
        if (id.contains("fried") || id.contains("skillet") || expectedState == IngredientState.PAN_FRIED || expectedState == IngredientState.DEEP_FRIED) {
            return CatalogFamily.FRIED;
        }
        if (id.contains("cake") || id.contains("brownie") || id.contains("pie") || id.contains("bread")
                || id.contains("pizza") || id.contains("gratin")
                || expectedState == IngredientState.BAKED || expectedState == IngredientState.BAKED_BREAD || expectedState == IngredientState.BAKED_PIE) {
            return CatalogFamily.BAKED;
        }
        if (id.contains("rice") || id.contains("pasta") || id.contains("noodle") || id.contains("spaghetti")
                || id.contains("ramen") || id.contains("couscous") || id.contains("biryani")
                || id.contains("pulao") || id.contains("chawal") || id.contains("pomodoro")) {
            return CatalogFamily.GRAIN;
        }
        if (meal || expectedState.isPlatedState()) {
            return CatalogFamily.PLATED;
        }
        return CatalogFamily.GENERIC;
    }

    private static float generatedFamilyFit(FoodMatterData matter, CatalogFamily family) {
        return switch (family) {
            case JUICE -> average(
                    band(matter.water(), 0.76F, 1.0F, 0.18F),
                    atMost(matter.proteinSet(), 0.10F, 0.10F),
                    atMost(matter.fragmentation(), 0.18F, 0.12F),
                    band(matter.cohesiveness(), 0.05F, 0.22F, 0.12F)
            );
            case BLEND -> average(
                    band(matter.water(), 0.46F, 0.86F, 0.18F),
                    band(matter.fragmentation(), 0.08F, 0.26F, 0.14F),
                    band(matter.cohesiveness(), 0.30F, 0.62F, 0.16F)
            );
            case DOUGH -> average(
                    band(matter.water(), 0.18F, 0.46F, 0.16F),
                    atMost(matter.proteinSet(), 0.22F, 0.12F),
                    atMost(matter.fragmentation(), 0.20F, 0.12F),
                    band(matter.cohesiveness(), 0.54F, 0.88F, 0.18F)
            );
            case SAUCE -> average(
                    band(matter.water(), 0.20F, 0.68F, 0.22F),
                    atMost(matter.fragmentation(), 0.30F, 0.16F),
                    band(matter.cohesiveness(), 0.24F, 0.58F, 0.18F)
            );
            case SOUP -> average(
                    band(matter.water(), 0.48F, 0.90F, 0.18F),
                    atMost(matter.browning(), 0.26F, 0.16F),
                    band(matter.cohesiveness(), 0.12F, 0.42F, 0.18F)
            );
            case FRIED -> average(
                    band(matter.water(), 0.18F, 0.54F, 0.18F),
                    band(matter.proteinSet(), 0.46F, 0.84F, 0.18F),
                    band(matter.browning(), 0.08F, 0.34F, 0.14F)
            );
            case BAKED -> average(
                    band(matter.water(), 0.12F, 0.48F, 0.18F),
                    band(matter.cohesiveness(), 0.36F, 0.84F, 0.18F),
                    band(matter.browning(), 0.08F, 0.30F, 0.14F)
            );
            case GRAIN -> average(
                    band(matter.water(), 0.28F, 0.60F, 0.18F),
                    band(matter.fragmentation(), 0.16F, 0.46F, 0.16F),
                    band(matter.cohesiveness(), 0.24F, 0.56F, 0.18F)
            );
            case PLATED -> average(
                    matter.finalizedServing() ? 1.0F : 0.56F,
                    band(matter.cohesiveness(), 0.22F, 0.62F, 0.20F)
            );
            case GENERIC -> average(
                    band(matter.water(), 0.20F, 0.70F, 0.24F),
                    band(matter.cohesiveness(), 0.18F, 0.62F, 0.22F)
            );
        };
    }

    private static float generatedStateFit(FoodMatterData matter, IngredientState expectedState, Item item) {
        IngredientState current = currentState(matter);
        if (current == expectedState) {
            return 1.0F;
        }
        if (expectedState.isPlatedState()) {
            return current.isPlatedState() || matter.finalizedServing() ? 1.0F : 0.28F;
        }
        if (current.isPlatedState() && !expectedState.isPlatedState()) {
            return 0.18F;
        }
        if (item instanceof KitchenMealItem) {
            return matter.finalizedServing() ? 0.82F : 0.32F;
        }
        if (item instanceof KitchenIngredientItem && isPrepLikeState(current) && isPantryLikeState(expectedState)) {
            return 0.82F;
        }
        if (sameStateFamily(current, expectedState)) {
            return 0.76F;
        }
        return 0.34F;
    }

    private static boolean sameStateFamily(IngredientState current, IngredientState expectedState) {
        if ((isSimmeredFamily(current) && isSimmeredFamily(expectedState))
                || (isBakedFamily(current) && isBakedFamily(expectedState))
                || (isFriedFamily(current) && isFriedFamily(expectedState))
                || (isPowderFamily(current) && isPowderFamily(expectedState))
                || (isPrepLikeState(current) && isPrepLikeState(expectedState))) {
            return true;
        }
        return false;
    }

    private static boolean isPantryLikeState(IngredientState state) {
        return state == IngredientState.PANTRY_READY || state.name().equals(state.getSerializedName().toUpperCase()) || state.ordinal() >= IngredientState.WHITE_SUGAR.ordinal();
    }

    private static boolean isPrepLikeState(IngredientState state) {
        return switch (state) {
            case RAW, WASHED, WHOLE, PEELED, SLICED, CHOPPED, DICED, MINCED, ROUGH_CUT, STRAINED, CRUSHED,
                    STUFFED, SHAPED_BASE, SLICED_APPLE, CHOPPED_TOMATO, CHOPPED_HERB, CHOPPED_CABBAGE,
                    DICED_ONION, CLEANED_FISH, SLICED_BREAD, GROUND_SPICE, GROUND_HERB -> true;
            default -> false;
        };
    }

    private static boolean isSimmeredFamily(IngredientState state) {
        return switch (state) {
            case BOILED, SIMMERED, SOUP_BASE, STRAINED_SOUP, SIMMERED_FILLING, GLAZED, MARINATED, MARINATED_PROTEIN -> true;
            default -> false;
        };
    }

    private static boolean isBakedFamily(IngredientState state) {
        return switch (state) {
            case BAKED, ROASTED, BAKED_BREAD, BAKED_PIE, COOLED_PIE, RESTED_PIE, RAW_ASSEMBLED_PIE, RAW_ASSEMBLED_PIZZA, BREAD_DOUGH, DOUGH -> true;
            default -> false;
        };
    }

    private static boolean isFriedFamily(IngredientState state) {
        return switch (state) {
            case PAN_FRIED, DEEP_FRIED, FRIED_PROTEIN, PANTRY_READY, GLAZED -> true;
            default -> false;
        };
    }

    private static boolean isPowderFamily(IngredientState state) {
        return switch (state) {
            case COARSE_POWDER, FINE_POWDER, GROUND_SPICE, GROUND_HERB -> true;
            default -> false;
        };
    }

    private static float generatedTraitCoverage(long matterTraits, long expectedTraits) {
        if (expectedTraits == 0L) {
            return 0.45F;
        }
        int expectedCount = 0;
        int matched = 0;
        for (FoodTrait trait : FoodTrait.unpack(expectedTraits)) {
            expectedCount++;
            if (FoodTrait.has(matterTraits, trait)) {
                matched++;
            }
        }
        return expectedCount > 0 ? matched / (float) expectedCount : 0.0F;
    }

    private static float closeness(float actual, float expected) {
        return Mth.clamp(1.0F - Math.abs(actual - expected), 0.0F, 1.0F);
    }

    private static IngredientState expectedState(Item item) {
        if (item instanceof KitchenIngredientItem ingredientItem) {
            return ingredientItem.defaultState();
        }
        return IngredientState.PANTRY_READY;
    }

    private static boolean isPreparedFood(Item item) {
        return item instanceof KitchenIngredientItem && !(item instanceof KitchenMealItem);
    }

    private static boolean isMeal(Item item) {
        return item instanceof KitchenMealItem;
    }

    private static DishRecognitionResult previewEggDish(FoodMatterData matter) {
        if (!isEggDishMatter(matter)) {
            return null;
        }
        return bestMatch(EGG_DISH_SCHEMAS, matter, false, item -> true);
    }

    private static DishRecognitionResult finalizeEggDish(FoodMatterData matter) {
        if (!isEggDishMatter(matter)) {
            return null;
        }
        return bestMatch(EGG_DISH_SCHEMAS, matter, true, item -> true);
    }

    private static boolean isFinishedServingState(FoodMatterData matter) {
        IngredientState state = currentState(matter);
        return state.isPlatedState() || state == IngredientState.PAN_FRIED;
    }

    private static boolean isCookedMatter(FoodMatterData matter) {
        return matter.finalizedServing()
                || matter.proteinSet() >= 0.28F
                || matter.browning() >= 0.08F
                || matter.charLevel() > 0.0F
                || matter.timeInPan() > 0
                || matter.processDepth() > 1
                || !isPantryLikeState(currentState(matter));
    }

    private static boolean isEggDishMatter(FoodMatterData matter) {
        return matter != null
                && matter.hasTrait(FoodTrait.EGG)
                && (matter.timeInPan() > 0 || matter.proteinSet() >= 0.25F || matter.finalizedServing());
    }

    private static DishRecognitionResult bestMatch(List<Schema> schemas, FoodMatterData matter, boolean finalize, Predicate<Item> filter) {
        DishRecognitionResult best = null;
        for (Schema schema : schemas) {
            if (!filter.test(schema.resultItem().get())) {
                continue;
            }
            DishRecognitionResult result = finalize ? schema.finalizeMatch(matter) : schema.preview(matter);
            if (isBetterMatch(best, result)) {
                best = result;
            }
        }
        return best;
    }

    private static Map<Integer, Schema> indexByPreviewId(List<Schema> schemas) {
        Map<Integer, Schema> byPreviewId = new LinkedHashMap<>();
        for (Schema schema : schemas) {
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
        if (states.length == 0) {
            return 1.0F;
        }
        IngredientState currentState = currentState(matter);
        for (IngredientState state : states) {
            if (currentState == state) {
                return 1.0F;
            }
        }
        return 0.0F;
    }

    private static float nourishmentAtLeast(FoodMatterData matter, int minimum, int softness) {
        int nourishment = derivedNourishment(matter);
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

    private static IngredientState currentState(FoodMatterData matter) {
        return KitchenStackUtil.inferStateFromMatter(matter);
    }

    private static int derivedNourishment(FoodMatterData matter) {
        return Math.max(0, Math.round(
                matter.protein() * 8.0F
                        + matter.fat() * 4.0F
                        + matter.water() * 2.0F
                        + matter.seasoningLoad() * 2.0F
                        + matter.cheeseLoad() * 2.0F
        ));
    }
}
