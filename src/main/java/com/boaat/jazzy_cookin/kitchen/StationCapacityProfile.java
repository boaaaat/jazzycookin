package com.boaat.jazzy_cookin.kitchen;

public record StationCapacityProfile(int inputCount) {
    public static final int INPUT_START = 0;
    public static final int MAX_INPUT_SLOTS = 10;
    public static final int MAX_INPUT_SLOT = INPUT_START + MAX_INPUT_SLOTS - 1;
    public static final int TOOL_SLOT = 10;
    public static final int OUTPUT_SLOT = 11;
    public static final int BYPRODUCT_SLOT = 12;
    public static final int TOTAL_SLOTS = 13;

    public StationCapacityProfile {
        if (inputCount < 1 || inputCount > MAX_INPUT_SLOTS) {
            throw new IllegalArgumentException("Station input count must stay between 1 and " + MAX_INPUT_SLOTS);
        }
    }

    public static StationCapacityProfile forStation(StationType stationType) {
        return switch (stationType) {
            case PREP_TABLE -> new StationCapacityProfile(10);
            case MIXING_BOWL, PLATING_STATION -> new StationCapacityProfile(8);
            case STOVE, OVEN, FOOD_PROCESSOR, BLENDER, FERMENTATION_CROCK, CANNING_STATION, STEAMER -> new StationCapacityProfile(6);
            case FREEZE_DRYER, DRYING_RACK, SMOKER -> new StationCapacityProfile(5);
            case MICROWAVE, JUICER, SPICE_GRINDER, STRAINER, COOLING_RACK, RESTING_BOARD -> new StationCapacityProfile(4);
        };
    }

    public int inputStart() {
        return INPUT_START;
    }

    public int inputEnd() {
        return this.inputCount == 0 ? INPUT_START - 1 : INPUT_START + this.inputCount - 1;
    }

    public boolean isInputSlot(int slot) {
        return slot >= INPUT_START && slot <= MAX_INPUT_SLOT;
    }

    public boolean isActiveInputSlot(int slot) {
        return slot >= INPUT_START && slot <= this.inputEnd();
    }

    public boolean isInactiveInputSlot(int slot) {
        return this.isInputSlot(slot) && !this.isActiveInputSlot(slot);
    }

    public int visibleStationSlotCount() {
        return this.inputCount + 3;
    }

    public int toolMenuSlotIndex() {
        return this.inputCount;
    }

    public int outputMenuSlotIndex() {
        return this.inputCount + 1;
    }

    public int byproductMenuSlotIndex() {
        return this.inputCount + 2;
    }
}
