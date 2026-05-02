package com.boaat.jazzy_cookin.kitchen;

import java.util.List;

import com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfile;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfiles;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishRecognitionResult;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishSchema;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaScore;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaScorer;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class DishEvaluation {
    private enum DishFamily {
        EGG,
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

    private record AggregateStats(
            long createdTick,
            float quality,
            float recipeAccuracy,
            float flavor,
            float texture,
            float structure,
            float moisture,
            float purity,
            float aeration,
            float freshness,
            int nourishment,
            int enjoyment,
            int maxDepth
    ) {
    }

    private record FoodMatterGradeScores(
            float finalScore,
            float ingredientQuality,
            float recipeAccuracy,
            float seasoning,
            float cooking,
            float texture,
            float freshness,
            float presentation,
            int nourishment,
            int enjoyment
    ) {
    }

    private DishEvaluation() {
    }

    public static QualityBreakdown evaluateStack(ItemStack stack, Level level) {
        IngredientStateData data = KitchenStackUtil.getOrCreateData(stack, level.getGameTime());
        if (data == null) {
            return new QualityBreakdown(DishGrade.FAILED, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0, 0);
        }

        float freshness = KitchenStackUtil.currentFreshnessScore(stack, level);
        IngredientState effectiveState = KitchenStackUtil.effectiveState(stack, level.getGameTime());
        FoodMatterData foodMatter = KitchenStackUtil.getOrCreateFoodMatter(stack, level.getGameTime());
        if (foodMatter == null) {
            return evaluateSummaryStack(data, stack, level, freshness, effectiveState);
        }

        FoodMatterGradeScores scores = gradeFoodMatter(data, stack, level, freshness, effectiveState, foodMatter);

        return new QualityBreakdown(
                DishGrade.fromScore(scores.finalScore()),
                scores.finalScore(),
                scores.ingredientQuality(),
                scores.freshness(),
                scores.recipeAccuracy(),
                scores.seasoning(),
                scores.cooking(),
                scores.texture(),
                scores.presentation(),
                scores.recipeAccuracy(),
                scores.nourishment(),
                scores.enjoyment()
        );
    }

    private static FoodMatterGradeScores gradeFoodMatter(
            IngredientStateData data,
            ItemStack stack,
            Level level,
            float freshness,
            IngredientState effectiveState,
            FoodMatterData foodMatter
    ) {
        DishRecognitionResult recognition = DishSchema.preview(stack, level.getGameTime());
        DishFamily family = dishFamily(stack, effectiveState, recognition, foodMatter);
        FoodMaterialProfile expectedProfile = FoodMaterialProfiles.profileFor(stack).orElse(null);
        DishSchemaScore schemaScore = DishSchemaScorer.bestScore(stack, foodMatter, item -> item == stack.getItem(), level.getGameTime());
        if (schemaScore == null) {
            schemaScore = DishSchemaScorer.bestScore(stack, foodMatter, item -> true, level.getGameTime());
        }

        float recognitionScore = recognition != null ? recognition.score() : 0.38F;
        float recognitionQuality = recognition != null ? recognition.score() * recognition.desirability() : 0.32F;
        if (schemaScore != null) {
            recognitionScore = Math.max(recognitionScore, schemaScore.score());
            recognitionQuality = Math.max(recognitionQuality, schemaScore.score() * schemaScore.schema().desirability());
        }

        float compositionFit = compositionFit(foodMatter, expectedProfile, family);
        float integrity = foodSafetyIntegrity(foodMatter);
        float structureFit = structureFit(foodMatter, effectiveState, recognition, family);
        float seasoningBalance = seasoningBalance(foodMatter, family);
        float moistureFit = moistureFit(foodMatter, effectiveState, recognition, family);
        float donenessFit = donenessFit(foodMatter, effectiveState, recognition, family);
        float browningFit = browningFit(foodMatter, effectiveState, recognition, family);
        float processFit = Mth.clamp(foodMatter.processDepth() / 4.0F, 0.0F, 1.0F);

        float schemaRecipeFit = schemaScore != null
                ? average(schemaScore.roleScore(), schemaScore.compositionScore(), schemaScore.techniqueScore(), schemaScore.score())
                : recognitionScore;
        float recipeAccuracy = Mth.clamp(
                schemaRecipeFit * 0.50F
                        + recognitionScore * 0.18F
                        + compositionFit * 0.14F
                        + processFit * 0.10F
                        + data.recipeAccuracy() * 0.08F,
                0.0F,
                1.0F
        );
        float seasoning = Mth.clamp(
                seasoningBalance * 0.68F
                        + (schemaScore != null ? schemaScore.seasoningScore() : seasoningBalance) * 0.24F
                        + compositionFit * 0.08F,
                0.0F,
                1.0F
        );
        float cooking = Mth.clamp(
                donenessFit * 0.30F
                        + browningFit * 0.22F
                        + moistureFit * 0.14F
                        + (1.0F - foodMatter.charLevel()) * 0.14F
                        + integrity * 0.12F
                        + (schemaScore != null ? schemaScore.cookingScore() : donenessFit) * 0.08F,
                0.0F,
                1.0F
        );
        float texture = Mth.clamp(
                structureFit * 0.36F
                        + moistureFit * 0.20F
                        + around(foodMatter.aeration(), targetAeration(family), 0.26F) * 0.12F
                        + (schemaScore != null ? schemaScore.textureScore() : structureFit) * 0.20F
                        + integrity * 0.12F,
                0.0F,
                1.0F
        );
        float presentation = Mth.clamp(
                servingFit(foodMatter, effectiveState) * 0.46F
                        + (schemaScore != null ? schemaScore.presentationScore() : foodMatter.finalizedServing() ? 1.0F : 0.42F) * 0.24F
                        + recognitionQuality * 0.16F
                        + processFit * 0.08F
                        + integrity * 0.06F,
                0.0F,
                1.0F
        );
        float freshnessScore = Mth.clamp(freshness * 0.70F + integrity * 0.30F, 0.0F, 1.0F);
        float ingredientQuality = Mth.clamp(
                compositionFit * 0.28F
                        + recognitionQuality * 0.22F
                        + integrity * 0.22F
                        + data.quality() * 0.14F
                        + average(seasoning, texture) * 0.14F,
                0.0F,
                1.0F
        );

        FreshnessBand band = KitchenStackUtil.freshnessBand(stack, level);
        if (band == FreshnessBand.SPOILED) {
            freshnessScore = Math.min(freshnessScore, 0.36F);
        } else if (band == FreshnessBand.MOLDY) {
            freshnessScore = Math.min(freshnessScore, 0.16F);
        }

        float total = Mth.clamp(
                recipeAccuracy * 0.24F
                        + seasoning * 0.15F
                        + cooking * 0.20F
                        + texture * 0.14F
                        + freshnessScore * 0.15F
                        + presentation * 0.08F
                        + ingredientQuality * 0.04F,
                0.0F,
                1.0F
        );
        if (band == FreshnessBand.SPOILED) {
            total *= 0.55F;
        } else if (band == FreshnessBand.MOLDY) {
            total *= 0.25F;
        }
        total = Mth.clamp(total, 0.0F, 1.0F);

        int enjoymentPenalty = Math.max(0, Math.round((1.0F - average(seasoning, cooking, texture, freshnessScore)) * 3.0F));
        return new FoodMatterGradeScores(
                total,
                ingredientQuality,
                recipeAccuracy,
                seasoning,
                cooking,
                texture,
                freshnessScore,
                presentation,
                data.nourishment(),
                Math.max(0, data.enjoyment() - enjoymentPenalty)
        );
    }

    private static QualityBreakdown evaluateSummaryStack(
            IngredientStateData data,
            ItemStack stack,
            Level level,
            float freshness,
            IngredientState effectiveState
    ) {
        float prep = Mth.clamp((data.texture() + data.purity()) * 0.5F, 0.0F, 1.0F);
        float combine = Mth.clamp((data.structure() + data.aeration() + data.moisture()) / 3.0F, 0.0F, 1.0F);
        float cooking = Mth.clamp((data.flavor() + data.moisture() + data.texture()) / 3.0F, 0.0F, 1.0F);
        float finishing = isFinishedState(effectiveState)
                ? 0.95F
                : requiresFinishing(effectiveState)
                ? 0.18F
                : data.processDepth() >= 4
                ? 0.7F
                : 0.45F;
        float plating = effectiveState.isPlatedState() ? 1.0F : 0.7F;
        float total = Mth.clamp(
                data.quality() * 0.22F
                        + freshness * 0.14F
                        + data.recipeAccuracy() * 0.16F
                        + prep * 0.12F
                        + combine * 0.12F
                        + cooking * 0.14F
                        + finishing * 0.06F
                        + plating * 0.04F,
                0.0F,
                1.0F
        );
        FreshnessBand band = KitchenStackUtil.freshnessBand(stack, level);
        if (band == FreshnessBand.SPOILED) {
            total *= 0.55F;
        } else if (band == FreshnessBand.MOLDY) {
            total *= 0.25F;
        }
        total = Mth.clamp(total, 0.0F, 1.0F);

        return new QualityBreakdown(
                DishGrade.fromScore(total),
                total,
                data.quality(),
                freshness,
                prep,
                combine,
                cooking,
                finishing,
                plating,
                data.recipeAccuracy(),
                data.nourishment(),
                data.enjoyment()
        );
    }

    private static AggregateStats aggregate(Level level, List<ItemStack> inputs) {
        long createdTick = 0L;
        float quality = 0.0F;
        float recipeAccuracy = 0.0F;
        float flavor = 0.0F;
        float texture = 0.0F;
        float structure = 0.0F;
        float moisture = 0.0F;
        float purity = 0.0F;
        float aeration = 0.0F;
        float freshness = 0.0F;
        int nourishment = 0;
        int enjoyment = 0;
        int maxDepth = 0;
        int counted = 0;

        for (ItemStack stack : inputs) {
            IngredientStateData data = KitchenStackUtil.getOrCreateData(stack, level.getGameTime());
            if (data == null) {
                continue;
            }

            createdTick += data.createdTick();
            quality += data.quality();
            recipeAccuracy += data.recipeAccuracy();
            flavor += data.flavor();
            texture += data.texture();
            structure += data.structure();
            moisture += data.moisture();
            purity += data.purity();
            aeration += data.aeration();
            freshness += KitchenStackUtil.currentFreshnessScore(stack, level);
            nourishment += data.nourishment();
            enjoyment += data.enjoyment();
            maxDepth = Math.max(maxDepth, data.processDepth());
            counted++;
        }

        if (counted == 0) {
            counted = 1;
            createdTick = level.getGameTime();
            quality = 0.55F;
            recipeAccuracy = 0.65F;
            flavor = 0.45F;
            texture = 0.45F;
            structure = 0.40F;
            moisture = 0.45F;
            purity = 0.40F;
            aeration = 0.15F;
            freshness = 0.55F;
        }

        return new AggregateStats(
                createdTick / counted,
                quality / counted,
                recipeAccuracy / counted,
                flavor / counted,
                texture / counted,
                structure / counted,
                moisture / counted,
                purity / counted,
                aeration / counted,
                freshness / counted,
                Math.max(0, nourishment / counted),
                Math.max(0, enjoyment / counted),
                maxDepth
        );
    }

    private static boolean isFinishedState(IngredientState state) {
        return switch (state) {
            case COOLED, RESTED, PLATED, COOLED_PIE, RESTED_PIE, RESTED_BREAD, SLICED_BREAD, SLICED_PIE,
                    PLATED_SLICE, PLATED_SOUP_MEAL, PLATED_DUMPLING_MEAL, PLATED_FRIED_MEAL, PLATED_ROAST_MEAL -> true;
            default -> false;
        };
    }

    private static boolean requiresFinishing(IngredientState state) {
        return switch (state) {
            case BAKED_BREAD, BAKED_PIE, COOLED_PIE, FRIED_PROTEIN, BROILED_PROTEIN, ROASTED_PROTEIN -> true;
            default -> false;
        };
    }

    private static float servingFit(FoodMatterData matter, IngredientState state) {
        if (matter.finalizedServing() || state.isPlatedState()) {
            return 1.0F;
        }
        if (isFinishedState(state)) {
            return 0.82F;
        }
        if (requiresFinishing(state)) {
            return 0.22F;
        }
        return matter.processDepth() >= 2 ? 0.56F : 0.34F;
    }

    private static float foodSafetyIntegrity(FoodMatterData matter) {
        return Mth.clamp(
                1.0F
                        - matter.oxidation() * 0.30F
                        - matter.microbialLoad() * 0.42F
                        - matter.charLevel() * 0.22F,
                0.0F,
                1.0F
        );
    }

    private static float targetAeration(DishFamily family) {
        return switch (family) {
            case BLEND -> 0.22F;
            case DOUGH -> 0.12F;
            case BAKED -> 0.18F;
            case EGG -> 0.12F;
            case SAUCE, SOUP -> 0.08F;
            default -> 0.10F;
        };
    }

    private static float seasoningBalance(FoodMatterData matter, DishFamily family) {
        float totalSeasoning = matter.seasoningLoad() * 0.45F
                + matter.cheeseLoad() * 0.24F
                + matter.onionLoad() * 0.14F
                + matter.herbLoad() * 0.09F
                + matter.pepperLoad() * 0.08F;
        float target = switch (family) {
            case JUICE -> 0.04F;
            case BLEND -> 0.10F;
            case DOUGH -> 0.08F;
            case SAUCE -> 0.18F;
            case SOUP -> 0.22F;
            case FRIED, BAKED, PLATED -> 0.20F;
            case GRAIN -> 0.18F;
            case EGG -> 0.20F;
            default -> matter.protein() > 0.16F ? 0.20F : 0.14F;
        };
        return around(totalSeasoning, target, 0.18F);
    }

    private static float moistureFit(FoodMatterData matter, IngredientState state, DishRecognitionResult recognition, DishFamily family) {
        float target = switch (recognitionKey(recognition)) {
            case "soft_scrambled_eggs" -> 0.60F;
            case "scrambled_eggs" -> 0.40F;
            case "omelet" -> 0.42F;
            case "browned_omelet" -> 0.34F;
            case "lemon_juice", "mixed_juice" -> 0.82F;
            case "fruit_juice_blend", "smoothie_blend" -> 0.68F;
            case "nut_butter", "packed_breadcrumbs" -> 0.12F;
            case "hummus_prep", "garlic_butter" -> 0.28F;
            case "cheese_sauce" -> 0.58F;
            case "pie_dough", "focaccia_dough" -> 0.34F;
            case "packed_freeze_dry_apples", "freeze_dried_meal" -> 0.08F;
            default -> switch (family) {
                case JUICE -> 0.82F;
                case BLEND -> 0.64F;
                case DOUGH -> 0.34F;
                case SAUCE -> 0.30F;
                case SOUP -> 0.72F;
                case BAKED -> 0.28F;
                case FRIED -> 0.34F;
                case GRAIN -> 0.44F;
                case PLATED -> 0.52F;
                case EGG -> 0.44F;
                default -> switch (state) {
                    case COARSE_POWDER, FREEZE_DRIED -> 0.10F;
                    case PAN_FRIED -> 0.40F;
                    default -> 0.50F;
                };
            };
        };
        return around(matter.water(), target, 0.24F);
    }

    private static float donenessFit(FoodMatterData matter, IngredientState state, DishRecognitionResult recognition, DishFamily family) {
        if (matter.protein() <= 0.08F && matter.timeInPan() <= 0 && matter.proteinSet() <= 0.05F) {
            return family == DishFamily.BAKED || family == DishFamily.FRIED ? 0.58F : 0.72F;
        }
        float target = switch (recognitionKey(recognition)) {
            case "soft_scrambled_eggs" -> 0.55F;
            case "scrambled_eggs" -> 0.72F;
            case "omelet", "browned_omelet" -> 0.68F;
            case "burnt_eggs" -> 0.78F;
            default -> switch (family) {
                case EGG -> 0.66F;
                case FRIED -> 0.72F;
                case BAKED -> 0.70F;
                case SOUP -> 0.62F;
                case PLATED -> 0.66F;
                default -> state == IngredientState.PAN_FRIED ? 0.68F : 0.60F;
            };
        };
        return around(matter.proteinSet(), target, 0.28F);
    }

    private static float browningFit(FoodMatterData matter, IngredientState state, DishRecognitionResult recognition, DishFamily family) {
        if (state != IngredientState.PAN_FRIED && matter.timeInPan() <= 0 && matter.browning() <= 0.01F) {
            return family == DishFamily.BAKED || family == DishFamily.FRIED ? 0.44F : 0.78F;
        }
        float target = switch (recognitionKey(recognition)) {
            case "soft_scrambled_eggs", "scrambled_eggs", "omelet" -> 0.10F;
            case "browned_omelet" -> 0.32F;
            case "burnt_eggs" -> 0.15F;
            default -> switch (family) {
                case FRIED -> 0.20F;
                case BAKED -> 0.22F;
                case SOUP -> 0.10F;
                case GRAIN -> 0.12F;
                case PLATED -> 0.16F;
                default -> state == IngredientState.PAN_FRIED ? 0.16F : 0.08F;
            };
        };
        return around(matter.browning(), target, 0.22F) * (1.0F - matter.charLevel() * 0.75F);
    }

    private static float structureFit(FoodMatterData matter, IngredientState state, DishRecognitionResult recognition, DishFamily family) {
        float targetFragmentation = switch (recognitionKey(recognition)) {
            case "soft_scrambled_eggs" -> 0.70F;
            case "scrambled_eggs" -> 0.62F;
            case "omelet", "browned_omelet" -> 0.20F;
            case "nut_butter", "hummus_prep" -> 0.30F;
            case "packed_breadcrumbs" -> 0.42F;
            case "garlic_butter" -> 0.20F;
            case "cheese_sauce" -> 0.18F;
            case "lemon_juice", "mixed_juice" -> 0.08F;
            case "fruit_juice_blend", "smoothie_blend" -> 0.16F;
            case "pie_dough", "focaccia_dough" -> 0.10F;
            case "packed_freeze_dry_apples", "freeze_dried_meal" -> 0.32F;
            case "chopped_produce_blend" -> 0.36F;
            default -> switch (family) {
                case JUICE -> 0.08F;
                case BLEND -> 0.18F;
                case SAUCE -> 0.22F;
                case DOUGH -> 0.10F;
                case SOUP -> 0.28F;
                case FRIED -> 0.40F;
                case BAKED -> 0.20F;
                case GRAIN -> 0.32F;
                case PLATED -> 0.30F;
                default -> switch (state) {
                    case COARSE_POWDER, FREEZE_DRIED -> 0.36F;
                    case ROUGH_CUT -> 0.36F;
                    case PAN_FRIED -> 0.45F;
                    default -> 0.28F;
                };
            };
        };
        float targetCohesiveness = switch (recognitionKey(recognition)) {
            case "soft_scrambled_eggs" -> 0.24F;
            case "scrambled_eggs" -> 0.34F;
            case "omelet", "browned_omelet" -> 0.72F;
            case "nut_butter" -> 0.30F;
            case "packed_breadcrumbs" -> 0.16F;
            case "garlic_butter" -> 0.36F;
            case "cheese_sauce" -> 0.46F;
            case "lemon_juice", "mixed_juice" -> 0.12F;
            case "fruit_juice_blend", "smoothie_blend" -> 0.46F;
            case "pie_dough" -> 0.70F;
            case "focaccia_dough" -> 0.76F;
            case "packed_freeze_dry_apples", "freeze_dried_meal" -> 0.28F;
            case "chopped_produce_blend" -> 0.30F;
            default -> switch (family) {
                case JUICE -> 0.12F;
                case BLEND -> 0.44F;
                case SAUCE -> 0.42F;
                case DOUGH -> 0.72F;
                case SOUP -> 0.24F;
                case FRIED -> 0.46F;
                case BAKED -> 0.56F;
                case GRAIN -> 0.42F;
                case PLATED -> 0.44F;
                default -> switch (state) {
                    case COARSE_POWDER, FREEZE_DRIED -> 0.20F;
                    case ROUGH_CUT -> 0.30F;
                    case PAN_FRIED -> 0.42F;
                    default -> 0.36F;
                };
            };
        };
        return Mth.clamp(
                around(matter.fragmentation(), targetFragmentation, 0.24F) * 0.50F
                        + around(matter.cohesiveness(), targetCohesiveness, 0.24F) * 0.50F,
                0.0F,
                1.0F
        );
    }

    private static float compositionFit(FoodMatterData matter, FoodMaterialProfile expectedProfile, DishFamily family) {
        if (expectedProfile == null) {
            return switch (family) {
                case JUICE -> average(around(matter.water(), 0.82F, 0.22F), around(matter.protein(), 0.04F, 0.12F));
                case DOUGH -> average(around(matter.water(), 0.34F, 0.18F), around(matter.protein(), 0.12F, 0.16F));
                case SAUCE -> average(around(matter.water(), 0.34F, 0.24F), around(matter.fat(), 0.18F, 0.18F));
                case SOUP -> average(around(matter.water(), 0.72F, 0.18F), around(matter.protein(), 0.18F, 0.16F));
                case FRIED -> average(around(matter.fat(), 0.18F, 0.18F), around(matter.protein(), 0.22F, 0.18F));
                case BAKED -> average(around(matter.fat(), 0.16F, 0.18F), around(matter.protein(), 0.16F, 0.18F));
                case GRAIN -> average(around(matter.water(), 0.46F, 0.18F), around(matter.protein(), 0.14F, 0.14F));
                default -> 0.62F;
            };
        }
        return Mth.clamp(
                average(
                        closeness(matter.water(), expectedProfile.water()),
                        closeness(matter.fat(), expectedProfile.fat()),
                        closeness(matter.protein(), expectedProfile.protein()),
                        closeness(matter.seasoningLoad(), expectedProfile.seasoningLoad()),
                        closeness(matter.cheeseLoad(), expectedProfile.cheeseLoad()),
                        closeness(matter.onionLoad(), expectedProfile.onionLoad()),
                        closeness(matter.herbLoad(), expectedProfile.herbLoad()),
                        closeness(matter.pepperLoad(), expectedProfile.pepperLoad()),
                        traitCoverage(matter, expectedProfile)
                ),
                0.0F,
                1.0F
        );
    }

    private static float traitCoverage(FoodMatterData matter, FoodMaterialProfile expectedProfile) {
        if (expectedProfile == null || expectedProfile.traitMask() == 0L) {
            return 0.60F;
        }
        int total = 0;
        int matched = 0;
        for (FoodTrait trait : FoodTrait.unpack(expectedProfile.traitMask())) {
            total++;
            if (matter.hasTrait(trait)) {
                matched++;
            }
        }
        return total > 0 ? matched / (float) total : 0.60F;
    }

    private static float closeness(float actual, float expected) {
        return Mth.clamp(1.0F - Math.abs(actual - expected), 0.0F, 1.0F);
    }

    private static float average(float... values) {
        if (values.length == 0) {
            return 0.0F;
        }
        float total = 0.0F;
        for (float value : values) {
            total += value;
        }
        return total / values.length;
    }

    private static DishFamily dishFamily(
            ItemStack stack,
            IngredientState state,
            DishRecognitionResult recognition,
            FoodMatterData matter
    ) {
        if ((state == IngredientState.PAN_FRIED || matter.timeInPan() > 0)
                && matter.hasTrait(FoodTrait.EGG)) {
            return DishFamily.EGG;
        }
        if (state == IngredientState.FRESH_JUICE) {
            return DishFamily.JUICE;
        }
        if (state == IngredientState.SMOOTH || state == IngredientState.CREAMY) {
            return DishFamily.BLEND;
        }
        if (state == IngredientState.DOUGH || state == IngredientState.BREAD_DOUGH || state == IngredientState.BATTER) {
            return DishFamily.DOUGH;
        }
        if (state == IngredientState.SMOOTH_PASTE || state == IngredientState.PASTE || state == IngredientState.CREAMY) {
            return DishFamily.SAUCE;
        }
        if (state == IngredientState.SIMMERED || state == IngredientState.SIMMERED_FILLING || state == IngredientState.SOUP_BASE) {
            return DishFamily.SOUP;
        }
        if (state == IngredientState.PAN_FRIED || state == IngredientState.DEEP_FRIED || state == IngredientState.FRIED_PROTEIN) {
            return DishFamily.FRIED;
        }
        if (state == IngredientState.BAKED || state == IngredientState.BAKED_BREAD || state == IngredientState.BAKED_PIE) {
            return DishFamily.BAKED;
        }
        if (state == IngredientState.BOILED || state == IngredientState.MIXED) {
            return DishFamily.GRAIN;
        }
        if (state.isPlatedState() || matter.finalizedServing()) {
            return DishFamily.PLATED;
        }
        return DishFamily.GENERIC;
    }

    private static float around(float value, float target, float tolerance) {
        if (tolerance <= 0.0F) {
            return value == target ? 1.0F : 0.0F;
        }
        return Mth.clamp(1.0F - (Math.abs(value - target) / tolerance), 0.0F, 1.0F);
    }

    private static String recognitionKey(DishRecognitionResult recognition) {
        return recognition != null ? recognition.key() : "";
    }
}
