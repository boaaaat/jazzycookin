#!/usr/bin/env python3
"""Generate additional dish schema JSON files for registered plated meals."""

from __future__ import annotations

import json
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
SCHEMA_DIR = REPO_ROOT / "src/main/resources/data/jazzycookin/dish_schema"

WEIGHTS = {
    "roles": 0.27,
    "composition": 0.12,
    "seasoning": 0.14,
    "cooking": 0.18,
    "texture": 0.11,
    "process": 0.07,
    "thermal": 0.05,
    "technique": 0.06,
    "presentation": 0.08,
}

MEAL_SCHEMAS = {
    "adobo_pork": (1301, "pan_dish", "ceramic_plate", [("protein", ["pork", "animal_protein"], 0.78), ("liquid", ["acidic", "sauce"], 0.42)], ["salt", "spice", "allium"]),
    "aloo_matar": (1302, "pan_dish", "ceramic_bowl", [("vegetable", ["starch", "vegetable"], 0.65), ("vegetable", ["vegetable", "plant_protein"], 0.42)], ["spice", "herb", "salt"]),
    "black_beans_and_rice": (1303, "plated", "ceramic_bowl", [("grain", ["rice", "grain"], 0.58), ("protein", ["legume", "plant_protein"], 0.58)], ["spice", "salt", "allium"]),
    "braised_beef": (1304, "pan_dish", "ceramic_plate", [("protein", ["beef", "animal_protein"], 0.82), ("liquid", ["sauce", "vegetable"], 0.38)], ["herb", "salt", "allium"]),
    "browned_omelet": (1305, "egg", "ceramic_plate", [("protein", ["egg", "animal_protein"], 0.8), ("dairy", ["dairy"], 0.22)], ["salt", "herb", "pepper"]),
    "cabbage_rolls": (1306, "plated", "ceramic_plate", [("vegetable", ["leafy_green", "vegetable"], 0.62), ("protein", ["protein", "legume", "animal_protein"], 0.45)], ["sauce", "salt", "herb"]),
    "cabbage_sabzi": (1307, "pan_dish", "ceramic_plate", [("vegetable", ["leafy_green", "vegetable"], 0.72), ("aromatic", ["allium", "aromatic"], 0.32)], ["spice", "salt", "herb"]),
    "chicken_biryani": (1308, "plated", "ceramic_bowl", [("grain", ["rice", "grain"], 0.55), ("protein", ["chicken", "animal_protein"], 0.65)], ["spice", "herb", "salt"]),
    "coconut_curry_noodles": (1309, "pan_dish", "ceramic_bowl", [("grain", ["pasta", "grain"], 0.45), ("liquid", ["coconut", "sauce"], 0.5)], ["spice", "herb", "acidic"]),
    "confetti_rice_with_fish": (1310, "plated", "ceramic_plate", [("grain", ["rice", "grain"], 0.52), ("protein", ["fish", "animal_protein"], 0.56), ("vegetable", ["vegetable", "pepper"], 0.32)], ["herb", "salt", "acidic"]),
    "couscous_bowl": (1311, "plated", "ceramic_bowl", [("grain", ["grain", "wheat"], 0.55), ("vegetable", ["vegetable"], 0.38)], ["herb", "acidic", "salt"]),
    "dal_tadka": (1312, "soup", "ceramic_bowl", [("protein", ["legume", "plant_protein"], 0.7), ("liquid", ["sauce", "vegetable"], 0.35)], ["spice", "fat", "salt"]),
    "egg_curry": (1313, "soup", "ceramic_bowl", [("protein", ["egg", "animal_protein"], 0.7), ("liquid", ["tomato", "sauce"], 0.45)], ["spice", "herb", "salt"]),
    "falafel_plate": (1314, "plated", "ceramic_plate", [("protein", ["legume", "plant_protein"], 0.7), ("vegetable", ["vegetable", "leafy_green"], 0.3)], ["herb", "acidic", "salt"]),
    "mac_and_cheese": (1315, "plated", "ceramic_bowl", [("grain", ["pasta", "grain"], 0.55), ("dairy", ["dairy"], 0.6)], ["salt", "fat", "spice"]),
    "palak_paneer": (1316, "pan_dish", "ceramic_bowl", [("vegetable", ["leafy_green", "vegetable"], 0.62), ("protein", ["dairy", "protein"], 0.55)], ["spice", "fat", "salt"]),
    "pancakes": (1317, "baked", "ceramic_plate", [("grain", ["flour", "wheat"], 0.58), ("fat", ["fat", "dairy"], 0.22)], ["sweetener", "syrup", "fruit"]),
    "pasta_e_fagioli": (1318, "soup", "ceramic_bowl", [("grain", ["pasta", "grain"], 0.42), ("protein", ["legume", "plant_protein"], 0.5), ("liquid", ["tomato", "sauce"], 0.35)], ["herb", "salt", "allium"]),
    "ratatouille": (1319, "pan_dish", "ceramic_bowl", [("vegetable", ["vegetable", "tomato"], 0.72), ("aromatic", ["allium", "aromatic"], 0.28)], ["herb", "oil", "salt"]),
    "rice_and_beans": (1320, "plated", "ceramic_bowl", [("grain", ["rice", "grain"], 0.55), ("protein", ["legume", "plant_protein"], 0.55)], ["spice", "salt", "allium"]),
    "shakshuka": (1321, "pan_dish", "ceramic_bowl", [("protein", ["egg", "animal_protein"], 0.58), ("liquid", ["tomato", "sauce"], 0.58)], ["spice", "pepper", "herb"]),
    "spaghetti_pomodoro": (1322, "plated", "ceramic_bowl", [("grain", ["pasta", "grain"], 0.6), ("liquid", ["tomato", "sauce"], 0.48)], ["herb", "salt", "fat"]),
    "vegetable_pulao": (1323, "plated", "ceramic_bowl", [("grain", ["rice", "grain"], 0.58), ("vegetable", ["vegetable"], 0.42)], ["spice", "herb", "salt"]),
    "patatas_bravas": (1324, "plated", "ceramic_plate", [("vegetable", ["starch", "vegetable"], 0.68), ("liquid", ["tomato", "sauce"], 0.35)], ["spice", "salt", "fat"]),
    "potato_gratin": (1325, "baked", "ceramic_plate", [("vegetable", ["starch", "vegetable"], 0.58), ("dairy", ["dairy", "fat"], 0.45)], ["salt", "herb", "allium"]),
    "fruit_juice": (1326, "plated", "glass_cup", [("fruit", ["fruit"], 0.72), ("liquid", ["acidic"], 0.28)], ["sweetener", "herb", "preserve"]),
    "soft_scrambled_eggs": (1327, "egg", "ceramic_plate", [("protein", ["egg", "animal_protein"], 0.82), ("fat", ["fat", "dairy"], 0.26)], ["salt", "herb", "pepper"]),
    "scrambled_eggs": (1328, "egg", "ceramic_plate", [("protein", ["egg", "animal_protein"], 0.82), ("fat", ["fat", "dairy"], 0.20)], ["salt", "herb", "pepper"]),
    "omelet": (1329, "egg", "ceramic_plate", [("protein", ["egg", "animal_protein"], 0.78), ("dairy", ["dairy"], 0.22)], ["salt", "herb", "vegetable"]),
    "burnt_eggs": (1330, "egg", "ceramic_plate", [("protein", ["egg", "animal_protein"], 0.78)], ["salt", "pepper", "spice"]),
    "pasta_tray_bake": (1331, "baked", "ceramic_plate", [("grain", ["pasta", "grain"], 0.55), ("dairy", ["dairy"], 0.32), ("liquid", ["tomato", "sauce"], 0.28)], ["herb", "salt", "fat"]),
    "hearty_stew": (1332, "soup", "ceramic_bowl", [("protein", ["protein", "animal_protein"], 0.55), ("vegetable", ["vegetable", "starch"], 0.45), ("liquid", ["sauce", "vegetable"], 0.35)], ["herb", "salt", "allium"]),
    "meat_platter": (1333, "plated", "ceramic_plate", [("protein", ["protein", "animal_protein"], 0.84), ("fat", ["fat"], 0.20)], ["salt", "herb", "spice"]),
    "dumpling_basket": (1334, "plated", "bamboo_tray", [("grain", ["flour", "wheat"], 0.44), ("protein", ["protein", "plant_protein", "animal_protein"], 0.44)], ["sauce", "salt", "herb"]),
    "smoothie": (1335, "plated", "glass_cup", [("fruit", ["fruit"], 0.58), ("liquid", ["dairy", "coconut"], 0.38)], ["sweetener", "herb", "preserve"]),
    "fried_chicken_basket": (1336, "pan_dish", "basket", [("protein", ["chicken", "animal_protein"], 0.72), ("grain", ["flour", "wheat"], 0.28)], ["salt", "spice", "fat"]),
    "freeze_dried_meal_pack": (1337, "generic", "tupperware", [("protein", ["protein", "plant_protein", "animal_protein"], 0.44), ("vegetable", ["vegetable", "fruit"], 0.38)], ["salt", "spice", "grain"]),
    "jam_toast": (1338, "plated", "ceramic_plate", [("grain", ["bread", "bread_loaf"], 0.62), ("fruit", ["fruit", "preserve"], 0.34)], ["sweetener", "fat", "dairy"]),
    "savory_pie": (1339, "baked", "ceramic_plate", [("grain", ["flour", "wheat"], 0.44), ("protein", ["protein", "animal_protein", "plant_protein"], 0.38), ("vegetable", ["vegetable"], 0.28)], ["salt", "herb", "fat"]),
    "fried_fish": (1340, "pan_dish", "ceramic_plate", [("protein", ["fish", "animal_protein"], 0.72), ("fat", ["fat", "oil"], 0.22)], ["salt", "acidic", "herb"]),
    "peanut_noodles": (1341, "plated", "ceramic_bowl", [("grain", ["pasta", "grain"], 0.52), ("protein", ["nut", "plant_protein"], 0.36)], ["sauce", "salt", "spice"]),
    "brownies": (1342, "baked", "ceramic_plate", [("grain", ["flour", "wheat"], 0.36), ("sweetener", ["sweetener", "chocolate"], 0.52), ("fat", ["fat", "dairy"], 0.24)], ["salt", "nut", "dairy"]),
    "cake": (1343, "baked", "ceramic_plate", [("grain", ["flour", "wheat"], 0.42), ("sweetener", ["sweetener"], 0.42), ("binder", ["egg", "dairy"], 0.24)], ["fruit", "chocolate", "fat"]),
    "glazed_chicken": (1344, "pan_dish", "ceramic_plate", [("protein", ["chicken", "animal_protein"], 0.72), ("sweetener", ["sweetener", "syrup"], 0.26)], ["salt", "spice", "acidic"]),
    "fried_jalapeno_bites": (1345, "pan_dish", "ceramic_plate", [("vegetable", ["pepper", "vegetable"], 0.62), ("grain", ["flour", "wheat"], 0.26)], ["salt", "dairy", "fat"]),
    "focaccia_pizza": (1346, "baked", "ceramic_plate", [("grain", ["bread", "wheat"], 0.52), ("liquid", ["tomato", "sauce"], 0.30), ("dairy", ["dairy"], 0.28)], ["herb", "oil", "salt"]),
    "soy_ginger_chicken": (1347, "pan_dish", "ceramic_plate", [("protein", ["chicken", "animal_protein"], 0.72), ("liquid", ["sauce", "acidic"], 0.30), ("aromatic", ["aromatic"], 0.20)], ["salt", "spice", "sweetener"]),
    "tomato_egg_skillet": (1348, "pan_dish", "ceramic_bowl", [("protein", ["egg", "animal_protein"], 0.55), ("liquid", ["tomato", "sauce"], 0.45)], ["salt", "pepper", "herb"]),
    "peanut_tofu_noodles": (1349, "plated", "ceramic_bowl", [("grain", ["pasta", "grain"], 0.44), ("protein", ["soy", "plant_protein"], 0.42), ("protein", ["nut"], 0.24)], ["sauce", "salt", "spice"]),
    "paneer_butter_masala": (1350, "pan_dish", "ceramic_bowl", [("protein", ["dairy", "protein"], 0.58), ("liquid", ["tomato", "sauce"], 0.44), ("fat", ["fat", "dairy"], 0.24)], ["spice", "salt", "herb"]),
    "matar_paneer": (1351, "pan_dish", "ceramic_bowl", [("protein", ["dairy", "protein"], 0.52), ("vegetable", ["vegetable", "plant_protein"], 0.40)], ["spice", "salt", "herb"]),
    "jeera_rice": (1352, "plated", "ceramic_bowl", [("grain", ["rice", "grain"], 0.72), ("spice", ["spice"], 0.20)], ["fat", "salt", "herb"]),
    "rajma_chawal": (1353, "plated", "ceramic_bowl", [("grain", ["rice", "grain"], 0.50), ("protein", ["legume", "plant_protein"], 0.56), ("liquid", ["tomato", "sauce"], 0.25)], ["spice", "salt", "allium"]),
    "saag_aloo": (1354, "pan_dish", "ceramic_bowl", [("vegetable", ["leafy_green", "vegetable"], 0.54), ("vegetable", ["starch", "vegetable"], 0.46)], ["spice", "salt", "fat"]),
    "chicken_noodle_soup": (1355, "soup", "ceramic_bowl", [("protein", ["chicken", "animal_protein"], 0.48), ("grain", ["pasta", "grain"], 0.34), ("liquid", ["sauce", "vegetable"], 0.44)], ["salt", "herb", "allium"]),
    "chickpea_couscous": (1356, "plated", "ceramic_bowl", [("grain", ["grain", "wheat"], 0.48), ("protein", ["legume", "plant_protein"], 0.46)], ["herb", "acidic", "salt"]),
}


