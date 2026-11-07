package com.awesomeshot5051.easyfarms.gui;

import com.awesomeshot5051.easyfarms.farm.FarmBlock;
import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import com.awesomeshot5051.easyfarms.farm.FarmKind;
import com.awesomeshot5051.easyfarms.registry.ModItems;
import com.awesomeshot5051.easyfarms.tier.FarmTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import com.awesomeshot5051.easyfarms.util.HandlerSlot;
import com.awesomeshot5051.easyfarms.util.ItemStackHandler;

/** Main farm GUI: three input slots, the tool slot, four take-only output slots, and a button to the Upgrades screen. */
public class FarmMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 52, INPUT_Y = 20, OUTPUT_Y = 51;
    private static final int TOOL_SLOT = FarmBlockEntity.INPUTS;
    private final int firstPlayer;
    private final FarmKind kind;

    private final BlockPos pos;
    private final FarmBlockEntity farm;
    private final ContainerData data;
    private final net.minecraft.world.level.Level level;

    public FarmMenu(int id, Inventory inventory, BlockPos pos, FarmBlockEntity farm) {
        super(ModMenus.FARM.get(), id);
        this.pos = pos;
        this.farm = farm;
        this.level = inventory.player.level();
        // The client only gets the position: the kind comes from the block there.
        this.kind = farm != null ? farm.kind()
                : inventory.player.level().getBlockState(pos).getBlock() instanceof FarmBlock block ? block.kind() : FarmKind.RESOURCE;
        if (farm != null && inventory.player instanceof net.minecraft.server.level.ServerPlayer) farm.setLastUser(inventory.player.getUUID());
        ItemStackHandler inputs = farm != null ? farm.inputs() : new ItemStackHandler(FarmBlockEntity.INPUTS);
        ItemStackHandler tool = farm != null ? farm.toolSlot() : new ItemStackHandler(1);
        ItemStackHandler outputs = farm != null ? farm.outputs() : new ItemStackHandler(FarmBlockEntity.OUTPUTS);

        for (int i = 0; i < FarmBlockEntity.INPUTS; i++) addSlot(new HandlerSlot(inputs, i, INPUT_X + i * 18, INPUT_Y));
        addSlot(new HandlerSlot(tool, 0, INPUT_X + FarmBlockEntity.INPUTS * 18, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // Only things that belong to a tool tier: a tool no tier knows about could never run a recipe.
                return FarmTier.find(inventory.player.level().registryAccess(), stack) != null;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        // A Liquid Farm has a tank bar where the output slots are.
        int outputSlots = kind == FarmKind.LIQUID ? 0 : FarmBlockEntity.OUTPUTS;
        for (int i = 0; i < outputSlots; i++) {
            addSlot(new HandlerSlot(outputs, i, INPUT_X + i * 18, OUTPUT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        this.firstPlayer = FarmBlockEntity.INPUTS + 1 + outputSlots;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 82 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 140));

        this.data = farm != null ? new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> (int) farm.getTimer();
                    case 1 -> (int) Math.ceil(farm.cycleTicks());
                    case 2 -> (int) (farm.lifetimeLeftTicks() / 24000L);
                    case 3 -> farm.hasInfiniteInputs() ? 1 : 0;
                    case 4 -> farm.isRunning() ? 1 : 0;
                    case 6 -> farm.usesTool() ? 1 : 0;
                    case 7 -> farm.catalystMask();
                    case 8 -> (int) (farm.energyStored() & 0xFFFF);
                    case 9 -> (int) ((farm.energyStored() >> 16) & 0xFFFF);
                    case 10 -> (int) (farm.energyCapacity() & 0xFFFF);
                    case 11 -> (int) ((farm.energyCapacity() >> 16) & 0xFFFF);
                    case 12 -> Math.min(32767, farm.fluidAmount());
                    case 13 -> Math.min(32767, farm.fluidCapacity());
                    case 14 -> farm.fluidId() == null ? -1 : net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(
                            net.minecraft.core.registries.BuiltInRegistries.FLUID.get(farm.fluidId()));
                    case 15 -> (farm.hasFilterCard() ? 1 : 0) | (farm.hasExportCard() ? 2 : 0) | (farm.showsXp() ? 4 : 0) | (farm.hasCraftingCard() ? 8 : 0);
                    case 16 -> farm.packedSideModes();
                    case 17 -> Math.min(32767, farm.xp());
                    default -> farm.environmentOk() ? 1 : 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 18;
            }
        } : new SimpleContainerData(18);
        addDataSlots(data);
    }

    /**
     * The menu slots a recipe's ingredients go into, in the order of the recipe viewers' input list: the recipe's inputs
     * (slots 0-2), then the tool slot when the recipe uses a tool. Used by the JEI / EMI "move items" button.
     */
    public java.util.List<Slot> recipeSlots(com.awesomeshot5051.easyfarms.recipe.FarmRecipe recipe) {
        java.util.List<Slot> list = new java.util.ArrayList<>();
        for (int i = 0; i < recipe.inputs().size(); i++) list.add(slots.get(i));
        if (recipe.tool().isPresent()) list.add(slots.get(TOOL_SLOT));
        return list;
    }

    /** The player's inventory and hotbar slots (what a recipe viewer takes items from). */
    public java.util.List<Slot> inventorySlots() {
        return new java.util.ArrayList<>(slots.subList(firstPlayer, slots.size()));
    }

    public FarmKind kind() {
        return kind;
    }

    /** Stored energy (ME). ContainerData sends 16 bits at a time. */
    public long energy() {
        return (data.get(8) & 0xFFFFL) | ((data.get(9) & 0xFFFFL) << 16);
    }

    public long energyCapacity() {
        return (data.get(10) & 0xFFFFL) | ((data.get(11) & 0xFFFFL) << 16);
    }

    /** True while a Powered card works in the farm (the energy bar is shown). */
    public boolean powered() {
        return energyCapacity() > 0;
    }

    public int fluidAmount() {
        return data.get(12);
    }

    public int fluidCapacity() {
        return Math.max(1, data.get(13));
    }

    /** The fluid in the tank (a game fluid or a virtual one such as honey), or null when empty. Read from the farm's client copy. */
    public net.minecraft.resources.ResourceLocation fluidKey() {
        return level != null && level.getBlockEntity(pos) instanceof FarmBlockEntity be ? be.fluidId() : null;
    }

    /** The fluid in the tank, or null when empty. */
    public net.minecraft.world.level.material.Fluid fluid() {
        int id = data.get(14);
        return id < 0 ? null : net.minecraft.core.registries.BuiltInRegistries.FLUID.byId(id);
    }

    public int progress() {
        return data.get(0);
    }

    public int cycle() {
        return data.get(1);
    }

    public int daysLeft() {
        return data.get(2);
    }

    public boolean usesTool() {
        return data.get(6) == 1;
    }

    public boolean infinite() {
        return data.get(3) == 1;
    }

    public boolean environmentOk() {
        return data.get(5) == 1;
    }

    public boolean running() {
        return data.get(4) == 1;
    }

    /** True while a Filter card is installed (its button is shown). */
    public boolean hasFilterCard() {
        return (data.get(15) & 1) != 0;
    }

    /** True while an Auto-Export card is installed (the side panel is available). */
    public boolean hasExportCard() {
        return (data.get(15) & 2) != 0;
    }

    /** True while a Crafting card is installed (its button is shown). */
    public boolean hasCraftingCard() {
        return (data.get(15) & 8) != 0;
    }

    /** True while an Experience card is installed (the collect button is shown). */
    public boolean hasExperienceCard() {
        return (data.get(15) & 4) != 0;
    }

    /** What a relative face does (0 front, 1 back, 2 left, 3 right, 4 top, 5 bottom): both / input / output / off. */
    public int sideMode(int face) {
        return (data.get(16) >> (face * 2)) & 3;
    }

    /** Experience waiting in the farm (shown capped at 32767). */
    public int xp() {
        return data.get(17);
    }

    /** True while an installed card needs a catalyst: the Catalysts button is shown. */
    public boolean hasCatalystCards() {
        return data.get(7) != 0;
    }

    /** Button 0: open the Upgrades screen. Button 1: open the Catalysts screen (only if a card needs one). */
    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button == 0 && farm != null && player instanceof ServerPlayer server) {
            MenuOpener.openUpgrades(server, pos, farm, Component.translatable("gui.easy_farms.upgrades"));
            return true;
        }
        if (button == 1 && farm != null && farm.catalystMask() != 0 && player instanceof ServerPlayer server) {
            MenuOpener.openCatalysts(server, pos, farm, Component.translatable("gui.easy_farms.catalysts"));
            return true;
        }
        if (button == 2 && farm != null && farm.hasFilterCard() && player instanceof ServerPlayer server) {
            MenuOpener.openFilter(server, pos, farm, Component.translatable("gui.easy_farms.filter"));
            return true;
        }
        if (button == 3 && farm != null && farm.showsXp()) {
            farm.collectXp(player);
            return true;
        }
        if (button == 10 && farm != null && farm.hasCraftingCard() && player instanceof ServerPlayer server) {
            MenuOpener.openCrafting(server, pos, farm, Component.translatable("gui.easy_farms.crafting"));
            return true;
        }
        if (button >= 4 && button < 10 && farm != null && farm.hasExportCard()) {
            farm.cycleSideMode(button - 4);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < firstPlayer) {
            if (!moveItemStackTo(stack, firstPlayer, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // Try the tool slot first (it only accepts tier tools), then the two inputs.
            if (!moveItemStackTo(stack, TOOL_SLOT, TOOL_SLOT + 1, false) && !moveItemStackTo(stack, 0, FarmBlockEntity.INPUTS, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(pos) instanceof FarmBlockEntity && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}
