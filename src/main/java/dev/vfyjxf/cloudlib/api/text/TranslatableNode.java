package dev.vfyjxf.cloudlib.api.text;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 *
 * @param key  the translation key
 * @param args the placeholder arguments in order; a {@code null} entry renders as
 *             {@code "null"}, like vanilla
 */
public record TranslatableNode(String key, List<@Nullable Object> args) implements RichNode {

    public TranslatableNode {
        if (key == null) throw new NullPointerException("key");
        args = Collections.unmodifiableList(new ArrayList<@Nullable Object>(args));
    }

    public TranslatableNode(String key, @Nullable Object... args) {
        this(key, Arrays.asList(args));
    }
}
