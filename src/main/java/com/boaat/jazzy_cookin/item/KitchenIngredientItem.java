package com.boaat.jazzy_cookin.item;

import java.util.List;

import com.boaat.jazzy_cookin.kitchen.DishGrade;
import com.boaat.jazzy_cookin.kitchen.DishEvaluation;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.QualityBreakdown;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class KitchenIngredientItem extends Item {
    private final IngredientState defaultState;
    private final float baseQuality;
    private final float baseFlavor;
    private final float baseTexture;
    private final float baseStructure;
    private final float baseMoisture;
    private final float basePurity;
    private final float baseAeration;
    private final long decayTicks;
    private final int nourishment;
    private final int enjoyment;

    public KitchenIngredientItem(
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
        super(properties);
        this.defaultState = defaultState;
        this.baseQuality = baseQuality;
        this.baseFlavor = baseFlavor;
        this.baseTexture = baseTexture;
        this.baseStructure = baseStructure;
        this.baseMoisture = baseMoisture;
        this.basePurity = basePurity;
        this.baseAeration = baseAeration;
        this.decayTicks = decayTicks;
        this.nourishment = nourishment;
        this.enjoyment = enjoyment;
    }

    public long decayTicks() {
        return this.decayTicks;
    }

    public IngredientStateData defaultData(long gameTime) {
        return new IngredientStateData(
                this.defaultState,
                gameTime,
                this.baseQuality,
                0.72F,
                this.baseFlavor,
                this.baseTexture,
                this.baseStructure,
                this.baseMoisture,
                this.basePurity,
                this.baseAeration,
                0,
                this.nourishment,
                this.enjoyment
        );
    }

    public ItemStack createStack(int count, long gameTime) {
        ItemStack stack = new ItemStack(this, count);
        stack.set(com.boaat.jazzy_cookin.registry.JazzyDataComponents.INGREDIENT_STATE.get(), this.defaultData(gameTime));
        return stack;
    }

    public ItemStack createStack(int count, long gameTime, IngredientStateData data) {
        ItemStack stack = new ItemStack(this, count);
        stack.set(com.boaat.jazzy_cookin.registry.JazzyDataComponents.INGREDIENT_STATE.get(), data);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        IngredientStateData data = KitchenStackUtil.getData(stack);
        if (data == null) {
            data = this.defaultData(0L);
        }

        Level level = context.level();
        IngredientState displayState = level != null ? KitchenStackUtil.effectiveState(stack, level.getGameTime()) : data.state();
        tooltipComponents.add(Component.translatable("tooltip.jazzycookin.state", Component.translatable("state.jazzycookin." + displayState.getSerializedName()))
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.jazzycookin.quality", DishGrade.fromScore(data.quality()).displayName())
                .withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.translatable("tooltip.jazzycookin.recipe_accuracy", Math.round(data.recipeAccuracy() * 100.0F))
                .withStyle(ChatFormatting.AQUA));
        if (level != null) {
            QualityBreakdown breakdown = DishEvaluation.evaluateStack(stack, level);
            tooltipComponents.add(breakdown.summary().withStyle(ChatFormatting.YELLOW));
            tooltipComponents.add(Component.translatable("tooltip.jazzycookin.freshness", Component.translatable("freshness.jazzycookin." + KitchenStackUtil.freshnessLabel(stack, level)))
                    .withStyle(ChatFormatting.GREEN));
            tooltipComponents.add(Component.translatable("tooltip.jazzycookin.prep_score", Math.round(breakdown.prepScore() * 100.0F)).withStyle(ChatFormatting.DARK_GREEN));
            tooltipComponents.add(Component.translatable("tooltip.jazzycookin.combine_score", Math.round(breakdown.combineScore() * 100.0F)).withStyle(ChatFormatting.DARK_AQUA));
            tooltipComponents.add(Component.translatable("tooltip.jazzycookin.cooking_score", Math.round(breakdown.cookingScore() * 100.0F)).withStyle(ChatFormatting.RED));
            tooltipComponents.add(Component.translatable("tooltip.jazzycookin.finishing_score", Math.round(breakdown.finishingScore() * 100.0F)).withStyle(ChatFormatting.BLUE));
            tooltipComponents.add(Component.translatable("tooltip.jazzycookin.plating_score", Math.round(breakdown.platingScore() * 100.0F)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        tooltipComponents.add(Component.translatable("tooltip.jazzycookin.nourishment", data.nourishment()).withStyle(ChatFormatting.BLUE));
        tooltipComponents.add(Component.translatable("tooltip.jazzycookin.enjoyment", data.enjoyment()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
