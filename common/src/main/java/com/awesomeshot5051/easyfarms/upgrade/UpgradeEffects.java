package com.awesomeshot5051.easyfarms.upgrade;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.platform.Services;
import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import com.awesomeshot5051.easyfarms.recipe.FarmRecipe;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The effect type registry ({@code easy_farms:upgrade_effect_type}) and the effects that ship with the mod. Another mod
 * registers its own into the registry with key {@link #KEY}; datapacks can then use it by name in farm_upgrade JSON.
 */
public final class UpgradeEffects {
    private UpgradeEffects() {
    }

    public static final ResourceKey<Registry<MapCodec<? extends UpgradeEffect>>> KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(EasyFarms.MODID, "upgrade_effect_type"));
    public static final Registry<MapCodec<? extends UpgradeEffect>> REGISTRY = Services.PLATFORM.createSyncedRegistry(KEY);

    /** Divides the "work" part of a cycle (everything except the animation window). Default: only for toolless recipes. */
    public record CycleTime(double divisor, boolean toolessOnly) implements UpgradeEffect {
        public static final MapCodec<CycleTime> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.DOUBLE.fieldOf("divisor").forGetter(CycleTime::divisor),
                Codec.BOOL.optionalFieldOf("toolless_only", true).forGetter(CycleTime::toolessOnly)
        ).apply(i, CycleTime::new));

        @Override
        public MapCodec<? extends UpgradeEffect> codec() {
            return CODEC;
        }

        @Override
        public double modifyCycleTicks(FarmBlockEntity farm, FarmRecipe r, double ticks) {
            if (toolessOnly && !r.toolless()) return ticks;
            return (ticks - r.visualTicks()) / divisor + r.visualTicks();
        }
    }

    /** Drops with a smelted form use it. */
    public record Smelt() implements UpgradeEffect {
        public static final MapCodec<Smelt> CODEC = MapCodec.unit(new Smelt());

        @Override
        public MapCodec<? extends UpgradeEffect> codec() {
            return CODEC;
        }

        @Override
        public boolean smelts(FarmRecipe r) {
            return r.smeltable();
        }
    }

    /** Recipes marked nether run anywhere. */
    public record NetherEnvironment() implements UpgradeEffect {
        public static final MapCodec<NetherEnvironment> CODEC = MapCodec.unit(new NetherEnvironment());

        @Override
        public MapCodec<? extends UpgradeEffect> codec() {
            return CODEC;
        }

        @Override
        public boolean waivesEnvironment(FarmRecipe r) {
            return r.nether();
        }
    }

    /** Inputs are never used up. */
    public record InfiniteInputs() implements UpgradeEffect {
        public static final MapCodec<InfiniteInputs> CODEC = MapCodec.unit(new InfiniteInputs());

        @Override
        public MapCodec<? extends UpgradeEffect> codec() {
            return CODEC;
        }

        @Override
        public boolean infiniteInputs() {
            return true;
        }
    }

    /** The farm only works while it has a redstone signal. */
    public record RedstoneControl() implements UpgradeEffect {
        public static final MapCodec<RedstoneControl> CODEC = MapCodec.unit(new RedstoneControl());

        @Override
        public MapCodec<? extends UpgradeEffect> codec() {
            return CODEC;
        }

        @Override
        public boolean pausesFarm(FarmBlockEntity farm) {
            return farm.getLevel() != null && !farm.getLevel().hasNeighborSignal(farm.getBlockPos());
        }
    }

    /** Multiplies how long inputs last (2 = twice as long). */
    public record InputLifetime(double multiplier) implements UpgradeEffect {
        public static final MapCodec<InputLifetime> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.DOUBLE.fieldOf("multiplier").forGetter(InputLifetime::multiplier)
        ).apply(i, InputLifetime::new));

        @Override
        public MapCodec<? extends UpgradeEffect> codec() {
            return CODEC;
        }

        @Override
        public double inputLifetimeMultiplier() {
            return multiplier;
        }
    }

    /** Multiplies the item counts of each finished cycle; a fractional part is a chance for one more of the stack. */
    public record DropMultiplier(double multiplier) implements UpgradeEffect {
        public static final MapCodec<DropMultiplier> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.DOUBLE.fieldOf("multiplier").forGetter(DropMultiplier::multiplier)
        ).apply(i, DropMultiplier::new));

        @Override
        public MapCodec<? extends UpgradeEffect> codec() {
            return CODEC;
        }

        @Override
        public void modifyDrops(FarmBlockEntity farm, ServerLevel level, FarmRecipe r, List<ItemStack> drops) {
            List<ItemStack> scaled = new ArrayList<>();
            for (ItemStack stack : drops) {
                double exact = stack.getCount() * multiplier;
                int count = (int) exact + (level.getRandom().nextDouble() < exact - (int) exact ? 1 : 0);
                if (count > 0) scaled.add(stack.copyWithCount(count));
            }
            drops.clear();
            drops.addAll(scaled);
        }
    }

    /**
     * Turns finished drops into other items, like a machine working on the farm's output: a log into a casing, an ingot
     * into a sheet, an ore into crushed ore with a chance of a bonus. Everything is data, so any mod's items can be used.
     * <pre>{"type": "easy_farms:convert_drops", "rules": [
     *   {"from": "#minecraft:logs", "to": "create:andesite_casing"},
     *   {"from": "minecraft:raw_iron", "per": 1, "to": [{"item": "create:crushed_raw_iron"}, {"item": "create:experience_nugget", "chance": 0.75}]}
     * ]}</pre>
     * A drop stack is handled by the first rule whose {@code from} matches it. {@code per} input items make one
     * conversion (default 1); leftover items stay as they were. If none of a rule's output items exist (the other mod is
     * not installed) the rule is skipped and the drops are left alone. Rules in a card are tried in order; cards run in
     * slot order, so an earlier card sees the drops first.
     */
    public record ConvertDrops(List<Rule> rules) implements UpgradeEffect {
        /** One result of a conversion. {@code count} items are made per conversion, each conversion with this {@code chance}. */
        public record Output(Identifier item, int count, float chance) {
            private static final Codec<Output> FULL = RecordCodecBuilder.create(i -> i.group(
                    Identifier.CODEC.fieldOf("item").forGetter(Output::item),
                    Codec.INT.optionalFieldOf("count", 1).forGetter(Output::count),
                    Codec.FLOAT.optionalFieldOf("chance", 1F).forGetter(Output::chance)
            ).apply(i, Output::new));
            public static final Codec<Output> CODEC = Codec.withAlternative(FULL, Identifier.CODEC.xmap(id -> new Output(id, 1, 1F), Output::item));
        }

        /**
         * An extra ingredient a conversion uses up, next to the rule's own {@code from}. {@code source} is {@code "drops"}
         * (default: taken from the same cycle's drops) or {@code "catalyst"} (taken from the card's catalyst slot, which the
         * player fills in the farm's Catalysts screen; such a card makes that screen's button appear).
         */
        public record Extra(String from, int per, boolean catalyst) {
            public static final Codec<Extra> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.STRING.fieldOf("from").forGetter(Extra::from),
                    Codec.intRange(1, 64).optionalFieldOf("per", 1).forGetter(Extra::per),
                    Codec.STRING.optionalFieldOf("source", "drops").forGetter(e -> e.catalyst() ? "catalyst" : "drops")
            ).apply(i, (from, per, source) -> new Extra(from, per, "catalyst".equals(source))));
        }

        /**
         * {@code from} is an item id, or {@code #namespace:tag} for an item tag. With {@code with} the rule needs all of
         * its ingredients at once (copper + zinc ingots -> brass); one conversion uses {@code per} of {@code from} and
         * each extra's {@code per}, and makes as many conversions as the scarcest ingredient allows.
         */
        public record Rule(String from, int per, List<Extra> with, List<Output> to) {
            private static final Codec<List<Output>> OUTPUTS = Codec.withAlternative(Output.CODEC.listOf(),
                    Output.CODEC.xmap(List::of, l -> l.get(0)));
            public static final Codec<Rule> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.STRING.fieldOf("from").forGetter(Rule::from),
                    Codec.intRange(1, 64).optionalFieldOf("per", 1).forGetter(Rule::per),
                    Extra.CODEC.listOf().optionalFieldOf("with", List.of()).forGetter(Rule::with),
                    OUTPUTS.fieldOf("to").forGetter(Rule::to)
            ).apply(i, Rule::new));

            boolean matches(ItemStack stack) {
                return matchesItem(from, stack);
            }

            public static boolean matchesItem(String from, ItemStack stack) {
                if (from.startsWith("#")) {
                    Identifier tag = Identifier.tryParse(from.substring(1));
                    return tag != null && stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, tag));
                }
                Identifier id = Identifier.tryParse(from);
                return id != null && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id);
            }

            /** False when none of the outputs exist, so a rule for a missing mod does nothing. */
            boolean usable() {
                for (Output o : to) if (net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(o.item())) return true;
                return false;
            }
        }

        public static final MapCodec<ConvertDrops> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Rule.CODEC.listOf().fieldOf("rules").forGetter(ConvertDrops::rules)
        ).apply(i, ConvertDrops::new));

        @Override
        public MapCodec<? extends UpgradeEffect> codec() {
            return CODEC;
        }

        @Override
        public void modifyDrops(FarmBlockEntity farm, ServerLevel level, FarmRecipe r, List<ItemStack> drops) {
            List<ItemStack> result = new ArrayList<>();
            List<ItemStack> combined = combine(farm, level, drops);
            for (ItemStack stack : drops) {
                Rule rule = null;
                for (Rule candidate : rules) {
                    if (candidate.with().isEmpty() && candidate.usable() && candidate.matches(stack)) {
                        rule = candidate;
                        break;
                    }
                }
                if (rule == null) {
                    add(result, stack);
                    continue;
                }
                int conversions = stack.getCount() / rule.per();
                int leftover = stack.getCount() - conversions * rule.per();
                if (leftover > 0) add(result, stack.copyWithCount(leftover));
                for (Output out : rule.to()) {
                    var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(out.item());
                    if (item.isEmpty()) continue;
                    int made = 0;
                    for (int c = 0; c < conversions; c++) {
                        if (out.chance() >= 1F || level.getRandom().nextFloat() < out.chance()) made += out.count();
                    }
                    if (made > 0) add(result, new ItemStack(item.get(), made));
                }
            }
            for (ItemStack made : combined) add(result, made);
            drops.clear();
            drops.addAll(result);
        }

        /**
         * Multi-ingredient rules ({@code with}): takes their ingredients out of {@code drops} (and the farm's catalyst slots
         * for "catalyst" ingredients) and returns what they make, so single-item rules do not convert it again.
         */
        private List<ItemStack> combine(FarmBlockEntity farm, ServerLevel level, List<ItemStack> drops) {
            List<ItemStack> made = new ArrayList<>();
            for (Rule rule : rules) {
                if (rule.with().isEmpty() || !rule.usable()) continue;
                int conversions = countIn(drops, rule.from()) / rule.per();
                for (Extra extra : rule.with()) {
                    int available = extra.catalyst() ? countIn(farm.catalysts(), extra.from()) : countIn(drops, extra.from());
                    conversions = Math.min(conversions, available / extra.per());
                }
                if (conversions <= 0) continue;
                takeFrom(drops, rule.from(), conversions * rule.per());
                for (Extra extra : rule.with()) {
                    // The Infinity card keeps catalysts as well as the tool: they are needed but not used up.
                    if (extra.catalyst()) {
                        if (!farm.hasInfiniteInputs()) takeFrom(farm.catalysts(), extra.from(), conversions * extra.per());
                    }
                    else takeFrom(drops, extra.from(), conversions * extra.per());
                }
                for (Output out : rule.to()) {
                    var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(out.item());
                    if (item.isEmpty()) continue;
                    int count = 0;
                    for (int c = 0; c < conversions; c++) {
                        if (out.chance() >= 1F || level.getRandom().nextFloat() < out.chance()) count += out.count();
                    }
                    if (count > 0) add(made, new ItemStack(item.get(), count));
                }
            }
            return made;
        }

        private static int countIn(List<ItemStack> stacks, String from) {
            int total = 0;
            for (ItemStack stack : stacks) if (Rule.matchesItem(from, stack)) total += stack.getCount();
            return total;
        }

        private static int countIn(com.awesomeshot5051.easyfarms.util.ItemStackHandler handler, String from) {
            int total = 0;
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (Rule.matchesItem(from, stack)) total += stack.getCount();
            }
            return total;
        }

        private static void takeFrom(List<ItemStack> stacks, String from, int amount) {
            for (ItemStack stack : stacks) {
                if (amount <= 0) break;
                if (!Rule.matchesItem(from, stack)) continue;
                int taken = Math.min(amount, stack.getCount());
                stack.shrink(taken);
                amount -= taken;
            }
            stacks.removeIf(ItemStack::isEmpty);
        }

        private static void takeFrom(com.awesomeshot5051.easyfarms.util.ItemStackHandler handler, String from, int amount) {
            for (int i = 0; i < handler.getSlots() && amount > 0; i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!Rule.matchesItem(from, stack)) continue;
                amount -= handler.extractItem(i, Math.min(amount, stack.getCount()), false).getCount();
            }
        }

        @Override
        public List<com.awesomeshot5051.easyfarms.farm.DropEntry> previewDrops(List<com.awesomeshot5051.easyfarms.farm.DropEntry> drops) {
            List<com.awesomeshot5051.easyfarms.farm.DropEntry> result = new ArrayList<>();
            for (var drop : drops) {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(drop.item());
                Rule rule = null;
                if (item.isPresent()) {
                    ItemStack probe = new ItemStack(item.get());
                    for (Rule candidate : rules) {
                        if (candidate.with().isEmpty() && candidate.usable() && candidate.matches(probe)) {
                            rule = candidate;
                            break;
                        }
                    }
                }
                if (rule == null) {
                    result.add(drop);
                    continue;
                }
                int lo = Math.max(1, drop.min() / rule.per()), hi = Math.max(1, drop.max() / rule.per());
                for (Output out : rule.to()) {
                    if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(out.item())) continue;
                    var made = com.awesomeshot5051.easyfarms.farm.DropEntry.simple(out.item(), out.count() * lo, out.count() * hi);
                    result.add(new com.awesomeshot5051.easyfarms.farm.DropEntry(made.item(), made.min(), made.max(),
                            drop.chance() * out.chance(), java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(),
                            java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(), false));
                }
            }
            return result;
        }

        /** Adds the stack to the list, merging with an equal stack and splitting past the item's stack size. */
        private static void add(List<ItemStack> list, ItemStack stack) {
            int remaining = stack.getCount();
            for (ItemStack existing : list) {
                if (remaining <= 0) break;
                if (ItemStack.isSameItemSameComponents(existing, stack)) {
                    int room = existing.getMaxStackSize() - existing.getCount();
                    int moved = Math.min(room, remaining);
                    existing.grow(moved);
                    remaining -= moved;
                }
            }
            while (remaining > 0) {
                int part = Math.min(remaining, stack.getMaxStackSize());
                list.add(stack.copyWithCount(part));
                remaining -= part;
            }
        }
    }

    /** The Inscriber card: recipes with an "inscriber" block drop that block's drops instead, once the press (if named) is installed. */
    public record Inscribe() implements UpgradeEffect {
        public static final MapCodec<Inscribe> CODEC = MapCodec.unit(new Inscribe());

        @Override
        public MapCodec<? extends UpgradeEffect> codec() {
            return CODEC;
        }

        @Override
        public void modifyDrops(FarmBlockEntity farm, ServerLevel level, FarmRecipe r, List<ItemStack> drops) {
            r.inscriber().ifPresent(ins -> {
                if (ins.press().isPresent() && !farm.hasPress(ins.press().get())) return;
                drops.clear();
                farm.rollInto(ins.drops(), drops);
            });
        }
    }

    public static final Supplier<MapCodec<ConvertDrops>> CONVERT_DROPS = Services.PLATFORM.register(KEY, "convert_drops", () -> ConvertDrops.CODEC);
    public static final Supplier<MapCodec<Inscribe>> INSCRIBE = Services.PLATFORM.register(KEY, "inscribe", () -> Inscribe.CODEC);
    public static final Supplier<MapCodec<CycleTime>> CYCLE_TIME = Services.PLATFORM.register(KEY, "cycle_time", () -> CycleTime.CODEC);
    public static final Supplier<MapCodec<Smelt>> SMELT = Services.PLATFORM.register(KEY, "smelt", () -> Smelt.CODEC);
    public static final Supplier<MapCodec<NetherEnvironment>> NETHER_ENVIRONMENT = Services.PLATFORM.register(KEY, "nether_environment", () -> NetherEnvironment.CODEC);
    public static final Supplier<MapCodec<InfiniteInputs>> INFINITE_INPUTS = Services.PLATFORM.register(KEY, "infinite_inputs", () -> InfiniteInputs.CODEC);
    public static final Supplier<MapCodec<RedstoneControl>> REDSTONE_CONTROL = Services.PLATFORM.register(KEY, "redstone_control", () -> RedstoneControl.CODEC);
    public static final Supplier<MapCodec<InputLifetime>> INPUT_LIFETIME = Services.PLATFORM.register(KEY, "input_lifetime", () -> InputLifetime.CODEC);
    public static final Supplier<MapCodec<DropMultiplier>> DROP_MULTIPLIER = Services.PLATFORM.register(KEY, "drop_multiplier", () -> DropMultiplier.CODEC);

    public static void init() {
        // class load registers the effect types
    }
}
