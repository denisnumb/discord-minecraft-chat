package com.denisnumb.discord_chat_mod.chat_images;

import com.denisnumb.discord_chat_mod.DiscordChatMod;
import com.denisnumb.discord_chat_mod.utils.JavaUtils;
import com.denisnumb.discord_chat_mod.utils.MinecraftUtils;
import com.denisnumb.discord_chat_mod.chat_images.model.*;
import com.denisnumb.discord_chat_mod.chat_images.model.Image;
import com.denisnumb.discord_chat_mod.chat_images.utils.ImageUtils;
import com.denisnumb.discord_chat_mod.config.ConfigProvider;
import com.denisnumb.discord_chat_mod.mixin.ChatComponentAccessor;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.denisnumb.discord_chat_mod.chat_images.utils.ImageUtils.*;
import static com.denisnumb.discord_chat_mod.chat_images.utils.WebpUtils.*;
import static com.denisnumb.discord_chat_mod.chat_images.utils.GifUtils.*;
import static com.denisnumb.discord_chat_mod.DiscordChatMod.LOGGER;

public final class ImageStorage {
    private ImageStorage() {}

    public static final float MAX_WIDTH = 128.0f;
    public static final float MAX_HEIGHT = 72.0f;
    public static final String OPEN_IMAGE_COMMAND = "open_image ";
    public static final String SEND_SCREENSHOT_COMMAND = "send_screenshot ";
    public static final int MAX_DRAG_DROP_FILE_SIZE_MB = 25;
    public static final long MAX_DRAG_DROP_FILE_SIZE = MAX_DRAG_DROP_FILE_SIZE_MB * 1024 * 1024;
    private static Integer MAX_CACHE_SIZE;

    private static CompletableFuture<List<AbstractImage>> lastTask = CompletableFuture.completedFuture(null);
    private static final Set<String> HANDLED_URLS = new HashSet<>();

