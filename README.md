# Jazzy Cookin

`Jazzy Cookin` is a NeoForge `1.21.1` cooking mod built around state-based kitchen processing instead of instant crafting. Ingredients move through prep, combining, cooking, finishing, and plating. Recipes check the exact item, the exact state, the station, the tool, and in many cases the heat or environment.

## Current Status

- This repo is a playable vertical slice of the full kitchen system.
- The custom kitchen workflow is implemented.
- There are currently no vanilla crafting table, furnace, smithing, or stonecutter recipes in `data/jazzycookin/recipes`.
- For now, place blocks and obtain tools from the Jazzy Cookin creative tabs or with `/give`.
- Most processed ingredients are meant to be made through stations, not crafted in a vanilla grid.

## Creative Tabs

- `Sources`: orchard, garden, animal, and forage blocks.
- `Kitchen`: pantry, cellar, and all processing stations.
- `Tools`: knives, whisk, rolling pin, pots, strainers, jar, pie tin, and other equipment.
- `Ingredients`: raw ingredients plus intermediate products for testing.
- `Meals`: ceramic plate and finished plated meals.

## Quick Start

1. Open the `Sources`, `Kitchen`, and `Tools` creative tabs.
2. Place a few source blocks such as `Apple Sapling`, `Tomato Vine`, `Herb Bed`, `Wheat Patch`, `Cabbage Patch`, and `Onion Patch`.
3. Place the stations you need, usually `Prep Table`, `Mixing Bowl`, `Spice Grinder`, `Stove`, `Oven`, `Cooling Rack`, `Resting Board`, and `Plating Station`.
4. Place a `Pantry` if you want quick access to flour, sugar, butter, spice, oil, plates, jars, and salt.
5. Harvest ingredients, process them through stations, and plate the final food.

## How Crafting Works In This Mod

This mod uses station recipes, not vanilla crafting recipes.

Each kitchen station works the same basic way:

1. Put recipe inputs into the first four slots.
2. Put the required tool in the tool slot if the recipe needs one.
3. Set heat if the station supports heat.
4. Set the station control if the station supports controls.
5. Make sure any environment requirement is satisfied.
6. Press `Start`.
7. Take the main result from the output slot and any extra item from the byproduct slot.

Important rules:

- Input order matters. Recipes match slot `1` through slot `4`, not an unordered pool.
- Extra filled input slots can prevent recipe matching.
- State matters. `whole_apple`, `peeled_apple`, and `sliced_apple` are different recipe ingredients.
- Heat matters. Wrong heat can block the recipe or reduce quality.
- Some recipes are passive and take longer than active prep recipes.
- Some recipes require nearby water.
- Oven recipes can require preheat.

## Ingredient Sources

All source blocks are harvested by right-clicking them when ripe.

- `Apple Sapling`: grows orchard apples. Best quality comes from harvesting when ripe, with decent light and nearby water.
- `Tomato Vine`: grows tomatoes.
- `Herb Bed`: grows fresh herbs.
- `Wheat Patch`: grows wheat sheaves.
- `Cabbage Patch`: grows cabbage.
- `Onion Patch`: grows onions.
- `Chicken Coop`: gives `Farm Egg` most of the time and occasionally `Raw Protein`.
- `Dairy Stall`: gives `Fresh Milk`.
- `Fishing Trap`: gives `Raw Fish`.
- `Forage Shrub`: gives `Wild Berries`.

Source notes:

- Plant-like sources grow with random ticks.
- Bonemeal works on growable source blocks until they are ripe.
- Some crops prefer nearby water and lose quality if grown dry.
- Harvesting a ripe plant resets it instead of destroying it.

## Storage

- `Pantry`: normal storage for shelf-stable food and kitchen supplies. The pantry UI also has shortcut buttons that give:
  `Flour`, `Cane Sugar`, `Butter`, `Baking Spice`, `Frying Oil`, `Ceramic Plate`, `Canning Jar`, and `Salt`.
- `Cellar`: slows freshness loss much more than the pantry.

Freshness still matters even in storage. Items move through:

- `Fresh`
- `Aging`
- `Stale`
- `Spoiled`
- `Moldy`

Spoiled or moldy ingredients can block recipes because their effective state changes.

## Tools And What They Do

