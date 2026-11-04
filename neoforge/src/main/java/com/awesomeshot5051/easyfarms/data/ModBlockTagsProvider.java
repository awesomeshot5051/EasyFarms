package com.awesomeshot5051.easyfarms.data;

import com.awesomeshot5051.easyfarms.EasyFarms;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagsProvider extends BlockTagsProvider {
    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper) {
        super(output, lookup, EasyFarms.MODID, helper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        EasyFarms.FARM_BLOCKS.values().forEach(b -> tag(BlockTags.MINEABLE_WITH_PICKAXE).add(b.get()));
    }
}
