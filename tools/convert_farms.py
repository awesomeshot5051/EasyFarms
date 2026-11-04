#!/usr/bin/env python3
"""
Converts the old per-farm Java classes of resourcefarm / plantFarms / mobFarms into EasyFarms machine recipes.
Run:  python3 tools/convert_farms.py
Writes src/main/resources/data/easy_farms/recipe/<kind>_farm/<id>.json and tools/convert_report.txt.
Anything the heuristics cannot read cleanly is listed in the report with the raw getDrops() text, to be fixed by hand
(or by adding an entry to OVERRIDES below, which wins over the parsed value).
"""
import json, os, re, glob, sys

ROOT = "/mnt/Data/MinecraftMods"
OUT = os.path.join(ROOT, "EasyFarms/src/main/resources/data/easy_farms/recipe")
REPORT = []


def _ins(out, press=None):
    d = {"drops": [{"item": out, "max": 3, "fortune_max": 5}]}
    return {"press": press, **d} if press else d


# What the Inscriber card changes (old resourcefarm: the farm produced the AE2 print / dust instead). Press = item in the press slot.
INSCRIBER = {
    "gold_ore": _ins("ae2:logic_processor_print", "ae2:logic_processor_press"),
    "deepslate_gold_ore": _ins("ae2:logic_processor_print", "ae2:logic_processor_press"),
    "diamond_ore": _ins("ae2:engineering_processor_print", "ae2:engineering_processor_press"),
    "deepslate_diamond_ore": _ins("ae2:engineering_processor_print", "ae2:engineering_processor_press"),
    "silicon": _ins("ae2:silicon_print", "ae2:silicon_press"),
    "sky_stone": _ins("ae2:sky_dust"),
    "certus_quartz_crystal": _ins("ae2:certus_quartz_dust"),
}

TOOL_TAG = {"pickaxe": "minecraft:pickaxes", "shovel": "minecraft:shovels", "hoe": "minecraft:hoes",
            "axe": "minecraft:axes", "sword": "minecraft:swords"}

def snake(name):
    return re.sub(r'(?<!^)(?=[A-Z])', '_', name).lower().replace("__", "_")

def read(p):
    with open(p, encoding="utf-8", errors="replace") as f:
        return f.read()

def get_drops_body(src):
    m = re.search(r'List<ItemStack>\s+getDrops\(\)\s*\{', src)
    if not m: return None
    i = m.end(); depth = 1
    while i < len(src) and depth:
        depth += {'{': 1, '}': -1}.get(src[i], 0); i += 1
    body = src[m.end():i - 1]
    return re.sub(r'//.*', '', body)

def eval_int(expr):
    expr = expr.strip()
    if re.fullmatch(r'[\d\s\*\+\-\(\)]+', expr):
        try: return int(eval(expr))
        except Exception: return None
    return None

def config_defaults(path):
    """var name -> effective default: the default clamped into [min, max] (several old entries had it outside)."""
    src = read(path); out = {}
    for m in re.finditer(r'(\w+)\s*=\s*builder(.*?)\.defineInRange\("[^"]*",\s*([^,]+),\s*([^,]+),\s*([^)]+)\)', src, re.S):
        d, lo, hi = eval_int(m.group(3)), eval_int(m.group(4)), eval_int(m.group(5))
        if d is not None and lo is not None and hi is not None: out[m.group(1)] = max(lo, min(hi, d))
    return out

def mc(name):  # Items.DIAMOND_ORE -> minecraft:diamond_ore
    return "minecraft:" + name.lower()

NS = {"AEItems": "ae2", "AEBlocks": "ae2", "AAEItems": "advanced_ae", "AAEBlocks": "advanced_ae", "EAESingletons": "extendedae"}

def item_id(ns_cls, name):
    return (NS.get(ns_cls, "minecraft") + ":" + name.lower())

# ---- a tiny interpreter for the old getDrops() methods: runs them once per scenario ----
def _match(text, i, open_c, close_c):
    depth = 0
    for j in range(i, len(text)):
        if text[j] == open_c: depth += 1
        elif text[j] == close_c:
            depth -= 1
            if depth == 0: return j
    raise ValueError("unbalanced")

def _parse_block(text):
    nodes = []; i = 0; n = len(text)
    def skip(i):
        while i < n and text[i].isspace(): i += 1
        return i
    def branch(i):
        i = skip(i)
        if i < n and text[i] == '{':
            j = _match(text, i, '{', '}')
            return _parse_block(text[i + 1:j]), j + 1
        j = i; depth = 0
        while j < n and not (text[j] == ';' and depth == 0):
            depth += {'(': 1, ')': -1}.get(text[j], 0); j += 1
        return [('stmt', text[i:j].strip())], j + 1
    while True:
        i = skip(i)
        if i >= n: break
        if re.match(r'if\s*\(', text[i:]):
            pi = text.index('(', i); pj = _match(text, pi, '(', ')')
            cond = text[pi + 1:pj]; body, i = branch(pj + 1)
            arms = [(cond, body)]; els = None
            while True:
                k = skip(i)
                if text[k:k + 4] == 'else' and not text[k + 4:k + 5].isalnum():
                    k2 = skip(k + 4)
                    if re.match(r'if\s*\(', text[k2:]):
                        pi = text.index('(', k2); pj = _match(text, pi, '(', ')')
                        body, i = branch(pj + 1); arms.append((text[pi + 1:pj], body)); continue
                    els, i = branch(k + 4)
                break
            nodes.append(('if', arms, els))
        else:
            j = i; depth = 0
            while j < n and not (text[j] == ';' and depth == 0):
                depth += {'(': 1, ')': -1, '{': 1, '}': -1}.get(text[j], 0); j += 1
            nodes.append(('stmt', text[i:j].strip())); i = j + 1
    return nodes

