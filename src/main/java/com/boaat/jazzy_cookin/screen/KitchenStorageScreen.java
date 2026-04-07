package com.boaat.jazzy_cookin.screen;

import java.util.ArrayList;
import java.util.List;

import com.boaat.jazzy_cookin.kitchen.PantrySortTab;
import com.boaat.jazzy_cookin.kitchen.StationUiProfile;
import com.boaat.jazzy_cookin.kitchen.StorageUiProfile;
import com.boaat.jazzy_cookin.menu.KitchenStorageMenu;
import com.boaat.jazzy_cookin.recipebook.client.RecipeBookClientState;
import com.boaat.jazzy_cookin.screen.layout.LayoutRegion;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class KitchenStorageScreen extends AbstractContainerScreen<KitchenStorageMenu> {
    private final StorageUiProfile profile;
    private final List<PantryTabButton> pantryTabs = new ArrayList<>();
    private Button recipeBookButton;
    private Button previousPageButton;
    private Button nextPageButton;

    public KitchenStorageScreen(KitchenStorageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.profile = menu.uiProfile();
        this.imageWidth = this.profile.width();
        this.imageHeight = this.profile.height();
        this.inventoryLabelY = this.profile.inventoryLabelRegion().y();
    }

    @Override
    protected void init() {
        super.init();
        this.pantryTabs.clear();
        this.recipeBookButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.jazzycookin.recipe_book_short"),
                button -> RecipeBookClientState.openRecipeBook()).bounds(this.leftPos + this.imageWidth - 68, this.topPos + 8, 58, 18).build());
        if (this.menu.isPantry()) {
            LayoutRegion up = this.profile.pageUpBounds();
            LayoutRegion down = this.profile.pageDownBounds();
            this.previousPageButton = this.addRenderableWidget(Button.builder(Component.literal("^"), pressed -> this.changePantryPage(-1))
                    .bounds(this.leftPos + up.x(), this.topPos + up.y(), up.width(), up.height())
                    .build());
            this.previousPageButton.setAlpha(0.0F);
            this.nextPageButton = this.addRenderableWidget(Button.builder(Component.literal("v"), pressed -> this.changePantryPage(1))
                    .bounds(this.leftPos + down.x(), this.topPos + down.y(), down.width(), down.height())
                    .build());
            this.nextPageButton.setAlpha(0.0F);
            for (int index = 0; index < PantrySortTab.tabs().size(); index++) {
                this.addTab(PantrySortTab.tabs().get(index), this.profile.tabBounds(index));
            }
            this.updateTabStates();
            this.updatePageButtons();
        }
    }

    private void addTab(PantrySortTab tab, LayoutRegion bounds) {
        Button button = this.addRenderableWidget(Button.builder(Component.empty(), pressed -> this.selectTab(tab))
                .bounds(this.leftPos + bounds.x(), this.topPos + bounds.y(), bounds.width(), bounds.height())
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

        JazzyGuiRenderer.drawStorageShell(guiGraphics, left, top, this.imageWidth, this.imageHeight, this.menu.storageType());
        JazzyGuiRenderer.drawPanel(guiGraphics, left + this.profile.storageRegion().x(), top + this.profile.storageRegion().y(),
                this.profile.storageRegion().width(), this.profile.storageRegion().height(),
                storageTheme(), JazzyGuiRenderer.PanelStyle.WORKSPACE);
        JazzyGuiRenderer.drawStorageSupport(guiGraphics, left, top, this.profile.supportRegion(), this.menu.storageType());
        JazzyGuiRenderer.drawInventoryShelf(guiGraphics, left, top, this.profile.inventoryShelfRegion(), storageTheme());

        for (int slotIndex = 0; slotIndex < this.menu.slots.size(); slotIndex++) {
            JazzyGuiRenderer.drawSlot(guiGraphics, left + this.menu.getSlot(slotIndex).x, top + this.menu.getSlot(slotIndex).y);
        }

        if (this.menu.isPantry()) {
            for (PantryTabButton tab : this.pantryTabs) {
                this.drawPantryTab(guiGraphics, tab, mouseX, mouseY);
            }
            this.drawPageButton(guiGraphics, this.previousPageButton, this.profile.pageUpBounds(), mouseX, mouseY, "^");
            this.drawPageButton(guiGraphics, this.nextPageButton, this.profile.pageDownBounds(), mouseX, mouseY, "v");
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.profile.titleRegion().x(), this.profile.titleRegion().y(), JazzyGuiRenderer.TITLE_TEXT, false);
        this.drawCenteredLabel(guiGraphics, this.menu.storageType().displayName(), this.profile.headerChipRegion().centerX(), this.profile.headerChipRegion().y() + 4,
                JazzyGuiRenderer.TEXT, false);

        guiGraphics.drawString(this.font, Component.translatable("screen.jazzycookin.storage_short"),
                this.profile.storageRegion().x() + 8, this.profile.storageRegion().y() + 8, JazzyGuiRenderer.TEXT_MUTED, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.profile.inventoryLabelRegion().x(), this.profile.inventoryLabelRegion().y(),
                JazzyGuiRenderer.TEXT, false);

        if (this.menu.isPantry()) {
            this.drawTrimmedLabel(guiGraphics, Component.translatable("screen.jazzycookin.sort_tabs"),
                    this.profile.supportRegion().x() + 10, this.profile.supportRegion().y() + 8, 128, JazzyGuiRenderer.TEXT_MUTED);
            this.drawRightAlignedLabel(guiGraphics,
                    Component.translatable("screen.jazzycookin.page_short", this.menu.currentPage() + 1, this.menu.pageCount()),
                    this.profile.supportRegion().right() - 8, this.profile.supportRegion().y() + 8, JazzyGuiRenderer.TEXT_SOFT, false);
        } else {
            this.drawWrappedCenteredLabel(
                    guiGraphics,
                    Component.translatable(this.menu.storageType().hintTranslationKey()),
                    this.profile.supportRegion().centerX(),
                    this.profile.supportRegion().y() + 16,
                    this.profile.supportRegion().width() - 24,
                    JazzyGuiRenderer.TEXT_MUTED
            );
        }
    }

    private void drawPageButton(GuiGraphics guiGraphics, Button button, LayoutRegion bounds, int mouseX, int mouseY, String label) {
        if (button == null) {
            return;
        }
        JazzyGuiRenderer.drawPageButton(guiGraphics, this.leftPos + bounds.x(), this.topPos + bounds.y(), bounds.width(), bounds.height(),
                this.menu.storageType(), button.active, button.isMouseOver(mouseX, mouseY));
        this.drawCenteredLabel(guiGraphics, Component.literal(label), this.leftPos + bounds.centerX(), this.topPos + bounds.y() + 2,
                button.active ? 0xFFF8F7F2 : JazzyGuiRenderer.TEXT_SOFT, false);
    }

    private StationUiProfile.Theme storageTheme() {
        return switch (this.menu.storageType()) {
            case PANTRY -> StationUiProfile.Theme.BOARD;
            case FRIDGE -> StationUiProfile.Theme.COLD;
            case FREEZER -> StationUiProfile.Theme.GLASS;
        };
    }

    private void drawCenteredLabel(GuiGraphics guiGraphics, Component label, int centerX, int y, int color, boolean shadow) {
        guiGraphics.drawString(this.font, label, centerX - this.font.width(label) / 2, y, color, shadow);
    }

    private void drawRightAlignedLabel(GuiGraphics guiGraphics, Component label, int rightX, int y, int color, boolean shadow) {
        guiGraphics.drawString(this.font, label, rightX - this.font.width(label), y, color, shadow);
    }

    private void drawTrimmedLabel(GuiGraphics guiGraphics, Component label, int x, int y, int maxWidth, int color) {
        String trimmed = this.font.plainSubstrByWidth(label.getString(), Math.max(0, maxWidth));
        guiGraphics.drawString(this.font, trimmed, x, y, color, false);
    }

    private void drawWrappedCenteredLabel(GuiGraphics guiGraphics, Component label, int centerX, int startY, int width, int color) {
        List<FormattedCharSequence> lines = this.font.split(label, width);
        for (int index = 0; index < lines.size(); index++) {
            FormattedCharSequence line = lines.get(index);
            guiGraphics.drawString(this.font, line, centerX - this.font.width(line) / 2, startY + index * 10, color, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        PantryTabButton hoveredTab = null;
        for (PantryTabButton tab : this.pantryTabs) {
            if (this.isMouseOverTab(tab, mouseX, mouseY)) {
                hoveredTab = tab;
                break;
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
        LayoutRegion region = this.profile.storageRegion();
        int left = this.leftPos + region.x();
        int top = this.topPos + region.y();
        return mouseX >= left && mouseX < left + region.width() && mouseY >= top && mouseY < top + region.height();
    }

    private void drawPantryTab(GuiGraphics guiGraphics, PantryTabButton tab, int mouseX, int mouseY) {
        int x = tab.button().getX();
        int y = tab.button().getY();
        JazzyGuiRenderer.drawStorageTab(guiGraphics, x, y, this.isMouseOverTab(tab, mouseX, mouseY), this.menu.selectedPantryTab() == tab.tab(),
                this.menu.storageType());

        ItemStack icon = tab.tab().iconStack();
        if (!icon.isEmpty()) {
            guiGraphics.renderItem(icon, x + 3, y + 3);
        }
    }

    private boolean isMouseOverTab(PantryTabButton tab, double mouseX, double mouseY) {
        int x = tab.button().getX();
        int y = tab.button().getY();
        return mouseX >= x && mouseX < x + StorageUiProfile.TAB_SIZE && mouseY >= y && mouseY < y + StorageUiProfile.TAB_SIZE;
    }

    private record PantryTabButton(Button button, PantrySortTab tab) {
    }
}
