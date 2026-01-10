package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Manages the build process for the element tree.
 * <p>
 * BuildOwner is responsible for:
 * <ul>
 *   <li>Scheduling rebuilds for dirty elements</li>
 *   <li>Executing the build phase each frame</li>
 *   <li>Managing the element lifecycle</li>
 * </ul>
 * <p>
 * Similar to Flutter's BuildOwner, this class coordinates the rebuild
 * process to ensure efficient updates.
 */
@ApiStatus.Experimental
public class BuildOwner {

    /**
     * Elements that need to be rebuilt this frame.
     */
    private final Set<UIElement<?>> dirtyElements = new LinkedHashSet<>();

    /**
     * Whether we're currently in a build phase.
     */
    private boolean isBuilding = false;

    /**
     * The root element of the tree.
     */
    @Nullable
    private UIElement<?> rootElement;

    /**
     * Schedules an element for rebuild.
     *
     * @param element the element to rebuild
     */
    public void scheduleBuildFor(UIElement<?> element) {
        if (element.isMounted()) {
            dirtyElements.add(element);
        }
    }

    /**
     * Performs the build phase.
     * <p>
     * This should be called once per frame. It rebuilds all dirty elements
     * in depth-first order.
     */
    public void buildScope() {
        if (isBuilding) {
            return; // Prevent re-entrant builds
        }

        isBuilding = true;
        try {
            // Keep rebuilding until no more dirty elements
            while (!dirtyElements.isEmpty()) {
                // Get elements to rebuild (copy to avoid concurrent modification)
                var elementsToRebuild = new LinkedHashSet<>(dirtyElements);
                dirtyElements.clear();

                // Sort by depth (parents before children)
                var sortedElements = elementsToRebuild.stream()
                        .sorted((a, b) -> Integer.compare(getDepth(a), getDepth(b)))
                        .toList();

                for (UIElement<?> element : sortedElements) {
                    if (element.isMounted() && element.getLifecycle() == UIElement.ElementLifecycle.DIRTY) {
                        element.performRebuild();
                    }
                }
            }
        } finally {
            isBuilding = false;
        }
    }

    /**
     * Gets the depth of an element in the tree.
     */
    private int getDepth(UIElement<?> element) {
        int depth = 0;
        UIElement<?> current = element.getParent();
        while (current != null) {
            depth++;
            current = current.getParent();
        }
        return depth;
    }

    /**
     * Checks if currently in a build phase.
     *
     * @return true if building
     */
    public boolean isBuilding() {
        return isBuilding;
    }

    /**
     * Checks if there are any dirty elements waiting to be rebuilt.
     *
     * @return true if there are dirty elements
     */
    public boolean hasDirtyElements() {
        return !dirtyElements.isEmpty();
    }

    /**
     * Sets the root element.
     *
     * @param root the root element
     */
    public void setRootElement(@Nullable UIElement<?> root) {
        this.rootElement = root;
    }

    /**
     * Gets the root element.
     *
     * @return the root element
     */
    @Nullable
    public UIElement<?> getRootElement() {
        return rootElement;
    }

    /**
     * Clears all scheduled rebuilds.
     */
    public void clearDirtyElements() {
        dirtyElements.clear();
    }
}
