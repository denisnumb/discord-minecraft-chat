package com.denisnumb.discord_chat_mod.mixin;

import com.denisnumb.discord_chat_mod.chat_images.model.*;
import com.google.common.collect.Lists;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

import static com.denisnumb.discord_chat_mod.MinecraftUtils.getTranslateClient;
import static com.denisnumb.discord_chat_mod.ModLanguageKey.CLICK_TO_OPEN_IMAGE;
import static com.denisnumb.discord_chat_mod.chat_images.ImageStorage.*;
import static com.denisnumb.discord_chat_mod.chat_images.ImageUtils.getCurrentFrameIndex;


@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    @Shadow private int chatScrollbarPos;
    @Shadow public abstract int getWidth();
    @Shadow protected abstract int getLineHeight();
    @Shadow protected abstract boolean isChatFocused();
    @Shadow public abstract double getScale();
    @Shadow @Final private List<GuiMessage> allMessages;
    @Shadow public abstract int getLinesPerPage();

    @Unique
    private static final int MAX_MESSAGES = 500;

    // get all message click event urls
    @Unique
    private List<String> discord_minecraft_chat$getComponentUrls(Component component){
        return component.toFlatList().stream().filter(comp -> {
            ClickEvent clickEvent = comp.getStyle().getClickEvent();
            return clickEvent != null && clickEvent.getAction() == ClickEvent.Action.OPEN_URL;
        }).map(comp -> comp.getStyle().getClickEvent().getValue()).toList();
    }

    // get count of empty chat lines for display message
    @Unique
    private int discord_minecraft_chat$getImageLinesCount(int imageHeight){
        return Mth.ceil((float) imageHeight / getLineHeight()) + 1;
    }

    // get guiMessage index for ChatComponent.allMessages by index of ChatComponent.trimmedMessages
    @Unique
    private int discord_minecraft_chat$getGuiMessageIndexByTrimmedMessageIndex(int targetIndex) {
        int messageIndex = -1;

        for (int i = 0; i <= targetIndex; i++)
            if (trimmedMessages.get(i).endOfEntry())
                messageIndex++;

        return messageIndex;
    }

    @ModifyConstant(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V",
            constant = @Constant(intValue = 100)
    )
    private int modifyAddMessageMessageLimit(int original){
        return MAX_MESSAGES;
    }

    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(ILjava/lang/Object;)V",
                    ordinal = 0,
                    shift = At.Shift.BY,
                    by = 2
            )
    )
    private void removeOldFromTrimmedMessages(Component message, MessageSignature signature, int addedTime, GuiMessageTag tag, boolean onlyTrimmed, CallbackInfo ci) {
        while(trimmedMessages.size() > MAX_MESSAGES) {
            int parentAddedTime = trimmedMessages.get(trimmedMessages.size() - 1).addedTime();

            do trimmedMessages.remove(trimmedMessages.size() - 1);
            while (trimmedMessages.get(trimmedMessages.size() - 1).addedTime() == parentAddedTime);
        }
    }

    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(ILjava/lang/Object;)V",
                    ordinal = 1,
                    shift = At.Shift.AFTER
            )
    )
    private void removeOldFromAllMessages(Component message, MessageSignature signature, int addedTime, GuiMessageTag tag, boolean onlyTrimmed, CallbackInfo ci) {
        while(allMessages.size() > MAX_MESSAGES) {
            int parentAddedTime = allMessages.get(allMessages.size() - 1).addedTime();

            do allMessages.remove(allMessages.size() - 1);
            while (allMessages.get(allMessages.size() - 1).addedTime() == parentAddedTime);
        }
    }

    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V",
            at = @At(value = "TAIL")
    )
    private void addMessageWithImage(Component message, MessageSignature signature, int addedTime, GuiMessageTag tag, boolean onlyTrimmed, CallbackInfo ci){
        List<String> componentUrls = discord_minecraft_chat$getComponentUrls(message);
        if (componentUrls.isEmpty())
            return;

        loadImagesParallel(componentUrls, () -> trimmedMessages.size(), () -> allMessages.size()).thenAccept(loadResult -> {
            int oldTrimmedSize = loadResult.trimmedMessagesSize();
            int oldAllSize = loadResult.allMessagesSize();
            int trimmedIndex = trimmedMessages.size() > oldTrimmedSize ? trimmedMessages.size() - oldTrimmedSize : 0;
            int allIndex = allMessages.size() > oldAllSize ? allMessages.size() - oldAllSize : 0;

            for (AbstractImage image : Lists.reverse(loadResult.images())){
                if (image == null)
                    continue;

                int linesCount = discord_minecraft_chat$getImageLinesCount(image.imageSize.height());
                Component imageComponent = Component.literal(" ".repeat(image.imageSize.width() / minecraft.font.width(" ")))
                        .withStyle(style ->
                                style.withHoverEvent(new HoverEvent(
                                                HoverEvent.Action.SHOW_TEXT,
                                                Component.literal(getTranslateClient(CLICK_TO_OPEN_IMAGE, "Click to open image")))
                                        )
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "open_image " + image.url))
                        );
                FormattedCharSequence imageCharSequence = imageComponent.getVisualOrderText();

                for (int i = 0; i < linesCount; i++)
                    trimmedMessages.add(trimmedIndex, new GuiMessage.Line(addedTime, imageCharSequence, tag, true));
                trimmedIndex += linesCount;

                if (!onlyTrimmed) {
                    for (int i = 0; i < linesCount; i++)
                        allMessages.add(allIndex, new GuiMessage(addedTime, imageComponent, signature, tag));
                    allIndex += linesCount;
                }
            }
        });
    }


    @Inject(method = "render", at = @At("TAIL"))
    private void render(GuiGraphics graphics, int currentTime, int mouseX, int mouseY, CallbackInfo ci){
        Map<Integer, List<String>> allChatUrls = new HashMap<>();
        for (int i = 0; i < allMessages.size(); ++i) {
            GuiMessage guiMessage = allMessages.get(i);
            List<String> urls = discord_minecraft_chat$getComponentUrls(guiMessage.content());
            if (!urls.isEmpty())
                allChatUrls.put(i, urls);
        }

        if (allChatUrls.isEmpty())
            return;

        int chatBottomY = Mth.floor((float) (graphics.guiHeight() - 40) / (float) this.getScale());
        int lineHeight = getLineHeight();
        int chatTopY = chatBottomY - lineHeight * getLinesPerPage();
        boolean isChatFocused = isChatFocused();

        Map<Integer, Integer> messagesY = new HashMap<>();
        for (int i = 0; i + chatScrollbarPos < trimmedMessages.size(); ++i) {
            int messageIndex = i + chatScrollbarPos;
            int guiMessageIndex = discord_minecraft_chat$getGuiMessageIndexByTrimmedMessageIndex(messageIndex);
            int messageY = chatBottomY - i * lineHeight;

            if (trimmedMessages.get(messageIndex).endOfEntry())
                messagesY.put(guiMessageIndex, messageY);
        }

        graphics.pose().pushPose();
        graphics.pose().translate(4.0F, 0.0F, 50.0F);

        for (Map.Entry<Integer, List<String>> entry : allChatUrls.entrySet()) {
            int messageIndex = entry.getKey();
            GuiMessage guiMessage = allMessages.get(messageIndex);
            List<String> urls = entry.getValue();

            if (!messagesY.containsKey(messageIndex))
                continue;
            if (currentTime - guiMessage.addedTime() >= 200 && !isChatFocused)
                continue;

            int messageY = messagesY.get(messageIndex);
            int offset = 0;

            for (String imageUrl : urls) {
                if (!IMAGE_CACHE.containsKey(imageUrl))
                    continue;

                AbstractImage abstractImage = IMAGE_CACHE.get(imageUrl);
                ImageSize imageSize = abstractImage.imageSize;
                ResourceLocation resourceLocation = abstractImage instanceof AnimatedImage gif
                        ? gif.frames.get(getCurrentFrameIndex(gif))
                        : ((Image) abstractImage).resourceLocation;

                int imageWidth = imageSize.width();
                int imageHeight = imageSize.height();
                int visibleHeight = imageHeight;

                int startY = messageY + offset + (lineHeight / 2);
                int startV = 0;
                if (startY < chatTopY){
                    startV = Math.abs(chatTopY - startY);
                    visibleHeight -= startV;
                    startY = chatTopY;
                }

                int endY = startY + imageHeight;
                if (endY > chatBottomY)
                    visibleHeight -= Math.abs(endY - chatBottomY);

                graphics.blit(resourceLocation,
                        0, startY,
                        0, startV,
                        imageWidth, visibleHeight,
                        imageWidth, imageHeight
                );

                offset += imageHeight + lineHeight;
            }
        }

        graphics.pose().popPose();
    }
}
