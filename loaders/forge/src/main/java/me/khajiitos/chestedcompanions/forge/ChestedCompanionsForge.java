package me.khajiitos.chestedcompanions.forge;
import me.khajiitos.chestedcompanions.common.ChestedCompanions;
import me.khajiitos.chestedcompanions.common.config.CCConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
@Mod(ChestedCompanions.MOD_ID)
public class ChestedCompanionsForge {
    public ChestedCompanionsForge(FMLJavaModLoadingContext context) {
        CCConfig.init(FMLPaths.CONFIGDIR.get());
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            me.khajiitos.chestedcompanions.forge.client.ChestedCompanionsForgeClient.init(context);
        }
    }
}
