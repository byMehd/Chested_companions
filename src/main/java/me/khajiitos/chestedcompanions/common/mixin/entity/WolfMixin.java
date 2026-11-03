package me.khajiitos.chestedcompanions.common.mixin.entity;

import me.khajiitos.chestedcompanions.common.config.CCConfig;
import me.khajiitos.chestedcompanions.common.util.ChestEntityCommon;
import me.khajiitos.chestedcompanions.common.util.PetMenuProvider;
import me.khajiitos.chestedcompanions.common.util.IChestEntity;
import me.khajiitos.chestedcompanions.common.util.InventoryCapacity;
import me.khajiitos.chestedcompanions.common.util.PetChestState;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Wolf.class)
public abstract class WolfMixin extends TamableAnimal implements IChestEntity {
    //? if !neoforge {
    @Unique
    @SuppressWarnings("all")
    private static final EntityDataAccessor<ItemStack> chestedCompanions$CHEST_ITEM = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.ITEM_STACK);

    //? }

    @Unique
    protected PetChestContainer<?> chestedCompanions$inventory;

    @Unique
    private final PetChestState chestedCompanions$state = new PetChestState();

    protected WolfMixin(EntityType<? extends TamableAnimal> $$0, Level $$1) {
        super($$0, $$1);
    }

    @Override
    public InventoryCapacity chestedCompanions$getInventoryCapacity() {
        return CCConfig.wolfInventoryCapacity.get();
    }

    @Override
    public boolean chestedCompanions$allowChest() {
        return CCConfig.allowChestOnWolves.get();
    }

    @Override
    public boolean chestedCompanions$allowChestOnBaby() {
        return CCConfig.allowChestOnBabyWolf.get();
    }

    @Override
    public PetChestContainer<?> chestedCompanions$getInventory() {
        return this.chestedCompanions$inventory;
    }

    @Override
    public PetChestState chestedCompanions$getState() {
        return this.chestedCompanions$state;
    }

    @Override
    public ItemStack chestedCompanions$getChestItemStack() {
        //? if neoforge {
        /*return me.khajiitos.chestedcompanions.neoforged.NeoPetChestData.get(this);*/
        //? } else {
        return this.getEntityData().get(chestedCompanions$CHEST_ITEM);
        //? }

    }

    public void chestedCompanions$setChestItemStack(ItemStack itemStack) {
        //? if neoforge {
        /*me.khajiitos.chestedcompanions.neoforged.NeoPetChestData.set(this, itemStack);*/
        //? } else {
        this.getEntityData().set(chestedCompanions$CHEST_ITEM, itemStack);
        //? }

    }

    //? if !neoforge {
    @Inject(at = @At("TAIL"), method = "defineSynchedData")
    public void defineSynchedData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(chestedCompanions$CHEST_ITEM, ItemStack.EMPTY);
    }

    //? }

    @Inject(at = @At("TAIL"), method = "addAdditionalSaveData")
    public void addAdditionalSaveData(ValueOutput valueOutput, CallbackInfo ci) {
        ChestEntityCommon.addAdditionalSaveData(this, valueOutput);
    }

    @Inject(at = @At("TAIL"), method = "readAdditionalSaveData")
    public void readAdditionalSaveData(ValueInput valueInput, CallbackInfo ci) {
        ChestEntityCommon.readAdditionalSaveData(this, valueInput);
    }

    @Unique
    @Override
    public void chestedCompanions$createInventory() {
        if (this.chestedCompanions$hasEnderChest()) {
            this.chestedCompanions$inventory = null;
            return;
        }
        this.chestedCompanions$inventory = new IChestEntity.PetChestContainer<>(this, this.chestedCompanions$getInventoryCapacity().containerRows * 9, this.chestedCompanions$inventory);
    }

    @Override
    public void chestedCompanions$removeInventory() {
        this.chestedCompanions$inventory = null;
    }

    @Unique
    public void chestedCompanions$removeChestContent(boolean dropChest) {
        ChestEntityCommon.removeChestContent(this, dropChest);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
        return ChestEntityCommon.createMenu(this, i, inventory, player);
    }

    public void openCustomInventoryScreen(Player player) {
        player.openMenu(new PetMenuProvider<>(this));
    }

    @Inject(at = @At("HEAD"), method = "mobInteract", cancellable = true)
    public void mobInteract(Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        ChestEntityCommon.mobInteract(this, player, interactionHand, cir);
    }

    @Inject(at = @At("TAIL"), method = "tick")
    public void chestedCompanions$tick(CallbackInfo ci) {
        ChestEntityCommon.tick(this);
    }

    @Override
    protected void dropEquipment(@NotNull ServerLevel serverLevel) {
        super.dropEquipment(serverLevel);
        if (this.chestedCompanions$hasChest()) {
            this.chestedCompanions$removeChestContent(true);
        }
    }
}
