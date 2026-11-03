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
        for (minecraft in listOf("1.21.11", "1.21.10", "1.21.9")) {
            for (loader in listOf("fabric", "neoforge", "forge")) {
                version("$minecraft-$loader", minecraft).buildscript("build.$loader.gradle")
            }
        }
        vcsVersion = "1.21.11-fabric"
    }
}
