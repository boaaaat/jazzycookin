package com.boaat.jazzy_cookin.kitchen.sim.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Items;
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

    public static List<String> validationProblems(List<DishSchemaDefinition> schemas) {
        List<String> problems = new ArrayList<>();
        Map<String, DishSchemaDefinition> byKey = new HashMap<>();
        Map<Integer, DishSchemaDefinition> byPreviewId = new HashMap<>();
        for (DishSchemaDefinition schema : schemas) {
            if (schema.key().isBlank()) {
                problems.add("schema has a blank key for result " + schema.result());
            }
            DishSchemaDefinition duplicateKey = byKey.putIfAbsent(schema.key(), schema);
            if (duplicateKey != null) {
                problems.add("duplicate schema key " + schema.key());
            }
            DishSchemaDefinition duplicatePreviewId = byPreviewId.putIfAbsent(schema.previewId(), schema);
            if (duplicatePreviewId != null) {
                problems.add("duplicate preview id " + schema.previewId() + " for " + duplicatePreviewId.key() + " and " + schema.key());
            }
            if (schema.previewId() <= 0) {
                problems.add("schema " + schema.key() + " has a non-positive preview id");
            }
            if (BuiltInRegistries.ITEM.get(schema.result()) == Items.AIR) {
                problems.add("schema " + schema.key() + " points at missing result item " + schema.result());
            }
            if (schema.previewThreshold() < 0.0F || schema.previewThreshold() > 1.0F
                    || schema.finalizeThreshold() < 0.0F || schema.finalizeThreshold() > 1.0F) {
                problems.add("schema " + schema.key() + " has thresholds outside 0..1");
            }
            if (schema.requiredRoles().isEmpty() && schema.requiredTechniques().isEmpty()) {
                problems.add("schema " + schema.key() + " has no required roles or techniques");
            }
        }
        return List.copyOf(problems);
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
            validationProblems(parsed).forEach(problem -> JazzyCookin.LOGGER.warn("Dish schema validation: {}", problem));
            loadedSchemas = List.copyOf(parsed);
            JazzyCookin.LOGGER.info("Loaded {} dish schemas", loadedSchemas.size());
        }
    }
}
