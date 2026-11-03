package me.khajiitos.chestedcompanions.common;

import me.khajiitos.chestedcompanions.common.client.renderer.ChestBag;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Vec3i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The hinged bag must look exactly like the single box older versions drew whenever its lid is shut. */
class ChestBagTest {
    private static final Vec3i[] ORIGINS = {
            new Vec3i(-8, -4, 6), new Vec3i(-8, -4, -9),     // wolf, left and right
            new Vec3i(-22, 4, 4), new Vec3i(-22, 4, -7),     // cat, left and right
    };

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
}
