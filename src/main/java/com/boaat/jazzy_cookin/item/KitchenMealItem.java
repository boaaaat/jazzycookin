package com.boaat.jazzy_cookin.item;

import java.util.List;

import com.boaat.jazzy_cookin.kitchen.DishEvaluation;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.QualityBreakdown;
import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class KitchenMealItem extends KitchenIngredientItem {
    private final boolean returnsPlate;

    public KitchenMealItem(
            Properties properties,
            IngredientState defaultState,
            float baseQuality,
            float baseFlavor,
            float baseTexture,
            float baseStructure,
            float baseMoisture,
            float basePurity,
            float baseAeration,
            long decayTicks,
            int nourishment,
            int enjoyment,
            boolean returnsPlate
    ) {
        super(
                properties.food(new FoodProperties.Builder().nutrition(nourishment).saturationModifier(Math.max(0.2F, nourishment / 10.0F)).alwaysEdible().build()),
                defaultState,
                baseQuality,
                baseFlavor,
                baseTexture,
                baseStructure,
                baseMoisture,
                basePurity,
                baseAeration,
                decayTicks,
                nourishment,
                enjoyment
        );
        this.returnsPlate = returnsPlate;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        ItemStack result = super.finishUsingItem(stack, level, livingEntity);

        if (!level.isClientSide && livingEntity instanceof Player player) {
            QualityBreakdown breakdown = DishEvaluation.evaluateStack(stack, level);
            player.sendSystemMessage(Component.translatable(
                    "message.jazzycookin.served",
                    breakdown.grade().displayName(),
                    breakdown.nourishment(),
                    breakdown.enjoyment()
            ));
        }

        if (this.returnsPlate) {
            ItemStack plate = new ItemStack(JazzyItems.CERAMIC_PLATE.get());
            if (result.isEmpty()) {
                return plate;
            }

            if (livingEntity instanceof Player player && !player.getInventory().add(plate)) {
                player.drop(plate, false);
            }
        }

        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.jazzycookin.meal_hint").withStyle(ChatFormatting.AQUA));
    }
}
