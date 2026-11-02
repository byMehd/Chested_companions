package me.khajiitos.chestedcompanions.common.golem;

import com.google.common.collect.ImmutableMap;
import me.khajiitos.chestedcompanions.common.config.CCConfig;
import me.khajiitos.chestedcompanions.common.util.IChestEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lets a copper golem fetch an item from the copper chest a nearby pet wears, the way it fetches one from a
 * placed copper chest: walk up, stop, look at the chest, open it, play the vanilla "getting item" animation and
 * sound for the same 60 ticks, then carry one stack of up to 16 items away. Delivering the item to a chest is
 * left to the vanilla {@link TransportItemsBetweenContainers} behaviour, which this one yields to and which
 * yields to this one while it works. The pet is held still for the whole visit.
 */
public final class PetChestPickupBehavior extends Behavior<CopperGolem> {
    private static final double SEARCH_RADIUS = 16.0;
    private static final double SEARCH_HEIGHT = 8.0;
    private static final int SEARCH_INTERVAL = 40;
    private static final int MAX_TRAVEL_TICKS = 600;
    private static final long VISITED_MEMORY_TICKS = 6000L;
    private static final double START_RANGE = 1.0;
    private static final double CONTINUE_RANGE = 2.0;
    /** The pet is held still only once the golem is about to reach it, so a golem stuck behind a wall never pins a pet down. */
    private static final double FREEZE_DISTANCE_SQR = 2.5 * 2.5;
    private static final int FREEZE_TICKS = 10;
    private static final int INTERACTION_TICKS = TransportItemsBetweenContainers.TARGET_INTERACTION_TIME;
    private static final int TICK_TO_OPEN = 1;
    private static final int TICK_TO_PLAY_SOUND = 9;
    private static final int MAX_STACK_SIZE = 16;

    private enum Phase { TRAVELLING, INTERACTING }

    private @Nullable TamableAnimal target;
    private Phase phase = Phase.TRAVELLING;
    private int ticks;
    private boolean gettingItem;
    private boolean opened;
    private long nextSearch;
    private final Map<UUID, Long> visited = new HashMap<>();

