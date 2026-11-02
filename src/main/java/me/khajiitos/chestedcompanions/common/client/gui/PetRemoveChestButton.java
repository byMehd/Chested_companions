package me.khajiitos.chestedcompanions.common.client.gui;

import me.khajiitos.chestedcompanions.common.util.PetChestMenu;
import me.khajiitos.chestedcompanions.common.util.PetMenuProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * The button, beside a pet's chest screen, that takes the chest off the pet: an ordinary vanilla button showing the
 * shears, which are the other way to do it. Everything it draws is vanilla, so resource packs restyle it too.
 */
public final class PetRemoveChestButton extends AbstractButton {
    private static final Component LABEL = Component.translatable("gui.chestedcompanions.remove_chest");
    private static final int SIZE = 20;
    private static final int GAP = 4;

    private final Runnable action;
    private final ItemStack icon = new ItemStack(Items.SHEARS);

    private PetRemoveChestButton(int x, int y, Runnable action) {
        super(x, y, SIZE, SIZE, LABEL);
        this.action = action;
        this.setTooltip(Tooltip.create(LABEL));
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.action.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderDefaultSprite(graphics);
        graphics.renderItem(this.icon, this.getX() + (this.getWidth() - 16) / 2, this.getY() + (this.getHeight() - 16) / 2);
    }

    /** @return the button for this screen, or null when it is not the chest screen of a pet */
    public static @Nullable PetRemoveChestButton create(Screen screen, int leftPos, int topPos, int imageWidth) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)
                || !(screen.getTitle().getContents() instanceof TranslatableContents title)
                || !PetMenuProvider.TITLE_KEY.equals(title.getKey())) {
            return null;
        }
        // Beside the frame, level with its top: on the right if there is room, else on the left, else above its corner.
        int x = leftPos + imageWidth + GAP;
        int y = topPos;
        if (x + SIZE > screen.width) {
            x = leftPos - GAP - SIZE;
        }
        if (x < 0) {
            x = leftPos + imageWidth - SIZE;
            y = Math.max(0, topPos - SIZE - GAP);
        }
        return new PetRemoveChestButton(x, y, () -> removeChest(containerScreen));
    }

    private static void removeChest(AbstractContainerScreen<?> screen) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(screen.getMenu().containerId, PetChestMenu.REMOVE_CHEST_BUTTON);
        }
    }
}
