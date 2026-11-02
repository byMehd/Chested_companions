package me.khajiitos.chestedcompanions.common;

import me.khajiitos.chestedcompanions.common.util.IChestEntity;
import me.khajiitos.chestedcompanions.common.util.PetChestState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** A tamed pet wearing a chest, backed by mocks, for tests that exercise the shared chest logic. */
final class PetFixture {
    abstract static class Pet extends TamableAnimal implements IChestEntity {
        protected Pet(EntityType<? extends TamableAnimal> type, Level level) { super(type, level); }
    }

    final Pet pet = mock(Pet.class);
    final ServerLevel level = mock(ServerLevel.class);
    final PetChestState state = new PetChestState();
    final AtomicReference<ItemStack> chest = new AtomicReference<>(ItemStack.EMPTY);
    final AtomicReference<IChestEntity.PetChestContainer<?>> inventory = new AtomicReference<>();

    PetFixture(ItemStack chestItem, int slots) {
        chest.set(chestItem);
        when(level.getRandom()).thenReturn(RandomSource.create(1234L));
        when(pet.level()).thenReturn(level);
        when(pet.isAlive()).thenReturn(true);
        when(pet.isTame()).thenReturn(true);
        setBox(pet, new AABB(0, 0, 0, 0.6, 0.7, 0.6));
        when(pet.position()).thenReturn(new Vec3(0, 0, 0));
        when(pet.chestedCompanions$getState()).thenReturn(state);
        when(pet.chestedCompanions$getInventory()).thenAnswer(i -> inventory.get());
        when(pet.chestedCompanions$getChestItemStack()).thenAnswer(i -> chest.get());
        when(pet.chestedCompanions$hasChest()).thenAnswer(i -> !chest.get().isEmpty());
        when(pet.chestedCompanions$hasEnderChest()).thenAnswer(i -> chest.get().is(net.minecraft.world.item.Items.ENDER_CHEST));
        when(pet.chestedCompanions$hasCopperChest()).thenAnswer(i -> me.khajiitos.chestedcompanions.common.util.PetCopperChest.isCopper(chest.get()));
        doAnswer(i -> { chest.set(i.getArgument(0)); return null; }).when(pet).chestedCompanions$setChestItemStack(any());
        if (slots > 0) {
            inventory.set(new IChestEntity.PetChestContainer<>(pet, slots, null));
        }
    }

    /**
     * Mocks are created without running constructors and the project mocks by subclassing, which cannot stub
     * {@code final} methods such as {@code getBoundingBox()}, {@code getX()} or {@code getBbHeight()}; the fields they read are set instead.
     */
    static void setBox(Entity entity, AABB box) {
        setField(entity, "bb", box);
        setField(entity, "position", new Vec3(box.minX, box.minY, box.minZ));
        setField(entity, "dimensions", EntityDimensions.scalable((float) box.getXsize(), (float) box.getYsize()));
    }

    static void setField(Entity entity, String name, Object value) {
        try {
            var field = Entity.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(entity, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A player the pet considers its owner and who is standing next to it. */
    Player ownerNearby() {
        Player player = mock(Player.class);
        when(player.level()).thenReturn(level);
        when(player.getLivingEntity()).thenReturn(player);
        when(player.isAlive()).thenReturn(true);
        when(player.getContainerInteractionRange()).thenReturn(4.5);
        when(pet.isOwnedBy(player)).thenReturn(true);
        when(player.isWithinEntityInteractionRange(pet, 4.0)).thenReturn(true);
        return player;
    }
}
