package me.khajiitos.chestedcompanions.neoforged;

import me.khajiitos.chestedcompanions.common.ChestedCompanions;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;

/** Native tracking sync; persistence stays in legacy ChestItem entity data. */
public final class NeoPetChestData {
    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ChestedCompanions.MOD_ID);
    private static final Supplier<AttachmentType<ItemStack>> CHEST = TYPES.register("pet_chest",
            () -> AttachmentType.builder(() -> ItemStack.EMPTY).sync(ItemStack.OPTIONAL_STREAM_CODEC).build());

    public static void register(IEventBus bus) { TYPES.register(bus); }
    public static ItemStack get(TamableAnimal pet) { return pet.getData(CHEST); }
    public static void set(TamableAnimal pet, ItemStack stack) { pet.setData(CHEST, stack.copy()); }
}
