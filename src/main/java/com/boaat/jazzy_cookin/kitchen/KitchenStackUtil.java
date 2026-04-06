package com.boaat.jazzy_cookin.kitchen;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.item.KitchenMealItem;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.registry.JazzyDataComponents;
import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class KitchenStackUtil {
    private KitchenStackUtil() {
    }

    public static IngredientStateData getData(ItemStack stack) {
        IngredientStateData existing = stack.get(JazzyDataComponents.INGREDIENT_STATE.get());
        if (existing != null) {
            return existing;
        }

        FoodMatterData matter = getFoodMatter(stack);
        if (matter == null) {
            return null;
        }

        IngredientStateData derived = deriveIngredientState(stack, matter, matter.createdTick());
        stack.set(JazzyDataComponents.INGREDIENT_STATE.get(), derived);
        return derived;
    }

    public static IngredientStateData getOrCreateData(ItemStack stack, long gameTime) {
        IngredientStateData existing = stack.get(JazzyDataComponents.INGREDIENT_STATE.get());
        if (existing != null && existing.createdTick() >= Long.MAX_VALUE / 8L) {
            FoodMatterData matter = getFoodMatter(stack);
            if (matter == null && stack.getItem() instanceof KitchenIngredientItem) {
                setFoodMatter(stack, FoodMatterData.fromLegacy(existing, isFinalizedServing(stack)), existing.createdTick());
            } else if (matter != null) {
                applyStackBehavior(stack, matter);
            }
            return existing;
        }

        FoodMatterData matter = getOrCreateFoodMatter(stack, gameTime);
        if (matter == null) {
            if (existing != null) {
                return existing;
            }
            if (stack.getItem() instanceof KitchenIngredientItem ingredientItem) {
                IngredientStateData created = ingredientItem.defaultData(gameTime);
                stack.set(JazzyDataComponents.INGREDIENT_STATE.get(), created);
                return created;
            }
            return null;
        }

        IngredientStateData derived = deriveIngredientState(stack, matter, gameTime);
        stack.set(JazzyDataComponents.INGREDIENT_STATE.get(), derived);
        applyStackBehavior(stack, matter);
        return derived;
    }

    public static void setData(ItemStack stack, IngredientStateData data) {
        stack.set(JazzyDataComponents.INGREDIENT_STATE.get(), data);
        FoodMatterData existingMatter = getFoodMatter(stack);
        if (existingMatter == null) {
            FoodMatterData created = FoodMatterData.fromLegacy(data, isFinalizedServing(stack));
            setFoodMatter(stack, created, data.createdTick());
        }
    }

    public static FoodMatterData getFoodMatter(ItemStack stack) {
        return stack.get(JazzyDataComponents.FOOD_MATTER.get());
    }

    public static FoodMatterData getOrCreateFoodMatter(ItemStack stack, long gameTime) {
        FoodMatterData existing = getFoodMatter(stack);
        if (existing != null) {
            return existing;
        }

        if (!(stack.getItem() instanceof KitchenIngredientItem ingredientItem)) {
            return null;
        }

        IngredientStateData legacy = stack.get(JazzyDataComponents.INGREDIENT_STATE.get());
        FoodMatterData created = legacy != null
                ? FoodMatterData.fromLegacy(legacy, isFinalizedServing(stack))
                : FoodMatterData.fromLegacy(ingredientItem.defaultData(gameTime), isFinalizedServing(stack));
        setFoodMatter(stack, created, gameTime);
        return created;
    }

    public static void setFoodMatter(ItemStack stack, FoodMatterData matter, long gameTime) {
        if (matter == null) {
            stack.remove(JazzyDataComponents.FOOD_MATTER.get());
            stack.remove(DataComponents.MAX_STACK_SIZE);
            return;
        }

        FoodMatterData clamped = matter.clamp();
        stack.set(JazzyDataComponents.FOOD_MATTER.get(), clamped);
        stack.set(JazzyDataComponents.INGREDIENT_STATE.get(), deriveIngredientState(stack, clamped, gameTime));
        applyStackBehavior(stack, clamped);
    }

    public static void initializeStack(ItemStack stack, IngredientStateData data, FoodMatterData matter, long gameTime) {
        if (data != null) {
            stack.set(JazzyDataComponents.INGREDIENT_STATE.get(), data);
        } else {
            stack.remove(JazzyDataComponents.INGREDIENT_STATE.get());
        }
        setFoodMatter(stack, matter, gameTime);
    }

    public static boolean isWorkedFood(ItemStack stack) {
        FoodMatterData matter = getFoodMatter(stack);
        return matter != null && matter.isWorkedButUnfinished();
    }

    public static boolean isEggSimulationItem(ItemStack stack) {
        return stack.is(JazzyItems.EGG_MIXTURE.get())
                || stack.is(JazzyItems.SOFT_SCRAMBLED_EGGS.get())
                || stack.is(JazzyItems.SCRAMBLED_EGGS.get())
                || stack.is(JazzyItems.OMELET.get())
                || stack.is(JazzyItems.BROWNED_OMELET.get())
                || stack.is(JazzyItems.BURNT_EGGS.get());
    }

    public static boolean matchesState(ItemStack stack, IngredientState state, long gameTime) {
        return effectiveState(stack, gameTime) == state;
    }

    public static float currentFreshnessScore(ItemStack stack, Level level) {
        return currentFreshnessScore(stack, level.getGameTime());
    }

    public static float currentFreshnessScore(ItemStack stack, long gameTime) {
        IngredientStateData data = getOrCreateData(stack, gameTime);
        if (data == null) {
            return 0.0F;
        }

        long decayTicks = decayTicks(stack);
        if (decayTicks <= 0L || decayTicks >= Long.MAX_VALUE / 4L) {
            return 1.0F;
        }

        long age = Math.max(0L, gameTime - data.createdTick());
        float ratio = age / (float) decayTicks;
        return Mth.clamp(1.0F - ratio, 0.0F, 1.0F);
    }

    public static FreshnessBand freshnessBand(ItemStack stack, Level level) {
        return freshnessBand(stack, level.getGameTime());
    }

    public static FreshnessBand freshnessBand(ItemStack stack, long gameTime) {
        return FreshnessBand.fromScore(currentFreshnessScore(stack, gameTime));
    }

    public static IngredientState effectiveState(ItemStack stack, long gameTime) {
        IngredientStateData data = getOrCreateData(stack, gameTime);
        if (data == null) {
            return IngredientState.PANTRY_READY;
        }

        return switch (freshnessBand(stack, gameTime)) {
            case SPOILED -> IngredientState.SPOILED;
            case MOLDY -> IngredientState.MOLDY;
            default -> data.state();
        };
    }

    public static String freshnessLabel(ItemStack stack, Level level) {
        return freshnessBand(stack, level).getSerializedName();
    }

    public static long decayTicks(ItemStack stack) {
        if (stack.getItem() instanceof KitchenIngredientItem ingredientItem) {
            return ingredientItem.decayTicks();
        }
        return Long.MAX_VALUE / 4L;
    }

    private static IngredientStateData deriveIngredientState(ItemStack stack, FoodMatterData matter, long gameTime) {
        IngredientStateData fallback = stack.get(JazzyDataComponents.INGREDIENT_STATE.get());
        if (fallback == null && stack.getItem() instanceof KitchenIngredientItem ingredientItem) {
            fallback = ingredientItem.defaultData(gameTime);
        }

        if (fallback == null) {
            fallback = new IngredientStateData(
                    IngredientState.PANTRY_READY,
                    gameTime,
                    0.70F,
                    0.72F,
                    0.50F,
                    0.40F,
                    0.36F,
                    0.42F,
                    0.72F,
                    0.12F,
                    0,
                    0,
                    0
            );
        }

        IngredientState summaryState = summaryStateFor(stack, fallback.state(), matter);
        float quality = Mth.clamp(
                0.35F
                        + matter.proteinSet() * 0.18F
                        + matter.cohesiveness() * 0.14F
                        + (1.0F - matter.charLevel()) * 0.14F
                        + matter.aeration() * 0.08F
                        + (1.0F - Math.abs(matter.water() - 0.52F)) * 0.16F
                        + (matter.cheeseLoad() + matter.herbLoad()) * 0.05F,
                0.05F,
                1.0F
        );
        float recipeAccuracy = matter.finalizedServing()
                ? Mth.clamp(0.62F + matter.proteinSet() * 0.18F - matter.charLevel() * 0.10F, 0.0F, 1.0F)
                : Mth.clamp(0.56F + matter.whiskWork() * 0.24F + matter.cohesiveness() * 0.08F, 0.0F, 1.0F);
        float flavor = Mth.clamp(
                0.36F
                        + matter.seasoningLoad() * 0.18F
                        + matter.cheeseLoad() * 0.10F
                        + matter.onionLoad() * 0.08F
                        + matter.herbLoad() * 0.08F
                        + matter.pepperLoad() * 0.06F
                        + matter.browning() * 0.12F
                        - matter.charLevel() * 0.22F,
                0.0F,
                1.0F
        );
        float texture = Mth.clamp(
                0.22F
                        + matter.aeration() * 0.18F
                        + matter.proteinSet() * 0.24F
                        + matter.fragmentation() * 0.10F
                        + matter.browning() * 0.10F
                        - matter.charLevel() * 0.24F,
                0.0F,
                1.0F
        );
        float structure = Mth.clamp(
                0.18F
                        + matter.cohesiveness() * 0.44F
                        + matter.proteinSet() * 0.18F
                        - matter.fragmentation() * 0.14F,
                0.0F,
                1.0F
        );
        float moisture = Mth.clamp(matter.water(), 0.0F, 1.0F);
        float purity = Mth.clamp(0.72F + matter.whiskWork() * 0.10F - matter.onionLoad() * 0.05F, 0.0F, 1.0F);
        float aeration = Mth.clamp(matter.aeration(), 0.0F, 1.0F);
        int nourishment = Math.max(fallback.nourishment(), Math.max(1, Math.round(matter.protein() * 8.0F + matter.fat() * 4.0F)));
        int enjoyment = Math.max(
                fallback.enjoyment(),
                Math.max(1, Math.round(3.0F + flavor * 3.0F + texture * 2.0F + matter.browning() - matter.charLevel() * 3.0F))
        );

        return new IngredientStateData(
                summaryState,
                matter.createdTick(),
                quality,
                recipeAccuracy,
                flavor,
                texture,
                structure,
                moisture,
                purity,
                aeration,
                Math.max(fallback.processDepth(), matter.processDepth()),
                nourishment,
                enjoyment
        );
    }

    private static IngredientState summaryStateFor(ItemStack stack, IngredientState fallbackState, FoodMatterData matter) {
        if (stack.is(JazzyItems.EGG_MIXTURE.get())) {
            return IngredientState.SMOOTH_MIXTURE;
        }
        if (stack.is(JazzyItems.SOFT_SCRAMBLED_EGGS.get())
                || stack.is(JazzyItems.SCRAMBLED_EGGS.get())
                || stack.is(JazzyItems.OMELET.get())
                || stack.is(JazzyItems.BROWNED_OMELET.get())
                || stack.is(JazzyItems.BURNT_EGGS.get())) {
            return IngredientState.PAN_FRIED;
        }
        if (matter.finalizedServing() && stack.getItem() instanceof KitchenMealItem) {
            return IngredientState.PLATED;
        }
        return fallbackState;
    }

    private static void applyStackBehavior(ItemStack stack, FoodMatterData matter) {
        if (matter.isWorkedButUnfinished()) {
            stack.set(DataComponents.MAX_STACK_SIZE, 1);
        } else {
            stack.remove(DataComponents.MAX_STACK_SIZE);
        }
    }

    private static boolean isFinalizedServing(ItemStack stack) {
        return stack.getItem() instanceof KitchenMealItem
                || stack.is(JazzyItems.SOFT_SCRAMBLED_EGGS.get())
                || stack.is(JazzyItems.SCRAMBLED_EGGS.get())
                || stack.is(JazzyItems.OMELET.get())
                || stack.is(JazzyItems.BROWNED_OMELET.get())
                || stack.is(JazzyItems.BURNT_EGGS.get());
    }
}
