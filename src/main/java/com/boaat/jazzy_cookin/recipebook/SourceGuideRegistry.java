package com.boaat.jazzy_cookin.recipebook;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.KitchenSourceProfile;
import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class SourceGuideRegistry {
    public record HarvestOutput(JazzyItems.IngredientId ingredientId, boolean randomResult) {
        public ResourceLocation itemId() {
            return BuiltInRegistries.ITEM.getKey(JazzyItems.ingredient(this.ingredientId).get());
        }

        public IngredientState state() {
            return JazzyItems.ingredient(this.ingredientId).get().defaultState();
        }

        public ItemStack displayStack() {
            return JazzyItems.ingredient(this.ingredientId).get().createStack(1, 0L);
        }
    }

    public record SourceGuide(
            String key,
            Supplier<? extends Item> sourceItem,
            List<HarvestOutput> outputs,
            boolean randomResult,
            String summary,
            List<String> notes
    ) {
        public boolean matches(ResourceLocation itemId, IngredientState state) {
            return this.outputs.stream().anyMatch(output -> output.itemId().equals(itemId) && output.state() == state);
        }
    }

    private static final SourceGuide APPLE_SAPLING = new SourceGuide(
            "apple_sapling",
            JazzyItems.APPLE_SAPLING_ITEM,
            List.of(new HarvestOutput(JazzyItems.IngredientId.APPLES, false)),
            false,
            "Grow an apple sapling and harvest ripe apples.",
            List.of(
                    "Bright light helps the sapling ripen.",
                    "Nearby water slightly improves harvest quality.",
                    "Harvest once the tree reaches age 5 or higher."
            )
    );

    private static final Map<KitchenSourceProfile, SourceGuide> GUIDES = Map.of(
            KitchenSourceProfile.TOMATO_VINE,
            new SourceGuide(
                    "tomato_vine",
                    JazzyItems.TOMATO_VINE_ITEM,
                    List.of(new HarvestOutput(JazzyItems.IngredientId.TOMATOES, false)),
                    false,
                    "Grow tomatoes on a ripe vine.",
                    List.of(
                            "Tomato vines need bright light to grow.",
                            "Harvest when the vine reaches its ripe stage."
                    )
            ),
            KitchenSourceProfile.HERB_BED,
            new SourceGuide(
                    "herb_bed",
                    JazzyItems.HERB_BED_ITEM,
                    List.of(
                            new HarvestOutput(JazzyItems.IngredientId.BASIL, true),
                            new HarvestOutput(JazzyItems.IngredientId.PARSLEY, true),
                            new HarvestOutput(JazzyItems.IngredientId.DILL, true),
                            new HarvestOutput(JazzyItems.IngredientId.OREGANO, true)
                    ),
                    true,
                    "Harvest herbs from a mature herb bed.",
                    List.of(
                            "Herb beds need bright light to regrow.",
                            "Each harvest yields one herb at random."
                    )
            ),
            KitchenSourceProfile.WHEAT_PATCH,
            new SourceGuide(
                    "wheat_patch",
                    JazzyItems.WHEAT_PATCH_ITEM,
                    List.of(new HarvestOutput(JazzyItems.IngredientId.WHOLE_WHEAT_FLOUR, false)),
                    false,
                    "Harvest flour from a ripe wheat patch.",
                    List.of("Wheat patches need bright light to ripen.")
            ),
            KitchenSourceProfile.CABBAGE_PATCH,
            new SourceGuide(
                    "cabbage_patch",
                    JazzyItems.CABBAGE_PATCH_ITEM,
                    List.of(new HarvestOutput(JazzyItems.IngredientId.CABBAGE, false)),
                    false,
                    "Harvest cabbage from a mature patch.",
                    List.of(
                            "Cabbage patches prefer nearby water.",
                            "Harvest when the patch reaches its ripe stage."
                    )
            ),
            KitchenSourceProfile.ONION_PATCH,
            new SourceGuide(
                    "onion_patch",
                    JazzyItems.ONION_PATCH_ITEM,
                    List.of(new HarvestOutput(JazzyItems.IngredientId.ONIONS, false)),
                    false,
                    "Harvest onions from a mature patch.",
                    List.of("Onion patches need bright light to ripen.")
            ),
            KitchenSourceProfile.CHICKEN_COOP,
            new SourceGuide(
                    "chicken_coop",
                    JazzyItems.CHICKEN_COOP_ITEM,
                    List.of(
                            new HarvestOutput(JazzyItems.IngredientId.EGGS, true),
                            new HarvestOutput(JazzyItems.IngredientId.CHICKEN, true)
                    ),
                    true,
                    "Collect eggs or chicken from a stocked coop.",
                    List.of("Each collection can yield eggs or chicken.")
            ),
            KitchenSourceProfile.DAIRY_STALL,
            new SourceGuide(
                    "dairy_stall",
                    JazzyItems.DAIRY_STALL_ITEM,
                    List.of(
                            new HarvestOutput(JazzyItems.IngredientId.BUTTER, true),
                            new HarvestOutput(JazzyItems.IngredientId.SHELF_STABLE_CREAM, true)
                    ),
                    true,
                    "Collect dairy from a stocked stall.",
                    List.of("Each collection can yield butter or shelf-stable cream.")
            ),
            KitchenSourceProfile.FISHING_TRAP,
            new SourceGuide(
                    "fishing_trap",
                    JazzyItems.FISHING_TRAP_ITEM,
                    List.of(new HarvestOutput(JazzyItems.IngredientId.FISH_FILLET, false)),
                    false,
                    "Collect fish fillets from a fishing trap.",
                    List.of("Check the trap after it matures.")
            ),
            KitchenSourceProfile.FORAGE_SHRUB,
            new SourceGuide(
                    "forage_shrub",
                    JazzyItems.FORAGE_SHRUB_ITEM,
                    List.of(
                            new HarvestOutput(JazzyItems.IngredientId.MINT, true),
                            new HarvestOutput(JazzyItems.IngredientId.ROSEMARY, true)
                    ),
                    true,
                    "Forage herbs from a mature shrub.",
                    List.of(
                            "Forage shrubs need bright light to regrow.",
                            "Each forage can yield mint or rosemary."
                    )
            )
    );

    private SourceGuideRegistry() {
    }

    public static SourceGuide appleSaplingGuide() {
        return APPLE_SAPLING;
    }

    public static SourceGuide guideFor(KitchenSourceProfile profile) {
        return GUIDES.get(profile);
    }

    public static List<HarvestOutput> outputsFor(KitchenSourceProfile profile) {
        return guideFor(profile).outputs();
    }

    public static Optional<SourceGuide> guideForOutput(ResourceLocation itemId, IngredientState state) {
        if (APPLE_SAPLING.matches(itemId, state)) {
            return Optional.of(APPLE_SAPLING);
        }
        return GUIDES.values().stream().filter(guide -> guide.matches(itemId, state)).findFirst();
    }

    public static Item selectHarvestItem(KitchenSourceProfile profile, RandomSource random) {
        List<HarvestOutput> outputs = outputsFor(profile);
        HarvestOutput output = outputs.get(outputs.size() == 1 ? 0 : random.nextInt(outputs.size()));
        return JazzyItems.ingredient(output.ingredientId()).get();
    }

    public static Item appleHarvestItem() {
        return JazzyItems.ingredient(JazzyItems.IngredientId.APPLES).get();
    }
}
