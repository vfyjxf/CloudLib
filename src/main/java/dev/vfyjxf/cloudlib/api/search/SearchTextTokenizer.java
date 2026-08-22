package dev.vfyjxf.cloudlib.api.search;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits normalized text into indexable words. The default {@link #words()}
 * tokenizer keeps maximal runs of ASCII letters/digits ("iron_ingot" →
 * {@code iron}, {@code ingot}; "minecraft:ingot_iron" → {@code minecraft},
 * {@code ingot}, {@code iron}) and emits every CJK character as its own token
 * so Chinese display names are searchable character by character.
 */
@FunctionalInterface
public interface SearchTextTokenizer {
    List<String> tokenize(String text);

    static SearchTextTokenizer words() {
        return DefaultWordTokenizer::tokenize;
    }

    static SearchTextTokenizer whole() {
        return text -> List.of(text);
    }

    final class DefaultWordTokenizer {
        private DefaultWordTokenizer() {
        }

        static List<String> tokenize(String text) {
            if (text == null || text.isEmpty()) {
                return List.of();
            }
            ArrayList<String> tokens = new ArrayList<>(4);
            int start = -1;
            for (int index = 0; index <= text.length(); index++) {
                char c = index < text.length() ? text.charAt(index) : ' ';
                if (isWordChar(c)) {
                    if (start < 0) {
                        start = index;
                    }
                    continue;
                }
                if (start >= 0) {
                    tokens.add(SearchTexts.lowercase(text.substring(start, index)));
                    start = -1;
                }
                if (index < text.length() && isCjk(c)) {
                    tokens.add(Character.toString(c));
                }
            }
            return tokens;
        }

        private static boolean isWordChar(char c) {
            return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9';
        }

        private static boolean isCjk(char c) {
            return c >= 0x2E80 && c <= 0x9FFF || c >= 0x3400 && c <= 0x4DBF || c >= 0xF900 && c <= 0xFAFF;
        }
    }
}
