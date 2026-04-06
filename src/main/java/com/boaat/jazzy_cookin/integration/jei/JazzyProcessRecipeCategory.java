package com.boaat.jazzy_cookin.integration.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.boaat.jazzy_cookin.kitchen.KitchenOutcomeBand;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.recipe.KitchenInputRequirement;
import com.boaat.jazzy_cookin.recipe.KitchenProcessOutcome;
import com.boaat.jazzy_cookin.recipe.KitchenProcessRecipe;

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
import net.minecraft.world.item.ItemStack;

public class JazzyProcessRecipeCategory implements IRecipeCategory<KitchenProcessRecipe> {
    private static final int WIDTH = 182;
    private static final int HEIGHT = 94;

    private final IDrawable icon;
    private final IDrawable arrow;
    private final IDrawableAnimated animatedArrow;

    public JazzyProcessRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(JazzyJeiStackUtil.stationStack(StationType.PREP_TABLE));
        this.arrow = guiHelper.getRecipeArrow();
        this.animatedArrow = guiHelper.createAnimatedRecipeArrow(80);
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<KitchenProcessRecipe> getRecipeType() {
        return JazzyJeiRecipeTypes.KITCHEN_PROCESS;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.jazzycookin.kitchen_process");
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
    public void setRecipe(IRecipeLayoutBuilder builder, KitchenProcessRecipe recipe, IFocusGroup focuses) {
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

        List<ItemStack> toolStacks = JazzyJeiStackUtil.toolStacks(recipe.allowedToolsOrPreferred());
        if (!toolStacks.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.CATALYST, 4, 56)
                    .setStandardSlotBackground()
                    .addItemStacks(toolStacks)
                    .addRichTooltipCallback((view, tooltip) -> {
                        if (recipe.preferredTool().isPresent()) {
                            tooltip.add(Component.translatable("jei.jazzycookin.preferred_tool", JazzyJeiStackUtil.toolLabel(recipe.preferredTool().get())));
                        }
                        if (recipe.allowedTools().size() > 1) {
                            tooltip.add(Component.translatable("jei.jazzycookin.allowed_tools"));
                            recipe.allowedTools().stream()
                                    .map(JazzyJeiStackUtil::toolLabel)
                                    .forEach(tooltip::add);
                        }
                    });
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, 88, 4)
                .setOutputSlotBackground()
                .addItemStack(JazzyJeiStackUtil.displayOutput(recipe.output()))
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("jei.jazzycookin.outcome.ideal"));
                    tooltip.add(Component.translatable("jei.jazzycookin.output_state", JazzyJeiStackUtil.stateLabel(recipe.output().state())));
                });

        if (!recipe.output().byproduct().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 108, 4)
                    .setOutputSlotBackground()
                    .addItemStack(recipe.output().byproduct().copy())
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("screen.jazzycookin.byproduct")));
        }

        Optional<KitchenProcessOutcome> underOutcome = recipe.outcomes().stream()
                .filter(outcome -> outcome.band() == KitchenOutcomeBand.UNDER)
                .findFirst();
        Optional<KitchenProcessOutcome> overOutcome = recipe.outcomes().stream()
                .filter(outcome -> outcome.band() == KitchenOutcomeBand.OVER)
                .findFirst();

        underOutcome.ifPresent(outcome -> builder.addSlot(RecipeIngredientRole.OUTPUT, 88, 24)
                .setOutputSlotBackground()
                .addItemStack(JazzyJeiStackUtil.displayOutput(outcome.output()))
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("jei.jazzycookin.outcome.under"));
                    tooltip.add(Component.translatable("jei.jazzycookin.output_state", JazzyJeiStackUtil.stateLabel(outcome.output().state())));
                }));
        overOutcome.ifPresent(outcome -> builder.addSlot(RecipeIngredientRole.OUTPUT, 108, 24)
                .setOutputSlotBackground()
                .addItemStack(JazzyJeiStackUtil.displayOutput(outcome.output()))
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("jei.jazzycookin.outcome.over"));
                    tooltip.add(Component.translatable("jei.jazzycookin.output_state", JazzyJeiStackUtil.stateLabel(outcome.output().state())));
                }));
    }

    @Override
    public void draw(KitchenProcessRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.arrow.draw(guiGraphics, 58, 12);
        this.animatedArrow.draw(guiGraphics, 58, 12);

        guiGraphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable("jei.jazzycookin.station", recipe.station().displayName()),
                4,
                78,
                0x404040,
                false
        );
        guiGraphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable("jei.jazzycookin.method", recipe.method().displayName()),
                4,
                88,
                0x404040,
                false
        );

        List<Component> detailLine = new ArrayList<>();
        detailLine.add(Component.translatable("jei.jazzycookin.duration_ticks", recipe.effectiveDuration()));
        detailLine.add(Component.translatable("jei.jazzycookin.mode." + recipe.mode().getSerializedName()));
        if (recipe.usesHeat()) {
            detailLine.add(Component.translatable("jei.jazzycookin.heat", heatLabel(recipe)));
        }
        if (recipe.requiresPreheat()) {
            detailLine.add(Component.translatable("jei.jazzycookin.preheat_required"));
        }
        if (recipe.requiresNearbyWater()) {
            detailLine.add(Component.translatable("jei.jazzycookin.nearby_water"));
        }

        String joined = detailLine.stream()
                .map(Component::getString)
                .collect(Collectors.joining(" | "));
        guiGraphics.drawString(Minecraft.getInstance().font, joined, 4, 68, 0x707070, false);

        if (!recipe.outcomes().isEmpty()) {
            guiGraphics.drawString(Minecraft.getInstance().font, "U", 94, 30, 0x707070, false);
            guiGraphics.drawString(Minecraft.getInstance().font, "O", 114, 30, 0x707070, false);
        }
    }

    private static Component heatLabel(KitchenProcessRecipe recipe) {
        if (recipe.minimumHeat() == recipe.maximumHeat()) {
            return Component.translatable("heat.jazzycookin." + recipe.minimumHeat().getSerializedName());
        }
        return Component.translatable(
                "jei.jazzycookin.heat_range",
                Component.translatable("heat.jazzycookin." + recipe.minimumHeat().getSerializedName()),
                Component.translatable("heat.jazzycookin." + recipe.maximumHeat().getSerializedName())
        );
    }
}
