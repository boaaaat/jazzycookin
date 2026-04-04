package com.boaat.jazzy_cookin.item;

import java.util.List;

import com.boaat.jazzy_cookin.kitchen.DishGrade;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

        tooltipComponents.add(Component.translatable("tooltip.jazzycookin.state", Component.translatable("state.jazzycookin." + data.state().getSerializedName()))
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.jazzycookin.quality", DishGrade.fromScore(data.quality()).displayName())
                .withStyle(ChatFormatting.GOLD));
    }
}
