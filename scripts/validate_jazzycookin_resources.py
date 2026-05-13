#!/usr/bin/env python3
"""Validate Jazzy Cookin resource/data pack consistency.

This intentionally avoids Gradle so resource edits can be checked even in
sandboxes where Java zip filesystem access prevents NeoForge compilation.
"""

from __future__ import annotations

import json
import re
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src/main/resources"
ASSETS = RESOURCES / "assets/jazzycookin"
DATA = RESOURCES / "data/jazzycookin"
JAVA_ITEMS = ROOT / "src/main/java/com/boaat/jazzy_cookin/registry/JazzyItems.java"
JAVA_BLOCKS = ROOT / "src/main/java/com/boaat/jazzy_cookin/registry/JazzyBlocks.java"
JAVA_SOURCE_BLOCK = ROOT / "src/main/java/com/boaat/jazzy_cookin/block/KitchenSourceBlock.java"
JAVA_SOURCE_PROFILES = ROOT / "src/main/java/com/boaat/jazzy_cookin/kitchen/KitchenSourceProfile.java"
JAVA_FOOD_PROFILES = ROOT / "src/main/java/com/boaat/jazzy_cookin/kitchen/sim/FoodMaterialProfiles.java"
JAVA_STATION_BLOCK = ROOT / "src/main/java/com/boaat/jazzy_cookin/block/KitchenStationBlock.java"
JAVA_STATION_ENTITY = ROOT / "src/main/java/com/boaat/jazzy_cookin/block/entity/KitchenStationBlockEntity.java"
JAVA_STORAGE_ENTITY = ROOT / "src/main/java/com/boaat/jazzy_cookin/block/entity/KitchenStorageBlockEntity.java"
JAVA_GAMETESTS = ROOT / "src/main/java/com/boaat/jazzy_cookin/gametest/KitchenGameTests.java"
JAVA_TOOL_PROFILE = ROOT / "src/main/java/com/boaat/jazzy_cookin/kitchen/ToolProfile.java"
JAVA_STATION_UI = ROOT / "src/main/java/com/boaat/jazzy_cookin/kitchen/StationUiProfile.java"
JAVA_STORAGE_UI = ROOT / "src/main/java/com/boaat/jazzy_cookin/kitchen/StorageUiProfile.java"
JAVA_STATION_SCREEN = ROOT / "src/main/java/com/boaat/jazzy_cookin/screen/KitchenStationScreen.java"
JAVA_CLIENT_ENTRYPOINT = ROOT / "src/main/java/com/boaat/jazzy_cookin/JazzyCookinClient.java"
JAVA_STATION_BER = ROOT / "src/main/java/com/boaat/jazzy_cookin/client/KitchenStationBlockEntityRenderer.java"
JAVA_SOURCE_GUIDE_REGISTRY = ROOT / "src/main/java/com/boaat/jazzy_cookin/recipebook/SourceGuideRegistry.java"
JAVA_RECIPE_BOOK_PLANNER = ROOT / "src/main/java/com/boaat/jazzy_cookin/recipebook/JazzyRecipeBookPlanner.java"
JAVA_RECIPE_BOOK_SCREEN = ROOT / "src/main/java/com/boaat/jazzy_cookin/recipebook/client/JazzyRecipeBookScreen.java"
JAVA_COMPOSITIONAL_SIM = ROOT / "src/main/java/com/boaat/jazzy_cookin/kitchen/sim/domain/CompositionalSimulationSupport.java"
JAVA_PAN_SIM = ROOT / "src/main/java/com/boaat/jazzy_cookin/kitchen/sim/domain/PanSchemaSimulationActions.java"
JAVA_POT_SIM = ROOT / "src/main/java/com/boaat/jazzy_cookin/kitchen/sim/domain/PotSimulationDomain.java"
JAVA_PREP_SIM = ROOT / "src/main/java/com/boaat/jazzy_cookin/kitchen/sim/domain/PrepSimulationDomain.java"
JAVA_PRESERVE_SIM = ROOT / "src/main/java/com/boaat/jazzy_cookin/kitchen/sim/domain/PreserveSimulationDomain.java"
PY_RECIPE_SIMULATOR = ROOT / "scripts/recipe_simulator.py"

ROLES = {
    "protein", "grain", "fat", "aromatic", "acid", "sweetener", "herb", "salt", "spice", "binder",
    "liquid", "vegetable", "fruit", "dairy", "container", "garnish",
}
TRAITS = {
    "sweetener", "syrup", "flour", "grain", "starch", "leavener", "salt", "spice", "herb", "fat", "oil",
    "dairy", "protein", "animal_protein", "plant_protein", "chicken", "fish", "pork", "beef", "egg", "soy",
    "fruit", "vegetable", "allium", "aromatic", "leafy_green", "legume", "nut", "acidic", "pepper", "tomato",
    "bread", "pasta", "condiment", "sauce", "preserve", "fermented", "chocolate", "caffeinated", "coconut",
    "rice", "wheat", "corn", "bread_loaf",
}
CATEGORIES = {"egg", "soup", "pan_dish", "baked", "sauce", "plated", "generic"}
TECHNIQUES = {"prepped", "cut", "mixed", "dip_or_coat", "simmered", "pan_fried", "baked", "rested", "plated"}
TARGETS = {
    "water", "fat", "protein", "seasoning", "cheese", "onion", "herb", "pepper", "protein_set", "browning",
    "char_level", "aeration", "fragmentation", "cohesiveness", "whisk_work", "stir_count", "flip_count",
    "time_in_pan", "process_depth", "surface_temp_c", "core_temp_c",
}
PROCESS_TARGETS = {"whisk_work", "stir_count", "flip_count", "time_in_pan", "process_depth"}
THERMAL_TARGETS = {"surface_temp_c", "core_temp_c"}


def main() -> int:
    errors: list[str] = []
    parsed_json = validate_json(errors)
    validate_models(parsed_json, errors)
    validate_blockstates(parsed_json, errors)
    validate_source_blocks(parsed_json, errors)
    validate_source_harvest_contract(errors)
    validate_redstone_automation(errors)
    validate_sided_automation(errors)
    validate_responsive_ui_profiles(errors)
    validate_station_grade_feedback(errors)
    validate_station_interaction_feedback(errors)
    validate_recipe_book_guidance(errors)
    validate_schemas(parsed_json, errors)
    validate_goal_delivery_contract(parsed_json, errors)
    validate_tool_contract(parsed_json, errors)
    validate_tool_runtime_contract(errors)
    validate_recipe_simulation_depth(parsed_json, errors)
    validate_sandwich_contract(parsed_json, errors)
    validate_meal_schema_coverage(errors)
    validate_no_placeholder_markers(errors)
    validate_pngs(errors)

    if errors:
        print("\n".join(errors))
        print(f"\nFAILED with {len(errors)} resource validation error(s)")
        return 1

    item_models = len(list((ASSETS / "models/item").glob("*.json")))
    block_models = len(list((ASSETS / "models/block").glob("*.json")))
    schemas = len(list((DATA / "dish_schema").glob("*.json")))
    item_textures = len(list((ASSETS / "textures/item").glob("*.png")))
    block_textures = len(list((ASSETS / "textures/block").glob("*.png")))
    print(
        "Resource validation passed: "
        f"{item_models} item models, {block_models} block models, "
        f"{schemas} dish schemas, {item_textures} item textures, {block_textures} block textures"
    )
    return 0


def validate_json(errors: list[str]) -> dict[Path, dict]:
    parsed: dict[Path, dict] = {}
    for path in RESOURCES.rglob("*.json"):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception as exc:
            errors.append(f"{rel(path)}: invalid JSON: {exc}")
            continue
        if isinstance(data, dict):
            parsed[path] = data
    return parsed


