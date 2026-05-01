package com.boaat.jazzy_cookin.registry;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.SpoilageDisplayData;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class JazzyDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, JazzyCookin.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<IngredientState>> FOOD_STATE =
            DATA_COMPONENTS.registerComponentType(
                    "food_state",
                    builder -> builder.persistent(IngredientState.CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FoodMatterData>> FOOD_MATTER =
            DATA_COMPONENTS.registerComponentType(
                    "food_matter",
                    builder -> builder.persistent(FoodMatterData.CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SpoilageDisplayData>> SPOILAGE_DISPLAY =
            DATA_COMPONENTS.registerComponentType(
                    "spoilage_display",
                    builder -> builder.persistent(SpoilageDisplayData.CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> COOKING_DISPLAY =
            DATA_COMPONENTS.registerComponentType(
                    "cooking_display",
                    builder -> builder.networkSynchronized(ByteBufCodecs.BOOL)
            );

    private JazzyDataComponents() {
    }
}
