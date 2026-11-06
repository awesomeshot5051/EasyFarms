package com.awesomeshot5051.easyfarms.data;

import com.awesomeshot5051.easyfarms.farm.DropEntry;
import com.awesomeshot5051.easyfarms.farm.FarmKind;
import com.awesomeshot5051.easyfarms.recipe.FarmRecipe;
import com.awesomeshot5051.easyfarms.recipe.ModRecipes;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import com.awesomeshot5051.easyfarms.recipe.SizedIngredient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * The machine recipes under src/main/resources/data/easy_farms/recipe/&lt;kind&gt;_farm/ are plain JSON (written by
 * tools/convert_farms.py or by hand). This provider parses every one with the real recipe codec and checks that the
 * ids it mentions exist, so a typo fails the data run instead of silently failing in game. Recipes that need another
 * mod (neoforge:conditions) are checked too, but only when that mod is loaded.
 */
public class MachineRecipeValidator implements DataProvider {
    private final PackOutput output;

    public MachineRecipeValidator(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path root = output.getOutputFolder().resolve("../../main/resources/data/easy_farms/recipe").normalize();
        List<String> errors = new ArrayList<>();
        int checked = 0;
        for (FarmKind kind : FarmKind.values()) {
            Path dir = root.resolve(kind.id());
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> files = Files.list(dir)) {
                for (Path file : files.filter(f -> f.toString().endsWith(".json")).sorted().toList()) {
                    checked++;
                    validate(kind, file, errors);
                }
            } catch (IOException e) {
                errors.add("cannot read " + dir + ": " + e.getMessage());
            }
        }
        // Upgrade cards: every farm_upgrade JSON must decode with the real codec (this also checks each effect type exists).
        Path upgrades = output.getOutputFolder().resolve("../../main/resources/data/easy_farms/easy_farms/farm_upgrade").normalize();
        int upgradeCount = 0;
        if (Files.isDirectory(upgrades)) {
            try (Stream<Path> files = Files.list(upgrades)) {
                for (Path file : files.filter(f -> f.toString().endsWith(".json")).sorted().toList()) {
                    upgradeCount++;
                    try {
                        var parsed = com.awesomeshot5051.easyfarms.tier.FarmUpgrade.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(file)));
                        if (parsed.error().isPresent()) errors.add("farm_upgrade/" + file.getFileName() + ": " + parsed.error().get().message());
                    } catch (Exception e) {
                        errors.add("farm_upgrade/" + file.getFileName() + ": " + e);
                    }
                }
            } catch (IOException e) {
                errors.add("cannot read " + upgrades + ": " + e.getMessage());
            }
        }
        System.out.println("Validated " + upgradeCount + " upgrade cards");
        if (!errors.isEmpty()) {
            throw new IllegalStateException(errors.size() + " bad machine recipe(s):\n  " + String.join("\n  ", errors));
        }
        System.out.println("Validated " + checked + " machine recipes");
        return CompletableFuture.completedFuture(null);
    }

    private void validate(FarmKind kind, Path file, List<String> errors) {
        String name = kind.id() + "/" + file.getFileName();
        try {
            JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            boolean modded = json.has("neoforge:conditions");
            // Recipes for other mods are only checked when those mods are loaded (they are, in the dev environment).
            if (modded && !conditionsMet(json)) return;
            var codec = ModRecipes.SERIALIZERS_BY_KIND.get(kind).get().codec().codec();
            var parsed = codec.parse(JsonOps.INSTANCE, json);
            if (parsed.error().isPresent()) {
                errors.add(name + ": " + parsed.error().get().message());
                return;
            }
            FarmRecipe recipe = parsed.result().orElseThrow();
            for (SizedIngredient in : recipe.inputs()) {
                for (var stack : in.getItems()) if (stack.is(Items.AIR)) errors.add(name + ": empty input ingredient");
                if (in.getItems().length == 0) errors.add(name + ": input matches no item");
            }
            for (DropEntry d : recipe.drops()) {
                checkItem(name, d.item(), errors);
                d.silkItem().ifPresent(i -> checkItem(name, i, errors));
                d.smeltItem().ifPresent(i -> checkItem(name, i, errors));
                d.fireItem().ifPresent(i -> checkItem(name, i, errors));
            }
            recipe.inscriber().ifPresent(ins -> {
                // Inscriber items belong to another mod (AE2); only check them when that mod is loaded.
                if (ins.press().isPresent() && BuiltInRegistries.ITEM.containsKey(ins.press().get()) == false
                        && net.neoforged.fml.ModList.get().isLoaded(ins.press().get().getNamespace())) errors.add(name + ": unknown press " + ins.press().get());
                for (DropEntry d : ins.drops()) {
                    if (net.neoforged.fml.ModList.get().isLoaded(d.item().getNamespace())) checkItem(name, d.item(), errors);
                }
            });
            ResourceLocation target = recipe.visual().target();
            boolean ok = switch (recipe.visual().type()) {
                case "mob" -> BuiltInRegistries.ENTITY_TYPE.containsKey(target);
                case "fluid" -> BuiltInRegistries.FLUID.containsKey(target) || target.getNamespace().equals("easy_farms");
                default -> BuiltInRegistries.BLOCK.containsKey(target);
            };
            if (!ok) errors.add(name + ": unknown visual target " + target);
            if (recipe.drops().isEmpty() && recipe.fluid().isEmpty()) errors.add(name + ": no drops");
            recipe.fluid().ifPresent(f -> {
                if (!BuiltInRegistries.FLUID.containsKey(f.fluid()) && !f.fluid().getNamespace().equals("easy_farms")) errors.add(name + ": unknown fluid " + f.fluid());
            });
        } catch (Exception e) {
            errors.add(name + ": " + e);
        }
    }

    private static boolean conditionsMet(JsonObject json) {
        for (JsonElement c : json.getAsJsonArray("neoforge:conditions")) {
            JsonObject o = c.getAsJsonObject();
            if ("neoforge:mod_loaded".equals(o.get("type").getAsString())
                    && !net.neoforged.fml.ModList.get().isLoaded(o.get("modid").getAsString())) return false;
        }
        return true;
    }

    private static void checkItem(String name, ResourceLocation id, List<String> errors) {
        if (!BuiltInRegistries.ITEM.containsKey(id)) errors.add(name + ": unknown item " + id);
    }

    @Override
    public String getName() {
        return "Easy Farms machine recipe validation";
    }
}
