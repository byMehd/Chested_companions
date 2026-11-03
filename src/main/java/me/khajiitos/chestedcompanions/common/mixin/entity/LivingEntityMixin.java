package me.khajiitos.chestedcompanions.common.mixin.entity;

import me.khajiitos.chestedcompanions.common.util.IChestEntity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /**
     * A pet whose chest is open, or that a copper golem is about to work on, stays where it is. An immobile entity does
     * not run its AI at all, so a sitting pet keeps sitting and a standing one keeps standing, with no goal interrupted.
     */
    @Inject(at = @At("HEAD"), method = "isImmobile", cancellable = true)
    private void chestedCompanions$holdStill(CallbackInfoReturnable<Boolean> cir) {
        Object self = this;
        if (self instanceof IChestEntity pet && pet.chestedCompanions$getState().keepsStill((LivingEntity) self)) {
            cir.setReturnValue(true);
        }
    }
}
