package com.boaat.jazzy_cookin.kitchen.sim.domain;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.MeasureUnit;
import com.boaat.jazzy_cookin.kitchen.MeasuredQuantity;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfiles;
import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;
import com.boaat.jazzy_cookin.kitchen.sim.SimulationSnapshot;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;
import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.world.item.ItemStack;

public final class MeasuringSimulationDomain implements StationSimulationDomain {
    @Override
    public SimulationDomainType type() {
        return SimulationDomainType.PREP;
    }

    @Override
    public boolean supports(StationSimulationAccess access) {
        return (access.simulationStationType() == StationType.PREP_TABLE || access.simulationStationType() == StationType.MIXING_BOWL)
                && isMeasuringTool(ToolProfile.fromStack(access.simulationItem(access.toolSlot())))
                && firstMeasurableInput(access) >= 0;
    }

    @Override
    public KitchenMethod method(StationSimulationAccess access) {
        return KitchenMethod.MEASURE;
    }

    @Override
    public SimulationSnapshot snapshot(StationSimulationAccess access, int executionMode) {
        return new SimulationSnapshot(executionMode, 0, 72, 72, 72, 100, 0, 0, 0, 0, 0, 0);
    }

    @Override
    public boolean handleAction(StationSimulationAccess access, int buttonId) {
        if (buttonId != 6 || access.simulationLevel() == null) {
            return false;
        }
        int inputSlot = firstMeasurableInput(access);
        if (inputSlot < 0) {
            return false;
        }
        ItemStack input = access.simulationItem(inputSlot);
        ItemStack portion = input.copyWithCount(1);
        Preset preset = presetFor(input, ToolProfile.fromStack(access.simulationItem(access.toolSlot())), access.simulationControlSetting());
        KitchenStackUtil.setMeasuredQuantity(portion, new MeasuredQuantity(
                preset.amount(),
                preset.unit(),
                preset.label(),
                KitchenStackUtil.itemKey(input),
                true
        ));
        KitchenStackUtil.setCreatedTick(portion, access.simulationLevel().getGameTime(), access.simulationLevel().getGameTime());

        ItemStack output = access.simulationItem(access.outputSlot());
        if (output.isEmpty()) {
            access.simulationRemoveItem(inputSlot, 1);
            access.simulationSetItem(access.outputSlot(), portion);
            access.simulationMarkChanged();
            return true;
        }
        if (canMergeMeasured(output, portion)) {
            MeasuredQuantity existing = KitchenStackUtil.measuredQuantity(output);
            KitchenStackUtil.setMeasuredQuantity(output, new MeasuredQuantity(
                    existing.amount() + preset.amount(),
                    existing.unit(),
                    formatAmount(existing.amount() + preset.amount(), existing.unit()),
                    existing.sourceItem(),
                    true
            ));
            access.simulationRemoveItem(inputSlot, 1);
            access.simulationSetItem(access.outputSlot(), output);
            access.simulationMarkChanged();
            return true;
        }
        if (!access.simulationCanAcceptStack(access.outputSlot(), portion)) {
            return false;
        }
        access.simulationRemoveItem(inputSlot, 1);
        access.simulationMergeIntoSlot(access.outputSlot(), portion);
        access.simulationMarkChanged();
        return true;
    }

    @Override
    public void serverTick(StationSimulationAccess access) {
    }

    private static boolean canMergeMeasured(ItemStack output, ItemStack portion) {
        MeasuredQuantity existing = KitchenStackUtil.measuredQuantity(output);
        MeasuredQuantity incoming = KitchenStackUtil.measuredQuantity(portion);
        return existing != null
                && incoming != null
                && output.is(portion.getItem())
                && existing.unit() == incoming.unit()
                && existing.sourceItem().equals(incoming.sourceItem());
    }

    private static int firstMeasurableInput(StationSimulationAccess access) {
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof KitchenIngredientItem) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isMeasuringTool(ToolProfile profile) {
        return profile == ToolProfile.MEASURING_CUP
                || profile == ToolProfile.MEASURING_SPOONS
                || profile == ToolProfile.KITCHEN_SCALE;
    }

    private static Preset presetFor(ItemStack stack, ToolProfile profile, int controlSetting) {
        if (profile == ToolProfile.KITCHEN_SCALE) {
            if (stack.is(JazzyItems.ingredient(JazzyItems.IngredientId.ALL_PURPOSE_FLOUR).get())) {
                return new Preset(30.0F, MeasureUnit.GRAM, "30 g");
            }
            if (FoodMaterialProfiles.hasTrait(stack, FoodTrait.SWEETENER)) {
                return new Preset(50.0F, MeasureUnit.GRAM, "50 g");
            }
            return switch (controlSetting) {
                case 0 -> new Preset(5.0F, MeasureUnit.GRAM, "5 g");
                case 2 -> new Preset(100.0F, MeasureUnit.GRAM, "100 g");
                default -> new Preset(25.0F, MeasureUnit.GRAM, "25 g");
            };
        }
        if (profile == ToolProfile.MEASURING_SPOONS) {
            if (FoodMaterialProfiles.hasTrait(stack, FoodTrait.SALT)) {
                return new Preset(1.25F, MeasureUnit.MILLILITER, "1/4 tsp");
            }
            return switch (controlSetting) {
                case 0 -> new Preset(1.25F, MeasureUnit.MILLILITER, "1/4 tsp");
                case 2 -> new Preset(15.0F, MeasureUnit.MILLILITER, "1 tbsp");
                default -> new Preset(5.0F, MeasureUnit.MILLILITER, "1 tsp");
            };
        }
        if (FoodMaterialProfiles.hasTrait(stack, FoodTrait.DAIRY)) {
            return new Preset(160.0F, MeasureUnit.MILLILITER, "2/3 cup");
        }
        return switch (controlSetting) {
            case 0 -> new Preset(60.0F, MeasureUnit.MILLILITER, "1/4 cup");
            case 2 -> new Preset(240.0F, MeasureUnit.MILLILITER, "1 cup");
            default -> new Preset(120.0F, MeasureUnit.MILLILITER, "1/2 cup");
        };
    }

    private static String formatAmount(float amount, MeasureUnit unit) {
        if (unit == MeasureUnit.GRAM) {
            return Math.round(amount) + " g";
        }
        if (unit == MeasureUnit.MILLILITER) {
            return Math.round(amount * 100.0F) / 100.0F + " ml";
        }
        return Math.round(amount) + " " + unit.getSerializedName();
    }

    private record Preset(float amount, MeasureUnit unit, String label) {
    }
}
