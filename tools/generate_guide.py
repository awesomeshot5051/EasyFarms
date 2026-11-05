#!/usr/bin/env python3
"""
Generates the Easy Farms guide book (a Patchouli book) from the mod's own recipe files, so the recipe lists in the book
always match what the farms really do.

    python3 tools/generate_guide.py

Reads   common/src/main/resources/data/easy_farms/recipe/{resource,plant,mob}_farm/**   (hand-made recipes)
        common/src/generated/resources/data/easy_farms/recipe/resource_farm/auto/**   (auto-generated block recipes)
Writes  common/src/main/resources/data/easy_farms/patchouli_books/guide/book.json
        common/src/main/resources/assets/easy_farms/patchouli_books/guide/en_us/{categories,entries}/**
        common/src/main/resources/data/easy_farms/recipe/guide_book.json

Run it again after adding or changing recipes, then commit the result. The hand-written chapters (how the farms and
cards work) live in this file.
"""
import glob
import json
import math
import os
import re
import shutil

ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
SRC = os.path.join(ROOT, "common", "src")
MAIN = os.path.join(SRC, "main", "resources")
BOOK_ID = "easy_farms:guide"
NS = "easy_farms"
DATA_DIR = os.path.join(MAIN, "data", NS, "patchouli_books", "guide")
ASSET_DIR = os.path.join(MAIN, "assets", NS, "patchouli_books", "guide", "en_us")

# ---------------------------------------------------------------- text helpers
LINE_WIDTH = 28          # characters per line on a book page (conservative; the book resizes text that still overflows)
PAGE_LINES = 16          # lines on a full page
FIRST_PAGE_LINES = 13    # the first page of an entry has a header


def plain(s):
    return re.sub(r"\$\([^)]*\)", "", s)


def line_count(text):
    """Estimated lines a block of text takes. '\n' is a line break, blank lines count."""
    total = 0
    for line in text.split("\n"):
        total += max(1, math.ceil(len(plain(line)) / LINE_WIDTH))
    return total


def pack(blocks, first=FIRST_PAGE_LINES, full=PAGE_LINES):
    """Like pages_from_blocks but separates blocks with a blank line ($(br2))."""
    pages, cur, used, cap = [], [], 0, first
    for block in blocks:
        n = line_count(block)
        need = n + (1 if cur else 0)
        if cur and used + need > cap:
            pages.append("$(br2)".join(cur))
            cur, used, cap, need = [], 0, full, n
        cur.append(block.replace("\n", "$(br)"))
        used += need
    if cur:
        pages.append("$(br2)".join(cur))
    return pages


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


# ---------------------------------------------------------------- names
NAME_OVERRIDES = {
    "tnt": "TNT", "ae2": "AE2", "tide_armor_trim_smithing_template": "Tide Armor Trim Template",
}


def item_name(item):
    item = item.strip()
    if item.startswith("#"):
        return "Any " + item.split(":")[-1].split("/")[-1].replace("_", " ").title()
    path = item.split(":")[-1]
    if path in NAME_OVERRIDES:
        return NAME_OVERRIDES[path]
    return path.replace("_", " ").title()


def ing_text(ing):
    if isinstance(ing, list):
        return " or ".join(item_name(i) for i in ing)
    return item_name(ing)


TIER = ["", "stone+ ", "iron+ ", "gold+ ", "diamond+ ", "netherite+ "]
TOOL_NAME = {"pickaxes": "pickaxe", "shovels": "shovel", "axes": "axe", "hoes": "hoe", "swords": "sword"}
MOD_NAMES = {"ae2": "Applied Energistics 2", "extendedae": "ExtendedAE", "advanced_ae": "Advanced AE",
             "integrateddynamics": "Integrated Dynamics", "integratedterminals": "Integrated Terminals"}


def cond_mod(r):
    for c in r.get("neoforge:conditions", []):
        if c.get("type") == "neoforge:mod_loaded":
            return c["modid"]
    return None


