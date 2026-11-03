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
