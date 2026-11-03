package me.khajiitos.chestedcompanions.smoke;

import me.khajiitos.chestedcompanions.common.client.config.cloth.ClothConfigCheck;
import me.khajiitos.chestedcompanions.common.client.config.cloth.ClothConfigScreenMaker;
import me.khajiitos.chestedcompanions.common.util.IChestEntityRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
//? if >=26.3 {
/*import org.lwjgl.sdl.SDLVideo;
*///?} else {
import org.lwjgl.glfw.GLFW;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//? if >=26.2 && <26.3 {
/*import org.spongepowered.asm.mixin.injection.Redirect;
*///?}
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ClientSmokeMixin {
    @Unique private int revived$ticks;
    @Unique private boolean revived$finished;

    //? if >=26.2 && <26.3 {
    /*// 26.2 explicitly shows its initially hidden GLFW window after the renderer is ready.
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwShowWindow(J)V"), remap = false)
    private void revived$keepWindowHidden(long handle) {
    }
    *///?}

    @Inject(method = "tick", at = @At("TAIL"))
    private void checkClient(CallbackInfo ci) {
        Minecraft client = (Minecraft)(Object)this;
        if (revived$finished || !Boolean.getBoolean("revived.smoke") || client.getOverlay() != null || client.screen == null) return;
        if (++revived$ticks < 40) return;
        //? if >=26.3 {
        /*if ((SDLVideo.SDL_GetWindowFlags(client.getWindow().handle()) & SDLVideo.SDL_WINDOW_HIDDEN) == 0)
        *///?} else {
        if (GLFW.glfwGetWindowAttrib(client.getWindow().handle(), GLFW.GLFW_VISIBLE) != GLFW.GLFW_FALSE)
        //?}
            throw new AssertionError("Smoke-test client must remain hidden");
        if (!((Object)new CatRenderState() instanceof IChestEntityRenderState)
                || !((Object)new WolfRenderState() instanceof IChestEntityRenderState))
            throw new AssertionError("Pet render-state mixins were not applied");
        var baseTexture = net.minecraft.resources.Identifier.fromNamespaceAndPath("chestedcompanions", "textures/chest/chest.png");
        if (client.getResourceManager().getResource(baseTexture).isEmpty())
            throw new AssertionError("The mod's assets were not loaded; client startup alone is insufficient");
        for (String texture : java.util.List.of("ender", "copper", "copper_exposed", "copper_weathered", "copper_oxidized")) {
            var sackTexture = net.minecraft.resources.Identifier.fromNamespaceAndPath("chestedcompanions", "textures/chest/special/" + texture + ".png");
            if (client.getResourceManager().getResource(sackTexture).isEmpty())
                throw new AssertionError("Missing special saddlebag texture " + sackTexture);
        }
        var config = me.khajiitos.chestedcompanions.common.client.config.ConfigScreens.create(client.screen);
        if (config == null) throw new AssertionError("Configuration screen unavailable");
        //? if >=1.21.11 {
        config.init(client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
        //?} else {
        /*config.init(client, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
        *///?}
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
