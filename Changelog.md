# Changelog

This is the changelog for the `master` branch (Minecraft 1.21.1, NeoForge 21.1.265), written from the commit history.
Versions are listed newest first, each with its commit and date.

## 3.2.0

### Chisel card and variant picker
- New `easy_farms:variant_select` effect for a datapack card (the Chisel pack ships one). With it installed, an arrow on the left of the
  farm screen (under the Crafting one) opens a **Variants** pop-out to the right of the screen: a scrollable grid of every variant of the
  block the farm makes. Click one and the farm outputs that variant (same count); click the plain block to go back.
- Variants are **data**: every item tag under `variants/` (any namespace, e.g. `easy_farms_chisel:variants/cobblestone`) is a group of
  interchangeable items, so any pack can add variants for any mod with one tag file. The choice is saved with the farm and copied by the
  Memory Card and a Silk Touch pickup.

### Auto Importer cards (storage networks)
- New `easy_farms:network_import` effect (`network`: `ae2`, `refinedstorage` or `dimstorage`). The farm stores its output straight into the
  storage network, leaving whatever does not fit in the output slots. Two ways to connect: put the farm **next to** a network block (cable,
  interface, Dimensional Chest), or **wirelessly**: link the card to the network and the farm imports while it is **within range of the
  network's wireless hardware**, the same rule a wireless terminal follows. AE2: link it like a terminal (the Wireless Access Point's
  link slot, or use it on a network block); the farm must be in range of an active Wireless Access Point, in the same dimension.
  Refined Storage: use the card on a network block; a Wireless Transmitter of that network must reach the farm (its Range Upgrades count).
  The linked block's chunk must be loaded (pair it with a Chunk Loader). Sneak-use on a block links to blocks that have a screen (a Grid, an Interface); sneak-use in the air clears the link (`easy_farms:link` component). Only real network blocks are accepted, with a message either way.
- **AE Auto Importer** card built in (needs AE2); it uses no channel and pays the normal AE power. Refined Storage and DimStorage cards
  ship in their datapacks. Those mods' code is only touched while they are installed.
- **DimStorage** has frequencies, not a network: using the DimStorage Auto Importer card opens a screen to set its **frequency** (channel
  number, public or private/owned by you; stored on the card as `easy_farms:frequency`). The farm stores into that frequency's Dim Chest
  storage from any distance. With no frequency set it falls back to an adjacent unlocked Dim Chest.
- Refined Storage import works on **both NeoForge and Fabric**; AE2 and DimStorage are NeoForge only (no Fabric 1.21.1 build).
- Fixed: a card's link was shared by every importer card in the farm; each network now uses its own card's link.

## 3.1.0

### New datapack options
- `convert_drops` ingredients (`with`) take `consume_chance` (0..1, default 1): the chance each conversion uses the ingredient up. The card tooltip shows it.
- `convert_drops` outputs (`to`) take `fortune_bonus`: up to `fortune_bonus x Fortune level` extra items per conversion when the tool has Fortune.
- New `goo` recipe visual (`target` + optional `input` and `result` blocks): the input block shrinks into the result block at the end of a cycle.
- The Experience card now fits Resource and Plant farms as well as Mob farms.

### Removed
- All of the mod's own sounds: the pickaxe and shovel swing sounds, the chime at the end of a cycle, and the `cycleSoundVolume`
  config. (Vanilla feedback sounds, such as filling a bucket or a tool breaking, are unchanged.)

### New upgrade card
- **Crafting Upgrade** (`easy_farms:auto_craft` effect): the farm crafts its own outputs into other items. An arrow on the left of
  the farm screen (under the Sides one) opens a targets screen with six ghost slots: click one with an item to set what the farm
  should make, right-click to clear. Each cycle the farm crafts every target from the new drops plus what is already in its
  output slots (up to `max_crafts` times, default 64) and keeps the leftovers. A target with no recipe in any grid shows red.
  Resource, Plant and Mob farms. Config `craftingCardEnabled`. Targets are saved with the farm and copied by the Memory Card.
