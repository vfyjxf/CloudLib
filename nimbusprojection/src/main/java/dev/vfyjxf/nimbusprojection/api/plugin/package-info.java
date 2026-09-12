/**
 * Nimbus's plugin surface, riding CloudLib's plugin infrastructure:
 * interfaces extend {@link dev.vfyjxf.cloudlib.api.plugin.ModPlugin}, are
 * discovered via {@code @PluginMarker} annotation scanning, and are
 * dependency-ordered by the {@code PluginLoader}.
 * <ul>
 *   <li>{@link dev.vfyjxf.nimbusprojection.api.plugin.NimbusPlugin} —
 *       common-side marker</li>
 *   <li>{@link dev.vfyjxf.nimbusprojection.api.plugin.NimbusClientPlugin} —
 *       client registrations: providers, presentation drivers, shared
 *       views</li>
 * </ul>
 */
@dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault
package dev.vfyjxf.nimbusprojection.api.plugin;
