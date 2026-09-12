package dev.vfyjxf.nimbusprojection.api.plugin;

import dev.vfyjxf.cloudlib.api.plugin.ModPlugin;
import dev.vfyjxf.cloudlib.api.plugin.PluginDependency;
import dev.vfyjxf.cloudlib.api.plugin.PluginDependency.Constraint;
import dev.vfyjxf.cloudlib.api.plugin.PluginDependency.Order;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.nimbusprojection.Constants;
import dev.vfyjxf.nimbusprojection.api.section.SectionRegister;

/**
 * Common-side Nimbus extension point — discovered via
 * {@code @PluginMarker} and loaded on both dists.
 * <p>
 * Client-facing hooks live on {@link NimbusClientPlugin}; this interface is
 * the marker for common registrations (server features, shared-panel
 * server handlers).
 */
public interface NimbusPlugin extends ModPlugin {

    Namespace builtin = Namespace.of(Constants.namespace, "builtin");
    PluginDependency afterBuiltin = new PluginDependency(builtin, Order.after, Constraint.required);

    /**
     * Register container section kinds — one {@code register} call wires
     * the type token, its wire codec, and the {@code SectionProvider}
     * that collects snapshots. Runs on both dists at load-complete.
     */
    default void registerContainerSections(SectionRegister register) {}
}
