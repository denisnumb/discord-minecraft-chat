package com.denisnumb.discord_chat_mod.mixin;

import com.denisnumb.discord_chat_mod.utils.MinecraftUtils;
import com.denisnumb.discord_chat_mod.chat_images.ImageScreen;
import com.denisnumb.discord_chat_mod.chat_images.ImageSendScreen;
import com.denisnumb.discord_chat_mod.chat_images.ImageStorage;
import com.denisnumb.discord_chat_mod.chat_images.model.AbstractImage;
import com.denisnumb.discord_chat_mod.chat_images.clipboard.ClipboardImageUtils;
import com.denisnumb.discord_chat_mod.chat_images.utils.ImageUtils;
import com.denisnumb.discord_chat_mod.chat_images.widgets.AttachImageWidget;
import com.denisnumb.discord_chat_mod.config.ConfigProvider;
import com.denisnumb.discord_chat_mod.config.IConfigProvider;
import com.denisnumb.discord_chat_mod.locale.MinecraftLocaleProvider;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static com.denisnumb.discord_chat_mod.chat_images.ImageStorage.MAX_DRAG_DROP_FILE_SIZE;
import static com.denisnumb.discord_chat_mod.chat_images.ImageStorage.MAX_DRAG_DROP_FILE_SIZE_MB;
import static com.denisnumb.discord_chat_mod.chat_images.utils.ImageUtils.LOCAL_RESOURCE_PREFIX;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    @Unique
    private static final Logger discord_chat_mod$LOGGER = LogUtils.getLogger();

    @Shadow
    protected EditBox input;

    protected ChatScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        IConfigProvider config = ConfigProvider.getConfig();
        if (!config.isAttachImageButtonEnabled() || config.isEmojifulCompatibilityEnabled()){
            return;
        }

        int iconSize = this.input.getHeight();
        int padding = 2;

        int newWidth = this.input.getWidth() - iconSize;
        this.input.setWidth(newWidth);

        int iconX = this.input.getX() + this.input.getWidth() - padding;
        int iconY = this.input.getY() - padding;

        addRenderableWidget(new AttachImageWidget(
                iconX, iconY, iconSize, iconSize,
                btn -> discord_minecraft_chat$openImageFileDialog()
        ));
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (!keyEvent.isPaste() || !ConfigProvider.getConfig().isClipboardImagePasteEnabled())
            return;

        try {
            byte[] imageBytes = ClipboardImageUtils.getImageBytes();
            if (imageBytes.length == 0)
                return;

            discord_minecraft_chat$openImageSendScreen(imageBytes, MinecraftLocaleProvider.image().getString());
            cir.setReturnValue(true);
        } catch (IllegalStateException e) {
            MinecraftUtils.showTitleBarMessage(MinecraftLocaleProvider.SendImage.Error.readClipboard(e.getMessage()));
            discord_chat_mod$LOGGER.error("ClipboardImagePasteError: ", e);
        }
    }

    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void handleComponentClicked(Style style, boolean bl, CallbackInfoReturnable<Boolean> cir){
        if (style != null){
            ClickEvent clickEvent = style.getClickEvent();
            if (clickEvent != null){
                if (clickEvent instanceof ClickEvent.OpenUrl(URI uri) && ImageUtils.isLocalResourceUrl(uri.toString())){
                    cir.setReturnValue(true);
                }
                if (clickEvent instanceof ClickEvent.RunCommand(String command)){
                    String value = StringUtil.filterText(command);
                    if (value.startsWith(ImageStorage.SEND_SCREENSHOT_COMMAND)) {
                        File screenshotFile = new File(value.replace(ImageStorage.SEND_SCREENSHOT_COMMAND, ""));
                        if (screenshotFile.exists() && screenshotFile.isFile() && screenshotFile.getName().endsWith(".png")) {
                            try {
                                discord_minecraft_chat$openImageSendScreen(
                                        Files.readAllBytes(screenshotFile.toPath()),
                                        MinecraftLocaleProvider.screenshot().getString()
                                );
                            } catch (IOException ignored) {}
                        }
                        cir.setReturnValue(true);
                    }
                    if (value.startsWith(ImageStorage.OPEN_IMAGE_COMMAND)) {
                        String imageUrl = value.replace(ImageStorage.OPEN_IMAGE_COMMAND, "");
                        AbstractImage image = ImageStorage.IMAGE_CACHE.getOrDefault(imageUrl, null);
                        if (image != null){
                            if (image.isSpoilerAndNotOpened())
                                image.openSpoiler();
                            else {
                                minecraft.setScreen(new ImageScreen(image, minecraft.screen));
                            }
                        }
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    @Unique
    private void discord_minecraft_chat$openImageFileDialog() {
        CompletableFuture.runAsync(() -> {
            String path;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(5);
                filters.put(stack.UTF8("*.png"));
                filters.put(stack.UTF8("*.jpg"));
                filters.put(stack.UTF8("*.gif"));
                filters.put(stack.UTF8("*.webp"));
                filters.put(stack.UTF8("*.bmp"));
                filters.flip();

                path = TinyFileDialogs.tinyfd_openFileDialog(
                        MinecraftLocaleProvider.SendImage.selectImage().getString(),
                        "",
                        filters,
                        "Image Files (png, jpg, gif, webp, bmp)",
                        false
                );
            }

            if (path == null)
                return;

            minecraft.execute(() -> {
                try {
                    Path filePath = Path.of(path);
                    if (Files.size(filePath) > MAX_DRAG_DROP_FILE_SIZE) {
                        MinecraftUtils.showTitleBarMessage(MinecraftLocaleProvider.SendImage.Error.fileTooLarge(MAX_DRAG_DROP_FILE_SIZE_MB));
                        return;
                    }

                    byte[] imageBytes = Files.readAllBytes(filePath);
                    discord_minecraft_chat$openImageSendScreen(imageBytes, filePath.getFileName().toString());
                } catch (IOException e) {
                    MinecraftUtils.showTitleBarMessage(MinecraftLocaleProvider.SendImage.Error.readFile(e.getMessage()));
                    discord_chat_mod$LOGGER.error("ReadImageFromFileError: ", e);
                }
            });
        });
    }

    @Unique
    private void discord_minecraft_chat$openImageSendScreen(byte[] imageBytes, String displayName) {
        try {
            String imagePreviewUrl = LOCAL_RESOURCE_PREFIX + System.currentTimeMillis() + "/imagePreview";
            String mimeType = ImageUtils.getMimeType(imageBytes);
            AbstractImage image = ImageStorage.registerImageFromBytes(imagePreviewUrl, mimeType, imageBytes);
            minecraft.setScreen(new ImageSendScreen(image, imageBytes, mimeType, displayName, minecraft.screen, minecraft.player));
        } catch (Exception ignored) {}
    }
}
