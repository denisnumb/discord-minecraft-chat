package com.denisnumb.discord_chat_mod.chat_images.model;

public abstract class AbstractImage {
    public final String url;
    public final ImageSize imageSize;
    public final ImageSize originalSize;

    protected AbstractImage(String url, ImageSize imageSize, ImageSize originalSize) {
        this.url = url;
        this.imageSize = imageSize;
        this.originalSize = originalSize;
    }
}
