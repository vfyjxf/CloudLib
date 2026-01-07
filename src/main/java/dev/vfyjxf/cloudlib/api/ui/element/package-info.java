/**
 * Element Layer for fine-grained UI recomposition.
 * <p>
 * This package implements a Flutter-inspired Element layer that sits between
 * RenderNode (Widget) descriptions and actual rendering. Elements provide:
 * <ul>
 *   <li><b>Fine-grained updates</b> - Only rebuild components that actually depend on changed state</li>
 *   <li><b>Instance persistence</b> - Elements maintain state across rebuilds</li>
 *   <li><b>Reconciliation</b> - Smart diffing to reuse elements when possible</li>
 *   <li><b>Lifecycle management</b> - Proper mount/unmount hooks</li>
 * </ul>
 * 
 * <h2>Architecture</h2>
 * <pre>
 * ┌─────────────┐       ┌─────────────┐       ┌─────────────────┐
 * │  RenderNode │  ──▶  │   Element   │  ──▶  │  RenderObject   │
 * │  (Widget)   │       │ (Instance)  │       │   (Painting)    │
 * └─────────────┘       └─────────────┘       └─────────────────┘
 *   Immutable             Mutable               Layout/Paint
 *   Configuration         State holder          Actual rendering
 * </pre>
 * 
 * <h2>Core Classes</h2>
 * <ul>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.element.Element} - Base class for all elements</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.element.ElementTree} - Root tree manager</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.element.BuildOwner} - Build scheduling and batching</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.element.ComponentElement} - Element for stateful components</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.element.ElementState} - State container for components</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * // Create tree
 * ElementTree tree = new ElementTree();
 * 
 * // Attach UI
 * tree.attachRoot(
 *     Column(() -> {
 *         Embed(new CounterComponent());
 *         Text("Footer");
 *     })
 * );
 * 
 * // On each frame
 * tree.flushBuild();  // Process pending rebuilds
 * }</pre>
 * 
 * <h2>Fine-grained Recomposition</h2>
 * <p>
 * When state changes, only the specific ComponentElement that depends on that
 * signal is marked dirty and rebuilt. Sibling and ancestor components are
 * untouched if they don't depend on the changed state.
 * <pre>{@code
 * class Parent implements Component.Stateful {
 *     RenderNode render(ComponentContext ctx) {
 *         var parentCount = ctx.signal(0);  // Parent's state
 *         return Column(() -> {
 *             Text("Parent: " + parentCount.get());
 *             Embed(new ChildComponent());  // Child won't rebuild when parentCount changes
 *         });
 *     }
 * }
 * 
 * class ChildComponent implements Component.Stateful {
 *     RenderNode render(ComponentContext ctx) {
 *         var childCount = ctx.signal(0);  // Child's own state
 *         return Text("Child: " + childCount.get());
 *     }
 * }
 * }</pre>
 * 
 * @since 1.0.0
 * @see dev.vfyjxf.cloudlib.api.ui.reactive.RenderNode
 * @see dev.vfyjxf.cloudlib.api.ui.reactive.Component
 */
@org.jetbrains.annotations.ApiStatus.Experimental
package dev.vfyjxf.cloudlib.api.ui.element;