def _cond(text, sc):
    t = text
    if 'instanceof ServerLevel' in t: return False, None
    if 'shears' in t or re.search(r'dropCount\s*[>!]', t): return True, None
    if 'POINTED_DRIPSTONE' in t: return False, None   # world-dependent bonus in the old mud farm; not representable
    if 'INSCRIBER' in t or 'inscriberPressInstalled' in t: return False, None   # inscriber output is not part of the machine recipes yet
    if 'isSword' in t: return True, None
    tern = re.fullmatch(r'\s*(.+?)\s*\?\s*(.+?)\s*:\s*(.+?)\s*', t)
    if tern and 'random' in tern.group(2):
        ok, _ = _cond(tern.group(1), sc)
        return _cond(tern.group(2) if ok else tern.group(3), sc)
    cm = re.search(r'random\.next(?:Double|Float)\(\)\s*(<|>=)\s*([^&|?:{]+)', t)
    if cm:
        v = cm.group(2).strip()
        expr = re.sub(r'\(float\)|\(double\)', '', v).strip().rstrip('Ff').strip()
        if re.fullmatch(r'[A-Za-z_]\w*', expr): val = sc['vars'].get(expr)
        else:
            try: val = float(eval(expr, {"__builtins__": {}}, {}))
            except Exception: val = None
        if val is None: sc['notes'].append('chance not understood: ' + v); val = 1.0
        return True, (val if cm.group(1) == '<' else 1.0 - val)
    t = re.sub(r'[\w.]+\([^()]*SILK_TOUCH[^()]*\)', 'SILK', t)
    t = re.sub(r'[\w.]+\([^()]*FORTUNE[^()]*\)', 'FORT', t)
    t = re.sub(r'[\w.]+\([^()]*LOOTING[^()]*\)', 'LOOT', t)
    t = re.sub(r'[\w.]+\([^()]*FIRE_ASPECT[^()]*\)', 'FIRE', t)
    t = re.sub(r'[\w.]+\([^()]*INSCRIBER[^()]*\)', 'False', t)
    t = t.replace('smelterUpgradeEnabled', 'SMELT').replace('inscriberPressInstalled', 'False')
    t = t.replace('&&', ' and ').replace('||', ' or ')
    t = re.sub(r'!(?!=)', ' not ', t)
    env = {'SILK': sc['silk'], 'SMELT': sc['smelt'], 'FORT': sc['fort'], 'LOOT': sc['loot'], 'FIRE': sc['fire'], 'False': False, 'True': True}
    try: return bool(eval(t, {"__builtins__": {}}, env)), None
    except Exception: sc['notes'].append('condition not understood: ' + text.strip()); return False, None

def _count(expr, sc):
    expr = expr.strip()
    if expr == 'dropCount': return sc['vars'].get('dropCount', (1, 1))
    if eval_int(expr) is not None: return (eval_int(expr),) * 2
    m = re.fullmatch(r'(?:serverWorld\.)?random\.nextInt\((\d+)\)', expr) or re.fullmatch(r'serverWorld\.random\.nextInt\((\d+)\)', expr)
    if m: return (0, int(m.group(1)) - 1)
    m = re.search(r'nextIntBetweenInclusive\((\d+),\s*(\d+)\)', expr)
    if m: return (int(m.group(1)), int(m.group(2)))
    sc['notes'].append('count not understood: ' + expr); return (1, 1)

def _exec(nodes, sc, chance=1.0):
    for node in nodes:
        if node[0] == 'if':
            done = False
            for cond, body in node[1]:
                ok, ch = _cond(cond, sc)
                if ok:
                    _exec(body, sc, chance * (ch if ch is not None else 1.0)); done = True; break
            if not done and node[2] is not None: _exec(node[2], sc, chance)
            continue
        st = node[1]
        m = re.match(r'(?:int\s+)?dropCount\s*=\s*(.+)', st)
        if m: sc['vars']['dropCount'] = _count(m.group(1), sc); continue
        m = re.match(r'(?:final\s+)?(?:double|float)\s+(\w+)\s*=\s*([\d.]+)F?$', st)
        if m: sc['vars'][m.group(1)] = float(m.group(2)); continue
        m = re.match(r'return\s+(?:Collections\.singletonList|List\.of)\((new ItemStack\(.*\))\)$', st)
        if m: st = 'drops.add(' + m.group(1) + ')'
        m = re.match(r'drops\.add\(new ItemStack\((.+?)\s*\?\s*(\w+)\.(\w+)\s*:\s*(\w+)\.(\w+)(?:\s*,\s*(.+))?\)\)$', st)
        if m:
            ok, _ = _cond(m.group(1), sc)
            cls, name = (m.group(2), m.group(3)) if ok else (m.group(4), m.group(5))
            lo, hi = _count(m.group(6), sc) if m.group(6) else (1, 1)
            sc['drops'].append({"item": item_id(cls, name), "min": lo, "max": hi, "chance": round(chance, 4), "cls": cls}); continue
        m = re.match(r'(?:final\s+)?ItemStack\s+(\w+)\s*=\s*new ItemStack\((\w+)\.(\w+)(?:\s*,\s*(.+))?\)$', st)
        if m:
            lo, hi = _count(m.group(4), sc) if m.group(4) else (1, 1)
            sc['stacks'][m.group(1)] = (m.group(2), m.group(3), lo, hi); continue
        if st.startswith('drops.clear'): sc['drops'].clear(); continue
        m = re.match(r'drops\.add\(new ItemStack\((\w+)\.(\w+)(?:\s*,\s*(.+))?\)\)$', st)
        if m:
            lo, hi = _count(m.group(3), sc) if m.group(3) else (1, 1)
            sc['drops'].append({"item": item_id(m.group(1), m.group(2)), "min": lo, "max": hi, "chance": round(chance, 4), "cls": m.group(1)}); continue
        m = re.match(r'drops\.add\((\w+)\)$', st)
        if m and m.group(1) in sc['stacks']:
            c, n, lo, hi = sc['stacks'][m.group(1)]
            sc['drops'].append({"item": item_id(c, n), "min": lo, "max": hi, "chance": round(chance, 4), "cls": c}); continue
        if st.startswith('drops.') or 'drops.add' in st:
            sc['notes'].append('statement not understood: ' + st[:80])

