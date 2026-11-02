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
        for (loader in listOf("fabric", "neoforge", "forge")) {
            version("1.21.11-$loader", "1.21.11").buildscript("build.$loader.gradle")
        }
        vcsVersion = "1.21.11-fabric"
    }
}
