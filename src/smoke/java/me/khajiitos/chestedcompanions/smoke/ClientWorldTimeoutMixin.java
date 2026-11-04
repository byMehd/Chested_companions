package me.khajiitos.chestedcompanions.smoke;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Software-rendered CI clients need longer to load worlds with animated model packs. Test-only, absent from release JARs. */
@Pseudo
@Mixin(targets = "net.fabricmc.fabric.impl.client.gametest.util.ClientGameTestImpl", remap = false)
public class ClientWorldTimeoutMixin {
    @ModifyConstant(method = "waitForWorldLoad", constant = @Constant(intValue = 1200), remap = false)
    private static int revived$allowWorldLoading(int ticks) {
        return ticks * 3;
    }
}
