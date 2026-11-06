# Easy Farms: guide for modpack authors and modders

Everything a farm does is data. You can add your mod's items to the farms, add tool tiers, and add upgrade cards
and their effects. This guide covers all of it.

[← Back to the main README](../../README.md)

* [Where files go](#where-files-go)
* [Machine recipes](#machine-recipes)
* [Tool tiers](#tool-tiers)
* [Upgrade cards and effects](#upgrade-cards-and-effects)
* [Secret recipes and achievements](#secret-recipes-and-achievements)
* [Changing or removing existing content](#changing-or-removing-existing-content)
* [The guide book](#the-guide-book)
* [Testing](#testing)

---

## Where files go

All paths are inside a datapack, or `data/` in a mod's resources.

| What | Path |
|---|---|
| Resource Farm recipes | `data/<namespace>/recipe/resource_farm/<name>.json` |
| Plant Farm recipes | `data/<namespace>/recipe/plant_farm/<name>.json` |
| Mob Farm recipes | `data/<namespace>/recipe/mob_farm/<name>.json` |
| Liquid Farm recipes | `data/<namespace>/recipe/liquid_farm/<name>.json` |
| Tool tiers | `data/<namespace>/easy_farms/farm_tier/<name>.json` |
| Upgrade cards | `data/<namespace>/easy_farms/farm_upgrade/<name>.json` |

Machine recipes are normal recipes that use the types `easy_farms:resource_farm`, `easy_farms:plant_farm` and
`easy_farms:mob_farm`. They load on `/reload`. Tiers and upgrades are datapack registries, so they load with the world.

---

## Machine recipes

### Example: a resource farm recipe

An ore from another mod, mined with a pickaxe:

```json
{
  "type": "easy_farms:resource_farm",
  "neoforge:conditions": [{ "type": "neoforge:mod_loaded", "modid": "mymod" }],
  "inputs": [{ "item": "mymod:ruby_ore", "count": 1 }],
  "tool": "minecraft:pickaxes",
  "min_tier_rank": 2,
  "visual": { "type": "block", "target": "mymod:ruby_ore" },
  "base_ticks": 600,
  "smeltable": true,
  "drops": [
    {
      "item": "mymod:ruby",
      "min": 1,
      "max": 3,
      "fortune_max": 5,
      "silk_item": "mymod:ruby_ore",
      "smelt_item": "mymod:polished_ruby"
    }
  ]
}
```

### Example: a plant farm recipe

Plant recipes are **slot-ordered**: the first input is the soil (slot 1), the second the plant.

```json
{
  "type": "easy_farms:plant_farm",
  "inputs": [
    { "item": "minecraft:farmland", "count": 1 },
    { "item": "mymod:rice_seeds", "count": 1 }
  ],
  "tool": "minecraft:hoes",
  "visual": { "type": "plant", "target": "mymod:rice_crop" },
  "base_ticks": 600,
  "visual_ticks": 80,
  "drops": [ { "item": "mymod:rice", "min": 1, "max": 3 } ]
}
```

### Example: a mob farm recipe

```json
{
  "type": "easy_farms:mob_farm",
  "inputs": [ { "item": "minecraft:porkchop", "count": 2 } ],
  "tool": "minecraft:swords",
  "visual": { "type": "mob", "target": "minecraft:pig" },
  "base_ticks": 600,
  "drops": [
    { "item": "minecraft:porkchop", "min": 1, "max": 3, "looting_max": 5, "fire_item": "minecraft:cooked_porkchop" }
  ]
}
```

### Example: a liquid farm recipe

Liquid recipes have no `drops`; they add a fluid to the tank every cycle. The visual type is `fluid`.

```json
{
  "type": "easy_farms:liquid_farm",
  "inputs": [
    { "item": "minecraft:honey_bottle" },
    { "item": "minecraft:cauldron" },
    { "item": "minecraft:pointed_dripstone" }
  ],
  "visual": { "type": "fluid", "target": "mymod:honey" },
  "base_ticks": 2400,
  "visual_ticks": 0,
  "fluid": { "fluid": "mymod:honey", "amount": 1000 }
}
```

`fluid.amount` is millibuckets per cycle. The tank holds `liquidBaseCapacityMb` (config, default 16000 = 16 buckets) times the Capacity cards.

### Recipe fields

| Field | Required | Meaning |
|---|---|---|
| `inputs` | yes | 1 to 3 inputs. Each is an ingredient (`item`, or `tag`) plus an optional `count` (default 1). |
| `tool` | no | An **item tag** the tool must be in, for example `minecraft:pickaxes`, `minecraft:swords`, `minecraft:hoes`. Omit it for a recipe that needs no tool ("toolless"). |
| `min_tier_rank` | no (0) | The lowest tier rank the tool must have (see [tiers](#tool-tiers)). |
| `visual` | yes | What the glass case shows. `type` is `block`, `plant` or `mob`; `target` is the block or entity id. |
| `base_ticks` | yes | Ticks one cycle takes at the slowest tier. The tool tier divides this. |
| `visual_ticks` | no (80) | The length of the animation at the end of a cycle. |
| `smeltable` | no (false) | Drops can use their `smelt_item` form when a card with the `smelt` effect is installed. |
| `nether` | no (false) | The recipe is a Nether recipe: the Nether card (`nether_environment` effect) lets it run outside its environment. |
| `inscriber` | no | Makes the recipe respond to the Inscriber card. An object, see [Inscriber recipes](#inscriber-recipes). |
| `dyeable` | no (false) | The farm can be dyed; the visual and drops are recoloured (needs ids in the form `white_wool`, `red_wool`...). |
| `secret` | no (false) | Hidden from JEI and EMI until the player fills a farm with the inputs. See [secret recipes](#secret-recipes-and-achievements). |
| `environment` | no | Where the recipe may run when there is no Nether card. See below. |
| `drops` | yes | The list of drop entries. |

**Matching rules**

* Every filled input slot must belong to the recipe. Extra items mean no match. This is how a big recipe is told apart from a small one that uses one of the same items.
* Resource and Mob recipes accept the inputs in **any slot order**. Plant recipes need them **in order**.
* The tool must be in the `tool` tag and belong to a [tool tier](#tool-tiers) of at least `min_tier_rank`. Tools that no tier lists are placed by their material (see below), so other mods' tools usually work with no extra data.
* If several recipes match, the first one found wins, so keep inputs distinct.

### `environment`

```json
"environment": {
  "dimension": "minecraft:the_nether",
  "block_below": ["minecraft:magma_block", "minecraft:lava"]
}
```

Both are optional. `dimension` is a dimension id. `block_below` lists the blocks (or `#tags`) the farm must stand on;
any one is enough. With both, both must hold. A farm with the matching card (see `nether`) ignores the environment.

### Drop entries

| Field | Default | Meaning |
|---|---|---|
| `item` | required | Item id of the drop. |
| `min`, `max` | 1, 1 | Count range. |
| `chance` | 1.0 | Chance per cycle (0 to 1). |
| `fortune_min`, `fortune_max` | none | A different count range when the tool has Fortune (resource and plant). |
| `looting_min`, `looting_max` | none | A different count range when the sword has Looting (mob). |
| `silk_item` | none | What drops instead when the tool has Silk Touch. |
| `silk_only` | false | Drops only with Silk Touch. |
| `smelt_item` | none | The smelted form, when a smelting card is installed. |
| `fire_item` | none | The cooked form, when the tool has Fire Aspect. |

### Inscriber recipes

The `inscriber` object replaces the drops with the processed form when a farm has the Inscriber card (effect
`easy_farms:inscribe`), optionally only with a press installed:

```json
"inscriber": {
  "press": "ae2:logic_processor_press",
  "drops": [ { "item": "ae2:logic_processor_print", "max": 3, "fortune_max": 5 } ]
}
```

`press` is optional (a recipe without it only needs the card, like grinding sky stone into dust). The press sits in the
Resource Farm's press slot; which items fit is the item tag `easy_farms:inscriber_presses`, so a pack can add its own:

```json
{ "values": [ { "id": "mymod:my_press", "required": false } ] }
```

at `data/<namespace>/tags/item/inscriber_presses.json` (use `"replace": false`). The `drops` entries use the
normal [drop entry](#drop-entries) fields.

### Recipes for other mods

Wrap them in `neoforge:conditions` so they only load when the mod is installed:

```json
"neoforge:conditions": [{ "type": "neoforge:mod_loaded", "modid": "mymod" }]
```

Recipes that are invalid fail loudly in the log when the datapack loads ("Parsing error loading recipe ...").

---

## Tool tiers

A tier says which tools belong to it and how fast and how long it works. A tool in the tool slot must be in at least
one tier. If it is in several, the highest `rank` wins.

```json
{
  "tool": [
    { "item": "mymod:ruby_pickaxe" },
    { "item": "mymod:ruby_sword" },
    { "tag": "mymod:ruby_tools" }
  ],
  "rank": 4,
  "speed_divisor": 28,
  "break_multiplier": 4,
  "weapon_break_multiplier": 5.0,
  "input_lifetime_multiplier": 20
}
```

| Field | Default | Meaning |
|---|---|---|
| `tool` | required | An ingredient: items and/or tags that belong to the tier. |
| `rank` | required | Ordering. Recipes use it for `min_tier_rank`. The built-in tiers are wood 0, stone 1, iron 2, gold 3, diamond 4, netherite 5. |
| `speed_divisor` | 1 | Divides the base time of a recipe (bigger is faster). |
| `break_multiplier` | 0 | Extra "work" time for pickaxe, shovel and axe recipes (× 20 ticks; halved with Efficiency). |
| `weapon_break_multiplier` | 0 | The same for sword, axe and hoe recipes (halved with Efficiency, or Sharpness on a sword). |
| `input_lifetime_multiplier` | 1 | Multiplies the config's `baseInputLifetimeDays`: how long the tool lasts. |
| `mining_tier` | none | The block tag of the vanilla material this tier stands for, for example `minecraft:incorrect_for_iron_tool`. |
| `mining_speed` | 0 | The vanilla material speed (wood 2, stone 4, iron 6, diamond 8, netherite 9, gold 12). The nearest speed places a tool that no tier lists. |
| `priority` | 0 | When two tiers list the same tool, the higher priority wins (then the higher rank). Use it to move a tool out of a tier another pack put it in. |
| `tool_durability` | none | The tool durability this tier stands for (wood 59, stone 131, gold 32, iron 250, diamond 1561, netherite 2031). Places damageable tools that have no vanilla material. |

**Which tier a tool gets, in order:**
1. A tier whose `tool` list contains it (highest `priority`, then highest `rank`). List your mod's items, or a tag of them,
   in your own tier file: that gives them your tier's speed and `input_lifetime_multiplier` (how long the tool lasts).
2. A tiered tool (pickaxe, shovel, axe, hoe, sword) that no tier lists: the tier with the nearest `mining_speed` to the
   material's speed. The tier whose `mining_tier` equals the material's "incorrect blocks" tag wins a tie. (Many mods reuse
   the wooden tag for materials that are not wooden, so the tag alone is not trusted.)
3. Any other damageable item: the tier with the nearest `tool_durability`.
4. Otherwise the item is not accepted as a tool.

To change a tier an item gets, put it in a tier of your own with a higher `priority`. To make a tool last longer or
shorter, give that tier a different `input_lifetime_multiplier`.

The built-in tiers are ordinary files in `data/easy_farms/easy_farms/farm_tier/`. Override one with the same file name
in your own datapack to change it.

---

## Upgrade cards and effects

A card is an **item** plus a `farm_upgrade` entry that says which farms accept it and what it does. The item can be
one you register yourself, any existing item, or, with no Java at all, the blank `easy_farms:upgrade_card` carrying a
card id (see [cards without registering an item](#cards-without-registering-an-item)).

```json
{
  "item": "mymod:golden_card",
  "kinds": ["resource", "plant", "mob"],
  "requires": "any",
  "effects": [
    { "type": "easy_farms:drop_multiplier", "multiplier": 2.0 },
    { "type": "easy_farms:input_lifetime", "multiplier": 4.0 }
  ]
}
```

| Field | Meaning |
|---|---|
| `item` | The card item. |
| `name` | Optional. The card's display name, a text component (a plain string works). Used by the generic `easy_farms:upgrade_card`, so a recipe only needs to set the `easy_farms:card` component. |
| `description` | Optional. What the card does, shown in its tooltip. |
| `card` | Optional. For the generic `easy_farms:upgrade_card` item: the id its `easy_farms:card` component must hold. Without it, any stack of `item` is the card. |
| `kinds` | `resource`, `plant`, `mob` and/or `liquid`: the farms that accept it. |
| `requires` | Shown in the card's tooltip: the text key `gui.easy_farms.requires.<value>`. The built-ins are `any`, `smeltable`, `toolless`, `nether` and `inscriber`. Add your own key in your lang file. |
| `restrictions` | Optional. Compatibility rules, see [Card restrictions](#card-restrictions). |
| `effects` | What the card does (a list; a card can have several). Empty means nothing happens. |

Cards are installed from the Upgrades screen, or by sneaking and right-clicking the farm with the card. A farm has 18
upgrade slots. A card can go in any free slot, but a farm holds each card only once. Cards run in slot order, which
matters when two cards change the same drops (the first one wins, so put "crush" before "wash").

### Cards without registering an item

The blank item `easy_farms:upgrade_card` turns any `farm_upgrade` into a card, so a datapack can add a card with
no mod. Give the item the component `easy_farms:card` and make the upgrade name the same id in `card`. A crafting
recipe makes it. The name, tooltip text and glint come from the `farm_upgrade`, so the result needs only the one component:

```json
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "pattern": ["AAA", "ABA", "AAA"],
  "key": { "A": { "item": "create:andesite_alloy" }, "B": { "item": "minecraft:andesite" } },
  "result": {
    "id": "easy_farms:upgrade_card",
    "count": 1,
    "components": {
      "easy_farms:card": "mypack:andesite_casing"
    }
  }
}
```

and the matching `data/mypack/easy_farms/farm_upgrade/andesite_casing.json` has `"item": "easy_farms:upgrade_card"`,
`"card": "mypack:andesite_casing"` and its effects. Every card with a `card` id is listed in the creative tab, and JEI shows, on the page of each farm it applies to, a copy of every recipe whose drops a `convert_drops` card changes. See the Create datapack for a complete example.

### Card restrictions

A card can be **exclusive** with other cards or tool enchantments, or **require** other cards. Each entry in
`restrictions` is a rule; all fields are optional:

```json
"restrictions": [
  { "excludes_enchantments": ["minecraft:silk_touch"] },
  { "when_drops": ["minecraft:raw_copper"], "requires": ["easy_farms:smelter"] },
  { "when_drops": ["minecraft:quartz"], "excludes": ["easy_farms:smelter", "mypack:processor"] }
]
```

| Field | Meaning |
|---|---|
| `when_drops` | Scope: the rule only applies while the farm's recipe drops one of these (item ids or `#tags`). Leave it out for a rule that always applies. A scoped rule does not apply while the farm has no recipe. |
| `requires` | Upgrade ids (the farm_upgrade file id, e.g. `easy_farms:smelter`) that must be installed as well. |
| `excludes` | Upgrade ids that cannot be installed together with this card. Both ways: you only have to write it on one of the two cards. |
| `excludes_enchantments` | Enchantments on the farm's tool that make the card unusable. |
| `requires_block_below` | Block ids or `#tags`; the farm must stand on one of them, or the card is refused and does nothing. Use it for heat sources (`create:blaze_burner`, `minecraft:lava`, `minecraft:magma_block`, `#minecraft:campfires`). Rechecked about once a second, so moving the farm switches the card off. |

What happens:
* **Adding a card** that breaks a rule is refused (Upgrades screen, shift-click, sneaking with the card, hoppers). While
  you hold the card over a slot, the tooltip says why. The card's own tooltip lists its rules too.
* **Rules are checked all the time**, not just when adding: if the recipe, the tool's enchantments or the other cards
  change so that a rule is broken, the card stays in its slot but is switched off. Where two cards clash, the one in
  the earlier slot keeps working. A card with a missing requirement does nothing until the requirement returns.

### Stacking cards

`farm_upgrade` files accept `max_count` (default 1): how many of the card one farm holds, stacked in one slot. `stack_requires` lists upgrade ids that
must be installed to stack beyond 1, and `stack_exempt_kinds` lists farm kinds (`resource`, `plant`, `mob`, `liquid`) that stack without them. `kind_max_counts` (e.g. `{"liquid": 3}`) overrides `max_count` for the listed kinds.
The Capacity card uses `"max_count": 4, "stack_requires": ["easy_farms:powered"], "stack_exempt_kinds": ["liquid"]`.

### Built-in effects

| Type | Parameters | Effect |
|---|---|---|
| `easy_farms:cycle_time` | `divisor`, `toolless_only` (default true), `kind_divisors` (e.g. `{"liquid": 2.0}`) | Divides the work part of a cycle. Used by the Speed card (8x, but 2x on the Liquid Farm). |
| `easy_farms:smelt` | none | Smeltable recipes drop their smelted form. |
| `easy_farms:nether_environment` | none | Recipes marked `nether` ignore their environment. |
| `easy_farms:infinite_inputs` | none | The tool never wears out and the inputs are never consumed. |
| `easy_farms:redstone_control` | none | The farm only works with a redstone signal. |
| `easy_farms:input_lifetime` | `multiplier` | Multiplies how long the tool lasts (or, for toolless recipes, the inputs). |
| `easy_farms:inscribe` | none | The Inscriber card: recipes with an `inscriber` object drop its drops instead. |
| `easy_farms:drop_multiplier` | `multiplier` | Multiplies the item counts of each cycle. A fractional part is a chance of one more. |
| `easy_farms:convert_drops` | `rules` | Turns the finished drops into other items. See below. |
| `easy_farms:powered` | `capacity` (1000), `min_energy` (100), `energy_per_tick` (2), `tier` (`easy_farms:diamond`), `toolless_divisor` (4) | The farm runs on energy (needs an energy API: FE on NeoForge, Team Reborn Energy on Fabric). `tier` is a farm_tier whose speed replaces the tool's if faster; toolless recipes are divided by `toolless_divisor`. Stops below `min_energy`, drains `energy_per_tick` while working. The card's tooltip always says "FE or other energy generation mod required to use" as a hint (no mod list is checked, so any energy mod works); on Fabric the card is refused (tooltip in red) only if Team Reborn Energy is missing. |
| `easy_farms:efficiency` | `energy_use_multiplier` (0.5) | Per installed card: multiplies the energy the Powered card drains per working tick (fractions carry over between ticks). The Efficiency card requires the Powered card through a `requires` restriction and stacks to 3 (`max_count`). |
| `easy_farms:chunk_loader` | none | Keeps the farm's chunk loaded through vanilla forced chunks (saved in the dimension's `easy_farms_chunk_loaders` data). A chunk already forced by /forceload is never released by it. |
| `easy_farms:capacity` | `energy_multiplier` (2), `fluid_multiplier` (2), `item_multiplier` (2), `unstackable_base` (8) | Per installed card: multiplies the ME buffer, the liquid tank and the output stack size. Unstackable items get `unstackable_base x multiplier`. |

These are enough to build new cards **with no code**: a datapack plus an item (for example one added with a
datapack-friendly item mod) is a complete card.

### `convert_drops`: machines as cards

`convert_drops` is how a card "emulates a machine": it rewrites the farm's drops with a list of rules, so any mod's
processing (crushing, pressing, casing, alloying...) becomes a card with no code.

```json
{
  "type": "easy_farms:convert_drops",
  "rules": [
    { "from": "#minecraft:logs", "to": "create:andesite_casing" },
    { "from": "minecraft:raw_iron",
      "to": [ { "item": "create:crushed_raw_iron" }, { "item": "create:experience_nugget", "chance": 0.75 } ] },
    { "from": "minecraft:gold_ingot", "per": 2, "to": { "item": "create:precision_mechanism", "count": 1 } },
    { "from": "minecraft:copper_ingot", "with": [ { "from": "create:zinc_ingot", "source": "catalyst" } ], "to": { "item": "create:brass_ingot", "count": 2 } }
  ]
}
```

| Rule field | Default | Meaning |
|---|---|---|
| `from` | required | An item id, or `#namespace:tag` for an item tag. |
| `per` | 1 | How many `from` items make one conversion. Leftovers are kept as they were. |
| `with` | none | Extra ingredients the rule needs together with `from`, each `{ "from": item or #tag, "per": n, "source": "drops" or "catalyst" }`. One conversion uses up `per` of `from` and of every extra; as many conversions happen as the scarcest ingredient allows. `source: "drops"` (default) takes the extra from the same cycle's drops. `source: "catalyst"` takes it from the card's catalyst slot, see below. |
| `to` | required | One output or a list. An output is an item id, or `{ "item", "count" (1), "chance" (1.0) }`; the chance is rolled once per conversion. |

A drop stack is handled by the first rule that matches it. A rule whose output items do not exist (the other mod is
not installed) is skipped, so the drops stay unchanged. Cards run in slot order, so conversions can be chained: a
Smelter card turns ore into ingots, then a pressing card turns the ingots into sheets.

#### Multi-ingredient rules and catalysts

Example, brass: a Brass card for the Resource Farm that mixes the farm's copper ingots with zinc ingots the player supplies.

```json
{
  "item": "easy_farms:upgrade_card", "card": "mypack:brass", "name": "Brass Card", "kinds": ["resource"],
  "effects": [ { "type": "easy_farms:convert_drops", "rules": [
    { "from": "minecraft:copper_ingot",
      "with": [ { "from": "create:zinc_ingot", "source": "catalyst" } ],
      "to": { "item": "create:brass_ingot", "count": 2 } }
  ] } ]
}
```

On a farm whose drops include copper ingots (a Copper Farm with the Smelter card), each conversion uses up 1 copper ingot from the
drops and 1 zinc ingot from the card's catalyst slot, and makes 2 brass ingots. If the catalyst slot is empty the copper ingots are
left alone. `"source": "drops"` (the default) is for an ingredient that the same cycle drops, for example `{"from": "minecraft:bone"}`
next to `minecraft:arrow` on a skeleton farm.

**Catalyst slots.** Any installed card with a `"source": "catalyst"` ingredient makes a second arrow button appear under the Upgrades
arrow on the farm screen (it disappears again when no installed card needs a catalyst). It opens the separate Catalysts screen. The
farm has **18 catalyst slots** to share out between its cards: a card gets **one slot per distinct catalyst ingredient** (the same `from`
used by several rules is one slot), up to 18 for a single card, and each slot accepts only its ingredient. Slots are shown grouped by
card, in card-slot order. A card that would need more slots than are left is refused with a tooltip. Taking a card out removes its
slots and drops their contents; catalysts stay put when other cards come and go, and are kept when the farm is picked up with Silk Touch.

**Feeding catalysts.** Hoppers, pipes and other mods reach the catalyst slots through the **back** of the farm: the side opposite the
way it faces (a farm facing west is fed from its east side). That side only accepts catalysts, each into the slot that takes it; the
other sides still use the input and output slots. Catalysts are never taken back out by automation. The **Infinity upgrade** keeps the
catalysts as well as the tool and inputs: they must still be present, but are not used up.

### Writing your own effect (Java)

Effect types live in the registry `easy_farms:upgrade_effect_type`. Implement `UpgradeEffect`, give it a `MapCodec`,
and register it. Every method on the interface has a "do nothing" default, so override only what you change.

```java
public record BonusDrop(ResourceLocation item, float chance) implements UpgradeEffect {
    public static final MapCodec<BonusDrop> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("item").forGetter(BonusDrop::item),
            Codec.FLOAT.optionalFieldOf("chance", 1f).forGetter(BonusDrop::chance)
    ).apply(i, BonusDrop::new));

    @Override
    public MapCodec<? extends UpgradeEffect> codec() { return CODEC; }

    @Override
    public void modifyDrops(FarmBlockEntity farm, ServerLevel level, FarmRecipe recipe, List<ItemStack> drops) {
        if (level.random.nextFloat() < chance) drops.add(new ItemStack(BuiltInRegistries.ITEM.get(item)));
    }
}

// Registration
public static final DeferredRegister<MapCodec<? extends UpgradeEffect>> EFFECTS =
        DeferredRegister.create(UpgradeEffects.KEY, "mymod");
public static final DeferredHolder<MapCodec<? extends UpgradeEffect>, MapCodec<BonusDrop>> BONUS_DROP =
        EFFECTS.register("bonus_drop", () -> BonusDrop.CODEC);
// ... EFFECTS.register(modEventBus);
```

Then use it from JSON as `{ "type": "mymod:bonus_drop", "item": "minecraft:diamond", "chance": 0.1 }`.

The hooks available on `UpgradeEffect`:

| Hook | Called | Use it to |
|---|---|---|
| `modifyCycleTicks(farm, recipe, ticks)` | Whenever the cycle time is computed (both sides) | Make a cycle faster or slower |
| `pausesFarm(farm)` | Every tick (both sides) | Stop the farm under a condition |
| `smelts(recipe)` | When drops are rolled | Turn on smelted drops |
| `waivesEnvironment(recipe)` | When the environment is checked | Let a recipe run anywhere |
| `infiniteInputs()` | When the tool or inputs would wear out | Stop that |
| `inputLifetimeMultiplier()` | When the lifetime is computed | Make the tool (or the inputs) last longer or shorter |
| `modifyDrops(farm, level, recipe, drops)` | Server side, after drops are rolled | Add to, remove from or change the drops |

Effect types are synced to clients, so a server and its clients must have the same mod installed.

### Items and the item handler

Hoppers, pipes and other mods can talk to the farm through the standard item handler capability
(`Capabilities.ItemHandler.BLOCK`): inserting goes into the input slots, extracting comes from the output slots. The tool slot
and upgrade slots are not exposed to automation.

---

## Secret recipes and achievements

Set `"secret": true` on a recipe to hide it from JEI and EMI. The recipe still works for everyone. It becomes
visible to a player when they **fill a farm with its inputs** (the tool is not needed). That also fires the criterion
`easy_farms:secret_recipe`, so you can attach an advancement to it:

```json
{
  "parent": "minecraft:story/root",
  "display": {
    "icon": { "id": "minecraft:nether_star" },
    "title": "Found it!",
    "description": "Discover a secret farm recipe",
    "frame": "challenge",
    "hidden": true
  },
  "criteria": {
    "found": {
      "trigger": "easy_farms:secret_recipe",
      "conditions": { "recipe": "mymod:mob_farm/my_secret_recipe" }
    }
  },
  "requirements": [["found"]],
  "rewards": { "experience": 100 }
}
```

`recipe` is the recipe id, the path from `recipe/` without `.json`. Leave `conditions` out to match any secret
recipe. The discovery is saved on the player, is kept on death, and is also what unhides the recipe in JEI/EMI.

---

## Changing or removing existing content

* **Change something:** put a file with the **same path** in your datapack. It replaces the original (vanilla datapack rules apply).
* **Remove a recipe:** replace it with a file that has a condition that is never true:
  ```json
  { "neoforge:conditions": [ { "type": "neoforge:false" } ] }
  ```
* **The Resource Farm's auto-generated recipes** (one per mineable block) are plain recipe files under
  `data/easy_farms/recipe/resource_farm/auto/`, so you can override or disable them the same way.
* **Make every farm slower, or the inputs last longer:** use the config (`timeMultiplier`, `baseInputLifetimeDays`) rather than editing recipes.

---

## The guide book

Easy Farms ships a Patchouli book, `easy_farms:guide` (craft it with a book and a glass pane; Patchouli is optional). It covers
the farms, the default cards, catalysts and restrictions, and lists every Resource, Plant and Mob Farm recipe with inputs and
outputs. The recipe chapters are generated: run `python3 tools/generate_guide.py` after changing recipes and commit the result.

**Adding your own chapter.** Ship a resource pack (a datapack folder can double as one) with
`assets/easy_farms/patchouli_books/guide/en_us/categories/<name>.json` and entries under
`.../entries/<name>/*.json` whose `category` is `easy_farms:<name>`. Use `"flag": "mod:<modid>"` on the category and entries to show
them only when that mod is installed. See Patchouli's documentation for the page formats.

---

## Testing

* `/reload` picks up recipe changes. Tiers and upgrade cards load with the world, so re-enter the world for those.
* A typo in a recipe is reported as "Parsing error loading recipe" with the field name, in the log and in the console.
* In JEI or EMI, open a farm category to see whether your recipe loaded and what its numbers are.
* Jade and The One Probe show what a placed farm is doing, including "wrong environment".
* `./gradlew runData` in the mod's own repository validates every shipped recipe and upgrade file against the real codecs before you ship.
