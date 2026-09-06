package com.denisnumb.discord_chat_mod.discord.utils;

import com.denisnumb.discord_chat_mod.utils.EmojiUtils;
import com.denisnumb.discord_chat_mod.config.ConfigProvider;
import com.denisnumb.discord_chat_mod.discord.model.DiscordMentionData;
import com.denisnumb.discord_chat_mod.locale.MinecraftLocaleProvider;
import com.denisnumb.discord_chat_mod.markdown.MarkdownParser;
import com.denisnumb.discord_chat_mod.markdown.MarkdownToComponentConverter;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.PlainTextContents;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static com.denisnumb.discord_chat_mod.utils.ColorUtils.Color.CHAT_LINK_COLOR;
import static com.denisnumb.discord_chat_mod.utils.ColorUtils.Color.DISCORD_DEFAULT_COLOR;

public final class EmbedToComponentConverter {
    private static final int EMBED_LINE_MAX_LENGTH = 45;
    private static final String BORDER_TOP_PREFIX = "┌─── ";
    private static final String BORDER_TOP_PLAIN = "┌───────";
    private static final String BORDER_BOTTOM = "└───";
    private static final String INLINE_FIELD_SEPARATOR = "   ║   ";
    private static final String FOOTER_TIMESTAMP_SEPARATOR = "  •  ";
    public static final String BORDER_SIDE_WITH_SPACE = "│ ";
    public static final String BORDER_SIDE = "│";
    public static final char BORDER_SIDE_CHAR = '│';

