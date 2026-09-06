package com.denisnumb.discord_chat_mod.chat_images.model;

import net.minecraft.resources.Identifier;

import java.util.List;

public class AnimatedImage extends AbstractImage {
    public final List<Identifier> frames;
    public final int frameDuration;

    public AnimatedImage(
            String url,
            List<Identifier> frames,
            ImageSize imageSize,
            ImageSize originalSize,
            int frameDuration,
            boolean isSpoiler,
            Identifier spoilerIdentifier
    ) {
        super(url, imageSize, originalSize, isSpoiler, spoilerIdentifier);
        this.frames = frames;
        this.frameDuration = frameDuration;
    }

    @Override
    public Identifier getRenderFrame() {
        long time = System.currentTimeMillis();
        int totalDuration = frames.size() * frameDuration;
        long timeInLoop = time % totalDuration;

        int elapsedTime = 0;
        for (Identifier frame : frames) {
            elapsedTime += frameDuration;
            if (elapsedTime > timeInLoop)
                return frame;
        }
        return frames.getFirst();
    }
}