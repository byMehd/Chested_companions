package me.khajiitos.chestedcompanions.common.mixin.golem;

import com.google.common.collect.ImmutableList;
import me.khajiitos.chestedcompanions.common.golem.PetChestPickupBehavior;
//? if >=26.1 {
/*import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?} else {
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//?}
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(CopperGolemAi.class)
public abstract class CopperGolemAiMixin {
    //? if >=26.1 {
    /*// Minecraft 26.1 builds the idle activity as a value instead of filling a brain, so the pickup is added to that value:
    // it is tried just before the vanilla block transport (priority 0), which then stays out of its way.
    @Inject(method = "initIdleActivity", at = @At("RETURN"), cancellable = true)
    private static void chestedCompanions$addPetPickup(CallbackInfoReturnable<ActivityData<CopperGolem>> cir) {
        ActivityData<CopperGolem> idle = cir.getReturnValue();
        ImmutableList.Builder<Pair<Integer, ? extends BehaviorControl<? super CopperGolem>>> behaviors = ImmutableList.builder();
        behaviors.add(Pair.of(-1, new PetChestPickupBehavior()));
        behaviors.addAll(idle.behaviorPriorityPairs());
        cir.setReturnValue(new ActivityData<>(idle.activityType(), behaviors.build(), idle.conditions(), idle.memoriesToEraseWhenStopped()));
    }
    *///?} else {
    /** Tried just before the vanilla block transport (priority 0), which then stays out of its way. */
    @Inject(method = "initIdleActivity", at = @At("TAIL"))
    private static void chestedCompanions$addPetPickup(Brain<CopperGolem> brain, CallbackInfo ci) {
        brain.addActivity(Activity.IDLE, -1, ImmutableList.of(new PetChestPickupBehavior()));
    }
    //?}
}
