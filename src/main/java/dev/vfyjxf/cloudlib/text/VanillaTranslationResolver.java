package dev.vfyjxf.cloudlib.text;

import dev.vfyjxf.cloudlib.api.text.layout.TranslationResolver;
import net.minecraft.locale.Language;

import java.util.Optional;

/**
 * {@link TranslationResolver} backed by the client's active {@link Language}.
 */
public final class VanillaTranslationResolver implements TranslationResolver {

    public static final VanillaTranslationResolver instance = new VanillaTranslationResolver();

    private VanillaTranslationResolver() {}

    @Override
    public Optional<String> pattern(String key) {
        Language language = Language.getInstance();
        return language.has(key) ? Optional.of(language.getOrDefault(key)) : Optional.empty();
    }
}
