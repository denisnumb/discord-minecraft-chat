package com.denisnumb.discord_chat_mod.chat_images.utils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public final class WebpUtils {
    private WebpUtils() {}

    public record FrameMetadata(int xOffset, int yOffset, int width, int height) { }

    private static int readLittleEndian3Bytes(byte[] data, int offset) {
        return (data[offset] & 0xFF)
                | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16);
    }

    public static List<FrameMetadata> parseAnimatedWebP(InputStream inputStream) throws IOException {
        List<FrameMetadata> frames = new ArrayList<>();

        byte[] data = inputStream.readAllBytes();
        int offset = 12;
        while (offset < data.length - 8) {
            String chunkId = new String(data, offset, 4);
            int chunkSize = ByteBuffer.wrap(data, offset + 4, 4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .getInt();

            if (chunkId.equals("ANMF")) {
                byte[] frameData = Arrays.copyOfRange(data, offset + 8, offset + 20);

                int xOffset = readLittleEndian3Bytes(frameData, 0) * 2;
                int yOffset = readLittleEndian3Bytes(frameData, 3) * 2;
                int width = readLittleEndian3Bytes(frameData, 6) + 1;
                int height = readLittleEndian3Bytes(frameData, 9) + 1;

                frames.add(new FrameMetadata(xOffset, yOffset, width, height));
            }

            offset += 8 + chunkSize + (chunkSize % 2);
        }

        return frames;
    }

    public static boolean isAnimatedWebp(byte[] bytes, String mimeType) {
        if (!mimeType.equalsIgnoreCase("image/webp"))
            return false;

        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("webp");
            if (!readers.hasNext())
                throw new RuntimeException("WebP ImageReader not found!");

            ImageReader reader = readers.next();
            reader.setInput(imageInputStream);

            boolean animated = reader.getNumImages(true) > 1;
            reader.dispose();
            return animated;
        } catch (Exception e){
            return false;
        }
    }
}
