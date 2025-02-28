package com.denisnumb.discord_chat_mod.chat_images;

import com.denisnumb.discord_chat_mod.DiscordChatMod;
import com.denisnumb.discord_chat_mod.chat_images.model.*;
import com.denisnumb.discord_chat_mod.chat_images.model.Image;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static com.denisnumb.discord_chat_mod.chat_images.ImageUtils.*;
import static com.mojang.text2speech.Narrator.LOGGER;

public class ImageStorage {
    public static final float MAX_WIDTH = 128.0f;
    public static final float MAX_HEIGHT = 72.0f;

    public static final Map<String, AbstractImage> IMAGE_CACHE = new HashMap<>();
    public static final Set<String> HANDLED_URLS = new HashSet<>();
    private static CompletableFuture<LoadResult> lastTask = CompletableFuture.completedFuture(null);

    public static CompletableFuture<LoadResult> loadImagesParallel(
            List<String> urls,
            Supplier<Integer> trimmedSize,
            Supplier<Integer> allSize
    ) {
        synchronized (ImageStorage.class) {
            lastTask = lastTask.thenComposeAsync(ignored -> runLoadImages(urls, trimmedSize.get(), allSize.get()));
            return lastTask;
        }
    }

    private static CompletableFuture<LoadResult> runLoadImages(List<String> urls, int trimmedSize, int allSize) {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        List<CompletableFuture<AbstractImage>> futures = urls.stream().distinct()
                .map(url -> CompletableFuture.supplyAsync(() -> parseImage(url), executor))
                .toList();

        return CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> new LoadResult(
                        trimmedSize,
                        allSize,
                        urls.stream().map(ImageStorage::parseImage).toList()
                ))
                .whenComplete((res, ex) -> executor.shutdown());
    }

    @Nullable
    private static AbstractImage parseImage(String url) {
        if (IMAGE_CACHE.containsKey(url))
            return IMAGE_CACHE.get(url);
        if (HANDLED_URLS.contains(url))
            return null;

        HANDLED_URLS.add(url);

        String mimeType = getMimeType(url);

        if (isImageUrl(mimeType)){
            try {
                if (isGifUrl(mimeType))
                    loadGif(url);
                else
                    loadImage(url);
                return parseImage(url);
            } catch (Exception e){
                LOGGER.error("ImageLoadError: {}", e.getMessage());
            }
        }
        return null;
    }

    private static void loadImage(String imageUrl) throws IOException, InterruptedException {
        try (InputStream inputStream = new URL(imageUrl).openStream()) {
            NativeImage nativeImage = NativeImage.read(inputStream);
            ResourceLocation textureLocation = new ResourceLocation(
                    DiscordChatMod.MODID + "/chat_image/" + imageUrl.hashCode()
            );

            CountDownLatch latch = new CountDownLatch(1);
            RenderSystem.recordRenderCall(() -> {
                try {
                    Minecraft.getInstance().getTextureManager().register(textureLocation, new DynamicTexture(nativeImage));
                } finally {
                    latch.countDown();
                }
            });
            latch.await();

            IMAGE_CACHE.put(imageUrl, new Image(
                    imageUrl,
                    getImageScaledSize(nativeImage.getWidth(), nativeImage.getHeight()),
                    new ImageSize(nativeImage.getWidth(), nativeImage.getHeight()),
                    textureLocation
            ));
        }
    }

    private static void loadGif(String gifUrl) throws IOException, InterruptedException {
        List<ResourceLocation> frames = new ArrayList<>();
        ImageSize frameSize = null;
        int frameDuration = -1;

        ImageInputStream input = ImageIO.createImageInputStream(new URL(gifUrl).openStream());
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
        if (!readers.hasNext())
            throw new IOException("No GIF reader found");

        ImageReader reader = readers.next();
        reader.setInput(input, false);
        int numFrames = reader.getNumImages(true);
        CountDownLatch latch = new CountDownLatch(numFrames);

        for (int i = 0; i < numFrames; i++) {
            BufferedImage frame = reader.read(i);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(frame, "png", outputStream);

            if (frameSize == null)
                frameSize = new ImageSize(frame.getWidth(), frame.getHeight());
            if (frameDuration == -1)
                frameDuration = getFrameDuration(reader.getImageMetadata(i));

            ResourceLocation frameLocation = new ResourceLocation(
                    DiscordChatMod.MODID + "/chat_gif/" + gifUrl.hashCode() + "_" + i
            );
            NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(outputStream.toByteArray()));
            RenderSystem.recordRenderCall(() -> {
                try {
                    Minecraft.getInstance().getTextureManager().register(frameLocation, new DynamicTexture(nativeImage));
                } finally {
                    latch.countDown();
                }
            });
            frames.add(frameLocation);
        }

        reader.dispose();
        input.close();
        latch.await();

        if (!frames.isEmpty()){
            IMAGE_CACHE.put(gifUrl, new AnimatedImage(
                    gifUrl,
                    frames,
                    getImageScaledSize(frameSize.width(), frameSize.height()),
                    frameSize,
                    frameDuration <= 0 ? 100 : frameDuration)
            );
        }
    }
}
