package com.boaat.jazzy_cookin.block;

import com.boaat.jazzy_cookin.block.entity.KitchenStationBlockEntity;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.registry.JazzyBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class KitchenStationBlock extends BaseEntityBlock implements EntityBlock {
    private final StationType stationType;
    private final MapCodec<KitchenStationBlock> codec;

    public KitchenStationBlock(StationType stationType, BlockBehaviour.Properties properties) {
        super(properties);
        this.stationType = stationType;
        this.codec = simpleCodec(blockProperties -> new KitchenStationBlock(stationType, blockProperties));
    }

    public StationType stationType() {
        return this.stationType;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return this.codec;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KitchenStationBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MenuProvider provider) {
                serverPlayer.openMenu(provider, buffer -> {
                    buffer.writeBlockPos(pos);
                    buffer.writeUtf(this.stationType.getSerializedName());
                });
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof KitchenStationBlockEntity stationBlockEntity) {
                stationBlockEntity.clearCookingDisplays();
                Containers.dropContents(level, pos, stationBlockEntity);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTickerHelper(blockEntityType, JazzyBlockEntities.KITCHEN_STATION.get(), KitchenStationBlockEntity::serverTick);
    }
}