def validate_models(parsed: dict[Path, dict], errors: list[str]) -> None:
    item_models = {p.stem for p in (ASSETS / "models/item").glob("*.json")}
    block_models = {p.stem for p in (ASSETS / "models/block").glob("*.json")}
    item_textures = {p.stem for p in (ASSETS / "textures/item").glob("*.png")}
    block_textures = {p.stem for p in (ASSETS / "textures/block").glob("*.png")}

    for folder in (ASSETS / "models/item", ASSETS / "models/block"):
        for path in folder.glob("*.json"):
            data = parsed.get(path)
            if data is None:
                continue
            parent = data.get("parent", "")
            if parent.startswith("jazzycookin:item/") and parent.rsplit("/", 1)[-1] not in item_models:
                errors.append(f"{rel(path)}: missing parent {parent}")
            if parent.startswith("jazzycookin:block/") and parent.rsplit("/", 1)[-1] not in block_models:
                errors.append(f"{rel(path)}: missing parent {parent}")
            for ref in data.get("textures", {}).values():
                if not isinstance(ref, str):
                    continue
                if ref.startswith("minecraft:item/"):
                    errors.append(f"{rel(path)}: still uses vanilla item texture {ref}")
                if ref.startswith("jazzycookin:item/") and ref.rsplit("/", 1)[-1] not in item_textures:
                    errors.append(f"{rel(path)}: missing texture {ref}")
                if ref.startswith("jazzycookin:block/") and ref.rsplit("/", 1)[-1] not in block_textures:
                    errors.append(f"{rel(path)}: missing texture {ref}")


def validate_blockstates(parsed: dict[Path, dict], errors: list[str]) -> None:
    block_models = {p.stem for p in (ASSETS / "models/block").glob("*.json")}
    for path in (ASSETS / "blockstates").glob("*.json"):
        data = parsed.get(path)
        if data is None:
            continue
        for key, variant in data.get("variants", {}).items():
            variants = variant if isinstance(variant, list) else [variant]
            for entry in variants:
                if not isinstance(entry, dict):
                    continue
                model = entry.get("model", "")
                if model.startswith("jazzycookin:block/") and model.rsplit("/", 1)[-1] not in block_models:
                    errors.append(f"{rel(path)}: blockstate {key} missing model {model}")


def validate_source_blocks(parsed: dict[Path, dict], errors: list[str]) -> None:
    profile_java = JAVA_SOURCE_PROFILES.read_text(encoding="utf-8")
    block_java = JAVA_BLOCKS.read_text(encoding="utf-8")
    profiles = {
        match.group(1): {
            "id": match.group(2),
            "plant_like": match.group(3) == "true",
            "max_age": int(match.group(4)),
            "ripe_age": int(match.group(5)),
        }
        for match in re.finditer(
            r'([A-Z0-9_]+)\("([a-z0-9_]+)",\s*(true|false),\s*(\d+),\s*(\d+),',
            profile_java,
        )
    }
    source_blocks = re.findall(r'source\("([a-z0-9_]+)",\s*KitchenSourceProfile\.([A-Z0-9_]+)', block_java)
    block_models = {p.stem for p in (ASSETS / "models/block").glob("*.json")}
    block_textures = {p.stem for p in (ASSETS / "textures/block").glob("*.png")}
    item_models = {p.stem for p in (ASSETS / "models/item").glob("*.json")}

    for block_name, profile_name in source_blocks:
        profile = profiles.get(profile_name)
        if profile is None:
            errors.append(f"{rel(JAVA_BLOCKS)}: source {block_name} references unknown profile {profile_name}")
            continue
        if profile["id"] != block_name:
            errors.append(f"{rel(JAVA_BLOCKS)}: source {block_name} uses mismatched profile id {profile['id']}")
        if profile["ripe_age"] > profile["max_age"]:
            errors.append(f"{rel(JAVA_SOURCE_PROFILES)}: profile {profile_name} ripe age exceeds max age")
        if block_name not in item_models:
            errors.append(f"source block {block_name}: missing block item model")

        for folder, kind in (("recipe", "recipe"), ("advancement/recipes", "recipe advancement"), ("loot_table/blocks", "loot table")):
            path = DATA / folder / f"{block_name}.json"
            if path not in parsed:
                errors.append(f"source block {block_name}: missing {kind} {rel(path)}")

        blockstate_path = ASSETS / "blockstates" / f"{block_name}.json"
        blockstate = parsed.get(blockstate_path)
        if blockstate is None:
            errors.append(f"source block {block_name}: missing blockstate")
            continue
        variants = blockstate.get("variants", {})
        for age in range(8):
            key = f"age={age}"
            if key not in variants:
                errors.append(f"{rel(blockstate_path)}: missing variant {key}")
                continue
            entries = variants[key] if isinstance(variants[key], list) else [variants[key]]
            if not any(isinstance(entry, dict) and str(entry.get("model", "")).startswith("jazzycookin:block/") for entry in entries):
                errors.append(f"{rel(blockstate_path)}: variant {key} lacks jazzycookin block model")

        if profile["plant_like"]:
            for age in range(8):
                stage = f"{block_name}_stage{age}"
                if stage not in block_models:
                    errors.append(f"source block {block_name}: missing staged block model {stage}")
                if stage not in block_textures:
                    errors.append(f"source block {block_name}: missing staged block texture {stage}")
        elif block_name not in block_models:
            errors.append(f"source block {block_name}: missing block model")


def validate_source_harvest_contract(errors: list[str]) -> None:
    profile_java = JAVA_SOURCE_PROFILES.read_text(encoding="utf-8")
    source_block_java = JAVA_SOURCE_BLOCK.read_text(encoding="utf-8")
    source_guide_java = JAVA_SOURCE_GUIDE_REGISTRY.read_text(encoding="utf-8")
    item_java = JAVA_ITEMS.read_text(encoding="utf-8")

    for token, message in {
        "Blocks.FARMLAND": "farm sources must be placeable on farmland",
        "isRandomlyTicking": "farm sources must grow over time",
        "randomTick": "farm sources must age on random ticks",
        "performBonemeal": "farm sources must support bonemeal growth",
        "useWithoutItem": "ripe farm sources must be harvestable by interaction",
        "Containers.dropItemStack": "harvested source outputs must drop into the world",
        "recordSourceHarvest": "harvests must advance recipe-book source guides",
        "withPreservationState": "harvest quality must affect generated ingredient matter",
    }.items():
        if token not in source_block_java:
            errors.append(f"{rel(JAVA_SOURCE_BLOCK)}: missing {message}")

    profiles = re.findall(r'([A-Z0-9_]+)\("([a-z0-9_]+)",', profile_java)
    registered_ingredients = set(re.findall(r'([A-Z0-9_]+)\("([a-z0-9_]+)",', item_java))
    ingredient_enums = {enum_name for enum_name, _ in registered_ingredients}
    for profile_name, serialized_name in profiles:
        if profile_name not in source_guide_java:
            errors.append(f"{rel(JAVA_SOURCE_GUIDE_REGISTRY)}: missing source guide for {serialized_name}")
        if f'"{serialized_name}"' not in source_guide_java:
            errors.append(f"{rel(JAVA_SOURCE_GUIDE_REGISTRY)}: missing source guide key {serialized_name}")

    for output_name in re.findall(r'IngredientId\.([A-Z0-9_]+)', source_guide_java):
        if output_name not in ingredient_enums:
            errors.append(f"{rel(JAVA_SOURCE_GUIDE_REGISTRY)}: source guide references unknown ingredient {output_name}")


