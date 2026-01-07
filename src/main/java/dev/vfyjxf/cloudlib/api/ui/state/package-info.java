/**
 * State management system for CloudLib UI.
 * <p>
 * This package provides production-ready state management with:
 * <ul>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.state.StateManager} - Main state holder for components</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.state.DependencyTracker} - Automatic dependency tracking</li>
 * </ul>
 * 
 * <h2>Architecture Overview</h2>
 * <pre>
 * ┌────────────────────────────────────────────────────────────────┐
 * │                      Component Render                          │
 * │                                                                │
 * │  ┌──────────────────┐        ┌──────────────────────────────┐ │
 * │  │   StateManager   │        │    DependencyTracker         │ │
 * │  │                  │        │                              │ │
 * │  │  Slot-based:     │        │  Signal → Elements mapping   │ │
 * │  │  - signal()      │◄──────►│  Auto-subscription setup     │ │
 * │  │  - computed()    │        │  Batch updates               │ │
 * │  │  - memo()        │        │                              │ │
 * │  │                  │        └──────────────────────────────┘ │
 * │  │  Keyed:          │                                        │
 * │  │  - signal(key)   │                                        │
 * │  │  - signalList()  │                                        │
 * │  └──────────────────┘                                        │
 * └────────────────────────────────────────────────────────────────┘
 * </pre>
 * 
 * <h2>Two State Management Modes</h2>
 * 
 * <h3>1. Slot-based (React Hooks style)</h3>
 * <pre>{@code
 * Component.stateful(ctx -> {
 *     var count = ctx.signal(0);     // Slot 0
 *     var name = ctx.signal("");     // Slot 1
 *     return text("Count: " + count.get());
 * });
 * }</pre>
 * <p><b>Rules:</b> Hooks must be called in same order every render. No conditional hooks.</p>
 * 
 * <h3>2. Keyed (Vue/Compose style)</h3>
 * <pre>{@code
 * Component.stateful(ctx -> {
 *     var state = (StateManager) ctx;
 *     var count = state.signal("count", 0);   // Key: "count"
 *     if (showExtra) {
 *         var extra = state.signal("extra", "");  // Key: "extra" - works in conditionals!
 *     }
 *     return text("Count: " + count.get());
 * });
 * }</pre>
 * <p><b>Benefit:</b> Supports conditional state, dynamic lists.</p>
 * 
 * <h2>Fine-grained Updates</h2>
 * <p>
 * When a Signal is read during render, the DependencyTracker records which component
 * read it. When the Signal changes, only those specific components rebuild - not the
 * entire tree.
 * </p>
 * <pre>{@code
 * Signal<Integer> count = Signal.of(0);
 * 
 * // Only CountDisplay rebuilds when count changes
 * var parent = column(
 *     embed(new CountDisplay(count)),  // Reads count → tracked
 *     embed(new StaticText())           // Doesn't read count → not tracked
 * );
 * 
 * count.set(1);  // Only CountDisplay rebuilds!
 * }</pre>
 * 
 * @see dev.vfyjxf.cloudlib.api.ui.state.StateManager
 * @see dev.vfyjxf.cloudlib.api.ui.state.DependencyTracker
 */
package dev.vfyjxf.cloudlib.api.ui.state;
