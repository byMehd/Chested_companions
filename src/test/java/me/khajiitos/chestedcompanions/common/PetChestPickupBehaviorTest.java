package me.khajiitos.chestedcompanions.common;

import me.khajiitos.chestedcompanions.common.config.CCConfig;
import me.khajiitos.chestedcompanions.common.golem.IPetPickupGolem;
import me.khajiitos.chestedcompanions.common.golem.PetChestPickupBehavior;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** The copper golem's visit to a pet's chest, played tick by tick against mocks of the golem and the world. */
class PetChestPickupBehaviorTest {
    private final AtomicLong time = new AtomicLong(1000L);
    private final AtomicReference<ItemStack> hand = new AtomicReference<>(ItemStack.EMPTY);
    private final AtomicReference<Double> golemToPetDistance = new AtomicReference<>(2.0);
    private PetFixture fixture;
    private CopperGolem golem;
    private Brain<CopperGolem> brain;
    private PetChestPickupBehavior behavior;

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @SuppressWarnings("unchecked")
    private void build(ItemStack chest) {
        fixture = new PetFixture(chest, 9);
        when(fixture.pet.getUUID()).thenReturn(UUID.randomUUID());
        PetFixture.setBox(fixture.pet, new AABB(1.2, 0, 0, 1.8, 0.7, 0.6));
        when(fixture.level.getGameTime()).thenAnswer(i -> time.get());
        golem = mock(CopperGolem.class, withSettings().extraInterfaces(IPetPickupGolem.class));
        brain = mock(Brain.class);
        when(brain.checkMemory(any(), any())).thenReturn(true);
        when(golem.getBrain()).thenReturn(brain);
        when(golem.level()).thenReturn(fixture.level);
        when(golem.getLivingEntity()).thenReturn(golem);
        when(golem.isAlive()).thenReturn(true);
        when(golem.getContainerInteractionRange()).thenReturn(3.0);
        PetFixture.setBox(golem, new AABB(0, 0, 0, 0.6, 0.9, 0.6));
        when(golem.getNavigation()).thenReturn(mock(PathNavigation.class));
        when(golem.getDeltaMovement()).thenReturn(Vec3.ZERO);
        when(golem.getMainHandItem()).thenAnswer(i -> hand.get());
        when(golem.distanceToSqr(fixture.pet)).thenAnswer(i -> golemToPetDistance.get() * golemToPetDistance.get());
        doAnswer(i -> { hand.set(i.getArgument(1)); return null; }).when(golem).setItemSlot(eq(EquipmentSlot.MAINHAND), any());
        doAnswer(i -> {
            @SuppressWarnings("unchecked") Predicate<TamableAnimal> filter = i.getArgument(2);
            AABB area = i.getArgument(1);
            return area.intersects(fixture.pet.getBoundingBox()) && filter.test(fixture.pet) ? List.of(fixture.pet) : List.of();
        }).when(fixture.level).getEntitiesOfClass(eq(TamableAnimal.class), any(AABB.class), any());
        behavior = new PetChestPickupBehavior();
    }

    @BeforeEach void defaults() { CCConfig.copperGolemsUsePetChests.set(true); }
    @AfterEach void restore() { CCConfig.copperGolemsUsePetChests.set(true); }

    private boolean start() { return behavior.tryStart(fixture.level, golem, time.get()); }

    private void tick(int ticks) {
        for (int i = 0; i < ticks; i++) {
            time.incrementAndGet();
            behavior.tickOrStop(fixture.level, golem, time.get());
        }
    }

    private int diamonds() {
        int total = hand.get().is(Items.DIAMOND) ? hand.get().getCount() : 0;
        for (var stack : fixture.inventory.get()) total += stack.is(Items.DIAMOND) ? stack.getCount() : 0;
        return total;
    }

    @Test void theGolemOpensTheChestAnimatesPlaysItsSoundAndCarriesOneStackAway() {
        build(new ItemStack(Items.COPPER_CHEST));
        fixture.inventory.get().setItem(2, new ItemStack(Items.DIAMOND, 5));
        assertTrue(start());

        tick(1);
        verify(golem).setState(CopperGolemState.GETTING_ITEM);
        assertTrue(fixture.state.isOpen(), "the lid opens as the golem starts");
        assertTrue(fixture.state.isFrozen(fixture.level), "the pet is held still");
        verify(golem, never()).playSound(SoundEvents.COPPER_GOLEM_ITEM_GET);
        tick(8);
        verify(golem).playSound(SoundEvents.COPPER_GOLEM_ITEM_GET);
        assertTrue(hand.get().isEmpty(), "nothing is taken before the interaction ends");
        assertEquals(5, fixture.inventory.get().getItem(2).getCount());

        tick(51);
        assertTrue(hand.get().is(Items.DIAMOND));
        assertEquals(5, hand.get().getCount());
        assertTrue(fixture.inventory.get().isEmpty());
        verify(golem).setGuaranteedDrop(EquipmentSlot.MAINHAND);
        assertFalse(fixture.state.isOpen(), "the lid closes when the golem has what it came for");
        verify(golem, atLeastOnce()).setState(CopperGolemState.IDLE);
        tick(1);
        assertEquals(Behavior.Status.STOPPED, behavior.getStatus());
        assertEquals(5, diamonds(), "nothing is lost or duplicated");
    }

