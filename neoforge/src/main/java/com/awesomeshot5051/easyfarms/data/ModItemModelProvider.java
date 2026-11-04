package com.awesomeshot5051.easyfarms.data;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, EasyFarms.MODID, helper);
    }

    @Override
    protected void registerModels() {
        ModItems.UPGRADES.forEach(card -> basicItem(card.get()));
        // The blank datapack card: one shared texture.
        withExistingParent("upgrade_card", "item/generated").texture("layer0", modLoc("item/upgrade_parent"));
        for (var kind : EasyFarms.FARM_BLOCKS.keySet()) {
            // builtin/entity hands rendering to FarmItemRenderer (glass case + tool/mob/plant); display values are the old farms'.
            ItemModelBuilder model = getBuilder(kind.id()).parent(new ModelFile.UncheckedModelFile("builtin/entity"));
            model.transforms()
                    .transform(ItemDisplayContext.GUI).rotation(30, 45, 0).translation(0, 0, 0).scale(0.625F).end()
                    .transform(ItemDisplayContext.GROUND).rotation(0, 0, 0).translation(0, 3, 0).scale(0.25F).end()
                    .transform(ItemDisplayContext.HEAD).rotation(0, 180, 0).translation(0, 0, 0).scale(1F).end()
                    .transform(ItemDisplayContext.FIXED).rotation(0, 180, 0).translation(0, 0, 0).scale(0.5F).end()
                    .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(75, 315, 0).translation(0, 2.5F, 0).scale(0.375F).end()
                    .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 315, 0).translation(0, 0, 0).scale(0.4F).end()
                    .end();
        }
    }
}
