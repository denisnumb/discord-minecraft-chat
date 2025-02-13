package com.denisnumb.discord_chat_mod.mixin;

import com.denisnumb.discord_chat_mod.chat_images.ImageScreen;
import com.denisnumb.discord_chat_mod.network.screenshot.ScreenshotSender;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Files;

import static com.denisnumb.discord_chat_mod.MinecraftUtils.getTranslateClient;
import static com.denisnumb.discord_chat_mod.ModLanguageKey.SCREENSHOT_SENDING_ERROR;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Shadow @Nullable protected Minecraft minecraft;

    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void handleComponentClicked(Style style, CallbackInfoReturnable<Boolean> cir){
        if (style != null){
            ClickEvent clickevent = style.getClickEvent();
            if (clickevent != null && clickevent.getAction() == ClickEvent.Action.RUN_COMMAND){
                String value = StringUtil.filterText(clickevent.getValue());
                if (value.startsWith("send_screenshot")) {
                    discord_minecraft_chat$sendScreenshot(value.replace("send_screenshot ", ""));
                    cir.setReturnValue(true);
                }
                if (value.startsWith("open_image")) {
                    minecraft.setScreen(new ImageScreen(value.replace("open_image ", "")));
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Unique
    private void discord_minecraft_chat$sendScreenshot(String filePath){
        File screenshotFile = new File(filePath);
        Player player = minecraft.player;

        if (screenshotFile.exists() && screenshotFile.getName().endsWith(".png")){
            try {
                ScreenshotSender.sendScreenshot(Files.readAllBytes(screenshotFile.toPath()));
            } catch (Exception e) {
                player.sendSystemMessage(
                        Component.literal(
                                String.format(getTranslateClient(SCREENSHOT_SENDING_ERROR, "Screenshot sending error: %s"), e.getMessage())
                        ).withStyle(ChatFormatting.RED)
                );
            }
        }
    }
}
