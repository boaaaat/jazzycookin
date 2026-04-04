package com.boaat.jazzy_cookin.menu;

import com.boaat.jazzy_cookin.block.entity.KitchenStationBlockEntity;
import com.boaat.jazzy_cookin.item.KitchenToolItem;
import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.registry.JazzyBlocks;
import com.boaat.jazzy_cookin.registry.JazzyMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
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

    public KitchenStationMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(7),
                new SimpleContainerData(4),
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

        checkContainerSize(container, 7);
        checkContainerDataCount(data, 4);
        container.startOpen(playerInventory.player);

        this.addSlot(new Slot(container, 0, 18, 22));
        this.addSlot(new Slot(container, 1, 36, 22));
        this.addSlot(new Slot(container, 2, 54, 22));
        this.addSlot(new Slot(container, 3, 72, 22));
        this.addSlot(new Slot(container, 4, 45, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof KitchenToolItem;
            }
        });
        this.addSlot(new Slot(container, 5, 128, 28) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new Slot(container, 6, 128, 50) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 94 + row * 18));
            }
        }

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            this.addSlot(new Slot(playerInventory, hotbarSlot, 8 + hotbarSlot * 18, 152));
        }

        this.addDataSlots(data);
    }

    public StationType stationType() {
        return this.stationType;
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

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, JazzyBlocks.blockForStation(this.stationType).get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack quickMoved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack slotStack = slot.getItem();
        quickMoved = slotStack.copy();
        if (index < 7) {
            if (!this.moveItemStackTo(slotStack, 7, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (slotStack.getItem() instanceof KitchenToolItem) {
            if (!this.moveItemStackTo(slotStack, 4, 5, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(slotStack, 0, 4, false)) {
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
