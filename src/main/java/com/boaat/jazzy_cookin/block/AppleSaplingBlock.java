package com.boaat.jazzy_cookin.block;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.recipebook.RecipeBookProgress;
import com.boaat.jazzy_cookin.recipebook.SourceGuideRegistry;
import com.boaat.jazzy_cookin.recipebook.network.RecipeBookNetworking;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AppleSaplingBlock extends BushBlock implements BonemealableBlock {
    public static final MapCodec<AppleSaplingBlock> CODEC = simpleCodec(AppleSaplingBlock::new);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    private static final VoxelShape SHAPE = box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    public AppleSaplingBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(net.minecraft.tags.BlockTags.DIRT) || state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
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
        return state.getValue(AGE) < 6;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getRawBrightness(pos.above(), 0) >= 9 && random.nextInt(4) == 0) {
            int age = state.getValue(AGE);
            if (age < 6) {
                level.setBlock(pos, state.setValue(AGE, age + 1), 2);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        int age = state.getValue(AGE);
        if (age < 5) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            KitchenIngredientItem appleItem = (KitchenIngredientItem) SourceGuideRegistry.appleHarvestItem();
            float quality = this.harvestQuality(level, pos, age);
            IngredientStateData baseData = appleItem.defaultData(level.getGameTime());
            IngredientStateData harvestData = baseData.withMetrics(
                    baseData.state(),
                    level.getGameTime(),
                    quality,
                    Math.min(1.0F, baseData.recipeAccuracy() + 0.06F),
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
            net.minecraft.world.item.ItemStack harvestStack = appleItem.createStack(1, level.getGameTime(), harvestData);
            Containers.dropItemStack(level, pos.getX(), pos.getY() + 0.75D, pos.getZ(), harvestStack);
            level.setBlock(pos, state.setValue(AGE, 4), 2);
            if (player instanceof ServerPlayer serverPlayer && RecipeBookProgress.recordSourceHarvest(serverPlayer, harvestStack, "apple_sapling")) {
                RecipeBookNetworking.sync(serverPlayer);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private float harvestQuality(Level level, BlockPos pos, int age) {
        float ripeness = age == 5 ? 0.84F : 0.68F;
        float lightBonus = level.getRawBrightness(pos.above(), 0) >= 9 ? 0.05F : -0.05F;
        float hydrationBonus = hasNearbyWater(level, pos) ? 0.04F : -0.03F;
        return Math.max(0.2F, Math.min(1.0F, ripeness + lightBonus + hydrationBonus));
    }

    private static boolean hasNearbyWater(Level level, BlockPos pos) {
        for (BlockPos checkPos : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 0, 2))) {
            if (level.getFluidState(checkPos).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(AGE) < 5;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int age = state.getValue(AGE);
        level.setBlock(pos, state.setValue(AGE, Math.min(5, age + 1)), 2);
    }
}
