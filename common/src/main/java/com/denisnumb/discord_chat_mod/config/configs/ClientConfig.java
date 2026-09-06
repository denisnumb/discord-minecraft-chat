package com.denisnumb.discord_chat_mod.config.configs;

import com.electronwill.nightconfig.core.CommentedConfig;

import static com.denisnumb.discord_chat_mod.config.ConfigComments.*;
import static com.denisnumb.discord_chat_mod.config.ConfigDefaults.*;

public final class ClientConfig {
    private ClientConfig() {}

    public static boolean emojifulCompatibility;
    public static int maxChatHistory;
    public static int maxImageCacheSize;
    public static int imageLoadTimeoutMs;
    public static boolean enableAttachImageButton;
    public static boolean enableClipboardImagePaste;

    public static void loadClientConfig(CommentedConfig clientConfig){
        emojifulCompatibility = clientConfig.getOrElse("emojifulCompatibility", EMOJIFUL_COMPATIBILITY_DEFAULT);
        clientConfig.set("emojifulCompatibility", emojifulCompatibility);
        clientConfig.setComment("emojifulCompatibility", EMOJIFUL_COMPATIBILITY_COMMENT);

        maxChatHistory = clientConfig.getOrElse("maxChatHistory", MAX_CHAT_HISTORY_DEFAULT);
        if (maxChatHistory < 20)
            maxChatHistory = 20;
        clientConfig.set("maxChatHistory", maxChatHistory);
        clientConfig.setComment("maxChatHistory", MAX_CHAT_HISTORY_COMMENT + String.format("\n Default: %d\n Range: > 20", MAX_CHAT_HISTORY_DEFAULT));

        maxImageCacheSize = clientConfig.getOrElse("maxImageCacheSize", MAX_IMAGE_CACHE_SIZE_DEFAULT);
        if (maxImageCacheSize < 50) maxImageCacheSize = 50;
        if (maxImageCacheSize > 10000) maxImageCacheSize = 10000;
        clientConfig.set("maxImageCacheSize", maxImageCacheSize);
        clientConfig.setComment("maxImageCacheSize", MAX_IMAGE_CACHE_SIZE_COMMENT + String.format("\n Default: %d\n Range: 50 ~ 10000", MAX_IMAGE_CACHE_SIZE_DEFAULT));

        int imageLoadTimeout = clientConfig.getOrElse("imageLoadTimeout", IMAGE_LOAD_TIMEOUT_DEFAULT);
        if (imageLoadTimeout < 5) imageLoadTimeout = 5;
        if (imageLoadTimeout > 300) imageLoadTimeout = 300;
        imageLoadTimeoutMs = imageLoadTimeout * 1000;
        clientConfig.set("imageLoadTimeout", imageLoadTimeout);
        clientConfig.setComment("imageLoadTimeout", IMAGE_LOAD_TIMEOUT_COMMENT + String.format("\n Default: %d\n Range: 5 ~ 300", IMAGE_LOAD_TIMEOUT_DEFAULT));

        enableAttachImageButton = clientConfig.getOrElse("enableAttachImageButton", ENABLE_ATTACH_IMAGE_BUTTON_DEFAULT);
        clientConfig.set("enableAttachImageButton", enableAttachImageButton);
        clientConfig.setComment("enableAttachImageButton", ENABLE_ATTACH_IMAGE_BUTTON_COMMENT);

        enableClipboardImagePaste = clientConfig.getOrElse("enableClipboardImagePaste", ENABLE_CLIPBOARD_IMAGE_PASTE_DEFAULT);
        clientConfig.set("enableClipboardImagePaste", enableClipboardImagePaste);
        clientConfig.setComment("enableClipboardImagePaste", ENABLE_CLIPBOARD_IMAGE_PASTE_COMMENT);
    }
}
