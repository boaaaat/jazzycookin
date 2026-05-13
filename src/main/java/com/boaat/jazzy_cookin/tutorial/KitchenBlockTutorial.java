package com.boaat.jazzy_cookin.tutorial;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.block.AppleSaplingBlock;
import com.boaat.jazzy_cookin.block.KitchenSourceBlock;
import com.boaat.jazzy_cookin.block.KitchenStationBlock;
import com.boaat.jazzy_cookin.block.KitchenStorageBlock;
import com.boaat.jazzy_cookin.kitchen.KitchenSourceProfile;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.StorageType;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public record KitchenBlockTutorial(String id, Component title, Component category, ItemStack icon, List<Component> steps) {
    public static Optional<KitchenBlockTutorial> forBlock(Block block) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        if (!JazzyCookin.MODID.equals(key.getNamespace())) {
            return Optional.empty();
        }

        String id = key.getPath();
        ItemStack icon = new ItemStack(block.asItem());
        Component title = Component.translatable(block.getDescriptionId());
        if (block instanceof KitchenStationBlock stationBlock) {
            return Optional.of(station(id, title, icon, stationBlock.stationType()));
        }
        if (block instanceof KitchenStorageBlock storageBlock) {
            return Optional.of(storage(id, title, icon, storageBlock.storageType()));
        }
        if (block instanceof KitchenSourceBlock sourceBlock) {
            return Optional.of(source(id, title, icon, sourceBlock.profile()));
        }
        if (block instanceof AppleSaplingBlock) {
            return Optional.of(appleSapling(id, title, icon));
        }
        return Optional.empty();
    }

    public static boolean hasTutorial(Block block) {
        return forBlock(block).isPresent();
    }

    private static KitchenBlockTutorial station(String id, Component title, ItemStack icon, StationType stationType) {
        List<Component> steps = new ArrayList<>();
        steps.add(Component.translatable("tutorial.jazzycookin.station.open", stationType.displayName()));
        steps.add(Component.translatable("tutorial.jazzycookin." + id + ".purpose"));
        if (stationType.usesTools()) {
            steps.add(Component.translatable("tutorial.jazzycookin.station.tools"));
        } else {
            steps.add(Component.translatable("tutorial.jazzycookin.station.no_tools"));
        }
        if (stationType.supportsHeat()) {
            steps.add(Component.translatable("tutorial.jazzycookin.station.heat"));
        } else {
            steps.add(Component.translatable("tutorial.jazzycookin.station.control"));
        }
        steps.add(Component.translatable("tutorial.jazzycookin.station.recipe_book"));
        return new KitchenBlockTutorial(
                id,
                title,
                Component.translatable("tutorial.jazzycookin.category.station"),
                icon,
                List.copyOf(steps)
        );
    }

    private static KitchenBlockTutorial storage(String id, Component title, ItemStack icon, StorageType storageType) {
        return new KitchenBlockTutorial(
                id,
                title,
                Component.translatable("tutorial.jazzycookin.category.storage"),
                icon,
                List.of(
                        Component.translatable("tutorial.jazzycookin.storage.open", title),
                        Component.translatable("tutorial.jazzycookin." + id + ".purpose"),
                        Component.translatable("tutorial.jazzycookin.storage.conditions", Math.round(storageType.targetTempC()), Math.round(storageType.decayMultiplier() * 100.0F)),
                        Component.translatable("tutorial.jazzycookin.storage.recipe_book")
                )
        );
    }

    private static KitchenBlockTutorial source(String id, Component title, ItemStack icon, KitchenSourceProfile profile) {
        List<Component> steps = new ArrayList<>();
        steps.add(Component.translatable("tutorial.jazzycookin.source.place", title));
        steps.add(Component.translatable("tutorial.jazzycookin." + id + ".purpose"));
        steps.add(Component.translatable("tutorial.jazzycookin.source.grow", profile.minimumLight(), profile.ripeAge()));
        if (profile.prefersHydration()) {
            steps.add(Component.translatable("tutorial.jazzycookin.source.water"));
        } else {
            steps.add(Component.translatable("tutorial.jazzycookin.source.harvest"));
        }
        steps.add(Component.translatable("tutorial.jazzycookin.source.recipe_book"));
        return new KitchenBlockTutorial(
                id,
                title,
                Component.translatable("tutorial.jazzycookin.category.source"),
                icon,
                List.copyOf(steps)
        );
    }

    private static KitchenBlockTutorial appleSapling(String id, Component title, ItemStack icon) {
        return new KitchenBlockTutorial(
                id,
                title,
                Component.translatable("tutorial.jazzycookin.category.source"),
                icon,
                List.of(
                        Component.translatable("tutorial.jazzycookin.source.place", title),
                        Component.translatable("tutorial.jazzycookin.apple_sapling.purpose"),
                        Component.translatable("tutorial.jazzycookin.apple_sapling.grow"),
                        Component.translatable("tutorial.jazzycookin.source.water"),
                        Component.translatable("tutorial.jazzycookin.source.recipe_book")
                )
        );
    }
}
