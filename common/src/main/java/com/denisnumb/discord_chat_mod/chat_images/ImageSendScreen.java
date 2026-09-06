package com.denisnumb.discord_chat_mod.chat_images;

import com.denisnumb.discord_chat_mod.chat_images.model.AbstractImage;
import com.denisnumb.discord_chat_mod.chat_images.model.AnimatedImage;
import com.denisnumb.discord_chat_mod.chat_images.model.Image;
import com.denisnumb.discord_chat_mod.chat_images.widgets.FlowButtonLayout;
import com.denisnumb.discord_chat_mod.chat_images.widgets.FlowButtonLayout.ButtonSlot;
import com.denisnumb.discord_chat_mod.chat_images.utils.ImageUtils;
import com.denisnumb.discord_chat_mod.chat_images.widgets.PlayerSelectionPopup;
import com.denisnumb.discord_chat_mod.locale.MinecraftLocaleProvider;
import com.denisnumb.discord_chat_mod.network.image.ImageTransceiver;
import com.denisnumb.discord_chat_mod.network.image.model.ImagePartPacketPayload;
import com.denisnumb.discord_chat_mod.network.image.model.SendTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ImageSendScreen extends Screen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 8;
    private static final int SCREEN_MARGIN = 10;
    private static final int MIN_BUTTON_WIDTH = 70;
    private static final int NATURAL_BUTTON_WIDTH = 150;
    private static final int ROW_SPACING = 6;

    private final AbstractImage image;
    private final Screen prevScreen;
    private final byte[] imageBytes;
    private final String imageMimeType;
    private final String defaultImageDisplayName;
    private final Player localPlayer;

    private final int imageWidth;
    private final int imageHeight;

    private final List<Button> mainButtons = new ArrayList<>();
    private EditBox imageNameBox;
    private Checkbox spoilerCheckbox;

    private PlayerSelectionPopup playerSelectionPopup;

    public ImageSendScreen(
            AbstractImage image,
            byte[] imageBytes,
            String imageMimeType,
            String imageDisplayName,
            Screen prevScreen,
            Player localPlayer
    ) {
        super(Component.literal("Send Image"));
        this.image = image;
        this.imageBytes = imageBytes;
        this.imageMimeType = imageMimeType;
        this.defaultImageDisplayName = imageDisplayName;
        this.prevScreen = prevScreen;
        this.localPlayer = localPlayer;
        this.imageWidth = image.originalSize.width();
        this.imageHeight = image.originalSize.height();
    }

    @Override
    protected void init() {
        clearWidgets();
        mainButtons.clear();

        playerSelectionPopup = new PlayerSelectionPopup(
                Minecraft.getInstance(),
                this::addRenderableWidget,
                this::removeWidget,
                this::sendToPlayers
        );

        List<PlayerInfo> otherPlayers = getOtherPlayers();
        List<ActionDef> actionDefs = getActionDefs(otherPlayers);

        List<ButtonSlot> slots = FlowButtonLayout.compute(
                actionDefs.size(),
                this.width,
                SCREEN_MARGIN,
                this.height - 30,
                MIN_BUTTON_WIDTH, NATURAL_BUTTON_WIDTH, BUTTON_HEIGHT,
                BUTTON_SPACING, ROW_SPACING
        );

        for (int i = 0; i < actionDefs.size(); i++) {
            ActionDef def = actionDefs.get(i);
            ButtonSlot slot = slots.get(i);

            Button btn = Button.builder(def.label(), def.onPress())
                    .size(slot.width(), slot.height())
                    .build();
            btn.setPosition(slot.x(), slot.y());
            addRenderableWidget(btn);
            mainButtons.add(btn);
        }

        int topRowY = slots.isEmpty() ? this.height - 30 : slots.get(0).y();
        int imageNameBoxWidth = Math.min(180, Math.max(90, this.width / 5));
        int aboveButtonsY = topRowY - 26;

        spoilerCheckbox = Checkbox.builder(
                MinecraftLocaleProvider.SendImage.Screen.sendAsSpoiler(),
                this.font
        ).pos((this.width - imageNameBoxWidth) / 2 + imageNameBoxWidth / 2, aboveButtonsY).build();
        addRenderableWidget(spoilerCheckbox);

        imageNameBox = new EditBox(this.font, 0, 0, imageNameBoxWidth, 20, Component.literal(""));
        imageNameBox.setHint(MinecraftLocaleProvider.SendImage.Screen.imageDisplayName());
        imageNameBox.setMaxLength(64);
        imageNameBox.setPosition(spoilerCheckbox.getX() - imageNameBoxWidth - BUTTON_SPACING, aboveButtonsY);
        imageNameBox.setValue(defaultImageDisplayName);
        addRenderableWidget(imageNameBox);
    }

    private record ActionDef(Component label, Button.OnPress onPress) {}

    private @NotNull List<ActionDef> getActionDefs(List<PlayerInfo> otherPlayers) {
        PlayerTeam team = localPlayer.getTeam();

        List<ActionDef> defs = new ArrayList<>();
        defs.add(new ActionDef(MinecraftLocaleProvider.SendImage.Screen.sendPublic(), btn -> sendPublic()));

        if (!otherPlayers.isEmpty()) {
            defs.add(new ActionDef(MinecraftLocaleProvider.SendImage.Screen.sendToPlayers(), btn -> {
                setMainControlsVisible(false);
                playerSelectionPopup.show(this.width, this.height, getOtherPlayers());
            }));
        }

        if (team != null) {
            String teamName = team.getDisplayName().getString();
            defs.add(new ActionDef(MinecraftLocaleProvider.SendImage.Screen.sendToTeam(teamName), btn -> sendToTeam(team)));
        }
        defs.add(new ActionDef(MinecraftLocaleProvider.cancel(), btn -> cancelAndRestorePreviousScreen()));

        return defs;
    }

    private void setMainControlsVisible(boolean visible) {
        for (Button b : mainButtons) {
            b.visible = visible;
            b.active = visible;
        }
        if (imageNameBox != null) {
            imageNameBox.visible = visible;
            imageNameBox.setEditable(visible);
        }
        if (spoilerCheckbox != null) {
            spoilerCheckbox.visible = visible;
            spoilerCheckbox.active = visible;
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(0, 0, this.width, this.height, 0x88000000);
        renderImage(graphics);
        playerSelectionPopup.render(graphics, this.font, this.width);
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderImage(GuiGraphics graphics) {
        int maxWidth = (int) (this.width * 0.7);
        int maxHeight = (int) (this.height * 0.6);
        double scale = Math.min(1.0, Math.min((double) maxWidth / imageWidth, (double) maxHeight / imageHeight));

        int renderWidth = (int) (imageWidth * scale);
        int renderHeight = (int) (imageHeight * scale);
        int centerX = (this.width - renderWidth) / 2;
        int centerY = ((this.height - 60) - renderHeight) / 2;

        Identifier resourceLocation = image instanceof AnimatedImage gif
                ? gif.getCurrentFrame()
                : ((Image) image).resourceLocation;

        graphics.blit(RenderPipelines.GUI_TEXTURED, resourceLocation,
                centerX, centerY, 0, 0,
                renderWidth, renderHeight,
                renderWidth, renderHeight
        );
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean bl) {
        if (playerSelectionPopup.handleOutsideClick(event.x(), event.y())) {
            setMainControlsVisible(true);
            return true;
        }
        return super.mouseClicked(event, bl);
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (event.key() == 256) {
            if (playerSelectionPopup.isVisible()) {
                playerSelectionPopup.hide();
                setMainControlsVisible(true);
            } else {
                cancelAndRestorePreviousScreen();
            }
            return true;
        }
        return super.keyPressed(event);
    }

    private List<PlayerInfo> getOtherPlayers() {
        if (Minecraft.getInstance().getConnection() == null)
            return List.of();

        return Minecraft.getInstance().getConnection().getOnlinePlayers()
                .stream()
                .filter(p -> !p.getProfile().name().equals(localPlayer.getName().getString()))
                .sorted((a, b) -> a.getProfile().name().compareToIgnoreCase(b.getProfile().name()))
                .toList();
    }

    private ImagePartPacketPayload buildPayload(SendTarget sendTarget) {
        String fileName = System.currentTimeMillis() + ImageUtils.mimeTypeToFileExt(imageMimeType);
        if (spoilerCheckbox.selected())
            fileName = ImageUtils.SPOILER_PREFIX + fileName;

        String displayName = imageNameBox.getValue();
        if (displayName.isBlank())
            displayName = MinecraftLocaleProvider.image().getString();

        return new ImagePartPacketPayload(null, fileName, imageMimeType, displayName, sendTarget, imageBytes);
    }

    private void sendPublic() {
        ImageTransceiver.sendImage(localPlayer, buildPayload(SendTarget.all()));
        closeAfterSending();
    }

    private void sendToTeam(PlayerTeam team) {
        ImageTransceiver.sendImage(localPlayer, buildPayload(SendTarget.team(team.getName())));
        closeAfterSending();
    }

    private void sendToPlayers() {
        List<String> selected = playerSelectionPopup.getSelectedPlayers();
        if (selected.isEmpty())
            return;
        ImageTransceiver.sendImage(localPlayer, buildPayload(SendTarget.players(selected)));
        closeAfterSending();
    }

    private void closeAfterSending() {
        ImageStorage.evictImage(image.url);
        Minecraft.getInstance().setScreen(null);
    }

    private void cancelAndRestorePreviousScreen() {
        ImageStorage.evictImage(image.url);
        Minecraft.getInstance().setScreen(prevScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}