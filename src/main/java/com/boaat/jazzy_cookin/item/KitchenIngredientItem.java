package com.boaat.jazzy_cookin.item;

import java.util.List;

import com.boaat.jazzy_cookin.kitchen.DishGrade;
import com.boaat.jazzy_cookin.kitchen.DishEvaluation;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.PantrySortTab;
import com.boaat.jazzy_cookin.kitchen.QualityBreakdown;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class KitchenIngredientItem extends Item {
    private static final long MINECRAFT_DAY_TICKS = 24_000L;
    private static final long MINECRAFT_HOUR_TICKS = 1_000L;
    private static final long CREATIVE_CREATED_TICK = Long.MAX_VALUE / 4L;

    private final IngredientState defaultState;
    private final PantrySortTab pantryTab;
    private final float baseQuality;
    private final float baseFlavor;
    private final float baseTexture;
    private final float baseStructure;
    private final float baseMoisture;
    private final float basePurity;
    private final float baseAeration;
    private final long decayTicks;
    private final int cookTimeTicks;
    private final int nourishment;
    private final int enjoyment;
    private final boolean fridgeSafe;
    private final boolean freezerSafe;

    public KitchenIngredientItem(
            Properties properties,
            IngredientState defaultState,
            PantrySortTab pantryTab,
            float baseQuality,
            float baseFlavor,
            float baseTexture,
            float baseStructure,
            float baseMoisture,
            float basePurity,
            float baseAeration,
            long decayTicks,
            int cookTimeTicks,
            int nourishment,
            int enjoyment,
            boolean fridgeSafe,
            boolean freezerSafe
    ) {
        super(properties);
        this.defaultState = defaultState;
        this.pantryTab = pantryTab;
        this.baseQuality = baseQuality;
        this.baseFlavor = baseFlavor;
        this.baseTexture = baseTexture;
        this.baseStructure = baseStructure;
        this.baseMoisture = baseMoisture;
        this.basePurity = basePurity;
        this.baseAeration = baseAeration;
        this.decayTicks = decayTicks;
        this.cookTimeTicks = cookTimeTicks;
        this.nourishment = nourishment;
        this.enjoyment = enjoyment;
        this.fridgeSafe = fridgeSafe;
        this.freezerSafe = freezerSafe;
    }

    public long decayTicks() {
        return this.decayTicks;
    }

    public PantrySortTab pantryTab() {
        return this.pantryTab;
    }

    public boolean isFridgeSafe() {
        return this.fridgeSafe;
    }

    public boolean isFreezerSafe() {
        return this.freezerSafe;
    }

    public int cookTimeTicks() {
        return this.cookTimeTicks;
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

    public IngredientStateData maxData() {
        return new IngredientStateData(
                this.defaultState,
                CREATIVE_CREATED_TICK,
                1.0F,
                1.0F,
                1.0F,
                1.0F,
                1.0F,
                1.0F,
                1.0F,
                1.0F,
                0,
                this.nourishment,
                this.enjoyment
        );
    }

    public ItemStack createStack(int count, long gameTime) {
        return this.createStack(count, gameTime, this.defaultData(gameTime));
    }

    public ItemStack createStack(int count, long gameTime, IngredientStateData data) {
        ItemStack stack = new ItemStack(this, count);
        FoodMatterData matter = FoodMatterData.fromLegacy(data, this instanceof KitchenMealItem);
        KitchenStackUtil.initializeStack(stack, data, matter, gameTime);
        return stack;
    }

    public ItemStack createCreativeStack(int count) {
        return this.createStack(count, CREATIVE_CREATED_TICK, this.maxData());
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
        tooltipComponents.add(Component.translatable("tooltip.jazzycookin.shelf_life", this.shelfLifeLabel()).withStyle(ChatFormatting.DARK_GRAY));
        if (this.cookTimeTicks > 0) {
            tooltipComponents.add(Component.translatable("tooltip.jazzycookin.cook_time", this.cookTimeLabel()).withStyle(ChatFormatting.RED));
        }
        tooltipComponents.add(Component.translatable("tooltip.jazzycookin.nourishment", data.nourishment()).withStyle(ChatFormatting.BLUE));
        tooltipComponents.add(Component.translatable("tooltip.jazzycookin.enjoyment", data.enjoyment()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private Component shelfLifeLabel() {
        if (this.decayTicks >= Long.MAX_VALUE / 4L) {
            return Component.translatable("tooltip.jazzycookin.shelf_life_stable");
        }
        return Component.literal(formatDuration(this.decayTicks, MINECRAFT_DAY_TICKS, "d", MINECRAFT_HOUR_TICKS, "h"));
    }

    private Component cookTimeLabel() {
        if (this.cookTimeTicks % 20 == 0) {
            return Component.literal((this.cookTimeTicks / 20) + "s");
        }
        return Component.literal(this.cookTimeTicks + "t");
    }

    private static String formatDuration(long ticks, long largeUnitTicks, String largeUnitLabel, long smallUnitTicks, String smallUnitLabel) {
        if (ticks % largeUnitTicks == 0) {
            return (ticks / largeUnitTicks) + largeUnitLabel;
        }
        if (ticks >= largeUnitTicks) {
            long whole = ticks / largeUnitTicks;
            long remainder = (ticks % largeUnitTicks) / smallUnitTicks;
            if (remainder == 0L) {
                return whole + largeUnitLabel;
            }
            return whole + largeUnitLabel + " " + remainder + smallUnitLabel;
        }
        if (ticks % smallUnitTicks == 0) {
            return (ticks / smallUnitTicks) + smallUnitLabel;
        }
        return ticks + "t";
    }
}
