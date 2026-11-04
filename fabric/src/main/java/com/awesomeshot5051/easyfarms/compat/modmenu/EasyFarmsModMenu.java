package com.awesomeshot5051.easyfarms.compat.modmenu;

import com.awesomeshot5051.easyfarms.client.ConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Shows the shared settings screen from Mod Menu's config button. */
public class EasyFarmsModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }
}
