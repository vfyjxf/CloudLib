package dev.vfyjxf.cloudlib.api.search;

import dev.vfyjxf.cloudlib.util.Checks;

import java.util.Locale;

/**
 * A search field definition. Fields are declared by each scenario (as its own
 * static constants) and bound into a {@link SearchFieldSet}, which owns the
 * dense ids, alias resolution and codecs — this class deliberately carries no
 * registry: the library defines the constraints ({@link SearchFieldSpec}) and
 * the machinery, scenarios define the fields they use.
 *
 * <p>Instances are singletons per scenario; identity comparison is valid.
 */
public final class SearchField {
    private final String name;
    private final SearchFieldSpec spec;
    private int id = -1;

    private SearchField(String name, SearchFieldSpec spec) {
        this.name = name;
        this.spec = spec;
    }

    /** Declares a field; the id is assigned when the owning {@link SearchFieldSet} is built. */
    public static SearchField of(String name, SearchFieldSpec spec) {
        Checks.checkNotNull(name, "name");
        Checks.checkNotNull(spec, "spec");
        return new SearchField(name.toLowerCase(Locale.ROOT), spec);
    }

    /** The canonical lowercase name of this field. */
    public String name() {
        return name;
    }

    /** Dense id within the owning {@link SearchFieldSet}; -1 while unbound. */
    public int id() {
        return id;
    }

    public SearchFieldSpec spec() {
        return spec;
    }

    public int weight() {
        return spec.weight();
    }

    public boolean numeric() {
        return spec.numeric();
    }

    /** Whether this field is a content channel ({@code in}, {@code out}, ...) supporting {@code * amount} modifiers. */
    public boolean contentChannel() {
        return spec.contentChannel();
    }

    void bind(int id) {
        if (this.id >= 0) {
            throw new IllegalStateException("Search field " + name + " is already bound to a set");
        }
        this.id = id;
    }

    @Override
    public String toString() {
        return name;
    }
}
