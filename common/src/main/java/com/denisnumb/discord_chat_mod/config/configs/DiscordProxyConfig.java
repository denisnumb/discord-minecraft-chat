package com.denisnumb.discord_chat_mod.config.configs;

import com.electronwill.nightconfig.core.CommentedConfig;

import static com.denisnumb.discord_chat_mod.config.ConfigComments.PROXY_HOSTNAME_COMMENT;
import static com.denisnumb.discord_chat_mod.config.ConfigComments.PROXY_USER_COMMENT;
import static com.denisnumb.discord_chat_mod.config.ConfigDefaults.*;
import static com.denisnumb.discord_chat_mod.config.ConfigDefaults.PROXY_PASSWORD_DEFAULT;

public final class DiscordProxyConfig {
    private DiscordProxyConfig() {}

    public static String proxyHostname;
    public static int proxyPort;
    public static String proxyUser;
    public static String proxyPassword;

    public static CommentedConfig loadDiscordProxyConfig(CommentedConfig commonConfig){
        CommentedConfig existedDiscordProxyConfig = commonConfig.getOrElse("discordProxyConfig", commonConfig.createSubConfig());
        CommentedConfig discordProxyConfig = commonConfig.createSubConfig();

        proxyHostname = existedDiscordProxyConfig.getOrElse("proxyHostname", PROXY_HOSTNAME_DEFAULT);
        discordProxyConfig.set("proxyHostname", proxyHostname);
        discordProxyConfig.setComment("proxyHostname", PROXY_HOSTNAME_COMMENT);

        proxyPort = existedDiscordProxyConfig.getOrElse("proxyPort", PROXY_PORT_DEFAULT);
        if (proxyPort < 0 || proxyPort > 65535)
            proxyPort = PROXY_PORT_DEFAULT;
        discordProxyConfig.set("proxyPort", proxyPort);
        discordProxyConfig.setComment("proxyPort", String.format(" Default: %d\n Range: 0 ~ 65535", PROXY_PORT_DEFAULT));

        proxyUser = existedDiscordProxyConfig.getOrElse("proxyUser", PROXY_USER_DEFAULT);
        discordProxyConfig.set("proxyUser", proxyUser);
        discordProxyConfig.setComment("proxyUser", PROXY_USER_COMMENT);

        proxyPassword = existedDiscordProxyConfig.getOrElse("proxyPassword", PROXY_PASSWORD_DEFAULT);
        discordProxyConfig.set("proxyPassword", proxyPassword);

        return discordProxyConfig;
    }
}
