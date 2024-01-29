package dev.vfyjxf.cloudlib.api.ui.traits;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;

/**
 * A series of event listeners.
 */
public interface ITrait {

    IWidget holder();

    /**
     * Only call when constructed
     */
    @CanIgnoreReturnValue
    ITrait setHolder(IWidget holder);

    default void init() {

    }

    default void update() {

    }

    default void dispose() {

    }

    default ITrait asChild(IWidget widget) {
        widget.trait(this);
        return this;
    }

}
