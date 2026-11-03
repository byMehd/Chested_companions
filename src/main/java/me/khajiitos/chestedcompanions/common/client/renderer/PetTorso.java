package me.khajiitos.chestedcompanions.common.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/**
 * Where the torso of a pet really is on the posed model.
 *
 * <p>Resource packs built on Entity Model Features (Fresh Animations, Fat Cat, Better Cats...) leave the vanilla
 * {@code body} part empty and rebuild the whole animal below it, so the pose of that part says nothing about where the
 * torso ended up. The torso is the biggest cube below the part, the head left out: its centre and size are measured on
 * the model as it is about to be drawn, whatever the pack did to it.
 *
 * <p>Those packs hang the head below the body part too, and make it bigger on a baby, so a kitten's head can outweigh
 * its torso: a Fat Cat kitten has a head of 9x8x7 at 0.75 and a body of 10x9x14 at 0.5. The head is known by its name,
 * which the packs take from the vanilla part ({@code head}, {@code head2}, {@code head_cat}...). Where no part is named
 * like that, or nothing else holds a cube, the biggest cube is the torso, as it was before the head was told apart.
 *
 * @param x         centre of the torso, in model units (sixteenths of a block)
 * @param y         see {@code x}
 * @param z         see {@code x}
 * @param length    longest side of the torso, which is 16 on the vanilla cat
 * @param halfWidth half of the extent across the pet, which is 2 on the vanilla cat
 */
public record PetTorso(float x, float y, float z, float length, float halfWidth) {
    private static final String HEAD = "head";

    /** @return the torso found below {@code root}, or null when it holds no cube at all */
    public static @Nullable PetTorso find(ModelPart root) {
        Finder finder = new Finder();
        root.visit(new PoseStack(), finder);
        return finder.outsideHead != null ? finder.outsideHead : finder.anywhere;
    }

    /** @param path the names of the parts between the model and a cube, as the visitor gets them */
    private static boolean isInHead(String path) {
        for (int i = path.length() - HEAD.length(); i >= 0; i--) {
            if (!path.regionMatches(true, i, HEAD, 0, HEAD.length())) continue;
            if (i > 0 && Character.isLetterOrDigit(path.charAt(i - 1))) continue;
            int end = i + HEAD.length();
            while (end < path.length() && Character.isDigit(path.charAt(end))) end++;
            if (end == path.length() || !Character.isLetterOrDigit(path.charAt(end))) return true;
        }
        return false;
    }


    private static float length(float x, float y, float z) {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    private static final class Finder implements ModelPart.Visitor {
        private final Vector3f centre = new Vector3f();
        private float biggest;
        private float biggestOutsideHead;
        private @Nullable PetTorso anywhere;
        private @Nullable PetTorso outsideHead;

        @Override
        public void visit(PoseStack.Pose pose, String path, int index, ModelPart.Cube cube) {
            Matrix4fc m = pose.pose();
            float dx = cube.maxX - cube.minX;
            float dy = cube.maxY - cube.minY;
            float dz = cube.maxZ - cube.minZ;
            // Size of each side once posed and scaled by the parts above the cube.
            float sx = dx * length(m.m00(), m.m01(), m.m02());
            float sy = dy * length(m.m10(), m.m11(), m.m12());
            float sz = dz * length(m.m20(), m.m21(), m.m22());
            float volume = sx * sy * sz;
            boolean biggestOfAll = volume > this.biggest;
            boolean biggestOfThoseOutsideHead = volume > this.biggestOutsideHead && !isInHead(path);
            if (!biggestOfAll && !biggestOfThoseOutsideHead) return;

            m.transformPosition((cube.minX + cube.maxX) / 32.0F, (cube.minY + cube.maxY) / 32.0F, (cube.minZ + cube.maxZ) / 32.0F, this.centre);
            float halfWidth = (dx * Math.abs(m.m00()) + dy * Math.abs(m.m10()) + dz * Math.abs(m.m20())) / 2.0F;
            PetTorso torso = new PetTorso(this.centre.x * 16.0F, this.centre.y * 16.0F, this.centre.z * 16.0F,
                    Math.max(sx, Math.max(sy, sz)), halfWidth);
            if (biggestOfAll) {
                this.biggest = volume;
                this.anywhere = torso;
            }
            if (biggestOfThoseOutsideHead) {
                this.biggestOutsideHead = volume;
                this.outsideHead = torso;
            }
        }
    }
}
