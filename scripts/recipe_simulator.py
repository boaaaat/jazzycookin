#!/usr/bin/env python3
"""Build and validate Jazzy Cookin dish-schema attempts.

This is a lightweight companion to the in-game simulation. It reads the same
``data/jazzycookin/dish_schema`` JSON files, scores flexible ingredient and
process attempts against schema roles/targets, and can emit starter schemas for
new recipe work.
"""

from __future__ import annotations

import argparse
import json
from dataclasses import dataclass
from pathlib import Path
from statistics import mean
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
SCHEMA_DIR = ROOT / "src/main/resources/data/jazzycookin/dish_schema"

COMPOSITION_TARGETS = {"water", "fat", "protein", "cheese", "onion", "herb", "pepper"}
SEASONING_TARGETS = {"seasoning"}
COOKING_TARGETS = {"protein_set", "browning", "char_level"}
TEXTURE_TARGETS = {"aeration", "fragmentation", "cohesiveness"}
PROCESS_TARGETS = {"whisk_work", "stir_count", "flip_count", "time_in_pan", "process_depth"}
THERMAL_TARGETS = {"surface_temp_c", "core_temp_c"}
KNOWN_STATIONS = {
    "prep_table",
    "spice_grinder",
    "strainer",
    "mixing_bowl",
    "canning_station",
    "drying_rack",
    "smoker",
    "fermentation_crock",
    "steamer",
    "stove",
    "oven",
    "microwave",
    "cooling_rack",
    "resting_board",
    "plating_station",
}
STATION_TECHNIQUES = {
    "prep_table": {"prepped", "cut", "mixed"},
    "spice_grinder": {"prepped", "cut", "mixed"},
    "strainer": {"prepped", "cut", "mixed"},
    "mixing_bowl": {"mixed", "dip_or_coat", "prepped"},
    "canning_station": {"prepped"},
    "drying_rack": {"prepped", "rested"},
    "smoker": {"simmered", "pan_fried"},
    "fermentation_crock": {"prepped", "mixed", "rested"},
    "steamer": {"simmered"},
    "stove": {"simmered", "pan_fried", "mixed"},
    "oven": {"baked"},
    "microwave": {"simmered"},
    "cooling_rack": {"rested"},
    "resting_board": {"rested", "cut"},
    "plating_station": {"plated"},
}
TECHNIQUE_STATES = {
    "prepped": "prepped",
    "cut": "cut",
    "mixed": "mixed",
    "dip_or_coat": "coated",
    "simmered": "simmered",
    "pan_fried": "pan_fried",
    "baked": "baked",
    "rested": "rested",
    "plated": "plated",
}


@dataclass(frozen=True)
class Ingredient:
    name: str
    traits: frozenset[str]
    amount: float = 1.0


@dataclass(frozen=True)
class Attempt:
    ingredients: tuple[Ingredient, ...]
    techniques: frozenset[str]
    targets: dict[str, float]


def main() -> int:
    parser = argparse.ArgumentParser(description="Score or build Jazzy Cookin dish-schema attempts.")
    parser.add_argument("--schema", help="Schema key to score against. Omit with --best to rank all schemas.")
    parser.add_argument("--best", action="store_true", help="Rank this attempt against all schemas.")
    parser.add_argument("--ingredient", action="append", default=[], help="Ingredient as name:trait,trait[@amount].")
    parser.add_argument("--technique", action="append", default=[], help="Technique applied to the attempt.")
    parser.add_argument("--target", action="append", default=[], help="Process target as key=value, e.g. browning=0.25.")
    parser.add_argument("--validate-all", action="store_true", help="Build canonical midpoint attempts for every schema and require finalizable scores.")
    parser.add_argument("--validate-stations", action="store_true", help="Validate every schema's station steps and expected station output.")
    parser.add_argument("--station-report", action="store_true", help="Print expected station inputs and outputs for schemas.")
    parser.add_argument("--emit-template", metavar="KEY", help="Print a starter dish_schema JSON template for a new key.")
    parser.add_argument("--result", default="jazzycookin:new_dish", help="Result item id for --emit-template.")
    parser.add_argument("--category", default="generic", help="Category for --emit-template.")
    args = parser.parse_args()

    if args.emit_template:
        print(json.dumps(template_schema(args.emit_template, args.result, args.category), indent=2))
        return 0

    schemas = load_schemas()
    if args.validate_stations:
        return validate_stations(schemas)
    if args.station_report:
        print_station_report(schemas, args.schema)
        return 0
    if args.validate_all:
        return validate_all(schemas)

    attempt = parse_attempt(args)
    if args.best:
        ranked = sorted((score_schema(schema, attempt) for schema in schemas.values()), key=lambda result: result["score"], reverse=True)
        for result in ranked[:10]:
            print(format_result(result))
        return 0

    if not args.schema:
        parser.error("--schema is required unless --best, --validate-all, or --emit-template is used")
    schema = schemas.get(args.schema)
    if schema is None:
        raise SystemExit(f"Unknown schema {args.schema!r}. Available: {', '.join(sorted(schemas))}")
    print(format_result(score_schema(schema, attempt)))
    return 0


