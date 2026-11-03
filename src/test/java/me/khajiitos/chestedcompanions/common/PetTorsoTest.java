package me.khajiitos.chestedcompanions.common;

import me.khajiitos.chestedcompanions.common.client.renderer.ChestBag;
import me.khajiitos.chestedcompanions.common.client.renderer.PetTorso;
import me.khajiitos.chestedcompanions.common.client.renderer.layer.ChestLayer;
import net.minecraft.SharedConstants;
//? if >=26.1 {
/*import net.minecraft.client.model.animal.feline.AdultFelineModel;
import net.minecraft.client.model.animal.feline.BabyFelineModel;
*///?} else {
import net.minecraft.client.model.animal.feline.FelineModel;
//?}
import net.minecraft.client.model.geom.LayerDefinitions;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.Bootstrap;
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
        //? if >=26.1 {
        /*// The baby cat has a model of its own since 26.1.
        LayerDefinition layer = baby ? BabyFelineModel.createBabyLayer()
                : LayerDefinition.create(AdultFelineModel.createBodyMesh(CubeDeformation.NONE), 64, 32);
        return layer.bakeRoot().getChild("body");
        *///?} else {
        LayerDefinition layer = LayerDefinition.create(FelineModel.createBodyMesh(CubeDeformation.NONE), 64, 32);
        return (baby ? layer.apply(FelineModel.BABY_TRANSFORMER) : layer).bakeRoot().getChild("body");
        //?}
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

    //? if >=26.1 {
    /*// Since 26.1 the baby is a box of its own (4 wide, 3 high, 7 long) instead of the adult one scaled down by half.
    @Test void aBabyCatTorsoIsItsOwnShorterBox() {
        PetTorso torso = PetTorso.find(vanillaBody(true));
        assertNotNull(torso);
        assertEquals(7.0F, torso.length(), 1.0E-3F);
        assertEquals(2.0F, torso.halfWidth(), 1.0E-3F);
    }
    *///?} else {
    @Test void aBabyCatTorsoIsHalfAsLongAndTheSameBagsScaleDownWithIt() {
        PetTorso torso = PetTorso.find(vanillaBody(true));
        assertNotNull(torso);
        assertEquals(8.0F, torso.length(), 1.0E-3F);
        assertEquals(1.0F, torso.halfWidth(), 1.0E-3F);
    }
    //?}

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

    @Test void theVanillaWolfTorsoIsItsBodyPartAndNotItsMane() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var layers = LayerDefinitions.createRoots();
        ModelPart wolf = layers.get(ModelLayers.WOLF).bakeRoot();
        PetTorso torso = PetTorso.find(wolf.getChild("body"));
        assertNotNull(torso);
        assertEquals(3.0F, torso.halfWidth(), 1.0E-4F);
        assertEquals(9.0F, torso.length(), 1.0E-4F);
        // The mane is a part of its own, wider (4 from the middle) and bigger than the body: measuring the whole model
        // would take it for the torso.
        assertEquals(4.0F, PetTorso.find(wolf).halfWidth(), 1.0E-4F);

        PetTorso pup = PetTorso.find(layers.get(ModelLayers.WOLF_BABY).bakeRoot().getChild("body"));
        assertNotNull(pup);
        //? if >=26.1 {
        /*assertEquals(3.0F, pup.halfWidth(), 1.0E-4F);
        assertEquals(8.0F, pup.length(), 1.0E-4F);
        *///?} else {
        assertEquals(1.5F, pup.halfWidth(), 1.0E-4F);
        assertEquals(4.5F, pup.length(), 1.0E-4F);
        //?}
    }

    /** The open lids of a wolf whose body part is empty, as Entity Model Features leaves it, with this torso below it. */
    private static float spreadOfARebuiltWolf(float torsoWidth) {
        ModelPart torso = part(0.0F, 0.0F, 0.0F, 0.0F, List.of(cube(-torsoWidth / 2.0F, -2.0F, -3.0F, torsoWidth, 9.0F, 6.0F)), Map.of());
        ModelPart body = part(0.0F, 14.0F, 2.0F, PI / 2, List.of(), Map.of("EMF_body", torso));

        ChestBag left = new ChestBag(new Vec3i(-8, -4, 6), false, 0, 0, 22, 11);
        ChestBag right = new ChestBag(new Vec3i(-8, -4, -9), true, 0, 0, 22, 11);
        ModelPart chest = new ModelPart(List.of(), Map.of(
                "left_body", left.createBody(), "left_lid", left.createLid(0.0F),
                "right_body", right.createBody(), "right_lid", right.createLid(0.0F)));
        chest.setPos(body.x, body.y, body.z);
        chest.xScale = chest.yScale = chest.zScale = 0.6F;
        PetTorso found = PetTorso.find(body);
        assertNotNull(found);
        ChestLayer.keepLidsOffTheTorso(left, chest, found);
        assertEquals(-chest.getChild("right_body").z, chest.getChild("left_body").z, 1.0E-6F, "both bags move apart together");
        return chest.getChild("left_body").z;
    }

    @Test void aRebuiltWolfKeepsItsOpenLidsOffItsTorsoToo() {
        // As wide as the vanilla one: the hinge plane (5 * 0.6 = 3) would lie on its side, so the bags move out a little.
        assertEquals(ChestBag.LID_CLEARANCE, spreadOfARebuiltWolf(6.0F), 1.0E-4F);
        // Slightly narrower: the plane is 0.1 outside the torso, which is still too close, so the bags move the rest of the way.
        assertEquals(ChestBag.LID_CLEARANCE - (3.0F - 2.9F) / 0.6F, spreadOfARebuiltWolf(5.8F), 1.0E-4F);
        // Narrower or much wider ones leave the bags where they are, floating clear of the torso or burying their lids in it.
        assertEquals(0.0F, spreadOfARebuiltWolf(4.0F));
        assertEquals(0.0F, spreadOfARebuiltWolf(9.0F));
    }
}
