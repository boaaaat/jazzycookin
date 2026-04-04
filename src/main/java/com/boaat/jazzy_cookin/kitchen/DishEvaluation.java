package com.boaat.jazzy_cookin.kitchen;

import java.util.List;

import com.boaat.jazzy_cookin.item.KitchenToolItem;
import com.boaat.jazzy_cookin.recipe.KitchenPlateRecipe;
import com.boaat.jazzy_cookin.recipe.KitchenProcessRecipe;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class DishEvaluation {
    private DishEvaluation() {
    }

    public static IngredientStateData evaluateProcess(
            Level level,
            KitchenProcessRecipe recipe,
            List<ItemStack> inputs,
            ItemStack toolStack,
            HeatLevel actualHeat,
            boolean preheated
    ) {
        long createdTick = 0L;
        float quality = 0.0F;
        float flavor = 0.0F;
        float texture = 0.0F;
        float structure = 0.0F;
        float moisture = 0.0F;
        float purity = 0.0F;
        float aeration = 0.0F;
        float freshness = 0.0F;
        int nourishment = 0;
        int enjoyment = 0;
        int counted = 0;

        for (ItemStack stack : inputs) {
            IngredientStateData data = KitchenStackUtil.getOrCreateData(stack, level.getGameTime());
            if (data == null) {
                continue;
            }

            createdTick += data.createdTick();
            quality += data.quality();
            flavor += data.flavor();
            texture += data.texture();
            structure += data.structure();
            moisture += data.moisture();
            purity += data.purity();
            aeration += data.aeration();
            freshness += KitchenStackUtil.currentFreshnessScore(stack, level);
            nourishment += data.nourishment();
            enjoyment += data.enjoyment();
            counted++;
        }

        if (counted == 0) {
            counted = 1;
            createdTick = level.getGameTime();
            quality = 0.55F;
            flavor = 0.45F;
            texture = 0.45F;
            structure = 0.4F;
            moisture = 0.45F;
            purity = 0.4F;
            aeration = 0.15F;
        }

        createdTick /= counted;
        quality /= counted;
        flavor /= counted;
        texture /= counted;
        structure /= counted;
        moisture /= counted;
        purity /= counted;
        aeration /= counted;
        freshness /= counted;
        nourishment /= counted;
        enjoyment /= counted;

        float toolBonus = 0.0F;
        float speedPenalty = 0.0F;
        if (recipe.preferredTool().isPresent()) {
            ToolProfile actualProfile = ToolProfile.fromStack(toolStack);
            if (actualProfile == recipe.preferredTool().get() && toolStack.getItem() instanceof KitchenToolItem toolItem) {
                toolBonus = toolItem.qualityBonus();
            } else if (actualProfile == ToolProfile.NONE) {
                toolBonus = -0.1F;
                speedPenalty = -0.05F;
            } else {
                toolBonus = -0.04F;
            }
        }

        float heatBonus = 0.0F;
        if (recipe.preferredHeat() != HeatLevel.OFF) {
            if (actualHeat == recipe.preferredHeat()) {
                heatBonus = 0.08F;
            } else if (actualHeat == HeatLevel.OFF) {
                heatBonus = -0.12F;
            } else {
                heatBonus = -0.06F;
            }
        }

        float preheatBonus = recipe.requiresPreheat() ? (preheated ? 0.06F : -0.07F) : 0.0F;
        float finalQuality = Mth.clamp(
                quality * 0.55F + freshness * 0.2F + recipe.output().qualityBonus() + toolBonus + heatBonus + preheatBonus + speedPenalty,
                0.05F,
                1.0F
        );

        return new IngredientStateData(
                recipe.output().state(),
                createdTick,
                finalQuality,
                Mth.clamp(flavor + recipe.output().flavorDelta() + heatBonus * 0.5F, 0.0F, 1.0F),
                Mth.clamp(texture + recipe.output().textureDelta() + toolBonus * 0.25F, 0.0F, 1.0F),
                Mth.clamp(structure + recipe.output().structureDelta(), 0.0F, 1.0F),
                Mth.clamp(moisture + recipe.output().moistureDelta(), 0.0F, 1.0F),
                Mth.clamp(purity + recipe.output().purityDelta(), 0.0F, 1.0F),
                Mth.clamp(aeration + recipe.output().aerationDelta(), 0.0F, 1.0F),
                counted + 1,
                Math.max(recipe.output().nourishment(), nourishment),
                Math.max(recipe.output().enjoyment(), enjoyment)
        );
    }

    public static IngredientStateData evaluatePlate(Level level, KitchenPlateRecipe recipe, List<ItemStack> inputs) {
        long createdTick = level.getGameTime();
        float quality = 0.0F;
        float freshness = 0.0F;
        float flavor = 0.0F;
        float texture = 0.0F;
        float structure = 0.0F;
        float moisture = 0.0F;
        float purity = 0.0F;
        float aeration = 0.0F;
        int nourishment = 0;
        int enjoyment = 0;
        int counted = 0;

        for (ItemStack stack : inputs) {
            IngredientStateData data = KitchenStackUtil.getOrCreateData(stack, level.getGameTime());
            if (data == null) {
                continue;
            }

            createdTick += data.createdTick();
            quality += data.quality();
            freshness += KitchenStackUtil.currentFreshnessScore(stack, level);
            flavor += data.flavor();
            texture += data.texture();
            structure += data.structure();
            moisture += data.moisture();
            purity += data.purity();
            aeration += data.aeration();
            nourishment += data.nourishment();
            enjoyment += data.enjoyment();
            counted++;
        }

        if (counted == 0) {
            counted = 1;
        }

        return new IngredientStateData(
                recipe.output().state(),
                createdTick / counted,
                Mth.clamp(quality / counted + recipe.output().qualityBonus() + 0.08F, 0.05F, 1.0F),
                Mth.clamp(flavor / counted + recipe.output().flavorDelta() + 0.02F, 0.0F, 1.0F),
                Mth.clamp(texture / counted + recipe.output().textureDelta() + 0.04F, 0.0F, 1.0F),
                Mth.clamp(structure / counted + recipe.output().structureDelta(), 0.0F, 1.0F),
                Mth.clamp(moisture / counted + recipe.output().moistureDelta(), 0.0F, 1.0F),
                Mth.clamp(purity / counted + recipe.output().purityDelta(), 0.0F, 1.0F),
                Mth.clamp(aeration / counted + recipe.output().aerationDelta(), 0.0F, 1.0F),
                counted + 1,
                Math.max(recipe.output().nourishment(), nourishment / counted),
                Math.max(recipe.output().enjoyment(), enjoyment / counted)
        );
    }

    public static QualityBreakdown evaluateStack(ItemStack stack, Level level) {
        IngredientStateData data = KitchenStackUtil.getOrCreateData(stack, level.getGameTime());
        if (data == null) {
            return new QualityBreakdown(DishGrade.FAILED, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0, 0);
        }

        float freshness = KitchenStackUtil.currentFreshnessScore(stack, level);
        float prep = (data.texture() + data.purity()) * 0.5F;
        float cooking = (data.flavor() + data.moisture()) * 0.5F;
        float finishing = data.processDepth() >= 4 ? 0.85F : 0.45F;
        float plating = data.state() == IngredientState.PLATED_SLICE ? 1.0F : 0.3F;
        float total = Mth.clamp(
                data.quality() * 0.35F + freshness * 0.15F + prep * 0.15F + cooking * 0.2F + finishing * 0.1F + plating * 0.05F,
                0.0F,
                1.0F
        );

        return new QualityBreakdown(
                DishGrade.fromScore(total),
                total,
                data.quality(),
                freshness,
                prep,
                cooking,
                finishing,
                plating,
                data.nourishment(),
                data.enjoyment()
        );
    }
}
