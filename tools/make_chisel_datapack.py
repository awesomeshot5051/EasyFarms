#!/usr/bin/env python3
"""Builds the Easy Farms x Chisel datapack from a Chisel 3 jar.

Chisel lists every block family in data/chisel/chisel/variant_families/*.json. For each family with more than one block this writes an
item tag easy_farms_chisel:variants/<family> (Easy Farms treats every item tag under variants/ as a group of interchangeable items),
plus the Chisel card (farm_upgrade) and its crafting recipe.

usage: make_chisel_datapack.py <chisel.jar> <output dir> <1.21.1|26.1.2>
"""
import json
import shutil
import sys
import zipfile
from pathlib import Path

NS = "easy_farms_chisel"
FAMILIES = "data/chisel/chisel/variant_families/"


def write(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def main(jar, out, version):
    out = Path(out)
    if out.exists():
        shutil.rmtree(out)
    legacy = version == "1.21.1"
    load = {"neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "chisel"}]}
    fabric = {"fabric:load_conditions": [{"condition": "fabric:all_mods_loaded", "values": ["chisel"]}]}

    if legacy:
        mcmeta = {"pack": {"description": "Easy Farms x Chisel 3: a card that swaps a farm's output for any Chisel variant",
                           "pack_format": 48, "supported_formats": {"min_inclusive": 34, "max_inclusive": 48}}}
    else:
        mcmeta = {"pack": {"description": "Easy Farms x Chisel 3 (26.1.2): a card that swaps a farm's output for any Chisel variant",
                           "min_format": 84, "max_format": 101}}
    write(out / "pack.mcmeta", mcmeta)

    count = 0
    with zipfile.ZipFile(jar) as z:
        for name in sorted(z.namelist()):
            if not (name.startswith(FAMILIES) and name.endswith(".json")):
                continue
            family = json.loads(z.read(name).decode("utf-8-sig"))
            blocks = []
            for v in family.get("variants", []):
                block = v.get("block")
                if block and block not in blocks:
                    blocks.append(block)
            if len(blocks) < 2:
                continue
            key = Path(name).stem
            write(out / "data" / NS / "tags" / "item" / "variants" / f"{key}.json",
                  {"values": [{"id": b, "required": False} for b in blocks]})
            count += 1

    write(out / "data" / NS / "easy_farms" / "farm_upgrade" / "chisel.json", {
        **{"neoforge:conditions": load["neoforge:conditions"]},
        "item": "easy_farms:upgrade_card",
        "card": f"{NS}:chisel",
        "name": "Chisel Card",
        "description": "Opens a variant picker in the farm's GUI: choose any Chisel variant of the block the farm makes.",
        "kinds": ["resource", "plant", "mob"],
        "requires": "any",
        "effects": [{"type": "easy_farms:variant_select"}],
    })

    def ingredient(item):
        return {"item": item} if legacy else item

    write(out / "data" / NS / "recipe" / "chisel.json", {
        **load, **fabric,
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": [" D ", "SCS", " I "],
        "key": {
            "D": ingredient("chisel:chisel_diamond"),
            "S": ingredient("minecraft:stone"),
            "C": ingredient("chisel:chisel_iron"),
            "I": ingredient("minecraft:iron_ingot"),
        },
        "result": {"id": "easy_farms:upgrade_card", "count": 1, "components": {"easy_farms:card": f"{NS}:chisel"}},
    })
    print(f"{out}: {count} variant groups")


if __name__ == "__main__":
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    main(*sys.argv[1:])
