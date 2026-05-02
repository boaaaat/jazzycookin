package com.boaat.jazzy_cookin.kitchen.sim.schema;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishRecognitionResult;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
        return bestScore(context, filter);
    }

    public static DishSchemaScore bestScore(ItemStack stack, FoodMatterData matter, Predicate<Item> filter, long gameTime) {
        DishAttemptContext context = stack != null && !stack.isEmpty()
                ? DishAttemptContext.fromStack(stack, gameTime)
                : DishAttemptContext.fromMatter(matter);
        return bestScore(context, filter);
    }

    public static DishSchemaScore bestScore(DishAttemptContext context, Predicate<Item> filter) {
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
        IngredientEvaluation ingredientEvaluation = ingredientScore(schema, context);
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
        if (!schema.ingredients().isEmpty()) {
            score = score * 0.68F + ingredientEvaluation.score() * 0.32F;
        }
        score = Mth.clamp(score + optionalRoleBonus, 0.0F, 1.0F);
        float cap = scoreCap(schema, context, ingredientEvaluation);
        score = Math.min(score, cap);
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
                presentationScore,
                ingredientEvaluation.score(),
                cap
        ));
    }

    private static float scoreCap(DishSchemaDefinition schema, DishAttemptContext context, IngredientEvaluation ingredients) {
        float cap = 1.0F;
        if (ingredients.missingCore()) {
            cap = Math.min(cap, 0.55F);
        }
        if (ingredients.unmeasured()) {
            cap = Math.min(cap, 0.75F);
        }
        DishAttemptData attempt = context.attempt();
        if (attempt != null && attempt.wrongTechnique()) {
            cap = Math.min(cap, 0.65F);
        }
        if (!schema.steps().isEmpty() && context.stack() != null && !context.stack().isEmpty()) {
            boolean sameAttempt = attempt != null && schema.key().equals(attempt.schemaKey());
            boolean missingStep = !sameAttempt;
            if (sameAttempt) {
                for (DishStepRequirement step : schema.steps()) {
                    if (!attempt.hasStep(step.id())) {
                        missingStep = true;
                        break;
                    }
                    for (String prerequisite : step.prerequisites()) {
                        if (!attempt.hasStep(prerequisite)) {
                            missingStep = true;
                            break;
                        }
                    }
                    if (missingStep) {
                        break;
                    }
                }
            }
            if (missingStep) {
                cap = Math.min(cap, 0.60F);
            }
        }
        if (attempt != null && attempt.qualityPenalty() > 0.0F) {
            cap = Math.min(cap, 1.0F - attempt.qualityPenalty() * 0.35F);
        }
        return Mth.clamp(cap, 0.0F, 1.0F);
    }

    private static IngredientEvaluation ingredientScore(DishSchemaDefinition schema, DishAttemptContext context) {
        if (schema.ingredients().isEmpty()) {
            return IngredientEvaluation.EMPTY;
        }
        DishAttemptData attempt = context.attempt();
        if (attempt != null && schema.key().equals(attempt.schemaKey()) && attempt.ingredientScore() > 0.0F) {
            return new IngredientEvaluation(attempt.ingredientScore(), attempt.missingCoreIngredient(), attempt.unmeasuredIngredient());
        }

        float total = 0.0F;
        int count = 0;
        boolean missingCore = false;
        boolean unmeasured = false;
        for (DishIngredientRequirement ingredient : schema.ingredients()) {
            float score = ingredientFallbackScore(ingredient, context);
            total += score;
            count++;
            if (ingredient.core() && score < 0.50F) {
                missingCore = true;
            }
            if (ingredient.measuredRequired() && score > 0.0F && !hasMeasuredContext(context)) {
                unmeasured = true;
            }
        }
        return new IngredientEvaluation(count > 0 ? Mth.clamp(total / count, 0.0F, 1.0F) : 1.0F, missingCore, unmeasured);
    }

    private static boolean hasMeasuredContext(DishAttemptContext context) {
        return context.stack() != null && !context.stack().isEmpty() && KitchenStackUtil.isMeasured(context.stack());
    }

    private static float ingredientFallbackScore(DishIngredientRequirement ingredient, DishAttemptContext context) {
        FoodMatterData matter = context.matter();
        ItemStack stack = context.stack();
        boolean itemMatched = stack != null && !stack.isEmpty()
                && ingredient.item().map(item -> KitchenStackUtil.itemKey(stack).equals(item)).orElse(false);
        boolean traitMatched = matter != null
                && (ingredient.allTraits().isEmpty() || ingredient.allTraits().stream().allMatch(matter::hasTrait))
                && (ingredient.anyTraits().isEmpty() || ingredient.anyTraits().stream().anyMatch(matter::hasTrait));
        boolean matched = itemMatched || traitMatched;
        if (!matched) {
            return 0.0F;
        }
        if (!ingredient.hasMeasuredAmount()) {
            return 1.0F;
        }
        if (stack == null || stack.isEmpty()) {
            return 1.0F;
        }
        float amount = KitchenStackUtil.measuredAmount(stack, ingredient.unit());
        if (amount >= ingredient.minAmount() && amount <= ingredient.maxAmount()) {
            return 1.0F;
        }
        if (amount < ingredient.minAmount()) {
            return Mth.clamp(amount / Math.max(0.001F, ingredient.minAmount()), 0.0F, 1.0F);
        }
        return Mth.clamp(ingredient.maxAmount() / Math.max(0.001F, amount), 0.0F, 1.0F);
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

    private record IngredientEvaluation(float score, boolean missingCore, boolean unmeasured) {
        private static final IngredientEvaluation EMPTY = new IngredientEvaluation(1.0F, false, false);
    }
}
