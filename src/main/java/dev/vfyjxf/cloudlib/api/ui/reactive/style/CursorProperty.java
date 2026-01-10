package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Built-in cursor visual property.
 * <p>
 * Cursor is applied to the {@link VisualContext} for rendering.
 *
 * @see Styles#cursor(Cursor)
 */
@ApiStatus.Experimental
public final class CursorProperty implements VisualProperty {

    public static final String NAME = "cursor";

    private final Cursor cursor;

    public CursorProperty(Cursor cursor) {
        this.cursor = cursor;
    }

    @Override
    public void applyToWidget(VisualContext context) {
        context.setCursor(cursor);
    }

    @Override
    public String name() {
        return NAME;
    }

    public Cursor getCursor() {
        return cursor;
    }

    @Override
    public String valueToString() {
        return cursor.name().toLowerCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CursorProperty that)) return false;
        return cursor == that.cursor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cursor);
    }
}
