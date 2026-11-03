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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * The button, beside a pet's chest screen, that takes the chest off the pet: an ordinary vanilla button showing a
 * crossed-out chest: the vanilla chest item under a thin red cross in the colours of the anvil's. Everything else is
 * vanilla too (the button sprites and highlight), so resource packs restyle the button and the chest.
 */
public final class PetRemoveChestButton extends AbstractButton {
    /** The two reds of the cross the anvil draws over a recipe it cannot make. */
    private static final int CROSS_LIGHT = 0xFFBA370F;
    private static final int CROSS_DARK = 0xFF962300;
    private static final Component LABEL = Component.translatable("gui.chestedcompanions.remove_chest");
    private static final int SIZE = 20;
    private static final int GAP = 4;
    private static final int CROSS_SIZE = 12;

    private final Runnable action;
    private final ItemStack icon = new ItemStack(Items.CHEST);

    private PetRemoveChestButton(int x, int y, Runnable action) {
        // Without a message the vanilla button draws no text; the label is only narrated and shown as the tooltip.
        super(x, y, SIZE, SIZE, Component.empty());
        this.action = action;
        this.setTooltip(Tooltip.create(LABEL));
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.action.run();
    }

    @Override
    public MutableComponent createNarrationMessage() {
        return wrapDefaultNarrationMessage(LABEL);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    //? if >=1.21.11 {
    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderDefaultSprite(graphics);
        this.renderIcon(graphics, this.getX(), this.getY());
    }
    //?} else {
    /*@Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        this.renderIcon(graphics, this.getX(), this.getY());
    }
    *///?}

    /** The chest with a thin red cross over it, both inside the 16x16 an item takes. */
    private void renderIcon(GuiGraphics graphics, int left, int top) {
        int x = left + (SIZE - 16) / 2;
        int y = top + (SIZE - 16) / 2;
        graphics.renderItem(this.icon, x, y);
        for (int i = 0; i < CROSS_SIZE; i++) {
            int row = y + 2 + i;
            int down = x + 2 + i;
            int up = x + 2 + CROSS_SIZE - 1 - i;
            graphics.fill(down, row, down + 1, row + 1, CROSS_LIGHT);
            graphics.fill(down + 1, row, down + 2, row + 1, CROSS_DARK);
            graphics.fill(up, row, up + 1, row + 1, CROSS_LIGHT);
            graphics.fill(up + 1, row, up + 2, row + 1, CROSS_DARK);
        }
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
