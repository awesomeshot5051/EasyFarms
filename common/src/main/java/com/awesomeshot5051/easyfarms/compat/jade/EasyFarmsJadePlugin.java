package com.awesomeshot5051.easyfarms.compat.jade;

import com.awesomeshot5051.easyfarms.farm.FarmBlock;
import com.awesomeshot5051.easyfarms.farm.FarmBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/** Jade: shows progress, tool and remaining input lifetime when looking at a farm. */
@WailaPlugin
public class EasyFarmsJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(FarmJadeProvider.INSTANCE, FarmBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(FarmJadeProvider.INSTANCE, FarmBlock.class);
    }
}
