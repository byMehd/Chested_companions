package me.khajiitos.chestedcompanions.common.mixin.entity;

import me.khajiitos.chestedcompanions.common.util.IChestEntity;
import me.khajiitos.chestedcompanions.common.util.PetChestState;
import net.minecraft.world.entity.TamableAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client side of the chest lid: cats and wolves receive the open and close events broadcast by the server. */
@Mixin(TamableAnimal.class)
public abstract class TamableAnimalMixin {
    @Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
    private void chestedCompanions$handleChestEvent(byte id, CallbackInfo ci) {
        if ((id == PetChestState.EVENT_OPEN || id == PetChestState.EVENT_CLOSE) && (Object) this instanceof IChestEntity chest) {
            chest.chestedCompanions$getState().setLidOpen(id == PetChestState.EVENT_OPEN);
            ci.cancel();
        }
    }
}
