#!/usr/bin/env python3
"""Generate block item, loot, recipe, and advancement JSON for source crops."""

from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/jazzycookin"
DATA = ROOT / "src/main/resources/data/jazzycookin"

SOURCES = {
    "root_vegetable_patch": {
        "pattern": ["CRC", " D ", " S "],
        "key": {
            "C": {"item": "minecraft:carrot"},
            "R": {"item": "minecraft:potato"},
            "D": {"item": "minecraft:dirt"},
            "S": {"item": "minecraft:stick"},
        },
    },
    "leafy_greens_bed": {
        "pattern": ["LLL", " D ", " S "],
        "key": {
            "L": {"item": "minecraft:wheat_seeds"},
            "D": {"item": "minecraft:dirt"},
            "S": {"item": "minecraft:stick"},
        },
    },
    "pepper_bush": {
        "pattern": ["RVR", " D ", " S "],
        "key": {
            "R": {"item": "minecraft:red_dye"},
            "V": {"item": "minecraft:vine"},
            "D": {"item": "minecraft:dirt"},
            "S": {"item": "minecraft:stick"},
        },
    },
    "pea_trellis": {
        "pattern": ["SWS", " V ", " S "],
        "key": {
            "S": {"item": "minecraft:stick"},
            "W": {"item": "minecraft:wheat_seeds"},
            "V": {"item": "minecraft:vine"},
        },
    },
    "citrus_sapling": {
        "pattern": [" Y ", " T ", " D "],
        "key": {
            "Y": {"item": "minecraft:yellow_dye"},
            "T": {"item": "minecraft:oak_sapling"},
            "D": {"item": "minecraft:dirt"},
        },
    },
}


def main() -> int:
    for folder in (
        ASSETS / "models/item",
        DATA / "loot_table/blocks",
        DATA / "recipe",
        DATA / "advancement/recipes",
    ):
        folder.mkdir(parents=True, exist_ok=True)

    for name, recipe in SOURCES.items():
        write_json(ASSETS / "models/item" / f"{name}.json", {"parent": f"jazzycookin:block/{name}"})
        write_json(DATA / "loot_table/blocks" / f"{name}.json", loot_table(name))
        write_json(DATA / "recipe" / f"{name}.json", shaped_recipe(name, recipe))
        write_json(DATA / "advancement/recipes" / f"{name}.json", advancement(name, recipe["key"]))

    print("Generated source resources:", ", ".join(sorted(SOURCES)))
    return 0


def loot_table(name: str) -> dict:
    return {
        "type": "minecraft:block",
        "pools": [
            {
                "rolls": 1,
                "entries": [{"type": "minecraft:item", "name": f"jazzycookin:{name}"}],
            }
        ],
    }


def shaped_recipe(name: str, recipe: dict) -> dict:
    return {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": recipe["pattern"],
        "key": recipe["key"],
        "result": {"id": f"jazzycookin:{name}", "count": 1},
    }


def advancement(name: str, key: dict) -> dict:
    criteria = {
        "has_the_recipe": {
            "trigger": "minecraft:recipe_unlocked",
            "conditions": {"recipe": f"jazzycookin:{name}"},
        }
    }
    requirements = ["has_the_recipe"]
    for symbol, ingredient in key.items():
        item = ingredient["item"]
        criterion = f"has_{item.replace(':', '_')}"
        criteria[criterion] = {
            "trigger": "minecraft:inventory_changed",
            "conditions": {"items": [{"items": item}]},
        }
        requirements.append(criterion)
    return {
        "parent": "minecraft:recipes/root",
        "criteria": criteria,
        "requirements": [requirements],
        "rewards": {"recipes": [f"jazzycookin:{name}"]},
    }


def write_json(path: Path, data: dict) -> None:
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    raise SystemExit(main())
