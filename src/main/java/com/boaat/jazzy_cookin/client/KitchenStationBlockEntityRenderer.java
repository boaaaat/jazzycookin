package com.boaat.jazzy_cookin.client;

import com.boaat.jazzy_cookin.block.entity.KitchenStationBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class KitchenStationBlockEntityRenderer implements BlockEntityRenderer<KitchenStationBlockEntity> {
    private static final float[][] INPUT_POSITIONS = {
            {0.24F, 0.30F},
            {0.40F, 0.22F},
            {0.58F, 0.22F},
            {0.74F, 0.30F},
            {0.28F, 0.56F},
            {0.72F, 0.56F}
    };

    public KitchenStationBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            KitchenStationBlockEntity station,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        Level level = station.getLevel();
        float time = level == null ? partialTick : level.getGameTime() + partialTick;
        boolean active = station.simulationActive();
        int renderedInputs = 0;
        for (int slot = station.inputStart(); slot <= station.inputEnd() && renderedInputs < INPUT_POSITIONS.length; slot++) {
            ItemStack stack = station.getItem(slot);
            if (!stack.isEmpty()) {
                float[] position = INPUT_POSITIONS[renderedInputs];
                float pulse = active ? ((time * 0.035F + renderedInputs * 0.19F) % 1.0F) : 0.0F;
                float x = lerp(position[0], 0.50F, pulse * 0.30F);
                float z = lerp(position[1], 0.50F, pulse * 0.30F);
                this.renderStack(station, stack, poseStack, bufferSource, packedLight, x, z, time, renderedInputs, active);
                renderedInputs++;
            }
        }

        this.renderStationSlot(station, KitchenStationBlockEntity.OUTPUT_SLOT, poseStack, bufferSource, packedLight, 0.50F, 0.73F, time, 8, active);
        this.renderStationSlot(station, KitchenStationBlockEntity.BYPRODUCT_SLOT, poseStack, bufferSource, packedLight, 0.73F, 0.73F, time, 9, active);
    }

    private void renderStationSlot(
            KitchenStationBlockEntity station,
            int slot,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            float x,
            float z,
            float time,
            int seed,
            boolean active
    ) {
        ItemStack stack = station.getItem(slot);
        if (!stack.isEmpty()) {
            this.renderStack(station, stack, poseStack, bufferSource, packedLight, x, z, time, seed, active);
        }
    }

    private void renderStack(
            KitchenStationBlockEntity station,
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            float x,
            float z,
            float time,
            int seed,
            boolean active
    ) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        ItemStack displayStack = stack.copyWithCount(1);
        float bob = active ? (float) Math.sin((time + seed * 7.0F) * 0.18F) * 0.035F : 0.0F;
        poseStack.pushPose();
        poseStack.translate(x, 1.03F + bob, z);
        poseStack.mulPose(Axis.YP.rotationDegrees((time * 2.0F + seed * 41.0F) % 360.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(0.34F, 0.34F, 0.34F);
        itemRenderer.renderStatic(
                displayStack,
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                station.getLevel(),
                seed
        );
        poseStack.popPose();
    }

    private static float lerp(float start, float end, float amount) {
        return start + (end - start) * amount;
    }
}
