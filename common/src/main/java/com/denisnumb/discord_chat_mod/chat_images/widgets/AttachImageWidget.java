package com.denisnumb.discord_chat_mod.chat_images.widgets;

import com.denisnumb.discord_chat_mod.locale.MinecraftLocaleProvider;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class AttachImageWidget extends AbstractWidget {
    private final Consumer<AttachImageWidget> onPress;

    public AttachImageWidget(int x, int y, int width, int height, Consumer<AttachImageWidget> onPress) {
        super(x, y, width, height, Component.literal("📎"));
        this.onPress = onPress;
        this.setFocused(false);
        this.setTooltip(Tooltip.create(MinecraftLocaleProvider.SendImage.attachImage()));
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.isHovered()) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }

        graphics.drawCenteredString(
                Minecraft.getInstance().font,
                getMessage(),
                getX() + width / 2,
                getY() + (height - 8) / 2,
                0xFFFFFFFF
        );
    }

    @Override
    public void onClick(@NotNull MouseButtonEvent mouseButtonEvent, boolean bl) {
        onPress.accept(this);
    }

    @Override
    public ComponentPath nextFocusPath(@NotNull FocusNavigationEvent event) {
        return null;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}