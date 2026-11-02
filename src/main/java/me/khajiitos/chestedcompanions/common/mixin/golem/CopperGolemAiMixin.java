package me.khajiitos.chestedcompanions.common.mixin.golem;

import com.google.common.collect.ImmutableList;
import me.khajiitos.chestedcompanions.common.golem.PetChestPickupBehavior;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemAi;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CopperGolemAi.class)
public abstract class CopperGolemAiMixin {
    /** Tried just before the vanilla block transport (priority 0), which then stays out of its way. */
    @Inject(method = "initIdleActivity", at = @At("TAIL"))
    private static void chestedCompanions$addPetPickup(Brain<CopperGolem> brain, CallbackInfo ci) {
        brain.addActivity(Activity.IDLE, -1, ImmutableList.of(new PetChestPickupBehavior()));
    }
}
