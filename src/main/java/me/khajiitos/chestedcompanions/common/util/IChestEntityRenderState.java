package me.khajiitos.chestedcompanions.common.util;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface IChestEntityRenderState {
    ItemStack chestedCompanions$getChestItemStack();
    void chestedCompanions$setChestItemStack(@NotNull ItemStack itemStack);
    boolean chestedCompanions$shouldRenderChestIcon();
    void chestedCompanions$setShouldRenderChestIcon(boolean shouldRenderChestIcon);
    float chestedCompanions$getLidOpenness();
    void chestedCompanions$setLidOpenness(float lidOpenness);

    default boolean chestedCompanions$hasChest() {
        return !this.chestedCompanions$getChestItemStack().isEmpty();
    }
}
