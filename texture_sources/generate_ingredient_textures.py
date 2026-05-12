#!/usr/bin/env python3
"""Generate custom item textures for common cooking ingredients and tools."""

from __future__ import annotations

import json
import math
from pathlib import Path
import sys

REPO_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO_ROOT))

from text_to_texture import extract_aliases, parse_grid, write_png

SIZE = 16
TEXTURE_DIR = REPO_ROOT / "src/main/resources/assets/jazzycookin/textures/item"
MODEL_DIR = REPO_ROOT / "src/main/resources/assets/jazzycookin/models/item"
SOURCE_DIR = REPO_ROOT / "texture_sources/item"
TRANSPARENT = "."


ITEMS = {
    "tomatoes": ("round", "C93D32", "8B231D", "EF6B52", "4F8A3B"),
    "cabbage": ("leafy", "77AD5A", "3D7339", "A9D582", "D8EAA0"),
    "onions": ("round", "D7C78C", "9D7B4A", "F0E0AE", "6E5C35"),
    "carrots": ("root", "DE7A28", "9B4E1E", "F1A142", "4F8A3B"),
    "potatoes": ("round", "B98A55", "7A5735", "D9B377", "5E452D"),
    "garlic": ("bulb", "E7D9B7", "B49E73", "FFF0CF", "6E5C35"),
    "ginger": ("root", "C6924B", "8A6234", "E0B367", "6E5C35"),
    "spinach": ("leafy", "4B9B45", "246B31", "80C46A", "2F7A3A"),
    "green_peas": ("peas", "74AF4C", "3F7D35", "A7D66B", "2E6630"),
    "red_pepper": ("pepper", "C93B31", "7F211E", "F06C4D", "4F8A3B"),
    "jalapenos": ("pepper", "4E9B3D", "246B31", "7DC65D", "2E6630"),
    "apples": ("round", "C94738", "8C241F", "F07358", "4F8A3B"),
    "lemons": ("round", "E6D64E", "9C8F29", "FFF080", "6A9B42"),
    "basil": ("herb", "5BA847", "276A32", "91D36B", "3F853A"),
    "thyme": ("herb", "6FA25A", "365F35", "A0C978", "557A43"),
    "rosemary": ("herb", "4F8760", "2D5D43", "8DB18A", "355F55"),
    "parsley": ("herb", "55A24A", "2A6B35", "91CE70", "3F8E42"),
    "dill": ("herb", "7BAA4A", "4F7430", "B6D975", "638F38"),
    "oregano": ("herb", "6B8E4D", "3E5E35", "9BB875", "547541"),
    "mint": ("herb", "5FBF7A", "2E7A4A", "9BE3A8", "47A05E"),
    "sage": ("herb", "8EA078", "58684D", "BBC7A0", "6F8060"),
    "black_pepper": ("powder", "2B2725", "141312", "5F5650", "8B7A68"),
    "table_salt": ("powder", "EDE8D8", "BDB49F", "FFFFFF", "D7D0BB"),
    "kosher_salt": ("powder", "E8E1CF", "B8AD98", "FFFFFF", "D0C5AA"),
    "sea_salt": ("powder", "DDE8E4", "9DB7B4", "FFFFFF", "B7D0CE"),
    "paprika": ("powder", "C64A30", "7D261F", "F0804E", "A43A28"),
    "cumin": ("powder", "9A6530", "5E3B20", "C08A4A", "7A4C28"),
    "turmeric": ("powder", "D2A320", "8A681A", "F0CA3A", "B5841E"),
    "chili_powder": ("powder", "A73825", "6A1F18", "D45B34", "84281C"),
    "curry_powder": ("powder", "C99028", "805A1D", "E7B543", "A16D23"),
    "italian_seasoning": ("powder", "6C8F42", "3D5F30", "A7B96B", "557A35"),
    "cheese": ("wedge", "E9C34C", "B98428", "F7DA72", "D79A32"),
    "butter": ("wedge", "F2D978", "C7A84E", "FFF0A0", "E0C45E"),
    "eggs": ("egg", "EFE2C2", "B8A070", "FFFFFF", "E4C05A"),
    "chicken": ("protein", "DCA06F", "9B5A3B", "F0C090", "BE6D4B"),
    "fish_fillet": ("protein", "D7E0D8", "7EA0A8", "F1F6EF", "A8C5C9"),
    "tofu": ("cube", "E5D7B8", "B8A985", "F8EACA", "D0BF9A"),
    "rice": ("grain", "E8D9A8", "B8A46F", "FFF0C8", "D0BC82"),
    "lentils": ("grain", "A85A32", "6C3824", "D08A4A", "8A4B2C"),
    "chickpeas": ("grain", "C99B55", "8A6235", "E4BC78", "A97842"),
    "bread": ("loaf", "C9893D", "8B5728", "E3B05C", "F0CF7A"),
    "white_sugar": ("powder", "F4F1E8", "C8C0AE", "FFFFFF", "E4DDCE"),
    "all_purpose_flour": ("powder", "E7E0D0", "B9AD98", "FFF7E8", "D6C7AD"),
    "olive_oil": ("bottle", "B0A03A", "5F6B2A", "E0D66A", "6F8A3D"),
    "mayonnaise": ("jar", "E9DFB8", "B8A77D", "FFF1CF", "D0C49A"),
    "mustard": ("jar", "D4A72A", "8A641C", "F0CA3E", "B98924"),
    "ketchup": ("jar", "C93B31", "81211D", "F0674F", "A53028"),
    "hot_sauce": ("bottle", "C9412E", "762019", "F36B45", "D89A35"),
    "chef_knife": ("tool", "C7D0D0", "5B666B", "F1F5F2", "6B4528"),
    "whisk": ("tool", "B8C2C2", "5B666B", "E6EEEE", "6B4528"),
    "rolling_pin": ("tool", "B9844A", "6B4528", "D8A96A", "8A5A35"),
    "mortar_pestle": ("tool", "8D8676", "4E4A42", "B8AE9A", "6E6658"),
    "measuring_cup": ("tool", "B7DCE8", "5F8795", "E9FAFF", "7BA9B8"),
    "measuring_spoons": ("tool", "BBC4C4", "687276", "EEF2F0", "7D8588"),
    "kitchen_scale": ("tool", "C8CBC4", "646A69", "F0F1E8", "7F8B88"),
}


