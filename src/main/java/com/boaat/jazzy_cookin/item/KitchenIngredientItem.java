package com.boaat.jazzy_cookin.item;

import java.util.List;

import com.boaat.jazzy_cookin.kitchen.DishGrade;
import com.boaat.jazzy_cookin.kitchen.DishEvaluation;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.KitchenStateRules;
import com.boaat.jazzy_cookin.kitchen.MeasuredQuantity;
import com.boaat.jazzy_cookin.kitchen.PantrySortTab;
import com.boaat.jazzy_cookin.kitchen.QualityBreakdown;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfiles;
import com.boaat.jazzy_cookin.kitchen.sim.reaction.EggPanReactionSolver;
import com.boaat.jazzy_cookin.registry.JazzyDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.ItemUtils;

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
        super(nourishment > 0
                ? properties.food(new net.minecraft.world.food.FoodProperties.Builder()
                .nutrition(Math.max(1, nourishment))
                .saturationModifier(Math.max(0.2F, nourishment / 10.0F))
                .alwaysEdible()
                .build())
                : properties);
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

    public IngredientState defaultState() {
        return this.defaultState;
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

    public float baseQuality() {
        return this.baseQuality;
    }

    public float baseFlavor() {
        return this.baseFlavor;
    }

    public float baseTexture() {
        return this.baseTexture;
    }

    public float baseStructure() {
        return this.baseStructure;
    }

    public float baseMoisture() {
        return this.baseMoisture;
    }

    public float basePurity() {
        return this.basePurity;
    }

    public float baseAeration() {
        return this.baseAeration;
    }

    public int nourishment() {
        return this.nourishment;
    }

    public int enjoyment() {
        return this.enjoyment;
    }

    public ItemStack createStack(int count, long gameTime) {
        ItemStack stack = new ItemStack(this, count);
        KitchenStackUtil.initializeCanonicalStack(stack, gameTime);
        return stack;
    }

    public ItemStack createCreativeStack(int count) {
        ItemStack stack = this.createStack(count, CREATIVE_CREATED_TICK);
        KitchenStackUtil.setCreatedTick(stack, CREATIVE_CREATED_TICK, CREATIVE_CREATED_TICK);
        stack.remove(JazzyDataComponents.SPOILAGE_DISPLAY.get());
        KitchenStackUtil.setCookingDisplay(stack, false);
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide) {
            KitchenStackUtil.setCookingDisplay(stack, false);
            KitchenStackUtil.refreshSpoilageDisplay(stack, level.getGameTime());
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !stack.isEmpty() && (cookingBarRatio(stack) >= 0.0F || stack.get(JazzyDataComponents.SPOILAGE_DISPLAY.get()) != null);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide) {
            boolean changed = KitchenStackUtil.setCookingDisplay(stack, false);
            changed |= KitchenStackUtil.refreshSpoilageDisplay(stack, entity.level().getGameTime());
            if (changed) {
                entity.setItem(stack.copy());
            }
        }
        return false;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        float cookingRatio = cookingBarRatio(stack);
        if (cookingRatio >= 0.0F) {
            return Math.round(13.0F * cookingRatio);
        }
        return Math.round(13.0F * KitchenStackUtil.spoilageDisplayFreshness(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        FoodMatterData matter = KitchenStackUtil.getFoodMatter(stack);
        if (matter != null && cookingBarRatio(stack) >= 0.0F) {
            if (EggPanReactionSolver.isOvercooked(matter) || matter.charLevel() > 0.08F) {
                return 0x6B1F12;
            }
            if (EggPanReactionSolver.isPanOil(matter)) {
                float heat = Mth.clamp((Math.max(matter.surfaceTempC(), matter.coreTempC()) - 22.0F) / 190.0F, 0.0F, 1.0F);
                return Mth.hsvToRgb(0.13F - heat * 0.07F, 0.92F, 0.96F);
            }
            if (matter.timeInPan() <= 0 && Math.max(matter.surfaceTempC(), matter.coreTempC()) <= 34.0F) {
                float work = Mth.clamp(Math.max(matter.aeration(), Math.max(matter.fragmentation(), matter.whiskWork() * 0.5F)), 0.0F, 1.0F);
                return Mth.hsvToRgb(0.56F - work * 0.18F, 0.62F, 0.95F);
            }
            float cook = Mth.clamp(Math.max(matter.proteinSet(), Math.max(matter.browning() * 0.85F, matter.timeInPan() / 220.0F)), 0.0F, 1.0F);
            return Mth.hsvToRgb(0.30F - cook * 0.20F, 0.82F, 0.95F);
        }
        float freshness = KitchenStackUtil.spoilageDisplayFreshness(stack);
        return Mth.hsvToRgb(Mth.clamp(freshness * 0.33F, 0.0F, 0.33F), 0.90F, 0.96F);
    }

    private static float cookingBarRatio(ItemStack stack) {
        if (!KitchenStackUtil.isCookingDisplayActive(stack)) {
            return -1.0F;
        }
        FoodMatterData matter = KitchenStackUtil.getFoodMatter(stack);
        if (matter == null) {
            return -1.0F;
        }
        float hottestTemp = Math.max(matter.surfaceTempC(), matter.coreTempC());
        boolean heated = hottestTemp > 34.0F || matter.timeInPan() > 0;
        if (!heated) {
            if (!matter.isWorkedButUnfinished() && matter.whiskWork() <= 0.0F && matter.fragmentation() <= 0.0F && matter.aeration() <= 0.0F) {
                return -1.0F;
            }
            float work = Math.max(
                    Mth.clamp(matter.processDepth() / 4.0F, 0.0F, 1.0F),
                    Math.max(matter.whiskWork() * 0.5F, Math.max(matter.fragmentation(), matter.aeration()))
            );
            return Mth.clamp(work - matter.oxidation() * 0.18F - matter.microbialLoad() * 0.12F, 0.0F, 1.0F);
        }
        if (EggPanReactionSolver.isOvercooked(matter)) {
            return 0.0F;
        }
        if (EggPanReactionSolver.isPanOil(matter)) {
            return Mth.clamp((hottestTemp - 22.0F) / 190.0F, 0.0F, 1.0F);
        }
        float cooked = EggPanReactionSolver.cookProgress(matter);
        return Mth.clamp(cooked - matter.charLevel() * 0.45F, 0.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        IngredientStateData data = KitchenStackUtil.getData(stack);
        if (data == null) {
            data = FoodMaterialProfiles.canonicalSummary(stack, this.defaultState, 0L, this instanceof KitchenMealItem);
        }

        Level level = context.level();
        IngredientState displayState = level != null ? KitchenStackUtil.effectiveState(stack, level.getGameTime()) : data.state();
        tooltipComponents.add(Component.translatable("tooltip.jazzycookin.state", Component.translatable("state.jazzycookin." + displayState.getSerializedName()))
                .withStyle(ChatFormatting.GRAY));
        MeasuredQuantity measured = KitchenStackUtil.measuredQuantity(stack);
        if (measured != null) {
            tooltipComponents.add(Component.translatable("tooltip.jazzycookin.measured_quantity",
                    measured.displayLabel().isBlank() ? Component.literal(measured.amount() + " " + measured.unit().getSerializedName()) : Component.literal(measured.displayLabel()))
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
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
        if (level != null && this.canConsume(stack, level)) {
            tooltipComponents.add(Component.translatable("tooltip.jazzycookin.meal_hint").withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (this.canConsume(stack, level)) {
            return ItemUtils.startUsingInstantly(level, player, usedHand);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return switch (KitchenStackUtil.getData(stack) != null ? KitchenStackUtil.getData(stack).state() : this.defaultState) {
            case FRESH_JUICE, SMOOTH, CREAMY -> UseAnim.DRINK;
            default -> UseAnim.EAT;
        };
    }

    protected boolean canConsume(ItemStack stack, Level level) {
        if (this instanceof KitchenMealItem) {
            return true;
        }
        return KitchenStateRules.isFinishedUnplatedState(KitchenStackUtil.effectiveState(stack, level.getGameTime()));
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
