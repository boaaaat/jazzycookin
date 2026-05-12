#!/usr/bin/env python3
"""Repair empty item model JSON files with generated mod-owned textures."""

from __future__ import annotations

import json
from pathlib import Path
import sys

REPO_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO_ROOT / "texture_sources"))

from generate_ingredient_textures import draw_item, source_text
from text_to_texture import extract_aliases, parse_grid, write_png

SIZE = 16
TEXTURE_DIR = REPO_ROOT / "src/main/resources/assets/jazzycookin/textures/item"
MODEL_DIR = REPO_ROOT / "src/main/resources/assets/jazzycookin/models/item"
SOURCE_DIR = REPO_ROOT / "texture_sources/item"

PALETTES = {
    "powder": ("powder", "E8DDC8", "BBAA8E", "FFF3DC", "D4C2A5"),
    "spice": ("powder", "B86A2F", "743A20", "E39A45", "9A552A"),
    "liquid": ("bottle", "A66A35", "5D3B24", "D0924C", "86A85A"),
    "dairy": ("jar", "E7DEC4", "B8A77D", "FFF1D0", "D3C196"),
    "grain": ("grain", "C69B55", "846137", "E4BC78", "A97842"),
    "legume": ("grain", "8A4A2F", "4F2B20", "B97645", "6E3C28"),
    "protein": ("protein", "B97652", "7B4633", "D99A72", "9E5C3E"),
    "sweet": ("powder", "E2C273", "A47A34", "F4DC96", "C59645"),
    "snack": ("grain", "D0A34E", "855C28", "EAC875", "A97835"),
    "default": ("cube", "B8A06A", "705C3C", "D7C28A", "8F744A"),
}


def invalid_json(path: Path) -> bool:
    try:
        json.loads(path.read_text(encoding="utf-8"))
        return False
    except Exception:
        return True


def palette_for(name: str) -> tuple[str, str, str, str, str]:
    if any(part in name for part in ("flour", "sugar", "powder", "soda", "tartar", "salt", "meal")):
        return PALETTES["powder"]
    if any(part in name for part in ("cumin", "cinnamon", "cloves", "nutmeg", "seasoning", "matcha")):
        return PALETTES["spice"]
    if any(part in name for part in ("oil", "syrup", "vinegar", "sauce", "extract", "water", "nectar", "molasses")):
        return PALETTES["liquid"]
    if any(part in name for part in ("milk", "cream", "condensed", "lard", "shortening")):
        return PALETTES["dairy"]
    if any(part in name for part in ("rice", "oats", "pasta", "spaghetti", "macaroni", "ramen", "couscous", "quinoa", "semolina")):
        return PALETTES["grain"]
    if any(part in name for part in ("beans", "chickpeas", "lentils")):
        return PALETTES["legume"]
    if any(part in name for part in ("beef", "pork")):
        return PALETTES["protein"]
    if any(part in name for part in ("honey", "jam", "jelly", "candy", "caramel", "chocolate", "sprinkles", "raisins")):
        return PALETTES["sweet"]
    if any(part in name for part in ("chips", "cookies", "crackers", "nuts", "almonds", "walnuts", "pecans", "breadcrumbs")):
        return PALETTES["snack"]
    return PALETTES["default"]


def write_texture(name: str, spec: tuple[str, str, str, str, str]) -> None:
    style, base, dark, light, accent = spec
    aliases = {"b": base, "d": dark, "l": light, "a": accent}
    text = source_text(aliases, draw_item(style))
    (SOURCE_DIR / f"{name}.txt").write_text(text, encoding="utf-8")
    parsed_aliases, grid_text = extract_aliases(text)
    width, height, pixels = parse_grid(grid_text, SIZE, SIZE, parsed_aliases)
    write_png(TEXTURE_DIR / f"{name}.png", width, height, pixels)


def write_model(name: str) -> None:
    model = {
        "parent": "minecraft:item/generated",
        "textures": {
            "layer0": f"jazzycookin:item/{name}",
        },
    }
    (MODEL_DIR / f"{name}.json").write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")


def main() -> int:
    TEXTURE_DIR.mkdir(parents=True, exist_ok=True)
    SOURCE_DIR.mkdir(parents=True, exist_ok=True)
    repaired: list[str] = []
    for model_path in sorted(MODEL_DIR.glob("*.json")):
        if not invalid_json(model_path):
            continue
        name = model_path.stem
        write_texture(name, palette_for(name))
        write_model(name)
        repaired.append(name)
    print("Repaired empty item models:", ", ".join(repaired))
    print(f"Repaired count: {len(repaired)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
