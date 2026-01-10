/**
 * Flutter-style class-based component system.
 * <p>
 * This package provides a class-based approach to defining UI components,
 * inspired by Flutter's StatelessWidget and StatefulWidget patterns.
 * <p>
 * <b>Component Types:</b>
 * <ul>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.component.StatelessComponent} -
 *       Components without internal mutable state</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.reactive.component.StatefulComponent} -
 *       Components with internal mutable state managed by
 *       {@link dev.vfyjxf.cloudlib.api.ui.reactive.component.ComponentState}</li>
 * </ul>
 * <p>
 * <b>Mixing with Functional Blueprints:</b>
 * <p>
 * Class-based components can be freely mixed with functional blueprints:
 * <pre>{@code
 * // Functional blueprint
 * StatefulBlueprint counter = StatefulBlueprint.of(() -> {
 *     Signal<Integer> count = Hooks.useState(0);
 *     return Column(() -> {
 *         Text("Count: " + count.get());
 *         Button("+", () -> count.update(n -> n + 1));
 *     });
 * });
 * 
 * // Class-based component
 * class UserCard extends StatelessComponent {
 *     private final User user;
 *     
 *     UserCard(User user) { this.user = user; }
 *     
 *     @Override
 *     protected Blueprint build(BuildContext context) {
 *         return Column(() -> {
 *             Text(user.getName());
 *             Text(user.getEmail());
 *         });
 *     }
 * }
 * 
 * // Mix them together
 * Column(() -> {
 *     counter;  // Functional
 *     new UserCard(currentUser);  // Class-based
 *     StatelessBlueprint.of(() -> Text("Footer"));  // Functional
 * });
 * }</pre>
 * <p>
 * <b>When to Use Which:</b>
 * <table border="1">
 *   <tr>
 *     <th>Approach</th>
 *     <th>Best For</th>
 *   </tr>
 *   <tr>
 *     <td>Functional (StatefulBlueprint/StatelessBlueprint)</td>
 *     <td>Simple components, quick prototyping, inline definitions</td>
 *   </tr>
 *   <tr>
 *     <td>Class-based (StatelessComponent)</td>
 *     <td>Reusable components with clear API, multiple props</td>
 *   </tr>
 *   <tr>
 *     <td>Class-based (StatefulComponent)</td>
 *     <td>Components with lifecycle needs, complex state, team collaboration</td>
 *   </tr>
 * </table>
 *
 * @see dev.vfyjxf.cloudlib.api.ui.reactive.StatefulBlueprint
 * @see dev.vfyjxf.cloudlib.api.ui.reactive.StatelessBlueprint
 */
@ApiStatus.Experimental
package dev.vfyjxf.cloudlib.api.ui.reactive.component;

import org.jetbrains.annotations.ApiStatus;
