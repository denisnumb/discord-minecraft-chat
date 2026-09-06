package com.denisnumb.discord_chat_mod.chat_style;

import com.denisnumb.discord_chat_mod.config.ConfigProvider;
import com.denisnumb.discord_chat_mod.markdown.MarkdownParser;
import com.denisnumb.discord_chat_mod.markdown.MarkdownToComponentConverter;
import com.denisnumb.discord_chat_mod.utils.JavaUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.denisnumb.discord_chat_mod.chat_style.Parameters.Translatable.unwrapBraces;
import static com.denisnumb.discord_chat_mod.chat_style.Parameters.X;
import static com.denisnumb.discord_chat_mod.chat_style.Parameters.Y;
import static com.denisnumb.discord_chat_mod.chat_style.Parameters.Z;
import static com.denisnumb.discord_chat_mod.chat_style.Parameters.DIMENSION;
import static com.denisnumb.discord_chat_mod.utils.JavaUtils.mergeMaps;
import static com.denisnumb.discord_chat_mod.utils.JavaUtils.nullSafeElse;

public class ChatStyleUtils {
    public static Component getStyledTranslatableMessage(
            MutableComponent template,
            String translatableParam,
            LinkedHashMap<String, Component> placeholderComponents,
            Map<String, Component> extraParams
    ) {
        if (!template.getString().contains(translatableParam))
            return applyParametersToTemplate(template, mergeMaps(placeholderComponents, extraParams));

        String[] placeholderNames = placeholderComponents.keySet().toArray(new String[0]);
        Map<String, Style> paramStyles = parseTemplateParameterStyles(template.copy(), placeholderNames);

        Object[] translatableArgs = placeholderComponents.entrySet().stream()
                .map(entry -> {
                    String placeholder = entry.getKey();
                    Component baseComponent = entry.getValue();
                    Style style = paramStyles.getOrDefault(placeholder, Style.EMPTY);
                    return baseComponent.copy().setStyle(mergeStyles(baseComponent.getStyle(), style));
                })
                .toArray();

        Component translatableContent = Component.translatable(unwrapBraces(translatableParam), translatableArgs);
        MutableComponent cleanedTemplate = removeParametersFromTemplate(template.copy(), placeholderNames);
        Map<String, Component> allParams = mergeMaps(Map.of(translatableParam, translatableContent), extraParams);

        return applyParametersToTemplate(cleanedTemplate, allParams);
    }

    public static Map<String, Style> parseTemplateParameterStyles(MutableComponent template, String... parameters) {
        Map<String, Style> parameterStyles = new HashMap<>();

        String orPattern = Arrays.stream(parameters)
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        Pattern parameterPattern = Pattern.compile(orPattern);

        for (Component templatePart : template.toFlatList()) {
            String templateString = templatePart.getString();
            Matcher matcher = parameterPattern.matcher(templateString);

            if (!matcher.find())
                continue;

            do {
                parameterStyles.put(matcher.group(), templatePart.getStyle());
            } while (matcher.find());
        }

        return parameterStyles;
    }

    public static MutableComponent removeParametersFromTemplate(MutableComponent template, String... parameters) {
        String orPattern = Arrays.stream(parameters)
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        Pattern placeholderPattern = Pattern.compile(orPattern);

        MutableComponent result = Component.empty();
        boolean pendingSpaceTrim = false;

        for (Component templatePart : template.toFlatList()) {
            String templateString = templatePart.getString();
            boolean hadPlaceholder = placeholderPattern.matcher(templateString).find();
            String cleaned = placeholderPattern.matcher(templateString).replaceAll("");

            if (hadPlaceholder) {
                cleaned = cleaned.stripLeading();
                pendingSpaceTrim = true;
            } else if (pendingSpaceTrim) {
                cleaned = cleaned.stripLeading();
                pendingSpaceTrim = false;
            }

            if (!cleaned.isEmpty()) {
                result.append(Component.literal(cleaned).withStyle(templatePart.getStyle()));
            }
        }

        return result;
    }

    public static MutableComponent parseConfigTemplateMarkdown(String configTemplate) {
        return new MarkdownToComponentConverter(
                MarkdownParser.parseMarkdown(configTemplate)
        ).convertMarkdownTokensToComponent();
    }

