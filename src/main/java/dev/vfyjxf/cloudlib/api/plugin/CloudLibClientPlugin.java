package dev.vfyjxf.cloudlib.api.plugin;

import dev.vfyjxf.cloudlib.api.plugin.PluginDependency.Constraint;
import dev.vfyjxf.cloudlib.api.plugin.PluginDependency.Order;
import dev.vfyjxf.cloudlib.api.register.ui.UIOverlayRegister;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.CloudNamespaces;

public interface CloudLibClientPlugin extends ModPlugin {

    Namespace builtin = CloudNamespaces.ofMod("client/builtin");
    PluginDependency afterBuiltin = new PluginDependency(builtin, Order.after, Constraint.required);

    default void registerOverlay(UIOverlayRegister register) {
    }

}
