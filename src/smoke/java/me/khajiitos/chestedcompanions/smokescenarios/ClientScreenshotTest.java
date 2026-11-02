package me.khajiitos.chestedcompanions.smokescenarios;

import me.khajiitos.chestedcompanions.common.util.IChestEntity;
import me.khajiitos.chestedcompanions.common.util.PetChestState;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Real-client check of the rendering: every chest type on a cat and on a wolf, seen from both flanks, with the lid
 * shut and open, plus a copper golem caught in the middle of emptying a pet's chest. Screenshots land in the run
 * folder's {@code screenshots} directory; the scenario fails if a pet never appears or the golem never opens the chest.
 * Run with {@code -Dfabric.client.gametest} and Fabric API present; any resource packs or mods installed in the run
 * folder are rendered too, which is how third-party combinations are inspected.
 */
public final class ClientScreenshotTest implements FabricClientGameTest {
    private static final Map<String, Item> CHESTS = new LinkedHashMap<>();
    static {
        CHESTS.put("chest", Items.CHEST);
        CHESTS.put("ender", Items.ENDER_CHEST);
        CHESTS.put("copper", Items.COPPER_CHEST);
        CHESTS.put("exposed", Items.EXPOSED_COPPER_CHEST);
        CHESTS.put("weathered", Items.WEATHERED_COPPER_CHEST);
        CHESTS.put("oxidized", Items.OXIDIZED_COPPER_CHEST);
    }

    private static final String PREFIX = System.getProperty("revived.screens.prefix", "");

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            client.options.hideGui = true;
            client.options.fov().set(40);
        });
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            world.getServer().runCommand("gamemode spectator @p");
            int groundY = world.getServer().computeOnServer(server -> server.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0));
            for (boolean wolf : new boolean[]{false, true}) {
                for (Map.Entry<String, Item> chest : CHESTS.entrySet()) {
                    String pet = (wolf ? "wolf_" : "cat_") + chest.getKey();
                    UUID uuid = spawnPet(world, groundY, wolf, chest.getValue());
                    context.waitTicks(15);
                    for (int side : new int[]{1, -1}) {
                        String flank = side == 1 ? "a" : "b";
                        look(world, groundY, side);
                        context.waitTicks(8);
                        context.takeScreenshot(PREFIX + pet + "_" + flank + "_closed");
                        setLid(world, uuid, true);
                        context.waitTicks(14);
                        context.takeScreenshot(PREFIX + pet + "_" + flank + "_open");
                        setLid(world, uuid, false);
                        context.waitTicks(14);
                    }
                    discard(world, uuid);
                }
            }
            sittingPets(context, world, groundY);
            golemVisit(context, world, groundY);
        }
    }

    private static UUID spawnPet(TestSingleplayerContext world, int groundY, boolean wolf, Item chest) {
        return world.getServer().computeOnServer(server -> {
            ServerLevel level = server.overworld();
            TamableAnimal pet = (TamableAnimal) (wolf ? EntityType.WOLF : EntityType.CAT)
                    .spawn(level, new BlockPos(0, groundY, 0), EntitySpawnReason.COMMAND);
            pet.setTame(true, false);
            pet.setNoAi(true);
            pet.snapTo(0.5, groundY, 0.5, 0.0F, 0.0F);
            pet.setYHeadRot(0.0F);
            pet.setYBodyRot(0.0F);
            IChestEntity chested = (IChestEntity) pet;
            chested.chestedCompanions$setChestItemStack(new ItemStack(chest));
            chested.chestedCompanions$createInventory();
            return pet.getUUID();
        });
    }

    /** Puts the (invisible, spectating) camera a few blocks from the pet's left or right flank, at the pet's own height. */
    private static void look(TestSingleplayerContext world, int groundY, int side) {
        // The teleport sets the feet position; a spectator's eyes are 1.62 blocks above it.
        world.getServer().runCommand("tp @p " + (0.5 + 2.6 * side) + " " + (groundY - 0.9) + " 0.5 " + (side == 1 ? 90 : -90) + " 8");
    }

    private static void setLid(TestSingleplayerContext world, UUID uuid, boolean open) {
        world.getServer().runOnServer(server -> {
            var pet = server.overworld().getEntity(uuid);
            server.overworld().broadcastEntityEvent(pet, open ? PetChestState.EVENT_OPEN : PetChestState.EVENT_CLOSE);
        });
    }

    private static void discard(TestSingleplayerContext world, UUID uuid) {
        world.getServer().runOnServer(server -> server.overworld().getEntity(uuid).discard());
    }

    /** Sitting cats lie differently on the model; this is the pose the position hack for the cat body targets. */
    private static void sittingPets(ClientGameTestContext context, TestSingleplayerContext world, int groundY) {
        for (boolean wolf : new boolean[]{false, true}) {
            UUID uuid = spawnPet(world, groundY, wolf, Items.COPPER_CHEST);
            world.getServer().runOnServer(server -> ((TamableAnimal) server.overworld().getEntity(uuid)).setInSittingPose(true));
            context.waitTicks(15);
            look(world, groundY, 1);
            context.waitTicks(8);
            context.takeScreenshot(PREFIX + (wolf ? "wolf" : "cat") + "_copper_sitting_a_closed");
            setLid(world, uuid, true);
            context.waitTicks(14);
            context.takeScreenshot(PREFIX + (wolf ? "wolf" : "cat") + "_copper_sitting_a_open");
            discard(world, uuid);
        }
    }

    /** A copper golem walks up to a cat wearing a copper chest and, mid-visit, the lid is open and the pet has not moved. */
    private static void golemVisit(ClientGameTestContext context, TestSingleplayerContext world, int groundY) {
        UUID cat = world.getServer().computeOnServer(server -> {
            ServerLevel level = server.overworld();
            TamableAnimal pet = EntityType.CAT.spawn(level, new BlockPos(0, groundY, 0), EntitySpawnReason.COMMAND);
            pet.setTame(true, false);
            pet.setOrderedToSit(true);
            IChestEntity chested = (IChestEntity) pet;
            chested.chestedCompanions$setChestItemStack(new ItemStack(Items.COPPER_CHEST));
            chested.chestedCompanions$createInventory();
            chested.chestedCompanions$getInventory().setItem(0, new ItemStack(Items.DIAMOND, 5));
            EntityType.COPPER_GOLEM.spawn(level, new BlockPos(-5, groundY, 0), EntitySpawnReason.COMMAND);
            return pet.getUUID();
        });
        world.getServer().runCommand("tp @p -0.5 " + (groundY - 0.4) + " 4.5 180 10");
        for (int tick = 0; tick < 900; tick++) {
            context.waitTick();
            boolean open = world.getServer().computeOnServer(server ->
                    ((IChestEntity) server.overworld().getEntity(cat)).chestedCompanions$getState().isOpen());
            if (open) {
                context.waitTicks(30);
                context.takeScreenshot(PREFIX + "golem_visit_open");
                context.waitTicks(20);
                context.takeScreenshot(PREFIX + "golem_visit_later");
                return;
            }
        }
        throw new AssertionError("REVIVED_CLIENT_SCREENSHOTS_FAILED: the golem never opened the pet's chest");
    }
}
