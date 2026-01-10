package dev.vfyjxf.cloudlib.api.ui.reactive.debug;

import dev.vfyjxf.cloudlib.api.ui.reactive.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.CompositeBlueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.CompositeElement;
import dev.vfyjxf.cloudlib.api.ui.reactive.Key;
import dev.vfyjxf.cloudlib.api.ui.reactive.StableBlueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.StatefulBlueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.StatefulElement;
import dev.vfyjxf.cloudlib.api.ui.reactive.StatelessBlueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.UIElement;
import dev.vfyjxf.cloudlib.api.ui.reactive.blueprint.ButtonBlueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.blueprint.ColumnBlueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.blueprint.ContainerBlueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.blueprint.RowBlueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.blueprint.TextBlueprint;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Utility for comparing Blueprint trees and Element trees.
 * <p>
 * TreeDiffer provides:
 * <ul>
 *   <li>Structural comparison between Blueprint and Element trees</li>
 *   <li>Detection of mismatches and inconsistencies</li>
 *   <li>Detailed diff reports for debugging</li>
 * </ul>
 */
@ApiStatus.Experimental
public class TreeDiffer {

    /**
     * The type of difference detected.
     */
    public enum DiffType {
        /** Trees match */
        MATCH,
        /** Blueprint type differs from element's blueprint */
        TYPE_MISMATCH,
        /** Key mismatch */
        KEY_MISMATCH,
        /** Different number of children */
        CHILD_COUNT_MISMATCH,
        /** Blueprint exists but no corresponding element */
        MISSING_ELEMENT,
        /** Element exists but doesn't match blueprint */
        EXTRA_ELEMENT,
        /** Structural mismatch in subtree */
        SUBTREE_MISMATCH
    }

    /**
     * Represents a single difference between trees.
     */
    public record Diff(
            DiffType type,
            String path,
            @Nullable Blueprint expectedBlueprint,
            @Nullable UIElement<?> actualElement,
            String message
    ) {
        @Override
        public String toString() {
            return String.format("[%s] %s: %s", type, path, message);
        }
    }

