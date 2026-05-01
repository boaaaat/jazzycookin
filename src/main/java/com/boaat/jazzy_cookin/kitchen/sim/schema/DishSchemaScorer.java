package com.boaat.jazzy_cookin.kitchen.sim.schema;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishRecognitionResult;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class DishSchemaScorer {
    private DishSchemaScorer() {
    }

    public static List<DishSchemaDefinition> schemas() {
        return DishSchemaManager.schemas();
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
}
