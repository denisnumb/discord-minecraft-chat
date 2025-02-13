package com.denisnumb.discord_chat_mod.chat_images.model;

import java.util.List;

public record LoadResult(int trimmedMessagesSize, int allMessagesSize, List<AbstractImage> images) {
}
