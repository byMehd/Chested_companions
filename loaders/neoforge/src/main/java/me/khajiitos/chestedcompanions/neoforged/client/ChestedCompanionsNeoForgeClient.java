package me.khajiitos.chestedcompanions.neoforged.client;

import me.khajiitos.chestedcompanions.common.client.config.ConfigScreens;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class ChestedCompanionsNeoForgeClient {
    public static void init(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> ConfigScreens.create(parent));
    }
}
