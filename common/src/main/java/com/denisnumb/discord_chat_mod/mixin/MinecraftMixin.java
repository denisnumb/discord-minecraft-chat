package com.denisnumb.discord_chat_mod.mixin;

import com.denisnumb.discord_chat_mod.MinecraftUtils;
import com.denisnumb.discord_chat_mod.chat_images.ImageSendScreen;
import com.denisnumb.discord_chat_mod.chat_images.ImageStorage;
import com.denisnumb.discord_chat_mod.chat_images.model.AbstractImage;
import com.denisnumb.discord_chat_mod.chat_images.utils.ImageUtils;
import com.denisnumb.discord_chat_mod.locale.MinecraftLocaleProvider;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.nio.file.Files;

import static com.denisnumb.discord_chat_mod.chat_images.ImageStorage.MAX_DRAG_DROP_FILE_SIZE;
import static com.denisnumb.discord_chat_mod.chat_images.ImageStorage.MAX_DRAG_DROP_FILE_SIZE_MB;
import static com.denisnumb.discord_chat_mod.chat_images.utils.ImageUtils.LOCAL_RESOURCE_PREFIX;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Unique
    private static final Logger discord_chat_mod$LOGGER = LogUtils.getLogger();

    @Inject(method = "resizeDisplay", at = @At("TAIL"))
    private void onWindowReady(CallbackInfo ci) {
        Minecraft mc = (Minecraft)(Object)this;
        GLFW.glfwSetDropCallback(mc.getWindow().handle(), (window, count, names) -> {
            for (int i = 0; i < count; i++) {
                String path = GLFWDropCallback.getName(names, i);
                discord_chat_mod$handleDroppedFile(mc, path);
            }
        });
    }

    @Unique
    private void discord_chat_mod$handleDroppedFile(Minecraft mc, String path) {
        File file = new File(path);
        if (!file.exists() || !file.isFile()) return;

        String name = file.getName().toLowerCase();
        boolean isImage = name.endsWith(".png") || name.endsWith(".jpg")
                || name.endsWith(".jpeg") || name.endsWith(".gif")
                || name.endsWith(".webp") || name.endsWith(".bmp");

        if (!isImage)
            return;

        if (file.length() > MAX_DRAG_DROP_FILE_SIZE) {
            mc.execute(() -> MinecraftUtils.showTitleBarMessage(
                    MinecraftLocaleProvider.SendImage.Error.fileTooLarge(MAX_DRAG_DROP_FILE_SIZE_MB)
            ));
            return;
        }

        mc.execute(() -> {
            if (!(mc.screen instanceof ChatScreen)) return;

            try {
                byte[] imageBytes = Files.readAllBytes(file.toPath());
                String imagePreviewUrl = LOCAL_RESOURCE_PREFIX + System.currentTimeMillis() + "/imagePreview";
                String mimeType = ImageUtils.getMimeType(imageBytes);
                AbstractImage image = ImageStorage.registerImageFromBytes(imagePreviewUrl, mimeType, imageBytes);
                mc.setScreen(new ImageSendScreen(image, imageBytes, mimeType, file.getName(), mc.screen, mc.player));
            } catch (Exception e) {
                MinecraftUtils.showTitleBarMessage(MinecraftLocaleProvider.SendImage.Error.readFile(e.getMessage()));
                discord_chat_mod$LOGGER.error("ReadImageFromFileError: ", e);
            }
        });
    }
}