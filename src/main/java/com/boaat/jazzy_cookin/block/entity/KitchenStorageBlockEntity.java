package com.boaat.jazzy_cookin.block.entity;

import com.boaat.jazzy_cookin.block.KitchenStorageBlock;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.StorageRules;
import com.boaat.jazzy_cookin.kitchen.StorageType;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class KitchenStorageBlockEntity extends BlockEntity implements Container, MenuProvider {
    private static final int PANTRY_CONTAINER_SIZE = 54;
    private static final int COLD_STORAGE_CONTAINER_SIZE = 18;

    private final NonNullList<ItemStack> items;
    private long[] insertedAt;

    public KitchenStorageBlockEntity(BlockPos pos, BlockState blockState) {
        super(JazzyBlockEntities.KITCHEN_STORAGE.get(), pos, blockState);
        int size = storageSize(blockState);
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        this.insertedAt = new long[size];
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, KitchenStorageBlockEntity blockEntity) {
        boolean changed = false;
        for (int slot = 0; slot < blockEntity.items.size(); slot++) {
            ItemStack stack = blockEntity.items.get(slot);
            if (stack.isEmpty()) {
                if (blockEntity.insertedAt[slot] != 0L) {
                    blockEntity.insertedAt[slot] = 0L;
                    changed = true;
                }
                continue;
            }

            if (blockEntity.insertedAt[slot] <= 0L) {
                blockEntity.insertedAt[slot] = level.getGameTime();
                changed = true;
            }

            long storedTicks = Math.max(0L, level.getGameTime() - blockEntity.insertedAt[slot]);
            if (storedTicks >= KitchenStackUtil.SPOILAGE_BAR_UPDATE_TICKS) {
                KitchenStackUtil.applyStorageExposure(stack, blockEntity.getStorageType(), storedTicks, level.getGameTime());
                blockEntity.insertedAt[slot] = level.getGameTime();
                changed = true;
            }
            changed |= KitchenStackUtil.refreshSpoilageDisplay(stack, level.getGameTime());
        }

        if (changed) {
            blockEntity.setChanged();
        }
    }

    public StorageType getStorageType() {
        return this.getBlockState().getBlock() instanceof KitchenStorageBlock block ? block.storageType() : StorageType.PANTRY;
    }

    private static int storageSize(BlockState blockState) {
        if (blockState.getBlock() instanceof KitchenStorageBlock block && block.storageType() != StorageType.PANTRY) {
            return COLD_STORAGE_CONTAINER_SIZE;
        }
        return PANTRY_CONTAINER_SIZE;
    }

    public boolean handleButton(int buttonId, Player player) {
        return false;
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

        long storedTicks = Math.max(0L, this.level.getGameTime() - this.insertedAt[slot]);
        KitchenStackUtil.applyStorageExposure(extractedStack, this.getStorageType(), storedTicks, this.level.getGameTime());
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
            if (this.level != null) {
                KitchenStackUtil.refreshSpoilageDisplay(stack, this.level.getGameTime());
            }
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
        return StorageRules.canStore(this.getStorageType(), stack);
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
}
