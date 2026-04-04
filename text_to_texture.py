#!/usr/bin/env python3
"""Turn text color data into a Minecraft-style texture PNG.

Examples:
    python3 text_to_texture.py "F54927,C7C70C,D6D629,18C4DE,..."

    python3 text_to_texture.py @apple_pixels.txt --width 16 --output apple.png

    python3 text_to_texture.py "
        00000000,00000000,00000000,00000000;
        00000000,F54927,F54927,00000000;
        00000000,C7C70C,D6D629,00000000;
        00000000,00000000,00000000,00000000
    " --width 4 --preview-scale 32

Input rules:
    - Colors can be RGB or RGBA hex: F54927, #F54927, F54927FF, FFA, FFAA
    - Transparent cells: transparent, none, clear, _, ., -
    - Cells can be separated by commas, spaces, tabs, or newlines
    - Use ';' or line breaks to force row boundaries
    - Repeat syntax is supported: F54927*8, transparent*16
    - Palette aliases are supported on their own lines: pan=5B5B63
"""

from __future__ import annotations

import argparse
import binascii
import re
import struct
import sys
import zlib
from datetime import datetime
from pathlib import Path


DEFAULT_WIDTH = 16
DEFAULT_OUTPUT_DIR = Path("generated_textures")
TRANSPARENT_TOKENS = {"transparent", "none", "clear", "_", ".", "-", "x"}
ROW_SPLIT_RE = re.compile(r"[;\n]+")
CELL_SPLIT_RE = re.compile(r"[\s,]+")
REPEAT_RE = re.compile(r"^(.*?)(?:\*(\d+))?$")
ALIAS_RE = re.compile(r"^([A-Za-z0-9_.-]+)\s*=\s*(\S+)\s*$")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Convert text color data into a texture PNG.",
    )
    parser.add_argument(
        "pixels",
        nargs="?",
        help="Pixel data, @path/to/file.txt, or omit to read from stdin.",
    )
    parser.add_argument(
        "--width",
        type=int,
        default=DEFAULT_WIDTH,
        help=f"Texture width in pixels. Default: {DEFAULT_WIDTH}",
    )
    parser.add_argument(
        "--height",
        type=int,
        default=0,
        help="Texture height in pixels. Default: infer from input",
    )
    parser.add_argument(
        "--output",
        default="",
        help="Output PNG path or directory. Default: generated_textures/",
    )
    parser.add_argument(
        "--preview-scale",
        type=int,
        default=16,
        help="Nearest-neighbor preview scale. Set 0 or 1 to skip preview. Default: 16",
    )
    parser.add_argument(
        "--no-preview",
        action="store_true",
        help="Skip saving the upscaled preview image.",
    )
    parser.add_argument(
        "--name",
        default="texture",
        help="Base filename when --output is a directory or omitted. Default: texture",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()

    if args.width <= 0:
        print("--width must be greater than 0", file=sys.stderr)
        return 1
    if args.height < 0:
        print("--height cannot be negative", file=sys.stderr)
        return 1
    if args.preview_scale < 0:
        print("--preview-scale cannot be negative", file=sys.stderr)
        return 1

    try:
        source_text = load_source_text(args.pixels)
        aliases, grid_text = extract_aliases(source_text)
        width, height, pixels = parse_grid(grid_text, args.width, args.height, aliases)
    except ValueError as error:
        print(f"Input error: {error}", file=sys.stderr)
        return 1
    except OSError as error:
        print(f"File error: {error}", file=sys.stderr)
        return 1

    texture_path = choose_output_path(args.output, args.name, width, height)
    texture_path.parent.mkdir(parents=True, exist_ok=True)
    write_png(texture_path, width, height, pixels)

    print(f"Saved texture: {texture_path.resolve()}")
    print(f"Texture size: {width}x{height}")

    if not args.no_preview and args.preview_scale > 1:
        preview_path = texture_path.with_name(f"{texture_path.stem}_preview.png")
        scaled_pixels = make_preview_pixels(pixels, width, height, args.preview_scale)
        write_png(
            preview_path,
            width * args.preview_scale,
            height * args.preview_scale,
            scaled_pixels,
        )
        print(f"Saved preview: {preview_path.resolve()}")

    return 0


def load_source_text(pixels_arg: str | None) -> str:
    if pixels_arg is None:
        text = sys.stdin.read()
        if not text.strip():
            raise ValueError("no pixel data provided")
        return text

    if pixels_arg.startswith("@"):
        path = Path(pixels_arg[1:])
        return path.read_text(encoding="utf-8")

    return pixels_arg


def extract_aliases(text: str) -> tuple[dict[str, tuple[int, int, int, int]], str]:
    aliases: dict[str, tuple[int, int, int, int]] = {}
    body_lines: list[str] = []

    for raw_line in text.splitlines():
        line = raw_line.split("//", 1)[0].strip()
        if not line:
            continue

        match = ALIAS_RE.fullmatch(line)
        if match:
            key = match.group(1).lower()
            aliases[key] = parse_literal_color(match.group(2))
            continue

        body_lines.append(line)

    if body_lines:
        return aliases, "\n".join(body_lines)
    return aliases, text


def parse_grid(
    text: str,
    width: int,
    height: int,
    aliases: dict[str, tuple[int, int, int, int]],
) -> tuple[int, int, list[tuple[int, int, int, int]]]:
    raw_rows = [row.strip() for row in ROW_SPLIT_RE.split(text) if row.strip()]

    if raw_rows:
        row_tokens = [expand_row_tokens(row) for row in raw_rows]
        if len(row_tokens) > 1:
            inferred_width = len(row_tokens[0])
            if inferred_width == 0:
                raise ValueError("rows cannot be empty")
            for index, row in enumerate(row_tokens, start=1):
                if len(row) != inferred_width:
                    raise ValueError(
                        f"row {index} has {len(row)} cells, expected {inferred_width}"
                    )
            if width != inferred_width and text_has_explicit_rows(text):
                raise ValueError(
                    f"--width={width} does not match explicit row width {inferred_width}"
                )
            width = inferred_width
            if height and height != len(row_tokens):
                raise ValueError(
                    f"--height={height} does not match explicit row count {len(row_tokens)}"
                )
            height = len(row_tokens)
            flat_tokens = [token for row in row_tokens for token in row]
            return width, height, [parse_color(token, aliases) for token in flat_tokens]

    flat_tokens = expand_row_tokens(text)
    if not flat_tokens:
        raise ValueError("no pixel tokens found")

    if height == 0:
        if len(flat_tokens) % width != 0:
            raise ValueError(
                f"{len(flat_tokens)} cells do not divide evenly into width {width}; "
                "set --height explicitly or fix the input"
            )
        height = len(flat_tokens) // width

    expected_cells = width * height
    if len(flat_tokens) != expected_cells:
        raise ValueError(
            f"expected {expected_cells} cells for a {width}x{height} texture, "
            f"got {len(flat_tokens)}"
        )

    return width, height, [parse_color(token, aliases) for token in flat_tokens]


def text_has_explicit_rows(text: str) -> bool:
    return ";" in text or "\n" in text


def expand_row_tokens(text: str) -> list[str]:
    tokens = [token for token in CELL_SPLIT_RE.split(text.strip()) if token]
    expanded: list[str] = []
    for token in tokens:
        match = REPEAT_RE.fullmatch(token)
        if not match:
            raise ValueError(f"bad token: {token}")
        value = match.group(1).strip()
        repeat = int(match.group(2) or "1")
        if repeat <= 0:
            raise ValueError(f"repeat must be greater than 0: {token}")
        if not value:
            raise ValueError(f"missing color before repeat: {token}")
        expanded.extend([value] * repeat)
    return expanded


def parse_color(
    token: str,
    aliases: dict[str, tuple[int, int, int, int]],
) -> tuple[int, int, int, int]:
    cleaned = token.strip().lower()
    if cleaned in aliases:
        return aliases[cleaned]
    return parse_literal_color(cleaned)


def parse_literal_color(token: str) -> tuple[int, int, int, int]:
    cleaned = token.strip().lower()
    if cleaned in TRANSPARENT_TOKENS:
        return (0, 0, 0, 0)

    if cleaned.startswith("#"):
        cleaned = cleaned[1:]

    if len(cleaned) == 3:
        cleaned = "".join(ch * 2 for ch in cleaned) + "ff"
    elif len(cleaned) == 4:
        cleaned = "".join(ch * 2 for ch in cleaned)
    elif len(cleaned) == 6:
        cleaned = cleaned + "ff"
    elif len(cleaned) != 8:
        raise ValueError(
            f"invalid color '{token}'. Use RGB/RGBA hex like F54927 or F54927FF"
        )

    try:
        rgba = bytes.fromhex(cleaned)
    except ValueError as error:
        raise ValueError(f"invalid hex color '{token}'") from error

    return tuple(rgba)  # type: ignore[return-value]


def choose_output_path(output_arg: str, name: str, width: int, height: int) -> Path:
    timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    filename = f"{slugify(name)}_{width}x{height}_{timestamp}.png"

    if output_arg:
        requested = Path(output_arg)
        if output_arg.endswith(("/", "\\")) or requested.is_dir():
            return requested / filename
        if requested.suffix.lower() != ".png":
            return requested.with_suffix(".png")
        return requested

    return DEFAULT_OUTPUT_DIR / filename


def slugify(text: str) -> str:
    slug = re.sub(r"[^a-zA-Z0-9]+", "_", text.strip().lower()).strip("_")
    return slug or "texture"


def scale_pixels(
    pixels: list[tuple[int, int, int, int]],
    width: int,
    height: int,
    scale: int,
) -> list[tuple[int, int, int, int]]:
    scaled: list[tuple[int, int, int, int]] = []
    for y in range(height):
        row = pixels[y * width : (y + 1) * width]
        stretched_row: list[tuple[int, int, int, int]] = []
        for pixel in row:
            stretched_row.extend([pixel] * scale)
        for _ in range(scale):
            scaled.extend(stretched_row)
    return scaled


def make_preview_pixels(
    pixels: list[tuple[int, int, int, int]],
    width: int,
    height: int,
    scale: int,
) -> list[tuple[int, int, int, int]]:
    preview: list[tuple[int, int, int, int]] = []
    light = (238, 241, 245, 255)
    dark = (201, 205, 214, 255)

    for y in range(height):
        row = pixels[y * width : (y + 1) * width]
        expanded_rows: list[list[tuple[int, int, int, int]]] = [[] for _ in range(scale)]
        for x, pixel in enumerate(row):
            background = light if (x + y) % 2 == 0 else dark
            composited = alpha_over(pixel, background)
            for expanded_row in expanded_rows:
                expanded_row.extend([composited] * scale)
        for expanded_row in expanded_rows:
            preview.extend(expanded_row)

    return preview


def alpha_over(
    foreground: tuple[int, int, int, int],
    background: tuple[int, int, int, int],
) -> tuple[int, int, int, int]:
    fr, fg, fb, fa = foreground
    br, bg, bb, ba = background

    alpha = fa / 255.0
    inv_alpha = 1.0 - alpha

    r = round(fr * alpha + br * inv_alpha)
    g = round(fg * alpha + bg * inv_alpha)
    b = round(fb * alpha + bb * inv_alpha)
    a = round(fa + ba * inv_alpha)
    return (r, g, b, a)


def write_png(
    path: Path,
    width: int,
    height: int,
    pixels: list[tuple[int, int, int, int]],
) -> None:
    if len(pixels) != width * height:
        raise ValueError("pixel count does not match image size")

    raw_rows = bytearray()
    for y in range(height):
        raw_rows.append(0)
        row = pixels[y * width : (y + 1) * width]
        for r, g, b, a in row:
            raw_rows.extend((r, g, b, a))

    png = bytearray(b"\x89PNG\r\n\x1a\n")
    png.extend(make_chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)))
    png.extend(make_chunk(b"IDAT", zlib.compress(bytes(raw_rows), level=9)))
    png.extend(make_chunk(b"IEND", b""))
    path.write_bytes(png)


def make_chunk(chunk_type: bytes, data: bytes) -> bytes:
    return (
        struct.pack(">I", len(data))
        + chunk_type
        + data
        + struct.pack(">I", binascii.crc32(chunk_type + data) & 0xFFFFFFFF)
    )


if __name__ == "__main__":
    raise SystemExit(main())
