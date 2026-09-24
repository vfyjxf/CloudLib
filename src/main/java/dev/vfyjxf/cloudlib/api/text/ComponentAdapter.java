package dev.vfyjxf.cloudlib.api.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Adapter between rich text nodes and vanilla {@link Component}s.
 * <p>
 * {@link #flatten(Component, Style, SegmentSink)} walks a vanilla component tree
 * (resolving translations, sibling structure and style inheritance through the
 * vanilla pipeline) into a sequence of styled text segments. {@link #toComponent}
 * converts a purely textual rich text subtree back into a vanilla component for
 * vanilla integration points.
 */
public final class ComponentAdapter {

    /**
     * A styled run of characters produced by flattening a vanilla component.
     */
    public record Segment(String text, Style style) {}

    @FunctionalInterface
    public interface SegmentSink {

        void accept(String text, Style style);
    }

    private ComponentAdapter() {
        throw new AssertionError("This class should not be instantiated!");
    }

    /**
     * Flattens a vanilla component into styled segments, in reading order.
     * Translation patterns and nested styles are resolved by the vanilla
     * {@link Component#visit(FormattedText.StyledContentConsumer, Style)} walk.
     *
     * @param component the component to flatten
     * @param base      the inherited style (use {@link Style#EMPTY} at the top level)
     * @param sink      receives each non-empty segment
     */
    public static void flatten(Component component, Style base, SegmentSink sink) {
        component.visit((style, text) -> {
            if (!text.isEmpty()) {
                sink.accept(text, style);
            }
            return Optional.empty();
        }, base);
    }

    /**
     * Converts a textual rich text subtree into a vanilla component.
     *
     * @throws IllegalStateException when the subtree embeds non-textual nodes
     */
    public static MutableComponent toComponent(RichNode node) {
        return switch (node) {
            case TextNode text -> Component.literal(text.text());
            case ComponentNode component -> component.component().copy();
            case TranslatableNode translatable ->
                Component.translatable(translatable.key(), convertArgs(translatable.args()));
            case GroupNode group -> {
                MutableComponent result = Component.empty();
                for (RichNode child : group.children()) {
                    result.append(toComponent(child));
                }
                yield result;
            }
            case StyledNode styled ->
                Component.empty().withStyle(styled.style().style()).append(toComponent(styled.child()));
            default -> throw new IllegalStateException(
                "Rich text node " + node.getClass().getSimpleName()
                        + " is not textual and cannot be converted to a vanilla Component"
            );
        };
    }

    private static Object[] convertArgs(Iterable<@Nullable Object> args) {
        var list = new ArrayList<@Nullable Object>();
        for (var arg : args) {
            if (arg instanceof RichNode node) {
                list.add(toComponent(node));
            } else {
                list.add(arg);
            }
        }
        return list.toArray();
    }
}
