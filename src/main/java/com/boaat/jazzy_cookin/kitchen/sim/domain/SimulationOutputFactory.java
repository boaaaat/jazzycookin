package com.boaat.jazzy_cookin.kitchen.sim.domain;

import java.util.function.UnaryOperator;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;

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
        return output;
    }
}
