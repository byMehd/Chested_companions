package me.khajiitos.chestedcompanions.smoke;

import me.khajiitos.chestedcompanions.smokescenarios.ServerSmokeScenario;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

/** Test-only: when started with {@code -Drevived.smoke.server=true}, plays the gameplay scenarios and stops the server. */
@Mixin(MinecraftServer.class)
public class ServerSmokeMixin {
    @Unique private ServerSmokeScenario revived$scenario;
    @Unique private boolean revived$finished;

    @Inject(method = "tickServer", at = @At("TAIL"))
    private void revived$playScenarios(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        if (revived$finished || !Boolean.getBoolean("revived.smoke.server")) return;
        MinecraftServer server = (MinecraftServer)(Object)this;
        if (revived$scenario == null) revived$scenario = new ServerSmokeScenario(server);
        if (revived$scenario.tick()) {
            revived$finished = true;
            System.out.println("REVIVED_SERVER_SMOKE_PASSED: golem pickup, oxidation and waxing, removal and death drops");
            server.halt(false);
        }
    }
}
