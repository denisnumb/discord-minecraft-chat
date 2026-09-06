package com.denisnumb.discord_chat_mod.config;

public final class ConfigProvider {
    private ConfigProvider() {}

    private static IConfigProvider config;

    public static void setConfigProvider(IConfigProvider cfg) {
        config = cfg;
    }

    public static IConfigProvider getConfig() {
        return config;
    }
}
