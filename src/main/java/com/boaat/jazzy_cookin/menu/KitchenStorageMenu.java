package com.boaat.jazzy_cookin.menu;

import com.boaat.jazzy_cookin.block.entity.KitchenStorageBlockEntity;
import com.boaat.jazzy_cookin.kitchen.PantrySortTab;
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
    private PantrySortTab selectedPantryTab;

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
                        : new StorageSlot(container, visibleIndex, STORAGE_START_X + col * 18, STORAGE_START_Y + row * 18));
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
        this.clampPantryPage();
        return this.pantryPage;
    }

    public int pageCount() {
        if (!this.isPantry()) {
            return 1;
        }
        return Math.max(1, (this.visibleStorageSlots().size() + PAGE_SIZE - 1) / PAGE_SIZE);
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

        this.clampPantryPage();
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

    public PantrySortTab selectedPantryTab() {
        return this.selectedPantryTab;
    }

    public boolean togglePantryFilter(PantrySortTab tab) {
        if (!this.isPantry() || tab == null) {
            return false;
        }

        this.selectedPantryTab = this.selectedPantryTab == tab ? null : tab;
        this.clampPantryPage();
        this.broadcastChanges();
        return true;
    }

    private void clampPantryPage() {
        this.pantryPage = Math.max(0, Math.min(this.pageCount() - 1, this.pantryPage));
    }

    private java.util.List<Integer> visibleStorageSlots() {
        java.util.ArrayList<Integer> visible = new java.util.ArrayList<>(this.storageSlotCount);
        if (!this.isPantry() || this.selectedPantryTab == null) {
            for (int slot = 0; slot < this.storageSlotCount; slot++) {
                visible.add(slot);
            }
            return visible;
        }

        for (int slot = 0; slot < this.storageSlotCount; slot++) {
            ItemStack stack = this.container.getItem(slot);
            if (!stack.isEmpty() && PantrySortTab.classify(stack) == this.selectedPantryTab) {
                visible.add(slot);
            }
        }
        for (int slot = 0; slot < this.storageSlotCount; slot++) {
            if (this.container.getItem(slot).isEmpty()) {
                visible.add(slot);
            }
        }
        return visible;
    }

    private int visibleSlotToActualSlot(int visibleIndex) {
        java.util.List<Integer> visibleSlots = this.visibleStorageSlots();
        int absoluteIndex = this.currentPageOffset() + visibleIndex;
        return absoluteIndex >= 0 && absoluteIndex < visibleSlots.size() ? visibleSlots.get(absoluteIndex) : -1;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) -> {
            boolean matchesStorage = switch (this.storageType) {
                case PANTRY -> level.getBlockState(pos).is(JazzyBlocks.PANTRY.get());
                case FRIDGE -> level.getBlockState(pos).is(JazzyBlocks.FRIDGE.get()) || level.getBlockState(pos).is(JazzyBlocks.CELLAR.get());
                case FREEZER -> level.getBlockState(pos).is(JazzyBlocks.FREEZER.get());
            };
            if (!matchesStorage) {
                return false;
            }
            return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
        }, true);
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
        PantrySortTab pantrySortTab = PantrySortTab.byButtonId(id);
        if (pantrySortTab != null) {
            return this.togglePantryFilter(pantrySortTab);
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

    private class StorageSlot extends Slot {
        private StorageSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        protected int actualSlot() {
            return super.getContainerSlot();
        }

        @Override
        public int getContainerSlot() {
            return Math.max(0, this.actualSlot());
        }

        @Override
        public ItemStack getItem() {
            int actualSlot = this.actualSlot();
            return actualSlot >= 0 ? KitchenStorageMenu.this.container.getItem(actualSlot) : ItemStack.EMPTY;
        }

        @Override
        public boolean hasItem() {
            return !this.getItem().isEmpty();
        }

        @Override
        public void set(ItemStack stack) {
            int actualSlot = this.actualSlot();
            if (actualSlot >= 0 && (stack.isEmpty() || KitchenStorageMenu.this.container.canPlaceItem(actualSlot, stack))) {
                KitchenStorageMenu.this.container.setItem(actualSlot, stack);
                this.setChanged();
            }
        }

        @Override
        public void setByPlayer(ItemStack stack) {
            int actualSlot = this.actualSlot();
            if (actualSlot >= 0 && (stack.isEmpty() || KitchenStorageMenu.this.container.canPlaceItem(actualSlot, stack))) {
                KitchenStorageMenu.this.container.setItem(actualSlot, stack);
            }
        }

        @Override
        public ItemStack remove(int amount) {
            int actualSlot = this.actualSlot();
            return actualSlot >= 0 ? KitchenStorageMenu.this.container.removeItem(actualSlot, amount) : ItemStack.EMPTY;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            int actualSlot = this.actualSlot();
            return actualSlot >= 0 && KitchenStorageMenu.this.container.canPlaceItem(actualSlot, stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return this.actualSlot() >= 0;
        }

        @Override
        public void setChanged() {
            KitchenStorageMenu.this.container.setChanged();
        }
    }

    private final class PagedStorageSlot extends StorageSlot {
        private final int visibleIndex;

        private PagedStorageSlot(Container container, int visibleIndex, int x, int y) {
            super(container, visibleIndex, x, y);
            this.visibleIndex = visibleIndex;
        }

        @Override
        protected int actualSlot() {
            return KitchenStorageMenu.this.visibleSlotToActualSlot(this.visibleIndex);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return super.mayPlace(stack)
                    && (KitchenStorageMenu.this.selectedPantryTab == null
                    || PantrySortTab.classify(stack) == KitchenStorageMenu.this.selectedPantryTab);
        }
    }
}
