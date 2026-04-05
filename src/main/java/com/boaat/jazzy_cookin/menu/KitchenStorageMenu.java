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
    public static final int PAGE_SIZE = 18;
    public static final int PREVIOUS_PAGE_BUTTON_ID = 2000;
    public static final int NEXT_PAGE_BUTTON_ID = 2001;

    private static final int STORAGE_START_X = 34;
    private static final int STORAGE_START_Y = 43;
    private static final int PLAYER_INVENTORY_START_X = 34;
    private static final int PLAYER_INVENTORY_START_Y = 172;
    private static final int HOTBAR_Y = 230;

    private final Container container;
    private final StorageType storageType;
    private final ContainerLevelAccess access;
    private final int storageSlotCount;
    private int pantryPage;

    public KitchenStorageMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, readClientInit(extraData));
    }

    private KitchenStorageMenu(int containerId, Inventory playerInventory, ClientInit clientInit) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(clientInit.storageSlotCount()),
                clientInit.storageType(),
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
        this.storageSlotCount = container.getContainerSize();

        checkContainerSize(container, storageSlotCount(storageType));
        container.startOpen(playerInventory.player);

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                int visibleIndex = col + row * 9;
                this.addSlot(this.isPantry()
                        ? new PagedStorageSlot(container, visibleIndex, STORAGE_START_X + col * 18, STORAGE_START_Y + row * 18)
                        : new Slot(container, visibleIndex, STORAGE_START_X + col * 18, STORAGE_START_Y + row * 18));
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

    private static ClientInit readClientInit(RegistryFriendlyByteBuf extraData) {
        extraData.readBlockPos();
        StorageType storageType = StorageType.byName(extraData.readUtf());
        return new ClientInit(storageType, storageSlotCount(storageType));
    }

    private static int storageSlotCount(StorageType storageType) {
        return storageType == StorageType.PANTRY ? PAGE_SIZE * 3 : PAGE_SIZE;
    }

    public boolean isPantry() {
        return this.storageType == StorageType.PANTRY;
    }

    public int currentPage() {
        return this.pantryPage;
    }

    public int pageCount() {
        return this.isPantry() ? Math.max(1, this.storageSlotCount / PAGE_SIZE) : 1;
    }

    public boolean canPageBackward() {
        return this.isPantry() && this.pantryPage > 0;
    }

    public boolean canPageForward() {
        return this.isPantry() && this.pantryPage < this.pageCount() - 1;
    }

    public boolean changePantryPage(int delta) {
        if (!this.isPantry() || delta == 0) {
            return false;
        }

        int nextPage = Math.max(0, Math.min(this.pageCount() - 1, this.pantryPage + delta));
        if (nextPage == this.pantryPage) {
            return false;
        }

        this.pantryPage = nextPage;
        this.broadcastChanges();
        return true;
    }

    private int currentPageOffset() {
        return this.pantryPage * PAGE_SIZE;
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
        if (id == PREVIOUS_PAGE_BUTTON_ID) {
            return this.changePantryPage(-1);
        }
        if (id == NEXT_PAGE_BUTTON_ID) {
            return this.changePantryPage(1);
        }
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

    private record ClientInit(StorageType storageType, int storageSlotCount) {
    }

    private final class PagedStorageSlot extends Slot {
        private final int visibleIndex;

        private PagedStorageSlot(Container container, int visibleIndex, int x, int y) {
            super(container, visibleIndex, x, y);
            this.visibleIndex = visibleIndex;
        }

        private int actualSlot() {
            return KitchenStorageMenu.this.currentPageOffset() + this.visibleIndex;
        }

        @Override
        public int getContainerSlot() {
            return this.actualSlot();
        }

        @Override
        public ItemStack getItem() {
            return KitchenStorageMenu.this.container.getItem(this.actualSlot());
        }

        @Override
        public boolean hasItem() {
            return !this.getItem().isEmpty();
        }

        @Override
        public void set(ItemStack stack) {
            KitchenStorageMenu.this.container.setItem(this.actualSlot(), stack);
            this.setChanged();
        }

        @Override
        public void setByPlayer(ItemStack stack) {
            KitchenStorageMenu.this.container.setItem(this.actualSlot(), stack);
        }

        @Override
        public ItemStack remove(int amount) {
            return KitchenStorageMenu.this.container.removeItem(this.actualSlot(), amount);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return KitchenStorageMenu.this.container.canPlaceItem(this.actualSlot(), stack);
        }

        @Override
        public void setChanged() {
            KitchenStorageMenu.this.container.setChanged();
        }
    }
}