def simulate(body, silk=False, smelt=False, fort=False, loot=False, fire=False):
    sc = {'silk': silk, 'smelt': smelt, 'fort': fort, 'loot': loot, 'fire': fire, 'vars': {}, 'stacks': {}, 'drops': [], 'notes': []}
    try: _exec(_parse_block(body), sc)
    except Exception as e: sc['notes'].append('parse failure: %s' % e)
    return sc['drops'], sc['notes']

def parse_drops(body):
    """Plain drops (no upgrades/enchants), merged with how each upgrade or enchantment changes them."""
    base, notes = simulate(body)
    silk, n2 = simulate(body, silk=True)
    smelt, n3 = simulate(body, smelt=True)
    fort, n4 = simulate(body, fort=True)
    loot, n5 = simulate(body, loot=True)
    fire, n6 = simulate(body, fire=True)
    notes = list(dict.fromkeys(notes + n2 + n3 + n4 + n5 + n6))
    merged = []; index = []
    for i, e in enumerate(base):
        if merged and merged[-1]["item"] == e["item"] and merged[-1]["chance"] == e["chance"]:
            notes.append("duplicate drop of " + e["item"] + " merged (old code gave it twice)"); continue
        merged.append(dict(e)); index.append(i)
    for e, i in zip(merged, index):
        def alt(lst):
            return lst[i] if len(lst) == len(base) else None
        f, l, k, sm, fi = alt(fort), alt(loot), alt(silk), alt(smelt), alt(fire)
        if f and (f["max"], f["min"]) != (e["max"], e["min"]):
            e["fortune_max"] = f["max"]
            if f["min"] != e["min"]: e["fortune_min"] = f["min"]
        if l and (l["max"], l["min"]) != (e["max"], e["min"]):
            e["looting_max"] = l["max"]
            if l["min"] != e["min"]: e["looting_min"] = l["min"]
        if k and k["item"] != e["item"]: e["silk_item"] = k["item"]
        if sm and sm["item"] != e["item"]: e["smelt_item"] = sm["item"]
        if fi and fi["item"] != e["item"]: e["fire_item"] = fi["item"]
    for name, lst in (("silk", silk), ("smelter", smelt)):
        if len(lst) != len(base): notes.append("%s case has %d drops vs %d (not represented)" % (name, len(lst), len(base)))
    return merged, notes

def drop_json(e, **extra):
    d = {"item": e["item"]}
    if e["min"] != 1: d["min"] = e["min"]
    if e["max"] != 1: d["max"] = e["max"]
    if e["chance"] != 1.0: d["chance"] = e["chance"]
    for k in ("fortune_min", "fortune_max", "looting_min", "looting_max", "silk_item", "smelt_item", "fire_item"):
        if k in e: d[k] = e[k]
    d.update(extra)
    return d

def write_recipe(kind, rid, recipe, mods):
    recipe = dict(recipe)
    recipe["type"] = "easy_farms:%s_farm" % kind
    if mods:
        recipe = {"neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": m} for m in sorted(mods)], **recipe}
    d = os.path.join(OUT, kind + "_farm"); os.makedirs(d, exist_ok=True)
    with open(os.path.join(d, rid + ".json"), "w") as f:
        json.dump(recipe, f, indent=2); f.write("\n")

def report(kind, rid, text):
    REPORT.append("[%s/%s] %s" % (kind, rid, text))

# ---------- resource ----------
MIN_RANK = {  # vanilla mining requirement of the displayed block, by name fragment
    "diamond": 2, "emerald": 2, "gold": 2, "redstone": 2, "iron": 1, "copper": 1, "lapis": 1,
    "ancient_debris": 4, "obsidian": 4, "netherite": 4,
}

