package dev.vfyjxf.cloudlib.api.text;

import dev.vfyjxf.cloudlib.api.text.RichNode;

import java.util.List;

/**
 * A localized text with arguments, resolved against the current language at layout
 * time (so language switching relayouts correctly).
 * <p>
 * The translation pattern may use {@code %s} (sequential) and {@code %n$s} (indexed)
 * placeholders. Arguments may be:
 * <ul>
 *   <li>{@link RichNode} — spliced inline into the flow; object nodes (images, items,
 *       entities, widgets...) are embedded at the placeholder position, which is what
 *       makes localized text with embedded content possible;</li>
 *   <li>{@link net.minecraft.network.chat.Component} — flattened into styled text;</li>
 *   <li>{@link String} / {@link Number} / others — rendered via {@link String#valueOf}.</li>
 * </ul>
 * A missing translation falls back to the key itself, like vanilla does.
 */
public record TranslatableNode(String key, List<Object> args) implements RichNode {

    public TranslatableNode {
        if (key == null) throw new NullPointerException("key");
        args = List.copyOf(args);
    }

    public TranslatableNode(String key, Object... args) {
        this(key, List.of(args));
    }
}
