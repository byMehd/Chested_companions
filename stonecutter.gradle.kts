plugins { id("dev.kikugie.stonecutter") }
stonecutter active "1.21.11-fabric"
stonecutter parameters {
    constants {
        match(current.project.substringAfterLast('-'), "fabric", "neoforge", "forge")
    }
    // The source is written with the Minecraft 1.21.11 names. 1.21.11 renamed ResourceLocation, moved the golem, the cat,
    // their models and the game rules into feature packages, and renamed one Player method; older versions get the
    // names they know through these replacements. Behaviour that differs is written out with conditions instead.
    replacements.string(current.parsed >= "1.21.11") {
        replace("ResourceLocation", "Identifier")
        replace("canInteractWithEntity", "isWithinEntityInteractionRange")
        replace("net.minecraft.world.entity.animal.Cat", "net.minecraft.world.entity.animal.feline.Cat")
        replace("net/minecraft/world/entity/animal/Cat", "net/minecraft/world/entity/animal/feline/Cat") // in mixin method descriptors
        replace("net.minecraft.world.entity.animal.coppergolem.", "net.minecraft.world.entity.animal.golem.")
        replace("net.minecraft.world.level.GameRules", "net.minecraft.world.level.gamerules.GameRules")
        replace("net.minecraft.client.model.CatModel", "net.minecraft.client.model.animal.feline.CatModel")
        replace("net.minecraft.client.model.FelineModel", "net.minecraft.client.model.animal.feline.FelineModel")
        replace("net.minecraft.client.model.WolfModel", "net.minecraft.client.model.animal.wolf.WolfModel")
    }
}
