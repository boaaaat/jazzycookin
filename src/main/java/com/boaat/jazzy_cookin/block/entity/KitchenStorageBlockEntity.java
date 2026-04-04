package com.boaat.jazzy_cookin.block.entity;

import com.boaat.jazzy_cookin.block.KitchenStorageBlock;
import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.kitchen.StorageType;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.menu.KitchenStorageMenu;
import com.boaat.jazzy_cookin.registry.JazzyBlockEntities;
import com.boaat.jazzy_cookin.registry.JazzyItems;

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
    private static final int CONTAINER_SIZE = 18;

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private long[] insertedAt = new long[CONTAINER_SIZE];

    public KitchenStorageBlockEntity(BlockPos pos, BlockState blockState) {
        super(JazzyBlockEntities.KITCHEN_STORAGE.get(), pos, blockState);
    }

    public StorageType getStorageType() {
        return this.getBlockState().getBlock() instanceof KitchenStorageBlock block ? block.storageType() : StorageType.PANTRY;
    }

    public boolean handleButton(int buttonId, Player player) {
        if (this.getStorageType() != StorageType.PANTRY || this.level == null) {
            return false;
        }

        ItemStack stack = switch (buttonId) {
            case 0 -> JazzyItems.FLOUR.get().createStack(1, this.level.getGameTime());
            case 1 -> JazzyItems.CANE_SUGAR.get().createStack(1, this.level.getGameTime());
            case 2 -> JazzyItems.BUTTER.get().createStack(1, this.level.getGameTime());
            case 3 -> JazzyItems.BAKING_SPICE.get().createStack(1, this.level.getGameTime());
            default -> ItemStack.EMPTY;
        };

        if (stack.isEmpty()) {
            return false;
        }

        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        return true;
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
            this.applyStorageModifier(slot, removed);
            if (this.items.get(slot).isEmpty()) {
                this.insertedAt[slot] = 0L;
            }
            this.setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        if (!removed.isEmpty()) {
            this.applyStorageModifier(slot, removed);
        }
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
        tag.putLongArray("InsertedAt", this.insertedAt);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        long[] loaded = tag.getLongArray("InsertedAt");
        if (loaded.length == CONTAINER_SIZE) {
            this.insertedAt = loaded;
        }
    }
}
