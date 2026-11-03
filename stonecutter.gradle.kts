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
        replace("net.minecraft.client.model.FelineModel", "net.minecraft.client.model.animal.feline.FelineModel")
        replace("net.minecraft.client.model.WolfModel", "net.minecraft.client.model.animal.wolf.WolfModel")
    }
    // Minecraft 26.1 renamed the 2D drawing class, which now "extracts" what a frame shows instead of drawing it, moved the
    // camera state and split the cat model in two, so the shared base class of the feline models changed its name.
    // The renames of screen and widget methods are written with the receiver or the signature they appear in, because
    // a replacement is a plain text swap over the whole file and its reverse runs on every older version.
    replacements.string(current.parsed >= "26.1") {
        replace("public void render(GuiGraphics graphics", "public void extractRenderState(GuiGraphicsExtractor graphics")
        replace("super.render(graphics", "super.extractRenderState(graphics")
        replace("graphics.drawCenteredString(", "graphics.centeredText(")
        replace("graphics.renderItem(", "graphics.item(")
        replace("world.getClientWorld()", "world.getClientLevel()") // Fabric's client game test API, used by the smoke scenarios
        replace("renderContents", "extractContents")
        replace("renderDefaultSprite", "extractDefaultSprite")
        replace("GuiGraphics", "GuiGraphicsExtractor")
        replace("net.minecraft.client.renderer.state.CameraRenderState", "net.minecraft.client.renderer.state.level.CameraRenderState")
        replace("net.minecraft.client.model.animal.feline.FelineModel", "net.minecraft.client.model.animal.feline.AbstractFelineModel")
    }
}
