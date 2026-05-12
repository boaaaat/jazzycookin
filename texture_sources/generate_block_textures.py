#!/usr/bin/env python3
"""Generate custom block textures for kitchen stations and source blocks."""

from __future__ import annotations

import json
from pathlib import Path
import sys

REPO_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO_ROOT))

from text_to_texture import extract_aliases, parse_grid, write_png

SIZE = 16
TEXTURE_DIR = REPO_ROOT / "src/main/resources/assets/jazzycookin/textures/block"
MODEL_DIR = REPO_ROOT / "src/main/resources/assets/jazzycookin/models/block"
BLOCKSTATE_DIR = REPO_ROOT / "src/main/resources/assets/jazzycookin/blockstates"
SOURCE_DIR = REPO_ROOT / "texture_sources/block"


BLOCKS = {
    "apple_sapling_young": ("plant", "6E9C3F", "3D6E2E", "8BC052", "5E4025"),
    "apple_sapling_ripe": ("plant", "6E9C3F", "3D6E2E", "C64535", "5E4025"),
    "tomato_vine": ("plant", "4F8A3A", "2F5E2A", "C64535", "6B4B2A"),
    "herb_bed": ("plant", "5B9B48", "2F6A35", "9FD26A", "4C3520"),
    "wheat_patch": ("plant", "B99535", "7A6227", "E2C15A", "5E4025"),
    "cabbage_patch": ("plant", "6FA85D", "3D7538", "9BD27A", "4C3520"),
    "onion_patch": ("plant", "A5B75D", "6F7D38", "D7C68A", "5E4025"),
    "forage_shrub": ("plant", "5B8E45", "345E32", "8A5DA8", "4C3520"),
    "chicken_coop": ("wood", "B0783E", "704722", "D9B27A", "8E5A2C"),
    "dairy_stall": ("wood", "B88A56", "6F4C2D", "E4D5B7", "8E5A2C"),
    "fishing_trap": ("wood", "8A6A42", "4E3A24", "B9965C", "5B7D87"),
    "pantry": ("wood", "9A6436", "5E3B20", "C08A52", "D7B071"),
    "cellar": ("stone", "6C665C", "3F3B35", "8A8174", "554B40"),
    "prep_table": ("wood", "B47A42", "70431F", "D0A064", "E0C188"),
    "canning_station": ("wood", "B0783E", "704722", "D8C08A", "8CC2C4"),
    "drying_rack": ("wood", "A0713E", "64411F", "D6B071", "C59A4A"),
    "fermentation_crock": ("stone", "7E5D47", "4D3429", "A9876C", "C7A66B"),
    "cooling_rack": ("metal", "8B9290", "535B5D", "B9C2BD", "D9DFDA"),
    "resting_board": ("wood", "A56D3C", "6A4324", "D39A5E", "B78249"),
    "plating_station": ("stone", "D8D2C0", "9B927F", "F1EBD8", "B6AA91"),
    "spice_grinder": ("stone", "7A6E60", "423A34", "A08B76", "C9923E"),
    "strainer": ("metal", "A7AFAE", "656F72", "D9DFDA", "8F9A9D"),
    "mixing_bowl": ("ceramic", "C36D3A", "7D3E24", "E6A46E", "F1D3B4"),
    "food_processor": ("metal", "D7D5C8", "737979", "B9C2BD", "6AA8C2"),
    "blender": ("glass", "92C8D6", "4F7885", "D5F0F4", "CF6D4F"),
    "juicer": ("glass", "9CCE7E", "587D42", "D7F2C4", "F0D35C"),
    "freeze_dryer": ("metal", "BFD7E2", "647B8A", "E7F5F8", "7BB7D8"),
    "freezer": ("metal", "B7D8E5", "5C7482", "E4F5FA", "7BB7D8"),
    "fridge": ("metal", "D8DDD8", "7B8581", "F2F5EF", "8FC3D2"),
    "microwave": ("metal", "8E9290", "4E5354", "C7CEC8", "3C5262"),
    "smoker": ("stone", "59483C", "2E2925", "8B705D", "C87538"),
    "steamer": ("metal", "AEB8B5", "626E70", "D6DFDB", "7FA8B0"),
    "stove": ("metal", "555B5D", "25292B", "9AA2A0", "D05C38"),
    "oven": ("stone", "746A5E", "3E3933", "A49684", "D08A45"),
}

STAGED_PLANTS = {
    "tomato_vine",
    "herb_bed",
    "wheat_patch",
    "cabbage_patch",
    "onion_patch",
    "forage_shrub",
}

CROSS_PLANTS = {
    "apple_sapling_young",
    "apple_sapling_ripe",
    *STAGED_PLANTS,
}


