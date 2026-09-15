package dev.vfyjxf.cloudlib.api.ui.base;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

/**
 * Defines a rendering layer in a Scene.
 * <p>
 * SceneLayer controls both rendering order and hit testing behavior for widgets.
 * Widgets are rendered in layer order (lower priority renders first), and within each layer,
 * they are rendered according to their zIndex.
 *
 * @see HitTestAction
 */
public enum SceneLayer {

    /**
     * Default layer for normal UI content.
     * <p>
     * HitTest: {@link HitTestAction#enabled} - full input support.
     */
    content(HitTestAction.enabled),

    /**
     * Layer for floating elements like dropdowns, popups, and context menus.
     * <p>
     * HitTest: {@link HitTestAction#enabled} - full input support.
     */
    floating(HitTestAction.enabled),

    /**
     * Overlay layer for visual effects like highlights, selection boxes, and guides.
     * <p>
     * HitTest: {@link HitTestAction#none} - does not intercept input.
     */
    overlay(HitTestAction.none),

    /**
     * Debug layer for development tools and overlays.
     * <p>
     * HitTest: {@link HitTestAction#enabled} - debug UI can receive input when enabled.
     */
    debug(HitTestAction.enabled);


    public static final ImmutableList<SceneLayer> layers = Lists.immutable.with(values());
    public static final ImmutableList<SceneLayer> extraLayers = layers.reject(l -> l == content || l == debug);

    /**
     * Gets the hit test mode of this layer.
     *
     * @return the hit test mode
     */
    public HitTestAction hitTestMode() {
        return hitTestAction;
    }

    private final HitTestAction hitTestAction;

    SceneLayer(HitTestAction hitTestAction) {
        this.hitTestAction = hitTestAction;
    }

}

