package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.util.Checks;
import dev.vfyjxf.cloudlib.api.ui.event.WidgetEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A handle for managing scoped resources during widget lifecycle.
 *
 * <p>SceneHandle enables widgets to register cleanup actions that will be
 * automatically executed based on the handle's scope.
 * <p>Stores cleanup actions in a stack (LIFO order) and executes them
 * when {@link #cleanup()} is called. This ensures that resources are
 * released in reverse order of acquisition.</p>
 *
 * @see Widget#onMount(WidgetEvent.OnMount)
 */
public final class SceneHandle {

    private static final Logger log = LoggerFactory.getLogger("SceneHandle");

    /**
     * Creates a new SceneHandle bound to a scene.
     *
     * @param scene the scene this handle belongs to
     * @return a new SceneHandle instance
     */
    public static SceneHandle create(Scene scene) {
        return new SceneHandle(scene);
    }

    private final Scene scene;

    // Cleanup actions stored in LIFO order (stack)
    private final Deque<Runnable> cleanupActions = new ArrayDeque<>();
    private boolean cleaned = false;

    private SceneHandle(Scene scene) {
        Checks.checkNotNull(scene, "scene");
        this.scene = scene;
    }

    /**
     * Gets the scene-level (global) handle.
     *
     * <p>Cleanup actions registered on the returned handle will be executed
     * when the entire scene is destroyed. Use this for truly global resources
     * that should persist across widget mount/unmount cycles.</p>
     *
     * @return the scene-level handle
     */
    public SceneHandle scene() {
        return scene.globalHandle;
    }

    //region cleanup registration

    /**
     * Registers a cleanup action to be executed when the widget is unmounted.
     *
     * @param cleanup the cleanup action to execute
     * @return this handle for chaining
     */
    public SceneHandle onCleanup(Runnable cleanup) {
        if (cleaned) {
            throw new IllegalStateException("Cannot register cleanup on an already cleaned handle");
        }
        cleanupActions.push(cleanup);
        return this;
    }

    /**
     * Attaches a value to a target and registers automatic detachment on cleanup.
     *
     * @param target   the target to attach to
     * @param value    the value to attach
     * @param attacher function to attach the value
     * @param detacher function to detach the value
     * @param <T>      the target type
     * @param <V>      the value type
     * @return this handle for chaining
     */
    public <T, V> SceneHandle attach(T target, V value, Attacher<T, V> attacher, Detacher<T, V> detacher) {
        attacher.attach(target, value);
        return onCleanup(() -> detacher.detach(target, value));
    }

    /**
     * Function to attach a value to a target.
     */
    @FunctionalInterface
    public interface Attacher<T, V> {
        void attach(T target, V value);
    }

    /**
     * Function to detach a value from a target.
     */
    @FunctionalInterface
    public interface Detacher<T, V> {
        void detach(T target, V value);
    }

    /**
     * Executes cleanup actions registered on this handle.
     *
     * <p>This is called automatically when the widget is unmounted.
     * Manual invocation is typically not needed.</p>
     */
    public void cleanup() {
        if (cleaned) {
            return;
        }
        cleaned = true;

        // Execute cleanup actions in LIFO order
        while (!cleanupActions.isEmpty()) {
            Runnable action = cleanupActions.pop();
            try {
                action.run();
            } catch (Exception e) {
                // Log but don't stop cleanup
                log.error("Error during SceneHandle cleanup", e);
            }
        }
    }

    /**
     * Checks if this handle has any pending cleanup actions.
     *
     * @return true if there are cleanup actions registered
     */
    public boolean hasCleanupActions() {
        return !cleanupActions.isEmpty();
    }

    //endregion
}
