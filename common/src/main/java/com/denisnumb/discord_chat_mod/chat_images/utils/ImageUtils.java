package com.denisnumb.discord_chat_mod.chat_images.utils;

import com.denisnumb.discord_chat_mod.chat_images.model.ImageSize;
import com.denisnumb.discord_chat_mod.config.ConfigProvider;
import com.mojang.blaze3d.platform.NativeImage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

import static com.denisnumb.discord_chat_mod.chat_images.ImageStorage.*;

public final class ImageUtils {
    private ImageUtils() {}

    public static final String SPOILER_PREFIX = "/SPOILER_";
    public static final String LOCAL_RESOURCE_PREFIX = "https://localResource/";

    public static boolean isGifPlatformUrl(String url) {
        return isTenorGifUrl(url) || isGiphyGifUrl(url);
    }

    public static boolean isGiphyGifUrl(String url) {
        return url.startsWith("https://giphy.com/");
    }

    public static boolean isTenorGifUrl(String url) {
        return url.startsWith("https://tenor.com/");
    }

    public static boolean isImageUrl(String mimeType) {
        return mimeType.matches("(?i)^image/(png|jpeg|jpg|gif|bmp|webp)$");
    }

    public static boolean isGif(String mimeType) {
        return mimeType.equalsIgnoreCase("image/gif");
    }

    public static String mimeTypeToFileExt(String mimeType){
        return mimeType.isEmpty()
                ? ""
                : "." + mimeType.split("/")[1];
    }

    public static String getMimeType(byte[] bytes) {
        if (bytes.length < 4)
            return "";
        // PNG: 89 50 4E 47
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47)
            return "image/png";
        // JPEG: FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF)
            return "image/jpeg";
        // GIF: 47 49 46 38
        if (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x38)
            return "image/gif";
        // BMP: 42 4D
        if (bytes[0] == 0x42 && bytes[1] == 0x4D)
            return "image/bmp";
        // WebP: RIFF????WEBP
        if (bytes.length >= 12
                && bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46
                && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50)
            return "image/webp";

