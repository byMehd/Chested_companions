package me.khajiitos.chestedcompanions.common;

import com.mojang.blaze3d.vertex.PoseStack;
import me.khajiitos.chestedcompanions.common.client.renderer.ChestBag;
import me.khajiitos.chestedcompanions.common.client.renderer.PetTorso;
import me.khajiitos.chestedcompanions.common.client.renderer.layer.ChestLayer;
import net.minecraft.SharedConstants;
import net.minecraft.client.model.geom.LayerDefinitions;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.core.Vec3i;
import net.minecraft.server.Bootstrap;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The hinged bag must look exactly like the single box older versions drew whenever its lid is shut, and its open lid
 * must never lie in the same plane as a side of the pet.
 */
class ChestBagTest {
    private static final Vec3i[] ORIGINS = {
            new Vec3i(-8, -4, 6), new Vec3i(-8, -4, -9),     // wolf, left and right
            new Vec3i(-22, 4, 4), new Vec3i(-22, 4, -7),     // cat, left and right
    };
    static final float OPEN = (float) Math.PI / 2.0F;

    /** min x, y, z then max x, y, z of the vertices of a cube, once moved by the given part pivot. */
    private static float[] extents(ModelPart.Cube cube, float pivotX, float pivotY, float pivotZ) {
        float[] box = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (ModelPart.Polygon polygon : cube.polygons) {
            for (ModelPart.Vertex vertex : polygon.vertices()) {
                float[] at = {vertex.x() + pivotX, vertex.y() + pivotY, vertex.z() + pivotZ};
                for (int axis = 0; axis < 3; axis++) {
                    box[axis] = Math.min(box[axis], at[axis]);
                    box[axis + 3] = Math.max(box[axis + 3], at[axis]);
                }
            }
        }
        return box;
    }

    @Test void closedLidAndBodyFillTheOriginalBoxWithoutGapOrOverlap() {
        for (Vec3i origin : ORIGINS) {
            ChestBag bag = new ChestBag(origin, origin.getZ() < 0, 0, 0, 22, 11);
            ModelPart lid = bag.createLid(0.0F);
            float[] lidBox = extents(bag.lidCube(), lid.x, lid.y, lid.z);
            float[] bodyBox = extents(bag.bodyCube(), 0, 0, 0);
            float x0 = origin.getX() - 1, y0 = origin.getY() - 1, z0 = origin.getZ() - 1;
            assertArrayEquals(new float[]{x0, y0, z0, x0 + 10, y0 + 3.75F, z0 + 5}, lidBox, 1.0E-4F, "lid " + origin);
            assertArrayEquals(new float[]{x0, y0 + 3.75F, z0, x0 + 10, y0 + 10, z0 + 5}, bodyBox, 1.0E-4F, "body " + origin);
        }
    }

    @Test void theHingeSitsOnTheEdgeFacingThePetAndTheLidRisesAwayFromIt() {
        ChestBag left = new ChestBag(new Vec3i(-8, -4, 6), false, 0, 0, 22, 11);
        ChestBag right = new ChestBag(new Vec3i(-8, -4, -9), true, 0, 0, 22, 11);
        ModelPart leftLid = left.createLid(1.0F);
        ModelPart rightLid = right.createLid(1.0F);
        assertEquals(5.0F, leftLid.z, 1.0E-4F, "left bag: inner edge is its smallest z");
        assertEquals(-5.0F, rightLid.z, 1.0E-4F, "right bag: inner edge is its largest z");
        assertEquals(-5.0F + 3.75F, leftLid.y, 1.0E-4F, "hinge at the seam between lid and body");
        assertTrue(leftLid.xRot > 0.0F && rightLid.xRot < 0.0F, "the two lids swing in opposite directions");
        assertEquals(leftLid.xRot, -rightLid.xRot, 1.0E-6F);
        assertEquals(0.0F, left.createLid(0.0F).xRot);
    }

    @Test void theLidUsesTheTopRowsOfTheTextureAndTheBodyTheRest() {
        ChestBag bag = new ChestBag(new Vec3i(-8, -4, 6), false, 0, 0, 22, 11);
        float[] lidV = textureRows(bag.lidCube(), false);
        float[] bodyV = textureRows(bag.bodyCube(), false);
        assertEquals(3.0F / 11.0F, lidV[0], 1.0E-5F, "lid faces start under the 3 top strip rows");
        assertEquals(6.0F / 11.0F, lidV[1], 1.0E-5F, "lid faces cover texture rows 3..5");
        assertEquals(6.0F / 11.0F, bodyV[0], 1.0E-5F, "body faces start at texture row 6");
        assertEquals(1.0F, bodyV[1], 1.0E-5F, "body faces end at the bottom of the 11 row texture");
    }

