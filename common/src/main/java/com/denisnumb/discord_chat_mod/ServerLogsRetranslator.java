package com.denisnumb.discord_chat_mod;

import com.denisnumb.discord_chat_mod.discord.DiscordChannelRegistry;
import com.denisnumb.discord_chat_mod.discord.model.DiscordGuildContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.isDiscordConnected;

public final class ServerLogsRetranslator {
    private ServerLogsRetranslator() {}

    private static boolean DO_LOGGING = false;

    public static void init(String loggingLevel, String logPattern) {
        DiscordLogBuffer.init();
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();

        PatternLayout layout = PatternLayout.newBuilder()
                .withPattern(logPattern)
                .build();

        Appender appender = new AbstractAppender("DiscordAppender", null, layout, false, null) {
            @Override
            public void append(LogEvent event) {
                if (DO_LOGGING)
                    DiscordLogBuffer.add(new String(getLayout().toByteArray(event)));
            }
        };

        Level level = switch (loggingLevel){
            case "ERROR" -> Level.ERROR;
            case "WARN" -> Level.WARN;
            default -> Level.INFO;
        };

        appender.start();
        config.getRootLogger().addAppender(appender, level, null);
        ctx.updateLoggers();
    }

    private static void sendLogsToDiscord(String logs) {
        if (logs == null || logs.isEmpty())
            return;

        if (logs.length() > 2000)
            logs = logs.substring(0, 1990) + ". . .";

        if (isDiscordConnected()){
            String finalLogs = logs;
            DiscordChannelRegistry.getAllContexts().stream()
                    .map(DiscordGuildContext::getServerLogsChannel)
                    .filter(Objects::nonNull)
                    .forEach(logsChannel -> {
                        try {
                            logsChannel.sendMessage(String.format("```cmd\n%s\n```", finalLogs)).queue();
                        } catch (Exception ignored) {}
                    });
        }
    }

    public static void start(){
        DO_LOGGING = true;
    }

    public static void stop(){
        DiscordLogBuffer.shutdown();
        DO_LOGGING = false;
    }

    static class DiscordLogBuffer {
        private static final int MAX_LENGTH = 2000;
        private static final long MAX_DELAY_MS = 10000;
        private static final long MIN_SEND_INTERVAL_MS = 2000;
        private static final StringBuilder buffer = new StringBuilder();
        private static final Object lock = new Object();
        private static Instant lastSend = Instant.now();
        private static ScheduledExecutorService scheduler;

        public static void init(){
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ServerLogsRetranslatorThread");
                t.setDaemon(true);
                return t;
            });
            scheduler.scheduleAtFixedRate(DiscordLogBuffer::checkAndFlush, 2, 2, TimeUnit.SECONDS);
        }

        public static void add(String message) {
            synchronized (lock) {
                buffer.append(message).append("\n");

                long elapsed = Instant.now().toEpochMilli() - lastSend.toEpochMilli();

                if (elapsed < MIN_SEND_INTERVAL_MS)
                    return;

                if (buffer.length() >= MAX_LENGTH)
                    flushLimited();
            }
        }

        private static void checkAndFlush() {
            synchronized (lock) {
                if (buffer.isEmpty())
                    return;

                long elapsed = Instant.now().toEpochMilli() - lastSend.toEpochMilli();

                if (elapsed > MAX_DELAY_MS) {
                    flushFull();
                    return;
                }

                if (elapsed >= MIN_SEND_INTERVAL_MS) {
                    if (buffer.length() > MAX_LENGTH)
                        flushLimited();
                    else
                        flushFull();
                }
            }
        }

        private static void flushLimited() {
            String content = buffer.substring(0, MAX_LENGTH);
            buffer.delete(0, MAX_LENGTH);

            lastSend = Instant.now();
            CompletableFuture.runAsync(() -> sendLogsToDiscord(content));
        }

        private static void flushFull() {
            if (buffer.isEmpty())
                return;

            String content = buffer.toString();
            buffer.setLength(0);
            lastSend = Instant.now();

            CompletableFuture.runAsync(() -> sendLogsToDiscord(content));
        }

        public static void shutdown() {
            if (scheduler == null)
                return;
            try {
                flushFull();
                scheduler.shutdown();
                if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
    }
}
