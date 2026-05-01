package com.boaat.jazzy_cookin.item;

import java.util.List;

import com.boaat.jazzy_cookin.kitchen.DishEvaluation;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.MasteryProgress;
import com.boaat.jazzy_cookin.kitchen.PantrySortTab;
import com.boaat.jazzy_cookin.kitchen.QualityBreakdown;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class KitchenMealItem extends KitchenIngredientItem {
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
            int enjoyment
    ) {
        super(
                properties.food(new FoodProperties.Builder().nutrition(nourishment).saturationModifier(Math.max(0.2F, nourishment / 10.0F)).alwaysEdible().build()),
                defaultState,
                PantrySortTab.OTHER,
                baseQuality,
                baseFlavor,
                baseTexture,
                baseStructure,
                baseMoisture,
                basePurity,
                baseAeration,
                decayTicks,
                0,
                nourishment,
                enjoyment,
                true,
                true
        );
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        ItemStack result = super.finishUsingItem(stack, level, livingEntity);

        if (!level.isClientSide && livingEntity instanceof Player player) {
            QualityBreakdown breakdown = DishEvaluation.evaluateStack(stack, level);
            MasteryProgress.awardForMeal(player, stack, breakdown);
            player.sendSystemMessage(Component.translatable(
                    "message.jazzycookin.served",
                    breakdown.grade().displayName(),
                    breakdown.nourishment(),
                    breakdown.enjoyment()
            ));
            player.sendSystemMessage(Component.translatable(
                    "message.jazzycookin.mastery",
                    MasteryProgress.totalServes(player),
                    MasteryProgress.forMeal(player, stack.getItemHolder().unwrapKey().map(reference -> reference.location().getPath()).orElse("unknown_meal"))
            ));
        }

        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
