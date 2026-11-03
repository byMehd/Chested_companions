package me.khajiitos.chestedcompanions.common.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import me.khajiitos.chestedcompanions.common.client.renderer.ChestBag;
import me.khajiitos.chestedcompanions.common.client.renderer.PetTorso;
import me.khajiitos.chestedcompanions.common.config.CCConfig;
import me.khajiitos.chestedcompanions.common.mixin.accessor.CatModelAccessor;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class CatChestLayer extends ChestLayer<CatRenderState, EntityModel<CatRenderState>> {
    private static final float BAG_SCALE = 0.6F;
    // The vanilla cat: its torso is a 4x16x6 box, and the chest part sits this far from the middle of it.
    private static final float TORSO_LENGTH = 16.0F;
    private static final float TORSO_HALF_WIDTH = 2.0F;
    private static final float CHEST_FROM_TORSO_Y = -3.0F;
    private static final float CHEST_FROM_TORSO_Z = -11.0F;
    //? if >=26.1 {
    /*// Since 26.1 the baby cat has a model of its own, whose torso is not the adult one scaled down: the bags are placed on
    // the torso measured on it, hung so that their top meets the back line.
    private static final boolean OWN_BABY_MODEL = true;
    private static final float BABY_CHEST_FROM_TORSO_Y = -2.3F;
    *///?} else {
    private static final boolean OWN_BABY_MODEL = false;
    private static final float BABY_CHEST_FROM_TORSO_Y = 0.0F;
    //?}

    public CatChestLayer(RenderLayerParent<CatRenderState, EntityModel<CatRenderState>> renderLayerParent) {
        super(renderLayerParent);
    }

    @Override
    protected ModelPart getParentModelBody() {
        return ((CatModelAccessor)this.getParentModel()).getBody();
    }

    @Override
    protected Vec3i positionLeftChestCube() {
        return new Vec3i(-22, 4, 4);
    }

    @Override
    protected Vec3i positionRightChestCube() {
        return new Vec3i(-22, 4, -7);
    }

    /**
     * Resource packs built on Entity Model Features (Fresh Animations, Fat Cat, Better Cats...) leave the vanilla body
     * part empty and rebuild the cat below it, so the pose of that part no longer says where the torso is.
     */
    private static boolean isRebuilt(ModelPart catBody) {
        return catBody.hasChild("EMF_body") || catBody.isEmpty();
    }

    @Override
    protected void poseModel(CatRenderState renderState) {
        // Entity Model Features animates a rebuilt cat right before the layers run: posing it again would undo that.
        if (!isRebuilt(this.getParentModelBody())) {
            super.poseModel(renderState);
        }
    }

    @Override
    protected void setupPosition(CatRenderState catRenderState, ModelPart chestModelPart) {
        ModelPart catBody = this.getParentModelBody();

        chestModelPart.xRot = HALF_PI;
        chestModelPart.yRot = HALF_PI;
        chestModelPart.zRot = HALF_PI;

        boolean rebuilt = isRebuilt(catBody);
        boolean vanillaBaby = OWN_BABY_MODEL && catRenderState.isBaby && !rebuilt;
        PetTorso torso = rebuilt || vanillaBaby ? PetTorso.find(catBody) : null;
        if (torso != null) {
            // Keep the vanilla layout, but measured from the real torso: a baby, a fat or a rebuilt cat follows.
            float size = Mth.clamp(torso.length() / TORSO_LENGTH, 0.25F, 4.0F);
            chestModelPart.x = torso.x();
            chestModelPart.y = torso.y() + (vanillaBaby ? BABY_CHEST_FROM_TORSO_Y : CHEST_FROM_TORSO_Y * size);
            chestModelPart.z = torso.z() + CHEST_FROM_TORSO_Z * size;
            chestModelPart.xScale = BAG_SCALE * size;
            chestModelPart.yScale = BAG_SCALE * size;
            chestModelPart.zScale = BAG_SCALE * size;
            // A torso wider than the vanilla one would swallow the bags: hang them on its flanks.
            float amount = Math.max(0.0F, torso.halfWidth() - TORSO_HALF_WIDTH * size) / (BAG_SCALE * size);
            if (vanillaBaby) {
                // The kitten's own torso is wider than the adult shape the layout is tuned for, scaled down: spread the bags until
                // the hinge is flush with the side of it. The rule that keeps an open lid off the side of the torso, which runs
                // after this, moves them the hair further that shows the lid instead of burying it in the body.
                float hingeFromMiddle = this.positionLeftChestCube().getZ() - ChestBag.GROW;
                amount = torso.halfWidth() / (BAG_SCALE * size) - hingeFromMiddle;
            }
            spread(chestModelPart, amount);
            return;
        }

        if (catRenderState.isBaby) {
            chestModelPart.x = catBody.x;
            chestModelPart.y = catBody.y;
            chestModelPart.z = catBody.z;
            chestModelPart.xScale = 0.3f;
            chestModelPart.yScale = 0.3f;
            chestModelPart.zScale = 0.3f;
        } else {
            chestModelPart.x = catBody.x;
            chestModelPart.y = catBody.y + 2.f;
            chestModelPart.z = catBody.z;
            chestModelPart.xScale = BAG_SCALE;
            chestModelPart.yScale = BAG_SCALE;
            chestModelPart.zScale = BAG_SCALE;
        }

        // This is the rotation of the body when sitting.
        // Unless I find a better way, I will have to manually
        // adjust the chest's position.
        if (catBody.xRot == HALF_PI / 2) {
            chestModelPart.y += catRenderState.isBaby ? 4.0f : 6.0f;
            chestModelPart.z -= catRenderState.isBaby ? 3.0f : 6.0f;
        }
    }

    @Override
    public void submit(@NotNull PoseStack poseStack, @NotNull SubmitNodeCollector collector, int pPackedLight, @NotNull CatRenderState renderState, float v, float v1) {
        if (!CCConfig.hideCatChest.get()) {
            super.submit(poseStack, collector, pPackedLight, renderState, v, v1);
        }
    }
}
