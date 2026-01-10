/**
 * Built-in Blueprint implementations for common UI patterns.
 * <p>
 * This package contains ready-to-use Blueprint classes:
 * 
 * <h2>Layout Blueprints</h2>
 * <ul>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.blueprint.ColumnBlueprint} - Vertical layout</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.blueprint.RowBlueprint} - Horizontal layout</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.blueprint.StackBlueprint} - Layered/overlapping layout</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.blueprint.ContainerBlueprint} - Container with padding/background</li>
 * </ul>
 * 
 * <h2>Leaf Blueprints</h2>
 * <ul>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.blueprint.TextBlueprint} - Text display</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.blueprint.ButtonBlueprint} - Clickable button</li>
 * </ul>
 * 
 * <h2>Control Flow Blueprints</h2>
 * <ul>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.blueprint.ConditionalBlueprint} - If/else rendering</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.blueprint.ForEachBlueprint} - List rendering</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * 
 * Each Blueprint class provides static factory methods for use in the DSL:
 * <pre>{@code
 * import static dev.vfyjxf.cloudlib.api.ui.reactive.blueprint.ColumnBlueprint.Column;
 * import static dev.vfyjxf.cloudlib.api.ui.reactive.blueprint.TextBlueprint.Text;
 * 
 * Blueprint ui = Column(() -> {
 *     Text("Hello");
 *     Text("World");
 * });
 * }</pre>
 * 
 * Or use the unified {@link dev.vfyjxf.cloudlib.api.ui.reactive.Blueprints} class:
 * <pre>{@code
 * import static dev.vfyjxf.cloudlib.api.ui.reactive.Blueprints.*;
 * 
 * Blueprint ui = Column(() -> {
 *     Text("Hello");
 *     Text("World");
 * });
 * }</pre>
 * 
 * @see dev.vfyjxf.cloudlib.api.ui.reactive.Blueprint
 * @see dev.vfyjxf.cloudlib.api.ui.reactive.Blueprints
 */
@ApiStatus.Experimental
package dev.vfyjxf.cloudlib.api.ui.reactive.blueprint;

import org.jetbrains.annotations.ApiStatus;
