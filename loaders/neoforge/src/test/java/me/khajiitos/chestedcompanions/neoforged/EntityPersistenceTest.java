package me.khajiitos.chestedcompanions.neoforged;

import me.khajiitos.chestedcompanions.common.config.CCConfig;
import me.khajiitos.chestedcompanions.common.util.IChestEntity;
import me.khajiitos.chestedcompanions.common.util.InventoryCapacity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real mixins and codecs, with a registry-only ephemeral server; no world chunks are modified. */
@ExtendWith(EphemeralTestServerProvider.class)
class EntityPersistenceTest {
    @Test void chestTagContainsVanillaEnderAndEveryInstalledVariant(MinecraftServer server) {
        var tag = me.khajiitos.chestedcompanions.common.ChestedCompanions.PET_CHEST_ITEM;
        assertTrue(new ItemStack(Items.CHEST).is(tag));
        assertTrue(new ItemStack(Items.ENDER_CHEST).is(tag));
        for (String variant : java.util.List.of("copper", "exposed_copper", "weathered_copper", "oxidized_copper",
                "waxed_copper", "waxed_exposed_copper", "waxed_weathered_copper", "waxed_oxidized_copper")) {
            var id = net.minecraft.resources.Identifier.withDefaultNamespace(variant + "_chest");
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id);
            assertNotEquals(Items.AIR, item, "Missing vanilla copper chest item " + id);
            assertTrue(new ItemStack(item).is(tag), "Copper chest rejected by pet tag: " + id);
        }
        if (net.neoforged.fml.ModList.get().isLoaded("lolmcv")) {
            for (String wood : java.util.List.of("oak", "birch", "spruce", "jungle", "dark_oak", "acacia",
                    "mangrove", "cherry", "bamboo", "crimson", "warped", "pale_oak")) {
                var id = net.minecraft.resources.Identifier.fromNamespaceAndPath("lolmcv", wood + "_chest");
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id);
                assertNotEquals(Items.AIR, item, "Missing registered MCV item " + id);
                assertTrue(new ItemStack(item).is(tag), "MCV chest rejected by pet tag: " + id);
            }
        }
    }

    private TamableAnimal create(EntityType<? extends TamableAnimal> type, MinecraftServer server) {
        // The ephemeral server supplies registries, not a tickable world. Detached entities
        // use a minimal level and must never attempt chunk access or network tracking.
        var level = mock(net.minecraft.world.level.Level.class);
        when(level.registryAccess()).thenReturn(server.registryAccess());
        when(level.enabledFeatures()).thenReturn(net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS);
        when(level.dimension()).thenReturn(net.minecraft.world.level.Level.OVERWORLD);
        when(level.isClientSide()).thenReturn(true);
        return type.create(level, EntitySpawnReason.COMMAND);
    }

    @Test void catsAndWolvesRoundTripChestComponentsAndAllSlots(MinecraftServer server) {
        CCConfig.catInventoryCapacity.set(InventoryCapacity.THREE_ROWS);
        CCConfig.wolfInventoryCapacity.set(InventoryCapacity.THREE_ROWS);
        try {
            for (var type : java.util.List.of(EntityType.CAT, EntityType.WOLF)) {
                TamableAnimal animal = create(type, server);
                IChestEntity pet = (IChestEntity)animal;
                ItemStack chest = new ItemStack(Items.CHEST);
                chest.set(DataComponents.CUSTOM_NAME, Component.literal("Migration chest"));
                pet.chestedCompanions$setChestItemStack(chest);
                pet.chestedCompanions$createInventory();
                pet.chestedCompanions$getInventory().setItem(26, new ItemStack(Items.DIAMOND, 7));
                var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, server.registryAccess());
                animal.saveWithoutId(output);
                CompoundTag saved = output.buildResult();
                assertTrue(saved.contains("ChestItem"));
                assertTrue(saved.contains("CCItems"));
                TamableAnimal loaded = create(type, server);
                loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, server.registryAccess(), saved));
                IChestEntity restored = (IChestEntity)loaded;
                assertTrue(restored.chestedCompanions$hasChest());
                assertEquals(Component.literal("Migration chest"), restored.chestedCompanions$getChestItemStack().get(DataComponents.CUSTOM_NAME));
                assertEquals(7, restored.chestedCompanions$getInventory().getItem(26).getCount());
                assertTrue(restored.chestedCompanions$getInventory().getItem(26).is(Items.DIAMOND));
            }
        } finally {
            CCConfig.catInventoryCapacity.set(InventoryCapacity.ONE_ROW);
            CCConfig.wolfInventoryCapacity.set(InventoryCapacity.ONE_ROW);
        }
    }

    @Test void enderChestSavesOnlyEquipmentAndNeverPetContents(MinecraftServer server) {
        for (var type : java.util.List.of(EntityType.CAT, EntityType.WOLF)) {
            TamableAnimal animal = create(type, server);
            IChestEntity pet = (IChestEntity)animal;
            pet.chestedCompanions$setChestItemStack(new ItemStack(Items.ENDER_CHEST));
            pet.chestedCompanions$createInventory();
            assertNull(pet.chestedCompanions$getInventory());
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, server.registryAccess());
            animal.saveWithoutId(output);
            assertFalse(output.buildResult().contains("CCItems"));
            TamableAnimal loaded = create(type, server);
            loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, server.registryAccess(), output.buildResult()));
            assertTrue(((IChestEntity)loaded).chestedCompanions$hasEnderChest());
            assertNull(((IChestEntity)loaded).chestedCompanions$getInventory());
        }
    }

    @Test void oldHasChestSavesStillMigrate(MinecraftServer server) {
        CompoundTag legacy = new CompoundTag();
        legacy.putBoolean("HasChest", true);
        for (var type : java.util.List.of(EntityType.CAT, EntityType.WOLF)) {
            TamableAnimal loaded = create(type, server);
            loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, server.registryAccess(), legacy));
            assertTrue(((IChestEntity)loaded).chestedCompanions$getChestItemStack().is(Items.CHEST));
            assertNotNull(((IChestEntity)loaded).chestedCompanions$getInventory());
        }
    }
}