    @Test void theGolemTakesAtMostSixteenItemsFromTheFirstStack() {
        build(new ItemStack(Items.WEATHERED_COPPER_CHEST));
        fixture.inventory.get().setItem(0, new ItemStack(Items.DIAMOND, 40));
        fixture.inventory.get().setItem(1, new ItemStack(Items.EMERALD, 3));
        assertTrue(start());
        tick(61);
        assertEquals(16, hand.get().getCount());
        assertEquals(24, fixture.inventory.get().getItem(0).getCount());
        assertEquals(3, fixture.inventory.get().getItem(1).getCount());
        assertEquals(40, diamonds());
    }

    @Test void waxedAndOxidisedCopperChestsAreFairGame() {
        for (var item : List.of(Items.WAXED_COPPER_CHEST, Items.OXIDIZED_COPPER_CHEST, Items.WAXED_OXIDIZED_COPPER_CHEST)) {
            build(new ItemStack(item));
            fixture.inventory.get().setItem(0, new ItemStack(Items.DIAMOND, 2));
            assertTrue(start(), item.toString());
        }
    }

    @Test void onlyCopperChestsWithSomethingInsideAreVisited() {
        build(new ItemStack(Items.CHEST));
        fixture.inventory.get().setItem(0, new ItemStack(Items.DIAMOND));
        assertFalse(start(), "a wooden chest is not a copper chest");
        build(new ItemStack(Items.ENDER_CHEST));
        assertFalse(start());
        build(new ItemStack(Items.COPPER_CHEST));
        assertFalse(start(), "an empty chest is not worth the trip");
    }

    @Test void theGolemLeavesBusyOrDisabledSituationsAlone() {
        build(new ItemStack(Items.COPPER_CHEST));
        fixture.inventory.get().setItem(0, new ItemStack(Items.DIAMOND));

        hand.set(new ItemStack(Items.STICK));
        assertFalse(start(), "a golem already carrying something goes to deliver it");
        hand.set(ItemStack.EMPTY);

        fixture.inventory.get().startOpen(fixture.ownerNearby());
        assertFalse(start(), "somebody is looking inside");
        fixture.state.reset(fixture.pet);

        CCConfig.copperGolemsUsePetChests.set(false);
        assertFalse(start(), "disabled in the configuration");
        CCConfig.copperGolemsUsePetChests.set(true);

        when(((IPetPickupGolem) golem).chestedCompanions$isTransportingBetweenChests()).thenReturn(true);
        assertFalse(start(), "it is already on its way to a placed chest");
        when(((IPetPickupGolem) golem).chestedCompanions$isTransportingBetweenChests()).thenReturn(false);

        PetFixture.setBox(fixture.pet, new AABB(40, 0, 0, 40.6, 0.7, 0.6));
        assertFalse(start(), "too far away");
    }

    @Test void aPetThatWalksAwayInterruptsTheVisitWithoutLosingAnything() {
        build(new ItemStack(Items.COPPER_CHEST));
        fixture.inventory.get().setItem(0, new ItemStack(Items.DIAMOND, 5));
        assertTrue(start());
        tick(20);
        assertTrue(fixture.state.isOpen());
        PetFixture.setBox(golem, new AABB(10, 0, 0, 10.6, 0.9, 0.6));
        tick(1);
        assertFalse(fixture.state.isOpen(), "the chest closes if the pet gets away");
        assertTrue(hand.get().isEmpty());
        assertEquals(5, diamonds());
    }

    @Test void removingTheChestDuringTheVisitEndsItAtOnce() {
        build(new ItemStack(Items.COPPER_CHEST));
        fixture.inventory.get().setItem(0, new ItemStack(Items.DIAMOND, 5));
        assertTrue(start());
        tick(10);
        assertTrue(fixture.state.isOpen());
        me.khajiitos.chestedcompanions.common.util.ChestEntityCommon.removeChestContent(fixture.pet, true);
        tick(1);
        assertEquals(Behavior.Status.STOPPED, behavior.getStatus());
        assertFalse(fixture.state.isOpen());
        assertTrue(hand.get().isEmpty(), "the golem leaves empty handed");
        assertEquals(0, fixture.inventory.get() == null ? 0 : diamonds());
    }

    @Test void aPetThatIsNotReachedInTimeIsSkippedForAWhile() {
        build(new ItemStack(Items.COPPER_CHEST));
        fixture.inventory.get().setItem(0, new ItemStack(Items.DIAMOND));
        PetFixture.setBox(golem, new AABB(8, 0, 0, 8.6, 0.9, 0.6));
        assertTrue(start());
        tick(700);
        assertEquals(Behavior.Status.STOPPED, behavior.getStatus());
        assertFalse(start(), "the unreachable pet is remembered for a few minutes");
        time.addAndGet(6001);
        assertTrue(start());
    }

    @Test void ownersCanStillOpenTheChestWhileNoGolemIsAround() {
        build(new ItemStack(Items.COPPER_CHEST));
        Player owner = fixture.ownerNearby();
        fixture.inventory.get().startOpen(owner);
        assertTrue(fixture.state.isOpen());
        fixture.inventory.get().stopOpen(owner);
        assertFalse(fixture.state.isOpen());
    }
}
