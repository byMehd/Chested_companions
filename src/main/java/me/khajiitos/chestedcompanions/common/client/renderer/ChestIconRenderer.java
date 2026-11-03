package me.khajiitos.chestedcompanions.common.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.khajiitos.chestedcompanions.common.ChestedCompanions;
import me.khajiitos.chestedcompanions.common.config.CCConfig;
import me.khajiitos.chestedcompanions.common.util.IChestEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class ChestIconRenderer {
    private static final Identifier CHEST_ICON_LOCATION = Identifier.fromNamespaceAndPath("chestedcompanions", "textures/chest_icon.png");
    private static final float SCALE = 1.f / 64.f;

    public static <T extends LivingEntity> boolean shouldRender(T entity) {
        if (entity != Minecraft.getInstance().getEntityRenderDispatcher().crosshairPickEntity) {
            return false;
        }

        if (!(entity instanceof IChestEntity chestEntity) || !chestEntity.chestedCompanions$hasChest()) {
            return false;
        }

        if (!Minecraft.renderNames()) {
            return false;
        }

        if (entity instanceof Cat) {
            return CCConfig.showChestIconOnCats.get();
        } else if (entity instanceof Wolf) {
            return CCConfig.showChestIconOnWolves.get();
        }

        return false;
    }

    public static void submit(@NotNull PoseStack poseStack, @NotNull net.minecraft.client.renderer.SubmitNodeCollector collector,
                              @NotNull LivingEntityRenderState state, net.minecraft.client.renderer.state.CameraRenderState camera, double yOffset) {
        poseStack.pushPose();
        poseStack.translate(0.0, yOffset, 0.0);
        poseStack.mulPose(camera.orientation);
        poseStack.scale(-SCALE, -SCALE, SCALE);
        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.f);
        int light = state.lightCoords;
        collector.submitCustomGeometry(poseStack, PetRenderTypes.entityCutout(CHEST_ICON_LOCATION), (pose, vertices) -> {
            Matrix4f matrix = pose.pose();
            vertices.addVertex(matrix, 0, 0, 0).setColor(-1).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            vertices.addVertex(matrix, 0, 16, 0).setColor(-1).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            vertices.addVertex(matrix, 16, 16, 0).setColor(-1).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            vertices.addVertex(matrix, 16, 0, 0).setColor(-1).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        });
        poseStack.popPose();
    }
}
