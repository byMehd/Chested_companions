package me.khajiitos.chestedcompanions.common.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import me.khajiitos.chestedcompanions.common.client.renderer.ChestBag;
import me.khajiitos.chestedcompanions.common.client.renderer.PetRenderTypes;
import me.khajiitos.chestedcompanions.common.util.IChestEntityRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public abstract class ChestLayer<T extends LivingEntityRenderState, M extends EntityModel<T>> extends RenderLayer<T, M> {
    protected static final float HALF_PI = (float)Math.PI / 2.f;
    private static final Identifier BASE_CHEST_LOCATION = Identifier.fromNamespaceAndPath("chestedcompanions", "textures/chest/chest.png");

    private static @Nullable String copperTexture(ItemStack chestItem) {
        Identifier item = BuiltInRegistries.ITEM.getKey(chestItem.getItem());
        if (!item.getNamespace().equals("minecraft")) return null;
        String path = item.getPath();
        if (path.startsWith("waxed_")) path = path.substring("waxed_".length());
        return switch (path) {
            case "copper_chest" -> "copper";
            case "exposed_copper_chest" -> "copper_exposed";
            case "weathered_copper_chest" -> "copper_weathered";
            case "oxidized_copper_chest" -> "copper_oxidized";
            default -> null;
        };
    }

    private static Identifier getTexture(@Nullable ItemStack chestItem) {
        if (chestItem == null || chestItem.isEmpty()) return BASE_CHEST_LOCATION;
        if (chestItem.is(Items.ENDER_CHEST)) {
            return Identifier.fromNamespaceAndPath("chestedcompanions", "textures/chest/special/ender.png");
        }
        String copperTexture = copperTexture(chestItem);
        if (copperTexture != null) {
            return Identifier.fromNamespaceAndPath("chestedcompanions", "textures/chest/special/" + copperTexture + ".png");
        }
        Identifier item = BuiltInRegistries.ITEM.getKey(chestItem.getItem());
        if (item.getNamespace().equals("lolmcv") && item.getPath().equals("pale_oak_chest")) {
            return Identifier.fromNamespaceAndPath("lolmcv", "textures/entity/chest/pale_oak.png");
        }
        Identifier texture = Identifier.fromNamespaceAndPath("chestedcompanions", "textures/chest/"
                + (item.getNamespace().equals("minecraft") ? "" : item.getNamespace() + "/") + item.getPath() + ".png");
        // Query the current resource manager, so F3+T immediately applies texture-pack changes.
        return Minecraft.getInstance().getResourceManager().getResource(texture).isPresent() ? texture : BASE_CHEST_LOCATION;
    }

    private final ChestBag[] bags;
    private final ChestBag[] atlasBags;

    public ChestLayer(RenderLayerParent<T, M> renderLayerParent) {
        super(renderLayerParent);

        Vec3i positionLeftChestCube = this.positionLeftChestCube();
        Vec3i positionRightChestCube = this.positionRightChestCube();

        this.bags = new ChestBag[] {
                new ChestBag(positionLeftChestCube, false, 0, 0, 22, 11),
                new ChestBag(positionRightChestCube, true, 0, 0, 22, 11)
        };
        // Pale oak MCV retains a 64x64 vanilla-style chest atlas. Select its body/front
        // area for these small side chests.
        this.atlasBags = new ChestBag[] {
                new ChestBag(positionLeftChestCube, false, 33, 30, 64, 64),
                new ChestBag(positionRightChestCube, true, 33, 30, 64, 64)
        };
    }

    protected abstract ModelPart getParentModelBody();
    protected abstract void setupPosition(T renderState, ModelPart chestModelPart);

    protected abstract Vec3i positionLeftChestCube();
    protected abstract Vec3i positionRightChestCube();

    /** Poses the parent model for this pet, the way the main model is posed. */
    protected void poseModel(T renderState) {
        this.getParentModel().setupAnim(renderState);
    }

    /** Moves the two bags of a chest part apart across the pet by {@code amount} (closer when negative). */
    protected static void spread(ModelPart chestModelPart, float amount) {
        chestModelPart.getChild("left_body").z += amount;
        chestModelPart.getChild("left_lid").z += amount;
        chestModelPart.getChild("right_body").z -= amount;
        chestModelPart.getChild("right_lid").z -= amount;
    }

    @Override
    public void submit(@NotNull PoseStack poseStack, @NotNull SubmitNodeCollector collector, int pPackedLight, @NotNull T renderState, float v, float v1) {
        if (!(renderState instanceof IChestEntityRenderState chestEntityRenderState) || !chestEntityRenderState.chestedCompanions$hasChest()) {
            return;
        }

        if (renderState.isInvisible) return;
        // Rendering is deferred in 1.21.11. Snapshot this pet's transform in its own part.
        this.poseModel(renderState);
        ItemStack chest = chestEntityRenderState.chestedCompanions$getChestItemStack();
        Identifier item = BuiltInRegistries.ITEM.getKey(chest.getItem());
        boolean atlas = item.getNamespace().equals("lolmcv") && item.getPath().equals("pale_oak_chest");
        ChestBag[] sides = atlas ? this.atlasBags : this.bags;
        float angle = ChestBag.lidAngle(chestEntityRenderState.chestedCompanions$getLidOpenness());
        ModelPart part = new ModelPart(List.of(), Map.of(
                "left_body", sides[0].createBody(), "left_lid", sides[0].createLid(angle),
                "right_body", sides[1].createBody(), "right_lid", sides[1].createLid(angle)));
        this.setupPosition(renderState, part);
        collector.submitModelPart(part, poseStack,
                PetRenderTypes.entityCutoutNoCull(getTexture(chestEntityRenderState.chestedCompanions$getChestItemStack())),
                pPackedLight, LivingEntityRenderer.getOverlayCoords(renderState, 0.f), null);

    }
}
