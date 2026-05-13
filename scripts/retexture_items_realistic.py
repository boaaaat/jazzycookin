#!/usr/bin/env python3
"""Retexture Jazzy Cookin' item PNGs with cohesive, depth-heavy palettes.

The pass is intentionally deterministic: it preserves each existing item's
silhouette and animation dimensions, then remaps opaque pixels to realistic
food/tool materials with top-left lighting, edge shadows, and subtle grain.
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Iterable
import colorsys
import hashlib
import math
import sys

from PIL import Image


REPO_ROOT = Path(__file__).resolve().parents[1]
ITEM_DIR = REPO_ROOT / "src/main/resources/assets/jazzycookin/textures/item"
FRAME = 16
MIN_COLORS = 10


@dataclass(frozen=True)
class Material:
    name: str
    colors: tuple[tuple[int, int, int], ...]
    roughness: float = 0.08


def rgb(hex_color: str) -> tuple[int, int, int]:
    value = hex_color.strip().lstrip("#")
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16))


def lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t


def mix_rgb(a: tuple[int, int, int], b: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    return tuple(round(lerp(a[i], b[i], t)) for i in range(3))


def ramp(stops: Iterable[str], steps: int = 18) -> tuple[tuple[int, int, int], ...]:
    stop_colors = [rgb(stop) for stop in stops]
    if len(stop_colors) < 2:
        raise ValueError("palette ramp needs at least two colors")
    out: list[tuple[int, int, int]] = []
    segments = len(stop_colors) - 1
    for i in range(steps):
        pos = i / max(steps - 1, 1)
        segment = min(int(pos * segments), segments - 1)
        local = (pos - segment / segments) * segments
        out.append(mix_rgb(stop_colors[segment], stop_colors[segment + 1], local))
    return tuple(out)


MATERIALS: dict[str, Material] = {
    "apple": Material("apple", ramp(["4f1713", "8d241d", "c94835", "e77855", "ffd19b"]), 0.10),
    "berry": Material("berry", ramp(["331123", "6e2138", "a83e55", "d96f76", "ffd0bd"]), 0.12),
    "bean_black": Material("bean_black", ramp(["11100d", "242019", "403322", "665036", "ad8750"]), 0.12),
    "bean_red": Material("bean_red", ramp(["381811", "6d2d1c", "9d4b2f", "c5794b", "e9b37a"]), 0.10),
    "bread": Material("bread", ramp(["5b3419", "8d5428", "bd7b38", "dfaa5f", "f4d58d"]), 0.12),
    "brownie": Material("brownie", ramp(["1f120d", "412315", "70401f", "9b6532", "d7a15c"]), 0.10),
    "cabbage": Material("cabbage", ramp(["203f24", "3f7136", "75a854", "a8c978", "dfe9ac"]), 0.12),
    "carrot": Material("carrot", ramp(["5c2712", "a8491e", "d8752a", "f1a64a", "ffd58b"]), 0.10),
    "ceramic": Material("ceramic", ramp(["7d827c", "a8ada4", "d2d3c8", "ece9db", "fff9ea"]), 0.04),
    "cheese": Material("cheese", ramp(["7a4c16", "b97923", "dda43a", "f1cb63", "fff0a4"]), 0.08),
    "chocolate": Material("chocolate", ramp(["20100b", "4b2515", "7b4523", "a9703e", "d8a868"]), 0.08),
    "citrus": Material("citrus", ramp(["706613", "a99b22", "d8ca3d", "f3e768", "fff6a8"]), 0.10),
    "dairy": Material("dairy", ramp(["b5a47e", "d5c59a", "eadfba", "fff1cf", "fff8df"]), 0.05),
    "dark_oil": Material("dark_oil", ramp(["15110d", "2c2116", "503620", "82552a", "b17b3a"]), 0.05),
    "dough": Material("dough", ramp(["8b7450", "b09464", "d0b887", "ecd9aa", "fff0cd"]), 0.10),
    "egg": Material("egg", ramp(["a76b26", "d39235", "ecc36a", "fff1bd", "fff8e1"]), 0.08),
    "fish": Material("fish", ramp(["5d7680", "8ca4aa", "c3cec7", "edf0df", "fff7e8"]), 0.07),
    "glass": Material("glass", ramp(["4d6570", "78939a", "aec5c7", "dce8e7", "ffffff"]), 0.03),
    "grain": Material("grain", ramp(["7b6136", "a68a50", "ccb06d", "ead596", "fff0be"]), 0.13),
    "green_herb": Material("green_herb", ramp(["17351e", "2e642d", "5d913e", "91bd64", "cde39c"]), 0.15),
    "green_pepper": Material("green_pepper", ramp(["18391f", "2c6b2e", "4f963c", "80bf5b", "c1e58e"]), 0.12),
    "honey": Material("honey", ramp(["6b3c0e", "a76919", "d99a2b", "f4c45a", "ffe28c"]), 0.04),
    "meat_cooked": Material("meat_cooked", ramp(["3c1810", "6d2d1b", "9d5030", "c27748", "e2a270"]), 0.12),
    "meat_raw": Material("meat_raw", ramp(["5a2020", "8e3a34", "c36a57", "e59a7a", "ffd0a6"]), 0.09),
    "metal": Material("metal", ramp(["30343a", "5d6469", "90999a", "c9d0ca", "f4f1e6"]), 0.02),
    "milk": Material("milk", ramp(["9ca7a1", "c5cec5", "e5e5d4", "fbf4dc", "ffffff"]), 0.03),
    "nut": Material("nut", ramp(["4b2a16", "7b4a25", "aa743e", "d09b61", "edc58d"]), 0.12),
    "oil": Material("oil", ramp(["4a4b16", "72711f", "9b942c", "c9bd48", "efe07a"]), 0.04),
    "onion": Material("onion", ramp(["6e5731", "9c7945", "c8a86d", "e6d4a4", "fff0c9"]), 0.09),
    "pasta": Material("pasta", ramp(["80632d", "aa8642", "d2ad61", "efd084", "fff1b0"]), 0.11),
    "pepper_red": Material("pepper_red", ramp(["501611", "8b231c", "c5442e", "e8754f", "ffc184"]), 0.11),
    "plate_shadow": Material("plate_shadow", ramp(["5b5b54", "888982", "babaae", "ded9c8", "fff5df"]), 0.03),
    "potato": Material("potato", ramp(["5d3d21", "8f6335", "b98b52", "d9b778", "f1d49c"]), 0.11),
    "powder_light": Material("powder_light", ramp(["9a907d", "beb39d", "ddd3ba", "f4ead4", "fff9eb"]), 0.12),
    "powder_spice": Material("powder_spice", ramp(["552416", "8a3d21", "bf6b2b", "dc9b3c", "f2c66b"]), 0.13),
    "purple": Material("purple", ramp(["301827", "60334b", "8d516c", "bf7b91", "edb2b9"]), 0.10),
    "rice": Material("rice", ramp(["8c7a4d", "b8a46d", "dcc98e", "f3e3b2", "fff4d2"]), 0.13),
    "sauce_brown": Material("sauce_brown", ramp(["2b140d", "5b2b18", "895028", "b4763f", "dba464"]), 0.08),
    "sauce_orange": Material("sauce_orange", ramp(["4a180e", "87301a", "bd5b28", "df8a3c", "f2c46b"]), 0.09),
    "sauce_red": Material("sauce_red", ramp(["4c120f", "84231b", "b9422e", "df6c48", "f7aa72"]), 0.09),
    "soup": Material("soup", ramp(["42210f", "75401c", "a96b2e", "d69b4c", "f0c878"]), 0.08),
    "soy": Material("soy", ramp(["130b08", "2b160f", "512819", "7f4626", "b07643"]), 0.05),
    "stone": Material("stone", ramp(["383833", "626159", "8d897c", "b9ae9a", "ddd0b5"]), 0.04),
    "tomato": Material("tomato", ramp(["4b140f", "84231b", "b93c2d", "df6a4a", "ffaf7f"]), 0.10),
    "vegetable": Material("vegetable", ramp(["1d3c20", "3e7131", "6f9e41", "a6bf65", "e1d992"]), 0.14),
    "water": Material("water", ramp(["4c6974", "71929d", "a8c1c4", "dae5dd", "fff6e5"]), 0.04),
    "wicker": Material("wicker", ramp(["4b2d18", "7a4d25", "ad7941", "d4a45f", "edca8e"]), 0.11),
    "wood": Material("wood", ramp(["3a2113", "68401f", "966235", "c28a52", "e4b878"]), 0.09),
}


def stable_noise(key: str, x: int, y: int, frame_y: int = 0) -> float:
    digest = hashlib.blake2b(f"{key}:{x}:{y}:{frame_y}".encode("utf-8"), digest_size=4).digest()
    return int.from_bytes(digest, "big") / 0xFFFFFFFF


def has_any(name: str, words: Iterable[str]) -> bool:
    return any(word in name for word in words)


def liquid_material(name: str) -> str:
    if "soy" in name or "worcestershire" in name or "balsamic" in name:
        return "soy"
    if "tomato" in name or "ketchup" in name or "hot_sauce" in name:
        return "sauce_red"
    if "honey" in name or "maple" in name or "syrup" in name or "agave" in name:
        return "honey"
    if "molasses" in name:
        return "sauce_brown"
    if "oil" in name:
        return "dark_oil" if has_any(name, ["burnt", "used", "dirty"]) else "oil"
    if "milk" in name or "cream" in name:
        return "milk"
    if "lemon" in name or "vinegar" in name or "brine" in name or "rose_water" in name:
        return "water"
    if "juice" in name or "smoothie" in name:
        return "berry" if has_any(name, ["berry", "mixed"]) else "citrus"
    return "soup"


def primary_material(name: str) -> str:
    ordered: list[tuple[tuple[str, ...], str]] = [
        (("knife", "cleaver", "spoon", "fork", "whisk", "strainer", "pot", "pan", "skillet", "scale"), "metal"),
        (("rolling_pin", "chopsticks"), "wood"),
        (("basket", "steamer_basket"), "wicker"),
        (("mortar", "pestle"), "stone"),
        (("plate", "bowl", "pie_tin", "paper_liner"), "ceramic"),
        (("glass", "jar", "bottle", "canning"), "glass"),
        (("apple", "jam", "jelly", "preserve"), "apple"),
        (("berry", "cranberr", "raisin"), "berry"),
        (("lemon",), "citrus"),
        (("tomato", "ketchup"), "tomato"),
        (("cabbage", "spinach", "palak", "saag"), "cabbage"),
        (("herb", "basil", "thyme", "rosemary", "parsley", "dill", "oregano", "mint", "sage"), "green_herb"),
        (("pepper", "jalapeno", "pickles"), "green_pepper"),
        (("carrot",), "carrot"),
        (("potato", "aloo", "patatas"), "potato"),
        (("onion", "shallot", "garlic", "ginger"), "onion"),
        (("almond", "pecan", "walnut", "peanut", "nut"), "nut"),
        (("black_beans",), "bean_black"),
        (("kidney", "beans", "chickpeas", "lentils", "dal", "chana", "rajma"), "bean_red"),
        (("fish",), "fish"),
        (("beef", "pork", "meat", "protein", "roast", "bacon"), "meat_cooked"),
        (("chicken",), "meat_cooked"),
        (("raw",), "meat_raw"),
        (("egg", "omelet"), "egg"),
        (("cheese", "paneer"), "cheese"),
        (("butter", "lard", "shortening"), "dairy"),
        (("milk", "cream", "mayonnaise"), "milk"),
        (("rice", "chawal", "pulao", "biryani"), "rice"),
        (("pasta", "spaghetti", "macaroni", "noodle", "ramen", "couscous"), "pasta"),
        (("quinoa", "oat", "semolina", "cornmeal"), "grain"),
        (("bread", "toast", "focaccia", "sandwich", "dough", "crust", "cracker", "chips"), "bread"),
        (("cake", "pancake", "pie", "cookie", "brownie", "batter"), "dough"),
        (("cocoa", "chocolate", "brownie"), "chocolate"),
        (("flour", "powder", "sugar", "salt", "soda", "tartar", "yeast", "starch"), "powder_light"),
        (("spice", "cumin", "curry_powder", "paprika", "turmeric", "chili", "pepper", "cinnamon", "cloves", "nutmeg", "seasoning"), "powder_spice"),
        (("oil", "vinegar", "sauce", "syrup", "honey", "molasses", "juice", "stock", "broth", "soup", "marinade"), "soup"),
    ]
    for keys, material in ordered:
        if has_any(name, keys):
            if material == "soup":
                return liquid_material(name)
            return material
    return "vegetable"


def components_for(name: str) -> tuple[str, ...]:
    plain_components = {
        "ceramic_bowl": ("ceramic",),
        "ceramic_plate": ("ceramic",),
        "glass_cup": ("glass",),
        "glass_jar": ("glass",),
        "pie_tin": ("metal",),
        "paper_liner": ("ceramic",),
        "stock_pot": ("metal", "wood"),
        "pot": ("metal", "wood"),
        "frying_pan": ("metal", "wood"),
        "frying_skillet": ("metal", "wood"),
        "saucepan": ("metal", "wood"),
        "basket": ("wicker",),
    }
    if name in plain_components:
        return plain_components[name]
    if has_any(name, ["knife", "cleaver"]):
        return ("metal", "wood")
    if has_any(name, ["strainer", "spoon", "fork", "whisk", "pot", "pan", "skillet", "scale"]):
        return ("metal", "wood")
    if has_any(name, ["glass", "jar", "canning_jar"]):
        return ("glass", liquid_material(name))
    if has_any(name, ["plate", "bowl", "plated", "hummus_plate", "meat_platter", "sandwich_plate"]):
        return ("ceramic", primary_material(name), "green_herb")
    if "basket" in name:
        return ("wicker", primary_material(name), "bread")
    if has_any(name, ["bottle", "oil", "vinegar", "sauce", "syrup", "juice", "milk", "cream", "stock", "broth", "smoothie", "marinade", "brine"]):
        return ("glass", liquid_material(name))
    if has_any(name, ["curry", "masala", "tadka", "stew", "soup", "ramen", "shakshuka", "ratatouille"]):
        return ("sauce_orange", "sauce_red", "green_herb", "rice")
    if has_any(name, ["rice", "chawal", "pulao", "biryani", "fried_rice", "confetti"]):
        return ("rice", "green_pepper", "carrot", "meat_cooked")
    if has_any(name, ["pasta", "spaghetti", "mac", "noodle"]):
        return ("pasta", liquid_material(name), "cheese", "green_herb")
    if has_any(name, ["sandwich", "toast", "pizza", "focaccia", "garlic_bread"]):
        return ("bread", "cheese", "tomato", "green_herb")
    if has_any(name, ["pie", "cake", "pancake", "brownie", "cookie"]):
        return ("bread", "dough", "chocolate", "berry")
    if has_any(name, ["prep", "base", "assembly", "filling", "blend", "mixture"]):
        return (primary_material(name), "green_herb", "onion", "tomato")
    return (primary_material(name),)


def frame_bounds(alpha: list[list[int]]) -> tuple[int, int, int, int] | None:
    xs: list[int] = []
    ys: list[int] = []
    for y, row in enumerate(alpha):
        for x, value in enumerate(row):
            if value:
                xs.append(x)
                ys.append(y)
    if not xs:
        return None
    return min(xs), min(ys), max(xs), max(ys)


def is_edge(alpha: list[list[int]], x: int, y: int) -> bool:
    for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
        if nx < 0 or ny < 0 or ny >= len(alpha) or nx >= len(alpha[0]) or alpha[ny][nx] == 0:
            return True
    return False


def choose_component(name: str, comps: tuple[str, ...], x: int, y: int, bounds: tuple[int, int, int, int], edge: bool, source: tuple[int, int, int]) -> str:
    if len(comps) == 1:
        return comps[0]

    min_x, min_y, max_x, max_y = bounds
    width = max(max_x - min_x, 1)
    height = max(max_y - min_y, 1)
    fx = (x - min_x) / width
    fy = (y - min_y) / height

    if comps[0] in {"ceramic", "glass", "wicker"} and (edge or fx < 0.12 or fx > 0.88 or fy < 0.10 or fy > 0.88):
        return comps[0]
    if comps[0] == "metal" and ("knife" in name or "cleaver" in name):
        return "wood" if (fy > 0.62 or fx < 0.28) else "metal"
    if comps[0] == "metal" and "whisk" not in name:
        return "wood" if fy > 0.72 and fx < 0.55 else "metal"
    if comps[0] == "glass":
        return comps[0] if edge or fy < 0.18 else comps[1]

    hue = colorsys.rgb_to_hsv(source[0] / 255, source[1] / 255, source[2] / 255)[0]
    n = stable_noise(name, x, y)
    if len(comps) >= 4:
        if hue < 0.08 or hue > 0.92:
            return "tomato" if "tomato" in comps else comps[1]
        if 0.18 < hue < 0.43:
            return "green_herb" if "green_herb" in comps else comps[min(2, len(comps) - 1)]
        if n > 0.86:
            return comps[-1]
        if n > 0.70:
            return comps[min(2, len(comps) - 1)]
    return comps[1 + int(n * (len(comps) - 1)) % (len(comps) - 1)]


def shade_index(
    name: str,
    material: Material,
    x: int,
    y: int,
    frame_y: int,
    bounds: tuple[int, int, int, int],
    luma_norm: float,
    edge: bool,
) -> int:
    min_x, min_y, max_x, max_y = bounds
    width = max(max_x - min_x, 1)
    height = max(max_y - min_y, 1)
    fx = (x - min_x) / width
    fy = (y - min_y) / height
    light = (1.0 - fx) * 0.18 + (1.0 - fy) * 0.22
    roundness = max(0.0, 1.0 - math.dist((fx, fy), (0.38, 0.32)) * 1.35) * 0.18
    grain = (stable_noise(material.name + name, x, y, frame_y) - 0.5) * material.roughness * 2.0
    shade = luma_norm * 0.56 + light + roundness + grain
    if edge:
        shade -= 0.18
    shade = max(0.0, min(1.0, shade))
    return min(len(material.colors) - 1, max(0, round(shade * (len(material.colors) - 1))))


def recolor_frame(im: Image.Image, frame_y: int, frame_h: int, name: str, out: Image.Image) -> None:
    pixels = im.load()
    out_pixels = out.load()
    alpha = [[pixels[x, frame_y + y][3] for x in range(im.width)] for y in range(frame_h)]
    bounds = frame_bounds(alpha)
    if bounds is None:
        return

    lumas: list[float] = []
    for y in range(frame_h):
        for x in range(im.width):
            r, g, b, a = pixels[x, frame_y + y]
            if a:
                lumas.append(0.2126 * r + 0.7152 * g + 0.0722 * b)
    low = min(lumas)
    high = max(lumas)
    span = max(high - low, 1.0)
    comps = components_for(name)

    for y in range(frame_h):
        for x in range(im.width):
            r, g, b, a = pixels[x, frame_y + y]
            if a == 0:
                out_pixels[x, frame_y + y] = (0, 0, 0, 0)
                continue
            luma_norm = ((0.2126 * r + 0.7152 * g + 0.0722 * b) - low) / span
            edge = is_edge(alpha, x, y)
            component = choose_component(name, comps, x, y, bounds, edge, (r, g, b))
            material = MATERIALS[component]
            index = shade_index(name, material, x, y, frame_y, bounds, luma_norm, edge)
            nr, ng, nb = material.colors[index]
            out_pixels[x, frame_y + y] = (nr, ng, nb, a)


def ensure_minimum_colors(im: Image.Image, name: str) -> None:
    pixels = im.load()
    opaque = [(x, y) for y in range(im.height) for x in range(im.width) if pixels[x, y][3] > 0]
    colors = {pixels[x, y] for x, y in opaque}
    if len(colors) >= MIN_COLORS or len(opaque) < MIN_COLORS:
        return

    material = MATERIALS[primary_material(name)]
    spread = max(1, len(opaque) // MIN_COLORS)
    for i in range(MIN_COLORS):
        x, y = opaque[min(i * spread, len(opaque) - 1)]
        r, g, b = material.colors[round(i * (len(material.colors) - 1) / (MIN_COLORS - 1))]
        a = pixels[x, y][3]
        pixels[x, y] = (r, g, b, a)


def retexture(path: Path) -> int:
    name = path.stem
    image = Image.open(path).convert("RGBA")
    out = Image.new("RGBA", image.size, (0, 0, 0, 0))
    frame_h = FRAME if image.height % FRAME == 0 else image.height
    for frame_y in range(0, image.height, frame_h):
        recolor_frame(image, frame_y, min(frame_h, image.height - frame_y), name, out)
    ensure_minimum_colors(out, name)
    out.save(path)
    return len({px for px in out.getdata() if px[3] > 0})


def main() -> int:
    if not ITEM_DIR.exists():
        print(f"Missing item texture directory: {ITEM_DIR}", file=sys.stderr)
        return 1

    failures: list[tuple[str, int]] = []
    count = 0
    for path in sorted(ITEM_DIR.glob("*.png")):
        color_count = retexture(path)
        count += 1
        if 0 < color_count < MIN_COLORS:
            failures.append((path.name, color_count))

    print(f"Retextured {count} item textures in {ITEM_DIR}")
    if failures:
        for filename, color_count in failures:
            print(f"{filename}: {color_count} opaque colors", file=sys.stderr)
        return 2
    print(f"All non-empty item textures have at least {MIN_COLORS} opaque colors.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