    public static Optional<Component> buildEmbedComponent(MessageEmbed embed, Map<String, DiscordMentionData> mentions) {
        if (isEmbedEmpty(embed))
            return Optional.empty();

        int sideColor = embed.getColor() != null
                ? embed.getColorRaw()
                : DISCORD_DEFAULT_COLOR;

        MutableComponent result = Component.empty();
        boolean[] isFirstLine = { true };

        // Title
        if (embed.getTitle() != null) {
            appendTitleLines(result, isFirstLine, sideColor, embed.getTitle(), embed.getUrl(), mentions, EMBED_LINE_MAX_LENGTH);
        } else {
            appendRawLine(result, isFirstLine, () -> Component.literal(BORDER_TOP_PLAIN).withColor(sideColor));
        }

        // Author
        if (embed.getAuthor() != null) {
            MessageEmbed.AuthorInfo author = embed.getAuthor();
            MutableComponent authorComponent = Component.literal("\uD83D\uDC64 " + author.getName())
                    .withColor(ChatFormatting.GRAY.getColor());

            if (author.getUrl() != null) {
                authorComponent = authorComponent.withStyle(style -> style
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(author.getUrl())))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(author.getUrl())))
                );
            }
            appendComponentLines(result, sideColor, authorComponent, EMBED_LINE_MAX_LENGTH);
        }

        List<Runnable> separateEmbedSections = new ArrayList<>();
        // Description
        if (embed.getDescription() != null && !embed.getDescription().isEmpty()) {
            separateEmbedSections.add(() -> appendMarkdownLines(result, sideColor, embed.getDescription(), mentions, EMBED_LINE_MAX_LENGTH));
        }

        // Fields
        List<MessageEmbed.Field> fields = embed.getFields();
        int i = 0;
        while (i < fields.size()) {
            MessageEmbed.Field field = fields.get(i);

            if (field.isInline()) {
                List<MessageEmbed.Field> inlineGroup = new ArrayList<>();
                while (i < fields.size() && fields.get(i).isInline()) {
                    inlineGroup.add(fields.get(i));
                    i++;
                }
                separateEmbedSections.add(() -> appendInlineFieldGroup(result, sideColor, inlineGroup, mentions, EMBED_LINE_MAX_LENGTH));
            } else {
                separateEmbedSections.add(() -> appendFieldLines(result, sideColor, field, mentions, EMBED_LINE_MAX_LENGTH));
                i++;
            }
        }

        // Thumbnail + Image
        if (embed.getThumbnail() != null || embed.getImage() != null) {
            separateEmbedSections.add(() -> {
                if (embed.getThumbnail() != null) {
                    String thumbnailUrl = embed.getThumbnail().getUrl();
                    appendEmbedLine(result, sideColor, () ->
                            buildImageLine(Component.literal("\uD83D\uDDBC ").append(MinecraftLocaleProvider.thumbnail()).append(": "), thumbnailUrl));
                }
                if (embed.getImage() != null) {
                    String imageUrl = embed.getImage().getUrl();
                    appendEmbedLine(result, sideColor, () ->
                            buildImageLine(Component.literal("\uD83D\uDDBC ").append(MinecraftLocaleProvider.image()).append(": "), imageUrl));
                }
            });
        }

        for (int s = 0; s < separateEmbedSections.size(); s++) {
            if (s > 0)
                appendEmptyLine(result, sideColor);
            separateEmbedSections.get(s).run();
        }

        // Footer + Timestamp
        appendFooterLines(result, isFirstLine, sideColor, embed.getFooter(), embed.getTimestamp(), EMBED_LINE_MAX_LENGTH, !separateEmbedSections.isEmpty());

        return Optional.of(result);
    }

    private static void appendEmptyLine(MutableComponent result, int sideColor) {
        appendEmbedLine(result, sideColor, null);
    }

    private static void appendEmbedLine(MutableComponent result, int sideColor, @Nullable Supplier<Component> lineSupplier) {
        result.append(Component.literal("\n"));
        result.append(Component.literal(BORDER_SIDE_WITH_SPACE).withColor(sideColor));
        if (lineSupplier != null)
            result.append(lineSupplier.get());
    }

    private static void appendRawLine(MutableComponent result, boolean[] firstLine, Supplier<Component> lineSupplier) {
        if (!firstLine[0])
            result.append(Component.literal("\n"));

        firstLine[0] = false;
        result.append(lineSupplier.get());
    }

    private static Component convertMarkdown(String text, Map<String, DiscordMentionData> mentions) {
        return new MarkdownToComponentConverter(
                MarkdownParser.parseMarkdown(EmojiUtils.replaceDiscordEmojiMentionsToEmojiNames(text)),
                mentions
        ).convertMarkdownTokensToComponent();
    }

    private record TextRun(String text, Style style) {}

    /**
     * Flattens a Component tree (this node + siblings, recursively) into an ordered list
     * of (text, style) leaves. Each leaf's style is the node's own style merged on top of
     * the style inherited from its parent, matching Minecraft's actual style resolution.
     */
    private static List<TextRun> flatten(Component component) {
        List<TextRun> runs = new ArrayList<>();
        flattenInto(component, Style.EMPTY, runs);
        return runs;
    }

    private static void flattenInto(Component component, Style inheritedStyle, List<TextRun> out) {
        Style effectiveStyle = inheritedStyle.applyTo(component.getStyle());

        if (component.getContents() instanceof PlainTextContents plain) {
            String text = plain.text();
            if (!text.isEmpty())
                out.add(new TextRun(text, effectiveStyle));
        }

        for (Component sibling : component.getSiblings()) {
            flattenInto(sibling, effectiveStyle, out);
        }
    }

    /**
     * Splits text into tokens where each token is either a contiguous run of non-space
     * characters ("word") or a contiguous run of space characters ("gap").
     * <p>
     * Concatenating all tokens reproduces the original string exactly.
     */
    private static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text.isEmpty())
            return tokens;

        StringBuilder current = new StringBuilder();
        boolean currentIsSpace = Character.isWhitespace(text.charAt(0));

        for (char c : text.toCharArray()) {
            boolean isSpace = Character.isWhitespace(c);
            if (isSpace != currentIsSpace) {
                tokens.add(current.toString());
                current = new StringBuilder();
                currentIsSpace = isSpace;
            }
            current.append(c);
        }
        tokens.add(current.toString());
        return tokens;
    }

    /**
     * Wraps an already-styled Component to {@code maxLineLength} characters per visual
     * line, preserving each leaf's style across the wrap.
     * <p>
     * Word boundaries (spaces) are preferred break points; words longer than
     * {@code maxLineLength} are hard-broken mid-word.
     */
    private static List<MutableComponent> wrapStyledComponent(Component component, int maxLineLength) {
        maxLineLength = Math.max(1, maxLineLength);
        List<MutableComponent> lines = new ArrayList<>();
        MutableComponent currentLine = Component.empty();
        int currentLength = 0;

        for (TextRun run : flatten(component)) {
            for (String token : tokenize(run.text())) {
                if (token.isEmpty())
                    continue;

                boolean isSpace = token.isBlank();

                if (currentLength + token.length() <= maxLineLength) {
                    currentLine.append(Component.literal(token).setStyle(run.style()));
                    currentLength += token.length();
                    continue;
                }

                if (isSpace) {
                    lines.add(currentLine);
                    currentLine = Component.empty();
                    currentLength = 0;
                    continue;
                }

                if (currentLength > 0 && token.length() <= maxLineLength) {
                    lines.add(currentLine);
                    currentLine = Component.empty();
                    currentLine.append(Component.literal(token).setStyle(run.style()));
                    currentLength = token.length();
                    continue;
                }

                String remaining = token;
                while (!remaining.isEmpty()) {
                    int available = maxLineLength - currentLength;
                    if (available <= 0) {
                        lines.add(currentLine);
                        currentLine = Component.empty();
                        currentLength = 0;
                        available = maxLineLength;
                    }
                    int take = Math.min(available, remaining.length());
                    currentLine.append(Component.literal(remaining.substring(0, take)).setStyle(run.style()));
                    currentLength += take;
                    remaining = remaining.substring(take);
                }
            }
        }

        lines.add(currentLine);
        return lines;
    }

    private static void appendComponentLines(MutableComponent result, int sideColor, Component component, int maxLineLength) {
        for (MutableComponent line : wrapStyledComponent(component, maxLineLength)) {
            appendEmbedLine(result, sideColor, () -> line);
        }
    }

    private static void appendMarkdownLines(MutableComponent result, int sideColor, String text, Map<String, DiscordMentionData> mentions, int maxLineLength) {
        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                appendEmbedLine(result, sideColor, () -> Component.literal(""));
                continue;
            }

            Component markdownComponent = convertMarkdown(paragraph, mentions);
            appendComponentLines(result, sideColor, markdownComponent, maxLineLength);
        }
    }

    private static void appendTitleLines(MutableComponent result, boolean[] firstLine, int sideColor, String title, @Nullable String url, Map<String, DiscordMentionData> mentions, int maxLineLength) {
        Component titleMarkdown = convertMarkdown(title, mentions);

        int baseColor = url != null ? CHAT_LINK_COLOR : sideColor;
        UnaryOperator<Style> baseStyle = style -> {
            Style styled = style;
            if (styled.getColor() == null)
                styled = styled.withColor(baseColor);
            if (!styled.isBold())
                styled = styled.withBold(true);
            if (url != null && styled.getClickEvent() == null) {
                styled = styled
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(url)));
            }
            return styled;
        };

        MutableComponent styledTitle = applyBaseStyle(titleMarkdown, baseStyle);

        List<MutableComponent> lines = wrapStyledComponent(styledTitle, Math.max(1, maxLineLength - BORDER_TOP_PREFIX.length()));
        for (int idx = 0; idx < lines.size(); idx++) {
            MutableComponent line = lines.get(idx);
            if (idx == 0) {
                appendRawLine(result, firstLine, () ->
                        Component.literal(BORDER_TOP_PREFIX).withColor(sideColor).append(line));
            } else {
                appendEmbedLine(result, sideColor, () -> line);
            }
        }
    }

    /**
     * Recursively applies {@code styler} to every node's style in the component tree.
     */
    private static MutableComponent applyBaseStyle(Component component, UnaryOperator<Style> styler) {
        MutableComponent copy = component.copy();
        copy.setStyle(styler.apply(copy.getStyle()));

        List<Component> siblings = copy.getSiblings();
        List<Component> restyled = new ArrayList<>();
        for (Component sibling : siblings)
            restyled.add(applyBaseStyle(sibling, styler));

        siblings.clear();
        siblings.addAll(restyled);

        return copy;
    }

    /**
     * Appends a non-inline field as one or more lines.
     */
    private static void appendFieldLines(MutableComponent result, int sideColor, MessageEmbed.Field field, Map<String, DiscordMentionData> mentions, int maxLineLength) {
        Component lineComponent = buildFieldLineComponent(field, mentions);
        appendComponentLines(result, sideColor, lineComponent, maxLineLength);
    }

    /**
     * Builds a single styled Component representing "Name:  Value" for one field
     */
    private static Component buildFieldLineComponent(MessageEmbed.Field field, Map<String, DiscordMentionData> mentions) {
        boolean hasName = field.getName() != null && !field.getName().isEmpty() && !field.getName().equals("‎");
        String value = field.getValue() != null ? field.getValue() : "";

        MutableComponent result = Component.empty();
        if (hasName) {
            Component nameMarkdown = convertMarkdown(field.getName(), mentions);
            MutableComponent styledName = applyBaseStyle(nameMarkdown, style -> {
                Style styled = style.getColor() != null ? style : style.withColor(ChatFormatting.GOLD.getColor());
                return styled.isBold() ? styled : styled.withBold(true);
            });
            result.append(styledName);
            result.append(Component.literal(":  ").withColor(ChatFormatting.WHITE.getColor()));
        }

        result.append(convertMarkdown(value, mentions));

        return result;
    }

    /**
     * Appends a group of inline fields on one line, separated by {@link EmbedToComponentConverter#INLINE_FIELD_SEPARATOR},
     * if the combined content fits within {@code maxLineLength}.
     */
    private static void appendInlineFieldGroup(MutableComponent result, int sideColor, List<MessageEmbed.Field> inlineGroup, Map<String, DiscordMentionData> mentions, int maxLineLength) {
        List<Component> fieldComponents = new ArrayList<>();
        int totalLength = 0;

        for (MessageEmbed.Field field : inlineGroup) {
            MessageEmbed.Field flatField = field.getValue() != null
                    ? new MessageEmbed.Field(field.getName(), field.getValue().replace("\n", " "), field.isInline())
                    : field;
            Component fieldComponent = buildFieldLineComponent(flatField, mentions);
            fieldComponents.add(fieldComponent);
            totalLength += plainLength(fieldComponent);
        }
        totalLength += INLINE_FIELD_SEPARATOR.length() * Math.max(0, fieldComponents.size() - 1);

        if (totalLength <= maxLineLength) {
            MutableComponent line = Component.empty();
            for (int i = 0; i < fieldComponents.size(); i++) {
                if (i > 0)
                    line.append(Component.literal(INLINE_FIELD_SEPARATOR).withColor(ChatFormatting.DARK_GRAY.getColor()));
                line.append(fieldComponents.get(i));
            }
            appendComponentLines(result, sideColor, line, maxLineLength);
        } else {
            for (MessageEmbed.Field field : inlineGroup) {
                appendFieldLines(result, sideColor, field, mentions, maxLineLength);
            }
        }
    }

    private static int plainLength(Component component) {
        int total = 0;
        for (TextRun run : flatten(component))
            total += run.text().length();
        return total;
    }

    /**
     * Appends the bottom border together with the footer text and/or timestamp.
     * <p>
     * If it doesn't fit on the border line, it's truncated with "…".
     * The timestamp is prioritized and always shown in full if present; the footer text is truncated first.
     */
    private static void appendFooterLines(MutableComponent result, boolean[] firstLine, int sideColor, @Nullable MessageEmbed.Footer footer, @Nullable OffsetDateTime timestamp, int maxLineLength, boolean hasContentAbove) {
        String footerText = footer != null && footer.getText() != null ? footer.getText() : null;
        String timestampText = timestamp == null
                ? null
                : timestamp.withOffsetSameInstant(ZoneOffset.ofHours(ConfigProvider.getConfig().utcOffsetHours()))
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        if (footerText == null && timestampText == null) {
            appendRawLine(result, firstLine, () -> Component.literal(BORDER_BOTTOM).withColor(sideColor));
            return;
        }

        if (hasContentAbove)
            appendEmptyLine(result, sideColor);

        int available = Math.max(0, maxLineLength - (BORDER_BOTTOM.length() + 2));
        String shownFooterText = footerText;
        if (footerText != null) {
            int separatorLength = timestampText != null ? FOOTER_TIMESTAMP_SEPARATOR.length() : 0;
            int timestampLength = timestampText != null ? timestampText.length() : 0;
            int footerBudget = available - separatorLength - timestampLength;

            if (footerBudget <= 0) {
                shownFooterText = null;
            } else if (footerText.length() > footerBudget) {
                shownFooterText = footerBudget <= 1
                        ? "…"
                        : footerText.substring(0, footerBudget - 1) + "…";
            }
        }

        boolean truncated = footerText != null && shownFooterText != null && !footerText.equals(shownFooterText);
        String combined;
        if (shownFooterText != null && timestampText != null) {
            combined = shownFooterText + FOOTER_TIMESTAMP_SEPARATOR + timestampText;
        } else if (shownFooterText != null) {
            combined = shownFooterText;
        } else if (timestampText != null) {
            combined = timestampText;
        } else {
            combined = null;
        }

        MutableComponent footerComponent = combined != null
                ? Component.literal(combined).withColor(ChatFormatting.DARK_GRAY.getColor())
                : Component.empty();

        if (truncated) {
            String fullFooterText = footerText;
            footerComponent = footerComponent.withStyle(style ->
                    style.withHoverEvent(new HoverEvent.ShowText(Component.literal(fullFooterText))));
        }

        MutableComponent finalFooterComponent = footerComponent;
        appendRawLine(result, firstLine, () ->
                Component.literal(BORDER_BOTTOM + "  ").withColor(sideColor).append(finalFooterComponent));
    }

    private static Component buildImageLine(MutableComponent label, String url) {
        MutableComponent line = label.withColor(ChatFormatting.GRAY.getColor());
        line.append(
                ComponentUtils.wrapInSquareBrackets(MinecraftLocaleProvider.open())
                        .withColor(ChatFormatting.AQUA.getColor())
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal(url)))
                        )
        );
        return line;
    }

    private static boolean isEmbedEmpty(MessageEmbed embed) {
        return embed.getTitle() == null
                && (embed.getDescription() == null || embed.getDescription().isEmpty())
                && embed.getFields().isEmpty()
                && embed.getThumbnail() == null
                && embed.getImage() == null
                && embed.getAuthor() == null
                && embed.getFooter() == null;
    }
}