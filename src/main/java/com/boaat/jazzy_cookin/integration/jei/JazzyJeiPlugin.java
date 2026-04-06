package com.boaat.jazzy_cookin.integration.jei;

import java.util.List;
import java.util.stream.Collectors;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.recipe.KitchenPlateRecipe;
import com.boaat.jazzy_cookin.recipe.KitchenProcessRecipe;
import com.boaat.jazzy_cookin.registry.JazzyItems;
import com.boaat.jazzy_cookin.registry.JazzyRecipes;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

@JeiPlugin
public class JazzyJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_UID = ResourceLocation.fromNamespaceAndPath(JazzyCookin.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        ISubtypeInterpreter<ItemStack> interpreter = new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack stack, UidContext context) {
                return stateKey(stack);
            }

            @Override
            @SuppressWarnings("deprecation")
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                String stateKey = stateKey(stack);
                return stateKey == null ? "" : stateKey;
            }
        };

        BuiltInRegistries.ITEM.stream()
                .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(JazzyCookin.MODID))
                .filter(KitchenIngredientItem.class::isInstance)
                .forEach(item -> registration.registerSubtypeInterpreter(item, interpreter));
    }

    private static String stateKey(ItemStack stack) {
        if (stack.getItem() instanceof KitchenIngredientItem ingredientItem) {
            var data = KitchenStackUtil.getData(stack);
            if (data != null) {
                return data.state().getSerializedName();
            }
            return ingredientItem.defaultData(0L).state().getSerializedName();
        }
        return null;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new JazzyProcessRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new JazzyPlateRecipeCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return;
        }

        List<KitchenProcessRecipe> processRecipes = connection.getRecipeManager()
                .getAllRecipesFor(JazzyRecipes.KITCHEN_PROCESS_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .collect(Collectors.toList());
        List<KitchenPlateRecipe> plateRecipes = connection.getRecipeManager()
                .getAllRecipesFor(JazzyRecipes.KITCHEN_PLATE_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .collect(Collectors.toList());

        registration.addRecipes(JazzyJeiRecipeTypes.KITCHEN_PROCESS, processRecipes);
        registration.addRecipes(JazzyJeiRecipeTypes.KITCHEN_PLATE, plateRecipes);

        registration.addItemStackInfo(new ItemStack(JazzyItems.PANTRY_ITEM.get()), net.minecraft.network.chat.Component.translatable("screen.jazzycookin.pantry_hint"));
        registration.addItemStackInfo(new ItemStack(JazzyItems.FRIDGE_ITEM.get()), net.minecraft.network.chat.Component.translatable("screen.jazzycookin.fridge_hint"));
        registration.addItemStackInfo(new ItemStack(JazzyItems.FREEZER_ITEM.get()), net.minecraft.network.chat.Component.translatable("screen.jazzycookin.freezer_hint"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (StationType stationType : StationType.values()) {
            if (stationType == StationType.PLATING_STATION) {
                registration.addRecipeCatalyst(JazzyJeiStackUtil.stationStack(stationType), JazzyJeiRecipeTypes.KITCHEN_PLATE);
            } else {
                registration.addRecipeCatalyst(JazzyJeiStackUtil.stationStack(stationType), JazzyJeiRecipeTypes.KITCHEN_PROCESS);
            }
        }
    }
}