def blank_canvas() -> list[list[str]]:
    return [[TRANSPARENT for _ in range(SIZE)] for _ in range(SIZE)]


def set_pixel(canvas: list[list[str]], x: int, y: int, token: str) -> None:
    if 0 <= x < SIZE and 0 <= y < SIZE:
        canvas[y][x] = token


def draw_disc(canvas: list[list[str]], cx: float, cy: float, radius: float, fill: str, shade: str, light: str) -> None:
    for y in range(SIZE):
        for x in range(SIZE):
            distance = math.dist((x, y), (cx, cy))
            if distance <= radius:
                canvas[y][x] = fill
            if radius - 1.0 < distance <= radius:
                canvas[y][x] = shade
    set_pixel(canvas, int(cx - radius / 2), int(cy - radius / 2), light)


def draw_item(style: str) -> list[str]:
    canvas = blank_canvas()
    if style == "round":
        draw_disc(canvas, 7.5, 8.0, 5.0, "b", "d", "l")
        for x, y in [(7, 3), (8, 3), (9, 4), (6, 4)]:
            set_pixel(canvas, x, y, "a")
    elif style == "leafy" or style == "herb":
        for x, y in [(6, 4), (8, 4), (5, 6), (7, 6), (9, 6), (4, 8), (6, 8), (8, 8), (10, 8), (6, 10), (8, 10)]:
            draw_disc(canvas, x, y, 2.0 if style == "leafy" else 1.3, "b", "d", "l")
        for y in range(4, 12):
            set_pixel(canvas, 7, y, "a")
    elif style == "root" or style == "pepper":
        for y in range(4, 13):
            width = max(1, 5 - abs(y - 8) // 2)
            for x in range(8 - width // 2, 8 + width // 2 + 1):
                set_pixel(canvas, x, y, "b" if (x + y) % 3 else "l")
        for x, y in [(7, 3), (8, 3), (9, 3), (8, 2)]:
            set_pixel(canvas, x, y, "a")
    elif style == "bulb":
        draw_disc(canvas, 7.5, 8.5, 4.5, "b", "d", "l")
        for x in range(6, 10):
            set_pixel(canvas, x, 4, "a")
        for y in range(6, 12):
            set_pixel(canvas, 7, y, "d")
            set_pixel(canvas, 9, y, "d")
    elif style == "peas" or style == "grain":
        positions = [(5, 5), (8, 5), (11, 6), (6, 8), (9, 8), (4, 10), (8, 11), (12, 10)]
        for x, y in positions:
            draw_disc(canvas, x, y, 1.4, "b", "d", "l")
    elif style == "powder":
        for y in range(6, 13):
            for x in range(4, 12):
                if (x - 7.5) ** 2 / 16 + (y - 10) ** 2 / 9 <= 1:
                    set_pixel(canvas, x, y, "b" if (x + y) % 3 else "l")
        for x in range(5, 11):
            set_pixel(canvas, x, 12, "d")
    elif style == "wedge":
        for y in range(5, 12):
            for x in range(4, 13 - (y - 5) // 2):
                set_pixel(canvas, x, y, "b" if y not in (5, 11) else "d")
        for x, y in [(6, 7), (10, 8), (8, 10)]:
            set_pixel(canvas, x, y, "a")
    elif style == "egg":
        draw_disc(canvas, 6.0, 9.0, 3.5, "b", "d", "l")
        draw_disc(canvas, 10.0, 8.0, 3.3, "b", "d", "l")
        set_pixel(canvas, 10, 8, "a")
    elif style == "protein" or style == "cube":
        for y in range(5, 12):
            for x in range(4, 12):
                if abs(x - 8) + abs(y - 8) < 7:
                    set_pixel(canvas, x, y, "b" if (x + y) % 4 else "l")
        for x in range(5, 12):
            set_pixel(canvas, x, 5, "d")
            set_pixel(canvas, x, 12, "d")
    elif style == "loaf":
        for y in range(6, 12):
            for x in range(3, 13):
                set_pixel(canvas, x, y, "b" if y not in (6, 11) else "d")
        for x in range(5, 11):
            set_pixel(canvas, x, 5, "l")
    elif style == "bottle" or style == "jar":
        for y in range(4, 13):
            for x in range(5, 11):
                edge = x in (5, 10) or y in (4, 12)
                set_pixel(canvas, x, y, "d" if edge else "b")
        for x in range(6, 10):
            set_pixel(canvas, x, 3, "a")
            set_pixel(canvas, x, 7, "l")
    elif style == "tool":
        for i in range(3, 13):
            set_pixel(canvas, i, 15 - i, "l")
            set_pixel(canvas, i, 14 - i, "b")
        for x in range(4, 8):
            for y in range(11, 14):
                set_pixel(canvas, x, y, "a" if x in (4, 7) or y in (11, 13) else "d")
    return ["".join(row) for row in canvas]


def source_text(aliases: dict[str, str], rows: list[str]) -> str:
    alias_lines = [f"{key}={value}" for key, value in aliases.items()]
    return "\n".join(alias_lines + ["", *[" ".join(row) for row in rows], ""])


def write_item_texture(name: str, spec: tuple[str, str, str, str, str]) -> None:
    style, base, dark, light, accent = spec
    aliases = {"b": base, "d": dark, "l": light, "a": accent}
    text = source_text(aliases, draw_item(style))
    (SOURCE_DIR / f"{name}.txt").write_text(text, encoding="utf-8")
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
    for name, spec in ITEMS.items():
        write_item_texture(name, spec)
        write_model(name)
    print("Generated ingredient textures:", ", ".join(sorted(ITEMS)))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
