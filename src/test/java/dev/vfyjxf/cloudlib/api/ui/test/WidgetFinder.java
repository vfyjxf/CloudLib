package dev.vfyjxf.cloudlib.api.ui.test;

import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetTree;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Functional interface for finding widgets in a widget tree.
 * <p>
 * Use the static factory methods to create finders, then compose them with
 * {@link #and}, {@link #or}, {@link #within}, etc.
 *
 * <h3>Examples</h3>
 * <pre>{@code
 * find(byKey("submit"));
 * find(byType(ButtonWidget.class));
 * find(byType(ButtonWidget.class).and(byKey("submit")));
 * find(byText("OK"));
 * find(where(w -> w.visible()));
 * findAll(byType(ButtonWidget.class).within(byKey("toolbar")));
 * }</pre>
 */
@FunctionalInterface
public interface WidgetFinder {

    /**
     * Searches the tree starting from {@code root} and returns all matching widgets.
     */
    List<Widget> findAll(Widget root);

    // ==================== Convenience ====================

    default @Nullable Widget findFirst(Widget root) {
        List<Widget> results = findAll(root);
        return results.isEmpty() ? null : results.getFirst();
    }

    /**
     * Finds exactly one matching widget. Throws if zero or more than one found.
     */
    default Widget findOne(Widget root) {
        List<Widget> results = findAll(root);
        if (results.isEmpty()) {
            throw new AssertionError("Expected exactly 1 widget, found 0. Finder: " + this);
        }
        if (results.size() > 1) {
            throw new AssertionError("Expected exactly 1 widget, found " + results.size() + ". Finder: " + this);
        }
        return results.getFirst();
    }

    // ==================== Static factories ====================

    static WidgetFinder byKey(Object key) {
        Objects.requireNonNull(key, "key");
        return new WidgetFinder() {
            @Override
            public List<Widget> findAll(Widget root) {
                return collectMatching(root, w -> key.equals(w.key()));
            }

            @Override
            public String toString() {
                return "byKey(" + key + ")";
            }
        };
    }

    static WidgetFinder byType(Class<? extends Widget> type) {
        Objects.requireNonNull(type, "type");
        return new WidgetFinder() {
            @Override
            public List<Widget> findAll(Widget root) {
                return collectMatching(root, type::isInstance);
            }

            @Override
            public String toString() {
                return "byType(" + type.getSimpleName() + ")";
            }
        };
    }

    static WidgetFinder byType(Class<? extends Widget> type, Object key) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(key, "key");
        return new WidgetFinder() {
            @Override
            public List<Widget> findAll(Widget root) {
                return collectMatching(root, w -> type.isInstance(w) && key.equals(w.key()));
            }

            @Override
            public String toString() {
                return "byType(" + type.getSimpleName() + ", key=" + key + ")";
            }
        };
    }

    static WidgetFinder where(Predicate<Widget> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return new WidgetFinder() {
            @Override
            public List<Widget> findAll(Widget root) {
                return collectMatching(root, predicate);
            }

            @Override
            public String toString() {
                return "where(<predicate>)";
            }
        };
    }

    /**
     * Finds widgets by their display text. Checks ButtonWidget.label, LabelWidget.text,
     * TextWidget.text, TextFieldWidget text.
     */
    static WidgetFinder byText(String text) {
        Objects.requireNonNull(text, "text");
        return new WidgetFinder() {
            @Override
            public List<Widget> findAll(Widget root) {
                return collectMatching(root, w -> text.equals(WidgetInspector.textOf(w)));
            }

            @Override
            public String toString() {
                return "byText(\"" + text + "\")";
            }
        };
    }

    /**
     * Path-based finder. See {@link PathFinder#parse(String)}.
     */
    static WidgetFinder path(String pathExpr) {
        return PathFinder.parse(pathExpr);
    }

    // ==================== Composition ====================

    default WidgetFinder and(WidgetFinder other) {
        Objects.requireNonNull(other, "other");
        WidgetFinder self = this;
        return new WidgetFinder() {
            @Override
            public List<Widget> findAll(Widget root) {
                List<Widget> first = self.findAll(root);
                List<Widget> second = other.findAll(root);
                List<Widget> result = new ArrayList<>(first);
                result.retainAll(second);
                return result;
            }

            @Override
            public String toString() {
                return self + ".and(" + other + ")";
            }
        };
    }

    default WidgetFinder or(WidgetFinder other) {
        Objects.requireNonNull(other, "other");
        WidgetFinder self = this;
        return new WidgetFinder() {
            @Override
            public List<Widget> findAll(Widget root) {
                List<Widget> result = new ArrayList<>(self.findAll(root));
                for (Widget w : other.findAll(root)) {
                    if (!result.contains(w)) {
                        result.add(w);
                    }
                }
                return result;
            }

            @Override
            public String toString() {
                return self + ".or(" + other + ")";
            }
        };
    }

    /**
     * Searches within the subtree of widgets matched by {@code scope}.
     */
    default WidgetFinder within(WidgetFinder scope) {
        Objects.requireNonNull(scope, "scope");
        WidgetFinder self = this;
        return new WidgetFinder() {
            @Override
            public List<Widget> findAll(Widget root) {
                List<Widget> scopes = scope.findAll(root);
                List<Widget> result = new ArrayList<>();
                for (Widget scopeRoot : scopes) {
                    for (Widget w : self.findAll(scopeRoot)) {
                        if (!result.contains(w)) {
                            result.add(w);
                        }
                    }
                }
                return result;
            }

            @Override
            public String toString() {
                return self + ".within(" + scope + ")";
            }
        };
    }

    default WidgetFinder at(int index) {
        WidgetFinder self = this;
        return new WidgetFinder() {
            @Override
            public List<Widget> findAll(Widget root) {
                List<Widget> results = self.findAll(root);
                if (index >= 0 && index < results.size()) {
                    return List.of(results.get(index));
                }
                return List.of();
            }

            @Override
            public String toString() {
                return self + ".at(" + index + ")";
            }
        };
    }

    default WidgetFinder first() {
        return at(0);
    }

    // ==================== Internal helpers ====================

    private static List<Widget> collectMatching(Widget root, Predicate<Widget> predicate) {
        List<Widget> results = new ArrayList<>();
        WidgetTree.walkBreadthFirst(root, true, -1, (widget, depth) -> {
            if (predicate.test(widget)) {
                results.add(widget);
            }
            return WidgetTree.TraversalControl.proceed;
        });
        return results;
    }
}
