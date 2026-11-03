package me.khajiitos.chestedcompanions.common.client.renderer;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One saddlebag: a body plus a lid hinged on the edge facing the pet.
 *
 * <p>Closed, the two cubes fill exactly the single 8x8x3 box (drawn one unit larger on every side) that older versions
 * rendered, so existing resource packs and textures keep working: the lid is the top {@value #LID_ROWS} texture rows of
 * every face and the body the remaining {@value #BODY_ROWS}. The texture offset {@code (u, v)} is the one of the
 * original box; the body samples the same layout {@value #LID_ROWS} rows further down.
 */
public final class ChestBag {
    public static final int LID_ROWS = 3;
    public static final int BODY_ROWS = 5;
    /** Each bag is drawn this many units larger than its texture on every side. */
    public static final float GROW = 1.0F;
    /** The least gap kept between the plane an open lid lies in and the side of the torso, in bag units. */
    public static final float LID_CLEARANCE = 0.3F;
    private static final int WIDTH = 8;
    private static final int DEPTH = 3;
    private static final float TEXEL = (WIDTH + 2 * GROW) / WIDTH;

    private final ModelPart.Cube lid;
    private final ModelPart.Cube body;
    private final float hingeY;
    private final float hingeZ;
    private final float openDirection;

    /**
     * @param origin where the original box starts; its sign on z tells which flank of the pet the bag is on
     * @param u      texture offset of the original box (the body samples {@value #LID_ROWS} rows further down)
     * @param v      see {@code u}
     */
    public ChestBag(Vec3i origin, boolean mirror, int u, int v, int textureWidth, int textureHeight) {
        float lidHeight = LID_ROWS * TEXEL;
        float bodyHeight = BODY_ROWS * TEXEL;
        float lidGrow = (lidHeight - LID_ROWS) / 2.0F;
        float bodyGrow = (bodyHeight - BODY_ROWS) / 2.0F;
        float seam = origin.getY() - GROW + lidHeight;
        boolean outerIsPositive = origin.getZ() > 0;
        this.hingeY = seam;
        this.hingeZ = outerIsPositive ? origin.getZ() - GROW : origin.getZ() + DEPTH + GROW;
        this.openDirection = outerIsPositive ? 1.0F : -1.0F;
        Set<Direction> faces = Set.of(Direction.values());
        this.lid = new ModelPart.Cube(u, v, origin.getX(), -lidHeight + lidGrow, origin.getZ() - this.hingeZ,
                WIDTH, LID_ROWS, DEPTH, GROW, lidGrow, GROW, mirror, textureWidth, textureHeight, faces);
        this.body = new ModelPart.Cube(u, v + LID_ROWS, origin.getX(), seam + bodyGrow, origin.getZ(),
                WIDTH, BODY_ROWS, DEPTH, GROW, bodyGrow, GROW, mirror, textureWidth, textureHeight, faces);
    }

    public ModelPart.Cube lidCube() {
        return this.lid;
    }

    public ModelPart.Cube bodyCube() {
        return this.body;
    }

    /**
     * How far the hinge is from the middle of the pet, in bag units. An open lid stands in the plane of its hinge, on
     * the side of the pet.
     */
    public float hingeDistance() {
        return Math.abs(this.hingeZ);
    }

    /**
     * An open lid is a slab standing in the plane of the hinge. The side of the torso is flat too, and where the two
     * planes coincide the game draws both in the same place, so they flicker. This is how far the bags have to move
     * apart, in bag units, so that the plane keeps {@value #LID_CLEARANCE} bag units from the side of the torso: it ends
     * up outside the torso, where the open lid shows. A plane already that far from the torso, on either side, stays.
     *
     * @param plane     distance from the middle of the pet to the hinge planes, in model units
     * @param halfWidth distance from the middle of the pet to the sides of its torso, in model units
     * @param scale     scale of the bags, which turns bag units into model units
     */
    public static float spreadToClear(float plane, float halfWidth, float scale) {
        float gap = LID_CLEARANCE * scale;
        float clearance = plane - halfWidth;
        return Math.abs(clearance) < gap ? (gap - clearance) / scale : 0.0F;
    }

    public ModelPart createBody() {
        return new ModelPart(List.of(this.body), Map.of());
    }

    /** @param angle how far the lid is raised, in radians (zero is closed) */
    public ModelPart createLid(float angle) {
        ModelPart part = new ModelPart(List.of(this.lid), Map.of());
        part.setPos(0.0F, this.hingeY, this.hingeZ);
        part.xRot = this.openDirection * angle;
        return part;
    }

    /** Vanilla chest easing: the lid rises fast and settles gently, up to a quarter turn. */
    public static float lidAngle(float openness) {
        float eased = 1.0F - openness;
        eased = 1.0F - eased * eased * eased;
        return eased * ((float) Math.PI / 2.0F);
    }
}
