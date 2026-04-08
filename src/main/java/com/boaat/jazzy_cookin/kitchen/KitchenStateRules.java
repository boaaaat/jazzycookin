package com.boaat.jazzy_cookin.kitchen;

import java.util.EnumSet;
import java.util.Set;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.item.KitchenMealItem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class KitchenStateRules {
    private static final Set<IngredientState> DOUGH_STATES = EnumSet.of(
            IngredientState.DOUGH,
            IngredientState.SHAGGY_DOUGH,
            IngredientState.ROUGH_DOUGH,
            IngredientState.DEVELOPING_DOUGH,
            IngredientState.DEVELOPED_DOUGH,
            IngredientState.SMOOTH_DOUGH,
            IngredientState.ELASTIC_DOUGH,
            IngredientState.OVERWORKED_DOUGH
    );
    private static final Set<IngredientState> PIE_STATES = EnumSet.of(
            IngredientState.RAW_ASSEMBLED_PIE,
            IngredientState.BAKED_PIE,
            IngredientState.COOLED_PIE,
            IngredientState.SLICED_PIE,
            IngredientState.PLATED_SLICE
    );
    private static final Set<IngredientState> SMOKED_MEAT_STATES = EnumSet.of(
            IngredientState.SMOKED,
            IngredientState.RESTED,
            IngredientState.PLATED_ROAST_MEAL
    );
    private static final Set<IngredientState> JUICE_STATES = EnumSet.of(
            IngredientState.FRESH_JUICE,
            IngredientState.BITTER_JUICE
    );
    private static final Set<IngredientState> BLEND_STATES = EnumSet.of(
            IngredientState.SMOOTH,
            IngredientState.CREAMY,
            IngredientState.SEPARATED_BLEND
    );
    private static final Set<IngredientState> PRODUCE_BLEND_STATES = EnumSet.of(
            IngredientState.ROUGH_CUT,
            IngredientState.CHOPPED,
            IngredientState.DICED
    );

    private KitchenStateRules() {
    }

    public static float stateMatchScore(ItemStack stack, IngredientState requiredState, long gameTime) {
        if (stack.isEmpty()) {
            return 0.0F;
        }

        IngredientState actualState = KitchenStackUtil.effectiveState(stack, gameTime);
        if (actualState == requiredState) {
            return 1.0F;
        }

        if (requiredState == IngredientState.PANTRY_READY) {
            return !actualState.isSpoiledState() && StorageRules.canStore(StorageType.PANTRY, stack) ? 1.0F : 0.0F;
        }

        Integer requiredStage = produceStage(requiredState, stack);
        Integer actualStage = produceStage(actualState, stack);
        if (requiredStage != null && actualStage != null) {
            int distance = Math.abs(requiredStage - actualStage);
            return switch (distance) {
                case 0 -> 1.0F;
                case 1 -> 0.86F;
                case 2 -> 0.72F;
                case 3 -> 0.58F;
                default -> 0.0F;
            };
        }

        return 0.0F;
    }

    public static boolean isStateAllowed(ItemStack stack, IngredientState targetState, long gameTime) {
        if (stack.isEmpty() || targetState.isSpoiledState()) {
            return true;
        }

        IngredientState actualState = KitchenStackUtil.effectiveState(stack, gameTime);
        if (actualState == targetState) {
            return true;
        }

        if (stack.getItem() instanceof KitchenMealItem) {
            return targetState.isPlatedState();
        }

        if (!(stack.getItem() instanceof KitchenIngredientItem ingredientItem)) {
            return false;
        }

        String itemPath = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (isProduceItem(itemPath)) {
            return produceStage(targetState, stack) != null;
        }

        if ("chopped_produce_blend".equals(itemPath)) {
            return PRODUCE_BLEND_STATES.contains(targetState);
        }
        if ("pie_dough".equals(itemPath)) {
            return DOUGH_STATES.contains(targetState);
        }
        if ("assembled_tray_pie".equals(itemPath)) {
            return PIE_STATES.contains(targetState);
        }
        if ("smoked_meat".equals(itemPath)) {
            return SMOKED_MEAT_STATES.contains(targetState);
        }
        if ("lemon_juice".equals(itemPath) || "jarred_lemon_juice".equals(itemPath) || "mixed_juice".equals(itemPath)) {
            return JUICE_STATES.contains(targetState);
        }
        if ("fruit_juice_blend".equals(itemPath)) {
            return BLEND_STATES.contains(targetState);
        }

        return targetState == ingredientItem.defaultState();
    }

    public static boolean isFinishedUnplatedState(IngredientState state) {
        return switch (state) {
            case RESTED,
                    COOLED,
                    COOLED_PIE,
                    RESTED_PIE,
                    SLICED_PIE,
                    RESTED_BREAD,
                    SLICED_BREAD,
                    FRESH_JUICE,
                    SMOOTH,
                    CREAMY,
                    SMOKED,
                    BAKED_BREAD,
                    BAKED_PIE,
                    ROASTED_PROTEIN,
                    FRIED_PROTEIN,
                    BROILED_PROTEIN -> true;
            default -> false;
        };
    }

    private static Integer produceStage(IngredientState state, ItemStack stack) {
        if (!isProduceLikeState(state, stack)) {
            return null;
        }
        return switch (state) {
            case APPLES, CARROTS, LEMONS -> 0;
            case SLICED_APPLE, SLICED -> 1;
            case ROUGH_CUT -> 2;
            case CHOPPED -> 3;
            case DICED -> 4;
            case MINCED -> 5;
            default -> null;
        };
    }

    private static boolean isProduceLikeState(IngredientState state, ItemStack stack) {
        if (!isProduceItem(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()) && !stack.is(com.boaat.jazzy_cookin.registry.JazzyItems.CHOPPED_PRODUCE_BLEND.get())) {
            return false;
        }
        return switch (state) {
            case APPLES, CARROTS, LEMONS, SLICED_APPLE, SLICED, ROUGH_CUT, CHOPPED, DICED, MINCED -> true;
            default -> false;
        };
    }

    private static boolean isProduceItem(String itemPath) {
        return "apples".equals(itemPath) || "carrots".equals(itemPath) || "lemons".equals(itemPath);
    }
}
