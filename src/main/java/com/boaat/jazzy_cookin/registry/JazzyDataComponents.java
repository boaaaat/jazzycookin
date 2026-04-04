package com.boaat.jazzy_cookin.registry;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class JazzyDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, JazzyCookin.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<IngredientStateData>> INGREDIENT_STATE =
            DATA_COMPONENTS.registerComponentType(
                    "ingredient_state",
                    builder -> builder.persistent(IngredientStateData.CODEC)
            );

    private JazzyDataComponents() {
    }
}
