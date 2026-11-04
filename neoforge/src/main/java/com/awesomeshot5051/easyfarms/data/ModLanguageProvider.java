package com.awesomeshot5051.easyfarms.data;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, EasyFarms.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + EasyFarms.MODID, "Easy Farms");
        EasyFarms.FARM_BLOCKS.forEach((kind, block) -> add(block.get(), titleCase(kind.getSerializedName()) + " Farm"));
        add(ModItems.SPEED_UPGRADE.get(), "Speed Upgrade");
        add(ModItems.SMELTER_UPGRADE.get(), "Smelter Upgrade");
        add(ModItems.REDSTONE_UPGRADE.get(), "Redstone Upgrade");
        add(ModItems.NETHER_UPGRADE.get(), "Nether Upgrade");
        add(ModItems.INSCRIBER_UPGRADE.get(), "Inscriber Upgrade");
        add(ModItems.INFINITY_UPGRADE.get(), "Infinity Upgrade");
        add(ModItems.UPGRADE_CARD.get(), "Upgrade Card");
        add("gui.easy_farms.config", "Easy Farms Settings");
        add("gui.easy_farms.upgrade_slot", "Upgrade card slot");
        add("tooltip.easy_farms.card_kinds", "Fits: %s");
        add("gui.easy_farms.jei.with_card", "With: %s");
        add("gui.easy_farms.jei.card_needed", "Needs this upgrade card installed");
        add("farm_kind.easy_farms.resource", "Resource Farm");
        add("farm_kind.easy_farms.plant", "Plant Farm");
        add("farm_kind.easy_farms.mob", "Mob Farm");
        add("gui.easy_farms.upgrades", "Upgrades");
        add("gui.easy_farms.back", "Back");
        add("gui.easy_farms.idle", "Idle: put a matching recipe in the slots");
        add("gui.easy_farms.progress", "Progress: %s%%");
        add("gui.easy_farms.inputs_days", "Inputs last %s more day(s)");
        add("gui.easy_farms.inputs_infinite", "Inputs last forever");
        add("gui.easy_farms.tool_days", "Tool lasts %s more day(s)");
        add("gui.easy_farms.tool_infinite", "Tool never wears out");
        add("gui.easy_farms.inscriber_press", "Inscriber press");
        add("gui.easy_farms.inscriber_press.hint", "Put the matching AE2 press here (Inscriber card recipes)");
        add("gui.easy_farms.jei.card_inscriber", "Inscriber card");
        add("gui.easy_farms.jei.press", "Press: %s");
        add("gui.easy_farms.requires.any", "Works with every recipe");
        add("gui.easy_farms.requires.smeltable", "Only for recipes whose drops can be smelted");
        add("gui.easy_farms.requires.toolless", "Only for recipes that need no tool");
        add("gui.easy_farms.requires.nether", "Nether recipes: lets them run outside the Nether");
        add("gui.easy_farms.requires.inscriber", "Only for recipes that can be inscribed (AE2 items)");
        add("tooltip.easy_farms.contents", "Holds %s stack(s) of items");
        add("tooltip.easy_farms.tool", "Tool: %s");
        add("gui.easy_farms.jei.tool", "Tool (any tier works if it has a farm tier)");
        add("advancements.easy_farms.unlimited_power.title", "UNLIMITED POWER!");
        add("advancements.easy_farms.unlimited_power.description", "Discover the secret recipe for the Wither Farm");
        add("gui.easy_farms.jei.inputs", "Input");
        add("gui.easy_farms.jei.outputs", "Output");
        add("gui.easy_farms.jei.count", "Drops %s-%s");
        add("gui.easy_farms.jei.chance", "Chance: %s%%");
        add("gui.easy_farms.jei.fortune", "With Fortune: %s-%s");
        add("gui.easy_farms.jei.looting", "With Looting: %s-%s");
        add("gui.easy_farms.jei.silk_only", "Only drops with Silk Touch");
        add("gui.easy_farms.jei.silk", "With Silk Touch: %s");
        add("gui.easy_farms.jei.smelter", "With the Smelter Upgrade: %s");
        add("gui.easy_farms.jei.fire", "With Fire Aspect: %s");
        add("gui.easy_farms.jei.time", "Base time: %ss");
        add("gui.easy_farms.jei.card_smelter", "Smelter Upgrade applies");
        add("gui.easy_farms.jei.card_nether", "Runs anywhere with the Nether Upgrade");
        add("gui.easy_farms.jei.dimension", "Otherwise only in: %s");
        add("gui.easy_farms.jei.over", "Over: %s");
        add("gui.easy_farms.wrong_environment", "Wrong place: needs the Nether Upgrade, or the right environment");
        add("dimension.minecraft.the_nether", "the Nether");
        add("dimension.minecraft.the_end", "the End");
        add("dimension.minecraft.overworld", "the Overworld");
        add("gui.easy_farms.jei.min_tier", "Tool tier %s or higher");
        add("message.easy_farms.upgrade_installed", "Installed: %s");
        add("gui.easy_farms.catalysts", "Catalysts");
        add("gui.easy_farms.catalyst_slot", "Catalyst for %s");
        add("gui.easy_farms.catalyst_accepts", "Accepts: %s");
        add("tooltip.easy_farms.card_catalyst", "Needs a catalyst: %s (put it in the farm's Catalysts screen)");
        add("gui.easy_farms.restriction.requires_block_below", "Needs one of these under the farm: %s");
        add("gui.easy_farms.restriction.requires", "Requires: %s");
        add("gui.easy_farms.restriction.excludes", "Incompatible with: %s");
        add("gui.easy_farms.restriction.excludes_enchantment", "Incompatible with %s on the tool");
        add("gui.easy_farms.restriction.cannot_add", "Can't be added here");
        add("tooltip.easy_farms.card_when", "When the farm drops %s:");
        add("message.easy_farms.upgrade_blocked", "%s can't be installed: %s");
        add("message.easy_farms.upgrade_rejected", "%s does not fit this farm, or is already installed");
        // Jade refuses to load a tooltip provider (and fails the whole resource reload in dev) without this name.
        add("config.jade.plugin_easy_farms.farm", "Easy Farms");
        String p = EasyFarms.MODID + ".configuration.";
        add(p + "title", "Easy Farms Configs");
        add(p + "section.easy_farms.common.toml", "Easy Farms Configs");
        add(p + "section.easy_farms.common.toml.title", "Easy Farms Configs");
        add(p + "timeMultiplier", "Time Multiplier");
    }

    private static String titleCase(String id) {
        return Arrays.stream(id.split("_")).map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .collect(Collectors.joining(" "));
    }
}
