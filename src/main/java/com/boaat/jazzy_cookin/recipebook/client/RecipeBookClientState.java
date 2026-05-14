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
import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaManager;
import com.boaat.jazzy_cookin.recipebook.JazzyRecipeBookPlanner;
import com.boaat.jazzy_cookin.recipebook.JazzyRecipeBookSelection;
import com.boaat.jazzy_cookin.recipebook.RecipeBookDisplayUtil;
import com.boaat.jazzy_cookin.recipebook.network.RecipeBookSchemaRequestPayload;
import com.boaat.jazzy_cookin.recipebook.network.RecipeBookSchemaSyncPayload;
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
    private record InstructionRow(
            JazzyRecipeBookPlanner.PlanStep step,
            int instructionIndex,
            ItemStack icon,
            String title,
            String detail
    ) {
    }

    private static final int OVERLAY_WIDTH = 196;
    private static final int OVERLAY_HEIGHT = 96;
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
    private static int focusedInstructionIndex;
    private static ResourceLocation lastViewedItemId;
    private static IngredientState lastViewedState;
    private static String lastViewedChainKey = "";
    private static boolean requestedServerSchemas;
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
        if (minecraft.player == null || minecraft.level == null) {
            requestedServerSchemas = false;
        }
        if (minecraft.player != null && minecraft.level != null && !requestedServerSchemas) {
            requestedServerSchemas = true;
            PacketDistributor.sendToServer(RecipeBookSchemaRequestPayload.INSTANCE);
        }
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
        if (minecraft.player != null) {
            requestedServerSchemas = true;
            PacketDistributor.sendToServer(RecipeBookSchemaRequestPayload.INSTANCE);
        }
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
        List<InstructionRow> rows = instructionRows(plan);
        if (rows.isEmpty()) {
            return;
        }

        int currentIndex = focusedInstructionIndex(plan, rows, completed);
        InstructionRow currentRow = rows.get(currentIndex);
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
                Component.literal((currentIndex + 1) + "/" + rows.size()),
                x + OVERLAY_WIDTH - 34,
                y + 20,
                0xFF8C95A6,
                false
        );

        guiGraphics.renderItem(currentRow.icon(), x + 6, y + 38);
        int titleColor = completed.contains(currentRow.step().id()) ? 0xFF4ADE80 : 0xFFDCE0E8;
        guiGraphics.drawString(minecraft.font, trim(minecraft, currentRow.title(), OVERLAY_WIDTH - 30), x + 26, y + 40, titleColor, false);
        guiGraphics.drawString(minecraft.font, trim(minecraft, currentRow.detail(), OVERLAY_WIDTH - 12), x + 6, y + 56, 0xFF8C95A6, false);
        guiGraphics.drawString(minecraft.font, trim(minecraft, "Goal: " + currentRow.step().outputStack().getHoverName().getString(), OVERLAY_WIDTH - 12), x + 6, y + 68, 0xFF6FAAB0, false);
        if (plan.isComplete(completed)) {
            guiGraphics.drawString(minecraft.font, Component.translatable("screen.jazzycookin.recipe_book.complete"), x + 6, y + 80, 0xFF4ADE80, false);
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
        String previousFocusedStepId = focusedStepId;
        activeSelection = payload.selection();
        focusedStepId = payload.focusedStepId() == null ? "" : payload.focusedStepId();
        if (!focusedStepId.equals(previousFocusedStepId)) {
            focusedInstructionIndex = 0;
        }
        completedStepIds.clear();
        completedStepIds.addAll(payload.completedStepIds());
        if (activeSelection != null) {
            rememberSelection(activeSelection);
        }
    }

    public static void applySchemaSync(RecipeBookSchemaSyncPayload payload) {
        DishSchemaManager.loadSyncedSchemas(payload.schemas());
        Minecraft minecraft = Minecraft.getInstance();
        planner = minecraft.level != null ? JazzyRecipeBookPlanner.create(minecraft.level) : null;
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
        List<InstructionRow> rows = instructionRows(plan);
        if (rows.isEmpty()) {
            return false;
        }
        int currentIndex = focusedInstructionIndex(plan, rows, completedStepIds());
        int nextIndex = Math.max(0, Math.min(rows.size() - 1, currentIndex + delta));
        if (nextIndex == currentIndex) {
            return false;
        }
        InstructionRow row = rows.get(nextIndex);
        focusedStepId = row.step().id();
        focusedInstructionIndex = row.instructionIndex();
        pinSelection(activeSelection, focusedStepId);
        return true;
    }

    private static int focusedInstructionIndex(JazzyRecipeBookPlanner.Plan plan, List<InstructionRow> rows, Set<String> completed) {
        String preferredStepId = focusedStepId();
        int firstMatchingStep = -1;
        if (preferredStepId != null && !preferredStepId.isBlank()) {
            for (int index = 0; index < rows.size(); index++) {
                InstructionRow row = rows.get(index);
                if (row.step().id().equals(preferredStepId)) {
                    if (row.instructionIndex() == focusedInstructionIndex) {
                        return index;
                    }
                    if (firstMatchingStep < 0) {
                        firstMatchingStep = index;
                    }
                }
            }
            if (firstMatchingStep >= 0) {
                return firstMatchingStep;
            }
        }
        JazzyRecipeBookPlanner.PlanStep focusedStep = plan.focusedStep(completed, preferredStepId);
        if (focusedStep != null) {
            for (int index = 0; index < rows.size(); index++) {
                if (rows.get(index).step().id().equals(focusedStep.id())) {
                    return index;
                }
            }
        }
        return 0;
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

    private static List<InstructionRow> instructionRows(JazzyRecipeBookPlanner.Plan plan) {
        java.util.ArrayList<InstructionRow> rows = new java.util.ArrayList<>();
        for (JazzyRecipeBookPlanner.PlanStep step : plan.steps()) {
            rows.addAll(instructionRowsFor(step));
        }
        return rows;
    }

    private static List<InstructionRow> instructionRowsFor(JazzyRecipeBookPlanner.PlanStep step) {
        if (step.options().isEmpty()) {
            return List.of(new InstructionRow(step, 0, step.outputStack().copy(),
                    "Obtain " + step.outputStack().getHoverName().getString(),
                    "Make or collect this item."));
        }
        JazzyRecipeBookPlanner.StepOption option = step.options().get(0);
        java.util.ArrayList<InstructionRow> rows = new java.util.ArrayList<>();
        int row = 0;
        for (JazzyRecipeBookPlanner.Requirement requirement : option.requirements()) {
            ItemStack icon = requirement.choices().isEmpty() ? step.outputStack().copy() : requirement.choices().get(0).copy();
            rows.add(new InstructionRow(step, row++, icon,
                    requirementTitle(step, requirement),
                    requirementDetail(requirement)));
        }
        String setup = controlSummary(option);
        if (option.station() != null || !setup.isBlank()) {
            rows.add(new InstructionRow(step, row++, step.outputStack().copy(),
                    setupTitle(option),
                    setup.isBlank() ? "Prepare the station for this step." : setup));
        }
        rows.add(new InstructionRow(step, row, step.outputStack().copy(), stepActionTitle(step, option), stepActionDetail(step)));
        return rows;
    }

    private static String requirementTitle(JazzyRecipeBookPlanner.PlanStep step, JazzyRecipeBookPlanner.Requirement requirement) {
        String item = compactRequirementLabel(requirement);
        return switch (step.kind()) {
            case PLATE, PROCESS -> "Add " + item;
            case CRAFT -> "Get " + item;
            case SOURCE -> "Have " + item;
        };
    }

    private static String requirementDetail(JazzyRecipeBookPlanner.Requirement requirement) {
        if (requirement.choices().size() > 1) {
            return requirement.choices().size() + " matching choices; this is suggested.";
        }
        return requirement.requiredState() == IngredientState.PANTRY_READY
                ? "Use this in the next action."
                : "State: " + Component.translatable("state.jazzycookin." + requirement.requiredState().getSerializedName()).getString();
    }

    private static String setupTitle(JazzyRecipeBookPlanner.StepOption option) {
        if (option.station() != null) {
            return "Set up " + option.station().displayName().getString();
        }
        if (option.method() != null) {
            return "Set up " + option.method().displayName().getString();
        }
        return "Set controls";
    }

    private static String stepActionTitle(JazzyRecipeBookPlanner.PlanStep step, JazzyRecipeBookPlanner.StepOption option) {
        String output = step.outputStack().getHoverName().getString();
        return switch (step.kind()) {
            case CRAFT -> "Craft " + output;
            case SOURCE -> "Collect " + output;
            case PLATE -> "Plate " + output;
            case PROCESS -> option.method() != null ? option.method().displayName().getString() + " " + output : "Make " + output;
        };
    }

    private static String stepActionDetail(JazzyRecipeBookPlanner.PlanStep step) {
        String output = step.outputStack().getHoverName().getString();
        return switch (step.kind()) {
            case CRAFT -> "Use the normal crafting recipe.";
            case SOURCE -> "Harvest or collect this output.";
            case PLATE -> "Start plating to finish " + output + ".";
            case PROCESS -> "Start the station and cook to target.";
        };
    }

    private static String controlSummary(JazzyRecipeBookPlanner.StepOption option) {
        java.util.ArrayList<String> parts = new java.util.ArrayList<>();
        if (option.preferredTool() != null && option.preferredTool() != ToolProfile.NONE) {
            parts.add("Tool: " + toolName(option.preferredTool()));
        }
        if (option.preferredHeat() != null && !"off".equals(option.preferredHeat().getSerializedName())) {
            parts.add("Heat: " + recipeHeatLabel(option).getString());
        }
        if (option.requiresPreheat()) {
            parts.add("Preheat");
        }
        if (option.durationTicks() > 0) {
            parts.add(option.durationTicks() + " ticks");
        }
        return String.join(" | ", parts);
    }

    private static String toolName(ToolProfile tool) {
        return Component.translatable("tool.jazzycookin." + tool.getSerializedName()).getString();
    }

    private static String compactRequirementLabel(JazzyRecipeBookPlanner.Requirement requirement) {
        String count = requirement.count() > 1 ? requirement.count() + "x " : "";
        if (requirement.choices().isEmpty()) {
            return count + Component.translatable("screen.jazzycookin.recipe_book.step").getString();
        }
        String name = requirement.choices().get(0).getHoverName().getString();
        if (requirement.choices().size() > 1) {
            return count + name + " +" + (requirement.choices().size() - 1);
        }
        return count + name;
    }

    private static Component recipeHeatLabel(JazzyRecipeBookPlanner.StepOption option) {
        if (usesNumberedHeat(option.station())) {
            return Component.literal(switch (option.preferredHeat()) {
                case LOW -> "1-2";
                case MEDIUM -> "3-4";
                case HIGH -> "5-6";
                default -> "1";
            });
        }
        HeatLevel heatLevel = option.preferredHeat();
        return Component.translatable("heat.jazzycookin." + heatLevel.getSerializedName());
    }

    private static boolean usesNumberedHeat(StationType stationType) {
        return stationType == StationType.STOVE || stationType == StationType.SMOKER || stationType == StationType.STEAMER;
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
