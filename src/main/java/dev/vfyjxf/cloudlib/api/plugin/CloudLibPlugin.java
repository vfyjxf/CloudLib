package dev.vfyjxf.cloudlib.api.plugin;

import dev.vfyjxf.cloudlib.api.plugin.PluginDependency.Constraint;
import dev.vfyjxf.cloudlib.api.plugin.PluginDependency.Order;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleRegistry;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.CloudNamespaces;

public interface CloudLibPlugin extends ModPlugin {

    Namespace builtin = CloudNamespaces.ofMod("builtin");
    PluginDependency afterBuiltin = new PluginDependency(builtin, Order.after, Constraint.required);

    /**
     * Registers mod-defined {@link dev.vfyjxf.cloudlib.api.ui.style.key.StyleKey}s.
     * <p>
     * Runs during common setup, after the builtin {@code Styles} constants have
     * self-registered and before any css parses — custom keys can be referenced
     * by resource-pack stylesheets immediately.
     */
    default void registerStyleKeys(StyleRegistry registry) {}
}
