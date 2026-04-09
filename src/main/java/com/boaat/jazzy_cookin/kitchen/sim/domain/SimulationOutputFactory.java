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
            float carryWeight = Mth.clamp(
                    0.58F
                            + targetMatter.processDepth() * 0.06F
                            + (targetMatter.finalizedServing() ? 0.08F : 0.0F),
                    0.58F,
                    0.90F
            );
            float thermalWeight = Mth.clamp(carryWeight + 0.06F, 0.62F, 0.94F);
            float processWeight = Mth.clamp(carryWeight + 0.10F, 0.68F, 0.96F);
            FoodMatterData blended = new FoodMatterData(
                    Math.min(seeded.createdTick(), targetMatter.createdTick()),
                    targetMatter.summaryHint(),
                    seeded.traitMask() | targetMatter.traitMask() | analysis.traitMask(),
                    Mth.lerp(thermalWeight, seeded.surfaceTempC(), targetMatter.surfaceTempC()),
                    Mth.lerp(thermalWeight, seeded.coreTempC(), targetMatter.coreTempC()),
                    Mth.lerp(carryWeight, seeded.water(), targetMatter.water()),
                    Mth.lerp(carryWeight, seeded.fat(), targetMatter.fat()),
                    Mth.lerp(carryWeight, seeded.protein(), targetMatter.protein()),
                    Mth.lerp(carryWeight, seeded.aeration(), targetMatter.aeration()),
                    Mth.lerp(carryWeight, seeded.fragmentation(), targetMatter.fragmentation()),
                    Mth.lerp(carryWeight, seeded.cohesiveness(), targetMatter.cohesiveness()),
                    Mth.lerp(carryWeight, seeded.proteinSet(), targetMatter.proteinSet()),
                    Mth.lerp(carryWeight, seeded.browning(), targetMatter.browning()),
                    Mth.lerp(carryWeight, seeded.charLevel(), targetMatter.charLevel()),
                    Mth.lerp(carryWeight, seeded.seasoningLoad(), targetMatter.seasoningLoad()),
                    Mth.lerp(carryWeight, seeded.cheeseLoad(), targetMatter.cheeseLoad()),
                    Mth.lerp(carryWeight, seeded.onionLoad(), targetMatter.onionLoad()),
                    Mth.lerp(carryWeight, seeded.herbLoad(), targetMatter.herbLoad()),
                    Mth.lerp(carryWeight, seeded.pepperLoad(), targetMatter.pepperLoad()),
                    Mth.lerp(carryWeight, seeded.preservationLevel(), targetMatter.preservationLevel()),
                    Mth.lerp(carryWeight, seeded.oxidation(), targetMatter.oxidation()),
                    Mth.lerp(carryWeight, seeded.microbialLoad(), targetMatter.microbialLoad()),
                    Mth.lerp(processWeight, seeded.whiskWork(), targetMatter.whiskWork()),
                    Math.max(seeded.stirCount(), targetMatter.stirCount()),
                    Math.max(seeded.flipCount(), targetMatter.flipCount()),
                    Math.max(seeded.timeInPan(), targetMatter.timeInPan()),
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
            FoodMatterData matter = KitchenStackUtil.getFoodMatter(output);
            if (matter != null && (matter.processDepth() > 0 || matter.finalizedServing())) {
                return output;
            }
            return ItemStack.EMPTY;
        }
        return output;
    }
}
