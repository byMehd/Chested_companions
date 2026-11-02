package me.khajiitos.chestedcompanions.smokescenarios;

import me.khajiitos.chestedcompanions.common.util.IChestEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Lives outside the mixin package of the smoke mod, which Mixin forbids referencing directly from other classes.
 * Plays whole gameplay scenarios on a real dedicated server: a copper golem emptying a cat's copper chest into a
 * placed chest, copper chest oxidation and waxing, and chest removal and death drops. Every scenario keeps a running
 * count of the items it uses, so any duplication or loss fails the run on the tick it happens.
 */
public final class ServerSmokeScenario {
    private static final int SETUP_DELAY_TICKS = 60;
    private static final int GOLEM_TIMEOUT_TICKS = 3000;
    private static final int AGING_TIMEOUT_TICKS = 4000;

    private enum Stage { WAITING, GOLEM, AGING, DROPS, DONE }

    private final MinecraftServer server;
    private ServerLevel level;
    private BlockPos base;
    private Stage stage = Stage.WAITING;
    private int tick;
    private int stageTick;

    // golem scenario
    private Cat petWithCopper;
    private CopperGolem golem;
    private BlockPos chestPos;
    private Vec3 petStart;
    private boolean sawOpen;
    private boolean sawClosedAfterOpen;
    private boolean sawGetItemState;
    private double maxDriftWhileOpen;
    private int openTicks;

    // aging scenario
    private Cat agingPet;
    private Cat waxedPet;
    private final List<Item> agingStages = new ArrayList<>();

