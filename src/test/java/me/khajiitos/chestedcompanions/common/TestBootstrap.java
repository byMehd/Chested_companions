package me.khajiitos.chestedcompanions.common;

//? if >=26.1 {
/*import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.item.Items;
*///?}
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/** Starts the game's registries the way every test that builds an item or an entity needs them. */
final class TestBootstrap {
    private TestBootstrap() {
    }

    static void run() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        //? if >=26.1 {
        /*// Since 26.1 the default components of the items are attached when a world's data is loaded, not by the bootstrap.
        if (!Items.CHEST.builtInRegistryHolder().areComponentsBound()) {
            // NeoForge checks in a development environment that the components of items can be compared, which the
            // tag sets of a registry without any data cannot; the check is of no use to these tests.
            boolean inIde = SharedConstants.IS_RUNNING_IN_IDE;
            SharedConstants.IS_RUNNING_IN_IDE = false;
            try {
                BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup())
                        .forEach(DataComponentInitializers.PendingComponents::apply);
            } finally {
                SharedConstants.IS_RUNNING_IN_IDE = inIde;
            }
        }
        *///?}
    }
}
