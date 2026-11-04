package me.khajiitos.chestedcompanions.common;

//? if >=26.1 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import me.khajiitos.chestedcompanions.common.client.renderer.ChestBag;
import me.khajiitos.chestedcompanions.common.client.renderer.PetTorso;
import me.khajiitos.chestedcompanions.common.client.renderer.layer.CatChestLayer;
import me.khajiitos.chestedcompanions.common.client.renderer.layer.WolfChestLayer;
import net.minecraft.SharedConstants;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.feline.BabyCatModel;
import net.minecraft.client.model.animal.feline.BabyFelineModel;
import net.minecraft.client.model.animal.wolf.BabyWolfModel;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.server.Bootstrap;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Since 26.1 kittens and pups have models of their own, with torsos wider than the adult ones scaled down. An open lid lies in
// the plane of its hinge, so that plane has to clear the side of the torso: inside it, only the upper edge of the lid shows
// above the back, and on it the lid flickers against the body. The layers are placed as the game places them, on the real
// models, standing and sitting.
class BabyPlacementTest {
    // The layers as the game builds them, except that they find the body part of their model where the test put it.
    private static final class PlacedCatLayer extends CatChestLayer {
        private final ModelPart body;

        PlacedCatLayer(EntityModel<CatRenderState> model, ModelPart body) {
            super(() -> model);
            this.body = body;
        }

        @Override
        protected ModelPart getParentModelBody() {
            return this.body;
        }

        // The chest part the layer draws, both lids fully open, placed on this cat the way submit places it.
        ModelPart place(CatRenderState state) {
            ChestBag left = new ChestBag(this.positionLeftChestCube(), false, 0, 0, 22, 11);
            ChestBag right = new ChestBag(this.positionRightChestCube(), true, 0, 0, 22, 11);
            ModelPart chest = openChest(left, right);
            this.setupPosition(state, chest);
            keepLidsOffTheTorso(left, chest, PetTorso.find(this.getParentModelBody()));
            return chest;
        }
    }

    private static final class PlacedWolfLayer extends WolfChestLayer {
        private final ModelPart body;

        PlacedWolfLayer(WolfModel model, ModelPart body) {
            super(() -> model);
            this.body = body;
        }

        @Override
        protected ModelPart getParentModelBody() {
            return this.body;
        }

        // The chest part the layer draws, both lids fully open, placed on this wolf the way submit places it.
        ModelPart place(WolfRenderState state) {
            ChestBag left = new ChestBag(this.positionLeftChestCube(), false, 0, 0, 22, 11);
            ChestBag right = new ChestBag(this.positionRightChestCube(), true, 0, 0, 22, 11);
            ModelPart chest = openChest(left, right);
            this.setupPosition(state, chest);
            keepLidsOffTheTorso(left, chest, PetTorso.find(this.getParentModelBody()));
            return chest;
        }
    }

    private static ModelPart openChest(ChestBag left, ChestBag right) {
        float open = ChestBag.lidAngle(1.0F);
        return new ModelPart(List.of(), Map.of(
                "left_body", left.createBody(), "left_lid", left.createLid(open),
                "right_body", right.createBody(), "right_lid", right.createLid(open)));
    }

    // Smallest and largest position across the pet (x), in model units, of every part of the chest part, by part name.
    private static Map<String, float[]> acrossThePet(ModelPart chest) {
        Map<String, float[]> ranges = new HashMap<>();
        Vector3f at = new Vector3f();
        chest.visit(new PoseStack(), (pose, path, index, cube) -> {
            Matrix4fc m = pose.pose();
            float[] range = ranges.computeIfAbsent(path.substring(path.lastIndexOf('/') + 1), name -> new float[]{Float.MAX_VALUE, -Float.MAX_VALUE});
            for (ModelPart.Polygon polygon : cube.polygons) {
                for (ModelPart.Vertex vertex : polygon.vertices()) {
                    m.transformPosition(vertex.x() / 16.0F, vertex.y() / 16.0F, vertex.z() / 16.0F, at);
                    range[0] = Math.min(range[0], at.x * 16.0F);
                    range[1] = Math.max(range[1], at.x * 16.0F);
                }
            }
        });
        return ranges;
    }

    private static void assertBagsHangOutsideTheTorso(ModelPart root, ModelPart chest) {
        PetTorso torso = PetTorso.find(root.getChild("body"));
        assertNotNull(torso);
        Map<String, float[]> across = acrossThePet(chest);

        // The face of an open lid that shows is the one in the plane of the hinge, which is also where the bag starts.
        float leftPlane = across.get("left_lid")[1];
        float rightPlane = across.get("right_lid")[0];
        assertEquals(leftPlane, across.get("left_body")[0], 1.0E-3F, "the closed bag starts at its hinge");
        assertEquals(rightPlane, across.get("right_body")[1], 1.0E-3F);
        float leftGap = leftPlane - (torso.x() + torso.halfWidth());
        float rightGap = (torso.x() - torso.halfWidth()) - rightPlane;
        // Outside the torso, so that the lid shows, but only a hair: the bags hang on the pet, not beside it.
        assertTrue(leftGap > 0.0F && leftGap < 0.15F, "left lid plane is " + leftGap + " outside the torso");
        assertTrue(rightGap > 0.0F && rightGap < 0.15F, "right lid plane is " + rightGap + " outside the torso");
        // And no side of any other part of the model, a leg or the head, lies in the plane of a face of the open lid either.
        float closest = ChestBagTest.closestSidePlane(ChestBagTest.posedBoxes(chest, "_lid"), ChestBagTest.posedBoxes(root, ""));
        assertTrue(closest >= 0.05F, "the open lid is " + closest + " from a side plane of the model");
    }

    private static void assertKittenBagsHangOutsideTheTorso(boolean sitting) {
        ModelPart root = BabyFelineModel.createBabyLayer().bakeRoot();
        BabyCatModel model = new BabyCatModel(root);
        CatRenderState state = new CatRenderState();
        state.isBaby = true;
        state.isSitting = sitting;
        model.setupAnim(state);
        assertBagsHangOutsideTheTorso(root, new PlacedCatLayer(model, root.getChild("body")).place(state));
    }

    private static void assertPupBagsHangOutsideTheTorso(boolean sitting) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ModelPart root = BabyWolfModel.createBodyLayer().bakeRoot();
        BabyWolfModel model = new BabyWolfModel(root);
        WolfRenderState state = new WolfRenderState();
        state.isBaby = true;
        state.isSitting = sitting;
        model.setupAnim(state);
        assertBagsHangOutsideTheTorso(root, new PlacedWolfLayer(model, root.getChild("body")).place(state));
    }

    @Test void theBagsOfAStandingKittenHangOutsideItsTorso() {
        assertKittenBagsHangOutsideTheTorso(false);
    }

    @Test void theBagsOfASittingKittenHangOutsideItsTorso() {
        assertKittenBagsHangOutsideTheTorso(true);
    }

    @Test void theBagsOfAStandingPupHangOutsideItsTorso() {
        assertPupBagsHangOutsideTheTorso(false);
    }

    @Test void theBagsOfASittingPupHangOutsideItsTorso() {
        assertPupBagsHangOutsideTheTorso(true);
    }
}
*///?}