def validate_redstone_automation(errors: list[str]) -> None:
    block_java = JAVA_STATION_BLOCK.read_text(encoding="utf-8")
    entity_java = JAVA_STATION_ENTITY.read_text(encoding="utf-8")
    gametest_java = JAVA_GAMETESTS.read_text(encoding="utf-8")

    required_block_hooks = {
        "neighborChanged": "stations must react when neighboring redstone changes",
        "onPlace": "stations must sync initial redstone power when placed",
        "level.hasNeighborSignal(pos)": "station redstone hook must check power state",
        "level.getBestNeighborSignal(pos)": "station redstone hook must read analog signal strength",
        "applyRedstoneSignal": "station block must forward redstone to its block entity",
    }
    for token, message in required_block_hooks.items():
        if token not in block_java:
            errors.append(f"{rel(JAVA_STATION_BLOCK)}: missing {token}: {message}")

    required_entity_tokens = {
        "public void applyRedstoneSignal": "block entity must expose redstone control entrypoint",
        "boolean risingEdge = powered && !this.redstonePowered": "redstone automation must distinguish rising edges",
        "StationType.STOVE": "redstone automation must handle stoves",
        "Math.round(clampedStrength * (MAX_STOVE_BURNER_LEVEL / 15.0F))": "analog redstone must map to stove burner level",
        "this.ovenPreheating = powered": "redstone must toggle oven preheating",
        "stationType.supportsStationControl()": "redstone must drive controllable station intensity",
        "clampedStrength <= 5 ? 0 : clampedStrength <= 10 ? 1 : 2": "analog redstone must map to low/medium/high control",
        "StationSimulationResolver.handleAction(this, 6)": "rising redstone edge must trigger a station action",
        'tag.putBoolean("RedstonePowered", this.redstonePowered)': "redstone state must save to NBT",
        'this.redstonePowered = tag.getBoolean("RedstonePowered")': "redstone state must load from NBT",
    }
    for token, message in required_entity_tokens.items():
        if token not in entity_java:
            errors.append(f"{rel(JAVA_STATION_ENTITY)}: missing {message}")

    required_gametest_tokens = {
        "redstoneAutomatesKitchenStations": "redstone automation must have a GameTest",
        "Blocks.REDSTONE_BLOCK": "redstone GameTest must use real redstone power",
        "simulationControlSetting() == 2": "redstone GameTest must verify strong signal control intensity",
        "stove.stoveDialLevel() == 6": "redstone GameTest must verify stove analog maximum",
        "stove.stoveDialLevel() == 0": "redstone GameTest must verify stove shutoff",
        "oven.simulationPreheatProgress() > 0": "redstone GameTest must verify oven preheat",
    }
    for token, message in required_gametest_tokens.items():
        if token not in gametest_java:
            errors.append(f"{rel(JAVA_GAMETESTS)}: missing {message}")


def validate_sided_automation(errors: list[str]) -> None:
    station_java = JAVA_STATION_ENTITY.read_text(encoding="utf-8")
    storage_java = JAVA_STORAGE_ENTITY.read_text(encoding="utf-8")
    gametest_java = JAVA_GAMETESTS.read_text(encoding="utf-8")

    required_station_tokens = {
        "implements WorldlyContainer": "stations must expose sided container automation",
        "getSlotsForFace(Direction side)": "stations must declare side-visible slots",
        "Direction.DOWN": "stations must distinguish extraction from insertion sides",
        "new int[] { OUTPUT_SLOT, BYPRODUCT_SLOT }": "station bottom side must expose only output slots",
        "this.getStationType() == StationType.STOVE": "stove automation must special-case outputless burner-surface extraction",
        "slots[capacity.inputCount()] = BYPRODUCT_SLOT": "stove bottom side must expose byproducts after burner surface slots",
        "StationCapacityProfile.FUEL_SLOT": "fueled stations must expose a fuel insertion slot",
        "slots[index] = TOOL_SLOT": "stations must expose tool insertion for automation",
        "canPlaceItemThroughFace": "stations must validate automated insertion",
        "return this.canPlaceItem(slot, stack)": "automated insertion must reuse normal slot rules",
        "canTakeItemThroughFace": "stations must validate automated extraction",
        "slot == OUTPUT_SLOT || slot == BYPRODUCT_SLOT": "automation extraction must be limited to outputs/byproducts",
        "this.capacityProfile().isActiveInputSlot(slot)": "stove automation extraction must come from active burner surface slots",
        "this.simulationBatch == null": "stove automation must avoid pulling food while an active stove simulation is tracking it",
    }
    for token, message in required_station_tokens.items():
        if token not in station_java:
            errors.append(f"{rel(JAVA_STATION_ENTITY)}: missing {message}")

    required_storage_tokens = {
        "implements WorldlyContainer": "storage blocks must expose sided container automation",
        "getSlotsForFace(Direction side)": "storage blocks must expose inventory slots to automation",
        "new int[this.items.size()]": "storage automation should expose every storage slot",
        "canPlaceItemThroughFace": "storage blocks must validate automated insertion",
        "StorageRules.canStore": "storage automated insertion must respect pantry/fridge/freezer rules",
        "canTakeItemThroughFace": "storage blocks must validate automated extraction",
        "return true": "storage automation should permit extraction from stored slots",
    }
    for token, message in required_storage_tokens.items():
        if token not in storage_java:
            errors.append(f"{rel(JAVA_STORAGE_ENTITY)}: missing {message}")

    required_gametest_tokens = {
        "sidedAutomationRespectsKitchenSlotRoles": "sided automation must have a GameTest",
        "canPlaceItemThroughFace(0, tomatoes, Direction.UP)": "automation GameTest must verify station input insertion",
        "canPlaceItemThroughFace(KitchenStationBlockEntity.TOOL_SLOT": "automation GameTest must verify station tool insertion",
        "canPlaceItemThroughFace(KitchenStationBlockEntity.OUTPUT_SLOT": "automation GameTest must reject output insertion",
        "canTakeItemThroughFace(0, tomatoes, Direction.DOWN)": "automation GameTest must reject unfinished input extraction",
        "canTakeItemThroughFace(KitchenStationBlockEntity.OUTPUT_SLOT": "automation GameTest must allow output extraction",
        "canPlaceItemThroughFace(StationCapacityProfile.FUEL_SLOT": "automation GameTest must verify fueled station insertion",
        "Stove bottom automation should expose burner surface slots": "automation GameTest must verify outputless stove extraction slots",
        "canTakeItemThroughFace(0, stove.getItem(0), Direction.DOWN)": "automation GameTest must allow finished stove food extraction from burner surface",
        "hidden stove output slot": "automation GameTest must reject hidden stove output extraction",
        "canPlaceItemThroughFace(0, chicken, Direction.UP)": "automation GameTest must verify storage insertion",
        "canPlaceItemThroughFace(0, sugar, Direction.UP)": "automation GameTest must reject storage-invalid food",
    }
    for token, message in required_gametest_tokens.items():
        if token not in gametest_java:
            errors.append(f"{rel(JAVA_GAMETESTS)}: missing {message}")


def validate_responsive_ui_profiles(errors: list[str]) -> None:
    station_java = JAVA_STATION_UI.read_text(encoding="utf-8")
    storage_java = JAVA_STORAGE_UI.read_text(encoding="utf-8")
    for path, java in ((JAVA_STATION_UI, station_java), (JAVA_STORAGE_UI, storage_java)):
        if "width = VANILLA_WIDTH" in java or "height = VANILLA_HEIGHT" in java:
            errors.append(f"{rel(path)}: UI profile must not force vanilla container dimensions")
        if "resolveWidth(screenWidth, this.width)" not in java:
            errors.append(f"{rel(path)}: resolve() must use available screen width")
        if "resolveHeight(screenHeight" not in java:
            errors.append(f"{rel(path)}: resolve() must use available screen height")
        if "inventoryStartY = Math.max(150, height - 86)" not in java:
            errors.append(f"{rel(path)}: inventory shelf must be anchored to resolved height")
    if "arrangeInputSlots(resolvedCapacity.inputCount(), resolvedWorkspaceRegion, resolvedToolRegion)" not in station_java:
        errors.append(f"{rel(JAVA_STATION_UI)}: station inputs must be arranged inside the resolved workspace")
    if "centeredInventoryStart(width)" not in station_java:
        errors.append(f"{rel(JAVA_STATION_UI)}: station inventory must be horizontally centered")
    if "buildProfile(storageType, BASE_WIDTH, BASE_HEIGHT)" not in storage_java:
        errors.append(f"{rel(JAVA_STORAGE_UI)}: storage base profile must use finished themed dimensions")


