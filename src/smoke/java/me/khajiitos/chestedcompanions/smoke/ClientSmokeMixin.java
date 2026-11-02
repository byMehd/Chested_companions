package me.khajiitos.chestedcompanions.smoke;

import me.khajiitos.chestedcompanions.common.client.config.cloth.ClothConfigCheck;
import me.khajiitos.chestedcompanions.common.client.config.cloth.ClothConfigScreenMaker;
import me.khajiitos.chestedcompanions.common.util.IChestEntityRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ClientSmokeMixin {
    @Unique private int revived$ticks;
    @Unique private boolean revived$finished;

    @Inject(method = "tick", at = @At("TAIL"))
    private void checkClient(CallbackInfo ci) {
        Minecraft client = (Minecraft)(Object)this;
        if (revived$finished || client.getOverlay() != null || client.screen == null) return;
        if (++revived$ticks < 40) return;
        if (GLFW.glfwGetWindowAttrib(client.getWindow().handle(), GLFW.GLFW_VISIBLE) != GLFW.GLFW_FALSE)
            throw new AssertionError("Smoke-test client must remain hidden");
        if (!((Object)new CatRenderState() instanceof IChestEntityRenderState)
                || !((Object)new WolfRenderState() instanceof IChestEntityRenderState))
            throw new AssertionError("Pet render-state mixins were not applied");
        var baseTexture = net.minecraft.resources.Identifier.fromNamespaceAndPath("chestedcompanions", "textures/chest/chest.png");
        if (client.getResourceManager().getResource(baseTexture).isEmpty())
            throw new AssertionError("The mod's assets were not loaded; client startup alone is insufficient");
        var config = me.khajiitos.chestedcompanions.common.client.config.ConfigScreens.create(client.screen);
        if (config == null) throw new AssertionError("Configuration screen unavailable");
        config.init(client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
        var paleOak = net.minecraft.resources.Identifier.fromNamespaceAndPath("lolmcv", "pale_oak_chest");
        if (net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(paleOak)) {
            var texture = net.minecraft.resources.Identifier.fromNamespaceAndPath("lolmcv", "textures/entity/chest/pale_oak.png");
            if (client.getResourceManager().getResource(texture).isEmpty()) throw new AssertionError("Missing MCV pale oak texture");
            System.out.println("REVIVED_MCV_SMOKE_PASSED: More Chest Variants registered and pale oak texture available");
        }
        revived$finished = true;
        System.out.println("REVIVED_CLIENT_SMOKE_PASSED: hidden window, resource reload, pet renderer mixins, optional config screen");
        client.stop();
    }
}
