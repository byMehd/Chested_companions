package me.khajiitos.chestedcompanions.common.util;

import net.minecraft.world.Container;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;

/** A menu view of the viewer's vanilla Ender inventory; never stored on the pet. */
public final class PetEnderChestContainer<T extends TamableAnimal & IChestEntity> implements Container {
    private final T pet;
    private final Player viewer;
    private final PlayerEnderChestContainer inventory;

    public PetEnderChestContainer(T pet, Player viewer) {
        this.pet = pet;
        this.viewer = viewer;
        this.inventory = viewer.getEnderChestInventory();
    }

    @Override public int getContainerSize() { return inventory.getContainerSize(); }
    @Override public boolean isEmpty() { return inventory.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return inventory.getItem(slot); }
    @Override public ItemStack removeItem(int slot, int count) { return inventory.removeItem(slot, count); }
    @Override public ItemStack removeItemNoUpdate(int slot) { return inventory.removeItemNoUpdate(slot); }
    @Override public void setItem(int slot, ItemStack stack) { inventory.setItem(slot, stack); }
    @Override public void setChanged() { inventory.setChanged(); }
    @Override public void clearContent() { inventory.clearContent(); }

    @Override public boolean stillValid(Player player) {
        return player == viewer && pet.isAlive() && !pet.isRemoved()
                && pet.chestedCompanions$hasEnderChest() && player.level() == pet.level()
                && ChestEntityCommon.canAccess(pet, player)
                && player.isWithinEntityInteractionRange(pet, 4.0);
    }
}
