package dev.vfyjxf.cloudlib.gradle.style;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enforces the project naming rule: every {@code static final} field and
 * every enum constant is lowerCamelCase. Violations are reported, never
 * auto-fixed — renaming is a semantic change a formatter must not make.
 */
final class ConstantNamingLint {

    /** {@code static final <type> NAME} with an UPPER_SNAKE name. */
    private static final Pattern FIELD =
            Pattern.compile("\\bstatic\\s+final\\s+(?:[\\w<>\\[\\],.?]+\\s+)+([A-Z][A-Z0-9_]{1,})\\b");

    /** A line inside an enum's constant region that declares an UPPER name. */
    private static final Pattern ENUM_CONST =
            Pattern.compile("^\\s*([A-Z][A-Z0-9_]+)\\b");

    private static final Pattern ENUM_HEAD =
            Pattern.compile("\\benum\\s+\\w+[^{]*\\{");

    private ConstantNamingLint() {
    }

    /**
     * Throws {@link IllegalArgumentException} with every violation found;
     * returns {@code raw} untouched when clean.
     */
    static String check(String raw, String fileName) {
        List<String> violations = new ArrayList<>();
        collectFieldViolations(raw, fileName, violations);
        collectEnumViolations(raw, fileName, violations);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(
                    "constant names must be lowerCamelCase:\n  " + String.join("\n  ", violations));
        }
        return raw;
    }

    private static void collectFieldViolations(String raw, String fileName, List<String> violations) {
        Matcher m = FIELD.matcher(raw);
        while (m.find()) {
            violations.add(fileName + ": field '" + m.group(1) + "' at line " + lineOf(raw, m.start()));
        }
    }

    private static void collectEnumViolations(String raw, String fileName, List<String> violations) {
        for (Span span : enumConstantSpans(raw)) {
            for (String line : raw.substring(span.start, span.end).split("\n", -1)) {
                Matcher m = ENUM_CONST.matcher(line);
                if (m.matches() && declaresConstant(line, m.group(1))) {
                    violations.add(fileName + ": enum constant '" + m.group(1) + "'");
                }
            }
        }
    }

    /** The constant region of every enum body: from '{' to the depth-1 ';' or matching '}'. */
    private static List<Span> enumConstantSpans(String raw) {
        List<Span> spans = new ArrayList<>();
        Matcher m = ENUM_HEAD.matcher(raw);
        while (m.find()) {
            int i = m.end();
            int depth = 1;
            int j = i;
            while (j < raw.length() && depth > 0) {
                char c = raw.charAt(j);
                if (c == '{') depth++;
                else if (c == '}') depth--;
                else if (c == ';' && depth == 1) break;
                j++;
            }
            spans.add(new Span(i, j));
        }
        return spans;
    }

    /** A constant line ends with ',' / ';' or opens an argument/body brace. */
    private static boolean declaresConstant(String line, String name) {
        String rest = line.substring(line.indexOf(name) + name.length()).trim();
        return rest.isEmpty()
                || rest.startsWith(",")
                || rest.startsWith(";")
                || rest.startsWith("(")
                || rest.startsWith("{");
    }

    private static int lineOf(String raw, int offset) {
        int line = 1;
        for (int i = 0; i < offset && i < raw.length(); i++) {
            if (raw.charAt(i) == '\n') line++;
        }
        return line;
    }

    private record Span(int start, int end) {
    }

}
