package me.khajiitos.chestedcompanions.common.mixin.gui;

import me.khajiitos.chestedcompanions.common.client.gui.PetRemoveChestButton;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(at = @At("TAIL"), method = "init")
    private void chestedCompanions$addRemoveChestButton(CallbackInfo ci) {
        ImageButton button = PetRemoveChestButton.create(this, this.leftPos, this.topPos, this.imageWidth);
        if (button != null) {
            this.addRenderableWidget(button);
        }
    }
}
