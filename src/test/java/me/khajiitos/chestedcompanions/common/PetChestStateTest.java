package me.khajiitos.chestedcompanions.common;

import me.khajiitos.chestedcompanions.common.util.ChestEntityCommon;
import me.khajiitos.chestedcompanions.common.util.PetChestSounds;
import me.khajiitos.chestedcompanions.common.util.PetChestState;
import me.khajiitos.chestedcompanions.common.util.PetEnderChestContainer;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class PetChestStateTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static void verifySound(PetFixture fixture, SoundEvent sound, int times) {
        verify(fixture.level, times(times)).playSound(isNull(), anyDouble(), anyDouble(), anyDouble(), eq(sound),
                eq(SoundSource.BLOCKS), eq(0.5F), anyFloat());
    }

    @Test void firstOpenerPlaysTheOpenSoundAndTheLastOneLeavingPlaysTheCloseSound() {
        PetFixture fixture = new PetFixture(new ItemStack(Items.CHEST), 9);
        Player first = fixture.ownerNearby();
        Player second = fixture.ownerNearby();
        var inventory = fixture.inventory.get();

        inventory.startOpen(first);
        assertTrue(fixture.state.isOpen());
        verifySound(fixture, SoundEvents.CHEST_OPEN, 1);
        verify(fixture.level).broadcastEntityEvent(fixture.pet, PetChestState.EVENT_OPEN);

        inventory.startOpen(second);
        inventory.stopOpen(first);
        assertEquals(1, fixture.state.openerCount());
        verifySound(fixture, SoundEvents.CHEST_OPEN, 1);
        verifySound(fixture, SoundEvents.CHEST_CLOSE, 0);

        inventory.stopOpen(second);
        assertFalse(fixture.state.isOpen());
        verifySound(fixture, SoundEvents.CHEST_CLOSE, 1);
        verify(fixture.level).broadcastEntityEvent(fixture.pet, PetChestState.EVENT_CLOSE);
        inventory.stopOpen(second);
        verify(fixture.level, times(1)).broadcastEntityEvent(fixture.pet, PetChestState.EVENT_CLOSE);
    }

    @Test void spectatorsOpenChestsSilentlyLikeInVanilla() {
        PetFixture fixture = new PetFixture(new ItemStack(Items.CHEST), 9);
        Player spectator = fixture.ownerNearby();
        when(spectator.isSpectator()).thenReturn(true);
        fixture.inventory.get().startOpen(spectator);
        assertFalse(fixture.state.isOpen());
        verifyNoInteractions(fixture.level);
    }

    @Test void everyChestTypePlaysItsOwnSounds() {
        assertEquals(SoundEvents.ENDER_CHEST_OPEN, PetChestSounds.open(new ItemStack(Items.ENDER_CHEST)));
        assertEquals(SoundEvents.ENDER_CHEST_CLOSE, PetChestSounds.close(new ItemStack(Items.ENDER_CHEST)));
        assertEquals(SoundEvents.CHEST_OPEN, PetChestSounds.open(new ItemStack(Items.CHEST)));
        for (var item : java.util.List.of(Items.COPPER_CHEST, Items.EXPOSED_COPPER_CHEST, Items.WEATHERED_COPPER_CHEST,
                Items.OXIDIZED_COPPER_CHEST, Items.WAXED_COPPER_CHEST, Items.WAXED_OXIDIZED_COPPER_CHEST)) {
            ChestBlock block = (ChestBlock) net.minecraft.world.level.block.Block.byItem(item);
            assertEquals(block.getOpenChestSound(), PetChestSounds.open(new ItemStack(item)), item.toString());
            assertEquals(block.getCloseChestSound(), PetChestSounds.close(new ItemStack(item)), item.toString());
        }
        assertSame(((ChestBlock) Blocks.COPPER_CHEST).getOpenChestSound(), PetChestSounds.open(new ItemStack(Items.WAXED_COPPER_CHEST)),
                "A waxed chest sounds like its unwaxed twin");
    }

    @Test void theEnderViewOpensThePetToo() {
        PetFixture fixture = new PetFixture(new ItemStack(Items.ENDER_CHEST), 0);
        Player player = fixture.ownerNearby();
        when(player.getEnderChestInventory()).thenReturn(new PlayerEnderChestContainer());
        var view = new PetEnderChestContainer<>(fixture.pet, player);
        view.startOpen(player);
        assertTrue(fixture.state.isOpen());
        verifySound(fixture, SoundEvents.ENDER_CHEST_OPEN, 1);
        view.stopOpen(player);
        verifySound(fixture, SoundEvents.ENDER_CHEST_CLOSE, 1);
    }

    @Test void removingTheChestClosesItSilentlyAndForgetsEveryOpener() {
        PetFixture fixture = new PetFixture(new ItemStack(Items.CHEST), 9);
        fixture.inventory.get().startOpen(fixture.ownerNearby());
        ChestEntityCommon.removeChestContent(fixture.pet, true);
        assertFalse(fixture.state.isOpen());
        verifySound(fixture, SoundEvents.CHEST_CLOSE, 0);
        verify(fixture.level).broadcastEntityEvent(fixture.pet, PetChestState.EVENT_CLOSE);
    }

    @Test void openersWhoStoppedLookingAreDroppedByTheRecheck() {
        PetFixture fixture = new PetFixture(new ItemStack(Items.CHEST), 9);
        var inventory = fixture.inventory.get();
        Player viewer = fixture.ownerNearby();
        Player gone = fixture.ownerNearby();
        ChestMenu menu = mock(ChestMenu.class);
        when(menu.getContainer()).thenReturn(inventory);
        viewer.containerMenu = menu;
        inventory.startOpen(viewer);
        inventory.startOpen(gone);

        fixture.state.serverTick(fixture.pet);
        assertEquals(1, fixture.state.openerCount(), "The player whose menu is not this chest is dropped");
        verifySound(fixture, SoundEvents.CHEST_CLOSE, 0);

        viewer.containerMenu = null;
        fixture.state.serverTick(fixture.pet);
        assertFalse(fixture.state.isOpen());
        verifySound(fixture, SoundEvents.CHEST_CLOSE, 1);
    }

    @Test void golemsStayOpenersWhileNearAndAreDroppedOnceFarOrDead() {
        PetFixture fixture = new PetFixture(new ItemStack(Items.COPPER_CHEST), 9);
        CopperGolem golem = mock(CopperGolem.class);
        when(golem.getLivingEntity()).thenReturn(golem);
        when(golem.isAlive()).thenReturn(true);
        when(golem.level()).thenReturn(fixture.level);
        when(golem.getContainerInteractionRange()).thenReturn(3.0);
        when(golem.distanceToSqr(fixture.pet)).thenReturn(4.0);
        fixture.inventory.get().startOpen(golem);
        assertTrue(fixture.state.isOpen());
        fixture.state.serverTick(fixture.pet);
        assertTrue(fixture.state.isOpen());

        when(golem.distanceToSqr(fixture.pet)).thenReturn(400.0);
        fixture.state.serverTick(fixture.pet);
        assertFalse(fixture.state.isOpen());
        assertTrue(fixture.inventory.get().getEntitiesWithContainerOpen().isEmpty());
    }

    @Test void theContainerListsItsOpeners() {
        PetFixture fixture = new PetFixture(new ItemStack(Items.COPPER_CHEST), 9);
        Player player = fixture.ownerNearby();
        fixture.inventory.get().startOpen(player);
        assertEquals(java.util.List.<ContainerUser>of(player), fixture.inventory.get().getEntitiesWithContainerOpen());
    }

    @Test void freezeLastsTheRequestedTicksAndNeverShortensAnEarlierOne() {
        PetFixture fixture = new PetFixture(new ItemStack(Items.COPPER_CHEST), 9);
        when(fixture.level.getGameTime()).thenReturn(100L);
        assertFalse(fixture.state.isFrozen(fixture.level));
        fixture.state.freeze(fixture.level, 10);
        fixture.state.freeze(fixture.level, 2);
        when(fixture.level.getGameTime()).thenReturn(109L);
        assertTrue(fixture.state.isFrozen(fixture.level));
        when(fixture.level.getGameTime()).thenReturn(110L);
        assertFalse(fixture.state.isFrozen(fixture.level));
    }

    @Test void lidFollowsTheServerAtTheVanillaPace() {
        PetChestState state = new PetChestState();
        assertEquals(0.0F, state.lidOpenness(1.0F));
        state.setLidOpen(true);
        for (int i = 0; i < 5; i++) state.tickLid();
        assertEquals(0.5F, state.lidOpenness(1.0F), 1.0E-5F);
        for (int i = 0; i < 10; i++) state.tickLid();
        assertEquals(1.0F, state.lidOpenness(1.0F));
        state.setLidOpen(false);
        for (int i = 0; i < 10; i++) state.tickLid();
        assertEquals(0.0F, state.lidOpenness(1.0F));
    }
}
