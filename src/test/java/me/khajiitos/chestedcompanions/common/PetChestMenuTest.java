package me.khajiitos.chestedcompanions.common;

import me.khajiitos.chestedcompanions.common.config.CCConfig;
import me.khajiitos.chestedcompanions.common.util.PetChestMenu;
import me.khajiitos.chestedcompanions.common.util.PetEnderChestContainer;
import me.khajiitos.chestedcompanions.common.util.PetMenuProvider;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** The button of the pet's screen takes the chest off like shears do, and only for somebody who may open it. */
class PetChestMenuTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach void privateChests() {
        CCConfig.publicChest.set(false);
    }

    private static PetChestMenu<PetFixture.Pet> menu(PetFixture fixture, Player viewer, boolean ender) {
        Inventory inventory = new Inventory(viewer, new EntityEquipment());
        return ender
                ? new PetChestMenu<>(MenuType.GENERIC_9x3, 1, inventory, new PetEnderChestContainer<>(fixture.pet, viewer), 3, fixture.pet)
                : new PetChestMenu<>(MenuType.GENERIC_9x1, 1, inventory, fixture.inventory.get(), 1, fixture.pet);
    }

    private static Player survivalOwner(PetFixture fixture) {
        Player owner = fixture.ownerNearby();
        when(owner.getAbilities()).thenReturn(new Abilities());
        return owner;
    }

    @Test void theButtonDropsTheChestAndEverythingInIt() {
        PetFixture fixture = new PetFixture(new ItemStack(Items.CHEST), 9);
        fixture.inventory.get().setItem(0, new ItemStack(Items.DIAMOND, 7));
        Player owner = survivalOwner(fixture);
        var menu = menu(fixture, owner, false);

        assertTrue(menu.clickMenuButton(owner, PetChestMenu.REMOVE_CHEST_BUTTON));

        assertTrue(fixture.chest.get().isEmpty());
        verify(fixture.pet, times(1)).spawnAtLocation(eq(fixture.level), argThat((ItemStack s) -> s.is(Items.DIAMOND) && s.getCount() == 7), eq(0.25F));
        verify(fixture.pet, times(1)).spawnAtLocation(eq(fixture.level), argThat((ItemStack s) -> s.is(Items.CHEST)), eq(0.25F));
        verify(fixture.pet).playSound(SoundEvents.DONKEY_CHEST, 1.0F, 1.5F);
    }

    @Test void anEmptyChestComesOffToo() {
        PetFixture fixture = new PetFixture(new ItemStack(Items.COPPER_CHEST), 9);
        Player owner = survivalOwner(fixture);
        menu(fixture, owner, false).clickMenuButton(owner, PetChestMenu.REMOVE_CHEST_BUTTON);
        assertTrue(fixture.chest.get().isEmpty());
        verify(fixture.pet, times(1)).spawnAtLocation(eq(fixture.level), argThat((ItemStack s) -> s.is(Items.COPPER_CHEST)), eq(0.25F));
    }

    @Test void creativePlayersGetTheChestBackToo() {
        PetFixture fixture = new PetFixture(new ItemStack(Items.CHEST), 9);
        fixture.inventory.get().setItem(3, new ItemStack(Items.EMERALD, 2));
        Player owner = fixture.ownerNearby();
        Abilities abilities = new Abilities();
        abilities.instabuild = true;
        when(owner.getAbilities()).thenReturn(abilities);

        menu(fixture, owner, false).clickMenuButton(owner, PetChestMenu.REMOVE_CHEST_BUTTON);

        assertTrue(fixture.chest.get().isEmpty());
        verify(fixture.pet, times(1)).spawnAtLocation(eq(fixture.level), argThat((ItemStack s) -> s.is(Items.EMERALD)), eq(0.25F));
        verify(fixture.pet, times(1)).spawnAtLocation(eq(fixture.level), argThat((ItemStack s) -> s.is(Items.CHEST)), eq(0.25F));
    }

    @Test void somebodyWithoutAccessCannotUseTheButton() {
        PetFixture fixture = new PetFixture(new ItemStack(Items.CHEST), 9);
        fixture.inventory.get().setItem(0, new ItemStack(Items.DIAMOND));
        Player stranger = mock(Player.class);
        when(stranger.level()).thenReturn(fixture.level);
        when(stranger.getLivingEntity()).thenReturn(stranger);
        when(stranger.isWithinEntityInteractionRange(fixture.pet, 4.0)).thenReturn(true);
        when(stranger.getAbilities()).thenReturn(new Abilities());

        menu(fixture, stranger, false).clickMenuButton(stranger, PetChestMenu.REMOVE_CHEST_BUTTON);

        assertFalse(fixture.chest.get().isEmpty());
        verify(fixture.pet, never()).spawnAtLocation(any(), any(ItemStack.class), anyFloat());
    }

    @Test void aPlayerWhoWalkedAwayCannotUseTheButton() {
        PetFixture fixture = new PetFixture(new ItemStack(Items.CHEST), 9);
        Player owner = survivalOwner(fixture);
        var menu = menu(fixture, owner, false);
        when(owner.isWithinEntityInteractionRange(fixture.pet, 4.0)).thenReturn(false);
        menu.clickMenuButton(owner, PetChestMenu.REMOVE_CHEST_BUTTON);
        assertFalse(fixture.chest.get().isEmpty());
    }

    @Test void otherButtonsAreLeftToVanilla() {
        PetFixture fixture = new PetFixture(new ItemStack(Items.CHEST), 9);
        Player owner = survivalOwner(fixture);
        assertFalse(menu(fixture, owner, false).clickMenuButton(owner, 99));
        assertFalse(fixture.chest.get().isEmpty());
    }

    @Test void anEnderChestComesOffWithoutTouchingTheViewersEnderInventory() {
        PetFixture fixture = new PetFixture(new ItemStack(Items.ENDER_CHEST), 0);
        Player owner = survivalOwner(fixture);
        PlayerEnderChestContainer ender = new PlayerEnderChestContainer();
        ender.setItem(0, new ItemStack(Items.DIAMOND, 3));
        when(owner.getEnderChestInventory()).thenReturn(ender);

        menu(fixture, owner, true).clickMenuButton(owner, PetChestMenu.REMOVE_CHEST_BUTTON);

        assertTrue(fixture.chest.get().isEmpty());
        verify(fixture.pet, times(1)).spawnAtLocation(eq(fixture.level), argThat((ItemStack s) -> s.is(Items.ENDER_CHEST)), eq(0.25F));
        verify(fixture.pet, never()).spawnAtLocation(eq(fixture.level), argThat((ItemStack s) -> s.is(Items.DIAMOND)), anyFloat());
        assertEquals(3, ender.getItem(0).getCount(), "Ender contents never belong to the pet");
    }

    @Test void theScreenTitleNamesThePetAndItsChestLikeAVanillaChestScreen() {
        for (var chest : java.util.List.of(Items.CHEST, Items.ENDER_CHEST, Items.COPPER_CHEST)) {
            PetFixture fixture = new PetFixture(new ItemStack(chest), 9);
            when(fixture.pet.getDisplayName()).thenReturn(net.minecraft.network.chat.Component.literal("Misty"));
            var title = new PetMenuProvider<>(fixture.pet).getDisplayName();
            assertTrue(title.getContents() instanceof TranslatableContents contents && contents.getKey().equals(PetMenuProvider.TITLE_KEY));
            Object[] args = ((TranslatableContents) title.getContents()).getArgs();
            assertEquals(2, args.length);
            assertEquals("Misty", ((net.minecraft.network.chat.Component) args[0]).getString());
            assertEquals(new ItemStack(chest).getItemName(), args[1]);
        }
    }
}
