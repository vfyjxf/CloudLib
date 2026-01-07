/**
 * Reactive UI System - A Java-friendly declarative UI framework.
 * <p>
 * This system separates concerns more clearly than Flutter's "everything is widget" approach:
 * <ul>
 *   <li><b>Components</b> - Pure UI rendering units, only responsible for display</li>
 *   <li><b>Providers</b> - Data dependency injection, separate from UI tree</li>
 *   <li><b>Signals/State</b> - Reactive state management</li>
 *   <li><b>Modifiers</b> - Chainable transformations for styling/behavior</li>
 *   <li><b>RenderNodes</b> - Lightweight descriptors of what to render</li>
 * </ul>
 * 
 * <h2>Architecture Overview</h2>
 * <pre>
 *     ┌─────────────────────────────────────────────────────────────┐
 *     │                      ReactiveRoot                           │
 *     │  (Entry point, manages lifecycle and provider scope)        │
 *     └─────────────────────────┬───────────────────────────────────┘
 *                               │
 *     ┌─────────────────────────▼───────────────────────────────────┐
 *     │                     Providers.Scope                         │
 *     │  (Dependency injection - services, theme, config)           │
 *     │  NOT part of UI tree - accessed via ctx.provide()           │
 *     └─────────────────────────┬───────────────────────────────────┘
 *                               │
 *     ┌─────────────────────────▼───────────────────────────────────┐
 *     │                    ComponentNode Tree                       │
 *     │  (Runtime nodes managing lifecycle, state, reconciliation)  │
 *     │                                                             │
 *     │  ComponentNode                                              │
 *     │    ├── ComponentContext (hooks: signal, effect, memo)       │
 *     │    ├── Signals & Computed values                            │
 *     │    └── Children (ComponentNode...)                          │
 *     └─────────────────────────┬───────────────────────────────────┘
 *                               │ render()
 *     ┌─────────────────────────▼───────────────────────────────────┐
 *     │                      RenderNode Tree                        │
 *     │  (Lightweight output descriptors)                           │
 *     │                                                             │
 *     │  Types: Empty, Leaf, Group, Dynamic, Conditional, ForEach   │
 *     │  Created via Render.xxx() factory methods                   │
 *     └─────────────────────────────────────────────────────────────┘
 * </pre>
 * 
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.Component} - UI building blocks (Pure/Stateless/Stateful)</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.ComponentContext} - Hooks API for state management</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.ComponentNode} - Runtime node managing component lifecycle</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.Signal} - Mutable reactive state</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.Computed} - Derived reactive values</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.Providers} - Dependency injection system</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.RenderNode} - Render output descriptors</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.Render} - Factory methods for creating RenderNodes</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.Modifier} - Chainable style/behavior modifiers</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.ReactiveRoot} - Entry point for mounting UI</li>
 * </ul>
 * 
 * <h2>Design Principles</h2>
 * <ul>
 *   <li><b>UI = f(State)</b> - Components are pure functions of state</li>
 *   <li><b>Separation of Concerns</b> - Data flow is independent from UI structure</li>
 *   <li><b>Java-friendly API</b> - Builder pattern, fluent chains, minimal nesting</li>
 *   <li><b>Immediate mode rendering</b> - No dirty region tracking needed</li>
 *   <li><b>Hooks-style state</b> - No separate State classes like Flutter</li>
 * </ul>
 * 
 * <h2>Example</h2>
 * <pre>{@code
 * // 1. Define a stateful component
 * Component counter = Component.stateful(ctx -> {
 *     Signal<Integer> count = ctx.signal(0);
 *     
 *     return Render.column(col -> col
 *         .child(Render.text(() -> "Count: " + count.get()))
 *         .child(Render.button("Increment", () -> count.update(n -> n + 1)))
 *         .spacing(8)
 *     );
 * });
 * 
 * // 2. Create root with providers
 * ReactiveRoot root = ReactiveRoot.builder(counter)
 *     .provide(THEME_KEY, Theme.dark())
 *     .provide(USER_SERVICE, new UserService())
 *     .build();
 * 
 * // 3. Mount and run
 * root.mount();
 * while (running) {
 *     root.processChanges();
 *     RenderNode output = root.render();
 *     renderBackend.render(output);
 * }
 * root.unmount();
 * }</pre>
 *
 * @see dev.vfyjxf.cloudlib.api.ui.reactive.JavaFriendlyExamples
 */
@javax.annotation.ParametersAreNonnullByDefault
@org.jetbrains.annotations.ApiStatus.Experimental
package dev.vfyjxf.cloudlib.api.ui.reactive;
