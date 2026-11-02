package me.khajiitos.chestedcompanions.common.client.config.cloth;

public class ClothConfigCheck {
    public static boolean isInstalled() {
        try {
            Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder", false, ClothConfigCheck.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}