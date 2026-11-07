package com.awesomeshot5051.easyfarms.gui;

import com.awesomeshot5051.easyfarms.registry.CardFrequency;
import com.awesomeshot5051.easyfarms.registry.ModDataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * The screen of a DimStorage Auto Importer card, opened by using the card. It has no item slots: buttons set the frequency channel
 * and whether the frequency is public or private (owned by whoever sets it, like a Dimensional Chest's owner button). The card in
 * the player's hand is edited on the server; the client only sees the values through the data slots.
 */
public class CardFrequencyMenu extends AbstractContainerMenu {
    /** Buttons 0..5 add these to the channel; 6 toggles private; 7 resets. */
    public static final int[] STEPS = {1, -1, 10, -10, 100, -100};
    public static final int TOGGLE_PRIVATE = 6, RESET = 7;

    private final InteractionHand hand;
    private final ContainerData data;

    public CardFrequencyMenu(int id, Inventory inventory, int handOrdinal) {
        super(ModMenus.CARD_FREQUENCY.get(), id);
        this.hand = InteractionHand.values()[Math.floorMod(handOrdinal, InteractionHand.values().length)];
        Player player = inventory.player;
        this.data = player instanceof net.minecraft.server.level.ServerPlayer ? new ContainerData() {
            @Override
            public int get(int index) {
                CardFrequency f = frequency(player);
                return switch (index) {
                    case 0 -> f.channel() & 0xFFFF;
                    case 1 -> f.channel() >> 16;
                    default -> f.isPrivate() ? 1 : 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 3;
            }
        } : new SimpleContainerData(3);
        addDataSlots(data);
    }

    private CardFrequency frequency(Player player) {
        ItemStack card = player.getItemInHand(hand);
        CardFrequency f = card.get(ModDataComponents.FREQUENCY.get());
        return f == null ? CardFrequency.PUBLIC_ZERO : f;
    }

    public int channel() {
        return (data.get(0) & 0xFFFF) | (data.get(1) << 16);
    }

    public boolean isPrivate() {
        return data.get(2) == 1;
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        ItemStack card = player.getItemInHand(hand);
        if (card.isEmpty() || !card.has(ModDataComponents.CARD.get())) return false;
        CardFrequency f = frequency(player);
        if (button >= 0 && button < STEPS.length) {
            f = f.withChannel(f.channel() + STEPS[button]);
        } else if (button == TOGGLE_PRIVATE) {
            f = f.isPrivate() ? f.withOwner(Optional.empty(), "") : f.withOwner(Optional.of(player.getUUID()), player.getGameProfile().getName());
        } else if (button == RESET) {
            f = CardFrequency.PUBLIC_ZERO;
        } else {
            return false;
        }
        card.set(ModDataComponents.FREQUENCY.get(), f);
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).has(ModDataComponents.CARD.get());
    }
}
