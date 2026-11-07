package com.awesomeshot5051.easyfarms;

import com.awesomeshot5051.easyfarms.farm.FarmBlock;
import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import com.awesomeshot5051.easyfarms.farm.FarmKind;
import com.awesomeshot5051.easyfarms.gui.ModMenus;
import com.awesomeshot5051.easyfarms.platform.Services;
import com.awesomeshot5051.easyfarms.recipe.ModRecipes;
import com.awesomeshot5051.easyfarms.registry.ModDataComponents;
import com.awesomeshot5051.easyfarms.registry.ModItems;
import com.awesomeshot5051.easyfarms.tier.FarmTier;
import com.awesomeshot5051.easyfarms.tier.FarmUpgrade;
import com.awesomeshot5051.easyfarms.trigger.ModTriggers;
import com.awesomeshot5051.easyfarms.upgrade.UpgradeEffects;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/** Loader-independent mod setup. Each loader's entrypoint calls {@link #init()}. */
public final class EasyFarms {
    public static final String MODID = "easy_farms";
    public static final Logger LOGGER = LogUtils.getLogger();

    private EasyFarms() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    /** The three farm blocks, one per kind. */
    public static final Map<FarmKind, Supplier<FarmBlock>> FARM_BLOCKS = new EnumMap<>(FarmKind.class);

    static {
        for (FarmKind kind : FarmKind.values()) {
            Supplier<FarmBlock> block = Services.PLATFORM.register(Registries.BLOCK, kind.id(), () -> new FarmBlock(kind));
            Services.PLATFORM.register(Registries.ITEM, kind.id(), () -> new BlockItem(block.get(), new Item.Properties()));
            FARM_BLOCKS.put(kind, block);
        }
    }

    public static final Supplier<BlockEntityType<FarmBlockEntity>> FARM_BE = Services.PLATFORM.register(Registries.BLOCK_ENTITY_TYPE, "farm",
            () -> Services.PLATFORM.createBlockEntityType(FarmBlockEntity::new,
                    FARM_BLOCKS.values().stream().map(Supplier::get).toArray(Block[]::new)));

    public static final Supplier<CreativeModeTab> FARMS_TAB = Services.PLATFORM.register(Registries.CREATIVE_MODE_TAB, "farms", () ->
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0).title(Component.translatable("itemGroup." + MODID))
                    .icon(() -> new ItemStack(FARM_BLOCKS.get(FarmKind.RESOURCE).get()))
                    .displayItems((params, out) -> {
                        FARM_BLOCKS.values().forEach(b -> out.accept(b.get()));
                        ModItems.UPGRADES.forEach(u -> out.accept(u.get()));
                        out.accept(ModItems.MEMORY_CARD.get());
                        // Datapack cards (generic upgrade_card items): one per farm_upgrade that names a card id.
                        params.holders().lookup(FarmUpgrade.REGISTRY_KEY).ifPresent(lookup ->
                                lookup.listElements().map(h -> h.value())
                                        .filter(u -> u.card().isPresent())
                                        .sorted(Comparator.comparing(u -> u.card().get().toString()))
                                        .forEach(u -> out.accept(u.stack())));
                    })
                    .build());

    /** Registers everything. Called once from the loader's entrypoint, before registries open. */
    public static void init() {
        Config.load();
        // Touching these classes registers their contents.
        ModItems.init();
        ModDataComponents.init();
        UpgradeEffects.init();
        com.awesomeshot5051.easyfarms.crafting.CraftingGridTypes.init();
        ModTriggers.init();
        ModRecipes.init();
        ModMenus.init();

        // Synced to clients: the farm GUI and recipes look tiers up on both sides.
        Services.PLATFORM.registerClientboundPayload(com.awesomeshot5051.easyfarms.net.LootPreviewPayload.TYPE, com.awesomeshot5051.easyfarms.net.LootPreviewPayload.CODEC,
                com.awesomeshot5051.easyfarms.client.LootPreviewClient::receive);
        Services.PLATFORM.registerSyncedDatapackRegistry(FarmTier.REGISTRY_KEY, FarmTier.CODEC);
        Services.PLATFORM.registerSyncedDatapackRegistry(FarmUpgrade.REGISTRY_KEY, FarmUpgrade.CODEC);
        Services.PLATFORM.registerSyncedDatapackRegistry(com.awesomeshot5051.easyfarms.crafting.CraftingGrid.REGISTRY_KEY, com.awesomeshot5051.easyfarms.crafting.CraftingGrid.CODEC);
        Services.PLATFORM.registerSyncedDatapackRegistry(com.awesomeshot5051.easyfarms.tier.FluidContainer.REGISTRY_KEY, com.awesomeshot5051.easyfarms.tier.FluidContainer.CODEC);
    }
}
