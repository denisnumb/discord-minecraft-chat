package com.denisnumb.discord_chat_mod.discord.utils;

import com.denisnumb.discord_chat_mod.commands.set_avatar.AvatarUrlStorage;
import com.denisnumb.discord_chat_mod.config.IConfigProvider;
import com.denisnumb.discord_chat_mod.config.ConfigProvider;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.properties.Property;
import com.mojang.logging.LogUtils;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static com.denisnumb.discord_chat_mod.chat_images.utils.ImageUtils.isImageUrl;
import static com.denisnumb.discord_chat_mod.chat_images.utils.ImageUtils.getMimeType;

public class WebhookUtils {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static ExecutorService EXECUTOR;

    public record WebhookAttachment(byte[] data, String fileName) { }

    public static class WebhookPayload {
        String username;
        @SerializedName("avatar_url")
        String avatarUrl;
        List<WebhookEmbed> embeds;
        String content;

        private record EmbedImage(String url) {}
        private record EmbedAuthor(String name, String url, String icon_url) {}
        private record EmbedThumbnail(String url) {}
        private record EmbedFooter(String text, String icon_url) {}
        private record EmbedField(String name, String value, Boolean inline) {}

        private record WebhookEmbed(
                String title,
                String description,
                String url,
                int color,
                EmbedAuthor author,
                EmbedThumbnail thumbnail,
                EmbedImage image,
                List<EmbedField> fields,
                EmbedFooter footer,
                String timestamp
        ) {}

        public WebhookPayload(String content){
            this(content, null);
        }

        public WebhookPayload(MessageEmbed embed){
            this(null, embed);
        }

        public WebhookPayload(String content, MessageEmbed embed){
            if (content != null)
                content = content.isEmpty() ? null : content;
            this.content = content;
            if (embed != null){
                int color = embed.getColorRaw() == 536870911 ? 3881793 : embed.getColorRaw();
                EmbedImage image = embed.getImage() == null ? null : new EmbedImage(embed.getImage().getUrl());
                MessageEmbed.AuthorInfo authorInfo = embed.getAuthor();
                EmbedAuthor author = authorInfo == null ? null : new EmbedAuthor(authorInfo.getName(), authorInfo.getUrl(), authorInfo.getIconUrl());
                EmbedThumbnail thumbnail = embed.getThumbnail() == null ? null : new EmbedThumbnail(embed.getThumbnail().getUrl());
                List<EmbedField> embedFields = embed.getFields().stream().map(f -> new EmbedField(f.getName(), f.getValue(), f.isInline())).toList();
                EmbedFooter footer = embed.getFooter() == null ? null : new EmbedFooter(embed.getFooter().getText(), embed.getFooter().getIconUrl());

                this.embeds = List.of(new WebhookEmbed(
                        embed.getTitle(),
                        embed.getDescription(),
                        embed.getUrl(),
                        color,
                        author,
                        thumbnail,
                        image,
                        embedFields,
                        footer,
                        embed.getTimestamp() == null ? null : embed.getTimestamp().format(DateTimeFormatter.ISO_INSTANT)
                ));
            }
        }

        public WebhookPayload setUsername(String username){
            this.username = username;
            return this;
        }

