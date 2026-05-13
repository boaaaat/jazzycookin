#!/usr/bin/env python3
"""Generate static, station-specific block textures for placed kitchen blocks."""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw


REPO_ROOT = Path(__file__).resolve().parents[1]
BLOCK_DIR = REPO_ROOT / "src/main/resources/assets/jazzycookin/textures/block"

STATIONS = {
    "prep_table": ("wood", "table"),
    "spice_grinder": ("stone", "grinder"),
    "strainer": ("steel", "mesh"),
    "mixing_bowl": ("clay", "bowl"),
    "microwave": ("dark_metal", "microwave"),
    "food_processor": ("steel", "processor"),
    "blender": ("glass", "blender"),
    "juicer": ("green_glass", "juicer"),
    "freeze_dryer": ("cold_metal", "dryer"),
    "canning_station": ("wood", "jars"),
    "drying_rack": ("wood", "rack"),
    "smoker": ("smoke", "smoker"),
    "fermentation_crock": ("crock", "crock"),
    "steamer": ("steel", "steamer"),
    "stove": ("dark_metal", "stove"),
    "oven": ("stone", "oven"),
    "cooling_rack": ("steel", "rack"),
    "resting_board": ("wood", "board"),
    "plating_station": ("ceramic", "plate"),
    "pantry": ("wood", "shelves"),
    "fridge": ("cold_metal", "fridge"),
    "freezer": ("cold_blue", "freezer"),
    "cellar": ("stone", "shelves"),
    "chicken_coop": ("wood", "coop"),
    "dairy_stall": ("wood", "stall"),
    "fishing_trap": ("wood", "trap"),
}

PALETTES = {
    "wood": ["#3b2415", "#65401f", "#946235", "#bd854c", "#e1b876"],
    "stone": ["#2f2d29", "#4e4a43", "#716a5f", "#9d9280", "#c7b79a"],
    "steel": ["#242a2d", "#525b5e", "#7f8a8b", "#b4bcba", "#e4e0d3"],
    "dark_metal": ["#141719", "#2b3033", "#555d60", "#8d9695", "#d0ccc0"],
    "cold_metal": ["#263840", "#526c78", "#82a4b1", "#bdd4da", "#edf5ee"],
    "cold_blue": ["#203645", "#426780", "#6e9fba", "#aad0df", "#e8f7ff"],
    "glass": ["#284651", "#4f7b88", "#82b4be", "#c3e3e3", "#f4fff9"],
    "green_glass": ["#2f4d34", "#5f884d", "#95bf73", "#d0e7ad", "#fff0a4"],
    "clay": ["#4b2418", "#7c3b22", "#b15f36", "#d9925d", "#f0c39b"],
    "crock": ["#34221a", "#5a3828", "#875d42", "#b58b68", "#dec39a"],
    "ceramic": ["#5c5a52", "#8e8b7e", "#bdb8a5", "#e3dcc5", "#fff5df"],
    "smoke": ["#1d1b1a", "#37302d", "#5c4b41", "#8a6b55", "#c18455"],
}


def hex_rgb(value: str) -> tuple[int, int, int]:
    value = value.lstrip("#")
    return int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16)


def color(palette: list[str], index: int) -> tuple[int, int, int, int]:
    return (*hex_rgb(palette[max(0, min(len(palette) - 1, index))]), 255)


def base_tile(palette_name: str) -> Image.Image:
    palette = PALETTES[palette_name]
    image = Image.new("RGBA", (16, 16), color(palette, 2))
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, 15, 15), outline=color(palette, 0))
    draw.rectangle((1, 1, 14, 14), outline=color(palette, 1))
    draw.line((2, 2, 13, 2), fill=color(palette, 3))
    for y in (5, 10, 13):
        draw.line((2, y, 13, y), fill=color(palette, 1))
    for x in (4, 11):
        draw.point((x, 4), fill=color(palette, 3))
        draw.point((x, 8), fill=color(palette, 0))
        draw.point((x, 12), fill=color(palette, 3))
    return image


