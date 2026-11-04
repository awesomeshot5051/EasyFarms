package com.awesomeshot5051.easyfarms.data;

import com.awesomeshot5051.easyfarms.EasyFarms;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import com.awesomeshot5051.easyfarms.registry.ModDataComponents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Each farm drops itself. With Silk Touch the contents stay inside the item; otherwise the block entity spills them. */
public class ModBlockLoot extends BlockLootSubProvider {
    public ModBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        for (Block block : getKnownBlocks()) {
            // Silk Touch: the farm item with everything inside it (copied from the block entity).
            LootPoolEntryContainer.Builder<?> keepContents = LootItem.lootTableItem(block).when(hasSilkTouch())
                    .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                            .include(ModDataComponents.FARM_CONTENTS.get()));
            // Anything else: just the farm; the block entity spills its contents separately.
            LootPoolEntryContainer.Builder<?> plain = LootItem.lootTableItem(block);
            add(block, LootTable.lootTable().withPool(applyExplosionCondition(block, LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(AlternativesEntry.alternatives(keepContents, plain)))));
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        List<Block> blocks = new ArrayList<>();
        for (var entry : EasyFarms.FARM_BLOCKS.values()) blocks.add(entry.get());
        return blocks;
    }
}