def role(role: str, traits: list[str], weight: float) -> dict[str, object]:
    return {
        "role": role,
        "any_traits": traits,
        "weight": weight,
    }


def ingredient(role_name: str, traits: list[str]) -> dict[str, object]:
    return {
        "role": role_name,
        "any_traits": traits,
        "all_traits": [],
        "ideal_amount": 1.0,
        "min_amount": 0.5,
        "max_amount": 64.0,
        "unit": "count",
        "core": True,
        "measured_required": False,
    }


def target(minimum: float, maximum: float, softness: float) -> dict[str, float]:
    return {
        "min": minimum,
        "max": maximum,
        "softness": softness,
    }


def targets_for(category: str, required: list[tuple[str, list[str], float]], optional_traits: list[str]) -> dict[str, object]:
    all_traits = {trait for _role_name, traits, _weight in required for trait in traits} | set(optional_traits)
    soup_like = category == "soup"
    baked = category == "baked"
    protein_heavy = bool({"animal_protein", "plant_protein", "protein", "egg", "chicken", "fish", "pork", "beef"} & all_traits)
    dairy_heavy = "dairy" in all_traits
    starchy = bool({"grain", "rice", "pasta", "wheat", "starch", "flour"} & all_traits)
    vegetable_heavy = "vegetable" in all_traits or "leafy_green" in all_traits

    targets: dict[str, object] = {
        "water": target(0.46 if soup_like else 0.20, 0.86 if soup_like else 0.68, 0.18),
        "fat": target(0.04 if dairy_heavy else 0.0, 0.48 if dairy_heavy else 0.36, 0.16),
        "protein": target(0.12 if protein_heavy else 0.0, 0.66 if protein_heavy else 0.42, 0.18),
        "seasoning": target(0.06, 0.42, 0.16),
        "herb": target(0.0, 0.32, 0.14),
        "pepper": target(0.0, 0.34 if "pepper" in all_traits or "spice" in all_traits else 0.24, 0.14),
        "onion": target(0.0, 0.36 if "allium" in all_traits or "aromatic" in all_traits else 0.22, 0.14),
        "char_level": target(0.0, 0.12, 0.06),
        "fragmentation": target(0.18 if vegetable_heavy else 0.12, 0.70, 0.18),
        "cohesiveness": target(0.28 if starchy or soup_like else 0.20, 0.86, 0.18),
        "process_depth": target(2.0, 6.0, 1.2),
    }
    if dairy_heavy:
        targets["cheese"] = target(0.08, 0.55, 0.16)
    if category == "pan_dish":
        targets.update({
            "browning": target(0.08, 0.44, 0.14),
            "protein_set": target(0.36, 0.82, 0.16) if protein_heavy else target(0.0, 0.48, 0.18),
            "stir_count": target(1.0, 8.0, 2.0),
            "time_in_pan": target(80.0, 360.0, 80.0),
            "surface_temp_c": target(82.0, 190.0, 34.0),
            "core_temp_c": target(62.0, 98.0, 18.0),
        })
    elif soup_like:
        targets.update({
            "browning": target(0.0, 0.26, 0.12),
            "protein_set": target(0.22, 0.76, 0.20) if protein_heavy else target(0.0, 0.42, 0.16),
            "stir_count": target(2.0, 10.0, 2.5),
            "surface_temp_c": target(75.0, 105.0, 16.0),
            "core_temp_c": target(70.0, 100.0, 16.0),
        })
    elif baked:
        targets.update({
            "browning": target(0.14, 0.52, 0.14),
            "aeration": target(0.04, 0.46, 0.14),
            "surface_temp_c": target(90.0, 180.0, 28.0),
            "core_temp_c": target(76.0, 105.0, 14.0),
        })
    else:
        targets.update({
            "browning": target(0.0, 0.38, 0.16),
            "stir_count": target(0.0, 6.0, 2.0),
            "surface_temp_c": target(45.0, 120.0, 30.0),
            "core_temp_c": target(38.0, 100.0, 26.0),
        })
    return targets


