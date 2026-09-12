package dev.vfyjxf.nimbusprojection.internal;

import dev.vfyjxf.cloudlib.api.plugin.PluginMarker;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.nimbusprojection.api.NimbusClient;
import dev.vfyjxf.nimbusprojection.api.plugin.NimbusClientPlugin;
import dev.vfyjxf.nimbusprojection.api.provider.ProviderOptions;
import dev.vfyjxf.nimbusprojection.api.section.SectionWidgetRegister;
import dev.vfyjxf.nimbusprojection.feature.container.ContainerPanelProvider;
import dev.vfyjxf.nimbusprojection.feature.container.section.EnergySectionWidget;
import dev.vfyjxf.nimbusprojection.feature.container.section.FluidSectionWidget;
import dev.vfyjxf.nimbusprojection.feature.container.section.ItemSectionWidget;
import dev.vfyjxf.nimbusprojection.feature.container.section.SectionTypes;
import dev.vfyjxf.nimbusprojection.feature.entity.EntityPanelProvider;
import dev.vfyjxf.nimbusprojection.feature.machine.section.HiveSectionWidget;
import dev.vfyjxf.nimbusprojection.feature.machine.section.ProgressSectionWidget;
import dev.vfyjxf.nimbusprojection.feature.machine.section.TextSectionWidget;

/**
 * Nimbus's own client registrations as a first-class plugin — the
 * built-in features dogfood the same extension point third parties use.
 */
@PluginMarker
public final class NimbusBuiltinPlugin implements NimbusClientPlugin {

    @Override
    public Namespace pluginId() {
        return NimbusClientPlugin.builtin;
    }

    @Override
    public void registerProviders(NimbusClient client) {
        client.registerProvider(new ContainerPanelProvider(), 3, ProviderOptions.shared());
        client.registerProvider(new EntityPanelProvider(), 3, ProviderOptions.shared());
    }

    @Override
    public void registerSectionWidgets(SectionWidgetRegister register) {
        register.register(SectionTypes.item, ItemSectionWidget.factory);
        register.register(SectionTypes.fluid, FluidSectionWidget.factory);
        register.register(SectionTypes.energy, EnergySectionWidget.factory);
        register.register(SectionTypes.progress, ProgressSectionWidget.factory);
        register.register(SectionTypes.text, TextSectionWidget.factory);
        register.register(SectionTypes.hive, HiveSectionWidget.factory);
    }
}
