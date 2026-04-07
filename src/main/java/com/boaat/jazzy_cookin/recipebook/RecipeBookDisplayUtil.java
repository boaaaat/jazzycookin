package com.boaat.jazzy_cookin.recipebook;

import java.util.ArrayList;
import java.util.List;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.boaat.jazzy_cookin.recipe.KitchenInputRequirement;
import com.boaat.jazzy_cookin.recipe.KitchenProcessOutput;
import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class RecipeBookDisplayUtil {
    private RecipeBookDisplayUtil() {
    }

    public static List<ItemStack> displayStacks(KitchenInputRequirement requirement) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack matchingStack : requirement.ingredient().getItems()) {
            ItemStack copy = displayStack(matchingStack, requirement.requiredState(), requirement.count());
            if (!copy.isEmpty()) {
                stacks.add(copy);
            }
        }
        return stacks;
    }

    public static ItemStack displayOutput(KitchenProcessOutput output) {
        return displayStack(output.result(), output.state(), output.result().getCount());
    }

    public static ItemStack displayStack(ItemStack template, IngredientState state, int count) {
        ItemStack copy;
        if (template.getItem() instanceof KitchenIngredientItem ingredientItem) {
            copy = ingredientItem.createStack(Math.max(1, count), 0L);
        } else {
            copy = template.copy();
            copy.setCount(Math.max(1, count));
        }

        IngredientStateData data = KitchenStackUtil.getOrCreateData(copy, 0L);
        if (data != null) {
            KitchenStackUtil.setData(copy, data.withState(state));
        }
        return copy;
    }

    public static IngredientState defaultStateForItem(Item item) {
        if (item instanceof KitchenIngredientItem ingredientItem) {
            return ingredientItem.defaultState();
        }
        return IngredientState.PANTRY_READY;
    }

    public static IngredientState actualStateForStack(ItemStack stack, long gameTime) {
        if (stack.isEmpty()) {
            return IngredientState.PANTRY_READY;
        }
        IngredientStateData data = KitchenStackUtil.getData(stack);
        if (data != null) {
            return KitchenStackUtil.effectiveState(stack, gameTime);
        }
        return defaultStateForItem(stack.getItem());
    }

    public static boolean isModItem(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(JazzyCookin.MODID);
    }

    public static ResourceLocation itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    public static JazzyRecipeBookPlanner.OutputKey outputKey(ItemStack stack, IngredientState state) {
        return new JazzyRecipeBookPlanner.OutputKey(itemId(stack.getItem()), state);
    }

    public static JazzyRecipeBookPlanner.OutputKey outputKey(Item item, IngredientState state) {
        return new JazzyRecipeBookPlanner.OutputKey(itemId(item), state);
    }

    public static JazzyRecipeBookPlanner.OutputKey outputKeyFromActualStack(ItemStack stack, long gameTime) {
        return outputKey(stack, actualStateForStack(stack, gameTime));
    }

    public static ItemStack toolStack(ToolProfile profile) {
        return switch (profile) {
            case KNIFE, CHEF_KNIFE -> new ItemStack(JazzyItems.CHEF_KNIFE.get());
            case PARING_KNIFE -> new ItemStack(JazzyItems.PARING_KNIFE.get());
            case CLEAVER -> new ItemStack(JazzyItems.CLEAVER.get());
            case WHISK -> new ItemStack(JazzyItems.WHISK.get());
            case ROLLING_PIN -> new ItemStack(JazzyItems.ROLLING_PIN.get());
            case MORTAR_PESTLE -> new ItemStack(JazzyItems.MORTAR_PESTLE.get());
            case POT -> new ItemStack(JazzyItems.POT.get());
            case STOCK_POT -> new ItemStack(JazzyItems.STOCK_POT.get());
            case PAN -> new ItemStack(JazzyItems.FRYING_PAN.get());
            case SKILLET, FRYING_SKILLET -> new ItemStack(JazzyItems.FRYING_SKILLET.get());
            case BAKING_TRAY -> new ItemStack(JazzyItems.BAKING_TRAY.get());
            case SAUCEPAN -> new ItemStack(JazzyItems.SAUCEPAN.get());
            case STRAINER, COARSE_STRAINER -> new ItemStack(JazzyItems.COARSE_STRAINER.get());
            case FINE_STRAINER -> new ItemStack(JazzyItems.FINE_STRAINER.get());
            case STEAMER_BASKET -> new ItemStack(JazzyItems.STEAMER_BASKET.get());
            case JAR -> new ItemStack(JazzyItems.CANNING_JAR.get());
            case GLASS_JAR -> new ItemStack(JazzyItems.GLASS_JAR.get());
            case PIE_TIN -> new ItemStack(JazzyItems.PIE_TIN.get());
            case FORK -> new ItemStack(JazzyItems.FORK.get());
            case SPOON -> new ItemStack(JazzyItems.SPOON.get());
            case TABLE_KNIFE -> new ItemStack(JazzyItems.TABLE_KNIFE.get());
            default -> ItemStack.EMPTY;
        };
    }

    public static ItemStack stationStack(StationType stationType) {
        return switch (stationType) {
            case PREP_TABLE -> new ItemStack(JazzyItems.PREP_TABLE_ITEM.get());
            case SPICE_GRINDER -> new ItemStack(JazzyItems.SPICE_GRINDER_ITEM.get());
            case STRAINER -> new ItemStack(JazzyItems.STRAINER_ITEM.get());
            case MIXING_BOWL -> new ItemStack(JazzyItems.MIXING_BOWL_ITEM.get());
            case MICROWAVE -> new ItemStack(JazzyItems.MICROWAVE_ITEM.get());
            case FOOD_PROCESSOR -> new ItemStack(JazzyItems.FOOD_PROCESSOR_ITEM.get());
            case BLENDER -> new ItemStack(JazzyItems.BLENDER_ITEM.get());
            case JUICER -> new ItemStack(JazzyItems.JUICER_ITEM.get());
            case FREEZE_DRYER -> new ItemStack(JazzyItems.FREEZE_DRYER_ITEM.get());
            case CANNING_STATION -> new ItemStack(JazzyItems.CANNING_STATION_ITEM.get());
            case DRYING_RACK -> new ItemStack(JazzyItems.DRYING_RACK_ITEM.get());
            case SMOKER -> new ItemStack(JazzyItems.SMOKER_ITEM.get());
            case FERMENTATION_CROCK -> new ItemStack(JazzyItems.FERMENTATION_CROCK_ITEM.get());
            case STEAMER -> new ItemStack(JazzyItems.STEAMER_ITEM.get());
            case STOVE -> new ItemStack(JazzyItems.STOVE_ITEM.get());
            case OVEN -> new ItemStack(JazzyItems.OVEN_ITEM.get());
            case COOLING_RACK -> new ItemStack(JazzyItems.COOLING_RACK_ITEM.get());
            case RESTING_BOARD -> new ItemStack(JazzyItems.RESTING_BOARD_ITEM.get());
            case PLATING_STATION -> new ItemStack(JazzyItems.PLATING_STATION_ITEM.get());
        };
    }

    public static Component stateLabel(IngredientState state) {
        return Component.translatable("state.jazzycookin." + state.getSerializedName());
    }

    public static Component toolLabel(ToolProfile profile) {
        return Component.translatable("tool.jazzycookin." + profile.getSerializedName());
    }
}
