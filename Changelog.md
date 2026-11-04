# Changelog

This is the changelog for the `master` branch (Minecraft 1.21.1, NeoForge 21.1.265), written from the commit history.
Versions are listed newest first, each with its commit and date.

## 1.3.0

### Upgrade cards
- `farm_upgrade` files take a new optional `restrictions` list: a card can exclude other cards or tool enchantments
  (`excludes`, `excludes_enchantments`) and require other cards (`requires`), optionally only for recipes that drop
  certain items (`when_drops`). Refused cards explain why in a tooltip; rules are also enforced while running.
  See the modpack authors guide.

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
