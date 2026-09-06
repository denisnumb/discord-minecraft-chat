package com.denisnumb.buildtools;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class GenerateLocaleProviderTask extends DefaultTask {
    private static final String ROOT_PREFIX = "discord_chat_mod";
    private static final String PACKAGE_NAME = "com.denisnumb.discord_chat_mod.locale";

    private static final String MINECRAFT_CLASS_NAME = "MinecraftLocaleProvider";
    private static final String DISCORD_CLASS_NAME = "DiscordLocaleProvider";

    private static final Pattern PLACEHOLDER = Pattern.compile("%(\\d+\\$)?([sd])");

    @InputFile
    public abstract RegularFileProperty getSourceJson();

    @OutputDirectory
    public abstract org.gradle.api.file.DirectoryProperty getOutputDirectory();

    @TaskAction
    public void generate() throws IOException {
        Path jsonPath = getSourceJson().get().getAsFile().toPath();
        Map<String, String> flat;
        try (FileReader reader = new FileReader(jsonPath.toFile())) {
            Type type = new TypeToken<LinkedHashMap<String, String>>() {}.getType();
            flat = new Gson().fromJson(reader, type);
        }

        Node root = new Node("ROOT");
        for (Map.Entry<String, String> e : flat.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();
            String trimmed = key.startsWith(ROOT_PREFIX + ".")
                    ? key.substring(ROOT_PREFIX.length() + 1)
                    : key;
            String[] parts = trimmed.split("\\.");
            insert(root, parts, 0, key, value);
        }

        Path outDir = getOutputDirectory().get().getAsFile().toPath()
                .resolve(PACKAGE_NAME.replace('.', '/'));
        Files.createDirectories(outDir);

        String minecraftSource = renderClass(root, jsonPath.getFileName().toString(), Side.MINECRAFT);
        String discordSource = renderClass(root, jsonPath.getFileName().toString(), Side.DISCORD);

        Files.writeString(outDir.resolve(MINECRAFT_CLASS_NAME + ".java"), minecraftSource);
        Files.writeString(outDir.resolve(DISCORD_CLASS_NAME + ".java"), discordSource);
    }

    private enum Side { MINECRAFT, DISCORD }

    private static class Node {
        final String name;
        final Map<String, Node> children = new LinkedHashMap<>();
        String leafFullKey;
        String leafValue;

        Node(String name) {
            this.name = name;
        }

        boolean isLeaf() {
            return leafFullKey != null;
        }
    }

    private void insert(Node current, String[] parts, int idx, String fullKey, String value) {
        if (idx == parts.length - 1) {
            String leafName = parts[idx];
            Node childNode = current.children.computeIfAbsent(leafName, Node::new);
            if (!childNode.children.isEmpty()) {
                throw new IllegalStateException(
                        "Localization key conflict: \"" + fullKey + "\" is trying to become " +
                                "leaf, but is already used as a prefix for nested keys. " +
                                "Rename one of the conflicting keys."
                );
            }
            childNode.leafFullKey = fullKey;
            childNode.leafValue = value;
            return;
        }
        String segment = parts[idx];
        Node child = current.children.computeIfAbsent(segment, Node::new);
        if (child.isLeaf()) {
            throw new IllegalStateException(
                    "Localization key conflict: \"" + fullKey + "\" requires \"" +
                            child.leafFullKey + "\" was a container class, but it is already " +
                            "registered as a separate key. Rename one of the keys."
            );
        }
        insert(child, parts, idx + 1, fullKey, value);
    }

    private String renderClass(Node root, String sourceFileName, Side side) {
        String className = side == Side.MINECRAFT ? MINECRAFT_CLASS_NAME : DISCORD_CLASS_NAME;

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(PACKAGE_NAME).append(";\n\n");
        if (side == Side.MINECRAFT){
            sb.append("import net.minecraft.network.chat.Component;\n");
            sb.append("import net.minecraft.network.chat.MutableComponent;\n");
            sb.append("\n");
        }
        sb.append("// ============================================================\n");
        sb.append("// AUTO GENERATED FILE. DO NOT EDIT MANUALLY.\n");
        sb.append("// Source: ").append(sourceFileName).append("\n");
        sb.append("// ============================================================\n");
        sb.append("public final class ").append(className).append(" {\n");
        sb.append("    private ").append(className).append("() {}\n\n");
        if (side == Side.DISCORD){
            sb.append("    public static String getTranslate(String key) {\n")
                    .append("        return LocaleStorage.getTranslate(key);\n")
                    .append("    }\n\n");
        }
        writeNode(sb, root, 1, side);
        sb.append("}\n");
        return sb.toString();
    }

    private void writeNode(StringBuilder sb, Node node, int depth, Side side) {
        String indent = "    ".repeat(depth);
        for (Node child : node.children.values()) {
            if (child.isLeaf()) {
                writeLeaf(sb, child, indent, side);
            } else {
                String className = toPascalCase(child.name);
                sb.append(indent).append("public static final class ").append(className).append(" {\n");
                sb.append(indent).append("    private ").append(className).append("() {}\n\n");
                writeNode(sb, child, depth + 1, side);
                sb.append(indent).append("}\n\n");
            }
        }
    }

    private void writeLeaf(StringBuilder sb, Node leaf, String indent, Side side) {
        List<String> placeholderTypes = extractPlaceholderTypes(leaf.leafValue);
        String methodName = toCamelCase(leaf.name);

        String keyString = "\"" + leaf.leafFullKey + "\"";
        String escapedValue = leaf.leafValue.replace("\\", "\\\\").replace("\"", "\\\"");

        StringBuilder params = new StringBuilder();
        StringBuilder args = new StringBuilder();
        for (int i = 0; i < placeholderTypes.size(); i++) {
            if (i > 0) {
                params.append(", ");
                args.append(", ");
            }
            params.append(placeholderTypes.get(i)).append(" arg").append(i + 1);
            args.append("arg").append(i + 1);
        }
        String paramList = params.toString();
        String argList = args.toString();
        String argListWithLeadingComma = argList.isEmpty() ? "" : ", " + argList;

        if (side == Side.MINECRAFT) {
            sb.append(indent).append("/** <pre>{@code ").append(escapedValue).append(" }</pre> */\n");
            sb.append(indent).append("public static MutableComponent ").append(methodName)
                    .append("(").append(paramList).append(") {\n");
            sb.append(indent).append("    return Component.translatableWithFallback(")
                    .append(keyString)
                    .append(", LocaleStorage.getTranslate(").append(keyString).append(")")
                    .append(argListWithLeadingComma)
                    .append(");\n");
            sb.append(indent).append("}\n\n");
            return;
        }

        sb.append(indent).append("/** <pre>{@code ").append(escapedValue).append(" }</pre> */\n");
        sb.append(indent).append("public static String ").append(methodName)
                .append("(").append(paramList).append(") {\n");
        if (argList.isEmpty()) {
            sb.append(indent).append("    return LocaleStorage.getTranslate(").append(keyString).append(");\n");
        } else {
            sb.append(indent).append("    return String.format(LocaleStorage.getTranslate(")
                    .append(keyString).append("), ").append(argList).append(");\n");
        }
        sb.append(indent).append("}\n\n");
    }

    private List<String> extractPlaceholderTypes(String value) {
        List<String> types = new ArrayList<>();
        Matcher m = PLACEHOLDER.matcher(value);
        boolean positional = value.contains("$");
        if (!positional) {
            while (m.find()) {
                types.add(m.group(2).equals("d") ? "Number" : "Object");
            }
            return types;
        }
        TreeMap<Integer, String> byPos = new TreeMap<>();
        while (m.find()) {
            int pos = Integer.parseInt(m.group(1).replace("$", ""));
            byPos.put(pos, m.group(2).equals("d") ? "Number" : "Object");
        }
        types.addAll(byPos.values());
        return types;
    }

    private String toPascalCase(String snake) {
        String[] parts = snake.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    private String toCamelCase(String snake) {
        String pascal = toPascalCase(snake);
        return Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
    }
}