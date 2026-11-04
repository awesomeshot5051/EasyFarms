package com.awesomeshot5051.easyfarms.registry;

import com.awesomeshot5051.easyfarms.tier.FarmUpgrade;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * The blank datapack card. Its name and glint come from the farm_upgrade whose {@code card} id matches the stack's
 * {@code easy_farms:card} component, so a recipe only has to set that one component.
 */
public class UpgradeCardItem extends Item {
    /** Set by the client to give the card access to the synced farm_upgrade registry; null on a dedicated server. */
    public static Supplier<RegistryAccess> registryAccess = () -> null;

    public UpgradeCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        RegistryAccess access = registryAccess.get();
        if (access != null) {
            FarmUpgrade upgrade = FarmUpgrade.find(access, stack);
            if (upgrade != null && upgrade.name().isPresent()) return upgrade.name().get();
        }
        return super.getName(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModDataComponents.CARD.get()) || super.isFoil(stack);
    }
}