def validate_station_grade_feedback(errors: list[str]) -> None:
    screen_java = JAVA_STATION_SCREEN.read_text(encoding="utf-8")
    required_tokens = {
        "DishEvaluation.evaluateStack(output, level)": "station preview must evaluate finished output quality",
        "outputQualityBreakdown()": "station preview must expose output quality helper",
        "outputBreakdown.summary()": "station preview must show grade summary text",
        "gradeColor(outputBreakdown.finalScore())": "station preview must color-code grade feedback",
        "this.outputStack()": "station preview must read the actual output slot",
    }
    for token, message in required_tokens.items():
        if token not in screen_java:
            errors.append(f"{rel(JAVA_STATION_SCREEN)}: missing {message}")


def validate_station_interaction_feedback(errors: list[str]) -> None:
    screen_java = JAVA_STATION_SCREEN.read_text(encoding="utf-8")
    client_java = JAVA_CLIENT_ENTRYPOINT.read_text(encoding="utf-8")
    renderer_java = JAVA_STATION_BER.read_text(encoding="utf-8") if JAVA_STATION_BER.exists() else ""
    station_java = JAVA_STATION_ENTITY.read_text(encoding="utf-8")
    pan_java = JAVA_PAN_SIM.read_text(encoding="utf-8")
    pot_java = JAVA_POT_SIM.read_text(encoding="utf-8")
    gametest_java = JAVA_GAMETESTS.read_text(encoding="utf-8")
    required_tokens = {
        "renderItemFlow(guiGraphics, left, top, partialTick)": "station UI must render animated item flow",
        "shouldShowItemFlow()": "station UI must gate flow feedback on active cooking state",
        "drawFlowPath": "station UI must draw paths between ingredients and output",
        "this.menu.activeInputCount()": "item flow must consider station input slots",
        "this.menu.outputMenuSlotIndex()": "item flow must lead toward the output slot",
        "this.menu.simulationWorking()": "item flow must react to simulation work state",
    }
    for token, message in required_tokens.items():
        if token not in screen_java:
            errors.append(f"{rel(JAVA_STATION_SCREEN)}: missing {message}")

    required_renderer_tokens = {
        "registerBlockEntityRenderer(JazzyBlockEntities.KITCHEN_STATION.get()": "kitchen stations must register an in-world block entity renderer",
        "KitchenStationBlockEntityRenderer": "kitchen station block entity renderer must exist",
        "ItemDisplayContext.GROUND": "in-world station renderer must draw actual item stacks",
        "station.simulationActive()": "in-world station renderer must animate while station work is active",
        "lerp(position[0], 0.50F": "in-world station renderer must move inputs toward the work area",
        "getUpdatePacket()": "station block entity must sync contents to clients for in-world rendering",
        "syncClientInventoryState()": "station inventory changes must notify nearby clients",
    }
    for token, message in required_renderer_tokens.items():
        haystack = client_java + renderer_java + station_java
        if token not in haystack:
            errors.append(f"in-world station renderer contract: missing {message}")

    if "access.outputSlot()" in pan_java or "access.outputSlot()" in pot_java:
        errors.append("stove simulation contract: pan/pot finishes must stay on burner input slots, not station output slots")
    stove_surface_tokens = {
        "this.menu.stationType() == StationType.STOVE": "stove screen must special-case outputless burner UI",
        "SlotPositioning.setPosition(outputSlot, -1000, -1000)": "stove output slot must be hidden offscreen",
        "SlotPositioning.setPosition(byproductSlot, -1000, -1000)": "stove byproduct slot must be hidden offscreen",
        "stove.getItem(0).is": "GameTests must assert stove results appear on the burner surface",
    }
    for token, message in stove_surface_tokens.items():
        haystack = screen_java + gametest_java
        if token not in haystack:
            errors.append(f"stove surface cooking contract: missing {message}")


def validate_recipe_book_guidance(errors: list[str]) -> None:
    planner_java = JAVA_RECIPE_BOOK_PLANNER.read_text(encoding="utf-8")
    screen_java = JAVA_RECIPE_BOOK_SCREEN.read_text(encoding="utf-8")
    gametest_java = JAVA_GAMETESTS.read_text(encoding="utf-8")
    required_planner_tokens = {
        "Process targets:": "recipe book planner must emit visible process target guidance",
        "Thermal targets:": "recipe book planner must emit visible thermal target guidance",
        "schema.targets().timeInPan()": "recipe book planner must derive durations from schema timing targets",
        "schema.targets().surfaceTempC()": "recipe book planner must expose surface temperature targets",
        "allowedTools": "recipe book planner must expose alternate valid cooking tools",
    }
    for token, message in required_planner_tokens.items():
        if token not in planner_java:
            errors.append(f"{rel(JAVA_RECIPE_BOOK_PLANNER)}: missing {message}")

    required_screen_tokens = {
        "parts.add(option.notes().get(0))": "recipe book step rows must display planner notes alongside station/tool context",
        "instructionRowsFor(JazzyRecipeBookPlanner.PlanStep step)": "recipe book must expand planner steps into actionable instruction rows",
        "compactRequirementLabel": "recipe book instruction rows must keep requirement text compact",
        "visibleStep.title()": "recipe book rows must render action-specific titles",
        "visibleStep.detail()": "recipe book rows must render action-specific details",
        "renderStepTooltip(guiGraphics, mouseX, mouseY)": "recipe book must expose focused action guidance in a hover tooltip",
        "stepTooltip(VisibleStep visibleStep)": "recipe book must build action-specific step tooltips",
        "Component::getVisualOrderText": "recipe book tooltip lines must use the Minecraft formatted tooltip API",
        "heat.jazzycookin.": "recipe book tooltip heat values must use translated heat labels",
    }
    for token, message in required_screen_tokens.items():
        if token not in screen_java:
            errors.append(f"{rel(JAVA_RECIPE_BOOK_SCREEN)}: missing {message}")
    if 'Component.literal("Station:' in screen_java or 'Component.literal("Method:' in screen_java:
        errors.append(f"{rel(JAVA_RECIPE_BOOK_SCREEN)}: recipe book tooltip labels must not be hardcoded English literals")

    lang = (ASSETS / "lang/en_us.json").read_text(encoding="utf-8")
    for key in (
            "screen.jazzycookin.recipe_book.tooltip.station",
            "screen.jazzycookin.recipe_book.tooltip.method",
            "screen.jazzycookin.recipe_book.tooltip.preferred_tool",
            "screen.jazzycookin.recipe_book.tooltip.valid_tools",
            "screen.jazzycookin.recipe_book.tooltip.duration",
            "screen.jazzycookin.recipe_book.tooltip.heat",
            "screen.jazzycookin.recipe_book.tooltip.needs",
    ):
        if key not in lang:
            errors.append(f"{rel(ASSETS / 'lang/en_us.json')}: missing recipe book tooltip translation {key}")

    required_gametest_tokens = {
        "cook 150-280 ticks": "recipe guide GameTest must verify process targets are explained",
        "surface 125-190C": "recipe guide GameTest must verify thermal targets are explained",
        "Sandwich guide should explain toast, cheese, and seasoning variations": "recipe guide GameTest must verify flexible sandwich notes",
    }
    for token, message in required_gametest_tokens.items():
        if token not in gametest_java:
            errors.append(f"{rel(JAVA_GAMETESTS)}: missing {message}")


