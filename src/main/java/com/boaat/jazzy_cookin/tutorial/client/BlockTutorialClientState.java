package com.boaat.jazzy_cookin.tutorial.client;

import org.lwjgl.glfw.GLFW;

import com.boaat.jazzy_cookin.tutorial.KitchenBlockTutorial;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public final class BlockTutorialClientState {
    private static final KeyMapping OPEN_TUTORIAL_KEY = new KeyMapping(
            "key.jazzycookin.open_block_tutorial",
            GLFW.GLFW_KEY_H,
            "key.categories.jazzycookin"
    );

    private BlockTutorialClientState() {
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_TUTORIAL_KEY);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_TUTORIAL_KEY.consumeClick()) {
            if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
                continue;
            }
            if (!(minecraft.hitResult instanceof BlockHitResult blockHit) || minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
                continue;
            }
            BlockState state = minecraft.level.getBlockState(blockHit.getBlockPos());
            KitchenBlockTutorial.forBlock(state.getBlock()).ifPresent(tutorial ->
                    minecraft.setScreen(new JazzyBlockTutorialScreen(tutorial)));
        }
    }
}
