package com.boaat.jazzy_cookin.screen;

import java.util.ArrayList;
import java.util.List;

import com.boaat.jazzy_cookin.kitchen.PantrySortTab;
import com.boaat.jazzy_cookin.menu.KitchenStorageMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class KitchenStorageScreen extends AbstractContainerScreen<KitchenStorageMenu> {
    private static final int STORAGE_CARD_X = 14;
    private static final int STORAGE_CARD_Y = 34;
    private static final int STORAGE_CARD_WIDTH = 202;
    private static final int STORAGE_CARD_HEIGHT = 62;
    private static final int SHORTCUT_CARD_X = 14;
    private static final int SHORTCUT_CARD_Y = 104;
    private static final int SHORTCUT_CARD_WIDTH = 202;
    private static final int SHORTCUT_CARD_HEIGHT = 70;
    private static final int INVENTORY_CARD_X = 14;
    private static final int INVENTORY_CARD_Y = 178;
    private static final int INVENTORY_CARD_WIDTH = 202;
    private static final int INVENTORY_CARD_HEIGHT = 84;
    private static final int SLOT_COUNT = 45;
    private static final int TAB_SIZE = 20;
    private static final int TAB_GAP_X = 8;
    private static final int TAB_GAP_Y = 4;
    private static final int TAB_START_Y = 124;

    private final List<PantryTabButton> pantryTabs = new ArrayList<>();
    private PantrySortTab selectedPantryTab;

    public KitchenStorageScreen(KitchenStorageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 230;
        this.imageHeight = 270;
        this.inventoryLabelY = 167;
    }

    @Override
    protected void init() {
        super.init();
        this.pantryTabs.clear();
        if (this.menu.isPantry()) {
            int index = 0;
            int[] rowCounts = new int[] { 6, 5 };
            for (int row = 0; row < rowCounts.length; row++) {
                int count = rowCounts[row];
                int rowWidth = count * TAB_SIZE + (count - 1) * TAB_GAP_X;
                int startX = SHORTCUT_CARD_X + 8 + (186 - rowWidth) / 2;
                for (int col = 0; col < count; col++) {
                    PantrySortTab tab = PantrySortTab.tabs().get(index++);
                    this.addTab(
                            tab,
                            startX + col * (TAB_SIZE + TAB_GAP_X),
                            TAB_START_Y + row * (TAB_SIZE + TAB_GAP_Y)
                    );
                }
            }
            this.updateTabStates();
        }
    }

    private void addTab(PantrySortTab tab, int x, int y) {
        Button button = this.addRenderableWidget(Button.builder(Component.empty(), pressed -> this.selectTab(tab))
                .bounds(this.leftPos + x, this.topPos + y, TAB_SIZE, TAB_SIZE)
                .build());
        button.setAlpha(0.0F);
        this.pantryTabs.add(new PantryTabButton(button, tab));
    }

    private void selectTab(PantrySortTab tab) {
        this.selectedPantryTab = tab;
        this.updateTabStates();
        this.sendButton(tab.buttonId());
    }

    private void updateTabStates() {
        for (PantryTabButton tab : this.pantryTabs) {
            tab.button().active = this.selectedPantryTab != tab.tab();
        }
    }

    private void sendButton(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        JazzyGuiRenderer.drawWindow(guiGraphics, left, top, this.imageWidth, this.imageHeight);
        JazzyGuiRenderer.drawCard(guiGraphics, left + STORAGE_CARD_X, top + STORAGE_CARD_Y, STORAGE_CARD_WIDTH, STORAGE_CARD_HEIGHT);
        JazzyGuiRenderer.drawCard(guiGraphics, left + SHORTCUT_CARD_X, top + SHORTCUT_CARD_Y, SHORTCUT_CARD_WIDTH, SHORTCUT_CARD_HEIGHT);
        JazzyGuiRenderer.drawCard(guiGraphics, left + INVENTORY_CARD_X, top + INVENTORY_CARD_Y, INVENTORY_CARD_WIDTH, INVENTORY_CARD_HEIGHT);

        for (int slotIndex = 0; slotIndex < SLOT_COUNT; slotIndex++) {
            JazzyGuiRenderer.drawSlot(guiGraphics, left + this.menu.getSlot(slotIndex).x, top + this.menu.getSlot(slotIndex).y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component shelfLabel = Component.translatable("screen.jazzycookin.shelves_short");

        guiGraphics.drawString(this.font, this.title, 14, 10, JazzyGuiRenderer.TITLE_TEXT, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.storage_short"), STORAGE_CARD_X, 35, JazzyGuiRenderer.TEXT_MUTED, false);
        guiGraphics.drawString(this.font, shelfLabel, STORAGE_CARD_X + STORAGE_CARD_WIDTH - this.font.width(shelfLabel), 35, JazzyGuiRenderer.TEXT_MUTED, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 20, this.inventoryLabelY, JazzyGuiRenderer.TEXT, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.hotbar_short"), 20, 233, JazzyGuiRenderer.TEXT_SOFT, false);

        if (this.menu.isPantry()) {
            this.drawCenteredLabel(
                    guiGraphics,
                    Component.translatable("screen.jazzycookin.sort_tabs"),
                    this.imageWidth / 2,
                    110,
                    JazzyGuiRenderer.TEXT_MUTED
            );
        } else {
            this.drawCenteredLabel(
                    guiGraphics,
                    Component.translatable("screen.jazzycookin.cellar_hint"),
                    this.imageWidth / 2,
                    132,
                    JazzyGuiRenderer.TEXT_MUTED
            );
        }
    }

    private void drawCenteredLabel(GuiGraphics guiGraphics, Component label, int centerX, int y, int color) {
        guiGraphics.drawString(this.font, label, centerX - this.font.width(label) / 2, y, color, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        PantryTabButton hoveredTab = null;
        for (PantryTabButton tab : this.pantryTabs) {
            this.drawPantryTab(guiGraphics, tab);
            if (tab.button().isMouseOver(mouseX, mouseY)) {
                hoveredTab = tab;
            }
        }

        if (hoveredTab != null) {
            guiGraphics.renderTooltip(this.font, hoveredTab.tab().label(), mouseX, mouseY);
        } else {
            this.renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    private void drawPantryTab(GuiGraphics guiGraphics, PantryTabButton tab) {
        int x = tab.button().getX();
        int y = tab.button().getY();
        JazzyGuiRenderer.drawIconTab(guiGraphics, x, y, tab.button().isHoveredOrFocused(), this.selectedPantryTab == tab.tab());

        ItemStack icon = tab.tab().iconStack();
        if (!icon.isEmpty()) {
            guiGraphics.renderItem(icon, x + 2, y + 2);
        }
    }

    private record PantryTabButton(Button button, PantrySortTab tab) {
    }
}