def resource():
    base = ROOT + "/resourcefarm/src/main/java/com/awesomeshot5051/resourceFarm"
    cfg = config_defaults(base + "/ServerConfig.java")
    renderers = {os.path.basename(p)[:-len("Renderer.java")]: p
                 for p in glob.glob(base + "/**/*Renderer.java", recursive=True) if "ItemRenderer" not in p}
    n = 0
    for p in sorted(glob.glob(base + "/**/*Tileentity.java", recursive=True)):
        cls = os.path.basename(p)[:-len("Tileentity.java")]
        if cls in ("Farm",): continue
        src = read(p); body = get_drops_body(src)
        rid = snake(cls)
        rid = re.sub(r'_farm$', '', rid)
        if rid in EXT or rid in ("concrete", "terracotta", "concrete_powder"): continue
        tool = "shovel" if "getShovelEnchantmentStatus" in src or "shovelEnchantments" in src else "pickaxe"
        mods = set()
        if "/integration/ae2/advancedae" in p: mods.update(["ae2", "advanced_ae"])
        elif "/integration/ae2/extendedae" in p: mods.update(["ae2", "extendedae"])
        elif "/integration/ae2" in p: mods.add("ae2")
        if "/integration/integrateddynamics" in p: mods.add("integrateddynamics")
        if "/integration/integratedterminals" in p: mods.update(["integrateddynamics", "integratedterminals"])
        if body is None: report("resource", rid, "no getDrops() found"); continue
        entries, notes = parse_drops(body)
        # visual block
        rsrc = read(renderers[cls]) if cls in renderers else ""
        vm = re.search(r'\b(Blocks|AEBlocks|AAEBlocks)\.([A-Z0-9_]+)(?!.*AIR)', rsrc.replace("Blocks.AIR", ""))
        vis = item_id(vm.group(1), vm.group(2)) if vm else None
        if not vis: report("resource", rid, "no display block found in renderer"); vis = "minecraft:stone"
        if not entries: report("resource", rid, "no drops parsed (colour-dependent drop?)\n" + "; ".join(notes)); continue
        smeltable = any("smelt_item" in e for e in entries)
        drops = [drop_json(e) for e in entries]
        if "INSCRIBER" in src: notes.append("inscriber/AE press output not converted (needs the inscriber upgrade work)")
        frag = vis.split(":")[1]
        rank = max([v for k, v in MIN_RANK.items() if k in frag] or [0])
        key = (re.search(r'SERVER_CONFIG\.(\w+)\.get\(\)', src) or [None, None])[1]
        base_ticks = cfg.get(key, 600) if key else 600
        inputs = [{"item": vis, "count": 1}]
        if vis == "minecraft:ancient_debris":
            # Too strong from debris alone: the netherite farm needs a netherite ingot as well as the ancient debris.
            inputs.append({"item": "minecraft:netherite_ingot", "count": 1})
        recipe = {
            "inputs": inputs,
            "tool": TOOL_TAG[tool], **({"min_tier_rank": rank} if rank else {}),
            "visual": {"type": "block", "target": vis}, "base_ticks": base_ticks,
            **({"smeltable": True} if smeltable else {}),
            **({"inscriber": INSCRIBER[rid]} if rid in INSCRIBER else {}),
            "drops": drops}
        if notes: report("resource", rid, "; ".join(notes))
        write_recipe("resource", rid, recipe, mods); n += 1
    print("resource:", n)

# ---------- plant ----------
SEED = {  # displayed plant block -> second input item
    "WHEAT": "minecraft:wheat_seeds", "BEETROOTS": "minecraft:beetroot_seeds", "PUMPKIN_STEM": "minecraft:pumpkin_seeds",
    "MELON_STEM": "minecraft:melon_seeds", "CARROTS": "minecraft:carrot", "POTATOES": "minecraft:potato",
    "SWEET_BERRY_BUSH": "minecraft:sweet_berries", "COCOA": "minecraft:cocoa_beans", "KELP_PLANT": "minecraft:kelp",
    "TALL_SEAGRASS": "minecraft:seagrass", "SEAGRASS": "minecraft:seagrass", "BIG_DRIPLEAF": "minecraft:big_dripleaf",
    "SMALL_DRIPLEAF": "minecraft:small_dripleaf", "BAMBOO_SAPLING": "minecraft:bamboo", "SUGAR_CANE": "minecraft:sugar_cane",
    "LILY_PAD": "minecraft:lily_pad", "VINE": "minecraft:vine", "SUNFLOWER": "minecraft:sunflower", "LILAC": "minecraft:lilac",
    "ROSE_BUSH": "minecraft:rose_bush", "PEONY": "minecraft:peony", "NETHER_WART": "minecraft:nether_wart",
    "CRIMSON_FUNGUS": "minecraft:crimson_fungus", "WARPED_FUNGUS": "minecraft:warped_fungus", "CHORUS_FLOWER": "minecraft:chorus_flower",
}
CROPS = {"WHEAT", "BEETROOTS", "PUMPKIN_STEM", "MELON_STEM", "CARROTS", "POTATOES", "COCOA", "SWEET_BERRY_BUSH"}
SOIL = lambda target, group: ("minecraft:farmland" if target in CROPS and target not in ("COCOA", "SWEET_BERRY_BUSH") else
    {"COCOA": "minecraft:jungle_log", "SUGAR_CANE": "minecraft:sand", "NETHER_WART": "minecraft:soul_sand",
     "CRIMSON_FUNGUS": "minecraft:crimson_nylium", "WARPED_FUNGUS": "minecraft:warped_nylium",
     "CHORUS_FLOWER": "minecraft:end_stone", "KELP_PLANT": "minecraft:sand", "TALL_SEAGRASS": "minecraft:sand",
     "SEAGRASS": "minecraft:sand", "LILY_PAD": "minecraft:dirt", "BIG_DRIPLEAF": "minecraft:clay", "SMALL_DRIPLEAF": "minecraft:clay",
     }.get(target, "minecraft:grass_block"))