    public static MutableComponent applyParametersToTemplate(MutableComponent template, Map<String, Component> parameterToComponent) {
        checkNoTranslatableContents(template);
        parameterToComponent = mergeMaps(parameterToComponent, buildTimestampParameters());

        MutableComponent result = Component.empty();

        String orPattern = parameterToComponent.keySet().stream()
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        Pattern parameterPattern = Pattern.compile(orPattern);

        for (Component templatePart : template.toFlatList()) {
            String templateString = templatePart.getString();
            Matcher matcher = parameterPattern.matcher(templateString);

            if (!matcher.find()) {
                result.append(Component.literal(templateString).withStyle(templatePart.getStyle()));
                continue;
            }

            int currentPos = 0;

            do {
                String textBefore = templateString.substring(currentPos, matcher.start());
                if (!textBefore.isEmpty())
                    result.append(Component.literal(textBefore).withStyle(templatePart.getStyle()));

                Component toReplace = parameterToComponent.get(matcher.group());
                if (toReplace != null) {
                    result.append(toReplace.copy().withStyle(mergeStyles(toReplace.getStyle(), templatePart.getStyle())));
                }

                currentPos = matcher.end();
            } while (matcher.find());

            if (currentPos < templateString.length()) {
                String textAfter = templateString.substring(currentPos);
                result.append(Component.literal(textAfter).withStyle(templatePart.getStyle()));
            }
        }

        return result;
    }

    public static Map<String, Component> buildPositionComponentParameters(@Nullable Entity entity){
        return buildPositionParameters(entity).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Component.literal(e.getValue())));
    }

    public static Map<String, Component> buildTimestampParameters(){
        HashMap<String, Component> result = new HashMap<>();
        OffsetDateTime now = JavaUtils.getDateTimeWithUtcOffset(ConfigProvider.getConfig().utcOffsetHours());
        result.put(Parameters.HH, Component.literal(String.format("%02d", now.getHour())));
        result.put(Parameters.MM, Component.literal(String.format("%02d", now.getMinute())));
        result.put(Parameters.SS, Component.literal(String.format("%02d", now.getSecond())));

        return result;
    }

    public static Map<String, String> buildPositionParameters(@Nullable Entity entity){
        HashMap<String, String> result = new HashMap<>();

        if (entity == null){
            result.put(X, "");
            result.put(Y, "");
            result.put(Z, "");
            result.put(DIMENSION, "");
            return result;
        }

        BlockPos pos = entity.blockPosition();
        result.put(X, String.valueOf(pos.getX()));
        result.put(Y, String.valueOf(pos.getY()));
        result.put(Z, String.valueOf(pos.getZ()));
        result.put(DIMENSION, getDimensionName(entity.level().dimension()));

        return result;
    }

    private static String getDimensionName(ResourceKey<Level> dimension){
        if (dimension == Level.OVERWORLD)
            return "Overworld";
        if (dimension == Level.NETHER)
            return "Nether";
        if (dimension == Level.END)
            return "End";
        return prettifyDimensionPath(dimension.identifier().getPath());
    }

    private static String prettifyDimensionPath(String path){
        return Arrays.stream(path.split("_"))
                .filter(word -> !word.isEmpty())
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    private static Style mergeStyles(Style main, Style second){
        return Style.EMPTY.withBold(main.isBold() || second.isBold())
                .withItalic(main.isItalic() || second.isItalic())
                .withUnderlined(main.isUnderlined() || second.isUnderlined())
                .withStrikethrough(main.isStrikethrough() || second.isStrikethrough())
                .withObfuscated(main.isObfuscated() || second.isObfuscated())
                .withColor(nullSafeElse(main.getColor(), second.getColor()))
                .withClickEvent(nullSafeElse(main.getClickEvent(), second.getClickEvent()))
                .withHoverEvent(nullSafeElse(main.getHoverEvent(), second.getHoverEvent()))
                .withInsertion(nullSafeElse(main.getInsertion(), second.getInsertion()));
    }

    private static void checkNoTranslatableContents(Component component) {
        if (component.getContents() instanceof TranslatableContents translatable) {
            throw new IllegalStateException(
                    "Template must not contain TranslatableContents, but it does (key=" + translatable.getKey() + ")"
            );
        }
        for (Component sibling : component.getSiblings()) {
            checkNoTranslatableContents(sibling);
        }
    }
}
