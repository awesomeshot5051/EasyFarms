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

    /** An Auto Importer card is linked to a storage network by using it on a block of that network (a cable, an interface...). */
    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        String network = networkOf(context.getLevel().registryAccess(), stack);
        if (network == null) return super.useOn(context);
        if (network.equals("dimstorage")) {
            // DimStorage has frequencies instead of a network: using the card (anywhere) opens its frequency screen.
            return use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
        }
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof com.awesomeshot5051.easyfarms.farm.FarmBlockEntity) {
            return super.useOn(context);
        }
        // Linking: use the card on a block of the network. Sneaking skips the block's own use, so sneak-use links to blocks with a
        // screen too (a Grid, an Interface).
        if (!context.getLevel().isClientSide && context.getLevel() instanceof net.minecraft.server.level.ServerLevel server) {
            var importer = com.awesomeshot5051.easyfarms.farm.network.NetworkImporters.get(network);
            var player = context.getPlayer();
            if (importer != null && !importer.canLink(server, context.getClickedPos())) {
                if (player != null) player.displayClientMessage(Component.translatable("item.easy_farms.upgrade_card.not_network").withStyle(net.minecraft.ChatFormatting.RED), true);
                return net.minecraft.world.InteractionResult.FAIL;
            }
            stack.set(ModDataComponents.LINK.get(), net.minecraft.core.GlobalPos.of(context.getLevel().dimension(), context.getClickedPos()));
            if (player != null) {
                player.displayClientMessage(Component.translatable("item.easy_farms.upgrade_card.linked",
                        context.getClickedPos().getX(), context.getClickedPos().getY(), context.getClickedPos().getZ()), true);
            }
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    /** The storage network an Auto Importer card imports into ("ae2", ...), or null for any other card. */
    @org.jetbrains.annotations.Nullable
    public static String networkOf(RegistryAccess access, ItemStack stack) {
        FarmUpgrade upgrade = FarmUpgrade.find(access, stack);
        if (upgrade == null) return null;
        for (var effect : upgrade.effects()) if (effect.importsToNetwork()) return effect.networkId();
        return null;
    }

    /** Using a DimStorage Auto Importer card in the air opens its frequency screen. */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if ("dimstorage".equals(networkOf(level.registryAccess(), stack))) {
            if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer server) {
                com.awesomeshot5051.easyfarms.platform.Services.PLATFORM.openMenu(server, getName(stack), hand.ordinal(),
                        net.minecraft.network.codec.ByteBufCodecs.VAR_INT.cast(),
                        (id, inv, p) -> new com.awesomeshot5051.easyfarms.gui.CardFrequencyMenu(id, inv, hand.ordinal()));
            }
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        // Sneak-using an importer card in the air clears its link.
        if (player.isShiftKeyDown() && networkOf(level.registryAccess(), stack) != null && stack.has(ModDataComponents.LINK.get())) {
            if (!level.isClientSide) {
                stack.remove(ModDataComponents.LINK.get());
                player.displayClientMessage(Component.translatable("item.easy_farms.upgrade_card.unlinked"), true);
            }
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        var frequency = stack.get(ModDataComponents.FREQUENCY.get());
        if (frequency != null) {
            tooltip.add(Component.translatable(frequency.isPrivate() ? "item.easy_farms.upgrade_card.frequency_private" : "item.easy_farms.upgrade_card.frequency_public",
                    frequency.channel(), frequency.ownerName()).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        net.minecraft.core.GlobalPos link = stack.get(ModDataComponents.LINK.get());
        if (link != null) {
            tooltip.add(Component.translatable("item.easy_farms.upgrade_card.link_tooltip", link.pos().getX(), link.pos().getY(), link.pos().getZ(),
                    link.dimension().location().toString()).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModDataComponents.CARD.get()) || super.isFoil(stack);
    }
}
