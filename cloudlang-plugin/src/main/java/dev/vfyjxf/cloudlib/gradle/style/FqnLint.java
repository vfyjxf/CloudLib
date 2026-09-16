package dev.vfyjxf.cloudlib.gradle.style;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reports fully qualified class references in code. A dotted type name such
 * as {@code java.util.Map.Entry} in a declaration or expression should be an
 * import plus the simple name; qualification is only legitimate when a
 * simple-name collision forces it — another type with the same name already
 * imported or declared in the file. That case is detected and allowed.
 * <p>
 * String/char/text-block literals and comments are ignored, so javadoc
 * {@code {@link}} references and example code never report. A violation on a
 * line carrying {@code // fqn-ok} is suppressed — the escape hatch for the
 * rare case the collision heuristic cannot see (e.g. an instance field chain
 * that merely looks like a package path).
 */
final class FqnLint {

    /** {@code pkg.segments.Type[.Nested]} — needs at least two package-looking segments. */
    private static final Pattern FQN =
            Pattern.compile("\\b(?:[a-z_]\\w*\\.){2,}[A-Z]\\w*(?:\\.[A-Z]\\w*)*");

    private static final Pattern PACKAGE = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);

    private static final Pattern IMPORT =
            Pattern.compile("^\\s*import\\s+(static\\s+)?([\\w.]+)\\s*;", Pattern.MULTILINE);

    private static final Pattern TYPE_DECL =
            Pattern.compile("\\b(?:class|interface|enum|record|@interface)\\s+([A-Z]\\w*)");

    private FqnLint() {}

    /**
     * Throws {@link IllegalArgumentException} with every violation found;
     * returns {@code raw} untouched when clean.
     */
    static String check(String raw) {
        String code = mask(raw);
        if (!TYPE_DECL.matcher(code).find()) {
            // no type declared → package-info / module-info / comment-only file
            return raw;
        }
        Map<String, String> imported = importedTypes(code);
        Set<String> declared = declaredTypes(code);
        int[] lineStarts = lineStarts(code);

        List<String> violations = new ArrayList<>();
        Matcher m = FQN.matcher(code);
        while (m.find()) {
            String fqn = m.group();
            if (fqn.startsWith("this.") || fqn.startsWith("super.")) {
                continue; // member-access chain, not a package path
            }
            int lineNo = lineOf(lineStarts, m.start());
            String line = rawLine(raw, lineNo);
            String trimmed = line.strip();
            if (trimmed.startsWith("import ") || trimmed.startsWith("package ")) {
                continue;
            }
            if (line.contains("fqn-ok")) {
                continue;
            }
            String[] parts = fqn.split("\\.");
            int ci = firstCapitalized(parts);
            String outer = String.join(".", List.of(parts).subList(0, ci + 1));
            String simple = parts[ci];
            String bound = imported.get(simple);
            if (bound != null && !bound.equals(outer)) {
                continue; // a different type already owns the simple name
            }
            if (bound == null && declared.contains(simple)) {
                continue; // qualification disambiguates from a declared type
            }
            String ref = String.join(".", List.of(parts).subList(ci, parts.length));
            violations.add(fqn + " at line " + lineNo + " — use '" + ref + "' (import '" + outer
                    + "'; suppress with '// fqn-ok' when the qualification is intentional)");
        }
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(
                    "fully qualified class references must be imports:\n  " + String.join("\n  ", violations));
        }
        return raw;
    }

    /** The first capitalized segment — the outer class of a (possibly nested) type name. */
    private static int firstCapitalized(String[] parts) {
        for (int i = 0; i < parts.length; i++) {
            if (Character.isUpperCase(parts[i].charAt(0))) {
                return i;
            }
        }
        return parts.length - 1;
    }

    /** Non-static imports bind a type's simple name; static imports do not. */
    private static Map<String, String> importedTypes(String code) {
        Map<String, String> imported = new HashMap<>();
        Matcher m = IMPORT.matcher(code);
        while (m.find()) {
            if (m.group(1) != null) {
                continue;
            }
            String fqn = m.group(2);
            int dot = fqn.lastIndexOf('.');
            if (dot > 0 && Character.isUpperCase(fqn.charAt(dot + 1))) {
                imported.put(fqn.substring(dot + 1), fqn);
            }
        }
        return imported;
    }

    private static Set<String> declaredTypes(String code) {
        Set<String> declared = new HashSet<>();
        Matcher m = TYPE_DECL.matcher(code);
        while (m.find()) {
            declared.add(m.group(1));
        }
        return declared;
    }

    /**
     * Replaces comments and literals with spaces, preserving newlines so
     * offsets and line numbers stay stable. Handles {@code //}, {@code /* *\/},
     * {@code "..."}, {@code '...'} and text blocks.
     */
    private static String mask(String raw) {
        StringBuilder out = new StringBuilder(raw.length());
        int i = 0;
        int n = raw.length();
        while (i < n) {
            char c = raw.charAt(i);
            if (c == '/' && i + 1 < n && raw.charAt(i + 1) == '/') {
                while (i < n && raw.charAt(i) != '\n') {
                    out.append(' ');
                    i++;
                }
            } else if (c == '/' && i + 1 < n && raw.charAt(i + 1) == '*') {
                out.append("  ");
                i += 2;
                while (i < n && !(raw.charAt(i) == '*' && i + 1 < n && raw.charAt(i + 1) == '/')) {
                    out.append(raw.charAt(i) == '\n' ? '\n' : ' ');
                    i++;
                }
                if (i < n) {
                    out.append("  ");
                    i += 2;
                }
            } else if (c == '"' && i + 2 < n && raw.charAt(i + 1) == '"' && raw.charAt(i + 2) == '"') {
                out.append("   ");
                i += 3;
                while (i < n && !(raw.charAt(i) == '"' && i + 2 < n && raw.charAt(i + 1) == '"' && raw.charAt(i + 2) == '"')) {
                    out.append(raw.charAt(i) == '\n' ? '\n' : ' ');
                    i++;
                }
                if (i < n) {
                    out.append("   ");
                    i += 3;
                }
            } else if (c == '"' || c == '\'') {
                char quote = c;
                out.append(' ');
                i++;
                while (i < n && raw.charAt(i) != quote) {
                    if (raw.charAt(i) == '\\' && i + 1 < n) {
                        out.append("  ");
                        i += 2;
                    } else {
                        out.append(raw.charAt(i) == '\n' ? '\n' : ' ');
                        i++;
                    }
                }
                if (i < n) {
                    out.append(' ');
                    i++;
                }
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static int[] lineStarts(String s) {
        List<Integer> starts = new ArrayList<>();
        starts.add(0);
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                starts.add(i + 1);
            }
        }
        return starts.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int lineOf(int[] lineStarts, int offset) {
        int lo = 0, hi = lineStarts.length - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (lineStarts[mid] <= offset) lo = mid;
            else hi = mid - 1;
        }
        return lo + 1;
    }

    private static String rawLine(String raw, int lineNo) {
        int start = 0;
        for (int l = 1; l < lineNo; l++) {
            int nl = raw.indexOf('\n', start);
            if (nl < 0) return "";
            start = nl + 1;
        }
        int end = raw.indexOf('\n', start);
        return end < 0 ? raw.substring(start) : raw.substring(start, end);
    }
}
