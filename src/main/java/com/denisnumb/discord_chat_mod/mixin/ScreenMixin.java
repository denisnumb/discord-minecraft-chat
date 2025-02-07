package com.denisnumb.discord_chat_mod.mixin;

import com.denisnumb.discord_chat_mod.network.screenshot.ScreenshotSender;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;

import javax.annotation.Nullable;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Set;

import static com.denisnumb.discord_chat_mod.MinecraftUtils.getTranslateClient;
import static com.denisnumb.discord_chat_mod.ModLanguageKey.SCREENSHOT_SENDING_ERROR;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Shadow
    public static boolean hasShiftDown() {
        return false;
    }

    @Shadow
    protected void insertText(String p_96587_, boolean p_96588_) {}

    @Shadow
    @Nullable
    protected Minecraft minecraft;

    @Shadow
    @Final
    private static Set<String> ALLOWED_PROTOCOLS;

    @Shadow
    @Nullable
    private URI clickedLink;

    @Shadow
    private void confirmLink(boolean p_96623_) {}

    @Shadow
    private void openLink(URI p_96590_) {}

    @Shadow
    @Final
    private static Logger LOGGER;

    /**
     * @author denisnumb
     * @reason for implement custom click event
     */
    @Overwrite
    public boolean handleComponentClicked(@Nullable Style p_96592_) {
        if (p_96592_ != null) {
            ClickEvent clickevent = p_96592_.getClickEvent();
            if (hasShiftDown()) {
                if (p_96592_.getInsertion() != null) {
                    this.insertText(p_96592_.getInsertion(), false);
                }
            } else if (clickevent != null) {
                if (clickevent.getAction() == ClickEvent.Action.OPEN_URL) {
                    if (!this.minecraft.options.chatLinks().get()) {
                        return false;
                    }

                    try {
                        URI uri = new URI(clickevent.getValue());
                        String s = uri.getScheme();
                        if (s == null) {
                            throw new URISyntaxException(clickevent.getValue(), "Missing protocol");
                        }

                        if (!ALLOWED_PROTOCOLS.contains(s.toLowerCase(Locale.ROOT))) {
                            throw new URISyntaxException(clickevent.getValue(), "Unsupported protocol: " + s.toLowerCase(Locale.ROOT));
                        }

                        if (this.minecraft.options.chatLinksPrompt().get()) {
                            this.clickedLink = uri;
                            this.minecraft.setScreen(new ConfirmLinkScreen(this::confirmLink, clickevent.getValue(), false));
                        } else {
                            this.openLink(uri);
                        }
                    } catch (URISyntaxException urisyntaxexception) {
                        LOGGER.error("Can't open url for {}", clickevent, urisyntaxexception);
                    }
                } else if (clickevent.getAction() == ClickEvent.Action.OPEN_FILE) {
                    URI uri1 = (new File(clickevent.getValue())).toURI();
                    this.openLink(uri1);
                } else if (clickevent.getAction() == ClickEvent.Action.SUGGEST_COMMAND) {
                    this.insertText(SharedConstants.filterText(clickevent.getValue()), true);
                } else if (clickevent.getAction() == ClickEvent.Action.RUN_COMMAND) {
                    String s1 = SharedConstants.filterText(clickevent.getValue());
                    if (s1.startsWith("/")) {
                        if (!this.minecraft.player.connection.sendUnsignedCommand(s1.substring(1))) {
                            LOGGER.error("Not allowed to run command with signed argument from click event: '{}'", s1);
                        }
                    } else {
                        if (s1.startsWith("send_screenshot")) {
                            discord_minecraft_chat$sendScreenshot(s1.replace("send_screenshot ", ""));
                        } else
                            LOGGER.error("Failed to run command without '/' prefix from click event: '{}'", s1);
                    }
                } else if (clickevent.getAction() == ClickEvent.Action.COPY_TO_CLIPBOARD) {
                    this.minecraft.keyboardHandler.setClipboard(clickevent.getValue());
                } else {
                    LOGGER.error("Don't know how to handle {}", clickevent);
                }

                return true;
            }

        }
        return false;
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
