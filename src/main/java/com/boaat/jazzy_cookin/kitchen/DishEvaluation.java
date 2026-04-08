package com.boaat.jazzy_cookin.kitchen;

import java.util.List;

import com.boaat.jazzy_cookin.item.KitchenToolItem;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishRecognitionResult;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishSchema;
import com.boaat.jazzy_cookin.recipe.KitchenPlateRecipe;
import com.boaat.jazzy_cookin.recipe.KitchenProcessOutput;
import com.boaat.jazzy_cookin.recipe.KitchenProcessRecipe;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class DishEvaluation {
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

    private record MethodAdjustment(
            float quality,
            float recipeAccuracy,
            float flavor,
            float texture,
            float structure,
            float moisture,
            float purity,
            float aeration
    ) {
    }

    private DishEvaluation() {
    }

    public static IngredientStateData evaluateProcess(
            Level level,
            KitchenProcessRecipe recipe,
            KitchenProcessOutput output,
            List<ItemStack> inputs,
            ItemStack toolStack,
            HeatLevel actualHeat,
            boolean preheated,
            float inputMatchScore
    ) {
        AggregateStats stats = aggregate(level, inputs);
        ToolProfile actualProfile = ToolProfile.fromStack(toolStack);
        List<ToolProfile> allowedTools = recipe.allowedToolsOrPreferred();
        float normalizedInputScore = Mth.clamp(inputMatchScore, 0.0F, 1.0F);
        float matchPenalty = (1.0F - normalizedInputScore) * 0.28F;
        float repeatPenalty = Mth.clamp(Math.max(0, stats.maxDepth() - 1) * 0.05F, 0.0F, 0.22F);
        float microwaveQualityPenalty = recipe.method() == KitchenMethod.MICROWAVE ? 0.10F : 0.0F;
        int enjoymentPenalty = (recipe.method() == KitchenMethod.MICROWAVE ? 1 : 0) + Math.min(2, Math.max(0, stats.maxDepth() - 2));

        float toolAccuracy = 0.0F;
        float toolQuality = 0.0F;
        if (!allowedTools.isEmpty()) {
            if (recipe.preferredTool().isPresent() && actualProfile == recipe.preferredTool().get() && toolStack.getItem() instanceof KitchenToolItem toolItem) {
                toolAccuracy = 0.12F;
                toolQuality = toolItem.qualityBonus();
            } else if (actualProfile != ToolProfile.NONE && recipe.allowsTool(actualProfile) && toolStack.getItem() instanceof KitchenToolItem toolItem) {
                toolAccuracy = -0.04F;
                toolQuality = toolItem.qualityBonus() * 0.35F;
            } else if (actualProfile == ToolProfile.NONE) {
                toolAccuracy = recipe.toolRequired() ? -0.22F : -0.10F;
                toolQuality = recipe.toolRequired() ? -0.14F : -0.06F;
            } else {
                toolAccuracy = recipe.toolRequired() ? -0.16F : -0.06F;
                toolQuality = -0.04F;
            }
        }

        float heatAccuracy = evaluateHeatAccuracy(recipe, actualHeat);
        float heatQuality = evaluateHeatQuality(recipe, actualHeat);
        float preheatAccuracy = recipe.requiresPreheat() ? (preheated ? 0.10F : -0.14F) : 0.02F;
        MethodAdjustment adjustment = methodAdjustment(recipe.method(), heatAccuracy, toolAccuracy, recipe.mode());
        float recipeAccuracy = Mth.clamp(
                stats.recipeAccuracy() * 0.36F
                        + normalizedInputScore * 0.28F
                        + 0.18F
                        + toolAccuracy
                        + heatAccuracy
                        + preheatAccuracy
                        + adjustment.recipeAccuracy()
                        + output.recipeAccuracyDelta()
                        - repeatPenalty
                        - matchPenalty,
                0.0F,
                1.0F
        );
        float freshnessPenalty = stats.freshness() < 0.2F ? -0.12F : stats.freshness() < 0.45F ? -0.05F : 0.0F;
        float finalQuality = Mth.clamp(
                stats.quality() * 0.34F
                        + stats.freshness() * 0.15F
                        + recipeAccuracy * 0.18F
                        + output.qualityBonus()
                        + toolQuality
                        + heatQuality
                        + adjustment.quality()
                        + freshnessPenalty
                        - repeatPenalty
                        - matchPenalty
                        - microwaveQualityPenalty,
                0.05F,
                1.0F
        );

        return new IngredientStateData(
                output.state(),
                stats.createdTick(),
                finalQuality,
                recipeAccuracy,
                Mth.clamp(stats.flavor() + output.flavorDelta() + adjustment.flavor() + heatQuality * 0.3F, 0.0F, 1.0F),
                Mth.clamp(stats.texture() + output.textureDelta() + adjustment.texture() + toolQuality * 0.2F, 0.0F, 1.0F),
                Mth.clamp(stats.structure() + output.structureDelta() + adjustment.structure(), 0.0F, 1.0F),
                Mth.clamp(stats.moisture() + output.moistureDelta() + adjustment.moisture(), 0.0F, 1.0F),
                Mth.clamp(stats.purity() + output.purityDelta() + adjustment.purity(), 0.0F, 1.0F),
                Mth.clamp(stats.aeration() + output.aerationDelta() + adjustment.aeration(), 0.0F, 1.0F),
                stats.maxDepth() + 1,
                Math.max(output.nourishment(), stats.nourishment()),
                Math.max(0, Math.max(output.enjoyment(), stats.enjoyment()) - enjoymentPenalty)
        );
    }

    public static IngredientStateData evaluatePlate(Level level, KitchenPlateRecipe recipe, List<ItemStack> inputs, float inputMatchScore) {
        AggregateStats stats = aggregate(level, inputs);
        float normalizedInputScore = Mth.clamp(inputMatchScore, 0.0F, 1.0F);
        float matchPenalty = (1.0F - normalizedInputScore) * 0.24F;
        float plateAccuracy = Mth.clamp(
                stats.recipeAccuracy() * 0.48F
                        + normalizedInputScore * 0.34F
                        + 0.14F
                        + recipe.output().recipeAccuracyDelta()
                        - matchPenalty,
                0.0F,
                1.0F
        );

        return new IngredientStateData(
                recipe.output().state(),
                stats.createdTick(),
                Mth.clamp(
                        stats.quality() * 0.42F
                                + stats.freshness() * 0.16F
                                + plateAccuracy * 0.22F
                                + recipe.output().qualityBonus()
                                + 0.08F
                                - matchPenalty,
                        0.05F,
                        1.0F
                ),
                plateAccuracy,
                Mth.clamp(stats.flavor() + recipe.output().flavorDelta() + 0.03F, 0.0F, 1.0F),
                Mth.clamp(stats.texture() + recipe.output().textureDelta() + 0.05F, 0.0F, 1.0F),
                Mth.clamp(stats.structure() + recipe.output().structureDelta() + 0.03F, 0.0F, 1.0F),
                Mth.clamp(stats.moisture() + recipe.output().moistureDelta(), 0.0F, 1.0F),
                Mth.clamp(stats.purity() + recipe.output().purityDelta() + 0.02F, 0.0F, 1.0F),
                Mth.clamp(stats.aeration() + recipe.output().aerationDelta(), 0.0F, 1.0F),
                stats.maxDepth() + 1,
                Math.max(recipe.output().nourishment(), stats.nourishment()),
                Math.max(0, Math.max(recipe.output().enjoyment(), stats.enjoyment()) - Math.max(0, Math.round(matchPenalty * 3.0F)))
        );
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

        DishRecognitionResult recognition = DishSchema.preview(foodMatter);
        float recognitionScore = recognition != null ? recognition.score() : 0.38F;
        float recognitionQuality = recognition != null ? recognition.score() * recognition.desirability() : 0.32F;
        float structureFit = structureFit(foodMatter, effectiveState, recognition);
        float seasoningBalance = seasoningBalance(foodMatter, effectiveState);
        float moistureFit = moistureFit(foodMatter, effectiveState, recognition);
        float donenessFit = donenessFit(foodMatter, effectiveState, recognition);
        float browningFit = browningFit(foodMatter, effectiveState, recognition);
        float prep = Mth.clamp(
                data.purity() * 0.30F
                        + structureFit * 0.30F
                        + Mth.clamp(foodMatter.processDepth() / 4.0F, 0.0F, 1.0F) * 0.16F
                        + foodMatter.aeration() * 0.10F
                        + recognitionScore * 0.14F,
                0.0F,
                1.0F
        );
        float combine = Mth.clamp(
                structureFit * 0.34F
                        + seasoningBalance * 0.26F
                        + moistureFit * 0.14F
                        + recognitionScore * 0.26F,
                0.0F,
                1.0F
        );
        float cooking = Mth.clamp(
                donenessFit * 0.36F
                        + moistureFit * 0.28F
                        + browningFit * 0.20F
                        + (1.0F - foodMatter.charLevel()) * 0.16F,
                0.0F,
                1.0F
        );
        boolean finalizedServing = foodMatter.finalizedServing();
        float finishing = Mth.clamp(
                recognitionQuality * 0.42F
                        + servingFit(foodMatter, effectiveState) * 0.32F
                        + Mth.clamp(foodMatter.processDepth() / 5.0F, 0.0F, 1.0F) * 0.12F
                        + (finalizedServing ? 0.14F : 0.0F),
                0.0F,
                1.0F
        );
        float plating = finalizedServing || effectiveState.isPlatedState()
                ? 1.0F
                : isFinishedState(effectiveState)
                ? 0.82F
                : recognition != null && recognition.desirability() >= 0.80F
                ? 0.58F
                : 0.42F;
        float total = Mth.clamp(
                data.quality() * 0.10F
                        + freshness * 0.14F
                        + data.recipeAccuracy() * 0.10F
                        + recognitionQuality * 0.16F
                        + prep * 0.12F
                        + combine * 0.12F
                        + cooking * 0.16F
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

    private static float evaluateHeatAccuracy(KitchenProcessRecipe recipe, HeatLevel actualHeat) {
        if (!recipe.usesHeat()) {
            return 0.03F;
        }
        if (actualHeat == HeatLevel.OFF) {
            return -0.18F;
        }
        if (actualHeat.ordinal() < recipe.minimumHeat().ordinal()) {
            return -0.14F;
        }
        if (recipe.maximumHeat() != HeatLevel.OFF && actualHeat.ordinal() > recipe.maximumHeat().ordinal()) {
            return -0.12F;
        }
        if (recipe.preferredHeat() != HeatLevel.OFF && actualHeat == recipe.preferredHeat()) {
            return 0.10F;
        }
        return 0.03F;
    }

    private static float evaluateHeatQuality(KitchenProcessRecipe recipe, HeatLevel actualHeat) {
        if (!recipe.usesHeat()) {
            return 0.0F;
        }
        if (actualHeat == HeatLevel.OFF) {
            return -0.12F;
        }
        if (recipe.maximumHeat() != HeatLevel.OFF && actualHeat.ordinal() > recipe.maximumHeat().ordinal()) {
            return -0.10F;
        }
        if (actualHeat.ordinal() < recipe.minimumHeat().ordinal()) {
            return -0.08F;
        }
        return actualHeat == recipe.preferredHeat() ? 0.07F : 0.02F;
    }

    private static MethodAdjustment methodAdjustment(KitchenMethod method, float heatAccuracy, float toolAccuracy, ProcessMode mode) {
        float passiveBonus = mode == ProcessMode.PASSIVE ? 0.02F : 0.0F;
        return switch (method) {
            case CUT -> new MethodAdjustment(0.02F, 0.06F, 0.00F, 0.08F + toolAccuracy * 0.2F, 0.02F, -0.02F, 0.02F, 0.00F);
            case GRIND -> new MethodAdjustment(0.03F, 0.07F, 0.12F + toolAccuracy * 0.15F, -0.02F, 0.00F, -0.02F, 0.10F, 0.02F);
            case STRAIN -> new MethodAdjustment(0.04F, 0.06F, 0.02F, -0.02F, 0.00F, -0.03F, 0.18F, 0.00F);
            case MIX -> new MethodAdjustment(0.03F, 0.06F, 0.02F, 0.02F, 0.08F, 0.05F, 0.03F, 0.02F);
            case PROCESS -> new MethodAdjustment(0.03F, 0.07F, 0.10F, 0.06F, 0.04F, -0.04F, 0.06F, 0.02F);
            case BLEND -> new MethodAdjustment(0.04F, 0.08F, 0.04F, 0.06F, 0.04F, 0.08F, 0.03F, 0.10F);
            case JUICE -> new MethodAdjustment(0.03F, 0.06F, 0.02F, 0.02F, 0.00F, 0.12F, 0.08F, 0.00F);
            case WHISK -> new MethodAdjustment(0.02F, 0.08F, 0.00F, 0.04F, 0.06F, 0.00F, 0.02F, 0.18F + toolAccuracy * 0.12F);
            case KNEAD -> new MethodAdjustment(0.03F, 0.09F, 0.00F, 0.06F, 0.18F + toolAccuracy * 0.15F, -0.01F, 0.02F, 0.03F);
            case BATTER -> new MethodAdjustment(0.03F, 0.07F, 0.04F, 0.10F, 0.06F, 0.05F, 0.02F, 0.01F);
            case MARINATE -> new MethodAdjustment(0.04F + passiveBonus, 0.08F + passiveBonus, 0.16F, 0.02F, 0.02F, 0.06F, 0.04F, 0.00F);
            case BOIL -> new MethodAdjustment(-0.01F + heatAccuracy * 0.1F, 0.04F, 0.06F, -0.03F, 0.02F, 0.10F, -0.04F, 0.00F);
            case SIMMER -> new MethodAdjustment(0.04F + passiveBonus, 0.08F + passiveBonus, 0.15F, 0.02F, 0.04F, 0.08F, 0.10F, 0.00F);
            case PAN_FRY -> new MethodAdjustment(0.05F, 0.08F, 0.18F, 0.16F, 0.04F, -0.07F, 0.00F, 0.00F);
            case DEEP_FRY -> new MethodAdjustment(0.04F, 0.07F, 0.20F, 0.22F, 0.02F, -0.10F, -0.02F, 0.00F);
            case BAKE -> new MethodAdjustment(0.06F, 0.10F, 0.08F, 0.12F, 0.18F, -0.04F, 0.02F, 0.04F);
            case ROAST -> new MethodAdjustment(0.05F, 0.08F, 0.18F, 0.12F, 0.08F, -0.10F, 0.00F, 0.00F);
            case BROIL -> new MethodAdjustment(0.03F, 0.05F, 0.14F, 0.08F, 0.02F, -0.08F, 0.00F, 0.00F);
            case STEAM -> new MethodAdjustment(0.03F, 0.08F, 0.06F, 0.08F, 0.04F, 0.10F, 0.04F, 0.00F);
            case SMOKE -> new MethodAdjustment(0.05F + passiveBonus, 0.08F + passiveBonus, 0.22F, 0.06F, 0.04F, -0.05F, 0.03F, 0.00F);
            case FERMENT -> new MethodAdjustment(0.04F + passiveBonus, 0.08F + passiveBonus, 0.12F, 0.02F, 0.02F, 0.03F, 0.08F, 0.00F);
            case CAN -> new MethodAdjustment(0.05F + passiveBonus, 0.08F, 0.08F, 0.00F, 0.02F, 0.08F, 0.12F, 0.00F);
            case DRY -> new MethodAdjustment(0.03F + passiveBonus, 0.07F, 0.12F, 0.05F, 0.02F, -0.20F, 0.04F, 0.00F);
            case FREEZE_DRY -> new MethodAdjustment(0.04F + passiveBonus, 0.08F, 0.10F, 0.06F, 0.04F, -0.18F, 0.18F, 0.00F);
            case MICROWAVE -> new MethodAdjustment(0.02F + heatAccuracy * 0.1F, 0.05F, 0.08F, 0.06F, 0.02F, -0.04F, 0.00F, 0.00F);
            case COOL -> new MethodAdjustment(0.03F, 0.06F, 0.00F, 0.04F, 0.06F, 0.02F, 0.02F, 0.00F);
            case REST -> new MethodAdjustment(0.05F, 0.08F, 0.02F, 0.10F, 0.10F, 0.04F, 0.02F, 0.00F);
            case SLICE -> new MethodAdjustment(0.02F, 0.05F, 0.00F, 0.04F, 0.02F, 0.00F, 0.03F, 0.00F);
            case PLATE -> new MethodAdjustment(0.04F, 0.10F, 0.03F, 0.04F, 0.03F, 0.02F, 0.02F, 0.00F);
            case NONE -> new MethodAdjustment(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        };
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

    private static float seasoningBalance(FoodMatterData matter, IngredientState state) {
        float totalSeasoning = matter.seasoningLoad() * 0.45F
                + matter.cheeseLoad() * 0.24F
                + matter.onionLoad() * 0.14F
                + matter.herbLoad() * 0.09F
                + matter.pepperLoad() * 0.08F;
        float target = switch (state) {
            case FRESH_JUICE, FREEZE_DRIED -> 0.05F;
            case SMOOTH, CREAMY -> 0.12F;
            case DOUGH, BREAD_DOUGH -> 0.10F;
            case PASTE, SMOOTH_PASTE -> 0.18F;
            case PAN_FRIED -> 0.22F;
            default -> matter.protein() > 0.16F ? 0.20F : 0.14F;
        };
        return around(totalSeasoning, target, 0.18F);
    }

    private static float moistureFit(FoodMatterData matter, IngredientState state, DishRecognitionResult recognition) {
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
            default -> switch (state) {
                case FRESH_JUICE -> 0.82F;
                case SMOOTH, CREAMY -> 0.64F;
                case DOUGH, BREAD_DOUGH -> 0.34F;
                case PASTE, SMOOTH_PASTE -> 0.26F;
                case COARSE_POWDER, FREEZE_DRIED -> 0.10F;
                case PAN_FRIED -> 0.40F;
                default -> 0.50F;
            };
        };
        return around(matter.water(), target, 0.24F);
    }

    private static float donenessFit(FoodMatterData matter, IngredientState state, DishRecognitionResult recognition) {
        if (matter.protein() <= 0.08F && matter.timeInPan() <= 0 && matter.proteinSet() <= 0.05F) {
            return 0.72F;
        }
        float target = switch (recognitionKey(recognition)) {
            case "soft_scrambled_eggs" -> 0.55F;
            case "scrambled_eggs" -> 0.72F;
            case "omelet", "browned_omelet" -> 0.68F;
            case "burnt_eggs" -> 0.78F;
            default -> state == IngredientState.PAN_FRIED ? 0.68F : 0.64F;
        };
        return around(matter.proteinSet(), target, 0.28F);
    }

    private static float browningFit(FoodMatterData matter, IngredientState state, DishRecognitionResult recognition) {
        if (state != IngredientState.PAN_FRIED && matter.timeInPan() <= 0 && matter.browning() <= 0.01F) {
            return 0.78F;
        }
        float target = switch (recognitionKey(recognition)) {
            case "soft_scrambled_eggs", "scrambled_eggs", "omelet" -> 0.10F;
            case "browned_omelet" -> 0.32F;
            case "burnt_eggs" -> 0.15F;
            default -> state == IngredientState.PAN_FRIED ? 0.16F : 0.08F;
        };
        return around(matter.browning(), target, 0.22F) * (1.0F - matter.charLevel() * 0.75F);
    }

    private static float structureFit(FoodMatterData matter, IngredientState state, DishRecognitionResult recognition) {
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
            default -> switch (state) {
                case FRESH_JUICE -> 0.08F;
                case SMOOTH, CREAMY -> 0.18F;
                case PASTE, SMOOTH_PASTE -> 0.28F;
                case DOUGH, BREAD_DOUGH -> 0.10F;
                case COARSE_POWDER, FREEZE_DRIED -> 0.36F;
                case ROUGH_CUT -> 0.36F;
                case PAN_FRIED -> 0.45F;
                default -> 0.28F;
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
            default -> switch (state) {
                case FRESH_JUICE -> 0.12F;
                case SMOOTH, CREAMY -> 0.44F;
                case PASTE, SMOOTH_PASTE -> 0.32F;
                case DOUGH, BREAD_DOUGH -> 0.72F;
                case COARSE_POWDER, FREEZE_DRIED -> 0.20F;
                case ROUGH_CUT -> 0.30F;
                case PAN_FRIED -> 0.42F;
                default -> 0.36F;
            };
        };
        return Mth.clamp(
                around(matter.fragmentation(), targetFragmentation, 0.24F) * 0.50F
                        + around(matter.cohesiveness(), targetCohesiveness, 0.24F) * 0.50F,
                0.0F,
                1.0F
        );
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