def plant():
    base = ROOT + "/plantFarms/src/main/java/com/awesomeshot5051/plantfarms"
    cfg = config_defaults(base + "/ServerConfig.java")
    renderers = {os.path.basename(p)[:-len("Renderer.java")]: p
                 for p in glob.glob(base + "/**/*Renderer.java", recursive=True) if "ItemRenderer" not in p}
    n = 0
    for p in sorted(glob.glob(base + "/**/*Tileentity.java", recursive=True)):
        cls = os.path.basename(p)[:-len("Tileentity.java")]
        src = read(p); body = get_drops_body(src)
        if body is None: continue
        rid = snake(cls); rid = re.sub(r'_farm$', '', rid)
        notes = []
        tool = "hoe" if "HoeType" in src else "axe" if "AxeType" in src else None
        rsrc = read(renderers[cls]) if cls in renderers else ""
        vm = re.search(r'Blocks\.([A-Z0-9_]+)', rsrc.replace("Blocks.AIR", ""))
        target = vm.group(1) if vm else None
        PLANT_TARGET = {"gcarrot": "CARROTS", "pad": "LILY_PAD", "menril": "integrateddynamics:menril_sapling"}
        if not target and rid in PLANT_TARGET: target = PLANT_TARGET[rid]
        if not target: report("plant", rid, "no plant block in renderer"); continue
        entries, n2 = parse_drops(body); notes += n2
        if not entries: report("plant", rid, "no drops parsed\n" + body); continue
        second = SEED.get(target, "minecraft:" + target.lower())
        if rid == "gcarrot": second = "minecraft:golden_carrot"
        if ":" in target: second = target
        if "SAPLING" in target or target.endswith("_FUNGUS") : pass
        key = (re.search(r'SERVER_CONFIG\.(\w+)\.get\(\)', src) or [None, None])[1]
        base_ticks = cfg.get(key, 600) if key else 600
        vis_ticks = 80
        m = re.search(r'20\s*\*\s*(\d+)', src.split("DeathTime")[1] if "DeathTime" in src else "")
        if tool is None and m: vis_ticks = 20 * int(m.group(1))
        mods = set()
        if "/integration" in p or "Menril" in cls: mods.add("integrateddynamics")
        nether = "/nether/" in p
        inputs = [{"item": SOIL(target, None), "count": 1}, {"item": second, "count": 1}]
        if ":" in target: inputs[0]["item"] = "minecraft:grass_block"
        if target == "VINE" or inputs[0]["item"] == inputs[1]["item"]:
            inputs = [inputs[0], {"item": second, "count": 1}]
        recipe = {"inputs": inputs, **({"tool": TOOL_TAG[tool]} if tool else {}),
                  "visual": {"type": "plant", "target": target if ":" in target else "minecraft:" + target.lower()},
                  "base_ticks": base_ticks, "visual_ticks": vis_ticks,
                  **({"nether": True} if nether else {}),
                  **({"environment": {"block_below": PLANT_ENV[rid]}} if rid in PLANT_ENV else {}),
                  "drops": [drop_json(e) for e in entries]}
        if any(e["cls"] not in ("Items",) for e in entries): notes.append("non-vanilla drop item")
        if notes: report("plant", rid, "; ".join(notes))
        write_recipe("plant", rid, recipe, mods); n += 1
    print("plant:", n)

# ---------- mob ----------
SKIP_KEYS = {"minecraft:glass_pane", "minecraft:lava_bucket", "minecraft:stone"}

def key_item(k):
    if "item" in k: return k["item"]
    if "tag" in k: return "#" + k["tag"]
    return None


# Inputs of every mob farm, from the 1.21.1 branch's ModRecipeProvider (the 1.21.3 branch the converter first read was outdated).
# Glass panes, the sword and stone filler are not inputs; a symbol repeated in the pattern is a count > 1.
# entries: item id | "#tag" | [alternatives]  ->  count
def _a(*items): return list(items)
MOB_INPUTS = {
    "blaze": [("minecraft:blaze_rod", 2)],
    "creeper": [("minecraft:gunpowder", 2)],
    "drowned": [("minecraft:rotten_flesh", 1), (_a("minecraft:nautilus_shell", "minecraft:trident"), 1)],
    "elder_guardian": [("minecraft:prismarine_crystals", 1), ("minecraft:prismarine_shard", 1)],
    "ghast": [("minecraft:ghast_tear", 2)],
    "hoglin": [("minecraft:leather", 1), (_a("minecraft:porkchop", "minecraft:cooked_porkchop"), 1)],
    "phantom": [("minecraft:phantom_membrane", 2)],
    "pillager": [("minecraft:emerald", 1), ("minecraft:ominous_bottle", 1)],
    "shulker": [("minecraft:shulker_box", 1), ("minecraft:shulker_shell", 1)],
    "skeleton": [("minecraft:skeleton_skull", 1), ("minecraft:bone", 1)],
    "slime": [("minecraft:slime_block", 1), ("minecraft:slime_ball", 1)],
    "vindicator": [(_a("minecraft:iron_shovel", "minecraft:iron_axe"), 1), (_a("minecraft:emerald", "minecraft:enchanted_book"), 1)],
    "warden": [("minecraft:sculk_catalyst", 2)],
    "witch": [("minecraft:glass_bottle", 1), ("minecraft:redstone", 1)],
    "wither_skeleton": [("minecraft:wither_skeleton_skull", 2)],
    # secret farm: the old "empty farm" recipe, 3 skulls + 4 soul sand/soil + 2 nether stars, one stack per slot
    "wither": [(_a("minecraft:soul_sand", "minecraft:soul_soil"), 4), ("minecraft:nether_star", 2), ("minecraft:wither_skeleton_skull", 3)],
    # magma cubes drop magma cream, but the old farm had no crafting recipe (a bug there): magma cream + a sword
    "magma_cube": [("minecraft:magma_cream", 1)],
    "zoglin": [("minecraft:rotten_flesh", 2)],
    "zombie": [("minecraft:rotten_flesh", 1), ("minecraft:zombie_head", 1)],
    "enderman": [(_a("minecraft:ender_eye", "minecraft:ender_pearl"), 2)],
    "goat": [("minecraft:goat_horn", 2)],
    "iron": [("minecraft:iron_ingot", 2)],
    "piglin": [("minecraft:piglin_head", 2)],
    "spider": [("minecraft:string", 1), ("minecraft:spider_eye", 1)],
    "zombified_piglin": [("minecraft:gold_ingot", 1), ("minecraft:gold_nugget", 1)],
    "chicken": [(_a("minecraft:chicken", "minecraft:cooked_chicken"), 1), ("minecraft:feather", 1)],
    "cod": [("minecraft:cod", 1), ("minecraft:cooked_cod", 1)],
    "cow": [("minecraft:beef", 1), ("minecraft:leather", 1)],
    "glow_squid": [("minecraft:glow_ink_sac", 2)],
    "horse": [("minecraft:leather", 2)],
    "mooshroom": [("minecraft:beef", 1), ("minecraft:red_mushroom", 1)],
    "parrot": [("minecraft:feather", 2)],
    "pig": [("minecraft:porkchop", 1), ("minecraft:cooked_porkchop", 1)],
    "pufferfish": [("minecraft:pufferfish", 2)],
    "rabbit": [("minecraft:rabbit", 1), (_a("minecraft:rabbit_foot", "minecraft:rabbit_hide"), 1)],
    "salmon": [("minecraft:salmon", 1), ("minecraft:bone_meal", 1)],
    "sheep": [("minecraft:mutton", 1), ("#minecraft:wool", 1)],
    "snow_golem": [("minecraft:snowball", 1), ("minecraft:carved_pumpkin", 1)],
    "squid": [("minecraft:ink_sac", 2)],
    "strider": [("minecraft:string", 2)],
    "tropical_fish": [("minecraft:tropical_fish", 2)],
    "turtle": [("minecraft:turtle_scute", 1), ("minecraft:seagrass", 1)],
    # no crafting recipe in either branch: evoker, guardian, illusioner, ravager -> their own drop is used
}