def validate_schemas(parsed: dict[Path, dict], errors: list[str]) -> None:
    known_items = known_item_ids()
    preview_ids: dict[int, str] = {}
    for path in (DATA / "dish_schema").glob("*.json"):
        data = parsed.get(path)
        if data is None:
            continue
        if data.get("category") not in CATEGORIES:
            errors.append(f"{rel(path)}: bad category {data.get('category')}")
        preview_id = data.get("preview_id")
        if preview_id in preview_ids:
            errors.append(f"{rel(path)}: duplicate preview_id {preview_id} also {preview_ids[preview_id]}")
        preview_ids[preview_id] = path.name
        validate_item_ref(path, data.get("result", ""), "result", known_items, errors)
        for serving in data.get("serving_items", []):
            validate_item_ref(path, serving, "serving item", known_items, errors)
        for section in ("required_roles", "optional_roles", "ingredients"):
            for item in data.get(section, []):
                role = item.get("role")
                if role and role not in ROLES:
                    errors.append(f"{rel(path)}: bad role {role}")
                for key in ("any_traits", "all_traits"):
                    for trait in item.get(key, []):
                        if trait not in TRAITS:
                            errors.append(f"{rel(path)}: bad trait {trait}")
        for technique in data.get("required_techniques", []):
            if technique not in TECHNIQUES:
                errors.append(f"{rel(path)}: bad technique {technique}")
        for step in data.get("steps", []):
            technique = step.get("technique")
            if technique and technique not in TECHNIQUES:
                errors.append(f"{rel(path)}: bad step technique {technique}")
        for key, value in data.get("targets", {}).items():
            if key not in TARGETS:
                errors.append(f"{rel(path)}: bad target {key}")
            if isinstance(value, dict):
                for required in ("min", "max", "softness"):
                    if required not in value:
                        errors.append(f"{rel(path)}: target {key} missing {required}")


def validate_goal_delivery_contract(parsed: dict[Path, dict], errors: list[str]) -> None:
    item_java = JAVA_ITEMS.read_text(encoding="utf-8")
    source_java = JAVA_SOURCE_PROFILES.read_text(encoding="utf-8")
    source_guide_java = JAVA_SOURCE_GUIDE_REGISTRY.read_text(encoding="utf-8")
    gametest_java = JAVA_GAMETESTS.read_text(encoding="utf-8")
    prep_sim_java = JAVA_PREP_SIM.read_text(encoding="utf-8")
    preserve_sim_java = JAVA_PRESERVE_SIM.read_text(encoding="utf-8")
    station_java = (ROOT / "src/main/java/com/boaat/jazzy_cookin/kitchen/StationType.java").read_text(encoding="utf-8")

    meal_items = re.findall(r'meal\("([a-z0-9_]+)"', item_java)
    if len(meal_items) < 20:
        errors.append(f"registered meal count {len(meal_items)} is below the requested 20+ recipes")

    schema_paths = list((DATA / "dish_schema").glob("*.json"))
    schema_keys = {
        data.get("key")
        for path in schema_paths
        if (data := parsed.get(path)) is not None
    }
    missing_schema_meals = [
        meal for meal in meal_items
        if meal not in schema_keys and f"{meal}_meal" not in schema_keys
    ]
    if missing_schema_meals:
        errors.append(f"registered meals missing dish schemas: {', '.join(sorted(missing_schema_meals))}")

    source_profiles = re.findall(r'([A-Z0-9_]+)\("([a-z0-9_]+)",\s*(true|false),', source_java)
    plant_sources = [serialized for _, serialized, plant_like in source_profiles if plant_like == "true"]
    if len(source_profiles) < 8:
        errors.append(f"source profile count {len(source_profiles)} is below the farming/source coverage target")
    if len(plant_sources) < 10:
        errors.append(f"plant-like source count {len(plant_sources)} is below the veggie farming coverage target")

    fresh_produce_ids = [
        "APPLES",
        "TOMATOES",
        "CARROTS",
        "ONIONS",
        "POTATOES",
        "LEMONS",
        "CABBAGE",
        "GARLIC",
        "GINGER",
        "SHALLOTS",
        "SPINACH",
        "GREEN_PEAS",
        "JALAPENOS",
        "RED_PEPPER",
    ]
    for ingredient_id in fresh_produce_ids:
        token = f"IngredientId.{ingredient_id}"
        if token not in source_guide_java:
            errors.append(f"{rel(JAVA_SOURCE_GUIDE_REGISTRY)}: missing source guide output for fresh produce {ingredient_id}")
    if "freshProduceIngredientsExposeSourceGuides" not in gametest_java:
        errors.append(f"{rel(JAVA_GAMETESTS)}: missing fresh produce source-guide GameTest coverage")

    station_names = re.findall(r'^\s*([A-Z0-9_]+)\("([a-z0-9_]+)"', station_java, flags=re.M)
    if len(station_names) < 12:
        errors.append(f"station count {len(station_names)} is below the interactive cooking station coverage target")

    secondary_station_tokens = {
        "secondaryStationsTransformFoodWithSimulationDomains": "secondary station GameTest coverage",
        "IngredientState.GROUND_SPICE": "spice grinder transformed output coverage",
        "IngredientState.STRAINED": "strainer transformed output coverage",
        "IngredientState.DRIED_FRUIT": "drying rack transformed output coverage",
        "IngredientState.FERMENTED_VEGETABLE": "fermentation crock transformed output coverage",
        "IngredientState.COOLED": "cooling rack transformed output coverage",
    }
    for token, message in secondary_station_tokens.items():
        if token not in gametest_java:
            errors.append(f"{rel(JAVA_GAMETESTS)}: missing {message}")
    if "StationType.SPICE_GRINDER || access.simulationStationType() == StationType.STRAINER" not in prep_sim_java:
        errors.append(f"{rel(JAVA_PREP_SIM)}: spice grinder/strainer must accept a single supportive ingredient as the primary input")
    if "directDominantOutput(access, analysis, matter)" not in preserve_sim_java:
        errors.append(f"{rel(JAVA_PRESERVE_SIM)}: drying and fermentation stations must fall back to direct transformed ingredient outputs")

    texture_generators = [
        ROOT / "texture_sources/generate_meal_textures.py",
        ROOT / "texture_sources/generate_block_textures.py",
        ROOT / "texture_sources/generate_ingredient_textures.py",
        ROOT / "texture_sources/generate_remaining_item_textures.py",
        ROOT / "texture_sources/generate_source_resources.py",
    ]
    for generator in texture_generators:
        if not generator.exists():
            errors.append(f"missing generated texture source script {rel(generator)}")

    if not PY_RECIPE_SIMULATOR.exists():
        errors.append(f"missing recipe builder/simulator script {rel(PY_RECIPE_SIMULATOR)}")
    else:
        simulator = PY_RECIPE_SIMULATOR.read_text(encoding="utf-8")
        required_simulator_tokens = {
            "def score_schema": "recipe simulator must score attempts against dish schemas",
            "def canonical_attempt": "recipe simulator must synthesize validation attempts",
            "--validate-all": "recipe simulator must expose whole-catalog validation",
            "--validate-stations": "recipe simulator must validate station step inputs and outputs",
            "--station-report": "recipe simulator must report expected station transformations",
            "def simulate_station_steps": "recipe simulator must model schema station transformations",
            "--emit-template": "recipe simulator must help build starter recipe schemas",
            "finalize_threshold": "recipe simulator must validate recipe output thresholds",
        }
        for token, message in required_simulator_tokens.items():
            if token not in simulator:
                errors.append(f"{rel(PY_RECIPE_SIMULATOR)}: missing {message}")


