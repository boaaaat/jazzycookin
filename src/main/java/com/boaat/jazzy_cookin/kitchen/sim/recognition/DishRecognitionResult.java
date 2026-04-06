package com.boaat.jazzy_cookin.kitchen.sim.recognition;

import java.util.function.Supplier;

import net.minecraft.world.item.Item;

public record DishRecognitionResult(String key, int previewId, Supplier<? extends Item> resultItem, float score) {
}
