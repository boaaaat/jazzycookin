package com.boaat.jazzy_cookin.block.entity;

import java.util.List;
import java.util.Optional;

import com.boaat.jazzy_cookin.block.KitchenStationBlock;
import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.item.KitchenToolItem;
import com.boaat.jazzy_cookin.kitchen.DishEvaluation;
import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.kitchen.KitchenOutcomeBand;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.boaat.jazzy_cookin.menu.KitchenStationMenu;
import com.boaat.jazzy_cookin.recipe.KitchenEnvironmentRequirements;
import com.boaat.jazzy_cookin.recipe.KitchenPlateRecipe;
import com.boaat.jazzy_cookin.recipe.KitchenProcessOutput;
import com.boaat.jazzy_cookin.recipe.KitchenProcessRecipe;
import com.boaat.jazzy_cookin.registry.JazzyBlockEntities;
import com.boaat.jazzy_cookin.registry.JazzyRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class KitchenStationBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int INPUT_START = 0;
    public static final int INPUT_END = 3;
    public static final int TOOL_SLOT = 4;
    public static final int OUTPUT_SLOT = 5;
    public static final int BYPRODUCT_SLOT = 6;
    private static final int CONTAINER_SIZE = 7;

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> KitchenStationBlockEntity.this.progress;
                case 1 -> KitchenStationBlockEntity.this.maxProgress;
                case 2 -> KitchenStationBlockEntity.this.heatLevel.ordinal();
                case 3 -> KitchenStationBlockEntity.this.preheatProgress;
                case 4 -> KitchenStationBlockEntity.this.currentMethod().ordinal();
                case 5 -> KitchenStationBlockEntity.this.controlSetting;
                case 6 -> KitchenStationBlockEntity.this.environmentStatus();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> KitchenStationBlockEntity.this.progress = value;
                case 1 -> KitchenStationBlockEntity.this.maxProgress = value;
                case 2 -> KitchenStationBlockEntity.this.heatLevel = HeatLevel.values()[Math.max(0, Math.min(HeatLevel.values().length - 1, value))];
                case 3 -> KitchenStationBlockEntity.this.preheatProgress = value;
                case 5 -> KitchenStationBlockEntity.this.controlSetting = Math.max(0, Math.min(2, value));
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    private int progress;
    private int maxProgress;
    private int preheatProgress;
    private int controlSetting = 1;
    private boolean processing;
    private HeatLevel heatLevel = HeatLevel.OFF;

    public KitchenStationBlockEntity(BlockPos pos, BlockState blockState) {
        super(JazzyBlockEntities.KITCHEN_STATION.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, KitchenStationBlockEntity blockEntity) {
        if (blockEntity.getStationType() == StationType.OVEN) {
            if (blockEntity.heatLevel == HeatLevel.OFF) {
                blockEntity.preheatProgress = Math.max(0, blockEntity.preheatProgress - 2);
            } else {
                int gain = switch (blockEntity.heatLevel) {
                    case LOW -> 1;
                    case MEDIUM -> 2;
                    case HIGH -> 3;
                    default -> 0;
                };
                blockEntity.preheatProgress = Math.min(100, blockEntity.preheatProgress + gain);
            }
        }

        if (!blockEntity.processing) {
            return;
        }

        if (blockEntity.getStationType() == StationType.PLATING_STATION) {
            Optional<KitchenPlateRecipe> plateRecipe = blockEntity.currentPlateRecipe();
            if (plateRecipe.isEmpty() || !blockEntity.canAcceptOutputs(plateRecipe.get())) {
                blockEntity.stopProcessing();
                return;
            }
        } else {
            Optional<KitchenProcessRecipe> recipe = blockEntity.currentRecipe();
            if (recipe.isEmpty() || !blockEntity.environmentAllows(recipe.get()) || !blockEntity.canAcceptOutputs(recipe.get())) {
                blockEntity.stopProcessing();
                return;
            }
        }

        blockEntity.progress++;
        if (blockEntity.progress >= blockEntity.maxProgress) {
            if (blockEntity.getStationType() == StationType.PLATING_STATION) {
                blockEntity.currentPlateRecipe().ifPresent(blockEntity::finishPlate);
            } else {
                blockEntity.currentRecipe().ifPresent(blockEntity::finishRecipe);
            }
        }

        blockEntity.setChanged();
    }

    public ContainerData dataAccess() {
        return this.dataAccess;
    }

    public StationType getStationType() {
        return this.getBlockState().getBlock() instanceof KitchenStationBlock block ? block.stationType() : StationType.PREP_TABLE;
    }

    public HeatLevel heatLevel() {
        return this.heatLevel;
    }

    public boolean handleButton(int buttonId, Player player) {
        if (buttonId == 0) {
            return this.startProcessing();
        }

        if (this.getStationType().supportsHeat()) {
            this.heatLevel = switch (buttonId) {
                case 1 -> HeatLevel.LOW;
                case 2 -> HeatLevel.MEDIUM;
                case 3 -> HeatLevel.HIGH;
                default -> this.heatLevel;
            };
            this.setChanged();
            return true;
        }

        if (this.getStationType().supportsStationControl()) {
            if (buttonId == 4) {
                this.controlSetting = Math.max(0, this.controlSetting - 1);
                this.setChanged();
                return true;
            }
            if (buttonId == 5) {
                this.controlSetting = Math.min(2, this.controlSetting + 1);
                this.setChanged();
                return true;
            }
        }

        return false;
    }

    private boolean startProcessing() {
        if (this.level == null || this.processing) {
            return false;
        }

        if (this.getStationType() == StationType.PLATING_STATION) {
            Optional<KitchenPlateRecipe> plateRecipe = this.currentPlateRecipe();
            if (plateRecipe.isEmpty() || !this.canAcceptOutputs(plateRecipe.get())) {
                return false;
            }

            this.processing = true;
            this.progress = 0;
            this.maxProgress = 24;
            this.setChanged();
            return true;
        }

        Optional<KitchenProcessRecipe> recipe = this.currentRecipe();
        if (recipe.isEmpty() || !this.environmentAllows(recipe.get()) || !this.canAcceptOutputs(recipe.get())) {
            return false;
        }

        this.processing = true;
        this.progress = 0;
        this.maxProgress = recipe.get().mode() == com.boaat.jazzy_cookin.kitchen.ProcessMode.PASSIVE
                ? Math.max(40, recipe.get().effectiveDuration())
                : Math.max(20, Math.round(recipe.get().effectiveDuration() / this.toolSpeedMultiplier(recipe.get())));
        this.setChanged();
        return true;
    }

    private void stopProcessing() {
        this.processing = false;
        this.progress = 0;
        this.maxProgress = 0;
        this.setChanged();
    }

    private Optional<KitchenProcessRecipe> currentRecipe() {
        if (this.level == null) {
            return Optional.empty();
        }

        return JazzyRecipes.findProcessRecipe(
                this.level,
                this.getStationType(),
                List.of(this.getItem(0), this.getItem(1), this.getItem(2), this.getItem(3)),
                this.getItem(TOOL_SLOT),
                this.heatLevel,
                this.preheatProgress >= 100
        );
    }

    private Optional<KitchenPlateRecipe> currentPlateRecipe() {
        if (this.level == null) {
            return Optional.empty();
        }

        return JazzyRecipes.findPlateRecipe(this.level, List.of(this.getItem(0), this.getItem(1), this.getItem(2), this.getItem(3)));
    }

    private KitchenMethod currentMethod() {
        if (this.getStationType() == StationType.PLATING_STATION) {
            return this.currentPlateRecipe().isPresent() ? KitchenMethod.PLATE : KitchenMethod.NONE;
        }
        return this.currentRecipe().map(KitchenProcessRecipe::method).orElse(KitchenMethod.NONE);
    }

    private boolean environmentAllows(KitchenProcessRecipe recipe) {
        ToolProfile actualProfile = ToolProfile.fromStack(this.getItem(TOOL_SLOT));
        List<ToolProfile> allowedTools = recipe.allowedToolsOrPreferred();
        if (recipe.toolRequired()) {
            if (allowedTools.isEmpty()) {
                return false;
            }
            if (actualProfile == ToolProfile.NONE || !recipe.allowsTool(actualProfile)) {
                return false;
            }
        }

        if (this.level == null) {
            return true;
        }

        KitchenEnvironmentRequirements requirements = recipe.environmentRequirements();
        if (requirements.nearbyWater()) {
            boolean hasWater = false;
            for (Direction direction : Direction.values()) {
                if (this.level.getFluidState(this.worldPosition.relative(direction)).is(FluidTags.WATER)) {
                    hasWater = true;
                    break;
                }
            }
            if (!hasWater) {
                return false;
            }
        }

        return !requirements.sheltered() || !this.level.canSeeSky(this.worldPosition.above());
    }

    private float toolSpeedMultiplier(KitchenProcessRecipe recipe) {
        List<ToolProfile> allowedTools = recipe.allowedToolsOrPreferred();
        if (allowedTools.isEmpty()) {
            return 1.0F;
        }

        if (this.getItem(TOOL_SLOT).getItem() instanceof KitchenToolItem toolItem) {
            if (recipe.preferredTool().isPresent() && toolItem.profile() == recipe.preferredTool().get()) {
                return toolItem.speedMultiplier();
            }
            if (recipe.allowsTool(toolItem.profile())) {
                return toolItem.speedMultiplier() * 0.88F;
            }
        }

        return this.getItem(TOOL_SLOT).isEmpty() ? 0.75F : 0.82F;
    }

    private boolean canAcceptOutputs(KitchenProcessRecipe recipe) {
        if (!this.canAcceptStack(OUTPUT_SLOT, recipe.output().result())) {
            return false;
        }
        return recipe.output().byproduct().isEmpty() || this.canAcceptStack(BYPRODUCT_SLOT, recipe.output().byproduct());
    }

    private boolean canAcceptOutputs(KitchenPlateRecipe recipe) {
        if (!this.canAcceptStack(OUTPUT_SLOT, recipe.output().result())) {
            return false;
        }
        return recipe.output().byproduct().isEmpty() || this.canAcceptStack(BYPRODUCT_SLOT, recipe.output().byproduct());
    }

    private boolean canAcceptStack(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        ItemStack existing = this.getItem(slot);
        if (existing.isEmpty()) {
            return true;
        }

        if (!ItemStack.isSameItemSameComponents(existing, stack)) {
            return false;
        }

        return existing.getCount() + stack.getCount() <= existing.getMaxStackSize();
    }

    private void mergeIntoSlot(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ItemStack existing = this.getItem(slot);
        if (existing.isEmpty()) {
            this.setItem(slot, stack);
            return;
        }

        existing.grow(stack.getCount());
        this.setChanged();
    }

    private void finishRecipe(KitchenProcessRecipe recipe) {
        if (this.level == null) {
            return;
        }

        List<ItemStack> consumedInputs = List.of(
                copySized(this.getItem(0), recipe.inputs().size() > 0 ? recipe.inputs().get(0).count() : 0),
                copySized(this.getItem(1), recipe.inputs().size() > 1 ? recipe.inputs().get(1).count() : 0),
                copySized(this.getItem(2), recipe.inputs().size() > 2 ? recipe.inputs().get(2).count() : 0),
                copySized(this.getItem(3), recipe.inputs().size() > 3 ? recipe.inputs().get(3).count() : 0)
        );

        KitchenOutcomeBand outcomeBand = this.currentOutcomeBand(recipe);
        KitchenProcessOutput resolvedOutput = recipe.outputForBand(outcomeBand);
        IngredientStateData outputData = DishEvaluation.evaluateProcess(
                this.level,
                recipe,
                resolvedOutput,
                consumedInputs,
                this.getItem(TOOL_SLOT),
                this.heatLevel,
                this.preheatProgress >= 100
        );

        for (int i = 0; i < recipe.inputs().size(); i++) {
            this.removeItem(i, recipe.inputs().get(i).count());
        }

        ItemStack outputStack = resolvedOutput.result().copy();
        if (outputStack.getItem() instanceof KitchenIngredientItem ingredientItem) {
            outputStack = ingredientItem.createStack(outputStack.getCount(), this.level.getGameTime(), outputData);
        }

        this.mergeIntoSlot(OUTPUT_SLOT, outputStack);
        ItemStack byproduct = resolvedOutput.byproduct().copy();
        if (!byproduct.isEmpty() && byproduct.getItem() instanceof KitchenIngredientItem ingredientItem) {
            byproduct = ingredientItem.createStack(byproduct.getCount(), this.level.getGameTime());
        }
        this.mergeIntoSlot(BYPRODUCT_SLOT, byproduct);
        this.damageTool(recipe);
        this.stopProcessing();
        if (this.getStationType() == StationType.OVEN) {
            this.preheatProgress = Math.max(0, this.preheatProgress - 30);
        }
        this.setChanged();
    }

    private void finishPlate(KitchenPlateRecipe recipe) {
        if (this.level == null) {
            return;
        }

        List<ItemStack> consumedInputs = List.of(
                copySized(this.getItem(0), recipe.inputs().size() > 0 ? recipe.inputs().get(0).count() : 0),
                copySized(this.getItem(1), recipe.inputs().size() > 1 ? recipe.inputs().get(1).count() : 0),
                copySized(this.getItem(2), recipe.inputs().size() > 2 ? recipe.inputs().get(2).count() : 0),
                copySized(this.getItem(3), recipe.inputs().size() > 3 ? recipe.inputs().get(3).count() : 0)
        );

        IngredientStateData outputData = DishEvaluation.evaluatePlate(this.level, recipe, consumedInputs);
        for (int i = 0; i < recipe.inputs().size(); i++) {
            this.removeItem(i, recipe.inputs().get(i).count());
        }

        ItemStack outputStack = recipe.output().result().copy();
        if (outputStack.getItem() instanceof KitchenIngredientItem ingredientItem) {
            outputStack = ingredientItem.createStack(outputStack.getCount(), this.level.getGameTime(), outputData);
        }

        this.mergeIntoSlot(OUTPUT_SLOT, outputStack);
        ItemStack byproduct = recipe.output().byproduct().copy();
        if (!byproduct.isEmpty() && byproduct.getItem() instanceof KitchenIngredientItem ingredientItem) {
            byproduct = ingredientItem.createStack(byproduct.getCount(), this.level.getGameTime());
        }
        this.mergeIntoSlot(BYPRODUCT_SLOT, byproduct);
        this.stopProcessing();
        this.setChanged();
    }

    private static ItemStack copySized(ItemStack stack, int count) {
        if (count <= 0 || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    private KitchenOutcomeBand currentOutcomeBand(KitchenProcessRecipe recipe) {
        if (!this.getStationType().supportsStationControl() || recipe.outcomes().isEmpty()) {
            return KitchenOutcomeBand.IDEAL;
        }
        return KitchenOutcomeBand.fromControlIndex(this.controlSetting);
    }

    private int environmentStatus() {
        if (this.getStationType() == StationType.PLATING_STATION) {
            return 2;
        }

        Optional<KitchenProcessRecipe> recipe = this.currentRecipe();
        if (recipe.isEmpty() || recipe.get().environmentRequirements().isEmpty()) {
            return 2;
        }

        return this.environmentAllows(recipe.get()) ? 1 : 0;
    }

    private void damageTool(KitchenProcessRecipe recipe) {
        if (recipe.allowedToolsOrPreferred().isEmpty()) {
            return;
        }

        ItemStack tool = this.getItem(TOOL_SLOT);
        if (!tool.isDamageableItem()) {
            return;
        }

        tool.setDamageValue(tool.getDamageValue() + 1);
        if (tool.getDamageValue() >= tool.getMaxDamage()) {
            this.setItem(TOOL_SLOT, ItemStack.EMPTY);
        } else {
            this.setChanged();
        }
    }

    @Override
    public Component getDisplayName() {
        return this.getStationType().displayName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new KitchenStationMenu(containerId, playerInventory, this, this.dataAccess);
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) {
            this.setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        if (slot != OUTPUT_SLOT && slot != BYPRODUCT_SLOT) {
            this.stopProcessing();
        }
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == OUTPUT_SLOT || slot == BYPRODUCT_SLOT) {
            return false;
        }
        if (slot == TOOL_SLOT) {
            return stack.getItem() instanceof KitchenToolItem;
        }
        return true;
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    public void startOpen(Player player) {
    }

    @Override
    public void stopOpen(Player player) {
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putInt("Progress", this.progress);
        tag.putInt("MaxProgress", this.maxProgress);
        tag.putInt("PreheatProgress", this.preheatProgress);
        tag.putInt("ControlSetting", this.controlSetting);
        tag.putBoolean("Processing", this.processing);
        tag.putString("HeatLevel", this.heatLevel.getSerializedName());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.progress = tag.getInt("Progress");
        this.maxProgress = tag.getInt("MaxProgress");
        this.preheatProgress = tag.getInt("PreheatProgress");
        this.controlSetting = Math.max(0, Math.min(2, tag.getInt("ControlSetting")));
        this.processing = tag.getBoolean("Processing");
        this.heatLevel = HeatLevel.byName(tag.getString("HeatLevel"));
    }
}
