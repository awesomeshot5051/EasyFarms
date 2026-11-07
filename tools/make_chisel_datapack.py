#!/usr/bin/env python3
"""Builds the Easy Farms x Chisel datapack from a Chisel 3 jar.

Chisel lists every block family in data/chisel/chisel/variant_families/*.json. For each family with more than one block this writes an
item tag easy_farms_chisel:variants/<family> (Easy Farms treats every item tag under variants/ as a group of interchangeable items),
plus the Chisel card (farm_upgrade) and its crafting recipe.

It also writes a base Resource Farm recipe (data/easy_farms_chisel/recipe/resource_farm/<family>.json) for every family made only of Chisel's
own blocks (marble, limestone, the metals, framed glass...), so those blocks can be farmed; the Chisel card then swaps in any variant.
Families that belong to another mod (AE2, All the Mods, ...) are skipped, as their base block is that mod's.

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


# Families named after another mod's blocks: their base block is not Chisel's, so no base recipe is made for them.
COMPAT_PREFIXES = ("ae2_", "atm_", "aii_", "bblc_", "dn_", "mcwp_", "tt_", "nv_")


def base_block(key, blocks):
    """The plainest block of a family: <family>_raw, any *_raw, the family's own name, *_regular/_normal/_block, else the first."""
    name = key
    for pre in COMPAT_PREFIXES:
        if name.startswith(pre):
            name = name[len(pre):]
    for pick in (f"chisel:{name}_raw", f"chisel:{key}_raw"):
        if pick in blocks:
            return pick
    for suffix in ("_raw", f"{name}", "_regular", "_normal", "_block"):
        found = sorted((b for b in blocks if (b == f"chisel:{suffix}" if suffix == name else b.endswith(suffix))), key=len)
        if found:
            return found[0]
    return blocks[0]


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
    recipes = 0
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
            if all(b.startswith("chisel:") for b in blocks) and not key.startswith(COMPAT_PREFIXES):
                base = base_block(key, blocks)
                inp = {"item": base, "count": 1} if legacy else {"ingredient": base, "count": 1}
                write(out / "data" / NS / "recipe" / "resource_farm" / f"{key}.json", {
                    **load, **fabric,
                    "inputs": [inp],
                    "tool": "minecraft:pickaxes",
                    "visual": {"type": "block", "target": base},
                    "base_ticks": 600,
                    "drops": [{"item": base}],
                    "type": "easy_farms:resource_farm",
                })
                recipes += 1

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
    write(out / "assets" / "easy_farms" / "patchouli_books" / "guide" / "en_us" / "entries" / "cards" / "chisel.json", {
        "name": "Chisel Card",
        "category": "easy_farms:cards",
        "icon": f"easy_farms:upgrade_card[easy_farms:card=\"{NS}:chisel\"]",
        "pages": [
            {"type": "patchouli:text", "text": "$(thing)Farms$(): Resource, Plant, Mob. Needs $(item)Chisel$().$(br2)With the card installed, an arrow on the left of the farm's screen opens the $(thing)Variants$() pop-out. It lists every Chisel variant of the block the farm makes (for a cobblestone farm, all the cobblestone variants).$(br2)Click one to make the farm output that variant instead. Click the plain block to go back. Scroll the list with the mouse wheel.$(br2)Chisel's own blocks (marble, limestone, the metals, framed glass...) can be farmed in a Resource Farm: put the plain block in the input and a pickaxe in the tool slot. The card then lets you pick any of its variants.$(br2)Wood: Chisel's wood variants are plank variants. On a Plant farm, add the $(item)Crafting Card$() with a planks target: it crafts the logs into planks, and the Chisel card then offers the plank variants."},
            {"type": "patchouli:crafting", "recipe": f"{NS}:chisel"},
        ],
        "flag": "mod:chisel",
    })
    (out / "README.md").write_text(f"""# Easy Farms x Chisel 3 ({version})

Adds the **Chisel Card**. Install it in a farm and the farm screen gets a *Variants* arrow on its left edge. It opens a pop-out listing
every Chisel variant of the block the farm makes; click one and the farm outputs that variant instead.

It also adds a base Resource Farm recipe for each of Chisel's own block families (marble, limestone, the metals, framed glass...):
the plain block goes in the input, a pickaxe in the tool slot, and the same block comes out. With the card installed, pick any
variant of it ({recipes} recipes). JEI shows each such recipe "with the Chisel Card", with one cycling output slot holding every
variant, so looking up (R) any Chisel block finds how to farm it.

Wood: Chisel's wood variants are plank variants. A Plant farm makes logs, so give it the **Crafting card** with a planks target (it crafts
the logs into planks) and the Chisel card then offers the plank variants of what the Crafting card makes. JEI shows this combination.

Needs a build of Easy Farms with the `easy_farms:variant_select` effect (3.2.0 or newer) and Chisel 3.

How it works: every item tag under `variants/` (this pack ships {count} of them, one per Chisel family, generated from Chisel's own
`variant_families`) is a group of interchangeable items. Any other pack can add its own group with one tag file.

Rebuild with `tools/make_chisel_datapack.py <chisel.jar> <out dir> {version}`.
""")
    print(f"{out}: {count} variant groups, {recipes} base farm recipes")


if __name__ == "__main__":
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    main(*sys.argv[1:])
