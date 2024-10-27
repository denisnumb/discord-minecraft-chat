package com.denisnumb.discord_chat_mod.markdown;

import com.google.gson.annotations.SerializedName;

public record TellRawTextComponentEvent(
        @SerializedName("action")
        String action,
        @SerializedName("value")
        String value
) {}
