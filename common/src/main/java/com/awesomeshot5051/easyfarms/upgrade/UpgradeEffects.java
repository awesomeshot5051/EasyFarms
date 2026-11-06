package com.awesomeshot5051.easyfarms.upgrade;

import com.awesomeshot5051.easyfarms.Config;
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
    public record CycleTime(double divisor, boolean toolessOnly, java.util.Map<com.awesomeshot5051.easyfarms.farm.FarmKind, Double> kindDivisors) implements UpgradeEffect {
        public static final MapCodec<CycleTime> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.DOUBLE.fieldOf("divisor").forGetter(CycleTime::divisor),
                Codec.BOOL.optionalFieldOf("toolless_only", true).forGetter(CycleTime::toolessOnly),
                Codec.unboundedMap(com.awesomeshot5051.easyfarms.farm.FarmKind.CODEC, Codec.DOUBLE).optionalFieldOf("kind_divisors", java.util.Map.of()).forGetter(CycleTime::kindDivisors)
        ).apply(i, CycleTime::new));

        @Override
        public MapCodec<? extends UpgradeEffect> codec() {
            return CODEC;
        }

        @Override
        public double modifyCycleTicks(FarmBlockEntity farm, FarmRecipe r, double ticks) {
            if (toolessOnly && !r.toolless()) return ticks;
            double d = kindDivisors.getOrDefault(farm.kind(), divisor);
            return (ticks - r.visualTicks()) / d + r.visualTicks();
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
                            java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(), false, java.util.Optional.empty(), java.util.List.of()));
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

    /**
     * The Powered card: the farm runs on energy (ME / FE / E) delivered by pipes, cables or any energy mod, and works at the speed of
     * another tool tier (diamond by default). It stops while less than {@code min_energy} is stored.
     * <pre>{"type": "easy_farms:powered", "capacity": 1000, "min_energy": 100, "energy_per_tick": 2,
     *  "tier": "easy_farms:diamond", "toolless_divisor": 4}</pre>
     * {@code tier} is a farm_tier id whose speed replaces the tool's. Recipes without a tool ignore tiers, so they are
     * divided by {@code toolless_divisor} instead (like the Speed card).
     */
    public record Powered(long capacity, long minEnergy, long energyPerTick, Identifier tier, double toollessDivisor) implements UpgradeEffect {
        public static final MapCodec<Powered> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.LONG.optionalFieldOf("capacity", 1000L).forGetter(Powered::capacity),
                Codec.LONG.optionalFieldOf("min_energy", 100L).forGetter(Powered::minEnergy),
                Codec.LONG.optionalFieldOf("energy_per_tick", 2L).forGetter(Powered::energyPerTick),
                Identifier.CODEC.optionalFieldOf("tier", Identifier.fromNamespaceAndPath(EasyFarms.MODID, "diamond")).forGetter(Powered::tier),
                Codec.DOUBLE.optionalFieldOf("toolless_divisor", 4.0).forGetter(Powered::toollessDivisor)
        ).apply(i, Powered::new));

        @Override
        public MapCodec<? extends UpgradeEffect> codec() {
            return CODEC;
        }

        @Override
        public boolean available() {
            return Services.PLATFORM.energyAvailable();   // only the energy API itself (always there on NeoForge)
        }

        /** Always shown as a hint: any mod that supplies energy will do, and there is no reliable way to detect one. */
        @Override
        public net.minecraft.network.chat.Component unavailableReason() {
            return net.minecraft.network.chat.Component.translatable("tooltip.easy_farms.energy_mod_required");
        }

        @Override
        public long energyCapacity() {
            return capacity;
        }

        @Override
        public boolean pausesFarm(FarmBlockEntity farm) {
            return farm.energyStored() < Math.max(1, minEnergy);
        }

        @Override
        public void onWorkTick(FarmBlockEntity farm) {
            double cost = energyPerTick;
            for (UpgradeEffect e : farm.effects()) cost *= e.energyUseMultiplier();
            farm.drainEnergy(cost);
        }

        @Override
        public com.awesomeshot5051.easyfarms.tier.FarmTier overrideTier(FarmBlockEntity farm, com.awesomeshot5051.easyfarms.tier.FarmTier current) {
            if (farm.getLevel() == null) return current;
            var registry = farm.getLevel().registryAccess().lookupOrThrow(com.awesomeshot5051.easyfarms.tier.FarmTier.REGISTRY_KEY);
            var powered = registry.getValue(tier);
            if (powered == null) return current;
            return current == null || powered.speedDivisor() > current.speedDivisor() ? powered : current;
        }

        @Override
        public double modifyCycleTicks(FarmBlockEntity farm, FarmRecipe r, double ticks) {
            if (!r.toolless() || toollessDivisor <= 1) return ticks;
            return (ticks - r.visualTicks()) / toollessDivisor + r.visualTicks();
        }
    }

    /**
     * The Capacity card. Each installed card multiplies: the energy buffer of a Powered farm, the tank of a Liquid Farm, and
     * the stack size of the output slots. Stacking comes from the card's {@code max_count}.
     * <pre>{"type": "easy_farms:capacity", "energy_multiplier": 2, "fluid_multiplier": 2, "item_multiplier": 2, "unstackable_base": 8}</pre>
     */
    public record Capacity(double energyMultiplier, double fluidMultiplier, double itemMultiplier, int unstackableBase) implements UpgradeEffect {
        public static final MapCodec<Capacity> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("energy_multiplier", 2.0).forGetter(Capacity::energyMultiplier),
                Codec.DOUBLE.optionalFieldOf("fluid_multiplier", 2.0).forGetter(Capacity::fluidMultiplier),
                Codec.DOUBLE.optionalFieldOf("item_multiplier", 2.0).forGetter(Capacity::itemMultiplier),
                Codec.INT.optionalFieldOf("unstackable_base", 8).forGetter(Capacity::unstackableBase)
        ).apply(i, Capacity::new));

        @Override
        public MapCodec<? extends UpgradeEffect> codec() {
            return CODEC;
        }

        @Override
        public double energyCapacityMultiplier() {
            return energyMultiplier;
        }

        @Override
        public double fluidCapacityMultiplier() {
            return fluidMultiplier;
        }

        @Override
        public double itemStackMultiplier() {
            return itemMultiplier;
        }

        @Override
        public int unstackableBase() {
            return unstackableBase;
        }
    }

    /** The Efficiency card: each installed card multiplies the energy a Powered farm uses per working tick. */
    public record Efficiency(double energyUseMultiplier) implements UpgradeEffect {
        public static final MapCodec<Efficiency> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("energy_use_multiplier", 0.5).forGetter(Efficiency::energyUseMultiplier)
        ).apply(i, Efficiency::new));

        @Override
        public MapCodec<? extends UpgradeEffect> codec() {
            return CODEC;
        }
    }

    /** The Chunk Loader card: the farm keeps its own chunk loaded, so it works while nobody is nearby. */
    public record ChunkLoader() implements UpgradeEffect {
        public static final MapCodec<ChunkLoader> CODEC = MapCodec.unit(new ChunkLoader());

        @Override
        public MapCodec<? extends UpgradeEffect> codec() {
            return CODEC;
        }

        @Override
        public boolean loadsChunk() {
            return true;
        }
    }

    /** The Void card: items that do not fit the output slots are destroyed, so the farm never stalls on a full output. */
    public record VoidOverflow() implements UpgradeEffect {
        public static final MapCodec<VoidOverflow> CODEC = MapCodec.unit(new VoidOverflow());

        @Override
        public MapCodec<? extends UpgradeEffect> codec() {
            return CODEC;
        }

        @Override
        public boolean voidsOverflow() {
            return Config.VOID_ENABLED.get();
        }
    }

    /** The Experience card: the farm drops the recipe's experience (its {@code xp} field). Put it on the kinds you want in the card's JSON. */
    public record Experience() implements UpgradeEffect {
        public static final MapCodec<Experience> CODEC = MapCodec.unit(new Experience());

        @Override
        public MapCodec<? extends UpgradeEffect> codec() {
            return CODEC;
        }

        @Override
        public boolean grantsExperience(FarmBlockEntity farm) {
            return Config.XP_ENABLED.get();
        }
    }

    /** The Filter card: a per-recipe list of blocked outputs (set on the card's own screen). Blocked items are discarded. */
    public record OutputFilter() implements UpgradeEffect {
        public static final MapCodec<OutputFilter> CODEC = MapCodec.unit(new OutputFilter());

        @Override
        public MapCodec<? extends UpgradeEffect> codec() {
            return CODEC;
        }

        @Override
        public boolean filtersOutputs() {
            return Config.FILTER_ENABLED.get();
        }
    }

    /** The Auto-Export card: outputs are pushed to neighbouring containers, and each side of the farm can be input, output or off. */
    public record AutoExport() implements UpgradeEffect {
        public static final MapCodec<AutoExport> CODEC = MapCodec.unit(new AutoExport());

        @Override
        public MapCodec<? extends UpgradeEffect> codec() {
            return CODEC;
        }

        @Override
        public boolean exportsOutputs() {
            return Config.AUTO_EXPORT_ENABLED.get();
        }
    }

    public static final Supplier<MapCodec<VoidOverflow>> VOID_OVERFLOW = Services.PLATFORM.register(KEY, "void_overflow", () -> VoidOverflow.CODEC);
    public static final Supplier<MapCodec<Experience>> EXPERIENCE = Services.PLATFORM.register(KEY, "experience", () -> Experience.CODEC);
    public static final Supplier<MapCodec<OutputFilter>> OUTPUT_FILTER = Services.PLATFORM.register(KEY, "output_filter", () -> OutputFilter.CODEC);
    public static final Supplier<MapCodec<AutoExport>> AUTO_EXPORT = Services.PLATFORM.register(KEY, "auto_export", () -> AutoExport.CODEC);
    public static final Supplier<MapCodec<ChunkLoader>> CHUNK_LOADER = Services.PLATFORM.register(KEY, "chunk_loader", () -> ChunkLoader.CODEC);
    public static final Supplier<MapCodec<Efficiency>> EFFICIENCY = Services.PLATFORM.register(KEY, "efficiency", () -> Efficiency.CODEC);
    public static final Supplier<MapCodec<Powered>> POWERED = Services.PLATFORM.register(KEY, "powered", () -> Powered.CODEC);
    public static final Supplier<MapCodec<Capacity>> CAPACITY = Services.PLATFORM.register(KEY, "capacity", () -> Capacity.CODEC);
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