def input_json(spec, count):
    if isinstance(spec, list):
        return {"type": "neoforge:compound", "ingredients": [{"item": i} for i in spec], "count": count}
    if spec.startswith("#"): return {"tag": spec[1:], "count": count}
    return {"item": spec, "count": count}

NETHER = "minecraft:the_nether"
# Where the old mob farms worked: only in the Nether (Shulker: only in the End). Without the Nether upgrade these recipes need that
# environment; blaze and magma cube also need the specific block under them (netherrack / magma block or lava).
MOB_ENV = {
    "blaze": (True, {"dimension": NETHER, "block_below": ["minecraft:netherrack"]}),
    "magma_cube": (True, {"dimension": NETHER, "block_below": ["minecraft:magma_block", "minecraft:lava"]}),
    "ghast": (True, {"dimension": NETHER}), "hoglin": (True, {"dimension": NETHER}), "piglin": (True, {"dimension": NETHER}),
    "strider": (True, {"dimension": NETHER}), "wither_skeleton": (True, {"dimension": NETHER}),
    "shulker": (False, {"dimension": "minecraft:the_end"}),
}
# Old plant farms checked the block under the farm.
PLANT_ENV = {"nether_wart": ["minecraft:soul_sand", "minecraft:soul_soil"], "wart": ["minecraft:soul_sand", "minecraft:soul_soil"],
             "crimson": ["minecraft:crimson_nylium"], "warped": ["minecraft:warped_nylium"]}

def mob():
    base = ROOT + "/mobFarms/src/main/java/com/awesomeshot5051/mobfarms"
    cfg_src = read(base + "/ServerConfig.java")
    cfg = {}
    for m in re.finditer(r'"(\w+)_farm\.spawn_time",\s*([^,]+),', cfg_src):
        v = eval_int(m.group(2))
        if v is not None: cfg[m.group(1)] = v
    recipes = {}
    for p in glob.glob(ROOT + "/mobFarms/src/main/resources/data/mob_farms/recipe/**/*.json", recursive=True):
        recipes[os.path.basename(p)[:-5]] = json.load(open(p))
    renderers = {os.path.basename(p)[:-len("Renderer.java")]: p
                 for p in glob.glob(base + "/**/*FarmRenderer.java", recursive=True)}
    n = 0
    for p in sorted(glob.glob(base + "/blocks/tileentity/**/*FarmTileentity.java", recursive=True)):
        cls = os.path.basename(p)[:-len("FarmTileentity.java")]
        if cls in ("Villager", "Empty"): continue
        src = read(p); body = get_drops_body(src)
        rid = snake(cls)
        if body is None: report("mob", rid, "no getDrops()"); continue
        rsrc = read(renderers[cls + "Farm"]) if (cls + "Farm") in renderers else ""
        em = re.search(r'EntityType\.([A-Z0-9_]+)', rsrc)
        if not em: report("mob", rid, "no EntityType in renderer"); continue
        entries, notes = parse_drops(body)
        MOB_DROPS = {  # drops that depend on runtime data (wool colour, random horn): the closest fixed item
            "sheep": [{"item": "minecraft:white_wool", "min": 1, "max": 3, "chance": 1.0, "looting_min": 4, "looting_max": 8},
                      {"item": "minecraft:mutton", "min": 1, "max": 3, "chance": 1.0, "looting_min": 4, "looting_max": 8, "fire_item": "minecraft:cooked_mutton"}],
            "goat": [{"item": "minecraft:goat_horn", "min": 1, "max": 1, "chance": 1.0}],
        }
        if rid in MOB_DROPS: entries = MOB_DROPS[rid]; notes.append("drops written by hand (the old farm built them at runtime)" + ("; dyeable" if rid == "sheep" else ""))
        if not entries: notes.append("no plain drops parsed (loot-table based?)")
        inputs = []
        if rid in MOB_INPUTS:
            inputs = [input_json(spec, n) for spec, n in MOB_INPUTS[rid]]
        else:
            notes.append("no crafting recipe in the old mod: its main drop is used as the input")
            inputs = [{"item": entries[0]["item"] if entries else "minecraft:rotten_flesh", "count": 1}]
        nsecs = re.search(r'20\s*\*\s*(\d+)', src.split("KillTime")[1] if "KillTime" in src else "")
        vis_ticks = 20 * int(nsecs.group(1)) if nsecs else 200
        base_ticks = cfg.get(rid, 600)
        vis_ticks = min(vis_ticks, base_ticks)
        recipe = {"inputs": inputs, "tool": TOOL_TAG["sword"],
                  "visual": {"type": "mob", "target": "minecraft:" + em.group(1).lower()},
                  "base_ticks": base_ticks, "visual_ticks": vis_ticks,
                  **({"nether": True} if MOB_ENV.get(rid, (False,))[0] else {}),
                  **({"environment": MOB_ENV[rid][1]} if rid in MOB_ENV else {}),
                  **({"dyeable": True} if rid == "sheep" else {}),
                  **({"secret": True} if rid == "wither" else {}),
                  "drops": [drop_json(e) for e in entries]}
        if notes or not entries: report("mob", rid, "; ".join(notes) + "\n" + (body if not entries else ""))
        write_recipe("mob", rid, recipe, set()); n += 1
    print("mob:", n)

COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]

def colored():
    """The old concrete / terracotta / concrete powder farms took their colour from the dye used on them: three dyeable recipes."""
    for rid, tag, tool, white in (("concrete", "c:concretes", "pickaxe", "minecraft:white_concrete"),
                                  ("concrete_powder", "c:concrete_powders", "shovel", "minecraft:white_concrete_powder"),
                                  ("terracotta", "minecraft:terracotta", "pickaxe", "minecraft:white_terracotta")):
        write_recipe("resource", rid, {
            "inputs": [{"tag": tag, "count": 1}], "tool": TOOL_TAG[tool],
            "visual": {"type": "block", "target": white}, "base_ticks": 600, "dyeable": True,
            "drops": [{"item": white, "max": 3, "fortune_max": 5}]}, set())
        report("resource", rid, "dyeable: one recipe for every colour (use dye on the farm)")

def D(item, mn=1, mx=3, **kw): return dict({"item": item, "min": mn, "max": mx, "chance": 1.0}, **kw)

# id: (mods, tool, inputs[(item,count)...], visual block, drops)  -- inputs are cut to 2 where the old farm needed more
EXT = {
    "fluix_crystal": (["ae2"], "pickaxe", [("ae2:charged_certus_quartz_crystal", 1), ("minecraft:redstone", 1)], "ae2:fluix_block",
        [D("ae2:fluix_crystal", fortune_max=5)], "old farm also needed a water bucket and quartz (4 inputs)"),
    "fluix_dust": (["ae2"], "pickaxe", [("ae2:fluix_crystal", 1), ("#minecraft:slabs", 1)], "ae2:fluix_block",
        [D("ae2:fluix_dust")], None),
    "certus_quartz_crystal": (["ae2"], "pickaxe", [("ae2:quartz_cluster", 1)], "ae2:quartz_cluster",
        [D("ae2:certus_quartz_crystal", fortune_max=5)], None),
    "charged_certus_quartz_crystal": (["ae2"], "pickaxe", [("ae2:quartz_cluster", 1)], "ae2:quartz_cluster",
        [D("ae2:charged_certus_quartz_crystal", fortune_max=5)], None),
    "flawed_budding_quartz": (["ae2"], "pickaxe", [("ae2:flawed_budding_quartz", 1)], "ae2:flawed_budding_quartz",
        [D("ae2:certus_quartz_crystal", fortune_max=5, silk_item="ae2:flawed_budding_quartz")], None),
    "flawless_budding_quartz": (["ae2"], "pickaxe", [("ae2:flawless_budding_quartz", 1)], "ae2:flawless_budding_quartz",
        [D("ae2:flawless_budding_quartz", fortune_max=5)], None),
    "quartz_glass": (["ae2"], "pickaxe", [("ae2:quartz_glass", 1)], "ae2:quartz_glass", [D("ae2:quartz_glass", fortune_max=5)], None),
    "sky_stone": (["ae2"], "pickaxe", [("ae2:sky_stone_block", 1)], "ae2:sky_stone_block", [D("ae2:sky_stone_block", fortune_max=5)], None),
    "mysterious_cube": (["ae2"], "pickaxe", [("ae2:mysterious_cube", 1)], "ae2:mysterious_cube",
        [D("ae2:engineering_processor_press", fortune_max=5), D("ae2:logic_processor_press", fortune_max=5),
         D("ae2:silicon_press", fortune_max=5), D("ae2:calculation_processor_press", fortune_max=5)], "silk touch cube drop not represented"),
    "silicon": (["ae2"], "pickaxe", [("ae2:silicon", 1)], "ae2:quartz_block", [D("ae2:silicon", fortune_max=5)], None),
    "singularity": (["ae2"], "pickaxe", [("ae2:matter_ball", 64), ("minecraft:lava_bucket", 1)], "ae2:singularity",
        [D("ae2:singularity", fortune_max=5)], None),
    "entro_block": (["ae2", "extendedae"], "pickaxe", [("extendedae:entro_block", 1)], "extendedae:entro_block", [D("extendedae:entro_block", fortune_max=5)], None),
    "entro_crystal": (["ae2", "extendedae"], "pickaxe", [("extendedae:entro_crystal", 1)], "extendedae:entro_block", [D("extendedae:entro_crystal", fortune_max=5)], "requirements of the old farm were not readable; the crystal itself is the input"),
    "entro_dust": (["ae2", "extendedae"], "pickaxe", [("extendedae:entro_dust", 1)], "extendedae:entro_block", [D("extendedae:entro_dust", fortune_max=5)], None),
    "silicon_block": (["ae2", "extendedae"], "pickaxe", [("extendedae:silicon_block", 1)], "extendedae:silicon_block", [D("extendedae:silicon_block", fortune_max=5)], None),
    "quantum_alloy": (["ae2", "advanced_ae"], "pickaxe", [("advanced_ae:shattered_singularity", 4), ("minecraft:copper_ingot", 4)], "ae2:singularity",
        [D("advanced_ae:quantum_alloy", fortune_max=5)], "old farm also needed 4 singularities and a quantum infusion bucket (4 inputs)"),
    "quantum_infused_dust": (["ae2", "advanced_ae"], "pickaxe", [("advanced_ae:quantum_infusion_bucket", 1)], "ae2:singularity",
        [D("advanced_ae:quantum_infused_dust", fortune_max=5)], "input chosen to match the drop; old farm needed only the inscriber upgrade"),
    "shattered_singularity": (["ae2", "advanced_ae"], "pickaxe", [("ae2:singularity", 1), ("ae2:ender_dust", 2)], "ae2:singularity",
        [D("advanced_ae:shattered_singularity", fortune_max=5)], "old farm also needed 2 sky dust and a lava bucket (4 inputs)"),
    "crystal_chorus": (["integrateddynamics"], "pickaxe", [("integrateddynamics:crystalized_chorus_block", 1)], "integrateddynamics:crystalized_chorus_block",
        [D("integrateddynamics:crystalized_chorus_chunk", fortune_max=5, silk_item="integrateddynamics:crystalized_chorus_block"),
         dict(D("integrateddynamics:crystalized_chorus_block", 1, 3), chance=0.05)], None),
    "crystal_chorus_brick": (["integrateddynamics"], "pickaxe", [("integrateddynamics:crystalized_chorus_brick", 1)], "integrateddynamics:crystalized_chorus_brick",
        [D("integrateddynamics:crystalized_chorus_brick", fortune_max=5)], None),
    "crystal_menril": (["integrateddynamics"], "pickaxe", [("integrateddynamics:crystalized_menril_block", 1)], "integrateddynamics:crystalized_menril_block",
        [D("integrateddynamics:crystalized_menril_chunk", fortune_max=5, silk_item="integrateddynamics:crystalized_menril_block"),
         dict(D("integrateddynamics:crystalized_menril_block", 1, 3), chance=0.05)], None),
    "menril_brick": (["integrateddynamics"], "pickaxe", [("integrateddynamics:crystalized_menril_brick", 1)], "integrateddynamics:crystalized_menril_brick",
        [D("integrateddynamics:crystalized_menril_brick", fortune_max=5)], None),
    "crystal_chorus_glass": (["integrateddynamics", "integratedterminals"], "pickaxe", [("integratedterminals:chorus_glass", 1)], "integratedterminals:chorus_glass",
        [D("integratedterminals:chorus_glass", fortune_max=5)], None),
    "menril_glass": (["integrateddynamics", "integratedterminals"], "pickaxe", [("integratedterminals:menril_glass", 1)], "integratedterminals:menril_glass",
        [D("integratedterminals:menril_glass", fortune_max=5)], None),
}

