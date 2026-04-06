package com.boaat.jazzy_cookin.kitchen.sim.domain;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;

import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfile;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfiles;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class SimulationIngredientAnalysis {
    private final Map<Item, Integer> itemCounts = new IdentityHashMap<>();
    private final Map<FoodTrait, Integer> traitCounts = new EnumMap<>(FoodTrait.class);
    private final long createdTick;
    private final long traitMask;
    private final int totalItems;
    private final float totalWater;
    private final float totalFat;
    private final float totalProtein;
    private final float totalSeasoning;
    private final float totalCheese;
    private final float totalOnion;
    private final float totalHerb;
    private final float totalPepper;

    private SimulationIngredientAnalysis(
            long createdTick,
            long traitMask,
            int totalItems,
            float totalWater,
            float totalFat,
            float totalProtein,
            float totalSeasoning,
            float totalCheese,
            float totalOnion,
            float totalHerb,
            float totalPepper
    ) {
        this.createdTick = createdTick;
        this.traitMask = traitMask;
        this.totalItems = totalItems;
        this.totalWater = totalWater;
        this.totalFat = totalFat;
        this.totalProtein = totalProtein;
        this.totalSeasoning = totalSeasoning;
        this.totalCheese = totalCheese;
        this.totalOnion = totalOnion;
        this.totalHerb = totalHerb;
        this.totalPepper = totalPepper;
    }

    public static SimulationIngredientAnalysis analyzeInputs(StationSimulationAccess access) {
        long createdTick = access.simulationLevel() != null ? access.simulationLevel().getGameTime() : 0L;
        long traitMask = 0L;
        int totalItems = 0;
        float water = 0.0F;
        float fat = 0.0F;
        float protein = 0.0F;
        float seasoning = 0.0F;
        float cheese = 0.0F;
        float onion = 0.0F;
        float herb = 0.0F;
        float pepper = 0.0F;

        SimulationIngredientAnalysis analysis = new SimulationIngredientAnalysis(
                createdTick,
                0L,
                0,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F
        );

        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            int count = Math.max(1, stack.getCount());
            totalItems += count;
            analysis.itemCounts.merge(stack.getItem(), count, Integer::sum);

            FoodMatterData matter = access.simulationLevel() != null
                    ? KitchenStackUtil.getOrCreateFoodMatter(stack, access.simulationLevel().getGameTime())
                    : KitchenStackUtil.getFoodMatter(stack);
            FoodMaterialProfile profile = FoodMaterialProfiles.profileFor(stack).orElse(null);

            if (matter != null) {
                createdTick = Math.min(createdTick, matter.createdTick());
                traitMask |= matter.traitMask();
                water += matter.water() * count;
                fat += matter.fat() * count;
                protein += matter.protein() * count;
                seasoning += matter.seasoningLoad() * count;
                cheese += matter.cheeseLoad() * count;
                onion += matter.onionLoad() * count;
                herb += matter.herbLoad() * count;
                pepper += matter.pepperLoad() * count;
            } else if (profile != null) {
                traitMask |= profile.traitMask();
                water += profile.water() * count;
                fat += profile.fat() * count;
                protein += profile.protein() * count;
                seasoning += profile.seasoningLoad() * count;
                cheese += profile.cheeseLoad() * count;
                onion += profile.onionLoad() * count;
                herb += profile.herbLoad() * count;
                pepper += profile.pepperLoad() * count;
            }

            long sourceMask = matter != null ? matter.traitMask() : profile != null ? profile.traitMask() : 0L;
            for (FoodTrait trait : FoodTrait.unpack(sourceMask)) {
                analysis.traitCounts.merge(trait, count, Integer::sum);
            }
        }

        SimulationIngredientAnalysis populated = new SimulationIngredientAnalysis(
                createdTick,
                traitMask,
                totalItems,
                water,
                fat,
                protein,
                seasoning,
                cheese,
                onion,
                herb,
                pepper
        );
        populated.itemCounts.putAll(analysis.itemCounts);
        populated.traitCounts.putAll(analysis.traitCounts);
        return populated;
    }

    public boolean isEmpty() {
        return this.totalItems <= 0;
    }

    public long createdTick() {
        return this.createdTick;
    }

    public long traitMask() {
        return this.traitMask;
    }

    public int totalItems() {
        return this.totalItems;
    }

    public boolean has(Item item) {
        return this.itemCounts.containsKey(item);
    }

    public int count(Item item) {
        return this.itemCounts.getOrDefault(item, 0);
    }

    public boolean hasTrait(FoodTrait trait) {
        return this.traitCounts.getOrDefault(trait, 0) > 0;
    }

    public int traitCount(FoodTrait trait) {
        return this.traitCounts.getOrDefault(trait, 0);
    }

    public float avgWater() {
        return this.totalItems > 0 ? this.totalWater / this.totalItems : 0.0F;
    }

    public float avgFat() {
        return this.totalItems > 0 ? this.totalFat / this.totalItems : 0.0F;
    }

    public float avgProtein() {
        return this.totalItems > 0 ? this.totalProtein / this.totalItems : 0.0F;
    }

    public float avgSeasoning() {
        return this.totalItems > 0 ? this.totalSeasoning / this.totalItems : 0.0F;
    }

    public float avgCheese() {
        return this.totalItems > 0 ? this.totalCheese / this.totalItems : 0.0F;
    }

    public float avgOnion() {
        return this.totalItems > 0 ? this.totalOnion / this.totalItems : 0.0F;
    }

    public float avgHerb() {
        return this.totalItems > 0 ? this.totalHerb / this.totalItems : 0.0F;
    }

    public float avgPepper() {
        return this.totalItems > 0 ? this.totalPepper / this.totalItems : 0.0F;
    }
}
