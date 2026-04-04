package com.boaat.jazzy_cookin.registry;

import java.util.function.Supplier;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.block.AppleSaplingBlock;
import com.boaat.jazzy_cookin.block.KitchenStationBlock;
import com.boaat.jazzy_cookin.block.KitchenStorageBlock;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.StorageType;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class JazzyBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(JazzyCookin.MODID);

    public static final DeferredBlock<Block> APPLE_SAPLING = BLOCKS.register("apple_sapling", () ->
            new AppleSaplingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .randomTicks()
                    .sound(SoundType.GRASS)));

    public static final DeferredBlock<Block> PANTRY = storage("pantry", StorageType.PANTRY, MapColor.WOOD, SoundType.WOOD);
    public static final DeferredBlock<Block> CELLAR = storage("cellar", StorageType.CELLAR, MapColor.STONE, SoundType.STONE);
    public static final DeferredBlock<Block> PREP_TABLE = station("prep_table", StationType.PREP_TABLE, MapColor.WOOD, SoundType.WOOD);
    public static final DeferredBlock<Block> SPICE_GRINDER = station("spice_grinder", StationType.SPICE_GRINDER, MapColor.STONE, SoundType.STONE);
    public static final DeferredBlock<Block> MIXING_BOWL = station("mixing_bowl", StationType.MIXING_BOWL, MapColor.TERRACOTTA_ORANGE, SoundType.WOOD);
    public static final DeferredBlock<Block> STOVE = station("stove", StationType.STOVE, MapColor.METAL, SoundType.METAL);
    public static final DeferredBlock<Block> OVEN = station("oven", StationType.OVEN, MapColor.STONE, SoundType.STONE);
    public static final DeferredBlock<Block> COOLING_RACK = station("cooling_rack", StationType.COOLING_RACK, MapColor.METAL, SoundType.METAL);
    public static final DeferredBlock<Block> RESTING_BOARD = station("resting_board", StationType.RESTING_BOARD, MapColor.WOOD, SoundType.WOOD);
    public static final DeferredBlock<Block> PLATING_STATION = station("plating_station", StationType.PLATING_STATION, MapColor.QUARTZ, SoundType.STONE);

    private JazzyBlocks() {
    }

    private static DeferredBlock<Block> station(String name, StationType stationType, MapColor mapColor, SoundType soundType) {
        return BLOCKS.register(name, () ->
                new KitchenStationBlock(stationType, BlockBehaviour.Properties.of()
                        .mapColor(mapColor)
                        .strength(2.0F)
                        .sound(soundType)));
    }

    private static DeferredBlock<Block> storage(String name, StorageType storageType, MapColor mapColor, SoundType soundType) {
        return BLOCKS.register(name, () ->
                new KitchenStorageBlock(storageType, BlockBehaviour.Properties.of()
                        .mapColor(mapColor)
                        .strength(2.5F)
                        .sound(soundType)));
    }

    public static Supplier<Block> blockForStation(StationType stationType) {
        return switch (stationType) {
            case PREP_TABLE -> PREP_TABLE;
            case SPICE_GRINDER -> SPICE_GRINDER;
            case MIXING_BOWL -> MIXING_BOWL;
            case STOVE -> STOVE;
            case OVEN -> OVEN;
            case COOLING_RACK -> COOLING_RACK;
            case RESTING_BOARD -> RESTING_BOARD;
            case PLATING_STATION -> PLATING_STATION;
        };
    }

    public static Supplier<Block> blockForStorage(StorageType storageType) {
        return switch (storageType) {
            case PANTRY -> PANTRY;
            case CELLAR -> CELLAR;
        };
    }
}
