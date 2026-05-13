package com.boaat.jazzy_cookin.recipebook.client;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.recipebook.JazzyRecipeBookPlanner;
import com.boaat.jazzy_cookin.recipebook.JazzyRecipeBookSelection;
import com.boaat.jazzy_cookin.recipebook.RecipeBookDisplayUtil;
import com.boaat.jazzy_cookin.recipebook.network.RecipeBookSelectionPayload;
import com.boaat.jazzy_cookin.recipebook.network.RecipeBookSyncPayload;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RecipeBookClientState {
    private static final int OVERLAY_WIDTH = 168;
    private static final int OVERLAY_HEIGHT = 86;
    private static final KeyMapping OPEN_BOOK_KEY = new KeyMapping(
            "key.jazzycookin.open_recipe_book",
            GLFW.GLFW_KEY_J,
            "key.categories.jazzycookin"
    );
    private static final KeyMapping PREVIOUS_STEP_KEY = new KeyMapping(
            "key.jazzycookin.recipe_book_previous_step",
            GLFW.GLFW_KEY_LEFT_BRACKET,
            "key.categories.jazzycookin"
    );
    private static final KeyMapping NEXT_STEP_KEY = new KeyMapping(
            "key.jazzycookin.recipe_book_next_step",
            GLFW.GLFW_KEY_RIGHT_BRACKET,
            "key.categories.jazzycookin"
    );

    private static JazzyRecipeBookPlanner planner;
    private static JazzyRecipeBookSelection activeSelection;
    private static String focusedStepId = "";
    private static ResourceLocation lastViewedItemId;
    private static IngredientState lastViewedState;
    private static String lastViewedChainKey = "";
    private static final Set<String> completedStepIds = new LinkedHashSet<>();

    private RecipeBookClientState() {
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_BOOK_KEY);
        event.register(PREVIOUS_STEP_KEY);
        event.register(NEXT_STEP_KEY);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_BOOK_KEY.consumeClick()) {
            if (minecraft.player != null) {
                minecraft.setScreen(new JazzyRecipeBookScreen());
            }
        }
        while (PREVIOUS_STEP_KEY.consumeClick()) {
            focusPinnedStep(-1);
        }
        while (NEXT_STEP_KEY.consumeClick()) {
            focusPinnedStep(1);
        }
    }

    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        planner = minecraft.level != null ? JazzyRecipeBookPlanner.create(minecraft.level) : null;
    }

    public static void onRenderHud(RenderGuiLayerEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null || minecraft.options.hideGui) {
            return;
        }
        if (!VanillaGuiLayers.HOTBAR.equals(event.getName())) {
            return;
        }

        Optional<JazzyRecipeBookPlanner.Plan> optionalPlan = activePlan();
        if (optionalPlan.isEmpty()) {
            return;
        }

        JazzyRecipeBookPlanner.Plan plan = optionalPlan.get();
        Set<String> completed = completedStepIds();
        JazzyRecipeBookPlanner.PlanStep currentStep = exactOrCurrentStep(plan, completed);
        if (currentStep == null) {
            return;
        }

        int currentIndex = Math.max(0, plan.focusedStepIndex(completed, focusedStepId()));
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int x = 8;
        int y = minecraft.getWindow().getGuiScaledHeight() - OVERLAY_HEIGHT - 48;
        guiGraphics.fill(x, y, x + OVERLAY_WIDTH, y + OVERLAY_HEIGHT, 0xCC101318);
        guiGraphics.fill(x, y, x + OVERLAY_WIDTH, y + 12, 0xFF1A2530);
        guiGraphics.fill(x, y + 12, x + OVERLAY_WIDTH, y + 13, 0xFF5CC8D0);
        guiGraphics.drawString(minecraft.font, Component.translatable("screen.jazzycookin.recipe_book.overlay"), x + 6, y + 2, 0xFFF0F2F5, false);

        ItemStack targetStack = plan.targetStack();
        guiGraphics.renderItem(targetStack, x + 6, y + 18);
        guiGraphics.drawString(minecraft.font, trim(minecraft, targetStack.getHoverName().getString(), OVERLAY_WIDTH - 30), x + 26, y + 20, 0xFFDCE0E8, false);
        guiGraphics.drawString(
                minecraft.font,
                Component.literal((currentIndex + 1) + "/" + plan.steps().size()),
                x + OVERLAY_WIDTH - 20,
                y + 20,
                0xFF8C95A6,
                false
        );

        ItemStack stepStack = currentStep.outputStack();
        guiGraphics.renderItem(stepStack, x + 6, y + 38);
        guiGraphics.drawString(minecraft.font, trim(minecraft, stepStack.getHoverName().getString(), OVERLAY_WIDTH - 30), x + 26, y + 40, 0xFFDCE0E8, false);

        JazzyRecipeBookPlanner.StepOption option = currentStep.options().get(0);
        int lineY = y + 54;
        if (!option.requirements().isEmpty()) {
            guiGraphics.drawString(minecraft.font, trim(minecraft, firstRequirementLine(option), OVERLAY_WIDTH - 12), x + 6, lineY, 0xFF8C95A6, false);
            lineY += 10;
        }
        String note = firstNoteLine(option);
        if (!note.isEmpty()) {
            guiGraphics.drawString(minecraft.font, trim(minecraft, note, OVERLAY_WIDTH - 12), x + 6, lineY, 0xFF4ADE80, false);
            lineY += 10;
        }
        if (currentStep.options().size() > 1) {
            guiGraphics.drawString(
                    minecraft.font,
                    Component.translatable("screen.jazzycookin.recipe_book.alternatives", currentStep.options().size()),
                    x + 6,
                    lineY,
                    0xFFF0B429,
                    false
            );
        } else if (plan.isComplete(completed)) {
            guiGraphics.drawString(minecraft.font, Component.translatable("screen.jazzycookin.recipe_book.complete"), x + 6, lineY, 0xFF4ADE80, false);
        }
    }

    public static JazzyRecipeBookPlanner planner() {
        Minecraft minecraft = Minecraft.getInstance();
        if (planner == null && minecraft.level != null) {
            planner = JazzyRecipeBookPlanner.create(minecraft.level);
        }
        return planner;
    }

    public static void openRecipeBook() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.setScreen(new JazzyRecipeBookScreen());
        }
    }

    public static void applySync(RecipeBookSyncPayload payload) {
        activeSelection = payload.selection();
        focusedStepId = payload.focusedStepId() == null ? "" : payload.focusedStepId();
        completedStepIds.clear();
        completedStepIds.addAll(payload.completedStepIds());
        if (activeSelection != null) {
            rememberSelection(activeSelection);
        }
    }

    public static void rememberSelection(JazzyRecipeBookSelection selection) {
        lastViewedItemId = selection.itemId();
        lastViewedState = selection.state();
        lastViewedChainKey = selection.normalizedChainKey();
    }

    public static Optional<JazzyRecipeBookSelection> lastViewedSelection() {
        if (lastViewedItemId == null || lastViewedState == null) {
            return Optional.empty();
        }
        return Optional.of(new JazzyRecipeBookSelection(lastViewedItemId, lastViewedState, lastViewedChainKey));
    }

    public static @Nullable JazzyRecipeBookSelection activeSelection() {
        return activeSelection;
    }

    public static @Nullable String focusedStepId() {
        return focusedStepId.isBlank() ? null : focusedStepId;
    }

    public static Set<String> completedStepIds() {
        return Set.copyOf(completedStepIds);
    }

    public static Optional<JazzyRecipeBookPlanner.Plan> activePlan() {
        return activeSelection == null ? Optional.empty() : Optional.ofNullable(planner()).flatMap(value -> value.planFor(activeSelection));
    }

    public static void pinSelection(JazzyRecipeBookSelection selection) {
        pinSelection(selection, null);
    }

    public static void pinSelection(JazzyRecipeBookSelection selection, @Nullable String stepId) {
        rememberSelection(selection);
        PacketDistributor.sendToServer(RecipeBookSelectionPayload.pin(selection, stepId));
    }

    public static boolean previousStepKeyMatches(int keyCode, int scanCode) {
        return PREVIOUS_STEP_KEY.matches(keyCode, scanCode);
    }

    public static boolean nextStepKeyMatches(int keyCode, int scanCode) {
        return NEXT_STEP_KEY.matches(keyCode, scanCode);
    }

    public static boolean focusPinnedStep(int delta) {
        if (activeSelection == null) {
            return false;
        }
        Optional<JazzyRecipeBookPlanner.Plan> optionalPlan = activePlan();
        if (optionalPlan.isEmpty()) {
            return false;
        }
        JazzyRecipeBookPlanner.Plan plan = optionalPlan.get();
        if (plan.steps().isEmpty()) {
            return false;
        }
        int currentIndex = focusedStepIndex(plan, completedStepIds(), focusedStepId());
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        int nextIndex = Math.max(0, Math.min(plan.steps().size() - 1, currentIndex + delta));
        if (nextIndex == currentIndex) {
            return false;
        }
        focusedStepId = plan.steps().get(nextIndex).id();
        pinSelection(activeSelection, focusedStepId);
        return true;
    }

    private static int focusedStepIndex(JazzyRecipeBookPlanner.Plan plan, Set<String> completed, @Nullable String preferredStepId) {
        if (preferredStepId != null && !preferredStepId.isBlank()) {
            int exactIndex = plan.indexOfStep(preferredStepId);
            if (exactIndex >= 0) {
                return exactIndex;
            }
        }
        return plan.focusedStepIndex(completed, preferredStepId);
    }

    private static @Nullable JazzyRecipeBookPlanner.PlanStep exactOrCurrentStep(JazzyRecipeBookPlanner.Plan plan, Set<String> completed) {
        String preferredStepId = focusedStepId();
        if (preferredStepId != null && !preferredStepId.isBlank()) {
            Optional<JazzyRecipeBookPlanner.PlanStep> exactStep = plan.step(preferredStepId);
            if (exactStep.isPresent()) {
                return exactStep.get();
            }
        }
        return plan.focusedStep(completed, preferredStepId);
    }

    public static void unpinSelection() {
        PacketDistributor.sendToServer(RecipeBookSelectionPayload.unpin());
    }

    public static String chainLabel(String chainKey) {
        if (chainKey == null || chainKey.isBlank()) {
            return Component.translatable("screen.jazzycookin.recipe_book.default_chain").getString();
        }
        if (chainKey.startsWith("schema:")) {
            return "Flexible " + chainLabel(chainKey.substring("schema:".length()));
        }
        ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(JazzyCookin.MODID, chainKey);
        Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId);
        if (item != null && item != net.minecraft.world.item.Items.AIR) {
            return new ItemStack(item).getHoverName().getString();
        }
        return Arrays.stream(chainKey.split("_"))
                .map(part -> part.isEmpty() ? part : part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1))
                .reduce((left, right) -> left + " " + right)
                .orElse(chainKey);
    }

    private static String firstRequirementLine(JazzyRecipeBookPlanner.StepOption option) {
        JazzyRecipeBookPlanner.Requirement requirement = option.requirements().get(0);
        ItemStack choice = requirement.choices().isEmpty() ? ItemStack.EMPTY : requirement.choices().get(0);
        String count = requirement.count() > 1 ? requirement.count() + "x " : "";
        String name = choice.isEmpty() ? "Unknown" : choice.getHoverName().getString();
        return count + name;
    }

    private static String firstNoteLine(JazzyRecipeBookPlanner.StepOption option) {
        if (!option.notes().isEmpty()) {
            return option.notes().get(0);
        }
        if (option.station() != null) {
            return option.station().displayName().getString();
        }
        return "";
    }

    private static String trim(Minecraft minecraft, String text, int width) {
        return minecraft.font.plainSubstrByWidth(text, Math.max(0, width));
    }
}
