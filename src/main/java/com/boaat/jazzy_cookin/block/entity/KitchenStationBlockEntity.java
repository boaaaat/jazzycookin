package com.boaat.jazzy_cookin.block.entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.boaat.jazzy_cookin.block.KitchenStationBlock;
import com.boaat.jazzy_cookin.item.KitchenToolItem;
import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.StationCapacityProfile;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.sim.CookingBatchState;
import com.boaat.jazzy_cookin.kitchen.sim.SimulationSnapshot;
import com.boaat.jazzy_cookin.kitchen.sim.StationPhysicsState;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationResolver;
import com.boaat.jazzy_cookin.menu.KitchenStationMenu;
import com.boaat.jazzy_cookin.registry.JazzyBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class KitchenStationBlockEntity extends BlockEntity implements Container, MenuProvider, StationSimulationAccess {
    public static final int INPUT_START = StationCapacityProfile.INPUT_START;
    public static final int INPUT_END = StationCapacityProfile.MAX_INPUT_SLOT;
    public static final int TOOL_SLOT = StationCapacityProfile.TOOL_SLOT;
    public static final int OUTPUT_SLOT = StationCapacityProfile.OUTPUT_SLOT;
    public static final int BYPRODUCT_SLOT = StationCapacityProfile.BYPRODUCT_SLOT;
    private static final int STOVE_BURNER_COUNT = 6;
    private static final int CONTAINER_SIZE = StationCapacityProfile.TOTAL_SLOTS;
    private static final int DATA_COUNT = 31;
    private static final int DEFAULT_MICROWAVE_DURATION_SECONDS = 30;
    private static final int MIN_MICROWAVE_DURATION_SECONDS = 10;
    private static final int MAX_MICROWAVE_DURATION_SECONDS = 300;
    private static final int MICROWAVE_DURATION_STEP_SECONDS = 10;
    private static final int DEFAULT_OVEN_COOK_TIME_MINUTES = 1;
    private static final int MIN_OVEN_COOK_TIME_MINUTES = 1;
    private static final int MAX_OVEN_COOK_TIME_MINUTES = 120;
    private static final int MIN_STOVE_BURNER_LEVEL = 0;
    private static final int MAX_STOVE_BURNER_LEVEL = 6;
    private static final int STOVE_BURNER_DATA_START = 21;
    private static final int STOVE_BURNER_BUTTON_BASE = 3100;
    private static final int OVEN_PREHEAT_BUTTON = 4000;
    private static final int OVEN_COOK_TIME_BUTTON_BASE = 5000;
    private static final float OVEN_PREHEAT_DEGREES_PER_TICK = 0.5F;
    private static final float OVEN_COOLDOWN_DEGREES_PER_TICK = 0.25F;

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            SimulationSnapshot snapshot = StationSimulationResolver.currentSnapshot(KitchenStationBlockEntity.this);
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
                case 20 -> KitchenStationBlockEntity.this.microwaveDurationSeconds;
                case 21, 22, 23, 24, 25, 26 -> KitchenStationBlockEntity.this.stoveDialLevel();
                case 27 -> KitchenStationBlockEntity.this.fuelBurnTime;
                case 28 -> KitchenStationBlockEntity.this.fuelBurnDuration;
                case 29 -> KitchenStationBlockEntity.this.ovenCookTimeMinutes;
                case 30 -> KitchenStationBlockEntity.this.ovenPreheating ? 1 : 0;
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
                case 20 -> KitchenStationBlockEntity.this.microwaveDurationSeconds = normalizeMicrowaveDuration(value);
                case 21, 22, 23, 24, 25, 26 -> KitchenStationBlockEntity.this.setStoveDialLevel(value);
                case 27 -> KitchenStationBlockEntity.this.fuelBurnTime = Math.max(0, value);
                case 28 -> KitchenStationBlockEntity.this.fuelBurnDuration = Math.max(0, value);
                case 29 -> KitchenStationBlockEntity.this.ovenCookTimeMinutes = normalizeOvenCookTimeMinutes(value);
                case 30 -> KitchenStationBlockEntity.this.ovenPreheating = value != 0;
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
    private int ovenCookTimeMinutes = DEFAULT_OVEN_COOK_TIME_MINUTES;
    private int microwaveDurationSeconds = DEFAULT_MICROWAVE_DURATION_SECONDS;
    private int controlSetting = 1;
    private int stoveDialLevel;
    private int fuelBurnTime;
    private int fuelBurnDuration;
    private float stoveFuelBurnRemainder;
    private boolean processing;
    private boolean ovenPreheating;
    private float ovenHeatDegrees;
    private HeatLevel heatLevel = HeatLevel.OFF;
    private StationPhysicsState stationPhysics = StationPhysicsState.idle();
    private CookingBatchState simulationBatch;
    private UUID activeGuidePlayerId;

    public KitchenStationBlockEntity(BlockPos pos, BlockState blockState) {
        super(JazzyBlockEntities.KITCHEN_STATION.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, KitchenStationBlockEntity blockEntity) {
        blockEntity.serverTickFuelAndHeat();
        blockEntity.refreshSpoilageDisplays(level.getGameTime());
        blockEntity.serverTickSimulation();
    }

    public ContainerData dataAccess() {
        return this.dataAccess;
    }

    public StationType getStationType() {
        return this.getBlockState().getBlock() instanceof KitchenStationBlock block ? block.stationType() : StationType.PREP_TABLE;
    }

    private StationCapacityProfile capacityProfile() {
        return StationCapacityProfile.forStation(this.getStationType());
    }

    private void refreshSpoilageDisplays(long gameTime) {
        boolean changed = false;
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                changed |= KitchenStackUtil.refreshSpoilageDisplay(stack, gameTime);
            }
        }
        if (changed) {
            this.setChanged();
        }
    }

    public HeatLevel heatLevel() {
        return this.currentHeatLevel();
    }

    public int ovenTemperature() {
        return this.ovenTemperature;
    }

    public static int fuelBurnTime(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        return Math.max(0, stack.getBurnTime(RecipeType.SMELTING));
    }

    public static boolean isFuel(ItemStack stack) {
        return fuelBurnTime(stack) > 0;
    }

    public int stoveDialLevel() {
        if (this.getStationType() != StationType.STOVE) {
            return 0;
        }
        return normalizeStoveBurnerLevel(this.stoveDialLevel);
    }

    public int stoveBurnerLevel(int burnerIndex) {
        if (this.getStationType() != StationType.STOVE || burnerIndex < 0 || burnerIndex >= STOVE_BURNER_COUNT) {
            return 0;
        }
        return this.stoveDialLevel();
    }

    @Override
    public int simulationMicrowaveDurationSeconds() {
        return this.microwaveDurationSeconds;
    }

    private HeatLevel currentHeatLevel() {
        if (this.getStationType() == StationType.MICROWAVE) {
            return HeatLevel.MEDIUM;
        }
        if (this.getStationType() == StationType.STOVE) {
            return this.hasActiveFuel() ? heatLevelForStoveDial(this.stoveDialLevel()) : HeatLevel.OFF;
        }
        return this.getStationType() == StationType.OVEN
                ? (this.ovenHeatDegrees >= HeatLevel.MIN_OVEN_TEMPERATURE
                ? HeatLevel.fromOvenTemperature(Math.round(this.ovenHeatDegrees))
                : HeatLevel.OFF)
                : this.heatLevel;
    }

    public boolean handleButton(int buttonId, Player player) {
        if (buttonId == 0) {
            boolean handled = StationSimulationResolver.handleAction(this, 6);
            if (handled) {
                this.activeGuidePlayerId = player.getUUID();
            }
            return handled;
        }
        if (buttonId >= 6 && buttonId <= 8) {
            boolean handled = StationSimulationResolver.handleAction(this, buttonId);
            if (handled) {
                this.activeGuidePlayerId = player.getUUID();
            }
            return handled;
        }

        if (this.getStationType() == StationType.OVEN) {
            if (buttonId == OVEN_PREHEAT_BUTTON) {
                this.ovenPreheating = !this.ovenPreheating;
                if (this.ovenPreheating) {
                    this.tryIgniteFuel();
                }
                this.setChanged();
                return true;
            }
            if (buttonId >= OVEN_COOK_TIME_BUTTON_BASE) {
                this.ovenCookTimeMinutes = normalizeOvenCookTimeMinutes(buttonId - OVEN_COOK_TIME_BUTTON_BASE);
                this.setChanged();
                return true;
            }
            if (buttonId >= 1000) {
                this.ovenTemperature = HeatLevel.normalizeOvenTemperature(buttonId - 1000);
                this.syncOvenPreheatProgress();
                this.setChanged();
                return true;
            }
        }
        if (this.getStationType() == StationType.MICROWAVE && buttonId >= 2000) {
            this.microwaveDurationSeconds = normalizeMicrowaveDuration(buttonId - 2000);
            this.setChanged();
            return true;
        }
        if (this.getStationType() == StationType.STOVE && buttonId >= STOVE_BURNER_BUTTON_BASE) {
            this.setStoveDialLevel(buttonId - STOVE_BURNER_BUTTON_BASE);
            this.tryIgniteFuel();
            this.setChanged();
            return true;
        }

        if (this.getStationType().supportsHeat() && this.getStationType() != StationType.MICROWAVE) {
            if (this.getStationType() == StationType.STOVE) {
                return false;
            }
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

    private void resetSimulationState() {
        this.processing = false;
        this.progress = 0;
        this.maxProgress = 0;
        this.simulationBatch = null;
        this.stationPhysics = StationPhysicsState.idle();
        this.activeGuidePlayerId = null;
        this.setChanged();
    }

    public UUID activeGuidePlayerId() {
        return this.activeGuidePlayerId;
    }

    private KitchenMethod currentMethod() {
        return StationSimulationResolver.currentMethod(this);
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

    private int environmentStatus() {
        return StationSimulationResolver.environmentStatus(this);
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
        if (slot != OUTPUT_SLOT && slot != BYPRODUCT_SLOT && slot != StationCapacityProfile.FUEL_SLOT) {
            this.resetSimulationState();
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
        if (slot == StationCapacityProfile.FUEL_SLOT && this.getStationType().usesFuel()) {
            return isFuel(stack);
        }
        if (slot == TOOL_SLOT) {
            return this.getStationType().usesTools() && stack.getItem() instanceof KitchenToolItem;
        }
        if (!this.capacityProfile().isActiveInputSlot(slot)) {
            return false;
        }
        return !(stack.getItem() instanceof KitchenToolItem);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        this.processing = false;
        this.progress = 0;
        this.maxProgress = 0;
        this.simulationBatch = null;
        this.fuelBurnTime = 0;
        this.fuelBurnDuration = 0;
        this.stoveFuelBurnRemainder = 0.0F;
        this.stoveDialLevel = 0;
        this.ovenPreheating = false;
        this.ovenHeatDegrees = 0.0F;
        this.preheatProgress = 0;
        this.stationPhysics = StationPhysicsState.idle();
        this.activeGuidePlayerId = null;
        if (this.getStationType() == StationType.STOVE) {
            this.syncStoveBurnerDerivedState();
        }
        this.setChanged();
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
        tag.putInt("OvenTemperature", this.ovenTemperature);
        tag.putInt("OvenCookTimeMinutes", this.ovenCookTimeMinutes);
        tag.putInt("MicrowaveDurationSeconds", this.microwaveDurationSeconds);
        tag.putInt("FuelBurnTime", this.fuelBurnTime);
        tag.putInt("FuelBurnDuration", this.fuelBurnDuration);
        tag.putFloat("StoveFuelBurnRemainder", this.stoveFuelBurnRemainder);
        if (this.getStationType().supportsStationControl() && this.getStationType() != StationType.STOVE) {
            tag.putInt("StationControlSetting", this.controlSetting);
        }
        tag.putBoolean("OvenPreheating", this.ovenPreheating);
        tag.putFloat("OvenHeatDegrees", this.ovenHeatDegrees);
        if (this.getStationType() == StationType.STOVE) {
            tag.putInt("StoveDialLevel", this.stoveDialLevel);
        }
        tag.putBoolean("Processing", this.processing);
        if (this.getStationType().supportsHeat() && this.getStationType() != StationType.STOVE && this.getStationType() != StationType.OVEN) {
            tag.putString("SelectedHeatLevel", this.heatLevel.getSerializedName());
        }
        if (this.activeGuidePlayerId != null) {
            tag.putUUID("ActiveGuidePlayer", this.activeGuidePlayerId);
        }
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
        this.heatLevel = tag.contains("SelectedHeatLevel") ? HeatLevel.byName(tag.getString("SelectedHeatLevel")) : HeatLevel.OFF;
        this.ovenTemperature = tag.contains("OvenTemperature")
                ? HeatLevel.normalizeOvenTemperature(tag.getInt("OvenTemperature"))
                : HeatLevel.DEFAULT_OVEN_TEMPERATURE;
        this.ovenCookTimeMinutes = tag.contains("OvenCookTimeMinutes")
                ? normalizeOvenCookTimeMinutes(tag.getInt("OvenCookTimeMinutes"))
                : DEFAULT_OVEN_COOK_TIME_MINUTES;
        this.microwaveDurationSeconds = tag.contains("MicrowaveDurationSeconds")
                ? normalizeMicrowaveDuration(tag.getInt("MicrowaveDurationSeconds"))
                : DEFAULT_MICROWAVE_DURATION_SECONDS;
        this.fuelBurnTime = Math.max(0, tag.getInt("FuelBurnTime"));
        this.fuelBurnDuration = Math.max(0, tag.getInt("FuelBurnDuration"));
        this.stoveFuelBurnRemainder = Math.max(0.0F, tag.getFloat("StoveFuelBurnRemainder"));
        this.ovenPreheating = tag.getBoolean("OvenPreheating");
        this.ovenHeatDegrees = tag.contains("OvenHeatDegrees") ? Math.max(0.0F, tag.getFloat("OvenHeatDegrees")) : 0.0F;
        if (this.getStationType() == StationType.STOVE) {
            this.stoveDialLevel = tag.contains("StoveDialLevel")
                    ? normalizeStoveBurnerLevel(tag.getInt("StoveDialLevel"))
                    : 0;
            this.syncStoveBurnerDerivedState();
        } else {
            this.controlSetting = Math.max(0, Math.min(2, tag.getInt("StationControlSetting")));
        }
        this.processing = tag.getBoolean("Processing");
        this.activeGuidePlayerId = tag.hasUUID("ActiveGuidePlayer") ? tag.getUUID("ActiveGuidePlayer") : null;
        this.stationPhysics = decodeCodec(tag, "SimulationPhysics", StationPhysicsState.CODEC).orElse(StationPhysicsState.idle());
        this.simulationBatch = decodeCodec(tag, "SimulationBatch", CookingBatchState.CODEC).orElse(null);
        this.syncOvenPreheatProgress();
    }

    private void serverTickSimulation() {
        StationSimulationResolver.serverTick(this);
    }

    public static int normalizeMicrowaveDuration(int seconds) {
        int clamped = Math.max(MIN_MICROWAVE_DURATION_SECONDS, Math.min(MAX_MICROWAVE_DURATION_SECONDS, seconds));
        int steps = Math.round(clamped / (float) MICROWAVE_DURATION_STEP_SECONDS);
        return Math.max(MIN_MICROWAVE_DURATION_SECONDS, steps * MICROWAVE_DURATION_STEP_SECONDS);
    }

    public static int normalizeOvenCookTimeMinutes(int minutes) {
        return Math.max(MIN_OVEN_COOK_TIME_MINUTES, Math.min(MAX_OVEN_COOK_TIME_MINUTES, minutes));
    }

    public static int normalizeStoveBurnerLevel(int level) {
        return Math.max(MIN_STOVE_BURNER_LEVEL, Math.min(MAX_STOVE_BURNER_LEVEL, level));
    }

    private void setStoveDialLevel(int level) {
        if (this.getStationType() != StationType.STOVE) {
            return;
        }
        this.stoveDialLevel = normalizeStoveBurnerLevel(level);
        this.syncStoveBurnerDerivedState();
    }

    private void syncStoveBurnerDerivedState() {
        int dialLevel = this.stoveDialLevel();
        this.controlSetting = dialLevel;
        this.heatLevel = heatLevelForStoveDial(dialLevel);
    }

    private void serverTickFuelAndHeat() {
        boolean changed = switch (this.getStationType()) {
            case STOVE -> this.serverTickStoveFuel();
            case OVEN -> this.serverTickOvenHeat();
            default -> false;
        };
        if (changed) {
            this.setChanged();
        }
    }

    private boolean serverTickStoveFuel() {
        int dialLevel = this.stoveDialLevel();
        if (dialLevel <= 0) {
            if (this.stoveFuelBurnRemainder > 0.0F) {
                this.stoveFuelBurnRemainder = 0.0F;
                return true;
            }
            return false;
        }
        return this.consumeStoveFuelTicks(dialLevel / 2.0F);
    }

    private boolean serverTickOvenHeat() {
        boolean changed = false;
        boolean heatingDemand = this.ovenPreheating || this.processing;
        if (heatingDemand) {
            changed |= this.consumeFuelTick();
            if (!this.hasActiveFuel()) {
                if (this.tryIgniteFuel()) {
                    changed = true;
                } else {
                    changed |= this.shutdownOvenForFuelLoss();
                    heatingDemand = false;
                }
            }
        } else if (this.fuelBurnTime != 0 || this.fuelBurnDuration != 0) {
            this.fuelBurnTime = 0;
            this.fuelBurnDuration = 0;
            changed = true;
        }

        float previousHeat = this.ovenHeatDegrees;
        if (heatingDemand && this.hasActiveFuel()) {
            if (this.ovenHeatDegrees < this.ovenTemperature) {
                this.ovenHeatDegrees = Math.min(this.ovenTemperature, this.ovenHeatDegrees + OVEN_PREHEAT_DEGREES_PER_TICK);
            } else if (this.ovenHeatDegrees > this.ovenTemperature) {
                this.ovenHeatDegrees = Math.max(this.ovenTemperature, this.ovenHeatDegrees - OVEN_COOLDOWN_DEGREES_PER_TICK);
            }
        } else if (this.ovenHeatDegrees > 0.0F) {
            this.ovenHeatDegrees = Math.max(0.0F, this.ovenHeatDegrees - OVEN_COOLDOWN_DEGREES_PER_TICK);
        }
        if (Float.compare(previousHeat, this.ovenHeatDegrees) != 0) {
            changed = true;
        }

        int previousProgress = this.preheatProgress;
        this.syncOvenPreheatProgress();
        return changed || previousProgress != this.preheatProgress;
    }

    private void syncOvenPreheatProgress() {
        if (this.getStationType() != StationType.OVEN) {
            this.preheatProgress = 0;
            return;
        }
        this.preheatProgress = Math.max(0, Math.min(100,
                Math.round((this.ovenHeatDegrees / Math.max(1.0F, this.ovenTemperature)) * 100.0F)));
    }

    private boolean consumeFuelTick() {
        boolean changed = false;
        if (this.fuelBurnTime <= 0 && this.tryIgniteFuel()) {
            changed = true;
        }
        if (this.fuelBurnTime > 0) {
            this.fuelBurnTime = Math.max(0, this.fuelBurnTime - 1);
            if (this.fuelBurnTime == 0) {
                this.fuelBurnDuration = 0;
            }
            changed = true;
        }
        return changed;
    }

    private boolean consumeStoveFuelTicks(float burnTicks) {
        if (burnTicks <= 0.0F) {
            return false;
        }

        boolean changed = false;
        if (this.fuelBurnTime <= 0) {
            if (!this.tryIgniteFuel()) {
                return this.shutdownStoveForFuelLoss();
            }
            changed = true;
        }

        this.stoveFuelBurnRemainder += burnTicks;
        int wholeTicksToConsume = (int) Math.floor(this.stoveFuelBurnRemainder);
        this.stoveFuelBurnRemainder -= wholeTicksToConsume;

        while (wholeTicksToConsume > 0) {
            if (this.fuelBurnTime <= 0) {
                this.fuelBurnDuration = 0;
                if (!this.tryIgniteFuel()) {
                    changed |= this.shutdownStoveForFuelLoss();
                    break;
                }
                changed = true;
            }

            int consumedTicks = Math.min(this.fuelBurnTime, wholeTicksToConsume);
            this.fuelBurnTime = Math.max(0, this.fuelBurnTime - consumedTicks);
            wholeTicksToConsume -= consumedTicks;
            changed = true;

            if (this.fuelBurnTime == 0) {
                this.fuelBurnDuration = 0;
            }
        }

        return changed;
    }

    private boolean tryIgniteFuel() {
        if (!this.getStationType().usesFuel() || this.fuelBurnTime > 0) {
            return false;
        }
        ItemStack fuelStack = this.getItem(StationCapacityProfile.FUEL_SLOT);
        if (fuelStack.isEmpty()) {
            return false;
        }
        int burnTime = fuelBurnTime(fuelStack);
        if (burnTime <= 0) {
            return false;
        }

        ItemStack containerItem = fuelStack.getCraftingRemainingItem();
        fuelStack.shrink(1);
        if (fuelStack.isEmpty()) {
            this.items.set(StationCapacityProfile.FUEL_SLOT, containerItem);
        }
        this.fuelBurnTime = burnTime;
        this.fuelBurnDuration = burnTime;
        return true;
    }

    private boolean hasActiveFuel() {
        return this.fuelBurnTime > 0;
    }

    private boolean shutdownStoveForFuelLoss() {
        boolean changed = this.stoveDialLevel() > 0
                || this.fuelBurnTime != 0
                || this.fuelBurnDuration != 0
                || this.stoveFuelBurnRemainder > 0.0F;
        this.stoveDialLevel = 0;
        this.fuelBurnTime = 0;
        this.fuelBurnDuration = 0;
        this.stoveFuelBurnRemainder = 0.0F;
        this.syncStoveBurnerDerivedState();
        return changed;
    }

    private boolean shutdownOvenForFuelLoss() {
        boolean changed = this.ovenPreheating
                || this.processing
                || this.progress != 0
                || this.maxProgress != 0
                || this.fuelBurnTime != 0
                || this.fuelBurnDuration != 0
                || this.simulationBatch != null;
        this.ovenPreheating = false;
        this.processing = false;
        this.progress = 0;
        this.maxProgress = 0;
        this.fuelBurnTime = 0;
        this.fuelBurnDuration = 0;
        this.simulationBatch = null;
        this.stationPhysics = StationPhysicsState.idle();
        return changed;
    }

    private static HeatLevel heatLevelForStoveDial(int level) {
        int normalized = normalizeStoveBurnerLevel(level);
        if (normalized == 0) {
            return HeatLevel.OFF;
        }
        if (normalized <= 2) {
            return HeatLevel.LOW;
        }
        if (normalized <= 4) {
            return HeatLevel.MEDIUM;
        }
        return HeatLevel.HIGH;
    }

    @Override
    public Level simulationLevel() {
        return this.level;
    }

    @Override
    public StationType simulationStationType() {
        return this.getStationType();
    }

    @Override
    public HeatLevel simulationHeatLevel() {
        return this.currentHeatLevel();
    }

    @Override
    public int simulationControlSetting() {
        return this.getStationType() == StationType.STOVE ? this.stoveDialLevel() : this.controlSetting;
    }

    @Override
    public int simulationPreheatProgress() {
        return this.preheatProgress;
    }

    @Override
    public int simulationOvenCookTimeTicks() {
        return this.getStationType() == StationType.OVEN ? this.ovenCookTimeMinutes * 20 * 60 : 0;
    }

    @Override
    public int simulationTargetTemperatureF() {
        return this.getStationType() == StationType.OVEN ? this.ovenTemperature : 0;
    }

    @Override
    public int simulationProgress() {
        return this.progress;
    }

    @Override
    public int simulationMaxProgress() {
        return this.maxProgress;
    }

    @Override
    public boolean simulationActive() {
        return this.processing;
    }

    @Override
    public StationCapacityProfile simulationCapacity() {
        return this.capacityProfile();
    }

    @Override
    public int inputStart() {
        return INPUT_START;
    }

    @Override
    public int inputEnd() {
        return this.capacityProfile().inputEnd();
    }

    @Override
    public int toolSlot() {
        return TOOL_SLOT;
    }

    @Override
    public int outputSlot() {
        return OUTPUT_SLOT;
    }

    @Override
    public int byproductSlot() {
        return BYPRODUCT_SLOT;
    }

    @Override
    public ItemStack simulationItem(int slot) {
        return this.getItem(slot);
    }

    @Override
    public ItemStack simulationRemoveItem(int slot, int amount) {
        return this.removeItem(slot, amount);
    }

    @Override
    public void simulationSetItem(int slot, ItemStack stack) {
        this.setItem(slot, stack);
    }

    @Override
    public boolean simulationCanAcceptStack(int slot, ItemStack stack) {
        return this.canAcceptStack(slot, stack);
    }

    @Override
    public void simulationMergeIntoSlot(int slot, ItemStack stack) {
        this.mergeIntoSlot(slot, stack);
    }

    @Override
    public StationPhysicsState simulationStationPhysics() {
        return this.stationPhysics;
    }

    @Override
    public void simulationSetStationPhysics(StationPhysicsState state) {
        this.stationPhysics = state;
    }

    @Override
    public CookingBatchState simulationBatch() {
        return this.simulationBatch;
    }

    @Override
    public void simulationSetBatch(CookingBatchState batch) {
        this.simulationBatch = batch;
    }

    @Override
    public void simulationSetProgress(int progress, int maxProgress, boolean active) {
        boolean wasProcessing = this.processing;
        this.progress = progress;
        this.maxProgress = maxProgress;
        this.processing = active;
        if (this.getStationType() == StationType.OVEN && wasProcessing && !active) {
            this.ovenPreheating = false;
        }
    }

    @Override
    public void simulationMarkChanged() {
        this.setChanged();
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

}
