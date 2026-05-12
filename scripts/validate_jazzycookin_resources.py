#!/usr/bin/env python3
"""Validate Jazzy Cookin resource/data pack consistency.

This intentionally avoids Gradle so resource edits can be checked even in
sandboxes where Java zip filesystem access prevents NeoForge compilation.
"""

from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src/main/resources"
ASSETS = RESOURCES / "assets/jazzycookin"
DATA = RESOURCES / "data/jazzycookin"
JAVA_ITEMS = ROOT / "src/main/java/com/boaat/jazzy_cookin/registry/JazzyItems.java"
JAVA_BLOCKS = ROOT / "src/main/java/com/boaat/jazzy_cookin/registry/JazzyBlocks.java"
JAVA_SOURCE_PROFILES = ROOT / "src/main/java/com/boaat/jazzy_cookin/kitchen/KitchenSourceProfile.java"
JAVA_FOOD_PROFILES = ROOT / "src/main/java/com/boaat/jazzy_cookin/kitchen/sim/FoodMaterialProfiles.java"
JAVA_STATION_BLOCK = ROOT / "src/main/java/com/boaat/jazzy_cookin/block/KitchenStationBlock.java"
JAVA_STATION_ENTITY = ROOT / "src/main/java/com/boaat/jazzy_cookin/block/entity/KitchenStationBlockEntity.java"
JAVA_GAMETESTS = ROOT / "src/main/java/com/boaat/jazzy_cookin/gametest/KitchenGameTests.java"
JAVA_TOOL_PROFILE = ROOT / "src/main/java/com/boaat/jazzy_cookin/kitchen/ToolProfile.java"

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
    validate_redstone_automation(errors)
    validate_schemas(parsed_json, errors)
    validate_tool_contract(parsed_json, errors)
    validate_recipe_simulation_depth(parsed_json, errors)
    validate_sandwich_contract(parsed_json, errors)
    validate_meal_schema_coverage(errors)
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
            if path.read_bytes()[:8] != png_signature:
                errors.append(f"{rel(path)}: invalid PNG signature")
        except OSError as exc:
            errors.append(f"{rel(path)}: cannot read PNG: {exc}")


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