    /** Smallest and largest v of the vertical (side) faces of a cube; the top and bottom faces are skipped. */
    private static float[] textureRows(ModelPart.Cube cube, boolean includeHorizontal) {
        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
        for (ModelPart.Polygon polygon : cube.polygons) {
            if (!includeHorizontal && Math.abs(polygon.normal().y()) > 0.5F) continue;
            for (ModelPart.Vertex vertex : polygon.vertices()) {
                min = Math.min(min, vertex.v());
                max = Math.max(max, vertex.v());
            }
        }
        return new float[]{min, max};
    }

    @Test void lidEasingMatchesTheVanillaChest() {
        assertEquals(0.0F, ChestBag.lidAngle(0.0F));
        assertEquals((float) Math.PI / 2.0F, ChestBag.lidAngle(1.0F), 1.0E-6F);
        assertEquals(0.875F * ((float) Math.PI / 2.0F), ChestBag.lidAngle(0.5F), 1.0E-6F, "1 - (1 - 0.5)^3");
    }

    @Test void bodyAndLidCubesNeverShareTheirTextureRows() {
        ChestBag atlas = new ChestBag(new Vec3i(-8, -4, 6), false, 33, 30, 64, 64);
        float[] lidV = textureRows(atlas.lidCube(), false);
        float[] bodyV = textureRows(atlas.bodyCube(), false);
        assertEquals(33.0F / 64.0F, lidV[0], 1.0E-5F);
        assertEquals(36.0F / 64.0F, lidV[1], 1.0E-5F);
        assertEquals(36.0F / 64.0F, bodyV[0], 1.0E-5F);
        assertEquals(41.0F / 64.0F, bodyV[1], 1.0E-5F);
    }

    /** Smallest and largest z of the vertices of the open lid, in the space of its chest part (the lid turns about x). */
    private static float[] lidSpanAcross(ChestBag bag) {
        ModelPart lid = bag.createLid(OPEN);
        float sin = (float) Math.sin(lid.xRot);
        float cos = (float) Math.cos(lid.xRot);
        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
        for (ModelPart.Polygon polygon : bag.lidCube().polygons) {
            for (ModelPart.Vertex vertex : polygon.vertices()) {
                float z = vertex.y() * sin + vertex.z() * cos + lid.z;
                min = Math.min(min, z);
                max = Math.max(max, z);
            }
        }
        return new float[]{min, max};
    }

    @Test void anOpenLidStandsInThePlaneOfItsHingeOnTheSideOfThePet() {
        for (Vec3i origin : ORIGINS) {
            ChestBag bag = new ChestBag(origin, origin.getZ() < 0, 0, 0, 22, 11);
            float[] span = lidSpanAcross(bag);
            float outer = origin.getZ() > 0 ? span[1] : -span[0];
            assertEquals(bag.hingeDistance(), outer, 1.0E-4F, "the open lid reaches out to its hinge plane and no further: " + origin);
            assertEquals(3.75F, span[1] - span[0], 1.0E-4F, "and is as thick as it is tall when shut: " + origin);
        }
    }

    @Test void bothBagsOfAPetHangTheSameDistanceFromItsMiddleWhateverTheTexture() {
        for (int pet = 0; pet < ORIGINS.length; pet += 2) {
            ChestBag left = new ChestBag(ORIGINS[pet], false, 0, 0, 22, 11);
            ChestBag right = new ChestBag(ORIGINS[pet + 1], true, 0, 0, 22, 11);
            assertEquals(left.hingeDistance(), right.hingeDistance(), 1.0E-6F);
            // The pale oak chest's atlas bags: other texture rows, the same geometry.
            ChestBag atlas = new ChestBag(ORIGINS[pet], false, 33, 30, 64, 64);
            assertEquals(left.hingeDistance(), atlas.hingeDistance(), 1.0E-6F);
            assertArrayEquals(extents(left.lidCube(), 0, 0, 0), extents(atlas.lidCube(), 0, 0, 0), 1.0E-6F);
            assertArrayEquals(extents(left.bodyCube(), 0, 0, 0), extents(atlas.bodyCube(), 0, 0, 0), 1.0E-6F);
        }
        assertEquals(5.0F, new ChestBag(ORIGINS[0], false, 0, 0, 22, 11).hingeDistance(), 1.0E-6F, "wolf");
        assertEquals(3.0F, new ChestBag(ORIGINS[2], false, 0, 0, 22, 11).hingeDistance(), 1.0E-6F, "cat");
    }

