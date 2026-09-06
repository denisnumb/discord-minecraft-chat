package com.denisnumb.discord_chat_mod.chat_images.widgets;

import java.util.ArrayList;
import java.util.List;

public final class FlowButtonLayout {
    private FlowButtonLayout() {}

    public record ButtonSlot(int x, int y, int width, int height) {}

    public static List<ButtonSlot> compute(
            int itemCount,
            int screenWidth,
            int horizontalMargin,
            int bottomY,
            int minWidth,
            int naturalWidth,
            int buttonHeight,
            int spacing,
            int rowSpacing
    ) {
        if (itemCount <= 0) {
            return List.of();
        }

        int constrainedWidth = screenWidth - horizontalMargin * 2;

        int maxPerRow = Math.max(1, (constrainedWidth + spacing) / (minWidth + spacing));
        int perRow = Math.min(itemCount, maxPerRow);
        int rows = (int) Math.ceil(itemCount / (double) perRow);

        int availableWidth = constrainedWidth - spacing * (perRow - 1);
        int buttonWidth = Math.min(naturalWidth, Math.max(minWidth, availableWidth / perRow));

        int topY = bottomY - (rows - 1) * (buttonHeight + rowSpacing);

        List<ButtonSlot> slots = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            int row = i / perRow;
            int col = i % perRow;

            int itemsInThisRow = Math.min(perRow, itemCount - row * perRow);
            int rowWidth = itemsInThisRow * buttonWidth + (itemsInThisRow - 1) * spacing;
            int rowStartX = (screenWidth - rowWidth) / 2;

            slots.add(new ButtonSlot(
                    rowStartX + col * (buttonWidth + spacing),
                    topY + row * (buttonHeight + rowSpacing),
                    buttonWidth,
                    buttonHeight
            ));
        }
        return slots;
    }
}