- **Crafting grids are data** (`easy_farms:crafting_grid` datapack registry, `data/<ns>/easy_farms/crafting_grid/*.json`): the mod
  ships `easy_farms:crafting_3x3`; a pack or another mod adds a grid (size, `recipe_type`, `requires_mod`, `priority`) with one
  file and the card uses it, with no other change. Mods with their own machines register a `CraftingGridType` in the new
  `easy_farms:crafting_grid_type` registry (the built-in `easy_farms:vanilla` type reads crafting-table recipes).

## 3.0.0

A large feature update: five new upgrade cards, a Mekanism-style side configuration, experience, honey and milk, JEI/EMI "move
items" support, and more tuning options. Datapack authors get new recipe fields (`xp`, `fluid_input`, `loot_table`) and a new
`fluid_container` registry.

### New upgrade cards
- **Efficiency Upgrade** (`easy_farms:efficiency`): each card halves the energy a Powered farm uses per tick, up to 3 stacked
  (1/8, fractions carry over between ticks). Needs the Powered card (a `requires` restriction). All farm kinds.
- **Chunk Loader Upgrade** (`easy_farms:chunk_loader`): keeps the farm's chunk loaded with a vanilla forced chunk (same on
  NeoForge and Fabric), so the farm works while nobody is near. Released when the card or the farm is removed; a chunk you
  `/forceload`ed yourself is never released by it.
- **Void Upgrade** (`easy_farms:void_overflow`): items that do not fit the output slots are destroyed, so a full output never
  stalls the farm. Config `voidCardEnabled`.
- **Filter Upgrade** (`easy_farms:output_filter`): an arrow on the farm screen opens a Filter screen that lists every item the
  current recipe can output (Smelter, Inscriber and `convert_drops` changes applied). Click an item to throw it away instead of
  keeping it. Each recipe keeps its own list. Config `filterCardEnabled`.
