package me.khajiitos.chestedcompanions.common.client.config;

import me.khajiitos.chestedcompanions.common.config.CCConfig;
import me.khajiitos.chestedcompanions.common.config.CCConfigValues;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Small-screen-friendly settings fallback, with no optional GUI library dependency. */
public final class NativeConfigScreen extends Screen {
    private final Screen parent;
    private final List<Setting> settings = new ArrayList<>();
    private int page;

    private static final class Setting {
        final String key;
        final CCConfigValues.Value<?> target;
        Object selected;
        Setting(String key, CCConfigValues.Value<?> target) {
            this.key = key; this.target = target; this.selected = target.get();
        }
        Component label() {
            Component value = selected instanceof Boolean enabled
                    ? Component.translatable(enabled ? "options.on" : "options.off")
                    : Component.translatable("chestedcompanions.config." + key + ".value." + ((Enum<?>)selected).name().toLowerCase(Locale.ROOT));
            return Component.translatable("chestedcompanions.config." + key + ".name").append(": ").append(value);
        }
        void cycle() {
            if (selected instanceof Boolean enabled) selected = !enabled;
            else if (selected instanceof Enum<?> choice) {
                Object[] choices = choice.getDeclaringClass().getEnumConstants();
                selected = choices[(choice.ordinal() + 1) % choices.length];
            }
        }
    }

    public NativeConfigScreen(Screen parent) {
        super(Component.translatable("chestedcompanions.config.header"));
        this.parent = parent;
        for (Field field : CCConfig.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(CCConfig.Entry.class)) {
                try { settings.add(new Setting(field.getName(), (CCConfigValues.Value<?>)field.get(null))); }
                catch (IllegalAccessException e) { throw new IllegalStateException(e); }
            }
        }
    }

    @Override protected void init() {
        int perPage = Math.max(1, Math.min(6, (height - 90) / 24));
        int pages = Math.max(1, (settings.size() + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);
        int buttonWidth = Math.min(340, width - 24);
        for (int row = 0; row < perPage && page * perPage + row < settings.size(); row++) {
            Setting setting = settings.get(page * perPage + row);
            Button button = Button.builder(setting.label(), pressed -> {
                setting.cycle(); pressed.setMessage(setting.label());
            }).bounds((width - buttonWidth) / 2, 40 + row * 24, buttonWidth, 20).build();
            button.setTooltip(Tooltip.create(Component.translatable("chestedcompanions.config." + setting.key + ".description")));
            addRenderableWidget(button);
        }
        Button previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> { page--; rebuildWidgets(); })
                .bounds(width / 2 - 110, height - 28, 40, 20).build());
        previous.active = page > 0;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> {
            settings.forEach(setting -> setting.target.setUnchecked(setting.selected));
            CCConfig.save(); onClose();
        }).bounds(width / 2 - 60, height - 28, 120, 20).build());
        Button next = addRenderableWidget(Button.builder(Component.literal(">"), b -> { page++; rebuildWidgets(); })
                .bounds(width / 2 + 70, height - 28, 40, 20).build());
        next.active = page + 1 < pages;
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFFFF);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
}
