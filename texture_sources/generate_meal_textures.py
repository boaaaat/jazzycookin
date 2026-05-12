#!/usr/bin/env python3
"""Generate custom meal item textures through text_to_texture.py helpers."""

from __future__ import annotations

import math
import json
from pathlib import Path
import sys

REPO_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO_ROOT))

from text_to_texture import extract_aliases, parse_grid, write_png

SIZE = 16
TEXTURE_DIR = REPO_ROOT / "src/main/resources/assets/jazzycookin/textures/item"
MODEL_DIR = REPO_ROOT / "src/main/resources/assets/jazzycookin/models/item"
SOURCE_DIR = REPO_ROOT / "texture_sources"

TRANSPARENT = "."
PLATE = "p"
RIM = "r"


MEALS = {
    "adobo_pork": ("b", "s", "c", "h", "8E3D24", "5A2A1B", "D0A15E", "D8D2B0"),
    "aloo_matar": ("b", "s", "c", "h", "C47A2A", "E0B64F", "78A95A", "6EA24E"),
    "chana_masala": ("b", "g", "h", "c", "C06A2A", "8D3F18", "F0D37A", "D9A035"),
    "black_beans_and_rice": ("b", "s", "c", "h", "E2D2A2", "332824", "6E4B34", "6CA85A"),
    "braised_beef": ("b", "s", "c", "h", "7A3424", "4A231A", "C48A4C", "D7BA73"),
    "browned_omelet": ("b", "s", "c", "h", "E5BD54", "9C5A28", "F4D984", "5C9D5A"),
    "butter_chicken": ("b", "s", "c", "h", "D8742D", "B64022", "F2D184", "67A65C"),
    "cabbage_rolls": ("b", "s", "c", "h", "79A95E", "4F7A3B", "B06B3A", "D9C17A"),
    "cabbage_sabzi": ("b", "s", "c", "h", "8DB85E", "C9852D", "E3C953", "4F8B3C"),
    "chicken_biryani": ("b", "s", "c", "h", "D9BB61", "A55832", "F1D782", "6B9D54"),
    "chicken_curry": ("b", "s", "c", "h", "B85C25", "DF8F34", "EBC27A", "5C9A55"),
    "chicken_fried_rice": ("b", "s", "c", "h", "E0C36E", "B78342", "F2D98E", "69A85A"),
    "chicken_noodle_soup": ("b", "s", "c", "h", "D49B3C", "F1D37A", "E7C35D", "72A158"),
    "chickpea_couscous": ("b", "s", "c", "h", "D7B45F", "B9873A", "E7CA83", "65A052"),
    "coconut_curry_noodles": ("b", "s", "c", "h", "E4C873", "C76E2C", "F3DFA6", "6AA05A"),
    "confetti_rice_with_fish": ("b", "s", "c", "h", "E4D294", "C84A3C", "D8E0D2", "6DAF58"),
    "couscous_bowl": ("b", "s", "c", "h", "D8B75F", "A86D35", "E7CF85", "6EA357"),
    "dal_tadka": ("b", "s", "c", "h", "D39B35", "A85026", "E2C45B", "58964A"),
    "egg_curry": ("b", "s", "c", "h", "C9652A", "B64022", "F1D786", "68A65C"),
    "falafel_plate": ("b", "s", "c", "h", "9C6A35", "D8C27A", "7DAA58", "5C9D5A"),
    "garlic_bread": ("b", "s", "c", "h", "D99A45", "F0D06A", "FFF0B0", "5C9D5A"),
    "potato_curry": ("b", "s", "c", "h", "C7792D", "E2BA62", "F0D58A", "59924D"),
    "lentil_soup": ("b", "s", "c", "h", "9B4B24", "C47638", "E1A14A", "6E9A55"),
    "hummus_plate": ("b", "s", "c", "h", "D5B86D", "B8944A", "8E6A35", "5C9E59"),
    "mac_and_cheese": ("b", "s", "c", "h", "E2AA35", "F0C85A", "D88E2E", "D7BA73"),
    "palak_paneer": ("b", "s", "c", "h", "4F8D44", "2F5E2F", "E8D9B4", "7CBF68"),
    "pan_seared_chicken": ("b", "s", "c", "h", "D9A15A", "9B5C2E", "F0D08A", "5E9B52"),
    "pancakes": ("b", "s", "c", "h", "C9853E", "E7B85F", "F6DA8F", "B45D2C"),
    "pasta_e_fagioli": ("b", "s", "c", "h", "B65A2A", "D59B53", "E6BF73", "6AA052"),
    "ratatouille": ("b", "s", "c", "h", "B84A35", "D6A03F", "6FAA55", "7B4A8F"),
    "rice_and_beans": ("b", "s", "c", "h", "E3D5AA", "5A3428", "C27A35", "6EA457"),
    "shakshuka": ("b", "s", "c", "h", "C44832", "8E2C22", "F0D580", "5FA255"),
    "spaghetti_pomodoro": ("b", "s", "c", "h", "E3C66D", "C63D2D", "F3D98A", "5FA055"),
    "vegetable_pulao": ("b", "s", "c", "h", "DCC56E", "A76D34", "E8D894", "69A85A"),
    "golden_rice": ("b", "s", "c", "h", "E2BE45", "F1D46A", "BB7F2A", "6E9B4D"),
    "apple_oatmeal": ("b", "s", "c", "h", "C9B486", "E0D0A0", "B94B3D", "F2E1B8"),
    "lemon_herb_fish": ("b", "s", "c", "h", "D8E0D2", "94B6BC", "F0D94F", "5C9D5A"),
    "tofu_stir_fry": ("b", "s", "c", "h", "E8D6B6", "C14F35", "6EAA5D", "E6C94B"),
}


