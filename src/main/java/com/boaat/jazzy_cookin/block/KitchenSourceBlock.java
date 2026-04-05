package com.boaat.jazzy_cookin.block;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.kitchen.KitchenSourceProfile;
import com.boaat.jazzy_cookin.registry.JazzyItems;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class KitchenSourceBlock extends BushBlock implements BonemealableBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    private static final VoxelShape SHAPE = box(2.0D, 0.0D, 2.0D, 14.0D, 15.0D, 14.0D);

    private final KitchenSourceProfile profile;
    private final MapCodec<KitchenSourceBlock> codec;

    public KitchenSourceBlock(KitchenSourceProfile profile, BlockBehaviour.Properties properties) {
        super(properties);
        this.profile = profile;
        this.codec = simpleCodec(blockProperties -> new KitchenSourceBlock(profile, blockProperties));
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    public KitchenSourceProfile profile() {
        return this.profile;
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return this.codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) || state.is(BlockTags.SAND);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < this.profile.maxAge();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) != 0) {
            return;
        }

        if (this.profile.plantLike() && level.getRawBrightness(pos.above(), 0) < this.profile.minimumLight()) {
            return;
        }

        int age = state.getValue(AGE);
        if (age < this.profile.maxAge()) {
            level.setBlock(pos, state.setValue(AGE, Math.min(this.profile.maxAge(), age + 1)), 2);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(AGE) < this.profile.ripeAge()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            Containers.dropItemStack(level, pos.getX(), pos.getY() + 0.75D, pos.getZ(), this.createHarvestStack(level, state, pos));
            int resetAge = this.profile.plantLike() ? Math.max(0, this.profile.ripeAge() - 2) : 0;
            level.setBlock(pos, state.setValue(AGE, resetAge), 2);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private ItemStack createHarvestStack(Level level, BlockState state, BlockPos pos) {
        KitchenIngredientItem ingredientItem = switch (this.profile) {
            case TOMATO_VINE -> JazzyItems.ingredient(JazzyItems.IngredientId.TOMATOES).get();
            case HERB_BED -> randomIngredient(level,
                    JazzyItems.IngredientId.BASIL,
                    JazzyItems.IngredientId.PARSLEY,
                    JazzyItems.IngredientId.DILL,
                    JazzyItems.IngredientId.OREGANO
            );
            case WHEAT_PATCH -> JazzyItems.ingredient(JazzyItems.IngredientId.WHOLE_WHEAT_FLOUR).get();
            case CABBAGE_PATCH -> JazzyItems.ingredient(JazzyItems.IngredientId.CABBAGE).get();
            case ONION_PATCH -> JazzyItems.ingredient(JazzyItems.IngredientId.ONIONS).get();
            case CHICKEN_COOP -> level.random.nextFloat() < 0.35F
                    ? JazzyItems.ingredient(JazzyItems.IngredientId.CHICKEN).get()
                    : JazzyItems.ingredient(JazzyItems.IngredientId.EGGS).get();
            case DAIRY_STALL -> level.random.nextFloat() < 0.40F
                    ? JazzyItems.ingredient(JazzyItems.IngredientId.BUTTER).get()
                    : JazzyItems.ingredient(JazzyItems.IngredientId.SHELF_STABLE_CREAM).get();
            case FISHING_TRAP -> JazzyItems.ingredient(JazzyItems.IngredientId.FISH_FILLET).get();
            case FORAGE_SHRUB -> randomIngredient(level,
                    JazzyItems.IngredientId.MINT,
                    JazzyItems.IngredientId.ROSEMARY
            );
        };

        float quality = harvestQuality(level, state, pos);
        IngredientStateData baseData = ingredientItem.defaultData(level.getGameTime());
        IngredientStateData harvestData = baseData.withMetrics(
                baseData.state(),
                level.getGameTime(),
                quality,
                Math.min(1.0F, baseData.flavor() + 0.05F),
                baseData.texture(),
                baseData.structure(),
                baseData.moisture(),
                baseData.purity(),
                baseData.aeration(),
                1,
                baseData.nourishment(),
                baseData.enjoyment()
        );
        return ingredientItem.createStack(1, level.getGameTime(), harvestData);
    }

    private static KitchenIngredientItem randomIngredient(Level level, JazzyItems.IngredientId... candidates) {
        return JazzyItems.ingredient(candidates[level.random.nextInt(candidates.length)]).get();
    }

    private float harvestQuality(Level level, BlockState state, BlockPos pos) {
        int age = state.getValue(AGE);
        float lightBonus = this.profile.plantLike() && level.getRawBrightness(pos.above(), 0) >= this.profile.minimumLight() ? 0.05F : -0.06F;
        float hydrationBonus = this.profile.prefersHydration() ? (hasNearbyWater(level, pos) ? 0.05F : -0.05F) : 0.0F;
        if (age == this.profile.ripeAge()) {
            return clampQuality(0.82F + lightBonus + hydrationBonus);
        }
        if (age > this.profile.ripeAge()) {
            return clampQuality(0.68F + lightBonus + hydrationBonus);
        }
        return clampQuality(0.60F + lightBonus + hydrationBonus);
    }

    private static boolean hasNearbyWater(LevelReader level, BlockPos pos) {
        for (BlockPos checkPos : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 0, 2))) {
            if (level.getFluidState(checkPos).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    private static float clampQuality(float quality) {
        return Math.max(0.2F, Math.min(1.0F, quality));
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(AGE) < this.profile.ripeAge();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int growth = this.profile.plantLike() ? 1 + random.nextInt(2) : 1;
        int age = state.getValue(AGE);
        level.setBlock(pos, state.setValue(AGE, Math.min(this.profile.maxAge(), age + growth)), 2);
    }
}