    @Test void aPlaneOnTheSideOfTheTorsoMovesOutByTheClearance() {
        // The adult wolf: bags drawn at 0.6, hinge planes at 5 * 0.6 = 3, torso 3 from its middle.
        assertEquals(ChestBag.LID_CLEARANCE, ChestBag.spreadToClear(3.0F, 3.0F, 0.6F), 1.0E-5F);
        // The pup: 5 * 0.3 = 1.5 against a torso half as wide.
        assertEquals(ChestBag.LID_CLEARANCE, ChestBag.spreadToClear(1.5F, 1.5F, 0.3F), 1.0E-5F);
        // A plane just inside the side is brought out past it, to the same gap.
        float scale = 0.6F;
        assertEquals(3.0F + ChestBag.LID_CLEARANCE * scale, 2.95F + ChestBag.spreadToClear(2.95F, 3.0F, scale) * scale, 1.0E-5F);
    }

    @Test void aPlaneThatIsClearOfTheTorsoStaysWhereItIs() {
        assertEquals(0.0F, ChestBag.spreadToClear(1.8F, 1.6F, 0.6F), "the vanilla cat, bags 0.2 outside its drawn torso");
        assertEquals(0.0F, ChestBag.spreadToClear(0.9F, 0.8F, 0.3F), "the vanilla kitten");
        assertEquals(0.0F, ChestBag.spreadToClear(4.0F, 3.0F, 0.6F), "a bag that floats well away from the pet");
        assertEquals(0.0F, ChestBag.spreadToClear(2.0F, 3.0F, 0.6F), "a bag buried in a wide torso, its open lid hidden");
        // The cat layer hangs the bags of a rebuilt cat 0.2 * size inside the side of the torso, with the bags at 0.6 * size.
        for (float size : new float[]{0.25F, 0.5F, 1.0F, 2.0F, 4.0F}) {
            assertEquals(0.0F, ChestBag.spreadToClear(5.0F * size - 0.2F * size, 5.0F * size, 0.6F * size), "size " + size);
        }
    }

    @Test void noPlaneStaysCloserToTheSideOfTheTorsoThanTheClearance() {
        for (float scale : new float[]{0.15F, 0.3F, 0.6F, 1.2F, 2.4F}) {
            float gap = ChestBag.LID_CLEARANCE * scale;
            for (float halfWidth = 0.5F; halfWidth < 12.0F; halfWidth += 0.1F) {
                for (float plane = halfWidth - 3.0F * gap; plane < halfWidth + 3.0F * gap; plane += gap / 8.0F) {
                    String what = "scale " + scale + ", torso " + halfWidth + ", plane " + plane;
                    float moved = plane + ChestBag.spreadToClear(plane, halfWidth, scale) * scale;
                    assertTrue(Math.abs(moved - halfWidth) >= gap - 1.0E-4F, "still too close: " + what);
                    assertTrue(moved >= plane, "the bags only ever move apart: " + what);
                    assertTrue(moved - plane <= 2.0F * gap + 1.0E-4F, "and never by more than twice the clearance: " + what);
                }
            }
        }
    }

    /** Every box of the cubes whose path holds {@code only}, as min x, y, z then max x, y, z in model units, once posed. */
    static List<float[]> posedBoxes(ModelPart root, String only) {
        List<float[]> boxes = new ArrayList<>();
        root.visit(new PoseStack(), (pose, path, index, cube) -> {
            if (!path.contains(only)) return;
            Matrix4fc m = pose.pose();
            // Parts turned off the axes have no flat side to compare with.
            float[][] rotation = {{m.m00(), m.m10(), m.m20()}, {m.m01(), m.m11(), m.m21()}, {m.m02(), m.m12(), m.m22()}};
            for (float[] row : rotation) {
                int used = 0;
                for (float entry : row) used += Math.abs(entry) > 1.0E-3F ? 1 : 0;
                if (used != 1) return;
            }
            float[] box = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
            for (ModelPart.Polygon polygon : cube.polygons) {
                for (ModelPart.Vertex vertex : polygon.vertices()) {
                    Vector3f at = m.transformPosition(vertex.x() / 16.0F, vertex.y() / 16.0F, vertex.z() / 16.0F, new Vector3f()).mul(16.0F);
                    float[] point = {at.x, at.y, at.z};
                    for (int axis = 0; axis < 3; axis++) {
                        box[axis] = Math.min(box[axis], point[axis]);
                        box[axis + 3] = Math.max(box[axis + 3], point[axis]);
                    }
                }
            }
            boxes.add(box);
        });
        return boxes;
    }

    /** Closest a face of the open lid, across the pet, comes to such a face of a box of the model that it overlaps. */
    static float closestSidePlane(List<float[]> lid, List<float[]> model) {
        float closest = Float.MAX_VALUE;
        for (float[] a : lid) {
            for (float[] b : model) {
                boolean overlapsUpDown = Math.min(a[4], b[4]) - Math.max(a[1], b[1]) > 1.0E-3F;
                boolean overlapsAlong = Math.min(a[5], b[5]) - Math.max(a[2], b[2]) > 1.0E-3F;
                if (!overlapsUpDown || !overlapsAlong) continue;
                for (float lidPlane : new float[]{a[0], a[3]}) {
                    for (float modelPlane : new float[]{b[0], b[3]}) {
                        closest = Math.min(closest, Math.abs(lidPlane - modelPlane));
                    }
                }
            }
        }
        return closest;
    }