- **Auto-Export Upgrade** (`easy_farms:auto_export`): an arrow on the left of the farm screen pops out a side panel where each
  face (front, back, left, right, top, bottom) is set to input and output, input only, output only or off, as in Mekanism.
  Output faces push items (and a Liquid Farm's fluid) into the neighbouring container; hoppers and pipes obey the face modes.
  Config `autoExportEnabled`, `autoExportIntervalTicks`, `autoExportItemsPerPush`.
- **Experience Upgrade** (`easy_farms:experience`): Mob farms drop the recipe's `xp` (all default mob recipes now have one).
  The xp builds up in the farm and an `xp` button on the farm screen hands it to you. A Resource Farm with a **Smelter** card
  also gives the vanilla furnace experience for every smelted item, with no extra card. Config `xpEnabled`, `xpMultiplier`.
- **Memory Card**: sneak-click a farm to copy its side and filter settings, click another farm to paste them.
- Every upgrade card now stacks to 64 in the inventory (installing takes one).

### Liquid Farm
- The default tank holds 16 buckets (16000 mB; config `liquidBaseCapacityMb`); Capacity cards still double it, up to 4 of them.
- Speed cards stack to 3 on the Liquid Farm: 2 min per bucket, then 1 min, 30 s and 15 s. Other farm kinds still take 1.
  New `kind_max_counts` field on `farm_upgrade` (e.g. `{"liquid": 3}`) overrides `max_count` per farm kind.
- New **honey** and **milk** recipes. A new data-driven `fluid_container` registry (`data/<ns>/easy_farms/fluid_container`) turns
  an empty item into a filled one when you right-click the farm (glass bottle to honey bottle, bucket to milk bucket) and defines
  "virtual" fluids with their own texture. Virtual fluids are bottled by hand and do not go through pipes.
- The tank is drawn as a solid column of fluid that fills the glass, not just a flat surface. Water is see-through; lava is
  opaque and bright.
- Recipes can use a fluid: `fluid_input` takes fluid from the farm's tank each cycle, and pipes or Auto-Export can fill it.

### Recipes and data
- New recipe field `xp` (experience per cycle). New recipe field `fluid_input`. A drop entry can name a `loot_table` instead of an
  item (fishing and chest tables; the tool and its luck are passed along), so any author can pull from existing loot pools.
  Example: the new **fishing** Resource Farm recipe (`resource_farm/fishing.json`): a fishing rod in the tool slot and a water
  bucket as input pull from the vanilla fishing loot table. The rod's Luck of the Sea works as in vanilla (more treasure, less junk) and Lure
  shortens the cycle (I/II/III: 71 %/43 %/14 % of the time); the fishing rod is in the iron tier and wears out like any tool. JEI shows
  what a loot-table drop can give, with the chance of each: the server rolls the table and sends the result (a new payload on both
  loaders, resent on `/reload`). A drop can still list a `preview` of extra items.
- The Powered card no longer checks a list of energy mods: it is always accepted on NeoForge (on Fabric it needs Team Reborn
  Energy) and its tooltip always shows "FE or other energy generation mod required to use" as a hint. The `energy_mods` field is gone.

### Recipe viewers
- JEI and EMI: the "+" button moves a recipe's inputs and tool from your inventory into the open farm, and missing ingredients are
  highlighted red, as for the crafting table. The farm's tool now counts as an input in JEI.

### Quality of life and config
- Comparator output (`comparatorMode`: cycle progress, output fill or tank fill).
- A soft chime and a few particles when a farm finishes a cycle (`cycleSoundVolume`, `cycleParticles`).
- `difficultyPreset` config: 0 = your own values, 1 = casual, 2 = balanced, 3 = hard.
- Config entries for every new feature; all of them can be switched off.
- German, Spanish and French translations.
- Guide book: new entries for every card above.

### Fixes
- Fabric now shows the mod icon (the icon is a square 512x512 image) and both loaders list the right description, source link,
  issue tracker and credits.
- A Resource Farm with a Smelter card could store experience with no way to collect it.

## 2.0.0

### Liquid Farm, Powered and Capacity upgrades
- New **Liquid Farm** (`easy_farms:liquid_farm` recipes): makes one bucket (1000 mB) of a fluid every 2 minutes (1 minute with
  the Speed card) into a one-bucket tank (water and lava included; recipes can add any fluid). GUI tank bar, Jade/TOP bar,
  pipes can extract on NeoForge and Fabric, and an empty bucket takes a full bucket. When its bucket input is used up an empty
  bucket is left behind. Crafted from glass panes around a bucket, pointed dripstone and a cauldron.
- New **Powered Upgrade** (`easy_farms:powered` effect): the farm runs on FE or any energy mod's power at diamond speed (1000 ME
  buffer, needs 100 ME). Red energy bar in the GUI and in Jade/TOP. It is refused, and its tooltip says "FE or other energy
  generation mod required to use", unless an energy mod from the effect's `energy_mods` list is loaded; the base mod needs none.
- New **Capacity Upgrade** (`easy_farms:capacity` effect, `max_count` / `stack_requires` / `stack_exempt_kinds` on
  `farm_upgrade`): doubles output stack size, ME buffer and liquid tank (up to 16000 mB) per card, stacking up to 4.
- `cycle_time` takes `kind_divisors`. Config: `liquidBaseCapacityMb`. `farm_upgrade` kinds accept `liquid`.
- Guide book: Liquid Farm, Powered and Capacity entries.

## 1.3.0

### Upgrade cards
- `farm_upgrade` files take a new optional `restrictions` list: a card can exclude other cards or tool enchantments
  (`excludes`, `excludes_enchantments`) and require other cards (`requires`), optionally only for recipes that drop
  certain items (`when_drops`). Refused cards explain why in a tooltip; rules are also enforced while running.
  See the modpack authors guide.
- `convert_drops` rules take `with`: extra ingredients used together with `from` (multi-ingredient recipes such as brass
  or andesite alloy). An extra comes from the same cycle's drops, or (`"source": "catalyst"`) from a catalyst slot.
- A card with a catalyst ingredient adds a Catalysts button under the Upgrades button on the farm screen. It opens its own
  Catalysts screen. The farm has 18 catalyst slots shared between its cards: a card gets one slot per distinct catalyst
  ingredient it uses (up to 18), and each slot accepts only that ingredient.
