package com.boaat.jazzy_cookin.kitchen.sim.domain;

import java.util.function.UnaryOperator;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class SimulationOutputFactory {
    private SimulationOutputFactory() {
    }

    public static ItemStack createOutput(
            KitchenIngredientItem item,
            long gameTime,
            SimulationIngredientAnalysis analysis,
            UnaryOperator<FoodMatterData> adjuster
    ) {
        ItemStack output = item.createStack(1, analysis.createdTick() > 0L ? analysis.createdTick() : gameTime);
        FoodMatterData matter = KitchenStackUtil.getOrCreateFoodMatter(output, gameTime);
        if (matter != null) {
            FoodMatterData adjusted = matter.withAddedTraits(analysis.traitMask());
            if (adjuster != null) {
                adjusted = adjuster.apply(adjusted);
            }
            KitchenStackUtil.setFoodMatter(output, adjusted, gameTime);
        }
        return legalOutput(output, gameTime);
    }

    public static ItemStack createOutput(
            KitchenIngredientItem item,
            long gameTime,
            SimulationIngredientAnalysis analysis,
            FoodMatterData targetMatter
    ) {
        ItemStack output = item.createStack(1, analysis.createdTick() > 0L ? analysis.createdTick() : gameTime);
        FoodMatterData seeded = KitchenStackUtil.getOrCreateFoodMatter(output, gameTime);
        if (seeded != null && targetMatter != null) {
            FoodMatterData blended = new FoodMatterData(
                    Math.min(seeded.createdTick(), targetMatter.createdTick()),
                    targetMatter.summaryHint(),
                    seeded.traitMask() | targetMatter.traitMask() | analysis.traitMask(),
                    Mth.lerp(0.30F, seeded.surfaceTempC(), targetMatter.surfaceTempC()),
                    Mth.lerp(0.30F, seeded.coreTempC(), targetMatter.coreTempC()),
                    Mth.lerp(0.35F, seeded.water(), targetMatter.water()),
                    Mth.lerp(0.35F, seeded.fat(), targetMatter.fat()),
                    Mth.lerp(0.35F, seeded.protein(), targetMatter.protein()),
                    Mth.lerp(0.30F, seeded.aeration(), targetMatter.aeration()),
                    Mth.lerp(0.30F, seeded.fragmentation(), targetMatter.fragmentation()),
                    Mth.lerp(0.30F, seeded.cohesiveness(), targetMatter.cohesiveness()),
                    Mth.lerp(0.25F, seeded.proteinSet(), targetMatter.proteinSet()),
                    Mth.lerp(0.25F, seeded.browning(), targetMatter.browning()),
                    Mth.lerp(0.20F, seeded.charLevel(), targetMatter.charLevel()),
                    Mth.lerp(0.30F, seeded.seasoningLoad(), targetMatter.seasoningLoad()),
                    Mth.lerp(0.30F, seeded.cheeseLoad(), targetMatter.cheeseLoad()),
                    Mth.lerp(0.30F, seeded.onionLoad(), targetMatter.onionLoad()),
                    Mth.lerp(0.30F, seeded.herbLoad(), targetMatter.herbLoad()),
                    Mth.lerp(0.30F, seeded.pepperLoad(), targetMatter.pepperLoad()),
                    targetMatter.preservationLevel(),
                    targetMatter.oxidation(),
                    targetMatter.microbialLoad(),
                    targetMatter.whiskWork(),
                    targetMatter.stirCount(),
                    targetMatter.flipCount(),
                    targetMatter.timeInPan(),
                    Math.max(seeded.processDepth(), targetMatter.processDepth()),
                    targetMatter.finalizedServing()
            ).clamp();
            KitchenStackUtil.setFoodMatter(output, blended, gameTime);
        }
        return legalOutput(output, gameTime);
    }

    private static ItemStack legalOutput(ItemStack output, long gameTime) {
        IngredientStateData data = KitchenStackUtil.getOrCreateData(output, gameTime);
        if (data != null && !KitchenStackUtil.isStateAllowed(output, data.state(), gameTime)) {
            return ItemStack.EMPTY;
        }
        return output;
    }
}
