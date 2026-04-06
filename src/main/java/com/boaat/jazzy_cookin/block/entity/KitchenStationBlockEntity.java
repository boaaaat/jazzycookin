package com.boaat.jazzy_cookin.block.entity;

import java.util.List;
import java.util.Optional;

import com.boaat.jazzy_cookin.block.KitchenStationBlock;
import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.item.KitchenToolItem;
import com.boaat.jazzy_cookin.kitchen.DishEvaluation;
import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.KitchenOutcomeBand;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.boaat.jazzy_cookin.kitchen.sim.CookingBatchState;
import com.boaat.jazzy_cookin.kitchen.sim.DishRecognitionResult;
import com.boaat.jazzy_cookin.kitchen.sim.DishSchema;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfile;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfiles;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.SimulationSnapshot;
import com.boaat.jazzy_cookin.kitchen.sim.StationPhysicsState;
import com.boaat.jazzy_cookin.menu.KitchenStationMenu;
import com.boaat.jazzy_cookin.recipe.KitchenEnvironmentRequirements;
import com.boaat.jazzy_cookin.recipe.KitchenPlateRecipe;
import com.boaat.jazzy_cookin.recipe.KitchenProcessOutput;
import com.boaat.jazzy_cookin.recipe.KitchenProcessRecipe;
import com.boaat.jazzy_cookin.registry.JazzyBlockEntities;
import com.boaat.jazzy_cookin.registry.JazzyRecipes;
import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
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
    private static final float ROOM_TEMP_C = 22.0F;
    private static final int DATA_COUNT = 20;

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            SimulationSnapshot snapshot = KitchenStationBlockEntity.this.currentSimulationSnapshot();
            return switch (index) {
                case 0 -> KitchenStationBlockEntity.this.progress;
                case 1 -> KitchenStationBlockEntity.this.maxProgress;
                case 2 -> KitchenStationBlockEntity.this.currentHeatLevel().ordinal();
                case 3 -> KitchenStationBlockEntity.this.preheatProgress;
                case 4 -> KitchenStationBlockEntity.this.currentMethod().ordinal();
                case 5 -> KitchenStationBlockEntity.this.controlSetting;
                case 6 -> KitchenStationBlockEntity.this.environmentStatus();
                case 7 -> KitchenStationBlockEntity.this.ovenTemperature;
                case 8 -> snapshot.executionMode();
                case 9 -> snapshot.batchPresent();
                case 10 -> snapshot.panTempF();
                case 11 -> snapshot.foodCoreTempF();
                case 12 -> snapshot.foodSurfaceTempF();
                case 13 -> snapshot.doneness();
                case 14 -> snapshot.moisture();
                case 15 -> snapshot.browning();
                case 16 -> snapshot.charLevel();
                case 17 -> snapshot.aeration();
                case 18 -> snapshot.fragmentation();
                case 19 -> snapshot.recognizerId();
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
                case 7 -> KitchenStationBlockEntity.this.ovenTemperature = HeatLevel.normalizeOvenTemperature(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    private int progress;
    private int maxProgress;
    private int preheatProgress;
    private int ovenTemperature = HeatLevel.DEFAULT_OVEN_TEMPERATURE;
    private int controlSetting = 1;
    private boolean processing;
    private HeatLevel heatLevel = HeatLevel.OFF;
    private StationPhysicsState stationPhysics = StationPhysicsState.idle();
    private CookingBatchState simulationBatch;

    public KitchenStationBlockEntity(BlockPos pos, BlockState blockState) {
        super(JazzyBlockEntities.KITCHEN_STATION.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, KitchenStationBlockEntity blockEntity) {
        if (blockEntity.getStationType() == StationType.OVEN) {
            int gain = switch (blockEntity.currentHeatLevel()) {
                case LOW -> 1;
                case MEDIUM -> 2;
                case HIGH -> 3;
                default -> 0;
            };
            blockEntity.preheatProgress = gain == 0
                    ? Math.max(0, blockEntity.preheatProgress - 2)
                    : Math.min(100, blockEntity.preheatProgress + gain);
        }

        if (blockEntity.executionMode() == ExecutionMode.SIMULATION) {
            if (blockEntity.processing) {
                blockEntity.stopProcessing();
            }
            blockEntity.serverTickSimulation();
            return;
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
        return this.currentHeatLevel();
    }

    public int ovenTemperature() {
        return this.ovenTemperature;
    }

    private HeatLevel currentHeatLevel() {
        return this.getStationType() == StationType.OVEN
                ? HeatLevel.fromOvenTemperature(this.ovenTemperature)
                : this.heatLevel;
    }

    public boolean handleButton(int buttonId, Player player) {
        if (buttonId >= 6 && buttonId <= 8 && this.executionMode() == ExecutionMode.SIMULATION) {
            return switch (buttonId) {
                case 6 -> this.getStationType() == StationType.MIXING_BOWL ? this.whiskEggMixture() : this.simulationBatch == null ? this.pourEggMixture() : this.removeEggDish();
                case 7 -> this.stirEggBatch();
                case 8 -> this.foldOrFlipEggBatch();
                default -> false;
            };
        }

        if (buttonId == 0) {
            return this.startProcessing();
        }

        if (this.getStationType() == StationType.OVEN) {
            if (buttonId >= 1000) {
                this.ovenTemperature = HeatLevel.normalizeOvenTemperature(buttonId - 1000);
                this.setChanged();
                return true;
            }

            int legacyTemperature = switch (buttonId) {
                case 1 -> 250;
                case 2 -> HeatLevel.DEFAULT_OVEN_TEMPERATURE;
                case 3 -> 450;
                default -> -1;
            };
            if (legacyTemperature > 0) {
                this.ovenTemperature = legacyTemperature;
                this.setChanged();
                return true;
            }
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
        if (this.level == null || this.processing || this.executionMode() == ExecutionMode.SIMULATION) {
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
        if (this.level == null || this.executionMode() == ExecutionMode.SIMULATION) {
            return Optional.empty();
        }

        return JazzyRecipes.findProcessRecipe(
                this.level,
                this.getStationType(),
                List.of(this.getItem(0), this.getItem(1), this.getItem(2), this.getItem(3)),
                this.getItem(TOOL_SLOT),
                this.currentHeatLevel(),
                this.preheatProgress >= 100
        );
    }

    private Optional<KitchenPlateRecipe> currentPlateRecipe() {
        if (this.level == null || this.executionMode() == ExecutionMode.SIMULATION) {
            return Optional.empty();
        }

        return JazzyRecipes.findPlateRecipe(this.level, List.of(this.getItem(0), this.getItem(1), this.getItem(2), this.getItem(3)));
    }

    private KitchenMethod currentMethod() {
        if (this.executionMode() == ExecutionMode.SIMULATION) {
            return switch (this.getStationType()) {
                case MIXING_BOWL -> KitchenMethod.WHISK;
                case STOVE -> KitchenMethod.PAN_FRY;
                default -> KitchenMethod.NONE;
            };
        }
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
                this.currentHeatLevel(),
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
        if (this.executionMode() == ExecutionMode.SIMULATION) {
            return 1;
        }

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
        this.simulationBatch = null;
        this.stationPhysics = StationPhysicsState.idle();
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
        tag.putInt("OvenTemperature", this.ovenTemperature);
        tag.putInt("ControlSetting", this.controlSetting);
        tag.putBoolean("Processing", this.processing);
        tag.putString("HeatLevel", this.heatLevel.getSerializedName());
        encodeCodec(tag, "SimulationPhysics", StationPhysicsState.CODEC, this.stationPhysics);
        if (this.simulationBatch != null) {
            encodeCodec(tag, "SimulationBatch", CookingBatchState.CODEC, this.simulationBatch);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.progress = tag.getInt("Progress");
        this.maxProgress = tag.getInt("MaxProgress");
        this.preheatProgress = tag.getInt("PreheatProgress");
        this.heatLevel = HeatLevel.byName(tag.getString("HeatLevel"));
        this.ovenTemperature = tag.contains("OvenTemperature")
                ? HeatLevel.normalizeOvenTemperature(tag.getInt("OvenTemperature"))
                : HeatLevel.legacyOvenTemperature(this.heatLevel);
        this.controlSetting = Math.max(0, Math.min(2, tag.getInt("ControlSetting")));
        this.processing = tag.getBoolean("Processing");
        this.stationPhysics = decodeCodec(tag, "SimulationPhysics", StationPhysicsState.CODEC).orElse(StationPhysicsState.idle());
        this.simulationBatch = decodeCodec(tag, "SimulationBatch", CookingBatchState.CODEC).orElse(null);
    }

    private void serverTickSimulation() {
        if (this.getStationType() != StationType.STOVE) {
            return;
        }

        float targetPanTemp = targetPanTemperatureC(this.currentHeatLevel());
        float currentPanTemp = this.stationPhysics.panTemperatureC();
        float nextPanTemp = currentPanTemp + (targetPanTemp - currentPanTemp) * 0.08F;
        boolean changed = Math.abs(nextPanTemp - currentPanTemp) > 0.001F;
        this.stationPhysics = new StationPhysicsState(nextPanTemp);

        if (this.simulationBatch != null) {
            FoodMatterData updated = tickEggBatch(this.simulationBatch.matter(), nextPanTemp);
            if (!updated.equals(this.simulationBatch.matter())) {
                this.simulationBatch = new CookingBatchState(updated);
                changed = true;
            }
        }

        if (changed) {
            this.setChanged();
        }
    }

    private boolean whiskEggMixture() {
        if (this.level == null || !this.supportsEggMixingSimulation()) {
            return false;
        }

        long gameTime = this.level.getGameTime();
        ItemStack output = this.getItem(OUTPUT_SLOT);
        FoodMatterData matter;
        if (output.is(JazzyItems.EGG_MIXTURE.get())) {
            matter = KitchenStackUtil.getOrCreateFoodMatter(output, gameTime);
            if (matter == null) {
                return false;
            }
            matter = this.mergeMixingBowlAddIns(matter, gameTime);
        } else {
            if (!output.isEmpty()) {
                return false;
            }
            matter = this.createEggMixture(gameTime);
            if (matter == null) {
                return false;
            }
            this.consumeMixingInputs(true);
            output = new ItemStack(JazzyItems.EGG_MIXTURE.get());
            KitchenStackUtil.initializeStack(output, null, matter, gameTime);
            this.setItem(OUTPUT_SLOT, output);
        }

        float whiskWorkDelta = switch (this.controlSetting) {
            case 0 -> 0.10F;
            case 2 -> 0.28F;
            default -> 0.18F;
        };
        float aerationDelta = switch (this.controlSetting) {
            case 0 -> 0.06F;
            case 2 -> 0.14F;
            default -> 0.10F;
        };

        float nextAeration = matter.aeration() + aerationDelta;
        float nextFragmentation = matter.fragmentation() + 0.02F + whiskWorkDelta * 0.04F;
        float nextCohesiveness = matter.cohesiveness() + 0.05F + whiskWorkDelta * 0.10F;
        if (this.controlSetting == 2 && nextAeration > 0.80F) {
            nextCohesiveness -= 0.05F;
            nextFragmentation += 0.04F;
        }

        FoodMatterData whisked = matter.withWorkingState(
                matter.water(),
                nextAeration,
                nextFragmentation,
                nextCohesiveness,
                matter.proteinSet(),
                matter.browning(),
                matter.charLevel(),
                matter.whiskWork() + whiskWorkDelta,
                matter.stirCount(),
                matter.flipCount(),
                matter.timeInPan(),
                Math.max(1, matter.processDepth() + 1),
                false
        );
        KitchenStackUtil.setFoodMatter(this.getItem(OUTPUT_SLOT), whisked, gameTime);
        this.setChanged();
        return true;
    }

    private boolean pourEggMixture() {
        if (this.level == null || this.simulationBatch != null || !this.supportsEggStoveSimulation()) {
            return false;
        }

        int mixtureSlot = this.firstInputSlotMatching(JazzyItems.EGG_MIXTURE.get());
        if (mixtureSlot < 0) {
            return false;
        }

        long gameTime = this.level.getGameTime();
        FoodMatterData matter = KitchenStackUtil.getOrCreateFoodMatter(this.getItem(mixtureSlot), gameTime);
        if (matter == null) {
            return false;
        }

        float addedFat = 0.0F;
        int fatSlot = this.firstStoveFatSlot();
        if (fatSlot >= 0) {
            FoodMaterialProfile fatProfile = FoodMaterialProfiles.profileFor(this.getItem(fatSlot)).orElse(FoodMaterialProfiles.BUTTER);
            addedFat = fatProfile.fat() * 0.18F;
            this.removeItem(fatSlot, 1);
        }

        this.removeItem(mixtureSlot, 1);
        FoodMatterData poured = matter.withFlavorLoads(
                matter.fat() + addedFat,
                matter.seasoningLoad(),
                matter.cheeseLoad(),
                matter.onionLoad(),
                matter.herbLoad(),
                matter.pepperLoad()
        ).withWorkingState(
                matter.water(),
                matter.aeration() * 0.94F,
                Math.max(0.10F, matter.fragmentation()),
                Math.max(0.18F, matter.cohesiveness() * 0.82F),
                0.0F,
                0.0F,
                0.0F,
                matter.whiskWork(),
                0,
                0,
                0,
                Math.max(2, matter.processDepth() + 1),
                false
        ).withTemps(ROOM_TEMP_C, ROOM_TEMP_C);
        this.simulationBatch = new CookingBatchState(poured);
        this.setChanged();
        return true;
    }

    private boolean stirEggBatch() {
        if (this.level == null || this.simulationBatch == null || this.getStationType() != StationType.STOVE) {
            return false;
        }

        FoodMatterData matter = this.simulationBatch.matter();
        FoodMatterData stirred = matter.withWorkingState(
                matter.water(),
                Math.max(0.0F, matter.aeration() - 0.01F),
                matter.fragmentation() + 0.16F,
                matter.cohesiveness() - 0.05F,
                matter.proteinSet(),
                Math.max(0.0F, matter.browning() - 0.01F),
                matter.charLevel(),
                matter.whiskWork(),
                matter.stirCount() + 1,
                matter.flipCount(),
                matter.timeInPan(),
                matter.processDepth(),
                false
        );
        this.simulationBatch = new CookingBatchState(stirred);
        this.setChanged();
        return true;
    }

    private boolean foldOrFlipEggBatch() {
        if (this.level == null || this.simulationBatch == null || this.getStationType() != StationType.STOVE) {
            return false;
        }

        FoodMatterData matter = this.simulationBatch.matter();
        FoodMatterData flipped = matter.withWorkingState(
                matter.water(),
                Math.max(0.0F, matter.aeration() - 0.01F),
                Math.max(0.0F, matter.fragmentation() - 0.08F),
                matter.cohesiveness() + 0.14F,
                matter.proteinSet(),
                matter.browning() + 0.01F,
                matter.charLevel(),
                matter.whiskWork(),
                matter.stirCount(),
                matter.flipCount() + 1,
                matter.timeInPan(),
                matter.processDepth(),
                false
        );
        this.simulationBatch = new CookingBatchState(flipped);
        this.setChanged();
        return true;
    }

    private boolean removeEggDish() {
        if (this.level == null || this.simulationBatch == null) {
            return false;
        }

        DishRecognitionResult result = DishSchema.finalizeResult(this.simulationBatch.matter());
        ItemStack outputStack = new ItemStack(result.resultItem().get());
        FoodMatterData matter = this.simulationBatch.matter().withWorkingState(
                this.simulationBatch.matter().water(),
                this.simulationBatch.matter().aeration(),
                this.simulationBatch.matter().fragmentation(),
                this.simulationBatch.matter().cohesiveness(),
                this.simulationBatch.matter().proteinSet(),
                this.simulationBatch.matter().browning(),
                this.simulationBatch.matter().charLevel(),
                this.simulationBatch.matter().whiskWork(),
                this.simulationBatch.matter().stirCount(),
                this.simulationBatch.matter().flipCount(),
                this.simulationBatch.matter().timeInPan(),
                this.simulationBatch.matter().processDepth(),
                true
        );
        KitchenStackUtil.initializeStack(outputStack, null, matter, this.level.getGameTime());
        if (!this.canAcceptStack(OUTPUT_SLOT, outputStack)) {
            return false;
        }

        this.mergeIntoSlot(OUTPUT_SLOT, outputStack);
        this.simulationBatch = null;
        this.setChanged();
        return true;
    }

    private FoodMatterData tickEggBatch(FoodMatterData matter, float panTempC) {
        float moistureDampening = 0.45F + matter.water() * 0.35F;
        float nextSurfaceTemp = matter.surfaceTempC() + (panTempC - matter.surfaceTempC()) * (0.09F - moistureDampening * 0.03F);
        float nextCoreTemp = matter.coreTempC() + (nextSurfaceTemp - matter.coreTempC()) * (0.05F - moistureDampening * 0.015F);
        float nextProteinSet = matter.proteinSet();
        if (nextCoreTemp > 62.0F) {
            nextProteinSet += Mth.clamp((nextCoreTemp - 62.0F) / 20.0F, 0.0F, 1.0F) * 0.010F;
        }

        float nextWater = matter.water();
        if (nextSurfaceTemp > 100.0F) {
            nextWater -= Mth.clamp((nextSurfaceTemp - 100.0F) / 90.0F, 0.0F, 1.0F) * 0.0038F;
        }

        float nextBrowning = matter.browning();
        if (nextSurfaceTemp > 140.0F && nextWater < 0.35F) {
            nextBrowning += Mth.clamp((nextSurfaceTemp - 140.0F) / 60.0F, 0.0F, 1.0F)
                    * Mth.clamp((0.35F - nextWater) / 0.35F, 0.0F, 1.0F)
                    * 0.010F;
        }

        float nextChar = matter.charLevel();
        if (nextSurfaceTemp > 200.0F && nextBrowning > 0.65F) {
            nextChar += Mth.clamp((nextSurfaceTemp - 200.0F) / 30.0F, 0.0F, 1.0F)
                    * Mth.clamp((nextBrowning - 0.65F) / 0.35F, 0.0F, 1.0F)
                    * 0.018F;
        }

        float nextAeration = Math.max(0.0F, matter.aeration() - 0.0012F);
        float naturalCurdFormation = nextProteinSet > 0.04F
                ? 0.0025F + nextProteinSet * 0.0035F + Mth.clamp((150.0F - panTempC) / 60.0F, 0.0F, 1.0F) * 0.0015F
                : 0.0F;
        float nextFragmentation = matter.fragmentation() + naturalCurdFormation;
        float nextCohesiveness = matter.cohesiveness() + nextProteinSet * 0.002F - nextFragmentation * 0.0008F + matter.flipCount() * 0.0004F;
        FoodMatterData cooked = matter.withTemps(nextSurfaceTemp, nextCoreTemp).withWorkingState(
                nextWater,
                nextAeration,
                nextFragmentation,
                nextCohesiveness,
                nextProteinSet,
                nextBrowning,
                nextChar,
                matter.whiskWork(),
                matter.stirCount(),
                matter.flipCount(),
                matter.timeInPan() + 1,
                matter.processDepth(),
                false
        );
        return cooked;
    }

    private FoodMatterData createEggMixture(long gameTime) {
        if (this.level == null) {
            return null;
        }

        int eggCount = 0;
        long createdTick = gameTime;
        float water = 0.0F;
        float fat = 0.0F;
        float protein = 0.0F;
        float seasoning = 0.0F;
        float cheese = 0.0F;
        float onion = 0.0F;
        float herb = 0.0F;
        float pepper = 0.0F;

        for (int slot = INPUT_START; slot <= INPUT_END; slot++) {
            ItemStack stack = this.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            Optional<FoodMaterialProfile> profile = FoodMaterialProfiles.profileFor(stack);
            if (profile.isEmpty()) {
                return null;
            }

            FoodMatterData sourceMatter = KitchenStackUtil.getOrCreateFoodMatter(stack, gameTime);
            if (sourceMatter != null) {
                createdTick = Math.min(createdTick, sourceMatter.createdTick());
            }

            if (stack.is(JazzyItems.ingredient(JazzyItems.IngredientId.EGGS).get())) {
                eggCount++;
                water += profile.get().water();
                fat += profile.get().fat();
                protein += profile.get().protein();
            } else {
                water += profile.get().water() * 0.08F;
                fat += profile.get().fat() * 0.12F;
                protein += profile.get().protein() * 0.08F;
                seasoning += profile.get().seasoningLoad();
                cheese += profile.get().cheeseLoad();
                onion += profile.get().onionLoad();
                herb += profile.get().herbLoad();
                pepper += profile.get().pepperLoad();
            }
        }

        if (eggCount <= 0) {
            return null;
        }

        return new FoodMatterData(
                createdTick,
                ROOM_TEMP_C,
                ROOM_TEMP_C,
                eggCount > 0 ? water / eggCount : FoodMaterialProfiles.EGGS.water(),
                eggCount > 0 ? fat / eggCount : FoodMaterialProfiles.EGGS.fat(),
                eggCount > 0 ? protein / eggCount : FoodMaterialProfiles.EGGS.protein(),
                0.12F,
                0.08F,
                0.22F,
                0.0F,
                0.0F,
                0.0F,
                seasoning,
                cheese,
                onion,
                herb,
                pepper,
                0.0F,
                0,
                0,
                0,
                1,
                false
        ).clamp();
    }

    private FoodMatterData mergeMixingBowlAddIns(FoodMatterData matter, long gameTime) {
        float water = matter.water();
        float fat = matter.fat();
        float protein = matter.protein();
        float seasoning = matter.seasoningLoad();
        float cheese = matter.cheeseLoad();
        float onion = matter.onionLoad();
        float herb = matter.herbLoad();
        float pepper = matter.pepperLoad();
        boolean consumed = false;

        for (int slot = INPUT_START; slot <= INPUT_END; slot++) {
            ItemStack stack = this.getItem(slot);
            if (stack.isEmpty() || !FoodMaterialProfiles.isMixingBowlAddIn(stack)) {
                continue;
            }

            FoodMaterialProfile profile = FoodMaterialProfiles.profileFor(stack).orElse(null);
            if (profile == null) {
                continue;
            }

            water += profile.water() * 0.08F;
            fat += profile.fat() * 0.12F;
            protein += profile.protein() * 0.08F;
            seasoning += profile.seasoningLoad();
            cheese += profile.cheeseLoad();
            onion += profile.onionLoad();
            herb += profile.herbLoad();
            pepper += profile.pepperLoad();
            this.removeItem(slot, 1);
            consumed = true;
        }

        if (!consumed) {
            return matter;
        }

        FoodMatterData flavored = matter.withFlavorLoads(fat, seasoning, cheese, onion, herb, pepper).withWorkingState(
                water,
                matter.aeration(),
                matter.fragmentation(),
                matter.cohesiveness(),
                matter.proteinSet(),
                matter.browning(),
                matter.charLevel(),
                matter.whiskWork(),
                matter.stirCount(),
                matter.flipCount(),
                matter.timeInPan(),
                matter.processDepth(),
                false
        );
        return flavored.withTemps(matter.surfaceTempC(), matter.coreTempC());
    }

    private void consumeMixingInputs(boolean consumeEggs) {
        for (int slot = INPUT_START; slot <= INPUT_END; slot++) {
            ItemStack stack = this.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(JazzyItems.ingredient(JazzyItems.IngredientId.EGGS).get())) {
                if (consumeEggs) {
                    this.removeItem(slot, 1);
                }
            } else if (FoodMaterialProfiles.isMixingBowlAddIn(stack)) {
                this.removeItem(slot, 1);
            }
        }
    }

    private SimulationSnapshot currentSimulationSnapshot() {
        int mode = this.executionMode().ordinal();
        if (this.executionMode() != ExecutionMode.SIMULATION) {
            return SimulationSnapshot.inactive(mode);
        }

        if (this.getStationType() == StationType.STOVE) {
            FoodMatterData matter = this.simulationBatch != null ? this.simulationBatch.matter() : this.previewInputMatter();
            if (matter == null) {
                return new SimulationSnapshot(mode, 0, toF(this.stationPhysics.panTemperatureC()), 72, 72, 0, 0, 0, 0, 0, 0, 0);
            }
            DishRecognitionResult preview = this.simulationBatch != null ? DishSchema.preview(matter) : null;
            return new SimulationSnapshot(
                    mode,
                    this.simulationBatch != null ? 1 : 0,
                    toF(this.stationPhysics.panTemperatureC()),
                    toF(matter.coreTempC()),
                    toF(matter.surfaceTempC()),
                    Math.round(matter.proteinSet() * 100.0F),
                    Math.round(matter.water() * 100.0F),
                    Math.round(matter.browning() * 100.0F),
                    Math.round(matter.charLevel() * 100.0F),
                    Math.round(matter.aeration() * 100.0F),
                    Math.round(matter.fragmentation() * 100.0F),
                    preview != null ? preview.previewId() : 0
            );
        }

        ItemStack output = this.getItem(OUTPUT_SLOT);
        FoodMatterData matter = output.is(JazzyItems.EGG_MIXTURE.get()) ? KitchenStackUtil.getOrCreateFoodMatter(output, this.level != null ? this.level.getGameTime() : 0L) : null;
        if (matter == null) {
            return SimulationSnapshot.inactive(mode);
        }

        return new SimulationSnapshot(
                mode,
                0,
                72,
                toF(matter.coreTempC()),
                toF(matter.surfaceTempC()),
                Math.round(matter.whiskWork() * 50.0F),
                Math.round(matter.water() * 100.0F),
                Math.round(matter.browning() * 100.0F),
                Math.round(matter.charLevel() * 100.0F),
                Math.round(matter.aeration() * 100.0F),
                Math.round(matter.fragmentation() * 100.0F),
                0
        );
    }

    private FoodMatterData previewInputMatter() {
        if (this.level == null) {
            return null;
        }
        int mixtureSlot = this.firstInputSlotMatching(JazzyItems.EGG_MIXTURE.get());
        if (mixtureSlot < 0) {
            return null;
        }
        return KitchenStackUtil.getOrCreateFoodMatter(this.getItem(mixtureSlot), this.level.getGameTime());
    }

    private boolean supportsEggMixingSimulation() {
        if (this.getStationType() != StationType.MIXING_BOWL) {
            return false;
        }

        ItemStack output = this.getItem(OUTPUT_SLOT);
        if (output.is(JazzyItems.EGG_MIXTURE.get())) {
            for (int slot = INPUT_START; slot <= INPUT_END; slot++) {
                ItemStack stack = this.getItem(slot);
                if (!stack.isEmpty() && !FoodMaterialProfiles.isMixingBowlAddIn(stack)) {
                    return false;
                }
            }
            return true;
        }

        boolean sawEgg = false;
        for (int slot = INPUT_START; slot <= INPUT_END; slot++) {
            ItemStack stack = this.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(JazzyItems.ingredient(JazzyItems.IngredientId.EGGS).get())) {
                sawEgg = true;
                continue;
            }
            if (!FoodMaterialProfiles.isMixingBowlAddIn(stack)) {
                return false;
            }
        }
        return sawEgg && this.getItem(OUTPUT_SLOT).isEmpty();
    }

    private boolean supportsEggStoveSimulation() {
        if (this.getStationType() != StationType.STOVE) {
            return false;
        }
        if (this.simulationBatch != null) {
            return true;
        }

        ToolProfile toolProfile = ToolProfile.fromStack(this.getItem(TOOL_SLOT));
        if (toolProfile != ToolProfile.PAN && toolProfile != ToolProfile.FRYING_SKILLET) {
            return false;
        }

        boolean sawMixture = false;
        for (int slot = INPUT_START; slot <= INPUT_END; slot++) {
            ItemStack stack = this.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(JazzyItems.EGG_MIXTURE.get())) {
                sawMixture = true;
                continue;
            }
            if (!FoodMaterialProfiles.isStoveFat(stack)) {
                return false;
            }
        }
        return sawMixture;
    }

    private int firstInputSlotMatching(net.minecraft.world.item.Item item) {
        for (int slot = INPUT_START; slot <= INPUT_END; slot++) {
            if (this.getItem(slot).is(item)) {
                return slot;
            }
        }
        return -1;
    }

    private int firstStoveFatSlot() {
        for (int slot = INPUT_START; slot <= INPUT_END; slot++) {
            if (FoodMaterialProfiles.isStoveFat(this.getItem(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private ExecutionMode executionMode() {
        if (this.getStationType() == StationType.PLATING_STATION) {
            return ExecutionMode.PLATE;
        }
        if (this.supportsEggMixingSimulation() || this.supportsEggStoveSimulation()) {
            return ExecutionMode.SIMULATION;
        }
        return ExecutionMode.LEGACY_RECIPE;
    }

    private static int toF(float celsius) {
        return Math.round(celsius * 9.0F / 5.0F + 32.0F);
    }

    private static float targetPanTemperatureC(HeatLevel heatLevel) {
        return switch (heatLevel) {
            case LOW -> 135.0F;
            case MEDIUM -> 175.0F;
            case HIGH -> 215.0F;
            default -> ROOM_TEMP_C;
        };
    }

    private static <T> void encodeCodec(CompoundTag tag, String key, com.mojang.serialization.Codec<T> codec, T value) {
        codec.encodeStart(NbtOps.INSTANCE, value).result().ifPresent(encoded -> tag.put(key, encoded));
    }

    private static <T> Optional<T> decodeCodec(CompoundTag tag, String key, com.mojang.serialization.Codec<T> codec) {
        if (!tag.contains(key, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        return codec.parse(NbtOps.INSTANCE, tag.get(key)).result();
    }

    private enum ExecutionMode {
        LEGACY_RECIPE,
        PLATE,
        SIMULATION
    }
}