    public static final Map<String, AbstractImage> IMAGE_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, AbstractImage> eldest) {
                    if (size() > getMaxCacheSize()) {
                        releaseImageResources(eldest.getValue());
                        return true;
                    }
                    return false;
                }
            }
    );

    private static int getMaxCacheSize(){
        if (MAX_CACHE_SIZE == null) {
            MAX_CACHE_SIZE = ConfigProvider.getConfig().maxImageCacheSize();
        }

        return MAX_CACHE_SIZE;
    }

    public static void evictImage(String url) {
        AbstractImage image = IMAGE_CACHE.remove(url);
        if (image != null)
            releaseImageResources(image);
    }

    public static CompletableFuture<List<AbstractImage>> loadImagesParallel(List<String> urls) {
        synchronized (ImageStorage.class) {
            lastTask = lastTask.thenComposeAsync(ignored -> runLoadImages(urls));
            return lastTask;
        }
    }

    private static CompletableFuture<List<AbstractImage>> runLoadImages(List<String> urls) {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        List<CompletableFuture<AbstractImage>> futures = urls.stream()
                .map(url -> CompletableFuture.supplyAsync(() -> parseImage(url), executor))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().map(CompletableFuture::join).toList())
                .whenComplete((res, ex) -> executor.shutdown());
    }

    @Nullable
    public static AbstractImage parseEmojiOrSticker(String url) {
        return parseImageInternal(url, false);
    }

    @Nullable
    private static AbstractImage parseImage(String url) {
        return parseImageInternal(url, true);
    }

    @Nullable
    private static AbstractImage parseImageInternal(String url, boolean skipHandledUrls) {
        if (IMAGE_CACHE.containsKey(url))
            return IMAGE_CACHE.get(url);
        if (skipHandledUrls && HANDLED_URLS.contains(url))
            return null;
        if (skipHandledUrls)
            HANDLED_URLS.add(url);

        if (isLocalResourceUrl(url))
            return JavaUtils.waitForLocalResource(IMAGE_CACHE, url, 15000, 50);

        String mimeType = getMimeType(url);
        if (isImageUrl(mimeType) || isGifPlatformUrl(url)) {
            try {
                registerImageFromUrl(url, mimeType);
                return parseImageInternal(url, skipHandledUrls);
            } catch (Exception e) {
                LOGGER.error("ImageLoadError", e);
            }
        }

        return null;
    }

    public static AbstractImage registerImageFromBytes(String imageUrl, String mimeType, byte[] imageBytes) throws Exception {
        return registerByMimeType(imageUrl, mimeType, imageBytes);
    }

    private static void registerImageFromUrl(String imageUrl, String mimeType) throws Exception {
        try (InputStream input = getImageInputStreamFromUrl(imageUrl)) {
            registerByMimeType(imageUrl, mimeType, input.readAllBytes());
        }
    }

    private static AbstractImage registerByMimeType(String imageUrl, String mimeType, byte[] bytes) throws Exception {
        if (isGifPlatformUrl(imageUrl) || isGif(mimeType)) {
            return registerGif(imageUrl, bytes);
        } else if (isAnimatedWebp(bytes, mimeType)) {
            return registerAnimatedWebp(imageUrl, bytes);
        }

        return registerImage(imageUrl, bytes);
    }

    private static AbstractImage registerImage(String imageUrl, byte[] imageBytes) throws Exception {
        imageBytes = ImageUtils.convertToPngIfNeeded(imageBytes);

        NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(imageBytes));
        boolean isSpoiler = ImageUtils.isSpoilerImageUrl(imageUrl);
        Identifier textureLocation = Identifier.parse(
                DiscordChatMod.MOD_ID + "/chat_image/" + imageUrl.hashCode()
        );
        Identifier spoilerTextureLocation = isSpoiler
                ? buildSpoilerTexture(imageUrl, NativeImage.read(new ByteArrayInputStream(imageBytes)))
                : null;

        registerTexture(textureLocation, nativeImage);

        Image image = new Image(
                imageUrl,
                getImageScaledSize(nativeImage.getWidth(), nativeImage.getHeight()),
                new ImageSize(nativeImage.getWidth(), nativeImage.getHeight()),
                textureLocation,
                isSpoiler,
                spoilerTextureLocation
        );
        IMAGE_CACHE.put(imageUrl, image);

        return image;
    }

    private static AbstractImage registerAnimatedWebp(String webpUrl, byte[] webpData) throws Exception {
        List<Identifier> frames = new ArrayList<>();
        ImageSize frameSize = null;
        int frameDuration = -1;
        boolean isSpoiler = ImageUtils.isSpoilerImageUrl(webpUrl);
        Identifier spoilerTextureLocation = null;

        List<FrameMetadata> frameMetadata;
        try (ByteArrayInputStream metaStream = new ByteArrayInputStream(webpData)) {
            frameMetadata = parseAnimatedWebP(metaStream);
        }

        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(webpData))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext())
                throw new IllegalStateException("No ImageReader for WebP");

            ImageReader reader = readers.next();
            reader.setInput(input);

            int frameCount = reader.getNumImages(true);
            BufferedImage canvas = new BufferedImage(
                    reader.getWidth(0),
                    reader.getHeight(0),
                    BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D g = canvas.createGraphics();

            for (int i = 0; i < frameCount; i++) {
                BufferedImage frame = reader.read(i);

                FrameMetadata meta = (i < frameMetadata.size())
                        ? frameMetadata.get(i)
                        : new FrameMetadata(0, 0, frame.getWidth(), frame.getHeight());

                g.drawImage(frame, meta.xOffset(), meta.yOffset(), null);

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageIO.write(canvas, "png", outputStream);
                byte[] imageBytes = outputStream.toByteArray();

                if (i == 0) {
                    frameSize = new ImageSize(frame.getWidth(), frame.getHeight());
                    frameDuration = 100;

                    if (isSpoiler)
                        spoilerTextureLocation = buildSpoilerTexture(webpUrl, NativeImage.read(new ByteArrayInputStream(imageBytes)));
                }

                Identifier frameLocation = Identifier.parse(
                        DiscordChatMod.MOD_ID + "/chat_webp/" + webpUrl.hashCode() + "_" + i
                );
                registerTexture(frameLocation, NativeImage.read(new ByteArrayInputStream(imageBytes)));
                frames.add(frameLocation);
            }

            g.dispose();
            reader.dispose();

            if (!frames.isEmpty()) {
                AnimatedImage image = new AnimatedImage(
                        webpUrl,
                        frames,
                        getImageScaledSize(frameSize.width(), frameSize.height()),
                        frameSize,
                        frameDuration,
                        isSpoiler,
                        spoilerTextureLocation
                );
                IMAGE_CACHE.put(webpUrl, image);
                return image;
            }

            throw new IllegalStateException("Failed to retrieve image frames");
        }
    }

    private static AbstractImage registerGif(String gifUrl, byte[] gifData) throws Exception {
        String cacheKey = gifUrl;
        if (isGiphyGifUrl(gifUrl))
            gifUrl = getGiphyGifSourceUrl(gifUrl);
        if (isTenorGifUrl(gifUrl))
            gifUrl = getTenorGifSourceUrl(gifUrl);

        List<Identifier> frames = new ArrayList<>();
        ImageSize frameSize = null;
        int frameDuration = -1;
        boolean isSpoiler = ImageUtils.isSpoilerImageUrl(gifUrl);
        Identifier spoilerTextureLocation = null;

        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(gifData))) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) throw new IOException("No GIF reader found");

            ImageReader reader = readers.next();
            reader.setInput(input);
            int numFrames = reader.getNumImages(true);
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = canvas.createGraphics();
            g2d.setBackground(new Color(0, 0, 0, 0));
            g2d.clearRect(0, 0, width, height);

            BufferedImage savedCanvas = null;

            for (int i = 0; i < numFrames; i++) {
                BufferedImage frame = reader.read(i);
                IIOMetadata metadata = reader.getImageMetadata(i);
                Rectangle frameRect = getFrameRect(metadata);
                String disposal = getDisposalMethod(metadata);

                switch (disposal) {
                    case "restoreToPrevious":
                        savedCanvas = bufferedImageDeepCopy(canvas);
                        break;
                    case "restoreToBackgroundColor":
                        clearBufferedImageArea(canvas, frameRect);
                        break;
                }

                Graphics2D g = canvas.createGraphics();
                g.drawImage(frame, frameRect.x, frameRect.y, null);
                g.dispose();

                if ("restoreToPrevious".equals(disposal) && savedCanvas != null) {
                    Graphics2D gRestore = canvas.createGraphics();
                    gRestore.drawImage(savedCanvas, 0, 0, null);
                    gRestore.dispose();
                }

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageIO.write(canvas, "png", outputStream);
                byte[] imageBytes = outputStream.toByteArray();

                if (i == 0) {
                    frameSize = new ImageSize(frame.getWidth(), frame.getHeight());
                    frameDuration = getGifFrameDuration(metadata);

                    if (isSpoiler)
                        spoilerTextureLocation = buildSpoilerTexture(gifUrl, NativeImage.read(new ByteArrayInputStream(imageBytes)));
                }

                Identifier frameLocation = Identifier.parse(
                        DiscordChatMod.MOD_ID + "/chat_gif/" + gifUrl.hashCode() + "_" + i
                );
                registerTexture(frameLocation, NativeImage.read(new ByteArrayInputStream(imageBytes)));
                frames.add(frameLocation);
            }

            g2d.dispose();
            reader.dispose();

            if (!frames.isEmpty()) {
                AnimatedImage image = new AnimatedImage(
                        cacheKey,
                        frames,
                        getImageScaledSize(frameSize.width(), frameSize.height()),
                        frameSize,
                        Math.max(frameDuration, 100),
                        isSpoiler,
                        spoilerTextureLocation
                );
                IMAGE_CACHE.put(cacheKey, image);
                return image;
            }

            throw new IllegalStateException("Failed to retrieve image frames");
        }
    }

    private static void releaseImageResources(AbstractImage image) {
        HANDLED_URLS.remove(image.url);

        Minecraft.getInstance().execute(() -> {
            ChatComponentAccessor accessor = (ChatComponentAccessor) Minecraft.getInstance().gui.getChat();
            String command = OPEN_IMAGE_COMMAND + image.url;

            accessor.getTrimmedMessages().removeIf(line ->
                    MinecraftUtils.hasRunCommandClickEvent(line.content(), command)
            );
            accessor.getAllMessages().removeIf(msg ->
                    MinecraftUtils.hasRunCommandClickEvent(msg.content().getVisualOrderText(), command)
            );

            TextureManager tm = Minecraft.getInstance().getTextureManager();

            if (image instanceof AnimatedImage animated) {
                for (Identifier frame : animated.frames) {
                    if (tm.getTexture(frame) instanceof DynamicTexture dt)
                        dt.close();
                    tm.release(frame);
                }
            } else if (image instanceof Image img) {
                if (tm.getTexture(img.resourceLocation) instanceof DynamicTexture dt)
                    dt.close();
                tm.release(img.resourceLocation);
            }

            if (image.spoilerIdentifier != null) {
                if (tm.getTexture(image.spoilerIdentifier) instanceof DynamicTexture dt)
                    dt.close();
                tm.release(image.spoilerIdentifier);
            }
        });
    }

    private static void registerTexture(Identifier location, NativeImage image) throws InterruptedException {
        if (RenderSystem.isOnRenderThread()) {
            Minecraft.getInstance().getTextureManager().register(
                    location,
                    new DynamicTexture(location::getPath, image)
            );
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            Minecraft.getInstance().execute(() -> {
                try {
                    Minecraft.getInstance().getTextureManager().register(
                            location,
                            new DynamicTexture(location::getPath, image)
                    );
                } finally {
                    latch.countDown();
                }
            });
            latch.await();
        }
    }

    private static Identifier buildSpoilerTexture(String imageUrl, NativeImage image) throws InterruptedException {
        ImageUtils.applyPixelation(image, image.getHeight() / 6);
        ImageUtils.applySpoilerOverlay(image);
        Identifier location = Identifier.parse(
                DiscordChatMod.MOD_ID + "/chat_image_spoiler/" + imageUrl.hashCode()
        );
        registerTexture(location, image);
        return location;
    }
}