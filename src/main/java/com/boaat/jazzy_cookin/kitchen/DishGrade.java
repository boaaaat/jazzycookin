package com.boaat.jazzy_cookin.kitchen;

import net.minecraft.network.chat.Component;

public enum DishGrade {
    FAILED(0.0F, "failed"),
    POOR(0.25F, "poor"),
    GOOD(0.45F, "good"),
    GREAT(0.62F, "great"),
    EXCELLENT(0.8F, "excellent"),
    MASTERPIECE(0.93F, "masterpiece");

    private final float floor;
    private final String key;

    DishGrade(float floor, String key) {
        this.floor = floor;
        this.key = key;
    }

    public static DishGrade fromScore(float score) {
        DishGrade result = FAILED;
        for (DishGrade grade : values()) {
            if (score >= grade.floor) {
                result = grade;
            }
        }
        return result;
    }

    public Component displayName() {
        return Component.translatable("grade.jazzycookin." + this.key);
    }
}
