package me.khajiitos.chestedcompanions.common.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Opens a pet's chest. Its title is the pet's name and the name of its chest ("Cat - Chest", "Wolf - Ender Chest"),
 * built by {@value #TITLE_KEY}: that is also how the client knows the screen it just opened belongs to a pet and adds
 * the remove-chest button.
 */
public record PetMenuProvider<T extends TamableAnimal & IChestEntity>(T pet) implements MenuProvider {
    public static final String TITLE_KEY = "container.chestedcompanions.pet_chest";

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable(TITLE_KEY, this.pet.getDisplayName(), this.pet.chestedCompanions$getChestItemStack().getItemName());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return ChestEntityCommon.createMenu(this.pet, containerId, inventory, player);
    }
}
