#!/usr/bin/env python3
"""Generate textures for item models that still use vanilla item layers."""

from __future__ import annotations

import json
from pathlib import Path
import sys

REPO_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO_ROOT / "texture_sources"))

from generate_missing_item_models import palette_for, write_model, write_texture

MODEL_DIR = REPO_ROOT / "src/main/resources/assets/jazzycookin/models/item"


def uses_vanilla_item_texture(path: Path) -> bool:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return False
    return any(
        isinstance(ref, str) and ref.startswith("minecraft:item/")
        for ref in data.get("textures", {}).values()
    )


def main() -> int:
    generated: list[str] = []
    for model_path in sorted(MODEL_DIR.glob("*.json")):
        if not uses_vanilla_item_texture(model_path):
            continue
        name = model_path.stem
        write_texture(name, palette_for(name))
        write_model(name)
        generated.append(name)
    print("Generated remaining item textures:", ", ".join(generated))
    print(f"Generated count: {len(generated)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
