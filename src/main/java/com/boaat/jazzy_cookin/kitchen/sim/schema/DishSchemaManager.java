package com.boaat.jazzy_cookin.kitchen.sim.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
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

    public static List<DishSchemaDefinition> schemas() {
        return loadedSchemas;
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
            if (schema.ingredients().isEmpty()) {
                problems.add("schema " + schema.key() + " has no quantified ingredients");
            }
            if (schema.steps().isEmpty()) {
                problems.add("schema " + schema.key() + " has no ordered steps");
            }
            validateRealismTargets(schema, problems);
            for (DishIngredientRequirement ingredient : schema.ingredients()) {
                ingredient.item().ifPresent(item -> {
                    if (BuiltInRegistries.ITEM.get(item) == Items.AIR) {
                        problems.add("schema " + schema.key() + " points at missing ingredient item " + item);
                    }
                });
                if (ingredient.hasMeasuredAmount() && ingredient.maxAmount() < ingredient.minAmount()) {
                    problems.add("schema " + schema.key() + " has an invalid amount range for " + ingredient.item().map(ResourceLocation::toString).orElse(ingredient.role().getSerializedName()));
                }
                if (ingredient.item().isPresent()
                        && ingredient.role() != DishRole.CONTAINER
                        && ingredient.allTraits().isEmpty()
                        && ingredient.anyTraits().isEmpty()) {
                    problems.add("schema " + schema.key() + " has item-specific food ingredient "
                            + ingredient.item().get() + " without explicit trait filters");
                }
            }
            Set<String> stepIds = new java.util.HashSet<>();
            for (DishStepRequirement step : schema.steps()) {
                if (step.id().isBlank()) {
                    problems.add("schema " + schema.key() + " has a blank step id");
                }
                if (!stepIds.add(step.id())) {
                    problems.add("schema " + schema.key() + " repeats step id " + step.id());
                }
                if (step.progressTarget() <= 0.0F) {
                    problems.add("schema " + schema.key() + " step " + step.id() + " has a non-positive progress target");
                }
                validateStepTools(schema, step, problems);
            }
        }
        for (DishSchemaDefinition schema : schemas) {
            Set<String> reachableStepIds = new java.util.HashSet<>();
            schema.steps().forEach(step -> reachableStepIds.add(step.id()));
            for (String prerequisiteSchema : schema.prerequisiteSchemas()) {
                DishSchemaDefinition prerequisite = byKey.get(prerequisiteSchema);
                if (prerequisite != null) {
                    prerequisite.steps().forEach(step -> reachableStepIds.add(step.id()));
                }
            }
            for (String prerequisiteSchema : schema.prerequisiteSchemas()) {
                if (!byKey.containsKey(prerequisiteSchema)) {
                    problems.add("schema " + schema.key() + " depends on missing prerequisite schema " + prerequisiteSchema);
                }
            }
            for (ResourceLocation servingItem : schema.servingItems()) {
                if (BuiltInRegistries.ITEM.get(servingItem) == Items.AIR) {
                    problems.add("schema " + schema.key() + " points at missing serving item " + servingItem);
                }
            }
            for (DishStepRequirement step : schema.steps()) {
                for (String prerequisite : step.prerequisites()) {
                    if (!reachableStepIds.contains(prerequisite)) {
                        problems.add("schema " + schema.key() + " step " + step.id() + " depends on missing step " + prerequisite);
                    }
                }
            }
        }
        return List.copyOf(problems);
    }

    private static void validateRealismTargets(DishSchemaDefinition schema, List<String> problems) {
        if (usesAnyTechnique(schema, DishTechnique.MIXED, DishTechnique.DIP_OR_COAT, DishTechnique.CUT, DishTechnique.PREPPED)
                && (!schema.targets().hasProcessTargets() || schema.weights().process() <= 0.0F)) {
            problems.add("schema " + schema.key() + " handles food but does not score process targets");
        }
        if (usesAnyTechnique(schema, DishTechnique.PAN_FRIED, DishTechnique.SIMMERED, DishTechnique.BAKED)) {
            if (!schema.targets().hasProcessTargets() || schema.weights().process() <= 0.0F) {
                problems.add("schema " + schema.key() + " cooks food but does not score timing or handling process");
            }
            if (!schema.targets().hasThermalTargets() || schema.weights().thermal() <= 0.0F) {
                problems.add("schema " + schema.key() + " cooks food but does not score thermal targets");
            }
        }
    }

    private static void validateStepTools(DishSchemaDefinition schema, DishStepRequirement step, List<String> problems) {
        if (step.technique() == DishTechnique.PAN_FRIED
                && !hasAnyTool(step, ToolProfile.PAN, ToolProfile.SKILLET, ToolProfile.FRYING_SKILLET)) {
            problems.add("schema " + schema.key() + " step " + step.id() + " pan-fries without pan cookware tools");
        } else if (step.technique() == DishTechnique.SIMMERED
                && !hasAnyTool(step, ToolProfile.POT, ToolProfile.STOCK_POT, ToolProfile.SAUCEPAN)) {
            problems.add("schema " + schema.key() + " step " + step.id() + " simmers without pot cookware tools");
        } else if (step.technique() == DishTechnique.BAKED
                && !hasAnyTool(step, ToolProfile.BAKING_TRAY, ToolProfile.PIE_TIN)) {
            problems.add("schema " + schema.key() + " step " + step.id() + " bakes without bakeware tools");
        } else if ((step.technique() == DishTechnique.CUT || step.technique() == DishTechnique.PREPPED)
                && !hasAnyTool(step, ToolProfile.KNIFE, ToolProfile.CHEF_KNIFE, ToolProfile.PARING_KNIFE, ToolProfile.CLEAVER, ToolProfile.TABLE_KNIFE)) {
            problems.add("schema " + schema.key() + " step " + step.id() + " preps food without knife tools");
        }
    }

    private static boolean usesAnyTechnique(DishSchemaDefinition schema, DishTechnique... techniques) {
        for (DishTechnique technique : techniques) {
            if (schema.requiredTechniques().contains(technique)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnyTool(DishStepRequirement step, ToolProfile... tools) {
        for (ToolProfile tool : tools) {
            if (step.tool().filter(tool::equals).isPresent() || step.tools().contains(tool)) {
                return true;
            }
        }
        return false;
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
