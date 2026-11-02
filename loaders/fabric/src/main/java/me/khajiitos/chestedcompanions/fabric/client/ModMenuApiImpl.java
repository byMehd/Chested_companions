package me.khajiitos.chestedcompanions.fabric.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.khajiitos.chestedcompanions.common.client.config.ConfigScreens;

public class ModMenuApiImpl implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreens::create;
    }
}