def build_schema(
    key: str,
    preview_id: int,
    category: str,
    serving_item: str,
    required: list[tuple[str, list[str], float]],
    optional_traits: list[str],
) -> dict[str, object]:
    return {
        "key": key,
        "preview_id": preview_id,
        "result": f"jazzycookin:{key}",
        "category": category,
        "meal": True,
        "preview_threshold": 0.52,
        "finalize_threshold": 0.58,
        "desirability": 0.82,
        "required_roles": [role(role_name, traits, weight) for role_name, traits, weight in required],
        "optional_roles": [
            {
                "role": "garnish",
                "any_traits": optional_traits,
                "weight": 0.24,
            }
        ],
        "required_techniques": ["plated"],
        "serving_items": [f"jazzycookin:{serving_item}"],
        "targets": targets_for(category, required, optional_traits),
        "weights": WEIGHTS,
        "ingredients": [ingredient(role_name, traits) for role_name, traits, _weight in required],
        "steps": [
            {
                "id": "plated",
                "station": "plating_station",
                "technique": "plated",
                "prerequisites": [],
                "progress_target": 1.0,
            }
        ],
    }


def main() -> int:
    SCHEMA_DIR.mkdir(parents=True, exist_ok=True)
    for key, (preview_id, category, serving_item, required, optional_traits) in MEAL_SCHEMAS.items():
        path = SCHEMA_DIR / f"{key}.json"
        schema = build_schema(key, preview_id, category, serving_item, required, optional_traits)
        path.write_text(json.dumps(schema, indent=2) + "\n", encoding="utf-8")
    print("Generated additional meal schemas:", ", ".join(sorted(MEAL_SCHEMAS)))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