- `Paring Knife`: best for apples and other small prep work.
- `Chef Knife`: general chopping and slicing.
- `Cleaver`: trimming larger proteins.
- `Whisk`: mixing, egg wash, batter, brine, syrup, dough starts.
- `Rolling Pin`: kneading dough and shaping some prep-table combinations.
- `Mortar Pestle`: grinding herbs and baking spice.
- `Stock Pot`: boiling, simmering, deep frying.
- `Frying Skillet`: pan frying.
- `Fine Strainer`: soup straining.
- `Coarse Strainer`: alternative strainer profile for rougher work.
- `Steamer Basket`: required for steaming dumplings.
- `Canning Jar`: used for canning, marinating, and fermentation recipes.
- `Pie Tin`: required for assembling pie.

Tool notes:

- The correct tool can be required for a recipe to match at all.
- Allowed-but-not-preferred tools are slower than the preferred tool.
- Tools take durability damage when a recipe completes successfully.

## Stations

### Pantry

- Stores items.
- Gives quick pantry staples through UI buttons.

### Cellar

- Stores items with stronger freshness preservation than the pantry.

### Prep Table

- Used for peeling, slicing, chopping, dicing, trimming, shaping, and assembly.
- Control buttons change the cut style:
  `Rough Cut`, `Precise Cut`, `Fine Cut`.
- Wrong control settings can create alternate states such as rough cuts that fail later recipes.

### Spice Grinder

- Used for grinding herbs and baking spice.
- Control buttons change grind intensity:
  `Coarse Grind`, `Balanced Grind`, `Fine Grind`.

### Strainer

- Used to turn soup base into strained soup.
- Best results use the correct strainer tool.

### Mixing Bowl

- Used for mixing, whisking, kneading, battering, brine, syrup, and sauce bases.
- Control buttons change work intensity:
  `Light Work`, `Balanced Work`, `Heavy Work`.
- Some recipes use outcome bands here, so underworking or overworking can create the wrong state.

### Canning Station

- Used for heat-processing preserves.
- Requires a `Canning Jar`.
- Tomatoes need `Brine`.
- Apples need `Canning Syrup`.
- Canning creates a `Hot Preserve` first. You must cool it on the `Cooling Rack` to finalize it.

### Drying Rack

- Passive station for drying sliced apples into dried fruit.

### Smoker

- Passive low-heat station for smoked protein.

### Fermentation Crock

- Passive station for marinating proteins and culturing or fermenting ingredients.
- Current recipes use a `Canning Jar` as the required container tool.

### Steamer

- Passive station for dumplings.
- Requires nearby water.
- Requires a `Steamer Basket`.

### Stove

- Used for boiling, simmering, pan frying, and deep frying.
- Some recipes require nearby water.
- Deep frying returns degraded oil in the byproduct slot.

### Oven

- Used for baking, roasting, and broiling.
- The oven must be preheated for recipes that require it.
- Preheat is built by leaving heat on before starting.

### Cooling Rack

- Used to cool hot preserves and baked pie.
- Cooling is a separate required finishing step.

### Resting Board

- Used for bread, pie, fried protein, and broiled protein resting.
- Some plating recipes refuse un-rested food.

### Plating Station

- Combines finished components into the final plated meals.
- Most meal recipes also require a `Ceramic Plate`.

## Implemented Recipe Chains

These are the major chains currently implemented in code and game tests.

### Apple Pie Slice

1. `Apple Sapling` -> harvest `Orchard Apple`
2. `Prep Table` + `Paring Knife` -> peel apple
3. `Prep Table` + `Paring Knife` -> slice apple
4. `Spice Grinder` + `Mortar Pestle` -> grind `Baking Spice`
5. `Stove` + `Stock Pot` + medium heat + nearby water -> simmer sliced apple + sugar + ground spice into `Pie Filling`
6. `Mixing Bowl` + `Whisk` -> mix flour + butter + sugar into crust mix
7. `Mixing Bowl` + `Rolling Pin` -> knead crust mix into developed dough
8. `Mixing Bowl` + `Whisk` -> whisk egg into `Egg Wash`
9. `Prep Table` + `Pie Tin` -> combine dough + filling + egg wash into raw pie
10. `Oven` + `Pie Tin` + preheat + high heat -> bake pie
11. `Cooling Rack` -> cool pie
12. `Resting Board` -> rest pie
13. `Prep Table` + `Chef Knife` -> slice pie
14. `Plating Station` + `Ceramic Plate` -> `Plated Apple Pie Slice`

### Tomato Soup And Bread Meal

