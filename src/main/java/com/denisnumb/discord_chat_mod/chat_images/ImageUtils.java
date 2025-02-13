package com.denisnumb.discord_chat_mod.chat_images;

import com.denisnumb.discord_chat_mod.chat_images.model.AnimatedImage;
import com.denisnumb.discord_chat_mod.chat_images.model.ImageSize;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOMetadata;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.denisnumb.discord_chat_mod.chat_images.ImageStorage.MAX_HEIGHT;
import static com.denisnumb.discord_chat_mod.chat_images.ImageStorage.MAX_WIDTH;

public class ImageUtils {
    public static boolean isImageUrl(String mimeType) {
        return mimeType.matches("(?i)^image/(png|jpeg|jpg|gif|bmp|webp)$");
    }

    public static boolean isGifUrl(String mimeType) {
        return mimeType.equalsIgnoreCase("image/gif");
    }

    public static String getMimeType(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            connection.connect();
            return connection.getContentType();
        } catch (Exception e) {
            return "";
        }
    }

    public static ImageSize getImageScaledSize(int width, int height){
        if (width > MAX_WIDTH || height > MAX_HEIGHT){
            float imageScale = Math.min(MAX_WIDTH / width, MAX_HEIGHT / height);
            width = (int) (width * imageScale);
            height = (int) (height * imageScale);
        }

        return new ImageSize(width, height);
    }

    public static int getFrameDuration(IIOMetadata metadata) {
        String metaFormat = metadata.getNativeMetadataFormatName();
        if (!"javax_imageio_gif_image_1.0".equals(metaFormat))
            return 100;

        Node root = metadata.getAsTree(metaFormat);
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if ("GraphicControlExtension".equals(node.getNodeName())) {
                Node delayNode = node.getAttributes().getNamedItem("delayTime");
                if (delayNode != null)
                    return Integer.parseInt(delayNode.getNodeValue()) * 10;
            }
        }

        return 100;
    }

    public static int getCurrentFrameIndex(AnimatedImage animation) {
        long time = System.currentTimeMillis();
        int totalDuration = animation.frames.size() * animation.frameDuration;
        long timeInLoop = time % totalDuration;

        int elapsedTime = 0;
        for (int i = 0; i < animation.frames.size(); i++) {
            elapsedTime += animation.frameDuration;
            if (elapsedTime > timeInLoop)
                return i;
        }
        return 0;
    }
}
