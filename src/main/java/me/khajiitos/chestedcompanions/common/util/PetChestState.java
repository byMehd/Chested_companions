package me.khajiitos.chestedcompanions.common.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime-only chest state of one pet: who currently has the chest open (players and copper golems),
 * the lid animation the client plays, and the short freeze a copper golem asks for while it works.
 * Opening and closing behave like a vanilla chest: the first opener plays the open sound, the last one
 * leaving plays the close sound, and trackers are told through an entity event.
 */
public final class PetChestState {
    /** Entity events sent to trackers; vanilla's own events stay far below these values. */
    public static final byte EVENT_OPEN = 118;
    public static final byte EVENT_CLOSE = 119;
    private static final int RECHECK_INTERVAL = 5;
    private static final double OPENER_EXTRA_RANGE = 4.0;

    private final List<ContainerUser> openers = new ArrayList<>();
    private final ChestLidController lid = new ChestLidController();
    private SoundEvent closeSound = SoundEvents.CHEST_CLOSE;
    private long frozenUntil = Long.MIN_VALUE;

    public boolean isOpen() {
        return !this.openers.isEmpty();
    }

    public int openerCount() {
        return this.openers.size();
    }

    public List<ContainerUser> openers() {
        return List.copyOf(this.openers);
    }

    public <T extends TamableAnimal & IChestEntity> void open(T pet, ContainerUser user) {
        LivingEntity opener = user.getLivingEntity();
        if (opener.isSpectator() || !(pet.level() instanceof ServerLevel level)) {
            return;
        }
        boolean wasClosed = this.openers.isEmpty();
        this.openers.add(user);
        if (wasClosed) {
            this.closeSound = PetChestSounds.close(pet.chestedCompanions$getChestItemStack());
            playSound(level, pet, PetChestSounds.open(pet.chestedCompanions$getChestItemStack()));
            level.gameEvent(opener, GameEvent.CONTAINER_OPEN, pet.position());
            level.broadcastEntityEvent(pet, EVENT_OPEN);
        }
    }

    public <T extends TamableAnimal & IChestEntity> void close(T pet, ContainerUser user) {
        if (this.openers.remove(user) && this.openers.isEmpty() && pet.level() instanceof ServerLevel level) {
            this.announceClosed(level, pet, user.getLivingEntity());
        }
    }

    /** Drops openers that left, died or stopped looking inside, like a vanilla chest rechecking its viewers. */
    public <T extends TamableAnimal & IChestEntity> void serverTick(T pet) {
        if (this.openers.isEmpty() || pet.tickCount % RECHECK_INTERVAL != 0) {
            return;
        }
        if (this.openers.removeIf(user -> !stillOpen(pet, user)) && this.openers.isEmpty()
                && pet.level() instanceof ServerLevel level) {
            this.announceClosed(level, pet, null);
        }
    }

    /** The chest is gone: forget every opener and close the lid without a sound. */
    public <T extends TamableAnimal & IChestEntity> void reset(T pet) {
        if (this.openers.isEmpty()) {
            return;
        }
        this.openers.clear();
        if (pet.level() instanceof ServerLevel level) {
            level.broadcastEntityEvent(pet, EVENT_CLOSE);
        }
    }

    public void freeze(Level level, int ticks) {
        this.frozenUntil = Math.max(this.frozenUntil, level.getGameTime() + ticks);
    }

    public boolean isFrozen(Level level) {
        return level.getGameTime() < this.frozenUntil;
    }

    public void setLidOpen(boolean open) {
        this.lid.shouldBeOpen(open);
    }

    public void tickLid() {
        this.lid.tickLid();
    }

    public float lidOpenness(float partialTick) {
        return this.lid.getOpenness(partialTick);
    }

    private <T extends TamableAnimal & IChestEntity> void announceClosed(ServerLevel level, T pet, @Nullable LivingEntity closer) {
        playSound(level, pet, this.closeSound);
        level.gameEvent(closer, GameEvent.CONTAINER_CLOSE, pet.position());
        level.broadcastEntityEvent(pet, EVENT_CLOSE);
    }

    private static <T extends TamableAnimal & IChestEntity> boolean stillOpen(T pet, ContainerUser user) {
        LivingEntity opener = user.getLivingEntity();
        if (!opener.isAlive() || opener.isRemoved() || opener.isSpectator() || opener.level() != pet.level()) {
            return false;
        }
        if (opener instanceof Player player) {
            return isViewing(pet, player.containerMenu);
        }
        double range = user.getContainerInteractionRange() + OPENER_EXTRA_RANGE;
        return opener.distanceToSqr(pet) <= range * range;
    }

    private static boolean isViewing(TamableAnimal pet, AbstractContainerMenu menu) {
        if (!(menu instanceof ChestMenu chestMenu)) {
            return false;
        }
        Container container = chestMenu.getContainer();
        return container instanceof IChestEntity.PetChestContainer<?> own && own.getPet() == pet
                || container instanceof PetEnderChestContainer<?> ender && ender.getPet() == pet;
    }

    private static void playSound(ServerLevel level, TamableAnimal pet, SoundEvent sound) {
        level.playSound(null, pet.getX(), pet.getY() + pet.getBbHeight() / 2.0, pet.getZ(), sound,
                SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
    }
}
