package me.khajiitos.chestedcompanions.common;

import me.khajiitos.chestedcompanions.common.client.renderer.PetTorso;
import net.minecraft.client.model.animal.feline.FelineModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The saddlebags follow the torso wherever the model, or a resource pack, put it. */
class PetTorsoTest {
    private static final float PI = (float) Math.PI;

    private static ModelPart.Cube cube(float x, float y, float z, float width, float height, float depth) {
        return new ModelPart.Cube(0, 0, x, y, z, width, height, depth, 0.0F, 0.0F, 0.0F, false, 64, 32, Set.of(Direction.values()));
    }

    private static ModelPart part(float x, float y, float z, float xRot, List<ModelPart.Cube> cubes, Map<String, ModelPart> children) {
        ModelPart part = new ModelPart(cubes, children);
        part.setPos(x, y, z);
        part.setRotation(xRot, 0.0F, 0.0F);
        return part;
    }

    private static ModelPart vanillaBody(boolean baby) {
        LayerDefinition layer = LayerDefinition.create(FelineModel.createBodyMesh(CubeDeformation.NONE), 64, 32);
        return (baby ? layer.apply(FelineModel.BABY_TRANSFORMER) : layer).bakeRoot().getChild("body");
    }

    @Test void theVanillaCatTorsoIsFoundWhereTheLayerExpectsIt() {
        ModelPart body = vanillaBody(false);
        PetTorso torso = PetTorso.find(body);
        assertNotNull(torso);
        assertEquals(0.0F, torso.x(), 1.0E-4F);
        assertEquals(17.0F, torso.y(), 1.0E-4F);
        assertEquals(1.0F, torso.z(), 1.0E-4F);
        assertEquals(16.0F, torso.length(), 1.0E-4F);
        assertEquals(2.0F, torso.halfWidth(), 1.0E-4F);
        // The chest part of the vanilla layout (body + 2 down) is the same point as the one measured from the torso.
        assertEquals(body.y + 2.0F, torso.y() - 3.0F, 1.0E-4F);
        assertEquals(body.z, torso.z() - 11.0F, 1.0E-4F);
    }

    @Test void aBabyCatTorsoIsHalfAsLongAndTheSameBagsScaleDownWithIt() {
        PetTorso torso = PetTorso.find(vanillaBody(true));
        assertNotNull(torso);
        assertEquals(8.0F, torso.length(), 1.0E-3F);
        assertEquals(1.0F, torso.halfWidth(), 1.0E-3F);
    }

    @Test void aRebuiltCatIsMeasuredThroughEveryPartAboveItsTorso() {
        // The shape Entity Model Features gives Fresh Animations: an empty body part, and the animal below it.
        ModelPart torso = part(0.0F, -8.0F, 1.0F, PI / 2, List.of(cube(-2.0F, -5.0F, -3.0F, 4.0F, 16.0F, 6.0F)), Map.of());
        ModelPart head = part(0.0F, -9.0F, -8.0F, 0.0F, List.of(cube(-2.5F, -2.0F, -3.0F, 5.0F, 4.0F, 4.0F)), Map.of());
        ModelPart wrapper = part(0.0F, 6.0F, -1.0F, 0.0F, List.of(), Map.of("EMF_rotation", torso, "EMF_head2", head));
        ModelPart body = part(0.0F, 12.0F, -10.0F, PI / 2, List.of(), Map.of("EMF_body", wrapper));

        PetTorso found = PetTorso.find(body);
        assertNotNull(found);
        assertEquals(0.0F, found.x(), 1.0E-3F);
        assertEquals(9.0F, found.y(), 1.0E-3F);
        assertEquals(-12.0F, found.z(), 1.0E-3F);
        assertEquals(16.0F, found.length(), 1.0E-3F);
        assertEquals(2.0F, found.halfWidth(), 1.0E-3F);

        // Tilting the part everything hangs from (a sitting cat) moves the torso with it.
        body.setRotation(PI / 4, 0.0F, 0.0F);
        PetTorso sitting = PetTorso.find(body);
        assertNotNull(sitting);
        assertEquals(8.4645F, sitting.y(), 1.0E-3F);
        assertEquals(-9.2929F, sitting.z(), 1.0E-3F);
        assertEquals(16.0F, sitting.length(), 1.0E-3F);
    }

    @Test void aWiderTorsoReportsItsWidthWhateverWayItIsTurned() {
        ModelPart fat = part(0.0F, 12.0F, -10.0F, PI / 2, List.of(cube(-4.0F, 3.0F, -8.0F, 8.0F, 16.0F, 6.0F)), Map.of());
        PetTorso torso = PetTorso.find(fat);
        assertNotNull(torso);
        assertEquals(4.0F, torso.halfWidth(), 1.0E-4F);
        assertEquals(16.0F, torso.length(), 1.0E-4F);
    }

    @Test void aModelWithoutCubesHasNoTorso() {
        assertNull(PetTorso.find(new ModelPart(List.of(), Map.of())));
    }
}
