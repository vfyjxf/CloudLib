package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

/**
 * Utility for printing the Blueprint/Element tree structure.
 * <p>
 * Useful for debugging the UI tree hierarchy.
 */
@ApiStatus.Experimental
public final class TreePrinter {

    private TreePrinter() {}

    /**
     * Prints a blueprint tree to standard output.
     *
     * @param blueprint the root blueprint
     */
    public static void print(Blueprint blueprint) {
        print(blueprint, System.out::println);
    }

    /**
     * Prints a blueprint tree using a custom output function.
     *
     * @param blueprint the root blueprint
     * @param output    the output function
     */
    public static void print(Blueprint blueprint, Consumer<String> output) {
        StringBuilder sb = new StringBuilder();
        printBlueprint(blueprint, sb, "", true);
        output.accept(sb.toString());
    }

    /**
     * Prints a UIElement tree to standard output.
     *
     * @param element the root element
     */
    public static void print(UIElement<?> element) {
        print(element, System.out::println);
    }

    /**
     * Prints a UIElement tree using a custom output function.
     *
     * @param element the root element
     * @param output  the output function
     */
    public static void print(UIElement<?> element, Consumer<String> output) {
        StringBuilder sb = new StringBuilder();
        printElement(element, sb, "", true);
        output.accept(sb.toString());
    }

    private static void printBlueprint(Blueprint blueprint, StringBuilder sb, String prefix, boolean isLast) {
        sb.append(prefix);
        sb.append(isLast ? "└── " : "├── ");
        sb.append(formatBlueprint(blueprint));
        sb.append("\n");

        if (blueprint instanceof CompositeBlueprint composite) {
            var children = composite.getChildren();
            for (int i = 0; i < children.size(); i++) {
                String childPrefix = prefix + (isLast ? "    " : "│   ");
                printBlueprint(children.get(i), sb, childPrefix, i == children.size() - 1);
            }
        }
    }

    private static void printElement(UIElement<?> element, StringBuilder sb, String prefix, boolean isLast) {
        sb.append(prefix);
        sb.append(isLast ? "└── " : "├── ");
        sb.append(formatElement(element));
        sb.append("\n");

        // Collect children
        java.util.List<UIElement<?>> children = new java.util.ArrayList<>();
        element.visitChildren(children::add);

        for (int i = 0; i < children.size(); i++) {
            String childPrefix = prefix + (isLast ? "    " : "│   ");
            printElement(children.get(i), sb, childPrefix, i == children.size() - 1);
        }
    }

    private static String formatBlueprint(Blueprint blueprint) {
        StringBuilder sb = new StringBuilder();
        sb.append(blueprint.getClass().getSimpleName());
        
        if (blueprint.key() != null) {
            sb.append(" [key=").append(blueprint.key()).append("]");
        }

        // Add specific info for known types
        if (blueprint instanceof dev.vfyjxf.cloudlib.api.ui.reactive.blueprint.TextBlueprint text) {
            sb.append(" \"").append(truncate(text.getText(), 20)).append("\"");
        } else if (blueprint instanceof dev.vfyjxf.cloudlib.api.ui.reactive.blueprint.ButtonBlueprint button) {
            sb.append(" \"").append(button.label()).append("\"");
        }

        return sb.toString();
    }

    private static String formatElement(UIElement<?> element) {
        StringBuilder sb = new StringBuilder();
        sb.append(element.getClass().getSimpleName());
        sb.append("<").append(element.getBlueprint().getClass().getSimpleName()).append(">");
        sb.append(" [").append(element.getLifecycle()).append("]");
        
        if (!element.getDependencies().isEmpty()) {
            sb.append(" deps=").append(element.getDependencies().size());
        }

        return sb.toString();
    }

    private static String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
    }

    /**
     * Converts a blueprint tree to a string representation.
     *
     * @param blueprint the root blueprint
     * @return the string representation
     */
    public static String toString(Blueprint blueprint) {
        StringBuilder sb = new StringBuilder();
        print(blueprint, sb::append);
        return sb.toString();
    }

    /**
     * Converts an element tree to a string representation.
     *
     * @param element the root element
     * @return the string representation
     */
    public static String toString(UIElement<?> element) {
        StringBuilder sb = new StringBuilder();
        print(element, sb::append);
        return sb.toString();
    }
}
