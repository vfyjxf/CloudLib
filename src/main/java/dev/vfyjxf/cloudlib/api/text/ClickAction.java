package dev.vfyjxf.cloudlib.api.text;

import net.minecraft.network.chat.ClickEvent;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Action performed when an interactive fragment of a laid-out rich text is clicked.
 * <p>
 * Actions attach to nodes via {@link dev.vfyjxf.cloudlib.api.text.StyledNode}
 * (or the {@code onClick} builder methods) and are resolved against the fragment
 * under the mouse at click time.
 */
public sealed interface ClickAction {

    /**
     * Context handed to {@link Callback} handlers: mouse position in widget-local
     * coordinates plus the mouse button.
     */
    record Context(double mouseX, double mouseY, int button) {}

    /**
     * Invokes a user callback.
     */
    record Callback(Consumer<Context> handler) implements ClickAction {}

    /**
     * Opens an URL, asking for confirmation like vanilla screens do.
     */
    record OpenUrl(String url) implements ClickAction {}

    /**
     * Copies text to the system clipboard.
     */
    record CopyToClipboard(String text) implements ClickAction {}

    /**
     * Adapts a vanilla {@link ClickEvent}; handled with vanilla screen semantics
     * ({@code OPEN_URL}, {@code COPY_TO_CLIPBOARD} and {@code CHANGE_PAGE} are
     * supported in GUI context).
     */
    record Vanilla(ClickEvent event) implements ClickAction {}

    static ClickAction of(Consumer<Context> handler) {
        return new Callback(handler);
    }

    static ClickAction run(Runnable runnable) {
        return new Callback(context -> runnable.run());
    }

    static ClickAction openUrl(String url) {
        return new OpenUrl(url);
    }

    static ClickAction copyToClipboard(String text) {
        return new CopyToClipboard(text);
    }

    static ClickAction vanilla(ClickEvent event) {
        return new Vanilla(event);
    }

    /**
     * Picks the action of {@code node} if present, otherwise the inherited one.
     */
    static @Nullable ClickAction resolve(@Nullable ClickAction own, @Nullable ClickAction inherited) {
        return own != null ? own : inherited;
    }
}