        return "";
    }

    public static String getMimeType(String url) {
        if (url.isEmpty())
            return "";
        try {
            HttpURLConnection connection = (HttpURLConnection) new URI(url).toURL().openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestMethod("HEAD");
            int loadTimeout = ConfigProvider.getConfig().imageLoadTimeoutMs();
            connection.setConnectTimeout(loadTimeout);
            connection.setReadTimeout(loadTimeout);
            connection.connect();
            String contentType = connection.getContentType();
            return contentType == null ? "" : contentType;
        } catch (Exception e) {
            return "";
        }
    }

    public static InputStream getImageInputStreamFromUrl(String url) throws IOException, URISyntaxException {
        HttpURLConnection conn = (HttpURLConnection) new URI(url).toURL().openConnection();
        int loadTimeout = ConfigProvider.getConfig().imageLoadTimeoutMs();
        conn.setConnectTimeout(loadTimeout);
        conn.setReadTimeout(loadTimeout);
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        + "AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/124.0.0.0 Safari/537.36");

        return conn.getInputStream();
    }

    public static byte[] convertToPngIfNeeded(byte[] imageBytes) throws IOException {
        if (imageBytes.length >= 4
                && imageBytes[0] == (byte) 0x89
                && imageBytes[1] == (byte) 0x50
                && imageBytes[2] == (byte) 0x4E
                && imageBytes[3] == (byte) 0x47) {
            return imageBytes;
        }

        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (bufferedImage == null)
            throw new IOException("Unsupported image format");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);
        return baos.toByteArray();
    }

    public static boolean isLocalResourceUrl(String imageUrl){
        return imageUrl.startsWith(LOCAL_RESOURCE_PREFIX);
    }

    public static boolean isSpoilerImageUrl(String imageUrl){
        return imageUrl.contains(SPOILER_PREFIX);
    }

    public static ImageSize getImageScaledSize(int width, int height){
        if (width > MAX_WIDTH || height > MAX_HEIGHT){
            float imageScale = Math.min(MAX_WIDTH / width, MAX_HEIGHT / height);
            width = (int) (width * imageScale);
            height = (int) (height * imageScale);
        }

        return new ImageSize(width, height);
    }

    public static byte[] scaleImage(byte[] originalBytes, double scale) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(originalBytes)) {
            BufferedImage original = ImageIO.read(bais);
            if (original == null)
                throw new IOException("Invalid image data");

            int newWidth = (int) Math.round(original.getWidth() * scale);
            int newHeight = (int) Math.round(original.getHeight() * scale);

            BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);

            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(original, 0, 0, newWidth, newHeight, null);
            g.dispose();

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(scaled, "png", baos);
                return baos.toByteArray();
            }
        }
    }

    public static void applyPixelation(NativeImage image, int pixelSize) {
        if (pixelSize <= 1)
            return;

        int width = image.getWidth();
        int height = image.getHeight();

        NativeImage small = new NativeImage(width / pixelSize, height / pixelSize, false);

        for (int y = 0; y < small.getHeight(); y++) {
            for (int x = 0; x < small.getWidth(); x++) {
                int srcX = x * pixelSize + pixelSize / 2;
                int srcY = y * pixelSize + pixelSize / 2;

                srcX = Math.min(srcX, width - 1);
                srcY = Math.min(srcY, height - 1);

                int color = image.getPixel(srcX, srcY);
                small.setPixel(x, y, color);
            }
        }

        for (int y = 0; y < height; y++) {
            int srcY = y / pixelSize;
            for (int x = 0; x < width; x++) {
                int srcX = x / pixelSize;

                srcX = Math.min(srcX, small.getWidth() - 1);
                srcY = Math.min(srcY, small.getHeight() - 1);

                int color = small.getPixel(srcX, srcY);
                image.setPixel(x, y, color);
            }
        }

        small.close();
    }

    public static void applySpoilerOverlay(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        float scale = width >= height ? 0.65f : 0.85f;
        int boxWidth = (int)(width * scale);
        int boxHeight = (int)(height * 0.25f);

        int startX = (width - boxWidth) / 2;
        int startY = (height - boxHeight) / 2;

        int radius = Math.min(boxWidth, boxHeight) / 2;
        int overlayColor = argb(0x95, 0x00, 0x00, 0x00);

        for (int y = 0; y < boxHeight; y++) {
            for (int x = 0; x < boxWidth; x++) {
                if (!isInsideRoundedRect(x, y, boxWidth, boxHeight, radius))
                    continue;

                int px = startX + x;
                int py = startY + y;

                if (px < 0 || px >= width || py < 0 || py >= height)
                    continue;

                int baseColor = image.getPixel(px, py);
                int blended = blendARGB(baseColor, overlayColor);
                image.setPixel(px, py, blended);
            }
        }
    }

    private static boolean isInsideRoundedRect(int x, int y, int width, int height, int radius) {
        if (x >= radius && x < width - radius && y >= 0 && y < height)
            return true;
        if (y >= radius && y < height - radius && x >= 0 && x < width)
            return true;

        int dx, dy;

        // Upper left corner
        dx = radius - x;
        dy = radius - y;
        if (x < radius && y < radius && dx*dx + dy*dy <= radius*radius)
            return true;

        // Upper right corner
        dx = x - (width - radius - 1);
        dy = radius - y;
        if (x >= width - radius && y < radius && dx*dx + dy*dy <= radius*radius)
            return true;

        // Lower left corner
        dx = radius - x;
        dy = y - (height - radius - 1);
        if (x < radius && y >= height - radius && dx*dx + dy*dy <= radius*radius)
            return true;

        // Lower right corner
        dx = x - (width - radius - 1);
        dy = y - (height - radius - 1);
        return x >= width - radius && y >= height - radius && dx * dx + dy * dy <= radius * radius;
    }



    private static int argb(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8)  |
                (b & 0xFF);
    }

    private static int blendARGB(int base, int overlay) {
        int baseA = (base >> 24) & 0xFF;
        int baseR = (base >> 16) & 0xFF;
        int baseG = (base >> 8)  & 0xFF;
        int baseB = base & 0xFF;

        int overlayA = (overlay >> 24) & 0xFF;
        int overlayR = (overlay >> 16) & 0xFF;
        int overlayG = (overlay >> 8)  & 0xFF;
        int overlayB = overlay & 0xFF;

        float alpha = overlayA / 255.0f;

        int outR = (int)(overlayR * alpha + baseR * (1 - alpha));
        int outG = (int)(overlayG * alpha + baseG * (1 - alpha));
        int outB = (int)(overlayB * alpha + baseB * (1 - alpha));

        return argb(baseA, outR, outG, outB);
    }
}