package com.denisnumb.discord_chat_mod.chat_images.model;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class AnimatedImage extends AbstractImage {
    public final List<ResourceLocation> frames;
    public final int frameDuration;

    public AnimatedImage(
            String url,
            List<ResourceLocation> frames,
            ImageSize imageSize,
            ImageSize originalSize,
            int frameDuration
    ) {
        super(url, imageSize, originalSize);
        this.frames = frames;
        this.frameDuration = frameDuration;
    }
}