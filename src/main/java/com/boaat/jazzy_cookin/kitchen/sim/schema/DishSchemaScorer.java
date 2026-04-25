package com.boaat.jazzy_cookin.kitchen.sim.schema;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishRecognitionResult;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class DishSchemaScorer {
    private static final List<DishSchemaDefinition> BUILTIN_SCHEMAS = builtInSchemas();

    private DishSchemaScorer() {
    }

    public static List<DishSchemaDefinition> schemas() {
        return DishSchemaManager.combinedSchemas(BUILTIN_SCHEMAS);
    }

    public static DishRecognitionResult bestRecognition(FoodMatterData matter, Predicate<Item> filter, boolean finalize) {
        DishSchemaScore score = bestScore(matter, filter);
        if (score == null) {
            return null;
        }
        float threshold = finalize ? score.schema().finalizeThreshold() : score.schema().previewThreshold();
        if (score.score() < threshold) {
            return null;
        }
        return new DishRecognitionResult(
                score.schema().key(),
                score.schema().previewId(),
                score.resultItem(),
                score.score(),
                score.schema().desirability()
        );
    }

    public static DishRecognitionResult descriptor(int previewId) {
        for (DishSchemaDefinition schema : schemas()) {
            if (schema.previewId() == previewId) {
                Item item = itemFor(schema);
                if (item != Items.AIR) {
                    return new DishRecognitionResult(schema.key(), schema.previewId(), () -> item, 1.0F, schema.desirability());
                }
            }
        }
        return null;
    }

    public static boolean hasRecognizerFor(Item item) {
        return schemas().stream().anyMatch(schema -> itemFor(schema) == item);
    }

    public static DishSchemaScore bestScore(FoodMatterData matter, Predicate<Item> filter) {
        if (matter == null) {
            return null;
        }
        DishAttemptContext context = DishAttemptContext.fromMatter(matter);
        return schemas().stream()
                .map(schema -> score(schema, context))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(score -> filter.test(score.resultItem().get()))
                .max(Comparator.comparing(DishSchemaScore::score).thenComparing(score -> score.schema().desirability()))
                .orElse(null);
    }

    public static Optional<DishSchemaScore> score(DishSchemaDefinition schema, DishAttemptContext context) {
        Item item = itemFor(schema);
        if (item == Items.AIR || context.matter() == null) {
            return Optional.empty();
        }
        FoodMatterData matter = context.matter();
        if (schema.forbiddenTraits().stream().anyMatch(matter::hasTrait)) {
            return Optional.empty();
        }

        float roleScore = roleScore(schema.requiredRoles(), matter);
        if (!schema.requiredRoles().isEmpty() && roleScore < 0.35F) {
            return Optional.empty();
        }
        float optionalRoleBonus = optionalRoleBonus(schema.optionalRoles(), matter);
        float compositionScore = schema.targets().compositionScore(matter);
        float seasoningScore = schema.targets().seasoningScore(matter);
        float cookingScore = schema.targets().cookingScore(matter);
        float textureScore = schema.targets().textureScore(matter);
        float techniqueScore = techniqueScore(schema, context);
        float presentationScore = presentationScore(schema, context);
        DishScoreWeights weights = schema.weights();
        float weightedTotal = weights.roles()
                + weights.composition()
                + weights.seasoning()
                + weights.cooking()
                + weights.texture()
                + weights.technique()
                + weights.presentation();
        float score = weightedTotal <= 0.0F
                ? 0.0F
                : (roleScore * weights.roles()
                + compositionScore * weights.composition()
                + seasoningScore * weights.seasoning()
                + cookingScore * weights.cooking()
                + textureScore * weights.texture()
                + techniqueScore * weights.technique()
                + presentationScore * weights.presentation()) / weightedTotal;
        score = Mth.clamp(score + optionalRoleBonus, 0.0F, 1.0F);
        return Optional.of(new DishSchemaScore(
                schema,
                () -> item,
                score,
                roleScore,
                compositionScore,
                seasoningScore,
                cookingScore,
                textureScore,
                techniqueScore,
                presentationScore
        ));
    }

    private static float roleScore(List<DishRoleRequirement> roles, FoodMatterData matter) {
        if (roles.isEmpty()) {
            return 0.68F;
        }
        float total = 0.0F;
        float weights = 0.0F;
        for (DishRoleRequirement role : roles) {
            total += role.score(matter) * role.weight();
            weights += role.weight();
        }
        return weights > 0.0F ? Mth.clamp(total / weights, 0.0F, 1.0F) : 0.0F;
    }

    private static float optionalRoleBonus(List<DishRoleRequirement> roles, FoodMatterData matter) {
        if (roles.isEmpty()) {
            return 0.0F;
        }
        float matched = 0.0F;
        for (DishRoleRequirement role : roles) {
            matched += role.score(matter);
        }
        return Mth.clamp((matched / roles.size()) * 0.14F, 0.0F, 0.14F);
    }

    private static float techniqueScore(DishSchemaDefinition schema, DishAttemptContext context) {
        if (schema.requiredTechniques().isEmpty()) {
            return 0.62F;
        }
        float total = 0.0F;
        for (DishTechnique technique : schema.requiredTechniques()) {
            total += technique.score(context.matter(), context.state());
        }
        return Mth.clamp(total / schema.requiredTechniques().size(), 0.0F, 1.0F);
    }

    private static float presentationScore(DishSchemaDefinition schema, DishAttemptContext context) {
        if (schema.meal()) {
            return context.matter().finalizedServing() || context.state().isPlatedState() ? 1.0F : 0.35F;
        }
        return context.matter().finalizedServing() ? 0.40F : 0.82F;
    }

    private static Item itemFor(DishSchemaDefinition schema) {
        return BuiltInRegistries.ITEM.get(schema.result());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(JazzyCookin.MODID, path);
    }

    private static List<DishSchemaDefinition> builtInSchemas() {
        List<DishSchemaDefinition> schemas = new ArrayList<>();
        schemas.add(egg("soft_scrambled_eggs", 1001, "soft_scrambled_eggs", 0.90F,
                FloatRange.of(0.45F, 0.75F, 0.18F), FloatRange.of(0.45F, 0.66F, 0.16F),
                FloatRange.of(0.0F, 0.18F, 0.10F), FloatRange.of(0.55F, 0.92F, 0.20F), FloatRange.of(0.10F, 0.60F, 0.22F)));
        schemas.add(egg("scrambled_eggs", 1002, "scrambled_eggs", 0.93F,
                FloatRange.of(0.25F, 0.56F, 0.18F), FloatRange.of(0.60F, 0.84F, 0.18F),
                FloatRange.of(0.0F, 0.30F, 0.12F), FloatRange.of(0.45F, 0.88F, 0.18F), FloatRange.of(0.12F, 0.56F, 0.20F)));
        schemas.add(egg("omelet", 1003, "omelet", 0.96F,
                FloatRange.of(0.30F, 0.60F, 0.18F), FloatRange.of(0.58F, 0.82F, 0.16F),
                FloatRange.of(0.02F, 0.24F, 0.10F), FloatRange.of(0.02F, 0.35F, 0.14F), FloatRange.of(0.45F, 0.95F, 0.20F)));
        schemas.add(egg("burnt_eggs", 1004, "burnt_eggs", 0.18F,
                FloatRange.of(0.02F, 0.44F, 0.20F), FloatRange.of(0.35F, 1.0F, 0.20F),
                FloatRange.of(0.42F, 1.0F, 0.12F), FloatRange.of(0.40F, 0.95F, 0.20F), FloatRange.of(0.08F, 0.52F, 0.20F)));

        schemas.add(new DishSchemaDefinition(
                "pan_seared_chicken_prep",
                1010,
                id("pan_seared_chicken_prep"),
                DishCategory.PAN_DISH,
                false,
                0.56F,
                0.62F,
                0.88F,
                List.of(
                        DishRoleRequirement.all(DishRole.PROTEIN, 1.0F, FoodTrait.CHICKEN, FoodTrait.PROTEIN),
                        DishRoleRequirement.any(DishRole.FAT, 0.45F, FoodTrait.FAT, FoodTrait.OIL)
                ),
                List.of(
                        DishRoleRequirement.any(DishRole.SALT, 0.25F, FoodTrait.SALT),
                        DishRoleRequirement.any(DishRole.HERB, 0.20F, FoodTrait.HERB),
                        DishRoleRequirement.any(DishRole.SPICE, 0.20F, FoodTrait.SPICE, FoodTrait.PEPPER)
                ),
                List.of(),
                List.of(DishTechnique.PAN_FRIED),
                new DishMatterTargets(
                        Optional.of(FloatRange.of(0.18F, 0.66F, 0.18F)),
                        Optional.of(FloatRange.of(0.08F, 0.62F, 0.18F)),
                        Optional.of(FloatRange.of(0.24F, 0.92F, 0.18F)),
                        Optional.of(FloatRange.of(0.06F, 0.34F, 0.16F)),
                        Optional.empty(), Optional.empty(),
                        Optional.of(FloatRange.of(0.0F, 0.28F, 0.16F)),
                        Optional.of(FloatRange.of(0.0F, 0.26F, 0.14F)),
                        Optional.of(FloatRange.of(0.42F, 0.92F, 0.20F)),
                        Optional.of(FloatRange.of(0.06F, 0.42F, 0.14F)),
                        Optional.of(FloatRange.of(0.0F, 0.16F, 0.10F)),
                        Optional.empty(),
                        Optional.of(FloatRange.of(0.02F, 0.34F, 0.14F)),
                        Optional.of(FloatRange.of(0.24F, 0.78F, 0.18F))
                ),
                DishScoreWeights.DEFAULT
        ));
        schemas.add(platedVariant("pan_seared_chicken", 1011, "pan_seared_chicken", DishCategory.PAN_DISH, FoodTrait.CHICKEN, FoodTrait.PROTEIN));
        schemas.add(simmered("chicken_curry_prep", 1020, "chicken_curry_prep", false, 0.90F,
                List.of(DishRoleRequirement.all(DishRole.PROTEIN, 0.85F, FoodTrait.CHICKEN, FoodTrait.PROTEIN)),
                List.of(DishRoleRequirement.any(DishRole.SPICE, 0.30F, FoodTrait.SPICE), DishRoleRequirement.any(DishRole.AROMATIC, 0.25F, FoodTrait.ALLIUM, FoodTrait.AROMATIC))));
        schemas.add(platedVariant("chicken_curry", 1021, "chicken_curry", DishCategory.SOUP, FoodTrait.CHICKEN, FoodTrait.PROTEIN));
        schemas.add(simmered("lentil_soup_prep", 1030, "lentil_soup_prep", false, 0.86F,
                List.of(DishRoleRequirement.any(DishRole.PROTEIN, 0.75F, FoodTrait.LEGUME, FoodTrait.PLANT_PROTEIN)),
                List.of(DishRoleRequirement.any(DishRole.LIQUID, 0.30F, FoodTrait.SAUCE), DishRoleRequirement.any(DishRole.AROMATIC, 0.25F, FoodTrait.ALLIUM, FoodTrait.AROMATIC))));
        schemas.add(platedVariant("lentil_soup", 1031, "lentil_soup", DishCategory.SOUP, FoodTrait.LEGUME));
        schemas.add(simmered("tomato_soup_base", 1040, "tomato_soup_base", false, 0.82F,
                List.of(DishRoleRequirement.any(DishRole.VEGETABLE, 1.0F, FoodTrait.TOMATO, FoodTrait.VEGETABLE)),
                List.of(DishRoleRequirement.any(DishRole.DAIRY, 0.20F, FoodTrait.DAIRY), DishRoleRequirement.any(DishRole.AROMATIC, 0.25F, FoodTrait.ALLIUM, FoodTrait.AROMATIC))));
        schemas.add(platedVariant("creamy_tomato_soup", 1041, "creamy_tomato_soup", DishCategory.SOUP, FoodTrait.TOMATO));
        schemas.add(baked("pie_dough", 1050, "pie_dough", false, DishTechnique.MIXED, FoodTrait.FLOUR));
        schemas.add(baked("garlic_bread", 1051, "garlic_bread", true, DishTechnique.BAKED, FoodTrait.BREAD));
        schemas.add(platedVariant("chicken_fried_rice", 1060, "chicken_fried_rice", DishCategory.PAN_DISH, FoodTrait.RICE, FoodTrait.CHICKEN));
        schemas.add(platedVariant("tofu_stir_fry", 1061, "tofu_stir_fry", DishCategory.PAN_DISH, FoodTrait.SOY, FoodTrait.VEGETABLE));
        return List.copyOf(schemas);
    }

    private static DishSchemaDefinition egg(
            String key,
            int previewId,
            String result,
            float desirability,
            FloatRange water,
            FloatRange proteinSet,
            FloatRange browning,
            FloatRange fragmentation,
            FloatRange cohesiveness
    ) {
        return new DishSchemaDefinition(
                key,
                previewId,
                id(result),
                DishCategory.EGG,
                true,
                0.58F,
                0.62F,
                desirability,
                List.of(DishRoleRequirement.any(DishRole.PROTEIN, 1.0F, FoodTrait.EGG)),
                List.of(
                        DishRoleRequirement.any(DishRole.DAIRY, 0.22F, FoodTrait.DAIRY),
                        DishRoleRequirement.any(DishRole.SALT, 0.22F, FoodTrait.SALT),
                        DishRoleRequirement.any(DishRole.AROMATIC, 0.20F, FoodTrait.ALLIUM),
                        DishRoleRequirement.any(DishRole.HERB, 0.18F, FoodTrait.HERB),
                        DishRoleRequirement.any(DishRole.SPICE, 0.18F, FoodTrait.PEPPER, FoodTrait.SPICE)
                ),
                List.of(),
                List.of(DishTechnique.MIXED, DishTechnique.PAN_FRIED),
                new DishMatterTargets(
                        Optional.of(water),
                        Optional.empty(),
                        Optional.of(FloatRange.of(0.18F, 0.92F, 0.18F)),
                        Optional.of(FloatRange.of(0.04F, 0.32F, 0.14F)),
                        Optional.of(FloatRange.of(0.0F, 0.24F, 0.12F)),
                        Optional.of(FloatRange.of(0.0F, 0.28F, 0.14F)),
                        Optional.of(FloatRange.of(0.0F, 0.24F, 0.12F)),
                        Optional.of(FloatRange.of(0.0F, 0.24F, 0.12F)),
                        Optional.of(proteinSet),
                        Optional.of(browning),
                        Optional.of(FloatRange.of(0.0F, 0.12F, 0.08F)),
                        Optional.of(FloatRange.of(0.04F, 0.36F, 0.16F)),
                        Optional.of(fragmentation),
                        Optional.of(cohesiveness)
                ),
                DishScoreWeights.DEFAULT
        );
    }

    private static DishSchemaDefinition simmered(
            String key,
            int previewId,
            String result,
            boolean meal,
            float desirability,
            List<DishRoleRequirement> required,
            List<DishRoleRequirement> optional
    ) {
        List<DishRoleRequirement> requiredRoles = new ArrayList<>(required);
        requiredRoles.add(DishRoleRequirement.any(DishRole.LIQUID, 0.65F, FoodTrait.SAUCE, FoodTrait.TOMATO, FoodTrait.VEGETABLE));
        return new DishSchemaDefinition(
                key,
                previewId,
                id(result),
                DishCategory.SOUP,
                meal,
                0.56F,
                0.62F,
                desirability,
                List.copyOf(requiredRoles),
                optional,
                List.of(),
                List.of(DishTechnique.SIMMERED),
                new DishMatterTargets(
                        Optional.of(FloatRange.of(0.44F, 0.88F, 0.18F)),
                        Optional.of(FloatRange.of(0.02F, 0.38F, 0.18F)),
                        Optional.of(FloatRange.of(0.06F, 0.72F, 0.20F)),
                        Optional.of(FloatRange.of(0.08F, 0.36F, 0.16F)),
                        Optional.empty(),
                        Optional.of(FloatRange.of(0.04F, 0.36F, 0.16F)),
                        Optional.of(FloatRange.of(0.0F, 0.28F, 0.16F)),
                        Optional.of(FloatRange.of(0.0F, 0.28F, 0.16F)),
                        Optional.of(FloatRange.of(0.34F, 0.82F, 0.20F)),
                        Optional.of(FloatRange.of(0.0F, 0.24F, 0.16F)),
                        Optional.of(FloatRange.of(0.0F, 0.10F, 0.08F)),
                        Optional.empty(),
                        Optional.of(FloatRange.of(0.14F, 0.46F, 0.16F)),
                        Optional.of(FloatRange.of(0.16F, 0.58F, 0.18F))
                ),
                DishScoreWeights.DEFAULT
        );
    }

    private static DishSchemaDefinition baked(String key, int previewId, String result, boolean meal, DishTechnique technique, FoodTrait... traits) {
        return new DishSchemaDefinition(
                key,
                previewId,
                id(result),
                DishCategory.BAKED,
                meal,
                0.54F,
                0.60F,
                0.82F,
                List.of(DishRoleRequirement.any(DishRole.GRAIN, 1.0F, traits)),
                List.of(DishRoleRequirement.any(DishRole.FAT, 0.25F, FoodTrait.FAT, FoodTrait.OIL), DishRoleRequirement.any(DishRole.SALT, 0.20F, FoodTrait.SALT)),
                List.of(),
                List.of(technique),
                new DishMatterTargets(
                        Optional.of(FloatRange.of(0.12F, 0.48F, 0.18F)),
                        Optional.of(FloatRange.of(0.04F, 0.46F, 0.20F)),
                        Optional.empty(),
                        Optional.of(FloatRange.of(0.02F, 0.22F, 0.14F)),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.of(FloatRange.of(0.0F, 0.36F, 0.16F)),
                        Optional.of(FloatRange.of(0.02F, 0.34F, 0.16F)),
                        Optional.of(FloatRange.of(0.0F, 0.12F, 0.08F)),
                        Optional.of(FloatRange.of(0.02F, 0.30F, 0.14F)),
                        Optional.of(FloatRange.of(0.02F, 0.28F, 0.14F)),
                        Optional.of(FloatRange.of(0.42F, 0.88F, 0.18F))
                ),
                DishScoreWeights.DEFAULT
        );
    }

    private static DishSchemaDefinition platedVariant(String key, int previewId, String result, DishCategory category, FoodTrait... traits) {
        return new DishSchemaDefinition(
                key,
                previewId,
                id(result),
                category,
                true,
                0.54F,
                0.60F,
                0.84F,
                List.of(DishRoleRequirement.any(DishRole.PROTEIN, 0.70F, traits)),
                List.of(DishRoleRequirement.any(DishRole.GARNISH, 0.25F, FoodTrait.HERB, FoodTrait.SALT, FoodTrait.SPICE)),
                List.of(),
                List.of(DishTechnique.PLATED),
                DishMatterTargets.EMPTY,
                new DishScoreWeights(0.26F, 0.10F, 0.14F, 0.18F, 0.12F, 0.08F, 0.12F)
        );
    }
}
