package com.boaat.jazzy_cookin.recipebook;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.boaat.jazzy_cookin.kitchen.IngredientState;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class RecipeBookProgress {
    public record SyncState(@Nullable JazzyRecipeBookSelection selection, List<String> completedStepIds, @Nullable String focusedStepId) {
    }

    private static final String ROOT_KEY = "jazzycookin_recipe_book";
    private static final String ITEM_ID_KEY = "item_id";
    private static final String STATE_KEY = "state";
    private static final String CHAIN_KEY = "chain_key";
    private static final String COMPLETED_STEPS_KEY = "completed_steps";
    private static final String FOCUSED_STEP_KEY = "focused_step_id";

    private RecipeBookProgress() {
    }

    public static CompoundTag get(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            root.put(ROOT_KEY, new CompoundTag());
        }
        return root.getCompound(ROOT_KEY);
    }

    public static Optional<JazzyRecipeBookSelection> selection(Player player) {
        CompoundTag data = get(player);
        if (!data.contains(ITEM_ID_KEY, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        return Optional.of(new JazzyRecipeBookSelection(
                net.minecraft.resources.ResourceLocation.parse(data.getString(ITEM_ID_KEY)),
                IngredientState.byName(data.getString(STATE_KEY)),
                data.getString(CHAIN_KEY)
        ));
    }

    public static Optional<String> focusedStepId(Player player) {
        CompoundTag data = get(player);
        if (!data.contains(FOCUSED_STEP_KEY, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        String focusedStepId = data.getString(FOCUSED_STEP_KEY);
        return focusedStepId.isBlank() ? Optional.empty() : Optional.of(focusedStepId);
    }

    public static void pin(Player player, JazzyRecipeBookSelection selection) {
        pin(player, selection, null);
    }

    public static void pin(Player player, JazzyRecipeBookSelection selection, @Nullable String requestedFocusedStepId) {
        Optional<JazzyRecipeBookSelection> existingSelection = selection(player);
        Set<String> completed = existingSelection.filter(selection::equals).map(ignored -> completedSteps(player)).orElseGet(LinkedHashSet::new);

        CompoundTag data = get(player);
        data.putString(ITEM_ID_KEY, selection.itemId().toString());
        data.putString(STATE_KEY, selection.state().getSerializedName());
        data.putString(CHAIN_KEY, selection.normalizedChainKey());
        storeCompletedSteps(player, completed);

        if (player instanceof ServerPlayer serverPlayer) {
            reconcilePinnedGuide(serverPlayer, requestedFocusedStepId);
        } else {
            storeFocusedStepId(player, requestedFocusedStepId == null ? "" : requestedFocusedStepId);
        }
    }

    public static void unpin(Player player) {
        player.getPersistentData().remove(ROOT_KEY);
    }

    public static Set<String> completedSteps(Player player) {
        CompoundTag data = get(player);
        Set<String> completed = new LinkedHashSet<>();
        ListTag list = data.getList(COMPLETED_STEPS_KEY, Tag.TAG_STRING);
        for (int index = 0; index < list.size(); index++) {
            completed.add(list.getString(index));
        }
        return completed;
    }

    public static SyncState syncState(Player player) {
        return new SyncState(selection(player).orElse(null), List.copyOf(completedSteps(player)), focusedStepId(player).orElse(null));
    }

    public static void copyToClone(Player original, Player clone) {
        if (original.getPersistentData().contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            clone.getPersistentData().put(ROOT_KEY, original.getPersistentData().getCompound(ROOT_KEY).copy());
        }
    }

    public static boolean recordCraft(ServerPlayer player, ItemStack craftedStack) {
        return recordOutput(player, RecipeBookDisplayUtil.outputKey(craftedStack, RecipeBookDisplayUtil.defaultStateForItem(craftedStack.getItem())), "");
    }

    public static boolean recordSourceHarvest(ServerPlayer player, ItemStack harvestedStack, String chainKey) {
        return recordOutput(player, RecipeBookDisplayUtil.outputKeyFromActualStack(harvestedStack, player.level().getGameTime()), chainKey);
    }

    public static boolean recordKitchenOutput(ServerPlayer player, ItemStack outputStack, String chainKey) {
        return recordOutput(player, RecipeBookDisplayUtil.outputKeyFromActualStack(outputStack, player.level().getGameTime()), chainKey);
    }

    public static boolean reconcilePinnedGuide(ServerPlayer player) {
        return reconcilePinnedGuide(player, null);
    }

    public static boolean reconcilePinnedGuide(ServerPlayer player, @Nullable String requestedFocusedStepId) {
        Optional<JazzyRecipeBookSelection> selection = selection(player);
        if (selection.isEmpty()) {
            return false;
        }

        JazzyRecipeBookPlanner planner = JazzyRecipeBookPlanner.create(player.level());
        Optional<JazzyRecipeBookPlanner.Plan> optionalPlan = planner.planFor(selection.get());
        if (optionalPlan.isEmpty()) {
            return storeFocusedStepId(player, "");
        }

        JazzyRecipeBookPlanner.Plan plan = optionalPlan.get();
        Set<String> completed = completedSteps(player);
        boolean changed = markInventorySatisfiedSteps(player, plan, completed);
        if (changed) {
            storeCompletedSteps(player, completed);
        }

        String preferredStepId = requestedFocusedStepId != null ? requestedFocusedStepId : focusedStepId(player).orElse("");
        String resolvedFocusedStepId = resolveFocusedStepId(plan, completed, preferredStepId);
        return storeFocusedStepId(player, resolvedFocusedStepId) || changed;
    }

    private static boolean recordOutput(ServerPlayer player, JazzyRecipeBookPlanner.OutputKey outputKey, String chainKey) {
        Optional<JazzyRecipeBookSelection> selection = selection(player);
        if (selection.isEmpty()) {
            return false;
        }

        JazzyRecipeBookPlanner planner = JazzyRecipeBookPlanner.create(player.level());
        Optional<JazzyRecipeBookPlanner.Plan> optionalPlan = planner.planFor(selection.get());
        if (optionalPlan.isEmpty()) {
            return false;
        }

        JazzyRecipeBookPlanner.Plan plan = optionalPlan.get();
        Set<String> completed = completedSteps(player);
        JazzyRecipeBookPlanner.PlanStep matchedStep = null;
        for (JazzyRecipeBookPlanner.PlanStep step : plan.steps()) {
            if (!step.outputKey().equals(outputKey)) {
                continue;
            }
            if (!chainKey.isBlank() && !step.chainKey().equals(chainKey) && !step.chainKey().isBlank()) {
                continue;
            }
            matchedStep = step;
            break;
        }
        if (matchedStep == null || completed.contains(matchedStep.id())) {
            return false;
        }

        completed.add(matchedStep.id());
        storeCompletedSteps(player, completed);
        String resolvedFocusedStepId = resolveFocusedStepId(plan, completed, focusedStepId(player).orElse(matchedStep.id()));
        storeFocusedStepId(player, resolvedFocusedStepId);
        return true;
    }

    private static boolean markInventorySatisfiedSteps(ServerPlayer player, JazzyRecipeBookPlanner.Plan plan, Set<String> completed) {
        boolean changed = false;
        for (JazzyRecipeBookPlanner.PlanStep step : plan.steps()) {
            if (completed.contains(step.id())) {
                continue;
            }
            if (inventoryHasOutput(player, step)) {
                completed.add(step.id());
                changed = true;
            }
        }
        return changed;
    }

    private static boolean inventoryHasOutput(ServerPlayer player, JazzyRecipeBookPlanner.PlanStep step) {
        int requiredCount = Math.max(1, step.outputStack().getCount());
        int foundCount = 0;
        long gameTime = player.level().getGameTime();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            JazzyRecipeBookPlanner.OutputKey stackKey = RecipeBookDisplayUtil.outputKeyFromActualStack(stack, gameTime);
            if (!stackKey.equals(step.outputKey())) {
                continue;
            }
            foundCount += stack.getCount();
            if (foundCount >= requiredCount) {
                return true;
            }
        }
        return false;
    }

    private static String resolveFocusedStepId(JazzyRecipeBookPlanner.Plan plan, Set<String> completedStepIds, @Nullable String preferredStepId) {
        JazzyRecipeBookPlanner.PlanStep focusedStep = plan.focusedStep(completedStepIds, preferredStepId);
        return focusedStep == null ? "" : focusedStep.id();
    }

    private static void storeCompletedSteps(Player player, Set<String> completedStepIds) {
        ListTag list = new ListTag();
        completedStepIds.forEach(stepId -> list.add(StringTag.valueOf(stepId)));
        get(player).put(COMPLETED_STEPS_KEY, list);
    }

    private static boolean storeFocusedStepId(Player player, @Nullable String focusedStepId) {
        CompoundTag data = get(player);
        String normalized = Objects.requireNonNullElse(focusedStepId, "");
        if (normalized.isBlank()) {
            if (data.contains(FOCUSED_STEP_KEY, Tag.TAG_STRING)) {
                data.remove(FOCUSED_STEP_KEY);
                return true;
            }
            return false;
        }
        if (normalized.equals(data.getString(FOCUSED_STEP_KEY))) {
            return false;
        }
        data.putString(FOCUSED_STEP_KEY, normalized);
        return true;
    }
}