    /**
     * Where the layers put the chest part: relative to the body part, at this scale, with the bags already moved this far
     * apart (see WolfChestLayer and CatChestLayer).
     */
    private record Pet(String name, ModelLayerLocation layer, Vec3i left, Vec3i right, float dy, float dz, float scale, float spread, boolean flushWithTorso) {}

    @Test void anOpenLidNeverLiesInTheSidePlaneOfAnyPartOfTheVanillaPets() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Map<ModelLayerLocation, LayerDefinition> layers = LayerDefinitions.createRoots();
        //? if >=26.1 {
        /*// Since 26.1 a pup has a body of its own and its bags hang flush against its flanks, at 5 units apart; the kitten is
        // placed from its measured torso, which BabyCatPlacementTest checks on the real layer.
        List<Pet> pets = List.of(
                new Pet("wolf", ModelLayers.WOLF, ORIGINS[0], ORIGINS[1], 0.0F, 0.0F, 0.6F, 0.0F, true),
                new Pet("pup", ModelLayers.WOLF_BABY, ORIGINS[0], ORIGINS[1], 0.0F, 0.0F, 0.3F, 5.0F, true),
                new Pet("sitting pup", ModelLayers.WOLF_BABY, ORIGINS[0], ORIGINS[1], 1.0F, 0.0F, 0.3F, 5.0F, true),
                new Pet("cat", ModelLayers.CAT, ORIGINS[2], ORIGINS[3], 2.0F, 0.0F, 0.6F, 0.0F, false));
        *///?} else {
        List<Pet> pets = List.of(
                new Pet("wolf", ModelLayers.WOLF, ORIGINS[0], ORIGINS[1], 0.0F, 0.0F, 0.6F, 0.0F, true),
                new Pet("pup", ModelLayers.WOLF_BABY, ORIGINS[0], ORIGINS[1], 0.0F, -1.0F, 0.3F, 0.0F, true),
                new Pet("sitting pup", ModelLayers.WOLF_BABY, ORIGINS[0], ORIGINS[1], 1.0F, -1.0F, 0.3F, 0.0F, true),
                new Pet("cat", ModelLayers.CAT, ORIGINS[2], ORIGINS[3], 2.0F, 0.0F, 0.6F, 0.0F, false),
                new Pet("kitten", ModelLayers.CAT_BABY, ORIGINS[2], ORIGINS[3], 0.0F, 0.0F, 0.3F, 0.0F, false));
        //?}
        for (Pet pet : pets) {
            ModelPart root = layers.get(pet.layer()).bakeRoot();
            ModelPart body = root.getChild("body");
            ChestBag left = new ChestBag(pet.left(), false, 0, 0, 22, 11);
            ChestBag right = new ChestBag(pet.right(), true, 0, 0, 22, 11);
            ModelPart chest = new ModelPart(List.of(), Map.of(
                    "left_body", left.createBody(), "left_lid", left.createLid(OPEN),
                    "right_body", right.createBody(), "right_lid", right.createLid(OPEN)));
            chest.setPos(body.x, body.y + pet.dy(), body.z + pet.dz());
            chest.setRotation(OPEN, OPEN, OPEN);
            chest.xScale = chest.yScale = chest.zScale = pet.scale();
            chest.getChild("left_body").z += pet.spread();
            chest.getChild("left_lid").z += pet.spread();
            chest.getChild("right_body").z -= pet.spread();
            chest.getChild("right_lid").z -= pet.spread();
            float placedZ = chest.getChild("left_body").z;
            List<float[]> model = posedBoxes(root, "");

            // The wolf's bags hang on the side of its torso, so without the clearance the lid is flush with it.
            float before = closestSidePlane(posedBoxes(chest, "_lid"), model);
            if (pet.flushWithTorso()) {
                assertEquals(0.0F, before, 1.0E-4F, pet.name() + ": the open lid lies on the side of the torso");
            }

            ChestLayer.keepLidsOffTheTorso(left, chest, PetTorso.find(body));
            float after = closestSidePlane(posedBoxes(chest, "_lid"), model);
            assertTrue(after >= 0.05F, pet.name() + ": the open lid is " + after + " from a side plane of the model");
            float moved = (chest.getChild("left_body").z - placedZ) * pet.scale();
            assertTrue(moved <= 0.2F, pet.name() + ": the bags stay attached, they moved " + moved);
            if (!pet.flushWithTorso()) {
                assertEquals(0.0F, moved, pet.name() + ": the cat layer's own placement is left as it is");
            }
        }
    }
}