def validate_recipe_simulation_depth(parsed: dict[Path, dict], errors: list[str]) -> None:
    schema_paths = sorted((DATA / "dish_schema").glob("*.json"))
    if len(schema_paths) < 20:
        errors.append(f"dish schema count {len(schema_paths)} is below required 20+ recipes")

    meal_paths = []
    for path in schema_paths:
        data = parsed.get(path)
        if data is None:
            continue
        result = data.get("result", "")
        if data.get("meal") is True or result.endswith("_meal") or data.get("category") in {"plated", "soup", "pan_dish", "baked"}:
            meal_paths.append(path)

    if len(meal_paths) < 20:
        errors.append(f"meal-like schema count {len(meal_paths)} is below required 20+ complex recipes")

    meal_categories = {
        data.get("category")
        for path in meal_paths
        if (data := parsed.get(path)) is not None
    }
    if len(meal_categories) < 5:
        errors.append(f"meal schema categories are too narrow for a varied cooking catalog: {', '.join(sorted(meal_categories))}")

    recipe_families = {
        recipe_family(data.get("key", path.stem))
        for path in meal_paths
        if (data := parsed.get(path)) is not None
    }
    if len(recipe_families) < 20:
        errors.append(f"distinct recipe family count {len(recipe_families)} is below required 20+ varied recipes")

    required_cooking_techniques = {"pan_fried", "simmered", "baked", "mixed", "plated"}
    catalog_techniques = {
        technique
        for path in meal_paths
        if (data := parsed.get(path)) is not None
        for technique in data.get("required_techniques", [])
    }
    missing_techniques = sorted(required_cooking_techniques - catalog_techniques)
    if missing_techniques:
        errors.append(f"meal schema catalog is missing core cooking technique(s): {', '.join(missing_techniques)}")

    for path in meal_paths:
        data = parsed.get(path)
        if data is None:
            continue
        target_keys = set(data.get("targets", {}))
        weights = data.get("weights", {})
        if not data.get("required_roles"):
            errors.append(f"{rel(path)}: meal schema must define required roles for flexible grading")
        if not (target_keys & PROCESS_TARGETS):
            errors.append(f"{rel(path)}: meal schema must define a process target so timing/handling matters")
        if not (target_keys & THERMAL_TARGETS):
            errors.append(f"{rel(path)}: meal schema must define a thermal target so cooking heat matters")
        if weights.get("process", 0) <= 0:
            errors.append(f"{rel(path)}: process target is present but process weight is not positive")
        if weights.get("thermal", 0) <= 0:
            errors.append(f"{rel(path)}: thermal target is present but thermal weight is not positive")


def recipe_family(key: str) -> str:
    for suffix in ("_prep", "_meal", "_plate", "_filling", "_batter", "_soaked"):
        if key.endswith(suffix):
            return key[: -len(suffix)]
    return key


def validate_tool_contract(parsed: dict[Path, dict], errors: list[str]) -> None:
    known_profiles = set(re.findall(r'\("([a-z0-9_]+)"\)', JAVA_TOOL_PROFILE.read_text(encoding="utf-8")))
    registered_profiles = registered_tool_profiles()
    item_models = {path.stem for path in (ASSETS / "models/item").glob("*.json")}

    for tool_name in registered_tool_item_names():
        if tool_name not in item_models:
            errors.append(f"registered tool {tool_name}: missing item model")

    for path in sorted((DATA / "dish_schema").glob("*.json")):
        data = parsed.get(path)
        if data is None:
            continue
        for step in data.get("steps", []):
            tools = []
            if step.get("tool"):
                tools.append(step["tool"])
            tools.extend(step.get("tools", []))
            if not tools:
                continue
            unknown = [tool for tool in tools if tool not in known_profiles]
            if unknown:
                errors.append(f"{rel(path)}: step {step.get('id')} references unknown tool profile(s): {', '.join(sorted(set(unknown)))}")
            if not any(tool in registered_profiles for tool in tools):
                errors.append(f"{rel(path)}: step {step.get('id')} has no registered in-game tool profile")
            if step.get("tools") and step["tools"][0] not in registered_profiles:
                errors.append(f"{rel(path)}: step {step.get('id')} primary tool {step['tools'][0]} is not registered; first tool gets the best grade")


def validate_tool_runtime_contract(errors: list[str]) -> None:
    station_java = JAVA_STATION_ENTITY.read_text(encoding="utf-8")
    simulation_java = JAVA_COMPOSITIONAL_SIM.read_text(encoding="utf-8")
    tool_java = (ROOT / "src/main/java/com/boaat/jazzy_cookin/item/KitchenToolItem.java").read_text(encoding="utf-8")
    gametest_java = JAVA_GAMETESTS.read_text(encoding="utf-8")
    required_station_tokens = {
        "damageToolOnAction()": "station actions must wear tools at runtime",
        "tool.isDamageableItem()": "tool wear must respect damageable item state",
        "tool.getDamageValue() + 1": "tool wear must advance durability damage",
        "tool.setDamageValue(nextDamage)": "tool wear must save partial damage",
        "tool.shrink(1)": "tool wear must break tools that reach max damage",
        "risingEdge && StationSimulationResolver.handleAction(this, 6)": "redstone-triggered station actions must share the normal simulation path",
    }
    for token, message in required_station_tokens.items():
        if token not in station_java:
            errors.append(f"{rel(JAVA_STATION_ENTITY)}: missing {message}")

    required_simulation_tokens = {
        "toolSpeedMultiplier(access)": "tool speed stats must affect station timing",
        "toolQualityBonus(access)": "tool quality stats must affect station results",
        "toolItem.speedMultiplier()": "station simulation must read tool speed multipliers",
        "toolItem.qualityBonus()": "station simulation must read tool quality bonuses",
        "modifier /= toolSpeedMultiplier(access)": "tool speed multipliers must shorten station action durations",
        "stationQuality(access, matter": "tool quality bonuses must feed schema attempt quality",
    }
    for token, message in required_simulation_tokens.items():
        if token not in simulation_java:
            errors.append(f"{rel(JAVA_COMPOSITIONAL_SIM)}: missing {message}")

    required_tool_tooltip_tokens = {
        "tooltip.jazzycookin.tool_quality_bonus": "tool tooltip must expose quality bonus",
        "tooltip.jazzycookin.tool_speed_multiplier": "tool tooltip must expose work speed",
        "Math.round(this.qualityBonus * 100.0F)": "tool quality tooltip must display a player-readable percent",
        "Math.round(this.speedMultiplier * 100.0F)": "tool speed tooltip must display a player-readable percent",
    }
    for token, message in required_tool_tooltip_tokens.items():
        if token not in tool_java:
            errors.append(f"src/main/java/com/boaat/jazzy_cookin/item/KitchenToolItem.java: missing {message}")

    required_gametest_tokens = {
        "stationActionsWearTools": "tool wear must have a GameTest",
        "toolStatsAffectStationSpeedAndQuality": "tool stat effects must have a GameTest",
        "getDamageValue() == 1": "tool wear GameTest must verify incremental damage",
        "setDamageValue(nearlyBrokenScale.getMaxDamage() - 1)": "tool wear GameTest must exercise near-break durability",
        "getItem(KitchenStationBlockEntity.TOOL_SLOT).isEmpty()": "tool wear GameTest must verify breaking tools",
        "chefPrep.simulationMaxProgress() < tablePrep.simulationMaxProgress()": "tool stat GameTest must verify speed differences",
        "chefMatter.cohesiveness() > tableMatter.cohesiveness()": "tool stat GameTest must verify quality differences",
    }
    for token, message in required_gametest_tokens.items():
        if token not in gametest_java:
            errors.append(f"{rel(JAVA_GAMETESTS)}: missing {message}")


def registered_tool_item_names() -> set[str]:
    java = JAVA_ITEMS.read_text(encoding="utf-8")
    return set(re.findall(r'tool\("([a-z0-9_]+)"\s*,\s*ToolProfile\.[A-Z0-9_]+', java))