# ---------------------------------------------------------------- recipe model
def load_recipes(kind):
    out = []
    for base in ("main/resources", "generated/resources", "generated_server/resources"):
        pattern = f"{SRC}/{base}/data/easy_farms/recipe/{kind}_farm/**/*.json"
        for f in sorted(glob.glob(pattern, recursive=True)):
            r = json.load(open(f))
            if any(c.get("type") == "neoforge:false" for c in r.get("neoforge:conditions", [])):
                continue
            if r.get("secret"):
                continue                      # secret recipes stay secret
            r["_id"] = os.path.basename(f)[:-5]
            r["_auto"] = "/auto/" in f
            r["_mod"] = cond_mod(r)
            out.append(r)
    return out


def input_ing(i):
    """The ingredient of a recipe input as an id, '#tag', or a list of ids (any of)."""
    if "ingredient" in i:
        return i["ingredient"]
    if "ingredients" in i:
        return [x.get("item") or "#" + x["tag"] for x in i["ingredients"]]
    return i.get("item") or ("#" + i["tag"] if "tag" in i else "?")


def inputs_of(r):
    parts = []
    for i in r["inputs"]:
        ing = input_ing(i)
        n = i.get("count", 1)
        parts.append((f"{n} " if n > 1 else "") + ing_text(ing))
    return parts


def rng(lo, hi):
    return str(lo) if lo == hi else f"{lo}-{hi}"


def drop_text(d):
    name = item_name(d["item"])
    lo, hi = d.get("min", 1), d.get("max", 1)
    s = name if (lo, hi) == (1, 1) else f"{name} x{rng(lo, hi)}"
    if d.get("chance", 1.0) < 1.0:
        s += f" ({round(d['chance'] * 100)}%)"
    return s


def env_text(r):
    env = r.get("environment")
    if not env:
        return None
    bits = []
    if env.get("dimension"):
        bits.append("in " + {"minecraft:the_nether": "the Nether", "minecraft:the_end": "the End"}.get(env["dimension"], env["dimension"]))
    if env.get("block_below"):
        bits.append("over " + " or ".join(item_name(b) for b in env["block_below"]))
    return " ".join(bits)


def notes_of(r):
    notes = []
    tool = r.get("tool")
    if tool:
        notes.append("Tool: " + TIER[min(r.get("min_tier_rank", 0), 5)] + TOOL_NAME.get(tool.split(":")[-1], tool.split(":")[-1]))
    else:
        notes.append("No tool needed")
    drops = r["drops"]
    fort = [d for d in drops if "fortune_max" in d or "fortune_min" in d]
    if fort:
        d = fort[0]
        notes.append(f"Fortune: {item_name(d['item'])} x{rng(d.get('fortune_min', d.get('min', 1)), d.get('fortune_max', d.get('max', 1)))}")
    loot = [d for d in drops if "looting_max" in d or "looting_min" in d]
    if loot:
        d = loot[0]
        notes.append(f"Looting: {item_name(d['item'])} x{rng(d.get('looting_min', d.get('min', 1)), d.get('looting_max', d.get('max', 1)))}")
    for d in drops:
        if d.get("silk_item") and d["silk_item"] != d["item"]:
            notes.append("Silk Touch: " + item_name(d["silk_item"]))
            break
    for d in drops:
        if d.get("smelt_item"):
            notes.append("Smelter: " + item_name(d["smelt_item"]))
            break
    for d in drops:
        if d.get("fire_item"):
            notes.append("Fire Aspect: " + item_name(d["fire_item"]))
            break
    if r.get("nether"):
        notes.append("Nether recipe (Nether Upgrade runs it anywhere)")
    e = env_text(r)
    if e:
        notes.append("Needs " + e)
    if r.get("dyeable"):
        notes.append("Dyeable")
    if r.get("inscriber"):
        ins = r["inscriber"]
        press = ins.get("press")
        outs = ", ".join(item_name(d["item"]) for d in ins["drops"])
        notes.append("Inscriber card: " + outs + (f" (press: {item_name(press)})" if press else ""))
    return notes


def recipe_block(r, title):
    lines = [f"$(thing){title}$()"]
    lines.append("In: " + " + ".join(inputs_of(r)))
    lines.append("Out: " + ", ".join(drop_text(d) for d in r["drops"]))
    lines.append("$(7)" + "; ".join(notes_of(r)) + "$()")
    return "\n".join(lines)


