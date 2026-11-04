package me.khajiitos.chestedcompanions.common.client.renderer;

//? if >=1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?} else {
/*import net.minecraft.client.renderer.RenderType;
*///?}
import net.minecraft.resources.Identifier;

/**
 * The render types of the pet chests and their icon; 1.21.11 moved the vanilla ones out of {@code RenderType}, and 26.1
 * renamed them: {@code entityCutout} became the one that is not culled and {@code entityCutoutCull} the culled one.
 */
public final class PetRenderTypes {
    private PetRenderTypes() {
    }

    /** The cutout type whose faces are culled when they face away. */
    public static RenderType entityCutout(Identifier texture) {
        //? if >=26.1 {
        /*return RenderTypes.entityCutoutCull(texture);
        *///?} else if >=1.21.11 {
        return RenderTypes.entityCutout(texture);
        //?} else {
        /*return RenderType.entityCutout(texture);
        *///?}
    }

    /** The cutout type that shows both sides of a face. */
    public static RenderType entityCutoutNoCull(Identifier texture) {
        //? if >=26.1 {
        /*return RenderTypes.entityCutout(texture);
        *///?} else if >=1.21.11 {
        return RenderTypes.entityCutoutNoCull(texture);
        //?} else {
        /*return RenderType.entityCutoutNoCull(texture);
        *///?}
    }
}