def blank_canvas() -> list[list[str]]:
    return [[TRANSPARENT for _ in range(SIZE)] for _ in range(SIZE)]


def draw_plate(canvas: list[list[str]]) -> None:
    center = (SIZE - 1) / 2
    for y in range(SIZE):
        for x in range(SIZE):
            distance = math.dist((x, y), (center, center))
            if distance <= 7.1:
                canvas[y][x] = PLATE
            if distance <= 5.8:
                canvas[y][x] = RIM


def draw_round_meal(base: str, sauce: str, chunk: str, herb: str) -> list[str]:
    canvas = blank_canvas()
    draw_plate(canvas)
    center = (SIZE - 1) / 2
    for y in range(3, 13):
        for x in range(3, 13):
            distance = math.dist((x, y), (center, center))
            if distance <= 4.8:
                canvas[y][x] = base
            if distance <= 3.7 and (x + y) % 3 == 0:
                canvas[y][x] = sauce

    for x, y in [(5, 5), (9, 5), (6, 8), (10, 9), (4, 10), (8, 11)]:
        canvas[y][x] = chunk
        if x + 1 < SIZE:
            canvas[y][x + 1] = chunk

    for x, y in [(7, 4), (11, 6), (4, 7), (8, 8), (6, 11), (11, 11)]:
        canvas[y][x] = herb

    return ["".join(row) for row in canvas]


def french_toast_rows() -> tuple[dict[str, str], list[str]]:
    aliases = {
        PLATE: "ECE4D2",
        RIM: "B8A88C",
        "b": "C9853E",
        "t": "E5B35C",
        "s": "F4E0A4",
        "r": "B8A88C",
        "p": "ECE4D2",
    }
    rows = [
        ".....pppppp.....",
        "...ppprrrrppp...",
        "..pprrrrrrrrpp..",
        ".pprrbbbbbbrrp.",
        ".prrbbttttbbrr.",
        "prrbbttssttbbrr",
        "prrbbttttttbbrr",
        "prrbbttssttbbrr",
        "prrbbttttttbbrr",
        "prrbbttssttbbrr",
        ".prrbbttttbbrr.",
        ".pprrbbbbbbrrp.",
        "..pprrrrrrrrpp..",
        "...ppprrrrppp...",
        ".....pppppp.....",
        "................",
    ]
    return aliases, rows


def sandwich_rows() -> tuple[dict[str, str], list[str]]:
    aliases = {
        "p": "ECE4D2",
        "r": "B8A88C",
        "b": "D5A15D",
        "c": "F2D36B",
        "l": "5AA457",
        "t": "C84A3C",
        "s": "8B5A35",
    }
    rows = [
        "....pppppppp....",
        "..pprrrrrrrrpp..",
        ".prrbbbbbbbbrrp.",
        ".prbbccccccbbrp.",
        "prbbllllllllbbrp",
        "prbbttttttttbbrp",
        "prbbssssssssbbrp",
        "prbbbbbbbbbbbbrp",
        "prbbbbbbbbbbbbrp",
        "prbbssssssssbbrp",
        "prbbttttttttbbrp",
        "prbbllllllllbbrp",
        ".prbbccccccbbrp.",
        ".prrbbbbbbbbrrp.",
        "..pprrrrrrrrpp..",
        "....pppppppp....",
    ]
    return aliases, rows


def source_text(aliases: dict[str, str], rows: list[str]) -> str:
    alias_lines = [f"{key}={value}" for key, value in aliases.items()]
    return "\n".join(alias_lines + ["", *[" ".join(row) for row in rows], ""])


def write_texture(name: str, aliases: dict[str, str], rows: list[str]) -> None:
    rows = [row[:SIZE].ljust(SIZE, TRANSPARENT) for row in rows]
    text = source_text(aliases, rows)
    source_path = SOURCE_DIR / f"{name}.txt"
    source_path.write_text(text, encoding="utf-8")
    parsed_aliases, grid_text = extract_aliases(text)
    width, height, pixels = parse_grid(grid_text, SIZE, SIZE, parsed_aliases)
    write_png(TEXTURE_DIR / f"{name}.png", width, height, pixels)


def write_model(name: str) -> None:
    model_path = MODEL_DIR / f"{name}.json"
    if not model_path.exists():
        return
    model = {
        "parent": "minecraft:item/generated",
        "textures": {
            "layer0": f"jazzycookin:item/{name}",
        },
    }
    model_path.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")


def main() -> int:
    TEXTURE_DIR.mkdir(parents=True, exist_ok=True)
    SOURCE_DIR.mkdir(parents=True, exist_ok=True)

    plate_aliases = {
        PLATE: "ECE4D2",
        RIM: "B8A88C",
    }
    for name, (base_key, sauce_key, chunk_key, herb_key, base, sauce, chunk, herb) in MEALS.items():
        aliases = {
            **plate_aliases,
            base_key: base,
            sauce_key: sauce,
            chunk_key: chunk,
            herb_key: herb,
        }
        write_texture(name, aliases, draw_round_meal(base_key, sauce_key, chunk_key, herb_key))
        write_model(name)

    aliases, rows = french_toast_rows()
    write_texture("french_toast", aliases, rows)
    write_model("french_toast")
    aliases, rows = sandwich_rows()
    write_texture("sandwich_plate", aliases, rows)
    write_model("sandwich_plate")

    print("Generated meal textures:", ", ".join(sorted([*MEALS.keys(), "french_toast", "sandwich_plate"])))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