def registered_tool_profiles() -> set[str]:
    item_java = JAVA_ITEMS.read_text(encoding="utf-8")
    profile_java = JAVA_TOOL_PROFILE.read_text(encoding="utf-8")
    enum_to_serialized = {
        name: serialized
        for name, serialized in re.findall(r'([A-Z0-9_]+)\("([a-z0-9_]+)"\)', profile_java)
    }
    profiles = set()
    for profile_name in re.findall(r'tool\("[a-z0-9_]+"\s*,\s*ToolProfile\.([A-Z0-9_]+)', item_java):
        profiles.add(enum_to_serialized.get(profile_name, profile_name.lower()))
    return profiles


def validate_sandwich_contract(parsed: dict[Path, dict], errors: list[str]) -> None:
    filling_path = DATA / "dish_schema/sandwich_filling.json"
    assembled_path = DATA / "dish_schema/assembled_sandwich.json"
    plate_path = DATA / "dish_schema/sandwich_plate.json"
    filling = parsed.get(filling_path)
    assembled = parsed.get(assembled_path)
    plate = parsed.get(plate_path)
    for path, data in ((filling_path, filling), (assembled_path, assembled), (plate_path, plate)):
        if data is None:
            errors.append(f"missing sandwich schema {rel(path)}")
            return

    vegetable_traits = {"vegetable", "leafy_green", "allium", "tomato", "pepper"}
    seasoning_traits = {"salt", "herb", "spice", "pepper", "condiment", "sauce", "acidic"}
    knife_tools = {"knife", "chef_knife", "paring_knife", "cleaver", "table_knife"}

    if not role_allows(filling, "required_roles", "vegetable", any_traits=vegetable_traits):
        errors.append(f"{rel(filling_path)}: filling must accept flexible vegetable traits")
    if "bread" not in set(filling.get("forbidden_traits", [])):
        errors.append(f"{rel(filling_path)}: filling must forbid bread so raw sandwiches cannot skip assembly")
    if not roles_cover(filling, "optional_roles", seasoning_traits):
        errors.append(f"{rel(filling_path)}: filling optional roles must cover seasoning/herb/spice variants")
    if not step_has(filling, "fill", station="prep_table", technique="cut", tools=knife_tools):
        errors.append(f"{rel(filling_path)}: fill step must use prep_table cut action with knife-family tools")
    require_targets(filling_path, filling, {"seasoning", "fragmentation", "cohesiveness", "process_depth"}, errors)

    if "sandwich_filling" not in set(assembled.get("prerequisite_schemas", [])):
        errors.append(f"{rel(assembled_path)}: assembled sandwich must depend on sandwich_filling")
    if not role_allows(assembled, "required_roles", "grain", any_traits={"bread"}, all_traits={"bread_loaf"}):
        errors.append(f"{rel(assembled_path)}: assembled sandwich must require bread loaf grain, not generic crumbs")
    if not role_allows(assembled, "required_roles", "vegetable", any_traits=vegetable_traits):
        errors.append(f"{rel(assembled_path)}: assembled sandwich must retain flexible vegetable requirement")
    if not ingredient_allows(assembled, "jazzycookin:sandwich_filling", "vegetable", any_traits=vegetable_traits):
        errors.append(f"{rel(assembled_path)}: assembled sandwich ingredient must require prepared vegetable filling")
    if not roles_cover(assembled, "optional_roles", {"dairy", "protein"} | seasoning_traits):
        errors.append(f"{rel(assembled_path)}: assembled sandwich optional roles must cover cheese, protein, and seasoning variants")
    if not step_has(assembled, "assemble", station="prep_table", technique="prepped", tools=knife_tools, prerequisites={"fill"}):
        errors.append(f"{rel(assembled_path)}: assemble step must preserve fill prerequisite and knife-family tools")
    require_targets(assembled_path, assembled, {"seasoning", "cheese", "browning", "char_level", "process_depth"}, errors)
    if assembled.get("weights", {}).get("cooking", 0) <= 0 or assembled.get("weights", {}).get("process", 0) <= 0:
        errors.append(f"{rel(assembled_path)}: assembled sandwich must grade toast/cooking and handling depth")

    if plate.get("meal") is not True:
        errors.append(f"{rel(plate_path)}: sandwich plate must be marked as a meal")
    if "assembled_sandwich" not in set(plate.get("prerequisite_schemas", [])):
        errors.append(f"{rel(plate_path)}: sandwich plate must depend on assembled_sandwich")
    if "jazzycookin:ceramic_plate" not in set(plate.get("serving_items", [])):
        errors.append(f"{rel(plate_path)}: sandwich plate must require a ceramic plate serving item")
    if not step_has(plate, "plate", station="plating_station", technique="plated", prerequisites={"assemble"}):
        errors.append(f"{rel(plate_path)}: plate step must use plating_station and preserve assemble prerequisite")
    require_targets(plate_path, plate, {"seasoning", "browning", "char_level", "process_depth"}, errors)
    if plate.get("weights", {}).get("process", 0) <= 0 or plate.get("weights", {}).get("technique", 0) <= 0:
        errors.append(f"{rel(plate_path)}: final sandwich plate must grade process and equipment lineage")

    validate_sandwich_food_profiles(errors)


def role_allows(data: dict, section: str, role: str, any_traits: set[str], all_traits: set[str] | None = None) -> bool:
    for entry in data.get(section, []):
        if entry.get("role") != role:
            continue
        if not any_traits.issubset(set(entry.get("any_traits", []))):
            continue
        if all_traits and not all_traits.issubset(set(entry.get("all_traits", []))):
            continue
        return True
    return False


def roles_cover(data: dict, section: str, required_traits: set[str]) -> bool:
    traits: set[str] = set()
    for entry in data.get(section, []):
        traits.update(entry.get("any_traits", []))
    return required_traits.issubset(traits)


def ingredient_allows(data: dict, item: str, role: str, any_traits: set[str]) -> bool:
    for entry in data.get("ingredients", []):
        if entry.get("item") == item and entry.get("role") == role and any_traits.issubset(set(entry.get("any_traits", []))):
            return True
    return False


def step_has(
        data: dict,
        step_id: str,
        station: str,
        technique: str,
        tools: set[str] | None = None,
        prerequisites: set[str] | None = None,
) -> bool:
    for step in data.get("steps", []):
        if step.get("id") != step_id:
            continue
        if step.get("station") != station or step.get("technique") != technique:
            return False
        if tools and not tools.issubset(set(step.get("tools", []))):
            return False
        if prerequisites and not prerequisites.issubset(set(step.get("prerequisites", []))):
            return False
        return True
    return False


def require_targets(path: Path, data: dict, required: set[str], errors: list[str]) -> None:
    missing = sorted(required - set(data.get("targets", {})))
    if missing:
        errors.append(f"{rel(path)}: missing sandwich grading targets {', '.join(missing)}")


def validate_sandwich_food_profiles(errors: list[str]) -> None:
    java = JAVA_FOOD_PROFILES.read_text(encoding="utf-8")
    for ingredient in ("BREAD", "WHOLE_WHEAT_BREAD", "SOURDOUGH_BREAD", "RYE_BREAD"):
        if not profile_register_has(java, ingredient, {"BREAD", "BREAD_LOAF"}):
            errors.append(f"{rel(JAVA_FOOD_PROFILES)}: {ingredient} must carry BREAD and BREAD_LOAF traits for sandwich variation")
    if not profile_register_has(java, "SOURDOUGH_BREAD", {"FERMENTED", "ACIDIC"}):
        errors.append(f"{rel(JAVA_FOOD_PROFILES)}: SOURDOUGH_BREAD must preserve fermented/acidic traits")
    for ingredient in ("BREADCRUMBS", "COOKIES"):
        if profile_register_has(java, ingredient, {"BREAD_LOAF"}):
            errors.append(f"{rel(JAVA_FOOD_PROFILES)}: {ingredient} must not satisfy sandwich bread loaf")