        public WebhookPayload setAvatarUrl(String avatarUrl){
            this.avatarUrl = avatarUrl;
            return this;
        }
    }

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
            WebhookAttachment attachment
    ) {
        return sendWebhookWithFiles(webhookUrl, payload, List.of(attachment), true);
    }

    public static void sendWebhookWithFiles(
            String webhookUrl,
            WebhookPayload payload,
            List<WebhookAttachment> images
    ) {
        if ((payload.content == null && payload.embeds == null) && (images == null || images.isEmpty()))
            return;

        sendWebhookWithFiles(webhookUrl, payload, images, false);
    }

    private static Future<Optional<String>> sendWebhookWithFiles(
            String webhookUrl,
            WebhookPayload payload,
            List<WebhookAttachment> images,
            boolean returnImageUrl
    ) {
        return EXECUTOR.submit(() -> {
            try {
                String boundary = UUID.randomUUID().toString();
                HttpURLConnection connection = getHttpURLConnection(webhookUrl, "multipart/form-data; boundary=" + boundary);

                try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                    writePayloadJson(out, boundary, payload);
                    writeImageParts(out, boundary, images);
                    finishMultipart(out, boundary);
                }

                handleResponseCode(connection);
                Optional<String> imageUrl = returnImageUrl ? getSentImageUrl(connection) : Optional.empty();
                connection.disconnect();

                return imageUrl;
            } catch (Exception e) {
                LOGGER.error("SendWebhookError: " + e.getMessage(), e);
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

    private static void writeImageParts(DataOutputStream out, String boundary, List<WebhookAttachment> images) throws IOException {
        int index = 0;
        for (WebhookAttachment image : images) {
            out.writeBytes("--" + boundary + "\r\n");
            out.writeBytes("Content-Disposition: form-data; name=\"file" + index + "\"; filename=\"" + image.fileName + "\"\r\n");
            out.writeBytes("Content-Type: application/octet-stream\r\n\r\n");
            out.write(image.data);
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

    public static String getPlayerAvatarUrl(Player player){
        IConfigProvider config = ConfigProvider.getConfig();

        if (config.isSetAvatarUrlCommandEnabled()){
            String customAvatarUrl = AvatarUrlStorage.getUrl(player);
            if (customAvatarUrl != null)
                return customAvatarUrl;
        }

        String avatarUrlTemplate = config.webhookPlayerAvatarUrl();
        String defaultAvatarUrl = config.webhookPlayerDefaultAvatarUrl();
        String playerName = player.getName().getString();

        avatarUrlTemplate = avatarUrlTemplate.replace("<name>", playerName);

        if (avatarUrlTemplate.contains("<uuid>")){
            Optional<String> optionalUUID = getUUIDFromMojangAPI(playerName);
            if (optionalUUID.isPresent())
                avatarUrlTemplate = avatarUrlTemplate.replace("<uuid>", optionalUUID.get());
        }

        if (avatarUrlTemplate.contains("<texture>")){
            Optional<String> optionalTexture = getPlayerTextureHash(player);
            if (optionalTexture.isPresent())
                avatarUrlTemplate = avatarUrlTemplate.replace("<texture>", optionalTexture.get());
        }

        if (isImageUrl(getMimeType(avatarUrlTemplate)))
            return avatarUrlTemplate;

        return isImageUrl(getMimeType(defaultAvatarUrl))
                ? defaultAvatarUrl
                : "https://mc-heads.net/avatar/steve_head_png";
    }

    public static Optional<String> getPlayerTextureHash(Player player) {
        try {
            Collection<Property> textures = player.getGameProfile().properties().get("textures");
            if (textures.isEmpty())
                return Optional.empty();

            String encoded = textures.iterator().next().value();
            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(decoded).getAsJsonObject();
            if (!json.has("textures"))
                return Optional.empty();

            JsonObject texturesObject = json.getAsJsonObject("textures");
            if (!texturesObject.has("SKIN"))
                return Optional.empty();

            String skinUrl = texturesObject.getAsJsonObject("SKIN").get("url").getAsString();
            int slashIndex = skinUrl.lastIndexOf('/');
            if (slashIndex < 0 || slashIndex == skinUrl.length() - 1)
                return Optional.empty();

            return Optional.of(skinUrl.substring(slashIndex + 1));
        } catch (Exception ignored) {}

        return Optional.empty();
    }

    public static Optional<String> getUUIDFromMojangAPI(String username) {
        try {
            URL url = new URI("https://api.mojang.com/users/profiles/minecraft/" + username).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);
            in.close();

            JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
            return Optional.of(json.get("id").getAsString());
        } catch (Exception ignored){}

        return Optional.empty();
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
