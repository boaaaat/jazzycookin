package com.boaat.jazzy_cookin.kitchen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class MasteryProgress {
    private static final String ROOT_KEY = "jazzycookin_mastery";
    private static final String TOTAL_SERVES = "total_serves";
    private static final String TOTAL_ENJOYMENT = "total_enjoyment";
    private static final String TOTAL_NOURISHMENT = "total_nourishment";

    private MasteryProgress() {
    }

    public static CompoundTag get(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(ROOT_KEY)) {
            root.put(ROOT_KEY, new CompoundTag());
        }
        return root.getCompound(ROOT_KEY);
    }

    public static void awardForMeal(Player player, ItemStack stack, QualityBreakdown breakdown) {
        CompoundTag mastery = get(player);
        mastery.putInt(TOTAL_SERVES, mastery.getInt(TOTAL_SERVES) + 1);
        mastery.putInt(TOTAL_ENJOYMENT, mastery.getInt(TOTAL_ENJOYMENT) + breakdown.enjoyment());
        mastery.putInt(TOTAL_NOURISHMENT, mastery.getInt(TOTAL_NOURISHMENT) + breakdown.nourishment());

        ResourceLocation key = stack.getItemHolder().unwrapKey()
                .map(reference -> reference.location())
                .orElse(ResourceLocation.withDefaultNamespace("unknown_meal"));
        String pathKey = key.getPath();
        mastery.putInt(pathKey, mastery.getInt(pathKey) + 1);
    }

    public static int totalServes(Player player) {
        return get(player).getInt(TOTAL_SERVES);
    }

    public static int forMeal(Player player, String path) {
        return get(player).getInt(path);
    }
}
