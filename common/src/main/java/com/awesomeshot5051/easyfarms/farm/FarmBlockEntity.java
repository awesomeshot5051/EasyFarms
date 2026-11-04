package com.awesomeshot5051.easyfarms.farm;

import com.awesomeshot5051.easyfarms.Config;
import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.recipe.FarmRecipe;
import com.awesomeshot5051.easyfarms.recipe.FarmRecipeInput;
import com.awesomeshot5051.easyfarms.recipe.ModRecipes;
import com.awesomeshot5051.easyfarms.registry.ModDataComponents;
import com.awesomeshot5051.easyfarms.registry.ModItems;
import com.awesomeshot5051.easyfarms.upgrade.UpgradeEffect;
import com.awesomeshot5051.easyfarms.tier.FarmTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.DyeColor;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import com.awesomeshot5051.easyfarms.util.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The block entity behind all three farms. It holds two input slots, the tool slot, four outputs and the upgrade cards,
 * finds the machine recipe that matches the inputs + tool, and runs it.
 */
public class FarmBlockEntity extends BlockEntity {
    public static final int INPUTS = 3;
    public static final int OUTPUTS = 4;
    public static final int UPGRADE_SLOTS = 18;
    private static final long TICKS_PER_DAY = 24000L;

    private final ItemStackHandler inputs = new ItemStackHandler(INPUTS) {
        @Override
        protected void onContentsChanged(int slot) {
            contentsChanged();
        }
    };
    private final ItemStackHandler toolSlot = new ItemStackHandler(1) {
        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            operatingTicks = 0;   // a different tool starts its own lifetime
            toolWear = 0;
            contentsChanged();
        }
    };
    private final ItemStackHandler outputs = new ItemStackHandler(OUTPUTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** The Inscriber card's press (an item in the easy_farms:inscriber_presses tag). Never consumed. */
    private final ItemStackHandler press = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(PRESS_TAG);
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            contentsChanged();
        }
    };
    public static final net.minecraft.tags.TagKey<Item> PRESS_TAG = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.awesomeshot5051.easyfarms.EasyFarms.MODID, "inscriber_presses"));

    /** The farm's 18 catalyst slots, shared out between the installed cards (see {@link #catalystOwners()}). */
    private final ItemStackHandler catalysts = new ItemStackHandler(UPGRADE_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isCatalystValid(slot, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ItemStackHandler upgrades = new ItemStackHandler(UPGRADE_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return canInstall(stack, slot);
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            contentsChanged();
            if (catalystSync) catalystDirty = true;
            else reconcileCatalysts();
        }
    };

    /** Outside view for hoppers and pipes: inputs accept, outputs only give. Each loader exposes it through its own item-transfer API. */
    private final WorldlyContainer automation = new WorldlyContainer() {
        private final int[] slots = {0, 1, 2, 3, 4, 5, 6};

        @Override
        public int[] getSlotsForFace(Direction side) {
            return slots;
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
            return slot < INPUTS && inputs.isItemValid(slot, stack);
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
            return slot >= INPUTS;
        }

        @Override
        public int getContainerSize() {
            return INPUTS + OUTPUTS;
        }

        @Override
        public boolean isEmpty() {
            return inputs.isEmpty() && outputs.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot < INPUTS ? inputs.getStackInSlot(slot) : outputs.getStackInSlot(slot - INPUTS);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return slot < INPUTS ? ItemStack.EMPTY : outputs.extractItem(slot - INPUTS, amount, false);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return slot < INPUTS ? ItemStack.EMPTY : outputs.removeItemNoUpdate(slot - INPUTS);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (slot < INPUTS) inputs.setStackInSlot(slot, stack);
            else outputs.setStackInSlot(slot - INPUTS, stack);
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot < INPUTS && inputs.isItemValid(slot, stack);
        }

        @Override
        public void setChanged() {
            FarmBlockEntity.this.setChanged();
        }

        @Override
        public boolean stillValid(net.minecraft.world.entity.player.Player player) {
            return false;
        }

        @Override
        public void clearContent() {
        }
    };

    private final FarmKind kind;
    private boolean recipeDirty = true;
    /** Fractions of a durability point owed to the tool; it wears out exactly as the lifetime ends. */
    private double toolWear;
    private final int[] catalystOwner = newCatalystOwners();
    /** Set after loading or restoring a farm without a saved layout: the next tick adopts the canonical layout without moving stacks. */
    private boolean catalystSync;
    private boolean catalystDirty;
    private List<UpgradeEffect> effectCache;
    /** Cards with a block-below rule depend on the world, so their cached effects are rebuilt now and then. */
    private boolean effectsDependOnWorld;
    private long effectCacheTick;
    private java.util.UUID lastUser;
    @Nullable
    private RecipeHolder<FarmRecipe> recipe;
    private DyeColor color = DyeColor.WHITE;
    /** Set while a Silk Touch break is in progress: the contents travel with the item instead of dropping. */
    private boolean keepContents;
    private long timer;
    /** Ticks the recipe has really been running since its inputs were last used up. */
    private long operatingTicks;

    // Client copies of what the renderer needs; the server fills them through the update tag.
    @Nullable
    private FarmRecipe.Visual clientVisual;
    private double clientCycleTicks = 1;
    private int clientVisualTicks;

    public FarmBlockEntity(BlockPos pos, BlockState state) {
        super(EasyFarms.FARM_BE.get(), pos, state);
        this.kind = ((FarmBlock) state.getBlock()).kind();
    }

    public FarmKind kind() {
        return kind;
    }

    public ItemStackHandler inputs() {
        return inputs;
    }

    public ItemStackHandler toolSlot() {
        return toolSlot;
    }

    public ItemStackHandler catalysts() {
        return catalysts;
    }

    /**
     * The catalyst items the card in this upgrade slot needs, one entry per distinct catalyst ingredient (so one catalyst
     * slot each); empty if there is no card, or it needs none.
     */
    public List<String> catalystItemsFor(int slot) {
        if (level == null) return List.of();
        com.awesomeshot5051.easyfarms.tier.FarmUpgrade upgrade = com.awesomeshot5051.easyfarms.tier.FarmUpgrade.find(level.registryAccess(), upgrades.getStackInSlot(slot));
        return upgrade == null ? List.of() : upgrade.catalystItems();
    }

    /** Bit i is set while the card in upgrade slot i needs a catalyst (shown by the Catalysts button). */
    public int catalystMask() {
        int mask = 0;
        for (int i = 0; i < UPGRADE_SLOTS; i++) if (!catalystItemsFor(i).isEmpty()) mask |= 1 << i;
        return mask;
    }

    // ---- catalyst slots: 18 in all, shared out between the installed cards ----

    /** For each catalyst slot, the upgrade slot whose card owns it, or -1. A card owns one slot per catalyst ingredient. */
    public int[] catalystOwners() {
        return catalystOwner.clone();
    }

    /** The catalyst slots owned by this upgrade slot's card, ascending; the j-th takes the card's j-th catalyst item. */
    public List<Integer> catalystSlotsOf(int upgradeSlot) {
        List<Integer> owned = new ArrayList<>();
        for (int h = 0; h < catalystOwner.length; h++) if (catalystOwner[h] == upgradeSlot) owned.add(h);
        return owned;
    }

    /** Which item the catalyst slot takes: the matching catalyst item of the card that owns it, or null if unowned. */
    @org.jetbrains.annotations.Nullable
    public String catalystItemOf(int catalystSlot) {
        int owner = catalystOwner[catalystSlot];
        if (owner < 0) return null;
        List<String> items = catalystItemsFor(owner);
        int sub = catalystSlotsOf(owner).indexOf(catalystSlot);
        return sub >= 0 && sub < items.size() ? items.get(sub) : null;
    }

    public boolean isCatalystValid(int catalystSlot, ItemStack stack) {
        String item = catalystItemOf(catalystSlot);
        return item != null && com.awesomeshot5051.easyfarms.upgrade.UpgradeRules.matchesAny(List.of(item), stack);
    }

    /** The layout the slots should have: each installed card, in slot order, gets the next free catalyst slots. */
    private int[] canonicalCatalystLayout() {
        int[] layout = new int[UPGRADE_SLOTS];
        java.util.Arrays.fill(layout, -1);
        int next = 0;
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            for (int n = catalystItemsFor(i).size(); n > 0 && next < UPGRADE_SLOTS; n--) layout[next++] = i;
        }
        return layout;
    }

    /**
     * Rebuilds the catalyst layout after the cards changed. Stacks follow their (card, n-th ingredient) to the new slot;
     * a stack that no longer has a home, or no longer fits its slot, goes back into the world.
     */
    private void reconcileCatalysts() {
        if (level == null || level.isClientSide) return;
        int[] target = canonicalCatalystLayout();
        if (java.util.Arrays.equals(target, catalystOwner) && catalystStacksFit()) return;
        // Key of an old slot: owner * 64 + n-th slot of that owner.
        java.util.Map<Integer, ItemStack> keyed = new java.util.HashMap<>();
        int[] seen = new int[UPGRADE_SLOTS];
        for (int h = 0; h < UPGRADE_SLOTS; h++) {
            ItemStack stack = catalysts.getStackInSlot(h);
            int owner = catalystOwner[h];
            if (owner < 0) {
                dropCatalyst(stack);
            } else if (!stack.isEmpty()) {
                keyed.put(owner * 64 + seen[owner], stack.copy());
            }
            if (owner >= 0) seen[owner]++;
            catalysts.setStackInSlot(h, ItemStack.EMPTY);
        }
        System.arraycopy(target, 0, catalystOwner, 0, UPGRADE_SLOTS);
        int[] placed = new int[UPGRADE_SLOTS];
        for (int h = 0; h < UPGRADE_SLOTS; h++) {
            int owner = catalystOwner[h];
            if (owner < 0) continue;
            int sub = placed[owner]++;
            ItemStack stack = keyed.remove(owner * 64 + sub);
            if (stack == null) continue;
            if (isCatalystValid(h, stack)) catalysts.setStackInSlot(h, stack);
            else dropCatalyst(stack);
        }
        for (ItemStack leftover : keyed.values()) dropCatalyst(leftover);
        setChanged();
    }

    private boolean catalystStacksFit() {
        for (int h = 0; h < UPGRADE_SLOTS; h++) {
            ItemStack stack = catalysts.getStackInSlot(h);
            if (!stack.isEmpty() && !isCatalystValid(h, stack)) return false;
        }
        return true;
    }

    private void dropCatalyst(ItemStack stack) {
        if (!stack.isEmpty()) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), stack);
    }

    public ItemStackHandler outputs() {
        return outputs;
    }

    public ItemStackHandler upgrades() {
        return upgrades;
    }

    public WorldlyContainer automation() {
        return automation;
    }

    public ItemStackHandler pressSlot() {
        return press;
    }

    /** True if the named press is installed. */
    public boolean hasPress(net.minecraft.resources.ResourceLocation id) {
        ItemStack stack = press.getStackInSlot(0);
        return !stack.isEmpty() && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id);
    }

    /** Rolls drop entries with this farm's tool enchantments and cards. */
    public void rollInto(List<DropEntry> entries, List<ItemStack> out) {
        if (!(level instanceof ServerLevel server) || recipe == null) return;
        boolean smelter = false;
        for (UpgradeEffect e : effects()) smelter |= e.smelts(recipe.value());
        boolean fortune = enchantLevel(Enchantments.FORTUNE) > 0;
        boolean silk = enchantLevel(Enchantments.SILK_TOUCH) > 0;
        boolean looting = kind == FarmKind.MOB && enchantLevel(Enchantments.LOOTING) > 0;
        boolean fire = enchantLevel(Enchantments.FIRE_ASPECT) > 0;
        for (DropEntry entry : entries) entry.roll(server, fortune, looting, silk, smelter, fire, out);
    }

    public ItemStack getTool() {
        return toolSlot.getStackInSlot(0);
    }

    /**
     * Nether recipes run anywhere with the Nether upgrade installed. Without it, a recipe that names an environment
     * (dimension, block under the farm) only runs there; recipes without one always run.
     */
    public boolean environmentOk() {
        if (recipe == null || level == null) return true;
        FarmRecipe r = recipe.value();
        for (UpgradeEffect e : effects()) if (e.waivesEnvironment(r)) return true;
        return r.environment().map(env -> env.satisfiedAt(level, worldPosition)).orElse(true);
    }

    public long getTimer() {
        return timer;
    }

    public boolean isRunning() {
        return level != null && level.isClientSide ? clientVisual != null : recipe != null;
    }

    @Nullable
    public FarmRecipe.Visual getVisual() {
        return level != null && level.isClientSide ? clientVisual : (recipe == null ? null : resolvedVisual(recipe.value()));
    }

    private FarmRecipe.Visual resolvedVisual(FarmRecipe r) {
        return r.dyeable() ? new FarmRecipe.Visual(r.visual().type(), DyeColors.recolor(r.visual().target(), color)) : r.visual();
    }

    public DyeColor getColor() {
        return color;
    }

    /** Dyeing works while the farm runs a dyeable recipe, or while it is idle (so a colour can be set up front). */
    public boolean canDye() {
        return recipe == null || recipe.value().dyeable();
    }

    public void setColor(DyeColor newColor) {
        if (color == newColor) return;
        color = newColor;
        sync();
    }

    /**
     * True if this farm accepts the card in this slot: it is a registered card for this farm kind, the farm does not
     * hold that card in another slot yet, and the card's restrictions (exclusive / required cards, tool enchantments)
     * allow it next to what is installed. Slot {@code -1} means "any free slot".
     */
    public boolean canInstall(ItemStack card, int slot) {
        return installProblem(card, slot) == null && isInstallable(card, slot);
    }

    private boolean isInstallable(ItemStack card, int slot) {
        if (level == null || card.isEmpty()) return false;
        com.awesomeshot5051.easyfarms.tier.FarmUpgrade upgrade = com.awesomeshot5051.easyfarms.tier.FarmUpgrade.find(level.registryAccess(), card);
        if (upgrade == null || !upgrade.kinds().contains(kind)) return false;
        for (int i = 0; i < upgrades.getSlots(); i++) {
            if (i != slot && upgrade.matches(upgrades.getStackInSlot(i))) return false;
        }
        return true;
    }

    /** Why the card's restrictions forbid it here (shown to the player), or null if they do not. */
    @Nullable
    public net.minecraft.network.chat.Component installProblem(ItemStack card, int slot) {
        if (!isInstallable(card, slot)) return null;
        com.awesomeshot5051.easyfarms.tier.FarmUpgrade upgrade = com.awesomeshot5051.easyfarms.tier.FarmUpgrade.find(level.registryAccess(), card);
        return com.awesomeshot5051.easyfarms.upgrade.UpgradeRules.installProblem(rulesContext(), upgrade, installedUpgrades(slot));
    }

    /** The registered upgrades in the slots, skipping {@code exceptSlot}, in slot order. */
    private List<com.awesomeshot5051.easyfarms.tier.FarmUpgrade> installedUpgrades(int exceptSlot) {
        List<com.awesomeshot5051.easyfarms.tier.FarmUpgrade> list = new ArrayList<>();
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack stack = upgrades.getStackInSlot(i);
            if (i == exceptSlot || stack.isEmpty()) continue;
            com.awesomeshot5051.easyfarms.tier.FarmUpgrade upgrade = com.awesomeshot5051.easyfarms.tier.FarmUpgrade.find(level.registryAccess(), stack);
            if (upgrade != null) list.add(upgrade);
        }
        return list;
    }

    /** What card restrictions look at: the recipe the inputs and tool match right now, and the tool. Works on both sides. */
    public com.awesomeshot5051.easyfarms.upgrade.UpgradeRules.Context rulesContext() {
        FarmRecipe current = null;
        if (level != null) {
            FarmRecipeInput input = new FarmRecipeInput(inputSlots(), getTool());
            current = level.getRecipeManager().getRecipeFor(ModRecipes.TYPES_BY_KIND.get(kind).get(), input, level)
                    .map(RecipeHolder::value).orElse(null);
        }
        return new com.awesomeshot5051.easyfarms.upgrade.UpgradeRules.Context(level.registryAccess(), current, getTool(), level.getBlockState(worldPosition.below()));
    }

    /** Puts an upgrade card into the first free upgrade slot if this farm accepts it and doesn't have it yet. */
    public boolean installCard(ItemStack card) {
        if (!canInstall(card, -1)) return false;
        for (int i = 0; i < upgrades.getSlots(); i++) {
            if (upgrades.getStackInSlot(i).isEmpty()) {
                upgrades.setStackInSlot(i, card.copyWithCount(1));
                contentsChanged();
                return true;
            }
        }
        return false;
    }

    public double visualStart() {
        return (level != null && level.isClientSide ? clientCycleTicks : cycleTicks()) - clientVisualTicksOr();
    }

    private double clientVisualTicksOr() {
        return level != null && level.isClientSide ? clientVisualTicks : visualWindow();
    }

    // ---- upgrades ----

    /** The effects of every card installed in this farm (cached; cleared whenever a slot or the farm data changes). */
    public List<UpgradeEffect> effects() {
        List<UpgradeEffect> cached = effectCache;
        if (cached != null && !(effectsDependOnWorld && level != null && level.getGameTime() - effectCacheTick >= 20)) return cached;
        List<UpgradeEffect> list = new ArrayList<>();
        effectsDependOnWorld = false;
        if (level != null) {
            effectCacheTick = level.getGameTime();
            // Cards whose requirements are missing, or that clash with an earlier card or the tool's enchantments, are off.
            for (com.awesomeshot5051.easyfarms.tier.FarmUpgrade upgrade :
                    com.awesomeshot5051.easyfarms.upgrade.UpgradeRules.working(rulesContext(), installedUpgrades(-1))) {
                list.addAll(upgrade.effects());
            }
            for (com.awesomeshot5051.easyfarms.tier.FarmUpgrade upgrade : installedUpgrades(-1)) {
                for (var restriction : upgrade.restrictions()) effectsDependOnWorld |= !restriction.requiresBlockBelow().isEmpty();
            }
        }
        return effectCache = List.copyOf(list);
    }

    public boolean hasInfiniteInputs() {
        for (UpgradeEffect e : effects()) if (e.infiniteInputs()) return true;
        return false;
    }

    /** The player who last opened this farm; used to credit discoveries (secret recipes). */
    public void setLastUser(java.util.UUID user) {
        this.lastUser = user;
    }

    public boolean isPaused() {
        if (level == null) return false;
        for (UpgradeEffect e : effects()) if (e.pausesFarm(this)) return true;
        return false;
    }

    // ---- enchantments on the tool ----

    private int enchantLevel(ResourceKey<Enchantment> key) {
        if (level == null) return 0;
        Holder<Enchantment> holder = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(key);
        return EnchantmentHelper.getItemEnchantmentLevel(holder, getTool());
    }

    // ---- recipe & cycle ----

    private void contentsChanged() {
        recipeDirty = true;
        effectCache = null;
        setChanged();
        updateFloor();
    }

    /** Plant farms show the soil in the first input slot: farmland (+ water), soul sand, or grass by default. */
    private void updateFloor() {
        if (kind != FarmKind.PLANT || level == null || level.isClientSide) return;
        ItemStack soil = inputs.getStackInSlot(0);
        FarmFloor wanted = soil.is(Items.FARMLAND) ? FarmFloor.FARMLAND
                : soil.is(Items.SOUL_SAND) ? FarmFloor.SOUL_SAND : FarmFloor.GRASS;
        BlockState state = getBlockState();
        if (state.getValue(FarmBlock.FLOOR) != wanted) level.setBlock(worldPosition, state.setValue(FarmBlock.FLOOR, wanted), 3);
    }

    private List<ItemStack> inputSlots() {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < INPUTS; i++) list.add(inputs.getStackInSlot(i));
        return list;
    }

    private void refreshRecipe() {
        recipeDirty = false;
        if (!(level instanceof ServerLevel server)) return;
        FarmRecipeInput input = new FarmRecipeInput(inputSlots(), getTool());
        Optional<RecipeHolder<FarmRecipe>> found = server.getRecipeManager().getRecipeFor(ModRecipes.TYPES_BY_KIND.get(kind).get(), input, server);
        RecipeHolder<FarmRecipe> next = found.orElse(null);
        boolean changed = (recipe == null) != (next == null) || (recipe != null && !recipe.id().equals(next.id()));
        recipe = next;
        discoverSecrets(server);
        if (changed) {
            timer = 0;
            operatingTicks = 0;
        }
        sync();
    }

    /** Filling the inputs of a secret recipe (tool or not) reveals it to the player who last opened the farm. */
    private void discoverSecrets(ServerLevel server) {
        if (lastUser == null) return;
        var player = server.getServer().getPlayerList().getPlayer(lastUser);
        if (player == null) return;
        List<ItemStack> slots = inputSlots();
        for (RecipeHolder<FarmRecipe> holder : server.getRecipeManager().getAllRecipesFor(ModRecipes.TYPES_BY_KIND.get(kind).get())) {
            if (holder.value().secret() && holder.value().matchesInputs(slots)) {
                com.awesomeshot5051.easyfarms.trigger.SecretRecipes.discover(player, holder.id());
            }
        }
    }

    /** Ticks per cycle for the active recipe. */
    public double cycleTicks() {
        if (recipe == null) return 1;
        FarmRecipe r = recipe.value();
        double ticks = r.baseTicks() * Config.TIME_MULTIPLIER.get();
        if (r.toolless()) {
        } else {
            FarmTier tier = tier();
            if (tier != null) ticks = ticks / tier.speedDivisor() + breakTicks(r, tier);
        }
        for (UpgradeEffect e : effects()) ticks = e.modifyCycleTicks(this, r, ticks);
        return Math.max(1, ticks);
    }

    /**
     * The extra "work" time after generation. Pickaxe and shovel recipes use the resource curve; sword, axe and hoe
     * recipes use the mob/plant curve (the old farms differed). Efficiency halves it, or Sharpness for swords.
     */
    private double breakTicks(FarmRecipe r, FarmTier tier) {
        boolean digging = r.tool().map(t -> t.equals(ItemTags.PICKAXES) || t.equals(ItemTags.SHOVELS)).orElse(false);
        boolean sword = r.tool().map(t -> t.equals(ItemTags.SWORDS)).orElse(false);
        int unit = enchantLevel(sword ? Enchantments.SHARPNESS : Enchantments.EFFICIENCY) > 0 ? 10 : 20;
        return unit * (digging ? tier.breakMultiplier() : tier.weaponBreakMultiplier());
    }

    /**
     * How long the mob or plant is visible at the end of a cycle: the old mob and plant farms showed it for exactly
     * the work time, which depends on the tool tier. Toolless recipes use the recipe's visual_ticks.
     */
    public double visualWindow() {
        if (recipe == null) return 0;
        FarmRecipe r = recipe.value();
        if (r.toolless()) return r.visualTicks();
        FarmTier tier = tier();
        return tier == null ? r.visualTicks() : breakTicks(r, tier);
    }

    @Nullable
    private FarmTier tier() {
        return level == null ? null : FarmTier.find(level.registryAccess(), getTool());
    }

    /** Ticks of operation before the input items are used up: base days (config) x the tool tier's multiplier. */
    public long inputLifetimeTicks() {
        FarmTier tier = recipe != null && !recipe.value().toolless() ? tier() : null;
        long multiplier = tier == null ? 1 : Math.max(1, tier.inputLifetimeMultiplier());
        double extra = 1;
        for (UpgradeEffect e : effects()) extra *= e.inputLifetimeMultiplier();
        return Math.max(1, Math.round(Config.BASE_INPUT_LIFETIME_DAYS.get() * TICKS_PER_DAY * multiplier * extra));
    }

    public long operatingTicks() {
        return operatingTicks;
    }

    public void tick() {
        if (level == null) return;
        if (level.isClientSide) {
            clientTick();
            return;
        }
        if (catalystDirty) {
            catalystDirty = false;
            catalystSync = false;
            System.arraycopy(canonicalCatalystLayout(), 0, catalystOwner, 0, UPGRADE_SLOTS);
            reconcileCatalysts();
        }
        if (recipeDirty) refreshRecipe();
        if (recipe == null || isPaused() || !hasOutputRoom() || !environmentOk()) return;

        operatingTicks++;
        if (++timer >= cycleTicks()) {
            timer = 0;
            produce((ServerLevel) level);
        }
        if (!hasInfiniteInputs()) wear();
    }

    private void clientTick() {
        if (clientVisual != null && !isPaused()) {
            timer++;
            if (timer >= clientCycleTicks) timer = 0;
        }
    }

    private boolean hasOutputRoom() {
        for (int i = 0; i < outputs.getSlots(); i++) {
            ItemStack stack = outputs.getStackInSlot(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) return true;
        }
        return false;
    }

    private void produce(ServerLevel server) {
        FarmRecipe r = recipe.value();
        boolean fortune = enchantLevel(Enchantments.FORTUNE) > 0;
        boolean silk = enchantLevel(Enchantments.SILK_TOUCH) > 0;
        boolean smelter = false;
        for (UpgradeEffect e : effects()) smelter |= e.smelts(r);
        boolean looting = kind == FarmKind.MOB && enchantLevel(Enchantments.LOOTING) > 0;
        boolean fire = enchantLevel(Enchantments.FIRE_ASPECT) > 0;
        List<ItemStack> drops = new ArrayList<>();
        for (DropEntry entry : r.drops()) entry.roll(server, fortune, looting, silk, smelter, fire, drops);
        if (r.dyeable()) drops = recolorAll(drops);
        for (UpgradeEffect e : effects()) e.modifyDrops(this, server, r, drops);
        for (ItemStack drop : drops) {
            ItemStack rest = outputs.insertItem(drop, false);
            if (!rest.isEmpty()) Containers.dropItemStack(server, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, rest);
        }
        setChanged();
    }

    private List<ItemStack> recolorAll(List<ItemStack> drops) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : drops) {
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            Item recolored = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(DyeColors.recolor(id, color));
            out.add(recolored == Items.AIR ? stack : new ItemStack(recolored, stack.getCount()));
        }
        return out;
    }

    /**
     * Ageing. A recipe with a tool wears the tool out (its durability drains evenly so it is gone exactly when the
     * lifetime ends) and keeps its inputs. A toolless recipe uses up its inputs after the lifetime instead.
     */
    private void wear() {
        FarmRecipe r = recipe.value();
        long life = inputLifetimeTicks();
        if (r.toolless()) {
            if (operatingTicks >= life) {
                operatingTicks = 0;
                consumeInputs();
            }
            return;
        }
        ItemStack tool = getTool();
        if (tool.isEmpty()) return;
        if (tool.isDamageableItem()) {
            toolWear += (double) tool.getMaxDamage() / life;
            while (toolWear >= 1) {
                toolWear -= 1;
                int damage = tool.getDamageValue() + 1;
                if (damage >= tool.getMaxDamage()) {
                    breakTool();
                    return;
                }
                tool.setDamageValue(damage);
            }
            setChanged();
        } else if (operatingTicks >= life) {
            breakTool();
        }
    }

    private void breakTool() {
        toolSlot.setStackInSlot(0, ItemStack.EMPTY);
        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.ITEM_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 0.8F + level.random.nextFloat() * 0.4F);
    }

    /** True if this farm's recipe wears out the tool (as opposed to a toolless recipe, which uses up its inputs). */
    public boolean usesTool() {
        return recipe != null && !recipe.value().toolless();
    }

    /** Ticks until the tool (or, for a toolless recipe, the inputs) is used up. */
    public long lifetimeLeftTicks() {
        long life = inputLifetimeTicks();
        if (usesTool()) {
            ItemStack tool = getTool();
            if (tool.isDamageableItem() && tool.getMaxDamage() > 0) {
                double left = tool.getMaxDamage() - tool.getDamageValue() - toolWear;
                return Math.max(0, (long) (left * life / tool.getMaxDamage()));
            }
        }
        return Math.max(0, life - operatingTicks);
    }

    /** Uses up the recipe's input counts. If the inputs no longer satisfy the recipe, it stops on the next refresh. */
    private void consumeInputs() {
        FarmRecipe r = recipe.value();
        int[] assigned = r.assignInputs(inputSlots());
        if (assigned != null) {
            for (int i = 0; i < assigned.length; i++) inputs.extractItem(assigned[i], r.inputs().get(i).count(), false);
        }
        recipeDirty = true;
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ---- breaking ----

    public void setKeepContents(boolean keep) {
        this.keepContents = keep;
    }

    public void dropContents() {
        if (level == null || keepContents) return;
        for (ItemStackHandler handler : List.of(inputs, toolSlot, outputs, upgrades, press, catalysts)) {
            for (int i = 0; i < handler.getSlots(); i++) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), handler.getStackInSlot(i));
            }
        }
    }

    // ---- persistence & client sync ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("Inputs", inputs.serializeNBT(provider));
        tag.put("Tool", toolSlot.serializeNBT(provider));
        tag.put("Outputs", outputs.serializeNBT(provider));
        tag.put("Upgrades", upgrades.serializeNBT(provider));
        tag.put("Press", press.serializeNBT(provider));
        tag.put("Catalysts", catalysts.serializeNBT(provider));
        tag.putIntArray("CatalystOwners", catalystOwner);
        tag.putDouble("ToolWear", toolWear);
        tag.putLong("Timer", timer);
        tag.putLong("OperatingTicks", operatingTicks);
        tag.putString("Color", color.getName());
        // What the client renderer needs (harmless extra data when saving to disk).
        if (recipe != null) {
            FarmRecipe r = recipe.value();
            FarmRecipe.Visual visual = resolvedVisual(r);
            tag.putString("VisualType", visual.type());
            tag.putString("VisualTarget", visual.target().toString());
            tag.putDouble("CycleTicks", cycleTicks());
            tag.putInt("VisualTicks", (int) Math.round(visualWindow()));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        effectCache = null;
        inputs.deserializeNBT(provider, tag.getCompound("Inputs"));
        toolSlot.deserializeNBT(provider, tag.getCompound("Tool"));
        outputs.deserializeNBT(provider, tag.getCompound("Outputs"));
        upgrades.deserializeNBT(provider, tag.getCompound("Upgrades"));
        if (upgrades.getSlots() < UPGRADE_SLOTS) {
            // A farm saved with fewer upgrade slots: keep its cards and add empty slots.
            List<ItemStack> kept = new ArrayList<>();
            for (int i = 0; i < upgrades.getSlots(); i++) kept.add(upgrades.getStackInSlot(i));
            upgrades.setSize(UPGRADE_SLOTS);
            for (int i = 0; i < kept.size(); i++) upgrades.setStackInSlot(i, kept.get(i));
        }
        press.deserializeNBT(provider, tag.getCompound("Press"));
        catalysts.deserializeNBT(provider, tag.getCompound("Catalysts"));
        if (catalysts.getSlots() < UPGRADE_SLOTS) catalysts.setSize(UPGRADE_SLOTS);
        int[] savedOwners = tag.getIntArray("CatalystOwners");
        if (savedOwners.length == UPGRADE_SLOTS) {
            System.arraycopy(savedOwners, 0, catalystOwner, 0, UPGRADE_SLOTS);
        } else {
            java.util.Arrays.fill(catalystOwner, -1);
            catalystSync = true;
            catalystDirty = true;
        }
        toolWear = tag.getDouble("ToolWear");
        timer = tag.getLong("Timer");
        operatingTicks = tag.getLong("OperatingTicks");
        color = DyeColor.byName(tag.getString("Color"), DyeColor.WHITE);
        if (tag.contains("VisualType")) {
            clientVisual = new FarmRecipe.Visual(tag.getString("VisualType"), net.minecraft.resources.ResourceLocation.parse(tag.getString("VisualTarget")));
            clientCycleTicks = Math.max(1, tag.getDouble("CycleTicks"));
            clientVisualTicks = tag.getInt("VisualTicks");
        } else {
            clientVisual = null;
        }
        recipeDirty = true;
    }

    // ---- Silk Touch pickup: contents travel in the item ----

    private List<ItemStackHandler> allHandlers() {
        return List.of(inputs, toolSlot, outputs, upgrades, press, catalysts);
    }

    private FarmContents snapshot() {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStackHandler handler : allHandlers()) {
            for (int i = 0; i < handler.getSlots(); i++) stacks.add(handler.getStackInSlot(i).copy());
        }
        return new FarmContents(stacks, operatingTicks, color.getName());
    }

    private void restore(FarmContents contents) {
        int index = 0;
        for (ItemStackHandler handler : allHandlers()) {
            for (int i = 0; i < handler.getSlots(); i++) {
                handler.setStackInSlot(i, index < contents.stacks().size() ? contents.stacks().get(index) : ItemStack.EMPTY);
                index++;
            }
        }
        operatingTicks = contents.operatingTicks();
        color = DyeColor.byName(contents.color(), DyeColor.WHITE);
        recipeDirty = true;
        // The stacks were saved in the canonical layout of the saved cards: adopt it instead of moving them.
        catalystSync = true;
        catalystDirty = true;
    }

    private static int[] newCatalystOwners() {
        int[] owners = new int[UPGRADE_SLOTS];
        java.util.Arrays.fill(owners, -1);
        return owners;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        FarmContents contents = snapshot();
        if (contents.stackCount() > 0) builder.set(ModDataComponents.FARM_CONTENTS.get(), contents);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        FarmContents contents = input.get(ModDataComponents.FARM_CONTENTS.get());
        if (contents != null) restore(contents);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        for (String key : new String[]{"Inputs", "Tool", "Outputs", "Upgrades", "Catalysts", "CatalystOwners", "OperatingTicks", "Color"}) tag.remove(key);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
