/**
 * Reactive UI framework inspired by Flutter and Jetpack Compose.
 * <p>
 * This package provides a declarative UI framework with a three-layer architecture:
 * <ul>
 *   <li><b>Blueprint</b> - Immutable configuration objects (like Flutter's Widget)</li>
 *   <li><b>UIElement</b> - Mutable lifecycle managers (like Flutter's Element)</li>
 *   <li><b>RenderWidget</b> - Actual renderable objects (like Flutter's RenderObject)</li>
 * </ul>
 * 
 * <h2>Key Concepts</h2>
 * 
 * <h3>Blueprints</h3>
 * Blueprints are lightweight, immutable descriptions of UI components. They are
 * designed to be created frequently (high-frequency execution) and compared for
 * differences. Think of them as "recipes" that describe what the UI should look like.
 * 
 * <h3>UIElements</h3>
 * Elements are the mutable, long-lived objects that manage a Blueprint's presence
 * in the tree. They handle lifecycle (mount, update, unmount), state subscriptions,
 * and child management.
 * 
 * <h3>RenderWidgets</h3>
 * RenderWidgets are the actual objects that perform layout and painting. They are
 * owned by Elements and updated when the Blueprint configuration changes.
 * 
 * <h2>DSL Usage</h2>
 * 
 * The framework provides a Compose-like DSL for building UI:
 * <pre>{@code
 * import static dev.vfyjxf.cloudlib.api.ui.reactive.Blueprints.*;
 * 
 * // Create reactive state
 * Signal<Integer> count = signal(0);
 * 
 * // Build UI declaratively
 * Blueprint ui = Column(() -> {
 *     Text("Counter Example");
 *     Row(() -> {
 *         Button("-", () -> count.update(n -> n - 1));
 *         Text(() -> String.valueOf(count.get()));
 *         Button("+", () -> count.update(n -> n + 1));
 *     });
 * });
 * 
 * // Mount the UI
 * UIElement<?> element = ui.createElement();
 * element.mount(null, null);
 * }</pre>
 * 
 * <h2>Reactive State</h2>
 * 
 * The framework integrates with the reactive state system:
 * <ul>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.state.Signal} - Mutable reactive state</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.state.Computed} - Derived reactive state</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.state.Tracker} - Dependency tracking</li>
 * </ul>
 * 
 * When a Blueprint's build function accesses reactive state, the framework automatically
 * tracks these dependencies and rebuilds the affected elements when the state changes.
 * 
 * @see dev.vfyjxf.cloudlib.api.ui.reactive.Blueprint
 * @see dev.vfyjxf.cloudlib.api.ui.reactive.UIElement
 * @see dev.vfyjxf.cloudlib.api.ui.reactive.RenderWidget
 * @see dev.vfyjxf.cloudlib.api.ui.reactive.Blueprints
 */
@ApiStatus.Experimental
package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.ApiStatus;
