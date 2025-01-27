package com.denisnumb.discord_chat_mod.markdown.tellraw;

import com.google.gson.annotations.SerializedName;

public record TellRawComponentEvent(
        @SerializedName("action")
        String action,
        @SerializedName("value")
        String value
) {}
