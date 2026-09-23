package dev.vfyjxf.cloudlib.api.ui.style.key;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared context handed to {@link StyleParser} implementations while turning a
 * css declaration value into a typed {@link StyleKey} value.
 * <p>
 * Parsers report recoverable problems through {@link #warn(String)} instead of
 * throwing — a malformed declaration is dropped, never fatal.
 */
public final class StyleParseContext {

    private static final Logger logger = LoggerFactory.getLogger("cloudlib/style");

    private final String source;

    public StyleParseContext(String source) {
        this.source = source;
    }

    /** Default context for programmatic / test parsing. */
    public static StyleParseContext plain() {
        return new StyleParseContext("<inline>");
    }

    /**
     * Where the declaration came from (theme file id, selector, …) — used in
     * warnings so a bad declaration can be traced back to its source.
     */
    public String source() {
        return source;
    }

    /** Reports a recoverable parse problem; the declaration is then dropped. */
    public void warn(String message) {
        logger.warn("[{}] {}", source, message);
    }

    /** Reports a recoverable parse problem with a detail value. */
    public void warn(String message, @Nullable Object detail) {
        logger.warn("[{}] {} ({})", source, message, detail);
    }
}
