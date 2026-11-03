package me.khajiitos.chestedcompanions.common.util;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;

/** Open and close sounds of the chest a pet wears, read from the matching vanilla block. */
public final class PetChestSounds {
    private PetChestSounds() {}

    public static SoundEvent open(ItemStack chest) {
        if (chest.is(Items.ENDER_CHEST)) {
            return SoundEvents.ENDER_CHEST_OPEN;
        }
        return Block.byItem(chest.getItem()) instanceof ChestBlock block ? block.getOpenChestSound() : SoundEvents.CHEST_OPEN;
    }

    public static SoundEvent close(ItemStack chest) {
        if (chest.is(Items.ENDER_CHEST)) {
            return SoundEvents.ENDER_CHEST_CLOSE;
        }
        return Block.byItem(chest.getItem()) instanceof ChestBlock block ? block.getCloseChestSound() : SoundEvents.CHEST_CLOSE;
    }
}
