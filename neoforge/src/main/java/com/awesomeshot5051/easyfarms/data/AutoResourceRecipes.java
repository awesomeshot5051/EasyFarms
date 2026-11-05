package com.awesomeshot5051.easyfarms.data;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.farm.DropEntry;
import com.awesomeshot5051.easyfarms.farm.FarmKind;
import com.awesomeshot5051.easyfarms.recipe.FarmRecipe;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import com.awesomeshot5051.easyfarms.recipe.SizedIngredient;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Writes a Resource Farm recipe for every suitable block in the game, read from the block registry, so new Minecraft
 * versions (or other mods' vanilla-style blocks) need no edits. A block gets a recipe when it is a full cube that a
 * pickaxe, shovel or hoe mines. What it drops comes from its own vanilla loot table: a "silk touch only" table (ice)
 * becomes a drop that needs Silk Touch; "silk touch gives the block, otherwise X" becomes X with the block as the silk item.
 * Blocks that already have a hand-made recipe are skipped. Stairs, slabs, walls, fences, doors etc. are not blocks "to farm".
 */
final class AutoResourceRecipes {
    private static final Set<String> NEVER = Set.of("bedrock", "barrier", "light", "structure_void", "reinforced_deepslate",
            "end_portal_frame", "end_portal", "end_gateway", "nether_portal", "respawn_anchor", "lodestone", "frosted_ice",
            "chiseled_bookshelf", "decorated_pot", "suspicious_sand", "suspicious_gravel", "trial_spawner", "vault",
            "heavy_core", "crafter", "tnt", "target");
    /** Colour families the dyeable recipes already cover. */
    private static final List<String> DYE_FAMILIES = List.of("_concrete", "_concrete_powder", "_terracotta");
    /** What a furnace turns the block's drop into (vanilla smelting, kept small on purpose). */
    private static final Map<String, String> SMELTS = Map.ofEntries(
            Map.entry("cobblestone", "stone"), Map.entry("cobbled_deepslate", "deepslate"), Map.entry("stone", "smooth_stone"),
            Map.entry("sand", "glass"), Map.entry("red_sand", "glass"), Map.entry("clay_ball", "brick"),
            Map.entry("netherrack", "nether_brick"), Map.entry("sandstone", "smooth_sandstone"),
            Map.entry("red_sandstone", "smooth_red_sandstone"), Map.entry("quartz_block", "smooth_quartz"),
            Map.entry("basalt", "smooth_basalt"), Map.entry("stone_bricks", "cracked_stone_bricks"),
            Map.entry("deepslate_bricks", "cracked_deepslate_bricks"), Map.entry("deepslate_tiles", "cracked_deepslate_tiles"),
            Map.entry("nether_bricks", "cracked_nether_bricks"), Map.entry("polished_blackstone_bricks", "cracked_polished_blackstone_bricks"),
            Map.entry("wet_sponge", "sponge"), Map.entry("clay", "terracotta"));

    private AutoResourceRecipes() {
    }

    static int generate(RecipeOutput out, Path existingRecipes) {
        Set<ResourceLocation> pickaxe = blockTag("mineable/pickaxe"), shovel = blockTag("mineable/shovel"), hoe = blockTag("mineable/hoe");
        Set<ResourceLocation> leaves = blockTag("leaves");
        Set<ResourceLocation> needsStone = blockTag("needs_stone_tool"), needsIron = blockTag("needs_iron_tool"), needsDiamond = blockTag("needs_diamond_tool");
        Set<String> alreadyCovered = existingVisualTargets(existingRecipes);

        int count = 0;
        List<String> skipped = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals("minecraft")) continue;
            String path = id.getPath();
            if (NEVER.contains(path) || path.startsWith("waxed_") || path.startsWith("infested_") || path.endsWith("_command_block")
                    || DYE_FAMILIES.stream().anyMatch(path::endsWith) || alreadyCovered.contains(id.toString())) continue;
            if (!isCube(block)) continue;
            var item = block.asItem();
            if (item == Items.AIR) continue;

            net.minecraft.tags.TagKey<Item> tool;
            if (pickaxe.contains(id)) tool = ItemTags.PICKAXES;
            else if (shovel.contains(id)) tool = ItemTags.SHOVELS;
            else if (hoe.contains(id) && !leaves.contains(id)) tool = ItemTags.HOES;
            else continue;

            Drop drop = analyze(id);
            if (drop == null) {
                skipped.add(path + " (no drops)");
                continue;
            }
            int rank = needsDiamond.contains(id) ? 4 : needsIron.contains(id) ? 2 : needsStone.contains(id) ? 1 : 0;
            ResourceLocation dropId = drop.item;
            List<DropEntry> drops = new ArrayList<>();
            if (drop.silkOnly) {
                drops.add(new DropEntry(id, 1, 1, 1F, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), true));
            } else {
                String smelted = SMELTS.get(dropId.getPath());
                boolean smeltable = smelted != null && dropId.getNamespace().equals("minecraft");
                drops.add(new DropEntry(dropId, drop.min, drop.max, 1F, Optional.empty(), drop.fortuneMax >= 0 ? Optional.of(drop.fortuneMax) : Optional.empty(),
                        Optional.empty(), Optional.empty(),
                        dropId.equals(id) ? Optional.empty() : Optional.of(id),
                        smeltable ? Optional.of(ResourceLocation.withDefaultNamespace(smelted)) : Optional.empty(),
                        Optional.empty(), false));
            }
            if (path.equals("budding_amethyst")) {
                // Vanilla gives the block no drops of its own; the buds that grow on it are what you get from it. Any of them, or nothing.
                drops = List.of(bud("small_amethyst_bud", 0.40F), bud("medium_amethyst_bud", 0.30F),
                        bud("large_amethyst_bud", 0.20F), bud("amethyst_cluster", 0.10F));
            }
            boolean smeltable = drops.get(0).smeltItem().isPresent();
            FarmRecipe recipe = new FarmRecipe(FarmKind.RESOURCE, List.of(SizedIngredient.of(item, 1)), Optional.of(tool), rank,
                    new FarmRecipe.Visual("block", id), 600, 80, smeltable, false, Optional.empty(), false, false, Optional.empty(), drops, Optional.empty());
            out.accept(ResourceLocation.fromNamespaceAndPath(EasyFarms.MODID, "resource_farm/auto/" + path), recipe, null);
            count++;
        }
        System.out.println("Auto resource recipes: " + count + (skipped.isEmpty() ? "" : "; skipped (no drops): " + skipped));
        return count;
    }

    private static DropEntry bud(String item, float chance) {
        return new DropEntry(ResourceLocation.withDefaultNamespace(item), 1, 1, chance, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), false);
    }

    // ---- which blocks ----

    /** A full cube with no block entity and none of the shaped / functional block classes. */
    private static boolean isCube(Block block) {
        if (block instanceof StairBlock || block instanceof SlabBlock || block instanceof WallBlock || block instanceof FenceBlock
                || block instanceof FenceGateBlock || block instanceof DoorBlock || block instanceof TrapDoorBlock
                || block instanceof ButtonBlock || block instanceof PressurePlateBlock || block instanceof WeightedPressurePlateBlock
                || block instanceof PistonBaseBlock || block instanceof ObserverBlock || block instanceof LiquidBlock
                || block instanceof EntityBlock || block instanceof BushBlock || block instanceof AmethystClusterBlock) return false;
        BlockState state = block.defaultBlockState();
        try {
            return !state.hasBlockEntity() && Block.isShapeFullBlock(state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        } catch (Exception e) {
            return false;
        }
    }

    // ---- drops from the vanilla loot table ----

    private static final class Drop {
        ResourceLocation item;
        boolean silkOnly;
        int min = 1, max = 3, fortuneMax = 5;
    }

    private static Drop analyze(ResourceLocation block) {
        JsonObject table = json("data/minecraft/loot_table/blocks/" + block.getPath() + ".json");
        if (table == null || !table.has("pools")) return fallback(block);
        List<Leaf> leaves = new ArrayList<>();
        for (JsonElement pool : table.getAsJsonArray("pools")) {
            JsonObject p = pool.getAsJsonObject();
            boolean poolSilk = silk(p.get("conditions"));
            for (JsonElement entry : p.getAsJsonArray("entries")) collect(entry.getAsJsonObject(), poolSilk, leaves);
        }
        if (leaves.isEmpty()) return null;
        // The fallback (last entry that does not need Silk Touch) is the normal drop.
        Leaf plain = null;
        boolean silkGivesBlock = false;
        for (Leaf leaf : leaves) {
            if (leaf.silk) silkGivesBlock |= leaf.item.equals(block);
            else plain = leaf;
        }
        Drop d = new Drop();
        if (plain == null) {
            if (!silkGivesBlock) return null;
            d.item = block;
            d.silkOnly = true;
            return d;
        }
        d.item = plain.item;
        d.min = plain.min;
        d.max = plain.max;
        d.fortuneMax = plain.bonus ? Math.max(plain.max * 2, plain.max + 2) : (plain.item.equals(block) ? 5 : -1);
        if (plain.item.equals(block) && !plain.countSet) {
            d.min = 1;
            d.max = 3;
            d.fortuneMax = 5;
        }
        return d;
    }

    /** Used when a loot table cannot be read: the block drops itself, as the old farms did. */
    private static Drop fallback(ResourceLocation block) {
        Drop d = new Drop();
        d.item = block;
        return d;
    }

    private static final class Leaf {
        ResourceLocation item;
        boolean silk, bonus, countSet;
        int min = 1, max = 1;
    }

    private static void collect(JsonObject entry, boolean inheritedSilk, List<Leaf> out) {
        boolean silk = inheritedSilk || silk(entry.get("conditions"));
        String type = entry.has("type") ? entry.get("type").getAsString() : "";
        if (type.equals("minecraft:alternatives") || type.equals("minecraft:group") || type.equals("minecraft:sequence")) {
            for (JsonElement child : entry.getAsJsonArray("children")) collect(child.getAsJsonObject(), silk, out);
            return;
        }
        if (!type.equals("minecraft:item") || !entry.has("name")) return;
        Leaf leaf = new Leaf();
        leaf.item = ResourceLocation.parse(entry.get("name").getAsString());
        leaf.silk = silk;
        if (entry.has("functions")) {
            for (JsonElement f : entry.getAsJsonArray("functions")) {
                JsonObject fn = f.getAsJsonObject();
                String name = fn.get("function").getAsString();
                if (name.equals("minecraft:set_count") && fn.has("count")) {
                    JsonElement c = fn.get("count");
                    leaf.countSet = true;
                    if (c.isJsonObject()) {
                        leaf.min = (int) c.getAsJsonObject().get("min").getAsFloat();
                        leaf.max = (int) c.getAsJsonObject().get("max").getAsFloat();
                    } else leaf.min = leaf.max = (int) c.getAsFloat();
                } else if (name.equals("minecraft:apply_bonus")) leaf.bonus = true;
            }
        }
        out.add(leaf);
    }

    /** True for a Silk Touch requirement; "inverted" silk (the not-silk branch) is not one. */
    private static boolean silk(JsonElement conditions) {
        if (conditions == null || !conditions.isJsonArray()) return false;
        for (JsonElement c : conditions.getAsJsonArray()) {
            String text = c.toString();
            if (text.contains("silk_touch") && !text.contains("minecraft:inverted")) return true;
        }
        return false;
    }

    // ---- reading vanilla data ----

    private static JsonObject json(String path) {
        try (InputStream in = AutoResourceRecipes.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) return null;
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    /** All blocks in a vanilla block tag, following references to other tags. */
    private static Set<ResourceLocation> blockTag(String name) {
        Set<ResourceLocation> out = new HashSet<>();
        readTag(name, out, new HashSet<>());
        return out;
    }

    private static void readTag(String name, Set<ResourceLocation> out, Set<String> seen) {
        if (!seen.add(name)) return;
        JsonObject tag = json("data/minecraft/tags/block/" + name + ".json");
        if (tag == null || !tag.has("values")) return;
        JsonArray values = tag.getAsJsonArray("values");
        for (JsonElement v : values) {
            String s = v.isJsonObject() ? v.getAsJsonObject().get("id").getAsString() : v.getAsString();
            if (s.startsWith("#")) {
                ResourceLocation ref = ResourceLocation.parse(s.substring(1));
                if (ref.getNamespace().equals("minecraft")) readTag(ref.getPath(), out, seen);
            } else out.add(ResourceLocation.parse(s));
        }
    }

    /** Visual targets of the hand-made resource recipes, so the generator does not duplicate them. */
    private static Set<String> existingVisualTargets(Path dir) {
        Set<String> targets = new HashSet<>();
        if (!Files.isDirectory(dir)) return targets;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.filter(x -> x.toString().endsWith(".json")).toList()) {
                JsonObject o = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
                if (o.has("visual")) targets.add(o.getAsJsonObject("visual").get("target").getAsString());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read " + dir, e);
        }
        return targets;
    }
}