def draw_station_details(image: Image.Image, palette_name: str, motif: str) -> None:
    palette = PALETTES[palette_name]
    draw = ImageDraw.Draw(image)
    dark = color(palette, 0)
    mid = color(palette, 1)
    base = color(palette, 2)
    light = color(palette, 3)
    hi = color(palette, 4)

    if motif in {"table", "board"}:
        draw.rectangle((2, 3, 13, 10), fill=color(PALETTES["wood"], 2), outline=color(PALETTES["wood"], 0))
        draw.line((3, 5, 12, 5), fill=color(PALETTES["wood"], 3))
        draw.line((3, 8, 12, 8), fill=color(PALETTES["wood"], 1))
        draw.rectangle((4, 11, 5, 14), fill=color(PALETTES["wood"], 0))
        draw.rectangle((10, 11, 11, 14), fill=color(PALETTES["wood"], 0))
    elif motif == "grinder":
        draw.ellipse((4, 3, 11, 10), fill=base, outline=dark)
        draw.rectangle((6, 1, 9, 4), fill=light, outline=mid)
        draw.rectangle((5, 10, 10, 13), fill=dark)
        draw.point((8, 6), fill=hi)
    elif motif == "mesh":
        draw.rectangle((3, 3, 12, 12), outline=dark, fill=mid)
        for x in range(4, 12, 2):
            draw.line((x, 4, x, 11), fill=hi)
        for y in range(4, 12, 2):
            draw.line((4, y, 11, y), fill=dark)
    elif motif == "bowl":
        draw.ellipse((3, 5, 12, 13), fill=dark)
        draw.ellipse((4, 3, 11, 10), fill=light, outline=dark)
        draw.ellipse((5, 4, 10, 8), fill=mid)
    elif motif == "microwave":
        draw.rectangle((2, 4, 13, 12), fill=dark, outline=light)
        draw.rectangle((3, 5, 9, 10), fill=color(PALETTES["glass"], 1), outline=mid)
        draw.rectangle((11, 5, 12, 6), fill=hi)
        draw.rectangle((11, 8, 12, 9), fill=mid)
    elif motif == "processor":
        draw.rectangle((4, 5, 11, 12), fill=light, outline=dark)
        draw.rectangle((5, 2, 10, 6), fill=color(PALETTES["glass"], 2), outline=mid)
        draw.rectangle((6, 12, 9, 13), fill=dark)
        draw.point((8, 8), fill=hi)
    elif motif == "blender":
        draw.rectangle((5, 2, 10, 9), fill=color(PALETTES["glass"], 2), outline=color(PALETTES["glass"], 0))
        draw.polygon([(6, 9), (10, 9), (12, 13), (4, 13)], fill=dark)
        draw.rectangle((6, 11, 9, 12), fill=mid)
        draw.line((6, 3, 8, 3), fill=hi)
    elif motif == "juicer":
        draw.pieslice((4, 3, 11, 10), 180, 360, fill=color(PALETTES["green_glass"], 3), outline=dark)
        draw.rectangle((5, 8, 11, 13), fill=mid, outline=dark)
        draw.rectangle((10, 6, 13, 7), fill=hi)
    elif motif == "dryer":
        draw.rectangle((3, 3, 12, 13), fill=mid, outline=dark)
        for y in (5, 8, 11):
            draw.line((4, y, 11, y), fill=hi)
        draw.rectangle((5, 1, 10, 3), fill=light)
    elif motif == "jars":
        draw.rectangle((2, 3, 13, 12), fill=color(PALETTES["wood"], 1), outline=color(PALETTES["wood"], 0))
        for x in (4, 7, 10):
            draw.rectangle((x, 5, x + 1, 10), fill=color(PALETTES["glass"], 3), outline=color(PALETTES["glass"], 0))
            draw.point((x, 7), fill=color(PALETTES["apple"], 2) if "apple" in PALETTES else (190, 70, 45, 255))
    elif motif == "rack":
        for y in (4, 7, 10, 13):
            draw.line((2, y, 13, y), fill=light)
        for x in (4, 8, 12):
            draw.line((x, 3, x, 14), fill=dark)
    elif motif == "smoker":
        draw.rectangle((3, 3, 12, 13), fill=mid, outline=dark)
        draw.rectangle((5, 7, 10, 12), fill=dark)
        draw.rectangle((6, 1, 9, 3), fill=light)
        draw.line((6, 10, 9, 10), fill=color(PALETTES["smoke"], 4))
    elif motif == "crock":
        draw.ellipse((3, 3, 12, 13), fill=base, outline=dark)
        draw.rectangle((5, 2, 10, 5), fill=light, outline=mid)
        draw.arc((4, 5, 11, 12), 0, 180, fill=hi)
    elif motif == "steamer":
        draw.rectangle((3, 5, 12, 13), fill=mid, outline=dark)
        for y in (6, 8, 10):
            draw.line((4, y, 11, y), fill=hi)
        for x in (5, 8, 11):
            draw.point((x, 4), fill=color(PALETTES["glass"], 4))
            draw.point((x - 1, 3), fill=color(PALETTES["glass"], 3))
    elif motif == "stove":
        draw.rectangle((2, 3, 13, 13), fill=mid, outline=dark)
        for x, y in ((5, 6), (10, 6), (5, 10), (10, 10)):
            draw.ellipse((x - 2, y - 2, x + 1, y + 1), outline=dark, fill=color(PALETTES["smoke"], 1))
        draw.rectangle((4, 1, 11, 3), fill=light)
    elif motif == "oven":
        draw.rectangle((2, 3, 13, 13), fill=mid, outline=dark)
        draw.rectangle((4, 5, 11, 11), fill=dark, outline=light)
        draw.line((5, 9, 10, 9), fill=color(PALETTES["smoke"], 4))
        draw.rectangle((5, 2, 10, 3), fill=hi)
    elif motif == "plate":
        draw.rectangle((2, 3, 13, 12), fill=mid, outline=dark)
        draw.ellipse((4, 4, 11, 11), fill=hi, outline=dark)
        draw.ellipse((6, 6, 9, 9), outline=mid)
    elif motif in {"shelves", "fridge", "freezer"}:
        draw.rectangle((3, 2, 12, 13), fill=mid, outline=dark)
        draw.line((8, 3, 8, 12), fill=dark)
        draw.rectangle((4, 4, 7, 6), fill=light)
        draw.rectangle((9, 7, 11, 10), fill=light)
        draw.point((7, 8), fill=hi)
    elif motif in {"coop", "stall", "trap"}:
        draw.rectangle((2, 4, 13, 13), fill=color(PALETTES["wood"], 2), outline=color(PALETTES["wood"], 0))
        draw.polygon([(2, 4), (8, 1), (13, 4)], fill=color(PALETTES["wood"], 3), outline=color(PALETTES["wood"], 0))
        for x in (5, 8, 11):
            draw.line((x, 5, x, 12), fill=color(PALETTES["wood"], 0))


def write_texture(name: str, palette_name: str, motif: str) -> None:
    image = base_tile(palette_name)
    draw_station_details(image, palette_name, motif)
    image.save(BLOCK_DIR / f"{name}.png")


def main() -> int:
    BLOCK_DIR.mkdir(parents=True, exist_ok=True)
    for name, (palette, motif) in STATIONS.items():
        write_texture(name, palette, motif)
        mcmeta = BLOCK_DIR / f"{name}.png.mcmeta"
        if mcmeta.exists():
            mcmeta.unlink()
    print(f"Retextured {len(STATIONS)} placed block textures as static 16x16 PNGs.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
