package com.denisnumb.discord_chat_mod.chat_images;

import com.denisnumb.discord_chat_mod.chat_images.model.AbstractImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ImageScreen extends Screen {
    private final AbstractImage image;
    private final Screen prevScreen;
    private final int imageWidth;
    private final int imageHeight;
    private int centerX = 0;
    private int centerY = 0;
    private int renderWidth = 0;
    private int renderHeight = 0;

    public ImageScreen(AbstractImage image, Screen prevScreen) {
        super(Component.literal("Image Viewer"));
        this.image = image;
        this.prevScreen = prevScreen;
        this.imageWidth = image.originalSize.width();
        this.imageHeight = image.originalSize.height();
    }


    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        graphics.fill(0, 0, this.width, this.height, 0x88000000);

        int maxWidth = (int) (this.width * 0.7);
        int maxHeight = (int) (this.height * 0.7);
        double scale = Math.min(1.0, Math.min((double) maxWidth / imageWidth, (double) maxHeight / imageHeight));

        renderWidth = (int) (imageWidth * scale);
        renderHeight = (int) (imageHeight * scale);
        centerX = (this.width - renderWidth) / 2;
        centerY = (this.height - renderHeight) / 2;

        graphics.blit(RenderPipelines.GUI_TEXTURED, image.getRenderFrame(),
                centerX, centerY,
                0, 0,
                renderWidth, renderHeight,
                renderWidth, renderHeight
        );
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean bl) {
        if (event.button() == 0) {
            boolean outsideImage = event.x() < centerX
                    || event.x() > centerX + renderWidth
                    || event.y() < centerY
                    || event.y() > centerY + renderHeight;
            if (outsideImage) {
                Minecraft.getInstance().setScreen(prevScreen);
                return true;
            }
        }
        return super.mouseClicked(event, bl);
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (event.key() == 256) {
            Minecraft.getInstance().setScreen(prevScreen);
            return true;
        }
        return super.keyPressed(event);
    }
}