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
    private static final float AMBIENT_TEMP_C = 22.0F;

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
    private final float totalSurfaceTempC;
    private final float totalCoreTempC;
    private final float totalAeration;
    private final float totalFragmentation;
    private final float totalCohesiveness;
    private final float totalProteinSet;
    private final float totalBrowning;
    private final float totalCharLevel;
    private final float totalPreservation;
    private final float totalOxidation;
    private final float totalMicrobialLoad;
    private final float totalWhiskWork;
    private final float totalProcessDepth;
    private final int totalStirCount;
    private final int totalFlipCount;
    private final int totalTimeInPan;
    private final int totalFinalizedServing;

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
            float totalPepper,
            float totalSurfaceTempC,
            float totalCoreTempC,
            float totalAeration,
            float totalFragmentation,
            float totalCohesiveness,
            float totalProteinSet,
            float totalBrowning,
            float totalCharLevel,
            float totalPreservation,
            float totalOxidation,
            float totalMicrobialLoad,
            float totalWhiskWork,
            float totalProcessDepth,
            int totalStirCount,
            int totalFlipCount,
            int totalTimeInPan,
            int totalFinalizedServing
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
        this.totalSurfaceTempC = totalSurfaceTempC;
        this.totalCoreTempC = totalCoreTempC;
        this.totalAeration = totalAeration;
        this.totalFragmentation = totalFragmentation;
        this.totalCohesiveness = totalCohesiveness;
        this.totalProteinSet = totalProteinSet;
        this.totalBrowning = totalBrowning;
        this.totalCharLevel = totalCharLevel;
        this.totalPreservation = totalPreservation;
        this.totalOxidation = totalOxidation;
        this.totalMicrobialLoad = totalMicrobialLoad;
        this.totalWhiskWork = totalWhiskWork;
        this.totalProcessDepth = totalProcessDepth;
        this.totalStirCount = totalStirCount;
        this.totalFlipCount = totalFlipCount;
        this.totalTimeInPan = totalTimeInPan;
        this.totalFinalizedServing = totalFinalizedServing;
    }

    public static SimulationIngredientAnalysis analyzeInputs(StationSimulationAccess access) {
        long fallbackGameTime = access.simulationLevel() != null ? access.simulationLevel().getGameTime() : 0L;
        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            stacks.add(access.simulationItem(slot));
        }
        return analyzeStacks(stacks, fallbackGameTime);
    }

    public static SimulationIngredientAnalysis analyzeStacks(Iterable<ItemStack> stacks, long fallbackGameTime) {
        long createdTick = fallbackGameTime;
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
        float surfaceTempC = 0.0F;
        float coreTempC = 0.0F;
        float aeration = 0.0F;
        float fragmentation = 0.0F;
        float cohesiveness = 0.0F;
        float proteinSet = 0.0F;
        float browning = 0.0F;
        float charLevel = 0.0F;
        float preservation = 0.0F;
        float oxidation = 0.0F;
        float microbialLoad = 0.0F;
        float whiskWork = 0.0F;
        float processDepth = 0.0F;
        int stirCount = 0;
        int flipCount = 0;
        int timeInPan = 0;
        int finalizedServing = 0;

        Map<Item, Integer> itemCounts = new IdentityHashMap<>();
        Map<FoodTrait, Integer> traitCounts = new EnumMap<>(FoodTrait.class);

        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            int count = Math.max(1, stack.getCount());
            totalItems += count;
            itemCounts.merge(stack.getItem(), count, Integer::sum);

            FoodMatterData matter = KitchenStackUtil.getOrCreateFoodMatter(stack, fallbackGameTime);
            FoodMaterialProfile profile = FoodMaterialProfiles.profileFor(stack).orElse(null);
            if (matter != null) {
                createdTick = createdTick > 0L ? Math.min(createdTick, matter.createdTick()) : matter.createdTick();
                traitMask |= matter.traitMask();
                water += matter.water() * count;
                fat += matter.fat() * count;
                protein += matter.protein() * count;
                seasoning += matter.seasoningLoad() * count;
                cheese += matter.cheeseLoad() * count;
                onion += matter.onionLoad() * count;
                herb += matter.herbLoad() * count;
                pepper += matter.pepperLoad() * count;
                surfaceTempC += matter.surfaceTempC() * count;
                coreTempC += matter.coreTempC() * count;
                aeration += matter.aeration() * count;
                fragmentation += matter.fragmentation() * count;
                cohesiveness += matter.cohesiveness() * count;
                proteinSet += matter.proteinSet() * count;
                browning += matter.browning() * count;
                charLevel += matter.charLevel() * count;
                preservation += matter.preservationLevel() * count;
                oxidation += matter.oxidation() * count;
                microbialLoad += matter.microbialLoad() * count;
                whiskWork += matter.whiskWork() * count;
                processDepth += matter.processDepth() * count;
                stirCount += matter.stirCount() * count;
                flipCount += matter.flipCount() * count;
                timeInPan += matter.timeInPan() * count;
                if (matter.finalizedServing()) {
                    finalizedServing += count;
                }
            } else {
                if (createdTick <= 0L) {
                    createdTick = fallbackGameTime;
                }
                surfaceTempC += AMBIENT_TEMP_C * count;
                coreTempC += AMBIENT_TEMP_C * count;
                if (profile != null) {
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
            }

            long sourceMask = matter != null ? matter.traitMask() : profile != null ? profile.traitMask() : 0L;
            for (FoodTrait trait : FoodTrait.unpack(sourceMask)) {
                traitCounts.merge(trait, count, Integer::sum);
            }
        }

        SimulationIngredientAnalysis analysis = new SimulationIngredientAnalysis(
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
                pepper,
                surfaceTempC,
                coreTempC,
                aeration,
                fragmentation,
                cohesiveness,
                proteinSet,
                browning,
                charLevel,
                preservation,
                oxidation,
                microbialLoad,
                whiskWork,
                processDepth,
                stirCount,
                flipCount,
                timeInPan,
                finalizedServing
        );
        analysis.itemCounts.putAll(itemCounts);
        analysis.traitCounts.putAll(traitCounts);
        return analysis;
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
        return average(this.totalWater);
    }

    public float avgFat() {
        return average(this.totalFat);
    }

    public float avgProtein() {
        return average(this.totalProtein);
    }

    public float avgSeasoning() {
        return average(this.totalSeasoning);
    }

    public float avgCheese() {
        return average(this.totalCheese);
    }

    public float avgOnion() {
        return average(this.totalOnion);
    }

    public float avgHerb() {
        return average(this.totalHerb);
    }

    public float avgPepper() {
        return average(this.totalPepper);
    }

    public float avgSurfaceTempC() {
        return averageOr(this.totalSurfaceTempC, AMBIENT_TEMP_C);
    }

    public float avgCoreTempC() {
        return averageOr(this.totalCoreTempC, AMBIENT_TEMP_C);
    }

    public float avgAerationCarry() {
        return average(this.totalAeration);
    }

    public float avgFragmentationCarry() {
        return average(this.totalFragmentation);
    }

    public float avgCohesivenessCarry() {
        return average(this.totalCohesiveness);
    }

    public float avgProteinSetCarry() {
        return average(this.totalProteinSet);
    }

    public float avgBrowningCarry() {
        return average(this.totalBrowning);
    }

    public float avgCharLevelCarry() {
        return average(this.totalCharLevel);
    }

    public float avgPreservation() {
        return average(this.totalPreservation);
    }

    public float avgOxidation() {
        return average(this.totalOxidation);
    }

    public float avgMicrobialLoad() {
        return average(this.totalMicrobialLoad);
    }

    public float avgWhiskWork() {
        return average(this.totalWhiskWork);
    }

    public float avgProcessDepth() {
        return average(this.totalProcessDepth);
    }

    public float avgStirCount() {
        return average(this.totalStirCount);
    }

    public float avgFlipCount() {
        return average(this.totalFlipCount);
    }

    public float avgTimeInPan() {
        return average(this.totalTimeInPan);
    }

    public float finalizedServingRatio() {
        return average(this.totalFinalizedServing);
    }

    private float average(float total) {
        return this.totalItems > 0 ? total / this.totalItems : 0.0F;
    }

    private float average(int total) {
        return this.totalItems > 0 ? total / (float) this.totalItems : 0.0F;
    }

    private float averageOr(float total, float fallback) {
        return this.totalItems > 0 ? total / this.totalItems : fallback;
    }
}
