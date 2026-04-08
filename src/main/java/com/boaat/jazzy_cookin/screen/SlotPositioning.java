package com.boaat.jazzy_cookin.screen;

import java.lang.reflect.Field;

import net.minecraft.world.inventory.Slot;

final class SlotPositioning {
    private static final Field SLOT_X_FIELD = field("x");
    private static final Field SLOT_Y_FIELD = field("y");

    private SlotPositioning() {
    }

    static void setPosition(Slot slot, int x, int y) {
        try {
            SLOT_X_FIELD.setInt(slot, x);
            SLOT_Y_FIELD.setInt(slot, y);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to reposition slot", exception);
        }
    }

    private static Field field(String name) {
        try {
            Field field = Slot.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to access Slot." + name, exception);
        }
    }
}
