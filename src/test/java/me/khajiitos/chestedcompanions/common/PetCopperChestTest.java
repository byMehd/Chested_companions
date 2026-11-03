package me.khajiitos.chestedcompanions.common;

import me.khajiitos.chestedcompanions.common.util.PetCopperChest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PetCopperChestTest {
    static {
        TestBootstrap.run();
    }

    private static final List<Item> UNWAXED = List.of(Items.COPPER_CHEST, Items.EXPOSED_COPPER_CHEST,
            Items.WEATHERED_COPPER_CHEST, Items.OXIDIZED_COPPER_CHEST);
    private static final List<Item> WAXED = List.of(Items.WAXED_COPPER_CHEST, Items.WAXED_EXPOSED_COPPER_CHEST,
            Items.WAXED_WEATHERED_COPPER_CHEST, Items.WAXED_OXIDIZED_COPPER_CHEST);

    private static Player player(boolean creative) {
        Player player = mock(Player.class);
        Abilities abilities = new Abilities();
        abilities.instabuild = creative;
        when(player.getAbilities()).thenReturn(abilities);
        return player;
    }

    @Test void onlyCopperChestsAreCopper() {
        UNWAXED.forEach(item -> assertTrue(PetCopperChest.isCopper(new ItemStack(item))));
        WAXED.forEach(item -> assertTrue(PetCopperChest.isCopper(new ItemStack(item))));
        assertFalse(PetCopperChest.isCopper(new ItemStack(Items.CHEST)));
        assertFalse(PetCopperChest.isCopper(new ItemStack(Items.ENDER_CHEST)));
        assertFalse(PetCopperChest.isCopper(new ItemStack(Items.COPPER_BLOCK)));
        assertFalse(PetCopperChest.isCopper(ItemStack.EMPTY));
    }

    @Test void oxidisationFollowsVanillaStagesAndStopsAtTheLastOne() {
        for (int stage = 0; stage < 3; stage++) {
            assertEquals(UNWAXED.get(stage + 1), PetCopperChest.next(new ItemStack(UNWAXED.get(stage))).orElseThrow());
            assertEquals(UNWAXED.get(stage), PetCopperChest.previous(new ItemStack(UNWAXED.get(stage + 1))).orElseThrow());
        }
        assertTrue(PetCopperChest.next(new ItemStack(Items.OXIDIZED_COPPER_CHEST)).isEmpty());
        assertTrue(PetCopperChest.previous(new ItemStack(Items.COPPER_CHEST)).isEmpty());
    }

    @Test void waxedChestsNeverAgeAndCannotBeScraped() {
        for (Item item : WAXED) {
            assertTrue(PetCopperChest.next(new ItemStack(item)).isEmpty());
            assertTrue(PetCopperChest.previous(new ItemStack(item)).isEmpty());
            assertTrue(PetCopperChest.waxed(new ItemStack(item)).isEmpty());
            assertEquals(0.0F, PetCopperChest.agingChancePerTick(3, new ItemStack(item)));
        }
        for (int stage = 0; stage < 4; stage++) {
            assertEquals(WAXED.get(stage), PetCopperChest.waxed(new ItemStack(UNWAXED.get(stage))).orElseThrow());
            assertEquals(UNWAXED.get(stage), PetCopperChest.unwaxed(new ItemStack(WAXED.get(stage))).orElseThrow());
            assertTrue(PetCopperChest.unwaxed(new ItemStack(UNWAXED.get(stage))).isEmpty());
        }
    }

    @Test void agingChanceIsTheChancePerTickOfAPlacedIsolatedChest() {
        float randomTickPick = 3.0F / 4096.0F;
        float vanilla = 0.05688889F;
        assertEquals(randomTickPick * vanilla * 0.75F, PetCopperChest.agingChancePerTick(3, new ItemStack(Items.COPPER_CHEST)), 1.0E-9F);
        assertEquals(randomTickPick * vanilla, PetCopperChest.agingChancePerTick(3, new ItemStack(Items.EXPOSED_COPPER_CHEST)), 1.0E-9F);
        assertEquals(randomTickPick * vanilla, PetCopperChest.agingChancePerTick(3, new ItemStack(Items.WEATHERED_COPPER_CHEST)), 1.0E-9F);
        assertEquals(0.0F, PetCopperChest.agingChancePerTick(0, new ItemStack(Items.COPPER_CHEST)), "randomTickSpeed 0 stops oxidisation");
        assertEquals(0.0F, PetCopperChest.agingChancePerTick(3, new ItemStack(Items.CHEST)));
        // A fresh chest takes about 32000 ticks (27 min) to its next stage, the others about 24000 (20 min), as placed blocks do.
        assertEquals(32000.0, 1.0 / PetCopperChest.agingChancePerTick(3, new ItemStack(Items.COPPER_CHEST)), 20.0);
        assertEquals(24000.0, 1.0 / PetCopperChest.agingChancePerTick(3, new ItemStack(Items.EXPOSED_COPPER_CHEST)), 15.0);
    }

    @Test void agingTickOxidisesWhenTheRollSucceedsAndKeepsComponents() {
        PetFixture fixture = new PetFixture(new ItemStack(Items.EXPOSED_COPPER_CHEST), 9);
        fixture.chest.get().set(DataComponents.CUSTOM_NAME, Component.literal("My chest"));
        GameRules rules = mock(GameRules.class);
        //? if >=1.21.11 {
        when(rules.get(GameRules.RANDOM_TICK_SPEED)).thenReturn(3);
        //?} else {
        /*when(rules.getInt(GameRules.RULE_RANDOMTICKING)).thenReturn(3);
        *///?}
        when(fixture.level.getGameRules()).thenReturn(rules);
        RandomSource lucky = mock(RandomSource.class);
        when(lucky.nextFloat()).thenReturn(0.0F);
        when(fixture.level.getRandom()).thenReturn(lucky);
        PetCopperChest.tickAging(fixture.pet, fixture.level);
        assertTrue(fixture.chest.get().is(Items.WEATHERED_COPPER_CHEST));
        assertEquals(Component.literal("My chest"), fixture.chest.get().get(DataComponents.CUSTOM_NAME));
        assertEquals(1, fixture.chest.get().getCount());

        when(lucky.nextFloat()).thenReturn(0.5F);
        PetCopperChest.tickAging(fixture.pet, fixture.level);
        assertTrue(fixture.chest.get().is(Items.WEATHERED_COPPER_CHEST), "An unlucky roll leaves the chest alone");
    }

    @Test void honeycombWaxesTheChestAndIsConsumed() {
        PetFixture fixture = new PetFixture(new ItemStack(Items.WEATHERED_COPPER_CHEST), 9);
        Player player = player(false);
        ItemStack honeycomb = new ItemStack(Items.HONEYCOMB, 3);
        assertTrue(PetCopperChest.tryUse(fixture.pet, player, net.minecraft.world.InteractionHand.MAIN_HAND, honeycomb));
        assertTrue(fixture.chest.get().is(Items.WAXED_WEATHERED_COPPER_CHEST));
        assertEquals(2, honeycomb.getCount());
        verify(fixture.level).levelEvent(isNull(), eq(3003), any(), eq(0));
        assertFalse(PetCopperChest.tryUse(fixture.pet, player, net.minecraft.world.InteractionHand.MAIN_HAND, honeycomb),
                "A waxed chest cannot be waxed again, so the click falls through to opening it");
        assertEquals(2, honeycomb.getCount());
    }

    @Test void honeycombIsFreeInCreative() {
        PetFixture fixture = new PetFixture(new ItemStack(Items.COPPER_CHEST), 9);
        ItemStack honeycomb = new ItemStack(Items.HONEYCOMB, 1);
        assertTrue(PetCopperChest.tryUse(fixture.pet, player(true), net.minecraft.world.InteractionHand.MAIN_HAND, honeycomb));
        assertEquals(1, honeycomb.getCount());
    }

    @Test void axeScrapesOneStageThenRemovesWaxFromWaxedChests() {
        PetFixture fixture = new PetFixture(new ItemStack(Items.OXIDIZED_COPPER_CHEST), 9);
        Player player = player(false);
        when(player.level()).thenReturn(fixture.level);
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        assertTrue(PetCopperChest.tryUse(fixture.pet, player, net.minecraft.world.InteractionHand.MAIN_HAND, axe));
        assertTrue(fixture.chest.get().is(Items.WEATHERED_COPPER_CHEST));
        verify(fixture.level).playSound(isNull(), eq(fixture.pet), eq(SoundEvents.AXE_SCRAPE), any(), anyFloat(), anyFloat());
        verify(fixture.level).levelEvent(isNull(), eq(3005), any(), eq(0));
        assertEquals(1, axe.getDamageValue(), "The axe loses one durability point per use");

        fixture.chest.set(new ItemStack(Items.WAXED_OXIDIZED_COPPER_CHEST));
        assertTrue(PetCopperChest.tryUse(fixture.pet, player, net.minecraft.world.InteractionHand.MAIN_HAND, axe));
        assertTrue(fixture.chest.get().is(Items.OXIDIZED_COPPER_CHEST), "Wax comes off first and the oxidation is kept");
        verify(fixture.level).playSound(isNull(), eq(fixture.pet), eq(SoundEvents.AXE_WAX_OFF), any(), anyFloat(), anyFloat());
        verify(fixture.level).levelEvent(isNull(), eq(3004), any(), eq(0));
    }

    @Test void axeOnAPristineUnwaxedChestDoesNothing() {
        PetFixture fixture = new PetFixture(new ItemStack(Items.COPPER_CHEST), 9);
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        assertFalse(PetCopperChest.tryUse(fixture.pet, player(false), net.minecraft.world.InteractionHand.MAIN_HAND, axe));
        assertTrue(fixture.chest.get().is(Items.COPPER_CHEST));
        assertEquals(0, axe.getDamageValue());
    }

    @Test void otherItemsAndChestsAreLeftAlone() {
        PetFixture wooden = new PetFixture(new ItemStack(Items.CHEST), 9);
        assertFalse(PetCopperChest.tryUse(wooden.pet, player(false), net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.HONEYCOMB)));
        assertFalse(PetCopperChest.tryUse(wooden.pet, player(false), net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE)));
        PetFixture copper = new PetFixture(new ItemStack(Items.COPPER_CHEST), 9);
        assertFalse(PetCopperChest.tryUse(copper.pet, player(false), net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.STICK)));
        assertFalse(PetCopperChest.tryUse(copper.pet, player(false), net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY));
    }

    @Test void contentsSurviveEveryChange() {
        PetFixture fixture = new PetFixture(new ItemStack(Items.OXIDIZED_COPPER_CHEST), 9);
        fixture.inventory.get().setItem(0, new ItemStack(Items.DIAMOND, 7));
        Player player = player(false);
        PetCopperChest.tryUse(fixture.pet, player, net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        PetCopperChest.tryUse(fixture.pet, player, net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.HONEYCOMB));
        assertSame(fixture.inventory.get(), fixture.pet.chestedCompanions$getInventory());
        assertEquals(7, fixture.inventory.get().getItem(0).getCount());
        verify(fixture.pet, never()).spawnAtLocation(any(), any(ItemStack.class));
    }
}
