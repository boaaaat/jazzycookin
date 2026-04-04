package com.boaat.jazzy_cookin;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FryingPanBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<FryingPanBlock> CODEC = simpleCodec(FryingPanBlock::new);

    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            Block.box(2.0D, 0.0D, 3.0D, 14.0D, 2.0D, 15.0D),
            Block.box(6.0D, 0.0D, 0.0D, 10.0D, 1.0D, 3.0D));
    private static final VoxelShape SHAPE_EAST = Shapes.or(
            Block.box(1.0D, 0.0D, 2.0D, 13.0D, 2.0D, 14.0D),
            Block.box(13.0D, 0.0D, 6.0D, 16.0D, 1.0D, 10.0D));
    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
            Block.box(2.0D, 0.0D, 1.0D, 14.0D, 2.0D, 13.0D),
            Block.box(6.0D, 0.0D, 13.0D, 10.0D, 1.0D, 16.0D));
    private static final VoxelShape SHAPE_WEST = Shapes.or(
            Block.box(3.0D, 0.0D, 2.0D, 15.0D, 2.0D, 14.0D),
            Block.box(0.0D, 0.0D, 6.0D, 3.0D, 1.0D, 10.0D));

    public FryingPanBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case EAST -> SHAPE_EAST;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }
}
