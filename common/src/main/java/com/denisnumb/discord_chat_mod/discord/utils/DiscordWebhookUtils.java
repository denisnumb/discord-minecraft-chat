package com.denisnumb.discord_chat_mod.discord.utils;

import com.denisnumb.discord_chat_mod.config.ConfigProvider;
import com.denisnumb.discord_chat_mod.discord.model.WebhookPayload;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public final class DiscordWebhookUtils {
    private DiscordWebhookUtils() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static ExecutorService EXECUTOR;

    public static void initWebhookSendExecutor(){
        EXECUTOR = Executors.newSingleThreadExecutor();
    }

    public static void stopWebhookSendExecutor(){
        if (EXECUTOR != null){
            EXECUTOR.shutdownNow();
            EXECUTOR = null;
        }
    }

    public static void sendWebhook(String webhookUrl, Supplier<WebhookPayload> payloadSupplier) {
        if (EXECUTOR != null)
            EXECUTOR.submit(() -> sendDiscordWebhook(webhookUrl, GSON.toJson(payloadSupplier.get())));
    }

    public static Future<Optional<String>> sendWebhookWithImage(
            String webhookUrl,
            WebhookPayload payload,
            WebhookPayload.WebhookAttachment attachment
    ) {
        return sendWebhookWithFiles(webhookUrl, payload, List.of(attachment), true);
    }

    public static void sendWebhookWithFiles(
            String webhookUrl,
            WebhookPayload payload,
            List<WebhookPayload.WebhookAttachment> attachments
    ) {
        if ((payload.content == null && payload.embeds == null) && (attachments == null || attachments.isEmpty()))
            return;

        sendWebhookWithFiles(webhookUrl, payload, attachments, false);
    }

    private static Future<Optional<String>> sendWebhookWithFiles(
            String webhookUrl,
            WebhookPayload payload,
            List<WebhookPayload.WebhookAttachment> attachments,
            boolean returnImageUrl
    ) {
        return EXECUTOR.submit(() -> {
            try {
                String boundary = UUID.randomUUID().toString();
                HttpURLConnection connection = getHttpURLConnection(webhookUrl, "multipart/form-data; boundary=" + boundary);

                try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                    writePayloadJson(out, boundary, payload);
                    writeAttachmentsParts(out, boundary, attachments);
                    finishMultipart(out, boundary);
                }

                handleResponseCode(connection);
                Optional<String> imageUrl = returnImageUrl ? getSentImageUrl(connection) : Optional.empty();
                connection.disconnect();

                return imageUrl;
            } catch (Exception e) {
                LOGGER.error("SendWebhookError: ", e);
                return Optional.empty();
            }
        });
    }

    private static void writePayloadJson(DataOutputStream out, String boundary, WebhookPayload payload) throws IOException {
        String payloadJson = GSON.toJson(payload);

        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"payload_json\"\r\n");
        out.writeBytes("Content-Type: application/json\r\n\r\n");
        out.write(payloadJson.getBytes(StandardCharsets.UTF_8));
        out.writeBytes("\r\n");
    }

    private static void writeAttachmentsParts(DataOutputStream out, String boundary, List<WebhookPayload.WebhookAttachment> attachments) throws IOException {
        int index = 0;
        for (WebhookPayload.WebhookAttachment attachment : attachments) {
            out.writeBytes("--" + boundary + "\r\n");
            out.writeBytes("Content-Disposition: form-data; name=\"file" + index + "\"; filename=\"" + attachment.fileName() + "\"\r\n");
            out.writeBytes("Content-Type: application/octet-stream\r\n\r\n");
            out.write(attachment.data());
            out.writeBytes("\r\n");
            index++;
        }
    }

    private static void finishMultipart(DataOutputStream out, String boundary) throws IOException {
        out.writeBytes("--" + boundary + "--\r\n");
        out.flush();
    }

    public static String getWebhookServerName(){
        String configValue = ConfigProvider.getConfig().webhookServerName();
        return configValue.isBlank() ? null : configValue.replaceAll("(?i)discord", "DC");
    }

    private static void sendDiscordWebhook(String webhookUrl, String jsonPayload){
        try {
            HttpURLConnection connection = getHttpURLConnection(webhookUrl, "application/json");

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            handleResponseCode(connection);
            connection.disconnect();
        } catch (Exception e) {
            LOGGER.error("SendWebhookError", e);
        }
    }

    private static Optional<String> getSentImageUrl(HttpURLConnection connection){
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            JsonObject responseJson = JsonParser.parseString(response.toString()).getAsJsonObject();
            JsonArray attachments = responseJson.getAsJsonArray("attachments");
            JsonArray embeds = responseJson.getAsJsonArray("embeds");

            if (attachments != null && !attachments.isEmpty()) {
                JsonObject attachment = attachments.get(0).getAsJsonObject();
                return Optional.of(attachment.get("url").getAsString());
            } else if (embeds != null && !embeds.isEmpty()) {
                JsonObject embed = embeds.get(0).getAsJsonObject();
                if (embed.has("image"))
                    return Optional.of(embed.getAsJsonObject("image").get("url").getAsString());
            }
        } catch (Exception ignored){}

        return Optional.empty();
    }

    private static void handleResponseCode(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        if (responseCode != 204 && responseCode != 200) {
            LOGGER.error("Webhook response code: " + responseCode);
            try (InputStream err = connection.getErrorStream()) {
                if (err != null) {
                    String response = new String(err.readAllBytes(), StandardCharsets.UTF_8);
                    LOGGER.error("DiscordWebhookError: " + response);
                }
            }
        }
    }

    private static HttpURLConnection getHttpURLConnection(String webhookUrl, String contentType) throws URISyntaxException, IOException {
        URL url = new URI(webhookUrl).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", contentType);
        connection.setDoOutput(true);

        return connection;
    }
}
