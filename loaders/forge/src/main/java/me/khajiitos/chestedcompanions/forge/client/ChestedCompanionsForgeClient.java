package me.khajiitos.chestedcompanions.forge.client;

import me.khajiitos.chestedcompanions.common.client.config.ConfigScreens;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ChestedCompanionsForgeClient {
    public static void init(FMLJavaModLoadingContext context) {
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> ConfigScreens.create(parent)));
    }
}
