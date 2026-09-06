package com.denisnumb.discord_chat_mod.chat_images.model;

import net.minecraft.resources.Identifier;

public abstract class AbstractImage {
    public final String url;
    public final ImageSize imageSize;
    public final ImageSize originalSize;
    public final Identifier spoilerIdentifier;
    private final boolean isSpoiler;
    private boolean isSpoilerOpened = false;

    protected AbstractImage(
            String url,
            ImageSize imageSize,
            ImageSize originalSize,
            boolean isSpoiler,
            Identifier spoilerIdentifier
    ) {
        this.url = url;
        this.imageSize = imageSize;
        this.originalSize = originalSize;
        this.isSpoiler = isSpoiler;
        this.spoilerIdentifier = spoilerIdentifier;
    }

    public abstract Identifier getRenderFrame();

    public boolean isSpoilerAndNotOpened(){
        return isSpoiler && !isSpoilerOpened;
    }

    public void openSpoiler(){
        isSpoilerOpened = true;
    }
}