package me.khajiitos.chestedcompanions.common.golem;

/** Implemented by the copper golem through a mixin: which of the two chest-fetching behaviours is at work. */
public interface IPetPickupGolem {
    /** Whether the golem is fetching an item from a pet's chest. */
    boolean chestedCompanions$isPickingFromPet();

    void chestedCompanions$setPickingFromPet(boolean pickingFromPet);

    /** Whether vanilla's behaviour is heading to, or working at, a placed chest. */
    boolean chestedCompanions$isTransportingBetweenChests();

    void chestedCompanions$setTransportingBetweenChests(boolean transporting);
}
