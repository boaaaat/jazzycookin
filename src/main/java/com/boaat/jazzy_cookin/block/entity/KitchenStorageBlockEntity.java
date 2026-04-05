package com.boaat.jazzy_cookin.block.entity;

import com.boaat.jazzy_cookin.block.KitchenStorageBlock;
import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.kitchen.StorageType;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.PantrySortTab;
import com.boaat.jazzy_cookin.menu.KitchenStorageMenu;
import com.boaat.jazzy_cookin.registry.JazzyBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class KitchenStorageBlockEntity extends BlockEntity implements Container, MenuProvider {
    private static final int PANTRY_CONTAINER_SIZE = 54;
    private static final int CELLAR_CONTAINER_SIZE = 18;

    private final NonNullList<ItemStack> items;
    private long[] insertedAt;

    public KitchenStorageBlockEntity(BlockPos pos, BlockState blockState) {
        super(JazzyBlockEntities.KITCHEN_STORAGE.get(), pos, blockState);
        int size = storageSize(blockState);
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        this.insertedAt = new long[size];
    }

    public StorageType getStorageType() {
        return this.getBlockState().getBlock() instanceof KitchenStorageBlock block ? block.storageType() : StorageType.PANTRY;
    }

    private static int storageSize(BlockState blockState) {
        if (blockState.getBlock() instanceof KitchenStorageBlock block && block.storageType() == StorageType.CELLAR) {
            return CELLAR_CONTAINER_SIZE;
        }
        return PANTRY_CONTAINER_SIZE;
    }

    public boolean handleButton(int buttonId, Player player) {
        if (this.getStorageType() != StorageType.PANTRY || this.level == null) {
            return false;
        }

        PantrySortTab sortTab = PantrySortTab.byButtonId(buttonId);
        if (sortTab == null) {
            return false;
        }

        this.sortByCategory(sortTab);
        return true;
    }

    private void sortByCategory(PantrySortTab selectedTab) {
        java.util.List<StorageEntry> entries = new java.util.ArrayList<>(this.getContainerSize());
        for (int slot = 0; slot < this.getContainerSize(); slot++) {
            entries.add(new StorageEntry(this.items.get(slot), this.insertedAt[slot]));
        }

        entries.sort((left, right) -> {
            if (left.stack().isEmpty() != right.stack().isEmpty()) {
                return left.stack().isEmpty() ? 1 : -1;
            }

            PantrySortTab leftTab = PantrySortTab.classify(left.stack());
            PantrySortTab rightTab = PantrySortTab.classify(right.stack());
            int leftPriority = this.sortPriority(selectedTab, leftTab);
            int rightPriority = this.sortPriority(selectedTab, rightTab);
            if (leftPriority != rightPriority) {
                return Integer.compare(leftPriority, rightPriority);
            }

            int categoryOrder = Integer.compare(leftTab.ordinal(), rightTab.ordinal());
            if (categoryOrder != 0) {
                return categoryOrder;
            }

            int descriptionOrder = left.stack().getDescriptionId().compareTo(right.stack().getDescriptionId());
            if (descriptionOrder != 0) {
                return descriptionOrder;
            }

            return Long.compare(left.insertedAt(), right.insertedAt());
        });

        for (int slot = 0; slot < this.getContainerSize(); slot++) {
            StorageEntry entry = entries.get(slot);
            this.items.set(slot, entry.stack());
            this.insertedAt[slot] = entry.insertedAt();
        }
        this.setChanged();
    }

    private int sortPriority(PantrySortTab selectedTab, PantrySortTab tab) {
        if (tab == selectedTab) {
            return 0;
        }
        if (tab == PantrySortTab.OTHER) {
            return PantrySortTab.tabs().size() + 1;
        }
        return PantrySortTab.tabs().indexOf(tab) + 1;
    }

    @Override
    public Component getDisplayName() {
        return this.getStorageType().displayName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new KitchenStorageMenu(containerId, playerInventory, this);
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
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
        if (this.level != null && !this.items.get(slot).isEmpty()) {
            this.applyStorageModifier(slot, this.items.get(slot));
            this.insertedAt[slot] = this.level.getGameTime();
        }

        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) {
            if (this.items.get(slot).isEmpty()) {
                this.insertedAt[slot] = 0L;
            }
            this.setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (this.level != null && !this.items.get(slot).isEmpty()) {
            this.applyStorageModifier(slot, this.items.get(slot));
        }

        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        this.insertedAt[slot] = 0L;
        return removed;
    }

    private void applyStorageModifier(int slot, ItemStack extractedStack) {
        if (this.level == null || this.insertedAt[slot] <= 0L) {
            return;
        }

        if (extractedStack.getItem() instanceof KitchenIngredientItem) {
            IngredientStateData data = KitchenStackUtil.getOrCreateData(extractedStack, this.level.getGameTime());
            if (data != null) {
                long storedTicks = Math.max(0L, this.level.getGameTime() - this.insertedAt[slot]);
                long ageReduction = Math.round(storedTicks * (1.0F - this.getStorageType().decayMultiplier()));
                KitchenStackUtil.setData(extractedStack, data.withCreatedTick(data.createdTick() + ageReduction));
            }
        }
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!stack.isEmpty() && !this.canPlaceItem(slot, stack)) {
            return;
        }

        ItemStack previous = this.items.get(slot);
        this.items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }

        if (!stack.isEmpty() && (previous.isEmpty() || !ItemStack.isSameItemSameComponents(previous, stack))) {
            this.insertedAt[slot] = this.level != null ? this.level.getGameTime() : 0L;
        } else if (stack.isEmpty()) {
            this.insertedAt[slot] = 0L;
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
        return this.getStorageType() != StorageType.PANTRY || PantrySortTab.classify(stack) != PantrySortTab.OTHER;
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.EMPTY);
            this.insertedAt[slot] = 0L;
        }
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
        tag.putLongArray("InsertedAt", this.insertedAt);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        long[] loaded = tag.getLongArray("InsertedAt");
        if (loaded.length > 0) {
            this.insertedAt = new long[this.items.size()];
            System.arraycopy(loaded, 0, this.insertedAt, 0, Math.min(loaded.length, this.insertedAt.length));
        }
    }

    private record StorageEntry(ItemStack stack, long insertedAt) {
    }
}