- Guide book: an optional in-game book (Patchouli) explains the farms, the default cards, catalysts and restrictions, and
  lists every farm recipe with its inputs and outputs. Craft it with a book and a glass pane. Patchouli is optional; the
  book is generated by `tools/generate_guide.py` from the recipe files. Addon packs can add chapters (see the modpack authors guide).
- Hoppers and pipes feed the catalyst slots through the back of the farm (opposite its facing); the other sides are unchanged.
  The Infinity upgrade now keeps catalysts as well as the tool and inputs.
- The Upgrades and Catalysts arrows show their name as a tooltip only; the label no longer scrolls over the arrow.
- Restrictions take `requires_block_below`: the farm must stand on one of the listed blocks or `#tags` (heat sources for
  Create-style cards). It is rechecked about once a second.

## 1.2.0 (2026-11-04, commit 55963c0: "Added ability to change what upgrade cards override.")

### Upgrade cards
- `farm_upgrade` files take two new optional fields:
  - `name`: the card's display name (a text component; a plain string works).
  - `description`: what the card does, shown in its tooltip.
- The blank `easy_farms:upgrade_card` item now takes its name and its enchantment glint from the `farm_upgrade` whose
  `card` id matches the stack's `easy_farms:card` component. A card recipe only has to set that one component.
- Card tooltips show the description as well as the farms that accept the card.
- Every `farm_upgrade` that names a card id is listed in the Easy Farms creative tab, built from the synced registry.
  It fills in once you are in a world.

### JEI
- Each farm's category shows a copy of every recipe whose drops a `convert_drops` card changes (for example, logs
  turning into Andesite Casing on the Plant Farm). The copy shows the card in a slot, with the text "With: <card>".
  This works for any mod's `convert_drops` card. Only one card at a time is shown; chains of cards are not combined.
- Datapack cards are also registered as JEI ingredients, so they can be searched for directly.
- New language keys: `gui.easy_farms.jei.with_card` and `gui.easy_farms.jei.card_needed`.

### Tool tiers
- Tier files take two new optional fields:
  - `priority`: when two tiers list the same tool, the higher priority wins (then the higher rank). A datapack can use
    this to move a tool to another tier.
  - `tool_durability`: the tool durability a tier stands for. All six built-in tiers now carry one (wood 59, stone 131,
    gold 32, iron 250, diamond 1561, netherite 2031).
- A tool that no tier lists is now placed in this order:
  1. a tier file that lists it;
  2. for a tiered item (pickaxe, shovel, axe, hoe, sword), the tier with the nearest mining speed of its material, with
     the material's "incorrect blocks" tag only deciding a tie;
  3. the tier with the nearest `tool_durability`, for any other damageable item;
  4. otherwise it is not accepted as a tool.
- Before, the "incorrect blocks" tag was matched first. Many modded materials reuse the wooden tag, so their tools
  fell into the wooden tier. This is the likely cause of modded items defaulting to wooden; it was not tested against a
  real modded tool.
- A tier's `input_lifetime_multiplier` sets how long a tool in that tier lasts.

### Docs
- The README for modpack authors covers the new card fields, the creative tab and JEI behaviour, and the tier fields and
  lookup order.

## 1.1.0 (2026-11-04, commit 5aa9c1a: "Added ability to change what upgrade cards override.")

### Upgrade cards
- New blank item `easy_farms:upgrade_card`. A datapack makes a card by giving a crafting recipe result the
  `easy_farms:card` component and adding a `farm_upgrade` with the same id in a new optional `card` field. No Java and
  no item registration are needed.
- New data component `easy_farms:card`.
- A farm now has 18 upgrade slots (it had 8):
  - any card the farm accepts can go in any free slot;
  - a farm holds each card once;
  - cards run in slot order, so they can be chained;
  - farms saved with fewer slots are extended on load, keeping their cards.
- Sneak-right-clicking a farm with a card installs it in the first free slot.
- The upgrade screen shows a generic "Upgrade card slot" tooltip on an empty slot.
- Card tooltips list the farm kinds that take the card and what the recipe needs.
- New language keys for the card, the slot tooltip, the farm kinds and the "Fits:" line.
- The upgrade card has a generated item model that uses the shared `upgrade_parent` texture.

