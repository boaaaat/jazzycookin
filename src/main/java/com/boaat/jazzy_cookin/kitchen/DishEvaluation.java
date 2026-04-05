package com.boaat.jazzy_cookin.kitchen;

import java.util.List;

import com.boaat.jazzy_cookin.item.KitchenToolItem;
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
            boolean preheated
    ) {
        AggregateStats stats = aggregate(level, inputs);
        ToolProfile actualProfile = ToolProfile.fromStack(toolStack);
        List<ToolProfile> allowedTools = recipe.allowedToolsOrPreferred();

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
                stats.recipeAccuracy() * 0.45F
                        + 0.45F
                        + toolAccuracy
                        + heatAccuracy
                        + preheatAccuracy
                        + adjustment.recipeAccuracy()
                        + output.recipeAccuracyDelta(),
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
                        + freshnessPenalty,
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
                Math.max(output.enjoyment(), stats.enjoyment())
        );
    }

    public static IngredientStateData evaluatePlate(Level level, KitchenPlateRecipe recipe, List<ItemStack> inputs) {
        AggregateStats stats = aggregate(level, inputs);
        float plateAccuracy = Mth.clamp(stats.recipeAccuracy() * 0.65F + 0.24F + recipe.output().recipeAccuracyDelta(), 0.0F, 1.0F);

        return new IngredientStateData(
                recipe.output().state(),
                stats.createdTick(),
                Mth.clamp(stats.quality() * 0.42F + stats.freshness() * 0.16F + plateAccuracy * 0.22F + recipe.output().qualityBonus() + 0.08F, 0.05F, 1.0F),
                plateAccuracy,
                Mth.clamp(stats.flavor() + recipe.output().flavorDelta() + 0.03F, 0.0F, 1.0F),
                Mth.clamp(stats.texture() + recipe.output().textureDelta() + 0.05F, 0.0F, 1.0F),
                Mth.clamp(stats.structure() + recipe.output().structureDelta() + 0.03F, 0.0F, 1.0F),
                Mth.clamp(stats.moisture() + recipe.output().moistureDelta(), 0.0F, 1.0F),
                Mth.clamp(stats.purity() + recipe.output().purityDelta() + 0.02F, 0.0F, 1.0F),
                Mth.clamp(stats.aeration() + recipe.output().aerationDelta(), 0.0F, 1.0F),
                stats.maxDepth() + 1,
                Math.max(recipe.output().nourishment(), stats.nourishment()),
                Math.max(recipe.output().enjoyment(), stats.enjoyment())
        );
    }

    public static QualityBreakdown evaluateStack(ItemStack stack, Level level) {
        IngredientStateData data = KitchenStackUtil.getOrCreateData(stack, level.getGameTime());
        if (data == null) {
            return new QualityBreakdown(DishGrade.FAILED, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0, 0);
        }

        float freshness = KitchenStackUtil.currentFreshnessScore(stack, level);
        IngredientState effectiveState = KitchenStackUtil.effectiveState(stack, level.getGameTime());
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
}
