package dev.vfyjxf.cloudlib.api.ui.overlay;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @param <T> the overlay widget type created by the provider
 */
public record OverlayEntry<T extends Widget>(
        Namespace id,
        OverlayScope scope,
        OverlayProvider<T> provider,
        @Nullable OverlayExclusion<T> exclusion
) {

    public OverlayEntry(
            Namespace id, OverlayScope scope,
            OverlayProvider<T> provider,
            @Nullable OverlayExclusion<T> exclusion
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.exclusion = exclusion;
    }


    public static <T extends Widget> OverlayEntry<T> screen(String id, OverlayProvider<T> provider) {
        return screen(Namespace.parse(id), provider, null);
    }

    public static <T extends Widget> OverlayEntry<T> screen(Namespace id, OverlayProvider<T> provider) {
        return screen(id, provider, null);
    }

    public static <T extends Widget> OverlayEntry<T> screen(
            String id, OverlayProvider<T> provider, @Nullable OverlayExclusion<T> exclusion
    ) {
        return screen(Namespace.parse(id), provider, exclusion);
    }

    public static <T extends Widget> OverlayEntry<T> screen(
            Namespace id, OverlayProvider<T> provider, @Nullable OverlayExclusion<T> exclusion
    ) {
        return new OverlayEntry<>(id, OverlayScope.screen, provider, exclusion);
    }

    public static <T extends Widget> OverlayEntry<T> global(String id, OverlayProvider<T> provider) {
        return global(Namespace.parse(id), provider, null);
    }

    public static <T extends Widget> OverlayEntry<T> global(Namespace id, OverlayProvider<T> provider) {
        return global(id, provider, null);
    }

    public static <T extends Widget> OverlayEntry<T> global(
            String id, OverlayProvider<T> provider, @Nullable OverlayExclusion<T> exclusion
    ) {
        return global(Namespace.parse(id), provider, exclusion);
    }

    public static <T extends Widget> OverlayEntry<T> global(
            Namespace id, OverlayProvider<T> provider, @Nullable OverlayExclusion<T> exclusion
    ) {
        return new OverlayEntry<>(id, OverlayScope.global, provider, exclusion);
    }

    public static <T extends Widget> OverlayEntry<T> of(
            String id, OverlayScope scope, OverlayProvider<T> provider,
            @Nullable OverlayExclusion<T> exclusion
    ) {
        return of(Namespace.parse(id), scope, provider, exclusion);
    }

    public static <T extends Widget> OverlayEntry<T> of(
            Namespace id, OverlayScope scope, OverlayProvider<T> provider,
            @Nullable OverlayExclusion<T> exclusion
    ) {
        return new OverlayEntry<>(id, scope, provider, exclusion);
    }
}
