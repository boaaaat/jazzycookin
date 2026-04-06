package com.boaat.jazzy_cookin.registry;

import java.util.function.Supplier;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.block.AppleSaplingBlock;
import com.boaat.jazzy_cookin.block.KitchenSourceBlock;
import com.boaat.jazzy_cookin.block.KitchenStationBlock;
import com.boaat.jazzy_cookin.block.KitchenStorageBlock;
import com.boaat.jazzy_cookin.kitchen.KitchenSourceProfile;
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
    public static final DeferredBlock<Block> TOMATO_VINE = source("tomato_vine", KitchenSourceProfile.TOMATO_VINE, MapColor.COLOR_RED, SoundType.CROP);
    public static final DeferredBlock<Block> HERB_BED = source("herb_bed", KitchenSourceProfile.HERB_BED, MapColor.COLOR_GREEN, SoundType.CROP);
    public static final DeferredBlock<Block> WHEAT_PATCH = source("wheat_patch", KitchenSourceProfile.WHEAT_PATCH, MapColor.COLOR_YELLOW, SoundType.CROP);
    public static final DeferredBlock<Block> CABBAGE_PATCH = source("cabbage_patch", KitchenSourceProfile.CABBAGE_PATCH, MapColor.COLOR_GREEN, SoundType.CROP);
    public static final DeferredBlock<Block> ONION_PATCH = source("onion_patch", KitchenSourceProfile.ONION_PATCH, MapColor.COLOR_LIGHT_GRAY, SoundType.CROP);
    public static final DeferredBlock<Block> CHICKEN_COOP = source("chicken_coop", KitchenSourceProfile.CHICKEN_COOP, MapColor.WOOD, SoundType.WOOD);
    public static final DeferredBlock<Block> DAIRY_STALL = source("dairy_stall", KitchenSourceProfile.DAIRY_STALL, MapColor.WOOD, SoundType.WOOD);
    public static final DeferredBlock<Block> FISHING_TRAP = source("fishing_trap", KitchenSourceProfile.FISHING_TRAP, MapColor.WOOD, SoundType.WOOD);
    public static final DeferredBlock<Block> FORAGE_SHRUB = source("forage_shrub", KitchenSourceProfile.FORAGE_SHRUB, MapColor.COLOR_PURPLE, SoundType.GRASS);

    public static final DeferredBlock<Block> PANTRY = storage("pantry", StorageType.PANTRY, MapColor.WOOD, SoundType.WOOD);
    public static final DeferredBlock<Block> FRIDGE = storage("fridge", StorageType.FRIDGE, MapColor.METAL, SoundType.METAL);
    public static final DeferredBlock<Block> FREEZER = storage("freezer", StorageType.FREEZER, MapColor.COLOR_LIGHT_BLUE, SoundType.GLASS);
    public static final DeferredBlock<Block> CELLAR = storage("cellar", StorageType.FRIDGE, MapColor.STONE, SoundType.STONE);
    public static final DeferredBlock<Block> PREP_TABLE = station("prep_table", StationType.PREP_TABLE, MapColor.WOOD, SoundType.WOOD);
    public static final DeferredBlock<Block> SPICE_GRINDER = station("spice_grinder", StationType.SPICE_GRINDER, MapColor.STONE, SoundType.STONE);
    public static final DeferredBlock<Block> STRAINER = station("strainer", StationType.STRAINER, MapColor.METAL, SoundType.METAL);
    public static final DeferredBlock<Block> MIXING_BOWL = station("mixing_bowl", StationType.MIXING_BOWL, MapColor.TERRACOTTA_ORANGE, SoundType.WOOD);
    public static final DeferredBlock<Block> MICROWAVE = station("microwave", StationType.MICROWAVE, MapColor.METAL, SoundType.METAL);
    public static final DeferredBlock<Block> FOOD_PROCESSOR = station("food_processor", StationType.FOOD_PROCESSOR, MapColor.QUARTZ, SoundType.STONE);
    public static final DeferredBlock<Block> BLENDER = station("blender", StationType.BLENDER, MapColor.COLOR_LIGHT_BLUE, SoundType.GLASS);
    public static final DeferredBlock<Block> JUICER = station("juicer", StationType.JUICER, MapColor.COLOR_LIGHT_GREEN, SoundType.GLASS);
    public static final DeferredBlock<Block> FREEZE_DRYER = station("freeze_dryer", StationType.FREEZE_DRYER, MapColor.ICE, SoundType.GLASS);
    public static final DeferredBlock<Block> CANNING_STATION = station("canning_station", StationType.CANNING_STATION, MapColor.WOOD, SoundType.WOOD);
    public static final DeferredBlock<Block> DRYING_RACK = station("drying_rack", StationType.DRYING_RACK, MapColor.WOOD, SoundType.WOOD);
    public static final DeferredBlock<Block> SMOKER = station("smoker", StationType.SMOKER, MapColor.WOOD, SoundType.WOOD);
    public static final DeferredBlock<Block> FERMENTATION_CROCK = station("fermentation_crock", StationType.FERMENTATION_CROCK, MapColor.TERRACOTTA_BROWN, SoundType.STONE);
    public static final DeferredBlock<Block> STEAMER = station("steamer", StationType.STEAMER, MapColor.METAL, SoundType.METAL);
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

    private static DeferredBlock<Block> source(String name, KitchenSourceProfile profile, MapColor mapColor, SoundType soundType) {
        return BLOCKS.register(name, () ->
                new KitchenSourceBlock(profile, BlockBehaviour.Properties.of()
                        .mapColor(mapColor)
                        .noCollission()
                        .instabreak()
                        .randomTicks()
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
            case STRAINER -> STRAINER;
            case MIXING_BOWL -> MIXING_BOWL;
            case MICROWAVE -> MICROWAVE;
            case FOOD_PROCESSOR -> FOOD_PROCESSOR;
            case BLENDER -> BLENDER;
            case JUICER -> JUICER;
            case FREEZE_DRYER -> FREEZE_DRYER;
            case CANNING_STATION -> CANNING_STATION;
            case DRYING_RACK -> DRYING_RACK;
            case SMOKER -> SMOKER;
            case FERMENTATION_CROCK -> FERMENTATION_CROCK;
            case STEAMER -> STEAMER;
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
            case FRIDGE -> FRIDGE;
            case FREEZER -> FREEZER;
        };
    }
}
