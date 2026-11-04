package com.awesomeshot5051.easyfarms.tier;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.farm.FarmKind;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Says which upgrade card fits which farm kind. Data: data/&lt;ns&gt;/easy_farms/farm_upgrade/*.json, so other mods can
 * offer their own cards in the Upgrades screen. The card's effect is implemented by the farm (or a mod's own code).
 *
 * @param item     the card item
 * @param card     optional: for a generic card item (easy_farms:upgrade_card), the id its easy_farms:card component must hold
 * @param name     optional: the card's display name (a text component), used for generic cards
 * @param description optional: what the card does, shown in its tooltip
 * @param kinds    farm kinds whose Upgrades screen shows a slot for it
 * @param requires translation key suffix explaining what the recipe needs ("any", "smeltable", "toolless", ...)
 * @param effects  what the card does; see {@link com.awesomeshot5051.easyfarms.upgrade.UpgradeEffect}
 * @param restrictions compatibility rules: other cards or tool enchantments it excludes, and cards it requires
 */
public record FarmUpgrade(Item item, Optional<Identifier> card, Optional<Component> name, Optional<Component> description,
                          List<FarmKind> kinds, String requires,
                          List<com.awesomeshot5051.easyfarms.upgrade.UpgradeEffect> effects,
                          List<UpgradeRestriction> restrictions) {
    public static final ResourceKey<Registry<FarmUpgrade>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(EasyFarms.MODID, "farm_upgrade"));

    public static final Codec<FarmUpgrade> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(FarmUpgrade::item),
            Identifier.CODEC.optionalFieldOf("card").forGetter(FarmUpgrade::card),
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(FarmUpgrade::name),
            ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(FarmUpgrade::description),
            FarmKind.CODEC.listOf().fieldOf("kinds").forGetter(FarmUpgrade::kinds),
            Codec.STRING.optionalFieldOf("requires", "any").forGetter(FarmUpgrade::requires),
            com.awesomeshot5051.easyfarms.upgrade.UpgradeEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(FarmUpgrade::effects),
            UpgradeRestriction.CODEC.listOf().optionalFieldOf("restrictions", List.of()).forGetter(FarmUpgrade::restrictions)
    ).apply(i, FarmUpgrade::new));

    /** The upgrades shown for a kind, in a stable order (by id) so slot positions don't move between sessions. */
    public static List<FarmUpgrade> forKind(RegistryAccess access, FarmKind kind) {
        Registry<FarmUpgrade> registry = access.lookupOrThrow(REGISTRY_KEY);
        List<FarmUpgrade> list = new ArrayList<>();
        registry.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().identifier().toString()))
                .forEach(e -> {
                    if (e.getValue().kinds().contains(kind)) list.add(e.getValue());
                });
        return list;
    }

    /** A stack of this card (with its card id set), for creative tabs and recipe viewers. */
    public ItemStack stack() {
        ItemStack stack = new ItemStack(item);
        card.ifPresent(id -> stack.set(com.awesomeshot5051.easyfarms.registry.ModDataComponents.CARD.get(), id));
        return stack;
    }

    /** True if this stack is the card this upgrade describes (the item, plus the card id when the upgrade names one). */
    public boolean matches(ItemStack stack) {
        if (!stack.is(item)) return false;
        return card.isEmpty() || card.get().equals(stack.get(com.awesomeshot5051.easyfarms.registry.ModDataComponents.CARD.get()));
    }

    /** The upgrade definition for a card stack, or null if the stack is not a registered card. */
    public static FarmUpgrade find(RegistryAccess access, ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (FarmUpgrade upgrade : access.lookupOrThrow(REGISTRY_KEY)) if (upgrade.matches(stack)) return upgrade;
        return null;
    }

    /** The items (ids or {@code #tags}) this card needs in its catalyst slot; empty if it needs none. */
    public List<String> catalystItems() {
        List<String> items = new ArrayList<>();
        for (var effect : effects) {
            if (!(effect instanceof com.awesomeshot5051.easyfarms.upgrade.UpgradeEffects.ConvertDrops convert)) continue;
            for (var rule : convert.rules()) {
                for (var extra : rule.with()) if (extra.catalyst() && !items.contains(extra.from())) items.add(extra.from());
            }
        }
        return items;
    }

    /** The registry id of an upgrade (its farm_upgrade file), or null if it is not registered. */
    public static Identifier id(RegistryAccess access, FarmUpgrade upgrade) {
        return access.lookupOrThrow(REGISTRY_KEY).getKey(upgrade);
    }

    /** The upgrade registered under this id, or null. */
    public static FarmUpgrade byId(RegistryAccess access, Identifier id) {
        return access.lookupOrThrow(REGISTRY_KEY).getValue(id);
    }

    /** The card's display name (works on a server too, where the blank card has no name of its own). */
    public Component displayName() {
        return name.orElseGet(() -> stack().getHoverName());
    }

    public static boolean isCard(RegistryAccess access, ItemStack stack) {
        return find(access, stack) != null;
    }
}