def self_drop(r):
    return (len(r["drops"]) == 1 and len(r["inputs"]) == 1 and r["drops"][0]["item"] == input_ing(r["inputs"][0])
            and not r.get("smeltable") and not r.get("inscriber") and "silk_item" not in r["drops"][0])


def first_item(r):
    d = r["drops"][0]["item"] if r["drops"] else "minecraft:stone"
    return d


# ---------------------------------------------------------------- book content
CATEGORIES = []     # (id, name, desc, icon, sortnum)
ENTRIES = []        # (category, entry_id, name, icon, pages, extra)


def category(cid, name, desc, icon, sort):
    CATEGORIES.append((cid, name, desc, icon, sort))


def entry(cat, eid, name, icon, pages, **extra):
    ENTRIES.append((cat, eid, name, icon, pages, extra))


def text_page(text, title=None):
    p = {"type": "patchouli:text", "text": text}
    if title:
        p["title"] = title
    return p


def prose(blocks, first=FIRST_PAGE_LINES):
    return [text_page(t) for t in pack(blocks, first)]


def recipe_pages(r_id, text=None):
    p = {"type": "patchouli:crafting", "recipe": r_id}
    if text:
        p["text"] = text
    return p


def build_basics():
    category("basics", "Getting Started", "What the farms are and how to use them.", f"{NS}:resource_farm", 0)
    entry("basics", "welcome", "Welcome", f"{NS}:resource_farm", prose([
        "$(thing)Easy Farms$() adds three machines that farm almost everything in Minecraft: a $(item)Resource Farm$(), a $(item)Plant Farm$() and a $(item)Mob Farm$().",
        "What a farm makes is not built into the block. You put items into it and it matches them against $(thing)recipes$(). Swap the items, get a different result.",
        "Each farm is a glass case that shows what it is doing: a block being mined by a swinging tool, a plant growing, or a mob standing inside.",
        "This book explains the farms, the upgrade cards, and lists every recipe the farms know.",
    ]), read_by_default=True, priority=True)
    entry("basics", "quick_start", "Quick Start", f"{NS}:resource_farm", prose([
        "1. Craft a $(item)Resource Farm$() (see $(l:easy_farms:farms/resource_farm)The Farms$()).",
        "2. Put a $(item)Coal Ore$() in an input slot and an $(item)iron pickaxe$() in the tool slot.",
        "3. Wait. Coal comes out of the output slots. Hoppers and pipes can pull it out.",
        "4. Swap the pickaxe for a better one for more speed, and enchant it: $(item)Fortune$() gives more drops.",
        "5. Try the upgrade cards in the $(l:easy_farms:cards/overview)Upgrades screen$().",
    ]))
    entry("basics", "using_a_farm", "Using a Farm", f"{NS}:plant_farm", prose([
        "Right-click a farm to open its screen.",
        "$(thing)3 input slots$(): the items the recipe is made of. 2 porkchops make a pig farm; wheat seeds plus a hoe make wheat.",
        "$(thing)1 tool slot$(): decides the speed and how long the farm lasts. Any pickaxe, shovel, axe, hoe or sword works, including other mods' tools.",
        "$(thing)4 output slots$(): take-only. Hoppers and pipes can pull from them, and push into the inputs.",
        "$(thing)Arrow buttons$(), top right: the first opens the Upgrades screen. A second appears under it when an installed card needs a catalyst.",
        "A farm only runs while its inputs and tool match a recipe $(item)exactly$(). Extra items in the input slots mean no match. Plant Farm recipes are slot-ordered: $(item)soil in slot 1$(), then the plant.",
    ]))
    entry("basics", "tools_and_tiers", "Tools and Tiers", "minecraft:diamond_pickaxe", prose([
        "The $(thing)tool$() sets the speed and the lifetime. A farm does not use up its inputs: the $(item)tool wears out$() instead, after a number of Minecraft days of operation.",
        "Base lifetime: $(item)30 days$() (configurable). The tier multiplies it.",
        "$(thing)Wood$(): slowest, x1\n$(thing)Stone$(): x10 faster, x2\n$(thing)Iron$(): x15 faster, x4\n$(thing)Gold$(): x20 faster, x8\n$(thing)Diamond$(): x25 faster, x16\n$(thing)Netherite$(): x30 faster, x32",
        "Tools from other mods are placed in a tier by their material, so you never need a recipe to allow a tool.",
        "A tool that was already damaged breaks sooner. A tool without durability is removed when its time is up. A paused or blocked farm does not wear its tool.",
        "Recipes that need $(item)no tool$() use up their $(item)inputs$() after that time instead. The Infinity Upgrade stops all of this.",
    ]))
    entry("basics", "enchantments", "Enchantments", "minecraft:enchanted_book", prose([
        "Enchant the tool and the farm uses it:",
        "$(thing)Fortune$(): more drops on recipes that support it.\n$(thing)Silk Touch$(): the block itself on recipes with a silk form (ores, glass...).\n$(thing)Looting$(): more mob drops (Mob Farm).\n$(thing)Fire Aspect$(): the cooked version of mob drops that have one.\n$(thing)Efficiency$() / $(thing)Sharpness$(): shorten the work time.",
        "Some upgrade cards cannot work with some enchantments. Hover a card to read its restrictions.",
    ]))
    entry("basics", "environments", "Environments", "minecraft:netherrack", prose([
        "Most recipes run anywhere. $(item)Nether recipes$() need either the $(item)Nether Upgrade$() or their real environment:",
        "Blaze over netherrack. Nether wart over soul sand or soil. Magma cubes over a magma block or lava. Crimson and warped fungus over their nylium. Other Nether mobs in the Nether dimension. The Shulker needs the End.",
        "If an environment is missing, Jade and The One Probe tell you what the farm needs.",
    ]))
    entry("basics", "dye_and_sheep", "Dye and Sheep", "minecraft:white_dye", prose([
        "Some recipes are $(item)dyeable$(): sheep, concrete, concrete powder and terracotta.",
        "Right-click the farm with a dye (or fire dye at it with a dispenser) and the farm produces that colour. Dye is accepted even while the farm is idle, so you can set it up first.",
    ]))
    entry("basics", "breaking_farms", "Breaking a Farm", "minecraft:diamond_pickaxe", prose([
        "With $(item)Silk Touch$(), the farm drops as an item $(item)with everything inside$(): inputs, tool, outputs, upgrades and catalysts.",
        "Without it, the contents spill on the ground.",
    ]))
    entry("basics", "hoe_and_grass", "Hoe and Grass", "minecraft:farmland", prose([
        "Craft a $(item)hoe$() with a $(item)grass block$() to get farmland. The hoe loses 1 durability.",
        "Farmland is what the Plant Farm shows for crops that need it.",
    ]))
    entry("basics", "config", "Configuration", "minecraft:comparator", prose([
        "Config file: $(item)config/easy_farms-common.toml$()",
        "$(thing)timeMultiplier$() (1.0): multiplies every farm's cycle time. 2.0 is twice as slow.",
        "$(thing)baseInputLifetimeDays$() (30): Minecraft days before the tool breaks (or, for toolless recipes, the inputs are used up) at the base tier.",
        "Tool tiers, recipes and upgrade cards are datapack data, so a modpack can change them without code.",
    ]))


