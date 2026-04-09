package com.boaat.jazzy_cookin.registry;

import java.util.List;
import java.util.Optional;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.recipe.KitchenPlateInput;
import com.boaat.jazzy_cookin.recipe.KitchenPlateRecipe;
import com.boaat.jazzy_cookin.recipe.KitchenPlateSerializer;
import com.boaat.jazzy_cookin.recipe.KitchenProcessInput;
import com.boaat.jazzy_cookin.recipe.KitchenProcessRecipe;
import com.boaat.jazzy_cookin.recipe.KitchenProcessSerializer;
import com.boaat.jazzy_cookin.recipe.KitchenRecipeMatchPlan;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class JazzyRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, JazzyCookin.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, JazzyCookin.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<KitchenProcessRecipe>> KITCHEN_PROCESS_TYPE = RECIPE_TYPES.register(
            "kitchen_process",
            () -> namedRecipeType("kitchen_process")
    );
    public static final DeferredHolder<RecipeType<?>, RecipeType<KitchenPlateRecipe>> KITCHEN_PLATE_TYPE = RECIPE_TYPES.register(
            "kitchen_plate",
            () -> namedRecipeType("kitchen_plate")
    );

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<KitchenProcessRecipe>> KITCHEN_PROCESS_SERIALIZER =
            RECIPE_SERIALIZERS.register("kitchen_process", KitchenProcessSerializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<KitchenPlateRecipe>> KITCHEN_PLATE_SERIALIZER =
            RECIPE_SERIALIZERS.register("kitchen_plate", KitchenPlateSerializer::new);

    private JazzyRecipes() {
    }

    private static <T extends Recipe<?>> RecipeType<T> namedRecipeType(String name) {
        return new RecipeType<>() {
            @Override
            public String toString() {
                return JazzyCookin.MODID + ":" + name;
            }
        };
    }

    public static Optional<KitchenProcessRecipe> findProcessRecipe(
            Level level,
            StationType station,
            List<ItemStack> inputs,
            ItemStack tool,
            HeatLevel heat,
            boolean preheated
    ) {
        KitchenProcessInput recipeInput = new KitchenProcessInput(inputs, tool, station, heat, preheated);
        KitchenProcessRecipe bestRecipe = null;
        KitchenRecipeMatchPlan bestPlan = null;
        for (RecipeHolder<KitchenProcessRecipe> holder : level.getRecipeManager().getAllRecipesFor(KITCHEN_PROCESS_TYPE.get())) {
            KitchenRecipeMatchPlan plan = holder.value().matchPlan(recipeInput, level).orElse(null);
            if (plan != null && plan.betterThan(bestPlan)) {
                bestRecipe = holder.value();
                bestPlan = plan;
            }
        }
        return Optional.ofNullable(bestRecipe);
    }

    public static Optional<KitchenProcessRecipe> findProcessRecipeCandidate(
            Level level,
            StationType station,
            List<ItemStack> inputs,
            ItemStack tool
    ) {
        KitchenProcessRecipe bestRecipe = null;
        KitchenRecipeMatchPlan bestPlan = null;
        for (RecipeHolder<KitchenProcessRecipe> holder : level.getRecipeManager().getAllRecipesFor(KITCHEN_PROCESS_TYPE.get())) {
            KitchenRecipeMatchPlan plan = holder.value().candidatePlan(station, inputs, tool, level).orElse(null);
            if (plan != null && plan.betterThan(bestPlan)) {
                bestRecipe = holder.value();
                bestPlan = plan;
            }
        }
        return Optional.ofNullable(bestRecipe);
    }

    public static Optional<KitchenPlateRecipe> findPlateRecipe(Level level, List<ItemStack> inputs) {
        KitchenPlateInput recipeInput = new KitchenPlateInput(inputs);
        KitchenPlateRecipe bestRecipe = null;
        KitchenRecipeMatchPlan bestPlan = null;
        for (RecipeHolder<KitchenPlateRecipe> holder : level.getRecipeManager().getAllRecipesFor(KITCHEN_PLATE_TYPE.get())) {
            KitchenRecipeMatchPlan plan = holder.value().matchPlan(recipeInput, level).orElse(null);
            if (plan != null && plan.betterThan(bestPlan)) {
                bestRecipe = holder.value();
                bestPlan = plan;
            }
        }
        return Optional.ofNullable(bestRecipe);
    }
}
