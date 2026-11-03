package me.khajiitos.chestedcompanions.common.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import me.khajiitos.chestedcompanions.common.config.CCConfig;
import me.khajiitos.chestedcompanions.common.mixin.accessor.WolfModelAccessor;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.core.Vec3i;
import org.jetbrains.annotations.NotNull;

public class WolfChestLayer extends ChestLayer<WolfRenderState, WolfModel> {
    public WolfChestLayer(RenderLayerParent<WolfRenderState, WolfModel> renderLayerParent) {
        super(renderLayerParent);
    }

    @Override
    protected ModelPart getParentModelBody() {
        return ((WolfModelAccessor)this.getParentModel()).getBody();
    }

    @Override
    protected Vec3i positionLeftChestCube() {
        return new Vec3i(-8, -4, 6);
    }

    @Override
    protected Vec3i positionRightChestCube() {
        return new Vec3i(-8, -4, -9);
    }

    @Override
    protected void setupPosition(WolfRenderState wolfRenderState, ModelPart chestModelPart) {
        ModelPart wolfBody = this.getParentModelBody();

        chestModelPart.xRot = HALF_PI;
        chestModelPart.yRot = HALF_PI;
        chestModelPart.zRot = HALF_PI;

        if (wolfRenderState.isBaby) {
            chestModelPart.x = wolfBody.x;
            chestModelPart.y = wolfBody.y + (wolfRenderState.isSitting ? 1.0f : 0.0f);
            //? if >=26.1 {
            /*// Since 26.1 the baby wolf has a body of its own, a 6x4x8 box centred on the part and wider than half the adult one:
            // the bags move out across the pet to hang on its flanks, a hair past them (5.3 units, where 5.0 would be flush with
            // the side of the body): an open lid lies in the plane of the hinge, which would flicker against the body's side.
            chestModelPart.z = wolfBody.z;
            spread(chestModelPart, 5.3f);
            *///?} else {
            chestModelPart.z = wolfBody.z - 1.f;
            //?}
            chestModelPart.xScale = 0.3f;
            chestModelPart.yScale = 0.3f;
            chestModelPart.zScale = 0.3f;
        } else {
            chestModelPart.x = wolfBody.x;
            chestModelPart.y = wolfBody.y;
            chestModelPart.z = wolfBody.z;
            chestModelPart.xScale = 0.6f;
            chestModelPart.yScale = 0.6f;
            chestModelPart.zScale = 0.6f;
        }
    }

    @Override
    public void submit(@NotNull PoseStack poseStack, @NotNull SubmitNodeCollector collector, int pPackedLight, @NotNull WolfRenderState renderState, float v, float v1) {
        if (!CCConfig.hideWolfChest.get()) {
            super.submit(poseStack, collector, pPackedLight, renderState, v, v1);
        }
    }
}