def build_farms():
    category("farms", "The Farms", "The three machines and how to craft them.", f"{NS}:plant_farm", 1)
    entry("farms", "resource_farm", "Resource Farm", f"{NS}:resource_farm", [
        text_page("The $(item)Resource Farm$() mines blocks: stone, ores, sand, nether blocks and so on. It needs a $(item)pickaxe, shovel or axe$() in the tool slot.$(br2)Its floor is stone.$(br2)$(thing)Ingredients$(): 6 glass panes (any), 1 pickaxe (any), 1 iron block."),
        recipe_pages(f"{NS}:resource_farm", "Glass panes around a pickaxe, over an iron block."),
        text_page("Every full block that can be mined is covered, with drops read from vanilla loot tables, plus hand-made recipes for ores, logs, nether blocks and special cases. See $(l:easy_farms:resource_recipes/ores)Resource Farm Recipes$()."),
    ])
    entry("farms", "plant_farm", "Plant Farm", f"{NS}:plant_farm", [
        text_page("The $(item)Plant Farm$() grows crops, flowers, mushrooms, saplings and trees. It usually needs a $(item)hoe or axe$() plus the plant (and soil) as inputs.$(br2)Its floor is grass, farmland with water, or soul sand, depending on the recipe.$(br2)$(thing)Ingredients$(): 6 glass panes (any), 1 hoe (any), 1 dirt."),
        recipe_pages(f"{NS}:plant_farm", "Glass panes around a hoe, over dirt."),
        text_page("Put the $(item)soil in slot 1$() and the plant in slot 2. See $(l:easy_farms:plant_recipes/crops)Plant Farm Recipes$() for every plant."),
    ])
    entry("farms", "mob_farm", "Mob Farm", f"{NS}:mob_farm", [
        text_page("The $(item)Mob Farm$() \"kills\" mobs for their drops. It needs a $(item)sword$() plus the food or item that mob needs as input.$(br2)Its floor is lava with stone.$(br2)$(thing)Ingredients$(): 5 glass panes (any), 1 lava bucket, 1 sword (any), 2 iron ingots."),
        recipe_pages(f"{NS}:mob_farm", "Glass panes over a lava bucket, with a sword between two iron ingots below."),
        text_page("Inputs follow the original Mob Farms mod: for example porkchops for pigs, raw beef for cows. See $(l:easy_farms:mob_recipes/passive)Mob Farm Recipes$() for every mob."),
    ])