def profile_register_has(java: str, ingredient: str, traits: set[str]) -> bool:
    matches = re.finditer(
        r'register\(profile\((?P<body>.*?)\)\s*,\s*(?P<ids>.*?)\);',
        java,
        flags=re.S,
    )
    for match in matches:
        ids = set(re.findall(r'IngredientId\.([A-Z0-9_]+)', match.group("ids")))
        if ingredient not in ids:
            continue
        body_traits = set(re.findall(r'FoodTrait\.([A-Z0-9_]+)', match.group("body")))
        return traits.issubset(body_traits)
    return False


def validate_meal_schema_coverage(errors: list[str]) -> None:
    java = JAVA_ITEMS.read_text(encoding="utf-8")
    meals = re.findall(r'meal\("([a-z0-9_]+)"', java)
    schemas = {p.stem for p in (DATA / "dish_schema").glob("*.json")}
    missing = [meal for meal in meals if meal not in schemas and f"{meal}_meal" not in schemas]
    for meal in missing:
        errors.append(f"registered meal lacks schema: {meal}")
    generated_names = set(re.findall(r'^\s*"([a-z0-9_]+)":', (ROOT / "texture_sources/generate_additional_meal_schemas.py").read_text(encoding="utf-8"), re.M))
    for name in generated_names:
        path = DATA / "dish_schema" / f"{name}.json"
        if not path.exists():
            continue
        data = json.loads(path.read_text(encoding="utf-8"))
        target_keys = set(data.get("targets", {}))
        if not (target_keys & PROCESS_TARGETS):
            errors.append(f"{rel(path)}: generated schema missing process targets")
        if not (target_keys & THERMAL_TARGETS):
            errors.append(f"{rel(path)}: generated schema missing thermal targets")
        weights = data.get("weights", {})
        if weights.get("process", 0) <= 0:
            errors.append(f"{rel(path)}: generated schema missing process weight")
        if weights.get("thermal", 0) <= 0:
            errors.append(f"{rel(path)}: generated schema missing thermal weight")


def validate_pngs(errors: list[str]) -> None:
    png_signature = b"\x89PNG\r\n\x1a\n"
    for path in (ASSETS / "textures").rglob("*.png"):
        try:
            data = path.read_bytes()
            if data[:8] != png_signature:
                errors.append(f"{rel(path)}: invalid PNG signature")
                continue
            info = png_info(data)
            if info is None:
                errors.append(f"{rel(path)}: missing PNG metadata")
                continue
            width, height, bit_depth, color_type, visible_pixels, visible_colors = info
            if (width, height) != (16, 16):
                errors.append(f"{rel(path)}: texture must be 16x16, got {width}x{height}")
            if bit_depth != 8 or color_type != 6:
                errors.append(f"{rel(path)}: texture must be 8-bit RGBA PNG, got bit depth {bit_depth}, color type {color_type}")
            if visible_pixels < 8:
                errors.append(f"{rel(path)}: texture has too few visible pixels ({visible_pixels})")
            if visible_colors < 2:
                errors.append(f"{rel(path)}: texture appears blank or flat ({visible_colors} visible colors)")
        except OSError as exc:
            errors.append(f"{rel(path)}: cannot read PNG: {exc}")
        except (struct.error, zlib.error, ValueError) as exc:
            errors.append(f"{rel(path)}: invalid PNG data: {exc}")


def png_info(data: bytes) -> tuple[int, int, int, int, int, int] | None:
    pos = 8
    width = height = bit_depth = color_type = None
    idat_chunks: list[bytes] = []
    while pos + 12 <= len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        chunk_type = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if chunk_type == b"IHDR":
            width, height, bit_depth, color_type, _, _, _ = struct.unpack(">IIBBBBB", chunk)
        elif chunk_type == b"IDAT":
            idat_chunks.append(chunk)
        elif chunk_type == b"IEND":
            break
    if width is None or height is None or bit_depth is None or color_type is None:
        return None
    if bit_depth != 8 or color_type != 6 or not idat_chunks:
        return width, height, bit_depth, color_type, 0, 0
    pixels = decode_rgba_png_pixels(width, height, b"".join(idat_chunks))
    visible = [pixel for pixel in pixels if pixel[3] > 0]
    return width, height, bit_depth, color_type, len(visible), len(set(visible))


def decode_rgba_png_pixels(width: int, height: int, compressed: bytes) -> list[tuple[int, int, int, int]]:
    raw = zlib.decompress(compressed)
    bytes_per_pixel = 4
    stride = width * bytes_per_pixel
    expected = height * (stride + 1)
    if len(raw) < expected:
        raise ValueError(f"decompressed PNG data is too short: {len(raw)} < {expected}")
    previous = bytearray(stride)
    pixels: list[tuple[int, int, int, int]] = []
    pos = 0
    for _ in range(height):
        filter_type = raw[pos]
        pos += 1
        row = bytearray(raw[pos:pos + stride])
        pos += stride
        for index in range(stride):
            left = row[index - bytes_per_pixel] if index >= bytes_per_pixel else 0
            above = previous[index]
            upper_left = previous[index - bytes_per_pixel] if index >= bytes_per_pixel else 0
            if filter_type == 1:
                row[index] = (row[index] + left) & 0xFF
            elif filter_type == 2:
                row[index] = (row[index] + above) & 0xFF
            elif filter_type == 3:
                row[index] = (row[index] + ((left + above) // 2)) & 0xFF
            elif filter_type == 4:
                row[index] = (row[index] + paeth_predictor(left, above, upper_left)) & 0xFF
            elif filter_type != 0:
                raise ValueError(f"unknown PNG filter {filter_type}")
        pixels.extend(tuple(row[index:index + 4]) for index in range(0, stride, bytes_per_pixel))
        previous = row
    return pixels


def paeth_predictor(left: int, above: int, upper_left: int) -> int:
    estimate = left + above - upper_left
    left_distance = abs(estimate - left)
    above_distance = abs(estimate - above)
    upper_left_distance = abs(estimate - upper_left)
    if left_distance <= above_distance and left_distance <= upper_left_distance:
        return left
    if above_distance <= upper_left_distance:
        return above
    return upper_left


def validate_no_placeholder_markers(errors: list[str]) -> None:
    scanned_roots = [
        ROOT / "src/main/java",
        ROOT / "src/main/resources",
        ROOT / "scripts",
        ROOT / "texture_sources",
    ]
    pattern = re.compile(r"\b(TODO|FIXME|placeholder|stub|not implemented)\b", re.IGNORECASE)
    for root in scanned_roots:
        if not root.exists():
            continue
        for path in root.rglob("*"):
            if path == Path(__file__).resolve():
                continue
            if not path.is_file() or path.suffix.lower() in {".png", ".jar", ".class"}:
                continue
            try:
                text = path.read_text(encoding="utf-8")
            except UnicodeDecodeError:
                continue
            for line_number, line in enumerate(text.splitlines(), start=1):
                if pattern.search(line):
                    errors.append(f"{rel(path)}:{line_number}: placeholder marker remains: {line.strip()}")


def validate_item_ref(path: Path, ref: str, label: str, known_items: set[str], errors: list[str]) -> None:
    if isinstance(ref, str) and ref.startswith("jazzycookin:"):
        item = ref.split(":", 1)[1]
        if item not in known_items:
            errors.append(f"{rel(path)}: {label} not registered/assets: {ref}")


def known_item_ids() -> set[str]:
    java = JAVA_ITEMS.read_text(encoding="utf-8")
    known = {p.stem for p in (ASSETS / "models/item").glob("*.json")}
    known |= set(re.findall(r'"([a-z0-9_]+)"', java))
    return known


def rel(path: Path) -> str:
    return str(path.relative_to(ROOT)).replace("\\", "/")


if __name__ == "__main__":
    raise SystemExit(main())