def ext():
    for rid, (mods, tool, inputs, vis, drops, note) in EXT.items():
        ins = []
        for item, n in inputs:
            ins.append({"tag": item[1:], "count": n} if item.startswith("#") else {"item": item, "count": n})
        write_recipe("resource", rid, {
            "inputs": ins, "tool": TOOL_TAG[tool], "visual": {"type": "block", "target": vis}, "base_ticks": 600,
            **({"inscriber": INSCRIBER[rid]} if rid in INSCRIBER else {}),
            "drops": [drop_json(d) for d in drops]}, set(mods))
        report("resource", rid, "hand-written from the old farm" + ("; " + note if note else ""))

def plant_ext():
    # Integrated Dynamics menril tree: the old tile entity used IDBlocks accessors; the registry ids below are my reading of them.
    write_recipe("plant", "menril", {
        "inputs": [{"item": "minecraft:grass_block", "count": 1}, {"item": "integrateddynamics:menril_sapling", "count": 1}],
        "tool": TOOL_TAG["axe"], "visual": {"type": "plant", "target": "integrateddynamics:menril_sapling"},
        "base_ticks": 600, "visual_ticks": 80,
        "drops": [{"item": "integrateddynamics:menril_sapling", "min": 1, "max": 4, "chance": 0.5},
                  {"item": "integrateddynamics:menril_berries", "min": 1, "max": 4, "chance": 0.5},
                  {"item": "integrateddynamics:menril_log"},
                  {"item": "integrateddynamics:menril_log_filled", "chance": 0.005},
                  {"item": "integrateddynamics:crystalized_menril_chunk", "chance": 0.005},
                  {"item": "integrateddynamics:menril_leaves"}]}, {"integrateddynamics"})
    report("plant", "menril", "hand-written; the integrateddynamics item ids are unverified guesses from IDBlocks field names")

if __name__ == "__main__":
    for d in ("resource_farm", "plant_farm", "mob_farm"):
        for f in glob.glob(os.path.join(OUT, d, "*.json")): os.remove(f)
    resource(); colored(); ext(); plant(); plant_ext(); mob()
    with open(os.path.join(ROOT, "EasyFarms/tools/convert_report.txt"), "w") as f:
        f.write("\n".join(REPORT) + "\n")
    print(len(REPORT), "report entries -> tools/convert_report.txt")
