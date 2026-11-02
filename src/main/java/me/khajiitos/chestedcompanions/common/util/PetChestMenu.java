package me.khajiitos.chestedcompanions.common.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

/** The chest screen of a pet: a vanilla chest menu, plus the button that takes the chest off the pet. */
public class PetChestMenu<T extends TamableAnimal & IChestEntity> extends ChestMenu {
    /** Id of the remove-chest button, sent by the client with the vanilla menu button packet. */
    public static final int REMOVE_CHEST_BUTTON = 0;

    private final T pet;

    public PetChestMenu(MenuType<?> type, int containerId, Inventory playerInventory, Container container, int rows, T pet) {
        super(type, containerId, playerInventory, container, rows);
        this.pet = pet;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != REMOVE_CHEST_BUTTON) {
            return super.clickMenuButton(player, id);
        }
        if (ChestEntityCommon.canTakeChestOff(this.pet, player)) {
            ChestEntityCommon.takeChestOff(this.pet, player);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.closeContainer();
            }
        }
        return true;
    }
}
