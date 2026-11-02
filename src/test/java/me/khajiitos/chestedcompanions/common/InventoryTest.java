package me.khajiitos.chestedcompanions.common;

import me.khajiitos.chestedcompanions.common.config.CCConfig;
import me.khajiitos.chestedcompanions.common.util.ChestEntityCommon;
import me.khajiitos.chestedcompanions.common.util.IChestEntity;
import me.khajiitos.chestedcompanions.common.util.PetEnderChestContainer;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class InventoryTest {
    abstract static class Pet extends TamableAnimal implements IChestEntity {
        protected Pet(EntityType<? extends TamableAnimal> type, Level level) { super(type, level); }
    }
    Pet pet;
    Player player;
    ServerLevel level;
    AtomicReference<ItemStack> chest;
    AtomicReference<IChestEntity.PetChestContainer<?>> current;

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
    }

    @BeforeEach void setup() {
        CCConfig.publicChest.set(false);
        pet = mock(Pet.class);
        player = mock(Player.class);
        level = mock(ServerLevel.class);
        chest = new AtomicReference<>(new ItemStack(Items.CHEST));
        current = new AtomicReference<>();
        when(pet.level()).thenReturn(level);
        when(player.level()).thenReturn(level);
        when(pet.isAlive()).thenReturn(true);
        when(pet.isTame()).thenReturn(true);
        when(pet.isOwnedBy(player)).thenReturn(true);
        when(player.isWithinEntityInteractionRange(pet, 4.0)).thenReturn(true);
        when(pet.chestedCompanions$getInventory()).thenAnswer(i -> current.get());
        when(pet.chestedCompanions$getState()).thenReturn(new me.khajiitos.chestedcompanions.common.util.PetChestState());
        when(pet.chestedCompanions$getChestItemStack()).thenAnswer(i -> chest.get());
        when(pet.chestedCompanions$hasChest()).thenAnswer(i -> !chest.get().isEmpty());
        when(pet.chestedCompanions$hasEnderChest()).thenAnswer(i -> chest.get().is(Items.ENDER_CHEST));
        doAnswer(i -> { chest.set(i.getArgument(0)); return null; }).when(pet).chestedCompanions$setChestItemStack(any());
        doAnswer(i -> { current.set(null); return null; }).when(pet).chestedCompanions$removeInventory();
    }

    IChestEntity.PetChestContainer<Pet> inventory(int slots) {
        var inventory = new IChestEntity.PetChestContainer<>(pet, slots, null);
        current.set(inventory);
        return inventory;
    }

    @Test void resizingMovesStacksAndEmptiesOldContainer() {
        var old = inventory(9);
        old.setItem(0, new ItemStack(Items.DIAMOND, 7));
        var expanded = new IChestEntity.PetChestContainer<>(pet, 27, old);
        current.set(expanded);
        assertTrue(old.isEmpty());
        assertEquals(7, expanded.getItem(0).getCount());
        assertFalse(old.stillValid(player));
        assertTrue(expanded.stillValid(player));
    }

    @Test void shrinkingDropsOverflowExactlyOnce() {
        var old = inventory(27);
        old.setItem(0, new ItemStack(Items.DIAMOND, 4));
        old.setItem(26, new ItemStack(Items.EMERALD, 3));
        var smaller = new IChestEntity.PetChestContainer<>(pet, 9, old);
        assertTrue(old.isEmpty());
        assertEquals(4, smaller.getItem(0).getCount());
        verify(pet, times(1)).spawnAtLocation(eq(level), argThat((ItemStack s) -> s.is(Items.EMERALD) && s.getCount() == 3));
    }

    @Test void removingChestInvalidatesMenuBeforeDroppingAndCannotDuplicate() {
        var inventory = inventory(9);
        inventory.setItem(0, new ItemStack(Items.DIAMOND, 7));
        doAnswer(i -> {
            assertNull(current.get());
            assertTrue(chest.get().isEmpty());
            assertFalse(inventory.stillValid(player));
            return null;
        }).when(pet).spawnAtLocation(eq(level), any(ItemStack.class), eq(0.25f));
        ChestEntityCommon.removeChestContent(pet, true);
        ChestEntityCommon.removeChestContent(pet, true);
        assertTrue(inventory.isEmpty());
        verify(pet, times(2)).spawnAtLocation(eq(level), any(ItemStack.class), eq(0.25f));
    }

    @Test void creativeRemovalStillDropsContentsButDoesNotDropChest() {
        var inventory = inventory(9);
        inventory.setItem(2, new ItemStack(Items.DIAMOND));
        ChestEntityCommon.removeChestContent(pet, false);
        verify(pet, times(1)).spawnAtLocation(eq(level), argThat((ItemStack s) -> s.is(Items.DIAMOND)), eq(0.25f));
        assertTrue(inventory.isEmpty());
    }

    @Test void privateAccessIsRecheckedAfterOwnershipChanges() {
        var inventory = inventory(9);
        assertTrue(inventory.stillValid(player));
        when(pet.isOwnedBy(player)).thenReturn(false);
        assertFalse(inventory.stillValid(player));
        CCConfig.publicChest.set(true);
        assertTrue(inventory.stillValid(player));
        when(pet.isTame()).thenReturn(false);
        assertFalse(inventory.stillValid(player), "Public mode must not allow wild pets");
    }

    @Test void menuClosesOnDeathUnloadDistanceDimensionAndChestRemoval() throws Exception {
        var inventory = inventory(9);
        when(pet.isAlive()).thenReturn(false); assertFalse(inventory.stillValid(player));
        when(pet.isAlive()).thenReturn(true);
        var removalReason = net.minecraft.world.entity.Entity.class.getDeclaredField("removalReason");
        removalReason.setAccessible(true);
        removalReason.set(pet, net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK);
        assertFalse(inventory.stillValid(player));
        removalReason.set(pet, null);
        when(player.isWithinEntityInteractionRange(pet, 4.0)).thenReturn(false); assertFalse(inventory.stillValid(player));
        when(player.isWithinEntityInteractionRange(pet, 4.0)).thenReturn(true);
        when(player.level()).thenReturn(mock(ServerLevel.class)); assertFalse(inventory.stillValid(player));
        when(player.level()).thenReturn(level);
        chest.set(ItemStack.EMPTY); assertFalse(inventory.stillValid(player));
    }

    @Test void enderViewSharesThePlayersRealInventoryInBothDirections() {
        chest.set(new ItemStack(Items.ENDER_CHEST));
        var ender = new net.minecraft.world.inventory.PlayerEnderChestContainer();
        when(player.getEnderChestInventory()).thenReturn(ender);
        var view = new PetEnderChestContainer<>(pet, player);
        ender.setItem(0, new ItemStack(Items.DIAMOND, 8));
        assertEquals(8, view.getItem(0).getCount());
        view.removeItem(0, 3);
        assertEquals(5, ender.getItem(0).getCount());
        view.setItem(1, new ItemStack(Items.EMERALD, 4));
        assertEquals(4, ender.getItem(1).getCount());
        assertEquals(27, view.getContainerSize());
        assertNull(pet.chestedCompanions$getInventory(), "Ender contents must never live on the pet");
    }

    @Test void publicEnderViewsAlwaysUseEachViewersPersonalInventory() {
        chest.set(new ItemStack(Items.ENDER_CHEST));
        CCConfig.publicChest.set(true);
        Player other = mock(Player.class);
        var first = new net.minecraft.world.inventory.PlayerEnderChestContainer();
        var second = new net.minecraft.world.inventory.PlayerEnderChestContainer();
        when(player.getEnderChestInventory()).thenReturn(first);
        when(other.getEnderChestInventory()).thenReturn(second);
        var ownerView = new PetEnderChestContainer<>(pet, player);
        var otherView = new PetEnderChestContainer<>(pet, other);
        ownerView.setItem(0, new ItemStack(Items.DIAMOND, 8));
        assertTrue(otherView.getItem(0).isEmpty());
        otherView.setItem(0, new ItemStack(Items.EMERALD, 4));
        assertTrue(ownerView.getItem(0).is(Items.DIAMOND));
        assertFalse(ownerView.stillValid(other));
    }

    @Test void removingEnderChestNeverDropsOrDeletesPersonalContents() {
        chest.set(new ItemStack(Items.ENDER_CHEST));
        var ender = new net.minecraft.world.inventory.PlayerEnderChestContainer();
        when(player.getEnderChestInventory()).thenReturn(ender);
        ender.setItem(0, new ItemStack(Items.DIAMOND, 8));
        var view = new PetEnderChestContainer<>(pet, player);
        assertTrue(view.stillValid(player));
        ChestEntityCommon.removeChestContent(pet, true);
        assertFalse(view.stillValid(player));
        assertEquals(8, ender.getItem(0).getCount());
        verify(pet, times(1)).spawnAtLocation(eq(level), argThat((ItemStack s) -> s.is(Items.ENDER_CHEST)), eq(0.25f));
    }
}