def build_cards():
    category("cards", "Upgrade Cards", "Cards that change what a farm does.", f"{NS}:smelter_upgrade", 2)
    entry("cards", "overview", "How Cards Work", f"{NS}:upgrade_card", prose([
        "Upgrade cards go in the $(item)Upgrades screen$(), opened with the arrow button in the top right of a farm. A farm has $(item)18 card slots$(), and holds each card only once.",
        "You can also $(item)sneak and right-click$() a farm with a card in hand to install it.",
        "Cards run in $(item)slot order$(). When two cards change the same drops, the first one wins.",
        "A card can also be $(thing)refused$(): hold it over a slot and the tooltip explains why (see $(l:easy_farms:cards/restrictions)Card Restrictions$()).",
        "Modpacks and other mods can add more cards: any item can become a card with a bit of data, and the same screens show them.",
    ]))
    entry("cards", "redstone", "Redstone Upgrade", f"{NS}:redstone_upgrade", [
        text_page("$(thing)Farms$(): all.$(br2)The farm only works while it has a $(item)redstone signal$().$(br2)Use it to switch a farm on and off with a lever or a circuit.$(br2)$(thing)Ingredients$(): 6 redstone dust, 2 observers, 1 lever."),
        recipe_pages(f"{NS}:redstone_upgrade"),
    ])
    entry("cards", "smelter", "Smelter Upgrade", f"{NS}:smelter_upgrade", [
        text_page("$(thing)Farms$(): Resource.$(br2)Drops come out $(item)smelted$() when the recipe allows it: ores to ingots, sand to glass and so on. The recipe lists show what each one smelts to (\"Smelter:\").$(br2)Other cards can build on it: some cards need the Smelter card on the same farm.$(br2)$(thing)Ingredients$(): 6 iron ingots, 1 blaze powder or blaze rod, 1 flint and steel, 1 netherrack."),
        recipe_pages(f"{NS}:smelter_upgrade"),
    ])
    entry("cards", "speed", "Speed Upgrade", f"{NS}:speed_upgrade", [
        text_page("$(thing)Farms$(): Plant, Mob.$(br2)Recipes that need $(item)no tool$() run $(item)8 times faster$().$(br2)$(thing)Ingredients$(): 7 redstone dust, 1 flint and steel, 1 nether quartz."),
        recipe_pages(f"{NS}:speed_upgrade"),
    ])
    entry("cards", "nether", "Nether Upgrade", f"{NS}:nether_upgrade", [
        text_page("$(thing)Farms$(): Plant, Mob.$(br2)$(item)Nether recipes$() run anywhere, not only in the Nether or over their special block.$(br2)$(thing)Ingredients$(): 3 nether quartz, 3 netherrack, 3 glowstone."),
        recipe_pages(f"{NS}:nether_upgrade"),
    ])
    entry("cards", "infinity", "Infinity Upgrade", f"{NS}:infinity_upgrade", [
        text_page("$(thing)Farms$(): all.$(br2)The tool $(item)never wears out$(), the inputs are $(item)never used up$(), and $(item)catalysts$() are never consumed.$(br2)$(thing)Ingredients$(): 4 diamonds, 4 eyes of ender, 1 nether star."),
        recipe_pages(f"{NS}:infinity_upgrade"),
    ])
    entry("cards", "inscriber", "Inscriber Upgrade", f"{NS}:inscriber_upgrade", [
        text_page("$(thing)Farms$(): Resource. Needs $(item)Applied Energistics 2$().$(br2)Does the AE2 Inscriber's job inside a farm. It is crafted with, and turns back into, an AE2 Inscriber.$(br2)$(thing)Grinding$(): with the card, a Sky Stone farm makes sky dust and a Certus Quartz farm makes certus quartz dust."),
        text_page("$(thing)Pressing$(): gold ore farms make logic processor prints, diamond ore farms engineering processor prints and the silicon farm silicon prints, but only with the matching $(item)press$() in the farm's $(item)press slot$().$(br2)The press slot is the last slot on the Resource Farm's Upgrades screen. The press is not consumed."),
        recipe_pages(f"{NS}:inscriber_upgrade"),
    ], flag="mod:ae2")
    entry("cards", "restrictions", "Card Restrictions", "minecraft:barrier", prose([
        "Some cards cannot be combined, or need something else first. The game explains this with tooltips.",
        "$(thing)Exclusive$(): a card can refuse to share a farm with another card or with a tool enchantment (for example Silk Touch).",
        "$(thing)Required$(): a card can need another card installed too, or a heat source under the farm.",
        "When you hold a card over a slot that would refuse it, the tooltip says why. A card's own tooltip lists its rules.",
        "Rules are checked all the time: if the recipe, the tool or the other cards change so that a rule is broken, the card stays in its slot but is switched off. Where two cards clash, the one in the earlier slot keeps working.",
    ]))
    entry("cards", "catalysts", "Catalysts", "minecraft:hopper", prose([
        "Some cards mix in extra items next to what the farm produces. Those extra items are $(item)catalysts$().",
        "When an installed card needs one, a $(item)second arrow$() appears under the Upgrades arrow. It opens the $(item)Catalysts screen$().",
        "The farm has $(item)18 catalyst slots$() in all, shared between its cards. A card gets $(item)one slot per catalyst item$() it needs, and each slot accepts only that item. A card that would need more slots than are left is refused.",
        "Taking the card out removes its slots and drops what was in them.",
        "$(thing)Automation$(): hoppers and pipes feed the catalyst slots through the $(item)back$() of the farm, the side opposite the way it faces. That side only accepts catalysts; the other sides still use the input and output slots.",
        "The $(item)Infinity Upgrade$() keeps catalysts: they must be present, but are never used up.",
    ]))


