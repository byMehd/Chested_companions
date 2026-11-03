package me.khajiitos.chestedcompanions.smoke;

import com.mojang.blaze3d.platform.Window;
//? if >=26.3 {
/*import com.mojang.renderpearl.api.device.GpuBackend;
import org.lwjgl.sdl.SDLVideo;
*///?} else {
import org.lwjgl.glfw.GLFW;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Test-only: hide the native window at creation, on both GLFW and SDL. */
@Mixin(Window.class)
public class HiddenWindowMixin {
    //? if >=26.3 {
    /*@Redirect(method = "createWindow", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/device/GpuBackend;createWindow(Ljava/lang/String;IIJ)J"), remap = false)
    private long hiddenWindow(GpuBackend backend, String title, int width, int height, long flags) {
        return backend.createWindow(title, 640, 480, flags | SDLVideo.SDL_WINDOW_HIDDEN);
    }
    *///?} else {
    //? if >=26.1 {
    /*// Minecraft 26.1 creates the GLFW window in a static method of its own instead of in the constructor.
    @Redirect(method = "createGlfwWindow", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"), remap = false)
    private static long hiddenWindow(int width, int height, CharSequence title, long monitor, long share) {
    *///?} else {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"), remap = false)
    private long hiddenWindow(int width, int height, CharSequence title, long monitor, long share) {
    //?}
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
        return GLFW.glfwCreateWindow(640, 480, title, 0, share);
    }
    //?}
}
