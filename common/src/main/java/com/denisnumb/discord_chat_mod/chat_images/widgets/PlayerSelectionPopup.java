package com.denisnumb.discord_chat_mod.chat_images.widgets;

import com.denisnumb.discord_chat_mod.locale.MinecraftLocaleProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

import java.util.List;
import java.util.function.Consumer;

public class PlayerSelectionPopup {

    private static final int LIST_VISIBLE_ROWS = 8;
    private static final int ROW_HEIGHT = 22;
    private static final int HEADER_HEIGHT = 52;
    private static final int FOOTER_HEIGHT = 32;
    private static final int SCREEN_VERTICAL_MARGIN = 20;
    private static final int MIN_VISIBLE_ROWS = 2;

    private record Bounds(int x, int y, int width, int height) {
        boolean contains(double px, double py) {
            return px >= x && px <= x + width && py >= y && py <= y + height;
        }
    }

    private final Minecraft minecraft;
    private final Consumer<AbstractWidget> addWidget;
    private final Consumer<GuiEventListener> removeWidget;
    private final Runnable onConfirm;

    private PlayerSelectionList playerList;
    private EditBox searchBox;
    private Button confirmButton;
    private Bounds bounds;
    private boolean visible = false;

    public PlayerSelectionPopup(
            Minecraft minecraft,
            Consumer<AbstractWidget> addWidget,
            Consumer<GuiEventListener> removeWidget,
            Runnable onConfirm
    ) {
        this.minecraft = minecraft;
        this.addWidget = addWidget;
        this.removeWidget = removeWidget;
        this.onConfirm = onConfirm;
    }

    public boolean isVisible() {
        return visible;
    }

    public List<String> getSelectedPlayers() {
        return playerList != null ? playerList.getSelectedPlayers() : List.of();
    }

    public void show(int screenWidth, int screenHeight, List<PlayerInfo> players) {
        hide();

        int listWidth = computeListWidth(screenWidth);

        int maxPopupHeight = Math.max(
                HEADER_HEIGHT + MIN_VISIBLE_ROWS * ROW_HEIGHT + FOOTER_HEIGHT,
                screenHeight - SCREEN_VERTICAL_MARGIN * 2
        );

        int visibleRows = Math.min(LIST_VISIBLE_ROWS, Math.max(players.size(), MIN_VISIBLE_ROWS));
        int listBodyHeight = visibleRows * ROW_HEIGHT;
        int totalHeight = HEADER_HEIGHT + listBodyHeight + FOOTER_HEIGHT;

        while (totalHeight > maxPopupHeight && visibleRows > MIN_VISIBLE_ROWS) {
            visibleRows--;
            listBodyHeight = visibleRows * ROW_HEIGHT;
            totalHeight = HEADER_HEIGHT + listBodyHeight + FOOTER_HEIGHT;
        }

        int popupY = Math.max(SCREEN_VERTICAL_MARGIN, (screenHeight - totalHeight) / 2);
        int popupX = (screenWidth - listWidth) / 2;
        this.bounds = new Bounds(popupX - 2, popupY - 2, listWidth + 4, totalHeight + 4);

        playerList = new PlayerSelectionList(
                minecraft, listWidth, listBodyHeight, popupX, popupY + HEADER_HEIGHT, ROW_HEIGHT,
                players,
                selected -> confirmButton.active = !selected.isEmpty()
        );
        addWidget.accept(playerList);

        searchBox = new EditBox(minecraft.font, popupX + 4, popupY + 28, listWidth - 8, 16, Component.literal(""));
        searchBox.setHint(MinecraftLocaleProvider.SendImage.Screen.playerSearch());
        searchBox.setResponder(text -> playerList.setFilter(text));
        addWidget.accept(searchBox);
        searchBox.setFocused(true);

        confirmButton = Button.builder(MinecraftLocaleProvider.send(), btn -> onConfirm.run())
                .pos(popupX + 4, popupY + totalHeight - FOOTER_HEIGHT + 6)
                .size(listWidth - 8, 20)
                .build();
        confirmButton.active = false;
        addWidget.accept(confirmButton);

        visible = true;
    }

    public void hide() {
        if (!visible) {
            return;
        }
        removeWidget.accept(playerList);
        removeWidget.accept(searchBox);
        removeWidget.accept(confirmButton);
        playerList = null;
        searchBox = null;
        confirmButton = null;
        bounds = null;
        visible = false;
    }

    public boolean handleOutsideClick(double mouseX, double mouseY) {
        if (!visible || bounds.contains(mouseX, mouseY)) {
            return false;
        }
        hide();
        return true;
    }

    public void render(GuiGraphics graphics, Font font, int screenWidth) {
        if (!visible) {
            return;
        }

        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), 0xFF000000);

        int innerX = bounds.x() + 2;
        int innerY = bounds.y() + 2;
        int innerWidth = bounds.width() - 4;

        graphics.fill(innerX, innerY, innerX + innerWidth, bounds.y() + bounds.height() - 2, 0xCC1a1a1a);

        graphics.drawCenteredString(font,
                MinecraftLocaleProvider.SendImage.Screen.selectPlayers(),
                screenWidth / 2, innerY + 9, ARGB.color(0xFFFFFF, -1));

        graphics.fill(innerX + 4, innerY + HEADER_HEIGHT - 2,
                innerX + innerWidth - 4, innerY + HEADER_HEIGHT - 1, 0x55FFFFFF);
    }

    private static int computeListWidth(int screenWidth) {
        return Math.min(220, Math.max(140, screenWidth - 40));
    }
}