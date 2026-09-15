package dev.vfyjxf.cloudlib.api.text.layout;

import java.util.Optional;

/**
 * Resolves translation patterns for {@link dev.vfyjxf.cloudlib.api.text.TranslatableNode}s
 * at layout time, so relayout after a language switch picks up the new locale.
 * <p>
 * The runtime implementation delegates to {@code net.minecraft.locale.Language};
 * tests can back it with a plain map.
 */
@FunctionalInterface
public interface TranslationResolver {

    /**
     * @return the pattern for the given key (containing {@code %s} / {@code %n$s}
     * placeholders), or empty when the key has no translation
     */
    Optional<String> pattern(String key);
}
