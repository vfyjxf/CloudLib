package dev.vfyjxf.cloudlib.api.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class SearchTokenizer {
    private SearchTokenizer() {
    }

    static List<Token> tokenize(String input) {
        ArrayList<Token> tokens = new ArrayList<>();
        if (input == null || input.isBlank()) {
            tokens.add(new Token(TokenType.end, "", false, 0, 0));
            return tokens;
        }

        int index = 0;
        while (index < input.length()) {
            char c = input.charAt(index);
            if (Character.isWhitespace(c)) {
                index++;
                continue;
            }
            int start = index;
            switch (c) {
                case '(' -> {
                    tokens.add(new Token(TokenType.leftParen, "(", false, start, start + 1));
                    index++;
                }
                case ')' -> {
                    tokens.add(new Token(TokenType.rightParen, ")", false, start, start + 1));
                    index++;
                }
                case '|' -> {
                    tokens.add(new Token(TokenType.or, "|", false, start, start + 1));
                    index++;
                }
                case '&' -> {
                    if (index + 1 >= input.length() || Character.isWhitespace(input.charAt(index + 1))) {
                        tokens.add(new Token(TokenType.and, "&", false, start, start + 1));
                        index++;
                    } else {
                        index = readWord(input, index, tokens);
                    }
                }
                case '-' -> {
                    tokens.add(new Token(TokenType.not, "-", false, start, start + 1));
                    index++;
                }
                case ':' -> {
                    tokens.add(new Token(TokenType.operator, ":", false, start, start + 1));
                    index++;
                }
                case '!' -> {
                    if (index + 1 < input.length() && (input.charAt(index + 1) == '=' || input.charAt(index + 1) == '~')) {
                        tokens.add(new Token(TokenType.operator, input.substring(index, index + 2), false, start, start + 2));
                        index += 2;
                    } else {
                        tokens.add(new Token(TokenType.not, "!", false, start, start + 1));
                        index++;
                    }
                }
                case '>', '<', '=', '~' -> {
                    if ((c == '>' || c == '<') && index + 1 < input.length() && input.charAt(index + 1) == '=') {
                        tokens.add(new Token(TokenType.operator, input.substring(index, index + 2), false, start, start + 2));
                        index += 2;
                    } else {
                        tokens.add(new Token(TokenType.operator, Character.toString(c), false, start, start + 1));
                        index++;
                    }
                }
                case '"' -> index = readQuoted(input, index, tokens);
                default -> index = readWord(input, index, tokens);
            }
        }
        tokens.add(new Token(TokenType.end, "", false, input.length(), input.length()));
        return List.copyOf(tokens);
    }

    private static int readQuoted(String input, int quoteIndex, List<Token> tokens) {
        StringBuilder buffer = new StringBuilder();
        int index = quoteIndex + 1;
        boolean escaped = false;
        while (index < input.length()) {
            char c = input.charAt(index);
            if (escaped) {
                buffer.append(c);
                escaped = false;
                index++;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                index++;
                continue;
            }
            if (c == '"') {
                tokens.add(new Token(TokenType.value, buffer.toString(), true, quoteIndex, index + 1));
                return index + 1;
            }
            buffer.append(c);
            index++;
        }
        throw new SearchParseException("Unclosed quoted string", quoteIndex);
    }

    private static int readWord(String input, int start, List<Token> tokens) {
        int index = start;
        while (index < input.length()) {
            char c = input.charAt(index);
            if (Character.isWhitespace(c) || c == '(' || c == ')' || c == '|' || (c == '&' && index != start) || c == '"') {
                break;
            }
            if (isStandaloneOperatorStart(input, index)) {
                break;
            }
            index++;
        }
        if (index == start) {
            char c = input.charAt(index);
            tokens.add(new Token(TokenType.value, Character.toString(c), false, start, start + 1));
            return index + 1;
        }

        String raw = input.substring(start, index);
        String lower = raw.toLowerCase(Locale.ROOT);
        TokenType type = switch (lower) {
            case "and" -> TokenType.and;
            case "or" -> TokenType.or;
            case "not" -> TokenType.not;
            default -> TokenType.value;
        };
        tokens.add(new Token(type, raw, false, start, index));
        return index;
    }

    private static boolean isStandaloneOperatorStart(String input, int index) {
        char c = input.charAt(index);
        if (c == '>' || c == '<' || c == '=' || c == '~') {
            return true;
        }
        if (c == ':') {
            return index == 0 || Character.isWhitespace(input.charAt(index - 1));
        }
        if (c == '!') {
            return index + 1 < input.length() && (input.charAt(index + 1) == '=' || input.charAt(index + 1) == '~');
        }
        return false;
    }

    enum TokenType {
        value,
        operator,
        leftParen,
        rightParen,
        and,
        or,
        not,
        end
    }

    record Token(TokenType type, String text, boolean quoted, int position, int end) {
    }
}
