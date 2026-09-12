package dev.vfyjxf.cloudlib.api.plugin;

import dev.vfyjxf.cloudlib.api.plugin.PluginDependency.Constraint;
import dev.vfyjxf.cloudlib.api.plugin.PluginDependency.Order;
import dev.vfyjxf.cloudlib.api.register.ui.OverlayRegister;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldUiApi;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.CloudNamespaces;

public interface CloudLibClientPlugin extends ModPlugin {

    Namespace builtin = CloudNamespaces.ofMod("client/builtin");
    PluginDependency afterBuiltin = new PluginDependency(builtin, Order.after, Constraint.required);

    default void registerOverlay(OverlayRegister register) {
    }

    /**
     * Registers in-world UI providers. Called once during client load-complete,
     * and only when an in-world UI implementation is installed
     * ({@link dev.vfyjxf.cloudlib.api.ui.inworld.InworldUi#available()}); the
     * reference implementation lives in the separate {@code nimbusprojection} mod.
     */
    default void registerInworld(InworldUiApi inworld) {
    }

}
