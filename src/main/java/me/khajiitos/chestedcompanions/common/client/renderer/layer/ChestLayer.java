package me.khajiitos.chestedcompanions.common.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import me.khajiitos.chestedcompanions.common.ChestedCompanions;
import me.khajiitos.chestedcompanions.common.util.IChestEntityRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public abstract class ChestLayer<T extends LivingEntityRenderState, M extends EntityModel<T>> extends RenderLayer<T, M> {
    protected static final float HALF_PI = (float)Math.PI / 2.f;
    private static final Identifier BASE_CHEST_LOCATION = Identifier.fromNamespaceAndPath("chestedcompanions", "textures/chest/chest.png");
    private static Identifier getResourceLocation(@Nullable ItemStack chestItem) {
        if (chestItem == null || chestItem.isEmpty()) return BASE_CHEST_LOCATION;
        if (chestItem.is(Items.ENDER_CHEST)) return Identifier.withDefaultNamespace("textures/entity/chest/ender.png");
        Identifier item = BuiltInRegistries.ITEM.getKey(chestItem.getItem());
        if (item.getNamespace().equals("lolmcv") && item.getPath().equals("pale_oak_chest")) {
            return Identifier.fromNamespaceAndPath("lolmcv", "textures/entity/chest/pale_oak.png");
        }
        Identifier texture = Identifier.fromNamespaceAndPath("chestedcompanions", "textures/chest/"
                + (item.getNamespace().equals("minecraft") ? "" : item.getNamespace() + "/") + item.getPath() + ".png");
        // Query the current resource manager, so F3+T immediately applies texture-pack changes.
        return Minecraft.getInstance().getResourceManager().getResource(texture).isPresent() ? texture : BASE_CHEST_LOCATION;
    }

    private final List<ModelPart.Cube> cubes;
    private final List<ModelPart.Cube> atlasCubes;

    public ChestLayer(RenderLayerParent<T, M> renderLayerParent) {
        super(renderLayerParent);

        Vec3i positionLeftChestCube = this.positionLeftChestCube();
        Vec3i positionRightChestCube = this.positionRightChestCube();

        ModelPart.Cube left = new ModelPart.Cube(
                0, 0,
                positionLeftChestCube.getX(), positionLeftChestCube.getY(), positionLeftChestCube.getZ(),
                8, 8, 3,
                1.f, 1.f, 1.f,
                false,
                22, 11,
                Set.of(Direction.values())
        );

        ModelPart.Cube right = new ModelPart.Cube(
                0, 0,
                positionRightChestCube.getX(), positionRightChestCube.getY(), positionRightChestCube.getZ(),
                8, 8, 3,
                1.f, 1.f, 1.f,
                true,
                22, 11,
                Set.of(Direction.values())
        );

        this.cubes = List.of(left, right);
        this.atlasCubes = List.of(
                new ModelPart.Cube(0, 19, positionLeftChestCube.getX(), positionLeftChestCube.getY(), positionLeftChestCube.getZ(),
                        8, 8, 3, 1.f, 1.f, 1.f, false, 64, 64, Set.of(Direction.values())),
                new ModelPart.Cube(0, 19, positionRightChestCube.getX(), positionRightChestCube.getY(), positionRightChestCube.getZ(),
                        8, 8, 3, 1.f, 1.f, 1.f, true, 64, 64, Set.of(Direction.values())));
    }

    protected abstract ModelPart getParentModelBody();
    protected abstract void setupPosition(T renderState, ModelPart chestModelPart);

    protected abstract Vec3i positionLeftChestCube();
    protected abstract Vec3i positionRightChestCube();

    @Override
    public void submit(@NotNull PoseStack poseStack, @NotNull SubmitNodeCollector collector, int pPackedLight, @NotNull T renderState, float v, float v1) {
        if (!(renderState instanceof IChestEntityRenderState chestEntityRenderState) || !chestEntityRenderState.chestedCompanions$hasChest()) {
            return;
        }

        if (renderState.isInvisible) return;
        // Rendering is deferred in 1.21.11. Snapshot this pet's transform in its own part.
        this.getParentModel().setupAnim(renderState);
        ItemStack chest = chestEntityRenderState.chestedCompanions$getChestItemStack();
        Identifier item = BuiltInRegistries.ITEM.getKey(chest.getItem());
        boolean atlas = chest.is(Items.ENDER_CHEST) || (item.getNamespace().equals("lolmcv") && item.getPath().equals("pale_oak_chest"));
        ModelPart part = new ModelPart(atlas ? this.atlasCubes : this.cubes, new HashMap<>());
        this.setupPosition(renderState, part);
        collector.submitModelPart(part, poseStack,
                RenderTypes.entityCutoutNoCull(getResourceLocation(chestEntityRenderState.chestedCompanions$getChestItemStack())),
                pPackedLight, LivingEntityRenderer.getOverlayCoords(renderState, 0.f), null);

    }
}
