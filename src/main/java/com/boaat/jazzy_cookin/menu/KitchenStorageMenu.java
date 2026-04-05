package com.boaat.jazzy_cookin.menu;

import com.boaat.jazzy_cookin.block.entity.KitchenStorageBlockEntity;
import com.boaat.jazzy_cookin.kitchen.StorageType;
import com.boaat.jazzy_cookin.registry.JazzyBlocks;
import com.boaat.jazzy_cookin.registry.JazzyMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class KitchenStorageMenu extends AbstractContainerMenu {
    private static final int STORAGE_START_X = 34;
    private static final int STORAGE_START_Y = 43;
    private static final int PLAYER_INVENTORY_START_X = 34;
    private static final int PLAYER_INVENTORY_START_Y = 226;
    private static final int HOTBAR_Y = 284;

    private final Container container;
    private final StorageType storageType;
    private final ContainerLevelAccess access;

    public KitchenStorageMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(18),
                readStorageType(extraData),
                ContainerLevelAccess.NULL
        );
    }

    public KitchenStorageMenu(int containerId, Inventory playerInventory, KitchenStorageBlockEntity blockEntity) {
        this(
                containerId,
                playerInventory,
                blockEntity,
                blockEntity.getStorageType(),
                ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos())
        );
    }

    private KitchenStorageMenu(
            int containerId,
            Inventory playerInventory,
            Container container,
            StorageType storageType,
            ContainerLevelAccess access
    ) {
        super(JazzyMenus.KITCHEN_STORAGE.get(), containerId);
        this.container = container;
        this.storageType = storageType;
        this.access = access;

        checkContainerSize(container, 18);
        container.startOpen(playerInventory.player);

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(container, col + row * 9, STORAGE_START_X + col * 18, STORAGE_START_Y + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, PLAYER_INVENTORY_START_X + col * 18, PLAYER_INVENTORY_START_Y + row * 18));
            }
        }

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            this.addSlot(new Slot(playerInventory, hotbarSlot, PLAYER_INVENTORY_START_X + hotbarSlot * 18, HOTBAR_Y));
        }
    }

    public StorageType storageType() {
        return this.storageType;
    }

    private static StorageType readStorageType(RegistryFriendlyByteBuf extraData) {
        extraData.readBlockPos();
        return StorageType.byName(extraData.readUtf());
    }

    public boolean isPantry() {
        return this.storageType == StorageType.PANTRY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, JazzyBlocks.blockForStorage(this.storageType).get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack quickMoved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return quickMoved;
        }

        ItemStack slotStack = slot.getItem();
        quickMoved = slotStack.copy();
        if (index < 18) {
            if (!this.moveItemStackTo(slotStack, 18, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(slotStack, 0, 18, false)) {
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
        if (this.container instanceof KitchenStorageBlockEntity blockEntity) {
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
