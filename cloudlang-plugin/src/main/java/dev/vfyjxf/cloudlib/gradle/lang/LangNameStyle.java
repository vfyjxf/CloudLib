package dev.vfyjxf.cloudlib.gradle.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Naming styles for the constants of the generated key class.
 */
public enum LangNameStyle {

    /** lowerCamelCase, e.g. {@code rebuildUi} */
    CAMEL,
    /** UpperCamelCase / PascalCase, e.g. {@code RebuildUi} */
    PASCAL,
    /** UPPER_SNAKE_CASE, e.g. {@code REBUILD_UI} (the classic "R class" look) */
    UPPER_SNAKE;

    /**
     * Parses a style name leniently; accepted values are
     * {@code camel}, {@code pascal} and {@code upper_snake}.
     */
    public static LangNameStyle parse(String value) {
        return switch (value == null ? "" : value.trim().toLowerCase(Locale.ROOT)) {
            case "camel", "lower_camel", "camel_case" -> CAMEL;
            case "pascal", "upper_camel", "pascal_case" -> PASCAL;
            case "upper_snake", "upper_case", "screaming_snake" -> UPPER_SNAKE;
            default -> throw new IllegalArgumentException(
                    "Unknown constant name style '" + value + "', expected one of: camel, pascal, upper_snake");
        };
    }

    /**
     * Names one constant after a key segment ({@code rebuild_ui} in, {@code rebuildUi} out
     * for {@link #CAMEL}). Words are split on non-alphanumeric characters and on
     * lower-to-upper case transitions.
     */
    public String fieldName(String segment) {
        List<String> words = splitWords(segment);
        StringBuilder name = new StringBuilder();
        switch (this) {
            case CAMEL -> {
                for (int i = 0; i < words.size(); i++) {
                    String word = words.get(i);
                    name.append(i == 0 ? Character.toLowerCase(word.charAt(0)) + word.substring(1) : capitalize(word));
                }
            }
            case PASCAL -> {
                for (String word : words) {
                    name.append(capitalize(word));
                }
            }
            case UPPER_SNAKE -> {
                for (int i = 0; i < words.size(); i++) {
                    if (i > 0) {
                        name.append('_');
                    }
                    name.append(words.get(i).toUpperCase(Locale.ROOT));
                }
            }
        }
        if (name.isEmpty() || name.charAt(0) == '_' || Character.isDigit(name.charAt(0))) {
            name.insert(0, '_');
        }
        if (KEYWORDS.contains(name.toString())) {
            name.append('_');
        }
        return name.toString();
    }

    /**
     * Nested class names always follow Java type conventions (PascalCase), independent of
     * the constant style.
     */
    public String className(String segment) {
        StringBuilder name = new StringBuilder();
        boolean capitalize = true;
        for (char c : segment.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                name.append(capitalize ? Character.toUpperCase(c) : c);
                capitalize = false;
            } else {
                capitalize = true;
            }
        }
        if (name.isEmpty() || name.charAt(0) == '_' || Character.isDigit(name.charAt(0))) {
            name.insert(0, '_');
        }
        return name.toString();
    }

    /**
     * Whether the given string is usable as an explicit constant name (a valid java
     * identifier that is not a keyword).
     */
    static boolean isValidFieldName(String name) {
        if (name == null || name.isEmpty() || KEYWORDS.contains(name)) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static List<String> splitWords(String segment) {
        List<String> words = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        char[] chars = segment.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (!Character.isLetterOrDigit(c)) {
                addWord(words, word);
            } else if (!word.isEmpty()
                    && Character.isLowerCase(chars[i - 1])
                    && Character.isUpperCase(c)) {
                addWord(words, word);
                word.append(c);
            } else {
                word.append(c);
            }
        }
        addWord(words, word);
        return words;
    }

    private static void addWord(List<String> words, StringBuilder word) {
        if (!word.isEmpty()) {
            words.add(word.toString());
            word.setLength(0);
        }
    }

    private static String capitalize(String word) {
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "true", "false", "null");

}