    /**
     * Result of a tree comparison.
     */
    public record DiffResult(
            boolean matches,
            List<Diff> differences,
            int nodesCompared
    ) {
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Tree Diff Result ===\n");
            sb.append("Matches: ").append(matches).append("\n");
            sb.append("Nodes compared: ").append(nodesCompared).append("\n");
            sb.append("Differences found: ").append(differences.size()).append("\n");

            if (!differences.isEmpty()) {
                sb.append("\nDifferences:\n");
                for (Diff diff : differences) {
                    sb.append("  ").append(diff).append("\n");
                }
            }

            return sb.toString();
        }
    }

    /**
     * Compares a Blueprint tree against an Element tree.
     *
     * @param blueprint the expected blueprint tree
     * @param element   the actual element tree
     * @return the diff result
     */
    public static DiffResult compare(Blueprint blueprint, UIElement<?> element) {
        List<Diff> diffs = new ArrayList<>();
        int[] nodeCount = {0};
        compareNode(blueprint, element, "root", diffs, nodeCount);
        return new DiffResult(diffs.isEmpty(), diffs, nodeCount[0]);
    }

    /**
     * Compares a Blueprint tree against an Element tree, re-executing build functions.
     * <p>
     * This is useful for checking if the element tree matches what would be
     * produced by re-executing the blueprint's build functions.
     *
     * @param blueprint the blueprint tree (may contain StatefulBlueprints)
     * @param element   the element tree
     * @return the diff result
     */
    public static DiffResult compareWithRebuild(Blueprint blueprint, UIElement<?> element) {
        // For stateful blueprints, we need to execute the builder to get the actual child
        Blueprint expandedBlueprint = expandBlueprint(blueprint);
        return compare(expandedBlueprint, element);
    }

    private static Blueprint expandBlueprint(Blueprint blueprint) {
        if (blueprint instanceof StatefulBlueprint stateful) {
            return expandBlueprint(stateful.getBuilder().get());
        }
        return blueprint;
    }

    private static void compareNode(
            @Nullable Blueprint blueprint,
            @Nullable UIElement<?> element,
            String path,
            List<Diff> diffs,
            int[] nodeCount
    ) {
        nodeCount[0]++;

        // Handle null cases
        if (blueprint == null && element == null) {
            return;
        }
        if (blueprint == null) {
            diffs.add(new Diff(
                    DiffType.EXTRA_ELEMENT, path, null, element,
                    "Element exists but no blueprint: " + element.getClass().getSimpleName()
            ));
            return;
        }
        if (element == null) {
            diffs.add(new Diff(
                    DiffType.MISSING_ELEMENT, path, blueprint, null,
                    "Blueprint has no element: " + blueprint.getClass().getSimpleName()
            ));
            return;
        }

        // Compare blueprint types
        Blueprint elementBlueprint = element.getBlueprint();
        if (!blueprint.getClass().equals(elementBlueprint.getClass())) {
            diffs.add(new Diff(
                    DiffType.TYPE_MISMATCH, path, blueprint, element,
                    String.format("Expected %s but got %s",
                            blueprint.getClass().getSimpleName(),
                            elementBlueprint.getClass().getSimpleName())
            ));
            return; // Don't compare children if types don't match
        }

        // Compare keys
        Key expectedKey = blueprint.key();
        Key actualKey = elementBlueprint.key();
        if (!Objects.equals(expectedKey, actualKey)) {
            diffs.add(new Diff(
                    DiffType.KEY_MISMATCH, path, blueprint, element,
                    String.format("Expected key %s but got %s", expectedKey, actualKey)
            ));
        }

        // Compare children for composite blueprints
        if (blueprint instanceof CompositeBlueprint compositeBlueprint) {
            compareCompositeChildren(compositeBlueprint, element, path, diffs, nodeCount);
        } else if (blueprint instanceof StatefulBlueprint statefulBlueprint) {
            compareStatefulChild(statefulBlueprint, element, path, diffs, nodeCount);
        }
    }

    private static void compareCompositeChildren(
            CompositeBlueprint blueprint,
            UIElement<?> element,
            String path,
            List<Diff> diffs,
            int[] nodeCount
    ) {
        List<Blueprint> expectedChildren = blueprint.getChildren();

        if (!(element instanceof CompositeElement<?> compositeElement)) {
            diffs.add(new Diff(
                    DiffType.TYPE_MISMATCH, path, blueprint, element,
                    "Expected CompositeElement but got " + element.getClass().getSimpleName()
            ));
            return;
        }

        List<UIElement<?>> actualChildren = compositeElement.getChildren();

        if (expectedChildren.size() != actualChildren.size()) {
            diffs.add(new Diff(
                    DiffType.CHILD_COUNT_MISMATCH, path, blueprint, element,
                    String.format("Expected %d children but got %d",
                            expectedChildren.size(), actualChildren.size())
            ));
        }

        // Compare each child
        int minSize = Math.min(expectedChildren.size(), actualChildren.size());
        for (int i = 0; i < minSize; i++) {
            compareNode(
                    expectedChildren.get(i),
                    actualChildren.get(i),
                    path + "[" + i + "]",
                    diffs,
                    nodeCount
            );
        }

        // Report extra expected children
        for (int i = minSize; i < expectedChildren.size(); i++) {
            diffs.add(new Diff(
                    DiffType.MISSING_ELEMENT,
                    path + "[" + i + "]",
                    expectedChildren.get(i),
                    null,
                    "No element for blueprint: " + expectedChildren.get(i).getClass().getSimpleName()
            ));
        }

        // Report extra actual children
        for (int i = minSize; i < actualChildren.size(); i++) {
            diffs.add(new Diff(
                    DiffType.EXTRA_ELEMENT,
                    path + "[" + i + "]",
                    null,
                    actualChildren.get(i),
                    "Extra element: " + actualChildren.get(i).getClass().getSimpleName()
            ));
        }
    }

    private static void compareStatefulChild(
            StatefulBlueprint blueprint,
            UIElement<?> element,
            String path,
            List<Diff> diffs,
            int[] nodeCount
    ) {
        if (!(element instanceof StatefulElement<?> statefulElement)) {
            diffs.add(new Diff(
                    DiffType.TYPE_MISMATCH, path, blueprint, element,
                    "Expected StatefulElement but got " + element.getClass().getSimpleName()
            ));
            return;
        }

        // Get the child blueprint by executing the builder
        Blueprint childBlueprint = blueprint.getBuilder().get();
        UIElement<?> childElement = statefulElement.getChild();

        compareNode(childBlueprint, childElement, path + ".child", diffs, nodeCount);
    }

    /**
     * Creates a snapshot of an element tree's structure for later comparison.
     */
    public static TreeSnapshot snapshot(UIElement<?> element) {
        return new TreeSnapshot(element);
    }

    /**
     * A snapshot of an element tree's structure, including blueprint content.
     */
    public static class TreeSnapshot {
        private final String blueprintType;
        private final String elementType;
        private final Key key;
        private final Map<String, String> properties;
        private final List<TreeSnapshot> children;

        public TreeSnapshot(UIElement<?> element) {
            this.blueprintType = element.getBlueprint().getClass().getSimpleName();
            this.elementType = element.getClass().getSimpleName();
            this.key = element.getBlueprint().key();
            this.properties = extractProperties(element.getBlueprint());
            this.children = new ArrayList<>();

            element.visitChildren(child -> children.add(new TreeSnapshot(child)));
        }

        /**
         * Extracts relevant properties from a blueprint for display and comparison.
         */
        private static Map<String, String> extractProperties(Blueprint blueprint) {
            Map<String, String> props = new LinkedHashMap<>();
            
            if (blueprint instanceof TextBlueprint text) {
                props.put("text", text.getText());
                if (text.color() != TextBlueprint.DEFAULT_COLOR) {
                    props.put("color", String.format("0x%08X", text.color()));
                }
            } else if (blueprint instanceof ButtonBlueprint button) {
                props.put("label", button.label());
                if (!button.enabled()) {
                    props.put("enabled", "false");
                }
            } else if (blueprint instanceof ContainerBlueprint container) {
                if (container.paddingTop() != 0 || container.paddingRight() != 0 ||
                    container.paddingBottom() != 0 || container.paddingLeft() != 0) {
                    if (container.paddingTop() == container.paddingRight() &&
                        container.paddingTop() == container.paddingBottom() &&
                        container.paddingTop() == container.paddingLeft()) {
                        props.put("padding", String.valueOf(container.paddingTop()));
                    } else {
                        props.put("padding", String.format("%.0f,%.0f,%.0f,%.0f",
                            container.paddingTop(), container.paddingRight(),
                            container.paddingBottom(), container.paddingLeft()));
                    }
                }
                if (container.backgroundColor() != ContainerBlueprint.NO_BACKGROUND) {
                    props.put("background", String.format("0x%08X", container.backgroundColor()));
                }
                if (container.width() != ContainerBlueprint.AUTO) {
                    props.put("width", String.valueOf(container.width()));
                }
                if (container.height() != ContainerBlueprint.AUTO) {
                    props.put("height", String.valueOf(container.height()));
                }
            } else if (blueprint instanceof ColumnBlueprint column) {
                if (column.spacing() != 0) {
                    props.put("spacing", String.valueOf(column.spacing()));
                }
            } else if (blueprint instanceof RowBlueprint row) {
                if (row.spacing() != 0) {
                    props.put("spacing", String.valueOf(row.spacing()));
                }
            } else if (blueprint instanceof StatefulBlueprint) {
                props.put("type", "stateful");
            } else if (blueprint instanceof StatelessBlueprint) {
                props.put("type", "stateless");
            } else if (blueprint instanceof StableBlueprint) {
                props.put("type", "stable");
            }
            
            return props;
        }

        public String getBlueprintType() {
            return blueprintType;
        }

        public String getElementType() {
            return elementType;
        }

        public Key getKey() {
            return key;
        }

        public List<TreeSnapshot> getChildren() {
            return children;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        /**
         * Compares this snapshot to another.
         */
        public SnapshotDiff compareTo(TreeSnapshot other) {
            List<String> differences = new ArrayList<>();
            compareSnapshots(this, other, "root", differences);
            return new SnapshotDiff(differences.isEmpty(), differences);
        }

        private static void compareSnapshots(
                TreeSnapshot expected,
                TreeSnapshot actual,
                String path,
                List<String> diffs
        ) {
            if (!expected.blueprintType.equals(actual.blueprintType)) {
                diffs.add(String.format("%s: Blueprint type mismatch - expected %s, got %s",
                        path, expected.blueprintType, actual.blueprintType));
            }

            if (!Objects.equals(expected.key, actual.key)) {
                diffs.add(String.format("%s: Key mismatch - expected %s, got %s",
                        path, expected.key, actual.key));
            }

            // Compare properties (content)
            compareProperties(expected.properties, actual.properties, path, diffs);

            if (expected.children.size() != actual.children.size()) {
                diffs.add(String.format("%s: Child count mismatch - expected %d, got %d",
                        path, expected.children.size(), actual.children.size()));
            }

            int minSize = Math.min(expected.children.size(), actual.children.size());
            for (int i = 0; i < minSize; i++) {
                compareSnapshots(
                        expected.children.get(i),
                        actual.children.get(i),
                        path + "[" + i + "]",
                        diffs
                );
            }
        }

        private static void compareProperties(
                Map<String, String> expected,
                Map<String, String> actual,
                String path,
                List<String> diffs
        ) {
            // Check for changed or removed properties
            for (Map.Entry<String, String> entry : expected.entrySet()) {
                String key = entry.getKey();
                String expectedValue = entry.getValue();
                String actualValue = actual.get(key);
                
                if (actualValue == null) {
                    diffs.add(String.format("%s: Property '%s' removed (was '%s')",
                            path, key, expectedValue));
                } else if (!expectedValue.equals(actualValue)) {
                    diffs.add(String.format("%s: Property '%s' changed - '%s' -> '%s'",
                            path, key, expectedValue, actualValue));
                }
            }
            
            // Check for added properties
            for (Map.Entry<String, String> entry : actual.entrySet()) {
                String key = entry.getKey();
                if (!expected.containsKey(key)) {
                    diffs.add(String.format("%s: Property '%s' added (value '%s')",
                            path, key, entry.getValue()));
                }
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb, "", true);
            return sb.toString();
        }

        private void toString(StringBuilder sb, String prefix, boolean isLast) {
            sb.append(prefix);
            sb.append(isLast ? "└── " : "├── ");
            sb.append(elementType).append("<").append(blueprintType).append(">");
            if (key != null) {
                sb.append(" [key=").append(key).append("]");
            }
            // Add properties
            if (!properties.isEmpty()) {
                sb.append(" {");
                boolean first = true;
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    if (!first) sb.append(", ");
                    first = false;
                    sb.append(entry.getKey()).append("=");
                    // Quote string values for clarity
                    String value = entry.getValue();
                    if (entry.getKey().equals("text") || entry.getKey().equals("label")) {
                        sb.append("\"").append(value).append("\"");
                    } else {
                        sb.append(value);
                    }
                }
                sb.append("}");
            }
            sb.append("\n");

            String childPrefix = prefix + (isLast ? "    " : "│   ");
            for (int i = 0; i < children.size(); i++) {
                children.get(i).toString(sb, childPrefix, i == children.size() - 1);
            }
        }
    }

    /**
     * Result of comparing two snapshots.
     */
    public record SnapshotDiff(boolean matches, List<String> differences) {
        public String getSummary() {
            if (matches) {
                return "Snapshots match";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Snapshots differ:\n");
            for (String diff : differences) {
                sb.append("  - ").append(diff).append("\n");
            }
            return sb.toString();
        }
    }
}
