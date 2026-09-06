package com.denisnumb.discord_chat_mod.mixin;

import com.denisnumb.discord_chat_mod.utils.ColorUtils;
import com.denisnumb.discord_chat_mod.MinecraftClientEvents;
import com.denisnumb.discord_chat_mod.chat_images.model.*;
import com.denisnumb.discord_chat_mod.config.ConfigProvider;
import com.denisnumb.discord_chat_mod.discord.utils.EmbedToComponentConverter;
import com.denisnumb.discord_chat_mod.locale.MinecraftLocaleProvider;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.denisnumb.discord_chat_mod.chat_images.ImageStorage.*;


@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    @Shadow private int chatScrollbarPos;
    @Shadow protected abstract int getLineHeight();
    @Shadow public abstract boolean isChatFocused();
    @Shadow protected abstract double getScale();
    @Shadow @Final private List<GuiMessage> allMessages;
    @Shadow public abstract int getLinesPerPage();

    @Unique private final String IMAGE_TAG_LABEL = "discord_chat_mod$IMAGE";
    @Unique private final String EMBEDDED_IMAGE_TAG_LABEL = IMAGE_TAG_LABEL + "_EMBED";

    @ModifyArg(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/GuiMessage;<init>(ILnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V"
            ),
            index = 1,
            require = 0
    )
    private Component handleChatMessageClient(Component component) {
        return MinecraftClientEvents.handleChatMessage(component);
    }

    @Unique
    private List<String> discord_minecraft_chat$getComponentUrls(Component component){
        return component.toFlatList().stream().filter(comp -> {
            ClickEvent clickEvent = comp.getStyle().getClickEvent();
            return clickEvent != null && clickEvent.action() == ClickEvent.Action.OPEN_URL;
        }).filter(comp -> comp.getStyle().getClickEvent() instanceof ClickEvent.OpenUrl)
                .map(comp -> ((ClickEvent.OpenUrl)comp.getStyle().getClickEvent()).uri().toString())
                .distinct()
                .toList();
    }

    @Unique
    private int discord_minecraft_chat$getImageLinesCount(int imageHeight){
        return Mth.ceil((float) imageHeight / getLineHeight()) + 1;
    }

    @Unique
    private int discord_minecraft_chat$getGuiMessageIndexByTrimmedMessageIndex(int targetIndex) {
        int messageIndex = -1;

        for (int i = 0; i <= targetIndex; i++)
            if (trimmedMessages.get(i).endOfEntry())
                messageIndex++;

        return messageIndex;
    }

    @Unique
    private boolean discord_minecraft_chat$isImageTag(@Nullable GuiMessageTag tag){
        if (tag != null && tag.logTag() != null)
            return tag.logTag().startsWith(IMAGE_TAG_LABEL);

        return false;
    }

    @Unique
    private boolean discord_minecraft_chat$isImageEmbedded(@Nullable GuiMessageTag tag){
        if (tag != null && tag.logTag() != null)
            return tag.logTag().equals(EMBEDDED_IMAGE_TAG_LABEL);

        return false;
    }

    @Unique
    private @Nullable String discord_minecraft_chat$getImageUrlFromPlaceholder(FormattedCharSequence line) {
        String[] result = {null};
        line.accept((index, style, codePoint) -> {
            if (style.getClickEvent() instanceof ClickEvent.RunCommand(String command)
                    && command.startsWith(OPEN_IMAGE_COMMAND))
                result[0] = command.substring(OPEN_IMAGE_COMMAND.length());
            return result[0] == null;
        });

        return result[0];
    }

    @Unique
    private Set<String> discord_minecraft_chat$findImageUrlsInLine(FormattedCharSequence line) {
        Set<String> urls = new LinkedHashSet<>();
        line.accept((index, style, codePoint) -> {
            if (style.getClickEvent() instanceof ClickEvent.OpenUrl(java.net.URI uri))
                urls.add(uri.toString());

            return true;
        });

        return urls;
    }

    @Unique
    private Map<String, GuiMessage.Line> discord_minecraft_chat$buildUrlToLineMap(GuiMessage guimessage) {
        Map<String, GuiMessage.Line> result = new HashMap<>();
        for (GuiMessage.Line line : trimmedMessages) {
            if (line.addedTime() != guimessage.addedTime())
                break;

            for (String url : discord_minecraft_chat$findImageUrlsInLine(line.content()))
                result.putIfAbsent(url, line);
        }

        return result;
    }

    @Unique
    private Component discord_minecraft_chat$createImagePlaceholderComponent(AbstractImage image, GuiMessage.Line line, boolean isEmbed) {
        AtomicInteger embedColor = new AtomicInteger(ColorUtils.Color.DISCORD_DEFAULT_COLOR);
        if (isEmbed){
            line.content().accept((index, style, codePoint) -> {
                if (style != null && style.getColor() != null)
                    embedColor.set(style.getColor().getValue());
                return false;
            });
        }

        MutableComponent base = isEmbed
                ? Component.literal(EmbedToComponentConverter.BORDER_SIDE).withColor(embedColor.get())
                : Component.empty();

        MutableComponent content = Component.literal(" ".repeat(image.imageSize.width() / minecraft.font.width(" ")));

        return base.append(content)
                .withStyle(style -> style
                .withHoverEvent(new HoverEvent.ShowText(MinecraftLocaleProvider.Command.clickToOpenImage()))
                .withClickEvent(new ClickEvent.RunCommand(OPEN_IMAGE_COMMAND + image.url))
        );
    }

    @Unique
    private boolean discord_minecraft_chat$isEmbedMessage(GuiMessage.Line line) {
        AtomicBoolean isEmbed = new AtomicBoolean(false);
        line.content().accept((index, style, codePoint) -> {
            isEmbed.set(codePoint == EmbedToComponentConverter.BORDER_SIDE_CHAR);
            return false;
        });

        return isEmbed.get();
    }

    @Unique
    private GuiMessageTag discord_minecraft_chat$buildImageTag(@Nullable GuiMessageTag parentTag, boolean isEmbed) {
        int color = parentTag != null
                ? parentTag.indicatorColor()
                : ColorUtils.Color.TRANSPARENT_IMAGE_TAG_COLOR;

        Component component = parentTag != null
                ? parentTag.text()
                : null;

        String metadata = isEmbed
                ? EMBEDDED_IMAGE_TAG_LABEL
                : IMAGE_TAG_LABEL;

        return new GuiMessageTag(color, null, component, metadata);
    }

    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("TAIL")
    )
    private void addMessage(Component chatComponent, MessageSignature headerSignature, GuiMessageTag tag, CallbackInfo ci, @Local GuiMessage guimessage){
        List<String> componentUrls = discord_minecraft_chat$getComponentUrls(chatComponent);
        if (componentUrls.isEmpty())
            return;

        GuiMessage.Line bottomLine = trimmedMessages.getFirst();
        Map<String, GuiMessage.Line> urlToLineObject = discord_minecraft_chat$buildUrlToLineMap(guimessage);

        loadImagesParallel(componentUrls).thenAccept(loadedImages -> {
            int allMessagesInsertIndex = allMessages.indexOf(guimessage);
            if (allMessagesInsertIndex == -1)
                return;

            List<Map.Entry<AbstractImage, GuiMessage.Line>> imageLines = new ArrayList<>();
            for (AbstractImage image : loadedImages) {
                if (image == null)
                    continue;
                GuiMessage.Line targetLine = urlToLineObject.getOrDefault(image.url, bottomLine);
                imageLines.add(Map.entry(image, targetLine));
            }

            Map<GuiMessage.Line, Integer> actualLineIndexMap = new HashMap<>();
            for (var entry : imageLines)
                actualLineIndexMap.computeIfAbsent(entry.getValue(), trimmedMessages::indexOf);

            imageLines.sort((a, b) ->
                    Integer.compare(actualLineIndexMap.get(b.getValue()), actualLineIndexMap.get(a.getValue())));

            for (var entry : imageLines) {
                AbstractImage image = entry.getKey();
                GuiMessage.Line targetLine = entry.getValue();
                int insertIndex = actualLineIndexMap.get(targetLine);
                if (insertIndex == -1)
                    continue;

                int linesCount = discord_minecraft_chat$getImageLinesCount(image.imageSize.height());
                boolean isEmbed = discord_minecraft_chat$isEmbedMessage(targetLine);
                Component imageComponent = discord_minecraft_chat$createImagePlaceholderComponent(image, targetLine, isEmbed);
                FormattedCharSequence imageSequence = imageComponent.getVisualOrderText();
                GuiMessageTag imageTag = discord_minecraft_chat$buildImageTag(tag, isEmbed);

                for (int i = 0; i < linesCount; i++)
                    trimmedMessages.add(insertIndex, new GuiMessage.Line(guimessage.addedTime(), imageSequence, i == 0 ? imageTag : tag, true));

                for (int i = 0; i < linesCount; i++)
                    allMessages.add(allMessagesInsertIndex, new GuiMessage(guimessage.addedTime(), imageComponent, headerSignature, i == 0 ? imageTag : tag));
                allMessagesInsertIndex += linesCount;
            }
        });
    }

    @ModifyConstant(method = "addMessageToQueue", constant = @Constant(intValue = 100), require = 0)
    private int modifyAddMessageToQueueMessageLimit(int original) {
        return ConfigProvider.getConfig().maxChatHistory();
    }

    @ModifyConstant(method = "addMessageToDisplayQueue", constant = @Constant(intValue = 100), require = 0)
    private int modifyAddMessageToDisplayQueueMessageLimit(int original) {
        return ConfigProvider.getConfig().maxChatHistory();
    }

    @Inject(method = "addMessageToQueue",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void removeOldFromAllMessages(GuiMessage message, CallbackInfo ci) {
        while(allMessages.size() > ConfigProvider.getConfig().maxChatHistory()) {
            int parentAddedTime = allMessages.getLast().addedTime();

            do allMessages.removeLast();
            while (!allMessages.isEmpty() && allMessages.getLast().addedTime() == parentAddedTime);
        }
    }

    @Inject(method = "addMessageToDisplayQueue",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void removeOldFromTrimmedMessages(GuiMessage message, CallbackInfo ci) {
        while(trimmedMessages.size() > ConfigProvider.getConfig().maxChatHistory()) {
            int parentAddedTime = trimmedMessages.getLast().addedTime();

            do trimmedMessages.removeLast();
            while (!trimmedMessages.isEmpty() && trimmedMessages.getLast().addedTime() == parentAddedTime);
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;IIIZZ)V", at = @At("TAIL"))
    private void render(GuiGraphics graphics, net.minecraft.client.gui.Font font, int tickCount, int mouseX, int mouseY, boolean focused, boolean focusedB, CallbackInfo ci){
        if (IMAGE_CACHE.isEmpty())
            return;

        float chatScale = (float) getScale();
        int chatBottomY = Mth.floor((graphics.guiHeight() - 40) / chatScale);
        int lineHeight = getLineHeight();
        int chatTopY = chatBottomY - lineHeight * getLinesPerPage();
        int chatTopYWithMargin = chatTopY - Mth.ceil(MAX_HEIGHT);
        boolean isChatFocused = isChatFocused();

        graphics.pose().pushMatrix();
        graphics.pose().translate(4.0F, 0.0F);
        graphics.pose().scale(chatScale, chatScale);

        for (int i = 0; i + chatScrollbarPos < trimmedMessages.size(); ++i) {
            int messageY = chatBottomY - i * lineHeight;
            if (messageY < chatTopYWithMargin)
                break;

            GuiMessage.Line line = trimmedMessages.get(i + chatScrollbarPos);
            if (tickCount - line.addedTime() >= 200 && !isChatFocused)
                continue;
            if (!discord_minecraft_chat$isImageTag(line.tag()))
                continue;

            String imageUrl = discord_minecraft_chat$getImageUrlFromPlaceholder(line.content());
            AbstractImage abstractImage = imageUrl == null ? null : IMAGE_CACHE.get(imageUrl);
            if (abstractImage == null)
                continue;

            Identifier resourceLocation = abstractImage.isSpoilerAndNotOpened()
                    ? abstractImage.spoilerIdentifier
                    : abstractImage.getRenderFrame();

            ImageSize imageSize = abstractImage.imageSize;
            int imageWidth = imageSize.width();
            int imageHeight = imageSize.height();
            int visibleHeight = imageHeight;

            int startX = 0;
            if (discord_minecraft_chat$isImageEmbedded(line.tag()))
                startX = minecraft.font.width(EmbedToComponentConverter.BORDER_SIDE_WITH_SPACE);

            int startY = messageY - lineHeight / 2;
            int startV = 0;
            if (startY < chatTopY){
                startV = Math.abs(chatTopY - startY);
                visibleHeight -= startV;
                startY = chatTopY;
            }

            int endY = startY + imageHeight;
            if (endY > chatBottomY)
                visibleHeight -= Math.abs(endY - chatBottomY);

            graphics.blit(RenderPipelines.GUI_TEXTURED, resourceLocation,
                    startX, startY,               // x, y
                    0, startV,                    // u, v
                    imageWidth, visibleHeight,    // size on screen
                    imageWidth, imageHeight       // real image size
            );

            if (abstractImage.isSpoilerAndNotOpened()) {
                Component spoilerText = MinecraftLocaleProvider.spoiler()
                        .setStyle(Style.EMPTY.withBold(true));
                float maxScale = 1.5f;
                float scaleX = (imageWidth / MAX_WIDTH) * maxScale;
                float scaleY = (imageHeight / MAX_HEIGHT) * maxScale;
                float scale = Mth.clamp(Math.min(scaleX, scaleY), 0.5f, maxScale);

                int originalTextWidth = minecraft.font.width(spoilerText);
                int originalTextHeight = minecraft.font.lineHeight;

                int centerX = imageWidth / 2;
                int fullCenterY = messageY - (lineHeight / 2) + imageHeight / 2;

                if (fullCenterY >= chatTopY && fullCenterY <= chatBottomY) {
                    int textX = centerX - (int)(originalTextWidth * scale / 2);
                    int textY = fullCenterY - (int)(originalTextHeight * scale / 2);

                    graphics.pose().pushMatrix();
                    graphics.pose().translate(textX, textY);
                    graphics.pose().scale(scale, scale);

                    graphics.drawString(minecraft.font,
                            spoilerText,
                            0, 0,
                            ARGB.color(0xFFFFFF, -1),
                            true
                    );

                    graphics.pose().popMatrix();
                }
            }
        }

        graphics.pose().popMatrix();
    }
}
