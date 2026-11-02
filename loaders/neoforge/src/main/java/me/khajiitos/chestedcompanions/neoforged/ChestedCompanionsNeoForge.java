package me.khajiitos.chestedcompanions.neoforged;

import me.khajiitos.chestedcompanions.common.ChestedCompanions;
import me.khajiitos.chestedcompanions.common.config.CCConfig;
import net.neoforged.fml.loading.FMLPaths;
import me.khajiitos.chestedcompanions.neoforged.client.ChestedCompanionsNeoForgeClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(ChestedCompanions.MOD_ID)
public class ChestedCompanionsNeoForge {
    public ChestedCompanionsNeoForge(ModContainer modContainer) {
        NeoPetChestData.register(java.util.Objects.requireNonNull(modContainer.getEventBus()));
        CCConfig.init(FMLPaths.CONFIGDIR.get());
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            ChestedCompanionsNeoForgeClient.init(modContainer);
        }
    }
}
