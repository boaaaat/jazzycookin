package com.boaat.jazzy_cookin.menu;

import com.boaat.jazzy_cookin.block.entity.KitchenStationBlockEntity;
import com.boaat.jazzy_cookin.item.KitchenToolItem;
import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
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
    private static final int PLAYER_INVENTORY_START_Y = 104;
    private static final int HOTBAR_Y = 162;

    private final Container container;
    private final ContainerData data;
    private final StationType stationType;
    private final ContainerLevelAccess access;

    public KitchenStationMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(7),
                new SimpleContainerData(7),
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
        checkContainerDataCount(data, 7);
        container.startOpen(playerInventory.player);

        SlotLayout layout = SlotLayout.forStation(this.stationType);
        for (int inputIndex = 0; inputIndex < 4; inputIndex++) {
            SlotPosition position = layout.inputs()[inputIndex];
            this.addSlot(new Slot(container, inputIndex, position.x(), position.y()) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !(stack.getItem() instanceof KitchenToolItem);
                }
            });
        }

        this.addSlot(new Slot(container, 4, layout.tool().x(), layout.tool().y()) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return KitchenStationMenu.this.stationType.usesTools() && stack.getItem() instanceof KitchenToolItem;
            }
        });
        this.addSlot(new Slot(container, 5, layout.output().x(), layout.output().y()) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new Slot(container, 6, layout.byproduct().x(), layout.byproduct().y()) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INVENTORY_START_Y + row * 18));
            }
        }

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            this.addSlot(new Slot(playerInventory, hotbarSlot, 8 + hotbarSlot * 18, HOTBAR_Y));
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

    public KitchenMethod currentMethod() {
        return KitchenMethod.values()[Math.max(0, Math.min(KitchenMethod.values().length - 1, this.data.get(4)))];
    }

    public int controlSetting() {
        return Math.max(0, Math.min(2, this.data.get(5)));
    }

    public int environmentStatus() {
        return this.data.get(6);
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
            if (!this.stationType.usesTools()) {
                return ItemStack.EMPTY;
            }
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

    private record SlotLayout(SlotPosition[] inputs, SlotPosition tool, SlotPosition output, SlotPosition byproduct) {
        private static final SlotPosition RESULT_SLOT = new SlotPosition(138, 30);
        private static final SlotPosition BYPRODUCT_SLOT = new SlotPosition(138, 52);

        private static SlotLayout forStation(StationType stationType) {
            return switch (stationType) {
                case PREP_TABLE -> new SlotLayout(
                        positions(
                                new SlotPosition(18, 28),
                                new SlotPosition(36, 28),
                                new SlotPosition(54, 28),
                                new SlotPosition(72, 28)
                        ),
                        new SlotPosition(45, 56),
                        RESULT_SLOT,
                        BYPRODUCT_SLOT
                );
                case SPICE_GRINDER, MIXING_BOWL, STRAINER, FERMENTATION_CROCK, PLATING_STATION -> new SlotLayout(
                        positions(
                                new SlotPosition(18, 24),
                                new SlotPosition(38, 24),
                                new SlotPosition(18, 44),
                                new SlotPosition(38, 44)
                        ),
                        toolPosition(stationType),
                        RESULT_SLOT,
                        BYPRODUCT_SLOT
                );
                case OVEN -> new SlotLayout(
                        positions(
                                new SlotPosition(18, 24),
                                new SlotPosition(38, 24),
                                new SlotPosition(18, 44),
                                new SlotPosition(38, 44)
                        ),
                        new SlotPosition(58, 56),
                        RESULT_SLOT,
                        BYPRODUCT_SLOT
                );
                default -> new SlotLayout(
                        positions(
                                new SlotPosition(18, 26),
                                new SlotPosition(36, 26),
                                new SlotPosition(54, 26),
                                new SlotPosition(72, 26)
                        ),
                        toolPosition(stationType),
                        RESULT_SLOT,
                        BYPRODUCT_SLOT
                );
            };
        }

        private static SlotPosition toolPosition(StationType stationType) {
            return switch (stationType) {
                case PREP_TABLE -> new SlotPosition(45, 56);
                case SPICE_GRINDER -> new SlotPosition(60, 34);
                case MIXING_BOWL -> new SlotPosition(58, 58);
                case STRAINER -> new SlotPosition(60, 42);
                case CANNING_STATION, SMOKER, STEAMER, STOVE -> new SlotPosition(54, 54);
                case OVEN -> new SlotPosition(58, 56);
                default -> new SlotPosition(82, 56);
            };
        }

        private static SlotPosition[] positions(SlotPosition first, SlotPosition second, SlotPosition third, SlotPosition fourth) {
            return new SlotPosition[] { first, second, third, fourth };
        }
    }

    private record SlotPosition(int x, int y) {
    }
}
