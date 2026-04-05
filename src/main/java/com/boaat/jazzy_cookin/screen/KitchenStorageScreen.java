package com.boaat.jazzy_cookin.screen;

import java.util.ArrayList;
import java.util.List;

import com.boaat.jazzy_cookin.kitchen.PantrySortTab;
import com.boaat.jazzy_cookin.menu.KitchenStorageMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class KitchenStorageScreen extends AbstractContainerScreen<KitchenStorageMenu> {
    private static final int STORAGE_CARD_X = 14;
    private static final int STORAGE_CARD_Y = 34;
    private static final int STORAGE_CARD_WIDTH = 202;
    private static final int STORAGE_CARD_HEIGHT = 62;
    private static final int PAGE_BUTTON_X = 219;
    private static final int PAGE_UP_BUTTON_Y = 48;
    private static final int PAGE_DOWN_BUTTON_Y = 72;
    private static final int SHORTCUT_CARD_X = 14;
    private static final int SHORTCUT_CARD_Y = 104;
    private static final int SHORTCUT_CARD_WIDTH = 202;
    private static final int SHORTCUT_CARD_HEIGHT = 58;
    private static final int STORAGE_HINT_WIDTH = 184;
    private static final int STORAGE_HINT_START_Y = 116;
    private static final int STORAGE_HINT_LINE_HEIGHT = 10;
    private static final int INVENTORY_CARD_X = 14;
    private static final int INVENTORY_CARD_Y = 164;
    private static final int INVENTORY_CARD_WIDTH = 202;
    private static final int INVENTORY_CARD_HEIGHT = 84;
    private static final int TAB_SIZE = 20;
    private static final int TAB_GAP_X = 8;
    private static final int TAB_GAP_Y = 2;
    private static final int TAB_START_Y = 119;

    private final List<PantryTabButton> pantryTabs = new ArrayList<>();
    private Button previousPageButton;
    private Button nextPageButton;

    public KitchenStorageScreen(KitchenStorageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 230;
        this.imageHeight = 256;
        this.inventoryLabelY = 153;
    }

    @Override
    protected void init() {
        super.init();
        this.pantryTabs.clear();
        if (this.menu.isPantry()) {
            this.previousPageButton = this.addRenderableWidget(Button.builder(Component.literal("^"), pressed -> this.changePantryPage(-1))
                    .bounds(this.leftPos + PAGE_BUTTON_X, this.topPos + PAGE_UP_BUTTON_Y, 10, 10)
                    .build());
            this.nextPageButton = this.addRenderableWidget(Button.builder(Component.literal("v"), pressed -> this.changePantryPage(1))
                    .bounds(this.leftPos + PAGE_BUTTON_X, this.topPos + PAGE_DOWN_BUTTON_Y, 10, 10)
                    .build());
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
            this.updatePageButtons();
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
        this.menu.togglePantryFilter(tab);
        this.updateTabStates();
        this.updatePageButtons();
        this.sendButton(tab.buttonId());
    }

    private void updateTabStates() {
        for (PantryTabButton tab : this.pantryTabs) {
            tab.button().active = true;
        }
    }

    private void updatePageButtons() {
        if (this.previousPageButton != null) {
            this.previousPageButton.active = this.menu.canPageBackward();
        }
        if (this.nextPageButton != null) {
            this.nextPageButton.active = this.menu.canPageForward();
        }
    }

    private void changePantryPage(int delta) {
        if (this.menu.changePantryPage(delta)) {
            this.sendButton(delta < 0 ? KitchenStorageMenu.PREVIOUS_PAGE_BUTTON_ID : KitchenStorageMenu.NEXT_PAGE_BUTTON_ID);
            this.updatePageButtons();
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
        if (this.menu.isPantry()) {
            JazzyGuiRenderer.drawCard(guiGraphics, left + SHORTCUT_CARD_X, top + SHORTCUT_CARD_Y, SHORTCUT_CARD_WIDTH, SHORTCUT_CARD_HEIGHT);
        }
        JazzyGuiRenderer.drawCard(guiGraphics, left + INVENTORY_CARD_X, top + INVENTORY_CARD_Y, INVENTORY_CARD_WIDTH, INVENTORY_CARD_HEIGHT);

        for (int slotIndex = 0; slotIndex < this.menu.slots.size(); slotIndex++) {
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
        guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.hotbar_short"), 20, 219, JazzyGuiRenderer.TEXT_SOFT, false);

        if (this.menu.isPantry()) {
            this.drawCenteredLabel(
                    guiGraphics,
                    Component.translatable("screen.jazzycookin.sort_tabs"),
                    this.imageWidth / 2,
                    108,
                    JazzyGuiRenderer.TEXT_MUTED
            );
            this.drawCenteredLabel(
                    guiGraphics,
                    Component.translatable("screen.jazzycookin.page_short", this.menu.currentPage() + 1, this.menu.pageCount()),
                    STORAGE_CARD_X + STORAGE_CARD_WIDTH / 2,
                    86,
                    JazzyGuiRenderer.TEXT_SOFT
            );
        } else {
            this.drawWrappedCenteredLabel(
                    guiGraphics,
                    Component.translatable(this.menu.storageType().hintTranslationKey()),
                    this.imageWidth / 2,
                    STORAGE_HINT_START_Y,
                    STORAGE_HINT_WIDTH,
                    JazzyGuiRenderer.TEXT_MUTED
            );
        }
    }

    private void drawCenteredLabel(GuiGraphics guiGraphics, Component label, int centerX, int y, int color) {
        guiGraphics.drawString(this.font, label, centerX - this.font.width(label) / 2, y, color, false);
    }

    private void drawWrappedCenteredLabel(GuiGraphics guiGraphics, Component label, int centerX, int startY, int width, int color) {
        List<FormattedCharSequence> lines = this.font.split(label, width);
        for (int index = 0; index < lines.size(); index++) {
            FormattedCharSequence line = lines.get(index);
            int lineX = centerX - this.font.width(line) / 2;
            int lineY = startY + index * STORAGE_HINT_LINE_HEIGHT;
            guiGraphics.drawString(this.font, line, lineX, lineY, color, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        PantryTabButton hoveredTab = null;
        for (PantryTabButton tab : this.pantryTabs) {
            this.drawPantryTab(guiGraphics, tab, mouseX, mouseY);
            if (this.isMouseOverTab(tab, mouseX, mouseY)) {
                hoveredTab = tab;
            }
        }

        if (hoveredTab != null) {
            guiGraphics.renderTooltip(this.font, hoveredTab.tab().label(), mouseX, mouseY);
        } else {
            this.renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.updatePageButtons();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.menu.isPantry()
                && scrollY != 0.0D
                && this.isMouseOverStorageCard(mouseX, mouseY)) {
            this.changePantryPage(scrollY > 0.0D ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isMouseOverStorageCard(double mouseX, double mouseY) {
        int left = this.leftPos + STORAGE_CARD_X;
        int top = this.topPos + STORAGE_CARD_Y;
        return mouseX >= left && mouseX < left + STORAGE_CARD_WIDTH && mouseY >= top && mouseY < top + STORAGE_CARD_HEIGHT;
    }

    private void drawPantryTab(GuiGraphics guiGraphics, PantryTabButton tab, int mouseX, int mouseY) {
        int x = tab.button().getX();
        int y = tab.button().getY();
        JazzyGuiRenderer.drawIconTab(guiGraphics, x, y, this.isMouseOverTab(tab, mouseX, mouseY), this.menu.selectedPantryTab() == tab.tab());

        ItemStack icon = tab.tab().iconStack();
        if (!icon.isEmpty()) {
            guiGraphics.renderItem(icon, x + 2, y + 2);
        }
    }

    private boolean isMouseOverTab(PantryTabButton tab, double mouseX, double mouseY) {
        int x = tab.button().getX();
        int y = tab.button().getY();
        return mouseX >= x && mouseX < x + TAB_SIZE && mouseY >= y && mouseY < y + TAB_SIZE;
    }

    private record PantryTabButton(Button button, PantrySortTab tab) {
    }
}
