package com.denisnumb.discord_chat_mod.chat_images.utils;

import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;

public final class GifUtils {
    private GifUtils() {}

    public static String getGiphyGifSourceUrl(String giphyUrl) throws IllegalArgumentException {
        String url = giphyUrl.stripTrailing();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        String lastSegment = url.substring(url.lastIndexOf('/') + 1);

        int lastDash = lastSegment.lastIndexOf('-');
        String gifId = lastDash >= 0
                ? lastSegment.substring(lastDash + 1)
                : lastSegment;

        if (gifId.isBlank()) {
            throw new IllegalArgumentException("Failed to extract gif ID from URL: " + giphyUrl);
        }

        return "https://i.giphy.com/" + gifId + ".gif";
    }

    public static String getTenorGifSourceUrl(String tenorUrl) throws IOException {
        Document doc = Jsoup.connect(tenorUrl)
                .userAgent("Mozilla/5.0")
                .get();

        Element gifMeta = doc.selectFirst("meta[property=og:image]");
        if (gifMeta != null) {
            String intermediateGifUrl = gifMeta.attr("content");
            String id = extractTenorGifId(intermediateGifUrl);
            if (id != null)
                return "https://c.tenor.com/" + id + "/tenor.gif";
        }

        throw new IOException("No tenor gif source url found");
    }

    @Nullable
    private static String extractTenorGifId(String url) {
        int start = url.indexOf("/m/") + 3;
        int end = url.indexOf('/', start);
        if (start > 2 && end > start) {
            return url.substring(start, end);
        }

        return null;
    }

    public static String getDisposalMethod(IIOMetadata metadata) {
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree("javax_imageio_gif_image_1.0");
        IIOMetadataNode gce = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);

        return gce.getAttribute("disposalMethod");
    }

    public static Rectangle getFrameRect(IIOMetadata metadata) {
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree("javax_imageio_gif_image_1.0");
        IIOMetadataNode imgDesc = (IIOMetadataNode) root.getElementsByTagName("ImageDescriptor").item(0);
        int x = Integer.parseInt(imgDesc.getAttribute("imageLeftPosition"));
        int y = Integer.parseInt(imgDesc.getAttribute("imageTopPosition"));
        int w = Integer.parseInt(imgDesc.getAttribute("imageWidth"));
        int h = Integer.parseInt(imgDesc.getAttribute("imageHeight"));

        return new Rectangle(x, y, w, h);
    }

    public static void clearBufferedImageArea(BufferedImage canvas, Rectangle rect) {
        Graphics2D g = canvas.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
        g.dispose();
    }

    public static BufferedImage bufferedImageDeepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPreMultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPreMultiplied, null);
    }

    public static int getGifFrameDuration(IIOMetadata metadata) {
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
}
