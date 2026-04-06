package com.boaat.jazzy_cookin.registry;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.block.entity.KitchenStationBlockEntity;
import com.boaat.jazzy_cookin.block.entity.KitchenStorageBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class JazzyBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, JazzyCookin.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KitchenStationBlockEntity>> KITCHEN_STATION =
            BLOCK_ENTITIES.register("kitchen_station", () -> BlockEntityType.Builder.of(
                    KitchenStationBlockEntity::new,
                    JazzyBlocks.PREP_TABLE.get(),
                    JazzyBlocks.SPICE_GRINDER.get(),
                    JazzyBlocks.STRAINER.get(),
                    JazzyBlocks.MIXING_BOWL.get(),
                    JazzyBlocks.MICROWAVE.get(),
                    JazzyBlocks.FOOD_PROCESSOR.get(),
                    JazzyBlocks.BLENDER.get(),
                    JazzyBlocks.JUICER.get(),
                    JazzyBlocks.FREEZE_DRYER.get(),
                    JazzyBlocks.CANNING_STATION.get(),
                    JazzyBlocks.DRYING_RACK.get(),
                    JazzyBlocks.SMOKER.get(),
                    JazzyBlocks.FERMENTATION_CROCK.get(),
                    JazzyBlocks.STEAMER.get(),
                    JazzyBlocks.STOVE.get(),
                    JazzyBlocks.OVEN.get(),
                    JazzyBlocks.COOLING_RACK.get(),
                    JazzyBlocks.RESTING_BOARD.get(),
                    JazzyBlocks.PLATING_STATION.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KitchenStorageBlockEntity>> KITCHEN_STORAGE =
            BLOCK_ENTITIES.register("kitchen_storage", () -> BlockEntityType.Builder.of(
                    KitchenStorageBlockEntity::new,
                    JazzyBlocks.PANTRY.get(),
                    JazzyBlocks.FRIDGE.get(),
                    JazzyBlocks.FREEZER.get(),
                    JazzyBlocks.CELLAR.get()
            ).build(null));

    private JazzyBlockEntities() {
    }
}
