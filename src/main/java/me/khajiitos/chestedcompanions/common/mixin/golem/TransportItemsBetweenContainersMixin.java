package me.khajiitos.chestedcompanions.common.mixin.golem;

import me.khajiitos.chestedcompanions.common.golem.IPetPickupGolem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps vanilla's chest-to-chest behaviour and the pet visit from running together: each one waits while the
 * other has the golem, and vanilla reports whether it already has a placed chest in sight.
 */
@Mixin(TransportItemsBetweenContainers.class)
public abstract class TransportItemsBetweenContainersMixin {
    @Shadow
    private TransportItemsBetweenContainers.TransportItemTarget target;

    @Inject(method = "checkExtraStartConditions", at = @At("HEAD"), cancellable = true)
    private void chestedCompanions$waitForPetVisit(ServerLevel level, PathfinderMob mob, CallbackInfoReturnable<Boolean> cir) {
        if (mob instanceof IPetPickupGolem golem && golem.chestedCompanions$isPickingFromPet()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void chestedCompanions$pauseDuringPetVisit(ServerLevel level, PathfinderMob mob, long time, CallbackInfo ci) {
        if (mob instanceof IPetPickupGolem golem && golem.chestedCompanions$isPickingFromPet()) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void chestedCompanions$reportTarget(ServerLevel level, PathfinderMob mob, long time, CallbackInfo ci) {
        if (mob instanceof IPetPickupGolem golem) {
            golem.chestedCompanions$setTransportingBetweenChests(this.target != null);
        }
    }

    @Inject(method = "stop", at = @At("TAIL"))
    private void chestedCompanions$reportStop(ServerLevel level, PathfinderMob mob, long time, CallbackInfo ci) {
        if (mob instanceof IPetPickupGolem golem) {
            golem.chestedCompanions$setTransportingBetweenChests(false);
        }
    }
}