    public PetChestPickupBehavior() {
        super(ImmutableMap.of(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, CopperGolem golem) {
        if (!CCConfig.copperGolemsUsePetChests.get() || golem.isLeashed() || golem.isPassenger()
                || !golem.getMainHandItem().isEmpty()) {
            return false;
        }
        long now = level.getGameTime();
        if (now < this.nextSearch || golem instanceof IPetPickupGolem busy && busy.chestedCompanions$isTransportingBetweenChests()) {
            return false;
        }
        this.target = findTarget(level, golem, now);
        if (this.target == null) {
            this.nextSearch = now + SEARCH_INTERVAL;
            return false;
        }
        return true;
    }

    @Override
    protected void start(ServerLevel level, CopperGolem golem, long time) {
        this.phase = Phase.TRAVELLING;
        this.ticks = 0;
        this.opened = false;
        markPickingFromPet(golem, true);
    }

    @Override
    protected boolean timedOut(long time) {
        return false;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, CopperGolem golem, long time) {
        return this.target != null && CCConfig.copperGolemsUsePetChests.get() && !golem.isLeashed()
                && !golem.isPassenger() && !golem.isPanicking() && stillReachable(golem, this.target);
    }

    @Override
    protected void tick(ServerLevel level, CopperGolem golem, long time) {
        TamableAnimal pet = this.target;
        if (pet == null) {
            return;
        }
        IChestEntity chest = (IChestEntity) pet;
        if (golem.distanceToSqr(pet) < FREEZE_DISTANCE_SQR) {
            chest.chestedCompanions$getState().freeze(level, FREEZE_TICKS);
        }
        if (this.phase == Phase.TRAVELLING) {
            if (withinReach(golem, pet, START_RANGE)) {
                this.phase = Phase.INTERACTING;
                this.ticks = 0;
                this.gettingItem = chest.chestedCompanions$getInventory() != null && !chest.chestedCompanions$getInventory().isEmpty();
            } else if (++this.ticks > MAX_TRAVEL_TICKS) {
                this.visited.put(pet.getUUID(), time + VISITED_MEMORY_TICKS);
                this.target = null;
                return;
            } else {
                BehaviorUtils.setWalkAndLookTargetMemories(golem, pet, 1.0F, 1);
                return;
            }
        }
        interact(level, golem, pet, chest, time);
    }

    private void interact(ServerLevel level, CopperGolem golem, TamableAnimal pet, IChestEntity chest, long time) {
        if (!withinReach(golem, pet, CONTINUE_RANGE)) {
            closeChest(golem, chest);
            golem.setState(CopperGolemState.IDLE);
            this.phase = Phase.TRAVELLING;
            this.ticks = 0;
            return;
        }
        golem.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(pet, true));
        stopInPlace(golem);
        this.ticks++;
        if (this.ticks == TICK_TO_OPEN && chest.chestedCompanions$getInventory() != null) {
            chest.chestedCompanions$getInventory().startOpen(golem);
            this.opened = true;
            golem.setState(this.gettingItem ? CopperGolemState.GETTING_ITEM : CopperGolemState.GETTING_NO_ITEM);
        }
        if (this.ticks == TICK_TO_PLAY_SOUND) {
            golem.playSound(this.gettingItem ? SoundEvents.COPPER_GOLEM_ITEM_GET : SoundEvents.COPPER_GOLEM_ITEM_NO_GET);
        }
        if (this.ticks >= INTERACTION_TICKS) {
            closeChest(golem, chest);
            var inventory = chest.chestedCompanions$getInventory();
            if (inventory != null && !inventory.isEmpty()) {
                golem.setItemSlot(EquipmentSlot.MAINHAND, takeFirstStack(inventory));
                golem.setGuaranteedDrop(EquipmentSlot.MAINHAND);
                inventory.setChanged();
                this.visited.clear();
            } else {
                this.visited.put(pet.getUUID(), time + VISITED_MEMORY_TICKS);
            }
            golem.setState(CopperGolemState.IDLE);
            this.target = null;
        }
    }

    @Override
    protected void stop(ServerLevel level, CopperGolem golem, long time) {
        if (this.target != null) {
            closeChest(golem, (IChestEntity) this.target);
        }
        if (this.phase == Phase.INTERACTING) {
            golem.setState(CopperGolemState.IDLE);
        }
        markPickingFromPet(golem, false);
        golem.getNavigation().stop();
        golem.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        this.target = null;
        this.phase = Phase.TRAVELLING;
        this.ticks = 0;
        this.opened = false;
    }

    private void closeChest(CopperGolem golem, IChestEntity chest) {
        if (this.opened) {
            this.opened = false;
            var inventory = chest.chestedCompanions$getInventory();
            if (inventory != null) {
                inventory.stopOpen(golem);
            }
        }
    }

    private @Nullable TamableAnimal findTarget(ServerLevel level, CopperGolem golem, long now) {
        AABB area = golem.getBoundingBox().inflate(SEARCH_RADIUS, SEARCH_HEIGHT, SEARCH_RADIUS);
        List<TamableAnimal> pets = level.getEntitiesOfClass(TamableAnimal.class, area, pet -> isCandidate(pet, now));
        return pets.stream().min(Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
    }

    private boolean isCandidate(TamableAnimal pet, long now) {
        if (!(pet instanceof IChestEntity chest) || !pet.isAlive() || pet.isRemoved() || pet.isPassenger()
                || !chest.chestedCompanions$hasCopperChest()) {
            return false;
        }
        var inventory = chest.chestedCompanions$getInventory();
        if (inventory == null || inventory.isEmpty() || chest.chestedCompanions$getState().isOpen()) {
            return false;
        }
        Long expiry = this.visited.get(pet.getUUID());
        return expiry == null || expiry <= now;
    }

    private static boolean stillReachable(CopperGolem golem, TamableAnimal pet) {
        return pet instanceof IChestEntity chest && pet.isAlive() && !pet.isRemoved() && pet.level() == golem.level()
                && chest.chestedCompanions$hasCopperChest() && chest.chestedCompanions$getInventory() != null
                && golem.distanceToSqr(pet) <= SEARCH_RADIUS * SEARCH_RADIUS * 4.0;
    }

    private static boolean withinReach(CopperGolem golem, TamableAnimal pet, double range) {
        return golem.getBoundingBox().inflate(range, 0.5, range).intersects(pet.getBoundingBox());
    }

    private static void markPickingFromPet(CopperGolem golem, boolean pickingFromPet) {
        if (golem instanceof IPetPickupGolem marked) {
            marked.chestedCompanions$setPickingFromPet(pickingFromPet);
        }
    }

    private static void stopInPlace(CopperGolem golem) {
        golem.getNavigation().stop();
        golem.setXxa(0.0F);
        golem.setYya(0.0F);
        golem.setSpeed(0.0F);
        golem.setDeltaMovement(0.0, golem.getDeltaMovement().y, 0.0);
    }

    /** Same pick as vanilla: the first non-empty slot, at most one transported stack of 16. */
    private static ItemStack takeFirstStack(net.minecraft.world.Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                return container.removeItem(slot, Math.min(stack.getCount(), MAX_STACK_SIZE));
            }
        }
        return ItemStack.EMPTY;
    }
}
