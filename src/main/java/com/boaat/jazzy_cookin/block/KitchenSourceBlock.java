package com.boaat.jazzy_cookin.block;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.kitchen.KitchenSourceProfile;
import com.boaat.jazzy_cookin.registry.JazzyItems;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
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

        if (this.profile.plantLike() && level.getRawBrightness(pos.above(), 0) < 8) {
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
            Containers.dropItemStack(level, pos.getX(), pos.getY() + 0.75D, pos.getZ(), this.createHarvestStack(level, state));
            int resetAge = this.profile.plantLike() ? Math.max(0, this.profile.ripeAge() - 2) : 0;
            level.setBlock(pos, state.setValue(AGE, resetAge), 2);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private ItemStack createHarvestStack(Level level, BlockState state) {
        KitchenIngredientItem ingredientItem = switch (this.profile) {
            case TOMATO_VINE -> JazzyItems.TOMATO.get();
            case HERB_BED -> JazzyItems.FRESH_HERB.get();
            case WHEAT_PATCH -> JazzyItems.WHEAT_SHEAF.get();
            case CABBAGE_PATCH -> JazzyItems.CABBAGE.get();
            case ONION_PATCH -> JazzyItems.ONION.get();
            case CHICKEN_COOP -> level.random.nextFloat() < 0.25F ? JazzyItems.RAW_PROTEIN.get() : JazzyItems.FARM_EGG.get();
            case DAIRY_STALL -> JazzyItems.FRESH_MILK.get();
            case FISHING_TRAP -> JazzyItems.RAW_FISH_ITEM.get();
            case FORAGE_SHRUB -> JazzyItems.WILD_BERRIES_ITEM.get();
        };

        float quality = harvestQuality(state.getValue(AGE));
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

    private float harvestQuality(int age) {
        if (age == this.profile.ripeAge()) {
            return 0.92F;
        }
        if (age > this.profile.ripeAge()) {
            return 0.76F;
        }
        return 0.68F;
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
