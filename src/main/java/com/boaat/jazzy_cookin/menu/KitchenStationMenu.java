package com.boaat.jazzy_cookin.menu;

import com.boaat.jazzy_cookin.block.entity.KitchenStationBlockEntity;
import com.boaat.jazzy_cookin.item.KitchenToolItem;
import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.StationCapacityProfile;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.StationUiProfile;
import com.boaat.jazzy_cookin.kitchen.sim.domain.SimulationDomainType;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishRecognitionResult;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishSchema;
import com.boaat.jazzy_cookin.registry.JazzyBlocks;
import com.boaat.jazzy_cookin.registry.JazzyMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class KitchenStationMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    private final StationType stationType;
    private final ContainerLevelAccess access;
    private final StationUiProfile uiProfile;
    private final StationCapacityProfile capacity;

    public KitchenStationMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(StationCapacityProfile.TOTAL_SLOTS),
                new SimpleContainerData(20),
                readStationType(extraData),
                ContainerLevelAccess.NULL
        );
    }

    public KitchenStationMenu(int containerId, Inventory playerInventory, KitchenStationBlockEntity blockEntity, ContainerData data) {
        this(
                containerId,
                playerInventory,
                blockEntity,
                data,
                blockEntity.getStationType(),
                ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos())
        );
    }

    private KitchenStationMenu(
            int containerId,
            Inventory playerInventory,
            Container container,
            ContainerData data,
            StationType stationType,
            ContainerLevelAccess access
    ) {
        super(JazzyMenus.KITCHEN_STATION.get(), containerId);
        this.container = container;
        this.data = data;
        this.stationType = stationType;
        this.access = access;
        this.uiProfile = StationUiProfile.forStation(stationType);
        this.capacity = this.uiProfile.capacity();

        checkContainerSize(container, StationCapacityProfile.TOTAL_SLOTS);
        checkContainerDataCount(data, 20);
        container.startOpen(playerInventory.player);

        for (int inputIndex = 0; inputIndex < this.capacity.inputCount(); inputIndex++) {
            StationUiProfile.Point position = this.uiProfile.inputPositions()[inputIndex];
            this.addSlot(new Slot(container, inputIndex, position.x(), position.y()) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !(stack.getItem() instanceof KitchenToolItem);
                }
            });
        }

        this.addSlot(new Slot(container, StationCapacityProfile.TOOL_SLOT, this.uiProfile.toolPosition().x(), this.uiProfile.toolPosition().y()) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return KitchenStationMenu.this.stationType.usesTools() && stack.getItem() instanceof KitchenToolItem;
            }
        });
        this.addSlot(new Slot(container, StationCapacityProfile.OUTPUT_SLOT, this.uiProfile.outputPosition().x(), this.uiProfile.outputPosition().y()) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new Slot(container, StationCapacityProfile.BYPRODUCT_SLOT, this.uiProfile.byproductPosition().x(), this.uiProfile.byproductPosition().y()) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        this.uiProfile.playerInventoryStartX() + col * 18,
                        this.uiProfile.playerInventoryStartY() + row * 18
                ));
            }
        }

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            this.addSlot(new Slot(playerInventory, hotbarSlot, this.uiProfile.playerInventoryStartX() + hotbarSlot * 18, this.uiProfile.hotbarY()));
        }

        this.addDataSlots(data);
    }

    public StationType stationType() {
        return this.stationType;
    }

    public StationUiProfile uiProfile() {
        return this.uiProfile;
    }

    public int activeInputCount() {
        return this.capacity.inputCount();
    }

    public int visibleStationSlotCount() {
        return this.capacity.visibleStationSlotCount();
    }

    public int toolMenuSlotIndex() {
        return this.capacity.toolMenuSlotIndex();
    }

    public int outputMenuSlotIndex() {
        return this.capacity.outputMenuSlotIndex();
    }

    public int byproductMenuSlotIndex() {
        return this.capacity.byproductMenuSlotIndex();
    }

    private static StationType readStationType(RegistryFriendlyByteBuf extraData) {
        extraData.readBlockPos();
        return StationType.byName(extraData.readUtf());
    }

    public int progress() {
        return this.data.get(0);
    }

    public int maxProgress() {
        return this.data.get(1);
    }

    public HeatLevel heatLevel() {
        return HeatLevel.values()[Math.max(0, Math.min(HeatLevel.values().length - 1, this.data.get(2)))];
    }

    public int preheatProgress() {
        return this.data.get(3);
    }

    public KitchenMethod currentMethod() {
        return KitchenMethod.values()[Math.max(0, Math.min(KitchenMethod.values().length - 1, this.data.get(4)))];
    }

    public int controlSetting() {
        return Math.max(0, Math.min(2, this.data.get(5)));
    }

    public int environmentStatus() {
        return this.data.get(6);
    }

    public int ovenTemperature() {
        if (this.stationType != StationType.OVEN) {
            return HeatLevel.DEFAULT_OVEN_TEMPERATURE;
        }
        int syncedTemperature = this.data.get(7);
        return syncedTemperature > 0 ? syncedTemperature : HeatLevel.DEFAULT_OVEN_TEMPERATURE;
    }

    public int executionMode() {
        return this.data.get(8);
    }

    public boolean simulationMode() {
        return this.executionMode() == 1;
    }

    public SimulationDomainType activeDomain() {
        if (!this.simulationMode()) {
            return SimulationDomainType.NONE;
        }
        return switch (this.stationType) {
            case STOVE -> switch (this.currentMethod()) {
                case BOIL, SIMMER, STEAM -> SimulationDomainType.POT;
                default -> SimulationDomainType.PAN;
            };
            case SPICE_GRINDER, STRAINER -> SimulationDomainType.PROCESS;
            case PLATING_STATION -> SimulationDomainType.PLATE;
            default -> this.stationType.simulationDomain();
        };
    }

    public boolean simulationWorking() {
        return this.data.get(9) != 0;
    }

    public boolean simulationBatchPresent() {
        return this.simulationWorking();
    }

    public int simPanTempF() {
        return this.data.get(10);
    }

    public int simFoodCoreTempF() {
        return this.data.get(11);
    }

    public int simFoodSurfaceTempF() {
        return this.data.get(12);
    }

    public int simDoneness() {
        return this.data.get(13);
    }

    public int simMoisture() {
        return this.data.get(14);
    }

    public int simBrowning() {
        return this.data.get(15);
    }

    public int simChar() {
        return this.data.get(16);
    }

    public int simAeration() {
        return this.data.get(17);
    }

    public int simFragmentation() {
        return this.data.get(18);
    }

    public int simRecognizerId() {
        return this.data.get(19);
    }

    public Component simulationPreviewName() {
        if (!this.simulationMode()) {
            return Component.empty();
        }
        DishRecognitionResult descriptor = DishSchema.descriptor(this.simRecognizerId());
        if (descriptor == null) {
            return Component.translatable("screen.jazzycookin.preview_none");
        }
        return descriptor.resultItem().get().getDefaultInstance().getHoverName();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, JazzyBlocks.blockForStation(this.stationType).get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack slotStack = slot.getItem();
        ItemStack quickMoved = slotStack.copy();
        int stationSlotCount = this.visibleStationSlotCount();
        if (index < stationSlotCount) {
            if (!this.moveItemStackTo(slotStack, stationSlotCount, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (slotStack.getItem() instanceof KitchenToolItem) {
            if (!this.stationType.usesTools()) {
                return ItemStack.EMPTY;
            }
            if (!this.moveItemStackTo(slotStack, this.toolMenuSlotIndex(), this.toolMenuSlotIndex() + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(slotStack, 0, this.activeInputCount(), false)) {
            return ItemStack.EMPTY;
        }

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        slot.onTake(player, slotStack);
        return quickMoved;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.container instanceof KitchenStationBlockEntity blockEntity) {
            return blockEntity.handleButton(id, player);
        }
        return false;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}
