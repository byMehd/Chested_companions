package me.khajiitos.chestedcompanions.common.golem;

import me.khajiitos.chestedcompanions.common.util.IChestEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Keeps a pet still while a copper golem works on its chest, so the golem can finish its animation
 * as it would at a placed chest. It never holds a pet that is hurt, burning or swimming.
 */
public final class PetGolemFreezeGoal<T extends TamableAnimal & IChestEntity> extends Goal {
    private final T pet;

    public PetGolemFreezeGoal(T pet) {
        this.pet = pet;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.pet.chestedCompanions$getState().isFrozen(this.pet.level())
                && this.pet.hurtTime == 0 && !this.pet.isOnFire() && !this.pet.isInWater() && !this.pet.isInLava();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.pet.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.pet.getNavigation().stop();
    }
}
