package me.khajiitos.chestedcompanions.common.util;

import me.khajiitos.chestedcompanions.common.config.CCConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

public class ChestEntityCommon {
    public static boolean canAccess(TamableAnimal pet, Player player) {
        return pet.isTame() && (CCConfig.publicChest.get() || pet.isOwnedBy(player));
    }


    public static <T extends TamableAnimal & IChestEntity> void mobInteract(T chestEntity, Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        if (chestEntity.isAlive() && !chestEntity.level().isClientSide() && player.isCrouching() == CCConfig.invertShiftToOpen.get() && canAccess(chestEntity, player) && (!CCConfig.feedingOverridesOpeningChest.get() || !(chestEntity.isFood(player.getItemInHand(interactionHand)) && chestEntity.getHealth() < chestEntity.getMaxHealth()))) {
            if (chestEntity.chestedCompanions$hasChest()) {
                ItemStack inHand = player.getItemInHand(interactionHand);
                if (inHand.is(Items.SHEARS)) {
                    inHand.hurtAndBreak(1, player, interactionHand);

                    chestEntity.chestedCompanions$removeChestContent(!player.getAbilities().instabuild);
                    chestEntity.playSound(SoundEvents.DONKEY_CHEST, 1.0F, 1.5F);
                } else {
                    chestEntity.openCustomInventoryScreen(player);
                }
                cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
            } else if (chestEntity.chestedCompanions$allowChest() && (!chestEntity.isBaby() || chestEntity.chestedCompanions$allowChestOnBaby())) {
                ItemStack inHand = player.getItemInHand(interactionHand);

                if (chestEntity.chestedCompanions$isValidChestItem(inHand)) {
                    chestEntity.chestedCompanions$setChestItemStack(inHand.copyWithCount(1));

                    if (!player.getAbilities().instabuild) {
                        inHand.shrink(1);
                    }

                    chestEntity.playSound(SoundEvents.DONKEY_CHEST, 1.0F, (chestEntity.getRandom().nextFloat() - chestEntity.getRandom().nextFloat()) * 0.2F + 1.0F);
                    chestEntity.chestedCompanions$createInventory();
                    cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
                }
            }
        }
    }

    public static <T extends TamableAnimal & IChestEntity> void addAdditionalSaveData(T chestEntity, ValueOutput valueOutput) {
        if (chestEntity.chestedCompanions$hasChest()) {
            ItemStack chestItemStack = chestEntity.chestedCompanions$getChestItemStack();
            valueOutput.store("ChestItem", ItemStack.CODEC, chestItemStack);
        }

        if (!chestEntity.chestedCompanions$hasEnderChest() && chestEntity.chestedCompanions$getInventory() != null) {
            var itemsList = valueOutput.list("CCItems", ItemStackWithSlot.CODEC);
            chestEntity.chestedCompanions$getInventory().storeAsItemListWithSlot(itemsList);
        }
    }

    public static <T extends TamableAnimal & IChestEntity> void readAdditionalSaveData(T chestEntity, ValueInput valueInput) {
        Optional<ItemStack> itemTag = valueInput.read("ChestItem", ItemStack.CODEC);

        ItemStack chestItemStack;

        if (itemTag.isPresent()) {
            chestItemStack = itemTag.get();
        } else if (valueInput.getBooleanOr("HasChest", false)) {
            // Loaded the mod with an old version of the mod that didn't keep the ItemStack - replace with vanilla chest
            chestItemStack = new ItemStack(Items.CHEST);
        } else {
            return;
        }

        if (!chestItemStack.isEmpty()) {
            chestEntity.chestedCompanions$setChestItemStack(chestItemStack);
            chestEntity.chestedCompanions$createInventory();
            if (!chestEntity.chestedCompanions$hasEnderChest()) {
                valueInput.list("CCItems", ItemStackWithSlot.CODEC).ifPresent(list -> chestEntity.chestedCompanions$getInventory().loadFromItemListWithSlot(list));
            }
        }
    }

    public static <T extends TamableAnimal & IChestEntity> void removeChestContent(T chestEntity, boolean dropChest) {
        if (!(chestEntity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        IChestEntity.PetChestContainer<?> inventory = chestEntity.chestedCompanions$getInventory();
        ItemStack chest = chestEntity.chestedCompanions$getChestItemStack().copy();
        // Invalidate open menus before spawning anything; callbacks must see an empty pet.
        chestEntity.chestedCompanions$setChestItemStack(ItemStack.EMPTY);
        chestEntity.chestedCompanions$removeInventory();
        if (inventory != null) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.removeItemNoUpdate(i);
                if (!stack.isEmpty()) {
                    chestEntity.spawnAtLocation(serverLevel, stack, 0.25f);
                }
            }
        }
        if (dropChest && !chest.isEmpty()) {
            chestEntity.spawnAtLocation(serverLevel, chest, 0.25f);
        }
    }

    public static <T extends TamableAnimal & IChestEntity> AbstractContainerMenu createMenu(T chestEntity, int i, @NotNull Inventory inventory, @NotNull Player player) {
        if (!chestEntity.isAlive() || !chestEntity.chestedCompanions$hasChest()
                || !canAccess(chestEntity, player) || player.level() != chestEntity.level()
                || !player.isWithinEntityInteractionRange(chestEntity, 4.0)) {
            return null;
        }
        if (chestEntity.chestedCompanions$hasEnderChest()) {
            return ChestMenu.threeRows(i, inventory, new PetEnderChestContainer<>(chestEntity, player));
        }
        InventoryCapacity inventoryCapacity = chestEntity.chestedCompanions$getInventoryCapacity();

        // Needs resizing, probably because of config change
        if (chestEntity.chestedCompanions$getInventory() == null || chestEntity.chestedCompanions$getInventory().getContainerSize() != inventoryCapacity.containerRows * 9) {
            chestEntity.chestedCompanions$createInventory();
        }

        return new ChestMenu(inventoryCapacity.menuType, i, inventory, chestEntity.chestedCompanions$getInventory(), inventoryCapacity.containerRows);
    }
}
