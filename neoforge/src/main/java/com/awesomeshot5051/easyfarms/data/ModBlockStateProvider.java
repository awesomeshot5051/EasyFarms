package com.awesomeshot5051.easyfarms.data;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.farm.FarmBlock;
import com.awesomeshot5051.easyfarms.farm.FarmFloor;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.EnumMap;
import java.util.Map;

/** The three farms share the glass-case model (assets/easy_farms/models/block/farm.json): one variant per facing and floor. */
public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, EasyFarms.MODID, helper);
    }

    @Override
    protected void registerStatesAndModels() {
        // One model per floor: the shared glass case with front/back floor textures filled in.
        Map<FarmFloor, ModelFile> models = new EnumMap<>(FarmFloor.class);
        for (FarmFloor floor : FarmFloor.values()) {
            models.put(floor, models().withExistingParent("farm_" + floor.getSerializedName(), modLoc("block/farm"))
                    .texture("front", ResourceLocation.parse(floor.front))
                    .texture("back", ResourceLocation.parse(floor.back)));
        }
        EasyFarms.FARM_BLOCKS.values().forEach(block -> getVariantBuilder(block.get()).forAllStates(state -> {
            int y = switch (state.getValue(FarmBlock.FACING)) {
                case SOUTH -> 180;
                case WEST -> 270;
                case EAST -> 90;
                default -> 0;
            };
            return ConfiguredModel.builder().modelFile(models.get(state.getValue(FarmBlock.FLOOR))).rotationY(y).build();
        }));
    }
}
