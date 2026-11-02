package me.khajiitos.chestedcompanions.common.client.gui;

import me.khajiitos.chestedcompanions.common.util.PetChestMenu;
import me.khajiitos.chestedcompanions.common.util.PetMenuProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/** The button, in the top right corner of a pet's chest screen, that takes the chest off the pet. */
public final class PetRemoveChestButton {
    private static final Identifier SPRITE = Identifier.fromNamespaceAndPath("chestedcompanions", "remove_chest");
    private static final Identifier SPRITE_FOCUSED = Identifier.fromNamespaceAndPath("chestedcompanions", "remove_chest_highlighted");
    private static final Component LABEL = Component.translatable("gui.chestedcompanions.remove_chest");
    private static final int SIZE = 12;

    private PetRemoveChestButton() {
    }

    /** @return the button for this screen, or null when it is not the chest screen of a pet */
    public static @Nullable ImageButton create(Screen screen, int leftPos, int topPos, int imageWidth) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)
                || !(screen.getTitle().getContents() instanceof TranslatableContents title)
                || !PetMenuProvider.TITLE_KEY.equals(title.getKey())) {
            return null;
        }
        ImageButton button = new ImageButton(leftPos + imageWidth - SIZE - 7, topPos + 3, SIZE, SIZE,
                new WidgetSprites(SPRITE, SPRITE_FOCUSED), pressed -> removeChest(containerScreen), LABEL);
        button.setTooltip(Tooltip.create(LABEL));
        return button;
    }

    private static void removeChest(AbstractContainerScreen<?> screen) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(screen.getMenu().containerId, PetChestMenu.REMOVE_CHEST_BUTTON);
        }
    }
}
