package me.khajiitos.chestedcompanions.smokescenarios;

import me.khajiitos.chestedcompanions.common.util.IChestEntity;
import me.khajiitos.chestedcompanions.common.util.PetChestState;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import me.khajiitos.chestedcompanions.common.client.gui.PetRemoveChestButton;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;

import com.mojang.blaze3d.platform.InputConstants;

import me.khajiitos.chestedcompanions.common.util.IChestEntityRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Locale;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Real-client check of the rendering and of the pet's chest screen: every chest type on a cat and on a wolf, seen from
 * both flanks, with the lid shut and open; sitting and baby pets; the chests of More Chest Variants when it is
 * installed; a copper golem caught in the middle of emptying a pet's chest; and the pet's screen, where the
 * remove-chest button is hovered and really clicked. Screenshots land in the run folder's {@code screenshots}
 * directory; the scenario fails if a pet never appears, the golem never opens the chest or the button does not
 * leave the chest and its contents on the ground.
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

    /** {@code -Drevived.screens.only=cats,wolves,sitting,baby,mcv,golem,gui} runs just those parts of the scenario. */
    private static boolean wanted(String part) {
        String only = System.getProperty("revived.screens.only", "");
        return only.isEmpty() || Arrays.asList(only.split(",")).contains(part);
    }

    /** Parts that are left out of the complete run, because they take many screenshots: {@code zfight} and {@code zfightadults}. */
    private static boolean requested(String part) {
        return Arrays.asList(System.getProperty("revived.screens.only", "").split(",")).contains(part);
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            String pack = System.getProperty("revived.screens.pack", "");
            if (!pack.isEmpty()) {
                var loader = net.fabricmc.loader.api.FabricLoader.getInstance();
                if (!loader.isModLoaded("entity_model_features") || !loader.isModLoaded("entity_texture_features"))
                    throw new AssertionError("Model-pack rendering requires EMF and ETF");
                var model = client.getResourceManager().getResource(
                        Identifier.fromNamespaceAndPath("minecraft", "optifine/cem/cat.jem")).orElseThrow();
                String expectedPack = pack.equals("fatcat") ? "fat-cat-test.zip" : "freshanimations-test.zip";
                if (!model.sourcePackId().equals("file/" + expectedPack))
                    throw new AssertionError("The selected model pack must be active before taking screenshots");
                //? if >=26.1 {
                /*if (pack.equals("freshanimations")) {
                    for (String baby : java.util.List.of("cat_baby", "wolf_baby")) {
                        var resource = client.getResourceManager().getResource(Identifier.fromNamespaceAndPath(
                                "minecraft", "optifine/cem/" + baby + ".jem")).orElseThrow();
                        if (!resource.sourcePackId().equals("file/" + expectedPack))
                            throw new AssertionError("Fresh Animations must supply its Minecraft 26 baby models");
                    }
                }
                *///?}
                System.out.println("REVIVED_MODEL_PACK_ACTIVE: " + pack + ", original model and textures, test-only pack metadata");
            }
            client.options.hideGui = true;
            client.options.fov().set(40);
            // A short render distance: on a small CI runner the client's chunk meshing starves the world loading, which
            // waits for the client to acknowledge its chunks, until the gametest gives up. The scenes are all close up.
            client.options.renderDistance().set(3);
            client.options.simulationDistance().set(5);
        });
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            world.getServer().runCommand("gamemode spectator @p");
            int groundY = world.getServer().computeOnServer(server -> server.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0));
            for (boolean wolf : new boolean[]{false, true}) {
                if (!wanted(wolf ? "wolves" : "cats")) continue;
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
            if (wanted("sitting")) sittingPets(context, world, groundY);
            if (wanted("baby")) babyPets(context, world, groundY);
            if (requested("geometry")) geometryScenes(context, world, groundY);
            if (requested("zfight")) lidViews(context, world, groundY, true);
            if (requested("zfightadults")) lidViews(context, world, groundY, false);
            if (wanted("mcv")) modChests(context, world, groundY);
            if (wanted("golem")) golemVisit(context, world, groundY);
            if (wanted("gui")) petScreen(context, world, groundY);
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
            look(world, groundY, -1);
            context.waitTicks(8);
            context.takeScreenshot(PREFIX + (wolf ? "wolf" : "cat") + "_copper_sitting_b_open");
            discard(world, uuid);
        }
    }

    /** Kittens and pups wear the bags at half size, standing and sitting, with the lid open, seen from both flanks. */
    private static void babyPets(ClientGameTestContext context, TestSingleplayerContext world, int groundY) {
        for (boolean wolf : new boolean[]{false, true}) {
            for (boolean sitting : new boolean[]{false, true}) {
                String name = (wolf ? "wolf" : "cat") + "_copper_baby" + (sitting ? "_sitting" : "");
                UUID uuid = spawnPet(world, groundY, wolf, Items.COPPER_CHEST);
                world.getServer().runOnServer(server -> {
                    TamableAnimal pet = (TamableAnimal) server.overworld().getEntity(uuid);
                    pet.setBaby(true);
                    pet.setInSittingPose(sitting);
                });
                context.waitTicks(15);
                look(world, groundY, 1);
                setLid(world, uuid, true);
                context.waitTicks(22);
                context.takeScreenshot(PREFIX + name + "_a_open");
                look(world, groundY, -1);
                context.waitTicks(8);
                context.takeScreenshot(PREFIX + name + "_b_open");
                discard(world, uuid);
            }
        }
    }

    /**
     * Camera positions of {@link #lidViews}: distance in blocks, degrees round the pet from the flank, degrees looking
     * down. A pet is not drawn beyond about 64 times its size, which is 20 blocks for a kitten.
     */
    private static final double[][] LID_VIEWS = {{2.6, 0, 8}, {2.6, 35, 30}, {2.6, -35, 30}, {1.3, 0, 8}, {10, 0, 3}, {10, 0, 20}, {15, 0, 5}};

    /**
     * Where the lids of the bags lie flat against the torso, they flicker (z-fighting) at some angles and distances only:
     * every pose of a cat or a wolf, lid shut and open, from both flanks and from near, far, high and oblique cameras.
     * Run with {@code -Drevived.screens.only=zfight} (kittens and pups) or {@code zfightadults}.
     */
    private static void lidViews(ClientGameTestContext context, TestSingleplayerContext world, int groundY, boolean baby) {
        for (boolean wolf : new boolean[]{false, true}) {
            for (boolean sitting : new boolean[]{false, true}) {
                UUID uuid = spawnPet(world, groundY, wolf, Items.COPPER_CHEST);
                world.getServer().runOnServer(server -> {
                    TamableAnimal pet = (TamableAnimal) server.overworld().getEntity(uuid);
                    pet.setBaby(baby);
                    pet.setInSittingPose(sitting);
                });
                context.waitTicks(15);
                for (boolean open : new boolean[]{true, false}) {
                    setLid(world, uuid, open);
                    context.waitTicks(open ? 22 : 16);
                    for (int side : new int[]{1, -1}) {
                        for (int view = 0; view < LID_VIEWS.length; view++) {
                            double[] at = LID_VIEWS[view];
                            lookFrom(world, groundY, side, at[0], at[1], at[2]);
                            context.waitTicks(6);
                            context.takeScreenshot(PREFIX + "zf_" + (wolf ? "wolf" : "cat") + (baby ? "_baby" : "_adult")
                                    + (sitting ? "_sit" : "_stand") + (open ? "_open_" : "_shut_") + (side == 1 ? "a" : "b") + "_v" + view);
                        }
                    }
                }
                discard(world, uuid);
            }
        }
    }

    /** Puts the (invisible, spectating) camera at a distance from the pet, round it from the given flank, looking down at it. */
    private static void lookFrom(TestSingleplayerContext world, int groundY, int side, double distance, double azimuth, double pitch) {
        double angle = Math.toRadians(azimuth) + (side == 1 ? 0.0 : Math.PI);
        // Eyes at the height that keeps the pet's back on the line of sight, 1.62 blocks above the feet of a spectator.
        double eyes = 0.35 + distance * Math.tan(Math.toRadians(pitch));
        world.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f %.2f %.2f",
                0.5 + distance * Math.cos(angle), groundY + eyes - 1.62, 0.5 + distance * Math.sin(angle), 90.0 + Math.toDegrees(angle), pitch));
    }

    /**
     * Opt-in ({@code geometry}): for every pet and pose, runs the real renderer into a collector that keeps the chest part the
     * layer submits, and prints one JSON line with the corners of every cube of the posed model and of the bags, for collision
     * maths done outside the game. Nothing is photographed.
     */
    private static void geometryScenes(ClientGameTestContext context, TestSingleplayerContext world, int groundY) {
        for (boolean wolf : new boolean[]{false, true}) {
            for (boolean baby : new boolean[]{false, true}) {
                for (boolean sitting : new boolean[]{false, true}) {
                    UUID uuid = spawnPet(world, groundY, wolf, Items.COPPER_CHEST);
                    world.getServer().runOnServer(server -> {
                        TamableAnimal pet = (TamableAnimal) server.overworld().getEntity(uuid);
                        pet.setBaby(baby);
                        pet.setInSittingPose(sitting);
                    });
                    context.waitTicks(15);
                    look(world, groundY, 1);
                    context.waitTicks(8);
                    String scene = (wolf ? "wolf" : "cat") + (baby ? "_baby" : "") + (sitting ? "_sitting" : "");
                    context.runOnClient(client -> {
                        for (Entity entity : client.level.entitiesForRendering()) {
                            if (entity.getUUID().equals(uuid)) printGeometry(client, entity, scene);
                        }
                    });
                    discard(world, uuid);
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void printGeometry(Minecraft client, Entity entity, String scene) {
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        EntityRenderer renderer = dispatcher.getRenderer(entity);
        EntityRenderState state = dispatcher.extractEntity(entity, 1.0F);
        for (float openness : new float[]{0.0F, 1.0F}) {
            ((IChestEntityRenderState) state).chestedCompanions$setLidOpenness(openness);
            StringBuilder line = new StringBuilder();
            float lid = openness;
            SubmitNodeCollector collector = (SubmitNodeCollector) Proxy.newProxyInstance(
                    SubmitNodeCollector.class.getClassLoader(), new Class<?>[]{SubmitNodeCollector.class}, (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return switch (method.getName()) {
                                case "hashCode" -> System.identityHashCode(proxy);
                                case "equals" -> proxy == args[0];
                                default -> "qa-collector";
                            };
                        }
                        if (method.getName().equals("submitModelPart") && args[0] instanceof ModelPart chest && chest.hasChild("left_body")) {
                            Model model = ((LivingEntityRenderer<?, ?, ?>) renderer).getModel();
                            line.append("{\"scene\":\"").append(scene).append("\",\"lid\":").append(lid)
                                    .append(",\"model\":\"").append(model.getClass().getSimpleName()).append("\",\"pet\":[");
                            solids(model.root(), "", new PoseStack(), true, line);
                            line.append("],\"chest\":[");
                            solids(chest, "", new PoseStack(), true, line);
                            line.append("]}");
                        }
                        return method.getReturnType().isInstance(proxy) ? proxy : null;
                    });
            renderer.submit(state, new PoseStack(), collector, new net.minecraft.client.renderer.state.CameraRenderState());
            System.out.println("REVIVED_QA_GEOM " + (line.length() == 0 ? "{\"scene\":\"" + scene + "\",\"lid\":" + openness + ",\"missing\":true}" : line));
        }
    }

    /** Every cube below a part as its eight corners (model units: sixteenths of a block) after the poses of the parts above it. */
    @SuppressWarnings("unchecked")
    private static void solids(ModelPart part, String path, PoseStack stack, boolean visibleAbove, StringBuilder out) {
        try {
            Field cubesField = ModelPart.class.getDeclaredField("cubes");
            Field childrenField = ModelPart.class.getDeclaredField("children");
            cubesField.setAccessible(true);
            childrenField.setAccessible(true);
            stack.pushPose();
            part.translateAndRotate(stack);
            boolean visible = visibleAbove && part.visible;
            Matrix4f pose = new Matrix4f(stack.last().pose());
            List<ModelPart.Cube> cubes = (List<ModelPart.Cube>) cubesField.get(part);
            for (int i = 0; i < cubes.size(); i++) {
                ModelPart.Cube cube = cubes.get(i);
                float[] lo = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
                float[] hi = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
                for (ModelPart.Polygon polygon : cube.polygons) {
                    for (ModelPart.Vertex vertex : polygon.vertices()) {
                        float[] v = {vertex.x(), vertex.y(), vertex.z()};
                        for (int a = 0; a < 3; a++) {
                            lo[a] = Math.min(lo[a], v[a]);
                            hi[a] = Math.max(hi[a], v[a]);
                        }
                    }
                }
                if (out.charAt(out.length() - 1) != '[') out.append(',');
                out.append("{\"path\":\"").append(path.isEmpty() ? "/" : path).append("\",\"i\":").append(i)
                        .append(",\"vis\":").append(visible && !part.skipDraw).append(",\"c\":[");
                for (int corner = 0; corner < 8; corner++) {
                    Vector3f p = pose.transformPosition(((corner & 1) == 0 ? lo[0] : hi[0]) / 16.0F,
                            ((corner & 2) == 0 ? lo[1] : hi[1]) / 16.0F, ((corner & 4) == 0 ? lo[2] : hi[2]) / 16.0F, new Vector3f());
                    out.append(corner == 0 ? "" : ",").append(String.format(Locale.ROOT, "%.3f,%.3f,%.3f", p.x * 16.0F, p.y * 16.0F, p.z * 16.0F));
                }
                out.append("]}");
            }
            for (var child : ((java.util.Map<String, ModelPart>) childrenField.get(part)).entrySet()) {
                solids(child.getValue(), path + "/" + child.getKey(), stack, visible, out);
            }
            stack.popPose();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /** Chests of More Chest Variants when it is installed: one with its own bag texture, and pale oak's shared atlas. */
    private static void modChests(ClientGameTestContext context, TestSingleplayerContext world, int groundY) {
        for (boolean wolf : new boolean[]{false, true}) {
            for (String id : new String[]{"cherry_chest", "pale_oak_chest"}) {
                Item chest = BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath("lolmcv", id)).orElse(null);
                if (chest == null) return;
                UUID uuid = spawnPet(world, groundY, wolf, chest);
                context.waitTicks(15);
                look(world, groundY, 1);
                setLid(world, uuid, true);
                context.waitTicks(22);
                context.takeScreenshot(PREFIX + (wolf ? "wolf" : "cat") + "_mcv_" + id + "_a_open");
                discard(world, uuid);
            }
        }
    }

    /**
     * The pet's chest screen: the remove-chest button is there, shows its tooltip when hovered, and a real click on it
     * closes the screen and leaves the chest and everything that was in it on the ground. Survival mode, so it runs last.
     */
    private static void petScreen(ClientGameTestContext context, TestSingleplayerContext world, int groundY) {
        UUID uuid = spawnPet(world, groundY, false, Items.CHEST);
        world.getServer().runCommand("gamemode survival @p");
        world.getServer().runCommand("tp @p 3.1 " + groundY + " 0.5 90 0");
        context.waitTicks(10);
        world.getServer().runOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            TamableAnimal pet = (TamableAnimal) server.overworld().getEntity(uuid);
            pet.tame(player);
            IChestEntity chested = (IChestEntity) pet;
            chested.chestedCompanions$getInventory().setItem(0, new ItemStack(Items.DIAMOND, 5));
            chested.chestedCompanions$getInventory().setItem(1, new ItemStack(Items.EMERALD, 3));
            chested.openCustomInventoryScreen(player);
        });
        context.waitForScreen(ContainerScreen.class);
        context.waitTicks(5);
        context.takeScreenshot(PREFIX + "gui_pet_chest");

        double[] button = context.computeOnClient(client -> {
            for (var child : client.screen.children()) {
                if (child instanceof PetRemoveChestButton image) {
                    double scale = client.getWindow().getGuiScale();
                    return new double[]{(image.getX() + image.getWidth() / 2.0) * scale, (image.getY() + image.getHeight() / 2.0) * scale};
                }
            }
            return null;
        });
        if (button == null) {
            throw new AssertionError("REVIVED_CLIENT_SCREENSHOTS_FAILED: the pet's chest screen has no remove-chest button");
        }
        context.getInput().setCursorPos(button[0], button[1]);
        context.waitTicks(25);
        context.takeScreenshot(PREFIX + "gui_pet_chest_hover");
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(15);

        boolean closed = context.computeOnClient(client -> client.screen == null);
        int[] result = world.getServer().computeOnServer(server -> {
            TamableAnimal pet = (TamableAnimal) server.overworld().getEntity(uuid);
            int chests = 0, diamonds = 0, emeralds = 0;
            for (ItemEntity item : server.overworld().getEntitiesOfClass(ItemEntity.class, pet.getBoundingBox().inflate(6.0))) {
                ItemStack stack = item.getItem();
                chests += stack.is(Items.CHEST) ? stack.getCount() : 0;
                diamonds += stack.is(Items.DIAMOND) ? stack.getCount() : 0;
                emeralds += stack.is(Items.EMERALD) ? stack.getCount() : 0;
            }
            return new int[]{((IChestEntity) pet).chestedCompanions$hasChest() ? 1 : 0, chests, diamonds, emeralds};
        });
        context.takeScreenshot(PREFIX + "gui_pet_chest_removed");
        if (!closed || result[0] != 0 || result[1] != 1 || result[2] != 5 || result[3] != 3) {
            throw new AssertionError("REVIVED_CLIENT_SCREENSHOTS_FAILED: the remove-chest button left screenClosed=" + closed + ", petHasChest=" + result[0]
                    + ", chestsOnGround=" + result[1] + ", diamondsOnGround=" + result[2] + ", emeraldsOnGround=" + result[3]);
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
