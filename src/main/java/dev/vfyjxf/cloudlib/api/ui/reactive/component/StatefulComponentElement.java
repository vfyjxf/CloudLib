package dev.vfyjxf.cloudlib.api.ui.reactive.component;

import dev.vfyjxf.cloudlib.api.ui.reactive.AbstractElement;
import dev.vfyjxf.cloudlib.api.ui.reactive.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.BuildOwner;
import dev.vfyjxf.cloudlib.api.ui.reactive.UIElement;
import dev.vfyjxf.cloudlib.api.ui.reactive.state.ReactiveState;
import dev.vfyjxf.cloudlib.api.ui.reactive.state.Tracker;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Element for {@link StatefulComponent} blueprints.
 * <p>
 * This element manages:
 * <ul>
 *   <li>The lifecycle of the ComponentState</li>
 *   <li>Reactive state dependency tracking</li>
 *   <li>Automatic rebuilds when state changes</li>
 * </ul>
 *
 * @param <C> the component type
 * @param <S> the state type
 */
@ApiStatus.Experimental
public class StatefulComponentElement<C extends StatefulComponent<S>, S extends ComponentState<C>>
        extends AbstractElement<C> {

    @Nullable
    private S state;

    @Nullable
    private UIElement<?> child;

    public StatefulComponentElement(C blueprint) {
        super(blueprint);
    }

    @Override
    public void mount(@Nullable UIElement<?> parent, @Nullable BuildOwner owner) {
        // Create state before mount
        createState();
        super.mount(parent, owner);
    }

    @SuppressWarnings("unchecked")
    private void createState() {
        if (state == null) {
            state = (S) blueprint.createState();
            state.attach((C) blueprint, this);
            state.performInitState();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void update(C newBlueprint) {
        C oldBlueprint = this.blueprint;
        this.blueprint = newBlueprint;
        
        // Notify state of the component change
        if (state != null) {
            state.updateComponent(newBlueprint);
        }
        
        // Trigger rebuild
        markNeedsBuild();
    }

    @Override
    protected void build() {
        if (state == null) {
            return;
        }

        // Clear old state subscriptions before rebuild
        clearSubscriptions();

        // Track state dependencies during build
        try (Tracker tracker = Tracker.start()) {
            // Build using state's build method
            BuildContext buildContext = new BuildContext(this);
            Blueprint childBlueprint = state.build(buildContext);

            // Subscribe to captured dependencies
            for (ReactiveState<?> reactiveState : tracker.captured()) {
                dependencies.add(reactiveState);
                ReactiveState.Subscription sub = reactiveState.subscribe(v -> markNeedsBuild());
                subscriptions.add(sub);
            }

            // Reconcile child
            if (childBlueprint == null) {
                if (child != null) {
                    child.unmount();
                    child = null;
                }
                return;
            }

            if (child != null) {
                child = updateChild(child, childBlueprint);
            } else {
                child = inflateBlueprint(childBlueprint);
            }
        }
    }

    @Override
    protected void unmountChildren() {
        if (child != null) {
            child.unmount();
            child = null;
        }
        
        // Dispose the state
        if (state != null) {
            state.performDispose();
            state = null;
        }
    }

    @Override
    public void visitChildren(ElementVisitor visitor) {
        if (child != null) {
            visitor.visit(child);
        }
    }

    /**
     * Gets the state object.
     *
     * @return the state, or null if not yet created
     */
    @Nullable
    public S getState() {
        return state;
    }

    /**
     * Gets the child element.
     *
     * @return the child element, or null if not built
     */
    @Nullable
    public UIElement<?> getChild() {
        return child;
    }
}
