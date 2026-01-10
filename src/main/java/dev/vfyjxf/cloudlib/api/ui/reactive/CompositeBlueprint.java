package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * A Blueprint that contains child blueprints.
 * <p>
 * CompositeBlueprint is the base interface for layouts and containers
 * that hold other blueprints (e.g., Column, Row, Stack).
 */
@ApiStatus.Experimental
public interface CompositeBlueprint extends Blueprint {

    /**
     * Gets the child blueprints.
     *
     * @return the list of children
     */
    List<Blueprint> getChildren();

    @Override
    default UIElement<?> createElement() {
        return new CompositeElement<>(this);
    }
}