### New upgrade effect
- `easy_farms:convert_drops` rewrites a farm's finished drops with rules, so a card can emulate a machine from any
  mod (crushing, pressing, casing, alloying, and so on).
  - A rule's `from` is an item id or a `#tag`; `per` inputs make one conversion; `to` is one output or a list, each with
    an optional `count` and `chance`.
  - The first usable rule that matches handles each drop. Leftovers stay. Rules whose output items do not exist (the
    other mod is not installed) are skipped.
  - Results are merged and split to stack size.

### Docs
- The README for modpack authors covers the `card` field, generic cards, `convert_drops` and the 18 slots.

### Datapack notes for 1.21.1
- Datapacks use `pack_format` 48 and the singular `recipe` folder.
- Text components in recipe results (`item_name`, `lore`) are JSON stored in a string on this version, and there is no
  `minecraft:item_model` component. With 1.2.0 a card recipe needs neither, because the name comes from the
  `farm_upgrade`.

## 1.0.0 (2026-11-01 to 2026-11-03)

Commits, oldest first:

- `ee404a8` Initial commit (2026-11-01): the NeoForge mod template.
- `865e964` "Brand new mod that combines all of my other mods. Completely modular." (2026-11-01): the mod itself.
- `e409256` "Updated license and added icon.png" (2026-11-01).
- `a62112d` "Commit before branching" (2026-11-03): adds `icon_curseforge.png`.

### The mod (865e964)
- Replaces the template's test farms (coal, cow, wheat) with three data-driven machines:
  - Resource Farm: mines blocks, using a pickaxe, shovel or axe.
  - Plant Farm: grows crops, flowers, mushrooms, saplings and trees, with soil in slot 1.
  - Mob Farm: "kills" mobs for their drops, using a sword.
- A farm matches its input and tool slots against data-driven machine recipes (`recipe/resource_farm`,
  `plant_farm`, `mob_farm`), so swapping items gives a different result. Resource Farm recipes are generated from the
  game's block loot tables, and every shipped recipe is checked against the real codecs during data generation.
- The tool wears out instead of the inputs. The tool lasts a number of Minecraft days set by the config and the tool
  tier; recipes with no tool use up their inputs instead.
- Tool tiers are data (`farm_tier`): wood, stone, iron, gold, diamond, netherite. A tool no tier lists is placed by its
  own material, so other mods' tools work without extra recipes.
- Upgrade cards are data (`farm_upgrade`): Redstone, Smelter, Speed, Nether, Inscriber (an AE2 Inscriber emulation) and
  a new Infinity card. Effects are a registry (`upgrade_effect_type`) that other mods can add to.
- Enchantments count: Fortune, Silk Touch, Looting (Mob Farm) and Fire Aspect.
- Recipes can need a dimension or a block under the farm, can be dyeable (sheep, concrete, terracotta; dye by hand or
  with a dispenser), and one secret Wither recipe is hidden until discovered (advancement trigger).
- Breaking a farm with Silk Touch keeps its contents. Hoppers and pipes use the standard item handler capability.
- Block entity renderer and item renderer show the glass case, the swinging tool, the growing plant or the mob.
- Crafting recipes and recipe advancements for the farms and cards are generated.
- Compatibility: JEI (a category per farm), EMI, Jade, The One Probe, and recipes for Applied Energistics 2,
  ExtendedAE, Advanced AE, Integrated Dynamics and Integrated Terminals.
- Configuration for the base tool lifetime in days.
- Docs: a player README and a guide for modpack authors and mod developers (`READMES/modpackauthors/README.md`).
- `tools/convert_farms.py` and `tools/convert_report.txt` record the conversion of the old per-mod farms.

### Template and metadata
- The final jar is named `<ModName> <version>-<minecraft_version>.jar`, dropping a trailing `.0` from the version
  (e409256).
- The license changed from All Rights Reserved to MIT, and `TEMPLATE_LICENSE.txt` became `LICENSE.txt` (e409256).
- The logo is now `icon.png` (it was `easyfarmsicon.png`) (e409256).
- The CurseForge icon was added (a62112d).
