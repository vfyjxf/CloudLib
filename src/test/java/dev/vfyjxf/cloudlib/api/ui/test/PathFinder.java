package dev.vfyjxf.cloudlib.api.ui.test;

import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Path-based widget finder using a simple path expression syntax.
 * <p>
 * Syntax: segments separated by {@code /}, each segment can be:
 * <ul>
 *     <li>{@code TypeName} — match by widget class simple name</li>
 *     <li>{@code TypeName[key=xxx]} — match by type + key</li>
 *     <li>{@code [key=xxx]} — any type with specified key</li>
 *     <li>{@code [N]} — match child by index (0-based)</li>
 *     <li>{@code *} — match any single widget</li>
 *     <li>{@code **} — match any depth (zero or more levels)</li>
 * </ul>
 *
 * <h3>Examples</h3>
 * <pre>{@code
 * path("root / WidgetGroup / ButtonWidget")
 * path("root / WidgetGroup[key=toolbar] / ButtonWidget")
 * path("root / ** / ButtonWidget[key=ok]")
 * path("root / WidgetGroup / [0]")
 * }</pre>
 */
public final class PathFinder {

    private PathFinder() {}

    record Segment(
            @Nullable String typeName,  // null for index-only; "*" for any; "**" for deep any
            @Nullable Object key,
            int index                   // -1 if not specified
    ) {
        boolean isDeepWildcard() {
            return "**".equals(typeName);
        }

        boolean isSingleWildcard() {
            return "*".equals(typeName);
        }

        boolean isIndexOnly() {
            return typeName == null && key == null && index >= 0;
        }

        boolean matches(Widget widget, int childIndex) {
            if (isDeepWildcard()) return true;
            if (isIndexOnly()) return childIndex == index;
            if (!isSingleWildcard() && typeName != null) {
                if (!widget.getClass().getSimpleName().equals(typeName)) return false;
            }
            if (key != null) {
                if (!key.equals(widget.key())) return false;
            }
            return true;
        }
    }

    /**
     * Parses a path expression and returns a {@link WidgetFinder}.
     */
    public static WidgetFinder parse(String pathExpr) {
        Objects.requireNonNull(pathExpr, "pathExpr");
        List<Segment> segments = parseSegments(pathExpr);
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Empty path expression: " + pathExpr);
        }
        return new WidgetFinder() {
            @Override
            public List<Widget> findAll(Widget root) {
                List<Widget> results = new ArrayList<>();
                // Root is implicit — start matching from root's children
                if (root instanceof CompositeWidget<?> group) {
                    List<? extends Widget> children = group.children();
                    for (int i = 0; i < children.size(); i++) {
                        matchPath(children.get(i), segments, 0, i, results);
                    }
                }
                return results;
            }

            @Override
            public String toString() {
                return "path(\"" + pathExpr + "\")";
            }
        };
    }

    private static void matchPath(Widget widget, List<Segment> segments, int segIdx, int childIndex, List<Widget> results) {
        if (segIdx >= segments.size()) {
            return;
        }

        Segment seg = segments.get(segIdx);

        if (seg.isDeepWildcard()) {
            // ** matches zero or more levels
            // Try skipping (match next segment at current level)
            if (segIdx + 1 < segments.size()) {
                matchPath(widget, segments, segIdx + 1, childIndex, results);
            }
            // Try going deeper
            if (widget instanceof CompositeWidget<?> group) {
                List<? extends Widget> children = group.children();
                for (int i = 0; i < children.size(); i++) {
                    matchPath(children.get(i), segments, segIdx, i, results);
                }
            }
            return;
        }

        if (!seg.matches(widget, childIndex)) {
            return;
        }

        // This segment matched
        if (segIdx == segments.size() - 1) {
            // Last segment — this is a result
            results.add(widget);
            return;
        }

        // Continue matching children against next segment
        if (widget instanceof CompositeWidget<?> group) {
            List<? extends Widget> children = group.children();
            for (int i = 0; i < children.size(); i++) {
                matchPath(children.get(i), segments, segIdx + 1, i, results);
            }
        }
    }

    static List<Segment> parseSegments(String pathExpr) {
        String[] parts = pathExpr.split("\\s*/\\s*");
        List<Segment> segments = new ArrayList<>();
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            segments.add(parseSegment(part));
        }
        return segments;
    }

    private static Segment parseSegment(String part) {
        if ("**".equals(part)) {
            return new Segment("**", null, -1);
        }
        if ("*".equals(part)) {
            return new Segment("*", null, -1);
        }

        // Check for index-only: [N]
        if (part.startsWith("[") && part.endsWith("]") && !part.contains("=")) {
            try {
                int idx = Integer.parseInt(part.substring(1, part.length() - 1).trim());
                return new Segment(null, null, idx);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid index segment: " + part);
            }
        }

        // TypeName or TypeName[key=xxx] or [key=xxx]
        String typeName = null;
        Object key = null;
        int index = -1;

        int bracketStart = part.indexOf('[');
        if (bracketStart >= 0) {
            if (bracketStart > 0) {
                typeName = part.substring(0, bracketStart);
            }
            String bracketContent = part.substring(bracketStart + 1, part.length() - 1).trim();

            if (bracketContent.startsWith("key=")) {
                key = bracketContent.substring(4);
            } else {
                try {
                    index = Integer.parseInt(bracketContent);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid bracket content: " + bracketContent);
                }
            }
        } else {
            typeName = part;
        }

        return new Segment(typeName, key, index);
    }
}
