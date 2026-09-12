package dev.vfyjxf.nimbusprojection.api.plugin;

import dev.vfyjxf.cloudlib.api.plugin.ModPlugin;
import dev.vfyjxf.cloudlib.api.plugin.PluginDependency;
import dev.vfyjxf.cloudlib.api.plugin.PluginDependency.Constraint;
import dev.vfyjxf.cloudlib.api.plugin.PluginDependency.Order;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.nimbusprojection.Constants;
import dev.vfyjxf.nimbusprojection.api.NimbusClient;
import dev.vfyjxf.nimbusprojection.api.section.SectionWidgetRegister;

/**
 * Client-side Nimbus extension point — discovered via
 * {@code @PluginMarker}, loaded only on the client dist and dispatched
 * after {@code Nimbus.install} (load-complete), so {@link NimbusClient} is
 * live inside every hook.
 * <p>
 * One method per registration surface — mirror of
 * {@code CloudLibClientPlugin}'s register-per-category style.
 */
public interface NimbusClientPlugin extends ModPlugin {

    Namespace builtin = Namespace.of(Constants.namespace, "client/builtin");
    PluginDependency afterBuiltin = new PluginDependency(builtin, Order.after, Constraint.required);

    /**
     * Register panel providers ({@code client.registerProvider}) —
     * container scans, entity panels, machine reads.
     */
    default void registerProviders(NimbusClient client) {}

    /**
     * Register {@code PresentationDriver}s for custom presentation
     * descriptors ({@code client.registerPresentation}).
     */
    default void registerPresentations(NimbusClient client) {}

    /**
     * Register shared-panel materializers ({@code client.registerView}) —
     * each registration also wires the view's payload type into the panel
     * channel.
     */
    default void registerSharedViews(NimbusClient client) {}

    /**
     * Bind section kinds to their widget factories — the render half of
     * the section SPI (the data half registers through
     * {@code NimbusPlugin.registerContainerSections}).
     */
    default void registerSectionWidgets(SectionWidgetRegister register) {}
}
