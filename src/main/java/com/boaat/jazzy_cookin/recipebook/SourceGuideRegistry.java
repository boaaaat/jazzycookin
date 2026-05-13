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

    private static final Map<KitchenSourceProfile, SourceGuide> GUIDES = Map.ofEntries(
            Map.entry(KitchenSourceProfile.TOMATO_VINE,
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
            )),
            Map.entry(KitchenSourceProfile.HERB_BED,
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
            )),
            Map.entry(KitchenSourceProfile.WHEAT_PATCH,
            new SourceGuide(
                    "wheat_patch",
                    JazzyItems.WHEAT_PATCH_ITEM,
                    List.of(new HarvestOutput(JazzyItems.IngredientId.WHOLE_WHEAT_FLOUR, false)),
                    false,
                    "Harvest flour from a ripe wheat patch.",
                    List.of("Wheat patches need bright light to ripen.")
            )),
            Map.entry(KitchenSourceProfile.CABBAGE_PATCH,
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
            )),
            Map.entry(KitchenSourceProfile.ONION_PATCH,
            new SourceGuide(
                    "onion_patch",
                    JazzyItems.ONION_PATCH_ITEM,
                    List.of(new HarvestOutput(JazzyItems.IngredientId.ONIONS, false)),
                    false,
                    "Harvest onions from a mature patch.",
                    List.of("Onion patches need bright light to ripen.")
            )),
            Map.entry(KitchenSourceProfile.ROOT_VEGETABLE_PATCH,
            new SourceGuide(
                    "root_vegetable_patch",
                    JazzyItems.ROOT_VEGETABLE_PATCH_ITEM,
                    List.of(
                            new HarvestOutput(JazzyItems.IngredientId.CARROTS, true),
                            new HarvestOutput(JazzyItems.IngredientId.POTATOES, true),
                            new HarvestOutput(JazzyItems.IngredientId.GARLIC, true),
                            new HarvestOutput(JazzyItems.IngredientId.GINGER, true),
                            new HarvestOutput(JazzyItems.IngredientId.SHALLOTS, true)
                    ),
                    true,
                    "Harvest sturdy root vegetables from a hydrated patch.",
                    List.of(
                            "Root vegetable patches prefer nearby water.",
                            "Each harvest yields carrots, potatoes, garlic, ginger, or shallots."
                    )
            )),
            Map.entry(KitchenSourceProfile.LEAFY_GREENS_BED,
            new SourceGuide(
                    "leafy_greens_bed",
                    JazzyItems.LEAFY_GREENS_BED_ITEM,
                    List.of(new HarvestOutput(JazzyItems.IngredientId.SPINACH, false)),
                    false,
                    "Cut spinach from a leafy greens bed.",
                    List.of(
                            "Leafy greens grow in moderate light.",
                            "Nearby water slightly improves harvest quality."
                    )
            )),
            Map.entry(KitchenSourceProfile.PEPPER_BUSH,
            new SourceGuide(
                    "pepper_bush",
                    JazzyItems.PEPPER_BUSH_ITEM,
                    List.of(
                            new HarvestOutput(JazzyItems.IngredientId.JALAPENOS, true),
                            new HarvestOutput(JazzyItems.IngredientId.RED_PEPPER, true)
                    ),
                    true,
                    "Pick ripe peppers from a pepper bush.",
                    List.of(
                            "Pepper bushes need bright light.",
                            "Each harvest can yield jalapenos or red peppers."
                    )
            )),
            Map.entry(KitchenSourceProfile.PEA_TRELLIS,
            new SourceGuide(
                    "pea_trellis",
                    JazzyItems.PEA_TRELLIS_ITEM,
                    List.of(new HarvestOutput(JazzyItems.IngredientId.GREEN_PEAS, false)),
                    false,
                    "Pick green peas from a mature trellis.",
                    List.of(
                            "Pea trellises prefer nearby water.",
                            "Harvest when the vines fill out."
                    )
            )),
            Map.entry(KitchenSourceProfile.CITRUS_SAPLING,
            new SourceGuide(
                    "citrus_sapling",
                    JazzyItems.CITRUS_SAPLING_ITEM,
                    List.of(new HarvestOutput(JazzyItems.IngredientId.LEMONS, false)),
                    false,
                    "Grow a citrus sapling and harvest lemons.",
                    List.of(
                            "Citrus saplings need bright light.",
                            "Nearby water slightly improves harvest quality."
                    )
            )),
            Map.entry(KitchenSourceProfile.CHICKEN_COOP,
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
            )),
            Map.entry(KitchenSourceProfile.DAIRY_STALL,
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
            )),
            Map.entry(KitchenSourceProfile.FISHING_TRAP,
            new SourceGuide(
                    "fishing_trap",
                    JazzyItems.FISHING_TRAP_ITEM,
                    List.of(new HarvestOutput(JazzyItems.IngredientId.FISH_FILLET, false)),
                    false,
                    "Collect fish fillets from a fishing trap.",
                    List.of("Check the trap after it matures.")
            )),
            Map.entry(KitchenSourceProfile.FORAGE_SHRUB,
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
            ))
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