def build_recipes():
    # ---------------------------------------------------------------- resource farm
    category("resource_recipes", "Resource Farm Recipes", "Everything the Resource Farm can mine, with its outputs.", "minecraft:iron_ore", 3)
    res = load_recipes("resource")
    groups = {}

    def add(cat, gid, gname, icon, r, flag=None):
        groups.setdefault((cat, gid), {"name": gname, "icon": icon, "recipes": [], "flag": flag})["recipes"].append(r)

    NETHER_WORDS = ("nether", "basalt", "blackstone", "soul", "glowstone", "magma", "crimson", "warped", "shroomlight", "nylium", "end_stone", "purpur", "chorus", "obsidian")
    for r in res:
        rid = r["_id"]
        mod = r["_mod"]
        if mod in MOD_NAMES:
            add("resource_recipes", mod, MOD_NAMES[mod], "minecraft:redstone", r, flag=f"mod:{mod}")
        elif "ore" in rid or "debris" in rid or "netherite" in rid:
            add("resource_recipes", "ores", "Ores", "minecraft:iron_ore", r)
        elif r["_auto"] and self_drop(r):
            add("resource_recipes", "other_blocks", "Other Blocks", "minecraft:bricks", r)
        elif r["_auto"]:
            add("resource_recipes", "special_blocks", "Special Blocks", "minecraft:budding_amethyst", r)
        elif any(w in rid for w in NETHER_WORDS):
            add("resource_recipes", "nether_end", "Nether and End", "minecraft:netherrack", r)
        else:
            add("resource_recipes", "stone_soil", "Stone, Soil and Sand", "minecraft:cobblestone", r)

    order = ["ores", "stone_soil", "nether_end", "special_blocks", "other_blocks"]
    for (cat, gid), g in sorted(groups.items(), key=lambda kv: (order.index(kv[0][1]) if kv[0][1] in order else 99, kv[0][1])):
        recs = sorted(g["recipes"], key=lambda r: r["_id"])
        if gid == "other_blocks":
            blocks = ["These blocks drop themselves (usually 1-3). The tool is a pickaxe unless noted.\n$(7)(shovel) / (hoe) mark other tools.$()"]
            names = []
            for r in recs:
                tool = TOOL_NAME.get((r.get("tool") or "").split(":")[-1], "")
                tag = {"pickaxe": "", "shovel": " (shovel)", "hoe": " (hoe)", "axe": " (axe)"}.get(tool, "")
                tier = TIER[min(r.get("min_tier_rank", 0), 5)].strip()
                names.append(item_name(input_ing(r["inputs"][0])) + tag + (f" [{tier}]" if tier else ""))
            for i in range(0, len(names), 8):
                blocks.append("$(thing)" + ", ".join(names[i:i + 8]) + "$()")
            pages = [text_page(t) for t in pack(blocks)]
        else:
            blocks = [recipe_block(r, item_name(first_item(r)) if len(r["inputs"]) > 1 else ing_text(input_ing(r["inputs"][0])))
                      for r in recs]
            pages = [text_page(t) for t in pack(blocks)]
        entry(cat, gid, g["name"], g["icon"], pages, **({"flag": g["flag"]} if g["flag"] else {}))

    # ---------------------------------------------------------------- plant farm
    category("plant_recipes", "Plant Farm Recipes", "Every plant the Plant Farm can grow, with its outputs.", "minecraft:wheat_seeds", 4)
    plants = load_recipes("plant")
    pg = {}
    FLOWERS = {"allium", "azure", "cornflower", "daisy", "dandelion", "lilac", "lily", "orange", "orchid", "peony", "pink", "poppy", "red", "rose", "sunflower", "white"}
    CROPS = {"beetroot", "berry", "carrot", "gcarrot", "melon", "potato", "pumpkin", "wheat", "coco", "nether_wart", "sugar", "bamboo", "kelp", "cgrass", "pad", "leaf", "vine"}
    for r in plants:
        rid = r["_id"]
        if r["_mod"] in MOD_NAMES:
            pg.setdefault(("mod:" + r["_mod"], MOD_NAMES[r["_mod"]], "minecraft:oak_sapling"), []).append(r)
        elif rid in FLOWERS:
            pg.setdefault(("flowers", "Flowers", "minecraft:poppy"), []).append(r)
        elif rid in CROPS:
            pg.setdefault(("crops", "Crops and Water Plants", "minecraft:wheat"), []).append(r)
        elif "mushroom" in rid or rid in ("crimson", "warped"):
            pg.setdefault(("fungi", "Mushrooms and Fungi", "minecraft:brown_mushroom"), []).append(r)
        else:
            pg.setdefault(("trees", "Trees", "minecraft:oak_sapling"), []).append(r)
    for (gid, gname, icon), recs in sorted(pg.items(), key=lambda kv: ["crops", "flowers", "trees", "fungi"].index(kv[0][0]) if kv[0][0] in ["crops", "flowers", "trees", "fungi"] else 99):
        recs = sorted(recs, key=lambda r: r["_id"])
        flag = gid.split(":")[1] if gid.startswith("mod:") else None
        blocks = [recipe_block(r, item_name(r["visual"]["target"])) for r in recs]
        entry("plant_recipes", gid.replace("mod:", ""), gname, icon, [text_page(t) for t in pack(blocks)], **({"flag": "mod:" + flag} if flag else {}))

    # ---------------------------------------------------------------- mob farm
    category("mob_recipes", "Mob Farm Recipes", "Every mob the Mob Farm can farm, with its outputs.", "minecraft:zombie_head", 5)
    mobs = load_recipes("mob")
    mg = {}
    PASSIVE = {"chicken", "cod", "cow", "glow_squid", "goat", "horse", "mooshroom", "parrot", "pig", "pufferfish", "rabbit", "salmon", "sheep", "squid", "tropical_fish", "turtle", "snow_golem", "iron", "slime"}
    NETHER = {"blaze", "ghast", "hoglin", "magma_cube", "piglin", "strider", "wither_skeleton", "zoglin", "zombified_piglin"}
    for r in mobs:
        rid = r["_id"]
        if r["_mod"] in MOD_NAMES:
            mg.setdefault(("mod:" + r["_mod"], MOD_NAMES[r["_mod"]]), []).append(r)
        elif rid in PASSIVE:
            mg.setdefault(("passive", "Passive and Neutral Mobs"), []).append(r)
        elif rid in NETHER:
            mg.setdefault(("nether", "Nether Mobs"), []).append(r)
        else:
            mg.setdefault(("hostile", "Hostile, Boss and End Mobs"), []).append(r)
    icons = {"passive": "minecraft:pig_spawn_egg", "nether": "minecraft:blaze_rod", "hostile": "minecraft:zombie_head"}
    for (gid, gname), recs in sorted(mg.items(), key=lambda kv: ["passive", "hostile", "nether"].index(kv[0][0]) if kv[0][0] in icons else 99):
        recs = sorted(recs, key=lambda r: r["_id"])
        flag = gid.split(":")[1] if gid.startswith("mod:") else None
        blocks = [recipe_block(r, item_name(r["visual"]["target"])) for r in recs]
        entry("mob_recipes", gid.replace("mod:", ""), gname, icons.get(gid, "minecraft:egg"), [text_page(t) for t in pack(blocks)], **({"flag": "mod:" + flag} if flag else {}))


