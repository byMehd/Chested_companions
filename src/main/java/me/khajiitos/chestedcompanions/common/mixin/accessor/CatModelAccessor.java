package me.khajiitos.chestedcompanions.common.mixin.accessor;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the body of the shared base class of the cat and ocelot models, which Minecraft 26.1 renamed (see stonecutter.gradle.kts). */
@Mixin(net.minecraft.client.model.animal.feline.FelineModel.class)
public interface CatModelAccessor {
    @Accessor
    ModelPart getBody();
}