def checker_rows(base: str, dark: str, light: str, accent: str, style: str) -> list[str]:
    rows: list[str] = []
    for y in range(SIZE):
        row: list[str] = []
        for x in range(SIZE):
            token = base
            if style == "metal":
                if x in (0, 15) or y in (0, 15):
                    token = dark
                elif y in (4, 11) or x in (4, 11):
                    token = light if (x + y) % 2 == 0 else base
                elif (x + y) % 7 == 0:
                    token = accent
            elif style == "wood":
                token = dark if y in (0, 15) or x in (0, 15) else base
                if y in (4, 9, 13):
                    token = light
                if x in (3, 12) and 3 < y < 13:
                    token = dark
                if (x + 2 * y) % 11 == 0:
                    token = accent
            elif style == "plant":
                token = dark if (x + y) % 4 == 0 else base
                if y > 11:
                    token = accent
                if 4 <= x <= 11 and 3 <= y <= 10 and (x * y) % 5 == 0:
                    token = light
            elif style == "glass":
                token = dark if x in (0, 15) or y in (0, 15) else base
                if x in (3, 4) or y in (3, 4):
                    token = light
                if 6 <= x <= 10 and 8 <= y <= 12:
                    token = accent
            elif style == "ceramic":
                token = dark if x in (0, 15) or y in (0, 15) else base
                if 4 <= x <= 11 and 4 <= y <= 11:
                    token = light if (x + y) % 3 else base
                if x in (6, 9) and 5 <= y <= 10:
                    token = accent
            else:
                token = dark if (x + y) % 5 == 0 else base
                if x in (0, 15) or y in (0, 15):
                    token = dark
                if (x - y) % 6 == 0:
                    token = light
                if 5 <= x <= 10 and 5 <= y <= 10 and (x + y) % 4 == 0:
                    token = accent
            row.append(token)
        rows.append("".join(row))
    return rows


def plant_stage_rows(base: str, dark: str, light: str, accent: str, stage: int) -> list[str]:
    rows = [["." for _ in range(SIZE)] for _ in range(SIZE)]
    height = 4 + min(7, stage)
    spread = 1 + stage // 2
    center = SIZE // 2
    for y in range(SIZE - 2, SIZE - 2 - height, -1):
        rows[y][center] = dark
        if y % 2 == 0:
            for offset in range(1, spread + 1):
                if 0 <= center - offset < SIZE:
                    rows[y][center - offset] = base if offset < spread else light
                if 0 <= center + offset < SIZE:
                    rows[y][center + offset] = base if offset < spread else light
    if stage >= 5:
        for x, y in [(center - 2, 7), (center + 2, 8), (center, 6)]:
            if 0 <= y < SIZE:
                rows[y][x] = accent
    if stage >= 7:
        for x, y in [(center - 3, 9), (center + 3, 10)]:
            rows[y][x] = accent
    return ["".join(row) for row in rows]


def source_text(aliases: dict[str, str], rows: list[str]) -> str:
    alias_lines = [f"{key}={value}" for key, value in aliases.items()]
    return "\n".join(alias_lines + ["", *[" ".join(row) for row in rows], ""])


def write_block_texture(name: str, spec: tuple[str, str, str, str, str]) -> None:
    style, base, dark, light, accent = spec
    aliases = {"b": base, "d": dark, "l": light, "a": accent}
    rows = checker_rows("b", "d", "l", "a", style)
    text = source_text(aliases, rows)
    (SOURCE_DIR / f"{name}.txt").write_text(text, encoding="utf-8")
    parsed_aliases, grid_text = extract_aliases(text)
    width, height, pixels = parse_grid(grid_text, SIZE, SIZE, parsed_aliases)
    write_png(TEXTURE_DIR / f"{name}.png", width, height, pixels)


def write_staged_plant_textures(name: str, spec: tuple[str, str, str, str, str]) -> None:
    _style, base, dark, light, accent = spec
    aliases = {"b": base, "d": dark, "l": light, "a": accent, ".": "00000000"}
    for stage in range(8):
        stage_name = f"{name}_stage{stage}"
        text = source_text(aliases, plant_stage_rows("b", "d", "l", "a", stage))
        (SOURCE_DIR / f"{stage_name}.txt").write_text(text, encoding="utf-8")
        parsed_aliases, grid_text = extract_aliases(text)
        width, height, pixels = parse_grid(grid_text, SIZE, SIZE, parsed_aliases)
        write_png(TEXTURE_DIR / f"{stage_name}.png", width, height, pixels)
        write_cross_model(stage_name)


def write_model(name: str) -> None:
    model_path = MODEL_DIR / f"{name}.json"
    if not model_path.exists():
        return
    if name in CROSS_PLANTS:
        write_cross_model(name)
        return
    model = {
        "parent": "minecraft:block/cube_all",
        "textures": {
            "all": f"jazzycookin:block/{name}",
        },
    }
    model_path.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")


def write_cross_model(name: str) -> None:
    model_path = MODEL_DIR / f"{name}.json"
    model = {
        "parent": "minecraft:block/cross",
        "textures": {
            "cross": f"jazzycookin:block/{name}",
        },
    }
    model_path.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")


def write_staged_blockstate(name: str) -> None:
    blockstate_path = BLOCKSTATE_DIR / f"{name}.json"
    variants = {
        f"age={stage}": {
            "model": f"jazzycookin:block/{name}_stage{stage}",
        }
        for stage in range(8)
    }
    blockstate_path.write_text(json.dumps({"variants": variants}, indent=2) + "\n", encoding="utf-8")


def main() -> int:
    TEXTURE_DIR.mkdir(parents=True, exist_ok=True)
    SOURCE_DIR.mkdir(parents=True, exist_ok=True)
    for name, spec in BLOCKS.items():
        write_block_texture(name, spec)
        write_model(name)
        if name in STAGED_PLANTS:
            write_staged_plant_textures(name, spec)
            write_staged_blockstate(name)
    print("Generated block textures:", ", ".join(sorted(BLOCKS)))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