# ---------------------------------------------------------------- output
def write_all():
    for d in (DATA_DIR, ASSET_DIR):
        shutil.rmtree(d, ignore_errors=True)
    write_json(os.path.join(DATA_DIR, "book.json"), {
        "name": "Easy Farms Guide",
        "landing_text": "Resource, Plant and Mob farms in a glass case. This guide covers how they work, the upgrade cards, and every recipe the farms know.",
        "subtitle": "How to farm everything",
        "book_texture": "patchouli:textures/gui/book_green.png",
        "creative_tab": "minecraft:tools_and_utilities",
        "use_resource_pack": True,
        "show_progress": False,
        "text_overflow_mode": "resize",
    })
    for cid, name, desc, icon, sort in CATEGORIES:
        write_json(os.path.join(ASSET_DIR, "categories", cid + ".json"), {"name": name, "description": desc, "icon": icon, "sortnum": sort})
    for cat, eid, name, icon, pages, extra in ENTRIES:
        body = {"name": name, "category": f"{NS}:{cat}", "icon": icon, "pages": pages}
        body.update(extra)
        write_json(os.path.join(ASSET_DIR, "entries", cat, eid + ".json"), body)
    # How a player gets the book: book + glass pane. Only loaded when Patchouli is installed.
    write_json(os.path.join(MAIN, "data", NS, "recipe", "guide_book.json"), {
        "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "patchouli"}],
        "fabric:load_conditions": [{"condition": "fabric:all_mods_loaded", "values": ["patchouli"]}],
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": [{"item": "minecraft:book"}, {"tag": "c:glass_panes"}],
        "result": {"id": "patchouli:guide_book", "count": 1, "components": {"patchouli:book": BOOK_ID}},
    })


if __name__ == "__main__":
    build_basics()
    build_farms()
    build_cards()
    build_recipes()
    write_all()
    pages = sum(len(e[4]) for e in ENTRIES)
    print(f"{len(CATEGORIES)} categories, {len(ENTRIES)} entries, {pages} pages")
