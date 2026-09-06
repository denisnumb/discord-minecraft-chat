package com.denisnumb.discord_chat_mod.chat_images.model;

import net.minecraft.resources.Identifier;

public class Image extends AbstractImage {
    public final Identifier resourceLocation;

    public Image(
            String url,
            ImageSize imageSize,
            ImageSize originalSize,
            Identifier resourceLocation,
            boolean isSpoiler,
            Identifier spoilerIdentifier
    ){
        super(url, imageSize, originalSize, isSpoiler, spoilerIdentifier);
        this.resourceLocation = resourceLocation;
    }

    @Override
    public Identifier getRenderFrame() {
        return this.resourceLocation;
    }
}