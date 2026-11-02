package me.khajiitos.chestedcompanions.common.mixin.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import me.khajiitos.chestedcompanions.common.client.renderer.ChestIconRenderer;
import me.khajiitos.chestedcompanions.common.util.IChestEntity;
import me.khajiitos.chestedcompanions.common.util.IChestEntityRenderState;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import me.khajiitos.chestedcompanions.common.client.renderer.layer.WolfChestLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WolfRenderer.class)
public abstract class WolfRendererMixin extends MobRenderer<Wolf, WolfRenderState, WolfModel> {
    public WolfRendererMixin(EntityRendererProvider.Context $$0, WolfModel $$1, float $$2) {
        super($$0, $$1, $$2);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void chestedCompanions$addLayer(EntityRendererProvider.Context context, CallbackInfo ci) {
        this.addLayer(new WolfChestLayer(this));
    }

    @Override
    public void submit(@NotNull WolfRenderState wolfRenderState, @NotNull PoseStack poseStack,
                       @NotNull SubmitNodeCollector collector, @NotNull CameraRenderState camera) {
        super.submit(wolfRenderState, poseStack, collector, camera);
        if (wolfRenderState instanceof IChestEntityRenderState renderState && renderState.chestedCompanions$shouldRenderChestIcon()) {
            ChestIconRenderer.submit(poseStack, collector, wolfRenderState, camera, wolfRenderState.boundingBoxHeight + 0.25);
        }
    }

    @Inject(at = @At("TAIL"), method = "extractRenderState(Lnet/minecraft/world/entity/animal/wolf/Wolf;Lnet/minecraft/client/renderer/entity/state/WolfRenderState;F)V")
    public void extractRenderState(Wolf wolf, WolfRenderState renderState, float v, CallbackInfo ci) {
        if (wolf instanceof IChestEntity chestEntity && renderState instanceof IChestEntityRenderState chestEntityRenderState) {
            chestEntityRenderState.chestedCompanions$setChestItemStack(chestEntity.chestedCompanions$getChestItemStack());
            chestEntityRenderState.chestedCompanions$setShouldRenderChestIcon(ChestIconRenderer.shouldRender(wolf));
        }
    }
}
