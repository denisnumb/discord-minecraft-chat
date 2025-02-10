package com.denisnumb.discord_chat_mod.chat_images;

import com.denisnumb.discord_chat_mod.chat_images.model.AbstractImage;
import com.denisnumb.discord_chat_mod.chat_images.model.AnimatedImage;
import com.denisnumb.discord_chat_mod.chat_images.model.Image;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static com.denisnumb.discord_chat_mod.chat_images.ImageStorage.IMAGE_CACHE;
import static com.denisnumb.discord_chat_mod.chat_images.ImageUtils.getCurrentFrameIndex;

public class ImageScreen extends Screen {
    private final AbstractImage image;
    private final int imageWidth;
    private final int imageHeight;
    private int centerX = 0;
    private int centerY = 0;
    private int renderWidth = 0;
    private int renderHeight = 0;

    public ImageScreen(String imageUrl) {
        super(Component.literal("Image Viewer"));
        this.image = IMAGE_CACHE.getOrDefault(imageUrl, null);
        imageWidth = image.originalSize.width();
        imageHeight = image.originalSize.height();
    }


    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        RenderSystem.disableDepthTest();
        graphics.fill(0, 0, this.width, this.height, 0x88000000);
        RenderSystem.enableDepthTest();

        int maxWidth = (int) (this.width * 0.7);
        int maxHeight = (int) (this.height * 0.7);
        double scale = Math.min(1.0, Math.min((double) maxWidth / imageWidth, (double) maxHeight / imageHeight));

        renderWidth = (int) (imageWidth * scale);
        renderHeight = (int) (imageHeight * scale);
        centerX = (this.width - renderWidth) / 2;
        centerY = (this.height - renderHeight) / 2;

        ResourceLocation resourceLocation = image instanceof AnimatedImage gif
                ? gif.frames.get(getCurrentFrameIndex(gif))
                : ((Image) image).resourceLocation;

        graphics.blit(resourceLocation,
                centerX, centerY,
                0, 0,
                renderWidth, renderHeight,
                renderWidth, renderHeight
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            boolean outsideImage = mouseX < centerX
                    || mouseX > centerX + renderWidth
                    || mouseY < centerY
                    || mouseY > centerY + renderHeight;
            if (outsideImage) {
                Minecraft.getInstance().setScreen(null);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
