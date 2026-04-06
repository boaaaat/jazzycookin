package com.boaat.jazzy_cookin.integration.jei;

import java.util.List;

import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.recipe.KitchenInputRequirement;
import com.boaat.jazzy_cookin.recipe.KitchenPlateRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class JazzyPlateRecipeCategory implements IRecipeCategory<KitchenPlateRecipe> {
    private static final int WIDTH = 160;
    private static final int HEIGHT = 62;

    private final IDrawable icon;
    private final IDrawableAnimated animatedArrow;

    public JazzyPlateRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(JazzyJeiStackUtil.stationStack(StationType.PLATING_STATION));
        this.animatedArrow = guiHelper.createAnimatedRecipeArrow(40);
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<KitchenPlateRecipe> getRecipeType() {
        return JazzyJeiRecipeTypes.KITCHEN_PLATE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.jazzycookin.kitchen_plate");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, KitchenPlateRecipe recipe, IFocusGroup focuses) {
        List<KitchenInputRequirement> inputs = recipe.inputs();
        for (int index = 0; index < inputs.size(); index++) {
            int x = 4 + (index % 2) * 20;
            int y = 4 + (index / 2) * 20;
            KitchenInputRequirement requirement = inputs.get(index);
            builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                    .setStandardSlotBackground()
                    .addItemStacks(JazzyJeiStackUtil.displayStacks(requirement))
                    .addRichTooltipCallback((view, tooltip) -> {
                        tooltip.add(Component.translatable("jei.jazzycookin.required_state", JazzyJeiStackUtil.stateLabel(requirement.requiredState())));
                        if (requirement.count() > 1) {
                            tooltip.add(Component.translatable("jei.jazzycookin.required_count", requirement.count()));
                        }
                    });
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, 94, 14)
                .setOutputSlotBackground()
                .addItemStack(JazzyJeiStackUtil.displayOutput(recipe.output()))
                .addRichTooltipCallback((view, tooltip) ->
                        tooltip.add(Component.translatable("jei.jazzycookin.output_state", JazzyJeiStackUtil.stateLabel(recipe.output().state()))));
    }

    @Override
    public void draw(KitchenPlateRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.animatedArrow.draw(guiGraphics, 62, 16);
        guiGraphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable("jei.jazzycookin.station", StationType.PLATING_STATION.displayName()),
                4,
                52,
                0x404040,
                false
        );
    }
}
