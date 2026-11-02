package me.khajiitos.chestedcompanions.common.client.config;

import me.khajiitos.chestedcompanions.common.client.config.cloth.ClothConfigCheck;
import me.khajiitos.chestedcompanions.common.client.config.cloth.ClothConfigScreenMaker;
import net.minecraft.client.gui.screens.Screen;

public final class ConfigScreens {
    public static Screen create(Screen parent) {
        return ClothConfigCheck.isInstalled() ? ClothConfigScreenMaker.create(parent) : new NativeConfigScreen(parent);
    }
}
