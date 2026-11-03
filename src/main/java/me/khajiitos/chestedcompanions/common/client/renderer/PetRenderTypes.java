package me.khajiitos.chestedcompanions.common.client.renderer;

//? if >=1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?} else {
/*import net.minecraft.client.renderer.RenderType;
*///?}
import net.minecraft.resources.Identifier;

/** The render types of the pet chests and their icon; 1.21.11 moved the vanilla ones out of {@code RenderType}. */
public final class PetRenderTypes {
    private PetRenderTypes() {
    }

    public static RenderType entityCutout(Identifier texture) {
        //? if >=1.21.11 {
        return RenderTypes.entityCutout(texture);
        //?} else {
        /*return RenderType.entityCutout(texture);
        *///?}
    }

    public static RenderType entityCutoutNoCull(Identifier texture) {
        //? if >=1.21.11 {
        return RenderTypes.entityCutoutNoCull(texture);
        //?} else {
        /*return RenderType.entityCutoutNoCull(texture);
        *///?}
    }
}
