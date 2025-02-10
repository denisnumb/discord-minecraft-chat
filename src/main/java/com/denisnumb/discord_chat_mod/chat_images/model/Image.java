package com.denisnumb.discord_chat_mod.chat_images.model;

import net.minecraft.resources.ResourceLocation;

public class Image extends AbstractImage {
    public final ResourceLocation resourceLocation;

    public Image(
            String url,
            ImageSize imageSize,
            ImageSize originalSize,
            ResourceLocation resourceLocation) {
        super(url, imageSize, originalSize);
        this.resourceLocation = resourceLocation;
    }
}
