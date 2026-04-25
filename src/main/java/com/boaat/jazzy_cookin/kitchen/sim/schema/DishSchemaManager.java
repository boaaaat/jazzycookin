package com.boaat.jazzy_cookin.kitchen.sim.schema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class DishSchemaManager {
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "dish_schema";
    private static volatile List<DishSchemaDefinition> loadedSchemas = List.of();

    private DishSchemaManager() {
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static List<DishSchemaDefinition> combinedSchemas(List<DishSchemaDefinition> fallbackSchemas) {
        Map<String, DishSchemaDefinition> byKey = new LinkedHashMap<>();
        for (DishSchemaDefinition schema : fallbackSchemas) {
            byKey.put(schema.key(), schema);
        }
        for (DishSchemaDefinition schema : loadedSchemas) {
            byKey.put(schema.key(), schema);
        }
        return List.copyOf(byKey.values());
    }

    private static final class ReloadListener extends SimpleJsonResourceReloadListener {
        private ReloadListener() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> elements, ResourceManager resourceManager, ProfilerFiller profiler) {
            List<DishSchemaDefinition> parsed = new ArrayList<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : elements.entrySet()) {
                DishSchemaDefinition.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                        .resultOrPartial(message -> JazzyCookin.LOGGER.warn("Failed to load dish schema {}: {}", entry.getKey(), message))
                        .ifPresent(parsed::add);
            }
            loadedSchemas = List.copyOf(parsed);
            JazzyCookin.LOGGER.info("Loaded {} dish schemas", loadedSchemas.size());
        }
    }
}