    public ServerSmokeScenario(MinecraftServer server) {
        this.server = server;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("REVIVED_SERVER_SMOKE_FAILED: " + message);
        }
    }

    /** @return true once every scenario has passed */
    public boolean tick() {
        this.tick++;
        this.stageTick++;
        switch (this.stage) {
            case WAITING -> {
                if (this.tick >= SETUP_DELAY_TICKS) {
                    this.setUpWorld();
                    this.startGolemScenario();
                    this.next(Stage.GOLEM);
                }
            }
            case GOLEM -> {
                this.tickGolemScenario();
            }
            case AGING -> this.tickAgingScenario();
            case DROPS -> {
                this.runDropScenarios();
                this.next(Stage.DONE);
            }
            case DONE -> {
                return true;
            }
        }
        return false;
    }

    private void next(Stage stage) {
        this.stage = stage;
        this.stageTick = 0;
    }

    private void setUpWorld() {
        this.level = this.server.overworld();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                this.level.setChunkForced(x, z, true);
            }
        }
        this.level.getChunk(0, 0);
        this.base = new BlockPos(0, this.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0), 0);
        System.out.println("REVIVED_SERVER_SMOKE: scenario ground at " + this.base);
    }

    private Cat spawnPet(BlockPos at, ItemStack chest) {
        Cat cat = EntityType.CAT.spawn(this.level, at, EntitySpawnReason.COMMAND);
        check(cat != null, "could not spawn a cat");
        cat.setTame(true, false);
        cat.setOrderedToSit(true);
        IChestEntity pet = (IChestEntity) cat;
        pet.chestedCompanions$setChestItemStack(chest);
        pet.chestedCompanions$createInventory();
        return cat;
    }

    private static Container inventory(TamableAnimal pet) {
        return ((IChestEntity) pet).chestedCompanions$getInventory();
    }

    // ---------------------------------------------------------------- golem

    private void startGolemScenario() {
        this.chestPos = this.base.offset(7, 0, 0);
        this.level.setBlockAndUpdate(this.chestPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST));
        this.petWithCopper = this.spawnPet(this.base, new ItemStack(Items.COPPER_CHEST));
        inventory(this.petWithCopper).setItem(0, new ItemStack(Items.DIAMOND, 5));
        this.golem = EntityType.COPPER_GOLEM.spawn(this.level, this.base.offset(-5, 0, 0), EntitySpawnReason.COMMAND);
        check(this.golem != null, "could not spawn a copper golem");
        this.petStart = this.petWithCopper.position();
    }

    private int countDiamondsInGolemScenario() {
        int total = countDiamonds(inventory(this.petWithCopper));
        ItemStack held = this.golem.getItemBySlot(EquipmentSlot.MAINHAND);
        total += held.is(Items.DIAMOND) ? held.getCount() : 0;
        if (this.level.getBlockEntity(this.chestPos) instanceof ChestBlockEntity chest) {
            total += countDiamonds(chest);
        }
        for (ItemEntity item : this.level.getEntitiesOfClass(ItemEntity.class, new AABB(this.base).inflate(20.0))) {
            total += item.getItem().is(Items.DIAMOND) ? item.getItem().getCount() : 0;
        }
        return total;
    }

    private static int countDiamonds(Container container) {
        int total = 0;
        for (ItemStack stack : container) {
            total += stack.is(Items.DIAMOND) ? stack.getCount() : 0;
        }
        return total;
    }

    private void tickGolemScenario() {
        var state = ((IChestEntity) this.petWithCopper).chestedCompanions$getState();
        if (state.isOpen()) {
            this.sawOpen = true;
            this.openTicks++;
            this.maxDriftWhileOpen = Math.max(this.maxDriftWhileOpen, this.petWithCopper.position().distanceTo(this.petStart));
        } else if (this.sawOpen) {
            this.sawClosedAfterOpen = true;
        }
        this.sawGetItemState |= this.golem.getState() == net.minecraft.world.entity.animal.golem.CopperGolemState.GETTING_ITEM;
        check(this.countDiamondsInGolemScenario() == 5, "diamonds were lost or duplicated: " + this.countDiamondsInGolemScenario() + " instead of 5");
        boolean delivered = this.level.getBlockEntity(this.chestPos) instanceof ChestBlockEntity chest && countDiamonds(chest) == 5;
        if (delivered) {
            check(this.sawOpen && this.sawClosedAfterOpen, "the pet's chest never opened and closed");
            check(this.sawGetItemState, "the golem never played its item pickup animation");
            check(this.maxDriftWhileOpen < 1.5, "the pet moved " + this.maxDriftWhileOpen + " blocks while the golem worked");
            check(this.openTicks >= 55, "the golem did not keep the chest open for its whole interaction: " + this.openTicks);
            check(countDiamonds(inventory(this.petWithCopper)) == 0 && this.golem.getMainHandItem().isEmpty(), "leftovers after delivery");
            System.out.println("REVIVED_SERVER_SMOKE: golem scenario passed after " + this.stageTick + " ticks (chest open for "
                    + this.openTicks + " ticks, pet moved " + String.format("%.2f", this.maxDriftWhileOpen) + " blocks)");
            this.petWithCopper.discard();
            this.golem.discard();
            this.startAgingScenario();
            this.next(Stage.AGING);
            return;
        }
        check(this.stageTick < GOLEM_TIMEOUT_TICKS, "timeout in the golem scenario: pet inventory " + inventory(this.petWithCopper)
                + ", golem holds " + this.golem.getMainHandItem() + ", state " + this.golem.getState() + ", opened=" + this.sawOpen);
    }

    // ---------------------------------------------------------------- aging

    private void startAgingScenario() {
        this.level.getGameRules().set(GameRules.RANDOM_TICK_SPEED, 3000, this.server);
        this.agingPet = this.spawnPet(this.base.offset(0, 0, 5), new ItemStack(Items.COPPER_CHEST));
        this.waxedPet = this.spawnPet(this.base.offset(0, 0, -5), new ItemStack(Items.WAXED_COPPER_CHEST));
        inventory(this.agingPet).setItem(0, new ItemStack(Items.EMERALD, 3));
        this.agingStages.add(Items.COPPER_CHEST);
    }

    private void tickAgingScenario() {
        Item now = ((IChestEntity) this.agingPet).chestedCompanions$getChestItemStack().getItem();
        if (now != this.agingStages.get(this.agingStages.size() - 1)) {
            this.agingStages.add(now);
        }
        check(((IChestEntity) this.waxedPet).chestedCompanions$getChestItemStack().is(Items.WAXED_COPPER_CHEST), "a waxed chest oxidised");
        if (now == Items.OXIDIZED_COPPER_CHEST) {
            check(this.agingStages.equals(List.of(Items.COPPER_CHEST, Items.EXPOSED_COPPER_CHEST,
                    Items.WEATHERED_COPPER_CHEST, Items.OXIDIZED_COPPER_CHEST)), "oxidation skipped a stage: " + this.agingStages);
            check(inventory(this.agingPet).getItem(0).getCount() == 3 && inventory(this.agingPet).getItem(0).is(Items.EMERALD),
                    "oxidation changed the chest contents");
            System.out.println("REVIVED_SERVER_SMOKE: aging scenario passed after " + this.stageTick + " ticks");
            this.level.getGameRules().set(GameRules.RANDOM_TICK_SPEED, 3, this.server);
            this.agingPet.discard();
            this.waxedPet.discard();
            this.next(Stage.DROPS);
            return;
        }
        check(this.stageTick < AGING_TIMEOUT_TICKS, "timeout in the aging scenario, stages seen: " + this.agingStages);
    }

    // ---------------------------------------------------------------- drops

    private void runDropScenarios() {
        // Shearing: contents and the chest come out exactly once, a second removal is harmless.
        Cat sheared = this.spawnPet(this.base.offset(8, 0, 8), new ItemStack(Items.COPPER_CHEST));
        inventory(sheared).setItem(0, new ItemStack(Items.DIAMOND, 3));
        inventory(sheared).setItem(4, new ItemStack(Items.EMERALD, 2));
        AABB area = new AABB(this.base.offset(8, 0, 8)).inflate(6.0);
        ((IChestEntity) sheared).chestedCompanions$removeChestContent(true);
        ((IChestEntity) sheared).chestedCompanions$removeChestContent(true);
        check(!((IChestEntity) sheared).chestedCompanions$hasChest() && ((IChestEntity) sheared).chestedCompanions$getInventory() == null,
                "the pet still has a chest after removal");
        check(this.countDropped(area, Items.DIAMOND) == 3 && this.countDropped(area, Items.EMERALD) == 2
                && this.countDropped(area, Items.COPPER_CHEST) == 1, "removal dropped the wrong amounts: "
                + this.countDropped(area, Items.DIAMOND) + " diamonds, " + this.countDropped(area, Items.EMERALD) + " emeralds, "
                + this.countDropped(area, Items.COPPER_CHEST) + " chests");
        sheared.discard();
        this.level.getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);

        // Death: the same, exactly once.
        Cat dying = this.spawnPet(this.base.offset(8, 0, 8), new ItemStack(Items.CHEST));
        inventory(dying).setItem(1, new ItemStack(Items.GOLD_INGOT, 9));
        dying.kill(this.level);
        check(this.countDropped(area, Items.GOLD_INGOT) == 9 && this.countDropped(area, Items.CHEST) == 1,
                "a cat's death dropped " + this.countDropped(area, Items.GOLD_INGOT) + " ingots and "
                        + this.countDropped(area, Items.CHEST) + " chests instead of 9 and 1");
        System.out.println("REVIVED_SERVER_SMOKE: removal and death drop scenarios passed");
    }

    private int countDropped(AABB area, Item item) {
        int total = 0;
        for (ItemEntity entity : this.level.getEntitiesOfClass(ItemEntity.class, area)) {
            total += entity.getItem().is(item) ? entity.getItem().getCount() : 0;
        }
        return total;
    }
}