1. `Tomato Vine` -> tomato
2. `Onion Patch` -> onion
3. `Herb Bed` -> herb
4. `Prep Table` + `Chef Knife` -> chopped tomato
5. `Prep Table` + `Chef Knife` -> diced onion
6. `Spice Grinder` + `Mortar Pestle` -> ground herb
7. `Stove` + `Stock Pot` + low heat + nearby water -> soup base
8. `Strainer` + `Fine Strainer` -> strained soup
9. `Pantry` -> flour
10. `Dairy Stall` -> fresh milk
11. `Mixing Bowl` + `Whisk` -> bread mix
12. `Mixing Bowl` + `Rolling Pin` -> kneaded bread dough
13. `Oven` + preheat + medium heat -> bread loaf
14. `Resting Board` -> rested bread
15. `Prep Table` + `Chef Knife` -> sliced bread
16. `Plating Station` + `Ceramic Plate` -> `Plated Tomato Soup Meal`

### Dumpling Meal

1. `Cabbage Patch` -> cabbage
2. `Onion Patch` -> onion
3. `Herb Bed` -> herb
4. `Prep Table` + `Chef Knife` -> chopped cabbage
5. `Prep Table` + `Chef Knife` -> diced onion
6. `Spice Grinder` + `Mortar Pestle` -> ground herb
7. `Mixing Bowl` -> dumpling filling
8. `Pantry` -> flour and salt
9. `Chicken Coop` -> egg
10. `Mixing Bowl` + `Whisk` -> dough mix
11. `Mixing Bowl` + `Rolling Pin` -> dumpling dough
12. `Prep Table` + `Rolling Pin` -> raw dumplings
13. `Steamer` + `Steamer Basket` + nearby water -> steamed dumplings
14. `Plating Station` + `Ceramic Plate` -> `Plated Dumpling Meal`

### Fried Meal

1. `Chicken Coop` -> raw protein
2. `Herb Bed` -> herb
3. `Baking Spice` from pantry -> grind into spice
4. `Mixing Bowl` + `Whisk` -> marinade
5. `Fermentation Crock` + `Canning Jar` -> marinated protein
6. `Mixing Bowl` + `Whisk` -> batter
7. `Prep Table` -> coat protein
8. `Stove` + `Stock Pot` + high heat -> fried protein, with oil returning in byproduct slot
9. `Oven` + preheat + high heat -> roast vegetables
10. `Resting Board` -> rest fried protein
11. `Plating Station` + `Ceramic Plate` -> `Plated Fried Meal`

### Roast Meal

1. `Chicken Coop` -> raw protein
2. `Prep Table` + `Cleaver` -> roast cut
3. `Oven` + preheat + medium heat -> roasted protein
4. `Oven` + preheat + high heat -> broiled protein finish
5. `Oven` + preheat + high heat -> roast vegetables
6. `Resting Board` -> rest broiled protein
7. `Plating Station` + `Ceramic Plate` -> `Plated Roast Meal`

## Other Implemented Processing Recipes

- `Farm Egg` -> `Boiled Egg`
- `Raw Fish` -> `Cleaned Fish` -> `Pan Fried Fish`
- `Salt` + nearby water -> `Brine`
- `Cane Sugar` + nearby water -> `Canning Syrup`
- `Chopped Tomato` + `Brine` + `Canning Jar` -> hot canned tomato -> cool to final canned tomato
- `Sliced Apple` + `Canning Syrup` + `Canning Jar` -> hot apple preserve -> cool to final preserve
- `Sliced Apple` -> `Dried Apple`
- `Roast Cut` -> `Smoked Protein`
- `Fresh Milk` -> `Cultured Dairy`
- `Chopped Cabbage` + `Diced Onion` -> `Fermented Vegetables`

## Important Gameplay Rules

- Recipes require the correct state, not just the correct item.
- Underworked or overworked outputs can block later recipes.
- Plating recipes can require finished states such as rested protein or sliced bread.
- Deep frying degrades oil from `Fresh Oil` -> `Used Oil` -> `Dirty Oil` -> `Burnt Oil`.
- Passive stations do not finish instantly and should not be interrupted.
- Quality, freshness, prep, combine, cooking, finishing, and plating all affect the final result.

## Tooltips And Food Rewards

Ingredient and meal tooltips show:

- current state
- grade
- recipe accuracy
- freshness
- prep score
- combine score
- cooking score
- finishing score
- plating score
- nourishment
- enjoyment

When you eat a plated meal:

- it restores food based on the meal item
- it shows serve and mastery messages
- it usually returns a `Ceramic Plate`

## Developer Notes

- Custom kitchen process recipes live in `src/main/resources/data/jazzycookin/recipe`.
- The main game tests for recipe chains and kitchen rules live in `src/main/java/com/boaat/jazzy_cookin/gametest/KitchenGameTests.java`.
- Run the dev client with:

```powershell
.\gradlew.bat runClient
```

- Run the automated game tests with:

```powershell
.\gradlew.bat runGameTestServer --console=plain
```
