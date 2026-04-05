package com.boaat.jazzy_cookin.integration.jei;

import java.util.ArrayList;
import java.util.List;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.boaat.jazzy_cookin.recipe.KitchenInputRequirement;
import com.boaat.jazzy_cookin.recipe.KitchenProcessOutput;
import com.boaat.jazzy_cookin.registry.JazzyBlocks;
import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class JazzyJeiStackUtil {
    private JazzyJeiStackUtil() {
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

    public static List<ItemStack> toolStacks(List<ToolProfile> profiles) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ToolProfile profile : profiles) {
            ItemStack stack = toolStack(profile);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    public static ItemStack toolStack(ToolProfile profile) {
        return switch (profile) {
            case PARING_KNIFE -> new ItemStack(JazzyItems.PARING_KNIFE.get());
            case CHEF_KNIFE -> new ItemStack(JazzyItems.CHEF_KNIFE.get());
            case CLEAVER -> new ItemStack(JazzyItems.CLEAVER.get());
            case WHISK -> new ItemStack(JazzyItems.WHISK.get());
            case ROLLING_PIN -> new ItemStack(JazzyItems.ROLLING_PIN.get());
            case MORTAR_PESTLE -> new ItemStack(JazzyItems.MORTAR_PESTLE.get());
            case STOCK_POT -> new ItemStack(JazzyItems.STOCK_POT.get());
            case FRYING_SKILLET -> new ItemStack(JazzyItems.FRYING_SKILLET.get());
            case FINE_STRAINER -> new ItemStack(JazzyItems.FINE_STRAINER.get());
            case COARSE_STRAINER -> new ItemStack(JazzyItems.COARSE_STRAINER.get());
            case STEAMER_BASKET -> new ItemStack(JazzyItems.STEAMER_BASKET.get());
            case JAR -> new ItemStack(JazzyItems.CANNING_JAR.get());
            case PIE_TIN -> new ItemStack(JazzyItems.PIE_TIN.get());
            default -> ItemStack.EMPTY;
        };
    }

    public static ItemStack stationStack(StationType stationType) {
        return switch (stationType) {
            case PREP_TABLE -> new ItemStack(JazzyItems.PREP_TABLE_ITEM.get());
            case SPICE_GRINDER -> new ItemStack(JazzyItems.SPICE_GRINDER_ITEM.get());
            case STRAINER -> new ItemStack(JazzyItems.STRAINER_ITEM.get());
            case MIXING_BOWL -> new ItemStack(JazzyItems.MIXING_BOWL_ITEM.get());
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
