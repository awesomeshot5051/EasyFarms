#!/usr/bin/env python3
"""Adds Just Dire Things tool abilities to the Easy Farms x Just Dire Things datapack.

Just Dire Things puts upgrades (smelter, ...) on its own tools. A tool with the Smelter upgrade carries the data component
justdirethings:smelter_upgrade_installed = true, and the player can switch it off with justdirethings:smelter_toggle = false
(a missing toggle means on). This writes an Easy Farms `tool_ability` file that gives a farm the same effects when such a tool is in
its tool slot, plus an item tag listing the tools that can take the upgrade, which recipe viewers use to show them. The tag is read
from Just Dire Things' own `justdirethings:ability` recipes (base tool + upgrade item), so nothing is typed in by hand.

usage: make_jdt_tool_ability.py <just dire things jar or data folder> <datapack dir> [ability ...]   (default ability: smelter)
"""
import json
import sys
import zipfile
from pathlib import Path

NS = "easy_farms_justdirethings"
JDT = "justdirethings"
# What each ability does on a farm. The tool's own upgrade already smelts, so no experience (the Smelter *card* gives it).
EFFECTS = {
    "smelter": {"name": "Smelter upgrade", "effects": [{"type": "easy_farms:smelt", "xp": False}],
                "text": "A Just Dire Things tool with the Smelter upgrade installed smelts the farm's drops like the Smelter card, "
                        "but without the experience."},
}


def write(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def ability_recipes(source):
    """Yields every justdirethings:ability recipe (addition item, base tool) from a jar or an unpacked data folder."""
    if Path(source).is_dir():
        for f in Path(source).rglob("recipe/*.json"):
            yield json.loads(f.read_text(encoding="utf-8-sig"))
        return
    with zipfile.ZipFile(source) as z:
        for name in z.namelist():
            if name.startswith(f"data/{JDT}/recipe/") and name.endswith(".json"):
                yield json.loads(z.read(name).decode("utf-8-sig"))


def main(source, pack, abilities):
    pack = Path(pack)
    recipes = list(ability_recipes(source))
    for ability in abilities:
        info = EFFECTS[ability]
        upgrade = f"{JDT}:upgrade_{ability}"
        tools = sorted({r["base"] for r in recipes if r.get("type") == f"{JDT}:ability" and r.get("addition") == upgrade})
        if not tools:
            sys.exit(f"no {JDT}:ability recipes add {upgrade}: is this the right jar?")
        write(pack / "data" / NS / "tags" / "item" / f"{ability}_tools.json", {"values": [{"id": t, "required": False} for t in tools]})
        write(pack / "data" / NS / "easy_farms" / "tool_ability" / f"{ability}.json", {
            "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": JDT}],
            "fabric:load_conditions": [{"condition": "fabric:all_mods_loaded", "values": [JDT]}],
            "name": info["name"],
            "components": {f"{JDT}:{ability}_upgrade_installed": True},
            "unless": [{f"{JDT}:tool_enabled": False}, {f"{JDT}:{ability}_toggle": False}],
            "display_tools": f"#{NS}:{ability}_tools",
            "kinds": ["resource", "plant", "mob"],
            "effects": info["effects"],
        })
        write(pack / "assets" / "easy_farms" / "patchouli_books" / "guide" / "en_us" / "entries" / "just_dire_things" / f"tool_{ability}.json", {
            "name": f"Tool {ability.capitalize()}",
            "category": "easy_farms:just_dire_things",
            "icon": upgrade,
            "pages": [{"type": "patchouli:text", "text": info["text"] + "$(br2)It works while the tool is switched on and its "
                       f"{ability} toggle is on, and applies to the farm's tool slot. Recipes that change are listed in JEI as "
                       "\"With: " + info["name"] + "\"."}],
            "flag": f"mod:{JDT}",
        })
        print(f"{ability}: {len(tools)} tools -> {pack}")


if __name__ == "__main__":
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2], sys.argv[3:] or ["smelter"])
