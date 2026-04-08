package com.boaat.jazzy_cookin.screen;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

import com.boaat.jazzy_cookin.block.entity.KitchenStationBlockEntity;
import com.boaat.jazzy_cookin.kitchen.StationUiProfile;
import com.boaat.jazzy_cookin.screen.layout.ActionWidgetSpec;
import com.boaat.jazzy_cookin.screen.layout.LayoutRegion;
import com.daqem.uilib.api.client.gui.component.IComponent;
import com.daqem.uilib.client.gui.component.AbstractComponent;
import com.daqem.uilib.client.gui.component.io.TextBoxComponent;
import com.daqem.uilib.client.gui.text.Text;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

final class ApplianceUiLibComponents {
    private ApplianceUiLibComponents() {
    }

    enum LabelAlign {
        LEFT,
        CENTER
    }

    static final class ApplianceRoot extends AbstractComponent<ApplianceRoot> {
        ApplianceRoot(int x, int y, int width, int height) {
            super(null, x, y, width, height);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        }

        void renderTree(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderBase(guiGraphics, mouseX, mouseY, partialTick);
        }

        boolean mouseClicked(double mouseX, double mouseY, int button) {
            return dispatchClick(this.getChildren(), mouseX, mouseY, button);
        }

        boolean mouseReleased(double mouseX, double mouseY, int button) {
            return dispatchRelease(this.getChildren(), mouseX, mouseY, button);
        }

        boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
            return dispatchScroll(this.getChildren(), mouseX, mouseY, amountX, amountY);
        }

        boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return dispatchKeyPressed(this.getChildren(), keyCode, scanCode, modifiers);
        }

        boolean charTyped(char codePoint, int modifiers) {
            return dispatchCharTyped(this.getChildren(), codePoint, modifiers);
        }

        private static boolean dispatchClick(List<IComponent<?>> components, double mouseX, double mouseY, int button) {
            for (IComponent<?> component : List.copyOf(components)) {
                if (!component.isVisible()) {
                    continue;
                }
                if (dispatchClick(component.getChildren(), mouseX, mouseY, button)) {
                    return true;
                }
                if (component.preformOnClickEvent(mouseX, mouseY, button)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean dispatchRelease(List<IComponent<?>> components, double mouseX, double mouseY, int button) {
            for (IComponent<?> component : List.copyOf(components)) {
                if (!component.isVisible()) {
                    continue;
                }
                if (dispatchRelease(component.getChildren(), mouseX, mouseY, button)) {
                    return true;
                }
                if (component.preformOnMouseReleaseEvent(mouseX, mouseY, button)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean dispatchScroll(List<IComponent<?>> components, double mouseX, double mouseY, double amountX, double amountY) {
            for (IComponent<?> component : List.copyOf(components)) {
                if (!component.isVisible()) {
                    continue;
                }
                if (dispatchScroll(component.getChildren(), mouseX, mouseY, amountX, amountY)) {
                    return true;
                }
                if (component.preformOnScrollEvent(mouseX, mouseY, amountX, amountY)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean dispatchKeyPressed(List<IComponent<?>> components, int keyCode, int scanCode, int modifiers) {
            for (IComponent<?> component : List.copyOf(components)) {
                if (!component.isVisible()) {
                    continue;
                }
                if (dispatchKeyPressed(component.getChildren(), keyCode, scanCode, modifiers)) {
                    return true;
                }
                if (component.preformOnKeyPressedEvent(keyCode, scanCode, modifiers)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean dispatchCharTyped(List<IComponent<?>> components, char codePoint, int modifiers) {
            for (IComponent<?> component : List.copyOf(components)) {
                if (!component.isVisible()) {
                    continue;
                }
                if (dispatchCharTyped(component.getChildren(), codePoint, modifiers)) {
                    return true;
                }
                if (component.preformOnCharTypedEvent(codePoint, modifiers)) {
                    return true;
                }
            }
            return false;
        }
    }

    static final class ApplianceLabel extends AbstractComponent<ApplianceLabel> {
        private final Text text;

        ApplianceLabel(int x, int y, int width, int height, Component value, LabelAlign align) {
            super(null, x, y, width, height);
            this.text = new Text(Minecraft.getInstance().font, value, 0, 0, width, height);
            this.text.setCenter(align == LabelAlign.CENTER, true);
            this.setText(this.text);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        }

        void setText(Component value) {
            this.text.setText(value);
        }

        void setColor(int color) {
            this.text.setTextColor(color);
        }
    }

    static class ApplianceButton extends AbstractComponent<ApplianceButton> {
        private final StationUiProfile.Theme theme;
        private final ApplianceLabel label;
        private final Runnable onPress;
        private boolean active = true;

        ApplianceButton(int x, int y, int width, int height, StationUiProfile.Theme theme, Component message, Runnable onPress) {
            super(null, x, y, width, height);
            this.theme = theme;
            this.onPress = onPress;
            this.label = new ApplianceLabel(0, 0, width, height, message, LabelAlign.CENTER);
            this.addChild(this.label);
            this.setOnClickEvent((button, screen, mouseX, mouseY, mouseButton) -> {
                if (!this.active || mouseButton != 0 || this.onPress == null) {
                    return false;
                }
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                this.onPress.run();
                return true;
            });
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.label.setColor(this.active ? JazzyGuiRenderer.TITLE_TEXT : JazzyGuiRenderer.TEXT_MUTED);
            JazzyGuiRenderer.drawActionPlate(
                    guiGraphics,
                    0,
                    0,
                    new ActionWidgetSpec(new LayoutRegion(0, 0, this.getWidth(), this.getHeight()), null),
                    this.theme,
                    this.active,
                    this.isTotalHovered(mouseX, mouseY),
                    false);
        }

        void setMessage(Component message) {
            this.label.setText(message);
        }

        void setActive(boolean active) {
            this.active = active;
        }

        boolean isMouseOver(double mouseX, double mouseY) {
            return this.isTotalHovered(mouseX, mouseY);
        }
    }

    static final class ApplianceTextField extends TextBoxComponent {
        static final Predicate<String> INTEGER_INPUT_PREDICATE = value -> value.isEmpty()
                || value.chars().allMatch(Character::isDigit);

        ApplianceTextField(int x, int y, int width, int height, String value) {
            super(x, y, width, height, value);
        }

        void setText(String value) {
            this.setValue(value);
        }

        String textValue() {
            return this.getValue();
        }

        void setTextPredicate(Predicate<String> predicate) {
            this.setFilter(predicate);
        }

        boolean isMouseOver(double mouseX, double mouseY) {
            return this.isTotalHovered(mouseX, mouseY);
        }
    }

    static final class StoveDialWidget extends ApplianceButton {
        private final IntConsumer onChange;
        private int dialValue;

        StoveDialWidget(int x, int y, int width, int height, StationUiProfile.Theme theme, int initialValue, IntConsumer onChange) {
            super(x, y, width, height, theme, Component.empty(), null);
            this.onChange = onChange;
            this.setDialValue(initialValue);
            this.setOnClickEvent((button, screen, mouseX, mouseY, mouseButton) -> {
                if (mouseButton != 0) {
                    return false;
                }
                this.applyValue(this.dialValue + 1);
                return true;
            });
            this.setOnScrollEvent((button, screen, mouseX, mouseY, amountX, amountY) -> {
                if (amountY == 0.0D) {
                    return false;
                }
                this.applyValue(this.dialValue + (amountY > 0.0D ? 1 : -1));
                return true;
            });
        }

        int dialValue() {
            return this.dialValue;
        }

        void setDialValue(int value) {
            this.dialValue = KitchenStationBlockEntity.normalizeStoveDialLevel(value);
            this.setMessage(Component.literal(Integer.toString(this.dialValue)));
        }

        private void applyValue(int value) {
            int normalized = KitchenStationBlockEntity.normalizeStoveDialLevel(value);
            if (normalized == this.dialValue) {
                return;
            }
            this.setDialValue(normalized);
            this.onChange.accept(normalized);
        }
    }

    static final class ApplianceStatusChip extends AbstractComponent<ApplianceStatusChip> {
        private final StationUiProfile.Theme theme;
        private final ApplianceLabel label;

        ApplianceStatusChip(int x, int y, int width, int height, StationUiProfile.Theme theme, Component text) {
            super(null, x, y, width, height);
            this.theme = theme;
            this.label = new ApplianceLabel(0, 0, width, height, text, LabelAlign.CENTER);
            this.label.setColor(JazzyGuiRenderer.TEXT_MUTED);
            this.addChild(this.label);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            JazzyGuiRenderer.drawActionPlate(
                    guiGraphics,
                    0,
                    0,
                    new ActionWidgetSpec(new LayoutRegion(0, 0, this.getWidth(), this.getHeight()), null),
                    this.theme,
                    true,
                    false,
                    false);
        }

        void setText(Component text) {
            this.label.setText(text);
        }

        void setTextColor(int color) {
            this.label.setColor(color);
        }
    }
}
