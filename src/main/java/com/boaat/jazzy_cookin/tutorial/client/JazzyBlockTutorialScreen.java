package com.boaat.jazzy_cookin.tutorial.client;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.boaat.jazzy_cookin.tutorial.KitchenBlockTutorial;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class JazzyBlockTutorialScreen extends Screen {
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 206;

    private final KitchenBlockTutorial tutorial;
    private int stepIndex;
    private Button previousButton;
    private Button nextButton;

    public JazzyBlockTutorialScreen(KitchenBlockTutorial tutorial) {
        super(Component.translatable("screen.jazzycookin.block_tutorial.title"));
        this.tutorial = tutorial;
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        this.previousButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.jazzycookin.block_tutorial.previous"), button -> this.focusStep(-1))
                .bounds(left + 18, top + PANEL_HEIGHT - 30, 72, 20)
                .build());
        this.nextButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.jazzycookin.block_tutorial.next"), button -> this.focusStep(1))
                .bounds(left + PANEL_WIDTH - 90, top + PANEL_HEIGHT - 30, 72, 20)
                .build());
        this.updateButtons();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x9004080D);

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xF0141820);
        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + 26, 0xFF20343A);
        guiGraphics.fill(left, top + 26, left + PANEL_WIDTH, top + 28, 0xFF62D2C8);
        guiGraphics.fill(left + 14, top + 44, left + 72, top + 102, 0xFF263139);
        guiGraphics.fill(left + 15, top + 45, left + 71, top + 101, 0xFF111820);

        guiGraphics.drawString(this.font, this.title, left + 10, top + 9, 0xFFEAF7F4, false);
        guiGraphics.drawString(this.font, this.tutorial.category().copy().withStyle(ChatFormatting.AQUA), left + PANEL_WIDTH - 10 - this.font.width(this.tutorial.category()), top + 9, 0xFF91E8E0, false);
        guiGraphics.renderItem(this.tutorial.icon(), left + 35, top + 65);

        guiGraphics.drawString(this.font, this.tutorial.title(), left + 88, top + 45, 0xFFFFF3D4, false);
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.jazzycookin.block_tutorial.step_count", this.stepIndex + 1, this.tutorial.steps().size()),
                left + 88,
                top + 59,
                0xFFAAB5C0,
                false
        );

        Component step = this.tutorial.steps().get(this.stepIndex);
        List<FormattedCharSequence> lines = this.font.split(step, PANEL_WIDTH - 116);
        int y = top + 78;
        int maxTextBottom = top + PANEL_HEIGHT - 48;
        for (FormattedCharSequence line : lines) {
            if (y + 9 > maxTextBottom) {
                break;
            }
            guiGraphics.drawString(this.font, line, left + 88, y, 0xFFE5E8EC, false);
            y += 11;
        }

        this.renderProgress(guiGraphics, left + 18, top + PANEL_HEIGHT - 54, PANEL_WIDTH - 36);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
    }

    @Override
    public void renderTransparentBackground(GuiGraphics guiGraphics) {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A || keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            this.focusStep(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D || keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            this.focusStep(1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderProgress(GuiGraphics guiGraphics, int x, int y, int width) {
        int stepCount = this.tutorial.steps().size();
        int gap = 4;
        int nodeWidth = Math.max(16, (width - gap * (stepCount - 1)) / stepCount);
        for (int i = 0; i < stepCount; i++) {
            int nodeX = x + i * (nodeWidth + gap);
            int fill = i == this.stepIndex ? 0xFF62D2C8 : (i < this.stepIndex ? 0xFF9AD66B : 0xFF43515C);
            guiGraphics.fill(nodeX, y, nodeX + nodeWidth, y + 6, fill);
        }
    }

    private void focusStep(int delta) {
        this.stepIndex = Math.max(0, Math.min(this.tutorial.steps().size() - 1, this.stepIndex + delta));
        this.updateButtons();
    }

    private void updateButtons() {
        if (this.previousButton != null) {
            this.previousButton.active = this.stepIndex > 0;
        }
        if (this.nextButton != null) {
            this.nextButton.active = this.stepIndex < this.tutorial.steps().size() - 1;
        }
    }
}
