pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.kikugie.dev/releases/")
        gradlePluginPortal()
        mavenCentral()
    }
}
plugins { id("dev.kikugie.stonecutter") version "0.9.8" }
rootProject.name = "ChestedCompanionsRevived"
stonecutter {
    create(rootProject) {
        for (minecraft in listOf("1.21.11", "1.21.10", "1.21.9", "26.1.2", "26.2", "26.3")) {
            for (loader in listOf("fabric", "neoforge", "forge")) {
                // Minecraft 26 is no longer obfuscated: Fabric Loom builds it with another plugin and another script.
                val unobfuscated = minecraft.startsWith("26.")
                val script = if (loader == "fabric" && unobfuscated) "build.fabric-unobfuscated.gradle" else "build.$loader.gradle"
                version("$minecraft-$loader", minecraft).buildscript(script)
            }
        }
        vcsVersion = "1.21.11-fabric"
    }
}
