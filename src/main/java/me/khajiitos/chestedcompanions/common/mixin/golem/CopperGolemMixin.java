package me.khajiitos.chestedcompanions.common.mixin.golem;

import me.khajiitos.chestedcompanions.common.golem.IPetPickupGolem;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CopperGolem.class)
public abstract class CopperGolemMixin implements IPetPickupGolem {
    @Unique
    private boolean chestedCompanions$pickingFromPet;

    @Unique
    private boolean chestedCompanions$transporting;

    @Override
    public boolean chestedCompanions$isPickingFromPet() {
        return this.chestedCompanions$pickingFromPet;
    }

    @Override
    public void chestedCompanions$setPickingFromPet(boolean pickingFromPet) {
        this.chestedCompanions$pickingFromPet = pickingFromPet;
    }

    @Override
    public boolean chestedCompanions$isTransportingBetweenChests() {
        return this.chestedCompanions$transporting;
    }

    @Override
    public void chestedCompanions$setTransportingBetweenChests(boolean transporting) {
        this.chestedCompanions$transporting = transporting;
    }
}
