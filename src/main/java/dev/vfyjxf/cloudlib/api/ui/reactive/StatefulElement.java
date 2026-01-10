package dev.vfyjxf.cloudlib.api.ui.reactive;

import dev.vfyjxf.cloudlib.api.ui.reactive.state.HookContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Element for stateful blueprints that have a build function.
 * <p>
 * StatefulElement is the core of the React-like reconciliation system.
 * It captures state dependencies during build and automatically schedules
 * rebuilds when any captured state changes.
 * <p>
 * The build function is executed during every rebuild:
 * <ol>
 *   <li>The builder function is called to produce a new child Blueprint</li>
 *   <li>State access is tracked via Tracker</li>
 *   <li>The child Element is reconciled with the new Blueprint</li>
 *   <li>When tracked state changes, the element is marked dirty for next frame</li>
 * </ol>
 * <p>
 * StatefulElement also supports React-like hooks via the {@link HookContext}.
 * Hooks can be used inside the builder function to create persistent state:
 * <pre>{@code
 * import static dev.vfyjxf.cloudlib.api.ui.reactive.state.Hooks.*;
 * 
 * StatefulBlueprint ui = Stateful(() -> {
 *     // Hooks persist across rebuilds
 *     Signal<Integer> count = useState(0);  // Same Signal every rebuild
 *     
 *     return Column(() -> {
 *         Text("Count: " + count.get());
 *         Button("Increment", () -> count.update(n -> n + 1));
 *     });
 * });
 * }</pre>
 *
 * @param <B> the type of StatefulBlueprint
 * @see HookContext
 * @see dev.vfyjxf.cloudlib.api.ui.reactive.state.Hooks
 */
@ApiStatus.Experimental
public class StatefulElement<B extends StatefulBlueprint> extends AbstractElement<B> {

    @Nullable
    private UIElement<?> child;
    
    /**
     * Hook context for React-like hooks.
     * Each StatefulElement has its own HookContext that persists across rebuilds.
     */
    private final HookContext hookContext = new HookContext();

    public StatefulElement(B blueprint) {
        super(blueprint);
    }

    @Override
    protected void build() {
        // Enter hook scope before executing builder
        HookContext.enter(hookContext);
        try {
            // Execute the build function to get the child blueprint
            // State access during this call is tracked by AbstractElement.performRebuild()
            // Hook calls (useState, useMemo, etc.) are handled by hookContext
            Supplier<Blueprint> builder = blueprint.getBuilder();
            Blueprint childBlueprint = builder.get();

            if (childBlueprint == null) {
                // Builder returned null - unmount existing child
                if (child != null) {
                    child.unmount();
                    child = null;
                }
                return;
            }

            if (child != null) {
                // Try to update existing child using reconciliation
                child = updateChild(child, childBlueprint);
            } else {
                // First build - create the child
                child = inflateBlueprint(childBlueprint);
            }
        } finally {
            // Always exit hook scope, even if build fails
            HookContext.exit();
        }
    }

    @Override
    protected void unmountChildren() {
        if (child != null) {
            child.unmount();
            child = null;
        }
        // Dispose hook context when element is unmounted
        // This runs cleanup functions from useEffect
        hookContext.dispose();
    }

    @Override
    public void visitChildren(ElementVisitor visitor) {
        if (child != null) {
            visitor.visit(child);
        }
    }

    /**
     * Gets the child element.
     *
     * @return the child element, or null if not yet built
     */
    @Nullable
    public UIElement<?> getChild() {
        return child;
    }
}
