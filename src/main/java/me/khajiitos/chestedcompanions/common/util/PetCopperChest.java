package me.khajiitos.chestedcompanions.common.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CopperChestBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.Optional;

/**
 * Copper chests on pets age, get waxed and get scraped exactly like the placed block.
 * The chest keeps its contents: only the equipped item changes between copper variants.
 */
public final class PetCopperChest {
    /** Level events that spawn the vanilla wax and scrape particles (and the honeycomb sound). */
    private static final int WAX_ON = 3003;
    private static final int WAX_OFF = 3004;
    private static final int SCRAPE = 3005;
    /** {@code ChangeOverTimeBlock#changeOverTime}: chance of looking for a next stage on a random tick. */
    private static final float AGING_CHANCE_PER_RANDOM_TICK = 0.05688889F;
    /** A block is picked by a random tick with probability {@code randomTickSpeed / 4096} each game tick. */
    private static final float BLOCKS_PER_SECTION = 4096.0F;

    private PetCopperChest() {}

    public static boolean isCopper(ItemStack chest) {
        return !chest.isEmpty() && Block.byItem(chest.getItem()) instanceof CopperChestBlock;
    }

    /** The chest this one turns into on its next random tick, if it can still oxidise. */
    public static Optional<Item> next(ItemStack chest) {
        return isCopper(chest) ? WeatheringCopper.getNext(Block.byItem(chest.getItem())).map(Block::asItem) : Optional.empty();
    }

    /** The less oxidised chest an axe scrapes this one back to. Waxed chests have none. */
    public static Optional<Item> previous(ItemStack chest) {
        return isCopper(chest) ? WeatheringCopper.getPrevious(Block.byItem(chest.getItem())).map(Block::asItem) : Optional.empty();
    }

    public static Optional<Item> waxed(ItemStack chest) {
        return isCopper(chest)
                ? HoneycombItem.getWaxed(Block.byItem(chest.getItem()).defaultBlockState()).map(state -> state.getBlock().asItem())
                : Optional.empty();
    }

    /** The unwaxed chest a waxed one was made from: the stage of the oxidation chain whose waxed form it is. */
    public static Optional<Item> unwaxed(ItemStack chest) {
        if (!isCopper(chest) || Block.byItem(chest.getItem()) instanceof WeatheringCopper) {
            return Optional.empty();
        }
        Block waxedBlock = Block.byItem(chest.getItem());
        for (Block stage = WeatheringCopper.getFirst(Blocks.COPPER_CHEST); stage != null;
                stage = WeatheringCopper.getNext(stage).orElse(null)) {
            if (HoneycombItem.getWaxed(stage.defaultBlockState()).filter(waxed -> waxed.getBlock() == waxedBlock).isPresent()) {
                return Optional.of(stage.asItem());
            }
        }
        return Optional.empty();
    }

    /**
     * Chance, per game tick, that a pet's chest takes its next oxidation step. This is the chance a placed,
     * isolated copper chest has per game tick (random tick speed, section size and vanilla's stage modifier).
     */
    public static float agingChancePerTick(int randomTickSpeed, ItemStack chest) {
        if (randomTickSpeed <= 0 || !(Block.byItem(chest.getItem()) instanceof WeatheringCopper weathering)) {
            return 0.0F;
        }
        return randomTickSpeed / BLOCKS_PER_SECTION * AGING_CHANCE_PER_RANDOM_TICK * weathering.getChanceModifier();
    }

    /** The world's random tick speed: what makes a placed copper chest age, and so a worn one. */
    public static int randomTickSpeed(ServerLevel level) {
        //? if >=1.21.11 {
        return level.getGameRules().get(GameRules.RANDOM_TICK_SPEED);
        //?} else {
        /*return level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        *///?}
    }

    public static <T extends TamableAnimal & IChestEntity> void tickAging(T pet, ServerLevel level) {
        ItemStack chest = pet.chestedCompanions$getChestItemStack();
        float chance = agingChancePerTick(randomTickSpeed(level), chest);
        if (chance > 0.0F && level.getRandom().nextFloat() < chance) {
            next(chest).ifPresent(item -> replace(pet, chest, item));
        }
    }

    /**
     * Honeycomb waxes the chest; an axe scrapes one oxidation stage, or removes the wax when there is none to scrape.
     * Returns whether the item was used, so the caller can fall back to opening the chest.
     */
    public static <T extends TamableAnimal & IChestEntity> boolean tryUse(T pet, Player player, InteractionHand hand, ItemStack held) {
        ItemStack chest = pet.chestedCompanions$getChestItemStack();
        if (!(pet.level() instanceof ServerLevel level) || !isCopper(chest)) {
            return false;
        }
        if (held.is(Items.HONEYCOMB)) {
            Optional<Item> waxed = waxed(chest);
            if (waxed.isEmpty()) {
                return false;
            }
            replace(pet, chest, waxed.get());
            level.levelEvent(null, WAX_ON, pet.blockPosition(), 0);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            return true;
        }
        if (held.is(ItemTags.AXES) || held.getItem() instanceof AxeItem) {
            Optional<Item> scraped = previous(chest);
            Optional<Item> unwaxed = scraped.isPresent() ? Optional.empty() : unwaxed(chest);
            if (scraped.isEmpty() && unwaxed.isEmpty()) {
                return false;
            }
            boolean scrape = scraped.isPresent();
            replace(pet, chest, scrape ? scraped.get() : unwaxed.get());
            SoundEvent sound = scrape ? SoundEvents.AXE_SCRAPE : SoundEvents.AXE_WAX_OFF;
            level.playSound(null, pet, sound, pet.getSoundSource(), 1.0F, 1.0F);
            level.levelEvent(null, scrape ? SCRAPE : WAX_OFF, pet.blockPosition(), 0);
            held.hurtAndBreak(1, player, hand);
            return true;
        }
        return false;
    }

    private static <T extends TamableAnimal & IChestEntity> void replace(T pet, ItemStack chest, Item item) {
        pet.chestedCompanions$setChestItemStack(chest.transmuteCopy(item, 1));
    }
}