def load_schemas() -> dict[str, dict[str, Any]]:
    schemas: dict[str, dict[str, Any]] = {}
    for path in sorted(SCHEMA_DIR.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        key = data.get("key", path.stem)
        schemas[key] = data
    return schemas


def parse_attempt(args: argparse.Namespace) -> Attempt:
    ingredients = tuple(parse_ingredient(raw) for raw in args.ingredient)
    techniques = frozenset(args.technique)
    targets: dict[str, float] = {}
    for raw in args.target:
        key, value = raw.split("=", 1)
        targets[key.strip()] = float(value)
    return Attempt(ingredients, techniques, targets)


def parse_ingredient(raw: str) -> Ingredient:
    name, _, trait_blob = raw.partition(":")
    if not trait_blob:
        trait_blob = name
        name = "ingredient"
    trait_blob, _, amount_blob = trait_blob.partition("@")
    traits = frozenset(part.strip() for part in trait_blob.split(",") if part.strip())
    amount = float(amount_blob) if amount_blob else 1.0
    return Ingredient(name.strip(), traits, amount)


def score_schema(schema: dict[str, Any], attempt: Attempt) -> dict[str, Any]:
    weights = schema.get("weights", {})
    components = {
        "roles": role_score(schema, attempt),
        "composition": target_group_score(schema, attempt, COMPOSITION_TARGETS),
        "seasoning": target_group_score(schema, attempt, SEASONING_TARGETS),
        "cooking": target_group_score(schema, attempt, COOKING_TARGETS),
        "texture": target_group_score(schema, attempt, TEXTURE_TARGETS),
        "process": target_group_score(schema, attempt, PROCESS_TARGETS),
        "thermal": target_group_score(schema, attempt, THERMAL_TARGETS),
        "technique": technique_score(schema, attempt),
        "presentation": presentation_score(schema, attempt),
    }
    weighted_total = 0.0
    total_weight = 0.0
    for name, component in components.items():
        weight = float(weights.get(name, 0.0))
        if weight <= 0.0:
            continue
        weighted_total += component * weight
        total_weight += weight
    score = weighted_total / total_weight if total_weight else mean(components.values())
    finalize_threshold = float(schema.get("finalize_threshold", 0.6))
    preview_threshold = float(schema.get("preview_threshold", 0.5))
    return {
        "key": schema.get("key"),
        "result": schema.get("result"),
        "score": score,
        "grade": grade(score),
        "preview": score >= preview_threshold,
        "finalize": score >= finalize_threshold,
        "preview_threshold": preview_threshold,
        "finalize_threshold": finalize_threshold,
        "components": components,
    }


def role_score(schema: dict[str, Any], attempt: Attempt) -> float:
    required = schema.get("required_roles", [])
    optional = schema.get("optional_roles", [])
    if not required and not optional:
        return 1.0

    required_scores = [best_role_match(role, attempt) * float(role.get("weight", 1.0)) for role in required]
    required_weights = [float(role.get("weight", 1.0)) for role in required]
    required_score = sum(required_scores) / sum(required_weights) if required_weights else 1.0

    optional_scores = [best_role_match(role, attempt) * float(role.get("weight", 0.0)) for role in optional]
    optional_weight = sum(float(role.get("weight", 0.0)) for role in optional)
    optional_bonus = (sum(optional_scores) / optional_weight) * 0.12 if optional_weight else 0.0
    forbidden_penalty = 0.0
    forbidden = set(schema.get("forbidden_traits", []))
    if forbidden and any(ingredient.traits & forbidden for ingredient in attempt.ingredients):
        forbidden_penalty = 0.25
    return clamp(required_score + optional_bonus - forbidden_penalty)


def best_role_match(role: dict[str, Any], attempt: Attempt) -> float:
    any_traits = set(role.get("any_traits", []))
    all_traits = set(role.get("all_traits", []))
    best = 0.0
    for ingredient in attempt.ingredients:
        if all_traits and not all_traits.issubset(ingredient.traits):
            continue
        any_score = 1.0 if not any_traits else len(any_traits & ingredient.traits) / max(1, min(len(any_traits), 2))
        best = max(best, clamp(any_score))
    return best


def target_group_score(schema: dict[str, Any], attempt: Attempt, group: set[str]) -> float:
    scores = [
        range_score(attempt.targets[name], target)
        for name, target in schema.get("targets", {}).items()
        if name in group and name in attempt.targets
    ]
    return mean(scores) if scores else 1.0


def range_score(value: float, target: dict[str, Any]) -> float:
    low = float(target.get("min", value))
    high = float(target.get("max", value))
    softness = max(0.0001, float(target.get("softness", 0.1)))
    if low <= value <= high:
        return 1.0
    distance = low - value if value < low else value - high
    return clamp(1.0 - distance / softness)


def technique_score(schema: dict[str, Any], attempt: Attempt) -> float:
    required = set(schema.get("required_techniques", []))
    if not required:
        return 1.0
    return len(required & attempt.techniques) / len(required)


def presentation_score(schema: dict[str, Any], attempt: Attempt) -> float:
    serving_items = schema.get("serving_items", [])
    if not serving_items:
        return 1.0
    names = {ingredient.name for ingredient in attempt.ingredients}
    return 1.0 if any(item in names or item.rsplit(":", 1)[-1] in names for item in serving_items) else 0.5


def validate_all(schemas: dict[str, dict[str, Any]]) -> int:
    failures = []
    for schema in schemas.values():
        result = score_schema(schema, canonical_attempt(schema))
        if not result["finalize"]:
            failures.append(result)
    if failures:
        for result in failures:
            print(format_result(result))
        print(f"FAILED: {len(failures)} schema(s) did not finalize from canonical midpoint attempts")
        return 1
    print(f"Recipe simulator validation passed: {len(schemas)} schema canonical attempts finalize")
    return 0


def validate_stations(schemas: dict[str, dict[str, Any]]) -> int:
    failures: list[str] = []
    step_ids_by_schema = {
        key: {str(step.get("id", "")).strip() for step in schema.get("steps", []) if str(step.get("id", "")).strip()}
        for key, schema in schemas.items()
    }
    external_step_ids = {
        key: set().union(*(step_ids_by_schema.get(prerequisite, set()) for prerequisite in schema.get("prerequisite_schemas", [])))
        for key, schema in schemas.items()
    }

    for key, schema in schemas.items():
        station_steps = simulate_station_steps(schema)
        if not station_steps:
            failures.append(f"{key}: missing station steps")
            continue

        required_techniques = set(schema.get("required_techniques", []))
        step_techniques = {step["technique"] for step in station_steps}
        missing_techniques = required_techniques - step_techniques
        if missing_techniques:
            failures.append(f"{key}: required techniques not represented by station steps: {', '.join(sorted(missing_techniques))}")

        known_prerequisites = step_ids_by_schema[key] | external_step_ids[key]
        for step in station_steps:
            station = step["station"]
            technique = step["technique"]
            if station not in KNOWN_STATIONS:
                failures.append(f"{key}:{step['id']}: unknown station {station!r}")
            elif technique not in STATION_TECHNIQUES.get(station, set()):
                failures.append(f"{key}:{step['id']}: station {station!r} cannot perform technique {technique!r}")
            for prerequisite in step["prerequisites"]:
                if prerequisite not in known_prerequisites:
                    failures.append(f"{key}:{step['id']}: unresolved prerequisite step {prerequisite!r}")
            if not step["output_item"]:
                failures.append(f"{key}:{step['id']}: missing expected output item")
            if not step["output_state"]:
                failures.append(f"{key}:{step['id']}: missing expected output state")

        final_step = station_steps[-1]
        if final_step["output_item"] != schema.get("result"):
            failures.append(f"{key}: final station output {final_step['output_item']} does not match schema result {schema.get('result')}")

        result = score_schema(schema, canonical_attempt(schema))
        if not result["finalize"]:
            failures.append(f"{key}: canonical station attempt does not finalize ({result['score']:.3f})")

    if failures:
        print("\n".join(failures))
        print(f"FAILED: {len(failures)} station simulation validation error(s)")
        return 1
    step_count = sum(len(simulate_station_steps(schema)) for schema in schemas.values())
    print(f"Station simulation validation passed: {len(schemas)} schemas, {step_count} station step outputs")
    return 0


def simulate_station_steps(schema: dict[str, Any]) -> list[dict[str, Any]]:
    steps = schema.get("steps", [])
    simulated = []
    for index, step in enumerate(steps):
        technique = str(step.get("technique", "")).strip()
        output_state = str(step.get("output_state") or TECHNIQUE_STATES.get(technique, "")).strip()
        simulated.append({
            "schema": schema.get("key"),
            "id": str(step.get("id", f"step_{index + 1}")).strip(),
            "station": str(step.get("station", "")).strip(),
            "technique": technique,
            "tools": tuple(step_tools(step)),
            "prerequisites": tuple(str(value).strip() for value in step.get("prerequisites", []) if str(value).strip()),
            "inputs": tuple(station_input_names(schema)),
            "output_item": schema.get("result") if index == len(steps) - 1 else str(step.get("output_item", "")).strip(),
            "output_state": output_state,
            "progress_target": float(step.get("progress_target", 1.0)),
        })
    return simulated


def station_input_names(schema: dict[str, Any]) -> list[str]:
    inputs: list[str] = []
    for entry in schema.get("ingredients", []):
        item = entry.get("item")
        if item:
            inputs.append(str(item))
        else:
            role = entry.get("role", "ingredient")
            traits = [*entry.get("all_traits", []), *entry.get("any_traits", [])]
            inputs.append(f"{role}:{','.join(traits[:3])}" if traits else str(role))
    for prerequisite in schema.get("prerequisite_schemas", []):
        inputs.append(f"schema:{prerequisite}")
    for serving_item in schema.get("serving_items", []):
        inputs.append(str(serving_item))
    return dedupe(inputs)


def step_tools(step: dict[str, Any]) -> list[str]:
    tools: list[str] = []
    if step.get("tool"):
        tools.append(str(step["tool"]))
    tools.extend(str(tool) for tool in step.get("tools", []))
    return dedupe(tools)


def dedupe(values: list[str]) -> list[str]:
    seen: set[str] = set()
    unique: list[str] = []
    for value in values:
        if value not in seen:
            seen.add(value)
            unique.append(value)
    return unique


def print_station_report(schemas: dict[str, dict[str, Any]], schema_key: str | None) -> None:
    if schema_key and schema_key not in schemas:
        raise SystemExit(f"Unknown schema {schema_key!r}. Available: {', '.join(sorted(schemas))}")
    selected = [schemas[schema_key]] if schema_key else [schemas[key] for key in sorted(schemas)]
    for schema in selected:
        for step in simulate_station_steps(schema):
            tool_text = ",".join(step["tools"]) if step["tools"] else "none"
            input_text = " + ".join(step["inputs"]) if step["inputs"] else "schema matter"
            print(
                f"{step['schema']}:{step['id']} "
                f"{step['station']}[{step['technique']}; tools={tool_text}] "
                f"{input_text} -> {step['output_item']}@{step['output_state']}"
            )


def canonical_attempt(schema: dict[str, Any]) -> Attempt:
    ingredients: list[Ingredient] = []
    for role in [*schema.get("required_roles", []), *schema.get("optional_roles", [])]:
        traits = set(role.get("all_traits", [])) | set(role.get("any_traits", [])[:1])
        if traits:
            ingredients.append(Ingredient(str(role.get("role", "ingredient")), frozenset(traits)))
    for item in schema.get("serving_items", []):
        ingredients.append(Ingredient(item, frozenset({"container"})))
    targets = {
        name: (float(target.get("min", 0.0)) + float(target.get("max", 0.0))) / 2.0
        for name, target in schema.get("targets", {}).items()
    }
    return Attempt(tuple(ingredients), frozenset(schema.get("required_techniques", [])), targets)


def template_schema(key: str, result: str, category: str) -> dict[str, Any]:
    return {
        "key": key,
        "preview_id": 9000,
        "result": result,
        "category": category,
        "preview_threshold": 0.52,
        "finalize_threshold": 0.6,
        "desirability": 0.75,
        "required_roles": [{"role": "vegetable", "any_traits": ["vegetable"], "weight": 1.0}],
        "optional_roles": [{"role": "salt", "any_traits": ["salt"], "weight": 0.2}],
        "required_techniques": ["cut"],
        "targets": {
            "water": {"min": 0.25, "max": 0.75, "softness": 0.2},
            "seasoning": {"min": 0.02, "max": 0.28, "softness": 0.14},
            "process_depth": {"min": 1.0, "max": 3.0, "softness": 1.0},
        },
        "weights": {
            "roles": 0.3,
            "composition": 0.18,
            "seasoning": 0.14,
            "process": 0.1,
            "technique": 0.18,
            "presentation": 0.1,
        },
        "ingredients": [],
        "steps": [{"id": "prepare", "station": "prep_table", "technique": "cut", "tools": ["chef_knife"], "progress_target": 1.0}],
    }


def format_result(result: dict[str, Any]) -> str:
    components = ", ".join(f"{name}={value:.2f}" for name, value in sorted(result["components"].items()))
    state = "finalize" if result["finalize"] else "preview" if result["preview"] else "miss"
    return f"{result['key']} -> {result['result']} score={result['score']:.3f} grade={result['grade']} state={state} ({components})"


def grade(score: float) -> str:
    if score >= 0.92:
        return "S"
    if score >= 0.82:
        return "A"
    if score >= 0.70:
        return "B"
    if score >= 0.58:
        return "C"
    if score >= 0.45:
        return "D"
    return "F"


def clamp(value: float) -> float:
    return max(0.0, min(1.0, value))


if __name__ == "__main__":
    raise SystemExit(main())
